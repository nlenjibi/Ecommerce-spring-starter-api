/**
 * Wishlist API Integration Layer
 * Provides type-safe, error-handled API calls for all wishlist operations
 * Matches API specification: /v1/wishlist/*
 */

import { API_BASE_URL, STORAGE_KEYS, TOAST_MESSAGES } from './constants';
import { generateUUID } from './utils';

// ==================== TypeScript Types ====================

export type WishlistPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface Product {
  id: number;
  name: string;
  slug: string;
  sku: string;
  price: number;
  discountPrice?: number;
  imageUrl: string;
  categoryName: string;
  inStock: boolean;
  availableQuantity: number;
  inventoryStatus: string;
}

export interface WishlistItem {
  id: number;
  userId: number;
  product: Product;
  notes?: string;
  priority: WishlistPriority;
  desiredQuantity: number;
  priceWhenAdded: number;
  currentPrice: number;
  priceDifference: number;
  isPriceDropped: boolean;
  targetPrice?: number;
  notifyOnPriceDrop: boolean;
  notifyOnStock: boolean;
  shouldNotifyPriceDrop: boolean;
  shouldNotifyStock: boolean;
  purchased: boolean;
  isPublic: boolean;
  inStock: boolean;
  addedAt: string;
  purchasedAt?: string;
}

export interface AddToWishlistRequest {
  productId: number;
  notes?: string;
  priority?: WishlistPriority;
  desiredQuantity?: number;
  targetPrice?: number;
  notifyOnPriceDrop?: boolean;
  notifyOnStock?: boolean;
  isPublic?: boolean;
}

export interface UpdateWishlistItemRequest {
  notes?: string;
  priority?: WishlistPriority;
  desiredQuantity?: number;
  targetPrice?: number;
  notifyOnPriceDrop?: boolean;
  notifyOnStock?: boolean;
  isPublic?: boolean;
}

export interface WishlistSummary {
  userId: number;
  totalItems: number;
  inStockItems: number;
  outOfStockItems: number;
  itemsWithPriceDrops: number;
  purchasedItems: number;
  totalValue: number;
  totalSavings: number;
  items: WishlistItem[];
}

export interface WishlistAnalytics {
  userId: number;
  totalItems: number;
  itemsAddedThisMonth: number;
  itemsPurchased: number;
  itemsWithPriceDrops: number;
  averagePriceDrop: number;
  totalSavings: number;
  mostAddedCategory: string;
  highestPriorityCategory: string;
  averageDaysInWishlist: number;
  categoryBreakdown: Array<{
    categoryName: string;
    itemCount: number;
    totalValue: number;
    averagePrice: number;
  }>;
}

export interface PriceHistoryItem {
  productId: number;
  productName: string;
  timestamp: string;
  price: number;
  discountPrice?: number;
  percentageChange?: number;
}

export interface GuestSessionResponse {
  sessionId: string;
  createdAt: string;
  expiresAt: string;
  itemCount: number;
}

export interface ShareWishlistRequest {
  shareName?: string;
  description?: string;
  allowPurchaseTracking?: boolean;
  showPrices?: boolean;
  expiresAt?: string;
  productIds?: number[];
  password?: string;
}

export interface ShareWishlistResponse {
  shareToken: string;
  shareUrl: string;
  shareName: string;
  description?: string;
  createdAt: string;
  expiresAt?: string;
  itemCount: number;
  isActive: boolean;
  passwordProtected: boolean;
}

export interface OptimizeWishlistRequest {
  maxBudget?: number;
  priorityOrder?: string[];
  includeOnlyInStock?: boolean;
  maxItems?: number;
  optimizationStrategy?: 'BUDGET' | 'PRIORITY' | 'SAVINGS' | 'AVAILABILITY';
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errors?: Record<string, string>;
  timestamp: string;
  path: string;
  statusCode: number;
}

export interface ApiError {
  status: string;
  message: string;
  statusCode: number;
  timestamp: string;
  path: string;
  errors?: Record<string, string>;
  traceId?: string;
}

// ==================== API Configuration ====================

const API_BASE = `${API_BASE_URL}/v1/wishlist`;

// ==================== Helper Functions ====================

