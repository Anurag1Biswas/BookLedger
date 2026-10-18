// ---- Auth Models ----
export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  role: string;
  username: string;
}

// ---- Member Models ----
export interface Member {
  id: number;
  name: string;
  address: string;
  phone: string;
  memberType: string;
  booksIssued: number;
  active: boolean;
  createdAt: string;
}

export interface MemberRequest {
  name: string;
  address: string;
  phone: string;
  memberType: string;
}

// ---- Book Models ----
export interface Book {
  id: number;
  title: string;
  author: string;
  genre: string;
  price: number;
  quantity: number;
  status: string;
}

export interface BookRequest {
  title: string;
  author: string;
  genre: string;
  price: number;
  quantity: number;
}

// ---- Issue Models ----
export interface Issue {
  id: number;
  bookId: number;
  bookTitle: string;
  memberId: number;
  memberName: string;
  issueDate: string;
  dueDate: string;
  status: number;
  overdue?: boolean;
  daysOverdue?: number;
}

export interface IssueRequest {
  bookId: number;
  memberId: number;
}

// ---- Submission Models ----
export interface Submission {
  id: number;
  bookTitle: string;
  memberName: string;
  daysKept: number;
  billAmount: number;
  damaged: boolean;
  returnDate: string;
}

export interface SubmitRequest {
  bookId: number;
  memberId: number;
  damaged: boolean;
}

// ---- Dashboard ----
export interface DashboardStats {
  totalMembers: number;
  totalBooks: number;
  activeIssues: number;
  totalSubmissions: number;
  availableBooks: number;
}
// ---- AI Models ----
export interface ChatMessage {
  role: 'user' | 'bot';
  text: string;
  time: string;
}

export interface AiChatRequest {
  message: string;
}

export interface AiChatResponse {
  reply: string;
  type: string;
}

export interface RecommendRequest {
  bookTitle: string;
}

export interface GenreRequest {
  title: string;
  author: string;
}

// ---- Paging ----
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ---- Reports ----
export interface OverviewStats {
  totalMembers: number;
  totalTitles: number;
  availableTitles: number;
  activeIssues: number;
  totalReturns: number;
  overdueCount: number;
  dueSoonCount: number;
  issuedLast30Days: number;
  returnedLast30Days: number;
  feesCollected: number;
  damagedReturns: number;
}

export interface OverdueItem {
  issueId: number;
  bookId: number;
  bookTitle: string;
  memberId: number;
  memberName: string;
  memberPhone: string;
  issueDate: string;
  dueDate: string;
  daysOverdue: number;
  accruedFee: number;
}

export interface PopularBook {
  bookId: number;
  title: string;
  author: string;
  timesIssued: number;
}

export interface DailyActivity {
  date: string;
  issued: number;
  returned: number;
}

export interface GenreStock {
  genre: string;
  titles: number;
  copies: number;
}

export interface InventorySummary {
  titles: number;
  totalCopies: number;
  copiesOnLoan: number;
  outOfStockTitles: number;
  lowStockTitles: number;
  inventoryValue: number;
  byGenre: GenreStock[];
}

export interface MemberActivity {
  member: Member;
  totalBorrowed: number;
  totalReturned: number;
  overdueCount: number;
  totalFees: number;
  activeIssues: Issue[];
  returns: Submission[];
}

export interface AppNotification {
  id: number;
  eventType: string;
  memberId: number | null;
  bookId: number | null;
  message: string;
  createdAt: string;
  read: boolean;
}
