import {
  Component, ElementRef, EventEmitter, HostListener, Input, OnChanges,
  Output, SimpleChanges, ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';

let nextId = 0;

/**
 * Accessible dialog: role="dialog", aria-modal, labelled by its title, Escape closes,
 * Tab is trapped inside, focus moves in on open and returns to the opener on close.
 * Usage: <app-modal [open]="show" title="Add book" (closed)="show=false"> ...body... <div modal-footer>...</div> </app-modal>
 */
@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-backdrop" *ngIf="open" (mousedown)="onBackdrop($event)">
      <div #dialog class="modal" role="dialog" aria-modal="true" tabindex="-1"
           [attr.aria-labelledby]="titleId" [class.modal-sm]="size === 'sm'" [class.modal-lg]="size === 'lg'">
        <div class="modal-header">
          <h2 class="modal-title" [id]="titleId">{{ title }}</h2>
          <button type="button" class="icon-btn" aria-label="Close dialog" (click)="close()">&times;</button>
        </div>
        <div class="modal-body"><ng-content></ng-content></div>
        <div class="modal-footer"><ng-content select="[modal-footer]"></ng-content></div>
      </div>
    </div>
  `
})
export class ModalComponent implements OnChanges {
  @Input() open = false;
  @Input() title = '';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Output() closed = new EventEmitter<void>();
  @ViewChild('dialog') dialog?: ElementRef<HTMLElement>;

  titleId = `modal-title-${nextId++}`;
  private opener: HTMLElement | null = null;

  ngOnChanges(changes: SimpleChanges) {
    if (!changes['open']) return;
    if (this.open) {
      this.opener = document.activeElement as HTMLElement | null;
      setTimeout(() => this.focusFirst());
    } else if (this.opener) {
      this.opener.focus?.();
      this.opener = null;
    }
  }

  close() { this.closed.emit(); }

  onBackdrop(e: MouseEvent) {
    if (e.target === e.currentTarget) this.close();
  }

  @HostListener('document:keydown', ['$event'])
  onKey(e: KeyboardEvent) {
    if (!this.open) return;
    if (e.key === 'Escape') { e.stopPropagation(); this.close(); return; }
    if (e.key !== 'Tab') return;
    const items = this.focusables();
    if (!items.length) { e.preventDefault(); return; }
    const first = items[0], last = items[items.length - 1];
    const active = document.activeElement;
    if (e.shiftKey && (active === first || active === this.dialog?.nativeElement)) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && active === last) { e.preventDefault(); first.focus(); }
  }

  private focusables(): HTMLElement[] {
    const root = this.dialog?.nativeElement;
    if (!root) return [];
    return Array.from(root.querySelectorAll<HTMLElement>(
      'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])'
    ));
  }

  private focusFirst() {
    const body = this.dialog?.nativeElement.querySelector<HTMLElement>('input,select,textarea');
    (body ?? this.focusables()[0] ?? this.dialog?.nativeElement)?.focus();
  }
}
