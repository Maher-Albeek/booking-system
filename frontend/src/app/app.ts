import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AuthStateService } from './auth-state.service';

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
  protected readonly activeView = signal<'login' | 'user' | 'admin'>('login');
  protected readonly userMenuOpen = signal(false);

  protected readonly pageTitle = computed(() =>
    this.activeView() === 'admin'
      ? 'Admin workspace'
      : this.activeView() === 'user'
        ? 'Customer workspace'
        : 'Sign in'
  );

  protected readonly pageSummary = computed(() =>
    this.activeView() === 'admin'
      ? 'Manage cars, time slots, users, and reservations.'
      : this.activeView() === 'user'
        ? 'Browse available cars, choose your own booking time window, and manage live bookings.'
        : 'Authenticate with your booking account to open the correct page for your role.'
  );

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
    this.syncActiveView(this.router.url);

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((event) => {
        this.userMenuOpen.set(false);
        this.syncActiveView(event.urlAfterRedirects);
      });
  }

  protected toggleUserMenu(): void {
    this.userMenuOpen.update((value) => !value);
  }

  protected closeUserMenu(): void {
    this.userMenuOpen.set(false);
  }

  protected logout(): void {
    this.userMenuOpen.set(false);
    this.auth.logout();
    void this.router.navigate(['/login']);
  }

  private syncActiveView(url: string): void {
    const path = url.split('?')[0];
    this.activeView.set(
      path.startsWith('/admin') ? 'admin' : path.startsWith('/user') ? 'user' : 'login'
    );
  }
}
