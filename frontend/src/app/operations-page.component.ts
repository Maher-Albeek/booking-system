import { CommonModule, DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize, Observable } from 'rxjs';

import { AuthStateService } from './auth-state.service';
import { NotificationService } from './notification.service';

type Resource = {
  id: number;
  name: string;
};

type User = {
  id: number;
  name: string;
  email: string | null;
};

type DamageReport = {
  type: string | null;
  notes: string | null;
  feeCents: number | null;
};

type BookingOperation = {
  id: number;
  userId: number | null;
  userName: string | null;
  userEmail: string | null;
  resourceId: number;
  resourceName: string;
  startDateTime: string | null;
  endDateTime: string | null;
  status: string | null;
  bookingTime: string | null;
  customerName: string | null;
  serviceName: string | null;
  paymentMethod: string | null;
  paymentStatus: string | null;
  payableAmountCents: number | null;
  payableCurrency: string | null;
  pickupOdometerKm: number | null;
  pickupFuelLevel: string | null;
  pickupNotes: string | null;
  pickupPhotoUrls: string[];
  checkedInAt: string | null;
  returnOdometerKm: number | null;
  actualReturnDateTime: string | null;
  returnNotes: string | null;
  returnPhotoUrls: string[];
  damageReports: DamageReport[];
  extraKmFeeCents: number | null;
  lateFeeCents: number | null;
  damageFeeCents: number | null;
  finalTotalAmountCents: number | null;
  finalInvoiceNumber: string | null;
  finalInvoiceIssuedAt: string | null;
};

