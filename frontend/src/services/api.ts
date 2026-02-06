import { CreateOrderRequest, CreateOrderResponse, Coupon, TaxConfiguration } from '@/types';

// Normalize API base URL to avoid duplicated '/api' when NEXT_PUBLIC_API_URL
// may already include the '/api' suffix. Ensure API_BASE_URL always ends with '/api'.
const _rawApiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9190';
const API_BASE_URL = (() => {
  try {
    let u = _rawApiBase.trim();
    // Remove trailing slash
    if (u.endsWith('/')) u = u.slice(0, -1);
    // If it already ends with '/api', keep it
    if (u.toLowerCase().endsWith('/api')) return u;
    // Otherwise append '/api'
    return `${u}/api`;
  } catch (e) {
    return 'http://localhost:9190/api';
  }
})();

// Token management utilities
const TOKEN_STORAGE_KEY = 'auth_tokens';

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  tokenType: string;
}

const getStoredTokens = (): AuthTokens | null => {
  if (typeof window === 'undefined') return null;
  const stored = localStorage.getItem(TOKEN_STORAGE_KEY);
  return stored ? JSON.parse(stored) : null;
};

const setStoredTokens = (tokens: AuthTokens): void => {
  if (typeof window === 'undefined') return;
  localStorage.setItem(TOKEN_STORAGE_KEY, JSON.stringify(tokens));
};

const clearStoredTokens = (): void => {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TOKEN_STORAGE_KEY);
};

const getAccessToken = (): string | null => {
  const tokens = getStoredTokens();
  return tokens?.accessToken || null;
};

const isTokenExpired = (): boolean => {
  const tokens = getStoredTokens();
  if (!tokens) return true;

  // Check if token is expired (with 5 minute buffer)
  const expiryTime = Date.now() + (tokens.expiresIn * 1000) - (5 * 60 * 1000);
  return Date.now() >= expiryTime;
};

// Refresh token function (defined here to avoid circular dependency)
async function refreshAccessToken(): Promise<void> {
  const tokens = getStoredTokens();
  if (!tokens?.refreshToken) {
    throw new Error('No refresh token available');
  }

  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: tokens.refreshToken }),
  });

  if (!response.ok) {
    throw new Error('Token refresh failed');
  }

  const data = await response.json();
  if (data.accessToken) {
    setStoredTokens({
      ...tokens,
      accessToken: data.accessToken,
      expiresIn: data.expiresIn || tokens.expiresIn,
    });
  }
}

// Generic fetch wrapper with automatic token refresh
async function fetchApi<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  let token = getAccessToken();

  // If token is expired, try to refresh it
  if (token && isTokenExpired()) {
    try {
      await refreshAccessToken();
      token = getAccessToken();
    } catch (error) {
      // Refresh failed, clear tokens and let user login again
      clearStoredTokens();
      token = null;
      throw new Error('Session expired. Please login again.');
    }
  }

  const config: RequestInit = {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'An error occurred' }));

    // Try to extract validation or field errors from common response shapes
    let detailedMessage: string | undefined;

    // If API returns an `errors` object with field messages, flatten them
    if (error && typeof error === 'object') {
      if (error.errors && typeof error.errors === 'object') {
        try {
          const parts: string[] = [];
          for (const key of Object.keys(error.errors)) {
            const val = (error.errors as any)[key];
            if (Array.isArray(val)) {
              parts.push(...val.map(String));
            } else if (typeof val === 'string') {
              parts.push(val);
            } else if (val && typeof val === 'object' && val.message) {
              parts.push(String(val.message));
            }
          }
          if (parts.length) detailedMessage = parts.join('; ');
        } catch (e) {
          // ignore and fallback
        }
      }

      // Some APIs return errors as an array
      if (!detailedMessage && Array.isArray((error as any).errors)) {
        detailedMessage = ((error as any).errors as any[]).map(String).join('; ');
      }

      // Some APIs include a nested `message` inside `error` details
      if (!detailedMessage && (error as any).detail) {
        detailedMessage = String((error as any).detail);
      }
    }

    const finalMessage = detailedMessage || error.message || `HTTP error! status: ${response.status}`;
    throw new Error(finalMessage);
  }

  if (response.status === 204) {
    return {} as T;
  }

  // If the response has no JSON content-type, avoid calling response.json() which would throw
  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    // try to read text and parse if possible, otherwise return empty object
    const text = await response.text().catch(() => '');
    try {
      return (text ? (JSON.parse(text) as T) : ({} as T));
    } catch (e) {
      // Not JSON, return empty object as fallback
      return {} as T;
    }
  }

  return response.json();
}

