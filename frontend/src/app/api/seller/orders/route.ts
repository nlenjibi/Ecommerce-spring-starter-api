import { NextResponse } from 'next/server';

export async function GET(req: Request) {
  const orders = [
    { id: 'O-9001', buyer: 'Jane Doe', items: [{ name: 'Red Shirt', qty: 2, sellerId: 'seller-123' }], total: 59.98, status: 'PENDING', date: '2026-02-02' },
  ];

  return NextResponse.json({ items: orders, total: orders.length });
}
