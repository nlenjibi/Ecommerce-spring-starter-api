'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Zap, TrendingDown, Gift, Clock, ChevronLeft, ChevronRight } from 'lucide-react';
import { ProductCard } from '@/components/ProductCard';
import { Product } from '@/types';
import { productsApi } from '@/services/api';
import { SkeletonProductCardGrid } from '@/components/skeletons';
import { EmptyState } from '@/components/EmptyState';

// Deals category name - must match your backend category name (case-insensitive)
const DEALS_CATEGORY_NAME = 'Deals';

interface PaginationData {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export default function DealsPage() {
  const [sortBy, setSortBy] = useState('discount');
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [pagination, setPagination] = useState<PaginationData | null>(null);
  const pageSize = 12;

  // Fetch products from deals category by name
  const fetchDealsProducts = useCallback(async () => {
    try {
      setIsLoading(true);
      
      // Determine sort parameters
      let sortByParam = 'createdAt';
      let direction = 'desc';
      
      switch (sortBy) {
        case 'discount':
          sortByParam = 'discountPrice';
          direction = 'asc';
          break;
        case 'price-low':
          sortByParam = 'price';
          direction = 'asc';
          break;
        case 'price-high':
          sortByParam = 'price';
          direction = 'desc';
          break;
        case 'rating':
          sortByParam = 'averageRating';
          direction = 'desc';
          break;
      }

      // Fetch products by category name
      const response = await productsApi.getByCategoryName(DEALS_CATEGORY_NAME, {
        page: currentPage,
        size: pageSize,
        sortBy: sortByParam,
        direction,
      });

      if (response.success && response.data) {
        let productsList = response.data.content || [];
        
        // Client-side sort for discount if needed (since backend might not support it)
        if (sortBy === 'discount') {
          productsList = [...productsList].sort((a: any, b: any) => {
            const discountA = a.discountPrice ? ((a.price - a.discountPrice) / a.price) * 100 : 0;
            const discountB = b.discountPrice ? ((b.price - b.discountPrice) / b.price) * 100 : 0;
            return discountB - discountA;
          });
        }
        
        setProducts(productsList);
        setPagination({
          page: response.data.page,
          size: response.data.size,
          totalElements: response.data.totalElements,
          totalPages: response.data.totalPages,
          hasNext: response.data.hasNext,
          hasPrevious: response.data.hasPrevious,
        });
      }
    } catch (error) {
      console.error('Error fetching deals products:', error);
      setProducts([]);
      setPagination(null);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, sortBy]);

  // Fetch products when page or sort changes
  useEffect(() => {
    fetchDealsProducts();
  }, [fetchDealsProducts]);

  // Reset to first page when sort changes
  useEffect(() => {
    setCurrentPage(0);
  }, [sortBy]);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 0 && pagination && newPage < pagination.totalPages) {
      setCurrentPage(newPage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  // Calculate discount percentage for display
  const getDiscountPercent = (product: any) => {
    if (product.discountPrice && product.price > product.discountPrice) {
      return Math.round(((product.price - product.discountPrice) / product.price) * 100);
    }
    if (product.originalPrice && product.originalPrice > product.price) {
      return Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100);
    }
    if (product.discountPercentage) {
      return Math.round(product.discountPercentage);
    }
    return 0;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero Section */}
      <div className="bg-gradient-to-r from-red-600 to-red-800 text-white">
        <div className="w-full px-4 sm:px-6 lg:px-8 py-16">
          <div className="flex items-center gap-4 mb-4">
            <Zap className="w-10 h-10" />
            <span className="text-lg font-semibold">LIMITED TIME OFFERS</span>
          </div>
          <h1 className="text-4xl sm:text-5xl font-bold mb-4">Amazing Deals & Offers</h1>
          <p className="text-xl text-red-100 mb-6">
            Save big on thousands of products. Shop our hottest deals before they're gone!
          </p>
          {pagination && (
            <p className="text-red-200">
              {pagination.totalElements} deal{pagination.totalElements !== 1 ? 's' : ''} available
            </p>
          )}
        </div>
      </div>

      <div className="w-full px-4 sm:px-6 lg:px-8 py-12">
        {/* Deal Types */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-12">
          <div className="bg-white rounded-lg shadow-md p-6 border-l-4 border-red-600">
            <div className="flex items-center gap-3 mb-2">
              <TrendingDown className="w-6 h-6 text-red-600" />
              <h3 className="font-semibold text-gray-900">Up to 60% Off</h3>
            </div>
            <p className="text-sm text-gray-600">On selected items</p>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6 border-l-4 border-orange-600">
            <div className="flex items-center gap-3 mb-2">
              <Gift className="w-6 h-6 text-orange-600" />
              <h3 className="font-semibold text-gray-900">Bundle Deals</h3>
            </div>
            <p className="text-sm text-gray-600">Buy more, save more</p>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6 border-l-4 border-yellow-600">
            <div className="flex items-center gap-3 mb-2">
              <Clock className="w-6 h-6 text-yellow-600" />
              <h3 className="font-semibold text-gray-900">Flash Sales</h3>
            </div>
            <p className="text-sm text-gray-600">Limited time only</p>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6 border-l-4 border-pink-600">
            <div className="flex items-center gap-3 mb-2">
              <Zap className="w-6 h-6 text-pink-600" />
              <h3 className="font-semibold text-gray-900">Clearance</h3>
            </div>
            <p className="text-sm text-gray-600">Up to 70% off</p>
          </div>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <h2 className="text-2xl font-bold text-gray-900">
              Current Deals
              {pagination && (
                <span className="text-base font-normal text-gray-500 ml-2">
                  ({pagination.totalElements} products)
                </span>
              )}
            </h2>
            <div className="flex items-center gap-3">
              <label className="text-sm font-medium text-gray-700">Sort by:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500"
              >
                <option value="discount">Biggest Discount</option>
                <option value="price-low">Price: Low to High</option>
                <option value="price-high">Price: High to Low</option>
                <option value="rating">Top Rated</option>
              </select>
            </div>
          </div>
        </div>

        {/* Products Grid */}
        {isLoading ? (
          <div className="mb-12">
            <SkeletonProductCardGrid count={8} />
          </div>
        ) : products.length > 0 ? (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
              {products.map((product: any) => (
                <div key={product.id} className="relative">
                  <ProductCard product={product} />
                  {getDiscountPercent(product) > 0 && (
                    <div className="absolute top-4 right-4 bg-red-600 text-white px-3 py-1 rounded-full text-sm font-bold z-10">
                      -{getDiscountPercent(product)}%
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Pagination */}
            {pagination && pagination.totalPages > 1 && (
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white rounded-xl shadow-sm p-4 mb-12">
                <div className="text-sm text-gray-600">
                  Showing {currentPage * pagination.size + 1}-{Math.min((currentPage + 1) * pagination.size, pagination.totalElements)} of {pagination.totalElements} deals
                </div>
                
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={!pagination.hasPrevious}
                    className={`p-2 rounded-lg border ${
                      pagination.hasPrevious
                        ? 'border-gray-300 hover:bg-gray-50 text-gray-700'
                        : 'border-gray-200 text-gray-300 cursor-not-allowed'
                    }`}
                  >
                    <ChevronLeft className="w-5 h-5" />
                  </button>

                  <div className="flex items-center gap-1">
                    {Array.from({ length: Math.min(pagination.totalPages, 5) }, (_, i) => {
                      let pageNum = i;
                      if (pagination.totalPages > 5) {
                        if (currentPage < 3) {
                          pageNum = i;
                        } else if (currentPage > pagination.totalPages - 4) {
                          pageNum = pagination.totalPages - 5 + i;
                        } else {
                          pageNum = currentPage - 2 + i;
                        }
                      }
                      return (
                        <button
                          key={pageNum}
                          onClick={() => handlePageChange(pageNum)}
                          className={`min-w-[40px] h-10 rounded-lg text-sm font-medium transition-colors ${
                            currentPage === pageNum
                              ? 'bg-red-600 text-white'
                              : 'hover:bg-gray-100 text-gray-700'
                          }`}
                        >
                          {pageNum + 1}
                        </button>
                      );
                    })}
                  </div>

                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={!pagination.hasNext}
                    className={`p-2 rounded-lg border ${
                      pagination.hasNext
                        ? 'border-gray-300 hover:bg-gray-50 text-gray-700'
                        : 'border-gray-200 text-gray-300 cursor-not-allowed'
                    }`}
                  >
                    <ChevronRight className="w-5 h-5" />
                  </button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="mb-12">
            <EmptyState
              icon={<Zap className="w-16 h-16 text-gray-300" />}
              title="No deals available"
              description="Check back later for amazing deals and offers. We're always adding new discounts!"
              action={{
                label: 'Browse All Products',
                onClick: () => window.location.href = '/products',
              }}
            />
          </div>
        )}

        {/* Newsletter */}
        <div className="bg-gradient-to-r from-red-600 to-red-800 text-white rounded-lg p-8 text-center">
          <h2 className="text-2xl font-bold mb-2">Don't Miss Out on Great Deals</h2>
          <p className="mb-6">Subscribe to get notified about our latest deals and offers</p>
          <div className="flex flex-col sm:flex-row gap-3 max-w-md mx-auto">
            <input
              type="email"
              placeholder="Enter your email"
              className="flex-1 px-4 py-3 rounded-lg text-gray-900 focus:outline-none focus:ring-2 focus:ring-white"
            />
            <button className="px-6 py-3 bg-white text-red-600 rounded-lg hover:bg-gray-100 transition-colors font-semibold">
              Subscribe
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
