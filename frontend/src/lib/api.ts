import axios, { AxiosError, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { authUtils } from './auth';
import { API_BASE_URL } from './constants';
import { ContactMessage, Subscriber } from '@/types';

// Extend Axios request config to include _retry property
interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

// Create axios instance
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = authUtils.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);
// Response interceptor - handle errors
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as CustomAxiosRequestConfig | undefined;

    if (originalRequest && error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = authUtils.getRefreshToken();
        const response = await axios.post('/auth/refresh', { refreshToken });
        const { accessToken } = response.data;

        authUtils.setToken(accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;

        return apiClient(originalRequest);
      } catch (refreshError) {
        authUtils.logout();
        if (typeof window !== 'undefined') {
          window.location.href = '/auth/login';
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);


// Generic API request function
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await apiClient.request<T>(config);
  return response.data;
}

// ============ AUTH API ============
export const authApi = {
  // Adapted to new backend public auth endpoints
  login: (usernameOrEmail: string, password: string) =>
    request<{ success: boolean; message?: string; data?: any; errors?: any }>({
      method: 'POST',
      url: '/v1/users/auth/login',
      data: { usernameOrEmail, password },
    }),

  register: (data: { username?: string; email: string; password: string; firstName?: string; lastName?: string; phoneNumber?: string }) =>
    request<{ success: boolean; message?: string; data?: any; errors?: any }>({
      method: 'POST',
      url: '/v1/users/auth/register',
      data,
    }),

  getProfile: () =>
    request<{ user: any }>({
      method: 'GET',
      url: '/auth/profile',
    }),

  updateProfile: (data: Partial<{ firstName: string; lastName: string; phone: string }>) =>
    request<{ user: any }>({
      method: 'PUT',
      url: '/auth/profile',
      data,
    }),

  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    request<{ message: string }>({
      method: 'PUT',
      url: '/auth/change-password',
      data,
    }),

  forgotPassword: (email: string) =>
    request<{ success: boolean; message: string }>({
      method: 'POST',
      url: '/auth/forgot-password',
      data: { email },
    }),

  resetPassword: (token: string, password: string) =>
    request<{ success: boolean; message: string }>({
      method: 'POST',
      url: '/auth/reset-password',
      data: { token, password },
    }),

  // User activities
  getActivities: (params?: { page?: number; limit?: number }) =>
    request<{ success: boolean; activities: any[]; total: number }>({
      method: 'GET',
      url: '/v1/users/activities',
      params,
    }),

  // User addresses (auth-level)
  getAuthAddresses: (params?: { page?: number; limit?: number }) =>
    request<{ success: boolean; addresses: any[]; total: number }>({
      method: 'GET',
      url: '/v1/users/addresses',
      params,
    }),

  createAuthAddress: (data: any) =>
    request<{ success: boolean; address: any }>({
      method: 'POST',
      url: '/v1/users/addresses',
      data,
    }),

  // Disabled: single address fetch is deprecated. Use `getAuthAddresses` instead.
  getAuthAddress: (id: number) =>
    Promise.reject(new Error('getAuthAddress endpoint is disabled. Use getAuthAddresses or contact support.')) as Promise<{ success: boolean; address: any }>,

  updateAuthAddress: (id: number, data: any) =>
    request<{ success: boolean; address: any }>({
      method: 'PUT',
      url: `/v1/users/addresses/${id}`,
      data,
    }),

  patchAuthAddress: (id: number, data: any) =>
    request<{ success: boolean; address: any }>({
      method: 'PATCH',
      url: `/v1/users/addresses/${id}`,
      data,
    }),

  deleteAuthAddress: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/v1/users/addresses/${id}`,
    }),

  // Email verification
  verifyEmail: (token: string) =>
    request<{ success: boolean; message: string }>({
      method: 'POST',
      url: '/auth/verify-email',
      data: { token },
    }),
};

// ============ PRODUCTS API ============
export const productsApi = {
  // List all products with filters
  getAll: (params?: { 
    page?: number; 
    size?: number; 
    sortBy?: string; 
    direction?: string; 
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
    search?: string;
  }) =>
    request<{ products: any[]; total: number; page: number; totalPages: number }>({
      method: 'GET',
      url: '/v1/products',
      params,
    }),

  // Search products
  search: (params: {
    search: string;
    page?: number;
    size?: number;
    sortBy?: string;
    direction?: string;
  }) =>
    request<{ products: any[]; total: number; page: number; totalPages: number }>({
      method: 'GET',
      url: '/v1/products/search',
      params,
    }),

  // Create product
  create: (data: any) =>
    request<{ success: boolean; product: any }>({
      method: 'POST',
      url: '/v1/products',
      data,
    }),

  // Get product by ID
  getById: (id: number) =>
    request<{ product: any }>({
      method: 'GET',
      url: `/v1/products/${id}`,
    }),

  // Update product (full)
  update: (id: number, data: any) =>
    request<{ success: boolean; product: any }>({
      method: 'PUT',
      url: `/v1/products/${id}`,
      data,
    }),

  // Update product (partial)
  patch: (id: number, data: any) =>
    request<{ success: boolean; product: any }>({
      method: 'PATCH',
      url: `/v1/products/${id}`,
      data,
    }),

  // Delete product
  delete: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/v1/products/${id}`,
    }),

  // Get product by slug (legacy support)
  getBySlug: (slug: string) =>
    request<{ product: any }>({
      method: 'GET',
      url: `/v1/products/slug/${slug}`,
    }),

  // Featured products
  getFeatured: () =>
    request<{ products: any[] }>({
      method: 'GET',
      url: '/v1/products/featured',
    }),

  // Trending products
  getTrending: () =>
    request<{ products: any[] }>({
      method: 'GET',
      url: '/products/trending',
    }),

  // Top rated products
  getTopRated: () =>
    request<{ products: any[] }>({
      method: 'GET',
      url: '/products/top-rated',
    }),

  // Frequently purchased products
  getFrequentlyPurchased: () =>
    request<{ success: boolean; products: any[] }>({
      method: 'GET',
      url: '/products/frequently-purchased',
    }),

  // ============ PRODUCT IMAGES ============
  // List product images
  getAllImages: (productId: number) =>
    request<{ success: boolean; images: any[] }>({
      method: 'GET',
      url: `/products/products/${productId}/images`,
    }),

  // Upload single image
  uploadImage: (productId: number, data: FormData) =>
    request<{ success: boolean; image: any }>({
      method: 'POST',
      url: `/products/products/${productId}/images`,
      data,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  // Bulk upload images
  bulkUploadImages: (productId: number, data: FormData) =>
    request<{ success: boolean; images: any[] }>({
      method: 'POST',
      url: `/products/products/${productId}/images/bulk-upload`,
      data,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  // Reorder images
  reorderImages: (productId: number, data: { imageIds: number[] }) =>
    request<{ success: boolean; images: any[] }>({
      method: 'POST',
      url: `/products/products/${productId}/images/reorder`,
      data,
    }),

  // Get specific image
  getImage: (productId: number, imageId: number) =>
    request<{ success: boolean; image: any }>({
      method: 'GET',
      url: `/products/products/${productId}/images/${imageId}`,
    }),

  // Update image (full)
  updateImage: (productId: number, imageId: number, data: any) =>
    request<{ success: boolean; image: any }>({
      method: 'PUT',
      url: `/products/products/${productId}/images/${imageId}`,
      data,
    }),

  // Update image (partial)
  patchImage: (productId: number, imageId: number, data: any) =>
    request<{ success: boolean; image: any }>({
      method: 'PATCH',
      url: `/products/products/${productId}/images/${imageId}`,
      data,
    }),

  // Delete image
  deleteImage: (productId: number, imageId: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/products/products/${productId}/images/${imageId}`,
    }),

  // Set primary image
  setPrimaryImage: (productId: number, imageId: number) =>
    request<{ success: boolean; image: any }>({
      method: 'POST',
      url: `/products/products/${productId}/images/${imageId}/set-primary`,
    }),

  // Legacy support - Get images by product ID
  getImages: (productId: number) =>
    request<{ images: Array<{ id: number; url: string; alt: string; displayOrder: number }> }>({
      method: 'GET',
      url: `/products/products/${productId}/images`,
    }),

  // ============ PRODUCT REVIEWS ============
  // List product reviews (full path) - returns paginated response in `data`
  getProductReviews: (productId: number, params?: { page?: number; limit?: number }) =>
    request<{
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
    }>({
      method: 'GET',
      url: `/v1/reviews/product/${productId}`,
      params,
    }),

  // Create product review (full path)
  createProductReview: (productId: number, data: { rating: number; comment: string; title?: string }) =>
    request<{ success: boolean; review: any }>({
      method: 'POST',
      url: `/v1/reviews/product/${productId}`,
      data,
    }),

  // Get specific review
  getReview: (productId: number, reviewId: number) =>
    request<{ success: boolean; review: any }>({
      method: 'GET',
      url: `/v1/reviews/${reviewId}`,
    }),

  // Update review (full)
  updateReview: (productId: number, reviewId: number, data: any) =>
    request<{ success: boolean; review: any }>({
      method: 'PUT',
      url: `/v1/reviews/${reviewId}`,
      data,
    }),

  // Update review (partial)
  patchReview: (productId: number, reviewId: number, data: any) =>
    request<{ success: boolean; review: any }>({
      method: 'PATCH',
      url: `/v1/reviews/${reviewId}`,
      data,
    }),

  // Delete review
  deleteReview: (productId: number, reviewId: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/v1/reviews/${reviewId}`,
    }),

  // Get product rating
  getProductRating: (productId: number) =>
    request<{ success: boolean; rating: any }>({
      method: 'GET',
      url: `/v1/reviews/product/${productId}/stats`,
    }),

  // List product reviews (short path - legacy support) - returns paginated response
  getProductReviewsShort: (productId: number, params?: { page?: number; limit?: number }) =>
    request<{
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
    }>({
      method: 'GET',
      url: `/products/${productId}/reviews`,
      params,
    }),

  // ============ RECENTLY VIEWED ============
  // Get recently viewed products
  getRecentlyViewed: () =>
    request<{ success: boolean; products: any[] }>({
      method: 'GET',
      url: '/products/recently-viewed',
    }),

  // Add to recently viewed
  addToRecentlyViewed: (productId: number) =>
    request<{ success: boolean }>({
      method: 'POST',
      url: '/products/recently-viewed',
      data: { productId },
    }),
};

// ============ BRANDS API ============
export const brandsApi = {
  getAll: (params?: { page?: number; page_size?: number; search?: string }) =>
    request<{ count: number; next: string | null; previous: string | null; results: any[] }>({
      method: 'GET',
      url: '/products/brands',
      params,
    }),

  getById: (id: number) =>
    request<{ brand: any }>({
      method: 'GET',
      url: `/products/brands/${id}`,
    }),

  // Brand CRUD operations
  create: (data: { name: string; slug?: string; description?: string; logo?: string }) =>
    request<{ success: boolean; brand: any }>({
      method: 'POST',
      url: '/products/brands',
      data,
    }),

  update: (id: number, data: Partial<any>) =>
    request<{ success: boolean; brand: any }>({
      method: 'PUT',
      url: `/products/brands/${id}`,
      data,
    }),

  patch: (id: number, data: Partial<any>) =>
    request<{ success: boolean; brand: any }>({
      method: 'PATCH',
      url: `/products/brands/${id}`,
      data,
    }),

  delete: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/products/brands/${id}`,
    }),
};

// ============ CATEGORIES API ============
export const categoriesApi = {
  getAll: (params?: { page?: number; size?: number; sortBy?: string; sortDir?: string; isActive?: boolean }) =>
    request<{ categories: any[] }>({
      method: 'GET',
      url: '/v1/categories',
      params,
    }),

  getBySlug: (slug: string) =>
    request<{ category: any }>({
      method: 'GET',
      url: `/v1/categories/slug/${slug}`,
    }),

  // Get category hierarchy (for parent selection dropdown)
  getHierarchy: () =>
    request<any>({
      method: 'GET',
      url: '/v1/categories/hierarchy',
    }),

  // Create parent category (no parentId)
  createParent: (data: { 
    name: string; 
    description?: string; 
    imageUrl?: string; 
    displayOrder?: number; 
  }) =>
    request<{ success: boolean; category: any }>({
      method: 'POST',
      url: '/v1/categories/parent',
      data,
    }),

  // Create child category (requires parentId)
  createChild: (data: { 
    name: string; 
    description?: string; 
    imageUrl?: string; 
    parentId: number; 
    displayOrder?: number; 
  }) =>
    request<{ success: boolean; category: any }>({
      method: 'POST',
      url: '/v1/categories/child',
      data,
    }),

  // Legacy create (kept for backwards compatibility)
  create: (data: { 
    name: string; 
    description?: string; 
    imageUrl?: string; 
    parentId?: number | null; 
    displayOrder?: number; 
  }) =>
    request<{ success: boolean; category: any }>({
      method: 'POST',
      url: '/v1/categories',
      data,
    }),

  getById: (id: number) =>
    request<{ success: boolean; category: any }>({
      method: 'GET',
      url: `/v1/categories/${id}`,
    }),

  // Get child categories of a parent
  getChildren: (id: number) =>
    request<any>({
      method: 'GET',
      url: `/v1/categories/${id}/children`,
    }),

  // Search categories by name
  search: (name: string) =>
    request<any>({
      method: 'GET',
      url: '/v1/categories/search',
      params: { name },
    }),

  // Get root (top-level) categories only
  getRoot: () =>
    request<any>({
      method: 'GET',
      url: '/v1/categories/root',
    }),

  // Get all active categories
  getActive: () =>
    request<any>({
      method: 'GET',
      url: '/v1/categories/active',
    }),

  // Activate a category
  activate: (id: number) =>
    request<{ success: boolean; category: any }>({
      method: 'PATCH',
      url: `/v1/categories/${id}/activate`,
    }),

  // Deactivate a category
  deactivate: (id: number) =>
    request<{ success: boolean; category: any }>({
      method: 'PATCH',
      url: `/v1/categories/${id}/deactivate`,
    }),

  update: (id: number, data: Partial<any>) =>
    request<{ success: boolean; category: any }>({
      method: 'PUT',
      url: `/v1/categories/${id}`,
      data,
    }),

  patch: (id: number, data: Partial<any>) =>
    request<{ success: boolean; category: any }>({
      method: 'PATCH',
      url: `/v1/categories/${id}`,
      data,
    }),

  delete: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/v1/categories/${id}`,
    }),

  getSlug: (slug: string) =>
    request<{ success: boolean; category: any }>({
      method: 'GET',
      url: `/v1/categories/slug/${slug}`,
    }),
};

