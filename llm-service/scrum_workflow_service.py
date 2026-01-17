"""
Scrum Workflow Service for PMS-IC

This service manages the complete Scrum workflow integration:
1. Requirement -> UserStory -> Task lineage tracking via OpenMetadata
2. Sprint <-> UserStory <-> Task relationships
3. Automatic metadata synchronization between PostgreSQL and OpenMetadata
4. Business glossary management

Architecture:
    PostgreSQL (Master Data) -> OpenMetadata (Metadata Catalog + Lineage)
                             -> Neo4j (GraphRAG + Advanced Analysis)

Usage:
    from scrum_workflow_service import ScrumWorkflowService

    service = ScrumWorkflowService()

    # Create requirement and track lineage
    service.create_requirement_with_lineage(project_id, requirement_data)

    # Link requirement to user story
    service.link_requirement_to_story(requirement_id, story_id)

    # Break down story to tasks
    service.create_task_from_story(story_id, task_data)

    # Get full traceability
    service.get_requirement_impact_analysis(requirement_id)
"""

import os
import logging
import json
import requests
import psycopg2
from psycopg2.extras import RealDictCursor
from typing import Dict, List, Optional, Any
from dataclasses import dataclass, asdict
from datetime import datetime
from enum import Enum

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# ============================================
# Data Classes
# ============================================

class RequirementStatus(Enum):
    IDENTIFIED = "IDENTIFIED"
    ANALYZED = "ANALYZED"
    APPROVED = "APPROVED"
    IMPLEMENTED = "IMPLEMENTED"
    VERIFIED = "VERIFIED"
    DEFERRED = "DEFERRED"
    REJECTED = "REJECTED"


class StoryStatus(Enum):
    BACKLOG = "BACKLOG"
    SELECTED = "SELECTED"
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"
    CANCELLED = "CANCELLED"


class TaskStatus(Enum):
    TODO = "TODO"
    IN_PROGRESS = "IN_PROGRESS"
    REVIEW = "REVIEW"
    DONE = "DONE"


@dataclass
class RequirementLineage:
    """Tracks lineage from Requirement through Stories to Tasks"""
    requirement_id: str
    requirement_code: str
    requirement_title: str
    stories: List[Dict]  # {story_id, title, status, tasks: [...]}
    total_story_points: int
    completion_percentage: float


@dataclass
class SprintWorkflow:
    """Complete sprint workflow state"""
    sprint_id: str
    sprint_name: str
    status: str
    start_date: str
    end_date: str
    stories: List[Dict]
    requirements_covered: List[str]
    total_story_points: int
    completed_story_points: int


# ============================================
# OpenMetadata Integration
# ============================================

