# JDK 源码解析

# Basic

## String

String 的源码大家应该都能看懂，这里就不一一分析咯，重点讲一下 equals()和 hashcode()方法，然后看一下 String 类常用方法的实现，就当一起温习一下咯。

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {

    /** 保存String的字节数组 */
    private final char value[];

    /** 缓存这个String的hash值 */
    private int hash; // Default to 0

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -6849794470754667710L;

    /**
     * 1、Object的 hashCode()返回该对象的内存地址编号，而equals()比较的是内存地址是否相等；
     * 2、需要注意的是当equals()方法被重写时，hashCode()也要被重写；
     * 3、按照一般hashCode()方法的实现来说，equals()相等的两个对象，hashcode()必须保持相等；
     *    equals()不相等的两个对象，hashcode()未必不相等
     * 4、一个类如果要作为 HashMap 的 key，必须重写equals()和hashCode()方法
     */
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String)anObject;
            int n = value.length;
            if (n == anotherString.value.length) {
                char v1[] = value;
                char v2[] = anotherString.value;
                int i = 0;
                while (n-- != 0) {
                    if (v1[i] != v2[i])
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        int h = hash;
        if (h == 0 && value.length > 0) {
            char val[] = value;

            for (int i = 0; i < value.length; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }

    /**
     * 指定下标的char
     */
    public char charAt(int index) {
        if ((index < 0) || (index >= value.length)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index];
    }

    /**
     * 是否以 prefix 为前缀
     */
    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    /**
     * 是否以 suffix 为后缀
     */
    public boolean endsWith(String suffix) {
        return startsWith(suffix, value.length - suffix.value.length);
    }

    /**
     * 该String对象 是否满足 regex正则表达式
     */
    public boolean matches(String regex) {
        return Pattern.matches(regex, this);
    }

    /**
     * 字符替换
     */
    public String replace(char oldChar, char newChar) {
        if (oldChar != newChar) {
            int len = value.length;
            int i = -1;
            char[] val = value; /* avoid getfield opcode */

            while (++i < len) {
                if (val[i] == oldChar) {
                    break;
                }
            }
            if (i < len) {
                char buf[] = new char[len];
                for (int j = 0; j < i; j++) {
                    buf[j] = val[j];
                }
                while (i < len) {
                    char c = val[i];
                    buf[i] = (c == oldChar) ? newChar : c;
                    i++;
                }
                return new String(buf, true);
            }
        }
        return this;
    }

    /**
     * 子串替换
     */
    public String replaceAll(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceAll(replacement);
    }

    /**
     * 子串替换，只替换第一个
     */
    public String replaceFirst(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceFirst(replacement);
    }

    /**
     * 按 regex 切割成多个子串
     */
    public String[] split(String regex) {
        return split(regex, 0);
    }

    /**
     * 剪切指定范围的字符串
     */
    public String substring(int beginIndex) {
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        int subLen = value.length - beginIndex;
        if (subLen < 0) {
            throw new StringIndexOutOfBoundsException(subLen);
        }
        return (beginIndex == 0) ? this : new String(value, beginIndex, subLen);
    }

    public String substring(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        if (endIndex > value.length) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        int subLen = endIndex - beginIndex;
        if (subLen < 0) {
            throw new StringIndexOutOfBoundsException(subLen);
        }
        return ((beginIndex == 0) && (endIndex == value.length)) ? this
                : new String(value, beginIndex, subLen);
    }

    /**
     * 获取该String 对应的 char[]
     */
    public char[] toCharArray() {
        // Cannot use Arrays.copyOf because of class initialization order issues
        char result[] = new char[value.length];
        System.arraycopy(value, 0, result, 0, value.length);
        return result;
    }

    /**
     * 大小写转换
     */
    public String toLowerCase() {
        return toLowerCase(Locale.getDefault());
    }
    public String toUpperCase() {
        return toUpperCase(Locale.getDefault());
    }

    /**
     * str在本String对象中第一次出现的下标
     */
    public int indexOf(String str) {
        return indexOf(str, 0);
    }

    /**
     * str在本String对象中最后一次出现的下标
     */
    public int lastIndexOf(String str) {
        return lastIndexOf(str, value.length);
    }
}
```

## Thread

本来想看 ThreadLocal 的源码的，但发现其中最重要的 get/set 方法都是操纵的 Thread 类 中的 threadLocals 变量 (java.lang.ThreadLocal.ThreadLocalMap)，索性先来看一下 Thread 的源码吧，可以留意一下其中与 ThreadLocal 相关的属性，这样下次阅读 ThreadLocal 的核心 API 时，就能够轻易理解其原理咯。不多 BB，直接上硬菜。

实现多线程从本质上都是由 Thread 类 来完成的，其源码量很多，本次只看一些常见且重要的部分，源码和解析如下。

```java
public class Thread implements Runnable {
	/** 这里只看一些 常见的参数 */
	/** 线程名 */
	private volatile char name[];
	/** 优先级 */
	private int priority;
	/** 是否为守护线程 */
	private boolean daemon;
	/** 线程要执行的目标任务 */
	private Runnable target;
	/** 所属线程组 */
	private ThreadGroup group;
	/** 类加载器 */
	private ClassLoader contextClassLoader;
	/**
	 * ThreadLocal 能为线程设置线程私有变量 就是通过下面这个threadLocals变量完成的，
	 * ThreadLocal的get/set方法就是通过操作 各个线程的 threadLocals 变量实现的。
	 * 1、线程A持有一个 ThreadLocalMap 变量；
	 * 2、线程A调用一个类的 ThreadLocal变量 tlA 的 get/set方法；
	 * 3、tlA（ThreadLocal）的 get/set方法 获取当前线程A，调用 线程A 的 ThreadLocalMap变量 的get/put方法；
	 * 4、其它线程 调用 tlA（ThreadLocal）的 get/set方法 同理。
	 */
	ThreadLocal.ThreadLocalMap threadLocals;
	ThreadLocal.ThreadLocalMap inheritableThreadLocals;
	/** 线程栈的大小 */
	private long stackSize;
	/**
	 * Thread类定义了6个线程状态：New、Runnable、Blocked、Waiting、TimedWaiting、Terminated(终止)
	 * 实际上还会把 Runnable 再细分为 就绪(未抢到时间片) 和 运行中(抢到时间片)
	 */
	private volatile int threadStatus;
	/** 最小优先级 */
	public static final int MIN_PRIORITY = 1;
	/** 中等优先级 */
	public static final int NORM_PRIORITY = 5;
	/** 最大优先级 */
	public static final int MAX_PRIORITY = 10;

	/**
	 * 内部枚举类，用来描述线程状态，状态值有：
	 * NEW：          新建，还未调用start()方法；
	 * RUNNABLE：     运行，在java多线程模型中，就绪和运行都是运行状态；
	 * BLOCKED：      阻塞；
	 * WAITING：      等待，需要其他的线程来唤醒；
	 * TIMED_WAITING：超时等待，可以在指定的时间内自动醒来，如 sleep()方法；
	 * TERMINATED：   终止，线程执行完毕。
	 */
	public static final class State extends Enum {

		public static final State NEW;
		public static final State RUNNABLE;
		public static final State BLOCKED;
		public static final State WAITING;
		public static final State TIMED_WAITING;
		public static final State TERMINATED;
		private static final State VALUES[];

		static {
			NEW = new State("NEW", 0);
			RUNNABLE = new State("RUNNABLE", 1);
			BLOCKED = new State("BLOCKED", 2);
			WAITING = new State("WAITING", 3);
			TIMED_WAITING = new State("TIMED_WAITING", 4);
			TERMINATED = new State("TERMINATED", 5);
			VALUES = (new State[] { NEW, RUNNABLE, BLOCKED, WAITING, TIMED_WAITING, TERMINATED });
		}

		private State(String s, int i) {
			super(s, i);
		}
	}

	/**
	 * 一系列 构造方法 ------------------------------------------------------
	 * 可以看出来，其中都调用了init()方法，这也是一个约定俗成的规矩， 即，如果要在 new 时进行一些初始化操作，
	 * 那么请将初始化操作单独写在 init()方法中，然后在构造函数中调用该 init()方法
	 */
	public Thread() {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(null, null, (new StringBuilder()).append("Thread-").append(nextThreadNum()).toString(), 0L);
	}

	public Thread(Runnable runnable) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(null, runnable, (new StringBuilder()).append("Thread-").append(nextThreadNum()).toString(), 0L);
	}

	Thread(Runnable runnable, AccessControlContext accesscontrolcontext) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(null, runnable, (new StringBuilder()).append("Thread-").append(nextThreadNum()).toString(), 0L,
				accesscontrolcontext);
	}

	public Thread(ThreadGroup threadgroup, Runnable runnable) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(threadgroup, runnable, (new StringBuilder()).append("Thread-").append(nextThreadNum()).toString(), 0L);
	}

	public Thread(String s) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(null, null, s, 0L);
	}

	public Thread(ThreadGroup threadgroup, String s) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(threadgroup, null, s, 0L);
	}

	public Thread(Runnable runnable, String s) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(null, runnable, s, 0L);
	}

	public Thread(ThreadGroup threadgroup, Runnable runnable, String s) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(threadgroup, runnable, s, 0L);
	}

	public Thread(ThreadGroup threadgroup, Runnable runnable, String s, long l) {
		daemon = false;
		stillborn = false;
		threadLocals = null;
		inheritableThreadLocals = null;
		threadStatus = 0;
		blockerLock = new Object();
		init(threadgroup, runnable, s, l);
	}

	private void init(ThreadGroup threadgroup, Runnable runnable, String s, long l) {
		init(threadgroup, runnable, s, l, null);
	}

	/**
	 * 初始化线程
	 */
	private void init(ThreadGroup threadgroup, Runnable runnable, String name, long l,
			AccessControlContext accesscontrolcontext) {

		// 参数校验，线程name不能为null
		if (name == null)
			throw new NullPointerException("name cannot be null");
		this.name = name.toCharArray();
		// 当前线程就是该线程的父线程
		Thread parent = currentThread();
		SecurityManager securitymanager = System.getSecurityManager();
		if (threadgroup == null) {
			if (securitymanager != null)
				threadgroup = securitymanager.getThreadGroup();
			if (threadgroup == null)
				threadgroup = parent.getThreadGroup();
		}
		threadgroup.checkAccess();
		if (securitymanager != null && isCCLOverridden(getClass()))
			securitymanager.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
		threadgroup.addUnstarted();
		// 守护线程、优先级等设置为父线程的对应属性
		group = threadgroup;
		daemon = parent.isDaemon();
		priority = parent.getPriority();
		if (securitymanager == null || isCCLOverridden(parent.getClass()))
			contextClassLoader = parent.getContextClassLoader();
		else
			contextClassLoader = parent.contextClassLoader;
		inheritedAccessControlContext = accesscontrolcontext == null ? AccessController.getContext()
				: accesscontrolcontext;
		target = runnable;
		setPriority(priority);
		if (parent.inheritableThreadLocals != null)
			// 创建线程共享变量副本
			inheritableThreadLocals = ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
		stackSize = l;
		// 分配线程id
		tid = nextThreadID();
	}

	public synchronized void start() {
	    //假若当前线程初始化还未做好，不能start，0->NEW状态
	    if (threadStatus != 0)
	        throw new IllegalThreadStateException();

	    //通知group该线程即将启动，group的未启动线程数量减1
	    group.add(this);

	    boolean started = false;
	    try {
	    	// 调用native的start0()方法 启动线程，启动后执行run()方法
	        start0();
	        started = true;
	    } finally {
	        try {
	            //启动不成功，group设置当前线程启动失败
	            if (!started) {
	                group.threadStartFailed(this);
	            }
	        } catch (Throwable ignore) {

	        }
	    }
	}

	private native void start0();

	public void run() {
		if (target != null)
			target.run();
	}

	/**
	 * 请求终止线程。interrupt不会真正停止一个线程，它仅仅是给这个线程发了一个信号，
	 * 告诉它要结束了，具体要中断还是继续运行，将由被通知的线程自己处理
	 */
	public void interrupt() {
	    if (this != Thread.currentThread())
	        checkAccess();
	    synchronized (blockerLock) {
	        Interruptible b = blocker;
	        if (b != null) {
	            interrupt0();
	            b.interrupt(this);
	            return;
	        }
	    }
	    interrupt0();
	}

	private native void interrupt0();

	/**
	 * 线程main 调用了线程A的join方法，则 线程main 会被阻塞，直到线程A执行完毕
	 */
    public final void join() throws InterruptedException {
        join(0);
    }

	/**
	 * 实际上是利用 wait/notify机制 来实现的
	 */
    public final synchronized void join(long millis) throws InterruptedException {
        long base = System.currentTimeMillis();
        long now = 0;

        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
		// millis 为 0，所以走这个分支
        if (millis == 0) {
        	// 当前线程是否还在运行，还在运行 则main线程 进入等待状态，直到 A线程运行完毕，将其唤醒
            while (isAlive()) {
                wait(0);
            }
        } else {
            while (isAlive()) {
                long delay = millis - now;
                if (delay <= 0) {
                    break;
                }
                wait(delay);
                now = System.currentTimeMillis() - base;
            }
        }
    }

	/**
	 * 线程睡眠指定的时间，释放CPU资源，但不释放锁
	 */
	public static native void sleep(long millis) throws InterruptedException;

	/**
	 * 线程是否还在运行
	 */
	public final native boolean isAlive();
}
```

之前一直对线程状态 及 状态切换的概念模糊不清，现在通过源码中对线程状态的定义，我们可以画张图来重新回顾一下，以使我们对其有更加深刻的理解。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/ThreadStatusChange-ca8496.png)

## ThreadLocal

前面我们分析了 Thread 类的源码，有了前面的铺垫，通过源码 理解 ThreadLocal 的秘密就容易多了。

ThreadLocal 类 提供了 get/set 线程局部变量的实现，ThreadLocal 成员变量与正常的成员变量不同，每个线程都可以通过 ThreadLocal 成员变量 get/set 自己的专属值。ThreadLocal 实例 通常是类中的私有静态变量，常用于将状态与线程关联，例如：用户 ID 或事务 ID。

tips：在类中定义 ThreadLocal 变量时，一般在定义时就进行实例化！

```java
public class ThreadLocal<T> {

