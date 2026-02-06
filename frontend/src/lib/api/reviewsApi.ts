import { Review, ReviewFilters, CreateReviewData, UpdateReviewData } from '@/types/review';

// Normalize API base URL to avoid duplicated '/api'
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

export const reviewsApi = {
  /**
   * Create a new review
   */
  async createReview(reviewData: CreateReviewData, userId: number): Promise<Review> {
    const response = await fetch(`${API_BASE_URL}/reviews`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...reviewData,
        userId,
      }),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to create review' }));
      throw new Error(error.message || 'Failed to create review');
    }

    return response.json();
  },

  /**
   * Get all reviews for a product
   */
  async getProductReviews(
    productId: number,
    options: {
      page?: number;
      limit?: number;
      sort?: string;
      filters?: ReviewFilters;
    } = {}
  ): Promise<{
    reviews: Review[];
    total: number;
    averageRating: number;
    ratingDistribution: Record<number, number>;
  }> {
    const params = new URLSearchParams();
    
    if (options.page) params.append('page', options.page.toString());
    if (options.limit) params.append('limit', options.limit.toString());
    if (options.sort) params.append('sort', options.sort);
    if (options.filters) {
      Object.entries(options.filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params.append(key, value.toString());
        }
      });
    }

    const response = await fetch(
      `${API_BASE_URL}/products/${productId}/reviews?${params.toString()}`
    );

    if (!response.ok) {
      throw new Error('Failed to fetch reviews');
    }

    // Normalize both new paginated backend format and legacy responses
    const payload = await response.json().catch(() => null);

    // New backend format: { success: true, data: { content: [], page, size, totalElements, ... } }
    if (payload && payload.data && Array.isArray(payload.data.content)) {
      const reviews = payload.data.content as Review[];
      const total = payload.data.totalElements ?? reviews.length;
      return {
        reviews,
        total,
        averageRating: 0,
        ratingDistribution: { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 },
      };
    }

    // Legacy format: { reviews: Review[], total: number, averageRating?, ratingDistribution? }
    if (payload && Array.isArray(payload.reviews)) {
      return {
        reviews: payload.reviews as Review[],
        total: payload.total ?? (payload.reviews as any[]).length,
        averageRating: payload.averageRating ?? 0,
        ratingDistribution: payload.ratingDistribution ?? { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 },
      };
    }

    // Fallback
    return {
      reviews: [],
      total: 0,
      averageRating: 0,
      ratingDistribution: { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 },
    };
  },

  /**
   * Get a specific review by ID
   */
  async getReviewById(reviewId: number): Promise<Review> {
    const response = await fetch(`${API_BASE_URL}/reviews/${reviewId}`);

    if (!response.ok) {
      throw new Error('Review not found');
    }

    return response.json();
  },

  /**
   * Update an existing review
   */
  async updateReview(reviewId: number, reviewData: UpdateReviewData, userId: number): Promise<Review> {
    const response = await fetch(`${API_BASE_URL}/reviews/${reviewId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        ...reviewData,
        userId,
      }),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to update review' }));
      throw new Error(error.message || 'Failed to update review');
    }

    return response.json();
  },

  /**
   * Delete a review
   */
  async deleteReview(reviewId: number, userId: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/reviews/${reviewId}?userId=${userId}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to delete review' }));
      throw new Error(error.message || 'Failed to delete review');
    }
  },

  /**
   * Mark a review as helpful/unhelpful
   */
  async markReviewHelpful(reviewId: number, helpful: boolean, userId?: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/reviews/${reviewId}/helpful`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        helpful,
        userId,
      }),
    });

    if (!response.ok) {
      throw new Error('Failed to mark review as helpful');
    }
  },

  /**
   * Report a review
   */
  async reportReview(reviewId: number, userId?: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/reviews/${reviewId}/report`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        userId,
      }),
    });

    if (!response.ok) {
      throw new Error('Failed to report review');
    }
  },

  /**
   * Add a response to a review (admin/seller)
   */
  async addReviewResponse(reviewId: number, response: string, userId: number): Promise<Review> {
    const res = await fetch(`${API_BASE_URL}/reviews/${reviewId}/respond`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        response,
        userId,
      }),
    });

    if (!res.ok) {
      throw new Error('Failed to add response to review');
    }

    return res.json();
  },

  /**
   * Get review statistics for a product
   */
  async getReviewStats(productId: number): Promise<{
    totalReviews: number;
    averageRating: number;
    ratingDistribution: Record<number, number>;
    verifiedPurchaseReviews: number;
  }> {
    const response = await fetch(`${API_BASE_URL}/products/${productId}/reviews/stats`);

    if (!response.ok) {
      throw new Error('Failed to fetch review statistics');
    }

    return response.json();
  },

  /**
   * Get user's reviews for a product (to check if they can review)
   */
  async getUserReviewForProduct(productId: number, userId: number): Promise<Review | null> {
    try {
      const response = await fetch(`${API_BASE_URL}/reviews/user/${userId}/product/${productId}`);

      if (response.status === 404) {
        return null;
      }

      if (!response.ok) {
        throw new Error('Failed to check user review');
      }

      return response.json();
    } catch {
      return null;
    }
  },

  /**
   * Get all reviews by a user
   */
  async getUserReviews(
    userId: number,
    options: {
      page?: number;
      limit?: number;
      status?: string;
    } = {}
  ): Promise<{
    reviews: Review[];
    total: number;
  }> {
    const params = new URLSearchParams();
    
    if (options.page) params.append('page', options.page.toString());
    if (options.limit) params.append('limit', options.limit.toString());
    if (options.status) params.append('status', options.status);

    const response = await fetch(
      `${API_BASE_URL}/reviews/user/${userId}?${params.toString()}`
    );

    if (!response.ok) {
      throw new Error('Failed to fetch user reviews');
    }

    return response.json();
  },

  /**
   * Admin: Get all reviews (with filtering and pagination)
   */
  async getAllReviews(
    options: {
      page?: number;
      limit?: number;
      status?: string;
      productId?: number;
      userId?: number;
      rating?: number;
      sortBy?: string;
    } = {}
  ): Promise<{
    reviews: Review[];
    total: number;
    page: number;
    totalPages: number;
  }> {
    const params = new URLSearchParams();
    
    Object.entries(options).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        params.append(key, value.toString());
      }
    });

    const response = await fetch(
      `${API_BASE_URL}/admin/reviews?${params.toString()}`
    );

    if (!response.ok) {
      throw new Error('Failed to fetch reviews');
    }

    return response.json();
  },

  /**
   * Admin: Approve a review
   */
  async approveReview(reviewId: number, adminId: number): Promise<Review> {
    const response = await fetch(`${API_BASE_URL}/admin/reviews/${reviewId}/approve`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        adminId,
      }),
    });

    if (!response.ok) {
      throw new Error('Failed to approve review');
    }

    return response.json();
  },

  /**
   * Admin: Reject a review
   */
  async rejectReview(reviewId: number, reason: string, adminId: number): Promise<Review> {
    const response = await fetch(`${API_BASE_URL}/admin/reviews/${reviewId}/reject`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        reason,
        adminId,
      }),
    });

    if (!response.ok) {
      throw new Error('Failed to reject review');
    }

    return response.json();
  },
};