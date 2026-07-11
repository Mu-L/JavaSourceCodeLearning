<h1 align="center">Java Source Code Learning</h1>

<p align="center">
  一份面向 Java 后端工程师的源码阅读地图：从 JDK / JUC 到 Spring、Netty、Kafka、RocketMQ，按核心链路拆解框架设计与底层实现。
</p>

<p align="center">
  <a href="https://www.oracle.com/cn/java/technologies/javase/javase-jdk8-downloads.html">
    <img src="https://img.shields.io/badge/JDK-1.8.0__77-brightgreen" alt="JDK">
  </a>
  <a href="https://spring.io/">
    <img src="https://img.shields.io/badge/Spring-5.2.1.RELEASE-green" alt="Spring">
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/SpringBoot-2.2.1.RELEASE-yellowgreen" alt="SpringBoot">
  </a>
  <a href="https://spring.io/projects/spring-security">
    <img src="https://img.shields.io/badge/SpringSecurity-5.1.0.RELEASE-orange" alt="SpringSecurity">
  </a>
  <a href="https://spring.io/projects/spring-security-oauth">
    <img src="https://img.shields.io/badge/Spring--Security--OAuth2-2.3.5.RELEASE-red" alt="Spring-Security-OAuth2">
  </a>
  <a href="https://netty.io/">
    <img src="https://img.shields.io/badge/Netty-4.1.43.Final-blue" alt="Netty">
  </a>
  <a href="https://rocketmq.apache.org/">
    <img src="https://img.shields.io/badge/RocketMQ-4.9.0-green" alt="RocketMQ">
  </a>
  <a href="https://kafka.apache.org/">
    <img src="https://img.shields.io/badge/Kafka-4.3-yellowgreen" alt="Kafka">
  </a>
</p>

## 项目亮点

| 你能看到什么 | 重点能力                                       |
| --- |--------------------------------------------|
| JDK / JUC 源码 | 集合、并发、线程池、内存模型、CompletableFuture           |
| Spring / SpringBoot 源码 | IOC 容器、事件机制、启动流程、自动装配、扩展点                  |
| SpringAOP / Security / OAuth2 | 代理机制、过滤器链、安全认证授权主流程                        |
| Netty 源码 | Reactor 模型、EventLoop、ChannelPipeline、网络通信链路 |
| Kafka / RocketMQ 源码 | Broker、Producer、Consumer、Rebalance、消息存储与复制 |

## 学习路线

```text
JDK / JUC 基础源码
        ↓
Spring 容器与扩展点
        ↓
SpringBoot 启动与自动装配
        ↓
Netty 网络通信模型
        ↓
Kafka / RocketMQ 消息系统源码
```

## 为什么要读源码？

源码学习不是为了记住每一行实现，而是为了把框架背后的设计选择看清楚。

当线上问题出现时，读过核心链路的人通常能更快定位边界、判断根因、验证假设；当自己做系统设计时，也更容易借鉴成熟框架在模块拆分、扩展点设计、并发控制、性能优化上的经验。

这个项目会围绕主流 Java 后端框架的核心路径持续整理源码分析、学习笔记和关键图解。

## 内容目录

<details open>
<summary><strong>Kafka 源码分析</strong></summary>

- Kafka 版本：4.3
- [Kafka 核心概念扫盲](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/Kafka%E6%A0%B8%E5%BF%83%E6%A6%82%E5%BF%B5%E6%89%AB%E7%9B%B2.md)
- [Kafka Broker 核心源码分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/kafka%20broker%E6%A0%B8%E5%BF%83%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md)
- [Kafka Broker 源码分析：生产者篇](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/kafka%20broker%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90%E2%80%94%E2%80%94%E7%94%9F%E4%BA%A7%E8%80%85%E7%AF%87.md)
- [Kafka 消费者核心源码分析（一）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/kafka%E6%B6%88%E8%B4%B9%E8%80%85%E6%A0%B8%E5%BF%83%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90%EF%BC%88%E4%B8%80%EF%BC%89.md)
- [Kafka Rebalance 核心逻辑分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/kafka%20rebalance%E6%A0%B8%E5%BF%83%E9%80%BB%E8%BE%91%E5%88%86%E6%9E%90.md)
- [Kafka ISR 原理](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/kafka/Kafka%20ISR%20%E5%BA%95%E5%B1%82%E5%8E%9F%E7%90%86.md)

