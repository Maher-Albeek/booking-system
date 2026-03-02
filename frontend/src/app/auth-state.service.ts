import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export type AuthRole = 'USER' | 'ADMIN';

export type AuthUser = {
  id: number;
  name: string;
  email: string | null;
  role: AuthRole;
};

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly http = inject(HttpClient);
  private readonly sessionStorageKey = 'booking-system.auth.session';

  readonly user = signal<AuthUser | null>(this.readUser());
  readonly isAuthenticated = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  login(identifier: string, password: string): Observable<AuthUser> {
    return this.http
      .post<AuthUser>('/api/auth/login', {
        identifier: identifier.trim(),
        password: password.trim()
      })
      .pipe(
        tap((user) => {
          const normalizedUser = this.normalizeUser(user);

          if (!normalizedUser) {
            throw new Error('Login response did not contain a valid user session.');
          }

          this.persistUser(normalizedUser);
        })
      );
  }

  logout(): void {
    this.user.set(null);

    try {
      localStorage.removeItem(this.sessionStorageKey);
    } catch {
      // Ignore storage failures so logout still clears in-memory state.
    }
  }

  landingRoute(): '/admin' | '/user' {
    return this.isAdmin() ? '/admin' : '/user';
  }

  private readUser(): AuthUser | null {
    try {
      const rawValue = localStorage.getItem(this.sessionStorageKey);
      if (!rawValue) {
        return null;
      }

      return this.normalizeUser(JSON.parse(rawValue) as Partial<AuthUser>);
    } catch {
      return null;
    }
  }

  private persistUser(user: AuthUser): void {
    this.user.set(user);

    try {
      localStorage.setItem(this.sessionStorageKey, JSON.stringify(user));
    } catch {
      // Ignore storage failures so the live session still works in memory.
    }
  }

  private normalizeUser(user: Partial<AuthUser> | null | undefined): AuthUser | null {
    if (!user || typeof user.id !== 'number' || !user.name) {
      return null;
    }

    return {
      id: user.id,
      name: user.name,
      email: user.email ?? null,
      role: user.role === 'ADMIN' ? 'ADMIN' : 'USER'
    };
  }
}