// ============ WISHLIST API ============
export const wishlistApi = {
  getAll: (params?: { page?: number; limit?: number }) =>
    request<{ success: boolean; wishlist: any[]; total: number; count: number }>({
      method: 'GET',
      url: '/api/v1/wishlist',
      params,
    }),

  getCount: () =>
    request<{ success: boolean; count: number }>({
      method: 'GET',
      url: '/api/v1/wishlist/summary',
    }),

  toggle: (productId: string) =>
    request<{ success: boolean; message: string; inWishlist: boolean; count: number }>({
      method: 'POST',
      url: '/api/v1/wishlist',
      data: { productId },
    }),

  addItem: (productId: number) =>
    request<{ success: boolean; message: string; wishlistItem: any }>({
      method: 'POST',
      url: '/api/v1/wishlist',
      data: { productId },
    }),

  removeItem: (productId: number) =>
    request<{ success: boolean; message: string }>({
      method: 'DELETE',
      url: `/api/v1/wishlist/${productId}`,
    }),

  addAllToCart: (productIds: number[]) =>
    request<{ success: boolean; message: string; addedCount: number }>({
      method: 'POST',
      url: '/wishlist/apply-to-cart',
      data: { productIds },
    }),
};

// ============ CART API ============
export const cartApi = {
  get: () =>
    request<{ items: any[]; total: number }>({
      method: 'GET',
      url: '/v1/carts',
    }),

  addItem: (productId: number, quantity: number) =>
    request<{ items: any[]; total: number }>({
      method: 'POST',
      url: '/v1/carts/items',
      data: { productId, quantity },
    }),

  updateItem: (itemId: number, quantity: number) =>
    request<{ items: any[]; total: number }>({
      method: 'PUT',
      url: `/v1/carts/items/${itemId}`,
      data: { quantity },
    }),

  removeItem: (itemId: number) =>
    request<{ items: any[]; total: number }>({
      method: 'DELETE',
      url: `/v1/carts/items/${itemId}`,
    }),

  clear: () =>
    request<void>({
      method: 'DELETE',
      url: '/v1/carts',
    }),

  // Guest cart operations
  getGuestCart: (guestId: string) =>
    request<{ success: boolean; items: any[]; total: number }>({
      method: 'GET',
      url: `/cart/guest/${guestId}`,
    }),

  addToGuestCart: (guestId: string, data: { productId: number; quantity: number }) =>
    request<{ success: boolean; items: any[]; total: number }>({
      method: 'POST',
      url: `/cart/guest/${guestId}/items`,
      data,
    }),

  // Create cart from tracking data
  createCartFromTracking: (data: { sessionId: string; products: any[] }) =>
    request<{ success: boolean; cartId: string; items: any[]; total: number }>({
      method: 'POST',
      url: '/v1/carts/from-tracking',
      data,
    }),
};

