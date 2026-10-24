# 基于 Claude Agent 的多 Agent 分层量化交易架构方案

## 方案概述

本方案采用 **多 Agent 分层架构**，参考 Claude Portfolio 的多智能体架构思想，将量化交易系统拆分为多个独立的子 Agent，通过上下文隔离和 Maker-Checker 模式保证决策质量。该架构适合生产级系统，具备高可靠性、可扩展性和可维护性。

> **风险提示**：量化交易存在本金亏损风险，AI 模型输出不构成投资建议，任何策略上线前必须经过充分回测和模拟盘验证。

---

## 一、整体架构设计

### 1.1 架构总览

系统采用分层调度架构，由主控调度器统一分发任务、汇总结果、管理上下文隔离，各子 Agent 职责明确、独立运行：

```
+----------------------------------------------------------------------+
|                    主控调度器 (Orchestrator)                            |
|         负责分发任务、汇总结果、管理上下文隔离                           |
+----------+--------------+--------------+--------------+---------------+
           |              |              |              |
    +------+------+  +----+----+  +------+------+
    | 新闻风控    |  | 策略研究  |  | 风险监控    |
    | Agent       |  | Agent     |  | Agent       |
    | (Sonnet)    |  | (Opus)    |  | (Sonnet)    |
    +------+------+  +----+----+  +------+------+
           |              |              |
           +--------------+--------------+
                          |
                   +------+------+
                   | 验证 Agent   |  <- 独立裁判，Maker-Checker
                   | (Opus)      |     不同模型，不同指令
                   +------+------+
                          |
                   +------+------+
                   | 执行 Agent   |  <- 通过 MCP 连接券商
                   | (Haiku)     |
                   +-------------+
```

### 1.2 架构分层说明

| 层级 | Agent 名称 | 推荐模型 | 核心职责 |
|:---|:---|:---|:---|
| L1 感知层 | 新闻风控 Agent | Claude Sonnet | 实时抓取并分析财经新闻，输出市场风险等级 |
| L2 决策层 | 策略研究 Agent | Claude Opus | 运行量化策略（爆点形态、缠论形态等），输出候选股票池 |
| L3 验证层 | 验证 Agent | Claude Opus | 独立审核交易信号，采用 Maker-Checker 模式防止误判 |
| L4 执行层 | 执行 Agent | Claude Haiku | 通过 MCP 连接器对接券商 API，执行实际交易订单 |
| 调度层 | 主控调度器 | — | 编排各 Agent 执行顺序，管理上下文隔离与状态持久化 |

### 1.3 设计原则

1. **上下文隔离**：每个子 Agent 只接收与其任务相关的上下文切片，防止幻觉扩散
2. **Maker-Checker 模式**：交易信号必须经过独立验证 Agent 审核后方可执行
3. **模型分层选型**：根据任务复杂度选择不同能力的模型（Opus > Sonnet > Haiku），平衡效果与成本
4. **结构化输出**：所有 Agent 强制输出 JSON 格式，便于程序化处理
5. **可插拔策略**：策略引擎支持动态注册新策略，无需修改核心代码

---

## 二、核心模块设计

### 2.1 上下文隔离机制

每个子 Agent 运行在独立的上下文环境中，仅接收与自身角色相关的任务切片：

```python
class SubAgent:
    """子Agent基础类，实现上下文隔离"""
    
    def __init__(self, role: str, model: str, skill_file: str):
        """
        初始化子Agent
        
        Args:
            role: Agent角色标识（如 news_risk, strategy, verifier, executor）
            model: 使用的Claude模型（claude-sonnet / claude-opus / claude-haiku）
            skill_file: 该Agent的SKILL.md配置文件路径
        """
        self.role = role
        self.model = model
        self.skill = load_skill(skill_file)
        self.state = {}
    
    def run(self, context_slice: dict) -> dict:
        """
        独立运行Agent，返回结构化JSON
        
        Args:
            context_slice: 与该Agent任务相关的上下文切片
            
        Returns:
            dict: 结构化JSON响应
        """
        response = claude_api(
            model=self.model,
            system=self.skill,
            messages=[{"role": "user", "content": json.dumps(context_slice)}]
        )
        return parse_json(response)  # 强制结构化输出
```

