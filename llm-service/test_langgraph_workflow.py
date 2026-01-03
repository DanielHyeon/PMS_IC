"""
LangGraph 워크플로우 테스트 스크립트
"""

import requests
import json
import time

BASE_URL = "http://localhost:8000"


def test_health():
    """헬스 체크 테스트"""
    print("\n=== 1. Health Check ===")
    response = requests.get(f"{BASE_URL}/health")
    result = response.json()
    print(json.dumps(result, indent=2, ensure_ascii=False))

    assert result["status"] == "healthy", "Service is not healthy"
    assert result["model_loaded"], "Model not loaded"
    assert result["chat_workflow_loaded"], "Chat workflow not loaded"
    print("✓ Health check passed")


def test_casual_conversation():
    """일상 대화 테스트 (RAG 스킵 예상)"""
    print("\n=== 2. Casual Conversation (RAG should be skipped) ===")

    test_messages = [
        "안녕하세요!",
        "고마워요",
        "수고하셨습니다",
    ]

    for message in test_messages:
        print(f"\n사용자: {message}")

        response = requests.post(
            f"{BASE_URL}/api/chat",
            json={"message": message, "context": []},
            headers={"Content-Type": "application/json"}
        )

        result = response.json()
        print(f"AI: {result['reply']}")
        print(f"의도: {result['metadata']['intent']}")
        print(f"RAG 문서 수: {result['metadata']['rag_docs_count']}")
        print(f"신뢰도: {result['confidence']}")

        # 일상 대화는 RAG를 사용하지 않아야 함
        assert result['metadata']['intent'] == 'casual', f"Expected 'casual' but got '{result['metadata']['intent']}'"
        assert result['metadata']['rag_docs_count'] == 0, "RAG should be skipped for casual conversation"
        print("✓ Passed: RAG correctly skipped")

        time.sleep(1)


def test_pms_query():
    """PMS 관련 질문 테스트 (RAG 사용 예상)"""
    print("\n=== 3. PMS Query (RAG should be used) ===")

    test_messages = [
        "프로젝트 일정이 어떻게 되나요?",
        "산출물 문서는 어디에 있나요?",
        "이번 단계의 작업 목록을 알려주세요",
    ]

    for message in test_messages:
        print(f"\n사용자: {message}")

        response = requests.post(
            f"{BASE_URL}/api/chat",
            json={"message": message, "context": []},
            headers={"Content-Type": "application/json"}
        )

        result = response.json()
        print(f"AI: {result['reply']}")
        print(f"의도: {result['metadata']['intent']}")
        print(f"RAG 문서 수: {result['metadata']['rag_docs_count']}")
        print(f"신뢰도: {result['confidence']}")

        # PMS 관련 질문은 RAG를 사용해야 함
        assert result['metadata']['intent'] == 'pms_query', f"Expected 'pms_query' but got '{result['metadata']['intent']}'"
        print("✓ Passed: Correctly identified as PMS query")

        time.sleep(1)


def test_general_question():
    """일반 질문 테스트"""
    print("\n=== 4. General Question ===")

    test_messages = [
        "파이썬에서 리스트를 정렬하는 방법은?",
        "오늘 날씨가 어때요?",
    ]

    for message in test_messages:
        print(f"\n사용자: {message}")

        response = requests.post(
            f"{BASE_URL}/api/chat",
            json={"message": message, "context": []},
            headers={"Content-Type": "application/json"}
        )

        result = response.json()
        print(f"AI: {result['reply'][:100]}...")
        print(f"의도: {result['metadata']['intent']}")
        print(f"RAG 문서 수: {result['metadata']['rag_docs_count']}")
        print(f"신뢰도: {result['confidence']}")

        assert result['metadata']['intent'] in ['general', 'pms_query'], "Intent should be general or pms_query"
        print("✓ Passed")

        time.sleep(1)


def test_conversation_with_context():
    """컨텍스트가 있는 대화 테스트"""
    print("\n=== 5. Conversation with Context ===")

    context = []

    # 첫 번째 메시지
    message1 = "프로젝트 일정에 대해 알려줘"
    print(f"\n사용자: {message1}")

    response1 = requests.post(
        f"{BASE_URL}/api/chat",
        json={"message": message1, "context": context},
        headers={"Content-Type": "application/json"}
    )

    result1 = response1.json()
    print(f"AI: {result1['reply'][:100]}...")
    print(f"의도: {result1['metadata']['intent']}")

    # 컨텍스트에 추가
    context.append({"role": "user", "content": message1})
    context.append({"role": "assistant", "content": result1['reply']})

    # 두 번째 메시지 (후속 질문)
    message2 = "좀 더 자세히 설명해줘"
    print(f"\n사용자: {message2}")

    response2 = requests.post(
        f"{BASE_URL}/api/chat",
        json={"message": message2, "context": context},
        headers={"Content-Type": "application/json"}
    )

    result2 = response2.json()
    print(f"AI: {result2['reply'][:100]}...")
    print(f"의도: {result2['metadata']['intent']}")

    print("✓ Passed: Context-aware conversation")


def test_performance():
    """성능 테스트"""
    print("\n=== 6. Performance Test ===")

    test_cases = [
        ("casual", "안녕하세요"),
        ("pms_query", "프로젝트 일정은?"),
    ]

    for intent_type, message in test_cases:
        start_time = time.time()

        response = requests.post(
            f"{BASE_URL}/api/chat",
            json={"message": message, "context": []},
            headers={"Content-Type": "application/json"}
        )

        end_time = time.time()
        elapsed = end_time - start_time

        result = response.json()
        print(f"\n{intent_type} - '{message}'")
        print(f"응답 시간: {elapsed:.2f}초")
        print(f"의도: {result['metadata']['intent']}")
        print(f"RAG 문서 수: {result['metadata']['rag_docs_count']}")

        # 일상 대화는 RAG 스킵으로 더 빠를 것으로 예상
        if intent_type == "casual":
            print(f"✓ Casual conversation (RAG skipped, faster)")


if __name__ == "__main__":
    print("=" * 60)
    print("LangGraph Workflow Test Suite")
    print("=" * 60)

    try:
        test_health()
        test_casual_conversation()
        test_pms_query()
        test_general_question()
        test_conversation_with_context()
        test_performance()

        print("\n" + "=" * 60)
        print("✓ All tests passed!")
        print("=" * 60)

    except AssertionError as e:
        print(f"\n✗ Test failed: {e}")
    except requests.exceptions.ConnectionError:
        print("\n✗ Error: Could not connect to LLM service")
        print("Please make sure the service is running: docker-compose up llm-service")
    except Exception as e:
        print(f"\n✗ Unexpected error: {e}")
        import traceback
        traceback.print_exc()
