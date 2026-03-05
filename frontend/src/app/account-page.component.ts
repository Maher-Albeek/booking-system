import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthStateService } from './auth-state.service';

const PAYMENT_METHOD_OPTIONS = [
  'PayPal',
  'Master Card',
  'Visa',
  'Klarna',
  'Giro Card'
] as const;

type PaymentMethod = (typeof PAYMENT_METHOD_OPTIONS)[number];

const PAYMENT_METHOD_META: Record<
  PaymentMethod,
  { iconClass: string; hint: string; accent: string; foreground: string }
> = {
  PayPal: {
    iconClass: 'fa-brands fa-paypal',
    hint: 'Wallet checkout',
    accent: '#1d4ed8',
    foreground: '#eff6ff'
  },
  'Master Card': {
    iconClass: 'fa-brands fa-cc-mastercard',
    hint: 'Credit card',
    accent: '#ea580c',
    foreground: '#fff7ed'
  },
  Visa: {
    iconClass: 'fa-brands fa-cc-visa',
    hint: 'Card payment',
    accent: '#2563eb',
    foreground: '#eff6ff'
  },
  Klarna: {
    iconClass: 'fa-solid fa-money-bill-wave',
    hint: 'Pay later',
    accent: '#f472b6',
    foreground: '#500724'
  },
  'Giro Card': {
    iconClass: 'fa-solid fa-credit-card',
    hint: 'Debit card',
    accent: '#16a34a',
    foreground: '#f0fdf4'
  }
};

type UserResponse = {
  id: number;
  name: string;
  email: string | null;
  role: string | null;
  firstName: string | null;
  lastName: string | null;
  addressStreet: string | null;
  addressHouseNumber: string | null;
  addressPostalCode: string | null;
  addressCity: string | null;
  addressCountry: string | null;
  birthDate: string | null;
  avatarUrl: string | null;
  paymentMethods?: string[] | null;
};

type AccountDraft = {
  firstName: string;
  lastName: string;
  addressStreet: string;
  addressHouseNumber: string;
  addressPostalCode: string;
  addressCity: string;
  addressCountry: string;
  birthDate: string;
  avatarUrl: string;
  paymentMethods: PaymentMethod[];
};

