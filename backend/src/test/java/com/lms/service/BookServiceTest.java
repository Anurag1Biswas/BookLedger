package com.lms.service;

import com.lms.dto.LibraryDTOs.BookRequest;
import com.lms.dto.LibraryDTOs.BookResponse;
import com.lms.dto.PageResponse;
import com.lms.exception.BusinessRuleException;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.model.Book;
import com.lms.repository.BookRepository;
import com.lms.repository.IssueRepository;
import com.lms.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock BookRepository bookRepository;
    @Mock IssueRepository issueRepository;
    @Mock SubmissionRepository submissionRepository;
    @InjectMocks BookService service;

    private Book book(long id, int qty, String status) {
        Book b = new Book();
        b.setId(id);
        b.setTitle("Dune");
        b.setAuthor("Frank Herbert");
        b.setGenre("Science Fiction");
        b.setPrice(420);
        b.setQuantity(qty);
        b.setStatus(status);
        return b;
    }

    private BookRequest request(int qty) {
        BookRequest r = new BookRequest();
        r.setTitle("Dune");
        r.setAuthor("Frank Herbert");
        r.setGenre("Science Fiction");
        r.setPrice(420);
        r.setQuantity(qty);
        return r;
    }

    @Test
    void addBook_existingTitleAndAuthor_addsCopiesInsteadOfCreatingDuplicate() {
        Book existing = book(1, 2, "Available");
        when(bookRepository.findByTitleIgnoreCaseAndAuthorIgnoreCase("Dune", "Frank Herbert"))
                .thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookResponse res = service.addBook(request(3));

        assertEquals(5, res.getQuantity());
        assertEquals(1L, res.getId());
    }

    @Test
    void addBook_newBook_isAvailable() {
        when(bookRepository.findByTitleIgnoreCaseAndAuthorIgnoreCase(any(), any())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookResponse res = service.addBook(request(4));

        assertEquals("Available", res.getStatus());
        assertEquals(4, res.getQuantity());
    }

    @Test
    void updateBook_restockingAnUnavailableBookMakesItAvailableAgain() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1, 0, "Unavailable")));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("Available", service.updateBook(1L, request(4)).getStatus());
    }

    @Test
    void updateBook_settingStockToZeroMarksItUnavailable() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1, 3, "Available")));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("Unavailable", service.updateBook(1L, request(0)).getStatus());
    }

    @Test
    void getBookById_missing_throwsNotFound() {
        when(bookRepository.findById(9L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getBookById(9L));
        assertTrue(ex.getMessage().contains("9"));
    }

    @Test
    void deleteById_missing_returns404() {
        when(bookRepository.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<String> res = service.deleteById(9L);

        assertEquals(404, res.getStatusCode().value());
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_withLoanHistory_isConflict() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1, 1, "Available")));
        when(issueRepository.existsByBook_Id(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.deleteById(1L));
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void deleteById_withReturnHistoryOnly_isConflict() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1, 1, "Available")));
        when(issueRepository.existsByBook_Id(1L)).thenReturn(false);
        when(submissionRepository.existsByBook_Id(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.deleteById(1L));
    }

    @Test
    void deleteById_unusedBook_isDeleted() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1, 1, "Available")));
        when(issueRepository.existsByBook_Id(1L)).thenReturn(false);
        when(submissionRepository.existsByBook_Id(1L)).thenReturn(false);

        assertEquals(200, service.deleteById(1L).getStatusCode().value());
        verify(bookRepository).deleteById(1L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchPage_mapsPageMetadata() {
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(book(1, 2, "Available"))));

        PageResponse<BookResponse> res = service.searchPage("dune", null, null, 0, 10, "title,asc");

        assertEquals(1, res.content().size());
        assertEquals(1, res.totalElements());
        assertEquals("Dune", res.content().get(0).getTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchPage_rejectsUnknownSortField_beforeTouchingTheDatabase() {
        assertThrows(BusinessRuleException.class, () -> service.searchPage(null, null, null, 0, 10, "secret,asc"));
        verify(bookRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
