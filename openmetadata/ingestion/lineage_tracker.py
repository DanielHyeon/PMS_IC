#!/usr/bin/env python3
"""
PMS OpenMetadata Lineage Tracker

This script provides utilities to track and query data lineage relationships
in the PMS PostgreSQL database through OpenMetadata API.

Usage:
    python lineage_tracker.py --table requirements --direction downstream
    python lineage_tracker.py --table tasks --direction upstream
    python lineage_tracker.py --impact requirements  # Impact analysis
"""

import argparse
import json
import os
import sys
from typing import Optional

import requests

# Configuration
OPENMETADATA_URL = os.getenv("OPENMETADATA_URL", "http://localhost:8585")
SERVICE_NAME = "pms-postgresql"
DATABASE_NAME = "pms_db"


def get_token() -> str:
    """Get ingestion-bot JWT token from environment or database."""
    token = os.getenv("OPENMETADATA_TOKEN")
    if token:
        return token

    # Default token for local development
    # In production, this should be fetched from secure storage
    print("Warning: Using environment variable OPENMETADATA_TOKEN is recommended")
    return ""


def get_headers(token: str) -> dict:
    """Build request headers with authorization."""
    return {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }


def get_table_fqn(schema: str, table: str) -> str:
    """Build fully qualified table name."""
    return f"{SERVICE_NAME}.{DATABASE_NAME}.{schema}.{table}"


def get_lineage(
    table_fqn: str,
    token: str,
    upstream_depth: int = 3,
    downstream_depth: int = 3
) -> Optional[dict]:
    """Fetch lineage information for a table."""
    url = f"{OPENMETADATA_URL}/api/v1/lineage/table/name/{table_fqn}"
    params = {
        "upstreamDepth": upstream_depth,
        "downstreamDepth": downstream_depth
    }

    try:
        resp = requests.get(url, headers=get_headers(token), params=params)
        resp.raise_for_status()
        return resp.json()
    except requests.RequestException as e:
        print(f"Error fetching lineage: {e}")
        return None


def format_lineage(lineage_data: dict, direction: str = "both") -> None:
    """Format and print lineage information."""
    entity = lineage_data.get("entity", {})
    nodes = lineage_data.get("nodes", [])
    upstream_edges = lineage_data.get("upstreamEdges", [])
    downstream_edges = lineage_data.get("downstreamEdges", [])

    print(f"\n{'='*60}")
    print(f"Lineage for: {entity.get('fullyQualifiedName')}")
    print(f"{'='*60}")

    # Build node lookup
    node_map = {entity.get("id"): entity}
    for node in nodes:
        node_map[node.get("id")] = node

    if direction in ["upstream", "both"] and upstream_edges:
        print(f"\n{'-'*30}")
        print("UPSTREAM (Data Sources):")
        print(f"{'-'*30}")
        for edge in upstream_edges:
            from_node = node_map.get(edge["fromEntity"], {})
            to_node = node_map.get(edge["toEntity"], {})
            print(f"  {from_node.get('name', 'unknown')} --> {to_node.get('name', 'unknown')}")

    if direction in ["downstream", "both"] and downstream_edges:
        print(f"\n{'-'*30}")
        print("DOWNSTREAM (Affected Tables):")
        print(f"{'-'*30}")
        for edge in downstream_edges:
            from_node = node_map.get(edge["fromEntity"], {})
            to_node = node_map.get(edge["toEntity"], {})
            print(f"  {from_node.get('name', 'unknown')} --> {to_node.get('name', 'unknown')}")

    print(f"\n{'='*60}")


