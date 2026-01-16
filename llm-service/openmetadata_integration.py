"""
PMS-OpenMetadata Integration Service

스크럼 데이터(유저스토리, 스프린트, 태스크)와 OpenMetadata 메타데이터를 연동하는 서비스.

Usage:
    python openmetadata_integration.py --project-id <PROJECT_ID> --action <full|glossary|stories|sync-tables>

Environment Variables:
    POSTGRES_HOST: PMS PostgreSQL 호스트
    POSTGRES_PORT: PMS PostgreSQL 포트 (기본: 5432)
    POSTGRES_DB: PMS 데이터베이스명
    POSTGRES_USER: PMS DB 사용자
    POSTGRES_PASSWORD: PMS DB 비밀번호
    OPENMETADATA_URL: OpenMetadata 서버 URL
    OM_JWT_TOKEN: OpenMetadata JWT 토큰
"""

import os
import sys
import json
import logging
from dataclasses import dataclass, asdict
from typing import List, Optional, Dict, Any
from datetime import datetime

import psycopg2
from psycopg2.extras import RealDictCursor
import requests

# Logging setup
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


@dataclass
class UserStory:
    """유저 스토리 데이터 클래스"""
    id: str
    project_id: str
    title: str
    status: str
    sprint_id: Optional[str] = None
    assignee_id: Optional[str] = None
    epic: Optional[str] = None
    priority: Optional[int] = None


@dataclass
class Sprint:
    """스프린트 데이터 클래스"""
    id: str
    project_id: str
    name: str
    status: str
    start_date: Optional[str] = None
    end_date: Optional[str] = None


@dataclass
class Task:
    """태스크 데이터 클래스"""
    id: str
    title: str
    status: str
    story_id: Optional[str] = None
    assignee_id: Optional[str] = None
    column_id: Optional[str] = None


