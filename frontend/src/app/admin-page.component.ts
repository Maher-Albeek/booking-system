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
  photoUrls: string[];
};

type ResourceResponse = Omit<Resource, 'photoUrls'> & {
  photoUrls?: string[] | null;
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
  protected readonly isPhotoDragActive = signal(false);
  protected readonly editingCarId = signal<number | null>(null);
  protected readonly editingSlotId = signal<number | null>(null);

  protected readonly resources = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly timeSlots = signal<TimeSlot[]>([]);
  protected readonly bookings = signal<Booking[]>([]);

  protected carDraft = {
    name: '',
    description: '',
    location: '',
    active: true,
    photoUrlsText: '',
    uploadedPhotoUrls: [] as string[]
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

  protected saveCar(): void {
    const name = this.carDraft.name.trim();
    const location = this.carDraft.location.trim();
    const editingCarId = this.editingCarId();

    if (!name || !location) {
      this.error.set('Enter a car name and location before saving.');
      return;
    }

    const request = editingCarId === null
      ? this.http.post<ResourceResponse>('/api/resources', {
          name,
          description: this.carDraft.description.trim(),
          type: 'Car',
          location,
          active: this.carDraft.active,
          photoUrls: this.buildPhotoUrls()
        })
      : this.http.put<ResourceResponse>(`/api/resources/${editingCarId}`, {
          id: editingCarId,
          name,
          description: this.carDraft.description.trim(),
          type: 'Car',
          location,
          active: this.carDraft.active,
          photoUrls: this.buildPhotoUrls()
        });

    this.runRequest(
      'save-car',
      request,
      editingCarId === null ? 'Car added to the fleet.' : 'Car updated.',
      () => this.cancelCarEdit()
    );
  }

  protected editCar(car: Resource): void {
    this.editingCarId.set(car.id);
    this.error.set(null);
    this.success.set(null);
    this.carDraft = {
      name: car.name,
      description: car.description,
      location: car.location,
      active: car.active,
      photoUrlsText: '',
      uploadedPhotoUrls: [...car.photoUrls]
    };
  }

  protected cancelCarEdit(): void {
    this.editingCarId.set(null);
    this.resetCarDraft();
  }

  protected onPhotoDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isPhotoDragActive.set(true);
  }

  protected onPhotoDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isPhotoDragActive.set(false);
  }

  protected async onPhotoDrop(event: DragEvent): Promise<void> {
    event.preventDefault();
    this.isPhotoDragActive.set(false);

    const files = Array.from(event.dataTransfer?.files ?? []);
    await this.addPhotoFiles(files);
  }

  protected async onPhotoInputChange(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement | null;
    const files = Array.from(input?.files ?? []);
    await this.addPhotoFiles(files);

    if (input) {
      input.value = '';
    }
  }

  protected removeDraftPhoto(index: number): void {
    this.carDraft = {
      ...this.carDraft,
      uploadedPhotoUrls: this.carDraft.uploadedPhotoUrls.filter((_, currentIndex) => currentIndex !== index)
    };
  }

  protected deleteCar(carId: number): void {
    if (typeof window !== 'undefined' && !window.confirm('Delete this car from the fleet?')) {
      return;
    }

    this.runRequest(
      `delete-car-${carId}`,
      this.http.delete<void>(`/api/resources/${carId}`),
      'Car removed from the fleet.',
      () => {
        if (this.editingCarId() === carId) {
          this.cancelCarEdit();
        }
      }
    );
  }

  protected saveTimeSlot(): void {
    const editingSlotId = this.editingSlotId();

    if (!this.slotDraft.resourceId || !this.slotDraft.startTime || !this.slotDraft.endTime) {
      this.error.set('Choose a car plus start and end times for the new slot.');
      return;
    }

    const request = editingSlotId === null
      ? this.http.post<TimeSlot>('/api/time_slots', {
          resourceId: this.slotDraft.resourceId,
          startTime: this.slotDraft.startTime,
          endTime: this.slotDraft.endTime,
          available: this.slotDraft.available
        })
      : this.http.put<TimeSlot>(`/api/time_slots/${editingSlotId}`, {
          id: editingSlotId,
          resourceId: this.slotDraft.resourceId,
          startTime: this.slotDraft.startTime,
          endTime: this.slotDraft.endTime,
          available: this.slotDraft.available
        });

    this.runRequest(
      'save-slot',
      request,
      editingSlotId === null ? 'Time slot created.' : 'Time slot updated.',
      () => this.cancelTimeSlotEdit()
    );
  }

  protected editTimeSlot(slot: TimeSlot): void {
    this.editingSlotId.set(slot.id);
    this.error.set(null);
    this.success.set(null);
    this.slotDraft = {
      resourceId: slot.resourceId,
      startTime: this.toDateTimeLocalValue(slot.startTime),
      endTime: this.toDateTimeLocalValue(slot.endTime),
      available: slot.available
    };
  }

  protected cancelTimeSlotEdit(): void {
    this.editingSlotId.set(null);
    this.resetSlotDraft();
  }

  protected deleteTimeSlot(slotId: number): void {
    if (typeof window !== 'undefined' && !window.confirm('Delete this time slot?')) {
      return;
    }

    this.runRequest(
      `delete-slot-${slotId}`,
      this.http.delete<void>(`/api/time_slots/${slotId}`),
      'Time slot deleted.',
      () => {
        if (this.editingSlotId() === slotId) {
          this.cancelTimeSlotEdit();
        }
      }
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

  protected photoCountLabel(photoUrls: string[] | null | undefined): string {
    const count = photoUrls?.length ?? 0;
    return count === 1 ? '1 photo' : `${count} photos`;
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      resources: this.http.get<ResourceResponse[]>('/api/resources'),
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
          this.resources.set(resources.map((resource) => this.normalizeResource(resource)));
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

  private resetCarDraft(): void {
    this.carDraft = {
      name: '',
      description: '',
      location: '',
      active: true,
      photoUrlsText: '',
      uploadedPhotoUrls: []
    };
  }

  private resetSlotDraft(): void {
    this.slotDraft = {
      resourceId: this.cars()[0]?.id ?? null,
      startTime: '',
      endTime: '',
      available: true
    };
  }

  private normalizeResource(resource: ResourceResponse): Resource {
    return {
      ...resource,
      photoUrls: Array.isArray(resource.photoUrls) ? resource.photoUrls : []
    };
  }

  private parsePhotoUrls(value: string): string[] {
    return value
      .split(/\r?\n|,/)
      .map((entry) => entry.trim())
      .filter(Boolean);
  }

  private toDateTimeLocalValue(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const offset = date.getTimezoneOffset();
    return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 16);
  }

  private buildPhotoUrls(): string[] {
    return [...this.carDraft.uploadedPhotoUrls, ...this.parsePhotoUrls(this.carDraft.photoUrlsText)];
  }

  private async addPhotoFiles(files: File[]): Promise<void> {
    const imageFiles = files.filter((file) => file.type.startsWith('image/'));

    if (!imageFiles.length) {
      if (files.length) {
        this.error.set('Only image files can be dropped into the car photo area.');
      }
      return;
    }

    this.error.set(null);

    try {
      const encodedPhotos = await Promise.all(imageFiles.map((file) => this.convertImageToAvifDataUrl(file)));

      this.carDraft = {
        ...this.carDraft,
        uploadedPhotoUrls: [...this.carDraft.uploadedPhotoUrls, ...encodedPhotos]
      };
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : 'Images could not be converted to AVIF format.'
      );
    }
  }

  private async convertImageToAvifDataUrl(file: File): Promise<string> {
    const image = await this.loadImage(file);
    const canvas = document.createElement('canvas');
    canvas.width = image.width;
    canvas.height = image.height;

    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('Image conversion failed because the canvas context is unavailable.');
    }

    context.drawImage(image, 0, 0);

    const avifBlob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob);
            return;
          }

          reject(new Error('Your browser could not encode this image as AVIF.'));
        },
        'image/avif',
        0.82
      );
    });

    return this.readBlobAsDataUrl(avifBlob);
  }

  private loadImage(file: File): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const objectUrl = URL.createObjectURL(file);
      const image = new Image();

      image.onload = () => {
        URL.revokeObjectURL(objectUrl);
        resolve(image);
      };

      image.onerror = () => {
        URL.revokeObjectURL(objectUrl);
        reject(new Error(`"${file.name}" could not be loaded for AVIF conversion.`));
      };

      image.src = objectUrl;
    });
  }

  private readBlobAsDataUrl(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = () => {
        if (typeof reader.result === 'string') {
          resolve(reader.result);
          return;
        }

        reject(new Error('Converted AVIF image could not be read.'));
      };

      reader.onerror = () => reject(reader.error ?? new Error('Converted AVIF image could not be read.'));
      reader.readAsDataURL(blob);
    });
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
