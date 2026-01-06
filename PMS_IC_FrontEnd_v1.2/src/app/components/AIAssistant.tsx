import { useEffect, useRef, useState } from 'react';
import { X, Send, Bot, Sparkles, TrendingUp, FileText, AlertTriangle } from 'lucide-react';
import { UserRole } from '../App';
import { apiService } from '../../services/api';

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
  const [sessionId, setSessionId] = useState<string | null>(null);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!messagesContainerRef.current) return;
    messagesContainerRef.current.scrollTo({
      top: messagesContainerRef.current.scrollHeight,
      behavior: 'smooth',
    });
  }, [messages, isTyping]);

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

  const handleSend = async () => {
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

    try {
      const response = await apiService.sendChatMessage({
        sessionId,
        message: userMessage.content,
      });
      const aiResponse: Message = {
        id: messages.length + 2,
        role: 'assistant',
        content: response?.reply ?? '답변을 생성하지 못했습니다. 잠시 후 다시 시도해주세요.',
        timestamp: new Date(),
      };
      if (response?.sessionId) {
        setSessionId(response.sessionId);
      }
      setMessages((prev) => [...prev, aiResponse]);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          id: messages.length + 2,
          role: 'assistant',
          content: '현재 AI 서비스와 통신할 수 없습니다. 잠시 후 다시 시도해주세요.',
          timestamp: new Date(),
        },
      ]);
    } finally {
      setIsTyping(false);
    }
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
      <div ref={messagesContainerRef} className="flex-1 overflow-y-auto p-4 space-y-4">
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
