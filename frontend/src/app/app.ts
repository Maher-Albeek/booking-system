import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, finalize } from 'rxjs';

import { AuthStateService } from './auth-state.service';
import { LANGUAGE_OPTIONS, I18nService, LanguageCode } from './i18n.service';

type HeaderLink = {
  path: string;
  label: string;
};

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly auth = inject(AuthStateService);
  protected readonly i18n = inject(I18nService);
  protected readonly languageOptions = LANGUAGE_OPTIONS;
  protected readonly menuOpen = signal(false);
  protected readonly authModalOpen = signal(false);
  protected readonly authMode = signal<'login' | 'register'>('login');
  protected readonly identifier = signal('');
  protected readonly password = signal('');
  protected readonly submitting = signal(false);
  protected readonly resettingPassword = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly resetMessage = signal<string | null>(null);
  protected readonly registerName = signal('');
  protected readonly registerEmail = signal('');
  protected readonly registerPassword = signal('');
  protected readonly registerSubmitting = signal(false);
  protected readonly registerError = signal<string | null>(null);

  protected readonly navLinks = computed<HeaderLink[]>(() => {
    const links: HeaderLink[] = [{ path: '/offers', label: this.i18n.t('app.link.offers') }];

    if (this.auth.isAuthenticated()) {
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
    this.handleRoute(this.router.url);

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((event) => {
        this.handleRoute(event.urlAfterRedirects);
      });
  }

  protected toggleMenu(): void {
    this.menuOpen.update((value) => !value);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected openAuthModal(mode: 'login' | 'register' = 'login'): void {
    this.setAuthMode(mode);
    this.authModalOpen.set(true);
    this.closeMenu();
  }

  protected closeAuthModal(): void {
    this.authModalOpen.set(false);
    this.error.set(null);
    this.resetMessage.set(null);
    this.registerError.set(null);
  }

  protected setAuthMode(mode: 'login' | 'register'): void {
    this.authMode.set(mode);
    this.error.set(null);
    this.resetMessage.set(null);
    this.registerError.set(null);
  }

  protected submitLogin(): void {
    const identifier = this.identifier().trim();
    const password = this.password().trim();

    if (!identifier || !password) {
      this.error.set(this.i18n.t('login.error.credentialsRequired'));
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    this.resetMessage.set(null);

    this.auth
      .login(identifier, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.closeAuthModal();
          void this.router.navigateByUrl(this.auth.landingRoute());
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, this.i18n.t('login.error.loginFailed')));
        }
      });
  }

  protected submitPasswordReset(): void {
    const identifier = this.identifier().trim();
    const newPassword = this.password().trim();

    if (!identifier || !newPassword) {
      this.error.set(this.i18n.t('login.error.credentialsRequired'));
      return;
    }

    if (newPassword.length < 6) {
      this.error.set(this.i18n.t('login.error.passwordLength'));
      return;
    }

    this.resettingPassword.set(true);
    this.error.set(null);
    this.resetMessage.set(null);

    this.auth
      .resetPassword(identifier, newPassword)
      .pipe(finalize(() => this.resettingPassword.set(false)))
      .subscribe({
        next: () => {
          this.password.set('');
          this.resetMessage.set(this.i18n.t('login.success.passwordReset'));
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, this.i18n.t('login.error.resetFailed')));
        }
      });
  }

  protected submitRegistration(): void {
    const name = this.registerName().trim();
    const email = this.registerEmail().trim();
    const password = this.registerPassword().trim();

    if (!name || !email) {
      this.registerError.set(this.i18n.t('login.error.registrationNameEmailRequired'));
      return;
    }

    if (password.length < 6) {
      this.registerError.set(this.i18n.t('login.error.passwordLength'));
      return;
    }

    this.registerSubmitting.set(true);
    this.registerError.set(null);
    this.resetMessage.set(null);

    this.auth
      .register(name, email, password)
      .pipe(finalize(() => this.registerSubmitting.set(false)))
      .subscribe({
        next: () => {
          this.closeAuthModal();
          void this.router.navigateByUrl('/offers');
        },
        error: (error: HttpErrorResponse) => {
          this.registerError.set(this.readApiError(error, this.i18n.t('login.error.registrationFailed')));
        }
      });
  }

  protected logout(): void {
    this.auth.logout();
    this.closeMenu();
    this.closeAuthModal();
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

  private handleRoute(url: string): void {
    this.closeMenu();

    const [path, query = ''] = url.split('?');
    if (!path.startsWith('/login')) {
      return;
    }

    const params = new URLSearchParams(query);
    const mode = params.get('mode') === 'register' ? 'register' : 'login';
    this.openAuthModal(mode);
    void this.router.navigate(['/offers'], { replaceUrl: true });
  }

  private readApiError(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }

    if (error.error && typeof error.error === 'object') {
      const message = (error.error as { message?: string }).message;
      if (message) {
        return message;
      }
    }

    return fallback;
  }
}
