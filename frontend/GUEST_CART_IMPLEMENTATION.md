# Guest Cart Implementation

This document describes the guest cart functionality implemented for the frontend, allowing users to add products to cart without logging in.

## Architecture Overview

```
localStorage
   └── cartId (UUID)

Frontend
   ├── getOrCreateCart()
   ├── addToCart(productId)
   └── updateCartIcon(itemCount)

Backend
   └── Owns cart data
```

## Key Features

- ✅ **Guest Users**: No login required to add products to cart
- ✅ **Persistence**: Cart persists across page refreshes using localStorage
- ✅ **Real-time Updates**: Cart icon badge updates immediately when items are added
- ✅ **Backend as Source of Truth**: All cart data is stored and managed by the backend
- ✅ **Automatic Cart Creation**: Frontend creates cart automatically if none exists

## Backend Endpoints Used

- `POST /v1/carts/items` → Add item to cart  
- `GET /v1/carts` → Get cart data (for authenticated users, adapted for guests)
- `POST /v1/carts/from-tracking` → Create cart from tracked products

**Note**: The backend doesn't have a `POST /v1/carts` endpoint for creating carts. 
For guest users, we generate a cartId locally and use it with existing endpoints.

## Frontend Implementation

### 1. Cart Utility Functions (`src/lib/cartUtils.ts`)

Core functions for guest cart operations:

```typescript
// Get or create cart, stores cartId in localStorage
export async function getOrCreateCart(): Promise<string>

// Add item to cart
export async function addToCart(productId: number, quantity: number): Promise<CartResponse>

// Get cart data
export async function getCart(cartId?: string): Promise<CartResponse | null>

// Update cart icon badge
export function updateCartIcon(count: number): void

// Load cart on startup
export async function loadCartOnStartup(): void
```

### 2. Cart Context (`src/context/CartContext.tsx`)

Updated to use guest cart architecture:

- Uses `getOrCreateCart()` and `addToCart()` from cartUtils
- Stores cartId in localStorage automatically
- Loads existing cart on component mount
- Updates cart state based on backend responses

### 3. Cart Icon (`src/components/Header.tsx`)

Cart icon with dynamic badge:

```jsx
<span id="cart-count" className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center font-semibold">
  {itemCount}
</span>
```

### 4. Cart Initialization (`src/components/CartInitializer.tsx`)

Component that loads cart on app startup:

```typescript
export function CartInitializer() {
  useEffect(() => {
    loadCartOnStartup();
  }, []);
  return null;
}
```

## Usage Examples

### Adding a Product to Cart

```typescript
import { useCart } from '@/context/CartContext';

function ProductComponent({ product }) {
  const { addToCart } = useCart();
  
  const handleAddToCart = async () => {
    await addToCart(product, 1); // Add 1 quantity
  };
  
  return <button onClick={handleAddToCart}>Add to Cart</button>;
}
```

### Manual Cart Operations

```typescript
import { getOrCreateCart, addToCart, getCart, updateCartIcon } from '@/lib/cartUtils';

// Add item to cart manually
const cartResponse = await addToCart(123, 2); // Product ID 123, quantity 2
updateCartIcon(cartResponse.itemCount);

// Get current cart data
const cart = await getCart();
console.log('Cart items:', cart?.items);
```

## Data Flow

1. **User clicks "Add to Cart"**
2. Frontend calls `getOrCreateCart()` to ensure cartId exists
3. If no cartId, generates a local cartId: `guest_cart_[timestamp]_[random]`
4. Stores cartId in localStorage
5. Adds item via `POST /v1/carts/items` (doesn't use cartId in URL)
6. Backend returns updated cart data with itemCount
7. Frontend updates cart state and icon badge

**Important**: Since the backend doesn't support cartId-based operations for guests,
the cartId is primarily used for frontend persistence and tracking.

## Persistence Strategy

- **cartId**: Stored in localStorage as string
- **Cart Data**: Only stored in backend database
- **Page Refresh**: Frontend loads cart using stored cartId
- **Invalid Cart**: If cartId is invalid, localStorage is cleared

## Error Handling

- Network errors show user-friendly toast messages
- Invalid cartId is automatically cleared from localStorage
- Backend validation errors are displayed to users
- Fallback UI state if cart operations fail

## Testing

A test page is available at `/test-cart` to verify:

- ✅ Add to cart functionality
- ✅ Cart persistence across refreshes
- ✅ Cart icon updates
- ✅ Clear cart operations
- ✅ localStorage management

## Backend Contract

The backend CartDto must include:

```typescript
interface CartDto {
  id: string;           // Cart UUID
  itemCount: number;   // Total item count (required for badge)
  items?: CartItem[];   // Array of cart items
  totalPrice?: number;  // Optional total price
  status?: string;      // Optional cart status
}
```

## Security Considerations

- cartId is a UUID, not sequential
- No sensitive data stored in localStorage
- Backend validates all cart operations
- Cart operations are rate-limited (backend responsibility)

## Browser Compatibility

- Uses modern localStorage API
- Requires ES6+ async/await support
- Compatible with all modern browsers
- Graceful fallback if localStorage unavailable

## Future Enhancements

- Implement `removeFromCart()` with cartId-based endpoint
- Implement `updateQuantity()` with cartId-based endpoint
- Add cart merge functionality for guest → authenticated user
- Implement cart expiration for abandoned carts
- Add cart analytics and tracking
