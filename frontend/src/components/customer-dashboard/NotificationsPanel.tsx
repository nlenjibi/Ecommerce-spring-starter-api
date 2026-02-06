"use client";
import React from 'react';

export default function NotificationsPanel() {
  const mock = [
    { id: 1, text: 'Your order ORD-1001 has been shipped.' },
    { id: 2, text: 'Flash sale: 20% off selected items!' },
  ];

  return (
    <div className="bg-white rounded-lg shadow-sm p-4">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold">Notifications</h3>
        <div className="text-sm text-gray-500">Recent</div>
      </div>
      <ul className="mt-3 space-y-2 text-sm text-gray-700">
        {mock.map((n) => (
          <li key={n.id} className="p-2 rounded hover:bg-gray-50">{n.text}</li>
        ))}
      </ul>
    </div>
  );
}
