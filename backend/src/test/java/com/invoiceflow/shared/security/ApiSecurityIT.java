package com.invoiceflow.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceflow.support.IntegrationTest;
import com.invoiceflow.support.TestJwt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class ApiSecurityIT {

    @Autowired
    MockMvc mvc;

    @Test
    void resolvesTheTenantFromTheTokenAndIgnoresClientSuppliedTenants() throws Exception {
        mvc.perform(bearer(get("/api/v1/me").param("tenantId", "globex").header("X-Tenant-Id", "globex"),
                        TestJwt.token("alice", "acme", "ACCOUNTANT", "NOT_A_ROLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("alice"))
                .andExpect(jsonPath("$.tenantId").value("acme"))
                .andExpect(jsonPath("$.roles[0]").value("ACCOUNTANT"))
                .andExpect(jsonPath("$.roles.length()").value(1));
    }

    @Test
    void rejectsRequestsWithoutATokenAsProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:invoiceflow:problem:unauthenticated"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/api/v1/me"));
    }

    @Test
    void rejectsTokensWithoutATenant() throws Exception {
        mvc.perform(bearer(get("/api/v1/me"), TestJwt.tokenWithoutTenant("alice", "OWNER")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsTokensWithAMalformedTenant() throws Exception {
        mvc.perform(bearer(get("/api/v1/me"), TestJwt.token("alice", "acme/../globex", "OWNER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTamperedTokens() throws Exception {
        String token = TestJwt.token("alice", "acme", "OWNER");
        mvc.perform(bearer(get("/api/v1/me"), token.substring(0, token.length() - 2) + "xx"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void appliesTheRoleHierarchy() throws Exception {
        mvc.perform(bearer(get("/api/v1/test-probes/accounting"), TestJwt.token("o", "acme", "OWNER")))
                .andExpect(status().isOk());
        mvc.perform(bearer(get("/api/v1/test-probes/accounting"), TestJwt.token("a", "acme", "ACCOUNTANT")))
                .andExpect(status().isOk());
        mvc.perform(bearer(get("/api/v1/test-probes/read"), TestJwt.token("s", "acme", "SALES")))
                .andExpect(status().isOk());
        mvc.perform(bearer(get("/api/v1/test-probes/accounting"), TestJwt.token("s", "acme", "SALES")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:invoiceflow:problem:forbidden"));
        mvc.perform(bearer(get("/api/v1/test-probes/accounting"), TestJwt.token("v", "acme", "VIEWER")))
                .andExpect(status().isForbidden());
        mvc.perform(bearer(get("/api/v1/test-probes/read"), TestJwt.token("n", "acme")))
                .andExpect(status().isForbidden());
    }

    @Test
    void servesControllersOnlyUnderTheVersionedPrefix() throws Exception {
        String token = TestJwt.token("alice", "acme", "OWNER");
        mvc.perform(bearer(get("/me"), token)).andExpect(status().isForbidden());
        mvc.perform(bearer(get("/api/v1/does-not-exist"), token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void exposesLivenessWithoutAuthentication() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder bearer(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
