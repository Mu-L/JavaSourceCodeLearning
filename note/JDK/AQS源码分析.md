AQS版本：JDK 1.8

# AQS灵魂三问
## 是什么？
AQS 全称是 AbstractQueuedSynchronizer，中文通常翻译为：**抽象队列同步器**。

AQS 本身不是一把可以直接使用的锁，而是一个用于构建锁和同步器的基础框架。

JDK 中很多并发工具都基于 AQS 实现：

```java
ReentrantLock
ReentrantReadWriteLock
Semaphore
CountDownLatch
ThreadPoolExecutor.Worker
```

## 解决什么问题？
AQS 主要解决两类通用问题：

1. 如何通过一个同步状态 state 判断资源是否可获取
2. 获取失败后，如何让线程排队、阻塞，并在合适时机被唤醒



AQS 用于统一解决线程竞争、等待队列、阻塞与唤醒问题。

## 起什么作用？
AQS是 ReentrantLock、Semaphore 等同步器的基础框架。

AQS 将同步器拆成了两部分：

1. 子类负责：
    1. 定义 state 的业务含义。
    2. 定义获取资源和释放资源的规则。
2. AQS负责：
    1. CAS 修改状态。
    2. 维护等待队列。    
    3. 阻塞线程。
    4. 唤醒线程。    
    5. 处理中断、超时和取消。

例如：

1. ReentrantLock：state 表示锁的重入次数
2. CountDownLatch：state 表示剩余计数
3. Semaphore：state 表示剩余许可证数量

****

**AQS 的核心内容可以浓缩为：**

1. 一个 volatile int state
2. 一个双向同步等待队列
3. CAS 原子操作
4. LockSupport.park/unpark 阻塞与唤醒

# AQS整体架构
AQS 支持两种资源获取模式：

1. 独占模式 Exclusive：同一时刻只允许一个线程成功获取资源。
2. 共享模式 Shared：同一时刻允许多个线程成功获取资源。



典型应用：

1. 独占模式：ReentrantLock、ReentrantReadWriteLock 写锁。
2. 共享模式：Semaphore、CountDownLatch、ReentrantReadWriteLock 读锁。



两种模式共用同一个同步队列：

```plain
head                                                    tail
  |                                                       |
  v                                                       v
[哨兵节点] <-> [独占节点 T1] <-> [共享节点 T2] <-> [独占节点 T3]
```



AQS 不理解“锁”“许可证”“计数器”等业务含义。它只负责：

1. tryAcquire/tryAcquireShared 成功：线程继续执行。
2. tryAcquire/tryAcquireShared 失败：线程进入队列并阻塞。
3. tryRelease/tryReleaseShared 成功：唤醒后继线程。



<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2026/png/12890164/1783689319993-5d064d1b-9d0e-4f6f-b208-5fbaedbc0952.png)



# AQS核心结论
AQS 本质上是一个用于构建锁和同步器的基础框架，核心由两部分组成：一个表示同步状态的 state，以及一个保存等待线程的 FIFO 双向队列。



线程获取资源时，首先通过 CAS 尝试修改 state：

+ 获取成功：线程继续执行。
+ 获取失败：线程封装成 Node 加入等待队列，并通过 LockSupport.park() 阻塞。
+ 资源释放：修改 state，再通过 LockSupport.unpark() 唤醒后继节点重新竞争。



AQS 自身不定义资源如何获取和释放，而是通过模板方法交给子类实现：

+ tryAcquire()
+ tryRelease()
+ tryAcquireShared()
+ tryReleaseShared()



因此，AQS 的核心可以概括为：

+ state 表示资源或锁状态。
+ CAS 保证状态修改的原子性。
+ 等待队列管理获取资源失败的线程。
+ park/unpark 实现线程阻塞与唤醒。
+ 模板方法定义具体同步规则。
+ 支持独占模式和共享模式。
+ ConditionObject 提供条件等待队列。



AQS 将“资源竞争、线程排队、阻塞与唤醒”统一封装，子类只需要实现资源的获取和释放规则。

