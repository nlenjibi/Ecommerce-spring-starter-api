// Product Stock Status Enum
export enum StockStatus {
  IN_STOCK = 'IN_STOCK',
  OUT_OF_STOCK = 'OUT_OF_STOCK',
  LOW_STOCK = 'LOW_STOCK',
}

// Product Fulfillment Type Enum
export enum FulfillmentType {
  SHIPPED = 'SHIPPED',
  IN_STORE_PICKUP = 'IN_STORE_PICKUP',
  EXPRESS_DELIVERY = 'EXPRESS_DELIVERY',
  INTERNATIONAL_SHIPPING = 'INTERNATIONAL_SHIPPING',
}

// Legacy Availability Type Enum (for backward compatibility)
export enum AvailabilityType {
  SHIPPED = 'SHIPPED',
  IN_STORE = 'IN_STORE',
}

// Product Condition Enum
export enum ProductCondition {
  NEW = 'NEW',
  LIKE_NEW = 'LIKE_NEW',
  REFURBISHED = 'REFURBISHED',
  USED = 'USED',
}

// Seller Type Enum
export enum SellerType {
  OFFICIAL = 'OFFICIAL',
  VERIFIED = 'VERIFIED',
  THIRD_PARTY = 'THIRD_PARTY',
}

// Promotion Type Enum
export enum PromotionType {
  DISCOUNT = 'DISCOUNT',
  FLASH_SALE = 'FLASH_SALE',
  FREE_SHIPPING = 'FREE_SHIPPING',
}

// Seller Info Interface
export interface SellerInfo {
  id: number;
  name: string;
  type: SellerType;
  rating: number;
  totalReviews: number;
  isVerified: boolean;
  responseTime?: string;
  returnRate?: number;
}

// Promotion Interface
export interface Promotion {
  id: number;
  type: PromotionType;
  discountPercentage?: number;
  discountAmount?: number;
  startDate?: string;
  endDate?: string;
  minPurchase?: number;
  maxDiscount?: number;
  code?: string;
}

// Product Types
export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  effectivePrice?: number;
  discountPrice?: number;
  originalPrice?: number;
  compareAtPrice?: number;
  imageUrl?: string | null;
  image?: string; // for backward compatibility
  images?: string[];
  category?: {
    id: number;
    slug: string;
    name: string;
  } | null;
  categoryName?: string; // for backward compatibility
  categoryId?: number | null;
  sku?: string;
  slug?: string;
  stockQuantity?: number;
  stock?: number; // for backward compatibility
  inStock?: boolean;
  stockStatus?: StockStatus; // IN_STOCK, OUT_OF_STOCK, or LOW_STOCK
  fulfillmentType?: FulfillmentType; // Delivery method
  condition?: ProductCondition; // NEW, LIKE_NEW, REFURBISHED, USED
  seller?: SellerInfo; // Seller information
  promotion?: Promotion; // Active promotion
  discountPercentage?: number; // Calculated discount percentage
  rating?: number;
  reviews?: number;
  isTrending?: boolean; // Customer signals
  isMostPurchased?: boolean;
  isRecommendedForYou?: boolean;
  isRecentlyViewed?: boolean;
  featured?: boolean;
  isActive?: boolean;
  isFeatured?: boolean;
  tags?: string[];
  createdAt?: string;
  updatedAt?: string;
}

// User Types
export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  // Added 'customer' so middleware and UI can distinguish customers
  role: 'user' | 'admin' | 'seller' | 'customer';
  avatar?: string;
  phone?: string;
  createdAt?: string;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
}

// Cart Types
export interface CartItem {
  id: number;
  product: Product;
  quantity: number;
}

export interface CartState {
  items: CartItem[];
  total: number;
  itemCount: number;
  loading?: boolean;
}

// Delivery Method Types
export enum DeliveryMethod {
  BUS_STATION = 'BUS_STATION',
  DIRECT_ADDRESS = 'DIRECT_ADDRESS',
  HOME_DELIVERY = 'HOME_DELIVERY',
  SHIPPING = 'SHIPPING',
}