    /**
     * ThreadLocal能为每个 Thread线程 绑定一个专属值的奥秘就是：
     * 每个Thread对象都持有一个 ThreadLocalMap类型的成员变量，其key为ThreadLocal对象，
     * value为绑定的值，所以每个线程调用 ThreadLocal对象 的set(T value)方法时，都会将
     * 该ThreadLocal对象和绑定的值 以键值对的形式存入当前线程，这样，同一个ThreadLocal对象
     * 就可以为每个线程绑定一个专属值咯。
     * 每个线程调用 ThreadLocal对象的get()方法时，就可以根据 当前ThreadLocal对象 get到 绑定的值。
     */
    public void set(T value) {
    	// 获取当前线程
        Thread t = Thread.currentThread();
        // 获取当前线程对象中持有的 ThreadLocalMap类型的成员变量
        // ThreadLocalMap，看名字也知道它是一个 Map类型的 类
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

    ThreadLocalMap getMap(Thread t) {
    	// 经过前面对 Thread类 源码的分析，可以知道，Thread类中有一个 ThreadLocalMap 类型的
    	// threadLocals变量
        return t.threadLocals;
    }

    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
        	// 通过当前 ThreadLocal对象，获取绑定的值
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }

     public void remove() {
     	 // 获取当前线程的ThreadLocalMap成员变量，不为空就将当前 ThreadLocal对象
     	 // 对应的 键值对 remove掉
         ThreadLocalMap m = getMap(Thread.currentThread());
         if (m != null)
             m.remove(this);
     }

    /**
     * 与大部分 Map 的实现相同，底层也是使用 动态数组来保存 键值对Entry，也有rehash、resize等
     * 操作
     */
    static class ThreadLocalMap {

        /**
         * 存储键值对，key 为 ThreadLocal对象，value 为 与该ThreadLocal对象绑定的值
         * Entry的key是对ThreadLocal的弱引用，当抛弃掉ThreadLocal对象时，垃圾收集器会
         * 忽略这个key的引用而清理掉ThreadLocal对象，防止了内存泄漏
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

		// 看过 HashMap 或 ConcurrentHashMap 源码的同学 一定下面对这些代码很眼熟
        /**
         * 数组初始容量
         */
        private static final int INITIAL_CAPACITY = 16;

        /**
         * Entry数组，用于存储 <ThreadLocal<?> k, Object v>键值对
         */
        private Entry[] table;

        /**
         * Entry元素数量
         */
        private int size = 0;

        /**
         * 类似于 HashMap 扩容因子机制
         */
        private int threshold; // Default to 0
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        /**
         * 系列构造方法
         */
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            table = new Entry[INITIAL_CAPACITY];
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);
            table[i] = new Entry(firstKey, firstValue);
            size = 1;
            setThreshold(INITIAL_CAPACITY);
        }

        private ThreadLocalMap(ThreadLocalMap parentMap) {
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;
            setThreshold(len);
            table = new Entry[len];

            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * 根据 ThreadLocal对象 获取其对应的 Entry实例
         */
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];
            if (e != null && e.get() == key)
                return e;
            else
                return getEntryAfterMiss(key, i, e);
        }

        /**
         * 常规Map实现类 的set()方法，只不过这里的 key被规定为 ThreadLocal类型
         */
        private void set(ThreadLocal<?> key, Object value) {

            Entry[] tab = table;
            int len = tab.length;
            // 根据哈希码和数组长度求元素放置的位置，如果该位置有其它元素，就依次尝试往后放
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();
				// 如果key相等，覆盖value
                if (k == key) {
                    e.value = value;
                    return;
                }
				// 如果key为null，用新key、value覆盖，同时清理历史key=null的陈旧数据
                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            tab[i] = new Entry(key, value);
            int sz = ++size;
            // 若超过阀值，则rehash
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        private void remove(ThreadLocal<?> key) {
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);
            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                if (e.get() == key) {
                    e.clear();
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * 调整当前table的容量。首先扫描整个容器，以删除过时的条目，如果这不能充分缩小表的大小，
         * 将进行扩容操作
         */
        private void rehash() {
        	// 扫描整个容器，删除过时的条目
            expungeStaleEntries();

            // 若未能充分缩小表的大小，则进行扩容操作
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * 扩容为原容量的两倍
         */
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;
			// 遍历Entry[]数组
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    // 如果key=null，把value也置null,有助于GC回收对象
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }
			// 设置新的阈值
            setThreshold(newLen);
            size = count;
            table = newTab;
        }
    }
}
```

简单画个图总结一下 ThreadLocal 的原理，如下。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/ThreadLocal%E5%8E%9F%E7%90%86-0e6c45.png)

最后强调一下 ThreadLocal 的使用注意事项：

1. ThreadLocal 不是用来解决线程安全问题的，多线程不共享，不存在竞争！其目的是使线程能够使用本地变量。

2. 项目如果使用了线程池，那么线程回收后 ThreadLocal 变量要 remove 掉，否则线程池回收线程后，变量还在内存中，可能会带来意想不到的后果！例如 Tomcat 容器的线程池，可以在拦截器中处理：继承 HandlerInterceptorAdapter，然后复写 afterCompletion()方法，remove 掉变量！！！

# Collection

# 一文直接带你吃透 ArrayList

> ArrayList 是日常开发中相当常见、面试也相当常考的一种 JDK 集合类，了解并熟悉、甚至能实现一个 ArrayList 对面试、提升自己编码功底大有益处。



## 一、写给小白 ArrayList 简单使用技巧

这部分是 ArrayList 的简单使用技巧，主要是介绍 ArrayList 的几个常见方法。

```java
/**
 * 编写一个ArrayList的简单实用demo
 * ArrayList 的常见方法包括：
 *		add(element):添加元素
 *		get(index):获取下标元素
 *		remove(index):移除下标对应元素
 *		set(index,element):将index处的元素修改为element
 */
public class arrayList {
    public static void main(String[] args) {
        // 创建 ArrayList 的对象
        ArrayList al = new ArrayList();
        // 添加元素
        al.add("finky");
        // 构造随机数并进行添加
        Random rnd = new Random();
        for (int i = 0; i < 20; i++) {
            al.add(rnd.nextInt(1000));
        }
        // 取出ArrayList里的元素进行打印
        for (int i = 0; i < al.size(); i++) {
            System.out.print(al.get(i) + "  ");
        }
        // 修改0号index成的元素为doocs
        System.out.println();
        al.set(0, "doocs");
        System.out.println(al.get(0));
        // 移除“doocs”元素
        al.remove(0);
        System.out.println(al.get(0));
    }
}
```

```java
// 这是上面打印后的demo，可以看到第0处下标元素先是修改成了doocs，进行移除后，第0处下标元素变成了912
finky  912  922  284  305  675  565  159  109  73  298  491  920  296  397  358  145  610  190  839  845
doocs
912
```

## 二、ArrayList 的源码分析

我们来看看 ArrayList 的源码：

#### 1、来看看 ArrayList 的初始化：

```java
// ArrayList 初始化时默认大小为10
private static final int DEFAULT_CAPACITY = 10;

// 直接初始化的话一个空数组
private static final Object[] EMPTY_ELEMENTDATA = {};

// 初始化ArrayList,传入初始化时的大小
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
// 如果不传入大小的话就默认大小是10，那么这里就有一个问题：我们上面插入的元素超过了10，继续插入元素就会进行拷贝扩容，性能不是特别高。所以我们一般情况下初始化时给定一个比较靠谱的数组大小，避免到时候导致元素不断拷贝
public ArrayList() {
    this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}

```

总结一下 ArrayList 初始化：我们创建 ArrayList 对象时，如果没有传入对应的大小，就会默认创建一个元素大小为 10 的数组，下次插入元素超过 10 时，会进行数组的拷贝扩容，这样性能消耗太高，所以建议就是在初始化时给定一个不要太小的容量大小。==

#### 2、 ArrayList 的 add 方法：

先上`add` 方法的代码：

```java
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}

