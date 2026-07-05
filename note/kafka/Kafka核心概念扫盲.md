+ 转载请标明出处



本章旨在快速扫盲\回顾kafka关键概念以及定义，不做原理深入分析。

# Producer
Kafka Producer 是 **消息生产者**，负责把业务数据写入 Kafka Topic。

它主要解决：

1. **发送消息**  
把应用产生的事件、日志、订单、状态变更写入 Kafka。
2. **选择分区**  
决定消息写到 Topic 的哪个 Partition。
3. **提高吞吐**  
通过批量发送、压缩、异步发送提升写入性能。
4. **保证可靠性**  
通过 `acks`、重试、幂等、事务控制写入语义。

**Producer 的原理**

Producer 发送消息大致流程：

```plain
业务线程
  -> 序列化 key/value
  -> 选择 Partition
  -> 写入本地缓冲区
  -> Sender 线程批量发送给 Partition Leader
  -> Broker 返回 ack
```

分区选择规则：

```plain
指定 partition：直接写入该 partition
有 key：通常按 key hash 选择 partition
无 key：默认策略尽量均衡分散写入
```

Producer 不直接写所有副本，只写 **Partition Leader**：

```plain
Producer -> Leader Replica -> Follower Replica 同步
```

写入是否成功，取决于 `acks`：

```plain
acks=0   不等响应，最快但可能丢
acks=1   Leader 写入成功即返回
acks=all 等 ISR 中足够副本确认后返回，最可靠
```

**关键机制**

`batch.size`：控制批次大小，提高吞吐。

`linger.ms`：等待凑批时间，增大可提升吞吐但增加延迟。

`compression.type`：压缩消息，减少网络和磁盘开销。

`retries`：失败自动重试。

`enable.idempotence=true`：启用幂等，避免重试导致重复写入。

`transactional.id`：启用事务，支持跨分区原子写入和 Exactly Once。

**需要注意**

+ Producer 是异步批量发送，不是每条消息都立刻发到 Broker。
+ 相同 key 通常进入同一 Partition，可保证 key 级顺序。
+ Kafka 只保证单 Partition 内有序，不保证 Topic 全局有序。
+ 可靠写入常用：

```plain
acks=all
enable.idempotence=true
retries=2147483647
```

+ 更强可靠性还要配合 Topic：

```plain
replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false
```

一句话总结：

**Producer 是 Kafka 的写入端，负责序列化消息、选择分区、批量压缩发送到 Partition Leader，并通过 ack、重试、幂等和事务控制吞吐与可靠性的平衡。**

# Consumer
Kafka Consumer 是 **消息消费者**，负责从 Kafka Topic 中拉取消息并交给业务处理。

它主要解决：

1. **读取消息**  
从 Topic 的 Partition 中按 offset 拉取数据。
2. **记录进度**  
通过提交 offset 记录自己消费到哪里。
3. **水平扩展**  
多个 Consumer 组成 Consumer Group，共同消费多个 Partition。
4. **故障转移**  
某个 Consumer 挂掉后，它负责的 Partition 会通过 Rebalance 分给其他 Consumer。

**Consumer 的原理**

Consumer 是 **主动拉取模型**，不是 Broker 推送。

基本流程：

```plain
Consumer 订阅 Topic
  -> 加入 Consumer Group
  -> Coordinator 分配 Partition
  -> Consumer 从 Partition Leader 拉取消息
  -> 业务处理消息
  -> 提交 offset
```

Consumer 实际消费的是 Partition：

```plain
Topic
 ├── Partition-0 -> Consumer-A
 ├── Partition-1 -> Consumer-B
 └── Partition-2 -> Consumer-A
```

同一个 Consumer Group 内：

```plain
一个 Partition 同一时刻只能被一个 Consumer 消费
一个 Consumer 可以消费多个 Partition
```

不同 Consumer Group 之间互不影响，可以各自完整消费同一份数据。

**Offset 机制**

Offset 是 Consumer 的消费进度：

```plain
Partition-0: offset 0, 1, 2, 3...
```

Consumer 处理完消息后提交 offset。提交的位置通常表示：

```plain
下一条要消费的 offset
```

Offset 默认存储在 Kafka 内部 topic：

```plain
__consumer_offsets
```

提交方式：

