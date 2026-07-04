# EventLoop
EventLoop 是Netty的事件循环线程模型，它负责：

    - 监听 I/O 事件，比如连接、读、写
    - 执行Channel相关任务
    - 驱动 pipeline 里的 handler 回调
    - 保证同一个Channel的事件通常在同一个线程串行执行



EventLoop解决以下问题：

    - 高效管理大量连接
    - 避免一个连接一个线程的高成本
    - 减少并发锁竞争
    - 让网络事件处理更有序、可控



一句话：EventLoop 就是 Netty 用来轮询 I/O 事件并执行 Channel 任务的核心线程。

## EventLoop实现
EventLoop是通用抽象，负责：

    - 执行任务
    - 注册/管理Channel
    - 处理 I/O 事件



具体底层怎么等 I/O 事件，由传输实现决定：

    - NIO：基于 Java Selector
    - epoll：基于 Linux epoll
    - kqueue：基于 BSD/macOS kqueue
    - io_uring：基于 Linux io_uring



EventLoop 通用调度模型，Transport / IoHandler 决定底层 I/O 轮询实现。



## 网络事件\socket是什么
网络事件就是 socket 状态变化或 I/O 就绪通知。常见包括：

    - 有新连接进来：accept
    - 连接建立完成：connect
    - 有数据可读：read
    - 可以继续写数据：write
    - 连接关闭或异常：close / error

操作系统告诉 Netty“这个连接现在可以做某件事了”，这就是网络事件。



那socket是什么呢？

Socket是应用程序和网络之间的通信端点。可以理解成：程序用来收发网络数据的“连接句柄”。比如 TCP 通信里：

```plain
客户端 Socket  <====网络====>  服务端 Socket
```

程序通过 socket：

    - 连接远程服务
    - 发送数据
    - 接收数据
    - 关闭连接



socket 是程序进行网络通信的入口。

# Pipeline
Pipeline 是 Netty 里处理请求/响应的流水线，里面的 Handler 分步骤完成拆包、编解码、序列化、业务分发。Netty Client和Netty Server的pipeline都是用的DefaultChannelPipeline，但是怎么区分是Netty Client的pipeline还是Netty Server的pipeline呢？

靠 Channel 类型和创建来源 区分，不靠 DefaultChannelPipeline 类型区分。DefaultChannelPipeline 本身是通用实现，Client 和 Server 都用它。但它绑定的 Channel 不同：

```plain
客户端连接：
NioSocketChannel
  -> new DefaultChannelPipeline(this)
  -> Client Pipeline

服务端监听端口：
NioServerSocketChannel
  -> new DefaultChannelPipeline(this)
  -> Server Boss Pipeline

服务端接收到的客户端连接：
NioSocketChannel
  -> new DefaultChannelPipeline(this)
  -> Server Child Pipeline
```

也就是说：

```plain
pipeline.channel()
```

	Pipeline 类型都一样，属于谁取决于它绑定的是哪个 Channel，以及这个 Channel 是由客户端 Bootstrap创建，还是服务端ServerBootstrap创建。



一个pipeline既可以处理入站请求，也可以处理出站请求，只不过方向不一样。

```plain
入站 Inbound：读数据、解码、业务处理
出站 Outbound：写数据、编码、发送

入站：Head -> Tail
出站：Tail -> Head
```



```plain
一个 ChannelPipeline
┌──────────────────────────────────────────────┐
│                                              │
│  Head ── H1 ── H2 ── H3 ── H4 ── Tail        │
│                                              │
└──────────────────────────────────────────────┘


入站 Inbound：从 Head 到 Tail
Socket 收到数据
      ↓
Head ──> H1 ──> H2 ──> H3 ──> H4 ──> Tail
        解码    反序列化  业务处理


出站 Outbound：从 Tail 到 Head
业务调用 writeAndFlush()
      ↓
Tail ──> H4 ──> H3 ──> H2 ──> H1 ──> Head
              序列化   编码    写 Socket
```

