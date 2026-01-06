"""
Mock 데이터를 RAG 시스템에 인덱싱하는 스크립트
프로젝트의 mock 데이터를 ChromaDB에 추가하여 AI 챗봇이 활용할 수 있도록 합니다.
"""

import requests
import json
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# LLM 서비스 API 엔드포인트
LLM_SERVICE_URL = "http://localhost:8000"

# Mock 데이터 정의
MOCK_DOCUMENTS = [
    # 1. 프로젝트 개요
    {
        "id": "project_overview",
        "content": """
프로젝트명: 보험 Claim Automation AI 시스템 구축
프로젝트 코드: INS-AI-2024-001
프로젝트 기간: 2024년 1월 ~ 2024년 12월
프로젝트 예산: 15억원
프로젝트 목표: 보험 청구 심사 프로세스의 AI 자동화를 통한 업무 효율성 30% 향상

주요 기능:
- 영수증 OCR 자동 인식 (목표 정확도 98%)
- 진단서 자동 분류 (목표 정확도 95%)
- XAI 설명 가능성 제공 (신뢰도 점수)
- 유사 케이스 자동 검색
- 모델 성능 모니터링 대시보드
        """,
        "metadata": {
            "type": "project",
            "category": "overview",
            "tags": "프로젝트,개요,목표"
        }
    },

    # 2. 프로젝트 조직
    {
        "id": "project_team",
        "content": """
프로젝트 조직 구성:

1. 경영진
   - 이사장 (sponsor@insure.com): 프로젝트 스폰서

2. PMO 조직
   - PMO 총괄 (pmo@insure.com): PMO 전체 관리

3. 프로젝트팀 (IT혁신팀)
   - 프로젝트 관리자 김철수 (pm@insure.com): 전체 프로젝트 관리

4. AI 개발팀 (12명)
   - 팀장 박민수 (dev@insure.com)
   - 주요 업무: AI 모델 개발, 데이터 처리, 알고리즘 개선

5. 품질보증팀 (4명)
   - 팀장 최지훈 (qa@insure.com)
   - 주요 업무: 테스트, 품질 검증

6. 현업 분석가팀 (보험심사팀, 5명)
   - 팀장 이영희 (ba@insure.com)
   - 주요 업무: 업무 분석, 요구사항 정의

7. 외부 감리 (2명)
   - 감리인 (auditor@insure.com): 외부감리법인

8. IT 운영팀 (2명)
   - 시스템관리자 (admin@insure.com): 인프라 관리
        """,
        "metadata": {
            "type": "project",
            "category": "team",
            "tags": "조직,팀,인력"
        }
    },

    # 3. 프로젝트 단계 (워터폴)
    {
        "id": "project_phases",
        "content": """
프로젝트 6단계 구성 (워터폴 방법론):

1단계: 업무 진단 및 목표 설정 (완료 100%)
- 산출물: AS-IS 프로세스 분석서, KPI 정의서, 프로젝트 헌장
- 기간: 2024-01-01 ~ 2024-02-29

2단계: 데이터 수집 및 준비 (완료 100%)
- 산출물: 데이터셋 인벤토리, 비식별화 보고서, 데이터 품질 검증서
- 기간: 2024-03-01 ~ 2024-04-30

3단계: AI 모델링 및 학습 (진행 중 85%)
- 산출물: OCR v2.0 모델, 분류 모델 v1.0, 학습 파이프라인, 하이퍼파라미터 보고서
- 기간: 2024-05-01 ~ 2024-07-31
- 현재 진행 중인 작업:
  * OCR 모델 v2.1 학습 데이터 증강
  * 특정 병원 진단서 양식 데이터 수집
  * 하이퍼파라미터 튜닝 실험

4단계: 시스템 통합 및 개발 (미시작)
- 산출물: 통합 시스템 아키텍처, API 문서, 통합 테스트 보고서
- 기간: 2024-08-01 ~ 2024-09-30

5단계: 성능 검증 및 UAT (미시작)
- 산출물: 성능 테스트 보고서, UAT 시나리오, 사용자 매뉴얼
- 기간: 2024-10-01 ~ 2024-11-15

6단계: 변화 관리 및 안정화 (미시작)
- 산출물: 교육 자료, 변화관리 계획서, 최종 프로젝트 보고서
- 기간: 2024-11-16 ~ 2024-12-31
        """,
        "metadata": {
            "type": "project",
            "category": "phases",
            "tags": "단계,일정,워터폴"
        }
    },

    # 4. 현재 스프린트 작업 (애자일)
    {
        "id": "current_sprint_tasks",
        "content": """
현재 스프린트 작업 현황 (칸반 보드):

[제품 백로그]
1. 진단서 이미지 전처리 알고리즘 개선
   - 담당자: 박민수, Story Points: 5, 우선순위: Medium

2. 약관 해석 NLP 모델 베이스라인 구축
   - 담당자: 김지은, Story Points: 8, 우선순위: High

[이번 스프린트]
3. OCR 모델 v2.1 학습 데이터 증강
   - 담당자: 이영희, Story Points: 8, 우선순위: High

4. 영수증 항목 분류 모델 정확도 개선
   - 담당자: 최지훈, Story Points: 5, 우선순위: Medium

[진행 중]
5. 특정 병원 진단서 양식 데이터 수집
   - 담당자: 박민수, Story Points: 3, 우선순위: High, 긴급

6. 하이퍼파라미터 튜닝 실험
   - 담당자: 김지은, Story Points: 5, 우선순위: Medium

[코드 리뷰]
7. 데이터 파이프라인 리팩토링
   - 담당자: 이영희, Story Points: 3, 우선순위: Low

[테스트 중]
8. OCR v2.0 통합 테스트
   - 담당자: 최지훈, Story Points: 5, 우선순위: High

[완료]
9. 모델 성능 지표 대시보드 구축
   - 담당자: 박민수, Story Points: 5, 우선순위: Medium

10. 데이터 비식별화 자동화 스크립트
    - 담당자: 김지은, Story Points: 8, 우선순위: High
        """,
        "metadata": {
            "type": "project",
            "category": "tasks",
            "tags": "스프린트,작업,칸반"
        }
    },

    # 5. 사용자 스토리
    {
        "id": "user_stories",
        "content": """
주요 사용자 스토리 (백로그):

US-001: 영수증 자동 OCR
- 설명: 보험 심사자로서 영수증을 스캔하면 자동으로 항목과 금액이 추출되어 수작업 입력 시간을 줄이고 싶다
- 인수 기준: OCR 정확도 98% 이상, 처리 시간 3초 이내
- Story Points: 8, 우선순위: High

US-002: 진단서 자동 분류
- 설명: 보험 심사자로서 다양한 병원의 진단서를 업로드하면 자동으로 질병 코드와 카테고리가 분류되어 빠르게 심사할 수 있다
- 인수 기준: 분류 정확도 95% 이상, 지원 양식 50개 이상
- Story Points: 13, 우선순위: High

US-003: XAI 설명 가능성
- 설명: 보험 심사자로서 AI가 내린 결정의 근거를 시각적으로 확인하여 신뢰도를 판단하고 싶다
- 인수 기준: 신뢰도 점수 표시, 주요 근거 3개 이상 제시
- Story Points: 5, 우선순위: Medium

US-004: 유사 케이스 검색
- 설명: 보험 심사자로서 현재 케이스와 유사한 과거 심사 사례를 자동으로 검색하여 일관성 있는 판단을 하고 싶다
- 인수 기준: 유사도 80% 이상 케이스 5건 추천, 검색 시간 2초 이내
- Story Points: 8, 우선순위: Medium

US-005: 모델 성능 대시보드
- 설명: AI 개발자로서 일별/주별 모델 성능 추이를 대시보드에서 확인하여 품질 저하를 빠르게 감지하고 싶다
- 인수 기준: 실시간 업데이트, 7개 이상의 성능 지표
- Story Points: 5, 우선순위: Low

US-006: 데이터 라벨링 도구
- 설명: 데이터 엔지니어로서 직관적인 UI로 데이터를 라벨링하고 품질을 검증하여 학습 데이터 품질을 높이고 싶다
- 인수 기준: 시간당 100건 이상 처리, 오류율 2% 이하
- Story Points: 8, 우선순위: Medium
        """,
        "metadata": {
            "type": "project",
            "category": "requirements",
            "tags": "사용자스토리,요구사항,백로그"
        }
    },

    # 6. 프로젝트 KPI
    {
        "id": "project_kpi",
        "content": """
프로젝트 핵심 성과 지표 (KPI):

1. 전체 진행률: 62%
   - 목표: 100% (2024-12-31)
   - 현재 상태: 순조

2. 예산 사용률: 58% (8.7억원 / 15억원)
   - 목표: 계획 대비 ±5% 이내
   - 현재 상태: 양호

3. 활성 이슈: 7건
   - 고위험: 1건 (데이터 품질)
   - 중위험: 3건
   - 저위험: 3건

4. 작업 완료율: 142/230 (61.7%)
   - 완료: 142건
   - 진행 중: 23건
   - 미시작: 65건

5. 스프린트 속도 (Story Points)
   - Sprint 1: 23 SP (계획 25 SP)
   - Sprint 2: 28 SP (계획 30 SP)
   - Sprint 3: 32 SP (계획 30 SP)
   - Sprint 4: 27 SP (계획 35 SP)
   - Sprint 5: 31 SP (계획 35 SP)
   - 평균 속도: 28.2 SP

6. OCR 모델 정확도
   - 현재: 96.5%
   - 목표: 98%
   - Gap: -1.5%p

7. 분류 모델 정확도
   - 현재: 92.3%
   - 목표: 95%
   - Gap: -2.7%p
        """,
        "metadata": {
            "type": "project",
            "category": "kpi",
            "tags": "성과지표,KPI,진척"
        }
    },

    # 7. 역할 및 권한
    {
        "id": "roles_permissions",
        "content": """
시스템 역할 및 권한:

역할 8개:
1. 프로젝트 스폰서 (SPONSOR)
   - 인원: 1명
   - 주요 권한: 대시보드 조회, 프로젝트 삭제, 예산 승인

2. PMO 총괄 (PMO_HEAD)
   - 인원: 1명
   - 주요 권한: 프로젝트 생성/삭제, WBS 관리, 예산 관리, 리스크 관리

3. 프로젝트 관리자 (PM)
   - 인원: 1명
   - 주요 권한: 프로젝트 생성, WBS 관리, 예산 관리, 리스크 관리, 백로그/스프린트 관리

4. 개발팀 (DEVELOPER)
   - 인원: 12명
   - 주요 권한: 대시보드 조회, WBS 관리, 백로그/스프린트 관리, AI 어시스턴트 사용

5. QA팀 (QA)
   - 인원: 4명
   - 주요 권한: 대시보드 조회, WBS 관리, 산출물 승인, AI 어시스턴트 사용

6. 현업 분석가 (BUSINESS_ANALYST)
   - 인원: 5명
   - 주요 권한: 대시보드 조회, WBS 관리, 리스크 관리, 백로그/스프린트 관리

7. 외부 감리 (AUDITOR)
   - 인원: 2명
   - 주요 권한: 대시보드 조회, 감사 로그 조회

8. 시스템 관리자 (ADMIN)
   - 인원: 2명
   - 주요 권한: 모든 권한

권한 14개:
- view_dashboard: 대시보드 조회
- create_project: 프로젝트 생성
- delete_project: 프로젝트 삭제
- manage_wbs: WBS 관리
- manage_budget: 예산 관리
- approve_budget: 예산 승인
- manage_risk: 리스크 관리
- approve_deliverable: 산출물 승인
- manage_backlog: 백로그 관리
- manage_sprint: 스프린트 관리
- use_ai_assistant: AI 어시스턴트 사용
- view_audit_log: 감사 로그 조회
- manage_users: 사용자 관리
        """,
        "metadata": {
            "type": "system",
            "category": "auth",
            "tags": "역할,권한,보안"
        }
    },

    # 8. AI 인사이트
    {
        "id": "ai_insights",
        "content": """
AI 어시스턴트 인사이트 (최근 분석):

1. 위험 감지
   - "3단계 일정이 2주 지연될 가능성이 있습니다. 데이터 수집 병목이 주요 원인입니다."
   - 카테고리: 일정 리스크
   - 심각도: High
   - 감지 일자: 2024-06-15

2. 주간 성과
   - "지난주 대비 스프린트 속도가 15% 향상되었습니다. 팀 협업이 우수합니다."
   - 카테고리: 성과 개선
   - 심각도: Low
   - 분석 일자: 2024-06-10

3. 권장 사항
   - "OCR 정확도 향상을 위해 데이터 증강 기법 적용을 권장합니다."
   - 카테고리: 품질 개선
   - 심각도: Medium
   - 제안 일자: 2024-06-08

4. 예산 알림
   - "현재 예산 사용률이 계획 대비 3% 초과 중입니다. 4단계 예산 조정이 필요할 수 있습니다."
   - 카테고리: 예산 관리
   - 심각도: Medium
   - 알림 일자: 2024-06-12

5. 모델 성능 모니터링
   - "OCR v2.0 모델의 정확도가 96.5%로 목표(98%) 대비 1.5%p 미달입니다."
   - "분류 모델의 정확도가 92.3%로 목표(95%) 대비 2.7%p 미달입니다."
   - 카테고리: 품질 지표
   - 심각도: High
   - 측정 일자: 2024-06-14
        """,
        "metadata": {
            "type": "ai",
            "category": "insights",
            "tags": "AI,인사이트,알림"
        }
    },

    # 9. 일반적인 질문과 답변
    {
        "id": "faq",
        "content": """
자주 묻는 질문 (FAQ):

Q1: 현재 프로젝트 진행률은?
A: 전체 진행률은 62%입니다. 3단계(AI 모델링 및 학습)가 85% 진행 중이며, 일정 대비 순조롭게 진행되고 있습니다.

Q2: 현재 활성 이슈는 몇 건인가요?
A: 총 7건의 활성 이슈가 있습니다. 고위험 1건, 중위험 3건, 저위험 3건입니다.

Q3: OCR 모델의 현재 정확도는?
A: OCR v2.0 모델의 현재 정확도는 96.5%이며, 목표인 98% 대비 1.5%p 미달입니다. 데이터 증강을 통해 개선 중입니다.

Q4: 이번 스프린트에서 진행 중인 주요 작업은?
A: 'OCR 모델 v2.1 학습 데이터 증강'(이영희, 8SP), '영수증 항목 분류 모델 정확도 개선'(최지훈, 5SP) 등이 진행 중입니다.

Q5: 예산 사용 현황은?
A: 전체 예산 15억원 중 8.7억원(58%)을 사용했습니다. 계획 대비 정상 범위 내입니다.

Q6: 다음 단계는 언제 시작하나요?
A: 4단계(시스템 통합 및 개발)는 2024년 8월 1일에 시작 예정입니다.

Q7: 프로젝트 팀은 몇 명인가요?
A: 총 27명입니다. AI 개발팀 12명, QA팀 4명, 현업 분석가 5명, PMO/PM 2명, 외부 감리 2명, 시스템 관리자 2명입니다.

Q8: AI 어시스턴트는 어떤 역할을 하나요?
A: 프로젝트 데이터를 분석하여 위험을 감지하고, 성과를 추적하며, 개선 사항을 권장합니다. 또한 사용자 질문에 답변하고 업무를 지원합니다.

Q9: WBS는 어떻게 관리되나요?
A: 워터폴 방법론의 6단계 WBS와 애자일 스프린트를 병행 관리합니다. PM, 개발자, 현업 분석가가 WBS 관리 권한을 가집니다.

Q10: 산출물은 어떻게 승인되나요?
A: QA팀과 PMO가 산출물 승인 권한을 가지며, 각 단계별 정의된 산출물을 검토하여 승인합니다.
        """,
        "metadata": {
            "type": "system",
            "category": "faq",
            "tags": "FAQ,질문,답변"
        }
    },

    # 10. 프로젝트 용어집
    {
        "id": "glossary",
        "content": """
프로젝트 용어집:

PMS: Project Management System (프로젝트 관리 시스템)
- 프로젝트 일정, 예산, 리스크, 산출물 등을 통합 관리하는 시스템

OCR: Optical Character Recognition (광학 문자 인식)
- 영수증, 진단서 등의 이미지에서 텍스트를 자동으로 추출하는 기술

XAI: Explainable AI (설명 가능한 AI)
- AI의 결정 근거를 사람이 이해할 수 있도록 설명하는 기술

WBS: Work Breakdown Structure (작업 분해 구조)
- 프로젝트를 작은 단위의 작업으로 분해한 계층 구조

Story Points (SP): 애자일 추정 단위
- 작업의 복잡도와 노력을 상대적으로 추정하는 단위 (예: 1, 2, 3, 5, 8, 13)

Sprint: 스프린트
- 애자일에서 1~4주의 짧은 개발 주기

Backlog: 백로그
- 구현해야 할 기능과 작업의 목록

UAT: User Acceptance Test (사용자 수용 테스트)
- 실제 사용자가 시스템을 테스트하여 요구사항 충족 여부를 확인

KPI: Key Performance Indicator (핵심 성과 지표)
- 프로젝트의 성과를 측정하는 주요 지표

PMO: Project Management Office (프로젝트 관리 오피스)
- 프로젝트 관리 표준과 방법론을 정의하고 지원하는 조직

Claim Automation: 보험 청구 자동화
- 보험 청구 서류를 자동으로 처리하여 심사 시간을 단축하는 시스템

NLP: Natural Language Processing (자연어 처리)
- 약관, 진단서 등의 텍스트를 컴퓨터가 이해하고 처리하는 기술

하이퍼파라미터: Hyperparameter
- AI 모델 학습 전에 설정하는 매개변수 (예: 학습률, 배치 크기 등)

데이터 증강: Data Augmentation
- 기존 데이터를 변형하여 학습 데이터의 양과 다양성을 늘리는 기법
        """,
        "metadata": {
            "type": "system",
            "category": "glossary",
            "tags": "용어,정의"
        }
    }
]

