import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from './toast.service';

/** One live region for the whole app: screen readers announce messages as they appear. */
@Component({
  selector: 'app-toasts',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-stack" role="region" aria-label="Notifications" aria-live="polite">
      <div *ngFor="let t of toasts.toasts$ | async" class="toast" [ngClass]="'toast-' + t.type"
           [attr.role]="t.type === 'error' ? 'alert' : 'status'">
        <span>{{ t.message }}</span>
        <button type="button" class="icon-btn" aria-label="Dismiss message" (click)="toasts.dismiss(t.id)">&times;</button>
      </div>
    </div>
  `
})
export class ToastContainerComponent {
  constructor(public toasts: ToastService) {}
}
