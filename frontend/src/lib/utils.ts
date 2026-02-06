import { API_BASE_URL } from './constants';

// ==================== Image Utilities ====================

/**
 * Constructs a full image URL from a relative path returned by the server
 * @param imagePath - The image path returned by the server
 * @returns Full URL that can be used with Next.js Image component
 */
export const getImageUrl = (imagePath: string | null | undefined): string => {
  if (!imagePath) return '/placeholder.svg';
  
  // If it's the placeholder, return as-is
  if (imagePath === '/placeholder.svg') return imagePath;
  
  // If it's already a full URL (starts with http), return as is
  if (imagePath.startsWith('http')) {
    return imagePath;
  }
  
  // If it's a relative path, construct the full URL
  // Remove the /api part if present to get the server base URL
  const serverBaseUrl = API_BASE_URL.replace('/api', '');
  
  // Ensure the path doesn't start with a slash
  const cleanPath = imagePath.startsWith('/') ? imagePath.slice(1) : imagePath;
  
  return `${serverBaseUrl}/${cleanPath}`;
};

/**
 * Checks if an image URL is valid and accessible
 * @param url - The image URL to check
 * @returns Promise<boolean> - Whether the image is accessible
 */
export const isImageAccessible = async (url: string): Promise<boolean> => {
  try {
    const response = await fetch(url, { method: 'HEAD' });
    return response.ok && (response.headers.get('content-type')?.startsWith('image/') || false);
  } catch {
    return false;
  }
};

/**
 * Get optimal image dimensions for lazy loading
 * @param containerWidth - Width of the container
 * @param containerHeight - Height of the container
 * @returns Object with width and height
 */
export const getOptimalImageDimensions = (
  containerWidth: number,
  containerHeight: number
): { width: number; height: number } => {
  // Use device pixel ratio for better quality on retina displays
  const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1;
  return {
    width: Math.ceil(containerWidth * dpr),
    height: Math.ceil(containerHeight * dpr),
  };
};

// ==================== String Utilities ====================

/**
 * Capitalize first letter of a string
 */
export const capitalize = (str: string): string => {
  if (!str) return '';
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
};

/**
 * Convert string to title case
 */
export const toTitleCase = (str: string): string => {
  if (!str) return '';
  return str
    .split(' ')
    .map(word => capitalize(word))
    .join(' ');
};

/**
 * Truncate string to specified length
 */
export const truncate = (str: string, length: number, suffix: string = '...'): string => {
  if (!str || str.length <= length) return str;
  return str.substring(0, length - suffix.length) + suffix;
};

/**
 * Convert string to URL-friendly slug
 */
export const slugify = (str: string): string => {
  return str
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '')
    .replace(/[\s_-]+/g, '-')
    .replace(/^-+|-+$/g, '');
};

/**
 * Strip HTML tags from string
 */
export const stripHtml = (html: string): string => {
  if (typeof window === 'undefined') {
    return html.replace(/<[^>]*>/g, '');
  }
  const tmp = document.createElement('DIV');
  tmp.innerHTML = html;
  return tmp.textContent || tmp.innerText || '';
};

// ==================== Number Utilities ====================

/**
 * Format number as currency
 */
export const formatCurrency = (
  amount: number,
  currency: string = 'USD',
  locale: string = 'en-US'
): string => {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
  }).format(amount);
};

/**
 * Format large numbers with K, M, B suffixes
 */
export const formatNumber = (num: number): string => {
  if (num >= 1000000000) return (num / 1000000000).toFixed(1) + 'B';
  if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
  return num.toString();
};

/**
 * Calculate percentage
 */
export const calculatePercentage = (part: number, whole: number): number => {
  if (whole === 0) return 0;
  return (part / whole) * 100;
};

/**
 * Calculate discount percentage
 */
export const calculateDiscountPercentage = (originalPrice: number, discountPrice: number): number => {
  if (originalPrice === 0) return 0;
  return ((originalPrice - discountPrice) / originalPrice) * 100;
};

/**
 * Clamp number between min and max
 */
export const clamp = (value: number, min: number, max: number): number => {
  return Math.min(Math.max(value, min), max);
};

// ==================== Date Utilities ====================

/**
 * Format date to readable string
 */
