package com.lms.service;

import com.lms.dto.LibraryDTOs.*;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.kafka.BookEvent;
import com.lms.model.*;
import com.lms.repository.*;
import com.lms.util.BillingCalculator;
import com.lms.util.DueDates;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final SubmissionRepository submissionRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public IssueService(IssueRepository issueRepository,
                        SubmissionRepository submissionRepository,
                        BookRepository bookRepository,
                        MemberRepository memberRepository,
                        ApplicationEventPublisher eventPublisher,
                        Clock clock) {
        this.issueRepository = issueRepository;
        this.submissionRepository = submissionRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public IssueResponse issueBook(IssueRequest req) {
        Member member = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Book book = bookRepository.findById(req.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (!member.isActive()) {
            throw new BusinessRuleException("Member is not active");
        }
        if (issueRepository.findByBook_IdAndMember_IdAndStatus(book.getId(), member.getId(), 1).isPresent()) {
            throw new BusinessRuleException("This member already has this book issued");
        }

        if (book.getQuantity() <= 0 || !"Available".equals(book.getStatus())) {
            throw new BusinessRuleException("Book is currently not available");
        }

        // Decrease quantity
        book.setQuantity(book.getQuantity() - 1);
        if (book.getQuantity() == 0) book.setStatus("Unavailable");
        bookRepository.save(book);

        // Increment member count
        member.setBooksIssued(member.getBooksIssued() + 1);
        memberRepository.save(member);

        // Create issue record
        Issue issue = new Issue();
        issue.setBook(book);
        issue.setMember(member);
        issue.setStatus(1);
        issue.setIssueDate(LocalDateTime.now(clock));
        issue.setDueDate(LocalDate.now(clock).plusDays(BillingCalculator.INCLUDED_DAYS));
        Issue saved = issueRepository.save(issue);

        // Announced to Kafka only after this transaction commits (see BookEventProducer)
        eventPublisher.publishEvent(BookEvent.issued(member.getId(), member.getName(),
                book.getId(), book.getTitle(), saved.getDueDate(), clock.instant()));

        return toIssueResponse(saved);
    }

    @Transactional
    public SubmissionResponse submitBook(SubmitRequest req) {
        Member member = memberRepository.findById(req.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Book book = bookRepository.findById(req.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        Issue issue = issueRepository
                .findByBook_IdAndMember_IdAndStatus(req.getBookId(), req.getMemberId(), 1)
                .orElseThrow(() -> new BusinessRuleException("No active issue found for this member and book"));

        // Calculate billing
        LocalDateTime now = LocalDateTime.now(clock);
        long daysKept = ChronoUnit.DAYS.between(issue.getIssueDate(), now);
        double bill = BillingCalculator.calculate(daysKept, req.isDamaged(), book.getPrice());

        // Update issue status
        issue.setStatus(0);
        issueRepository.save(issue);

        // Update book quantity
        book.setQuantity(book.getQuantity() + 1);
        book.setStatus("Available");
        bookRepository.save(book);

        // Update member count
        member.setBooksIssued(Math.max(0, member.getBooksIssued() - 1));
        memberRepository.save(member);

        // Create submission record
        Submission sub = new Submission();
        sub.setBook(book);
        sub.setMember(member);
        sub.setReturnDate(now);
        sub.setDaysKept(daysKept);
        sub.setBillAmount(bill);
        sub.setDamaged(req.isDamaged());
        Submission saved = submissionRepository.save(sub);

        eventPublisher.publishEvent(BookEvent.returned(member.getId(), member.getName(),
                book.getId(), book.getTitle(), bill, req.isDamaged(), clock.instant()));

        return toSubmissionResponse(saved);
    }

    public List<IssueResponse> getAllActiveIssues() {
        return issueRepository.findByStatus(1).stream()
                .map(this::toIssueResponse).collect(Collectors.toList());
    }

    public List<IssueResponse> getMemberIssues(Long memberId) {
        return issueRepository.findByMember_IdAndStatus(memberId, 1).stream()
                .map(this::toIssueResponse).collect(Collectors.toList());
    }

    public List<SubmissionResponse> getAllSubmissions() {
        return submissionRepository.findAll().stream()
                .map(this::toSubmissionResponse).collect(Collectors.toList());
    }

    public List<SubmissionResponse> getMemberSubmissions(Long memberId) {
        return submissionRepository.findByMember_Id(memberId).stream()
                .map(this::toSubmissionResponse).collect(Collectors.toList());
    }

    public long getActiveIssueCount() {
        return issueRepository.countByStatus(1);
    }

    public IssueResponse toIssueResponse(Issue i) {
        LocalDate today = LocalDate.now(clock);
        LocalDate due = DueDates.effectiveDueDate(i);
        long daysOverdue = i.getStatus() == 1 ? DueDates.daysOverdue(i, today) : 0;

        IssueResponse r = new IssueResponse();
        r.setId(i.getId());
        r.setBookId(i.getBook().getId());
        r.setBookTitle(i.getBook().getTitle());
        r.setMemberId(i.getMember().getId());
        r.setMemberName(i.getMember().getName());
        r.setIssueDate(i.getIssueDate() != null ? i.getIssueDate().toString() : "");
        // legacy rows without a due_date now return the computed one instead of ""
        r.setDueDate(due != null ? due.toString() : "");
        r.setStatus(i.getStatus());
        r.setOverdue(daysOverdue > 0);
        r.setDaysOverdue(daysOverdue);
        return r;
    }

    public SubmissionResponse toSubmissionResponse(Submission s) {
        SubmissionResponse r = new SubmissionResponse();
        r.setId(s.getId());
        r.setBookTitle(s.getBook().getTitle());
        r.setMemberName(s.getMember().getName());
        r.setDaysKept(s.getDaysKept());
        r.setBillAmount(s.getBillAmount());
        r.setDamaged(s.isDamaged());
        r.setReturnDate(s.getReturnDate() != null ? s.getReturnDate().toString() : "");
        return r;
    }
}
