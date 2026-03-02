import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthStateService } from './auth-state.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly auth = inject(AuthStateService);
  protected readonly mode = signal<'login' | 'register'>(
    this.route.snapshot.queryParamMap.get('mode') === 'register' ? 'register' : 'login'
  );
  protected readonly identifier = signal('');
  protected readonly password = signal('');
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly registerName = signal('');
  protected readonly registerEmail = signal('');
  protected readonly registerPassword = signal('');
  protected readonly registerSubmitting = signal(false);
  protected readonly registerError = signal<string | null>(null);

  protected setMode(mode: 'login' | 'register'): void {
    this.mode.set(mode);
    this.error.set(null);
    this.registerError.set(null);
  }

  protected submit(): void {
    const identifier = this.identifier().trim();
    const password = this.password().trim();

    if (!identifier || !password) {
      this.error.set('Enter your email or username and password.');
      return;
    }

    this.submitting.set(true);
    this.error.set(null);

    this.auth
      .login(identifier, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl(this.auth.landingRoute());
        },
        error: (error: HttpErrorResponse) => {
          this.error.set(this.readApiError(error, 'Login failed.'));
        }
      });
  }

  protected submitRegistration(): void {
    const name = this.registerName().trim();
    const email = this.registerEmail().trim();
    const password = this.registerPassword().trim();

    if (!name || !email) {
      this.registerError.set('Enter your name and email address.');
      return;
    }

    if (password.length < 6) {
      this.registerError.set('Password must be at least 6 characters long.');
      return;
    }

    this.registerSubmitting.set(true);
    this.registerError.set(null);

    this.auth
      .register(name, email, password)
      .pipe(finalize(() => this.registerSubmitting.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/user');
        },
        error: (error: HttpErrorResponse) => {
          this.registerError.set(this.readApiError(error, 'Registration failed.'));
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
