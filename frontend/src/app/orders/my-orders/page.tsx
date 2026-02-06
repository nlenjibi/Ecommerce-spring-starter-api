'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { ordersApi } from '@/lib/api';

interface Order {
  id: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  createdAt: string;
  itemCount: number;
  items?: OrderItem[];
}

interface OrderItem {
  id: number;
  product: {
    id: number;
    name: string;
    price: number;
    image?: string;
  };
  quantity: number;
  subtotal: number;
}

export default function MyOrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        setLoading(true);
        const response = await ordersApi.getAll();
        setOrders(response.orders || []);
      } catch (err: any) {
        console.error('Failed to fetch orders:', err);
        setError('Failed to load your orders');
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, []);

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMED':
        return 'bg-blue-100 text-blue-800';
      case 'PROCESSING':
        return 'bg-indigo-100 text-indigo-800';
      case 'SHIPPED':
        return 'bg-purple-100 text-purple-800';
      case 'DELIVERED':
        return 'bg-green-100 text-green-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 py-16">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p className="text-gray-600">Loading your orders...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 py-16">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="w-24 h-24 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg className="w-12 h-12 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Error Loading Orders</h1>
          <p className="text-gray-600 mb-8">{error}</p>
          <Button onClick={() => window.location.reload()}>Try Again</Button>
        </div>
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 py-16">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="w-24 h-24 bg-gray-200 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg className="w-12 h-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-4">No Orders Yet</h1>
          <p className="text-gray-600 mb-8">You haven't placed any orders yet. Start shopping to see your orders here!</p>
          <Link href="/products">
            <Button variant="primary" size="lg">
              Start Shopping
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">My Orders</h1>
          <p className="text-gray-600 mt-2">Track and manage your orders</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Orders List */}
          <div className="lg:col-span-2 space-y-4">
            {orders.map((order) => (
              <div
                key={order.id}
                className={`bg-white rounded-xl shadow-md p-6 cursor-pointer transition-all hover:shadow-lg ${
                  selectedOrder?.id === order.id ? 'ring-2 ring-blue-500' : ''
                }`}
                onClick={() => setSelectedOrder(order)}
              >
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-4">
                  <div>
                    <h3 className="font-semibold text-gray-900">
                      {order.orderNumber || `ORD-${order.id}`}
                    </h3>
                    <p className="text-sm text-gray-600">
                      {new Date(order.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="mt-4 sm:mt-0">
                    <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(order.status)}`}>
                      {order.status}
                    </span>
                  </div>
                </div>

                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm text-gray-600">
                      {order.itemCount} {order.itemCount === 1 ? 'item' : 'items'}
                    </p>
                    <p className="font-semibold text-lg">${order.totalAmount.toFixed(2)}</p>
                  </div>
                  <Link href={`/orders/${order.id}`}>
                    <Button variant="outline" size="sm" className="mt-4 sm:mt-0">
                      View Details
                    </Button>
                  </Link>
                </div>
              </div>
            ))}
          </div>

          {/* Order Details Sidebar */}
          <div className="lg:col-span-1">
            {selectedOrder ? (
              <div className="bg-white rounded-xl shadow-md p-6 sticky top-8">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Order Summary</h3>
                
                <div className="space-y-3 mb-6">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Order Number</span>
                    <span className="font-medium">{selectedOrder.orderNumber || `ORD-${selectedOrder.id}`}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Status</span>
                    <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(selectedOrder.status)}`}>
                      {selectedOrder.status}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Date</span>
                    <span className="font-medium">{new Date(selectedOrder.createdAt).toLocaleDateString()}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Items</span>
                    <span className="font-medium">{selectedOrder.itemCount}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Total</span>
                    <span className="font-bold text-lg">${selectedOrder.totalAmount.toFixed(2)}</span>
                  </div>
                </div>

                <div className="space-y-3">
                  <Link href={`/orders/${selectedOrder.id}`}>
                    <Button variant="primary" size="sm" className="w-full">
                      View Full Details
                    </Button>
                  </Link>
                  <Link href="/products">
                    <Button variant="outline" size="sm" className="w-full">
                      Shop Again
                    </Button>
                  </Link>
                </div>
              </div>
            ) : (
              <div className="bg-white rounded-xl shadow-md p-6 sticky top-8">
                <div className="text-center text-gray-500">
                  <svg className="w-12 h-12 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                  </svg>
                  <p>Select an order to view details</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
