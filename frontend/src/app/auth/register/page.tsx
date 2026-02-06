'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { Mail, Lock, User, Eye, EyeOff, AlertCircle, Check } from 'lucide-react';

export default function RegisterPage() {
  const router = useRouter();
  const { register, loading } = useAuth();
  
  const [formData, setFormData] = useState({
    username: '',
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const passwordRequirements = [
    { label: 'At least 8 characters', met: formData.password.length >= 8 },
    { label: 'Contains a number', met: /\d/.test(formData.password) },
    { label: 'Contains uppercase letter', met: /[A-Z]/.test(formData.password) },
    { label: 'Passwords match', met: formData.password === formData.confirmPassword && formData.confirmPassword !== '' },
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setFieldErrors({});

    // Basic client-side validation
    const newFieldErrors: Record<string, string> = {};
    if (!formData.username.trim()) newFieldErrors.username = 'Username is required';
    if (!formData.firstName.trim()) newFieldErrors.firstName = 'First name is required';
    if (!formData.lastName.trim()) newFieldErrors.lastName = 'Last name is required';
    if (!formData.email.trim()) newFieldErrors.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) newFieldErrors.email = 'Enter a valid email';
    if (!formData.password) newFieldErrors.password = 'Password is required';
    if (formData.password !== formData.confirmPassword) newFieldErrors.confirmPassword = 'Passwords do not match';
    if (!agreedToTerms) newFieldErrors.agreedToTerms = 'You must agree to the terms and conditions';

    if (Object.keys(newFieldErrors).length > 0) {
      setFieldErrors(newFieldErrors);
      setError('Please fix the errors below');
      return;
    }

    setIsSubmitting(true);
    try {
      await register({
        username: formData.username || undefined,
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        phoneNumber: formData.phoneNumber || undefined,
        password: formData.password,
      });
      router.push('/');
    } catch (err: any) {
      // Extract server-side validation errors
      const safeErr = err || {};
      const serverMessage = safeErr?.response?.data?.message || safeErr?.message || 'Registration failed. Please try again.';

      // Accept multiple possible shapes for validation errors
      const serverErrors = safeErr?.response?.data?.errors || safeErr?.response?.data?.fieldErrors || safeErr?.errors || null;

      if (serverErrors && typeof serverErrors === 'object') {
        // field => message or array
        const map: Record<string, string> = {};
        if (Array.isArray(serverErrors)) {
          serverErrors.forEach((it: any) => {
            if (it.field) map[it.field] = it.message || String(it);
          });
        } else {
          Object.entries(serverErrors).forEach(([k, v]) => {
            map[k] = typeof v === 'string' ? v : (v as any)?.message || JSON.stringify(v);
          });
        }
        setFieldErrors(map);
        setError(serverMessage);
      } else {
        setError(serverMessage);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        {/* Header */}
        <div className="text-center mb-8">
          <Link href="/" className="text-3xl font-bold text-blue-600">
            ShopHub
          </Link>
          <h2 className="mt-6 text-2xl font-bold text-gray-900">
            Create your account
          </h2>
          <p className="mt-2 text-gray-600">
            Join us and start shopping today
          </p>
        </div>

        {/* Form */}
        <div className="bg-white rounded-xl shadow-sm p-8 border border-gray-100">
          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3 text-red-600">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <p className="text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Username
                </label>
                <input
                  type="text"
                  required
                  aria-invalid={fieldErrors.username ? 'true' : 'false'}
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 ${fieldErrors.username ? 'border-red-400 focus:ring-red-300' : 'border-gray-300 focus:ring-blue-500'}`}
                  placeholder="your-username"
                />
                {fieldErrors.username && <p className="mt-1 text-sm text-red-600">{fieldErrors.username}</p>} 
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Phone Number
                </label>
                <input
                  type="tel"
                  value={formData.phoneNumber}
                  onChange={(e) => setFormData({ ...formData, phoneNumber: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="+1234567890"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="email"
                  required
                  aria-invalid={fieldErrors.email ? 'true' : 'false'}
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className={`w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 ${fieldErrors.email ? 'border-red-400 focus:ring-red-300' : 'border-gray-300 focus:ring-blue-500'}`}
                  placeholder="you@example.com"
                />
              </div>
              {fieldErrors.email && <p className="mt-1 text-sm text-red-600">{fieldErrors.email}</p>} 
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  aria-invalid={fieldErrors.password ? 'true' : 'false'}
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className={`w-full pl-10 pr-12 py-3 border rounded-lg focus:outline-none focus:ring-2 ${fieldErrors.password ? 'border-red-400 focus:ring-red-300' : 'border-gray-300 focus:ring-blue-500'}`}
                  placeholder="••••••••"
                />
                {fieldErrors.password && <p className="mt-1 text-sm text-red-600">{fieldErrors.password}</p>} 
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Confirm Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  aria-invalid={fieldErrors.confirmPassword ? 'true' : 'false'}
                  value={formData.confirmPassword}
                  onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                  className={`w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 ${fieldErrors.confirmPassword ? 'border-red-400 focus:ring-red-300' : 'border-gray-300 focus:ring-blue-500'}`}
                  placeholder="••••••••"
                />
                {fieldErrors.confirmPassword && <p className="mt-1 text-sm text-red-600">{fieldErrors.confirmPassword}</p>} 
              </div>
            </div>

            {/* Password Requirements */}
            <div className="space-y-2">
              {passwordRequirements.map((req, index) => (
                <div key={index} className="flex items-center gap-2 text-sm">
                  <div className={`w-4 h-4 rounded-full flex items-center justify-center ${
                    req.met ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-400'
                  }`}>
                    <Check className="w-3 h-3" />
                  </div>
                  <span className={req.met ? 'text-green-600' : 'text-gray-500'}>
                    {req.label}
                  </span>
                </div>
              ))}
            </div>

            <label className="flex items-start gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={agreedToTerms}
                onChange={(e) => setAgreedToTerms(e.target.checked)}
                className={`mt-1 w-4 h-4 rounded ${fieldErrors.agreedToTerms ? 'border-red-400' : 'border-gray-300'} text-blue-600 focus:ring-blue-500`}
              />
              <span className="text-sm text-gray-600">
                I agree to the{' '}
                <Link href="/terms" className="text-blue-600 hover:underline">
                  Terms of Service
                </Link>{' '}
                and{' '}
                <Link href="/privacy" className="text-blue-600 hover:underline">
                  Privacy Policy
                </Link>
              </span>
            </label>
            {fieldErrors.agreedToTerms && <p className="mt-1 text-sm text-red-600">{fieldErrors.agreedToTerms}</p>} 

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3 px-4 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isSubmitting ? 'Creating account...' : 'Create account'}
            </button>
          </form>
        </div>

        {/* Footer */}
        <p className="mt-8 text-center text-gray-600">
          Already have an account?{' '}
          <Link href="/auth/login" className="text-blue-600 hover:text-blue-700 font-medium">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
