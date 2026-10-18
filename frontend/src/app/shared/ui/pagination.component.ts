import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  template: `
    <nav class="pagination" *ngIf="totalPages > 0" aria-label="Pagination">
      <span class="pagination-info" aria-live="polite">
        {{ from }}–{{ to }} of {{ totalElements }}
      </span>
      <div class="pagination-buttons">
        <button type="button" class="btn btn-secondary btn-sm" (click)="go(page - 1)" [disabled]="page === 0"
                aria-label="Previous page">‹ Prev</button>
        <span class="pagination-page">Page {{ page + 1 }} of {{ totalPages }}</span>
        <button type="button" class="btn btn-secondary btn-sm" (click)="go(page + 1)" [disabled]="page >= totalPages - 1"
                aria-label="Next page">Next ›</button>
      </div>
    </nav>
  `
})
export class PaginationComponent {
  @Input() page = 0;
  @Input() size = 10;
  @Input() totalElements = 0;
  @Input() totalPages = 0;
  @Output() pageChange = new EventEmitter<number>();

  get from() { return this.totalElements === 0 ? 0 : this.page * this.size + 1; }
  get to() { return Math.min((this.page + 1) * this.size, this.totalElements); }

  go(p: number) {
    if (p >= 0 && p < this.totalPages) this.pageChange.emit(p);
  }
}
