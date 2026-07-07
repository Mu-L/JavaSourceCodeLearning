+ 转载请标明出处



本章旨在快速扫盲\回顾 Apache Kafka 最新版本最核心、最重要的概念以及原理，不做具体框架源码级深入分析。

截至本文编写时间，Apache Kafka 官网下载页显示最新支持版本为 **Kafka 4.3.1**，发布日期为 **2026-06-25**。Kafka 4.x 之后的核心变化是 **ZooKeeper 架构退出主线，KRaft 成为 Kafka 元数据管理和集群控制的核心模式**。

参考来源：

+ Apache Kafka Downloads: https://kafka.apache.org/community/downloads/
+ Apache Kafka 4.3 Documentation: https://kafka.apache.org/43/
+ Apache Kafka Documentation: https://kafka.apache.org/documentation/

重点覆盖：

+ Kafka 是什么
+ Topic、Partition、Replica、Broker
+ Producer、Consumer、Consumer Group
+ Offset、Log、Segment、Index
+ ISR、Leader、Follower、高水位 HW
+ Controller、KRaft、Metadata Quorum
+ 生产者发送流程、ACK、幂等、事务
+ 消费者拉取流程、位移提交、Rebalance
+ 传统 Consumer Group 和新版 Consumer 协议
+ Share Group / Queues for Kafka
+ 顺序性、可靠性、可用性、一致性
+ Page Cache、顺序写、零拷贝
+ Log Retention、Log Compaction
+ Tiered Storage
+ Kafka Connect、Kafka Streams
+ 安全机制、监控指标、常见问题排查
+ 工程选型和实践建议

# Kafka
Kafka 是 **一个分布式事件流平台，用于高吞吐、低延迟、可持久化地发布、存储、订阅和处理事件数据**。

它主要解决：

1. **系统解耦**  
生产者只把消息写入 Kafka，不需要直接调用所有下游系统。
2. **削峰填谷**  
流量高峰先写入 Kafka，下游按自身能力消费。
3. **数据持久化**  
消息不是消费后立即删除，而是按保留策略存储一段时间。
4. **实时数据流处理**  
适合日志、埋点、监控、交易事件、CDC、实时计算。
5. **多订阅方消费**  
同一份数据可以被多个消费者组独立消费。

**传统同步调用**

```plain
订单服务
  -> 库存服务
  -> 积分服务
  -> 风控服务
  -> 推荐服务
```

调用链长，任何下游慢都会影响上游。

**使用 Kafka**

```plain
订单服务
  -> 写入 order_created Topic

库存服务消费
积分服务消费
风控服务消费
推荐服务消费
```

各系统通过事件解耦。

**需要注意**

+ Kafka 不是传统意义上“发完即删”的简单消息队列。
+ Kafka 的核心抽象是分布式日志。
+ Kafka 强在高吞吐、可扩展、可回放、多消费者订阅。
+ Kafka 不适合所有低延迟 RPC 场景，也不替代数据库事务。
+ Kafka 4.x 新集群应优先理解 KRaft，而不是旧 ZooKeeper 架构。

一句话总结：

**Kafka 是以分布式日志为核心的事件流平台，通过 Topic、Partition、Offset 和 Consumer Group 实现高吞吐、可持久化、可回放的数据流。**

# Topic
Topic 是 **Kafka 中消息的逻辑分类，相当于一类事件流的名字**。

它主要解决：

1. **按业务分类消息**
2. **隔离不同事件流**
3. **让生产者和消费者通过名称解耦**
4. **作为权限、保留策略、分区配置的管理单位**

**例子**

```plain
order_created
payment_success
user_login
stock_changed
app_log
```

**生产和消费**

```plain
Producer -> order_created Topic
Consumer <- order_created Topic
```

**Topic 不是物理上的一个文件**

Kafka 内部会把 Topic 拆成多个 Partition：

```plain
order_created
  -> partition-0
  -> partition-1
  -> partition-2
```

**需要注意**

+ Topic 命名要稳定，避免随意变更。
+ Topic 数量过多会增加元数据和运维成本。
+ Topic 的分区数、保留时间、清理策略要结合业务设计。
+ 不同业务语义的数据不要随意混入同一个 Topic。

一句话总结：

**Topic 是 Kafka 消息的逻辑分类，真正承载数据和并行能力的是 Topic 下的 Partition。**

# Partition
Partition 是 **Topic 的物理分片，也是 Kafka 并行、顺序和扩展能力的基础单位**。

它主要解决：

1. **单个 Topic 水平扩展**
2. **提升读写吞吐**
3. **让消费者并行消费**
4. **保证分区内消息顺序**

**结构**

```plain
Topic: order_created

partition-0:
  msg0, msg1, msg2

partition-1:
  msg0, msg1, msg2

partition-2:
  msg0, msg1, msg2
```

**分区内有序**

```plain
partition-0:
  offset 0
  offset 1
  offset 2
```

Kafka 只保证：

```plain
同一个 Partition 内有序
```

不保证：

```plain
整个 Topic 全局有序
```

**分区选择**

```plain
有 key:
  根据 key hash 选择分区

无 key:
  按生产者分区策略分配

自定义:
  业务实现 Partitioner
```

**需要注意**

+ 分区数决定消费者组内最大并行度。
+ 分区数不是越多越好，过多会增加文件、网络和元数据成本。
+ 扩容分区会影响 key 到分区的映射，可能破坏同 key 顺序。
+ 需要同一业务实体有序时，应让同一 key 进入同一分区。

一句话总结：

**Partition 是 Kafka 并行和顺序的核心单位，Kafka 保证分区内有序，不保证 Topic 全局有序。**

