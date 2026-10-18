package com.lms.controller;

import com.lms.dto.LibraryDTOs.*;
import com.lms.dto.PageResponse;
import com.lms.model.Book;
import com.lms.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired private BookService bookService;

    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    @GetMapping("/available")
    public List<BookResponse> getAvailableBooks() {
        return bookService.getAvailableBooks();
    }

    @GetMapping("/unavailable")
    public List<Book> getUnAvailableBooks() {
        return bookService.getUnAvailableBooks();
    }

    /** Paged / filtered / sorted catalogue. sort = field,asc|desc (id, title, author, genre, price, quantity). */
    @GetMapping("/page")
    public PageResponse<BookResponse> page(@RequestParam(required = false) String query,
                                           @RequestParam(required = false) String genre,
                                           @RequestParam(required = false) Boolean available,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "12") int size,
                                           @RequestParam(defaultValue = "title,asc") String sort) {
        return bookService.searchPage(query, genre, available, page, size, sort);
    }

    @GetMapping("/genres")
    public List<String> genres() {
        return bookService.getGenres();
    }

    @GetMapping("/{id}")
    public BookResponse getBook(@PathVariable Long id) {
        return bookService.getBookById(id);
    }

    @PostMapping
    public BookResponse addBook(@Valid @RequestBody BookRequest req) {
        return bookService.addBook(req);
    }

    @PutMapping("/{id}")
    public BookResponse updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest req) {
        return bookService.updateBook(id, req);
    }

    @GetMapping("/search")
    public List<BookResponse> searchBooks(@RequestParam String query) {
        return bookService.searchBooks(query);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable long id){
        // exceptions (404 / 409) are translated by GlobalExceptionHandler
        return bookService.deleteById(id);
    }
}
