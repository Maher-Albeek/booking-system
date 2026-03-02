import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';

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
  userId: number;
  resourceId: number;
  timeSlotId: number;
  status: string;
  bookingTime: string;
  customerName: string;
  serviceName: string;
};

type ResourceSummary = {
  id: number;
  name: string;
  description: string;
  type: string;
  location: string;
  active: boolean;
  totalSlots: number;
  availableSlots: number;
  bookings: number;
};

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly title = 'Booking System';
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly resources = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly timeSlots = signal<TimeSlot[]>([]);
  protected readonly bookings = signal<Booking[]>([]);
  protected readonly resourceSearch = signal('');
  protected readonly selectedType = signal('all');
  protected readonly selectedLocation = signal('all');
  protected readonly showActiveOnly = signal(false);
  protected readonly bookingSearch = signal('');
  protected readonly bookingStatus = signal('all');

  protected readonly stats = computed(() => {
    const resources = this.resources();
    const users = this.users();
    const timeSlots = this.timeSlots();
    const bookings = this.bookings();

    return [
      {
        label: 'Resources',
        value: resources.length,
        note: `${resources.filter((resource) => resource.active).length} active`
      },
      {
        label: 'Users',
        value: users.length,
        note: users.length ? 'Ready to book' : 'No users loaded'
      },
      {
        label: 'Open Slots',
        value: timeSlots.filter((slot) => slot.available).length,
        note: `${timeSlots.length} slots total`
      },
      {
        label: 'Bookings',
        value: bookings.length,
        note: bookings.length ? 'Live reservation data' : 'No bookings yet'
      }
    ];
  });

  protected readonly resourceTypes = computed(() =>
    [...new Set(this.resources().map((resource) => resource.type).filter(Boolean))].sort()
  );

  protected readonly resourceLocations = computed(() =>
    [...new Set(this.resources().map((resource) => resource.location).filter(Boolean))].sort()
  );

  protected readonly bookingStatuses = computed(() =>
    [...new Set(this.bookings().map((booking) => booking.status).filter(Boolean))].sort()
  );

  protected readonly resourceSummaries = computed<ResourceSummary[]>(() => {
    const slots = this.timeSlots();
    const bookings = this.bookings();

    return this.resources().map((resource) => {
      const resourceSlots = slots.filter((slot) => slot.resourceId === resource.id);
      const resourceBookings = bookings.filter((booking) => booking.resourceId === resource.id);

      return {
        id: resource.id,
        name: resource.name,
        description: resource.description,
        type: resource.type,
        location: resource.location,
        active: resource.active,
        totalSlots: resourceSlots.length,
        availableSlots: resourceSlots.filter((slot) => slot.available).length,
        bookings: resourceBookings.length
      };
    });
  });

  protected readonly filteredResourceSummaries = computed(() => {
    const search = this.resourceSearch().trim().toLowerCase();
    const selectedType = this.selectedType();
    const selectedLocation = this.selectedLocation();
    const showActiveOnly = this.showActiveOnly();

    return this.resourceSummaries().filter((resource) => {
      const matchesSearch =
        !search ||
        resource.name.toLowerCase().includes(search) ||
        resource.description.toLowerCase().includes(search) ||
        resource.location.toLowerCase().includes(search);
      const matchesType = selectedType === 'all' || resource.type === selectedType;
      const matchesLocation = selectedLocation === 'all' || resource.location === selectedLocation;
      const matchesActive = !showActiveOnly || resource.active;

      return matchesSearch && matchesType && matchesLocation && matchesActive;
    });
  });

  protected readonly recentBookings = computed(() =>
    [...this.bookings()].sort((left, right) => {
      const leftTime = left.bookingTime ? new Date(left.bookingTime).getTime() : 0;
      const rightTime = right.bookingTime ? new Date(right.bookingTime).getTime() : 0;
      return rightTime - leftTime;
    })
  );

  protected readonly filteredBookings = computed(() => {
    const search = this.bookingSearch().trim().toLowerCase();
    const status = this.bookingStatus();

    return this.recentBookings().filter((booking) => {
      const resourceName = this.resourceLabel(booking.resourceId).toLowerCase();
      const userName = this.userLabel(booking.userId).toLowerCase();
      const slot = this.slotLabel(booking.timeSlotId).toLowerCase();
      const matchesSearch =
        !search ||
        booking.customerName.toLowerCase().includes(search) ||
        booking.serviceName.toLowerCase().includes(search) ||
        resourceName.includes(search) ||
        userName.includes(search) ||
        slot.includes(search);
      const matchesStatus = status === 'all' || booking.status === status;

      return matchesSearch && matchesStatus;
    });
  });

  constructor() {
    this.loadDashboard();
  }

  protected reload(): void {
    this.loadDashboard();
  }

  protected resetResourceFilters(): void {
    this.resourceSearch.set('');
    this.selectedType.set('all');
    this.selectedLocation.set('all');
    this.showActiveOnly.set(false);
  }

  protected resetBookingFilters(): void {
    this.bookingSearch.set('');
    this.bookingStatus.set('all');
  }

  protected resourceLabel(resourceId: number): string {
    return this.resources().find((resource) => resource.id === resourceId)?.name ?? `#${resourceId}`;
  }

  protected userLabel(userId: number): string {
    return this.users().find((user) => user.id === userId)?.name ?? `#${userId}`;
  }

  protected slotLabel(timeSlotId: number): string {
    const slot = this.timeSlots().find((item) => item.id === timeSlotId);
    if (!slot) {
      return `#${timeSlotId}`;
    }

    return `${this.formatDate(slot.startTime)} -> ${this.formatDate(slot.endTime)}`;
  }

  protected formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'Not set';
    }

    return new Intl.DateTimeFormat('en-GB', {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  private loadDashboard(): void {
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
        },
        error: () => {
          this.error.set(
            'Frontend is running, but the API did not respond. Start the Spring Boot server and reload.'
          );
        }
      });
  }
}
