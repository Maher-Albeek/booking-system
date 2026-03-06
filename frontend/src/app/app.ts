import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AuthStateService } from './auth-state.service';
import { LANGUAGE_OPTIONS, I18nService, LanguageCode } from './i18n.service';

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
  protected readonly i18n = inject(I18nService);
  protected readonly languageOptions = LANGUAGE_OPTIONS;
  protected readonly activeSection = signal<HeaderSection>('offers');

  protected readonly pageTitle = computed(() =>
    this.activeSection() === 'admin'
      ? this.i18n.t('app.title.admin')
      : this.activeSection() === 'profile'
        ? this.i18n.t('app.title.profile')
      : this.activeSection() === 'bookings'
        ? this.i18n.t('app.title.bookings')
      : this.activeSection() === 'payment'
        ? this.i18n.t('app.title.payment')
      : this.activeSection() === 'legal'
        ? this.i18n.t('app.title.legal')
      : this.activeSection() === 'login'
        ? this.i18n.t('app.title.login')
      : this.i18n.t('app.title.offers')
  );

  protected readonly pageSummary = computed(() =>
    this.activeSection() === 'admin'
      ? this.i18n.t('app.summary.admin')
      : this.activeSection() === 'profile'
        ? this.i18n.t('app.summary.profile')
      : this.activeSection() === 'bookings'
        ? this.i18n.t('app.summary.bookings')
      : this.activeSection() === 'payment'
        ? this.i18n.t('app.summary.payment')
      : this.activeSection() === 'legal'
        ? this.i18n.t('app.summary.legal')
      : this.activeSection() === 'login'
        ? this.i18n.t('app.summary.login')
      : this.i18n.t('app.summary.offers')
  );

  protected readonly roleLabel = computed(() => {
    if (!this.auth.isAuthenticated()) {
      return this.i18n.t('app.role.guest');
    }

    return this.auth.isAdmin() ? this.i18n.t('app.role.admin') : this.i18n.t('app.role.user');
  });

  protected readonly navLinks = computed<HeaderLink[]>(() => {
    const links: HeaderLink[] = [{ path: '/offers', label: this.i18n.t('app.link.offers') }];

    if (!this.auth.isAuthenticated()) {
      links.push({ path: '/login', label: this.i18n.t('app.link.login') });
    } else {
      links.push(
        { path: '/my-profile', label: this.i18n.t('app.link.myProfile') },
        { path: '/my-bookings', label: this.i18n.t('app.link.myBookings') },
        { path: '/payment-methods', label: this.i18n.t('app.link.paymentMethods') }
      );
    }

    if (this.auth.isAdmin()) {
      links.push(
        { path: '/admin/tools', label: this.i18n.t('app.link.adminTools') },
        { path: '/admin/manage-offers', label: this.i18n.t('app.link.manageOffers') },
        { path: '/admin/manage-cars', label: this.i18n.t('app.link.manageCars') },
        { path: '/admin/manage-users', label: this.i18n.t('app.link.manageUsers') }
      );
    }

    links.push(
      { path: '/impressum', label: this.i18n.t('app.link.impressum') },
      { path: '/datenschutz', label: this.i18n.t('app.link.datenschutz') }
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

  protected setLanguage(language: LanguageCode): void {
    this.i18n.setLanguage(language);
  }

  protected onLanguageChange(event: Event): void {
    const target = event.target as HTMLSelectElement | null;
    const language = target?.value as LanguageCode | undefined;
    if (!language) {
      return;
    }

    this.setLanguage(language);
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