// ============ ADDRESSES API ============
export const addressesApi = {
  getAll: () =>
    request<{ success: boolean; addresses: any[] }>({
      method: 'GET',
      url: '/v1/users/addresses',
    }),

  create: (data: {
    label: string;
    recipientName: string;
    phone: string;
    street: string;
    city: string;
    region: string;
    country: string;
    postalCode?: string;
  }) =>
    request<{ success: boolean; message: string; address: any }>({
      method: 'POST',
      url: '/v1/users/addresses',
      data,
    }),

  update: (id: number, data: Partial<any>) =>
    request<{ success: boolean; message: string; address: any }>({
      method: 'PUT',
      url: `/v1/users/addresses/${id}`,
      data,
    }),

  delete: (id: number) =>
    request<{ success: boolean; message: string }>({
      method: 'DELETE',
      url: `/v1/users/addresses/${id}`,
    }),

  setDefault: (id: number) =>
    request<{ success: boolean; message: string }>({
      method: 'PUT',
      url: `/v1/users/addresses/${id}/default`,
    }),
};

// ============ DELIVERY API ============
export const deliveryApi = {
  getRegions: () =>
    request<{ success: boolean; regions: Array<{ id: number; name: string; code: string }> }>({
      method: 'GET',
      url: '/v1/delivery/regions',
    }),

  getTowns: (regionId: number) =>
    request<{ success: boolean; towns: Array<{ id: number; name: string; regionId: number }> }>({
      method: 'GET',
      url: `/v1/delivery/regions/${regionId}/towns`,
    }),

  getStations: (townId: number) =>
    request<{ success: boolean; stations: Array<{ id: number; name: string; address: string; townId: number }> }>({
      method: 'GET',
      url: `/v1/delivery/towns/${townId}/stations`,
    }),

  getFees: (params: { townId: number; method: 'BUS_STATION' | 'HOME_DELIVERY' }) =>
    request<{ success: boolean; fees: Array<{ id: number; townId: number; method: string; fee: number; estimatedDays: number }> }>({
      method: 'GET',
      url: '/v1/delivery/fees',
      params,
    }),

  // Delivery Addresses
  getDeliveryAddresses: (params?: { page?: number; limit?: number }) =>
    request<{ success: boolean; addresses: any[]; total?: number }>({
      method: 'GET',
      url: '/v1/delivery/addresses',
      params,
    }),

  createDeliveryAddress: (data: {
    recipientName: string;
    phone: string;
    street: string;
    city: string;
    region: string;
    country?: string;
    postalCode?: string;
  }) =>
    request<{ success: boolean; message: string; address: any }>({
      method: 'POST',
      url: '/v1/delivery/addresses',
      data,
    }),

  getDeliveryAddress: (id: number) =>
    request<{ success: boolean; address: any }>({
      method: 'GET',
      url: `/v1/delivery/addresses/${id}`,
    }),

  updateDeliveryAddress: (id: number, data: Partial<any>) =>
    request<{ success: boolean; message: string; address: any }>({
      method: 'PUT',
      url: `/v1/delivery/addresses/${id}`,
      data,
    }),

  patchDeliveryAddress: (id: number, data: Partial<any>) =>
    request<{ success: boolean; message: string; address: any }>({
      method: 'PATCH',
      url: `/v1/delivery/addresses/${id}`,
      data,
    }),

  deleteDeliveryAddress: (id: number) =>
    request<{ success: boolean; message: string }>({
      method: 'DELETE',
      url: `/v1/delivery/addresses/${id}`,
    }),

  // Bus Stations
  getBusStations: (params?: { townId?: number; page?: number; limit?: number }) =>
    request<{ success: boolean; stations: Array<{ id: number; name: string; address: string; phone?: string; townId?: number }> }>({
      method: 'GET',
      url: '/v1/delivery/bus-stations',
      params,
    }),

  getBusStation: (id: number) =>
    request<{ success: boolean; station: any }>({
      method: 'GET',
      url: `/v1/delivery/bus-stations/${id}`,
    }),

  // Shipping Methods
  getShippingMethods: (params?: { active?: boolean }) =>
    request<{ success: boolean; methods: Array<{ id: number; name: string; code: string; description?: string; isActive?: boolean }> }>({
      method: 'GET',
      url: '/v1/delivery/shipping-methods',
      params,
    }),

  getShippingMethod: (id: number) =>
    request<{ success: boolean; method: any }>({
      method: 'GET',
      url: `/v1/delivery/shipping-methods/${id}`,
    }),
};

