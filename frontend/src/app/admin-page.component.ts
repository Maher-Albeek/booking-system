import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin, Observable } from 'rxjs';

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

type UserRole = 'USER' | 'ADMIN';

@Component({
  selector: 'app-admin-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-page.component.html',
  styleUrl: './admin-page.component.scss'
})
export class AdminPageComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly auth = inject(AuthStateService);
  protected readonly loading = signal(true);
  protected readonly busyKey = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);

  protected readonly resources = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly timeSlots = signal<TimeSlot[]>([]);
  protected readonly bookings = signal<Booking[]>([]);

  protected carDraft = {
    name: '',
    description: '',
    location: '',
    active: true
  };

  protected slotDraft = {
    resourceId: null as number | null,
    startTime: '',
    endTime: '',
    available: true
  };

  protected userDraft = {
    name: '',
    email: '',
    password: '',
    role: 'USER' as UserRole
  };

  protected readonly cars = computed(() =>
    [...this.resources()]
      .filter((resource) => resource.type.toLowerCase() === 'car')
      .sort((left, right) => left.name.localeCompare(right.name))
  );

  protected readonly orderedUsers = computed(() =>
    [...this.users()].sort((left, right) => left.name.localeCompare(right.name))
  );

  protected readonly orderedSlots = computed(() =>
    [...this.timeSlots()].sort(
      (left, right) => new Date(left.startTime).getTime() - new Date(right.startTime).getTime()
    )
  );

  protected readonly stats = computed(() => [
    {
      label: 'Cars',
      value: this.cars().length,
      note: `${this.cars().filter((car) => car.active).length} active`
    },
    {
      label: 'Time Slots',
      value: this.timeSlots().length,
      note: `${this.timeSlots().filter((slot) => slot.available).length} open`
    },
    {
      label: 'Users',
      value: this.users().length,
      note: `${this.users().filter((user) => (user.role ?? '').toUpperCase() === 'ADMIN').length} admins`
    },
    {
      label: 'Bookings',
      value: this.bookings().length,
      note: `${this.bookings().filter((booking) => booking.status === 'CONFIRMED').length} confirmed`
    }
  ]);

  constructor() {
    effect(() => {
      if (this.auth.isAdmin()) {
        this.loadData();
      } else {
        this.loading.set(false);
      }
    });
  }

  protected reload(): void {
    if (this.auth.isAdmin()) {
      this.loadData();
    }
  }

  protected isBusy(key: string): boolean {
    return this.busyKey() === key;
  }

  protected createCar(): void {
    const name = this.carDraft.name.trim();
    const location = this.carDraft.location.trim();

    if (!name || !location) {
      this.error.set('Enter a car name and location before saving.');
      return;
    }

    this.runRequest(
      'create-car',
      this.http.post<Resource>('/api/resources', {
        name,
        description: this.carDraft.description.trim(),
        type: 'Car',
        location,
        active: this.carDraft.active
      }),
      'Car added to the fleet.',
      () => {
        this.carDraft = {
          name: '',
          description: '',
          location: '',
          active: true
        };
      }
    );
  }

  protected deleteCar(carId: number): void {
    if (typeof window !== 'undefined' && !window.confirm('Delete this car from the fleet?')) {
      return;
    }

    this.runRequest(
      `delete-car-${carId}`,
      this.http.delete<void>(`/api/resources/${carId}`),
      'Car removed from the fleet.'
    );
  }

  protected createTimeSlot(): void {
    if (!this.slotDraft.resourceId || !this.slotDraft.startTime || !this.slotDraft.endTime) {
      this.error.set('Choose a car plus start and end times for the new slot.');
      return;
    }

    this.runRequest(
      'create-slot',
      this.http.post<TimeSlot>('/api/time_slots', {
        resourceId: this.slotDraft.resourceId,
        startTime: this.slotDraft.startTime,
        endTime: this.slotDraft.endTime,
        available: this.slotDraft.available
      }),
      'Time slot created.',
      () => {
        this.slotDraft = {
          resourceId: this.slotDraft.resourceId,
          startTime: '',
          endTime: '',
          available: true
        };
      }
    );
  }

  protected deleteTimeSlot(slotId: number): void {
    if (typeof window !== 'undefined' && !window.confirm('Delete this time slot?')) {
      return;
    }

    this.runRequest(
      `delete-slot-${slotId}`,
      this.http.delete<void>(`/api/time_slots/${slotId}`),
      'Time slot deleted.'
    );
  }

  protected createUser(): void {
    const name = this.userDraft.name.trim();
    const email = this.userDraft.email.trim();
    const password = this.userDraft.password.trim();

    if (!name || !email) {
      this.error.set('Enter a user name and email address.');
      return;
    }

    if (password.length < 6) {
      this.error.set('Password must be at least 6 characters long.');
      return;
    }

    this.runRequest(
      'create-user',
      this.http.post<User>('/api/users', {
        name,
        email,
        password,
        role: this.userDraft.role
      }),
      'User account created.',
      () => {
        this.userDraft = {
          name: '',
          email: '',
          password: '',
          role: 'USER'
        };
      }
    );
  }

  protected deleteUser(userId: number): void {
    if (typeof window !== 'undefined' && !window.confirm('Delete this user account?')) {
      return;
    }

    this.runRequest(
      `delete-user-${userId}`,
      this.http.delete<void>(`/api/users/${userId}`),
      'User account removed.'
    );
  }

  protected formatDate(value: string): string {
    return new Intl.DateTimeFormat('en-GB', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  protected roleLabel(role: string | null): string {
    return role ? role.toUpperCase() : 'USER';
  }

  protected resourceLabel(resourceId: number): string {
    return this.cars().find((car) => car.id === resourceId)?.name ?? `Car #${resourceId}`;
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      resources: this.http.get<Resource[]>('/api/resources'),
      users: this.http.get<User[]>('/api/users'),
      timeSlots: this.http.get<TimeSlot[]>('/api/time_slots'),
      bookings: this.http.get<Booking[]>('/api/bookings')
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ resources, users, timeSlots, bookings }) => {
          this.resources.set(resources);
          this.users.set(users);
          this.timeSlots.set(timeSlots);
          this.bookings.set(bookings);
          this.syncDraftDefaults();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(
            this.readApiError(
              error,
              'Admin data could not be loaded. Make sure the Spring Boot API is running.'
            )
          );
        }
      });
  }

  private syncDraftDefaults(): void {
    const firstCarId = this.cars()[0]?.id ?? null;
    const selectedCarStillExists = this.cars().some((car) => car.id === this.slotDraft.resourceId);

    if (!selectedCarStillExists) {
      this.slotDraft.resourceId = firstCarId;
    }
  }

  private runRequest<T>(
    key: string,
    request: Observable<T>,
    successMessage: string,
    afterSuccess?: () => void
  ): void {
    this.busyKey.set(key);
    this.error.set(null);
    this.success.set(null);

    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (this.busyKey() === key) {
            this.busyKey.set(null);
          }
        })
      )
      .subscribe({
        next: () => {
          afterSuccess?.();
          this.success.set(successMessage);
          this.loadData();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'The admin action could not be completed.'));
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
