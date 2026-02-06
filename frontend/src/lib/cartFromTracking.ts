import { cartApi } from '@/lib/api';

export interface TrackedProduct {
  productId: number;
  productName: string;
  category: string;
  price: number;
  timestamp: string;
}

export interface CartCreationRequest {
  sessionId: string;
  products: TrackedProduct[];
}

class CartFromTrackingService {
  private trackedProducts: Map<number, TrackedProduct> = new Map();
  private sessionId: string;

  constructor() {
    this.sessionId = this.generateSessionId();
    this.loadTrackedProducts();
  }

  private generateSessionId(): string {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }

  private loadTrackedProducts() {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem('tracked_products');
      if (stored) {
        try {
          const products = JSON.parse(stored);
          this.trackedProducts = new Map(products.map((p: TrackedProduct) => [p.productId, p]));
        } catch (e) {
          console.error('Failed to load tracked products:', e);
        }
      }
    }
  }

  private saveTrackedProducts() {
    if (typeof window !== 'undefined') {
      const products = Array.from(this.trackedProducts.values());
      localStorage.setItem('tracked_products', JSON.stringify(products));
    }
  }

  addTrackedProduct(product: TrackedProduct) {
    // Update or add the product with latest timestamp
    this.trackedProducts.set(product.productId, product);
    this.saveTrackedProducts();
    console.log('📊 Tracked product added:', product);
    console.log('📊 Total tracked products:', this.trackedProducts.size);
  }

  async createCartFromTrackedProducts(): Promise<boolean> {
    try {
      const products = Array.from(this.trackedProducts.values());
      
      if (products.length === 0) {
        console.log('📊 No tracked products to create cart from');
        return false;
      }

      console.log('🛒 Creating cart from tracked products:', products);

      const request: CartCreationRequest = {
        sessionId: this.sessionId,
        products: products
      };

      // Call backend to create cart with tracked products
      const response = await cartApi.createCartFromTracking(request);
      
      console.log('✅ Cart created from tracking:', response);
      
      // Clear tracked products after successful cart creation
      this.clearTrackedProducts();
      
      return true;
    } catch (error) {
      console.error('❌ Failed to create cart from tracked products:', error);
      return false;
    }
  }

  clearTrackedProducts() {
    this.trackedProducts.clear();
    if (typeof window !== 'undefined') {
      localStorage.removeItem('tracked_products');
    }
    console.log('📊 Tracked products cleared');
  }

  getTrackedProducts(): TrackedProduct[] {
    return Array.from(this.trackedProducts.values());
  }

  getTrackedProductsCount(): number {
    return this.trackedProducts.size;
  }

  getSessionId(): string {
    return this.sessionId;
  }

  // Optional: Sync sessionId with tracking service if needed
  syncSessionId(externalSessionId: string) {
    this.sessionId = externalSessionId;
    console.log('📊 Session ID synced:', this.sessionId);
  }
}

// Export singleton instance
export const cartFromTrackingService = new CartFromTrackingService();

// Hook for easy usage in components
export function useCartFromTracking() {
  return {
    addTrackedProduct: cartFromTrackingService.addTrackedProduct.bind(cartFromTrackingService),
    createCartFromTrackedProducts: cartFromTrackingService.createCartFromTrackedProducts.bind(cartFromTrackingService),
    clearTrackedProducts: cartFromTrackingService.clearTrackedProducts.bind(cartFromTrackingService),
    getTrackedProducts: cartFromTrackingService.getTrackedProducts.bind(cartFromTrackingService),
    getTrackedProductsCount: cartFromTrackingService.getTrackedProductsCount.bind(cartFromTrackingService),
    getSessionId: cartFromTrackingService.getSessionId.bind(cartFromTrackingService),
    syncSessionId: cartFromTrackingService.syncSessionId.bind(cartFromTrackingService),
  };
}
