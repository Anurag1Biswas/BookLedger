package com.lms.repository;

import com.lms.model.Book;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Optional filters are added only when supplied (avoids null-parameter typing problems on PostgreSQL). */
public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> search(String query, String genre, Boolean available) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like)));
            }
            if (genre != null && !genre.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("genre")), genre.trim().toLowerCase()));
            }
            if (available != null) {
                predicates.add(available
                        ? cb.greaterThan(root.<Integer>get("quantity"), 0)
                        : cb.lessThanOrEqualTo(root.<Integer>get("quantity"), 0));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
