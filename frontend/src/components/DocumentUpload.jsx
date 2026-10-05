import React, { useState, useRef } from 'react';
import { Upload, File, CheckCircle, XCircle, Loader2, Trash2 } from 'lucide-react';
import { uploadDocument, getDocuments, deleteDocument } from '../services/api';

/**
 * DocumentUpload Component
 * 
 * Handles document upload and displays list of uploaded documents.
 * Supported formats: PDF, DOCX, TXT
 * 
 * Props:
 * - documents: Array of uploaded documents
 * - onDocumentsChange: Callback when documents list changes
 */
function DocumentUpload({ documents, onDocumentsChange }) {
  const [uploading, setUploading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);
  
  const handleFileSelect = async (files) => {
    if (!files || files.length === 0) return;
    
    const file = files[0];
    const validTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain'];
    const validExtensions = ['.pdf', '.docx', '.txt'];
    
    const extension = '.' + file.name.split('.').pop().toLowerCase();
    if (!validExtensions.includes(extension)) {
      setUploadStatus({ type: 'error', message: 'Unsupported file type. Please upload PDF, DOCX, or TXT files.' });
      return;
    }
    
    setUploading(true);
    setUploadStatus({ type: 'info', message: `Processing ${file.name}...` });
    
    try {
      const result = await uploadDocument(file);
      setUploadStatus({ 
        type: 'success', 
        message: `Successfully processed ${file.name} (${result.chunkCount} chunks created)` 
      });
      
      // Refresh document list
      const updatedDocs = await getDocuments();
      onDocumentsChange(updatedDocs);
    } catch (error) {
      setUploadStatus({ 
        type: 'error', 
        message: error.response?.data?.error || 'Failed to upload document' 
      });
    } finally {
      setUploading(false);
    }
  };
  
  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    handleFileSelect(e.dataTransfer.files);
  };
  
  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };
  
  const handleDragLeave = () => {
    setDragOver(false);
  };
  
  const handleDelete = async (docId, filename) => {
    if (!confirm(`Delete ${filename}?`)) return;
    
    try {
      await deleteDocument(docId);
      const updatedDocs = await getDocuments();
      onDocumentsChange(updatedDocs);
    } catch (error) {
      console.error('Delete failed:', error);
    }
  };
  
  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };
  
  return (
    <div className="p-4 border-b border-gray-200 bg-gray-50">
      <h3 className="font-semibold text-gray-700 mb-3 flex items-center gap-2">
        <File className="w-4 h-4" />
        Documents ({documents.length})
      </h3>
      
      {/* Upload Area */}
      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => fileInputRef.current?.click()}
        className={`border-2 border-dashed rounded-lg p-4 text-center cursor-pointer transition-colors ${
          dragOver ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'
        } ${uploading ? 'opacity-50 cursor-not-allowed' : ''}`}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".pdf,.docx,.txt"
          onChange={(e) => handleFileSelect(e.target.files)}
          className="hidden"
          disabled={uploading}
        />
        
        {uploading ? (
          <div className="flex items-center justify-center gap-2 text-blue-600">
            <Loader2 className="w-5 h-5 animate-spin" />
            <span>Processing...</span>
          </div>
        ) : (
          <div className="text-gray-500">
            <Upload className="w-6 h-6 mx-auto mb-2" />
            <p className="text-sm">Drop files here or click to upload</p>
            <p className="text-xs mt-1">PDF, DOCX, TXT (max 10MB)</p>
          </div>
        )}
      </div>
      
      {/* Upload Status */}
      {uploadStatus && (
        <div className={`mt-3 p-2 rounded text-sm flex items-center gap-2 ${
          uploadStatus.type === 'success' ? 'bg-green-50 text-green-700' :
          uploadStatus.type === 'error' ? 'bg-red-50 text-red-700' :
          'bg-blue-50 text-blue-700'
        }`}>
          {uploadStatus.type === 'success' && <CheckCircle className="w-4 h-4" />}
          {uploadStatus.type === 'error' && <XCircle className="w-4 h-4" />}
          {uploadStatus.type === 'info' && <Loader2 className="w-4 h-4 animate-spin" />}
          <span>{uploadStatus.message}</span>
        </div>
      )}
      
      {/* Document List */}
      {documents.length > 0 && (
        <div className="mt-4 space-y-2">
          {documents.map((doc) => (
            <div key={doc.id} className="flex items-center justify-between p-2 bg-white rounded border">
              <div className="flex items-center gap-2 overflow-hidden">
                <File className="w-4 h-4 text-gray-400 flex-shrink-0" />
                <div className="overflow-hidden">
                  <p className="text-sm font-medium truncate">{doc.filename}</p>
                  <p className="text-xs text-gray-500">
                    {formatFileSize(doc.fileSize)} • {doc.chunkCount} chunks
                  </p>
                </div>
              </div>
              <button
                onClick={() => handleDelete(doc.id, doc.filename)}
                className="p-1 text-gray-400 hover:text-red-500 transition-colors"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
      
      {documents.length === 0 && (
        <p className="mt-3 text-sm text-gray-500 text-center">
          No documents uploaded yet. Upload documents to enable RAG.
        </p>
      )}
    </div>
  );
}

export default DocumentUpload;
