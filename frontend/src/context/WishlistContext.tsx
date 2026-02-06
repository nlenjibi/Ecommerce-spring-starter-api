"use client";

import React, { createContext, useContext, useEffect, useState, useCallback, useMemo } from "react";
import { useAuth } from "@/context/AuthContext";
import { useCart } from "./CartContext";
import { Product } from '@/types';
import toast from 'react-hot-toast';
import { API_BASE_URL } from '@/lib/constants';
import {
  generateGuestSession,
  getGuestWishlist as getGuestWishlistApi,
  addToGuestWishlist as addToGuestWishlistApi,
  removeFromGuestWishlist as removeFromGuestWishlistApi,
  clearGuestWishlist as clearGuestWishlistApi,
} from '@/lib/wishlistApi';

// ==================== Types ====================

export type WishlistPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export type WishlistItem = {
  id: number;
  userId?: number;
  productId: number;
  product: Product;
  notes?: string;
  priority: WishlistPriority;
  desiredQuantity: number;
  
  // Price tracking
  priceWhenAdded: number;
  currentPrice: number;
  priceDifference: number;
  isPriceDropped: boolean;
  targetPrice?: number;
  
  // Notifications
  notifyOnPriceDrop: boolean;
  notifyOnStock: boolean;
  shouldNotifyPriceDrop: boolean;
  shouldNotifyStock: boolean;
  
  // Status
  purchased: boolean;
  isPublic: boolean;
  inStock: boolean;
  
  // Collection & Organization
  collectionName?: string;
  tags?: string[];
  
  // Timestamps
  addedAt: string;
  purchasedAt?: string;
  
  // Reminder
  reminderEnabled?: boolean;
  reminderDate?: string;
};

export type WishlistSummary = {
  userId: number;
  totalItems: number;
  inStockItems: number;
  outOfStockItems: number;
  itemsWithPriceDrops: number;
  purchasedItems: number;
  totalValue: number;
  totalSavings: number;
  items: WishlistItem[];
};

export type AddToWishlistRequest = {
  productId: number;
  notes?: string;
  priority?: WishlistPriority;
  desiredQuantity?: number;
  targetPrice?: number;
  notifyOnPriceDrop?: boolean;
  notifyOnStock?: boolean;
  isPublic?: boolean;
  collectionName?: string;
  tags?: string[];
};

export type UpdateWishlistItemRequest = {
  notes?: string;
  priority?: WishlistPriority;
  desiredQuantity?: number;
  targetPrice?: number;
  notifyOnPriceDrop?: boolean;
  notifyOnStock?: boolean;
  isPublic?: boolean;
  collectionName?: string;
  tags?: string[];
};

export type WishlistAnalytics = {
  totalItems: number;
  itemsAddedThisMonth: number;
  itemsPurchased: number;
  itemsWithPriceDrops: number;
  averagePriceDrop: number;
  totalSavings: number;
  mostAddedCategory: string;
  categoryBreakdown: {
    categoryName: string;
    itemCount: number;
    totalValue: number;
    averagePrice: number;
  }[];
};

export type WishlistOptimizationRequest = {
  maxBudget?: number;
  priorityOrder?: string[];
  includeOnlyInStock?: boolean;
  maxItems?: number;
  optimizationStrategy?: "PRIORITY" | "PRICE" | "SAVINGS" | "BALANCED";
};

// ==================== Context Type ====================

