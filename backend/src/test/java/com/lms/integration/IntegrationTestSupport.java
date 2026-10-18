package com.lms.integration;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Shared helpers for the full-context tests (H2 + the real security filter chain). */
final class IntegrationTestSupport {

    private IntegrationTestSupport() {
    }

    /** Logs in with the accounts created by DataSeeder (test profile) and returns "Bearer ...". */
    static String bearer(MockMvc mvc, String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    static long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}
