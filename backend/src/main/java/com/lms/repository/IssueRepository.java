package com.lms.repository;

import com.lms.model.Issue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByBook_IdAndMember_IdAndStatus(Long bookId, Long memberId, int status);
    //SELECT * FROM issues
    //WHERE book_id = ?
    //AND member_id = ?
    //AND status = ?
    List<Issue> findByMember_IdAndStatus(Long memberId, int status);
    //SELECT * FROM issues
    //WHERE book_id = ?
    //AND member_id = ?
    //AND status = ?
    List<Issue> findByStatus(int status);
    //SELECT * FROM issues WHERE status = ?
    long countByMember_IdAndStatus(Long memberId, int status);
    //SELECT COUNT(*) FROM issues
    //WHERE member_id = ?
    //AND status = ?

    // ---- additions for reports ----
    long countByStatus(int status);

    long countByMember_Id(Long memberId);

    boolean existsByBook_Id(Long bookId);

    List<Issue> findByIssueDateGreaterThanEqual(LocalDateTime from);

    long countByIssueDateGreaterThanEqual(LocalDateTime from);

    /** rows of [bookId, title, author, timesIssued], most issued first. Every loan (active or returned) is one row in issues. */
    @Query("SELECT b.id, b.title, b.author, COUNT(i) FROM Issue i JOIN i.book b "
            + "GROUP BY b.id, b.title, b.author ORDER BY COUNT(i) DESC, b.title ASC")
    List<Object[]> findPopularBooks(Pageable pageable);
}
