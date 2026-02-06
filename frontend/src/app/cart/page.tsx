'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCart } from '@/context/CartContext';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { SkeletonCheckout } from '@/components/skeletons';
import toast from 'react-hot-toast';

// ==================== Components ====================

const EmptyCartView = () => (
  <div className="min-h-screen bg-gray-50 py-16">
    <div className="w-full px-4 sm:px-6 lg:px-8 text-center">
      <div className="w-24 h-24 bg-gray-200 rounded-full flex items-center justify-center mx-auto mb-6">
        <svg
          className="w-12 h-12 text-gray-400"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
          />
        </svg>
      </div>
      <h1 className="text-2xl font-bold text-gray-900 mb-4">
        Your cart is empty
      </h1>
      <p className="text-gray-600 mb-8">
        Looks like you haven't added any products to your cart yet.
      </p>
      <Link href="/products">
        <Button variant="primary" size="lg">
          Start Shopping
        </Button>
      </Link>
    </div>
  </div>
);

const CartItemSkeleton = () => (
  <div className="bg-white rounded-lg shadow-sm p-6 animate-pulse">
    <div className="flex items-center gap-4">
      <div className="w-20 h-20 bg-gray-200 rounded-lg"></div>
      <div className="flex-1">
        <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
        <div className="h-3 bg-gray-200 rounded w-1/2"></div>
      </div>
      <div className="w-24 h-8 bg-gray-200 rounded"></div>
    </div>
  </div>
);

