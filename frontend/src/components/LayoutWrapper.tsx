'use client';

import { usePathname } from 'next/navigation';
import { ReactNode } from 'react';
import { Header } from '@/components/Header';
import { Footer } from '@/components/Footer';
import { FloatingChatButton } from '@/components/FloatingChatButton';

interface LayoutWrapperProps {
  children: ReactNode;
}

export function LayoutWrapper({ children }: LayoutWrapperProps) {
  const pathname = usePathname();
  const isAdminRoute = pathname?.startsWith('/admin');
  const isCustomerRoute = pathname?.startsWith('/customer');
  const isSellerRoute = pathname?.startsWith('/seller');

  // Admin, customer, and seller routes use their own layout (no global Header/Footer)
  if (isAdminRoute || isCustomerRoute || isSellerRoute) {
    return <>{children}</>;
  }

  // Regular pages use the general Header and Footer
  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <main className="flex-grow">{children}</main>
      <Footer />
      <FloatingChatButton />
    </div>
  );
}
