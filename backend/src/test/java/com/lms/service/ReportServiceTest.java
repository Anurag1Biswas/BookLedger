package com.lms.service;

import com.lms.dto.LibraryDTOs.IssueResponse;
import com.lms.dto.LibraryDTOs.MemberResponse;
import com.lms.dto.LibraryDTOs.SubmissionResponse;
import com.lms.dto.ReportDTOs.*;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock IssueRepository issueRepository;
    @Mock SubmissionRepository submissionRepository;
    @Mock BookRepository bookRepository;
    @Mock MemberRepository memberRepository;
    @Mock IssueService issueService;
    @Mock MemberService memberService;

    // "today" = 2026-10-02
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneOffset.UTC);
    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(issueRepository, submissionRepository, bookRepository,
                memberRepository, issueService, memberService, clock);
    }

    private Issue loan(long id, String member, LocalDateTime issued, LocalDate due, double bookPrice) {
        Book b = new Book();
        b.setId(id * 10);
        b.setTitle("Book " + id);
        b.setPrice(bookPrice);
        Member m = new Member();
        m.setId(id * 100);
        m.setName(member);
        m.setPhone("900000000" + id);
        Issue i = new Issue();
        i.setId(id);
        i.setBook(b);
        i.setMember(m);
        i.setStatus(1);
        i.setIssueDate(issued);
        i.setDueDate(due);
        return i;
    }

    @Test
    void overdue_listsOnlyLateLoans_mostOverdueFirst_withAccruedFee() {
        Issue a = loan(1, "Asha", LocalDateTime.of(2026, 9, 12, 9, 0), LocalDate.of(2026, 9, 22), 100); // 10 days late
        Issue b = loan(2, "Bimal", LocalDateTime.of(2026, 9, 28, 9, 0), LocalDate.of(2026, 10, 8), 100); // not late
        Issue c = loan(3, "Chitra", LocalDateTime.of(2026, 9, 1, 9, 0), null, 100);                    // legacy row, 21 days late
        when(issueRepository.findByStatus(1)).thenReturn(List.of(a, b, c));

        List<OverdueIssue> result = service.overdue();

        assertEquals(2, result.size());
        assertEquals("Chitra", result.get(0).memberName());
        assertEquals(21, result.get(0).daysOverdue());
        assertEquals("Asha", result.get(1).memberName());
        assertEquals(10, result.get(1).daysOverdue());
        // Asha kept the book 20 days -> 30 + 20 * (20 - 10) = 230
        assertEquals(230.0, result.get(1).accruedFee(), 0.0001);
    }

    @Test
    void dueSoon_includesToday_excludesOverdueAndFarFuture() {
        Issue dueToday = loan(1, "Today", LocalDateTime.of(2026, 9, 22, 9, 0), LocalDate.of(2026, 10, 2), 50);
        Issue dueIn3 = loan(2, "In3", LocalDateTime.of(2026, 9, 25, 9, 0), LocalDate.of(2026, 10, 5), 50);
        Issue dueIn4 = loan(3, "In4", LocalDateTime.of(2026, 9, 26, 9, 0), LocalDate.of(2026, 10, 6), 50);
        Issue late = loan(4, "Late", LocalDateTime.of(2026, 9, 1, 9, 0), LocalDate.of(2026, 9, 11), 50);
        when(issueRepository.findByStatus(1)).thenReturn(List.of(dueIn4, late, dueIn3, dueToday));

        List<OverdueIssue> result = service.dueSoon(3);

        assertEquals(List.of("Today", "In3"), result.stream().map(OverdueIssue::memberName).toList());
        assertEquals(0, result.get(0).daysOverdue());
    }

    @Test
    void popularBooks_mapsAggregateRows() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{7L, "Dune", "Frank Herbert", 5L});
        rows.add(new Object[]{8L, "Emma", "Jane Austen", 2L});
        when(issueRepository.findPopularBooks(any(Pageable.class))).thenReturn(rows);

        List<PopularBook> result = service.popularBooks(5);

        assertEquals(2, result.size());
        assertEquals("Dune", result.get(0).title());
        assertEquals(5, result.get(0).timesIssued());
    }

    @Test
    void activity_isZeroFilledOldestFirst() {
        Issue today = loan(1, "A", LocalDateTime.of(2026, 10, 2, 8, 0), LocalDate.of(2026, 10, 12), 10);
        Submission yesterdayReturn = new Submission();
        yesterdayReturn.setReturnDate(LocalDateTime.of(2026, 10, 1, 15, 0));
        when(issueRepository.findByIssueDateGreaterThanEqual(any(LocalDateTime.class))).thenReturn(List.of(today));
        when(submissionRepository.findByReturnDateGreaterThanEqual(any(LocalDateTime.class)))
                .thenReturn(List.of(yesterdayReturn));

        List<DailyActivity> result = service.activity(3);

        assertEquals(3, result.size());
        assertEquals(LocalDate.of(2026, 9, 30), result.get(0).date());
        assertEquals(0, result.get(0).issued() + result.get(0).returned());
        assertEquals(1, result.get(1).returned());
        assertEquals(1, result.get(2).issued());
    }

    @Test
    void overview_combinesCountsFromRepositories() {
        when(memberRepository.countByActiveTrue()).thenReturn(12L);
        when(bookRepository.count()).thenReturn(40L);
        when(bookRepository.countByQuantityGreaterThan(0)).thenReturn(35L);
        when(issueRepository.countByStatus(1)).thenReturn(6L);
        when(submissionRepository.count()).thenReturn(90L);
        when(submissionRepository.sumBillAmount()).thenReturn(1850.0);
        when(submissionRepository.countByDamagedTrue()).thenReturn(3L);
        when(issueRepository.findByStatus(1)).thenReturn(List.of(
                loan(1, "Late", LocalDateTime.of(2026, 9, 1, 9, 0), LocalDate.of(2026, 9, 11), 10)));

        OverviewStats s = service.overview();

        assertEquals(12, s.totalMembers());
        assertEquals(40, s.totalTitles());
        assertEquals(35, s.availableTitles());
        assertEquals(6, s.activeIssues());
        assertEquals(90, s.totalReturns());
        assertEquals(1, s.overdueCount());
        assertEquals(1850.0, s.feesCollected(), 0.0001);
        assertEquals(3, s.damagedReturns());
    }

    @Test
    void inventory_labelsMissingGenreAndSumsStock() {
        List<Object[]> genres = new ArrayList<>();
        genres.add(new Object[]{"Fantasy", 3L, 9L});
        genres.add(new Object[]{null, 1L, 2L});
        when(bookRepository.stockByGenre()).thenReturn(genres);
        when(bookRepository.count()).thenReturn(4L);
        when(bookRepository.sumQuantity()).thenReturn(11L);
        when(bookRepository.inventoryValue()).thenReturn(5000.0);

        InventorySummary inv = service.inventory();

        assertEquals(11, inv.totalCopies());
        assertEquals("Uncategorised", inv.byGenre().get(1).genre());
        assertEquals(5000.0, inv.inventoryValue(), 0.0001);
    }

    @Test
    void memberActivity_countsOverdueAndFees() {
        MemberResponse member = new MemberResponse();
        member.setId(1L);
        member.setName("Asha");
        IssueResponse late = new IssueResponse();
        late.setOverdue(true);
        IssueResponse ok = new IssueResponse();
        SubmissionResponse older = new SubmissionResponse();
        older.setReturnDate("2026-09-01T10:00");
        SubmissionResponse newer = new SubmissionResponse();
        newer.setReturnDate("2026-09-20T10:00");
        when(memberService.getMemberById(1L)).thenReturn(member);
        when(issueService.getMemberIssues(1L)).thenReturn(List.of(late, ok));
        when(issueService.getMemberSubmissions(1L)).thenReturn(List.of(older, newer));
        when(issueRepository.countByMember_Id(1L)).thenReturn(5L);
        when(submissionRepository.countByMember_Id(1L)).thenReturn(3L);
        when(submissionRepository.sumBillAmountByMember(1L)).thenReturn(120.0);

        MemberActivity a = service.memberActivity(1L);

        assertEquals(1, a.overdueCount());
        assertEquals(5, a.totalBorrowed());
        assertEquals(120.0, a.totalFees(), 0.0001);
        assertEquals("2026-09-20T10:00", a.returns().get(0).getReturnDate()); // newest first
    }
}