@Component({
  selector: 'app-operations-page',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './operations-page.component.html',
  styleUrl: './operations-page.component.scss'
})
export class OperationsPageComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly auth = inject(AuthStateService);
  protected readonly notifications = inject(NotificationService);

  protected readonly loading = signal(true);
  protected readonly busyKey = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly bookings = signal<BookingOperation[]>([]);
  protected readonly resources = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly selectedBookingId = signal<number | null>(null);

  protected filters = {
    startDateTime: '',
    endDateTime: '',
    status: '',
    carId: '',
    userId: '',
    paymentStatus: ''
  };

  protected checkInDraft = {
    pickupOdometerKm: null as number | null,
    pickupFuelLevel: '',
    pickupNotes: '',
    pickupPhotoUrls: [] as string[]
  };

  protected checkOutDraft = {
    returnOdometerKm: null as number | null,
    actualReturnDateTime: '',
    returnNotes: '',
    returnPhotoUrls: [] as string[],
    damageReports: [{ type: 'SCRATCH', notes: '', feeCents: null as number | null }]
  };

  protected readonly selectedBooking = computed(() => {
    const selectedId = this.selectedBookingId();
    return this.bookings().find((booking) => booking.id === selectedId) ?? null;
  });

  constructor() {
    this.loadAll();
  }

  protected loadAll(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<Resource[]>('/api/resources')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resources) => {
          this.resources.set(resources.filter((resource) => !!resource.id));
        },
        error: () => {
          this.resources.set([]);
        }
      });

    this.http.get<User[]>('/api/users')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (users) => this.users.set(users),
        error: () => this.users.set([])
      });

    this.loadBookings();
  }

  protected loadBookings(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<BookingOperation[]>(`/api/admin/operations/bookings${this.buildQueryString()}`)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: (bookings) => {
          this.bookings.set(bookings);
          const selectedId = this.selectedBookingId();
          if (selectedId !== null && !bookings.some((booking) => booking.id === selectedId)) {
            this.selectedBookingId.set(null);
          }
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'Booking operations could not be loaded.'));
        }
      });
  }

  protected selectBooking(booking: BookingOperation): void {
    this.selectedBookingId.set(booking.id);
    this.checkInDraft = {
      pickupOdometerKm: booking.pickupOdometerKm,
      pickupFuelLevel: booking.pickupFuelLevel ?? '',
      pickupNotes: booking.pickupNotes ?? '',
      pickupPhotoUrls: [...booking.pickupPhotoUrls]
    };
    this.checkOutDraft = {
      returnOdometerKm: booking.returnOdometerKm,
      actualReturnDateTime: booking.actualReturnDateTime?.slice(0, 16) ?? '',
      returnNotes: booking.returnNotes ?? '',
      returnPhotoUrls: [...booking.returnPhotoUrls],
      damageReports: booking.damageReports.length
        ? booking.damageReports.map((report) => ({
            type: report.type ?? 'OTHER',
            notes: report.notes ?? '',
            feeCents: report.feeCents
          }))
        : [{ type: 'SCRATCH', notes: '', feeCents: null }]
    };
  }

  protected applyFilters(): void {
    this.loadBookings();
  }

  protected resetFilters(): void {
    this.filters = {
      startDateTime: '',
      endDateTime: '',
      status: '',
      carId: '',
      userId: '',
      paymentStatus: ''
    };
    this.loadBookings();
  }

  protected exportCsv(): void {
    this.busyKey.set('export');
    this.http.get(`/api/admin/operations/bookings/export${this.buildQueryString()}`, { responseType: 'blob' })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busyKey.set(null))
      )
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = 'bookings-report.csv';
          link.click();
          URL.revokeObjectURL(url);
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'CSV export failed.'));
        }
      });
  }

  protected submitCheckIn(): void {
    const booking = this.selectedBooking();
    if (!booking) {
      return;
    }
    this.runOperation(
      'check-in',
      this.http.patch<BookingOperation>(`/api/admin/operations/bookings/${booking.id}/check-in`, this.checkInDraft),
      'Check-in completed.'
    );
  }

  protected submitCheckOut(): void {
    const booking = this.selectedBooking();
    if (!booking) {
      return;
    }
    this.runOperation(
      'check-out',
      this.http.patch<BookingOperation>(`/api/admin/operations/bookings/${booking.id}/check-out`, this.checkOutDraft),
      'Check-out completed and invoice generated.'
    );
  }

  protected addDamageReport(): void {
    this.checkOutDraft = {
      ...this.checkOutDraft,
      damageReports: [...this.checkOutDraft.damageReports, { type: 'OTHER', notes: '', feeCents: null }]
    };
  }

  protected removeDamageReport(index: number): void {
    this.checkOutDraft = {
      ...this.checkOutDraft,
      damageReports: this.checkOutDraft.damageReports.filter((_, currentIndex) => currentIndex !== index)
    };
  }

  protected async onCheckInPhotoChange(event: Event): Promise<void> {
    const files = Array.from((event.target as HTMLInputElement | null)?.files ?? []);
    const encoded = await this.readFilesAsDataUrl(files);
    this.checkInDraft = {
      ...this.checkInDraft,
      pickupPhotoUrls: [...this.checkInDraft.pickupPhotoUrls, ...encoded]
    };
  }

  protected async onCheckOutPhotoChange(event: Event): Promise<void> {
    const files = Array.from((event.target as HTMLInputElement | null)?.files ?? []);
    const encoded = await this.readFilesAsDataUrl(files);
    this.checkOutDraft = {
      ...this.checkOutDraft,
      returnPhotoUrls: [...this.checkOutDraft.returnPhotoUrls, ...encoded]
    };
  }

  protected removeCheckInPhoto(index: number): void {
    this.checkInDraft = {
      ...this.checkInDraft,
      pickupPhotoUrls: this.checkInDraft.pickupPhotoUrls.filter((_, currentIndex) => currentIndex !== index)
    };
  }

  protected removeCheckOutPhoto(index: number): void {
    this.checkOutDraft = {
      ...this.checkOutDraft,
      returnPhotoUrls: this.checkOutDraft.returnPhotoUrls.filter((_, currentIndex) => currentIndex !== index)
    };
  }

  protected updateDamageReport(index: number, field: 'type' | 'notes' | 'feeCents', value: string | number | null): void {
    this.checkOutDraft = {
      ...this.checkOutDraft,
      damageReports: this.checkOutDraft.damageReports.map((report, currentIndex) =>
        currentIndex === index
          ? { ...report, [field]: field === 'feeCents' && typeof value === 'number' ? Math.round(value) : value }
          : report
      )
    };
  }

  protected formatMoney(value: number | null | undefined, currency: string | null | undefined): string {
    if (typeof value !== 'number') {
      return '-';
    }
    return `${(value / 100).toFixed(2)} ${(currency ?? 'EUR').toUpperCase()}`;
  }

  protected canCheckIn(booking: BookingOperation): boolean {
    return booking.status === 'PENDING' && this.auth.hasPermission('CHECKIN_CHECKOUT');
  }

  protected canCheckOut(booking: BookingOperation): boolean {
    return booking.status === 'ACTIVE' && this.auth.hasPermission('CHECKIN_CHECKOUT');
  }

  private buildQueryString(): string {
    const filters = this.filters;
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    const query = params.toString();
    return query ? `?${query}` : '';
  }

  private async readFilesAsDataUrl(files: File[]): Promise<string[]> {
    return Promise.all(
      files
        .filter((file) => file.type.startsWith('image/'))
        .map((file) => new Promise<string>((resolve, reject) => {
          const reader = new FileReader();
          reader.onload = () => typeof reader.result === 'string' ? resolve(reader.result) : reject(new Error('Image read failed.'));
          reader.onerror = () => reject(reader.error ?? new Error('Image read failed.'));
          reader.readAsDataURL(file);
        }))
    );
  }

  private runOperation(key: string, request: Observable<BookingOperation>, successMessage: string): void {
    this.busyKey.set(key);
    this.error.set(null);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busyKey.set(null))
      )
      .subscribe({
        next: (updatedBooking: unknown) => {
          const booking = updatedBooking as BookingOperation;
          this.notifications.success(successMessage);
          this.bookings.update((bookings) => bookings.map((item) => item.id === booking.id ? booking : item));
          this.selectBooking(booking);
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'Operation failed.'));
        }
      });
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
