package com.lms.controller;

import com.lms.dto.ReportDTOs.*;
import com.lms.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only reporting endpoints. Available to every authenticated user (same as /api/dashboard). */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    public OverviewStats overview() {
        return reportService.overview();
    }

    @GetMapping("/overdue")
    public List<OverdueIssue> overdue() {
        return reportService.overdue();
    }

    @GetMapping("/due-soon")
    public List<OverdueIssue> dueSoon(@RequestParam(defaultValue = "3") int days) {
        return reportService.dueSoon(days);
    }

    @GetMapping("/popular-books")
    public List<PopularBook> popularBooks(@RequestParam(defaultValue = "5") int limit) {
        return reportService.popularBooks(limit);
    }

    @GetMapping("/activity")
    public List<DailyActivity> activity(@RequestParam(defaultValue = "14") int days) {
        return reportService.activity(days);
    }

    @GetMapping("/inventory")
    public InventorySummary inventory() {
        return reportService.inventory();
    }

    @GetMapping("/members/{id}")
    public MemberActivity memberActivity(@PathVariable Long id) {
        return reportService.memberActivity(id);
    }
}
