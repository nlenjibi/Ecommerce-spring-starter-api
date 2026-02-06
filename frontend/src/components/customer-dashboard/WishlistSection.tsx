"use client";
import React from 'react';

export default function WishlistSection() {
  const mock = [
    { id: 'P1', title: 'Blue Shirt', price: 29.99, image: '/placeholder.png', inStock: true },
    { id: 'P2', title: 'Sneakers', price: 59.99, image: '/placeholder.png', inStock: false },
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <h3 className="font-semibold">Wishlist</h3>
      <div className="mt-3 space-y-3">
        {mock.map((p) => (
          <div key={p.id} className="flex items-center gap-3">
            <img src={p.image} alt={p.title} className="w-12 h-12 rounded object-cover" />
            <div className="flex-1">
              <div className="text-sm font-medium">{p.title}</div>
              <div className="text-xs text-gray-500">${p.price.toFixed(2)} • {p.inStock ? 'In stock' : 'Out of stock'}</div>
            </div>
            <div>
              <button className="px-3 py-1 bg-blue-600 text-white rounded-md text-sm">Add to cart</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
