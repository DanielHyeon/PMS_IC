"""
Redis Streams Consumer for Lineage Events

Consumes lineage events from Redis Streams and synchronizes to:
1. Neo4j - Creates/updates nodes and relationships
2. OpenMetadata - Creates/updates lineage edges

Usage:
    python lineage_consumer.py --consumer neo4j
    python lineage_consumer.py --consumer openmetadata
    python lineage_consumer.py --consumer all

Environment Variables:
    REDIS_HOST: Redis host (default: localhost)
    REDIS_PORT: Redis port (default: 6379)
    NEO4J_URI: Neo4j connection URI
    OPENMETADATA_URL: OpenMetadata API URL
"""

import os
import sys
import json
import time
import signal
import logging
import hashlib
from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Any
from dataclasses import dataclass
from datetime import datetime

import redis

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class LineageEvent:
    """Parsed lineage event from Redis Stream"""
    id: str
    event_type: str
    aggregate_type: str
    aggregate_id: str
    payload: Dict[str, Any]
    idempotency_key: str
    created_at: str
    stream_id: str  # Redis stream message ID


class BaseLineageConsumer(ABC):
    """Base class for lineage event consumers"""

    def __init__(
        self,
        redis_host: str = None,
        redis_port: int = None,
        stream_name: str = "lineage:events",
        group_name: str = None,
        consumer_name: str = None
    ):
        self.redis_host = redis_host or os.getenv("REDIS_HOST", "localhost")
        self.redis_port = redis_port or int(os.getenv("REDIS_PORT", 6379))
        self.stream_name = stream_name
        self.group_name = group_name
        self.consumer_name = consumer_name or f"{group_name}-{os.getpid()}"

        self.redis_client = redis.Redis(
            host=self.redis_host,
            port=self.redis_port,
            decode_responses=True
        )

        self.running = True
        self.processed_keys: set = set()  # In-memory idempotency cache

        # Graceful shutdown
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)

    def _signal_handler(self, signum, frame):
        logger.info(f"Received signal {signum}, shutting down...")
        self.running = False

    def ensure_consumer_group(self):
        """Create consumer group if not exists"""
        try:
            self.redis_client.xgroup_create(
                self.stream_name,
                self.group_name,
                id='0',
                mkstream=True
            )
            logger.info(f"Created consumer group: {self.group_name}")
        except redis.ResponseError as e:
            if "BUSYGROUP" in str(e):
                logger.debug(f"Consumer group {self.group_name} already exists")
            else:
                raise

    def parse_event(self, stream_id: str, data: Dict[str, str]) -> Optional[LineageEvent]:
        """Parse raw Redis message to LineageEvent"""
        try:
            # Skip init message
            if data.get("init") == "true":
                return None

            payload_str = data.get("payload", "{}")
            payload = json.loads(payload_str) if payload_str else {}

            return LineageEvent(
                id=data.get("id", ""),
                event_type=data.get("eventType", ""),
                aggregate_type=data.get("aggregateType", ""),
                aggregate_id=data.get("aggregateId", ""),
                payload=payload,
                idempotency_key=data.get("idempotencyKey", ""),
                created_at=data.get("createdAt", ""),
                stream_id=stream_id
            )
        except Exception as e:
            logger.error(f"Failed to parse event: {e}")
            return None

    def is_duplicate(self, event: LineageEvent) -> bool:
        """Check if event was already processed (idempotency)"""
        if not event.idempotency_key:
            return False

        if event.idempotency_key in self.processed_keys:
            return True

        # Could also check persistent storage here
        return False

    def mark_processed(self, event: LineageEvent):
        """Mark event as processed for idempotency"""
        if event.idempotency_key:
            self.processed_keys.add(event.idempotency_key)
            # Limit memory usage
            if len(self.processed_keys) > 10000:
                # Remove oldest entries (simple approach)
                self.processed_keys = set(list(self.processed_keys)[-5000:])

    def acknowledge(self, stream_id: str):
        """Acknowledge message to Redis Stream"""
        self.redis_client.xack(self.stream_name, self.group_name, stream_id)

    @abstractmethod
    def process_event(self, event: LineageEvent) -> bool:
        """Process a single event. Return True on success."""
        pass

    def run(self, batch_size: int = 10, block_ms: int = 5000):
        """Main consumer loop"""
        logger.info(f"Starting {self.group_name} consumer: {self.consumer_name}")
        self.ensure_consumer_group()

        # First, process pending messages (from previous crash)
        self._process_pending_messages()

        while self.running:
            try:
                # Read new messages
                messages = self.redis_client.xreadgroup(
                    groupname=self.group_name,
                    consumername=self.consumer_name,
                    streams={self.stream_name: '>'},
                    count=batch_size,
                    block=block_ms
                )

                if not messages:
                    continue

                for stream, stream_messages in messages:
                    for stream_id, data in stream_messages:
                        self._handle_message(stream_id, data)

            except redis.ConnectionError as e:
                logger.error(f"Redis connection error: {e}")
                time.sleep(5)
            except Exception as e:
                logger.error(f"Consumer error: {e}")
                time.sleep(1)

        logger.info(f"Consumer {self.consumer_name} stopped")

    def _process_pending_messages(self):
        """Process any pending messages from previous run"""
        logger.info("Checking for pending messages...")

        try:
            pending = self.redis_client.xpending_range(
                self.stream_name,
                self.group_name,
                min='-',
                max='+',
                count=100,
                consumername=self.consumer_name
            )

            if pending:
                logger.info(f"Found {len(pending)} pending messages to reprocess")

                for entry in pending:
                    stream_id = entry['message_id']
                    # Re-read the message
                    messages = self.redis_client.xrange(
                        self.stream_name,
                        min=stream_id,
                        max=stream_id
                    )
                    if messages:
                        _, data = messages[0]
                        self._handle_message(stream_id, data)

        except Exception as e:
            logger.error(f"Error processing pending messages: {e}")

    def _handle_message(self, stream_id: str, data: Dict[str, str]):
        """Handle a single message"""
        event = self.parse_event(stream_id, data)

        if event is None:
            # Init message or parse error - just acknowledge
            self.acknowledge(stream_id)
            return

        if self.is_duplicate(event):
            logger.debug(f"Skipping duplicate event: {event.idempotency_key}")
            self.acknowledge(stream_id)
            return

        try:
            success = self.process_event(event)
            if success:
                self.mark_processed(event)
                self.acknowledge(stream_id)
                logger.debug(f"Processed event: {event.event_type} - {event.aggregate_id}")
            else:
                logger.warning(f"Event processing returned False: {event.id}")
                # Don't acknowledge - will be retried
        except Exception as e:
            logger.error(f"Error processing event {event.id}: {e}")
            # Don't acknowledge - will be retried


