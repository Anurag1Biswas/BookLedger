import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiService } from '../../../services/api.service';
import { Book, Issue, Member } from '../../../models/models';
import { EntityPickerComponent } from '../../../shared/picker/entity-picker.component';
import { ToastService } from '../../../shared/toast/toast.service';
import { errorMessage } from '../../../shared/utils/error-message';

@Component({
  selector: 'app-issue',
  standalone: true,
  imports: [CommonModule, EntityPickerComponent],
  templateUrl: './issue.component.html'
})
export class IssueComponent {
  @ViewChild('memberPicker') memberPicker?: EntityPickerComponent<Member>;
  @ViewChild('bookPicker') bookPicker?: EntityPickerComponent<Book>;

  member: Member | null = null;
  book: Book | null = null;
  submitting = false;
  result: Issue | null = null;
  errorMsg = '';
  recommendations = '';
  loadingRecommendations = false;

  constructor(private api: ApiService, private toast: ToastService) {}

  /** Search by name, or by exact ID when the user types a number (keeps the old "enter an ID" habit working). */
  searchMembers = (q: string): Observable<Member[]> =>
    /^\d+$/.test(q)
      ? this.api.getMember(+q).pipe(map(m => [m]), catchError(() => of([] as Member[])))
      : this.api.searchMembers(q);

  searchBooks = (q: string): Observable<Book[]> =>
    (/^\d+$/.test(q)
      ? this.api.getBook(+q).pipe(map(b => [b]), catchError(() => of([] as Book[])))
      : this.api.searchBooks(q)
    ).pipe(map(list => list.filter(b => b.quantity > 0)));

  memberLabel = (m: Member) => m.name;
  memberSub = (m: Member) => `ID ${m.id} · ${m.memberType || 'Member'} · ${m.booksIssued} book(s) out`;
  bookLabel = (b: Book) => b.title;
  bookSub = (b: Book) => `ID ${b.id} · ${b.author} · ${b.quantity} in stock`;

  get canIssue(): boolean { return !!this.member && !!this.book && !this.submitting; }

  issueBook() {
    console.log('Issuing book', this.book, 'to member', this.member);
    if (!this.member || !this.book) return;
    this.submitting = true;
    this.result = null;
    this.errorMsg = '';
    this.recommendations = '';

    this.api.issueBook({ bookId: this.book.id, memberId: this.member.id }).subscribe({
      next: r => {
        this.submitting = false;
        this.result = r;
        this.toast.success(`"${r.bookTitle}" issued to ${r.memberName}.`);
        this.memberPicker?.reset();
        this.bookPicker?.reset();
        this.member = null;
        this.book = null;
        this.loadRecommendations(r.bookTitle);
      },
      error: err => {
        this.submitting = false;
        this.errorMsg = errorMessage(err, 'Failed to issue the book.');
      }
    });
  }

  // existing feature: AI "you might also enjoy" after an issue
  private loadRecommendations(title: string) {
    this.loadingRecommendations = true;
    this.api.getRecommendations(title).subscribe({
      next: res => { this.recommendations = res.reply; this.loadingRecommendations = false; },
      error: () => { this.recommendations = 'Could not load recommendations right now.'; this.loadingRecommendations = false; }
    });
  }
}