public void add(int index, E element) {
    rangeCheckForAdd(index);
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    System.arraycopy(elementData, index, elementData, index + 1,
                        size - index);
    elementData[index] = element;
    size++;
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

private void rangeCheck(int index) {
    if (index < 0 || index >= this.size)
        throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
}
```

![arraylist添加集合的方法](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/arraylist%E7%9A%84add%E6%96%B9%E6%B3%95-e79c95.png)

先判断当前数组元素是否满了，如果塞满了就会进行数组扩容，随后进行数组拷贝。

再然后插入元素，同时对应的 index++。

#### 3、瞧瞧 ArrayList 的 set 方法：

```java
public E set(int index, E element) {
    rangeCheck(index);
    E oldValue = elementData(index);
    elementData[index] = element;
    return oldValue;
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
```

1、先进行 index 判断是否越界，如果没有越界的话获取原来的旧的值

2、进行替换并返回该位置原来的旧的值

#### 4、ArrayList 的 get 方法：

```java
public E get(int index) {
    rangeCheck(index);
    checkForComodification();
    return ArrayList.this.elementData(offset + index);
}
```

进行 index 是否越界的判断，然后去取对应下标的值。

#### 5、ArrayList 的 remove 方法：

```java
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

public E remove(int index) {
    // 进行index是否越界的判断
    rangeCheck(index);
    checkForComodification();
    E result = parent.remove(parentOffset + index);
    this.modCount = parent.modCount;
    this.size--;
    return result;
}

public E remove(int index) {
    rangeCheck(index);
    modCount++;
    E oldValue = elementData(index);
    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                            numMoved);
    elementData[--size] = null;
    return oldValue;
}
```

![arrayList删除元素的过程.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/arrayList%E5%88%A0%E9%99%A4%E5%85%83%E7%B4%A0%E7%9A%84%E8%BF%87%E7%A8%8B-eaa40b.png)

1、先进行下标是否越界的判断，获取 index 处的元素值（这是要删除的值）

2、然后进行元素拷贝，把 index 后面的元素往前拷贝

#### 6、关于 ArrayList 动态扩容和数组拷贝：

```java
private void ensureCapacityInternal(int minCapacity) {
    ensureExplicitCapacity(calculateCapacity(elementData, minCapacity));
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;
    // 扩容的代码：这里做了位运算，相当于数组扩容了1.5倍
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // 随后进行元素拷贝
    elementData = Arrays.copyOf(elementData, newCapacity);
}

```

现在假定场景：arraylist 中已经有 10 个元素类，要放第 11 个元素。

此时进行容量检测，出现问题：空间大小不够。

解决方法：此时进行数组扩容右位移 1（**相当于总容量多加 1.5 倍**）扩容，老的大小+老大小的一半，进行元素拷贝

## 三、来仿照 JDK 源码写一个自己的 ArrayList 把

```java
public class OwnArrayList<E> {
    private E data[];
    private int size;

    public OwnArrayList(int capacity) {
        data = (E[]) new Object[capacity];
        size = 0;
    }
	//   初始化是默认设置大小为20
    public OwnArrayList() {
        this(20);
    }

    //    获取数组容量
    public int getCapacity() {
        return data.length;
    }

    //    获取数组元素个数
    public int getSize() {
        return size;
    }

    //    判断数组是否为空
    public boolean isEmpity() {
        return size == 0;
    }

    // 获取index索引位置的元素
    public E get(int index) {
        if (index < 0 || index >= size)
            throw new IllegalArgumentException("add failed,the index should >= 0 or <= size");
        return data[index];
    }

    // 修改index索引位置的元素为e
    public void set(int index, E e) {
        if (index < 0 || index >= size)
            throw new IllegalArgumentException("add failed,the index should >= 0 or <= size");
        data[index] = e;
    }

    //    在数组中间插入一个元素
    public void add(int index, E element) {
        if (size == data.length) {
            throw new IllegalArgumentException("AddLast failed,array has already full");
        }
        if (index < 0 || index > size) {
            throw new IllegalArgumentException("add failed,the index should >= 0 or <= size");
        }
        for (int i = size - 1; i >= index; i--) {
            data[i + 1] = data[i];
        }
        data[index] = element;
        size++;
    }

    //    向数组元素末尾添加一个元素
    public void addLast(E element) {
        add(size,element);
    }

    //    在数组头部插入一个元素
    public void addFirst(E element) {
        add(0, element);
    }

    // 判断是否含有元素
    public boolean contains(E e) {
        for (int i = 0; i < size; i++)
            if (data[i] == e)
                return true;
        return false;
    }

    // 查找元素e的位置
    public int find(E e) {
        for (int i = 0; i < size; i++) {
            if (data[i] == e) {
                return i;
            }
        }
        return -1;
    }

    // 删除index位置的元素
    public E remove(int index) {
        if (index < 0 || index > size) {
            throw new IllegalArgumentException("index should be 0 to size");
        }
        E remove_element = data[index];
        for (int i = index + 1; i < size; i++) {
            data[i - 1] = data[i];
        }
        size--;
        return remove_element;
    }

    // 删除末尾元素
    // 注意：这是逻辑删除，但是size的大小已经做了相应的减少，所以从实际意义上我们外界并不能访问到末尾元素的值
    public E removelast() {
        return remove(size - 1);
    }

    // 删除开头元素
    public E removeFirst() {
        return remove(0);
    }

    // 将数组空间的容量变成newCapacity大小
    private void resize(int newCapacity) {
        newCapacity = getCapacity()*2;
        E[] newData = (E[]) new Object[newCapacity];
        for (int i = 0; i < size; i++)
            newData[i] = data[i];
        data = newData;
    }
}
```

## 四、面试时关于 ArrayList 要说的事

如果有人问你 ArrayList 知多少，我觉得可以从这几个方面出发：

ArrayList 的底层是基于数组进行的，进行随机位置的插入和删除、以及扩容时性能很差，但进行随机的读和取时速度却很快。

接着可以从源码的角度分析 add、remove、set、get、数组扩容拷贝的过程场景。

最后也是特别重要的一点，就是要积极掌握主动性，延伸出 LinkedList 的特点、源码、两者间的对比等。

注：当需要动态数组时我们通常使用 ArrayList 而不是使用类似的 vector，这里有一点说明一下，就是尽管 Vector 的方法都是线程安全的，但其在单线程下需要花费的时间更多，而 ArrayList 尽管不是线程安全的，但其花费的时间很少。



作为工作中最重要、最常用的容器之一，当然还是要自己动手写一篇 HashMap 的源码解析来加深对其的印象咯，而且它的设计与实现 也有很多值得学习的地方。



##   HashMap源码赏析

JDK1.8 的 HashMap 底层使用的是 动态数组，数组中元素存放的是 链表或红黑树。核心源码如下。

```java
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>,
		Cloneable, Serializable {

    /**
     * 初始化容量，这里的 位运算 就不多解释咯，可以自行度娘
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * 最大容量
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 扩容因子，使用的容量达到 当前容量的 75% 就扩容
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 当前 HashMap 所能容纳键值对数量的最大值，超过这个值，则需扩容
     */
    int threshold;

    /**
     * 已使用的容量
     */
    transient int size;

    /**
     * Node数组，实际存放 键值对 的地方
     */
    transient Node<K,V>[] table;

    /**
     * 链表转红黑树的阈值，链表长度达到此值，会进化成红黑树
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 系列构造方法，推荐在初始化时根据实际情况设置好初始容量，用好了可以显著减少 resize，提升效率
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    public HashMap(Map<? extends K, ? extends V> m) {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        putMapEntries(m, false);
    }

    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        // 初始化桶数组 table，table 被延迟到插入新数据时再进行初始化
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        // 如果桶中不包含键值对节点引用，则将新键值对节点的引用存入桶中即可
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
            Node<K,V> e; K k;
            // 如果键的值以及节点 hash 等于链表中的第一个键值对节点时，则将 e 指向该键值对
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
			// 如果桶中的引用类型为 TreeNode，则调用红黑树的插入方法
	        else if (p instanceof TreeNode)
	            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
	        else {
	            // 对链表进行遍历，并统计链表长度
	            for (int binCount = 0; ; ++binCount) {
	                // 链表中不包含要插入的键值对节点时，则将该节点接在链表的最后
	                // ！！！ JDK1.7中 新增的Node节点采用头插入，而JDK1.8中改成了尾插入 ！！！
	                if ((e = p.next) == null) {
	                    p.next = newNode(hash, key, value, null);
	                    // 如果链表长度达到阈值，则进化成红黑树
	                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
	                        treeifyBin(tab, hash);
	                    break;
	                }

	                // 条件为 true，表示当前链表包含要插入的键值对，终止遍历
	                if (e.hash == hash &&
	                    ((k = e.key) == key || (key != null && key.equals(k))))
	                    break;
	                p = e;
	            }
	        }

	        // 判断要插入的键值对是否存在 HashMap 中
	        if (e != null) { // existing mapping for key
	            V oldValue = e.value;
	            // onlyIfAbsent 表示是否仅在 oldValue 为 null 的情况下更新键值对的值
	            if (!onlyIfAbsent || oldValue == null)
	                e.value = value;
	            afterNodeAccess(e);
	            return oldValue;
	        }
	    }
	    ++modCount;
	    // 键值对数量超过阈值时，则进行扩容
	    if (++size > threshold)
	        resize();
	    afterNodeInsertion(evict);
	    return null;
    }

    /**
     * 扩容为原容量的两倍，并将存在的元素 放到新的数组上
     */
	final Node<K,V>[] resize() {
	    Node<K,V>[] oldTab = table;
	    int oldCap = (oldTab == null) ? 0 : oldTab.length;
	    int oldThr = threshold;
	    int newCap, newThr = 0;
	    // 如果 table 不为空，表明已经初始化过了
	    if (oldCap > 0) {
	        // 当 table 容量超过容量最大值，则不再扩容
	        if (oldCap >= MAXIMUM_CAPACITY) {
	            threshold = Integer.MAX_VALUE;
	            return oldTab;
	        }
	        // 按旧容量和阈值的2倍计算新容量和阈值的大小
	        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
	                 oldCap >= DEFAULT_INITIAL_CAPACITY)
	            newThr = oldThr << 1; // double threshold
	    } else if (oldThr > 0) // initial capacity was placed in threshold
	        // 初始化时，将 threshold 的值赋值给 newCap，
	        // HashMap 使用 threshold 变量暂时保存 initialCapacity 参数的值
	        newCap = oldThr;
	    else {               // zero initial threshold signifies using defaults
	        // 调用无参构造方法时，桶数组容量为默认容量，
	        // 阈值为默认容量与默认负载因子乘积
	        newCap = DEFAULT_INITIAL_CAPACITY;
	        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
	    }

	    // newThr 为 0 时，按阈值计算公式进行计算
	    if (newThr == 0) {
	        float ft = (float)newCap * loadFactor;
	        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
	                  (int)ft : Integer.MAX_VALUE);
	    }
	    threshold = newThr;
	    // 创建新的桶数组，桶数组的初始化也是在这里完成的
	    Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
	    table = newTab;
	    if (oldTab != null) {
	        // 如果旧的桶数组不为空，则遍历桶数组，并将键值对映射到新的桶数组中
	        for (int j = 0; j < oldCap; ++j) {
	            Node<K,V> e;
	            if ((e = oldTab[j]) != null) {
	                oldTab[j] = null;
	                if (e.next == null)
	                    newTab[e.hash & (newCap - 1)] = e;
	                else if (e instanceof TreeNode)
	                    // 重新映射时，需要对红黑树进行拆分
	                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
	                else { // preserve order
	                    Node<K,V> loHead = null, loTail = null;
	                    Node<K,V> hiHead = null, hiTail = null;
	                    Node<K,V> next;
	                    // 遍历链表，并将链表节点按原顺序进行分组
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
	                    // 将分组后的链表映射到新桶中
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

    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * 根据 hash 和 key 获取相应的 Node节点
     */
	final Node<K,V> getNode(int hash, Object key) {
	    Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
	    // 1. 定位键值对所在桶的位置，如果该位置有元素，则获取第一个元素
	    if ((tab = table) != null && (n = tab.length) > 0 &&
	        (first = tab[(n - 1) & hash]) != null) {
	        // 如果hash和key都与 第一个元素相同，则第一个元素就是我们要获取的，直接返回
	        if (first.hash == hash &&
	        		((k = first.key) == key || (key != null && key.equals(k))))
	            return first;
	        if ((e = first.next) != null) {
	            // 2. 如果 first 是 TreeNode 类型，则调用黑红树查找方法
	            if (first instanceof TreeNode)
	                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
	            // 3. 对链表进行查找
	            do {
	                if (e.hash == hash &&
	                    ((k = e.key) == key || (key != null && key.equals(k))))
	                    return e;
	            } while ((e = e.next) != null);
	        }
	    }
	    return null;
	}

    /**
     * 还记HashMap底层的动态数组的定义吗 transient Node<K,V>[] table
     * 这里很明显是一个单向链表结构
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;

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

    /**
     * JDK8 加入的 红黑树TreeNode内部类，红黑树的方法比较复杂，这里只展示一些重要的
     * 属性结构代码
     */
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion
        // 颜色，true红，false黑
        boolean red;
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }
    }
}
```

源码部分 结合注释还是很容易看懂的，比较复杂的是红黑树这种数据结构，以及红黑树与链表之间的相互转换。下面我们回顾下这个数据结构。

## 红黑树

红黑树是一种自平衡的二叉查找树，比普通的二叉查找树效率更高，它可在 O(logN) 时间内完成查找、增加、删除等操作。

普通的二叉查找树在极端情况下可退化成链表，导致 增、删、查 效率低下。红黑树通过定义一些性质，将任意节点的左右子树高度差控制在规定范围内，以达到平衡状态，红黑树的性质定义如下。

1. 节点是红色或黑色。
2. 根是黑色。
3. 所有叶子都是黑色（叶子是 NIL 节点）。
4. 每个红色节点必须有两个黑色的子节点。（从每个叶子到根的所有路径上不能有两个连续的红色节点。）
5. 从任一节点到其每个叶子的所有简单路径都包含相同数目的黑色节点。

红黑树的操作和其他树一样，包括查找、插入、删除等，其查找过程和二叉查找树一样简单，但插入和删除操作要复杂的多，这也是其 为保持平衡性 不会退化成链表 所付出的代价。红黑树为保持平衡性 所进行的操作主要有 旋转（左旋、右旋）和变色。

红黑树的实现 确实比较复杂，光是理解其 插入、删除 的操作原理 就蛮费劲，所以这里先挖个坑，后面单独用一篇博文来分析 HashMap 的 内部类 TreeNode 对红黑树数据结构的实现。

## ConcurrentHashMap

HashMap 源码中主要了解其核心源码及实现逻辑。ConcurrentHashMap 就不再重复那些数据结构相关的内容咯，这里重点看一下它的并发安全实现。源码如下。

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>,
		Serializable {

    /* --------- 常量及成员变量的设计 几乎与HashMap相差无几 -------- */

    /**
     * 最大容量
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * 默认初始容量
     */
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * 单个数组最大容量
     */
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * 默认并发等级，也就分成多少个单独上锁的区域
     */
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 扩容因子
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     *
     */
    transient volatile Node<K,V>[] table;

    /**
     *
     */
    private transient volatile Node<K,V>[] nextTable;

    /* --------- 系列构造方法，依然推荐在初始化时根据实际情况设置好初始容量 -------- */
    public ConcurrentHashMap() {
    }

    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                   MAXIMUM_CAPACITY :
                   tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
            MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }

    /**
     * ConcurrentHashMap 的核心就在于其put元素时 利用synchronized局部锁 和
     * CAS乐观锁机制 大大提升了本集合的并发能力，比JDK7的分段锁性能更强
     */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

	/**
	 * 当前指定数组位置无元素时，使用CAS操作 将 Node键值对 放入对应的数组下标。
	 * 出现hash冲突，则用synchronized局部锁锁住，若当前hash对应的节点是链表的头节点，遍历链表，
	 * 若找到对应的node节点，则修改node节点的val，否则在链表末尾添加node节点；倘若当前节点是
	 * 红黑树的根节点，在树结构上遍历元素，更新或增加节点
	 */
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            	// 注意！这是一个CAS的方法，将新节点放入指定位置，不用加锁阻塞线程
            	// 也能保证并发安全
                if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            // 当前Map在扩容，先协助扩容，在更新值
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else { // hash冲突
                V oldVal = null;
                // 局部锁，有效减少锁竞争的发生
                synchronized (f) { // f 是 链表头节点/红黑树根节点
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                // 若节点已经存在，修改该节点的值
                                if (e.hash == hash && ((ek = e.key) == key ||
                                    	 (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                // 节点不存在，添加到链表末尾
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        // 如果该节点是 红黑树节点
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                // 链表节点超过了8，链表转为红黑树
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        // 统计节点个数，检查是否需要resize
        addCount(1L, binCount);
        return null;
    }
}
```

**与 JDK1.7 在同步机制上的区别** 总结如下：  
JDK1.7 使用的是分段锁机制，其内部类 Segment 继承了 ReentrantLock，将 容器内的数组划分成多段区域，每个区域对应一把锁，相比于 HashTable 确实提升了不少并发能力，但在数据量庞大的情况下，性能依然不容乐观，只能通过不断的增加锁来维持并发性能。而 JDK1.8 则使用了 CAS 乐观锁 + synchronized 局部锁 处理并发问题，锁粒度更细，即使数据量很大也能保证良好的并发性。

## HashSet

HashSet 本身并没有什么特别的东西，它提供的所有集合核心功能，都是基于 HashMap 来实现的。如果了解 HashMap 源码的实现，HashSet 源码看起来跟玩一样。我的博客中有专门分析 HashMap 源码的文章，不熟悉的请自行翻阅。

HashSet 的特点如下：

- 内部使用 HashMap 的 key 存储元素，以此来保证**元素不重复**；
- HashSet 是无序的，因为 HashMap 的 key 是**无序**的；
- HashSet 中允许有一个 null 元素，因为 HashMap 允许 key 为 null；
- HashSet 是**非线程安全**的。

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    static final long serialVersionUID = -5024744406713321676L;

    // 基于HashMap实现
    private transient HashMap<E,Object> map;

    // 只需要用到HashMap中key唯一的特性，所以value全部使用同一个 Object实例填充，节省内存空间
    private static final Object PRESENT = new Object();

    /**
     * 实例化 HashSet 的时候，初始化内部的 HashMap
     */
    public HashSet() {
        map = new HashMap<>();
    }

    /**
     * 根据一个集合实例，实例化 HashSet
     */
    public HashSet(Collection<? extends E> c) {
        map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }

    /**
     * 根据初始容量和扩容因子实例化 HashSet，减少rehash频率，提升性能，原理与HashMap相同
     */
    public HashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 同上
     */
    public HashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    HashSet(int initialCapacity, float loadFactor, boolean dummy) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * 返回迭代器，用于迭代
     * 下面所有的功能都是基于 HashMap 来实现的
     */
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * 元素个数
     */
    public int size() {
        return map.size();
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 是否包含给定元素
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * 添加元素，如果 Set集合中未包含该元素，返回true
     */
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }

    /**
     * 删除元素，如果Set集合包含该元素，返回true
     */
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    /**
     * 清除元素
     */
    public void clear() {
        map.clear();
    }

    /**
     * 浅克隆
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            HashSet<E> newSet = (HashSet<E>) super.clone();
            newSet.map = (HashMap<E, Object>) map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }
}
```

## LinkedHashMap

> HashMap 大家都清楚，底层是 数组 + (链表 / 红黑树)，**元素是无序的**，而 LinkedHashMap 则比 HashMap 多了这一个功能，并且，LinkedHashMap 的有序可以按两种顺序排列，一种是按照插入的顺序，一种是按照访问的顺序（初始化 LinkedHashMap 对象时设置 accessOrder 参数为 true），而其内部是靠 建立一个双向链表 来维护这个顺序的，在每次插入、删除后，都会调用一个函数来进行 双向链表的维护，这也是实现 LRU Cache 功能的基础。

先说几个比较重要的结论，大家可以根据这些结论从后面的源码解析中 得到证据。

1. LinkedHashMap 继承了 HashMap，所以和 HashMap 的底层数据结构是一样的，都是数组+链表+红黑树，扩容机制也一样；
2. LinkedHashMap 是通过双向链表来维护数据的，与 HashMap 的拉链式存储不一样；
3. LinkedHashMap 存储顺序与添加顺序是一样得，同时可以根据 accessOrder 参数 来决定是否在访问时移动元素，以实现 LRU 功能。

```java
public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V> {

    /**
     * 在 HashMap.Node节点 的基础上增加了 “前继节点” 和 “后继节点” 这种双向链表的功能特性
     */
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }

    /**
     * 记录这个 LinkedHashMap容器的 头节点
     */
    transient LinkedHashMap.Entry<K,V> head;

    /**
     * 记录这个 LinkedHashMap容器的 尾节点
     */
    transient LinkedHashMap.Entry<K,V> tail;

    /**
     * 是否根据访问 进行排序，true为是，可通过构造方法进行设置
     */
    final boolean accessOrder;

    // 下面是一些私有的内部公用方法

    // 将元素连接到链表尾部
    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }

    // apply src's links to dst
    private void transferLinks(LinkedHashMap.Entry<K,V> src, LinkedHashMap.Entry<K,V> dst) {
        LinkedHashMap.Entry<K,V> b = dst.before = src.before;
        LinkedHashMap.Entry<K,V> a = dst.after = src.after;
        if (b == null)
            head = dst;
        else
            b.after = dst;
        if (a == null)
            tail = dst;
        else
            a.before = dst;
    }

    // 下面是一些 重写的 HashMap 的 hook methods，其中 afterNodeInsertion、afterNodeRemoval
    // afterNodeAccess及方法，在每次插入、删除、访问后，都会回调 用来维护双向链表

    void reinitialize() {
        super.reinitialize();
        head = tail = null;
    }

    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<K,V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
        LinkedHashMap.Entry<K,V> t =
            new LinkedHashMap.Entry<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        TreeNode<K,V> p = new TreeNode<K,V>(hash, key, value, next);
        linkNodeLast(p);
        return p;
    }

    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        LinkedHashMap.Entry<K,V> q = (LinkedHashMap.Entry<K,V>)p;
        TreeNode<K,V> t = new TreeNode<K,V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    // 在删除元素之后，将元素从双向链表中删除
    void afterNodeRemoval(Node<K,V> e) { // unlink
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }

    // 可用于删除最老的元素
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
    // 是否删除 最近最少使用的元素
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return false;
    }

    // 在访问元素之后，将该元素放到双向链表的尾巴处
    void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMap.Entry<K,V> last;
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }

    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after) {
            s.writeObject(e.key);
            s.writeObject(e.value);
        }
    }

    /**
     * 跟 HashMap 的构造方法没啥区别，初始容量、扩容因子 用以减少resize和rehash，提升容器整体性能
     */
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }

    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }

    /**
     * 注意！accessOrder参数默认为false，如果想使用 LRU机制，记得设为 true
     */
    public LinkedHashMap() {
        super();
        accessOrder = false;
    }

    public LinkedHashMap(Map<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }

    /**
     * 使用这个构造方法 设置accessOrder
     */
    public LinkedHashMap(int initialCapacity, float loadFactor, boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }

    /**
     * 是否包含指定元素
     */
    public boolean containsValue(Object value) {
        for (LinkedHashMap.Entry<K,V> e = head; e != null; e = e.after) {
            V v = e.value;
            if (v == value || (value != null && value.equals(v)))
                return true;
        }
        return false;
    }

    /**
     * 获取指定key对应的value，如果accessOrder为true，会回调afterNodeAccess方法
     * 将元素放到队尾
     */
    public V get(Object key) {
        Node<K,V> e;
        if ((e = getNode(hash(key), key)) == null)
            return null;
        if (accessOrder)
            afterNodeAccess(e);
        return e.value;
    }

    /**
     * 根据 key 获取对应的 value，如果key不存在，则返回给定的默认值 defaultValue
     */
    public V getOrDefault(Object key, V defaultValue) {
       Node<K,V> e;
       if ((e = getNode(hash(key), key)) == null)
           return defaultValue;
       if (accessOrder)
           afterNodeAccess(e);
       return e.value;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        super.clear();
        head = tail = null;
    }

    /**
     * 获取key的set集合
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new LinkedKeySet();
            keySet = ks;
        }
        return ks;
    }

    /**
     * 返回 键值对 的Set集合
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;
        return (es = entrySet) == null ? (entrySet = new LinkedEntrySet()) : es;
    }
}
```

# 并发编程-concurrentCoding

## JUC并发包UML全量类图

利用 IDEA 整理类图还是蛮不错的，虽然这个功能 BUG 很多。下图是 J.U.C 并发包中所有类组成的类图，源码看多了 再去整理这个图，感觉还是很爽的。

根据功能，主要划分了六个部分，其中比较重要的是：线程池及其相关类、并发容器、AQS 与锁与同步工具类、原子类。图可能整理的不够细致，但看着这些类，回想一下其中的源码实现，感觉能侃一天。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/JUC%E5%85%A8%E9%87%8FUML%E5%9C%B0%E5%9B%BE-77e1c3.png)

##  Executor线程池核心组件图解

看源码之前，先了解一下该组件 最主要的几个 接口、抽象类和实现类的结构关系。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E7%BA%BF%E7%A8%8B%E6%B1%A0%E7%BB%84%E4%BB%B6%E7%B1%BB%E5%9B%BE-98b982.png)

该组件中，Executor 和 ExecutorService 接口 定义了线程池最核心的几个方法，提交任务 submit
()、关闭线程池 shutdown()。抽象类 AbstractExecutorService 主要对公共行为 submit()系列方法进行了实现，这些 submit()方法 的实现使用了 模板方法模式，其中调用的 execute()方法 是未实现的 来自 Executor 接口 的方法。实现类 ThreadPoolExecutor 则对线程池进行了具体而复杂的实现。

另外还有一个常见的工具类 Executors，里面为开发者封装了一些可以直接拿来用的线程池。

## 源码赏析

话不多说，直接上源码。（这里只看最主要的代码部分）

### Executor 和 ExecutorService 接口

```java
public interface Executor {

    /**
     * 在将来的某个时间执行给定的 Runnable。该 Runnable 可以在新线程、池线程或调用线程中执行。
     */
    void execute(Runnable command);
}