# AQS 的重要成员变量和内部类
## state：同步状态
state 是 AQS 中最核心的变量：

```java
private volatile int state;
```

AQS 使用一个 volatile int 保存同步状态，并通过以下方法读取和修改：

```java
protected final int getState()
protected final void setState(int newState)
protected final boolean compareAndSetState(int expect, int update)
```

其中：

```plain
getState()：读取当前同步状态
setState()：直接设置同步状态
compareAndSetState()：通过 CAS 原子更新同步状态
```

state 的含义由具体同步器决定。



ReentrantLock 中的 state表示

+ state = 0：锁未被任何线程持有
+ state = 1：当前线程第一次获取锁
+ state = 2：当前线程重入一次
+ state = 3：当前线程重入两次



Semaphore 中的 state表示当前剩余许可证数量。

例如：

```plain
初始许可证数量为 3

state = 3：还可以有 3 个线程获取
state = 2：还可以有 2 个线程获取
state = 0：没有剩余许可证
```



CountDownLatch中的state表示尚未完成的计数。

例如：

```plain
new CountDownLatch(3)

初始 state = 3
每次 countDown()，state 减 1
state = 0 时，所有 await() 线程可以继续执行
```

****

**总结：AQS 只提供一个线程安全的状态字段，不规定状态的业务含义。**



## head 和 tail：同步队列首尾节点
```java
private transient volatile Node head;
private transient volatile Node tail;
```

AQS 使用一个基于 CLH 思想改造的双向链表维护获取资源失败的线程。

```plain
head                                        tail
  |                                          |
  v                                          v
[哨兵节点] <-> [节点 T1] <-> [节点 T2] <-> [节点 T3]
```

其中：

```plain
head：队列头节点，通常是已经获取过资源的哨兵节点

tail：队列尾节点，新节点通过 CAS 追加到 tail
```

head和tail都使用 volatile 修饰，保证不同线程之间的可见性。



AQS 创建时不会立刻创建队列。只有第一次发生竞争、线程需要入队时，才会初始化一个空的哨兵节点：

```plain
head == tail == new Node()
```

这样可以避免在从未发生竞争的同步器上浪费节点对象。

## Node：同步队列节点
AQS 内部通过Node表示一个等待线程。

源码结构如下：

```java
static final class Node {
    ...
    static final Node SHARED = new Node();
    static final Node EXCLUSIVE = null;

    static final int CANCELLED = 1;
    static final int SIGNAL = -1;
    static final int CONDITION = -2;
    static final int PROPAGATE = -3;

    volatile int waitStatus;
    // 指向同步队列中的前驱节点。
    volatile Node prev;
    volatile Node next;
    // 保存当前节点对应的等待线程。
    // 当节点需要被唤醒时，AQS 会执行：LockSupport.unpark()
    volatile Thread thread;
    Node nextWaiter;
    ...
}
```

### prev
prev表示指向同步队列中的前驱节点。

```java
volatile Node prev;
```

指向同步队列中的前驱节点。

AQS 判断当前节点能否尝试获取资源时，最重要的条件是：**<font style="color:#DF2A3F;">当前节点的前驱节点是否是 head。</font>**

只有队列中的第一个有效等待节点，才有资格再次调用tryAcquire()或tryAcquireShared()竞争资源。



AQS中判断源码如下：

独占模式acquire：

```java
final boolean acquireQueued(final Node node, int arg) {
    ...
    final Node p = node.predecessor();
    if (p == head && tryAcquire(arg)) {
        ...
    }
    ...
}
```

共享模式acquire：

```java
private void doAcquireShared(int arg) {
    ...
    final Node p = node.predecessor();
    if (p == head) {
        ...
    }
    ...
}
```



**所以“当前节点的前驱节点是否是 head”是非常重要的一个条件。**



### next（设计思想非常的细节）
指向同步队列中的后继节点。

```java
volatile Node next;
```

释放资源时，AQS 通常通过 head.next 找到需要唤醒的线程。

