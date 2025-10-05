import {
  Card, CardMedia, CardContent, CardActions,
  Typography, Button, Skeleton,
} from '@mui/material';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import { type MouseEvent } from 'react';

import { useCart } from '../context/CartContext';
import { useRole } from '../context/RoleContext';
import type { Product } from '../lib/products';
import { resolveImgUrl } from '../lib/assets';

const CARD_W = 220;          // same width as admin card

interface Props {
  product: Product;
  /** optional click-handler (e.g. to open a modal) */
  onClick?: () => void;
}

export default function ProductCard({ product, onClick }: Props) {
  const { add, openDrawer } = useCart();
  const { role }            = useRole();

  const imgObj = product.images.find(i => i.isPrimary) ?? product.images[0];
  const imgSrc = resolveImgUrl(imgObj?.url);

  /** stop bubbling so the click doesn't also trigger the modal */
  const handleAdd = async (e: MouseEvent) => {
    e.stopPropagation();
    await add(product.id, 1, product);
    openDrawer();
  };

  return (
    <Card
      variant="outlined"
      onClick={onClick}
      sx={{
        cursor: onClick ? 'pointer' : 'default',   // pointer only when interactive
        width: CARD_W, minWidth: CARD_W, maxWidth: CARD_W,
        height: '100%', mx: 'auto',
        display: 'flex', flexDirection: 'column', px: 1, py: 2,
      }}
    >
      {imgSrc ? (
        <CardMedia
          component="img"
          image={imgSrc}
          alt={imgObj?.altText ?? product.name}
          sx={{ width: '100%', height: 175, objectFit: 'contain', borderRadius: 1 }}
        />
      ) : (
        <Skeleton variant="rectangular" sx={{ width: '100%', height: 175, borderRadius: 1 }} />
      )}

      <CardContent sx={{ flexGrow: 1, p: 1 }}>
        <Typography variant="subtitle1" noWrap gutterBottom>
          {product.name}
        </Typography>
        {product.brand && (
          <Typography variant="subtitle2" color="text.secondary" noWrap gutterBottom>
            {product.brand}
          </Typography>
        )}
        <Typography variant="subtitle1">${product.price.toFixed(2)}</Typography>
      </CardContent>

      <CardActions sx={{ justifyContent: 'center', pb: 1 }}>
        <Button
          size="small"
          variant="contained"
          startIcon={<AddShoppingCartIcon fontSize="small" />}
          onClick={handleAdd}
          disabled={product.inventoryQty !== undefined && product.inventoryQty < 1}
        >
          {role === 'GUEST' ? 'Add to Cart' : 'Add to Cart'}
        </Button>
      </CardActions>
    </Card>
  );
}
