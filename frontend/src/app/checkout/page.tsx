'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useCart } from '@/context/CartContext';
import { useCheckout } from '@/hooks/useCheckout';
import { Button } from '@/components/ui/Button';
import CheckoutAuthGate from '@/components/CheckoutAuthGate';
import toast from 'react-hot-toast';

interface ShippingAddress {
  fullName: string;
  email: string;
  phone: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export default function CheckoutPage() {
  const router = useRouter();
  const { cart, items, subtotal, discount, itemCount } = useCart();
  const { checkout, isProcessing, validationErrors } = useCheckout();

  const [shippingAddress, setShippingAddress] = useState<ShippingAddress>({
    fullName: '',
    email: '',
    phone: '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'Ghana',
  });

  const [shippingMethod, setShippingMethod] = useState<'STANDARD' | 'EXPRESS' | 'OVERNIGHT'>('STANDARD');
  const [paymentMethod, setPaymentMethod] = useState<'CREDIT_CARD' | 'DEBIT_CARD' | 'PAYPAL' | 'BANK_TRANSFER' | 'MOBILE_MONEY'>('MOBILE_MONEY');
  const [customerNotes, setCustomerNotes] = useState('');
  const [agreedToTerms, setAgreedToTerms] = useState(false);

  // Redirect if cart is empty
  useEffect(() => {
    if (!cart || itemCount === 0) {
      toast.error('Your cart is empty');
      router.push('/cart');
    }
  }, [cart, itemCount, router]);

  // Calculate totals
  const shippingCost = shippingMethod === 'STANDARD' ? 0 : shippingMethod === 'EXPRESS' ? 15 : 30;
  const taxRate = 0.08;
  const taxAmount = (subtotal - discount) * taxRate;
  const total = subtotal - discount + shippingCost + taxAmount;

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setShippingAddress(prev => ({ ...prev, [name]: value }));
  };

  const validateForm = (): boolean => {
    if (!shippingAddress.fullName) {
      toast.error('Please enter your full name');
      return false;
    }
    if (!shippingAddress.email) {
      toast.error('Please enter your email');
      return false;
    }
    if (!shippingAddress.phone) {
      toast.error('Please enter your phone number');
      return false;
    }
    if (!shippingAddress.addressLine1) {
      toast.error('Please enter your address');
      return false;
    }
    if (!shippingAddress.city) {
      toast.error('Please enter your city');
      return false;
    }
    if (!shippingAddress.state) {
      toast.error('Please enter your state/region');
      return false;
    }
    if (!agreedToTerms) {
      toast.error('Please agree to the terms and conditions');
      return false;
    }
    return true;
  };

  const handleCheckout = async () => {
    if (!validateForm()) return;

    // Format shipping address
    const formattedAddress = `${shippingAddress.addressLine1}${shippingAddress.addressLine2 ? ', ' + shippingAddress.addressLine2 : ''}, ${shippingAddress.city}, ${shippingAddress.state} ${shippingAddress.postalCode}, ${shippingAddress.country}`;

    // Prepare order data for API
    await checkout({
      shippingMethod,
      paymentMethod,
      customerEmail: shippingAddress.email,
      customerName: shippingAddress.fullName,
      customerNotes,
      shippingAddress: formattedAddress,
      // Optionally, you can add couponCode, taxRate, couponDiscount if available in context
      // couponCode: cart?.couponCode,
      // taxRate,
      // couponDiscount: discount,
    });
  };

  if (!cart || itemCount === 0) {
    return null;
  }

