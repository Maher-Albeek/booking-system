import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin, of } from 'rxjs';

import { AuthStateService, AuthUser } from './auth-state.service';
import { I18nService } from './i18n.service';

const PAYMENT_METHOD_OPTIONS = [
  'PayPal',
  'Master Card',
  'Visa',
  'Apple Pay',
  'Google Pay'
] as const;

type PaymentMethod = (typeof PAYMENT_METHOD_OPTIONS)[number];

const PAYMENT_METHOD_META: Record<
  PaymentMethod,
  { iconClass: string; hintKey: string; accent: string; foreground: string }
> = {
  PayPal: {
    iconClass: 'fa-brands fa-paypal',
    hintKey: 'payment.hint.walletCheckout',
    accent: '#1d4ed8',
    foreground: '#eff6ff'
  },
  'Master Card': {
    iconClass: 'fa-brands fa-cc-mastercard',
    hintKey: 'payment.hint.creditCard',
    accent: '#ea580c',
    foreground: '#fff7ed'
  },
  Visa: {
    iconClass: 'fa-brands fa-cc-visa',
    hintKey: 'payment.hint.cardPayment',
    accent: '#2563eb',
    foreground: '#eff6ff'
  },
  'Apple Pay': {
    iconClass: 'fa-brands fa-apple-pay',
    hintKey: 'payment.hint.applePay',
    accent: '#111827',
    foreground: '#f9fafb'
  },
  'Google Pay': {
    iconClass: 'fa-brands fa-google-pay',
    hintKey: 'payment.hint.googlePay',
    accent: '#0f766e',
    foreground: '#ecfeff'
  }
};

type Resource = {
  id: number;
  name: string;
  description: string;
  type: string;
  location: string;
  model: string | null;
  carType: string | null;
  color: string | null;
  year: number | null;
  seats: number | null;
  transmission: string | null;
  fuelType: string | null;
  dailyPrice: number | null;
  priceUnit: string | null;
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
  firstName: string | null;
  lastName: string | null;
  addressStreet: string | null;
  addressHouseNumber: string | null;
  addressPostalCode: string | null;
  addressCity: string | null;
  addressCountry: string | null;
  birthDate: string | null;
  avatarUrl: string | null;
  paymentMethods: PaymentMethod[];
};

type UserResponse = Omit<User, 'paymentMethods'> & {
  paymentMethods?: string[] | null;
};

type Booking = {
  id: number;
  userId: number | null;
  resourceId: number;
  startDateTime: string | null;
  endDateTime: string | null;
  status: string;
  bookingTime: string | null;
  customerName: string;
  serviceName: string | null;
  firstName: string | null;
  lastName: string | null;
  address: string | null;
  birthDate: string | null;
  paymentMethod: string | null;
};

type BookingRequest = {
  userId: number;
  resourceId: number;
  startDateTime: string;
  endDateTime: string;
  serviceName: string;
  firstName: string;
  lastName: string;
  address: string;
  birthDate: string;
  paymentMethod: PaymentMethod;
};

type CarSummary = Resource & {
  confirmedBookings: number;
};