// Public fetch wrapper (no authentication required)
async function fetchPublic<T>(endpoint: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'An error occurred' }));
    throw new Error(error.message || `HTTP error! status: ${response.status}`);
  }

  // Guard for non-JSON responses for public endpoints as well
  const contentType = response.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) {
    const text = await response.text().catch(() => '');
    try {
      return (text ? (JSON.parse(text) as T) : ({} as T));
    } catch (e) {
      return {} as T;
    }
  }

  return response.json();
}

// ============ PUBLIC APIs (No Auth Required) ============

// Paginated response type
interface PaginatedResponse<T> {
  success: boolean;
  message?: string;
  data: {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
    empty?: boolean;
    first?: boolean;
    last?: boolean;
  };
}

// Public Products API - for landing page and public product browsing
export const productsApi = {
  // Get all products with pagination
  getAll: (params?: { 
    page?: number; 
    size?: number; 
    sortBy?: string; 
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.direction) query.append('direction', params.direction);
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get products by seller
  getBySellerId: (sellerId: number, params?: { page?: number; size?: number }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    query.append('sellerId', sellerId.toString());

    return fetchApi<{
      success: boolean;
      data: {
        content: any[];
        total: number;
        page: number;
        totalPages: number;
      };
    }>(`/v1/products/seller?${query.toString()}`);
  },

  // Search products by name (q parameter)
  search: (params: { 
    q: string; 
    page?: number; 
    size?: number;
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    query.append('q', params.q);
    if (params.page !== undefined) query.append('page', params.page.toString());
    if (params.size) query.append('size', params.size.toString());
    if (params.sortBy) query.append('sortBy', params.sortBy);
    if (params.direction) query.append('direction', params.direction);
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/search?${query.toString()}`
    );
  },

  // Get products by price range
  // Status options: IN_STOCK, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED, PRE_ORDER, BACKORDER
  getByPriceRange: (params: { 
    minPrice: number; 
    maxPrice: number;
    page?: number; 
    size?: number; 
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.append('page', params.page.toString());
    if (params.size) query.append('size', params.size.toString());
    if (params.sortBy) query.append('sortBy', params.sortBy);
    if (params.direction) query.append('direction', params.direction);
    query.append('minPrice', params.minPrice.toString());
    query.append('maxPrice', params.maxPrice.toString());
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/price-range?${query.toString()}`
    );
  },

  // Get products by price range
  getByPriceRange: (params: { 
    minPrice: number; 
    maxPrice: number;
    page?: number; 
    size?: number;
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    query.append('minPrice', params.minPrice.toString());
    query.append('maxPrice', params.maxPrice.toString());
    if (params.page !== undefined) query.append('page', params.page.toString());
    if (params.size) query.append('size', params.size.toString());
    if (params.sortBy) query.append('sortBy', params.sortBy);
    if (params.direction) query.append('direction', params.direction);
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/price-range?${query.toString()}`
    );
  },

  // Get featured products
  getFeatured: (params?: { page?: number; size?: number }) => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/featured${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get products by category ID
  // Uses path parameter: /v1/products/category/{categoryId}
  getByCategory: (categoryId: number, params?: { 
    page?: number; 
    size?: number;
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.direction) query.append('direction', params.direction);
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/category/${categoryId}${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get products by category name (case-insensitive exact match)
  // Uses path parameter: /v1/products/category/name/{categoryName}
  getByCategoryName: (categoryName: string, params?: { 
    page?: number; 
    size?: number;
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.direction) query.append('direction', params.direction);
    const queryString = query.toString();
    const encodedName = encodeURIComponent(categoryName);
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/category/name/${encodedName}${queryString ? `?${queryString}` : ''}`
    );
  },

  // Alternative: Get products by category using query parameter
  // Uses query parameter: /v1/products?categoryId={categoryId}
  getByCategoryQuery: (categoryId: number, params?: { 
    page?: number; 
    size?: number;
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    query.append('categoryId', categoryId.toString());
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.direction) query.append('direction', params.direction);
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products?${query.toString()}`
    );
  },

  // Get products by inventory status
  // Status options: IN_STOCK, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED, PRE_ORDER, BACKORDER
  getByInventoryStatus: (status: string, params?: { 
    page?: number; 
    size?: number;
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.direction) query.append('direction', params.direction);
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/inventory-status/${status}${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get products needing reorder
  getNeedsReorder: (params?: { 
    page?: number; 
    size?: number;
    sortBy?: string;
    direction?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.direction) query.append('direction', params.direction);
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/needs-reorder${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get product by ID
  getById: (id: number) => 
    fetchPublic<{ success: boolean; data: any }>(`/v1/products/${id}`),

  // Get product by slug
  getBySlug: (slug: string) => 
    fetchPublic<{ success: boolean; data: any }>(`/v1/products/slug/${slug}`),

  // Get trending products (for landing page)
  getTrending: (params?: { size?: number }) => {
    const query = new URLSearchParams();
    if (params?.size) query.append('size', params.size.toString());
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/trending${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get new arrivals (for landing page)
  getNewArrivals: (params?: { size?: number }) => {
    const query = new URLSearchParams();
    if (params?.size) query.append('size', params.size.toString());
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/new-arrivals${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get top rated products
  getTopRated: (params?: { size?: number }) => {
    const query = new URLSearchParams();
    if (params?.size) query.append('size', params.size.toString());
    const queryString = query.toString();
    return fetchPublic<PaginatedResponse<any>>(
      `/v1/products/top-rated${queryString ? `?${queryString}` : ''}`
    );
  },
};

// User Management API (New)
export const usersApi = {
  getAll: (params?: { page?: number; limit?: number; search?: string; role?: string }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('size', params.limit.toString());
    if (params?.search) query.append('search', params.search);
    if (params?.role) query.append('role', params.role);

    return fetchApi<{ content: any[]; totalElements: number; page: number; totalPages: number }>(
      `/v1/users?${query.toString()}`
    );
  },
  create: (userData: any) => fetchApi('/v1/users', { method: 'POST', body: JSON.stringify(userData) }),
  update: (userId: number, userData: any) => fetchApi(`/v1/users/${userId}`, { method: 'PUT', body: JSON.stringify(userData) }),
  updateRole: (userId: number, role: string) => fetchApi(`/v1/users/${userId}/role`, { method: 'PATCH', body: JSON.stringify({ role }) }),
  delete: (userId: number) => fetchApi(`/v1/users/${userId}`, { method: 'DELETE' }),
};

export const categoriesApi = {
  // Public endpoints (no auth required) with pagination
  getAll: (params?: { 
    page?: number; 
    size?: number; 
    sortBy?: string; 
    sortDir?: string;
    isActive?: boolean;
  }) => {
    const query = new URLSearchParams();
    if (params?.page !== undefined) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.sortDir) query.append('sortDir', params.sortDir);
    if (params?.isActive !== undefined) query.append('isActive', params.isActive.toString());
    const queryString = query.toString();
    return fetchPublic<{ 
      success: boolean; 
      data: { 
        content: any[]; 
        page: number; 
        size: number; 
        totalElements: number; 
        totalPages: number;
        hasNext: boolean;
        hasPrevious: boolean;
      } 
    }>(`/v1/categories${queryString ? `?${queryString}` : ''}`);
  },

  // Search categories by name
  search: (params: { 
    name: string; 
    page?: number; 
    size?: number;
  }) => {
    const query = new URLSearchParams();
    query.append('name', params.name);
    if (params.page !== undefined) query.append('page', params.page.toString());
    if (params.size) query.append('size', params.size.toString());
    return fetchPublic<{ 
      success: boolean; 
      data: { 
        content: any[]; 
        page: number; 
        size: number; 
        totalElements: number; 
        totalPages: number;
        hasNext: boolean;
        hasPrevious: boolean;
      } 
    }>(`/v1/categories/search?${query.toString()}`);
  },

  getById: (id: number) => fetchPublic<{ success: boolean; data: any }>(`/v1/categories/${id}`),

  getSlug: (slug: string) => fetchPublic<{ success: boolean; data: any }>(`/v1/categories/slug/${slug}`),

  getChildren: (parentId: number) => fetchPublic<{ success: boolean; data: any[] }>(`/v1/categories/${parentId}/children`),

  // Get root categories (returns array directly, not paginated)
  getRoot: () => fetchPublic<{ success: boolean; data: any[] }>('/v1/categories/root'),

  getHierarchy: () => fetchPublic<{ success: boolean; data: any[] }>('/v1/categories/hierarchy'),

  // Admin endpoints (auth required)
  create: (data: any) =>
    fetchApi<{ category: any }>('/v1/categories', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  createParent: (data: any) =>
    fetchApi<{ success: boolean; data: any }>('/v1/categories/parent', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  createChild: (data: any) =>
    fetchApi<{ success: boolean; data: any }>('/v1/categories/child', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  update: (id: number, data: any) =>
    fetchApi<{ category: any }>(`/v1/categories/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  delete: (id: number) =>
    fetchApi<void>(`/v1/categories/${id}`, {
      method: 'DELETE',
    }),

  activate: (id: number) =>
    fetchApi<{ category: any }>(`/v1/categories/${id}/activate`, {
      method: 'PATCH',
    }),

  deactivate: (id: number) =>
    fetchApi<{ category: any }>(`/v1/categories/${id}/deactivate`, {
      method: 'PATCH',
    }),
};

// Cart API (Updated to match OpenAPI spec)
export const cartApi = {
  // Create new cart (for guests)
  create: () => fetchApi<{ id: number }>('/v1/carts', {
    method: 'POST',
  }),

  // Get cart by ID (no auth required)
  get: (cartId: number) => fetchApi<{ items: any[]; totalAmount: number; itemCount: number; id: number; status: string }>(`/v1/carts/${cartId}`),

  // Add item to cart (no auth required)
  addItem: (cartId: number, productId: number, quantity: number) =>
    fetchApi<{ id: number; productId: number; quantity: number; productName: string; productPrice: number; subtotal: number }>(`/v1/carts/${cartId}/items`, {
      method: 'POST',
      body: JSON.stringify({ productId, quantity }),
    }),

  // Update item quantity (no auth required)
  updateItem: (cartId: number, productId: number, quantity: number) =>
    fetchApi<{ id: number; productId: number; quantity: number; productName: string; productPrice: number; subtotal: number }>(`/v1/carts/${cartId}/items/${productId}`, {
      method: 'PUT',
      body: JSON.stringify({ quantity }),
    }),

  // Remove item from cart (no auth required)
  removeItem: (cartId: number, productId: number) =>
    fetchApi<void>(`/v1/carts/${cartId}/items/${productId}`, {
      method: 'DELETE',
    }),

  // Clear all items from cart (no auth required)
  clear: (cartId: number) => fetchApi<void>(`/v1/carts/${cartId}/items`, {
    method: 'DELETE',
  }),
};

// Orders API
export const ordersApi = {
  // User Orders
  getAll: () => fetchApi<{ orders: any[] }>('/v1/orders'),

  getById: (id: number) => fetchApi<{ order: any }>(`/v1/orders/${id}`),

  getUserOrders: (params?: { page?: number; limit?: number; status?: string }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    if (params?.status) query.append('status', params.status);

    return fetchApi<{ orders: any[]; total: number; page: number; totalPages: number }>(
      `/v1/orders/my-orders?${query.toString()}`
    );
  },

create: (data: CreateOrderRequest) =>
    fetchApi<CreateOrderResponse>('/v1/orders', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  // Add coupon validation endpoint
  validateCoupon: (code: string, total: number) =>
    fetchApi<{ coupon: Coupon }>('/v1/orders/coupon/validate', {
      method: 'POST',
      body: JSON.stringify({ code, total }),
    }),

  // Add tax configuration endpoint
  getTaxConfiguration: () =>
    fetchApi<TaxConfiguration>('/v1/orders/tax-config'),

  cancel: (id: number) =>
    fetchApi<{ order: any }>(`/v1/orders/${id}/cancel`, {
      method: 'PUT',
    }),

  getTracking: (id: number) =>
    fetchApi<{ tracking: any }>(`/v1/orders/${id}/tracking`),

  // Get order by order number
  getByOrderNumber: (orderNumber: string) =>
    fetchApi<{ order: any }>(`/v1/orders/number/${orderNumber}`),

  // User Addresses
  getUserAddresses: () =>
    fetchApi<{ addresses: any[] }>('/user/addresses'),

  saveAddress: (data: any) =>
    fetchApi<{ address: any }>('/user/addresses', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  updateAddress: (id: number, data: any) =>
    fetchApi<{ address: any }>(`/user/addresses/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  deleteAddress: (id: number) =>
    fetchApi<void>(`/user/addresses/${id}`, {
      method: 'DELETE',
    }),

  setDefaultAddress: (id: number) =>
    fetchApi<{ address: any }>(`/user/addresses/${id}/default`, {
      method: 'PUT',
    }),

  // Delivery Configuration
  getRegions: () =>
    fetchApi<{ regions: any[] }>('/delivery/regions'),

  getTowns: (regionId: number) =>
    fetchApi<{ towns: any[] }>(`/delivery/regions/${regionId}/towns`),

  getBusStations: (townId: number) =>
    fetchApi<{ stations: any[] }>(`/delivery/towns/${townId}/stations`),

  getDeliveryFees: (townId?: number) => {
    const query = townId ? `?townId=${townId}` : '';
    return fetchApi<{ fees: any[] }>(`/delivery/fees${query}`);
  },

  // Admin Delivery Management
  adminCreateRegion: (data: any) =>
    fetchApi<{ region: any }>('/admin/delivery/regions', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  adminUpdateRegion: (id: number, data: any) =>
    fetchApi<{ region: any }>(`/admin/delivery/regions/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  adminDeleteRegion: (id: number) =>
    fetchApi<void>(`/admin/delivery/regions/${id}`, {
      method: 'DELETE',
    }),

  adminCreateTown: (data: any) =>
    fetchApi<{ town: any }>('/admin/delivery/towns', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  adminUpdateTown: (id: number, data: any) =>
    fetchApi<{ town: any }>(`/admin/delivery/towns/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  adminDeleteTown: (id: number) =>
    fetchApi<void>(`/admin/delivery/towns/${id}`, {
      method: 'DELETE',
    }),

  adminCreateBusStation: (data: any) =>
    fetchApi<{ station: any }>('/admin/delivery/stations', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  adminUpdateBusStation: (id: number, data: any) =>
    fetchApi<{ station: any }>(`/admin/delivery/stations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  adminDeleteBusStation: (id: number) =>
    fetchApi<void>(`/admin/delivery/stations/${id}`, {
      method: 'DELETE',
    }),

  adminCreateDeliveryFee: (data: any) =>
    fetchApi<{ fee: any }>('/admin/delivery/fees', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  adminUpdateDeliveryFee: (id: number, data: any) =>
    fetchApi<{ fee: any }>(`/admin/delivery/fees/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  adminDeleteDeliveryFee: (id: number) =>
    fetchApi<void>(`/admin/delivery/fees/${id}`, {
      method: 'DELETE',
    }),

  // Customer Order Management (Dashboard for customers)
  getMyOrdersByStatus: (status: string, params?: { page?: number; limit?: number }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    
    return fetchApi<{ orders: any[]; total: number; page: number; totalPages: number }>(
      `/v1/orders/my-orders/status/${status}?${query.toString()}`
    );
  },

  // Customer can edit order only if status is pending
  editMyOrder: (id: number, data: any) =>
    fetchApi<{ order: any }>(`/v1/orders/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  // Admin Order Management
  adminGetAllOrders: (params?: { page?: number; limit?: number; status?: string }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    if (params?.status) query.append('status', params.status);

    return fetchApi<{ orders: any[]; total: number }>(`/v1/orders/admin/all?${query.toString()}`);
  },

  adminGetOrdersByStatus: (status: string) =>
    fetchApi<{ orders: any[] }>(`/v1/orders/admin/status/${status}`),

  adminUpdateOrder: (id: number, data: any) =>
    fetchApi<{ order: any }>(`/v1/orders/admin/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  adminShipOrder: (id: number) =>
    fetchApi<{ order: any }>(`/v1/orders/admin/${id}/ship`, {
      method: 'PUT',
    }),

  adminDeliverOrder: (id: number) =>
    fetchApi<{ order: any }>(`/v1/orders/admin/${id}/deliver`, {
      method: 'PUT',
    }),

  adminConfirmOrder: (id: number) =>
    fetchApi<{ order: any }>(`/v1/orders/admin/${id}/confirm`, {
      method: 'PUT',
    }),

  adminUpdatePaymentStatus: (id: number, data: any) =>
    fetchApi<{ order: any }>(`/v1/orders/admin/${id}/payment-status`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  adminGetOrder: (id: number) =>
    fetchApi<{ order: any }>(`/v1/orders/admin/${id}`),

  adminGetStatistics: () =>
    fetchApi<{ stats: any }>('/v1/orders/admin/statistics'),
};

// Admin API
export const adminApi = {
  // General admin endpoints
  hello: () =>
    fetchApi<{ message: string }>('/admin/hello'),

  getApiInfo: () =>
    fetchApi<{ info: any }>('/api/info'),

  // Dashboard
  getDashboardStats: () =>
    fetchApi<{
      totalOrders: number;
      totalRevenue: number;
      totalProducts: number;
      totalUsers: number;
      recentOrders: any[];
    }>('/admin/dashboard'),

  // Analytics
  getAnalytics: (params?: { startDate?: string; endDate?: string; granularity?: 'day' | 'week' | 'month' }) =>
    fetchApi<{
      analytics: any;
    }>(`/admin/analytics${params ? `?${new URLSearchParams(params as any).toString()}` : ''}`),

  // Products Management
  getProducts: (params?: { page?: number; limit?: number; search?: string }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    if (params?.search) query.append('search', params.search);
    
    return fetchApi<{ products: any[]; total: number; page: number; totalPages: number }>(
      `/admin/products/all?${query.toString()}`
    );
  },

  // Users (legacy endpoints - use usersApi instead)
  getAllUsers: () => fetchApi<{ users: any[] }>('/admin/users'),

  // Social Links (Fixed platforms with admin-assigned URLs)
  getSocialLinks: () => fetchApi<{ socialLinks: any[] }>('/admin/social-links'),

  updateSocialLink: (platform: string, data: { url: string | null; isActive: boolean }) =>
    fetchApi<{ socialLink: any }>(`/admin/social-links/${platform}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  // App Download Links (Apple App Store & Google Play Store)
  getAppDownloadLinks: () => fetchApi<{ appDownloadLinks: any[] }>('/admin/app-download-links'),

  updateAppDownloadLink: (platform: string, data: { url: string | null; isActive: boolean }) =>
    fetchApi<{ appDownloadLink: any }>(`/admin/app-download-links/${platform}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
};

// Wishlist API
export const wishlistApi = {
  // Get all wishlist items
  getAll: (params?: { page?: number; limit?: number }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    const queryString = query.toString();
    return fetchApi<{ success: boolean; wishlist: any[]; total: number; count: number }>(
      `/api/v1/wishlist${queryString ? `?${queryString}` : ''}`
    );
  },

  // Get wishlist count
  getCount: () =>
    fetchApi<{ success: boolean; count: number }>('/api/v1/wishlist/summary'),

  // Toggle product in wishlist (add/remove)
  toggle: (productId: string | number) =>
    fetchApi<{ success: boolean; message: string; inWishlist: boolean; count: number }>('/api/v1/wishlist', {
      method: 'POST',
      body: JSON.stringify({ productId }),
    }),

  // Add product to wishlist
  add: (productId: number) =>
    fetchApi<{ success: boolean; message: string; wishlistItem: any }>('/api/v1/wishlist', {
      method: 'POST',
      body: JSON.stringify({ productId }),
    }),

  // Remove product from wishlist
  remove: (productId: number) =>
    fetchApi<{ success: boolean; message: string }>(`/api/v1/wishlist/${productId}`, {
      method: 'DELETE',
    }),

  // Move single item to cart (updated path from OpenAPI)
  moveToCart: (productId: number) =>
    fetchApi<{ success: boolean; message: string }>(`/api/v1/wishlist/${productId}/move-to-cart`, {
      method: 'POST',
    }),

  // Add all wishlist items to cart
  addAllToCart: (productIds: number[]) =>
    fetchApi<{ success: boolean; message: string; addedCount: number }>('/api/v1/wishlist/apply-to-cart', {
      method: 'POST',
      body: JSON.stringify({ productIds }),
    }),

  // Check if product is in wishlist
  isInWishlist: (productId: number) =>
    fetchApi<{ inWishlist: boolean }>(`/api/v1/wishlist/check/${productId}`),

  // New endpoints from OpenAPI
  updateItem: (productId: number, data: any) =>
    fetchApi<{ success: boolean; item: any }>(`/api/v1/wishlist/${productId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  getPriceDrops: () =>
    fetchApi<{ success: boolean; items: any[] }>('/api/v1/wishlist/price-drops'),

  markAsPurchased: (productId: number) =>
    fetchApi<{ success: boolean; item: any }>(`/api/v1/wishlist/${productId}/purchase`, {
      method: 'PATCH',
    }),

  clear: () =>
    fetchApi<void>('/api/v1/wishlist/clear', {
      method: 'DELETE',
    }),
};

// Reviews API
export const reviewsApi = {
  // Get all reviews for a product
  getProductReviews: (productId: number, params?: { page?: number; size?: number; sortBy?: string; direction?: string }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.direction) query.append('direction', params.direction);

    return fetchApi<{
      success: boolean;
      data: {
        content: any[];
        page: number;
        size: number;
        totalElements: number;
        totalPages: number;
        hasNext: boolean;
        hasPrevious: boolean;
        empty: boolean;
        first: boolean;
        last: boolean;
      };
    }>(`/v1/reviews/product/${productId}?${query.toString()}`);
  },

  // Get product rating summary
  getProductRating: (productId: number) =>
    fetchApi<{
      success: boolean;
      data: {
        productId: number;
        averageRating: number;
        totalReviews: number;
        distribution: {
          fiveStars: number;
          fourStars: number;
          threeStars: number;
          twoStars: number;
          oneStar: number;
        };
      };
    }>(`/v1/reviews/product/${productId}/stats`),

  // Create a new review
  createReview: (data: { productId: number; rating: number; title: string; comment: string; pros?: string[]; cons?: string[]; images?: string[] }, userId: number) => {
    const query = new URLSearchParams();
    query.append('userId', userId.toString());
    
    return fetchApi<{
      success: boolean;
      data: {
        id: number;
        productId: number;
        productName: string;
        user: {
          id: number;
          firstName: string;
          lastName: string;
          email: string;
        };
        rating: number;
        title: string;
        comment: string;
        verifiedPurchase: boolean;
        approved: boolean;
        helpfulCount: number;
        notHelpfulCount: number;
        images: string[];
        pros: string[];
        cons: string[];
        adminResponse: string;
        createdAt: string;
        updatedAt: string;
      };
    }>(`/v1/reviews?${query.toString()}`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  // Get review by ID
  getById: (id: number) =>
    fetchApi<{ success: boolean; data: any }>(`/v1/reviews/${id}`),

  // Get user's reviews
  getUserReviews: (params?: { page?: number; size?: number }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());

    return fetchApi<{ success: boolean; data: any[] }>(`/v1/reviews?${query.toString()}`);
  },

  // Update a review (user can edit their own review)
  updateReview: (reviewId: number, data: { rating?: number; title?: string; comment?: string; pros?: string[]; cons?: string[]; images?: string[] }, userId: number) => {
    const query = new URLSearchParams();
    query.append('userId', userId.toString());
    
    return fetchApi<{ success: boolean; data: any }>(`/v1/reviews/${reviewId}?${query.toString()}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  // Delete a review (user can delete their own review)
  deleteReview: (reviewId: number, userId: number) => {
    const query = new URLSearchParams();
    query.append('userId', userId.toString());
    
    return fetchApi<{ success: boolean; message: string; data: {}; errors: {}; timestamp: string; path: string; statusCode: number }>(`/v1/reviews/${reviewId}?${query.toString()}`, {
      method: 'DELETE',
    });
  },

  // Mark review as helpful
  markHelpful: (reviewId: number) =>
    fetchApi<{ success: boolean; message: string; data: {}; errors: {}; timestamp: string; path: string; statusCode: number }>(`/v1/reviews/${reviewId}/helpful`, {
      method: 'POST',
    }),

  // Mark review as unhelpful
  markUnhelpful: (reviewId: number) =>
    fetchApi<{ success: boolean; message: string; data: {}; errors: {}; timestamp: string; path: string; statusCode: number }>(`/v1/reviews/${reviewId}/not-helpful`, {
      method: 'POST',
    }),

  // Admin: Get all reviews with filters
  adminGetAllReviews: (params?: { page?: number; limit?: number; status?: string; productId?: number }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    if (params?.status) query.append('status', params.status);
    if (params?.productId) query.append('productId', params.productId.toString());

    return fetchApi<{ reviews: any[]; total: number }>(`/admin/reviews?${query.toString()}`);
  },

  // Admin: Update review status (approve/reject)
  adminUpdateReviewStatus: (reviewId: number, data: { status: 'APPROVED' | 'REJECTED' | 'PENDING'; rejectionReason?: string }) =>
    fetchApi<{ review: any }>(`/admin/reviews/${reviewId}/status`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  // Admin: Delete review
  adminDeleteReview: (reviewId: number) =>
    fetchApi<void>(`/admin/reviews/${reviewId}`, {
      method: 'DELETE',
    }),

  // Admin: Edit review
  adminEditReview: (reviewId: number, data: { rating?: number; title?: string; comment?: string }) =>
    fetchApi<{ review: any }>(`/admin/reviews/${reviewId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  // Add/update review response (for sellers and admins)
  addReviewResponse: (reviewId: number, response: string) =>
    fetchApi<{ success: boolean; data: any }>(`/admin/reviews/${reviewId}/respond`, {
      method: 'POST',
      body: JSON.stringify({ response }),
    }),

  // Admin: Get all reviews with filtering
  adminGetAllReviews: (params?: { page?: number; size?: number; status?: string; search?: string }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.size) query.append('size', params.size.toString());
    if (params?.status) query.append('status', params.status);
    if (params?.search) query.append('search', params.search);

    return fetchApi<{
      success: boolean;
      data: {
        reviews: any[];
        total: number;
      };
    }>(`/admin/reviews?${query.toString()}`);
  },

  // Admin: Update review status
  adminUpdateReviewStatus: (reviewId: number, data: { status: string; rejectionReason?: string }) =>
    fetchApi<{ review: any }>(`/admin/reviews/${reviewId}/status`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  // Admin: Delete review
  adminDeleteReview: (reviewId: number) =>
    fetchApi<{ success: boolean; message: string; data: {}; errors: {}; timestamp: string; path: string; statusCode: number }>(`/admin/reviews/${reviewId}`, {
      method: 'DELETE',
    }),
};

// Public API for social links and app downloads (no auth required)
export const publicApi = {
  getSocialLinks: () =>
    fetch(`${API_BASE_URL}/social-links`)
      .then(res => res.json())
      .catch(() => ({ socialLinks: [] })),

  getAppDownloadLinks: () =>
    fetch(`${API_BASE_URL}/app-download-links`)
      .then(res => res.json())
      .catch(() => ({ appDownloadLinks: [] })),

  getHelpSupportSettings: () =>
    fetch(`${API_BASE_URL}/help-support/settings`)
      .then(res => res.json())
      .catch(() => ({ helpLinks: [], chatConfigs: [], floatingButtonEnabled: true, floatingButtonPosition: 'bottom-right' })),
};

export { fetchApi };
