import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';

import { AuthStateService } from './auth-state.service';

type Resource = {
  id: number;
  name: string;
  description: string;
  type: string;
  location: string;
  active: boolean;
};

type User = {
  id: number;
  name: string;
  email: string | null;
  role: string | null;
};

type TimeSlot = {
  id: number;
  startTime: string;
  endTime: string;
  resourceId: number;
  available: boolean;
};

type Booking = {
  id: number;
  userId: number | null;
  resourceId: number;
  timeSlotId: number;
  status: string;
  bookingTime: string | null;
  customerName: string;
  serviceName: string | null;
};

type BookingRequest = {
  userId: number;
  resourceId: number;
  timeSlotId: number;
  customerName: string;
  serviceName: string;
};

type CarSummary = Resource & {
  totalSlots: number;
  availableSlots: number;
  confirmedBookings: number;
};

@Component({
  selector: 'app-user-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-page.component.html',
  styleUrl: './user-page.component.scss'
})
export class UserPageComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly auth = inject(AuthStateService);
  protected readonly title = 'Book Your Next Car';
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly cancellingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly cars = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly timeSlots = signal<TimeSlot[]>([]);
  protected readonly bookings = signal<Booking[]>([]);

  protected readonly selectedCarId = signal<number | null>(null);
  protected readonly selectedUserId = signal<number | null>(null);
  protected readonly selectedTimeSlotId = signal<number | null>(null);
  protected readonly customerName = signal('');
  protected readonly serviceName = signal('');

  protected readonly stats = computed(() => [
    {
      label: 'Available Cars',
      value: this.cars().filter((car) => car.active).length,
      note: `${this.cars().length} cars loaded`
    },
    {
      label: 'Open Slots',
      value: this.timeSlots().filter((slot) => slot.available).length,
      note: 'Ready to reserve'
    },
    {
      label: 'Confirmed Trips',
      value: this.bookings().filter((booking) => booking.status === 'CONFIRMED').length,
      note: 'Live reservations'
    },
    {
      label: 'Client Profiles',
      value: this.users().length,
      note: this.users().length ? 'Accounts available for booking' : 'No client records'
    }
  ]);

  protected readonly carSummaries = computed<CarSummary[]>(() => {
    const bookings = this.bookings();
    const timeSlots = this.timeSlots();

    return [...this.cars()]
      .sort((left, right) => left.name.localeCompare(right.name))
      .map((car) => {
        const slots = timeSlots.filter((slot) => slot.resourceId === car.id);
        return {
          ...car,
          totalSlots: slots.length,
          availableSlots: slots.filter((slot) => slot.available).length,
          confirmedBookings: bookings.filter(
            (booking) => booking.resourceId === car.id && booking.status === 'CONFIRMED'
          ).length
        };
      });
  });

  protected readonly selectedCar = computed(
    () => this.cars().find((car) => car.id === this.selectedCarId()) ?? null
  );

  protected readonly selectedUser = computed(
    () => this.users().find((user) => user.id === this.selectedUserId()) ?? null
  );

  protected readonly canChooseUser = computed(
    () => this.auth.isAdmin() && this.users().length > 1
  );

  protected readonly availableSlots = computed(() =>
    [...this.timeSlots()]
      .filter((slot) => slot.resourceId === this.selectedCarId() && slot.available)
      .sort(
        (left, right) =>
          new Date(left.startTime).getTime() - new Date(right.startTime).getTime()
      )
  );

  protected readonly selectedSlot = computed(
    () => this.availableSlots().find((slot) => slot.id === this.selectedTimeSlotId()) ?? null
  );

  protected readonly visibleBookings = computed(() => {
    const authenticatedUserId = this.auth.user()?.id ?? null;

    if (!this.auth.isAdmin() && authenticatedUserId !== null) {
      return [...this.bookings()]
        .filter((booking) => booking.userId === authenticatedUserId)
        .sort((left, right) => {
          const leftTime = left.bookingTime ? new Date(left.bookingTime).getTime() : 0;
          const rightTime = right.bookingTime ? new Date(right.bookingTime).getTime() : 0;
          return rightTime - leftTime;
        });
    }

    const selectedUserId = this.selectedUserId();
    const hasUserScopedBookings = this.bookings().some((booking) => booking.userId === selectedUserId);
    const relevantBookings =
      selectedUserId !== null && hasUserScopedBookings
        ? this.bookings().filter((booking) => booking.userId === selectedUserId)
        : this.bookings();

    return [...relevantBookings].sort((left, right) => {
      const leftTime = left.bookingTime ? new Date(left.bookingTime).getTime() : 0;
      const rightTime = right.bookingTime ? new Date(right.bookingTime).getTime() : 0;
      return rightTime - leftTime;
    });
  });

  protected readonly bookingDisabled = computed(
    () =>
      this.loading() ||
      this.submitting() ||
      this.selectedCar() === null ||
      this.selectedUser() === null ||
      this.selectedSlot() === null ||
      !this.customerName().trim() ||
      !this.serviceName().trim()
  );

  constructor() {
    this.loadData();
  }

  protected reload(): void {
    this.loadData();
  }

  protected selectCar(carId: number): void {
    this.selectedCarId.set(carId);
    this.selectedTimeSlotId.set(null);
    this.success.set(null);
  }

  protected selectUser(userId: number | null): void {
    if (!this.auth.isAdmin()) {
      return;
    }

    const previousUser = this.selectedUser();
    const nextUser = this.users().find((user) => user.id === userId) ?? null;

    this.selectedUserId.set(nextUser?.id ?? null);

    if (!this.customerName().trim() || this.customerName() === previousUser?.name) {
      this.customerName.set(nextUser?.name ?? '');
    }
  }

  protected chooseTimeSlot(slotId: number): void {
    this.selectedTimeSlotId.set(slotId);
    this.success.set(null);
  }

  protected createBooking(): void {
    const selectedCar = this.selectedCar();
    const selectedUser = this.selectedUser();
    const selectedSlot = this.selectedSlot();
    const customerName = this.customerName().trim();
    const serviceName = this.serviceName().trim();

    if (!selectedCar || !selectedUser || !selectedSlot || !customerName || !serviceName) {
      this.error.set('Choose a car, select a slot, and complete the booking form.');
      return;
    }

    const payload: BookingRequest = {
      userId: selectedUser.id,
      resourceId: selectedCar.id,
      timeSlotId: selectedSlot.id,
      customerName,
      serviceName
    };

    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);

    this.http
      .post<Booking>('/api/bookings', payload)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: () => {
          this.selectedTimeSlotId.set(null);
          this.serviceName.set('');
          this.success.set(`Booking confirmed for ${selectedCar.name}.`);
          this.loadData();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'Booking could not be created.'));
        }
      });
  }

  protected cancelBooking(bookingId: number): void {
    this.cancellingId.set(bookingId);
    this.error.set(null);
    this.success.set(null);

    this.http
      .patch<void>(`/api/bookings/${bookingId}/cancel`, {})
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.cancellingId.set(null))
      )
      .subscribe({
        next: () => {
          this.success.set('Booking cancelled and slot reopened.');
          this.loadData();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'Booking could not be cancelled.'));
        }
      });
  }

  protected resourceLabel(resourceId: number): string {
    return this.cars().find((car) => car.id === resourceId)?.name ?? `Car #${resourceId}`;
  }

  protected slotLabel(timeSlotId: number): string {
    const slot = this.timeSlots().find((item) => item.id === timeSlotId);
    return slot ? this.formatSlotRange(slot) : `Slot #${timeSlotId}`;
  }

  protected formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'Not recorded';
    }

    return new Intl.DateTimeFormat('en-GB', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  protected formatDay(value: string): string {
    return new Intl.DateTimeFormat('en-GB', {
      weekday: 'short',
      day: 'numeric',
      month: 'short'
    }).format(new Date(value));
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat('en-GB', {
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value));
  }

  protected formatSlotRange(slot: TimeSlot): string {
    return `${this.formatDay(slot.startTime)} | ${this.formatTime(slot.startTime)} - ${this.formatTime(slot.endTime)}`;
  }

  protected isConfirmed(booking: Booking): boolean {
    return booking.status === 'CONFIRMED';
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      cars: this.http.get<Resource[]>('/api/resources/cars'),
      users: this.http.get<User[]>('/api/users'),
      timeSlots: this.http.get<TimeSlot[]>('/api/time_slots'),
      bookings: this.http.get<Booking[]>('/api/bookings')
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ cars, users, timeSlots, bookings }) => {
          this.cars.set(cars);
          this.users.set(users);
          this.timeSlots.set(timeSlots);
          this.bookings.set(bookings);
          this.syncDefaults(cars, users, timeSlots);
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(
            this.readApiError(
              error,
              'Frontend is running, but the booking API did not respond. Start the Spring Boot server and reload.'
            )
          );
        }
      });
  }

  private syncDefaults(cars: Resource[], users: User[], timeSlots: TimeSlot[]): void {
    const selectedCarId = this.selectedCarId();
    const selectedUser = this.selectedUser();
    const authenticatedUser = this.auth.user();

    if (!cars.some((car) => car.id === selectedCarId)) {
      this.selectedCarId.set(cars[0]?.id ?? null);
    }

    const nextUser = this.auth.isAdmin()
      ? users.find((user) => user.id === this.selectedUserId()) ?? users[0] ?? null
      : users.find((user) => user.id === authenticatedUser?.id) ?? null;

    this.selectedUserId.set(nextUser?.id ?? null);

    if (
      !this.customerName().trim() ||
      this.customerName() === selectedUser?.name ||
      this.customerName() === authenticatedUser?.name
    ) {
      this.customerName.set(nextUser?.name ?? authenticatedUser?.name ?? '');
    }

    const isSelectedSlotStillValid = timeSlots.some(
      (slot) =>
        slot.id === this.selectedTimeSlotId() &&
        slot.resourceId === this.selectedCarId() &&
        slot.available
    );

    if (!isSelectedSlotStillValid) {
      this.selectedTimeSlotId.set(null);
    }
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
}
