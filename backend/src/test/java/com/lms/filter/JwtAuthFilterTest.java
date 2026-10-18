package com.lms.filter;

import com.lms.config.AdminUserDetails;
import com.lms.config.AdminUserDetailsService;
import com.lms.model.AdminUser;
import com.lms.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    private static final String SECRET = "filter-test-secret-filter-test-secret-123456";

    @Mock AdminUserDetailsService userDetailsService;

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000);
    private JwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest("GET", "/api/books");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AdminUserDetails libUser() {
        AdminUser u = new AdminUser();
        u.setId(2L);
        u.setUsername("lib");
        u.setPassword("hash");
        u.setRole(AdminUser.Role.LIBRARIAN);
        return new AdminUserDetails(u);
    }

    private void run() throws Exception {
        filter.doFilter(request, response, chain);
    }

    @Test
    void noAuthorizationHeader_continuesUnauthenticated() throws Exception {
        run();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest(), "the request must still reach the rest of the chain");
    }

    @Test
    void nonBearerHeader_isIgnored() throws Exception {
        request.addHeader("Authorization", "Basic abc123");
        run();

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void validToken_authenticatesWithTheRoleFromTheDatabase() throws Exception {
        // the token claims ADMIN, but the database says LIBRARIAN -> the database wins
        String token = jwtService.generateToken(java.util.Map.of("role", "ADMIN"), "lib");
        when(userDetailsService.loadUserByUsername("lib")).thenReturn(libUser());
        request.addHeader("Authorization", "Bearer " + token);

        run();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("lib", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("LIBRARIAN")));
        assertFalse(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN")));
        assertNotNull(chain.getRequest());
    }

    @Test
    void expiredToken_doesNotThrow_andStaysUnauthenticated() throws Exception {
        JwtService shortLived = new JwtService(SECRET, 1);
        String token = shortLived.generateToken("lib");
        Thread.sleep(50);
        request.addHeader("Authorization", "Bearer " + token);

        assertDoesNotThrow(this::run);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void garbageToken_doesNotThrow_andStaysUnauthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer this.is.not.a.token");

        assertDoesNotThrow(this::run);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }

    @Test
    void emptyBearerValue_doesNotThrow() throws Exception {
        request.addHeader("Authorization", "Bearer ");

        assertDoesNotThrow(this::run);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void validTokenForUserWhoNoLongerExists_isRejected() throws Exception {
        String token = jwtService.generateToken("ghost");
        when(userDetailsService.loadUserByUsername("ghost")).thenThrow(new UsernameNotFoundException("gone"));
        request.addHeader("Authorization", "Bearer " + token);

        assertDoesNotThrow(this::run);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
    }
}
