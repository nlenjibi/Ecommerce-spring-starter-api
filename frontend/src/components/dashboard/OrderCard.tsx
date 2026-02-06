import React from 'react';

export type OrderSummary = {
  id: string;
  orderNumber: string;
  thumbnail?: string;
  status: 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  totalAmount: number;
  placedAt: string;
};

export default function OrderCard({ order, onView }: { order: OrderSummary; onView: (id: string) => void }) {
  const statusClasses: Record<string, string> = {
    PENDING: 'bg-amber-100 text-amber-800',
    PROCESSING: 'bg-blue-100 text-blue-800',
    SHIPPED: 'bg-indigo-100 text-indigo-800',
    DELIVERED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  };

  return (
    <div className="bg-white rounded-lg shadow p-4 flex gap-4 items-start">
      <div className="w-16 h-16 rounded overflow-hidden bg-gray-100 flex-shrink-0">
        {order.thumbnail ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={order.thumbnail} alt={`Order ${order.orderNumber}`} className="w-full h-full object-cover" />
        ) : (
          <div className="w-full h-full grid place-items-center text-gray-400">No image</div>
        )}
      </div>

      <div className="flex-1">
        <div className="flex justify-between items-center">
          <div className="text-sm text-gray-600">#{order.orderNumber}</div>
          <div className={`px-2 py-1 rounded-full text-xs font-semibold ${statusClasses[order.status]}`}>{order.status}</div>
        </div>
        <div className="mt-2 text-gray-800 font-medium">${order.totalAmount.toFixed(2)}</div>
        <div className="text-xs text-gray-500 mt-1">{new Date(order.placedAt).toLocaleDateString()}</div>
        <div className="mt-3 flex gap-2">
          <button onClick={() => onView(order.id)} className="px-3 py-2 bg-blue-600 text-white rounded-lg text-sm">View Details</button>
          <button className="px-3 py-2 border rounded-lg text-sm">Reorder</button>
        </div>
      </div>
    </div>
  );
}
