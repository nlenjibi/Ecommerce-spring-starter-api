// Review Status Enum
export enum ReviewStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

// Sort Option Interface
export interface SortOption {
  value: string;
  label: string;
}

// Review Filters Interface
export interface ReviewFilters {
  rating?: number;
  verified?: boolean;
  hasImages?: boolean;
  status?: ReviewStatus;
  userId?: number;
  sortBy?: 'recent' | 'helpful' | 'rating-high' | 'rating-low' | 'verified';
}

// Create Review Data Interface
export interface CreateReviewData {
  productId: number;
  rating: number;
  title: string;
  comment: string;
  recommended?: boolean;
  images?: string[];
}

// Update Review Data Interface
export interface UpdateReviewData {
  rating?: number;
  title?: string;
  comment?: string;
  recommended?: boolean;
  images?: string[];
}

// Review Interface
export interface Review {
  id: number;
  productId: number;
  userId: number;
  userName: string;
  userEmail?: string;
  userAvatar?: string;
  rating: number; // 1-5
  title?: string;
  comment: string;
  status: ReviewStatus; // PENDING, APPROVED, REJECTED
  verified: boolean; // Verified purchase
  helpfulCount: number;
  userMarkedHelpful?: boolean; // Current user's helpful vote
  userReported?: boolean; // Current user reported this review
  recommended?: boolean; // User recommends the product
  images?: string[]; // Review images
  adminResponse?: string; // Admin/seller response to review
  adminResponseDate?: string; // When admin responded
  rejectionReason?: string; // Admin field for rejected reviews
  createdAt: string | Date;
  updatedAt?: string | Date;
}

// Review Statistics Interface
export interface ReviewStats {
  totalReviews: number;
  averageRating: number; // 1-5
  ratingDistribution: {
    5: number;
    4: number;
    3: number;
    2: number;
    1: number;
  };
  verifiedPurchaseReviews: number;
  recommendedCount: number;
  notRecommendedCount: number;
}

// Review List Response Interface
export interface ReviewListResponse {
  reviews: Review[];
  total: number;
  averageRating: number;
  ratingDistribution: Record<number, number>;
  page: number;
  limit: number;
  totalPages: number;
}

// Product Review Summary Interface
export interface ProductReviewSummary {
  productId: number;
  totalReviews: number;
  averageRating: number;
  ratingDistribution: {
    5: number;
    4: number;
    3: number;
    2: number;
    1: number;
  };
  verifiedPurchasePercentage: number;
  recommendedPercentage: number;
}

// User Review Permission Interface
export interface ReviewPermissions {
  canCreate: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canReport: boolean;
  canRespond: boolean;
  reason?: string; // Why they can't create/edit
}

// Review Moderation Action Interface
export interface ReviewModerationAction {
  reviewId: number;
  action: 'approve' | 'reject' | 'delete';
  reason?: string;
  adminId: number;
  adminName: string;
  timestamp: string;
}