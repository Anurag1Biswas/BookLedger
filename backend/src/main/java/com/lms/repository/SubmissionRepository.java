package com.lms.repository;

import com.lms.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByMember_Id(Long memberId);
    //SELECT * FROM submissions WHERE member_id = ?
    List<Submission> findByBook_Id(Long bookId);
    //SELECT * FROM submissions WHERE book_id = ?

    // ---- additions for reports ----
    boolean existsByBook_Id(Long bookId);

    long countByMember_Id(Long memberId);

    long countByDamagedTrue();

    long countByReturnDateGreaterThanEqual(LocalDateTime from);

    List<Submission> findByReturnDateGreaterThanEqual(LocalDateTime from);

    @Query("SELECT COALESCE(SUM(s.billAmount), 0.0) FROM Submission s")
    double sumBillAmount();

    @Query("SELECT COALESCE(SUM(s.billAmount), 0.0) FROM Submission s WHERE s.member.id = :memberId")
    double sumBillAmountByMember(@Param("memberId") Long memberId);
}