// ============ ORDERS API ============
export const ordersApi = {
  getAll: (params?: { page?: number; limit?: number; status?: string }) =>
    request<{ orders: any[]; total: number; page: number }>({
      method: 'GET',
      url: '/v1/orders',
      params,
    }),

  getById: (id: number) =>
    request<{ order: any }>({
      method: 'GET',
      url: `/v1/orders/${id}`,
    }),

  create: (data: { shippingAddress: any; paymentMethod: string; paymentIntentId?: string }) =>
    request<{ order: any }>({
      method: 'POST',
      url: '/v1/orders',
      data,
    }),

  cancel: (id: number) =>
    request<{ order: any }>({
      method: 'PUT',
      url: `/v1/orders/${id}/cancel`,
    }),

  getTracking: (id: number) =>
    request<{ success: boolean; tracking: any }>({
      method: 'GET',
      url: `/v1/orders/${id}/tracking`,
    }),

  // Order stats
  getStats: (params?: { startDate?: string; endDate?: string; groupBy?: string }) =>
    request<{ success: boolean; stats: any[] }>({
      method: 'GET',
      url: '/orders/stats',
      params,
    }),

  // Update order
  update: (id: number, data: Partial<any>) =>
    request<{ success: boolean; order: any }>({
      method: 'PUT',
      url: `/orders/${id}`,
      data,
    }),
};

// ============ PAYMENTS API ============
export const paymentsApi = {
  initiatePayment: (orderId: number, paymentMethod: string, returnUrl: string) =>
    request<{ success: boolean; paymentUrl: string; paymentId: string; expiresAt: string }>({
      method: 'POST',
      url: '/payments/initiate',
      data: { orderId, paymentMethod, returnUrl },
    }),

  verifyPayment: (paymentId: string, status: string) =>
    request<{ success: boolean; message: string; order: any }>({
      method: 'POST',
      url: '/payments/verify',
      data: { paymentId, status },
    }),

  // Payment gateways
  getGateways: () =>
    request<{ success: boolean; gateways: any[] }>({
      method: 'GET',
      url: '/payments/gateways',
    }),

  // Process payment
  processPayment: (data: { orderId: number; gatewayId: string; amount: number; paymentData: any }) =>
    request<{ success: boolean; transaction: any }>({
      method: 'POST',
      url: '/payments/process',
      data,
    }),

  // Refund
  refundPayment: (transactionId: string, amount?: number, reason?: string) =>
    request<{ success: boolean; refund: any }>({
      method: 'POST',
      url: '/payments/refund',
      data: { transactionId, amount, reason },
    }),

  // Payment stats
  getStats: (params?: { startDate?: string; endDate?: string }) =>
    request<{ success: boolean; stats: any }>({
      method: 'GET',
      url: '/payments/stats',
      params,
    }),

  // Transactions
  getTransactions: (params?: { page?: number; limit?: number; status?: string }) =>
    request<{ success: boolean; transactions: any[]; total: number }>({
      method: 'GET',
      url: '/payments/transactions',
      params,
    }),
};

// ============ STRIPE API (Legacy - use paymentsApi instead) ============
export const stripeApi = {
  createPaymentIntent: (amount: number) =>
    request<{ clientSecret: string }>({
      method: 'POST',
      url: '/payments/create-intent',
      data: { amount },
    }),

  confirmPayment: (paymentIntentId: string) =>
    request<{ success: boolean }>({
      method: 'POST',
      url: '/payments/confirm',
      data: { paymentIntentId },
    }),
};

