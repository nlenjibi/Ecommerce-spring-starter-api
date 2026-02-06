'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { ordersApi } from '@/services/api';
import { Order } from '@/types';
import { Button } from '@/components/ui/Button';
import OrderTrackingTimeline from '@/components/OrderTrackingTimeline';

/**
 * Dashboard Orders Page
 * 
 * Displays user's order history with search, filter, and tracking functionality.
 * Shows order status, items, totals, and allows order tracking.
 */
export default function OrdersPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();

  const [orders, setOrders] = useState<Order[]>([]);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('all');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);

  // Check authentication
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.replace('/login?returnUrl=/dashboard/orders');
    }
  }, [isAuthenticated, authLoading, router]);

  // Load orders
  useEffect(() => {
    if (!isAuthenticated) return;

    const loadOrders = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await ordersApi.getUserOrders({
          page,
          limit: 10,
          status: statusFilter !== 'all' ? statusFilter : undefined,
        });
        setOrders(response.orders || []);
        setTotalPages(response.totalPages || 1);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to load orders';
        setError(message);
        console.error('Error loading orders:', err);
      } finally {
        setLoading(false);
      }
    };

    loadOrders();
  }, [isAuthenticated, page, statusFilter]);

  if (authLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  if (selectedOrder) {
    return (
      <div className="min-h-screen bg-gray-50 py-8 px-4">
        <div className="max-w-4xl mx-auto">
          <button
            onClick={() => setSelectedOrder(null)}
            className="mb-6 text-blue-600 hover:text-blue-700 font-medium"
          >
            ← Back to Orders
          </button>
          <OrderTrackingTimeline order={selectedOrder} />
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">My Orders</h1>
          <p className="text-gray-600 mt-2">Track and manage your orders</p>
        </div>

        {/* Status Filter */}
        <div className="mb-6 flex gap-2 flex-wrap">
          {[
            { value: 'all', label: 'All Orders' },
            { value: 'pending', label: 'Pending' },
            { value: 'paid', label: 'Paid' },
            { value: 'processing', label: 'Processing' },
            { value: 'shipped', label: 'Shipped' },
            { value: 'delivered', label: 'Delivered' },
            { value: 'cancelled', label: 'Cancelled' },
          ].map((filter) => (
            <button
              key={filter.value}
              onClick={() => {
                setStatusFilter(filter.value);
                setPage(1);
              }}
              className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                statusFilter === filter.value
                  ? 'bg-blue-600 text-white'
                  : 'bg-white text-gray-700 border border-gray-200 hover:bg-gray-50'
              }`}
            >
              {filter.label}
            </button>
          ))}
        </div>

        {/* Error State */}
        {error && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
            {error}
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
          </div>
        ) : orders.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
            <p className="text-gray-600 text-lg mb-4">No orders yet</p>
            <Button
              onClick={() => router.push('/products')}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              Start Shopping
            </Button>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <div
                key={order.id}
                className="bg-white p-4 rounded-lg border border-gray-200 hover:shadow-md transition-shadow"
              >
                <div className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-4">
                  <div>
                    <p className="text-sm text-gray-600">Order ID</p>
                    <p className="font-semibold text-gray-900">#{order.id}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Date</p>
                    <p className="font-semibold text-gray-900">
                      {new Date(order.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Items</p>
                    <p className="font-semibold text-gray-900">{order.items.length} item(s)</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Total</p>
                    <p className="font-semibold text-gray-900">
                      ₦{order.total.toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Status</p>
                    <p className={`font-semibold capitalize ${
                      order.status === 'delivered' || order.status === 'ready_for_pickup'
                        ? 'text-green-600'
                        : order.status === 'cancelled'
                        ? 'text-red-600'
                        : 'text-blue-600'
                    }`}>
                      {order.status.replace('_', ' ')}
                    </p>
                  </div>
                </div>

                <div className="flex gap-2 pt-4 border-t border-gray-200">
                  <Button
                    onClick={() => setSelectedOrder(order)}
                    className="flex-1 bg-blue-600 hover:bg-blue-700 text-white"
                  >
                    View Details & Track
                  </Button>
                </div>
              </div>
            ))}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-8">
                <Button
                  onClick={() => setPage(Math.max(1, page - 1))}
                  disabled={page === 1}
                  className="bg-gray-200 hover:bg-gray-300 text-gray-900 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </Button>
                <div className="px-4 py-2 text-gray-900 font-medium">
                  Page {page} of {totalPages}
                </div>
                <Button
                  onClick={() => setPage(Math.min(totalPages, page + 1))}
                  disabled={page === totalPages}
                  className="bg-gray-200 hover:bg-gray-300 text-gray-900 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </Button>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
