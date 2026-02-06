'use client';

import React, { useState, useEffect } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import {
  Heart,
  Calendar,
  DollarSign,
  User,
  Share2,
  Lock,
  Eye,
  Download,
  X,
  ExternalLink,
  ShoppingCart,
  AlertCircle,
  CheckCircle2,
  Info,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { StockBadge } from '@/components/StockBadge';
import { getImageUrl } from '@/lib/utils';

interface SharedWishlistItem {
  id: number;
  product: {
    id: number;
    name: string;
    slug?: string;
    sku?: string;
    price: number;
    discountPrice?: number;
    imageUrl?: string;
    categoryName?: string;
    inStock: boolean;
    availableQuantity?: number;
    inventoryStatus?: string;
  };
  notes?: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  desiredQuantity: number;
  priceWhenAdded: number;
  currentPrice: number;
  priceDifference: number;
  isPriceDropped: boolean;
  targetPrice?: number;
  purchased: boolean;
  isPublic: boolean;
  inStock: boolean;
  addedAt: string;
  purchasedAt?: string;
}

interface SharedWishlistData {
  userId: number;
  totalItems: number;
  inStockItems: number;
  outOfStockItems: number;
  itemsWithPriceDrops: number;
  purchasedItems: number;
  totalValue: number;
  totalSavings: number;
  items: SharedWishlistItem[];
}

interface PublicWishlistPageProps {
  params: {
    shareToken: string;
  };
}

export default function PublicWishlistPage({ params }: PublicWishlistPageProps) {
  const shareToken = params.shareToken;
  
  const [wishlistData, setWishlistData] = useState<SharedWishlistData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isPasswordProtected, setIsPasswordProtected] = useState(false);
  const [password, setPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [copiedUrl, setCopiedUrl] = useState(false);

  useEffect(() => {
    fetchSharedWishlist();
  }, [shareToken]);

  const fetchSharedWishlist = async () => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const url = `/api/v1/wishlist/shared/${shareToken}`;
      const response = await fetch(url);

      if (response.status === 401) {
        setIsPasswordProtected(true);
        return;
      }

      if (!response.ok) {
        throw new Error('Wishlist not found or expired');
      }

      const data = await response.json();
      if (data.success) {
        setWishlistData(data.data);
      } else {
        throw new Error(data.message || 'Failed to load wishlist');
      }
    } catch (err) {
      console.error('Failed to fetch shared wishlist:', err);
      setErrorMessage(err instanceof Error ? err.message : 'Failed to load wishlist');
    } finally {
      setIsLoading(false);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!password) return;

    try {
      const response = await fetch(`/api/v1/wishlist/shared/${shareToken}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ password }),
      });

      if (!response.ok) {
        throw new Error('Invalid password');
      }

      const data = await response.json();
      if (data.success) {
        setWishlistData(data.data);
        setIsPasswordProtected(false);
        setAuthError('');
      } else {
        setAuthError('Invalid password');
      }
    } catch (err) {
      console.error('Password authentication failed:', err);
      setAuthError('Invalid password');
    }
  };

  const copyShareUrl = async () => {
    const url = window.location.href;
    try {
      await navigator.clipboard.writeText(url);
      setCopiedUrl(true);
      setTimeout(() => setCopiedUrl(false), 2000);
    } catch (error) {
      console.error('Failed to copy URL:', error);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const priorityColors = {
    LOW: 'bg-gray-100 text-gray-800',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    HIGH: 'bg-orange-100 text-orange-800',
    URGENT: 'bg-red-100 text-red-800',
  };

  // Password protection screen
  if (isPasswordProtected) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8">
          <div className="text-center mb-6">
            <div className="p-3 bg-red-100 rounded-full inline-block mb-4">
              <Lock className="w-6 h-6 text-red-600" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900 mb-2">Private Wishlist</h1>
            <p className="text-gray-600">
              This wishlist is password protected. Please enter the password to view it.
            </p>
          </div>

          <form onSubmit={handlePasswordSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter password"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
              {authError && (
                <p className="text-sm text-red-600 mt-1">{authError}</p>
              )}
            </div>
            
            <Button type="submit" className="w-full">
              Access Wishlist
            </Button>
          </form>
        </div>
      </div>
    );
  }

  // Loading state
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <Heart className="w-12 h-12 text-gray-300 mx-auto mb-4 animate-pulse" />
          <p className="text-gray-600">Loading shared wishlist...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (errorMessage || !wishlistData) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
        <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold text-gray-900 mb-2">Wishlist Not Found</h1>
          <p className="text-gray-600 mb-6">
            {errorMessage || 'This wishlist may have expired or been removed.'}
          </p>
          <Link href="/">
            <Button>Return to Home</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-red-100 rounded-full">
                <Heart className="w-6 h-6 text-red-500 fill-red-500" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">Shared Wishlist</h1>
                <p className="text-sm text-gray-600">
                  {wishlistData.totalItems} item{wishlistData.totalItems !== 1 ? 's' : ''}
                </p>
              </div>
            </div>

            <Button
              variant="outline"
              onClick={copyShareUrl}
              className="flex items-center gap-2"
            >
              <Share2 className="w-4 h-4" />
              {copiedUrl ? 'Copied!' : 'Share'}
            </Button>
          </div>

          {/* Summary Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-6">
            <div className="bg-gray-50 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-gray-900">
                ${wishlistData.totalValue.toFixed(2)}
              </div>
              <div className="text-sm text-gray-600">Total Value</div>
            </div>
            
            <div className="bg-green-50 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-green-600">
                ${wishlistData.totalSavings.toFixed(2)}
              </div>
              <div className="text-sm text-gray-600">Total Savings</div>
            </div>
            
            <div className="bg-orange-50 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-orange-600">
                {wishlistData.itemsWithPriceDrops}
              </div>
              <div className="text-sm text-gray-600">Price Drops</div>
            </div>
            
            <div className="bg-blue-50 rounded-lg p-4 text-center">
              <div className="text-2xl font-bold text-blue-600">
                {wishlistData.inStockItems}
              </div>
              <div className="text-sm text-gray-600">In Stock</div>
            </div>
          </div>
        </div>
      </div>

      {/* Items Grid */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {wishlistData.items.length === 0 ? (
          <div className="text-center py-12">
            <Heart className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Empty Wishlist</h2>
            <p className="text-gray-600">
              This wishlist doesn't have any items yet.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {wishlistData.items.map((item) => (
              <div
                key={item.id}
                className="bg-white rounded-lg shadow-sm overflow-hidden hover:shadow-md transition-shadow"
              >
                <div className="relative aspect-square">
                  <Image
                    src={getImageUrl(item.product.imageUrl)}
                    alt={item.product.name}
                    fill
                    className="object-cover"
                  />
                  {item.isPriceDropped && (
                    <div className="absolute top-2 right-2 bg-red-500 text-white text-xs px-2 py-1 rounded">
                      {Math.round((item.priceDifference / item.priceWhenAdded) * 100)}% OFF
                    </div>
                  )}
                  {item.purchased && (
                    <div className="absolute top-2 left-2 bg-green-500 text-white text-xs px-2 py-1 rounded flex items-center gap-1">
                      <CheckCircle2 className="w-3 h-3" />
                      Purchased
                    </div>
                  )}
                </div>

                <div className="p-4">
                  <Link href={`/product/${item.product.slug || item.product.id}`}>
                    <h3 className="font-semibold text-gray-900 hover:text-blue-600 line-clamp-2 mb-2">
                      {item.product.name}
                    </h3>
                  </Link>

                  <div className="flex items-center gap-2 mb-3">
                    <span className={`text-xs px-2 py-1 rounded-full ${priorityColors[item.priority]}`}>
                      {item.priority}
                    </span>
                    <StockBadge stock={item.product.inStock ? 1 : 0} size="sm" />
                  </div>

                  <div className="space-y-2 mb-3">
                    <div className="flex items-baseline justify-between">
                      <div className="text-xl font-bold text-gray-900">
                        ${item.currentPrice.toFixed(2)}
                      </div>
                      {item.isPriceDropped && (
                        <div className="text-sm text-green-600 font-medium">
                          Save ${item.priceDifference.toFixed(2)}
                        </div>
                      )}
                    </div>
                    
                    {item.targetPrice && (
                      <div className="text-xs text-gray-500">
                        Target: ${item.targetPrice.toFixed(2)}
                      </div>
                    )}
                  </div>

                  {item.notes && (
                    <p className="text-sm text-gray-600 mb-3 line-clamp-2">
                      {item.notes}
                    </p>
                  )}

                  <div className="space-y-2">
                    {!item.purchased && (
                      <Button
                        size="sm"
                        className="w-full flex items-center justify-center gap-2"
                        disabled={!item.product.inStock}
                      >
                        <ShoppingCart className="w-4 h-4" />
                        Add to Cart
                      </Button>
                    )}
                    
                    <Link href={`/product/${item.product.slug || item.product.id}`}>
                      <Button variant="outline" size="sm" className="w-full">
                        <ExternalLink className="w-4 h-4 mr-2" />
                        View Details
                      </Button>
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="bg-white border-t border-gray-200 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm text-gray-600">
              <Eye className="w-4 h-4" />
              <span>Shared publicly</span>
            </div>
            <Link href="/" className="text-sm text-blue-600 hover:text-blue-700">
              Create your own wishlist →
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}