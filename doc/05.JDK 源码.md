# JDK 源码分析

> **前言：JDK & STL 源码分析计划**

为了学好数据结构以及相关算法，同时也为了更好地理解 JDK 的底层实现，计划对 JDK 集合类的源码做一个系统的阅读分析。

**总体思路**

1. 学习基本的数据结构认识。兵马未动粮草先行。先把基础理论搞清楚。

   1. 学Java的，可以从下面两本书中选一本：
      1. [数据结构与算法分析](https://book.douban.com/subject/26745780/) — 这本书的优点在于和 Java JDK 的集合类很贴近。
      2. [算法（第4版）](https://book.douban.com/subject/19952400/) — 这本书胜在图很多。
   2. 学 C/C++ 的，可以看下面这套书：
      1. [算法：C语言实现 (第1～4部分)](https://book.douban.com/subject/4065258/)
      2. [算法：C语言实现 （第5部分）](https://book.douban.com/subject/4191525/)

2. 自己实现一遍基本的数据结构；

3. 阅读 JDK 或 STL 源码，做学习笔记。

   > **更多操作对比一下自己的实现和这些经典代码的实现，总结自己差距，提高自己的编码能力。**

   1. [STL源码剖析 ](https://book.douban.com/subject/1110934/) — 阅读源码时，建议参考一下本书的内容。
   2. 建议把网上的源码分析笔记都看一看，取长补短，补充自己的分析。
   3. 建议把网上相关面试题也看一看，检验自己的学习成果。

4. 相关联的 LeetCode 上的题都刷掉。

>   还有两个想法：
>
> 1. 可以把 Redis 的实现也过一下，Redis 实现也有很多不错的思路。毕竟 Redis 是目前最常用的缓存解决方案。
> 2. Java 中有很多针对集合类做扩展的库，可以一并学了，这样就能更清楚了解 Java JDK 实现的不足，开阔自己的眼界：
>    1. [google/guava: Google core libraries for Java](https://github.com/google/guava)
>    2. [Apache Commons Collections](https://commons.apache.org/proper/commons-collections/)
>    3. [Eclipse Collections - Features you want with the collections you need.](https://www.eclipse.org/collections/)

> **_JDK 集合类_**

- **Base + Iterator**

  代码总行数： 103 + 135 + 302 + 195 + 838 + 127 + 734 + 480 = 2914 行，预计 5 个小时。

  ```ABAP
  java.lang.Iterable
  
  java.util.Iterator
  
  java.util.PrimitiveIterator
  
  java.util.ListIterator
  
  java.util.Spliterator
  
  java.util.Enumeration
  
  java.util.Collection
  
  java.util.AbstractCollection
  ```

  

- **List**

  代码总行数： 1063 + 942 + 253 + 1266 + 1509 + 141 + 1759 = 6933 行，预计 12 个小时。

  ```ABAP
  java.util.List
  
  java.util.AbstractList
  
  java.util.AbstractSequentialList
  
  java.util.LinkedList
  
  java.util.Vector
  
  java.util.Stack
  
  java.util.ArrayList
  ```

  

- **Queue**

  代码总行数： 212 + 616 + 192 + 1233 + 987 = 3240 行，预计 6 个小时。

  ```ABAP
  java.util.Queue
  
  java.util.Deque
  
  java.util.AbstractQueue
  
  java.util.ArrayDeque
  
  java.util.PriorityQueue
  ```

  

- **Set**

  代码总行数： 732 + 186 + 264 + 491 + 323 + 361 + 560 + 195 + 1395 = 4507 行，预计 8 个小时。

  ```ABAP
  java.util.Set
  
  java.util.AbstractSet
  
  java.util.SortedSet
  
  java.util.EnumSet
  
  java.util.NavigableSet
  
  java.util.HashSet
  
  java.util.TreeSet
  
  java.util.LinkedHashSet
  
  java.util.BitSet
  ```

  

![image-20220122233710259](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122233710259-b725d8.png)

- **Map**

  代码总行数： 1687 + 284 + 424 + 857 + 3012 + 1339 + 812 + 1600 + 756 + 2444 + 155 + 1521 = 14891 行，预计 28 个小时。

  ```ABAP
  java.util.Map
  
  java.util.SortedMap
  
  java.util.NavigableMap
  
  java.util.AbstractMap
  
  java.util.TreeMap
  
  java.util.WeakHashMap
  
  java.util.EnumMap
  
  java.util.IdentityHashMap
  
  java.util.LinkedHashMap
  
  java.util.HashMap
  
  java.util.Dictionary
  
  java.util.Hashtable
  
  
  ```

![image-20220122233653438](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122233653438-f4a424.png)

来张总体结构图：

![image-20220122233639069](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122233639069-fd22d7.png)

> 这里没有包含并发相关的集合类。

##  集合类概述

![java collections overview](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/java-collections-overview-3a2f2a.png)

## 迭代器 Iterator、 Enumeration、 Spliterator 与 Iterable

### 2.1. 涉及代码

1. `java.util.Iterator`
2. `java.util.PrimitiveIterator`
3. `java.util.ListIterator`
4. `java.util.Spliterator`
5. `java.util.Enumeration`
6. `java.lang.Iterable`

### 2.2. 迭代器模式

在进行代码分析之前，先来讲解一下设计模式。然后结合 Java 中 `Iterator` 和 `Iteratable` ，具体分析一下迭代器在 Java 中的实现。

> - 迭代器模式（Iterator）
>
>   **提供一种方法顺序访问一个聚合对象中各个元素，而不是暴露该对象的内部表示。**

— Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides《设计模式》

类图如下：

![image-20220122234358972](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122234358972-e1eb74.png)

当需要访问一个聚集对象，而且不管这些对象是什么都需要遍历的时候，就应该考虑用迭代器模式。

当需要对聚集有多种方式遍历时，可以考虑用迭代器模式。

为遍历不同的聚集结构提供如开始、下一个、是否结束、当前哪一项等统一的接口。

尽管我们不需要显式的引用迭代器，但系统本身还是通过迭代器来实现遍历的。总地来说，迭代器（`Iterator`）模式就是分离了集合对象的遍历行为，抽象出一个迭代器类来负责，这样既可以做到不暴露集合的内部结构，又可让外部代码透明地访问集合内部的数据。

请问： Java 中是如何应用迭代器模式呢？

### 2.3. `Iterator`

从上面的设计模式可以看出，迭代器模式就是为了遍历不同的聚集结构提供诸如开始、下一个、是否结束、当前元素等常见操作的统一接口。来看看 Java 集合类是如何提炼接口的。

java.util.Iterator

```java

public interface Iterator<E> {

    boolean hasNext();

    E next();

    default void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * @since 1.8
     */
    default void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (hasNext())
            action.accept(next());
    }
}
```

从上述代码中，可以看出 Java 提取了 `boolean hasNext()`、 `E next()`、 `void remove()` 等三个操作方法；在 Java 8 中，为了支持 Stream API，有增加了 `void forEachRemaining(Consumer<? super E> action)` 方法。

这里多扯一句，Java 在 1.2 以前迭代器是通过另外一个接口实现的：

java.util.Enumeration

```java

public interface Enumeration<E> {

    boolean hasMoreElements();

    E nextElement();
}
```

与上面的 `java.util.Iterator` 对比可以看出，两者差别不大。那为什么 Java 在已有 `java.util.Iterator` 接口的情况下，还要推出 `java.util.Enumeration` 接口呢？在 `java.util.Iterator` 接口的 JavaDoc 中给出了如下理由：

- Iterators allow the caller to remove elements from the underlying collection during the iteration with well-defined semantics.
- Method names have been improved.

我们都知道，在 Java 8 之前，接口中的方法不能有任何实现。所以，为了保持兼容性，不能在已有接口中增加方法。只能另起炉灶，把“洞”补上。这也就不难理解，为什么又搞出了个 `java.util.Iterator`。

这里再多提一句，需要增加自定义的迭代器实现时，请优先选择 `java.util.Iterator`。

请问：既然有迭代器接口定义了，那么 Java 又是如何生成迭代器实例呢？

### 2.4. `Iterable`

既然迭代器可以抽象成一个公共的接口，那么生成迭代器实例的这个操作，也可以抽象成一个接口。 Java 也确实是这样做的：

java.lang.Iterable

```java
public interface Iterable<T> {

    Iterator<T> iterator();

    /**
     * @since 1.8
     */
    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (T t : this) {
            action.accept(t);
        }
    }

    /**
     * @since 1.8
     */
    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }
}
```

从类的定义中，可以看到 `java.lang.Iterable` 提供了 `iterator()`，用于创建 `java.util.Iterator` 示例对象。

在 Java 8 中，为了支持 Lambda 表达式和 Stream API，又增加了 `forEach(Consumer<? super T> action)` 和 `spliterator()` 方法。

在思考实现原理的过程中，D瓜哥突然想到，`java.lang.Iterable` 就是一个工厂方法模式的应用。来分析一下：

### 2.5. 工厂方法模式

先来看看工厂方法模式的定义：

> - 工厂方法模式（Factory Method）
>
>   **定义一个用于创建对象的接口，让子类决定实例化哪一个类。工厂方法使一个类的实例化延迟到其子类。**

— Erich Gamma、Richard Helm、Ralph Johnson、John Vlissides《设计模式》

类图如下：

![image-20220122234605493](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122234605493-27d492.png)

- `java.lang.Iterable` 就相当于 `Factory` 接口，也就是工厂；
- `java.util.Iterator` 就相当于工厂生成的产品 `Product`；
- `iterator()` 方法就是工厂方法 `factoryMethod()`；
- `java.lang.Iterable` 和 `java.util.Iterator` 子类，都放在了各个集合类中来具体实现。

在各个聚集类中，去实现 `java.lang.Iterable` 接口，然后根据聚集类的情况，返回对应的 `java.util.Iterator` 具体类对象即可。

细心的童鞋，可能发现还有个类似迭代器的类 `Spliterator`。这是个什么类？为啥要增加相关的接口呢？

### 2.6. `Spliterator`

### 2.7. `ListIterator`

`java.util.Iterator` 是针对整个集合类抽象出来的通用迭代器。但是，可以思考一下，对于 `java.util.List` 是不是可以有更契合的迭代器？

关于这个问题的答案，JDK 给出了自己的答案：

```java
public interface ListIterator<E> extends Iterator<E> {
    // Query Operations

    boolean hasNext();

    E next();

    boolean hasPrevious();

    E previous();

    int nextIndex();

    int previousIndex();


    // Modification Operations

    void remove();

    void set(E e);

    void add(E e);
}
```

由于 `List` 是有序的，从代码中可以看出，所以，`ListIterator` 在 `Iterator` 基础之上，增加了获前后元素相关的方法；同时，还增加了修改相关的操作方法。

因为增加了 `hasPrevious()` 和 `previous()`，那么 `ListIterator` 就有了双向遍历的能力：既可以像传统迭代器那样，从前向后遍历；又可以逆向，从后想前遍历。这样在某些场景下就会特别方便。

### 2.8. 参考资料

- [Java ListIterator - ListIterator in Java - JournalDev](https://www.journaldev.com/13457/java-listiterator)

## . Collection

有一个问题，我们思考一下：如果让你设计 JDK 集合框架，你会怎么设计？说的更具体一些，现在需要一个可以包含重复对象的 `Aggregation` 集合，请问怎么设计？

可以先设想一下，有哪些操作？

1. 添加元素 `add()`
2. 删除元素 `remove()`
3. 是否包含元素 `boolean contain(Element e)`
4. 列表大小 `int size()`
5. 添加整个 `Bag` 元素 `addAll(Aggregation aggregation)`
6. 清空 `clear()`
7. 迭代器 `Iterator<T> iterator()`
8. 和数组互操作： `toArray()` 和 `addAll(T[] array)`

java.util.Collection

```java
public interface Collection<E> extends Iterable<E> {
    // Query Operations

    int size();

    boolean isEmpty();

    boolean contains(Object o);

    Iterator<E> iterator();

    Object[] toArray();

    <T> T[] toArray(T[] a);

    // Modification Operations

    boolean add(E e);

    boolean remove(Object o);


    // Bulk Operations

    boolean containsAll(Collection<?> c);

    boolean addAll(Collection<? extends E> c);

    boolean removeAll(Collection<?> c);

    /**
     * @since 1.8
     */
    default boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<E> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    boolean retainAll(Collection<?> c);

    void clear();


    // Comparison and hashing

    boolean equals(Object o);

    int hashCode();

    /**
     * @since 1.8
     */
    @Override
    default Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, 0);
    }

    /**
     * @since 1.8
     */
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * @since 1.8
     */
    default Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
```

## 4. AbstractCollection

## 5. List

## 6. AbstractList

## 7. AbstractSequentialList

### 7.1. 类图

先来看一下 `AbstractSequentialList` 的类图：

![image-20220122234738503](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122234738503-de8eab.png)

`AbstractSequentialList` 是 [`java.util.LinkedList`](https://diguage.github.io/jdk-source-analysis/#) 的父类，主要是基于 [`java.util.ListIterator`](https://diguage.github.io/jdk-source-analysis/#ListIterator) 实现了

- `get(int index)`
- `set(int index, E element)`
- `add(int index, E element)`
- `remove(int index)`
- `addAll(int index, Collection<? extends E> c)`

等与具体坐标相关的随机访问 List 的方法。

## 8. `ArrayList`

对于每种抽象数据类型并不存在什么法则来告诉我们必须要有哪些操作，这是一个设计决策。--《数据结构与算法分析》

### 8.1. 类图

先来看一下 `ArrayList` 的类图：

![image-20220122234804511](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122234804511-7f2493.png)

- 支持泛型，继承了 `AbstractList`，实现了 `List` 接口。
- `RandomAccess` 用来表明其支持快速（通常是固定时间）随机访问。在 `Collections.binarySearch()` 方法中，它要判断传入的list 是否 `RamdomAccess` 的实例，如果是，调用 `Collections.indexedBinarySearch(list, key)` 方法，如果不是，那么调用 `Collections.iteratorBinarySearch(list, key)` 方法。
- `Cloneable` 可以调用 `Object.clone()` 方法返回该对象的浅拷贝。
- `Serializable` 此类可被序列化

>  为什么还要再次实现 `List` 接口？（其父类已经实现此接口部分方法： `AbstractList` 实现 `List` 接口中除 `size()` , `get(int index)` 之外的所有函数）。

**抽象类实现接口，可以不真正实现所有方法（可以抽象实现）。**

### 8.2. 初始化

首先，我们看一下 `ArrayList` 的初始化：

```java
 /**
 * Default initial capacity.
 */
private static final int DEFAULT_CAPACITY = 10;

/**
 * Shared empty array instance used for empty instances.
 */
private static final Object[] EMPTY_ELEMENTDATA = {};

/**
 * Shared empty array instance used for default sized empty instances. We
 * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
 * first element is added.
 */
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

/**
 * The array buffer into which the elements of the ArrayList are stored.
 * The capacity of the ArrayList is the length of this array buffer. Any
 * empty ArrayList with elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
 * will be expanded to DEFAULT_CAPACITY when the first element is added.
 */
transient Object[] elementData; // non-private to simplify nested class access

/**
 * The size of the ArrayList (the number of elements it contains).
 *
 * @serial
 */
private int size;

/**
 * Constructs an empty list with the specified initial capacity.
 *
 * @param  initialCapacity  the initial capacity of the list
 * @throws IllegalArgumentException if the specified initial capacity
 *         is negative
 */
public ArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
        this.elementData = new Object[initialCapacity];
    } else if (initialCapacity == 0) {
        this.elementData = EMPTY_ELEMENTDATA;
    } else {
        throw new IllegalArgumentException("Illegal Capacity: "+
                                           initialCapacity);
    }
}

/**
 * Constructs an empty list with an initial capacity of ten.
 */
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}

/**
 * Constructs a list containing the elements of the specified
 * collection, in the order they are returned by the collection's
 * iterator.
 *
 * @param c the collection whose elements are to be placed into this list
 * @throws NullPointerException if the specified collection is null
 */
public ArrayList(Collection<? extends E> c) {
    elementData = c.toArray();
    if ((size = elementData.length) != 0) {
        // defend against c.toArray (incorrectly) not returning Object[]
        // (see e.g. https://bugs.openjdk.java.net/browse/JDK-6260652)
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(elementData, size, Object[].class);
    } else {
        // replace with empty array.
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
```

从上述代码中可以猜测，**`ArrayList` 内部使用数组来保存元素的，并且使用一个整型 `int` 变量来保存长度。**

`ArrayList` 初始化工作分三种情况：

1. 无参构造函数初始化时，直接将内部数组初始化为 `DEFAULTCAPACITY_EMPTY_ELEMENTDATA`。在第一次添加元素时，再初始化为默认容量是 `10` 的数组。
2. 指定容量大小进行初始化时，容量大于 `0` 则初始化为指定容量的数组；如果等于 `0` 则初始化为默认空数组 `EMPTY_ELEMENTDATA`。否则抛出异常。
3. 如果使用 `Collection` 实例来初始化，不为空则将调用 `toArray()` 方法来初始化 `elementData`；如果为空则初始化为默认空数组 `EMPTY_ELEMENTDATA`。

### 8.3. 添加元素

在进行正常测试前，先展示一下"透视" `ArrayList` 的工具类：

```java

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;


public class ArrayListBaseTest {
    /**
     * 通过反射查看 {@link ArrayList} 的内部属性
     */
    public void xray(ArrayList list) {
        Class clazz = list.getClass();
        try {
            Field elementData = clazz.getDeclaredField("elementData");
            elementData.setAccessible(true);
            Object[] objects = (Object[]) elementData.get(list);
            Field sizeField = clazz.getDeclaredField("size");
            sizeField.setAccessible(true);

            int size = 0;
            for (int i = 0; i < objects.length; i++) {
                if (Objects.nonNull(objects[i])) {
                    ++size;
                }
            }
            System.out.println("length = " + objects.length
                    + ", size = " + sizeField.get(list)
                    + ", arraySize = " + size);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
```

正式测试：将初始化长度设置为 `8`，同时将添加元素的个数设置为 `8 * 2` 来方便观察数组增长情况。通过上述的工具方法，可以将 `ArrayList` 内部的数据进一步展示出来：

```java
 @Test
    public void testAddAtTail() {
        int initialCapacity = 8;
        ArrayList<Integer> integers = new ArrayList<>(initialCapacity);
        for (int i = 0; i < initialCapacity * 2; i++) {
            xray(integers);
            integers.add(i);
        }
    }
```

JDK 相关源代码：

```java
/**
 * Increases the capacity to ensure that it can hold at least the
 * number of elements specified by the minimum capacity argument.
 *
 * @param minCapacity the desired minimum capacity
 * @throws OutOfMemoryError if minCapacity is less than zero
 */
private Object[] grow(int minCapacity) {
    return elementData = Arrays.copyOf(elementData,
                                       newCapacity(minCapacity));
}

private Object[] grow() {
    return grow(size + 1);
}

/**
 * Returns a capacity at least as large as the given minimum capacity.
 * Returns the current capacity increased by 50% if that suffices.
 * Will not return a capacity greater than MAX_ARRAY_SIZE unless
 * the given minimum capacity is greater than MAX_ARRAY_SIZE.
 *
 * @param minCapacity the desired minimum capacity
 * @throws OutOfMemoryError if minCapacity is less than zero
 */
private int newCapacity(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity <= 0) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            return Math.max(DEFAULT_CAPACITY, minCapacity);
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return minCapacity;
    }
    return (newCapacity - MAX_ARRAY_SIZE <= 0)
        ? newCapacity
        : hugeCapacity(minCapacity);
}

private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE)
        ? Integer.MAX_VALUE
        : MAX_ARRAY_SIZE;
}

/**
 * This helper method split out from add(E) to keep method
 * bytecode size under 35 (the -XX:MaxInlineSize default value),
 * which helps when add(E) is called in a C1-compiled loop.
 */
private void add(E e, Object[] elementData, int s) {
    if (s == elementData.length)
        elementData = grow();
    elementData[s] = e;
    size = s + 1;
}

/**
 * Appends the specified element to the end of this list.
 *
 * @param e element to be appended to this list
 * @return {@code true} (as specified by {@link Collection#add})
 */
public boolean add(E e) {
    modCount++;
    add(e, elementData, size);
    return true;
}
```

经过测试发现，`ArrayList` 是在容量达到数组长度之后，再次添加才会扩容，扩容长度为 `int newCapacity = oldCapacity + (oldCapacity >> 1)`，最小为原始长度，最大不能超过 `int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8`。



之所以最大长度为 `Integer.MAX_VALUE` 减去 `8`，文档解释是因为某些 VM 保留数组头部用于存储一些 header words。但是在 `hugeCapacity(int minCapacity)` 方法中，在最小容量大于 `MAX_ARRAY_SIZE`，又可以返回 `Integer.MAX_VALUE`。

```java
		@Test
    public void testAddAtHeader() {
        int initialCapacity = 8;
        ArrayList<Integer> integers = new ArrayList<>(initialCapacity);
        for (int i = 0; i < initialCapacity * 2; i++) {
            xray(integers);
            integers.add(0, i);
        }
    }
```



```java
/**
 * Inserts the specified element at the specified position in this
 * list. Shifts the element currently at that position (if any) and
 * any subsequent elements to the right (adds one to their indices).
 *
 * @param index index at which the specified element is to be inserted
 * @param element element to be inserted
 * @throws IndexOutOfBoundsException {@inheritDoc}
 */
public void add(int index, E element) {
    rangeCheckForAdd(index);
    modCount++;
    final int s;
    Object[] elementData;
    if ((s = size) == (elementData = this.elementData).length)
        elementData = grow();
    System.arraycopy(elementData, index,
                     elementData, index + 1,
                     s - index);
    elementData[index] = element;
    size = s + 1;
}

/**
 * A version of rangeCheck used by add and addAll.
 */
private void rangeCheckForAdd(int index) {
    if (index > size || index < 0)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}
```

从这里看出，在头部插入添加元素，实际就是将指定坐标位置以及右侧所有元素向后移动一位，腾出空间存放新元素。

```java
 /**
 * Appends all of the elements in the specified collection to the end of
 * this list, in the order that they are returned by the
 * specified collection's Iterator.  The behavior of this operation is
 * undefined if the specified collection is modified while the operation
 * is in progress.  (This implies that the behavior of this call is
 * undefined if the specified collection is this list, and this
 * list is nonempty.)
 *
 * @param c collection containing elements to be added to this list
 * @return {@code true} if this list changed as a result of the call
 * @throws NullPointerException if the specified collection is null
 */
public boolean addAll(Collection<? extends E> c) {
    Object[] a = c.toArray();
    modCount++;
    int numNew = a.length;
    if (numNew == 0)
        return false;
    Object[] elementData;
    final int s;
    if (numNew > (elementData = this.elementData).length - (s = size))
        elementData = grow(s + numNew);
    System.arraycopy(a, 0, elementData, s, numNew);
    size = s + numNew;
    return true;
}

/**
 * Inserts all of the elements in the specified collection into this
 * list, starting at the specified position.  Shifts the element
 * currently at that position (if any) and any subsequent elements to
 * the right (increases their indices).  The new elements will appear
 * in the list in the order that they are returned by the
 * specified collection's iterator.
 *
 * @param index index at which to insert the first element from the
 *              specified collection
 * @param c collection containing elements to be added to this list
 * @return {@code true} if this list changed as a result of the call
 * @throws IndexOutOfBoundsException {@inheritDoc}
 * @throws NullPointerException if the specified collection is null
 */
public boolean addAll(int index, Collection<? extends E> c) {
    rangeCheckForAdd(index);

    Object[] a = c.toArray();
    modCount++;
    int numNew = a.length;
    if (numNew == 0)
        return false;
    Object[] elementData;
    final int s;
    if (numNew > (elementData = this.elementData).length - (s = size))
        elementData = grow(s + numNew);

    int numMoved = s - index;
    if (numMoved > 0)
        System.arraycopy(elementData, index,
                         elementData, index + numNew,
                         numMoved);
    System.arraycopy(a, 0, elementData, index, numNew);
    size = s + numNew;
    return true;
}
```

向 `ArrayList` 中添加集合实例，则是集合示例转化成数组，然后利用数组拷贝的方式来高效完成添加工作。

### 8.4. 删除元素

### 8.5. 测试遍历速度

`ArrayList` 的遍历方式有如下几种：

1. 标准迭代器方式
2. 外部 `for` 循环 + `get(index)`
3. `forEach(lambda)` 方法
4. `for(E e: arrayList)`

基准测试当然非 Java Microbenchmark Harness 莫属了。直接上代码：

```java
 import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.Iterator;


@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3)
@State(Scope.Benchmark)
@Threads(8)
public class ArrayListIteratorSpeedTest {

    private ArrayList<Integer> arrayList = null;

    @Setup(Level.Iteration)
    public void setup() {
        int capacity = 1_000_000;
        arrayList = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            arrayList.add(i);
        }
    }

    @Benchmark
    public void testIterator() {
        Integer iteratorValue = null;
        Iterator<Integer> iterator = arrayList.iterator();
        while (iterator.hasNext()) {
            iteratorValue = iterator.next();
        }
    }

    @Benchmark
    public void testRandomAccess() {
        Integer randomAccessValue = null;
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            randomAccessValue = arrayList.get(i);
        }
    }

    @Benchmark
    public void testForEachLambda() {
        arrayList.forEach(this::devnull);
    }

    public void devnull(Integer value) {
        Integer forEachLambdaValue = value;
    }

    @Benchmark
    public void testForEach() {
        Integer forEachValue = null;
        for (Integer integer : arrayList) {
            forEachValue = integer;
        }
    }
}
```

运行结果如下：

在代码中，常常看到 `modCount` 属性，这个属性是从 `AbstractList` 继承到的一个重要属性。 这个属性用于在使用迭代器（`ListIterator`，`Iterator`）遍历的时候，用来检查列表中的元素是否发生结构性变化（列表元素数量发生改变）了，主要在多线程环境下需要使用，防止一个线程正在迭代遍历，另一个线程修改了这个列表的结构。`ArrayList` 是非线程安全的，多线程同时修改会抛出异常。`writeObject`（序列化时），也可能会抛出此异常。



检查到修改不一致就抛出异常是 fail-fast 机制，是 Java 集合(Collection)中的一种错误机制。它只能被用来检测错误，因为JDK并不保证 fail-fast 机制一定会发生。如果发生 fail-fast，则推荐使用 `JUC` 中对应的类。



简单来说，Java 的序列化机制是通过在运行时判断类的 `serialVersionUID` 来验证版本一致性的。在进行反序列化时，JVM会把传来的字节流中的 `serialVersionUID` 与本地相应实体（类）的 `serialVersionUID` 进行比较，如果相同就认为是一致的，可以进行反序列化，否则就会出现序列化版本不一致的异常。(InvalidCastException)



`serialVersionUID` 有两种显示的生成方式：

1. 一个是默认的L类型数字，比如：private static final long serialVersionUID = 1L;
2. 一个是根据类名、接口名、成员方法及属性等来生成一个64位的哈希字段。

当实现 `java.io.Serializable` 接口的实体（类）没有显式地定义一个名为 `serialVersionUID`，类型为 `long` 的变量时，Java序列化机制会根据编译的class(它通过类名，方法名等诸多因素经过计算而得，理论上是一一映射的关系，也就是唯一的)自动生成一个 `serialVersionUID` 作序列化版本比较用，这种情况下，如果class文件(类名,方法明等)没有发生变化(增加空格,换行,增加注释,等等)，就算再编译多次，`serialVersionUID` 也不会变化的.



如果我们不希望通过编译来强制划分软件版本，即实现序列化接口的实体能够兼容先前版本，未作更改的类，就需要显式地定义一个名为 `serialVersionUID`，类型为 `long` 的变量，不修改这个变量值的序列化实体都可以相互进行串行化和反串行化。



观察3 两个重要属性 关键字 transient（瞬态）被标记为transient的属性在对象被序列化的时候不会被保存。 why? 假如elementData的长度为10，而其中只有5个元素，那么在序列化的时候只需要存储5个元素，而数组中后面5个元素是不需要存储的。于是将elementData定义为transient，避免了Java自带的序列化机制，并定义了两个方法，实现了自己可控制的序列化操作。

```java
//更新
public E set(int index, E element) {
	rangeCheck(index);

	E oldValue = elementData(index);
	elementData[index] = element;
	return oldValue;
}
//查找

public E get(int index) {
	rangeCheck(index);

	return elementData(index);
}

//是否包含
public boolean contains(Object o) {
	return indexOf(o) >= 0;
}

public int indexOf(Object o) {
	if (o == null) {
		for (int i = 0; i < size; i++)
			if (elementData[i]==null)
				return i;
	} else {
		for (int i = 0; i < size; i++)
			if (o.equals(elementData[i]))
				return i;
	}
	return -1;
}
//反向查找
public int lastIndexOf(Object o) {
	if (o == null) {
		for (int i = size-1; i >= 0; i--)
			if (elementData[i]==null)
				return i;
	} else {
		for (int i = size-1; i >= 0; i--)
			if (o.equals(elementData[i]))
				return i;
	}
	return -1;
}
//容量判断

public int size() {
	return size;
}

public boolean isEmpty() {
	return size == 0;
}
```

**1.浅克隆（shallow clone）**

被复制对象的所有基础类型变量（byte,short,int,long,char,boolean,float,double）与原有对象中变量具有相同的值，修改其值不会影响原对象；而复制对象中引用类型（数组，类对象等）还是指向原来对象，修改其值会影响原对象。

**2.深克隆（deep clone）**

被复制对象的所有基础类型变量（byte,short,int,long,char,boolean,float,double）与原有对象中变量具有相同的值，修改其值不会影响原对象；并且复制对象中引用类型（数组，类对象等）指向被复制过的新对象，修改其值不会影响原对象。

```java
public Object clone() {
	try {
		ArrayList<?> v = (ArrayList<?>) super.clone();
		v.elementData = Arrays.copyOf(elementData, size);
		v.modCount = 0;
		return v;
	} catch (CloneNotSupportedException e) {
		// this shouldn't happen, since we are Cloneable
		throw new InternalError(e);
	}
}

public Object[] toArray() {
	return Arrays.copyOf(elementData, size);
}

@SuppressWarnings("unchecked")
public <T> T[] toArray(T[] a) {
	if (a.length < size)
		// Make a new array of a's runtime type, but my contents:
		return (T[]) Arrays.copyOf(elementData, size, a.getClass());
	System.arraycopy(elementData, 0, a, 0, size);
	if (a.length > size)
		a[size] = null;
	return a;
}

// Positional Access Operations
//得到指定索引处的元素
@SuppressWarnings("unchecked")
E elementData(int index) {
	return (E) elementData[index];
}
//清空
public void clear() {
	modCount++;

	// clear to let GC do its work
	for (int i = 0; i < size; i++)
		elementData[i] = null;

	size = 0;
}


protected void removeRange(int fromIndex, int toIndex) {
	modCount++;
	int numMoved = size - toIndex;
	System.arraycopy(elementData, toIndex, elementData, fromIndex,
					 numMoved);

	// clear to let GC do its work
	int newSize = size - (toIndex-fromIndex);
	for (int i = newSize; i < size; i++) {
		elementData[i] = null;
	}
	size = newSize;
}

 //检查数否超出数组长度 用于添加元素时
private void rangeCheck(int index) {
	if (index >= size)
		throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}
//检查是否溢出
private void rangeCheckForAdd(int index) {
	if (index > size || index < 0)
		throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
}

private String outOfBoundsMsg(int index) {
	return "Index: "+index+", Size: "+size;
}
//删除指定集合的元素
public boolean removeAll(Collection<?> c) {
	Objects.requireNonNull(c);
	return batchRemove(c, false);
}
//仅保留指定集合的元素
public boolean retainAll(Collection<?> c) {
	Objects.requireNonNull(c);
	return batchRemove(c, true);
}
// @param complement true时从数组保留指定集合中元素的值，为false时从数组删除指定集合中元素的值。
// @return 数组中重复的元素都会被删除(而不是仅删除一次或几次)，有任何删除操作都会返回true
private boolean batchRemove(Collection<?> c, boolean complement) {
	final Object[] elementData = this.elementData;
	int r = 0, w = 0;
	boolean modified = false;
	try {
		for (; r < size; r++)
			if (c.contains(elementData[r]) == complement)
				elementData[w++] = elementData[r];
	} finally {
		// Preserve behavioral compatibility with AbstractCollection,
		// even if c.contains() throws.
		if (r != size) {
			System.arraycopy(elementData, r,
							 elementData, w,
							 size - r);
			w += size - r;
		}
		if (w != size) {
			// clear to let GC do its work
			for (int i = w; i < size; i++)
				elementData[i] = null;
			modCount += size - w;
			size = w;
			modified = true;
		}
	}
	return modified;
}
 //保存数组实例的状态到一个流（即它序列化）。写入过程数组被更改会抛出异常
private void writeObject(java.io.ObjectOutputStream s)
	throws java.io.IOException{
	// Write out element count, and any hidden stuff
	int expectedModCount = modCount;
	s.defaultWriteObject();

	// Write out size as capacity for behavioural compatibility with clone()
	s.writeInt(size);

	// Write out all elements in the proper order.
	for (int i=0; i<size; i++) {
		s.writeObject(elementData[i]);
	}

	if (modCount != expectedModCount) {
		throw new ConcurrentModificationException();
	}
}
//上面是写，这个就是读了。
private void readObject(java.io.ObjectInputStream s)
	throws java.io.IOException, ClassNotFoundException {
	elementData = EMPTY_ELEMENTDATA;

	// Read in size, and any hidden stuff
	s.defaultReadObject();

	// Read in capacity
	s.readInt(); // ignored

	if (size > 0) {
		// be like clone(), allocate array based upon size not capacity
		ensureCapacityInternal(size);

		Object[] a = elementData;
		// Read in all elements in the proper order.
		for (int i=0; i<size; i++) {
			a[i] = s.readObject();
		}
	}
}

public ListIterator<E> listIterator(int index) {
	if (index < 0 || index > size)
		throw new IndexOutOfBoundsException("Index: "+index);
	return new ListItr(index);
}
实现Iterable
public ListIterator<E> listIterator() {
	return new ListItr(0);
}
实现Iterable
public Iterator<E> iterator() {
	return new Itr();
}





//通用的迭代器实现 迭代器（Iterator）模式
private class Itr implements Iterator<E> {
	int cursor;       // index of next element to return
	int lastRet = -1; // index of last element returned; -1 if no such
	int expectedModCount = modCount;

	public boolean hasNext() {
		return cursor != size;
	}

	@SuppressWarnings("unchecked")
	public E next() {
		checkForComodification();
		int i = cursor;
		if (i >= size)
			throw new NoSuchElementException();
		Object[] elementData = ArrayList.this.elementData;
		if (i >= elementData.length)
			throw new ConcurrentModificationException();
		cursor = i + 1;
		return (E) elementData[lastRet = i];
	}

	public void remove() {
		if (lastRet < 0)
			throw new IllegalStateException();
		checkForComodification();

		try {
			ArrayList.this.remove(lastRet);
			cursor = lastRet;
			lastRet = -1;
			expectedModCount = modCount;
		} catch (IndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void forEachRemaining(Consumer<? super E> consumer) {
		Objects.requireNonNull(consumer);
		final int size = ArrayList.this.size;
		int i = cursor;
		if (i >= size) {
			return;
		}
		final Object[] elementData = ArrayList.this.elementData;
		if (i >= elementData.length) {
			throw new ConcurrentModificationException();
		}
		while (i != size && modCount == expectedModCount) {
			consumer.accept((E) elementData[i++]);
		}
		// update once at end of iteration to reduce heap write traffic
		cursor = i;
		lastRet = i - 1;
		checkForComodification();
	}

	final void checkForComodification() {
		if (modCount != expectedModCount)
			throw new ConcurrentModificationException();
	}
}
```

其中的ListItr继承Itr，实现了ListIterator接口，同时重写了hasPrevious()，nextIndex()， previousIndex()，previous()，set(E e)，add(E e)等方法，所以这也可以看出了Iterator和ListIterator的区别，就是ListIterator在Iterator的基础上增加了添加对象，修改对象，逆向遍历等方法，这些是Iterator不能实现的。

```java
private class ListItr extends Itr implements ListIterator<E> {
	ListItr(int index) {
		super();
		cursor = index;
	}

	public boolean hasPrevious() {
		return cursor != 0;
	}

	public int nextIndex() {
		return cursor;
	}

	public int previousIndex() {
		return cursor - 1;
	}

	@SuppressWarnings("unchecked")
	public E previous() {
		checkForComodification();
		int i = cursor - 1;
		if (i < 0)
			throw new NoSuchElementException();
		Object[] elementData = ArrayList.this.elementData;
		if (i >= elementData.length)
			throw new ConcurrentModificationException();
		cursor = i;
		return (E) elementData[lastRet = i];
	}

	public void set(E e) {
		if (lastRet < 0)
			throw new IllegalStateException();
		checkForComodification();

		try {
			ArrayList.this.set(lastRet, e);
		} catch (IndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}

	public void add(E e) {
		checkForComodification();

		try {
			int i = cursor;
			ArrayList.this.add(i, e);
			cursor = i + 1;
			lastRet = -1;
			expectedModCount = modCount;
		} catch (IndexOutOfBoundsException ex) {
			throw new ConcurrentModificationException();
		}
	}
}
//返回指定范围的子数组
public List<E> subList(int fromIndex, int toIndex) {
	subListRangeCheck(fromIndex, toIndex, size);
	return new SubList(this, 0, fromIndex, toIndex);
}

static void subListRangeCheck(int fromIndex, int toIndex, int size) {
	if (fromIndex < 0)
		throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
	if (toIndex > size)
		throw new IndexOutOfBoundsException("toIndex = " + toIndex);
	if (fromIndex > toIndex)
		throw new IllegalArgumentException("fromIndex(" + fromIndex +
										   ") > toIndex(" + toIndex + ")");
}
```

其中的SubList继承AbstractList，实现了RandmAccess接口，类内部实现了对子序列的增删改查等方法，但它同时也充分利用了内部类的优点，

就是共享ArrayList的全局变量，例如检查器变量modCount，数组elementData等，所以SubList进行的增删改查操作都是对ArrayList的数组进行的，并没有创建新的数组(不浪费内存资源)。

```java
private class SubList extends AbstractList<E> implements RandomAccess {
	private final AbstractList<E> parent;
	private final int parentOffset;
	private final int offset;
	int size;

	SubList(AbstractList<E> parent,
			int offset, int fromIndex, int toIndex) {
		this.parent = parent;
		this.parentOffset = fromIndex;
		this.offset = offset + fromIndex;
		this.size = toIndex - fromIndex;
		this.modCount = ArrayList.this.modCount;
	}

	public E set(int index, E e) {
		rangeCheck(index);
		checkForComodification();
		E oldValue = ArrayList.this.elementData(offset + index);
		ArrayList.this.elementData[offset + index] = e;
		return oldValue;
	}

	public E get(int index) {
		rangeCheck(index);
		checkForComodification();
		return ArrayList.this.elementData(offset + index);
	}

	public int size() {
		checkForComodification();
		return this.size;
	}

	public void add(int index, E e) {
		rangeCheckForAdd(index);
		checkForComodification();
		parent.add(parentOffset + index, e);
		this.modCount = parent.modCount;
		this.size++;
	}

	public E remove(int index) {
		rangeCheck(index);
		checkForComodification();
		E result = parent.remove(parentOffset + index);
		this.modCount = parent.modCount;
		this.size--;
		return result;
	}

	protected void removeRange(int fromIndex, int toIndex) {
		checkForComodification();
		parent.removeRange(parentOffset + fromIndex,
						   parentOffset + toIndex);
		this.modCount = parent.modCount;
		this.size -= toIndex - fromIndex;
	}

	public boolean addAll(Collection<? extends E> c) {
		return addAll(this.size, c);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		rangeCheckForAdd(index);
		int cSize = c.size();
		if (cSize==0)
			return false;

		checkForComodification();
		parent.addAll(parentOffset + index, c);
		this.modCount = parent.modCount;
		this.size += cSize;
		return true;
	}

	public Iterator<E> iterator() {
		return listIterator();
	}

	public ListIterator<E> listIterator(final int index) {
		checkForComodification();
		rangeCheckForAdd(index);
		final int offset = this.offset;

		return new ListIterator<E>() {
			int cursor = index;
			int lastRet = -1;
			int expectedModCount = ArrayList.this.modCount;

			public boolean hasNext() {
				return cursor != SubList.this.size;
			}

			@SuppressWarnings("unchecked")
			public E next() {
				checkForComodification();
				int i = cursor;
				if (i >= SubList.this.size)
					throw new NoSuchElementException();
				Object[] elementData = ArrayList.this.elementData;
				if (offset + i >= elementData.length)
					throw new ConcurrentModificationException();
				cursor = i + 1;
				return (E) elementData[offset + (lastRet = i)];
			}

			public boolean hasPrevious() {
				return cursor != 0;
			}

			@SuppressWarnings("unchecked")
			public E previous() {
				checkForComodification();
				int i = cursor - 1;
				if (i < 0)
					throw new NoSuchElementException();
				Object[] elementData = ArrayList.this.elementData;
				if (offset + i >= elementData.length)
					throw new ConcurrentModificationException();
				cursor = i;
				return (E) elementData[offset + (lastRet = i)];
			}

			@SuppressWarnings("unchecked")
			public void forEachRemaining(Consumer<? super E> consumer) {
				Objects.requireNonNull(consumer);
				final int size = SubList.this.size;
				int i = cursor;
				if (i >= size) {
					return;
				}
				final Object[] elementData = ArrayList.this.elementData;
				if (offset + i >= elementData.length) {
					throw new ConcurrentModificationException();
				}
				while (i != size && modCount == expectedModCount) {
					consumer.accept((E) elementData[offset + (i++)]);
				}
				// update once at end of iteration to reduce heap write traffic
				lastRet = cursor = i;
				checkForComodification();
			}

			public int nextIndex() {
				return cursor;
			}

			public int previousIndex() {
				return cursor - 1;
			}

			public void remove() {
				if (lastRet < 0)
					throw new IllegalStateException();
				checkForComodification();

				try {
					SubList.this.remove(lastRet);
					cursor = lastRet;
					lastRet = -1;
					expectedModCount = ArrayList.this.modCount;
				} catch (IndexOutOfBoundsException ex) {
					throw new ConcurrentModificationException();
				}
			}

			public void set(E e) {
				if (lastRet < 0)
					throw new IllegalStateException();
				checkForComodification();

				try {
					ArrayList.this.set(offset + lastRet, e);
				} catch (IndexOutOfBoundsException ex) {
					throw new ConcurrentModificationException();
				}
			}

			public void add(E e) {
				checkForComodification();

				try {
					int i = cursor;
					SubList.this.add(i, e);
					cursor = i + 1;
					lastRet = -1;
					expectedModCount = ArrayList.this.modCount;
				} catch (IndexOutOfBoundsException ex) {
					throw new ConcurrentModificationException();
				}
			}

			final void checkForComodification() {
				if (expectedModCount != ArrayList.this.modCount)
					throw new ConcurrentModificationException();
			}
		};
	}

	public List<E> subList(int fromIndex, int toIndex) {
		subListRangeCheck(fromIndex, toIndex, size);
		return new SubList(this, offset, fromIndex, toIndex);
	}

	private void rangeCheck(int index) {
		if (index < 0 || index >= this.size)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	private void rangeCheckForAdd(int index) {
		if (index < 0 || index > this.size)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	private String outOfBoundsMsg(int index) {
		return "Index: "+index+", Size: "+this.size;
	}

	private void checkForComodification() {
		if (ArrayList.this.modCount != this.modCount)
			throw new ConcurrentModificationException();
	}

	public Spliterator<E> spliterator() {
		checkForComodification();
		return new ArrayListSpliterator<E>(ArrayList.this, offset,
										   offset + this.size, this.modCount);
	}
}







//按照比较器的判断逻辑进行排序
@Override
@SuppressWarnings("unchecked")
public void sort(Comparator<? super E> c) {
	final int expectedModCount = modCount;
	Arrays.sort((E[]) elementData, 0, size, c);
	if (modCount != expectedModCount) {
		throw new ConcurrentModificationException();
	}
	modCount++;
}



// 以下基于 1.8，和函数式编程相关的方法
@Override
public void forEach(Consumer<? super E> action) {
	Objects.requireNonNull(action);
	final int expectedModCount = modCount;
	@SuppressWarnings("unchecked")
	final E[] elementData = (E[]) this.elementData;
	final int size = this.size;
	for (int i=0; modCount == expectedModCount && i < size; i++) {
		action.accept(elementData[i]);
	}
	if (modCount != expectedModCount) {
		throw new ConcurrentModificationException();
	}
}

@Override
public Spliterator<E> spliterator() {
	return new ArrayListSpliterator<>(this, 0, -1, 0);
}

static final class ArrayListSpliterator<E> implements Spliterator<E> {


	private final ArrayList<E> list;
	private int index; // current index, modified on advance/split
	private int fence; // -1 until used; then one past last index
	private int expectedModCount; // initialized when fence set

	/** Create new spliterator covering the given  range */
	ArrayListSpliterator(ArrayList<E> list, int origin, int fence,
						 int expectedModCount) {
		this.list = list; // OK if null unless traversed
		this.index = origin;
		this.fence = fence;
		this.expectedModCount = expectedModCount;
	}

	private int getFence() { // initialize fence to size on first use
		int hi; // (a specialized variant appears in method forEach)
		ArrayList<E> lst;
		if ((hi = fence) < 0) {
			if ((lst = list) == null)
				hi = fence = 0;
			else {
				expectedModCount = lst.modCount;
				hi = fence = lst.size;
			}
		}
		return hi;
	}

	public ArrayListSpliterator<E> trySplit() {
		int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
		return (lo >= mid) ? null : // divide range in half unless too small
			new ArrayListSpliterator<E>(list, lo, index = mid,
										expectedModCount);
	}

	public boolean tryAdvance(Consumer<? super E> action) {
		if (action == null)
			throw new NullPointerException();
		int hi = getFence(), i = index;
		if (i < hi) {
			index = i + 1;
			@SuppressWarnings("unchecked") E e = (E)list.elementData[i];
			action.accept(e);
			if (list.modCount != expectedModCount)
				throw new ConcurrentModificationException();
			return true;
		}
		return false;
	}

	public void forEachRemaining(Consumer<? super E> action) {
		int i, hi, mc; // hoist accesses and checks from loop
		ArrayList<E> lst; Object[] a;
		if (action == null)
			throw new NullPointerException();
		if ((lst = list) != null && (a = lst.elementData) != null) {
			if ((hi = fence) < 0) {
				mc = lst.modCount;
				hi = lst.size;
			}
			else
				mc = expectedModCount;
			if ((i = index) >= 0 && (index = hi) <= a.length) {
				for (; i < hi; ++i) {
					@SuppressWarnings("unchecked") E e = (E) a[i];
					action.accept(e);
				}
				if (lst.modCount == mc)
					return;
			}
		}
		throw new ConcurrentModificationException();
	}

	public long estimateSize() {
		return (long) (getFence() - index);
	}

	public int characteristics() {
		return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
	}
}

@Override
public boolean removeIf(Predicate<? super E> filter) {
	Objects.requireNonNull(filter);
	// figure out which elements are to be removed
	// any exception thrown from the filter predicate at this stage
	// will leave the collection unmodified
	int removeCount = 0;
	final BitSet removeSet = new BitSet(size);
	final int expectedModCount = modCount;
	final int size = this.size;
	for (int i=0; modCount == expectedModCount && i < size; i++) {
		@SuppressWarnings("unchecked")
		final E element = (E) elementData[i];
		if (filter.test(element)) {
			removeSet.set(i);
			removeCount++;
		}
	}
	if (modCount != expectedModCount) {
		throw new ConcurrentModificationException();
	}

	// shift surviving elements left over the spaces left by removed elements
	final boolean anyToRemove = removeCount > 0;
	if (anyToRemove) {
		final int newSize = size - removeCount;
		for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
			i = removeSet.nextClearBit(i);
			elementData[j] = elementData[i];
		}
		for (int k=newSize; k < size; k++) {
			elementData[k] = null;  // Let gc do its work
		}
		this.size = newSize;
		if (modCount != expectedModCount) {
			throw new ConcurrentModificationException();
		}
		modCount++;
	}

	return anyToRemove;
}

@Override
@SuppressWarnings("unchecked")
public void replaceAll(UnaryOperator<E> operator) {
	Objects.requireNonNull(operator);
	final int expectedModCount = modCount;
	final int size = this.size;
	for (int i=0; modCount == expectedModCount && i < size; i++) {
		elementData[i] = operator.apply((E) elementData[i]);
	}
	if (modCount != expectedModCount) {
		throw new ConcurrentModificationException();
	}
	modCount++;
}

}
```

总结， List接口可调整大小的数组实现。实现所有可选的List操作，并允许所有元素，包括null，元素可重复。 除了列表接口外，该类提供了一种方法来操作该数组的大小来存储该列表中的数组的大小。

> 时间复杂度：
> 方法size、isEmpty、get、set、iterator和listIterator的调用是常数时间的。
> 	添加删除的时间复杂度为O(N)。其他所有操作也都是线性时间复杂度。

> 容量：
> 	每个ArrayList都有容量，容量大小至少为List元素的长度，默认初始化为10。
>  容量可以自动增长。
>  如果提前知道数组元素较多，可以在添加元素前通过调用ensureCapacity()方法提前增加容量以减小后期容量自动增长的开销。
>  也可以通过带初始容量的构造器初始化这个容量。

>  线程不安全：
>  	ArrayList不是线程安全的。
>  	如果需要应用到多线程中，需要在外部做同步。
> **指导意义**
> 那种遍历性能更优？应该使用哪种遍历方式？
> 《编写高质量代码：改善Java程序的151个建议》一书认为使用传统的下标遍历是优于增强型for循环的，而《Effective Java中文版 第2版》推荐的是增强型for循环，说for-each循环没有性能损失。何解？
> 在ArrayList大小为十万之前，五种遍历方式时间消耗几乎一样
> 即便在千万大小的ArrayList中，几种遍历方式相差也不过50ms左右（for-each循环较大），且在常用的十万左右时间几乎相等，考虑foreach简洁的优点，我们大可选用foreach这种简便方式进行遍历。

![image-20220122235630758](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122235630758-0da9e7.png)

这是对ArrayList效率影响比较大的一个因素。 每当执行Add等添加元素的方法，都会检查内部数组的容量是否不够了，如果是，它就会以当前容量 的 1.5 倍来重新构建一个数组，将旧元素Copy到新数组中，然后丢弃旧数组，在这个临界点的扩容操作，应该来说是比较影响效率的。 正确的预估可能的元素，是提高ArrayList使用效率的重要途径。

### 8.6. 问题

1. Java 中有很多标识类的接口。这些表示类有什么意义？是否在 Java 虚拟机中对其进行了特殊处理？
2. 在实现 `java.io.Serializable` 时，如果不声明 `serialVersionUID` 变量时，是否会生成这个值？默认的值是什么？在序列化时，是如何保存这个值？在反序列化时，如何从对象的字节码中获取这个值？比较后，如果不同又怎么处理的？
3. 在 `ArrayList` 中有 `writeObject(java.io.ObjectOutputStream s)` 和 `readObject(java.io.ObjectInputStream s)` 方法。在单例模式中，为了解决反序列化的问题，会添加 `readResolve()` 方法。这三个方法有什么用？什么时候被什么调用？被什么调用？设置断点调试一下，看 **调用栈**。
4. ArrayList 在扩容时，使用的是 `oldCapacity + (oldCapacity >> 1)`，这里 `oldCapacity >> 1` 就是直接移位将 oldCapacity 的值减半，取到的值就是 oldCapacity/2 后的最大正整数。
5. ArrayList 中有 `rangeCheck(int index)` 和 `rangeCheckForAdd(int index)`，区别就是前者没有做负数检查。为什么会有这种区别？为什么不检查负数？再为什么不检查负数为什么还能抛出 `ArrayIndexOutOfBoundsException` 异常？（文档中）
6. 《数据结构与算法分析》 中提到 `Iterator` 和 `ListIterator` 的区别以及 `ListIterator` 中一个特殊的使用。再次看书来确认一下。
7. 通过指令来对比 Iterat or 和 foreach 之间的性能差异。

### 8.7. 参考资料

1. [单例与序列化的那些事儿-HollisChuang’s Blog](http://www.hollischuang.com/archives/1144)
2. [深度分析Java的枚举类型—-枚举的线程安全性及序列化问题-HollisChuang’s Blog](http://www.hollischuang.com/archives/197)

## 9. LinkedList

`LinkedList` 底层使用的是 双向链表 数据结构（JDK1.6 之前为循环链表，JDK1.7 取消了循环。注意双向链表和双向循环链表的区别。）

### 9.1. 类图

先来看一下 `LinkedList` 的类图：

![image-20220122235657939](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/image-20220122235657939-b2eb87.png)

### 9.2. 初始化

先看看 `LinkedList` 中内部属性和构造函数：

```java
transient int size = 0;

/**
 * Pointer to first node.
 */
transient Node<E> first;

/**
 * Pointer to last node.
 */
transient Node<E> last;

/*
void dataStructureInvariants() {
    assert (size == 0)
        ? (first == null && last == null)
        : (first.prev == null && last.next == null);
}
*/

/**
 * Constructs an empty list.
 */
public LinkedList() {
}

/**
 * Constructs a list containing the elements of the specified
 * collection, in the order they are returned by the collection's
 * iterator.
 *
 * @param  c the collection whose elements are to be placed into this list
 * @throws NullPointerException if the specified collection is null
 */
public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}

private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;

    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

从这里一眼即可看出内部使用一个双向链表来保存数据。初始化工作也及其干净，什么也不干。另外一个构造函数后面再分析。

>   **使用初始化的头尾节点更方便代码书写，少了很多繁琐的判断。**

### 9.3. 分析工具

使用反射来获取内部属性，然后做进一步分析。

```java
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Objects;


public class LinkedListBaseTest {
    /**
     * 使用反射读取 LinkedList 内部属性
     */
    public void xray(LinkedList<?> list) {
        Class<? extends LinkedList> clazz = list.getClass();
        try {
            Field nodeField = clazz.getDeclaredField("first");
            nodeField.setAccessible(true);
            Object node = nodeField.get(list);
            System.out.println("length=" + length(node) + ", size=" + list.size());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public int length(Object node) {
        int result = 0;
        if (Objects.isNull(node)) {
            return result;
        }
        try {
            Class<?> nodeClass = node.getClass();
            Field nextField = nodeClass.getDeclaredField("next");
            nextField.setAccessible(true);
            while (Objects.nonNull(node)) {
                node = nextField.get(node);
                result++;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result;
    }
}
```

### 9.4. 添加元素

测试代码：

```java
		@Test
    public void testAddAtTail() {
        LinkedList<Integer> list = new LinkedList<>();
        for (int i = 0; i < 16; i++) {
            xray(list);
            list.add(i);
        }
    }
```

JDK 源码：

```java
/**
 * Links e as last element.
 */
void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null)
        first = newNode;
    else
        l.next = newNode;
    size++;
    modCount++;
}

/**
 * Appends the specified element to the end of this list.
 *
 * <p>This method is equivalent to {@link #addLast}.
 *
 * @param e element to be appended to this list
 * @return {@code true} (as specified by {@link Collection#add})
 */
public boolean add(E e) {
    linkLast(e);
    return true;
}
```

再来看看从头部插入元素：

```java
		@Test
    public void testAddAtHeader() {
        LinkedList<Integer> list = new LinkedList<>();
        for (int i = 0; i < 16; i++) {
            xray(list);
            list.addFirst(i);
        }
    }
```

JDK 源码：

```java
/**
 * Links e as first element.
 */
private void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null)
        last = newNode;
    else
        f.prev = newNode;
    size++;
    modCount++;
}

/**
 * Inserts the specified element at the beginning of this list.
 *
 * @param e the element to add
 */
public void addFirst(E e) {
    linkFirst(e);
}
```

从这里就能看出，`LinkedList` 在 `add(e)`、`addLast(e)` 或者 `addFirst(e)` 时，都是对链表的首尾进行操作，会比较高效。

### 9.5. Redis 的 linkedlist

Redis 底层也有很多地方使用到 linkedlist，并且也是双向链表。

adlist.h

```cc
typedef struct listNode {
    struct listNode prev;
    struct listNode next;
    void value;
} listNode;

typedef struct listIter {
    listNode next;
    int direction;
} listIter;

typedef struct list {
    listNode head;
    listNode tail;
    void (dup)(void ptr);
    void (free)(void ptr);
    int (match)(void ptr, void key);
    unsigned long len;
} list;
```

Redis 的 linkedlist 实现特点是：

1. 双向：节点带有前后指针；
2. 无环：首尾没有相连，所以没有构成环状；
3. 链表保存了首尾指针；
4. 多态：可以保存不同类型的值，这里成为泛型也许更符合 Java 中的语义。

Redis 在 2014 年实现了 [quicklist](https://diguage.github.io/jdk-source-analysis/#quicklist)，并使用 quicklist 代替了 linkedlist。所以，现在 linkedlist 几乎已经是废弃状态。

### 9.6. Redis 的 ziplist

Redis 官方在 ziplist.c 文件的注释中对 ziplist 进行了定义：

> The ziplist is a specially encoded dually linked list that is designed to be very memory efficient. It stores both strings and integer values, where integers are encoded as actual integers instead of a series of characters. It allows push and pop operations on either side of the list in O(1) time. However, because every operation requires a reallocation of the memory used by the ziplist, the actual complexity is related to the amount of memory used by the ziplist.

— ziplist.c

就是说，ziplist 是一个经过**特殊编码的双向链表**，它的设计目标就是为了提高存储效率。ziplist 可以用于存储字符串或整数，其中整数是按真正的二进制表示进行编码的，而不是编码成字符串序列。它能以 O(1) 的时间复杂度在表的两端提供 `push` 和 `pop` 操作。

```
The general layout of the ziplist is as follows:

<zlbytes> <zltail> <zllen> <entry> <entry> ... <entry> <zlend>

NOTE: all fields are stored in little endian, if not specified otherwise.
```

![image-20220123000216195](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123000216195-ae7541.png)

1. `<zlbytes>`: 32bit，表示ziplist占用的字节总数（也包括<zlbytes>本身占用的4个字节）。

2. `<zltail>`: 32bit，表示ziplist表中最后一项（entry）在ziplist中的偏移字节数。

   `<zltail>` 的存在，使得我们可以很方便地找到最后一项（不用遍历整个ziplist），从而可以在ziplist尾端快速地执行push或pop操作。

3. `<zllen>`: 16bit， 表示ziplist中数据项（entry）的个数。zllen字段因为只有16bit，所以可以表达的最大值为216-1。`<zllen>` 等于16bit全为1的情况，那么 `<zllen>` 就不表示数据项个数了，这时要想知道 ziplist 中数据项总数，那么必须对ziplist从头到尾遍历各个数据项，才能计数出来。

4. `<entry>`: 表示真正存放数据的数据项，长度不定。一个数据项（entry）也有它自己的内部结构，这个稍后再解释。

5. `<zlend>`: ziplist 最后 1 个字节，是一个结束标记，值固定等于 255。

ziplist 将表中每一项存放在前后连续的地址空间内，一个ziplist整体占用一大块内存。它是一个表（list），但其实不是一个链表（linked list）。

ziplist 为了在细节上节省内存，对于值的存储采用了变长的编码方式。

每一个数据项<entry>的构成：

```c
<prevlen> <encoding> <entry-data> 
  
```

1. 	<prevlen>: 表示前一个数据项占用的总字节数。
   如果前一个数据项占用字节数小于254，那么 <prevlen> 就只用一个字节来表示，这个字节的值就是前一个数据项的占用字节数： <prevlen from 0 to 253> <encoding> <entry>

   如果前一个数据项占用字节数大于等于254，那么 <prevlen> 就用5个字节来表示，其中第1个字节的值是254（作为这种情况的一个标记），而后面4个字节组成一个整型值，来真正存储前一个数据项的占用字节数

   ```c
   0xFE <4 bytes unsigned little endian prevlen> <encoding> <entry>
     
   ```

2. ​	<encoding>: 表示当前数据项的类型，整型或者字符串。

3. ​    <entry-data>: 数据

关于 `<encoding> <entry-data>` 的编码，直接引用官方文档：

> The encoding field of the entry depends on the content of the entry. When the entry is a string, the first 2 bits of the encoding first byte will hold the type of encoding used to store the length of the string, followed by the actual length of the string. When the entry is an integer the first 2 bits are both set to 1. The following 2 bits are used to specify what kind of integer will be stored after this header. An overview of the different types and encodings is as follows. The first byte is always enough to determine the kind of entry.
>
> ```
> |00pppppp| - 1 byte
>      String value with length less than or equal to 63 bytes (6 bits).
>      "pppppp" represents the unsigned 6 bit length.
> |01pppppp|qqqqqqqq| - 2 bytes
>      String value with length less than or equal to 16383 bytes (14 bits).
>      IMPORTANT: The 14 bit number is stored in big endian.
> |10000000|qqqqqqqq|rrrrrrrr|ssssssss|tttttttt| - 5 bytes
>      String value with length greater than or equal to 16384 bytes.
>      Only the 4 bytes following the first byte represents the length
>      up to 32^2-1. The 6 lower bits of the first byte are not used and
>      are set to zero.
>      IMPORTANT: The 32 bit number is stored in big endian.
> |11000000| - 3 bytes
>      Integer encoded as int16_t (2 bytes).
> |11010000| - 5 bytes
>      Integer encoded as int32_t (4 bytes).
> |11100000| - 9 bytes
>      Integer encoded as int64_t (8 bytes).
> |11110000| - 4 bytes
>      Integer encoded as 24 bit signed (3 bytes).
> |11111110| - 2 bytes
>      Integer encoded as 8 bit signed (1 byte).
> |1111xxxx| - (with xxxx between 0000 and 1101) immediate 4 bit integer.
>      Unsigned integer from 0 to 12. The encoded value is actually from
>      1 to 13 because 0000 and 1111 can not be used, so 1 should be
>      subtracted from the encoded 4 bit value to obtain the right value.
> |11111111| - End of ziplist special entry.
> ```

— ziplist.c

引用在网上找的例子，来做个说明：

![image-20220123000653385](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123000653385-4f9d63.png)

1. 这个ziplist一共包含 33 个字节。字节编号从 `byte[0]` 到 `byte[32]`。图中每个字节的值使用 16 进制表示。
2. 头 4 个字节（`0x21000000`）是按小端（little endian）模式存储的 `<zlbytes>` 字段。什么是小端呢？就是指数据的低字节保存在内存的低地址中（参见维基百科词条 [Endianness](https://en.wikipedia.org/wiki/Endianness)）。因此，这里 `<zlbytes>` 的值应该解析成 `0x00000021`，用十进制表示正好就是33。
3. 接下来 4 个字节（`byte[4..7]`）是 `<zltail>`，用小端存储模式来解释，它的值是 `0x0000001D`（值为29），表示最后一个数据项在 `byte[29]` 的位置（那个数据项为 `0x05FE14`）。
4. 再接下来 2 个字节（`byte[8..9]`），值为 `0x0004`，表示这个 ziplist 里一共存有4项数据。
5. 接下来 6 个字节（`byte[10..15]`）是第 1 个数据项。其中，`prevlen=0`，因为它前面没有数据项；`len=4`，相当于前面定义的9种情况中的第1种，表示后面4个字节按字符串存储数据，数据的值为：`name`。
6. 接下来 8 个字节（`byte[16..23]`）是第 2 个数据项，与前面数据项存储格式类似，存储 1 个字符串：`tielei`。
7. 接下来 5 个字节（`byte[24..28]`）是第 3 个数据项，与前面数据项存储格式类似，存储 1 个字符串： `age`。
8. 接下来3个字节（`byte[29..31]`）是最后一个数据项，它的格式与前面的数据项存储格式不太一样。其中，第 1 个字节 `prevlen=5`，表示前一个数据项占用 5 个字节；第 2 个字节 = `FE`，相当于前面定义的9种情况中的第8种，所以后面还有1个字节用来表示真正的数据，并且以整数表示。它的值是20（0x14）。
9. 最后1个字节（`byte[32]`）表示 `<zlend>`，是固定的值255（0xFF）。

有两个问题需要注意：

1. *如何反向遍历 ziplist ？*

   `<prevlen>`: 表示前一个数据项占用的总字节数。那么就能找到前一个元素的起始位置，就能实现反向遍历。

2. *如何从 ziplist 中添加/删除数据？删除数据后，对应位置的 Bits 位怎么处理？*

   在某个/某些节点的前面添加新节点之后， 程序必须沿着路径挨个检查后续的节点，是否满足新长度的编码要求， 直到遇到一个能满足要求的节点（如果有一个能满足，则这个节点之后的其他节点也满足）， 或者到达 ziplist 的末端 zlend 为止， 这种检查操作的复杂度为 O(N2) 。

   因为只有在新添加节点的后面有连续多个长度接近 254 的节点时， 这种连锁更新才会发生， 所以可以普遍地认为， 这种连锁更新发生的概率非常小， 在一般情况下， 将添加操作看成是 O(N) 复杂度也是可以的。

   删除元素就进行内存移位，覆盖 target 原本的数据，然后通过内存重分配，收缩多余空间。

Redis 在下面这个几个地方使用了 ziplist：

1. 列表包含少量的列表项，并且列表项只是整数或者短小的字符串时。（在下面 [quicklist](https://diguage.github.io/jdk-source-analysis/#quicklist) 小节中，在最新版 Redis 中测试，显示的是 quicklist，而 quicklist 内部使用的是 ziplist 来存储数据，只是外面被 quicklist 包裹着。）

2. 在哈希键值包含少量键值对，并且每个键值对只包含整数或短小字符串时。

   

   ` 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 $ redis-cli --raw 127.0.0.1:6379> HMSET site domain "https://www.diguage.com" owner "D瓜哥" OK 127.0.0.1:6379> HGET site domain https://www.diguage.com 127.0.0.1:6379> HGET site owner D瓜哥 127.0.0.1:6379> TYPE site hash 127.0.0.1:6379> OBJECT encoding site ziplist `

```c
$ redis-cli --raw

127.0.0.1:6379> HMSET site domain "https://www.woniuxy.com" owner "linKe"
OK

127.0.0.1:6379> HGET site domain
https://www.woniuxy.com

127.0.0.1:6379> HGET site owner
linKe

127.0.0.1:6379> TYPE site
hash

127.0.0.1:6379> OBJECT encoding site
ziplist
```



### 9.7. quicklist

Redis 对外暴露的 list 数据类型，它底层实现所依赖的内部数据结构就是 quicklist。

list 是一个能维持数据项先后顺序的列表（各个数据项的先后顺序由插入位置决定），便于在表的两端追加和删除数据，而对于中间位置的存取具有 O(N) 的时间复杂度。

> **quicklist.c - A doubly linked list of ziplists**

— redis/quicklist.c

Redis 在 `quicklist.c` 就说明了，quicklist 是一个双向链表，而且是一个 ziplist 的双向链表。quicklist 的每个节点都是一个 ziplist。这样设计大概又是一个空间和时间的折中：

1. 双向链表便于在表的两端进行 `push` 和 `pop` 操作，但是它的内存开销比较大。首先，它在每个节点上除了要保存数据之外，还要额外保存两个指针；其次，双向链表的各个节点是单独的内存块，地址不连续，节点多了容易产生内存碎片。
2. ziplist 由于是一整块连续内存，所以存储效率很高。但是，它不利于修改操作，每次数据变动都会引发一次内存的 `realloc` 。特别是当 ziplist 长度很长的时候，一次 `realloc` 可能会导致大批量的数据拷贝，进一步降低性能。

于是，结合了双向链表和 ziplist 的优点，quicklist 就应运而生了。

新问题：到底一个 quicklist 节点包含多长的 ziplist 合适呢？

1. 每个quicklist节点上的ziplist越短，则内存碎片越多。
2. 每个quicklist节点上的ziplist越长，则为ziplist分配大块连续内存空间的难度就越大。

Redis 提供了一个配置参数 `list-max-ziplist-size` 让使用者可以来根据自己的情况进行调整:

```c
list-max-ziplist-size -2
```

这个参数可正可负：

- 当取正值的时候，表示按照数据项个数来限定每个 quicklist 节点上的 ziplist 长度。
- 当取负值的时候，表示按照占用字节数来限定每个 quicklist 节点上的 ziplist 长度。这时，它只能取 `-1` 到 `-5` 这五个值，每个值含义如下：
  - `-5`: 每个 quicklist 节点上的 ziplist 大小不能超过 64 Kb。（注：1kb ⇒ 1024 bytes）
  - `-4`: 每个 quicklist 节点上的 ziplist 大小不能超过 32 Kb。
  - `-3`: 每个 quicklist 节点上的 ziplist 大小不能超过 16 Kb。
  - `-2`: 每个 quicklist 节点上的 ziplist 大小不能超过 8 Kb。（-2是Redis给出的默认值）
  - `-1`: 每个 quicklist 节点上的 ziplist 大小不能超过 4 Kb。

list的设计目标是能够用来存储很长的数据列表的。当列表很长的时候，最容易被访问的很可能是两端的数据，中间的数据被访问的频率比较低。list 还提供了一个选项，能够把中间的数据节点进行压缩，从而进一步节省内存空间。Redis 的配置参数 `list-compress-depth` 就是用来完成这个设置的。

```c
list-compress-depth 0 // 0 是特殊值，表示都不压缩，默认值。
```

这个参数表示一个quicklist两端不被压缩的节点个数。注：这里的节点个数是指quicklist双向链表的节点个数，而不是指ziplist里面的数据项个数。一个 quicklist 节点上的 ziplist，如果被压缩，就是整体被压缩的。

Redis 对于 quicklist 内部节点的压缩算法，采用的 [LZF](https://en.wikipedia.org/wiki/LZ4_(compression_algorithm)) ——一种无损压缩算法。

1. 添加过程中，如何处理中间位置的压缩工作？
2. 头部或者尾部删除，导致 quicklistNode 的非压缩节点不符合设置，怎么处理？
3. 如果中间删除，节点为压缩节点，怎么处理？

quicklist.h

```C
 /* Node, quicklist, and Iterator are the only data structures used currently. /

/ quicklistNode is a 32 byte struct describing a ziplist for a quicklist.
 * We use bit fields keep the quicklistNode at 32 bytes.
 * count: 16 bits, max 65536 (max zl bytes is 65k, so max count actually < 32k).
 * encoding: 2 bits, RAW=1, LZF=2.
 * container: 2 bits, NONE=1, ZIPLIST=2.
 * recompress: 1 bit, bool, true if node is temporarry decompressed for usage.
 * attempted_compress: 1 bit, boolean, used for verifying during testing.
 * extra: 10 bits, free for future use; pads out the remainder of 32 bits /
typedef struct quicklistNode {
    struct quicklistNode prev;
    struct quicklistNode next;
    unsigned char zl;
    unsigned int sz;             /* ziplist size in bytes /
    unsigned int count : 16;     / count of items in ziplist /
    unsigned int encoding : 2;   / RAW==1 or LZF==2 /
    unsigned int container : 2;  / NONE==1 or ZIPLIST==2 /
    unsigned int recompress : 1; / was this node previous compressed? /
    unsigned int attempted_compress : 1; / node can't compress; too small /
    unsigned int extra : 10; / more bits to steal for future usage /
} quicklistNode;

/ quicklistLZF is a 4+N byte struct holding 'sz' followed by 'compressed'.
 * 'sz' is byte length of 'compressed' field.
 * 'compressed' is LZF data with total (compressed) length 'sz'
 * NOTE: uncompressed length is stored in quicklistNode->sz.
 * When quicklistNode->zl is compressed, node->zl points to a quicklistLZF /
typedef struct quicklistLZF {
    unsigned int sz; / LZF size in bytes*/
    char compressed[];
} quicklistLZF;

/* Bookmarks are padded with realloc at the end of of the quicklist struct.
 * They should only be used for very big lists if thousands of nodes were the
 * excess memory usage is negligible, and there's a real need to iterate on them
 * in portions.
 * When not used, they don't add any memory overhead, but when used and then
 * deleted, some overhead remains (to avoid resonance).
 * The number of bookmarks used should be kept to minimum since it also adds
 * overhead on node deletion (searching for a bookmark to update). /
typedef struct quicklistBookmark {
    quicklistNode node;
    char name;
} quicklistBookmark;

/ quicklist is a 40 byte struct (on 64-bit systems) describing a quicklist.
 * 'count' is the number of total entries.
 * 'len' is the number of quicklist nodes.
 * 'compress' is: -1 if compression disabled, otherwise it's the number
 *                of quicklistNodes to leave uncompressed at ends of quicklist.
 * 'fill' is the user-requested (or default) fill factor.
 * 'bookmakrs are an optional feature that is used by realloc this struct,
 *      so that they don't consume memory when not used. /
typedef struct quicklist {
    quicklistNode head;
    quicklistNode tail;
    unsigned long count;        / total count of all entries in all ziplists /
    unsigned long len;          / number of quicklistNodes /
    int fill : QL_FILL_BITS;              / fill factor for individual nodes /
    unsigned int compress : QL_COMP_BITS; / depth of end nodes not to compress;0=off /
    unsigned int bookmark_count: QL_BM_BITS;
    quicklistBookmark bookmarks[];
} quicklist;

typedef struct quicklistIter {
    const quicklist quicklist;
    quicklistNode current;
    unsigned char zi;
    long offset; /* offset in current ziplist /
    int direction;
} quicklistIter;

typedef struct quicklistEntry {
    const quicklist quicklist;
    quicklistNode node;
    unsigned char zi;
    unsigned char *value;
    long long longval;
    unsigned int sz;
    int offset;
} quicklistEntry; 
```

![image-20220123001026650](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123001026650-d438b6.png)



```bash
$ redis-cli --raw

127.0.0.1:6379> RPUSH names lin "linKe" "https://www.woniuxy.com/"
2

127.0.0.1:6379> LRANGE names 0 -1
diguage
linKe
https://www.linKe.com/

127.0.0.1:6379> TYPE names
list

127.0.0.1:6379> OBJECT encoding names
quicklist
```

### 9.8. 参考资料

1. [Redis内部数据结构详解(4)——ziplist](http://zhangtielei.com/posts/blog-redis-ziplist.html)
2. [Redis内部数据结构详解(5)——quicklist](http://zhangtielei.com/posts/blog-redis-quicklist.html)

ArrayList更适合随机访问，而LinkedList更适合插入和删除。

- 对add(E e)方法的分析，可以得知LinkedList添加数据的效率高；
- 对remove(int index)方法的分析，可以了解到LinkedList删除数据的效率高；
- 对get(int index),set(int index, E element)方法的分析，可以看出LinkdedList查询的效率不高（需要定位，最差要遍历一半）；

核心数据结构通过内部类体现，Node就是实际的结点，存放了结点元素和前后结点的引用。

在1.7之前LinkedList是通过headerEntry实现的一个首尾相连的循环链表的。
从1.7开始，LinkedList是一个Node实现的非循环链表。

```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;

    Node(Node<E> prev, E element, Node<E> next) {
        this.item = element;
        this.next = next;
        this.prev = prev;
    }
}
```

代码开始

```
package java.util;

import java.util.function.Consumer;
```

### 9.9. 类的继承关系

![image-20220123001234239](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123001234239-563bfb.png)

继承自AbstractSequentialList，一个LinkedList抽象的实现； 重点关注实现了Deque接口。

```java
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
{
```

### 9.10. 类的属性

```java
    //存储元素个数
    transient int size = 0;

    //存储头结点
    transient Node<E> first;

    //存储尾结点
    transient Node<E> last;
```

### 9.11. 类的构造器

```java
    //无参构造器
    public LinkedList() {
    }

    //通过一个集合初始化LinkedList，元素顺序由这个集合的迭代器返回顺序决定
    public LinkedList(Collection<? extends E> c) {
        //调用无参构造器
        this();
        //添加元素
        addAll(c);
    }
```

### 9.12. 类的方法

主要的方法的基础是link和unlink方法组,Node<E> node(int index)定位方法（均不是public）

![image-20220123001313122](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123001313122-fc5f84.png)

```java
    //在指定节点前插入节点，节点succ不能为空
    void linkBefore(E e, Node<E> succ) {
        //获取succ的前结点
        final Node<E> pred = succ.prev;
        final Node<E> newNode = new Node<>(pred, e, succ);
        succ.prev = newNode;
        if (pred == null)//如果前结点为空
            first = newNode;
        else
            pred.next = newNode;
        size++;
        modCount++;
    }
    //把对应参数作为第一个节点，内部使用
    private void linkFirst(E e) {
        //获取头结点
        final Node<E> f = first;
        //定义新结点
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
        if (f == null)//头结点为null
            // 赋值尾结点（结果只有一个元素）
            last = newNode;
        else
            //把原来的首结点的引用指向这个新加的结点
            f.prev = newNode;
        size++;
        modCount++;
        //LinkedList也采用了“快速失败”的机制，通过记录modCount参数来实现。在面对并发的修改时，
        //迭代器很快就会完全失败，而不是冒着在将来某个不确定时间发生任意不确定行为的风险。

    }

    //把对应参数作为尾节点（和前一个方法类似）
    void linkLast(E e) {
        // 获取尾结点，l为final类型，不可更改
        final Node<E> l = last;
        final Node<E> newNode = new Node<>(l, e, null);
        last = newNode;
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        size++;
        modCount++;
    }
```

![image-20220123001451316](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123001451316-99df8b.png)

```java
      //删除指定节点并返回被删除的元素值
      E unlink(Node<E> x) {
          // assert x != null;
          final E element = x.item;
          final Node<E> next = x.next;
          final Node<E> prev = x.prev;

          if (prev == null) {
              first = next;
          } else {
              prev.next = next;
              x.prev = null;
          }

          if (next == null) {
              last = prev;
          } else {
              next.prev = prev;
              x.next = null;
          }

          x.item = null;
          size--;
          modCount++;
          return element;
      }
    //删除首节点并返回删除前首节点的值，内部使用
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        final E element = f.item;
        final Node<E> next = f.next;
        f.item = null;
        f.next = null; // help GC
        first = next;
        if (next == null)
            last = null;
        else
            next.prev = null;
        size--;
        modCount++;
        return element;
    }
    //删除尾节点并返回删除前尾节点的值，内部使用
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        final E element = l.item;
        final Node<E> prev = l.prev;
        l.item = null;
        l.prev = null; // help GC
        last = prev;
        if (prev == null)
            first = null;
        else
            prev.next = null;
        size--;
        modCount++;
        return element;
    }
//获取第一个元素
    public E getFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }
//获取最后一个元素
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

//删除第一个元素并返回删除的元素
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }
//删除最后一个元素并返回删除的值
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }
//添加元素作为第一个元素
    public void addFirst(E e) {
        linkFirst(e);
    }
 //添加元素作为最后一个元素
    public void addLast(E e) {
        linkLast(e);
    }
 //检查是否包含某个元素，返回bool
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }
//返回列表长度
    public int size() {
        return size;
    }
//添加一个元素，默认添加到末尾作为最后一个元素
    public boolean add(E e) {
        linkLast(e);
        return true;
    }
//删除指定元素，默认从first节点开始，删除第一次出现的那个元素（需要迭代）
    public boolean remove(Object o) {
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }
    //添加指定集合的元素到列表，从最后开始添加
    public boolean addAll(Collection<? extends E> c) {
        //调用addAll(int index, Collection<? extends E> c)
        return addAll(size, c);
    }
   //从指定位置往后追加，index和之后的元素向后顺延
    public boolean addAll(int index, Collection<? extends E> c) {
        checkPositionIndex(index);
        //转化成数组
        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0)
            return false;

        Node<E> pred, succ;
        if (index == size) {
            succ = null;
            pred = last;
        } else {//如果不是从末尾开始添加，获取新加串的前后结点
            succ = node(index);
            pred = succ.prev;
        }
        //遍历数组并添加到列表中
        for (Object o : a) {
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);
            if (pred == null)
                first = newNode;
            else
                pred.next = newNode;//如果存在前节点，前节点会向后指向新加的节点
            pred = newNode;//新加的节点成为前一个节点
        }

        if (succ == null) {
            last = pred;//如果是从最后开始添加的，则最后添加的节点成为尾节点
        } else {
            pred.next = succ;//如果不是从最后开始添加的，则最后添加的节点向后指向之前得到的后续第一个节点
            succ.prev = pred;//后续的第一个节点也应改为向前指向最后一个添加的节点
        }

        size += numNew;
        modCount++;
        return true;
    }
    //清空表
    public void clear() {
        //方便gc回收垃圾
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;
        size = 0;
        modCount++;
    }

    //获取指定索引的节点的值
    public E get(int index) {
        checkElementIndex(index);
        return node(index).item;
    }
    //修改指定索引的值并返回之前的值
    public E set(int index, E element) {
        checkElementIndex(index);
        Node<E> x = node(index);
        E oldVal = x.item;
        //只是把item替换掉
        x.item = element;
        return oldVal;
    }
    //在指定位置后面添加元素
    public void add(int index, E element) {
        checkPositionIndex(index);

        if (index == size)
            linkLast(element);
        else
            linkBefore(element, node(index));
    }
    //删除指定位置的元素
    public E remove(int index) {
        checkElementIndex(index);
        return unlink(node(index));
    }
    //检查索引是否超出范围（checkElementIndex调用），因为元素索引是0~size-1的，所以index必须满足0<=index<size
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }
    //检查位置是否超出范围（checkPositionIndex调用），index必须在index~size之间（含），如果超出，返回false
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }
    //异常详情
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }
    //检查元素索引是否超出范围（set,get,remove时检查），若已超出，就抛出异常
    private void checkElementIndex(int index) {
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
    //检查位置是否超出范围（为添加和迭代检查使用），若已超出，就抛出异常
    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
    //获取指定位置的节点
    //该方法返回双向链表中指定位置处的节点，而链表中是没有下标索引的，要指定位置出的元素，就要遍历该链表，从源码的实现中，我们看到这里有一个加速动作。
    //源码中先将index与长度size的一半比较，如果index<size/2，就只从位置0往后遍历到位置index处，而如果index>size/2，就只从位置size往前遍历到位置index处。这样可以减少一部分不必要的遍历。
    Node<E> node(int index) {
        // assert isElementIndex(index);

        if (index < (size >> 1)) {
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
    //获取第一个指定元素的索引位置并返回索引，不存在就返回-1
    public int indexOf(Object o) {
        int index = 0;
        if (o == null) {
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }
        return -1;
    }
    //获取最后一个指定元素索引的索引并返回索引，不存在就返回-1
    public int lastIndexOf(Object o) {
        int index = size;
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;
            }
        }
        return -1;
    }
```

Queue操作

```java
    //提供普通队列和双端队列的功能，FIFO
     //出队（从前端），获得第一个元素，不存在会返回null，不会删除元素（节点）
    public E peek() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
    }
    //出队（从前端），不删除元素，若为null会抛出异常而不是返回null
    public E element() {
        return getFirst();
    }
    //出队（从前端），如果不存在会返回null，存在的话会返回值并移除这个元素（节点）
    public E poll() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }
    //出队（从前端），如果不存在会抛出异常而不是返回null，存在的话会返回值并移除这个元素（节点）
    public E remove() {
        return removeFirst();
    }
    //入队（从后端），始终返回true
    public boolean offer(E e) {
        return add(e);
    }

```

Deque（双端队列）操作

```java

    //入队（从前端），始终返回true
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }
    //入队（从后端），始终返回true
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }
    //出队（从前端），获得第一个元素，不存在会返回null，不会删除元素（节点）
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
     }
     //出队（从后端），获得最后一个元素，不存在会返回null，不会删除元素（节点）
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }
    //出队（从前端），获得第一个元素，不存在会返回null，会删除元素（节点）
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }
    //出队（从后端），获得最后一个元素，不存在会返回null，会删除元素（节点）
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }
    //入栈，从前面添加
    public void push(E e) {
        addFirst(e);
    }
    //出栈，返回栈顶元素，从前面移除（会删除）
    public E pop() {
        return removeFirst();
    }
    //删除列表中第一出现o的节点
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }
    //逆向搜索，删除第一次出现o的节点
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

```

>  通用迭代器实现  继承自AbstractSequentialList的方法，AbstractSequentialList抽象类中
>
>  ```java
>  public Iterator<E> iterator() {
>         return listIterator();
>   }
>  ```
>
>
> 通用迭代器与ArrayList不同，ArrayList自己实现了Iterator，说明linkedlist的迭代器天生支持反向迭代。

ListIterator迭代器实现与ArrayList类似 其中的ListItr继承Itr，实现了ListIterator接口，同时重写了hasPrevious()，nextIndex()， previousIndex()，previous()，set(E e)，add(E e)等方法， 所以这也可以看出了Iterator和ListIterator的区别，就是ListIterator在Iterator的基础上增加了添加对象，修改对象， 逆向遍历等方法。

```java
    public ListIterator<E> listIterator(int index) {
        checkPositionIndex(index);
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned;
        private Node<E> next;
        private int nextIndex;
        private int expectedModCount = modCount;

        ListItr(int index) {
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);
            nextIndex = index;
        }

        public boolean hasNext() {
            return nextIndex < size;
        }

        public E next() {
            checkForComodification();
            if (!hasNext())
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        public E previous() {
            checkForComodification();
            if (!hasPrevious())
                throw new NoSuchElementException();

            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;
            return lastReturned.item;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();

            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
    
    
    
    
    //节点的数据结构内部类，包含前后节点的引用和当前节点
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
    
    
    
    
    //反向迭代器（实现Deque接口）
    //Deque接口定义的方法，实现Iterator接口，用listIterator迭代器返回一个迭代在此双端队列逆向顺序的元素
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }
    //
    private class DescendingIterator implements Iterator<E> {
        private final ListItr itr = new ListItr(size());
        public boolean hasNext() {
            return itr.hasPrevious();
        }
        public E next() {
            return itr.previous();
        }
        public void remove() {
            itr.remove();
        }
    }
    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
    
    //与ArrayList一样都是调用super。clone()
    //protected native Object clone() throws CloneNotSupportedException;
    //被复制对象的所有变量都含有与原来的对象相同的值，而所有的对其他对象的引用仍然指向原来的对象。
    public Object clone() {
        LinkedList<E> clone = superClone();

        // Put clone into "virgin" state
        clone.first = clone.last = null;
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)
            clone.add(x.item);

        return clone;
    }
```

转换成数组

```java
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    @SuppressWarnings("unchecked")
    //如果没有参数，就默认生成一个Object数组，如果给了T类型，就将节点内容放入a数组，
    //如果a的长度小于链表，就使用反射生成一个链表大小的数组，这个时候由于类型是T，所以无法直接实例化。
    public <T> T[] toArray(T[] a) {
        if (a.length < size)
            a = (T[])java.lang.reflect.Array.newInstance(
                                a.getClass().getComponentType(), size);
        int i = 0;
        Object[] result = a;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;

        if (a.length > size)
            a[size] = null;

        return a;
    }
```

如果声明该方法，它将会被ObjectOutputStream调用而不是默认的序列化进程。如果你是第一次看见它， 你会很惊奇尽管它们被外部类调用但事实上这是两个private的方法。并且它们既不存在于java.lang.Object，也没有在Serializable中声明。 那么ObjectOutputStream如何使用它们的呢？这个吗，ObjectOutputStream使用了反射来寻找是否声明了这两个方法。 因为ObjectOutputStream使用getPrivateMethod，所以这些方法不得不被声明为priate以至于供ObjectOutputStream来使用。

```java
    private static final long serialVersionUID = 876323262645176354L;
    //定义了自己的序列化方法，通过反射调用
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            linkLast((E)s.readObject());
    }
    
    
    
    //以下关于1.8函数式编程
    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator<E>(this, -1, 0);
    }

    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1 << 10;  // batch array size increment
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedList<E> list; // null OK unless traversed
        Node<E> current;      // current node; null until initialized
        int est;              // size estimate; -1 until first needed
        int expectedModCount; // initialized when est set
        int batch;            // batch size for splits

        LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
            this.list = list;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEst() {
            int s; // force initialization
            final LinkedList<E> lst;
            if ((s = est) < 0) {
                if ((lst = list) == null)
                    s = est = 0;
                else {
                    expectedModCount = lst.modCount;
                    current = lst.first;
                    s = est = lst.size;
                }
            }
            return s;
        }

        public long estimateSize() { return (long) getEst(); }

        public Spliterator<E> trySplit() {
            Node<E> p;
            int s = getEst();
            if (s > 1 && (p = current) != null) {
                int n = batch + BATCH_UNIT;
                if (n > s)
                    n = s;
                if (n > MAX_BATCH)
                    n = MAX_BATCH;
                Object[] a = new Object[n];
                int j = 0;
                do { a[j++] = p.item; } while ((p = p.next) != null && j < n);
                current = p;
                batch = j;
                est = s - j;
                return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p; int n;
            if (action == null) throw new NullPointerException();
            if ((n = getEst()) > 0 && (p = current) != null) {
                current = null;
                est = 0;
                do {
                    E e = p.item;
                    p = p.next;
                    action.accept(e);
                } while (p != null && --n > 0);
            }
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            if (getEst() > 0 && (p = current) != null) {
                --est;
                E e = p.item;
                current = p.next;
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

}
```

LinkedList与ArrayList的区别： LinkedList与ArrayList在性能上各有优缺点，都有各自适用的地方，总结如下：

ArrayList是实现了基于动态数组的数据结构，LinkedList基于链表的数据结构。
LinkedList不支持高效的随机元素访问。
ArrayList的空间浪费主要体现在在list列表的结尾预留一定的容量空间，
而LinkedList的空间花费则体现在它的每一个元素都需要消耗相当的空间（需要附加的空间来表明数据元素的逻辑关系），就存储密度来说，ArrayList是优于LinkedList的。 +　　 当操作是在一列数据的后面添加数据而不是在前面或中间,并且需要随机地访问其中的元素时,使用ArrayList会提供比较好的性能，
当你的操作是在一列数据的前面或中间添加或删除数据,并且按照顺序访问其中的元素时,就应该使用LinkedList了。

## 10. Stack

![image-20220123001919413](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123001919413-db2567.png)

`Stack` 的实现极其简单。可以用几句话概括完：

1. `Stack` 直接继承至 `Vector`，在其基础之上，只是增加了栈相关的操作；
2. 在方法上使用 `synchronized` 来实现线程安全；

## 11. Vector

![image-20220123001945672](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123001945672-ec1cad.png)

`Vector` 内部实现与 `ArrayList` 类似，都是使用数组来存储元素。不同的是，`Vector` 在方法上加了 `synchronized` 修饰词，来实现线程安全。

## 12. Set

## 13. AbstractSet

## 14. SortedSet

### 14.1. Redis 的 SkipList

跳跃表是一种有序数据结构，支持平均 O(logN)、最坏 O(N) 复杂度的节点查找；大部分情况效率可以和平衡树相媲美，实现却比平衡树简单。

跳跃表就是 Redis 中有序集合键的底层实现之一。

server.h

```c
 typedef struct zskiplistNode {
    sds ele;
    double score;
    struct zskiplistNode backward;
    struct zskiplistLevel {
        struct zskiplistNode forward;
        unsigned long span;
    } level[];
} zskiplistNode;

typedef struct zskiplist {
    struct zskiplistNode header, tail;
    unsigned long length;
    int level;
} zskiplist;

typedef struct zset {
    dict dict;
    zskiplist zsl;
} zset; 
```

skiplist，顾名思义，首先它是一个list。实际上，它是在有序链表的基础上发展起来的。

![image-20220123002050601](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123002050601-4cd3ce.png)

当我们想查找数据的时候，可以先沿着跨度大的链进行查找。当碰到比待查数据大的节点时，再回到跨度小的链表中进行查找。

skiplist正是受这种多层链表的想法的启发而设计出来的。按照上面生成链表的方式，上面每一层链表的节点个数，是下面一层的节点个数的一半，这样查找过程就非常类似于一个二分查找，使得查找的时间复杂度可以降低到 O(logN)。但是，存在的一个问题是：如果插入新节点后就会打乱上下相邻两层节点是 2:1 的对应关系。如果要维持，则需要调整后面所有的节点。

skiplist为了避免这一问题，它不要求上下相邻两层链表之间的节点个数有严格的对应关系，而是为每个节点随机出一个层数(level)。

![image-20220123002114552](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123002114552-e5ee83.png)

![image-20220123002145840](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123002145840-d2ee6e.png)

插入操作只需要修改插入节点前后的指针，而不需要对很多节点都进行调整。这就降低了插入操作的复杂度。实际上，这是 skiplist 的一个很重要的特性，这让它在插入性能上明显优于平衡树的方案。

skiplist，翻译成中文，可以翻译成“跳表”或“跳跃表”，指的就是除了最下面第1层链表之外，它会产生若干层稀疏的链表，这些链表里面的指针故意跳过了一些节点（而且越高层的链表跳过的节点越多）。这就使得我们在查找数据的时候能够先在高层的链表中进行查找，然后逐层降低，最终降到第1层链表来精确地确定数据位置。在这个过程中，我们跳过了一些节点，从而也就加快了查找速度。

> 1. 在中间插入一个有比较高 Level 的节点，如何维护前面节点到这个节点的这些链接？
> 2. 在平衡树种，如何做范围查找？先确定边界，然后其他节点怎么查找？



![image-20220123002219727](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123002219727-15a03c.png)

1. skiplist 中 key 允许重复。
2. 在比较时，不仅比较分数（即key），还要比较数据自身。
3. 第一层链表是双向链表，并且反向指针只有一个。
4. 在 skiplist 中可以很方便计算每个元素的排名。

**Redis 中的有序集合（sorted set），是在 skiplist, dict 和 ziplist 基础上构建起来的:**

1. 当数据较少时，sorted set是由一个 ziplist 来实现的。其中集合元素按照分值从小到大排序。
2. 当数据多的时候，sorted set 是由一个叫 zset 的数据结构来实现的，这个 zset 包含一个 dict + 一个 skiplist。dict 用来查询数据到分数(score)的对应关系，而 skiplist 用来根据分数查询数据（可能是范围查找）。

转换的条件是：

1. 有序集合保存的元素数量小于 128 个；（通过参数 `zset-max-ziplist-entries` 来调节，默认为 128。）
2. 有序集合保存的所有元素成员的长度都要小于 64 个字节；（通过参数 `zset-max-ziplist-value` 来调节，默认为 64。）

在 `t_zset.c/zsetConvert` 中执行转换操作。

```bash
 $ redis-cli --raw

127.0.0.1:6379> ZADD NameRanking 1 "linKe"
1

127.0.0.1:6379> ZADD NameRanking 2 "https://www.woniuxy.com"
1

127.0.0.1:6379> ZADD NameRanking 3 "https://github.com/linKe"
1

127.0.0.1:6379> ZRANGE NameRanking 0 -1 WITHSCORES
linKe
1
https://www.woniuxy.com
2
https://github.com/linKe
3

127.0.0.1:6379> TYPE NameRanking
zset

127.0.0.1:6379> OBJECT encoding NameRanking
ziplist

127.0.0.1:6379> ZADD NameRanking 4 "1234567890123456789012345678901234567890123456789012345678901234"
1

127.0.0.1:6379> ZRANGE NameRanking 0 -1 WITHSCORES
linKe
1
https://www.woniuxy.com
2
https://github.com/linKe
3
1234567890123456789012345678901234567890123456789012345678901234
4

127.0.0.1:6379> OBJECT encoding NameRanking
ziplist

127.0.0.1:6379> ZADD NameRanking 5 "12345678901234567890123456789012345678901234567890123456789012345"
1

127.0.0.1:6379> ZRANGE NameRanking 0 -1 WITHSCORES
linKe
1
https://www.woniuxy.com
2
https://github.com/linKe
3
1234567890123456789012345678901234567890123456789012345678901234
4
12345678901234567890123456789012345678901234567890123456789012345
5

127.0.0.1:6379> OBJECT encoding NameRanking
skiplist

127.0.0.1:6379> TYPE NameRanking
zset
```

在 JDK 中，也有 skiplist 的实现，在 `ConcurrentSkipListMap` 中。不过，它不是作为一个独立的 `Collection` 来实现的，而是作为 `Map` 的一部分来实现的。

### 14.2. 参考资料

1. [William Pugh《Skip Lists: A Probabilistic Alternative to Balanced Trees》](ftp://ftp.cs.umd.edu/pub/skipLists/skiplists.pdf)
2. [Redis为什么用跳表而不用平衡树？- 张铁蕾](https://mp.weixin.qq.com/s?__biz=MzA4NTg1MjM0Mg==&mid=2657261425&idx=1&sn=d840079ea35875a8c8e02d9b3e44cf95&scene=21#wechat_redirect)

## 15. NavigableSet

## 16. HashSet

### 16.1. Redis 中的 Set

Redis 中的集合对象编码可以是：

1. intset
2. hashtable

转换的条件是：

1. 集合对象保存的所有元素都是整数值；
2. 集合对象保存的元素个数不超过 512 个；（通过参数 `set-max-intset-entries` 来调整，默认是 512）

```bash
127.0.0.1:6379> SADD num 1 3 5
(integer) 3
127.0.0.1:6379> OBJECT encoding num
"intset"

127.0.0.1:6379> sadd num "seven"
(integer) 1
127.0.0.1:6379> OBJECT encoding num
"hashtable"
```

在 `t_set.c/setTypeConvert` 中执行转换操作。

## 17. TreeSet

## 18. LinkedHashSet

## 19. BitSet

## 20. EnumSet

## 21. Map

## 22. SortedMap

## 23. NavigableMap

## 24. AbstractMap

## 25. HashMap

### 25.1. HashMap 问题集

1. 如果判断是否到达容量阈值？
2. 为什么
3. 遍历查找时，如果保证可以让红黑树的节点也可以使用next方法来查找？

### 25.2. Redis 中的字典

Redis 底层中的字典就是一个典型的 Hash 实现。

dict.h

```c
typedef struct dictEntry { 
    void key;
    union {
        void val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v;
    struct dictEntry next;
} dictEntry;

typedef struct dictType {
    uint64_t (hashFunction)(const void key);
    void (keyDup)(void privdata, const void key);
    void (valDup)(void privdata, const void obj);
    int (keyCompare)(void privdata, const void key1, const void key2);
    void (keyDestructor)(void privdata, void key);
    void (valDestructor)(void privdata, void obj);
} dictType;

/ This is our hash table structure. Every dictionary has two of this as we
 * implement incremental rehashing, for the old to the new table. /
typedef struct dictht {
    dictEntry *table; 
    unsigned long size;
    unsigned long sizemask;
    unsigned long used;
} dictht;

typedef struct dict {
    dictType type;
    void privdata;
    dictht ht[2]; 
    long rehashidx; /* rehashing not in progress if rehashidx == -1 /
    unsigned long iterators; / number of iterators currently running */
} dict;
```

| 1    | `dictEntry` 保存一个键值对。                                 |
| ---- | ------------------------------------------------------------ |
| 2    | `table` 属性是一个数组，数组中每个元素都是一个指向 `dictEntry` 结构的指针。 |
| 3    | 通常使用 `ht[0]`，`ht[1]` 在 Rehash 时才会用到。             |

添加新元素时，和 Java 一样，计算 Key 的哈希值，然后再根据哈希值与长度掩码（`sizemask`）相与得到数组下标。

Redis 底层使用 [MurmurHash2](https://en.wikipedia.org/wiki/MurmurHash) 算法来计算键的哈希值。

#### 25.2.1. Rehash 操作

1. 计算新的数组长度
   1. 如果是扩容，则 `used * 2`；
   2. 如果是缩容，则是第一个大于等于 `used` 的 2n。 — 这点和 Java 不同，`HashMap` 中没有自动缩容的机制。
2. 将 `ht[0]` 中的所有键值对重新 Rehash，重新计算哈希值和索引值，放置到 `ht[1]` 上；
3. 迁移完成后，将 `ht[1]` 设置为 `ht[0]`，为 `ht[1]` 创建一个空白哈希表。

还有几点需要特别注意：

1. 根据是否正在执行 `BGSAVE` 或 `BGREADWRITEAOF` 命令，使用不同的负载阈值来决定是否开启对哈希表的自动扩展工作；
2. 当哈希表负载因子小于 0.1 时，会自动开始对哈希表缩容；
3. Rehash 过程是渐进式的：
   1. 开始 Rehash 后，每次对自动进行的添加、删除、查找或更新时，程序会自动将对应的键值对从 `ht[0]` Rehash 到 `ht[1]` 上；rehashidx 属性值增一。
   2. 记得有后台定时任务来自动扩展的，怎么没有看到说明文档？

Redis 在哈希对象上的编码有可能是：

1. ziplist
2. hashtable

转换条件是：

1. 哈希对象保存的所有键值对象字符串长度都小于 64 个字节；（通过参数 `hash-max-ziplist-value` 来调节，默认为 64）
2. 哈希对象保存的键值对数量小于 512 个；（通过参数 `hash-max-ziplist-entries` 来调节，默认为 512）

```bash
 $ redis-cli --raw

127.0.0.1:6379> HMSET profile name "lin" site "https://www.woniuxy.com" job "Developer"
OK

127.0.0.1:6379> TYPE profile
hash

127.0.0.1:6379> OBJECT encoding profile
ziplist

127.0.0.1:6379> HSET profile address "1234567890123456789012345678901234567890123456789012345678901234" 
1

127.0.0.1:6379> HVALS profile
linKe
https://www.woniuxy.com
Developer
1234567890123456789012345678901234567890123456789012345678901234
127.0.0.1:6379> OBJECT encoding profile
ziplist

127.0.0.1:6379> HSET profile address "12345678901234567890123456789012345678901234567890123456789012345" 
0

127.0.0.1:6379> HVALS profile
https://www.woniuxy.com
linKe
12345678901234567890123456789012345678901234567890123456789012345
Developer

127.0.0.1:6379> OBJECT encoding profile
hashtable
```

| 1    | 这是 64 个字符。 |
| ---- | ---------------- |
| 2    | 这是 65 个字符。 |

通过 `t_hash.c/hashTypeConvertZiplist` 方法来转换。

### 25.3. 参考资料

1. [Redis内部数据结构详解(1)——dict](https://mp.weixin.qq.com/s?__biz=MzA4NTg1MjM0Mg==&mid=2657261203&idx=1&sn=f7ff61ce42e29b874a8026683875bbb1&scene=21#wechat_redirect)

### 25.4. 简介

>  HashMap是基于哈希表实现的，用来存储key-value形式的键值对，允许key和value都为null值； HashMap是非线程安全的，只是用于单线程环境下，多线程环境下可以采用concurrent并发包下的concurrentHashMap； HashMap实现了Serializable接口，支持序列化，实现了Cloneable接口，能被克隆。

### 25.5. 签名

```java
public class HashMap<K,V>
    extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable
```

**可以看到HashMap 实现了Cloneable和Serializable标记接口：**

- 标记接口Cloneable，用于表明HashMap对象会重写java.lang.Object#clone()方法，HashMap实现的是浅拷贝（shallow copy）。
- 标记接口Serializable，用于表明HashMap对象可以被序列化。

**HashMap继承了AbstractMap抽象类，同时也实现了Map接口。**

|      | 在语法层面继承接口Map是多余的，这么做仅仅是为了让阅读代码的人明确知道HashMap是属于Map体系的，起到了文档的作用。 AbstractMap相当于个辅助类，Map的一些操作这里面已经提供了默认实现，后面具体的子类如果没有特殊行为，可直接使用AbstractMap提供的实现。 |
| :--- | ------------------------------------------------------------ |

AbstractMap相当于个辅助类，Map的一些操作这里面已经提供了默认实现，后面具体的子类如果没有特殊行为，可直接使用AbstractMap提供的实现。

接口java.util.Map,主要有四个常用的实现类，分别是HashMap、Hashtable、LinkedHashMap和TreeMap，类继承关系如下图所示：

![map structure](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/map_structure-cd3038.png)

**下面针对各个实现类的特点做一些说明：**

1. (1) HashMap：它根据键的hashCode值存储数据，大多数情况下可以直接定位到它的值，因而具有很快的访问速度，但遍历顺序却是不确定的。
   HashMap最多只允许一条记录的键为null，允许多条记录的值为null。HashMap非线程安全，即任一时刻可以有多个线程同时写HashMap，
   可能会导致数据的不一致。如果需要满足线程安全，可以用 Collections的synchronizedMap方法使HashMap具有线程安全的能力，
   或者使用ConcurrentHashMap。
2. (2) Hashtable：Hashtable是遗留类，很多映射的常用功能与HashMap类似，不同的是它承自Dictionary类，并且是线程安全的，
   任一时间只有一个线程能写Hashtable，并发性不如ConcurrentHashMap，因为ConcurrentHashMap引入了分段锁。
   Hashtable不建议在新代码中使用，不需要线程安全的场合可以用HashMap替换，需要线程安全的场合可以用ConcurrentHashMap替换。
3. (3) LinkedHashMap：LinkedHashMap是HashMap的一个子类，保存了记录的插入顺序，在用Iterator遍历LinkedHashMap时，先得到的记录肯定是先插入的，也可以在构造时带参数，按照访问次序排序。
4. (4) TreeMap：TreeMap实现SortedMap接口，能够把它保存的记录根据键排序，默认是按键值的升序排序，也可以指定排序的比较器，当用Iterator遍历TreeMap时，得到的记录是排过序的。如果使用排序的映射，建议使用TreeMap。在使用TreeMap时，key必须实现Comparable接口或者在构造TreeMap传入自定义的Comparator，否则会在运行时抛出java.lang.ClassCastException类型的异常。

对于上述四种Map类型的类，要求映射中的key是不可变对象。不可变对象是该对象在创建后它的哈希值不会被改变。如果对象的哈希值发生变化，Map对象很可能就定位不到映射的位置了。

通过上面的比较，我们知道了HashMap是Java的Map家族中一个普通成员，鉴于它可以满足大多数场景的使用条件，所以是使用频度最高的一个。下文我们主要结合源码，从存储结构、常用方法分析、扩容等方面了解一下HashMap的工作原理。

### 25.6. 存储结构

HashMap是基于哈希表存储的，在JDK1.6，JDK1.7版本采用数组(桶位) + 链表实现存储元素和解决冲突，同一hash值的链表都存储在一个链表里。 但是当位于一个桶中的元素较多，即hash值相等的元素较多时，通过key值依次查找的效率较低。但是到JDK1.8版本时HashMap采用位桶 + 链表 + 红黑树实现， 当链表长度超过阈值（8）时，将链表转换为红黑树，这样大大减少了查找时间。

### 25.7. 实现原理

首先有一个元素是链表的数组，当添加一个元素（key-value）时，就首先计算元素key的hash值，以此确定元素在数组中的位置，但是可能存在同一hash值的元素 已经被放在数组同一位置了（也就出现了Hash冲突），这时就添加到同一hash值的元素的后面，他们在数组的同一位置，但是形成了链表，同一个链表上的Hash值是 相同的，所以说数组存放的是链表。而当链表长度太长时，链表就转换为红黑树，这样大大提高了查找的效率。
当链表数组的容量超过初始容量的0.75（阀值）时，将链表数组扩大2倍，然后把原来数组中的链表重新散列，把原链表数组中的元素迁移到新的数组中。

HashMap原理图：





### 25.8. 源码剖析

#### 25.8.1. 重要属性

```java
/**
 *  序列号
 */
private static final long serialVersionUID = 362498820763181265L;

/**
 *  默认初始容量（容量为HashMap中槽的数目）是16，且必须是2的整数次幂。
 */
static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

/**
 * 最大容量（必须是2的幂且小于2的30次方，传入容量过大将被这个值替换）
 */
static final int MAXIMUM_CAPACITY = 1 << 30;

/**
 * 默认装载因子为0.75
 */
static final float DEFAULT_LOAD_FACTOR = 0.75f;

/**
 * 当put一个元素到某个桶位，其链表长度达到8时将链表转换为红黑树
 */
static final int TREEIFY_THRESHOLD = 8;

/**
 * 一个桶位上的链表长度小于这个值时将红黑树转链表
 */
static final int UNTREEIFY_THRESHOLD = 6;

/**
 * 树的最小的容量，至少是 4 x TREEIFY_THRESHOLD = 32
 * 然后为了避免(resizing 和 treeification thresholds) 设置成64
 */
static final int MIN_TREEIFY_CAPACITY = 64;

/**
 * 实际存放元素的个数，不等于数组的长度
 */
transient int size;

/**
 * 达到这个阈值就要进行扩容，其等于容量 * 装载因子
 */
int threshold;

/**
 * 实际装载因子
 */
final float loadFactor;

/**
 * 每次扩容和更改map结构的计数器
 * 如果在使用迭代器的过程中有其他线程修改了map，将抛出ConcurrentModificationException，
 * 这就是所谓fail-fast策略（速错），这一策略的实现就是通过modCount
 */
transient int modCount;

/*
 * 存放具体key-value对元素的集和
 */
transient Set<Map.Entry<K,V>> entrySet;

/*
 * 存储元素的数组，总是2的幂次倍
 */
transient Node<K,V>[] table;
```

**加载因子**

> 加载因子（默认0.75）：为什么需要使用加载因子，为什么需要扩容呢？因为如果加载因子很大， 说明利用的空间很多，如果一直不进行扩容的话，链表就会越来越长，这样查找的效率很低， 因为链表的长度很大（当然最新版本使用了红黑树后会改进很多），扩容之后，将原来链表数 组的每一个链表分成奇偶两个子链表分别挂在新链表数组的散列位置，这样就减少了每个链表 的长度，增加查找效率。HashMap本来是以空间换时间，所以装载因子没必要太大。但是装载因子太小 又会导致空间浪费。如果关注内存，装载因子可以稍大，如果主要关注查找性能，装载因子可以稍小。

#### 25.8.2. 数据结构

- 桶位数组

```java
/**
 * 1.存储元素（桶位）的数组
 */
transient Node<k,v>[] table;
```

- 数组元素Node<K,V>

```java
//Node是单向链表，它实现了Map.Entry接口
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;  //下一个节点

    Node(int hash, K key, V value, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.value = value;
        this.next = next;
    }

    public final K getKey()        { return key; }
    public final V getValue()      { return value; }
    public final String toString() { return key + "=" + value; }

    public final int hashCode() {
        return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    public final V setValue(V newValue) {
        V oldValue = value;
        value = newValue;
        return oldValue;
    }

    public final boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof Map.Entry) {
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                return true;
        }
        return false;
    }
}
```

|      | 其实Node就是一个基于单向链表数据结构的存储key和value的一个对象。next指向下一个Node.实现了Map.Entry接口 |
| ---- | ------------------------------------------------------------ |

- 红黑树

```
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
    TreeNode<k,v> parent;  //父节点
    TreeNode<k,v> left;    //左子树
    TreeNode<k,v> right;   //右子树
    TreeNode<k,v> prev;    // needed to unlink next upon deletion
    boolean red;           //颜色属性
    TreeNode(int hash, K key, V val, Node<K,V> next) {
        super(hash, key, val, next);
    }

    /**
     * 返回当前节点的根节点
     */
    final TreeNode<K,V> root() {
        for (TreeNode<K,V> r = this, p;;) {
            if ((p = r.parent) == null)
                return r;
            r = p;
        }
    }
```

**transient 关键字**

>  Java序列化会把某一个类存储以文件形式存储在物理空间，但是以文件形式存储某些信息时，容易涉及到安全问题，因为数据位于Java运行环境之外， 不在Java安全机制的控制之中。对于这些需要保密的字段，不应保存在永久介质中 ，或者不应简单地不加处理地保存下来 ，为了保证安全性。 应该在这些字段前加上transient关键字。它的意思是临时的，即不会随类一起序列化到本地，所以当还原后，这个关键字定义的变量也就不再存在。

#### 25.8.3. 构造函数

- 默认构造函数HashMap()

```java
public HashMap() {
  //初始话加载因子为默认0.75；其他属性均为默认
  this.loadFactor = DEFAULT_LOAD_FACTOR;
  // all other fields defaulted
}
```

|      | 这是一个默认构造器，潜在的问题是初始容量16太小了，可能中间需要不断扩容的问题，会影响插入的效率。 |
| ---- | ------------------------------------------------------------ |

- ja指定初始容量和加载因子的构造函数HashMap(int, float)

```java
public HashMap(int initialCapacity, float loadFactor) {
    //初始容量不能小于0
    if (initialCapacity < 0)
        throw new IllegalArgumentException("Illegal initial capacity: " +
                initialCapacity);
    // 初始容量不能大于最大值，否则为最大值
    if (initialCapacity > MAXIMUM_CAPACITY)
        initialCapacity = MAXIMUM_CAPACITY;
    // 填充因子不能小于或等于0，不能为非数字
    if (loadFactor <= 0 || Float.isNaN(loadFactor))
        throw new IllegalArgumentException("Illegal load factor: " +
                loadFactor);
    //初始话加载因子
    this.loadFactor = loadFactor;
    //初始化(阀值)threshold，数组元素数量达到该值时会扩容
    this.threshold = tableSizeFor(initialCapacity);
}

/**
 * tableSizeFor的功能主要是用来保证容量应该大于cap,且为2的整数
 */
static final int tableSizeFor(int cap) {
      int n = cap - 1;
      n |= n >>> 1;
      n |= n >>> 2;
      n |= n >>> 4;
      n |= n >>> 8;
      n |= n >>> 16;
      return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
}
```

1. *这里可能还有一个疑问，明明给的是初始容量，为什么要计算阀值，而不是容量呢？*

   其实这也是jdk1.8的改变，它将table的初始化放入了resize()中，而且压根就没有capacity这个属性， 所以这里只能重新计算threshold，而resize()后面就会根据threshold来重新计算capacity，来进行 table数组的初始化，然后在重新按照装载因子计算threshold。

|      | 可以指定初始容量，以及装载因子，但是一般情况下指定装载因子意义不大，采用默认0.75就可以。 |
| ---- | ------------------------------------------------------------ |

- 指定初始容量的构造函数HashMap(int initialCapacity)

```java
public HashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
}
```

|      | 用这种构造函数创建HashMap的对象，如果知道map要存放的元素个数，可以直接指定容量的大小， 减除不停的扩容，提高效率 |
| ---- | ------------------------------------------------------------ |

- 将已有Map放入当前map的构造函数HashMap(Map<? extends K, ? extends V> m)

```java
public HashMap(Map<? extends K, ? extends V> m) {
   this.loadFactor = DEFAULT_LOAD_FACTOR;  //初始化加载因子
   putMapEntries(m, false);
}

// 其实就是一个一个取出m中的元素调用putVal,一个个放入table中的过程。
final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
    int s = m.size();
    if (s > 0) {
        if (table == null) { // pre-size
            float ft = ((float)s / loadFactor) + 1.0F;
            int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                    (int)ft : MAXIMUM_CAPACITY);
            if (t > threshold)
                threshold = tableSizeFor(t);
        }
        else if (s > threshold)   //如果m中的元素个数大于阀值，调用resize进行扩容
            resize();
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            K key = e.getKey();
            V value = e.getValue();
            putVal(hash(key), key, value, false, evict);  //调用putVal向map中添加元素
        }
    }
}
```

#### 25.8.4. HashMap存取机制

##### 1.添加元素

```java
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);    //调用putVal()方法
}
```

JDK1.8计算hash值

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

JDK1.7计算hash值

```java
final int hash(Object k) {
    int h = hashSeed;
    if (0 != h && k instanceof String) {
        return sun.misc.Hashing.stringHash32((String) k);
    }
    h ^= k.hashCode();
    h ^= (h >>> 20) ^ (h >>> 12);
    return h ^ (h >>> 7) ^ (h >>> 4);
}
```

|      | JDK1.8计算hash值的方法进行了改进，取得key的hashcode后，高16位与低16位异或运算重新计算hash值。 key有可能是null，key为null时，hash值为0，放在数组的0位置。 |
| ---- | ------------------------------------------------------------ |

- putVal()方法

  执行过程如图：

![image-20220123003517331](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123003517331-146e44.png)



```java
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    //table未初始化或者长度为0，进行扩容
    if ((tab = table) == null || (n = tab.length) == 0)
        //可以看到put元素时，如果数组没有初始化，会调用resize()方法进行初始化。后面分析resize()方法
        n = (tab = resize()).length;

    /*
     * 这里就是HASH算法了，用来定位桶位的方式，可以看到是采用容量-1和键的hash值进行与运算
     * n-1,的原因就是n一定是一个2的整数幂，而(n - 1) & hash其实质就是n%hash,但是取余运算
     * 的效率明显不如位运算与，并且(n - 1) & hash也能保证散列均匀，不会产生只有偶数位有值的现象
     */
    if ((p = tab[i = (n - 1) & hash]) == null)
        /*
         * 当这里是空桶位时，就直接构造新的Node节点，将其放入桶位中(此时，这个结点是放在数组中)
         * newNode()方法，就是对new Node(,,,)的包装,同时也可以看到Node中的hash值就是重新计算的hash(key)
         */
        tab[i] = newNode(hash, key, value, null);
    else {
        //桶中已经存在元素
        Node<K,V> e; K k;
        // 比较桶中第一个元素(数组中的结点)的hash值相等，key相等
        if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
            //比较桶中第一个元素(数组中的结点)的hash值相等，key相等
            e = p;
        else if (p instanceof TreeNode)
            // hash值不相等，即key不相等；为红黑树结点
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);  // 放入树中
        else {
            // 为链表结点
            // 在链表最末插入结点
            for (int binCount = 0; ; ++binCount) {
              // 到达链表的尾部
                if ((e = p.next) == null) {
                    // 在尾部插入新结点
                    p.next = newNode(hash, key, value, null);
                    // 结点数量达到阈值，转化为红黑树
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    break; // 跳出循环
                }
                // 判断链表中结点的key值与插入的元素的key值是否相等
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                    break;   // 相等，跳出循环
                // 用于遍历桶中的链表，与前面的e = p.next组合，可以遍历链表
                p = e;
            }
        }
        // 表示在桶中找到key值、hash值与插入元素相等的结点
        if (e != null) { // existing mapping for key
            V oldValue = e.value;  // 记录e的value
            // onlyIfAbsent为false或者旧值为null
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;  //用新值替换旧值
            afterNodeAccess(e);   // 访问后回调
            return oldValue;      // 返回旧值
        }
    }
    // 结构性修改
    ++modCount;
    // 实际大小大于阈值则扩容
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);  // 插入后回调
    return null;  // 返回null
}
```

- resize()方法

```java
final Node<K,V>[] resize() {
    // 当前table保存
    Node<K,V>[] oldTab = table;
    // 保存table大小
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    // 保存当前阈值
    int oldThr = threshold;
    int newCap, newThr = 0;
    // 之前table大小大于0
    if (oldCap > 0) {
        // 之前table大于最大容量
        if (oldCap >= MAXIMUM_CAPACITY) {
            // 阈值为最大整形
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 容量翻倍，使用左移，效率更高
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
            oldCap >= DEFAULT_INITIAL_CAPACITY)
            // 阈值翻倍
            newThr = oldThr << 1; // double threshold
    }
    // 之前阈值大于0
    else if (oldThr > 0)
        newCap = oldThr;
    // oldCap = 0并且oldThr = 0，使用缺省值（如使用HashMap()构造函数，之后再插入一个元素会调用resize函数，会进入这一步）
    else {
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }
    // 新阈值为0
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                  (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
    // 初始化table
    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    // 之前的table已经初始化过
    if (oldTab != null) {
        // 复制元素，重新进行hash
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else { // preserve order
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    // 将同一桶中的元素根据(e.hash & oldCap)是否为0进行分割，分成两个不同的链表，完成rehash
                    do {
                        next = e.next;
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

|      | 扩容实际上就是创建一个容量是原来容量两倍的数组， 把原来数组中的元素经过重新散列，然后添加到新的数组中。 扩容会伴随着一次重新hash分配，并且会遍历hash表中所有 的元素，是非常耗时的。在编写程序中，要尽量避免resize。 |
| ---- | ------------------------------------------------------------ |

- putAll()方法

```java
public void putAll(Map<? extends K, ? extends V> m) {
  //内部也是调用putVal()方法，将m中的元素循环放入table中
  putMapEntries(m, true);
}
```

##### 获取元素

```java
/**
 * 通过key获取value
 */
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        //如果Node链表的第一个元素相等
        if (first.hash == hash && // always check first node
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        if ((e = first.next) != null) {
            //红黑树查找
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            //链表查找
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    //找不到返回null
    return null;
}

/**
 * 判断是否包含指定key
 */
public boolean containsKey(Object key) {
    return getNode(hash(key), key) != null;  //返回node是否为null
}

/**
 * 判断是否包含指定value
 */
public boolean containsValue(Object value) {
    Node<K,V>[] tab; V v;
    if ((tab = table) != null && size > 0) {
        for (int i = 0; i < tab.length; ++i) {
            //按照单链表的方式进行遍历，
            //因为HashMap中 TreeNode 节点也存在next成员，可以用链表的方式进行遍历
            for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                if ((v = e.value) == value ||
                        (value != null && value.equals(v)))
                    return true;
            }
        }
    }
    return false;
}
```

|      | get方法相对put要简单的多，分析源码可以看出hash算法的精髓，不用遍历就可以直接通过 计算key的hash值，得到查找元素在数组中的桶位，然后比较hash值、key是否相等来获取node。 |
| ---- | ------------------------------------------------------------ |

##### 移除元素

```java
public V remove(Object key) {
    Node<K,V> e;
    return (e = removeNode(hash(key), key, null, false, true)) == null ? null : e.value;
}

final Node<K,V> removeNode(int hash, Object key, Object value,
                           boolean matchValue, boolean movable) {
    Node<K,V>[] tab; Node<K,V> p; int n, index;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (p = tab[index = (n - 1) & hash]) != null) {
        //node就是要查找的结点
        Node<K,V> node = null, e; K k; V v;
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            node = p;
        else if ((e = p.next) != null) {
            if (p instanceof TreeNode)
                node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
            else {
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key ||
                         (key != null && key.equals(k)))) {
                        node = e;
                        break;
                    }
                    //这里p保存的是父节点，因为这里涉及到链表删除的操作
                    p = e;
                } while ((e = e.next) != null);
            }
        }
        /*
         * 当matchValue为false时，直接短路后面的运算，
         * 进行删除操作，而不用关注value值是否相等或者equals
         */
        if (node != null && (!matchValue || (v = node.value) == value ||
                             (value != null && value.equals(v)))) {
            if (node instanceof TreeNode)
                //movable用在树的删除上
                ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
            else if (node == p)
                 //要删除节点就是链表的头节点，则将子节点放进桶位
                tab[index] = node.next;
            else
                //删除节点后节点，父节点的next重新连接
                p.next = node.next;
            ++modCount; //删除操作也是要记录进modCount
            --size;
            afterNodeRemoval(node);
            return node;
        }
    }
    return null;
}

/**
 * jdk1.8新增的重载方法，matchValue为true时，
 * 只有当key和value都相等时，才会删除
 */
public boolean remove(Object key, Object value) {
    return removeNode(hash(key), key, value, true, true) != null;
}
```

### 25.9. 小结

本文对JDK1.8 HashMap的原代码进行了简要的分析，主要目的是了解其内部的 存储机制和实现原理，从而达到在编程中更高效的使用HashMap。

HashMap 内部是基于一个数组来实现的，数组中的每个元素称为一个桶(bucket)。 当数组中被占用的桶的数量超过了装载因子和数组容量设定的阈值后，会对数组进行扩容， 容量将扩展为原来的2倍。哈希表中所有的 Entry 会被重新散列到新的位置中。

因为两个不同的key在散列时有可能发生冲突，HashMap为了避免哈希冲突带来的影响 做了几点优化。在进行散列处理时，将高位与低位进行异或，从而减小冲突的概率。 当不同的node被散列到同一个桶中时，每个桶中使用单向链表的方式来保存数据。 在Java 8 的实现中，如果一个桶中的Node数量超过了阈值(TREEIFY_THRESHOLD = 8)， 就会将单链表转化为红黑树，当低于阈值(UNTREEIFY_THRESHOLD = 6)时重新转化为 单链表。

分析了HashMap的resize方法可以知道，HashMap在进行扩容时是非常耗性能的操作， 所以在使用HashMap的时候，应该先估算一下map的大小，初始化的时候给一个大致的数值， 避免map进行频繁的扩容。

### 25.10. 参考

1. [Java 容器源码分析之 HashMap](http://blog.jrwang.me/2016/java-collections-hashmap/)
2. [JDK1.8源码分析之HashMap（一）](http://www.tuicode.com/article/56da289f8e6d72823e30a024)
3. [Java中HashMap底层实现原理(JDK1.8)源码分析](http://blog.csdn.net/tuke_tuke/article/details/51588156)
4. [基于jdk1.8的HashMap源码学习笔记](http://www.cnblogs.com/ToBeAProgrammer/p/4787761.html)
5. [Java 8系列之重新认识HashMap](http://tech.meituan.com/java-hashmap.html)

### 25.11. JDK 1.8 的实现

**继上一章介绍了HashMap的签名、数据结构以及存储原理之后，相信大家对HashMap有了更加深入的理解，在使用时也会得心应手。**
**本章将继续介绍HashMap的使用，主要是分析HashMap的三种遍历方式。**

### 25.12. HashMap遍历

------

- **HashMap提供了三种遍历方式：**
  1. 遍历所有的Key：Set<K> keySet()
  2. 遍历所有的Entry：Set<Map.Entry<K,V>> entrySet()
  3. 遍历所有的Value（不常用）：Collection<V> values()

这三个方法的基本用法将不在详细介绍，它们都是返回可迭代的Set或者Collection。要弄清楚这三个方法 的内部实现机制，首先主要来看一下内部抽象类#HashIterator#。

- HashIterator内部类：

```java
abstract class HashIterator {
    Node<K,V> next;        // next entry to return
    Node<K,V> current;     // current entry
    int expectedModCount;  // for fast-fail
    int index;             // current slot

    HashIterator() {
        //用expectedModCount保存刚创建迭代器时的modCount，
        //实现fail-fast机制需要对比该值和使用时的modCount
        expectedModCount = modCount;
        Node<K,V>[] t = table;
        current = next = null;
        index = 0;
        //找到第一个有效的槽
        if (t != null && size > 0) { // advance to first entry
            do {} while (index < t.length && (next = t[index++]) == null);
        }
    }

    public final boolean hasNext() {
        return next != null;
    }

    final Node<K,V> nextNode() {
        Node<K,V>[] t;
        Node<K,V> e = next;
        /*
         * fail-fast 检查
         * 当另外一个线程对当前Map修改时，会修改modCount，
         * 当前线程遍历正在，如果expectedModCount和modCount
         * 不相等，就会抛出ConcurrentModificationException异常
         */
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        // table数组中没有元素，抛出NoSuchElementException异常
        if (e == null)
            throw new NoSuchElementException();
        //next = e.next
        //遍历是通过单链表的方式来访问的，即便是红黑树也可以这样来遍历
        //TreeNode中也存在next引用，也可以看做单链表
        if ((next = (current = e).next) == null && (t = table) != null) {
            //如果到达当前链表末尾next == null
            //寻找下一个有效的槽
            do {} while (index < t.length && (next = t[index++]) == null);
        }
        return e;
    }

    public final void remove() {
        Node<K,V> p = current;
        if (p == null)
            throw new IllegalStateException();
        //fail-fast 检查
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();
        current = null;
        K key = p.key;
        //调用removeNode移除Entry
        removeNode(hash(key), key, null, false, false);
        //更新expectedModCount
        expectedModCount = modCount;
    }
}

final class KeyIterator extends HashIterator
    implements Iterator<K> {
    public final K next() { return nextNode().key; }  //返回Key
}

final class ValueIterator extends HashIterator
    implements Iterator<V> {
    public final V next() { return nextNode().value; } // 返回Value
}

final class EntryIterator extends HashIterator
    implements Iterator<Map.Entry<K,V>> {
    public final Map.Entry<K,V> next() { return nextNode(); } // 返回Entry
}
```

**KeyIterator、ValueIterator 和 EntryIterator 都继承了 HashIterator，区别只在于 next() 方法返回的是 Key、Value 还是 Entry。**

- Set<Map.Entry<K,V>> entrySet()

```java
public Set<Map.Entry<K,V>> entrySet() {
    Set<Map.Entry<K,V>> es;
    return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
}

final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
  public final int size()                 { return size; }
  public final void clear()               { HashMap.this.clear(); }
  //返回一个迭代器
  public final Iterator<Map.Entry<K,V>> iterator() {
    return new EntryIterator();
  }
  public final boolean contains(Object o) {
    if (!(o instanceof Map.Entry))
      return false;
    Map.Entry<?,?> e = (Map.Entry<?,?>) o;
    Object key = e.getKey();
    Node<K,V> candidate = getNode(hash(key), key);
    return candidate != null && candidate.equals(e);
  }
  public final boolean remove(Object o) {
    if (o instanceof Map.Entry) {
      Map.Entry<?,?> e = (Map.Entry<?,?>) o;
      Object key = e.getKey();
      Object value = e.getValue();
      return removeNode(hash(key), key, value, true, true) != null;
    }
    return false;
  }
  public final Spliterator<Map.Entry<K,V>> spliterator() {
    return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
  }
  public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
    Node<K,V>[] tab;
    if (action == null)
      throw new NullPointerException();
    if (size > 0 && (tab = table) != null) {
      int mc = modCount;
      for (int i = 0; i < tab.length; ++i) {
        for (Node<K,V> e = tab[i]; e != null; e = e.next)
          action.accept(e);
      }
      if (modCount != mc)
        throw new ConcurrentModificationException();
    }
  }
}
```

理解了 HashIterator 后再看 entrySet() 和 EntrySet 类就比较容易理解了，注意到 HashMap 的实现中使用了一个 entrySet 成员来缓存结果。 keySet() 和 values() 的实现也是类似的，只是 values() 返回的是 Collection ，因为值不能保证唯一性，而键是可以的。

### 25.13. 注意

**对于Map中的Key是包装类型时，从map总get元素时要特别注意，get元素的key也必须是对应的包装类型，否者不能获得到对应的value。 因为get方法内部通过key查找对应的value时，key用的是equals方法比较，所以key的数据类型也必须相同。
例如：**

```java
long key = 123;
Map<Long,String> map = Maps.newHashMap();
map.put(123L,"java");
String value = map.get(key);  // value是null？还是java？
```

#### 25.13.1. 小结

有关HashMap的遍历就介绍这写，遍历HashMap一共有三种方式，一般遍历key和遍历Entry用的比较多，而且遍历Entry要比遍历 key效率要更快些。对于HashMap的源码暂时就分析这么多，由于本人还是一个菜鸟，水平有限，有些地方也许没有分析的透彻，希望 大家可以见谅，同时，本次HashMap的源码分析也有很多地方没有讲到，比如：HashMap的存储结构红黑树，以后有时间再来研究一下红黑树 的实现原理，这里先推荐一篇讲解红黑树的文章[红黑树深入剖析及Java实现](http://tech.meituan.com/redblack-tree.html)。

### 25.14. 参考

- [Java 容器源码分析之 HashMap - JR’s Blog](https://blog.jrwang.me/2016/java-collections-hashmap/)

## 26. TreeMap

先看一个问题：

```java
 @Test
    public void testQuestion() {
        TreeMap<Pair, Pair> data = new TreeMap<>();
        Pair pair = new Pair(1, System.currentTimeMillis());

        data.put(pair, pair);
        Pair value = data.get(new Pair(1));
        // 请问，这里会输出 true ？还是 false ？
        System.out.println(pair.equals(value));
    }

    class Pair implements Comparable<Pair> {
        int key;
        long time;

        public Pair(int key) {
            this.key = key;
        }

        public Pair(int key, long time) {
            this.key = key;
            this.time = time;
        }

        @Override
        public int compareTo(Pair o) {
            return Long.compare(this.time, o.time);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pair pair = (Pair) o;
            return key == pair.key;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
```

`TreeMap` 底层是一个红黑树。

先定义一个测试实体类：

```java
public static class Person {
        long id;
        int sortFactor;

        public Person(long id) {
            this(id, (int) id);
        }

        public Person(long id, int sortFactor) {
            this.id = id;
            this.sortFactor = sortFactor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Person person = (Person) o;
            return id == person.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Person{" +
                    "id=" + id +
                    ", age=" + sortFactor +
                    '}';
        }
    }
    
    
    
    
    @Test
    public void testSort() {
        Comparator<Person> comparator
                = Comparator.comparingInt(a -> a.sortFactor);
        TreeMap<Person, Person> map = new TreeMap<>(comparator);
        for (int i = 0; i < 10; i++) {
            if ((i & 1) == 1) {
                Person person = new Person(i);
                map.put(person, person);
            } else {
                Person param = new Person(i / 2);
                Person person = map.get(param);
                if (Objects.nonNull(person)) {
                    person.sortFactor = new Random().nextInt();
                }
            }
        }
        //
        map.forEach((k, v) -> {
            System.out.println(k);
        });
        System.out.println("-------------");
        for (Person person : map.navigableKeySet()) {
            System.out.println(person);
        }
        System.out.println("-------------");
        for (Person person : map.descendingKeySet()) {
            System.out.println(person);
        }
    }
    
    
    
```

修改排序字段，打印时，依然可以保持有序性。*这个实现是怎么回事？*

```java
@Test
    public void testDuplicateSortFactor() {
        Comparator<Person> comparator
                = Comparator.comparingInt(a -> a.sortFactor);
        TreeMap<Person, Person> treeMap = new TreeMap<>(comparator);
        Person p1 = new Person(1, 0);
        Person p2 = new Person(2, 0);
        assert !p1.equals(p2);
        System.out.println(p1.equals(p2));

        for (int i = 0; i < 10; i++) {
            Person person = new Person(i, 0);
            treeMap.put(person, person);
        }

        assert (treeMap.size() == 1);
        treeMap.forEach((k, v) -> {
            System.out.println("-----------------------");
            System.out.printf("kid= %-4d kfactor= %-8d%n", k.id, k.sortFactor);
            System.out.printf("vid= %-4d vfactor= %-8d%n", v.id, v.sortFactor);
        });
    }
```

由此看出，`TreeMap` 不能接受排序因子相同的值。如果存在，则后来者把前者的 `Value` 覆盖掉。

```java
@Test
    public void testPut() {
        TreeMap<Integer, Integer> treeMap = new TreeMap<>();
        for (int i = 0; i < 10; i++) {
            treeMap.put(i, i * 100);
        }
    }
```

## 27. LinkedHashMap

![image-20220123003920852](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123003920852-ea458b.png)

问题：

1. `new LinkedHashMap<>(10, 0.75F, true)` 和 `new LinkedHashMap<>()` 除了容量之外，还有什么差别吗？

## 28. Dictionary

## 29. Hashtable

![Hashtable lock](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/Hashtable-lock-017d9e.png)

### 29.1. 参考资料

- [ConcurrentHashMap实现原理及源码分析 - dreamcatcher-cx - 博客园](https://www.cnblogs.com/chengxiao/p/6842045.html)

## 30. EnumMap

## 31. WeakHashMap

## 32. IdentityHashMap

## 33. Queue

## 34. Deque

## 35. AbstractQueue

## 36. ArrayDeque

## 37. PriorityQueue

对于 `PriorityQueue` 来说，最重要的一点就是要清楚他是基于堆结构实现，可以用它来实现优先队列。

![image-20220123003958360](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123003958360-8c4e80.png)



![image-20220123004023796](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123004023796-ab923d.png)



![PriorityQueue remove2](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/PriorityQueue-remove2-8292c0.png)

```java
@Test
    public void testSize() {
        PriorityQueue<Integer> queue = new PriorityQueue<>(5);
        for (int i = 0; i < 10; i++) {
            queue.add(i);
        }
        assertThat(queue).hasSize(10);
    }

    @Test
    public void test() {
        int capacity = 5;
        PriorityQueue<Integer> queue = new PriorityQueue<>(capacity);
        for (int i = 0; i < 10; i++) {
            queue.add(i);
            if (queue.size() > capacity) {
                Integer num = queue.poll();
                System.out.println(num);
            }
        }
        assertThat(queue).hasSize(capacity);
        assertThat(queue).contains(9);
        assertThat(queue).doesNotContain(0);
        Integer num = queue.poll();
        // 可见，默认就是最小堆。
        assertThat(num).isEqualTo(5);
    }
```

从上述例子中可以看出，`PriorityQueue` 的长度是回增长的。所以，如果需要定长的优先队列，则需要将多余数据"弹出"。

### 37.1. 参考资料

1. [Collection - PriorityQueue源码解析 | Java 全栈知识体系](https://www.pdai.tech/md/java/collection/java-collection-PriorityQueue.html)

## 38. 工具类 `Arrays`

## 39. 工具类 `Collections`

## 40. 并发库概述



![java-concurrent-overview](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/java-concurrent-overview-654461.png)

### 40.1. Happy-Before 原则

1. Single Thread Rule 单一线程原则
2. Monitor Lock Rule 管程锁定规则
3. Volatile Variable Rule `volatile` 变量规则
4. Thread Start Rule 线程启动规则
5. Thread Join Rule 线程加入规则
6. Thread Interruption Rule 线程中断规则
7. Finalizer Rule 对象终结规则
8. Transitivity 传递性

## 41. Thread

在JDK1.2之后，Java线程模型已经确定了基于操作系统原生线程模型实现。

![jvm thread to os thread](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/jvm-thread-to-os-thread-998e87.jpeg)

Java线程最终会映射为系统内核原生线程，所以Java线程调度最终取决于系操作系统，而目前主流的操作系统内核线程调度基本都是使用抢占式线程调度。也就是可以死记硬背一下：**Java线程是使用抢占式线程调度方式进行线程调度的。**

线程状态在 `Thread` 类已经通过一个枚举给出了所有可能：

Thread

  ```java
  public class Thread implements Runnable {
      // ……
      /**
       * A thread state.  A thread can be in one of the following states:
       * <ul>
       * <li>{@link #NEW}<br>
       *     A thread that has not yet started is in this state.
       *     </li>
       * <li>{@link #RUNNABLE}<br>
       *     A thread executing in the Java virtual machine is in this state.
       *     </li>
       * <li>{@link #BLOCKED}<br>
       *     A thread that is blocked waiting for a monitor lock
       *     is in this state.
       *     </li>
       * <li>{@link #WAITING}<br>
       *     A thread that is waiting indefinitely for another thread to
       *     perform a particular action is in this state.
       *     </li>
       * <li>{@link #TIMED_WAITING}<br>
       *     A thread that is waiting for another thread to perform an action
       *     for up to a specified waiting time is in this state.
       *     </li>
       * <li>{@link #TERMINATED}<br>
       *     A thread that has exited is in this state.
       *     </li>
       * </ul>
       *
       * <p>
       * A thread can be in only one state at a given point in time.
       * These states are virtual machine states which do not reflect
       * any operating system thread states.
       *
       * @since   1.5
       * @see #getState
       */
      public enum State {
          /**
           * Thread state for a thread which has not yet started.
           */
          NEW,
  
          /**
           * Thread state for a runnable thread.  A thread in the runnable
           * state is executing in the Java virtual machine but it may
           * be waiting for other resources from the operating system
           * such as processor.
           */
          RUNNABLE,
  
          /**
           * Thread state for a thread blocked waiting for a monitor lock.
           * A thread in the blocked state is waiting for a monitor lock
           * to enter a synchronized block/method or
           * reenter a synchronized block/method after calling
           * {@link Object#wait() Object.wait}.
           */
          BLOCKED,
  
          /**
           * Thread state for a waiting thread.
           * A thread is in the waiting state due to calling one of the
           * following methods:
           * <ul>
           *   <li>{@link Object#wait() Object.wait} with no timeout</li>
           *   <li>{@link #join() Thread.join} with no timeout</li>
           *   <li>{@link LockSupport#park() LockSupport.park}</li>
           * </ul>
           *
           * <p>A thread in the waiting state is waiting for another thread to
           * perform a particular action.
           *
           * For example, a thread that has called {@code Object.wait()}
           * on an object is waiting for another thread to call
           * {@code Object.notify()} or {@code Object.notifyAll()} on
           * that object. A thread that has called {@code Thread.join()}
           * is waiting for a specified thread to terminate.
           */
          WAITING,
  
          /**
           * Thread state for a waiting thread with a specified waiting time.
           * A thread is in the timed waiting state due to calling one of
           * the following methods with a specified positive waiting time:
           * <ul>
           *   <li>{@link #sleep Thread.sleep}</li>
           *   <li>{@link Object#wait(long) Object.wait} with timeout</li>
           *   <li>{@link #join(long) Thread.join} with timeout</li>
           *   <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
           *   <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
           * </ul>
           */
          TIMED_WAITING,
  
          /**
           * Thread state for a terminated thread.
           * The thread has completed execution.
           */
          TERMINATED;
      }
  
      // ……
  }
  ```



![thread lifecycle](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/thread-lifecycle-efaeba.jpeg)

![thread states](https://diguage.github.io/jdk-source-analysis/images/thread-states.jpeg)

Java 对象内存占用大小：

1. 对象头在32位系统上占用8bytes，64位系统上占用16bytes。开启（-XX:+UseCompressedOops）对象头大小为12bytes（64位机器）。
2. 64位机器上，数组对象的对象头占用24个字节，启用压缩之后占用16个字节。之所以比普通对象占用内存多是因为需要额外的空间存储数组的长度。
3. 64位机器上reference类型占用8个字节，开启指针压缩后占用4个字节。
4. 复合对象，直接计算当前对象占用空间大小，包括当前类及超类的基本类型实例字段大小、引用类型实例字段引用大小、实例基本类型数组总占用空间、实例引用类型数组引用本身占用空间大小; 但是不包括超类继承下来的和当前类声明的实例引用字段的对象本身的大小、实例引用数组引用的对象本身的大小。
5. 对齐填充是以每个对象为单位进行的。

HotSpot的对齐方式为8字节对齐： `（对象头 + 实例数据 + padding） % 8等于0且0 ⇐ padding < 8`。

```java
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;


public class JolTest {
    /**
     * Java Object Layout
     */
    public static void main(String[] args) {

        System.out.println(VM.current().details());
        System.out.println("--o = 12--------------");
        Object o = new Object() {};
        System.out.println(ClassLayout.parseInstance(o).toPrintable());

        System.out.println("--o2 = 12--------------");
        Object o2 = new Object() {
            private String name = "";
            private long age = 0;
        };
        System.out.println(ClassLayout.parseInstance(o2).toPrintable());
        System.out.println("--\"119\"--------------");
        String s = "119";
        System.out.println(s.hashCode());
        System.out.println(ClassLayout.parseInstance(s).toPrintable());
        System.out.println("--119L--------------");
        System.out.println(ClassLayout.parseInstance(119L).toPrintable());

        System.out.println("--o[] = 16--------------");
        System.out.println(ClassLayout.parseInstance(new Object[0]).toPrintable());

        System.out.println("--o[1]--------------");
        System.out.println(ClassLayout.parseInstance(new Object[]{new Object()}).toPrintable());
    }
}
```



`join()` 方法的本质是当前线程对象实例调用线程 `wait()` 方法。

```java
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;



public class ThreadTest {
    @Test
    public void testState() throws InterruptedException {
        Thread thread = new Thread(() -> {
            System.out.println("StartTime: " + LocalDateTime.now());
            int i = 0;
            try {
                Thread.sleep(10 * 1000);
                while (true) {
                    i++;
                    if (i > Integer.MAX_VALUE >> 1) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("testState: is interrupted at "
                        + LocalDateTime.now());
                e.printStackTrace();
            }
            System.out.println("  EndTime: " + LocalDateTime.now());
        });
        // NEW
        System.out.println(thread.getState());
        thread.start();
        // RUNNABLE
        System.out.println(thread.getState());
        Thread.sleep(1000);
        // TIMED_WAITING
        System.out.println(thread.getState());
        Thread.sleep(9200);
        // RUNNABLE ??
        System.out.println(thread.getState());
        Thread.sleep(10 * 1000);
        // TERMINATED
        System.out.println(thread.getState());
    }

    @Test
    public void testBlockState() throws InterruptedException {
        Object lock = new Object();
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread t2 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("thread2 got monitor lock...");
            }
        });
        t1.start();
        Thread.sleep(50);
        t2.start();
        Thread.sleep(50);
        System.out.println(t2.getState());
    }

    @Test
    public void testInterrupt() throws InterruptedException {
        class InterruptTask implements Runnable {
            @Override
            public void run() {
                Thread.interrupted();
                Thread thread = Thread.currentThread();
                while (true) {
                    if (thread.isInterrupted()) {
                        System.out.println("InterruptTask was interrupted at "
                                + LocalDateTime.now());
                    }
//                    try {
//                        Thread.sleep(5 * 1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            }
        }

        Thread thread = new Thread(new InterruptTask());
        thread.start();
        Thread.sleep(20 * 1000);
        thread.interrupt();
    }

    // TODO
    @Test
    public void testInterruptStatus1() throws InterruptedException {
        class InterruptTask implements Runnable {
            @Override
            public void run() {
                long i = 0;
                while (true) {
                    i++;
                }
            }
        }
        Thread thread = new Thread(new InterruptTask());
        thread.start();
        Thread.sleep(1000);
        thread.interrupt();
        System.out.println("thread.isInterrupted() = " + thread.isInterrupted());
        System.out.println("thread.isInterrupted() = " + thread.isInterrupted());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
    }

    // TODO
    @Test
    public void testInterruptStatus2() throws InterruptedException {
        class IntDelay implements Delayed {

            private int num;
            private long deadline;

            public IntDelay(int num) {
                this.num = num;
                deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(num);
            }

            @Override
            public long getDelay(TimeUnit unit) {
                return deadline - System.currentTimeMillis();
            }

            @Override
            public int compareTo(Delayed o) {
                IntDelay param = (IntDelay) o;
                return Integer.compare(this.num, param.num);
            }
        }

        class InterruptTask implements Runnable {
            @Override
            public void run() {
                Thread current = Thread.currentThread();
                DelayQueue<IntDelay> queue = new DelayQueue<>();
                queue.add(new IntDelay(1));
                try {
                    System.out.println("Wait  " + LocalDateTime.now());
                    queue.take();
                    System.out.println("Taken " + LocalDateTime.now());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("current.isInterrupted() = " + current.isInterrupted());
                System.out.println("current.isInterrupted() = " + current.isInterrupted());
            }
        }

        Thread thread = new Thread(new InterruptTask());
        thread.start();
        Thread.sleep(500);
        thread.interrupt();
        System.out.println("thread.isInterrupted() = " + thread.isInterrupted());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testWaitLock() throws InterruptedException {
        // 测试 wait 是否释放锁
        // 根据运行结果来看，thread1 和 thread2 是交叉执行的，
        // 则：线程在 wait 时，是释放了锁的，
        // 再次获取锁后，会接着上次执行点继续执行。
        //
        // 这里还有一点需要注意：wait 需要在锁对象上执行，否则会报错。
        Object lock = new Object();
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("thread1 start to wait...");
                    lock.wait(1000);
                    System.out.println("thread1 weak up...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread t2 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("thread2 got monitor lock...");
            }
        });
        t1.start();
        Thread.sleep(50);
        t2.start();
        Thread.sleep(2000);
    }

    @Test
    public void testSleepLock() throws InterruptedException {
        // 测试 sleep 是否释放锁
        // 根据输出来看，thread1 执行完后再次执行的 thread2
        // 则：线程在 sleep 时，不释放锁。
        Object lock = new Object();
        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("thread1 start to wait...");
                    Thread.sleep(2000);
                    System.out.println("thread1 weak up...");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread t2 = new Thread(() -> {
            synchronized (lock) {
                System.out.println("thread2 got monitor lock...");
            }
        });
        t1.start();
        Thread.sleep(50);
        t2.start();
        Thread.sleep(3000);
    }


    @Test
    public void testJoin() throws InterruptedException {
        JoinMain.AddThread thread = new JoinMain.AddThread();
        thread.start();
        // 执行这句话，则下面的输出会等 thread 执行完成后，i值等于100000；
        // 如果注释掉，则瞬间向下执行，i值很小。
        thread.join();
        System.out.println(JoinMain.i);
    }

    static class JoinMain {
        public volatile static int i = 0;

        static class AddThread extends Thread {
            @Override
            public void run() {
                for (i = 0; i < 100000; i++) {
                }
            }
        }
    }

    @Test
    public void testYield() throws InterruptedException {
        Map<Integer, Integer> map = new HashMap<>();
        Integer key = 1;
        Integer key2 = 2;
        Thread thread = new Thread(() -> {
            while (true) {
                Thread.yield();
                Integer num = map.getOrDefault(key, 1);
                map.put(key, ++num);
            }
        });
        Thread thread2 = new Thread(() -> {
            while (true) {
                Integer num = map.getOrDefault(key2, 1);
                map.put(key2, ++num);
            }
        });
        thread.start();
        thread2.start();
        Thread.sleep(1000);
        // 如果 Thread.yield() 没有让出 CPU，则两个值相差不多；否则相差很大。
        System.out.println(map.toString().replace(",", "\n"));
        System.out.println(thread.getState());
    }

    @Test
    public void testChildThread() {
        // TODO 如何掩饰父子线程？如何在父子线程之间传递数据？
        List<Thread> threads = new ArrayList<>();
        Thread thread1 = new Thread(() -> {
            Thread thread = Thread.currentThread();
            ThreadLocal<Integer> threadLocal = ThreadLocal.withInitial(() -> 119);

            Thread child = new Thread(() -> {
            });

            System.out.printf("id=%d, parentId=%d %n", thread.getId(), 123);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        threads.add(thread1);
        thread1.start();
    }

    @Test
    public void testInterruptNoAction() {
        // 虽然给线程发出了中断信号，但程序中并没有响应中断信号的逻辑，所以程序不会有任何反应。
        Thread thread = new Thread(() -> {
            while (true) {
                Thread.yield();
            }
        });
        thread.start();
        thread.interrupt();
        LockSupport.park();
    }

    @Test
    public void testInterruptAction() {
        Thread thread = new Thread(() -> {
            while (true) {
                Thread.yield();
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Java技术栈线程被中断，程序退出。");
                    return;
                }
            }
        });
        thread.start();
        thread.interrupt();
        LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(1));
    }

    @Test
    public void testInterruptFailure() throws InterruptedException {
        Thread thread = new Thread(() -> {
            while (true) {
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Java技术栈线程被中断，程序退出。");
                    return;
                }

                try {
                    // sleep() 方法被中断后会清除中断标记，所以循环会继续运行。。
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Java技术栈线程休眠被中断，程序退出。");
                }
                System.out.println(Thread.currentThread().getState() + " 线程苏醒，继续执行……");
            }
        });
        thread.start();
        Thread.sleep(100); // 注意加上这句话！否则线程还没启动就被终端了
        thread.interrupt();
        LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(1));
        System.out.println(thread.getState());
    }

    @Test
    public void testInterruptSleep() throws InterruptedException {
        Thread thread = new Thread(() -> {
            while (true) {
                // 响应中断
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("Java技术栈线程被中断，程序退出。");
                    return;
                }

                try {
                    // sleep() 方法被中断后会清除中断标记，所以循环会继续运行。。
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Java技术栈 线程 休眠被中断，程序退出。");
                    Thread.currentThread().interrupt();
                }
                System.out.println(Thread.currentThread().getState() + " 线程苏醒，继续执行……");
            }
        });
        thread.start();
        Thread.sleep(100); // 注意加上这句话！否则线程还没启动就被终端了
        thread.interrupt();
        LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(1));
        System.out.println(thread.getState());
    }

    @Test
    public void testSynchronized() throws InterruptedException {
        class Account {
            int money = 100;

            synchronized void increase() {
                System.out.println("start to increase");
                money -= 10;
                double var = 0;
                for (int i = 0; i < 10000000; i++) {
                    var = Math.PI * Math.E * i;
                    if (i % 2000000 == 0) {
                        throw new RuntimeException("fire");
                    }
                }
                System.out.println("finish increasing." + var);
            }

            synchronized void decrease() {
                System.out.println("start to decrease");
                money += 20;
                System.out.println("finish decreasing.");
            }
        }
        Account account = new Account();
        new Thread(account::increase).start();
        Thread.sleep(1);
        new Thread(account::decrease).start();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(30));
        System.out.println(account.money);
    }
}
```



线程休眠苏醒后，中断信号就会被清除！所以，如果要响应这种中断，还需要再异常捕获代码段再次中断才行！

线程上下文切换(`Context Switch`)，都保存了哪些信息？怎么保存的？

- Windows 系统中，https://docs.microsoft.com/zh-cn/sysinternals/downloads/process-explorer[Process Explorer] 可以查看上下文切换信息。
- 阿里巴巴推出的 [Alibaba Arthas](https://github.com/alibaba/arthas) 也是一个诊断利器。

### 41.1. 参考资料

1. [Java线程生命周期与状态切换 - Throwable’s Blog](http://www.throwable.club/2019/06/23/java-concurrency-thread-state/)
2. [一个对象占用多少字节？ - 田麦 - ITeye博客](https://www.iteye.com/blog/yueyemaitian-2033046)
3. [一个Java对象到底占用多大内存？ - zhanjindong - 博客园](https://www.cnblogs.com/zhanjindong/p/3757767.html)
4. [一文搞懂 Java 线程中断 - 知乎](https://zhuanlan.zhihu.com/p/45667127)
5. [Java 并发：线程间通信与协作_Java_Rico’s Blogs-CSDN博客](https://blog.csdn.net/justloveyou_/article/details/54929949)
6. [synchronized和锁(ReentrantLock) 区别_Java_小风筝-CSDN博客](https://blog.csdn.net/zheng548/article/details/54426947)

## 42. LockSupport

```java
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.LockSupport;



public class LockSupportTest {
    @Test
    public void test() throws InterruptedException {
        Thread t1 = new Thread(new Task("t1"));
        Thread t2 = new Thread(new Task("t2"));
        t1.start();
        Thread.sleep(1000);
        t2.start();
        LockSupport.unpark(t1);
        LockSupport.unpark(t2);
        t1.join();
        t2.join();
        System.out.println("finish...");
    }

    static class Task implements Runnable {
        private String name;

        public Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            synchronized (LockSupportTest.class) {
                System.out.println("in " + name);
                LockSupport.park();
            }
        }
    }

    @Test
    public void testParkAndUnpark() throws InterruptedException {
        System.out.println("--m1------");
        Thread thread = new Thread(() -> {
            System.out.println("--t1------");
            LockSupport.park();
            System.out.println("--t2------");
        });
        thread.start();
        Thread.sleep(5000);
        LockSupport.unpark(thread);
        System.out.println("--m2------");
    }

}
```



`synchronized` 关键字在方法上使用时，在方法修饰符上增加了一个标志位 `flags: (0x0021) ACC_PUBLIC, ACC_SYNCHRONIZED`。而用在代码块时，则是生成了 `monitorenter` 和 `monitorexit` 指令。

```java
import org.junit.jupiter.api.Test;

import java.util.concurrent.locks.LockSupport;



public class SynchronizedTest {
  public synchronized void lockMethod() {
    System.out.println("lock method");
  }

  public void lockObject() {
    synchronized (this) {
      System.out.println("lock object");
    }
  }

  @Test
  public void testInstanceLock() {
    SynMain main = new SynMain();
    new Thread(main::getInstanceLock1).start();
    new Thread(main::getInstanceLock2).start();
    new Thread(SynMain::getStaticLock1).start();
    new Thread(SynMain::getStaticLock2).start();
    LockSupport.park();
  }


  public static class SynMain {
    public static synchronized void getStaticLock1() {
      System.out.println("getStaticLock1 get lock, running...");
      try {
        Thread.sleep(Integer.MAX_VALUE);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public static synchronized void getStaticLock2() {
      System.out.println("getStaticLock2 get lock, running...");
      try {
        Thread.sleep(Integer.MAX_VALUE);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public synchronized void getInstanceLock1() {
      System.out.println("getInstanceLock1 get lock, running...");
      try {
        Thread.sleep(Integer.MAX_VALUE);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public synchronized void getInstanceLock2() {
      System.out.println("getInstanceLock2 get lock, running...");
      try {
        Thread.sleep(Integer.MAX_VALUE);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
```



可以利用 jclasslib Bytecode viewer 工具，或者 `javap -c -v XXX.class` 来查看。

## 43. AbstractQueuedSynchronizer

Doug Lea

在 Java 5 之后，JDK 内置了大量的并发工具类。粗略去看这些工具类的源码，你会发现，大多数都在内部继承了 `AbstractQueuedSynchronizer`。由此可见，`AbstractQueuedSynchronizer` 的核心地位。想搞清楚这些并发工具类的原理，`AbstractQueuedSynchronizer` 的源码可以说是不可不看。

### 43.1. CLH lock queue 介绍

![image-20220123151217678](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123151217678-c17dcc.png)

终于看明白了 CLH lock queue。CLH 通过自旋来锁定当前节点。自旋的好处是线程不需要睡眠和唤醒，减小了系统调用的开销。

AQS 中线程不是一直在自旋的，而可能会反复的睡眠和唤醒，这就需要前继释放锁的时候通过 next 指针找到其后继将其唤醒，也就是 AQS 的等待队列中后继是被前继唤醒的。AQS 结合了自旋和睡眠/唤醒两种方法的优点。

*AQS 结合了自旋和睡眠/唤醒两种方法的优点。* 这句话该如何理解？刚刚想到的一点： AQS 中会先自旋两次，如果不成功则休眠。应该是这样来使用两者的好处！

自己实现一个 CLH 锁！

### 43.2. 核心点

1. 模板方法模式
   1. `boolean tryAcquire(int arg)`
   2. `boolean tryRelease(int arg)`
   3. `int tryAcquireShared(int arg)`
   4. `boolean tryReleaseShared(int arg)`
   5. `boolean isHeldExclusively()`

基于 `AbstractQueuedSynchronizer`，我们实现一个互斥锁：

```java
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;



public class Mutex implements Lock {

    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        Condition newCodition() {
            return new ConditionObject();
        }
    }

    private final Sync sync = new Sync();

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(time));
    }
    
    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCodition();
    }

    public boolean isLocked() {
        return sync.isHeldExclusively();
    }

    public boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }
}
```



互斥锁，也可以称为独占锁，顾名思义就是同一个时刻只能有一个线程获取到锁，而其他获取锁的线程只能在同步队列中等待，只有获取锁的线程释放了锁，后继的线程才能获取锁。

```java
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class AbstractQueuedSynchronizerTest {

    @Test
    public void testNode() {
        AqsNode head = new AqsNode();
        AqsNode next = new AqsNode(AqsNode.EXCLUSIVE);
        head.next = next;
        next.prev = head;
        AqsNode tail = new AqsNode(AqsNode.EXCLUSIVE);
        next.next = tail;
        tail.prev = next;
        List<Thread> threads = new ArrayList<>();
        for (AqsNode node = head; node != null; node = node.next) {
            threads.add(node.thread);
        }
        System.out.println(threads);
    }

    public static class AqsNode {

        static final AqsNode SHARED = new AqsNode();
        static final AqsNode EXCLUSIVE = null;

        static final int CANCELLED = 1;
        static final int SIGNAL = -1;
        static final int CONDITION = -2;
        static final int PROPAGATE = -3;

        volatile int waitStatus;

        volatile AqsNode prev;

        volatile AqsNode next;

        volatile Thread thread;

        AqsNode nextWaiter;

        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        final AqsNode predecessor() {
            AqsNode p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        AqsNode() {
        }

        AqsNode(AqsNode nextWaiter) {
            this.nextWaiter = nextWaiter;
            THREAD.set(this, Thread.currentThread());
        }

        AqsNode(int waitStatus) {
            WAITSTATUS.set(this, waitStatus);
            THREAD.set(this, Thread.currentThread());
        }

        final boolean compareAndSetWaitStatus(int expect, int update) {
            return WAITSTATUS.compareAndSet(this, expect, update);
        }

        final boolean compareAndSetNext(AqsNode expect, AqsNode update) {
            return NEXT.compareAndSet(this, expect, update);
        }

        final void setPrevRelaxed(AqsNode p) {
            PREV.set(this, p);
        }

        private static final VarHandle NEXT;
        private static final VarHandle PREV;
        private static final VarHandle THREAD;
        private static final VarHandle WAITSTATUS;

        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                NEXT = l.findVarHandle(AqsNode.class, "next", AqsNode.class);
                PREV = l.findVarHandle(AqsNode.class, "prev", AqsNode.class);
                THREAD = l.findVarHandle(AqsNode.class, "thread", Thread.class);
                WAITSTATUS = l.findVarHandle(AqsNode.class, "waitStatus", int.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    @Test
    public void testCustomLock() throws InterruptedException {
        Mutex mutex = new Mutex();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                try {
                    int time = new Random().nextInt(5000);
                    mutex.lock();
                    System.out.printf("thread=%d running time=%d%n",
                            Thread.currentThread().getId(), time);
                    Thread.sleep(time);
                    System.out.printf("thread=%d finished%n",
                            Thread.currentThread().getId());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    mutex.unlock();

                }
            });
        }
        executorService.shutdown();
        while (executorService.isTerminated()) {
        }
        Thread.sleep(50000);
        System.out.println("All task were finished.");
    }
}
```



运行结果显示，指定时刻，只有一个线程在运行。

查看 `AbstractQueuedSynchronizer` 继承关系可以看出，在 `ReentrantLock`，`ReentrantReadWriteLock` 和 `Semaphore` 三个类中实现了公平锁和非公平锁。

### 43.3. `Node` 详解

- `waitStatus`：当前 `Node` 的等待状态，有五个可选值。
- `prev`：当前 `Node` 实例的前驱节点引用。
- `next`：当前 `Node` 实例的后继节点引用。
- `thread`：当前 `Node` 实例持有的线程实例引用。
- `nextWaiter`：这个值是一个比较容易令人生疑的值，虽然表面上它称为"下一个等待的节点"，但是实际上它有三种取值的情况。
  1. 值为静态实例 `Node.EXCLUSIVE`(也就是 `null`)，代表当前的 `Node` 实例是独占模式。
  2. 值为静态实例 `Node.SHARED`，代表当前的 `Node` 实例是共享模式。
  3. 值为非 `Node.EXCLUSIVE` 和 `Node.SHARED` 的其他节点实例，代表 `Condition` 等待队列中当前节点的下一个等待节点。

#### 43.3.1. `Node` 中一些常量定义

区分共享锁还是独占式锁的常量，是如何被使用的？独占锁为何没有初始化？

1. `static final Node SHARED = new Node();`
2. `static final Node EXCLUSIVE = null;` — 为何没有被初始化？

共享锁的话，大家使用同一个 `Node` 实例，而独自锁则是每个任务使用一个 `Node` 实例。可以这样理解吗？

节点的状态

1. `static final int CANCELLED = 1;` — 表示当前的线程被取消；
2. `static final int SIGNAL = -1;` — 表示当前节点的后继节点包含的线程需要运行，也就是unpark；
3. `static final int CONDITION = -2;` — 表示当前节点在等待condition，也就是在condition队列中；
4. `static final int PROPAGATE = -3;` — 表示当前场景下后续的acquireShared能够得以执行；
5. `0` — 表示当前节点在sync队列中，等待着获取锁。

模板方法：

1. `isHeldExclusively()` — 该线程是否正在独占资源。只有用到condition才需要去实现它。
2. `tryAcquire(int)` — 独占方式。尝试获取资源，成功则返回true，失败则返回false。
3. `tryRelease(int)` — 独占方式。尝试释放资源，成功则返回true，失败则返回false。
4. `tryAcquireShared(int)` — 共享方式。尝试获取资源。负数表示失败；0表示成功，但没有剩余可用资源；正数表示成功，且有剩余资源。
5. `tryReleaseShared(int)` — 共享方式。尝试释放资源，成功则返回true，失败则返回false。

### 43.4. 独占模式

独占模式的同步器的一个显著特点就是：头节点的第一个有效(非取消)的后继节点，总是尝试获取资源，一旦获取资源成功就会解除阻塞并且晋升为头节点，原来所在节点会移除出同步等待队列，原来的队列长度就会减少1，然后头结点的第一个有效的后继节点继续开始竞争资源。

```java
 /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block
     */
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // 如果前一个节点已经在排队，则新加入的节点就应该 park
        int ws = pred.waitStatus;
        if (ws == Node.SIGNAL)
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
            return true;
        // 如果前一个节点已经取消，则删除取消节点
        if (ws > 0) {
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            // 跳过已经取消的节点
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            // 加入队列
            pred.next = node;
        } else {
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            // 如果前一个节点没有取消，则尝试将前一个节点设置为 Node.SIGNAL
            pred.compareAndSetWaitStatus(ws, Node.SIGNAL);
        }
        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     */
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted.
     *
     * @return {@code true} if interrupted
     */
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting
     */
    final boolean acquireQueued(final Node node, int arg) {
        boolean interrupted = false;
        try {
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null; // help GC
                    return interrupted;
                }
                // 此时 node 节点已经通过 addWaiter 方法加入到队列中
                // 1、如果前一个节点是 SIGNAL，则返回 true，park 该线程
                // 2.1 如果前一个节点取消，则通过遍历将之前的连续的取消节点全部删除，
                //     返回 false，再次自旋尝试获取锁
                // 2.2 如果前一个节点没有取消，则将前一个节点尝试修改为 SIGNAL。
                //     那么下一次循环时，走第一个判断，返回 true，park 该线程。
                //     或者前驱节点已经弹出队列，则该线程尝试获取锁。
                if (shouldParkAfterFailedAcquire(p, node))
                    interrupted |= parkAndCheckInterrupt();
            }
        } catch (Throwable t) {
            cancelAcquire(node);
            if (interrupted)
                selfInterrupt();
            throw t;
        }
    }
```



可以画一下流程图：

1. 以 `ReentrantLock` 为例，在独占模式下，获取锁的过程
2. 以 `ReentrantLock` 为例，在独占模式下，释放锁的过程
3. 使用 `Condition` 对象，`await()` 的过程
4. 使用 `Condition` 对象，`signal()` 的过程
5. 以 `Semaphore` 为例，在共享模式下，获取锁的过程
6. 以 `Semaphore` 为例，在共享模式下，释放锁的过程

### 43.5. 共享模式

共享模式的同步器的一个显著特点就是：头节点的第一个有效(非取消)的后继节点，总是尝试获取资源，一旦获取资源成功就会解除阻塞并且晋升为头节点，原来所在节点会移除出同步等待队列，原来的队列长度就会减少1，重新设置头节点的过程会传播唤醒的状态，简单来说就是唤醒一个有效的后继节点，只要一个节点可以晋升为头节点，它的后继节点就能被唤醒。节点的唤醒顺序遵循类似于FIFO的原则，通俗说就是先阻塞或者阻塞时间最长则先被唤醒。

### 43.6. `ConditionObject`

关于这段代码的研究，可以参看 [`java.util.concurrent.ArrayBlockingQueue`](https://diguage.github.io/jdk-source-analysis/#)。`ArrayBlockingQueue` 在实现 `poll(long, java.util.concurrent.TimeUnit)` 方法时，使用了 `Condition notEmpty` 对象来调用 `ConditionObject.awaitNanos(long)` 方法。

### 43.7. 参考资料

|      | 访问一些页面时发现一些页面已经不能访问了，后续再搜索补上吧。 |
| ---- | ------------------------------------------------------------ |

1. [JUC同步器框架AbstractQueuedSynchronizer源码图文分析 - Throwable’s Blog](http://www.throwable.club/2019/04/07/java-juc-aqs-source-code/)
2. [《The java.util.concurrent Synchronizer Framework》 JUC同步器框架（AQS框架）原文翻译 - 只会一点java - 博客园](https://www.cnblogs.com/dennyzhangdd/p/7218510.html)
3. [Java并发之AQS详解 - waterystone - 博客园](https://www.cnblogs.com/waterystone/p/4920797.html)
4. [Java并发包基石-AQS详解 - dreamcatcher-cx - 博客园](https://www.cnblogs.com/chengxiao/archive/2017/07/24/7141160.html)
5. [【JUC】JDK1.8源码分析之AbstractQueuedSynchronizer（二） - leesf - 博客园](http://www.cnblogs.com/leesf456/p/5350186.html)
6. [深度解析Java 8：JDK1.8 AbstractQueuedSynchronizer的实现分析（上）](http://www.infoq.com/cn/articles/jdk1.8-abstractqueuedsynchronizer)
7. [深度解析Java 8：AbstractQueuedSynchronizer的实现分析（下）](http://www.infoq.com/cn/articles/java8-abstractqueuedsynchronizer)
8. [Lock、ReentrantLock和AbstractQueuedSynchronizer的源码要点分析整理 | 三石·道](http://www.molotang.com/articles/480.html)
9. [Java并发包源码学习之AQS框架（一）概述 - Jindong Zhan](http://zhanjindong.com/2015/03/10/java-concurrent-package-aqs-overview)
10. [Java并发包源码学习之AQS框架（二）CLH lock queue和自旋锁 - Jindong Zhan](http://zhanjindong.com/2015/03/11/java-concurrent-package-aqs-clh-and-spin-lock)
11. [Java并发包源码学习之AQS框架（三）LockSupport和interrupt - Jindong Zhan](http://zhanjindong.com/2015/03/14/java-concurrent-package-aqs-locksupport-and-thread-interrupt)
12. [Java并发包源码学习之AQS框架（四）AbstractQueuedSynchronizer源码分析 - Jindong Zhan](http://zhanjindong.com/2015/03/15/java-concurrent-package-aqs-AbstractQueuedSynchronizer)
13. [AbstractQueuedSynchronizer的介绍和原理分析 | 并发编程网 - ifeve.com](http://ifeve.com/introduce-abstractqueuedsynchronizer/)
14. [JUC 源码分析 一 AbstractQueuedSynchronizer | 码蜂笔记](http://coderbee.net/index.php/concurrent/20131209/614)
15. [Java 多线程基本工具的原理AQS](http://www.hiyangqi.com/java concurrency/java-concurrency-AQS.html)
16. [JUC 源码分析 3 AbstractQueuedSynchronizer 共享模式 与 CountDownLatch - 互联网 - 爱上编程技术博客](http://www.tqcto.com/article/internet/5807.html)
17. [通过CountDownLatch来分析AbstractQueuedSynchronizer的源码 - - ITeye技术网站](http://jiangwenfeng762.iteye.com/blog/1293814)

## 44. ReentrantLock

`ReentrantLock` 是重入锁，也是排他锁。

### 44.1. 谈谈 synchronized 和 ReentrantLock 的区别

1. 两者都是可重入锁

   两者都是可重入锁。“可重入锁”概念是：自己可以再次获取自己的内部锁。比如一个线程获得了某个对象的锁，此时这个对象锁还没有释放，当其再次想要获取这个对象的锁的时候还是可以获取的，如果不可锁重入的话，就会造成死锁。同一个线程每次获取锁，锁的计数器都自增1，所以要等到锁的计数器下降为0时才能释放锁。

2. synchronized 依赖于 JVM 而 ReentrantLock 依赖于 API

   synchronized 是依赖于 JVM 实现的，前面我们也讲到了 虚拟机团队在 JDK1.6 为 synchronized 关键字进行了很多优化，但是这些优化都是在虚拟机层面实现的，并没有直接暴露给我们。ReentrantLock 是 JDK 层面实现的（也就是 API 层面，需要 lock() 和 unlock() 方法配合 try/finally 语句块来完成），所以我们可以通过查看它的源代码，来看它是如何实现的。

3. ReentrantLock 比 synchronized 增加了一些高级功能

   相比synchronized，ReentrantLock增加了一些高级功能。主要来说主要有三点：**①等待可中断；②可实现公平锁；③可实现选择性通知（锁可以绑定多个条件）**

   1. **ReentrantLock提供了一种能够中断等待锁的线程的机制**，通过lock.lockInterruptibly()来实现这个机制。也就是说正在等待的线程可以选择放弃等待，改为处理其他事情。

   2. **ReentrantLock可以指定是公平锁还是非公平锁。***而synchronized只能是非公平锁。所谓的公平锁就是先等待的线程先获得锁。 ReentrantLock默认情况是非公平的，可以通过 ReentrantLock类的ReentrantLock(boolean fair)构造方法来制定是否是公平的。

   3. synchronized关键字与wait()和notify()/notifyAll()方法相结合可以实现等待/通知机制，ReentrantLock类当然也可以实现，但是需要借助于Condition接口与newCondition() 方法。Condition是JDK1.5之后才有的，它具有很好的灵活性，比如可以实现多路通知功能也就是在一个Lock对象中可以创建多个Condition实例（即对象监视器），**线程对象可以注册在指定的Condition中，从而可以有选择性的进行线程通知，在调度线程上更加灵活。 在使用notify()/notifyAll()方法进行通知时，被通知的线程是由 JVM 选择的，用ReentrantLock类结合Condition实例可以实现“选择性通知”**，这个功能非常重要，而且是Condition接口默认提供的。而synchronized关键字就相当于整个Lock对象中只有一个Condition实例，所有的线程都注册在它一个身上。如果执行notifyAll()方法的话就会通知所有处于等待状态的线程这样会造成很大的效率问题，而Condition实例的signalAll()方法 只会唤醒注册在该Condition实例中的所有等待线程。

      如果你想使用上述功能，那么选择ReentrantLock是一个不错的选择。

4. 性能已不是选择标准

```JAVA

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;





public class ReentrantLockTest {
    /**
     * 测试可重入性
     */
    @Test
    public void testReentrant() throws InterruptedException {
        SumTask task = new SumTask();
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(SumTask.i);
    }

    static class SumTask implements Runnable {
        public static Lock lock = new ReentrantLock();
        public static int i = 0;

        @Override
        public void run() {
            for (int j = 0; j < 1_000_000; j++) {
                lock.lock();
                lock.lock();
                try {
                    i++;
                } finally {
                    lock.unlock();
                    lock.unlock();
                }
            }
        }
    }

    @Test
    public void testExtraUnlock() throws InterruptedException {
        Lock lock = new ReentrantLock();
        Thread thread = new Thread(() -> {
            lock.lock();
            System.out.println("Locking...");
            lock.unlock();
            lock.unlock();
        });
        thread.start();
        thread.join();
        System.out.println("Finished...");
    }

    @Test
    public void testInterrupt() throws InterruptedException {
        Thread thread = new Thread(new InterruptTask());
        thread.start();
//        Thread.sleep(2);
        thread.interrupt();
        Thread.sleep(10 * 1000);
        System.out.println("Finished...");
    }

    static class InterruptTask implements Runnable {
        public static volatile int i = 0;
        public ReentrantLock lock = new ReentrantLock();

        @Override
        public void run() {
            try {
                lock.lockInterruptibly();
                for (int j = 0; j < 100_000; j++) {
                    i += j;
                }
            } catch (InterruptedException e) {
                System.out.println("InterruptTask was interrupted.");
                System.out.println("i=" + i);
                e.printStackTrace();
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    System.out.println("i=" + i);
                    lock.unlock();
                }
            }
        }
    }

    @Test
    public void testCondition() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        Thread thread = new Thread(new ConditionTask(lock, condition));
        thread.start();
        Thread.sleep(2000);
        lock.lock();
        condition.signal();
        lock.unlock();
    }

    static class ConditionTask implements Runnable {
        private final Lock lock;
        private final Condition condition;

        public ConditionTask(Lock lock, Condition condition) {
            this.lock = lock;
            this.condition = condition;
        }

        @Override
        public void run() {
            try {
                lock.lock();
                condition.await();
                System.out.println("Thread is going on...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    @Test
    public void testExclusive() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(true);
        Thread t1 = new Thread("t1") {
            @Override
            public void run() {
                try {
                    lock.lock();
                    System.out.println("t1 : " + LocalDateTime.now());
                    Thread.sleep(10000);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        };
        t1.start();

        Thread.sleep(10);

        Thread t2 = new Thread("t2") {
            @Override
            public void run() {
                try {
                    lock.lock();
                    System.out.println("t2 : " + LocalDateTime.now());
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        };
        t2.start();

        Thread.sleep(15000);
        System.out.println("DONE");
    }
}
```

```JAVA
/**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }
```



先判断是否有中断，有则响应。从这里就可以看出，在加锁之前也可以被中断。

## 45. ReentrantReadWriteLock

```java

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;




public class ReentrantReadWriteLockTest {
    private volatile int value;

    public int handleRead(Lock lock) throws InterruptedException {
        try {
            lock.lock();
            Thread.sleep(1000);
            System.out.println(Thread.currentThread().getId() + " : read done!");
            return value;
        } finally {
            lock.unlock();
        }
    }

    public void handleWrite(Lock lock, int value) throws InterruptedException {
        try {
            lock.lock();
            Thread.sleep(1000);
            this.value = value;
            System.out.println(Thread.currentThread().getId() + " : write done!");
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void test() throws InterruptedException {
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
        ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();

        Runnable readTask = () -> {
            try {
                handleRead(readLock);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        Runnable writeTask = () -> {
            try {
                handleWrite(writeLock, new Random().nextInt());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        for (int i = 1; i <= 20; i++) {
            if (i % 10 == 5) {
                new Thread(writeTask).start();
            } else {
                new Thread(readTask).start();
            }
        }
        Thread.sleep(20 * 1000);
    }
}
```



## 46. StampedLock

```java
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.StampedLock;

/**
 * @author D瓜哥, https://www.diguage.com/
 * @since 2020-03-16 23:15
 */
public class StampedLockTest {
    @Test
    public void test() {
        StampedLock lock = new StampedLock();
        Point point = new Point(lock);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 1000; i++) {
            executorService.execute(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                point.move(random.nextDouble(100), random.nextDouble(100));
                System.out.println("move point...");
            });

            executorService.execute(() -> {
                double distance = point.distanceFromOrigin();
                System.out.println("current distance = " + distance);
            });
        }
    }

    static class Point {
        private volatile double x, y;
        private final StampedLock lock;

        public Point(StampedLock lock) {
            this.lock = lock;
        }

        void move(double deltaX, double deltaxY) {
            long stamp = lock.writeLock();
            try {
                x += deltaX;
                y += deltaxY;
            } finally {
                lock.unlock(stamp);
            }
        }

        double distanceFromOrigin() {
            long stamp = lock.tryOptimisticRead();
            double currentX = x, currentY = y;
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    currentX = x;
                    currentY = y;
                } finally {
                    lock.unlock(stamp);
                }
            }
            return Math.sqrt(currentX * currentX + currentY * currentY);
        }
    }
}
```



## 47. Semaphore

信号量

```java

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

  

public class SemaphoreTest {
    @Test
    public void test() {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        Semaphore semaphore = new Semaphore(5);
        for (int i = 0; i < 20; i++) {
            executorService.execute(new Task(semaphore));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
        }
        System.out.println("Ok...");
    }

    static class Task implements Runnable {
        private final Semaphore semaphore;

        public Task(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void run() {
            try {
                semaphore.acquire();
                Thread.sleep(2000);
                System.out.println(Thread.currentThread().getId() + " :done!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
            }
        }
    }

    @Test
    public void testReentrant() {
        // 将 Semaphore 的参数分别设置成 1 和 5 运行看结果
        // 递归调用的次数跟 Semaphore 的参数一致
        // 说明，如果 Semaphore 参数为 1 时，它不支持重入。
        Semaphore semaphore = new Semaphore(5);
        class Task implements Runnable {
            private final Semaphore semaphore;
            private int len = 1;

            public Task(Semaphore semaphore) {
                this.semaphore = semaphore;
            }

            @Override
            public void run() {
                try {
                    semaphore.acquire();
                    System.out.println(len++);
                    run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
            }
        }
        new Thread(new Task(semaphore)).start();
        LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(1));
    }
}
```

 

## 48. CountDownLatch

"Count Down" 在英语中意为倒计数，一个典型场景就是火箭🚀发射时的倒计时。它允许一个或多个线程等待其他线程完成操作。

```JAVA

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;




public class CountDownLatchTest {
    private int count = 2;

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(count);
        ExecutorService executorService = Executors.newFixedThreadPool(count);
        for (int i = 0; i < count; i++) {
            executorService.execute(new Task(latch));
        }
        latch.await();
        System.out.println("Fire...");
        executorService.shutdown();
        while (executorService.isTerminated()) {
        }
        System.out.println("All task were done.");
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
        System.out.println("Terminal at " + LocalDateTime.now());
    }

    static class Task implements Runnable {
        private final CountDownLatch latch;

        public Task(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                int time = new Random().nextInt(5000);
                latch.countDown();
                System.out.println(Thread.currentThread().getId() + " time = " + LocalDateTime.now());
                Thread.sleep(time);
                System.out.println(Thread.currentThread().getId() + " sleep = " + time + ": check finished.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```



下面，我们开始看 `CountDownLatch` 源码：

`CountDownLatch` 类中存在一个内部类 `Sync`，继承自 `AbstractQueuedSynchronizer`，代码如下：

```JAVA
/**
 * Synchronization control For CountDownLatch.
 * Uses AQS state to represent count.
 */
private static final class Sync extends AbstractQueuedSynchronizer {
    private static final long serialVersionUID = 4982264981922014374L;

    Sync(int count) {
        setState(count);
    }

    int getCount() {
        return getState();
    }

    protected int tryAcquireShared(int acquires) {
        return (getState() == 0) ? 1 : -1;
    }

    protected boolean tryReleaseShared(int releases) {
        // Decrement count; signal when transition to zero
        for (;;) {
            int c = getState();
            if (c == 0)
                return false;
            int nextc = c - 1;
            if (compareAndSetState(c, nextc))
                return nextc == 0;
        }
    }
}

private final Sync sync;
```



管中窥豹，从这里也可以看出 `CountDownLatch` 中的等待控制几乎都是依赖 `AbstractQueuedSynchronizer` 来实现的。

### 48.1. `await()`

```JAVA
/**
 * Causes the current thread to wait until the latch has counted down to
 * zero, unless the thread is {@linkplain Thread#interrupt interrupted}.
 *
 * <p>If the current count is zero then this method returns immediately.
 *
 * <p>If the current count is greater than zero then the current
 * thread becomes disabled for thread scheduling purposes and lies
 * dormant until one of two things happen:
 * <ul>
 * <li>The count reaches zero due to invocations of the
 * {@link #countDown} method; or
 * <li>Some other thread {@linkplain Thread#interrupt interrupts}
 * the current thread.
 * </ul>
 *
 * <p>If the current thread:
 * <ul>
 * <li>has its interrupted status set on entry to this method; or
 * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
 * </ul>
 * then {@link InterruptedException} is thrown and the current thread's
 * interrupted status is cleared.
 *
 * @throws InterruptedException if the current thread is interrupted
 *         while waiting
 */
public void await() throws InterruptedException {
    sync.acquireSharedInterruptibly(1);
}
```



对 `await()` 的处理直接委托给了 `sync` 的 `acquireSharedInterruptibly(1)` 方法，当然这个方法是从 `AbstractQueuedSynchronizer` 继承而来的。来看一下这个方法：

AbstractQueuedSynchronizer

```JAVA
 /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    public final void acquireSharedInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0)
            doAcquireSharedInterruptibly(arg);
    }
```



结合上面提到的 `Sync` 中的 `tryAcquireShared(int acquires)` 方法，可以看出，当 `getState()` 不为零时，就会导致 `tryAcquireShared(arg)` 结果返回小于零，进而调用 `doAcquireSharedInterruptibly(arg)`，将线程进入排队，然后挂起线程。

![CountDownLatch await park](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/CountDownLatch-await-park-d63588.png)

![CountDownLatch await unpark](https://diguage.github.io/jdk-source-analysis/images/CountDownLatch-await-unpark.png)

### 48.2. `countDown()`

```JAVA
/**
 * Decrements the count of the latch, releasing all waiting threads if
 * the count reaches zero.
 *
 * <p>If the current count is greater than zero then it is decremented.
 * If the new count is zero then all waiting threads are re-enabled for
 * thread scheduling purposes.
 *
 * <p>If the current count equals zero then nothing happens.
 */
public void countDown() {
    sync.releaseShared(1);
}
```



这里的 `releaseShared(1)` 方法是从 `AbstractQueuedSynchronizer` 继承过来的，来看一下这个方法的实现：

AbstractQueuedSynchronizer

```JAVA
/**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }
```



结合上面提到的 `Sync` 中的 `tryReleaseShared(int releases)` 方法，我们可以看出：`countDown()` 方法直接减少锁存器计数，如果不为零，则无所作为；减少到零，则释放所有上述通过 `await()` 方法挂起的所有等待线程。

![image-20220123152123878](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123152123878-7fafc9.png)

![CountDownLatch countDown 2](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/CountDownLatch-countDown-2-9212e9.png)

1. *`CountDownLatch` 为什么使用共享锁？*

   答：前面我们分析 `ReentrantReadWriteLock` 的时候学习过AQS的共享锁模式，比如当前锁是由一个线程获取为互斥锁，那么这时候所有需要获取共享锁的线程都要进入AQS队列中进行排队，当这个互斥锁释放的时候，会一个接着一个地唤醒这些连续的排队的等待获取共享锁的线程，注意，这里的用语是“一个接着一个地唤醒”，也就是说这些等待获取共享锁的线程不是一次性唤醒的。

### 48.3. 参考资料

1. [【JUC】JDK1.8源码分析之CountDownLatch（五） - leesf - 博客园](https://www.cnblogs.com/leesf456/p/5406191.html)
2. [死磕 java同步系列之CountDownLatch源码解析 - 掘金](https://juejin.im/post/5d0660f2518825092c7170dd)

## 49. CyclicBarrier

```java
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author D瓜哥, https://www.diguage.com/
 * @since 2020-03-16 17:24
 */
public class CyclicBarrierTest {
    @Test
    public void test() {
        CyclicBarrier barrier = new CyclicBarrier(5,
                () -> System.out.println("集合完毕，出发……"));
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            executorService.execute(new Task(barrier, "Task-" + (i + 1)));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
        }
        System.out.println("Finished.");
    }

    static class Task implements Runnable {
        private final CyclicBarrier barrier;
        private final String name;

        public Task(CyclicBarrier barrier, String name) {
            this.barrier = barrier;
            this.name = name;
        }

        @Override
        public void run() {
            try {
                System.out.println(name + " start...");
                barrier.await();
                Thread.sleep(new Random().nextInt(1000));
                System.out.println(name + " running...");
                barrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }
}
```

 

## 50. Phaser

## 51. Exchanger

## 52. ThreadLocal

原以为神秘兮兮的 `ThreadLocal` 是何方神圣？没想到，点开代码，竟然如此简单明了，直接上代码：

Thread

```java
/* ThreadLocal values pertaining to this thread. This map is maintained
 * by the ThreadLocal class. */
ThreadLocal.ThreadLocalMap threadLocals = null;
```





ThreadLocal

```java
/**
 * Get the map associated with a ThreadLocal. Overridden in
 * InheritableThreadLocal.
 *
 * @param  t the current thread
 * @return the map
 */
ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

/**
 * Returns the value in the current thread's copy of this
 * thread-local variable.  If the variable has no value for the
 * current thread, it is first initialized to the value returned
 * by an invocation of the {@link #initialValue} method.
 *
 * @return the current thread's value of this thread-local
 */
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    return setInitialValue();
}

/**
 * Sets the current thread's copy of this thread-local variable
 * to the specified value.  Most subclasses will have no need to
 * override this method, relying solely on the {@link #initialValue}
 * method to set the values of thread-locals.
 *
 * @param value the value to be stored in the current thread's copy of
 *        this thread-local.
 */
public void set(T value) {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        map.set(this, value);
    } else {
        createMap(t, value);
    }
}

static class ThreadLocalMap {

    /**
     * The entries in this hash map extend WeakReference, using
     * its main ref field as the key (which is always a
     * ThreadLocal object).  Note that null keys (i.e. entry.get()
     * == null) mean that the key is no longer referenced, so the
     * entry can be expunged from table.  Such entries are referred to
     * as "stale entries" in the code that follows.
     */
    static class Entry extends WeakReference<ThreadLocal<?>> {
        /** The value associated with this ThreadLocal. */
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }
    // ……
}
```



简单解释一下：

当前线程调用 `ThreadLocal` 类的 `set` 或 `get` 方法时，其实是调用的当前线程的 `ThreadLocal.ThreadLocalMap threadLocals` 变量的 `set(ThreadLocal<?> key, Object value)` 和 `Entry getEntry(ThreadLocal<?> key)`。还有一点需要稍加注意：虽然 `ThreadLocal.ThreadLocalMap` 名称是以 `Map` 结尾，但是它并没有实现 `Map` 接口，只是操作上有些类似。

![ThreadLocal set get](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/ThreadLocal-set-get-2bcb6d.jpeg)

有一点需要特别注意：`ThreadLocalMap` 中使用的 `key` 为 `ThreadLocal` 的弱引用,而 `value` 是强引用。所以，如果 `ThreadLocal` 没有被外部强引用的情况下，在垃圾回收的时候，`key` 会被清理掉，而 `value` 不会被清理掉。这样一来，`ThreadLocalMap` 中就会出现 `key` 为 `null` 的 `Entry`。假如我们不做任何措施的话，`value` 永远无法被 GC 回收，这个时候就可能会产生内存泄露。`ThreadLocalMap` 实现中已经考虑了这种情况，在调用 `set()`、`get()`、`remove()` 方法的时候，会清理掉 `key` 为 `null` 的记录。使用完 `ThreadLocal` 方法后 最好手动调用 `remove()` 方法。

```JAVA
import org.junit.jupiter.api.Test;






public class ThreadLocalTest {
    @Test
    public void test() {
        new Thread(new FirstApp()).start();
    }

    @Test
    public void testInheritableThreadLocal() {
        // TODO: InheritableThreadLocal
    }

    private static class FirstApp implements Runnable {
        private ThreadLocal<String> threadLocal
                = ThreadLocal.withInitial(() -> "FirstApp-1");

        private ThreadLocal<String> threadLocal2
                = ThreadLocal.withInitial(() -> "FirstApp-2");

        private SecondApp secondApp = new SecondApp();
        private ThridApp thridApp = new ThridApp();

        @Override
        public void run() {
            System.out.println(threadLocal.get());
            System.out.println(threadLocal2.get());
            new Thread(secondApp).start();
            thridApp.run();
        }
    }

    private static class SecondApp implements Runnable {
        private ThreadLocal<String> threadLocal
                = ThreadLocal.withInitial(() -> "SecondApp");

        @Override
        public void run() {
            System.out.println(threadLocal.get());
        }
    }

    private static class ThridApp implements Runnable {
        private ThreadLocal<String> threadLocal
                = ThreadLocal.withInitial(() -> getClass().getName());

        @Override
        public void run() {
            threadLocal.set("new-ThridApp-value");
            System.out.println(threadLocal.get());
        }
    }
}

```



关注一下： `java.lang.InheritableThreadLocal`。

JDK 的 `InheritableThreadLocal` 类可以完成父线程到子线程的值传递。但对于使用线程池等会池化复用线程的执行组件的情况，线程由线程池创建好，并且线程是池化起来反复使用的；这时父子线程关系的 `ThreadLocal` 值传递已经没有意义，应用需要的实际上是把任务提交给线程池时的 `ThreadLocal` 值传递到 任务执行时。为了解决这个问题，阿里巴巴研发了 [alibaba/transmittable-thread-local](https://github.com/alibaba/transmittable-thread-local) 库。

哈希冲突了怎么解决？

### 52.1. 参考资料

1. [JavaGuide 之 ThreadLocal](https://snailclimb.gitee.io/javaguide/#/docs/java/Multithread/JavaConcurrencyAdvancedCommonInterviewQuestions?id=_33-threadlocal原理)
2. [ThreadLocal终极源码剖析-一篇足矣！ - 只会一点java - 博客园](https://www.cnblogs.com/dennyzhangdd/p/7978455.html)
3. [ThreadLocal源码分析-黄金分割数的使用 - Throwable’s Blog](http://www.throwable.club/2019/02/17/java-concurrency-threadlocal-source-code/)

## 53. AtomicInteger

`AtomicInteger` 类主要利用 CAS (compare and swap) + volatile 和 native 方法来保证原子操作，从而避免 `synchronized` 的高开销，执行效率大为提升。

## 54. LongAdder

```JAVA
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;




public class LongAdderTest {
    @Test
    public void test() {
        LongAdder adder = new LongAdder();
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println(processors);
        ExecutorService executor = Executors.newFixedThreadPool(processors);
        for (int i = 0; i < processors - 1; i++) {
            executor.execute(() -> {
                for (int j = 0; j < Integer.MAX_VALUE; j++) {
                    adder.increment();
                }
            });
        }
        executor.execute(() -> {
            while (true) {
                try {
                    System.out.println(adder.sum());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        executor.shutdown();
        LockSupport.park();
    }
}
```



## 55. JUC 包基础类分析

JUC 包中有一些提交比较小的类，这类类单列出来重量太小，不够篇幅。

## 56. FutureTask

### 56.1. 类图

先来看一下 `FutureTask` 的类图：

![image-20220123152440636](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123152440636-9ce42f.png)

实现了 `Runnable` 的 `run()`，在方法结束时，获取返回值。

`V get()` 方法之所以能阻塞直到方法执行，拿到结果，是因为在 `get()` 方法通过 `awaitDone(boolean timed, long nanos)` 执行了一个无限循环。在循环过程中，不断获取任务执行的状态，进一步获取结果或者响应中断请求。

```java
/**
 * @throws CancellationException {@inheritDoc}
 */
public V get() throws InterruptedException, ExecutionException {
    int s = state;
    if (s <= COMPLETING)
        s = awaitDone(false, 0L);
    return report(s);
}

/**
 * Awaits completion or aborts on interrupt or timeout.
 *
 * @param timed true if use timed waits
 * @param nanos time to wait, if timed
 * @return state upon completion or at timeout
 */
private int awaitDone(boolean timed, long nanos)
    throws InterruptedException {
    // The code below is very delicate, to achieve these goals:
    // - call nanoTime exactly once for each call to park
    // - if nanos <= 0L, return promptly without allocation or nanoTime
    // - if nanos == Long.MIN_VALUE, don't underflow
    // - if nanos == Long.MAX_VALUE, and nanoTime is non-monotonic
    //   and we suffer a spurious wakeup, we will do no worse than
    //   to park-spin for a while
    long startTime = 0L;    // Special value 0L means not yet parked
    WaitNode q = null;
    boolean queued = false;
    for (;;) {
        int s = state;
        if (s > COMPLETING) {
            if (q != null)
                q.thread = null;
            return s;
        }
        else if (s == COMPLETING)
            // We may have already promised (via isDone) that we are done
            // so never return empty-handed or throw InterruptedException
            Thread.yield();
        else if (Thread.interrupted()) {
            removeWaiter(q);
            throw new InterruptedException();
        }
        else if (q == null) {
            if (timed && nanos <= 0L)
                return s;
            q = new WaitNode();
        }
        else if (!queued)
            queued = WAITERS.weakCompareAndSet(this, q.next = waiters, q);
        else if (timed) {
            final long parkNanos;
            if (startTime == 0L) { // first time
                startTime = System.nanoTime();
                if (startTime == 0L)
                    startTime = 1L;
                parkNanos = nanos;
            } else {
                long elapsed = System.nanoTime() - startTime;
                if (elapsed >= nanos) {
                    removeWaiter(q);
                    return state;
                }
                parkNanos = nanos - elapsed;
            }
            // nanoTime may be slow; recheck before parking
            if (state < COMPLETING)
                LockSupport.parkNanos(this, parkNanos);
        }
        else
            LockSupport.park(this);
    }
}
```

 

### 56.2. 参考资料

1. [JUC线程池扩展可回调的Future - Throwable’s Blog](http://www.throwable.club/2019/07/02/java-concurrency-listenable-future/)

## 57. CompletableFuture

Java 中的 Promise。

问题：在一大堆任务中，如何获取第一个完成的返回值？

```JAVA

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;




public class CompletableFutureTest {
    @Test
    public void test() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        FutureTask<Integer> futureTask = new FutureTask<>(new Task());

        executorService.submit(futureTask);
        System.out.println("FutureTask...");
        System.out.println(futureTask.get());
        System.out.println("FutureTask done.");

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(executorService.submit(new Task()));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
        }

        for (Future<Integer> future : futures) {
            if (future.isDone()) {
                System.out.println(future.get());
            }
        }
        CompletableFuture.runAsync(() -> System.out.println(""));
        System.out.println("All tasks were done.");
    }

    public static class Task implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            int second = 0;
            try {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                second = random.nextInt(10000);
                Thread.sleep(second);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return second;
        }
    }

}
```



### 57.1. 参考资料

1. [Guide To CompletableFuture | Baeldung](https://www.baeldung.com/java-completablefuture)

## 58. ThreadPoolExecutor 源码分析

| 状态名称   | 比特位                            | 十进制     | 描述                                                         |
| :--------- | :-------------------------------- | :--------- | :----------------------------------------------------------- |
| RUNNING    | 111-00000000000000000000000000000 | -536870912 | 运行中状态，可以接收新的任务和执行任务队列中的任务           |
| SHUTDOWN   | 000-00000000000000000000000000000 | 0          | shutdown状态，不再接收新的任务，但是会执行任务队列中的任务   |
| STOP       | 001-00000000000000000000000000000 | 536870912  | 停止状态，不再接收新的任务，也不会执行任务队列中的任务，中断所有执行中的任务 |
| TIDYING    | 010-00000000000000000000000000000 | 1073741824 | 整理中状态，所有任务已经终结，工作线程数为0，过渡到此状态的工作线程会调用钩子方法 `terminated()` |
| TERMINATED | 011-00000000000000000000000000000 | 1610612736 | 终结状态，钩子方法 `terminated()` 执行完毕                   |

由于运行状态值存放在高3位，所以可以直接通过十进制值（甚至可以忽略低29位，直接用ctl进行比较，或者使用ctl和线程池状态常量进行比较）来比较和判断线程池的状态：工作线程数为0的前提下：`RUNNING(-536870912)` < `SHUTDOWN(0)` < `STOP(536870912)` < `TIDYING(1073741824)` < `TERMINATED(1610612736)`。

![image-20220123152620219](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123152620219-e0770e.png)

```java
/**
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@link RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         */
        int c = ctl.get();
        // 如果当前工作线程数小于核心线程数，则创建新的线程并执行传入的任务
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                // 创建核心线程成功则直接返回
                return;
            // 创建核心线程失败，则在其他任务提交时，已经创建了足够多的线程数
            // 或者线程池关闭等等，总之线程池状态已经发生变化，
            // 则更新 ctl 的临时变量
            c = ctl.get();
        }
        // 运行到这里说明创建核心线程失败，则当前工作线程已经大于等于 corePoolSize
        // 判断线程池是否运行并且尝试用非阻塞方法向任务队列中添加任务（失败则返回 false）
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            // 向任务队列投放任务成功，对线程池状态做二次检查
            // 如果线程池状态不是运行中，则从任务队列中移除任务并执行拒绝策略
            if (! isRunning(recheck) && remove(command))
                // 执行拒绝策略 -- 结束
                reject(command);
            // 走到下面的 else if 分支，则说明
            // 0、线程池可能是 RUNNING 状态
            // 1、任务移除失败（失败原因可能是任务已经被执行）
            // 如果当前线程数为0，则创建一个非核心线程并传入任务为 null -- 结束
            // 创建的线程不会马上执行任务，而是等待获取任务队列中的任务去执行
            // 如果当前线程数不为0，则什么也不做。因为任务已经成功加入队列，总会执行。
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        // 执行到这里说明：
        // 0、线程池中的工作线程总数已经大于等于 corePoolSize
        // 1、线程池可能不是 RUNNING 状态
        // 2、线程池可能是 RUNNING 状态同时任务队列已经满了
        // 如果向任务队列投放任务失败，则会尝试创建非核心线程传入任务执行
        // 创建非核心线程失败，此时需要拒绝执行任务
        else if (!addWorker(command, false))
            // 执行拒绝策略 -- 结束
            reject(command);
    }
```



为什么需要二次检查线程池的运行状态，当前工作线程数量为 `0`，尝试创建一个非核心线程并且传入的任务对象为 `null`？这个可以看API注释：

如果一个任务成功加入任务队列，我们依然需要二次检查是否需要添加一个工作线程（因为所有存活的工作线程有可能在最后一次检查之后已经终结）或者执行当前方法的时候线程池是否已经 `shutdown` 了。所以我们需要二次检查线程池的状态，必要时把任务从任务队列中移除或者在没有可用的工作线程的前提下新建一个工作线程。

![ThreadPoolExecutor process](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/ThreadPoolExecutor-process-8a5e91.jpeg)

`runWorker()` 方法的核心流程：

![image-20220123152710610](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123152710610-63dd17.png)

### 58.1. 大纲

1. 基本使用
2. 使用 `Executors` 创建线程池
3. 自定义任务，并提交任务
4. 获取返回结果
5. 线程池的类图结构
6. 创建执行线程
7. 取出任务执行
8. 如何实现 `invokeAny(Collection<? extends Callable<T>> tasks)` ？
9. 如何实现 `invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)` ?
10. 如何实现 `invokeAll(Collection<? extends Callable<T>> tasks)` ?
11. 如何实现 `invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)` ？
12. 如何判断线程超时？以及超时后如何杀掉线程？
13. 如何终止任务？温柔终止？或者野蛮终止？
14. 线程池在jDK5、6、7中有哪些升级变化？
15. 拒绝策略

### 58.2. 核心点

1. 关键参数
   1. `corePoolSize`
   2. `maximumPoolSize`
   3. `BlockingQueue`
   4. `RejectedExecutionHandler`
   5. `keepAliveTime`
   6. `threadFactory`
2. `RejectedExecutionHandler`
   1. `AbortPolicy`
   2. `CallerRunsPolicy`
   3. `DiscardPolicy`
   4. `DiscardOldestPolicy`

在生产环境，为了避免首次调用超时，可以调用 `executor.prestartAllCoreThreads()` 预创建所有 `core` 线程，避免来一个创一个带来首次调用慢的问题。

### 58.3. 问题

1. 任务添加后，如何执行？
2. 一个任务执行完成后，如何在同一个线程执行下一个任务？
3. 在 `corePoolSize` 比 `maximumPoolSize` 小的情况下，如何判定一个线程是否超时？并且如何删除一个线程？
4. 任务添加后，
5. 如何返回任务执行的结果？
6. 这个线程池还有哪些可以改进的地方？比如 Guava 中提供了哪些线程池？
7. 如何改造成添加任务，如果没有达到 `maxPoolSize` 则先创建线程？

#### 58.3.1. Tomcat 改进

可不可以自己封装一个Queue，在插入时增加以下逻辑呢？

- 如果当前有空闲线程等待接客，则把任务加入队列让孩儿们去抢。
- 如果没有空闲的了，总线程数又没到达max，那就返回false，让Executor去创建线程。
- 如果总线程数已达到max，则继续把任务加入队列缓冲一下。
- 如果缓冲队列也满了，抛出拒绝异常。

https://github.com/apache/tomcat/blob/master/java/org/apache/tomcat/util/threads/TaskQueue.java

```JAVA
public class TaskQueue extends LinkedBlockingQueue<Runnable> {

    private transient volatile ThreadPoolExecutor parent = null;

    @Override
    public boolean offer(Runnable o) {
      //we can't do any checks
        if (parent==null) return super.offer(o);
        //we are maxed out on threads, simply queue the object
        if (parent.getPoolSize() == parent.getMaximumPoolSize()) return super.offer(o);
        //we have idle threads, just add it to the queue
        if (parent.getSubmittedCount()<=(parent.getPoolSize())) return super.offer(o);
        //if we have less threads than maximum force creation of a new thread
        if (parent.getPoolSize()<parent.getMaximumPoolSize()) return false;
        //if we reached here, we need to add it to the queue
        return super.offer(o);
    }
}
```



如何判断当前有没有空闲的线程等待接客？Tomcat 则靠扩展 `Executor`，增加一个当前请求数的计数器，在 `execute()` 方法前加1，再重载 `afterExecute()` 方法减1，然后判断当前线程总数是否大于当前请求总数就知道有咩有围观群众。

### 58.4. 需要注意的点

1. 线程池如何初始化？
2. 任务如何添加？
3. 任务如何执行？
4. 任务如何终止？
5. 遇到异常如何处理？
6. 线程池队列已满，如何拒绝？
7. 任务执行过程中出现异常，如何处理？关闭该线程，重启一个吗？
8. ？？
9. 任务如何存放？
10. 任务存放后，如何取出来？
11. 如何做到不断地一个一个执行下去？
12. 为什么 `Worker` 继承 `AbstractQueuedSynchronizer` ？AQS起什么作用？是否需要先研究一下？

### 58.5. 收获

1. 可以继承 `ThreadPoolExecutor`，实现 `beforeExecute()` 和 `afterExecute()` 等方法，来加入执行时的回调。类似的回调，还有 `terminated()`
2. 添加任务时， `execute()` 方法的第二种情况，为什么还有再次检查？

```JAVA
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;




public class ThreadPoolExecutorTest {

    @Test
    public void testStatus() {
        int COUNT_BITS = Integer.SIZE - 3;
        int COUNT_MASK = (1 << COUNT_BITS) - 1;

        // runState is stored in the high-order bits
        int RUNNING = -1 << COUNT_BITS;
        int SHUTDOWN = 0 << COUNT_BITS;
        int STOP = 1 << COUNT_BITS;
        int TIDYING = 2 << COUNT_BITS;
        int TERMINATED = 3 << COUNT_BITS;

        System.out.printf("%32s // %d%n", Integer.toBinaryString(RUNNING), RUNNING);
        System.out.printf("%32s // %d%n", Integer.toBinaryString(SHUTDOWN), SHUTDOWN);
        System.out.printf("%32s // %d%n", Integer.toBinaryString(STOP), STOP);
        System.out.printf("%32s // %d%n", Integer.toBinaryString(TIDYING), TIDYING);
        System.out.printf("%32s // %d%n", Integer.toBinaryString(TERMINATED), TERMINATED);
    }

    @Test
    public void testPoolSize() {
        ThreadPoolExecutor executorService
                = new ThreadPoolExecutor(2, 4, 1L,
                TimeUnit.MINUTES, new LinkedBlockingQueue<>(6));

        for (int i = 0; i < 10; i++) {
            executorService.execute(new Task("Task-" + i));
        }
        executorService.shutdown();
        while (!executorService.isTerminated()) {
        }
        System.out.println("Finish all thread...");
    }

    @Test
    public void testCallable() {
        ThreadPoolExecutor executorService
                = new ThreadPoolExecutor(2, 4, 1L,
                TimeUnit.MINUTES, new LinkedBlockingQueue<>(6));

        List<Future<String>> futures = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            Future<String> future = executorService.submit(
                    new CallableTask("CallableTask-" + i));

            futures.add(future);
        }
        for (Future<String> future : futures) {
            try {
                System.out.println(LocalDateTime.now() + "::" + future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        while (!executorService.isTerminated()) {
        }
        System.out.println("Finish all thread...");
    }

    @Test
    public void testComplete() {
        ThreadPoolExecutor executorService
                = new ThreadPoolExecutor(2, 4, 1L,
                TimeUnit.MINUTES, new LinkedBlockingQueue<>(6));

        List<Future<String>> futures = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            Future<String> future = executorService.submit(
                    new CallableTask("CallableTask-" + i));
            futures.add(future);
        }
        for (Future<String> future : futures) {
            try {
                System.out.println(LocalDateTime.now() + "::" + future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        while (!executorService.isTerminated()) {
        }
        System.out.println("Finish all thread...");
    }

    public static class CallableTask implements Callable<String> {
        private final String name;

        public CallableTask(String name) {
            this.name = name;
        }

        @Override
        public String call() throws Exception {
            Thread.sleep(1000);
            //返回执行当前 Callable 的线程名字
            return Thread.currentThread().getName();
        }
    }

    public static class Task implements Runnable {
        private String name;

        public Task(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName()
                    + " Start. Time = " + LocalDateTime.now());

            processCommand();

            System.out.println(Thread.currentThread().getName()
                    + " End. Time = " + LocalDateTime.now());
        }

        private void processCommand() {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
```

  

### 58.6. 思考题

自己实现一个线程池。

### 58.7. 参考资料

1. [JUC线程池ThreadPoolExecutor源码分析 - Throwable’s Blog](http://www.throwable.club/2019/07/15/java-concurrency-thread-pool-executor/)
2. [【JUC】JDK1.8源码分析之ThreadPoolExecutor（一） - leesf - 博客园](https://www.cnblogs.com/leesf456/p/5585627.html)
3. [Java线程池实现原理及其在美团业务中的实践](https://mp.weixin.qq.com/s/baYuX8aCwQ9PP6k7TDl2Ww)
4. [Java ThreadPool的正确打开方式 | 江南白衣](http://calvin1978.blogcn.com/articles/java-threadpool.html)
5. [Tomcat线程池，更符合大家想象的可扩展线程池 | 江南白衣](http://calvin1978.blogcn.com/articles/tomcat-threadpool.html)
6. [每天都在用，但你知道 Tomcat 的线程池有多努力吗？](https://mp.weixin.qq.com/s/YAeneN-x_En55FlC2mVcaA)
7. http://www.throwable.club/2019/07/15/java-concurrency-thread-pool-executor/[JUC线程池ThreadPoolExecutor源码分析 - Throwable’s Blog
8. [Java并发包源码学习之线程池（一）ThreadPoolExecutor源码分析 - Jindong](http://jindong.io/2015/03/30/java-concurrent-package-ThreadPoolExecutor/)
9. [ThreadPoolExecutor简介与源码分析 - 邹胜群的个人页面 - 开源中国社区](http://my.oschina.net/zouqun/blog/407149)
10. [Java并发源码分析 - ThreadPoolExecutor - SHOW ME THE CODE](http://onlychoice.github.io/blog/2013/09/13/java-concurrent-source-code-reading-2/)
11. [Java线程池架构原理和源码解析(ThreadPoolExecutor) - xieyuooo的专栏 - 博客频道 - CSDN.NET](http://blog.csdn.net/xieyuooo/article/details/8718741)
12. [Java多线程系列目录(共43篇) - 如果天空不死 - 博客园](http://www.cnblogs.com/skywang12345/p/java_threads_category.html)
    1. 搞清楚了 `ctl` 的含义，高三位是状态，低29位是线程数
    2. 主要属性的含义，主要方法的实现，任务添加后，三种不同的处理方式
    3. 线程池状态变换
    4. 线程池拒绝策略的实现
    5. 带返回值的任务的实现方式，`Callable`，`Future`
13. [ThreadPoolExecutor的基本使用 | 三石·道](http://www.molotang.com/articles/514.html)
14. [ThreadPoolExecutor的任务提交、任务处理、线程复用和维护相关源码分析 | 三石·道](http://www.molotang.com/articles/522.html)
15. [ThreadPoolExecutor的生命周期相关源码分析 | 三石·道](http://www.molotang.com/articles/526.html)
16. [ThreadPoolExecutor的任务饱和丢弃策略及源码实现 | 三石·道](http://www.molotang.com/articles/553.html)
17. [聊聊并发（三）Java线程池的分析和使用 | 并发编程网 - ifeve.com](http://ifeve.com/java-threadpool/)
18. [戏（细）说Executor框架线程池任务执行全过程（上）](http://www.infoq.com/cn/articles/executor-framework-thread-pool-task-execution-part-01?utm_campaign=rightbar_v2&utm_source=infoq&utm_medium=articles_link&utm_content=link_text)
19. [戏（细）说Executor框架线程池任务执行全过程（下）](http://www.infoq.com/cn/articles/executor-framework-thread-pool-task-execution-part-02?utm_campaign=rightbar_v2&utm_source=infoq&utm_medium=articles_link&utm_content=link_text)
20. [ThreadPoolExecutor 源码分析 - techq’s blog - 博客频道 - CSDN.NET](http://blog.csdn.net/techq/article/details/6818201)
21. [JAVA线程池(ThreadPoolExecutor)源码分析_journeylin_新浪博客](http://blog.sina.com.cn/s/blog_753035050100wbtm.html)
22. [ThreadPoolExecutor源码分析 - rilley - 博客园](http://www.cnblogs.com/rilley/archive/2012/02/07/2341767.html)

## 59. ForkJoinTask

### 59.1. 类图

先来看一下 `ForkJoinTask` 的类图：

![image-20220123152929556](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123152929556-fcc486.png)

## 60. ForkJoinPool

![image-20220123152951895](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123152951895-fc16f6.png)

![ForkJoinPool work stealing](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/ForkJoinPool-work-stealing-5b4f0d.webp)

![ForkJoinPool invoke link](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/ForkJoinPool-invoke-link-5ca056.png)

![ForkJoinPool data structures](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/ForkJoinPool-data-structures-a97a78.png)

![image-20220123153033446](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123153033446-e2bcaf.png)

```java
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * @author D瓜哥, https://www.diguage.com/
 * @since 2020-03-12 10:54
 */
public class ForkJoinPoolTest {
    @Test
    public void test() {
        ForkJoinPool pool = new ForkJoinPool(2);
        String homePath = System.getProperty("user.home");
        FileCountTask task = new FileCountTask(homePath);
        ForkJoinTask<Integer> result = pool.submit(task);
        try {
            Integer count = result.get();
            System.out.println("file count = " + count);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        pool.shutdown();
        while (!pool.isTerminated()) {
        }
        System.out.println("All thread finish...");
    }

    public static class FileCountTask extends RecursiveTask<Integer> {
        private File file;

        public FileCountTask(File file) {
            this.file = file;
        }

        public FileCountTask(String file) {
            this.file = new File(file);
        }

        @Override
        protected Integer compute() {
            int count = 0;
            if (file.isFile()) {
                count += 1;
            } else {
                File[] files = file.listFiles();
                if (Objects.isNull(files)) {
                    files = new File[0];
                }
                List<FileCountTask> subTasks = new LinkedList<>();
                for (File f : files) {
                    if (f.isDirectory()) {
                        FileCountTask task = new FileCountTask(f);
                        subTasks.add(task);
                        task.fork();
                    } else {
                        count += 1;
                    }
                }
                for (FileCountTask subTask : subTasks) {
                    count += subTask.join();
                }
            }
            System.out.printf("%8d     %s %n", count, file.getAbsolutePath());
            return count;
        }
    }
}
```



### 60.1. 参考资料

1. [ForkJoinPool实现原理和源码解析_Java_Java程序员的进阶之路-CSDN博客](https://blog.csdn.net/u010841296/article/details/83963637)
2. [Java多线程进阶（四三）—— J.U.C之executors框架：Fork/Join框架（1） 原理 - 透彻理解Java并发编程 - SegmentFault 思否](https://segmentfault.com/a/1190000016781127)
3. [Java多线程进阶（四四）—— J.U.C之executors框架：Fork/Join框架（2）实现 - 透彻理解Java并发编程 - SegmentFault 思否](https://segmentfault.com/a/1190000016877931)
4. [Java-concurrent-Fork-Join-源码剖析 | Liuye Blog](https://liuyehcf.github.io/2017/08/01/Java-concurrent-Fork-Join-源码剖析/)
5. [JUC源码分析-线程池篇（四）：ForkJoinPool - 1 - 简书](https://www.jianshu.com/p/32a15ef2f1bf)
6. [JUC源码分析-线程池篇（五）：ForkJoinPool - 2 - 简书](https://www.jianshu.com/p/6a14d0b54b8d)

## 61. `ConcurrentHashMap`

![image-20220123153124617](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123153124617-48aad4.png)

![JDK1.8 ConcurrentHashMap Structure](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/JDK1.8-ConcurrentHashMap-Structure-af1e70.jpeg)

### 61.1. 参考资料

- [ConcurrentHashMap源码分析—Java8](http://note.youdao.com/share/?spm=5176.100239.blogcont36781.3.nHffVb&id=dde7a10b98aee57676408bc475ab0680&type=note#/)
- [探索jdk8之ConcurrentHashMap 的实现机制 - 淮左 - 博客园](http://www.cnblogs.com/huaizuo/p/5413069.html) — 参考资料非常棒，建议都看看！
- [ConcurrentHashMap源码分析（JDK8版本） - 惟愿无事 - 博客频道 - CSDN.NET](http://blog.csdn.net/u010723709/article/details/48007881)
- [ConcurrentHashMap实现原理及源码分析 - dreamcatcher-cx - 博客园](https://www.cnblogs.com/chengxiao/p/6842045.html)
- [ConcurrentHashMap 原理解析（JDK1.8） - 简书](https://www.jianshu.com/p/d10256f0ebea)

## 62. `ConcurrentSkipListMap`

没想到 JDK 中，居然有跳跃表的实现！

![ConcurrentSkipListMap search](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/ConcurrentSkipListMap-search-b94e81.jpeg)

## 63. CopyOnWriteArrayList

[`ReentrantReadWriteLock`](https://diguage.github.io/jdk-source-analysis/#) 读写锁的思想非常类似，也就是读读共享、写写互斥、读写互斥、写读互斥。JDK 中提供了 `CopyOnWriteArrayList` 类比相比于在读写锁的思想又更进一步。为了将读取的性能发挥到极致，`CopyOnWriteArrayList` 读取是完全不用加锁的，并且更厉害的是：写入也不会阻塞读取操作。只有写入和写入之间需要进行同步等待。这样一来，读操作的性能就会大幅度提升。

```JAVA
/** The array, accessed only via getArray/setArray. */
private transient volatile Object[] array;

/**
 * Gets the array.  Non-private so as to also be accessible
 * from CopyOnWriteArraySet class.
 */
final Object[] getArray() {
    return array;
}

/**
 * Sets the array.
 */
final void setArray(Object[] a) {
    array = a;
}

@SuppressWarnings("unchecked")
static <E> E elementAt(Object[] a, int index) {
    return (E) a[index];
}

/**
 * {@inheritDoc}
 *
 * @throws IndexOutOfBoundsException {@inheritDoc}
 */
public E get(int index) {
    return elementAt(getArray(), index);
}

/**
 * Appends the specified element to the end of this list.
 *
 * @param e element to be appended to this list
 * @return {@code true} (as specified by {@link Collection#add})
 */
public boolean add(E e) {
    synchronized (lock) {
        Object[] es = getArray();
        int len = es.length;
        es = Arrays.copyOf(es, len + 1);
        es[len] = e;
        setArray(es);
        return true;
    }
}

    /**
 * Removes the element at the specified position in this list.
 * Shifts any subsequent elements to the left (subtracts one from their
 * indices).  Returns the element that was removed from the list.
 *
 * @throws IndexOutOfBoundsException {@inheritDoc}
 */
public E remove(int index) {
    synchronized (lock) {
        Object[] es = getArray();
        int len = es.length;
        E oldValue = elementAt(es, index);
        int numMoved = len - index - 1;
        Object[] newElements;
        if (numMoved == 0)
            newElements = Arrays.copyOf(es, len - 1);
        else {
            newElements = new Object[len - 1];
            System.arraycopy(es, 0, newElements, 0, index);
            System.arraycopy(es, index + 1, newElements, index,
                             numMoved);
        }
        setArray(newElements);
        return oldValue;
    }
}
```

 

`CopyOnWriteArrayList` 类的所有可变操作（`add`，`set` 等等）都是通过创建底层数组的新副本来实现的。当 `List` 需要被修改的时候，我并不修改原有内容，而是对原有数据进行一次复制，将修改的内容写入副本。写完之后，再将修改完的副本替换原来的数据，这样就可以保证写操作不会影响读操作了。

从 `CopyOnWriteArrayList` 的名字就能看出 `CopyOnWriteArrayList` 是满足 `CopyOnWrite` 的 `ArrayList`，所谓 `CopyOnWrite` 也就是说：在计算机，如果你想要对一块内存进行修改时，我们不在原有内存块中进行写操作，而是将内存拷贝一份，在新的内存中进行写操作，写完之后呢，就将指向原来内存指针指向新的内存，原来的内存就可以被回收掉了。

### 63.1. 参考资料

1. [JavaGuide 之 并发容器总结](https://snailclimb.gitee.io/javaguide/#/docs/java/Multithread/并发容器总结)

## 64. ConcurrentLinkedQueue

`ConcurrentLinkedQueue` 主要使用 CAS 非阻塞算法来实现线程安全。这是一个非阻塞队列。对比来看，`LinkedBlockingQueue` 是一个可以阻塞的队列。

## 65. ArrayBlockingQueue

![BlockingQueue](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/BlockingQueue-bfce96.png)

### 65.1. 方法组

|        | 插入                                              | 移除                                  | 检查          |
| :----- | :------------------------------------------------ | :------------------------------------ | :------------ |
| 抛异常 | `boolean add(E e)`                                | `boolean remove(Object o)`            | `E element()` |
| 特定值 | `boolean offer(E e)`                              | `E poll()`                            | `E peek()`    |
| 阻塞   | `void put(E e)`                                   | `E take()`                            |               |
| 超时   | `boolean offer(E e, long timeout, TimeUnit unit)` | `E poll(long timeout, TimeUnit unit)` |               |

四组方法：

- **抛异常**：如果试图的操作无法立即执行，抛一个异常。
- **特定值**：如果试图的操作无法立即执行，返回一个特定的值(通常是 `true`, `false` 或 `null`)。
- **阻塞**：如果试图的操作无法立即执行，该方法调用将会发生阻塞，直到能够执行。
- **超时**：如果试图的操作无法立即执行，该方法调用将会发生阻塞，直到能够执行，但等待时间不会超过给定值。返回一个特定值以告知该操作是否成功(典型的是 true / false)。

`ArrayBlockingQueue` 是 `BlockingQueue` 接口的有界队列实现类，底层采用数组来实现。`ArrayBlockingQueue` 一旦创建，容量不能改变。

`ArrayBlockingQueue` 默认情况下不能保证线程访问队列的公平性，所谓公平性是指严格按照线程等待的绝对时间顺序，即最先等待的线程能够最先访问到 `ArrayBlockingQueue`。而非公平性则是指访问 `ArrayBlockingQueue` 的顺序不是遵守严格的时间顺序，有可能存在，当 `ArrayBlockingQueue` 可以被访问时，长时间阻塞的线程依然无法访问到 `ArrayBlockingQueue`。如果保证公平性，通常会降低吞吐量。

```java
/**
 * Inserts element at current put position, advances, and signals.
 * Call only when holding lock.
 */
private void enqueue(E e) {
    // assert lock.isHeldByCurrentThread();
    // assert lock.getHoldCount() == 1;
    // assert items[putIndex] == null;
    final Object[] items = this.items;
    items[putIndex] = e;
    if (++putIndex == items.length) putIndex = 0;
    count++;
    notEmpty.signal();
}

/**
 * Extracts element at current take position, advances, and signals.
 * Call only when holding lock.
 */
private E dequeue() {
    // assert lock.isHeldByCurrentThread();
    // assert lock.getHoldCount() == 1;
    // assert items[takeIndex] != null;
    final Object[] items = this.items;
    @SuppressWarnings("unchecked")
    E e = (E) items[takeIndex];
    items[takeIndex] = null;
    if (++takeIndex == items.length) takeIndex = 0;
    count--;
    if (itrs != null)
        itrs.elementDequeued();
    notFull.signal();
    return e;
}



/**
 * Inserts the specified element at the tail of this queue if it is
 * possible to do so immediately without exceeding the queue's capacity,
 * returning {@code true} upon success and throwing an
 * {@code IllegalStateException} if this queue is full.
 *
 * @param e the element to add
 * @return {@code true} (as specified by {@link Collection#add})
 * @throws IllegalStateException if this queue is full
 * @throws NullPointerException if the specified element is null
 */
public boolean add(E e) {
    return super.add(e);
}

/**
 * Inserts the specified element at the tail of this queue if it is
 * possible to do so immediately without exceeding the queue's capacity,
 * returning {@code true} upon success and {@code false} if this queue
 * is full.  This method is generally preferable to method {@link #add},
 * which can fail to insert an element only by throwing an exception.
 *
 * @throws NullPointerException if the specified element is null
 */
public boolean offer(E e) {
    Objects.requireNonNull(e);
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        if (count == items.length)
            return false;
        else {
            enqueue(e);
            return true;
        }
    } finally {
        lock.unlock();
    }
}

public E poll() {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        return (count == 0) ? null : dequeue();
    } finally {
        lock.unlock();
    }
}
```

 

底层使用数组来实现，长度确定后就不再变化，通过下标循环往复地使用数组，类似与将数组组成了一个圆。

```JAVA
import org.junit.jupiter.api.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;





public class ArrayBlockingQueueTest {

    @Test
    public void testTimeoutPoll() {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue<Long>(5);
        executorService.execute(() -> {
            for (long i = 0; i < 5; i++) {
                queue.add(i);
                try {
                    Thread.sleep(2 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        for (int i = 0; i < 10; i++) {
            final long time = i;
            executorService.execute(() -> {
                try {
                    Long num = queue.poll(time, TimeUnit.MINUTES);
                    System.out.println("poll:" + num);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        LockSupport.parkNanos(TimeUnit.MINUTES.toNanos(10));
    }
}
```

 

`poll(long, java.util.concurrent.TimeUnit)` 方法，其实就是使用 `Condition notEmpty` 对象来调用 `ConditionObject.awaitNanos(long)` 方法，在其中再调用了 [`LockSupport.parkNanos(java.lang.Object, long)`](https://diguage.github.io/jdk-source-analysis/#) 方法来实现"休眠等待"。

### 65.2. 参考资料

- [JUC集合: BlockingQueue详解 | Java 全栈知识体系](https://www.pdai.tech/md/java/thread/java-thread-x-juc-collection-BlockingQueue.html)

## 66. LinkedBlockingQueue

`LinkedBlockingQueue` 底层是一个单向链表结构。如果需要的话，这一链式结构可以选择一个上限。如果没有定义上限，将使用 `Integer.MAX_VALUE` 作为上限。

## 67. PriorityBlockingQueue

`PriorityBlockingQueue` 是一个支持优先级的无界阻塞队列。默认情况下元素采用自然顺序进行排序，也可以通过自定义类实现 `compareTo()` 方法来指定元素排序规则，或者初始化时通过构造器参数 `Comparator` 来指定排序规则。

`PriorityBlockingQueue` 并发控制采用的是 `ReentrantLock`，队列为无界队列（`ArrayBlockingQueue` 是有界队列，`LinkedBlockingQueue` 也可以通过在构造函数中传入 `capacity` 指定队列最大的容量，但是 `PriorityBlockingQueue` 只能指定初始的队列大小，后面插入元素的时候，如果空间不够的话会自动扩容）。

### 67.1. 参考资料

1. [解读 java 并发队列 BlockingQueue_Javadoop](https://www.javadoop.com/post/java-concurrent-queue)

## 68. DelayQueue

`DelayQueue` 对元素进行持有直到一个特定的延迟到期。注入其中的元素必须实现 `java.util.concurrent.Delayed` 接口，该接口定义:

```JAVA
package java.util.concurrent;

/**
 * A mix-in style interface for marking objects that should be
 * acted upon after a given delay.
 *
 * <p>An implementation of this interface must define a
 * {@code compareTo} method that provides an ordering consistent with
 * its {@code getDelay} method.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Delayed extends Comparable<Delayed> {

    /**
     * Returns the remaining delay associated with this object, in the
     * given time unit.
     *
     * @param unit the time unit
     * @return the remaining delay; zero or negative values indicate
     * that the delay has already elapsed
     */
    long getDelay(TimeUnit unit);
}
```





加入延迟队列的元素都必须实现 `Delayed` 接口。延迟队列内部是利用 `PriorityQueue` 实现的，所以还是利用优先队列！`Delayed` 接口继承了 `Comparable`。因此优先队列是通过 `delay` 来排序的。

示例如下：

```JAVA
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @author D瓜哥, https://www.diguage.com/
 * @since 2020-04-22 16:38
 */
public class DelayQueueTest {
    @Test
    public void test() throws InterruptedException {
        DelayQueue<IntDelay> delayQueue = new DelayQueue<>();
        for (int i = 0; i < 10; i++) {
            delayQueue.add(new IntDelay(i));
        }
        while (!delayQueue.isEmpty()) {
            IntDelay delay = delayQueue.take();
            if (Objects.nonNull(delay)) {
                System.out.println(delay.num);
            }
        }
    }

    public static class IntDelay implements Delayed {

        private int num;
        private long deadline;

        public IntDelay(int num) {
            this.num = num;
            deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(num);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return deadline - System.currentTimeMillis();
        }

        @Override
        public int compareTo(Delayed o) {
            IntDelay param = (IntDelay) o;
            return Integer.compare(this.num, param.num);
        }
    }

}
```



## 69. `ScheduledThreadPoolExecutor`

`Timer`、`ScheduledThreadPool` 和 `DelayQueue`，总结的说下它们都是通过优先队列来获取最早需要执行的任务，因此插入和删除任务的时间复杂度都为 O(logn)，并且 `Timer` 、`ScheduledThreadPool` 的周期性任务是通过重置任务的下一次执行时间来完成的。

问题就出在时间复杂度上，插入删除时间复杂度是O(logn)，那么假设频繁插入删除次数为 `m`，总的时间复杂度就是 O(mlogn)，这种时间复杂度满足不了 Kafka 这类中间件对性能的要求，而时间轮算法的插入删除时间复杂度是 O(1)。我们来看看时间轮算法是如何实现的。

### 69.1. 参考资料

1. [面试官：知道时间轮算法吗？在Netty和Kafka中如何应用的？](https://mp.weixin.qq.com/s/xBB72hJGn8geZ7SkM0FqJw)

## 70. ClassLoader

虽然对 `ClassLoader` 的双亲委派流程比较了解，但是一直没有仔细专研过代码实现。今天翻看代码，代码写的简单明了：

```JAVA
/**
 * Loads the class with the specified <a href="#binary-name">binary name</a>.  The
 * default implementation of this method searches for classes in the
 * following order:
 *
 * <ol>
 *
 *   <li><p> Invoke {@link #findLoadedClass(String)} to check if the class
 *   has already been loaded.  </p></li>
 *
 *   <li><p> Invoke the {@link #loadClass(String) loadClass} method
 *   on the parent class loader.  If the parent is {@code null} the class
 *   loader built into the virtual machine is used, instead.  </p></li>
 *
 *   <li><p> Invoke the {@link #findClass(String)} method to find the
 *   class.  </p></li>
 *
 * </ol>
 *
 * <p> If the class was found using the above steps, and the
 * {@code resolve} flag is true, this method will then invoke the {@link
 * #resolveClass(Class)} method on the resulting {@code Class} object.
 *
 * <p> Subclasses of {@code ClassLoader} are encouraged to override {@link
 * #findClass(String)}, rather than this method.  </p>
 *
 * <p> Unless overridden, this method synchronizes on the result of
 * {@link #getClassLoadingLock getClassLoadingLock} method
 * during the entire class loading process.
 *
 * @param  name
 *         The <a href="#binary-name">binary name</a> of the class
 *
 * @param  resolve
 *         If {@code true} then resolve the class
 *
 * @return  The resulting {@code Class} object
 *
 * @throws  ClassNotFoundException
 *          If the class could not be found
 */
protected Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException
{
    synchronized (getClassLoadingLock(name)) {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            long t0 = System.nanoTime();
            try {
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // ClassNotFoundException thrown if class not found
                // from the non-null parent class loader
            }

            if (c == null) {
                // If still not found, then invoke findClass in order
                // to find the class.
                long t1 = System.nanoTime();
                c = findClass(name);

                // this is the defining class loader; record the stats
                PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                PerfCounter.getFindClasses().increment();
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}

/**
 * Finds the class with the specified <a href="#binary-name">binary name</a>.
 * This method should be overridden by class loader implementations that
 * follow the delegation model for loading classes, and will be invoked by
 * the {@link #loadClass loadClass} method after checking the
 * parent class loader for the requested class.
 *
 * @implSpec The default implementation throws {@code ClassNotFoundException}.
 *
 * @param  name
 *         The <a href="#binary-name">binary name</a> of the class
 *
 * @return  The resulting {@code Class} object
 *
 * @throws  ClassNotFoundException
 *          If the class could not be found
 *
 * @since  1.2
 */
protected Class<?> findClass(String name) throws ClassNotFoundException {
    throw new ClassNotFoundException(name);
}
```



如果自定义 `ClassLoader`，最简单的办法就是重载 `Class<?> findClass(String name)` 方法就可以了。或者为了避免双亲委托机制，可以自己定义一个类加载器，然后重写 `loadClass()` 即可。

```JAVA
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Objects;




public class ClassLoaderTest {
    public static void main(String[] args) {
        System.out.println("ClassLodarDemo's ClassLoader is " + ClassLoaderTest.class.getClassLoader());
        System.out.println("The Parent of ClassLodarDemo's ClassLoader is " + ClassLoaderTest.class.getClassLoader().getParent());
        System.out.println("The GrandParent of ClassLodarDemo's ClassLoader is " + ClassLoaderTest.class.getClassLoader().getParent().getParent());
    }

    @Test
    public void test() throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException,
            InstantiationException {
        DiguageClassLoader loader = new DiguageClassLoader();
        // 如何识别内部类？
        // 如何获取内部类的正确类名？
        Class<?> clazz = loader.loadClass(
                "com.diguage.truman.ClassLoaderTest.HelloWorld");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        System.out.println(instance.toString());
    }

    public static class HelloWorld {
        @Override
        public String toString() {
            return "Hello, https://www.diguage.com/";
        }
    }

    public static class DiguageClassLoader extends ClassLoader {
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (Objects.isNull(name)) {
                throw new IllegalArgumentException("class name is null.");
            }
            String fileName = name.replaceAll("\\.", "/") + ".class";
            int index = fileName.lastIndexOf("/");
            fileName = fileName.substring(0, index) + "$"
                    + fileName.substring(index + 1);
            InputStream inputStream = getResourceAsStream(fileName);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                int size = 0;
                byte[] bytes = new byte[1024];
                while ((size = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, size);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] bytecodes = outputStream.toByteArray();
            if (bytecodes.length == 0) {
                throw new ClassNotFoundException(name);
            }
            int i = name.lastIndexOf(".");
            String className = name.substring(0, i) + "$" + name.substring(i + 1);
            return defineClass(className, bytecodes, 0, bytecodes.length);
        }
    }
}
```

 

1. `jdk.internal.loader.ClassLoaders.PlatformClassLoader`
2. `jdk.internal.loader.ClassLoaders.AppClassLoader`

![image-20220123153720741](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123153720741-b29911.png)

Figure 1. JDK 8 及以前的加载体系

在 JDK 8 及以前，`AppClassLoader` 和 `ExtClassLoader` 这两个类都是 `sun.misc.Launcher` 中的内部类。 `BootstrapClassLoader` 是由 C++ 代码实现的。所以，不存在 Java 类定义。

![load class process](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/22/load-class-process-173f6b.png)

![image-20220123153755500](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123153755500-3c6cb3.png)

Figure 2. JDK 9 之后的加载体系

在 JDK 9 以后，`AppClassLoader`，`PlatformClassLoader` 和 `BootClassLoader` 三个类都定义在 `jdk.internal.loader.ClassLoaders` 中。`BootClassLoader` 是由 Java 和 C++ 混合实现，所以有类的定义。

`ClassLoader` 提供的资源加载的方法中的核心方法是 `ClassLoader#getResource(String name)`，它是基于用户应用程序的 ClassPath 搜索资源，遵循"资源加载的双亲委派模型"，资源名称必须使用路径分隔符 `/` 去分隔目录，但是不能以 `/` 作为资源名的起始字符，其他几个方法都是基于此方法进行衍生，添加复数操作等其他操作。`getResource(String name)` 方法不会显示抛出异常，当资源搜索失败的时候，会返回 `null`。

- 如何识别内部类？
- 如何获取内部类的正确类名？

### 70.1. 参考资料

1. [通过源码浅析Java中的资源加载 - Throwable’s Blog](http://www.throwable.club/2018/11/30/java-resource-load/)

## 71. Field

属性操作方法 `Field#set(Object obj, Object value)` 和 `Field#get(Object obj)` 底层都是委托到 `jdk.internal.reflect.FieldAccessor` 实现。

`FieldAccessor` 接口有很多的实现，`FieldAccessor` 接口实例是通过 `jdk.internal.reflect.ReflectionFactory` 这个工厂构造的：

jdk.internal.reflect.ReflectionFactory

```JAVA
public FieldAccessor newFieldAccessor(Field field, boolean override) {
        checkInitted();

        Field root = langReflectAccess.getRoot(field);
        if (root != null) {
            // FieldAccessor will use the root unless the modifiers have
            // been overrridden
            if (root.getModifiers() == field.getModifiers() || !override) {
                field = root;
            }
        }
        return UnsafeFieldAccessorFactory.newFieldAccessor(field, override);
    }
```



最终委托到 `UnsafeFieldAccessorFactory#newFieldAccessor()`。

`UnsafeObjectFieldAccessorImpl` 中除了 `get(Object obj)` 和 `set(Object obj, Object value)` 方法，其他方法都是直接抛出 `IllegalArgumentException`。而 `get(Object obj)` 和 `set(Object obj, Object value)` 底层分别依赖于 `jdk.internal.misc.Unsafe` 的 `putObject(obj, fieldOffset, value)` 和 `getObject(obj, fieldOffset)` 方法。而属性的内存偏移地址是在 `UnsafeObjectFieldAccessorImpl` 的父类 `UnsafeFieldAccessorImpl` 的构造函数中计算出来的。

属性反射操作 `Field` 的 `setXX` 和 `getXX` 方法最终委托到 `jdk.internal.misc.Unsafe` 的 `putXX` 和 `getXX` 方法，而属性的内存偏移地址是通过 `jdk.internal.misc.Unsafe` 的 `staticFieldBase()`、`staticFieldOffset` 和 `objectFieldOffset` 几个方法计算的。

### 71.1. 参考资料

1. [深入分析Java反射(七)-简述反射调用的底层实现 - Throwable’s Blog](http://www.throwable.club/2018/12/16/java-reflection-implementance/)

## 72. Proxy

```java
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;




public class ProxyTest {
    public static class LogProxy implements InvocationHandler {
        private Object realObject;

        public LogProxy(Object realObject) {
            this.realObject = realObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            System.out.println("Proxy: " + proxy.getClass().getName());
            System.out.println("start to invoke: "
                    + realObject.getClass().getName() + "#" + method.getName()
                    + " args =" + Arrays.toString(args));
            return method.invoke(realObject, args);
        }
    }

    public static interface UserGetService {
        String getById(Integer id);
    }

    static interface UserPostService {
        String postUser(String name);
    }

    public static class UserGetServiceImpl implements UserGetService, UserPostService {
        @Override
        public String getById(Integer id) {
            return "linKe-" + id;
        }

        @Override
        public String postUser(String name) {
            return "119-" + name;
        }
    }

    public static void main(String[] args) {
        // 注意：这里不能使用 JUnit 来运行，JUnit 也是通过代理启动的，
        // 先于我们的测试运行，导致设置无效。
        System.getProperties()
                .put("jdk.proxy.ProxyGenerator.saveGeneratedFiles", "true");

        UserGetServiceImpl userService = new UserGetServiceImpl();
        ClassLoader classLoader = UserGetService.class.getClassLoader();
        Class<?>[] interfaces = UserGetServiceImpl.class.getInterfaces();
        Object proxy = Proxy.newProxyInstance(classLoader,
                interfaces, new LogProxy(userService));
        System.out.println("UserName = "
                + ((UserGetService) proxy).getById(119));

        System.out.println("UserCode = "
                + ((UserPostService) proxy).postUser("diguage"));

        Object proxy2 = Proxy.newProxyInstance(classLoader,
                interfaces, new LogProxy(userService));
        System.out.println("UserName = "
                + ((UserGetService) proxy2).getById(119));

        System.out.println("UserCode = "
                + ((UserPostService) proxy2).postUser("diguage"));
    }

    @Test
    public void testGetCallerMethodName() {
        System.out.println(getCallerMethod());

        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        System.out.println(methodName);
    }

    public String getCallerMethod() {
        String methodName = Thread.currentThread()
                .getStackTrace()[2] // 注意下标值
                .getMethodName();
        return methodName;
    }
}
```

 

![Proxy-newProxyInstance-sequence-diagram](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/Proxy-newProxyInstance-sequence-diagram-0eb217.png)

跟着代码整体走下来，所谓的"动态代理"，其实是在 `java.lang.reflect.ProxyGenerator.generateProxyClass(java.lang.String, java.lang.Class<?>[], int)` 中生成了一个实现了接口的代理类。生成字节码的逻辑封装在了 `java.lang.reflect.ProxyGenerator.generateClassFile` 中，按照字节码规范中规定的格式（魔数、版本号、常量池、访问标识符、当前类索引、父类索引、接口索引、字段表、方法表、附加属性），一点一点追加内容。

生成出来的类，继承了 `java.lang.reflect.Proxy`，同时实现了参数中传递的接口。在生成的类中，

- 包含一个参数为 `InvocationHandler` 的构造函数，用于保存代理业务的实现；
- 每一个方法都用一个静态 `Method` 来表示；
- 除了接口中的方法，还会生成 `boolean equals(Object obj)`，`int hashCode()` 和 `String toString()` 三个方法。

调用时，通过 `InvocationHandler` 对象的 `Object invoke(Object proxy, Method method, Object[] args)` 方法来调起代理和目标对象的方法。其中，这里的 `Object proxy` 就是生成的类本身的对象；`Method method` 就是上述生成的静态 `Method` 对象；`Object[] args` 就是实际调用的参数。

反编译的示例类

```JAVA
import com.diguage.proxy.UserService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;



public final class $Proxy0 extends Proxy implements UserService {
    private static Method m0;
    private static Method m1;
    private static Method m2;
    private static Method m3;

    public $Proxy0(InvocationHandler var1) throws  {
        super(var1);
    }

    public final int hashCode() throws  {
        try {
            return (Integer)super.h.invoke(this, m0, (Object[])null);
        } catch (RuntimeException | Error var2) {
            throw var2;
        } catch (Throwable var3) {
            throw new UndeclaredThrowableException(var3);
        }
    }

    public final boolean equals(Object var1) throws  {
        try {
            return (Boolean)super.h.invoke(this, m1, new Object[]{var1});
        } catch (RuntimeException | Error var3) {
            throw var3;
        } catch (Throwable var4) {
            throw new UndeclaredThrowableException(var4);
        }
    }

    public final String toString() throws  {
        try {
            return (String)super.h.invoke(this, m2, (Object[])null);
        } catch (RuntimeException | Error var2) {
            throw var2;
        } catch (Throwable var3) {
            throw new UndeclaredThrowableException(var3);
        }
    }

    public final String getById(Integer var1) throws  {
        try {
            return (String)super.h.invoke(this, m3, new Object[]{var1});
        } catch (RuntimeException | Error var3) {
            throw var3;
        } catch (Throwable var4) {
            throw new UndeclaredThrowableException(var4);
        }
    }

    static {
        try {
            m0 = Class.forName("java.lang.Object").getMethod("hashCode");
            m1 = Class.forName("java.lang.Object").getMethod("equals",
                     Class.forName("java.lang.Object"));
            m2 = Class.forName("java.lang.Object").getMethod("toString");
            m3 = Class.forName("com.diguage.proxy.UserService")
                     .getMethod("getById", Class.forName("java.lang.Integer"));
        } catch (NoSuchMethodException var2) {
            throw new NoSuchMethodError(var2.getMessage());
        } catch (ClassNotFoundException var3) {
            throw new NoClassDefFoundError(var3.getMessage());
        }
    }
}
```



|      | 为了排版，做了小调整。 |
| ---- | ---------------------- |

还有两点值得注意：

1. 运行代理时，如果想要保存生成的代理类字节码，需要系统属性 `jdk.proxy.ProxyGenerator.saveGeneratedFiles` 设置为 `true`。这个属性被解析后赋值给了 `java.lang.reflect.ProxyGenerator.saveGeneratedFiles` 字段，这个字段是 `final` 的。所以，需要在运行代码之初就要设置这个属性。所以，最好使用 `main` 方法来运行测试。否则，有可能设置失效。
2. 如果代码是在 Maven 项目中运行，如果接口都是 `public` 修饰，生成的类会被保存在 `${project.basedir}/com/sun/proxy/` 目录下；如果有接口是包私有的，则生成的类为接口所在的包。如果目录不存在，则会自动创建。
3. 最多可以有 `65535` 个接口；有两个解释：
   1. 代码中有明确限制：在 `java.lang.reflect.Proxy.ProxyBuilder.ProxyBuilder(java.lang.ClassLoader, java.util.List<java.lang.Class<?>>)` 中有 `interfaces.size() > 65535` 的判断语句。
   2. 字节码中，对于接口数量是用一个 `u2` 变量表示的，该变量的最大值是 `216 - 1 = 65535`。

注解底层也是基于动态代理实现的。

```java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;




public class ProxyAnnoTest {
    @Diguage
    public static class AnnoTest {
    }

    @Diguage("https://github.com/diguage")
    public static class AnnoTest2 {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.TYPE)
    static @interface Diguage {
        String value() default "https://www.diguage.com";

        String name() default "D瓜哥";
    }

    public static void main(String[] args) {
        System.getProperties()
                .put("jdk.proxy.ProxyGenerator.saveGeneratedFiles", "true");

        Class<AnnoTest> clazz = AnnoTest.class;
        Diguage annotation = clazz.getAnnotation(Diguage.class);
        System.out.println(annotation + " : " + annotation.hashCode());
        System.out.println("Name: " + annotation.name());
        System.out.println("Value: " + annotation.value());

        Class<? extends Diguage> annoClass = annotation.getClass();


        System.out.println("\n----Class----");
        String className = annoClass.getName();

        System.out.println("\n----SuperClass----");
        System.out.println(annoClass.getSuperclass().getName());

        System.out.println("\n----Interfaces----");
        System.out.println(Arrays.toString(annoClass.getInterfaces()));

        System.out.println("\n----Methods----");
        System.out.println(Arrays.toString(annoClass.getDeclaredMethods())
                .replaceAll(", p", ",\n p"));

        System.out.println("\n\n==============");
        Diguage anno2 = AnnoTest2.class.getAnnotation(Diguage.class);
        System.out.println(anno2 + " : " + anno2.hashCode());
    }
}
```



每个注解都是一个接口声明，然后基于这个接口使用动态代理生成一个代理类。而被标注的注解，就是一个代理类的实例对象。

代理类中的 `InvocationHandler` 则是 `AnnotationInvocationHandler` 实例，实例变量 `Map<String, Object> memberValues` 保存着注解中成员属性的名称和值的映射，注解成员属性的名称实际上就对应着接口中抽象方法的名称。

总结

1. 用反射 + 字节码生成技术来生成字节码，然后加载出来代理对象。
2. 从java的角度来看这本语言,就是一个动态性语言,一切的动态性来源于类的加载方式, 在程序运行期间,可以很大程度上修改class
3. `newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h)` 实际上从生成的 Class 文件和这个传递参数来看 jdk Proxy 仅仅对于接口进行代理, 即生成实现了接口的临时类对象.
4. 由于生成的类，继承了 `java.lang.reflect.Proxy` 类，而 Java 是单继承的。所以，动态代理只能代理生成接口，不能代理类。

既然都生成代理类了，为什么不直接继承代理类呢？这样就可以对代理类所有的方法进行增强了。

### 72.1. 思考题

如何自己写代码来实现代码生成、加载类的动态代理？

1. 如何生成代理类？-- 可以考虑直接生成 Java 代码，然后调用 `JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();` 获取编译器来编译 Java 代码。
2. 如何加载类？-- 可以使用 `java.net.URLClassLoader` 来加载字节码文件。
3. 如何抽象代理动作？
4. 如何在方法内获取参数列表？

### 72.2. CGLIB

1. 通过生成子类的方式来产生代理，某些情况比动态代理运行速度要快一些。
2. 不能代理 `final` 修饰的类。

### 72.3. 参考资料

1. [java动态代理实现与原理详细分析 - Gonjian - 博客园](https://www.cnblogs.com/gonjan-blog/p/6685611.html)
2. [java动态代理 | 博客](https://www.fancylight.top/2019/05/22/java动态代理/#代理类文件)
3. [字节码增强技术探索 - 美团技术团队](https://tech.meituan.com/2019/09/05/java-bytecode-enhancement.html)
4. [Get name of current method being executed in Java - GeeksforGeeks](https://www.geeksforgeeks.org/get-name-of-current-method-being-executed-in-java/)
5. [深入分析Java反射(四)-动态代理 - Throwable’s Blog](http://www.throwable.club/2018/12/08/java-reflection-dynamic-proxy/)
6. [CGLIB动态代理原理分析 - Throwable’s Blog](http://www.throwable.club/2018/12/16/cglib-dynamic-proxy-analyze/)
7. [JDK中注解的底层实现 - Throwable’s Blog](http://www.throwable.club/2020/03/16/annotation-implementation/)

## 73. ServerSocket

在纠结了 N 久之后，终于动手写程序完成了 Socket 全双工实验。实验证实，Socket 在接受的同时，还可以发送报文。

![image-20220123154439350](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123154439350-153d90.png)

```JAVA

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.LockSupport;

/**
 * Socket 全双工演示示例
 *
 */
public class SocketFullDuplexTest {
    private int port = 11233;

    @Test
    public void testServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket socket = serverSocket.accept();
        InputStream inputStream = socket.getInputStream();

        new Thread(() -> {
            InputStreamReader reader = new InputStreamReader(new BufferedInputStream(inputStream));
            BufferedReader bufferedReader = new BufferedReader(reader);
            try {
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                    Thread.sleep(100);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }).start();

        OutputStream outputStream = socket.getOutputStream();
        new Thread(() -> {
            try {
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    String msg = "S-" + i + "\n";
                    System.out.println("msg = " + msg);
                    outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    Thread.sleep(100);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }).start();

        LockSupport.park();
    }

    @Test
    public void testClient() throws IOException {
        Socket socket = new Socket("localhost", port);

        InputStream inputStream = socket.getInputStream();
        new Thread(() -> {
            InputStreamReader reader = new InputStreamReader(new BufferedInputStream(inputStream));
            BufferedReader bufferedReader = new BufferedReader(reader);
            try {
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                    Thread.sleep(100);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }).start();

        OutputStream outputStream = socket.getOutputStream();
        new Thread(() -> {
            try {
                for (int i = 0; i < Integer.MAX_VALUE; i++) {
                    String msg = "C-" + i + "\n";
                    System.out.println("msg = " + msg);
                    outputStream.write(msg.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    Thread.sleep(100);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }).start();

        LockSupport.park();
    }
}
```



### 73.1. 参考资料

1. [P2E for Students: FP303 CN: Simplex, Half-Duplex and Full-Duplex](http://hafizahabdullah.blogspot.com/2013/08/fp303-cn-simplex-half-duplex-and-full.html)

## 74. `ServiceLoader`

```java
public interface ServiceLoaderSay {
    void say();
}
```

```java
public class ServiceLoaderSayHello implements ServiceLoaderSay {
    @Override
    public void say() {
        System.out.println("Hello, https://www.diguage.com/");
    }
}
```

```java
public class ServiceLoaderSayGoodbye implements ServiceLoaderSay {
    @Override
    public void say() {
        System.out.println("Goodbye, https://www.diguage.com/");
    }
}
```

```java
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.ServiceLoader;




public class ServiceLoaderTest {
    @Test
    public void test() {
        ServiceLoader<ServiceLoaderSay> loader
                = ServiceLoader.load(ServiceLoaderSay.class);
        Iterator<ServiceLoaderSay> iterator = loader.iterator();
        while (iterator.hasNext()) {
            ServiceLoaderSay say = iterator.next();
            say.say();
        }
    }
}
```



经过测试，有几点需要注意：

1. 只支持 `public` 的类。测试，这是因为内部需要创建对象，其他访问控制不能在 `ServiceLoader` 类中创建对象。
2. 不支持 `Outter.Inner` 这样的内部类 ，即使是 `public static class`。

### 74.1. 参考资料

1. [浅析JDK中ServiceLoader的源码 - Throwable’s Blog](http://www.throwable.club/2018/11/30/java-service-loader/) — 这里的代码是基于 JDK 8 的，和 JDK 11 的代码已经相差很大。

## 75. `String`

```JAVA
public class StringUtils {
  public static String switchFormat(int cur, int length) {
    String str = "" + cur;
    int q = length - str.length();
    switch (q) {
      case 0:
        break;
      case 1:
        str = "0" + str;
        break;
      case 2:
        str = "00" + str;
        break;
      case 3:
        str = "000" + str;
        break;
      case 4:
        str = "0000" + str;
        break;
      case 5:
        str = "00000" + str;
        break;
      case 6:
        str = "000000" + str;
        break;
      default:
        break;
    }
    return str;
  }

  public static String format(int cur, int len) {
    return String.format("%0" + len + "d", cur);
  }
}
```



## 76. `Date`

```java
import org.apache.commons.lang3.time.FastDateFormat;
import org.joda.time.format.DateTimeFormat;
import org.openjdk.jmh.annotations.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.diguage.truman.DateFormatTest.DateFormatUtils.yyyyMMdd;


@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5)
@Fork(1)
@Threads(8)
public class DateFormatTest {

  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyyyMMdd);
  private static final FastDateFormat fastFormat = FastDateFormat.getInstance(yyyyMMdd);
  private static final org.joda.time.format.DateTimeFormatter
    jodaFormat = DateTimeFormat.forPattern(yyyyMMdd);

  @Benchmark
  public String testSimpleDateFormat() {
    SimpleDateFormat format = new SimpleDateFormat(yyyyMMdd);
    return format.format(new Date());
  }

  @Benchmark
  public String testLocalSimpleDateFormat() {
    return DateFormatUtils.formatDate(new Date());
  }

  @Benchmark
  public String testVariaFormatter() {
    LocalDateTime now = LocalDateTime.now();
    return now.format(DateTimeFormatter.ofPattern(yyyyMMdd));
  }

  @Benchmark
  public String testConstFormatter() {
    LocalDateTime now = LocalDateTime.now();
    return now.format(formatter);
  }

  @Benchmark
  public String testConstFormatterDate() {
    Date date = new Date();
    LocalDateTime now = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    return now.format(formatter);
  }

  @Benchmark
  public String testConstFormatterDateZ() {
    Date date = new Date();
    ZonedDateTime now = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    return now.format(formatter);
  }

  @Benchmark
  public String testVariaFastDateFormat() {
    FastDateFormat format = FastDateFormat.getInstance(yyyyMMdd);
    return format.format(new Date());
  }

  @Benchmark
  public String testConstFastDateFormat() {
    return fastFormat.format(new Date());
  }

  @Benchmark
  public String testJodaFormat() {
    org.joda.time.LocalDateTime now = org.joda.time.LocalDateTime.fromDateFields(new Date());
    return jodaFormat.print(now);
  }

  public static class DateFormatUtils {
    public static final String yyyyMMdd = "yyyyMMdd";
    private static ThreadLocal<DateFormat> dateFormatThreadLocal
      = ThreadLocal.withInitial(() -> new SimpleDateFormat(yyyyMMdd));

    public static String formatDate(Date date) {
      return dateFormatThreadLocal.get().format(date);
    }
  }
}
```

 

## 77. `Serializable`

```JAVA
import java.io.*;
import java.util.StringJoiner;

public class OuterClass implements Serializable {
  private int age = 119;
  private String name = "D瓜哥";

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", OuterClass.class.getSimpleName() + "[", "]")
      .add("age=" + age)
      .add("name='" + name + "'")
      .toString();
  }

  public static class InnerClass {
    private int iage = 120;
    private String iname = "https://www.diguage.com";

    public int getIage() {
      return iage;
    }

    public void setIage(int iage) {
      this.iage = iage;
    }

    public String getIname() {
      return iname;
    }

    public void setIname(String iname) {
      this.iname = iname;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", InnerClass.class.getSimpleName() + "[", "]")
        .add("iage=" + iage)
        .add("iname='" + iname + "'")
        .toString();
    }

    public static void main(String[] args) throws Throwable {
      test(new OuterClass());
      test(new InnerClass());
    }

    private static void test(Object param) throws Exception {
      System.out.println("param = " + param);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(param);

      ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object object = ois.readObject();
      System.out.println("deser = " + object);
    }
  }
}
```



## 78. Netty

Netty 和 `ServerSocketChannel` 可以互相操作吗？

### 78.1. JDK 原生 Socket 编程

使用 JDK 原生的 Socket API 进行网络编程：

```JAVA
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.LockSupport;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author D瓜哥, https://www.diguage.com/
 * @since 2020-04-21 14:31
 */
public class ServerSocketTest {
  public static final String HOST = "127.0.0.1";
  public static final int PORT = 11911;

  @Test
  public void testServer() {
    Server server = new Server(PORT);
    server.start();
    LockSupport.park();
  }

  @Test
  public void testClient() throws IOException {
    Socket socket = new Socket(HOST, PORT);
    new Thread(() -> {
      System.out.println("客户端启动成功");
      while (true) {
        try {
          String message = "Hello, D瓜哥！";
          System.out.println("客户端发送数据：" + message);
          socket.getOutputStream().write(message.getBytes(UTF_8));
        } catch (IOException e) {
          System.out.println("写入数据报错！");
          e.printStackTrace();
        }
        sleep();
      }
    }).start();
    LockSupport.park();
  }

  private void sleep() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static class Server {
    private ServerSocket serverSocket;

    public Server(int port) {
      try {
        this.serverSocket = new ServerSocket(port);
        System.out.println("服务端启动成功，端口号：" + port);
      } catch (IOException e) {
        System.out.println("服务端启动失败");
        e.printStackTrace();
      }
    }

    public void start() {
      new Thread(() -> doStart()).start();
    }

    private void doStart() {
      while (true) {
        try {
          Socket socket = serverSocket.accept();
          new ClientHandler(socket).start();
        } catch (IOException e) {
          System.out.println("服务端异常");
          e.printStackTrace();
        }
      }
    }
  }

  private static class ClientHandler {
    public static final int MAX_DATA_LEN = 1024;
    private final Socket socket;

    public ClientHandler(Socket socket) {
      this.socket = socket;
    }

    public void start() {
      System.out.println("新客户端接入");
      new Thread(() -> doStart()).start();
    }

    private void doStart() {
      try {
        InputStream inputStream = socket.getInputStream();
        while (true) {
          byte[] data = new byte[MAX_DATA_LEN];
          int len;
          while ((len = inputStream.read(data)) != -1) {
            String message = new String(data, 0, len);
            System.out.println("客户端传来消息：" + message);
            socket.getOutputStream().write(data);
          }
        }
      } catch (IOException e) {
        System.out.println("服务端读取错误失败");
        e.printStackTrace();
      }
    }
  }
}
```



- 阻塞
- 高性能

### 78.2. Server startup

```JAVA

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.locks.LockSupport;





public class Test03 {
  private static final int PORT = 11911;

  @Test
  public void testServer() throws InterruptedException {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childAttr(AttributeKey.newInstance("childAttr"), null)
        .handler(new ServerHandler())
        .childHandler(new ChannelInitializer<>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {

          }
        });
      ChannelFuture f = b.bind(PORT).sync();
      f.addListener(future -> {
        if (future.isSuccess()) {
          System.out.println(LocalDateTime.now() + ": 端口[" + PORT + "]绑定成功！");
        } else {
          System.out.println(LocalDateTime.now() + ": 端口[" + PORT + "]绑定失败！");
        }
      });
      f.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
    LockSupport.park();
  }

  public static class ServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      System.out.println("channelRegistered");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      System.out.println("channelActive");
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      System.out.println("handlerAdded");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//            ((ByteBuf) msg).release();
//            ReferenceCountUtil.release(msg);
      ctx.write(msg);
      ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      cause.printStackTrace();
      ctx.close();
    }
  }

  @Test
  public void testClient() throws InterruptedException {
    NioEventLoopGroup executors = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(executors)
        .channel(NioSocketChannel.class)
        .remoteAddress(new InetSocketAddress("localhost", PORT))
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new ClientHandler());
          }
        });
    } finally {
      executors.shutdownGracefully().sync();
    }
  }

  public static class ClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      System.out.println("Handl");
    }
  }
}
```

  

两个问题

1. 服务端的 Socket 在哪里初始化？
2. 在哪里 accept 连接？

**Netty 服务端启动**

1. 创建服务端 `Channel`
   1. `io.netty.bootstrap.AbstractBootstrap.bind(int)`
   2. `io.netty.bootstrap.AbstractBootstrap.doBind(SocketAddress)`
   3. `io.netty.bootstrap.AbstractBootstrap.initAndRegister()`
2. 初始化服务端 `Channel`
   1. `NioServerSocketChannel.NioServerSocketChannel()` 在 `NioServerSocketChannel` 默认初始化函数中，使用了 JDK 内置的 `SelectorProvider.provider()` 方法返回的 `SelectorProvider` 对象。
   2. 在 `NioServerSocketChannel.newSocket(SelectorProvider)` 方法中，调用 `provider.openServerSocketChannel()` 来创建 `ServerSocketChannel` 对象。
   3. 在 `AbstractNioChannel.AbstractNioChannel(Channel, SelectableChannel, int)` 构造函数中，设置 `selectableChannel.configureBlocking(false)`。
3. 注册 Selector
4. 端口绑定 — 调用底层 API，实现对端口的绑定。

**服务端 `Channel` 初始化过程**

1. 通过 `bind()` 进入
2. `initAndRegister()`
3. 在 `AbstractBootstrap.initAndRegister()` 中通过 `channelFactory.newChannel()` 利用反射机制来创建 `Channel`。
4. 在 `ServerBootstrap.init(Channel)` 中，初始化 `Channel`
   1. `setChannelOptions`
   2. `setAttributes`
   3. 配置 `ChannelHandler` — 配置服务端 pipeline。
      1. 初始化时，调用 `AbstractBootstrap.handler(io.netty.channel.ChannelHandler)`，配置 `ChannelHandler` 对象
      2. 通过调用 `AbstractBootstrap.initAndRegister()` 方法调用 `ServerBootstrap.init(Channel)` 方法，在其中，将 `ChannelHandler` 对象追加到 `Channel` 对象的 pipeline 的最后面。
   4. add `ServerBootstrapAcceptor`
      1. 上一步执行完毕后，在 `ServerBootstrap.init(Channel)` 方法中，会创建一个 `ServerBootstrapAcceptor` 对象添加到 pipeline 后面。

**注册 selector**

1. `AbstractChannel.AbstractUnsafe.register(EventLoop, ChannelPromise)` 入口

   1. `AbstractChannel.this.eventLoop = eventLoop;` 绑定线程

   2. `AbstractChannel.AbstractUnsafe.register0(ChannelPromise)` 实际注册

      1. `AbstractChannel.doRegister()` 调用底层 JDK API 注册

      2. `pipeline.invokeHandlerAddedIfNeeded()`

      3. `pipeline.fireChannelRegistered()`

         从示例代码的输出可以看出，`Test03.ServerHandler` 中三个"事件"方法被调用的顺序是： `handlerAdded`，`channelRegistered` 和 `channelActive`。

**端口绑定**

1. `AbstractChannel.AbstractUnsafe.bind(SocketAddress, ChannelPromise)` 入口
   1. `AbstractBootstrap.doBind(SocketAddress)`
      1. `javaChannel().bind()` JDK 底层绑定 `io.netty.channel.AbstractChannel.AbstractUnsafe.bind`
         1. ` pipeline.fireChannelActive()` 传播事件
      2. `HeadContext.readIfIsAutoRead()`

### 78.3. `NioEventLoop`

1. 默认情况下，Netty 服务端起多少个线程？何时启动？
2. Netty 是如何解决 JDK 空轮询 Bug 的？
3. Netty 如何保证异步串行无锁化？

1. `NioEventLoop` 创建
2. `NioEventLoop` 启动
3. `NioEventLoop` 执行逻辑

#### 78.3.1. `NioEventLoop` 创建

`NioEventLoop` 默认是在调用 `NioEventLoopGroup()` 时被创建，默认是 2 倍的 CPU 数量（由常量 `MultithreadEventLoopGroup.DEFAULT_EVENT_LOOP_THREADS` 来定义）。

在 `MultithreadEventExecutorGroup.MultithreadEventExecutorGroup(int, Executor, EventExecutorChooserFactory, Object…)` 构造函数中：

1. 创建 `new ThreadPerTaskExecutor(newDefaultThreadFactory())` 线程池；

   每次执行任务都会创建一个线程实体。

   `NioEventLoop` 线程命名规则 `nioEventLoop-1-XX`。在 `io.netty.util.concurrent.DefaultThreadFactory` 中可以看到。

   这里还有两点需要注意：创建的线程对象和 `Runable` 被分别包装成了 `FastThreadLocalThread` 和 `FastThreadLocalRunnable`，主要是对 `ThreadLocal` 做了一些优化。

2. 使用 `for` 循环，利用 `MultithreadEventExecutorGroup.newChild(Executor, Object…)` 方法创建 `NioEventLoop` 对象。

   有三个作用：①保存线程执行器 `ThreadPerTaskExecutor`；②创建一个 `MpscQueue`；③创建一个 selector。

   在 `NioEventLoop.newTaskQueue(int)` 方法，然后调用 `NioEventLoop.newTaskQueue0(int)` 方法，创建 `MpscQueue`。

3. 调用 `DefaultEventExecutorChooserFactory.newChooser(EventExecutor[])` 方法，创建线程选择器。

   `isPowerOfTwo()` 判断是否是 2 的幂，如果是则返回 `PowerOfTwoEventExecutorChooser`（优化），返回 `index & (length - 1)`；否则返回 `GenericEventExecutorChooser`（普通），返回 `abs(index % length)`。

#### 78.3.2. `NioEventLoop` 启动

- 服务端启动绑定接口
- 新连接接入，通过 choose 绑定一个 `NioEventLoop`

在 `AbstractBootstrap.doBind0` 方法中，调用 `SingleThreadEventExecutor.execute(java.lang.Runnable)` 开始启动，再调用 `SingleThreadEventExecutor.execute(java.lang.Runnable, boolean)`，最后通过 `SingleThreadEventExecutor.startThread` 方法来启动。实际启动工作，最后委托给了 `SingleThreadEventExecutor.doStartThread` 方法来执行，这个方法中，调用 `SingleThreadEventExecutor.this.run();` 来启动 `NioEventLoop`。

#### 78.3.3. `NioEventLoop` 执行逻辑

1. `run() → for(;;)`
   1. `select()` 检查是否有 I/O 事件
   2. `processSelectedKeys()` 处理 I/O 事件
   3. `SingleThreadEventExecutor.runAllTasks(long)` 处理异步任务队列

**select() 方法**

- deadline 以及任务穿插逻辑处理

  `NioEventLoop.select(long)`

- 阻塞式select

- 避免 JDK 空轮询的 Bug

  在 `NioEventLoop.run()` 方法中，每次轮询，都会记录一下轮询次数 `selectCnt`；在 `NioEventLoop.unexpectedSelectorWakeup(selectCnt)`方法中，如果轮询次数大于 `SELECTOR_AUTO_REBUILD_THRESHOLD`(该值默认是 `512`，可以通过 `io.netty.selectorAutoRebuildThreshold` 参数来改)，则重建。

  重建工作在 `NioEventLoop.rebuildSelector()` 方法中完成，然后又委托给 `NioEventLoop.rebuildSelector0()` 来实际执行。主要工作就是创建一个新的 `selector`，然后把老的 `selector` 上的 `SelectionKey` 注册到新的 `selector` 上。

**`processSelectedKeys()`**

- selected keySet 优化

  `SelectedSelectionKeySet` 底层是一个数组。只实现了增加操作，删除操作没有实现。为什么？

- `processSelectedKeysOptimized()`

  `NioEventLoop.processSelectedKeysOptimized()`，重点在 `NioEventLoop.processSelectedKey(SelectionKey, AbstractNioChannel)` 。

### 78.4. Netty

创建 bossGroup 和 workerGroup . 创建两个线程组 bossGroup 和 workerGroup . bossGroup 只是处理连接请求，真正的客户端业务处理，会交给 workerGroup 完成 . 两个都是无限循环 . bossGroup 和 workerGroup 含有的子线程(NioEventLoop)的个数。默认 `CPU 内核数 * 2`，在 `io.netty.channel.MultithreadEventLoopGroup.DEFAULT_EVENT_LOOP_THREADS` 常量中定义

#### 78.4.1. 异步任务

比如这里我们有一个非常耗时长的业务→ 异步执行 → 提交该 channel 对应的 NIOEventLoop 的 taskQueue 中, 从 ctx → pipeline → eventLoop → taskQueue 可以看到提交的任务。

NettyServer

```JAVA

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Test;




public class NettyServer {
  /**
   * 在 JDK 11 下启动错误： https://stackoverflow.com/a/57892679/951836
   */
  @Test
  public void server() throws InterruptedException {
    // 创建 bossGroup 和 workerGroup
    // 1. 创建两个线程组 bossGroup 和 workerGroup
    // 2. bossGroup 只是处理连接请求，真正的客户端业务处理，会交给 workerGroup 完成
    // 3. 两个都是无限循环
    // 4. bossGroup 和 workerGroup 含有的子线程(NioEventLoop)的个数
    //    默认 CPU 内核数 * 2，在 io.netty.channel.MultithreadEventLoopGroup.DEFAULT_EVENT_LOOP_THREADS 常量中定义
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      // 创建服务器端的启动对象，配置参数
      ServerBootstrap bootstrap = new ServerBootstrap();
      // 使用链式编程进行配置
      bootstrap.group(bossGroup, workerGroup) // 设置两个 线程组
        .channel(NioServerSocketChannel.class) // 使用 NioSocketChannel 作为服务器的通道实现
        .option(ChannelOption.SO_BACKLOG, 128) // 设置线程队列得到连接个数
        .childOption(ChannelOption.SO_KEEPALIVE, true) // 设置保持活动连接状态
        .childHandler(new ChannelInitializer<SocketChannel>() { // 创建一个通道测试对象
          // 给 pipeline 设置处理器
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new NettyServerHandler());
          }
        }); // 给 workerGroup 的 EventLoop 对应的管道设置处理器
      System.out.println("....服务器 is ready...");

      // 绑定一个端口并且同步，生成一个 ChannelFuture 对象
      // 启动服务器（并绑定端口）
      ChannelFuture future = bootstrap.bind(11911).sync();

      // 给 future 注册监听器
      future.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            System.out.println("监听端口 11911 成功");
          } else {
            System.out.println("监听端口 11911 失败");
          }
        }
      });


      // 对关闭通道进行监听
      future.channel().closeFuture().sync();

      // TODO Netty 的异步模型
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

 

NettyServerHandler

```JAVA
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 说明：
 * 我们自定义一个 Handler 需要继承 netty 规定好的某个 HandlerAdapter
 *
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
  /**
   * 读取数据实际（这里我们可以读取客户端发送的消息）
   *
   * @param ctx ChannelHandlerContext 上下文对象，含有管道 pipeline，通道 channel，地址
   * @param msg 客户端发送的数据，默认是 Object
   * @throws Exception
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

//    // 第一：正常情况
//    System.out.println("服务器读取线程 " + Thread.currentThread().getName());
//    System.out.println("server ctx = " + ctx);
//    System.out.println("看看 channel 和 pipeline 的关系");
//    // 从 ctx 可以拿到非常非常多的信息
//    Channel channel = ctx.channel();
//    ChannelPipeline pipeline = ctx.pipeline(); // 本质是一个双向链接
//
//    // 将 msg 转成一个 ByteBuf
//    // ByteBuf 是 Netty 提供的，不是 NIO 提供的 ByteBuffer
//    ByteBuf buf = (ByteBuf) msg;
//    System.out.println("客户端发送消息是：" + buf.toString(UTF_8));
//    System.out.println("客户端地址：" + channel.remoteAddress());


    // 第二种情况：
    // 比如这里我们有一个非常耗时长的业务-> 异步执行 -> 提交该 channel 对应的
    // NIOEventLoop 的 taskQueue 中,
    // 从 ctx -> pipeline -> eventLoop -> taskQueue 可以看到提交的任务
    ctx.channel().eventLoop().execute(() -> {
      try {
        Thread.sleep(20 * 1000);
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵 2", UTF_8));
        System.out.println("channel code=" + ctx.channel().hashCode());
      } catch (Exception ex) {
        System.out.println("发生异常" + ex.getMessage());
      }
    });

    // 定时任务
    ctx.channel().eventLoop().schedule(() -> {
      try {
        Thread.sleep(20 * 1000);
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵 2", UTF_8));
        System.out.println("channel code=" + ctx.channel().hashCode());
      } catch (Exception ex) {
        System.out.println("发生异常" + ex.getMessage());
      }
    }, 10, TimeUnit.SECONDS);

    System.out.println(ctx);
  }

  /**
   * 数据读取完毕
   */
  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    // 将数据写入到缓存，并刷新
    // 一般讲，我们对这个发送的数据进行编码
    ctx.writeAndFlush(Unpooled.copiedBuffer("Hello, D瓜哥~, pong -> O(∩_∩)O哈哈~", UTF_8));
  }

  /**
   * 处理异常，一般需要关闭通道
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
```

 

NettyClient

```JAVA
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;




public class NettyClient {
  public static void main(String[] args) throws InterruptedException {
    // 客户端只需要一个事件循环组即可
    NioEventLoopGroup group = new NioEventLoopGroup();

    try {
      // 创建客户端启动对象
      Bootstrap bootstrap = new Bootstrap();
      // 设置相关参数
      bootstrap.group(group) // 设置线程组
        .channel(NioSocketChannel.class) // 设置客户端通讯通道的实现类
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast(new NettyClientHandler()); // 加入自己的处理器
          }
        });
      System.out.println("....客户端 OK ...");

      // 启动客户端去连接服务器端
      // 关于 ChannelFuture 还要分析，涉及到 Netty 的异步模型
      ChannelFuture future = bootstrap.connect("127.0.0.1", 11911).sync();

      // 给关闭通道进行监听
      future.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}
```



NettyClientHandler

```JAVA
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static java.nio.charset.StandardCharsets.UTF_8;




public class NettyClientHandler extends ChannelInboundHandlerAdapter {
  /**
   * 当通道就绪就会触发该方法
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("client " + ctx);
    ctx.writeAndFlush(Unpooled.copiedBuffer("Hello, D瓜哥，ping -> \\(^o^)/", UTF_8));
  }

  /**
   * 当通道有读取事件时，会触发
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    System.out.println("");
    ByteBuf buf = (ByteBuf) msg;
    System.out.println("服务器回复的消息：" + buf.toString(UTF_8));
    System.out.println("服务器的地址：" + ctx.channel().remoteAddress());
  }
}
```



Netty 的异步模型，就是基于 `ChannelFuture` 和 Listener 的监听回调模型。在入站、出站整个处理链上，可以注册各种各样的 Listener，以事件来驱动它们的调用。

#### 78.4.2. HTTP

`io.netty.handler.codec.http.DefaultHttpRequest` 是 `io.netty.handler.codec.http.HttpObject` 的一个实现类。

为什么刷新一次浏览器，会有两个请求？ 浏览器增加了一次访问 ico 图标的请求。

HTTP 协议用完就关闭，所以，每次 pipeline 都不一样。跟 TCP 不太一样。

TestServer

```JAVA
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Test;




public class TestServer {

  @Test
  public void test() throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new TestServerInitializer());

      ChannelFuture future = bootstrap.bind(11911).sync();

      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```



TestServerInitializer

```JAVA
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;



public class TestServerInitializer extends ChannelInitializer<SocketChannel> {
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    // 向管道加入处理器
    // 得到管道
    ChannelPipeline pipeline = ch.pipeline();
    // 加入一个 Netty 提供的 HttpServerCodec
    // HttpServerCodec 的说明
    // 1. HttpServerCodec 是 Netty 提供的处理 HTTP 的编解码器
    pipeline.addLast("MyHttpServerCodec", new HttpServerCodec());
    // 2. 增加一个自定义的 Handler
    pipeline.addLast("MyTestServerHandler", new TestServerHandler());

    System.out.println(pipeline);
  }
}
```



TestServerHandler

```java
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 说明
 * <p>
 * 1. SimpleChannelInboundHandler 就是 ChannelInboundHandlerAdapter 的子类
 * 2. HttpObject 客户端和服务器端相互同学的数据被封装成 HttpObject。
 *
 */
public class TestServerHandler extends SimpleChannelInboundHandler<HttpObject> {
  /**
   * 读取客户端数据
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
    // 判断 msg 是不是一个 HttpRequest 请求
    if (msg instanceof HttpRequest) {

      System.out.println("ctx 类型 " + ctx.getClass().getName());

      System.out.println("pipeline hashcode=" + ctx.pipeline().hashCode()
        + " TestServerHandler hash=" + this.hashCode());

      System.out.println("msg 类型 " + msg.getClass());
      System.out.println("客户端地址 " + ctx.channel().remoteAddress());

      HttpRequest httpRequest = (HttpRequest) msg;
      URI uri = new URI(httpRequest.uri());
      if ("/favicon.ico".equals(uri.getPath())) {
        System.out.println("请求了 favicon.ico，不做响应");
        return;
      }

      // 回复信息给浏览器(http协议)
      ByteBuf content = Unpooled.copiedBuffer("Hello, D瓜哥。我是服务器", UTF_8);
      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

      // 将构建好的 response 返回
      ctx.writeAndFlush(response);
    }
  }
}
```

 ```JAVA
 ServerBootstrap bootstrap = new ServerBootstrap();
 bootstrap.group(bossGroup, workerGroup)
   .handler(null) // 添加到 bossGroup
   .childHandler(null); // 添加到 workerGroup
 
 ```



核心 API

1. `Bootstrap`
2. `ServerBootstrap`
3. `Channel`
4. `ChannelFuture`
5. `ChannelHandler`
6. `ChannelHandlerContext` — `ChannelHandler` 中包含了一个 `ChannelHandlerContext`。
7. `ChannelInboundHandler` 用于处理入站 I/O 事件
8. `ChannelOutboundHandler` 用于处理出站 I/O 事件
9. `ChannelPipeline` — 一个重点。`ChannelPipeline` 是一个 Handler 的集合，它负责处理和拦截 inbound 或 outbound 的事件和操作，相当于一个贯穿 Netty 的链。

![image-20220123155706863](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123155706863-bce7d4.png)

`ChannelHandler` 是一个很庞大的体系，也是 Netty 中非常核心的知识点。

在 Netty 中，每个 Channel 都有且仅有一个 `ChannelPipeline` 与之对应。

#### 78.4.3. `ChannelHandlerContext`

`ChannelHandlerContext` 和 `ChannelHandler` 属于一对一关系。

#### 78.4.4. `ChannelOption`

1. `ChannelOption.SO_BACKLOG` — 对应 TCP/IP 协议 `listen` 函数中的 `backlog` 参数，用于初始化服务器可连接队列大小。服务端处理客户端连接请求是顺序处理的，所以同一时间只能处理一个客户端连接。多个客户端来的时候，服务端将不能处理的客户端连接请求放在队列中等待处理，`backlog` 参数指定了队列大小。
2. `ChannelOption.SO_KEEPALIVE` — 一直保持连接活动状态。

可以从下面三个方面来学习网络编程

1. 结合 epoll 源码，来说说 Java NIO 的实现。
2. 了解 Linux 网络编程接口
3. Java JDK 中对 Linux 网络编程接口的封装

`Unpooled` 操作缓冲区的。

`HttpObjectAggregator` 这个 Handler 可以实现报文聚合。怎么实现的？

在自定义 Handler 之前，经历过哪些 Handler？

`io.netty.handler.codec.ByteToMessageDecoder.decode` 这个方法会被反复调用，直到确定没有新的元素被添加到list，或者 `ByteBuf` 没有更多的可读字节为止。*这是怎么实现的？*

`io.netty.handler.codec.MessageToByteEncoder.write`

```JAVA
@Override
public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    ByteBuf buf = null;
    try {
        if (acceptOutboundMessage(msg)) {
            @SuppressWarnings("unchecked")
            I cast = (I) msg;
            buf = allocateBuffer(ctx, cast, preferDirect);
            try {
                encode(ctx, cast, buf);
            } finally {
                ReferenceCountUtil.release(cast);
            }

            if (buf.isReadable()) {
                ctx.write(buf, promise);
            } else {
                buf.release();
                ctx.write(Unpooled.EMPTY_BUFFER, promise);
            }
            buf = null;
        } else {
            ctx.write(msg, promise);
        }
    } catch (EncoderException e) {
        throw e;
    } catch (Throwable e) {
        throw new EncoderException(e);
    } finally {
        if (buf != null) {
            buf.release();
        }
    }
}
```



在 `ReplayingDecoder` 不需要判断数据是否足够读取，内部会进行处理判断。它是怎么实现的？

`ReplayingDecoder` 有如下问题：

1. 并不是所有的 `ByteBuf` 操作都支持，如果调用了一个不支持的方法，就会抛出 `UnsupportedOperationException`
2. 在某些情况下可能稍慢于 `ByteToMessageDecoder`，例如网络缓慢并且消息格式复杂时，消息会被拆成多个碎片，速度变慢。

有很多编解码器

1. `LineBasedFrameDecoder`
2. `DelimiterBasedFrameDecoder`
3. `HttpObjectDecoder`
4. `LengthFieldBasedFrameDecoder`

#### 78.4.5. TCP 粘包和拆包

遇到异常就关闭连接，这样做合适吗？工程化的做法应该是怎么样的？如何重连或者重试？

粘包好奇怪，为什么第一次全部接受？后面的确开始不规则接受呢？

解决粘包问题的关键是解决服务器端每次读取数据长度的问题，这个问题解决，就不会出现服务器多读或者少读数据的问题，从而避免 TCP 粘包、拆包的问题。

### 78.5. 源码分析

1. `doBind` 方法
2. `NioEventLoop` 中的 `run`

`io.netty.bootstrap.AbstractBootstrap.doBind` 建立 NIO 和 Netty 之间的联系。

`io.netty.channel.socket.nio.NioServerSocketChannel.doBind` 是一个重要的点。

执行到 `io.netty.channel.AbstractChannel.AbstractUnsafe.safeSetSuccess` 方法，就说明 promise 任务成功了。

`new NioEventLoopGroup()` 如果不指定参数，则默认创建的个数是内核数*2。

`io.netty.bootstrap.AbstractBootstrap.initAndRegister` 是源码分析的一个关键方法。

在 `io.netty.bootstrap.ServerBootstrap.init` 方法中完成了，`Channel` 和 `ChannelPipeline` 的关联。

`ChannelPipeline` 是一个双向链表，但是 `head` 和 `tail` 节点是空节点，添加和删除 Handler 都是在这两个节点之间进行。

调用 `ChannelPipeline` 的 `addLast` 方法增加 `ChannelHandler`，最后是在 `io.netty.channel.DefaultChannelPipeline.addLast(EventExecutorGroup, String, ChannelHandler)` 方法中，将 `ChannelHandler` 封装成了 `ChannelHandlerContext`，然后添加到 `ChannelPipeline` 链上的。

`io.netty.channel.nio.NioEventLoop.run` 方法中封装了事件轮询。

`io.netty.channel.socket.nio.NioServerSocketChannel.doReadMessages` 方法中，把 NIO 的 `SocketChannel` 封装成了 Netty 的 `NioSocketChannel` 对象。

### 78.6. 时间轮

![image-20220123155826229](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/23/image-20220123155826229-6bb715.png)

从图中可以看到此时指针指向的是第一个槽，一共有八个槽 0~7，假设槽的时间单位为 1 秒，现在要加入一个延时 5 秒的任务，计算方式就是 `5 % 8 + 1 = 6`，即放在槽位为 6，下标为 5 的那个槽中。更具体的就是拼到槽的双向链表的尾部。

然后每秒指针顺时针移动一格，这样就扫到了下一格，遍历这格中的双向链表执行任务。然后再循环继续。

可以看到插入任务从计算槽位到插入链表，时间复杂度都是O(1)。那假设现在要加入一个50秒后执行的任务怎么办？这槽好像不够啊？难道要加槽嘛？和HashMap一样扩容？

不是的，常见有两种方式，一种是通过增加轮次的概念。`50 % 8 + 1 = 3`，即应该放在槽位是 3，下标是 2 的位置。然后 `(50 - 1) / 8 = 6`，即轮数记为 6。也就是说当循环 6 轮之后扫到下标的 2 的这个槽位会触发这个任务。Netty 中的 `HashedWheelTimer` 使用的就是这种方式。

### 78.7. 参考资料

1. [面试官：知道时间轮算法吗？在Netty和Kafka中如何应用的？](https://mp.weixin.qq.com/s/xBB72hJGn8geZ7SkM0FqJw)

### 78.8. 自定义 Dubbo

  ```JAVA
  import net.bytebuddy.ByteBuddy;
  import net.bytebuddy.ClassFileVersion;
  import net.bytebuddy.implementation.DefaultMethodCall;
  import net.bytebuddy.implementation.MethodDelegation;
  import net.bytebuddy.implementation.bind.annotation.SuperCall;
  import org.junit.jupiter.api.Test;
  
  import java.io.File;
  import java.io.IOException;
  import java.lang.reflect.Field;
  import java.lang.reflect.Method;
  import java.nio.file.Files;
  import java.nio.file.Path;
  import java.util.Arrays;
  import java.util.Set;
  import java.util.concurrent.Callable;
  import java.util.stream.Collectors;
  
  import static net.bytebuddy.matcher.ElementMatchers.named;
  
  
  
  
  public class ByteBuddyTest {
    @Test
    public void test() {
      System.out.println("--subclass-----------------------");
      saveToFile(subclass(User.class), "gen.Subclass");
      System.out.println("--rebase-----------------------");
      saveToFile(rebase(User.class), "gen.Rebase");
      System.out.println("--redefine-----------------------");
      saveToFile(redefine(User.class), "gen.Redefine");
    }
  
    public void saveToFile(byte[] bytes, String name) {
      int i = name.lastIndexOf('.');
      Path dir = Path.of(name.substring(0, i).replace('.', File.separatorChar));
      try {
        Files.createDirectories(dir);
        Path path = dir.resolve(name.substring(i + 1, name.length()) + ".class");
        Files.write(path, bytes);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  
    public static class LoggerInterceptor {
      public static String log(@SuperCall Callable<String> zuper) throws Exception {
        System.out.println("Calling database");
        try {
          return zuper.call();
        } finally {
          System.out.println("Returned from database");
        }
      }
    }
  
    public byte[] subclass(Class<?> clazz) {
      return new ByteBuddy()
        .subclass(clazz)
  //      .method(named("getName")).intercept(FixedValue.value("Hello World!"))
        .method(named("getInfo")).intercept(MethodDelegation.to(LoggerInterceptor.class))
        .make()
        .getBytes();
    }
  
    public byte[] redefine(Class<?> clazz) {
      return new ByteBuddy()
        .redefine(clazz)
  //      .method(named("getName")).intercept(FixedValue.value("Hello World!"))
        .method(named("getInfo")).intercept(MethodDelegation.to(LoggerInterceptor.class))
        .make()
        .getBytes();
    }
  
    public byte[] rebase(Class<?> clazz) {
      return new ByteBuddy()
        .rebase(clazz)
  //      .method(named("getName")).intercept(FixedValue.value("Hello World!"))
        .method(named("getInfo")).intercept(MethodDelegation.to(LoggerInterceptor.class))
        .make()
        .getBytes();
    }
  
    public static class User {
      private int age;
      private String name;
  
      public int getAge() {
        return age;
      }
  
      public void setAge(int age) {
        this.age = age;
      }
  
      public String getName() {
        return name;
      }
  
      public void setName(String name) {
        this.name = name;
      }
  
      public String getInfo(String info) {
        return String.format("%s-%d-%s", getName(), getAge(), info);
      }
    }
  
  
    public void xray(Class<?> clazz) {
      System.out.println("className = " + clazz.getName());
      Field[] fields = clazz.getFields();
      System.out.println("fields: ");
      for (Field field : fields) {
        System.out.printf(" %s %s%n", field.getDeclaringClass().getName(), field.getName());
      }
      Method[] methods = clazz.getMethods();
      System.out.println("methodName: ");
      Method[] baseMethods = Object.class.getMethods();
      Set<String> methodNames = Arrays.stream(baseMethods)
        .map(Method::getName)
        .collect(Collectors.toSet());
      for (Method method : methods) {
        if (methodNames.contains(method.getName())) {
          continue;
        }
        Class<?>[] classes = method.getParameterTypes();
        String params = Arrays.stream(classes)
          .map(Class::getName)
          .collect(Collectors.joining(", "));
        System.out.printf("  %s(%s)%n", method.getName(), params);
      }
    }
  
    public static interface First {
      default String qux() {
        return "FOO";
      }
    }
  
    public static interface Second {
      default String qux() {
        return "BAR";
      }
    }
  
    @Test
    public void test2() {
      byte[] bytes = new ByteBuddy(ClassFileVersion.JAVA_V11)
        .subclass(Object.class)
        .implement(First.class)
        .implement(Second.class)
        .method(named("qux")).intercept(DefaultMethodCall.prioritize(First.class))
        .make()
        .getBytes();
      saveToFile(bytes, "gen.NewClass");
    }
  
  }
  ```

```JAVA
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;



public class NettyByteBuf {
  @Test
  public void test01() {
    // 1. 创建对象，对象包含一个 byte[10] 的数组
    // 2. 在 Netty 的 ByteBuf 中，不需要使用 flip 进行反转
    //    底层维护了 readIndex 和 writeIndex
    // 3. 通过 readIndex、 writeIndex 和 capacity，将 buffer 分成三个区域
    //    3.1 0 -- readIndex 已读
    //    3.2 readIndex - writeIndex 可读
    //    3.3 writeIndex - capacity 可写
    ByteBuf buffer = Unpooled.buffer(10);
    for (int i = 0; i < 10; i++) {
      buffer.writeByte(i);
    }

    System.out.println("capacity=" + buffer.capacity());
    // 输出
    for (int i = 0; i < buffer.capacity(); i++) {
      System.out.println(buffer.getByte(i));
    }

    for (int i = 0; i < buffer.capacity(); i++) {
      System.out.println(buffer.readByte());
    }
  }

  @Test
  public void test02() {
    // 创建 ByteBuf
    ByteBuf byteBuf = Unpooled.copiedBuffer("Hello, D瓜哥！", UTF_8);

    // 使用相关方法
    if (byteBuf.hasArray()) {
      byte[] content = byteBuf.array();
      // 将 content 转成字符串
      System.out.println(new String(content, UTF_8));

      System.out.println("byteBuf=" + byteBuf);
      System.out.println(byteBuf.arrayOffset());
      System.out.println(byteBuf.readerIndex());
      System.out.println(byteBuf.writerIndex());
      System.out.println(byteBuf.capacity());
      System.out.println(byteBuf.readByte());

      int len = byteBuf.readableBytes(); // 可读取的字符数
      System.out.println(len);

      for (int i = 0; i < len; i++) {
        System.out.println((char) byteBuf.getByte(i));
      }

      // 按照范围读取
      System.out.println(byteBuf.getCharSequence(0, 4, UTF_8));
    }
  }


}
```



```JAVA
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.junit.jupiter.api.Test;

import java.util.Scanner;


public class GroupChatClient {
  // 属性
  private final String host;
  private final int port;

  public GroupChatClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void run() throws InterruptedException {
    NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(eventExecutors)
        .channel(NioSocketChannel.class)
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new StringDecoder());
            pipeline.addLast("encoder", new StringEncoder());
            // 加入自定义的 handler TODO
            pipeline.addLast(new GroupChatClientHandler());
          }
        });
      ChannelFuture future = bootstrap.connect(host, port).sync();
      Channel channel = future.channel();
      System.out.println("-----" + channel.localAddress() + "------");
      // 客户端需要输入信息，创建一个扫描器
      // TODO 还不能输入，调试一下，看看怎么回事？
      Scanner scanner = new Scanner(System.in);
      while (scanner.hasNextLine()) {
        String msg = scanner.nextLine();
        channel.writeAndFlush(msg + "\r\n");
      }
    } finally {
      eventExecutors.shutdownGracefully();
    }
  }

  public static class Clients {
    @Test
    public void client1() throws InterruptedException {
      new GroupChatClient("127.0.0.1", 11911).run();
    }

    @Test
    public void client2() throws InterruptedException {
      new GroupChatClient("127.0.0.1", 11911).run();
    }

    @Test
    public void client3() throws InterruptedException {
      new GroupChatClient("127.0.0.1", 11911).run();
    }
  }
}
```



```JAVA
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;



public class GroupChatClientHandler extends SimpleChannelInboundHandler<String> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    System.out.println(msg);
  }
}
```



```JAVA
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;



public class GroupChatServer {
  private int port;

  public GroupChatServer(int port) {
    this.port = port;
  }

  // 处理客户端请求
  public void run() throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 128)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            // 获取 pipeline
            ChannelPipeline pipeline = ch.pipeline();
            // 向 pipeline 加入解码器
            pipeline.addLast("decoder", new StringDecoder());
            // 向 pipeline 加入编码器
            pipeline.addLast("encoder", new StringEncoder());
            // 加入自己的业务处理 handler TODO
            pipeline.addLast(new GroupChatServerHandler());
          }
        });
      System.out.println("Netty 服务器启动");
      ChannelFuture future = bootstrap.bind(port).sync();
      // 关闭监听
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  public static void main(String[] args) throws InterruptedException {
    new GroupChatServer(11911).run();
  }
}
```

```JAVA
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.time.LocalDateTime;



public class GroupChatServerHandler extends SimpleChannelInboundHandler<String> {
  // 定义一个 channel 组，管理所有的 channel
  // GlobalEventExecutor.INSTANCE 是全局的事件执行器，是一个单例
  private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

  /**
   * 表示连接建立，一旦建立，第一个执行
   * <p>
   * 将当前 channel 加入到 channelGroup
   */
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    /**
     * 将该客户加入聊天的信息推送给其他在线的客户端
     *
     * 该方法会将 channelGroup 中所有的 channel 遍历，并发送消息。
     *
     * 我们不需要自己遍历。
     */
    channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 加入群聊 at" + LocalDateTime.now() + " \n");
    channelGroup.add(channel);

    super.handlerAdded(ctx);
  }

  /**
   * 表示 channel 处于活动状态，表示 xx 上线
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println(ctx.channel().remoteAddress() + " 上线了~");
  }

  /**
   * 表示 channel 处于不活动状态，表示 xx 离线
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    System.out.println(channel.remoteAddress() + " 下线了");
  }

  /**
   * 断开连接，将 xx客户离开信息推送给当前在线客户
   */
  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();
    channelGroup.writeAndFlush("[客户端]" + channel.remoteAddress() + " 离开了\n");
    // 触发这个方法后，不需要手动删除，Netty 会自动删除的
    //  channelGroup.remove(channel);
    System.out.println("channelGroup size=" + channelGroup.size());
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
    // 获取当前 channel
    Channel channel = ctx.channel();
    // 这时我们遍历 channelGroup，根据不同的情况，回送不同的消息
    channelGroup.forEach(ch -> {
      if (channel != ch) {
        ch.writeAndFlush("[客户]" + channel.remoteAddress() + " 发送消息：" + msg + "\n");

      } else {
        ch.writeAndFlush("[自己]发送了消息：" + msg + "\n");
      }
    });

  }
}
```

```JAVA
import com.diguage.truman.netty.dubbo.interfaces.HelloService;
import com.diguage.truman.netty.dubbo.netty.NettyClient;



public class DubboClientBootstrap {
  // 这里定义协议头
  public static final String providerName = "#Hello#";

  public static void main(String[] args) throws InterruptedException {
    NettyClient consumer = new NettyClient();
    HelloService helloService = (HelloService) consumer.getBean(HelloService.class, providerName);
    // 通过代理对象调用服务提供者的方法
    for (int i = 0; i < 100; i++) {
      Thread.sleep(1000);
      String result = helloService.hello("您好啊，Dubbo~~~" + i);
      System.out.println("调用结果 res=" + result);
    }
  }
}
```

```JAVA
public interface HelloService {
  String hello(String msg);
}

```

```JAVA
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class NettyClient {
  // 创建线程池
  private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  private static NettyClientHandler client;

  // 使用 代理模式，创建代理对象
  public Object getBean(final Class<?> serviceService, final String providerName) {
    return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
      new Class<?>[]{serviceService}, (proxy, method, args) -> {
        if (client == null) {
          initClient();
        }
        // 设置要发送给服务器的消息
        // providerName 协议头，args[0] 就是客户端调用 API hello(msg) 时，传的参数
        client.setParam(providerName + args[0]);
        return executor.submit(client).get();
      });
  }

  private static void initClient() {
    client = new NettyClientHandler();
    NioEventLoopGroup group = new NioEventLoopGroup();
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.group(group)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.TCP_NODELAY, true)
      .handler(new ChannelInitializer<SocketChannel>() {
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
          ChannelPipeline pipeline = ch.pipeline();
          pipeline.addLast(new StringDecoder());
          pipeline.addLast(new StringEncoder());
          pipeline.addLast(client);
        }
      });
    try {
      bootstrap.connect("127.0.0.1", 11911).sync();
      System.out.println("客户端建立链接……");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
```

```JAVA
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Callable;



public class NettyClientHandler extends ChannelInboundHandlerAdapter implements Callable {

  private ChannelHandlerContext context;
  private String result;
  private String param;

  /**
   * 与服务器的连接创建后，就会被调用
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.context = ctx;
  }

  /**
   * 收到服务器的数据后，调用方法
   */
  @Override
  public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    this.result = msg.toString();
    notify(); // 唤醒等待的线程
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }

  /**
   * 被代理对象调用，发送数据给服务器，-> wait -> 等待被唤醒(channel read) -> 返回结果
   */
  @Override
  public synchronized Object call() throws Exception {
    context.writeAndFlush(param);
    // 进行 wait
    wait(); // 等待 channel read 方法获取的到服务器的结果后，唤醒
    return result;
  }

  public void setParam(String param) {
    this.param = param;
  }
}
```

```JAVA
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;



public class NettyServer {
  /**
   * 完成对 NettyServer 的初始化和启动
   */
  public static void startServer(String hostname, int port) {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup(8);
    try {
      ServerBootstrap bootstrap = new ServerBootstrap();
      bootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 128)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new StringDecoder());
            pipeline.addLast(new StringEncoder());
            pipeline.addLast(new NettyServerHandler());
          }
        });
      ChannelFuture future = bootstrap.bind(hostname, port).sync();
      System.out.println("服务器启动成功，服务端开始提供服务");
      future.channel().closeFuture().sync();
    } catch (Exception e) {

    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

```JAVA
import com.diguage.truman.netty.dubbo.provider.HelloServiceImpl;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;



public class NettyServerHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // 获取客户端发送的消息，并调用服务
    System.out.println("msg=" + msg);
    // 客户端在调用服务器的 API 时，需要定义一个协议
    // 比如每次发消息时，都必须以某个字符串开头 "#Hello#"
    String prefix = "#Hello#";
    if (msg.toString().startsWith(prefix)) {
      String result = new HelloServiceImpl().hello(msg.toString().substring(prefix.length()));
      ctx.writeAndFlush(result);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
```

```JAVA
import com.diguage.truman.netty.dubbo.netty.NettyServer;



public class DubboServerBootstrap {
  public static void main(String[] args) {
    NettyServer.startServer("127.0.0.1", 11911);
  }
}
```

```JAVA
import com.diguage.truman.netty.dubbo.interfaces.HelloService;



public class HelloServiceImpl implements HelloService {

  private static int count = 0;

  @Override
  public String hello(String msg) {
    System.out.println("接收到客户端消息=" + msg);
    if (msg != null) {
      return "您好，哥！我接收到了你的消息[" + msg + "] 第" + (++this.count) + "次";
    } else {
      return "对不起！没有收到你的消息！";
    }
  }
}
```

```JAVA
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;



public class MyServer {
  public static void main(String[] args) throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO)) // 在 bossGroup 增加一个日志处理器
        .childHandler(new ChannelInitializer<SocketChannel>() {
          /**
           * 加入一个 Netty 提供的 IdleStateHandler
           *
           * 说明：
           *
           * 1. IdleStateHandler 是 Netty 提供的处理空闲状态的处理器
           * 2. long readerIdleTime 表示多长时间没有读取，就会发送一个心跳检查包，检查是否是连接状态
           * 3. long writerIdleTime 表示多长时间没有写，就会发送一个心跳检查包，检查是否是连接状态
           * 4. long allIdleTime 表示多长时间没有读写，就会发送一个心跳检查包，检查是否是连接状态
           *
           * 当 IdleStateHandler 触发后，就会传递给管道的下一个 handler 去处理
           * 通过调用（触发）下一个 handler 的 userEventTriggered，在该方法中处理 IdleStateEvent(读空闲，写空闲，读写空闲)
           */
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            // 调整参数，显示不同的事件
            pipeline.addLast(new IdleStateHandler(13, 15, 7, TimeUnit.SECONDS));
            // 加入一个对空闲检测进一步处理的 handler
            pipeline.addLast(new MyServerHandler());
          }
        });

      ChannelFuture future = serverBootstrap.bind(11911).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

```JAVA

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;



public class MyServerHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      String eventType = null;
      switch (event.state()) {
        case READER_IDLE:
          eventType = "读空闲";
          break;
        case WRITER_IDLE:
          eventType = "写空闲";
          break;
        case ALL_IDLE:
          eventType = "读写空闲";
          break;
      }
      System.out.println(ctx.channel().remoteAddress() + " --超时时间--" + eventType);
      System.out.println("服务器做相应处理");
      //比如：如果发生空闲，我们关闭通道
      ctx.channel().close();
    }
  }
}
```

```JAVA

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;



public class ByteToLongDecoder extends ByteToMessageDecoder {
  /**
   * decode 会根据接收的数据，被调用多次，直到确定没有新的元素被添加到 list 或者 `ByteBuf` 没有更多的可读字节为止。
   * <p>
   * 如果 list out 不为空，就会将 List 的内容传递给下一个 ChannelInboundHandler 处理，该处理器方法也会被调用多次。
   *
   * @param ctx 上下文对象
   * @param in  入站的 ByteBuf
   * @param out List 集合，将解码后的数据传给下一个 handler
   * @throws Exception
   */
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    System.out.println("ByteToLongDecoder decode 被调用");
    if (in.readableBytes() >= 8) {
      out.add(in.readLong());
    }
  }
}
```

```JAVA
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;



public class ByteToLongDecoder2 extends ReplayingDecoder<Void> {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    // 在 ReplayingDecoder 不需要判断数据是否足够读取，内部会进行处理判断。
    out.add(in.readLong());
  }
}
```



```JAVA

import com.diguage.truman.netty.protobuf.ClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;



public class IoClient {
  public static void main(String[] args) throws InterruptedException {
    // 客户端只需要一个事件循环组即可
    NioEventLoopGroup group = new NioEventLoopGroup();

    try {
      // 创建客户端启动对象
      Bootstrap bootstrap = new Bootstrap();
      // 设置相关参数
      bootstrap.group(group) // 设置线程组
        .channel(NioSocketChannel.class) // 设置客户端通讯通道的实现类
        .handler(new IoClientInitializer());
      System.out.println("....客户端 OK ...");

      // 启动客户端去连接服务器端
      // 关于 ChannelFuture 还要分析，涉及到 Netty 的异步模型
      ChannelFuture future = bootstrap.connect("127.0.0.1", 11911).sync();

      // 给关闭通道进行监听
      future.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}
```



```JAVA
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static java.nio.charset.StandardCharsets.UTF_8;




public class IoClientHandler extends SimpleChannelInboundHandler<Long> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Long msg) throws Exception {
    System.out.println("从服务器接受消息， msg=" + msg);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("IoClientHandler 发送数据");
    //  ctx.writeAndFlush(119L); // 发送一个 long
    // 注意：这个实验数据！！！
    // 分析
    // 1. "abcdabcdabcdabcd" 是 16 个字节
    // 2. 该处理器的前一个 handler 是 LongToByteEncoder
    // 3. LongToByteEncoder 父类是 MessageToByteEncoder
    // 4. 父类的 write 调用 acceptOutboundMessage 方法来检查是不是可以接受的数据，
    //    是则执行子类 encode 的方法，否则直接传递到下一个 handler
    // 因此，在编写 Enoder 时，要注意传入的数据类型和处理的数据类型一致。否则就跳过由下一个handler处理了。
    ctx.writeAndFlush(Unpooled.copiedBuffer("abcdabcdabcdabcd", UTF_8));
  }
}
```

```JAVA

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;



public class IoClientInitializer extends ChannelInitializer<SocketChannel> {
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    // 加入一个出站的 handler，对数据进行一个编码
    pipeline.addLast(new LongToByteEncoder());
    // 入站解码器
    // pipeline.addLast(new ByteToLongDecoder());
    pipeline.addLast(new ByteToLongDecoder2());
    pipeline.addLast(new IoClientHandler());
  }
}
```

```JAVA
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;



public class IoServer {
  public static void main(String[] args) throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO)) // 在 bossGroup 增加一个日志处理器
        .childHandler(new IoServerInitializer());

      ChannelFuture future = serverBootstrap.bind(11911).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

```JAVA

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;



public class IoServerHandler extends SimpleChannelInboundHandler<Long> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Long msg) throws Exception {
    System.out.println("IoServerHandler 被调用");
    System.out.println("从客户的" + ctx.channel().remoteAddress() + " 读取long=" + msg);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.writeAndFlush(120L);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.channel().close();
  }
}
```

```JAVA

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;



public class IoServerInitializer extends ChannelInitializer<SocketChannel> {
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    // 入站的 handler 进行编码
    // pipeline.addLast(new ByteToLongDecoder());
    pipeline.addLast(new ByteToLongDecoder2());
    // 出站编码器
    pipeline.addLast(new LongToByteEncoder());
    pipeline.addLast(new IoServerHandler());
    System.out.println(ch);
  }
}

```



```JAVA
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;



public class LongToByteEncoder extends MessageToByteEncoder<Long> {
  @Override
  protected void encode(ChannelHandlerContext ctx, Long msg, ByteBuf out) throws Exception {
    System.out.println("LongToByteEncoder encode 被调用");
    System.out.println("msg=" + msg);
    out.writeLong(msg);
  }
}
```



```JAVA

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static java.nio.charset.StandardCharsets.UTF_8;



public class ClientHandler extends ChannelInboundHandlerAdapter {
  /**
   * 当通道就绪就会触发该方法
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("client " + ctx);

    StudentPOJO.Student student = StudentPOJO.Student
      .newBuilder()
      .setId(119)
      .setName("D瓜哥")
      .build();

    ctx.writeAndFlush(student);
  }

  /**
   * 当通道有读取事件时，会触发
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    System.out.println("");
    ByteBuf buf = (ByteBuf) msg;
    System.out.println("服务器回复的消息：" + buf.toString(UTF_8));
    System.out.println("服务器的地址：" + ctx.channel().remoteAddress());
  }
}
```



```JAVA
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;




public class ProtobufClient {
  public static void main(String[] args) throws InterruptedException {
    // 客户端只需要一个事件循环组即可
    NioEventLoopGroup group = new NioEventLoopGroup();

    try {
      // 创建客户端启动对象
      Bootstrap bootstrap = new Bootstrap();
      // 设置相关参数
      bootstrap.group(group) // 设置线程组
        .channel(NioSocketChannel.class) // 设置客户端通讯通道的实现类
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            // 加入 protobuf handler
            pipeline.addLast("encoder", new ProtobufEncoder());
            pipeline.addLast(new ClientHandler()); // 加入自己的处理器
          }
        });
      System.out.println("....客户端 OK ...");

      // 启动客户端去连接服务器端
      // 关于 ChannelFuture 还要分析，涉及到 Netty 的异步模型
      ChannelFuture future = bootstrap.connect("127.0.0.1", 11911).sync();

      // 给关闭通道进行监听
      future.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}
```



```JAVA
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;



public class ProtobufServer {
  public static void main(String[] args) throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO)) // 在 bossGroup 增加一个日志处理器
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new ProtobufDecoder(StudentPOJO.Student.getDefaultInstance()));
            pipeline.addLast(new ServerHandler());
          }
        });

      ChannelFuture future = serverBootstrap.bind(11911).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```



```JAVA
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 说明：
 * 我们自定义一个 Handler 需要继承 netty 规定好的某个 HandlerAdapter
 *
 */
public class ServerHandler extends SimpleChannelInboundHandler<StudentPOJO.Student> {
  /**
   * 读取数据实际（这里我们可以读取客户端发送的消息）
   */
  @Override
  public void channelRead0(ChannelHandlerContext ctx, StudentPOJO.Student student) throws Exception {
    System.out.println("客户端发送的数据 id=" + student.getId() + "，name=" + student.getName());
  }

  /**
   * 数据读取完毕
   */
  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    // 将数据写入到缓存，并刷新
    // 一般讲，我们对这个发送的数据进行编码
    ctx.writeAndFlush(Unpooled.copiedBuffer("Hello, D瓜哥~, pong -> O(∩_∩)O哈哈~", UTF_8));
  }

  /**
   * 处理异常，一般需要关闭通道
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
```

```JAVA
syntax = "proto3"; // 版本号

option java_outer_classname = "StudentPOJO"; // 生成的外部类名，同时也是文件名

// protobuf 使用 message 管理数据
// 会在 StudentPOJO 外部类生成一个内部类 Student，
message Student {
    int32 id = 1; // 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
    string name = 2;
}

// 在这个文件所在目录执行如下命令，生成Java类：
// protoc --java_out=. Student.proto
// WARNING: 需要给生成的类，加上 package 名。
```



```JAVA
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: Student.proto
package com.diguage.truman.netty.protobuf;

public final class StudentPOJO {
  private StudentPOJO() {
  }

  public static void registerAllExtensions(
    com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
    com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
      (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public interface StudentOrBuilder extends
    // @@protoc_insertion_point(interface_extends:Student)
    com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
     * </pre>
     *
     * <code>int32 id = 1;</code>
     *
     * @return The id.
     */
    int getId();

    /**
     * <code>string name = 2;</code>
     *
     * @return The name.
     */
    java.lang.String getName();

    /**
     * <code>string name = 2;</code>
     *
     * @return The bytes for name.
     */
    com.google.protobuf.ByteString
    getNameBytes();
  }

  /**
   * <pre>
   * protobuf 使用 message 管理数据
   * 会在 StudentPOJO 外部类生成一个内部类 Student，
   * </pre>
   * <p>
   * Protobuf type {@code Student}
   */
  public static final class Student extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:Student)
    StudentOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use Student.newBuilder() to construct.
    private Student(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private Student() {
      name_ = "";
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
      return new Student();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    private Student(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              id_ = input.readInt32();
              break;
            }
            case 18: {
              java.lang.String s = input.readStringRequireUtf8();

              name_ = s;
              break;
            }
            default: {
              if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return StudentPOJO.internal_static_Student_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
      return StudentPOJO.internal_static_Student_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
          StudentPOJO.Student.class, StudentPOJO.Student.Builder.class);
    }

    public static final int ID_FIELD_NUMBER = 1;
    private int id_;

    /**
     * <pre>
     * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
     * </pre>
     *
     * <code>int32 id = 1;</code>
     *
     * @return The id.
     */
    @java.lang.Override
    public int getId() {
      return id_;
    }

    public static final int NAME_FIELD_NUMBER = 2;
    private volatile java.lang.Object name_;

    /**
     * <code>string name = 2;</code>
     *
     * @return The name.
     */
    @java.lang.Override
    public java.lang.String getName() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs =
          (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        name_ = s;
        return s;
      }
    }

    /**
     * <code>string name = 2;</code>
     *
     * @return The bytes for name.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
    getNameBytes() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8(
            (java.lang.String) ref);
        name_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;

    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
      throws java.io.IOException {
      if (id_ != 0) {
        output.writeInt32(1, id_);
      }
      if (!getNameBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, name_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (id_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(1, id_);
      }
      if (!getNameBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, name_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof StudentPOJO.Student)) {
        return super.equals(obj);
      }
      StudentPOJO.Student other = (StudentPOJO.Student) obj;

      if (getId()
        != other.getId()) return false;
      if (!getName()
        .equals(other.getName())) return false;
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + ID_FIELD_NUMBER;
      hash = (53 * hash) + getId();
      hash = (37 * hash) + NAME_FIELD_NUMBER;
      hash = (53 * hash) + getName().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static StudentPOJO.Student parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static StudentPOJO.Student parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static StudentPOJO.Student parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static StudentPOJO.Student parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static StudentPOJO.Student parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static StudentPOJO.Student parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static StudentPOJO.Student parseFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static StudentPOJO.Student parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static StudentPOJO.Student parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
    }

    public static StudentPOJO.Student parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static StudentPOJO.Student parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static StudentPOJO.Student parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(StudentPOJO.Student prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * <pre>
     * protobuf 使用 message 管理数据
     * 会在 StudentPOJO 外部类生成一个内部类 Student，
     * </pre>
     * <p>
     * Protobuf type {@code Student}
     */
    public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:Student)
      StudentPOJO.StudentOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return StudentPOJO.internal_static_Student_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
        return StudentPOJO.internal_static_Student_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
            StudentPOJO.Student.class, StudentPOJO.Student.Builder.class);
      }

      // Construct using StudentPOJO.Student.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
          .alwaysUseFieldBuilders) {
        }
      }

      @java.lang.Override
      public Builder clear() {
        super.clear();
        id_ = 0;

        name_ = "";

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return StudentPOJO.internal_static_Student_descriptor;
      }

      @java.lang.Override
      public StudentPOJO.Student getDefaultInstanceForType() {
        return StudentPOJO.Student.getDefaultInstance();
      }

      @java.lang.Override
      public StudentPOJO.Student build() {
        StudentPOJO.Student result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public StudentPOJO.Student buildPartial() {
        StudentPOJO.Student result = new StudentPOJO.Student(this);
        result.id_ = id_;
        result.name_ = name_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }

      @java.lang.Override
      public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.setField(field, value);
      }

      @java.lang.Override
      public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @java.lang.Override
      public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @java.lang.Override
      public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @java.lang.Override
      public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof StudentPOJO.Student) {
          return mergeFrom((StudentPOJO.Student) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(StudentPOJO.Student other) {
        if (other == StudentPOJO.Student.getDefaultInstance()) return this;
        if (other.getId() != 0) {
          setId(other.getId());
        }
        if (!other.getName().isEmpty()) {
          name_ = other.name_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
        StudentPOJO.Student parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (StudentPOJO.Student) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int id_;

      /**
       * <pre>
       * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
       * </pre>
       *
       * <code>int32 id = 1;</code>
       *
       * @return The id.
       */
      @java.lang.Override
      public int getId() {
        return id_;
      }

      /**
       * <pre>
       * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
       * </pre>
       *
       * <code>int32 id = 1;</code>
       *
       * @param value The id to set.
       * @return This builder for chaining.
       */
      public Builder setId(int value) {

        id_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
       * </pre>
       *
       * <code>int32 id = 1;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearId() {

        id_ = 0;
        onChanged();
        return this;
      }

      private java.lang.Object name_ = "";

      /**
       * <code>string name = 2;</code>
       *
       * @return The name.
       */
      public java.lang.String getName() {
        java.lang.Object ref = name_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          name_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }

      /**
       * <code>string name = 2;</code>
       *
       * @return The bytes for name.
       */
      public com.google.protobuf.ByteString
      getNameBytes() {
        java.lang.Object ref = name_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
          name_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>string name = 2;</code>
       *
       * @param value The name to set.
       * @return This builder for chaining.
       */
      public Builder setName(
        java.lang.String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        name_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string name = 2;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearName() {

        name_ = getDefaultInstance().getName();
        onChanged();
        return this;
      }

      /**
       * <code>string name = 2;</code>
       *
       * @param value The bytes for name to set.
       * @return This builder for chaining.
       */
      public Builder setNameBytes(
        com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        name_ = value;
        onChanged();
        return this;
      }

      @java.lang.Override
      public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:Student)
    }

    // @@protoc_insertion_point(class_scope:Student)
    private static final StudentPOJO.Student DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new StudentPOJO.Student();
    }

    public static StudentPOJO.Student getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Student>
      PARSER = new com.google.protobuf.AbstractParser<Student>() {
      @java.lang.Override
      public Student parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new Student(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<Student> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Student> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public StudentPOJO.Student getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Student_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internal_static_Student_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
  getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor
    descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n\rStudent.proto\"#\n\007Student\022\n\n\002id\030\001 \001(\005\022\014" +
        "\n\004name\030\002 \001(\tB\rB\013StudentPOJOb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[]{
        });
    internal_static_Student_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_Student_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
      internal_static_Student_descriptor,
      new java.lang.String[]{"Id", "Name",});
  }

  // @@protoc_insertion_point(outer_class_scope)
}
```

```JAVA
package com.diguage.truman.netty.protobuf2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;




public class ClientHandler extends ChannelInboundHandlerAdapter {
  /**
   * 当通道就绪就会触发该方法
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    System.out.println("client " + ctx);

    // 随机发送 Student 或 Worker
    int random = new Random().nextInt(3);
    MyDataInfo.MyMessage message = null;
    if (0 == random) {
      // 发送学生
      message = MyDataInfo.MyMessage.newBuilder()
        .setDataType(MyDataInfo.MyMessage.DataType.StudentType)
        .setStudent(
          MyDataInfo.Student
            .newBuilder()
            .setId(119)
            .setName("瓜哥")
            .build())
        .build();
    } else {
      // 发送 worker
      message = MyDataInfo.MyMessage.newBuilder()
        .setDataType(MyDataInfo.MyMessage.DataType.WorkerType)
        .setWorker(
          MyDataInfo.Worker
            .newBuilder()
            .setName("瓜哥")
            .setAge(119)
            .build())
        .build();
    }

    ctx.writeAndFlush(message);
  }

  /**
   * 当通道有读取事件时，会触发
   */
  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    System.out.println("");
    ByteBuf buf = (ByteBuf) msg;
    System.out.println("服务器回复的消息：" + buf.toString(UTF_8));
    System.out.println("服务器的地址：" + ctx.channel().remoteAddress());
  }
}
```



```JAVA
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: Student.proto

package com.diguage.truman.netty.protobuf2;

public final class MyDataInfo {
  private MyDataInfo() {
  }

  public static void registerAllExtensions(
    com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
    com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
      (com.google.protobuf.ExtensionRegistryLite) registry);
  }

  public interface MyMessageOrBuilder extends
    // @@protoc_insertion_point(interface_extends:MyMessage)
    com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * 用 data_type 来标识传的是哪一个枚举类型？
     * </pre>
     *
     * <code>.MyMessage.DataType data_type = 1;</code>
     *
     * @return The enum numeric value on the wire for dataType.
     */
    int getDataTypeValue();

    /**
     * <pre>
     * 用 data_type 来标识传的是哪一个枚举类型？
     * </pre>
     *
     * <code>.MyMessage.DataType data_type = 1;</code>
     *
     * @return The dataType.
     */
    com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType getDataType();

    /**
     * <code>.Student student = 2;</code>
     *
     * @return Whether the student field is set.
     */
    boolean hasStudent();

    /**
     * <code>.Student student = 2;</code>
     *
     * @return The student.
     */
    com.diguage.truman.netty.protobuf2.MyDataInfo.Student getStudent();

    /**
     * <code>.Student student = 2;</code>
     */
    com.diguage.truman.netty.protobuf2.MyDataInfo.StudentOrBuilder getStudentOrBuilder();

    /**
     * <code>.Worker worker = 3;</code>
     *
     * @return Whether the worker field is set.
     */
    boolean hasWorker();

    /**
     * <code>.Worker worker = 3;</code>
     *
     * @return The worker.
     */
    com.diguage.truman.netty.protobuf2.MyDataInfo.Worker getWorker();

    /**
     * <code>.Worker worker = 3;</code>
     */
    com.diguage.truman.netty.protobuf2.MyDataInfo.WorkerOrBuilder getWorkerOrBuilder();

    public com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataBodyCase getDataBodyCase();
  }

  /**
   * Protobuf type {@code MyMessage}
   */
  public static final class MyMessage extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:MyMessage)
    MyMessageOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use MyMessage.newBuilder() to construct.
    private MyMessage(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private MyMessage() {
      dataType_ = 0;
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
      return new MyMessage();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    private MyMessage(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {
              int rawValue = input.readEnum();

              dataType_ = rawValue;
              break;
            }
            case 18: {
              com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder subBuilder = null;
              if (dataBodyCase_ == 2) {
                subBuilder = ((com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_).toBuilder();
              }
              dataBody_ =
                input.readMessage(com.diguage.truman.netty.protobuf2.MyDataInfo.Student.parser(), extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom((com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_);
                dataBody_ = subBuilder.buildPartial();
              }
              dataBodyCase_ = 2;
              break;
            }
            case 26: {
              com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder subBuilder = null;
              if (dataBodyCase_ == 3) {
                subBuilder = ((com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_).toBuilder();
              }
              dataBody_ =
                input.readMessage(com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.parser(), extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom((com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_);
                dataBody_ = subBuilder.buildPartial();
              }
              dataBodyCase_ = 3;
              break;
            }
            default: {
              if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_MyMessage_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
      return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_MyMessage_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
          com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.class, com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.Builder.class);
    }

    /**
     * <pre>
     * 定义一个枚举
     * </pre>
     * <p>
     * Protobuf enum {@code MyMessage.DataType}
     */
    public enum DataType
      implements com.google.protobuf.ProtocolMessageEnum {
      /**
       * <pre>
       * 在 proto3 中，要求 enum 的编号从 0 开始
       * </pre>
       *
       * <code>StudentType = 0;</code>
       */
      StudentType(0),
      /**
       * <code>WorkerType = 1;</code>
       */
      WorkerType(1),
      UNRECOGNIZED(-1),
      ;

      /**
       * <pre>
       * 在 proto3 中，要求 enum 的编号从 0 开始
       * </pre>
       *
       * <code>StudentType = 0;</code>
       */
      public static final int StudentType_VALUE = 0;
      /**
       * <code>WorkerType = 1;</code>
       */
      public static final int WorkerType_VALUE = 1;


      public final int getNumber() {
        if (this == UNRECOGNIZED) {
          throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
        }
        return value;
      }

      /**
       * @param value The numeric wire value of the corresponding enum entry.
       * @return The enum associated with the given numeric wire value.
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @java.lang.Deprecated
      public static DataType valueOf(int value) {
        return forNumber(value);
      }

      /**
       * @param value The numeric wire value of the corresponding enum entry.
       * @return The enum associated with the given numeric wire value.
       */
      public static DataType forNumber(int value) {
        switch (value) {
          case 0:
            return StudentType;
          case 1:
            return WorkerType;
          default:
            return null;
        }
      }

      public static com.google.protobuf.Internal.EnumLiteMap<DataType>
      internalGetValueMap() {
        return internalValueMap;
      }

      private static final com.google.protobuf.Internal.EnumLiteMap<
        DataType> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<DataType>() {
          public DataType findValueByNumber(int number) {
            return DataType.forNumber(number);
          }
        };

      public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
        if (this == UNRECOGNIZED) {
          throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
        }
        return getDescriptor().getValues().get(ordinal());
      }

      public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
        return getDescriptor();
      }

      public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.getDescriptor().getEnumTypes().get(0);
      }

      private static final DataType[] VALUES = values();

      public static DataType valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
        if (desc.getType() != getDescriptor()) {
          throw new java.lang.IllegalArgumentException(
            "EnumValueDescriptor is not for this type.");
        }
        if (desc.getIndex() == -1) {
          return UNRECOGNIZED;
        }
        return VALUES[desc.getIndex()];
      }

      private final int value;

      private DataType(int value) {
        this.value = value;
      }

      // @@protoc_insertion_point(enum_scope:MyMessage.DataType)
    }

    private int dataBodyCase_ = 0;
    private java.lang.Object dataBody_;

    public enum DataBodyCase
      implements com.google.protobuf.Internal.EnumLite,
      com.google.protobuf.AbstractMessage.InternalOneOfEnum {
      STUDENT(2),
      WORKER(3),
      DATABODY_NOT_SET(0);
      private final int value;

      private DataBodyCase(int value) {
        this.value = value;
      }

      /**
       * @param value The number of the enum to look for.
       * @return The enum associated with the given number.
       * @deprecated Use {@link #forNumber(int)} instead.
       */
      @java.lang.Deprecated
      public static DataBodyCase valueOf(int value) {
        return forNumber(value);
      }

      public static DataBodyCase forNumber(int value) {
        switch (value) {
          case 2:
            return STUDENT;
          case 3:
            return WORKER;
          case 0:
            return DATABODY_NOT_SET;
          default:
            return null;
        }
      }

      public int getNumber() {
        return this.value;
      }
    }

    ;

    public DataBodyCase
    getDataBodyCase() {
      return DataBodyCase.forNumber(
        dataBodyCase_);
    }

    public static final int DATA_TYPE_FIELD_NUMBER = 1;
    private int dataType_;

    /**
     * <pre>
     * 用 data_type 来标识传的是哪一个枚举类型？
     * </pre>
     *
     * <code>.MyMessage.DataType data_type = 1;</code>
     *
     * @return The enum numeric value on the wire for dataType.
     */
    @java.lang.Override
    public int getDataTypeValue() {
      return dataType_;
    }

    /**
     * <pre>
     * 用 data_type 来标识传的是哪一个枚举类型？
     * </pre>
     *
     * <code>.MyMessage.DataType data_type = 1;</code>
     *
     * @return The dataType.
     */
    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType getDataType() {
      @SuppressWarnings("deprecation")
      com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType result = com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType.valueOf(dataType_);
      return result == null ? com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType.UNRECOGNIZED : result;
    }

    public static final int STUDENT_FIELD_NUMBER = 2;

    /**
     * <code>.Student student = 2;</code>
     *
     * @return Whether the student field is set.
     */
    @java.lang.Override
    public boolean hasStudent() {
      return dataBodyCase_ == 2;
    }

    /**
     * <code>.Student student = 2;</code>
     *
     * @return The student.
     */
    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.Student getStudent() {
      if (dataBodyCase_ == 2) {
        return (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_;
      }
      return com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance();
    }

    /**
     * <code>.Student student = 2;</code>
     */
    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.StudentOrBuilder getStudentOrBuilder() {
      if (dataBodyCase_ == 2) {
        return (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_;
      }
      return com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance();
    }

    public static final int WORKER_FIELD_NUMBER = 3;

    /**
     * <code>.Worker worker = 3;</code>
     *
     * @return Whether the worker field is set.
     */
    @java.lang.Override
    public boolean hasWorker() {
      return dataBodyCase_ == 3;
    }

    /**
     * <code>.Worker worker = 3;</code>
     *
     * @return The worker.
     */
    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.Worker getWorker() {
      if (dataBodyCase_ == 3) {
        return (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_;
      }
      return com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance();
    }

    /**
     * <code>.Worker worker = 3;</code>
     */
    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.WorkerOrBuilder getWorkerOrBuilder() {
      if (dataBodyCase_ == 3) {
        return (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_;
      }
      return com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance();
    }

    private byte memoizedIsInitialized = -1;

    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
      throws java.io.IOException {
      if (dataType_ != com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType.StudentType.getNumber()) {
        output.writeEnum(1, dataType_);
      }
      if (dataBodyCase_ == 2) {
        output.writeMessage(2, (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_);
      }
      if (dataBodyCase_ == 3) {
        output.writeMessage(3, (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (dataType_ != com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType.StudentType.getNumber()) {
        size += com.google.protobuf.CodedOutputStream
          .computeEnumSize(1, dataType_);
      }
      if (dataBodyCase_ == 2) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(2, (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_);
      }
      if (dataBodyCase_ == 3) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(3, (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage)) {
        return super.equals(obj);
      }
      com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage other = (com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage) obj;

      if (dataType_ != other.dataType_) return false;
      if (!getDataBodyCase().equals(other.getDataBodyCase())) return false;
      switch (dataBodyCase_) {
        case 2:
          if (!getStudent()
            .equals(other.getStudent())) return false;
          break;
        case 3:
          if (!getWorker()
            .equals(other.getWorker())) return false;
          break;
        case 0:
        default:
      }
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + DATA_TYPE_FIELD_NUMBER;
      hash = (53 * hash) + dataType_;
      switch (dataBodyCase_) {
        case 2:
          hash = (37 * hash) + STUDENT_FIELD_NUMBER;
          hash = (53 * hash) + getStudent().hashCode();
          break;
        case 3:
          hash = (37 * hash) + WORKER_FIELD_NUMBER;
          hash = (53 * hash) + getWorker().hashCode();
          break;
        case 0:
        default:
      }
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code MyMessage}
     */
    public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:MyMessage)
      com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessageOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_MyMessage_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_MyMessage_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
            com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.class, com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.Builder.class);
      }

      // Construct using com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
          .alwaysUseFieldBuilders) {
        }
      }

      @java.lang.Override
      public Builder clear() {
        super.clear();
        dataType_ = 0;

        dataBodyCase_ = 0;
        dataBody_ = null;
        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_MyMessage_descriptor;
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage getDefaultInstanceForType() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.getDefaultInstance();
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage build() {
        com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage buildPartial() {
        com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage result = new com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage(this);
        result.dataType_ = dataType_;
        if (dataBodyCase_ == 2) {
          if (studentBuilder_ == null) {
            result.dataBody_ = dataBody_;
          } else {
            result.dataBody_ = studentBuilder_.build();
          }
        }
        if (dataBodyCase_ == 3) {
          if (workerBuilder_ == null) {
            result.dataBody_ = dataBody_;
          } else {
            result.dataBody_ = workerBuilder_.build();
          }
        }
        result.dataBodyCase_ = dataBodyCase_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }

      @java.lang.Override
      public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.setField(field, value);
      }

      @java.lang.Override
      public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @java.lang.Override
      public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @java.lang.Override
      public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @java.lang.Override
      public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage) {
          return mergeFrom((com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage other) {
        if (other == com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.getDefaultInstance()) return this;
        if (other.dataType_ != 0) {
          setDataTypeValue(other.getDataTypeValue());
        }
        switch (other.getDataBodyCase()) {
          case STUDENT: {
            mergeStudent(other.getStudent());
            break;
          }
          case WORKER: {
            mergeWorker(other.getWorker());
            break;
          }
          case DATABODY_NOT_SET: {
            break;
          }
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
        com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int dataBodyCase_ = 0;
      private java.lang.Object dataBody_;

      public DataBodyCase
      getDataBodyCase() {
        return DataBodyCase.forNumber(
          dataBodyCase_);
      }

      public Builder clearDataBody() {
        dataBodyCase_ = 0;
        dataBody_ = null;
        onChanged();
        return this;
      }


      private int dataType_ = 0;

      /**
       * <pre>
       * 用 data_type 来标识传的是哪一个枚举类型？
       * </pre>
       *
       * <code>.MyMessage.DataType data_type = 1;</code>
       *
       * @return The enum numeric value on the wire for dataType.
       */
      @java.lang.Override
      public int getDataTypeValue() {
        return dataType_;
      }

      /**
       * <pre>
       * 用 data_type 来标识传的是哪一个枚举类型？
       * </pre>
       *
       * <code>.MyMessage.DataType data_type = 1;</code>
       *
       * @param value The enum numeric value on the wire for dataType to set.
       * @return This builder for chaining.
       */
      public Builder setDataTypeValue(int value) {

        dataType_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * 用 data_type 来标识传的是哪一个枚举类型？
       * </pre>
       *
       * <code>.MyMessage.DataType data_type = 1;</code>
       *
       * @return The dataType.
       */
      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType getDataType() {
        @SuppressWarnings("deprecation")
        com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType result = com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType.valueOf(dataType_);
        return result == null ? com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType.UNRECOGNIZED : result;
      }

      /**
       * <pre>
       * 用 data_type 来标识传的是哪一个枚举类型？
       * </pre>
       *
       * <code>.MyMessage.DataType data_type = 1;</code>
       *
       * @param value The dataType to set.
       * @return This builder for chaining.
       */
      public Builder setDataType(com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage.DataType value) {
        if (value == null) {
          throw new NullPointerException();
        }

        dataType_ = value.getNumber();
        onChanged();
        return this;
      }

      /**
       * <pre>
       * 用 data_type 来标识传的是哪一个枚举类型？
       * </pre>
       *
       * <code>.MyMessage.DataType data_type = 1;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearDataType() {

        dataType_ = 0;
        onChanged();
        return this;
      }

      private com.google.protobuf.SingleFieldBuilderV3<
        com.diguage.truman.netty.protobuf2.MyDataInfo.Student, com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder, com.diguage.truman.netty.protobuf2.MyDataInfo.StudentOrBuilder> studentBuilder_;

      /**
       * <code>.Student student = 2;</code>
       *
       * @return Whether the student field is set.
       */
      @java.lang.Override
      public boolean hasStudent() {
        return dataBodyCase_ == 2;
      }

      /**
       * <code>.Student student = 2;</code>
       *
       * @return The student.
       */
      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Student getStudent() {
        if (studentBuilder_ == null) {
          if (dataBodyCase_ == 2) {
            return (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_;
          }
          return com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance();
        } else {
          if (dataBodyCase_ == 2) {
            return studentBuilder_.getMessage();
          }
          return com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance();
        }
      }

      /**
       * <code>.Student student = 2;</code>
       */
      public Builder setStudent(com.diguage.truman.netty.protobuf2.MyDataInfo.Student value) {
        if (studentBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          dataBody_ = value;
          onChanged();
        } else {
          studentBuilder_.setMessage(value);
        }
        dataBodyCase_ = 2;
        return this;
      }

      /**
       * <code>.Student student = 2;</code>
       */
      public Builder setStudent(
        com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder builderForValue) {
        if (studentBuilder_ == null) {
          dataBody_ = builderForValue.build();
          onChanged();
        } else {
          studentBuilder_.setMessage(builderForValue.build());
        }
        dataBodyCase_ = 2;
        return this;
      }

      /**
       * <code>.Student student = 2;</code>
       */
      public Builder mergeStudent(com.diguage.truman.netty.protobuf2.MyDataInfo.Student value) {
        if (studentBuilder_ == null) {
          if (dataBodyCase_ == 2 &&
            dataBody_ != com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance()) {
            dataBody_ = com.diguage.truman.netty.protobuf2.MyDataInfo.Student.newBuilder((com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_)
              .mergeFrom(value).buildPartial();
          } else {
            dataBody_ = value;
          }
          onChanged();
        } else {
          if (dataBodyCase_ == 2) {
            studentBuilder_.mergeFrom(value);
          }
          studentBuilder_.setMessage(value);
        }
        dataBodyCase_ = 2;
        return this;
      }

      /**
       * <code>.Student student = 2;</code>
       */
      public Builder clearStudent() {
        if (studentBuilder_ == null) {
          if (dataBodyCase_ == 2) {
            dataBodyCase_ = 0;
            dataBody_ = null;
            onChanged();
          }
        } else {
          if (dataBodyCase_ == 2) {
            dataBodyCase_ = 0;
            dataBody_ = null;
          }
          studentBuilder_.clear();
        }
        return this;
      }

      /**
       * <code>.Student student = 2;</code>
       */
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder getStudentBuilder() {
        return getStudentFieldBuilder().getBuilder();
      }

      /**
       * <code>.Student student = 2;</code>
       */
      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.StudentOrBuilder getStudentOrBuilder() {
        if ((dataBodyCase_ == 2) && (studentBuilder_ != null)) {
          return studentBuilder_.getMessageOrBuilder();
        } else {
          if (dataBodyCase_ == 2) {
            return (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_;
          }
          return com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance();
        }
      }

      /**
       * <code>.Student student = 2;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<
        com.diguage.truman.netty.protobuf2.MyDataInfo.Student, com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder, com.diguage.truman.netty.protobuf2.MyDataInfo.StudentOrBuilder>
      getStudentFieldBuilder() {
        if (studentBuilder_ == null) {
          if (!(dataBodyCase_ == 2)) {
            dataBody_ = com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance();
          }
          studentBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.diguage.truman.netty.protobuf2.MyDataInfo.Student, com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder, com.diguage.truman.netty.protobuf2.MyDataInfo.StudentOrBuilder>(
            (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) dataBody_,
            getParentForChildren(),
            isClean());
          dataBody_ = null;
        }
        dataBodyCase_ = 2;
        onChanged();
        ;
        return studentBuilder_;
      }

      private com.google.protobuf.SingleFieldBuilderV3<
        com.diguage.truman.netty.protobuf2.MyDataInfo.Worker, com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder, com.diguage.truman.netty.protobuf2.MyDataInfo.WorkerOrBuilder> workerBuilder_;

      /**
       * <code>.Worker worker = 3;</code>
       *
       * @return Whether the worker field is set.
       */
      @java.lang.Override
      public boolean hasWorker() {
        return dataBodyCase_ == 3;
      }

      /**
       * <code>.Worker worker = 3;</code>
       *
       * @return The worker.
       */
      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Worker getWorker() {
        if (workerBuilder_ == null) {
          if (dataBodyCase_ == 3) {
            return (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_;
          }
          return com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance();
        } else {
          if (dataBodyCase_ == 3) {
            return workerBuilder_.getMessage();
          }
          return com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance();
        }
      }

      /**
       * <code>.Worker worker = 3;</code>
       */
      public Builder setWorker(com.diguage.truman.netty.protobuf2.MyDataInfo.Worker value) {
        if (workerBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          dataBody_ = value;
          onChanged();
        } else {
          workerBuilder_.setMessage(value);
        }
        dataBodyCase_ = 3;
        return this;
      }

      /**
       * <code>.Worker worker = 3;</code>
       */
      public Builder setWorker(
        com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder builderForValue) {
        if (workerBuilder_ == null) {
          dataBody_ = builderForValue.build();
          onChanged();
        } else {
          workerBuilder_.setMessage(builderForValue.build());
        }
        dataBodyCase_ = 3;
        return this;
      }

      /**
       * <code>.Worker worker = 3;</code>
       */
      public Builder mergeWorker(com.diguage.truman.netty.protobuf2.MyDataInfo.Worker value) {
        if (workerBuilder_ == null) {
          if (dataBodyCase_ == 3 &&
            dataBody_ != com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance()) {
            dataBody_ = com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.newBuilder((com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_)
              .mergeFrom(value).buildPartial();
          } else {
            dataBody_ = value;
          }
          onChanged();
        } else {
          if (dataBodyCase_ == 3) {
            workerBuilder_.mergeFrom(value);
          }
          workerBuilder_.setMessage(value);
        }
        dataBodyCase_ = 3;
        return this;
      }

      /**
       * <code>.Worker worker = 3;</code>
       */
      public Builder clearWorker() {
        if (workerBuilder_ == null) {
          if (dataBodyCase_ == 3) {
            dataBodyCase_ = 0;
            dataBody_ = null;
            onChanged();
          }
        } else {
          if (dataBodyCase_ == 3) {
            dataBodyCase_ = 0;
            dataBody_ = null;
          }
          workerBuilder_.clear();
        }
        return this;
      }

      /**
       * <code>.Worker worker = 3;</code>
       */
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder getWorkerBuilder() {
        return getWorkerFieldBuilder().getBuilder();
      }

      /**
       * <code>.Worker worker = 3;</code>
       */
      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.WorkerOrBuilder getWorkerOrBuilder() {
        if ((dataBodyCase_ == 3) && (workerBuilder_ != null)) {
          return workerBuilder_.getMessageOrBuilder();
        } else {
          if (dataBodyCase_ == 3) {
            return (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_;
          }
          return com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance();
        }
      }

      /**
       * <code>.Worker worker = 3;</code>
       */
      private com.google.protobuf.SingleFieldBuilderV3<
        com.diguage.truman.netty.protobuf2.MyDataInfo.Worker, com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder, com.diguage.truman.netty.protobuf2.MyDataInfo.WorkerOrBuilder>
      getWorkerFieldBuilder() {
        if (workerBuilder_ == null) {
          if (!(dataBodyCase_ == 3)) {
            dataBody_ = com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance();
          }
          workerBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.diguage.truman.netty.protobuf2.MyDataInfo.Worker, com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder, com.diguage.truman.netty.protobuf2.MyDataInfo.WorkerOrBuilder>(
            (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) dataBody_,
            getParentForChildren(),
            isClean());
          dataBody_ = null;
        }
        dataBodyCase_ = 3;
        onChanged();
        ;
        return workerBuilder_;
      }

      @java.lang.Override
      public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:MyMessage)
    }

    // @@protoc_insertion_point(class_scope:MyMessage)
    private static final com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage();
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<MyMessage>
      PARSER = new com.google.protobuf.AbstractParser<MyMessage>() {
      @java.lang.Override
      public MyMessage parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new MyMessage(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<MyMessage> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<MyMessage> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.MyMessage getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface StudentOrBuilder extends
    // @@protoc_insertion_point(interface_extends:Student)
    com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
     * </pre>
     *
     * <code>int32 id = 1;</code>
     *
     * @return The id.
     */
    int getId();

    /**
     * <code>string name = 2;</code>
     *
     * @return The name.
     */
    java.lang.String getName();

    /**
     * <code>string name = 2;</code>
     *
     * @return The bytes for name.
     */
    com.google.protobuf.ByteString
    getNameBytes();
  }

  /**
   * <pre>
   * 会在 StudentPOJO 外部类生成一个内部类 Student，
   * </pre>
   * <p>
   * Protobuf type {@code Student}
   */
  public static final class Student extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:Student)
    StudentOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use Student.newBuilder() to construct.
    private Student(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private Student() {
      name_ = "";
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
      return new Student();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    private Student(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {

              id_ = input.readInt32();
              break;
            }
            case 18: {
              java.lang.String s = input.readStringRequireUtf8();

              name_ = s;
              break;
            }
            default: {
              if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Student_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
      return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Student_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
          com.diguage.truman.netty.protobuf2.MyDataInfo.Student.class, com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder.class);
    }

    public static final int ID_FIELD_NUMBER = 1;
    private int id_;

    /**
     * <pre>
     * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
     * </pre>
     *
     * <code>int32 id = 1;</code>
     *
     * @return The id.
     */
    @java.lang.Override
    public int getId() {
      return id_;
    }

    public static final int NAME_FIELD_NUMBER = 2;
    private volatile java.lang.Object name_;

    /**
     * <code>string name = 2;</code>
     *
     * @return The name.
     */
    @java.lang.Override
    public java.lang.String getName() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs =
          (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        name_ = s;
        return s;
      }
    }

    /**
     * <code>string name = 2;</code>
     *
     * @return The bytes for name.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
    getNameBytes() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8(
            (java.lang.String) ref);
        name_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    private byte memoizedIsInitialized = -1;

    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
      throws java.io.IOException {
      if (id_ != 0) {
        output.writeInt32(1, id_);
      }
      if (!getNameBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 2, name_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (id_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(1, id_);
      }
      if (!getNameBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, name_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof com.diguage.truman.netty.protobuf2.MyDataInfo.Student)) {
        return super.equals(obj);
      }
      com.diguage.truman.netty.protobuf2.MyDataInfo.Student other = (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) obj;

      if (getId()
        != other.getId()) return false;
      if (!getName()
        .equals(other.getName())) return false;
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + ID_FIELD_NUMBER;
      hash = (53 * hash) + getId();
      hash = (37 * hash) + NAME_FIELD_NUMBER;
      hash = (53 * hash) + getName().hashCode();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.diguage.truman.netty.protobuf2.MyDataInfo.Student prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * <pre>
     * 会在 StudentPOJO 外部类生成一个内部类 Student，
     * </pre>
     * <p>
     * Protobuf type {@code Student}
     */
    public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:Student)
      com.diguage.truman.netty.protobuf2.MyDataInfo.StudentOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Student_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Student_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
            com.diguage.truman.netty.protobuf2.MyDataInfo.Student.class, com.diguage.truman.netty.protobuf2.MyDataInfo.Student.Builder.class);
      }

      // Construct using com.diguage.truman.netty.protobuf2.MyDataInfo.Student.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
          .alwaysUseFieldBuilders) {
        }
      }

      @java.lang.Override
      public Builder clear() {
        super.clear();
        id_ = 0;

        name_ = "";

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Student_descriptor;
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Student getDefaultInstanceForType() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance();
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Student build() {
        com.diguage.truman.netty.protobuf2.MyDataInfo.Student result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Student buildPartial() {
        com.diguage.truman.netty.protobuf2.MyDataInfo.Student result = new com.diguage.truman.netty.protobuf2.MyDataInfo.Student(this);
        result.id_ = id_;
        result.name_ = name_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }

      @java.lang.Override
      public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.setField(field, value);
      }

      @java.lang.Override
      public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @java.lang.Override
      public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @java.lang.Override
      public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @java.lang.Override
      public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.diguage.truman.netty.protobuf2.MyDataInfo.Student) {
          return mergeFrom((com.diguage.truman.netty.protobuf2.MyDataInfo.Student) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.diguage.truman.netty.protobuf2.MyDataInfo.Student other) {
        if (other == com.diguage.truman.netty.protobuf2.MyDataInfo.Student.getDefaultInstance()) return this;
        if (other.getId() != 0) {
          setId(other.getId());
        }
        if (!other.getName().isEmpty()) {
          name_ = other.name_;
          onChanged();
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
        com.diguage.truman.netty.protobuf2.MyDataInfo.Student parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.diguage.truman.netty.protobuf2.MyDataInfo.Student) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private int id_;

      /**
       * <pre>
       * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
       * </pre>
       *
       * <code>int32 id = 1;</code>
       *
       * @return The id.
       */
      @java.lang.Override
      public int getId() {
        return id_;
      }

      /**
       * <pre>
       * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
       * </pre>
       *
       * <code>int32 id = 1;</code>
       *
       * @param value The id to set.
       * @return This builder for chaining.
       */
      public Builder setId(int value) {

        id_ = value;
        onChanged();
        return this;
      }

      /**
       * <pre>
       * 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
       * </pre>
       *
       * <code>int32 id = 1;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearId() {

        id_ = 0;
        onChanged();
        return this;
      }

      private java.lang.Object name_ = "";

      /**
       * <code>string name = 2;</code>
       *
       * @return The name.
       */
      public java.lang.String getName() {
        java.lang.Object ref = name_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          name_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }

      /**
       * <code>string name = 2;</code>
       *
       * @return The bytes for name.
       */
      public com.google.protobuf.ByteString
      getNameBytes() {
        java.lang.Object ref = name_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
          name_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>string name = 2;</code>
       *
       * @param value The name to set.
       * @return This builder for chaining.
       */
      public Builder setName(
        java.lang.String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        name_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string name = 2;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearName() {

        name_ = getDefaultInstance().getName();
        onChanged();
        return this;
      }

      /**
       * <code>string name = 2;</code>
       *
       * @param value The bytes for name to set.
       * @return This builder for chaining.
       */
      public Builder setNameBytes(
        com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        name_ = value;
        onChanged();
        return this;
      }

      @java.lang.Override
      public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:Student)
    }

    // @@protoc_insertion_point(class_scope:Student)
    private static final com.diguage.truman.netty.protobuf2.MyDataInfo.Student DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new com.diguage.truman.netty.protobuf2.MyDataInfo.Student();
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Student getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Student>
      PARSER = new com.google.protobuf.AbstractParser<Student>() {
      @java.lang.Override
      public Student parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new Student(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<Student> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Student> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.Student getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  public interface WorkerOrBuilder extends
    // @@protoc_insertion_point(interface_extends:Worker)
    com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string name = 1;</code>
     *
     * @return The name.
     */
    java.lang.String getName();

    /**
     * <code>string name = 1;</code>
     *
     * @return The bytes for name.
     */
    com.google.protobuf.ByteString
    getNameBytes();

    /**
     * <code>int32 age = 2;</code>
     *
     * @return The age.
     */
    int getAge();
  }

  /**
   * Protobuf type {@code Worker}
   */
  public static final class Worker extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:Worker)
    WorkerOrBuilder {
    private static final long serialVersionUID = 0L;

    // Use Worker.newBuilder() to construct.
    private Worker(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }

    private Worker() {
      name_ = "";
    }

    @java.lang.Override
    @SuppressWarnings({"unused"})
    protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
      return new Worker();
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }

    private Worker(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 10: {
              java.lang.String s = input.readStringRequireUtf8();

              name_ = s;
              break;
            }
            case 16: {

              age_ = input.readInt32();
              break;
            }
            default: {
              if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
      } finally {
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
      return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Worker_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
      return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Worker_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
          com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.class, com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder.class);
    }

    public static final int NAME_FIELD_NUMBER = 1;
    private volatile java.lang.Object name_;

    /**
     * <code>string name = 1;</code>
     *
     * @return The name.
     */
    @java.lang.Override
    public java.lang.String getName() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        return (java.lang.String) ref;
      } else {
        com.google.protobuf.ByteString bs =
          (com.google.protobuf.ByteString) ref;
        java.lang.String s = bs.toStringUtf8();
        name_ = s;
        return s;
      }
    }

    /**
     * <code>string name = 1;</code>
     *
     * @return The bytes for name.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
    getNameBytes() {
      java.lang.Object ref = name_;
      if (ref instanceof java.lang.String) {
        com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8(
            (java.lang.String) ref);
        name_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    public static final int AGE_FIELD_NUMBER = 2;
    private int age_;

    /**
     * <code>int32 age = 2;</code>
     *
     * @return The age.
     */
    @java.lang.Override
    public int getAge() {
      return age_;
    }

    private byte memoizedIsInitialized = -1;

    @java.lang.Override
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    @java.lang.Override
    public void writeTo(com.google.protobuf.CodedOutputStream output)
      throws java.io.IOException {
      if (!getNameBytes().isEmpty()) {
        com.google.protobuf.GeneratedMessageV3.writeString(output, 1, name_);
      }
      if (age_ != 0) {
        output.writeInt32(2, age_);
      }
      unknownFields.writeTo(output);
    }

    @java.lang.Override
    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (!getNameBytes().isEmpty()) {
        size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, name_);
      }
      if (age_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(2, age_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof com.diguage.truman.netty.protobuf2.MyDataInfo.Worker)) {
        return super.equals(obj);
      }
      com.diguage.truman.netty.protobuf2.MyDataInfo.Worker other = (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) obj;

      if (!getName()
        .equals(other.getName())) return false;
      if (getAge()
        != other.getAge()) return false;
      if (!unknownFields.equals(other.unknownFields)) return false;
      return true;
    }

    @java.lang.Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + NAME_FIELD_NUMBER;
      hash = (53 * hash) + getName().hashCode();
      hash = (37 * hash) + AGE_FIELD_NUMBER;
      hash = (53 * hash) + getAge();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
    }

    @java.lang.Override
    public Builder newBuilderForType() {
      return newBuilder();
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(com.diguage.truman.netty.protobuf2.MyDataInfo.Worker prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    @java.lang.Override
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }

    /**
     * Protobuf type {@code Worker}
     */
    public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:Worker)
      com.diguage.truman.netty.protobuf2.MyDataInfo.WorkerOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Worker_descriptor;
      }

      @java.lang.Override
      protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Worker_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
            com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.class, com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.Builder.class);
      }

      // Construct using com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }

      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
          .alwaysUseFieldBuilders) {
        }
      }

      @java.lang.Override
      public Builder clear() {
        super.clear();
        name_ = "";

        age_ = 0;

        return this;
      }

      @java.lang.Override
      public com.google.protobuf.Descriptors.Descriptor
      getDescriptorForType() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.internal_static_Worker_descriptor;
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Worker getDefaultInstanceForType() {
        return com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance();
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Worker build() {
        com.diguage.truman.netty.protobuf2.MyDataInfo.Worker result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      @java.lang.Override
      public com.diguage.truman.netty.protobuf2.MyDataInfo.Worker buildPartial() {
        com.diguage.truman.netty.protobuf2.MyDataInfo.Worker result = new com.diguage.truman.netty.protobuf2.MyDataInfo.Worker(this);
        result.name_ = name_;
        result.age_ = age_;
        onBuilt();
        return result;
      }

      @java.lang.Override
      public Builder clone() {
        return super.clone();
      }

      @java.lang.Override
      public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.setField(field, value);
      }

      @java.lang.Override
      public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
        return super.clearField(field);
      }

      @java.lang.Override
      public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return super.clearOneof(oneof);
      }

      @java.lang.Override
      public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
        return super.setRepeatedField(field, index, value);
      }

      @java.lang.Override
      public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
        return super.addRepeatedField(field, value);
      }

      @java.lang.Override
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) {
          return mergeFrom((com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(com.diguage.truman.netty.protobuf2.MyDataInfo.Worker other) {
        if (other == com.diguage.truman.netty.protobuf2.MyDataInfo.Worker.getDefaultInstance()) return this;
        if (!other.getName().isEmpty()) {
          name_ = other.name_;
          onChanged();
        }
        if (other.getAge() != 0) {
          setAge(other.getAge());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      @java.lang.Override
      public final boolean isInitialized() {
        return true;
      }

      @java.lang.Override
      public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
        com.diguage.truman.netty.protobuf2.MyDataInfo.Worker parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (com.diguage.truman.netty.protobuf2.MyDataInfo.Worker) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private java.lang.Object name_ = "";

      /**
       * <code>string name = 1;</code>
       *
       * @return The name.
       */
      public java.lang.String getName() {
        java.lang.Object ref = name_;
        if (!(ref instanceof java.lang.String)) {
          com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
          java.lang.String s = bs.toStringUtf8();
          name_ = s;
          return s;
        } else {
          return (java.lang.String) ref;
        }
      }

      /**
       * <code>string name = 1;</code>
       *
       * @return The bytes for name.
       */
      public com.google.protobuf.ByteString
      getNameBytes() {
        java.lang.Object ref = name_;
        if (ref instanceof String) {
          com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
              (java.lang.String) ref);
          name_ = b;
          return b;
        } else {
          return (com.google.protobuf.ByteString) ref;
        }
      }

      /**
       * <code>string name = 1;</code>
       *
       * @param value The name to set.
       * @return This builder for chaining.
       */
      public Builder setName(
        java.lang.String value) {
        if (value == null) {
          throw new NullPointerException();
        }

        name_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>string name = 1;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearName() {

        name_ = getDefaultInstance().getName();
        onChanged();
        return this;
      }

      /**
       * <code>string name = 1;</code>
       *
       * @param value The bytes for name to set.
       * @return This builder for chaining.
       */
      public Builder setNameBytes(
        com.google.protobuf.ByteString value) {
        if (value == null) {
          throw new NullPointerException();
        }
        checkByteStringIsUtf8(value);

        name_ = value;
        onChanged();
        return this;
      }

      private int age_;

      /**
       * <code>int32 age = 2;</code>
       *
       * @return The age.
       */
      @java.lang.Override
      public int getAge() {
        return age_;
      }

      /**
       * <code>int32 age = 2;</code>
       *
       * @param value The age to set.
       * @return This builder for chaining.
       */
      public Builder setAge(int value) {

        age_ = value;
        onChanged();
        return this;
      }

      /**
       * <code>int32 age = 2;</code>
       *
       * @return This builder for chaining.
       */
      public Builder clearAge() {

        age_ = 0;
        onChanged();
        return this;
      }

      @java.lang.Override
      public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFields(unknownFields);
      }

      @java.lang.Override
      public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }


      // @@protoc_insertion_point(builder_scope:Worker)
    }

    // @@protoc_insertion_point(class_scope:Worker)
    private static final com.diguage.truman.netty.protobuf2.MyDataInfo.Worker DEFAULT_INSTANCE;

    static {
      DEFAULT_INSTANCE = new com.diguage.truman.netty.protobuf2.MyDataInfo.Worker();
    }

    public static com.diguage.truman.netty.protobuf2.MyDataInfo.Worker getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Worker>
      PARSER = new com.google.protobuf.AbstractParser<Worker>() {
      @java.lang.Override
      public Worker parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
        return new Worker(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<Worker> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Worker> getParserForType() {
      return PARSER;
    }

    @java.lang.Override
    public com.diguage.truman.netty.protobuf2.MyDataInfo.Worker getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_MyMessage_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internal_static_MyMessage_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Student_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internal_static_Student_fieldAccessorTable;
  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_Worker_descriptor;
  private static final
  com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internal_static_Worker_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
  getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor
    descriptor;

  static {
    java.lang.String[] descriptorData = {
      "\n\rStudent.proto\"\244\001\n\tMyMessage\022&\n\tdata_ty" +
        "pe\030\001 \001(\0162\023.MyMessage.DataType\022\033\n\007student" +
        "\030\002 \001(\0132\010.StudentH\000\022\031\n\006worker\030\003 \001(\0132\007.Wor" +
        "kerH\000\"+\n\010DataType\022\017\n\013StudentType\020\000\022\016\n\nWo" +
        "rkerType\020\001B\n\n\010dataBody\"#\n\007Student\022\n\n\002id\030" +
        "\001 \001(\005\022\014\n\004name\030\002 \001(\t\"#\n\006Worker\022\014\n\004name\030\001 " +
        "\001(\t\022\013\n\003age\030\002 \001(\005B2\n\"com.diguage.truman.n" +
        "etty.protobuf2B\nMyDataInfoH\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[]{
        });
    internal_static_MyMessage_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_MyMessage_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
      internal_static_MyMessage_descriptor,
      new java.lang.String[]{"DataType", "Student", "Worker", "DataBody",});
    internal_static_Student_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_Student_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
      internal_static_Student_descriptor,
      new java.lang.String[]{"Id", "Name",});
    internal_static_Worker_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_Worker_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
      internal_static_Worker_descriptor,
      new java.lang.String[]{"Name", "Age",});
  }

  // @@protoc_insertion_point(outer_class_scope)
}
```

```JAVA
package com.diguage.truman.netty.protobuf2;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;



public class ProtobufClient2 {
  public static void main(String[] args) throws InterruptedException {
    // 客户端只需要一个事件循环组即可
    NioEventLoopGroup group = new NioEventLoopGroup();

    try {
      // 创建客户端启动对象
      Bootstrap bootstrap = new Bootstrap();
      // 设置相关参数
      bootstrap.group(group) // 设置线程组
        .channel(NioSocketChannel.class) // 设置客户端通讯通道的实现类
        .handler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            // 加入 protobuf handler
            pipeline.addLast("encoder", new ProtobufEncoder());
            pipeline.addLast(new ClientHandler()); // 加入自己的处理器
          }
        });
      System.out.println("....客户端 OK ...");

      // 启动客户端去连接服务器端
      // 关于 ChannelFuture 还要分析，涉及到 Netty 的异步模型
      ChannelFuture future = bootstrap.connect("127.0.0.1", 11911).sync();

      // 给关闭通道进行监听
      future.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}
```

```JAVA
package com.diguage.truman.netty.protobuf2;

import com.diguage.truman.netty.protobuf.StudentPOJO;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;




public class ProtobufServer2 {
  public static void main(String[] args) throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO)) // 在 bossGroup 增加一个日志处理器
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("decoder", new ProtobufDecoder(MyDataInfo.MyMessage.getDefaultInstance()));
            pipeline.addLast(new ServerHandler());
          }
        });

      ChannelFuture future = serverBootstrap.bind(11911).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

```JAVA
package com.diguage.truman.netty.protobuf2;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 说明：
 * 我们自定义一个 Handler 需要继承 netty 规定好的某个 HandlerAdapter
 *
 */
public class ServerHandler extends SimpleChannelInboundHandler<MyDataInfo.MyMessage> {
  /**
   * 读取数据实际（这里我们可以读取客户端发送的消息）
   */
  @Override
  public void channelRead0(ChannelHandlerContext ctx, MyDataInfo.MyMessage msg) throws Exception {
    MyDataInfo.MyMessage.DataType dataType = msg.getDataType();
    if (dataType == MyDataInfo.MyMessage.DataType.StudentType) {
      MyDataInfo.Student student = msg.getStudent();
      System.out.println("学生 id=" + student.getId() + ", name=" + student.getName());
    } else if (dataType == MyDataInfo.MyMessage.DataType.WorkerType) {
      MyDataInfo.Worker worker = msg.getWorker();
      System.out.println("工人 name=" + worker.getName() + ", age=" + worker.getAge());
    } else {
      System.out.println("传输类型不正确！");
    }
  }

  /**
   * 数据读取完毕
   */
  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    // 将数据写入到缓存，并刷新
    // 一般讲，我们对这个发送的数据进行编码
    ctx.writeAndFlush(Unpooled.copiedBuffer("Hello, D瓜哥~, pong -> O(∩_∩)O哈哈~", UTF_8));
  }

  /**
   * 处理异常，一般需要关闭通道
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ctx.close();
  }
}
```

```JAVA
syntax = "proto3"; // 版本号
option optimize_for = SPEED;
option java_package = "com.diguage.truman.netty.protobuf2";

option java_outer_classname = "MyDataInfo"; // 生成的外部类名，同时也是文件名

// protobuf 使用 message 管理数据
// protobuf 可以使用 message 管理其他的 message

message MyMessage {
    // 定义一个枚举
    enum DataType {
        StudentType = 0; // 在 proto3 中，要求 enum 的编号从 0 开始
        WorkerType = 1;
    }

    // 用 data_type 来标识传的是哪一个枚举类型？
    DataType data_type = 1;

    // 表示每次枚举类型最多只能出现其中的一个，节省空间
    oneof dataBody {
        Student student = 2;
        Worker worker = 3;
    }
}

// 会在 StudentPOJO 外部类生成一个内部类 Student，
message Student {
    int32 id = 1; // 在 Student 类中有一个属性名称为 id 类型为 int32，1表示属性序号，不是值
    string name = 2;
}

message Worker {
    string name = 1;
    int32 age = 2;
}

// 在这个文件所在目录执行如下命令，生成Java类：
// protoc --java_out=. Student.proto
// WARNING: 需要给生成的类，加上 package 名。
```

```JAVA
package com.diguage.truman.netty.tcp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;



public class TcpClient {
  public static void main(String[] args) throws InterruptedException {
    // 客户端只需要一个事件循环组即可
    NioEventLoopGroup group = new NioEventLoopGroup();

    try {
      // 创建客户端启动对象
      Bootstrap bootstrap = new Bootstrap();
      // 设置相关参数
      bootstrap.group(group) // 设置线程组
        .channel(NioSocketChannel.class) // 设置客户端通讯通道的实现类
        .handler(new TcpClientInitializer());
      System.out.println("....客户端 OK ...");

      // 启动客户端去连接服务器端
      // 关于 ChannelFuture 还要分析，涉及到 Netty 的异步模型
      ChannelFuture future = bootstrap.connect("127.0.0.1", 11911).sync();

      // 给关闭通道进行监听
      future.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}
```

```JAVA
package com.diguage.truman.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static java.nio.charset.StandardCharsets.UTF_8;




public class TcpClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
  private int count;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    byte[] bytes = new byte[msg.readableBytes()];
    msg.readBytes(bytes);
    String message = new String(bytes, UTF_8);
    System.out.println("客户端接收到消息=" + message);
    System.out.println("客户端接收到消息量=" + (++this.count));
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    // 使用客户端发送10条数据
    for (int i = 0; i < 10; i++) {
      ByteBuf buffer = Unpooled.copiedBuffer("Hello，D瓜哥！~~" + i, UTF_8);
      ctx.writeAndFlush(buffer);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.channel().close();
  }
}
```

```JAVA
package com.diguage.truman.netty.tcp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;



public class TcpClientInitializer extends ChannelInitializer<SocketChannel> {
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new TcpClientHandler());
  }
}
```

```JAVA
package com.diguage.truman.netty.tcp;

import com.diguage.truman.netty.iobound.IoServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;



public class TcpServer {
  public static void main(String[] args) throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO)) // 在 bossGroup 增加一个日志处理器
        .childHandler(new TcpServerInitializer());

      ChannelFuture future = serverBootstrap.bind(11911).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

```JAVA
package com.diguage.truman.netty.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;



public class TcpServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
  private int count;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    byte[] bytes = new byte[msg.readableBytes()];
    msg.readBytes(bytes);

    // 将 bytes 转成字符串
    String message = new String(bytes, UTF_8);
    System.out.println("服务器接收到数据=" + message);
    System.out.println("服务器接收到消息量=" + (++this.count));

    // 服务器回送数据给客户端，回送一个随机ID
    ByteBuf buffer = Unpooled.copiedBuffer(UUID.randomUUID().toString() + "  ", UTF_8);
    ctx.writeAndFlush(buffer);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.channel().close();
  }
}
```

```JAVA
package com.diguage.truman.netty.tcp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;



public class TcpServerInitializer extends ChannelInitializer<SocketChannel> {
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new TcpServerHandler());
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;




public class MessageDecoder extends ReplayingDecoder<Void> {
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    System.out.println("\n\nMessageDecoder decode 被调用");
    // 需要将得到的二进制字节码 -> MessageProtocol 数据包
    int len = in.readInt();
    byte[] bytes = new byte[len];
    in.readBytes(bytes);
    // 封装成 MessageProtocol 对象，放入out，传递给下一个 handler 业务处理
    MessageProtocol msp = new MessageProtocol();
    msp.setLen(len);
    msp.setContent(bytes);
    out.add(msp);
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;



public class MessageEncoder extends MessageToByteEncoder<MessageProtocol> {
  @Override
  protected void encode(ChannelHandlerContext ctx, MessageProtocol msg, ByteBuf out) throws Exception {
    System.out.println("MessageEncoder encode 方法被调用");
    out.writeInt(msg.getLen());
    out.writeBytes(msg.getContent());
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;



public class MessageProtocol {
  private int len;
  private byte[] content;

  public int getLen() {
    return len;
  }

  public void setLen(int len) {
    this.len = len;
  }

  public byte[] getContent() {
    return content;
  }

  public void setContent(byte[] content) {
    this.content = content;
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static java.nio.charset.StandardCharsets.UTF_8;




public class TcpClientHandler extends SimpleChannelInboundHandler<MessageProtocol> {
  private int count;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
    // 接收到数据，并处理
    int len = msg.getLen();
    byte[] bytes = msg.getContent();
    System.out.println("客户端接收到信息如下：");
    System.out.println("长度=" + len);
    System.out.println("消息=" + new String(bytes, UTF_8));
    System.out.println("客户端接收到消息包数量=" + (++this.count));
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    // 使用客户端发送10条数据
    for (int i = 0; i < 10; i++) {
      String msg = "Hello，D瓜哥！~~" + i;
      byte[] content = msg.getBytes(UTF_8);
      // 创建协议包
      MessageProtocol msp = new MessageProtocol();
      msp.setContent(content);
      msp.setLen(content.length);

      ctx.writeAndFlush(msp);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.channel().close();
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;




public class TcpClientInitializer extends ChannelInitializer<SocketChannel> {
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new MessageEncoder());// TODO 必须放在 handler 上面吗？
    pipeline.addLast(new MessageDecoder());
    pipeline.addLast(new TcpClientHandler());
  }
}

```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;




public class TcpServerHandler extends SimpleChannelInboundHandler<MessageProtocol> {
  private int count;

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MessageProtocol msg) throws Exception {
    // 接收到数据，并处理
    int len = msg.getLen();
    byte[] bytes = msg.getContent();
    System.out.println("服务器接收到信息如下：");
    System.out.println("长度=" + len);
    System.out.println("消息=" + new String(bytes, UTF_8));
    System.out.println("服务器接收到消息包数量=" + (++this.count));

    String responseContent = UUID.randomUUID().toString();
    byte[] responseContentBytes = responseContent.getBytes(UTF_8);
    MessageProtocol messageProtocol = new MessageProtocol();
    messageProtocol.setContent(responseContentBytes);
    messageProtocol.setLen(responseContentBytes.length);

    ctx.writeAndFlush(messageProtocol);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.channel().close();
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;



public class TcpServerInitializer extends ChannelInitializer<SocketChannel> {
  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new MessageDecoder());
    pipeline.addLast(new MessageEncoder());
    pipeline.addLast(new TcpServerHandler());
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;




public class TcprotocolClient {
  public static void main(String[] args) throws InterruptedException {
    // 客户端只需要一个事件循环组即可
    NioEventLoopGroup group = new NioEventLoopGroup();

    try {
      // 创建客户端启动对象
      Bootstrap bootstrap = new Bootstrap();
      // 设置相关参数
      bootstrap.group(group) // 设置线程组
        .channel(NioSocketChannel.class) // 设置客户端通讯通道的实现类
        .handler(new TcpClientInitializer());
      System.out.println("....客户端 OK ...");

      // 启动客户端去连接服务器端
      // 关于 ChannelFuture 还要分析，涉及到 Netty 的异步模型
      ChannelFuture future = bootstrap.connect("127.0.0.1", 11911).sync();

      // 给关闭通道进行监听
      future.channel().closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}
```

```JAVA
package com.diguage.truman.netty.tcprotocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;



public class TcprotocolServer {
  public static void main(String[] args) throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 128)
        .handler(new LoggingHandler(LogLevel.INFO)) // 在 bossGroup 增加一个日志处理器
        .childHandler(new TcpServerInitializer());

      ChannelFuture future = serverBootstrap.bind(11911).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

```JAVA
package com.diguage.truman.netty.ws;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;



public class Server {
  public static void main(String[] args) throws InterruptedException {
    NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
    NioEventLoopGroup workerGroup = new NioEventLoopGroup();
    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO)) // 在 bossGroup 增加一个日志处理器
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            // 因为基于 HTTP 协议，使用 HTTP 的编解码器
            pipeline.addLast(new HttpServerCodec());
            // 是以块方法写，加 ChunkedWriteHandler 处理器
            pipeline.addLast(new ChunkedWriteHandler());
            /**
             * 说明
             * 1. HTTP 数据在传输过程中是分段的，HttpObjectAggregator 就可以将多个段聚合
             * 2. 这就是为什么，当浏览器发送大量数据时，就会发出多次 HTTP 请求
             */
            pipeline.addLast(new HttpObjectAggregator(8192));
            /**
             * 说明
             * 1. 对应 WebSocket，它的数据是以帧(frame)形式传递
             * 2. 可以看到 WebSocketFrame 下面有六个子类
             * 3. 浏览器请求时： ws://localhost:11911/hello
             * 4. WebSocketServerProtocolHandler 核心功能是将 HTTP 协议升级为 WS 协议，保持长连接
             *    这一点可以观察浏览器的链接信息，可以看到协议升级的过程。
             */
            pipeline.addLast(new WebSocketServerProtocolHandler("/hello"));

            // 自定义 Handler，处理业务逻辑
            pipeline.addLast(new TextWebSocketFrameHandler());
          }
        });

      ChannelFuture future = serverBootstrap.bind(11911).sync();
      future.channel().closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }
}
```

```JAVA
package com.diguage.truman.netty.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.time.LocalDateTime;

/**
 * TextWebSocketFrame 类型，表示一个文本帧
 *
 */
public class TextWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    System.out.println("服务器收到消息：" + msg.text());
    // 回复消息
    ctx.channel().writeAndFlush(new TextWebSocketFrame("服务器时间："
      + LocalDateTime.now() + " " + msg.text()));
  }

  /**
   * 当 Web 客户端连接后，出发方法
   */
  @Override
  public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
    System.out.println("handlerAdded 被调用" + ctx.channel().id().asLongText());
    System.out.println("handlerAdded 被调用" + ctx.channel().id().asShortText());
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    System.out.println("handlerRemoved 被调用" + ctx.channel().id().asLongText());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    System.out.println("发生异常：" + cause.getMessage());
    ctx.channel().close();
  }
}
```

```JAVA
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<script type="application/javascript">
    var socket;
    if (window.WebSocket) {
        socket = new WebSocket("ws://localhost:11911/hello")
        // 相当于 channel Read0，event 收到服务器回送的消息
        socket.onmessage = function (event) {
            var ele = document.getElementById("responseText");
            ele.value = ele.value + "\n" + event.data;
        };
        // 连接开启
        socket.onopen = function (event) {
            var ele = document.getElementById("responseText");
            ele.value = "连接开启了...";
        };
    } else {
        alert("当前浏览器不支持 WebSocket")
    }

    function send(message) {
        if (!window.WebSocket) {
            return;
        }
        if (socket.readyState === WebSocket.OPEN) {
            socket.send(message);
        } else {
            alert("连接未开启！");
        }
    }
</script>
<form onsubmit="return false">
    <textarea name="message" style="height: 300px;width: 300px;"></textarea>
    <input type="button" value="发送消息" onclick="send(this.form.message.value)">
    <textarea id="responseText" style="height: 300px;width: 300px;"></textarea>
    <input type="button" value="清空消息" onclick="document.getElementById('responseText').value=''">
</form>
</body>
</html>
```

