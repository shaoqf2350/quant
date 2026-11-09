package com.quantpf.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Isolated Spring MVC contract test for the static Phase 0C health endpoint.
 *
 * <p>This test intentionally uses standalone MockMvc instead of the production
 * application context. It must not connect to a database, Redis, broker,
 * scheduler or live gateway.</p>
 */
class QuantPfHealthControllerWebMvcTest {

    private final MockMvc mockMvc = standaloneSetup(new QuantPfHealthController()).build();

    @Test
    void healthReturnsStaticPhase0cPayload() throws Exception {
        mockMvc.perform(get("/api/quant/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.phase").value("PHASE0C"))
            .andExpect(jsonPath("$.data.liveEnabled").value(false));
    }
}