**设计要点：**

- 每个 Agent 拥有独立的 skill 配置文件，定义其角色、任务和输出格式
- 通过 context_slice 精确控制每个 Agent 可见的数据范围
- 使用 parse_json 强制解析为结构化数据，避免自由文本带来的解析不确定性

---

### 2.2 新闻风控 Agent

**职责**：实时分析财经新闻，判断市场整体风险等级，识别受影响行业，在重大黑天鹅事件时发出 HALT 信号。

**SKILL 配置文件（news_risk.md）：**

```
你是风控分析师。你的任务是：
1. 分析过去24小时的重要财经新闻
2. 判断市场整体风险等级（LOW/MEDIUM/HIGH/CRITICAL）
3. 识别受影响行业，输出行业风险评分
4. 如果存在重大黑天鹅事件，直接输出 HALT 信号

输出格式（严格JSON）：
{
  "risk_level": "LOW|MEDIUM|HIGH|CRITICAL",
  "affected_sectors": [
    {"sector": "银行", "score": 85},
    {"sector": "房地产", "score": 92}
  ],
  "halt_signal": false,
  "reasoning": "美联储超预期加息50bp，市场对经济衰退担忧加剧",
  "timestamp": "2026-09-28T09:00:00Z"
}
```

**风控等级说明：**

| 风险等级 | 含义 | 系统行为 |
|:---|:---|:---|
| LOW | 市场正常 | 允许全仓交易 |
| MEDIUM | 轻度风险 | 限制最大仓位至 30% |
| HIGH | 高风险 | 限制最大仓位至 10%，暂停新开仓 |
| CRITICAL | 极高风险 | 触发 HALT，暂停所有交易，通知管理员 |

---

### 2.3 策略选股 Agent

**职责**：运行多种量化选股策略（爆点形态、缠论形态等），输出候选股票列表及交易信号。

**SKILL 配置文件（strategy.md）：**

```
你是量化策略研究员。根据以下规则选股：

## 策略一：爆点形态策略
- 条件：放量突破20日高点，成交量 > 5日均量2倍
- 止损：突破K线最低点
- 仓位：置信度 > 0.7 时建仓

## 策略二：缠论形态策略
- 一买：下跌趋势中，最后一个中枢的第三类卖点后出现底分型
- 二买：一买后第一次回踩不破前低
- 三买：离开中枢后第一次回踩不进入中枢

## 策略扩展机制
- 支持通过注册方式动态添加新策略
- 新策略只需实现标准接口即可被策略引擎调用
- 策略权重可配置，支持加权投票机制

输出格式（严格JSON）：
{
  "candidates": [
    {
      "stock": "600519.SH",
      "stock_name": "贵州茅台",
      "strategy": "breakout_v1",
      "entry_price": 1850.00,
      "stop_loss": 1780.00,
      "target_price": 1950.00,
      "confidence": 0.85,
      "risk_reward_ratio": 2.1,
      "reasoning": "放量突破20日高点，成交量为5日均量2.3倍"
    }
  ],
  "summary": "共筛选出3只候选股票，策略覆盖率68%"
}
```

**策略引擎扩展设计：**

```python
class StrategyEngine:
    """可扩展的策略引擎"""
    
    def __init__(self):
        self.strategies = {}  # 策略注册表
    
    def register(self, name: str, config: dict, func):
        """
        注册新策略
        
        Args:
            name: 策略名称（唯一标识）
            config: 策略配置参数
            func: 策略执行函数
        """
        self.strategies[name] = {
            'config': config,
            'func': func
        }
    
    def run_all(self, stock_data: dict) -> list:
        """运行所有已注册策略，返回候选股票列表"""
        candidates = []
        for name, strategy in self.strategies.items():
            result = strategy['func'](stock_data, strategy['config'])
            if result.get('signal') == 'BUY':
                candidates.append({
                    'stock': result['stock'],
                    'strategy': name,
                    'confidence': result['confidence'],
                    'entry_price': result['entry_price'],
                    'stop_loss': result['stop_loss'],
                    'target_price': result['target_price'],
                    'reasoning': result['reasoning']
                })
        return sorted(candidates, key=lambda x: x['confidence'], reverse=True)
```

