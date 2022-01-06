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