class PMSOpenMetadataIntegration:
    """PMS와 OpenMetadata 간 메타데이터 동기화 서비스"""
    
    def __init__(self):
        # PMS PostgreSQL 연결
        self.pms_conn = self._create_pms_connection()
        
        # OpenMetadata API 설정
        self.om_base_url = os.getenv("OPENMETADATA_URL", "http://localhost:8585")
        self.om_token = os.getenv("OM_JWT_TOKEN", "")
        self.om_headers = {
            "Authorization": f"Bearer {self.om_token}",
            "Content-Type": "application/json"
        }
        
        # 서비스명 (OpenMetadata에 등록된 PostgreSQL 서비스)
        self.pms_service_name = "pms-postgres"
        self.pms_database_name = os.getenv("POSTGRES_DB", "pms_db")
    
    def _create_pms_connection(self):
        """PMS PostgreSQL 연결 생성"""
        try:
            conn = psycopg2.connect(
                host=os.getenv("POSTGRES_HOST", "localhost"),
                port=os.getenv("POSTGRES_PORT", "5433"),
                database=os.getenv("POSTGRES_DB", "pms_db"),
                user=os.getenv("POSTGRES_USER", "pms_user"),
                password=os.getenv("POSTGRES_PASSWORD", "pms_password")
            )
            logger.info("Successfully connected to PMS PostgreSQL")
            return conn
        except Exception as e:
            logger.error(f"Failed to connect to PMS PostgreSQL: {e}")
            raise
    
    def close(self):
        """리소스 정리"""
        if self.pms_conn:
            self.pms_conn.close()
    
    # =========================================
    # 1. PMS 데이터 조회
    # =========================================
    
    def get_active_user_stories(self, project_id: str) -> List[UserStory]:
        """활성 유저 스토리 조회"""
        cursor = self.pms_conn.cursor(cursor_factory=RealDictCursor)
        cursor.execute("""
            SELECT id, project_id, title, status, sprint_id, assignee_id, epic, priority_order
            FROM task.user_stories
            WHERE project_id = %s
              AND status NOT IN ('DONE', 'CANCELLED')
            ORDER BY priority_order
        """, (project_id,))
        
        stories = []
        for row in cursor.fetchall():
            stories.append(UserStory(
                id=row['id'],
                project_id=row['project_id'],
                title=row['title'],
                status=row['status'],
                sprint_id=row.get('sprint_id'),
                assignee_id=row.get('assignee_id'),
                epic=row.get('epic'),
                priority=row.get('priority_order')
            ))
        
        cursor.close()
        logger.info(f"Found {len(stories)} active user stories for project {project_id}")
        return stories
    
    def get_sprints(self, project_id: str) -> List[Sprint]:
        """프로젝트 스프린트 조회"""
        cursor = self.pms_conn.cursor(cursor_factory=RealDictCursor)
        cursor.execute("""
            SELECT id, project_id, name, status, start_date, end_date
            FROM task.sprints
            WHERE project_id = %s
            ORDER BY start_date DESC
        """, (project_id,))
        
        sprints = []
        for row in cursor.fetchall():
            sprints.append(Sprint(
                id=row['id'],
                project_id=row['project_id'],
                name=row['name'],
                status=row['status'],
                start_date=str(row['start_date']) if row.get('start_date') else None,
                end_date=str(row['end_date']) if row.get('end_date') else None
            ))
        
        cursor.close()
        logger.info(f"Found {len(sprints)} sprints for project {project_id}")
        return sprints
    
    def get_user_info(self, user_id: str) -> Optional[Dict]:
        """사용자 정보 조회"""
        cursor = self.pms_conn.cursor(cursor_factory=RealDictCursor)
        cursor.execute("""
            SELECT id, email, full_name, role
            FROM auth.users
            WHERE id = %s
        """, (user_id,))
        
        row = cursor.fetchone()
        cursor.close()
        
        if row:
            return dict(row)
        return None
    
    # =========================================
    # 2. OpenMetadata 태그 관리
    # =========================================
    
    def ensure_classification_exists(self, name: str, description: str) -> bool:
        """태그 분류(Classification) 확인/생성"""
        # 먼저 조회
        response = requests.get(
            f"{self.om_base_url}/api/v1/classifications/name/{name}",
            headers=self.om_headers
        )
        
        if response.status_code == 200:
            logger.debug(f"Classification '{name}' already exists")
            return True
        
        # 생성
        data = {
            "name": name,
            "displayName": name,
            "description": description
        }
        
        response = requests.post(
            f"{self.om_base_url}/api/v1/classifications",
            json=data,
            headers=self.om_headers
        )
        
        if response.status_code in [200, 201]:
            logger.info(f"Created classification: {name}")
            return True
        else:
            logger.error(f"Failed to create classification {name}: {response.text}")
            return False
    
    def ensure_tag_exists(self, classification: str, tag_name: str, description: str) -> bool:
        """태그 확인/생성"""
        tag_fqn = f"{classification}.{tag_name}"
        
        # 조회
        response = requests.get(
            f"{self.om_base_url}/api/v1/tags/name/{tag_fqn}",
            headers=self.om_headers
        )
        
        if response.status_code == 200:
            return True
        
        # 생성
        data = {
            "name": tag_name,
            "displayName": tag_name,
            "description": description,
            "classification": classification
        }
        
        response = requests.post(
            f"{self.om_base_url}/api/v1/tags",
            json=data,
            headers=self.om_headers
        )
        
        if response.status_code in [200, 201]:
            logger.info(f"Created tag: {tag_fqn}")
            return True
        else:
            logger.warning(f"Failed to create tag {tag_fqn}: {response.text}")
            return False
    
    def tag_table(self, table_fqn: str, tag_fqn: str) -> bool:
        """테이블에 태그 추가"""
        # 현재 테이블 조회
        response = requests.get(
            f"{self.om_base_url}/api/v1/tables/name/{table_fqn}",
            headers=self.om_headers
        )
        
        if response.status_code != 200:
            logger.warning(f"Table not found: {table_fqn}")
            return False
        
        table_data = response.json()
        current_tags = table_data.get("tags", [])
        
        # 이미 태그가 있는지 확인
        if any(t.get("tagFQN") == tag_fqn for t in current_tags):
            logger.debug(f"Tag {tag_fqn} already exists on {table_fqn}")
            return True
        
        # 태그 추가
        current_tags.append({
            "tagFQN": tag_fqn,
            "labelType": "Manual",
            "state": "Confirmed"
        })
        
        response = requests.patch(
            f"{self.om_base_url}/api/v1/tables/{table_data['id']}",
            json=[{
                "op": "add",
                "path": "/tags",
                "value": current_tags
            }],
            headers={**self.om_headers, "Content-Type": "application/json-patch+json"}
        )
        
        if response.status_code == 200:
            logger.info(f"Tagged {table_fqn} with {tag_fqn}")
            return True
        else:
            logger.error(f"Failed to tag table: {response.text}")
            return False
    
    # =========================================
    # 3. 유저 스토리-테이블 연결
    # =========================================
    
    def tag_table_with_story(self, table_fqn: str, story: UserStory):
        """테이블에 유저 스토리 정보 태깅"""
        # PMS 분류 확인/생성
        self.ensure_classification_exists("PMS", "PMS 프로젝트 관리 메타데이터")
        
        # 스토리 태그 생성
        story_tag = f"STORY-{story.id[:8]}"
        self.ensure_tag_exists("PMS", story_tag, f"User Story: {story.title}")
        
        # 테이블에 태그 추가
        self.tag_table(table_fqn, f"PMS.{story_tag}")
        
        # 스프린트 태그 추가 (있는 경우)
        if story.sprint_id:
            sprint_tag = f"SPRINT-{story.sprint_id[:8]}"
            self.ensure_tag_exists("PMS", sprint_tag, f"Sprint ID: {story.sprint_id}")
            self.tag_table(table_fqn, f"PMS.{sprint_tag}")
    
    def find_tables_by_story_comment(self, story_id: str) -> List[str]:
        """
        테이블 주석에서 스토리 ID가 포함된 테이블 찾기
        
        주석 형식: @story: STORY-001, STORY-002
        """
        cursor = self.pms_conn.cursor(cursor_factory=RealDictCursor)
        
        cursor.execute("""
            SELECT 
                n.nspname as schema_name,
                c.relname as table_name,
                d.description
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            LEFT JOIN pg_description d ON d.objoid = c.oid AND d.objsubid = 0
            WHERE c.relkind = 'r'
              AND n.nspname IN ('auth', 'project', 'task', 'chat', 'report', 'risk')
              AND d.description LIKE %s
        """, (f'%@story:%{story_id}%',))
        
        tables = []
        for row in cursor.fetchall():
            table_fqn = f"{self.pms_service_name}.{self.pms_database_name}.{row['schema_name']}.{row['table_name']}"
            tables.append(table_fqn)
        
        cursor.close()
        return tables
    
    # =========================================
    # 4. 비즈니스 글로서리 관리
    # =========================================
    
    def sync_domain_glossary(self):
        """PMS 도메인 용어를 비즈니스 글로서리로 동기화"""
        
        glossary_name = "PMS_Insurance_Domain"
        
        # 글로서리 생성
        glossary_data = {
            "name": glossary_name,
            "displayName": "PMS 보험 도메인 용어집",
            "description": "보험 심사 프로젝트 관리 시스템의 도메인 용어 정의"
        }
        
        response = requests.put(
            f"{self.om_base_url}/api/v1/glossaries",
            json=glossary_data,
            headers=self.om_headers
        )
        
        if response.status_code not in [200, 201]:
            # 이미 존재하면 조회
            response = requests.get(
                f"{self.om_base_url}/api/v1/glossaries/name/{glossary_name}",
                headers=self.om_headers
            )
        
        if response.status_code not in [200, 201]:
            logger.error(f"Failed to create/get glossary: {response.text}")
            return
        
        glossary_id = response.json().get("id")
        logger.info(f"Glossary ready: {glossary_name}")
        
        # 도메인 용어 정의
        terms = [
            {
                "name": "KCD_CODE",
                "displayName": "한국표준질병사인분류",
                "description": "Korean Standard Classification of Diseases. 의료 보험 심사에서 질병/상해를 분류하는 표준 코드 체계. ICD-10 기반.",
                "synonyms": ["KCD", "질병코드", "상병코드", "진단코드"]
            },
            {
                "name": "RFP",
                "displayName": "제안요청서",
                "description": "Request for Proposal. 프로젝트 발주 시 요구사항을 정의한 공식 문서. 기능/비기능 요구사항 포함.",
                "synonyms": ["제안요청서", "요구사항문서", "RFP문서"]
            },
            {
                "name": "REQUIREMENT",
                "displayName": "요구사항",
                "description": "RFP에서 추출된 개별 기능/비기능 요구사항. 스프린트에 매핑되어 태스크로 분해됨. REQ-{프로젝트코드}-{카테고리}-{순번} 형식의 ID 부여.",
                "synonyms": ["기능요구", "스펙", "요건"]
            },
            {
                "name": "SPRINT",
                "displayName": "스프린트",
                "description": "2-4주 단위의 개발 반복 주기. 백로그에서 선택된 작업을 완료하는 타임박스.",
                "synonyms": ["이터레이션", "반복주기", "개발주기"]
            },
            {
                "name": "USER_STORY",
                "displayName": "유저 스토리",
                "description": "사용자 관점에서 작성된 기능 요구사항. '~로서, ~하고 싶다, ~하기 위해' 형식. 스프린트 백로그의 기본 단위.",
                "synonyms": ["사용자스토리", "US", "기능명세"]
            },
            {
                "name": "DELIVERABLE",
                "displayName": "산출물",
                "description": "프로젝트 단계(Phase)별 산출물. 문서, 코드, 테스트 결과 등 포함. 버전 관리 대상.",
                "synonyms": ["결과물", "산출문서", "아티팩트"]
            },
            {
                "name": "KANBAN_COLUMN",
                "displayName": "칸반 컬럼",
                "description": "태스크의 진행 상태를 나타내는 칸반 보드의 열. 일반적으로 Todo-InProgress-Done 구조.",
                "synonyms": ["칸반열", "상태열", "워크플로우단계"]
            },
            {
                "name": "GRAPH_RAG",
                "displayName": "GraphRAG",
                "description": "그래프 기반 검색 증강 생성. Neo4j에 저장된 지식 그래프를 활용하여 LLM 응답의 정확도를 높이는 기술.",
                "synonyms": ["그래프RAG", "지식그래프검색"]
            }
        ]
        
        for term in terms:
            self._create_glossary_term(glossary_name, term)
    
    def _create_glossary_term(self, glossary_name: str, term: Dict):
        """글로서리 용어 생성"""
        term_fqn = f"{glossary_name}.{term['name']}"
        
        # 이미 존재하는지 확인
        response = requests.get(
            f"{self.om_base_url}/api/v1/glossaryTerms/name/{term_fqn}",
            headers=self.om_headers
        )
        
        if response.status_code == 200:
            logger.debug(f"Glossary term already exists: {term['name']}")
            return
        
        term_data = {
            "name": term["name"],
            "displayName": term["displayName"],
            "description": term["description"],
            "synonyms": term.get("synonyms", []),
            "glossary": glossary_name
        }
        
        response = requests.post(
            f"{self.om_base_url}/api/v1/glossaryTerms",
            json=term_data,
            headers=self.om_headers
        )
        
        if response.status_code in [200, 201]:
            logger.info(f"Created glossary term: {term['name']}")
        else:
            logger.warning(f"Failed to create term {term['name']}: {response.text}")
    
    # =========================================
    # 5. 테이블 커스텀 속성 업데이트
    # =========================================
    
    def update_table_extension(self, table_fqn: str, extension_data: Dict):
        """테이블 커스텀 속성(extension) 업데이트"""
        # 테이블 조회
        response = requests.get(
            f"{self.om_base_url}/api/v1/tables/name/{table_fqn}",
            headers=self.om_headers
        )
        
        if response.status_code != 200:
            logger.warning(f"Table not found: {table_fqn}")
            return False
        
        table_data = response.json()
        current_extension = table_data.get("extension", {}) or {}
        
        # 확장 데이터 병합
        current_extension.update(extension_data)
        current_extension["last_synced"] = datetime.now().isoformat()
        
        # 업데이트
        response = requests.patch(
            f"{self.om_base_url}/api/v1/tables/{table_data['id']}",
            json=[{
                "op": "add",
                "path": "/extension",
                "value": current_extension
            }],
            headers={**self.om_headers, "Content-Type": "application/json-patch+json"}
        )
        
        if response.status_code == 200:
            logger.info(f"Updated extension for {table_fqn}")
            return True
        else:
            logger.error(f"Failed to update extension: {response.text}")
            return False
    
    # =========================================
    # 6. 전체 동기화
    # =========================================
    
    def full_sync(self, project_id: str):
        """
        프로젝트의 전체 메타데이터 동기화
        
        스프린트 종료 시 또는 CI/CD 파이프라인에서 호출
        """
        logger.info(f"Starting full sync for project: {project_id}")
        
        # 1. 비즈니스 글로서리 동기화
        logger.info("Step 1: Syncing domain glossary...")
        self.sync_domain_glossary()
        
        # 2. 태그 분류 준비
        logger.info("Step 2: Preparing tag classifications...")
        self.ensure_classification_exists("PMS", "PMS 프로젝트 관리 메타데이터")
        self.ensure_classification_exists("DataTier", "데이터 중요도 분류")
        
        # 3. 스프린트 정보 동기화
        logger.info("Step 3: Syncing sprint information...")
        sprints = self.get_sprints(project_id)
        for sprint in sprints:
            sprint_tag = f"SPRINT-{sprint.id[:8]}"
            self.ensure_tag_exists("PMS", sprint_tag, f"Sprint: {sprint.name} ({sprint.status})")
        
        # 4. 유저 스토리 태깅
        logger.info("Step 4: Tagging tables with user stories...")
        stories = self.get_active_user_stories(project_id)
        
        for story in stories:
            # 스토리 태그 생성
            story_tag = f"STORY-{story.id[:8]}"
            self.ensure_tag_exists("PMS", story_tag, f"User Story: {story.title}")
            
            # 관련 테이블 찾기 및 태깅
            related_tables = self.find_tables_by_story_comment(story.id)
            
            for table_fqn in related_tables:
                self.tag_table(table_fqn, f"PMS.{story_tag}")
                
                # 스프린트 태그도 추가
                if story.sprint_id:
                    sprint_tag = f"SPRINT-{story.sprint_id[:8]}"
                    self.tag_table(table_fqn, f"PMS.{sprint_tag}")
                
                # 커스텀 속성 업데이트
                self.update_table_extension(table_fqn, {
                    "story_id": story.id,
                    "story_title": story.title,
                    "sprint_id": story.sprint_id,
                    "assignee_id": story.assignee_id,
                    "project_id": project_id
                })
        
        logger.info(f"Full sync completed! Processed {len(stories)} stories, {len(sprints)} sprints")
    
    def sync_core_tables(self):
        """핵심 테이블에 기본 태그 추가"""
        logger.info("Syncing core tables with default tags...")
        
        # 데이터 티어 태그 생성
        self.ensure_classification_exists("DataTier", "데이터 중요도 분류")
        self.ensure_tag_exists("DataTier", "Tier1_Critical", "비즈니스 크리티컬 데이터")
        self.ensure_tag_exists("DataTier", "Tier2_Important", "중요 운영 데이터")
        self.ensure_tag_exists("DataTier", "Tier3_Normal", "일반 데이터")
        
        # 핵심 테이블 태깅
        core_tables = [
            # Tier 1: 핵심 데이터
            (f"{self.pms_service_name}.{self.pms_database_name}.auth.users", "Tier1_Critical"),
            (f"{self.pms_service_name}.{self.pms_database_name}.project.projects", "Tier1_Critical"),
            (f"{self.pms_service_name}.{self.pms_database_name}.project.requirements", "Tier1_Critical"),
            
            # Tier 2: 중요 운영 데이터
            (f"{self.pms_service_name}.{self.pms_database_name}.task.sprints", "Tier2_Important"),
            (f"{self.pms_service_name}.{self.pms_database_name}.task.user_stories", "Tier2_Important"),
            (f"{self.pms_service_name}.{self.pms_database_name}.task.tasks", "Tier2_Important"),
            
            # Tier 3: 일반 데이터
            (f"{self.pms_service_name}.{self.pms_database_name}.chat.chat_sessions", "Tier3_Normal"),
            (f"{self.pms_service_name}.{self.pms_database_name}.chat.chat_messages", "Tier3_Normal"),
        ]
        
        for table_fqn, tier in core_tables:
            self.tag_table(table_fqn, f"DataTier.{tier}")


