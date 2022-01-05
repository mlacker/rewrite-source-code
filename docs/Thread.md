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

### Volatile

因为 java 语言的限制，无法保障数组元素的可见性。

### Synchronized

synchronized是一种互斥锁，一次只能允许一个线程进入被锁住的代码块

又因为每个对象都会有一个与之对应的monitor对象，monitor对象中存储着当前持有锁的线程以及等待锁的线程队列

锁对象的 hashCode 方法被调用，需要撤销偏向锁。

偏向锁适用于只有一个线程获取锁的场景，释放锁后依然保留 mark word 的状态，所以只有一次 CAS 操作的开销。当其它线程 CAS 失败则升级为轻量级锁
轻量级锁适用于线程交替获取锁的场景，CAS 失败则升级为重量级锁，无自旋重试操作。
重量级锁则先尝试 CAS 自适应自旋获取锁（非公平锁），根据 JVM 统计信息调整自旋时间长短。
失败则进入 cxq 队列，当对象锁的持有者释放后，进入 Entry List 队列。
当调用 Object.wait() 进入 Wait Set 队列

当一个线程尝试获得锁时，如果该锁已经被占用，则会将该线程封装成一个ObjectWaiter对象插入到cxq的队列的队首，然后调用park函数挂起当前线程。在linux系统上，park函数底层调用的是gclib库的pthread_cond_wait，JDK的ReentrantLock底层也是用该方法挂起线程的。更多细节可以看我之前的两篇文章：关于同步的一点思考-下，linux内核级同步机制--futex

当线程释放锁时，会从cxq或EntryList中挑选一个线程唤醒，被选中的线程叫做Heir presumptive即假定继承人（应该是这样翻译），就是图中的Ready Thread，假定继承人被唤醒后会尝试获得锁，但synchronized是非公平的，所以假定继承人不一定能获得锁（这也是它叫"假定"继承人的原因）。

如果线程获得锁后调用Object#wait方法，则会将线程加入到WaitSet中，当被Object#notify唤醒后，会将线程从WaitSet移动到cxq或EntryList中去。需要注意的是，当调用一个锁对象的wait或notify方法时，如当前锁的状态是偏向锁或轻量级锁则会先膨胀成重量级锁。

#### Synchronized和ReentrantLock的区别

Synchronized是JVM层次的锁实现，ReentrantLock是JDK层次的锁实现；
Synchronized的锁状态是无法在代码中直接判断的，但是ReentrantLock可以通过ReentrantLock#isLocked判断；
Synchronized是非公平锁，ReentrantLock是可以是公平也可以是非公平的；
Synchronized是不可以被中断的，而ReentrantLock#lockInterruptibly方法是可以被中断的；
在发生异常时Synchronized会自动释放锁（由javac编译时自动实现），而ReentrantLock需要开发者在finally块中显示释放锁；
ReentrantLock获取锁的形式有多种：如立即返回是否成功的tryLock(),以及等待指定时长的获取，更加灵活；
Synchronized在特定的情况下对于已经在等待的线程是后来的线程先获得锁（上文有说），而ReentrantLock对于已经在等待的线程一定是先来的线程先获得锁；

#### 偏向锁

- 检查对象的 mark word 的锁标志位为无锁状态，且偏向模式（101）
- 检查对象的偏向线程是否为当前线程，是则获取对象锁（可重入）
  - 如果（类）偏向模式关闭，则尝试撤销偏向锁（默认开启）
  - 利用 CAS 操作将 mark word 替换为 class 的 mark word
- 如果 epoch 不等于 class 的 epoch，则尝试重偏向
  - 失败则撤销偏向锁
- 利用 CAS 操作从匿名偏向设为偏向当前线程，成功则获取对象锁
- 否则说明存在竞争，进入 monitorenter 方法

- 如果锁对象不是偏向模式或者已经偏向其它线程
- 构造一个无锁状态的 Displaced Mark Word，并将 Lock Record 的 lock 指向它
- 利用 CAS 将对象头的 mark word 替换为指向 Lock Record 的指针
  - CAS 失败则检查是不是锁重入，是这设置 displaced header 为 null 表示重入
  - 否则进入 monitorenter 方法

monitorenter 》 slow_enter

- 如果是无锁状态，设置 Displaced Mark Word，并用 CAS 替换对象头为 Lock Record 的地址
- 否则，检查对象锁（轻量级锁）的所有者是否为当前线程，是则为重入锁
- 否则，说明存在竞争锁，直接膨胀为重量级锁。**无自旋重试**

偏向锁撤销

- 检查偏向的线程是否存活，遍历 jvm 的所有线程，否则直接撤销偏向锁。
  - 撤销至匿名偏向状态
  - 否则设置为无锁状态
- 检查偏向的线程是否还在同步块中，存在则升级为**轻量级锁**
- 撤销偏向锁

## Lock

## 死锁

## 实践、排查

### 优化

- 锁粗化（Lock Coarsening）：将多个连续的锁扩展成一个范围更大的锁，用以减少频繁互斥同步导致的性能损耗。
- 锁消除（Lock Elimination）：JVM 即时编译器在运行时，通过逃逸分析，如果判断一段代码中，堆上所有数据不会逃逸出去被其它线程访问到，就可以去除这些所。
- 轻量级锁（Lightweight Locking）：JDK 1.6 引入

- 工作中常用的锁有哪些
- 关于线程安全，常用的锁介绍一下
- 熟悉 JDK 的哪些锁？它们的优缺点和区别？
  Lock, Synchronized, volatile
- Synchronized 锁的使用场景
  同步代码块，原子语义执行
- 你阅读过 Java 虚拟机偏向锁的源码，能否说明下偏向锁怎样一步步升级的？
  检查对象的 Mark Word ，其中锁标志位如果为 010 表示为无锁状态
  使用 CAS 操作尝试获取偏向锁，更新成功则获取偏向锁，设置 ThreadId 为当前线程，偏向模式为 1
  当对象是可偏向模式时，检查 ThreadId 是否为当前线程，相同则成功获取锁（可重入）
  否则，撤销偏向锁，进化为轻量级锁，锁标志位为：00
  偏向锁适用于无竞争的场景，而轻量级锁适用于少量的并发冲突
  轻量级锁会在当前线程的栈帧下创建 Lock Record，LockRecord 会把 Mark Word 的信息拷贝进去，且有个 Owner 指针指向加锁的对象。
  轻量级锁是通过 CAS 争用锁，假如 CAS 失败则自旋等待，适用于少量并发的场景
  当 CAS 操作失败后，则膨胀为重量级锁，
  重量锁是通过 ObjectMonitor 实现的，依赖系统底层的 Mutex Lock 实现
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
