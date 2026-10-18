import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { interval, Subscription } from 'rxjs';
import { ApiService } from '../services/api.service';
import { AppNotification } from '../models/models';

/** Bell in the top bar. Notifications are produced asynchronously from Kafka events; we poll the count. */
@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bell">
      <button type="button" class="icon-btn bell-btn" (click)="toggle()"
              [attr.aria-expanded]="open" aria-haspopup="true"
              [attr.aria-label]="unread ? 'Notifications, ' + unread + ' unread' : 'Notifications'">
        <span aria-hidden="true">🔔</span>
        <span class="bell-count" *ngIf="unread" aria-hidden="true">{{ unread > 99 ? '99+' : unread }}</span>
      </button>

      <div class="bell-panel" *ngIf="open" role="region" aria-label="Recent notifications">
        <div class="bell-head">
          <strong>Notifications</strong>
          <button type="button" class="btn btn-ghost btn-sm" (click)="readAll()" [disabled]="!unread">Mark all read</button>
        </div>
        <div class="bell-loading" *ngIf="loading"><span class="spinner spinner-sm"></span> Loading…</div>
        <p class="bell-error" *ngIf="failed">Could not load notifications.</p>
        <p class="bell-empty" *ngIf="!loading && !failed && !items.length">No notifications yet. Issuing or returning a book creates one.</p>
        <ul class="bell-list">
          <li *ngFor="let n of items" [class.unread]="!n.read">
            <button type="button" (click)="read(n)">
              <span class="bell-msg">{{ n.message }}</span>
              <time class="muted" [attr.datetime]="n.createdAt">{{ n.createdAt | date:'d MMM, h:mm a' }}</time>
            </button>
          </li>
        </ul>
      </div>
    </div>
  `
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  unread = 0;
  open = false;
  loading = false;
  failed = false;
  items: AppNotification[] = [];
  private timer?: Subscription;

  constructor(private api: ApiService, private el: ElementRef<HTMLElement>) {}

  ngOnInit() {
    this.refreshCount();
    this.timer = interval(30000).subscribe(() => { this.refreshCount(); if (this.open) this.load(); });
  }

  ngOnDestroy() { this.timer?.unsubscribe(); }

  toggle() {
    this.open = !this.open;
    if (this.open) this.load();
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: Event) { if (this.open && !this.el.nativeElement.contains(e.target as Node)) this.open = false; }

  @HostListener('document:keydown.escape')
  onEsc() { this.open = false; }

  private refreshCount() {
    this.api.getUnreadCount().subscribe({ next: r => this.unread = r.unread, error: () => {} });
  }

  private load() {
    this.loading = true; this.failed = false;
    this.api.getNotifications({ size: 10 }).subscribe({
      next: p => { this.items = p.content; this.loading = false; },
      error: () => { this.loading = false; this.failed = true; }
    });
  }

  read(n: AppNotification) {
    if (n.read) return;
    this.api.markNotificationRead(n.id).subscribe(() => { n.read = true; this.refreshCount(); });
  }

  readAll() {
    this.api.markAllNotificationsRead().subscribe(() => { this.items.forEach(i => i.read = true); this.unread = 0; });
  }
}
