'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { NewYearSaleHomepage } from '@/components/NewYearSaleHomepage';
import { BlackFridayHomepage } from '@/components/BlackFridayHomepage';
import { FlashSaleHomepage } from '@/components/FlashSaleHomepage';
import { SeasonalHomepage } from '@/components/SeasonalHomepage';
import { HolidayHomepage } from '@/components/HolidayHomepage';
import { getActiveVariation } from '@/components/HomepageVariations';
import { SkeletonProductCardGrid } from '@/components/skeletons';
import { Product } from '@/types';
import { ProductCard } from '@/components/ProductCard';
import { productsApi, categoriesApi } from '@/services/api';

/**
 * Default Homepage Component
 * Fallback for non-promotional periods
 */
function DefaultHomepage({ 
  featuredProducts, 
  recommendedProducts, 
  trendingProducts,
  categories, 
  isLoadingFeatured, 
  isLoadingRecommended, 
  isLoadingTrending,
  isLoadingCategories,
  error 
}: {
  featuredProducts: Product[];
  recommendedProducts: Product[];
  trendingProducts: Product[];
  categories: any[];
  isLoadingFeatured: boolean;
  isLoadingRecommended: boolean;
  isLoadingTrending: boolean;
  isLoadingCategories: boolean;
  error: string | null;
}) {
  return (
    <div className="bg-white">
      {/* Hero Section */}
      <section className="relative bg-gradient-to-br from-indigo-600 via-purple-600 to-pink-600 text-white overflow-hidden">
        <div className="absolute inset-0 bg-black/10"></div>
        <div className="relative w-full px-4 sm:px-6 lg:px-8 py-20">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div className="space-y-8">
              <div className="space-y-4">
                <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold leading-tight">
                  Shop Premium Products
                  <span className="block text-3xl md:text-4xl text-yellow-300">at Unbeatable Prices</span>
                </h1>
                <p className="text-xl text-blue-100 max-w-lg">
                  Discover amazing deals on top-quality products. Free shipping on orders over GHS 500!
                </p>
              </div>
              <div className="flex flex-col sm:flex-row gap-4">
                <Link href="/products">
                  <Button variant="secondary" size="lg" className="w-full sm:w-auto">
                    Shop Now
                  </Button>
                </Link>
                <Link href="/new-arrivals">
                  <Button variant="outline" size="lg" className="w-full sm:w-auto border-white text-white hover:bg-white hover:text-purple-600">
                    New Arrivals
                  </Button>
                </Link>
              </div>
            </div>
            <div className="hidden lg:flex justify-center">
              <div className="relative">
                <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/20 to-pink-400/20 rounded-full blur-2xl"></div>
                <div className="relative w-96 h-96 bg-white/10 backdrop-blur-sm rounded-full border border-white/20 shadow-2xl"></div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Animated background elements */}
        <div className="absolute top-10 left-10 w-20 h-20 bg-white/5 rounded-full animate-pulse"></div>
        <div className="absolute bottom-10 right-10 w-32 h-32 bg-white/5 rounded-full animate-pulse delay-75"></div>
        <div className="absolute top-1/2 left-1/4 w-16 h-16 bg-white/5 rounded-full animate-pulse delay-150"></div>
      </section>

      {/* Flash Deals Banner */}
      <section className="bg-gradient-to-r from-red-600 to-orange-600 text-white py-6">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <span className="bg-yellow-400 text-red-900 px-3 py-1 rounded-full text-sm font-bold animate-pulse">LIMITED TIME</span>
              <h2 className="text-2xl font-bold">Flash Sale - Up to 60% Off!</h2>
            </div>
            <Link href="/deals" className="bg-white text-red-600 px-6 py-2 rounded-lg font-semibold hover:bg-gray-100 transition-colors">
              Shop Deals
            </Link>
          </div>
        </div>
      </section>

      {/* Featured Products Section */}
      <section className="py-16 bg-gray-50">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-3xl font-bold text-gray-900">🔥 Featured Products</h2>
              <p className="text-gray-600 mt-1">Handpicked by our experts</p>
            </div>
            <Link href="/products" className="text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1">
              View All <span className="text-xl">→</span>
            </Link>
          </div>
          {isLoadingFeatured ? (
            <SkeletonProductCardGrid count={6} />
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              {featuredProducts.slice(0, 6).map((product, index) => (
                <div
                  key={product.id} 
                  className="animate-in fade-in-50 duration-500"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <ProductCard product={product} />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Categories Section */}
      <section className="py-16 bg-white">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Shop by Category</h2>
            <p className="text-gray-600 max-w-2xl mx-auto">Find exactly what you're looking for in our curated collections</p>
          </div>
          {isLoadingCategories ? (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
              {[...Array(6)].map((_, i) => (
                <div key={i} className="bg-white rounded-xl p-6 shadow-md animate-pulse">
                  <div className="w-20 h-20 bg-gray-200 rounded-xl mx-auto mb-4"></div>
                  <div className="h-4 bg-gray-200 rounded w-3/4 mx-auto mb-2"></div>
                  <div className="h-3 bg-gray-200 rounded w-1/2 mx-auto"></div>
                </div>
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6">
              {categories.slice(0, 6).map((category, index) => (
                <Link
                  key={category.id || category.slug}
                  href={`/products?category=${category.id}`}
                  className="group bg-white rounded-xl p-6 text-center hover:shadow-xl transition-all duration-300 border border-gray-100 hover:border-indigo-200"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <div className="w-20 h-20 bg-gradient-to-br from-indigo-50 to-purple-50 rounded-xl mx-auto mb-4 flex items-center justify-center text-3xl group-hover:scale-110 transition-transform duration-300">
                    {category.image || '🛍️'}
                  </div>
                  <h3 className="font-semibold text-gray-900 group-hover:text-indigo-600 transition-colors">
                    {category.name}
                  </h3>
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* New Arrivals */}
      <section className="py-16 bg-purple-50">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">✨ New Arrivals</h2>
              <p className="text-gray-600">Just in store</p>
            </div>
            <Link href="/new-arrivals" className="text-indigo-600 hover:text-indigo-800 font-medium">
              View All →
            </Link>
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
            {recommendedProducts.slice(0, 6).map((product, index) => (
              <div
                key={product.id}
                className="animate-in slide-in-from-bottom-5 duration-500"
                style={{ animationDelay: `${index * 50}ms` }}
              >
                <ProductCard product={product} />
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Trending Now */}
      <section className="py-16 bg-indigo-50">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">🔥 Trending Now</h2>
              <p className="text-gray-600">Popular this week</p>
            </div>
            <Link href="/products" className="text-indigo-600 hover:text-indigo-800 font-medium">
              View All →
            </Link>
          </div>
          {isLoadingTrending ? (
            <SkeletonProductCardGrid count={6} />
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              {trendingProducts.slice(0, 6).map((product, index) => (
                <div
                  key={product.id}
                  className="animate-in slide-in-from-bottom-5 duration-500"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <ProductCard product={product} />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Recommended Products Section */}
      <section className="py-16 bg-white">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-3xl font-bold text-gray-900">Recommended for You</h2>
              <p className="text-gray-600 mt-1">Based on your browsing history</p>
            </div>
            <Link href="/products" className="text-indigo-600 hover:text-indigo-800 font-medium flex items-center gap-1">
              View All <span className="text-xl">→</span>
            </Link>
          </div>
          {isLoadingRecommended ? (
            <SkeletonProductCardGrid count={6} />
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
              {recommendedProducts.slice(0, 6).map((product, index) => (
                <div
                  key={product.id} 
                  className="animate-in fade-in-50 duration-500"
                  style={{ animationDelay: `${index * 50}ms` }}
                >
                  <ProductCard product={product} />
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Trust Badges Section */}
      <section className="py-16 bg-gray-900 text-white">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold mb-4">Why Shop With Us?</h2>
            <p className="text-gray-300 max-w-2xl mx-auto">Experience the difference with our premium service</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            {[
              { icon: '🚚', title: 'Fast Delivery', desc: 'Express shipping within 24 hours', color: 'from-blue-500 to-blue-600' },
              { icon: '🛡️', title: 'Secure Payment', desc: '100% secure transactions guaranteed', color: 'from-green-500 to-green-600' },
              { icon: '↩️', title: 'Easy Returns', desc: '30-day hassle-free returns', color: 'from-purple-500 to-purple-600' },
              { icon: '💬', title: '24/7 Support', desc: 'Always here to help you', color: 'from-orange-500 to-orange-600' },
            ].map((item, idx) => (
              <div key={idx} className="text-center space-y-4 group">
                <div className={`w-20 h-20 mx-auto bg-gradient-to-br ${item.color} rounded-2xl flex items-center justify-center text-3xl shadow-lg group-hover:scale-110 transition-transform duration-300`}>
                  {item.icon}
                </div>
                <h3 className="text-xl font-semibold text-white">{item.title}</h3>
                <p className="text-gray-300 text-sm">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-16 bg-gray-50">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Shop with Confidence</h2>
            <p className="text-gray-600 max-w-2xl mx-auto">Your satisfaction is our top priority</p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              { icon: '✓', title: 'Quality Guarantee', desc: 'All products tested for quality and durability', highlight: 'Premium materials & craftsmanship' },
              { icon: '💰', title: 'Best Prices', desc: 'Competitive pricing with regular sales and discounts', highlight: 'Price match guarantee available' },
              { icon: '🌍', title: 'Wide Selection', desc: 'Thousands of products across multiple categories', highlight: 'New arrivals every week' },
            ].map((item, idx) => (
              <div key={idx} className="bg-white rounded-xl p-6 shadow-md hover:shadow-xl transition-all duration-300 border border-gray-100">
                <div className="text-4xl mb-4 text-indigo-600">{item.icon}</div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">{item.title}</h3>
                <p className="text-gray-600 mb-3">{item.desc}</p>
                <p className="text-sm text-indigo-600 font-medium bg-indigo-50 rounded-lg px-3 py-2">{item.highlight}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}

export default function HomePage() {
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const [recommendedProducts, setRecommendedProducts] = useState<Product[]>([]);
  const [trendingProducts, setTrendingProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [isLoadingFeatured, setIsLoadingFeatured] = useState(true);
  const [isLoadingRecommended, setIsLoadingRecommended] = useState(true);
  const [isLoadingTrending, setIsLoadingTrending] = useState(true);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [mounted, setMounted] = useState(false);

  // Fetch data from API
  useEffect(() => {
    const fetchData = async () => {
      // Helper to extract products from various response structures
      const extractProducts = (response: any): any[] => {
        const data = response?.data?.content || response?.data || response?.content || [];
        return Array.isArray(data) ? data : [];
      };

      // Fetch featured products with fallback to regular products
      try {
        const featuredResponse = await productsApi.getFeatured({ size: 6 });
        let featuredData = extractProducts(featuredResponse);
        
        // Fallback to regular products if featured is empty
        if (featuredData.length === 0) {
          const fallbackResponse = await productsApi.getAll({ size: 6 });
          featuredData = extractProducts(fallbackResponse);
        }
        setFeaturedProducts(featuredData);
      } catch (err) {
        console.error('Failed to fetch featured products:', err);
        try {
          const fallbackResponse = await productsApi.getAll({ size: 6 });
          setFeaturedProducts(extractProducts(fallbackResponse));
        } catch {
          setFeaturedProducts([]);
        }
      } finally {
        setIsLoadingFeatured(false);
      }

      // Fetch new arrivals with fallback
      try {
        const newArrivalsResponse = await productsApi.getNewArrivals({ size: 6 });
        let newArrivalsData = extractProducts(newArrivalsResponse);
        
        if (newArrivalsData.length === 0) {
          const fallbackResponse = await productsApi.getAll({ size: 6, sortBy: 'createdAt', direction: 'desc' });
          newArrivalsData = extractProducts(fallbackResponse);
        }
        setRecommendedProducts(newArrivalsData);
      } catch (err) {
        console.error('Failed to fetch new arrivals:', err);
        try {
          const fallbackResponse = await productsApi.getAll({ size: 6, sortBy: 'createdAt', direction: 'desc' });
          setRecommendedProducts(extractProducts(fallbackResponse));
        } catch {
          setRecommendedProducts([]);
        }
      } finally {
        setIsLoadingRecommended(false);
      }

      // Fetch parent categories (root categories only)
      try {
        const categoriesResponse = await categoriesApi.getRoot();
        const categoriesData = (categoriesResponse as any)?.data || (categoriesResponse as any)?.categories || [];
        setCategories(Array.isArray(categoriesData) ? categoriesData : []);
      } catch (err) {
        console.error('Failed to fetch root categories:', err);
        try {
          // Fallback to all categories
          const fallbackResponse = await categoriesApi.getAll();
          const fallbackData = fallbackResponse?.data?.content || fallbackResponse?.data || [];
          setCategories(Array.isArray(fallbackData) ? fallbackData : []);
        } catch {
          setCategories([]);
        }
      } finally {
        setIsLoadingCategories(false);
      }

      // Fetch trending products with fallback
      try {
        const trendingResponse = await productsApi.getTrending({ size: 6 });
        let trendingData = extractProducts(trendingResponse);
        
        if (trendingData.length === 0) {
          const fallbackResponse = await productsApi.getAll({ size: 6, sortBy: 'salesCount', direction: 'desc' });
          trendingData = extractProducts(fallbackResponse);
        }
        setTrendingProducts(trendingData);
      } catch (err) {
        console.error('Failed to fetch trending products:', err);
        try {
          const fallbackResponse = await productsApi.getAll({ size: 6 });
          setTrendingProducts(extractProducts(fallbackResponse));
        } catch {
          setTrendingProducts([]);
        }
      } finally {
        setIsLoadingTrending(false);
      }
    };

    fetchData();
  }, []);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  // Get active homepage variation
  const activeVariation = getActiveVariation();

  // Render New Year Sale homepage if active
  if (activeVariation === 'new-year-sale') {
    return (
      <NewYearSaleHomepage
        flashDealProducts={featuredProducts.slice(0, 5)}
        recommendedProducts={recommendedProducts}
        categories={categories}
        isLoading={isLoadingFeatured || isLoadingRecommended || isLoadingCategories}
      />
    );
  }

  // Render Black Friday homepage if active
  if (activeVariation === 'black-friday') {
    return (
      <BlackFridayHomepage
        dealProducts={featuredProducts.slice(0, 5)}
        recommendedProducts={recommendedProducts}
        categories={categories}
        isLoading={isLoadingFeatured || isLoadingRecommended || isLoadingCategories}
      />
    );
  }

  // Render Flash Sale homepage if active
  if (activeVariation === 'flash-sale') {
    return (
      <FlashSaleHomepage
        flashDealProducts={featuredProducts.slice(0, 5)}
        recommendedProducts={recommendedProducts}
        categories={categories}
        isLoading={isLoadingFeatured || isLoadingRecommended || isLoadingCategories}
      />
    );
  }

  // Render Seasonal homepage if active
  if (activeVariation === 'seasonal') {
    return (
      <SeasonalHomepage
        seasonalProducts={featuredProducts}
        recommendedProducts={recommendedProducts}
        categories={categories}
        isLoading={isLoadingFeatured || isLoadingRecommended || isLoadingCategories}
      />
    );
  }

  // Render Holiday homepage if active
  if (activeVariation === 'holiday') {
    return (
      <HolidayHomepage
        giftProducts={featuredProducts.slice(0, 6)}
        categories={categories}
        isLoading={isLoadingFeatured || isLoadingCategories}
      />
    );
  }

  // Default homepage for other periods
  return (
    <DefaultHomepage
      featuredProducts={featuredProducts}
      recommendedProducts={recommendedProducts}
      trendingProducts={trendingProducts}
      categories={categories}
      isLoadingFeatured={isLoadingFeatured}
      isLoadingRecommended={isLoadingRecommended}
      isLoadingTrending={isLoadingTrending}
      isLoadingCategories={isLoadingCategories}
      error={error}
    />
  );
}
