import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { UserRole } from '../models/auth.model';
import { AuthService } from '../services/auth.service';

export function roleGuard(...allowedRoles: UserRole[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url }
      });
    }

    if (auth.hasRole(...allowedRoles)) {
      return true;
    }

    return router.createUrlTree(['/']);
  };
}

export const contributorGuard = roleGuard('CONTRIBUTOR', 'MODERATOR', 'ADMIN');
export const moderatorGuard = roleGuard('MODERATOR', 'ADMIN');
export const adminGuard = roleGuard('ADMIN');
