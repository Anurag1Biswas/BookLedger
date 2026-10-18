package com.lms.repository;

import com.lms.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findByStatus(String status);
    //SELECT * FROM books_info WHERE status = ?
    Optional<Book> findByTitleIgnoreCaseAndAuthorIgnoreCase(String title, String author);
    //SELECT * FROM books_info
    //WHERE LOWER(title) = LOWER(?)
    //AND LOWER(author) = LOWER(?)
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);
    //SELECT * FROM books_info
    //WHERE LOWER(title) LIKE LOWER(CONCAT('%', ?, '%'))
    //OR LOWER(author) LIKE LOWER(CONCAT('%', ?, '%'))

    // ---- additions for catalogue filters and inventory reporting ----
    @Query("SELECT DISTINCT b.genre FROM Book b WHERE b.genre IS NOT NULL AND b.genre <> '' ORDER BY b.genre")
    List<String> findDistinctGenres();

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Book b")
    long sumQuantity();

    @Query("SELECT COALESCE(SUM(b.price * b.quantity), 0.0) FROM Book b")
    double inventoryValue();

    long countByQuantityGreaterThan(int quantity);

    long countByQuantityLessThanEqual(int quantity);

    long countByQuantityBetween(int from, int to);

    /** rows of [genre, titles, copies] */
    @Query("SELECT b.genre, COUNT(b), COALESCE(SUM(b.quantity), 0) FROM Book b GROUP BY b.genre ORDER BY b.genre")
    List<Object[]> stockByGenre();
}
