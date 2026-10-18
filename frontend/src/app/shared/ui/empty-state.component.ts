import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="empty-state" role="status">
      <div class="empty-icon" aria-hidden="true">{{ icon }}</div>
      <h3>{{ title }}</h3>
      <p *ngIf="hint">{{ hint }}</p>
      <ng-content></ng-content>
    </div>
  `,
  imports: [CommonModule]
})
export class EmptyStateComponent {
  @Input() icon = '📭';
  @Input() title = 'Nothing here yet';
  @Input() hint = '';
}
