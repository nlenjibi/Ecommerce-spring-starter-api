"use client";
import React, { useState, useEffect } from 'react';
import { Package, Eye, X, ChevronLeft, ChevronRight, Filter } from 'lucide-react';
import { ordersApi } from '@/services/api';

interface Order {
  id: number;
  orderNumber: string;
  userId: number;
  status: 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  paymentStatus: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
  paymentMethod: 'CREDIT_CARD' | 'PAYPAL' | 'BANK_TRANSFER';
  shippingMethod: 'STANDARD' | 'EXPRESS' | 'OVERNIGHT';
  subtotal: number;
  taxAmount: number;
  shippingCost: number;
  discountAmount: number;
  totalAmount: number;
  shippingAddress: string;
  trackingNumber?: string;
  carrier?: string;
  orderDate: string;
  shippedAt?: string;
  deliveredAt?: string;
  estimatedDeliveryDate?: string;
  items: OrderItem[];
  itemCount: number;
  customerNotes?: string;
}

interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  productImageUrl: string;
  totalPrice: number;
}

interface OrdersManagementProps {
  onOrderSelect?: (order: Order) => void;
  filterStatus?: string;
}

export default function OrdersManagement({ onOrderSelect, filterStatus }: OrdersManagementProps) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [selectedStatus, setSelectedStatus] = useState<string>(filterStatus || 'ALL');
  const [showFilters, setShowFilters] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const pageSize = 10;
  const userId = 1; // Replace with actual user ID from auth context

  const statusOptions = [
    { value: 'ALL', label: 'All Orders', color: 'gray' },
    { value: 'PENDING', label: 'Pending', color: 'yellow' },
    { value: 'PROCESSING', label: 'Processing', color: 'blue' },
    { value: 'SHIPPED', label: 'Shipped', color: 'purple' },
    { value: 'DELIVERED', label: 'Delivered', color: 'green' },
    { value: 'CANCELLED', label: 'Cancelled', color: 'red' },
  ];

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-200',
      PROCESSING: 'bg-blue-100 text-blue-800 border-blue-200',
      SHIPPED: 'bg-purple-100 text-purple-800 border-purple-200',
      DELIVERED: 'bg-green-100 text-green-800 border-green-200',
      CANCELLED: 'bg-red-100 text-red-800 border-red-200',
    };
    return colors[status] || 'bg-gray-100 text-gray-800 border-gray-200';
  };

  const getPaymentStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      PAID: 'text-green-600',
      PENDING: 'text-yellow-600',
      FAILED: 'text-red-600',
      REFUNDED: 'text-blue-600',
    };
    return colors[status] || 'text-gray-600';
  };

  const fetchOrders = async () => {
    setLoading(true);
    setError(null);
    
    try {
      let response;
      
      if (selectedStatus === 'ALL') {
        response = await ordersApi.getUserOrders({
          page: currentPage,
          limit: pageSize,
        });
      } else {
        response = await ordersApi.getMyOrdersByStatus(selectedStatus, {
          page: currentPage,
          limit: pageSize,
        });
      }

      // Handle different response formats from your API
      if (response.data?.content) {
        setOrders(response.data.content);
        setTotalPages(response.data.totalPages || 0);
        setTotalElements(response.data.totalElements || 0);
      } else if (response.orders) {
        setOrders(response.orders);
        setTotalPages(Math.ceil((response.total || 0) / pageSize));
        setTotalElements(response.total || 0);
      } else {
        setOrders([]);
        setTotalPages(0);
        setTotalElements(0);
      }
    } catch (err) {
      console.error('Error fetching orders:', err);
      setError('Failed to load orders. Please try again.');
      setOrders([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [currentPage, selectedStatus, filterStatus]);

  useEffect(() => {
    if (filterStatus && filterStatus !== selectedStatus) {
      setSelectedStatus(filterStatus);
      setCurrentPage(0);
    }
  }, [filterStatus]);

  const handleStatusChange = (status: string) => {
    setSelectedStatus(status);
    setCurrentPage(0);
    setShowFilters(false);
  };

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && newPage < totalPages) {
      setCurrentPage(newPage);
    }
  };

  const handleCancelOrder = async (orderId: number, orderNumber: string) => {
    if (!confirm(`Are you sure you want to cancel order ${orderNumber}?`)) return;

    try {
      await ordersApi.cancel(orderId);
      // Refresh orders list
      fetchOrders();
      alert('Order cancelled successfully');
    } catch (err) {
      console.error('Error cancelling order:', err);
      alert('Failed to cancel order. Please try again.');
    }
  };

  const filteredOrders = orders.filter(order => 
    order.orderNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
    order.items.some(item => 
      item.productName.toLowerCase().includes(searchTerm.toLowerCase())
    )
  );

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="animate-pulse space-y-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="border border-gray-200 rounded-lg p-4">
              <div className="h-4 bg-gray-300 rounded w-1/4 mb-2"></div>
              <div className="h-3 bg-gray-300 rounded w-1/2 mb-2"></div>
              <div className="h-3 bg-gray-300 rounded w-3/4"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="text-center">
          <Package className="mx-auto text-gray-400 mb-4" size={48} />
          <h3 className="text-lg font-medium text-gray-900 mb-2">Error Loading Orders</h3>
          <p className="text-gray-600 mb-4">{error}</p>
          <button
            onClick={fetchOrders}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            Try Again
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200">
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">My Orders</h2>
            <p className="text-sm text-gray-600 mt-1">
              {totalElements} order{totalElements !== 1 ? 's' : ''} found
            </p>
          </div>
          
          <div className="flex items-center gap-3">
            {/* Search */}
            <div className="relative">
              <input
                type="text"
                placeholder="Search orders..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
              <Package className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={16} />
            </div>
            
            {/* Status Filter */}
            <div className="relative">
              <button
                onClick={() => setShowFilters(!showFilters)}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                <Filter size={16} />
                <span>
                  {statusOptions.find(opt => opt.value === selectedStatus)?.label}
                </span>
              </button>
              
              {showFilters && (
                <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-10">
                  {statusOptions.map((status) => (
                    <button
                      key={status.value}
                      onClick={() => handleStatusChange(status.value)}
                      className={`w-full text-left px-4 py-2 hover:bg-gray-50 first:rounded-t-lg last:rounded-b-lg ${
                        selectedStatus === status.value ? 'bg-blue-50 text-blue-600' : 'text-gray-700'
                      }`}
                    >
                      {status.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Orders List */}
      <div className="divide-y divide-gray-200">
        {filteredOrders.length === 0 ? (
          <div className="p-12 text-center">
            <Package className="mx-auto text-gray-400 mb-4" size={48} />
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              {searchTerm ? 'No matching orders found' : 'No orders yet'}
            </h3>
            <p className="text-gray-600">
              {searchTerm 
                ? 'Try adjusting your search or filters'
                : 'When you place your first order, it will appear here.'
              }
            </p>
          </div>
        ) : (
          filteredOrders.map((order) => (
            <div key={order.id} className="p-6 hover:bg-gray-50 transition-colors">
              <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                {/* Order Info */}
                <div className="flex-1">
                  <div className="flex items-center gap-4 mb-2">
                    <h3 className="font-semibold text-gray-900">{order.orderNumber}</h3>
                    <span className={`px-2 py-1 text-xs font-medium rounded-full border ${getStatusColor(order.status)}`}>
                      {order.status}
                    </span>
                    <span className={`text-sm font-medium ${getPaymentStatusColor(order.paymentStatus)}`}>
                      {order.paymentStatus}
                    </span>
                  </div>
                  
                  <p className="text-sm text-gray-600 mb-2">
                    Placed on {new Date(order.orderDate).toLocaleDateString()} • {order.itemCount} item{order.itemCount !== 1 ? 's' : ''}
                  </p>
                  
                  {order.trackingNumber && (
                    <div className="flex items-center gap-2 text-sm text-gray-600">
                      <span>Tracking: {order.trackingNumber}</span>
                      {order.carrier && <span>• {order.carrier}</span>}
                    </div>
                  )}
                  
                  {/* Order Items Preview */}
                  <div className="mt-3 flex items-center gap-2">
                    {order.items.slice(0, 3).map((item, index) => (
                      <img
                        key={index}
                        src={item.productImageUrl || '/placeholder.png'}
                        alt={item.productName}
                        className="w-12 h-12 rounded object-cover border border-gray-200"
                      />
                    ))}
                    {order.items.length > 3 && (
                      <div className="w-12 h-12 rounded bg-gray-100 border border-gray-200 flex items-center justify-center text-xs text-gray-600">
                        +{order.items.length - 3}
                      </div>
                    )}
                  </div>
                </div>

                {/* Price and Actions */}
                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <p className="text-lg font-semibold text-gray-900">
                      ${order.totalAmount.toFixed(2)}
                    </p>
                    <p className="text-sm text-gray-600">
                      {order.paymentMethod.replace('_', ' ')}
                    </p>
                  </div>
                  
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => onOrderSelect?.(order)}
                      className="flex items-center gap-2 px-4 py-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                    >
                      <Eye size={16} />
                      View
                    </button>
                    
                    {order.status === 'PENDING' && (
                      <button
                        onClick={() => handleCancelOrder(order.id, order.orderNumber)}
                        className="px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="p-6 border-t border-gray-200">
          <div className="flex items-center justify-between">
            <div className="text-sm text-gray-600">
              Showing {currentPage * pageSize + 1} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} orders
            </div>
            
            <div className="flex items-center gap-2">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="p-2 rounded-lg border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ChevronLeft size={16} />
              </button>
              
              {[...Array(totalPages)].map((_, index) => {
                if (
                  index === 0 ||
                  index === totalPages - 1 ||
                  (index >= currentPage - 1 && index <= currentPage + 1)
                ) {
                  return (
                    <button
                      key={index}
                      onClick={() => handlePageChange(index)}
                      className={`px-3 py-2 rounded-lg ${
                        index === currentPage
                          ? 'bg-blue-600 text-white'
                          : 'border border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      {index + 1}
                    </button>
                  );
                } else if (
                  index === currentPage - 2 ||
                  index === currentPage + 2
                ) {
                  return <span key={index} className="px-2">...</span>;
                }
                return null;
              })}
              
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                className="p-2 rounded-lg border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}