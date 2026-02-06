'use client';

import React, { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { useCartFromTracking } from '@/lib/cartFromTracking';
import { useCart } from '@/context/CartContext';
import toast from 'react-hot-toast';

export default function CartFromTrackingButton() {
  const { 
    getTrackedProducts, 
    getTrackedProductsCount, 
    createCartFromTrackedProducts,
    clearTrackedProducts 
  } = useCartFromTracking();
  
  const { addToCart } = useCart();
  const [loading, setLoading] = useState(false);

  const trackedProducts = getTrackedProducts();
  const trackedCount = getTrackedProductsCount();

  const handleCreateCartFromTracking = async () => {
    if (trackedCount === 0) {
      toast.error('No tracked products to add to cart');
      return;
    }

    setLoading(true);
    
    try {
      console.log('🛒 Creating cart from tracked products...');
      
      // Try to create cart from tracking API
      const success = await createCartFromTrackedProducts();
      
      if (success) {
        toast.success(`Cart created with ${trackedCount} tracked products!`);
        
        // Optionally, add each product to the current cart context as well
        for (const product of trackedProducts) {
          try {
            // Convert tracked product to Product format
            const productData = {
              id: product.productId,
              name: product.productName,
              category: product.category,
              price: product.price,
              // Add other required fields as needed
            };
            
            await addToCart(productData.id, 1);
          } catch (error) {
            console.error('Failed to add tracked product to cart:', error);
          }
        }
      } else {
        // Fallback: add each tracked product to cart individually
        console.log('🔄 Falling back to individual cart adds...');
        
        let addedCount = 0;
        for (const product of trackedProducts) {
          try {
            const productData = {
              id: product.productId,
              name: product.productName,
              category: product.category,
              price: product.price,
            };
            
            await addToCart(productData.id, 1);
            addedCount++;
          } catch (error) {
            console.error('Failed to add tracked product to cart:', error);
          }
        }
        
        if (addedCount > 0) {
          toast.success(`Added ${addedCount} tracked products to cart!`);
          clearTrackedProducts(); // Clear after successful addition
        } else {
          toast.error('Failed to add any tracked products to cart');
        }
      }
    } catch (error) {
      console.error('Error creating cart from tracking:', error);
      toast.error('Failed to create cart from tracked products');
    } finally {
      setLoading(false);
    }
  };

  const handleClearTrackedProducts = () => {
    clearTrackedProducts();
    toast.success('Tracked products cleared');
  };

  if (trackedCount === 0) {
    return null; // Don't show button if no tracked products
  }

  return (
    <div className="fixed bottom-4 right-4 z-50 bg-white rounded-lg shadow-lg p-4 border border-gray-200">
      <div className="text-sm text-gray-600 mb-2">
        📊 {trackedCount} tracked product{trackedCount !== 1 ? 's' : ''}
      </div>
      
      <div className="flex gap-2">
        <Button
          onClick={handleCreateCartFromTracking}
          disabled={loading}
          size="sm"
          className="text-xs"
        >
          {loading ? '🔄 Adding...' : '🛒 Add All to Cart'}
        </Button>
        
        <Button
          onClick={handleClearTrackedProducts}
          variant="outline"
          size="sm"
          className="text-xs"
        >
          🗑️ Clear
        </Button>
      </div>
      
      {trackedProducts.length > 0 && (
        <div className="mt-2 text-xs text-gray-500 max-w-xs">
          Last viewed: {trackedProducts[trackedProducts.length - 1].productName}
        </div>
      )}
    </div>
  );
}
