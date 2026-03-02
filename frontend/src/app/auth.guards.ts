import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthStateService } from './auth-state.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthStateService);
  const router = inject(Router);

  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthStateService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  return auth.isAdmin() ? true : router.createUrlTree(['/user']);
};

export const loginRedirectGuard: CanActivateFn = () => {
  const auth = inject(AuthStateService);
  const router = inject(Router);

  return auth.isAuthenticated() ? router.parseUrl(auth.landingRoute()) : true;
};
