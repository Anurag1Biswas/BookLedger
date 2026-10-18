package com.lms.service;

import com.lms.dto.LibraryDTOs.IssueResponse;
import com.lms.dto.LibraryDTOs.MemberResponse;
import com.lms.dto.LibraryDTOs.SubmissionResponse;
import com.lms.dto.ReportDTOs.*;
import com.lms.model.Issue;
import com.lms.repository.BookRepository;
import com.lms.repository.IssueRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.util.BillingCalculator;
import com.lms.util.DueDates;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only reporting built from the existing tables (no new data is stored for reports). */
@Service
@Transactional(readOnly = true)
public class ReportService {

    static final int LOW_STOCK_THRESHOLD = 2;
    static final int DUE_SOON_DAYS = 3;

    private final IssueRepository issueRepository;
    private final SubmissionRepository submissionRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final IssueService issueService;
    private final MemberService memberService;
    private final Clock clock;

    public ReportService(IssueRepository issueRepository,
                         SubmissionRepository submissionRepository,
                         BookRepository bookRepository,
                         MemberRepository memberRepository,
                         IssueService issueService,
                         MemberService memberService,
                         Clock clock) {
        this.issueRepository = issueRepository;
        this.submissionRepository = submissionRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.issueService = issueService;
        this.memberService = memberService;
        this.clock = clock;
    }

    public OverviewStats overview() {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(30);
        return new OverviewStats(
                memberRepository.countByActiveTrue(),
                bookRepository.count(),
                bookRepository.countByQuantityGreaterThan(0),
                issueRepository.countByStatus(1),
                submissionRepository.count(),
                overdue().size(),
                dueSoon(DUE_SOON_DAYS).size(),
                issueRepository.countByIssueDateGreaterThanEqual(since),
                submissionRepository.countByReturnDateGreaterThanEqual(since),
                submissionRepository.sumBillAmount(),
                submissionRepository.countByDamagedTrue());
    }

    /** Active loans whose due date is before today, most overdue first. */
    public List<OverdueIssue> overdue() {
        LocalDate today = LocalDate.now(clock);
        return issueRepository.findByStatus(1).stream()
                .filter(i -> DueDates.daysOverdue(i, today) > 0)
                .map(i -> toOverdue(i, today))
                .sorted(Comparator.comparingLong(OverdueIssue::daysOverdue).reversed()
                        .thenComparing(OverdueIssue::issueId))
                .toList();
    }

    /** Active loans due within the next {@code days} days (including today) that are not yet overdue. */
    public List<OverdueIssue> dueSoon(int days) {
        int window = Math.min(Math.max(days, 0), 30);
        LocalDate today = LocalDate.now(clock);
        LocalDate until = today.plusDays(window);
        return issueRepository.findByStatus(1).stream()
                .filter(i -> {
                    LocalDate due = DueDates.effectiveDueDate(i);
                    return due != null && !due.isBefore(today) && !due.isAfter(until);
                })
                .map(i -> toOverdue(i, today))
                .sorted(Comparator.comparing(OverdueIssue::dueDate).thenComparing(OverdueIssue::issueId))
                .toList();
    }

    public List<PopularBook> popularBooks(int limit) {
        int safe = Math.min(Math.max(limit, 1), 50);
        List<PopularBook> result = new ArrayList<>();
        for (Object[] row : issueRepository.findPopularBooks(PageRequest.of(0, safe))) {
            result.add(new PopularBook((Long) row[0], (String) row[1], (String) row[2], ((Number) row[3]).longValue()));
        }
        return result;
    }

    /** One entry per day (oldest first) for the last {@code days} days, zero-filled. */
    public List<DailyActivity> activity(int days) {
        int window = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now(clock);
        LocalDate first = today.minusDays(window - 1L);
        LocalDateTime from = first.atStartOfDay();

        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < window; i++) {
            buckets.put(first.plusDays(i), new long[2]);
        }
        issueRepository.findByIssueDateGreaterThanEqual(from).forEach(i -> {
            if (i.getIssueDate() != null) {
                long[] b = buckets.get(i.getIssueDate().toLocalDate());
                if (b != null) b[0]++;
            }
        });
        submissionRepository.findByReturnDateGreaterThanEqual(from).forEach(s -> {
            if (s.getReturnDate() != null) {
                long[] b = buckets.get(s.getReturnDate().toLocalDate());
                if (b != null) b[1]++;
            }
        });
        List<DailyActivity> result = new ArrayList<>();
        buckets.forEach((date, b) -> result.add(new DailyActivity(date, b[0], b[1])));
        return result;
    }

    public InventorySummary inventory() {
        List<GenreStock> byGenre = new ArrayList<>();
        for (Object[] row : bookRepository.stockByGenre()) {
            String genre = row[0] == null || ((String) row[0]).isBlank() ? "Uncategorised" : (String) row[0];
            byGenre.add(new GenreStock(genre, ((Number) row[1]).longValue(), ((Number) row[2]).longValue()));
        }
        return new InventorySummary(
                bookRepository.count(),
                bookRepository.sumQuantity(),
                issueRepository.countByStatus(1),
                bookRepository.countByQuantityLessThanEqual(0),
                bookRepository.countByQuantityBetween(1, LOW_STOCK_THRESHOLD),
                bookRepository.inventoryValue(),
                byGenre);
    }

    public MemberActivity memberActivity(Long memberId) {
        MemberResponse member = memberService.getMemberById(memberId); // 404 if unknown
        List<IssueResponse> active = issueService.getMemberIssues(memberId);
        List<SubmissionResponse> returns = new ArrayList<>(issueService.getMemberSubmissions(memberId));
        returns.sort(Comparator.comparing(SubmissionResponse::getReturnDate).reversed());
        return new MemberActivity(
                member,
                issueRepository.countByMember_Id(memberId),
                submissionRepository.countByMember_Id(memberId),
                active.stream().filter(IssueResponse::isOverdue).count(),
                submissionRepository.sumBillAmountByMember(memberId),
                active,
                returns.stream().limit(20).toList());
    }

    private OverdueIssue toOverdue(Issue i, LocalDate today) {
        long daysOverdue = DueDates.daysOverdue(i, today);
        long daysKept = i.getIssueDate() == null ? 0
                : ChronoUnit.DAYS.between(i.getIssueDate().toLocalDate(), today);
        return new OverdueIssue(
                i.getId(),
                i.getBook().getId(), i.getBook().getTitle(),
                i.getMember().getId(), i.getMember().getName(), i.getMember().getPhone(),
                i.getIssueDate(), DueDates.effectiveDueDate(i),
                daysOverdue,
                // fee if it were returned today, undamaged (same policy as the return flow)
                BillingCalculator.calculate(daysKept, false, i.getBook().getPrice()));
    }
}
