'use client';

import React from 'react';
import { CreditCard, TrendingUp, TrendingDown, Wallet, Heart } from 'lucide-react';
import Link from 'next/link';

interface WalletData {
  balance: number;
  availableCredit: number;
  transactions: Array<{
    type: string;
    amount: number;
    date: string;
    description: string;
  }>;
}

interface WishlistData {
  itemCount: number;
  totalValue: number;
  items: Array<any>;
}

export function WalletSection({ data }: { data: WalletData }) {
  return (
    <div className="bg-white rounded-lg shadow-sm p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-gray-900">Wallet Balance</h3>
        <Link href="/dashboard/wallet/transactions" className="text-blue-600 hover:text-blue-700 text-sm font-medium">
          View All Transactions
        </Link>
      </div>
      
      <div className="space-y-4">
        {/* Current Balance */}
        <div className="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
              <Wallet className="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <div className="text-3xl font-bold text-gray-900">${data.balance.toFixed(2)}</div>
              <div className="text-sm text-gray-600">Available balance</div>
            </div>
          </div>
          <div className="text-right">
            <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
              Add Funds
            </button>
          </div>
        </div>

        {/* Store Credit */}
        <div className="flex items-center justify-between p-4 bg-green-50 rounded-lg">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center">
              <CreditCard className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <div className="text-3xl font-bold text-gray-900">${data.availableCredit.toFixed(2)}</div>
              <div className="text-sm text-gray-600">Available store credit</div>
            </div>
          </div>
          <div className="text-right">
            <button className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
              Manage Credit
            </button>
          </div>
        </div>

        {/* Recent Transactions */}
        <div className="border-t border-gray-200 pt-6">
          <h4 className="text-md font-semibold text-gray-900 mb-4">Recent Transactions</h4>
          <div className="space-y-3">
            {data.transactions.slice(0, 5).map((transaction, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center">
                  <div className="w-2 h-2 rounded-full mr-3">
                    {transaction.type === 'purchase' && (
                      <div className="w-full h-full bg-red-500 rounded-full"></div>
                    )}
                    {transaction.type === 'refund' && (
                      <div className="w-full h-full bg-green-500 rounded-full"></div>
                    )}
                    {transaction.type === 'credit' && (
                      <div className="w-full h-full bg-blue-500 rounded-full"></div>
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="font-medium text-gray-900">{transaction.description}</div>
                    <div className="text-sm text-gray-500">{transaction.date}</div>
                    <div className={`text-lg font-semibold ${
                      transaction.type === 'purchase' ? 'text-red-600' :
                      transaction.type === 'refund' ? 'text-green-600' :
                      'text-blue-600'
                    }`}>
                      {transaction.type === 'purchase' && '-'}
                      {transaction.type === 'refund' && '+'}
                      {transaction.type === 'credit' && '+'}
                      ${Math.abs(transaction.amount).toFixed(2)}
                    </div>
                  </div>
                </div>
                <div className="text-lg font-semibold text-gray-900">
                  ${Math.abs(transaction.amount).toFixed(2)}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export function WishlistSummary({ data }: { data: WishlistData }) {
  return (
    <div className="bg-white rounded-lg shadow-sm p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-gray-900">Wishlist</h3>
        <Link href="/wishlist" className="text-blue-600 hover:text-blue-700 text-sm font-medium">
          View Full Wishlist
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Wishlist Stats */}
        <div>
          <div className="bg-red-50 rounded-lg p-6 text-center">
            <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Heart className="w-8 h-8 text-red-600" />
            </div>
            <div className="text-3xl font-bold text-gray-900">{data.itemCount}</div>
            <div className="text-sm text-gray-600">Total items</div>
          </div>
          <div className="text-right">
            <Link href="/wishlist" className="text-red-600 hover:text-red-700 font-medium text-sm">
              View All
            </Link>
          </div>
        </div>

        {/* Total Value */}
        <div>
          <div className="bg-green-50 rounded-lg p-6 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <div className="text-3xl font-bold text-green-600">$</div>
            </div>
            <div className="text-3xl font-bold text-gray-900">{data.totalValue.toFixed(2)}</div>
            <div className="text-sm text-gray-600">Total value</div>
          </div>
        </div>
      </div>

      {/* Top Items Preview */}
      <div className="md:col-span-2">
        <h4 className="text-md font-semibold text-gray-900 mb-4">Top Items</h4>
        <div className="space-y-3">
          {data.items.slice(0, 4).map((item, index) => {
            const id = item?.id ?? item?.productId ?? String(index);
            const name = item?.name ?? item?.product?.name ?? 'Item';
            const price = item?.price ?? item?.product?.price ?? 0;
            return (
              <div key={id} className="flex items-center p-4 bg-white rounded-lg shadow-sm">
                <img
                  src={`https://via.placeholder.com/150x150?text=${encodeURIComponent(name)}`}
                  alt={name}
                  className="w-16 h-16 object-cover rounded-lg mr-4"
                />
                <div className="flex-1">
                  <h5 className="font-semibold text-gray-900 mb-1">{name}</h5>
                  <p className="text-sm text-gray-600 mb-2">High-quality product with excellent features</p>
                  <div className="flex items-center justify-between">
                    <div className="text-lg font-bold text-blue-600">${price.toFixed(2)}</div>
                    <Link href={`/products/${id}`} className="text-blue-600 hover:text-blue-700 text-sm font-medium">
                      View Product
                    </Link>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
        <div className="text-center mt-6">
          <Link href="/wishlist" className="text-blue-600 hover:text-blue-700 font-medium">
            View All {data.itemCount} Items
          </Link>
        </div>
      </div>
    </div>
  );
}