# Broker
Broker 是 **Kafka 集群中的服务节点，负责接收生产请求、存储分区日志、处理消费请求和复制数据**。

它主要解决：

1. **承载 Topic Partition 数据**
2. **处理客户端读写请求**
3. **参与副本复制**
4. **向集群汇报状态**
5. **作为 KRaft 节点参与元数据管理，取决于角色配置**

**集群结构**

```plain
Broker-1
  -> topicA partition-0 leader
  -> topicB partition-1 follower

Broker-2
  -> topicA partition-1 leader
  -> topicA partition-0 follower

Broker-3
  -> topicA partition-2 leader
  -> topicB partition-0 follower
```

**Broker 角色**

```plain
普通 Broker:
  处理数据读写

Controller:
  管理集群元数据和分区状态

Combined 节点:
  同时承担 Broker 和 Controller 角色
```

**需要注意**

+ Broker 宕机会触发副本 Leader 切换。
+ 单个 Broker 磁盘、网络、CPU 都可能成为瓶颈。
+ Broker 不是无状态服务，磁盘数据和 broker.id/node.id 很关键。
+ 生产环境要合理规划磁盘、网络、机架和副本分布。

一句话总结：

**Broker 是 Kafka 集群的数据服务节点，负责消息读写、日志存储、副本复制，并在 KRaft 架构中配合集群元数据管理。**

# Replica
Replica 是 **Partition 的副本，用于提升 Kafka 数据可靠性和可用性**。

它主要解决：

1. **Broker 宕机后的数据可用**
2. **降低单点故障风险**
3. **支持 Leader 故障切换**
4. **提升数据持久性**

**副本结构**

```plain
partition-0
  leader: Broker-1
  follower: Broker-2
  follower: Broker-3
```

**Leader 副本**

```plain
处理生产者写入
处理消费者读取
维护分区日志主流程
```

**Follower 副本**

```plain
从 Leader 拉取数据
保持日志同步
在 Leader 故障时可能被选为新 Leader
```

**副本因子**

```plain
replication.factor = 3
```

表示每个 Partition 有 3 个副本。

**需要注意**

+ 副本越多，可靠性越好，但存储和复制成本越高。
+ 生产常见副本因子是 3。
+ Follower 默认不对普通消费者提供读服务。
+ 副本分布要避免多个副本落在同一故障域。

一句话总结：

**Replica 通过 Leader/Follower 副本复制提升 Kafka 的可靠性和可用性，生产环境通常使用 3 副本。**

# ISR
ISR 是 **In-Sync Replicas，同步副本集合，表示当前跟得上 Leader 的副本列表**。

它主要解决：

1. **判断哪些副本是健康同步副本**
2. **控制消息提交安全性**
3. **Leader 故障时选择可靠新 Leader**
4. **配合 acks 和 min.insync.replicas 保证写入可靠性**

**结构**

```plain
partition-0
  leader: Broker-1
  ISR: [Broker-1, Broker-2, Broker-3]
```

如果 Broker-3 落后太多：

```plain
ISR: [Broker-1, Broker-2]
```

**写入确认**

```plain
acks=all
min.insync.replicas=2
```

含义：

```plain
消息至少要被 ISR 中足够数量副本确认
才认为写入成功
```

**需要注意**

+ ISR 不是固定副本列表，会动态变化。
+ ISR 缩小通常表示副本复制延迟或故障。
+ min.insync.replicas 设置过低会降低可靠性。
+ 设置过高会降低可用性，副本不足时写入失败。

一句话总结：

**ISR 表示当前与 Leader 保持同步的副本集合，是 Kafka 判断写入可靠性和故障切换安全性的核心机制。**

# Offset
Offset 是 **消息在 Partition 内的递增位置编号**。

它主要解决：

1. **标识消息位置**
2. **支持消费者断点续读**
3. **支持消息回放**
4. **让不同消费者组独立维护消费进度**

**结构**

```plain
partition-0:
  offset 0 -> msgA
  offset 1 -> msgB
  offset 2 -> msgC
```

**消费者进度**

```plain
consumer group A:
  partition-0 committed offset = 2

consumer group B:
  partition-0 committed offset = 0
```

同一个 Topic 可以被多个消费者组独立消费。

**Offset 提交**

```plain
消费消息
  -> 处理业务
  -> 提交 offset
```

**需要注意**

+ Offset 只在单个 Partition 内有意义。
+ 提交 offset 表示消费者认为之前的数据已经处理完成。
+ 先提交再处理可能丢消息。
+ 先处理再提交可能重复消费。
+ Kafka 通常要求业务端做好幂等处理。

一句话总结：

**Offset 是 Partition 内消息位置，消费者通过提交 Offset 记录消费进度，从而支持断点续读和消息回放。**

# Log
Kafka Log 是 **Partition 在磁盘上的追加写日志，每条消息按 Offset 顺序追加到日志末尾**。

它主要解决：

1. **消息持久化**
2. **顺序写入**
3. **按 Offset 查询**
4. **支持消息保留和回放**

**日志结构**

```plain
topic-partition/
  00000000000000000000.log
  00000000000000000000.index
  00000000000000000000.timeindex
  00000000000000100000.log
  00000000000000100000.index
  00000000000000100000.timeindex
```

**追加写**

```plain
Producer 写入
  -> Broker 追加到当前 active segment
  -> 分配 offset
  -> 等待确认
```

**读取**

```plain
Consumer 请求 offset
  -> Broker 通过 index 定位 segment
  -> 从 log 文件顺序读取
  -> 返回消息批次
```

**需要注意**

