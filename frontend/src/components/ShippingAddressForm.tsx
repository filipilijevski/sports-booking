import { Grid, TextField } from '@mui/material';
import type { ShippingAddress } from '../types';

interface Props {
  value: ShippingAddress;
  onChange: (v: ShippingAddress) => void;
}

/**
 * Controlled form - mutates parent state in a type-safe way
 * while staying 100 % compatible with the existing
 * ShippingAddress interface and backend DTOs.
 */
export default function ShippingAddressForm({ value, onChange }: Props) {
  /** Generic field updater */
  const upd = <K extends keyof ShippingAddress>(k: K, v: ShippingAddress[K]) =>
    onChange({ ...value, [k]: v });

  return (
    <Grid container spacing={2}>
      {/* full name + phone */}
      <Grid item xs={12} sm={6}>
        <TextField
          label="Full Name"
          fullWidth required autoComplete="name"
          value={value.fullName}
          onChange={e => upd('fullName', e.target.value)}
        />
      </Grid>
      <Grid item xs={12} sm={6}>
        <TextField
          label="Phone"
          fullWidth required autoComplete="tel"
          value={value.phone}
          onChange={e => upd('phone', e.target.value)}
        />
      </Grid>

      {/* email */}
      <Grid item xs={12}>
        <TextField
          label="Email"
          fullWidth required autoComplete="email"
          value={value.email}
          onChange={e => upd('email', e.target.value)}
        />
      </Grid>

      {/* address lines */}
      <Grid item xs={12}>
        <TextField
          label="Address Line 1"
          fullWidth required autoComplete="address-line1"
          value={value.line1}
          onChange={e => upd('line1', e.target.value)}
        />
      </Grid>
      <Grid item xs={12}>
        <TextField
          label="Address Line 2"
          fullWidth autoComplete="address-line2"
          value={value.line2}
          onChange={e => upd('line2', e.target.value)}
        />
      </Grid>

      {/* city / province / postal */}
      <Grid item xs={12} sm={6}>
        <TextField
          label="City"
          fullWidth required autoComplete="address-level2"
          value={value.city}
          onChange={e => upd('city', e.target.value)}
        />
      </Grid>
      <Grid item xs={6} sm={3}>
        <TextField
          label="Province"
          fullWidth required autoComplete="address-level1"
          value={value.province}
          onChange={e => upd('province', e.target.value)}
        />
      </Grid>
      <Grid item xs={6} sm={3}>
        <TextField
          label="Postal Code"
          fullWidth required autoComplete="postal-code"
          value={value.postalCode}
          onChange={e => upd('postalCode', e.target.value)}
        />
      </Grid>
    </Grid>
  );
}
