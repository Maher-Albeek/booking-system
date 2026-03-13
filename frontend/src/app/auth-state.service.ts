import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { persistAuthToken, readToken } from './auth-token.interceptor';

export type AuthRole = 'USER' | 'ADMIN' | 'EMPLOYEE';

export type AuthUser = {
  id: number;
  name: string;
  email: string | null;
  role: AuthRole;
  firstName: string | null;
  lastName: string | null;
  addressStreet: string | null;
  addressHouseNumber: string | null;
  addressPostalCode: string | null;
  addressCity: string | null;
  addressCountry: string | null;
  birthDate: string | null;
  avatarUrl: string | null;
  paymentMethods: string[];
  paymentDetails: Record<string, string>;
};

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  private readonly http = inject(HttpClient);
  private readonly sessionStorageKey = 'booking-system.auth.session';

  readonly user = signal<AuthUser | null>(this.readUser());
  readonly isAuthenticated = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  login(identifier: string, password: string): Observable<AuthUser> {
    return this.persistSession(this.http.post<AuthUser>('/api/auth/login', {
        identifier: identifier.trim(),
        password: password.trim()
      }, { observe: 'response' }));
  }

  register(name: string, email: string, password: string): Observable<AuthUser> {
    return this.persistSession(this.http.post<AuthUser>('/api/auth/register', {
        name: name.trim(),
        email: email.trim(),
        password: password.trim()
      }, { observe: 'response' }));
  }

  resetPassword(identifier: string, newPassword: string): Observable<void> {
    return this.http.post<void>('/api/auth/reset-password', {
      identifier: identifier.trim(),
      newPassword: newPassword.trim()
    });
  }

  logout(): void {
    this.user.set(null);
    persistAuthToken(null);

    try {
      localStorage.removeItem(this.sessionStorageKey);
    } catch {
      // Ignore storage failures so logout still clears in-memory state.
    }
  }

  landingRoute(): '/admin/tools' | '/offers' {
    return this.isAdmin() ? '/admin/tools' : '/offers';
  }

  syncUser(user: Partial<AuthUser>): void {
    const normalizedUser = this.normalizeUser(user);

    if (!normalizedUser) {
      throw new Error('User session update did not contain a valid user.');
    }

    this.persistUser(normalizedUser);
  }

  private persistSession(request: Observable<AuthUser | HttpResponse<AuthUser>>): Observable<AuthUser> {
    return request.pipe(
      tap((payload) => {
        const user = this.unwrapUser(payload);
        const normalizedUser = this.normalizeUser(user);

        if (!normalizedUser) {
          throw new Error('Login response did not contain a valid user session.');
        }

        this.persistTokenFromPayload(payload);
        this.persistUser(normalizedUser);
      }),
      map((payload) => this.unwrapUser(payload))
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

    const legacyAddress = (user as { address?: string | null }).address ?? null;

    return {
      id: user.id,
      name: user.name,
      email: user.email ?? null,
      role: user.role === 'ADMIN' ? 'ADMIN' : user.role === 'EMPLOYEE' ? 'EMPLOYEE' : 'USER',
      firstName: user.firstName ?? null,
      lastName: user.lastName ?? null,
      addressStreet: user.addressStreet ?? legacyAddress,
      addressHouseNumber: user.addressHouseNumber ?? null,
      addressPostalCode: user.addressPostalCode ?? null,
      addressCity: user.addressCity ?? null,
      addressCountry: user.addressCountry ?? null,
      birthDate: user.birthDate ?? null,
      avatarUrl: user.avatarUrl ?? null,
      paymentMethods: Array.isArray(user.paymentMethods)
        ? user.paymentMethods.filter((value): value is string => typeof value === 'string')
        : [],
      paymentDetails:
        user.paymentDetails && typeof user.paymentDetails === 'object'
          ? Object.fromEntries(
              Object.entries(user.paymentDetails).filter(
                (entry): entry is [string, string] =>
                  typeof entry[0] === 'string' &&
                  !!entry[0].trim() &&
                  typeof entry[1] === 'string' &&
                  !!entry[1].trim()
              )
            )
          : {}
    };
  }

  private unwrapUser(payload: AuthUser | HttpResponse<AuthUser>): AuthUser {
    if (this.isHttpResponse(payload)) {
      if (!payload.body) {
        throw new Error('Login response did not contain a user body.');
      }
      return payload.body;
    }
    return payload;
  }

  private persistTokenFromPayload(payload: AuthUser | HttpResponse<AuthUser>): void {
    if (this.isHttpResponse(payload)) {
      const headerToken = payload.headers.get('X-Auth-Token') ?? payload.headers.get('Authorization');
      const normalized = this.normalizeToken(headerToken);
      if (normalized) {
        persistAuthToken(normalized);
        return;
      }
    }

    const existingToken = readToken();
    if (existingToken) {
      persistAuthToken(existingToken);
    }
  }

  private normalizeToken(rawToken: string | null): string | null {
    if (!rawToken) {
      return null;
    }
    const token = rawToken.trim();
    if (!token) {
      return null;
    }
    return token.startsWith('Bearer ') ? token.slice('Bearer '.length).trim() : token;
  }

  private isHttpResponse(payload: AuthUser | HttpResponse<AuthUser>): payload is HttpResponse<AuthUser> {
    return typeof (payload as HttpResponse<AuthUser>).status === 'number';
  }
}
