# Java

## 基础

### 引用

- 强引用：如果对象具有强引用，不会被 GC 回收
- 软引用：内存不足时被 GC 回收
- 弱引用：发生 GC 即回收
- 虚引用：发生 GC 时放入引用队列中

## 集合

### ArrayList

读操作时间复杂度 O(1)，写操作 O(n)，适用于读多写少的随机访问场景。
默认初始容量为 10，每次扩容 50%。
迭代器分别为 `ArrayList<E>.Itr` 和 `ArrayList<E>.ListItr`
通过 modCount 变量保证并发下的 fail-fast 机制。

```java
public class ArrayList<E> extends AbstractList<E> implements List<E> {
  private static final int DEFAULT_CAPACITY = 10;
  transient Object[] elementData;
  private int size;

  int modCount

  private int newCapacity(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1)
    return newCapacity;
  }

  public Iterator<E> iterator() {
      return new Itr();
  }

  private class Itr implements Iterator<E> {
      int cursor;       // index of next element to return
      int lastRet = -1; // index of last element returned; -1 if no such
      int expectedModCount = modCount;

      public boolean hasNext() {
        return cursor != size;
      }
      
      final void checkForComodification() {
        if (modCount != expectedModCount)
          throw new ConcurrentModificationException();
      }

      public void remove() {
        ArrayList.this.remove(lastRet);
        cursor = lastRet;
        expectedModCount = modCount;
      }
  }
}
```

---

- ArrayList 如果要移除一个元素能通过 foreach 来移除吗？
  在迭代器中不能添加或者移除元素，但可以通过 Itr.remove 移除元素。
- ArrayList 的子类迭代器是哪一个？
  `ArrayList<E>.Itr<E>`

### LinkedList

双向链表结构，写操作 O(1)，读操作 O(n)。

### HashMap

```java
// 默认初始容量：16，容量必须为 2的指数倍
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; 
static final int MAXIMUM_CAPACITY = 1 << 30;
// 负载因子（时间和空间效率的权衡）
static final float DEFAULT_LOAD_FACTOR = 0.75f;
// 红黑树相关的阈值
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;
static final int MIN_TREEIFY_CAPACITY = 64;
// 元素存储
transient Node<K,V>[] table;
transient int size;
// 负载因子
final float loadFactor = DEFAULT_LOAD_FACTOR
// 扩容阈值
int threshold = loadFactor * size
```

---

- HashMap 的实现原理？
  HashMap 采用“Hash算法”来决定每个元素的存储位置，通过计算 key 的 hashCode() & (capacity - 1) 来确定元素的索引位置。
- 初始容量是32么？
  初始容量 16
- Hash 冲突怎么解决？
  当 Hash 冲突时采用链表的方式存储元素
- HashMap 是怎么扩容的？
  如果实际元素大于扩容阈值时会将容量扩大一倍，将所有元素重新计算hash索引位置分配到新表中。
- 扩容为什么是翻两倍？
  计算元素索引位置是通过 & 运算符来执行取余运算的

## 并发集合

### CopyOnWriteArrayList

适用于读多写少的高并发场景，通过写时复制技术和 volatile 保障线程安全性。

缺点是会频繁分配内存，无法保证读写一致性。

## 多线程

### 线程安全

当多个线程同时访问一个对象时，调用这个对象的行为都可以获得正确的结果，那就称这个对象是线程安全的。

#### 线程安全特性

- 原子性：要么全部执行并且执行的过程不会被任何因素打断，要么就都不执行。
- 可见性：指当多个线程访问同一个变量时，一个线程修改了这个变量的值，其他线程能够立即看得到修改的值。
- 有序性：即程序执行的顺序按照代码的先后顺序执行。

##### 原子性

原子性就是指对数据的操作是一个独立的、不可分割的整体。是一个连续不可中断的过程，数据不会执行的一半的时候被其他线程所修改。

操作系统指令即为原子操作，但是很多操作不能通过一条指令就完成。例如：i++ 和 long 类型的运算。

##### 可见性

指当多个线程访问同一个变量时，一个线程修改了这个变量的值，其他线程能够立即看得到修改的值。

JMM (Java Memory Model) 设定每个线程都有自己的工作内存，存储着主内存中共享变量的副本。  
每次读取或写入的是工作内存中的副本值，然后在某个时间点上与主内存进行同步。  
实际中线程的工作内存对应 CPU 的缓存，主内存对应内存。

可见性可以通过 volatile 或 synchronized 的内存屏障来保证。

##### 有序性

即程序执行的顺序按照代码的先后顺序执行。

为了提高性能，编译器和处理器可能会对指令做重排序。重排序可以分为三种：

1. 编译器优化的重排序。编译器在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序。
2. 指令级并行的重排序。现代处理器采用了指令级并行技术（Instruction-Level Parallelism， ILP）来将多条指令重叠执行。  
   如果不存在数据依赖性，处理器可以改变语句对应机器指令的执行顺序。