## 出站和入站
Netty出站——ChannelInboundHandler，是指当前 Channel 将数据写出到网络。比如 RPC Client 发送请求，或 RPC Server 返回响应，都会经过出站流程：Java 对象序列化、协议编码/封装，转成 ByteBuf，再通过 Socket 发送出去。

Netty入站——ChannelOutboundHandler，是指当前 Channel 从网络读取数据。比如 RPC Server 接收请求，或 RPC Client 接收响应，都会经过入站流程：读取 ByteBuf，按协议处理粘包半包，解码协议，再反序列化成 Java 对象。

## 为什么入站：HEAD->TAIL，出站：TAIL->HEAD？
核心逻辑就是：**<font style="color:#DF2A3F;">Head 更靠近 Socket / 底层 I/O，Tail 更靠近业务处理端。</font>**

因为 Netty 把 `Head` 设计成**靠近底层 Socket 的入口**，`Tail` 设计成**靠近业务处理的末端**。

所以网络数据进来时：

```plain
Socket 读到数据
  ↓
HeadContext
  ↓
InboundHandler1
  ↓
InboundHandler2
  ↓
TailContext
```

也就是：

```plain
Head -> Tail
```

反过来，业务要写数据出去时，是从业务侧往 Socket 走：

```plain
Tail -> Head
```

最后到 `HeadContext`，由它调用底层 unsafe 把数据写到 Socket。

# Channel
Channel是Netty对网络连接/通信端点的抽象，也就是Netty里对 socket 连接或监听端口的封装。它代表一个可进行 I/O 操作的对象，比如：

+ 服务端监听端口：ServerSocketChannel
+ 客户端连接：SocketChannel
+ UDP 通信：DatagramChannel



它主要负责：

+ 读写数据
+ 绑定/连接/关闭
+ 持有关联的EventLoop
+ 持有ChannelPipeline

# EventLoopGroup、EventLoop、Channel关系
在Netty中，一般这样设置：

```plain
EventLoopGroup bossGroup = new NioEventLoopGroup(1);
EventLoopGroup workerGroup = new NioEventLoopGroup();
```

bossGroup：负责监听端口、accept 新连接，通常 1 个线程就够。workerGroup：负责已连接客户端的读写 I/O，默认线程数通常是 CPU核数 * 2。



EventLoop、线程、channel之间的关系：

    - 1 个 EventLoop 通常绑定 1 个线程
    - 1 个 EventLoopGroup 包含多个 EventLoop
    - 1 个 Channel 注册到 1 个 EventLoop
    - 1个EventLoop会有多个channel，一个EventLoop会轮训多个channel的事件



在Netty中，一个EventLoop线程不是被某个Channel独占的。它会轮询处理多个Channel的事件：

```plain
EventLoop-1
  -> Channel A read
  -> Channel B read
  -> Channel C write
  -> 定时任务
  -> 下一轮 select
```



总结：BossGroup 里通常配置一个或少数几个 EventLoop。Netty 服务端调用 bind() 时，会创建并将 XxxServerSocketChannel 注册到 BossGroup 的某个 EventLoop 上。这个 EventLoop 主要负责轮询 XxxServerSocketChannel 上的客户端新连接事件，也就是 TCP accept 事件。当 accept 到新连接后，Netty 会创建对应的 XxxSocketChannel，并将它注册到 WorkerGroup 按策略选出的某个 EventLoop 上。后续该客户端连接的 read/write 事件，都由这个 XxxSocketChannel 绑定的 Worker EventLoop 负责轮询和处理。

不同的EventLoop底层使用的轮询机制不一样，不管底层是用的：Selector / epoll / kqueue / io_uring / 阻塞 I/O，都需要轮询事件。

    - NioEventLoop -> Selector
    - EpollEventLoop   -> Linux epoll
    - KQueueEventLoop  -> BSD/macOS kqueue
    - IoUringEventLoop -> Linux io_uring
    - OioEventLoop     -> 阻塞 I/O 模型