+ Kafka 不是随机更新消息，而是追加日志。
+ 日志文件按 Segment 切分，便于删除、压缩和索引。
+ 磁盘顺序写是 Kafka 高吞吐的重要原因。
+ 日志保留时间到期后，旧消息会被清理，不能无限回放。

一句话总结：

**Kafka 的核心存储模型是 Partition 追加日志，消息按 Offset 顺序写入磁盘，并通过 Segment 和索引支持高效读写。**

# Segment 和 Index
Segment 是 **Kafka 把 Partition 日志切成多个文件段的机制**。

Index 是 **Kafka 用于快速定位 Offset 或时间戳对应日志位置的稀疏索引**。

它主要解决：

1. **避免单个日志文件过大**
2. **方便按时间或大小清理旧数据**
3. **提升 Offset 定位效率**
4. **支持快速查找时间戳附近消息**

**Segment 文件**

```plain
00000000000000000000.log
00000000000000000000.index
00000000000000000000.timeindex
```

文件名前缀表示：

```plain
该 Segment 起始 offset
```

**索引定位**

```plain
Consumer 请求 offset=12345
  -> 找到起始 offset <= 12345 的 segment
  -> 查 offset index 找近似位置
  -> 从 log 文件顺序扫描到目标消息
```

**需要注意**

+ Kafka 使用稀疏索引，不是每条消息都有索引。
+ Segment 越大，文件数量少，但清理粒度更粗。
+ Segment 越小，清理更灵活，但文件数量更多。
+ 索引损坏通常可以根据 log 文件重建。

一句话总结：

**Segment 把分区日志拆成可管理文件段，Index 用稀疏索引快速定位消息位置，是 Kafka 高效存储和清理的基础。**

# Producer
Producer 是 **向 Kafka Topic 写入消息的客户端**。

它主要解决：

1. **序列化业务数据**
2. **选择目标 Topic 和 Partition**
3. **批量发送消息**
4. **处理重试、幂等和事务**
5. **接收 Broker 写入确认**

**发送流程**

```plain
业务调用 send()
  -> 序列化 key/value/header
  -> 分区器选择 partition
  -> 写入 RecordAccumulator
  -> Sender 线程批量发送
  -> Broker 写入日志
  -> 返回 ack
```

**重要参数**

```plain
acks:
  写入确认级别

batch.size:
  批次大小

linger.ms:
  等待聚合批次时间

compression.type:
  压缩方式

retries:
  重试次数

enable.idempotence:
  幂等生产
```

**需要注意**

+ batch.size 和 linger.ms 影响吞吐和延迟。
+ 压缩可以降低网络和磁盘成本，但增加 CPU 开销。
+ 重试可能导致乱序，幂等生产可以降低重复写入风险。
+ 生产者发送成功不等于所有消费者已经消费。

一句话总结：

**Producer 通过序列化、分区、批量、压缩、重试和 ACK 机制把业务事件高效可靠地写入 Kafka。**

# ACK
ACK 是 **生产者写入消息时要求 Broker 返回确认的级别**。

它主要解决：

1. **控制写入可靠性**
2. **平衡吞吐和数据安全**
3. **配合副本同步策略**

**acks=0**

```plain
Producer 不等待 Broker 确认
吞吐高
可能丢消息
```

**acks=1**

```plain
Leader 写入成功后返回
性能较好
Leader 宕机且 Follower 未同步时可能丢消息
```

**acks=all**

```plain
Leader 等待 ISR 副本满足要求后返回
可靠性最高
延迟更高
```

**常见可靠配置**

```plain
acks=all
min.insync.replicas=2
replication.factor=3
enable.idempotence=true
```

**需要注意**

+ acks=all 还需要合理设置 min.insync.replicas。
+ min.insync.replicas 过低会降低副本确认意义。
+ 副本不足时，acks=all 可能导致写入失败。
+ 高可靠配置会牺牲部分可用性和延迟。

一句话总结：

**ACK 决定生产者等待到什么程度才认为写入成功，acks=all 配合 ISR 和 min.insync.replicas 是高可靠写入的核心。**

# 幂等生产者
幂等生产者是 **Kafka 通过 Producer ID、Producer Epoch 和 Sequence Number 避免生产者重试导致重复写入的机制**。

它主要解决：

1. **网络超时后重试可能重复写入**
2. **Broker 已写入但 ACK 丢失**
3. **单分区内重试乱序和重复**

**问题场景**

```plain
Producer 发送 msg-1
  -> Broker 写入成功
  -> ACK 在网络中丢失
  -> Producer 重试 msg-1
  -> 可能重复写入
```

**幂等机制**

```plain
每个 Producer 有 PID
每个分区维护递增 sequence
Broker 检查 sequence 是否连续
重复 sequence 被识别并丢弃
```

**需要注意**

+ 幂等生产者主要保证单 Producer 会话内、单分区的幂等写入。
+ 它不等于业务幂等。
+ 应用重启、业务重放、消费者重复处理仍要业务幂等。
+ 跨分区、跨 Topic 的原子写入需要事务。

一句话总结：

**幂等生产者通过 PID 和序列号避免发送重试造成的重复写入，是 Kafka 精确一次语义的重要基础。**

# Kafka 事务
Kafka 事务是 **让生产者把多条消息、多个分区写入和消费位移提交作为一个原子单元提交或回滚的机制**。

它主要解决：

1. **跨分区原子写入**
2. **消费-处理-生产链路一致性**
3. **Exactly Once 语义基础**
4. **避免下游读到未提交事务消息**

**典型流程**

