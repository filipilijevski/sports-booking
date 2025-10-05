import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],

  server: {
    proxy: {
      // REST - everything under /api
      '^/api/.*': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },

      // Google OAuth entry point *only*
      '/oauth2/authorization/google': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },

      // Spring’s callback that exchanges the code for tokens
      '^/login/.*': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
