package com.quantpf.web.controller;

import com.quantpf.finance.PersistentPaperRiskApplicationService;
import com.quantpf.risk.PaperRiskApplicationService;
import com.quantpf.web.dto.RiskReasonRequest;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quant/risk")
public class QuantPaperRiskController {
    private final PersistentPaperRiskApplicationService persistentService;
    private final PaperRiskApplicationService standaloneService;

    @Autowired
    public QuantPaperRiskController(PersistentPaperRiskApplicationService persistentService) {
        this.persistentService = persistentService;
        this.standaloneService = null;
    }

    /** Compatibility constructor used only by standalone MVC tests. */
    QuantPaperRiskController() {
        this.persistentService = null;
        this.standaloneService = PaperApiRuntime.RISK;
    }

    @GetMapping("/status")
    public R<?> status() {
        return R.ok(persistentService != null ? persistentService.status() : standaloneService.status());
    }

    @PostMapping("/halt")
    public R<?> halt(@RequestBody RiskReasonRequest request) {
        return R.ok(persistentService != null ? persistentService.halt(request.reason()) : standaloneService.halt(request.reason()));
    }

    @PostMapping("/resume")
    public R<?> resume(@RequestBody RiskReasonRequest request) {
        return R.ok(persistentService != null ? persistentService.resume(request.reason()) : standaloneService.resume(request.reason()));
    }
}