/**
 * Get API request headers with optional authorization
 */
const getApiHeaders = (token: string | null = null): HeadersInit => {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
};

/**
 * Parse and validate API response
 */
const parseResponse = async <T>(response: Response): Promise<T> => {
  const contentType = response.headers.get('content-type') || '';

  if (!contentType.includes('application/json')) {
    const text = await response.text();
    throw new Error(`Expected JSON response, got ${contentType}: ${text.substring(0, 100)}`);
  }

  const data: ApiResponse<T> = await response.json();

  if (!response.ok) {
    throw new Error(data.message || `HTTP ${response.status}: ${response.statusText}`);
  }

  return data.data as T;
};

/**
 * Handle API errors with logging
 */
const handleApiError = (error: any, operation: string): never => {
  const errorMessage = error instanceof Error ? error.message : 'Unknown error';
  console.error(`[Wishlist API] ${operation} failed:`, error);
  throw new Error(`${operation} failed: ${errorMessage}`);
};

// ==================== Authenticated User API ====================

/**
 * Get user's wishlist
 */
export const getUserWishlist = async (
  userId: number,
  token: string | null
): Promise<WishlistItem[]> => {
  try {
    const response = await fetch(`${API_BASE}?userId=${userId}`, {
      method: 'GET',
      headers: getApiHeaders(token),
    });

    return await parseResponse<WishlistItem[]>(response);
  } catch (error) {
    handleApiError(error, 'Get wishlist');
  }
};

/**
 * Add product to wishlist
 */
export const addToWishlist = async (
  userId: number,
  token: string | null,
  request: AddToWishlistRequest
): Promise<WishlistItem> => {
  try {
    const response = await fetch(`${API_BASE}?userId=${userId}`, {
      method: 'POST',
      headers: getApiHeaders(token),
      body: JSON.stringify(request),
    });

    return await parseResponse<WishlistItem>(response);
  } catch (error) {
    handleApiError(error, 'Add to wishlist');
  }
};

/**
 * Update wishlist item
 */
export const updateWishlistItem = async (
  userId: number,
  productId: number,
  token: string | null,
  updates: UpdateWishlistItemRequest
): Promise<WishlistItem> => {
  try {
    const response = await fetch(`${API_BASE}/${productId}?userId=${userId}`, {
      method: 'PUT',
      headers: getApiHeaders(token),
      body: JSON.stringify(updates),
    });

    return await parseResponse<WishlistItem>(response);
  } catch (error) {
    handleApiError(error, 'Update wishlist item');
  }
};

/**
 * Remove product from wishlist
 */
export const removeFromWishlist = async (
  userId: number,
  productId: number,
  token: string | null
): Promise<void> => {
  try {
    const response = await fetch(`${API_BASE}/${productId}?userId=${userId}`, {
      method: 'DELETE',
      headers: getApiHeaders(token),
    });

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Remove from wishlist');
  }
};

/**
 * Clear entire wishlist
 */
export const clearWishlist = async (
  userId: number,
  token: string | null
): Promise<void> => {
  try {
    const response = await fetch(`${API_BASE}/clear?userId=${userId}`, {
      method: 'DELETE',
      headers: getApiHeaders(token),
    });

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Clear wishlist');
  }
};

/**
 * Get wishlist summary with statistics
 */
export const getWishlistSummary = async (
  userId: number,
  token: string | null
): Promise<WishlistSummary> => {
  try {
    const response = await fetch(`${API_BASE}/summary?userId=${userId}`, {
      method: 'GET',
      headers: getApiHeaders(token),
    });

    return await parseResponse<WishlistSummary>(response);
  } catch (error) {
    handleApiError(error, 'Get wishlist summary');
  }
};

/**
 * Get wishlist analytics
 */
export const getWishlistAnalytics = async (
  userId: number,
  token: string | null
): Promise<WishlistAnalytics> => {
  try {
    const response = await fetch(`${API_BASE}/analytics?userId=${userId}`, {
      method: 'GET',
      headers: getApiHeaders(token),
    });

    return await parseResponse<WishlistAnalytics>(response);
  } catch (error) {
    handleApiError(error, 'Get wishlist analytics');
  }
};

/**
 * Get price history for a product
 */
