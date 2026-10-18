package com.lms.service;

import com.lms.config.AdminUserDetails;
import com.lms.model.AdminUser;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    static final String SECRET = "unit-test-secret-unit-test-secret-1234567890";
    private final JwtService jwt = new JwtService(SECRET, 3_600_000);

    private AdminUserDetails user(String name, AdminUser.Role role) {
        AdminUser u = new AdminUser();
        u.setId(1L);
        u.setUsername(name);
        u.setPassword("hash");
        u.setRole(role);
        return new AdminUserDetails(u);
    }

    @Test
    void tokenRoundTrip_carriesUsernameAndRole() {
        String token = jwt.generateToken(Map.of("role", "LIBRARIAN"), "lib");

        assertEquals("lib", jwt.extractUsername(token));
        assertEquals("LIBRARIAN", jwt.extractRole(token));
        assertTrue(jwt.extractExpiration(token).getTime() > System.currentTimeMillis());
        assertTrue(jwt.validateToken(token, user("lib", AdminUser.Role.LIBRARIAN)));
    }

    @Test
    void tokenWithoutExtraClaims_isStillValid() {
        String token = jwt.generateToken("admin");
        assertTrue(jwt.validateToken(token, user("admin", AdminUser.Role.ADMIN)));
    }

    @Test
    void tokenForAnotherUser_isRejected() {
        String token = jwt.generateToken("admin");
        assertFalse(jwt.validateToken(token, user("someone-else", AdminUser.Role.ADMIN)));
    }

    @Test
    void expiredToken_isRejected_andReportedAsExpired() throws Exception {
        JwtService shortLived = new JwtService(SECRET, 1);   // 1 ms
        String token = shortLived.generateToken("admin");
        Thread.sleep(50);

        assertTrue(shortLived.isTokenExpired(token));
        assertFalse(shortLived.validateToken(token, user("admin", AdminUser.Role.ADMIN)));
        assertThrows(ExpiredJwtException.class, () -> shortLived.extractUsername(token));
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwt.generateToken("admin");
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

        assertFalse(jwt.validateToken(tampered, user("admin", AdminUser.Role.ADMIN)));
        assertThrows(JwtException.class, () -> jwt.extractUsername(tampered));
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        String foreign = new JwtService("a-completely-different-secret-0123456789abcdef", 3_600_000).generateToken("admin");
        assertFalse(jwt.validateToken(foreign, user("admin", AdminUser.Role.ADMIN)));
    }

    @Test
    void garbageToken_isRejectedWithoutThrowing() {
        assertFalse(jwt.validateToken("not.a.jwt", user("admin", AdminUser.Role.ADMIN)));
        assertFalse(jwt.validateToken("", user("admin", AdminUser.Role.ADMIN)));
    }

    @Test
    void weakOrMissingSecret_failsAtStartup() {
        IllegalStateException weak = assertThrows(IllegalStateException.class, () -> new JwtService("too-short", 1000));
        assertTrue(weak.getMessage().contains("JWT_SECRET"));
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 1000));
    }

    @Test
    void nonPositiveLifetime_failsAtStartup() {
        assertThrows(IllegalStateException.class, () -> new JwtService(SECRET, 0));
    }
}