type OfferSection = {
  id: number;
  sortOrder: number;
  title: string;
  description: string;
  imageUrl: string;
  backgroundColor: string;
  textColor: string;
  heightPx: number;
  columns: number;
  descriptionColumnGapPx: number;
  descriptionColumnDividerWidthPx: number;
  descriptionColumnDividerColor: string;
  titleFontSizePx: number;
  descriptionFontSizePx: number;
  titleXPercent: number;
  titleYPercent: number;
  descriptionXPercent: number;
  descriptionYPercent: number;
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
  selector: 'app-user-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './user-page.component.html',
  styleUrl: './user-page.component.scss'
})
export class UserPageComponent {
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);

  protected readonly auth = inject(AuthStateService);
  protected readonly i18n = inject(I18nService);
  protected readonly pageMode: 'offers' | 'bookings' =
    this.route.snapshot.data['userPageMode'] === 'bookings' ? 'bookings' : 'offers';
  protected readonly title = computed(() => this.i18n.t('user.title.bookYourNextCar'));
  protected readonly supportedPaymentMethods = PAYMENT_METHOD_OPTIONS;
  protected readonly paymentMethodMeta = PAYMENT_METHOD_META;
  protected readonly loading = signal(true);
  protected readonly submitting = signal(false);
  protected readonly accountSaving = signal(false);
  protected readonly avatarUploading = signal(false);
  protected readonly cancellingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly reservationModalOpen = signal(false);
  protected readonly carDetailsId = signal<number | null>(null);

  protected readonly cars = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly bookings = signal<Booking[]>([]);
  protected readonly profileUser = signal<User | null>(null);
  protected readonly publishedOfferSections = signal<OfferSection[]>([]);

  protected readonly selectedCarId = signal<number | null>(null);
  protected readonly selectedUserId = signal<number | null>(null);
  protected readonly bookingStartDateTime = signal('');
  protected readonly bookingEndDateTime = signal('');
  protected readonly bookingFirstName = signal('');
  protected readonly bookingLastName = signal('');
  protected readonly bookingAddress = signal('');
  protected readonly bookingBirthDate = signal('');
  protected readonly bookingPaymentMethod = signal<PaymentMethod | ''>('');
  protected readonly bookingPaypalEmail = signal('');
  protected readonly bookingCardHolderName = signal('');
  protected readonly bookingCardNumber = signal('');
  protected readonly bookingCardExpiry = signal('');
  protected readonly bookingCardCvv = signal('');
  protected readonly bookingWalletEmail = signal('');
  protected readonly serviceName = signal('');
  protected readonly searchLocationInput = signal('');
  protected readonly searchStartDateInput = signal('');
  protected readonly searchEndDateInput = signal('');
  protected readonly searchLocation = signal('');
  protected readonly searchStartDate = signal('');
  protected readonly searchEndDate = signal('');

  protected accountDraft: AccountDraft = this.emptyAccountDraft();

  protected readonly stats = computed(() => [
    {
      label: this.i18n.t('user.stats.availableCars'),
      value: this.cars().filter((car) => car.active).length,
      note: this.i18n.t('user.stats.carsLoaded', { count: this.cars().length })
    },
    {
      label: this.i18n.t('user.stats.confirmedTrips'),
      value: this.bookings().filter((booking) => booking.status === 'CONFIRMED').length,
      note: this.i18n.t('user.stats.liveReservations')
    },
    {
      label: this.i18n.t('user.stats.bookingAccess'),
      value: this.auth.isAuthenticated()
        ? this.i18n.t('user.stats.enabled')
        : this.i18n.t('app.link.login'),
      note: this.auth.isAuthenticated() ? this.i18n.t('user.stats.readyToReserve') : this.i18n.t('user.stats.requiredToReserve')
    },
    {
      label: this.auth.isAuthenticated() ? this.i18n.t('user.stats.profileReady') : this.i18n.t('user.stats.guestMode'),
      value: this.auth.isAuthenticated()
        ? `${this.accountCompleteness().completed}/${this.accountCompleteness().total}`
        : this.i18n.t('user.stats.browse'),
      note: this.auth.isAuthenticated()
        ? this.i18n.t('user.stats.profileFieldsSaved')
        : this.i18n.t('user.stats.registrationAvailable')
    }
  ]);

  protected readonly carSummaries = computed<CarSummary[]>(() => {
    const bookings = this.bookings();

    return [...this.cars()]
      .sort((left, right) => left.name.localeCompare(right.name))
      .map((car) => {
        return {
          ...car,
          confirmedBookings: bookings.filter(
            (booking) => booking.resourceId === car.id && booking.status === 'CONFIRMED'
          ).length
        };
      });
  });

  protected readonly searchDateRangeInvalid = computed(() => {
    const start = this.searchStartDateInput().trim();
    const end = this.searchEndDateInput().trim();

    if (!start || !end) {
      return false;
    }

    const startDate = new Date(`${start}T00:00:00`);
    const endDate = new Date(`${end}T00:00:00`);

    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) {
      return false;
    }

    return startDate > endDate;
  });

  protected readonly hasCompleteSearchFilters = computed(() => {
    const location = this.searchLocation().trim();
    const start = this.searchStartDate().trim();
    const end = this.searchEndDate().trim();
    return Boolean(location && start && end);
  });

  protected readonly filteredCarSummaries = computed<CarSummary[]>(() => {
    if (!this.hasCompleteSearchFilters()) {
      return [];
    }

    const locationQuery = this.searchLocation().trim().toLowerCase();
    const start = this.searchStartDate().trim();
    const end = this.searchEndDate().trim();
    const requestedStart = new Date(`${start}T00:00:00`);
    const requestedEnd = new Date(`${end}T23:59:59`);

    if (Number.isNaN(requestedStart.getTime()) || Number.isNaN(requestedEnd.getTime())) {
      return [];
    }

    return this.carSummaries().filter((car) => {
      const matchesLocation = car.location.toLowerCase().includes(locationQuery);
      const matchesDateRange = this.isCarAvailableInDateRange(car.id, requestedStart, requestedEnd);

      return matchesLocation && matchesDateRange;
    });
  });

  protected readonly selectedCar = computed(
    () => this.cars().find((car) => car.id === this.selectedCarId()) ?? null
  );

  protected readonly selectedCarPhotos = computed(() => this.selectedCar()?.photoUrls ?? []);

  protected readonly selectedCarDetails = computed(
    () => this.carSummaries().find((car) => car.id === this.carDetailsId()) ?? null
  );

  protected readonly bookingWindowPreview = computed(() => {
    const start = this.bookingStartDateTime().trim();
    const end = this.bookingEndDateTime().trim();

    if (!start || !end) {
      return null;
    }

    const startDate = new Date(start);
    const endDate = new Date(end);

    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()) || startDate >= endDate) {
      return null;
    }

    return this.formatPeriod(start, end);
  });

  protected readonly rentalDays = computed(() =>
    this.calculateRentalDays(this.bookingStartDateTime().trim(), this.bookingEndDateTime().trim())
  );

  protected readonly bookingPricePreview = computed(() => {
    const car = this.selectedCar();
    const days = this.rentalDays();

    if (!car || days === null || typeof car.dailyPrice !== 'number') {
      return null;
    }

    return {
      days,
      dailyPrice: car.dailyPrice,
      totalPrice: Number((car.dailyPrice * days).toFixed(2))
    };
  });

  protected readonly accountUser = computed(
    () => this.profileUser() ?? this.toLocalUser(this.auth.user())
  );

  protected readonly avatarInitials = computed(() => this.buildInitials(this.accountUser()));

  protected readonly accountCompleteness = computed(() => {
    const user = this.accountUser();
    const hasAddress = this.hasCompleteAddress(user);
    const completed = [
      user?.firstName,
      user?.lastName,
      hasAddress ? 'yes' : null,
      user?.birthDate,
      user?.paymentMethods.length ? 'yes' : null
    ].filter((value) => !!value).length;

    return {
      completed,
      total: 5
    };
  });

  protected readonly selectedUser = computed(() => {
    if (!this.auth.isAdmin()) {
      return this.profileUser() ?? this.toLocalUser(this.auth.user());
    }

    return this.users().find((user) => user.id === this.selectedUserId()) ?? null;
  });

  protected readonly canChooseUser = computed(
    () => this.auth.isAdmin() && this.users().length > 1
  );

  protected readonly visibleBookings = computed(() => {
    if (!this.auth.isAuthenticated()) {
      return [];
    }

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

  protected readonly bookingPaymentDetailsMissing = computed(() => {
    const paymentMethod = this.bookingPaymentMethod();

    if (!paymentMethod) {
      return true;
    }

    if (paymentMethod === 'PayPal') {
      return !this.isValidEmail(this.bookingPaypalEmail().trim());
    }

    if (paymentMethod === 'Master Card' || paymentMethod === 'Visa') {
      return (
        !this.bookingCardHolderName().trim() ||
        !this.isValidCardNumber(this.bookingCardNumber()) ||
        !this.isValidCardExpiry(this.bookingCardExpiry()) ||
        !this.isValidCardCvv(this.bookingCardCvv())
      );
    }

    if (paymentMethod === 'Apple Pay' || paymentMethod === 'Google Pay') {
      return !this.isValidEmail(this.bookingWalletEmail().trim());
    }

    return false;
  });

  protected readonly bookingDisabled = computed(
    () =>
      this.loading() ||
      this.submitting() ||
      !this.auth.isAuthenticated() ||
      this.selectedCar() === null ||
      this.selectedUser() === null ||
      !this.bookingStartDateTime().trim() ||
      !this.bookingEndDateTime().trim() ||
      !this.bookingFirstName().trim() ||
      !this.bookingLastName().trim() ||
      !this.bookingAddress().trim() ||
      !this.bookingBirthDate().trim() ||
      !this.bookingPaymentMethod() ||
      this.bookingPaymentDetailsMissing() ||
      !this.serviceName().trim()
  );

  constructor() {
    effect(() => {
      this.syncSearchFiltersFromInput();
    });

    this.loadData();
  }

  protected reload(): void {
    this.loadData();
  }

  protected selectCar(carId: number): void {
    this.selectedCarId.set(carId);
    this.success.set(null);
  }

  protected openReservationModal(carId: number): void {
    this.selectedCarId.set(carId);
    this.carDetailsId.set(null);
    this.bookingStartDateTime.set('');
    this.bookingEndDateTime.set('');
    this.resetBookingPaymentDetails();
    this.syncBookingFieldsFromUser(this.selectedUser(), this.selectedUser(), true);
    this.error.set(null);
    this.success.set(null);
    this.reservationModalOpen.set(true);
  }

  protected closeReservationModal(): void {
    this.reservationModalOpen.set(false);
  }

  protected closeReservationModalOnBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeReservationModal();
    }
  }

  protected openCarDetails(carId: number): void {
    this.carDetailsId.set(carId);
    this.success.set(null);
  }

  protected closeCarDetails(): void {
    this.carDetailsId.set(null);
  }

  protected onCarCardKeydown(event: KeyboardEvent, carId: number): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.openCarDetails(carId);
    }
  }

  protected selectUser(userId: number | null): void {
    if (!this.auth.isAdmin()) {
      return;
    }

    const previousUser = this.selectedUser();
    const nextUser = this.users().find((user) => user.id === userId) ?? null;

    this.selectedUserId.set(nextUser?.id ?? null);
    this.syncBookingFieldsFromUser(nextUser, previousUser);
  }

  protected selectBookingPaymentMethod(method: PaymentMethod): void {
    this.bookingPaymentMethod.set(method);
    this.error.set(null);
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
      this.error.set(this.i18n.t('account.error.avatarImageOnly'));
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
      this.error.set(this.i18n.t('account.error.avatarLoadFailed'));
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

  protected isAccountPaymentMethodSelected(method: PaymentMethod): boolean {
    return this.accountDraft.paymentMethods.includes(method);
  }

  protected toggleAccountPaymentMethod(method: PaymentMethod, checked: boolean): void {
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
      this.error.set(this.i18n.t('account.error.loginBeforeUpdate'));
      return;
    }

    this.accountSaving.set(true);
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
        finalize(() => this.accountSaving.set(false))
      )
      .subscribe({
        next: (user) => {
          const normalizedUser = this.normalizeUser(user);
          this.applyProfileUser(normalizedUser);
          this.success.set(this.i18n.t('account.success.updated'));
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, this.i18n.t('account.error.saveFailed')));
        }
      });
  }

  protected createBooking(): void {
    if (!this.auth.isAuthenticated()) {
      this.error.set(this.i18n.t('user.error.loginBeforeBooking'));
      return;
    }

    const selectedCar = this.selectedCar();
    const selectedUser = this.selectedUser();
    const startDateTime = this.bookingStartDateTime().trim();
    const endDateTime = this.bookingEndDateTime().trim();
    const firstName = this.bookingFirstName().trim();
    const lastName = this.bookingLastName().trim();
    const address = this.bookingAddress().trim();
    const birthDate = this.bookingBirthDate().trim();
    const paymentMethod = this.bookingPaymentMethod();
    const serviceName = this.serviceName().trim();

    if (
      !selectedCar ||
      !selectedUser ||
      !startDateTime ||
      !endDateTime ||
      !firstName ||
      !lastName ||
      !address ||
      !birthDate ||
      !paymentMethod ||
      !serviceName
    ) {
      this.error.set(this.i18n.t('user.error.completeBookingDetails'));
      return;
    }

    if (this.bookingPaymentDetailsMissing()) {
      this.error.set(this.i18n.t('user.error.completePaymentDetails'));
      return;
    }

    const startDate = new Date(startDateTime);
    const endDate = new Date(endDateTime);
    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()) || startDate >= endDate) {
      this.error.set(this.i18n.t('user.error.invalidDateRange'));
      return;
    }

    const payload: BookingRequest = {
      userId: selectedUser.id,
      resourceId: selectedCar.id,
      startDateTime,
      endDateTime,
      serviceName,
      firstName,
      lastName,
      address,
      birthDate,
      paymentMethod
    };
    const estimatedDays = this.calculateRentalDays(startDateTime, endDateTime);
    const estimatedTotalPrice =
      estimatedDays !== null && typeof selectedCar.dailyPrice === 'number'
        ? Number((selectedCar.dailyPrice * estimatedDays).toFixed(2))
        : null;

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
          this.bookingStartDateTime.set('');
          this.bookingEndDateTime.set('');
          this.serviceName.set('');
          this.resetBookingPaymentDetails();
          this.reservationModalOpen.set(false);
          let pricingNote = '';
          if (estimatedTotalPrice !== null && estimatedDays) {
            pricingNote = ` ${this.i18n.t('user.success.totalPrice', {
              total: this.formatPrice(estimatedTotalPrice, selectedCar.priceUnit),
              days: estimatedDays
            })}`;
          }
          this.success.set(this.i18n.t('user.success.bookingConfirmed', { car: selectedCar.name }) + pricingNote);
          this.loadData();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, this.i18n.t('user.error.bookingCreateFailed')));
        }
      });
  }

  protected cancelBooking(bookingId: number): void {
    if (!this.auth.isAuthenticated()) {
      this.error.set(this.i18n.t('user.error.loginBeforeManage'));
      return;
    }

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
          this.success.set(this.i18n.t('user.success.bookingCancelled'));
          this.loadData();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, this.i18n.t('user.error.bookingCancelFailed')));
        }
      });
  }

  protected resourceLabel(resourceId: number): string {
    return this.cars().find((car) => car.id === resourceId)?.name ?? this.i18n.t('user.label.carNumber', { id: resourceId });
  }

  protected bookingPeriodLabel(booking: Booking): string {
    if (booking.startDateTime && booking.endDateTime) {
      return this.formatPeriod(booking.startDateTime, booking.endDateTime);
    }

    return this.i18n.t('common.notRecorded');
  }

  protected formatDate(value: string | null | undefined): string {
    if (!value) {
      return this.i18n.t('common.notRecorded');
    }

    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  protected formatCalendarDate(value: string | null | undefined): string {
    if (!value) {
      return this.i18n.t('common.notRecorded');
    }

    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'medium'
    }).format(new Date(value));
  }

  protected formatDay(value: string): string {
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      weekday: 'short',
      day: 'numeric',
      month: 'short'
    }).format(new Date(value));
  }

  protected formatTime(value: string): string {
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value));
  }

  protected isConfirmed(booking: Booking): boolean {
    return booking.status === 'CONFIRMED';
  }

  protected hasPhotos(car: Resource | null | undefined): boolean {
    return (car?.photoUrls?.length ?? 0) > 0;
  }

  protected additionalPhotoCount(car: Resource | null | undefined): number {
    return Math.max((car?.photoUrls?.length ?? 0) - 1, 0);
  }

  protected primaryPhotoUrl(car: Resource | null | undefined): string | null {
    return car?.photoUrls?.[0] ?? null;
  }

  protected offerTextWidthPercent(xPercent: number): number {
    return Math.max(24, 95 - this.clampNumber(xPercent, 0, 90, 8));
  }

  protected applySearchFilters(): void {
    this.syncSearchFiltersFromInput();
  }

  protected clearSearchFilters(): void {
    this.searchLocationInput.set('');
    this.searchStartDateInput.set('');
    this.searchEndDateInput.set('');
    this.searchLocation.set('');
    this.searchStartDate.set('');
    this.searchEndDate.set('');
  }

  private syncSearchFiltersFromInput(): void {
    const location = this.searchLocationInput().trim();
    const start = this.searchStartDateInput().trim();
    const end = this.searchEndDateInput().trim();
    const hasInvalidDateRange = this.searchDateRangeInvalid();

    if (!location || !start || !end || hasInvalidDateRange) {
      this.searchLocation.set('');
      this.searchStartDate.set('');
      this.searchEndDate.set('');
      if (this.carDetailsId() !== null) {
        this.carDetailsId.set(null);
      }
      return;
    }

    this.searchLocation.set(location);
    this.searchStartDate.set(start);
    this.searchEndDate.set(end);
  }

  protected bookingContactName(booking: Booking): string {
    const name = [booking.firstName, booking.lastName].filter(Boolean).join(' ').trim();
    return name || booking.customerName || this.i18n.t('user.label.unnamedCustomer');
  }

  protected bookingTotalPriceLabel(booking: Booking): string {
    const car = this.cars().find((entry) => entry.id === booking.resourceId);

    if (!car || typeof car.dailyPrice !== 'number' || !booking.startDateTime || !booking.endDateTime) {
      return this.i18n.t('common.notAvailable');
    }

    const days = this.calculateRentalDays(booking.startDateTime, booking.endDateTime);
    if (days === null) {
      return this.i18n.t('common.notAvailable');
    }

    const total = Number((car.dailyPrice * days).toFixed(2));
    return this.i18n.t('user.label.totalWithDays', { total: this.formatPrice(total, car.priceUnit), days });
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    const authenticatedUser = this.auth.user();
    const usersRequest = this.auth.isAdmin()
      ? this.http.get<UserResponse[]>('/api/users')
      : of([] as UserResponse[]);
    const bookingsRequest = this.auth.isAuthenticated()
      ? this.http.get<Booking[]>('/api/bookings')
      : of([] as Booking[]);
    const profileRequest =
      authenticatedUser !== null
        ? this.http.get<UserResponse>(`/api/users/${authenticatedUser.id}`)
        : of(null);

    forkJoin({
      cars: this.http.get<ResourceResponse[]>('/api/resources/cars'),
      users: usersRequest,
      bookings: bookingsRequest,
      profile: profileRequest,
      offers: this.http.get<OfferSection[]>('/api/offers/published')
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ cars, users, bookings, profile, offers }) => {
          const normalizedCars = cars.map((car) => this.normalizeResource(car));
          const normalizedUsers = users.map((user) => this.normalizeUser(user));
          const normalizedProfile = profile ? this.normalizeUser(profile) : null;
          const normalizedOffers = offers
            .map((section, index) => this.normalizeOfferSection(section, index))
            .sort((left, right) => left.sortOrder - right.sortOrder);

          this.cars.set(normalizedCars);
          this.users.set(normalizedUsers);
          this.bookings.set(bookings);
          this.publishedOfferSections.set(normalizedOffers);

          if (normalizedProfile) {
            this.applyProfileUser(normalizedProfile);
          } else {
            this.profileUser.set(null);
            this.accountDraft = this.emptyAccountDraft();
          }

          this.syncDefaults(normalizedCars, normalizedUsers, normalizedProfile);
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(
            this.readApiError(
              error,
              this.i18n.t('user.error.apiUnavailable')
            )
          );
        }
      });
  }

  private syncDefaults(
    cars: Resource[],
    users: User[],
    profile: User | null
  ): void {
    const selectedCarId = this.selectedCarId();
    const previousUser = this.selectedUser();

    if (!cars.some((car) => car.id === selectedCarId)) {
      this.selectedCarId.set(cars[0]?.id ?? null);
    }

    if (!cars.some((car) => car.id === this.carDetailsId())) {
      this.carDetailsId.set(null);
    }

    const nextUser = this.auth.isAdmin()
      ? users.find((user) => user.id === this.selectedUserId()) ?? users[0] ?? null
      : profile ?? this.toLocalUser(this.auth.user());

    this.selectedUserId.set(nextUser?.id ?? null);
    this.syncBookingFieldsFromUser(nextUser, previousUser);

    if (!this.selectedCarId()) {
      this.reservationModalOpen.set(false);
    }
  }

  private applyProfileUser(user: User): void {
    const previousProfile = this.profileUser();

    this.profileUser.set(user);
    this.accountDraft = this.accountDraftFromUser(user);
    this.auth.syncUser(this.toAuthUser(user));

    if (!this.auth.isAdmin()) {
      this.selectedUserId.set(user.id);
      this.syncBookingFieldsFromUser(user, previousProfile, true);
    }
  }

  private syncBookingFieldsFromUser(
    nextUser: User | null,
    previousUser: User | null,
    force = false
  ): void {
    if (!nextUser) {
      return;
    }

    const previousFirstName = previousUser?.firstName ?? '';
    const previousLastName = previousUser?.lastName ?? '';
    const previousAddress = this.formattedAddress(previousUser);
    const previousBirthDate = previousUser?.birthDate ?? '';
    const previousPaymentMethod = this.preferredPaymentMethod(previousUser) ?? '';
    const nextPaymentMethod = this.preferredPaymentMethod(nextUser) ?? '';

    if (force || !this.bookingFirstName().trim() || this.bookingFirstName() === previousFirstName) {
      this.bookingFirstName.set(nextUser.firstName ?? '');
    }

    if (force || !this.bookingLastName().trim() || this.bookingLastName() === previousLastName) {
      this.bookingLastName.set(nextUser.lastName ?? '');
    }

    if (force || !this.bookingAddress().trim() || this.bookingAddress() === previousAddress) {
      this.bookingAddress.set(this.formattedAddress(nextUser));
    }

    if (force || !this.bookingBirthDate().trim() || this.bookingBirthDate() === previousBirthDate) {
      this.bookingBirthDate.set(nextUser.birthDate ?? '');
    }

    if (
      force ||
      !this.bookingPaymentMethod() ||
      this.bookingPaymentMethod() === previousPaymentMethod
    ) {
      this.bookingPaymentMethod.set(nextPaymentMethod);
    }
  }

  private emptyAccountDraft(): AccountDraft {
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

  private accountDraftFromUser(user: User | null): AccountDraft {
    if (!user) {
      return this.emptyAccountDraft();
    }

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
      paymentMethods: [...user.paymentMethods]
    };
  }

  private preferredPaymentMethod(user: User | null): PaymentMethod | '' {
    return user?.paymentMethods[0] ?? '';
  }

  private sortPaymentMethods(methods: PaymentMethod[]): PaymentMethod[] {
    return [...new Set(methods)].sort(
      (left, right) =>
        this.supportedPaymentMethods.indexOf(left) - this.supportedPaymentMethods.indexOf(right)
    );
  }

  private normalizeResource(resource: ResourceResponse): Resource {
    return {
      ...resource,
      model: resource.model ?? null,
      carType: resource.carType ?? null,
      color: this.normalizeText(resource.color),
      year: this.normalizeWholeNumber(resource.year),
      seats: this.normalizeWholeNumber(resource.seats),
      transmission: resource.transmission ?? null,
      fuelType: resource.fuelType ?? null,
      dailyPrice: this.normalizeDecimal(resource.dailyPrice),
      priceUnit: this.normalizePriceUnit(resource.priceUnit),
      baggageBags: this.normalizeWholeNumber(resource.baggageBags),
      hasAirConditioning:
        typeof resource.hasAirConditioning === 'boolean' ? resource.hasAirConditioning : null,
      horsepower: this.normalizeWholeNumber(resource.horsepower),
      photoUrls: Array.isArray(resource.photoUrls) ? resource.photoUrls : []
    };
  }

  private normalizeOfferSection(section: Partial<OfferSection>, fallbackIndex: number): OfferSection {
    return {
      id: this.normalizePositiveInt(section.id) ?? fallbackIndex + 1,
      sortOrder: this.normalizePositiveInt(section.sortOrder) ?? fallbackIndex,
      title: (section.title ?? '').trim(),
      description: (section.description ?? '').trim(),
      imageUrl: (section.imageUrl ?? '').trim(),
      backgroundColor: this.normalizeColor(section.backgroundColor, '#10243a'),
      textColor: this.normalizeColor(section.textColor, '#f7f2ea'),
      heightPx: this.normalizePositiveInt(section.heightPx) ?? 420,
      columns: this.clampNumber(this.normalizePositiveInt(section.columns), 1, 3, 1),
      descriptionColumnGapPx: this.clampNumber(this.normalizeNonNegativeInt(section.descriptionColumnGapPx), 0, 120, 24),
      descriptionColumnDividerWidthPx: this.clampNumber(
        this.normalizeNonNegativeInt(section.descriptionColumnDividerWidthPx),
        0,
        12,
        1
      ),
      descriptionColumnDividerColor: this.normalizeColor(section.descriptionColumnDividerColor, '#f7f2ea'),
      titleFontSizePx: this.clampNumber(this.normalizePositiveInt(section.titleFontSizePx), 20, 96, 38),
      descriptionFontSizePx: this.clampNumber(this.normalizePositiveInt(section.descriptionFontSizePx), 12, 52, 18),
      titleXPercent: this.clampNumber(section.titleXPercent ?? 8, 2, 80, 8),
      titleYPercent: this.clampNumber(section.titleYPercent ?? 12, 2, 78, 12),
      descriptionXPercent: this.clampNumber(section.descriptionXPercent ?? 8, 2, 80, 8),
      descriptionYPercent: this.clampNumber(section.descriptionYPercent ?? 40, 2, 88, 40)
    };
  }

  private normalizeUser(user: UserResponse): User {
    return {
      id: user.id,
      name: user.name,
      email: user.email ?? null,
      role: user.role ?? 'USER',
      firstName: user.firstName ?? null,
      lastName: user.lastName ?? null,
      addressStreet: user.addressStreet ?? null,
      addressHouseNumber: user.addressHouseNumber ?? null,
      addressPostalCode: user.addressPostalCode ?? null,
      addressCity: user.addressCity ?? null,
      addressCountry: user.addressCountry ?? null,
      birthDate: user.birthDate ?? null,
      avatarUrl: user.avatarUrl ?? null,
      paymentMethods: this.normalizePaymentMethods(user.paymentMethods)
    };
  }

  private toLocalUser(user: AuthUser | null): User | null {
    if (!user) {
      return null;
    }

    return {
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role,
      firstName: user.firstName,
      lastName: user.lastName,
      addressStreet: user.addressStreet,
      addressHouseNumber: user.addressHouseNumber,
      addressPostalCode: user.addressPostalCode,
      addressCity: user.addressCity,
      addressCountry: user.addressCountry,
      birthDate: user.birthDate,
      avatarUrl: user.avatarUrl,
      paymentMethods: this.normalizePaymentMethods(user.paymentMethods)
    };
  }

  private toAuthUser(user: User): AuthUser {
    return {
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role === 'ADMIN' ? 'ADMIN' : 'USER',
      firstName: user.firstName,
      lastName: user.lastName,
      addressStreet: user.addressStreet,
      addressHouseNumber: user.addressHouseNumber,
      addressPostalCode: user.addressPostalCode,
      addressCity: user.addressCity,
      addressCountry: user.addressCountry,
      birthDate: user.birthDate,
      avatarUrl: user.avatarUrl,
      paymentMethods: [...user.paymentMethods],
      paymentDetails: {}
    };
  }

  private normalizePaymentMethods(values: string[] | null | undefined): PaymentMethod[] {
    return this.supportedPaymentMethods.filter((method) => values?.includes(method));
  }

  private hasCompleteAddress(user: User | null): boolean {
    if (!user) {
      return false;
    }

    return Boolean(
      user.addressStreet &&
      user.addressHouseNumber &&
      user.addressPostalCode &&
      user.addressCity &&
      user.addressCountry
    );
  }

  private formattedAddress(user: User | null): string {
    if (!user) {
      return '';
    }

    const firstLine = [user.addressStreet, user.addressHouseNumber].filter(Boolean).join(' ').trim();
    const secondLine = [user.addressPostalCode, user.addressCity].filter(Boolean).join(' ').trim();
    const country = user.addressCountry?.trim() ?? '';

    return [firstLine, secondLine, country].filter(Boolean).join(', ');
  }

  private buildInitials(user: User | null): string {
    const source = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() || user?.name || 'U';

    return source
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  private formatPeriod(start: string, end: string): string {
    return `${this.formatDay(start)} | ${this.formatTime(start)} - ${this.formatTime(end)}`;
  }

  private resetBookingPaymentDetails(): void {
    this.bookingPaypalEmail.set('');
    this.bookingCardHolderName.set('');
    this.bookingCardNumber.set('');
    this.bookingCardExpiry.set('');
    this.bookingCardCvv.set('');
    this.bookingWalletEmail.set('');
  }

  protected formatPrice(value: number | null | undefined, priceUnit: string | null | undefined): string {
    if (typeof value !== 'number' || Number.isNaN(value)) {
      return this.i18n.t('common.notSet');
    }

    const normalizedUnit = this.normalizePriceUnit(priceUnit);
    const formattedValue = new Intl.NumberFormat(this.i18n.locale(), {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value);
    return `${formattedValue} ${normalizedUnit}`;
  }

  private calculateRentalDays(startValue: string, endValue: string): number | null {
    if (!startValue || !endValue) {
      return null;
    }

    const start = new Date(startValue);
    const end = new Date(endValue);

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start >= end) {
      return null;
    }

    const dayInMs = 24 * 60 * 60 * 1000;
    return Math.max(1, Math.ceil((end.getTime() - start.getTime()) / dayInMs));
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

  private normalizeText(value: string | null | undefined): string | null {
    const normalizedValue = (value ?? '').trim();
    return normalizedValue || null;
  }

  private normalizePriceUnit(value: string | null | undefined): string {
    return this.normalizeText(value) ?? '€';
  }

  private isValidEmail(value: string): boolean {
    if (!value) {
      return false;
    }

    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  private isValidCardNumber(value: string): boolean {
    const digits = this.toDigits(value);

    if (digits.length < 13 || digits.length > 19) {
      return false;
    }

    return this.passesLuhn(digits);
  }

  private isValidCardExpiry(value: string): boolean {
    const trimmedValue = value.trim();
    const match = trimmedValue.match(/^(\d{2})\s*\/\s*(\d{2}|\d{4})$/);

    if (!match) {
      return false;
    }

    const month = Number(match[1]);
    const yearPart = match[2];
    const fullYear = yearPart.length === 2 ? 2000 + Number(yearPart) : Number(yearPart);

    if (!Number.isInteger(month) || !Number.isInteger(fullYear) || month < 1 || month > 12) {
      return false;
    }

    const now = new Date();
    const expiryDate = new Date(fullYear, month, 0, 23, 59, 59, 999);
    return expiryDate.getTime() >= now.getTime();
  }

  private isValidCardCvv(value: string): boolean {
    return /^\d{3,4}$/.test(this.toDigits(value));
  }

  private toDigits(value: string): string {
    return value.replace(/\D/g, '');
  }

  private passesLuhn(value: string): boolean {
    let sum = 0;
    let shouldDouble = false;

    for (let index = value.length - 1; index >= 0; index -= 1) {
      let digit = Number(value[index]);

      if (shouldDouble) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      shouldDouble = !shouldDouble;
    }

    return sum % 10 === 0;
  }

  private normalizePositiveInt(value: unknown): number | null {
    if (typeof value !== 'number' || Number.isNaN(value) || value <= 0) {
      return null;
    }
    return Math.round(value);
  }

  private normalizeNonNegativeInt(value: unknown): number | null {
    if (typeof value !== 'number' || Number.isNaN(value) || value < 0) {
      return null;
    }
    return Math.round(value);
  }

  private normalizeColor(value: string | null | undefined, fallback: string): string {
    const normalized = (value ?? '').trim();
    if (/^#[0-9a-fA-F]{6}$/.test(normalized)) {
      return normalized;
    }
    return fallback;
  }

  private clampNumber(value: number | null, min: number, max: number, fallback: number): number {
    if (value === null || Number.isNaN(value)) {
      return fallback;
    }
    return Number(Math.max(min, Math.min(max, value)).toFixed(2));
  }

  private isCarAvailableInDateRange(resourceId: number, start: Date, end: Date): boolean {
    return !this.bookings().some((booking) => {
      if (
        booking.resourceId !== resourceId ||
        booking.status !== 'CONFIRMED' ||
        !booking.startDateTime ||
        !booking.endDateTime
      ) {
        return false;
      }

      const bookingStart = new Date(booking.startDateTime);
      const bookingEnd = new Date(booking.endDateTime);

      if (Number.isNaN(bookingStart.getTime()) || Number.isNaN(bookingEnd.getTime())) {
        return false;
      }

      return bookingStart < end && bookingEnd > start;
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

        reject(new Error(this.i18n.t('account.error.avatarReadFailed')));
      };

      reader.onerror = () => reject(reader.error ?? new Error(this.i18n.t('account.error.avatarReadFailed')));
      reader.readAsDataURL(file);
    });
  }
}
