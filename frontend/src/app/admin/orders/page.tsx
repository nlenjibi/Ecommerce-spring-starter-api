'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '@/lib/api';
import { Eye, Search, X, Package, Calendar, DollarSign, ShoppingBag, Truck, CheckCircle, XCircle, Clock, RefreshCw, CreditCard, AlertCircle } from 'lucide-react';
import { Pagination } from '@/components/admin/Pagination';
import toast from 'react-hot-toast';

// Status configuration with colors and icons
const ORDER_STATUSES = {
  PENDING: { label: 'Pending', color: 'bg-yellow-100 text-yellow-700', icon: Clock },
  CONFIRMED: { label: 'Confirmed', color: 'bg-blue-100 text-blue-700', icon: CheckCircle },
  PROCESSING: { label: 'Processing', color: 'bg-purple-100 text-purple-700', icon: RefreshCw },
  SHIPPED: { label: 'Shipped', color: 'bg-indigo-100 text-indigo-700', icon: Truck },
  DELIVERED: { label: 'Delivered', color: 'bg-green-100 text-green-700', icon: CheckCircle },
  CANCELLED: { label: 'Cancelled', color: 'bg-red-100 text-red-700', icon: XCircle },
};

const PAYMENT_STATUSES = {
  PENDING: { label: 'Pending', color: 'bg-yellow-100 text-yellow-700' },
  PAID: { label: 'Paid', color: 'bg-green-100 text-green-700' },
  FAILED: { label: 'Failed', color: 'bg-red-100 text-red-700' },
  REFUNDED: { label: 'Refunded', color: 'bg-gray-100 text-gray-700' },
};

type OrderStatus = keyof typeof ORDER_STATUSES;
type PaymentStatus = keyof typeof PAYMENT_STATUSES;

