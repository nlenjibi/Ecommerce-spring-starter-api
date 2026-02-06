import axios, { AxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '@/lib/constants';

// Create axios instance for seller API
const sellerApiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor - add seller auth token
sellerApiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sellerAccessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle errors
sellerApiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('sellerRefreshToken');
        const response = await axios.post('/auth/refresh', { refreshToken });
        const { accessToken } = response.data;

        localStorage.setItem('sellerAccessToken', accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;

        return sellerApiClient(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('sellerAccessToken');
        localStorage.removeItem('sellerRefreshToken');
        if (typeof window !== 'undefined') {
          window.location.href = '/seller/login';
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// Generic API request function for seller endpoints
async function sellerRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await sellerApiClient.request<T>(config);
  return response.data;
}

// ============ SELLER API ============
export const sellerApi = {
  // Dashboard & Overview
  getDashboardStats: () =>
    sellerRequest<{
      totalProducts: number;
      totalOrders: number;
      pendingOrders: number;
      totalRevenue: number;
      lowStockProducts: number;
      monthOrders: number;
      orderTrend: 'up' | 'down';
      earningsChange: string;
    }>({
      method: 'GET',
      url: '/v1/seller/dashboard/stats',
    }),

  // Product Management (Seller scoped)
  getProducts: (params?: { 
    page?: number; 
    limit?: number; 
    search?: string;
    status?: 'active' | 'inactive' | 'all';
    sortBy?: string;
    sortDir?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    if (params?.search) query.append('search', params.search);
    if (params?.status) query.append('status', params.status);
    if (params?.sortBy) query.append('sortBy', params.sortBy);
    if (params?.sortDir) query.append('sortDir', params.sortDir);

    return sellerRequest<{
      products: any[];
      total: number;
      page: number;
      totalPages: number;
    }>({
      method: 'GET',
      url: `/v1/seller/products?${query.toString()}`,
    });
  },

  createProduct: (productData: {
    name: string;
    description: string;
    sku: string;
    price: number;
    comparePrice?: number;
    costPrice?: number;
    stock: number;
    categoryId?: number;
    images: string[];
    tags?: string[];
    specifications?: any;
    isActive: boolean;
  }) =>
    sellerRequest<{ product: any }>({
      method: 'POST',
      url: '/v1/seller/products',
      data: productData,
    }),

  updateProduct: (productId: number, productData: Partial<{
    name: string;
    description: string;
    price: number;
    comparePrice?: number;
    costPrice?: number;
    stock: number;
    categoryId?: number;
    images: string[];
    tags?: string[];
    specifications?: any;
    isActive: boolean;
  }>) =>
    sellerRequest<{ product: any }>({
      method: 'PUT',
      url: `/v1/seller/products/${productId}`,
      data: productData,
    }),

  deleteProduct: (productId: number) =>
    sellerRequest<{ message: string }>({
      method: 'DELETE',
      url: `/v1/seller/products/${productId}`,
    }),

  updateProductStatus: (productId: number, status: 'active' | 'inactive') =>
    sellerRequest<{ product: any }>({
      method: 'PATCH',
      url: `/v1/seller/products/${productId}/status`,
      data: { status },
    }),

  uploadProductImages: (productId: number, formData: FormData) =>
    sellerRequest<{ images: any[] }>({
      method: 'POST',
      url: `/v1/seller/products/${productId}/images`,
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  // Order Management (Seller scoped - only orders with seller's products)
  getOrders: (params?: {
    page?: number;
    limit?: number;
    status?: 'pending' | 'processing' | 'shipped' | 'delivered' | 'cancelled';
    search?: string;
    dateFrom?: string;
    dateTo?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    if (params?.status) query.append('status', params.status);
    if (params?.search) query.append('search', params.search);
    if (params?.dateFrom) query.append('dateFrom', params.dateFrom);
    if (params?.dateTo) query.append('dateTo', params.dateTo);

    return sellerRequest<{
      orders: any[];
      total: number;
      page: number;
      totalPages: number;
    }>({
      method: 'GET',
      url: `/v1/seller/orders?${query.toString()}`,
    });
  },

  getOrderById: (orderId: number) =>
    sellerRequest<{ order: any }>({
      method: 'GET',
      url: `/v1/seller/orders/${orderId}`,
    }),

  updateOrderStatus: (orderId: number, status: {
    status: 'processing' | 'shipped';
    trackingNumber?: string;
    carrier?: string;
    notes?: string;
  }) =>
    sellerRequest<{ order: any }>({
      method: 'PUT',
      url: `/v1/seller/orders/${orderId}/status`,
      data: status,
    }),

  addTrackingInfo: (orderId: number, trackingData: {
    trackingNumber: string;
    carrier: string;
    estimatedDelivery?: string;
  }) =>
    sellerRequest<{ order: any }>({
      method: 'PUT',
      url: `/v1/seller/orders/${orderId}/tracking`,
      data: trackingData,
    }),

  // Analytics (Seller scoped)
  getSalesAnalytics: (params?: {
    period?: 'day' | 'week' | 'month' | 'year';
    dateFrom?: string;
    dateTo?: string;
  }) => {
    const query = new URLSearchParams();
    if (params?.period) query.append('period', params.period);
    if (params?.dateFrom) query.append('dateFrom', params.dateFrom);
    if (params?.dateTo) query.append('dateTo', params.dateTo);

    return sellerRequest<{
      salesData: Array<{
        date: string;
        revenue: number;
        orders: number;
        products: number;
      }>;
      totalRevenue: number;
      totalOrders: number;
      averageOrderValue: number;
      growth: {
        revenue: number;
        orders: number;
      };
    }>({
      method: 'GET',
      url: `/v1/seller/analytics/sales?${query.toString()}`,
    });
  },

  getProductAnalytics: () =>
    sellerRequest<{
      topProducts: Array<{
        id: number;
        name: string;
        sku: string;
        sales: number;
        revenue: number;
        views: number;
        conversionRate: number;
      }>;
      lowPerforming: Array<{
        id: number;
        name: string;
        sales: number;
        revenue: number;
        issues: string[];
      }>;
      categoryBreakdown: Array<{
        category: string;
        products: number;
        revenue: number;
        percentage: number;
      }>;
    }>({
      method: 'GET',
      url: '/v1/seller/analytics/products',
    }),

  // Inventory Management
  getInventory: () =>
    sellerRequest<{
      products: Array<{
        id: number;
        name: string;
        sku: string;
        currentStock: number;
        reorderPoint: number;
        status: 'in-stock' | 'low-stock' | 'out-of-stock';
        lastUpdated: string;
      }>;
      lowStock: number;
      outOfStock: number;
    }>({
      method: 'GET',
      url: '/v1/seller/inventory',
    }),

  updateInventory: (productId: number, stockData: {
    stock: number;
    reorderPoint?: number;
    notes?: string;
  }) =>
    sellerRequest<{ product: any }>({
      method: 'PUT',
      url: `/v1/seller/inventory/${productId}`,
      data: stockData,
    }),

  // Payouts & Earnings
  getPayouts: (params?: { page?: number; limit?: number }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());

    return sellerRequest<{
      payouts: Array<{
        id: number;
        amount: number;
        status: 'pending' | 'processing' | 'completed' | 'failed';
        period: string;
        created: string;
        processed?: string;
        method: string;
        details?: string;
      }>;
      total: number;
      page: number;
      totalPages: number;
    }>({
      method: 'GET',
      url: `/v1/seller/payouts?${query.toString()}`,
    });
  },

  getEarningsSummary: () =>
    sellerRequest<{
      totalEarnings: number;
      availableBalance: number;
      pendingAmount: number;
      lastPayout?: number;
      nextPayoutDate?: string;
      currentMonthEarnings: number;
      previousMonthEarnings: number;
      growth: number;
    }>({
      method: 'GET',
      url: '/v1/seller/earnings/summary',
    }),

  requestPayout: (payoutData: {
    amount: number;
    method: string;
    bankDetails?: any;
    mobileMoneyDetails?: any;
  }) =>
    sellerRequest<{ payout: any }>({
      method: 'POST',
      url: '/v1/seller/payouts/request',
      data: payoutData,
    }),

  // Reviews (Seller's products only)
  getReviews: (params?: { 
    page?: number; 
    limit?: number; 
    rating?: number;
    productId?: number;
  }) => {
    const query = new URLSearchParams();
    if (params?.page) query.append('page', params.page.toString());
    if (params?.limit) query.append('limit', params.limit.toString());
    if (params?.rating) query.append('rating', params.rating.toString());
    if (params?.productId) query.append('productId', params.productId.toString());

    return sellerRequest<{
      reviews: Array<{
        id: number;
        productId: number;
        productName: string;
        customerName: string;
        rating: number;
        title: string;
        comment: string;
        verified: boolean;
        helpfulCount: number;
        createdAt: string;
        sellerResponse?: string;
      }>;
      total: number;
      page: number;
      totalPages: number;
      averageRating: number;
      ratingDistribution: Array<{
        rating: number;
        count: number;
      }>;
    }>({
      method: 'GET',
      url: `/v1/seller/reviews?${query.toString()}`,
    });
  },

  respondToReview: (reviewId: number, response: string) =>
    sellerRequest<{ review: any }>({
      method: 'POST',
      url: `/v1/seller/reviews/${reviewId}/respond`,
      data: { response },
    }),

  // Store Profile & Settings
  getStoreProfile: () =>
    sellerRequest<{
      store: {
        id: number;
        name: string;
        description: string;
        logo: string;
        banner: string;
        contactEmail: string;
        phoneNumber: string;
        address: string;
        businessHours: any;
        socialLinks: any;
        verificationStatus: 'pending' | 'verified' | 'rejected';
        verificationDocuments?: any[];
      };
    }>({
      method: 'GET',
      url: '/v1/seller/store/profile',
    }),

  updateStoreProfile: (storeData: Partial<{
        name: string;
        description: string;
        contactEmail: string;
        phoneNumber: string;
        address: string;
        businessHours: any;
        socialLinks: any;
      }>) =>
    sellerRequest<{ store: any }>({
      method: 'PUT',
      url: '/v1/seller/store/profile',
      data: storeData,
    }),

  uploadStoreLogo: (formData: FormData) =>
    sellerRequest<{ logo: string }>({
      method: 'POST',
      url: '/v1/seller/store/logo',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  uploadStoreBanner: (formData: FormData) =>
    sellerRequest<{ banner: string }>({
      method: 'POST',
      url: '/v1/seller/store/banner',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
};