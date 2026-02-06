'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  Heart,
  Truck,
  MapPin,
  Shield,
  Share2,
  ChevronRight,
  CheckCircle,
  AlertCircle,
  Globe,
} from 'lucide-react';
import { useParams } from 'next/navigation';
import { useCart } from '@/context/CartContext';
import { useWishlist } from '@/context/WishlistContext';
import { Button } from '@/components/ui/Button';
import { StockBadge } from '@/components/StockBadge';
import ProductReviews from '@/components/ProductReviews';
import { Product, StockStatus, FulfillmentType } from '@/types';
import { productsApi } from '@/services/api';
import {
  FulfillmentBadge,
  PromotionBadge,
  CustomerSignalBadge,
} from '@/components/ProductBadges';
import { getImageUrl } from '@/lib/utils';
import { useTracking } from '@/lib/tracking';

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
  rating: product.rating || product.averageRating || 0,
  reviews: product.reviews || product.reviewCount || 0,
});

function ProductDetailContent({ product, relatedProducts }: { product: Product; relatedProducts: Product[] }) {
  const { addToCart } = useCart();
  const { addToWishlist, removeFromWishlist, isInWishlist } = useWishlist();
  const { trackProductView, trackAddToCart, trackWishlistAdd } = useTracking();
  const [quantity, setQuantity] = useState(1);
  const [selectedImage, setSelectedImage] = useState(0);

  // Determine stock status
  const stockStatus = product.stockStatus || (product.inStock ? StockStatus.IN_STOCK : StockStatus.OUT_OF_STOCK);
  const isOutOfStock = stockStatus === StockStatus.OUT_OF_STOCK;
  const isLowStock = stockStatus === StockStatus.LOW_STOCK;
  const inWishlist = isInWishlist(product.id);

  // Track product view when component mounts
  useEffect(() => {
    trackProductView(
      product.id,
      product.name || 'Unknown Product',
      product.category?.name || product.categoryName || 'Unknown Category',
      product.effectivePrice || product.price
    );
  }, [product.id, product.name, product.category, product.price, trackProductView]);

  const handleAddToCart = async () => {
    if (!isOutOfStock) {
      await addToCart(product.id, quantity);
      // Track add to cart event
      await trackAddToCart(
        product.id,
        product.name || 'Unknown Product',
        product.category?.name || product.categoryName || 'Unknown Category',
        product.effectivePrice || product.price
      );
    }
  };

  const handleWishlistToggle = async () => {
    if (inWishlist) {
      removeFromWishlist(product.id);
    } else {
      await addToWishlist(product.id);
    }
  };

  const discountPercentage = product.discountPrice
    ? Math.round(
        ((product.price - product.discountPrice) / product.price) * 100
      )
    : product.originalPrice
    ? Math.round(
        ((product.originalPrice - product.price) / product.originalPrice) * 100
      )
    : 0;

  return (
    <div className="w-full px-4 sm:px-6 lg:px-8 py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 mb-8 text-sm">
        <Link href="/" className="text-blue-600 hover:text-blue-700">
          Home
        </Link>
        <ChevronRight className="w-4 h-4 text-gray-400" />
        <Link href="/products" className="text-blue-600 hover:text-blue-700">
          Products
        </Link>
        <ChevronRight className="w-4 h-4 text-gray-400" />
        <span className="text-gray-600">{product.name || 'Product'}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-12">
        {/* Product Images */}
        <div className="space-y-4">
          <div className="bg-white rounded-lg overflow-hidden aspect-square">
            <Image
              src={getImageUrl(product.imageUrl || product.images?.[selectedImage] || product.image)}
              alt={product.name || 'Product'}
              width={500}
              height={500}
              className="w-full h-full object-cover"
              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw"
              priority={true}
            />
          </div>
          <div className="flex gap-2 overflow-x-auto">
            {(product.images || [product.imageUrl || product.image]).map((image, idx) => (
              <button
                key={idx}
                onClick={() => setSelectedImage(idx)}
                className={`w-20 h-20 rounded-lg overflow-hidden flex-shrink-0 border-2 transition-colors ${
                  selectedImage === idx ? 'border-blue-600' : 'border-gray-200'
                }`}
              >
                <Image
                  src={getImageUrl(image)}
                  alt={`Product ${idx + 1}`}
                  width={80}
                  height={80}
                  className="w-full h-full object-cover"
                  sizes="80px"
                />
              </button>
            ))}
          </div>
        </div>

        {/* Product Info */}
        <div className="space-y-6">
          {/* Category and Title */}
          <div>
            <p className="text-sm text-gray-500 uppercase tracking-wide mb-2">
              {product.category?.name || product.categoryName || 'Unknown Category'}
            </p>
            <h1 className="text-3xl font-bold text-gray-900 mb-3">
              {product.name || 'Product Name'}
            </h1>

            {/* Badges */}
            <div className="flex flex-wrap gap-2 mb-4">
              <StockBadge status={stockStatus} size="sm" />
              <FulfillmentBadge type={product.fulfillmentType || FulfillmentType.SHIPPED} />
              <CustomerSignalBadge
                isTrending={product.isTrending}
                isMostPurchased={product.isMostPurchased}
                isRecommended={product.isRecommendedForYou}
                rating={product.rating}
              />
            </div>

            {/* Rating */}
            <div className="flex items-center gap-4">
              <div className="flex items-center">
                {[...Array(5)].map((_, i) => (
                  <svg
                    key={i}
                    className={`w-5 h-5 ${
                      i < Math.floor(product.rating || 0)
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
              <span className="text-gray-600">
                {product.rating || 0} ({product.reviews || 0} reviews)
              </span>
            </div>
          </div>

          {/* Price Section */}
          <div className="bg-white rounded-lg p-6">
            <div className="flex items-end gap-3 mb-2">
              <span className="text-4xl font-bold text-blue-600">
                GHS {(product.effectivePrice || product.price || 0).toFixed(2)}
              </span>
              {product.discountPrice && (
                <>
                  <span className="text-2xl text-gray-400 line-through">
                    GHS {(product.price || 0).toFixed(2)}
                  </span>
                  <PromotionBadge discountPercentage={discountPercentage} />
                </>
              )}
              {product.originalPrice && !product.discountPrice && (
                <>
                  <span className="text-2xl text-gray-400 line-through">
                    GHS {(product.originalPrice || 0).toFixed(2)}
                  </span>
                  <PromotionBadge discountPercentage={discountPercentage} />
                </>
              )}
            </div>
            <p className="text-sm text-green-600 font-medium">In Stock: {product.stockQuantity || product.stock} units</p>
          </div>

          {/* Stock and Availability */}
          <div className="bg-white rounded-lg p-6 space-y-4">
            <h3 className="font-semibold text-gray-900">Availability & Fulfillment</h3>

            {isOutOfStock ? (
              <div className="flex gap-3 p-4 bg-red-50 border border-red-200 rounded-lg">
                <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div>
                  <p className="font-medium text-red-900">Out of Stock</p>
                  <p className="text-sm text-red-700">This item is currently unavailable</p>
                </div>
              </div>
            ) : (
              <>
                {isLowStock && (
                  <div className="flex gap-3 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                    <AlertCircle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="font-medium text-yellow-900">Limited Stock</p>
                      <p className="text-sm text-yellow-700">Only {product.stockQuantity || product.stock} left - order soon!</p>
                    </div>
                  </div>
                )}

                {/* Fulfillment Options */}
                <div className="space-y-3">
                  <div className="flex items-start gap-3 p-3 bg-blue-50 rounded-lg">
                    <Truck className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="font-medium text-gray-900">Express Delivery</p>
                      <p className="text-sm text-gray-600">Get it delivered within 2-3 business days</p>
                    </div>
                  </div>
                  <div className="flex items-start gap-3 p-3 bg-green-50 rounded-lg">
                    <Shield className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="font-medium text-gray-900">Quality Guarantee</p>
                      <p className="text-sm text-gray-600">100% authentic products with warranty</p>
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Actions */}
          <div className="bg-white rounded-lg p-6 space-y-4">
            {/* Quantity and Total */}
            <div className="flex items-center justify-between pb-4 border-b border-gray-200">
              <div className="flex items-center gap-4">
                <span className="text-sm font-medium text-gray-700">Quantity:</span>
                <div className="flex items-center border border-gray-300 rounded-lg">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="px-3 py-2 text-gray-600 hover:bg-gray-100 transition-colors"
                  >
                    -
                  </button>
                  <span className="px-4 py-2 border-x border-gray-300 min-w-[50px] text-center">
                    {quantity}
                  </span>
                  <button
                    onClick={() => setQuantity(Math.min(quantity + 1, product.stockQuantity || product.stock || 999))}
                    className="px-3 py-2 text-gray-600 hover:bg-gray-100 transition-colors"
                    disabled={quantity >= (product.stockQuantity || product.stock || 999)}
                  >
                    +
                  </button>
                </div>
              </div>
              <div className="text-right">
                <p className="text-sm text-gray-500">Total</p>
                <p className="text-2xl font-bold text-blue-600">
                  GHS {((product.effectivePrice || product.price || 0) * quantity).toFixed(2)}
                </p>
              </div>
            </div>
            
            {/* Action Buttons */}
            <div className="flex items-center gap-4">
              <Button
                variant="primary"
                size="lg"
                onClick={handleAddToCart}
                disabled={isOutOfStock}
                className="flex-1"
              >
                {isOutOfStock ? 'Out of Stock' : `Add ${quantity} to Cart`}
              </Button>
            </div>
            <div className="flex gap-3">
              <Button
                variant="outline"
                size="lg"
                onClick={handleWishlistToggle}
                className="flex-1"
              >
                <Heart className={`w-5 h-5 mr-2 ${inWishlist ? 'fill-red-500 text-red-500' : ''}`} />
                {inWishlist ? 'Remove from Wishlist' : 'Add to Wishlist'}
              </Button>
              <Button variant="outline" size="lg">
                <Share2 className="w-5 h-5" />
              </Button>
            </div>
          </div>

          {/* Product Details */}
          <div className="bg-white rounded-lg p-6">
            <h3 className="font-semibold text-gray-900 mb-4">Product Details</h3>
            <div className="space-y-3">
              <div className="flex justify-between py-2 border-b border-gray-100">
                <span className="text-gray-600">SKU</span>
                <span className="font-medium">{product.sku}</span>
              </div>
              {product.category && (
                <div className="flex justify-between py-2 border-b border-gray-100">
                  <span className="text-gray-600">Category</span>
                  <span className="font-medium">
                    {typeof product.category === 'string' ? product.category : product.category.name}
                  </span>
                </div>
              )}
              <div className="flex justify-between py-2 border-b border-gray-100">
                <span className="text-gray-600">Condition</span>
                <span className="font-medium">New</span>
              </div>
            </div>
          </div>

          {/* Description */}
          <div className="bg-white rounded-lg p-6">
            <h3 className="font-semibold text-gray-900 mb-4">Description</h3>
            <p className="text-gray-700 leading-relaxed">
              {product.description || 'No description available for this product.'}
            </p>
          </div>
        </div>
      </div>

      {/* Related Products */}
      {relatedProducts.length > 0 && (
        <section className="mb-12">
          <h2 className="text-2xl font-bold text-gray-900 mb-6">Related Products</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {relatedProducts.map((relatedProduct) => (
              <div key={relatedProduct.id} className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow">
                <Link href={`/products/${relatedProduct.slug}`}>
                  <div className="aspect-square bg-gray-100">
                    <Image
                      src={getImageUrl(relatedProduct.imageUrl || relatedProduct.image)}
                      alt={relatedProduct.name}
                      width={300}
                      height={300}
                      className="w-full h-full object-cover"
                      sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 25vw"
                    />
                  </div>
                  <div className="p-4">
                    <h3 className="font-semibold text-gray-900 mb-2 line-clamp-2">
                      {relatedProduct.name}
                    </h3>
                    <p className="text-lg font-bold text-blue-600">
                      GHS {(relatedProduct.effectivePrice || relatedProduct.price || 0).toFixed(2)}
                    </p>
                  </div>
                </Link>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Reviews Section */}
      <ProductReviews productId={product.id} />
    </div>
  );
}

export default function ProductDetailPage() {
  const params = useParams();
  const [product, setProduct] = useState<Product | null>(null);
  const [relatedProducts, setRelatedProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch product data on mount
  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const slug = params.slug as string;
        if (!slug) {
          setError('Invalid product slug');
          setLoading(false);
          return;
        }

        // Fetch product details by slug
        const productResponse = await productsApi.getBySlug(slug);
        if (!productResponse.success || !productResponse.data) {
          setError('Product not found');
          setLoading(false);
          return;
        }
        
        // Normalize the product data
        const normalizedProduct = normalizeProduct(productResponse.data);
        setProduct(normalizedProduct);

        // Fetch related products from same category
        if (normalizedProduct?.category?.id) {
          try {
            const relatedResponse = await productsApi.getByCategory(normalizedProduct.category.id, { size: 4 });
            if (relatedResponse.success && relatedResponse.data?.content) {
              // Filter out the current product and normalize
              const related = relatedResponse.data.content
                .filter((p: any) => p.id !== normalizedProduct.id)
                .slice(0, 4)
                .map(normalizeProduct);
              setRelatedProducts(related);
            }
          } catch (relatedErr) {
            console.error('Failed to fetch related products:', relatedErr);
            // Don't fail the whole page if related products fail
          }
        }
      } catch (err) {
        console.error('Failed to fetch product:', err);
        setError('Product not found or failed to load. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchProduct();
  }, [params.slug]);

  // Loading state
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading product details...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error || !product) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center bg-white rounded-lg p-8 shadow-lg">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Product Not Found</h2>
          <p className="text-gray-600 mb-6">{error || 'The product you are looking for could not be found.'}</p>
          <Link href="/products" className="text-blue-600 hover:text-blue-800 font-medium">
            Continue Shopping →
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <ProductDetailContent product={product} relatedProducts={relatedProducts} />
    </div>
  );
}