```plain
initTransactions
beginTransaction
  -> consume input records
  -> produce output records
  -> sendOffsetsToTransaction
commitTransaction
```

失败时：

```plain
abortTransaction
```

**读隔离级别**

```plain
read_uncommitted:
  可以读到未提交事务消息

read_committed:
  只读已提交事务消息
```

**需要注意**

+ Kafka 事务解决 Kafka 内部读写链路的一致性，不自动保证外部数据库事务。
+ 事务会增加协调成本和延迟。
+ transactional.id 必须稳定且唯一。
+ EOS 仍然要求业务端正确处理外部副作用。

一句话总结：

**Kafka 事务把多分区写入和消费位移提交纳入同一原子单元，是 Kafka Exactly Once 处理链路的核心机制。**

# Consumer
Consumer 是 **从 Kafka Topic 拉取并处理消息的客户端**。

它主要解决：

1. **按 Offset 拉取消息**
2. **维护消费进度**
3. **参与消费者组分区分配**
4. **处理重试、提交和回放**

**消费流程**

```plain
Consumer 加入 Consumer Group
  -> 获取分区分配
  -> poll 拉取消息
  -> 执行业务处理
  -> 提交 offset
```

**拉模式**

Kafka 消费是客户端主动拉取：

```plain
Consumer -> Broker: fetch request
Broker -> Consumer: records
```

优点：

```plain
消费者按自身能力拉取
容易做批量
方便控制进度
```

**需要注意**

+ poll 要持续调用，否则可能被认为消费者失活。
+ 消费处理慢会导致消费堆积。
+ 自动提交 offset 简单但容易产生丢失或重复风险。
+ 业务处理必须考虑重复消费。

一句话总结：

**Consumer 通过 poll 主动拉取消息，并用 Offset 管理消费进度；消费可靠性核心在于处理和提交 Offset 的顺序。**

# Consumer Group
Consumer Group 是 **Kafka 实现消费负载均衡和多订阅方隔离的机制**。

它主要解决：

1. **同组消费者分摊分区消费**
2. **不同组独立消费同一 Topic**
3. **消费者故障后自动接管分区**
4. **支持水平扩展消费能力**

**同组消费**

```plain
Topic: order_created
  partition-0
  partition-1
  partition-2

Group-A:
  consumer-1 -> partition-0
  consumer-2 -> partition-1
  consumer-3 -> partition-2
```

同一个消费者组内：

```plain
一个 Partition 同一时刻通常只分配给一个 Consumer
```

不同消费者组：

```plain
Group-A 可以消费一遍
Group-B 也可以独立消费一遍
```

**需要注意**

+ 同一组消费者数量超过分区数时，多出来的消费者会空闲。
+ 分区数决定同组最大并行消费数。
+ 消费者上下线会触发 Rebalance。
+ 不同业务系统通常应该使用不同 group.id。

一句话总结：

**Consumer Group 让同一组消费者分摊分区消费，让不同组可以独立订阅同一份 Kafka 数据。**

# Offset 提交
Offset 提交是 **消费者把已处理到的位置记录到 Kafka 内部 Topic 中的机制**。

它主要解决：

1. **消费者重启后从上次位置继续**
2. **消费者组故障恢复**
3. **消费进度可观测**
4. **支持手动回放和重置**

**自动提交**

```plain
enable.auto.commit=true
```

特点：

```plain
使用简单
可能处理前就提交
异常时可能丢消息
```

**手动提交**

```plain
处理成功后 commitSync 或 commitAsync
```

常见流程：

```plain
poll records
  -> 处理业务
  -> 处理成功
  -> commit offset
```

**重复和丢失**

```plain
先提交，再处理:
  处理失败会丢消息

先处理，再提交:
  提交失败会重复消费
```

**需要注意**

+ Kafka 默认更容易做到至少一次。
+ 想避免业务重复，必须实现幂等。
+ offset 提交的是下一条要消费的位置。
+ 提交粒度过细影响性能，过粗增加重复范围。

一句话总结：

**Offset 提交决定消费者故障恢复位置，可靠消费通常选择处理成功后再提交，并通过业务幂等接受重复消费。**

# Rebalance
Rebalance 是 **消费者组成员或订阅分区变化时，重新分配 Partition 的过程**。

它主要解决：

1. **消费者扩缩容**
2. **消费者故障接管**
3. **分区新增后的重新分配**
4. **消费负载均衡**

**触发条件**

```plain
消费者加入组
消费者离开组
消费者心跳超时
Topic 分区变化
订阅 Topic 变化
```

**传统流程**

```plain
暂停消费
  -> 组协调器确认成员
  -> 执行分区分配
  -> 通知消费者新分配
  -> 恢复消费
```

**问题**

```plain
消费暂停
重复消费风险
分区频繁迁移
消费延迟抖动
```

**优化方向**

```plain
静态成员
协作式再均衡
新版消费者组协议
合理 session.timeout.ms 和 max.poll.interval.ms
```

**需要注意**

+ Rebalance 不是错误，但频繁 Rebalance 是问题。
+ 处理时间超过 max.poll.interval.ms 可能被踢出消费者组。
+ 消费者扩缩容会带来短暂消费抖动。
+ 需要监控 Rebalance 次数和消费延迟。

一句话总结：

**Rebalance 负责消费者组分区重新分配，是消费弹性和故障接管的基础，但频繁发生会造成延迟和重复消费风险。**

# 新版 Consumer 协议
新版 Consumer 协议是 **Kafka 近年来对消费者组协调和 Rebalance 流程的改进方向，目标是降低协调成本、减少客户端复杂度和提升 Rebalance 稳定性**。

它主要解决：

