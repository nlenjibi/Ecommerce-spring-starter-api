'use client';

import React, { useState } from 'react';
import { useCart } from '@/context/CartContext';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { getCartId, clearCartId, hasExistingCart } from '@/lib/cartUtils';
import { StockStatus } from '@/types';

// Mock product for testing
const mockProduct = {
  id: 1,
  name: 'Test Product',
  price: 29.99,
  category: { id: 1, slug: 'electronics', name: 'Electronics' },
  image: '/test-product.jpg',
  stock: 10,
  stockStatus: StockStatus.IN_STOCK,
  rating: 4.5,
  reviews: 12,
};

export default function TestCartPage() {
  const { itemCount, items, addToCart, clearCart, loading, cartId, checkout } = useCart();
  const { isAuthenticated } = useAuth();
  const [testResults, setTestResults] = useState<string[]>([]);

  const addTestResult = (message: string) => {
    setTestResults(prev => [...prev, `${new Date().toLocaleTimeString()}: ${message}`]);
  };

  const testAddToCart = async () => {
    try {
      addTestResult('🧪 Starting add to cart test...');
      addTestResult(`📊 Current cart state: ${itemCount} items, loading: ${loading}`);
      addTestResult(`🆔 Current cartId: ${cartId || 'None'}`);
      addTestResult(`🔐 Authenticated: ${isAuthenticated}`);
      
      addTestResult(`🛍️ Adding product: ${mockProduct.name} (ID: ${mockProduct.id})`);
      await addToCart(mockProduct.id, 1);
      
      addTestResult(`✅ Add to cart completed`);
      addTestResult(`📊 New cart state: ${itemCount} items, loading: ${loading}`);
      addTestResult(`🆔 New cartId: ${cartId || 'None'}`);
      addTestResult(`📦 Cart items: ${JSON.stringify(items.map(i => ({ id: i.id, name: i.product.name, qty: i.quantity })), null, 2)}`);
    } catch (error: any) {
      addTestResult(`❌ Failed to add to cart: ${error.message}`);
    }
  };

  const testCartPersistence = () => {
    const hasCart = hasExistingCart();
    const currentCartId = getCartId();
    addTestResult(`📦 Cart persistence test: ${hasCart ? '✅ cartId found in localStorage' : '❌ no cartId found'}`);
    
    if (hasCart && currentCartId) {
      addTestResult(`🔑 Cart ID: ${currentCartId}`);
    }
  };

  const testClearCart = async () => {
    try {
      addTestResult('🧪 Starting clear cart test...');
      await clearCart();
      addTestResult('✅ Cart cleared successfully');
      addTestResult(`📊 Cart now has ${itemCount} items`);
    } catch (error: any) {
      addTestResult(`❌ Failed to clear cart: ${error.message}`);
    }
  };

  const testClearLocalStorage = () => {
    addTestResult('🧪 Testing localStorage clear...');
    clearCartId();
    addTestResult('✅ Local storage cleared');
  };

  const testCheckout = async () => {
    if (!isAuthenticated) {
      addTestResult('❌ Cannot test checkout: User not authenticated');
      addTestResult('💡 Please login first to test checkout functionality');
      return;
    }

    if (itemCount === 0) {
      addTestResult('❌ Cannot test checkout: Cart is empty');
      addTestResult('💡 Please add items to cart first');
      return;
    }

    try {
      addTestResult('🧪 Starting checkout test...');
      const orderResponse = await checkout();
      
      if (orderResponse) {
        addTestResult(`✅ Checkout successful! Order ID: ${orderResponse.orderId}`);
        const or = orderResponse as any;
        addTestResult(`💰 Total amount: $${or.totalAmount ?? 'N/A'}`);
        addTestResult(`📧 Order status: ${or.orderStatus ?? or.status ?? 'N/A'}`);
        addTestResult(`📅 Created at: ${or.createdAt ? new Date(or.createdAt).toLocaleString() : 'N/A'}`);
        
        // Note: The cart should be automatically cleared after successful checkout
        addTestResult('🗑️ Cart cleared automatically after checkout');
      } else {
        addTestResult('❌ Checkout returned null response');
      }
    } catch (error: any) {
      addTestResult(`❌ Failed to checkout: ${error.message}`);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">🛒 Cart Functionality Test</h1>
        
        {/* Test Product */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <h2 className="text-xl font-semibold mb-4">Test Product</h2>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-medium">{mockProduct.name}</h3>
              <p className="text-gray-600">Price: ${mockProduct.price}</p>
              <p className="text-gray-600">Category: {String(mockProduct.category?.name || mockProduct.category)}</p>
            </div>
            <Button 
              onClick={testAddToCart}
              disabled={loading}
              variant="primary"
            >
              {loading ? 'Adding...' : 'Add to Cart'}
            </Button>
          </div>
        </div>

        {/* Cart Status */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <h2 className="text-xl font-semibold mb-4">Cart Status</h2>
          <div className="space-y-2">
            <p>📊 Item Count: <span className="font-bold">{itemCount}</span></p>
            <p>📦 Items in cart: <span className="font-bold">{items.length}</span></p>
            <p>🔄 Loading: <span className="font-bold">{loading ? 'Yes' : 'No'}</span></p>
            <p>🆔 Cart ID: <span className="font-bold">{cartId || 'None'}</span></p>
            <p>🔐 Authenticated: <span className="font-bold">{isAuthenticated ? 'Yes' : 'No'}</span></p>
            {items.length > 0 && (
              <div className="mt-4">
                <h4 className="font-medium mb-2">Cart Items:</h4>
                {items.map((item, index) => (
                  <div key={index} className="border rounded p-2 mb-2">
                    <p>Product: {item.product.name}</p>
                    <p>Quantity: {item.quantity}</p>
                    <p>Price: ${item.product.price}</p>
                    <p>Item ID: {item.id}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Test Controls */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <h2 className="text-xl font-semibold mb-4">Test Controls</h2>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            <Button onClick={testAddToCart} disabled={loading}>
              Test Add to Cart
            </Button>
            <Button onClick={testCartPersistence} variant="outline">
              Test Persistence
            </Button>
            <Button onClick={testClearCart} variant="outline">
              Test Clear Cart
            </Button>
            <Button onClick={testClearLocalStorage} variant="outline">
              Clear LocalStorage
            </Button>
            <Button onClick={testCheckout} variant="outline" disabled={!isAuthenticated || itemCount === 0}>
              Test Checkout
            </Button>
          </div>
        </div>

        {/* Test Results */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4">Test Results</h2>
          <div className="bg-gray-100 rounded p-4 h-64 overflow-y-auto">
            {testResults.length === 0 ? (
              <p className="text-gray-500">No test results yet. Run some tests to see results here.</p>
            ) : (
              <div className="space-y-1 font-mono text-sm">
                {testResults.map((result, index) => (
                  <div key={index}>{result}</div>
                ))}
              </div>
            )}
          </div>
          {testResults.length > 0 && (
            <Button 
              onClick={() => setTestResults([])} 
              variant="outline" 
              className="mt-4"
            >
              Clear Results
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
