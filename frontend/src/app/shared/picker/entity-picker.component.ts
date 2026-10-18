import { Component, EventEmitter, Input, OnDestroy, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, Subject, of, Subscription } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';

let nextId = 0;

/**
 * Searchable single-select (WAI-ARIA combobox pattern): type to search, Up/Down to move,
 * Enter to choose, Escape to close. Replaces typing raw numeric IDs.
 */
@Component({
  selector: 'app-entity-picker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="picker">
      <label class="label" [for]="id">{{ label }}</label>
      <div class="picker-input" *ngIf="!selected; else chosen">
        <input [id]="id" type="text" role="combobox" autocomplete="off" [placeholder]="placeholder"
               [attr.aria-expanded]="open" [attr.aria-controls]="id + '-list'" aria-autocomplete="list"
               [attr.aria-activedescendant]="active >= 0 ? id + '-opt-' + active : null"
               [ngModel]="query" (ngModelChange)="onType($event)"
               (focus)="onFocus()" (blur)="onBlur()" (keydown)="onKey($event)">
        <span class="spinner spinner-sm" *ngIf="loading" aria-hidden="true"></span>
      </div>
      <ng-template #chosen>
        <div class="picker-selected">
          <div>
            <strong>{{ display(selected) }}</strong>
            <span class="muted" *ngIf="sub(selected)"> · {{ sub(selected) }}</span>
          </div>
          <button type="button" class="btn btn-ghost btn-sm" (click)="clear()">Change</button>
        </div>
      </ng-template>
      <ul class="picker-list" [id]="id + '-list'" role="listbox" *ngIf="open && !selected">
        <li *ngFor="let r of results; let i = index" role="option" [id]="id + '-opt-' + i"
            [class.active]="i === active" [attr.aria-selected]="i === active"
            (mousedown)="$event.preventDefault(); choose(r)" (mouseenter)="active = i">
          <strong>{{ display(r) }}</strong>
          <span class="muted">{{ sub(r) }}</span>
        </li>
        <li class="picker-empty" *ngIf="!results.length && !loading" role="option" aria-selected="false">
          {{ query.trim() ? 'No matches found' : 'Start typing to search' }}
        </li>
      </ul>
    </div>
  `
})
export class EntityPickerComponent<T = any> implements OnDestroy {
  @Input() label = '';
  @Input() placeholder = 'Search…';
  @Input() searchFn!: (q: string) => Observable<T[]>;
  @Input() display: (t: any) => string = t => String(t);
  @Input() sub: (t: any) => string = () => '';
  @Output() picked = new EventEmitter<T | null>();

  id = `picker-${nextId++}`;
  query = '';
  results: T[] = [];
  selected: T | null = null;
  open = false;
  loading = false;
  active = -1;

  private input$ = new Subject<string>();
  private sub$: Subscription = this.input$.pipe(
    debounceTime(250), distinctUntilChanged(),
    tap(() => this.loading = true),
    switchMap(q => q.trim().length < 1 ? of([] as T[]) : this.searchFn(q.trim()).pipe(catchError(() => of([] as T[]))))
  ).subscribe(r => { this.results = r.slice(0, 8); this.loading = false; this.active = r.length ? 0 : -1; });

  ngOnDestroy() { this.sub$.unsubscribe(); }

  onType(v: string) { this.query = v; this.open = true; this.input$.next(v); }
  onFocus() { this.open = true; }
  onBlur() { this.open = false; }

  onKey(e: KeyboardEvent) {
    if (e.key === 'ArrowDown') { e.preventDefault(); this.open = true; this.active = Math.min(this.active + 1, this.results.length - 1); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); this.active = Math.max(this.active - 1, 0); }
    else if (e.key === 'Enter' && this.open && this.active >= 0 && this.results[this.active]) { e.preventDefault(); this.choose(this.results[this.active]); }
    else if (e.key === 'Escape') { this.open = false; }
  }

  choose(item: T) { this.selected = item; this.open = false; this.picked.emit(item); }

  clear() {
    this.selected = null; this.query = ''; this.results = [];
    this.picked.emit(null);
    setTimeout(() => (document.getElementById(this.id) as HTMLElement | null)?.focus());
  }

  /** Lets a parent reset the picker after a successful action. */
  reset() { this.selected = null; this.query = ''; this.results = []; }
}
