import { Routes } from '@angular/router';

import { AdminPageComponent } from './admin-page.component';
import { adminGuard, authGuard, loginRedirectGuard } from './auth.guards';
import { LoginPageComponent } from './login-page.component';
import { UserPageComponent } from './user-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login'
  },
  {
    path: 'login',
    component: LoginPageComponent,
    title: 'Login',
    canActivate: [loginRedirectGuard]
  },
  {
    path: 'user',
    component: UserPageComponent,
    title: 'User Interface',
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    component: AdminPageComponent,
    title: 'Admin Interface',
    canActivate: [adminGuard]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