但next只是一个优化路径，不是绝对可靠的队列判断依据。

原因是节点入队过程如下：

1. node.prev = oldTail
2. CAS 把 tail 从 oldTail 修改为 node
3. oldTail.next = node

```java
// 入队操作，并返回当前node的前序节点
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) {
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            // 1. oldTail <- node
            node.prev = t;
            // 2. oldTail <- node(tail)  将node赋值给tail，表明当前node成为了tail
            if (compareAndSetTail(t, node)) {
                // oldTail -> node(tail)
                t.next = node;
                // 返回oldTail
                return t;
            }
        }
    }
}
```



在线程完成第 2 步、尚未完成第 3 步时：

```plain
tail 已经指向新节点
但 oldTail.next 仍然是 null
```

因此在部分场景下，如果判断到next=null，则AQS 会从tail沿着prev反向扫描，寻找有效后继节点。

```java
private void unparkSuccessor(Node node) {
    ...
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            s = null;
            // 通过next找不到要unpark的节点，则从tail开始向前遍历
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }    
    ...
}
```



**总结：**因为有 next 指针，所以 release 时可以直接取 head，再通过 head.next 找到要唤醒的后继节点，通常是 O(1)。而如果没有next指针，则需要从tail尾结点向前通过prev遍历，时间复杂度为O(n)。所以在入队维护next指针的时候，因为：

1）node.prev = t;

2）compareAndSetTail(t, node)

3）t.next = node;

导致可能t.next会存在null的场景，所以通过next指针遍历会拿不到数据。所以如果next为null，就会退化成通过prev指针去获取目标节点。



### nextWaiter
nextWaiter 有两个用途。

#### 用途一：标识节点模式
```java
nextWaiter == Node.SHARED：共享节点
nextWaiter == Node.EXCLUSIVE：独占节点
```

其中 Node.EXCLUSIVE 实际是 null。

#### 用途二：连接 Condition 条件队列
Condition 条件队列是单向链表，节点之间通过 `nextWaiter` 连接：

```plain
firstWaiter
    |
    v
[T1 CONDITION] -> [T2 CONDITION] -> [T3 CONDITION]
                                              ^
                                              |
                                         lastWaiter
```

所以：

```plain
prev/next：用于 AQS 同步队列
nextWaiter：用于 Condition 条件队列，或者标记共享模式
```



### waitStatus 状态详解
Node 中最难理解的字段是：

```java
volatile int waitStatus;
```

它表示节点当前的等待状态。

JDK 8 中主要有以下几种状态：

+ CANCELLED =  1
+ SIGNAL    = -1
+ CONDITION = -2
+ PROPAGATE = -3
+ 默认状态  =  0

****

#### 0：默认状态
新创建的同步队列节点，`waitStatus` 默认为 0。

表示节点当前没有特殊状态。



#### CANCELLED = 1：节点已取消
```java
static final int CANCELLED = 1;
```

出现以下情况时，节点可能被取消：

+ 线程等待超时
+ 线程在可中断等待中被中断
+ 获取资源过程中发生异常



取消后的节点不会再次参与资源竞争。

AQS 会在后续遍历中跳过 waitStatus > 0 的节点。

需要注意：CANCELLED 是唯一的正数状态。

因此源码中经常通过下面的方式判断节点是否已取消：waitStatus > 0



#### SIGNAL = -1：后继节点需要被唤醒
```java
static final int SIGNAL = -1;
```

**这是同步队列中最重要的状态。**



假设队列结构如下：pred -> node

当 pred.waitStatus == SIGNAL  时，表示：

```plain
node 准备阻塞。
当 pred 释放资源、成为无效头节点或被取消时，pred 负责唤醒 node。
```

一个非常容易混淆的点是：

**SIGNAL 状态保存在前驱节点上，但它表达的是后继节点需要被唤醒。**

例如：

```plain
head(waitStatus = SIGNAL) -> T1
```

表示 T1 可以安全阻塞，head 对应的资源持有者释放资源时，需要唤醒 T1。



