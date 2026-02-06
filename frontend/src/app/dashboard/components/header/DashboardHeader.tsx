'use client';

import React, { useState } from 'react';
import { Search, ShoppingCart, Bell, User, ChevronDown, Settings, LogOut } from 'lucide-react';
import { useCart } from '@/context/CartContext';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';

export function DashboardHeader() {
  const { user } = useAuth();
  const { cartItems } = useCart();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [isCartOpen, setIsCartOpen] = useState(false);

  return (
    <div className="bg-white border-b border-gray-200 sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <Link href="/dashboard" className="flex items-center">
              <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-xl">S</span>
              </div>
              <span className="ml-3 text-xl font-semibold text-gray-900">hopHub</span>
            </Link>

            {/* Search Bar */}
            <div className="flex-1 max-w-md mx-8">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search products, orders..."
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            {/* Navigation Items */}
            <div className="flex items-center gap-6">
              {/* Cart */}
              <div className="relative">
                <Link href="/cart" className="flex items-center text-gray-700 hover:text-blue-600">
                  <ShoppingCart className="w-6 h-6" />
                  <span className="ml-2">Cart</span>
                  {cartItems && cartItems.length > 0 && (
                    <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full px-2">
                      {cartItems.length}
                    </span>
                  )}
                </Link>
              </div>

              {/* Notifications */}
              <div className="relative">
                <button 
                  onClick={() => setIsNotificationsOpen(!isNotificationsOpen)}
                  className="flex items-center text-gray-700 hover:text-blue-600"
                >
                  <Bell className="w-6 h-6" />
                  {/* Notification Badge */}
                  {/* {user?.notifications?.length > 0 && (
                    <span className="absolute -top-1 -right-1 bg-blue-500 text-white text-xs rounded-full px-2">
                      {user.notifications.length}
                    </span>
                  )} */}
                </button>
              </div>

              {/* User Menu */}
              <div className="relative">
                <button 
                  onClick={() => setIsMenuOpen(!isMenuOpen)}
                  className="flex items-center text-gray-700 hover:text-blue-600"
                >
                  {user?.avatar ? (
                    <img 
                      src={user.avatar} 
                      alt={(user as any)?.name || (user as any)?.firstName || ''}
                      className="w-8 h-8 rounded-full"
                    />
                  ) : (
                    <User className="w-8 h-8" />
                  )}
                  <ChevronDown className="ml-2 w-4 h-4" />
                </button>

                {/* Dropdown Menu */}
                {isMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-50">
                    <div className="px-4 py-2">
                      <Link href="/dashboard" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                        <div className="flex items-center">
                          <Settings className="w-4 h-4 mr-3" />
                          Dashboard
                        </div>
                      </Link>
                      <Link href="/dashboard/orders" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                        <div className="flex items-center">
                          {/* Package Icon Placeholder */}
                          <div className="w-4 h-4 mr-3 bg-gray-200 rounded"></div>
                          Orders
                        </div>
                      </Link>
                      <Link href="/dashboard/wallet" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                        <div className="flex items-center">
                          <div className="w-4 h-4 mr-3 bg-gray-200 rounded"></div>
                          Wallet
                        </div>
                      </Link>
                      <Link href="/dashboard/wishlist" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                        <div className="flex items-center">
                          <div className="w-4 h-4 mr-3 bg-gray-200 rounded"></div>
                          Wishlist
                        </div>
                      </Link>
                      <hr className="my-1 border-gray-200" />
                      <div className="px-4 py-2">
                        <Settings className="w-4 h-4 mr-3" />
                        Account Settings
                      </div>
                      <div className="px-4 py-2">
                        <button 
                          onClick={() => {
                            if (typeof window !== 'undefined') {
                              window.location.href = '/auth/logout';
                            }
                          }}
                          className="flex items-center w-full text-red-600 hover:bg-red-50"
                        >
                          <LogOut className="w-4 h-4 mr-3" />
                          Logout
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
  );
}