---

### 2.4 验证 Agent（Maker-Checker 模式）

**职责**：作为独立裁判，对交易信号进行二次审核，确保每笔交易满足风控要求后方可执行。

**SKILL 配置文件（verifier.md）：**

```
你是独立的风控审核员。你的任务是审核交易信号，你与信号生成者完全独立。

审核清单：
1. 止损点是否明确且合理？（必须存在）
2. 期望收益/风险比是否 > 1.5？
3. 仓位是否符合凯利公式约束？
4. 是否与当前风控信号冲突？
5. 是否存在过度集中持仓（单只股票超过总仓位20%）？

输出格式（严格JSON）：
{
  "decision": "APPROVE|REJECT",
  "signal_id": "sig_20260928_001",
  "stock": "600519.SH",
  "checks": {
    "stop_loss_defined": true,
    "risk_reward_ratio": 2.1,
    "position_within_limit": true,
    "no_risk_conflict": true,
    "no_over_concentration": true
  },
  "reasoning": "止损明确，风险收益比2.1满足要求，仓位合理",
  "rejected_reason": null
}
```

**验证流程：**

```
策略Agent输出信号 -> 验证Agent独立审核 -> 通过则进入执行队列 / 不通过则记录原因并丢弃
```

---

### 2.5 执行 Agent

**职责**：接收通过验证的交易信号，通过 MCP 连接器对接券商 API 执行实际交易订单。

**SKILL 配置文件（executor.md）：**

```
你是交易执行专员。你的任务是准确执行通过验证的交易信号。

执行规范：
1. 严格执行限价单（LIMIT ORDER），禁止市价单
2. 下单前二次确认止损和止盈价格
3. 记录每笔交易的完整日志
4. 执行失败时立即重试（最多3次），仍失败则告警

输出格式（严格JSON）：
{
  "order_id": "ord_20260928_001",
  "status": "SUBMITTED|FILLED|FAILED",
  "symbol": "600519.SH",
  "side": "BUY",
  "quantity": 100,
  "order_type": "LIMIT",
  "price": 1850.00,
  "stop_loss": 1780.00,
  "take_profit": 1950.00,
  "timestamp": "2026-09-28T09:05:00Z",
  "message": "订单已提交"
}
```

---

### 2.6 主控调度器（Orchestrator）

**职责**：编排各 Agent 的执行顺序，管理上下文隔离，处理异常和状态持久化。

