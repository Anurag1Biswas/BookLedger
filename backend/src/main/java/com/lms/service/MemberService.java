package com.lms.service;

import com.lms.dto.LibraryDTOs.*;
import com.lms.dto.PageResponse;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.model.Member;
import com.lms.repository.MemberRepository;
import com.lms.repository.MemberSpecifications;
import com.lms.util.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberService {

    @Autowired private MemberRepository memberRepository;

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id", "name", "name", "memberType", "memberType",
            "booksIssued", "booksIssued", "createdAt", "createdAt");

    public List<MemberResponse> getAllMembers() {
        return memberRepository.findByActiveTrue().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public MemberResponse getMemberById(Long id) {
        return memberRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + id));
    }

    public MemberResponse addMember(MemberRequest req) {
        Member member = new Member();
        member.setName(req.getName());
        member.setAddress(req.getAddress());
        member.setPhone(req.getPhone());
        member.setMemberType(req.getMemberType());
        return toResponse(memberRepository.save(member));
    }

    public MemberResponse updateMember(Long id, MemberRequest req) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        member.setName(req.getName());
        member.setAddress(req.getAddress());
        member.setPhone(req.getPhone());
        member.setMemberType(req.getMemberType());
        return toResponse(memberRepository.save(member));
    }

    public String deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (member.getBooksIssued() > 0) {
            throw new BusinessRuleException("Member has books to return before cancelling membership");
        }
        member.setActive(false);
        memberRepository.save(member);
        return "Member " + id + " successfully deleted from records";
    }

    public List<MemberResponse> searchMembers(String name) {
        return memberRepository.findByNameContainingIgnoreCaseAndActiveTrue(name)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PageResponse<MemberResponse> searchPage(String query, String memberType,
                                                   int page, int size, String sort) {
        Page<Member> result = memberRepository.findAll(
                MemberSpecifications.activeMatching(query, memberType),
                PageUtils.toPageable(page, size, sort, SORT_FIELDS));
        return PageResponse.from(result, this::toResponse);
    }

    private MemberResponse toResponse(Member m) {
        MemberResponse r = new MemberResponse();
        r.setId(m.getId());
        r.setName(m.getName());
        r.setAddress(m.getAddress());
        r.setPhone(m.getPhone());
        r.setMemberType(m.getMemberType());
        r.setBooksIssued(m.getBooksIssued());
        r.setActive(m.isActive());
        r.setCreatedAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : "");
        return r;
    }
}