def impact_analysis(table_fqn: str, token: str) -> None:
    """Perform impact analysis - what would be affected if this table changes."""
    lineage = get_lineage(table_fqn, token, upstream_depth=0, downstream_depth=5)

    if not lineage:
        print("Could not fetch lineage data")
        return

    entity = lineage.get("entity", {})
    nodes = lineage.get("nodes", [])

    print(f"\n{'='*60}")
    print(f"IMPACT ANALYSIS for: {entity.get('name')}")
    print(f"{'='*60}")

    if not nodes:
        print("\nNo downstream dependencies found.")
        print("Changes to this table will not affect other tables.")
    else:
        print(f"\nChanges to '{entity.get('name')}' may affect {len(nodes)} table(s):")
        print()
        for i, node in enumerate(nodes, 1):
            print(f"  {i}. {node.get('fullyQualifiedName')}")
            if node.get("description"):
                print(f"     Description: {node.get('description')[:100]}...")

    print(f"\n{'='*60}")


def trace_requirement_to_task(token: str, requirement_id: Optional[str] = None) -> None:
    """Trace the full path from a requirement to its derived tasks."""
    print("\n" + "="*60)
    print("REQUIREMENT TO TASK TRACING")
    print("="*60)

    # Get requirements table lineage
    req_fqn = get_table_fqn("project", "requirements")
    lineage = get_lineage(req_fqn, token, upstream_depth=0, downstream_depth=3)

    if not lineage:
        print("Could not fetch lineage")
        return

    print("\nData Flow Path:")
    print("-" * 40)
    print("  requirements (project schema)")
    print("       |")
    print("       v [Decomposition]")
    print("  user_stories (task schema)")
    print("       |")
    print("       v [Implementation]")
    print("  tasks (task schema)")
    print()

    # Show all related tables
    nodes = lineage.get("nodes", [])
    print(f"Related Tables: {len(nodes)}")
    for node in nodes:
        print(f"  - {node.get('fullyQualifiedName')}")


# Table schema mapping
TABLE_SCHEMAS = {
    "requirements": "project",
    "projects": "project",
    "phases": "project",
    "deliverables": "project",
    "issues": "project",
    "rfps": "project",
    "user_stories": "task",
    "tasks": "task",
    "sprints": "task",
    "kanban_columns": "task",
    "users": "auth",
    "permissions": "auth",
    "chat_sessions": "chat",
    "chat_messages": "chat",
}


def main():
    parser = argparse.ArgumentParser(description="PMS OpenMetadata Lineage Tracker")
    parser.add_argument("--table", help="Table name to analyze")
    parser.add_argument("--schema", help="Schema name (auto-detected if not provided)")
    parser.add_argument(
        "--direction",
        choices=["upstream", "downstream", "both"],
        default="both",
        help="Lineage direction to show"
    )
    parser.add_argument("--impact", help="Perform impact analysis for a table")
    parser.add_argument("--trace", action="store_true", help="Trace requirement to task flow")
    parser.add_argument("--token", help="OpenMetadata JWT token")
    parser.add_argument("--json", action="store_true", help="Output raw JSON")

    args = parser.parse_args()

    token = args.token or get_token()
    if not token:
        print("Error: OPENMETADATA_TOKEN environment variable or --token required")
        sys.exit(1)

    if args.trace:
        trace_requirement_to_task(token)
        return

    if args.impact:
        table = args.impact
        schema = args.schema or TABLE_SCHEMAS.get(table)
        if not schema:
            print(f"Error: Unknown table '{table}'. Please specify --schema")
            sys.exit(1)
        table_fqn = get_table_fqn(schema, table)
        impact_analysis(table_fqn, token)
        return

    if args.table:
        table = args.table
        schema = args.schema or TABLE_SCHEMAS.get(table)
        if not schema:
            print(f"Error: Unknown table '{table}'. Please specify --schema")
            sys.exit(1)

        table_fqn = get_table_fqn(schema, table)
        lineage = get_lineage(table_fqn, token)

        if lineage:
            if args.json:
                print(json.dumps(lineage, indent=2))
            else:
                format_lineage(lineage, args.direction)
        return

    parser.print_help()


if __name__ == "__main__":
    main()