```plain
自动提交：简单，但可能重复或丢处理语义
手动提交：可控，生产更常用
```

**需要注意**

+ Kafka 保证 Partition 内有序，不保证 Topic 全局有序。
+ Consumer 数量超过 Partition 数，多出来的 Consumer 会空闲。
+ `max.poll.interval.ms` 控制两次 poll 的最大间隔，处理太慢可能被踢出 Group。
+ `session.timeout.ms` 和 `heartbeat.interval.ms` 控制心跳存活。
+ Rebalance 会导致短暂停顿，也可能带来重复消费。
+ 业务要按“至少一次”语义设计，处理逻辑最好支持幂等。

**常见关键配置**

```plain
group.id=order-consumer
enable.auto.commit=false
auto.offset.reset=latest
max.poll.records=500
max.poll.interval.ms=300000
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

事务场景：

```plain
isolation.level=read_committed
```

一句话总结：

**Consumer 是 Kafka 的读取端，通过主动拉取 Partition 数据、提交 offset 记录进度，并依靠 Consumer Group 和 Rebalance 实现并行消费与故障转移。**

# Broker
Kafka Broker 是 **Kafka 集群中的服务节点**。

它主要负责：

1. **存储数据**  
保存 Topic 的 Partition 日志文件。
2. **处理读写请求**  
Producer 写入消息，Consumer 拉取消息，最终都由对应 Partition 的 Leader Broker 处理。
3. **副本同步**  
Leader Broker 接收写入，Follower Broker 从 Leader 拉取数据复制。
4. **参与集群管理**  
Broker 上下线会影响 Leader 选举、ISR 变化、分区迁移等。

**Broker 的原理**

一个 Kafka 集群由多个 Broker 组成：

```plain
Kafka Cluster
 ├── Broker-1
 ├── Broker-2
 └── Broker-3
```

Topic 被拆成多个 Partition，Partition 分布在不同 Broker 上：

```plain
Topic order
 ├── Partition-0 Leader -> Broker-1
 ├── Partition-1 Leader -> Broker-2
 └── Partition-2 Leader -> Broker-3