class OpenMetadataClient:
    """Client for OpenMetadata API interactions"""

    def __init__(self):
        self.base_url = os.getenv("OPENMETADATA_URL", "http://localhost:8585")
        self.token = os.getenv("OM_JWT_TOKEN", "")
        self.headers = {
            "Authorization": f"Bearer {self.token}",
            "Content-Type": "application/json"
        }

    def _request(self, method: str, endpoint: str, data: dict = None) -> Optional[dict]:
        """Make API request to OpenMetadata"""
        url = f"{self.base_url}/api/v1/{endpoint}"

        try:
            if method == "GET":
                response = requests.get(url, headers=self.headers)
            elif method == "POST":
                response = requests.post(url, headers=self.headers, json=data)
            elif method == "PUT":
                response = requests.put(url, headers=self.headers, json=data)
            elif method == "PATCH":
                response = requests.patch(url, headers=self.headers, json=data)
            elif method == "DELETE":
                response = requests.delete(url, headers=self.headers)
            else:
                return None

            if response.status_code in [200, 201]:
                return response.json() if response.text else {}
            else:
                logger.warning(f"API request failed: {response.status_code} - {response.text}")
                return None

        except Exception as e:
            logger.error(f"API request error: {e}")
            return None

    # =========================================
    # Tag Management
    # =========================================

    def ensure_classification(self, name: str, description: str) -> bool:
        """Ensure a tag classification exists"""
        data = {
            "name": name,
            "displayName": name,
            "description": description
        }
        result = self._request("PUT", "classifications", data)
        return result is not None

    def create_tag(self, classification: str, name: str, description: str) -> bool:
        """Create a tag under a classification"""
        data = {
            "name": name,
            "displayName": name,
            "description": description,
            "classification": classification
        }
        result = self._request("PUT", "tags", data)
        return result is not None

    def tag_entity(self, entity_type: str, entity_fqn: str, tag_fqn: str) -> bool:
        """Add a tag to an entity"""
        data = [{
            "tagFQN": tag_fqn,
            "labelType": "Manual",
            "state": "Confirmed"
        }]
        result = self._request("PUT", f"{entity_type}/name/{entity_fqn}/tags", data)
        return result is not None

    # =========================================
    # Lineage Management
    # =========================================

    def create_lineage(
        self,
        from_entity_type: str,
        from_fqn: str,
        to_entity_type: str,
        to_fqn: str,
        description: str = ""
    ) -> bool:
        """Create lineage edge between two entities"""
        data = {
            "edge": {
                "fromEntity": {
                    "type": from_entity_type,
                    "fqn": from_fqn
                },
                "toEntity": {
                    "type": to_entity_type,
                    "fqn": to_fqn
                },
                "description": description
            }
        }
        result = self._request("PUT", "lineage", data)
        return result is not None

    def get_lineage(self, entity_type: str, entity_fqn: str, direction: str = "both") -> Optional[dict]:
        """Get lineage for an entity"""
        result = self._request(
            "GET",
            f"lineage/{entity_type}/name/{entity_fqn}?upstreamDepth=3&downstreamDepth=3"
        )
        return result

    # =========================================
    # Custom Properties
    # =========================================

    def set_custom_properties(self, entity_type: str, entity_fqn: str, properties: dict) -> bool:
        """Set custom properties on an entity"""
        data = [{"op": "add", "path": "/extension", "value": properties}]
        result = self._request("PATCH", f"{entity_type}/name/{entity_fqn}", data)
        return result is not None

    # =========================================
    # Glossary Management
    # =========================================

    def ensure_glossary(self, name: str, description: str) -> Optional[str]:
        """Ensure a glossary exists, return its ID"""
        # Check if exists
        result = self._request("GET", f"glossaries/name/{name}")
        if result:
            return result.get("id")

        # Create new
        data = {
            "name": name,
            "displayName": name.replace("_", " "),
            "description": description
        }
        result = self._request("POST", "glossaries", data)
        return result.get("id") if result else None

    def create_glossary_term(
        self,
        glossary_name: str,
        term_name: str,
        description: str,
        synonyms: List[str] = None
    ) -> bool:
        """Create a glossary term"""
        data = {
            "name": term_name,
            "displayName": term_name,
            "description": description,
            "synonyms": synonyms or [],
            "glossary": glossary_name
        }
        result = self._request("PUT", "glossaryTerms", data)
        return result is not None


# ============================================
# Scrum Workflow Service
# ============================================