export interface Region {
  id: number;
  name: string;
  code: string;
  country: string;
  isActive: boolean;
}

export interface Town {
  id: number;
  regionId: number;
  name: string;
  code: string;
  isActive: boolean;
}

export interface BusStation {
  id: number;
  townId: number;
  name: string;
  address: string;
  phone?: string;
  pickupInstructions?: string;
  isActive: boolean;
}

export interface DeliveryFee {
  id: number;
  townId?: number;
  method: DeliveryMethod;
  baseFee: number;
  perKmFee?: number;
  estimatedDays: number;
  isActive: boolean;
}

export interface UserAddress {
  id: number;
  userId?: number;
  label: string;
  recipientName: string;
  phone: string;
  street: string;
  city: string;
  region: string;
  country: string;
  postalCode?: string;
  isDefault: boolean;
  createdAt?: string;
}

export interface DeliveryDetails {
  method: DeliveryMethod;
  busStationId?: number;
  busStationName?: string;
  addressId?: number;
  region?: string;
  town?: string;
  street?: string;
  phone?: string;
  fee: number;
  estimatedDays: number;
}

// Order Types
export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  productImageUrl: string;
  totalPrice: number;
}

export interface ShippingAddress {
  firstName: string;
  lastName: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  phone: string;
}

export interface OrderTimeline {
  status: string;
  timestamp: string;
  description: string;
  location?: string;
}

// New Order Create Request Interface
export interface CreateOrderRequest {
  items: {
    productId: number;
    quantity: number;
  }[];
  shippingMethod: DeliveryMethod;
  paymentMethod: string;
  customerEmail: string;
  customerName: string;
  couponCode?: string;
  taxRate: number;
  customerNotes?: string;
  couponDiscount?: number;
  // Delivery-specific fields
  busStationId?: number;
  addressId?: number;
  shippingAddress?: any;
}

// New Order Create Response Interface
export interface CreateOrderResponse {
  success: boolean;
  message: string;
  data: {
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
    shippedAt?: string;
    deliveredAt?: string;
    estimatedDeliveryDate?: string;
    items: OrderItem[];
    itemCount: number;
    customerNotes?: string;
  };
}

// Coupon Interface
export interface Coupon {
  code: string;
  discountPercentage?: number;
  discountAmount?: number;
  minPurchase?: number;
  maxDiscount?: number;
  isValid: boolean;
  message?: string;
}

// Tax Configuration Interface
export interface TaxConfiguration {
  rate: number;
  description: string;
}

// Updated Order Interface to match new API response
export interface Order {
  id: number;
  orderNumber?: string;
  userId?: number;
  items: OrderItem[];
  customerEmail: string;
  customerName: string;
  shippingMethod: string;
  paymentMethod: string;
  subtotal: number;
  deliveryFee: number;
  shippingCost: number;
  tax: number;
  taxRate: number;
  couponDiscount?: number;
  total: number;
  totalAmount?: number;
  status: string;
  paymentStatus: string;
  customerNotes?: string;
  couponCode?: string;
  shippingAddress?: ShippingAddress;
  deliveryDetails?: DeliveryDetails;
  timeline?: OrderTimeline[];
  trackingNumber?: string;
  carrier?: string;
  orderDate: string;
  shippedAt?: string;
  deliveredAt?: string;
  estimatedDeliveryDate?: string;
  createdAt: string;
  updatedAt?: string;
}

// Category Types
export interface Category {
  id: number;
  name: string;
  slug: string;
  image?: string;
  description?: string;
}

// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface PaginatedResponse<T> {
  success: boolean;
  data: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

// Form Types
export interface LoginFormData {
  email: string;
  password: string;
}

export interface RegisterFormData {
  username?: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  password: string;
  confirmPassword?: string;
} 

export interface CheckoutFormData {
  shippingAddress: ShippingAddress;
  paymentMethod: string;
  cardNumber?: string;
  cardExpiry?: string;
  cardCvc?: string;
}

