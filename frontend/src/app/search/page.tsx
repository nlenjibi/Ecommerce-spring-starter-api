'use client';

import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { Search as SearchIcon, X } from 'lucide-react';
import { ProductCard } from '@/components/ProductCard';
import { Product } from '@/types';
import { productsApi } from '@/services/api';

const sortOptions = [
  { value: 'relevance', label: 'Most Relevant' },
  { value: 'price-low', label: 'Price: Low to High' },
  { value: 'price-high', label: 'Price: High to Low' },
  { value: 'rating', label: 'Highest Rated' },
  { value: 'newest', label: 'Newest' },
];

const inventoryStatusOptions = [
  { value: '', label: 'All Status' },
  { value: 'IN_STOCK', label: 'In Stock' },
  { value: 'LOW_STOCK', label: 'Low Stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of Stock' },
  { value: 'DISCONTINUED', label: 'Discontinued' },
  { value: 'PRE_ORDER', label: 'Pre-Order' },
  { value: 'BACKORDER', label: 'Backorder' },
];

export default function SearchPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const initialQuery = searchParams.get('q') || '';
  
  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [debouncedQuery, setDebouncedQuery] = useState(initialQuery);
  const [allProducts, setAllProducts] = useState<Product[]>([]);
  const [filteredProducts, setFilteredProducts] = useState<Product[]>([]);
  const [suggestions, setSuggestions] = useState<Product[]>([]);
  const [isSuggesting, setIsSuggesting] = useState(false);
  const [sortBy, setSortBy] = useState('relevance');
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 500]);
  const [priceBounds, setPriceBounds] = useState<[number, number]>([0, 500]);
  const [inventoryStatus, setInventoryStatus] = useState('');
  const [needsReorder, setNeedsReorder] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isPriceLoading, setIsPriceLoading] = useState(false);
  const [activeFilters, setActiveFilters] = useState<Set<string>>(new Set());
  const [isAdmin, setIsAdmin] = useState(false);
  const debounceRef = useRef<number | null>(null);

  // Check admin status on component mount
  useEffect(() => {
    const checkAdminStatus = () => {
      // Check if user is authenticated and has admin role
      if (typeof window !== 'undefined') {
        const tokens = localStorage.getItem('auth_tokens');
        if (tokens) {
          try {
            const { accessToken } = JSON.parse(tokens);
            // Simple token payload parsing (in production, verify properly)
            const payload = JSON.parse(atob(accessToken.split('.')[1]));
            setIsAdmin(payload.role === 'ADMIN' || payload.authorities?.includes('ROLE_ADMIN'));
          } catch (error) {
            console.error('Error parsing token:', error);
            setIsAdmin(false);
          }
        }
      }
    };

    checkAdminStatus();
  }, []);

  // Fetch price bounds on component mount
  useEffect(() => {
    const fetchPriceBounds = async () => {
      try {
        setIsPriceLoading(true);
        // Get a large sample of products to determine price bounds
        const response = await productsApi.getAll({ page: 0, size: 1000 });
        const products = response.data?.content || [];
        
        if (products.length > 0) {
          const prices = products.map(p => p.effectivePrice || p.price || 0);
          const minPrice = Math.min(...prices);
          const maxPrice = Math.max(...prices);
          
          // Round bounds to nice numbers
          const roundedMin = Math.floor(minPrice / 10) * 10;
          const roundedMax = Math.ceil(maxPrice / 10) * 10;
          
          setPriceBounds([roundedMin, roundedMax]);
          setPriceRange([roundedMin, roundedMax]);
        }
      } catch (error) {
        console.error('Error fetching price bounds:', error);
        // Keep default values if API fails
      } finally {
        setIsPriceLoading(false);
      }
    };
    
    fetchPriceBounds();
  }, []);

  // Debounce the search query to avoid spamming API on every keystroke
  useEffect(() => {
    if (debounceRef.current) window.clearTimeout(debounceRef.current);
    // wait 300ms after the last keystroke
    debounceRef.current = window.setTimeout(() => {
      setDebouncedQuery(searchQuery.trim());
    }, 300) as unknown as number;

    return () => {
      if (debounceRef.current) window.clearTimeout(debounceRef.current);
    };
  }, [searchQuery]);

  // Multi-filter search implementation
  useEffect(() => {
    const fetchProducts = async () => {
      const filters = new Set<string>();
      
      // Track active filters for display
      if (debouncedQuery) filters.add('search');
      if (inventoryStatus) filters.add('inventory');
      if (priceRange[0] > priceBounds[0] || priceRange[1] < priceBounds[1]) filters.add('price');
      if (needsReorder && isAdmin) filters.add('reorder');
      setActiveFilters(filters);

      // If no active filters, show empty state
      if (filters.size === 0) {
        setFilteredProducts([]);
        setSuggestions([]);
        setIsSuggesting(false);
        return;
      }

      try {
        setIsLoading(true);
        const [sort, direction] = sortBy.split('-') as [string, 'ASC' | 'DESC' | undefined];
        let allResults: Product[] = [];

        // Execute API calls based on active filters
        const apiCalls = [];

        if (debouncedQuery) {
          apiCalls.push(
            productsApi.search({
              q: debouncedQuery,
              sortBy: sort,
              direction: direction?.toUpperCase() as 'ASC' | 'DESC' | undefined,
              page: 0,
              size: 50, // Larger size for better filtering results
            })
          );
        }

        if (inventoryStatus) {
          apiCalls.push(
            productsApi.getByInventoryStatus(inventoryStatus, {
              sortBy: sort,
              direction: direction?.toUpperCase() as 'ASC' | 'DESC' | undefined,
              page: 0,
              size: 50,
            })
          );
        }

        if (priceRange[0] > priceBounds[0] || priceRange[1] < priceBounds[1]) {
          apiCalls.push(
            productsApi.getByPriceRange({
              minPrice: priceRange[0],
              maxPrice: priceRange[1],
              sortBy: sort,
              direction: direction?.toUpperCase() as 'ASC' | 'DESC' | undefined,
              page: 0,
              size: 50,
            })
          );
        }

        if (needsReorder && isAdmin) {
          apiCalls.push(
            productsApi.getNeedsReorder({
              sortBy: sort,
              direction: direction?.toUpperCase() as 'ASC' | 'DESC' | undefined,
              page: 0,
              size: 50,
            })
          );
        }

        // Execute all API calls
        const responses = await Promise.all(apiCalls);
        
        // Extract products from responses
        const productArrays = responses.map(response => response.data?.content || []);
        
        // Combine and deduplicate results
        if (productArrays.length === 1) {
          allResults = productArrays[0];
        } else if (productArrays.length > 1) {
          // Find intersection of all results
          const productMap = new Map<number, Product>();
          
          // Add all products from first array to map
          productArrays[0].forEach(product => {
            productMap.set(product.id, product);
          });
          
          // Keep only products that appear in all arrays
          for (let i = 1; i < productArrays.length; i++) {
            const currentIds = new Set(productArrays[i].map(p => p.id));
            // Use forEach to iterate map keys to support older TS targets
            productMap.forEach((product, id) => {
              if (!currentIds.has(id)) {
                productMap.delete(id);
              }
            });
          }
          
          const vals: Product[] = [];
          productMap.forEach((p) => vals.push(p));
          allResults = vals;
        }

        // Sort the final results
        allResults.sort((a, b) => {
          switch (sort) {
            case 'price':
              const aPrice = a.effectivePrice || a.price || 0;
              const bPrice = b.effectivePrice || b.price || 0;
              return direction === 'DESC' ? bPrice - aPrice : aPrice - bPrice;
            case 'rating':
              const aRating = a.rating || 0;
              const bRating = b.rating || 0;
              return direction === 'DESC' ? bRating - aRating : aRating - bRating;
            case 'newest':
              const aDate = new Date(a.updatedAt || a.createdAt || 0);
              const bDate = new Date(b.updatedAt || b.createdAt || 0);
              return direction === 'DESC' ? bDate.getTime() - aDate.getTime() : aDate.getTime() - bDate.getTime();
            default:
              // Default to relevance (keep API order)
              return 0;
          }
        });

        // Limit final results
        setFilteredProducts(allResults.slice(0, 24));

        // Fetch suggestions only when there's a search query
        if (debouncedQuery) {
          const sres = await productsApi.search({ q: debouncedQuery, page: 0, size: 5 });
          setSuggestions(sres.data?.content || []);
          setIsSuggesting(true);
        } else {
          setSuggestions([]);
          setIsSuggesting(false);
        }

      } catch (error) {
        console.error('Error fetching products:', error);
        setFilteredProducts([]);
        setSuggestions([]);
        setIsSuggesting(false);
      } finally {
        setIsLoading(false);
      }
    };

    fetchProducts();
  }, [debouncedQuery, sortBy, priceRange, inventoryStatus, needsReorder, isAdmin, priceBounds]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    router.push(`/search?q=${encodeURIComponent(searchQuery)}`);
  };

  const clearSearch = () => {
    setSearchQuery('');
    setInventoryStatus('');
    setNeedsReorder(false);
    setPriceRange(priceBounds);
    router.push('/search');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 sticky top-0 z-40">
        <div className="w-full px-4 sm:px-6 lg:px-8 py-4">
          <form onSubmit={handleSearch} className="flex gap-2">
              <div className="flex-1 relative">
              <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products..."
                className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {/* Suggestions dropdown */}
              {isSuggesting && suggestions.length > 0 && (
                <ul className="absolute left-0 right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg z-50 max-h-60 overflow-auto">
                  {suggestions.map((s) => (
                    <li
                      key={s.id}
                      className="px-4 py-2 hover:bg-gray-100 cursor-pointer flex items-center gap-3"
                      onMouseDown={() => {
                        // use onMouseDown to prevent blur before click
                        setSearchQuery(s.name || '');
                        setDebouncedQuery(s.name || '');
                        setIsSuggesting(false);
                        router.push(`/search?q=${encodeURIComponent(s.name || '')}`);
                      }}
                    >
                      <img src={s.image ? s.image : '/placeholder.png'} alt={s.name} className="w-10 h-10 object-cover rounded" />
                      <div className="text-sm text-gray-800 truncate">{s.name}</div>
                    </li>
                  ))}
                </ul>
              )}
              {searchQuery && (
                <button
                  type="button"
                  onClick={clearSearch}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  <X className="w-5 h-5" />
                </button>
              )}
            </div>
            <button
              type="submit"
              className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              Search
            </button>
          </form>
        </div>
      </div>

      <div className="w-full px-4 sm:px-6 lg:px-8 py-8">
        {/* Search Results Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Search Results</h1>
          
          {/* Active Filters Display */}
          {activeFilters.size > 0 && (
            <div className="mb-4 flex flex-wrap gap-2">
              {activeFilters.has('search') && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                  Search: "{searchQuery}"
                </span>
              )}
              {activeFilters.has('price') && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
                  Price: ${priceRange[0]} - ${priceRange[1]}
                </span>
              )}
              {activeFilters.has('inventory') && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-purple-100 text-purple-800">
                  Status: {inventoryStatusOptions.find(opt => opt.value === inventoryStatus)?.label}
                </span>
              )}
              {activeFilters.has('reorder') && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-orange-100 text-orange-800">
                  🔔 Needs Reorder
                </span>
              )}
              {activeFilters.size > 1 && (
                <button
                  onClick={() => {
                    setSearchQuery('');
                    setInventoryStatus('');
                    setNeedsReorder(false);
                    setPriceRange(priceBounds);
                  }}
                  className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-red-100 text-red-800 hover:bg-red-200"
                >
                  Clear All
                </button>
              )}
            </div>
          )}
          
          {searchQuery && activeFilters.size === 1 && (
            <p className="text-gray-600">
              Found <span className="font-semibold text-gray-900">{filteredProducts.length}</span> products for "{searchQuery}"
            </p>
          )}
          {activeFilters.size > 0 && (
            <p className="text-gray-600">
              Found <span className="font-semibold text-gray-900">{filteredProducts.length}</span> products
            </p>
          )}
          {activeFilters.size === 0 && (
            <p className="text-gray-600">Enter a search term or apply filters to find products</p>
          )}
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Filters Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-sm p-6 space-y-6">
              {/* Sort */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Sort By</h3>
                <div className="space-y-2">
                  {sortOptions.map((option) => (
                    <label key={option.value} className="flex items-center cursor-pointer group">
                      <input
                        type="radio"
                        name="sort"
                        value={option.value}
                        checked={sortBy === option.value}
                        onChange={(e) => setSortBy(e.target.value)}
                        className="w-4 h-4 text-blue-600 cursor-pointer"
                      />
                      <span className="ml-3 text-gray-700 group-hover:text-gray-900">{option.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Price Range */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Price Range</h3>
                {isPriceLoading ? (
                  <div className="text-sm text-gray-500">Loading price range...</div>
                ) : (
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm text-gray-700 mb-2">Min: ${priceRange[0]}</label>
                      <input
                        type="range"
                        min={priceBounds[0]}
                        max={priceBounds[1]}
                        value={priceRange[0]}
                        onChange={(e) => setPriceRange([parseInt(e.target.value), priceRange[1]])}
                        className="w-full"
                      />
                    </div>
                    <div>
                      <label className="block text-sm text-gray-700 mb-2">Max: ${priceRange[1]}</label>
                      <input
                        type="range"
                        min={priceBounds[0]}
                        max={priceBounds[1]}
                        value={priceRange[1]}
                        onChange={(e) => setPriceRange([priceRange[0], parseInt(e.target.value)])}
                        className="w-full"
                      />
                    </div>
                    <div className="text-xs text-gray-500">
                      Range: ${priceBounds[0]} - ${priceBounds[1]}
                    </div>
                  </div>
                )}
              </div>

              {/* Inventory Status */}
              <div>
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Inventory Status</h3>
                <div className="space-y-2">
                  {inventoryStatusOptions.map((option) => (
                    <label key={option.value} className="flex items-center cursor-pointer group">
                      <input
                        type="radio"
                        name="inventory"
                        value={option.value}
                        checked={inventoryStatus === option.value}
                        onChange={(e) => setInventoryStatus(e.target.value)}
                        className="w-4 h-4 text-blue-600 cursor-pointer"
                      />
                      <span className="ml-3 text-gray-700 group-hover:text-gray-900">{option.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Admin-only Needs Reorder Filter */}
              {isAdmin && (
                <div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-4">Admin Filters</h3>
                  <div className="space-y-2">
                    <label className="flex items-center cursor-pointer group">
                      <input
                        type="checkbox"
                        checked={needsReorder}
                        onChange={(e) => setNeedsReorder(e.target.checked)}
                        className="w-4 h-4 text-blue-600 cursor-pointer"
                      />
                      <span className="ml-3 text-gray-700 group-hover:text-gray-900 flex items-center gap-2">
                        🔔 Products Needing Reorder
                      </span>
                    </label>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Products Grid */}
          <div className="lg:col-span-3">
            {isLoading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <p className="text-gray-600">Searching products...</p>
              </div>
            ) : filteredProducts.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredProducts.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <div className="text-center py-12">
                <SearchIcon className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <h2 className="text-2xl font-semibold text-gray-900 mb-2">No products found</h2>
                <p className="text-gray-600 mb-6">
                  {activeFilters.size > 0
                    ? 'Try adjusting your search terms or filters'
                    : 'Start by searching for a product or applying filters'}
                </p>
                {activeFilters.size > 0 && (
                  <button
                    onClick={() => {
                      setSearchQuery('');
                      setInventoryStatus('');
                      setPriceRange(priceBounds);
                    }}
                    className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Clear All Filters
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
