import React, { useState, useEffect, useRef } from 'react';
import { MessageSquare, AlertCircle, User, LogOut, Plus } from 'lucide-react';
import ChatMessage from './components/ChatMessage';
import ChatInput from './components/ChatInput';
import DocumentUpload from './components/DocumentUpload';
import AuthModal from './components/AuthModal';
import { 
  sendChatMessage, 
  getDocuments, 
  checkHealth, 
  isLoggedIn, 
  getUsername, 
  logout,
  createSession
} from './services/api';

/**
 * Main App Component
 * 
 * RAG Chatbot Application with:
 * - JWT Authentication (optional - guest mode available)
 * - Conversation History Persistence
 * - Document Deduplication
 * - MMR Search for diverse results
 * 
 * ARCHITECTURE:
 * ┌─────────────────────────────────────────────────────┐
 * │  React Frontend (This app)                         │
 * │    ↓ HTTP Requests (with JWT token)                │
 * │  Spring Boot Backend (port 8080)                   │
 * │    ↓ LangChain4j + MMR Search                      │
 * │  PostgreSQL + pgvector (Vector Store + History)    │
 * │    ↓ Embeddings                                    │
 * │  Ollama (Local LLM)                                │
 * └─────────────────────────────────────────────────────┘
 */
function App() {
  const [messages, setMessages] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [backendStatus, setBackendStatus] = useState('checking');
  const [error, setError] = useState(null);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [user, setUser] = useState(null);
  const [sessionId, setSessionId] = useState(null);
  const messagesEndRef = useRef(null);
  
  // Check auth state on mount
  useEffect(() => {
    if (isLoggedIn()) {
      setUser({ username: getUsername() });
    }
    // Get existing session ID
    const existingSession = localStorage.getItem('sessionId');
    if (existingSession) {
      setSessionId(existingSession);
    }
  }, []);
  
  // Check backend health on mount
  useEffect(() => {
    const checkBackend = async () => {
      try {
        const health = await checkHealth();
        setBackendStatus(health.status === 'UP' ? 'connected' : 'error');
        
        // Load documents
        const docs = await getDocuments();
        setDocuments(docs);
      } catch (err) {
        setBackendStatus('disconnected');
        setError('Cannot connect to backend. Make sure the Java server is running on port 8080.');
      }
    };
    
    checkBackend();
    const interval = setInterval(checkBackend, 30000);
    return () => clearInterval(interval);
  }, []);
  
  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);
  
  // Handle auth success
  const handleAuthSuccess = () => {
    setUser({ username: getUsername() });
  };
  
  // Handle logout
  const handleLogout = () => {
    logout();
    setUser(null);
    setMessages([]);
    setSessionId(null);
  };
  
  // Start new conversation
  const handleNewChat = async () => {
    try {
      const response = await createSession();
      setSessionId(response.sessionId);
      setMessages([]);
    } catch (err) {
      console.error('Failed to create session:', err);
      // Just clear messages locally if API fails
      setMessages([]);
      localStorage.removeItem('sessionId');
    }
  };
  
  // Send message handler
  const handleSendMessage = async (content) => {
    const userMessage = { role: 'user', content };
    setMessages(prev => [...prev, userMessage]);
    setIsLoading(true);
    setError(null);
    
    try {
      const history = messages.slice(-6).map(m => ({
        role: m.role,
        content: m.content
      }));
      
      const response = await sendChatMessage(content, history, sessionId);
      
      // Update session ID from response
      if (response.sessionId) {
        setSessionId(response.sessionId);
      }
      
      const assistantMessage = {
        role: 'assistant',
        content: response.answer,
        sources: response.sources,
        responseTime: response.responseTimeMs,
        contextChunks: response.contextChunksUsed
      };
      setMessages(prev => [...prev, assistantMessage]);
    } catch (err) {
      console.error('Chat error:', err);
      setError(err.response?.data?.error || 'Failed to get response. Please try again.');
      
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, I encountered an error processing your request. Please try again.'
      }]);
    } finally {
      setIsLoading(false);
    }
  };
  
  return (
    <div className="flex h-screen bg-gray-100">
      {/* Auth Modal */}
      <AuthModal 
        isOpen={showAuthModal}
        onClose={() => setShowAuthModal(false)}
        onAuthSuccess={handleAuthSuccess}
      />
      
      {/* Sidebar */}
      <div className="w-80 bg-white border-r border-gray-200 flex flex-col">
        {/* Logo/Header */}
        <div className="p-4 border-b border-gray-200">
          <h1 className="text-xl font-bold text-gray-800 flex items-center gap-2">
            <MessageSquare className="w-6 h-6 text-blue-500" />
            RAG Chatbot
          </h1>
        </div>
        
        {/* New Chat Button */}
        <div className="p-3 border-b border-gray-200">
          <button
            onClick={handleNewChat}
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-500 hover:bg-blue-600 text-white rounded-lg transition-colors font-medium"
          >
            <Plus className="w-5 h-5" />
            New Chat
          </button>
        </div>
        
        {/* Document Upload */}
        <div className="flex-1 overflow-auto">
          <DocumentUpload 
            documents={documents} 
            onDocumentsChange={setDocuments}
          />
        </div>
        
        {/* Footer with Auth */}
        <div className="p-4 border-t border-gray-200 bg-gray-50">
          {/* User Info / Auth */}
          {user ? (
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                  <User className="w-4 h-4 text-white" />
                </div>
                <span className="text-sm font-medium text-gray-700">{user.username}</span>
              </div>
              <button
                onClick={handleLogout}
                className="text-gray-500 hover:text-red-500 transition-colors"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowAuthModal(true)}
              className="w-full mb-3 flex items-center justify-center gap-2 px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors text-sm font-medium"
            >
              <User className="w-4 h-4" />
              Login / Sign Up
            </button>
          )}
          
          {/* Backend Status */}
          <div className="text-xs text-gray-500">
            <div className="flex items-center gap-2">
              {backendStatus === 'connected' && (
                <span className="flex items-center gap-1 text-green-600">
                  <span className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
                  Online
                </span>
              )}
              {backendStatus === 'checking' && (
                <span className="flex items-center gap-1 text-yellow-600">
                  <span className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse"></span>
                  Connecting...
                </span>
              )}
              {backendStatus === 'disconnected' && (
                <span className="flex items-center gap-1 text-red-600">
                  <span className="w-2 h-2 bg-red-500 rounded-full"></span>
                  Offline
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
      
      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col">
        {/* Chat Header */}
        <div className="h-14 border-b border-gray-200 bg-white flex items-center justify-between px-6">
          <div>
            <h2 className="font-semibold text-gray-800">Chat</h2>
            <p className="text-xs text-gray-500">
              {documents.length} document{documents.length !== 1 ? 's' : ''} loaded
              {sessionId && <span className="ml-2 text-gray-400">• Session active</span>}
            </p>
          </div>
          <div className="flex items-center gap-2 text-xs text-gray-400">
            <span className="px-2 py-1 bg-emerald-50 text-emerald-700 rounded">MMR Search</span>
            <span className="px-2 py-1 bg-blue-50 text-blue-700 rounded">Deduplication</span>
          </div>
        </div>
        
        {/* Messages Area */}
        <div className="flex-1 overflow-auto bg-gray-50">
          {messages.length === 0 ? (
            <div className="flex items-center justify-center h-full text-gray-500">
              <div className="text-center">
                <MessageSquare className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                <h3 className="text-lg font-medium text-gray-700">Start a Conversation</h3>
                <p className="mt-2 text-sm max-w-md">
                  Upload documents to the left, then ask questions about them.
                  The AI will use RAG with MMR search for diverse, relevant answers.
                </p>
                {!user && (
                  <p className="mt-2 text-xs text-gray-400">
                    Login to save your conversation history
                  </p>
                )}
                {backendStatus === 'disconnected' && (
                  <div className="mt-4 p-3 bg-red-50 rounded-lg text-red-700 text-sm">
                    <AlertCircle className="w-4 h-4 inline mr-2" />
                    Backend not connected. Start the Java server first.
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div>
              {messages.map((msg, idx) => (
                <ChatMessage key={idx} message={msg} />
              ))}
              
              {isLoading && (
                <div className="flex justify-start p-4">
                  <div className="flex gap-3 max-w-[80%]">
                    <div className="w-9 h-9 rounded-full bg-emerald-500 flex items-center justify-center shadow-sm">
                      <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    </div>
                    <div className="flex flex-col items-start">
                      <div className="font-medium text-xs text-gray-500 mb-1 px-1">AI Assistant</div>
                      <div className="bg-white border border-gray-200 rounded-2xl rounded-bl-md px-4 py-3 shadow-sm">
                        <div className="flex items-center gap-2 text-gray-500 text-sm">
                          <span className="flex gap-1">
                            <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{animationDelay: '0ms'}}></span>
                            <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{animationDelay: '150ms'}}></span>
                            <span className="w-2 h-2 bg-emerald-400 rounded-full animate-bounce" style={{animationDelay: '300ms'}}></span>
                          </span>
                          <span>Thinking...</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              )}
              
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>
        
        {/* Error Display */}
        {error && (
          <div className="px-4 py-2 bg-red-50 border-t border-red-200">
            <p className="text-sm text-red-700 flex items-center gap-2">
              <AlertCircle className="w-4 h-4" />
              {error}
            </p>
          </div>
        )}
        
        {/* Chat Input */}
        <ChatInput 
          onSend={handleSendMessage}
          isLoading={isLoading}
          disabled={backendStatus === 'disconnected'}
        />
      </div>
    </div>
  );
}

export default App;
