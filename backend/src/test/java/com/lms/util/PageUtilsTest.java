package com.lms.util;

import com.lms.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PageUtilsTest {

    private static final Map<String, String> ALLOWED = Map.of("title", "title", "id", "id");

    @Test
    void buildsPageableWithRequestedSortAndStableTieBreaker() {
        Pageable p = PageUtils.toPageable(2, 10, "title,desc", ALLOWED);
        assertEquals(2, p.getPageNumber());
        assertEquals(10, p.getPageSize());
        assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("title").getDirection());
        assertNotNull(p.getSort().getOrderFor("id"));
    }

    @Test
    void clampsPageAndSize() {
        Pageable p = PageUtils.toPageable(-5, 100000, "title", ALLOWED);
        assertEquals(0, p.getPageNumber());
        assertEquals(PageUtils.MAX_PAGE_SIZE, p.getPageSize());
        assertEquals(Sort.Direction.ASC, p.getSort().getOrderFor("title").getDirection());
    }

    @Test
    void rejectsFieldsThatAreNotWhitelisted() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> PageUtils.toPageable(0, 10, "password,asc", ALLOWED));
        assertTrue(ex.getMessage().contains("password"));
    }

    @Test
    void rejectsUnknownDirection() {
        assertThrows(BusinessRuleException.class, () -> PageUtils.toPageable(0, 10, "title,sideways", ALLOWED));
    }
}
