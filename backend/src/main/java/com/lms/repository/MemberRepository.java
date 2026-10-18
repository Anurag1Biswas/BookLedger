package com.lms.repository;

import com.lms.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
    List<Member> findByActiveTrue();//SELECT * FROM lib_members WHERE active = true

    @Query("SELECT m FROM Member m WHERE m.booksIssued > 0 AND m.active = true")
    List<Member> findMembersWithOverdueBooks();
    //SELECT * FROM lib_members
    //WHERE books_issued > 0
    //AND active = true

    List<Member> findByNameContainingIgnoreCaseAndActiveTrue(String name);
    //SELECT * FROM lib_members
    //WHERE LOWER(name) LIKE LOWER(CONCAT('%', ?, '%'))
    //AND active = true

    long countByActiveTrue();
}
