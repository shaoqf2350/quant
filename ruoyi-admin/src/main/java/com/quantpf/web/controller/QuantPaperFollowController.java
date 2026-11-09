package com.quantpf.web.controller;

import com.quantpf.domain.exception.QuantPfDomainException;
import com.quantpf.finance.PersistentPaperFollowApplicationService;
import com.quantpf.web.dto.PaperFollowStartRequest;
import com.quantpf.web.dto.PaperFollowStopRequest;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quant/paper")
public class QuantPaperFollowController {
    private final PersistentPaperFollowApplicationService persistentService;

    @Autowired
    public QuantPaperFollowController(PersistentPaperFollowApplicationService persistentService) {
        this.persistentService = persistentService;
    }

    @PostMapping("/follow-start")
    public R<?> start(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                      @RequestBody(required = false) PaperFollowStartRequest request) {
        if (persistentService == null) return R.fail(409, "Persistent Paper Follow path is required");
        if (idempotencyKey == null || idempotencyKey.isBlank()) return R.fail(400, "QPF-FOLLOW-004: Idempotency-Key is required");
        if (request == null) return R.fail(400, "QPF-REQ-001: Follow start body is required");
        try {
            return R.ok(persistentService.start(request.accountId(), request.strategyId(), request.mode(), idempotencyKey));
        } catch (QuantPfDomainException e) {
            return R.fail(httpStatus(e.code()), e.code() + ":" + e.getMessage());
        } catch (RuntimeException e) {
            return R.fail(500, "QPF-INTERNAL-001: Paper Follow persistence failed");
        }
    }

    @PostMapping("/follow-stop")
    public R<?> stop(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                     @RequestBody(required = false) PaperFollowStopRequest request) {
        if (persistentService == null) return R.fail(409, "Persistent Paper Follow path is required");
        if (idempotencyKey == null || idempotencyKey.isBlank()) return R.fail(400, "QPF-FOLLOW-004: Idempotency-Key is required");
        if (request == null) return R.fail(400, "QPF-REQ-001: Follow stop body is required");
        try {
            return R.ok(persistentService.stop(request.sessionId(), idempotencyKey, request.reason()));
        } catch (QuantPfDomainException e) {
            return R.fail(httpStatus(e.code()), e.code() + ":" + e.getMessage());
        } catch (RuntimeException e) {
            return R.fail(500, "QPF-INTERNAL-001: Paper Follow persistence failed");
        }
    }

    private static int httpStatus(String code) {
        return switch (code) {
            case "QPF-FOLLOW-002" -> 404;
            case "QPF-IDEMP-001", "QPF-FOLLOW-001", "QPF-FOLLOW-003" -> 409;
            case "QPF-FOLLOW-004", "QPF-REQ-001" -> 400;
            default -> 409;
        };
    }
}