为什么不把 SIGNAL 放在当前节点上？

因为释放资源时，释放线程主要操作当前 `head`，由前驱节点记录唤醒责任，可以减少对后继节点状态的竞争修改。



#### CONDITION = -2：节点正在 Condition 条件队列中等待
```java
static final int CONDITION = -2;
```

线程调用 Condition.await() 后，会进入 Condition 条件队列，此时节点状态为 CONDITION。

```plain
Condition 条件队列：

[T1 CONDITION] -> [T2 CONDITION] -> [T3 CONDITION]
```



当其他线程调用 signal() 后，节点会：

```plain
waitStatus：CONDITION -> 0
从 Condition 条件队列转移到 AQS 同步队列
```

只有重新进入同步队列并再次获取锁后，`await()` 才会返回。



#### PROPAGATE = -3：共享模式继续传播
```java
static final int PROPAGATE = -3;
```

PROPAGATE 只用于共享模式。

它用于记录：即使当前释放动作没有直接找到需要唤醒的节点，后续共享节点仍需要继续检查并传播唤醒。



该状态主要用于解决共享模式下并发获取、释放交错时可能出现的传播遗漏问题。

可以先把它理解为：共享唤醒传播标记。



## waitStatus 的正负设计
AQS 中对 `waitStatus` 的判断非常精简：

```plain
waitStatus > 0：节点已取消，需要跳过
waitStatus < 0：节点处于有效的信号、条件或传播状态
waitStatus = 0：普通初始状态
```

这种设计让很多分支只需要判断正负，而不必逐个比较状态常量。



# AQS 同步队列为什么是 CLH 变体
AQS 的同步队列通常被称为 CLH 队列，但它并不是原始的 CLH 自旋锁队列。

原始 CLH 队列的核心思想是：每个线程关注自己的前驱节点状态。



AQS 保留了这个思想，但做了改造：

```plain
原始 CLH：线程主要自旋等待
AQS：线程尝试几次后，通过 LockSupport.park 阻塞

原始 CLH：通常只需要前驱引用
AQS：增加 prev 和 next，便于取消、唤醒和队列维护
```



AQS 节点主要观察前驱节点：

```plain
前驱是 head：尝试获取资源
前驱为 SIGNAL：当前线程可以安全 park
前驱已取消：跳过取消节点，重新连接有效前驱
```

因此，AQS 同步队列可以理解为：基于 CLH 思想改造的 FIFO 双向阻塞队列



# AQS 留给子类实现的五个方法
AQS 将排队和阻塞机制实现好，但资源获取规则需要子类定义。

核心扩展方法如下：

```java
protected boolean tryAcquire(int arg)
protected boolean tryRelease(int arg)
protected int tryAcquireShared(int arg)
protected boolean tryReleaseShared(int arg)
protected boolean isHeldExclusively()
```

默认实现都会抛出UnsupportedOperationException，子类根据需要选择实现独占模式、共享模式或两者。



# AQS多线程竞争入队流程
## 独占锁场景
下面按**独占锁**场景流程，比如ReentrantLock.lock()。

**背景：当前有T0、T1、T2三个线程竞争锁，默认T0已获得锁。**

### 1. T0 已经持有锁
```plain
state = 1
owner = T0

同步队列：空
head = null
tail = null
```

****

### 2. T1 来竞争，tryAcquire 失败，封装成 Node 入队
AQS 执行：addWaiter(Node.EXCLUSIVE)



队列第一次初始化，会先创建哨兵 head：

```plain
head(dummy) <-> T1
                ^
               tail
```

此时节点状态：

```plain
head.waitStatus = 0
T1.waitStatus   = 0
```

### 3. T1 入队后<font style="color:#DF2A3F;">马上自旋</font>再次竞争，失败，准备阻塞
T1 在 acquireQueued 里判断：

```plain
p == head && tryAcquire(arg)
```

此时 `p == head`，但锁还被 T0 持有，所以失败。

然后进入：

```plain
shouldParkAfterFailedAcquire(head, T1)
```

