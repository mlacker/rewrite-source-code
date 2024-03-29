# Java

## 基础

### String

字符串为什么被定义为 final 类型

1. 可以共享字符串常量池
2. 数据安全
3. 线程安全
4. 高效 hashCode

### final关键字

如果一个实例的字段被声明为final，则JVM会在初始化final变量后插入一个 sfence。

类的final字段在`<clinit>()`方法中初始化，其可见性由JVM的类加载过程保证。

final字段的初始化在`<init>()`方法中完成。sfence禁用了sfence前后对store的重排序，且保证final字段初始化之前（include）的内存更新都是可见的。

### 引用

- 强引用：如果对象具有强引用，不会被 GC 回收
- 弱引用：发生 GC 即回收
- 软引用：内存不足时被 GC 回收
- 虚引用：发生 GC 时放入引用队列中

## AQS

Abstract Queued Synchronizer **抽象队列式同步器**框架，是 JDK 中可重入锁、读写锁和各类同步器实现的核心依赖。
由主类实现对共享资源的获取和释放，内部类继承 AQS 负责当发生资源竞争时，阻塞（Unsafe.park）当前线程并加入到队列中等待，当资源释放时通知队首的线程，重新竞争资源。
AQS 是由 volatile 修饰的 state 和双向双端的链表组成。state 表示共享资源，由实现类通过 CAS 操作更新，而链表则表示等待唤醒的线程队列。
线程的阻塞/唤醒最终会调用 Unsafe.park()/unpark() 方法，底层是操作系统提供的的互斥量 mutex 实现阻塞/唤醒线程。

## Unsafe

Unsafe 提供执行底层，不安全的一系列操作。包括：

- 内存的分配、释放、拷贝、交换
- park / unpark 操作，阻塞当前线程
- CAS 操作，该操作含有 volatile 的 read and write 内存语义
- loadFence, 确保 loads 先于 fence 之后的 loads and stores 不会被重排序，LoadLoad + LoadStore 屏障
- storeFence, 确保 loads and stores 先于 fence 之后的 stores 不会被重排序，StoreStore + LoadStore 屏障
- fullFence, 确保 loads and stores 先于 fence 之后的 loads and stores，隐式效果为同时使用 loadFence, storeFence 和 StoreLoad 屏障
- 获取字段相对对象地址的偏移量
- 类加载，分配类实例对象
- 通过偏移量访问或修改对象的数据
- 获取/存储值到指定对象的偏移量中，该操作含有 volatile 的 load / write 内存语义
- 大小端机的二进制操作

## 集合

### ArrayList

访问元素、修改元素、追加元素和移除最后一个元素的时间复杂度均是 O(1)，插入元素或移除元素的时间复杂度 O(n)，适用于随机访问、读多写少的场景。
非线程安全，并发添加和移除元素会导致异常。
默认初始容量为 10，每次扩容 50%。
迭代器分别为 `ArrayList<E>.Itr` 和 `ArrayList<E>.ListItr`（双向迭代）
通过 modCount 变量保证并发下的 fail-fast 机制。

```java
public class ArrayList<E> extends AbstractList<E> implements List<E> {
  private static final int DEFAULT_CAPACITY = 10;
  transient Object[] elementData;
  private int size;

  int modCount;

  private int newCapacity(int minCapacity) {
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
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

可用于实现 Queue, Duque, Stack, LRU 队列。

需要额外维护指针的开销，但不需要预分配空间，适用于修改频繁的场景。

### HashMap

HashMap 底层是基于数组 + 链表组成的，在 JDK 1.8 中如果链表长度超过 8，则会转换为红黑树。

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

### LinkedHashMap

### TreeHashMap

## 并发集合

### CopyOnWriteArrayList

通过写时复制技术和 volatile 保障线程安全性，适用于读多写少的高并发场景。

频繁的修改数组对 GC 造成较大的压力，并发访问时无法保证读写一致性。

### 动态代理

静态代理在程序运行之前被代理类就需要存在
动态代理的被代理类可以是在运行时生存的，通过 InvocationHandler 接口实现动态调用，Proxy 创建代理类的实例
JDK 代理如果目标代理类没有实现的接口无法代理，则需要使用 CGLIB 动态代理
CGLIB 通过 Enhancer 创建代理类实例，目标类作为父类

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
