import { useState, useEffect } from 'react';
import { UserRole } from '../App';
import { Cpu, AlertCircle, CheckCircle, Loader, RefreshCw } from 'lucide-react';

interface SettingsProps {
  userRole: UserRole;
}

interface ModelInfo {
  name: string;
  displayName: string;
  size: string;
  description: string;
}

const availableModels: ModelInfo[] = [
  {
    name: 'LFM2-2.6B-Uncensored-X64.i1-Q6_K.gguf',
    displayName: 'LFM2 2.6B (빠른 응답)',
    size: '2.6B',
    description: '가벼운 모델로 빠른 응답 속도를 제공합니다. 간단한 질문에 적합합니다.'
  },
  {
    name: 'google.gemma-3-12b-pt.Q5_K_M.gguf',
    displayName: 'Gemma 3 12B Q5 (균형)',
    size: '12B',
    description: '속도와 성능의 균형을 제공합니다. 일반적인 업무에 권장됩니다.'
  },
  {
    name: 'google.gemma-3-12b-pt.Q6_K.gguf',
    displayName: 'Gemma 3 12B Q6 (고성능)',
    size: '12B',
    description: '최고 품질의 응답을 제공합니다. 복잡한 분석 작업에 적합합니다.'
  }
];

export default function Settings({ userRole }: SettingsProps) {
  const [currentModel, setCurrentModel] = useState<string>('');
  const [selectedModel, setSelectedModel] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
  const [isChangingModel, setIsChangingModel] = useState(false);

  const isAdmin = userRole === 'admin';
  const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

  const getAuthHeaders = () => {
    const token = localStorage.getItem('auth_token');
    return token ? { Authorization: `Bearer ${token}` } : {};
  };

  const extractModelName = (modelPath?: string) => {
    if (!modelPath) {
      return '';
    }
    const parts = modelPath.split('/');
    return parts[parts.length - 1];
  };

  useEffect(() => {
    fetchCurrentModel();
  }, []);

  const fetchCurrentModel = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${apiBaseUrl}/llm/model`, {
        headers: {
          ...getAuthHeaders(),
        },
      });
      if (response.ok) {
        const data = await response.json();
        const modelName = extractModelName(data?.data?.currentModel || data?.currentModel);
        setCurrentModel(modelName);
        setSelectedModel(modelName);
      } else {
        const error = await response.json().catch(() => ({}));
        setStatusMessage({ type: 'error', text: error.message || '현재 모델 정보를 가져올 수 없습니다.' });
      }
    } catch (error) {
      console.error('Failed to fetch current model:', error);
      setStatusMessage({ type: 'error', text: '현재 모델 정보를 가져올 수 없습니다.' });
    } finally {
      setLoading(false);
    }
  };

  const handleModelChange = async () => {
    if (!selectedModel || selectedModel === currentModel) {
      return;
    }

    setIsChangingModel(true);
    setStatusMessage({ type: 'info', text: '모델을 변경하는 중입니다. 잠시만 기다려주세요...' });

    try {
      const response = await fetch(`${apiBaseUrl}/llm/model`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          ...getAuthHeaders(),
        },
        body: JSON.stringify({ modelPath: selectedModel }),
      });

      if (response.ok) {
        const data = await response.json();
        const modelName = extractModelName(data?.data?.currentModel || selectedModel);
        setCurrentModel(modelName);
        setSelectedModel(modelName);
        setStatusMessage({ type: 'success', text: `모델이 성공적으로 변경되었습니다: ${getModelDisplayName(selectedModel)}` });
      } else {
        const error = await response.json();
        setStatusMessage({ type: 'error', text: `모델 변경 실패: ${error.message || '알 수 없는 오류'}` });
      }
    } catch (error) {
      console.error('Failed to change model:', error);
      setStatusMessage({ type: 'error', text: '모델 변경 중 오류가 발생했습니다.' });
    } finally {
      setIsChangingModel(false);
    }
  };

  const getModelDisplayName = (modelPath: string): string => {
    const model = availableModels.find(m => m.name === modelPath);
    return model ? model.displayName : modelPath;
  };

  if (!isAdmin) {
    return (
      <div className="p-8">
        <div className="max-w-4xl mx-auto">
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
            <div className="flex items-center gap-3">
              <AlertCircle className="text-yellow-600" size={24} />
              <div>
                <h3 className="font-semibold text-yellow-900">접근 권한 없음</h3>
                <p className="text-yellow-700 mt-1">설정 페이지는 시스템 관리자만 접근할 수 있습니다.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">시스템 설정</h1>
          <p className="text-gray-600">AI 모델 및 시스템 구성을 관리합니다.</p>
        </div>

        {/* LLM 모델 설정 섹션 */}
        <div className="bg-white rounded-lg shadow-md border border-gray-200">
          <div className="p-6 border-b border-gray-200">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                <Cpu className="text-purple-600" size={20} />
              </div>
              <div>
                <h2 className="text-xl font-semibold text-gray-900">AI 모델 설정</h2>
                <p className="text-sm text-gray-600">사용할 LLM 모델을 선택하세요</p>
              </div>
            </div>
          </div>

          <div className="p-6">
            {loading ? (
              <div className="flex items-center justify-center py-12">
                <Loader className="animate-spin text-gray-400" size={32} />
              </div>
            ) : (
              <>
                {/* 현재 모델 정보 */}
                <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <div className="flex items-center gap-2 mb-2">
                    <CheckCircle className="text-blue-600" size={16} />
                    <span className="text-sm font-medium text-blue-900">현재 활성 모델</span>
                  </div>
                  <p className="text-blue-700 font-semibold">{getModelDisplayName(currentModel)}</p>
                  <p className="text-sm text-blue-600 mt-1">{currentModel}</p>
                </div>

                {/* 모델 선택 */}
                <div className="space-y-3">
                  <label className="block text-sm font-medium text-gray-700 mb-3">
                    모델 선택
                  </label>
                  {availableModels.map((model) => (
                    <div
                      key={model.name}
                      onClick={() => setSelectedModel(model.name)}
                      className={`p-4 border-2 rounded-lg cursor-pointer transition-all ${
                        selectedModel === model.name
                          ? 'border-purple-500 bg-purple-50'
                          : 'border-gray-200 hover:border-gray-300 bg-white'
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <h3 className="font-semibold text-gray-900">{model.displayName}</h3>
                            <span className="px-2 py-0.5 bg-gray-100 text-gray-700 text-xs rounded-full">
                              {model.size}
                            </span>
                            {currentModel === model.name && (
                              <span className="px-2 py-0.5 bg-green-100 text-green-700 text-xs rounded-full">
                                활성
                              </span>
                            )}
                          </div>
                          <p className="text-sm text-gray-600 mt-1">{model.description}</p>
                          <p className="text-xs text-gray-400 mt-2 font-mono">{model.name}</p>
                        </div>
                        <div className="ml-4">
                          <div
                            className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                              selectedModel === model.name
                                ? 'border-purple-500 bg-purple-500'
                                : 'border-gray-300'
                            }`}
                          >
                            {selectedModel === model.name && (
                              <div className="w-2 h-2 bg-white rounded-full"></div>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* 상태 메시지 */}
                {statusMessage && (
                  <div
                    className={`mt-4 p-4 rounded-lg ${
                      statusMessage.type === 'success'
                        ? 'bg-green-50 border border-green-200'
                        : statusMessage.type === 'error'
                        ? 'bg-red-50 border border-red-200'
                        : 'bg-blue-50 border border-blue-200'
                    }`}
                  >
                    <p
                      className={`text-sm ${
                        statusMessage.type === 'success'
                          ? 'text-green-700'
                          : statusMessage.type === 'error'
                          ? 'text-red-700'
                          : 'text-blue-700'
                      }`}
                    >
                      {statusMessage.text}
                    </p>
                  </div>
                )}

                {/* 액션 버튼 */}
                <div className="mt-6 flex gap-3">
                  <button
                    onClick={handleModelChange}
                    disabled={isChangingModel || selectedModel === currentModel}
                    className={`flex items-center gap-2 px-6 py-2.5 rounded-lg font-medium transition-all ${
                      isChangingModel || selectedModel === currentModel
                        ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                        : 'bg-purple-600 text-white hover:bg-purple-700 shadow-md hover:shadow-lg'
                    }`}
                  >
                    {isChangingModel ? (
                      <>
                        <Loader className="animate-spin" size={18} />
                        <span>모델 변경 중...</span>
                      </>
                    ) : (
                      <>
                        <RefreshCw size={18} />
                        <span>모델 변경</span>
                      </>
                    )}
                  </button>
                  <button
                    onClick={fetchCurrentModel}
                    disabled={isChangingModel}
                    className="px-6 py-2.5 border-2 border-gray-300 text-gray-700 rounded-lg font-medium hover:border-gray-400 hover:bg-gray-50 transition-all"
                  >
                    새로고침
                  </button>
                </div>
              </>
            )}
          </div>
        </div>

        {/* 추가 설정 섹션 (향후 확장 가능) */}
        <div className="mt-6 p-4 bg-gray-50 border border-gray-200 rounded-lg">
          <p className="text-sm text-gray-600">
            <strong>참고:</strong> 모델 변경 시 LLM 서비스가 재시작되므로 약 10-30초 정도 소요될 수 있습니다.
            변경 중에는 AI 채팅 기능을 사용할 수 없습니다.
          </p>
        </div>
      </div>
    </div>
  );
}