T1 会把自己的前驱 head 设置成 **<font style="color:#DF2A3F;">SIGNAL状态</font>**：

```plain
head(SIGNAL) <-> T1(0)
                  ^
                 tail
```



然后T1发现前序节点是SIGNAL状态，然后T1调用park()进入阻塞状态。



### 4. T2 也来竞争，失败后入队
```plain
head(SIGNAL) <-> T1(0) <-> T2(0)
                              ^
                             tail
```

T2 的前驱是 T1。T2 获取失败后，马上自旋再次竞争锁，失败之后会把前驱 T1 设置成 `SIGNAL`：

```plain
head(SIGNAL) <-> T1(SIGNAL) <-> T2(0)
                                  ^
                                 tail
```

然后T2发现前序节点是SIGNAL状态，然后T2调用park()进入阻塞状态。

### 5. T0 释放锁，AQS 从 head 唤醒后继
T0 调用：

```plain
unlock()
  -> release(1)
  -> tryRelease(1)
  -> unparkSuccessor(head)
```

队列：

```plain
head(SIGNAL) <-> T1(SIGNAL) <-> T2(0)
```

AQS 找：

```plain
head.next == T1
```

然后：

```plain
LockSupport.unpark(T1.thread)
```

结果：

```plain
T1 被唤醒
T2 仍阻塞
```

### 6. T1 被唤醒后重新竞争锁，成功
T1 醒来后回到循环：

```plain
p == head && tryAcquire(arg)
```

此时锁已经空了，所以 T1 成功获取锁。

然后：

```plain
setHead(T1)
```

队列变成：

```plain
head = T1
T1(thread=null, prev=null, SIGNAL) <-> T2(0)
                                        ^
                                       tail
```

旧的 dummy head 会断开，帮助 GC：

```plain
T1 成为新的 head
```

注意：

```plain
T1 成为 head 后，它本身不再代表一个等待线程；
它代表当前成功获取过锁的节点。
```

### 7. T1 释放锁，再唤醒 T2
T1 执行：

```plain
unlock()
  -> release(1)
  -> unparkSuccessor(head)
```

此时：

```plain
head(T1, SIGNAL) <-> T2(0)
```

AQS 唤醒：

```plain
head.next == T2
```

T2 醒来，重新执行：

```plain
p == head && tryAcquire(arg)
```

成功后：

```plain
setHead(T2)
```

队列变成：

```plain
head = T2
tail = T2
```

如果后面没有等待节点：

```plain
T2.waitStatus 通常是 0
```

# AQS核心方法
## acquire()
当某个线程发起锁获取，比如调用ReentrantLock.lock()方法时，调用链会走到AbstractQueuedSynchronizer.acquire()方法，源码如下：

```java
public final void acquire(int arg) {
if (!tryAcquire(arg) &&
    acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
    selfInterrupt();
}
```

## nonfairTryAcquire()
tryAcquire()逻辑在AQS子类里，当前就是在ReentrantLock中，核心逻辑：

```java
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    // 获取AQS状态
    int c = getState();
    // 没有线程持有这个共享状态
    if (c == 0) {
        // CAS 变更state状态
        if (compareAndSetState(0, acquires)) {
            // 标识当前线程持有state
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // 如果是持有锁的线程
    else if (current == getExclusiveOwnerThread()) {
        // state加一，重复持有锁
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

nonfairTryAcquire()方法比较简单，主要就是通过CAS变更state状态，然后将持有锁的线程标识为owner，后续竞争锁则判断该线程是否持有，否则加锁失败返回false。



## addWaiter()
```java
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // 尝试一次快速入队
        Node pred = tail;
        // 如果pred=null，则表示队列还未初始化，快速入队逻辑不负责初始化
        if (pred != null) {
            node.prev = pred;
            // 并发竞争失败，会走到兜底入队逻辑
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }
        }
        // 兜底入队逻辑
        enq(node);
        return node;
    }
