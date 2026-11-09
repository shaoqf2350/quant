package com.quantpf.web.controller;

import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * QuantPF Phase 0C static health endpoint.
 *
 * <p>This endpoint intentionally has no database, cache, broker, scheduler,
 * secret or business dependency. It is a skeleton marker, not a business
 * readiness signal.</p>
 */
@RestController
@RequestMapping("/api/quant/health")
public class QuantPfHealthController {

    @GetMapping
    public R<Map<String, Object>> health() {
        return R.ok(Map.of(
            "status", "UP",
            "phase", "PHASE0C",
            "liveEnabled", false
        ));
    }
}
