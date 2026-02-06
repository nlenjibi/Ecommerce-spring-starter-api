'use client';

import { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, MapPin, Check } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { useAddress } from '@/context/AddressContext';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { UserAddress } from '@/types';

export default function AddressesPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const { 
    addresses, 
    loading, 
    addAddress, 
    updateAddress, 
    deleteAddress, 
    setDefaultAddress 
  } = useAddress();

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formData, setFormData] = useState({
    label: 'Home',
    recipientName: '',
    phone: '',
    street: '',
    city: '',
    region: '',
    country: 'Ghana',
    postalCode: '',
  });

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSaveAddress = async () => {
    try {
      if (editingId) {
        await updateAddress(editingId, formData);
      } else {
        await addAddress(formData as any);
      }
      
      resetForm();
    } catch (err) {
      console.error('Error saving address:', err);
    }
  };

  const handleEdit = (address: UserAddress) => {
    setEditingId(address.id);
    setFormData({
      label: address.label || 'Home',
      recipientName: address.recipientName,
      phone: address.phone,
      street: address.street,
      city: address.city,
      region: address.region,
      country: address.country || 'Ghana',
      postalCode: address.postalCode || '',
    });
    setShowForm(true);
  };

  const handleDeleteAddress = async (id: number) => {
    try {
      await deleteAddress(id);
    } catch (err) {
      console.error('Error deleting address:', err);
    }
  };

  const resetForm = () => {
    setFormData({
      label: 'Home',
      recipientName: '',
      phone: '',
      street: '',
      city: '',
      region: '',
      country: 'Ghana',
      postalCode: '',
    });
    setEditingId(null);
    setShowForm(false);
  };

  if (authLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-4xl mx-auto">
        <div className="mb-8 flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">My Addresses</h1>
            <p className="text-gray-600 mt-2">Manage your delivery addresses</p>
          </div>
          {!showForm && (
            <Button
              onClick={() => setShowForm(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2"
            >
              <Plus className="w-5 h-5" />
              Add New Address
            </Button>
          )}
        </div>

        {/* Add/Edit Form */}
        {showForm && (
          <div className="mb-8 p-6 bg-white rounded-lg shadow-sm">
            <h2 className="text-xl font-bold text-gray-900 mb-4">
              {editingId ? 'Edit Address' : 'Add New Address'}
            </h2>

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Label *
                </label>
                <select
                  name="label"
                  value={formData.label}
                  onChange={handleInputChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                >
                  <option value="Home">Home</option>
                  <option value="Work">Work</option>
                  <option value="Other">Other</option>
                </select>
              </div>

              <Input
                label="Recipient Name *"
                name="recipientName"
                value={formData.recipientName}
                onChange={handleInputChange}
                placeholder="Full name"
              />

              <Input
                label="Phone Number *"
                name="phone"
                type="tel"
                value={formData.phone}
                onChange={handleInputChange}
                placeholder="+233 XXX XXX XXX"
              />

              <Input
                label="Street Address *"
                name="street"
                value={formData.street}
                onChange={handleInputChange}
                placeholder="House number, street name"
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="City *"
                  name="city"
                  value={formData.city}
                  onChange={handleInputChange}
                  placeholder="City"
                />
                <Input
                  label="Region *"
                  name="region"
                  value={formData.region}
                  onChange={handleInputChange}
                  placeholder="Region"
                />

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Country *
                  </label>
                  <Input
                    name="country"
                    value={formData.country}
                    onChange={handleInputChange}
                    placeholder="Ghana"
                  />
                </div>
              </div>

              <div className="flex gap-3 pt-4">
                <Button
                  onClick={handleSaveAddress}
                  className="flex-1 bg-blue-600 hover:bg-blue-700 text-white"
                >
                  {editingId ? 'Update Address' : 'Save Address'}
                </Button>
                <Button
                  onClick={resetForm}
                  className="flex-1 bg-gray-200 hover:bg-gray-300 text-gray-900"
                >
                  Cancel
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Loading State */}
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
          </div>
        ) : addresses.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow-sm">
            <MapPin className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-600 text-lg mb-4">No addresses saved yet</p>
            <Button
              onClick={() => setShowForm(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              Add Your First Address
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {addresses.map((address) => (
              <div
                key={address.id}
                className={`p-4 rounded-lg border-2 ${
                  address.isDefault
                    ? 'bg-blue-50 border-blue-300'
                    : 'bg-white border-gray-200'
                }`}
              >
                <div className="flex justify-between items-start mb-3">
                  <div>
                    <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800">
                      {address.label}
                    </span>
                    {address.isDefault && (
                      <span className="ml-2 inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                        <Check className="w-3 h-3 mr-1" />
                        Default
                      </span>
                    )}
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleEdit(address)}
                      className="text-blue-600 hover:text-blue-700"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDeleteAddress(address.id)}
                      className="text-red-600 hover:text-red-700"
                      disabled={address.isDefault}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                <div className="space-y-1 text-sm text-gray-600 mb-4">
                  <p className="font-semibold text-gray-900">{address.recipientName}</p>
                  <p>{address.phone}</p>
                  <p>{address.street}</p>
                  <p>
                    {address.city}, {address.region}
                  </p>
                  {address.postalCode && <p>{address.postalCode}</p>}
                  <p>{address.country}</p>
                </div>

                {!address.isDefault && (
                  <button
                    onClick={() => setDefaultAddress(address.id)}
                    className="text-sm text-blue-600 hover:text-blue-700 font-medium"
                  >
                    Set as default
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}