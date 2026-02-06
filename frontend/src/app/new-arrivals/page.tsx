'use client';

import React, { useState, useEffect, useCallback } from 'react';
import { Star, ShoppingCart, ChevronLeft, ChevronRight, Sparkles } from 'lucide-react';
import Link from 'next/link';
import { ProductCard } from '@/components/ProductCard';
import { Product } from '@/types';
import { productsApi } from '@/services/api';
import { SkeletonProductCardGrid } from '@/components/skeletons';
import { EmptyState } from '@/components/EmptyState';

// New Arrivals category name - must match your backend category name (case-insensitive)
const NEW_ARRIVALS_CATEGORY_NAME = 'New Arrivals';

interface PaginationData {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export default function NewArrivalsPage() {
  const [sortBy, setSortBy] = useState('newest');
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [pagination, setPagination] = useState<PaginationData | null>(null);
  const pageSize = 12;

  // Stats for display
  const [stats, setStats] = useState({
    totalProducts: 0,
    avgRating: 0,
    lowestPrice: 0,
  });

  // Fetch products from New Arrivals category by name
  const fetchProducts = useCallback(async () => {
    try {
      setIsLoading(true);
      
      // Determine sort parameters
      let sortByParam = 'createdAt';
      let direction = 'desc';
      
      switch (sortBy) {
        case 'newest':
          sortByParam = 'createdAt';
          direction = 'desc';
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
      const response = await productsApi.getByCategoryName(NEW_ARRIVALS_CATEGORY_NAME, {
        page: currentPage,
        size: pageSize,
        sortBy: sortByParam,
        direction,
      });

      if (response.success && response.data) {
        const productsList = response.data.content || [];
        
        setProducts(productsList);
        setPagination({
          page: response.data.page,
          size: response.data.size,
          totalElements: response.data.totalElements,
          totalPages: response.data.totalPages,
          hasNext: response.data.hasNext,
          hasPrevious: response.data.hasPrevious,
        });

        // Calculate stats
        if (productsList.length > 0) {
          const totalRating = productsList.reduce((sum: number, p: any) => 
            sum + (p.averageRating || p.rating || 0), 0
          );
          const prices = productsList.map((p: any) => p.discountPrice || p.effectivePrice || p.price);
          
          setStats({
            totalProducts: response.data.totalElements,
            avgRating: productsList.length > 0 ? totalRating / productsList.length : 0,
            lowestPrice: Math.min(...prices),
          });
        }
      }
    } catch (error) {
      console.error('Error fetching new arrivals:', error);
      setProducts([]);
      setPagination(null);
    } finally {
      setIsLoading(false);
    }
  }, [currentPage, sortBy]);

  // Fetch products when page or sort changes
  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

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

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Hero Section */}
      <div className="bg-gradient-to-r from-green-600 to-green-800 text-white">
        <div className="w-full px-4 sm:px-6 lg:px-8 py-16">
          <div className="flex items-center gap-4 mb-4">
            <Sparkles className="w-10 h-10" />
            <span className="text-lg font-semibold">JUST ARRIVED</span>
          </div>
          <h1 className="text-4xl sm:text-5xl font-bold mb-4">New Arrivals</h1>
          <p className="text-xl text-green-100">
            Check out our latest collection of premium products, freshly added to our store
          </p>
          {pagination && (
            <p className="text-green-200 mt-2">
              {pagination.totalElements} new product{pagination.totalElements !== 1 ? 's' : ''} available
            </p>
          )}
        </div>
      </div>

      <div className="w-full px-4 sm:px-6 lg:px-8 py-12">
        {/* Quick Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-12">
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <div className="text-4xl font-bold text-green-600 mb-2">
              {stats.totalProducts}
            </div>
            <p className="text-gray-600">New Products</p>
          </div>
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <div className="text-4xl font-bold text-green-600 mb-2">
              {stats.avgRating > 0 ? stats.avgRating.toFixed(1) : '-'}
            </div>
            <p className="text-gray-600">Average Rating</p>
          </div>
          <div className="bg-white rounded-lg shadow-md p-6 text-center">
            <div className="text-4xl font-bold text-green-600 mb-2">
              {stats.lowestPrice > 0 ? `GHS ${stats.lowestPrice.toFixed(2)}` : '-'}
            </div>
            <p className="text-gray-600">Starting Price</p>
          </div>
        </div>

        {/* Filters and Sorting */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-8">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">
                Browse New Arrivals
                {pagination && (
                  <span className="text-base font-normal text-gray-500 ml-2">
                    ({pagination.totalElements} products)
                  </span>
                )}
              </h2>
              <p className="text-gray-600 text-sm mt-1">Discover the latest products added to our catalog</p>
            </div>
            <div className="flex items-center gap-3">
              <label className="text-sm font-medium text-gray-700">Sort by:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              >
                <option value="newest">Newest First</option>
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
              {products.map((product: any, index: number) => (
                <div key={product.id} className="relative group">
                  {index < 3 && currentPage === 0 && (
                    <div className="absolute top-4 left-4 bg-green-600 text-white px-3 py-1 rounded-full text-xs font-bold z-10 flex items-center gap-1">
                      <Star className="w-3 h-3 fill-current" />
                      NEW
                    </div>
                  )}
                  <ProductCard product={product} />
                </div>
              ))}
            </div>

            {/* Pagination */}
            {pagination && pagination.totalPages > 1 && (
              <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-white rounded-xl shadow-sm p-4 mb-12">
                <div className="text-sm text-gray-600">
                  Showing {currentPage * pagination.size + 1}-{Math.min((currentPage + 1) * pagination.size, pagination.totalElements)} of {pagination.totalElements} products
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
                              ? 'bg-green-600 text-white'
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
              icon={<Sparkles className="w-16 h-16 text-gray-300" />}
              title="No new arrivals yet"
              description="We're constantly adding new products. Check back soon for the latest additions!"
              action={{
                label: 'Browse All Products',
                onClick: () => window.location.href = '/products',
              }}
            />
          </div>
        )}

        {/* Call to Action */}
        <div className="bg-gradient-to-r from-green-600 to-green-800 text-white rounded-lg p-8 text-center">
          <h2 className="text-2xl font-bold mb-2">Discover What's New</h2>
          <p className="mb-6 text-green-100">
            New products arrive weekly! Be the first to know by following us on social media
          </p>
          <div className="flex justify-center gap-4">
            <Link href="/products">
              <button className="px-6 py-3 bg-white text-green-600 rounded-lg hover:bg-gray-100 transition-colors font-semibold flex items-center gap-2">
                <ShoppingCart className="w-5 h-5" />
                Start Shopping
              </button>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
