import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel: string;
  danger: boolean;
  resolve: (ok: boolean) => void;
}

/** Replaces window.confirm(): `if (await this.confirm.ask({...})) { ... }` */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly request$ = new BehaviorSubject<ConfirmRequest | null>(null);

  ask(o: { title: string; message: string; confirmLabel?: string; danger?: boolean }): Promise<boolean> {
    return new Promise(resolve => {
      this.request$.next({
        title: o.title, message: o.message,
        confirmLabel: o.confirmLabel ?? 'Confirm', danger: o.danger ?? false,
        resolve
      });
    });
  }

  answer(ok: boolean) {
    const r = this.request$.value;
    this.request$.next(null);
    r?.resolve(ok);
  }
}
