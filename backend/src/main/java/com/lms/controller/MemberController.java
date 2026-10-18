package com.lms.controller;

import com.lms.dto.LibraryDTOs.*;
import com.lms.dto.PageResponse;
import com.lms.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    @Autowired private MemberService memberService;

    @GetMapping
    public List<MemberResponse> getAllMembers() {
        return memberService.getAllMembers();
    }

    /** Paged / filtered / sorted member list. sort = field,asc|desc (id, name, memberType, booksIssued, createdAt). */
    @GetMapping("/page")
    public PageResponse<MemberResponse> page(@RequestParam(required = false) String query,
                                             @RequestParam(required = false) String memberType,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(defaultValue = "name,asc") String sort) {
        return memberService.searchPage(query, memberType, page, size, sort);
    }

    @GetMapping("/{id}")
    public MemberResponse getMember(@PathVariable Long id) {
        return memberService.getMemberById(id);
    }

    @PostMapping
    public MemberResponse addMember(@Valid @RequestBody MemberRequest req) {
        return memberService.addMember(req);
    }

    @PutMapping("/{id}")
    public MemberResponse updateMember(@PathVariable Long id, @Valid @RequestBody MemberRequest req) {
        return memberService.updateMember(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMember(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(memberService.deleteMember(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public List<MemberResponse> searchMembers(@RequestParam String name) {
        return memberService.searchMembers(name);
    }
}
