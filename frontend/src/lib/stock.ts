/* read-only availability from the backend
 * Returns a map: { [productId]: availableQty } */
import { api } from './api';

export async function fetchAvailability(productIds: number[]): Promise<Record<number, number>> {
  if (!productIds || productIds.length === 0) return {};
  const res = await api<{ available: Record<string, number> }>('/stock/availability', {
    method: 'POST',
    body:   JSON.stringify({ productIds }),
  });
  const map: Record<number, number> = {};
  const payload = res?.available ?? {};
  for (const [k, v] of Object.entries(payload)) {
    map[Number(k)] = typeof v === 'number' ? v : 0;
  }
  return map;
}
