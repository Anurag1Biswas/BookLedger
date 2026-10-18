package com.lms.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.lms.integration.IntegrationTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With ENFORCE_ROLES=true the API itself applies the same split as the Angular route guards. */
@SpringBootTest(properties = {"app.security.enforce-roles=true",
        "app.security.password-hash-endpoint-enabled=false"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleEnforcementTest {

    @Autowired MockMvc mvc;

    private static final String MEMBER = "{\"name\":\"Role Test\",\"address\":\"X\",\"memberType\":\"Student\"}";
    private static final String BOOK = "{\"title\":\"Role Book\",\"author\":\"A\",\"price\":1,\"quantity\":1}";

    @Test
    void librarianCannotManageMembers_butCanLookThemUp() throws Exception {
        String lib = bearer(mvc, "librarian", "lib123");
        mvc.perform(post("/api/members").header("Authorization", lib).contentType("application/json").content(MEMBER))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/members").header("Authorization", lib)).andExpect(status().isOk());
    }

    @Test
    void adminCannotChangeTheCatalogue_butCanReadIt() throws Exception {
        String admin = bearer(mvc, "admin", "admin123");
        mvc.perform(post("/api/books").header("Authorization", admin).contentType("application/json").content(BOOK))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/books").header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void adminCanCreateMembers_librarianCanCreateBooks() throws Exception {
        mvc.perform(post("/api/members").header("Authorization", bearer(mvc, "admin", "admin123"))
                .contentType("application/json").content(MEMBER)).andExpect(status().isOk());
        mvc.perform(post("/api/books").header("Authorization", bearer(mvc, "librarian", "lib123"))
                .contentType("application/json").content(BOOK)).andExpect(status().isOk());
    }

    @Test
    void passwordHashEndpointCanBeSwitchedOff() throws Exception {
        mvc.perform(post("/api/auth/getPassword").contentType("application/json").content("{\"password\":\"x\"}"))
                .andExpect(status().isNotFound());
    }
}