// ============ ADMIN API ============
export const adminApi = {
  // Dashboard
  getDashboardStats: (params?: { startDate?: string; endDate?: string }) =>
    request<{
      totalOrders: number;
      totalRevenue: number;
      totalProducts: number;
      totalUsers: number;
      recentOrders: any[];
      revenueChart: any[];
      orderStatusChart: any[];
      topProducts: any[];
    }>({
      method: 'GET',
      url: '/admin/dashboard',
      params,
    }),

  // Products
  getProducts: (params?: { page?: number; limit?: number; search?: string; category?: string }) =>
    request<{ products: any[]; total: number; page: number }>({
      method: 'GET',
      url: '/admin/products',
      params,
    }),

  getProduct: (id: number) =>
    request<{ product: any }>({
      method: 'GET',
      url: `/admin/products/${id}`,
    }),

  createProduct: (data: FormData) =>
    request<{ product: any }>({
      method: 'POST',
      url: '/admin/products',
      data,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  updateProduct: (id: number, data: FormData) =>
    request<{ product: any }>({
      method: 'PUT',
      url: `/admin/products/${id}`,
      data,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  deleteProduct: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/products/${id}`,
    }),

  // Categories
  getCategories: () =>
    request<{ categories: any[] }>({
      method: 'GET',
      url: '/v1/categories',
    }),

  createCategory: (data: { name: string; slug: string; description?: string; image?: string }) =>
    request<{ category: any }>({
      method: 'POST',
      url: '/v1/categories',
      data,
    }),

  updateCategory: (id: number, data: Partial<{ name: string; slug: string; description: string }>) =>
    request<{ category: any }>({
      method: 'PUT',
      url: `/v1/categories/${id}`,
      data,
    }),

  deleteCategory: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/v1/categories/${id}`,
    }),

  // Tags
  getTags: () =>
    request<{ tags: any[] }>({
      method: 'GET',
      url: '/admin/tags',
    }),

  createTag: (data: { name: string; slug: string }) =>
    request<{ tag: any }>({
      method: 'POST',
      url: '/admin/tags',
      data,
    }),

  updateTag: (id: number, data: { name: string; slug: string }) =>
    request<{ tag: any }>({
      method: 'PUT',
      url: `/admin/tags/${id}`,
      data,
    }),

  deleteTag: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/tags/${id}`,
    }),

  // ============ ADMIN ORDERS API ============
  // Get all orders (Admin)
  getOrders: (params?: { page?: number; size?: number; sortBy?: string; direction?: string }) =>
    request<any>({
      method: 'GET',
      url: '/v1/orders/admin/all',
      params,
    }),

  // Get orders by status (Admin)
  getOrdersByStatus: (status: string, params?: { page?: number; size?: number }) =>
    request<any>({
      method: 'GET',
      url: `/v1/orders/admin/status/${status}`,
      params,
    }),

  // Get order statistics (Admin) - for dashboard
  getOrderStatistics: () =>
    request<any>({
      method: 'GET',
      url: '/v1/orders/admin/statistics',
    }),

  // Get single order (Admin)
  getOrder: (id: number) =>
    request<any>({
      method: 'GET',
      url: `/v1/orders/admin/${id}`,
    }),

  // Update order (Admin)
  updateOrder: (id: number, data: { status?: string; trackingNumber?: string; carrier?: string; adminNotes?: string }) =>
    request<any>({
      method: 'PUT',
      url: `/v1/orders/admin/${id}`,
      data,
    }),

  // Confirm order (Admin)
  confirmOrder: (id: number) =>
    request<any>({
      method: 'PUT',
      url: `/v1/orders/admin/${id}/confirm`,
    }),

  // Ship order (Admin)
  shipOrder: (id: number, trackingNumber: string, carrier?: string) =>
    request<any>({
      method: 'PUT',
      url: `/v1/orders/admin/${id}/ship`,
      params: { trackingNumber, carrier },
    }),

  // Mark as delivered (Admin)
  deliverOrder: (id: number) =>
    request<any>({
      method: 'PUT',
      url: `/v1/orders/admin/${id}/deliver`,
    }),

  // Refund order (Admin)
  refundOrder: (id: number, amount: number, reason: string) =>
    request<any>({
      method: 'PUT',
      url: `/v1/orders/admin/${id}/refund`,
      params: { amount, reason },
    }),

  // Update payment status (Admin)
  updatePaymentStatus: (id: number, status: string) =>
    request<any>({
      method: 'PUT',
      url: `/v1/orders/admin/${id}/payment-status`,
      params: { status },
    }),

  // Delete order (Admin)
  deleteOrder: (id: number) =>
    request<any>({
      method: 'DELETE',
      url: `/v1/orders/admin/${id}`,
    }),

  // Legacy alias for status update
  updateOrderStatus: (id: number, status: string) =>
    request<any>({
      method: 'PUT',
      url: `/v1/orders/admin/${id}`,
      data: { status },
    }),

  // Users
  getUsers: (params?: { page?: number; limit?: number; search?: string; role?: string }) =>
    request<{ users: any[]; total: number; page: number }>({
      method: 'GET',
      url: '/admin/users',
      params,
    }),

  updateUserStatus: (id: number, status: 'active' | 'blocked') =>
    request<{ user: any }>({
      method: 'PUT',
      url: `/admin/users/${id}/status`,
      data: { status },
    }),

  updateUserRole: (id: number, role: 'user' | 'admin' | 'seller' | 'customer') =>
    request<{ user: any }>({
      method: 'PUT',
      url: `/admin/users/${id}/role`,
      data: { role },
    }),

  // Promotions
  getPromotions: () =>
    request<{ promotions: any[] }>({
      method: 'GET',
      url: '/admin/promotions',
    }),

  createPromotion: (data: {
    code: string;
    discount: number;
    type: 'percentage' | 'fixed';
    startDate: string;
    endDate: string;
    minPurchase?: number;
    maxUses?: number;
  }) =>
    request<{ promotion: any }>({
      method: 'POST',
      url: '/admin/promotions',
      data,
    }),

  updatePromotion: (id: number, data: Partial<any>) =>
    request<{ promotion: any }>({
      method: 'PUT',
      url: `/admin/promotions/${id}`,
      data,
    }),

  deletePromotion: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/promotions/${id}`,
    }),

  // Analytics
  getAnalytics: (params: { startDate: string; endDate: string; granularity?: 'day' | 'week' | 'month' }) =>
    request<{
      revenue: { date: string; amount: number }[];
      orders: { date: string; count: number }[];
      topProducts: { name: string; revenue: number; quantity: number }[];
      topCategories: { name: string; revenue: number }[];
      customerStats: { newCustomers: number; returningCustomers: number };
    }>({
      method: 'GET',
      url: '/admin/analytics',
      params,
    }),

  // Settings
  getSettings: () =>
    request<{ settings: any }>({
      method: 'GET',
      url: '/admin/settings',
    }),

  updateSettings: (data: any) =>
    request<{ settings: any }>({
      method: 'PUT',
      url: '/admin/settings',
      data,
    }),

  // Delivery Management
  getRegions: () =>
    request<{ regions: any[] }>({
      method: 'GET',
      url: '/admin/delivery/regions',
    }),

  createRegion: (data: { name: string; code: string }) =>
    request<{ region: any }>({
      method: 'POST',
      url: '/admin/delivery/regions',
      data,
    }),

  updateRegion: (id: number, data: { name: string; code: string }) =>
    request<{ region: any }>({
      method: 'PUT',
      url: `/admin/delivery/regions/${id}`,
      data,
    }),

  deleteRegion: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/delivery/regions/${id}`,
    }),

  getTowns: (params?: { regionId?: number }) =>
    request<{ towns: any[] }>({
      method: 'GET',
      url: '/admin/delivery/towns',
      params,
    }),

  createTown: (data: { name: string; regionId: number }) =>
    request<{ town: any }>({
      method: 'POST',
      url: '/admin/delivery/towns',
      data,
    }),

  updateTown: (id: number, data: { name: string; regionId: number }) =>
    request<{ town: any }>({
      method: 'PUT',
      url: `/admin/delivery/towns/${id}`,
      data,
    }),

  deleteTown: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/delivery/towns/${id}`,
    }),

  getStations: (params?: { townId?: number }) =>
    request<{ stations: any[] }>({
      method: 'GET',
      url: '/admin/delivery/stations',
      params,
    }),

  createStation: (data: { name: string; address: string; townId: number }) =>
    request<{ station: any }>({
      method: 'POST',
      url: '/admin/delivery/stations',
      data,
    }),

  updateStation: (id: number, data: { name: string; address: string; townId: number }) =>
    request<{ station: any }>({
      method: 'PUT',
      url: `/admin/delivery/stations/${id}`,
      data,
    }),

  deleteStation: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/delivery/stations/${id}`,
    }),

  getDeliveryFees: (params?: { townId?: number; method?: string }) =>
    request<{ fees: any[] }>({
      method: 'GET',
      url: '/admin/delivery/fees',
      params,
    }),

  createDeliveryFee: (data: { townId: number; method: 'BUS_STATION' | 'HOME_DELIVERY'; fee: number; estimatedDays: number }) =>
    request<{ deliveryFee: any }>({
      method: 'POST',
      url: '/admin/delivery/fees',
      data,
    }),

  updateDeliveryFee: (id: number, data: { fee: number; estimatedDays: number }) =>
    request<{ deliveryFee: any }>({
      method: 'PUT',
      url: `/admin/delivery/fees/${id}`,
      data,
    }),

  deleteDeliveryFee: (id: number) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/delivery/fees/${id}`,
    }),

  // Reviews Moderation
  getAllReviews: (params?: { page?: number; limit?: number; status?: string; productId?: number }) =>
    request<{ reviews: any[]; total: number; page: number; totalPages: number }>({
      method: 'GET',
      url: '/admin/reviews',
      params,
    }),

  approveReview: (reviewId: number) =>
    request<{ review: any }>({
      method: 'PUT',
      url: `/admin/reviews/${reviewId}/approve`,
    }),

  rejectReview: (reviewId: number, data: { reason: string }) =>
    request<{ review: any }>({
      method: 'PUT',
      url: `/admin/reviews/${reviewId}/reject`,
      data,
    }),

  deleteReview: (reviewId: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/admin/reviews/${reviewId}`,
    }),
};

// ============ CONTACT API ============
export const contactApi = {
  submitMessage: (data: { name: string; email: string; subject: string; message: string }) =>
    request<{ message: string; id: number }>({
      method: 'POST',
      url: '/contact/messages',
      data,
    }),

  getContactDetails: () =>
    request<{ contactDetails: any }>({
      method: 'GET',
      url: '/contact/details',
    }),

  // Admin
  getMessages: (params?: { page?: number; limit?: number; status?: string; sort?: string }) =>
    request<ContactMessage[]>({
      method: 'GET',
      url: '/admin/contact/messages',
      params,
    }).then((response: any) => response.items || response),

  updateMessageStatus: (id: string, status: 'new' | 'read' | 'archived') =>
    request<{ message: string }>({
      method: 'PATCH',
      url: `/admin/contact/messages/${id}`,
      data: { status },
    }),

  deleteMessage: (id: string) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/contact/messages/${id}`,
    }),

  updateContactDetails: (data: any) =>
    request<{ contactDetails: any }>({
      method: 'PUT',
      url: '/admin/contact/details',
      data,
    }),
};

