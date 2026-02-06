'use client';

import React from 'react';
import { Package, ArrowUp, ExternalLink } from 'lucide-react';
import Link from 'next/link';

interface Order {
  id: string;
  orderNumber: string;
  status: 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  createdAt: string;
  items: Array<{
    name: string;
    quantity: number;
    price: number;
  }>;
  trackingNumber?: string;
  progress?: number;
  shippedAt?: string;
  deliveredAt?: string;
}

interface RecentOrdersProps {
  orders: Order[];
}

const statusConfig = {
  PENDING: { color: 'orange', icon: Package, label: 'Pending' },
  PROCESSING: { color: 'blue', icon: Package, label: 'Processing' },
  SHIPPED: { color: 'blue', icon: Package, label: 'Shipped' },
  DELIVERED: { color: 'green', icon: Package, label: 'Delivered' },
  CANCELLED: { color: 'red', icon: Package, label: 'Cancelled' },
};

const getStatusColor = (status: string) => {
  return statusConfig[status as keyof typeof statusConfig]?.color || 'gray';
};

const getStatusIcon = (status: string) => {
  return statusConfig[status as keyof typeof statusConfig]?.icon || Package;
};

const getStatusLabel = (status: string) => {
  return statusConfig[status as keyof typeof statusConfig]?.label || status;
};

export function RecentOrders({ orders }: RecentOrdersProps) {
  return (
    <div className="space-y-6">
      <h3 className="text-xl font-bold text-gray-900 mb-6">Recent Orders</h3>
      
      {orders.length === 0 ? (
        <div className="text-center py-12">
          <Package className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600">No orders yet</p>
          <Link href="/products" className="text-blue-600 hover:text-blue-700 font-medium">
            Start Shopping
          </Link>
        </div>
      ) : (
        orders.map((order) => (
          <div key={order.id} className="bg-white rounded-lg shadow-sm p-6 border border-gray-200">
            {/* Order Header */}
            <div className="flex items-start justify-between mb-4">
              <div>
                <h4 className="text-lg font-semibold text-gray-900">Order #{order.orderNumber}</h4>
                <p className="text-sm text-gray-500">Placed on {new Date(order.createdAt).toLocaleDateString()}</p>
              </div>
              
              <div className="text-right">
                <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(order.status)}`}>
                  {(() => {
                    const Icon = getStatusIcon(order.status);
                    return <Icon className="w-3 h-3 mr-1" />;
                  })()}
                  {getStatusLabel(order.status)}
                </span>
              </div>
            </div>

            {/* Order Progress */}
            {order.progress && order.progress < 100 && (
              <div className="mb-4">
                <div className="flex items-center justify-between mb-2">
                  <div className="text-sm text-gray-600">Order Progress</div>
                  <div className="text-sm font-medium text-gray-900">{order.progress}% Complete</div>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div 
                    className="h-2 bg-blue-600 rounded-full transition-all duration-300"
                    style={{ width: `${order.progress}%` }}
                  ></div>
                </div>
              </div>
            )}

            {/* Tracking Info */}
            {order.trackingNumber && (
              <div className="mb-4 p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center justify-between mb-2">
                  <h5 className="text-sm font-semibold text-gray-900">Tracking Number</h5>
                  <div className="flex items-center">
                    <span className="text-blue-600 font-mono">{order.trackingNumber}</span>
                    <button
                      onClick={() => navigator.clipboard.writeText(order.trackingNumber)}
                      className="ml-2 text-gray-500 hover:text-gray-700"
                      title="Copy tracking number"
                    >
                      <ExternalLink className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {order.shippedAt && (
                  <div className="text-sm text-gray-600">
                    Shipped on {new Date(order.shippedAt).toLocaleDateString()}
                  </div>
                )}

                {order.deliveredAt && (
                  <div className="text-sm text-green-600">
                    Delivered on {new Date(order.deliveredAt).toLocaleDateString()}
                  </div>
                )}
              </div>
            )}

            {/* Order Items */}
            <div className="mb-4">
              <h5 className="text-sm font-semibold text-gray-900 mb-3">Items ({order.items.length})</h5>
              <div className="space-y-2">
                {order.items.map((item, index) => (
                  <div key={index} className="flex items-center justify-between">
                    <div className="flex-1">
                      <div>
                        <p className="font-medium text-gray-900">{item.name}</p>
                        <p className="text-sm text-gray-500">Qty: {item.quantity}</p>
                      </div>
                    </div>
                    <div className="text-right">
                      <p className="font-semibold text-gray-900">${(item.price * item.quantity).toFixed(2)}</p>
                      <p className="text-sm text-gray-500">${item.price.toFixed(2)} each</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Order Total */}
            <div className="flex items-center justify-between pt-4 border-t border-gray-200">
              <div>
                <h5 className="text-sm font-semibold text-gray-900">Total Amount</h5>
                <p className="text-2xl font-bold text-gray-900">${order.totalAmount.toFixed(2)}</p>
              </div>
              <div className="text-right">
                <Link href={`/dashboard/orders/${order.id}`} className="text-blue-600 hover:text-blue-700 font-medium text-sm">
                  <div className="flex items-center">
                    View Details
                    <ArrowUp className="w-4 h-4 ml-1" />
                  </div>
                </Link>
              </div>
            </div>
          </div>
        ))
      )}
    </div>
  );
}