import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin, Observable } from 'rxjs';

import { AuthStateService } from './auth-state.service';

type Resource = {
  id: number;
  name: string;
  description: string;
  type: string;
  location: string;
  model: string | null;
  carType: string | null;
  year: number | null;
  seats: number | null;
  transmission: string | null;
  fuelType: string | null;
  dailyPrice: number | null;
  baggageBags: number | null;
  hasAirConditioning: boolean | null;
  horsepower: number | null;
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

type Booking = {
  id: number;
  userId: number | null;
  resourceId: number;
  status: string;
  bookingTime: string | null;
  customerName: string;
  serviceName: string | null;
};

type UserRole = 'USER' | 'ADMIN';

@Component({
  selector: 'app-admin-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-page.component.html',
  styleUrl: './admin-page.component.scss'
})
export class AdminPageComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);

  protected readonly auth = inject(AuthStateService);
  protected readonly pageMode: 'tools' | 'offers' | 'cars' | 'users' = this.resolvePageMode();
  protected readonly loading = signal(true);
  protected readonly busyKey = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly isPhotoDragActive = signal(false);
  protected readonly editingCarId = signal<number | null>(null);
  protected readonly selectedCarId = signal<number | null>(null);

  protected readonly resources = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly bookings = signal<Booking[]>([]);

  protected carDraft = {
    name: '',
    description: '',
    location: '',
    model: '',
    carType: '',
    year: null as number | null,
    seats: null as number | null,
    transmission: 'Automatic',
    fuelType: 'Benzin',
    dailyPrice: null as number | null,
    baggageBags: null as number | null,
    hasAirConditioning: true,
    horsepower: null as number | null,
    active: true,
    photoUrlsText: '',
    uploadedPhotoUrls: [] as string[]
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

  protected readonly selectedCar = computed(() => {
    const selectedCarId = this.selectedCarId();
    if (selectedCarId === null) {
      return null;
    }

    return this.cars().find((car) => car.id === selectedCarId) ?? null;
  });

  protected readonly stats = computed(() => [
    {
      label: 'Cars',
      value: this.cars().length,
      note: `${this.cars().filter((car) => car.active).length} active`
    },
    {
      label: 'Active Cars',
      value: this.cars().filter((car) => car.active).length,
      note: `${this.cars().length} total cars`
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

  protected readonly heroTitle = computed(() => {
    if (this.pageMode === 'users') {
      return 'Manage users';
    }

    if (this.pageMode === 'offers') {
      return 'Manage offers';
    }

    if (this.pageMode === 'cars') {
      return 'Manage cars';
    }

    return 'Admin tools';
  });

  protected readonly heroDescription = computed(() => {
    if (this.pageMode === 'users') {
      return 'Create and remove user accounts from this dedicated users page.';
    }

    if (this.pageMode === 'offers') {
      return 'Maintain public offers and vehicle availability.';
    }

    if (this.pageMode === 'cars') {
      return 'Create, edit, and delete fleet cars.';
    }

    return 'Choose a dedicated admin area: offers, cars, or users.';
  });

  protected readonly showToolsPanel = computed(() => this.pageMode === 'tools');
  protected readonly showCarsPanel = computed(() => this.pageMode === 'offers' || this.pageMode === 'cars');
  protected readonly showUsersPanel = computed(() => this.pageMode === 'users');

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

    const payload = this.buildCarPayload(name, location);
    const request = editingCarId === null
      ? this.http.post<ResourceResponse>('/api/resources', payload)
      : this.http.put<ResourceResponse>(`/api/resources/${editingCarId}`, {
          id: editingCarId,
          ...payload
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
      model: car.model ?? '',
      carType: car.carType ?? '',
      year: car.year,
      seats: car.seats,
      transmission: car.transmission ?? 'Automatic',
      fuelType: car.fuelType ?? 'Benzin',
      dailyPrice: car.dailyPrice,
      baggageBags: car.baggageBags,
      hasAirConditioning: car.hasAirConditioning ?? true,
      horsepower: car.horsepower,
      active: car.active,
      photoUrlsText: '',
      uploadedPhotoUrls: [...car.photoUrls]
    };
  }

  protected openCarDetails(car: Resource): void {
    this.selectedCarId.set(car.id);
  }

  protected closeCarDetails(): void {
    this.selectedCarId.set(null);
  }

  protected onCarCardKeydown(event: KeyboardEvent, car: Resource): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.openCarDetails(car);
    }
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
        if (this.selectedCarId() === carId) {
          this.closeCarDetails();
        }

        if (this.editingCarId() === carId) {
          this.cancelCarEdit();
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

  protected roleLabel(role: string | null): string {
    return role ? role.toUpperCase() : 'USER';
  }

  protected photoCountLabel(photoUrls: string[] | null | undefined): string {
    const count = photoUrls?.length ?? 0;
    return count === 1 ? '1 photo' : `${count} photos`;
  }

  protected climateLabel(hasAirConditioning: boolean | null | undefined): string {
    if (hasAirConditioning === null || hasAirConditioning === undefined) {
      return 'Not set';
    }
    return hasAirConditioning ? 'Yes' : 'No';
  }

  protected formatCurrency(amount: number | null | undefined): string {
    if (typeof amount !== 'number' || Number.isNaN(amount)) {
      return 'Not set';
    }

    return new Intl.NumberFormat('de-DE', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  protected primaryPhotoUrl(resource: Resource): string | null {
    return resource.photoUrls[0] ?? null;
  }

  private resolvePageMode(): 'tools' | 'offers' | 'cars' | 'users' {
    const dataMode = this.route.snapshot.data['adminPageMode'];
    if (dataMode === 'offers' || dataMode === 'cars' || dataMode === 'users' || dataMode === 'tools') {
      return dataMode;
    }

    const path = this.route.snapshot.routeConfig?.path ?? '';
    if (path.includes('manage-offers')) {
      return 'offers';
    }

    if (path.includes('manage-cars')) {
      return 'cars';
    }

    if (path.includes('manage-users')) {
      return 'users';
    }

    return 'tools';
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      resources: this.http.get<ResourceResponse[]>('/api/resources'),
      users: this.http.get<User[]>('/api/users'),
      bookings: this.http.get<Booking[]>('/api/bookings')
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ resources, users, bookings }) => {
          this.resources.set(resources.map((resource) => this.normalizeResource(resource)));
          this.users.set(users);
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
    const selectedCarId = this.selectedCarId();
    const selectedCarStillExists = this.cars().some((car) => car.id === selectedCarId);

    if (selectedCarId !== null && !selectedCarStillExists) {
      this.selectedCarId.set(null);
    }
  }

  private resetCarDraft(): void {
    this.carDraft = {
      name: '',
      description: '',
      location: '',
      model: '',
      carType: '',
      year: null,
      seats: null,
      transmission: 'Automatic',
      fuelType: 'Benzin',
      dailyPrice: null,
      baggageBags: null,
      hasAirConditioning: true,
      horsepower: null,
      active: true,
      photoUrlsText: '',
      uploadedPhotoUrls: []
    };
  }

  private normalizeResource(resource: ResourceResponse): Resource {
    return {
      ...resource,
      model: resource.model ?? null,
      carType: resource.carType ?? null,
      year: this.normalizeWholeNumber(resource.year),
      seats: this.normalizeWholeNumber(resource.seats),
      transmission: resource.transmission ?? null,
      fuelType: resource.fuelType ?? null,
      dailyPrice: this.normalizeDecimal(resource.dailyPrice),
      baggageBags: this.normalizeWholeNumber(resource.baggageBags),
      hasAirConditioning:
        typeof resource.hasAirConditioning === 'boolean' ? resource.hasAirConditioning : null,
      horsepower: this.normalizeWholeNumber(resource.horsepower),
      photoUrls: Array.isArray(resource.photoUrls) ? resource.photoUrls : []
    };
  }

  private buildCarPayload(name: string, location: string): Omit<Resource, 'id'> {
    return {
      name,
      description: this.carDraft.description.trim(),
      type: 'Car',
      location,
      model: this.carDraft.model.trim() || null,
      carType: this.carDraft.carType.trim() || null,
      year: this.normalizeWholeNumber(this.carDraft.year),
      seats: this.normalizeWholeNumber(this.carDraft.seats),
      transmission: this.carDraft.transmission.trim() || null,
      fuelType: this.carDraft.fuelType.trim() || null,
      dailyPrice: this.normalizeDecimal(this.carDraft.dailyPrice),
      baggageBags: this.normalizeWholeNumber(this.carDraft.baggageBags),
      hasAirConditioning: this.carDraft.hasAirConditioning,
      horsepower: this.normalizeWholeNumber(this.carDraft.horsepower),
      active: this.carDraft.active,
      photoUrls: this.buildPhotoUrls()
    };
  }

  private normalizeWholeNumber(value: unknown): number | null {
    if (typeof value !== 'number' || Number.isNaN(value)) {
      return null;
    }

    return Math.round(value);
  }

  private normalizeDecimal(value: unknown): number | null {
    if (typeof value !== 'number' || Number.isNaN(value)) {
      return null;
    }

    return Number(value.toFixed(2));
  }

  private parsePhotoUrls(value: string): string[] {
    return value
      .split(/\r?\n|,/)
      .map((entry) => entry.trim())
      .filter(Boolean);
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
