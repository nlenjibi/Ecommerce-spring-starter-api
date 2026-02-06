"use client";
import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { authApi } from '@/lib/api';
import { authUtils } from '@/lib/auth';

interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  address?: string;
  role: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  updateUser: (userData: Partial<User>) => Promise<void>;
  changePassword: (oldPassword: string, newPassword: string) => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Check for existing session on mount
  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const token = authUtils.getToken();
      if (!token) {
        setLoading(false);
        return;
      }

      // Try to get user profile with existing token
      const response = await authApi.getProfile();
      const profile = (response as any)?.data || (response as any)?.user || response;
      // Ensure local storage + role cookie are set for client usage
      try {
        authUtils.setUser(profile);
      } catch {}
      setUser(profile);
    } catch (err) {
      console.error('Auth check failed:', err);
      // Clear invalid tokens
      authUtils.removeToken();
      authUtils.removeUser();
    } finally {
      setLoading(false);
    }
  };

  const login = async (email: string, password: string) => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await authApi.login(email, password);
      const userData = (response as any)?.data || (response as any)?.user || response;
      const sessionToken = (response as any)?.data?.sessionToken || (response as any)?.sessionToken || null;
      const refreshToken = (response as any)?.data?.refreshToken || (response as any)?.refreshToken || null;

      // Store token and user in cookies/localStorage so middleware and API client work
      if (sessionToken) {
        authUtils.setToken(sessionToken);
      }
      if (refreshToken) {
        authUtils.setRefreshToken(refreshToken);
      }
      try {
        authUtils.setUser(userData);
      } catch {}

      // Set user data in context
      setUser(userData);
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || 'Login failed';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    // Clear tokens and user state
    authUtils.logout();
    setUser(null);
    setError(null);

    // Redirect to login page
    if (typeof window !== 'undefined') {
      window.location.href = '/auth/login';
    }
  };

  const updateUser = async (userData: Partial<User>) => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await authApi.updateProfile(userData);
      const updated = (response as any)?.data || (response as any)?.user || response;
      setUser(updated);
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || 'Failed to update profile';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  };

  const changePassword = async (oldPassword: string, newPassword: string) => {
    try {
      setLoading(true);
      setError(null);
      
      await authApi.changePassword({
        currentPassword: oldPassword,
        newPassword: newPassword
      });
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || 'Failed to change password';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  };

  const refreshUser = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await authApi.getProfile();
      const profile = (response as any)?.data || (response as any)?.user || response;
      setUser(profile);
    } catch (err: any) {
      const message = err.response?.data?.message || err.message || 'Failed to refresh user data';
      setError(message);
      throw new Error(message);
    } finally {
      setLoading(false);
    }
  };

  const value: AuthContextType = {
    user,
    loading,
    error,
    login,
    logout,
    updateUser,
    changePassword,
    refreshUser,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

// HOC to protect routes
export function withAuth<P extends object>(Component: React.ComponentType<P>) {
  return function AuthenticatedComponent(props: P) {
    const { user, loading } = useAuth();
    
    if (loading) {
      return (
        <div className="min-h-screen flex items-center justify-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>
      );
    }
    
    if (!user) {
      if (typeof window !== 'undefined') {
        window.location.href = '/auth/login';
      }
      return null;
    }
    
    return <Component {...props} />;
  };
}