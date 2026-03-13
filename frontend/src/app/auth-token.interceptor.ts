import { HttpInterceptorFn } from '@angular/common/http';

const AUTH_TOKEN_STORAGE_KEY = 'booking-system.auth.token';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const token = readToken();
  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
  return next(authReq);
};

export function persistAuthToken(token: string | null): void {
  try {
    if (!token) {
      localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
      return;
    }
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
  } catch {
    // Ignore storage failures so app still works in memory.
  }
}

export function readToken(): string | null {
  try {
    const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
    if (!token || !token.trim()) {
      return null;
    }
    return token.trim();
  } catch {
    return null;
  }
}
