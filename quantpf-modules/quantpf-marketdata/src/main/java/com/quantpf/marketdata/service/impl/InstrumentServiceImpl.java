package com.quantpf.marketdata.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quantpf.domain.entity.CapacityAssessment;
import com.quantpf.domain.entity.DataQualityIssue;
import com.quantpf.domain.entity.InstrumentCalendarBinding;
import com.quantpf.domain.entity.InstrumentEntity;
import com.quantpf.domain.entity.InstrumentVersion;
import com.quantpf.domain.entity.MarketDataReadiness;
import com.quantpf.domain.enums.CapacityStatus;
import com.quantpf.domain.enums.DataQualityIssueStatus;
import com.quantpf.domain.enums.DataQualityStatus;
import com.quantpf.domain.enums.DataUsageEligibility;
import com.quantpf.domain.enums.InstrumentIdentityStatus;
import com.quantpf.domain.persistence.mapper.CapacityAssessmentMapper;
import com.quantpf.domain.persistence.mapper.DataQualityIssueMapper;
import com.quantpf.domain.persistence.mapper.InstrumentCalendarBindingMapper;
import com.quantpf.domain.persistence.mapper.InstrumentMapper;
import com.quantpf.domain.persistence.mapper.InstrumentVersionMapper;
import com.quantpf.domain.persistence.mapper.MarketDataReadinessMapper;
import com.quantpf.domain.value.DataUsageEligibilityResult;
import com.quantpf.domain.vo.InstrumentVersionVo;
import com.quantpf.domain.vo.InstrumentVo;
import com.quantpf.marketdata.service.IInstrumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 标的身份服务实现（《详设_1 §11》）。
 * <p>可交易资格按 §16.1 输入组合 + §9.4 用途资格矩阵确定性计算：
 * 过期/冲突/授权不明/容量不可交易的数据一律禁止 Paper 新开仓（§24.4 验收红线）。</p>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InstrumentServiceImpl implements IInstrumentService {

    private final InstrumentMapper instrumentMapper;
    private final InstrumentVersionMapper versionMapper;
    private final InstrumentCalendarBindingMapper bindingMapper;
    private final DataQualityIssueMapper qualityIssueMapper;
    private final CapacityAssessmentMapper capacityAssessmentMapper;
    private final MarketDataReadinessMapper readinessMapper;

    @Override
    public PageResult<InstrumentVo> queryPage(String keyword, String productType, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<InstrumentEntity> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(InstrumentEntity::getInstrumentCode, keyword)
                .or().like(InstrumentEntity::getInstrumentName, keyword));
        }
        wrapper.eq(StrUtil.isNotBlank(productType), InstrumentEntity::getProductType, productType)
               .eq(StrUtil.isNotBlank(status), InstrumentEntity::getStatus, status)
               .orderByAsc(InstrumentEntity::getInstrumentCode);
        Page<InstrumentEntity> page = instrumentMapper.selectPage(pageQuery.build(), wrapper);
        List<InstrumentVo> rows = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.build(rows, page.getTotal());
    }

    @Override
    public InstrumentVo queryById(Long id) {
        InstrumentEntity entity = instrumentMapper.selectById(id);
        return entity == null ? null : toVo(entity);
    }

    @Override
    public List<InstrumentVersionVo> listVersion(Long instrumentId) {
        List<InstrumentVersion> list = versionMapper.selectList(
            new LambdaQueryWrapper<InstrumentVersion>()
                .eq(InstrumentVersion::getInstrumentId, instrumentId)
                .orderByDesc(InstrumentVersion::getValidFrom));
        return list.stream().map(this::toVersionVo).collect(Collectors.toList());
    }

    @Override
    public void syncInstruments() {
        log.info("标的基础信息同步触发（占位）：待 Tushare 授权后接入 stock_basic/fut_basic 适配器（详设_1 §25.2.1）");
    }

    @Override
    public Map<String, Object> tradability(Long instrumentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("instrumentId", instrumentId);
        InstrumentEntity instrument = instrumentMapper.selectById(instrumentId);
        if (instrument == null) {
            result.put("tradable", false);
            result.put("reason", "标的不存在");
            result.put("nextStep", "请先完成标的同步或核对标的ID");
            return result;
        }
        boolean listed = InstrumentIdentityStatus.tradableOf(instrument.getStatus());

        // 1. 日历绑定（§12.3.4：MarketCalendarOpen ∧ InstrumentListed ∧ NOT Suspended/Expired）
        InstrumentCalendarBinding binding = bindingMapper.selectOne(
            new LambdaQueryWrapper<InstrumentCalendarBinding>()
                .eq(InstrumentCalendarBinding::getInstrumentId, instrumentId)
                .eq(InstrumentCalendarBinding::getTradabilityStatus, "ACTIVE")
                .last("LIMIT 1"));
        boolean bindingTradable = binding != null;
        result.put("instrumentStatus", instrument.getStatus());
        result.put("instrumentStatusText", InstrumentIdentityStatus.descOf(instrument.getStatus()));
        result.put("bindingTradable", bindingTradable);

        // 2. 质量状态（§16.1 输入）：取该标的未关闭问题中最严重者；无问题视为 AVAILABLE
        String qualityStatus = resolveQualityStatus(instrumentId);
        result.put("qualityStatus", qualityStatus);
        result.put("qualityStatusText", DataQualityStatus.descOf(qualityStatus));

        // 3. 容量状态（§16.1 输入）：最新评估；无评估视为 UNKNOWN（§15.4：UNKNOWN 不得生成新开仓计划）
        String capacityStatus = resolveCapacityStatus(instrumentId);
        result.put("capacityStatus", capacityStatus);
        result.put("capacityStatusText", CapacityStatus.descOf(capacityStatus));

        // 4. 日线就绪（§12.3.5：Paper 可新增风险门禁）
        Boolean paperReady = null;
        String readinessDate = null;
        if (binding != null) {
            MarketDataReadiness readiness = readinessMapper.selectOne(
                new LambdaQueryWrapper<MarketDataReadiness>()
                    .eq(MarketDataReadiness::getCalendarId, binding.getCalendarId())
                    .eq(MarketDataReadiness::getMarketLocalDate, LocalDate.now())
                    .last("LIMIT 1"));
            if (readiness != null) {
                paperReady = Boolean.TRUE.equals(readiness.getPaperReadyFlag());
                readinessDate = String.valueOf(readiness.getMarketLocalDate());
            }
        }
        result.put("readinessDate", readinessDate);
        result.put("paperReady", paperReady);

        // 5. 用途资格矩阵（§9.4）：质量状态 → 各用途资格；再叠加标的/绑定/容量/就绪输入
        DataUsageEligibilityResult eligibilityResult = DataUsageEligibilityResult.ofQuality(
            qualityStatus, qualityReason(qualityStatus));
        Map<String, Boolean> eligibilityText = new LinkedHashMap<>();
        eligibilityResult.getEligibility().forEach((k, v) -> eligibilityText.put(k.name(), v));
        // Paper 资格叠加：质量矩阵 ∧ 标的上市 ∧ 日历绑定 ∧ 容量非 UNTRADEABLE/UNKNOWN ∧ 就绪 paperReady
        boolean paperEligible = Boolean.TRUE.equals(eligibilityText.get(DataUsageEligibility.PAPER_ALLOWED.name()))
            && listed && bindingTradable
            && !CapacityStatus.UNTRADEABLE.name().equals(capacityStatus)
            && !CapacityStatus.UNKNOWN.name().equals(capacityStatus);
        if (paperEligible && paperReady != null) {
            paperEligible = paperReady;
        }
        eligibilityText.put(DataUsageEligibility.PAPER_ALLOWED.name(), paperEligible);
        result.put("eligibility", eligibilityText);

        // 6. 结论（普通语言 + 下一步，§24.6 用户展示口径）
        boolean tradable = paperEligible;
        result.put("tradable", tradable);
        result.put("reason", tradableReason(listed, bindingTradable, qualityStatus, capacityStatus, paperReady));
        result.put("nextStep", nextStep(listed, bindingTradable, qualityStatus, capacityStatus, paperReady));
        return result;
    }

    /**
     * 质量状态输入（§16.1）：未关闭（非 RESOLVED/VERIFIED/CLOSED）问题中按严重度取最不利状态；
     * 无未关闭问题时视为 AVAILABLE。
     */
    private String resolveQualityStatus(Long instrumentId) {
        List<DataQualityIssue> openIssues = qualityIssueMapper.selectList(
            new LambdaQueryWrapper<DataQualityIssue>()
                .eq(DataQualityIssue::getSubjectType, "INSTRUMENT")
                .eq(DataQualityIssue::getSubjectId, instrumentId)
                .notIn(DataQualityIssue::getIssueStatus,
                    DataQualityIssueStatus.RESOLVED.name(),
                    DataQualityIssueStatus.VERIFIED.name(),
                    DataQualityIssueStatus.CLOSED.name()));
        String worst = null;
        int worstRank = -1;
        for (DataQualityIssue issue : openIssues) {
            int rank = qualityRank(issue.getQualityStatus());
            if (rank > worstRank) {
                worstRank = rank;
                worst = issue.getQualityStatus();
            }
        }
        return worst == null ? DataQualityStatus.AVAILABLE.name() : worst;
    }

    /** 质量状态不利程度排序（越大越不利，用于取最严重者） */
    private int qualityRank(String status) {
        return switch (status == null ? "" : status) {
            case "UNAUTHORIZED" -> 8;
            case "INVALID" -> 7;
            case "CONFLICTED" -> 6;
            case "STALE" -> 5;
            case "DELAYED" -> 4;
            case "PARTIAL" -> 3;
            case "PENDING_REVIEW" -> 2;
            default -> 0;
        };
    }

    /** 容量状态输入（§16.1）：取该标的最新一次容量评估 */
    private String resolveCapacityStatus(Long instrumentId) {
        CapacityAssessment latest = capacityAssessmentMapper.selectOne(
            new LambdaQueryWrapper<CapacityAssessment>()
                .eq(CapacityAssessment::getInstrumentId, instrumentId)
                .orderByDesc(CapacityAssessment::getAssessDate)
                .last("LIMIT 1"));
        return latest == null ? CapacityStatus.UNKNOWN.name() : latest.getCapacityStatus();
    }

    private String qualityReason(String qualityStatus) {
        return switch (qualityStatus == null ? "" : qualityStatus) {
            case "AVAILABLE" -> "数据可用";
            case "DELAYED" -> "数据延迟，暂不生成新判断";
            case "PARTIAL" -> "数据部分可用";
            case "CONFLICTED" -> "来源冲突，待人工复核";
            case "UNAUTHORIZED" -> "数据授权不明确，禁止下游使用";
            case "STALE" -> "数据已过期，仅可历史查看与复盘";
            case "INVALID" -> "数据无效";
            case "PENDING_REVIEW" -> "数据待人工复核";
            default -> "数据状态未知，按最严格口径处理";
        };
    }

    private String tradableReason(boolean listed, boolean bindingTradable,
                                  String qualityStatus, String capacityStatus, Boolean paperReady) {
        if (!listed) {
            return "标的状态非 ACTIVE（停牌/退市/未核验）";
        }
        if (!bindingTradable) {
            return "无有效可交易日历绑定";
        }
        if (!DataQualityStatus.AVAILABLE.name().equals(qualityStatus)) {
            return "数据质量状态为 " + qualityStatus + "，禁止 Paper 新开仓";
        }
        if (CapacityStatus.UNTRADEABLE.name().equals(capacityStatus)) {
            return "容量评估为不可交易（流动性不足）";
        }
        if (CapacityStatus.UNKNOWN.name().equals(capacityStatus)) {
            return "容量未评估（UNKNOWN），禁止生成新开仓计划";
        }
        if (paperReady != null && !paperReady) {
            return "当日日线数据未就绪（Paper 门禁未通过）";
        }
        if (CapacityStatus.LIMITED.name().equals(capacityStatus)) {
            return "可交易（容量受限，需限制风险规模）";
        }
        if (CapacityStatus.HIGH_IMPACT.name().equals(capacityStatus)) {
            return "可交易（高冲击成本，仅建议低规模 Paper）";
        }
        return "可交易";
    }

    private String nextStep(boolean listed, boolean bindingTradable,
                            String qualityStatus, String capacityStatus, Boolean paperReady) {
        if (!listed) {
            return "等待标的状态恢复或完成身份核验";
        }
        if (!bindingTradable) {
            return "配置标的与交易日历绑定";
        }
        if (!DataQualityStatus.AVAILABLE.name().equals(qualityStatus)) {
            return "在「数据质量」中处理未关闭的质量问题后复核";
        }
        if (CapacityStatus.UNTRADEABLE.name().equals(capacityStatus)
            || CapacityStatus.UNKNOWN.name().equals(capacityStatus)) {
            return "等待容量评估更新（容量/冲击成本评估任务盘后计算）";
        }
        if (paperReady != null && !paperReady) {
            return "等待日线数据就绪后复核（readinessCalcJob 盘后计算）";
        }
        return "可进入研究/候选流程";
    }

    private InstrumentVo toVo(InstrumentEntity source) {
        InstrumentVo vo = new InstrumentVo();
        BeanUtils.copyProperties(source, vo);
        vo.setStatusText(InstrumentIdentityStatus.descOf(source.getStatus()));
        return vo;
    }

    private InstrumentVersionVo toVersionVo(InstrumentVersion source) {
        InstrumentVersionVo vo = new InstrumentVersionVo();
        BeanUtils.copyProperties(source, vo);
        return vo;
    }
}