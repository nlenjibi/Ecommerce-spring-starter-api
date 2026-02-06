'use client';

import { useState, useMemo } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productsApi } from '@/lib/api';
import { Product } from '@/types';
import { Plus, Edit, Trash2, Search, X } from 'lucide-react';
import { Pagination } from '@/components/admin/Pagination';
import toast from 'react-hot-toast';
import { useDebounce } from '@/lib/hooks';

function ProductsPage() {
  const [page, setPage] = useState(0); // 0-based for API
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [searchInput, setSearchInput] = useState('');
  const debouncedSearch = useDebounce(searchInput, 300);
  const queryClient = useQueryClient();
  const size = 10;

  // Reset page when search changes
  const searchTerm = useMemo(() => {
    setPage(0);
    return debouncedSearch;
  }, [debouncedSearch]);

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin', 'products', { page, size, search: searchTerm }],
    queryFn: () => searchTerm 
      ? productsApi.search({ search: searchTerm, page, size })
      : productsApi.getAll({ page, size }),
  });

  // API response: { success, data: { content, page, totalPages, ... } }
  const apiData = data as any;
  const products = apiData?.data?.content || apiData?.products || apiData?.content || [];
  const totalPages = apiData?.data?.totalPages || apiData?.totalPages || 1;
  const totalElements = apiData?.data?.totalElements || 0;
  const currentPage = apiData?.data?.page ?? apiData?.page ?? 0;

  const deleteMutation = useMutation({
    mutationFn: (id: number) => productsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'products'] });
      toast.success('Product deleted successfully');
      setDeleteId(null);
    },
    onError: () => {
      toast.error('Failed to delete product');
    },
  });

  const handleDelete = (id: number) => {
    if (confirm('Are you sure you want to delete this product?')) {
      deleteMutation.mutate(id);
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Products</h1>
          <p className="text-gray-600">Manage your product inventory</p>
        </div>
        <Link
          href="/admin/products/new"
          className="inline-flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-5 h-5" />
          Add Product
        </Link>
      </div>

      {/* Search Bar */}
      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
          <input
            type="text"
            placeholder="Search products by name..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {searchInput && (
            <button
              onClick={() => setSearchInput('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            >
              <X className="w-5 h-5" />
            </button>
          )}
        </div>
        {searchTerm && (
          <p className="text-sm text-gray-500">
            {totalElements} result{totalElements !== 1 ? 's' : ''} for "{searchTerm}"
          </p>
        )}
      </div>

      {/* Products Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Product</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Category</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Price</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Stock</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Status</th>
              <th className="text-right px-6 py-3 text-sm font-medium text-gray-500">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {isLoading ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-gray-500">
                  Loading products...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-red-500">
                  Failed to load products
                </td>
              </tr>
            ) : products.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-gray-500">
                  No products found
                </td>
              </tr>
            ) : (
              products.map((product: Product) => (
                <tr key={product.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-gray-100 rounded-lg overflow-hidden relative">
                        {product.imageUrl ? (
                          <Image
                            src={product.imageUrl}
                            alt={product.name}
                            fill
                            className="object-cover"
                          />
                        ) : (
                          <div className="w-full h-full flex items-center justify-center text-gray-400">
                            📦
                          </div>
                        )}
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">{product.name}</p>
                        <p className="text-sm text-gray-500">SKU: {product.sku || 'N/A'}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-gray-600">
                    {product.category?.name || 'Uncategorized'}
                  </td>
                  <td className="px-6 py-4">
                    <div>
                      <p className="font-medium text-gray-900">
                        ${(product.effectivePrice ?? product.price ?? 0).toFixed(2)}
                      </p>
                      {product.discountPrice && product.discountPrice < (product.price ?? 0) && (
                        <p className="text-sm text-gray-500 line-through">
                          ${product.price?.toFixed(2)}
                        </p>
                      )}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    {(() => {
                      const stock = product.stockQuantity ?? product.stock ?? 0;
                      return (
                        <span
                          className={
                            stock > 10
                              ? 'text-green-600'
                              : stock > 0
                              ? 'text-yellow-600'
                              : 'text-red-600'
                          }
                        >
                          {stock}
                        </span>
                      );
                    })()}
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={
                        'px-2 py-1 rounded-full text-xs font-medium ' +
                        ((product.inStock ?? (product.stockQuantity ?? product.stock ?? 0) > 0)
                          ? 'bg-green-100 text-green-700'
                          : 'bg-gray-100 text-gray-700')
                      }
                    >
                      {(product.inStock ?? (product.stockQuantity ?? product.stock ?? 0) > 0) ? 'In Stock' : 'Out of Stock'}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center justify-end gap-2">
                      <Link
                        href={`/admin/products/${product.id}/edit`}
                        className="p-2 text-gray-600 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                      >
                        <Edit className="w-4 h-4" />
                      </Link>
                      <button
                        onClick={() => handleDelete(product.id)}
                        disabled={deleteMutation.isPending}
                        className="p-2 text-gray-600 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      <Pagination 
        page={currentPage} 
        totalPages={totalPages} 
        onPageChange={setPage}
        totalElements={totalElements}
        size={size}
      />
      </div>
    </div>
  );
}

export default ProductsPage;
