import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { AccessRole, AccessStateService } from './access-state.service';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly access = inject(AccessStateService);
  protected readonly activeView = signal<'user' | 'admin'>('user');

  protected readonly pageTitle = computed(() =>
    this.activeView() === 'admin' ? 'Admin operations interface' : 'Customer booking interface'
  );

  protected readonly pageSummary = computed(() =>
    this.activeView() === 'admin'
      ? 'Create and remove fleet cars, time slots, and user accounts from the same Angular app.'
      : 'Browse available cars, reserve an open slot, and manage live bookings against the Spring API.'
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

  protected setRole(role: AccessRole): void {
    this.access.setRole(role);
  }

  protected setAlias(alias: string): void {
    this.access.setAlias(alias);
  }

  protected openSelectedInterface(): void {
    void this.router.navigate([this.access.isAdmin() ? '/admin' : '/user']);
  }

  private syncActiveView(url: string): void {
    const path = url.split('?')[0];
    this.activeView.set(path.startsWith('/admin') ? 'admin' : 'user');
  }
}