export const getPriceHistory = async (
  userId: number,
  productId: number,
  token: string | null
): Promise<PriceHistoryItem[]> => {
  try {
    const response = await fetch(
      `${API_BASE}/${productId}/price-history?userId=${userId}`,
      {
        method: 'GET',
        headers: getApiHeaders(token),
      }
    );

    return await parseResponse<PriceHistoryItem[]>(response);
  } catch (error) {
    handleApiError(error, 'Get price history');
  }
};

/**
 * Move item to cart
 */
export const moveToCart = async (
  userId: number,
  productId: number,
  token: string | null
): Promise<void> => {
  try {
    const response = await fetch(
      `${API_BASE}/${productId}/move-to-cart?userId=${userId}`,
      {
        method: 'POST',
        headers: getApiHeaders(token),
      }
    );

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Move to cart');
  }
};

/**
 * Move multiple items to cart
 */
export const moveMultipleToCart = async (
  userId: number,
  productIds: number[],
  token: string | null
): Promise<void> => {
  try {
    const response = await fetch(`${API_BASE}/move-to-cart/multiple?userId=${userId}`, {
      method: 'POST',
      headers: getApiHeaders(token),
      body: JSON.stringify(productIds),
    });

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Move multiple to cart');
  }
};

/**
 * Mark item as purchased
 */
export const markAsPurchased = async (
  userId: number,
  productId: number,
  token: string | null
): Promise<WishlistItem> => {
  try {
    const response = await fetch(`${API_BASE}/${productId}/purchase?userId=${userId}`, {
      method: 'PATCH',
      headers: getApiHeaders(token),
    });

    return await parseResponse<WishlistItem>(response);
  } catch (error) {
    handleApiError(error, 'Mark as purchased');
  }
};

/**
 * Mark multiple items as purchased
 */
export const markMultipleAsPurchased = async (
  userId: number,
  productIds: number[],
  token: string | null
): Promise<void> => {
  try {
    const response = await fetch(`${API_BASE}/purchase/multiple?userId=${userId}`, {
      method: 'PATCH',
      headers: getApiHeaders(token),
      body: JSON.stringify(productIds),
    });

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Mark multiple as purchased');
  }
};

/**
 * Share wishlist
 */
export const shareWishlist = async (
  userId: number,
  token: string | null,
  request: ShareWishlistRequest
): Promise<ShareWishlistResponse> => {
  try {
    const response = await fetch(`${API_BASE}/share?userId=${userId}`, {
      method: 'POST',
      headers: getApiHeaders(token),
      body: JSON.stringify(request),
    });

    return await parseResponse<ShareWishlistResponse>(response);
  } catch (error) {
    handleApiError(error, 'Share wishlist');
  }
};

/**
 * Optimize wishlist based on criteria
 */
export const optimizeWishlist = async (
  userId: number,
  token: string | null,
  request: OptimizeWishlistRequest
): Promise<WishlistItem[]> => {
  try {
    const response = await fetch(`${API_BASE}/optimize?userId=${userId}`, {
      method: 'POST',
      headers: getApiHeaders(token),
      body: JSON.stringify(request),
    });

    return await parseResponse<WishlistItem[]>(response);
  } catch (error) {
    handleApiError(error, 'Optimize wishlist');
  }
};

/**
 * Get wishlist collections
 */
export const getWishlistCollections = async (
  userId: number,
  token: string | null
): Promise<string[]> => {
  try {
    const response = await fetch(`${API_BASE}/collections?userId=${userId}`, {
      method: 'GET',
      headers: getApiHeaders(token),
    });

    return await parseResponse<string[]>(response);
  } catch (error) {
    handleApiError(error, 'Get collections');
  }
};

/**
 * Get items by collection
 */
export const getItemsByCollection = async (
  userId: number,
  collectionName: string,
  token: string | null
): Promise<WishlistItem[]> => {
  try {
    const response = await fetch(
      `${API_BASE}/collection/${encodeURIComponent(collectionName)}?userId=${userId}`,
      {
        method: 'GET',
        headers: getApiHeaders(token),
      }
    );

    return await parseResponse<WishlistItem[]>(response);
  } catch (error) {
    handleApiError(error, 'Get items by collection');
  }
};