```

addWaiter() 先用一次 CAS 快速入队；如果队列未初始化或发生并发竞争，就交给 enq() 自旋，确保当前线程节点最终进入 AQS 同步队列。



enq() 方法负责同步队列的延迟初始化，以及节点竞争入队。当队列未初始化时，通过 CAS 创建哨兵头节点；当队列已经初始化时，通过 CAS 将当前节点设置为新的尾节点。如果 CAS 失败，则不断自旋重试，直到节点成功入队。

```java
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            // 队列初始化
            if (t == null) { // Must initialize
                if (compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                // CAS往队列尾部添加节点
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    // !返回的是入队节点的前序节点
                    return t;
                }
            }
        }
    }
```

## acquireQueued()


```java
/**
 * 已入队节点以独占模式获取同步状态。
 *
 * @return 等待过程中是否发生过中断
 */
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;

        // 自旋，直到成功获取同步状态
        for (;;) {
            final Node p = node.predecessor();

            // 只有头节点的直接后继才有资格尝试获取同步状态
            if (p == head && tryAcquire(arg)) {
                // 获取成功，当前节点成为新的头节点
                setHead(node);
                p.next = null; // 断开旧头节点，帮助 GC
                failed = false;
                return interrupted;
            }

            // 获取失败，判断是否需要阻塞；被唤醒后检查中断状态
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt()) {
                interrupted = true;
            }
        }
    } finally {
        // 出现异常等获取失败的情况时，取消当前节点
        if (failed) {
            cancelAcquire(node);
        }
    }
}
```

shouldParkAfterFailedAcquire()方法核心就是将入参node的前驱节点状态置为：SIGNAL，然后方法返回false，继续执行后面的parkAndCheckInterrupt()，将node节点置为阻塞状态。



这里就是对应着有新线程竞争锁失败之后，先加入队列，之后自旋尝试再次竞争锁，失败了则将前序节点置为SIGNAL，然后将自己阻塞等待被唤醒。



下面再分析下释放锁的流程，释放锁核心会调用AQS的release()方法。

```java
public final boolean release(int arg) {
    // 子类负责更新锁状态，并判断锁是否已完全释放
    if (tryRelease(arg)) {
        Node h = head;

        // 头节点存在且后继节点需要唤醒
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);

        return true;
    }

    // 重入次数尚未归零，本次只减少持锁次数
    return false;
}
```



tryRelease()方法在ReentrantLock中，源码如下：

```java
protected final boolean tryRelease(int releases) {
    // 减少重入计数
    int c = getState() - releases;

    // 只有锁的持有线程才能释放锁
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();

    boolean free = false;

    // 重入计数归零，锁才算完全释放
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }

    // 更新剩余重入次数
    setState(c);
    return free;
}
```



AQS的unparkSuccessor()源码如下：

```java
/**
 * 唤醒等待队列中有效的后继节点。
 */
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;

    // 清除当前节点的待唤醒标记
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    Node s = node.next;

    // next 无效时，从队尾反向查找最靠前的有效等待节点
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev) {
            // waitStatus > 0 表示节点已经取消
            if (t.waitStatus <= 0)
                s = t;
        }
    }

    // 唤醒目标线程，让其重新参与锁竞争
    if (s != null)
        LockSupport.unpark(s.thread);
}
```



释放锁流程总结：

1. 当前线程调用 unlock()，最终进入 AQS 的 release(1)。
2. release() 调用 tryRelease(1)，减少 state 表示的重入次数。
3. 如果当前线程不是锁的持有者，抛出 IllegalMonitorStateException。
4. 如果 state 仍大于 0，说明当前线程还持有重入锁，不唤醒其他线程。
5. 如果 state 减少到 0，清空锁的持有线程，表示锁已完全释放。
6. release() 检查等待队列，通过 unparkSuccessor() 找到有效的后继节点。
7. 调用 LockSupport.unpark() 唤醒对应线程，使其重新尝试获取锁。



需要注意：unpark() 只是让等待线程具备继续运行的条件，并不代表它立刻获得锁。线程被唤醒后，仍然需要参与锁竞争。

