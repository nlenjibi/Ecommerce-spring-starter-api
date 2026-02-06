/**
 * Cart System Test Suite
 * Tests for cart functionality including guest/user operations, merge logic, and error handling
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { CartProvider, useCart } from '@/context/CartContext';
import { AuthProvider, useAuth } from '@/context/AuthContext';

// Mock fetch API
global.fetch = vi.fn();

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
};
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock toast
vi.mock('react-hot-toast', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

// Test wrapper component
const TestWrapper = ({ children }: { children: React.ReactNode }) => (
  <AuthProvider>
    <CartProvider>
      {children}
    </CartProvider>
  </AuthProvider>
);

describe('Cart System', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.getItem.mockReturnValue(null);
    (fetch as any).mockReset();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Cart Initialization', () => {
    it('should initialize empty cart for new user', async () => {
      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [],
            itemCount: 0,
            subtotal: 0,
            totalPrice: 0,
            discount: 0,
            status: 'active'
          }
        })
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.cart).toBeDefined();
      expect(result.current.items).toHaveLength(0);
      expect(result.current.itemCount).toBe(0);
    });

    it('should load existing cart from localStorage', async () => {
      localStorageMock.getItem.mockReturnValue('456');

      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 456,
            items: [
              {
                id: 1,
                product: { id: 1, name: 'Test Product', price: 10 },
                quantity: 2,
                unitPrice: 10,
                totalPrice: 20
              }
            ],
            itemCount: 2,
            subtotal: 20,
            totalPrice: 20,
            discount: 0,
            status: 'active'
          }
        })
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.cartId).toBe(456);
      expect(result.current.items).toHaveLength(1);
      expect(result.current.itemCount).toBe(2);
    });
  });

  describe('Add to Cart', () => {
    it('should add item to cart successfully', async () => {
      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [],
            itemCount: 0,
            subtotal: 0,
            totalPrice: 0,
            discount: 0,
            status: 'active'
          }
        })
      }).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [
              {
                id: 1,
                product: { id: 1, name: 'Test Product', price: 10 },
                quantity: 1,
                unitPrice: 10,
                totalPrice: 10
              }
            ],
            itemCount: 1,
            subtotal: 10,
            totalPrice: 10,
            discount: 0,
            status: 'active'
          }
        })
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        await result.current.addToCart(1, 1);
      });

      expect(result.current.items).toHaveLength(1);
      expect(result.current.itemCount).toBe(1);
    });

    it('should handle insufficient stock error', async () => {
      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [],
            itemCount: 0,
            subtotal: 0,
            totalPrice: 0,
            discount: 0,
            status: 'active'
          }
        })
      }).mockRejectedValueOnce({
        status: 400,
        data: { field: 'quantity', availableQuantity: 2 },
        message: 'Insufficient stock available'
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });
      const { error } = await import('react-hot-toast');

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        await result.current.addToCart(1, 5);
      });

      expect(error).toHaveBeenCalledWith('Only 2 items available in stock');
    });
  });

  describe('Update Quantity', () => {
    it('should update item quantity with optimistic update', async () => {
      localStorageMock.getItem.mockReturnValue('123');

      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [
              {
                id: 1,
                product: { id: 1, name: 'Test Product', price: 10 },
                quantity: 1,
                unitPrice: 10,
                totalPrice: 10
              }
            ],
            itemCount: 1,
            subtotal: 10,
            totalPrice: 10,
            discount: 0,
            status: 'active'
          }
        })
      }).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [
              {
                id: 1,
                product: { id: 1, name: 'Test Product', price: 10 },
                quantity: 3,
                unitPrice: 10,
                totalPrice: 30
              }
            ],
            itemCount: 3,
            subtotal: 30,
            totalPrice: 30,
            discount: 0,
            status: 'active'
          }
        })
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Optimistic update should happen immediately
      await act(async () => {
        await result.current.updateQuantity(1, 3);
      });

      expect(result.current.items[0].quantity).toBe(3);
    });
  });

  describe('Remove from Cart', () => {
    it('should remove item from cart successfully', async () => {
      localStorageMock.getItem.mockReturnValue('123');

      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [
              {
                id: 1,
                product: { id: 1, name: 'Test Product', price: 10 },
                quantity: 1,
                unitPrice: 10,
                totalPrice: 10
              }
            ],
            itemCount: 1,
            subtotal: 10,
            totalPrice: 10,
            discount: 0,
            status: 'active'
          }
        })
      }).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [],
            itemCount: 0,
            subtotal: 0,
            totalPrice: 0,
            discount: 0,
            status: 'active'
          }
        })
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        await result.current.removeFromCart(1);
      });

      expect(result.current.items).toHaveLength(0);
      expect(result.current.itemCount).toBe(0);
    });
  });

  describe('Cart Merge', () => {
    it('should merge guest cart with user cart on login', async () => {
      localStorageMock.getItem.mockReturnValue('456');

      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 456,
            items: [
              {
                id: 1,
                product: { id: 1, name: 'Guest Product', price: 10 },
                quantity: 1,
                unitPrice: 10,
                totalPrice: 10
              }
            ],
            itemCount: 1,
            subtotal: 10,
            totalPrice: 10,
            discount: 0,
            status: 'active'
          }
        })
      }).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 789,
            items: [
              {
                id: 1,
                product: { id: 1, name: 'Guest Product', price: 10 },
                quantity: 1,
                unitPrice: 10,
                totalPrice: 10
              },
              {
                id: 2,
                product: { id: 2, name: 'User Product', price: 20 },
                quantity: 1,
                unitPrice: 20,
                totalPrice: 20
              }
            ],
            itemCount: 2,
            subtotal: 30,
            totalPrice: 30,
            discount: 0,
            status: 'active'
          }
        })
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        await result.current.mergeCart(456);
      });

      expect(result.current.cartId).toBe(789);
      expect(result.current.items).toHaveLength(2);
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors gracefully', async () => {
      (fetch as any).mockRejectedValueOnce(new Error('Network error'));

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });
      const { error } = await import('react-hot-toast');

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      await act(async () => {
        await result.current.addToCart(1, 1);
      });

      expect(error).toHaveBeenCalled();
    });

    it('should handle 404 errors for missing cart', async () => {
      localStorageMock.getItem.mockReturnValue('999');

      (fetch as any).mockRejectedValueOnce({
        status: 404,
        message: 'Cart not found'
      });

      const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      // Should create new cart when old one not found
      expect(result.current.cart).toBeDefined();
    });
  });

  describe('Performance Optimizations', () => {
    it('should use useMemo for computed values', async () => {
      (fetch as any).mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve({
          success: true,
          data: {
            id: 123,
            items: [],
            itemCount: 0,
            subtotal: 0,
            totalPrice: 0,
            discount: 0,
            status: 'active'
          }
        })
      });

      const { result, rerender } = renderHook(() => useCart(), { wrapper: TestWrapper });

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      const initialItems = result.current.items;
      
      // Rerender should not create new objects unnecessarily
      rerender();
      
      expect(result.current.items).toBe(initialItems);
    });
  });
});

describe('Cart Integration Tests', () => {
  it('should handle complete cart flow: add -> update -> remove', async () => {
    (fetch as any).mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({
        success: true,
        data: {
          id: 123,
          items: [],
          itemCount: 0,
          subtotal: 0,
          totalPrice: 0,
          discount: 0,
          status: 'active'
        }
      })
    });

    const { result } = renderHook(() => useCart(), { wrapper: TestWrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Add item
    await act(async () => {
      await result.current.addToCart(1, 1);
    });

    expect(result.current.items).toHaveLength(1);

    // Update quantity
    await act(async () => {
      await result.current.updateQuantity(1, 3);
    });

    expect(result.current.items[0].quantity).toBe(3);

    // Remove item
    await act(async () => {
      await result.current.removeFromCart(1);
    });

    expect(result.current.items).toHaveLength(0);
  });
});