// Wishlist Types
export interface WishlistItem {
  id: number;
  product: Product;
  addedAt: string;
}

export interface WishlistState {
  items: WishlistItem[];
  itemCount: number;
}

// Contact Types
export interface ContactMessage {
  id: string;
  name: string;
  email: string;
  subject: string;
  message: string;
  status: 'new' | 'read' | 'archived';
  createdAt: string;
  updatedAt?: string;
}

export interface ContactDetails {
  id: number;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  phone: string;
  email: string;
  facebook?: string;
  twitter?: string;
  instagram?: string;
  linkedin?: string;
  visible: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Subscriber Types
export interface Subscriber {
  email: string;
  status: 'active' | 'unsubscribed';
  subscribedAt: string;
  unsubscribedAt?: string;
}

// Legal Document Types
export interface LegalDocument {
  id: number;
  type: 'privacy-policy' | 'terms-of-service';
  title: string;
  content: string;
  version: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

// Social Media Platform Enum (Fixed platforms, admin manages URLs only)
export enum SocialPlatform {
  X = 'X',
  FACEBOOK = 'FACEBOOK',
  INSTAGRAM = 'INSTAGRAM',
  TELEGRAM = 'TELEGRAM',
  TIKTOK = 'TIKTOK',
  YOUTUBE = 'YOUTUBE',
  WHATSAPP = 'WHATSAPP',
}

// Social Links Types (Fixed platforms with admin-assigned URLs)
export interface SocialLink {
  id: number;
  platform: SocialPlatform; // Fixed enum: X, FACEBOOK, INSTAGRAM, TELEGRAM, TIKTOK, YOUTUBE, WHATSAPP
  url: string | null; // Admin-assigned URL, null if not yet assigned
  isActive: boolean; // Toggle visibility
  createdAt?: string;
  updatedAt?: string;
}

// App Download Links (Fixed platforms for app stores)
export enum AppPlatform {
  APPLE_APP_STORE = 'APPLE_APP_STORE',
  GOOGLE_PLAY_STORE = 'GOOGLE_PLAY_STORE',
}

export interface AppDownloadLink {
  id: number;
  platform: AppPlatform;
  url: string | null; // Admin-assigned URL, null if not yet assigned
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Review Approval Status Enum
export enum ReviewStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

// Rating/Review Interfaces
export interface UserReview {
  id: number;
  productId: number;
  userId: number;
  userName: string;
  userEmail?: string;
  userAvatar?: string;
  rating: number; // 1-5
  title: string;
  comment: string;
  status: ReviewStatus; // PENDING, APPROVED, REJECTED
  isVerifiedPurchase: boolean;
  helpfulCount: number;
  unhelpfulCount: number;
  images?: string[]; // Review images
  adminResponse?: string; // Admin/seller response to the review
  createdAt: string;
  updatedAt?: string;
  rejectionReason?: string; // Admin field for rejected reviews
}

export interface ProductRating {
  productId: number;
  averageRating: number; // 1-5
  totalReviews: number;
  distributionByRating: {
    5: number;
    4: number;
    3: number;
    2: number;
    1: number;
  };
}

export interface CreateReviewPayload {
  productId: number;
  rating: number;
  title: string;
  comment: string;
  images?: string[];
}

// Help & Support Interfaces
export interface HelpLink {
  id: number;
  title: string;
  description?: string;
  url: string;
  icon?: string; // Icon name from lucide-react
  category: 'contact' | 'live-chat' | 'help' | 'tracking' | 'returns' | 'faq';
  displayOrder: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ChatConfig {
  id: number;
  type: 'whatsapp' | 'facebook' | 'telegram' | 'custom_link' | 'live_chat';
  title: string;
  url: string; // WhatsApp link, Facebook messenger link, etc.
  displayText?: string;
  icon?: string;
  displayOrder: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface LiveSupportSettings {
  helpLinks: HelpLink[];
  chatConfigs: ChatConfig[];
  floatingButtonEnabled: boolean;
  floatingButtonPosition: 'bottom-left' | 'bottom-right';
}
