import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;

    httpTesting.expectOne('/api/resources/cars').flush([]);
    httpTesting.expectOne('/api/users').flush([]);
    httpTesting.expectOne('/api/time_slots').flush([]);
    httpTesting.expectOne('/api/bookings').flush([]);

    expect(app).toBeTruthy();
  });

  it('should render the client booking title', async () => {
    const fixture = TestBed.createComponent(App);

    httpTesting.expectOne('/api/resources/cars').flush([]);
    httpTesting.expectOne('/api/users').flush([]);
    httpTesting.expectOne('/api/time_slots').flush([]);
    httpTesting.expectOne('/api/bookings').flush([]);

    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Book Your Next Car');
  });
});
