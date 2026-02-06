import Cookies from 'js-cookie';

const TOKEN_KEY = 'token';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_KEY = 'user';
const ROLE_KEY = 'userRole';

export interface AuthUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  // Include all roles used across the app
  role: 'user' | 'admin' | 'seller' | 'customer';
} 

export const authUtils = {
  // Get token from cookies (works on both client and can be read by middleware)
  getToken: (): string | null => {
    if (typeof window === 'undefined') return null;
    return Cookies.get(TOKEN_KEY) || null;
  },

  // Get refresh token from cookies
  getRefreshToken: (): string | null => {
    if (typeof window === 'undefined') return null;
    return Cookies.get(REFRESH_TOKEN_KEY) || null;
  },
  // set refresh token in cookies
  setRefreshToken: (token: string): void => {
    Cookies.set(REFRESH_TOKEN_KEY, token, {
      expires: 7, // 7 days
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
    });
  },
  // Set token in cookies
  setToken: (token: string): void => {
    Cookies.set(TOKEN_KEY, token, {
      expires: 7, // 7 days
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
    });
  },

  // Remove token from cookies
  removeToken: (): void => {
    Cookies.remove(TOKEN_KEY);
    Cookies.remove(REFRESH_TOKEN_KEY)
    Cookies.remove(ROLE_KEY);
  },

  // Get user from localStorage
  getUser: (): AuthUser | null => {
    if (typeof window === 'undefined') return null;
    const userStr = localStorage.getItem(USER_KEY);
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  // Set user in localStorage and role in cookies
  setUser: (user: AuthUser): void => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    Cookies.set(ROLE_KEY, user.role, {
      expires: 7,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
    });
  },

  // Remove user from localStorage
  removeUser: (): void => {
    localStorage.removeItem(USER_KEY);
  },

  // Check if user is authenticated
  isAuthenticated: (): boolean => {
    return !!authUtils.getToken();
  },

  // Check if user is admin
  isAdmin: (): boolean => {
    const user = authUtils.getUser();
    return user?.role === 'admin';
  },

  // Login helper
  login: (accessToken: string, refreshToken: string, user: AuthUser): void => {
    authUtils.setToken(accessToken);
    authUtils.setRefreshToken(refreshToken);
    authUtils.setUser(user);
  },

  // Logout helper
  logout: (): void => {
    authUtils.removeToken();
    authUtils.removeUser();
  },

  // Parse JWT token (for client-side use only)
  parseToken: (token: string): Record<string, unknown> | null => {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch {
      return null;
    }
  },

  // Check if token is expired
  isTokenExpired: (token: string): boolean => {
    const payload = authUtils.parseToken(token);
    if (!payload || !payload.exp) return true;
    const expiry = (payload.exp as number) * 1000;
    return Date.now() > expiry;
  },
};
