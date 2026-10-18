package com.lms.integration;

import com.lms.model.Book;
import com.lms.model.Issue;
import com.lms.model.Member;
import com.lms.repository.BookRepository;
import com.lms.repository.IssueRepository;
import com.lms.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.lms.integration.IntegrationTestSupport.bearer;
import static com.lms.integration.IntegrationTestSupport.idOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end through controllers, security, services, repositories and H2.
 * Also pins the behaviour of the PRE-EXISTING endpoints so regressions are caught.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired BookRepository bookRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired IssueRepository issueRepository;

    private String admin;
    private String librarian;

    @BeforeEach
    void login() throws Exception {
        admin = bearer(mvc, "admin", "admin123");
        librarian = bearer(mvc, "librarian", "lib123");
    }

    private long createBook(String title, int qty) throws Exception {
        String json = "{\"title\":\"" + title + "\",\"author\":\"Test Author\",\"genre\":\"Testing\",\"price\":200,\"quantity\":" + qty + "}";
        return idOf(mvc.perform(post("/api/books").header("Authorization", librarian)
                        .contentType("application/json").content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long createMember(String name) throws Exception {
        String json = "{\"name\":\"" + name + "\",\"address\":\"Somewhere\",\"phone\":\"9000000000\",\"memberType\":\"Student\"}";
        return idOf(mvc.perform(post("/api/members").header("Authorization", admin)
                        .contentType("application/json").content(json))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    // ---------- authentication ----------
    @Test
    void login_withWrongPassword_returns401JsonNotA500() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void login_returnsTokenRoleAndUsername() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"librarian\",\"password\":\"lib123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("LIBRARIAN"))
                .andExpect(jsonPath("$.username").value("librarian"));
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_rejectsTooShortNewPassword() throws Exception {
        mvc.perform(put("/api/auth/change-password").header("Authorization", librarian)
                        .contentType("application/json")
                        .content("{\"oldPassword\":\"lib123\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    // ---------- validation / error shape ----------
    @Test
    void createBook_withInvalidBody_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/books").header("Authorization", librarian).contentType("application/json")
                        .content("{\"title\":\"\",\"author\":\"A\",\"quantity\":-1,\"price\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.quantity").exists());
    }

    @Test
    void unknownBook_returns404Json_andNeverLeaksStackTraces() throws Exception {
        mvc.perform(get("/api/books/987654").header("Authorization", librarian))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("987654")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Exception"))));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mvc.perform(post("/api/books").header("Authorization", librarian).contentType("application/json").content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid request"));
    }

    // ---------- pre-existing endpoints keep working ----------
    @Test
    void existingReadEndpoints_stillRespond() throws Exception {
        for (String path : new String[]{"/api/books", "/api/books/available", "/api/books/unavailable",
                "/api/books/search?query=a", "/api/members", "/api/members/search?name=a",
                "/api/issues", "/api/submissions", "/api/dashboard"}) {
            mvc.perform(get(path).header("Authorization", librarian)).andExpect(status().isOk());
        }
    }

    // ---------- full circulation flow ----------
    @Test
    void issueThenReturn_updatesStock_blocksDuplicates_andBillsFlatFee() throws Exception {
        long bookId = createBook("Flow Book " + UUID.randomUUID(), 1);
        long memberId = createMember("Flow Member");
        String body = "{\"bookId\":" + bookId + ",\"memberId\":" + memberId + "}";

        mvc.perform(post("/api/issue").header("Authorization", librarian).contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.status").value(1));

        // last copy -> now unavailable, so a second issue is refused with the original text message
        mvc.perform(post("/api/issue").header("Authorization", librarian).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/books/" + bookId).header("Authorization", librarian))
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.status").value("Unavailable"));

        mvc.perform(post("/api/submit").header("Authorization", librarian).contentType("application/json")
                        .content("{\"bookId\":" + bookId + ",\"memberId\":" + memberId + ",\"damaged\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billAmount").value(30.0));

        mvc.perform(get("/api/books/" + bookId).header("Authorization", librarian))
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.status").value("Available"));
    }

    @Test
    void issue_withMissingIds_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/issue").header("Authorization", librarian).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bookId").exists());
    }

    @Test
    void deleteBookWithHistory_returns409() throws Exception {
        long bookId = createBook("History Book " + UUID.randomUUID(), 2);
        long memberId = createMember("History Member");
        mvc.perform(post("/api/issue").header("Authorization", librarian).contentType("application/json")
                .content("{\"bookId\":" + bookId + ",\"memberId\":" + memberId + "}")).andExpect(status().isOk());

        mvc.perform(delete("/api/books/" + bookId).header("Authorization", librarian))
                .andExpect(status().isConflict());
    }

    // ---------- new APIs ----------
    @Test
    void bookPage_filtersSortsAndPaginates() throws Exception {
        String tag = "Paged" + UUID.randomUUID().toString().substring(0, 8);
        createBook(tag + " B", 1);
        createBook(tag + " A", 1);
        createBook(tag + " C", 0);

        mvc.perform(get("/api/books/page").header("Authorization", librarian)
                        .param("query", tag).param("sort", "title,asc").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value(tag + " A"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mvc.perform(get("/api/books/page").header("Authorization", librarian)
                        .param("query", tag).param("available", "false"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value(tag + " C"));
    }

    @Test
    void bookPage_withUnsupportedSortField_returns400() throws Exception {
        mvc.perform(get("/api/books/page").header("Authorization", librarian).param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Unsupported sort field")));
    }

    @Test
    void genres_listsDistinctGenres() throws Exception {
        createBook("Genre Book " + UUID.randomUUID(), 1);
        mvc.perform(get("/api/books/genres").header("Authorization", librarian))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Testing")));
    }

    @Test
    void memberPage_searchesByName() throws Exception {
        String name = "Pageable" + UUID.randomUUID().toString().substring(0, 8);
        createMember(name);
        mvc.perform(get("/api/members/page").header("Authorization", admin).param("query", name.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(name));
    }

    @Test
    void overdueReport_andIssueFlags_reflectPastDueLoans() throws Exception {
        Book b = new Book();
        b.setTitle("Late Book " + UUID.randomUUID());
        b.setAuthor("Someone");
        b.setQuantity(0);
        b.setStatus("Unavailable");
        b.setPrice(100);
        b = bookRepository.save(b);
        Member m = new Member();
        m.setName("Overdue Olive");
        m.setAddress("Nowhere");
        m.setActive(true);
        m.setBooksIssued(1);
        m = memberRepository.save(m);
        Issue i = new Issue();
        i.setBook(b);
        i.setMember(m);
        i.setStatus(1);
        i.setIssueDate(LocalDateTime.now().minusDays(20));
        i.setDueDate(LocalDate.now().minusDays(10));
        issueRepository.save(i);

        mvc.perform(get("/api/reports/overdue").header("Authorization", librarian))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.memberName=='Overdue Olive')].daysOverdue").isNotEmpty())
                .andExpect(jsonPath("$[?(@.memberName=='Overdue Olive')].accruedFee").isNotEmpty());

        mvc.perform(get("/api/issues").header("Authorization", librarian))
                .andExpect(jsonPath("$[?(@.memberName=='Overdue Olive')].overdue").value(org.hamcrest.Matchers.contains(true)));

        mvc.perform(get("/api/reports/members/" + m.getId()).header("Authorization", librarian))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overdueCount").value(1))
                .andExpect(jsonPath("$.activeIssues", hasSize(1)));
    }

    @Test
    void reports_overviewInventoryPopularAndActivity_respond() throws Exception {
        mvc.perform(get("/api/reports/overview").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalTitles").isNumber());
        mvc.perform(get("/api/reports/inventory").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.byGenre").isArray());
        mvc.perform(get("/api/reports/popular-books?limit=3").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/reports/activity?days=7").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(7)));
        mvc.perform(get("/api/reports/due-soon").header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void reports_unknownMember_returns404() throws Exception {
        mvc.perform(get("/api/reports/members/987654").header("Authorization", admin)).andExpect(status().isNotFound());
    }

    @Test
    void notifications_endpointsRespond_andMarkingUnknownIdIs404() throws Exception {
        mvc.perform(get("/api/notifications").header("Authorization", librarian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", librarian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unread").isNumber());
        mvc.perform(post("/api/notifications/read-all").header("Authorization", librarian))
                .andExpect(status().isOk()).andExpect(jsonPath("$.updated").isNumber());
        mvc.perform(patch("/api/notifications/987654/read").header("Authorization", librarian))
                .andExpect(status().isNotFound());
    }
}
