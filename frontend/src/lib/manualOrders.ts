/* Admin manual/in-person checkout helpers
 *  Posts to: POST /api/admin/manual-orders/checkout
 *  Payload names match ManualCheckoutRequestDto exactly */

import { api } from './api';
import type { ShippingAddress } from '../types';

/*  enums  */
export type OfflinePaymentMethod = 'CASH' | 'ETRANSFER' | 'TERMINAL' | 'OTHER';
export type ShippingMethod = 'REGULAR' | 'EXPRESS';

/* DTOs (aligned with backend OrderDto) */
export interface OrderItemDto {
  id: number;
  product: {
    id: number;
    sku?: string;
    name: string;
    brand?: string | null;
    price?: number;
    categoryId?: number | null;
    categoryName?: string | null;
    images?: { url: string; isPrimary: boolean; altText?: string | null }[];
  };
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface OrderDto {
  id: number;
  userId: number;
  status: 'PENDING_PAYMENT' | 'PAID' | 'FULFILLED' | 'REFUNDED' | 'CANCELLED';
  subtotalAmount: number;
  taxAmount: number;
  shippingAmount: number;
  discountAmount: number;
  totalAmount: number;
  shippingMethod: ShippingMethod;
  stripePaymentIntentId?: string | null;
  offlinePaymentMethod?: OfflinePaymentMethod | null;
  couponCode?: string | null;
  origin?: 'ONLINE' | 'IN_PERSON';
  shippingAddress?: ShippingAddress | null;
  createdAt?: string;
  updatedAt?: string;
  items: OrderItemDto[];
}

/* Admin manual checkout payload (backend names)  */
export interface ManualCheckoutPayload {
  clientUserId?: number | null;             // optional - if provided, wins
  clientFullName?: string;                  // required if no clientUserId
  clientEmail?: string;                     // required if no clientUserId
  clientPhone?: string;                     // required if no clientUserId
  shippingAddress?: ShippingAddress | null; // optional; we usually send contact here
  shippingMethod?: ShippingMethod;          // default REGULAR on server
  shippingFee?: number | null;              // optional override
  paymentMethod: OfflinePaymentMethod;      // CASH / ETRANSFER / TERMINAL / OTHER
  couponCode?: string | null;               // optional
}

/*  call  */
export async function manualCheckout(payload: ManualCheckoutPayload): Promise<OrderDto> {
  // The backend expects exact property names; api() auto-prefixes '/api'
  const body = {
    clientUserId   : payload.clientUserId ?? null,
    clientFullName : payload.clientFullName ?? null,
    clientEmail    : payload.clientEmail ?? null,
    clientPhone    : payload.clientPhone ?? null,
    shippingAddress: payload.shippingAddress ?? null,
    shippingMethod : payload.shippingMethod ?? 'REGULAR',
    shippingFee    : payload.shippingFee ?? null,
    paymentMethod  : payload.paymentMethod,
    couponCode     : payload.couponCode ?? null,
  };

  const res = await api<OrderDto>('/admin/manual-orders/checkout', {
    method: 'POST',
    body  : JSON.stringify(body),
  });
  if (!res) throw new Error('Empty response from server.');
  return res;
}

export interface RefundLineSummaryDto {
  orderItemId: number;
  quantity: number;
}

export interface RefundEventDto {
  id: number;
  provider: 'STRIPE' | 'OFFLINE' | string;
  providerTxnId?: string | null;
  amount: number;
  currency: string;
  status?: string | null;
  reason?: string | null;
  includesShipping: boolean;
  shippingAmount: number;
  createdAt?: string;
  lines: RefundLineSummaryDto[];
}

export interface OrderDto {
  id: number;
  userId: number;
  status: 'PENDING_PAYMENT' | 'PAID' | 'FULFILLED' | 'REFUNDED' | 'CANCELLED';
  subtotalAmount: number;
  taxAmount: number;
  shippingAmount: number;
  discountAmount: number;
  totalAmount: number;

  /** refund summary */
  refundedAmount?: number;
  shippingRefundedAmount?: number;
  fullyRefunded?: boolean;
  refunds?: RefundEventDto[];

  shippingMethod: ShippingMethod;
  stripePaymentIntentId?: string | null;
  offlinePaymentMethod?: OfflinePaymentMethod | null;
  couponCode?: string | null;
  origin?: 'ONLINE' | 'IN_PERSON';
  shippingAddress?: ShippingAddress | null;
  createdAt?: string;
  updatedAt?: string;
  items: OrderItemDto[];
}

