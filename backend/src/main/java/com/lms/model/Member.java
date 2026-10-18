package com.lms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "lib_members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "mem_name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "mem_addrs", nullable = false)
    private String address;

    @Column(name = "mem_ph")
    private String phone;

    @Column(name = "mem_type")
    private String memberType;

    @Column(name = "no_of_books_issued")
    private int booksIssued = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active")
    private boolean active = true;

    public Member() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getMemberType() { return memberType; }
    public int getBooksIssued() { return booksIssued; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setMemberType(String memberType) { this.memberType = memberType; }
    public void setBooksIssued(int booksIssued) { this.booksIssued = booksIssued; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active) { this.active = active; }
}
