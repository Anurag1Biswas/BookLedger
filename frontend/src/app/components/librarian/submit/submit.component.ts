import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ApiService } from '../../../services/api.service';
import { Issue, Member, Submission } from '../../../models/models';
import { EntityPickerComponent } from '../../../shared/picker/entity-picker.component';
import { ToastService } from '../../../shared/toast/toast.service';
import { errorMessage } from '../../../shared/utils/error-message';

@Component({
  selector: 'app-submit',
  standalone: true,
  imports: [CommonModule, FormsModule, EntityPickerComponent],
  templateUrl: './submit.component.html'
})
export class SubmitComponent {
  @ViewChild('memberPicker') memberPicker?: EntityPickerComponent<Member>;

  member: Member | null = null;
  memberIssues: Issue[] = [];
  loadingIssues = false;
  selectedIssue: Issue | null = null;
  isDamaged = false;
  submitting = false;
  result: Submission | null = null;
  errorMsg = '';

  constructor(private api: ApiService, private toast: ToastService) {}

  searchMembers = (q: string): Observable<Member[]> =>
    /^\d+$/.test(q)
      ? this.api.getMember(+q).pipe(map(m => [m]), catchError(() => of([] as Member[])))
      : this.api.searchMembers(q);

  memberLabel = (m: Member) => m.name;
  memberSub = (m: Member) => `ID ${m.id} · ${m.booksIssued} book(s) out`;

  onMember(m: Member | null) {
    this.member = m;
    this.memberIssues = [];
    this.selectedIssue = null;
    this.errorMsg = '';
    if (m) this.loadIssues(m.id);
  }

  private loadIssues(memberId: number) {
    this.loadingIssues = true;
    this.api.getMemberIssues(memberId).subscribe({
      next: list => { this.memberIssues = list; this.loadingIssues = false; },
      error: err => { this.errorMsg = errorMessage(err, 'Could not load this member\'s loans.'); this.loadingIssues = false; }
    });
  }

  select(issue: Issue) {
    this.selectedIssue = issue;
    this.isDamaged = false;
    this.errorMsg = '';
  }

  returnBook() {
    if (!this.selectedIssue || !this.member) return;
    this.submitting = true;
    this.result = null;
    this.errorMsg = '';
    this.api.submitBook({ bookId: this.selectedIssue.bookId, memberId: this.member.id, damaged: this.isDamaged }).subscribe({
      next: r => {
        this.submitting = false;
        this.result = r;
        this.toast.success(`"${r.bookTitle}" returned. Bill: ₹${r.billAmount}.`);
        this.selectedIssue = null;
        this.isDamaged = false;
        if (this.member) this.loadIssues(this.member.id);
      },
      error: err => { this.submitting = false; this.errorMsg = errorMessage(err, 'Failed to process the return.'); }
    });
  }
}
