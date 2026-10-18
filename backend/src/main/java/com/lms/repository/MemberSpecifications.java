package com.lms.repository;

import com.lms.model.Member;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class MemberSpecifications {

    private MemberSpecifications() {
    }

    /** Active members only (soft-deleted members stay hidden, like the existing endpoints). */
    public static Specification<Member> activeMatching(String query, String memberType) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.<Boolean>get("active")));
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("address")), like),
                        cb.like(cb.lower(root.get("phone")), like)));
            }
            if (memberType != null && !memberType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("memberType")), memberType.trim().toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
