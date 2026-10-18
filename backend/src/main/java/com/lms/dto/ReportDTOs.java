package com.lms.dto;

import com.lms.dto.LibraryDTOs.IssueResponse;
import com.lms.dto.LibraryDTOs.MemberResponse;
import com.lms.dto.LibraryDTOs.SubmissionResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Read-only DTOs for the reporting endpoints under /api/reports. */
public final class ReportDTOs {

    private ReportDTOs() {
    }

    public record OverviewStats(
            long totalMembers,
            long totalTitles,
            long availableTitles,
            long activeIssues,
            long totalReturns,
            long overdueCount,
            long dueSoonCount,
            long issuedLast30Days,
            long returnedLast30Days,
            double feesCollected,
            long damagedReturns) {
    }

    public record OverdueIssue(
            Long issueId,
            Long bookId,
            String bookTitle,
            Long memberId,
            String memberName,
            String memberPhone,
            LocalDateTime issueDate,
            LocalDate dueDate,
            long daysOverdue,
            double accruedFee) {
    }

    public record PopularBook(Long bookId, String title, String author, long timesIssued) {
    }

    public record DailyActivity(LocalDate date, long issued, long returned) {
    }

    public record GenreStock(String genre, long titles, long copies) {
    }

    public record InventorySummary(
            long titles,
            long totalCopies,
            long copiesOnLoan,
            long outOfStockTitles,
            long lowStockTitles,
            double inventoryValue,
            List<GenreStock> byGenre) {
    }

    public record MemberActivity(
            MemberResponse member,
            long totalBorrowed,
            long totalReturned,
            long overdueCount,
            double totalFees,
            List<IssueResponse> activeIssues,
            List<SubmissionResponse> returns) {
    }

    public record NotificationResponse(
            Long id,
            String eventType,
            Long memberId,
            Long bookId,
            String message,
            LocalDateTime createdAt,
            boolean read) {
    }

    public record UnreadCount(long unread) {
    }
}