def add_documents_to_rag():
    """RAG 시스템에 문서 추가"""
    try:
        logger.info(f"Adding {len(MOCK_DOCUMENTS)} documents to RAG system...")

        response = requests.post(
            f"{LLM_SERVICE_URL}/api/documents",
            json={"documents": MOCK_DOCUMENTS},
            headers={"Content-Type": "application/json"},
            timeout=30
        )

        if response.status_code == 200:
            result = response.json()
            logger.info(f"✅ Successfully added {result.get('success_count')}/{result.get('total')} documents")
            return True
        else:
            logger.error(f"❌ Failed to add documents: {response.status_code} - {response.text}")
            return False

    except requests.exceptions.ConnectionError:
        logger.error("❌ Cannot connect to LLM service. Is it running on http://localhost:8000?")
        return False
    except Exception as e:
        logger.error(f"❌ Error adding documents: {e}")
        return False

def get_collection_stats():
    """컬렉션 통계 조회"""
    try:
        response = requests.get(
            f"{LLM_SERVICE_URL}/api/documents/stats",
            timeout=10
        )

        if response.status_code == 200:
            stats = response.json()
            logger.info(f"📊 Collection stats: {json.dumps(stats, indent=2, ensure_ascii=False)}")
            return stats
        else:
            logger.error(f"Failed to get stats: {response.status_code}")
            return None

    except Exception as e:
        logger.error(f"Error getting stats: {e}")
        return None