@Component({
  selector: 'app-account-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './account-page.component.html',
  styleUrl: './account-page.component.scss'
})
export class AccountPageComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly auth = inject(AuthStateService);
  protected readonly supportedPaymentMethods = PAYMENT_METHOD_OPTIONS;
  protected readonly paymentMethodMeta = PAYMENT_METHOD_META;
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly avatarUploading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected accountDraft: AccountDraft = this.emptyDraft();

  protected readonly accountCompleteness = computed(() => {
    const hasAddress =
      !!this.accountDraft.addressStreet.trim() &&
      !!this.accountDraft.addressHouseNumber.trim() &&
      !!this.accountDraft.addressPostalCode.trim() &&
      !!this.accountDraft.addressCity.trim() &&
      !!this.accountDraft.addressCountry.trim();
    const completed = [
      this.accountDraft.firstName.trim(),
      this.accountDraft.lastName.trim(),
      hasAddress ? 'yes' : '',
      this.accountDraft.birthDate.trim(),
      this.accountDraft.paymentMethods.length ? 'yes' : ''
    ].filter(Boolean).length;

    return {
      completed,
      total: 5
    };
  });

  protected readonly avatarInitials = computed(() => {
    const source =
      `${this.accountDraft.firstName} ${this.accountDraft.lastName}`.trim() ||
      this.auth.user()?.name ||
      'U';

    return source
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  });

  constructor() {
    this.loadProfile();
  }

  protected reload(): void {
    this.loadProfile();
  }

  protected async onAvatarInputChange(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.[0] ?? null;

    if (input) {
      input.value = '';
    }

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.error.set('Only image files can be used as an avatar.');
      return;
    }

    this.avatarUploading.set(true);
    this.error.set(null);

    try {
      const avatarUrl = await this.readFileAsDataUrl(file);
      this.accountDraft = {
        ...this.accountDraft,
        avatarUrl
      };
    } catch {
      this.error.set('The selected image could not be loaded.');
    } finally {
      this.avatarUploading.set(false);
    }
  }

  protected removeAvatar(): void {
    this.accountDraft = {
      ...this.accountDraft,
      avatarUrl: ''
    };
  }

  protected isPaymentMethodSelected(method: PaymentMethod): boolean {
    return this.accountDraft.paymentMethods.includes(method);
  }

  protected togglePaymentMethod(method: PaymentMethod, checked: boolean): void {
    const nextMethods = checked
      ? [...this.accountDraft.paymentMethods, method]
      : this.accountDraft.paymentMethods.filter((entry) => entry !== method);

    this.accountDraft = {
      ...this.accountDraft,
      paymentMethods: this.sortPaymentMethods(nextMethods)
    };
  }

  protected saveAccount(): void {
    const authenticatedUser = this.auth.user();

    if (!authenticatedUser) {
      this.error.set('Login before updating your account.');
      return;
    }

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);

    this.http
      .put<UserResponse>(`/api/users/${authenticatedUser.id}`, {
        firstName: this.accountDraft.firstName.trim(),
        lastName: this.accountDraft.lastName.trim(),
        addressStreet: this.accountDraft.addressStreet.trim(),
        addressHouseNumber: this.accountDraft.addressHouseNumber.trim(),
        addressPostalCode: this.accountDraft.addressPostalCode.trim(),
        addressCity: this.accountDraft.addressCity.trim(),
        addressCountry: this.accountDraft.addressCountry.trim(),
        birthDate: this.accountDraft.birthDate.trim(),
        avatarUrl: this.accountDraft.avatarUrl.trim(),
        paymentMethods: this.accountDraft.paymentMethods
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false))
      )
      .subscribe({
        next: (user) => {
          const currentUser = this.auth.user();
          this.accountDraft = this.accountDraftFromUser(user);
          this.auth.syncUser({
            ...(currentUser ?? {}),
            ...user,
            role: user.role === 'ADMIN' ? 'ADMIN' : 'USER',
            paymentMethods: this.normalizePaymentMethods(user.paymentMethods)
          });
          this.success.set('Your account information has been updated.');
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'Account information could not be saved.'));
        }
      });
  }

  private loadProfile(): void {
    const authenticatedUser = this.auth.user();

    if (!authenticatedUser) {
      this.loading.set(false);
      this.accountDraft = this.emptyDraft();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.http
      .get<UserResponse>(`/api/users/${authenticatedUser.id}`)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (user) => {
          const currentUser = this.auth.user();
          this.accountDraft = this.accountDraftFromUser(user);
          this.auth.syncUser({
            ...(currentUser ?? {}),
            ...user,
            role: user.role === 'ADMIN' ? 'ADMIN' : 'USER',
            paymentMethods: this.normalizePaymentMethods(user.paymentMethods)
          });
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'Profile data could not be loaded.'));
        }
      });
  }

  private accountDraftFromUser(user: UserResponse): AccountDraft {
    return {
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      addressStreet: user.addressStreet ?? '',
      addressHouseNumber: user.addressHouseNumber ?? '',
      addressPostalCode: user.addressPostalCode ?? '',
      addressCity: user.addressCity ?? '',
      addressCountry: user.addressCountry ?? '',
      birthDate: user.birthDate ?? '',
      avatarUrl: user.avatarUrl ?? '',
      paymentMethods: this.normalizePaymentMethods(user.paymentMethods)
    };
  }

  private emptyDraft(): AccountDraft {
    return {
      firstName: '',
      lastName: '',
      addressStreet: '',
      addressHouseNumber: '',
      addressPostalCode: '',
      addressCity: '',
      addressCountry: '',
      birthDate: '',
      avatarUrl: '',
      paymentMethods: []
    };
  }

  private normalizePaymentMethods(values: string[] | null | undefined): PaymentMethod[] {
    return this.supportedPaymentMethods.filter((method) => values?.includes(method));
  }

  private sortPaymentMethods(methods: PaymentMethod[]): PaymentMethod[] {
    return [...new Set(methods)].sort(
      (left, right) =>
        this.supportedPaymentMethods.indexOf(left) - this.supportedPaymentMethods.indexOf(right)
    );
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

      const errors = (error.error as { errors?: Record<string, string> }).errors;
      if (errors) {
        return Object.values(errors).join(' ');
      }
    }

    return fallback;
  }

  private readFileAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = () => {
        if (typeof reader.result === 'string') {
          resolve(reader.result);
          return;
        }

        reject(new Error('Avatar could not be read.'));
      };

      reader.onerror = () => reject(reader.error ?? new Error('Avatar could not be read.'));
      reader.readAsDataURL(file);
    });
  }
}
