import { NextResponse } from 'next/server';

export async function GET(req: Request) {
  // Normally you'd validate session and sellerId here server-side.
  const products = [
    { id: 'p1', name: 'Red Shirt', sku: 'RS-001', price: 29.99, stock: 12, active: true, image: '/placeholder.png' },
    { id: 'p2', name: 'Green Hat', sku: 'GH-002', price: 14.5, stock: 2, active: true, image: '/placeholder.png' },
  ];

  return NextResponse.json({ items: products, total: products.length });
}
