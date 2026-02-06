'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useCart } from '@/context/CartContext';
import { Button } from './ui/Button';
import { getImageUrl } from '@/lib/utils';

export function CartDropdown() {
  const { items, total, itemCount } = useCart();

  return (
    <div className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-xl py-2 z-50">
      <div className="px-4 py-2 border-b">
        <h3 className="font-semibold text-gray-900">Your Cart ({itemCount})</h3>
      </div>
      {items.length > 0 ? (
        <>
          <div className="max-h-64 overflow-y-auto">
            {items.map((item) => {
              const unitPrice = item.product.effectivePrice || item.product.price;
              const itemTotal = unitPrice * item.quantity;
              
              return (
                <div key={item.id} className="flex items-center gap-4 px-4 py-3 border-b">
                  <Image
                    src={getImageUrl(item.product.imageUrl || item.product.image)}
                    alt={item.product.name}
                    width={64}
                    height={64}
                    className="rounded-md object-cover"
                  />
                  <div className="flex-1">
                    <p className="font-semibold text-sm text-gray-800">{item.product.name}</p>
                    <p className="text-xs text-gray-500">
                      {item.quantity} x GHS {unitPrice.toFixed(2)}
                    </p>
                  </div>
                  <p className="font-semibold text-sm text-gray-900">GHS {itemTotal.toFixed(2)}</p>
                </div>
              );
            })}
          </div>
          <div className="px-4 py-3">
            <div className="flex justify-between items-center mb-4">
              <span className="font-semibold text-gray-900">Subtotal</span>
              <span className="font-bold text-xl text-blue-600">GHS {total.toFixed(2)}</span>
            </div>
            <div className="flex flex-col gap-2">
              <Link href="/cart">
                <Button variant="secondary" className="w-full">
                  View Cart
                </Button>
              </Link>
              <Link href="/checkout">
                <Button variant="primary" className="w-full">
                  Checkout
                </Button>
              </Link>
            </div>
          </div>
        </>
      ) : (
        <div className="px-4 py-6 text-center">
          <p className="text-gray-600">Your cart is empty.</p>
        </div>
      )}
    </div>
  );
}
