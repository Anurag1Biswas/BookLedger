package com.lms.util;

import com.lms.model.Issue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class DueDates {

    private DueDates() {
    }

    /**
     * due_date is nullable in the existing schema. For legacy rows without one we apply the
     * library's standard 10-day loan period to the issue date. Returns null only if both are missing.
     */
    public static LocalDate effectiveDueDate(Issue issue) {
        if (issue.getDueDate() != null) {
            return issue.getDueDate();
        }
        if (issue.getIssueDate() != null) {
            return issue.getIssueDate().toLocalDate().plusDays(BillingCalculator.INCLUDED_DAYS);
        }
        return null;
    }

    /** Whole days past the due date (0 when not overdue or unknown). */
    public static long daysOverdue(Issue issue, LocalDate today) {
        LocalDate due = effectiveDueDate(issue);
        if (due == null || !today.isAfter(due)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(due, today);
    }
}
