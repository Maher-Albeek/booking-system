import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthStateService } from './auth-state.service';

@Component({
  selector: 'app-login-page',
  imports: [CommonModule, FormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private readonly router = inject(Router);

  protected readonly auth = inject(AuthStateService);
  protected readonly identifier = signal('');
  protected readonly password = signal('');
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

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