type WishlistContextType = {
  // State
  wishlist: WishlistItem[];
  items: WishlistItem[];
  itemCount: number;
  summary: WishlistSummary | null;
  analytics: WishlistAnalytics | null;
  collections: string[];
  isLoading: boolean;
  guestSessionId: string | null;
  
  // Basic Operations
  addToWishlist: (productId: number, options?: Partial<AddToWishlistRequest>) => Promise<boolean>;
  removeFromWishlist: (productId: number) => Promise<boolean>;
  updateWishlistItem: (productId: number, updates: UpdateWishlistItemRequest) => Promise<boolean>;
  isInWishlist: (productId: number) => boolean;
  clearWishlist: () => Promise<boolean>;
  loadWishlist: () => Promise<void>;
  
  // Advanced Operations
  moveToCart: (productId: number, quantity?: number) => Promise<void>;
  moveMultipleToCart: (productIds: number[]) => Promise<void>;
  bulkAddToCart: (productIds?: number[]) => Promise<void>;
  markAsPurchased: (productId: number) => Promise<void>;
  markMultipleAsPurchased: (productIds: number[]) => Promise<void>;
  
  // Collections & Organization
  getItemsByCollection: (collectionName: string) => WishlistItem[];
  getItemsByPriority: (priority: WishlistPriority) => WishlistItem[];
  getItemsByTags: (tags: string[]) => WishlistItem[];
  moveToCollection: (productIds: number[], collectionName: string) => Promise<void>;
  loadCollections: () => Promise<void>;
  
  // Price Tracking
  getItemsWithPriceDrops: () => WishlistItem[];
  getItemsBelowTargetPrice: () => WishlistItem[];
  
  // Purchase Status
  getPurchasedItems: () => WishlistItem[];
  getUnpurchasedItems: () => WishlistItem[];
  getAvailableItems: () => WishlistItem[];
  
  // Bulk Operations
  addMultipleToWishlist: (products: Product[], options?: Partial<AddToWishlistRequest>) => Promise<void>;
  removeMultipleFromWishlist: (productIds: number[]) => Promise<void>;
  
  // Summary & Analytics
  loadSummary: () => Promise<void>;
  loadAnalytics: () => Promise<void>;
  
  // Optimization
  optimizeWishlist: (options: WishlistOptimizationRequest) => Promise<WishlistItem[]>;
  
  // Sharing
  shareWishlist: (options?: { shareName?: string; description?: string }) => Promise<{ shareUrl: string; shareToken: string }>;
  
  // Guest Operations
  mergeGuestWishlist: () => Promise<void>;
  initGuestSession: () => void;
};

const WishlistContext = createContext<WishlistContextType | undefined>(undefined);

// ==================== Provider ====================

