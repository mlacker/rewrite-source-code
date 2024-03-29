# Thread

## 异步

异步是非阻塞的编程模式

异步和同步是相对的，同步就是顺序执行，执行完一个再执行下一个，需要等待、协调运行。
异步就是彼此独立,在等待某事件的过程中继续做自己的事，不需要等待这一事件完成后再工作。
线程就是实现异步的一个方式。异步是让调用方法的主线程不需要同步等待另一线程的完成，从而可以让主线程干其它的事情。

异步和多线程并不是一个同等关系,异步是最终目的,多线程只是我们实现异步的一种手段。

### 并发（Concurrent）和并行（Parallel）

在单处理器中多道程序设计系统中，进程被交替执行，表现出一种并发的外部特征；在多处理器系统中，进程不仅可以交替执行，而且可以重叠执行。在多处理器上的程序才可实现并行处理。从而可知，并行是针对多处理器而言的。并行是同时发生的多个并发事件，具有并发的含义，但并发不一定并行，也亦是说并发事件之间不一定要同一时刻发生。

![并发（Concurrent）和并行（Parallel）](https://upload-images.jianshu.io/upload_images/1286371-417c4eff57c89998?imageMogr2/auto-orient/strip|imageView2/2/w/473/format/webp)

## 多线程

![线程状态转换](https://tech.youzan.com/content/images/2021/06/-----5.svg)

- 多线程用的多吗？
- JDK 的线程池都有用过嘛？
  newFixedThreadPool
  newSingleThreadExecutor
  newCachedThreadPool
  newScheduledThreadPool

## 线程安全

当多个线程同时访问一个对象时，调用这个对象的行为都可以获得正确的结果，那就称这个对象是线程安全的。

### 线程安全特性

#### 原子性

原子性就是指对数据的操作是一个独立的、不可分割的整体。是一个连续不可中断的过程，数据不会执行的一半的时候被其他线程所修改。

操作系统指令即为原子操作，但是很多操作不能通过一条指令就完成。例如：i++ 和 long 类型的运算。

#### 可见性

指当多个线程访问同一个变量时，一个线程修改了这个变量的值，其他线程能够立即看得到修改的值。

JMM (Java Memory Model) 设定每个线程都有自己的工作内存，存储着主内存中共享变量的副本。  
每次读取或写入的是工作内存中的副本值，然后在某个时间点上与主内存进行同步。  
实际中线程的工作内存对应 CPU 的缓存，主内存对应内存。

可见性可以通过 volatile 或 synchronized 的内存屏障来保证。

#### 有序性

即程序执行的顺序按照代码的先后顺序执行。

为了提高性能，编译器和处理器可能会对指令做重排序。重排序可以分为三种：

1. 编译器优化的重排序。编译器在不改变单线程程序语义（as-serial-if）的前提下，可以重新安排语句的执行顺序。
2. 指令级并行的重排序。现代处理器采用了指令级并行技术（Instruction-Level Parallelism， ILP）来将多条指令重叠执行。  
   如果不存在数据依赖性，处理器可以改变语句对应机器指令的执行顺序。
3. 内存系统的重排序。由于处理器使用缓存和读/写缓冲区，这使得加载和存储操作看上去可能是在乱序执行。

数据无相关依赖，从而进行指令重排序

```c
var a = b =  0
var x = y = 0
// Thread 1
a = 1
x = b
// Thread 2
b = 1
y = a 
```

### 保障线程安全的策略

- 首先避免共享资源的使用
- 不可变的数据，只读不写，例如 String
- 赋值或状态修改的原生原子操作，并保障对其它线程的可见性（CopyOnWriteArrayList，两个线程同时修改？）
- 将一组对象封装成一个对象，通过原子操作替换对象
- 保障取值计算并赋值的原子性
- CAS 操作，自旋
- 使资源的修改操作互斥，即同一时刻只有一个线程可以操作，并且操作结果对其它线程可见

## 同步机制

### Volatile

轻量级的同步语义，它不会引起线程上下文的切换和调度。通过内存屏障刷新写前/后缓存，禁止指令重排序，保障线程间的可见性和有序性。

基于不同硬件或底层实现，为了统一行为，volatile 是提供给开发者的保障，即保障了内存的可见性和有序性的同步语义。

volatile 会提示编译器，禁止编译重排序。

通过 Read/Write Barrier 保障内存的可见性，即通过 MESI API 同步 CPU 内核之间的缓存来实现。

通过 load fence (LoadLoadBarriers), store fence (StoreStoreBarriers), mfence (StoreLoadBarriers) 保障指令之间的有序性。

因为 java 语言的限制，无法保障数组元素的可见性。

#### 单例模式的双重锁为什么要加 volatile

```java
public class TestInstance {
  private volatile static TestInstance instance;
  
  public static TestInstance getInstance(){        //1
      if(instance == null){                        //2
          synchronized(TestInstance.class){        //3
              if(instance == null){                //4
                  instance = new TestInstance();   //5
              }
          }
      }
      return instance;                             //6
  }
}
```

在并发情况下，如果没有volatile关键字，在第5行会出现问题。instance = new TestInstance()；可以分解为3行伪代码

```c++
// a.分配内存
memory = allocate()
// b.初始化对象
ctorInstanc(memory)
// c.设置instance指向刚分配的地址
instance = memory
```

上面的代码在编译运行时，可能会出现重排序从 a-b-c 排序为 a-c-b。当A线程执行了c 导致 instance 指向了一段地址，所以B线程判断 instance 不为 null，会直接跳到第6行并返回一个未初始化的对象。

### CAS

在x86架构上，CAS被翻译为"lock cmpxchg..."。在CPU架构中依靠lock信号保证可见性并禁止重排序，lock前缀是一个特殊的信号，执行过程如下：

- 对总线和缓存上锁。
- 强制所有lock信号之前的指令，都在此之前被执行，并同步相关缓存。
- 执行lock后的指令（如cmpxchg）。
- 释放对总线和缓存上的锁。
- 强制所有lock信号之后的指令，都在此之后被执行，并同步相关缓存。

因此，lock信号虽然不是内存屏障，但具有mfence的语义（当然，还有排他性的语义）。与内存屏障相比，lock信号要额外对总线和缓存上锁，成本更高。

### Atomic

Atomic 原子类是通过自旋 + CAS 实现原子操作，内部是通过 Unsafe 类的方法实现，同时提供各类内存相关的语义，保障可见性和有序性。

#### AtomicStampedReference

CAS 操作会存在 ABBA 的问题，因此通过 Stamped 版本号来解决该问题。

#### LongAdder

因为在高度并发的场景下，多数线程的 CAS 更新都是失败的，从而降低整体效率。因此 JDK 1.8 加入 LongAdder 类应对此场景。

LongAdder 采用分段的思想降低并发冲突，内部维护 base 变量和 cells 数组。
当原子更新时，首先尝试 cas 更新 base 变量，若失败则通过当前线程的 hash 值所对应的 cells 槽，去除相应的元素进行 cas 更新。
取值时则通过 base + sum(cells) 计算得到最后的结果。因此在高并发下提高了效率，在低并发下反而增加了一些多余的开销。

### Synchronized

synchronized 可以保证方法或代码块在运行时，同一时刻只有一个线程可以进入到临界区（互斥性），同时它还保证了共享变量的内存可见性。

JDK 1.6 加入轻量级锁，偏向锁，自旋锁优化，JDK 1.7 加入自适应自旋锁。

由于管程是一种互斥资源，修改互斥资源至少需要一个CAS操作。因此，锁必然也使用了lock信号，具有mfence的语义。锁的mfence语义实现了Happens-Before关系中的监视器锁规则。

当对象的 hashCode 方法被调用，需要撤销偏向锁。

#### 偏向锁

偏向锁适用于只有一个线程获取锁的场景，当有多个线程竞争时升级为轻量级锁。

获取锁时，用 CAS 操作尝试修改对象的 mark word，把对象从匿名偏向状态偏向当前线程。而释放锁时，任保持对象的偏向状态。
因此整个过程只有一次 CAS 操作的开销。

##### 偏向锁加锁

- 检查对象的 mark word 的是否为偏向模式（101）
- 检查对象的偏向线程是否为当前线程，是则获取对象锁（可重入）
  - 如果（类）偏向模式关闭（默认开启），则尝试撤销偏向锁，利用 CAS 操作将 mark word 替换为 class 的 mark word
- 如果 epoch 不等于 class 的 epoch，则尝试重偏向
  - 失败则撤销偏向锁
- 利用 CAS 操作从匿名偏向设为偏向当前线程，成功则获取对象锁
- 否则说明存在竞争，进入 monitorenter 方法

##### 偏向锁撤销

- 遍历 jvm 的所有线程，检查偏向的线程是否存活，若不存在则直接撤销偏向锁。
  - 撤销至匿名偏向状态
  - 否则设置为无锁状态
- 检查偏向的线程是否还在同步块中，存在则升级为**轻量级锁**
- 撤销偏向锁

#### 轻量级锁（Lightweight Locking）

轻量级锁适用于线程交替获取锁的场景

在栈帧上分配一个 Lock Record 对象，并存储锁对象的引用和 mark word。利用 CAS 操作将对象头的 mark word 替换为 Lock Record 的地址。

CAS 失败则升级为重量级锁，无自旋重试操作。

- 如果锁对象不是偏向模式或者已经偏向其它线程
- 构造一个无锁状态的 Displaced Mark Word，并将 Lock Record 的 lock 指向它
- 利用 CAS 将对象头的 mark word 替换为指向 Lock Record 的指针
  - CAS 失败则检查是不是锁重入，是这设置 displaced header 为 null 表示重入
  - 否则进入 monitorenter 方法

monitorenter 》 slow_enter

- 如果是无锁状态，设置 Displaced Mark Word，并用 CAS 替换对象头为 Lock Record 的地址
- 否则，检查对象锁（轻量级锁）的所有者是否为当前线程，是则为重入锁
- 否则，说明存在竞争锁，直接膨胀为重量级锁。**无自旋重试**

#### 自适应自旋锁

当对象膨化至重量级锁时，并且对象尝试加锁，首先会通过 CAS + 自旋操作尝试获取锁，若失败再使用重量级锁的机制。

优势是避免用户态和内核态的切换带来的开销，缺点是当 CPU 空转浪费 CPU 资源。

JDK 1.6 可以通过 -XX:UseSpinning 开启自旋锁，默认开启。-XX:PreBlockSpin=10 调节自旋次数。

JDK 1.7 加入自适应自旋锁，根据 JVM 收集统计信息动态调节自旋时常。

1. 如果平均负载小于CPUs 则一直自旋
2. 如果有超过(CPUs/2)个线程正在自旋，则后来线程直接阻塞
3. 如果正在自旋的线程发现Owner发生了变化则延迟自旋时间（自旋计数）或进入阻塞
4. 如果CPU处于节电模式则停止自旋
5. 自旋时间的最坏情况是CPU的存储延迟（CPU A存储了一个数据，到CPU B得知这个数据直接的时间差）
6. 自旋时会适当放弃线程优先级之间的差异

#### 重量级锁

synchronized 是由 JVM 实现的同步机制，通过监视器锁（monitor）来实现的，依赖于底层的操作系统的 Mutex Lock 来实现的。

当一个线程尝试获得锁时，如果该锁已经被占用，则会将该线程封装成一个ObjectWaiter对象插入到cxq的队列的队首，然后调用park函数挂起当前线程。
在linux系统上，park函数底层调用的是gclib库的pthread_cond_wait，JDK的ReentrantLock底层也是用该方法挂起线程的。

当线程释放锁时，会从cxq或EntryList中挑选一个线程唤醒，被选中的线程叫做Heir presumptive即假定继承人，就是图中的Ready Thread，假定继承人被唤醒后会尝试获得锁，
但synchronized是非公平的，所以假定继承人不一定能获得锁（这也是它叫"假定"继承人的原因）。

如果线程获得锁后调用Object#wait方法，则会将线程加入到WaitSet中，当被Object#notify唤醒后，会将线程从WaitSet移动到cxq或EntryList中去。需要注意的是，当调用一个锁对象的wait或notify方法时，如当前锁的状态是偏向锁或轻量级锁则会先膨胀成重量级锁。

```c++
ObjectMonitor() {
    _header       = NULL;
    _count        = 0;
    _waiters      = 0,
    _recursions   = 0;  // 线程重入次数
    _object       = NULL;  // 存储Monitor对象
    _owner        = NULL;  // 持有当前线程的owner
    _WaitSet      = NULL;  // wait状态的线程列表
    _WaitSetLock  = 0 ;
    _Responsible  = NULL ;
    _succ         = NULL ;
    _cxq          = NULL ;  // 单向列表
    FreeNext      = NULL ;
    _EntryList    = NULL ;  // 处于等待锁状态block状态的线程列表
    _SpinFreq     = 0 ;
    _SpinClock    = 0 ;
    OwnerIsThread = 0 ;
    _previous_owner_tid = 0;
  }
```

#### Synchronized和ReentrantLock的区别

Synchronized是JVM层次的锁实现，ReentrantLock是JDK层次的锁实现；
Synchronized的锁状态是无法在代码中直接判断的，但是ReentrantLock可以通过ReentrantLock#isLocked判断；
Synchronized是非公平锁，ReentrantLock是可以是公平也可以是非公平的；
Synchronized是不可以被中断的，而ReentrantLock#lockInterruptibly方法是可以被中断的；
在发生异常时Synchronized会自动释放锁（由javac编译时自动实现），而ReentrantLock需要开发者在finally块中显示释放锁；
ReentrantLock获取锁的形式有多种：如立即返回是否成功的tryLock(),以及等待指定时长的获取，更加灵活；
Synchronized在特定的情况下对于已经在等待的线程是后来的线程先获得锁（上文有说），而ReentrantLock对于已经在等待的线程一定是先来的线程先获得锁；

#### 线程同步机制

- Object.wait()/Object.notify()
- Condition.await()/single()
- Semaphore
- CountDownLatch
- CyclicBarrier

## 同步器

### AbstractQueuedSynchronizer

抽象的队列式同步器，简称 AQS。

AQS定义两种资源共享方式：Exclusive 和 Share。

不同的自定义同步器争用共享资源的方式也不同。**自定义同步器在实现时只需要实现共享资源state的获取与释放方式即可**，至于具体线程等待队列的维护（如获取资源失败入队/唤醒出队等），AQS 已经实现好了。自定义同步器实现时主要实现以下几种方法：

```kotlin
// 共享资源
volatile val state: Int
volatile val head: Node
volatile val tail: Node

// 独占方式。尝试获取资源
fun tryAcquire(acquires: Int): Boolean
// 独占方式。尝试释放资源
fun tryRelease(releases: Int): Boolean
// 共享方式。尝试获取资源。负数表示失败；0表示成功，但没有剩余可用资源；正数表示成功，且有剩余资源
fun tryAcquireShared(acquires: Int): Int
// 共享方式。尝试释放资源，如果释放后允许唤醒后续等待结点返回true，否则返回false
fun tryReleaseShared(releases: Int): Boolean
// 当前线程是否正在独占资源。只有用到 condition 才需要去实现它
fun isHeldExclusively(): Boolean

// 表示等待获取资源线程的双向链表
class Node {
  volatile prev: Node
  volatile next: Node
  volatile waitStatus: Int = 0
  volatile thread: Thread
}
```

AQS 使用 state 来表示共享资源，由同步器实现对共享资源的获取和释放，state 的含义由同步器定义。
通过内置的 FIFO 来完成获取资源线程的排队工作，该队列由 Node 节点组成，表示等待获取资源的线程。每个节点维护 prev 和 next 的引用，分别指向自己的前序和后序节点。AQS 维护着 head 和 tail 的指针，分别指向队列的头部和尾部。即一个**双端双向的链表**

![AQS 队列](https://images2015.cnblogs.com/blog/1024555/201707/1024555-20170717114859238-311670115.png)

当线程获取资源失败（执行 tryAcquire 失败），会被构造成一个节点加入 CLH 队列中，同时当前线程会被阻塞在队列中（LockSupport.park，底层通过操作系统的 Mutex 实现）。当持有共享资源的线程释放资源时，会唤醒后序节点，然后此节点继续加入到共享资源的竞争中。

---

- AQS 的 head 节点会处于什么状态？
  - 当只有一个线程或者两个线程交替执行的时候，因为没有并发竞争，所以通过 tryAcquire 直接获取锁，head 为 null
  - 当两个线程并发竞争时，队列中的节点的前序节点就是头节点，尝试获取成功则设置 head 为当前节点，状态为 0。否则，会将前序节点状态设为 SIGNEL，阻塞当前节点
  - 如果存在更多的线程加入竞争队列，会依次设置前序节点为 SIGNEL 状态，阻塞自己并等待通知
  - 当持有资源的线程释放资源时，会将 head 节点的状态设置为 0，并通知队列中的下一个非 CANCELLED Successor 节点 unpark，当 Successor 尝试获取资源成功后，设置自己为 head 节点，如果还存在后续节点，状态为 SIGNEL，否则为 0
- 当两个线程并发竞争互斥锁时，是否一直处于自旋等待状态？
  否，tryAcquire 失败后会阻塞等待。
- AQS.acquire 和 Condition.await 均使用 LockSupport.park 实现，线程如何区分 BLOCKING 和 WAITING 状态？
  ReentrantLock.lock() 和 Condition.await() 是通过 LockSupport.park() 实现的，状态均为 WAITING
- 用 jstat 检查 ReentrantLock.lock() 阻塞的线程时，线程持有的锁对象是什么？
  状态为 WAITING (parking)，- parking to wait for  <0x00000007118eb080>

### 死锁

1. lock 未释放
2. Object.await()/Condition.await() 未唤醒
3. 线程互相持有对方的锁并等待对方
4. 死循环

#### 死锁的条件

1. 互斥性
2. 占有并等待：一个线程应占有至少一个资源，并等待另一个资源，而该资源为其他线程所占有。
3. 非抢占：资源不能被抢占，即资源只能被线程在完成任务后自愿释放。
4. 循环等待条件

#### 死锁的检测

1. 每个线程、每个资源制定唯一编号
2. 设定一张资源分配表，记录各线程与占用资源之间的关系
3. 设置一张线程等待表，记录各线程与要申请资源之间的关系
4. 检索线程等代表和

## 并发容器

### ConcurrentHashMap

#### JDK 1.6

HashMap 在并发环境下最典型线程安全的问题是，在并发扩容进行 rehash 操作时，因为采用头插法的方式构建新链表，因此可能导致环行链表出现，当再次访问或修改 HashMap 时，会陷入死循环。

ConcurrentHashMap 是允许**并发访问**的线程安全的 HashMap，其关键在于使用了锁分段技术，它通过将 hash table 拆分为 n 段（Segment），提供相互独立的锁提供并发访问的能力，默认的并发级别 n = 16。

##### 定义

```kotlin
class ConcurrentHashMap<K, V>: AbstractMap<K, V>, ConcurrentMap<K, V> {

    // 用于定位段的掩码
    val segmentMask: Int
    // 用于定位段的偏移量
    val segmentShift: Int
    // The segments, each of which is a specialized hash table
    val segments: Array<Segment<K, V>>

    class Segment<K, V>: ReentrantLock {

        volatile var count: Int
        volatile var modCount: Int
        var threshold: Int
        val loadFactor: Float
        volatile var table: HashEntry<K, V>
    }

    class HashEntry<K, V>(
      val key: K,
      val hash: Int,
      // 头插法, 不可变
      val next: HashEntry<K, V>,
      volatile var value: V
    )
}
```

### CopyOnWriteArrayList

### BlockingQueue

## 实践、排查

### 优化

并发度 = 并行 / (并行 + 串行)

- 减少锁的时间，互斥范围越大程序的并发度越低
- 拆分锁，提高并发度
- 双头队列锁，列头入队，列为出队
- 锁粗化，将循环内的锁放到循坏外，避免频繁的切换
- 读写锁
- 读写分离，写时复制
- 锁消除（Lock Elimination）：JVM 即时编译器在运行时，通过逃逸分析，如果判断一段代码中，堆上所有数据不会逃逸出去被其它线程访问到，就可以去除这些锁。

---

- 工作中常用的锁有哪些
- 关于线程安全，常用的锁介绍一下
- 熟悉 JDK 的哪些锁？它们的优缺点和区别？
  volatile, Synchronized, Lock, ReadWriteLock
  volatile 作为轻量级的同步机制，对 volatile 修饰的变量的操作保障了可见性和有序性。优点是没有上下文的切换，速度非常快。缺点是无法保障多个操作的原子性。
  synchronized 是由 JVM 实现的同步机制，底层调用操作系统的互斥锁 Metux 实现的。它通过线程互斥的行为保障了临界区的原子性，临界区边界的内存屏障保障可见性，以及 happens-before 原则的有序性。缺点是会引起用户态和内核态的转换，开销比较大。
  Lock 是由 JDK 提供，基于 AQS 框架实现的同步器：可重入的非公平锁，优点使用起来比较灵活，并且可以设置超时或响应中断请求。缺点和 synchronized 相同，并且若没有正确释放资源容易造成死锁。
- Synchronized 锁的使用场景
  用于多线程并发同步，保障临界区操作的原子性，内存可见性。适用于并发竞争度高，临界区执行时间长的场景。
- 你阅读过 Java 虚拟机偏向锁的源码，能否说明下偏向锁怎样一步步升级的？
  虚拟机执行到 monitorenter 指令会 case 到相应代码块。
  检查对象的 Mark Word 是否为偏向模式，即后三个标志位为 101
  检查对象的偏向线程是否为当前线程，偏向锁为可重入锁
  检查偏向模式是否关闭，关闭则尝试撤销偏向锁
  检查 epoch 是否不同，尝试重偏向，失败则进行锁升级
  否则说明对象为匿名偏向或已偏向其它线程
  用 CAS 操作尝试将对象的 mark word 替换为构造 mark word，成功则获取锁
  否则进行锁升级，偏向锁适用于无竞争的场景
  轻量级锁会在当前线程的栈帧下创建 Lock Record，LockRecord 会把 Mark Word 的信息拷贝进去，且有个 Owner 指针指向加锁的对象。
  通过 CAS 操作尝试将对象的 mark word 替换为 lock record 的地址，成功则获取锁
  失败则进入 monitorentry 方法，膨胀为重量级锁
  重量锁是通过 ObjectMonitor 实现的，依赖系统底层的 Mutex Lock 实现
- 轻量级锁自旋多少次会失败膨胀到重量级锁？
  轻量级锁失败则会膨胀为重量级锁，在重量级锁加锁时会尝试通过自旋锁的方式，
- 轻量级锁升级为重量级锁的条件？等待时间多久？
  JDK1.6 使用 -XX:PreBlockSpin 设置自旋锁等待次数，默认为10
  JDK1.7 加入自适应自旋锁，由虚拟机自动调整。如果自旋线程之前刚刚获得过锁，则虚拟机会认为这次自旋也很有可能会成功，进而延长自旋时间。
  反之，如果某个锁自旋很少获得过成功，则之后获取锁的时候可能省略掉自旋过程，避免浪费处理器资源。
- 多线程的内存屏障机制你能简单说一下嘛？
  内存屏障可以禁止特定类型处理器的重排序，load、write、fence
- 禁止指令重排序有哪些方法可以做到？
  volatile、synchronized、Unsafe.fullFence()，final、cas
- volatile 有什么作用？
- 能说一下 volatile 关键字嘛，它跟 Atomic 有什么不同
  轻量级同步语义，通过内存屏障刷新写前/后缓存，禁止指令重排序。保障了可见性和有序性
  Atomic 是原子操作，内部通过 volatile 关键字修饰和 CAS + 自旋实现，即保障了可见性和有序性，同时保障了原子性
- new 一个实例的过程在 JVM 中是怎么操作的？它是线程安全的嘛？
  不安全，new 非原子性操作，分为分配内存，初始化内存，赋值到变量三个操作
- 如何做到 new 对象的线程安全
  synchronized + volatile
- 比如单例模式，使用或不用 volatile 有什么区别吗？
- volatile 为何要结合 synchronized 保障可见性？因为 synchronized 已经保障了可见性
- volatile 可以使一个非原子操作跟原子操作码？比如 long 类型得加持
  如果使用volatile修饰long和double，那么其读写都是原子操作
- 状态的变更，有具体的场景吗？
- volatile 更适合使用在什么场景？
  对变量的写操作不依赖于当前值。
  使用一个布尔状态标志，用于指示发生了一个重要的一次性事件，例如完成初始化或请求停机。
  单例模式的双重检查锁定，禁止指令重排序而看到不完全构造的对象。
  独立观察，由一个线程维护数值，其它线程读取这个变量。
  开销较低的读-写策略，适用于读多写少。写操作由 synchronized 保障原子性，读操作由 volatile 保障可见性，读不加锁。
- CAS 无锁机制的好处和坏处？
  减少线程上下文的切换，用户态和内核态的转换。
  读不加锁，读写不冲突
  高并发下带来额外的 CPU 开销降低总体效率
  存在 ABA 的问题

- 并发包下得其它工具都用过哪些？
  ReentrantLock, ReadWriteLock, Semaphore, CountDownLatch, Atomic, CopyOnWriteArrayList, LinkedBlockingQueue, Executor
- 介绍一下 AQS 框架
  AQS 作为抽象的队列式同步器，是许多同步器实现的基础框架，内部维护着 state 和双向双端链分别表示共享资源和获取资源失败的线程等待队列。
  由子类实现**尝试获取/释放**的独占/共享资源的抽象方法，当线程尝试获取资源失败后，由 AQS 负责阻塞或唤醒当前线程，并且维护线程等待队列。
- 读写锁的原理是什么？
  内部分别实现了读锁和写锁，通过 Sync 控制独占/共享资源的获取和释放。state 的高16位和低16位分别表示独占资源和共享资源。尝试获取写锁时，如果资源被独占且不等于当前线程或被共享，则获取失败。尝试获取读锁时，如果资源被独占则获取失败。
- 我们可以通过 Unsafe 可以做哪些事情？
- Unsafe 跟内存管理相关的方法有哪些？
  内存的分配、释放、拷贝、交换，类加载，对象的分配，获取字段的偏移量，对不同内存语义的对象字段的访问和修改，CAS 操作，设置 load/store/full 栅栏，线程的 park/unpark，以及大小端机的内存操作
- Unsafe 我们怎么去获取？还有别的方式嘛？
  通过 Unsafe.getUnsafe()，或使用反射获取。
- Java Lock 接口的实现原理？
- Atomic 的源码也看过吧？AtomicInteger 的默认值怎么来的？ValueObject 的值是怎么来的？
  默认值通过构造函数赋值，初始值为 0。通过 Unsafe 获取 value 的偏移量，实现 CAS 操作。
- ConcurrentHashMap 底层实现原理能说一下嘛？
  ConcurrentHashMap 是 HashMap 的并发实现，结合 volatile, spin cas, synchronized 实现的线程安全。
- 有看过内部的源代码嘛？能简单说一下 put 操作的流程
  先判断元素表是否为空，为空则初始化
  然后计算 key 的 hash，并获取桶的索引，并获取桶
  如果桶为空，则通过 cas 操作更新指定索引的桶
  否则对桶加锁，追加链表元素并记录长度，或追加到红黑树中。
  更新后判断链表长度，如果大于阈值则转换为红黑树
  最后增加实际元素数量，如果超过容量则进行扩容。
- HashMap 存在什么线程安全的问题？
  当两个线程并发访问相同 hash 桶，即在链表追加元素时，会造成元素丢失的问题
  当扩容并发 rehash 时，JDK 1.7 使用了头插法，因此容易形成一个环形链表，导致 get 时死循环。
- ConcurrentHashMap 和 SynchronizedHashMap 有什么区别？分别用于什么场景
  ConcurrentHashMap 是使用了多种优化手段的并发容器。而 SynchronizedMap 封装了 HashMap 并且加了同步锁。都适用于并发场景，前者性能优于后者。
- ConcurrentHashMap 在 1.8 前后的实现区别？
  1.7 之前 ConcurrentHashMap 采用分段锁优化机制，Segment 继承 ReentrantLock 提供并发安全的访问，分为 16个段降低并发冲突。
  1.7 取消了 segments 字段，采用了 `volatile HashEntry<K, V>[] table` 保存数据，使用 synchroinzed(f) 对每个元素加锁，进一步减少并发冲突的概率。
  1.8 加入了红黑树优化当链表元素过长时访问比较慢的问题。
- ConcurrentHashMap 底层用的分段锁是哪个版本？之后使用的什么方式？
  1.7 之前，之后用 synchronized + cas 保障线程安全
- CopyOnWriteArrayList、ConcurrentHashMap 在底层是怎样保障线程安全的？
  CopyOnWriteArrayList 利用写时复制技术解决并发访问冲突。当修改元素时，clone 原数组进行操作，操作完毕后更新数组引用。
- 说一下并发容器，像 ConncurrentHashMap，CopyOnWriteArrayList 在什么场景下使用呢？（说的还不错，好的表达）
  ConcurrentHashMap 适用于并发访问 Map 数据结构的对象，
  CopyOnWriteArrayList 适用于并发访问数组，因其读不加锁，读写不冲突，适用于读多写少的场景，缺点是会存在读写不一致的情况。
- CopyOnWriteArrayList 有什么缺点？
  读写不一致，读到的不一定时最新的值
  写多的场景下会频繁创建垃圾对象
- 除了以上两种方式外还有没有其它的保障线程安全的方式？
  不可变对象、cas、lock、同步器、并发容器

- 有没有遇到过死锁的问题？怎么排查或解决的？
  通过 jstack 查看当前所有线程的状态，持有的锁，以及代码堆栈。
- 什么情况下会导致死锁的场景？除了循环依赖还有别的吗？
  锁未释放
  进入等待状态后未唤醒
  互相锁定资源并且等待资源
  死循环
- 有什么解决方案嘛？（除了同时申请资源的方式还有其它么，超时）
  使用 synchronized 或 try-final 确保锁正确释放，或加入超时避免死锁
  实现死锁检测，并通过中断唤醒线程
  互相锁定资源并等待：
  - 获取所有锁后再执行代码
  - 按相同的顺序获取锁
  - 合并锁
  - 减少锁的粒度（占用时间）
- 在 JVM 中有什么方式提升加锁的效率？
  偏向锁、轻量级锁、自适应自旋锁
- 分布式锁了解吗？
- 有用过分布式锁么，怎么实现的嘛
- 有没有了解过其它开源框架提供的锁？（Redisson，zk）
  RedLock
- zk 的锁有尝试过吗？
- zk 的节点有哪些类型？
- ConcurrentHashMap 如果要放入 100个对象，初始化的容量应该指定多大？（128 是错误的答案）
  指定初始容量：100 / 0.75 + 1 = 134.33 < 256
  指定初始对象：100 + (100 >>> 1) + 1 = 151 < 256
- 针对线程池大小有做过具体的优化吗？
  如果请求频繁则调整合适的核型线程数量，避免频繁创建/销毁线程。
  如果请求频率低且并发量较少可以减低核型线程，节约资源。
  如果任务属于 CPU 密集性运算，则根据 CPU 核心数量设置最大线程量。
  如果任务属于 IO 密集型运算，可是适当提高最大线程，充分利用 CPU 减低时间。
- 多线程中如何实现一个计数器？
  如果只有一个线程负责维护计数器可以使用 volatile 修饰
  如果并发较低使用 AtomicLong 原子更新实现，如果并发较高使用 LongAdder 分段锁优化实现。
- 如果让你做内存计数器，会用哪种锁？例如 metric 统计每个 URL 的 RT
