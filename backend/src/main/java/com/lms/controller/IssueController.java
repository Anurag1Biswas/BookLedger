package com.lms.controller;

import com.lms.dto.LibraryDTOs.*;
import com.lms.repository.BookRepository;
import com.lms.repository.IssueRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.service.BookService;
import com.lms.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class IssueController {

    @Autowired private IssueService issueService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private BookService bookService;

    @PostMapping("/issue")
    public ResponseEntity<?> issueBook(@Valid @RequestBody IssueRequest req) {
        try {
            return ResponseEntity.ok(issueService.issueBook(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitBook(@Valid @RequestBody SubmitRequest req) {
        try {
            return ResponseEntity.ok(issueService.submitBook(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/issues")
    public List<IssueResponse> getAllIssues() {
        return issueService.getAllActiveIssues();
    }

    @GetMapping("/issues/member/{memberId}")
    public List<IssueResponse> getMemberIssues(@PathVariable Long memberId) {
        return issueService.getMemberIssues(memberId);
    }

    @GetMapping("/submissions")
    public List<SubmissionResponse> getAllSubmissions() {
        return issueService.getAllSubmissions();
    }

    @GetMapping("/submissions/member/{memberId}")
    public List<SubmissionResponse> getMemberSubmissions(@PathVariable Long memberId) {
        return issueService.getMemberSubmissions(memberId);
    }

    @GetMapping("/dashboard")
    public DashboardStats getDashboard() {
        long totalMembers = memberRepository.findByActiveTrue().size();
        long totalBooks = bookRepository.count();
        long activeIssues = issueService.getActiveIssueCount();
        long totalSubmissions = submissionRepository.count();
        long availableBooks = bookRepository.findByStatus("Available").stream()
                .filter(b -> b.getQuantity() > 0).count();
        return new DashboardStats(totalMembers, totalBooks, activeIssues, totalSubmissions, availableBooks);
    }
}
