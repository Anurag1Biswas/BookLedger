import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  Member, MemberRequest, Book, BookRequest,
  Issue, IssueRequest, Submission, SubmitRequest, DashboardStats, AiChatResponse,
  Page, OverviewStats, OverdueItem, PopularBook, DailyActivity, InventorySummary,
  MemberActivity, AppNotification
} from '../models/models';
import { environment } from '../../environments/environment';

/** Optional filters are only sent when they have a value. */
function params(values: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(values)) {
    if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
  }
  return p;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ---- Dashboard (existing) ----
  getDashboard() {
    return this.http.get<DashboardStats>(`${this.base}/dashboard`);
  }

  // ---- Members ----
  getMembers() {
    return this.http.get<Member[]>(`${this.base}/members`);
  }

  getMember(id: number) {
    return this.http.get<Member>(`${this.base}/members/${id}`);
  }

  addMember(req: MemberRequest) {
    return this.http.post<Member>(`${this.base}/members`, req);
  }

  updateMember(id: number, req: MemberRequest) {
    return this.http.put<Member>(`${this.base}/members/${id}`, req);
  }

  deleteMember(id: number) {
    return this.http.delete<string>(`${this.base}/members/${id}`, { responseType: 'text' as 'json' });
  }

  searchMembers(name: string) {
    return this.http.get<Member[]>(`${this.base}/members/search`, { params: params({ name }) });
  }

  pageMembers(o: { query?: string; memberType?: string; page?: number; size?: number; sort?: string }) {
    return this.http.get<Page<Member>>(`${this.base}/members/page`, { params: params(o) });
  }

  // ---- Books ----
  getBooks() {
    return this.http.get<Book[]>(`${this.base}/books`);
  }

  deleteBook(id: number) {
    return this.http.delete<string>(`${this.base}/books/${id}`, { responseType: 'text' as 'json' });
  }

  getAvailableBooks() {
    return this.http.get<Book[]>(`${this.base}/books/available`);
  }

  getBook(id: number) {
    return this.http.get<Book>(`${this.base}/books/${id}`);
  }

  addBook(req: BookRequest) {
    return this.http.post<Book>(`${this.base}/books`, req);
  }

  updateBook(id: number, req: BookRequest) {
    return this.http.put<Book>(`${this.base}/books/${id}`, req);
  }

  searchBooks(query: string) {
    return this.http.get<Book[]>(`${this.base}/books/search`, { params: params({ query }) });
  }

  pageBooks(o: { query?: string; genre?: string; available?: boolean | null; page?: number; size?: number; sort?: string }) {
    return this.http.get<Page<Book>>(`${this.base}/books/page`, { params: params(o) });
  }

  getGenres() {
    return this.http.get<string[]>(`${this.base}/books/genres`);
  }

  // ---- Issues ----
  getAllIssues() {
    return this.http.get<Issue[]>(`${this.base}/issues`);
  }

  getMemberIssues(memberId: number) {
    return this.http.get<Issue[]>(`${this.base}/issues/member/${memberId}`);
  }

  issueBook(req: IssueRequest) {
    return this.http.post<Issue>(`${this.base}/issue`, req);
  }

  // ---- Submissions ----
  getAllSubmissions() {
    return this.http.get<Submission[]>(`${this.base}/submissions`);
  }

  submitBook(req: SubmitRequest) {
    return this.http.post<Submission>(`${this.base}/submit`, req);
  }

  // ---- Reports (new) ----
  getOverview() {
    return this.http.get<OverviewStats>(`${this.base}/reports/overview`);
  }

  getOverdue() {
    return this.http.get<OverdueItem[]>(`${this.base}/reports/overdue`);
  }

  getDueSoon(days = 3) {
    return this.http.get<OverdueItem[]>(`${this.base}/reports/due-soon`, { params: params({ days }) });
  }

  getPopularBooks(limit = 5) {
    return this.http.get<PopularBook[]>(`${this.base}/reports/popular-books`, { params: params({ limit }) });
  }

  getActivity(days = 14) {
    return this.http.get<DailyActivity[]>(`${this.base}/reports/activity`, { params: params({ days }) });
  }

  getInventory() {
    return this.http.get<InventorySummary>(`${this.base}/reports/inventory`);
  }

  getMemberActivity(memberId: number) {
    return this.http.get<MemberActivity>(`${this.base}/reports/members/${memberId}`);
  }

  // ---- Notifications (new) ----
  getNotifications(o: { unreadOnly?: boolean; page?: number; size?: number } = {}) {
    return this.http.get<Page<AppNotification>>(`${this.base}/notifications`, { params: params(o) });
  }

  getUnreadCount() {
    return this.http.get<{ unread: number }>(`${this.base}/notifications/unread-count`);
  }

  markNotificationRead(id: number) {
    return this.http.patch<AppNotification>(`${this.base}/notifications/${id}/read`, {});
  }

  markAllNotificationsRead() {
    return this.http.post<{ updated: number }>(`${this.base}/notifications/read-all`, {});
  }

  // ---- AI ----
  chatWithAI(message: string) {
    return this.http.post<AiChatResponse>(`${this.base}/ai/chat`, { message });
  }

  getRecommendations(bookTitle: string) {
    return this.http.post<AiChatResponse>(`${this.base}/ai/recommend`, { bookTitle });
  }

  classifyGenre(title: string, author: string) {
    return this.http.post<AiChatResponse>(`${this.base}/ai/classify-genre`, { title, author });
  }

  generateOverdueMessage(memberName: string, bookTitle: string, daysOverdue: number) {
    return this.http.post<AiChatResponse>(
      `${this.base}/ai/overdue-message?memberName=${encodeURIComponent(memberName)}&bookTitle=${encodeURIComponent(bookTitle)}&daysOverdue=${daysOverdue}`,
      {}
    );
  }
}
