import {
  Card, CardMedia, CardContent, Typography, Box,
} from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { type Product } from '../lib/products';
import { resolveImgUrl } from '../lib/assets';

const CARD_W = 220;          // single source of truth

interface Props {
  product: Product;
  /** optional click-handler to open modal */
  onClick?: () => void;
}

export default function AdminProductCard({ product, onClick }: Props) {
  const nav = useNavigate();

  const primary = product.images.find(i => i.isPrimary) ?? product.images[0];
  const imgSrc  = resolveImgUrl(primary?.url);

  const handle = onClick ?? (() => nav(`/shop/${product.id}`));

  return (
    <Card
      onClick={handle}
      sx={{
        cursor: 'pointer',
        width: CARD_W, minWidth: CARD_W, maxWidth: CARD_W,
        height: '100%', mx: 'auto',
        display: 'flex', flexDirection: 'column', px: 1, py: 2,
      }}
    >
      <Box>
        <CardMedia
          component="img"
          sx={{ width: '100%', height: 175, objectFit: 'contain' }}
          image={imgSrc}
          alt={primary?.altText ?? product.name}
        />
      </Box>

      <CardContent sx={{ flexGrow: 1 }}>
        <Typography variant="subtitle2" color="text.secondary" noWrap>
          {product.brand}
          {product.categoryName && ` • ${product.categoryName}`}
        </Typography>
        <Typography variant="subtitle1" noWrap>
          {product.name}
        </Typography>
        <Typography variant="subtitle1" sx={{ mt: 1 }}>
          ${product.price.toFixed(2)}
        </Typography>
      </CardContent>
    </Card>
  );
}
