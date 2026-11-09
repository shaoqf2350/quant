package com.quantpf.marketdata.service.impl;

import com.quantpf.domain.enums.AiDraftValidationStatus;
import com.quantpf.domain.enums.BarTimeframe;
import com.quantpf.domain.enums.CalendarCodeCatalog;
import com.quantpf.domain.enums.CalendarConflictStatus;
import com.quantpf.domain.enums.CalendarDayStatus;
import com.quantpf.domain.enums.CalendarEndpoint;
import com.quantpf.domain.enums.CalendarLevel;
import com.quantpf.domain.enums.CapacityStatus;
import com.quantpf.domain.enums.CollectionTaskStatus;
import com.quantpf.domain.enums.CollectionTaskType;
import com.quantpf.domain.enums.ContractEventType;
import com.quantpf.domain.enums.CorporateActionType;
import com.quantpf.domain.enums.CurrencyCode;
import com.quantpf.domain.enums.DataQualityIssueStatus;
import com.quantpf.domain.enums.DataQualityStatus;
import com.quantpf.domain.enums.DataSourceCatalog;
import com.quantpf.domain.enums.DataSourceStatus;
import com.quantpf.domain.enums.EvidenceType;
import com.quantpf.domain.enums.InstrumentIdentityStatus;
import com.quantpf.domain.enums.LifecycleEventStatus;
import com.quantpf.domain.enums.MarketStateCode;
import com.quantpf.domain.enums.MarketTimezone;
import com.quantpf.domain.enums.MarketType;
import com.quantpf.domain.enums.ProductType;
import com.quantpf.domain.enums.QuantDictEnum;
import com.quantpf.domain.enums.RegionCode;
import com.quantpf.domain.enums.ResearchDirection;
import com.quantpf.domain.enums.ResearchEventStatus;
import com.quantpf.domain.enums.ResearchTaskStatus;
import com.quantpf.domain.enums.ResearchTaskType;
import com.quantpf.domain.enums.SessionType;
import com.quantpf.domain.enums.SourceGroupCatalog;
import com.quantpf.domain.enums.SourceGroupStatus;
import com.quantpf.domain.enums.SubjectType;
import com.quantpf.domain.enums.VenueCode;
import com.quantpf.domain.vo.DictOptionVo;
import com.quantpf.marketdata.service.IQuantDictService;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一枚举字典服务实现（《详设_1_补充规范_前端输入最小化与枚举字典》§三）。
 * <p>以「字典名 → 选项列表」注册表维护全量字典；枚举走 {@link #registerEnum}（要求实现 {@link QuantDictEnum}），
 * 目录走 {@link #registerCatalog}（{@code getCode/getDefaultName/getDescription} 契约）。</p>
 * <p>无状态、只读，纯内存读取，不涉及数据访问；选项顺序稳定（按枚举声明顺序，并附 {@code order}）。</p>
 */
@Service
public class QuantDictServiceImpl implements IQuantDictService {

    /** 字典名 → 选项列表（有序，保证选项顺序稳定） */
    private final Map<String, List<DictOptionVo>> registry = new LinkedHashMap<>();

    public QuantDictServiceImpl() {
        // 既有状态/类型枚举（§3.2）
        registerEnum("DataSourceStatus", DataSourceStatus.class);
        registerEnum("SourceGroupStatus", SourceGroupStatus.class);
        registerEnum("CollectionTaskStatus", CollectionTaskStatus.class);
        registerEnum("DataQualityStatus", DataQualityStatus.class);
        registerEnum("DataQualityIssueStatus", DataQualityIssueStatus.class);
        registerEnum("MarketStateCode", MarketStateCode.class);
        registerEnum("CapacityStatus", CapacityStatus.class);
        registerEnum("CorporateActionType", CorporateActionType.class);
        registerEnum("ContractEventType", ContractEventType.class);
        registerEnum("LifecycleEventStatus", LifecycleEventStatus.class);
        registerEnum("ResearchTaskStatus", ResearchTaskStatus.class);
        registerEnum("ResearchEventStatus", ResearchEventStatus.class);
        registerEnum("ResearchDirection", ResearchDirection.class);
        registerEnum("AiDraftValidationStatus", AiDraftValidationStatus.class);
        registerEnum("EvidenceType", EvidenceType.class);
        registerEnum("ProductType", ProductType.class);
        // 标的状态：既有 DDD 枚举 InstrumentStatus 已废弃，服务层实际采用 InstrumentIdentityStatus（§11.6）
        registerEnum("InstrumentIdentityStatus", InstrumentIdentityStatus.class);
        registerEnum("CalendarConflictStatus", CalendarConflictStatus.class);
        registerEnum("CalendarDayStatus", CalendarDayStatus.class);
        registerEnum("SessionType", SessionType.class);

        // 新增枚举（§3.3）
        registerEnum("MarketType", MarketType.class);
        registerEnum("CalendarLevel", CalendarLevel.class);
        registerEnum("RegionCode", RegionCode.class);
        registerEnum("VenueCode", VenueCode.class);
        registerEnum("CurrencyCode", CurrencyCode.class);
        registerEnum("MarketTimezone", MarketTimezone.class);
        registerEnum("BarTimeframe", BarTimeframe.class);
        registerEnum("CalendarEndpoint", CalendarEndpoint.class); // 日历导入来源接口（施工期 Mock，补充规范 §4.2）
        registerEnum("CollectionTaskType", CollectionTaskType.class);
        registerEnum("ResearchTaskType", ResearchTaskType.class);
        registerEnum("SubjectType", SubjectType.class);

        // 目录（§3.3，与 DataSourceCatalog 同构）
        registerCatalog("DataSourceCatalog", DataSourceCatalog.list().stream()
            .map(c -> DictOptionVo.of(c.getCode(), c.getDefaultName(), c.getDescription()))
            .collect(Collectors.toList()));
        registerCatalog("SourceGroupCatalog", SourceGroupCatalog.list().stream()
            .map(c -> DictOptionVo.of(c.getCode(), c.getDefaultName(), c.getDescription()))
            .collect(Collectors.toList()));
        registerCatalog("CalendarCodeCatalog", CalendarCodeCatalog.list().stream()
            .map(c -> DictOptionVo.of(c.getCode(), c.getDefaultName(), c.getDescription()))
            .collect(Collectors.toList()));
    }

    @Override
    public List<DictOptionVo> listDict(String enumName) {
        List<DictOptionVo> options = registry.get(enumName);
        if (options == null) {
            throw new ServiceException("未知枚举字典（QPF-DICT-001）：" + enumName);
        }
        return options;
    }

    /**
     * 注册实现 {@link QuantDictEnum} 的枚举：按 {@code value/getDesc/description} 生成选项，附声明序 {@code order}。
     */
    private <E extends Enum<E> & QuantDictEnum> void registerEnum(String name, Class<E> type) {
        List<DictOptionVo> options = new ArrayList<>();
        int order = 1;
        for (E e : type.getEnumConstants()) {
            options.add(DictOptionVo.of(e.value(), e.getDesc(), e.description(), order++));
        }
        registry.put(name, options);
    }

    /**
     * 注册目录枚举：登记预生成的选项列表（value=编码、label=默认名、description=说明），补声明序 {@code order}。
     */
    private void registerCatalog(String name, List<DictOptionVo> options) {
        for (int i = 0; i < options.size(); i++) {
            options.get(i).setOrder(i + 1);
        }
        registry.put(name, options);
    }
}
