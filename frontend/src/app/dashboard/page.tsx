"use client";
'use client';

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';
import { 
  ShoppingCart, 
  Package, 
  Bell, 
  Search, 
  User, 
  ChevronDown, 
  Menu,
  X,
  CreditCard,
  MapPin,
  Settings,
  LogOut,
  HelpCircle,
  ArrowUp,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Heart
} from 'lucide-react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { useCart } from '@/context/CartContext';
import { useWishlist } from '@/context/WishlistContext';
import { OverviewCards } from '@/app/dashboard/components/overview/OverviewCards';
import { RecentOrders } from '@/app/dashboard/components/orders/RecentOrders';
import { QuickActions } from '@/app/dashboard/components/header/QuickActions';
import { LoadingSkeletons } from '@/app/dashboard/components/shared/LoadingSkeletons';
import { WalletSection, WishlistSummary } from '@/app/dashboard/components/orders/WalletSection';

export default function DashboardPage() {
  const { user, isAuthenticated } = useAuth();
  const { items: cartItems } = useCart();
  const { items: wishlistItems } = useWishlist();
  const searchParams = useSearchParams();
  
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');

  // Simulate data loading
  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 1500);
    
    return () => clearTimeout(timer);
  }, []);

  // Mock data (replace with actual API calls)
  const dashboardData = {
    overview: {
      totalOrders: 1234,
      pendingOrders: 12,
      walletBalance: 125.50,
      storeCredit: 45.00,
      wishlistCount: wishlistItems?.length || 0
    },
    recentOrders: [
      {
        id: 'ORD-12345',
        orderNumber: '#12345',
        status: 'SHIPPED',
        totalAmount: 999.99,
        createdAt: '2024-01-20T10:30:00Z',
        shippedAt: '2024-01-18T14:20:00Z',
        items: [
          { name: 'Wireless Headphones', quantity: 1, price: 299.99 },
          { name: 'Phone Case', quantity: 2, price: 24.99 }
        ],
        trackingNumber: '1Z999999999999',
        progress: 75 // 75% delivered
      },
      {
        id: 'ORD-12344',
        orderNumber: '#12344',
        status: 'PROCESSING',
        totalAmount: 549.00,
        createdAt: '2024-01-22T09:15:00Z',
        items: [
          { name: 'Smart Watch', quantity: 1, price: 399.00 }
        ],
        progress: 25 // 25% complete
      },
      {
        id: 'ORD-12343',
        orderNumber: '#12343',
        status: 'DELIVERED',
        totalAmount: 129.99,
        createdAt: '2024-01-15T16:45:00Z',
        deliveredAt: '2024-01-17T11:20:00Z',
        items: [
          { name: 'Bluetooth Speaker', quantity: 1, price: 79.99 },
          { name: 'USB Cable', quantity: 2, price: 12.99 }
        ],
        progress: 100 // 100% complete
      }
    ],
    wallet: {
      balance: 125.50,
      availableCredit: 45.00,
      transactions: [
        { type: 'purchase', amount: -299.99, date: '2024-01-18', description: 'Online Store Purchase' },
        { type: 'refund', amount: 50.00, date: '2024-01-15', description: 'Order Cancellation' },
        { type: 'credit', amount: 25.00, date: '2024-01-10', description: 'Loyalty Bonus' }
      ]
    },
    wishlist: {
      itemCount: wishlistItems?.length || 0,
      totalValue: 2843.50,
      items: wishlistItems || []
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo */}
            <div className="flex items-center">
              <div className="w-10 h-10 bg-blue-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-xl">S</span>
              </div>
              <span className="ml-3 text-xl font-semibold text-gray-900">hopHub</span>
            </div>

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
                  {dashboardData.wishlist.itemCount > 0 && (
                    <span className="absolute -top-1 -right-1 bg-blue-500 text-white text-xs rounded-full px-2">
                      {dashboardData.wishlist.itemCount}
                    </span>
                  )}
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
                        alt={(user as any)?.name ?? (user as any)?.firstName ?? 'User'}
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
                          <Package className="w-4 h-4 mr-3" />
                          Dashboard
                        </div>
                      </Link>
                      <Link href="/dashboard/orders" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                        <div className="flex items-center">
                          <Package className="w-4 h-4 mr-3" />
                          Orders
                        </div>
                      </Link>
                      <Link href="/dashboard/wallet" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                        <div className="flex items-center">
                          <CreditCard className="w-4 h-4 mr-3" />
                          Wallet
                        </div>
                      </Link>
                      <Link href="/dashboard/wishlist" className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
                        <div className="flex items-center">
                          <Heart className="w-4 h-4 mr-3" />
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
      </header>

      {/* Quick Actions Bar */}
      <QuickActions />

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Overview Section */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-xl font-bold text-gray-900 mb-6">Dashboard Overview</h2>
              
              {/* Tab Navigation */}
              <div className="flex space-x-1 mb-6 border-b border-gray-200">
                {['overview', 'orders', 'wallet', 'wishlist'].map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`px-4 py-2 font-medium text-sm border-b-2 transition-colors ${
                      activeTab === tab
                        ? 'text-blue-600 border-blue-600'
                        : 'text-gray-500 border-transparent hover:text-gray-700'
                    }`}
                  >
                    {tab === 'overview' && 'Overview'}
                    {tab === 'orders' && 'Orders'}
                    {tab === 'wallet' && 'Wallet'}
                    {tab === 'wishlist' && 'Wishlist'}
                  </button>
                ))}
              </div>

              {/* Tab Content */}
              {isLoading ? (
                <LoadingSkeletons type="overview" />
              ) : (
                <>
                  {activeTab === 'overview' && <OverviewCards data={dashboardData.overview} />}
                  {activeTab === 'orders' && <RecentOrders orders={dashboardData.recentOrders as any} />}
                  {activeTab === 'wallet' && <WalletSection data={dashboardData.wallet} />}
                  {activeTab === 'wishlist' && <WishlistSummary data={dashboardData.wishlist} />}
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}