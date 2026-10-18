package com.lms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mem_id", nullable = false)
    private Member member;

    @Column(name = "issue_dt")
    private LocalDateTime issueDate = LocalDateTime.now();

    @Column(name = "due_date")
    private LocalDate dueDate = LocalDate.now().plusDays(10);

    @Column(name = "issue_status")
    private int status = 1;

    public Issue() {}

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public Member getMember() { return member; }
    public LocalDateTime getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public int getStatus() { return status; }

    public void setId(Long id) { this.id = id; }
    public void setBook(Book book) { this.book = book; }
    public void setMember(Member member) { this.member = member; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setStatus(int status) { this.status = status; }
}