export const WishlistProvider = ({ children }: { children: React.ReactNode }) => {
  const { user, token } = useAuth();
  const isLoggedIn = !!user;
  const { addToCart } = useCart();
  
  // State
  const [wishlist, setWishlist] = useState<WishlistItem[]>([]);
  const [summary, setSummary] = useState<WishlistSummary | null>(null);
  const [analytics, setAnalytics] = useState<WishlistAnalytics | null>(null);
  const [collections, setCollections] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [guestSessionId, setGuestSessionId] = useState<string | null>(null);
  
  const GUEST_WISHLIST_KEY = 'guest_wishlist';
  const GUEST_SESSION_KEY = 'guest_session_id';
  
  const API_BASE = `${API_BASE_URL}/v1/wishlist`;

  // ==================== Helper Functions ====================

  const getHeaders = useCallback((): HeadersInit => {
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
    };
    if (isLoggedIn && token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
  }, [isLoggedIn, token]);

  const handleApiError = useCallback((error: any, operation: string, silent = false) => {
    console.error(`[Wishlist] ${operation} failed:`, error);
    if (!silent) {
      const message = error?.message || `Failed to ${operation.toLowerCase()}`;
      toast.error(message);
    }
  }, []);

  const parseApiResponse = useCallback(async (response: Response) => {
    const contentType = response.headers.get('content-type') || '';
    
    if (!contentType.includes('application/json')) {
      const text = await response.text().catch(() => '');
      throw new Error(`Non-JSON response: ${text.substring(0, 100)}`);
    }

    const data = await response.json();
    return data;
  }, []);

  // ==================== Guest Session Management ====================

  const initGuestSession = useCallback(() => {
    if (isLoggedIn || guestSessionId) return;

    const storedSessionId = localStorage.getItem(GUEST_SESSION_KEY);
    if (storedSessionId) {
      setGuestSessionId(storedSessionId);
      return;
    }

    // Generate new session ID
    generateGuestSession()
      .then((res) => {
        if (res?.sessionId) {
          localStorage.setItem(GUEST_SESSION_KEY, res.sessionId);
          setGuestSessionId(res.sessionId);
          console.log('[Wishlist] Created new guest session:', res.sessionId);
        } else {
          throw new Error('No session ID returned');
        }
      })
      .catch((error) => {
        console.warn('[Wishlist] Failed to generate guest session, using fallback', error);
        const fallbackId = `GUEST_${crypto.randomUUID()}`;
        localStorage.setItem(GUEST_SESSION_KEY, fallbackId);
        setGuestSessionId(fallbackId);
      });
  }, [isLoggedIn, guestSessionId]);

  // Initialize guest session on mount
  useEffect(() => {
    if (!isLoggedIn && !guestSessionId) {
      initGuestSession();
    }
  }, [isLoggedIn, guestSessionId, initGuestSession]);

  // ==================== Load Wishlist ====================

  const loadGuestWishlist = useCallback(async () => {
    if (isLoggedIn || !guestSessionId) return;

    try {
      // Try to fetch from API first
      const items = await getGuestWishlistApi(guestSessionId);
      if (items && Array.isArray(items)) {
        setWishlist(items);
        localStorage.setItem(GUEST_WISHLIST_KEY, JSON.stringify(items));
        return;
      }
    } catch (error) {
      console.warn('[Wishlist] Failed to fetch guest wishlist from API, falling back to localStorage');
    }

    // Fallback to localStorage
    try {
      const stored = localStorage.getItem(GUEST_WISHLIST_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed)) {
          setWishlist(parsed);
        }
      }
    } catch (error) {
      console.error('[Wishlist] Failed to parse guest wishlist from localStorage', error);
    }
  }, [isLoggedIn, guestSessionId]);

  const loadUserWishlist = useCallback(async () => {
    if (!isLoggedIn || !user) return;

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}?userId=${user.id}`, {
        headers: getHeaders(),
      });

      if (!response.ok) {
        if (response.status === 404) {
          setWishlist([]);
          return;
        }
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await parseApiResponse(response);
      const items = data?.data || [];
      setWishlist(Array.isArray(items) ? items : []);
    } catch (error) {
      handleApiError(error, 'Load wishlist', true);
      setWishlist([]);
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, API_BASE, getHeaders, parseApiResponse, handleApiError]);

  const loadWishlist = useCallback(async () => {
    if (isLoggedIn) {
      await loadUserWishlist();
    } else {
      await loadGuestWishlist();
    }
  }, [isLoggedIn, loadUserWishlist, loadGuestWishlist]);

  // Load wishlist on mount and auth change
  useEffect(() => {
    if (isLoggedIn && user) {
      loadUserWishlist();
    } else if (guestSessionId) {
      loadGuestWishlist();
    }
  }, [isLoggedIn, user, guestSessionId, loadUserWishlist, loadGuestWishlist]);

  // ==================== Add to Wishlist ====================

  const addToWishlist = useCallback(async (
    productId: number,
    options: Partial<AddToWishlistRequest> = {}
  ): Promise<boolean> => {
    // Check if already in wishlist
    if (wishlist.some(item => item.product.id === productId)) {
      toast.info('Item is already in your wishlist');
      return false;
    }

    const request: AddToWishlistRequest = {
      productId,
      priority: options.priority || 'MEDIUM',
      desiredQuantity: options.desiredQuantity || 1,
      notifyOnPriceDrop: options.notifyOnPriceDrop ?? true,
      notifyOnStock: options.notifyOnStock ?? true,
      isPublic: options.isPublic ?? false,
      ...options,
    };

    if (!isLoggedIn) {
      // Guest wishlist
      if (!guestSessionId) {
        toast.error('Session not initialized. Please refresh the page.');
        return false;
      }

      try {
        const item = await addToGuestWishlistApi(guestSessionId, request);
        if (item) {
          const updatedWishlist = [...wishlist, item];
          setWishlist(updatedWishlist);
          localStorage.setItem(GUEST_WISHLIST_KEY, JSON.stringify(updatedWishlist));
          toast.success('Added to wishlist');
          return true;
        }
        return false;
      } catch (error) {
        handleApiError(error, 'Add to wishlist');
        return false;
      }
    }

    // User wishlist
    if (!user) return false;

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}?userId=${user.id}`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const errorData = await parseApiResponse(response).catch(() => null);
        throw new Error(errorData?.message || 'Failed to add to wishlist');
      }

      const data = await parseApiResponse(response);
      if (data?.data) {
        setWishlist(prev => [...prev, data.data]);
        toast.success('Added to wishlist');
        return true;
      }
      return false;
    } catch (error) {
      handleApiError(error, 'Add to wishlist');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [wishlist, isLoggedIn, guestSessionId, user, API_BASE, getHeaders, parseApiResponse, handleApiError]);

  // ==================== Remove from Wishlist ====================

  const removeFromWishlist = useCallback(async (productId: number): Promise<boolean> => {
    if (!isLoggedIn) {
      // Guest wishlist
      if (!guestSessionId) return false;

      try {
        const success = await removeFromGuestWishlistApi(guestSessionId, productId);
        if (success) {
          const updatedWishlist = wishlist.filter(item => item.product.id !== productId);
          setWishlist(updatedWishlist);
          localStorage.setItem(GUEST_WISHLIST_KEY, JSON.stringify(updatedWishlist));
          toast.success('Removed from wishlist');
          return true;
        }
        return false;
      } catch (error) {
        handleApiError(error, 'Remove from wishlist');
        return false;
      }
    }

    // User wishlist
    if (!user) return false;

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/${productId}?userId=${user.id}`, {
        method: 'DELETE',
        headers: getHeaders(),
      });

      if (!response.ok) {
        throw new Error('Failed to remove from wishlist');
      }

      setWishlist(prev => prev.filter(item => item.product.id !== productId));
      toast.success('Removed from wishlist');
      return true;
    } catch (error) {
      handleApiError(error, 'Remove from wishlist');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, guestSessionId, user, wishlist, API_BASE, getHeaders, handleApiError]);

  // ==================== Update Wishlist Item ====================

  const updateWishlistItem = useCallback(async (
    productId: number,
    updates: UpdateWishlistItemRequest
  ): Promise<boolean> => {
    if (!isLoggedIn || !user) {
      // For guest, update locally
      const updatedWishlist = wishlist.map(item => 
        item.product.id === productId ? { ...item, ...updates } : item
      );
      setWishlist(updatedWishlist);
      localStorage.setItem(GUEST_WISHLIST_KEY, JSON.stringify(updatedWishlist));
      toast.success('Updated wishlist item');
      return true;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/${productId}?userId=${user.id}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(updates),
      });

      if (!response.ok) {
        throw new Error('Failed to update wishlist item');
      }

      const data = await parseApiResponse(response);
      if (data?.data) {
        setWishlist(prev => prev.map(item => 
          item.product.id === productId ? data.data : item
        ));
        toast.success('Updated wishlist item');
        return true;
      }
      return false;
    } catch (error) {
      handleApiError(error, 'Update wishlist item');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, wishlist, API_BASE, getHeaders, parseApiResponse, handleApiError]);

  // ==================== Clear Wishlist ====================

  const clearWishlist = useCallback(async (): Promise<boolean> => {
    if (!isLoggedIn) {
      // Guest wishlist
      if (!guestSessionId) return false;

      try {
        const success = await clearGuestWishlistApi(guestSessionId);
        if (success) {
          setWishlist([]);
          localStorage.removeItem(GUEST_WISHLIST_KEY);
          toast.success('Wishlist cleared');
          return true;
        }
        return false;
      } catch (error) {
        handleApiError(error, 'Clear wishlist');
        return false;
      }
    }

    // User wishlist
    if (!user) return false;

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/clear?userId=${user.id}`, {
        method: 'DELETE',
        headers: getHeaders(),
      });

      if (!response.ok) {
        throw new Error('Failed to clear wishlist');
      }

      setWishlist([]);
      toast.success('Wishlist cleared');
      return true;
    } catch (error) {
      handleApiError(error, 'Clear wishlist');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, guestSessionId, user, API_BASE, getHeaders, handleApiError]);

  // ==================== Move to Cart ====================

  const moveToCart = useCallback(async (productId: number, quantity = 1) => {
    const item = wishlist.find(i => i.product.id === productId);
    if (!item) {
      toast.error('Item not found in wishlist');
      return;
    }

    try {
      await addToCart(item.product, quantity);
      await removeFromWishlist(productId);
    } catch (error) {
      handleApiError(error, 'Move to cart');
    }
  }, [wishlist, addToCart, removeFromWishlist, handleApiError]);

  const moveMultipleToCart = useCallback(async (productIds: number[]) => {
    if (!isLoggedIn || !user) {
      // Move one by one for guests
      for (const id of productIds) {
        await moveToCart(id);
      }
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/move-to-cart/multiple?userId=${user.id}`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(productIds),
      });

      if (!response.ok) {
        throw new Error('Failed to move items to cart');
      }

      // Add items to cart and remove from wishlist
      for (const id of productIds) {
        const item = wishlist.find(i => i.product.id === id);
        if (item) {
          await addToCart(item.product, item.desiredQuantity);
        }
      }

      setWishlist(prev => prev.filter(item => !productIds.includes(item.product.id)));
      toast.success(`Moved ${productIds.length} items to cart`);
    } catch (error) {
      handleApiError(error, 'Move items to cart');
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, wishlist, API_BASE, getHeaders, addToCart, handleApiError]);

  const bulkAddToCart = useCallback(async (productIds?: number[]) => {
    const items = productIds 
      ? wishlist.filter(item => productIds.includes(item.product.id))
      : wishlist;

    if (items.length === 0) {
      toast.error('No items to add to cart');
      return;
    }

    for (const item of items) {
      await addToCart(item.product, item.desiredQuantity);
    }

    toast.success(`Added ${items.length} items to cart`);
  }, [wishlist, addToCart]);

  // ==================== Mark as Purchased ====================

  const markAsPurchased = useCallback(async (productId: number) => {
    if (!isLoggedIn || !user) {
      toast.error('Please sign in to mark items as purchased');
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/${productId}/purchase?userId=${user.id}`, {
        method: 'PATCH',
        headers: getHeaders(),
      });

      if (!response.ok) {
        throw new Error('Failed to mark as purchased');
      }

      const data = await parseApiResponse(response);
      if (data?.data) {
        setWishlist(prev => prev.map(item => 
          item.product.id === productId ? data.data : item
        ));
        toast.success('Marked as purchased');
      }
    } catch (error) {
      handleApiError(error, 'Mark as purchased');
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, API_BASE, getHeaders, parseApiResponse, handleApiError]);

  const markMultipleAsPurchased = useCallback(async (productIds: number[]) => {
    if (!isLoggedIn || !user) {
      toast.error('Please sign in to mark items as purchased');
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/purchase/multiple?userId=${user.id}`, {
        method: 'PATCH',
        headers: getHeaders(),
        body: JSON.stringify(productIds),
      });

      if (!response.ok) {
        throw new Error('Failed to mark items as purchased');
      }

      // Update local state
      setWishlist(prev => prev.map(item => 
        productIds.includes(item.product.id) 
          ? { ...item, purchased: true, purchasedAt: new Date().toISOString() }
          : item
      ));

      toast.success(`Marked ${productIds.length} items as purchased`);
    } catch (error) {
      handleApiError(error, 'Mark items as purchased');
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, API_BASE, getHeaders, handleApiError]);

  // ==================== Collections & Organization ====================

  const getItemsByCollection = useCallback((collectionName: string) => {
    return wishlist.filter(item => item.collectionName === collectionName);
  }, [wishlist]);

  const getItemsByPriority = useCallback((priority: WishlistPriority) => {
    return wishlist.filter(item => item.priority === priority);
  }, [wishlist]);

  const getItemsByTags = useCallback((tags: string[]) => {
    return wishlist.filter(item => 
      item.tags?.some(tag => tags.includes(tag))
    );
  }, [wishlist]);

  const moveToCollection = useCallback(async (productIds: number[], collectionName: string) => {
    if (!isLoggedIn || !user) {
      // Update locally for guests
      const updatedWishlist = wishlist.map(item =>
        productIds.includes(item.product.id) ? { ...item, collectionName } : item
      );
      setWishlist(updatedWishlist);
      localStorage.setItem(GUEST_WISHLIST_KEY, JSON.stringify(updatedWishlist));
      toast.success('Moved to collection');
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(
        `${API_BASE}/collections/move?userId=${user.id}&collectionName=${encodeURIComponent(collectionName)}`,
        {
          method: 'PUT',
          headers: getHeaders(),
          body: JSON.stringify({ productIds }),
        }
      );

      if (!response.ok) {
        throw new Error('Failed to move to collection');
      }

      setWishlist(prev => prev.map(item =>
        productIds.includes(item.product.id) ? { ...item, collectionName } : item
      ));
      toast.success('Moved to collection');
    } catch (error) {
      handleApiError(error, 'Move to collection');
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, wishlist, API_BASE, getHeaders, handleApiError]);

  const loadCollections = useCallback(async () => {
    if (!isLoggedIn || !user) {
      // Extract collections from local wishlist
      const localCollections = Array.from(
        new Set(wishlist.map(item => item.collectionName).filter(Boolean) as string[])
      );
      setCollections(localCollections);
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/collections?userId=${user.id}`, {
        headers: getHeaders(),
      });

      if (response.ok) {
        const data = await parseApiResponse(response);
        setCollections(data?.data || []);
      }
    } catch (error) {
      console.error('[Wishlist] Failed to load collections:', error);
    }
  }, [isLoggedIn, user, wishlist, API_BASE, getHeaders, parseApiResponse]);

  // ==================== Price Tracking ====================

  const getItemsWithPriceDrops = useCallback(() => {
    return wishlist.filter(item => item.isPriceDropped);
  }, [wishlist]);

  const getItemsBelowTargetPrice = useCallback(() => {
    return wishlist.filter(item => 
      item.targetPrice && item.currentPrice <= item.targetPrice
    );
  }, [wishlist]);

  // ==================== Purchase Status ====================

  const getPurchasedItems = useCallback(() => {
    return wishlist.filter(item => item.purchased);
  }, [wishlist]);

  const getUnpurchasedItems = useCallback(() => {
    return wishlist.filter(item => !item.purchased);
  }, [wishlist]);

  const getAvailableItems = useCallback(() => {
    return wishlist.filter(item => item.inStock && !item.purchased);
  }, [wishlist]);

  // ==================== Bulk Operations ====================

  const addMultipleToWishlist = useCallback(async (
    products: Product[],
    options: Partial<AddToWishlistRequest> = {}
  ) => {
    const promises = products.map(product => 
      addToWishlist(product.id, options)
    );
    await Promise.all(promises);
  }, [addToWishlist]);

  const removeMultipleFromWishlist = useCallback(async (productIds: number[]) => {
    if (!isLoggedIn || !user) {
      // Remove locally for guests
      const updatedWishlist = wishlist.filter(item => !productIds.includes(item.product.id));
      setWishlist(updatedWishlist);
      localStorage.setItem(GUEST_WISHLIST_KEY, JSON.stringify(updatedWishlist));
      toast.success(`Removed ${productIds.length} items`);
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(`${API_BASE}/bulk/remove?userId=${user.id}`, {
        method: 'DELETE',
        headers: getHeaders(),
        body: JSON.stringify(productIds),
      });

      if (!response.ok) {
        throw new Error('Failed to remove items');
      }

      setWishlist(prev => prev.filter(item => !productIds.includes(item.product.id)));
      toast.success(`Removed ${productIds.length} items`);
    } catch (error) {
      handleApiError(error, 'Remove items');
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, wishlist, API_BASE, getHeaders, handleApiError]);

  // ==================== Summary & Analytics ====================

  const loadSummary = useCallback(async () => {
    if (!isLoggedIn || !user) {
      // Calculate summary locally for guests
      const localSummary: WishlistSummary = {
        userId: 0,
        totalItems: wishlist.length,
        inStockItems: wishlist.filter(i => i.inStock).length,
        outOfStockItems: wishlist.filter(i => !i.inStock).length,
        itemsWithPriceDrops: wishlist.filter(i => i.isPriceDropped).length,
        purchasedItems: wishlist.filter(i => i.purchased).length,
        totalValue: wishlist.reduce((sum, i) => sum + i.currentPrice * i.desiredQuantity, 0),
        totalSavings: wishlist.reduce((sum, i) => sum + i.priceDifference, 0),
        items: wishlist,
      };
      setSummary(localSummary);
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/summary?userId=${user.id}`, {
        headers: getHeaders(),
      });

      if (response.ok) {
        const data = await parseApiResponse(response);
        setSummary(data?.data || null);
      }
    } catch (error) {
      console.error('[Wishlist] Failed to load summary:', error);
    }
  }, [isLoggedIn, user, wishlist, API_BASE, getHeaders, parseApiResponse]);

  const loadAnalytics = useCallback(async () => {
    if (!isLoggedIn || !user) {
      setAnalytics(null);
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/analytics?userId=${user.id}`, {
        headers: getHeaders(),
      });

      if (response.ok) {
        const data = await parseApiResponse(response);
        setAnalytics(data?.data || null);
      }
    } catch (error) {
      console.error('[Wishlist] Failed to load analytics:', error);
      setAnalytics(null);
    }
  }, [isLoggedIn, user, API_BASE, getHeaders, parseApiResponse]);

  // ==================== Optimization ====================

  const optimizeWishlist = useCallback(async (options: WishlistOptimizationRequest): Promise<WishlistItem[]> => {
    if (!isLoggedIn || !user) {
      // Client-side optimization for guests
      let items = [...wishlist];

      if (options.includeOnlyInStock) {
        items = items.filter(i => i.inStock);
      }

      // Sort by strategy
      switch (options.optimizationStrategy) {
        case 'PRIORITY':
          items.sort((a, b) => {
            const priorityOrder = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
            return priorityOrder[b.priority] - priorityOrder[a.priority];
          });
          break;
        case 'PRICE':
          items.sort((a, b) => a.currentPrice - b.currentPrice);
          break;
        case 'SAVINGS':
          items.sort((a, b) => b.priceDifference - a.priceDifference);
          break;
        default:
          items.sort((a, b) => {
            if (a.priority !== b.priority) {
              const priorityOrder = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
              return priorityOrder[b.priority] - priorityOrder[a.priority];
            }
            return a.currentPrice - b.currentPrice;
          });
      }

      // Apply budget constraint
      if (options.maxBudget) {
        let total = 0;
        items = items.filter(item => {
          const cost = item.currentPrice * item.desiredQuantity;
          if (total + cost <= options.maxBudget!) {
            total += cost;
            return true;
          }
          return false;
        });
      }

      // Apply max items
      if (options.maxItems) {
        items = items.slice(0, options.maxItems);
      }

      return items;
    }

    try {
      const response = await fetch(`${API_BASE}/optimize?userId=${user.id}`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(options),
      });

      if (!response.ok) {
        throw new Error('Failed to optimize wishlist');
      }

      const data = await parseApiResponse(response);
      return data?.data || [];
    } catch (error) {
      handleApiError(error, 'Optimize wishlist');
      return [];
    }
  }, [isLoggedIn, user, wishlist, API_BASE, getHeaders, parseApiResponse, handleApiError]);

  // ==================== Sharing ====================

  const shareWishlist = useCallback(async (options?: { shareName?: string; description?: string }) => {
    if (!isLoggedIn || !user) {
      throw new Error('Sharing is only available for authenticated users');
    }

    try {
      const response = await fetch(`${API_BASE}/share?userId=${user.id}`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(options || {}),
      });

      if (!response.ok) {
        throw new Error('Failed to share wishlist');
      }

      const data = await parseApiResponse(response);
      return {
        shareUrl: data.data.shareUrl,
        shareToken: data.data.shareToken,
      };
    } catch (error) {
      handleApiError(error, 'Share wishlist');
      throw error;
    }
  }, [isLoggedIn, user, API_BASE, getHeaders, parseApiResponse, handleApiError]);

  // ==================== Guest to User Migration ====================

  const mergeGuestWishlist = useCallback(async () => {
    if (!isLoggedIn || !user || !guestSessionId) return;

    const guestItems = localStorage.getItem(GUEST_WISHLIST_KEY);
    if (!guestItems || guestItems === '[]') return;

    setIsLoading(true);
    try {
      const response = await fetch(
        `${API_BASE}/guest/merge?guestSessionId=${guestSessionId}&userId=${user.id}`,
        {
          method: 'POST',
          headers: getHeaders(),
        }
      );

      if (!response.ok) {
        throw new Error('Failed to merge guest wishlist');
      }

      // Clear guest data
      localStorage.removeItem(GUEST_WISHLIST_KEY);
      localStorage.removeItem(GUEST_SESSION_KEY);
      setGuestSessionId(null);

      // Reload user wishlist
      await loadWishlist();
      
      toast.success('Guest wishlist merged successfully');
    } catch (error) {
      handleApiError(error, 'Merge guest wishlist');
    } finally {
      setIsLoading(false);
    }
  }, [isLoggedIn, user, guestSessionId, API_BASE, getHeaders, loadWishlist, handleApiError]);

  // Auto-merge on login
  useEffect(() => {
    if (isLoggedIn && user && guestSessionId) {
      const hasGuestItems = localStorage.getItem(GUEST_WISHLIST_KEY);
      if (hasGuestItems && hasGuestItems !== '[]') {
        mergeGuestWishlist();
      }
    }
  }, [isLoggedIn, user, guestSessionId, mergeGuestWishlist]);

  // ==================== Helper Check ====================

  const isInWishlist = useCallback((productId: number) => {
    return wishlist.some(item => item.product.id === productId);
  }, [wishlist]);

  // ==================== Memoized Value ====================

  const value = useMemo<WishlistContextType>(() => ({
    // State
    wishlist,
    items: wishlist,
    itemCount: wishlist.length,
    summary,
    analytics,
    collections,
    isLoading,
    guestSessionId,
    
    // Basic Operations
    addToWishlist,
    removeFromWishlist,
    updateWishlistItem,
    isInWishlist,
    clearWishlist,
    loadWishlist,
    
    // Advanced Operations
    moveToCart,
    moveMultipleToCart,
    bulkAddToCart,
    markAsPurchased,
    markMultipleAsPurchased,
    
    // Collections & Organization
    getItemsByCollection,
    getItemsByPriority,
    getItemsByTags,
    moveToCollection,
    loadCollections,
    
    // Price Tracking
    getItemsWithPriceDrops,
    getItemsBelowTargetPrice,
    
    // Purchase Status
    getPurchasedItems,
    getUnpurchasedItems,
    getAvailableItems,
    
    // Bulk Operations
    addMultipleToWishlist,
    removeMultipleFromWishlist,
    
    // Summary & Analytics
    loadSummary,
    loadAnalytics,
    
    // Optimization
    optimizeWishlist,
    
    // Sharing
    shareWishlist,
    
    // Guest Operations
    mergeGuestWishlist,
    initGuestSession,
  }), [
    wishlist,
    summary,
    analytics,
    collections,
    isLoading,
    guestSessionId,
    addToWishlist,
    removeFromWishlist,
    updateWishlistItem,
    isInWishlist,
    clearWishlist,
    loadWishlist,
    moveToCart,
    moveMultipleToCart,
    bulkAddToCart,
    markAsPurchased,
    markMultipleAsPurchased,
    getItemsByCollection,
    getItemsByPriority,
    getItemsByTags,
    moveToCollection,
    loadCollections,
    getItemsWithPriceDrops,
    getItemsBelowTargetPrice,
    getPurchasedItems,
    getUnpurchasedItems,
    getAvailableItems,
    addMultipleToWishlist,
    removeMultipleFromWishlist,
    loadSummary,
    loadAnalytics,
    optimizeWishlist,
    shareWishlist,
    mergeGuestWishlist,
    initGuestSession,
  ]);

  return (
    <WishlistContext.Provider value={value}>
      {children}
    </WishlistContext.Provider>
  );
};

// ==================== Hook ====================

export const useWishlist = () => {
  const context = useContext(WishlistContext);
  if (!context) {
    throw new Error('useWishlist must be used within WishlistProvider');
  }
  return context;
};