'use client';

import React, { useEffect, useState, useMemo } from 'react';
import Image from 'next/image';
import Link from 'next/link';
import {
  Heart,
  ShoppingCart,
  X,
  Package,
  TrendingDown,
  Share2,
  Search,
  Filter,
  DollarSign,
  Tag,
  Calendar,
  Bell,
  Grid,
  List,
  Download,
  Star,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  BarChart3,
  Folder,
  ChevronDown,
  Settings,
  ArrowUpDown,
  Plus,
} from 'lucide-react';
import { useWishlist } from '@/context/WishlistContext';
import { useCart } from '@/context/CartContext';
import { useAuth } from '@/context/AuthContext';
import { Button } from '@/components/ui/Button';
import { StockBadge } from '@/components/StockBadge';
import { Product } from '@/types';
import { getImageUrl } from '@/lib/utils';
import toast from 'react-hot-toast';
import { ComprehensiveWishlistManager } from '@/components/wishlist/ComprehensiveWishlistManager';

type ViewMode = 'grid' | 'list';
type FilterTab = 'all' | 'available' | 'price-drops' | 'purchased';

export default function WishlistPage() {
  const {
    wishlist,
    itemCount,
    summary,
    analytics,
    collections,
    isLoading,
    addToWishlist,
    removeFromWishlist,
    updateWishlistItem,
    isInWishlist,
    clearWishlist,
    moveToCart,
    moveMultipleToCart,
    markAsPurchased,
    markMultipleAsPurchased,
    getItemsWithPriceDrops,
    getItemsBelowTargetPrice,
    getPurchasedItems,
    getUnpurchasedItems,
    getAvailableItems,
    getItemsByCollection,
    getItemsByPriority,
    moveToCollection,
    loadSummary,
    loadAnalytics,
    optimizeWishlist,
    shareWishlist,
    loadCollections,
  } = useWishlist();

  const { addToCart } = useCart();
  const { user, isAuthenticated } = useAuth();

  // View & Filter State
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [filterTab, setFilterTab] = useState<FilterTab>('all');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPriority, setSelectedPriority] = useState<string>('all');
  const [selectedCollection, setSelectedCollection] = useState<string>('all');
  const [sortBy, setSortBy] = useState('date-added');
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 10000]);
  
  // Selection State
  const [selectedItems, setSelectedItems] = useState<number[]>([]);
  const [isSelectionMode, setIsSelectionMode] = useState(false);

  // Modal State
  const [isShareModalOpen, setIsShareModalOpen] = useState(false);
  const [isOptimizeModalOpen, setIsOptimizeModalOpen] = useState(false);
  const [isCollectionModalOpen, setIsCollectionModalOpen] = useState(false);
  const [showAnalytics, setShowAnalytics] = useState(false);

  // Load data on mount
  useEffect(() => {
    if (isAuthenticated) {
      // Call async loaders and ensure any rejection is caught to avoid unhandled promise errors
      loadSummary().catch((err) => console.warn('[Wishlist] loadSummary error:', err));
      loadAnalytics().catch((err) => console.warn('[Wishlist] loadAnalytics error:', err));
      loadCollections().catch((err) => console.warn('[Wishlist] loadCollections error:', err));
    }
  }, [isAuthenticated]);

  // Filter items based on active tab and filters
  const filteredItems = useMemo(() => {
    let items = [...wishlist];

    // Apply tab filter
    switch (filterTab) {
      case 'available':
        items = getAvailableItems();
        break;
      case 'price-drops':
        items = getItemsWithPriceDrops();
        break;
      case 'purchased':
        items = getPurchasedItems();
        break;
      default:
        items = wishlist;
    }

    // Apply search
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      items = items.filter(item =>
        item.product.name.toLowerCase().includes(query) ||
        item.notes?.toLowerCase().includes(query) ||
        item.tags?.some(tag => tag.toLowerCase().includes(query))
      );
    }

    // Apply priority filter
    if (selectedPriority !== 'all') {
      items = items.filter(item => item.priority === selectedPriority);
    }

    // Apply collection filter
    if (selectedCollection !== 'all') {
      items = items.filter(item => item.collectionName === selectedCollection);
    }

    // Apply price range
    items = items.filter(item => {
      const price = item.currentPrice;
      return price >= priceRange[0] && price <= priceRange[1];
    });

    // Apply sorting
    switch (sortBy) {
      case 'date-added':
        items.sort((a, b) => new Date(b.addedAt).getTime() - new Date(a.addedAt).getTime());
        break;
      case 'price-low':
        items.sort((a, b) => a.currentPrice - b.currentPrice);
        break;
      case 'price-high':
        items.sort((a, b) => b.currentPrice - a.currentPrice);
        break;
      case 'priority':
        const priorityOrder = { URGENT: 4, HIGH: 3, MEDIUM: 2, LOW: 1 };
        items.sort((a, b) => priorityOrder[b.priority] - priorityOrder[a.priority]);
        break;
      case 'savings':
        items.sort((a, b) => b.priceDifference - a.priceDifference);
        break;
    }

    return items;
  }, [wishlist, filterTab, searchQuery, selectedPriority, selectedCollection, priceRange, sortBy]);

  // Selection handlers
  const toggleItemSelection = (productId: number) => {
    setSelectedItems(prev =>
      prev.includes(productId)
        ? prev.filter(id => id !== productId)
        : [...prev, productId]
    );
  };

  const selectAll = () => {
    setSelectedItems(filteredItems.map(item => item.productId));
  };

  const clearSelection = () => {
    setSelectedItems([]);
    setIsSelectionMode(false);
  };

  // Bulk actions
  const handleBulkMoveToCart = async () => {
    try {
      await moveMultipleToCart(selectedItems);
      toast.success(`${selectedItems.length} items moved to cart`);
      clearSelection();
    } catch (error) {
      toast.error('Failed to move items to cart');
    }
  };

  const handleBulkMarkPurchased = async () => {
    try {
      await markMultipleAsPurchased(selectedItems);
      toast.success(`${selectedItems.length} items marked as purchased`);
      clearSelection();
    } catch (error) {
      toast.error('Failed to mark items as purchased');
    }
  };

  const handleBulkDelete = async () => {
    if (!confirm(`Remove ${selectedItems.length} items from wishlist?`)) return;
    
    try {
      for (const productId of selectedItems) {
        await removeFromWishlist(productId);
      }
      toast.success(`${selectedItems.length} items removed`);
      clearSelection();
    } catch (error) {
      toast.error('Failed to remove items');
    }
  };

  // Share wishlist
  const handleShare = async () => {
    try {
      const result = await shareWishlist({
        shareName: `${user?.firstName || 'My'}'s Wishlist`,
        description: 'Check out my wishlist!',
      });
      
      const shareUrl = `${window.location.origin}${result.shareUrl}`;
      await navigator.clipboard.writeText(shareUrl);
      toast.success('Wishlist link copied to clipboard!');
      setIsShareModalOpen(false);
    } catch (error) {
      console.error('Failed to share wishlist:', error);
      toast.error('Failed to share wishlist');
    }
  };

  // Optimize wishlist
  const handleOptimize = async (strategy: string, budget?: number) => {
    try {
      const optimized = await optimizeWishlist({
        maxBudget: budget,
        optimizationStrategy: strategy as any,
        includeOnlyInStock: true,
      });
      
      toast.success(`Optimized to ${optimized.length} items`);
      setIsOptimizeModalOpen(false);
    } catch (error) {
      toast.error('Failed to optimize wishlist');
    }
  };

  // Empty state
  if (!isLoading && wishlist.length === 0) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center max-w-md mx-auto px-4">
          <Heart className="w-24 h-24 text-gray-300 mx-auto mb-6" />
          <h2 className="text-3xl font-bold text-gray-900 mb-4">Your Wishlist is Empty</h2>
          <p className="text-gray-600 mb-8">
            Start adding items you love to your wishlist and never lose track of what you want!
          </p>
          <Link href="/shop">
            <Button size="lg" className="inline-flex items-center gap-2">
              <ShoppingCart className="w-5 h-5" />
              Start Shopping
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <ComprehensiveWishlistManager />
    </div>
  );
}

