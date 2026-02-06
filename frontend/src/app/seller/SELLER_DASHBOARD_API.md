# Seller Dashboard — API & Data Model Suggestions

This document describes suggested backend endpoints, request/response shapes, and database relationships for the Seller (Vendor) Dashboard. All endpoints must be server-filtered by `sellerId` (extracted from the authenticated session) — never rely on client-side filtering.

Authentication & RBAC

- Use JWT / session cookie with role claim. Validate on every request.
- Seller-only endpoints must verify role === `seller` and that the `sellerId` in the token matches resource ownership.

Database relationship assumptions

- sellers(id)
- products(id, seller_id -> sellers.id, name, sku, price, stock, images[], active, created_at)
- orders(id, buyer_id, total, payment_status, created_at)
- order_items(id, order_id -> orders.id, product_id -> products.id, seller_id -> sellers.id, quantity, price)
- payouts(id, seller_id -> sellers.id, amount, status, requested_at, processed_at)
- reviews(id, product_id -> products.id, rating, body, user_id, created_at)

Key API endpoints (seller-scoped)

- GET /api/seller/products
  - Query params: page, limit, search, status
  - Response: { items: Product[], total, page, limit }

- POST /api/seller/products
  - Body: { name, sku, price, stock, images[], active }
  - Response: created Product

- PUT /api/seller/products/:id
  - Body: partial product fields (seller may only update own product)

- POST /api/seller/products/:id/images
  - Body: multipart file upload (verify seller owns product)

- GET /api/seller/orders
  - Query params: status, page, limit
  - Note: return orders that contain items where seller_id === session.sellerId. For each order include only the items that belong to the seller.

- GET /api/seller/orders/:orderId
  - Return order details filtered to seller's items only.

- POST /api/seller/orders/:orderId/ship
  - Body: { trackingNumber }
  - Action: set delivery status for seller items to SHIPPED and attach trackingNumber. Validate order contains seller items.

- GET /api/seller/analytics/sales?range=7d|30d|90d
  - Response: { series: [{ date, revenue }], topProducts: [{ id, name, soldCount }] }

- GET /api/seller/payouts
  - Response: { totalEarnings, availableBalance, pendingPayouts, history: Payout[] }

- POST /api/seller/payouts/request
  - Body: { amount, payoutMethod } — creates payout request, validate available balance

- GET /api/seller/reviews
  - Return reviews only for products owned by seller

Security notes

- Always verify ownership server-side (product.seller_id === session.sellerId) before mutating.
- Limit fields returned to seller to avoid leaking platform data (e.g., do not return buyer's email unless required).
- Rate-limit and log seller actions (especially payouts and inventory updates).

Export/Reporting

- Provide an export endpoint (CSV / PDF) which streams results; ensure filters are seller-scoped.

Integration tips for frontend

- Use server components (or secure server-side fetches) to get seller-scoped data initially, and client components + mutations for interactive actions.
- Use optimistic UI updates carefully; reflect server validation errors.
