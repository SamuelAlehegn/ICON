import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const RETRY_HEADER = 'X-Auth-Retry';

const AUTH_PATHS = [
  '/api/v1/auth/login',
  '/api/v1/auth/register',
  '/api/v1/auth/refresh',
  '/api/v1/auth/logout'
];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isApiRequest = request.url.includes('/api/');
  const isAuthRequest = AUTH_PATHS.some((path) => request.url.includes(path));

  if (!isApiRequest || isAuthRequest) {
    return next(request);
  }

  const accessToken = auth.getAccessToken();
  const authorizedRequest = accessToken
    ? request.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
    : request;

  return next(authorizedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || (error.status !== 401 && error.status !== 403)) {
        return throwError(() => error);
      }

      const isSessionError = error.status === 401 || isTokenExpired(auth.getAccessToken());
      if (!isSessionError) {
        return throwError(() => error);
      }

      if (request.headers.has(RETRY_HEADER) || !auth.getRefreshToken()) {
        endSession(auth, router);
        return throwError(() => error);
      }

      return auth.refreshAccessToken().pipe(
        catchError((refreshError: unknown) => {
          endSession(auth, router);
          return throwError(() => refreshError);
        }),
        switchMap((tokens) =>
          next(
            request.clone({
              setHeaders: {
                Authorization: `Bearer ${tokens.accessToken}`,
                [RETRY_HEADER]: '1'
              }
            })
          ).pipe(
            catchError((retryError: unknown) => {
              if (retryError instanceof HttpErrorResponse && retryError.status === 401) {
                endSession(auth, router);
              }
              return throwError(() => retryError);
            })
          )
        )
      );
    })
  );
};

function isTokenExpired(token: string | null): boolean {
  if (!token) {
    return true;
  }

  const payload = token.split('.')[1];
  if (!payload) {
    return true;
  }

  try {
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    const claims = JSON.parse(atob(padded)) as { exp?: number };
    if (typeof claims.exp !== 'number') {
      return true;
    }
    return claims.exp * 1000 <= Date.now() + 30_000;
  } catch {
    return true;
  }
}

function endSession(auth: AuthService, router: Router): void {
  auth.clearSession();
  if (router.url.startsWith('/login')) {
    return;
  }
  void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
}
