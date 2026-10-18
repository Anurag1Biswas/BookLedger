package com.lms.service;

import com.lms.dto.LibraryDTOs.IssueRequest;
import com.lms.dto.LibraryDTOs.IssueResponse;
import com.lms.dto.LibraryDTOs.SubmissionResponse;
import com.lms.dto.LibraryDTOs.SubmitRequest;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.kafka.BookEvent;
import com.lms.kafka.BookEventType;
import com.lms.model.Book;
import com.lms.model.Issue;
import com.lms.model.Member;
import com.lms.model.Submission;
import com.lms.repository.BookRepository;
import com.lms.repository.IssueRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Mock IssueRepository issueRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock BookRepository bookRepository;
    @Mock MemberRepository memberRepository;
    @Mock ApplicationEventPublisher publisher;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private IssueService service;

    @BeforeEach
    void setUp() {
        service = new IssueService(issueRepository, submissionRepository, bookRepository,
                memberRepository, publisher, clock);
    }

    private Member member(long id, boolean active, int booksIssued) {
        Member m = new Member();
        m.setId(id);
        m.setName("Asha Rao");
        m.setAddress("Kolkata");
        m.setActive(active);
        m.setBooksIssued(booksIssued);
        return m;
    }

    private Book book(long id, int qty, String status, double price) {
        Book b = new Book();
        b.setId(id);
        b.setTitle("Dune");
        b.setAuthor("Frank Herbert");
        b.setQuantity(qty);
        b.setStatus(status);
        b.setPrice(price);
        return b;
    }

    private IssueRequest issueRequest(long memberId, long bookId) {
        IssueRequest r = new IssueRequest();
        r.setMemberId(memberId);
        r.setBookId(bookId);
        return r;
    }

    private SubmitRequest submitRequest(long memberId, long bookId, boolean damaged) {
        SubmitRequest r = new SubmitRequest();
        r.setMemberId(memberId);
        r.setBookId(bookId);
        r.setDamaged(damaged);
        return r;
    }

    private void stubActiveIssue(Member m, Book b, int daysAgo) {
        Issue issue = new Issue();
        issue.setId(50L);
        issue.setMember(m);
        issue.setBook(b);
        issue.setStatus(1);
        issue.setIssueDate(LocalDateTime.now(clock).minusDays(daysAgo));
        issue.setDueDate(LocalDate.now(clock).minusDays(daysAgo).plusDays(10));
        when(memberRepository.findById(m.getId())).thenReturn(Optional.of(m));
        when(bookRepository.findById(b.getId())).thenReturn(Optional.of(b));
        when(issueRepository.findByBook_IdAndMember_IdAndStatus(b.getId(), m.getId(), 1))
                .thenReturn(Optional.of(issue));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- issueBook ----------
    @Test
    void issueBook_decrementsStock_countsLoan_setsTenDayDueDate_andPublishesEvent() {
        Member m = member(1, true, 0);
        Book b = book(2, 3, "Available", 400);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(b));
        when(issueRepository.findByBook_IdAndMember_IdAndStatus(2L, 1L, 1)).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

        IssueResponse res = service.issueBook(issueRequest(1, 2));

        assertEquals(2, b.getQuantity());
        assertEquals("Available", b.getStatus());
        assertEquals(1, m.getBooksIssued());
        assertEquals("2026-10-12", res.getDueDate());
        assertFalse(res.isOverdue());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        BookEvent event = (BookEvent) captor.getValue();
        assertEquals(BookEventType.BOOK_ISSUED, event.type());
        assertEquals(1L, event.memberId());
        assertEquals("Dune", event.bookTitle());
        assertEquals(LocalDate.of(2026, 10, 12), event.dueDate());
        assertNotNull(event.eventId());
    }

    @Test
    void issueBook_lastCopyMarksBookUnavailable() {
        Member m = member(1, true, 0);
        Book b = book(2, 1, "Available", 100);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(b));
        when(issueRepository.findByBook_IdAndMember_IdAndStatus(2L, 1L, 1)).thenReturn(Optional.empty());
        when(issueRepository.save(any(Issue.class))).thenAnswer(inv -> inv.getArgument(0));

        service.issueBook(issueRequest(1, 2));

        assertEquals(0, b.getQuantity());
        assertEquals("Unavailable", b.getStatus());
    }

    @Test
    void issueBook_unknownMember_throwsNotFound_andPublishesNothing() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.issueBook(issueRequest(1, 2)));
        verify(publisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void issueBook_unknownBook_throwsNotFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, true, 0)));
        when(bookRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.issueBook(issueRequest(1, 2)));
    }

    @Test
    void issueBook_inactiveMember_isRejected() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, false, 0)));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book(2, 3, "Available", 100)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.issueBook(issueRequest(1, 2)));
        assertEquals("Member is not active", ex.getMessage());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void issueBook_sameBookTwiceToSameMember_isRejected() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, true, 1)));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book(2, 3, "Available", 100)));
        when(issueRepository.findByBook_IdAndMember_IdAndStatus(2L, 1L, 1)).thenReturn(Optional.of(new Issue()));

        assertThrows(BusinessRuleException.class, () -> service.issueBook(issueRequest(1, 2)));
        verify(issueRepository, never()).save(any(Issue.class));
    }

    @Test
    void issueBook_outOfStock_isRejectedWithOriginalMessage() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, true, 0)));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book(2, 0, "Unavailable", 100)));
        when(issueRepository.findByBook_IdAndMember_IdAndStatus(2L, 1L, 1)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.issueBook(issueRequest(1, 2)));
        assertEquals("Book is currently not available", ex.getMessage());
    }

    // ---------- submitBook ----------
    @Test
    void submitBook_withinTenDays_chargesFlatFee_restocks_andPublishesReturnEvent() {
        Member m = member(1, true, 1);
        Book b = book(2, 0, "Unavailable", 400);
        stubActiveIssue(m, b, 5);

        SubmissionResponse res = service.submitBook(submitRequest(1, 2, false));

        assertEquals(30.0, res.getBillAmount(), 0.0001);
        assertEquals(5, res.getDaysKept());
        assertEquals(1, b.getQuantity());
        assertEquals("Available", b.getStatus());
        assertEquals(0, m.getBooksIssued());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        BookEvent event = (BookEvent) captor.getValue();
        assertEquals(BookEventType.BOOK_RETURNED, event.type());
        assertEquals(30.0, event.billAmount(), 0.0001);
        assertFalse(event.damaged());
    }

    @Test
    void submitBook_fifteenDaysKept_addsTwentyPerExtraDay() {
        stubActiveIssue(member(1, true, 1), book(2, 0, "Unavailable", 400), 15);

        assertEquals(130.0, service.submitBook(submitRequest(1, 2, false)).getBillAmount(), 0.0001);
    }

    @Test
    void submitBook_damaged_addsBookPrice() {
        stubActiveIssue(member(1, true, 1), book(2, 0, "Unavailable", 400), 5);

        SubmissionResponse res = service.submitBook(submitRequest(1, 2, true));

        assertEquals(430.0, res.getBillAmount(), 0.0001);
        assertTrue(res.isDamaged());
    }

    @Test
    void submitBook_memberCounterNeverGoesNegative() {
        Member m = member(1, true, 0);
        stubActiveIssue(m, book(2, 0, "Unavailable", 100), 1);

        service.submitBook(submitRequest(1, 2, false));

        assertEquals(0, m.getBooksIssued());
    }

    @Test
    void submitBook_withoutActiveIssue_isRejected() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, true, 0)));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book(2, 1, "Available", 100)));
        when(issueRepository.findByBook_IdAndMember_IdAndStatus(2L, 1L, 1)).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submitBook(submitRequest(1, 2, false)));
        assertEquals("No active issue found for this member and book", ex.getMessage());
        verify(publisher, never()).publishEvent(any(Object.class));
    }

    // ---------- response mapping ----------
    @Test
    void toIssueResponse_flagsOverdueLoans_andFillsMissingDueDateForLegacyRows() {
        Issue legacy = new Issue();
        legacy.setMember(member(1, true, 1));
        legacy.setBook(book(2, 0, "Unavailable", 100));
        legacy.setStatus(1);
        legacy.setIssueDate(LocalDateTime.of(2026, 9, 12, 9, 0));
        legacy.setDueDate(null);

        IssueResponse r = service.toIssueResponse(legacy);

        assertEquals("2026-09-22", r.getDueDate());
        assertTrue(r.isOverdue());
        assertEquals(10, r.getDaysOverdue());
    }

    @Test
    void toIssueResponse_returnedLoanIsNeverOverdue() {
        Issue returned = new Issue();
        returned.setMember(member(1, true, 0));
        returned.setBook(book(2, 1, "Available", 100));
        returned.setStatus(0);
        returned.setIssueDate(LocalDateTime.of(2026, 8, 1, 9, 0));
        returned.setDueDate(LocalDate.of(2026, 8, 11));

        assertFalse(service.toIssueResponse(returned).isOverdue());
    }
}
