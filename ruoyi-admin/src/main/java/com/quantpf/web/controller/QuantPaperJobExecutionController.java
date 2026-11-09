package com.quantpf.web.controller;

import com.quantpf.finance.PersistentJobExecutionApplicationService;
import org.dromara.common.core.domain.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quant/job/execution")
public class QuantPaperJobExecutionController {
    private final PersistentJobExecutionApplicationService persistentService;

    @Autowired
    public QuantPaperJobExecutionController(PersistentJobExecutionApplicationService persistentService) {
        this.persistentService = persistentService;
    }

    /** Compatibility constructor used only by the existing standalone MVC test. */
    QuantPaperJobExecutionController() {
        this.persistentService = null;
    }

    @GetMapping("/page")
    public R<?> page(@RequestParam(defaultValue = "1") int pageNum,
                     @RequestParam(defaultValue = "20") int pageSize,
                     @RequestParam(name = "jobName", required = false) String jobName,
                     @RequestParam(name = "job_name", required = false) String jobNameSnake,
                     @RequestParam(name = "executionId", required = false) String executionId,
                     @RequestParam(name = "execution_id", required = false) String executionIdSnake,
                     @RequestParam(name = "status", required = false) String status) {
        if (persistentService == null) {
            return R.fail(409, "Persistent Paper job execution path is required");
        }
        return R.ok(persistentService.page(pageNum, pageSize,
            firstNonBlank(jobName, jobNameSnake),
            firstNonBlank(executionId, executionIdSnake),
            status));
    }

    private static String firstNonBlank(String primary, String alias) {
        return primary != null && !primary.isBlank() ? primary : alias;
    }
}