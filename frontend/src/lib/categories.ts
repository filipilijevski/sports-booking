import { api } from './api';

export interface Category {
  id:   number;
  name: string;
}

/*  read  */
export async function fetchCategories(): Promise<Category[]> {
  return api<Category[]>('/categories') as Promise<Category[]>;
}

/*  admin create */
export async function createCategory(name: string): Promise<Category> {
  return api<Category>('/categories', {
    method: 'POST',
    body:   JSON.stringify({ name }),
  }) as Promise<Category>;
}

export const deleteCategory = (id: number) =>
  api<void>(`/categories/${id}`, { method: 'DELETE' });


/* (update could be added later) */