public interface ExecutorService extends Executor {

    /**
     * 优雅关闭，该关闭会继续执行完以前提交的任务，但不再接受新任务。
     */
    void shutdown();

    /**
     * 提交一个有返回值的任务，并返回该任务的 未来执行完成后的结果。
     * Future的 get()方法 将在成功完成后返回任务的结果。
     */
    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);
}
```

### AbstractExecutorService 抽象类

```java
/**
 * 该抽象类最主要的内容就是，实现了 ExecutorService 中的 submit()系列方法
 */
public abstract class AbstractExecutorService implements ExecutorService {

    /**
     * 提交任务 进行执行，返回获取未来结果的 Future对象。
     * 这里使用了 “模板方法模式”，execute()方法来自 Executor接口，该抽象类中并未进行实现，
     * 而是交由子类具体实现。
     */
    public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<Void> ftask = newTaskFor(task, null);
        execute(ftask);
        return ftask;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task, result);
        execute(ftask);
        return ftask;
    }

    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }
}
```

### ThreadPoolExecutor

```java
public class ThreadPoolExecutor extends AbstractExecutorService {

    /**
     * **************
     * ** 主要属性 **
     * **************
     */

	/** 阻塞队列 */
    private final BlockingQueue<Runnable> workQueue;

    /** 用于创建线程的 线程工厂 */
    private volatile ThreadFactory threadFactory;

