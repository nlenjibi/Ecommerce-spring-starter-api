// ==================== API Configuration ====================
// Normalize API base URL to avoid duplicated '/api'
const _rawApiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:9190';
export const API_BASE_URL = (() => {
  try {
    let u = _rawApiBase.trim();
    if (u.endsWith('/')) u = u.slice(0, -1);
    if (u.toLowerCase().endsWith('/api')) return u;
    return `${u}/api`;
  } catch (e) {
    console.error('[Constants] Failed to parse API_BASE_URL:', e);
    return 'http://localhost:9190/api';
  }
})();

export const STRIPE_PUBLIC_KEY = process.env.NEXT_PUBLIC_STRIPE_KEY || '';

// ==================== App Constants ====================
export const APP_NAME = 'ShopHub';
export const APP_DESCRIPTION = 'Your one-stop destination for all your shopping needs';

// ==================== Pagination ====================
export const DEFAULT_PAGE_SIZE = 12;
export const ADMIN_PAGE_SIZE = 10;

// ==================== Order Statuses ====================
export const ORDER_STATUSES = {
  PENDING: 'pending',
  CONFIRMED: 'confirmed',
  PROCESSING: 'processing',
  SHIPPED: 'shipped',
  DELIVERED: 'delivered',
  CANCELLED: 'cancelled',
  REFUNDED: 'refunded',
} as const;

export const ORDER_STATUS_COLORS: Record<string, string> = {
  pending: 'bg-yellow-100 text-yellow-800',
  confirmed: 'bg-blue-100 text-blue-800',
  processing: 'bg-indigo-100 text-indigo-800',
  shipped: 'bg-purple-100 text-purple-800',
  delivered: 'bg-green-100 text-green-800',
  cancelled: 'bg-red-100 text-red-800',
  refunded: 'bg-gray-100 text-gray-800',
};

// ==================== User Roles ====================
export const USER_ROLES = {
  USER: 'user',
  ADMIN: 'admin',
} as const;

// ==================== Shipping ====================
export const FREE_SHIPPING_THRESHOLD = 50;
export const SHIPPING_COST = 9.99;
export const TAX_RATE = 0.08;

// ==================== Date Formats ====================
export const DATE_FORMAT = 'MMM dd, yyyy';
export const DATETIME_FORMAT = 'MMM dd, yyyy HH:mm';

// ==================== Chart Colors ====================
export const CHART_COLORS = {
  primary: '#3b82f6',
  secondary: '#10b981',
  warning: '#f59e0b',
  danger: '#ef4444',
  purple: '#8b5cf6',
  pink: '#ec4899',
};

// ==================== Wishlist Constants ====================
export const WISHLIST_PRIORITIES = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  URGENT: 'URGENT',
} as const;

export const WISHLIST_PRIORITY_COLORS: Record<string, string> = {
  LOW: 'bg-gray-100 text-gray-800',
  MEDIUM: 'bg-yellow-100 text-yellow-800',
  HIGH: 'bg-orange-100 text-orange-800',
  URGENT: 'bg-red-100 text-red-800',
};

export const WISHLIST_PRIORITY_WEIGHTS: Record<string, number> = {
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  URGENT: 4,
};

export const WISHLIST_SORT_OPTIONS = [
  { value: 'date-added', label: 'Date Added' },
  { value: 'price-low', label: 'Price: Low to High' },
  { value: 'price-high', label: 'Price: High to Low' },
  { value: 'priority', label: 'Priority' },
  { value: 'savings', label: 'Biggest Savings' },
  { value: 'name', label: 'Name A-Z' },
] as const;

// ==================== Storage Keys ====================
export const STORAGE_KEYS = {
  GUEST_SESSION_ID: 'wishlist_guest_session_id',
  GUEST_WISHLIST: 'wishlist_guest_items',
  WISHLIST_FILTERS: 'wishlist_filters',
  WISHLIST_VIEW_MODE: 'wishlist_view_mode',
  CART_ITEMS: 'cart_items',
  AUTH_TOKEN: 'auth_token',
  REFRESH_TOKEN: 'refresh_token',
  USER_DATA: 'user_data',
} as const;

