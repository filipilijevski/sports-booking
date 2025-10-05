/* Handles the JWT / refresh-token payload sent by Spring Security
 * after Google OAuth2 sign-in (302 to /oauth2/callback?accessToken=).
 * Dual-mode: in cookie-mode we ignore URL tokens (cookies are set). */

import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  setAccessToken,
  setRefreshToken,
  setFirstName,
  broadcastJwtUpdate,
} from '../lib/auth';
import { api } from '../lib/api';

const USE_COOKIE_AUTH =
  String(import.meta.env.VITE_USE_COOKIE_AUTH ?? '').toLowerCase() === 'true';

interface Props { onLogin?: () => void }

export default function OAuth2Callback({ onLogin }: Props = {}) {
  const navigate = useNavigate();
  const [params] = useSearchParams();

  useEffect(() => {
    (async () => {

      const accessToken  = params.get('accessToken');
      const refreshToken = params.get('refreshToken');
      const firstNameUrl = (params.get('firstName') ?? '').trim();

      // In legacy mode we expect tokens; in cookie-mode we can proceed without them
      if (!USE_COOKIE_AUTH && !accessToken) {
        navigate('/', { replace: true });
        return;
      }

      // Persist tokens only in legacy mode
      if (!USE_COOKIE_AUTH) {
        if (accessToken)  setAccessToken(accessToken);
        if (refreshToken) setRefreshToken(refreshToken);
      }

      // Persist firstName (param to fallback to /users/me)
      if (firstNameUrl) {
        setFirstName(firstNameUrl);
      } else {
        try {
          const me = await api<any>('/users/me');
          const fetched = me?.firstName ?? me?.given_name ?? me?.name ?? null;
          if (fetched) setFirstName(fetched);
        } catch { /* ignore */ }
      }

      const back = sessionStorage.getItem('postLoginRedirect') || '/';
      sessionStorage.removeItem('postLoginRedirect');

      setTimeout(() => {
        broadcastJwtUpdate();
        if (onLogin) onLogin();
        navigate(back, { replace: true });
      }, 0);
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return null;
}
