import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, catchError, finalize, forkJoin, fromEvent, of, timeout } from 'rxjs';

import { AuthStateService, AuthUser } from './auth-state.service';
import { I18nService } from './i18n.service';
import { NotificationService } from './notification.service';

const PAYMENT_METHOD_OPTIONS = [
  'PayPal',
  'Master Card',
  'Visa',
  'Apple Pay',
  'Google Pay'
] as const;
const CAR_PAGE_SIZE = 9;

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
  kmPerDayLimit: number | null;
  extraKmFeePerKm: number | null;
  lateFeePerHour: number | null;
  depositAmount: number | null;
  maintenanceStartDateTime: string | null;
  maintenanceEndDateTime: string | null;
  maintenanceNotes: string | null;
  active: boolean;
  available: boolean;
  favorite: boolean;
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
  permissions: string[];
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
  paymentDetails: Partial<Record<PaymentMethod, string>>;
};

type UserResponse = Omit<User, 'paymentMethods'> & {
  paymentMethods?: string[] | null;
  paymentDetails?: Record<string, string> | null;
};

type Booking = {
  id: number;
  userId: number | null;
  resourceId: number;
  offerId: number | null;
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
  paymentStatus: string | null;
  payableAmountCents: number | null;
  payableCurrency: string | null;
  paymentProvider: string | null;
  cancellationRefundPercentage: number | null;
  refundedAmountCents: number | null;
  refundReason: string | null;
  depositHoldStatus: string | null;
  depositHoldAmountCents: number | null;
};

type BookingRequest = {
  userId: number;
  resourceId: number;
  offerId?: number;
  startDateTime: string;
  endDateTime: string;
  serviceName: string;
  firstName: string;
  lastName: string;
  address: string;
  birthDate: string;
  paymentMethod: PaymentMethod;
};

type CreateCheckoutSessionRequest = {
  booking: BookingRequest;
  successUrl: string;
  cancelUrl: string;
  savePaymentMethod: boolean;
  agreedToCancellationPolicy: boolean;
};

type CreateCheckoutSessionResponse = {
  bookingId: number;
  paymentStatus: string;
  checkoutSessionId: string;
  checkoutUrl: string | null;
};

type CancellationPolicyRule = {
  minimumHoursBeforePickup: number;
  refundPercentage: number;
  label: string;
};

type CancellationPolicy = {
  version: string;
  agreementText: string;
  rules: CancellationPolicyRule[];
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
  enabled: boolean;
  startDateTime: string | null;
  endDateTime: string | null;
  ctaLabel: string;
  linkedResourceIds: number[];
};

