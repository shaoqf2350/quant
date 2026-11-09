package com.quantpf.web.controller;

import com.quantpf.execution.PaperOrderApplicationService;
import com.quantpf.finance.PaperAccountApplicationService;
import com.quantpf.risk.PaperRiskApplicationService;

final class PaperApiRuntime {
    static final PaperAccountApplicationService ACCOUNTS = new PaperAccountApplicationService();
    static final PaperRiskApplicationService RISK = new PaperRiskApplicationService();
    static final PaperOrderApplicationService ORDERS = new PaperOrderApplicationService(RISK.gate());
    private PaperApiRuntime() {}
}