1. **传统 Rebalance 停顿时间较长**
2. **客户端承担过多分配逻辑**
3. **成员变化时影响范围较大**
4. **大规模消费者组协调成本高**

**传统模型**

```plain
Consumer Leader 负责分区分配
Group Coordinator 管理成员
分配结果再同步给组成员
```

**新版思路**

```plain
Broker 侧承担更多协调职责
协议更适合增量变化
减少全组停顿
降低客户端分配复杂度
```

**需要注意**

+ 不同 Kafka 客户端版本对新协议支持程度不同。
+ 升级前要确认 broker、client、配置兼容性。
+ 新协议优化 Rebalance，但不消除业务处理慢导致的消费堆积。
+ 老版本消费者组仍然可能继续使用传统协议。

一句话总结：

**新版 Consumer 协议把消费者组协调能力进一步服务端化，目标是减少大规模消费者组 Rebalance 的停顿和复杂度。**

# Share Group
Share Group 是 **Kafka 4.x 引入的队列式消费能力方向，也常被称为 Queues for Kafka，用于让多个消费者共享处理同一分区中的不同消息**。

它主要解决：

1. **传统消费者组并行度受分区数限制**
2. **队列场景下不希望过度增加分区**
3. **多个消费者共同处理同一分区消息**
4. **消息级确认和重投递需求**

**传统 Consumer Group**

```plain
一个 Partition 同一时刻分配给一个 Consumer
消费者数量超过分区数会空闲
```

**Share Group**

```plain
多个 Consumer 可以共享同一 Partition 的消息处理
更接近队列语义
支持消息级别的处理跟踪
```

**适合场景**

```plain
任务队列
异步作业处理
消费者数量动态变化
不要求严格分区顺序的工作负载
```

**需要注意**

+ Share Group 更适合队列式任务，不是替代所有 Consumer Group。
+ 如果业务强依赖分区内顺序，仍要谨慎使用。
+ 客户端和 Broker 版本都要确认支持。
+ 消息级确认也要求业务处理幂等。

一句话总结：

**Share Group 让 Kafka 支持更接近队列的消费模型，突破传统消费者组并行度受分区数限制的问题。**

# KRaft
KRaft 是 **Kafka Raft Metadata mode，用 Kafka 自身的 Raft 协议管理集群元数据，替代旧 ZooKeeper 架构**。

它主要解决：

1. **去除 ZooKeeper 外部依赖**
2. **统一 Kafka 元数据管理**
3. **提升控制面扩展性和恢复速度**
4. **简化部署和运维**

**旧架构**

```plain
Broker
  -> ZooKeeper 保存元数据
  -> Controller 监听和管理集群状态
```

**KRaft 架构**

```plain
Controller Quorum
  -> 使用 Raft 管理元数据日志
  -> Broker 从 Controller 获取元数据
```

**角色**

```plain
broker:
  处理数据读写

controller:
  管理元数据、分区状态、Leader 选举

broker,controller:
  同一进程同时承担两种角色
```

**需要注意**

+ Kafka 4.x 新集群应按 KRaft 理解和部署。
+ Controller Quorum 的节点数和可靠性非常关键。
+ 元数据日志也需要持久化和备份意识。
+ 从旧 ZooKeeper 集群迁移要严格遵循版本和迁移流程。

一句话总结：

**KRaft 用 Kafka 自身的 Raft 元数据仲裁替代 ZooKeeper，是 Kafka 4.x 集群控制面的核心架构。**

# Controller
Controller 是 **Kafka 集群控制面核心角色，负责管理元数据、分区 Leader、Broker 状态和集群变更**。

它主要解决：

1. **分区 Leader 选举**
2. **Broker 上下线处理**
3. **Topic 和 Partition 元数据管理**
4. **副本状态变更**
5. **集群控制事件传播**

**KRaft Controller Quorum**

```plain
Controller-1
Controller-2
Controller-3
```

通过 Raft 维护元数据日志：

```plain
metadata log
  -> topic 创建
  -> partition leader 变更
  -> broker 注册
  -> 配置变更
```

**Broker 获取元数据**

```plain
Broker 启动
  -> 注册到 Controller
  -> 接收元数据变更
  -> 根据分区角色处理读写和复制
```

**需要注意**

+ Controller 不直接承载普通消息数据读写，主要负责控制面。
+ Controller Quorum 不稳定会影响集群管理能力。
+ Controller 与 Broker 的网络、磁盘、时间配置都要稳定。
+ 小集群可以 combined 模式，大规模生产更建议角色隔离。

一句话总结：

**Controller 是 Kafka 控制面核心，KRaft 模式下通过 Controller Quorum 和元数据日志管理整个集群状态。**

# 高水位 HW
高水位 HW 是 **High Watermark，表示消费者可见的最高已提交 Offset 边界**。

它主要解决：

1. **防止消费者读到未复制安全的数据**
2. **定义已提交消息范围**
3. **支持副本故障恢复**
4. **保证消费者读取的一致性**

**简化示例**

```plain
Leader log end offset = 10
Follower-1 log end offset = 10
Follower-2 log end offset = 8

HW = 8
```

消费者只能读到：

```plain
offset < HW
```

**为什么需要 HW**

如果消费者读到 Leader 上还没复制到足够副本的数据：

```plain
Leader 宕机
新 Leader 没有这条消息
消费者之前读到的数据消失
```

HW 避免这种不一致。

**需要注意**

+ HW 推进依赖副本复制进度。
+ 副本落后会导致 HW 推进变慢。
+ 消费延迟不只和消费者有关，也可能和副本同步有关。
+ 事务场景还会涉及 LSO 等读可见边界。

