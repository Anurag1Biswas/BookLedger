package com.lms.integration;

import com.lms.model.AdminUser;
import com.lms.repository.AdminUserRepository;
import com.lms.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static com.lms.integration.IntegrationTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The real filter chain end to end: JWT filter + database-backed users + authorization rules. */
@SpringBootTest(properties = "app.security.enforce-roles=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AdminUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwtService;
    @Value("${jwt.secret}") String secret;

    private String createUser(AdminUser.Role role) {
        String name = "sec-" + UUID.randomUUID().toString().substring(0, 8);
        AdminUser u = new AdminUser();
        u.setUsername(name);
        u.setPassword(encoder.encode("password-123"));
        u.setRole(role);
        users.save(u);
        return name;
    }

    private static String book() {
        return "{\"title\":\"Sec Book " + UUID.randomUUID() + "\",\"author\":\"A\",\"price\":1,\"quantity\":1}";
    }

    @Test
    void validToken_isAccepted() throws Exception {
        mvc.perform(get("/api/books").header("Authorization", bearer(mvc, "librarian", "lib123")))
                .andExpect(status().isOk());
    }

    @Test
    void noToken_returns401() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedToken_returns401_notAServerError() throws Exception {
        String token = bearer(mvc, "librarian", "lib123");
        mvc.perform(get("/api/books").header("Authorization", token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void garbageToken_returns401() throws Exception {
        mvc.perform(get("/api/books").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        JwtService shortLived = new JwtService(secret, 1);
        String token = shortLived.generateToken(Map.of("role", "LIBRARIAN"), "librarian");
        Thread.sleep(50);

        mvc.perform(get("/api/books").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenOnAPublicUrl_doesNotBreakLogin() throws Exception {
        mvc.perform(post("/api/auth/login").header("Authorization", "Bearer garbage")
                        .contentType("application/json")
                        .content("{\"username\":\"librarian\",\"password\":\"lib123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void tokenOfADeletedUser_isRejectedImmediately() throws Exception {
        String name = createUser(AdminUser.Role.LIBRARIAN);
        String token = bearer(mvc, name, "password-123");
        mvc.perform(get("/api/books").header("Authorization", token)).andExpect(status().isOk());

        users.delete(users.findByUsername(name).orElseThrow());

        mvc.perform(get("/api/books").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeInTheDatabase_appliesToAnExistingToken() throws Exception {
        String name = createUser(AdminUser.Role.LIBRARIAN);
        String token = bearer(mvc, name, "password-123");

        // librarian may create books
        mvc.perform(post("/api/books").header("Authorization", token)
                .contentType("application/json").content(book())).andExpect(status().isOk());

        // demote/move to ADMIN: same token, but the catalogue is now off limits
        AdminUser u = users.findByUsername(name).orElseThrow();
        u.setRole(AdminUser.Role.ADMIN);
        users.save(u);

        mvc.perform(post("/api/books").header("Authorization", token)
                .contentType("application/json").content(book())).andExpect(status().isForbidden());
    }

    @Test
    void forgingARoleClaim_doesNotGrantAccess() throws Exception {
        // a validly signed token that CLAIMS ADMIN for a user who is really a librarian
        String forged = jwtService.generateToken(Map.of("role", "ADMIN"), "librarian");
        mvc.perform(post("/api/members").header("Authorization", "Bearer " + forged)
                        .contentType("application/json")
                        .content("{\"name\":\"X\",\"address\":\"Y\",\"memberType\":\"Student\"}"))
                .andExpect(status().isForbidden());
    }
}
