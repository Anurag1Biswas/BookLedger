import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../../services/api.service';
import { Issue, Submission } from '../../../models/models';
import { PaginationComponent } from '../../../shared/ui/pagination.component';
import { EmptyStateComponent } from '../../../shared/ui/empty-state.component';
import { ToastService } from '../../../shared/toast/toast.service';
import { errorMessage } from '../../../shared/utils/error-message';

type Tab = 'issues' | 'returns';
const PAGE_SIZE = 15;

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent, EmptyStateComponent],
  templateUrl: './transactions.component.html'
})
export class TransactionsComponent implements OnInit {
  tab: Tab = 'issues';
  issues: Issue[] = [];
  submissions: Submission[] = [];
  loading = true;
  loadError = '';

  search = '';
  onlyOverdue = false;
  pageIndex = 0;

  overdueMsg = '';
  overdueFor = '';
  generatingFor = 0;
  copied = false;

  constructor(private api: ApiService, private toast: ToastService) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading = true;
    this.loadError = '';
    forkJoin({ issues: this.api.getAllIssues(), submissions: this.api.getAllSubmissions() }).subscribe({
      next: r => {
        // newest first
        this.issues = [...r.issues].sort((a, b) => (b.issueDate || '').localeCompare(a.issueDate || ''));
        this.submissions = [...r.submissions].sort((a, b) => (b.returnDate || '').localeCompare(a.returnDate || ''));
        this.loading = false;
      },
      error: err => { this.loadError = errorMessage(err, 'Could not load transactions.'); this.loading = false; }
    });
  }

  setTab(t: Tab) { this.tab = t; this.pageIndex = 0; }

  get overdueCount(): number { return this.issues.filter(i => i.overdue).length; }

  private matches(text: string): boolean {
    const q = this.search.trim().toLowerCase();
    return !q || text.toLowerCase().includes(q);
  }

  get filteredIssues(): Issue[] {
    return this.issues.filter(i => (!this.onlyOverdue || i.overdue) && this.matches(`${i.bookTitle} ${i.memberName}`));
  }

  get filteredSubmissions(): Submission[] {
    return this.submissions.filter(s => this.matches(`${s.bookTitle} ${s.memberName}`));
  }

  get total(): number { return this.tab === 'issues' ? this.filteredIssues.length : this.filteredSubmissions.length; }
  get totalPages(): number { return Math.ceil(this.total / PAGE_SIZE); }
  get pageSize(): number { return PAGE_SIZE; }

  get pagedIssues(): Issue[] { return this.filteredIssues.slice(this.pageIndex * PAGE_SIZE, (this.pageIndex + 1) * PAGE_SIZE); }
  get pagedSubmissions(): Submission[] { return this.filteredSubmissions.slice(this.pageIndex * PAGE_SIZE, (this.pageIndex + 1) * PAGE_SIZE); }

  onFilterChange() { this.pageIndex = 0; }

  // existing feature: AI-written overdue reminder (days overdue now come from the server)
  generateOverdueMsg(issue: Issue) {
    this.overdueMsg = '';
    this.overdueFor = `${issue.memberName} — ${issue.bookTitle}`;
    this.generatingFor = issue.id;
    this.copied = false;
    this.api.generateOverdueMessage(issue.memberName, issue.bookTitle, issue.daysOverdue ?? 0).subscribe({
      next: res => { this.overdueMsg = res.reply; this.generatingFor = 0; },
      error: () => {
        this.overdueMsg = 'Could not generate a message. Make sure the AI service (Ollama) is running.';
        this.generatingFor = 0;
      }
    });
  }

  copyMsg() {
    navigator.clipboard.writeText(this.overdueMsg).then(
      () => { this.copied = true; this.toast.success('Message copied.'); setTimeout(() => this.copied = false, 2000); },
      () => this.toast.error('Could not copy. Select the text and copy it manually.')
    );
  }
}