```

客户端不会固定读写某一台 Broker，而是先获取元数据，再找到目标 Partition 的 Leader：

```plain
Client -> 获取 Metadata
Client -> Partition Leader 所在 Broker
```

Broker 本地会把 Partition 存成追加日志：

```plain
log segment + index + timeindex
```

如果一个 Broker 宕机，它上面的 Leader Partition 会触发 Leader 选举，由其他 ISR 副本接管。

**需要注意**

+ Broker 是物理/进程节点，Topic/Partition 是逻辑数据结构。
+ 一个 Broker 可以存多个 Topic 的多个 Partition。
+ 一个 Partition 的 Leader 只能在一个 Broker 上。
+ Broker 越多，集群容量和并行能力越强，但运维复杂度也更高。
+ 客户端连接是否正常，常和 `listeners`、`advertised.listeners` 配置有关。
+ Broker 磁盘、网络、页缓存对 Kafka 性能影响很大。

一句话总结：

**Broker 是 Kafka 的服务节点，负责承载 Partition 数据、处理客户端读写、副本复制和故障切换，是 Kafka 集群容量、性能和可用性的基础。**

# Offset
Kafka Offset 是 **消息在 Partition 内的位置编号**。

它主要用于：

1. **定位消息**  
每条消息在某个 Partition 内都有唯一 offset。
2. **记录消费进度**  
Consumer 通过提交 offset 表示自己消费到哪里。
3. **支持断点续消**  
Consumer 重启后，可以从上次提交的 offset 继续消费。
4. **支持回溯消费**  
可以手动 seek 到某个 offset，重新消费历史消息。

**Offset 的原理**

Kafka 的消息存储在 Partition 中，Partition 是有序追加日志：

```plain
Partition-0:
offset 0 -> offset 1 -> offset 2 -> offset 3
```

Offset 只在 **单个 Partition 内有意义**：

```plain
Topic 全局没有统一 offset
不同 Partition 的 offset 互不相关
```

Consumer 拉取消息后，会提交自己的消费进度。提交的 offset 通常表示：

```plain
下一条要消费的 offset
```

例如处理完 `offset=100`，通常提交：

```plain
offset=101
```

Consumer Group 的 offset 默认存储在 Kafka 内部 Topic：

```plain
__consumer_offsets
```

**需要注意**

+ Offset 不是消息 ID，只是 Partition 内的位置。
+ Kafka 保证 Partition 内 offset 递增有序。
+ Consumer 提交 offset 不代表业务一定成功，取决于提交时机。
+ 自动提交简单，但可能造成重复消费或处理语义不清。
+ 生产中常用手动提交：业务处理成功后再提交 offset。
+ 如果 offset 过期或不存在，会按 `auto.offset.reset` 处理：

```plain
earliest
latest
none
```

**几个相关概念**

```plain
LEO：Log End Offset，日志末尾下一条 offset
HW ：High Watermark，消费者可见的已提交边界
Committed Offset：Consumer Group 已提交的消费进度
```

一句话总结：

**Offset 是 Kafka 在 Partition 内定位消息和记录消费进度的核心机制；Consumer 通过提交 offset 实现断点续消，但是否重复或丢处理，取决于业务处理和 offset 提交的顺序。**

# ISR
ISR 是 **In-Sync Replicas，同步副本集合**。

它表示：**当前和 Leader 保持同步、具备成为新 Leader 资格的副本集合**。

主要作用：

1. **保证数据可靠性**  
Producer 使用 `acks=all` 时，Leader 需要等待 ISR 中足够副本确认。
2. **控制 Leader 选举范围**  
Leader 挂掉后，Kafka 通常只从 ISR 中选新 Leader，避免选到落后副本导致数据丢失。
3. **推进 HW**  
Kafka 根据 ISR 中副本的复制进度推进 High Watermark，消费者只能读取 HW 之前的数据。

**ISR 的原理**

每个 Partition 都维护自己的 ISR：

```plain
Partition-0
Leader: broker-1
ISR: [broker-1, broker-2, broker-3]
```

Follower 会不断向 Leader 拉取消息。如果某个 Follower 长时间没追上 Leader，就会被移出 ISR。

关键配置：

```plain
replica.lag.time.max.ms
```

如果 Follower 在这个时间内没有保持有效同步，就会被踢出 ISR。

例如：

```plain
Leader LEO = 100
Follower-2 LEO = 100
Follower-3 LEO = 70
```

如果 Follower-3 落后太久：

```plain
ISR: [broker-1, broker-2]
```

之后 HW 推进和 `acks=all` 确认就不再等待 broker-3。当 broker-3 后续追上 Leader 后，可以重新加入 ISR。



**和 acks/min.insync.replicas 的关系**

常见可靠配置：

```plain
replication.factor=3
min.insync.replicas=2
acks=all
```

含义是：

```plain
3 个副本
至少 2 个 ISR 副本可用
Producer 写入才算成功
```

如果 ISR 只剩 1 个，`acks=all` 写入会失败，避免单副本写入带来的数据风险。

**需要注意**

+ ISR 不是所有副本，所有副本叫 AR。
+ ISR 是动态变化的，Follower 落后会被踢出，追上后会加入。
+ ISR 中的副本不一定和 Leader 完全一样新，但必须满足同步要求。
+ Leader 通常必须在 ISR 中。
+ `unclean.leader.election.enable=true` 时，非 ISR 副本也可能当 Leader，但可能导致数据丢失。

一句话总结：

**ISR 是 Kafka 判断副本是否“足够同步”的核心机制，它决定了写入确认、HW 推进和 Leader 故障切换的安全边界。**

# HW
Kafka 中 **HW** 是 **High Watermark，高水位线**。

它表示：**一个分区中已经被所有 ISR 副本确认复制到的位置**。消费者只能读取到 HW 之前的消息，也就是 Kafka 认为“已经提交、不会丢失”的消息。

简单说：

```plain
Log End Offset, LEO: 某个副本本地日志的末尾位置
High Watermark, HW: ISR 副本都已经复制到的最大安全位置
```

例如一个分区有 3 个副本：

```plain
Leader LEO = 10
Follower1 LEO = 10
Follower2 LEO = 8
```

那么 HW 最多只能推进到 `8`，因为 offset `8` 之后的消息还没有被所有 ISR 副本复制到。

**HW 解决的问题**

主要解决两个问题：

1. **防止消费者读到可能丢失的消息**

如果消费者能直接读取 Leader 的最新 LEO，那么它可能读到一条还没复制到 Follower 的消息。此时 Leader 宕机，新 Leader 可能没有这条消息，这条消息就会被截断，消费者之前读到的数据就“消失”了。HW 限制消费者只能读已经被 ISR 副本确认的消息，避免这种不一致。

2. **Leader 切换时保证日志一致性**

Kafka 发生 Leader 选举后，新 Leader 会基于 HW 进行日志截断。超过 HW 的数据如果没有被提交，可能会被删除，以保证各副本日志一致。



所以 HW 本质上是 Kafka 的 **提交边界**：

```plain
小于 HW 的消息：已提交，对消费者可见
大于等于 HW 的消息：未完全确认，对消费者不可见，可能被截断
```

一句话总结：  
**	HW 是 Kafka 用来标记“哪些消息已经足够安全，可以对消费者可见”的机制，主要用于保证副本一致性和避免消费者读到未来可能丢失的数据。**

# Partition
Kafka 中的 **Partition 是 Topic 的物理分片**。一个 Topic 可以拆成多个 Partition。

它主要解决三个问题：

1. **提升吞吐**  
多个 Partition 可以分布在不同 Broker 上，Producer、Consumer 可以并行读写。
2. **支持扩展**  
Consumer Group 中，一个 Partition 同一时刻只能被同组内一个 Consumer 消费；Partition 越多，消费并行度上限越高。
3. **保证局部顺序**  
Kafka 不保证整个 Topic 全局有序，只保证 **单个 Partition 内消息按 offset 有序**。相同 key 通常会被写入同一个 Partition，从而保证 key 级顺序。

**Partition 的原理**

Producer 写消息时，会根据规则选择 Partition：

```plain
有 key：通常按 key hash 选择 partition
无 key：按默认分区策略分散写入
指定 partition：直接写入指定 partition
```

每个 Partition 本质是一段有序追加日志：

```plain
Partition-0:
offset 0 -> offset 1 -> offset 2 -> offset 3
```

每条消息在 Partition 内都有一个递增的 `offset`，Consumer 通过保存 offset 来记录消费进度。

为了高可用，每个 Partition 可以有多个副本：

```plain
Partition-0:
Leader Replica   处理读写
Follower Replica 从 Leader 同步
```

正常情况下，客户端只和 Leader 副本交互；Follower 负责复制。当 Leader 挂掉时，Kafka 会从 ISR 中选新的 Leader。

**需要注意**

+ Partition 数决定消费并行度上限。
+ Partition 内有序，Topic 整体不保证全局有序。
+ Partition 数可以增加，但增加后 key 到 partition 的映射可能变化，影响顺序语义。
+ Partition 太多会增加 Broker、Controller、文件句柄、内存和故障恢复成本。
+ 一个 Consumer Group 内，同一个 Partition 不能同时被多个 Consumer 消费。

一句话总结：

**Partition 是 Kafka 实现高吞吐、水平扩展和局部有序的核心机制；它把 Topic 拆成多个有序日志分片，每个分片可独立读写、复制和故障切换。**

# Replica
Kafka 副本是 **Partition 的多份拷贝**，主要解决：

1. **高可用**  
Leader 副本挂了，可以从其他同步副本中选新 Leader。
2. **防止数据丢失**  
消息不只存在一台 Broker 上，降低单机故障导致数据丢失的风险。
3. **故障恢复**  
Broker 重启后，副本可以继续从 Leader 拉取缺失数据，重新追上进度。

**副本的原理**

每个 Partition 有多个副本：

```plain
Partition-0:
Leader Replica    处理读写
Follower Replica  从 Leader 拉取日志
Follower Replica  从 Leader 拉取日志
```

Kafka 默认客户端读写都走 **Leader 副本**。Follower 不直接处理普通客户端请求，而是像 Consumer 一样向 Leader 发送 fetch 请求，同步日志。

Kafka 用 **ISR** 管理同步副本：

```plain
ISR = In-Sync Replicas，同步副本集合
```

只有跟得上 Leader 的副本才在 ISR 中。Leader 根据 ISR 中副本的复制进度推进 **HW**

```plain
HW = High Watermark，高水位线
```

消费者只能读 HW 之前的数据，因为这些数据被认为已经提交、相对安全。如果 Leader 宕机，Kafka 通常从 ISR 中选新的 Leader，保证新 Leader 拥有已提交数据。

**需要注意**

+ 副本数常见配置是 `replication.factor=3`。
+ 副本不是越多越好，副本越多，磁盘和网络复制成本越高。
+ `acks=all` 配合 `min.insync.replicas` 才能真正提升写入可靠性。
+ 非 ISR 副本如果被选为 Leader，可能导致数据丢失，所以生产通常关闭：

```plain
unclean.leader.election.enable=false
```

+ Follower 同步是主动拉取，不是 Leader 推送。

一句话总结：

**Kafka 副本机制通过 Leader 处理读写、Follower 复制日志、ISR 判断同步状态，在 Broker 故障时保证分区仍可用，并尽量避免已提交数据丢失。**

# Rebalance
Kafka 中的重平衡是 **Consumer Group 内部分区归属重新分配** 的过程。

它主要解决：

1. **消费者扩缩容**  
新 Consumer 加入后，把部分 Partition 分给它，提高并行消费能力。
2. **消费者故障转移**  
某个 Consumer 下线后，它负责的 Partition 会转移给其他 Consumer。
3. **Topic/Partition 变化**  
Topic 增加分区后，Consumer Group 需要重新分配新 Partition。

**重平衡的原理**

同一个 Consumer Group 里：

```plain
一个 Partition 同一时刻只能分配给一个 Consumer
一个 Consumer 可以消费多个 Partition
```

Group Coordinator 负责管理 Consumer Group。Consumer 通过心跳维持成员身份。

触发重平衡的常见情况：

```plain
Consumer 加入 group
Consumer 离开 group
Consumer 心跳超时
订阅的 topic 分区数变化
Consumer 长时间不 poll
```

重平衡大致流程：

```plain
1. Consumer 加入/离开或状态变化
2. Group Coordinator 触发 rebalance
3. 选出一个 Consumer 作为 Group Leader
4. Group Leader 根据分配策略计算 Partition 分配方案
5. Coordinator 把分配结果下发给各 Consumer
6. Consumer 从新分配的 Partition 继续消费
```

常见分配策略：

```plain
RangeAssignor
RoundRobinAssignor
StickyAssignor
CooperativeStickyAssignor
```

现代生产环境更推荐 `CooperativeStickyAssignor`，因为它是增量重平衡，可以减少整体停顿。

**需要注意**

+ 重平衡期间，相关 Consumer 可能暂停消费。
+ 频繁重平衡会导致消费抖动、延迟升高、重复消费增加。
+ `max.poll.interval.ms` 太小，业务处理慢，会导致 Consumer 被踢出 group。
+ `session.timeout.ms` 和 `heartbeat.interval.ms` 控制心跳失效判断。
+ Consumer 数量超过 Partition 数时，多出来的 Consumer 会空闲。
+ 手动提交 offset 时，要在分区被撤销前处理好提交，避免重复消费或丢处理进度。

**常见优化**

```plain
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
max.poll.interval.ms=适当调大
max.poll.records=适当调小
session.timeout.ms=适当设置
heartbeat.interval.ms=session.timeout.ms 的 1/3 左右
```

如果业务处理很慢，可以减少每次拉取数量：

```plain
max.poll.records=100
```

或者把拉取和处理解耦，避免长时间不调用 `poll()`。

一句话总结：

**重平衡是 Kafka Consumer Group 为了适应消费者数量、分区数量和故障变化而重新分配 Partition 的机制；它保证负载动态转移，但频繁发生会带来消费暂停、延迟上升和重复消费风险。**

# Topic
Kafka 中的 **Topic 是消息的逻辑分类**，类似消息队列里的“主题”或“类别”。

它主要用于：

1. **业务隔离**  
不同业务数据放到不同 Topic，比如：

```plain
order-events
payment-events
user-log
```

2. **发布订阅**  
Producer 向 Topic 写消息，Consumer 订阅 Topic 读消息。
3. **数据管理**  
Topic 可以配置保留时间、分区数、副本数、清理策略等。

**Topic 的原理**

Topic 本身只是逻辑概念，真正存储数据的是它下面的 **Partition**。

```plain
Topic
 ├── Partition-0
 ├── Partition-1
 └── Partition-2