一句话总结：

**HW 定义消费者可见的已提交日志边界，确保消费者不会读到可能在故障切换中丢失的未安全复制数据。**

# 顺序性
Kafka 顺序性是 **Kafka 保证同一 Partition 内消息按 Offset 顺序追加和读取，但不保证 Topic 全局顺序**。

它主要解决：

1. **同一业务实体事件有序**
2. **状态变更按顺序处理**
3. **避免乱序更新导致状态错误**

**保证同 key 有序**

```plain
key = orderId

order_created
order_paid
order_shipped
```

通过 key hash 进入同一分区：

```plain
partition-3:
  order_created
  order_paid
  order_shipped
```

**无法天然保证**

```plain
不同 Partition 之间的全局顺序
多个 Producer 之间的严格全局顺序
扩分区后的同 key 历史映射不变
```

**需要注意**

+ 需要有序的业务必须设计 key。
+ 消费端并发处理同一分区消息也可能破坏业务顺序。
+ 重试、异步处理、死信补偿都可能引入乱序。
+ 全局有序通常意味着单分区，吞吐和可用性会受限。

一句话总结：

**Kafka 只保证分区内顺序，业务要通过合理 key 设计把同一实体事件路由到同一分区。**

# 可靠性
Kafka 可靠性是 **消息从生产、存储、复制到消费处理过程中尽量不丢失、可恢复、可重试的能力**。

它主要依赖：

```plain
replication.factor
acks
min.insync.replicas
ISR
幂等生产者
事务
手动提交 offset
消费者幂等
```

**高可靠生产**

```plain
replication.factor=3
min.insync.replicas=2
acks=all
enable.idempotence=true
```

**高可靠消费**

```plain
关闭自动提交
处理成功后提交 offset
业务幂等
失败重试
死信或补偿
```

**需要注意**

+ Kafka 能降低消息丢失概率，但不能替业务处理外部副作用兜底。
+ 消费端提交 offset 和业务数据库事务之间仍可能不一致。
+ 高可靠配置会增加延迟并降低部分故障场景下可用性。
+ 消息至少一次通常比恰好一次更常见，业务幂等必须做。

一句话总结：

**Kafka 可靠性来自副本、ACK、ISR、幂等、事务和正确的 Offset 提交，最终仍需要业务幂等和补偿闭环。**

# Exactly Once
Exactly Once 是 **Kafka 在特定读写 Kafka 的处理链路中，通过幂等生产者和事务实现的一次性处理语义**。

它主要解决：

1. **生产重试导致重复写入**
2. **消费后生产再提交 Offset 的一致性**
3. **流处理拓扑中重复输出**

**典型场景**

```plain
input-topic
  -> Kafka Streams 处理
  -> output-topic
```

事务保证：

```plain
输出消息
  + 输入 offset 提交
作为一个事务提交
```

**需要注意**

+ Exactly Once 通常限定在 Kafka 到 Kafka 的处理链路内。
+ 如果写外部数据库、调用 HTTP 接口，仍要外部系统支持幂等或事务。
+ EOS 会带来额外协调开销。
+ 不要把 Kafka EOS 理解成所有业务副作用绝对只发生一次。

一句话总结：

**Kafka Exactly Once 主要保证 Kafka 内部消费-处理-生产链路的一致性，外部系统仍需要幂等和事务配合。**

# Page Cache
Page Cache 是 **Linux 用内存缓存文件数据的机制，也是 Kafka 高吞吐的重要基础**。

它主要解决：

1. **减少磁盘读写延迟**
2. **让顺序写更高效**
3. **利用操作系统缓存热点日志**
4. **降低 JVM 堆内缓存压力**

**写入流程**

```plain
Broker 追加消息
  -> 写入 OS Page Cache
  -> 后台刷盘
```

**读取流程**

```plain
Consumer 拉取消息
  -> 如果日志在 Page Cache
  -> 直接从内存读取
  -> 否则触发磁盘读取
```

**为什么 Kafka 不把消息都放 JVM 堆**

```plain
避免 GC 压力
利用 OS Page Cache
支持大数据量日志缓存
```

**需要注意**

+ Kafka 写入返回不一定等于立刻 fsync 到磁盘。
+ Page Cache 被挤压会影响消费读取性能。
+ Broker 内存规划要给 Page Cache 留空间。
+ 不要只看 JVM 堆，还要看系统可用内存和磁盘缓存。

一句话总结：

**Kafka 高吞吐大量依赖 Linux Page Cache，合理的内存规划应给操作系统缓存留出足够空间。**

# 顺序写和零拷贝
顺序写和零拷贝是 **Kafka 高吞吐的重要底层优化思想**。

**顺序写**

Kafka 追加日志：

```plain
只在文件末尾追加
避免大量随机写
充分利用磁盘顺序写能力
```

**零拷贝**

消费发送数据时：

```plain
磁盘文件
  -> Page Cache
  -> Socket
  -> 网卡
```

减少：

```plain
内核态到用户态拷贝
用户态到内核态拷贝
CPU 拷贝成本
```

**典型机制**

```plain
sendfile
transferTo
```

**需要注意**

+ 零拷贝不是完全没有拷贝，而是减少不必要的数据拷贝。
+ 压缩、加密、消息转换可能影响零拷贝路径。
+ 顺序写不代表磁盘永远不是瓶颈。
+ 网络带宽常常比磁盘更早成为 Kafka 瓶颈。

一句话总结：

**Kafka 通过追加顺序写和零拷贝减少磁盘随机 IO 与 CPU 拷贝开销，从而获得高吞吐。**