/**
 * Move items to collection
 */
export const moveToCollection = async (
  userId: number,
  productIds: number[],
  collectionName: string,
  token: string | null
): Promise<void> => {
  try {
    const response = await fetch(
      `${API_BASE}/collections/move?userId=${userId}&collectionName=${encodeURIComponent(collectionName)}&productIds=${productIds.join(',')}`,
      {
        method: 'PUT',
        headers: getApiHeaders(token),
      }
    );

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Move to collection');
  }
};

// ==================== Guest Wishlist API ====================

/**
 * Generate guest session ID
 */
export const generateGuestSession = async (): Promise<GuestSessionResponse> => {
  try {
    const response = await fetch(`${API_BASE}/guest/session`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
    });

    const sessionData = await parseResponse<GuestSessionResponse>(response);
    
    // Save to localStorage
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.GUEST_SESSION_ID, sessionData.sessionId);
    }

    return sessionData;
  } catch (error) {
    console.error('[Wishlist API] Guest session generation failed, using fallback:', error);
    
    // Fallback to local UUID if API fails
    const fallbackId = generateUUID();
    const fallbackData: GuestSessionResponse = {
      sessionId: fallbackId,
      createdAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
      itemCount: 0,
    };

    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEYS.GUEST_SESSION_ID, fallbackId);
    }

    return fallbackData;
  }
};

/**
 * Get guest wishlist
 */
