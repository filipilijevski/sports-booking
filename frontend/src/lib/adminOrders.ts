import { api } from './api';

/* Keep names/types aligned with backend OrderStatus and OrderSearchFilter */
export type AdminOrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'FULFILLED' | 'REFUNDED' | 'CANCELLED';
export type OfflinePaymentMethod = 'CASH' | 'ETRANSFER' | 'TERMINAL' | 'OTHER';
export type ShippingMethod = 'REGULAR' | 'EXPRESS';

/* Minimal DTOs aligned with backend OrderDto */
export interface OrderItemDto {
  id: number;
  product: {
    id: number;
    sku?: string | null;
    name: string;
    brand?: string | null;
    price?: number | null;
    categoryId?: number | null;
    categoryName?: string | null;
    images?: { url: string; isPrimary: boolean; altText?: string | null }[];
  };
  quantity: number;
  unitPrice: number;
  totalPrice: number;
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
  userId: number | null;
  status: AdminOrderStatus;

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
  shippingAddress?: {
    fullName?: string | null;
    phone?: string | null;
    email?: string | null;
    line1?: string | null;
    line2?: string | null;
    city?: string | null;
    province?: string | null;
    postalCode?: string | null;
    country?: string | null;
  } | null;
  createdAt?: string;
  updatedAt?: string;
  items: OrderItemDto[];
}

/* Search filter mirrors backend OrderSearchFilter names */
export interface AdminOrderSearchFilter {
  orderId?: number;
  email?: string;
  name?: string;
  status?: AdminOrderStatus;
  includePendingPayment?: boolean;
  dateFrom?: string; // ISO instant
  dateTo?: string;   // ISO instant
  amountMin?: number;
  amountMax?: number;
  origin?: 'ONLINE' | 'IN_PERSON';
  offlinePaymentMethod?: OfflinePaymentMethod;
  page?: number;
  size?: number;
}

export async function searchAdminOrders(filter: AdminOrderSearchFilter = {}): Promise<OrderDto[]> {
  const usp = new URLSearchParams();
  if (filter.orderId != null)             usp.set('orderId', String(filter.orderId));
  if (filter.email)                       usp.set('email', filter.email.trim());
  if (filter.name)                        usp.set('name', filter.name.trim());
  if (filter.status)                      usp.set('status', filter.status);
  if (filter.includePendingPayment)       usp.set('includePendingPayment', 'true');
  if (filter.dateFrom)                    usp.set('dateFrom', filter.dateFrom);
  if (filter.dateTo)                      usp.set('dateTo', filter.dateTo);
  if (filter.amountMin != null)           usp.set('amountMin', String(filter.amountMin));
  if (filter.amountMax != null)           usp.set('amountMax', String(filter.amountMax));
  if (filter.origin)                      usp.set('origin', filter.origin);
  if (filter.offlinePaymentMethod)        usp.set('offlinePaymentMethod', filter.offlinePaymentMethod);
  if (filter.page != null)                usp.set('page', String(filter.page));
  if (filter.size != null)                usp.set('size', String(filter.size));

  return api<OrderDto[]>(`/admin/orders?${usp.toString()}`) as Promise<OrderDto[]>;
}

/** PATCH /api/admin/orders/{id}?status=FULFILLED|REFUNDED */
export async function updateAdminOrderStatus(id: number, status: Exclude<AdminOrderStatus, 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED'>): Promise<void> {
  await api<void>(`/admin/orders/${id}?status=${encodeURIComponent(status)}`, { method: 'PATCH' });
}