// ============ NEWSLETTER API ============
export const newsletterApi = {
  subscribe: (email: string) =>
    request<{ message: string; id: number }>({
      method: 'POST',
      url: '/newsletter/subscribe',
      data: { email },
    }),

  unsubscribe: (email: string) =>
    request<{ message: string }>({
      method: 'POST',
      url: '/newsletter/unsubscribe',
      data: { email },
    }),

  // Admin
  getSubscribers: (params?: { page?: number; limit?: number; status?: string; sort?: string }) =>
    request<Subscriber[]>({
      method: 'GET',
      url: '/admin/newsletter/subscribers',
      params,
    }).then((response: any) => response.items || response),

  deleteSubscriber: (email: string) =>
    request<void>({
      method: 'DELETE',
      url: `/admin/newsletter/subscribers/${email}`,
    }),

  exportSubscribers: () =>
    request<string>({
      method: 'GET',
      url: '/admin/newsletter/export',
    }),
};

// ============ LEGAL API ============
export const legalApi = {
  getPrivacyPolicy: () =>
    request<{ document: any }>({
      method: 'GET',
      url: '/legal/privacy-policy',
    }),

  getTermsOfService: () =>
    request<{ document: any }>({
      method: 'GET',
      url: '/legal/terms-of-service',
    }),

  // Admin
  updateDocument: (type: 'privacy-policy' | 'terms-of-service', data: { title: string; content: string }) =>
    request<{ document: any }>({
      method: 'PUT',
      url: `/admin/legal/${type}`,
      data,
    }),
};

