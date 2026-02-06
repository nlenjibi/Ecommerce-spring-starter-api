'use client';

import React from 'react';

interface LoadingSkeletonsProps {
  type: 'overview' | 'orders' | 'wallet' | 'wishlist';
}

export function LoadingSkeletons({ type }: LoadingSkeletonsProps) {
  return (
    <div className="space-y-6">
      {type === 'overview' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white rounded-lg shadow-sm p-6 animate-pulse">
              <div className="h-8 bg-gray-200 rounded-lg mb-4 w-full"></div>
              <div className="h-4 bg-gray-300 rounded-lg mb-2 w-3/4"></div>
              <div className="h-4 bg-gray-300 rounded-lg w-2/3"></div>
              <div className="h-4 bg-gray-300 rounded-lg w-1/2"></div>
            </div>
          ))}
        </div>
      )}

      {type === 'orders' && (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-lg shadow-sm p-6">
              <div className="flex items-center justify-between mb-4">
                <div className="h-6 bg-gray-200 rounded-lg w-24"></div>
                <div className="h-4 bg-gray-300 rounded-lg w-3/4"></div>
                <div className="h-4 bg-gray-300 rounded-lg w-1/4"></div>
              </div>
              <div className="space-y-2">
                <div className="h-3 bg-gray-300 rounded-lg"></div>
                <div className="h-3 bg-gray-300 rounded-lg"></div>
              </div>
            </div>
          ))}
        </div>
      )}

      {type === 'wallet' && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-lg shadow-sm p-6">
              <div className="h-8 bg-gray-200 rounded-lg mb-4 w-full flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
              </div>
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-gray-300 rounded-lg w-1/2"></div>
                <div className="h-4 bg-gray-300 rounded-lg"></div>
                <div className="h-4 bg-gray-300 rounded-lg w-3/4"></div>
              </div>
            </div>
          ))}
        </div>
      )}

      {type === 'wishlist' && (
        <div className="space-y-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white rounded-lg shadow-sm p-6">
              <div className="flex items-center justify-center">
                <div className="w-16 h-16 bg-gray-200 rounded-lg mr-4"></div>
                <div className="space-y-2">
                  <div className="h-3 bg-gray-300 rounded-lg w-full"></div>
                  <div className="h-3 bg-gray-300 rounded-lg w-full"></div>
                  <div className="h-3 bg-gray-300 rounded-lg"></div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

interface EmptyStatesProps {
  type: 'cart' | 'orders' | 'wishlist';
}

export function EmptyStates({ type }: EmptyStatesProps) {
  const getEmptyConfig = (t: string) => {
    const configs: Record<string, { title: string; description: string; icon: string; action: string }> = {
      cart: {
        title: 'Your Cart is Empty',
        description: 'Add some products to get started',
        icon: '🛒',
        action: 'Browse Products',
      },
      orders: {
        title: 'No Orders Yet',
        description: 'Your first order will appear here once you complete a purchase',
        icon: '📦',
        action: 'Start Shopping',
      },
      wishlist: {
        title: 'Your Wishlist is Empty',
        description: 'Save items you love for later',
        icon: '❤️',
        action: 'Browse Products',
      },
    };
    return configs[t] || configs.wishlist;
  };

  const config = getEmptyConfig(type);

  return (
    <div className="text-center py-12">
      <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <span className="text-4xl">{config.icon}</span>
      </div>
      <h2 className="text-2xl font-bold text-gray-900 mb-2">{config.title}</h2>
      <p className="text-gray-600 mb-6">{config.description}</p>
      <button className="px-8 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium">
        {config.action}
      </button>
    </div>
  );
}