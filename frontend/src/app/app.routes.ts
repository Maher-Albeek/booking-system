import { Routes } from '@angular/router';

import { AdminPageComponent } from './admin-page.component';
import { UserPageComponent } from './user-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'user'
  },
  {
    path: 'user',
    component: UserPageComponent,
    title: 'User Interface'
  },
  {
    path: 'admin',
    component: AdminPageComponent,
    title: 'Admin Interface'
  },
  {
    path: '**',
    redirectTo: 'user'
  }
];
