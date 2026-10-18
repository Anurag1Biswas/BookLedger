import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { Member, MemberActivity, Page } from '../../models/models';
import { ModalComponent } from '../../shared/modal/modal.component';
import { PaginationComponent } from '../../shared/ui/pagination.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ToastService } from '../../shared/toast/toast.service';
import { ConfirmService } from '../../shared/confirm/confirm.service';
import { errorMessage } from '../../shared/utils/error-message';

type SortField = 'name' | 'memberType' | 'booksIssued' | 'createdAt';

@Component({
  selector: 'app-members',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ModalComponent, PaginationComponent, EmptyStateComponent],
  templateUrl: './members.component.html'
})
export class MembersComponent implements OnInit, OnDestroy {
  page: Page<Member> | null = null;
  loading = true;
  loadError = '';
  saving = false;

  searchQuery = '';
  filterType = '';
  sortField: SortField = 'name';
  sortDir: 'asc' | 'desc' = 'asc';
  pageIndex = 0;
  pageSize = 10;

  memberTypes = ['Student', 'Teacher', 'Businessman', 'Other'];
  showModal = false;
  editingId: number | null = null;
  form: FormGroup;

  // member activity dialog (new /api/reports/members/{id})
  activityOpen = false;
  activityLoading = false;
  activityError = '';
  activity: MemberActivity | null = null;

  private search$ = new Subject<string>();
  private sub?: Subscription;

  constructor(private api: ApiService, private fb: FormBuilder,
              private toast: ToastService, private confirm: ConfirmService) {
    this.form = this.fb.group({
      name:       ['', [Validators.required, Validators.maxLength(255)]],
      address:    ['', [Validators.required, Validators.maxLength(255)]],
      phone:      ['', Validators.maxLength(255)],
      memberType: ['Student']
    });
  }

  ngOnInit() {
    this.sub = this.search$.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => { this.pageIndex = 0; this.load(); });
    this.load();
  }

  ngOnDestroy() { this.sub?.unsubscribe(); }

  onSearchInput() { this.search$.next(this.searchQuery); }

  load() {
    this.loading = true;
    this.loadError = '';
    this.api.pageMembers({
      query: this.searchQuery.trim(), memberType: this.filterType,
      page: this.pageIndex, size: this.pageSize, sort: `${this.sortField},${this.sortDir}`
    }).subscribe({
      next: p => {
        if (p.content.length === 0 && p.totalPages > 0 && this.pageIndex >= p.totalPages) {
          this.pageIndex = p.totalPages - 1; this.load(); return;
        }
        this.page = p; this.loading = false;
      },
      error: err => { this.loadError = errorMessage(err, 'Could not load members.'); this.loading = false; }
    });
  }

  applyFilters() { this.pageIndex = 0; this.load(); }
  resetFilters() { this.searchQuery = ''; this.filterType = ''; this.applyFilters(); }
  get hasFilters(): boolean { return !!(this.searchQuery.trim() || this.filterType); }

  sortBy(f: SortField) {
    if (this.sortField === f) this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    else { this.sortField = f; this.sortDir = 'asc'; }
    this.applyFilters();
  }

  ariaSort(f: SortField): 'ascending' | 'descending' | 'none' {
    return this.sortField === f ? (this.sortDir === 'asc' ? 'ascending' : 'descending') : 'none';
  }

  goTo(p: number) { this.pageIndex = p; this.load(); }

  openModal() {
    this.editingId = null;
    this.form.reset({ name: '', address: '', phone: '', memberType: 'Student' });
    this.showModal = true;
  }

  editMember(m: Member) {
    this.editingId = m.id;
    this.form.reset({ name: m.name, address: m.address, phone: m.phone ?? '', memberType: m.memberType || 'Student' });
    this.showModal = true;
  }

  closeModal() { if (!this.saving) this.showModal = false; }

  err(name: string): string {
    const c = this.form.get(name);
    if (!c || !c.touched || !c.errors) return '';
    if (c.errors['required']) return 'This field is required.';
    if (c.errors['maxlength']) return 'Too long (255 characters max).';
    return 'Invalid value.';
  }

  saveMember() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    const editing = this.editingId;
    const req$ = editing ? this.api.updateMember(editing, this.form.value) : this.api.addMember(this.form.value);
    req$.subscribe({
      next: m => {
        this.saving = false;
        this.toast.success(editing ? `${m.name} updated.` : `${m.name} added (member ID ${m.id}).`);
        this.showModal = false;
        this.load();
      },
      error: err => { this.saving = false; this.toast.error(errorMessage(err, 'Could not save the member.')); }
    });
  }

  async deleteMember(m: Member) {
    const ok = await this.confirm.ask({
      title: 'Cancel this membership?',
      message: `${m.name} will no longer appear in member lists. Their history is kept. Members who still hold books cannot be removed.`,
      confirmLabel: 'Cancel membership', danger: true
    });
    if (!ok) return;
    this.api.deleteMember(m.id).subscribe({
      next: msg => { this.toast.success(typeof msg === 'string' && msg ? msg : `${m.name} removed.`); this.load(); },
      error: err => this.toast.error(errorMessage(err, 'Cannot delete — member may still hold books.'))
    });
  }

  viewActivity(m: Member) {
    this.activityOpen = true;
    this.activityLoading = true;
    this.activityError = '';
    this.activity = null;
    this.api.getMemberActivity(m.id).subscribe({
      next: a => { this.activity = a; this.activityLoading = false; },
      error: err => { this.activityError = errorMessage(err, 'Could not load member activity.'); this.activityLoading = false; }
    });
  }
}
