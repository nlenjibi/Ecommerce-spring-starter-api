// Order request and response types for order creation

export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface CreateOrderRequest {
  items: OrderItemRequest[];
  shippingMethod: string;
  paymentMethod: string;
  customerEmail: string;
  customerName: string;
  couponCode?: string;
  taxRate?: number;
  customerNotes?: string;
  couponDiscount?: number;
}

export interface OrderItemResponse {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  productImageUrl: string;
  totalPrice: number;
}

export interface CreateOrderResponse {
  success: boolean;
  message: string;
  data: {
    id: number;
    orderNumber: string;
    userId: number;
    status: string;
    paymentStatus: string;
    paymentMethod: string;
    shippingMethod: string;
    subtotal: number;
    taxAmount: number;
    shippingCost: number;
    discountAmount: number;
    totalAmount: number;
    shippingAddress: string;
    trackingNumber: string;
    carrier: string;
    orderDate: string;
    shippedAt: string;
    deliveredAt: string;
    estimatedDeliveryDate: string;
    items: OrderItemResponse[];
    itemCount: number;
    customerNotes: string;
  };
  errors?: Record<string, any>;
  timestamp: string;
  path: string;
  statusCode: number;
}