    /** 核心线程数 */
    private volatile int corePoolSize;

    /** 最大线程数 */
    private volatile int maximumPoolSize;


    /**
     * **************
     * ** 构造方法 **
     * **************
     */

    /** 最后都使用了最后一个构造方法的实现 */
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * **************
     * ** 主要实现 **
     * **************
     */

	/** 执行 Runnable任务 */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
        /*
         * 分三步进行：
         *
         * 1、如果运行的线程少于 corePoolSize，尝试开启一个新的线程；否则尝试进入工作队列
         *
         * 2. 如果工作队列没满，则进入工作队列；否则 判断是否超出最大线程数
         *
         * 3. 如果未超出最大线程数，则尝试开启一个新的线程；否则 按饱和策略处理无法执行的任务
         */
        int c = ctl.get();
        if (workerCountOf(c) < corePoolSize) {
            if (addWorker(command, true))
                return;
            c = ctl.get();
        }
        if (isRunning(c) && workQueue.offer(command)) {
            int recheck = ctl.get();
            if (! isRunning(recheck) && remove(command))
                reject(command);
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        else if (!addWorker(command, false))
            reject(command);
    }

    /**
     * 优雅关闭，在其中执行以前提交的任务，但不接受新任务。如果已关闭，则调用没有其他效果。
     */
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(SHUTDOWN);
            interruptIdleWorkers();
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
    }
}
```

ThreadPoolExecutor 中的 execute()方法 执行 Runnable 任务 的流程逻辑可以用下图表示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E7%BA%BF%E7%A8%8B%E6%B1%A0%E6%B5%81%E7%A8%8B-6af836.png)

### 工具类 Executors

看类名也知道，它最主要的作用就是提供 static 的工具方法，为开发者提供各种封装好的 具有各自特性的线程池。

```java
public class Executors {

    /**
     * 创建一个固定线程数量的线程池
     */
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }

    /**
     * 创建一个单线程的线程池
     */
    public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService
            (new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>()));
    }

    /**
     * 创建一个缓存的，可动态伸缩的线程池。
     * 可以看出来：核心线程数为0，最大线程数为Integer.MAX_VALUE，如果任务数在某一瞬间暴涨，
     * 这个线程池很可能会把 服务器撑爆。
     * 另外需要注意的是，它们底层都是使用了 ThreadPoolExecutor，只不过帮我们配好了参数
     */
    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>());
    }
}
```

# Lock锁组件

## 类图结构

J.U.C 的锁组件中 类相对较少，从 JDK 相应的包中也能看出来，下图标记了其中最主要的几个接口和类，也是本文要分析的重点。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/JUC%E7%9A%84locks%E5%8C%85-347e11.png)

下图 将这几个接口和类 以类图的方式展现出来，其中包含了它们所声明的主要方法。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/JUC%E9%94%81%E7%BB%84%E4%BB%B6%E7%B1%BB%E5%9B%BE-676edd.png)

## Lock 组件

Lock 组件的结构很简单，只有一个接口和一个实现类，源码如下。

```java
public interface Lock {

    /**
     * 获取锁
     */
    void lock();

    /**
     * 获取锁，除非当前线程中断
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 只有当调用时 锁是空闲的情况下，才获取锁
     */
    boolean tryLock();

    /**
     * 如果锁在给定的等待时间内空闲且当前线程未被中断，则获取该锁
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁
     */
    void unlock();
}

public class ReentrantLock implements Lock, java.io.Serializable {

	/** 提供所有实现机制的同步器，ReentrantLock 的主要方法都依赖于该对象进行实现 */
    private final Sync sync;

