import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Routes that require authentication
const protectedRoutes = ['/checkout', '/orders'];

// Routes that require admin role
const adminRoutes = ['/admin'];

// Routes accessible only to non-authenticated users
const authRoutes = ['/auth/login', '/auth/register'];

// Routes that require customer role (allow both 'user' and 'customer' roles)
const customerRoutes = ['/customer'];

// Routes that require seller role
const sellerRoutes = ['/seller', '/seller-dashboard'];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  
  // Get token from cookies
  const token = request.cookies.get('token')?.value;
  // Read role from cookie; support both `userRole` (client) and `role` (server) keys
  const userRole = request.cookies.get('userRole')?.value || request.cookies.get('role')?.value;

  // Check if trying to access auth routes while logged in
  if (authRoutes.some((route) => pathname.startsWith(route))) {
    if (token) {
      return NextResponse.redirect(new URL('/', request.url));
    }
    return NextResponse.next();
  }

// Check if trying to access protected routes without token
  if (protectedRoutes.some((route) => pathname.startsWith(route))) {
    if (!token) {
      const loginUrl = new URL('/auth/login', request.url);
      loginUrl.searchParams.set('callbackUrl', pathname);
      
      // Special handling for checkout - preserve cart information
      if (pathname.startsWith('/checkout')) {
        const cartCookie = request.cookies.get('cart_id')?.value;
        if (cartCookie) {
          loginUrl.searchParams.set('preserve_cart', 'true');
        }
      }
      
      return NextResponse.redirect(loginUrl);
    }
  }

  // Check if trying to access admin routes without admin role
  if (adminRoutes.some((route) => pathname.startsWith(route))) {
    if (!token) {
      const loginUrl = new URL('/auth/login', request.url);
      loginUrl.searchParams.set('callbackUrl', pathname);
      return NextResponse.redirect(loginUrl);
    }
    
    if (userRole !== 'admin') {
      return NextResponse.redirect(new URL('/unauthorized', request.url));
    }
  }

  // Check customer routes: allow both 'user' and 'customer' roles
  if (customerRoutes.some((route) => pathname.startsWith(route))) {
    if (!token) {
      const loginUrl = new URL('/auth/login', request.url);
      loginUrl.searchParams.set('callbackUrl', pathname);
      return NextResponse.redirect(loginUrl);
    }

    if (userRole !== 'customer' && userRole !== 'user') {
      return NextResponse.redirect(new URL('/unauthorized', request.url));
    }
  }

  // Check seller routes: require seller role
  if (sellerRoutes.some((route) => pathname.startsWith(route))) {
    if (!token) {
      const loginUrl = new URL('/auth/login', request.url);
      loginUrl.searchParams.set('callbackUrl', pathname);
      return NextResponse.redirect(loginUrl);
    }

    if (userRole !== 'seller') {
      return NextResponse.redirect(new URL('/unauthorized', request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public files
     */
    '/((?!api|_next/static|_next/image|favicon.ico|.*\\..*|_next).*)',
  ],
};
