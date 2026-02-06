'use client';

import React from 'react';
import { DollarSign, Package, TrendingUp, TrendingDown, Heart, ShoppingCart } from 'lucide-react';
import Link from 'next/link';

interface OverviewCardsProps {
  data: {
    totalOrders: number;
    pendingOrders: number;
    walletBalance: number;
    storeCredit: number;
    wishlistCount: number;
  };
}

export function OverviewCards({ data }: OverviewCardsProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      {/* Total Orders Card */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">Total Orders</h3>
          <Link href="/dashboard/orders" className="text-blue-600 hover:text-blue-700 font-medium text-sm">
            View All Orders
          </Link>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
              <Package className="w-6 h-6 text-blue-600" />
            </div>
            <div className="text-3xl font-bold text-gray-900">{data.totalOrders.toLocaleString()}</div>
          </div>
          <div className="text-sm text-gray-500">All time orders</div>
        </div>
      </div>

      {/* Pending Orders Card */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">Pending Orders</h3>
          <div className="flex items-center gap-2 text-sm text-orange-600">
            <div className="w-2 h-2 bg-orange-600 rounded-full"></div>
            {data.pendingOrders} pending
          </div>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-orange-100 rounded-full flex items-center justify-center">
              <TrendingUp className="w-6 h-6 text-orange-600" />
            </div>
            <div className="text-3xl font-bold text-orange-600">{data.pendingOrders}</div>
          </div>
          <div className="text-sm text-gray-500">Requires attention</div>
        </div>
      </div>

      {/* Wallet Balance Card */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">Wallet Balance</h3>
          <Link href="/dashboard/wallet" className="text-green-600 hover:text-green-700 font-medium text-sm">
            Manage Wallet
          </Link>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <DollarSign className="w-6 h-6 text-green-600" />
            </div>
            <div className="text-3xl font-bold text-gray-900">${data.walletBalance.toFixed(2)}</div>
          </div>
          <div className="text-sm text-gray-500">Available balance</div>
        </div>
      </div>

      {/* Store Credit Card */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">Store Credit</h3>
          <div className="flex items-center gap-2 text-sm text-green-600">
            <div className="w-2 h-2 bg-green-600 rounded-full"></div>
            Available
          </div>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <DollarSign className="w-6 h-6 text-green-600" />
            </div>
            <div className="text-3xl font-bold text-gray-900">${data.storeCredit.toFixed(2)}</div>
          </div>
          <div className="text-sm text-gray-500">Available store credit</div>
        </div>
      </div>

      {/* Wishlist Summary Card */}
      <div className="bg-white rounded-lg shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">Wishlist</h3>
          <Link href="/dashboard/wishlist" className="text-red-600 hover:text-red-700 font-medium text-sm">
            View Wishlist
          </Link>
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
              <Heart className="w-6 h-6 text-red-600" />
            </div>
            <div className="text-3xl font-bold text-gray-900">{data.wishlistCount}</div>
          </div>
          <div className="text-sm text-gray-500">Saved items</div>
        </div>
      </div>
    </div>
  );
}