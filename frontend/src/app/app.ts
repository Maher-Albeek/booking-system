import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AuthStateService } from './auth-state.service';

type HeaderSection = 'login' | 'offers' | 'bookings' | 'profile' | 'payment' | 'admin' | 'legal';

type HeaderLink = {
  path: string;
  label: string;
};

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly auth = inject(AuthStateService);
  protected readonly activeSection = signal<HeaderSection>('offers');

  protected readonly pageTitle = computed(() =>
    this.activeSection() === 'admin'
      ? 'Admin workspace'
      : this.activeSection() === 'profile'
        ? 'My profile'
      : this.activeSection() === 'bookings'
        ? 'My bookings'
      : this.activeSection() === 'payment'
        ? 'Payment methods'
      : this.activeSection() === 'legal'
        ? 'Legal pages'
      : this.activeSection() === 'login'
        ? 'Sign in'
      : 'Offers'
  );

  protected readonly pageSummary = computed(() =>
    this.activeSection() === 'admin'
      ? 'Manage cars, users, and reservations.'
      : this.activeSection() === 'profile'
        ? 'Edit your personal profile details.'
      : this.activeSection() === 'bookings'
        ? 'Review and manage your reservations.'
      : this.activeSection() === 'payment'
        ? 'Set and update your payment options.'
      : this.activeSection() === 'legal'
        ? 'Impressum and Datenschutz information.'
      : this.activeSection() === 'login'
        ? 'Authenticate with your booking account.'
      : 'Browse available car offers.'
  );

  protected readonly roleLabel = computed(() => {
    if (!this.auth.isAuthenticated()) {
      return 'Guest';
    }

    return this.auth.isAdmin() ? 'Administrator' : 'User';
  });

  protected readonly navLinks = computed<HeaderLink[]>(() => {
    const links: HeaderLink[] = [{ path: '/offers', label: 'Offers' }];

    if (!this.auth.isAuthenticated()) {
      links.push({ path: '/login', label: 'Login' });
    } else {
      links.push(
        { path: '/my-profile', label: 'My Profile' },
        { path: '/my-bookings', label: 'My Bookings' },
        { path: '/payment-methods', label: 'My Payment Method' }
      );
    }

    if (this.auth.isAdmin()) {
      links.push(
        { path: '/admin/tools', label: 'Admin Tools' },
        { path: '/admin/manage-offers', label: 'Manage Offers' },
        { path: '/admin/manage-cars', label: 'Manage Cars' },
        { path: '/admin/manage-users', label: 'Manage Users' }
      );
    }

    links.push(
      { path: '/impressum', label: 'Impressum' },
      { path: '/datenschutz', label: 'Datenschutz' }
    );

    return links;
  });

  protected readonly userInitials = computed(() => {
    const user = this.auth.user();
    const source = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.name || 'U';

    return source
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  });

  constructor() {
    this.syncActiveSection(this.router.url);

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((event) => {
        this.syncActiveSection(event.urlAfterRedirects);
      });
  }

  protected logout(): void {
    this.auth.logout();
    void this.router.navigate(['/offers']);
  }

  private syncActiveSection(url: string): void {
    const path = url.split('?')[0];

    if (path.startsWith('/admin')) {
      this.activeSection.set('admin');
      return;
    }

    if (path.startsWith('/my-profile') || path.startsWith('/account')) {
      this.activeSection.set('profile');
      return;
    }

    if (path.startsWith('/my-bookings')) {
      this.activeSection.set('bookings');
      return;
    }

    if (path.startsWith('/payment-methods')) {
      this.activeSection.set('payment');
      return;
    }

    if (path.startsWith('/impressum') || path.startsWith('/datenschutz')) {
      this.activeSection.set('legal');
      return;
    }

    if (path.startsWith('/login')) {
      this.activeSection.set('login');
      return;
    }

    this.activeSection.set('offers');
  }
}
