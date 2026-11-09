package com.quantpf.web.controller;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QuantPaperApiStandaloneTest {
    private final MockMvc mockMvc = standaloneSetup(
        new QuantPaperAccountController(),
        new QuantPaperOrderController(),
        new QuantPaperRiskController(),
        new QuantPaperAuditController(),
        new QuantPaperJobExecutionController()
    ).build();

    @Test @Order(1)
    void accountPageReturnsPaperAccount() throws Exception {
        mockMvc.perform(get("/api/quant/account/page"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].accountCode").value("PAPER-DEMO"));
    }
    @Test @Order(2)
    void orderRequiresIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/quant/order/paper").contentType(MediaType.APPLICATION_JSON).content(orderJson("client-missing")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }
    @Test @Order(3)
    void paperOrderIsAcceptedAndFilledInMemory() throws Exception {
        mockMvc.perform(post("/api/quant/order/paper").header("Idempotency-Key", "api-test-idem-1").contentType(MediaType.APPLICATION_JSON).content(orderJson("client-1")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.status").value("FILLED"));
    }
    @Test @Order(4)
    void duplicateIdempotencyKeyIsRejected() throws Exception {
        mockMvc.perform(post("/api/quant/order/paper").header("Idempotency-Key", "api-test-idem-1").contentType(MediaType.APPLICATION_JSON).content(orderJson("client-duplicate")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(409));
    }
    @Test @Order(5)
    void riskHaltAndResumeArePaperOnly() throws Exception {
        mockMvc.perform(post("/api/quant/risk/halt").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test halt\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("HALTED"));
        mockMvc.perform(get("/api/quant/risk/status")).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("HALTED"));
        mockMvc.perform(post("/api/quant/risk/resume").contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"test resume\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLEAR"));
    }
    private static String orderJson(String clientOrderId) {
        return "{\"accountId\":1,\"instrumentId\":1,\"side\":\"BUY\",\"orderType\":\"LIMIT\",\"quantity\":2,\"limitPrice\":10,\"estimatedNotional\":20,\"fillPrice\":10,\"clientOrderId\":\"" + clientOrderId + "\",\"requestId\":\"api-request-1\"}";
    }
}
