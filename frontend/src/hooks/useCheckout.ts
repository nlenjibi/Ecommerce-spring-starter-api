/**
 * useCheckout Hook
 * 
 * Handles the complete checkout flow including:
 * - Cart validation
 * - Order creation from cart
 * - Payment processing
 * - Post-checkout cleanup
 */

'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useCart } from '@/context/CartContext';
import { validateCart, clearCartId } from '@/lib/cartUtils';
import toast from 'react-hot-toast';

// API Configuration
const _rawApiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9190';
const API_BASE_URL = (() => {
  try {
    let u = _rawApiBase.trim();
    if (u.endsWith('/')) u = u.slice(0, -1);
    if (u.toLowerCase().endsWith('/api')) return u;
    return `${u}/api`;
  } catch (e) {
    return 'http://localhost:9190/api';
  }
})();

interface CheckoutOptions {
  shippingMethod?: 'STANDARD' | 'EXPRESS' | 'OVERNIGHT';
  paymentMethod?: 'CREDIT_CARD' | 'DEBIT_CARD' | 'PAYPAL' | 'BANK_TRANSFER' | 'MOBILE_MONEY';
  customerEmail?: string;
  customerName?: string;
  customerNotes?: string;
  shippingAddress?: string;
}

interface OrderResponse {
  id: number;
  orderNumber: string;
  userId: number;
  status: string;
  paymentStatus: string;
  paymentMethod: string;
  shippingMethod: string;
  subtotal: number;
  taxAmount: number;
  shippingCost: number;
  discountAmount: number;
  totalAmount: number;
  shippingAddress: string;
  trackingNumber?: string;
  carrier?: string;
  orderDate: string;
  items: Array<{
    id: number;
    productId: number;
    productName: string;
    productSku: string;
    productImageUrl: string;
    totalPrice: number;
  }>;
  itemCount: number;
  customerNotes?: string;
}

export function useCheckout() {
  const [isProcessing, setIsProcessing] = useState(false);
  const [validationErrors, setValidationErrors] = useState<any[]>([]);
  const router = useRouter();
  const { cart, cartId, fetchCart } = useCart();

  const getAuthToken = (): string | null => {
    if (typeof window === 'undefined') return null;
    // Support multiple token storage formats used across the app:
    // 1. Single-token legacy key: 'auth_token'
    // 2. Newer structured key: 'auth_tokens' (JSON with accessToken)
    const legacy = localStorage.getItem('auth_token');
    if (legacy) return legacy;

    const stored = localStorage.getItem('auth_tokens');
    if (stored) {
      try {
        const parsed = JSON.parse(stored) as any;
        return parsed?.accessToken || parsed?.access_token || null;
      } catch (e) {
        // fall through
      }
    }

    // Fallbacks for other possible keys
    return localStorage.getItem('access_token') || localStorage.getItem('token') || null;
  };

  const getUsername = (): string | null => {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('username');
  };

  /**
   * Validate cart before checkout
   */
  const validateBeforeCheckout = async (): Promise<boolean> => {
    try {
      console.log('🔍 Validating cart before checkout...');
      
      const validation = await validateCart();
      
      if (!validation || !validation.valid) {
        setValidationErrors(validation?.issues || []);
        
        if (validation?.issues && validation.issues.length > 0) {
          // Show first 3 validation errors
          validation.issues.slice(0, 3).forEach(issue => {
            toast.error(issue.message);
          });
        } else {
          toast.error('Cart validation failed. Please review your items.');
        }
        
        return false;
      }
      
      setValidationErrors([]);
      console.log('✅ Cart validation passed');
      return true;
    } catch (error) {
      console.error('❌ Cart validation error:', error);
      toast.error('Failed to validate cart');
      return false;
    }
  };

  /**
   * Create order from cart
   */
  const createOrder = async (options: CheckoutOptions = {}): Promise<OrderResponse | null> => {
    if (!cart || !cartId) {
      toast.error('No active cart found');
      return null;
    }

    if (cart.items.length === 0) {
      toast.error('Your cart is empty');
      return null;
    }

    setIsProcessing(true);

    try {
      // Step 1: Validate cart
      const isValid = await validateBeforeCheckout();
      if (!isValid) {
        setIsProcessing(false);
        return null;
      }

      console.log('🛒 Creating order from cart:', cartId);

      const token = getAuthToken();
      const username = getUsername();

      if (!token || !username) {
        toast.error('Please login to complete checkout');
        router.push('/login?returnUrl=/checkout');
        setIsProcessing(false);
        return null;
      }

      // Step 2: Prepare order data from cart
      const orderData = {
        // Map cart items to order items
        items: cart.items.map(item => ({
          productId: item.product.id,
          quantity: item.quantity,
        })),
        
        // Shipping and payment details
        shippingMethod: options.shippingMethod || 'STANDARD',
        paymentMethod: options.paymentMethod || 'CREDIT_CARD',
        
        // Customer information
        customerEmail: options.customerEmail || '',
        customerName: options.customerName || username,
        customerNotes: options.customerNotes || '',
        
        // Apply coupon if exists
        ...(cart.couponCode && { couponCode: cart.couponCode }),
        ...(cart.discount > 0 && { couponDiscount: cart.discount }),
        
        // Tax rate (you might want to calculate this based on location)
        taxRate: 0.08, // 8% tax rate
        
        // Shipping address
        ...(options.shippingAddress && { shippingAddress: options.shippingAddress }),
      };

      console.log('📦 Order data:', orderData);

      // Step 3: Create order via API
      const headers: HeadersInit = {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      };

      const response = await fetch(
        `${API_BASE_URL}/v1/orders?Id=${cartId}`,
        {
          method: 'POST',
          headers,
          body: JSON.stringify(orderData),
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || 'Failed to create order');
      }

      const responseData = await response.json();
      
      // Handle wrapped response
      const order: OrderResponse = responseData.success !== undefined && responseData.data !== undefined
        ? responseData.data
        : responseData;

      console.log('✅ Order created successfully:', order);

      // Step 4: Clear cart after successful order
      clearCartId();
      await fetchCart(); // This will initialize a new cart

      toast.success(`Order ${order.orderNumber} placed successfully!`);

      return order;
    } catch (error: any) {
      console.error('❌ Checkout failed:', error);
      toast.error(error.message || 'Failed to complete checkout');
      return null;
    } finally {
      setIsProcessing(false);
    }
  };

  /**
   * Complete checkout and redirect to success page
   */
  const checkout = async (options: CheckoutOptions = {}): Promise<void> => {
    const order = await createOrder(options);
    
    if (order) {
      // Redirect to success page with order details
      router.push(`/checkout/success?orderId=${order.orderNumber}`);
    }
  };

  return {
    checkout,
    createOrder,
    validateBeforeCheckout,
    isProcessing,
    validationErrors,
  };
}