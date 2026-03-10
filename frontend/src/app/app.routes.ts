import { Routes } from '@angular/router';

import { AdminPageComponent } from './admin-page.component';
import { AccountPageComponent } from './account-page.component';
import { adminGuard, authGuard, loginRedirectGuard } from './auth.guards';
import { LegalPageComponent } from './legal-page.component';
import { LoginPageComponent } from './login-page.component';
import { UserPageComponent } from './user-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'offers'
  },
  {
    path: 'login',
    component: LoginPageComponent,
    title: 'Login',
    canActivate: [loginRedirectGuard]
  },
  {
    path: 'offers',
    component: UserPageComponent,
    title: 'Offers',
    data: {
      userPageMode: 'offers'
    }
  },
  {
    path: 'user',
    pathMatch: 'full',
    redirectTo: 'offers'
  },
  {
    path: 'my-bookings',
    component: UserPageComponent,
    title: 'My bookings',
    canActivate: [authGuard],
    data: {
      userPageMode: 'bookings'
    }
  },
  {
    path: 'my-profile',
    component: AccountPageComponent,
    title: 'My profile',
    canActivate: [authGuard],
    data: {
      accountPageMode: 'profile'
    }
  },
  {
    path: 'payment-methods',
    component: AccountPageComponent,
    title: 'Payment methods',
    canActivate: [authGuard],
    data: {
      accountPageMode: 'payment'
    }
  },
  {
    path: 'account',
    pathMatch: 'full',
    redirectTo: 'my-profile'
  },
  {
    path: 'impressum',
    component: LegalPageComponent,
    title: 'Impressum',
    data: {
      legalPageMode: 'impressum'
    }
  },
  {
    path: 'datenschutz',
    component: LegalPageComponent,
    title: 'Datenschutz',
    data: {
      legalPageMode: 'datenschutz'
    }
  },
  {
    path: 'admin',
    component: AdminPageComponent,
    title: 'Admin tools',
    canActivate: [adminGuard],
    data: {
      adminPageMode: 'tools'
    }
  },
  {
    path: 'admin/tools',
    component: AdminPageComponent,
    title: 'Admin tools',
    canActivate: [adminGuard],
    data: {
      adminPageMode: 'tools'
    }
  },
  {
    path: 'admin/manage-offers',
    component: AdminPageComponent,
    title: 'Manage offers',
    canActivate: [adminGuard],
    data: {
      adminPageMode: 'offers'
    }
  },
  {
    path: 'admin/manage-cars',
    component: AdminPageComponent,
    title: 'Manage cars',
    canActivate: [adminGuard],
    data: {
      adminPageMode: 'cars'
    }
  },
  {
    path: 'admin/manage-users',
    component: AdminPageComponent,
    title: 'Manage users',
    canActivate: [adminGuard],
    data: {
      adminPageMode: 'users'
    }
  },
  {
    path: 'admin/manage-legal',
    component: AdminPageComponent,
    title: 'Manage legal',
    canActivate: [adminGuard],
    data: {
      adminPageMode: 'legal'
    }
  },
  {
    path: '**',
    redirectTo: 'offers'
  }
];
