package com.quantpf.web.controller;

import com.quantpf.domain.enums.OrderSide;
import com.quantpf.domain.enums.OrderType;
import com.quantpf.execution.PaperOrderApplicationService;
import com.quantpf.finance.PersistentPaperOrderApplicationService;
import com.quantpf.web.dto.PaperOrderRequest;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quant/order")
public class QuantPaperOrderController {
    private final PersistentPaperOrderApplicationService persistentService;
    private final PaperOrderApplicationService standaloneService;

    @Autowired
    public QuantPaperOrderController(PersistentPaperOrderApplicationService persistentService) {
        this.persistentService = persistentService;
        this.standaloneService = null;
    }

    /** Compatibility constructor used only by the existing standalone MVC test. */
    QuantPaperOrderController() {
        this.persistentService = null;
        this.standaloneService = PaperApiRuntime.ORDERS;
    }

    @GetMapping("/page")
    public R<?> page(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "20") int pageSize) {
        if (persistentService != null) return R.ok(persistentService.page(pageNum, pageSize));
        return R.ok(standaloneService.page(pageNum, pageSize));
    }

    @PostMapping("/{id}/cancel")
    public R<?> cancel(@org.springframework.web.bind.annotation.PathVariable Long id,
                       @RequestParam(defaultValue = "Canceled by Paper API") String reason) {
        if (persistentService == null) return R.fail(409, "Persistent Paper order path is required");
        try {
            return R.ok(persistentService.cancel(id, reason));
        } catch (IllegalStateException e) {
            return R.fail(409, e.getMessage());
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        }
    }
    @PostMapping("/paper")
    public R<?> submit(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                       @RequestBody PaperOrderRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return R.fail(400, "Idempotency-Key is required");
        try {
            if (persistentService != null) {
                var command = new PersistentPaperOrderApplicationService.SubmitOrderCommand(
                    request.accountId(), request.instrumentId(), request.clientOrderId(), request.requestId(), idempotencyKey,
                    request.side(), request.orderType(), request.quantity(), request.limitPrice(), request.estimatedNotional(), request.fillPrice());
                return R.ok(persistentService.submit(command));
            }
            var command = new PaperOrderApplicationService.SubmitOrderCommand(
                System.nanoTime(), request.accountId(), request.instrumentId(), request.clientOrderId(), request.requestId(), idempotencyKey,
                OrderSide.valueOf(request.side()), OrderType.valueOf(request.orderType()), request.quantity(), request.limitPrice(),
                new com.quantpf.domain.value.Money(request.estimatedNotional()), request.fillPrice());
            return R.ok(standaloneService.submit(command));
        } catch (IllegalStateException e) {
            return R.fail(409, e.getMessage());
        } catch (IllegalArgumentException e) {
            return R.fail(400, e.getMessage());
        } catch (com.quantpf.domain.exception.QuantPfDomainException e) {
            return R.fail(409, e.code() + ":" + e.getMessage());
        }
    }
}

