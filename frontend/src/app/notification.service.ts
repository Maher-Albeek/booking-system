import { Injectable, signal } from '@angular/core';

export type NotificationType = 'success' | 'error' | 'info';

export type NotificationToast = {
  id: number;
  type: NotificationType;
  message: string;
};

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly nextId = signal(1);
  private readonly toastsSignal = signal<NotificationToast[]>([]);

  readonly toasts = this.toastsSignal.asReadonly();

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  info(message: string): void {
    this.push('info', message);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private push(type: NotificationType, message: string): void {
    const trimmedMessage = message.trim();
    if (!trimmedMessage) {
      return;
    }

    const id = this.nextId();
    this.nextId.update((value) => value + 1);

    this.toastsSignal.update((toasts) => [...toasts, { id, type, message: trimmedMessage }]);
  }
}