```

Producer 写入 Topic 时，Kafka 会把消息分配到某个 Partition：

```plain
有 key：通常按 key hash 分区
无 key：按策略均衡分区
```

Consumer 订阅 Topic 后，本质上是从 Topic 的多个 Partition 中拉取消息。

每个 Partition 是一段有序日志：

```plain
Partition-0: offset 0, 1, 2, 3...
Partition-1: offset 0, 1, 2, 3...
```

所以 Kafka 保证的是 **Partition 内有序**，不是 Topic 全局有序。

**需要注意**

+ Topic 是逻辑分类，Partition 才是物理存储和并行单位。
+ 一个 Topic 可以有多个 Consumer Group 独立消费。
+ Topic 的分区数影响吞吐和消费并行度。
+ Topic 的副本数影响高可用和数据可靠性。
+ Topic 可以配置 `delete` 或 `compact` 清理策略。
+ 生产环境通常关闭自动创建 Topic，避免误创建：

```plain
auto.create.topics.enable=false
```

一句话总结：

**Topic 是 Kafka 对消息流的逻辑命名和管理单元；它通过多个 Partition 实现并行读写，通过副本实现高可用，通过消费组实现一份数据被多类业务独立消费。**

# KRaft
KRaft 是 Kafka 的自管理元数据机制，用来替代 ZooKeeper。

它主要解决：

1. **去掉 ZooKeeper 依赖**  
Kafka 不再需要单独维护 ZooKeeper 集群，部署和运维更简单。
2. **统一元数据管理**  
Topic、Partition、Broker、ACL、配置等元数据由 Kafka 自己管理。
3. **提升扩展性和恢复效率**  
元数据变更通过 Kafka 内部的 Raft 日志复制，Controller 可以更快恢复状态。

**KRaft 的原理**

KRaft 基于 **Raft 共识协议**。

Kafka 集群中会有一组 **Controller 节点** 组成 quorum：

```plain
Controller Quorum
 ├── Controller 1
 ├── Controller 2
 └── Controller 3