3. 内存系统的重排序。由于处理器使用缓存和读/写缓冲区，这使得加载和存储操作看上去可能是在乱序执行。

#### 保障线程安全的策略

- 首先避免共享资源的使用
- 不可变的数据，只读不写，例如 String
- 赋值或状态修改的原生原子操作，并保障对其它线程的可见性（CopyOnWriteArrayList，两个线程同时修改？）
- 保障取值计算并赋值的原子性
- CAS 操作，自旋（一定失败？）
- 使资源的修改操作互斥，即同一时刻只有一个线程可以操作，并且操作结果对其它线程可见

#### 线程同步机制

- Object.wait()/Object.notify()
- Condition.await()/single()
- Semaphore
- CountDownLatch
- CycleBrirrne

### 同步机制

#### Synchronized

synchronized 可以保证方法或代码块在运行时，同一时刻只有一个线程可以进入到临界区（互斥性），同时它还保证了共享变量的内存可见性。

synchronized 是由 JVM 实现的同步机制，通过监视器锁（monitor）来实现的，依赖于底层的操作系统的 Mutex Lock 来实现的。

非公平锁
可重入锁

synchronized statements 是通过 monitorenter 和 monitorexit 指令实现。
synchronized methods 是通过方法上的修饰符 ACC_SYNCHRONIZED 隐式调用 monitorenter 和 monitorexit 实现。

> Each object is associated with a monitor. A monitor is locked if and only if it has an owner.  
> The thread that executes monitorenter attempts to gain ownership of the monitor associated with objectref, as follows:  
>
> - If the entry count of the monitor associated with objectref is zero, the thread enters the monitor and sets its entry count to one. The thread is then the owner of the monitor.
> - If the thread already owns the monitor associated with objectref, it reenters the monitor, incrementing its entry count.
> - If another thread already owns the monitor associated with objectref, the thread blocks until the monitor's entry count is zero, then tries again to gain ownership.

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

### volatile

轻量级的同步机制，它不会引起线程上下文的切换和调度。

它保障了：

- 可见性
- 有序性

##### 单例模式的双重锁为什么要加volatile

```java
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

### 异步

异步是非阻塞的编程模式
异步和同步是相对的，同步就是顺序执行，执行完一个再执行下一个，需要等待、协调运行。异步就是彼此独立,在等待某事件的过程中继续做自己的事，不需要等待这一事件完成后再工作。线程就是实现异步的一个方式。异步是让调用方法的主线程不需要同步等待另一线程的完成，从而可以让主线程干其它的事情。异步和多线程并不是一个同等关系,异步是最终目的,多线程只是我们实现异步的一种手段。

#### 并发（Concurrent）和并行（Parallel）

在单处理器中多道程序设计系统中，进程被交替执行，表现出一种并发的外部特征；在多处理器系统中，进程不仅可以交替执行，而且可以重叠执行。在多处理器上的程序才可实现并行处理。从而可知，并行是针对多处理器而言的。并行是同时发生的多个并发事件，具有并发的含义，但并发不一定并行，也亦是说并发事件之间不一定要同一时刻发生。

![并发（Concurrent）和并行（Parallel）](https://upload-images.jianshu.io/upload_images/1286371-417c4eff57c89998?imageMogr2/auto-orient/strip|imageView2/2/w/473/format/webp)

### 锁

#### 死锁

##### 死锁的条件

1. 互斥性
2. 占有并等待：一个线程应占有至少一个资源，并等待另一个资源，而该资源为其他线程所占有。
3. 非抢占：资源不能被抢占，即资源只能被线程在完成任务后自愿释放。
4. 循环等待条件

##### 死锁的检测

1. 每个线程、每个资源制定唯一编号
2. 设定一张资源分配表，记录各线程与占用资源之间的关系
3. 设置一张线程等待表，记录各线程与要申请资源之间的关系
4. 检索线程等代表和

### 动态代理

#### JDK Proxy

```kotlin
interface Subject {
    fun doSomething()
}

class RealSubject : Subject {
    override fun doSomething() {
        println("RealSubject do something")
    }
}

class DynamicProxy(private val target: Any) : InvocationHandler {
    fun proxy(): Any {
        return Proxy.newProxyInstance(target.javaClass.classLoader, target.javaClass.interfaces, this)
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
        return method.invoke(target, args)
    }
}

class $Proxy0(handler: InvocationHandler) : Proxy(handler), Subject {
    companion object {
        private val m0: Method = Class.forName("Subject").getMethod("doSomething")
    }

    override fun doSomething(): Any {
        try {
            return super.h.invoke(this, m0, null)
        } catch (ex: RuntimeException) {
            throw ex
        } catch (ex: Throwable) {
            throw UndeclaredThrowableException(ex)
        }
    }
}
```
