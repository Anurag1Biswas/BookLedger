import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ApiService } from '../../services/api.service';
import { Book, Page } from '../../models/models';
import { ModalComponent } from '../../shared/modal/modal.component';
import { PaginationComponent } from '../../shared/ui/pagination.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ToastService } from '../../shared/toast/toast.service';
import { ConfirmService } from '../../shared/confirm/confirm.service';
import { errorMessage } from '../../shared/utils/error-message';

type SortField = 'title' | 'author' | 'genre' | 'price' | 'quantity';

@Component({
  selector: 'app-books',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, ModalComponent, PaginationComponent, EmptyStateComponent],
  templateUrl: './books.component.html'
})
export class BooksComponent implements OnInit, OnDestroy {
  page: Page<Book> | null = null;
  genres: string[] = [];
  loading = true;
  loadError = '';
  saving = false;

  searchQuery = '';
  filterGenre = '';
  filterStatus: '' | 'available' | 'unavailable' = '';
  sortField: SortField = 'title';
  sortDir: 'asc' | 'desc' = 'asc';
  pageIndex = 0;
  pageSize = 10;

  showModal = false;
  editingId: number | null = null;
  form: FormGroup;

  private search$ = new Subject<string>();
  private sub?: Subscription;

  constructor(private api: ApiService, private fb: FormBuilder,
              private toast: ToastService, private confirm: ConfirmService) {
    this.form = this.fb.group({
      title:    ['', [Validators.required, Validators.maxLength(255)]],
      author:   ['', [Validators.required, Validators.maxLength(255)]],
      genre:    ['', Validators.maxLength(255)],
      price:    [0,  [Validators.required, Validators.min(0)]],
      quantity: [1,  [Validators.required, Validators.min(1), Validators.pattern(/^\d+$/)]]
    });
  }

  ngOnInit() {
    this.sub = this.search$.pipe(debounceTime(300), distinctUntilChanged()).subscribe(() => {
      this.pageIndex = 0;
      this.load();
    });
    this.load();
    this.loadGenres();
  }

  ngOnDestroy() { this.sub?.unsubscribe(); }

  onSearchInput() { this.search$.next(this.searchQuery); }

  load() {
    this.loading = true;
    this.loadError = '';
    this.api.pageBooks({
      query: this.searchQuery.trim(),
      genre: this.filterGenre,
      available: this.filterStatus === '' ? null : this.filterStatus === 'available',
      page: this.pageIndex,
      size: this.pageSize,
      sort: `${this.sortField},${this.sortDir}`
    }).subscribe({
      next: p => {
        // the last page can disappear after a delete; step back instead of showing an empty page
        if (p.content.length === 0 && p.totalPages > 0 && this.pageIndex >= p.totalPages) {
          this.pageIndex = p.totalPages - 1;
          this.load();
          return;
        }
        this.page = p;
        this.loading = false;
      },
      error: err => { this.loadError = errorMessage(err, 'Could not load books.'); this.loading = false; }
    });
  }

  private loadGenres() {
    this.api.getGenres().subscribe({ next: g => this.genres = g, error: () => {} });
  }

  applyFilters() { this.pageIndex = 0; this.load(); }

  resetFilters() {
    this.searchQuery = ''; this.filterGenre = ''; this.filterStatus = '';
    this.sortField = 'title'; this.sortDir = 'asc';
    this.applyFilters();
  }

  get hasFilters(): boolean { return !!(this.searchQuery.trim() || this.filterGenre || this.filterStatus); }

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
    this.form.reset({ title: '', author: '', genre: '', price: 0, quantity: 1 });
    this.showModal = true;
  }

  editBook(b: Book) {
    this.editingId = b.id;
    this.form.reset({ title: b.title, author: b.author, genre: b.genre ?? '', price: b.price, quantity: b.quantity });
    this.showModal = true;
  }

  closeModal() { if (!this.saving) this.showModal = false; }

  err(name: string): string {
    const c = this.form.get(name);
    if (!c || !c.touched || !c.errors) return '';
    if (c.errors['required']) return 'This field is required.';
    if (c.errors['min']) return name === 'quantity' ? 'Quantity must be at least 1.' : 'Must not be negative.';
    if (c.errors['pattern']) return 'Enter a whole number.';
    if (c.errors['maxlength']) return 'Too long (255 characters max).';
    return 'Invalid value.';
  }

  saveBook() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = { ...this.form.value };
    this.saving = true;

    // existing behaviour: if no genre is entered, ask the AI to classify it
    if (!value.genre && value.title && value.author) {
      this.api.classifyGenre(value.title, value.author).subscribe({
        next: res => { value.genre = res.reply; this.submit(value); },
        error: () => this.submit(value)   // AI is optional - save without a genre rather than failing
      });
    } else {
      this.submit(value);
    }
  }

  private submit(value: any) {
    const req$ = this.editingId ? this.api.updateBook(this.editingId, value) : this.api.addBook(value);
    req$.subscribe({
      next: b => {
        this.saving = false;
        this.toast.success(this.editingId ? `"${b.title}" updated.` : `"${b.title}" added (ID ${b.id}).`);
        this.showModal = false;
        this.load();
        this.loadGenres();
      },
      error: err => { this.saving = false; this.toast.error(errorMessage(err, 'Could not save the book.')); }
    });
  }

  async deleteBook(b: Book) {
    const ok = await this.confirm.ask({
      title: 'Remove this book?',
      message: `"${b.title}" will be removed from the catalogue. Books with issue history cannot be removed.`,
      confirmLabel: 'Remove', danger: true
    });
    if (!ok) return;
    this.api.deleteBook(b.id).subscribe({
      next: () => { this.toast.success(`"${b.title}" removed.`); this.load(); this.loadGenres(); },
      error: err => this.toast.error(errorMessage(err, 'Cannot delete — this book may have issue history.'))
    });
  }
}
