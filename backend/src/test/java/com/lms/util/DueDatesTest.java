package com.lms.util;

import com.lms.model.Issue;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DueDatesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    private Issue issue(LocalDateTime issued, LocalDate due) {
        Issue i = new Issue();
        i.setIssueDate(issued);
        i.setDueDate(due);
        return i;
    }

    @Test
    void usesStoredDueDateWhenPresent() {
        Issue i = issue(LocalDateTime.of(2026, 9, 1, 9, 0), LocalDate.of(2026, 9, 25));
        assertEquals(LocalDate.of(2026, 9, 25), DueDates.effectiveDueDate(i));
        assertEquals(7, DueDates.daysOverdue(i, TODAY));
    }

    @Test
    void legacyRowWithoutDueDateFallsBackToIssueDatePlusTenDays() {
        Issue i = issue(LocalDateTime.of(2026, 9, 1, 9, 0), null);
        assertEquals(LocalDate.of(2026, 9, 11), DueDates.effectiveDueDate(i));
        assertEquals(21, DueDates.daysOverdue(i, TODAY));
    }

    @Test
    void notOverdueOnTheDueDateItself() {
        Issue i = issue(LocalDateTime.of(2026, 9, 22, 9, 0), TODAY);
        assertEquals(0, DueDates.daysOverdue(i, TODAY));
    }

    @Test
    void unknownWhenBothDatesMissing() {
        Issue i = issue(null, null);
        assertNull(DueDates.effectiveDueDate(i));
        assertEquals(0, DueDates.daysOverdue(i, TODAY));
    }
}