// ==================== Toast Messages ====================
export const TOAST_MESSAGES = {
  WISHLIST: {
    ADDED: 'Added to wishlist',
    REMOVED: 'Removed from wishlist',
    UPDATED: 'Wishlist item updated',
    CLEARED: 'Wishlist cleared',
    MERGED: 'Guest wishlist merged successfully',
    MOVE_TO_CART_SUCCESS: 'Moved to cart',
    MOVE_TO_CART_ERROR: 'Failed to move to cart',
    MARK_PURCHASED_SUCCESS: 'Marked as purchased',
    MARK_PURCHASED_ERROR: 'Failed to mark as purchased',
  },
  CART: {
    ADDED: 'Added to cart',
    REMOVED: 'Removed from cart',
    UPDATED: 'Cart updated',
    CLEARED: 'Cart cleared',
  },
  AUTH: {
    LOGIN_SUCCESS: 'Login successful',
    LOGIN_ERROR: 'Login failed',
    LOGOUT_SUCCESS: 'Logged out successfully',
    REGISTER_SUCCESS: 'Registration successful',
    REGISTER_ERROR: 'Registration failed',
  },
  ERROR: {
    GENERIC: 'Something went wrong',
    NETWORK: 'Network error. Please check your connection',
    UNAUTHORIZED: 'Please login to continue',
    FORBIDDEN: 'You don\'t have permission to do that',
    NOT_FOUND: 'Resource not found',
  },
} as const;

// ==================== Validation Rules ====================
export const VALIDATION = {
  EMAIL_REGEX: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PHONE_REGEX: /^[\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?[0-9]{4,6}$/,
  PASSWORD_MIN_LENGTH: 8,
  USERNAME_MIN_LENGTH: 3,
  USERNAME_MAX_LENGTH: 30,
  NOTES_MAX_LENGTH: 500,
  PRODUCT_NAME_MAX_LENGTH: 200,
} as const;

// ==================== Product Filters ====================
export const PRODUCT_SORT_OPTIONS = [
  { value: 'newest', label: 'Newest First' },
  { value: 'price-low', label: 'Price: Low to High' },
  { value: 'price-high', label: 'Price: High to Low' },
  { value: 'name', label: 'Name A-Z' },
  { value: 'popular', label: 'Most Popular' },
  { value: 'rating', label: 'Highest Rated' },
] as const;

export const PRICE_RANGES = [
  { min: 0, max: 50, label: 'Under $50' },
  { min: 50, max: 100, label: '$50 - $100' },
  { min: 100, max: 200, label: '$100 - $200' },
  { min: 200, max: 500, label: '$200 - $500' },
  { min: 500, max: Infinity, label: 'Over $500' },
] as const;

export const RATING_FILTERS = [
  { value: 4, label: '4★ & above' },
  { value: 3, label: '3★ & above' },
  { value: 2, label: '2★ & above' },
  { value: 1, label: '1★ & above' },
] as const;

// ==================== Animation & Timing ====================
export const ANIMATION_DURATION = 200; // milliseconds
export const DEBOUNCE_DELAY = 300; // milliseconds
export const TOAST_DURATION = 3000; // milliseconds

// ==================== Retry Configuration ====================
export const RETRY_CONFIG = {
  MAX_RETRIES: 3,
  INITIAL_DELAY: 1000,
  MAX_DELAY: 10000,
  BACKOFF_MULTIPLIER: 2,
} as const;

// ==================== Guest Wishlist ====================
export const GUEST_WISHLIST = {
  SESSION_EXPIRY_DAYS: 30,
  MAX_ITEMS: 100,
  MERGE_ON_LOGIN: true,
  PERSIST_TO_LOCAL_STORAGE: true,
} as const;

// ==================== Feature Flags ====================
export const FEATURES = {
  ENABLE_WISHLIST_SHARING: true,
  ENABLE_PRICE_TRACKING: true,
  ENABLE_PRICE_ALERTS: true,
  ENABLE_GUEST_WISHLIST: true,
  ENABLE_WISHLIST_COLLECTIONS: true,
  ENABLE_WISHLIST_ANALYTICS: true,
  ENABLE_PRODUCT_RECOMMENDATIONS: true,
  ENABLE_WISHLIST_IMPORT_EXPORT: true,
} as const;