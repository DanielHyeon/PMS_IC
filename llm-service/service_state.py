"""
LLM Service State Management - Singleton Pattern
Encapsulates global state for model, RAG service, and workflow
"""

from typing import Optional, Tuple, Any
from llama_cpp import Llama
import os
import logging
import threading

logger = logging.getLogger(__name__)


class LLMServiceState:
    """Singleton class for managing LLM service state"""

    _instance: Optional['LLMServiceState'] = None
    _lock = threading.Lock()

    def __new__(cls) -> 'LLMServiceState':
        if cls._instance is None:
            with cls._lock:
                # Double-checked locking
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if self._initialized:
            return

        self._llm: Optional[Llama] = None
        self._rag_service: Optional[Any] = None
        self._chat_workflow: Optional[Any] = None
        self._current_model_path: str = os.getenv(
            "MODEL_PATH",
            "./models/google.gemma-3-12b-pt.Q5_K_M.gguf"
        )
        self._initialized = True
        logger.info("LLMServiceState singleton initialized")

    @property
    def llm(self) -> Optional[Llama]:
        """Get the current LLM instance"""
        return self._llm

    @llm.setter
    def llm(self, value: Optional[Llama]):
        """Set the LLM instance"""
        self._llm = value

    @property
    def rag_service(self) -> Optional[Any]:
        """Get the RAG service instance"""
        return self._rag_service

    @rag_service.setter
    def rag_service(self, value: Optional[Any]):
        """Set the RAG service instance"""
        self._rag_service = value

    @property
    def chat_workflow(self) -> Optional[Any]:
        """Get the chat workflow instance"""
        return self._chat_workflow

    @chat_workflow.setter
    def chat_workflow(self, value: Optional[Any]):
        """Set the chat workflow instance"""
        self._chat_workflow = value

    @property
    def current_model_path(self) -> str:
        """Get the current model path"""
        return self._current_model_path

    @current_model_path.setter
    def current_model_path(self, value: str):
        """Set the current model path"""
        self._current_model_path = value

    @property
    def is_model_loaded(self) -> bool:
        """Check if model is loaded"""
        return self._llm is not None

    @property
    def is_rag_loaded(self) -> bool:
        """Check if RAG service is loaded"""
        return self._rag_service is not None

    @property
    def is_workflow_loaded(self) -> bool:
        """Check if chat workflow is loaded"""
        return self._chat_workflow is not None

    def get_all(self) -> Tuple[Optional[Llama], Optional[Any], Optional[Any]]:
        """Get all service instances as a tuple"""
        return self._llm, self._rag_service, self._chat_workflow

    def reset(self):
        """Reset all state (useful for testing)"""
        if self._llm is not None:
            try:
                del self._llm
            except Exception as e:
                logger.warning(f"Error deleting LLM: {e}")

        self._llm = None
        self._rag_service = None
        self._chat_workflow = None
        logger.info("LLMServiceState reset")

    def health_status(self) -> dict:
        """Get health status of all services"""
        return {
            "model_loaded": self.is_model_loaded,
            "rag_service_loaded": self.is_rag_loaded,
            "chat_workflow_loaded": self.is_workflow_loaded,
            "current_model_path": self._current_model_path
        }


# Module-level singleton instance for convenient access
_state = LLMServiceState()


def get_state() -> LLMServiceState:
    """Get the singleton state instance"""
    return _state
