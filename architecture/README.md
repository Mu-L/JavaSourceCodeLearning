# 架构方案伪代码库

`architecture` module 用于沉淀不同业务场景下的架构思路、设计方案和关键伪代码。它关注的是
复杂问题如何拆分、核心流程如何协作、数据一致性如何保证，以及不同技术组件之间的边界，而不是提供
可以直接上线的完整业务系统。

当前实现以机票业务为背景，模拟以下两个相互关联的架构场景：

1. **多供应商并发搜索**：一次机票查询并发分发给多个供应商，通过异步回调聚合结果，并支持超时返回部分结果。
2. **海量政策高性能匹配**：使用 RocksDB 保存政策明细、RoaringBitmap 构建匹配索引，并通过版本化快照实现运行时无损切换。

后续会继续增加交易、下单等其他场景的架构伪代码，使该 module 逐步形成可复用的架构与设计方案集合。

## 模块目标

- 用最少的代码表达架构中的核心职责、协作关系和一致性约束。
- 为类似业务问题提供可讨论、可验证、可演进的设计参考。
- 通过接口隔离外部系统，使方案不被 Dubbo、Kafka、Redis 等具体技术绑定。
- 通过测试验证关键架构行为，而不是覆盖完整的生产业务流程。
- 持续积累搜索、政策匹配、交易、下单等不同场景的方案。

本模块属于架构伪代码，生产落地时仍需根据实际情况补充鉴权、限流、熔断、监控、链路追踪、异常分级、
数据迁移、容量评估、容灾和部署方案。

## 场景目录

| 场景 | 核心问题 | 当前状态 |
| --- | --- | --- |
| 多供应商并发搜索 | 任务分发、异步回调、结果聚合、超时与迟到回调 | 已实现伪代码 |
| 海量政策匹配 | 全量加载、增量追平、位图索引、快照校验与热切换 | 已实现伪代码 |
| 交易/下单 | 幂等、状态流转、库存确认、分布式一致性与补偿 | 待补充 |

## 分层结构

代码先区分项目级公开契约、公共能力和具体业务场景，再在场景内部按照应用、领域和基础设施分层。

```text
com.arch.policy
├── api
│   └── search                 # 项目级搜索契约、请求和响应 DTO
├── common
│   ├── config                 # Spring Bean 装配
│   └── model                  # 可跨场景复用的模型
├── search
│   ├── application            # 查询编排、回调处理、启动和增量更新用例
│   ├── domain
│   │   └── snapshot           # RocksDB/位图快照领域逻辑
│   └── infrastructure
│       ├── demo               # 模拟供应商
│       ├── kafka              # 政策变更消息适配器
│       ├── redis              # 聚合状态和完成通知适配器
│       └── rpc                # Dubbo 接口实现
└── PolicySearchApplication    # 当前搜索场景的启动入口
```

依赖方向如下：

```mermaid
flowchart LR
    Caller["外部调用方"] --> API["api.search<br/>项目级公开契约"]
    Infrastructure["search.infrastructure<br/>RPC / Redis / Kafka"] --> API
    Infrastructure --> Application["search.application<br/>流程编排"]
    Application --> Domain["search.domain<br/>领域规则"]
    Application --> Common["common<br/>公共配置与模型"]
    Domain --> Common
```

公开 API 不依赖场景内部实现，领域层不依赖 Dubbo、Redis、Kafka 等外部技术。后续增加交易场景时，
可以平行新增 `api.order` 和 `order.application/domain/infrastructure`，避免交易代码与搜索代码混杂。
只有真正跨场景稳定复用的模型或能力才应放入 `common`。

## 场景一：多供应商并发搜索

查询服务将一次请求分发给多个供应商。供应商可以分批回调结果，最后一次回调负责声明该供应商完成。
Redis 保存聚合结果、待完成供应商集合和查询状态，是整个流程的唯一事实来源。