// ============ REVIEWS & RATINGS API ============
export const reviewsApi = {
  // Get all reviews for a product
  getProductReviews: (productId: number, params?: { page?: number; limit?: number; sort?: 'recent' | 'helpful' | 'rating' }) =>
    request<{ reviews: any[]; total: number; page: number; totalPages: number; rating: any }>({
      method: 'GET',
      url: `/products/${productId}/reviews`,
      params,
    }),

  // Get product rating summary
  getProductRating: (productId: number) =>
    request<{ rating: any }>({
      method: 'GET',
      url: `/products/${productId}/rating`,
    }),

  // Create review
  createReview: (productId: number, data: { rating: number; title: string; comment: string; images?: string[] }) =>
    request<{ review: any }>({
      method: 'POST',
      url: `/products/${productId}/reviews`,
      data,
    }),

  // Update own review
  updateReview: (productId: number, reviewId: number, data: { rating?: number; title?: string; comment?: string; images?: string[] }) =>
    request<{ review: any }>({
      method: 'PUT',
      url: `/products/${productId}/reviews/${reviewId}`,
      data,
    }),

  // Delete own review
  deleteReview: (productId: number, reviewId: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/products/${productId}/reviews/${reviewId}`,
    }),

  // Mark review as helpful/unhelpful
  markReviewHelpful: (productId: number, reviewId: number, helpful: boolean) =>
    request<{ success: boolean }>({
      method: 'POST',
      url: `/products/${productId}/reviews/${reviewId}/helpful`,
      data: { helpful },
    }),
};

// ============ HELP & SUPPORT API ============
export const helpSupportApi = {
  // Get all help links and chat configs
  getSettings: () =>
    request<{ helpLinks: any[]; chatConfigs: any[]; floatingButtonEnabled: boolean; floatingButtonPosition: string }>({
      method: 'GET',
      url: '/help-support/settings',
    }),

  // Admin: Create help link
  createHelpLink: (data: { title: string; description?: string; url: string; category: string; displayOrder: number; icon?: string }) =>
    request<{ helpLink: any }>({
      method: 'POST',
      url: '/admin/help-support/links',
      data,
    }),

  // Admin: Update help link
  updateHelpLink: (id: number, data: Partial<{ title: string; description: string; url: string; category: string; displayOrder: number; icon: string; isActive: boolean }>) =>
    request<{ helpLink: any }>({
      method: 'PUT',
      url: `/admin/help-support/links/${id}`,
      data,
    }),

  // Admin: Delete help link
  deleteHelpLink: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/admin/help-support/links/${id}`,
    }),

  // Admin: Create chat config
  createChatConfig: (data: { type: string; title: string; url: string; displayText?: string; displayOrder: number; icon?: string }) =>
    request<{ chatConfig: any }>({
      method: 'POST',
      url: '/admin/help-support/chat-configs',
      data,
    }),

  // Admin: Update chat config
  updateChatConfig: (id: number, data: Partial<{ type: string; title: string; url: string; displayText: string; displayOrder: number; icon: string; isActive: boolean }>) =>
    request<{ chatConfig: any }>({
      method: 'PUT',
      url: `/admin/help-support/chat-configs/${id}`,
      data,
    }),

  // Admin: Delete chat config
  deleteChatConfig: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/admin/help-support/chat-configs/${id}`,
    }),

  // Admin: Update floating button settings
  updateFloatingButton: (data: { enabled: boolean; position: 'bottom-left' | 'bottom-right' }) =>
    request<{ success: boolean }>({
      method: 'PUT',
      url: '/admin/help-support/floating-button',
      data,
    }),

  // Admin: Get all reviews (for moderation)
  getAllReviews: (params?: { page?: number; limit?: number; status?: string; productId?: number }) =>
    request<{ reviews: any[]; total: number; page: number; totalPages: number }>({
      method: 'GET',
      url: '/admin/reviews',
      params,
    }),

  // Admin: Approve review
  approveReview: (reviewId: number) =>
    request<{ review: any }>({
      method: 'PUT',
      url: `/admin/reviews/${reviewId}/approve`,
    }),

  // Admin: Reject review
  rejectReview: (reviewId: number, data: { reason: string }) =>
    request<{ review: any }>({
      method: 'PUT',
      url: `/admin/reviews/${reviewId}/reject`,
      data,
    }),

  // Admin: Delete review
  deleteReviewAdmin: (reviewId: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/admin/reviews/${reviewId}`,
    }),
};

// ============ ANALYTICS API ============
export const analyticsApi = {
  getBehavior: (params?: { startDate?: string; endDate?: string }) =>
    request<{ success: boolean; data: any[] }>({
      method: 'GET',
      url: '/analytics/behavior',
      params,
    }),

  getOrders: (params?: { startDate?: string; endDate?: string }) =>
    request<{ success: boolean; data: any[] }>({
      method: 'GET',
      url: '/analytics/orders',
      params,
    }),

  getProducts: (params?: { startDate?: string; endDate?: string }) =>
    request<{ success: boolean; data: any[] }>({
      method: 'GET',
      url: '/analytics/products',
      params,
    }),

  getRevenue: (params?: { startDate?: string; endDate?: string; groupBy?: string }) =>
    request<{ success: boolean; data: any[] }>({
      method: 'GET',
      url: '/analytics/revenue',
      params,
    }),

  getTopCustomers: (params?: { limit?: number; startDate?: string; endDate?: string }) =>
    request<{ success: boolean; customers: any[] }>({
      method: 'GET',
      url: '/analytics/top-customers',
      params,
    }),

  getTopProducts: (params?: { limit?: number; startDate?: string; endDate?: string }) =>
    request<{ success: boolean; products: any[] }>({
      method: 'GET',
      url: '/analytics/top-products',
      params,
    }),

  getUsers: (params?: { startDate?: string; endDate?: string }) =>
    request<{ success: boolean; data: any[] }>({
      method: 'GET',
      url: '/analytics/users',
      params,
    }),
};

