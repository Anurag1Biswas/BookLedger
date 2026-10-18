package com.lms.service;

import com.lms.dto.LibraryDTOs.MemberRequest;
import com.lms.dto.LibraryDTOs.MemberResponse;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.model.Member;
import com.lms.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock MemberRepository memberRepository;
    @InjectMocks MemberService service;

    private Member member(long id, int booksIssued) {
        Member m = new Member();
        m.setId(id);
        m.setName("Asha Rao");
        m.setAddress("Kolkata");
        m.setBooksIssued(booksIssued);
        m.setActive(true);
        return m;
    }

    @Test
    void getMemberById_missing_throwsNotFound() {
        when(memberRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getMemberById(5L));
    }

    @Test
    void addMember_copiesAllFields() {
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        MemberRequest req = new MemberRequest();
        req.setName("Ravi");
        req.setAddress("Howrah");
        req.setPhone("9000000000");
        req.setMemberType("Student");

        MemberResponse res = service.addMember(req);

        assertEquals("Ravi", res.getName());
        assertEquals("Student", res.getMemberType());
        assertTrue(res.isActive());
    }

    @Test
    void deleteMember_withBooksOut_isRejected() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member(1, 2)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.deleteMember(1L));
        assertEquals("Member has books to return before cancelling membership", ex.getMessage());
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void deleteMember_isASoftDelete() {
        Member m = member(1, 0);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(m));

        String msg = service.deleteMember(1L);

        assertFalse(m.isActive());
        assertTrue(msg.contains("1"));
        verify(memberRepository).save(m);
    }

    @Test
    void searchMembers_returnsRepositoryResultsAsDtos() {
        when(memberRepository.findByNameContainingIgnoreCaseAndActiveTrue("asha"))
                .thenReturn(List.of(member(1, 0)));
        assertEquals(1, service.searchMembers("asha").size());
    }

    @Test
    void searchPage_rejectsUnknownSortField() {
        assertThrows(BusinessRuleException.class, () -> service.searchPage(null, null, 0, 10, "address,asc"));
    }
}