```python
import json
import logging
from datetime import datetime

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class TradingOrchestrator:
    """量化交易主控调度器"""
    
    def __init__(self, config: dict):
        """
        初始化调度器
        
        Args:
            config: 系统配置字典，包含各Agent的模型配置、策略配置等
        """
        # 初始化各子Agent
        self.news_agent = SubAgent(
            role="news_risk",
            model="claude-sonnet",
            skill_file="configs/news_risk.md"
        )
        self.strategy_agent = SubAgent(
            role="strategy",
            model="claude-opus",
            skill_file="configs/strategy.md"
        )
        self.verifier = SubAgent(
            role="verifier",
            model="claude-opus",
            skill_file="configs/verifier.md"
        )
        self.executor = SubAgent(
            role="executor",
            model="claude-haiku",
            skill_file="configs/executor.md"
        )
        
        # 状态管理
        self.state_file = config.get('state_file', 'trading_state.json')
        self.log_file = config.get('log_file', 'trading_log.jsonl')
        self.state = self._load_state()
    
    def run_cycle(self):
        """
        执行一个完整的交易周期
        
        执行流程：
        1. 数据采集 -> 2. 风控判断 -> 3. 策略选股 -> 4. 独立验证 -> 5. 执行交易 -> 6. 记录日志
        """
        cycle_start = datetime.utcnow()
        logger.info("=" * 60)
        logger.info(f"开始新交易周期: {cycle_start.isoformat()}")
        
        try:
            # Stage 1: 数据采集
            logger.info("[Stage 1] 采集市场数据...")
            market_data = self._fetch_all_data()
            
            # Stage 2: 风控判断
            logger.info("[Stage 2] 风控判断...")
            risk = self.news_agent.run({
                "market_data": market_data,
                "current_state": self.state.get("last_risk_level")
            })
            
            # 检查是否触发紧急停机
            if risk.get("halt_signal"):
                logger.critical(f"触发紧急停机! 原因: {risk.get('reasoning')}")
                self._emergency_halt(risk)
                return
            
            # 更新风控状态
            self.state["last_risk_level"] = risk.get("risk_level")
            
            # Stage 3: 策略选股
            logger.info("[Stage 3] 策略选股...")
            candidates = self.strategy_agent.run({
                "market_data": market_data,
                "risk_constraints": risk
            })
            
            if not candidates.get("candidates"):
                logger.info("本轮无候选股票，跳过交易")
                return
            
            logger.info(f"策略筛选出 {len(candidates['candidates'])} 只候选股票")
            
            # Stage 4: 独立验证（Maker-Checker）
            logger.info("[Stage 4] 独立验证交易信号...")
            approved_signals = []
            rejected_signals = []
            
            for signal in candidates["candidates"]:
                verdict = self.verifier.run({
                    "signal": signal,
                    "risk_context": risk,
                    "portfolio_state": self.state.get("portfolio", {})
                })
                
                if verdict.get("decision") == "APPROVE":
                    approved_signals.append(signal)
                    logger.info(f"  通过验证: {signal.get('stock')} - {signal.get('strategy')}")
                else:
                    rejected_signals.append({
                        "signal": signal,
                        "reason": verdict.get("rejected_reason")
                    })
                    logger.warning(f"  验证拒绝: {signal.get('stock')} - {verdict.get('rejected_reason')}")
            
            # Stage 5: 执行交易
            logger.info("[Stage 5] 执行交易...")
            execution_results = []
            for signal in approved_signals:
                result = self.executor.run({
                    "action": "EXECUTE",
                    "signal": signal,
                    "risk_context": risk
                })
                execution_results.append(result)
                logger.info(f"  执行结果: {signal.get('stock')} - {result.get('status')}")
            
            # Stage 6: 记录日志与更新状态
            logger.info("[Stage 6] 记录日志...")
            self._log_cycle({
                "cycle_start": cycle_start.isoformat(),
                "cycle_end": datetime.utcnow().isoformat(),
                "risk": risk,
                "candidates": candidates,
                "approved": approved_signals,
                "rejected": rejected_signals,
                "executions": execution_results
            })
            
            # 更新持仓状态
            self._update_portfolio_state(execution_results)
            
            logger.info(f"交易周期完成: {len(approved_signals)}笔成交, {len(rejected_signals)}笔拒绝")
            
        except Exception as e:
            logger.error(f"交易周期异常: {str(e)}", exc_info=True)
            self._handle_exception(e)
    
    def _fetch_all_data(self) -> dict:
        """采集所有市场数据"""
        return {
            "market_index": {},
            "sector_data": {},
            "news": [],
            "stock_data": {}
        }
    
    def _emergency_halt(self, risk: dict):
        """紧急停机处理"""
        self.state["halted"] = True
        self.state["halt_reason"] = risk.get("reasoning")
        self.state["halt_time"] = datetime.utcnow().isoformat()
        self._save_state()
        self._send_alert(f"系统已紧急停机: {risk.get('reasoning')}")
    
    def _log_cycle(self, log_entry: dict):
        """记录交易周期日志"""
        with open(self.log_file, 'a', encoding='utf-8') as f:
            f.write(json.dumps(log_entry, ensure_ascii=False) + '\n')
    
    def _update_portfolio_state(self, executions: list):
        """更新持仓状态"""
        pass
    
    def _load_state(self) -> dict:
        """加载持久化状态"""
        try:
            with open(self.state_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except FileNotFoundError:
            return {"initialized_at": datetime.utcnow().isoformat()}
    
    def _save_state(self):
        """保存持久化状态"""
        with open(self.state_file, 'w', encoding='utf-8') as f:
            json.dump(self.state, f, ensure_ascii=False, indent=2)
    
    def _handle_exception(self, exception: Exception):
        """异常处理"""
        self._send_alert(f"系统异常: {str(exception)}")
    
    def _send_alert(self, message: str):
        """发送告警通知"""
        logger.critical(f"【告警】{message}")
```

