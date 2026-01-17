"""
Neo4j Custom Metadata Ingestion for OpenMetadata

Since OpenMetadata doesn't have a native Neo4j connector,
this script creates a custom ingestion pipeline that:
1. Extracts node labels and properties from Neo4j
2. Maps them to OpenMetadata table/column format
3. Creates lineage relationships based on Neo4j relationships

Usage:
    python neo4j_ingestion.py --project-id <project_id>
"""

import os
import sys
import json
import logging
import requests
from typing import Dict, List, Optional, Any
from dataclasses import dataclass
from datetime import datetime

try:
    from neo4j import GraphDatabase
except ImportError:
    print("neo4j driver not installed. Run: pip install neo4j")
    sys.exit(1)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@dataclass
class Neo4jNodeSchema:
    """Represents a Neo4j node label as a table schema"""
    label: str
    properties: List[Dict[str, str]]
    sample_count: int
    relationships: List[Dict[str, str]]


class Neo4jMetadataExtractor:
    """Extracts metadata from Neo4j and formats for OpenMetadata"""

    NEO4J_TO_OM_TYPE_MAP = {
        "STRING": "STRING",
        "INTEGER": "BIGINT",
        "LONG": "BIGINT",
        "FLOAT": "DOUBLE",
        "DOUBLE": "DOUBLE",
        "BOOLEAN": "BOOLEAN",
        "DATE": "DATE",
        "DATETIME": "DATETIME",
        "LOCALDATETIME": "DATETIME",
        "LIST": "ARRAY",
        "MAP": "MAP",
        "POINT": "STRING",
        "DURATION": "STRING",
    }

    def __init__(self):
        self.driver = GraphDatabase.driver(
            os.getenv("NEO4J_URI", "bolt://localhost:7687"),
            auth=(
                os.getenv("NEO4J_USER", "neo4j"),
                os.getenv("NEO4J_PASSWORD", "pmspassword123")
            )
        )
        self.om_url = os.getenv("OPENMETADATA_URL", "http://localhost:8585")
        self.om_token = os.getenv("OM_JWT_TOKEN", "")

    def close(self):
        if self.driver:
            self.driver.close()

    def get_all_labels(self) -> List[str]:
        """Get all node labels in the database"""
        with self.driver.session() as session:
            result = session.run("CALL db.labels()")
            return [record["label"] for record in result]

    def get_label_properties(self, label: str) -> List[Dict[str, str]]:
        """Get properties and their types for a specific label"""
        with self.driver.session() as session:
            # Use APOC to get property types
            query = f"""
            MATCH (n:{label})
            WITH n LIMIT 100
            UNWIND keys(n) AS key
            WITH key, apoc.meta.cypher.type(n[key]) AS type
            RETURN DISTINCT key AS property, type
            ORDER BY key
            """
            try:
                result = session.run(query)
                return [
                    {
                        "name": record["property"],
                        "type": record["type"],
                        "om_type": self._map_type(record["type"])
                    }
                    for record in result
                ]
            except Exception as e:
                logger.warning(f"APOC not available, using fallback: {e}")
                return self._get_properties_fallback(session, label)

    def _get_properties_fallback(self, session, label: str) -> List[Dict[str, str]]:
        """Fallback method without APOC"""
        query = f"""
        MATCH (n:{label})
        WITH n LIMIT 100
        UNWIND keys(n) AS key
        RETURN DISTINCT key AS property
        ORDER BY key
        """
        result = session.run(query)
        return [
            {
                "name": record["property"],
                "type": "STRING",
                "om_type": "STRING"
            }
            for record in result
        ]

    def get_label_count(self, label: str) -> int:
        """Get count of nodes with specific label"""
        with self.driver.session() as session:
            result = session.run(f"MATCH (n:{label}) RETURN count(n) AS count")
            record = result.single()
            return record["count"] if record else 0

    def get_relationships(self, label: str) -> List[Dict[str, str]]:
        """Get outgoing relationships from a label"""
        with self.driver.session() as session:
            query = f"""
            MATCH (n:{label})-[r]->(m)
            RETURN DISTINCT type(r) AS relationship, labels(m)[0] AS target
            """
            result = session.run(query)
            return [
                {
                    "type": record["relationship"],
                    "target": record["target"]
                }
                for record in result
            ]

    def _map_type(self, neo4j_type: str) -> str:
        """Map Neo4j type to OpenMetadata type"""
        # Handle LIST types like "LIST OF STRING"
        if neo4j_type and neo4j_type.startswith("LIST"):
            return "ARRAY"

        return self.NEO4J_TO_OM_TYPE_MAP.get(
            neo4j_type.upper() if neo4j_type else "STRING",
            "STRING"
        )

    def extract_all_schemas(self) -> List[Neo4jNodeSchema]:
        """Extract schema information for all labels"""
        schemas = []
        labels = self.get_all_labels()

        for label in labels:
            logger.info(f"Extracting schema for label: {label}")

            properties = self.get_label_properties(label)
            count = self.get_label_count(label)
            relationships = self.get_relationships(label)

            schema = Neo4jNodeSchema(
                label=label,
                properties=properties,
                sample_count=count,
                relationships=relationships
            )
            schemas.append(schema)

        return schemas

    def create_om_database_service(self) -> Optional[str]:
        """Create or get Neo4j database service in OpenMetadata"""
        service_name = "pms-neo4j"

        headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }

        # Check if service exists
        response = requests.get(
            f"{self.om_url}/api/v1/services/databaseServices/name/{service_name}",
            headers=headers
        )

        if response.status_code == 200:
            logger.info(f"Database service '{service_name}' already exists")
            return response.json().get("id")

        # Create new service
        service_data = {
            "name": service_name,
            "displayName": "PMS Neo4j Graph Database",
            "description": "Neo4j graph database for PMS-IC GraphRAG and Scrum workflow",
            "serviceType": "CustomDatabase",
            "connection": {
                "config": {
                    "type": "CustomDatabase",
                    "sourcePythonClass": "custom_connector.Neo4jConnector"
                }
            }
        }

        response = requests.post(
            f"{self.om_url}/api/v1/services/databaseServices",
            headers=headers,
            json=service_data
        )

        if response.status_code in [200, 201]:
            logger.info(f"Created database service: {service_name}")
            return response.json().get("id")
        else:
            logger.error(f"Failed to create service: {response.text}")
            return None

    def create_om_database(self, service_id: str) -> Optional[str]:
        """Create database entity in OpenMetadata"""
        headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }

        database_name = "graph"
        fqn = f"pms-neo4j.{database_name}"

        # Check if exists
        response = requests.get(
            f"{self.om_url}/api/v1/databases/name/{fqn}",
            headers=headers
        )

        if response.status_code == 200:
            return response.json().get("id")

        # Create
        database_data = {
            "name": database_name,
            "displayName": "Neo4j Graph Database",
            "service": f"pms-neo4j"
        }

        response = requests.post(
            f"{self.om_url}/api/v1/databases",
            headers=headers,
            json=database_data
        )

        if response.status_code in [200, 201]:
            return response.json().get("id")

        logger.error(f"Failed to create database: {response.text}")
        return None

    def create_om_schema(self, database_id: str) -> Optional[str]:
        """Create schema entity in OpenMetadata"""
        headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }

        schema_name = "nodes"
        fqn = f"pms-neo4j.graph.{schema_name}"

        # Check if exists
        response = requests.get(
            f"{self.om_url}/api/v1/databaseSchemas/name/{fqn}",
            headers=headers
        )

        if response.status_code == 200:
            return response.json().get("id")

        # Create
        schema_data = {
            "name": schema_name,
            "displayName": "Neo4j Node Labels",
            "database": "pms-neo4j.graph"
        }

        response = requests.post(
            f"{self.om_url}/api/v1/databaseSchemas",
            headers=headers,
            json=schema_data
        )

        if response.status_code in [200, 201]:
            return response.json().get("id")

        logger.error(f"Failed to create schema: {response.text}")
        return None

    def create_om_table(self, schema: Neo4jNodeSchema) -> bool:
        """Create table entity in OpenMetadata for a Neo4j label"""
        headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }

        table_fqn = f"pms-neo4j.graph.nodes.{schema.label}"

        # Build columns from properties
        columns = [
            {
                "name": prop["name"],
                "dataType": prop["om_type"],
                "description": f"Neo4j property: {prop['name']} (type: {prop['type']})"
            }
            for prop in schema.properties
        ]

        # Build relationship description
        rel_desc = ""
        if schema.relationships:
            rel_desc = "\n\nRelationships:\n" + "\n".join([
                f"- ({schema.label})-[:{r['type']}]->({r['target']})"
                for r in schema.relationships
            ])

        table_data = {
            "name": schema.label,
            "displayName": f"Neo4j:{schema.label}",
            "description": f"Graph node label: {schema.label}\nNode count: {schema.sample_count}{rel_desc}",
            "tableType": "Regular",
            "columns": columns,
            "databaseSchema": "pms-neo4j.graph.nodes",
            "tags": [
                {"tagFQN": "GraphDB.Neo4j", "labelType": "Automated"},
                {"tagFQN": "PMS.GraphRAG", "labelType": "Automated"}
            ]
        }

        # Check if exists
        response = requests.get(
            f"{self.om_url}/api/v1/tables/name/{table_fqn}",
            headers=headers
        )

        if response.status_code == 200:
            # Update existing
            response = requests.patch(
                f"{self.om_url}/api/v1/tables/name/{table_fqn}",
                headers=headers,
                json=[
                    {"op": "replace", "path": "/description", "value": table_data["description"]},
                    {"op": "replace", "path": "/columns", "value": columns}
                ]
            )
        else:
            # Create new
            response = requests.post(
                f"{self.om_url}/api/v1/tables",
                headers=headers,
                json=table_data
            )

        if response.status_code in [200, 201]:
            logger.info(f"Created/updated table: {table_fqn}")
            return True
        else:
            logger.error(f"Failed to create table {schema.label}: {response.text}")
            return False

    def create_lineage(self, schemas: List[Neo4jNodeSchema]):
        """Create lineage edges based on Neo4j relationships"""
        headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }

        for schema in schemas:
            source_fqn = f"pms-neo4j.graph.nodes.{schema.label}"

            for rel in schema.relationships:
                target_label = rel["target"]
                if not target_label:
                    continue

                target_fqn = f"pms-neo4j.graph.nodes.{target_label}"

                lineage_data = {
                    "edge": {
                        "fromEntity": {
                            "type": "table",
                            "fqn": source_fqn
                        },
                        "toEntity": {
                            "type": "table",
                            "fqn": target_fqn
                        },
                        "description": f"Neo4j relationship: [:{rel['type']}]"
                    }
                }

                response = requests.put(
                    f"{self.om_url}/api/v1/lineage",
                    headers=headers,
                    json=lineage_data
                )

                if response.status_code in [200, 201]:
                    logger.info(f"Created lineage: {schema.label} -> {target_label}")
                else:
                    logger.debug(f"Lineage creation note: {response.text}")

    def run_full_ingestion(self):
        """Run complete ingestion process"""
        logger.info("Starting Neo4j metadata ingestion...")

        # 1. Create service hierarchy
        logger.info("Creating OpenMetadata service hierarchy...")
        service_id = self.create_om_database_service()
        if not service_id:
            logger.error("Failed to create database service")
            return False

        database_id = self.create_om_database(service_id)
        if not database_id:
            logger.error("Failed to create database")
            return False

        schema_id = self.create_om_schema(database_id)
        if not schema_id:
            logger.error("Failed to create schema")
            return False

        # 2. Extract Neo4j schemas
        logger.info("Extracting Neo4j node schemas...")
        schemas = self.extract_all_schemas()
        logger.info(f"Found {len(schemas)} node labels")

        # 3. Create tables
        logger.info("Creating OpenMetadata tables...")
        success_count = 0
        for schema in schemas:
            if self.create_om_table(schema):
                success_count += 1

        logger.info(f"Created {success_count}/{len(schemas)} tables")

        # 4. Create lineage
        logger.info("Creating lineage relationships...")
        self.create_lineage(schemas)

        logger.info("Neo4j metadata ingestion completed!")
        return True


def main():
    import argparse

    parser = argparse.ArgumentParser(description="Neo4j Metadata Ingestion for OpenMetadata")
    parser.add_argument("--dry-run", action="store_true", help="Extract schemas without uploading")

    args = parser.parse_args()

    extractor = Neo4jMetadataExtractor()

    try:
        if args.dry_run:
            schemas = extractor.extract_all_schemas()
            print(json.dumps([
                {
                    "label": s.label,
                    "properties": s.properties,
                    "count": s.sample_count,
                    "relationships": s.relationships
                }
                for s in schemas
            ], indent=2))
        else:
            extractor.run_full_ingestion()
    finally:
        extractor.close()


if __name__ == "__main__":
    main()