```

其中一个 Controller 是 active controller，负责处理元数据变更：

```plain
创建 Topic
删除 Topic
分区 Leader 选举
Broker 上下线
配置变更
ACL 变更
```

这些元数据变更会写入 Kafka 内部的元数据日志：

```plain
__cluster_metadata
```

Controller quorum 通过 Raft 复制这份日志，只有多数派确认后，元数据变更才算提交。Broker 会从 Controller 拉取或接收最新元数据，然后据此处理客户端请求。

****

**和 ZooKeeper 模式的区别**

ZooKeeper 模式：

```plain
Kafka Broker + ZooKeeper
元数据存在 ZooKeeper
Controller 通过 ZooKeeper 协调
```

KRaft 模式：

```plain
Kafka Broker + Kafka Controller Quorum
元数据存在 Kafka 内部 Raft 日志
Controller 由 Kafka 自己选举和管理
```

**需要注意**

+ KRaft 是新 Kafka 架构的主流方向。
+ 生产环境建议 Controller 节点使用奇数个，比如 3 或 5。
+ Controller quorum 需要多数派可用，否则元数据变更不可用。
+ Broker 和 Controller 可以混合部署，也可以分离部署。
+ 较大生产集群通常建议 Broker 和 Controller 分离。
+ KRaft 模式下常见关键配置：

```plain
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@host1:9093,2@host2:9093,3@host3:9093
controller.listener.names=CONTROLLER
```

一句话总结：

**KRaft 是 Kafka 用 Raft 协议实现的内置元数据管理机制，用来替代 ZooKeeper，让 Kafka 自己完成 Controller 选举、元数据复制、分区管理和故障恢复。**

# Metadata
Kafka 元数据就是集群的“路由表 + 状态表”。

它告诉客户端和 Broker：

1. **有哪些 Topic 和 Partition**
2. **每个 Partition 的 Leader 是谁**
3. **每个 Partition 有哪些副本、ISR 是哪些**
4. **Broker 列表和地址是什么**
5. **Topic/集群配置是什么**
6. **ACL、配额等管理信息是什么**

客户端必须先拿到元数据，才知道消息该发到哪个 Broker、从哪个 Broker 拉取。

**元数据的原理**

Producer / Consumer 启动时会连接 `bootstrap.servers` 中的任意 Broker，请求集群元数据：

```plain
Client -> 任意 Broker -> 返回 Metadata
```

拿到元数据后，客户端就知道：

```plain
Topic A / Partition 0 的 Leader 是 broker-1
Topic A / Partition 1 的 Leader 是 broker-2
```

然后客户端直接连接对应 Leader 读写：

```plain
Producer -> Partition Leader
Consumer <- Partition Leader
```

如果发生变化，比如：

```plain
Leader 切换
Broker 上下线
Topic 新增/删除
Partition 增加
```

客户端会刷新元数据，重新找到正确的 Leader。

在服务端，元数据由 **Controller** 管理：

```plain
ZooKeeper 模式：元数据主要存 ZooKeeper，Controller 负责协调
KRaft 模式：元数据存 Kafka 内部 Raft 日志，由 Controller Quorum 管理
```

现代 Kafka 的 KRaft 模式中，元数据变更会写入内部元数据日志：

```plain
__cluster_metadata
```

Controller quorum 通过 Raft 复制并提交这些变更，Broker 再同步最新元数据。

**需要注意**

+ `bootstrap.servers` 只是入口，不是完整路由配置。
+ 真正决定客户端连接地址的是 `advertised.listeners`。
+ 客户端会缓存元数据，不是每次请求都查。
+ Leader 变化后，客户端可能短暂收到 `NOT_LEADER_OR_FOLLOWER`，然后刷新元数据重试。
+ 元数据异常常见表现是：客户端连得上 bootstrap，但无法生产/消费。
+ KRaft 模式下，Controller quorum 不可用会影响元数据变更，但已知 Leader 的普通读写不一定立刻中断。

一句话总结：

**Kafka 元数据负责描述集群拓扑、Topic/Partition 分布、Leader 副本和配置状态，是客户端路由、Broker 协作、Leader 选举和集群管理的基础。**

# ACK
Kafka 中的 ACK 是 **Producer 写消息时要求 Broker 返回的确认级别**。

它主要决定：

1. **消息写入成功的判定标准**
2. **可靠性和吞吐之间的取舍**
3. **Producer 是否需要等待副本同步完成**

**ACK 的原理**

Producer 发送消息到 Partition Leader 后，Broker 根据 `acks` 配置决定什么时候返回成功。

常见配置：

```plain
acks=0
acks=1
acks=all
```

含义：

```plain
acks=0
Producer 不等 Broker 响应。
吞吐最高，可靠性最低，消息可能还没到 Broker 就丢。