<strong>Kafka架构图</strong>
![kafka_architecture](https://github.com/coderbruis/JavaSourceCodeLearning/releases/download/images-v1/kafka_architecture.png)

<strong>Kafka Rebalance流程图</strong>
![Kafka EAGER Rebalance](https://github.com/coderbruis/JavaSourceCodeLearning/releases/download/images-v1/kafka_EAGER_rebalance.png)

![Kafka Cooperative Rebalance](https://github.com/coderbruis/JavaSourceCodeLearning/releases/download/images-v1/kafka_COOPERATIVE_rebalance.png)

<strong>Kafka ISR / HW / LEO关系图</strong>
![Kafka_HW_ISR_LEO](https://github.com/coderbruis/JavaSourceCodeLearning/releases/download/images-v1/kafka_HW_ISR_LEO.png)


</details>

<details>
<summary><strong>JDK / JUC 源码学习</strong></summary>

- JDK 版本：1.8.0_77
- [深入学习 String 源码与底层（一）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0String%E6%BA%90%E7%A0%81%E4%B8%8E%E5%BA%95%E5%B1%82%EF%BC%88%E4%B8%80%EF%BC%89.md)
- [深入学习 String 源码与底层（二）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0String%E6%BA%90%E7%A0%81%E4%B8%8E%E5%BA%95%E5%B1%82%EF%BC%88%E4%BA%8C%EF%BC%89.md)
- [深入解读 CompletableFuture 源码与原理](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E8%A7%A3%E8%AF%BBCompletableFuture%E6%BA%90%E7%A0%81%E4%B8%8E%E5%8E%9F%E7%90%86.md)
- [深入分析 ThreadLocal](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%88%86%E6%9E%90ThreadLocal.md)
- [深入学习 Java volatile 关键字](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0Java%20volatile%E5%85%B3%E9%94%AE%E5%AD%97.md)
- [深入学习 Thread 底层原理](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0Thread%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81.md)
- [深入学习HashMap 底层源码与原理](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/HashMap%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md)
- [开源项目里那些看不懂的位运算分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/%E5%BC%80%E6%BA%90%E9%A1%B9%E7%9B%AE%E9%87%8C%E9%82%A3%E4%BA%9B%E7%9C%8B%E4%B8%8D%E6%87%82%E7%9A%84%E4%BD%8D%E8%BF%90%E7%AE%97%E5%88%86%E6%9E%90.md)
- [ThreadPoolExecutor 源码分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/JDK/%E6%B7%B1%E5%85%A5%E8%A7%A3%E6%9E%90ThreadPoolExecutor%E5%BA%95%E5%B1%82%E5%8E%9F%E7%90%86.md)

<strong>HashMap原理图</strong>
![HashMap原理图](https://github.com/coderbruis/JavaSourceCodeLearning/releases/download/images-v1/HashMap.png)

<strong>AQS架构图</strong>
![AQS架构图](https://github.com/coderbruis/JavaSourceCodeLearning/releases/download/images-v1/AQS.png)

</details>

<details>
<summary><strong>Spring 源码学习</strong></summary>

- Spring 版本：5.2.1.RELEASE
- [深入 Spring 源码系列（一）：在 IDEA 中构建 Spring 源码](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%E5%9C%A8IDEA%E4%B8%AD%E6%9E%84%E5%BB%BASpring%E6%BA%90%E7%A0%81.md)
- [深入 Spring 容器源码与时序图（上）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%E6%B7%B1%E5%85%A5Spring%E5%AE%B9%E5%99%A8%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB%E5%92%8C%E6%97%B6%E5%BA%8F%E5%9B%BE%E6%9D%A5%E5%BD%BB%E5%BA%95%E5%BC%84%E6%87%82Spring%E5%AE%B9%E5%99%A8%EF%BC%88%E4%B8%8A%EF%BC%89.md)
- [深入 Spring 容器源码与时序图（下）](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%E6%B7%B1%E5%85%A5Spring%E5%AE%B9%E5%99%A8%EF%BC%8C%E9%80%9A%E8%BF%87%E6%BA%90%E7%A0%81%E9%98%85%E8%AF%BB%E5%92%8C%E6%97%B6%E5%BA%8F%E5%9B%BE%E6%9D%A5%E5%BD%BB%E5%BA%95%E5%BC%84%E6%87%82Spring%E5%AE%B9%E5%99%A8%EF%BC%88%E4%B8%8B%EF%BC%89.md)
- [深入 Spring 源码系列（补充篇）：程序调用 Spring 源码](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E6%B7%B1%E5%85%A5Spring%E6%BA%90%E7%A0%81%E7%B3%BB%E5%88%97%EF%BC%88%E8%A1%A5%E5%85%85%E7%AF%87%EF%BC%89%E2%80%94%E2%80%94%E7%A8%8B%E5%BA%8F%E8%B0%83%E7%94%A8Spring%E6%BA%90%E7%A0%81.md)
- [从 Spring 源码中学习策略模式](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Spring/%E4%BB%8ESpring%E6%BA%90%E7%A0%81%E4%B8%AD%E5%AD%A6%E4%B9%A0%E2%80%94%E2%80%94%E7%AD%96%E7%95%A5%E6%A8%A1%E5%BC%8F.md)

</details>

<details>
<summary><strong>SpringAOP 源码学习</strong></summary>

- Spring 版本：5.2.1.RELEASE
- [深入学习 SpringAOP 源码（一）：注册 AnnotationAwareAspectJAutoProxyCreator](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringAOP/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0SpringAOP%E6%BA%90%E7%A0%81%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%E6%B3%A8%E5%86%8CAnnotationAwareAspectJAutoProxyCreator.md)
- [深入学习 SpringAOP 源码（二）：深入 AnnotationAwareAspectJAutoProxyCreator](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringAOP/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0SpringAOP%E6%BA%90%E7%A0%81%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20%E6%B7%B1%E5%85%A5AnnotationAwareAspectJAutoProxyCreator.md)
- [深入学习 SpringAOP 源码（三）：揭开 JDK 动态代理和 CGLIB 代理的神秘面纱](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringAOP/%E6%B7%B1%E5%85%A5%E5%AD%A6%E4%B9%A0SpringAOP%E6%BA%90%E7%A0%81%EF%BC%88%E4%B8%89%EF%BC%89%E2%80%94%E2%80%94%E6%8F%AD%E5%BC%80JDK%E5%8A%A8%E6%80%81%E4%BB%A3%E7%90%86%E5%92%8CCGLIB%E4%BB%A3%E7%90%86%E7%9A%84%E7%A5%9E%E7%A7%98%E9%9D%A2%E7%BA%B1.md)

</details>

<details>
<summary><strong>SpringBoot 源码学习</strong></summary>

- SpringBoot 版本：2.2.1.RELEASE
- [深入浅出 SpringBoot 源码：SpringFactoriesLoader](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94SpringFactoriesLoader.md)
- [深入浅出 SpringBoot 源码：监听器与事件机制](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94%E7%9B%91%E5%90%AC%E5%99%A8%E4%B8%8E%E4%BA%8B%E4%BB%B6%E6%9C%BA%E5%88%B6.md)
- [深入浅出 SpringBoot 源码：系统初始化器](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94%E7%B3%BB%E7%BB%9F%E5%88%9D%E5%A7%8B%E5%8C%96%E5%99%A8.md)
- [深入浅出 SpringBoot 源码：启动加载器](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringBoot/%E6%B7%B1%E5%85%A5SpringBoot%E6%BA%90%E7%A0%81%E5%AD%A6%E4%B9%A0%E4%B9%8B%E2%80%94%E2%80%94%E5%90%AF%E5%8A%A8%E5%8A%A0%E8%BD%BD%E5%99%A8.md)

</details>

<details>
<summary><strong>SpringSecurity / OAuth2 源码学习</strong></summary>

- SpringSecurity 版本：5.1.0.RELEASE
- [深入浅出 SpringSecurity 和 OAuth2（一）：初识 SpringSecurity](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%B8%80%EF%BC%89%E2%80%94%E2%80%94%20%E5%88%9D%E8%AF%86SpringSecurity.md)
- [深入浅出 SpringSecurity 和 OAuth2（二）：安全过滤器 FilterChainProxy](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%BA%8C%EF%BC%89%E2%80%94%E2%80%94%20%E5%AE%89%E5%85%A8%E8%BF%87%E6%BB%A4%E5%99%A8FilterChainProxy.md)
- [深入浅出 SpringSecurity 和 OAuth2（三）：WebSecurity 建造核心逻辑](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E4%B8%89%EF%BC%89%E2%80%94%E2%80%94%20WebSecurity%E5%BB%BA%E9%80%A0%E6%A0%B8%E5%BF%83%E9%80%BB%E8%BE%91.md)
- [深入浅出 SpringSecurity 和 OAuth2（四）：FilterChainProxy 过滤器链中的几个重要过滤器](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/SpringSecurity/%E4%BB%8E%E9%9B%B6%E5%BC%80%E5%A7%8B%E7%B3%BB%E7%BB%9F%E5%AD%A6%E4%B9%A0SpringSecurity%E5%92%8COAuth2%EF%BC%88%E5%9B%9B%EF%BC%89%E2%80%94%E2%80%94%20FilterChainProxy%E8%BF%87%E6%BB%A4%E5%99%A8%E9%93%BE%E4%B8%AD%E7%9A%84%E5%87%A0%E4%B8%AA%E9%87%8D%E8%A6%81%E7%9A%84%E8%BF%87%E6%BB%A4%E5%99%A8.md)

</details>

<details>
<summary><strong>Netty 底层源码解析</strong></summary>

- Netty 版本：4.1.43.Final
- [Netty 概念扫盲](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E6%A6%82%E5%BF%B5%E6%89%AB%E7%9B%B2.md)
- [二进制运算以及源码、反码以及补码学习](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Netty/%E4%BA%8C%E8%BF%9B%E5%88%B6.md)
- [Netty 源码包结构](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Netty/Netty%E6%BA%90%E7%A0%81%E5%8C%85%E7%BB%93%E6%9E%84.md)
- [Netty 底层源码解析：EventLoopGroup](https://github.com/coderbruis/JavaSourceLearning/blob/master/note/Netty/Netty%E4%B8%AD%E7%9A%84EventLoopGroup%E6%98%AF%E4%BB%80%E4%B9%88.md)
- [Netty 底层源码解析：初始 Netty 及其架构](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-%E5%88%9D%E5%A7%8BNetty%E5%8F%8A%E5%85%B6%E6%9E%B6%E6%9E%84.md)
- [Netty 底层源码解析：Netty 服务端启动分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-Netty%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%90%AF%E5%8A%A8%E5%88%86%E6%9E%90.md)
- [Netty 底层源码解析：NioEventLoop 原理分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-NioEventLoop%E5%8E%9F%E7%90%86%E5%88%86%E6%9E%90.md)
- [Netty 底层源码解析：ChannelPipeline 分析（上）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-ChannelPipeline%E5%88%86%E6%9E%90%EF%BC%88%E4%B8%8A%EF%BC%89.md)
- [Netty 底层源码解析：ChannelPipeline 分析（下）](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-ChannelPipeline%E5%88%86%E6%9E%90%EF%BC%88%E4%B8%8B%EF%BC%89.md)
- [Netty 底层源码解析：NioServerSocketChannel 接受数据原理分析](https://github.com/coderbruis/JavaSourceCodeLearning/blob/master/note/Netty/Netty%E5%BA%95%E5%B1%82%E6%BA%90%E7%A0%81%E8%A7%A3%E6%9E%90-NioServerSocketChannel%E6%8E%A5%E5%8F%97%E6%95%B0%E6%8D%AE%E5%8E%9F%E7%90%86%E5%88%86%E6%9E%90.md)
- Netty 底层源码解析：NioSocketChannel 接受、发送数据原理分析
- Netty 底层源码解析：FastThreadLocal 原理分析
- Netty 底层源码解析：内存分配原理分析
- Netty 底层源码解析：RocketMQ 底层使用到的 Netty

Netty 实战课相关代码位于 `Spring-Netty` 模块下的 `com/bruis/learnnetty/im` 包。

</details>

<details>
<summary><strong>RocketMQ 底层源码解析</strong></summary>

- RocketMQ 版本：4.9.0
- RocketMQ 底层源码解析：RocketMQ 环境搭建
- RocketMQ 底层源码解析：本地调试 RocketMQ 源码
- RocketMQ 底层源码解析：NameServer 分析
- 持续更新中...

</details>

## 支持

如果这个项目对你有帮助，欢迎 Star。源码学习是长期工程，我会持续补充更多核心链路分析、图解和实践示例。
