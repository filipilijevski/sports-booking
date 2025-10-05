import * as yup from 'yup';
import type { ShippingAddress } from '../types';

/** Matches the exact field names we already use */
export const shippingSchema: yup.Schema<ShippingAddress> = yup.object({
  fullName:   yup.string().required('Full name is required'),
  phone:      yup.string().min(10, 'Phone must be at least 10 digits').required(),
  email:      yup.string().email('Invalid e-mail').required(),
  line1:      yup.string().required('Address line 1 is required'),
  line2:      yup.string(),                     // optional
  city:       yup.string().required(),
  province:   yup.string().required(),
  postalCode: yup.string().required(),
  country:    yup.string().required(),
});
