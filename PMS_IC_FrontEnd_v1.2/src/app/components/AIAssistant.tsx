import { useState } from 'react';
import { X, Send, Bot, Sparkles, TrendingUp, FileText, AlertTriangle } from 'lucide-react';
import { UserRole } from '../App';

interface Message {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface SuggestedPrompt {
  icon: React.ReactNode;
  text: string;
  prompt: string;
}

export default function AIAssistant({ onClose, userRole }: { onClose: () => void; userRole: UserRole }) {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 1,
      role: 'assistant',
      content: '안녕하세요! InsureTech AI-PMS의 AI 어시스턴트입니다. 프로젝트 관리와 관련하여 무엇을 도와드릴까요?',
      timestamp: new Date(),
    },
  ]);
  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  const suggestedPrompts: SuggestedPrompt[] = [
    {
      icon: <FileText size={16} />,
      text: 'WBS 생성',
      prompt: '3단계 AI 모델링을 위한 WBS를 생성해줘',
    },
    {
      icon: <AlertTriangle size={16} />,
      text: '리스크 분석',
      prompt: '현재 프로젝트의 주요 리스크를 분석해줘',
    },
    {
      icon: <TrendingUp size={16} />,
      text: '주간 보고서',
      prompt: '이번 주 프로젝트 진행 상황을 요약해줘',
    },
    {
      icon: <Sparkles size={16} />,
      text: '일정 예측',
      prompt: '현재 속도로 스프린트 목표를 달성할 수 있을까?',
    },
  ];

  const simulateAIResponse = (userMessage: string): string => {
    const lowerMessage = userMessage.toLowerCase();

    if (lowerMessage.includes('wbs') || lowerMessage.includes('작업')) {
      return `AI 모델링 단계를 위한 WBS를 생성했습니다:

**3단계: AI 모델링 및 학습**

1. **데이터 정제 및 증강** (5일, 담당: 데이터팀)
   - 비식별화된 데이터 품질 검증
   - 데이터 증강(Data Augmentation) 기법 적용
   - Train/Validation/Test 세트 분리

2. **특징 공학 (Feature Engineering)** (3일, 담당: AI팀)
   - 영수증 이미지 전처리 파이프라인 구축
   - 텍스트 특징 추출 알고리즘 개발
   - 메타데이터 특징 생성

3. **OCR 모델 학습 및 튜닝** (10일, 담당: AI팀)
   - 베이스라인 모델 학습 (Tesseract, EasyOCR 비교)
   - 커스텀 모델 파인튜닝 (한글 진단서 특화)
   - 하이퍼파라미터 최적화 (Learning Rate, Batch Size 등)

4. **분류 모델 개발** (7일, 담당: AI팀)
   - 진료 항목 분류 모델 학습 (BERT 기반)
   - 약관 매칭 알고리즘 구현
   - Ensemble 기법 적용

5. **성능 평가 및 보고** (3일, 담당: PM + AI팀)
   - Accuracy, Precision, Recall 측정
   - 혼동 행렬(Confusion Matrix) 분석
   - 성능 개선 포인트 도출

**예상 총 공수:** 28 Story Points
**리스크:** 데이터 품질 이슈, 특정 양식 인식률 저하`;
    }

    if (lowerMessage.includes('리스크') || lowerMessage.includes('위험')) {
      return `**현재 프로젝트 주요 리스크 분석 결과:**

🔴 **High Risk (즉시 조치 필요)**
1. **OCR 인식률 목표 미달 위험** (발생 확률: 75%)
   - 현재 93.5%, 목표 95%
   - 특정 병원(서울대병원, 세브란스) 진단서 양식 인식률 85% 수준
   - **권장 조치:** 해당 병원 데이터 500건 추가 확보 및 파인튜닝

🟡 **Medium Risk (모니터링 필요)**
2. **데이터 라벨링 지연** (발생 확률: 60%)
   - 현업 검증자 부족으로 라벨링 속도 저하
   - **권장 조치:** 외주 라벨링 업체 활용 검토

3. **레거시 시스템 연동 복잡도** (발생 확률: 50%)
   - 기존 심사 시스템 API 문서 불완전
   - **권장 조치:** IT 인프라팀과 사전 기술 검토 회의

🟢 **Low Risk (계속 관찰)**
4. **팀원 교체 가능성** (발생 확률: 20%)
   - 핵심 개발자 1명 타 프로젝트 배정 가능성
   - **권장 조치:** 지식 이전 문서화 강화`;
    }

    if (lowerMessage.includes('보고서') || lowerMessage.includes('요약') || lowerMessage.includes('진행')) {
      return `**금주 프로젝트 진행 현황 요약** (2025년 8월 11일 ~ 8월 15일)

📊 **전체 진행률:** 62% (계획 대비 +2%p)

✅ **주요 성과:**
- OCR 모델 v2.1 학습 완료 (인식률 93.5% → 94.2% 향상)
- 데이터 파이프라인 성능 최적화 (처리 속도 30% 개선)
- 모델 성능 모니터링 대시보드 구축 완료

⚠️ **주요 이슈:**
- 특정 병원 진단서 인식률 저하 문제 지속 (85% 수준)
- 데이터 라벨링 일정 2일 지연

📈 **다음 주 계획:**
- 데이터 증강 기법 적용 (Rotation, Noise 추가 등)
- 하이퍼파라미터 튜닝 실험 (Grid Search)
- 현업 검증 피드백 반영

👥 **팀 현황:** 5명 (개발 3, QA 1, PM 1)`;
    }

    if (lowerMessage.includes('달성') || lowerMessage.includes('예측') || lowerMessage.includes('스프린트')) {
      return `**스프린트 목표 달성 예측 분석:**

현재 Sprint 5 진행 상황 (Day 10 / 14일):
- **남은 작업:** 8 Story Points
- **남은 기간:** 4일
- **현재 Velocity:** 40 SP/Sprint (최근 3개 스프린트 평균)
- **일일 평균 소화량:** 약 3 SP/Day

📊 **예측 결과:**
- **목표 달성 확률:** 85% ✅
- **예상 완료일:** 8월 18일 (마감일 준수 가능)

💡 **AI 권장 사항:**
1. 현재 속도 유지 시 목표 달성 가능
2. 긴급 이슈 1건(진단서 데이터 수집)이 병목 요인
3. 해당 작업에 추가 리소스 투입 권장 (박민수 → 이영희 지원)

⚡ **위험 요소:**
- 코드 리뷰 대기 중인 작업 2건 → 신속한 리뷰 필요`;
    }

    return `말씀하신 내용에 대해 분석 중입니다. 프로젝트 데이터를 기반으로 더 구체적인 질문을 주시면 도움을 드리겠습니다. 

예를 들어:
- "3단계 모델링을 위한 WBS 생성해줘"
- "현재 프로젝트의 리스크를 분석해줘"
- "이번 주 진행 상황을 요약해줘"`;
  };

  const handleSend = () => {
    if (!input.trim()) return;

    const userMessage: Message = {
      id: messages.length + 1,
      role: 'user',
      content: input,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsTyping(true);

    // Simulate AI response
    setTimeout(() => {
      const aiResponse: Message = {
        id: messages.length + 2,
        role: 'assistant',
        content: simulateAIResponse(input),
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, aiResponse]);
      setIsTyping(false);
    }, 1500);
  };

  const handleSuggestedPrompt = (prompt: string) => {
    setInput(prompt);
  };

  return (
    <div className="w-96 bg-white border-l border-gray-200 flex flex-col h-screen">
      {/* Header */}
      <div className="p-4 border-b border-gray-200 bg-gradient-to-r from-purple-600 to-blue-600 text-white">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-white/20 rounded-full flex items-center justify-center">
              <Bot size={20} />
            </div>
            <div>
              <h3 className="font-semibold">AI 어시스턴트</h3>
              <p className="text-xs text-purple-100">On-Premise LLM v2.0</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-white/20 rounded transition-colors">
            <X size={20} />
          </button>
        </div>
      </div>

      {/* Suggested Prompts */}
      {messages.length === 1 && (
        <div className="p-4 border-b border-gray-200 bg-gradient-to-b from-purple-50 to-transparent">
          <p className="text-xs text-gray-600 mb-2">추천 질문:</p>
          <div className="grid grid-cols-2 gap-2">
            {suggestedPrompts.map((prompt, idx) => (
              <button
                key={idx}
                onClick={() => handleSuggestedPrompt(prompt.prompt)}
                className="flex items-center gap-2 p-2 bg-white border border-purple-200 rounded-lg hover:bg-purple-50 hover:border-purple-400 transition-all text-left"
              >
                <div className="text-purple-600">{prompt.icon}</div>
                <span className="text-xs text-gray-700">{prompt.text}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.map((message) => (
          <div key={message.id} className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[85%] rounded-lg p-3 ${
                message.role === 'user'
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-900 border border-gray-200'
              }`}
            >
              {message.role === 'assistant' && (
                <div className="flex items-center gap-2 mb-2 text-purple-600">
                  <Sparkles size={14} />
                  <span className="text-xs font-medium">AI 분석</span>
                </div>
              )}
              <p className="text-sm whitespace-pre-wrap">{message.content}</p>
              <p className={`text-xs mt-2 ${message.role === 'user' ? 'text-blue-200' : 'text-gray-500'}`}>
                {message.timestamp.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
              </p>
            </div>
          </div>
        ))}

        {isTyping && (
          <div className="flex justify-start">
            <div className="bg-gray-100 border border-gray-200 rounded-lg p-3">
              <div className="flex items-center gap-2 text-purple-600 mb-2">
                <Sparkles size={14} />
                <span className="text-xs font-medium">AI 분석</span>
              </div>
              <div className="flex gap-1">
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className="p-4 border-t border-gray-200">
        <div className="flex gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSend()}
            placeholder="질문을 입력하세요..."
            className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
          />
          <button
            onClick={handleSend}
            disabled={!input.trim()}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
          >
            <Send size={18} />
          </button>
        </div>
        <p className="text-xs text-gray-500 mt-2">
          🔒 폐쇄망 환경 - 모든 데이터는 사내 서버에서 처리됩니다
        </p>
      </div>
    </div>
  );
}