const ValidationIssuesAlert = ({ issues }: { issues: any[] }) => (
  <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
    <div className="flex items-start">
      <svg
        className="w-5 h-5 text-red-600 mt-0.5 mr-3"
        fill="currentColor"
        viewBox="0 0 20 20"
      >
        <path
          fillRule="evenodd"
          d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
          clipRule="evenodd"
        />
      </svg>
      <div className="flex-1">
        <h3 className="text-red-800 font-semibold mb-2">
          Cart Validation Issues
        </h3>
        <div className="space-y-2">
          {issues.map((issue, index) => (
            <div key={index} className="text-red-700 text-sm">
              <span className="font-medium">{issue.productName}:</span>{' '}
              {issue.message}
              {issue.type === 'PRICE_CHANGED' && issue.oldPrice && issue.newPrice && (
                <span className="ml-2">
                  (GHS {issue.oldPrice.toFixed(2)} → GHS {issue.newPrice.toFixed(2)})
                </span>
              )}
              {issue.type === 'INSUFFICIENT_STOCK' && (
                <span className="ml-2">
                  (Available: {issue.availableQuantity})
                </span>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  </div>
);

const InvalidItemsAlert = ({ items }: { items: any[] }) => {
  if (!items || items.length === 0) return null;

  return (
    <div className="mb-6 bg-yellow-50 border border-yellow-200 rounded-lg p-4">
      <div className="flex items-start">
        <svg className="w-5 h-5 text-yellow-600 mt-0.5 mr-3" fill="currentColor" viewBox="0 0 20 20">
          <path d="M8.257 3.099c.765-1.36 2.72-1.36 3.485 0l5.58 9.918A1.75 1.75 0 0116.585 16H3.415a1.75 1.75 0 01-1.737-1.983L8.257 3.1zM11 13a1 1 0 10-2 0 1 1 0 002 0zm-1-9a1 1 0 00-.993.883L8 5v5a1 1 0 001.993.117L10 10V5a1 1 0 00-1-1z" />
        </svg>
        <div className="flex-1">
          <h3 className="text-yellow-800 font-semibold mb-2">Some cart items have missing product data</h3>
          <p className="text-yellow-700 text-sm">
            One or more items in your cart are missing product details (price, name or image). This can happen when a product is removed or the backend returned incomplete data.
          </p>
          <div className="mt-3 flex gap-2">
            <button
              onClick={() => {
                try {
                  // Remove items with missing price or product
                  items.forEach((it: any) => {
                    if (!it.product || it.unitPrice == null) {
                      // attempt to remove via DOM-local handler: dispatch a custom event
                      window.dispatchEvent(new CustomEvent('remove-cart-item', { detail: { productId: it.product?.id } }));
                    }
                  });
                } catch (e) {
                  console.error('Failed to remove invalid items', e);
                }
              }}
              className="px-3 py-2 bg-yellow-600 text-white rounded hover:bg-yellow-700"
            >
              Remove Invalid Items
            </button>
            <a href="/help" className="px-3 py-2 border border-yellow-200 rounded text-yellow-700 hover:bg-yellow-100">Contact Support</a>
          </div>
        </div>
      </div>
    </div>
  );
};

const CouponSection = ({
  couponCode,
  discount,
  onApply,
  onRemove,
  loading,
}: {
  couponCode?: string;
  discount: number;
  onApply: (code: string) => void;
  onRemove: () => void;
  loading: boolean;
}) => {
  const [code, setCode] = useState('');
  const [isExpanded, setIsExpanded] = useState(false);

  const handleApply = () => {
    if (code.trim()) {
      onApply(code.trim().toUpperCase());
      setCode('');
    }
  };

  if (couponCode) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-lg p-4 mb-4">
        <div className="flex items-center justify-between">
          <div>
            <span className="text-green-800 font-semibold">
              Coupon Applied: {couponCode}
            </span>
            <p className="text-green-600 text-sm mt-1">
              You saved GHS {discount.toFixed(2)}!
            </p>
          </div>
          <button
            onClick={onRemove}
            disabled={loading}
            className="text-green-600 hover:text-green-800 text-sm font-medium"
          >
            Remove
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="mb-4">
      {!isExpanded ? (
        <button
          onClick={() => setIsExpanded(true)}
          className="text-blue-600 hover:text-blue-800 text-sm font-medium"
        >
          Have a coupon code?
        </button>
      ) : (
        <div className="flex gap-2">
          <input
            type="text"
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
            placeholder="Enter coupon code"
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            disabled={loading}
          />
          <Button
            variant="outline"
            onClick={handleApply}
            disabled={loading || !code.trim()}
          >
            Apply
          </Button>
          <button
            onClick={() => setIsExpanded(false)}
            className="text-gray-500 hover:text-gray-700"
          >
            Cancel
          </button>
        </div>
      )}
    </div>
  );
};

const CartItemRow = ({
  item,
  onUpdateQuantity,
  onRemove,
  loading,
}: {
  item: any;
  onUpdateQuantity: (id: number, quantity: number) => void;
  onRemove: (id: number) => void;
  loading: boolean;
}) => {
  return (
    <div className="bg-white rounded-xl shadow-md p-6 flex items-center gap-6 animate-in fade-in-50 duration-300">
      {/* Product Image */}
      <div className="w-24 h-24 bg-gray-100 rounded-lg flex items-center justify-center shrink-0">
        {item.product?.imageUrl ? (
          <img
            src={item.product.imageUrl}
            alt={item.product?.name || 'Product image'}
            className="w-full h-full object-cover rounded-lg"
          />
        ) : (
          <span className="text-4xl">🛍️</span>
        )}
      </div>

      {/* Product Info */}
      <div className="flex-1 min-w-0">
        <Link
          href={item.product?.id ? `/products/${item.product.id}` : '/products'}
          className="text-lg font-semibold text-gray-900 hover:text-blue-600 truncate block"
        >
          {item.product?.name || 'Product information unavailable'}
        </Link>
        <p className="text-gray-500 text-sm mt-1">
          {typeof item.product?.category === 'object'
            ? item.product?.category?.name
            : item.product?.category}
        </p>
        <div className="flex items-center gap-2 mt-2">
          <p className="text-blue-600 font-bold">
            GHS {(item.unitPrice ?? 0).toFixed(2)}
          </p>
          {item.product?.inStock === false && (
            <span className="text-xs bg-red-100 text-red-600 px-2 py-1 rounded">
              Out of Stock
            </span>
          )}
        </div>
      </div>

      {/* Quantity Controls */}
      <div className="flex items-center gap-3">
        <button
          onClick={() => onUpdateQuantity(item.product?.id ?? item.id, item.quantity - 1)}
          disabled={loading || item.quantity <= 1}
          className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          -
        </button>
        <span className="w-8 text-center font-medium">{item.quantity}</span>
        <button
          onClick={() => onUpdateQuantity(item.product?.id ?? item.id, item.quantity + 1)}
          disabled={loading}
          className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100 disabled:opacity-50 transition-colors"
        >
          +
        </button>
      </div>

      {/* Item Total */}
      <div className="text-right min-w-[100px]">
        <p className="text-lg font-bold text-gray-900">
          GHS {(item.totalPrice ?? 0).toFixed(2)}
        </p>
      </div>

      {/* Remove Button */}
      <button
        onClick={() => onRemove(item.product?.id ?? item.id)}
        disabled={loading}
        className="text-gray-400 hover:text-red-500 transition-colors disabled:opacity-50"
        title="Remove item"
      >
        <svg
          className="w-6 h-6"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
          />
        </svg>
      </button>
    </div>
  );
};

// ==================== Main Component ====================

export default function CartPage() {
  const {
    items,
    total,
    subtotal,
    discount,
    itemCount,
    removeFromCart,
    updateQuantity,
    clearCart,
    loading,
    checkout,
    cartId,
    applyCoupon,
    removeCoupon,
    validateCart,
    validationResult,
    shareCart,
    saveForLater,
    cart,
  } = useCart();

  const { isAuthenticated } = useAuth();
  const router = useRouter();

  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [showActions, setShowActions] = useState(false);

  // Calculate additional costs
  const shipping = subtotal >= 50 ? 0 : 9.99;
  const tax = subtotal * 0.08;
  const finalTotal = subtotal + shipping + tax - discount;

  // Validate cart on mount and when items change
  useEffect(() => {
    if (items.length > 0 && !loading) {
      handleValidate();
    }
  }, [items.length]);

  // Listen for remove-cart-item custom events (fired by InvalidItemsAlert)
  useEffect(() => {
    const handler = (e: any) => {
      const productId = e?.detail?.productId;
      if (!productId) return;
      try {
        removeFromCart(productId);
      } catch (err) {
        console.error('Failed to remove invalid item', err);
      }
    };
    window.addEventListener('remove-cart-item', handler as EventListener);
    return () => window.removeEventListener('remove-cart-item', handler as EventListener);
  }, [removeFromCart]);

  const handleValidate = async () => {
    setIsValidating(true);
    try {
      await validateCart();
    } finally {
      setIsValidating(false);
    }
  };

const handleCheckout = async () => {
    if (!isAuthenticated) {
      toast.error('Please login to checkout');
      // Store cart items before redirecting to login
      if (cartId) {
        localStorage.setItem('checkout_cart_id', cartId.toString());
      }
      router.push('/auth/login?redirect=/checkout');
      return;
    }

    if (itemCount === 0) {
      toast.error('Cannot checkout an empty cart');
      return;
    }

    // Validate cart before checkout
    setIsValidating(true);
    const validation = await validateCart();
    setIsValidating(false);

    if (!validation?.valid) {
      toast.error('Please resolve cart issues before checkout');
      return;
    }

    setIsCheckingOut(true);

    try {
      const orderResponse = await checkout();

      if (orderResponse) {
        toast.success('Order placed successfully!');
        router.push(`/orders/${orderResponse.orderId}`);
      }
    } catch (error: any) {
      console.error('Checkout failed:', error);
      toast.error(error.message || 'Checkout failed. Please try again.');
    } finally {
      setIsCheckingOut(false);
    }
  };

  const handleShareCart = async () => {
    const shareUrl = await shareCart();
    if (shareUrl) {
      toast.success('Share link copied!', {
        duration: 3000,
      });
    }
  };

  const handleSaveForLater = async () => {
    if (!isAuthenticated) {
      toast.error('Please login to save items');
      return;
    }
    await saveForLater();
  };

  // Empty cart view
  if (items.length === 0 && !loading) {
    return <EmptyCartView />;
  }

  // Loading view
  if (loading && items.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900">Shopping Cart</h1>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2">
              <SkeletonCheckout variant="cart" />
            </div>
            <div className="lg:col-span-1">
              <SkeletonCheckout variant="payment" />
            </div>
          </div>
        </div>
      </div>
    );
  }

return (
    <div className="min-h-screen bg-gray-50 py-8 relative">
      {/* Loading overlay */}
      {(loading || isCheckingOut) && (
        <div className="fixed inset-0 bg-black bg-opacity-10 z-50 flex items-center justify-center">
          <div className="bg-white rounded-lg p-6 shadow-lg flex items-center gap-3">
            <svg className="animate-spin h-6 w-6 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            <span className="text-gray-700 font-medium">
              {isCheckingOut ? 'Processing checkout...' : 'Updating cart...'}
            </span>
          </div>
        </div>
      )}
      
      <div className="w-full px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Shopping Cart</h1>
            {cartId && (
              <p className="text-sm text-gray-500 mt-1">Cart ID: {cartId}</p>
            )}
          </div>
          <div className="flex items-center gap-4">
            <button
              onClick={() => setShowActions(!showActions)}
              className="text-gray-600 hover:text-gray-900 p-2 rounded-lg hover:bg-gray-100 transition-colors"
              title="More actions"
            >
              <svg
                className="w-6 h-6"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"
                />
              </svg>
            </button>

            {/* Actions Dropdown */}
            {showActions && (
              <div className="absolute right-8 mt-2 w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-10">
                <button
                  onClick={handleShareCart}
                  className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                  </svg>
                  Share Cart
                </button>
                <button
                  onClick={handleSaveForLater}
                  className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                  </svg>
                  Save for Later
                </button>
                <button
                  onClick={clearCart}
                  disabled={loading}
                  className="w-full text-left px-4 py-2 hover:bg-gray-50 text-red-600 flex items-center gap-2"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  Clear Cart
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Validation Issues */}
        {validationResult && !validationResult.valid && (
          <ValidationIssuesAlert issues={validationResult.issues} />
        )}
        {/* Alert for invalid/missing product data */}
        {items.some((it: any) => !it.product || it.unitPrice == null) && (
          <InvalidItemsAlert items={items.filter((it: any) => !it.product || it.unitPrice == null)} />
        )}

<div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Cart Items */}
          <div className="lg:col-span-2 space-y-4">
            {loading && items.length === 0 ? (
              // Show skeletons when initially loading
              Array.from({ length: 3 }).map((_, index) => (
                <CartItemSkeleton key={index} />
              ))
            ) : items.length > 0 ? (
              items.map((item) => (
                <CartItemRow
                  key={item.id}
                  item={item}
                  onUpdateQuantity={updateQuantity}
                  onRemove={removeFromCart}
                  loading={loading}
                />
              ))
            ) : null}
          </div>

          {/* Order Summary */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-md p-6 sticky top-24">
              <h2 className="text-xl font-bold text-gray-900 mb-6">
                Order Summary
              </h2>

              {/* Coupon Section */}
              <CouponSection
                couponCode={cart?.couponCode}
                discount={discount}
                onApply={applyCoupon}
                onRemove={removeCoupon}
                loading={loading}
              />

              <div className="space-y-4 mb-6">
                <div className="flex justify-between text-gray-600">
                  <span>Subtotal ({itemCount} items)</span>
                  <span>GHS {subtotal.toFixed(2)}</span>
                </div>

                {discount > 0 && (
                  <div className="flex justify-between text-green-600">
                    <span>Discount</span>
                    <span>-GHS {discount.toFixed(2)}</span>
                  </div>
                )}

                <div className="flex justify-between text-gray-600">
                  <span>Shipping</span>
                  <span>{shipping === 0 ? 'Free' : `GHS ${shipping.toFixed(2)}`}</span>
                </div>

                <div className="flex justify-between text-gray-600">
                  <span>Tax (8%)</span>
                  <span>GHS {tax.toFixed(2)}</span>
                </div>

                <hr />

                <div className="flex justify-between text-xl font-bold text-gray-900">
                  <span>Total</span>
                  <span>GHS {finalTotal.toFixed(2)}</span>
                </div>
              </div>

              {subtotal < 50 && (
                <p className="text-sm text-gray-500 mb-4 bg-blue-50 p-3 rounded-lg">
                  💡 Add GHS {(50 - subtotal).toFixed(2)} more for free shipping!
                </p>
              )}

              {/* Validate Button */}
              <Button
                variant="outline"
                size="md"
                className="w-full mb-3"
                disabled={loading || isValidating}
                onClick={handleValidate}
              >
                {isValidating ? 'Validating...' : '🔍 Validate Cart'}
              </Button>

{/* Checkout Button */}
              <Button
                variant="primary"
                size="lg"
                className="w-full mb-4 relative"
                disabled={
                  loading ||
                  isCheckingOut ||
                  isValidating ||
                  itemCount === 0 ||
                  (validationResult && !validationResult.valid)
                }
                onClick={handleCheckout}
              >
                {isCheckingOut ? (
                  <span className="flex items-center justify-center">
                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Processing...
                  </span>
                ) : isValidating ? (
                  <span className="flex items-center justify-center">
                    <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Validating...
                  </span>
                ) : isAuthenticated ? (
                  `Checkout - GHS ${finalTotal.toFixed(2)}`
                ) : (
                  'Login to Checkout'
                )}
              </Button>

              {!isAuthenticated && (
                <p className="text-sm text-gray-500 mb-4 text-center">
                  You need to be logged in to checkout
                </p>
              )}

              <Link href="/products" className="block mt-4">
                <Button variant="outline" className="w-full">
                  Continue Shopping
                </Button>
              </Link>

              {/* Trust Badges */}
              <div className="mt-6 pt-6 border-t border-gray-200">
                <div className="flex items-center justify-center gap-4 text-sm text-gray-500">
                  <div className="flex items-center gap-1">
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
                    </svg>
                    Secure Payment
                  </div>
                  <div className="flex items-center gap-1">
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z" />
                      <path d="M3 4a1 1 0 00-1 1v10a1 1 0 001 1h1.05a2.5 2.5 0 014.9 0H10a1 1 0 001-1V5a1 1 0 00-1-1H3zM14 7a1 1 0 00-1 1v6.05A2.5 2.5 0 0115.95 16H17a1 1 0 001-1v-5a1 1 0 00-.293-.707l-2-2A1 1 0 0015 7h-1z" />
                    </svg>
                    Free Returns
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}