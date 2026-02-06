 'use client';

import React, { useState } from 'react';
import { Package, ShoppingCart, HelpCircle, ArrowUp } from 'lucide-react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';

export function QuickActions() {
  const { user } = useAuth();
  const [showTrackModal, setShowTrackModal] = useState(false);

  return (
    <div className="bg-white rounded-lg shadow-sm p-4 mb-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Track Order */}
        <div className="text-center">
          <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-3">
            <Package className="w-6 h-6 text-blue-600" />
          </div>
          <p className="text-sm font-medium text-gray-900">Track Package</p>
        </div>
        
        <button 
          onClick={() => setShowTrackModal(true)}
          className="w-full px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          Enter Order Number
        </button>

        {/* Track Package Modal */}
        {showTrackModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl p-6 max-w-md w-full">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Track Package</h3>
              <div className="space-y-4">
                <input
                  type="text"
                  placeholder="Enter your order number"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <div className="flex gap-3 mt-4">
                  <button
                    onClick={() => setShowTrackModal(false)}
                    className="flex-1 px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300"
                  >
                    Cancel
                  </button>
                  <button className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                    Track
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* View Cart */}
        <div className="text-center">
          <Link href="/cart" className="block">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-3">
              <ShoppingCart className="w-6 h-6 text-green-600" />
            </div>
            <p className="text-sm font-medium text-gray-900">View Cart</p>
          </Link>
        </div>

        {/* Help Center */}
        <div className="text-center">
          <Link href="/help" className="block">
            <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-3">
              <HelpCircle className="w-6 h-6 text-purple-600" />
            </div>
            <p className="text-sm font-medium text-gray-900">Help Center</p>
          </Link>
        </div>
      </div>
    </div>
  );
}