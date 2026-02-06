'use client';

import React, { useState, useEffect } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { ChevronLeft } from 'lucide-react';
import { ProductCard } from '@/components/ProductCard';
import { Product } from '@/types';
import { productsApi, categoriesApi } from '@/services/api';

export default function CategoryPage() {
  const router = useRouter();
  const params = useParams();
  const searchParams = useSearchParams();
  const categorySlug = params.slug as string;
  const sortParam = searchParams.get('sort') || 'featured';

  const [products, setProducts] = useState<Product[]>([]);
  const [category, setCategory] = useState<{ name: string; description: string } | null>(null);
  const [sortBy, setSortBy] = useState(sortParam);
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 500]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchCategoryData = async () => {
      try {
        setIsLoading(true);
        
        // Fetch category info by slug (support different response shapes)
        const categoryResponse = await categoriesApi.getSlug(categorySlug);
        const cat = (categoryResponse as any)?.data?.category || (categoryResponse as any)?.category || (categoryResponse as any)?.data || null;
        setCategory({
          name: cat?.name || cat?.title || 'Category',
          description: cat?.description || ''
        });

        // Fetch products by category (API may accept id or slug)
        const productsResponse = await productsApi.getByCategory((cat?.id ?? categorySlug) as any);
        setProducts((productsResponse as any)?.products || (productsResponse as any)?.data?.content || (productsResponse as any)?.data?.products || []);
      } catch (error) {
        console.error('Error fetching category data:', error);
        router.push('/products');
      } finally {
        setIsLoading(false);
      }
    };

    fetchCategoryData();
  }, [categorySlug, router]);

  useEffect(() => {
    let filtered = [...products];

    // Filter by price range
    filtered = filtered.filter((p) => p.price >= priceRange[0] && p.price <= priceRange[1]);

    // Sort results
    switch (sortBy) {
      case 'price-low':
        filtered.sort((a, b) => a.price - b.price);
        break;
      case 'price-high':
        filtered.sort((a, b) => b.price - a.price);
        break;
      case 'rating':
        filtered.sort((a, b) => b.rating - a.rating);
        break;
      case 'newest':
        filtered.sort((a, b) => b.id - a.id);
        break;
      case 'featured':
      default:
        break;
    }

    setProducts(filtered);
  }, [sortBy, priceRange]);

  if (!category) {
    return null;
  }

  const sortOptions = [
    { value: 'featured', label: 'Featured' },
    { value: 'price-low', label: 'Price: Low to High' },
    { value: 'price-high', label: 'Price: High to Low' },
    { value: 'rating', label: 'Highest Rated' },
    { value: 'newest', label: 'Newest' },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200">
        <div className="w-full px-4 sm:px-6 lg:px-8 py-8">
          <button
            onClick={() => router.back()}
            className="flex items-center text-blue-600 hover:text-blue-700 mb-6 transition-colors"
          >
            <ChevronLeft className="w-5 h-5 mr-1" />
            Back
          </button>
          <div className="text-center">
            <h1 className="text-4xl font-bold text-gray-900 mb-2">{category.name}</h1>
            <p className="text-xl text-gray-600">{category.description}</p>
            <div className="mt-4 text-gray-500">
              Showing <span className="font-semibold text-gray-900">{products.length}</span> products
            </div>
          </div>
        </div>
      </div>

      <div className="w-full px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Filters Sidebar */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-sm p-6 space-y-6 sticky top-20">
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
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm text-gray-700 mb-2">Min: ${priceRange[0]}</label>
                    <input
                      type="range"
                      min="0"
                      max="500"
                      value={priceRange[0]}
                      onChange={(e) => setPriceRange([parseInt(e.target.value), priceRange[1]])}
                      className="w-full"
                    />
                  </div>
                  <div>
                    <label className="block text-sm text-gray-700 mb-2">Max: ${priceRange[1]}</label>
                    <input
                      type="range"
                      min="0"
                      max="500"
                      value={priceRange[1]}
                      onChange={(e) => setPriceRange([priceRange[0], parseInt(e.target.value)])}
                      className="w-full"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Products Grid */}
          <div className="lg:col-span-3">
            {isLoading ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                <p className="text-gray-600">Loading products...</p>
              </div>
            ) : products.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>
            ) : (
              <div className="text-center py-12 bg-white rounded-lg">
                <h2 className="text-2xl font-semibold text-gray-900 mb-2">No products found</h2>
                <p className="text-gray-600">Try adjusting your filters to find products</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
