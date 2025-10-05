import {
  Box, Button, Container, Grid, Typography, Breadcrumbs,
  Link, Chip, CircularProgress,
} from '@mui/material';
import { useEffect, useState } from 'react';
import {
  useNavigate, useParams, Link as RouterLink,
} from 'react-router-dom';

import { type Product, fetchProduct } from '../lib/products';
import { addToCart } from '../lib/cart';
import { resolveImgUrl } from '../lib/assets';

export default function ProductDetail() {
  const { id }          = useParams();
  const nav             = useNavigate();
  const [product, setP] = useState<Product | null>(null);
  const [loading, setL] = useState(false);

  useEffect(() => {
    (async () => {
      setL(true);
      try { if (id) setP(await fetchProduct(Number(id))); }
      finally { setL(false); }
    })();
  }, [id]);

  if (loading) return <Box sx={{ textAlign:'center', py:10 }}><CircularProgress /></Box>;
  if (!product)  return null;

  const img    = product.images.find(i => i.isPrimary) ?? product.images[0];
  const imgSrc = resolveImgUrl(img?.url);
  const authed = !!localStorage.getItem('accessToken');

  return (
    <Container sx={{ mt: 4 }}>
      <Breadcrumbs sx={{ mb: 2 }}>
        <Link component={RouterLink} to="/shop">Pro Shop</Link>
        <Typography>{product.name}</Typography>
      </Breadcrumbs>

      <Grid container spacing={4}>
        {/* image */}
        <Grid item xs={12} md={6}>
          <Box
            component="img"
            src={imgSrc}
            alt={img?.altText ?? product.name}
            sx={{ width:'100%', borderRadius:2 }}
          />
        </Grid>

        {/* info */}
        <Grid item xs={12} md={6}>
          <Typography variant="h4" gutterBottom>{product.name}</Typography>
          <Typography variant="subtitle1" color="text.secondary" gutterBottom>
            {product.brand}
          </Typography>

          {product.categoryName && (
            <Chip label={product.categoryName} sx={{ mb: 2 }} />
          )}

          <Typography variant="h5" sx={{ my: 2 }}>
            ${product.price.toFixed(2)}
          </Typography>

          <Button
            variant="contained"
            size="large"
            startIcon={<img src="/cart-icon.svg" width={20} />}
            onClick={() => authed ? addToCart(product.id) : nav('/login')}
            disabled={!authed}
          >
            {authed ? 'Add to cart' : 'Login to buy'}
          </Button>

          {!!product.description && (
            <Typography sx={{ mt: 4, whiteSpace:'pre-line' }}>
              {product.description}
            </Typography>
          )}

          {product.grams !== undefined && (
            <Typography sx={{ mt: 2 }} color="text.secondary">
              Weight: <b>{product.grams} g</b>
            </Typography>
          )}
        </Grid>
      </Grid>
    </Container>
  );
}
