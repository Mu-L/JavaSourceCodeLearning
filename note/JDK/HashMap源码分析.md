+ 当前HashMap版本：JDK1.8
+ 转载请标明出处

# HashMap底层原理图
![HashMap原理图](https://github.com/coderbruis/JavaSourceCodeLearning/releases/download/images-v1/HashMap.png)

# HashMap的重要成员变量以及内部类
> **默认容量：DEFAULT_INITIAL_CAPACITY**
>

```java
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16
```

当HashMap没有设置大小时，调用HashMap的put方法时，会进行初始值，并使用DEFAULT_INITIAL_CAPACITY设置默认大小。调用位置在：

```java
final Node<K,V>[] resize() {
    ...
        else {               // zero initial threshold signifies using defaults
            // HashMap默认大小，16
            newCap = DEFAULT_INITIAL_CAPACITY;
            // 扩容阈值，12
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }    
    ...	
}
```



> **扩容阈值：DEFAULT_LOAD_FACTOR**
>

```java
static final float DEFAULT_LOAD_FACTOR = 0.75f;
```

在resize()中可以看到，当HashMap首次添加元素，调用put时，会计算第一次扩容阈值12，也就是说HashMap中元素=12即触发扩容。



扩容实际发生在putVal()中，源码如下：

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
    ...
        if (++size > threshold)
            resize();    
    ...
}
```

当HashMap中添加了新元素，size递增之后判断是否大于扩容阈值。



> **树化相关配置：TREEIFY_THRESHOLD、UNTREEIFY_THRESHOLD、MIN_TREEIFY_CAPACITY**
>

```java
// 桶内链表长度达到 8，考虑红黑树化
static final int TREEIFY_THRESHOLD = 8;
// 数组容量至少 64，才真正允许红黑树化
static final int UNTREEIFY_THRESHOLD = 6;
// 红黑树节点减少到 6 个或更少，考虑退化回链表
static final int MIN_TREEIFY_CAPACITY = 64;
```

链表长到 8 时，如果数组容量小于 64，先扩容；如果数组容量已经至少 64，才转红黑树。树节点少到 6 时，再退化回链表。



```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
    ...
 if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
    ...
}

final void treeifyBin(Node<K,V>[] tab, int hash) {
    ...
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            // 先扩容
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            // 红黑树化
        }    
    ...
}
```

可以看到，当HashMap中元素小于MIN_TREEIFY_CAPACITY时，是先进行的扩容，而非直接红黑树化。



> **HashMap内部类Node**
>

这个类就是HashMap桶中存储的基本单元类，HashMap其实可以理解为一个数组，数组里每个位置叫一个桶bucket。

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}
```

在HashMap中通过一个Node数组来存储Node节点。

```java
transient Node<K,V>[] table;
```

如果多个 key 经过 hash 计算后落到同一个桶，就会通过 `next` 串起来，形成链表：

```plain
table[3]
   |
   v
Node(key1, value1)
   |
  next
   v
Node(key2, value2)
   |
  next
   v
Node(key3, value3)
```

HashMap 的每个桶位保存一个头节点引用；发生 hash 冲突时，新节点通过 next 挂在这个桶的链表后面；链表过长时可能转成红黑树。

还有一个细节需要注意，Node不仅存了key、value最核心的键值对信息，还存储了这个Node的hash值，这个hash值的作用是：**<font style="color:#DF2A3F;">用于快速比较、查找、扩容迁移和树化查找，避免反复计算 hashCode，也保证节点定位稳定。</font>**

hash核心作用代码：

```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {
    ...
     Node<K,V> e; K k;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
    ...
}
```

putVal() 里比较 key 是否已经存在，这里先比较hash值，如果hash值不一样，则key一定不相等，可以直接跳过，避免频繁调用 equals()。



查找时也会用到hash：

```java
final Node<K,V> getNode(int hash, Object key) {
    ...
    if (first.hash == hash && // always check first node
        ((k = first.key) == key || (key != null && key.equals(k))))
        return first;
    ...
}
```

**总结：Node.hash 是 key 的缓存 hash 值，用于快速比较、查找、扩容迁移和树化查找，避免反复计算 hashCode，也保证节点定位稳定。**







