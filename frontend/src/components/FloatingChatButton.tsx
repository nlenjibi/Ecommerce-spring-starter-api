'use client';

import React, { useContext } from 'react';
import { MessageCircle, X, Github, Send, HelpCircle } from 'lucide-react';
import { LiveSupportContext } from '@/context/SocialLinksContext';

interface FloatingChatButtonProps {
  position?: 'bottom-left' | 'bottom-right';
}

export function FloatingChatButton({ position = 'bottom-right' }: FloatingChatButtonProps) {
  const context = useContext(LiveSupportContext);
  const [isOpen, setIsOpen] = React.useState(false);

  if (!context?.chatConfigs || context.chatConfigs.length === 0) {
    return null;
  }

  const positionClass = position === 'bottom-left' ? 'left-6' : 'right-6';

  const getIconComponent = (type: string) => {
    switch (type) {
      case 'whatsapp':
        return '💬';
      case 'facebook':
        return 'f';
      case 'telegram':
        return '✈️';
      case 'live_chat':
        return '💬';
      default:
        return '💭';
    }
  };

  const openChat = (url: string) => {
    if (url) {
      window.open(url, '_blank');
    }
  };

  return (
    <div className={`fixed ${positionClass} bottom-6 z-50`}>
      {/* Chat Menu */}
      {isOpen && (
        <div className="absolute bottom-20 bg-white rounded-lg shadow-lg border mb-4 w-64 overflow-hidden animate-in fade-in slide-in-from-bottom-4 duration-200">
          {/* Help Section */}
          {context?.helpLinks && context.helpLinks.length > 0 && (
            <>
              <div className="p-4 border-b bg-gradient-to-r from-blue-50 to-purple-50">
                <h3 className="font-semibold text-sm flex items-center gap-2">
                  <HelpCircle size={16} />
                  Help Center
                </h3>
              </div>

              <div className="max-h-40 overflow-y-auto">
                {context.helpLinks.map((link) => (
                  <a
                    key={link.id}
                    href={link.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 border-b last:border-b-0 transition"
                  >
                    {link.title}
                  </a>
                ))}
              </div>
            </>
          )}

          {/* Chat Options */}
          <div className="p-4 space-y-2 bg-gray-50 border-t">
            <p className="text-xs font-semibold text-gray-600 mb-3">Contact Us</p>
            {context.chatConfigs.map((config) => (
              <button
                key={config.id}
                onClick={() => openChat(config.url)}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg hover:bg-blue-50 text-left text-sm transition"
              >
                <span className="text-lg">{getIconComponent(config.type)}</span>
                <span className="text-gray-700">{config.title}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Main Button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className={`relative w-14 h-14 rounded-full shadow-lg hover:shadow-xl transition-all flex items-center justify-center text-white font-bold text-xl ${
          isOpen
            ? 'bg-red-500 hover:bg-red-600'
            : 'bg-gradient-to-br from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700'
        }`}
        aria-label="Toggle live chat"
      >
        {isOpen ? (
          <X size={24} />
        ) : (
          <MessageCircle size={24} />
        )}
      </button>

      {/* Pulse animation when closed */}
      {!isOpen && (
        <div className="absolute inset-0 w-14 h-14 rounded-full animate-pulse bg-blue-400 opacity-20" />
      )}
    </div>
  );
}