def main():
    """CLI 진입점"""
    import argparse
    
    parser = argparse.ArgumentParser(
        description="PMS-OpenMetadata Integration Service",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    # 전체 동기화
    python openmetadata_integration.py --project-id abc123 --action full
    
    # 글로서리만 동기화
    python openmetadata_integration.py --action glossary
    
    # 핵심 테이블 태깅
    python openmetadata_integration.py --action sync-tables
        """
    )
    
    parser.add_argument(
        "--project-id",
        help="동기화할 프로젝트 ID"
    )
    
    parser.add_argument(
        "--action",
        choices=["full", "glossary", "stories", "sync-tables"],
        default="full",
        help="실행할 작업 (기본: full)"
    )
    
    args = parser.parse_args()
    
    # 환경변수 검증
    required_vars = ["OPENMETADATA_URL", "OM_JWT_TOKEN"]
    missing = [v for v in required_vars if not os.getenv(v)]
    
    if missing:
        logger.error(f"Missing required environment variables: {missing}")
        sys.exit(1)
    
    if args.action in ["full", "stories"] and not args.project_id:
        logger.error("--project-id is required for 'full' and 'stories' actions")
        sys.exit(1)
    
    # 실행
    integration = PMSOpenMetadataIntegration()
    
    try:
        if args.action == "full":
            integration.full_sync(args.project_id)
        elif args.action == "glossary":
            integration.sync_domain_glossary()
        elif args.action == "stories":
            stories = integration.get_active_user_stories(args.project_id)
            print(f"Found {len(stories)} active stories:")
            for s in stories:
                print(f"  - {s.id[:8]}: {s.title} ({s.status})")
        elif args.action == "sync-tables":
            integration.sync_core_tables()
    finally:
        integration.close()


if __name__ == "__main__":
    main()