---

## 三、定时调度与运行方式

### 3.1 调度方式

系统支持多种触发方式：

| 触发方式 | 适用场景 | 实现方式 |
|:---|:---|:---|
| 定时轮询 | 常规交易日定时扫描（如每5分钟） | schedule 库 / Quartz 定时任务 |
| 事件驱动 | 重大新闻发布时即时触发 | 消息队列（RabbitMQ / Kafka） |
| 手动触发 | 管理员手动执行单次扫描 | REST API 接口 |
| cron 表达式 | 复杂调度需求 | cron 表达式配置 |

### 3.2 示例：定时轮询配置

```python
import schedule
import time
import threading

orchestrator = TradingOrchestrator(config={
    'state_file': 'data/trading_state.json',
    'log_file': 'data/trading_log.jsonl'
})

# 交易日每分钟执行一次（可根据需要调整频率）
schedule.every().minute.do(orchestrator.run_cycle)

# 每日收盘后执行日结报告
schedule.every().day.at("15:10").do(orchestrator.generate_daily_report)

# 启动调度器
logger.info("调度器已启动，开始监听任务...")
while True:
    schedule.run_pending()
    time.sleep(1)
```

---

## 四、系统监控与运维

### 4.1 监控指标

| 监控维度 | 指标项 | 告警阈值 |
|:---|:---|:---|
| 性能 | Agent 响应延迟 | > 30s 告警 |
| 性能 | 交易周期耗时 | > 5min 告警 |
| 可靠性 | API 调用成功率 | < 99% 告警 |
| 可靠性 | 连续失败次数 | >= 3 次触发降级 |
| 业务 | 日交易笔数 | 异常波动告警 |
| 业务 | 持仓集中度 | 单票 > 20% 告警 |
| 资源 | CPU/内存使用率 | > 80% 告警 |
| 资源 | 磁盘空间 | < 10% 剩余告警 |

### 4.2 日志规范

```
# 日志级别说明
INFO   - 正常业务流程日志
WARN   - 可恢复的异常情况（如某次API超时后重试成功）
ERROR  - 需要关注的错误（如某笔交易执行失败）
CRITICAL - 严重错误（如系统紧急停机、数据异常）

# 日志格式
[时间戳] [级别] [模块] [上下文] 消息内容
例: [2026-09-28T09:05:23Z] [INFO] [executor] [stock=600519.SH] 订单已提交，订单号: ord_20260928_001
```

### 4.3 异常恢复策略

| 异常类型 | 处理策略 |
|:---|:---|
| API 临时不可用 | 指数退避重试（1s -> 2s -> 4s -> 8s），最多3次 |
| 数据解析失败 | 丢弃该次数据，记录错误日志，继续下一轮 |
| Agent 调用超时 | 超时后标记该次结果为 SKIP，不阻塞后续流程 |
| 系统OOM | 自动重启容器，保留现场日志供排查 |
| 紧急停机后恢复 | 管理员手动确认后方可解除停机状态 |

---

## 五、方案优缺点分析

### 5.1 优点

| 优势 | 说明 |
|:---|:---|
| 上下文隔离 | 每个子 Agent 运行在独立上下文中，防止幻觉扩散到整个系统 |
| Maker-Checker 双重校验 | 交易信号必须经过独立验证 Agent 审核，显著降低误判率 |
| 模块独立迭代 | 各 Agent 可独立开发、测试、部署和升级，互不影响 |
| 模型成本优化 | 简单任务用 Haiku（低成本），复杂推理用 Opus（高质量），按需分配 |
| 可观测性强 | 每笔交易有完整的决策链路日志，便于审计和回溯 |
| 策略可扩展 | 新策略通过注册方式接入，无需修改核心调度逻辑 |

### 5.2 缺点

| 劣势 | 说明 |
|:---|:---|
| 架构复杂度高 | 多 Agent 协调、上下文管理、异常处理等复杂度显著增加 |
| 调试难度大 | 分布式调用链长，定位问题需要追踪跨 Agent 的完整上下文 |
| API 调用成本高 | 每个交易周期需调用多次 Claude API，费用相对较高 |
| 延迟较高 | 串行执行多个 Agent 导致单周期延迟较大，不适合高频交易 |
| 运维复杂度 | 需要监控多个 Agent 的健康状态、依赖服务和资源占用 |

