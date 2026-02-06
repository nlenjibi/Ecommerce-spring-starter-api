'use client';

import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { toast } from 'react-hot-toast';

// ==================== Types ====================

export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  category?: string | { name: string };
  imageUrl?: string;
  inStock?: boolean;
  stockQuantity?: number;
}

export interface CartItem {
  id: number;
  product: Product;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Cart {
  id: number;
  status: string;
  dateCreated: string;
  updatedAt: string;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  discount: number;
  totalPrice: number;
  couponCode?: string;
}

export interface CartSummary {
  id: number;
  status: string;
  itemCount: number;
  uniqueItemCount: number;
  subtotal: number;
  discount: number;
  totalPrice: number;
  couponCode?: string;
}

export interface ValidationIssue {
  type: 'OUT_OF_STOCK' | 'INSUFFICIENT_STOCK' | 'PRICE_CHANGED' | 'ITEM_UNAVAILABLE';
  productId: number;
  productName: string;
  message: string;
  requestedQuantity?: number;
  availableQuantity?: number;
  oldPrice?: number;
  newPrice?: number;
}

export interface CartValidationResult {
  valid: boolean;
  message: string;
  issues: ValidationIssue[];
  originalTotal: number;
  updatedTotal: number;
  priceChanged: boolean;
  stockChanged: boolean;
}

export interface ShippingOption {
  method: string;
  name: string;
  cost: number;
  minDays: number;
  maxDays: number;
  description: string;
}

export interface ShippingEstimate {
  cost: number;
  currency: string;
  estimatedDays: number;
  method: string;
  availableOptions: ShippingOption[];
}

// ==================== Context Type ====================

interface CartContextType {
  // Cart state
  cart: Cart | null;
  items: CartItem[];
  itemCount: number;
  total: number;
  subtotal: number;
  discount: number;
  loading: boolean;
  cartId: number | null;
  
  // Cart operations
  fetchCart: () => Promise<void>;
  loadCart: () => Promise<void>;
  mergeCart: (guestCartId: number) => Promise<any>;
  addToCart: (productId: number, quantity?: number) => Promise<void>;
  bulkAddToCart: (items: Array<{ productId: number; quantity: number }>) => Promise<void>;
  updateQuantity: (productId: number, quantity: number) => Promise<void>;
  removeFromCart: (productId: number) => Promise<void>;
  clearCart: () => Promise<void>;
  
  // Coupon operations
  applyCoupon: (couponCode: string) => Promise<void>;
  removeCoupon: () => Promise<void>;
  
  // Validation
  validateCart: () => Promise<CartValidationResult | null>;
  validationResult: CartValidationResult | null;
  
  // Shipping
  estimateShipping: (address: {
    country: string;
    state: string;
    postalCode: string;
    city?: string;
    shippingMethod?: string;
  }) => Promise<ShippingEstimate | null>;
  shippingEstimate: ShippingEstimate | null;
  
  // Advanced features
  shareCart: () => Promise<string | null>;
  saveForLater: () => Promise<void>;
  
  // Checkout
  checkout: () => Promise<any>;
  
  // Summary (lightweight)
  fetchSummary: () => Promise<CartSummary | null>;
}

// ==================== Context ====================

const CartContext = createContext<CartContextType | undefined>(undefined);

// ==================== API Configuration ====================

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

const CART_STORAGE_KEY = 'cart_id';
const USERNAME_STORAGE_KEY = 'username'; // Add username storage

// ==================== Helper Functions ====================

const getStoredCartId = (): number | null => {
  if (typeof window === 'undefined') return null;
  const stored = localStorage.getItem(CART_STORAGE_KEY);
  return stored ? parseInt(stored, 10) : null;
};

const setStoredCartId = (cartId: number) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem(CART_STORAGE_KEY, cartId.toString());
  }
};

const removeStoredCartId = () => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem(CART_STORAGE_KEY);
  }
};

const getAuthToken = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('auth_token');
};

const getUsername = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(USERNAME_STORAGE_KEY);
};

const isAuthenticated = (): boolean => {
  return !!getAuthToken() && !!getUsername();
};

// ==================== Provider ====================

