// Server-side helper to enforce seller access (stub).
// Replace with your real session/auth integration (getServerSession, cookies, etc.).

export type SellerSession = {
  sellerId: string;
  role: 'seller' | 'admin' | 'customer';
};

export function requireSeller(session?: SellerSession | null) {
  // Example guard: ensure session exists and role === 'seller'
  if (!session || session.role !== 'seller') {
    const e: any = new Error('Forbidden: seller access required');
    e.code = 403;
    throw e;
  }
  return session;
}

// Usage (server component):
// const session = getMySession(req);
// requireSeller(session);
