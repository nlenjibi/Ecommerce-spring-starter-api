import { NextResponse } from 'next/server';

export async function GET(req: Request) {
  const series = [
    { date: '2026-01-28', revenue: 20 },
    { date: '2026-01-29', revenue: 50 },
    { date: '2026-01-30', revenue: 10 },
    { date: '2026-01-31', revenue: 40 },
    { date: '2026-02-01', revenue: 30 },
    { date: '2026-02-02', revenue: 60 },
  ];

  return NextResponse.json({ series, topProducts: [] });
}
