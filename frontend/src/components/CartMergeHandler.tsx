'use client';

import { useEffect, useRef } from 'react';
import { useCart } from '@/context/CartContext';
import { getCartId, getUsername, getAuthToken } from '@/lib/cartUtils';
import toast from 'react-hot-toast';

/**
 * CartMergeHandler component
 * Handles cart merge when user authenticates
 * Merges guest cart with user cart if both exist
 * Should be placed inside both AuthProvider and CartProvider
 */
export function CartMergeHandler() {
  const { mergeCart, fetchCart, cart } = useCart();
  const hasMergedRef = useRef(false);
  const lastAuthStateRef = useRef<boolean>(false);

  useEffect(() => {
    const handleCartMerge = async () => {
      const username = getUsername();
      const token = getAuthToken();
      const isAuthenticated = !!(username && token);
      
      // Check if authentication state changed from false to true
      const justLoggedIn = isAuthenticated && !lastAuthStateRef.current;
      
      // Update last auth state
      lastAuthStateRef.current = isAuthenticated;

      // Only handle merge when user just logged in and hasn't merged yet
      if (justLoggedIn && !hasMergedRef.current) {
        hasMergedRef.current = true;
        
        const guestCartId = getCartId();
        
        if (guestCartId) {
          console.log('🔀 User authenticated, attempting cart merge...');
          console.log('📦 Guest cart ID:', guestCartId);
          
          try {
            // Try to merge the guest cart with user cart
            const mergedCart = await mergeCart(guestCartId);
            
            if (mergedCart) {
              console.log('✅ Cart merged successfully');
              toast.success(`Welcome back! Your cart has been updated.`);
            } else {
              console.log('📦 Merge not available, loading user cart');
              await fetchCart();
            }
          } catch (error: any) {
            console.error('❌ Failed to merge cart on authentication:', error);
            
            // Don't show error to user - just load their cart
            console.log('📦 Fallback: Loading user cart');
            await fetchCart();
          }
        } else {
          console.log('📦 No guest cart to merge, loading user cart');
          await fetchCart();
        }
      }
      
      // Reset merge flag when user logs out
      if (!isAuthenticated && hasMergedRef.current) {
        console.log('👋 User logged out, resetting merge flag');
        hasMergedRef.current = false;
      }
    };

    // Small delay to ensure cart context is fully initialized
    const timer = setTimeout(handleCartMerge, 200);

    return () => clearTimeout(timer);
  }, [mergeCart, fetchCart]);

  return null; // This component doesn't render anything
}