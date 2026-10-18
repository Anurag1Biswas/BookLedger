package com.lms.service;

import com.lms.dto.LibraryDTOs.*;
import com.lms.dto.PageResponse;
import com.lms.exception.ConflictException;
import com.lms.exception.ResourceNotFoundException;
import com.lms.model.Book;
import com.lms.repository.BookRepository;
import com.lms.repository.BookSpecifications;
import com.lms.repository.IssueRepository;
import com.lms.repository.SubmissionRepository;
import com.lms.util.PageUtils;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Autowired private BookRepository bookRepository;
    @Autowired private IssueRepository issueRepository;
    @Autowired private SubmissionRepository submissionRepository;

    /** public sort field -> entity property */
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id", "title", "title", "author", "author",
            "genre", "genre", "price", "price", "quantity", "quantity");

    public List<Book> getAllBooks() {
//        return bookRepository.findAll().stream()
//                .map(this::toResponse).collect(Collectors.toList());
//        List<Book> result = new ArrayList<>();
        List<Book> books = bookRepository.findAll();
        return books;
    }

    public List<BookResponse> getAvailableBooks() {
        return bookRepository.findByStatus("Available").stream()
                .filter(b -> b.getQuantity() > 0)
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<Book> getUnAvailableBooks() {
//        return bookRepository.findByStatus("Available").stream()
//                .filter(b -> b.getQuantity() > 0)
//                .map(this::toResponse).collect(Collectors.toList());
        List<Book> unavailableBooks = bookRepository.findByStatus("Unavailable");
        return unavailableBooks;
    }

    public BookResponse getBookById(Long id) {
        return bookRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    public BookResponse addBook(BookRequest req) {
        Optional<Book> existing = bookRepository
                .findByTitleIgnoreCaseAndAuthorIgnoreCase(req.getTitle(), req.getAuthor());
        if (existing.isPresent()) {
            Book book = existing.get();
            book.setQuantity(book.getQuantity() + req.getQuantity());
            return toResponse(bookRepository.save(book));
        }
        Book book = new Book();
        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setGenre(req.getGenre());
        book.setPrice(req.getPrice());
        book.setQuantity(req.getQuantity());
        book.setStatus("Available");
        return toResponse(bookRepository.save(book));
    }

    public BookResponse updateBook(Long id, BookRequest req) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        book.setTitle(req.getTitle());
        book.setAuthor(req.getAuthor());
        book.setGenre(req.getGenre());
        book.setPrice(req.getPrice());
        book.setQuantity(req.getQuantity());
        // keep status consistent with stock (previously a restocked book stayed "Unavailable")
        book.setStatus(req.getQuantity() > 0 ? "Available" : "Unavailable");
        return toResponse(bookRepository.save(book));
    }

    public List<BookResponse> searchBooks(String query) {
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(query, query)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PageResponse<BookResponse> searchPage(String query, String genre, Boolean available,
                                                 int page, int size, String sort) {
        Page<Book> result = bookRepository.findAll(
                BookSpecifications.search(query, genre, available),
                PageUtils.toPageable(page, size, sort, SORT_FIELDS));
        return PageResponse.from(result, this::toResponse);
    }

    public List<String> getGenres() {
        return bookRepository.findDistinctGenres();
    }

    public BookResponse toResponse(Book b) {
        BookResponse r = new BookResponse();
        r.setId(b.getId());
        r.setTitle(b.getTitle());
        r.setAuthor(b.getAuthor());
        r.setGenre(b.getGenre());
        r.setPrice(b.getPrice());
        r.setQuantity(b.getQuantity());
        r.setStatus(b.getStatus());
        return r;
    }

    public ResponseEntity<String> deleteById(long id) {
        if (bookRepository.findById(id).isEmpty()) {
            return ResponseEntity.status(404).build();
        }
        if (issueRepository.existsByBook_Id(id) || submissionRepository.existsByBook_Id(id)) {
            throw new ConflictException("Book has issue/return history and cannot be deleted");
        }
        bookRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
