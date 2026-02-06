'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Zap, Flame, Clock } from 'lucide-react';
import { CountdownTimer } from './CountdownTimer';
import { UrgencyBadge } from './UrgencyBadges';
import { ProductCard } from './ProductCard';
import { Button } from './ui/Button';
import { Product } from '@/types';
import { SkeletonProductCardGrid, SkeletonCategoryCardGrid } from './skeletons';

interface FlashSaleHomepageProps {
  flashDealProducts?: Product[];
  recommendedProducts?: Product[];
  categories?: Array<{ name: string; slug: string; image: string; count?: number }>;
  isLoading?: boolean;
}

/**
 * Flash Sale Homepage
 *
 * Features:
 * - Urgent flash sale banner with lightning-fast countdown
 * - "LIMITED TIME" messaging
 * - Lightning deals with stock indicators
 * - Quick add-to-cart functionality
 * - Mobile-optimized horizontal scrolling
 * - Stock countdown warnings
 */
export function FlashSaleHomepage({
  flashDealProducts = [],
  recommendedProducts = [],
  categories = [],
  isLoading = false,
}: FlashSaleHomepageProps) {
  const [mounted, setMounted] = useState(false);
  const saleEndDate = new Date(Date.now() + 2 * 60 * 60 * 1000); // 2 hours from now

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) return null;

  // Default categories if not provided
  const defaultCategories = categories.length > 0 ? categories : [
    { name: 'Electronics', slug: 'electronics', image: '📱' },
    { name: 'Fashion', slug: 'fashion', image: '👕' },
    { name: 'Home & Kitchen', slug: 'home-kitchen', image: '🏠' },
    { name: 'Beauty & Health', slug: 'beauty', image: '💄' },
    { name: 'Sports & Outdoors', slug: 'sports', image: '⚽' },
    { name: 'Books & Media', slug: 'books', image: '📚' },
    { name: 'Toys & Games', slug: 'toys', image: '🎮' },
    { name: 'Automotive', slug: 'automotive', image: '🚗' },
  ];

  return (
    <div className="min-h-screen bg-orange-50">
      {/* Flash Sale Banner Hero */}
      <section className="relative overflow-hidden bg-gradient-to-r from-orange-500 via-red-500 to-pink-500 text-white">
        {/* Animated lightning effects */}
        <div className="absolute inset-0 opacity-20">
          <div className="absolute top-1/4 left-1/4 w-32 h-32 bg-yellow-300 rounded-full blur-2xl animate-ping" />
          <div className="absolute bottom-1/4 right-1/4 w-24 h-24 bg-white rounded-full blur-xl animate-pulse" />
          <div className="absolute top-1/2 left-1/2 w-16 h-16 bg-orange-300 rounded-full blur-lg animate-bounce" />
        </div>

        <div className="relative w-full px-4 sm:px-6 lg:px-8 py-12 md:py-16">
          <div className="text-center">
            {/* Flash Icon */}
            <div className="mb-6 flex justify-center">
              <div className="relative">
                <Zap className="w-16 h-16 text-yellow-300 animate-pulse" />
                <div className="absolute inset-0 bg-yellow-300/30 rounded-full blur-xl animate-ping" />
              </div>
            </div>

            {/* Badge */}
            <div className="mb-4 inline-block">
              <UrgencyBadge type="flash-sale" size="lg" animate={true} />
            </div>

            {/* Headline */}
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-black mb-4 leading-tight">
              FLASH SALE
            </h1>

            {/* Subheading */}
            <p className="text-2xl sm:text-3xl font-bold mb-6 text-yellow-200">
              UP TO 70% OFF
            </p>

            {/* Description */}
            <p className="text-lg mb-8 text-orange-100 leading-relaxed max-w-2xl mx-auto">
              Lightning-fast deals that disappear in the blink of an eye! Limited stock available. Shop now before it's too late!
            </p>

            {/* Countdown */}
            <div className="mb-8 bg-black/20 backdrop-blur-sm p-6 rounded-xl inline-block mx-auto">
              <p className="text-sm font-semibold mb-3 text-yellow-200">ENDS IN</p>
              <CountdownTimer
                endDate={saleEndDate}
                size="lg"
                format="full"
                showLabels={true}
              />
            </div>

            {/* CTAs */}
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link href="/products?promotion=flash-sale">
                <Button className="bg-yellow-400 hover:bg-yellow-500 text-gray-900 font-bold w-full sm:w-auto">
                  <Flame className="w-5 h-5 mr-2" />
                  SHOP FLASH DEALS
                </Button>
              </Link>
              <Link href="/deals">
                <Button variant="outline" className="border-white text-white hover:bg-white/10 w-full sm:w-auto">
                  View All Deals
                </Button>
              </Link>
            </div>
          </div>
        </div>

        {/* Wave divider */}
        <div className="absolute bottom-0 left-0 right-0 h-16 bg-orange-50" style={{
          clipPath: 'polygon(0 50%, 5% 45%, 10% 50%, 15% 45%, 20% 50%, 25% 45%, 30% 50%, 35% 45%, 40% 50%, 45% 45%, 50% 50%, 55% 45%, 60% 50%, 65% 45%, 70% 50%, 75% 45%, 80% 50%, 85% 45%, 90% 50%, 95% 45%, 100% 50%, 100% 100%, 0 100%)',
        }} />
      </section>

      {/* Lightning Deals Section */}
      {flashDealProducts.length > 0 && (
        <section className="w-full px-4 sm:px-6 lg:px-8 py-16">
          <div className="mb-12">
            <div className="flex items-center gap-3 mb-2">
              <Flame className="w-8 h-8 text-orange-600" />
              <h2 className="text-3xl font-bold text-gray-900">Lightning Deals</h2>
            </div>
            <p className="text-gray-600 ml-11">Strike while the iron is hot! These deals won't last long.</p>
          </div>

          {isLoading ? (
            <SkeletonProductCardGrid count={6} />
          ) : (
            <div className="overflow-x-auto pb-4 -mx-4 px-4 sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8">
              <div className="flex gap-4 md:gap-6 min-w-min md:min-w-full md:grid md:grid-cols-6">
                {flashDealProducts.map((product) => (
                  <div key={product.id} className="flex-shrink-0 w-48 md:w-auto md:flex-shrink">
                    <div className="relative bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow">
                      <ProductCard product={product} />

                      {/* Stock indicator */}
                      <div className="absolute top-2 right-2 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-full">
                        Only {Math.floor(Math.random() * 10) + 1} left!
                      </div>

                      {/* Quick countdown */}
                      <div className="absolute bottom-16 left-2 right-2 bg-orange-500/90 backdrop-blur-sm text-white text-xs font-bold px-2 py-1 rounded-lg text-center">
                        <CountdownTimer
                          endDate={saleEndDate}
                          size="sm"
                          format="inline"
                          showLabels={false}
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </section>
      )}

      {/* Categories Grid */}
      <section className="w-full px-4 sm:px-6 lg:px-8 py-16">
        <div className="mb-12">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Shop by Category</h2>
          <p className="text-gray-600">Find flash deals in your favorite categories</p>
        </div>

        {isLoading ? (
          <SkeletonCategoryCardGrid count={8} />
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4 md:gap-6">
            {defaultCategories.map((cat, idx) => (
              <Link
                key={idx}
                href={`/categories/${cat.slug}?promotion=flash-sale`}
                className="group"
              >
                <div className="relative bg-white rounded-2xl shadow-sm hover:shadow-md transition-all duration-300 p-6 text-center hover:-translate-y-1">
                  {/* Circular background with flash effect */}
                  <div className="w-24 h-24 mx-auto mb-4 bg-gradient-to-br from-orange-100 to-red-50 rounded-full flex items-center justify-center group-hover:scale-110 transition-transform duration-300 shadow-md relative">
                    <span className="text-4xl">{typeof cat.image === 'string' && cat.image.length === 1 ? cat.image : '🛍️'}</span>
                    <div className="absolute -top-1 -right-1 w-6 h-6 bg-yellow-400 rounded-full flex items-center justify-center">
                      <Zap className="w-3 h-3 text-orange-800" />
                    </div>
                  </div>

                  {/* Category name */}
                  <h3 className="font-semibold text-gray-900 text-sm mb-2 line-clamp-2">
                    {cat.name}
                  </h3>

                  {/* Item count */}
                  {cat.count && (
                    <p className="text-xs text-gray-500">{cat.count} items on sale</p>
                  )}

                  {/* Hover overlay */}
                  <div className="absolute inset-0 rounded-2xl bg-orange-600/0 group-hover:bg-orange-600/5 transition-colors duration-300" />
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Clearance Deals Section */}
      <section className="bg-gradient-to-r from-gray-100 to-gray-200 py-16">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <div className="mb-12">
            <div className="flex items-center gap-3 mb-2">
              <Clock className="w-8 h-8 text-gray-700" />
              <h2 className="text-3xl font-bold text-gray-900">Clearance Deals</h2>
            </div>
            <p className="text-gray-600 ml-11">Final reductions on remaining stock</p>
          </div>

          {isLoading ? (
            <SkeletonProductCardGrid count={4} />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              {recommendedProducts.slice(0, 4).map((product) => (
                <div key={product.id} className="relative">
                  <ProductCard product={product} />

                  {/* Clearance badge */}
                  <div className="absolute top-2 left-2 bg-red-600 text-white text-xs font-bold px-3 py-1 rounded-full">
                    CLEARANCE
                  </div>

                  {/* Stock remaining */}
                  <div className="absolute bottom-2 right-2 bg-gray-800 text-white text-xs px-2 py-1 rounded">
                    {Math.floor(Math.random() * 20) + 5} left
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Trust Section */}
      <section className="bg-gradient-to-r from-orange-50 to-red-50 py-16">
        <div className="w-full px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold text-gray-900 mb-12 text-center">Why Shop Flash Sales?</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
            {[
              { icon: '⚡', title: 'Lightning Fast', desc: 'Deals that move quickly - act fast!' },
              { icon: '💰', title: 'Massive Savings', desc: 'Up to 70% off regular prices' },
              { icon: '📦', title: 'Limited Stock', desc: 'Exclusive deals with limited quantities' },
              { icon: '🚚', title: 'Quick Shipping', desc: 'Fast delivery on flash sale items' },
            ].map((item, idx) => (
              <div key={idx} className="text-center">
                <div className="text-4xl mb-4">{item.icon}</div>
                <h3 className="font-bold text-gray-900 mb-2">{item.title}</h3>
                <p className="text-gray-600 text-sm">{item.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