# HashMap核心方法源码分析
## putVal()
```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
               boolean evict) {

    // tab：HashMap 底层数组
    // p：当前桶的第一个节点
    // n：数组长度
    // i：key 对应的桶下标
    Node<K,V>[] tab; Node<K,V> p; int n, i;

    // 如果 table 还没初始化，或者长度为 0，则先 resize 初始化数组
    // new HashMap<>() 第一次 put 时，会在这里创建默认长度 16 的数组
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;

    // 计算桶下标：(n - 1) & hash
    // 因为 n 是 2 的幂，所以等价于 hash % n，但效率更高
    // 如果这个桶为空，直接放入新 Node
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);

    else {
        // e：最终找到的旧节点；如果为 null，说明是新增 key
        // k：临时保存已有节点的 key
        Node<K,V> e; K k;

        // 先检查桶中第一个节点是否就是目标 key
        // 先比 hash，再比 key 引用或 equals
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;

        // 如果桶已经是红黑树结构，走红黑树插入/查找逻辑
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);

        else {
            // 桶是普通链表，遍历链表
            for (int binCount = 0; ; ++binCount) {

                // 如果下一个节点为空，说明没有找到相同 key
                // 把新节点追加到链表尾部
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);

                    // 链表长度达到树化阈值 8 时，尝试树化
                    // 注意：treeifyBin 内部还会判断 table 长度是否 >= 64
                    // 如果小于 64，优先扩容，不会真正树化
                    if (binCount >= TREEIFY_THRESHOLD - 1)
                        treeifyBin(tab, hash);

                    break;
                }

                // 找到 hash 和 key 都相同的旧节点，停止遍历
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;

                // 继续向后遍历链表
                p = e;
            }
        }

        // e != null 表示找到了旧 key，不是新增，而是更新 value
        if (e != null) {
            V oldValue = e.value;

            // onlyIfAbsent 为 false：直接覆盖旧值
            // onlyIfAbsent 为 true：只有旧值为 null 时才覆盖
            // put() 传 false，putIfAbsent() 传 true
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;

            // LinkedHashMap 扩展点：访问节点后回调
            // HashMap 中是空实现
            afterNodeAccess(e);

            // 返回旧值
            return oldValue;
        }
    }

    // 结构性修改次数 +1
    // 用于 fail-fast，比如迭代时检测并发修改
    ++modCount;

    // 新增节点后 size +1
    // 如果 size 超过扩容阈值 threshold，则扩容
    if (++size > threshold)
        resize();

    // LinkedHashMap 扩展点：插入节点后回调
    // HashMap 中是空实现
    afterNodeInsertion(evict);

    // 新增 key 时返回 null
    return null;
}
```

从源码中可以看到几个细节。

### 1）**JDK8 的 HashMap 链表插入用的是尾插法**。
```java
if ((e = p.next) == null) {
    p.next = newNode(hash, key, value, null);
    ...
    break;
}
```

```plain
table[i] -> A -> B
```

插入新元素之后

```plain
table[i] -> A -> B -> C
```

对比 JDK7，JDK7 HashMap 扩容迁移时使用头插法，可能在并发扩容下形成环链表，导致死循环。JDK8 改了扩容迁移逻辑，并且普通链表插入也是尾插，能保持链表相对顺序。

JDK8 HashMap 仍然不是线程安全的。尾插法解决不了所有并发问题。并发 put 仍可能出现数据覆盖、丢数据、size 不准、扩容状态异常等问题。



### 2）JDK8 HashMap线程不安全原因分析
JDK8 HashMap 不安全，不是因为还会像 JDK7 那样容易成环，而是因为 put、size++、resize、table 发布、链表/红黑树修改都没有加锁或 CAS，多线程并发读写会发生覆盖、丢数据、计数错误和可见性问题。



> **桶为空时，多线程操作桶，会直接覆盖table[i]**
>

```plain
if ((p = tab[i = (n - 1) & hash]) == null)
    tab[i] = newNode(hash, key, value, null);
```

此处最核心原因是操作同一个桶位置，没有加锁，也没有进行CAS，线程不安全。



> **链表尾插时，p.next 可能互相覆盖**
>

```java
if ((e = p.next) == null) {
    p.next = newNode(hash, key, value, null);
    ...
    break;
}
```

多线程尾插法容易导致p.next正确结果被覆盖。



> **++size不是原子操作**
>

```java
if (++size > threshold)
    resize();
```

此处++size不是原子操作，会导致最终size结果不准确。





### 3）JDK7 HashMap线程不安全原因分析
JDK7 并发扩容时，头插法会反转链表，两个线程交叉修改同一批 Entry 的 next 指针，就可能把 A.next 指向 B，同时又把 B.next 指回 A，形成死循环。

```plain
T1 线程处理原始链表：

A -> B -> null


T2 线程头插迁移后，把指针改成：

B -> A -> null


T1 线程继续按旧进度迁移，但读到了 T2 线程改过的 B.next：

B.next = A


最后成了循环链表，变成下图：

A -> B
^    |
|____|
```

## getNode()
getNode()方法源码如下

```java
final Node<K,V> getNode(int hash, Object key) {
    // tab：底层数组
    // first：桶里的第一个节点
    // e：遍历链表时的当前节点
    // n：数组长度
    // k：临时保存节点 key
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;

    // table 不为空、长度大于 0，并且目标桶不为空，才继续查找
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {

        // 先检查桶里的第一个节点
        // 先比 hash，再比 key 引用或 equals
        if (first.hash == hash &&
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;

        // 第一个节点不是目标 key，并且后面还有节点
        if ((e = first.next) != null) {

            // 如果桶已经树化，走红黑树查找
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);

            // 普通链表，依次向后遍历
            do {
                // 找到 hash 和 key 都匹配的节点，直接返回
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;

                // 继续访问下一个节点
            } while ((e = e.next) != null);
        }
    }

    // table 为空、桶为空，或者遍历完没找到
    return null;
}
```

	核心流程：

