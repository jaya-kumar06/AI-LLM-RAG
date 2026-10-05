import React from 'react';
import ReactMarkdown from 'react-markdown';
import { User, Bot, Clock, FileText } from 'lucide-react';

/**
 * ChatMessage Component
 * 
 * Displays a single message in the chat.
 * - User messages: Right side, blue background
 * - AI messages: Left side, gray background
 * 
 * Props:
 * - message: { role: 'user'|'assistant', content: string, sources?: string[], responseTime?: number }
 */
function ChatMessage({ message }) {
  const isUser = message.role === 'user';
  
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} p-4`}>
      <div className={`flex gap-3 max-w-[80%] ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
        {/* Avatar */}
        <div className={`flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center shadow-sm ${
          isUser ? 'bg-blue-500' : 'bg-emerald-500'
        }`}>
          {isUser ? (
            <User className="w-5 h-5 text-white" />
          ) : (
            <Bot className="w-5 h-5 text-white" />
          )}
        </div>
        
        {/* Content */}
        <div className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}>
          <div className="font-medium text-xs text-gray-500 mb-1 px-1">
            {isUser ? 'You' : 'AI Assistant'}
          </div>
          
          {/* Message bubble */}
          <div className={`rounded-2xl px-4 py-3 shadow-sm ${
            isUser 
              ? 'bg-blue-500 text-white rounded-br-md' 
              : 'bg-white border border-gray-200 text-gray-800 rounded-bl-md'
          }`}>
            {/* Message content with markdown support */}
            <div className={`prose prose-sm max-w-none ${
              isUser ? 'prose-invert' : ''
            }`}>
              {isUser ? (
                <p className="m-0">{message.content}</p>
              ) : (
                <ReactMarkdown 
                  components={{
                    p: ({node, ...props}) => <p className="m-0 mb-2 last:mb-0" {...props} />,
                    ul: ({node, ...props}) => <ul className="m-0 mb-2 pl-4" {...props} />,
                    ol: ({node, ...props}) => <ol className="m-0 mb-2 pl-4" {...props} />,
                    li: ({node, ...props}) => <li className="m-0" {...props} />,
                  }}
                >
                  {message.content}
                </ReactMarkdown>
              )}
            </div>
          </div>
          
          {/* Sources (for assistant messages) */}
          {!isUser && message.sources && message.sources.length > 0 && (
            <div className="mt-2 px-1">
              <div className="flex items-center gap-1 text-xs text-gray-400 mb-1">
                <FileText className="w-3 h-3" />
                <span>Sources:</span>
              </div>
              <div className="flex flex-wrap gap-1">
                {message.sources.map((source, idx) => (
                  <span 
                    key={idx}
                    className="px-2 py-0.5 bg-emerald-50 text-emerald-700 text-xs rounded-full border border-emerald-200"
                  >
                    {source}
                  </span>
                ))}
              </div>
            </div>
          )}
          
          {/* Response time */}
          {!isUser && message.responseTime && (
            <div className="flex items-center gap-1 mt-1 px-1 text-xs text-gray-400">
              <Clock className="w-3 h-3" />
              <span>{message.responseTime}ms</span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default ChatMessage;
