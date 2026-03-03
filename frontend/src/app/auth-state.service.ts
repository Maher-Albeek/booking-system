import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export type AuthRole = 'USER' | 'ADMIN';

export type AuthUser = {
  id: number;
  name: string;
  email: string | null;
  role: AuthRole;
  firstName: string | null;
  lastName: string | null;
  address: string | null;
  birthDate: string | null;
  avatarUrl: string | null;
  paymentMethods: string[];
};

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly http = inject(HttpClient);
  private readonly sessionStorageKey = 'booking-system.auth.session';

  readonly user = signal<AuthUser | null>(this.readUser());
  readonly isAuthenticated = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  login(identifier: string, password: string): Observable<AuthUser> {
    return this.persistSession(
      this.http.post<AuthUser>('/api/auth/login', {
        identifier: identifier.trim(),
        password: password.trim()
      })
    );
  }

  register(name: string, email: string, password: string): Observable<AuthUser> {
    return this.persistSession(
      this.http.post<AuthUser>('/api/auth/register', {
        name: name.trim(),
        email: email.trim(),
        password: password.trim()
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

  syncUser(user: Partial<AuthUser>): void {
    const normalizedUser = this.normalizeUser(user);

    if (!normalizedUser) {
      throw new Error('User session update did not contain a valid user.');
    }

    this.persistUser(normalizedUser);
  }

  private persistSession(request: Observable<AuthUser>): Observable<AuthUser> {
    return request.pipe(
      tap((user) => {
        const normalizedUser = this.normalizeUser(user);

        if (!normalizedUser) {
          throw new Error('Login response did not contain a valid user session.');
        }

        this.persistUser(normalizedUser);
      })
    );
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
      role: user.role === 'ADMIN' ? 'ADMIN' : 'USER',
      firstName: user.firstName ?? null,
      lastName: user.lastName ?? null,
      address: user.address ?? null,
      birthDate: user.birthDate ?? null,
      avatarUrl: user.avatarUrl ?? null,
      paymentMethods: Array.isArray(user.paymentMethods)
        ? user.paymentMethods.filter((value): value is string => typeof value === 'string')
        : []
    };
  }
}
