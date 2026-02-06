'use client';

import React, { useState, useEffect, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { productsApi, categoriesApi } from '@/services/api';
import { useDebounce } from '@/lib/hooks';
import { ProductCard } from '@/components/ProductCard';
import { SkeletonProductCardGrid } from '@/components/skeletons';
import { EmptyState } from '@/components/EmptyState';

// Sort options
const sortOptions = [
  { value: 'name-asc', label: 'Name: A to Z', sortBy: 'name', direction: 'asc' },
  { value: 'name-desc', label: 'Name: Z to A', sortBy: 'name', direction: 'desc' },
  { value: 'price-asc', label: 'Price: Low to High', sortBy: 'price', direction: 'asc' },
  { value: 'price-desc', label: 'Price: High to Low', sortBy: 'price', direction: 'desc' },
  { value: 'createdAt-desc', label: 'Newest First', sortBy: 'createdAt', direction: 'desc' },
  { value: 'createdAt-asc', label: 'Oldest First', sortBy: 'createdAt', direction: 'asc' },
  { value: 'averageRating-desc', label: 'Highest Rated', sortBy: 'averageRating', direction: 'desc' },
  { value: 'salesCount-desc', label: 'Best Selling', sortBy: 'salesCount', direction: 'desc' },
];

// Rating filter options
const ratingOptions = [
  { value: '', label: 'All Ratings' },
  { value: '4', label: '4★ & Up' },
  { value: '3', label: '3★ & Up' },
  { value: '2', label: '2★ & Up' },
  { value: '1', label: '1★ & Up' },
];

// Inventory status options
const inventoryStatusOptions = [
  { value: '', label: 'All Stock Status' },
  { value: 'IN_STOCK', label: 'In Stock' },
  { value: 'LOW_STOCK', label: 'Low Stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of Stock' },
  { value: 'PRE_ORDER', label: 'Pre-Order' },
  { value: 'BACKORDER', label: 'Backorder' },
];

interface Category {
  id: number;
  name: string;
  slug?: string;
}

interface Product {
  id: number;
  name: string;
  slug?: string;
  description?: string;
  sku?: string;
  price: number;
  effectivePrice?: number;
  discountPrice?: number;
  originalPrice?: number;
  images?: string[];
  imageUrl?: string;
  image?: string;
  category?: Category;
  categoryName?: string;
  categoryId?: number;
  averageRating?: number;
  rating?: number;
  reviewCount?: number;
  reviews?: number;
  stockQuantity?: number;
  inStock?: boolean;
  stockStatus?: string;
  featured?: boolean;
  discountPercentage?: number;
  promotion?: any;
  createdAt?: string;
  updatedAt?: string;
}

interface PaginationData {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// Helper function to normalize price value (handles both number and object formats)
const normalizePrice = (price: any): number => {
  if (typeof price === 'number') return price;
  if (price && typeof price === 'object' && 'parsedValue' in price) return price.parsedValue;
  if (price && typeof price === 'object' && 'source' in price) return parseFloat(price.source) || 0;
  return parseFloat(price) || 0;
};

// Helper function to normalize a product object
const normalizeProduct = (product: any): Product => ({
  ...product,
  price: normalizePrice(product.price),
  effectivePrice: normalizePrice(product.effectivePrice),
  discountPrice: product.discountPrice ? normalizePrice(product.discountPrice) : undefined,
  originalPrice: product.originalPrice ? normalizePrice(product.originalPrice) : undefined,
});

function ProductsPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // State for filters
  const [searchQuery, setSearchQuery] = useState(searchParams.get('q') || '');
  const [selectedCategory, setSelectedCategory] = useState<number | null>(
    searchParams.get('category') ? parseInt(searchParams.get('category')!) : null
  );
  const [minPrice, setMinPrice] = useState(searchParams.get('minPrice') || '');
  const [maxPrice, setMaxPrice] = useState(searchParams.get('maxPrice') || '');
  const [featuredOnly, setFeaturedOnly] = useState(searchParams.get('featured') === 'true');
  const [inStockOnly, setInStockOnly] = useState(searchParams.get('inStock') === 'true');
  const [onSaleOnly, setOnSaleOnly] = useState(searchParams.get('onSale') === 'true');
  const [minRating, setMinRating] = useState(searchParams.get('rating') || '');
  const [inventoryStatus, setInventoryStatus] = useState(searchParams.get('status') || '');
  const [sortValue, setSortValue] = useState(searchParams.get('sort') || 'createdAt-desc');
  const [currentPage, setCurrentPage] = useState(
    parseInt(searchParams.get('page') || '0')
  );

  // State for data
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [pagination, setPagination] = useState<PaginationData | null>(null);
  const [loading, setLoading] = useState(true);
  const [categoriesLoading, setCategoriesLoading] = useState(true);

  // View state
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');

  // Debounce search and price inputs
  const debouncedSearch = useDebounce(searchQuery, 300);
  const debouncedMinPrice = useDebounce(minPrice, 300);
  const debouncedMaxPrice = useDebounce(maxPrice, 300);

  const pageSize = 12;

  // Get sort params from value
  const getSortParams = (value: string) => {
    const option = sortOptions.find((o) => o.value === value);
    return option ? { sortBy: option.sortBy.trim(), direction: option.direction.trim() } : { sortBy: 'createdAt', direction: 'desc' };
  };

  // Update URL with current filters
  const updateUrl = useCallback(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set('q', debouncedSearch);
    if (selectedCategory) params.set('category', selectedCategory.toString());
    if (debouncedMinPrice) params.set('minPrice', debouncedMinPrice);
    if (debouncedMaxPrice) params.set('maxPrice', debouncedMaxPrice);
    if (featuredOnly) params.set('featured', 'true');
    if (inStockOnly) params.set('inStock', 'true');
    if (onSaleOnly) params.set('onSale', 'true');
    if (minRating) params.set('rating', minRating);
    if (inventoryStatus) params.set('status', inventoryStatus);
    if (sortValue !== 'createdAt-desc') params.set('sort', sortValue);
    if (currentPage > 0) params.set('page', currentPage.toString());
    
    const queryString = params.toString();
    router.replace(`/products${queryString ? `?${queryString}` : ''}`, { scroll: false });
  }, [debouncedSearch, selectedCategory, debouncedMinPrice, debouncedMaxPrice, featuredOnly, inStockOnly, onSaleOnly, minRating, inventoryStatus, sortValue, currentPage, router]);

  // Fetch categories for filter dropdown
  useEffect(() => {
    const fetchCategories = async () => {
      setCategoriesLoading(true);
      try {
        const response = await categoriesApi.getAll({ size: 100, isActive: true });
        if (response.success && response.data?.content) {
          setCategories(response.data.content);
        }
      } catch (error) {
        console.error('Failed to fetch categories:', error);
      } finally {
        setCategoriesLoading(false);
      }
    };
    fetchCategories();
  }, []);

  // Fetch products based on current filters
  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const { sortBy, direction } = getSortParams(sortValue);
      let response;

      // Determine which endpoint to use based on filters
      if (debouncedSearch) {
        // Search endpoint
        response = await productsApi.search({
          q: debouncedSearch,
          page: currentPage,
          size: pageSize,
          sortBy,
          direction,
        });
      } else if (debouncedMinPrice || debouncedMaxPrice) {
        // Price range endpoint
        response = await productsApi.getByPriceRange({
          minPrice: parseFloat(debouncedMinPrice) || 0,
          maxPrice: parseFloat(debouncedMaxPrice) || 999999999,
          page: currentPage,
          size: pageSize,
          sortBy,
          direction,
        });
      } else if (selectedCategory) {
        // Category endpoint - try path parameter first, fallback to query parameter
        try {
          response = await productsApi.getByCategory(selectedCategory, {
            page: currentPage,
            size: pageSize,
            sortBy,
            direction,
          });
        } catch (categoryError) {
          // Fallback to query parameter approach
          console.log('Falling back to query parameter for category filter');
          response = await productsApi.getByCategoryQuery(selectedCategory, {
            page: currentPage,
            size: pageSize,
            sortBy,
            direction,
          });
        }
      } else if (featuredOnly) {
        // Featured endpoint
        response = await productsApi.getFeatured({
          page: currentPage,
          size: pageSize,
        });
      } else if (inventoryStatus) {
        // Inventory status endpoint
        response = await productsApi.getByInventoryStatus(inventoryStatus, {
          page: currentPage,
          size: pageSize,
          sortBy,
          direction,
        });
      } else {
        // Default: get all products
        response = await productsApi.getAll({
          page: currentPage,
          size: pageSize,
          sortBy,
          direction,
        });
      }

      if (response.success && response.data) {
        let productsList = (response.data.content || []).map(normalizeProduct);
        
        // Apply client-side filters
        if (inStockOnly) {
          productsList = productsList.filter((p: any) => 
            p.stockQuantity > 0 || p.inStock === true || p.stockStatus === 'IN_STOCK'
          );
        }
        if (onSaleOnly) {
          productsList = productsList.filter((p: any) => 
            (p.discountPercentage && p.discountPercentage > 0) || 
            (p.originalPrice && p.price < p.originalPrice)
          );
        }
        if (minRating) {
          const ratingThreshold = parseFloat(minRating);
          productsList = productsList.filter((p: any) => 
            (p.averageRating || p.rating || 0) >= ratingThreshold
          );
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
      console.error('Failed to fetch products:', error);
      setProducts([]);
      setPagination(null);
    } finally {
      setLoading(false);
    }
  }, [debouncedSearch, selectedCategory, debouncedMinPrice, debouncedMaxPrice, featuredOnly, inStockOnly, onSaleOnly, minRating, inventoryStatus, sortValue, currentPage]);

  // Fetch products when filters change
  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Update URL when filters change
  useEffect(() => {
    updateUrl();
  }, [updateUrl]);

  // Sync state with URL params when URL changes (e.g., navigation from categories page)
  useEffect(() => {
    const categoryParam = searchParams.get('category');
    const newCategory = categoryParam ? parseInt(categoryParam) : null;
    if (newCategory !== selectedCategory) {
      setSelectedCategory(newCategory);
    }
    
    const queryParam = searchParams.get('q') || '';
    if (queryParam !== searchQuery) {
      setSearchQuery(queryParam);
    }
    
    const pageParam = parseInt(searchParams.get('page') || '0');
    if (pageParam !== currentPage) {
      setCurrentPage(pageParam);
    }
  }, [searchParams]);

  // Reset to first page when filters change (except page itself)
  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearch, selectedCategory, debouncedMinPrice, debouncedMaxPrice, featuredOnly, inStockOnly, onSaleOnly, minRating, inventoryStatus, sortValue]);

  // Clear all filters
  const clearFilters = () => {
    setSearchQuery('');
    setSelectedCategory(null);
    setMinPrice('');
    setMaxPrice('');
    setFeaturedOnly(false);
    setInStockOnly(false);
    setOnSaleOnly(false);
    setMinRating('');
    setInventoryStatus('');
    setSortValue('createdAt-desc');
    setCurrentPage(0);
  };

  // Check if any filters are active
  const hasActiveFilters = searchQuery || selectedCategory || minPrice || maxPrice || featuredOnly || inStockOnly || onSaleOnly || minRating || inventoryStatus;

  // Generate page numbers for pagination
  const getPageNumbers = () => {
    if (!pagination) return [];
    const pages: number[] = [];
    const total = pagination.totalPages;
    const current = pagination.page;

    if (total <= 7) {
      for (let i = 0; i < total; i++) pages.push(i);
    } else {
      if (current <= 3) {
        for (let i = 0; i < 5; i++) pages.push(i);
        pages.push(-1); // ellipsis
        pages.push(total - 1);
      } else if (current >= total - 4) {
        pages.push(0);
        pages.push(-1);
        for (let i = total - 5; i < total; i++) pages.push(i);
      } else {
        pages.push(0);
        pages.push(-1);
        for (let i = current - 1; i <= current + 1; i++) pages.push(i);
        pages.push(-2);
        pages.push(total - 1);
      }
    }
    return pages;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Page Header */}
      <div className="bg-white border-b">
        <div className="w-full px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center gap-2 text-sm text-gray-500 mb-2">
            <Link href="/" className="hover:text-primary">Home</Link>
            <span>/</span>
            <span className="text-gray-900">Products</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">All Products</h1>
          {pagination && (
            <p className="mt-1 text-sm text-gray-500">
              Showing {products.length} of {pagination.totalElements} products
            </p>
          )}
        </div>
      </div>

      <div className="w-full px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex flex-col lg:flex-row gap-6">
          {/* Sidebar Filters */}
          <div className="lg:w-64 flex-shrink-0">
            <div className="bg-white rounded-lg shadow p-4 sticky top-4">
              <div className="flex items-center justify-between mb-4">
                <h2 className="font-semibold text-gray-900">Filters</h2>
                {hasActiveFilters && (
                  <button
                    onClick={clearFilters}
                    className="text-sm text-primary hover:text-primary/80"
                  >
                    Clear all
                  </button>
                )}
              </div>

              {/* Search */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Search
                </label>
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search products..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                />
              </div>

              {/* Category Filter */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Category
                </label>
                <select
                  value={selectedCategory || ''}
                  onChange={(e) => setSelectedCategory(e.target.value ? parseInt(e.target.value) : null)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                  disabled={categoriesLoading}
                >
                  <option value="">All Categories</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Price Range */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Price Range (GHS)
                </label>
                <div className="flex gap-2">
                  <input
                    type="number"
                    value={minPrice}
                    onChange={(e) => setMinPrice(e.target.value)}
                    placeholder="Min"
                    min="0"
                    className="w-1/2 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                  <input
                    type="number"
                    value={maxPrice}
                    onChange={(e) => setMaxPrice(e.target.value)}
                    placeholder="Max"
                    min="0"
                    className="w-1/2 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                  />
                </div>
              </div>

              {/* Featured Only */}
              <div className="mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={featuredOnly}
                    onChange={(e) => setFeaturedOnly(e.target.checked)}
                    className="w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary"
                  />
                  <span className="text-sm text-gray-700">Featured Only</span>
                </label>
              </div>

              {/* In Stock Only */}
              <div className="mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={inStockOnly}
                    onChange={(e) => setInStockOnly(e.target.checked)}
                    className="w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary"
                  />
                  <span className="text-sm text-gray-700">In Stock Only</span>
                </label>
              </div>

              {/* On Sale Only */}
              <div className="mb-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={onSaleOnly}
                    onChange={(e) => setOnSaleOnly(e.target.checked)}
                    className="w-4 h-4 text-primary border-gray-300 rounded focus:ring-primary"
                  />
                  <span className="text-sm text-gray-700">On Sale</span>
                </label>
              </div>

              {/* Rating Filter */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Rating
                </label>
                <select
                  value={minRating}
                  onChange={(e) => setMinRating(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                >
                  {ratingOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Inventory Status Filter */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Stock Status
                </label>
                <select
                  value={inventoryStatus}
                  onChange={(e) => setInventoryStatus(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                >
                  {inventoryStatusOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* Active Filters Tags */}
              {hasActiveFilters && (
                <div className="pt-4 border-t">
                  <p className="text-xs text-gray-500 mb-2">Active Filters:</p>
                  <div className="flex flex-wrap gap-2">
                    {searchQuery && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-primary/10 text-primary text-xs rounded-full">
                        Search: {searchQuery}
                        <button
                          onClick={() => setSearchQuery('')}
                          className="hover:text-primary/80"
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {selectedCategory && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-primary/10 text-primary text-xs rounded-full">
                        Category: {categories.find((c) => c.id === selectedCategory)?.name}
                        <button
                          onClick={() => setSelectedCategory(null)}
                          className="hover:text-primary/80"
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {(minPrice || maxPrice) && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-primary/10 text-primary text-xs rounded-full">
                        Price: GHS {minPrice || '0'} - {maxPrice || '∞'}
                        <button
                          onClick={() => { setMinPrice(''); setMaxPrice(''); }}
                          className="hover:text-primary/80"
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {featuredOnly && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-primary/10 text-primary text-xs rounded-full">
                        Featured
                        <button
                          onClick={() => setFeaturedOnly(false)}
                          className="hover:text-primary/80"
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {inStockOnly && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-green-100 text-green-700 text-xs rounded-full">
                        In Stock
                        <button
                          onClick={() => setInStockOnly(false)}
                          className="hover:text-green-900"
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {onSaleOnly && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-red-100 text-red-700 text-xs rounded-full">
                        On Sale
                        <button
                          onClick={() => setOnSaleOnly(false)}
                          className="hover:text-red-900"
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {minRating && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-yellow-100 text-yellow-700 text-xs rounded-full">
                        {minRating}★ & Up
                        <button
                          onClick={() => setMinRating('')}
                          className="hover:text-yellow-900"
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {inventoryStatus && (
                      <span className="inline-flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded-full">
                        {inventoryStatusOptions.find(o => o.value === inventoryStatus)?.label}
                        <button
                          onClick={() => setInventoryStatus('')}
                          className="hover:text-blue-900"
                        >
                          ×
                        </button>
                      </span>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Main Content */}
          <div className="flex-1">
            {/* Toolbar */}
            <div className="bg-white rounded-lg shadow p-4 mb-4 flex flex-wrap items-center justify-between gap-4">
              {/* Sort */}
              <div className="flex items-center gap-2">
                <label className="text-sm text-gray-600">Sort by:</label>
                <select
                  value={sortValue}
                  onChange={(e) => setSortValue(e.target.value)}
                  className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                >
                  {sortOptions.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>

              {/* View Toggle */}
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setViewMode('grid')}
                  className={`p-2 rounded-lg ${
                    viewMode === 'grid'
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                  title="Grid view"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"
                    />
                  </svg>
                </button>
                <button
                  onClick={() => setViewMode('list')}
                  className={`p-2 rounded-lg ${
                    viewMode === 'list'
                      ? 'bg-primary text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                  title="List view"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 6h16M4 12h16M4 18h16"
                    />
                  </svg>
                </button>
              </div>
            </div>

            {/* Products Grid/List */}
            {loading ? (
              <SkeletonProductCardGrid count={pageSize} />
            ) : products.length === 0 ? (
              <EmptyState
                title="No products found"
                description={
                  hasActiveFilters
                    ? 'Try adjusting your filters or search query'
                    : 'Check back later for new products'
                }
                action={hasActiveFilters ? { label: 'Clear Filters', onClick: clearFilters } : undefined}
              />
            ) : viewMode === 'grid' ? (
              <div className="grid grid-cols-2 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product as any} />
                ))}
              </div>
            ) : (
              <div className="space-y-4">
                {products.map((product) => (
                  <div
                    key={product.id}
                    className="bg-white rounded-lg shadow p-4 flex gap-4 hover:shadow-md transition-shadow"
                  >
                    <Link
                      href={`/products/${product.slug || product.id}`}
                      className="w-32 h-32 flex-shrink-0"
                    >
                      <img
                        src={
                          product.images?.[0] ||
                          product.imageUrl ||
                          '/placeholder.png'
                        }
                        alt={product.name}
                        className="w-full h-full object-cover rounded-lg"
                      />
                    </Link>
                    <div className="flex-1 min-w-0">
                      <Link
                        href={`/products/${product.slug || product.id}`}
                        className="text-lg font-semibold text-gray-900 hover:text-primary line-clamp-1"
                      >
                        {product.name}
                      </Link>
                      {product.category && (
                        <p className="text-sm text-gray-500 mt-1">
                          {product.category.name}
                        </p>
                      )}
                      <div className="flex items-center gap-2 mt-2">
                        <span className="text-xl font-bold text-primary">
                          GHS {product.price?.toFixed(2)}
                        </span>
                        {product.originalPrice && product.originalPrice > product.price && (
                          <span className="text-sm text-gray-400 line-through">
                            GHS {product.originalPrice.toFixed(2)}
                          </span>
                        )}
                      </div>
                      {product.averageRating !== undefined && product.averageRating > 0 && (
                        <div className="flex items-center gap-1 mt-2">
                          <div className="flex">
                            {[...Array(5)].map((_, i) => (
                              <svg
                                key={i}
                                className={`w-4 h-4 ${
                                  i < Math.round(product.averageRating!)
                                    ? 'text-yellow-400'
                                    : 'text-gray-300'
                                }`}
                                fill="currentColor"
                                viewBox="0 0 20 20"
                              >
                                <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                              </svg>
                            ))}
                          </div>
                          <span className="text-sm text-gray-500">
                            ({product.reviewCount || 0})
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Pagination */}
            {pagination && pagination.totalPages > 1 && (
              <div className="mt-6 flex items-center justify-center gap-2">
                <button
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                  disabled={!pagination.hasPrevious}
                  className="px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>

                <div className="flex items-center gap-1">
                  {getPageNumbers().map((pageNum, idx) =>
                    pageNum < 0 ? (
                      <span key={`ellipsis-${idx}`} className="px-2 text-gray-400">
                        ...
                      </span>
                    ) : (
                      <button
                        key={pageNum}
                        onClick={() => setCurrentPage(pageNum)}
                        className={`px-3 py-2 text-sm font-medium rounded-lg ${
                          pageNum === pagination.page
                            ? 'bg-primary text-white'
                            : 'text-gray-700 bg-white border border-gray-300 hover:bg-gray-50'
                        }`}
                      >
                        {pageNum + 1}
                      </button>
                    )
                  )}
                </div>

                <button
                  onClick={() => setCurrentPage((p) => p + 1)}
                  disabled={!pagination.hasNext}
                  className="px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            )}

            {/* Results Summary */}
            {pagination && (
              <div className="mt-4 text-center text-sm text-gray-500">
                Page {pagination.page + 1} of {pagination.totalPages} ({pagination.totalElements} total products)
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ProductsPage() {
  return (
    <Suspense fallback={<div className="min-h-screen bg-gray-50 flex items-center justify-center"><SkeletonProductCardGrid count={12} /></div>}>
      <ProductsPageContent />
    </Suspense>
  );
}
