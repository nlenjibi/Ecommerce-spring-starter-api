'use client';

import React, { createContext, useContext, useReducer, useEffect, ReactNode } from 'react';
import { User, AuthState } from '@/types';
import { authApi } from '@/lib/api';
import { authUtils } from '@/lib/auth';
import { useCart } from './CartContext';
import { USER_ROLES } from '@/constants/roles';
import toast from 'react-hot-toast';

interface AuthContextType extends AuthState {
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  register: (data: { username?: string; firstName?: string; lastName?: string; email: string; phoneNumber?: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
  updateProfile: (data: Partial<{ firstName: string; lastName: string; phone: string }>) => Promise<void>;
}

type AuthAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_USER'; payload: User }
  | { type: 'LOGOUT' }
  | { type: 'UPDATE_USER'; payload: User };

const initialState: AuthState = {
  user: null,
  token: null,
  isAuthenticated: false,
  loading: true,
};

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    case 'SET_USER':
      return {
        ...state,
        user: action.payload,
        isAuthenticated: true,
        loading: false,
      };
    case 'LOGOUT':
      return {
        ...state,
        user: null,
        token: null,
        isAuthenticated: false,
        loading: false,
      };
    case 'UPDATE_USER':
      return { ...state, user: action.payload };
    default:
      return state;
  }
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        if (authUtils.isAuthenticated()) {
          // Restore from cached user in localStorage for instant UX
          const cached = authUtils.getUser();
          if (cached) {
            const normalized: any = {
              id: cached.id,
              email: cached.email,
              firstName: cached.firstName,
              lastName: cached.lastName,
              role: cached.role || USER_ROLES.USER,
              avatar: (cached as any).avatar || null,
            };
            dispatch({ type: 'SET_USER', payload: normalized });

            // Refresh profile in background and update cache
            try {
              const profileResponse = await authApi.getProfile();
              const profile = (profileResponse as any)?.data || (profileResponse as any)?.user || profileResponse || null;
              if (profile) {
                const updated: any = {
                  id: profile.id,
                  email: profile.email,
                  firstName: profile.firstName,
                  lastName: profile.lastName,
role: profile.role || USER_ROLES.USER,
                  avatar: profile.avatar || null,
                };
                dispatch({ type: 'SET_USER', payload: updated });
                authUtils.setUser(updated);
              }
            } catch {
              // ignore background refresh errors
            }
          } else {
            // No cached user, try to fetch profile
            const profileResponse = await authApi.getProfile();
            const profile = (profileResponse as any)?.data || (profileResponse as any)?.user || profileResponse || null;
            if (profile) {
              const normalized: any = {
                id: profile.id,
                email: profile.email,
                firstName: profile.firstName,
                lastName: profile.lastName,
                role: (profile.role || 'user').toLowerCase(),
                avatar: profile.avatar || null,
              };
              dispatch({ type: 'SET_USER', payload: normalized });
              authUtils.setUser(normalized);
            } else {
              dispatch({ type: 'SET_LOADING', payload: false });
            }
          }
        } else {
          dispatch({ type: 'SET_LOADING', payload: false });
        }
      } catch (error) {
        // Token is invalid, user needs to login again
        dispatch({ type: 'SET_LOADING', payload: false });
      }
    };

    initializeAuth();
  }, []);

  const login = async (usernameOrEmail: string, password: string) => {
    dispatch({ type: 'SET_LOADING', payload: true });
    try {
      const response = await authApi.login(usernameOrEmail, password);
      const user = (response as any)?.data || (response as any) || null;
      const sessionToken = (response as any)?.data?.sessionToken || (response as any)?.sessionToken || null;

      const refreshToken = (response as any)?.data?.refreshToken || null;
      if (refreshToken) {
        authUtils.setRefreshToken(refreshToken);
      }

      if (sessionToken) {
        authUtils.setToken(sessionToken);
      }
      if (user) {
        // Normalize to our User shape if possible
        const normalized: any = {
          id: user.id,
          email: user.email,
          firstName: user.firstName,
          lastName: user.lastName,
          role: (user.role || 'user').toLowerCase(),
          avatar: (user.avatar || null),
        };
        dispatch({ type: 'SET_USER', payload: normalized });
        // Persist user to localStorage so session survives refresh
        try {
          authUtils.setUser(normalized);
        } catch {}
      }

      // Note: Cart merge will be handled by CartMergeHandler component
      // to avoid hook conflicts in this context
    } catch (error) {
      dispatch({ type: 'SET_LOADING', payload: false });
      throw error;
    }
  };

  const register = async (data: { username?: string; firstName?: string; lastName?: string; email: string; phoneNumber?: string; password: string }) => {
    dispatch({ type: 'SET_LOADING', payload: true });
    try {
      const response = await authApi.register(data);
      const user = (response as any)?.data || (response as any) || null;
      const sessionToken = (response as any)?.data?.sessionToken || (response as any)?.sessionToken || null;

      const refreshToken = (response as any)?.data?.refreshToken || null;
      if (refreshToken) {
        authUtils.setRefreshToken(refreshToken);
      }

      if (sessionToken) {
        authUtils.setToken(sessionToken);
      }

      if (user) {
        const normalized: any = {
          id: user.id,
          email: user.email,
          firstName: user.firstName,
          lastName: user.lastName,
          role: (user.role || 'user').toLowerCase(),
          avatar: (user.avatar || null),
        };
        dispatch({ type: 'SET_USER', payload: normalized });
        // Persist user to localStorage so session survives refresh
        try {
          authUtils.setUser(normalized);
        } catch {}
        // Show success toast on successful registration
        try {
          toast.success('Account created successfully!');
        } catch (e) {
          // no-op if toast fails for some reason
        }
      }
      
      // Note: Cart merge will be handled by CartMergeHandler component
      // to avoid hook conflicts in this context
    } catch (error) {
      dispatch({ type: 'SET_LOADING', payload: false });
      throw error;
    }
  };

  const logout = async () => {
    try {
      // Use authUtils to clear local tokens/storage
      authUtils.logout();
      // Also call server logout if available (best-effort)
      try {
        if ((authApi as any).logout) await (authApi as any).logout();
      } catch {}
    } finally {
      dispatch({ type: 'LOGOUT' });
    }
  };

  const updateProfile = async (data: Partial<{ firstName: string; lastName: string; phone: string }>) => {
    const response = await authApi.updateProfile(data);
    const updated = (response as any)?.data || (response as any)?.user || response;
    const normalized: any = {
      id: updated.id,
      email: updated.email,
      firstName: updated.firstName,
      lastName: updated.lastName,
      role: (updated.role || 'user').toLowerCase(),
      avatar: updated.avatar || null,
    };
    dispatch({ type: 'UPDATE_USER', payload: normalized });
  };

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        register,
        logout,
        updateProfile,
      }}
    >
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
