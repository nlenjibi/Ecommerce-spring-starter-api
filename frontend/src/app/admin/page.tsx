'use client';

import { useQuery } from '@tanstack/react-query';
import { adminApi } from '@/lib/api';
import { Package, ShoppingCart, Users, DollarSign, TrendingUp, TrendingDown } from 'lucide-react';
import Link from 'next/link';
import { ORDER_STATUS_COLORS } from '@/lib/constants';

export default function AdminDashboardPage() {
  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['admin', 'stats'],
    queryFn: () => adminApi.getDashboardStats(),
  });

  // Use recentOrders from stats instead of separate call
  const recentOrders = stats?.recentOrders || [];
  const ordersLoading = statsLoading;

  const statCards = [
    { 
      label: 'Total Revenue', 
      value: stats?.totalRevenue ? `$${stats.totalRevenue.toLocaleString()}` : '$0', 
      change: 12.5, // Placeholder - would come from calculated comparison
      icon: DollarSign,
      color: 'bg-green-500' 
    },
    { 
      label: 'Total Orders', 
      value: stats?.totalOrders?.toLocaleString() || '0', 
      change: 8.2,
      icon: ShoppingCart,
      color: 'bg-blue-500' 
    },
    { 
      label: 'Total Products', 
      value: stats?.totalProducts?.toLocaleString() || '0', 
      change: 5.1,
      icon: Package,
      color: 'bg-purple-500' 
    },
    { 
      label: 'Total Users', 
      value: stats?.totalUsers?.toLocaleString() || '0', 
      change: 15.3,
      icon: Users,
      color: 'bg-orange-500' 
    },
  ];

  return (
    <div className="space-y-6">
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat) => {
          const Icon = stat.icon;
          const isPositive = stat.change >= 0;
          return (
            <div
              key={stat.label}
              className="bg-white rounded-xl shadow-sm p-6 border border-gray-100"
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-gray-500 text-sm">{stat.label}</p>
                  <p className="text-2xl font-bold text-gray-900 mt-1">
                    {statsLoading ? '...' : stat.value}
                  </p>
                </div>
                <div className={`${stat.color} p-3 rounded-lg`}>
                  <Icon className="w-6 h-6 text-white" />
                </div>
              </div>
              <div className="flex items-center mt-4">
                {isPositive ? (
                  <TrendingUp className="w-4 h-4 text-green-500 mr-1" />
                ) : (
                  <TrendingDown className="w-4 h-4 text-red-500 mr-1" />
                )}
                <span className={`text-sm ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                  {isPositive ? '+' : ''}{stat.change}%
                </span>
                <span className="text-gray-500 text-sm ml-2">vs last month</span>
              </div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Orders */}
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-lg font-semibold">Recent Orders</h2>
            <Link
              href="/admin/orders"
              className="text-blue-600 hover:text-blue-700 text-sm font-medium"
            >
              View All →
            </Link>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="text-left text-gray-500 text-sm border-b">
                  <th className="pb-3 font-medium">Order ID</th>
                  <th className="pb-3 font-medium">Customer</th>
                  <th className="pb-3 font-medium">Total</th>
                  <th className="pb-3 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                {ordersLoading ? (
                  <tr>
                    <td colSpan={4} className="py-4 text-center text-gray-500">
                      Loading...
                    </td>
                  </tr>
                ) : recentOrders?.length > 0 ? (
                  recentOrders.map((order: any) => (
                    <tr key={order.id} className="border-b last:border-0">
                      <td className="py-3 font-medium">#{order.id}</td>
                      <td className="py-3">{order.user?.firstName || 'N/A'}</td>
                      <td className="py-3">${order.total?.toFixed(2)}</td>
                      <td className="py-3">
                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${ORDER_STATUS_COLORS[order.status] || 'bg-gray-100 text-gray-700'}`}>
                          {order.status}
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} className="py-4 text-center text-gray-500">
                      No recent orders
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <h2 className="text-lg font-semibold mb-6">Quick Actions</h2>
          <div className="grid grid-cols-2 gap-4">
            <Link
              href="/admin/products/new"
              className="flex items-center justify-center gap-2 p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <Package className="w-5 h-5 text-blue-600" />
              <span className="font-medium">Add Product</span>
            </Link>
            <Link
              href="/admin/orders"
              className="flex items-center justify-center gap-2 p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <ShoppingCart className="w-5 h-5 text-green-600" />
              <span className="font-medium">View Orders</span>
            </Link>
            <Link
              href="/admin/users"
              className="flex items-center justify-center gap-2 p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <Users className="w-5 h-5 text-purple-600" />
              <span className="font-medium">Manage Users</span>
            </Link>
            <Link
              href="/admin/analytics"
              className="flex items-center justify-center gap-2 p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <TrendingUp className="w-5 h-5 text-orange-600" />
              <span className="font-medium">Analytics</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
