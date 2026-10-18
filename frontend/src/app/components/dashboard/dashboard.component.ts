import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { DailyActivity, InventorySummary, OverdueItem, OverviewStats, PopularBook } from '../../models/models';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  loading = true;
  failed = false;
  stats: OverviewStats | null = null;
  overdue: OverdueItem[] = [];
  popular: PopularBook[] = [];
  activity: DailyActivity[] = [];
  inventory: InventorySummary | null = null;

  isAdmin = false;
  isLibrarian = false;
  username: string | null = '';
  accessDenied = false;
  today = new Date();

  constructor(private api: ApiService, private auth: AuthService, private route: ActivatedRoute) {}

  ngOnInit() {
    this.isAdmin = this.auth.isAdmin();
    this.isLibrarian = this.auth.isLibrarian();
    this.username = this.auth.getUsername();
    this.route.queryParams.subscribe(p => this.accessDenied = p['denied'] === 'true');
    this.load();
  }

  load() {
    this.loading = true;
    this.failed = false;
    // each widget fails independently so one broken report never blanks the whole dashboard
    forkJoin({
      stats: this.api.getOverview().pipe(catchError(() => of(null))),
      overdue: this.api.getOverdue().pipe(catchError(() => of([] as OverdueItem[]))),
      popular: this.api.getPopularBooks(5).pipe(catchError(() => of([] as PopularBook[]))),
      activity: this.api.getActivity(14).pipe(catchError(() => of([] as DailyActivity[]))),
      inventory: this.api.getInventory().pipe(catchError(() => of(null)))
    }).subscribe(r => {
      this.stats = r.stats;
      this.overdue = r.overdue;
      this.popular = r.popular;
      this.activity = r.activity;
      this.inventory = r.inventory;
      this.failed = !r.stats;
      this.loading = false;
    });
  }

  get maxDaily(): number {
    return Math.max(1, ...this.activity.map(a => Math.max(a.issued, a.returned)));
  }

  get maxPopular(): number {
    return Math.max(1, ...this.popular.map(p => p.timesIssued));
  }

  barHeight(v: number): string {
    return (v / this.maxDaily) * 100 + '%';
  }

  get activityTotal(): { issued: number; returned: number } {
    return this.activity.reduce((t, a) => ({ issued: t.issued + a.issued, returned: t.returned + a.returned }),
      { issued: 0, returned: 0 });
  }

  get greeting(): string {
    const h = new Date().getHours();
    return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening';
  }
}
