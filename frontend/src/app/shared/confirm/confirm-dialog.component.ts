import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmService } from './confirm.service';
import { ModalComponent } from '../modal/modal.component';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, ModalComponent],
  template: `
    <ng-container *ngIf="confirm.request$ | async as req">
      <app-modal [open]="true" [title]="req.title" size="sm" (closed)="confirm.answer(false)">
        <p class="confirm-text">{{ req.message }}</p>
        <div modal-footer>
          <button type="button" class="btn btn-secondary" (click)="confirm.answer(false)">Cancel</button>
          <button type="button" class="btn" [ngClass]="req.danger ? 'btn-danger' : 'btn-primary'"
                  (click)="confirm.answer(true)">{{ req.confirmLabel }}</button>
        </div>
      </app-modal>
    </ng-container>
  `
})
export class ConfirmDialogComponent {
  constructor(public confirm: ConfirmService) {}
}