  return (
    <CheckoutAuthGate>
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-8">Checkout</h1>

          {/* Validation Errors */}
          {validationErrors.length > 0 && (
            <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4">
              <h3 className="text-red-800 font-semibold mb-2">
                Please resolve the following issues:
              </h3>
              <ul className="list-disc list-inside space-y-1">
                {validationErrors.map((error, index) => (
                  <li key={index} className="text-red-700 text-sm">
                    {error.message}
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Left Column - Forms */}
            <div className="lg:col-span-2 space-y-6">
              {/* Shipping Address */}
              <div className="bg-white rounded-xl shadow-md p-6">
                <h2 className="text-xl font-bold text-gray-900 mb-4">
                  Shipping Address
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Full Name *
                    </label>
                    <input
                      type="text"
                      name="fullName"
                      value={shippingAddress.fullName}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Email *
                    </label>
                    <input
                      type="email"
                      name="email"
                      value={shippingAddress.email}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Phone *
                    </label>
                    <input
                      type="tel"
                      name="phone"
                      value={shippingAddress.phone}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Address Line 1 *
                    </label>
                    <input
                      type="text"
                      name="addressLine1"
                      value={shippingAddress.addressLine1}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Address Line 2 (Optional)
                    </label>
                    <input
                      type="text"
                      name="addressLine2"
                      value={shippingAddress.addressLine2}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      City *
                    </label>
                    <input
                      type="text"
                      name="city"
                      value={shippingAddress.city}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      State/Region *
                    </label>
                    <input
                      type="text"
                      name="state"
                      value={shippingAddress.state}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Postal Code
                    </label>
                    <input
                      type="text"
                      name="postalCode"
                      value={shippingAddress.postalCode}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Country *
                    </label>
                    <input
                      type="text"
                      name="country"
                      value={shippingAddress.country}
                      onChange={handleInputChange}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      required
                    />
                  </div>
                </div>
              </div>

              {/* Shipping Method */}
              <div className="bg-white rounded-xl shadow-md p-6">
                <h2 className="text-xl font-bold text-gray-900 mb-4">
                  Shipping Method
                </h2>
                <div className="space-y-3">
                  {[
                    { value: 'STANDARD', label: 'Standard Shipping', cost: 0, days: '5-7 business days' },
                    { value: 'EXPRESS', label: 'Express Shipping', cost: 15, days: '2-3 business days' },
                    { value: 'OVERNIGHT', label: 'Overnight Shipping', cost: 30, days: 'Next business day' },
                  ].map((method) => (
                    <label
                      key={method.value}
                      className={`flex items-center justify-between p-4 border rounded-lg cursor-pointer transition ${
                        shippingMethod === method.value
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-300 hover:border-blue-300'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <input
                          type="radio"
                          name="shippingMethod"
                          value={method.value}
                          checked={shippingMethod === method.value}
                          onChange={(e) => setShippingMethod(e.target.value as any)}
                          className="w-4 h-4 text-blue-600"
                        />
                        <div>
                          <p className="font-medium text-gray-900">{method.label}</p>
                          <p className="text-sm text-gray-500">{method.days}</p>
                        </div>
                      </div>
                      <p className="font-semibold text-gray-900">
                        {method.cost === 0 ? 'Free' : `GHS ${method.cost.toFixed(2)}`}
                      </p>
                    </label>
                  ))}
                </div>
              </div>

              {/* Payment Method */}
              <div className="bg-white rounded-xl shadow-md p-6">
                <h2 className="text-xl font-bold text-gray-900 mb-4">
                  Payment Method
                </h2>
                <div className="space-y-3">
                  {[
                    { value: 'MOBILE_MONEY', label: 'Mobile Money (MTN/Vodafone)', icon: '📱' },
                    { value: 'CREDIT_CARD', label: 'Credit Card', icon: '💳' },
                    { value: 'DEBIT_CARD', label: 'Debit Card', icon: '💳' },
                    { value: 'BANK_TRANSFER', label: 'Bank Transfer', icon: '🏦' },
                    { value: 'PAYPAL', label: 'PayPal', icon: '🅿️' },
                  ].map((method) => (
                    <label
                      key={method.value}
                      className={`flex items-center gap-3 p-4 border rounded-lg cursor-pointer transition ${
                        paymentMethod === method.value
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-300 hover:border-blue-300'
                      }`}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value={method.value}
                        checked={paymentMethod === method.value}
                        onChange={(e) => setPaymentMethod(e.target.value as any)}
                        className="w-4 h-4 text-blue-600"
                      />
                      <span className="text-2xl">{method.icon}</span>
                      <p className="font-medium text-gray-900">{method.label}</p>
                    </label>
                  ))}
                </div>
              </div>

              {/* Order Notes */}
              <div className="bg-white rounded-xl shadow-md p-6">
                <h2 className="text-xl font-bold text-gray-900 mb-4">
                  Order Notes (Optional)
                </h2>
                <textarea
                  value={customerNotes}
                  onChange={(e) => setCustomerNotes(e.target.value)}
                  placeholder="Any special instructions for your order?"
                  rows={4}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>

              {/* Terms and Conditions */}
              <div className="bg-white rounded-xl shadow-md p-6">
                <label className="flex items-start gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={agreedToTerms}
                    onChange={(e) => setAgreedToTerms(e.target.checked)}
                    className="mt-1 w-4 h-4 text-blue-600 rounded"
                  />
                  <span className="text-sm text-gray-700">
                    I agree to the{' '}
                    <a href="/terms" className="text-blue-600 hover:underline">
                      Terms and Conditions
                    </a>{' '}
                    and{' '}
                    <a href="/privacy" className="text-blue-600 hover:underline">
                      Privacy Policy
                    </a>
                    *
                  </span>
                </label>
              </div>
            </div>

            {/* Right Column - Order Summary */}
            <div className="lg:col-span-1">
              <div className="bg-white rounded-xl shadow-md p-6 sticky top-24">
                <h2 className="text-xl font-bold text-gray-900 mb-6">
                  Order Summary
                </h2>

                {/* Items */}
                <div className="space-y-3 mb-6 max-h-64 overflow-y-auto">
                  {items.map((item) => (
                    <div key={item.id} className="flex items-center gap-3">
                      <div className="w-16 h-16 bg-gray-100 rounded-lg flex items-center justify-center shrink-0">
                        {item.product.imageUrl ? (
                          <img
                            src={item.product.imageUrl}
                            alt={item.product.name}
                            className="w-full h-full object-cover rounded-lg"
                          />
                        ) : (
                          <span>📦</span>
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {item.product.name}
                        </p>
                        <p className="text-xs text-gray-500">
                          Qty: {item.quantity} × GHS {item.unitPrice.toFixed(2)}
                        </p>
                      </div>
                      <p className="text-sm font-semibold text-gray-900">
                        GHS {item.totalPrice.toFixed(2)}
                      </p>
                    </div>
                  ))}
                </div>

                {/* Pricing */}
                <div className="space-y-3 mb-6">
                  <div className="flex justify-between text-gray-600">
                    <span>Subtotal</span>
                    <span>GHS {subtotal.toFixed(2)}</span>
                  </div>

                  {discount > 0 && (
                    <div className="flex justify-between text-green-600">
                      <span>Discount</span>
                      <span>-GHS {discount.toFixed(2)}</span>
                    </div>
                  )}

                  <div className="flex justify-between text-gray-600">
                    <span>Shipping</span>
                    <span>{shippingCost === 0 ? 'Free' : `GHS ${shippingCost.toFixed(2)}`}</span>
                  </div>

                  <div className="flex justify-between text-gray-600">
                    <span>Tax (8%)</span>
                    <span>GHS {taxAmount.toFixed(2)}</span>
                  </div>

                  <hr />

                  <div className="flex justify-between text-xl font-bold text-gray-900">
                    <span>Total</span>
                    <span>GHS {total.toFixed(2)}</span>
                  </div>
                </div>

                {/* Checkout Button */}
                <Button
                  variant="primary"
                  size="lg"
                  className="w-full"
                  onClick={handleCheckout}
                  disabled={isProcessing || !agreedToTerms}
                >
                  {isProcessing ? (
                    <span className="flex items-center justify-center gap-2">
                      <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      Processing...
                    </span>
                  ) : (
                    `Place Order - GHS ${total.toFixed(2)}`
                  )}
                </Button>

                <p className="text-xs text-gray-500 text-center mt-4">
                  Your payment information is secure and encrypted
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </CheckoutAuthGate>
  );
}