export const formatDate = (
  date: string | Date,
  format: 'short' | 'long' | 'full' = 'short'
): string => {
  const d = new Date(date);
  const options: Intl.DateTimeFormatOptions = {
    short: { year: 'numeric', month: 'short', day: 'numeric' },
    long: { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' },
    full: { 
      weekday: 'long', 
      year: 'numeric', 
      month: 'long', 
      day: 'numeric', 
      hour: '2-digit', 
      minute: '2-digit' 
    },
  }[format];
  
  return d.toLocaleDateString('en-US', options);
};

/**
 * Get relative time string (e.g., "2 hours ago")
 */
export const formatRelativeTime = (date: string | Date): string => {
  const now = new Date();
  const past = new Date(date);
  const seconds = Math.floor((now.getTime() - past.getTime()) / 1000);

  const intervals = {
    year: 31536000,
    month: 2592000,
    week: 604800,
    day: 86400,
    hour: 3600,
    minute: 60,
    second: 1,
  };

  for (const [unit, secondsInUnit] of Object.entries(intervals)) {
    const interval = Math.floor(seconds / secondsInUnit);
    if (interval >= 1) {
      return `${interval} ${unit}${interval !== 1 ? 's' : ''} ago`;
    }
  }

  return 'just now';
};

/**
 * Check if date is in the past
 */
export const isPast = (date: string | Date): boolean => {
  return new Date(date).getTime() < Date.now();
};

/**
 * Check if date is today
 */
export const isToday = (date: string | Date): boolean => {
  const d = new Date(date);
  const today = new Date();
  return (
    d.getDate() === today.getDate() &&
    d.getMonth() === today.getMonth() &&
    d.getFullYear() === today.getFullYear()
  );
};

// ==================== Array Utilities ====================

/**
 * Get unique values from array
 */
export const unique = <T>(array: T[]): T[] => {
  return Array.from(new Set(array));
};

/**
 * Group array by key
 */
export const groupBy = <T>(array: T[], key: keyof T): Record<string, T[]> => {
  return array.reduce((result, item) => {
    const groupKey = String(item[key]);
    if (!result[groupKey]) {
      result[groupKey] = [];
    }
    result[groupKey].push(item);
    return result;
  }, {} as Record<string, T[]>);
};

/**
 * Chunk array into smaller arrays
 */
export const chunk = <T>(array: T[], size: number): T[][] => {
  const chunks: T[][] = [];
  for (let i = 0; i < array.length; i += size) {
    chunks.push(array.slice(i, i + size));
  }
  return chunks;
};

/**
 * Shuffle array randomly
 */
export const shuffle = <T>(array: T[]): T[] => {
  const shuffled = [...array];
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
  }
  return shuffled;
};

// ==================== Object Utilities ====================

/**
 * Deep clone an object
 */
export const deepClone = <T>(obj: T): T => {
  return JSON.parse(JSON.stringify(obj));
};

/**
 * Check if object is empty
 */
export const isEmpty = (obj: object): boolean => {
  return Object.keys(obj).length === 0;
};

/**
 * Pick specific properties from object
 */
export const pick = <T extends object, K extends keyof T>(
  obj: T,
  keys: K[]
): Pick<T, K> => {
  const result = {} as Pick<T, K>;
  keys.forEach(key => {
    if (key in obj) {
      result[key] = obj[key];
    }
  });
  return result;
};

/**
 * Omit specific properties from object
 */
export const omit = <T extends object, K extends keyof T>(
  obj: T,
  keys: K[]
): Omit<T, K> => {
  const result = { ...obj };
  keys.forEach(key => {
    delete result[key];
  });
  return result as Omit<T, K>;
};

// ==================== Validation Utilities ====================

/**
 * Validate email address
 */
export const isValidEmail = (email: string): boolean => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
};

/**
 * Validate phone number
 */
