package com.lms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mem_id", nullable = false)
    private Member member;

    @Column(name = "return_date")
    private LocalDateTime returnDate = LocalDateTime.now();

    @Column(name = "days_kept")
    private long daysKept;

    @Column(name = "bill_amount")
    private double billAmount;

    @Column(name = "damaged")
    private boolean damaged = false;

    public Submission() {}

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public Member getMember() { return member; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public long getDaysKept() { return daysKept; }
    public double getBillAmount() { return billAmount; }
    public boolean isDamaged() { return damaged; }

    public void setId(Long id) { this.id = id; }
    public void setBook(Book book) { this.book = book; }
    public void setMember(Member member) { this.member = member; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public void setDaysKept(long daysKept) { this.daysKept = daysKept; }
    public void setBillAmount(double billAmount) { this.billAmount = billAmount; }
    public void setDamaged(boolean damaged) { this.damaged = damaged; }
}
