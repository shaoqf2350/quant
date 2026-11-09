package com.quantpf.job;

import com.quantpf.domain.enums.ResearchTaskStatus;
import com.quantpf.domain.vo.ResearchTaskVo;
import com.quantpf.marketdata.service.ISourceGroupService;
import com.quantpf.research.service.IResearchService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.stereotype.Component;

/**
 * QuantPF 模块一 XXL-JOB 调度 Handler（《详设_1 §20.4 调度任务清单》）。
 * <p>Handler 命名与《详设_1 §10.2 / §20.4》一致，在 xxl-job-admin 中按同名 JobHandler 配置 Cron。</p>
 * <p>说明：本期数据源未授权，采集/同步类 Handler 为占位（§25.2.1：施工期 Mock，联调前购买 Tushare 并授权新闻源后接入真实适配器）。
 * 已落地：来源组核验（qualityCheckJob）、研究任务补偿（researchTaskRetryJob）。</p>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class QuantPfJobHandler {

    private final IResearchService researchService;
    private final ISourceGroupService sourceGroupService;

    /**
     * 新闻/公告采集（占位：待财联社/新浪/雪球授权后接入采集适配器）。
     */
    @XxlJob("newsScanJob")
    public void newsScanJob() {
        log.info("[newsScanJob] 触发新闻采集调度（占位：待新闻源授权后接入采集适配器，详设_1 §25.2.1）");
    }

    /**
     * 股票/期货基础信息同步（占位：待 Tushare 授权后接入 stock_basic/fut_basic 适配器）。
     */
    @XxlJob("instrumentSyncJob")
    public void instrumentSyncJob() {
        log.info("[instrumentSyncJob] 触发标的基础信息同步（占位：待 Tushare 授权后接入）");
    }

    /**
     * 交易日历同步（占位：trade_cal / fut_trade_cal / hk_tradecal / us_tradecal）。
     */
    @XxlJob("calendarSyncJob")
    public void calendarSyncJob() {
        log.info("[calendarSyncJob] 触发交易日历同步（占位：待 Tushare 授权后接入各市场日历适配器，§12.2）");
    }

    /**
     * 外汇时段规则同步（占位：fx_obasic / fx_daily）。
     */
    @XxlJob("fxSessionSyncJob")
    public void fxSessionSyncJob() {
        log.info("[fxSessionSyncJob] 触发外汇时段规则同步（占位：待 Tushare 授权后接入 fx_obasic，§12.2.5）");
    }

    /**
     * 交易日历重叠区间比对 / 冲突检测（占位：CalendarConflict 流程）。
     */
    @XxlJob("calendarConflictJob")
    public void calendarConflictJob() {
        log.info("[calendarConflictJob] 触发日历重叠比对与冲突检测（占位：待日历同步接入后启用）");
    }

    /**
     * 日线数据就绪状态计算（占位：MarketDataReadiness）。
     */
    @XxlJob("readinessCalcJob")
    public void readinessCalcJob() {
        log.info("[readinessCalcJob] 触发日线数据就绪状态计算（占位：待日线同步接入后启用）");
    }

    /**
     * K 线增量同步（占位：30M/DAY/WEEK/MONTH）。
     */
    @XxlJob("marketBarSyncJob")
    public void marketBarSyncJob() {
        log.info("[marketBarSyncJob] 触发 K 线增量同步（占位：待 Tushare 授权后接入）");
    }

    /**
     * 数据质量与过期检查：执行来源组健康核验（已落地，§6.3 主备切换/冲突上报/状态流转）；
     * 数据接入后扩展快照过期检查与 DataQualityIssue 生成。
     */
    @XxlJob("qualityCheckJob")
    public void qualityCheckJob() {
        log.info("[qualityCheckJob] 开始来源组健康核验");
        int failed = sourceGroupService.verifyAllGroups().size();
        log.info("[qualityCheckJob] 来源组核验完成，共处理 {} 组（快照过期检查待数据接入后启用）", failed);
    }

    /**
     * 研究任务创建/重试补偿（已落地）：分页遍历重试失败与待人工复核的研究任务。
     * <p>覆盖 FAILED（可自动重试）与 PENDING_REVIEW（AI 草稿校验未过/适配器异常降级态；
     * 重试由幂等保证不产生重复副作用，超上限任务保留待人工处理，不无限刷日志）。</p>
     */
    @XxlJob("researchTaskRetryJob")
    public void researchTaskRetryJob() {
        log.info("[researchTaskRetryJob] 开始补偿失败/待复核研究任务");
        int pageNum = 1;
        int retried = 0;
        int failed = 0;
        boolean hasMore = true;
        while (hasMore) {
            PageQuery pageQuery = new PageQuery();
            pageQuery.setPageNum(pageNum);
            pageQuery.setPageSize(50);
            PageResult<ResearchTaskVo> page = researchService.queryTaskPage(
                ResearchTaskStatus.FAILED.name(), null, pageQuery);
            retryBatch(page, "FAILED");
            PageResult<ResearchTaskVo> pendingPage = researchService.queryTaskPage(
                ResearchTaskStatus.PENDING_REVIEW.name(), null, pageQuery);
            retried += retryBatch(page, "FAILED") + retryBatch(pendingPage, "PENDING_REVIEW");
            failed += page.getRows() != null ? page.getRows().size() : 0;
            failed += pendingPage.getRows() != null ? pendingPage.getRows().size() : 0;
            hasMore = false;
            pageNum++;
        }
        log.info("[researchTaskRetryJob] 完成，重试 {} 个任务，仍有 {} 个任务待下轮补偿（幂等保证不重复副作用）", retried, failed);
    }

    /**
     * 重试一批任务：全部经 submitTask 幂等入口，单条失败不影响其余。
     */
    private int retryBatch(PageResult<ResearchTaskVo> page, String statusLabel) {
        if (page == null || page.getRows() == null || page.getRows().isEmpty()) {
            return 0;
        }
        int retried = 0;
        for (ResearchTaskVo vo : page.getRows()) {
            try {
                researchService.submitTask(vo.getPkId());
                retried++;
            } catch (Exception e) {
                log.warn("[researchTaskRetryJob] 重试研究任务失败（{}），pkId={}", statusLabel, vo.getPkId(), e);
            }
        }
        return retried;
    }

    /**
     * 研究事件发布补偿（占位：发布失败的 ResearchEvent 补偿）。研究链路已保证幂等，此处预留。
     */
    @XxlJob("researchEventPublishJob")
    public void researchEventPublishJob() {
        log.info("[researchEventPublishJob] 触发研究事件发布补偿（占位：发布由 Spring Event 同步完成，补偿逻辑待接入）");
    }

    /**
     * 量价基础特征计算（占位：二期量价时机优化预留，§23）。一期只预留特征/状态/标签/回放接口。
     */
    @XxlJob("featureCalcJob")
    public void featureCalcJob() {
        log.info("[featureCalcJob] 触发量价基础特征计算（占位：一期预留，二期实现，§23）");
    }

    /**
     * 容量与市场状态更新（占位：确定性规则计算 §14.4/§15 后落库量化评估）。
     */
    @XxlJob("capacityStateJob")
    public void capacityStateJob() {
        log.info("[capacityStateJob] 触发容量与市场状态更新（占位：待行情数据接入后实现确定性规则计算）");
    }
}