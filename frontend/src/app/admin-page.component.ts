import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, Observable, of, switchMap } from 'rxjs';

import { AuthStateService } from './auth-state.service';
import { I18nService } from './i18n.service';

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

type OfferPageSettings = {
  heroBackgroundImageUrl: string;
};

type OfferDragState = {
  pointerId: number;
  sectionId: number;
  target: 'title' | 'description';
  startX: number;
  startY: number;
  startLeftPercent: number;
  startTopPercent: number;
  canvasWidth: number;
  canvasHeight: number;
};

type LegalField = {
  label: string;
  value: string;
};

type LegalContent = {
  impressumFields: LegalField[];
  datenschutzFields: LegalField[];
};

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
  protected readonly i18n = inject(I18nService);
  protected readonly pageMode: 'tools' | 'offers' | 'cars' | 'users' | 'legal' = this.resolvePageMode();
  protected readonly loading = signal(true);
  protected readonly busyKey = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly success = signal<string | null>(null);
  protected readonly isPhotoDragActive = signal(false);
  protected readonly isOfferHeroDragActive = signal(false);
  protected readonly editingCarId = signal<number | null>(null);
  protected readonly selectedCarId = signal<number | null>(null);
  protected readonly selectedOfferSectionId = signal<number | null>(null);
  protected readonly offerDragState = signal<OfferDragState | null>(null);

  protected readonly resources = signal<Resource[]>([]);
  protected readonly users = signal<User[]>([]);
  protected readonly bookings = signal<Booking[]>([]);
  protected readonly offerDraftSections = signal<OfferSection[]>([]);
  protected readonly offerPublishedSections = signal<OfferSection[]>([]);
  protected readonly offerDraftSettings = signal<OfferPageSettings>({ heroBackgroundImageUrl: '' });
  protected readonly offerPublishedSettings = signal<OfferPageSettings>({ heroBackgroundImageUrl: '' });
  protected readonly legalDraftContent = signal<LegalContent>({ impressumFields: [], datenschutzFields: [] });
  protected readonly legalPublishedContent = signal<LegalContent>({ impressumFields: [], datenschutzFields: [] });

  protected carDraft = {
    name: '',
    description: '',
    location: '',
    model: '',
    carType: '',
    color: '',
    year: null as number | null,
    seats: null as number | null,
    transmission: 'Automatic',
    fuelType: 'Benzin',
    dailyPrice: null as number | null,
    priceUnit: '€',
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

  protected readonly selectedOfferSection = computed(() => {
    const selectedSectionId = this.selectedOfferSectionId();
    if (selectedSectionId === null) {
      return null;
    }

    return this.offerDraftSections().find((section) => section.id === selectedSectionId) ?? null;
  });

  protected readonly stats = computed(() => [
    {
      label: this.i18n.t('admin.stats.cars'),
      value: this.cars().length,
      note: `${this.cars().filter((car) => car.active).length} ${this.i18n.t('admin.stats.active')}`
    },
    {
      label: this.i18n.t('admin.stats.activeCars'),
      value: this.cars().filter((car) => car.active).length,
      note: `${this.cars().length} ${this.i18n.t('admin.stats.totalCars')}`
    },
    {
      label: this.i18n.t('admin.stats.users'),
      value: this.users().length,
      note: `${this.users().filter((user) => (user.role ?? '').toUpperCase() === 'ADMIN').length} ${this.i18n.t('admin.stats.admins')}`
    },
    {
      label: this.i18n.t('admin.stats.bookings'),
      value: this.bookings().length,
      note: `${this.bookings().filter((booking) => booking.status === 'CONFIRMED').length} ${this.i18n.t('admin.stats.confirmed')}`
    }
  ]);

  protected readonly heroTitle = computed(() => {
    if (this.pageMode === 'users') {
      return this.i18n.t('admin.hero.usersTitle');
    }

    if (this.pageMode === 'offers') {
      return this.i18n.t('admin.hero.offersTitle');
    }

    if (this.pageMode === 'cars') {
      return this.i18n.t('admin.hero.carsTitle');
    }

    if (this.pageMode === 'legal') {
      return 'Manage legal pages';
    }

    return this.i18n.t('admin.hero.toolsTitle');
  });

  protected readonly heroDescription = computed(() => {
    if (this.pageMode === 'users') {
      return this.i18n.t('admin.hero.usersDescription');
    }

    if (this.pageMode === 'offers') {
      return this.i18n.t('admin.hero.offersDescription');
    }

    if (this.pageMode === 'cars') {
      return this.i18n.t('admin.hero.carsDescription');
    }

    if (this.pageMode === 'legal') {
      return 'Edit Impressum and Datenschutz fields, then publish.';
    }

    return this.i18n.t('admin.hero.toolsDescription');
  });

  protected readonly showToolsPanel = computed(() => this.pageMode === 'tools');
  protected readonly showOffersPanel = computed(() => this.pageMode === 'offers');
  protected readonly showCarsPanel = computed(() => this.pageMode === 'cars');
  protected readonly showUsersPanel = computed(() => this.pageMode === 'users');
  protected readonly showLegalPanel = computed(() => this.pageMode === 'legal');

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

  protected addOfferSection(): void {
    const newSection = this.createDefaultOfferSection();
    this.offerDraftSections.update((sections) => [...sections, newSection]);
    this.selectedOfferSectionId.set(newSection.id);
    this.success.set(null);
    this.error.set(null);
  }

  protected selectOfferSection(sectionId: number): void {
    this.selectedOfferSectionId.set(sectionId);
  }

  protected isOfferSectionSelected(sectionId: number): boolean {
    return this.selectedOfferSectionId() === sectionId;
  }

  protected removeOfferSection(sectionId: number): void {
    const nextSections = this.offerDraftSections().filter((section) => section.id !== sectionId);
    this.offerDraftSections.set(this.reorderOfferSections(nextSections));

    if (this.selectedOfferSectionId() === sectionId) {
      this.selectedOfferSectionId.set(nextSections[0]?.id ?? null);
    }
  }

  protected moveOfferSection(sectionId: number, direction: -1 | 1): void {
    const sections = [...this.offerDraftSections()];
    const currentIndex = sections.findIndex((section) => section.id === sectionId);
    const targetIndex = currentIndex + direction;

    if (currentIndex < 0 || targetIndex < 0 || targetIndex >= sections.length) {
      return;
    }

    const [section] = sections.splice(currentIndex, 1);
    sections.splice(targetIndex, 0, section);
    this.offerDraftSections.set(this.reorderOfferSections(sections));
  }

  protected updateSelectedOfferSection(patch: Partial<OfferSection>): void {
    const selectedSectionId = this.selectedOfferSectionId();
    if (selectedSectionId === null) {
      return;
    }

    this.updateOfferSection(selectedSectionId, patch);
  }

  protected saveOfferDraft(): void {
    this.runRequest(
      'save-offer-draft',
      this.http
        .put<OfferSection[]>('/api/offers/draft', this.offerDraftSections())
        .pipe(
          switchMap(() =>
            this.http.put<OfferPageSettings>('/api/offers/settings/draft', this.offerDraftSettings())
          ),
          catchError(() => of(this.offerDraftSettings()))
        ),
      'Offer draft saved successfully.'
    );
  }

  protected publishOfferDraft(): void {
    this.runRequest(
      'publish-offer-draft',
      this.http
        .put<OfferSection[]>('/api/offers/draft', this.offerDraftSections())
        .pipe(
          switchMap(() =>
            this.http.put<OfferPageSettings>('/api/offers/settings/draft', this.offerDraftSettings())
          ),
          catchError(() => of(this.offerDraftSettings())),
          switchMap(() => this.http.post<OfferSection[]>('/api/offers/publish', {})),
          switchMap(() => this.http.post<OfferPageSettings>('/api/offers/settings/publish', {})),
          catchError(() => of(this.offerDraftSettings()))
        ),
      'Offer page is now published.'
    );
  }

  protected updateOfferHeroBackgroundImageUrl(value: string): void {
    this.offerDraftSettings.set({
      heroBackgroundImageUrl: (value ?? '').trim()
    });
  }

  protected onOfferHeroDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isOfferHeroDragActive.set(true);
  }

  protected onOfferHeroDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isOfferHeroDragActive.set(false);
  }

  protected async onOfferHeroDrop(event: DragEvent): Promise<void> {
    event.preventDefault();
    this.isOfferHeroDragActive.set(false);
    const files = Array.from(event.dataTransfer?.files ?? []);
    await this.setOfferHeroImageFromFiles(files);
  }

  protected async onOfferHeroInputChange(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement | null;
    const files = Array.from(input?.files ?? []);
    await this.setOfferHeroImageFromFiles(files);
    if (input) {
      input.value = '';
    }
  }

  protected removeOfferHeroImage(): void {
    this.updateOfferHeroBackgroundImageUrl('');
  }

  protected startOfferElementDrag(event: PointerEvent, section: OfferSection, target: 'title' | 'description'): void {
    const handle = event.currentTarget as HTMLElement | null;
    const canvas = handle?.closest('.offer-preview-canvas') as HTMLElement | null;
    if (!handle || !canvas) {
      return;
    }

    handle.setPointerCapture(event.pointerId);
    const canvasRect = canvas.getBoundingClientRect();
    const startLeftPercent = target === 'title' ? section.titleXPercent : section.descriptionXPercent;
    const startTopPercent = target === 'title' ? section.titleYPercent : section.descriptionYPercent;

    this.offerDragState.set({
      pointerId: event.pointerId,
      sectionId: section.id,
      target,
      startX: event.clientX,
      startY: event.clientY,
      startLeftPercent,
      startTopPercent,
      canvasWidth: canvasRect.width,
      canvasHeight: canvasRect.height
    });

    event.preventDefault();
  }

  protected onOfferElementDrag(event: PointerEvent): void {
    const dragState = this.offerDragState();
    if (!dragState || dragState.pointerId !== event.pointerId) {
      return;
    }

    const deltaLeftPercent =
      dragState.canvasWidth > 0
        ? ((event.clientX - dragState.startX) / dragState.canvasWidth) * 100
        : 0;
    const deltaTopPercent =
      dragState.canvasHeight > 0
        ? ((event.clientY - dragState.startY) / dragState.canvasHeight) * 100
        : 0;

    const leftPercent = this.clampPercent(dragState.startLeftPercent + deltaLeftPercent, 2, 80);
    const topPercent = this.clampPercent(dragState.startTopPercent + deltaTopPercent, 2, 88);

    if (dragState.target === 'title') {
      this.updateOfferSection(dragState.sectionId, {
        titleXPercent: leftPercent,
        titleYPercent: topPercent
      });
      return;
    }

    this.updateOfferSection(dragState.sectionId, {
      descriptionXPercent: leftPercent,
      descriptionYPercent: topPercent
    });
  }

  protected endOfferElementDrag(event: PointerEvent): void {
    const dragState = this.offerDragState();
    const handle = event.currentTarget as HTMLElement | null;
    if (dragState && handle?.hasPointerCapture(dragState.pointerId)) {
      handle.releasePointerCapture(dragState.pointerId);
    }

    this.offerDragState.set(null);
  }

  protected offerTextWidthPercent(xPercent: number): number {
    return Math.max(24, 95 - this.clampPercent(xPercent, 0, 90));
  }

  protected saveCar(): void {
    const name = this.carDraft.name.trim();
    const location = this.carDraft.location.trim();
    const editingCarId = this.editingCarId();

    if (!name || !location) {
      this.error.set(this.i18n.t('admin.error.carNameLocationRequired'));
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
      editingCarId === null ? this.i18n.t('admin.success.carAdded') : this.i18n.t('admin.success.carUpdated'),
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
      color: car.color ?? '',
      year: car.year,
      seats: car.seats,
      transmission: car.transmission ?? 'Automatic',
      fuelType: car.fuelType ?? 'Benzin',
      dailyPrice: car.dailyPrice,
      priceUnit: car.priceUnit ?? '€',
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
    if (typeof window !== 'undefined' && !window.confirm(this.i18n.t('admin.confirm.deleteCar'))) {
      return;
    }

    this.runRequest(
      `delete-car-${carId}`,
      this.http.delete<void>(`/api/resources/${carId}`),
      this.i18n.t('admin.success.carDeleted'),
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
      this.error.set(this.i18n.t('admin.error.userNameEmailRequired'));
      return;
    }

    if (password.length < 6) {
      this.error.set(this.i18n.t('admin.error.passwordLength'));
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
      this.i18n.t('admin.success.userCreated'),
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
    if (typeof window !== 'undefined' && !window.confirm(this.i18n.t('admin.confirm.deleteUser'))) {
      return;
    }

    this.runRequest(
      `delete-user-${userId}`,
      this.http.delete<void>(`/api/users/${userId}`),
      this.i18n.t('admin.success.userDeleted')
    );
  }

  protected roleLabel(role: string | null): string {
    return role?.toUpperCase() === 'ADMIN'
      ? this.i18n.t('app.role.admin')
      : this.i18n.t('app.role.user');
  }

  protected photoCountLabel(photoUrls: string[] | null | undefined): string {
    const count = photoUrls?.length ?? 0;
    return count === 1 ? this.i18n.t('admin.photo.one') : this.i18n.t('admin.photo.many', { count });
  }

  protected climateLabel(hasAirConditioning: boolean | null | undefined): string {
    if (hasAirConditioning === null || hasAirConditioning === undefined) {
      return this.i18n.t('common.notSet');
    }
    return hasAirConditioning ? this.i18n.t('common.yes') : this.i18n.t('common.no');
  }

  protected formatPrice(amount: number | null | undefined, priceUnit: string | null | undefined): string {
    if (typeof amount !== 'number' || Number.isNaN(amount)) {
      return this.i18n.t('common.notSet');
    }

    const normalizedUnit = this.normalizePriceUnit(priceUnit);
    const formattedAmount = new Intl.NumberFormat(this.i18n.locale(), {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(amount);
    return `${formattedAmount} ${normalizedUnit}`;
  }

  protected legalFields(page: 'impressum' | 'datenschutz'): LegalField[] {
    return page === 'impressum'
      ? this.legalDraftContent().impressumFields
      : this.legalDraftContent().datenschutzFields;
  }

  protected addLegalField(page: 'impressum' | 'datenschutz'): void {
    this.updateLegalFields(page, [...this.legalFields(page), { label: '', value: '' }]);
  }

  protected updateLegalField(
    page: 'impressum' | 'datenschutz',
    index: number,
    patch: Partial<LegalField>
  ): void {
    const nextFields = this.legalFields(page).map((field, currentIndex) =>
      currentIndex === index
        ? {
            label: (patch.label ?? field.label),
            value: (patch.value ?? field.value)
          }
        : field
    );
    this.updateLegalFields(page, nextFields);
  }

  protected removeLegalField(page: 'impressum' | 'datenschutz', index: number): void {
    const nextFields = this.legalFields(page).filter((_, currentIndex) => currentIndex !== index);
    this.updateLegalFields(page, nextFields);
  }

  protected saveLegalDraft(): void {
    this.runRequest(
      'save-legal-draft',
      this.http.put<LegalContent>('/api/legal/draft', this.legalDraftContent()),
      'Legal draft saved successfully.'
    );
  }

  protected publishLegalDraft(): void {
    this.runRequest(
      'publish-legal-draft',
      this.http
        .put<LegalContent>('/api/legal/draft', this.legalDraftContent())
        .pipe(switchMap(() => this.http.post<LegalContent>('/api/legal/publish', {}))),
      'Legal pages are now published.'
    );
  }

  protected primaryPhotoUrl(resource: Resource): string | null {
    return resource.photoUrls[0] ?? null;
  }

  private resolvePageMode(): 'tools' | 'offers' | 'cars' | 'users' | 'legal' {
    const dataMode = this.route.snapshot.data['adminPageMode'];
    if (dataMode === 'offers' || dataMode === 'cars' || dataMode === 'users' || dataMode === 'tools' || dataMode === 'legal') {
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

    if (path.includes('manage-legal')) {
      return 'legal';
    }

    return 'tools';
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      resources: this.http.get<ResourceResponse[]>('/api/resources'),
      users: this.http.get<User[]>('/api/users'),
      bookings: this.http.get<Booking[]>('/api/bookings'),
      offerDraft: this.http.get<OfferSection[]>('/api/offers/draft'),
      offerPublished: this.http.get<OfferSection[]>('/api/offers/published'),
      offerDraftSettings: this.http
        .get<OfferPageSettings>('/api/offers/settings/draft')
        .pipe(catchError(() => of({ heroBackgroundImageUrl: '' } as OfferPageSettings))),
      offerPublishedSettings: this.http
        .get<OfferPageSettings>('/api/offers/settings/published')
        .pipe(catchError(() => of({ heroBackgroundImageUrl: '' } as OfferPageSettings))),
      legalDraft: this.http.get<LegalContent>('/api/legal/draft'),
      legalPublished: this.http.get<LegalContent>('/api/legal/published')
    })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false))
      )
      .subscribe({
        next: ({
          resources,
          users,
          bookings,
          offerDraft,
          offerPublished,
          offerDraftSettings,
          offerPublishedSettings,
          legalDraft,
          legalPublished
        }) => {
          this.resources.set(resources.map((resource) => this.normalizeResource(resource)));
          this.users.set(users);
          this.bookings.set(bookings);
          this.offerDraftSections.set(
            this.reorderOfferSections(
              offerDraft.map((section, index) => this.normalizeOfferSection(section, index))
            )
          );
          this.offerPublishedSections.set(
            this.reorderOfferSections(
              offerPublished.map((section, index) => this.normalizeOfferSection(section, index))
            )
          );
          this.offerDraftSettings.set(this.normalizeOfferPageSettings(offerDraftSettings));
          this.offerPublishedSettings.set(this.normalizeOfferPageSettings(offerPublishedSettings));
          this.legalDraftContent.set(this.normalizeLegalContent(legalDraft));
          this.legalPublishedContent.set(this.normalizeLegalContent(legalPublished));
          this.syncDraftDefaults();
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(
            this.readApiError(
              error,
              this.i18n.t('admin.error.loadFailed')
            )
          );
        }
      });
  }

  private syncDraftDefaults(): void {
    const selectedCarId = this.selectedCarId();
    const selectedCarStillExists = this.cars().some((car) => car.id === selectedCarId);
    const selectedOfferSectionId = this.selectedOfferSectionId();
    const selectedOfferSectionStillExists = this.offerDraftSections().some(
      (section) => section.id === selectedOfferSectionId
    );

    if (selectedCarId !== null && !selectedCarStillExists) {
      this.selectedCarId.set(null);
    }

    if (selectedOfferSectionId !== null && !selectedOfferSectionStillExists) {
      this.selectedOfferSectionId.set(this.offerDraftSections()[0]?.id ?? null);
      return;
    }

    if (selectedOfferSectionId === null && this.offerDraftSections().length) {
      this.selectedOfferSectionId.set(this.offerDraftSections()[0].id);
    }
  }

  private resetCarDraft(): void {
    this.carDraft = {
      name: '',
      description: '',
      location: '',
      model: '',
      carType: '',
      color: '',
      year: null,
      seats: null,
      transmission: 'Automatic',
      fuelType: 'Benzin',
      dailyPrice: null,
      priceUnit: '€',
      baggageBags: null,
      hasAirConditioning: true,
      horsepower: null,
      active: true,
      photoUrlsText: '',
      uploadedPhotoUrls: []
    };
  }

  private updateOfferSection(sectionId: number, patch: Partial<OfferSection>): void {
    this.offerDraftSections.update((sections) =>
      sections.map((section) =>
        section.id === sectionId
          ? this.normalizeOfferSection(
              {
                ...section,
                ...patch
              },
              section.sortOrder
            )
          : section
      )
    );
  }

  private createDefaultOfferSection(): OfferSection {
    const nextId = this.nextOfferSectionId();
    return {
      id: nextId,
      sortOrder: this.offerDraftSections().length,
      title: `Section ${nextId}`,
      description: 'Describe your offer here.',
      imageUrl: '',
      backgroundColor: '#10243a',
      textColor: '#f7f2ea',
      heightPx: 420,
      columns: 1,
      descriptionColumnGapPx: 24,
      descriptionColumnDividerWidthPx: 1,
      descriptionColumnDividerColor: '#f7f2ea',
      titleFontSizePx: 38,
      descriptionFontSizePx: 18,
      titleXPercent: 8,
      titleYPercent: 12,
      descriptionXPercent: 8,
      descriptionYPercent: 38
    };
  }

  private nextOfferSectionId(): number {
    const allIds = [...this.offerDraftSections(), ...this.offerPublishedSections()].map((section) => section.id);
    const maxId = allIds.length ? Math.max(...allIds) : 0;
    return maxId + 1;
  }

  private reorderOfferSections(sections: OfferSection[]): OfferSection[] {
    return sections.map((section, index) => ({
      ...section,
      sortOrder: index
    }));
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
      heightPx: this.clampInt(this.normalizePositiveInt(section.heightPx), 220, 980, 420),
      columns: this.clampInt(this.normalizePositiveInt(section.columns), 1, 3, 1),
      descriptionColumnGapPx: this.clampInt(this.normalizeNonNegativeInt(section.descriptionColumnGapPx), 0, 120, 24),
      descriptionColumnDividerWidthPx: this.clampInt(
        this.normalizeNonNegativeInt(section.descriptionColumnDividerWidthPx),
        0,
        12,
        1
      ),
      descriptionColumnDividerColor: this.normalizeColor(section.descriptionColumnDividerColor, '#f7f2ea'),
      titleFontSizePx: this.clampInt(this.normalizePositiveInt(section.titleFontSizePx), 20, 96, 38),
      descriptionFontSizePx: this.clampInt(this.normalizePositiveInt(section.descriptionFontSizePx), 12, 52, 18),
      titleXPercent: this.clampPercent(this.normalizeNumber(section.titleXPercent), 2, 80, 8),
      titleYPercent: this.clampPercent(this.normalizeNumber(section.titleYPercent), 2, 78, 12),
      descriptionXPercent: this.clampPercent(this.normalizeNumber(section.descriptionXPercent), 2, 80, 8),
      descriptionYPercent: this.clampPercent(this.normalizeNumber(section.descriptionYPercent), 2, 88, 38)
    };
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

  private normalizeNumber(value: unknown): number | null {
    if (typeof value !== 'number' || Number.isNaN(value) || !Number.isFinite(value)) {
      return null;
    }
    return Number(value.toFixed(2));
  }

  private normalizeColor(value: string | null | undefined, fallback: string): string {
    const normalized = (value ?? '').trim();
    if (/^#[0-9a-fA-F]{6}$/.test(normalized)) {
      return normalized;
    }
    return fallback;
  }

  private clampInt(value: number | null, min: number, max: number, fallback: number): number {
    if (value === null) {
      return fallback;
    }
    return Math.max(min, Math.min(max, value));
  }

  private clampPercent(value: number | null, min: number, max: number, fallback = min): number {
    if (value === null) {
      return fallback;
    }
    return Number(Math.max(min, Math.min(max, value)).toFixed(2));
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

  private buildCarPayload(name: string, location: string): Omit<Resource, 'id'> {
    return {
      name,
      description: this.carDraft.description.trim(),
      type: 'Car',
      location,
      model: this.carDraft.model.trim() || null,
      carType: this.carDraft.carType.trim() || null,
      color: this.normalizeText(this.carDraft.color),
      year: this.normalizeWholeNumber(this.carDraft.year),
      seats: this.normalizeWholeNumber(this.carDraft.seats),
      transmission: this.carDraft.transmission.trim() || null,
      fuelType: this.carDraft.fuelType.trim() || null,
      dailyPrice: this.normalizeDecimal(this.carDraft.dailyPrice),
      priceUnit: this.normalizePriceUnit(this.carDraft.priceUnit),
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

  private updateLegalFields(page: 'impressum' | 'datenschutz', fields: LegalField[]): void {
    const normalizedFields = this.normalizeLegalFields(fields);
    this.legalDraftContent.update((content) =>
      page === 'impressum'
        ? { ...content, impressumFields: normalizedFields }
        : { ...content, datenschutzFields: normalizedFields }
    );
  }

  private normalizeLegalContent(content: Partial<LegalContent> | null | undefined): LegalContent {
    return {
      impressumFields: this.normalizeLegalFields(content?.impressumFields),
      datenschutzFields: this.normalizeLegalFields(content?.datenschutzFields)
    };
  }

  private normalizeLegalFields(fields: LegalField[] | null | undefined): LegalField[] {
    const safeFields = Array.isArray(fields) ? fields : [];
    return safeFields.map((field) => ({
      label: (field?.label ?? '').trim(),
      value: (field?.value ?? '').trim()
    }));
  }

  private normalizeOfferPageSettings(settings: Partial<OfferPageSettings> | null | undefined): OfferPageSettings {
    return {
      heroBackgroundImageUrl: (settings?.heroBackgroundImageUrl ?? '').trim()
    };
  }

  private normalizeText(value: string | null | undefined): string | null {
    const normalizedValue = (value ?? '').trim();
    return normalizedValue || null;
  }

  private normalizePriceUnit(value: string | null | undefined): string {
    return this.normalizeText(value) ?? '€';
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
        this.error.set(this.i18n.t('admin.error.photoImageOnly'));
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
        error instanceof Error ? error.message : this.i18n.t('admin.error.photoConvertFailed')
      );
    }
  }

  private async setOfferHeroImageFromFiles(files: File[]): Promise<void> {
    const imageFile = files.find((file) => file.type.startsWith('image/'));
    if (!imageFile) {
      if (files.length) {
        this.error.set(this.i18n.t('admin.error.photoImageOnly'));
      }
      return;
    }

    this.error.set(null);
    try {
      const encodedImage = await this.convertImageToAvifDataUrl(imageFile, 1920, 1080, 0.78);
      this.updateOfferHeroBackgroundImageUrl(encodedImage);
    } catch (error) {
      this.error.set(
        error instanceof Error ? error.message : this.i18n.t('admin.error.photoConvertFailed')
      );
    }
  }

  private async convertImageToAvifDataUrl(
    file: File,
    maxWidth = Number.POSITIVE_INFINITY,
    maxHeight = Number.POSITIVE_INFINITY,
    quality = 0.82
  ): Promise<string> {
    const image = await this.loadImage(file);
    const canvas = document.createElement('canvas');
    const widthScale = Number.isFinite(maxWidth) ? maxWidth / image.width : 1;
    const heightScale = Number.isFinite(maxHeight) ? maxHeight / image.height : 1;
    const scale = Math.min(1, widthScale, heightScale);
    canvas.width = Math.max(1, Math.round(image.width * scale));
    canvas.height = Math.max(1, Math.round(image.height * scale));

    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error(this.i18n.t('admin.error.canvasUnavailable'));
    }

    context.drawImage(image, 0, 0, canvas.width, canvas.height);

    const avifBlob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (blob) => {
          if (blob) {
            resolve(blob);
            return;
          }

          reject(new Error(this.i18n.t('admin.error.browserAvifFailed')));
        },
        'image/avif',
        quality
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
        reject(new Error(this.i18n.t('admin.error.fileLoadFailed', { fileName: file.name })));
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

        reject(new Error(this.i18n.t('admin.error.avifReadFailed')));
      };

      reader.onerror = () => reject(reader.error ?? new Error(this.i18n.t('admin.error.avifReadFailed')));
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
          this.error.set(this.readApiError(error, this.i18n.t('admin.error.actionFailed')));
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