// ============ REPORTS API ============
export const reportsApi = {
  getCustomers: (params?: { startDate?: string; endDate?: string }) =>
    request<{ success: boolean; report: any }>({
      method: 'GET',
      url: '/reports/customers',
      params,
    }),

  exportReport: (data: { reportType: string; format?: string; startDate?: string; endDate?: string }) =>
    request<{ success: boolean; downloadUrl: string }>({
      method: 'POST',
      url: '/reports/export',
      data,
    }),

  getExports: (params?: { page?: number; limit?: number }) =>
    request<{ success: boolean; exports: any[]; total: number }>({
      method: 'GET',
      url: '/reports/exports',
      params,
    }),

  getExport: (id: number) =>
    request<{ success: boolean; export: any }>({
      method: 'GET',
      url: `/reports/exports/${id}`,
    }),

  getInventory: (params?: { lowStock?: boolean }) =>
    request<{ success: boolean; report: any }>({
      method: 'GET',
      url: '/reports/inventory',
      params,
    }),

  getOrders: (params?: { startDate?: string; endDate?: string; status?: string }) =>
    request<{ success: boolean; report: any }>({
      method: 'GET',
      url: '/reports/orders',
      params,
    }),

  getRevenue: (params?: { startDate?: string; endDate?: string; groupBy?: string }) =>
    request<{ success: boolean; report: any }>({
      method: 'GET',
      url: '/reports/revenue',
      params,
    }),

  getSales: (params?: { startDate?: string; endDate?: string }) =>
    request<{ success: boolean; report: any }>({
      method: 'GET',
      url: '/reports/sales',
      params,
    }),

  // Scheduled Reports
  getScheduledReports: (params?: { page?: number; limit?: number }) =>
    request<{ success: boolean; reports: any[]; total: number }>({
      method: 'GET',
      url: '/reports/scheduled',
      params,
    }),

  createScheduledReport: (data: { name: string; reportType: string; frequency: string; format?: string }) =>
    request<{ success: boolean; report: any }>({
      method: 'POST',
      url: '/reports/scheduled',
      data,
    }),

  getScheduledReport: (id: number) =>
    request<{ success: boolean; report: any }>({
      method: 'GET',
      url: `/reports/scheduled/${id}`,
    }),

  updateScheduledReport: (id: number, data: Partial<any>) =>
    request<{ success: boolean; report: any }>({
      method: 'PUT',
      url: `/reports/scheduled/${id}`,
      data,
    }),

  patchScheduledReport: (id: number, data: Partial<any>) =>
    request<{ success: boolean; report: any }>({
      method: 'PATCH',
      url: `/reports/scheduled/${id}`,
      data,
    }),

  deleteScheduledReport: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/reports/scheduled/${id}`,
    }),
};

// ============ SUPPORT API ============
export const supportApi = {
  // Contact Messages
  getContacts: (params?: { page?: number; limit?: number; status?: string }) =>
    request<{ success: boolean; contacts: any[]; total: number }>({
      method: 'GET',
      url: '/support/contact',
      params,
    }),

  createContact: (data: { name: string; email: string; subject: string; message: string; phone?: string }) =>
    request<{ success: boolean; contact: any }>({
      method: 'POST',
      url: '/support/contact',
      data,
    }),

  getContact: (id: number) =>
    request<{ success: boolean; contact: any }>({
      method: 'GET',
      url: `/support/contact/${id}`,
    }),

  updateContact: (id: number, data: Partial<any>) =>
    request<{ success: boolean; contact: any }>({
      method: 'PUT',
      url: `/support/contact/${id}`,
      data,
    }),

  patchContact: (id: number, data: Partial<any>) =>
    request<{ success: boolean; contact: any }>({
      method: 'PATCH',
      url: `/support/contact/${id}`,
      data,
    }),

  deleteContact: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/support/contact/${id}`,
    }),

  // FAQ
  getFAQs: (params?: { category?: string }) =>
    request<{ success: boolean; faqs: any[] }>({
      method: 'GET',
      url: '/support/faq',
      params,
    }),

  getFAQ: (id: number) =>
    request<{ success: boolean; faq: any }>({
      method: 'GET',
      url: `/support/faq/${id}`,
    }),

  // Support Tickets
  getTickets: (params?: { page?: number; limit?: number; status?: string; priority?: string }) =>
    request<{ success: boolean; tickets: any[]; total: number }>({
      method: 'GET',
      url: '/support/tickets',
      params,
    }),

  createTicket: (data: { subject: string; message: string; category?: string; priority?: string }) =>
    request<{ success: boolean; ticket: any }>({
      method: 'POST',
      url: '/support/tickets',
      data,
    }),

  getTicket: (id: number) =>
    request<{ success: boolean; ticket: any }>({
      method: 'GET',
      url: `/support/tickets/${id}`,
    }),

  updateTicket: (id: number, data: Partial<any>) =>
    request<{ success: boolean; ticket: any }>({
      method: 'PUT',
      url: `/support/tickets/${id}`,
      data,
    }),

  patchTicket: (id: number, data: Partial<any>) =>
    request<{ success: boolean; ticket: any }>({
      method: 'PATCH',
      url: `/support/tickets/${id}`,
      data,
    }),

  deleteTicket: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/support/tickets/${id}`,
    }),
};

// ============ TAGS API ============
export const tagsApi = {
  getAll: (params?: { page?: number; limit?: number; search?: string }) =>
    request<{ success: boolean; tags: any[]; total: number }>({
      method: 'GET',
      url: '/tags',
      params,
    }),

  getById: (id: number) =>
    request<{ success: boolean; tag: any }>({
      method: 'GET',
      url: `/tags/${id}`,
    }),

  create: (data: { name: string; slug?: string; description?: string }) =>
    request<{ success: boolean; tag: any }>({
      method: 'POST',
      url: '/tags',
      data,
    }),

  update: (id: number, data: Partial<any>) =>
    request<{ success: boolean; tag: any }>({
      method: 'PUT',
      url: `/tags/${id}`,
      data,
    }),

  patch: (id: number, data: Partial<any>) =>
    request<{ success: boolean; tag: any }>({
      method: 'PATCH',
      url: `/tags/${id}`,
      data,
    }),

  delete: (id: number) =>
    request<{ success: boolean }>({
      method: 'DELETE',
      url: `/tags/${id}`,
    }),

  // Tag products
  getTagProducts: (id: number, params?: { page?: number; limit?: number }) =>
    request<{ success: boolean; products: any[]; total: number }>({
      method: 'GET',
      url: `/tags/${id}/products`,
      params,
    }),
};