acks=1
Leader 写入本地日志后就返回成功。
性能较好，但如果 Leader 宕机且 Follower 还没同步，消息可能丢。

acks=all
Leader 等待 ISR 中足够副本确认后再返回成功。
可靠性最高，但延迟更高。
```

`acks=all` 通常还要配合：

```plain
min.insync.replicas=2
replication.factor=3
```

含义是：

```plain
3 个副本
至少 2 个 ISR 副本确认
Producer 才认为写入成功
```

如果 ISR 数量不足，Producer 会收到失败，而不是冒险写入。

**需要注意**

+ `acks=all` 不是等所有副本，而是等 ISR 中满足条件的副本。
+ `acks=1` 只保证 Leader 写入，不保证 Follower 已复制。
+ `acks=0` 无法知道消息是否成功，失败也不容易重试。
+ 强可靠场景建议：

```plain
acks=all
enable.idempotence=true
retries=2147483647
```

+ 可靠性还依赖服务端：

```plain
min.insync.replicas=2
unclean.leader.election.enable=false
```

一句话总结：

**ACK 是 Kafka Producer 控制写入确认语义的核心配置；**`**acks=0**`** 追求吞吐，**`**acks=1**`** 折中，**`**acks=all**`** 配合 ISR 和 **`**min.insync.replicas**`** 提供更强可靠性。**

# Segment
Kafka Segment 是 **Partition 日志文件的分段存储单元**。

它主要解决：

1. **避免单个日志文件过大**  
Partition 是无限追加日志，如果只用一个文件会越来越大，难以管理。
2. **提升清理效率**  
Kafka 可以按 Segment 删除过期数据，而不是逐条删除消息。
3. **加快查找**  
每个 Segment 配套索引文件，可以快速根据 offset 或时间定位消息。

**Segment 的原理**

一个 Partition 底层由多个 Segment 组成：

```plain
Partition-0
 ├── 00000000000000000000.log
 ├── 00000000000000000000.index
 ├── 00000000000000000000.timeindex
 ├── 00000000000000001000.log
 ├── 00000000000000001000.index
 └── 00000000000000001000.timeindex