---

## 六、部署架构建议

### 6.1 基础设施

| 组件 | 推荐方案 | 说明 |
|:---|:---|:---|
| 运行时 | Docker + Kubernetes | 容器化部署，支持自动扩缩容 |
| 消息队列 | RabbitMQ / Redis Streams | Agent 间异步通信，解耦各模块 |
| 数据库 | PostgreSQL + Redis | 持久化存储 + 缓存热点数据 |
| 日志收集 | ELK (Elasticsearch + Logstash + Kibana) | 集中日志管理和分析 |
| 监控 | Prometheus + Grafana | 指标采集和可视化监控 |
| CI/CD | GitLab CI / GitHub Actions | 自动化构建、测试和部署 |

### 6.2 安全加固

1. **API Key 管理**：Claude API Key 通过环境变量或密钥管理服务（如 Vault）注入，禁止硬编码
2. **网络隔离**：交易执行 Agent 部署在内网，仅通过白名单访问券商 API
3. **审计日志**：所有交易操作记录不可篡改的审计日志
4. **权限控制**：紧急停机按钮仅限授权管理员操作
5. **数据脱敏**：日志中敏感信息（如账户ID、API Key）自动脱敏

---

## 七、实施路线图

### 第一阶段：原型验证（1-2周）

- [ ] 搭建基础框架：SubAgent 基类、调度器骨架
- [ ] 实现新闻风控 Agent（Sonnet 模型）
- [ ] 实现单一策略（爆点形态）Agent
- [ ] 模拟数据源，完成端到端流程验证

### 第二阶段：功能完善（2-4周）

- [ ] 接入真实市场数据源
- [ ] 实现缠论形态策略
- [ ] 实现验证 Agent（Maker-Checker）
- [ ] 对接券商模拟盘 API
- [ ] 完善日志和监控体系

### 第三阶段：生产就绪（2-3周）

- [ ] 容器化部署 + 自动化测试
- [ ] 压力测试和异常注入测试
- [ ] 告警通知通道对接
- [ ] 运维文档和应急预案编写
- [ ] 小资金实盘验证

### 第四阶段：优化迭代（持续）

- [ ] 策略效果评估和参数调优
- [ ] 新增策略模块
- [ ] 模型选型优化（效果 vs 成本）
- [ ] 性能调优和延迟降低

---

## 八、附录

### 8.1 目录结构参考

```
quant-trading-agent/
├── configs/                    # 配置文件
│   ├── news_risk.md           # 新闻风控Agent SKILL
│   ├── strategy.md            # 策略Agent SKILL
│   ├── verifier.md            # 验证Agent SKILL
│   ├── executor.md            # 执行Agent SKILL
│   └── system_config.json     # 系统配置
├── src/
│   ├── orchestrator.py        # 主控调度器
│   ├── sub_agent.py           # 子Agent基类
│   ├── strategy_engine.py     # 策略引擎
│   ├── data_fetcher.py        # 数据采集器
│   └── executor.py            # 交易执行器
├── tests/                     # 测试文件
├── data/                      # 数据目录
│   ├── trading_state.json     # 状态持久化
│   └── trading_log.jsonl      # 交易日志
├── requirements.txt           # Python依赖
└── Dockerfile                 # 容器化配置
```

### 8.2 关键依赖版本

```
anthropic-sdk>=0.30.0
schedule>=1.2.0
pydantic>=2.0.0
httpx>=0.27.0
structlog>=24.0.0
```

### 8.3 环境变量配置

```bash
# Claude API 配置
CLAUDE_API_KEY=sk-ant-xxx
CLAUDE_API_URL=https://api.anthropic.com

# 系统配置
TRADING_STATE_FILE=./data/trading_state.json
TRADING_LOG_FILE=./data/trading_log.jsonl
LOG_LEVEL=INFO

# 券商API配置（生产环境使用）
BROKER_API_URL=https://broker-api.example.com
BROKER_API_KEY=xxx
```
