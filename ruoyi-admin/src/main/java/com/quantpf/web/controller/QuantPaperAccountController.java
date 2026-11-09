package com.quantpf.web.controller;

import com.quantpf.finance.PaperAccountApplicationService;
import com.quantpf.finance.PersistentPaperAccountApplicationService;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quant/account")
public class QuantPaperAccountController {
    private final PersistentPaperAccountApplicationService persistentService;
    private final PaperAccountApplicationService standaloneService;

    @Autowired
    public QuantPaperAccountController(PersistentPaperAccountApplicationService persistentService) {
        this.persistentService = persistentService;
        this.standaloneService = null;
    }

    /** Compatibility constructor used only by the existing standalone MVC test. */
    QuantPaperAccountController() {
        this.persistentService = null;
        this.standaloneService = PaperApiRuntime.ACCOUNTS;
    }

    @GetMapping("/page")
    public R<?> page(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        if (persistentService != null) return R.ok(persistentService.page(pageNum, pageSize));
        return R.ok(standaloneService.page(pageNum, pageSize));
    }

    @GetMapping("/{id}/ledger")
    public R<?> ledger(@PathVariable Long id) {
        try {
            if (persistentService != null) return R.ok(persistentService.ledger(id));
            return R.ok(standaloneService.ledger(id));
        } catch (IllegalArgumentException e) {
            return R.fail(404, e.getMessage());
        }
    }
}
