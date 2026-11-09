package com.quantpf.web.controller;

import com.quantpf.finance.PersistentAuditApplicationService;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quant/audit")
public class QuantPaperAuditController {
    private final PersistentAuditApplicationService persistentService;

    @Autowired
    public QuantPaperAuditController(PersistentAuditApplicationService persistentService) {
        this.persistentService = persistentService;
    }

    /** Compatibility constructor used only by the existing standalone MVC test. */
    QuantPaperAuditController() {
        this.persistentService = null;
    }

    @GetMapping("/page")
    public R<?> page(@RequestParam(defaultValue = "1") int pageNum,
                     @RequestParam(defaultValue = "20") int pageSize,
                     @RequestParam(name = "requestId", required = false) String requestId,
                     @RequestParam(name = "request_id", required = false) String requestIdSnake,
                     @RequestParam(name = "aggregateType", required = false) String aggregateType,
                     @RequestParam(name = "aggregate_type", required = false) String aggregateTypeSnake,
                     @RequestParam(name = "aggregateId", required = false) String aggregateId,
                     @RequestParam(name = "aggregate_id", required = false) String aggregateIdSnake) {
        if (persistentService == null) {
            return R.fail(409, "Persistent Paper audit path is required");
        }
        return R.ok(persistentService.page(pageNum, pageSize,
            firstNonBlank(requestId, requestIdSnake),
            firstNonBlank(aggregateType, aggregateTypeSnake),
            firstNonBlank(aggregateId, aggregateIdSnake)));
    }

    private static String firstNonBlank(String primary, String alias) {
        return primary != null && !primary.isBlank() ? primary : alias;
    }
}