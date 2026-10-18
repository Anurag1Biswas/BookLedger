package com.lms.util;

import com.lms.exception.BusinessRuleException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

public final class PageUtils {

    public static final int MAX_PAGE_SIZE = 100;

    private PageUtils() {
    }

    /**
     * Builds a safe Pageable.
     *
     * @param sort          "field,asc|desc" as sent by the client
     * @param allowedFields public API field name -> entity property name (whitelist)
     */
    public static Pageable toPageable(int page, int size, String sort, Map<String, String> allowedFields) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        String[] parts = (sort == null ? "" : sort).split(",");
        String field = parts[0].trim();
        String property = allowedFields.get(field);
        if (property == null) {
            throw new BusinessRuleException("Unsupported sort field '" + field + "'. Allowed: "
                    + String.join(", ", allowedFields.keySet()));
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            String dir = parts[1].trim();
            if (dir.equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            } else if (!dir.equalsIgnoreCase("asc")) {
                throw new BusinessRuleException("Sort direction must be 'asc' or 'desc'");
            }
        }
        // secondary sort on id keeps pagination stable when the primary key has ties
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property).and(Sort.by("id")));
    }
}
