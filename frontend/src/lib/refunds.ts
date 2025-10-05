import { api } from './api';

export interface RefundRequestPayload {
  amount?: number; // optional; if lines are provided, backend derives amount from lines + shipping
  reason: string;
  refundShipping?: boolean;
  lines?: { orderItemId: number; quantity: number }[];
}

/** POST /api/admin/orders/{id}/refund */
export async function adminRefundOrder(orderId: number, payload: RefundRequestPayload): Promise<void> {
  await api<void>(`/admin/orders/${orderId}/refund`, {
    method: 'POST',
    body:   JSON.stringify(payload),
  });
}
