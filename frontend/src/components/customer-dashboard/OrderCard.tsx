"use client";
import React from 'react';

type Props = {
  order: any;
};

export default function OrderCard({ order }: Props) {
  return (
    <div className="bg-white rounded-lg shadow-sm p-4 flex items-center gap-4">
      <img src={order.thumbnail} alt="thumb" className="w-16 h-16 rounded-md object-cover" />
      <div className="flex-1">
        <div className="flex items-center justify-between">
          <div className="font-medium">{order.id}</div>
          <div className="text-sm text-gray-500">{order.date}</div>
        </div>
        <div className="mt-2 text-sm text-gray-700">Total: ${order.total.toFixed(2)}</div>
        <div className="mt-3">
          <div className="w-full bg-gray-100 rounded-full h-2 overflow-hidden">
            <div className="h-2 bg-blue-600" style={{ width: `${order.progress}%` }} />
          </div>
          <div className="mt-1 text-xs text-gray-500">Status: {order.status}</div>
        </div>
      </div>
      <div>
        <button className="px-3 py-2 bg-blue-600 text-white rounded-md">View</button>
      </div>
    </div>
  );
}
