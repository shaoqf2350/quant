package com.quantpf.research.service;

import com.quantpf.domain.bo.ResearchTaskBo;
import com.quantpf.domain.vo.AiResearchDraftVo;
import com.quantpf.domain.vo.EventEvidenceVo;
import com.quantpf.domain.vo.ResearchEventVo;
import com.quantpf.domain.vo.ResearchMaterialVo;
import com.quantpf.domain.vo.ResearchTaskVo;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;

import java.util.List;

/**
 * 新闻研究链服务（《详设_1 §10》）。
 */
public interface IResearchService {

    /**
     * 分页查询研究任务。
     */
    PageResult<ResearchTaskVo> queryTaskPage(String status, String taskType, PageQuery pageQuery);

    /**
     * 查询研究任务详情。
     *
     * @param pkId 任务主键
     */
    ResearchTaskVo queryTaskById(Long pkId);

    /**
     * 创建研究任务。
     *
     * @param bo 创建表单
     * @return 任务主键
     */
    Long createTask(ResearchTaskBo bo);

    /**
     * 提交任务给 Dify/TradingAgents：保存草稿、校验、生成研究事件并发布（§10.2）。
     *
     * @param taskId 任务主键
     */
    void submitTask(Long taskId);

    /**
     * 分页查询研究事件。
     */
    PageResult<ResearchEventVo> queryEventPage(String status, String subjectType, PageQuery pageQuery);

    /**
     * 查询研究事件详情。
     */
    ResearchEventVo queryEventById(Long pkId);

    /**
     * 查询任务下的 AI 草稿列表。
     */
    List<AiResearchDraftVo> listDraftByTask(Long taskId);

    /**
     * 查询事件证据链。
     */
    List<EventEvidenceVo> listEvidenceByEvent(Long eventId);

    /**
     * 分页查询研究材料清单（原始快照，供创建研究任务时勾选）。
     *
     * @param keyword   关键字（快照ID/外部ID 模糊匹配，可为空）
     * @param pageQuery 分页参数
     */
    PageResult<ResearchMaterialVo> queryMaterialPage(String keyword, PageQuery pageQuery);
}