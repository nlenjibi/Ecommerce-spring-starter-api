import { CreateOrderRequest, CreateOrderResponse } from "../types/order";

export async function createOrder(order: CreateOrderRequest): Promise<CreateOrderResponse> {
  const response = await fetch("/v1/orders", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(order),
  });
  if (!response.ok) {
    // Optionally, you can throw or handle error here
    throw new Error("Failed to create order");
  }
  return response.json();
}