    /**
     * ReentrantLock锁 的同步控制基础。它的两个子类分别实现了公平锁和非公平锁，如下。
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = -5179523762034025860L;

        abstract void lock();

        /**
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         */
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }

        protected final boolean tryRelease(int releases) {
            int c = getState() - releases;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        final boolean isLocked() {
            return getState() != 0;
        }
    }

    /**
     * 非公平锁，基于上面的 Sync类
     */
    static final class NonfairSync extends Sync {
        private static final long serialVersionUID = 7316153563782823691L;

        final void lock() {
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            else
                acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    /**
     * 公平锁，基于上面的 Sync类
     */
    static final class FairSync extends Sync {
        private static final long serialVersionUID = -3000897897090466540L;

        final void lock() {
            acquire(1);
        }

        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();
            int c = getState();
            if (c == 0) {
                if (!hasQueuedPredecessors() &&
                    compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }
            return false;
        }
    }

    /**
     * 无参初始化时，默认实例化 非公平锁
     */
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    /**
     * 可通过参数fair 控制实例化的是 公平锁还是非公平锁
     */
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    public void lock() {
        sync.lock();
    }

    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    public boolean tryLock(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    public void unlock() {
        sync.release(1);
    }

    public boolean isLocked() {
        return sync.isLocked();
    }

    public final boolean isFair() {
        return sync instanceof FairSync;
    }
}
```

## ReadWriteLock 组件

ReadWriteLock 组件的结构也很简单，与上面的 Lock 组件 不同的是，它提供了 公平的读锁写锁，以及非公平的读锁写锁。

```java
public interface ReadWriteLock {
    /**
     * 获取一个 读锁
     */
    Lock readLock();

    /**
     * 获取一个 写锁
     */
    Lock writeLock();
}

public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {

    /** 由内部类提供的读锁 */
    private final ReentrantReadWriteLock.ReadLock readerLock;
    /** 由内部类提供的写锁 */
    private final ReentrantReadWriteLock.WriteLock writerLock;
    /** 提供所有实现机制的同步器 */
    final Sync sync;

    /**
     * 默认创建 非公平的读锁写锁
     */
    public ReentrantReadWriteLock() {
        this(false);
    }

    /**
     * 由参数 fair 指定读锁写锁是公平的还是非公平的
     */
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    /**
     * 获取写锁
     * 获取读锁
     */
    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    abstract static class Sync extends AbstractQueuedSynchronizer {

        protected final boolean tryRelease(int releases) {
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();
            int nextc = getState() - releases;
            boolean free = exclusiveCount(nextc) == 0;
            if (free)
                setExclusiveOwnerThread(null);
            setState(nextc);
            return free;
        }

        protected final boolean tryAcquire(int acquires) {
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            int w = exclusiveCount(c);
            if (c != 0) {
                // (Note: if c != 0 and w == 0 then shared count != 0)
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                // Reentrant acquire
                setState(c + acquires);
                return true;
            }
            if (writerShouldBlock() ||
                !compareAndSetState(c, c + acquires))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            } else {
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }
            for (;;) {
                int c = getState();
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    return nextc == 0;
            }
        }

        protected final int tryAcquireShared(int unused) {
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            Thread current = Thread.currentThread();
            int c = getState();
            if (exclusiveCount(c) != 0 &&
                getExclusiveOwnerThread() != current)
                return -1;
            int r = sharedCount(c);
            if (!readerShouldBlock() &&
                r < MAX_COUNT &&
                compareAndSetState(c, c + SHARED_UNIT)) {
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                } else if (firstReader == current) {
                    firstReaderHoldCount++;
                } else {
                    HoldCounter rh = cachedHoldCounter;
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }
                return 1;
            }
            return fullTryAcquireShared(current);
        }

        /**
         * Performs tryLock for write, enabling barging in both modes.
         * This is identical in effect to tryAcquire except for lack
         * of calls to writerShouldBlock.
         */
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();
            if (c != 0) {
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }
            if (!compareAndSetState(c, c + 1))
                return false;
            setExclusiveOwnerThread(current);
            return true;
        }

        /**
         * Performs tryLock for read, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();
            for (;;) {
                int c = getState();
                if (exclusiveCount(c) != 0 &&
                    getExclusiveOwnerThread() != current)
                    return false;
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
            }
        }

        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }
    }

    /**
     * 非公平锁
     */
    static final class NonfairSync extends Sync {

        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }
        final boolean readerShouldBlock() {
            /* As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    /**
     * 公平锁
     */
    static final class FairSync extends Sync {

        final boolean writerShouldBlock() {
            return hasQueuedPredecessors();
        }
        final boolean readerShouldBlock() {
            return hasQueuedPredecessors();
        }
    }

    /**
     * 读锁
     */
    public static class ReadLock implements Lock, java.io.Serializable {

        private final Sync sync;

        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        public void lock() {
            sync.acquireShared(1);
        }

        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        public boolean tryLock() {
            return sync.tryReadLock();
        }

        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        public void unlock() {
            sync.releaseShared(1);
        }
    }

    /**
     * 写锁
     */
    public static class WriteLock implements Lock, java.io.Serializable {

        private final Sync sync;

        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        public void lock() {
            sync.acquire(1);
        }

        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        public boolean tryLock(long timeout, TimeUnit unit)
                throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        public void unlock() {
            sync.release(1);
        }
    }

    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }
}
```

## AbstractQueuedSynchronizer

最后看一下抽象类 AbstractQueuedSynchronizer，在同步组件的实现中，AQS 是核心部分，同步组件的实现者通过使用 AQS 提供的模板方法实现同步组件语义，AQS 则实现了对同步状态的管理，以及对阻塞线程进行排队，等待通知等等一些底层的实现处理。AQS 的核心包括：同步队列，独占式锁的获取和释放，共享锁的获取和释放以及可中断锁，超时等待锁获取这些特性的实现，而这些实际上则是 AQS 提供出来的模板方法。源码如下。

```java
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer
    implements java.io.Serializable {

    /**
     * 当共享资源被某个线程占有，其他请求该资源的线程将会阻塞，从而进入同步队列。
     * 就数据结构而言，队列的实现方式无外乎两者一是通过数组的形式，另外一种则是链表的形式。
     * AQS中的同步队列则是通过链式方式进行实现，下面的内部类Node便是其实现的载体
     */
    static final class Node {

        /** Marker to indicate a node is waiting in shared mode */
        static final Node SHARED = new Node();
        /** Marker to indicate a node is waiting in exclusive mode */
        static final Node EXCLUSIVE = null;

        // 节点从同步队列中取消
        static final int CANCELLED =  1;
        // 后继节点的线程处于等待状态，如果当前节点释放同步状态会通知后继节点，
        // 使得后继节点的线程能够运行；
        static final int SIGNAL    = -1;
        // 当前节点进入等待队列中
        static final int CONDITION = -2;
        // 表示下一次共享式同步状态获取将会无条件传播下去
        static final int PROPAGATE = -3;

		// 节点状态
        volatile int waitStatus;

		// 当前节点/线程的前驱节点
        volatile Node prev;

		// 当前节点/线程的后驱节点
        volatile Node next;

		// 加入同步队列的线程引用
        volatile Thread thread;

		// 等待队列中的下一个节点
        Node nextWaiter;

        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {    // Used to establish initial head or SHARED marker
        }

        Node(Thread thread, Node mode) {     // Used by addWaiter
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) { // Used by Condition
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * AQS实际上通过头尾指针来管理同步队列，同时实现包括获取锁失败的线程进行入队，
     * 释放锁时对同步队列中的线程进行通知等核心方法。
     */
    private transient volatile Node head;
    private transient volatile Node tail;

    /**
     * 获取独占式锁
     */
    public final void acquire(int arg) {
        if (!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    /**
     * 释放独占式锁
     */
    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    /**
     * 获取可中断式锁
     */
    public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
            doAcquireInterruptibly(arg);
    }

    /**
     * 获取共享锁
     */
    public final void acquireShared(int arg) {
        if (tryAcquireShared(arg) < 0)
            doAcquireShared(arg);
    }

    /**
     * 释放共享锁
     */
    public final boolean releaseShared(int arg) {
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }
}
```

# Semaphore

Semaphore 信号量，可用于控制一定时间内，并发执行的线程数，基于 AQS 实现。可应用于网关限流、资源限制 (如 最大可发起连接数)。由于 release() 释放许可时，未对释放许可数做限制，所以可以通过该方法增加总的许可数量。

**获取许可** 支持公平和非公平模式，默认非公平模式。公平模式无论是否有许可，都会先判断是否有线程在排队，如果有线程排队，则进入排队，否则尝试获取许可；非公平模式无论许可是否充足，直接尝试获取许可。

不多废话，下面直接看源码。

#### 核心内部类 Sync

```java
abstract static class Sync extends AbstractQueuedSynchronizer {

    private static final long serialVersionUID = 1192457210091910933L;

    /* 赋值state为总许可数 */
    Sync(int permits) {
        setState(permits);
    }

    /* 剩余许可数 */
    final int getPermits() {
        return getState();
    }

    /* 自旋 + CAS非公平获取 */
    final int nonfairTryAcquireShared(int acquires) {
        for (;;) {
            // 剩余可用许可数
            int available = getState();
            // 本次获取许可后，剩余许可
            int remaining = available - acquires;
            // 如果获取后，剩余许可大于0，则CAS更新剩余许可，否则获取失败失败
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }

    /**
     * 自旋 + CAS 释放许可
     * 由于未对释放许可数做限制，所以可以通过release动态增加许可数量
     */
    protected final boolean tryReleaseShared(int releases) {
        for (;;) {
            // 当前剩余许可
            int current = getState();
            // 许可更新值
            int next = current + releases;
            // 如果许可更新值为负数，说明许可数量溢出，抛出错误
            if (next < current) // overflow
                throw new Error("Maximum permit count exceeded");
            // CAS更新许可数量
            if (compareAndSetState(current, next))
                return true;
        }
    }

    /* 自旋 + CAS 减少许可数量 */
    final void reducePermits(int reductions) {
        for (;;) {
            // 当前剩余许可
            int current = getState();
            // 更新值
            int next = current - reductions;
           	// 较少许可数错误，抛出异常
            if (next > current) // underflow
                throw new Error("Permit count underflow");
            // CAS更新许可数
            if (compareAndSetState(current, next))
                return;
        }
    }

    /* 丢弃所有许可 */
    final int drainPermits() {
        for (;;) {
            int current = getState();
            if (current == 0 || compareAndSetState(current, 0))
                return current;
        }
    }
}

/**
 * 非公平模式
 */
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = -2694183684443567898L;

    NonfairSync(int permits) {
        super(permits);
    }

    protected int tryAcquireShared(int acquires) {
        return nonfairTryAcquireShared(acquires);
    }
}

/**
 * 公平模式
 */
static final class FairSync extends Sync {
    private static final long serialVersionUID = 2014338818796000944L;

    FairSync(int permits) {
        super(permits);
    }

    /**
     * 公平模式获取许可
     * 公平模式不论许可是否充足，都会判断同步队列中是否有线程在等地，如果有，获取失败，排队阻塞
     */
    protected int tryAcquireShared(int acquires) {
        for (;;) {
            // 如果有线程在排队，立即返回
            if (hasQueuedPredecessors())
                return -1;
            // 自旋 + cas获取许可
            int available = getState();
            int remaining = available - acquires;
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }
}
```

#### 主要 API

```java
public class Semaphore implements java.io.Serializable {

    private static final long serialVersionUID = -3222578661600680210L;

    /** All mechanics via AbstractQueuedSynchronizer subclass */
    private final Sync sync;

    /**
     * 根据给定的 总许可数permits，创建 Semaphore
     */
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    /**
     * fair为true表示使用公平锁模式，false使用非公平锁
     */
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    // --------------------- 获取许可 --------------------

    /* 获取指定数量的许可	*/
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /* 获取一个许可	*/
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    public final void acquireSharedInterruptibly(int arg)
        throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (tryAcquireShared(arg) < 0) // 获取许可，剩余许可>=0，则获取许可成功，<0获取许可失败，进入排队
            doAcquireSharedInterruptibly(arg);
    }

    protected int tryAcquireShared(int acquires) {
        return nonfairTryAcquireShared(acquires);
    }

    /**
     * @return 剩余许可数量。非负数，获取许可成功，负数，获取许可失败
     */
    final int nonfairTryAcquireShared(int acquires) {
        for (;;) {
            int available = getState();
            int remaining = available - acquires;
            if (remaining < 0 ||
                compareAndSetState(available, remaining))
                return remaining;
        }
    }

    /**
     * 获取许可失败，当前线程进入同步队列，排队阻塞
     */
    private void doAcquireSharedInterruptibly(int arg)
        throws InterruptedException {
        // 创建同步队列节点，并入队
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            for (;;) {
                // 如果当前节点是第二个节点，尝试获取锁
                final Node p = node.predecessor();
                if (p == head) {
                    int r = tryAcquireShared(arg);
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }
                // 阻塞当前线程
                if (shouldParkAfterFailedAcquire(p, node) &&
                    parkAndCheckInterrupt())
                    throw new InterruptedException();
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }

    // --------------------- 释放归还许可 -------------------------

    /* 释放指定数量的许可 */
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /* 释放一个许可 */
    public void release() {
        sync.releaseShared(1);
    }

    public final boolean releaseShared(int arg) {
        // 归还许可成功
        if (tryReleaseShared(arg)) {
            doReleaseShared();
            return true;
        }
        return false;
    }

    /**
     * 释放许可
     * 由于未对释放许可数做限制，所以可以通过release动态增加许可数量
     */
    protected final boolean tryReleaseShared(int releases) {
        for (;;) {
            int current = getState();
            int next = current + releases;
            if (next < current) // overflow
                throw new Error("Maximum permit count exceeded");
            if (compareAndSetState(current, next))
                return true;
        }
    }

    private void doReleaseShared() {
        // 自旋，唤醒等待的第一个线程(其他线程将由第一个线程向后传递唤醒)
        for (;;) {
            Node h = head;
            if (h != null && h != tail) {
                int ws = h.waitStatus;
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        continue;            // loop to recheck cases
                    // 唤醒第一个等待线程
                    unparkSuccessor(h);
                }
                else if (ws == 0 &&
                         !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    continue;                // loop on failed CAS
            }
            if (h == head)                   // loop if head changed
                break;
        }
    }
}
```



## 简介

AbstractQueuedSynchronizer 是 Doug Lea 大师创作的用来构建锁或者其他同步组件的基础框架类。J.U.C 中许多锁和并发工具类的核心实现都依赖于 AQS，如：ReentrantLock、ReentrantReadWriteLock、Semaphore、CountDownLatch 等。

AQS 的源码中 方法很多，但主要做了三件事情：

1. 管理 同步状态；
2. 维护 同步队列；
3. 阻塞和唤醒 线程。

另外，从行为上来区分就是 获取锁 和 释放锁，从模式上来区分就是 独占锁 和 共享锁。

## 实现原理

AQS 内部维护了一个 FIFO 队列来管理锁。线程首先会尝试获取锁，如果失败，则将当前线程以及等待状态等信息包成一个 Node 节点放入同步队列阻塞起来，当持有锁的线程释放锁时，就会唤醒队列中的后继线程。

#### 获取锁的伪代码

```
while (不满足获取锁的条件) {
    把当前线程包装成节点插入同步队列
    if (需要阻塞当前线程)
        阻塞当前线程直至被唤醒
}
将当前线程从同步队列中移除
```

#### 释放锁的伪代码

```
修改同步状态
if (修改后的状态允许其他线程获取到锁)
    唤醒后继线程
```

## 源码解析

#### AQS 的核心数据结构 Node(内部类)

```java
/**
 * 当共享资源被某个线程占有，其他请求该资源的线程将会阻塞，从而进入同步队列。
 * AQS 中的同步队列通过链表实现，下面的内部类 Node 便是其实现的载体
 */
static final class Node {

    /* 用于标记一个节点在共享模式下等待 */
    static final Node SHARED = new Node();

    /* 用于标记一个节点在独占模式下等待 */
    static final Node EXCLUSIVE = null;

    /* 当前线程因为超时或者中断被取消。这是一个终结态，也就是状态到此为止 */
    static final int CANCELLED = 1;

    /**
     * 当前线程的后继线程被阻塞或者即将被阻塞，当前线程释放锁或者取消后需要唤醒后继线程。
     * 这个状态一般都是后继线程来设置前驱节点的
     */
    static final int SIGNAL = -1;

    /* 当前线程在condition队列中 */
    static final int CONDITION = -2;

    /**
     * 用于将唤醒后继线程传递下去，这个状态的引入是为了完善和增强共享锁的唤醒机制。
     * 在一个节点成为头节点之前，是不会跃迁为此状态的
     */
    static final int PROPAGATE = -3;

    /* 等待状态 */
    volatile int waitStatus;

    /* 前驱节点 */
    volatile Node prev;

    /* 后继节点 */
    volatile Node next;

    /* 节点对应的线程 */
    volatile Thread thread;

    /* 等待队列中的后继节点 */
    Node nextWaiter;

    /* 当前节点是否处于共享模式等待 */
    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    /* 获取前驱节点，如果为空的话抛出空指针异常 */
    final Node predecessor() throws NullPointerException {
        Node p = prev;
        if (p == null) {
            throw new NullPointerException();
        } else {
            return p;
        }
    }

    Node() {
    }

    /* addWaiter会调用此构造函数 */
    Node(Thread thread, Node mode) {
        this.nextWaiter = mode;
        this.thread = thread;
    }

    /* Condition会用到此构造函数 */
    Node(Thread thread, int waitStatus) {
        this.waitStatus = waitStatus;
        this.thread = thread;
    }
}
```

#### 获取独占锁的实现

```java
/**
 * 首先尝试获取一次锁，如果成功，则返回；
 * 否则会把当前线程包装成Node插入到队列中，在队列中会检测是否为head的直接后继，并尝试获取锁,
 * 如果获取失败，则阻塞当前线程，直至被 "释放锁的线程" 唤醒或者被中断，随后再次尝试获取锁，如此反复
 */
public final void acquire(int arg) {
    if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}

/**
 * 在队列中新增一个节点
 */
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    // 快速尝试
    if (pred != null) {
        node.prev = pred;
        // 通过CAS在队尾插入当前节点
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    // 初始情况或者在快速尝试失败后插入节点
    enq(node);
    return node;
}

/**
 * 通过循环+CAS在队列中成功插入一个节点后返回
 */
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        // 初始化head和tail
        if (t == null) {
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            /*
             * AQS的精妙在于很多细节代码，比如需要用CAS往队尾里增加一个元素
             * 此处的else分支是先在CAS的if前设置node.prev = t，而不是在CAS成功之后再设置。
             * 一方面是基于CAS的双向链表插入目前没有完美的解决方案，另一方面这样子做的好处是：
             * 保证每时每刻tail.prev都不会是一个null值，否则如果node.prev = t
             * 放在下面if的里面，会导致一个瞬间tail.prev = null，这样会使得队列不完整
             */
            node.prev = t;
            // CAS设置tail为node，成功后把老的tail也就是t连接到node
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}

/**
 * 在队列中的节点通过此方法获取锁
 */
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            /*
             * 检测当前节点的前驱节点是否为head，这是试获取锁的资格。
             * 如果是的话，则调用tryAcquire尝试获取锁，成功，则将head置为当前节点
             */
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            /*
             * 如果未成功获取锁，则根据前驱节点判断是否要阻塞。
             * 如果阻塞过程中被中断，则置interrupted标志位为true。
             * shouldParkAfterFailedAcquire方法在前驱状态不为SIGNAL的情况下都会循环重试获取锁
             */
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}

/**
 * 根据前驱节点中的waitStatus来判断是否需要阻塞当前线程
 */
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        /*
         * 前驱节点设置为SIGNAL状态，在释放锁的时候会唤醒后继节点，
         * 所以后继节点（也就是当前节点）现在可以阻塞自己
         */
        return true;
    if (ws > 0) {
        /*
         * 前驱节点状态为取消，向前遍历，更新当前节点的前驱为往前第一个非取消节点。
         * 当前线程会之后会再次回到循环并尝试获取锁
         */
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
         /**
          * 等待状态为0或者PROPAGATE(-3)，设置前驱的等待状态为SIGNAL,
          * 并且之后会回到循环再次重试获取锁
          */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}


/**
 * 该方法实现某个node取消获取锁
 */
private void cancelAcquire(Node node) {

   if (node == null)
       return;

   node.thread = null;

   // 遍历并更新节点前驱，把node的prev指向前部第一个非取消节点
   Node pred = node.prev;
   while (pred.waitStatus > 0)
       node.prev = pred = pred.prev;

   // 记录pred节点的后继为predNext，后续CAS会用到
   Node predNext = pred.next;

   // 直接把当前节点的等待状态置为取消,后继节点即便也在cancel可以跨越node节点
   node.waitStatus = Node.CANCELLED;

   /*
    * 如果CAS将tail从node置为pred节点了
    * 则剩下要做的事情就是尝试用CAS将pred节点的next更新为null以彻底切断pred和node的联系。
    * 这样一来就断开了pred与pred的所有后继节点，这些节点由于变得不可达，最终会被回收掉。
    * 由于node没有后继节点，所以这种情况到这里整个cancel就算是处理完毕了。
    *
    * 这里的CAS更新pred的next即使失败了也没关系，说明有其它新入队线程或者其它取消线程更新掉了。
    */
   if (node == tail && compareAndSetTail(node, pred)) {
       compareAndSetNext(pred, predNext, null);
   } else {
       // 如果node还有后继节点，这种情况要做的事情是把pred和后继非取消节点拼起来
       int ws;
       if (pred != head &&
           ((ws = pred.waitStatus) == Node.SIGNAL ||
            (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
           pred.thread != null) {
           Node next = node.next;
           /*
            * 如果node的后继节点next非取消状态的话，则用CAS尝试把pred的后继置为node的后继节点
            * 这里if条件为false或者CAS失败都没关系，这说明可能有多个线程在取消，总归会有一个能成功的
            */
           if (next != null && next.waitStatus <= 0)
               compareAndSetNext(pred, predNext, next);
       } else {
           /*
            * 这时说明pred == head或者pred状态取消或者pred.thread == null
            * 在这些情况下为了保证队列的活跃性，需要去唤醒一次后继线程。
            * 举例来说pred == head完全有可能实际上目前已经没有线程持有锁了，
            * 自然就不会有释放锁唤醒后继的动作。如果不唤醒后继，队列就挂掉了。
            *
            * 这种情况下看似由于没有更新pred的next的操作，队列中可能会留有一大把的取消节点。
            * 实际上不要紧，因为后继线程唤醒之后会走一次试获取锁的过程，
            * 失败的话会走到shouldParkAfterFailedAcquire的逻辑。
            * 那里面的if中有处理前驱节点如果为取消则维护pred/next,踢掉这些取消节点的逻辑。
            */
           unparkSuccessor(node);
       }

       /*
        * 取消节点的next之所以设置为自己本身而不是null,
        * 是为了方便AQS中Condition部分的isOnSyncQueue方法,
        * 判断一个原先属于条件队列的节点是否转移到了同步队列。
        *
        * 因为同步队列中会用到节点的next域，取消节点的next也有值的话，
        * 可以断言next域有值的节点一定在同步队列上。
        *
        * 在GC层面，和设置为null具有相同的效果
        */
       node.next = node;
   }
}

/**
 * 唤醒后继线程
 */
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    // 尝试将node的等待状态置为0,这样的话,后继争用线程可以有机会再尝试获取一次锁
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    Node s = node.next;
    /*
     * 这里的逻辑就是如果node.next存在并且状态不为取消，则直接唤醒s即可
     * 否则需要从tail开始向前找到node之后最近的非取消节点。
     *
     * 这里为什么要从tail开始向前查找也是值得琢磨的:
     * 如果读到s == null，不代表node就为tail，参考addWaiter以及enq函数中的我的注释。
     * 不妨考虑到如下场景：
     * 1. node某时刻为tail
     * 2. 有新线程通过addWaiter中的if分支或者enq方法添加自己
     * 3. compareAndSetTail成功
     * 4. 此时这里的Node s = node.next读出来s == null，但事实上node已经不是tail，它有后继了!
     */
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

#### 释放独占锁的实现

释放一个独占锁，首先会调用 tryRelease 方法，在完全释放掉独占锁后，其后继线程是可以获取到独占锁的，因此释放线程需要做的事情是：唤醒一个队列中的后继线程，让它去尝试获取独占锁。

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        /*
         * 此时的head节点可能有3种情况:
         * 1. null (AQS的head延迟初始化+无竞争的情况)
         * 2. 当前线程在获取锁时new出来的节点通过setHead设置的
         * 3. 由于通过tryRelease已经完全释放掉了独占锁，有新的节点在acquireQueued中获取到了独占锁，并设置了head

         * 第三种情况可以再分为两种情况：
         *     情况一：
         *     		时刻1：线程A通过acquireQueued，持锁成功，set了head
         *          时刻2：线程B通过tryAcquire试图获取独占锁失败失败，进入acquiredQueued
         *          时刻3：线程A通过tryRelease释放了独占锁
         *          时刻4：线程B通过acquireQueued中的tryAcquire获取到了独占锁并调用setHead
         *          时刻5：线程A读到了此时的head实际上是线程B对应的node
         *     情况二：
         *     		时刻1：线程A通过tryAcquire直接持锁成功，head为null
         *          时刻2：线程B通过tryAcquire试图获取独占锁失败失败，入队过程中初始化了head，进入acquiredQueued
         *          时刻3：线程A通过tryRelease释放了独占锁，此时线程B还未开始tryAcquire
         *          时刻4：线程A读到了此时的head实际上是线程B初始化出来的傀儡head
         */
        Node h = head;
        // head节点状态不会是CANCELLED，所以这里h.waitStatus != 0相当于h.waitStatus < 0
        if (h != null && h.waitStatus != 0)
            // 唤醒后继线程，此函数在acquire中已经分析过，不再列举说明
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

整个 release 做的事情就是：

1. 调用 tryRelease；
2. 如果 tryRelease 返回 true 也就是独占锁被完全释放，唤醒后继线程。

#### 获取共享锁的实现

共享锁允许多个线程持有，如果要使用 AQS 中的共享锁，在实现 tryAcquireShared 方法 时需要注意，返回负数表示获取失败，返回 0 表示成功，但是后继争用线程不会成功，返回正数表示获取成功，并且后继争用线程也可能成功。

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}

private void doAcquireShared(int arg) {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                // 一旦共享获取成功，设置新的头结点，并且唤醒后继线程
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}

/**
 * 这个函数做的事情有两件:
 * 1. 在获取共享锁成功后，设置head节点
 * 2. 根据调用tryAcquireShared返回的状态以及节点本身的等待状态来判断是否需要唤醒后继线程
 */
private void setHeadAndPropagate(Node node, int propagate) {
    // 把当前的head封闭在方法栈上，用以下面的条件检查
    Node h = head;
    setHead(node);
    /*
     * propagate是tryAcquireShared的返回值，这是决定是否传播唤醒的依据之一。
     * h.waitStatus为SIGNAL或者PROPAGATE时也根据node的下一个节点共享来决定是否传播唤醒，
     * 这里为什么不能只用propagate > 0来决定是否可以传播在本文下面的思考问题中有相关讲述
     */
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared();
    }
}

/**
 * 这是共享锁中的核心唤醒函数，主要做的事情就是唤醒下一个线程或者设置传播状态。
 * 后继线程被唤醒后，会尝试获取共享锁，如果成功之后，则又会调用setHeadAndPropagate,将唤醒传播下去。
 * 这个函数的作用是保障在acquire和release存在竞争的情况下，保证队列中处于等待状态的节点能够有办法被唤醒。
 */
private void doReleaseShared() {
    /*
     * 以下的循环做的事情就是，在队列存在后继线程的情况下，唤醒后继线程；
     * 或者由于多线程同时释放共享锁由于处在中间过程，读到head节点等待状态为0的情况下，
     * 虽然不能unparkSuccessor，但为了保证唤醒能够正确稳固传递下去，设置节点状态为PROPAGATE。
     * 这样的话获取锁的线程在执行setHeadAndPropagate时可以读到PROPAGATE，从而由获取锁的线程去释放后继等待线程
     */
    for (;;) {
        Node h = head;
        // 如果队列中存在后继线程。
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;
                unparkSuccessor(h);
            }
            // 如果h节点的状态为0，需要设置为PROPAGATE用以保证唤醒的传播。
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;
        }
        // 检查h是否仍然是head，如果不是的话需要再进行循环。
        if (h == head)
            break;
    }
}
```

#### 释放共享锁的实现

共享锁的获取和释放都会涉及到 doReleaseShared 方法，也就是后继线程的唤醒。

```java
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        // doReleaseShared的实现上面获取共享锁已经介绍
        doReleaseShared();
        return true;
    }
    return false;
}
```



# 详解AbstractQueuedSynchronizer



## 简介

AbstractQueuedSynchronizer 是 Doug Lea 大师创作的用来构建锁或者其他同步组件的基础框架类。J.U.C 中许多锁和并发工具类的核心实现都依赖于 AQS，如：ReentrantLock、ReentrantReadWriteLock、Semaphore、CountDownLatch 等。

AQS 的源码中 方法很多，但主要做了三件事情：

1. 管理 同步状态；
2. 维护 同步队列；
3. 阻塞和唤醒 线程。

另外，从行为上来区分就是 获取锁 和 释放锁，从模式上来区分就是 独占锁 和 共享锁。

## 实现原理

AQS 内部维护了一个 FIFO 队列来管理锁。线程首先会尝试获取锁，如果失败，则将当前线程以及等待状态等信息包成一个 Node 节点放入同步队列阻塞起来，当持有锁的线程释放锁时，就会唤醒队列中的后继线程。

#### 获取锁的伪代码

```
while (不满足获取锁的条件) {
    把当前线程包装成节点插入同步队列
    if (需要阻塞当前线程)
        阻塞当前线程直至被唤醒
}
将当前线程从同步队列中移除
```

#### 释放锁的伪代码

```
修改同步状态
if (修改后的状态允许其他线程获取到锁)
    唤醒后继线程