type OfferPageSettings = {
  heroBackgroundImageUrl: string;
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
  protected readonly notifications = inject(NotificationService);
  protected readonly pageMode: 'offers' | 'bookings' =
    this.route.snapshot.data['userPageMode'] === 'bookings' ? 'bookings' : 'offers';
  protected readonly title = computed(() => this.i18n.t('user.title.bookYourNextCar'));
  protected readonly supportedPaymentMethods = PAYMENT_METHOD_OPTIONS;
  protected readonly paymentMethodMeta = PAYMENT_METHOD_META;
  protected readonly loading = signal(true);
  protected readonly catalogLoading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly accountSaving = signal(false);
  protected readonly avatarUploading = signal(false);
  protected readonly cancellingId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly cancellationPolicy = signal<CancellationPolicy | null>(null);
  protected readonly reservationModalOpen = signal(false);
  protected readonly paymentDetailsModalOpen = signal(false);
  protected readonly carDetailsId = signal<number | null>(null);

  protected readonly cars = signal<Resource[]>([]);
  protected readonly catalogCars = signal<Resource[]>([]);
  protected readonly favoriteCars = signal<Resource[]>([]);
  protected readonly similarCars = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly bookings = signal<Booking[]>([]);
  protected readonly profileUser = signal<User | null>(null);
  protected readonly publishedOfferSections = signal<OfferSection[]>([]);
  protected readonly offerCarsBySectionId = signal<Record<number, Resource[]>>({});
  protected readonly heroBackgroundImageUrl = signal('');
  protected readonly selectedOfferId = signal<number | null>(null);
  protected readonly offerClock = signal(Date.now());
  protected readonly heroBackgroundStyle = computed(() => {
    const url = this.heroBackgroundImageUrl().trim();
    if (!url) {
      return '';
    }
    const escapedUrl = url.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
    return `linear-gradient(rgba(6, 12, 24, 0.62), rgba(6, 12, 24, 0.62)), url("${escapedUrl}") center / cover no-repeat`;
  });

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
  protected readonly bookingPolicyAccepted = signal(false);
  protected readonly searchLocationInput = signal('');
  protected readonly searchStartDateInput = signal('');
  protected readonly searchEndDateInput = signal('');
  protected readonly searchLocation = signal('');
  protected readonly searchStartDate = signal('');
  protected readonly searchEndDate = signal('');
  protected readonly detailFilterCarType = signal('');
  protected readonly detailFilterTransmission = signal('');
  protected readonly detailFilterFuelType = signal('');
  protected readonly detailFilterMinSeats = signal('');
  protected readonly detailFilterMaxPrice = signal('');
  protected readonly visibleCarCount = signal(CAR_PAGE_SIZE);

  protected accountDraft: AccountDraft = this.emptyAccountDraft();

  protected readonly stats = computed(() => [
    {
      label: this.i18n.t('user.stats.availableCars'),
      value: this.cars().filter((car) => car.active).length,
      note: this.i18n.t('user.stats.carsLoaded', { count: this.cars().length })
    },
    {
      label: this.i18n.t('user.stats.confirmedTrips'),
      value: this.bookings().filter((booking) => this.isBlockingBookingStatus(booking.status)).length,
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
            (booking) => booking.resourceId === car.id && this.isBlockingBookingStatus(booking.status)
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

    const startDate = new Date(start);
    const endDate = new Date(end);

    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime())) {
      return false;
    }

    return startDate >= endDate;
  });

  protected readonly hasCompleteSearchFilters = computed(() => {
    const start = this.searchStartDate().trim();
    const end = this.searchEndDate().trim();
    return Boolean(start && end);
  });

  protected readonly availableCarTypes = computed(() =>
    [...new Set(
      this.cars()
        .map((car) => (car.carType ?? car.type)?.trim())
        .filter((value): value is string => !!value)
    )]
      .sort((left, right) => left.localeCompare(right))
  );

  protected readonly availableTransmissions = computed(() =>
    [...new Set(this.cars().map((car) => car.transmission?.trim()).filter((value): value is string => !!value))]
      .sort((left, right) => left.localeCompare(right))
  );

  protected readonly availableFuelTypes = computed(() =>
    [...new Set(this.cars().map((car) => car.fuelType?.trim()).filter((value): value is string => !!value))]
      .sort((left, right) => left.localeCompare(right))
  );

  protected readonly availableSeatCounts = computed(() =>
    [...new Set(this.cars().map((car) => car.seats).filter((value): value is number => typeof value === 'number'))]
      .sort((left, right) => left - right)
  );

  protected readonly hasDetailFilters = computed(() =>
    Boolean(
      this.detailFilterCarType().trim() ||
      this.detailFilterTransmission().trim() ||
      this.detailFilterFuelType().trim() ||
      this.detailFilterMinSeats().trim() ||
      this.detailFilterMaxPrice().trim()
    )
  );

  protected readonly filteredCarSummaries = computed<CarSummary[]>(() => {
    if (!this.hasCompleteSearchFilters()) {
      return [];
    }
    const bookings = this.bookings();
    const selectedCarType = this.detailFilterCarType().trim().toLowerCase();
    const selectedTransmission = this.detailFilterTransmission().trim().toLowerCase();
    const selectedFuelType = this.detailFilterFuelType().trim().toLowerCase();
    const minSeatsRaw = this.detailFilterMinSeats().trim();
    const maxPriceRaw = this.detailFilterMaxPrice().trim();
    const minSeatsValue = Number(minSeatsRaw);
    const maxPriceValue = Number(maxPriceRaw);
    const hasMinSeatsFilter = minSeatsRaw !== '' && Number.isFinite(minSeatsValue) && minSeatsValue > 0;
    const hasMaxPriceFilter = maxPriceRaw !== '' && Number.isFinite(maxPriceValue) && maxPriceValue >= 0;

    return [...this.catalogCars()]
      .filter(
        (car) => !selectedCarType || ((car.carType ?? car.type) ?? '').trim().toLowerCase() === selectedCarType
      )
      .filter(
        (car) =>
          !selectedTransmission || (car.transmission ?? '').trim().toLowerCase() === selectedTransmission
      )
      .filter((car) => !selectedFuelType || (car.fuelType ?? '').trim().toLowerCase() === selectedFuelType)
      .filter((car) => !hasMinSeatsFilter || (car.seats ?? 0) >= minSeatsValue)
      .filter((car) => !hasMaxPriceFilter || (car.dailyPrice ?? Number.POSITIVE_INFINITY) <= maxPriceValue)
      .sort((left, right) => left.name.localeCompare(right.name))
      .map((car) => ({
        ...car,
        confirmedBookings: bookings.filter(
          (booking) => booking.resourceId === car.id && this.isBlockingBookingStatus(booking.status)
        ).length
      }));
  });
  protected readonly visibleFilteredCarSummaries = computed<CarSummary[]>(() =>
    this.filteredCarSummaries().slice(0, this.visibleCarCount())
  );
  protected readonly selectedSearchDays = computed(() =>
    this.calculateRentalDays(this.searchStartDate().trim(), this.searchEndDate().trim())
  );
  protected readonly canShowMoreCars = computed(
    () => this.filteredCarSummaries().length > this.visibleFilteredCarSummaries().length
  );

  protected readonly selectedCar = computed(
    () => this.cars().find((car) => car.id === this.selectedCarId()) ?? this.catalogCars().find((car) => car.id === this.selectedCarId()) ?? null
  );

  protected readonly selectedCarPhotos = computed(() => this.selectedCar()?.photoUrls ?? []);

  protected readonly selectedCarDetails = computed(
    () => this.filteredCarSummaries().find((car) => car.id === this.carDetailsId()) ?? this.carSummaries().find((car) => car.id === this.carDetailsId()) ?? null
  );

  protected readonly favoriteCarSummaries = computed<CarSummary[]>(() => {
    const bookings = this.bookings();
    return this.favoriteCars().map((car) => ({
      ...car,
      confirmedBookings: bookings.filter(
        (booking) => booking.resourceId === car.id && this.isBlockingBookingStatus(booking.status)
      ).length
    }));
  });

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
    return !this.bookingPaymentMethod();
  });

  protected readonly bookingDisabled = computed(
    () =>
      this.loading() ||
      this.submitting() ||
      !this.auth.isAuthenticated() ||
      this.cancellationPolicy() === null ||
      this.selectedCar() === null ||
      this.selectedUser() === null ||
      !this.bookingStartDateTime().trim() ||
      !this.bookingEndDateTime().trim() ||
      !this.bookingFirstName().trim() ||
      !this.bookingLastName().trim() ||
      !this.bookingAddress().trim() ||
      !this.bookingBirthDate().trim() ||
      !this.bookingPaymentMethod() ||
      !this.serviceName().trim() ||
      !this.bookingPolicyAccepted()
  );

  protected readonly bookingNeedsPaymentDetailsInput = computed(() => {
    return false;
  });

  constructor() {
    const offerClockInterval = window.setInterval(() => this.offerClock.set(Date.now()), 1000);
    this.destroyRef.onDestroy(() => window.clearInterval(offerClockInterval));
    let wasGuestInfoVisible = false;
    let wasProfileInfoVisible = false;

    effect(() => {
      const message = this.error();
      if (message) {
        this.notifications.error(message);
      }
    });

    effect(() => {
      const message = this.success();
      if (message) {
        this.notifications.success(message);
      }
    });

    effect(() => {
      const isAuthenticated = this.auth.isAuthenticated();
      const completeness = this.accountCompleteness();
      const guestInfoVisible = !isAuthenticated;
      const profileInfoVisible = isAuthenticated && completeness.completed < completeness.total;

      if (guestInfoVisible && !wasGuestInfoVisible) {
        this.notifications.info(this.i18n.t('user.banner.guestAccessText'));
      }

      if (profileInfoVisible && !wasProfileInfoVisible) {
        const missingFieldsMessage = this.profileIncompleteMessage();
        if (missingFieldsMessage) {
          this.notifications.info(missingFieldsMessage);
        }
      }

      wasGuestInfoVisible = guestInfoVisible;
      wasProfileInfoVisible = profileInfoVisible;
    });

    fromEvent<CustomEvent<{ carId: number; favorite: boolean }>>(window, 'favorites-updated')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => {
        const detail = event.detail;
        if (!detail) {
          return;
        }

        this.cars.update((cars) =>
          cars.map((car) => (car.id === detail.carId ? { ...car, favorite: detail.favorite } : car))
        );
        this.catalogCars.update((cars) =>
          cars.map((car) => (car.id === detail.carId ? { ...car, favorite: detail.favorite } : car))
        );
        this.similarCars.update((cars) =>
          cars.map((car) => (car.id === detail.carId ? { ...car, favorite: detail.favorite } : car))
        );
        this.favoriteCars.update((cars) =>
          detail.favorite ? cars : cars.filter((car) => car.id !== detail.carId)
        );
      });

    this.loadData();
  }

  protected reload(): void {
    this.loadData();
  }

  protected toggleFavorite(car: Resource): void {
    const authenticatedUser = this.auth.user();
    if (!authenticatedUser) {
      this.notifications.info(this.i18n.t('user.favorite.loginRequired'));
      return;
    }

    const nextFavoriteState = !car.favorite;

    const request = car.favorite
      ? this.http.delete<ResourceResponse>(`/api/resources/${car.id}/favorites/${authenticatedUser.id}`)
      : this.http.post<ResourceResponse>(`/api/resources/${car.id}/favorites`, { userId: authenticatedUser.id });

    request
      .pipe(takeUntilDestroyed(this.destroyRef), timeout(10000))
      .subscribe({
        next: (resource) => {
          const normalized = {
            ...this.normalizeResource(resource),
            favorite: nextFavoriteState
          };
          this.replaceCatalogResource(normalized);
          this.refreshFavoriteCars();
          this.refreshSimilarCars();
          window.dispatchEvent(
            new CustomEvent('favorites-updated', {
              detail: { carId: normalized.id, favorite: normalized.favorite }
            })
          );
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, this.i18n.t('user.favorite.updateFailed')));
        }
      });
  }

  protected selectCar(carId: number): void {
    this.selectedCarId.set(carId);
    this.success.set(null);
  }

  protected openReservationModal(carId: number): void {
    this.openReservationModalForOffer(carId, null);
  }

  protected openOfferReservationModal(offerId: number, carId: number): void {
    this.openReservationModalForOffer(carId, offerId);
  }

  protected offerCars(offerId: number): Resource[] {
    return this.offerCarsBySectionId()[offerId] ?? [];
  }

  protected offerCountdownLabel(section: OfferSection): string | null {
    const endValue = section.endDateTime?.trim();
    if (!endValue) {
      return null;
    }

    const endDate = new Date(endValue);
    const remainingMs = endDate.getTime() - this.offerClock();
    if (Number.isNaN(endDate.getTime()) || remainingMs <= 0) {
      return 'Offer ended';
    }

    const totalSeconds = Math.floor(remainingMs / 1000);
    const days = Math.floor(totalSeconds / 86400);
    const hours = Math.floor((totalSeconds % 86400) / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    if (days > 0) {
      return `${days}d ${hours}h ${minutes}m left`;
    }
    return `${hours}h ${minutes}m ${seconds}s left`;
  }

  protected offerScheduleLabel(section: OfferSection): string | null {
    const start = section.startDateTime?.trim();
    const end = section.endDateTime?.trim();
    if (start && end) {
      return `${this.formatDay(start)} - ${this.formatDay(end)}`;
    }
    if (start) {
      return `Starts ${this.formatDay(start)}`;
    }
    if (end) {
      return `Ends ${this.formatDay(end)}`;
    }
    return null;
  }

  private openReservationModalForOffer(carId: number, offerId: number | null): void {
    this.selectedCarId.set(carId);
    this.selectedOfferId.set(offerId);
    this.carDetailsId.set(null);
    this.bookingStartDateTime.set(this.searchStartDate().trim());
    this.bookingEndDateTime.set(this.searchEndDate().trim());
    this.resetBookingPaymentDetails();
    this.bookingPolicyAccepted.set(false);
    this.syncBookingFieldsFromUser(this.selectedUser(), this.selectedUser(), true);
    this.error.set(null);
    this.success.set(null);
    this.updatePaymentDetailsModalState();
    this.reservationModalOpen.set(true);
  }

  protected closeReservationModal(): void {
    this.reservationModalOpen.set(false);
    this.paymentDetailsModalOpen.set(false);
    this.selectedOfferId.set(null);
  }

  protected closeReservationModalOnBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeReservationModal();
    }
  }

  protected openCarDetails(carId: number): void {
    this.carDetailsId.set(carId);
    this.success.set(null);
    this.refreshSimilarCars();
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
    this.updatePaymentDetailsModalState();
  }

  protected closePaymentDetailsModal(): void {
    this.paymentDetailsModalOpen.set(false);
  }

  protected selectedUserHasStoredPaymentDetails(method: PaymentMethod): boolean {
    const value = this.selectedUser()?.paymentDetails?.[method];
    if (typeof value !== 'string' || !value.trim()) {
      return false;
    }

    if (method === 'Master Card' || method === 'Visa') {
      const details = this.decodeCardDetails(value);
      return (
        !!details.name.trim() &&
        this.isValidCardNumber(details.number) &&
        this.isValidCardExpiry(details.expiry) &&
        this.isValidCardCvv(details.cvv)
      );
    }

    if (method === 'PayPal' || method === 'Apple Pay' || method === 'Google Pay') {
      return this.isValidEmail(value.trim());
    }

    return false;
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
        timeout(10000),
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
    if (!this.cancellationPolicy()) {
      this.error.set('Cancellation policy is currently unavailable. Please refresh and try again.');
      return;
    }
    if (!this.bookingPolicyAccepted()) {
      this.error.set('Please agree to the cancellation policy before payment.');
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

    const startDate = new Date(startDateTime);
    const endDate = new Date(endDateTime);
    if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()) || startDate >= endDate) {
      this.error.set(this.i18n.t('user.error.invalidDateRange'));
      return;
    }

    const payload: BookingRequest = {
      userId: selectedUser.id,
      resourceId: selectedCar.id,
      ...(this.selectedOfferId() !== null ? { offerId: this.selectedOfferId()! } : {}),
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

    const checkoutPayload: CreateCheckoutSessionRequest = {
      booking: payload,
      successUrl: `${window.location.origin}/bookings`,
      cancelUrl: `${window.location.origin}/offers`,
      savePaymentMethod: true,
      agreedToCancellationPolicy: this.bookingPolicyAccepted()
    };
    const idempotencyKey =
      typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
        ? crypto.randomUUID()
        : `${Date.now()}-${selectedUser.id}-${selectedCar.id}`;

    this.http
      .post<CreateCheckoutSessionResponse>('/api/payments/checkout-session', checkoutPayload, {
        headers: {
          'Idempotency-Key': idempotencyKey
        }
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false))
      )
      .subscribe({
        next: (response) => {
          if (response.checkoutUrl) {
            window.location.href = response.checkoutUrl;
            return;
          }
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
      .patch<Booking>(`/api/bookings/${bookingId}/cancel`, {})
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.cancellingId.set(null))
      )
      .subscribe({
        next: (booking) => {
          this.success.set(this.buildCancellationMessage(booking));
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

  protected canCancelBooking(booking: Booking): boolean {
    return booking.status === 'PENDING' || booking.status === 'ACTIVE';
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
    this.error.set(null);
    this.success.set(null);
    this.syncSearchFiltersFromInput();
    this.clearDetailFilters();
    this.fetchCatalogCars();
  }

  protected clearSearchFilters(): void {
    this.searchLocationInput.set('');
    this.searchStartDateInput.set('');
    this.searchEndDateInput.set('');
    this.searchLocation.set('');
    this.searchStartDate.set('');
    this.searchEndDate.set('');
    this.error.set(null);
    this.visibleCarCount.set(CAR_PAGE_SIZE);
  }

  protected showMoreCars(): void {
    this.visibleCarCount.update((count) => count + CAR_PAGE_SIZE);
  }

  protected clearDetailFilters(): void {
    this.detailFilterCarType.set('');
    this.detailFilterTransmission.set('');
    this.detailFilterFuelType.set('');
    this.detailFilterMinSeats.set('');
    this.detailFilterMaxPrice.set('');
    this.visibleCarCount.set(CAR_PAGE_SIZE);
  }

  private syncSearchFiltersFromInput(): void {
    const location = this.searchLocationInput().trim();
    const start = this.searchStartDateInput().trim();
    const end = this.searchEndDateInput().trim();
    const hasInvalidDateRange = this.searchDateRangeInvalid();

    if (!start || !end || hasInvalidDateRange) {
      this.searchLocation.set('');
      this.searchStartDate.set('');
      this.searchEndDate.set('');
      this.catalogCars.set([]);
      this.visibleCarCount.set(CAR_PAGE_SIZE);
      return;
    }

    this.searchLocation.set(location);
    this.searchStartDate.set(start);
    this.searchEndDate.set(end);
    this.visibleCarCount.set(CAR_PAGE_SIZE);
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
      ? this.http.get<Booking[]>('/api/bookings').pipe(catchError(() => of([] as Booking[])))
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
      cancellationPolicy: this.http
        .get<CancellationPolicy>('/api/payments/cancellation-policy')
        .pipe(catchError(() => of(null))),
      offers: this.http.get<OfferSection[]>('/api/offers/live'),
      offerSettings: this.http
        .get<OfferPageSettings>('/api/offers/settings/published')
        .pipe(catchError(() => of({ heroBackgroundImageUrl: '' } as OfferPageSettings)))
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        timeout(10000),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({ cars, users, bookings, profile, cancellationPolicy, offers, offerSettings }) => {
          const normalizedCars = cars.map((car) => this.normalizeResource(car));
          const normalizedUsers = users.map((user) => this.normalizeUser(user));
          const normalizedProfile = profile ? this.normalizeUser(profile) : null;
          const normalizedOffers = offers
            .map((section, index) => this.normalizeOfferSection(section, index))
            .sort((left, right) => left.sortOrder - right.sortOrder);

          this.cars.set(normalizedCars);
          this.catalogCars.set([]);
          this.users.set(normalizedUsers);
          this.bookings.set(bookings);
          this.cancellationPolicy.set(cancellationPolicy);
          this.publishedOfferSections.set(normalizedOffers);
          this.loadOfferCars(normalizedOffers);
          this.heroBackgroundImageUrl.set((offerSettings?.heroBackgroundImageUrl ?? '').trim());

          if (normalizedProfile) {
            this.applyProfileUser(normalizedProfile);
          } else {
            this.profileUser.set(null);
            this.accountDraft = this.emptyAccountDraft();
          }

          this.syncDefaults(normalizedCars, normalizedUsers, normalizedProfile);
          this.refreshFavoriteCars();
          if (this.hasCompleteSearchFilters()) {
            this.fetchCatalogCars();
          }
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

    this.updatePaymentDetailsModalState();
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
      kmPerDayLimit: this.normalizeWholeNumber(resource.kmPerDayLimit),
      extraKmFeePerKm: this.normalizeDecimal(resource.extraKmFeePerKm),
      lateFeePerHour: this.normalizeDecimal(resource.lateFeePerHour),
      depositAmount: this.normalizeDecimal(resource.depositAmount),
      maintenanceStartDateTime: resource.maintenanceStartDateTime ?? null,
      maintenanceEndDateTime: resource.maintenanceEndDateTime ?? null,
      maintenanceNotes: this.normalizeText(resource.maintenanceNotes),
      available: typeof resource.available === 'boolean' ? resource.available : resource.active,
      favorite: typeof resource.favorite === 'boolean' ? resource.favorite : false,
      photoUrls: Array.isArray(resource.photoUrls) ? resource.photoUrls : []
    };
  }

  private fetchCatalogCars(): void {
    if (!this.hasCompleteSearchFilters()) {
      this.catalogCars.set([]);
      this.catalogLoading.set(false);
      return;
    }

    const params = new URLSearchParams();
    const location = this.searchLocation().trim();
    if (location) {
      params.set('location', location);
    }
    const start = this.searchStartDate().trim();
    const end = this.searchEndDate().trim();
    params.set('pickupDateTime', start);
    params.set('returnDateTime', end);
    if (this.auth.user()?.id) {
      params.set('userId', String(this.auth.user()!.id));
    }

    this.catalogLoading.set(true);

    this.http
      .get<ResourceResponse[]>(`/api/resources/catalog?${params.toString()}`)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        timeout(10000),
        finalize(() => this.catalogLoading.set(false))
      )
      .subscribe({
        next: (resources) => {
          this.error.set(null);
          this.catalogCars.set(resources.map((resource) => this.normalizeResource(resource)));
          this.refreshSimilarCars();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, this.i18n.t('user.error.loadCatalogFailed')));
        }
      });
  }

  private refreshFavoriteCars(): void {
    const authenticatedUser = this.auth.user();
    if (!authenticatedUser) {
      this.favoriteCars.set([]);
      return;
    }

    this.http
      .get<ResourceResponse[]>(`/api/resources/favorites?userId=${authenticatedUser.id}`)
      .pipe(takeUntilDestroyed(this.destroyRef), timeout(10000), catchError(() => of([] as ResourceResponse[])))
      .subscribe((resources) => {
        this.favoriteCars.set(resources.map((resource) => this.normalizeResource(resource)));
      });
  }

  private refreshSimilarCars(): void {
    const selectedCarId = this.carDetailsId();
    if (!selectedCarId) {
      this.similarCars.set([]);
      return;
    }

    const params = new URLSearchParams();
    if (this.searchStartDate().trim()) {
      params.set('pickupDateTime', this.searchStartDate().trim());
    }
    if (this.searchEndDate().trim()) {
      params.set('returnDateTime', this.searchEndDate().trim());
    }
    if (this.auth.user()?.id) {
      params.set('userId', String(this.auth.user()!.id));
    }

    this.http
      .get<ResourceResponse[]>(`/api/resources/${selectedCarId}/similar?${params.toString()}`)
      .pipe(takeUntilDestroyed(this.destroyRef), timeout(10000), catchError(() => of([] as ResourceResponse[])))
      .subscribe((resources) => {
        this.similarCars.set(resources.map((resource) => this.normalizeResource(resource)));
      });
  }

  private replaceCatalogResource(resource: Resource): void {
    this.cars.update((cars) => cars.map((entry) => (entry.id === resource.id ? { ...entry, ...resource } : entry)));
    this.catalogCars.update((cars) => cars.map((entry) => (entry.id === resource.id ? { ...entry, ...resource } : entry)));
    this.similarCars.update((cars) => cars.map((entry) => (entry.id === resource.id ? { ...entry, ...resource } : entry)));
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
      descriptionYPercent: this.clampNumber(section.descriptionYPercent ?? 40, 2, 88, 40),
      enabled: section.enabled !== false,
      startDateTime: this.normalizeText(section.startDateTime) ?? null,
      endDateTime: this.normalizeText(section.endDateTime) ?? null,
      ctaLabel: (section.ctaLabel ?? '').trim() || 'Book now',
      linkedResourceIds: Array.isArray(section.linkedResourceIds)
        ? section.linkedResourceIds
            .filter((value): value is number => typeof value === 'number' && Number.isFinite(value) && value > 0)
            .map((value) => Math.round(value))
        : []
    };
  }

  private loadOfferCars(sections: OfferSection[]): void {
    if (!sections.length) {
      this.offerCarsBySectionId.set({});
      return;
    }

    const requests = sections.reduce<Record<string, Observable<ResourceResponse[]>>>((acc, section) => {
      acc[String(section.id)] = this.http
        .get<ResourceResponse[]>(`/api/offers/${section.id}/cars`)
        .pipe(catchError(() => of([] as ResourceResponse[])));
      return acc;
    }, {});

    forkJoin(requests)
      .pipe(takeUntilDestroyed(this.destroyRef), timeout(10000))
      .subscribe((responseMap) => {
        const mapped: Record<number, Resource[]> = {};
        for (const [sectionId, resources] of Object.entries(responseMap)) {
          mapped[Number(sectionId)] = (resources as ResourceResponse[]).map((resource) => this.normalizeResource(resource));
        }
        this.offerCarsBySectionId.set(mapped);
      });
  }

  private normalizeUser(user: UserResponse): User {
    return {
      id: user.id,
      name: user.name,
      email: user.email ?? null,
      role: user.role ?? 'CUSTOMER',
      permissions: Array.isArray((user as { permissions?: unknown[] }).permissions)
        ? (user as { permissions?: unknown[] }).permissions!.filter((value): value is string => typeof value === 'string')
        : [],
      firstName: user.firstName ?? null,
      lastName: user.lastName ?? null,
      addressStreet: user.addressStreet ?? null,
      addressHouseNumber: user.addressHouseNumber ?? null,
      addressPostalCode: user.addressPostalCode ?? null,
      addressCity: user.addressCity ?? null,
      addressCountry: user.addressCountry ?? null,
      birthDate: user.birthDate ?? null,
      avatarUrl: user.avatarUrl ?? null,
      paymentMethods: this.normalizePaymentMethods(user.paymentMethods),
      paymentDetails: this.normalizePaymentDetails(user.paymentDetails, user.paymentMethods)
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
      permissions: [...user.permissions],
      paymentMethods: this.normalizePaymentMethods(user.paymentMethods),
      paymentDetails: this.normalizePaymentDetails(user.paymentDetails, user.paymentMethods)
    };
  }

  private toAuthUser(user: User): AuthUser {
    return {
      id: user.id,
      name: user.name,
      email: user.email,
      role: user.role === 'ADMIN' ? 'ADMIN' : user.role === 'EMPLOYEE' ? 'EMPLOYEE' : 'CUSTOMER',
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
      paymentDetails: this.normalizedPaymentDetailsForAuth(user.paymentDetails),
      permissions: [...user.permissions]
    };
  }

  private normalizePaymentMethods(values: string[] | null | undefined): PaymentMethod[] {
    return this.supportedPaymentMethods.filter((method) => values?.includes(method));
  }

  private normalizePaymentDetails(
    details: Record<string, string> | null | undefined,
    methods: string[] | null | undefined
  ): Partial<Record<PaymentMethod, string>> {
    const selectedMethods = new Set(this.normalizePaymentMethods(methods));
    const normalized: Partial<Record<PaymentMethod, string>> = {};

    for (const method of this.supportedPaymentMethods) {
      if (!selectedMethods.has(method)) {
        continue;
      }

      const value = details?.[method];
      if (typeof value === 'string' && value.trim()) {
        normalized[method] = value.trim();
      }
    }

    return normalized;
  }

  private normalizedPaymentDetailsForAuth(
    details: Partial<Record<PaymentMethod, string>>
  ): Record<string, string> {
    const mapped: Record<string, string> = {};
    for (const method of this.supportedPaymentMethods) {
      const value = details[method]?.trim();
      if (value) {
        mapped[method] = value;
      }
    }

    return mapped;
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

  private profileIncompleteMessage(): string {
    const missingFields = this.missingProfileFieldLabels(this.accountUser());
    if (!missingFields.length) {
      return '';
    }

    return `${this.i18n.t('user.banner.profileIncompleteText')} (${missingFields.join(', ')})`;
  }

  private missingProfileFieldLabels(user: User | null): string[] {
    if (!user) {
      return [];
    }

    const missing: string[] = [];

    if (!(user.firstName ?? '').trim()) {
      missing.push(this.i18n.t('account.field.firstName'));
    }

    if (!(user.lastName ?? '').trim()) {
      missing.push(this.i18n.t('account.field.lastName'));
    }

    if (!(user.addressStreet ?? '').trim()) {
      missing.push(this.i18n.t('account.field.street'));
    }

    if (!(user.addressHouseNumber ?? '').trim()) {
      missing.push(this.i18n.t('account.field.houseNumber'));
    }

    if (!(user.addressPostalCode ?? '').trim()) {
      missing.push(this.i18n.t('account.field.postalCode'));
    }

    if (!(user.addressCity ?? '').trim()) {
      missing.push(this.i18n.t('account.field.city'));
    }

    if (!(user.addressCountry ?? '').trim()) {
      missing.push(this.i18n.t('account.field.country'));
    }

    if (!(user.birthDate ?? '').trim()) {
      missing.push(this.i18n.t('account.field.birthDate'));
    }

    if (!user.paymentMethods.length) {
      missing.push(this.i18n.t('account.field.paymentMethods'));
    }

    return missing;
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

  protected formatPeriod(start: string, end: string): string {
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

  private updatePaymentDetailsModalState(): void {
    this.paymentDetailsModalOpen.set(false);
  }

  private decodeCardDetails(rawValue: string | undefined): {
    name: string;
    number: string;
    expiry: string;
    cvv: string;
  } {
    if (!rawValue?.trim()) {
      return { name: '', number: '', expiry: '', cvv: '' };
    }

    try {
      const parsed = JSON.parse(rawValue) as Partial<{
        name: string;
        number: string;
        expiry: string;
        cvv: string;
      }>;

      return {
        name: typeof parsed.name === 'string' ? parsed.name : '',
        number: typeof parsed.number === 'string' ? parsed.number : '',
        expiry: typeof parsed.expiry === 'string' ? parsed.expiry : '',
        cvv: typeof parsed.cvv === 'string' ? parsed.cvv : ''
      };
    } catch {
      return {
        name: '',
        number: rawValue,
        expiry: '',
        cvv: ''
      };
    }
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

  protected formatMoneyFromCents(value: number | null | undefined, currency: string | null | undefined): string {
    if (typeof value !== 'number' || Number.isNaN(value)) {
      return this.i18n.t('common.notSet');
    }

    return new Intl.NumberFormat(this.i18n.locale(), {
      style: 'currency',
      currency: (currency ?? 'EUR').trim().toUpperCase(),
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(value / 100);
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

  protected totalCarPriceForSearch(car: Resource): number | null {
    if (typeof car.dailyPrice !== 'number') {
      return null;
    }

    const days = this.selectedSearchDays();
    if (days === null) {
      return car.dailyPrice;
    }

    return Number((car.dailyPrice * days).toFixed(2));
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
        !this.isBlockingBookingStatus(booking.status) ||
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

  private isBlockingBookingStatus(status: string): boolean {
    return status === 'PENDING' || status === 'ACTIVE';
  }

  private readApiError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 0) {
      return `${fallback} Backend is not reachable on http://localhost:8099.`;
    }

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

  private buildCancellationMessage(booking: Booking): string {
    const refundedAmount = booking.refundedAmountCents ?? 0;
    const refundPercentage = booking.cancellationRefundPercentage ?? 0;

    if (refundedAmount > 0) {
      return `Booking cancelled. Refund ${this.formatMoneyFromCents(refundedAmount, booking.payableCurrency)} (${refundPercentage}%).`;
    }

    if (booking.refundReason?.trim()) {
      return `Booking cancelled. ${booking.refundReason.trim()}.`;
    }

    return this.i18n.t('user.success.bookingCancelled');
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
