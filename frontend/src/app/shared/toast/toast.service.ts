import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast { id: number; type: 'success' | 'error' | 'info'; message: string; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts$ = new BehaviorSubject<Toast[]>([]);

  success(message: string) { this.push('success', message, 4000); }
  info(message: string) { this.push('info', message, 4000); }
  /** Errors stay a little longer so they can be read. */
  error(message: string) { this.push('error', message, 7000); }

  dismiss(id: number) {
    this.toasts$.next(this.toasts$.value.filter(t => t.id !== id));
  }

  private push(type: Toast['type'], message: string, ms: number) {
    const id = ++this.seq;
    this.toasts$.next([...this.toasts$.value, { id, type, message }]);
    setTimeout(() => this.dismiss(id), ms);
  }
}
