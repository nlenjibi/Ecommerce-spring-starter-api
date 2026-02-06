'use client';

import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '@/services/api';
import { Search, Shield, ShieldOff, Mail, PlusCircle, Edit, Trash2, X, User as UserIcon, Phone } from 'lucide-react';
import { Pagination } from '@/components/admin/Pagination';
import { useDebounce } from '@/lib/hooks';
import toast from 'react-hot-toast';

// Define the User type based on the API response
interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address?: string;
  role: 'USER' | 'ADMIN' | 'SELLER';
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
  orderCount?: number;
}

// TODO: Create a separate modal component for this
function UserFormModal({ user, onClose, onSave }: { user: Partial<User> | null, onClose: () => void, onSave: (data: any) => void }) {
    const [formData, setFormData] = useState({
        firstName: user?.firstName || '',
        lastName: user?.lastName || '',
        email: user?.email || '',
        username: user?.username || '',
        phoneNumber: user?.phoneNumber || '',
        password: '',
        role: user?.role || 'USER',
    });

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        const dataToSave = { ...formData };
        if (!user?.id && !formData.password) {
            toast.error('Password is required for new users.');
            return;
        }
        if (!formData.password) {
            delete (dataToSave as any).password;
        }
        onSave(dataToSave);
    };

    return (
        <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center">
            <div className="bg-white rounded-xl shadow-2xl p-8 w-full max-w-md">
                <h2 className="text-2xl font-bold mb-6">{user?.id ? 'Edit User' : 'Create User'}</h2>
                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <input name="firstName" value={formData.firstName} onChange={handleChange} placeholder="First Name" required className="p-2 border rounded-lg" />
                        <input name="lastName" value={formData.lastName} onChange={handleChange} placeholder="Last Name" required className="p-2 border rounded-lg" />
                    </div>
                    <input name="email" type="email" value={formData.email} onChange={handleChange} placeholder="Email" required className="w-full p-2 border rounded-lg" />
                    <input name="username" value={formData.username} onChange={handleChange} placeholder="Username" required className="w-full p-2 border rounded-lg" />
                    <input name="phoneNumber" value={formData.phoneNumber} onChange={handleChange} placeholder="Phone Number" className="w-full p-2 border rounded-lg" />
                    {!user?.id && (
                        <input name="password" type="password" value={formData.password} onChange={handleChange} placeholder="Password" required className="w-full p-2 border rounded-lg" />
                    )}
                    <select name="role" value={formData.role} onChange={handleChange} className="w-full p-2 border rounded-lg">
                        <option value="USER">User</option>
                        <option value="ADMIN">Admin</option>
                        <option value="SELLER">Seller</option>
                    </select>
                    <div className="flex justify-end gap-4 pt-4">
                        <button type="button" onClick={onClose} className="px-4 py-2 rounded-lg">Cancel</button>
                        <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">Save</button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default function UsersPage() {
  const [page, setPage] = useState(0); // 0-based for API
  const [searchInput, setSearchInput] = useState('');
  const debouncedSearch = useDebounce(searchInput, 300);
  const [roleFilter, setRoleFilter] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [deletingUser, setDeletingUser] = useState<User | null>(null);
  const queryClient = useQueryClient();
  const size = 10;

  // Reset page when search changes
  const searchTerm = useMemo(() => {
    setPage(0);
    return debouncedSearch;
  }, [debouncedSearch]);

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'users', { page, size, search: searchTerm, role: roleFilter }],
    queryFn: () => usersApi.getAll({ page, limit: size, search: searchTerm, role: roleFilter }),
  });

  // Handle API response structure
  const apiData = data as any;
  const users = apiData?.data?.content || [];
  const totalPages = apiData?.data?.totalPages || 1;
  const totalElements = apiData?.data?.totalElements || 0;
  const currentPage = apiData?.data?.page ?? 0;

  const createUserMutation = useMutation({
    mutationFn: (userData: any) => usersApi.create(userData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User created successfully');
      setIsModalOpen(false);
    },
    onError: (error: any) => toast.error(error.message || 'Failed to create user'),
  });

  const updateUserMutation = useMutation({
    mutationFn: ({ id, ...userData }: { id: number } & any) => usersApi.update(id, userData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User updated successfully');
      setIsModalOpen(false);
    },
    onError: (error: any) => toast.error(error.message || 'Failed to update user'),
  });

  const deleteUserMutation = useMutation({
    mutationFn: (userId: number) => usersApi.delete(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User deleted successfully');
      setDeletingUser(null);
    },
    onError: (error: any) => toast.error(error.message || 'Failed to delete user'),
  });

  const handleSaveUser = (userData: any) => {
    if (editingUser?.id) {
      updateUserMutation.mutate({ id: editingUser.id, ...userData });
    } else {
      createUserMutation.mutate(userData);
    }
  };

  const openCreateModal = () => {
    setEditingUser(null);
    setIsModalOpen(true);
  };

  const openEditModal = (user: User) => {
    setEditingUser(user);
    setIsModalOpen(true);
  };

  const handleDeleteUser = (user: User) => {
    if (window.confirm(`Are you sure you want to delete ${user.firstName} ${user.lastName}?`)) {
      deleteUserMutation.mutate(user.id);
    }
  };


  const updateRoleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: number; role: 'ADMIN' | 'USER' | 'SELLER' }) =>
      usersApi.updateRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User role updated');
    },
    onError: () => toast.error('Failed to update user role'),
  });

  const toggleStatusMutation = useMutation({
    mutationFn: ({ userId, isActive }: { userId: number; isActive: boolean }) =>
      usersApi.update(userId, { isActive }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
      toast.success('User status updated');
    },
    onError: () => toast.error('Failed to update user status'),
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
            <h1 className="text-2xl font-bold text-gray-900">Users</h1>
            <p className="text-gray-600">Manage user accounts and permissions</p>
        </div>
        <button onClick={openCreateModal} className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
            <PlusCircle className="w-5 h-5" />
            <span>Create User</span>
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
        <div className="flex flex-col sm:flex-row gap-4">
          {/* Search */}
          <div className="flex-1">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search by name, email, or username..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-full pl-10 pr-10 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
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
          </div>
          {/* Role Filter */}
          <select
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              setPage(0);
            }}
            className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white min-w-[140px]"
          >
            <option value="">All Roles</option>
            <option value="USER">User</option>
            <option value="ADMIN">Admin</option>
            <option value="SELLER">Seller</option>
          </select>
        </div>
        {searchTerm && (
          <p className="text-sm text-gray-500 mt-3">
            Found {totalElements} user{totalElements !== 1 ? 's' : ''} matching "{searchTerm}"
          </p>
        )}
      </div>

      {/* Users Table */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">User</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Contact</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Role</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Status</th>
                <th className="text-left px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Joined</th>
                <th className="text-right px-6 py-4 text-xs font-semibold text-gray-600 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="px-6 py-4"><div className="flex items-center gap-3"><div className="w-10 h-10 rounded-full bg-gray-200"></div><div className="space-y-2"><div className="h-4 w-32 bg-gray-200 rounded"></div><div className="h-3 w-24 bg-gray-100 rounded"></div></div></div></td>
                    <td className="px-6 py-4"><div className="space-y-2"><div className="h-4 w-40 bg-gray-200 rounded"></div><div className="h-3 w-28 bg-gray-100 rounded"></div></div></td>
                    <td className="px-6 py-4"><div className="h-6 w-16 bg-gray-200 rounded-full"></div></td>
                    <td className="px-6 py-4"><div className="h-6 w-16 bg-gray-200 rounded-full"></div></td>
                    <td className="px-6 py-4"><div className="h-4 w-24 bg-gray-200 rounded"></div></td>
                    <td className="px-6 py-4"><div className="h-8 w-24 bg-gray-200 rounded ml-auto"></div></td>
                  </tr>
                ))
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-16 text-center">
                    <UserIcon className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-500 font-medium">No users found</p>
                    <p className="text-gray-400 text-sm mt-1">
                      {searchTerm ? `No results for "${searchTerm}"` : 'Get started by creating a new user'}
                    </p>
                  </td>
                </tr>
              ) : (
                users.map((user: User) => (
                  <tr key={user.id} className="hover:bg-gray-50/50 transition-colors">
                    {/* User Info */}
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-semibold ${
                          user.role === 'ADMIN' 
                            ? 'bg-purple-100 text-purple-700' 
                            : user.role === 'SELLER'
                            ? 'bg-amber-100 text-amber-700'
                            : 'bg-blue-100 text-blue-700'
                        }`}>
                          {user.firstName?.charAt(0)?.toUpperCase()}{user.lastName?.charAt(0)?.toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">{user.firstName} {user.lastName}</p>
                          <p className="text-sm text-gray-500">@{user.username}</p>
                        </div>
                      </div>
                    </td>
                    {/* Contact */}
                    <td className="px-6 py-4">
                      <div className="space-y-1">
                        <p className="text-gray-900 text-sm">{user.email}</p>
                        {user.phoneNumber && (
                          <p className="text-gray-500 text-sm flex items-center gap-1">
                            <Phone className="w-3 h-3" />
                            {user.phoneNumber}
                          </p>
                        )}
                      </div>
                    </td>
                    {/* Role */}
                    <td className="px-6 py-4">
                      <select
                        value={user.role}
                        onChange={(e) => updateRoleMutation.mutate({ userId: user.id, role: e.target.value as 'ADMIN' | 'USER' | 'SELLER' })}
                        className={`px-3 py-1.5 rounded-full text-xs font-semibold border-0 cursor-pointer appearance-none ${
                          user.role === 'ADMIN' 
                            ? 'bg-purple-100 text-purple-700' 
                            : user.role === 'SELLER'
                            ? 'bg-amber-100 text-amber-700'
                            : 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        <option value="USER">User</option>
                        <option value="ADMIN">Admin</option>
                        <option value="SELLER">Seller</option>
                      </select>
                    </td>
                    {/* Status */}
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold ${
                        user.isActive 
                          ? 'bg-green-100 text-green-700' 
                          : 'bg-red-100 text-red-700'
                      }`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${user.isActive ? 'bg-green-500' : 'bg-red-500'}`}></span>
                        {user.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    {/* Joined */}
                    <td className="px-6 py-4">
                      <p className="text-gray-900 text-sm">
                        {new Date(user.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                      </p>
                    </td>
                    {/* Actions */}
                    <td className="px-6 py-4">
                      <div className="flex justify-end gap-1">
                        <button 
                          onClick={() => openEditModal(user)} 
                          className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors" 
                          title="Edit user"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => toggleStatusMutation.mutate({ userId: user.id, isActive: !user.isActive })}
                          className={`p-2 rounded-lg transition-colors ${
                            user.isActive 
                              ? 'text-gray-500 hover:text-orange-600 hover:bg-orange-50' 
                              : 'text-gray-500 hover:text-green-600 hover:bg-green-50'
                          }`}
                          title={user.isActive ? 'Deactivate user' : 'Activate user'}
                        >
                          {user.isActive ? <ShieldOff className="w-4 h-4" /> : <Shield className="w-4 h-4" />}
                        </button>
                        <a
                          href={`mailto:${user.email}`}
                          className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          title="Send email"
                        >
                          <Mail className="w-4 h-4" />
                        </a>
                        <button 
                          onClick={() => handleDeleteUser(user)} 
                          className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors" 
                          title="Delete user"
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
      {isModalOpen && (
        <UserFormModal
            user={editingUser}
            onClose={() => setIsModalOpen(false)}
            onSave={handleSaveUser}
        />
      )}
    </div>
  );
}
