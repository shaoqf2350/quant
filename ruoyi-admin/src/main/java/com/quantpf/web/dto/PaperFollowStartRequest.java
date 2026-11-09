package com.quantpf.web.dto;

public record PaperFollowStartRequest(Long accountId, Long strategyId, String mode) {}