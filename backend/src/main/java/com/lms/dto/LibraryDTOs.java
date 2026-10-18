package com.lms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class LibraryDTOs {

    // ---- Member DTOs ----
    public static class MemberRequest {
        @NotBlank @Size(max = 255) private String name;
        @NotBlank @Size(max = 255) private String address;
        @Size(max = 255) private String phone;
        @Size(max = 255) private String memberType;

        public MemberRequest() {}
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getMemberType() { return memberType; }
        public void setName(String name) { this.name = name; }
        public void setAddress(String address) { this.address = address; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setMemberType(String memberType) { this.memberType = memberType; }
    }

    public static class MemberResponse {
        private Long id;
        private String name;
        private String address;
        private String phone;
        private String memberType;
        private int booksIssued;
        private boolean active;
        private String createdAt;

        public MemberResponse() {}
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getMemberType() { return memberType; }
        public int getBooksIssued() { return booksIssued; }
        public boolean isActive() { return active; }
        public String getCreatedAt() { return createdAt; }
        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setAddress(String address) { this.address = address; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setMemberType(String memberType) { this.memberType = memberType; }
        public void setBooksIssued(int booksIssued) { this.booksIssued = booksIssued; }
        public void setActive(boolean active) { this.active = active; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // ---- Book DTOs ----
    public static class BookRequest {
        @NotBlank @Size(max = 255) private String title;
        @NotBlank @Size(max = 255) private String author;
        @Size(max = 255) private String genre;
        @PositiveOrZero(message = "must not be negative") private double price;
        @PositiveOrZero(message = "must not be negative") private int quantity;

        public BookRequest() {}
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getGenre() { return genre; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public void setTitle(String title) { this.title = title; }
        public void setAuthor(String author) { this.author = author; }
        public void setGenre(String genre) { this.genre = genre; }
        public void setPrice(double price) { this.price = price; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class BookResponse {
        private Long id;
        private String title;
        private String author;
        private String genre;
        private double price;
        private int quantity;
        private String status;

        public BookResponse() {}
        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getGenre() { return genre; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public String getStatus() { return status; }
        public void setId(Long id) { this.id = id; }
        public void setTitle(String title) { this.title = title; }
        public void setAuthor(String author) { this.author = author; }
        public void setGenre(String genre) { this.genre = genre; }
        public void setPrice(double price) { this.price = price; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public void setStatus(String status) { this.status = status; }
    }

    // ---- Issue DTOs ----
    public static class IssueRequest {
        @NotNull private Long bookId;
        @NotNull private Long memberId;

        public IssueRequest() {}
        public Long getBookId() { return bookId; }
        public Long getMemberId() { return memberId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
    }

    public static class IssueResponse {
        private Long id;
        private Long bookId;
        private String bookTitle;
        private Long memberId;
        private String memberName;
        private String issueDate;
        private String dueDate;
        private int status;
        private boolean overdue;
        private long daysOverdue;

        public IssueResponse() {}
        public Long getId() { return id; }
        public Long getBookId() { return bookId; }
        public String getBookTitle() { return bookTitle; }
        public Long getMemberId() { return memberId; }
        public String getMemberName() { return memberName; }
        public String getIssueDate() { return issueDate; }
        public String getDueDate() { return dueDate; }
        public int getStatus() { return status; }
        public boolean isOverdue() { return overdue; }
        public long getDaysOverdue() { return daysOverdue; }
        public void setOverdue(boolean overdue) { this.overdue = overdue; }
        public void setDaysOverdue(long daysOverdue) { this.daysOverdue = daysOverdue; }
        public void setId(Long id) { this.id = id; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public void setStatus(int status) { this.status = status; }
    }

    // ---- Submission DTOs ----
    public static class SubmitRequest {
        @NotNull private Long bookId;
        @NotNull private Long memberId;
        private boolean damaged;

        public SubmitRequest() {}
        public Long getBookId() { return bookId; }
        public Long getMemberId() { return memberId; }
        public boolean isDamaged() { return damaged; }
        public void setBookId(Long bookId) { this.bookId = bookId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public void setDamaged(boolean damaged) { this.damaged = damaged; }
    }

    public static class SubmissionResponse {
        private Long id;
        private String bookTitle;
        private String memberName;
        private long daysKept;
        private double billAmount;
        private boolean damaged;
        private String returnDate;

        public SubmissionResponse() {}
        public Long getId() { return id; }
        public String getBookTitle() { return bookTitle; }
        public String getMemberName() { return memberName; }
        public long getDaysKept() { return daysKept; }
        public double getBillAmount() { return billAmount; }
        public boolean isDamaged() { return damaged; }
        public String getReturnDate() { return returnDate; }
        public void setId(Long id) { this.id = id; }
        public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        public void setDaysKept(long daysKept) { this.daysKept = daysKept; }
        public void setBillAmount(double billAmount) { this.billAmount = billAmount; }
        public void setDamaged(boolean damaged) { this.damaged = damaged; }
        public void setReturnDate(String returnDate) { this.returnDate = returnDate; }
    }

    // ---- Dashboard ----
    public static class DashboardStats {
        private long totalMembers;
        private long totalBooks;
        private long activeIssues;
        private long totalSubmissions;
        private long availableBooks;

        public DashboardStats() {}
        public DashboardStats(long totalMembers, long totalBooks, long activeIssues,
                              long totalSubmissions, long availableBooks) {
            this.totalMembers = totalMembers;
            this.totalBooks = totalBooks;
            this.activeIssues = activeIssues;
            this.totalSubmissions = totalSubmissions;
            this.availableBooks = availableBooks;
        }
        public long getTotalMembers() { return totalMembers; }
        public long getTotalBooks() { return totalBooks; }
        public long getActiveIssues() { return activeIssues; }
        public long getTotalSubmissions() { return totalSubmissions; }
        public long getAvailableBooks() { return availableBooks; }
        public void setTotalMembers(long totalMembers) { this.totalMembers = totalMembers; }
        public void setTotalBooks(long totalBooks) { this.totalBooks = totalBooks; }
        public void setActiveIssues(long activeIssues) { this.activeIssues = activeIssues; }
        public void setTotalSubmissions(long totalSubmissions) { this.totalSubmissions = totalSubmissions; }
        public void setAvailableBooks(long availableBooks) { this.availableBooks = availableBooks; }
    }
}