```mermaid
sequenceDiagram
    autonumber
    actor Caller as 调用方
    participant SearchAPI as PolicySearchRpcService
    participant Coordinator as AsyncSearchCoordinator
    participant Redis as RedisSearchStateStore
    participant Dispatcher as SupplierTaskDispatcher
    participant Supplier as 多个供应商
    participant CallbackAPI as SupplierCallbackRpcService
    participant Callback as SupplierCallbackService
    participant Waiter as LocalSearchWaiters

    Caller->>SearchAPI: asyncSearch(request)
    SearchAPI->>Coordinator: 执行查询
    Coordinator->>Redis: 初始化状态、待完成集合和 TTL
    Coordinator->>Waiter: 注册本地等待器
    Coordinator->>Dispatcher: 并发分发供应商任务
    Dispatcher-->>Supplier: 异步查询

    loop 供应商分批返回结果
        Supplier->>CallbackAPI: callback(result, searchFinished)
        CallbackAPI->>Callback: 处理回调
        Callback->>Redis: 追加结果并更新待完成集合
    end

    Redis-->>Waiter: 全部完成后发布通知
    Waiter-->>Coordinator: 提前唤醒
    Coordinator->>Redis: 读取最终状态和结果
    Coordinator-->>SearchAPI: PolicySearchResponse
    SearchAPI-->>Caller: 完成异步调用
```

本地等待器和 Redis Pub/Sub 只用于提前唤醒。即使通知丢失，查询线程仍会定期检查 Redis。超过总超时时间后，
Lua 脚本会原子地将查询状态更新为 `TIMED_OUT`，返回已经收到的部分结果，并拒绝迟到回调继续修改结果。

## 场景二：海量政策匹配与快照切换

每个政策快照使用独立目录保存 RocksDB 明细库，并在内存中维护 RoaringBitmap 匹配索引。新版本先在旁路
完成全量加载、增量追平和一致性校验，只有通过校验后才会替换当前服务版本。

```mermaid
flowchart TD
    Start["应用启动或运行时刷新"] --> Create["创建版本隔离目录"]
    Create --> Full["加载全量政策"]
    Full --> Position["记录全量数据切点"]
    Position --> Replay["回放切点后的增量消息"]
    Replay --> CaughtUp{"已追平最新位置？"}
    CaughtUp -- 否 --> Replay
    CaughtUp -- 是 --> Validate["校验明细、索引和消息位置"]
    Validate --> Valid{"校验成功？"}
    Valid -- 否 --> Discard["关闭并删除候选快照"]
    Valid -- 是 --> Activate["原子激活新快照"]
    Activate --> NewQuery["新查询使用新版本"]
    Activate --> Drain["旧版本等待已有查询释放租约"]
    Drain --> Delete["关闭并删除旧快照"]
```

查询通过 `ActiveSnapshotRegistry.acquire()` 获取快照租约，并使用 `try-with-resources` 释放。切换完成后，
新查询立即使用新快照，旧快照则保留到最后一个旧查询结束，从而避免正在执行的查询访问已关闭的 RocksDB。

快照方案的主要扩展点：

1. `FullPolicyLoader`：加载全量政策，并返回全量数据对应的消息位置。
2. `IncrementalReplayer`：回放全量切点之后的新增、修改和删除事件。
3. `SnapshotValidator`：校验政策明细、位图索引和消息位置的一致性。
4. `SnapshotDirectory`：隔离不同快照版本的存储目录。
5. `PolicySnapshotService`：负责首次初始化、失败重试和运行时刷新。

Kafka 政策变更消息使用全局单调递增的 `position`，重复或乱序消息会被忽略。如果 Topic 使用多个分区，
生产端必须提供全局序列；否则应将当前位置模型调整为按分区保存 offset。

## 新增架构场景的约定

新增交易、下单或其他架构伪代码时，应遵循以下约定：

1. 在 README 的场景目录中说明要解决的问题、关键约束和方案状态。
2. 对外契约放在 `api.<scene>`，内部实现放在对应的 `<scene>` 业务包。
3. 使用 `application/domain/infrastructure` 表达职责边界，不让领域规则依赖具体中间件。
4. 优先提供展示关键协作关系的最小实现，避免把伪代码扩展成不完整的生产框架。
5. 为幂等、一致性、并发、超时、切换和失败隔离等关键架构行为编写测试。
6. 在场景文档中记录设计取舍、适用边界，以及生产落地仍需补充的能力。

## 运行验证

在仓库根目录执行：

```bash
mvn -f architecture/pom.xml clean test
```

当前测试覆盖异步供应商聚合、超时返回部分结果、全量加增量快照构建、运行时快照切换以及失败候选版本隔离。
