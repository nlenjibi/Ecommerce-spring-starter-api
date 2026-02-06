'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { ReactNode } from 'react';
import {
  LayoutDashboard,
  Package,
  FolderTree,
  Tags,
  ShoppingCart,
  Users,
  Percent,
  BarChart3,
  Settings,
  LogOut,
  Activity,
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { AdminHeader } from '@/components/admin/AdminHeader';
import { AdminFooter } from '@/components/admin/AdminFooter';

interface AdminLayoutProps {
  children: ReactNode;
}

const sidebarLinks = [
  { href: '/admin', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/admin/products', label: 'Products', icon: Package },
  { href: '/admin/categories', label: 'Categories', icon: FolderTree },
  { href: '/admin/tags', label: 'Tags', icon: Tags },
  { href: '/admin/orders', label: 'Orders', icon: ShoppingCart },
  { href: '/admin/users', label: 'Users', icon: Users },
  { href: '/admin/promotions', label: 'Promotions', icon: Percent },
  { href: '/admin/analytics', label: 'Analytics', icon: BarChart3 },
  { href: '/admin/tracking', label: 'Event Tracking', icon: Activity },
  { href: '/admin/settings', label: 'Settings', icon: Settings },
];

export default function AdminLayout({ children }: AdminLayoutProps) {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const isActiveLink = (href: string) => {
    if (href === '/admin') {
      return pathname === '/admin';
    }
    return pathname.startsWith(href);
  };

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="fixed inset-y-0 left-0 w-64 bg-gray-900 text-white">
        <div className="flex items-center justify-center h-16 border-b border-gray-800">
          <Link href="/admin" className="text-xl font-bold">
            ShopHub Admin
          </Link>
        </div>

        <nav className="mt-6">
          <ul className="space-y-1 px-3">
            {sidebarLinks.map((link) => {
              const Icon = link.icon;
              const isActive = isActiveLink(link.href);
              return (
                <li key={link.href}>
                  <Link
                    href={link.href}
                    className={`flex items-center px-4 py-3 rounded-lg transition-colors ${
                      isActive
                        ? 'bg-blue-600 text-white'
                        : 'text-gray-300 hover:bg-gray-800 hover:text-white'
                    }`}
                  >
                    <Icon className="w-5 h-5 mr-3" />
                    {link.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>

        {/* User section */}
        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-800">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <div className="w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center">
                <span className="text-sm font-medium">
                  {user?.firstName?.charAt(0) || 'A'}
                </span>
              </div>
              <div className="ml-3">
                <p className="text-sm font-medium">{user?.firstName || 'Admin User'}</p>
                <p className="text-xs text-gray-400">{user ? 'Administrator' : 'Guest Mode'}</p>
              </div>
            </div>
            {user && (
              <button
                onClick={logout}
                className="p-2 text-gray-400 hover:text-white transition-colors"
                title="Logout"
              >
                <LogOut className="w-5 h-5" />
              </button>
            )}
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="ml-64 min-h-screen flex flex-col">
        <AdminHeader />
        <div className="flex-1 p-6">{children}</div>
        <AdminFooter />
      </main>
    </div>
  );
}
