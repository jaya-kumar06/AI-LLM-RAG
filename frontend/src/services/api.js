import axios from 'axios';

/**
 * API Service
 * 
 * Handles all HTTP communication with the Java Spring Boot backend.
 * 
 * ENDPOINTS:
 * - POST /api/chat         - Send chat message (with RAG + MMR)
 * - POST /api/documents    - Upload document (with deduplication)
 * - GET  /api/documents    - List all documents
 * - GET  /api/health       - Health check
 * - POST /api/auth/login   - Login and get JWT token
 * - POST /api/auth/register - Register new user
 * - GET  /api/history/{id} - Get session history
 */

const API_BASE_URL = '/api';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 120000, // 2 minutes (LLM can be slow)
});

// Add auth token to requests if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ============================================
// Authentication APIs
// ============================================

/**
 * Register a new user
 */
export const register = async (username, password, email = null) => {
  try {
    const response = await api.post('/auth/register', {
      username,
      password,
      email,
    });
    // Store token
    if (response.data.token) {
      localStorage.setItem('authToken', response.data.token);
      localStorage.setItem('username', response.data.username);
    }
    return response.data;
  } catch (error) {
    console.error('Register error:', error);
    throw error;
  }
};

/**
 * Login and get JWT token
 */
export const login = async (username, password) => {
  try {
    const response = await api.post('/auth/login', {
      username,
      password,
    });
    // Store token
    if (response.data.token) {
      localStorage.setItem('authToken', response.data.token);
      localStorage.setItem('username', response.data.username);
    }
    return response.data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
};

/**
 * Logout - clear stored token
 */
export const logout = () => {
  localStorage.removeItem('authToken');
  localStorage.removeItem('username');
  localStorage.removeItem('sessionId');
};

/**
 * Check if user is logged in
 */
export const isLoggedIn = () => {
  return !!localStorage.getItem('authToken');
};

/**
 * Get current username
 */
export const getUsername = () => {
  return localStorage.getItem('username');
};

// ============================================
// Chat APIs
// ============================================

/**
 * Send a chat message and get AI response
 * 
 * @param {string} question - User's question
 * @param {Array} conversationHistory - Previous messages
 * @param {string} sessionId - Session ID for history persistence
 * @returns {Promise<Object>} - Chat response with answer and sources
 */
export const sendChatMessage = async (question, conversationHistory = [], sessionId = null) => {
  try {
    // Get or create session ID
    if (!sessionId) {
      sessionId = localStorage.getItem('sessionId');
    }
    
    const response = await api.post('/chat', {
      question,
      conversationHistory,
      sessionId,
    });
    
    // Save session ID from response
    if (response.data.sessionId) {
      localStorage.setItem('sessionId', response.data.sessionId);
    }
    
    return response.data;
  } catch (error) {
    console.error('Chat API error:', error);
    throw error;
  }
};

/**
 * Create a new chat session
 */
export const createSession = async () => {
  try {
    const response = await api.post('/history/session');
    if (response.data.sessionId) {
      localStorage.setItem('sessionId', response.data.sessionId);
    }
    return response.data;
  } catch (error) {
    console.error('Create session error:', error);
    throw error;
  }
};

/**
 * Get session history
 */
export const getSessionHistory = async (sessionId) => {
  try {
    const response = await api.get(`/history/${sessionId}`);
    return response.data;
  } catch (error) {
    console.error('Get history error:', error);
    throw error;
  }
};

// ============================================
// Document APIs
// ============================================

/**
 * Upload a document for processing
 * 
 * The backend will:
 * 1. Check for duplicates (SHA-256 hash)
 * 2. Extract text from the document
 * 3. Split into chunks
 * 4. Generate embeddings
 * 5. Store in PostgreSQL + pgvector
 * 
 * @param {File} file - The file to upload
 * @returns {Promise<Object>} - Document info with processing status
 */
export const uploadDocument = async (file) => {
  try {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await api.post('/documents', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 300000, // 5 minutes for large files
    });
    return response.data;
  } catch (error) {
    console.error('Upload API error:', error);
    throw error;
  }
};

/**
 * Get list of all uploaded documents
 */
export const getDocuments = async () => {
  try {
    const response = await api.get('/documents');
    return response.data;
  } catch (error) {
    console.error('Documents API error:', error);
    throw error;
  }
};

/**
 * Delete a document
 */
export const deleteDocument = async (id) => {
  try {
    const response = await api.delete(`/documents/${id}`);
    return response.data;
  } catch (error) {
    console.error('Delete API error:', error);
    throw error;
  }
};

// ============================================
// Health Check
// ============================================

/**
 * Health check
 */
export const checkHealth = async () => {
  try {
    const response = await api.get('/health');
    return response.data;
  } catch (error) {
    console.error('Health check error:', error);
    throw error;
  }
};

export default api;