```

## 源码解析

#### AQS 的核心数据结构 Node(内部类)

```java
/**
 * 当共享资源被某个线程占有，其他请求该资源的线程将会阻塞，从而进入同步队列。
 * AQS 中的同步队列通过链表实现，下面的内部类 Node 便是其实现的载体
 */
static final class Node {

    /* 用于标记一个节点在共享模式下等待 */
    static final Node SHARED = new Node();

    /* 用于标记一个节点在独占模式下等待 */
    static final Node EXCLUSIVE = null;

    /* 当前线程因为超时或者中断被取消。这是一个终结态，也就是状态到此为止 */
    static final int CANCELLED = 1;

    /**
     * 当前线程的后继线程被阻塞或者即将被阻塞，当前线程释放锁或者取消后需要唤醒后继线程。
     * 这个状态一般都是后继线程来设置前驱节点的
     */
    static final int SIGNAL = -1;

    /* 当前线程在condition队列中 */
    static final int CONDITION = -2;

    /**
     * 用于将唤醒后继线程传递下去，这个状态的引入是为了完善和增强共享锁的唤醒机制。
     * 在一个节点成为头节点之前，是不会跃迁为此状态的
     */
    static final int PROPAGATE = -3;

    /* 等待状态 */
    volatile int waitStatus;

    /* 前驱节点 */
    volatile Node prev;

    /* 后继节点 */
    volatile Node next;

    /* 节点对应的线程 */
    volatile Thread thread;

    /* 等待队列中的后继节点 */
    Node nextWaiter;

    /* 当前节点是否处于共享模式等待 */
    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    /* 获取前驱节点，如果为空的话抛出空指针异常 */
    final Node predecessor() throws NullPointerException {
        Node p = prev;
        if (p == null) {
            throw new NullPointerException();
        } else {
            return p;
        }
    }

    Node() {
    }

    /* addWaiter会调用此构造函数 */
    Node(Thread thread, Node mode) {
        this.nextWaiter = mode;
        this.thread = thread;
    }

    /* Condition会用到此构造函数 */
    Node(Thread thread, int waitStatus) {
        this.waitStatus = waitStatus;
        this.thread = thread;
    }
}
```

#### 获取独占锁的实现

```java
/**
 * 首先尝试获取一次锁，如果成功，则返回；
 * 否则会把当前线程包装成Node插入到队列中，在队列中会检测是否为head的直接后继，并尝试获取锁,
 * 如果获取失败，则阻塞当前线程，直至被 "释放锁的线程" 唤醒或者被中断，随后再次尝试获取锁，如此反复
 */
