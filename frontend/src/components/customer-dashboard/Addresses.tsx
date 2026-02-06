"use client";
import React from 'react';

export default function Addresses() {
  const addr = {
    name: 'Home',
    street: '123 Main St',
    city: 'Lagos',
    phone: '+234 800 000 000',
  };

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">Saved Addresses</h3>
        <button className="text-sm text-blue-600">Add</button>
      </div>
      <div className="mt-3 text-sm text-gray-700">
        <div className="font-medium">{addr.name}</div>
        <div>{addr.street}</div>
        <div>{addr.city}</div>
        <div className="text-xs text-gray-500">{addr.phone}</div>
        <div className="mt-3 flex gap-2">
          <button className="px-2 py-1 border rounded">Edit</button>
          <button className="px-2 py-1 border rounded">Delete</button>
        </div>
      </div>
    </div>
  );
}