export default function AdminOrdersPage() {
  const [page, setPage] = useState(0); // 0-based for API
  const [statusFilter, setStatusFilter] = useState('');
  const queryClient = useQueryClient();
  const size = 10;

  // Fetch orders - if status filter is set, use getOrdersByStatus, otherwise getOrders
  const { data, isLoading, error } = useQuery({
    queryKey: ['admin', 'orders', { page, size, status: statusFilter }],
    queryFn: () => statusFilter 
      ? adminApi.getOrdersByStatus(statusFilter, { page, size })
      : adminApi.getOrders({ page, size }),
  });

  // Fetch order statistics for the dashboard cards (optional - may not be implemented)
  const { data: statsData, isError: statsError } = useQuery({
    queryKey: ['admin', 'orders', 'statistics'],
    queryFn: () => adminApi.getOrderStatistics(),
    retry: false, // Don't retry if endpoint doesn't exist
  });

  // Handle API response structure
  const apiData = data as any;
  const orders = apiData?.data?.content || [];
  const totalPages = apiData?.data?.totalPages || 1;
  const totalElements = apiData?.data?.totalElements || 0;
  const currentPage = apiData?.data?.page ?? 0;

  // Use stats if available, otherwise calculate from current data or show 0
  const stats = statsError ? {} : ((statsData as any)?.data || {});

  const updateStatusMutation = useMutation({
    mutationFn: ({ orderId, status }: { orderId: number; status: string }) =>
      adminApi.updateOrderStatus(orderId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'orders'] });
      toast.success('Order status updated');
    },
    onError: () => toast.error('Failed to update order status'),
  });

  const confirmOrderMutation = useMutation({
    mutationFn: (orderId: number) => adminApi.confirmOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'orders'] });
      toast.success('Order confirmed');
    },
    onError: () => toast.error('Failed to confirm order'),
  });

  const deliverOrderMutation = useMutation({
    mutationFn: (orderId: number) => adminApi.deliverOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'orders'] });
      toast.success('Order marked as delivered');
    },
    onError: () => toast.error('Failed to update order'),
  });

  const getStatusConfig = (status: string) => {
    const upperStatus = status?.toUpperCase() as OrderStatus;
    return ORDER_STATUSES[upperStatus] || { label: status, color: 'bg-gray-100 text-gray-700', icon: Package };
  };

  const getPaymentStatusConfig = (status: string) => {
    const upperStatus = status?.toUpperCase() as PaymentStatus;
    return PAYMENT_STATUSES[upperStatus] || { label: status, color: 'bg-gray-100 text-gray-700' };
  };

  const formatCurrency = (amount: number | { parsedValue?: number; source?: string } | null | undefined) => {
    if (amount === null || amount === undefined) return '$0.00';
    if (typeof amount === 'object') {
      return `$${(amount.parsedValue ?? parseFloat(amount.source || '0')).toFixed(2)}`;
    }
    return `$${amount.toFixed(2)}`;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Orders</h1>
        <p className="text-gray-600">Manage and track customer orders</p>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
        <div 
          onClick={() => setStatusFilter('')}
          className={`bg-white rounded-xl shadow-sm p-4 border cursor-pointer transition-all ${
            !statusFilter ? 'border-blue-500 ring-2 ring-blue-100' : 'border-gray-100 hover:border-gray-200'
          }`}
        >
          <p className="text-sm text-gray-500">Total Orders</p>
          <p className="text-2xl font-bold text-gray-900">{stats.totalOrders || 0}</p>
        </div>
        <div 
          onClick={() => setStatusFilter('PENDING')}
          className={`bg-white rounded-xl shadow-sm p-4 border cursor-pointer transition-all ${
            statusFilter === 'PENDING' ? 'border-yellow-500 ring-2 ring-yellow-100' : 'border-gray-100 hover:border-gray-200'
          }`}
        >
          <p className="text-sm text-yellow-600">Pending</p>
          <p className="text-2xl font-bold text-yellow-700">{stats.pendingOrders || 0}</p>
        </div>
        <div 
          onClick={() => setStatusFilter('PROCESSING')}
          className={`bg-white rounded-xl shadow-sm p-4 border cursor-pointer transition-all ${
            statusFilter === 'PROCESSING' ? 'border-purple-500 ring-2 ring-purple-100' : 'border-gray-100 hover:border-gray-200'
          }`}
        >
          <p className="text-sm text-purple-600">Processing</p>
          <p className="text-2xl font-bold text-purple-700">{stats.processingOrders || 0}</p>
        </div>
        <div 
          onClick={() => setStatusFilter('SHIPPED')}
          className={`bg-white rounded-xl shadow-sm p-4 border cursor-pointer transition-all ${
            statusFilter === 'SHIPPED' ? 'border-indigo-500 ring-2 ring-indigo-100' : 'border-gray-100 hover:border-gray-200'
          }`}
        >
          <p className="text-sm text-indigo-600">Shipped</p>
          <p className="text-2xl font-bold text-indigo-700">{stats.shippedOrders || 0}</p>
        </div>
        <div 
          onClick={() => setStatusFilter('DELIVERED')}
          className={`bg-white rounded-xl shadow-sm p-4 border cursor-pointer transition-all ${
            statusFilter === 'DELIVERED' ? 'border-green-500 ring-2 ring-green-100' : 'border-gray-100 hover:border-gray-200'
          }`}
        >
          <p className="text-sm text-green-600">Delivered</p>
          <p className="text-2xl font-bold text-green-700">{stats.deliveredOrders || 0}</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
          <p className="text-sm text-gray-500">Revenue</p>
          <p className="text-2xl font-bold text-gray-900">{formatCurrency(stats.totalRevenue)}</p>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
        <div className="flex flex-col sm:flex-row gap-4 items-center">
          {/* Status Filter Dropdown */}
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white min-w-[160px]"
          >
            <option value="">All Statuses</option>
            {Object.entries(ORDER_STATUSES).map(([key, { label }]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
          {statusFilter && (
            <button
              onClick={() => setStatusFilter('')}
              className="text-sm text-blue-600 hover:text-blue-800 flex items-center gap-1"
            >
              <X className="w-4 h-4" />
              Clear filter
            </button>
          )}
          <p className="text-sm text-gray-500 ml-auto">
            Showing {totalElements} order{totalElements !== 1 ? 's' : ''}
            {statusFilter && ` with status "${ORDER_STATUSES[statusFilter as OrderStatus]?.label || statusFilter}"`}
          </p>
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Order</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Customer</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Items</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Total</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Order Status</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Payment</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Date</th>
                <th className="text-right px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="px-6 py-4"><div className="h-5 w-24 bg-gray-200 rounded"></div></td>
                    <td className="px-6 py-4"><div className="space-y-2"><div className="h-4 w-32 bg-gray-200 rounded"></div><div className="h-3 w-40 bg-gray-100 rounded"></div></div></td>
                    <td className="px-6 py-4"><div className="h-4 w-16 bg-gray-200 rounded"></div></td>
                    <td className="px-6 py-4"><div className="h-5 w-20 bg-gray-200 rounded"></div></td>
                    <td className="px-6 py-4"><div className="h-7 w-24 bg-gray-200 rounded-full"></div></td>
                    <td className="px-6 py-4"><div className="h-6 w-16 bg-gray-200 rounded-full"></div></td>
                    <td className="px-6 py-4"><div className="h-4 w-24 bg-gray-200 rounded"></div></td>
                    <td className="px-6 py-4"><div className="h-8 w-24 bg-gray-200 rounded ml-auto"></div></td>
                  </tr>
                ))
              ) : error ? (
                <tr>
                  <td colSpan={8} className="px-6 py-16 text-center">
                    <XCircle className="w-12 h-12 text-red-300 mx-auto mb-4" />
                    <p className="text-gray-500 font-medium">Failed to load orders</p>
                    <p className="text-gray-400 text-sm mt-1">Please try again later</p>
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-6 py-16 text-center">
                    <ShoppingBag className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-500 font-medium">No orders found</p>
                    <p className="text-gray-400 text-sm mt-1">
                      {statusFilter 
                        ? `No orders with status "${ORDER_STATUSES[statusFilter as OrderStatus]?.label || statusFilter}"` 
                        : 'Orders will appear here once customers make purchases'}
                    </p>
                  </td>
                </tr>
              ) : (
                orders.map((order: any) => {
                  const orderStatus = order.status || order.orderStatus || '';
                  const paymentStatus = order.paymentStatus || '';
                  const statusConfig = getStatusConfig(orderStatus);
                  const paymentConfig = getPaymentStatusConfig(paymentStatus);
                  const StatusIcon = statusConfig.icon;
                  
                  // Determine available quick actions based on current status
                  const canConfirm = orderStatus.toUpperCase() === 'PENDING';
                  const canShip = orderStatus.toUpperCase() === 'CONFIRMED' || orderStatus.toUpperCase() === 'PROCESSING';
                  const canDeliver = orderStatus.toUpperCase() === 'SHIPPED';
                  
                  return (
                    <tr key={order.id} className="hover:bg-gray-50/50 transition-colors">
                      {/* Order Number */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <div className="w-8 h-8 rounded-lg bg-blue-100 flex items-center justify-center">
                            <Package className="w-4 h-4 text-blue-600" />
                          </div>
                          <div>
                            <p className="font-semibold text-gray-900">
                              {order.orderNumber || `#${order.id}`}
                            </p>
                            <p className="text-xs text-gray-500">ID: {order.id}</p>
                          </div>
                        </div>
                      </td>
                      {/* Customer */}
                      <td className="px-6 py-4">
                        <div>
                          <p className="font-medium text-gray-900">
                            {order.user?.firstName || order.customerName || 'Guest'} {order.user?.lastName || ''}
                          </p>
                          <p className="text-sm text-gray-500">{order.user?.email || order.customerEmail || `User #${order.userId || 'N/A'}`}</p>
                        </div>
                      </td>
                      {/* Items */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5">
                          <ShoppingBag className="w-4 h-4 text-gray-400" />
                          <span className="text-gray-700">{order.items?.length || order.itemCount || 0} items</span>
                        </div>
                      </td>
                      {/* Total */}
                      <td className="px-6 py-4">
                        <span className="font-semibold text-gray-900">
                          {formatCurrency(order.total || order.totalAmount || 0)}
                        </span>
                      </td>
                      {/* Order Status */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5">
                          <StatusIcon className={`w-4 h-4 ${statusConfig.color.includes('yellow') ? 'text-yellow-600' : statusConfig.color.includes('blue') ? 'text-blue-600' : statusConfig.color.includes('purple') ? 'text-purple-600' : statusConfig.color.includes('indigo') ? 'text-indigo-600' : statusConfig.color.includes('green') ? 'text-green-600' : statusConfig.color.includes('red') ? 'text-red-600' : 'text-gray-600'}`} />
                          <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ${statusConfig.color}`}>
                            {statusConfig.label}
                          </span>
                        </div>
                      </td>
                      {/* Payment Status */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5">
                          <CreditCard className="w-4 h-4 text-gray-400" />
                          <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-semibold ${paymentConfig.color}`}>
                            {paymentConfig.label}
                          </span>
                        </div>
                      </td>
                      {/* Date */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5 text-gray-600">
                          <Calendar className="w-4 h-4 text-gray-400" />
                          <span className="text-sm">
                            {new Date(order.createdAt || order.orderDate).toLocaleDateString('en-US', { 
                              month: 'short', 
                              day: 'numeric', 
                              year: 'numeric' 
                            })}
                          </span>
                        </div>
                      </td>
                      {/* Actions */}
                      <td className="px-6 py-4">
                        <div className="flex items-center justify-end gap-1">
                          {/* Quick Action Buttons */}
                          {canConfirm && (
                            <button
                              onClick={() => confirmOrderMutation.mutate(order.id)}
                              disabled={confirmOrderMutation.isPending}
                              className="px-2 py-1 text-xs font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-md transition-colors disabled:opacity-50"
                              title="Confirm order"
                            >
                              Confirm
                            </button>
                          )}
                          {canShip && (
                            <Link
                              href={`/admin/orders/${order.id}?action=ship`}
                              className="px-2 py-1 text-xs font-medium text-indigo-600 bg-indigo-50 hover:bg-indigo-100 rounded-md transition-colors"
                              title="Ship order"
                            >
                              Ship
                            </Link>
                          )}
                          {canDeliver && (
                            <button
                              onClick={() => deliverOrderMutation.mutate(order.id)}
                              disabled={deliverOrderMutation.isPending}
                              className="px-2 py-1 text-xs font-medium text-green-600 bg-green-50 hover:bg-green-100 rounded-md transition-colors disabled:opacity-50"
                              title="Mark as delivered"
                            >
                              Deliver
                            </button>
                          )}
                          <Link
                            href={`/admin/orders/${order.id}`}
                            className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="View order details"
                          >
                            <Eye className="w-4 h-4" />
                          </Link>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        <Pagination 
          page={currentPage} 
          totalPages={totalPages} 
          onPageChange={setPage}
          totalElements={totalElements}
          size={size}
        />
      </div>
    </div>
  );
}