class Neo4jLineageConsumer(BaseLineageConsumer):
    """Consumer that syncs lineage events to Neo4j"""

    def __init__(self, **kwargs):
        super().__init__(group_name="neo4j-sync", **kwargs)

        try:
            from neo4j import GraphDatabase
            self.driver = GraphDatabase.driver(
                os.getenv("NEO4J_URI", "bolt://localhost:7687"),
                auth=(
                    os.getenv("NEO4J_USER", "neo4j"),
                    os.getenv("NEO4J_PASSWORD", "pmspassword123")
                )
            )
            logger.info("Connected to Neo4j")
        except ImportError:
            logger.error("neo4j driver not installed. Run: pip install neo4j")
            sys.exit(1)

    def process_event(self, event: LineageEvent) -> bool:
        """Process lineage event in Neo4j"""
        event_type = event.event_type
        payload = event.payload

        try:
            with self.driver.session() as session:
                if event_type == "REQUIREMENT_CREATED":
                    return self._create_requirement_node(session, payload)

                elif event_type == "STORY_CREATED":
                    return self._create_story_node(session, payload)

                elif event_type == "TASK_CREATED":
                    return self._create_task_node(session, payload)

                elif event_type == "REQUIREMENT_STORY_LINKED":
                    return self._create_derives_relationship(session, payload)

                elif event_type == "REQUIREMENT_STORY_UNLINKED":
                    return self._delete_derives_relationship(session, payload)

                elif event_type == "STORY_TASK_LINKED":
                    return self._create_breaks_down_relationship(session, payload)

                elif event_type == "STORY_TASK_UNLINKED":
                    return self._delete_breaks_down_relationship(session, payload)

                elif event_type == "REQUIREMENT_TASK_LINKED":
                    return self._create_implemented_by_relationship(session, payload)

                elif event_type == "REQUIREMENT_TASK_UNLINKED":
                    return self._delete_implemented_by_relationship(session, payload)

                elif event_type.endswith("_DELETED"):
                    return self._delete_node(session, event.aggregate_type, payload)

                elif event_type.endswith("_UPDATED"):
                    return self._update_node(session, event.aggregate_type, payload)

                else:
                    logger.debug(f"Unhandled event type: {event_type}")
                    return True  # Acknowledge anyway

        except Exception as e:
            logger.error(f"Neo4j error: {e}")
            return False

    def _create_requirement_node(self, session, payload: Dict) -> bool:
        query = """
        MERGE (r:Requirement {id: $id})
        SET r.code = $code,
            r.title = $title,
            r.projectId = $projectId,
            r.updatedAt = datetime()
        RETURN r
        """
        session.run(
            query,
            id=payload.get("requirementId"),
            code=payload.get("code", ""),
            title=payload.get("title", ""),
            projectId=payload.get("projectId", "")
        )
        return True

    def _create_story_node(self, session, payload: Dict) -> bool:
        query = """
        MERGE (s:UserStory {id: $id})
        SET s.title = $title,
            s.projectId = $projectId,
            s.storyPoints = $storyPoints,
            s.updatedAt = datetime()
        RETURN s
        """
        session.run(
            query,
            id=payload.get("storyId"),
            title=payload.get("title", ""),
            projectId=payload.get("projectId", ""),
            storyPoints=payload.get("storyPoints")
        )
        return True

    def _create_task_node(self, session, payload: Dict) -> bool:
        query = """
        MERGE (t:Task {id: $id})
        SET t.title = $title,
            t.projectId = $projectId,
            t.updatedAt = datetime()
        RETURN t
        """
        session.run(
            query,
            id=payload.get("taskId"),
            title=payload.get("title", ""),
            projectId=payload.get("projectId", "")
        )
        return True

    def _create_derives_relationship(self, session, payload: Dict) -> bool:
        """Create DERIVES relationship: Requirement -> UserStory"""
        query = """
        MATCH (r:Requirement {id: $sourceId})
        MATCH (s:UserStory {id: $targetId})
        MERGE (r)-[rel:DERIVES]->(s)
        SET rel.createdAt = datetime()
        RETURN rel
        """
        session.run(
            query,
            sourceId=payload.get("sourceId"),
            targetId=payload.get("targetId")
        )
        logger.info(f"Created DERIVES: {payload.get('sourceId')} -> {payload.get('targetId')}")
        return True

    def _delete_derives_relationship(self, session, payload: Dict) -> bool:
        query = """
        MATCH (r:Requirement {id: $sourceId})-[rel:DERIVES]->(s:UserStory {id: $targetId})
        DELETE rel
        """
        session.run(
            query,
            sourceId=payload.get("sourceId"),
            targetId=payload.get("targetId")
        )
        return True

    def _create_breaks_down_relationship(self, session, payload: Dict) -> bool:
        """Create BREAKS_DOWN_TO relationship: UserStory -> Task"""
        query = """
        MATCH (s:UserStory {id: $sourceId})
        MATCH (t:Task {id: $targetId})
        MERGE (s)-[rel:BREAKS_DOWN_TO]->(t)
        SET rel.createdAt = datetime()
        RETURN rel
        """
        session.run(
            query,
            sourceId=payload.get("sourceId"),
            targetId=payload.get("targetId")
        )
        logger.info(f"Created BREAKS_DOWN_TO: {payload.get('sourceId')} -> {payload.get('targetId')}")
        return True

    def _delete_breaks_down_relationship(self, session, payload: Dict) -> bool:
        query = """
        MATCH (s:UserStory {id: $sourceId})-[rel:BREAKS_DOWN_TO]->(t:Task {id: $targetId})
        DELETE rel
        """
        session.run(
            query,
            sourceId=payload.get("sourceId"),
            targetId=payload.get("targetId")
        )
        return True

    def _create_implemented_by_relationship(self, session, payload: Dict) -> bool:
        """Create IMPLEMENTED_BY relationship: Requirement -> Task"""
        query = """
        MATCH (r:Requirement {id: $sourceId})
        MATCH (t:Task {id: $targetId})
        MERGE (r)-[rel:IMPLEMENTED_BY]->(t)
        SET rel.createdAt = datetime()
        RETURN rel
        """
        session.run(
            query,
            sourceId=payload.get("sourceId"),
            targetId=payload.get("targetId")
        )
        return True

    def _delete_implemented_by_relationship(self, session, payload: Dict) -> bool:
        query = """
        MATCH (r:Requirement {id: $sourceId})-[rel:IMPLEMENTED_BY]->(t:Task {id: $targetId})
        DELETE rel
        """
        session.run(
            query,
            sourceId=payload.get("sourceId"),
            targetId=payload.get("targetId")
        )
        return True

    def _delete_node(self, session, aggregate_type: str, payload: Dict) -> bool:
        label = {
            "REQUIREMENT": "Requirement",
            "USER_STORY": "UserStory",
            "TASK": "Task",
            "SPRINT": "Sprint"
        }.get(aggregate_type, aggregate_type)

        id_key = {
            "REQUIREMENT": "requirementId",
            "USER_STORY": "storyId",
            "TASK": "taskId",
            "SPRINT": "sprintId"
        }.get(aggregate_type, "id")

        query = f"""
        MATCH (n:{label} {{id: $id}})
        DETACH DELETE n
        """
        session.run(query, id=payload.get(id_key))
        return True

    def _update_node(self, session, aggregate_type: str, payload: Dict) -> bool:
        # For updates, just ensure node exists with latest data
        # The actual update logic can be extended based on requirements
        return True

    def close(self):
        if hasattr(self, 'driver') and self.driver:
            self.driver.close()


