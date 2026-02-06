'use client';

import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productsApi, categoriesApi, ordersApi, adminApi } from './api';
import toast from 'react-hot-toast';

// ============ UTILITY HOOKS ============
export function useDebounce<T>(value: T, delay: number = 300): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return debouncedValue;
}

// ============ PRODUCT HOOKS ============
export function useProducts(params?: { page?: number; limit?: number; category?: string; search?: string }) {
  return useQuery({
    queryKey: ['products', params],
    queryFn: () => productsApi.getAll(params),
  });
}

export function useProduct(slug: string) {
  return useQuery({
    queryKey: ['product', slug],
    queryFn: () => productsApi.getBySlug(slug),
    enabled: !!slug,
  });
}

export function useFeaturedProducts() {
  return useQuery({
    queryKey: ['products', 'featured'],
    queryFn: () => productsApi.getFeatured(),
  });
}

// ============ CATEGORY HOOKS ============
export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => categoriesApi.getAll(),
  });
}

// ============ ORDER HOOKS ============
export function useOrders(params?: { page?: number; limit?: number; status?: string }) {
  return useQuery({
    queryKey: ['orders', params],
    queryFn: () => ordersApi.getAll(params),
  });
}

export function useOrder(id: number) {
  return useQuery({
    queryKey: ['order', id],
    queryFn: () => ordersApi.getById(id),
    enabled: !!id,
  });
}

export function useCreateOrder() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ordersApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      queryClient.invalidateQueries({ queryKey: ['cart'] });
      toast.success('Order placed successfully!');
    },
    onError: () => {
      toast.error('Failed to place order');
    },
  });
}

// ============ ADMIN HOOKS ============
export function useAdminDashboard(params?: { startDate?: string; endDate?: string }) {
  return useQuery({
    queryKey: ['admin', 'dashboard', params],
    queryFn: () => adminApi.getDashboardStats(params),
  });
}

export function useAdminProducts(params?: { page?: number; limit?: number; search?: string }) {
  return useQuery({
    queryKey: ['admin', 'products', params],
    queryFn: () => adminApi.getProducts(params),
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.createProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success('Product created successfully!');
    },
    onError: () => {
      toast.error('Failed to create product');
    },
  });
}

export function useUpdateProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: FormData }) => adminApi.updateProduct(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success('Product updated successfully!');
    },
    onError: () => {
      toast.error('Failed to update product');
    },
  });
}

export function useDeleteProduct() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.deleteProduct,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success('Product deleted successfully!');
    },
    onError: () => {
      toast.error('Failed to delete product');
    },
  });
}

// ============ ADMIN ORDERS HOOKS ============
export function useAdminOrders(params?: { page?: number; limit?: number; status?: string; search?: string }) {
  return useQuery({
    queryKey: ['admin', 'orders', params],
    queryFn: () => adminApi.getOrders(params),
  });
}

export function useUpdateOrderStatus() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) => adminApi.updateOrderStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'orders'] });
      toast.success('Order status updated!');
    },
    onError: () => {
      toast.error('Failed to update order status');
    },
  });
}

export function useRefundOrder() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, reason }: { id: number; reason?: string }) => adminApi.refundOrder(id, 0, reason ?? ''),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'orders'] });
      toast.success('Order refunded successfully!');
    },
    onError: () => {
      toast.error('Failed to refund order');
    },
  });
}

// ============ ADMIN USERS HOOKS ============
export function useAdminUsers(params?: { page?: number; limit?: number; search?: string; role?: string }) {
  return useQuery({
    queryKey: ['admin', 'users', params],
    queryFn: () => adminApi.getUsers(params),
  });
}

export function useUpdateUserStatus() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: 'active' | 'blocked' }) =>
      adminApi.updateUserStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User status updated!');
    },
    onError: () => {
      toast.error('Failed to update user status');
    },
  });
}

export function useUpdateUserRole() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, role }: { id: number; role: 'user' | 'admin' | 'seller' | 'customer' }) =>
      adminApi.updateUserRole(id, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User role updated!');
    },
    onError: () => {
      toast.error('Failed to update user role');
    },
  });
}

// ============ ADMIN CATEGORIES HOOKS ============
export function useAdminCategories() {
  return useQuery({
    queryKey: ['admin', 'categories'],
    queryFn: () => adminApi.getCategories(),
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.createCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'categories'] });
      queryClient.invalidateQueries({ queryKey: ['categories'] });
      toast.success('Category created!');
    },
    onError: () => {
      toast.error('Failed to create category');
    },
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<any> }) => adminApi.updateCategory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'categories'] });
      queryClient.invalidateQueries({ queryKey: ['categories'] });
      toast.success('Category updated!');
    },
    onError: () => {
      toast.error('Failed to update category');
    },
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.deleteCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'categories'] });
      queryClient.invalidateQueries({ queryKey: ['categories'] });
      toast.success('Category deleted!');
    },
    onError: () => {
      toast.error('Failed to delete category');
    },
  });
}

// ============ ADMIN PROMOTIONS HOOKS ============
export function useAdminPromotions() {
  return useQuery({
    queryKey: ['admin', 'promotions'],
    queryFn: () => adminApi.getPromotions(),
  });
}

export function useCreatePromotion() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.createPromotion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
      toast.success('Promotion created!');
    },
    onError: () => {
      toast.error('Failed to create promotion');
    },
  });
}

export function useUpdatePromotion() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<any> }) => adminApi.updatePromotion(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
      toast.success('Promotion updated!');
    },
    onError: () => {
      toast.error('Failed to update promotion');
    },
  });
}

export function useDeletePromotion() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.deletePromotion,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
      toast.success('Promotion deleted!');
    },
    onError: () => {
      toast.error('Failed to delete promotion');
    },
  });
}

// ============ ADMIN ANALYTICS HOOKS ============
export function useAdminAnalytics(params: { startDate: string; endDate: string; granularity?: 'day' | 'week' | 'month' }) {
  return useQuery({
    queryKey: ['admin', 'analytics', params],
    queryFn: () => adminApi.getAnalytics(params),
    enabled: !!params.startDate && !!params.endDate,
  });
}

// ============ ADMIN TAGS HOOKS ============
export function useAdminTags() {
  return useQuery({
    queryKey: ['admin', 'tags'],
    queryFn: () => adminApi.getTags(),
  });
}

export function useCreateTag() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.createTag,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'tags'] });
      toast.success('Tag created!');
    },
    onError: () => {
      toast.error('Failed to create tag');
    },
  });
}

export function useUpdateTag() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: { name: string; slug: string } }) =>
      adminApi.updateTag(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'tags'] });
      toast.success('Tag updated!');
    },
    onError: () => {
      toast.error('Failed to update tag');
    },
  });
}

export function useDeleteTag() {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: adminApi.deleteTag,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'tags'] });
      toast.success('Tag deleted!');
    },
    onError: () => {
      toast.error('Failed to delete tag');
    },
  });
}
