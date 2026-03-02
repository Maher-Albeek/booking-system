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
        ? 'Browse available cars, reserve an open slot, and manage live bookings.'
        : 'Authenticate with your booking account to open the correct page for your role.'
  );

  constructor() {
    this.syncActiveView(this.router.url);

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((event) => this.syncActiveView(event.urlAfterRedirects));
  }

  protected logout(): void {
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