export const getGuestWishlist = async (
  guestSessionId: string
): Promise<WishlistItem[]> => {
  try {
    const response = await fetch(
      `${API_BASE}/guest?guestSessionId=${encodeURIComponent(guestSessionId)}`,
      {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    return await parseResponse<WishlistItem[]>(response);
  } catch (error) {
    console.error('[Wishlist API] Get guest wishlist failed:', error);
    return [];
  }
};

/**
 * Add to guest wishlist
 */
export const addToGuestWishlist = async (
  guestSessionId: string,
  request: AddToWishlistRequest
): Promise<WishlistItem> => {
  try {
    const response = await fetch(
      `${API_BASE}/guest?guestSessionId=${encodeURIComponent(guestSessionId)}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
      }
    );

    return await parseResponse<WishlistItem>(response);
  } catch (error) {
    handleApiError(error, 'Add to guest wishlist');
  }
};

/**
 * Remove from guest wishlist
 */
export const removeFromGuestWishlist = async (
  guestSessionId: string,
  productId: number
): Promise<void> => {
  try {
    const response = await fetch(
      `${API_BASE}/guest/${productId}?guestSessionId=${encodeURIComponent(guestSessionId)}`,
      {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Remove from guest wishlist');
  }
};

/**
 * Clear guest wishlist
 */
export const clearGuestWishlist = async (guestSessionId: string): Promise<void> => {
  try {
    const response = await fetch(
      `${API_BASE}/guest/clear?guestSessionId=${encodeURIComponent(guestSessionId)}`,
      {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Clear guest wishlist');
  }
};

/**
 * Merge guest wishlist to user account
 */
export const mergeGuestWishlist = async (
  guestSessionId: string,
  userId: number,
  token: string | null
): Promise<void> => {
  try {
    const response = await fetch(
      `${API_BASE}/guest/merge?guestSessionId=${encodeURIComponent(guestSessionId)}&userId=${userId}`,
      {
        method: 'POST',
        headers: getApiHeaders(token),
      }
    );

    await parseResponse<void>(response);

    // Clear guest session after successful merge
    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEYS.GUEST_SESSION_ID);
      localStorage.removeItem(STORAGE_KEYS.GUEST_WISHLIST);
    }
  } catch (error) {
    handleApiError(error, 'Merge guest wishlist');
  }
};

/**
 * Get guest wishlist count
 */
export const getGuestWishlistCount = async (
  guestSessionId: string
): Promise<number> => {
  try {
    const response = await fetch(
      `${API_BASE}/guest/count?guestSessionId=${encodeURIComponent(guestSessionId)}`,
      {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      }
    );

    return await parseResponse<number>(response);
  } catch (error) {
    console.error('[Wishlist API] Get guest count failed:', error);
    return 0;
  }
};

// ==================== Bulk Operations ====================

/**
 * Add multiple products to wishlist
 */
export const bulkAddToWishlist = async (
  userId: number,
  token: string | null,
  requests: AddToWishlistRequest[]
): Promise<{ successful: number; failed: number; errors: string[] }> => {
  try {
    const response = await fetch(`${API_BASE}/bulk/add?userId=${userId}`, {
      method: 'POST',
      headers: getApiHeaders(token),
      body: JSON.stringify(requests),
    });

    return await parseResponse<{
      successful: number;
      failed: number;
      errors: string[];
    }>(response);
  } catch (error) {
    handleApiError(error, 'Bulk add to wishlist');
  }
};

/**
 * Remove multiple products from wishlist
 */
export const bulkRemoveFromWishlist = async (
  userId: number,
  token: string | null,
  productIds: number[]
): Promise<void> => {
  try {
    const response = await fetch(`${API_BASE}/bulk/remove?userId=${userId}`, {
      method: 'DELETE',
      headers: getApiHeaders(token),
      body: JSON.stringify(productIds),
    });

    await parseResponse<void>(response);
  } catch (error) {
    handleApiError(error, 'Bulk remove from wishlist');
  }
};

// ==================== Utility Functions ====================

/**
 * Check if product is in wishlist
 */
export const isInWishlist = (
  wishlist: WishlistItem[],
  productId: number
): boolean => {
  return wishlist.some(item => item.product.id === productId);
};

/**
 * Get wishlist item by product ID
 */
export const getWishlistItem = (
  wishlist: WishlistItem[],
  productId: number
): WishlistItem | undefined => {
  return wishlist.find(item => item.product.id === productId);
};

/**
 * Calculate total wishlist value
 */
export const calculateWishlistValue = (wishlist: WishlistItem[]): number => {
  return wishlist.reduce(
    (total, item) => total + item.currentPrice * item.desiredQuantity,
    0
  );
};

/**
 * Calculate total savings
 */
export const calculateTotalSavings = (wishlist: WishlistItem[]): number => {
  return wishlist.reduce((total, item) => total + item.priceDifference, 0);
};

/**
 * Filter wishlist items
 */
export const filterWishlistItems = (
  wishlist: WishlistItem[],
  filters: {
    priority?: WishlistPriority;
    inStock?: boolean;
    priceDropped?: boolean;
    purchased?: boolean;
    collection?: string;
    minPrice?: number;
    maxPrice?: number;
  }
): WishlistItem[] => {
  return wishlist.filter(item => {
    if (filters.priority && item.priority !== filters.priority) return false;
    if (filters.inStock !== undefined && item.inStock !== filters.inStock) return false;
    if (filters.priceDropped !== undefined && item.isPriceDropped !== filters.priceDropped)
      return false;
    if (filters.purchased !== undefined && item.purchased !== filters.purchased) return false;
    if (filters.collection && item.collectionName !== filters.collection) return false;
    if (filters.minPrice !== undefined && item.currentPrice < filters.minPrice) return false;
    if (filters.maxPrice !== undefined && item.currentPrice > filters.maxPrice) return false;
    return true;
  });
};

/**
 * Sort wishlist items
 */
export const sortWishlistItems = (
  wishlist: WishlistItem[],
  sortBy:
    | 'date-added'
    | 'price-low'
    | 'price-high'
    | 'priority'
    | 'savings'
    | 'name'
): WishlistItem[] => {
  const sorted = [...wishlist];

  switch (sortBy) {
    case 'date-added':
      return sorted.sort(
        (a, b) => new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime()
      );
    case 'price-low':
      return sorted.sort((a, b) => a.currentPrice - b.currentPrice);
    case 'price-high':
      return sorted.sort((a, b) => b.currentPrice - a.currentPrice);
    case 'priority':
      const priorityOrder = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
      return sorted.sort(
        (a, b) => priorityOrder[b.priority] - priorityOrder[a.priority]
      );
    case 'savings':
      return sorted.sort((a, b) => b.priceDifference - a.priceDifference);
    case 'name':
      return sorted.sort((a, b) =>
        a.product.name.localeCompare(b.product.name)
      );
    default:
      return sorted;
  }
};