1.table 为空，直接返回 null。

2.根据 hash 定位桶下标。

3.先查桶里的第一个节点。

4.如果是红黑树，走树查找。

5.否则遍历链表。

6.找不到返回 null。

## hash()
hash()方法是HashMap中的hash扰动函数，作用是：把 key 的 hashCode() 再处理一下，让高位信息也参与到低位计算，减少哈希冲突。

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

	最核心的是这一段：(h = key.hashCode()) ^ (h >>> 16)

> **为什么要这么做？**
>

在HashMap中计算桶下标都需要通过：(n - 1) & hash  来计算。又因为n是2的幂，所以这个计算主要以来的是hash的**<font style="color:#DF2A3F;">低位</font>**，高位一直没有利用到。HashMap初始容量为16，则n-1=15，15的二进制位：0000 1111，那么：(n - 1) & hash由于与操作的特性，这实际上只看hash的低4位。**<font style="color:#DF2A3F;">这会导致：如果很多 key 的低位相同，即使高位不同，也会落到同一个桶里。</font>**

所以 HashMap 做了这个扰动：h ^ (h >>> 16)。把高 16 位右移到低 16 位，再和原 hash 异或，让高位信息参与低位计算。

举例：

```java
>>>是无符号右移：整体向右移动，左边补 0，右边被移出去的低位丢弃。

原始 hash:

高 16 位        低 16 位
AAAA AAAA      BBBB BBBB

h >>> 16:

0000 0000      AAAA AAAA

异或后:

AAAA AAAA      (BBBB BBBB ^ AAAA AAAA)
```

## resize()
resize也是HashMap的核心方法之一，源码如下：

```java
final Node<K,V>[] resize() {
    // 旧数组
    Node<K,V>[] oldTab = table;

    // 旧容量，table 为空则为 0
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    // 旧扩容阈值
    int oldThr = threshold;
    // 新容量、新阈值
    int newCap, newThr = 0;

    // 情况一：旧数组已经存在，说明是正常扩容
    if (oldCap > 0) {
        // 已经达到最大容量，不能再扩容
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 容量扩大 2 倍
        // 阈值也扩大 2 倍
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                 oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1;
    }

    // 情况二：数组还没创建，但 threshold > 0
    // 说明构造 HashMap 时指定了初始容量
    // 例如 new HashMap<>(32)
    // 此时 threshold 暂时存的是 tableSizeFor(initialCapacity)
    else if (oldThr > 0)
        newCap = oldThr;

    // 情况三：无参构造 new HashMap<>()
    // 第一次 put 时走这里，使用默认容量 16，默认阈值 12
    else {
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }

    // 如果上面没有算出新阈值，则按 newCap * loadFactor 计算
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }

    // 更新扩容阈值
    threshold = newThr;
    // 创建新数组
    @SuppressWarnings({"rawtypes","unchecked"})
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    // table 指向新数组
    table = newTab;

    // 如果旧数组不为空，需要迁移旧数据
    if (oldTab != null) {

        // 遍历旧数组每个桶
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            // 如果当前桶不为空
            if ((e = oldTab[j]) != null) {

                // 旧桶置空，帮助 GC
                oldTab[j] = null;
                // 情况一：桶里只有一个节点，直接重新计算下标放入新数组
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                // 情况二：桶里是红黑树，走红黑树拆分逻辑
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                // 情况三：桶里是链表
                else {
                    // lo 链：扩容后仍然留在原下标 j
                    Node<K,V> loHead = null, loTail = null;
                    // hi 链：扩容后移动到 j + oldCap
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;

                    // 遍历旧链表，把节点拆成 lo 和 hi 两条链
                    do {
                        // 先保存下一个节点
                        next = e.next;
                        // 判断扩容后位置是否不变
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        // 扩容后位置变为 原下标 + oldCap
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }

                    } while ((e = next) != null);

                    // lo 链放回原下标 j
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    // hi 链放到新下标 j + oldCap
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }

    // 返回新数组
    return newTab;
}
```





# 总结
HashMap中最核心的概念如下

```java
数组 + 链表 + 红黑树
默认容量 16
负载因子 0.75
容量始终是 2 的幂
链表长度 >= 8 且 table 容量 >= 64 时树化
树节点过少时退化回链表
允许 null key / null value
非线程安全
```

	

JDK8和JDK7相比：

```java
JDK7:
数组 + 链表

JDK8:
数组 + 链表 + 红黑树
```