class OpenMetadataLineageConsumer(BaseLineageConsumer):
    """Consumer that syncs lineage events to OpenMetadata"""

    def __init__(self, **kwargs):
        super().__init__(group_name="openmetadata-sync", **kwargs)

        import requests
        self.requests = requests
        self.om_url = os.getenv("OPENMETADATA_URL", "http://localhost:8585")
        self.om_token = os.getenv("OM_JWT_TOKEN", "")

        # Entity type to FQN prefix mapping
        self.fqn_prefix = {
            "REQUIREMENT": "pms-postgres.pms_db.project.requirements",
            "USER_STORY": "pms-postgres.pms_db.task.user_stories",
            "TASK": "pms-postgres.pms_db.task.tasks",
            "SPRINT": "pms-postgres.pms_db.task.sprints"
        }

    def process_event(self, event: LineageEvent) -> bool:
        """Process lineage event in OpenMetadata"""
        event_type = event.event_type
        payload = event.payload

        try:
            # Only handle lineage relationship events
            if event_type in ["REQUIREMENT_STORY_LINKED", "STORY_TASK_LINKED", "REQUIREMENT_TASK_LINKED"]:
                return self._create_lineage_edge(payload)

            elif event_type in ["REQUIREMENT_STORY_UNLINKED", "STORY_TASK_UNLINKED", "REQUIREMENT_TASK_UNLINKED"]:
                return self._delete_lineage_edge(payload)

            else:
                # Other events don't need OpenMetadata sync
                return True

        except Exception as e:
            logger.error(f"OpenMetadata error: {e}")
            return False

    def _get_table_fqn(self, entity_type: str, entity_id: str) -> str:
        """Get OpenMetadata table FQN for an entity"""
        prefix = self.fqn_prefix.get(entity_type, "")
        return f"{prefix}.{entity_id}" if prefix else ""

    def _create_lineage_edge(self, payload: Dict) -> bool:
        """Create lineage edge in OpenMetadata"""
        source_type = payload.get("sourceType")
        source_id = payload.get("sourceId")
        target_type = payload.get("targetType")
        target_id = payload.get("targetId")
        relationship_type = payload.get("relationshipType", "")

        source_fqn = self._get_table_fqn(source_type, source_id)
        target_fqn = self._get_table_fqn(target_type, target_id)

        if not source_fqn or not target_fqn:
            logger.warning(f"Cannot create lineage: missing FQN for {source_type} or {target_type}")
            return True  # Don't retry

        headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }

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
                "description": f"PMS Lineage: {relationship_type}"
            }
        }

        response = self.requests.put(
            f"{self.om_url}/api/v1/lineage",
            headers=headers,
            json=lineage_data
        )

        if response.status_code in [200, 201]:
            logger.info(f"Created OM lineage: {source_fqn} -> {target_fqn}")
            return True
        else:
            logger.warning(f"Failed to create OM lineage: {response.status_code} - {response.text}")
            return response.status_code == 404  # Don't retry 404 (entity not in OM)

    def _delete_lineage_edge(self, payload: Dict) -> bool:
        """Delete lineage edge in OpenMetadata"""
        # OpenMetadata lineage deletion requires different API
        # For now, just acknowledge
        logger.debug("OpenMetadata lineage deletion not implemented yet")
        return True


def main():
    import argparse

    parser = argparse.ArgumentParser(description="Lineage Event Consumer")
    parser.add_argument(
        "--consumer",
        choices=["neo4j", "openmetadata", "all"],
        default="all",
        help="Which consumer to run"
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=10,
        help="Number of messages to read per batch"
    )

    args = parser.parse_args()

    consumers = []

    if args.consumer in ["neo4j", "all"]:
        consumers.append(Neo4jLineageConsumer())

    if args.consumer in ["openmetadata", "all"]:
        consumers.append(OpenMetadataLineageConsumer())

    if not consumers:
        logger.error("No consumers configured")
        return

    # For simplicity, run consumers sequentially
    # In production, use multiprocessing or separate processes
    if len(consumers) == 1:
        consumers[0].run(batch_size=args.batch_size)
    else:
        import multiprocessing
        processes = []
        for consumer in consumers:
            p = multiprocessing.Process(
                target=consumer.run,
                kwargs={"batch_size": args.batch_size}
            )
            p.start()
            processes.append(p)

        for p in processes:
            p.join()


if __name__ == "__main__":
    main()
