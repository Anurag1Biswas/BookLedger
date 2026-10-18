import { Component, HostListener, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { AuthService } from './services/auth.service';
import { AiChatComponent } from './components/ai-chat/ai-chat.component';
import { ToastContainerComponent } from './shared/toast/toast-container.component';
import { ConfirmDialogComponent } from './shared/confirm/confirm-dialog.component';
import { NotificationBellComponent } from './shared/notification-bell.component';

interface NavItem { path: string; label: string; icon: string; }
interface NavGroup { label: string; items: NavItem[]; }

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, AiChatComponent,
    ToastContainerComponent, ConfirmDialogComponent, NotificationBellComponent],
  template: `
    <a class="skip-link" href="#main" (click)="focusMain($event)">Skip to main content</a>

    <div class="app-shell" *ngIf="isLoggedIn; else loginPage">
      <header class="topbar">
        <button type="button" class="icon-btn menu-btn" (click)="menuOpen = !menuOpen"
                [attr.aria-expanded]="menuOpen" aria-controls="sidebar" aria-label="Toggle navigation menu">☰</button>
        <a class="topbar-brand" routerLink="/dashboard">📚 LibraryOS</a>
        <div class="topbar-spacer"></div>
        <app-notification-bell></app-notification-bell>
      </header>

      <div class="scrim" *ngIf="menuOpen" (click)="menuOpen = false" aria-hidden="true"></div>

      <nav class="sidebar" id="sidebar" aria-label="Main navigation" [class.open]="menuOpen">
        <div class="sidebar-logo">
          <div class="logo-icon" aria-hidden="true">📚</div>
          <div class="logo-text">
            <span class="logo-main">LibraryOS</span>
            <span class="logo-sub">Management System</span>
          </div>
        </div>

        <div class="nav-section" *ngFor="let g of groups">
          <span class="nav-label" aria-hidden="true">{{ g.label }}</span>
          <a *ngFor="let n of g.items" [routerLink]="n.path" routerLinkActive="active"
             #rla="routerLinkActive" [attr.aria-current]="rla.isActive ? 'page' : null" class="nav-item">
            <span class="nav-icon" aria-hidden="true">{{ n.icon }}</span> {{ n.label }}
          </a>
        </div>

        <div class="sidebar-footer">
          <div class="user-info">
            <div class="user-avatar" aria-hidden="true">{{ username?.charAt(0)?.toUpperCase() }}</div>
            <div class="user-details">
              <span class="user-name">{{ username }}</span>
              <span class="badge-role" [class.admin-badge]="isAdmin" [class.lib-badge]="isLibrarian">{{ role }}</span>
            </div>
          </div>
          <button type="button" class="logout-btn" (click)="logout()">Sign out</button>
        </div>
      </nav>

      <main class="main-content" id="main" tabindex="-1">
        <router-outlet></router-outlet>
      </main>
      <app-ai-chat></app-ai-chat>
    </div>

    <ng-template #loginPage>
      <main id="main" tabindex="-1"><router-outlet></router-outlet></main>
    </ng-template>

    <app-toasts></app-toasts>
    <app-confirm-dialog></app-confirm-dialog>
  `
})
export class AppComponent implements OnInit {
  isLoggedIn = false;
  username: string | null = '';
  role: string | null = '';
  isAdmin = false;
  isLibrarian = false;
  menuOpen = false;
  groups: NavGroup[] = [];

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit() {
    this.auth.isLoggedIn$.subscribe(v => {
      this.isLoggedIn = v;
      this.username = this.auth.getUsername();
      this.role = this.auth.getRole();
      this.isAdmin = this.auth.isAdmin();
      this.isLibrarian = this.auth.isLibrarian();
      this.groups = this.buildNav();
    });
    // close the drawer and move focus to the new page after each navigation
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      this.menuOpen = false;
      setTimeout(() => document.getElementById('main')?.focus({ preventScroll: true }));
    });
  }

  @HostListener('document:keydown.escape')
  onEscape() { this.menuOpen = false; }

  /** Same role split as before: ADMIN -> members, LIBRARIAN -> catalogue + operations. */
  private buildNav(): NavGroup[] {
    const groups: NavGroup[] = [{ label: 'OVERVIEW', items: [{ path: '/dashboard', label: 'Dashboard', icon: '◈' }] }];
    if (this.isAdmin) {
      groups.push({ label: 'ADMIN PANEL', items: [{ path: '/members', label: 'Members', icon: '◉' }] });
    }
    if (this.isLibrarian) {
      groups.push({ label: 'CATALOGUE', items: [{ path: '/books', label: 'Books', icon: '◆' }] });
      groups.push({
        label: 'OPERATIONS', items: [
          { path: '/issue', label: 'Issue Book', icon: '▶' },
          { path: '/submit', label: 'Return Book', icon: '◀' },
          { path: '/transactions', label: 'Transactions', icon: '≡' }
        ]
      });
    }
    return groups;
  }

  focusMain(e: Event) {
    e.preventDefault();
    document.getElementById('main')?.focus();
  }

  logout() { this.auth.logout(); }
}