export function CartProvider({ children }: { children: React.ReactNode }) {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [validationResult, setValidationResult] = useState<CartValidationResult | null>(null);
  const [shippingEstimate, setShippingEstimate] = useState<ShippingEstimate | null>(null);

  // ==================== API Calls ====================

  const apiCall = async (
    endpoint: string,
    options: RequestInit = {}
  ): Promise<any> => {
    const token = getAuthToken();
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      let errorData;
      try {
        errorData = await response.json();
      } catch {
        errorData = { message: 'An error occurred' };
      }
      
      const errorMessage = errorData.message || errorData.error || `HTTP ${response.status}`;
      const error = new Error(errorMessage);
      
      (error as any).status = response.status;
      (error as any).data = errorData;
      
      throw error;
    }

    const data = await response.json();
    
    // Handle wrapped responses (success: true, data: {...})
    if (data.success !== undefined && data.data !== undefined) {
      return data.data;
    }
    
    return data;
  };

  // ==================== Initialize or Get Cart ====================

  const initializeCart = useCallback(async () => {
    try {
      setLoading(true);
      const storedCartId = getStoredCartId();

      // If user is authenticated, try to get their cart using /v1/carts/me endpoint
      if (isAuthenticated()) {
        const username = getUsername();
        try {
          const cartData = await apiCall(`/v1/carts/me?username=${encodeURIComponent(username!)}`);
          setCart(cartData);
          setStoredCartId(cartData.id);
          return;
        } catch (error: any) {
          console.log('No existing user cart, will create one when item is added');
        }
      }

      // For guest users or if authenticated user has no cart yet
      if (storedCartId) {
        try {
          const cartData = await apiCall(`/v1/carts/${storedCartId}`);
          setCart(cartData);
          return;
        } catch (error) {
          console.log('Stored cart not found');
          removeStoredCartId();
        }
      }

      // No cart exists yet - will be created when first item is added
      setCart(null);
    } catch (error: any) {
      console.error('Failed to initialize cart:', error);
      toast.error('Failed to load cart');
    } finally {
      setLoading(false);
    }
  }, []);

  // ==================== Create Cart ====================

  const createCart = useCallback(async (): Promise<Cart> => {
    const username = getUsername();
    const endpoint = username 
      ? `/v1/carts?username=${encodeURIComponent(username)}`
      : '/v1/carts';
    
    const newCart = await apiCall(endpoint, { method: 'POST' });
    setCart(newCart);
    setStoredCartId(newCart.id);
    return newCart;
  }, []);

  // ==================== Fetch Cart ====================

  const fetchCart = useCallback(async () => {
    const cartId = getStoredCartId();
    
    if (!cartId) {
      await initializeCart();
      return;
    }

    try {
      const cartData = await apiCall(`/v1/carts/${cartId}`);
      setCart(cartData);
    } catch (error: any) {
      console.error('Failed to fetch cart:', error);
      if (error.status === 404) {
        await initializeCart();
      }
    }
  }, [initializeCart]);

  // Alias for fetchCart
  const loadCart = fetchCart;

  // ==================== Merge Cart ====================

  const mergeCart = useCallback(async (guestCartId: number) => {
    try {
      setLoading(true);
      const username = getUsername();
      
      if (!username) {
        throw new Error('User must be authenticated to merge carts');
      }

      const mergedCart = await apiCall(`/v1/carts/merge?username=${encodeURIComponent(username)}`, {
        method: 'POST',
        body: JSON.stringify({ guestCartId }),
      });

      setCart(mergedCart);
      setStoredCartId(mergedCart.id);
      toast.success('Carts merged successfully');
      
      return mergedCart;
    } catch (error: any) {
      console.error('Failed to merge cart:', error);
      toast.error('Failed to merge carts');
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  // ==================== Add to Cart ====================

  const addToCart = useCallback(
    async (productId: number, quantity: number = 1) => {
      try {
        setLoading(true);
        let currentCartId = getStoredCartId();

        // Create cart if it doesn't exist
        if (!currentCartId) {
          const newCart = await createCart();
          currentCartId = newCart.id;
        }

        await apiCall(`/v1/carts/${currentCartId}/items`, {
          method: 'POST',
          body: JSON.stringify({ productId, quantity }),
        });

        await fetchCart();
        toast.success('Item added to cart');
      } catch (error: any) {
        console.error('Failed to add to cart:', error);
        
        let userMessage = 'Failed to add item to cart';
        
        if (error.message?.includes('stock')) {
          userMessage = error.message;
        } else if (error.status === 404) {
          userMessage = 'Product not found';
        } else if (error.status === 400) {
          userMessage = error.message || 'Invalid request';
        }
        
        toast.error(userMessage);
      } finally {
        setLoading(false);
      }
    },
    [createCart, fetchCart]
  );

  // ==================== Bulk Add to Cart ====================

  const bulkAddToCart = useCallback(
    async (items: Array<{ productId: number; quantity: number }>) => {
      try {
        setLoading(true);
        let currentCartId = getStoredCartId();

        if (!currentCartId) {
          const newCart = await createCart();
          currentCartId = newCart.id;
        }

        await apiCall(`/v1/carts/${currentCartId}/items/bulk`, {
          method: 'POST',
          body: JSON.stringify({ items }),
        });

        await fetchCart();
        toast.success(`${items.length} items added to cart`);
      } catch (error: any) {
        console.error('Failed to bulk add to cart:', error);
        toast.error('Failed to add items to cart');
      } finally {
        setLoading(false);
      }
    },
    [createCart, fetchCart]
  );

  // ==================== Update Quantity ====================

  const updateQuantity = useCallback(
    async (productId: number, quantity: number) => {
      try {
        setLoading(true);
        const cartId = getStoredCartId();
        
        if (!cartId) return;

        await apiCall(`/v1/carts/${cartId}/items/${productId}`, {
          method: 'PUT',
          body: JSON.stringify({ quantity }),
        });

        await fetchCart();
      } catch (error: any) {
        console.error('Failed to update quantity:', error);
        
        let userMessage = 'Failed to update quantity';
        
        if (error.message?.includes('stock')) {
          userMessage = error.message;
        } else if (error.status === 404) {
          userMessage = 'Item not found in cart';
        }
        
        toast.error(userMessage);
      } finally {
        setLoading(false);
      }
    },
    [fetchCart]
  );

  // ==================== Remove from Cart ====================

  const removeFromCart = useCallback(
    async (productId: number) => {
      try {
        setLoading(true);
        const cartId = getStoredCartId();
        
        if (!cartId) return;

        await apiCall(`/v1/carts/${cartId}/items/${productId}`, {
          method: 'DELETE',
        });

        await fetchCart();
        toast.success('Item removed from cart');
      } catch (error: any) {
        console.error('Failed to remove item:', error);
        
        let userMessage = 'Failed to remove item';
        
        if (error.status === 404) {
          userMessage = 'Item not found in cart';
        }
        
        toast.error(userMessage);
      } finally {
        setLoading(false);
      }
    },
    [fetchCart]
  );

  // ==================== Clear Cart ====================

  const clearCart = useCallback(async () => {
    try {
      setLoading(true);
      const cartId = getStoredCartId();
      
      if (!cartId) return;

      await apiCall(`/v1/carts/${cartId}/items`, {
        method: 'DELETE',
      });

      await fetchCart();
      toast.success('Cart cleared');
    } catch (error: any) {
      console.error('Failed to clear cart:', error);
      toast.error('Failed to clear cart');
    } finally {
      setLoading(false);
    }
  }, [fetchCart]);

  // ==================== Apply Coupon ====================

  const applyCoupon = useCallback(
    async (couponCode: string) => {
      try {
        setLoading(true);
        const cartId = getStoredCartId();
        
        if (!cartId) return;

        const updatedCart = await apiCall(`/v1/carts/${cartId}/coupons`, {
          method: 'POST',
          body: JSON.stringify({ couponCode }),
        });

        setCart(updatedCart);
        toast.success(`Coupon "${couponCode}" applied successfully!`);
      } catch (error: any) {
        console.error('Failed to apply coupon:', error);
        toast.error(error.message || 'Invalid coupon code');
      } finally {
        setLoading(false);
      }
    },
    []
  );

  // ==================== Remove Coupon ====================

  const removeCoupon = useCallback(async () => {
    try {
      setLoading(true);
      const cartId = getStoredCartId();
      
      if (!cartId) return;

      const updatedCart = await apiCall(`/v1/carts/${cartId}/coupons`, {
        method: 'DELETE',
      });

      setCart(updatedCart);
      toast.success('Coupon removed');
    } catch (error: any) {
      console.error('Failed to remove coupon:', error);
      toast.error('Failed to remove coupon');
    } finally {
      setLoading(false);
    }
  }, []);

  // ==================== Validate Cart ====================

  const validateCart = useCallback(async (): Promise<CartValidationResult | null> => {
    try {
      const cartId = getStoredCartId();
      
      if (!cartId) return null;

      const result = await apiCall(`/v1/carts/${cartId}/validate`, {
        method: 'POST',
      });

      setValidationResult(result);
      
      if (!result.valid) {
        toast.error(`Cart has ${result.issues.length} issue(s)`);
      }

      return result;
    } catch (error: any) {
      console.error('Failed to validate cart:', error);
      toast.error('Failed to validate cart');
      return null;
    }
  }, []);

  // ==================== Estimate Shipping ====================

  const estimateShipping = useCallback(
    async (address: {
      country: string;
      state: string;
      postalCode: string;
      city?: string;
      shippingMethod?: string;
    }): Promise<ShippingEstimate | null> => {
      try {
        const cartId = getStoredCartId();
        
        if (!cartId) return null;

        const estimate = await apiCall(`/v1/carts/${cartId}/estimate-shipping`, {
          method: 'POST',
          body: JSON.stringify(address),
        });

        setShippingEstimate(estimate);
        return estimate;
      } catch (error: any) {
        console.error('Failed to estimate shipping:', error);
        toast.error('Failed to estimate shipping');
        return null;
      }
    },
    []
  );

  // ==================== Share Cart ====================

  const shareCart = useCallback(async (): Promise<string | null> => {
    try {
      const cartId = getStoredCartId();
      
      if (!cartId) return null;

      const response = await apiCall(`/v1/carts/${cartId}/share`, {
        method: 'POST',
      });

      const shareUrl = `${window.location.origin}/cart/shared/${response.shareToken}`;
      
      await navigator.clipboard.writeText(shareUrl);
      toast.success('Share link copied to clipboard!');
      
      return shareUrl;
    } catch (error: any) {
      console.error('Failed to share cart:', error);
      toast.error('Failed to create share link');
      return null;
    }
  }, []);

  // ==================== Save for Later ====================

  const saveForLater = useCallback(async () => {
    try {
      setLoading(true);
      const cartId = getStoredCartId();
      const username = getUsername();
      
      if (!cartId || !username) {
        toast.error('Please login to save items');
        return;
      }

      await apiCall(`/v1/carts/${cartId}/save-for-later?username=${encodeURIComponent(username)}`, {
        method: 'POST',
      });

      await fetchCart();
      toast.success('Cart items saved to wishlist');
    } catch (error: any) {
      console.error('Failed to save for later:', error);
      toast.error(error.message || 'Failed to save items');
    } finally {
      setLoading(false);
    }
  }, [fetchCart]);

  // ==================== Fetch Summary ====================

  const fetchSummary = useCallback(async (): Promise<CartSummary | null> => {
    try {
      const cartId = getStoredCartId();
      
      if (!cartId) return null;

      const summary = await apiCall(`/v1/carts/${cartId}/summary`);
      return summary;
    } catch (error: any) {
      console.error('Failed to fetch cart summary:', error);
      return null;
    }
  }, []);

  // ==================== Checkout ====================

  const checkout = useCallback(async () => {
    try {
      setLoading(true);
      
      // Validate cart first
      const validation = await validateCart();
      
      if (!validation?.valid) {
        throw new Error('Please resolve cart issues before checkout');
      }

      const cartId = getStoredCartId();
      if (!cartId) throw new Error('No active cart');

      // Create order (you'll need to implement the order creation endpoint)
      // For now, returning a placeholder
      const orderResponse = {
        orderId: Math.floor(Math.random() * 10000),
        total: cart?.totalPrice || 0,
      };

      // Clear cart after successful checkout
      removeStoredCartId();
      setCart(null);

      return orderResponse;
    } catch (error: any) {
      console.error('Checkout failed:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [validateCart, cart]);

  // ==================== Initialize on Mount ====================

  useEffect(() => {
    initializeCart();
  }, [initializeCart]);

  // ==================== Computed Values ====================

  const computedValues = useMemo(() => ({
    items: cart?.items || [],
    itemCount: cart?.itemCount || 0,
    total: cart?.totalPrice || 0,
    subtotal: cart?.subtotal || 0,
    discount: cart?.discount || 0,
    cartId: cart?.id || null,
  }), [cart]);

  const { items, itemCount, total, subtotal, discount, cartId } = computedValues;

  // ==================== Context Value ====================

  const value: CartContextType = {
    cart,
    items,
    itemCount,
    total,
    subtotal,
    discount,
    loading,
    cartId,
    fetchCart,
    loadCart,
    mergeCart,
    addToCart,
    bulkAddToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    applyCoupon,
    removeCoupon,
    validateCart,
    validationResult,
    estimateShipping,
    shippingEstimate,
    shareCart,
    saveForLater,
    checkout,
    fetchSummary,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

// ==================== Hook ====================

export function useCart() {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}