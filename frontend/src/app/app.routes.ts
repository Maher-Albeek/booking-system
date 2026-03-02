import { Routes } from '@angular/router';

import { AdminPageComponent } from './admin-page.component';
import { adminGuard, loginRedirectGuard } from './auth.guards';
import { LoginPageComponent } from './login-page.component';
import { UserPageComponent } from './user-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'user'
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
    title: 'User Interface'
  },
  {
    path: 'admin',
    component: AdminPageComponent,
    title: 'Admin Interface',
    canActivate: [adminGuard]
  },
  {
    path: '**',
    redirectTo: 'user'
  }
];
