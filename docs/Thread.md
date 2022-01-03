# Thread

## 多线程

- 多线程用的多吗？
- JDK 的线程池都有用过嘛？
  newFixedThreadPool
  newSingleThreadExecutor
  newCachedThreadPool
  newScheduledThreadPool

## 线程安全

## 同步机制

### Synchronized

synchronized是一种互斥锁，一次只能允许一个线程进入被锁住的代码块

又因为每个对象都会有一个与之对应的monitor对象，monitor对象中存储着当前持有锁的线程以及等待锁的线程队列

## Lock

## 死锁

## 实践、排查

- 工作中常用的锁有哪些
- 关于线程安全，常用的锁介绍一下
- 熟悉 JDK 的哪些锁？它们的优缺点和区别？
  Lock, Synchronized, volatile
- Synchronized 锁的使用场景
  同步代码块，原子语义执行
- 你阅读过 Java 虚拟机偏向锁的源码，能否说明下偏向锁怎样一步步升级的？
  无竞争时通过 CAS 更新对象 Mark word 的 ThreaID 获取偏向锁
  有竞争时撤销偏向锁，升级为轻量级锁，轻量级锁的目标是减少线程挂起的次数
  当前线程会在栈帧下创建 Lock Record，LockRecord 会把 Mark Word 的信息拷贝进去，且有个 Owner 指针指向加锁的对象。
  通过 CAS 争用锁，假如 CAS 失败则自旋等待，适用于少量并发的场景
  当竞争频繁，自旋失败一定次数后，膨胀为重量级锁，依赖系统底层的 Mutex Lock 实现
- 轻量级锁自旋多少次会失败膨胀到重量级锁？
  直接失败
- 轻量级锁升级为重量级锁的条件？等待时间多久？
  CAS 失败，1.6 使用 -XX:PreBlockSpin 设置自旋锁等待次数，默认为10
  1.7 加入自适应自旋锁，由虚拟机自动调整。如果自选线程之前刚刚获得过锁，则虚拟机会认为这次自旋也很有可能会成功，进而演唱自旋时间。繁殖，如果某个锁自旋很少获得过成功，则之后获取锁的时候可能省略掉自旋过程，避免浪费处理器资源。
- 多线程的内存屏障机制你能简单说一下嘛？
  内存屏障可以禁止特定类型处理器的重排序，load、write、fince
- 禁止指令重排序有哪些方法可以做到？
  volatile、synchronized、Unsafe.fullFence()，final、cas
- volatile 有什么作用？
- 能说一下 volatile 关键字嘛，它跟 Atomic 有什么不同
  轻量级同步语义，通过内存屏障刷新写前/后缓存，禁止指令重排序。保障了可见性和有序性
- new 一个实例的过程在 JVM 中是怎么操作的？它是线程安全的嘛？
  不安全，new 非原子性操作，分为分配内存，初始化内存，赋值到变量
- 如何做到 new 对象的线程安全
  synchronized + volatile
- 状态的变更，有具体的场景吗？
- 比如单例模式，使用或不用 volitile 有什么区别吗？
- volitile 为何要结合 synchornized 保障可见性？因为 synchornized 已经保障了可见性
- volatile 可以使一个非原子操作跟原子操作码？比如 long 类型得加持
  如果使用volatile修饰long和double，那么其读写都是原子操作
- volitile 更适合使用在什么场景？
- CAS 无锁机制的好处和坏处？
  减少线程上下文的切换，用户态和内核态的转换。
  读不加锁，读写不冲突

- 并发包下得其它工具都用过哪些？
- 介绍一下 AQS 框架
- 我们可以通过 Unsafe 可以做哪些事情？
- Unsafe 我们怎么去获取？还有别的方式嘛？
- Unsafe 跟内存管理相关的方法有哪些？
- Java Lock 接口的实现原理？
- Atomic 的源码也看过吧？AtomicInteger 的默认值怎么来的？ValueObject 的值是怎么来- 的？
- ConcurrentHashMap 底层实现原理能说一下嘛？
- ConcurrentHashMap 在 1.8 前后的实现区别？
- 有看过内部的源代码嘛？能简单说一下 put 操作的流程
- ConcurrentHashMap 和 SynchronizedHashMap 有什么区别？分别用于什么场景
- ConcurrentHashMap 底层用的分段锁是哪个版本？之后使用的什么方式？
- CopyOnWriteArrayList 有什么缺点？（写多的场景下会存在频繁回收垃圾的缺点）
- CopyOnWriteArrayList、ConcurrentHashMap 在底层是怎样保障线程安全的？
- 说一下并发容器，像 ConncurrentHashMap，CopyOnWriteArrayList 在什么场景下使用呢？- （说的还不错，好的表达）
- 除了以上两种方式外还有没有其它的保障线程安全的方式？
- 读写锁的原理是什么？
  AQS 的共享锁和互斥锁

- 有没有遇到过死锁的问题？怎么排查或解决的？
- 什么情况下会导致死锁的场景？***（除了循环依赖还有别的吗？）
- T：循环依赖是一种，另一种是不设超时无法释放的资源
- 有什么解决方案嘛？（除了同时申请资源的方式还有其它么，超时）
- T：设置占用超时时间，有序获取锁，死锁检测释放，减小锁的粒度（尽快释放减少占用时间）- 而非扩大（合并锁）？

- 在 JVM 中有什么方式提升加锁的效率？
- 分布式锁了解吗？
- 有用过分布式锁么，怎么实现的嘛
- 有没有了解过其它开源框架提供的锁？（Redisson，zk）
- zk 的锁有尝试过吗？
- zk 的节点有哪些类型？
- ConcurrentHashMap 如果要放入 100个对象，初始化的容量应该指定多大？（128 是错误的答案）
- 针对线程池大小有做过具体的优化吗？
- 如果让你做内存计数器，会用哪种锁？例如 metric 统计每个 URL 的 RT
- 多线程中如何实现一个计数器？