export const isValidPhone = (phone: string): boolean => {
  const regex = /^[\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?[0-9]{4,6}$/;
  return regex.test(phone);
};

/**
 * Validate URL
 */
export const isValidUrl = (url: string): boolean => {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
};

/**
 * Validate credit card number (Luhn algorithm)
 */
export const isValidCreditCard = (cardNumber: string): boolean => {
  const digits = cardNumber.replace(/\D/g, '');
  if (digits.length < 13 || digits.length > 19) return false;

  let sum = 0;
  let isEven = false;

  for (let i = digits.length - 1; i >= 0; i--) {
    let digit = parseInt(digits[i]);

    if (isEven) {
      digit *= 2;
      if (digit > 9) digit -= 9;
    }

    sum += digit;
    isEven = !isEven;
  }

  return sum % 10 === 0;
};

// ==================== Async Utilities ====================

/**
 * Delay execution
 */
export const delay = (ms: number): Promise<void> => {
  return new Promise(resolve => setTimeout(resolve, ms));
};

/**
 * Retry async function with exponential backoff
 */
export const retry = async <T>(
  fn: () => Promise<T>,
  maxRetries: number = 3,
  delayMs: number = 1000
): Promise<T> => {
  let lastError: Error;

  for (let i = 0; i < maxRetries; i++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error as Error;
      if (i < maxRetries - 1) {
        await delay(delayMs * Math.pow(2, i));
      }
    }
  }

  throw lastError!;
};

/**
 * Debounce function
 */
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout | null = null;

  return (...args: Parameters<T>) => {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
};

/**
 * Throttle function
 */
export const throttle = <T extends (...args: any[]) => any>(
  func: T,
  limit: number
): ((...args: Parameters<T>) => void) => {
  let inThrottle: boolean = false;

  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
};

// ==================== Storage Utilities ====================

/**
 * Safely get item from localStorage
 */
export const getFromStorage = <T>(key: string, defaultValue: T): T => {
  if (typeof window === 'undefined') return defaultValue;

  try {
    const item = window.localStorage.getItem(key);
    return item ? JSON.parse(item) : defaultValue;
  } catch (error) {
    console.error(`Error reading from localStorage (${key}):`, error);
    return defaultValue;
  }
};

/**
 * Safely set item in localStorage
 */
export const setInStorage = <T>(key: string, value: T): boolean => {
  if (typeof window === 'undefined') return false;

  try {
    window.localStorage.setItem(key, JSON.stringify(value));
    return true;
  } catch (error) {
    console.error(`Error writing to localStorage (${key}):`, error);
    return false;
  }
};

/**
 * Safely remove item from localStorage
 */
export const removeFromStorage = (key: string): boolean => {
  if (typeof window === 'undefined') return false;

  try {
    window.localStorage.removeItem(key);
    return true;
  } catch (error) {
    console.error(`Error removing from localStorage (${key}):`, error);
    return false;
  }
};

// ==================== URL Utilities ====================

/**
 * Build URL with query parameters
 */
export const buildUrl = (base: string, params: Record<string, any>): string => {
  const url = new URL(base, window.location.origin);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      url.searchParams.append(key, String(value));
    }
  });
  return url.toString();
};

/**
 * Parse query parameters from URL
 */
export const parseQueryParams = (url: string): Record<string, string> => {
  const params: Record<string, string> = {};
  const searchParams = new URL(url, window.location.origin).searchParams;
  searchParams.forEach((value, key) => {
    params[key] = value;
  });
  return params;
};

// ==================== Browser Utilities ====================

/**
 * Check if code is running in browser
 */
export const isBrowser = (): boolean => {
  return typeof window !== 'undefined';
};

/**
 * Copy text to clipboard
 */
export const copyToClipboard = async (text: string): Promise<boolean> => {
  if (!isBrowser()) return false;

  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch (error) {
    console.error('Failed to copy to clipboard:', error);
    return false;
  }
};

/**
 * Download file from blob
 */
export const downloadFile = (blob: Blob, filename: string): void => {
  if (!isBrowser()) return;

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

/**
 * Get current scroll position
 */
export const getScrollPosition = (): { x: number; y: number } => {
  if (!isBrowser()) return { x: 0, y: 0 };

  return {
    x: window.pageXOffset || document.documentElement.scrollLeft,
    y: window.pageYOffset || document.documentElement.scrollTop,
  };
};

/**
 * Scroll to element smoothly
 */
export const scrollToElement = (
  elementId: string,
  options?: ScrollIntoViewOptions
): void => {
  if (!isBrowser()) return;

  const element = document.getElementById(elementId);
  if (element) {
    element.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
      ...options,
    });
  }
};

// ==================== Random Utilities ====================

/**
 * Generate random ID
 */
export const generateId = (prefix: string = 'id'): string => {
  return `${prefix}_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Generate UUID v4
 */
export const generateUUID = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};