# Log Retention
Log Retention 是 **Kafka 按时间或大小保留消息日志，超过保留策略后清理旧 Segment 的机制**。

它主要解决：

1. **控制磁盘空间**
2. **支持一段时间内消息回放**
3. **让 Kafka 成为持久事件日志**
4. **自动清理过期数据**

**常见配置**

```plain
retention.ms
retention.bytes
segment.ms
segment.bytes
```

**清理流程**

```plain
Segment 达到保留条件
  -> 被标记为可删除
  -> 后台清理线程删除
```

**需要注意**

+ Retention 是按 Segment 粒度清理，不是逐条消息。
+ 消费者太慢，消息可能在消费前被清理。
+ 磁盘容量规划必须结合写入速率和保留时间。
+ Retention 不等于归档，长期留存可考虑 Tiered Storage。

一句话总结：

**Log Retention 通过时间和大小策略清理旧日志，是 Kafka 控制磁盘空间和支持有限回放窗口的核心机制。**

# Log Compaction
Log Compaction 是 **Kafka 按 key 保留最新值、清理旧值的日志压缩机制**。

它主要解决：

1. **保存最新状态**
2. **支持状态重建**
3. **减少相同 key 的历史冗余**
4. **适合变更日志和配置类数据**

**普通日志**

```plain
key=user1 value=A
key=user1 value=B
key=user1 value=C
```

压缩后至少保留：

```plain
key=user1 value=C
```

**删除语义**

```plain
key=user1 value=null
```

称为 tombstone，用于表示删除。

**适合场景**

```plain
用户最新状态
配置变更
CDC upsert 事件
状态存储 changelog
```

**需要注意**

+ Compaction 不保证立刻清理。
+ 同一个 key 的旧值在一段时间内仍可能存在。
+ key 不能为空，否则无法按 key 压缩。
+ 不适合需要完整历史审计的场景。

一句话总结：

**Log Compaction 按 key 保留最新状态，适合状态重建和变更日志，不适合完整历史事件审计。**

# Tiered Storage
Tiered Storage 是 **Kafka 把较旧日志从本地磁盘分层存储到远端存储的能力，用于降低本地磁盘压力并扩大保留窗口**。

它主要解决：

1. **本地磁盘容量有限**
2. **长时间保留消息成本高**
3. **历史数据回放需求**
4. **冷热数据分层**

**基本思路**

```plain
热数据:
  保留在 Broker 本地磁盘

冷数据:
  上传到远端对象存储或远端存储系统
```

消费旧数据时：

```plain
Consumer 请求旧 offset
  -> Broker 从远端读取
  -> 返回给 Consumer
```

**需要注意**

+ Tiered Storage 会引入远端存储延迟和成本。
+ 历史回放性能通常不如本地热数据。
+ 远端存储可靠性和权限也要纳入运维。
+ 要区分本地保留策略和远端保留策略。

一句话总结：

**Tiered Storage 通过冷热分层把旧日志放到远端存储，扩大 Kafka 数据保留窗口并降低本地磁盘压力。**

# Kafka Connect
Kafka Connect 是 **Kafka 的数据集成框架，用于把外部系统数据导入 Kafka，或把 Kafka 数据导出到外部系统**。

它主要解决：

1. **数据库 CDC 接入**
2. **日志和文件采集**
3. **写入搜索、数仓、对象存储**
4. **减少重复开发导入导出程序**

**Source Connector**

```plain
外部系统
  -> Connector
  -> Kafka Topic
```

例如：

```plain
MySQL CDC -> Kafka
```

**Sink Connector**

```plain
Kafka Topic
  -> Connector
  -> 外部系统
```

例如：

```plain
Kafka -> Elasticsearch
Kafka -> S3
```

**需要注意**

+ Connector 也要考虑 offset、重试、幂等和死信。
+ Source 和 Sink 的一致性语义取决于外部系统能力。
+ 大规模 Connect 集群要关注任务分配和 Rebalance。
+ Connector 配置错误可能导致重复写入或数据延迟。

一句话总结：

**Kafka Connect 用标准化 Connector 把 Kafka 与数据库、搜索、数仓、对象存储等系统连接起来，是数据集成的重要组件。**

# Kafka Streams
Kafka Streams 是 **Kafka 官方 Java 流处理库，用于基于 Kafka Topic 构建实时处理应用**。

它主要解决：

1. **流式转换**
2. **聚合和窗口**
3. **Join**
4. **状态ful 处理**
5. **Exactly Once 流处理**

**基本模型**

```plain
input topic
  -> filter/map/groupBy/window/join
  -> output topic
```

**状态存储**

```plain
本地 State Store
  -> changelog topic
  -> 故障后从 changelog 恢复
```

**常见场景**

```plain
实时统计
风控规则
订单状态聚合
用户行为窗口计算
流表 Join
```

**需要注意**

+ Kafka Streams 是库，不是独立集群计算框架。
+ 应用实例数量和 Topic 分区数影响并行度。
+ 状态存储需要磁盘和恢复时间规划。
+ 复杂大规模计算可以评估 Flink、Spark Streaming 等框架。

一句话总结：

**Kafka Streams 让应用直接基于 Kafka 构建实时流处理拓扑，适合轻量到中等复杂度的流式计算。**

# 安全机制
Kafka 安全机制包括 **认证、授权、加密和审计相关能力**。

它主要解决：

1. **客户端身份确认**
2. **Topic 访问控制**
3. **网络传输加密**
4. **多租户隔离**
5. **操作审计**

**常见能力**

