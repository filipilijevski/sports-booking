/* src/components/CartDrawer.tsx */
import {
  Drawer, Box, IconButton, Typography, Divider, List, ListItem,
  ListItemText, ListItemSecondaryAction, Button, Stack, TextField,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import DeleteIcon from '@mui/icons-material/Delete';
import { useCart } from '../context/CartContext';
import { useRole } from '../context/RoleContext';
import { useLocation, useNavigate } from 'react-router-dom';
import { handleNavbarCheckout } from '../lib/checkoutRouting';

export default function CartDrawer() {
  const { state, closeDrawer, update, remove } = useCart();
  const { items, subtotal, drawerOpen } = state;
  const { role } = useRole();
  const navigate = useNavigate();
  const location = useLocation();

  const onCheckout = () => {
    handleNavbarCheckout({
      role,
      locationPath: location.pathname,
      navigate,
      closeDrawer,
    });
  };

  return (
    <Drawer anchor="right" open={drawerOpen} onClose={closeDrawer}>
      <Box sx={{ width: 340, p: 2, display: 'flex', flexDirection: 'column', height: '100%' }}>
        {/* header */}
        <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
          <Typography variant="h6">Your Cart</Typography>
          <IconButton size="small" onClick={closeDrawer}><CloseIcon /></IconButton>
        </Stack>
        <Divider sx={{ mb: 2 }} />

        {/* items */}
        <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
          {items.length === 0 ? (
            <Typography sx={{ mt: 4, textAlign: 'center' }}>Cart is empty.</Typography>
          ) : (
            <List>
              {items.map(it => (
                <ListItem key={it.id} alignItems="flex-start">
                  <ListItemText
                    primary={it.product.name}
                    secondary={`$${(it.unitPrice * it.quantity).toFixed(2)}`}
                  />
                  <TextField
                    type="number"
                    size="small"
                    value={it.quantity}
                    inputProps={{ min: 1, style: { width: 60, textAlign: 'center' } }}
                    onChange={e => update(it.id, Number(e.target.value))}
                  />
                  <ListItemSecondaryAction>
                    <IconButton edge="end" onClick={() => remove(it.id)}>
                      <DeleteIcon />
                    </IconButton>
                  </ListItemSecondaryAction>
                </ListItem>
              ))}
            </List>
          )}
        </Box>

        {/* footer */}
        <Divider sx={{ my: 2 }} />
        <Typography variant="subtitle1" sx={{ mb: 2 }}>
          Sub-total: <strong>${subtotal.toFixed(2)}</strong>
        </Typography>
        <Button
          variant="contained"
          color="primary"
          fullWidth
          disabled={items.length === 0}
          onClick={onCheckout}
        >
          Checkout
        </Button>
      </Box>
    </Drawer>
  );
}
