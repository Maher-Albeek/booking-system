import { Injectable, computed, signal } from '@angular/core';

export type AccessRole = 'USER' | 'ADMIN';

@Injectable({ providedIn: 'root' })
export class AccessStateService {
  private readonly roleStorageKey = 'booking-system.access.role';
  private readonly aliasStorageKey = 'booking-system.access.alias';

  readonly role = signal<AccessRole>(this.readRole());
  readonly alias = signal(this.readAlias());
  readonly isAdmin = computed(() => this.role() === 'ADMIN');

  setRole(role: AccessRole): void {
    this.role.set(role);
    this.persist(this.roleStorageKey, role);
  }

  setAlias(alias: string): void {
    this.alias.set(alias);
    this.persist(this.aliasStorageKey, alias);
  }

  private readRole(): AccessRole {
    try {
      return localStorage.getItem(this.roleStorageKey) === 'ADMIN' ? 'ADMIN' : 'USER';
    } catch {
      return 'USER';
    }
  }

  private readAlias(): string {
    try {
      return localStorage.getItem(this.aliasStorageKey) ?? '';
    } catch {
      return '';
    }
  }

  private persist(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // Ignore storage failures so the UI still works in restricted environments.
    }
  }
}