def test_search(query: str):
    """문서 검색 테스트"""
    try:
        logger.info(f"🔍 Testing search with query: '{query}'")

        response = requests.post(
            f"{LLM_SERVICE_URL}/api/documents/search",
            json={"query": query, "top_k": 3},
            headers={"Content-Type": "application/json"},
            timeout=10
        )

        if response.status_code == 200:
            results = response.json()
            logger.info(f"Found {results.get('count')} results:")
            for i, result in enumerate(results.get('results', []), 1):
                logger.info(f"\n{i}. Distance: {result.get('distance', 'N/A')}")
                logger.info(f"   Content preview: {result.get('content', '')[:200]}...")
                logger.info(f"   Metadata: {result.get('metadata', {})}")
            return results
        else:
            logger.error(f"Search failed: {response.status_code}")
            return None

    except Exception as e:
        logger.error(f"Error during search: {e}")
        return None

def main():
    """메인 실행 함수"""
    logger.info("=" * 80)
    logger.info("Mock 데이터 RAG 인덱싱 시작")
    logger.info("=" * 80)

    # 1. 문서 추가
    if add_documents_to_rag():
        logger.info("\n✅ 문서 인덱싱 완료\n")
    else:
        logger.error("\n❌ 문서 인덱싱 실패\n")
        return

    # 2. 통계 조회
    logger.info("-" * 80)
    get_collection_stats()

    # 3. 검색 테스트
    logger.info("\n" + "-" * 80)
    logger.info("검색 테스트 시작")
    logger.info("-" * 80)

    test_queries = [
        "현재 프로젝트 진행률은?",
        "OCR 모델의 정확도는?",
        "프로젝트 팀원은 누가 있나요?",
        "활성 이슈는 몇 건인가요?",
        "Story Points가 뭔가요?"
    ]

    for query in test_queries:
        test_search(query)
        logger.info("")

    logger.info("=" * 80)
    logger.info("✅ 모든 작업 완료!")
    logger.info("=" * 80)

if __name__ == "__main__":
    main()
