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
  private readonly timers = new Map<number, ReturnType<typeof setTimeout>>();

  readonly toasts = this.toastsSignal.asReadonly();

  success(message: string): void {
    this.push('success', message, 3500);
  }

  error(message: string): void {
    this.push('error', message, 5000);
  }

  info(message: string): void {
    this.push('info', message, 4000);
  }

  dismiss(id: number): void {
    this.toastsSignal.update((toasts) => toasts.filter((toast) => toast.id !== id));
    const timer = this.timers.get(id);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(id);
    }
  }

  private push(type: NotificationType, message: string, durationMs: number): void {
    const trimmedMessage = message.trim();
    if (!trimmedMessage) {
      return;
    }

    const id = this.nextId();
    this.nextId.update((value) => value + 1);

    this.toastsSignal.update((toasts) => [...toasts, { id, type, message: trimmedMessage }]);
    const timer = setTimeout(() => this.dismiss(id), durationMs);
    this.timers.set(id, timer);
  }
}