public final void acquire(int arg) {
    if (!tryAcquire(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}

/**
 * 在队列中新增一个节点
 */
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    Node pred = tail;
    // 快速尝试
    if (pred != null) {
        node.prev = pred;
        // 通过CAS在队尾插入当前节点
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    // 初始情况或者在快速尝试失败后插入节点
    enq(node);
    return node;
}

/**
 * 通过循环+CAS在队列中成功插入一个节点后返回
 */
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        // 初始化head和tail
        if (t == null) {
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            /*
             * AQS的精妙在于很多细节代码，比如需要用CAS往队尾里增加一个元素
             * 此处的else分支是先在CAS的if前设置node.prev = t，而不是在CAS成功之后再设置。
             * 一方面是基于CAS的双向链表插入目前没有完美的解决方案，另一方面这样子做的好处是：
             * 保证每时每刻tail.prev都不会是一个null值，否则如果node.prev = t
             * 放在下面if的里面，会导致一个瞬间tail.prev = null，这样会使得队列不完整
             */
            node.prev = t;
            // CAS设置tail为node，成功后把老的tail也就是t连接到node
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}

/**
 * 在队列中的节点通过此方法获取锁
 */
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            /*
             * 检测当前节点的前驱节点是否为head，这是试获取锁的资格。
             * 如果是的话，则调用tryAcquire尝试获取锁，成功，则将head置为当前节点
             */
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            /*
             * 如果未成功获取锁，则根据前驱节点判断是否要阻塞。
             * 如果阻塞过程中被中断，则置interrupted标志位为true。
             * shouldParkAfterFailedAcquire方法在前驱状态不为SIGNAL的情况下都会循环重试获取锁
             */
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}

/**
 * 根据前驱节点中的waitStatus来判断是否需要阻塞当前线程
 */
private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
    int ws = pred.waitStatus;
    if (ws == Node.SIGNAL)
        /*
         * 前驱节点设置为SIGNAL状态，在释放锁的时候会唤醒后继节点，
         * 所以后继节点（也就是当前节点）现在可以阻塞自己
         */
        return true;
    if (ws > 0) {
        /*
         * 前驱节点状态为取消，向前遍历，更新当前节点的前驱为往前第一个非取消节点。
         * 当前线程会之后会再次回到循环并尝试获取锁
         */
        do {
            node.prev = pred = pred.prev;
        } while (pred.waitStatus > 0);
        pred.next = node;
    } else {
         /**
          * 等待状态为0或者PROPAGATE(-3)，设置前驱的等待状态为SIGNAL,
          * 并且之后会回到循环再次重试获取锁
          */
        compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
    }
    return false;
}


/**
 * 该方法实现某个node取消获取锁
 */
private void cancelAcquire(Node node) {

   if (node == null)
       return;

   node.thread = null;

   // 遍历并更新节点前驱，把node的prev指向前部第一个非取消节点
   Node pred = node.prev;
   while (pred.waitStatus > 0)
       node.prev = pred = pred.prev;

   // 记录pred节点的后继为predNext，后续CAS会用到
   Node predNext = pred.next;

   // 直接把当前节点的等待状态置为取消,后继节点即便也在cancel可以跨越node节点
   node.waitStatus = Node.CANCELLED;

   /*
    * 如果CAS将tail从node置为pred节点了
    * 则剩下要做的事情就是尝试用CAS将pred节点的next更新为null以彻底切断pred和node的联系。
    * 这样一来就断开了pred与pred的所有后继节点，这些节点由于变得不可达，最终会被回收掉。
    * 由于node没有后继节点，所以这种情况到这里整个cancel就算是处理完毕了。
    *
    * 这里的CAS更新pred的next即使失败了也没关系，说明有其它新入队线程或者其它取消线程更新掉了。
    */
   if (node == tail && compareAndSetTail(node, pred)) {
       compareAndSetNext(pred, predNext, null);
   } else {
       // 如果node还有后继节点，这种情况要做的事情是把pred和后继非取消节点拼起来
       int ws;
       if (pred != head &&
           ((ws = pred.waitStatus) == Node.SIGNAL ||
            (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
           pred.thread != null) {
           Node next = node.next;
           /*
            * 如果node的后继节点next非取消状态的话，则用CAS尝试把pred的后继置为node的后继节点
            * 这里if条件为false或者CAS失败都没关系，这说明可能有多个线程在取消，总归会有一个能成功的
            */
           if (next != null && next.waitStatus <= 0)
               compareAndSetNext(pred, predNext, next);
       } else {
           /*
            * 这时说明pred == head或者pred状态取消或者pred.thread == null
            * 在这些情况下为了保证队列的活跃性，需要去唤醒一次后继线程。
            * 举例来说pred == head完全有可能实际上目前已经没有线程持有锁了，
            * 自然就不会有释放锁唤醒后继的动作。如果不唤醒后继，队列就挂掉了。
            *
            * 这种情况下看似由于没有更新pred的next的操作，队列中可能会留有一大把的取消节点。
            * 实际上不要紧，因为后继线程唤醒之后会走一次试获取锁的过程，
            * 失败的话会走到shouldParkAfterFailedAcquire的逻辑。
            * 那里面的if中有处理前驱节点如果为取消则维护pred/next,踢掉这些取消节点的逻辑。
            */
           unparkSuccessor(node);
       }

       /*
        * 取消节点的next之所以设置为自己本身而不是null,
        * 是为了方便AQS中Condition部分的isOnSyncQueue方法,
        * 判断一个原先属于条件队列的节点是否转移到了同步队列。
        *
        * 因为同步队列中会用到节点的next域，取消节点的next也有值的话，
        * 可以断言next域有值的节点一定在同步队列上。
        *
        * 在GC层面，和设置为null具有相同的效果
        */
       node.next = node;
   }
}

/**
 * 唤醒后继线程
 */
private void unparkSuccessor(Node node) {
    int ws = node.waitStatus;
    // 尝试将node的等待状态置为0,这样的话,后继争用线程可以有机会再尝试获取一次锁
    if (ws < 0)
        compareAndSetWaitStatus(node, ws, 0);

    Node s = node.next;
    /*
     * 这里的逻辑就是如果node.next存在并且状态不为取消，则直接唤醒s即可
     * 否则需要从tail开始向前找到node之后最近的非取消节点。
     *
     * 这里为什么要从tail开始向前查找也是值得琢磨的:
     * 如果读到s == null，不代表node就为tail，参考addWaiter以及enq函数中的我的注释。
     * 不妨考虑到如下场景：
     * 1. node某时刻为tail
     * 2. 有新线程通过addWaiter中的if分支或者enq方法添加自己
     * 3. compareAndSetTail成功
     * 4. 此时这里的Node s = node.next读出来s == null，但事实上node已经不是tail，它有后继了!
     */
    if (s == null || s.waitStatus > 0) {
        s = null;
        for (Node t = tail; t != null && t != node; t = t.prev)
            if (t.waitStatus <= 0)
                s = t;
    }
    if (s != null)
        LockSupport.unpark(s.thread);
}
```

#### 释放独占锁的实现

释放一个独占锁，首先会调用 tryRelease 方法，在完全释放掉独占锁后，其后继线程是可以获取到独占锁的，因此释放线程需要做的事情是：唤醒一个队列中的后继线程，让它去尝试获取独占锁。

```java
public final boolean release(int arg) {
    if (tryRelease(arg)) {
        /*
         * 此时的head节点可能有3种情况:
         * 1. null (AQS的head延迟初始化+无竞争的情况)
         * 2. 当前线程在获取锁时new出来的节点通过setHead设置的
         * 3. 由于通过tryRelease已经完全释放掉了独占锁，有新的节点在acquireQueued中获取到了独占锁，并设置了head

         * 第三种情况可以再分为两种情况：
         *     情况一：
         *     		时刻1：线程A通过acquireQueued，持锁成功，set了head
         *          时刻2：线程B通过tryAcquire试图获取独占锁失败失败，进入acquiredQueued
         *          时刻3：线程A通过tryRelease释放了独占锁
         *          时刻4：线程B通过acquireQueued中的tryAcquire获取到了独占锁并调用setHead
         *          时刻5：线程A读到了此时的head实际上是线程B对应的node
         *     情况二：
         *     		时刻1：线程A通过tryAcquire直接持锁成功，head为null
         *          时刻2：线程B通过tryAcquire试图获取独占锁失败失败，入队过程中初始化了head，进入acquiredQueued
         *          时刻3：线程A通过tryRelease释放了独占锁，此时线程B还未开始tryAcquire
         *          时刻4：线程A读到了此时的head实际上是线程B初始化出来的傀儡head
         */
        Node h = head;
        // head节点状态不会是CANCELLED，所以这里h.waitStatus != 0相当于h.waitStatus < 0
        if (h != null && h.waitStatus != 0)
            // 唤醒后继线程，此函数在acquire中已经分析过，不再列举说明
            unparkSuccessor(h);
        return true;
    }
    return false;
}
```

整个 release 做的事情就是：

1. 调用 tryRelease；
2. 如果 tryRelease 返回 true 也就是独占锁被完全释放，唤醒后继线程。

#### 获取共享锁的实现

共享锁允许多个线程持有，如果要使用 AQS 中的共享锁，在实现 tryAcquireShared 方法 时需要注意，返回负数表示获取失败，返回 0 表示成功，但是后继争用线程不会成功，返回正数表示获取成功，并且后继争用线程也可能成功。

```java
public final void acquireShared(int arg) {
    if (tryAcquireShared(arg) < 0)
        doAcquireShared(arg);
}

private void doAcquireShared(int arg) {
    final Node node = addWaiter(Node.SHARED);
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head) {
                int r = tryAcquireShared(arg);
                // 一旦共享获取成功，设置新的头结点，并且唤醒后继线程
                if (r >= 0) {
                    setHeadAndPropagate(node, r);
                    p.next = null; // help GC
                    if (interrupted)
                        selfInterrupt();
                    failed = false;
                    return;
                }
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}

/**
 * 这个函数做的事情有两件:
 * 1. 在获取共享锁成功后，设置head节点
 * 2. 根据调用tryAcquireShared返回的状态以及节点本身的等待状态来判断是否需要唤醒后继线程
 */
private void setHeadAndPropagate(Node node, int propagate) {
    // 把当前的head封闭在方法栈上，用以下面的条件检查
    Node h = head;
    setHead(node);
    /*
     * propagate是tryAcquireShared的返回值，这是决定是否传播唤醒的依据之一。
     * h.waitStatus为SIGNAL或者PROPAGATE时也根据node的下一个节点共享来决定是否传播唤醒，
     * 这里为什么不能只用propagate > 0来决定是否可以传播在本文下面的思考问题中有相关讲述
     */
    if (propagate > 0 || h == null || h.waitStatus < 0 ||
        (h = head) == null || h.waitStatus < 0) {
        Node s = node.next;
        if (s == null || s.isShared())
            doReleaseShared();
    }
}

/**
 * 这是共享锁中的核心唤醒函数，主要做的事情就是唤醒下一个线程或者设置传播状态。
 * 后继线程被唤醒后，会尝试获取共享锁，如果成功之后，则又会调用setHeadAndPropagate,将唤醒传播下去。
 * 这个函数的作用是保障在acquire和release存在竞争的情况下，保证队列中处于等待状态的节点能够有办法被唤醒。
 */
private void doReleaseShared() {
    /*
     * 以下的循环做的事情就是，在队列存在后继线程的情况下，唤醒后继线程；
     * 或者由于多线程同时释放共享锁由于处在中间过程，读到head节点等待状态为0的情况下，
     * 虽然不能unparkSuccessor，但为了保证唤醒能够正确稳固传递下去，设置节点状态为PROPAGATE。
     * 这样的话获取锁的线程在执行setHeadAndPropagate时可以读到PROPAGATE，从而由获取锁的线程去释放后继等待线程
     */
    for (;;) {
        Node h = head;
        // 如果队列中存在后继线程。
        if (h != null && h != tail) {
            int ws = h.waitStatus;
            if (ws == Node.SIGNAL) {
                if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                    continue;
                unparkSuccessor(h);
            }
            // 如果h节点的状态为0，需要设置为PROPAGATE用以保证唤醒的传播。
            else if (ws == 0 &&
                     !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                continue;
        }
        // 检查h是否仍然是head，如果不是的话需要再进行循环。
        if (h == head)
            break;
    }
}
```

#### 释放共享锁的实现

共享锁的获取和释放都会涉及到 doReleaseShared 方法，也就是后继线程的唤醒。

```java
public final boolean releaseShared(int arg) {
    if (tryReleaseShared(arg)) {
        // doReleaseShared的实现上面获取共享锁已经介绍
        doReleaseShared();
        return true;
    }
    return false;
}
```