```plain
SSL/TLS:
  加密传输和证书认证

SASL:
  用户认证机制

ACL:
  资源访问控制

Principal:
  客户端身份

Authorizer:
  授权判断
```

**ACL 示例语义**

```plain
用户 userA
  -> 允许写 Topic order_created
  -> 允许读 Group inventory-service
```

**需要注意**

+ 生产环境不要裸奔明文无认证。
+ ACL 要按最小权限配置。
+ 证书和密码要有轮换机制。
+ 多租户 Kafka 要同时做配额、ACL 和 Topic 命名规范。

一句话总结：

**Kafka 安全依赖 TLS、SASL、ACL 和配额等机制，核心目标是确认身份、限制权限和保护数据传输。**

# 监控指标
Kafka 监控是 **围绕 Broker、Topic、Partition、Producer、Consumer、Controller 和系统资源建立可观测性的过程**。

重点关注：

```plain
Broker 存活
Controller 状态
Under Replicated Partitions
Offline Partitions
ISR 变化
请求延迟
生产吞吐
消费吞吐
Consumer Lag
磁盘使用率
网络吞吐
Page Cache 命中
GC
```

**Consumer Lag**

```plain
Lag = 最新 offset - 已提交 offset
```

表示：

```plain
消费者落后多少消息
```

**副本健康**

```plain
Under Replicated Partitions > 0:
  有副本落后或不可用

Offline Partitions > 0:
  有分区不可服务
```

**需要注意**

+ 只看 Broker 是否存活不够。
+ Lag 高可能是消费者慢，也可能是分区不均、下游慢、Rebalance 频繁。
+ 磁盘满会直接影响 Broker 稳定性。
+ Controller 和元数据仲裁健康在 KRaft 模式下非常重要。

一句话总结：

**Kafka 监控要同时关注吞吐、延迟、Lag、副本健康、Controller、磁盘、网络和 JVM，不能只看进程存活。**

# 常见问题排查
Kafka 问题通常涉及客户端、Broker、网络、磁盘、Controller、消费者组和业务处理多个层面。

**消息堆积**

排查方向：

```plain
Consumer Lag 是否持续增长
消费者实例数是否不足
分区数是否限制并行度
业务处理是否变慢
下游数据库是否慢
是否频繁 Rebalance
```

**生产延迟升高**

排查方向：

```plain
acks 配置是否变更
ISR 是否缩小
Broker 请求队列是否堆积
磁盘 IO 是否变慢
网络是否打满
batch 和 linger 是否合理
```

**频繁 Rebalance**

排查方向：

```plain
消费者是否频繁重启
poll 间隔是否超过 max.poll.interval.ms
session.timeout.ms 是否太短
GC 是否过长
网络是否抖动
```

**副本不同步**

排查方向：

```plain
Follower 网络是否慢
Broker 磁盘 IO 是否慢
复制线程是否繁忙
Leader 写入压力是否过高
跨机房复制是否延迟大
```

**消息重复消费**

排查方向：

```plain
是否处理成功但 offset 提交失败
是否 Rebalance 后重复拉取
是否生产者重试重复写
业务是否缺少幂等键
```

**需要注意**

+ 先区分生产端、Broker 端、消费端、下游业务端。
+ Kafka 层成功不代表业务处理成功。
+ 消息重复是常态风险，业务必须幂等。
+ 排查要看时间线：配置变更、发布、流量峰值、Broker 事件。

一句话总结：

**Kafka 排查要从生产、存储、复制、消费、下游和控制面逐层定位，Lag、ISR、Rebalance、磁盘和网络是最常见线索。**

# 工程选型
Kafka 适合高吞吐、可持久化、可回放的事件流场景，但不是所有异步通信都适合 Kafka。

**适合 Kafka**

```plain
日志采集
埋点数据
CDC 数据流
事件驱动架构
实时计算输入
多系统订阅同一事件
高吞吐异步处理
数据回放
```

**不适合 Kafka**

```plain
极低延迟 RPC
小规模简单任务队列
强事务同步调用
复杂延迟调度
大量单条消息精细 ACK 的传统队列场景
```

**选型关注**

```plain
吞吐量
消息保留时间
顺序要求
可靠性要求
消费并行度
运维能力
客户端生态
监控和告警
```

**需要注意**

+ Kafka 强项是事件流和日志，不是简单 RPC 替代品。
+ 分区数、副本数、保留时间要在上线前规划。
+ 高可靠要接受更高延迟和更复杂配置。
+ 上线 Kafka 必须配套监控、容量规划和故障演练。

一句话总结：

**Kafka 最适合高吞吐、可持久化、可回放、多订阅的事件流场景，选型时要重点评估顺序、可靠性、延迟、容量和运维成本。**

# 总结
Kafka 的核心不是简单消息队列，而是一个围绕分布式日志构建的事件流平台。

常见理解路径：

```plain
基础模型:
  Topic、Partition、Replica、Broker

写入链路:
  Producer、Partitioner、Batch、ACK、ISR、幂等、事务

存储模型:
  Log、Segment、Index、Page Cache、顺序写、零拷贝

消费模型:
  Consumer、Consumer Group、Offset、Rebalance、Share Group

控制面:
  KRaft、Controller、Metadata Quorum

可靠性:
  副本、ISR、HW、acks、min.insync.replicas、事务

生态:
  Kafka Connect、Kafka Streams、Tiered Storage

运维:
  Lag、ISR、Controller、磁盘、网络、GC、Rebalance
```

一句话总结：

**Kafka 的本质是分布式、可复制、可持久化、可回放的日志系统；真正用好 Kafka，要理解分区日志、消费者组、副本同步、KRaft 控制面和端到端可靠性。**