class ScrumWorkflowService:
    """
    Main service for Scrum workflow management with OpenMetadata integration.

    Provides:
    - Requirement to UserStory to Task lineage tracking
    - Sprint workflow state management
    - Impact analysis for requirement changes
    - Business glossary synchronization
    """

    def __init__(self):
        # PostgreSQL connection
        self.pg_conn = psycopg2.connect(
            host=os.getenv("POSTGRES_HOST", "localhost"),
            port=os.getenv("POSTGRES_PORT", "5433"),
            database=os.getenv("POSTGRES_DB", "pms_db"),
            user=os.getenv("POSTGRES_USER", "pms_user"),
            password=os.getenv("POSTGRES_PASSWORD", "pms_password")
        )

        # OpenMetadata client
        self.om = OpenMetadataClient()

        # Table FQN prefix
        self.table_prefix = "pms-postgres.pms_db"

        # Initialize classifications
        self._init_classifications()

    def _init_classifications(self):
        """Initialize OpenMetadata classifications for Scrum"""
        classifications = [
            ("PMS", "PMS Insurance Claims project metadata"),
            ("Scrum", "Scrum framework entities"),
            ("Sprint", "Sprint-related tags"),
            ("Priority", "Priority levels"),
            ("Status", "Workflow status tags")
        ]

        for name, desc in classifications:
            self.om.ensure_classification(name, desc)

    def close(self):
        """Close connections"""
        if self.pg_conn:
            self.pg_conn.close()

    # =========================================
    # Requirement Management
    # =========================================

    def get_requirement(self, requirement_id: str) -> Optional[dict]:
        """Get requirement from PostgreSQL"""
        cursor = self.pg_conn.cursor(cursor_factory=RealDictCursor)
        cursor.execute("""
            SELECT r.*, rfp.title as rfp_title
            FROM project.requirements r
            LEFT JOIN project.rfps rfp ON r.rfp_id = rfp.id
            WHERE r.id = %s
        """, (requirement_id,))
        return cursor.fetchone()

    def sync_requirement_to_om(self, requirement_id: str) -> bool:
        """Sync requirement metadata to OpenMetadata"""
        req = self.get_requirement(requirement_id)
        if not req:
            return False

        table_fqn = f"{self.table_prefix}.project.requirements"

        # Create requirement tag
        tag_name = f"REQ_{req['code']}"
        self.om.create_tag("Scrum", tag_name, req['title'])

        # Tag the requirements table
        self.om.tag_entity("tables", table_fqn, f"Scrum.{tag_name}")

        # Set custom properties
        properties = {
            "requirement_id": requirement_id,
            "requirement_code": req['code'],
            "category": req.get('category'),
            "priority": req.get('priority'),
            "status": req.get('status'),
            "assignee_id": req.get('assignee_id'),
            "synced_at": datetime.now().isoformat()
        }
        self.om.set_custom_properties("tables", table_fqn, properties)

        logger.info(f"Synced requirement {req['code']} to OpenMetadata")
        return True

    # =========================================
    # UserStory Management
    # =========================================

    def get_user_story(self, story_id: str) -> Optional[dict]:
        """Get user story from PostgreSQL"""
        cursor = self.pg_conn.cursor(cursor_factory=RealDictCursor)
        cursor.execute("""
            SELECT us.*, s.name as sprint_name, s.status as sprint_status
            FROM task.user_stories us
            LEFT JOIN task.sprints s ON us.sprint_id = s.id
            WHERE us.id = %s
        """, (story_id,))
        return cursor.fetchone()

    def get_stories_by_sprint(self, sprint_id: str) -> List[dict]:
        """Get all stories in a sprint"""
        cursor = self.pg_conn.cursor(cursor_factory=RealDictCursor)
        cursor.execute("""
            SELECT *
            FROM task.user_stories
            WHERE sprint_id = %s
            ORDER BY priority_order
        """, (sprint_id,))
        return cursor.fetchall()

    # =========================================
    # Lineage Management
    # =========================================

    def link_requirement_to_story(
        self,
        requirement_id: str,
        story_id: str,
        create_lineage: bool = True
    ) -> bool:
        """
        Link a requirement to a user story and create lineage.

        This creates the traceability:
        Requirement -> UserStory (DERIVES relationship)
        """
        # Get entities
        req = self.get_requirement(requirement_id)
        story = self.get_user_story(story_id)

        if not req or not story:
            logger.error("Requirement or Story not found")
            return False

        # Store mapping in PostgreSQL (using existing requirement_sprint_mapping or new table)
        cursor = self.pg_conn.cursor()

        # Check if we need to create a mapping table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS project.requirement_story_mapping (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                requirement_id VARCHAR(36) NOT NULL,
                story_id VARCHAR(50) NOT NULL,
                mapped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                mapped_by VARCHAR(36),
                UNIQUE(requirement_id, story_id)
            )
        """)

        # Insert mapping
        try:
            cursor.execute("""
                INSERT INTO project.requirement_story_mapping (requirement_id, story_id)
                VALUES (%s, %s)
                ON CONFLICT (requirement_id, story_id) DO NOTHING
            """, (requirement_id, story_id))
            self.pg_conn.commit()
        except Exception as e:
            logger.error(f"Failed to create mapping: {e}")
            self.pg_conn.rollback()
            return False

        # Create lineage in OpenMetadata
        if create_lineage:
            req_table_fqn = f"{self.table_prefix}.project.requirements"
            story_table_fqn = f"{self.table_prefix}.task.user_stories"

            self.om.create_lineage(
                from_entity_type="table",
                from_fqn=req_table_fqn,
                to_entity_type="table",
                to_fqn=story_table_fqn,
                description=f"Requirement {req['code']} derives UserStory: {story['title']}"
            )

            # Tag story with requirement code
            tag_name = f"REQ_{req['code']}"
            self.om.tag_entity("tables", story_table_fqn, f"Scrum.{tag_name}")

        logger.info(f"Linked requirement {req['code']} to story {story['title']}")
        return True

    def link_story_to_task(self, story_id: str, task_id: str) -> bool:
        """
        Link a user story to a task and create lineage.

        This creates the traceability:
        UserStory -> Task (BREAKS_DOWN_TO relationship)
        """
        story = self.get_user_story(story_id)

        if not story:
            logger.error("Story not found")
            return False

        # Update task with story_id (requires schema change)
        cursor = self.pg_conn.cursor()

        # Add story_id column if not exists
        try:
            cursor.execute("""
                ALTER TABLE task.tasks
                ADD COLUMN IF NOT EXISTS user_story_id VARCHAR(50),
                ADD COLUMN IF NOT EXISTS sprint_id VARCHAR(50)
            """)

            cursor.execute("""
                UPDATE task.tasks
                SET user_story_id = %s, sprint_id = %s
                WHERE id = %s
            """, (story_id, story.get('sprint_id'), task_id))

            self.pg_conn.commit()
        except Exception as e:
            logger.error(f"Failed to link task: {e}")
            self.pg_conn.rollback()
            return False

        # Create lineage in OpenMetadata
        story_table_fqn = f"{self.table_prefix}.task.user_stories"
        task_table_fqn = f"{self.table_prefix}.task.tasks"

        self.om.create_lineage(
            from_entity_type="table",
            from_fqn=story_table_fqn,
            to_entity_type="table",
            to_fqn=task_table_fqn,
            description=f"Story '{story['title']}' breaks down to Task"
        )

        logger.info(f"Linked story {story_id} to task {task_id}")
        return True

    # =========================================
    # Impact Analysis
    # =========================================

    def get_requirement_lineage(self, requirement_id: str) -> Optional[RequirementLineage]:
        """
        Get complete lineage from Requirement -> Stories -> Tasks

        Returns:
            RequirementLineage with all downstream entities
        """
        req = self.get_requirement(requirement_id)
        if not req:
            return None

        cursor = self.pg_conn.cursor(cursor_factory=RealDictCursor)

        # Get linked stories
        cursor.execute("""
            SELECT us.*, rsm.mapped_at
            FROM project.requirement_story_mapping rsm
            JOIN task.user_stories us ON rsm.story_id = us.id
            WHERE rsm.requirement_id = %s
        """, (requirement_id,))
        stories_raw = cursor.fetchall()

        stories = []
        total_points = 0
        completed_points = 0

        for story in stories_raw:
            # Get tasks for each story
            cursor.execute("""
                SELECT t.*, kc.name as column_name
                FROM task.tasks t
                JOIN task.kanban_columns kc ON t.column_id = kc.id
                WHERE t.user_story_id = %s
                ORDER BY t.order_num
            """, (story['id'],))
            tasks = cursor.fetchall()

            story_data = {
                "story_id": story['id'],
                "title": story['title'],
                "status": story['status'],
                "story_points": story.get('story_points', 0),
                "sprint_id": story.get('sprint_id'),
                "tasks": [
                    {
                        "task_id": t['id'],
                        "title": t['title'],
                        "status": t['status'],
                        "column": t['column_name'],
                        "assignee_id": t.get('assignee_id')
                    }
                    for t in tasks
                ]
            }
            stories.append(story_data)

            points = story.get('story_points') or 0
            total_points += points
            if story['status'] == 'COMPLETED':
                completed_points += points

        completion_pct = (completed_points / total_points * 100) if total_points > 0 else 0

        return RequirementLineage(
            requirement_id=requirement_id,
            requirement_code=req['code'],
            requirement_title=req['title'],
            stories=stories,
            total_story_points=total_points,
            completion_percentage=round(completion_pct, 1)
        )

    def get_requirement_impact_analysis(self, requirement_id: str) -> dict:
        """
        Analyze the impact of changing a requirement.

        Returns:
            Impact analysis including affected stories, tasks, and sprints
        """
        lineage = self.get_requirement_lineage(requirement_id)
        if not lineage:
            return {"error": "Requirement not found"}

        # Collect affected entities
        affected_sprints = set()
        affected_assignees = set()
        in_progress_tasks = []

        for story in lineage.stories:
            if story.get('sprint_id'):
                affected_sprints.add(story['sprint_id'])

            for task in story.get('tasks', []):
                if task.get('assignee_id'):
                    affected_assignees.add(task['assignee_id'])

                if task['status'] in ['IN_PROGRESS', 'REVIEW']:
                    in_progress_tasks.append({
                        "task_id": task['task_id'],
                        "title": task['title'],
                        "status": task['status']
                    })

        return {
            "requirement": {
                "id": lineage.requirement_id,
                "code": lineage.requirement_code,
                "title": lineage.requirement_title
            },
            "impact_summary": {
                "total_stories": len(lineage.stories),
                "total_story_points": lineage.total_story_points,
                "completion_percentage": lineage.completion_percentage,
                "affected_sprints": list(affected_sprints),
                "affected_assignees": list(affected_assignees),
                "in_progress_tasks": in_progress_tasks
            },
            "risk_level": self._calculate_risk_level(lineage, in_progress_tasks),
            "lineage": asdict(lineage)
        }

    def _calculate_risk_level(
        self,
        lineage: RequirementLineage,
        in_progress_tasks: List[dict]
    ) -> str:
        """Calculate risk level for requirement change"""
        if len(in_progress_tasks) > 3:
            return "HIGH"
        elif lineage.completion_percentage > 50:
            return "MEDIUM"
        elif len(lineage.stories) > 0:
            return "LOW"
        else:
            return "NONE"

    # =========================================
    # Sprint Workflow
    # =========================================

    def get_sprint_workflow(self, sprint_id: str) -> Optional[SprintWorkflow]:
        """Get complete sprint workflow state"""
        cursor = self.pg_conn.cursor(cursor_factory=RealDictCursor)

        # Get sprint
        cursor.execute("""
            SELECT * FROM task.sprints WHERE id = %s
        """, (sprint_id,))
        sprint = cursor.fetchone()

        if not sprint:
            return None

        # Get stories in sprint
        stories = self.get_stories_by_sprint(sprint_id)

        # Get covered requirements
        cursor.execute("""
            SELECT DISTINCT r.code
            FROM project.requirement_story_mapping rsm
            JOIN task.user_stories us ON rsm.story_id = us.id
            JOIN project.requirements r ON rsm.requirement_id = r.id
            WHERE us.sprint_id = %s
        """, (sprint_id,))
        requirements = [row['code'] for row in cursor.fetchall()]

        # Calculate points
        total_points = sum(s.get('story_points') or 0 for s in stories)
        completed_points = sum(
            s.get('story_points') or 0
            for s in stories
            if s['status'] == 'COMPLETED'
        )

        return SprintWorkflow(
            sprint_id=sprint_id,
            sprint_name=sprint['name'],
            status=sprint['status'],
            start_date=str(sprint.get('start_date', '')),
            end_date=str(sprint.get('end_date', '')),
            stories=[dict(s) for s in stories],
            requirements_covered=requirements,
            total_story_points=total_points,
            completed_story_points=completed_points
        )

    # =========================================
    # Business Glossary
    # =========================================

    def sync_business_glossary(self):
        """Sync PMS domain terms to OpenMetadata glossary"""
        glossary_id = self.om.ensure_glossary(
            "PMS_Insurance_Domain",
            "PMS Insurance Claims project domain terminology"
        )

        if not glossary_id:
            logger.error("Failed to create glossary")
            return False

        # Domain terms
        terms = [
            {
                "name": "Requirement",
                "description": "A documented need from RFP that must be satisfied. Extracted from RFP documents and tracked through User Stories to Tasks.",
                "synonyms": ["REQ", "Requirement Specification", "Feature Request"]
            },
            {
                "name": "UserStory",
                "description": "A unit of work in Scrum representing a feature from user perspective. Format: 'As a [user], I want [goal] so that [benefit]'.",
                "synonyms": ["Story", "PBI", "Product Backlog Item"]
            },
            {
                "name": "Sprint",
                "description": "A time-boxed iteration (1-4 weeks) where a set of User Stories are completed. Core Scrum ceremony.",
                "synonyms": ["Iteration", "Development Cycle"]
            },
            {
                "name": "Task",
                "description": "A unit of work on the Kanban board. Tasks are created by breaking down User Stories into implementable pieces.",
                "synonyms": ["Work Item", "Kanban Card"]
            },
            {
                "name": "KanbanColumn",
                "description": "A column on the Kanban board representing a workflow stage (e.g., TODO, IN_PROGRESS, DONE).",
                "synonyms": ["Workflow Stage", "Status Column"]
            },
            {
                "name": "RFP",
                "description": "Request for Proposal. Official document containing project requirements from the client.",
                "synonyms": ["Proposal Request", "Requirements Document"]
            },
            {
                "name": "StoryPoints",
                "description": "Relative estimation unit for User Story complexity. Used in Planning Poker estimation.",
                "synonyms": ["SP", "Complexity Points"]
            },
            {
                "name": "Backlog",
                "description": "Prioritized list of User Stories. Product Backlog contains all stories; Sprint Backlog contains sprint-selected stories.",
                "synonyms": ["Product Backlog", "Sprint Backlog"]
            }
        ]

        for term in terms:
            self.om.create_glossary_term(
                glossary_name="PMS_Insurance_Domain",
                term_name=term["name"],
                description=term["description"],
                synonyms=term.get("synonyms", [])
            )

        logger.info(f"Synced {len(terms)} terms to business glossary")
        return True

    # =========================================
    # Full Sync
    # =========================================

    def full_sync(self, project_id: str):
        """
        Perform full synchronization for a project.

        This should be run:
        1. After initial OpenMetadata setup
        2. At sprint boundaries
        3. When major schema changes occur
        """
        logger.info(f"Starting full sync for project: {project_id}")

        # 1. Sync business glossary
        logger.info("Syncing business glossary...")
        self.sync_business_glossary()

        # 2. Sync all requirements
        logger.info("Syncing requirements...")
        cursor = self.pg_conn.cursor(cursor_factory=RealDictCursor)
        cursor.execute("""
            SELECT id FROM project.requirements WHERE project_id = %s
        """, (project_id,))
        requirements = cursor.fetchall()

        for req in requirements:
            self.sync_requirement_to_om(req['id'])

        # 3. Create lineage for existing mappings
        logger.info("Creating lineage edges...")
        cursor.execute("""
            SELECT rsm.requirement_id, rsm.story_id
            FROM project.requirement_story_mapping rsm
            JOIN project.requirements r ON rsm.requirement_id = r.id
            WHERE r.project_id = %s
        """, (project_id,))
        mappings = cursor.fetchall()

        for mapping in mappings:
            self.link_requirement_to_story(
                mapping['requirement_id'],
                mapping['story_id'],
                create_lineage=True
            )

        logger.info(f"Full sync completed for project {project_id}")


# ============================================
# CLI Entry Point
# ============================================

def main():
    import argparse

    parser = argparse.ArgumentParser(description="Scrum Workflow Service")
    parser.add_argument("--action", required=True,
                       choices=["sync", "lineage", "impact", "glossary"])
    parser.add_argument("--project-id", help="Project ID for sync")
    parser.add_argument("--requirement-id", help="Requirement ID for lineage/impact")

    args = parser.parse_args()

    service = ScrumWorkflowService()

    try:
        if args.action == "sync":
            if not args.project_id:
                print("--project-id required for sync")
                return
            service.full_sync(args.project_id)

        elif args.action == "lineage":
            if not args.requirement_id:
                print("--requirement-id required for lineage")
                return
            lineage = service.get_requirement_lineage(args.requirement_id)
            print(json.dumps(asdict(lineage), indent=2, default=str))

        elif args.action == "impact":
            if not args.requirement_id:
                print("--requirement-id required for impact")
                return
            impact = service.get_requirement_impact_analysis(args.requirement_id)
            print(json.dumps(impact, indent=2, default=str))

        elif args.action == "glossary":
            service.sync_business_glossary()

    finally:
        service.close()


if __name__ == "__main__":
    main()
