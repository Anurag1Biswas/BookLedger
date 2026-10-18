package com.lms.config;

import com.lms.model.*;
import com.lms.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class DataSeeder implements CommandLineRunner {

    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.seed.librarian-password:lib123}")
    private String librarianPassword;

    @Override
    public void run(String... args) {
        // Seed admin users
        if (adminUserRepository.count() == 0) {
            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(AdminUser.Role.ADMIN);

            AdminUser librarian = new AdminUser();
            librarian.setUsername("librarian");
            librarian.setPassword(passwordEncoder.encode(librarianPassword));
            librarian.setRole(AdminUser.Role.LIBRARIAN);

            adminUserRepository.save(admin);
            adminUserRepository.save(librarian);
        }

        // Seed sample members
        if (memberRepository.count() == 0) {
            memberRepository.save(createMember("Avipso Sinha", "Bangaon, West Bengal", "9876543210", "Student"));
            memberRepository.save(createMember("Krishna Roy", "77, Dharmatala", "9837263672", "Businessman"));
            memberRepository.save(createMember("Priya Das", "Salt Lake, Kolkata", "9123456789", "Teacher"));
        }

        // Seed sample books
        if (bookRepository.count() == 0) {
            bookRepository.save(createBook("Alice in Wonderland", "Lewis Carroll", "Fantasy", 400, 3));
            bookRepository.save(createBook("Harry Potter and the Sorcerer's Stone", "J K Rowling", "Fantasy", 200, 5));
            bookRepository.save(createBook("Sherlock Holmes", "Arthur Conan Doyle", "Thriller", 450, 3));
            bookRepository.save(createBook("The Great Gatsby", "F. Scott Fitzgerald", "Classic", 350, 2));
            bookRepository.save(createBook("To Kill a Mockingbird", "Harper Lee", "Classic", 300, 4));
            bookRepository.save(createBook("1984", "George Orwell", "Dystopian", 280, 3));
        }
    }

    private Member createMember(String name, String address, String phone, String type) {
        Member m = new Member();
        m.setName(name);
        m.setAddress(address);
        m.setPhone(phone);
        m.setMemberType(type);
        m.setBooksIssued(0);
        m.setActive(true);
        return m;
    }

    private Book createBook(String title, String author, String genre, double price, int qty) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);
        b.setPrice(price);
        b.setQuantity(qty);
        b.setStatus("Available");
        return b;
    }
}
