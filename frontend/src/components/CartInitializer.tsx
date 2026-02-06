'use client';

import { useEffect } from 'react';
import { useCart } from '@/context/CartContext';
import { updateCartIcon } from '@/lib/cartUtils';

/**
 * CartInitializer component
 * Initializes cart on app startup and syncs cart icon
 * Should be placed inside CartProvider
 */
export function CartInitializer() {
  const { cart, itemCount, fetchCart } = useCart();

  useEffect(() => {
    // Initialize cart on mount
    const initializeCart = async () => {
      try {
        console.log('🚀 Initializing cart on app startup...');
        await fetchCart();
      } catch (error) {
        console.warn('⚠️ Failed to initialize cart on startup:', error);
      }
    };

    initializeCart();
  }, [fetchCart]);

  useEffect(() => {
    // Update cart icon whenever item count changes
    if (typeof window !== 'undefined') {
      updateCartIcon(itemCount);
    }
  }, [itemCount]);

  return null; // This component doesn't render anything
}