```

每个 Segment 文件名是该 Segment 的 **base offset**。

例如：

```plain
00000000000000001000.log
```

表示这个 Segment 从 offset `1000` 附近开始存消息。

Kafka 写入时，只追加到当前 active Segment。达到一定大小或时间后，会滚动生成新的 Segment：

```plain
segment.bytes
segment.ms
```

查询消息时：

```plain
1. 根据 offset 找到对应 Segment
2. 通过 .index 定位物理文件位置
3. 从 .log 文件读取消息
```

**相关文件**

```plain
.log        存储真实消息数据
.index      offset -> 文件物理位置索引
.timeindex  timestamp -> offset 索引
```

**需要注意**

+ Segment 是 Partition 内部存储结构，不是 Kafka 对外概念。
+ Kafka 删除过期数据通常以 Segment 为单位。
+ active Segment 一般不会被删除，只有滚动后的旧 Segment 才会按策略清理。
+ `segment.bytes` 越小，清理更及时，但文件更多。
+ `segment.bytes` 越大，文件更少，但过期数据释放可能不够及时。
+ Log Compaction 也主要围绕 Segment 做清理和重写。

一句话总结：

**Segment 是 Kafka 将 Partition 日志切分成多个文件的机制，用于支撑顺序追加写、快速索引查询、按时间/大小滚动以及高效日志清理。**

# Index文件
Kafka 的 index 文件是 **Segment 的稀疏索引文件**，用于快速定位消息在 `.log` 文件中的物理位置。

它主要解决：

1. **避免全量扫描 log 文件**  
根据 offset 快速找到消息大概位置。
2. **提升消费和查找效率**  
Consumer 从某个 offset 开始消费时，可以快速定位。
3. **支持时间查询**  
根据时间戳查找对应 offset。

**Index 文件类型**

一个 Segment 通常有几类相关文件：

```plain
.log        真实消息数据
.index      offset 索引
.timeindex  时间索引
```

核心是：

```plain
.index:     relative offset -> physical position
.timeindex: timestamp -> relative offset
```

**Index 文件原理**

Kafka 的 `.index` 不是每条消息都建索引，而是 **稀疏索引**。

例如 Segment base offset 是 `1000`：

```plain
00000000000000001000.log
00000000000000001000.index
```

`.index` 中记录的是相对 offset：

```plain
relative offset = message offset - base offset
```

示例：

```plain
offset 1000 -> relative offset 0
offset 1050 -> relative offset 50
offset 1100 -> relative offset 100
```

索引项大致保存：

```plain
relative offset -> log 文件中的物理 position
```

查找 offset `1080` 时：

```plain
1. 根据 offset 找到对应 Segment
2. 在 .index 中二分查找 <= 1080 的最大索引项
3. 拿到对应 physical position
4. 从 .log 文件该位置顺序扫描少量消息
5. 找到目标 offset
```

因为是稀疏索引，所以最终还需要扫一小段 log，但不用从 Segment 开头扫。

**timeindex 原理**

`.timeindex` 用于按时间查找消息。

它保存类似：

```plain
timestamp -> relative offset
```

当 Consumer 使用按时间定位，比如 `offsetsForTimes()` 时，Kafka 会通过 `.timeindex` 找到接近该时间的 offset，再结合 `.index` 和 `.log` 定位消息。

**需要注意**

+ index 文件是 Segment 的辅助文件。
+ `.index` 加速 offset 查找。
+ `.timeindex` 加速 timestamp 查找。
+ Kafka index 是稀疏索引，不是每条消息一条索引。
+ 稀疏索引节省空间，但查找后还需顺序扫描少量消息。
+ 索引损坏时 Kafka 可以基于 `.log` 文件重建索引。

一句话总结：

**Kafka index 文件是 Segment 的稀疏索引，通过 offset 或 timestamp 快速定位 **`**.log**`** 文件中的物理位置，在节省索引空间的同时提升消息查找和消费定位效率。**

