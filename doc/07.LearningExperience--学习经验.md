# LearningExperience--学习经验

# Java并发编程在各主流框架中的应用

Spring、Netty、Mybatis 等框架的代码中大量运用了 Java 多线程编程技巧。并发编程处理的恰当与否，将直接影响架构的性能。本章通过对 这些框架源码 的分析，结合并发编程的常用技巧，来讲解多线程编程在这些主流框架中的应用。

## Java 内存模型

JVM 规范 定义了 Java 内存模型 来屏蔽掉各种操作系统、虚拟机实现厂商和硬件的内存访问差异，以确保 Java 程序 在所有操作系统和平台上能够达到一致的内存访问效果。

### 工作内存和主内存

Java 内存模型 规定所有的变量都存储在主内存中，每个线程都有自己独立的工作内存，工作内存保存了 对应该线程使用的变量的主内存副本拷贝。线程对这些变量的操作都在自己的工作内存中进行，不能直接操作主内存 和 其他工作内存中存储的变量或者变量副本。线程间的变量传递需通过主内存来完成，三者的关系如下图所示。
![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/20200221000348294-0d33c4.png)

### Java 内存操作协议

Java 内存模型定义了 8 种操作来完成主内存和工作内存的变量访问，具体如下。
![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/20200221001115193-9c08c6.png)

- read：把一个变量的值从主内存传输到线程的工作内存中，以便随后的 load 动作使用。
- load：把从主内存中读取的变量值载入工作内存的变量副本中。
- use：把工作内存中一个变量的值传递给 Java 虚拟机执行引擎。
- assign：把从执行引擎接收到的变量的值赋值给工作内存中的变量。
- store：把工作内存中一个变量的值传送到主内存中，以便随后的 write 操作。
- write：工作内存传递过来的变量值放入主内存中。
- lock：把主内存的一个变量标识为某个线程独占的状态。
- unlock：把主内存中 一个处于锁定状态的变量释放出来，被释放后的变量才可以被其他线程锁定。

### 内存模型三大特性

#### 1、原子性

这个概念与事务中的原子性大概一致，表明此操作是不可分割，不可中断的，要么全部执行，要么全部不执行。 Java 内存模型直接保证的原子性操作包括 read、load、use、assign、store、write、lock、unlock 这八个。

#### 2、可见性

可见性是指当一个线程修改了共享变量的值，其他线程能够立即得知这个修改。Java 内存模型 是通过在变量修改后将新值同步回主内存，在变量读取前从主内存刷新变量值这种依赖主内存作为传递媒介的方式来实现可见性的，无论是普通变量还是 volatile 变量 都是如此，普通变量与 volatile 变量 的区别是，volatile 的特殊规则保证了新值能立即同步到主内存，以及每次使用前立即从主内存刷新。因此，可以说 volatile 保证了多线程操作时变量的可见性，而普通变量则不能保证这一点。除了 volatile 外，synchronized 也提供了可见性，synchronized 的可见性是由 “对一个变量执行 unlock 操作 之前，必须先把此变量同步回主内存中（执行 store、write 操作）” 这条规则获得。

#### 3、有序性

单线程环境下，程序会 “有序的”执行，即：线程内表现为串行语义。但是在多线程环境下，由于指令重排，并发执行的正确性会受到影响。在 Java 中使用 volatile 和 synchronized 关键字，可以保证多线程执行的有序性。volatile 通过加入内存屏障指令来禁止内存的重排序。synchronized 通过加锁，保证同一时刻只有一个线程来执行同步代码。

## volatile 的应用

打开 NioEventLoop 的代码中，有一个控制 IO 操作 和 其他任务运行比例的，用 volatile 修饰的 int 类型字段 ioRatio，代码如下。

```java
    private volatile int ioRatio = 50;
```

这里为什么要用 volatile 修饰呢？我们首先对 volatile 关键字进行说明，然后再结合 Netty 的代码进行分析。

关键字 volatile 是 Java 提供的最轻量级的同步机制，Java 内存模型对 volatile 专门定义了一些特殊的访问规则。下面我们就看它的规则。当一个变量被 volatile 修饰后，它将具备以下两种特性。

- 线程可见性：当一个线程修改了被 volatile 修饰的变量后，无论是否加锁，其他线程都可以立即看到最新的修改（什么叫立即看到最新的修改？感觉这句话太口语化且模糊，搞不太懂！），而普通变量却做不到这点。
- 禁止指令重排序优化：普通的变量仅仅保证在该方法的执行过程中所有依赖赋值结果的地方都能获取正确的结果，而不能保证变量赋值操作的顺序与程序代码的执行顺序一致。举个简单的例子说明下指令重排序优化问题，代码如下。

```java
public class ThreadStopExample {

	private static boolean stop;

	public static void main(String[] args) throws InterruptedException {
		Thread workThread = new Thread(new Runnable() {
			public void run() {
				int i= 0;
				while (!stop) {
					i++;
					try{
						TimeUnit.SECONDS.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		workThread.start();
		TimeUnit.SECONDS.sleep(3);
		stop = true;
	}
}
```

我们预期程序会在 3s 后停止，但是实际上它会一直执行下去，原因就是虚拟机对代码进行了指令重排序和优化，优化后的指令如下。

```java
	if (!stop)
	While(true)
		......
```

workThread 线程 在执行重排序后的代码时，是无法发现 变量 stop 被其它线程修改的，因此无法停止运行。要解决这个问题，只要将 stop 前增加 volatile 修饰符即可。volatile 解决了如下两个问题。第一，主线程对 stop 的修改在 workThread 线程 中可见，也就是说 workThread 线程 立即看到了其他线程对于 stop 变量 的修改。第二，禁止指令重排序，防止因为重排序导致的并发访问逻辑混乱。

一些人认为使用 volatile 可以代替传统锁，提升并发性能，这个认识是错误的。volatile 仅仅解决了可见性的问题，但是它并不能保证互斥性，也就是说多个线程并发修改某个变量时，依旧会产生多线程问题。因此，不能靠 volatile 来完全替代传统的锁。根据经验总结，volatile 最适用的场景是 “ 一个线程写，其他线程读 ”，如果有多个线程并发写操作，仍然需要使用锁或者线程安全的容器或者原子变量来代替。下面我们继续对 Netty 的源码做分析。上面讲到了 ioRatio 被定义成 volatile，下面看看代码为什么要这样定义。

```java
    final long ioTime = System.nanoTime() - ioStartTime;
    runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
```

通过代码分析我们发现，在 NioEventLoop 线程 中，ioRatio 并没有被修改，它是只读操作。既然没有修改，为什么要定义成 volatile 呢？继续看代码，我们发现 NioEventLoop 提供了重新设置 IO 执行时间比例的公共方法。

```java
    public void setIoRatio(int ioRatio) {
        if (ioRatio <= 0 || ioRatio > 100) {
            throw new IllegalArgumentException("ioRatio: " + ioRatio + " (expected: 0 < ioRatio <= 100)");
        }
        this.ioRatio = ioRatio;
    }
```

首先，NioEventLoop 线程 没有调用该 set 方法，说明调整 IO 执行时间比例 是外部发起的操作，通常是由业务的线程调用该方法，重新设置该参数。这样就形成了一个线程写、一个线程读。根据前面针对 volatile 的应用总结，此时可以使用 volatile 来代替传统的 synchronized 关键字，以提升并发访问的性能。

## ThreadLocal 的应用及源码解析

ThreadLocal 又称为线程本地存储区（Thread Local Storage，简称为 TLS），每个线程都有自己的私有的本地存储区域，不同线程之间彼此不能访问对方的 TLS 区域。使用 ThreadLocal 变量 的 set(T value)方法 可以将数据存入 该线程本地存储区，使用 get() 方法 可以获取到之前存入的值。

### ThreadLocal 的常见应用

不使用 ThreadLocal。

```java
public class SessionBean {
    public static class Session {
        private String id;
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
    }

    public Session createSession() {
        return new Session();
    }

    public void setId(Session session, String id) {
        session.setId(id);
    }
    public String getId(Session session) {
        return session.getId();
    }
    public static void main(String[] args) {
    //没有使用ThreadLocal，在方法间共享session需要进行session在方法间的传递
        new Thread(() -> {
            SessionBean bean = new SessionBean();
            Session session = bean.createSession();
            bean.setId(session, "susan");
            System.out.println(bean.getId(session));
        }).start();
    }
}
```

上述代码中，session 需要在方法间传递才可以修改和读取，保证线程中各方法操作的是一个。下面看一下使用 ThreadLocal 的代码。

```java
public class SessionBean {
//定义一个静态ThreadLocal变量session，就能够保证各个线程有自己的一份，并且方法可以方便获取，不用传递
    private static ThreadLocal<Session> session = new ThreadLocal<>();
    public static class Session {
        private String id;
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
    }

    public void createSession() {
        session.set(new Session());
    }

    public void setId(String id) {
        session.get().setId(id);
    }

    public String getId() {
        return session.get().getId();
    }

    public static void main(String[] args) {
        new Thread(() -> {
            SessionBean bean = new SessionBean();
            bean.createSession();
            bean.setId("susan");
            System.out.println(bean.getId());
        }).start();
    }
}
```

在方法的内部实现中，直接可以通过 session.get() 获取到当前线程的 session，省掉了参数在方法间传递的环节。

### ThreadLocal 的实现原理

一般，类属性中的数据是多个线程共享的，但 ThreadLocal 类型的数据 声明为类属性，却可以为每一个使用它（通过 set(T value)方法）的线程存储 线程私有的数据，通过其源码我们可以发现其中的原理。

```java
public class ThreadLocal<T> {

    /**
     * 下面的 getMap()方法 传入当前线程，获得一个ThreadLocalMap对象，说明每一个线程维护了
     * 自己的一个 map，保证读取出来的value是自己线程的。
     *
     * ThreadLocalMap 是ThreadLocal静态内部类，存储value的键值就是ThreadLocal本身。
     *
     * 因此可以断定，每个线程维护一个ThreadLocalMap的键值对映射Map。不同线程的Map的 key值 是一样的，
     * 都是ThreadLocal，但 value 是不同的。
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

    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
}
```

### ThreadLocal 在 Spring 中的使用

Spring 事务处理的设计与实现中大量使用了 ThreadLocal 类，比如，TransactionSynchronizationManager 维护了一系列的 ThreadLocal 变量，用于存储线程私有的 事务属性及资源。源码如下。

```java
/**
 * 管理每个线程的资源和事务同步的中心帮助程序。供资源管理代码使用，但不供典型应用程序代码使用。
 *
 * 资源管理代码应该检查线程绑定的资源，如，JDBC连接 或 Hibernate Sessions。
 * 此类代码通常不应该将资源绑定到线程，因为这是事务管理器的职责。另一个选项是，
 * 如果事务同步处于活动状态，则在首次使用时延迟绑定，以执行跨任意数量资源的事务。
 */
public abstract class TransactionSynchronizationManager {

	/**
	 *  一般是一个线程持有一个 独立的事务，以相互隔离地处理各自的事务。
	 *  所以这里使用了很多 ThreadLocal对象，为每个线程绑定 对应的事务属性及资源，
	 *  以便后续使用时能直接获取。
	 */
	private static final ThreadLocal<Map<Object, Object>> resources =
			new NamedThreadLocal<Map<Object, Object>>("Transactional resources");

	private static final ThreadLocal<Set<TransactionSynchronization>> synchronizations =
			new NamedThreadLocal<Set<TransactionSynchronization>>("Transaction synchronizations");

	private static final ThreadLocal<String> currentTransactionName =
			new NamedThreadLocal<String>("Current transaction name");

	private static final ThreadLocal<Boolean> currentTransactionReadOnly =
			new NamedThreadLocal<Boolean>("Current transaction read-only status");

	private static final ThreadLocal<Integer> currentTransactionIsolationLevel =
			new NamedThreadLocal<Integer>("Current transaction isolation level");

	private static final ThreadLocal<Boolean> actualTransactionActive =
			new NamedThreadLocal<Boolean>("Actual transaction active");

	/**
	 * 为当前线程 绑定 对应的resource资源
	 */
	public static void bindResource(Object key, Object value) throws IllegalStateException {
		Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
		Assert.notNull(value, "Value must not be null");
		Map<Object, Object> map = resources.get();
		// 如果当前线程的 resources中，绑定的数据map为空，则为 resources 绑定 map
		if (map == null) {
			map = new HashMap<Object, Object>();
			resources.set(map);
		}
		Object oldValue = map.put(actualKey, value);
		if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
			oldValue = null;
		}
		if (oldValue != null) {
			throw new IllegalStateException("Already value [" + oldValue + "] for key [" +
					actualKey + "] bound to thread [" + Thread.currentThread().getName() + "]");
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Bound value [" + value + "] for key [" + actualKey + "] to thread [" +
					Thread.currentThread().getName() + "]");
		}
	}

	/**
	 * 返回当前线程绑定的所有资源
	 */
	public static Map<Object, Object> getResourceMap() {
		Map<Object, Object> map = resources.get();
		return (map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap());
	}
}
```

### ThreadLocal 在 Mybatis 中的使用

Mybatis 的 SqlSession 对象 也是各线程私有的资源，所以对其的管理也使用到了 ThreadLocal 类。源码如下。

```java
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

  private final ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<>();

  public void startManagedSession() {
    this.localSqlSession.set(openSession());
  }

  public void startManagedSession(boolean autoCommit) {
    this.localSqlSession.set(openSession(autoCommit));
  }

  public void startManagedSession(Connection connection) {
    this.localSqlSession.set(openSession(connection));
  }

  public void startManagedSession(TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(level));
  }

  public void startManagedSession(ExecutorType execType) {
    this.localSqlSession.set(openSession(execType));
  }

  public void startManagedSession(ExecutorType execType, boolean autoCommit) {
    this.localSqlSession.set(openSession(execType, autoCommit));
  }

  public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(execType, level));
  }

  public void startManagedSession(ExecutorType execType, Connection connection) {
    this.localSqlSession.set(openSession(execType, connection));
  }

  public boolean isManagedSessionStarted() {
    return this.localSqlSession.get() != null;
  }

  @Override
  public Connection getConnection() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
    }
    return sqlSession.getConnection();
  }

  @Override
  public void clearCache() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
    }
    sqlSession.clearCache();
  }

  @Override
  public void commit() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit();
  }

  @Override
  public void commit(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit(force);
  }

  @Override
  public void rollback() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback();
  }

  @Override
  public void rollback(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback(force);
  }

  @Override
  public List<BatchResult> flushStatements() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    return sqlSession.flushStatements();
  }

  @Override
  public void close() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
    }
    try {
      sqlSession.close();
    } finally {
      localSqlSession.set(null);
    }
  }
}
```

## J.U.C 包的实际应用

### 线程池 ThreadPoolExecutor

首先通过 ThreadPoolExecutor 的源码 看一下线程池的主要参数及方法。

```java
public class ThreadPoolExecutor extends AbstractExecutorService {

    /**
     * 核心线程数
     * 当向线程池提交一个任务时，若线程池已创建的线程数小于corePoolSize，即便此时存在空闲线程，
     * 也会通过创建一个新线程来执行该任务，直到已创建的线程数大于或等于corePoolSize
     */
    private volatile int corePoolSize;

    /**
     * 最大线程数
     * 当队列满了，且已创建的线程数小于maximumPoolSize，则线程池会创建新的线程来执行任务。
     * 另外，对于无界队列，可忽略该参数
     */
    private volatile int maximumPoolSize;
    /**
     * 线程存活保持时间
     * 当线程池中线程数 超出核心线程数，且线程的空闲时间也超过 keepAliveTime时，
     * 那么这个线程就会被销毁，直到线程池中的线程数小于等于核心线程数
     */
    private volatile long keepAliveTime;

    /**
     * 任务队列
     * 用于传输和保存等待执行任务的阻塞队列
     */
    private final BlockingQueue<Runnable> workQueue;

    /**
     * 线程工厂
     * 用于创建新线程。threadFactory 创建的线程也是采用 new Thread() 方式，threadFactory
     * 创建的线程名都具有统一的风格：pool-m-thread-n（m为线程池的编号，n为线程池中线程的编号
     */
    private volatile ThreadFactory threadFactory;

    /**
     * 线程饱和策略
     * 当线程池和队列都满了，再加入的线程会执行此策略
     */
    private volatile RejectedExecutionHandler handler;

    /**
     * 构造方法提供了多种重载，但实际上都使用了最后一个重载 完成了实例化
     */
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
     * 执行一个任务，但没有返回值
     */
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();
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
     * 提交一个线程任务，有返回值。该方法继承自其父类 AbstractExecutorService，有多种重载，这是最常用的一个。
     * 通过future.get()获取返回值（阻塞直到任务执行完）
     */
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }

    /**
     * 关闭线程池，不再接收新的任务，但会把已有的任务执行完
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

    /**
     * 立即关闭线程池，已有的任务也会被抛弃
     */
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();
            advanceRunState(STOP);
            interruptWorkers();
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }
        tryTerminate();
        return tasks;
    }

    public boolean isShutdown() {
        return ! isRunning(ctl.get());
    }
}
```

线程池执行流程，如下图所示。

![image-20220125231944268](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/image-20220125231944268-e62e34.png)

#### Executors 提供的 4 种线程池

Executors 类 通过 ThreadPoolExecutor 封装了 4 种常用的线程池：CachedThreadPool，FixedThreadPool，ScheduledThreadPool 和 SingleThreadExecutor。其功能如下。

1. CachedThreadPool：用来创建一个几乎可以无限扩大的线程池（最大线程数为 Integer.MAX_VALUE），适用于执行大量短生命周期的异步任务。
2. FixedThreadPool：创建一个固定大小的线程池，保证线程数可控，不会造成线程过多，导致系统负载更为严重。
3. SingleThreadExecutor：创建一个单线程的线程池，可以保证任务按调用顺序执行。
4. ScheduledThreadPool：适用于执行 延时 或者 周期性 任务。

#### 如何配置线程池

- **CPU 密集型任务**  
  尽量使用较小的线程池，一般为 CPU 核心数+1。 因为 CPU 密集型任务 使得 CPU 使用率 很高，若开过多的线程数，会造成 CPU 过度切换。

- **IO 密集型任务**  
  可以使用稍大的线程池，一般为 2\*CPU 核心数。 IO 密集型任务 CPU 使用率 并不高，因此可以让 CPU 在等待 IO 的时候有其他线程去处理别的任务，充分利用 CPU 时间。

#### 线程池的实际应用

Tomcat 在分发 web 请求 时使用了线程池来处理。

### BlockingQueue

#### 核心方法

```java
public interface BlockingQueue<E> extends Queue<E> {

    // 将给定元素设置到队列中，如果设置成功返回true, 否则返回false。如果是往限定了长度的队列中设置值，推荐使用offer()方法。
    boolean add(E e);

    // 将给定的元素设置到队列中，如果设置成功返回true, 否则返回false. e的值不能为空，否则抛出空指针异常。
    boolean offer(E e);

    // 将元素设置到队列中，如果队列中没有多余的空间，该方法会一直阻塞，直到队列中有多余的空间。
    void put(E e) throws InterruptedException;

    // 将给定元素在给定的时间内设置到队列中，如果设置成功返回true, 否则返回false.
    boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    // 从队列中获取值，如果队列中没有值，线程会一直阻塞，直到队列中有值，并且该方法取得了该值。
    E take() throws InterruptedException;

    // 在给定的时间里，从队列中获取值，时间到了直接调用普通的 poll()方法，为null则直接返回null。
    E poll(long timeout, TimeUnit unit)
        throws InterruptedException;

    // 获取队列中剩余的空间。
    int remainingCapacity();

    // 从队列中移除指定的值。
    boolean remove(Object o);

    // 判断队列中是否拥有该值。
    public boolean contains(Object o);

    // 将队列中值，全部移除，并发设置到给定的集合中。
    int drainTo(Collection<? super E> c);

    // 指定最多数量限制将队列中值，全部移除，并发设置到给定的集合中。
    int drainTo(Collection<? super E> c, int maxElements);
}
```

#### 主要实现类

- **ArrayBlockingQueue**  
  基于数组的阻塞队列实现，在 ArrayBlockingQueue 内部，维护了一个定长数组，以便缓存队列中的数据对象，这是一个常用的阻塞队列，除了一个定长数组外，ArrayBlockingQueue 内部还保存着两个整形变量，分别标识着队列的头部和尾部在数组中的位置。  
  ArrayBlockingQueue 在生产者放入数据 和 消费者获取数据时，都是共用同一个锁对象，由此也意味着两者无法真正并行运行，这点尤其不同于 LinkedBlockingQueue。ArrayBlockingQueue 和 LinkedBlockingQueue 间还有一个明显的不同之处在于，前者在插入或删除元素时不会产生或销毁任何额外的对象实例，而后者则会生成一个额外的 Node 对象。这在长时间内需要高效并发地处理大批量数据的系统中，其对于 GC 的影响还是存在一定的区别。而在创建 ArrayBlockingQueue 时，我们还可以控制对象的内部锁是否采用公平锁，默认采用非公平锁。

- **LinkedBlockingQueue**  
  基于链表的阻塞队列，同 ArrayListBlockingQueue 类似，其内部也维持着一个数据缓冲队列（该队列由一个链表构成），当生产者往队列中放入一个数据时，队列会从生产者手中获取数据，并缓存在队列内部，而生产者立即返回；只有当队列缓冲区达到最大值缓存容量时（LinkedBlockingQueue 可以通过构造函数指定该值），才会阻塞生产者队列，直到消费者从队列中消费掉一份数据，生产者线程会被唤醒，反之对于消费者这端的处理也基于同样的原理。而 LinkedBlockingQueue 之所以能够高效的处理并发数据，还因为其对于生产者端和消费者端分别采用了独立的锁来控制数据同步，这也意味着在高并发的情况下生产者和消费者可以并行地操作队列中的数据，以此来提高整个队列的并发性能。  
  需要注意的是，如果构造一个 LinkedBlockingQueue 对象，而没有指定其容量大小，LinkedBlockingQueue 会默认一个类似无限大小的容量（Integer.MAX_VALUE），这样的话，如果生产者的速度一旦大于消费者的速度，也许还没有等到队列满阻塞产生，系统内存就有可能已被消耗殆尽了。

- **PriorityBlockingQueue**  
  基于优先级的阻塞队列（优先级的判断通过构造函数传入的 Compator 对象来决定），但需要注意的是 PriorityBlockingQueue 并不会阻塞数据生产者，而只会在没有可消费的数据时，阻塞数据的消费者。因此使用的时候要特别注意，生产者生产数据的速度绝对不能快于消费者消费数据的速度，否则时间一长，会最终耗尽所有的可用堆内存空间。在实现 PriorityBlockingQueue 时，内部控制线程同步的锁采用的是公平锁。

### CAS 指令和原子类（应用比较多的就是计数器）

互斥同步最主要的问题就是进行线程阻塞和唤醒所带来的性能的额外损耗，因此这种同步被称为**阻塞同步**，它属于一种**悲观的并发策略，我们称之为悲观锁**。随着硬件和操作系统指令集的发展和优化，产生了**非阻塞同步**，被称为**乐观锁**。简单地说，就是**先进行操作，操作完成之后再判断操作是否成功，是否有并发问题，如果有则进行失败补偿，如果没有就算操作成功**，这样就从根本上避免了同步锁的弊端。

目前，在 Java 中应用最广泛的非阻塞同步就是 CAS。从 JDK1.5 以后，可以使用 CAS 操作，该操作由 sun.misc.Unsafe 类里的 compareAndSwapInt() 和 compareAndSwapLong() 等方法实现。通常情况下 sun.misc.Unsafe 类 对于开发者是不可见的，因此，JDK 提供了很多 CAS 包装类 简化开发者的使用，如 AtomicInteger。使用 Java 自带的 Atomic 原子类，可以避免同步锁带来的并发访问性能降低的问题，减少犯错的机会。



# 从Spring及Mybatis框架源码中学习设计模式(创建型)

设计模式是解决问题的方案，从大神的代码中学习对设计模式的使用，可以有效提升个人编码及设计代码的能力。本系列博文用于总结阅读过的框架源码（Spring 系列、Mybatis）及 JDK 源码中 所使用过的设计模式，并结合个人工作经验，重新理解设计模式。

本篇博文主要看一下创建型的几个设计模式，即，单例模式、各种工厂模式 及 建造者模式。

## 单例模式

### 个人理解

确保某个类只有一个实例，并提供该实例的获取方法。实际应用很多，不管是框架、JDK 还是实际的项目开发，但大都会使用“饿汉式”或“枚举”来实现单例。“懒汉式”也有一些应用，但通过“双检锁机制”来保证单例的实现很少见。

### 实现方式

最简单的就是 使用一个私有构造函数、一个私有静态变量，以及一个公共静态方法的方式来实现。懒汉式、饿汉式等简单实现就不多 BB 咯，这里强调一下双检锁懒汉式实现的坑，以及枚举方式的实现吧，最后再结合 spring 源码 扩展一下单例 bean 的实现原理。

**1. 双检锁实现的坑**

```java
/**
* @author 云之君
* 双检锁 懒汉式，实现线程安全的单例
* 关键词：JVM指令重排、volatile、反射攻击
*/
public class Singleton3 {
    /**
     * 对于我们初级开发来说，这个volatile在实际开发中可能见过，但很少会用到
     * 这里加个volatile进行修饰，也是本单例模式的精髓所在。
     * 下面的 instance = new Singleton3(); 这行代码在JVM中其实是分三步执行的：
     * 1、分配内存空间；
     * 2、初始化对象；
     * 3、将instance指向分配的内存地址。
     * 但JVM具有指令重排的特性，实际的执行顺序可能会是1、3、2，导致多线程情况下出问题，
     * 使用volatile修饰instance变量 可以 避免上述的指令重排
     * tips：不太理解的是 第一个线程在执行第2步之前就已经释放了锁吗？导致其它线程进入synchronized代码块
     *      执行 instance == null 的判断？
     *  回答：第一个线程在执行第2步之前就已经释放了锁吗？（没有）。如果不使用volatile修饰instance变量，那么其他线程进来的时候，看到的instance就有可能不是null的，因为已经执行了第3步，那么此时这个线程（执行 return instance;）使用的instance是一个没有初始化的instance，就会有问题。
     */
    private volatile static Singleton3 instance;

    private Singleton3(){

    }

    public static Singleton3 getInstance(){
        if(instance == null){
            synchronized(Singleton3.class){
                if(instance == null){
                    instance = new Singleton3();
                }
            }
        }
        return instance;
    }
}
```

**2. 枚举实现**  
其它的单例模式实现往往都会面临序列化 和 反射攻击的问题，比如上面的 Singleton3 如果实现了 Serializable 接口，那么在每次序列化时都会创建一个新对象，若要保证单例，必须声明所有字段都是 transient 的，并且提供一个 readResolve()方法。反射攻击可以通过 setAccessible()方法将私有的构造方法公共化，进而实例化。若要防止这种攻击，就需要在构造方法中添加 防止实例化第二个对象的代码。

枚举实现的单例在面对 复杂的序列化及反射攻击时，依然能够保持自己的单例状态，所以被认为是单例的最佳实践。比如，mybatis 在定义 SQL 命令类型时就使用到了枚举。

```java
package org.apache.ibatis.mapping;

/**
 * @author Clinton Begin
 */
public enum SqlCommandType {
  UNKNOWN, INSERT, UPDATE, DELETE, SELECT, FLUSH;
}
```

### JDK 中的范例

**1. java.lang.Runtime**

```java
/**
 * 每个Java应用程序都有一个单例的Runtime对象，通过getRuntime()方法获得
 * @author  unascribed
 * @see     java.lang.Runtime#getRuntime()
 * @since   JDK1.0
 */
public class Runtime {
    /** 很明显，这里用的是饿汉式 实现单例 */
    private static Runtime currentRuntime = new Runtime();

    public static Runtime getRuntime() {
        return currentRuntime;
    }

    /** Don't let anyone else instantiate this class */
    private Runtime() {}
}
```

**2. java.awt.Desktop**

```java
public class Desktop {

    /**
     * Suppresses default constructor for noninstantiability.
     */
    private Desktop() {
        peer = Toolkit.getDefaultToolkit().createDesktopPeer(this);
    }

    /**
     * 由于对象较大，这里使用了懒汉式延迟加载，方式比较简单，直接把锁加在方法上。
     * 使用双检锁方式实现的单例 还没怎么碰到过，有经验的小伙伴 欢迎留言补充
     */
    public static synchronized Desktop getDesktop(){
        if (GraphicsEnvironment.isHeadless()) throw new HeadlessException();
        if (!Desktop.isDesktopSupported()) {
            throw new UnsupportedOperationException("Desktop API is not " +
                                                    "supported on the current platform");
        }

        sun.awt.AppContext context = sun.awt.AppContext.getAppContext();
        Desktop desktop = (Desktop)context.get(Desktop.class);

        if (desktop == null) {
            desktop = new Desktop();
            context.put(Desktop.class, desktop);
        }

        return desktop;
    }
}
```

### Spring 的单例 bean 是如何实现的？

Spring 实现单例 bean 是使用 map 注册表和 synchronized 同步机制实现的，通过分析 spring 的 AbstractBeanFactory 中的 doGetBean 方法和 DefaultSingletonBeanRegistry 的 getSingleton()方法，可以理解其实现原理。

```java
public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

    ......

    /**
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * 真正实现向IOC容器获取Bean的功能，也是触发依赖注入(DI)功能的地方
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    @SuppressWarnings("unchecked")
    protected <T> T doGetBean(final String name, final Class<T> requiredType, final Object[] args,
            boolean typeCheckOnly) throws BeansException {

        ......

        //创建单例模式bean的实例对象
        if (mbd.isSingleton()) {
            //这里使用了一个匿名内部类，创建Bean实例对象，并且注册给所依赖的对象
            sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
                public Object getObject() throws BeansException {
                    try {
                        /**
                         * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
                         * 创建一个指定的Bean实例对象，如果有父级继承，则合并子类和父类的定义
                         * 走子类中的实现
                         * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
                         */
                        return createBean(beanName, mbd, args);
                    }
                    catch (BeansException ex) {
                        destroySingleton(beanName);
                        throw ex;
                    }
                }
            });
            //获取给定Bean的实例对象
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
        }

        ......

    }
}


/**
 * 默认的单例bean注册器
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

    /** 单例的bean实例的缓存  */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(64);

    /**
     * 返回给定beanName的 已经注册的 单例bean，如果没有注册，则注册并返回
     */
    public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        Assert.notNull(beanName, "'beanName' must not be null");

        // 加锁，保证单例bean在多线程环境下不会创建多个
        synchronized (this.singletonObjects) {
            // 先从缓存中取，有就直接返回，没有就创建、注册到singletonObjects、返回
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null) {
                if (this.singletonsCurrentlyInDestruction) {
                    throw new BeanCreationNotAllowedException(beanName,
                            "Singleton bean creation not allowed while the singletons of this factory are in destruction " +
                            "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
                }
                beforeSingletonCreation(beanName);
                boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
                if (recordSuppressedExceptions) {
                    this.suppressedExceptions = new LinkedHashSet<Exception>();
                }
                try {
                    singletonObject = singletonFactory.getObject();
                }
                catch (BeanCreationException ex) {
                    if (recordSuppressedExceptions) {
                        for (Exception suppressedException : this.suppressedExceptions) {
                            ex.addRelatedCause(suppressedException);
                        }
                    }
                    throw ex;
                }
                finally {
                    if (recordSuppressedExceptions) {
                        this.suppressedExceptions = null;
                    }
                    afterSingletonCreation(beanName);
                }
                // 注册到单例bean的缓存
                addSingleton(beanName, singletonObject);
            }
            return (singletonObject != NULL_OBJECT ? singletonObject : null);
        }
    }
}
```

## 简单工厂模式

### 个人理解

把同一系列类的实例化交由一个工厂类进行集中管控。与其说它是一种设计模式，倒不如把它看成一种编程习惯，因为它不符合“开闭原则”，增加新的产品类需要修改工厂类的代码。

### 简单实现

```java
public interface Hero {
    void speak();
}

public class DaJi implements Hero {
    @Override
    public void speak() {
        System.out.println("妲己，陪你玩 ~");
    }
}

public class LiBai implements Hero{
    @Override
    public void speak() {
        System.out.println("今朝有酒 今朝醉 ~");
    }
}

/** 对各种英雄进行集中管理 */
public class HeroFactory {
    public static Hero getShibing(String name){
        if("LiBai".equals(name))
            return new LiBai();
        else if("DaJi".equals(name))
            return new DaJi();
        else
            return null;
    }
}
```

这种设计方式只在我们产品的“FBM 资金管理”模块有看到过，其中对 100+个按钮类进行了集中管控，不过其设计结构比上面这种要复杂的多。

## 工厂方法模式

### 个人理解

在顶级工厂（接口/抽象类）中定义 产品类的获取方法，由具体的子工厂实例化对应的产品，一般是一个子工厂对应一个特定的产品，实现对产品的集中管控，并且符合“开闭原则”。

### Mybatis 中的范例

mybatis 中数据源 DataSource 的获取使用到了该设计模式。接口 DataSourceFactory 定义了获取 DataSource 对象的方法，各实现类 完成了获取对应类型的 DataSource 对象的实现。(mybatis 的源码都是缩进两个空格，难道国外的编码规范有独门派系？)

```java
public interface DataSourceFactory {

  // 设置DataSource的属性，一般紧跟在DataSource初始化之后
  void setProperties(Properties props);

  // 获取DataSource对象
  DataSource getDataSource();
}


public class JndiDataSourceFactory implements DataSourceFactory {

  private DataSource dataSource;

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  @Override
  public void setProperties(Properties properties) {
    try {
      InitialContext initCtx;
      Properties env = getEnvProperties(properties);
      if (env == null) {
        initCtx = new InitialContext();
      } else {
        initCtx = new InitialContext(env);
      }

      if (properties.containsKey(INITIAL_CONTEXT)
          && properties.containsKey(DATA_SOURCE)) {
        Context ctx = (Context) initCtx.lookup(properties.getProperty(INITIAL_CONTEXT));
        dataSource = (DataSource) ctx.lookup(properties.getProperty(DATA_SOURCE));
      } else if (properties.containsKey(DATA_SOURCE)) {
        dataSource = (DataSource) initCtx.lookup(properties.getProperty(DATA_SOURCE));
      }

    } catch (NamingException e) {
      throw new DataSourceException("There was an error configuring JndiDataSourceTransactionPool. Cause: " + e, e);
    }
  }
}


public class UnpooledDataSourceFactory implements DataSourceFactory {

  protected DataSource dataSource;

  // 在实例化该工厂时，就完成了DataSource的实例化
  public UnpooledDataSourceFactory() {
    this.dataSource = new UnpooledDataSource();
  }

  @Override
  public DataSource getDataSource() {
    return dataSource;
  }
}


public class PooledDataSourceFactory extends UnpooledDataSourceFactory {

  // 与UnpooledDataSourceFactory的不同之处是，其初始化的DataSource为PooledDataSource
  public PooledDataSourceFactory() {
    this.dataSource = new PooledDataSource();
  }
}


public interface DataSource  extends CommonDataSource, Wrapper {

  Connection getConnection() throws SQLException;

  Connection getConnection(String username, String password)
    throws SQLException;
}
```

DataSource 最主要的几个实现类内容都比较多，代码就不贴出来咯，感兴趣的同学可以到我的源码分析专题中看到详细解析。

**tips：什么时候该用简单工厂模式？什么时候该用工厂方法模式呢？**  
个人认为，工厂方法模式符合“开闭原则”，增加新的产品类不用修改代码，应当优先考虑使用这种模式。如果产品类结构简单且数量庞大时，还是使用简单工厂模式更容易维护些，如：上百个按钮类。

## 抽象工厂模式

### 个人理解

设计结构上与“工厂方法”模式很像，最主要的区别是，工厂方法模式中 一个子工厂只对应**一个**具体的产品，而抽象工厂模式中，一个子工厂对应**一组**具有相关性的产品，即，存在多个获取不同产品的方法。这种设计模式也很少见人用，倒是“工厂方法”模式见的最多。

### 简单实现

```java
public abstract class AbstractFactory {

    abstract protected AbstractProductA createProductA();

    abstract protected AbstractProductB createProductB();
}


public class ConcreteFactory1 extends AbstractFactory {

    @Override
    protected AbstractProductA createProductA() {
        return new ProductA1();
    }

    @Override
    protected AbstractProductB createProductB() {
        return new ProductB1();
    }
}


public class ConcreteFactory2 extends AbstractFactory {

    @Override
    protected AbstractProductA createProductA() {
        return new ProductA2();
    }

    @Override
    protected AbstractProductB createProductB() {
        return new ProductB2();
    }
}


public class Client {

    public static void main(String[] args) {
        AbstractFactory factory = new ConcreteFactory1();
        AbstractProductA productA = factory.createProductA();
        AbstractProductB productB = factory.createProductB();

        ...
        // 结合使用productA和productB进行后续操作
        ...
    }
}
```

### JDK 中的范例

JDK 的 javax.xml.transform.TransformerFactory 组件使用了类似“抽象工厂”模式的设计，抽象类 TransformerFactory 定义了两个抽象方法 newTransformer()和 newTemplates()分别用于生成 Transformer 对象 和 Templates 对象，其两个子类进行了不同的实现，源码如下（版本 1.8）。

```java
public abstract class TransformerFactory {

    public abstract Transformer newTransformer(Source source)
        throws TransformerConfigurationException;

    public abstract Templates newTemplates(Source source)
        throws TransformerConfigurationException;
}


/**
 * SAXTransformerFactory 继承了 TransformerFactory
 */
public class TransformerFactoryImpl
    extends SAXTransformerFactory implements SourceLoader, ErrorListener {

    @Override
    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        final Templates templates = newTemplates(source);
        final Transformer transformer = templates.newTransformer();
        if (_uriResolver != null) {
            transformer.setURIResolver(_uriResolver);
        }
        return(transformer);
    }


    @Override
    public Templates newTemplates(Source source) throws TransformerConfigurationException {

        ......

        return new TemplatesImpl(bytecodes, transletName,
            xsltc.getOutputProperties(), _indentNumber, this);
    }
}


public class SmartTransformerFactoryImpl extends SAXTransformerFactory {

    public Transformer newTransformer(Source source) throws TransformerConfigurationException {
        if (_xalanFactory == null) {
            createXalanTransformerFactory();
        }
        if (_errorlistener != null) {
            _xalanFactory.setErrorListener(_errorlistener);
        }
        if (_uriresolver != null) {
            _xalanFactory.setURIResolver(_uriresolver);
        }
        _currFactory = _xalanFactory;
        return _currFactory.newTransformer(source);
    }

    public Templates newTemplates(Source source) throws TransformerConfigurationException {
        if (_xsltcFactory == null) {
            createXSLTCTransformerFactory();
        }
        if (_errorlistener != null) {
            _xsltcFactory.setErrorListener(_errorlistener);
        }
        if (_uriresolver != null) {
            _xsltcFactory.setURIResolver(_uriresolver);
        }
        _currFactory = _xsltcFactory;
        return _currFactory.newTemplates(source);
    }
}
```

## 建造者模式

### 个人理解

该模式主要用于将复杂对象的构建过程分解成一个个简单的步骤，或者分摊到多个类中进行构建，保证构建过程层次清晰，代码不会过分臃肿，屏蔽掉了复杂对象内部的具体构建细节，其类图结构如下所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E5%BB%BA%E9%80%A0%E8%80%85%E6%A8%A1%E5%BC%8F%E7%B1%BB%E5%9B%BE-bba3f6.png)

该模式的主要角色如下：

- 建造者接口（Builder）：用于定义建造者构建产品对象的各种公共行为，主要分为 建造方法 和 获取构建好的产品对象；
- 具体建造者（ConcreteBuilder）：实现上述接口方法；
- 导演（Director）：通过调用具体建造者创建需要的产品对象；
- 产品（Product）：被建造的复杂对象。

其中的导演角色不必了解产品类的内部细节，只提供需要的信息给建造者，由具体建造者处理这些信息（这个处理过程可能会比较复杂）并完成产品构造，使产品对象的上层代码与产品对象的创建过程解耦。建造者模式将复杂产品的创建过程分散到不同的构造步骤中，这样可以对产品创建过程实现更加精细的控制，也会使创建过程更加清晰。每个具体建造者都可以创建出完整的产品对象，而且具体建造者之间是相互独立的， 因此系统就可以通过不同的具体建造者，得到不同的产品对象。当有新产品出现时，无须修改原有的代码，只需要添加新的具体建造者即可完成扩展，这符合“开放一封闭” 原则。

### 典型的范例 StringBuilder 和 StringBuffer

相信在拼 SQL 语句时大家一定经常用到 StringBuffer 和 StringBuilder 这两个类，它们就用到了建造者设计模式，源码如下（版本 1.8）：

```java
abstract class AbstractStringBuilder implements Appendable, CharSequence {

    /**
     * The value is used for character storage.
     */
    char[] value;

    /**
     * The count is the number of characters used.
     */
    int count;

    /**
     * Creates an AbstractStringBuilder of the specified capacity.
     */
    AbstractStringBuilder(int capacity) {
        value = new char[capacity];
    }

    public AbstractStringBuilder append(String str) {
        if (str == null)
            return appendNull();
        int len = str.length();
        ensureCapacityInternal(count + len);
        // 这里完成了对复杂String的构造，将str拼接到当前对象后面
        str.getChars(0, len, value, count);
        count += len;
        return this;
    }
}


/**
 * @since      JDK 1.5
 */
public final class StringBuilder extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence {

    public StringBuilder() {
        super(16);
    }

    @Override
    public StringBuilder append(String str) {
        super.append(str);
        return this;
    }

    @Override
    public String toString() {
        // Create a copy, don't share the array
        return new String(value, 0, count);
    }
}


/**
 * @since      JDK 1.0
 */
public final class StringBuffer extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence {

    /**
     * toString返回的最后一个值的缓存。在修改StringBuffer时清除。
     */
    private transient char[] toStringCache;

    public StringBuffer() {
        super(16);
    }

	/**
	  * 与StringBuilder建造者最大的不同就是，增加了线程安全机制
	  */
    @Override
    public synchronized StringBuffer append(String str) {
        toStringCache = null;
        super.append(str);
        return this;
    }
}
```

### Mybatis 中的范例

MyBatis 的初始化过程使用了建造者模式，抽象类 BaseBuilder 扮演了“建造者接口”的角色，对一些公用方法进行了实现，并定义了公共属性。XMLConfigBuilder、XMLMapperBuilder、XMLStatementBuilder 等实现类扮演了“具体建造者”的角色，分别用于解析 mybatis-config.xml 配置文件、映射配置文件 以及 SQL 节点。Configuration 和 SqlSessionFactoryBuilder 则分别扮演了“产品” 和 “导演”的角色。**即，SqlSessionFactoryBuilder 使用了 BaseBuilder 建造者组件 对复杂对象 Configuration 进行了构建。**

BaseBuilder 组件的设计与上面标准的建造者模式是有很大不同的，BaseBuilder 的建造者模式主要是为了将复杂对象 Configuration 的构建过程分解的层次更清晰，将整个构建过程分解到多个“具体构造者”类中，需要这些“具体构造者”共同配合才能完成 Configuration 的构造，单个“具体构造者”不具有单独构造产品的能力，这与 StringBuilder 及 StringBuffer 是不同的。

个人理解的构建者模式 其核心就是用来构建复杂对象的，比如 mybatis 对 Configuration 对象的构建。当然，我们也可以把 对这个对象的构建过程 写在一个类中，来满足我们的需求，但这样做的话，这个类就会变得及其臃肿，难以维护。所以把整个构建过程合理地拆分到多个类中，分别构建，整个代码就显得非常规整，且思路清晰，而且 建造者模式符合 开闭原则。其源码实现如下。

```java
public abstract class BaseBuilder {

  /**
   * Configuration 是 MyBatis 初始化过程的核心对象并且全局唯一，
   * MyBatis 中几乎全部的配置信息会保存到Configuration 对象中。
   * 也有人称它是一个“All-In-One”配置对象
   */
  protected final Configuration configuration;

  /**
   * 在 mybatis-config.xml 配置文件中可以使用<typeAliases>标签定义别名，
   * 这些定义的别名都会记录在该 TypeAliasRegistry 对象中
   */
  protected final TypeAliasRegistry typeAliasRegistry;

  /**
   * 在 mybatis-config.xml 配置文件中可以使用<typeHandlers>标签添加自定义
   * TypeHandler，完成指定数据库类型与 Java 类型的转换，这些 TypeHandler
   * 都会记录在 TypeHandlerRegistry 中
   */
  protected final TypeHandlerRegistry typeHandlerRegistry;

  /**
   * BaseBuilder 中记录的 TypeAliasRegistry 对象和 TypeHandlerRegistry 对象，
   * 其实是全局唯一的，它们都是在 Configuration 对象初始化时创建的
   */
  public BaseBuilder(Configuration configuration) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
  }
}


public class XMLConfigBuilder extends BaseBuilder {

  /** 标识是否已经解析过 mybatis-config.xml 配置文件 */
  private boolean parsed;
  /** 用于解析 mybatis-config.xml 配置文件 */
  private final XPathParser parser;
  /** 标识 <environment> 配置的名称，默认读取 <environment> 标签的 default 属性 */
  private String environment;
  /** 负责创建和缓存 Reflector 对象 */
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    // 在 mybatis-config.xml 配置文件中查找<configuration>节点，并开始解析
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first
      // 解析<properties>节点
      propertiesElement(root.evalNode("properties"));
      // 解析<settings>节点
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      loadCustomLogImpl(settings);
      // 解析<typeAliases>节点
      typeAliasesElement(root.evalNode("typeAliases"));
      // 解析<plugins>节点
      pluginElement(root.evalNode("plugins"));
      // 解析<objectFactory>节点
      objectFactoryElement(root.evalNode("objectFactory"));
      // 解析<objectWrapperFactory>节点
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      // 解析<reflectorFactory>节点
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      // 解析<environments>节点
      environmentsElement(root.evalNode("environments"));
      // 解析<databaseIdProvider>节点
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      // 解析<typeHandlers>节点
      typeHandlerElement(root.evalNode("typeHandlers"));
      // 解析<mappers>节点
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }
}


public class XMLMapperBuilder extends BaseBuilder {

  private final XPathParser parser;
  private final MapperBuilderAssistant builderAssistant;
  private final Map<String, XNode> sqlFragments;
  private final String resource;

  public void parse() {
    // 判断是否已经加载过该映射文件
    if (!configuration.isResourceLoaded(resource)) {
      // 处理<mapper>节点
      configurationElement(parser.evalNode("/mapper"));
      // 将 resource 添加到 Configuration.loadedResources 集合中保存，
      // 它是 HashSet<String> 类型的集合，其中记录了已经加载过的映射文件
      configuration.addLoadedResource(resource);
      // 注册 Mapper 接口
      bindMapperForNamespace();
    }
    // 处理 configurationElement() 方法中解析失败的<resultMap>节点
    parsePendingResultMaps();
    // 处理 configurationElement() 方法中解析失败的<cache-ref>节点
    parsePendingCacheRefs();
    // 处理 configurationElement() 方法中解析失败的 SQL 语句节点
    parsePendingStatements();
  }

  private void configurationElement(XNode context) {
    try {
      // 获取<mapper>节点的 namespace 属性,若 namespace 属性为空，则抛出异常
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      // 设置 MapperBuilderAssistant 的 currentNamespace 字段，记录当前命名空间
      builderAssistant.setCurrentNamespace(namespace);
      // 解析<cache-ref>节点
      cacheRefElement(context.evalNode("cache-ref"));
      // 解析<cache>节点
      cacheElement(context.evalNode("cache"));
      // 解析<parameterMap>节点,(该节点 已废弃，不再推荐使用)
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      // 解析<resultMap>节点
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      // 解析<sql>节点
      sqlElement(context.evalNodes("/mapper/sql"));
      // 解析<select>、<insert>、<update>、<delete>等SQL节点
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }
}


public class XMLStatementBuilder extends BaseBuilder {

  private final MapperBuilderAssistant builderAssistant;
  private final XNode context;
  private final String requiredDatabaseId;

  public void parseStatementNode() {
    // 获取 SQL 节点的 id 以及 databaseId 属性，若其 databaseId属性值与当前使用的数据库不匹配，
    // 则不加载该 SQL 节点；若存在相同 id 且 databaseId 不为空的 SQL 节点，则不再加载该 SQL 节点
    String id = context.getStringAttribute("id");
    String databaseId = context.getStringAttribute("databaseId");

    if (!databaseIdMatchesCurrent(id, databaseId, this.requiredDatabaseId)) {
      return;
    }

    // 根据 SQL 节点的名称决定其 SqlCommandType
    String nodeName = context.getNode().getNodeName();
    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);

    // 在解析 SQL 语句之前，先处理其中的<include>节点
    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
    includeParser.applyIncludes(context.getNode());

    String parameterType = context.getStringAttribute("parameterType");
    Class<?> parameterTypeClass = resolveClass(parameterType);

    String lang = context.getStringAttribute("lang");
    LanguageDriver langDriver = getLanguageDriver(lang);

    // 处理<selectKey>节点
    processSelectKeyNodes(id, parameterTypeClass, langDriver);

    // Parse the SQL (pre: <selectKey> and <include> were parsed and removed)
    KeyGenerator keyGenerator;
    String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
    if (configuration.hasKeyGenerator(keyStatementId)) {
      keyGenerator = configuration.getKeyGenerator(keyStatementId);
    } else {
      keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
          configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
          ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
    }

    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
    StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    Integer fetchSize = context.getIntAttribute("fetchSize");
    Integer timeout = context.getIntAttribute("timeout");
    String parameterMap = context.getStringAttribute("parameterMap");
    String resultType = context.getStringAttribute("resultType");
    Class<?> resultTypeClass = resolveClass(resultType);
    String resultMap = context.getStringAttribute("resultMap");
    String resultSetType = context.getStringAttribute("resultSetType");
    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
    if (resultSetTypeEnum == null) {
      resultSetTypeEnum = configuration.getDefaultResultSetType();
    }
    String keyProperty = context.getStringAttribute("keyProperty");
    String keyColumn = context.getStringAttribute("keyColumn");
    String resultSets = context.getStringAttribute("resultSets");

    builderAssistant.addMappedStatement(id, sqlSource, statementType, sqlCommandType,
        fetchSize, timeout, parameterMap, parameterTypeClass, resultMap, resultTypeClass,
        resultSetTypeEnum, flushCache, useCache, resultOrdered,
        keyGenerator, keyProperty, keyColumn, databaseId, langDriver, resultSets);
  }
}


public class SqlSessionFactoryBuilder {

  public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
      // 读取配置文件
      XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
      // 解析配置文件得到 Configuration 对象，然后用其创建 DefaultSqlSessionFactory 对象
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        inputStream.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
  }
}
```

设计模式是解决问题的方案，从大神的代码中学习对设计模式的使用，可以有效提升个人编码及设计代码的能力。本系列博文用于总结阅读过的框架源码（Spring 系列、Mybatis）及 JDK 源码中 所使用过的设计模式，并结合个人工作经验，重新理解设计模式。



# 从Spring及Mybatis框架源码中学习设计模式(结构型)

> 本篇博文主要看一下结构型的几个设计模式，即，适配器模式、代理模式 及 装饰器模式。

## 适配器模式

#### 个人理解

从名字就很好理解，主要起到一个连接适配的作用。生活中也有很多这样的例子，比如我们给笔记本充电，不能直接使用国家标准电源，都需要一个“电源适配器”来适配电源输入的电流。使用适配器模式最大的好处就是复用现有组件。应用程序需要复用现有的类，但接口不能被该应用程序兼容，则无法直接使用。这种场景下就适合使用适配器模式实现接口的适配，从而完成组件的复用。

很明显，适配器模式通过提供 Adapter 的方式完成接口适配，实现了程序复用 Adaptee(被适配者) 的需求，避免了修改 Adaptee 实现接口，当有新的 Adaptee 需要被复用时，只要添加新的 Adapter 即可，这是符合“开放封闭”原则的。

本模式的应用也比较广泛，因为实际的开发中也有很多适配工作要做，所以 这些都可以考虑使用适配器模式。在 spring 及 mybatis 中也使用了本模式，分析如下。

#### Spring 中的应用

Spring 在 AOP 模块中，设计了一套 AdvisorAdapter 组件，将各种 Advice 对象适配成了相对应的 MethodInterceptor 对象。其中，AfterReturningAdviceAdapter、MethodBeforeAdviceAdapter 及 ThrowsAdviceAdapter 实现类扮演了“适配器”的角色，AfterReturningAdvice、MethodBeforeAdvice 及 ThrowsAdvice 扮演了“被适配者”角色，而 AfterReturningAdviceInterceptor、MethodBeforeAdviceInterceptor 及 ThrowsAdviceInterceptor 则扮演了“适配目标”的角色。其源码实现如下。

```java
/**
 * Advice 适配器的顶级接口
 * @author Rod Johnson
 */
public interface AdvisorAdapter {

    /**
     * 此适配器是否能适配 给定的 advice 对象
     */
    boolean supportsAdvice(Advice advice);

    /**
     * 获取传入的 advisor 中的 Advice 对象，将其适配成 MethodInterceptor 对象
     */
    MethodInterceptor getInterceptor(Advisor advisor);
}


/**
 * 将 AfterReturningAdvice 适配成 AfterReturningAdviceInterceptor 的适配器
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class AfterReturningAdviceAdapter implements AdvisorAdapter, Serializable {

    public boolean supportsAdvice(Advice advice) {
        return (advice instanceof AfterReturningAdvice);
    }

    public MethodInterceptor getInterceptor(Advisor advisor) {
        AfterReturningAdvice advice = (AfterReturningAdvice) advisor.getAdvice();
        return new AfterReturningAdviceInterceptor(advice);
    }
}


/**
 * 将 MethodBeforeAdvice 适配成 MethodBeforeAdviceInterceptor 的适配器
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class MethodBeforeAdviceAdapter implements AdvisorAdapter, Serializable {

    public boolean supportsAdvice(Advice advice) {
        return (advice instanceof MethodBeforeAdvice);
    }

    public MethodInterceptor getInterceptor(Advisor advisor) {
        MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
        return new MethodBeforeAdviceInterceptor(advice);
    }
}


/**
 * 将 ThrowsAdvice 适配成 ThrowsAdviceInterceptor 的适配器
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
class ThrowsAdviceAdapter implements AdvisorAdapter, Serializable {

    public boolean supportsAdvice(Advice advice) {
        return (advice instanceof ThrowsAdvice);
    }

    public MethodInterceptor getInterceptor(Advisor advisor) {
        return new ThrowsAdviceInterceptor(advisor.getAdvice());
    }
}


/**
 * 下面这三个接口的实现类 均为 “被适配者”
 */
public interface AfterReturningAdvice extends AfterAdvice {

    /**
     * 目标方法method执行后，AOP会回调此方法，注意，它还传入了method的返回值
     */
    void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable;
}

public interface MethodBeforeAdvice extends BeforeAdvice {

    /**
     * 目标方法method要开始执行时，AOP会回调此方法
     */
    void before(Method method, Object[] args, Object target) throws Throwable;
}

public interface ThrowsAdvice extends AfterAdvice {

}


/**
 * 下面这三个类均为“适配目标”
 */
public class AfterReturningAdviceInterceptor implements MethodInterceptor, AfterAdvice, Serializable {

    private final AfterReturningAdvice advice;

    /**
     * 为给定的 advice 创建一个 AfterReturningAdviceInterceptor 对象
     */
    public AfterReturningAdviceInterceptor(AfterReturningAdvice advice) {
        Assert.notNull(advice, "Advice must not be null");
        this.advice = advice;
    }

    public Object invoke(MethodInvocation mi) throws Throwable {
        Object retVal = mi.proceed();
        this.advice.afterReturning(retVal, mi.getMethod(), mi.getArguments(), mi.getThis());
        return retVal;
    }
}

public class MethodBeforeAdviceInterceptor implements MethodInterceptor, Serializable {

    private MethodBeforeAdvice advice;

    /**
     * 为指定的advice创建对应的MethodBeforeAdviceInterceptor对象
     */
    public MethodBeforeAdviceInterceptor(MethodBeforeAdvice advice) {
        Assert.notNull(advice, "Advice must not be null");
        this.advice = advice;
    }

    /**
     * 这个invoke方法是拦截器的回调方法，会在代理对象的方法被调用时触发回调
     */
    public Object invoke(MethodInvocation mi) throws Throwable {
        // 首先触发了advice的before()方法的回调
        // 然后才是MethodInvocation的process()方法回调
        this.advice.before(mi.getMethod(), mi.getArguments(), mi.getThis() );
        return mi.proceed();
    }
}

public class ThrowsAdviceInterceptor implements MethodInterceptor, AfterAdvice {

    private static final String AFTER_THROWING = "afterThrowing";

    private static final Log logger = LogFactory.getLog(ThrowsAdviceInterceptor.class);

    private final Object throwsAdvice;

    private final Map<Class, Method> exceptionHandlerMap = new HashMap<Class, Method>();

    public ThrowsAdviceInterceptor(Object throwsAdvice) {
        Assert.notNull(throwsAdvice, "Advice must not be null");
        this.throwsAdvice = throwsAdvice;

        // 配置 throwsAdvice 的回调
        Method[] methods = throwsAdvice.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(AFTER_THROWING) &&
                    (method.getParameterTypes().length == 1 || method.getParameterTypes().length == 4) &&
                    Throwable.class.isAssignableFrom(method.getParameterTypes()[method.getParameterTypes().length - 1])
                ) {
                // 配置异常处理
                this.exceptionHandlerMap.put(method.getParameterTypes()[method.getParameterTypes().length - 1], method);
                if (logger.isDebugEnabled()) {
                    logger.debug("Found exception handler method: " + method);
                }
            }
        }

        if (this.exceptionHandlerMap.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one handler method must be found in class [" + throwsAdvice.getClass() + "]");
        }
    }

    public Object invoke(MethodInvocation mi) throws Throwable {
        // 把对目标对象的方法调用放入 try/catch 中，并在 catch 中触发
        // throwsAdvice 的回调，把异常接着向外抛，不做过多处理
        try {
            return mi.proceed();
        }
        catch (Throwable ex) {
            Method handlerMethod = getExceptionHandler(ex);
            if (handlerMethod != null) {
                invokeHandlerMethod(mi, ex, handlerMethod);
            }
            throw ex;
        }
    }
}


/**
 * 本类的 getInterceptors() 方法使用上述 适配器组件，完成了
 * 从 Advice 到 MethodInterceptor 的适配工作
 */
public class DefaultAdvisorAdapterRegistry implements AdvisorAdapterRegistry, Serializable {

    /**
     * 持有AdvisorAdapter的list，这个list中的AdvisorAdapter与
     * 实现 spring AOP 的 Advice 增强功能相对应
     */
    private final List<AdvisorAdapter> adapters = new ArrayList<AdvisorAdapter>(3);

    /**
     * 将已实现的 AdviceAdapter 加入 list
     */
    public DefaultAdvisorAdapterRegistry() {
        registerAdvisorAdapter(new MethodBeforeAdviceAdapter());
        registerAdvisorAdapter(new AfterReturningAdviceAdapter());
        registerAdvisorAdapter(new ThrowsAdviceAdapter());
    }

    public MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException {
        List<MethodInterceptor> interceptors = new ArrayList<MethodInterceptor>(3);

        // 从Advisor通知器中获取配置的Advice
        Advice advice = advisor.getAdvice();

        // 如果advice是MethodInterceptor类型的，直接加进interceptors，不用适配
        if (advice instanceof MethodInterceptor) {
            interceptors.add((MethodInterceptor) advice);
        }

        // 如果advice不是MethodInterceptor类型的，就将其适配成MethodInterceptor，
        // 当前的DefaultAdvisorAdapterRegistry对象 在初始化时就已经为 adapters 添加了
        // 三种 AdvisorAdapter 的实例
        for (AdvisorAdapter adapter : this.adapters) {
            // 依次使用 adapters集合中的 adapter 对 advice 进行适配
            // 将其适配成 MethodInterceptor 对象
            if (adapter.supportsAdvice(advice)) {
                interceptors.add(adapter.getInterceptor(advisor));
            }
        }
        if (interceptors.isEmpty()) {
            throw new UnknownAdviceTypeException(advisor.getAdvice());
        }
        return interceptors.toArray(new MethodInterceptor[interceptors.size()]);
    }

    public void registerAdvisorAdapter(AdvisorAdapter adapter) {
        this.adapters.add(adapter);
    }

    /**
     * 如果adviceObject是Advisor的实例，则将adviceObject转换成Advisor类型并返回
     */
    public Advisor wrap(Object adviceObject) throws UnknownAdviceTypeException {
        if (adviceObject instanceof Advisor) {
            return (Advisor) adviceObject;
        }
        if (!(adviceObject instanceof Advice)) {
            throw new UnknownAdviceTypeException(adviceObject);
        }
        Advice advice = (Advice) adviceObject;
        if (advice instanceof MethodInterceptor) {
            return new DefaultPointcutAdvisor(advice);
        }
        for (AdvisorAdapter adapter : this.adapters) {
            if (adapter.supportsAdvice(advice)) {
                return new DefaultPointcutAdvisor(advice);
            }
        }
        throw new UnknownAdviceTypeException(advice);
    }
}
```

像这样整理出来以后，其类结构及层次设计还是比较清晰明了的，比起很多书上范例的浅尝辄止，结合这些实际场景及源码去理解这些设计模式，要让人更加印象深刻。

#### Mybatis 中的应用

MyBatis 的日志模块中使用了适配器模式，MyBatis 内部调用其日志模块时，使用了其内部接口（org.apache.ibatis.logging.Log）。但是 Log4j、Slf4j 等第三方日志框架对外提供的接口各不相同，MyBatis 为了集成和复用这些第三方日志框架，在其日志模块中提供了多种 Adapter 实现 如：Log4jImpl、Slf4jImpl 等等，它们将这些 “第三方日志框架对外的接口方法” 适配成 “Log 接口方法”，这样 MyBatis 内部就可以统一通过该 Log 接口调用第三方日志框架的功能了。

其中，Log 接口定义了日志模块的功能，日志适配器 Log4jImpl、Slf4jImpl 等通过实现此接口，将对应框架中的日志类 (Logger) 里的方法 适配成 Log 接口中定义的方法。

```java
/**
 * mybatis的日志接口，统一了不同日志框架的 日志操作，
 * 由各实现类 对各日志框架进行具体的适配
 */
public interface Log {

  boolean isDebugEnabled();

  boolean isTraceEnabled();

  void error(String s, Throwable e);

  void error(String s);

  void debug(String s);

  void trace(String s);

  void warn(String s);
}


/**
 * Log4j 日志框架适配器
 */
public class Log4jImpl implements Log {

  /**
   * 注意！！！！！
   * 下面的 log 对象是 Log4j框架的 org.apache.log4j.Logger
   * 本适配器完成了 “org.apache.log4j.Logger中的方法” 到
   * “org.apache.ibatis.logging.Log中的方法” 的适配
   * 从下面的代码中可以很轻易地看出来
   */
  private final Logger log;

  private static final String FQCN = Log4jImpl.class.getName();

  public Log4jImpl(String clazz) {
    log = Logger.getLogger(clazz);
  }

  /**
   * ！！！！！！！！！！！！！！！
   * 具体适配过程如下：
   * ！！！！！！！！！！！！！！！
   */
  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  @Override
  public void error(String s, Throwable e) {
    log.log(FQCN, Level.ERROR, s, e);
  }

  @Override
  public void error(String s) {
    log.log(FQCN, Level.ERROR, s, null);
  }

  @Override
  public void debug(String s) {
    log.log(FQCN, Level.DEBUG, s, null);
  }

  @Override
  public void trace(String s) {
    log.log(FQCN, Level.TRACE, s, null);
  }

  @Override
  public void warn(String s) {
    log.log(FQCN, Level.WARN, s, null);
  }
}


/**
 * JDK 日志组件适配器
 */
public class Jdk14LoggingImpl implements Log {

  /**
   * 使用了JDK中的日志类 java.util.logging.Logger
   */
  private final Logger log;

  public Jdk14LoggingImpl(String clazz) {
    log = Logger.getLogger(clazz);
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isLoggable(Level.FINE);
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isLoggable(Level.FINER);
  }

  @Override
  public void error(String s, Throwable e) {
    log.log(Level.SEVERE, s, e);
  }

  @Override
  public void error(String s) {
    log.log(Level.SEVERE, s);
  }

  @Override
  public void debug(String s) {
    log.log(Level.FINE, s);
  }

  @Override
  public void trace(String s) {
    log.log(Level.FINER, s);
  }

  @Override
  public void warn(String s) {
    log.log(Level.WARNING, s);
  }
}
```

## 代理模式

#### 个人理解

代理模式的实际应用 主要体现在框架开发中，日常业务上的开发工作中很少有场景需要使用该模式。而代理模式中 动态代理尤为重要，不管是自己公司的内部框架 还是 一些知名的开源框架，很多重要的实现都用到了该模式。比如，有些 CS 架构中，Client 端的远程方法调用 就使用了动态代理，在 invoke()方法中 为被代理对象调用的方法 织入远程调用处理，然后将远程处理的结果返回给调用者；Spring 的 AOP 也是优先使用 JDK 动态代理来完成；Mybatis 为 JDBC 操作织入日志处理，等等。下面我们结合源码来深入理解一下这个模式。

#### 动态代理原理

静态代理没什么好讲的，很少见用到，功能也比较薄弱，本篇重点讲解动态代理。首先了解一下 JDK 动态代理的原理，这对理解 Spring AOP 部分的源码及实现原理也很有帮助。

JDK 动态代理的实现原理是，动态创建代理类井通过指定类加载器加载，然后在创建代理对象时将 InvokerHandler 对象作为构造参数传入。当调用代理对象的方法时，会调用 InvokerHandler 的 invoke() 方法，并最终调用真正业务对象的相应方法。 JDK 动态代理不仅在 Spring 及 MyBatis 的多个模块中都有所涉及， 在其它很多开源框架中也能看到其身影。

```java
/**
 * 一般会使用实现了 InvocationHandler 的类作为代理对象的生产工厂，
 * 并且通过持有被代理对象target，来在invoke()方法中对被代理对象的目标方法进行调用和增强，
 * 这些我们都能通过下面这段代码看懂，但代理对象是如何生成的？invoke()方法又是如何被调用的呢？
 */
public class ProxyFactory implements InvocationHandler{

    private Object target = null;

    public Object getInstanse(Object target){

        this.target = target;
        return Proxy.newProxyInstance(target.getClass().getClassLoader(),
                target.getClass().getInterfaces(), this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {

        Object ret = null;
        System.out.println("前置增强");
        ret = method.invoke(target, args);
        System.out.println("后置增强");
        return ret;
    }
}


/**
 * 实现了接口MyInterface和接口的play()方法，可以作为被代理类
 */
public class TargetObject implements MyInterface {

    @Override
    public void play() {
        System.out.println("妲己，陪你玩 ~");

    }
}


/**
 * 测试类
 */
public class ProxyTest {

    public static void main(String[] args) {
        TargetObject target = new TargetObject();
        // ProxyFactory 实现了 InvocationHandler接口，其中的 getInstanse() 方法利用 Proxy 类
        // 生成了target目标对象的代理对象，并返回；且ProxyFactory持有对target的引用，可以在
        // invoke() 中完成对 target 相应方法的调用，以及目标方法前置后置的增强处理
        ProxyFactory proxyFactory = new ProxyFactory();
        // 这个mi就是JDK的 Proxy 类动态生成的代理类 $Proxy0 的实例，该实例中的方法都持有对
        // invoke() 方法的回调，所以当调用其方法时，就能够执行 invoke() 中的增强处理
        MyInterface mi = (MyInterface)proxyFactory.getInstanse(target);
        // 这样可以看到 mi 的 Class 到底是什么
        System.out.println(mi.getClass());
        // 这里实际上调用的就是 $Proxy0代理类中对 play() 方法的实现，结合下面的代码可以看到
        // play() 方法通过 super.h.invoke() 完成了对 InvocationHandler对象(proxyFactory)中
        // invoke()方法的回调，所以我们才能够通过 invoke() 方法实现对 target 对象方法的
        // 前置后置增强处理
        mi.play();
        // 总的来说，就是在invoke()方法中完成target目标方法的调用，及前置后置增强，
        // JDK动态生成的代理类中对 invoke() 方法进行了回调
    }

    /**
     * 将ProxyGenerator生成的动态代理类的输出到文件中，利用反编译工具luyten等就可
     * 以看到生成的代理类的源码咯，下面给出了其反编译好的代码实现
     */
    @Test
    public void generatorSrc(){
        byte[] bytesFile = ProxyGenerator.generateProxyClass("$Proxy0", TargetObject.class.getInterfaces());
        FileOutputStream fos = null;
        try{
            String path = System.getProperty("user.dir") + "\\$Proxy0.class";
            File file = new File(path);
            fos = new FileOutputStream(file);
            fos.write(bytesFile);
            fos.flush();
        } catch (Exception e){
            e.printStackTrace();
        } finally{
            try {
                fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}


/**
 * Proxy生成的代理类，可以看到，其继承了Proxy，并且实现了被代理类的接口
 */
public final class $Proxy0 extends Proxy implements MyInterface {
    private static Method m1;
    private static Method m0;
    private static Method m3;
    private static Method m2;

    static {
        try {
            $Proxy0.m1 = Class.forName("java.lang.Object").getMethod("equals", Class.forName("java.lang.Object"));
            $Proxy0.m0 = Class.forName("java.lang.Object").getMethod("hashCode", (Class<?>[])new Class[0]);
            //实例化MyInterface的play方法
            $Proxy0.m3 = Class.forName("com.shuitu.test.MyInterface").getMethod("play", (Class<?>[])new Class[0]);
            $Proxy0.m2 = Class.forName("java.lang.Object").getMethod("toString", (Class<?>[])new Class[0]);
        }
        catch (NoSuchMethodException ex) {
            throw new NoSuchMethodError(ex.getMessage());
        }
        catch (ClassNotFoundException ex2) {
            throw new NoClassDefFoundError(ex2.getMessage());
        }
    }

    public $Proxy0(final InvocationHandler invocationHandler) {
        super(invocationHandler);
    }

    public final void play() {
        try {
        	// 这个 h 其实就是我们调用 Proxy.newProxyInstance() 方法时传进去的ProxyFactory(InvocationHandler对象)，
        	// 该对象的 invoke() 方法中实现了对目标对象的目标方法的增强。看到这里，利用动态代理实现方法增强的
        	// 实现原理就全部理清咯
            super.h.invoke(this, $Proxy0.m3, null);
        }
        catch (Error | RuntimeException error) {
            throw new RuntimeException();
        }
        catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }

    public final boolean equals(final Object o) {
        try {
            return (boolean)super.h.invoke(this, $Proxy0.m1, new Object[] { o });
        }
        catch (Error | RuntimeException error) {
            throw new RuntimeException();
        }
        catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }

    public final int hashCode() {
        try {
            return (int)super.h.invoke(this, $Proxy0.m0, null);
        }
        catch (Error | RuntimeException error) {
            throw new RuntimeException();
        }
        catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }

    public final String toString() {
        try {
            return (String)super.h.invoke(this, $Proxy0.m2, null);
        }
        catch (Error | RuntimeException error) {
            throw new RuntimeException();
        }
        catch (Throwable t) {
            throw new UndeclaredThrowableException(t);
        }
    }
}
```

#### Spring 中的应用

Spring 在生成动态代理类时，会优先选择使用 JDK 动态代理，除非被代理类没有实现接口。

```java
/**
 * 可以看到，其实现了 InvocationHandler 接口，所以肯定也定义了一个 使用 java.lang.reflect.Proxy
 * 动态生成代理对象的方法，并在实现的 invoke() 方法中为代理对象织入增强方法
 */
final class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

    public Object getProxy() {
        return getProxy(ClassUtils.getDefaultClassLoader());
    }

    /**
     * 获取 JVM 动态生成的代理对象
     */
    public Object getProxy(ClassLoader classLoader) {
        if (logger.isDebugEnabled()) {
            logger.debug("Creating JDK dynamic proxy: target source is " + this.advised.getTargetSource());
        }

        // 获取代理类要实现的接口
        Class[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised);
        findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);

        // 通过 Proxy 生成代理对象
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
    }

    /**
     * 本类所生成的代理对象中，所有方法的调用 都会回调本方法。
     * 根据用户的配置，对指定的切面进行相应的增强
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocation invocation;
        Object oldProxy = null;
        boolean setProxyContext = false;

        // 通过 targetSource 可以获取被代理对象
        TargetSource targetSource = this.advised.targetSource;
        Class targetClass = null;
        Object target = null;

        try {
            // 如果目标对象调用的是 Obejct 类中的基本方法，如：equals、hashCode 则进行相应的处理
            if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
                // 如果目标对象没有重写 Object 类的基本方法：equals(Object other)
                return equals(args[0]);
            }
            if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
                // 如果目标对象没有重写 Object类的基本方法：hashCode()
                return hashCode();
            }
            if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
                    method.getDeclaringClass().isAssignableFrom(Advised.class)) {
                // 使用代理配置对 ProxyConfig 进行服务调用
                return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
            }

            Object retVal;

            if (this.advised.exposeProxy) {
                // 如果有必要，可以援引
                oldProxy = AopContext.setCurrentProxy(proxy);
                setProxyContext = true;
            }

            // 获取目标对象，为目标方法的调用做准备
            target = targetSource.getTarget();
            if (target != null) {
                targetClass = target.getClass();
            }

            // 获取定义好的拦截器链
            List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

            // 如果没有配置拦截器，就直接调用目标对象target的method方法，并获取返回值
            if (chain.isEmpty()) {
                retVal = AopUtils.invokeJoinpointUsingReflection(target, method, args);
            }
            else {
                // 如果有拦截器链，则需要先调用拦截器链中的拦截器，再调用目标的对应方法
                // 这里通过构造 ReflectiveMethodInvocation 来实现
                invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
                // 沿着拦截器链继续向下处理
                retVal = invocation.proceed();
            }

            // 获取 method 返回值的类型
            Class<?> returnType = method.getReturnType();
            if (retVal != null && retVal == target && returnType.isInstance(proxy) &&
                    !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
                // 特殊提醒：它返回“this”，方法的返回类型与类型兼容。
                // 注意，如果 target 在另一个返回的对象中设置了对自身的引用，spring 将无法处理
                retVal = proxy;
            } else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
                throw new AopInvocationException("Null return value from advice does not match primitive return type for: " + method);
            }
            return retVal;
        }
        finally {
            if (target != null && !targetSource.isStatic()) {
                // 必须来自 TargetSource.
                targetSource.releaseTarget(target);
            }
            if (setProxyContext) {
                // 存储旧的 proxy.
                AopContext.setCurrentProxy(oldProxy);
            }
        }
    }
}
```

#### Mybatis 中的应用

Mybatis 的 PooledConnection 类中封装了数据库连接的代理对象，对数据库连接的操作大都会通过该代理对象完成。

```java
/**
 * Mybatis 封装的数据库连接类，它实现了 InvocationHandler 接口，封装了真正的
 * 数据库连接对象 (java.sql.Connection) 及其代理对象，该代理对象是通过
 * JDK 动态代理类 Proxy 产生的
 * @author Clinton Begin
 */
class PooledConnection implements InvocationHandler {

  private static final String CLOSE = "close";
  private static final Class<?>[] IFACES = new Class<?>[] { Connection.class };

  private final int hashCode;

  /**
   * 记录当前 PooledConnection对象 是从哪个 PooledDataSource(数据库连接池)对象获取的。
   * 当调用 close() 方法时会将 PooledConnection 放回该 dataSource 连接池
   */
  private final PooledDataSource dataSource;
  /** 真正的 数据库连接对象 */
  private final Connection realConnection;
  /** 数据库连接的 代理对象 */
  private final Connection proxyConnection;
  /** 从连接池中取出该连接的时间戳 */
  private long checkoutTimestamp;
  /** 该连接创建的时间戳 */
  private long createdTimestamp;
  /** 最后一次被使用的时间戳 */
  private long lastUsedTimestamp;
  /** 由数据库 URL、用户名 和 密码 计算出来的 hash值，可用于标识该连接所在的连接池 */
  private int connectionTypeCode;
  /**
   * 检测当前 PooledConnection 是否有效，主要是为了防止程序通过 close() 方法
   * 将连接归还给连接池之后，依然通过该连接操作数据库
   */
  private boolean valid;

  /**
   * 注意该构造方法中对 proxyConnection 的初始化
   */
  public PooledConnection(Connection connection, PooledDataSource dataSource) {
    this.hashCode = connection.hashCode();
    this.realConnection = connection;
    this.dataSource = dataSource;
    this.createdTimestamp = System.currentTimeMillis();
    this.lastUsedTimestamp = System.currentTimeMillis();
    this.valid = true;
    // 这里使用了 JDK 的 Proxy 为数据库连接创建了一个代理对象，对该代理对象的所有操作
    // 都会回调 本类中的 invoke() 方法
    this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), IFACES, this);
  }

  /**
   * 实现了 InvocationHandler 接口中的方法
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();
    // 如果调用的是 close() 方法，则将其放入连接池，而不是真正关闭数据库连接
    if (CLOSE.equals(methodName)) {
      dataSource.pushConnection(this);
      return null;
    }
    try {
      if (!Object.class.equals(method.getDeclaringClass())) {
        // 通过 valid 字段检测连接是否有效
        checkConnection();
      }
      // 调用真正数据库连接对象的对应方法
      return method.invoke(realConnection, args);
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }
}
```

## 装饰器模式

#### 个人理解

在实际生产中，新需求在软件的整个生命过程中总是不断出现的。当有新需求出现时，就需要为某些组件添加新的功能来满足这些需求。 添加新功能的方式有很多，我们可以直接修改已有组件的代码井添加相应的新功能，但这样会破坏己有组件的稳定性，修改完成后，整个组件需要重新进行测试才能上线使用。 这种方式显然违反了 “开放封闭” 原则。

另一种方式是使用继承，我们可以创建子类并在子类中添加新功能实现扩展。 这种方法是静态的，用户不能控制增加行为的方式和时机。 而且有些情况下继承是不可行的，例如 己有组件是被 final 修饰的类。 另外，如果待添加的新功能存在多种组合，使用继承方式可能会导致大量子类的出现。 例如，有 4 个待添加的新功能，系统需要动态使用任意多个功能的组合， 则需要添加 15 个子类才能满足全部需求。

装饰器模式能够帮助我们解决上述问题，装饰器可以动态地为对象添加功能，它是基于组合的方式实现该功能的。在实践中，我们应该尽量使用组合的方式来扩展系统的功能，而非使用继承的方式。通过装饰器模式的介绍，可以帮助读者更好地理解设计模式中常见的一句话：组合优于继承。下面先来看一下装饰器模式的类图，及其核心角色。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E8%A3%85%E9%A5%B0%E5%99%A8%E6%A8%A1%E5%BC%8F%E7%B1%BB%E5%9B%BE-91940c.png)

- Component (组件)：组件接口定义了全部 “组件实现类” 以及所有 “装饰器实现” 的行为。
- ConcreteComponent (具体组件实现类)：通常情况下，具体组件实现类就是被装饰器装饰的原始对象，该类提供了 Component 接口中定义的最基本的功能，其他高级功能或后续添加的新功能，都是通过装饰器的方式添加到该类的对象之上的。
- Decorator (装饰器)：所有装饰器的父类，它是一个实现了 Component 接口的抽象类，并持有一个 Component 被装饰对象，这就实现了装饰器的嵌套组合和复用。
- ConcreteDecorator (具体的装饰器实现类)：该实现类要向被装饰对象添加某些功能，被装饰的对象只要是 Component 类型即可。

#### Mybatis 中的应用

在 MyBatis 的缓存模块中，使用了装饰器模式的变体，其中将 Decorator 接口和 Component 接口合并为一个 Component 接口，即，去掉了 Decorator 这个中间层，ConcreteDecorator 直接实现了 Component 接口。

MyBatis 中缓存模块相关的代码位于 cache 包下， 其中 Cache 接口是缓存模块的核心接口，它定义了所有缓存的基本行为，扮演了 Component 的角色。实现类 PerpetualCache 扮演了 ConcreteComponent 的角色，其实现比较简单，底层使用 HashMap 记录缓存项，也是通过该 HashMap 对象的方法实现了 Cache 接口中定义的相应方法。而 cache 包下的 decorators 包中，则定义了一系列 ConcreteDecorator 的实现，如 BlockingCache、FifoCache 及 LruCache 等等，它们都持有一个 Cache 类型的对象，通过嵌套组合的方式为该 Cache 对象 装饰相应的功能。其源码实现如下。

```java
public interface Cache {

  /** 该缓存对象的 id */
  String getId();

  /** 向缓存中添加数据，一般 key 是 CacheKey，value 是查询结果 */
  void putObject(Object key, Object value);

  /** 根据指定的 key，在缓存中查找对应的结果对象 */
  Object getObject(Object key);

  /** 删除 key 对应的缓存项 */
  Object removeObject(Object key);

  /** 清空缓存 */
  void clear();

  /** 缓存项的个数，该方法不会被 MyBatis 核心代码使用，所以可提供空实现 */
  int getSize();

  /**
   * 获取读写锁，该方法不会被 MyBatis 核心代码使用，所以可提供空实现。
   * 这里在接口中为此方法提供了默认实现，也是 JDK8 的新特性
   */
  default ReadWriteLock getReadWriteLock() {
    return null;
  }
}


public class PerpetualCache implements Cache {

  /** Cache 对象的唯一标识 */
  private final String id;
  /** 用于记录缓存项的 Map 对象 */
  private final Map<Object, Object> cache = new HashMap<>();

  public PerpetualCache(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  /**
   * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
   * 下面所有的方法都是通过 cache 这个 HashMap对象 的相应方法实现的
   * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
   */
  @Override
  public int getSize() {
    return cache.size();
  }

  @Override
  public void putObject(Object key, Object value) {
    cache.put(key, value);
  }

  @Override
  public Object getObject(Object key) {
    return cache.get(key);
  }

  @Override
  public Object removeObject(Object key) {
    return cache.remove(key);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  /**
   * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
   * 重写了 equals() 和 hashCode() 方法，两者都只关心 id 字段，并不关心 cache 字段
   * ！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
   */
  @Override
  public boolean equals(Object o) {
    if (getId() == null) {
      throw new CacheException("Cache instances require an ID.");
    }
    if (this == o) {
      return true;
    }
    if (!(o instanceof Cache)) {
      return false;
    }

    Cache otherCache = (Cache) o;
    return getId().equals(otherCache.getId());
  }

  @Override
  public int hashCode() {
    if (getId() == null) {
      throw new CacheException("Cache instances require an ID.");
    }
    return getId().hashCode();
  }
}


/**
 * 阻塞版本的缓存装饰器，它会保证只有一个线程到数据库中查找指定 key 对应的数据。
 * 假设线程 A 在 BlockingCache 中未查找到 keyA 对应的缓存项时，线程 A 会获取 keyA 对应的锁，
 * 这样后续线程在查找 keyA 时会被阻塞
 */
public class BlockingCache implements Cache {

  /** 阻塞超时时长 */
  private long timeout;
  /** 被装饰的底层 Cache 对象 */
  private final Cache delegate;
  /** 每个 key 都有对应的 ReentrantLock 对象 */
  private final ConcurrentHashMap<Object, ReentrantLock> locks;

  public BlockingCache(Cache delegate) {
    this.delegate = delegate;
    this.locks = new ConcurrentHashMap<>();
  }

  @Override
  public Object getObject(Object key) {
    // 获取该 key 对应的锁
    acquireLock(key);
    // 查询 key
    Object value = delegate.getObject(key);
    // 缓存中有 key 对应的缓存项，则释放锁，否则继续持有锁
    if (value != null) {
      releaseLock(key);
    }
    return value;
  }

  private void acquireLock(Object key) {
    // 获取 ReentrantLock 对象
    Lock lock = getLockForKey(key);
    // 获取锁，带超时时长
    if (timeout > 0) {
      try {
        boolean acquired = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
        // 超时，则抛出异常
        if (!acquired) {
          throw new CacheException("Couldn't get a lock in " + timeout + " for the key " +  key + " at the cache " + delegate.getId());
        }
      } catch (InterruptedException e) {
        throw new CacheException("Got interrupted while trying to acquire lock for key " + key, e);
      }
    } else {
      // 获取锁，不带起时时长
      lock.lock();
    }
  }

  private ReentrantLock getLockForKey(Object key) {
    // 创建 ReentrantLock 对象，尝试添加到 locks 集合中，若 locks 集合中已经有了
    // 相应的 ReentrantLock 对象，则使用 locks 集合中的 ReentrantLock 对象
    return locks.computeIfAbsent(key, k -> new ReentrantLock());
  }

  @Override
  public void putObject(Object key, Object value) {
    try {
      // 向缓存中添加缓存项
      delegate.putObject(key, value);
    } finally {
      // 释放锁
      releaseLock(key);
    }
  }

  private void releaseLock(Object key) {
    // 获取锁
    ReentrantLock lock = locks.get(key);
    // 锁是否被当前线程持有
    if (lock.isHeldByCurrentThread()) {
      // 释放锁
      lock.unlock();
    }
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  @Override
  public Object removeObject(Object key) {
    // despite of its name, this method is called only to release locks
    releaseLock(key);
    return null;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}


/**
 * 在很多场景中，为了控制缓存的大小，系统需要按照一定的规则清理缓存。
 * FifoCache 是先入先出版本的装饰器，当向缓存添加数据时，如果缓存项的个数已经达到上限，
 * 则会将缓存中最老(即最早进入缓存)的缓存项删除
 */
public class FifoCache implements Cache {

  /** 底层被装饰的底层 Cache 对象 */
  private final Cache delegate;
  /** 用于记录 key 进入缓存的先后顺序，使用的是 LinkedList<Object> 类型的集合对象 */
  private final Deque<Object> keyList;
  /** 记录了缓存项的上限，超过该值，则需要清理最老的缓存项 */
  private int size;

  public FifoCache(Cache delegate) {
    this.delegate = delegate;
    this.keyList = new LinkedList<>();
    this.size = 1024;
  }

  @Override
  public void putObject(Object key, Object value) {
    // 检测并清理缓存
    cycleKeyList(key);
    // 添加缓存项
    delegate.putObject(key, value);
  }

  private void cycleKeyList(Object key) {
    // 记录 key
    keyList.addLast(key);
    // 如果达到缓存上限，则清理最老的缓存项
    if (keyList.size() > size) {
      Object oldestKey = keyList.removeFirst();
      delegate.removeObject(oldestKey);
    }
  }

  @Override
  public String getId() {
    return delegate.getId();
  }

  @Override
  public int getSize() {
    return delegate.getSize();
  }

  public void setSize(int size) {
    this.size = size;
  }

  @Override
  public Object getObject(Object key) {
    return delegate.getObject(key);
  }

  @Override
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override
  public void clear() {
    delegate.clear();
    keyList.clear();
  }
}
```



# 从Spring及Mybatis框架源码中学习设计模式(行为型)

设计模式是解决问题的方案，从大神的代码中学习对设计模式的使用，可以有效提升个人编码及设计代码的能力。本系列博文用于总结阅读过的框架源码（Spring 系列、Mybatis）及 JDK 源码中 所使用过的设计模式，并结合个人工作经验，重新理解设计模式。

本篇博文主要看一下行为型的几个设计模式，即，策略模式、模板方法模式、迭代器模式、观察者模式 及 责任链模式。

## 策略模式

#### 个人理解

去年看了蛮多源码，发现 框架的开发者在实际使用设计模式时，大都会根据实际情况 使用其变体，老老实实按照书上的类图及定义去设计代码的比较少。不过我们依然还是先看一下书上的定义，然后比较一下理论与实践的一些差别吧。策略模式的类图及定义如下。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E7%AD%96%E7%95%A5%E6%A8%A1%E5%BC%8F%E7%B1%BB%E5%9B%BE-e5a2ff.png)

定义一系列算法，封装每个算法 并使它们可以互换。该模式的主要角色如下：

- Strategy 接口：用于定义一个算法族，它们都具有 behavior()方法；
- Context：使用该算法的类，持有 Strategy 对象，其中的 setStrategy(Strategy stra)方法可以动态地改变 strategy 对象，以此改变自己所使用的算法。

很多书上都使用 Duck 和 QuackBehavior 作为示例进行说明，这里就不重复咯，主要看一下 Spring 中是如何使用该模式的。

#### Spring 中的实现

Spring 的 AbstractAutowireCapableBeanFactory 在进行 bean 实例化时使用了策略模式的变种，其中 InstantiationStrategy 接口 定义了实例化方法，实现类 SimpleInstantiationStrategy 和 CglibSubclassingInstantiationStrategy 分别实现了各自的算法，AbstractAutowireCapableBeanFactory 则通过持有 InstantiationStrategy 对象，对算进行使用。其源码实现如下。

```java
/**
 * 本接口用于定义bean实例的创建，通过给定的RootBeanDefinition对象
 * 本组件使用了策略模式，因为各种情况，需要使用不同的方法来实现，包括使用CGLIB动态创建子类
 * @author Rod Johnson
 * @since 1.1
 */
public interface InstantiationStrategy {

    /**
     * 返回一个bean实例，使用BeanFactory给定的参数
     */
    Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner)
            throws BeansException;

    Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
            Constructor<?> ctor, Object[] args) throws BeansException;

    Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
            Object factoryBean, Method factoryMethod, Object[] args) throws BeansException;

}


public class SimpleInstantiationStrategy implements InstantiationStrategy {

    /**
     * 具体使用哪个策略进行bean的实例化，是在这个实现类中决定的
     */
    public Object instantiate(RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {
        // 如果beanDefinition中没有方法覆盖，则使用Java的反射机制实例化对象，否则使用CGLIB策略
        if (beanDefinition.getMethodOverrides().isEmpty()) {
            Constructor<?> constructorToUse;
            synchronized (beanDefinition.constructorArgumentLock) {
                // 获取对象的构造方法或生成对象的工厂方法对bean进行实例化
                constructorToUse = (Constructor<?>) beanDefinition.resolvedConstructorOrFactoryMethod;

                // 如果前面没有获取到构造方法，则通过反射获取
                if (constructorToUse == null) {
                    // 使用JDK的反射机制，判断要实例化的Bean是否是接口
                    final Class clazz = beanDefinition.getBeanClass();
                    // 如果clazz是一个接口，直接抛出异常
                    if (clazz.isInterface()) {
                        throw new BeanInstantiationException(clazz, "Specified class is an interface");
                    }
                    try {
                        if (System.getSecurityManager() != null) {
                            // 这里是一个匿名内置类，使用反射机制获取Bean的构造方法
                            constructorToUse = AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor>() {
                                public Constructor run() throws Exception {
                                    return clazz.getDeclaredConstructor((Class[]) null);
                                }
                            });
                        }
                        else {
                            constructorToUse =	clazz.getDeclaredConstructor((Class[]) null);
                        }
                        beanDefinition.resolvedConstructorOrFactoryMethod = constructorToUse;
                    }
                    catch (Exception ex) {
                        throw new BeanInstantiationException(clazz, "No default constructor found", ex);
                    }
                }
            }
            // 使用BeanUtils实例化，通过反射机制调用”构造方法.newInstance(arg)”来进行实例化
            return BeanUtils.instantiateClass(constructorToUse);
        }
        else {
            /**
             * ！！！！！！！！！！！！！！
             * 使用CGLIB来实例化对象
             * 调用了其子类CglibSubclassingInstantiationStrategy中的实现
             * ！！！！！！！！！！！！！！
             */
            return instantiateWithMethodInjection(beanDefinition, beanName, owner);
        }
    }
}


public class CglibSubclassingInstantiationStrategy extends SimpleInstantiationStrategy {

    /**
     * 下面两个方法都通过实例化自己的私有静态内部类CglibSubclassCreator，
     * 然后调用该内部类对象的实例化方法instantiate()完成实例化
     */
    protected Object instantiateWithMethodInjection(
            RootBeanDefinition beanDefinition, String beanName, BeanFactory owner) {

        // 必须生成cglib子类
        return new CglibSubclassCreator(beanDefinition, owner).instantiate(null, null);
    }

    @Override
    protected Object instantiateWithMethodInjection(
            RootBeanDefinition beanDefinition, String beanName, BeanFactory owner,
            Constructor ctor, Object[] args) {

        return new CglibSubclassCreator(beanDefinition, owner).instantiate(ctor, args);
    }

    /**
     * 为避免3.2之前的Spring版本中的外部cglib依赖而创建的内部类
     */
    private static class CglibSubclassCreator {

        private final RootBeanDefinition beanDefinition;

        private final BeanFactory owner;

        public CglibSubclassCreator(RootBeanDefinition beanDefinition, BeanFactory owner) {
            this.beanDefinition = beanDefinition;
            this.owner = owner;
        }

        //使用CGLIB进行Bean对象实例化
        public Object instantiate(Constructor ctor, Object[] args) {
            //实例化Enhancer对象，并为Enhancer对象设置父类，生成Java对象的参数，比如：基类、回调方法等
            Enhancer enhancer = new Enhancer();
            //将Bean本身作为其父类
            enhancer.setSuperclass(this.beanDefinition.getBeanClass());
            enhancer.setCallbackFilter(new CallbackFilterImpl());
            enhancer.setCallbacks(new Callback[] {
                    NoOp.INSTANCE,
                    new LookupOverrideMethodInterceptor(),
                    new ReplaceOverrideMethodInterceptor()
            });

            //使用CGLIB的create方法生成实例对象
            return (ctor == null) ? enhancer.create() : enhancer.create(ctor.getParameterTypes(), args);
        }
    }
}


public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
		implements AutowireCapableBeanFactory {

    /** 创建bean实例的策略，注意 这里直接实例化的是 CglibSubclassingInstantiationStrategy 对象 */
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    /**
     * 设置用于创建bean实例的实例化策略，默认使用CglibSubclassingInstantiationStrategy
     */
    public void setInstantiationStrategy(InstantiationStrategy instantiationStrategy) {
        this.instantiationStrategy = instantiationStrategy;
    }

    protected InstantiationStrategy getInstantiationStrategy() {
        return this.instantiationStrategy;
    }

    /**
     * 使用默认的无参构造方法实例化Bean对象
     */
    protected BeanWrapper instantiateBean(final String beanName, final RootBeanDefinition mbd) {
        try {
            Object beanInstance;
            final BeanFactory parent = this;
            // 获取系统的安全管理接口，JDK标准的安全管理API
            if (System.getSecurityManager() != null) {
                // 这里是一个匿名内置类，根据实例化策略创建实例对象
                beanInstance = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        return getInstantiationStrategy().instantiate(mbd, beanName, parent);
                    }
                }, getAccessControlContext());
            }
            else {

                /**
                 * ！！！！！！！！！！！！！！
                 * 使用初始化策略实例化Bean对象
                 * ！！！！！！！！！！！！！！
                 */
                beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
            }
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw);
            return bw;
        }
        catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Instantiation of bean failed", ex);
        }
    }

    ...

}
```

与标准的策略模式的设计区别在于，实现类 CglibSubclassingInstantiationStrategy 并不是直接实现了 InstantiationStrategy 接口，而是继承了 SimpleInstantiationStrategy，SimpleInstantiationStrategy 直接实现了 通过 JDK 反射机制实例化 bean 的策略，而 CglibSubclassingInstantiationStrategy 则是在自己的私有静态内部类中 完成的 通过 CGLIB 实例化 bean 的策略。

另外，虽然 AbstractAutowireCapableBeanFactory 默认持有的是 CglibSubclassingInstantiationStrategy 的实例，但具体使用哪个实现类中的策略，则是由 CglibSubclassingInstantiationStrategy 的父类 SimpleInstantiationStrategy 中的 instantiate()方法决定的。也就是说，虽然持有的是 CglibSubclassingInstantiationStrategy 对象，但实际上可能使用的是 JDK 反射机制实例化 bean 的策略。

设计模式的生产实践可能比 理论上的那些示例复杂的多，所以，若想确实提高自己代码的设计能力，还是要摆脱书本，多看实际应用。

#### Mybatis 中的实现

mybatis 的 DefaultSqlSession 使用了策略模式，DefaultSqlSession 扮演了 Context 的角色，Executor 接口及其实现类扮演了策略接口及实现。DefaultSqlSession 持有 Executor 对象，在 DefaultSqlSession 实例化时通过构造方法传入具体的 Executor 对象，根据持有的 Executor 对象的不同，而使用不同的策略进行数据库操作。具体使用哪个 Executor 的实例，由 Configuration 的 newExecutor() 方法决定。

```java
public class DefaultSqlSession implements SqlSession {

  private final Executor executor;

  /**
   * 在构造方法中为 executor 赋值，没有提供专门的set方法
   */
  public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
    this.configuration = configuration;
    this.executor = executor;
    this.dirty = false;
    this.autoCommit = autoCommit;
  }

  /**
   * executor的不同，决定了DefaultSqlSession使用哪种策略执行SQL操作
   */
  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      executor.query(ms, wrapCollection(parameter), rowBounds, handler);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  @Override
  public int update(String statement, Object parameter) {
    try {
      dirty = true;
      MappedStatement ms = configuration.getMappedStatement(statement);
      return executor.update(ms, wrapCollection(parameter));
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }
}


public interface Executor {

  int update(MappedStatement ms, Object parameter) throws SQLException;

  <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

  void commit(boolean required) throws SQLException;

  void rollback(boolean required) throws SQLException;

  Transaction getTransaction();

  ......
}


public abstract class BaseExecutor implements Executor {

  ......

  protected BaseExecutor(Configuration configuration, Transaction transaction) {
    this.transaction = transaction;
    this.deferredLoads = new ConcurrentLinkedQueue<>();
    this.localCache = new PerpetualCache("LocalCache");
    this.localOutputParameterCache = new PerpetualCache("LocalOutputParameterCache");
    this.closed = false;
    this.configuration = configuration;
    this.wrapper = this;
  }

  ......
}


public class SimpleExecutor extends BaseExecutor {

  public SimpleExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      // 获取配置对象
      Configuration configuration = ms.getConfiguration();
      // 创建 StatementHandler 对象，实际返回的是 RoutingStatementHandler 对象，前面介绍过，
      // 其中根据 MappedStatement.statementType 选择具体的 StatementHandler 实现
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      // 完成 Statement 的创建和初始化，该方法首先会调用 StatementHandler.prepare() 方法
      // 创建 Statement 对象， 然后调用 StatementHandler. parameterize() 方法处理占位符
      stmt = prepareStatement(handler, ms.getStatementLog());
      // 调用 StatementHandler.query() 方法，执行 SQL 语句，并通过 ResultSetHandler 完成
      // 结果集的映射
      return handler.query(stmt, resultHandler);
    } finally {
      // 关闭 Statement 对象
      closeStatement(stmt);
    }
  }

  ......
}


public class BatchExecutor extends BaseExecutor {

  @Override
  public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
    // 获取配置对象
    final Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT, null, null);
    final BoundSql boundSql = handler.getBoundSql();
    // 获取 SQL 语句
    final String sql = boundSql.getSql();
    final Statement stmt;
    // 如果当前执行的 SQL 模式与上次执行的 SQL 模式相同且对应的 MappedStatement 对象相同
    if (sql.equals(currentSql) && ms.equals(currentStatement)) {
      // 获取 statementList 集合中最后一个 Statement 对象
      int last = statementList.size() - 1;
      stmt = statementList.get(last);
      applyTransactionTimeout(stmt);
      // 绑定实参，处理占位符 “？”
      handler.parameterize(stmt);//fix Issues 322
      // 查找对应的 BatchResult 对象，并记录用户传入的实参
      BatchResult batchResult = batchResultList.get(last);
      batchResult.addParameterObject(parameterObject);
    } else {
      Connection connection = getConnection(ms.getStatementLog());
      // 创建新的 Statement 对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 绑定实参，处理占位符“？”
      handler.parameterize(stmt);    //fix Issues 322
      // 更新 currentSql 和 currentStatement
      currentSql = sql;
      currentStatement = ms;
      // 将新创建的 Statement 对象添加到 statementList 集合中
      statementList.add(stmt);
      // 添加新的 BatchResult 对象
      batchResultList.add(new BatchResult(ms, sql, parameterObject));
    }
    // 底层通过调用 Statement.addBatch() 方法添加 SQL 语句
    handler.batch(stmt);
    return BATCH_UPDATE_RETURN_VALUE;
  }

  ......
}


public class Configuration {

  /**
   * 在这个方法中决定使用哪个 Executor 实现
   */
  public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
      executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
      executor = new ReuseExecutor(this, transaction);
    } else {
      executor = new SimpleExecutor(this, transaction);
    }
    if (cacheEnabled) {
      executor = new CachingExecutor(executor);
    }
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
  }

  ......
}
```

## 模板方法模式

#### 个人理解

在该模式中，一个算法可以分为多个步骤，这些步骤的执行次序在一个被称为“模板方法”的方法中定义，而算法的每个步骤都对应着一个方法，这些方法被称为 “基本方法”。 模板方法按照它定义的顺序依次调用多个基本方法，从而实现整个算法流程。在模板方法模式中，会将模板方法的实现以及那些固定不变的基本方法的实现放在父类中，而那些不固定的基 本方法在父类中只是抽象方法，其真正的实现代码会被延迟到子类中完成。

我觉得这是最简单且常用的设计模式之一咯，自己在实现一些功能时也会使用这种模式，在抽象类中定义好流程的执行顺序，通用的流程在抽象类中实现，个性化的流程交给各个子类去实现。spring 及 mybatis 中均有应用。

#### Spring 中的应用

Spring 中的 AbstractApplicationContext 和其子类 AbstractRefreshableApplicationContext、GenericApplicationContext 使用了模板方法模式。源码实现及详细注释如下。

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext, DisposableBean {

    /**
     * 告诉子类启动refreshBeanFactory()方法，BeanDefinition资源文件的载入
     * 从子类的refreshBeanFactory()方法启动开始
     */
    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        // 这里使用了模板方法模式，自己定义了流程，个性化的方法实现交由子类完成
        // 其中，refreshBeanFactory() 和 getBeanFactory()为抽象方法
        refreshBeanFactory();
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (logger.isDebugEnabled()) {
            logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
        }
        return beanFactory;
    }

    protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

    public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}


public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    /**
     * 在这里完成了容器的初始化，并赋值给自己private的beanFactory属性，为下一步调用做准备
     * 从父类AbstractApplicationContext继承的抽象方法，自己做了实现
     */
    @Override
    protected final void refreshBeanFactory() throws BeansException {
        // 如果已经建立了IoC容器，则销毁并关闭容器
        if (hasBeanFactory()) {
            destroyBeans();
            closeBeanFactory();
        }
        try {
            // 创建IoC容器，DefaultListableBeanFactory类实现了ConfigurableListableBeanFactory接口
            DefaultListableBeanFactory beanFactory = createBeanFactory();
            beanFactory.setSerializationId(getId());
            // 定制化IoC容器，如设置启动参数，开启注解的自动装配等
            customizeBeanFactory(beanFactory);
            // 载入BeanDefinition，这里又使用了一个委派模式，在当前类定义此抽象方法，子类容器具体实现
            loadBeanDefinitions(beanFactory);
            synchronized (this.beanFactoryMonitor) {
                // 给自己的属性赋值
                this.beanFactory = beanFactory;
            }
        }
        catch (IOException ex) {
            throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
        }
    }

    @Override
    public final ConfigurableListableBeanFactory getBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory == null) {
                throw new IllegalStateException("BeanFactory not initialized or already closed - " +
                        "call 'refresh' before accessing beans via the ApplicationContext");
            }
            return this.beanFactory;
        }
    }
}


public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

    @Override
    protected final void refreshBeanFactory() throws IllegalStateException {
        if (this.refreshed) {
            throw new IllegalStateException(
                    "GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
        }
        this.beanFactory.setSerializationId(getId());
        this.refreshed = true;
    }

    public final ConfigurableListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }
}
```

#### Mybatis 中的应用

mybatis 的 Executor 组件使用了该模式，其中抽象类 BaseExecutor 定义了模板方法和抽象方法，实现类 SimpleExecutor、BatchExecutor 及 ReuseExecutor 对抽象方法进行具体实现。源码如下。

```java
public abstract class BaseExecutor implements Executor {

  @Override
  public int update(MappedStatement ms, Object parameter) throws SQLException {
    ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
    // 判断当前 Executor 是否已经关闭
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // clearLocalCache() 方法中会调用 localCache()、localOutputParameterCache() 两个
    // 缓存的 clear() 方法完成清理工作。这是影响一级缓存中数据存活时长的第三个方面
    clearLocalCache();
    // 调用 doUpdate() 抽象方法执行 SQL 语句
    return doUpdate(ms, parameter);
  }

  public List<BatchResult> flushStatements(boolean isRollBack) throws SQLException {
    // 判断当前 Executor 是否已经关闭
    if (closed) {
      throw new ExecutorException("Executor was closed.");
    }
    // 调用抽象方法 doFlushStatements()，其参数 isRollBack 表示是否执行 Executor 中缓存的
    // SQL 语句，false 表示执行，true 表示不执行
    return doFlushStatements(isRollBack);
  }

  /**
   * 调用 doQuery() 方法完成数据库查询，并得到映射后的结果集对象，
   */
  private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    List<E> list;
    // 在缓存中添加占位符
    localCache.putObject(key, EXECUTION_PLACEHOLDER);
    try {
      // 调用 doQuery() 抽象方法，完成数据库查询操作，并返回结果集对象
      list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    } finally {
      // 删除占位符
      localCache.removeObject(key);
    }
    // 将真正的结采对象添加到一级缓存中
    localCache.putObject(key, list);
    // 是否为存储过程调用
    if (ms.getStatementType() == StatementType.CALLABLE) {
      // 缓存输出类型的参数
      localOutputParameterCache.putObject(key, parameter);
    }
    return list;
  }

  @Override
  public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
    BoundSql boundSql = ms.getBoundSql(parameter);
    return doQueryCursor(ms, parameter, rowBounds, boundSql);
  }

  protected abstract int doUpdate(MappedStatement ms, Object parameter)
      throws SQLException;

  protected abstract List<BatchResult> doFlushStatements(boolean isRollback)
      throws SQLException;

  protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
      throws SQLException;

  protected abstract <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql)
      throws SQLException;
}


public class SimpleExecutor extends BaseExecutor {

  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      // 获取配置对象
      Configuration configuration = ms.getConfiguration();
      // 创建 StatementHandler 对象，实际返回的是 RoutingStatementHandler 对象，前面介绍过，
      // 其中根据 MappedStatement.statementType 选择具体的 StatementHandler 实现
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      // 完成 Statement 的创建和初始化，该方法首先会调用 StatementHandler.prepare() 方法
      // 创建 Statement 对象， 然后调用 StatementHandler. parameterize() 方法处理占位符
      stmt = prepareStatement(handler, ms.getStatementLog());
      // 调用 StatementHandler.query() 方法，执行 SQL 语句，并通过 ResultSetHandler 完成
      // 结果集的映射
      return handler.query(stmt, resultHandler);
    } finally {
      // 关闭 Statement 对象
      closeStatement(stmt);
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    Cursor<E> cursor = handler.queryCursor(stmt);
    stmt.closeOnCompletion();
    return cursor;
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) {
    return Collections.emptyList();
  }

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    Connection connection = getConnection(statementLog);
    stmt = handler.prepare(connection, transaction.getTimeout());
    handler.parameterize(stmt);
    return stmt;
  }
}


public class BatchExecutor extends BaseExecutor {

  @Override
  public int doUpdate(MappedStatement ms, Object parameterObject) throws SQLException {
    // 获取配置对象
    final Configuration configuration = ms.getConfiguration();
    // 创建 StatementHandler 对象
    final StatementHandler handler = configuration.newStatementHandler(this, ms, parameterObject, RowBounds.DEFAULT, null, null);
    final BoundSql boundSql = handler.getBoundSql();
    // 获取 SQL 语句
    final String sql = boundSql.getSql();
    final Statement stmt;
    // 如果当前执行的 SQL 模式与上次执行的 SQL 模式相同且对应的 MappedStatement 对象相同
    if (sql.equals(currentSql) && ms.equals(currentStatement)) {
      // 获取 statementList 集合中最后一个 Statement 对象
      int last = statementList.size() - 1;
      stmt = statementList.get(last);
      applyTransactionTimeout(stmt);
      // 绑定实参，处理占位符 “？”
      handler.parameterize(stmt);//fix Issues 322
      // 查找对应的 BatchResult 对象，并记录用户传入的实参
      BatchResult batchResult = batchResultList.get(last);
      batchResult.addParameterObject(parameterObject);
    } else {
      Connection connection = getConnection(ms.getStatementLog());
      // 创建新的 Statement 对象
      stmt = handler.prepare(connection, transaction.getTimeout());
      // 绑定实参，处理占位符“？”
      handler.parameterize(stmt);    //fix Issues 322
      // 更新 currentSql 和 currentStatement
      currentSql = sql;
      currentStatement = ms;
      // 将新创建的 Statement 对象添加到 statementList 集合中
      statementList.add(stmt);
      // 添加新的 BatchResult 对象
      batchResultList.add(new BatchResult(ms, sql, parameterObject));
    }
    // 底层通过调用 Statement.addBatch() 方法添加 SQL 语句
    handler.batch(stmt);
    return BATCH_UPDATE_RETURN_VALUE;
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
      throws SQLException {
    Statement stmt = null;
    try {
      flushStatements();
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameterObject, rowBounds, resultHandler, boundSql);
      Connection connection = getConnection(ms.getStatementLog());
      stmt = handler.prepare(connection, transaction.getTimeout());
      handler.parameterize(stmt);
      return handler.query(stmt, resultHandler);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    flushStatements();
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Connection connection = getConnection(ms.getStatementLog());
    Statement stmt = handler.prepare(connection, transaction.getTimeout());
    handler.parameterize(stmt);
    Cursor<E> cursor = handler.queryCursor(stmt);
    stmt.closeOnCompletion();
    return cursor;
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    try {
      // results 集合用于储存批处理的结采
      List<BatchResult> results = new ArrayList<>();
      // 如果明确指定了要回滚事务，则直接返回空集合，忽略 statementList 集合中记录的 SQL 语句
      if (isRollback) {
        return Collections.emptyList();
      }
      // 遍历 statementList 集合
      for (int i = 0, n = statementList.size(); i < n; i++) {
        // 获取 Statement 对象
        Statement stmt = statementList.get(i);
        applyTransactionTimeout(stmt);
        // 获取对应 BatchResult 对象
        BatchResult batchResult = batchResultList.get(i);
        try {
          // 调用 Statement.executeBatch() 方法批量执行其中记录的 SQL 语句，并使用返回的 int 数组
          // 更新 BatchResult.updateCounts 字段，其中每一个元素都表示一条 SQL 语句影响的记录条数
          batchResult.setUpdateCounts(stmt.executeBatch());
          MappedStatement ms = batchResult.getMappedStatement();
          List<Object> parameterObjects = batchResult.getParameterObjects();
          // 获取配置的 KeyGenerator 对象
          KeyGenerator keyGenerator = ms.getKeyGenerator();
          if (Jdbc3KeyGenerator.class.equals(keyGenerator.getClass())) {
            Jdbc3KeyGenerator jdbc3KeyGenerator = (Jdbc3KeyGenerator) keyGenerator;
            // 获取数据库生成的主键，并设置到 parameterObjects 中，前面已经分析过，这里不再重复
            jdbc3KeyGenerator.processBatch(ms, stmt, parameterObjects);
          } else if (!NoKeyGenerator.class.equals(keyGenerator.getClass())) { //issue #141
            // 对于其他类型的 KeyGenerator，会调用其 processAfter() 方法
            for (Object parameter : parameterObjects) {
              keyGenerator.processAfter(this, ms, stmt, parameter);
            }
          }
          // Close statement to close cursor #1109
          closeStatement(stmt);
        } catch (BatchUpdateException e) {
          StringBuilder message = new StringBuilder();
          message.append(batchResult.getMappedStatement().getId())
              .append(" (batch index #")
              .append(i + 1)
              .append(")")
              .append(" failed.");
          if (i > 0) {
            message.append(" ")
                .append(i)
                .append(" prior sub executor(s) completed successfully, but will be rolled back.");
          }
          throw new BatchExecutorException(message.toString(), e, results, batchResult);
        }
        // 添加 BatchResult 到 results 集合
        results.add(batchResult);
      }
      return results;
    } finally {
      // 关闭所有 Statement 对象，并清空 currentSql 字段、清空 statementList 集合、
      // 清空 batchResultList 集合（略）
      for (Statement stmt : statementList) {
        closeStatement(stmt);
      }
      currentSql = null;
      statementList.clear();
      batchResultList.clear();
    }
  }
}


public class ReuseExecutor extends BaseExecutor {

  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    return handler.update(stmt);
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    return handler.query(stmt, resultHandler);
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    return handler.queryCursor(stmt);
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) {
    for (Statement stmt : statementMap.values()) {
      closeStatement(stmt);
    }
    statementMap.clear();
    return Collections.emptyList();
  }
}
```

可以看得出来，模板方法就是 BaseExecutor 的 update()、flushStatements()、queryFromDatabase() 及 queryCursor()，分别使用了抽象方法 doUpdate()、doFlushStatements()、doQuery() 及 doQueryCursor()。

## 迭代器模式

#### 个人理解

这个模式最经典的实现莫过于 Java 的集合类咯。同样还是先简单介绍一下这个设计模式，然后结合 ArrayList 的源码进行分析。

本设计模式用于提供一种遍历集合元素的方法，且不暴露集合对象的内部表示。其主要角色 和 简单实现如下：

- Aggregate：聚合类，有一个可以获取 Iterator 对象的 iterator() 方法；
- Iterator：主要定义了 hasNest() 和 next()方法；

```java
public interface Aggregate {
    Iterator iterator();
}


public class ConcreteAggregate implements Aggregate {

    private Integer[] elements;

    public ConcreteAggregate() {
        elements = new Integer[10];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = i;
        }
    }

    @Override
    public Iterator iterator() {
        return new ConcreteIterator(elements);
    }
}


public interface Iterator<Integer> {

    boolean hasNext();

    Integer next();
}


public class ConcreteIterator<Integer> implements Iterator {

    private Integer[] elements;
    private int position = 0;

    public ConcreteIterator(Integer[] elements) {
        this.elements = elements;
    }

    @Override
    public boolean hasNext() {
        return position < elements.length;
    }

    @Override
    public Integer next() {
        return elements[position ++];
    }
}


public class Client {

    public static void main(String[] args) {
        Aggregate aggregate = new ConcreteAggregate();
        Iterator<Integer> iterator = aggregate.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
    }
}
```

#### ArrayList 对迭代器模式的实现

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

    /**
     * 存储ArrayList元素的数组缓冲区。ArrayList的容量是此数组缓冲区的长度
     */
    transient Object[] elementData; // non-private to simplify nested class access

    /**
     * ArrayList的大小（其包含的元素数）
     */
    private int size;

    /**
     * 以正确的顺序返回此列表中元素的迭代器
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * ArrayList的私有内部类，其实现了Iterator接口，所以也是一个迭代器
     * AbstractList.Itr的优化版本
     */
    private class Itr implements Iterator<E> {
        int cursor;       // 要返回的下一个元素的索引
        int lastRet = -1; // 返回的最后一个元素的索引；如果没有则返回 -1
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
    }
}
```

## 观察者模式

#### 个人理解

这个模式也是平时很少使用的，所以就简单介绍一下，然后结合 JDK 中的源码加深理解。该模式用于定义对象之间的一对多依赖，当一个对象状态改变时，它的所有依赖都会收到通知，然后自动更新。类图和主要角色如下：

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E8%A7%82%E5%AF%9F%E8%80%85%E6%A8%A1%E5%BC%8F%E7%B1%BB%E5%9B%BE-8fee25.png)

- Subject 主题：具有注册、移除及通知观察者的功能，主题是通过维护一个观察者列表来实现这些功能的；
- Observer 观察者：其注册需要 Subject 的 registerObserver()方法。

#### JDK 中的源码实现

java.util 包中提供了 Observable 类和 Observer 接口，其中要求，被观察者需要继承 Observable 类，观察则需要实现 Observer 接口。下面看一下其源码实现。

```java
/**
 * 当一个类希望获知 所观察的对象的变化时，可以通过实现本接口来完成
 * 而被观察的对象则需要继承 Observable 类
 * @author  Chris Warth
 * @see     java.util.Observable
 * @since   JDK1.0
 */
public interface Observer {
    /**
     * 每当所观察的对象发生更改时，此方法都会被调用。应用程序调用Observable对象的
     * notifyObservators()方法时，将通知所有注册的观察者 本对象（被观察者）已更新
     */
    void update(Observable o, Object arg);
}


/**
 * 这个类表示一个可被观察的对象，或者模型视图范例中的“数据”。它可以被继承，
 * 以表示该类 可以被观察到。
 * 一个Observable对象可以有一个或多个观察者，观察者可以是实现了Observer接口的任何对象。
 * 在一个Observable实例更改之后，调用其notifyObservers()方法，该方法可以通过调用 所有
 * 已注册的Observer对象的update()方法通知其所有观察者，被观察的对象已更新。
 * @author  Chris Warth
 * @see     java.util.Observable#notifyObservers()
 * @see     java.util.Observable#notifyObservers(java.lang.Object)
 * @see     java.util.Observer
 * @see     java.util.Observer#update(java.util.Observable, java.lang.Object)
 * @since   JDK1.0
 */
public class Observable {
    private boolean changed = false;
    /**
     * 通过一个Vector来维护 观察者列表。
     * 由于该集合主要涉及元素的增删操作，所以个人认为使用LinkedList
     * 效果会更好一下
     */
    private Vector<Observer> obs;

    public Observable() {
        obs = new Vector<>();
    }

    /**
     * 注册 观察者对象
     */
    public synchronized void addObserver(Observer o) {
        if (o == null)
            throw new NullPointerException();
        if (!obs.contains(o)) {
            obs.addElement(o);
        }
    }

    /**
     * 移除观察者对象
     */
    public synchronized void deleteObserver(Observer o) {
        obs.removeElement(o);
    }

    public void notifyObservers() {
        notifyObservers(null);
    }

    /**
     * 如果此对象已更改，则通知其所有观察者，然后调用clearChanged()方法 清除更改标记
     */
    public void notifyObservers(Object arg) {
        // 一个临时数组缓冲区，用作当前观察者状态的快照
        Object[] arrLocal;

        synchronized (this) {
            if (!changed)
                return;
            arrLocal = obs.toArray();
            clearChanged();
        }

        for (int i = arrLocal.length-1; i>=0; i--)
            ((Observer)arrLocal[i]).update(this, arg);
    }

    /**
     * 清除观察者列表，使此对象不再具有任何观察者
     */
    public synchronized void deleteObservers() {
        obs.removeAllElements();
    }

    /**
     * 将本对象标记为已更改
     */
    protected synchronized void setChanged() {
        changed = true;
    }

    /**
     * 清除本对象的更改标准
     */
    protected synchronized void clearChanged() {
        changed = false;
    }

    /**
     * 查看本对象是否已更改
     */
    public synchronized boolean hasChanged() {
        return changed;
    }

    /**
     * 返回观察者列表的长度
     */
    public synchronized int countObservers() {
        return obs.size();
    }
}
```

## 责任链模式

一般用在消息请求的处理上，如 Netty 的 ChannelHandler 组件，Tomcat 对 HTTP 请求的处理。我们当然可以将 请求的处理逻辑都写在一个类中，但这个类会非常雕肿且不易于维护，不符合开发封闭原则。

在责任链模式中，将上述臃肿的请求处理逻辑 拆分到多个 功能逻辑单一的 Handler 处理类中，这样我们就可以根据业务需求，将多个 Handler 对象组合成一条责任链，实现请求的处理。在一条责任链中，每个 Handler 对象 都包含对下一个 Handler 对象 的引用，一个 Handler 对象 处理完请求消息（或不能处理该请求）时， 会把请求传给下一个 Handler 对象 继续处理，依此类推，直至整条责任链结束。简单看一下责任链模式的类图。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E8%B4%A3%E4%BB%BB%E9%93%BE%E6%A8%A1%E5%BC%8F-8f92a5.png)

#### Netty 中的应用

在 Netty 中，将 Channel 的数据管道抽象为 ChannelPipeline，消息在 ChannelPipeline 中流动和传递。ChannelPipeline 是 ChannelHandler 的容器，持有 I/O 事件拦截器 ChannelHandler 的链表，负责对 ChannelHandler 的管理和调度。由 ChannelHandler 对 I/O 事件 进行拦截和处理，并可以通过接口方便地新增和删除 ChannelHandler 来实现不同业务逻辑的处理。下图是 ChannelPipeline 源码中描绘的责任链事件处理过程。
![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/ChannelPipeline%E8%B4%A3%E4%BB%BB%E9%93%BE%E4%BA%8B%E4%BB%B6%E5%A4%84%E7%90%86%E8%BF%87%E7%A8%8B-af68cd.png)
其具体过程处理如下：

1. 底层 SocketChannel 的 read 方法 读取 ByteBuf，触发 ChannelRead 事件，由 I/O 线程 NioEventLoop 调用 ChannelPipeline 的 fireChannelRead()方法，将消息传输到 ChannelPipeline 中。

2. 消息依次被 InboundHandler 1、InboundHandler 2 … InboundHandler N 拦截处理，在这个过程中，任何 ChannelHandler 都可以中断当前的流程，结束消息的传递。

3. 当调用 ChannelHandlerContext 的 write()方法 发送消息，消息从 OutbountHandler 1 开始 一直到 OutboundHandler N，最终被添加到消息发送缓冲区中等待刷新和发送。

在 Netty 中将事件根据源头的不同分为 InBound 事件 和 OutBound 事件。InBound 事件 通常由 I/O 线程 触发，例如 TCP 链路 建立和关闭、读事件等等，分别会触发相应的事件方法。而 OutBound 事件 则一般由用户主动发起的 网络 I/O 操作，例如用户发起的连接操作，绑定操作和消息发送操作等，也会分别触发相应的事件方法。由于 netty 中提供了一个抽象类 ChannelHandlerAdapter，它默认不处理拦截的事件。所以，在实际编程过程中，我们只需要继承 ChannelHandlerAdapter，在我们的 自定义 Handler 中覆盖业务关心的事件方法即可。其源码如下。

```java
/**
 * 它扮演了 责任链模式中的 Client角色，持有 构造 并使用 ChannelHandler责任链
 */
public interface ChannelPipeline
        extends ChannelInboundInvoker, ChannelOutboundInvoker, Iterable<Entry<String, ChannelHandler>> {

    ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler);

    ChannelPipeline addLast(String name, ChannelHandler handler);

    ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler);

    ChannelPipeline addFirst(ChannelHandler... handlers);

    ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers);

    ChannelPipeline addLast(ChannelHandler... handlers);

    ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers);

    ChannelPipeline remove(ChannelHandler handler);

    ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler);

    ChannelHandler get(String name);

    @Override
    ChannelPipeline fireChannelRegistered();

    @Override
    ChannelPipeline fireChannelUnregistered();

    @Override
    ChannelPipeline fireChannelActive();

    @Override
    ChannelPipeline fireChannelInactive();

    @Override
    ChannelPipeline fireExceptionCaught(Throwable cause);

    @Override
    ChannelPipeline fireUserEventTriggered(Object event);

    @Override
    ChannelPipeline fireChannelRead(Object msg);

    @Override
    ChannelPipeline fireChannelReadComplete();

    @Override
    ChannelPipeline fireChannelWritabilityChanged();
}


/**
 * ChannelHandler本身并不是链式结构的，链式结构是交由 ChannelHandlerContext
 * 进行维护的
 */
public interface ChannelHandler {

    void handlerAdded(ChannelHandlerContext ctx) throws Exception;

    void handlerRemoved(ChannelHandlerContext ctx) throws Exception;

    void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;x
}


/**
 * DefaultChannelHandlerContext 持有一个 final 的 ChannelHandler对象，
 * 其父类 AbstractChannelHandlerContext 是一个双向链表的结构设计，这样就保证了
 * ChannelHandler 的 责任链式处理
 */
final class DefaultChannelHandlerContext extends AbstractChannelHandlerContext {

    private final ChannelHandler handler;

    DefaultChannelHandlerContext(
            DefaultChannelPipeline pipeline, EventExecutor executor, String name, ChannelHandler handler) {
        super(pipeline, executor, name, isInbound(handler), isOutbound(handler));
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
    }

    @Override
    public ChannelHandler handler() {
        return handler;
    }

    private static boolean isInbound(ChannelHandler handler) {
        return handler instanceof ChannelInboundHandler;
    }

    private static boolean isOutbound(ChannelHandler handler) {
        return handler instanceof ChannelOutboundHandler;
    }
}


/**
 * 很容易看出来，这是一个双向链表的结构设计
 */
abstract class AbstractChannelHandlerContext extends DefaultAttributeMap
        implements ChannelHandlerContext, ResourceLeakHint {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannelHandlerContext.class);
    volatile AbstractChannelHandlerContext next;
    volatile AbstractChannelHandlerContext prev;

	......

}
```

# 编码规范

本文用于总结《阿里 Java 开发手册》、《用友技术 review 手册》及个人 Java 开发工作经验，并结合这半年来的源码阅读经验进行编写。回顾那些写过的和读过的代码，回顾自己。

## 第一章 基础编码规范

### 1.1 命名规范

- 代码中的命名均不能以下划线或美元符号开始，也不能以下划线或美元符号结束。  
  tips：JDK 动态代理生成的代理类 类名使用了\$符号开头，如\$Proxy1。

- 代码中的命名严禁使用拼音与英文混合的方式，更不允许直接使用中文的方式。  
  tips：正确的英文拼写和语法可以让阅读者易于理解，避免歧义。注意，即使纯拼音命名方式也要避免采用。alibaba，yonyou，Beijing 等国际通用的名称，可视同英文。  
  在我们的财务相关模块的工程代码及数据库表设计中，可以看到一些拼音首字母缩写的命名方式，如：arap_djzb，arap 是“应收应付”的英文缩写，djzb 是“单据主表”的汉语拼音首字母，zdr、shr、lrr 都是汉语拼音首字母缩写。当然，这些都是历史包袱，经历了这么多年的代码积累，很难从底层去修正咯，但在自己的实际编码中要以史为鉴，让自己的代码更加优雅规范，这也是同事是否尊重你的重要考量之一。

- 类名使用 UpperCamelCase——大驼峰风格，但以下情形例外: DO / BO / DTO/ VO / AO /
  PO / UID 等。  
  tips：合理的类名后缀能够让我们在开发中快速地找到自己想要的代码，想看某个业务层就 ctrl + shift + T 搜索“XXXBO”，想看某展示层代码 就搜索“XXXVO”。

- 抽象类命名使用 Abstract 或 Base 开头；异常类命名使用 Exception 结尾；测试类命名以它要测试的类的名称开始，以 Test 结尾。  
  例如：Spring 框架的 AbstractApplicationContext 和 Mybatis 框架的 BaseExecutor 都是抽象类。

- 方法名、参数名、成员变量、局部变量都统一使用 lowerCamelCase——小驼峰风格。

- 常量命名全部大写，单词间用下划线隔开，力求语义表达完整清楚，不要嫌名字长。  
  tips：实际编码中，有时确实会嫌某常量名太长，不便于使用。以后应该在语义完整清楚的情况下再考虑尽量缩短名称长度。

- 类型与中括号紧挨相连来表示数组。  
  正例：定义整形数组 int[] arrayDemo；  
  反例：在 main 参数中，使用 String args[]来定义。

- POJO 类中布尔类型的变量，都不要加 is 前缀，否则部分框架解析会引起序列化错误。  
  反例：定义为基本数据类型 Boolean isDeleted 的属性，它的方法也是 isDeleted()，RPC 框架在反向解析的时候，“误以为”对应的属性名称是 deleted，导致属性获取不到，进而抛出异常。  
  tips：我们的 VO 类中有很多 is 开头的 Boolean 类型变量，如：DJZBHeaderVO 中的 isjszxzf（是否结算中心支付）字段。

- 包名统一使用小写，点分隔符之间有且仅有一个自然语义的英语单词。包名统一使用 单数形式，但是类名如果有复数含义，类名可以使用复数形式。  
  正例：应用工具类包名为 com.alibaba.ai.util、类名为 MessageUtils（此规则参考 spring 的框架结构）

- 杜绝完全不规范的缩写，避免望文不知义。  
  反例：AbstractClass“缩写”命名成 AbsClass；condition“缩写”命名成 condi，此类随 意缩写严重降低了代码的可阅读性。

- 为了达到代码自解释的目标，任何自定义编程元素在命名时，使用尽量完整的单词 组合来表达其意。  
  正例：在 JDK 中，表达原子更新的类名为：AtomicReferenceFieldUpdater。  
  反例：变量 int a 的随意命名方式。

- 如果模块、接口、类、方法使用了设计模式，在命名时需体现出具体模式。  
  tips：将设计模式体现在名字中，有利于阅读者快速理解架构设计理念。 如：Spring 框架的 BeanFactory（工厂模式）、JdkDynamicAopProxy（JDK 动态代理模式）。

- 接口类中的方法和属性不要加任何修饰符号（public 也不要加），保持代码的简洁性，并加上有效的 Javadoc 注释。尽量不要在接口里定义变量，如果一定要定义变量，肯定是与接口方法相关，并且是整个应用的基础常量。  
  正例：接口方法签名 void commit();  
  接口基础常量 String COMPANY = "alibaba";  
  反例：接口方法定义 public abstract void f();  
  说明：JDK8 中接口允许有默认实现，那么这个 default 方法，是对所有实现类都有价值的默认实现。

- 接口和实现类的命名有两套规则：  
  【强制】对于 Service 和 DAO 类，基于 SOA 的理念，暴露出来的服务一定是接口，内部 的实现类用 Impl 的后缀与接口区别。  
  正例：CacheServiceImpl 实现 CacheService 接口。  
  【推荐】 如果是形容能力的接口名称，取对应的形容词为接口名（通常是–able 的形式）。  
  正例：AbstractTranslator 实现 Translatable 接口。

- 枚举类名建议带上 Enum 后缀，枚举成员名称需要全大写，单词间用下划线隔开。  
  说明：枚举其实就是特殊的类，域成员均为常量，且构造方法被默认强制是私有。  
  正例：枚举名字为 ProcessStatusEnum 的成员名称：SUCCESS / UNKNOWN_REASON。

- 各层命名规约：  
  A) Service/DAO 层方法命名规约  
  1） 获取单个对象的方法用 get 做前缀。  
  2） 获取多个对象的方法用 list 做前缀，复数形式结尾如：listObjects。  
  3） 获取统计值的方法用 count 做前缀。  
  4） 插入的方法用 save/insert 做前缀。  
  5） 删除的方法用 remove/delete 做前缀。  
  6） 修改的方法用 update 做前缀。  
  B) 领域模型命名规约  
  1） 数据对象：xxxDO，xxx 即为数据表名。  
  2） 数据传输对象：xxxDTO，xxx 为业务领域相关的名称。  
  3） 展示对象：xxxVO，xxx 一般为网页名称。  
  4） POJO 是 DO/DTO/BO/VO 的统称，禁止命名成 xxxPOJO。

### 1.2 常量定义

- 不允许任何魔法值（意义不明的变量 / 常量）直接出现在代码中。  
  反例：  
  String key = "Id#taobao\_" + tradeId;  
  cache.put(key, value);

- 在 long 或者 Long 赋值时，数值后使用大写的 L，不能是小写的 l，小写容易跟数字 1 混淆，造成误解。  
  说明：Long a = 2l; 写的是数字的 21，还是 Long 型的 2?

- 不要使用一个常量类维护所有常量，要按常量功能进行归类，分开维护。  
  说明：大而全的常量类，杂乱无章，使用查找功能才能定位到修改的常量，不利于理解和维护。  
  正例：缓存相关常量放在类 CacheConsts 下；系统配置相关常量放在类 ConfigConsts 下。

- 常量的复用层次有五层：跨应用共享常量、应用内共享常量、子工程内共享常量、包内共享常量、类内共享常量。  
  1）跨应用共享常量：放置在二方库中，通常是 client.jar 中的 constant 目录下。  
  2）应用内共享常量：放置在一方库中，通常是子模块中的 constant 目录下。  
  反例：易懂变量也要统一定义成应用内共享常量，两位攻城师在两个类中分别定义了表示 “是” 的变量。  
  类 A 中：public static final String YES = "yes"；  
  类 B 中：public static final String YES = "y"；  
  A.YES.equals(B.YES)，预期是 true，但实际返回为 false，导致线上问题。  
  3）子工程内部共享常量：即在当前子工程的 constant 目录下。  
  4）包内共享常量：即在当前包下单独的 constant 目录下。  
  5）类内共享常量：直接在类内部 private static final 定义。

- 如果变量值仅在一个固定范围内变化用 enum 类型来定义。  
  说明：如果存在名称之外的延伸属性应使用 enum 类型，下面正例中的数字就是延伸信息，表示一年中的第几个季节。  
  正例：

```java
public enum SeasonEnum {
     SPRING(1), SUMMER(2), AUTUMN(3), WINTER(4);
     private int seq;

     SeasonEnum(int seq){
         this.seq = seq;
     }
}
```

### 1.3 代码格式

代码格式无非就是一些空格、换行、缩进的问题，没必要死记，直接用开发工具（eclipse、IDEA）format 一下即可，省时省力。

### 1.4 OOP 规约

- 避免通过一个类的对象引用访问此类的静态变量或方法，增加编译器解析成本。

- 所有的覆写方法，必须加@Override 注解。  
  说明：getObject()与 get0bject()的问题。一个是字母的 O，一个是数字的 0，加@Override 可以准确判断是否覆盖成功。另外，如果在抽象类中对方法签名（由方法名、参数的类型及**顺序** 确定唯一的方法签名）进行修改，其实现类会马上编译报错。

- 相同参数类型，相同业务含义，才可以使用 Java 的可变参数，避免使用 Object。  
  说明：可变参数必须放置在参数列表的最后。（能用数组的就不要使用可变参数编程，可变参数在编译时会被编译成数组类型。可变参数能兼容数组类参数，但是数组类参数却无法兼容可变参数。可变参数类型必须作为参数列表的最后一项，且不能放在定长参数的前面。）  
  正例：public List<User> listUsers(String type, Long... ids) {...}

- 外部正在调用或者二方库依赖的接口，不允许修改方法签名，避免对接口调用方产生影响。接口过时必须加@Deprecated 注解，并清晰地说明采用的新接口或者新服务是什么。  
  tips：  
  一方库：本工程范围内，各个模块和包之间的相互依赖。  
  二方库：引入的同一个公司内部的其他工程。  
  三方库：公司以外的其他依赖，比如 apache，google 等。

- 不能使用过时的类或方法。  
  说明：java.net.URLDecoder 中的方法 decode(String encodeStr) 这个方法已经过时，应该使用双参数 decode(String source, String encode)。接口提供方既然明确是过时接口， 那么有义务同时提供新的接口；作为调用方来说，有义务去考证过时方法的新实现是什么。

- Object 的 equals 方法容易抛空指针异常，应使用常量或确定有值的对象来调用 equals。  
  正例："test".equals(object);  
  反例：object.equals("test");  
  说明：推荐使用 java.util.Objects#equals（JDK7 引入的工具类）。**个人认为，当要比较两个不确定的对象时，可以考虑使用这个类，如果只是想确定某个对象是否为目标值，使用上面的方法并不差**。

- 所有的相同类型的包装类对象之间值的比较，全部使用 equals() 方法比较。  
  说明：对于 Integer var = ? 在-128 至 127 范围内的赋值，Integer 对象是在 IntegerCache.cache 产生，会复用已有对象，这个区间内的 Integer 值可以直接使用 == 进行判断，但是这个区间之外的所有数据，都会在堆上产生，并不会复用已有对象，这是一个大坑， 推荐使用 equals() 方法进行判断。

- 关于基本数据类型与包装数据类型的使用标准如下：  
  1）【强制】所有的 POJO 类属性必须使用包装数据类型。  
  2）【强制】RPC 方法的返回值和参数必须使用包装数据类型。  
  3）【推荐】所有的局部变量使用基本数据类型。  
  说明：POJO 类属性没有初值是提醒使用者在需要使用时，必须自己显式地进行赋值，任何 NPE 问题，或者入库检查，都由使用者来保证。  
  正例：数据库的查询结果可能是 null，因为自动拆箱，用基本数据类型接收有 NPE 风险。(不是很理解这个结论，ResultSet.getInt()等方法获得的是基本数据类型，ORM 映射时怎么就拆箱咯？)  
  反例：比如显示成交总额涨跌情况，即正负 x%，x 为基本数据类型，调用的 RPC 服务，调用不成功时，返回的是默认值，页面显示为 0%，这是不合理的，应该显示成中划线。所以包装数据类型的 null 值，能够表示额外的信息，如：远程调用失败，异常退出。

- 定义 DO/DTO/VO 等 POJO 类时，不要设定任何属性默认值。  
  反例：POJO 类的 gmtCreate 默认值为 new Date()，但是这个属性在数据提取时并没有置入具 体值，在更新其它字段时又附带更新了此字段，导致创建时间被修改成当前时间。

- 序列化类新增属性时，请不要修改 serialVersionUID 字段，避免反序列化失败；如果完全不兼容升级，避免反序列化混乱，那么请修改 serialVersionUID 值。  
  说明：注意 serialVersionUID 不一致会抛出序列化运行时异常。

- **构造方法里面禁止加入任何业务逻辑，如果有初始化逻辑，请放在 init() 方法中。**  
  在很多 client 端的代码中有看到这种编码方式。

- POJO 类必须写 toString() 方法。使用 IDE 中的工具：source -> generate toString() 时，如果继承了另一个 POJO 类，注意在前面加一下 super.toString()。  
  说明：在方法执行抛出异常时，可以直接调用 POJO 的 toString()方法打印其属性值，便于排查问题。

- 当一个类有多个构造方法，或者多个同名方法，这些方法应该按顺序放置在一起， 便于阅读。

- 类内方法定义的顺序依次是：公有方法或保护方法 > 私有方法 > getter/setter 方法。  
  说明：公有方法是类的调用者和维护者最关心的方法，首屏展示最好；保护方法虽然只是子类 关心，也可能是“模板设计模式”下的核心方法；而私有方法外部一般不需要特别关心，是一个 黑盒实现；因为承载的信息价值较低，所有 Service 和 DAO 的 getter/setter 方法放在类体 最后。

- setter 方法中，参数名称与类成员变量名称一致，this.成员名 = 参数名。在 getter/setter 方法中，不要增加业务逻辑，增加排查问题的难度。

- 循环体内，字符串的连接方式，使用 StringBuilder 的 append 方法进行扩展。

- final 可以声明类、成员变量、方法、以及本地变量，下列情况使用 final 关键字：  
  1） 不允许被继承的类，如：String 类。  
  2） 不允许修改引用的域对象。  
  3） 不允许被重写的方法，如：POJO 类的 setter 方法。  
  4） **不允许运行过程中重新赋值的局部变量**，可以看到有些方法的形参中使用了 final 修饰。  
  5） 避免上下文重复使用一个变量，使用 final 描述可以强制重新定义一个变量，方便更好 地进行重构。

- 慎用 Object 的 clone 方法来拷贝对象。  
  说明：对象的 clone 方法默认是浅拷贝，若想实现深拷贝需要重写 clone 方法实现域对象的 深度遍历式拷贝。

- **类成员与方法访问控制从严**（合理使用 Java 的访问修饰符）：  
  1） 如果不允许外部直接通过 new 来创建对象，那么构造方法必须是 private。  
  2） 工具类不允许有 public 或 default 构造方法。  
  3） 类非 static 成员变量并且与子类共享，必须是 protected。  
  4） 类非 static 成员变量并且仅在本类使用，必须是 private。  
  5） 类 static 成员变量如果仅在本类使用，必须是 private。  
  6） 若是 static 成员变量，考虑是否为 final。  
  7） 类成员方法只供类内部调用，必须是 private。  
  8） 类成员方法只对继承类公开，那么限制为 protected。 说明：任何类、方法、参数、变量，严控访问范围。过于宽泛的访问范围，不利于模块解耦。  
  思考：如果是一个 private 的方法，想删除就删除，可是一个 public 的 service 成员方法或成员变量，删除一下，不得手心冒点汗吗？变量像自己的小孩，尽量在自己的视线内，变量作 用域太大，无限制的到处跑，那么你会担心的。

### 1.5 集合处理

- 关于 hashCode 和 equals 的处理，遵循如下规则：  
  1） 只要重写 equals，就必须重写 hashCode。  
  2） 因为 Set 存储的是不重复的对象，依据 hashCode 和 equals 进行判断，所以 Set 存储的 对象必须重写这两个方法。  
  3） 如果自定义对象作为 Map 的键，那么必须重写 hashCode 和 equals。  
  说明：String 重写了 hashCode 和 equals 方法，所以我们可以非常愉快地使用 String 对象 作为 key 来使用。

- ArrayList 的 subList 结果不可强转成 ArrayList，否则会抛出 ClassCastException 异常，即 java.util.RandomAccessSubList cannot be cast to java.util.ArrayList。  
  说明：subList 返回的是 ArrayList 的内部类 SubList，并不是 ArrayList 而是 ArrayList 的一个视图，对于 SubList 子列表的所有操作最终会反映到原列表上。

- 在 subList 场景中，高度注意对原集合元素的增加或删除，均会导致子列表的遍历、 增加、删除产生 ConcurrentModificationException 异常。

- 使用集合转数组的方法，必须使用集合的 toArray(T[] array)方法，传入的是类型完全一样的数组，大小就是 list.size()。  
  说明：使用 toArray 带参方法，入参分配的数组空间不够大时，toArray 方法内部将重新分配 内存空间，并返回新数组地址；如果数组元素个数大于实际所需，下标为[ list.size() ] 的数组元素将被置为 null，其它数组元素保持原值，因此最好将方法入参数组大小定义与集 合元素个数一致。

```java
// 正例：
List<String> list = new ArrayList<String>(2);
list.add("guan");
list.add("bao");
String[] array = new String[list.size()];
array = list.toArray(array);
// 反例：直接使用 toArray 无参方法存在问题，此方法返回值只能是 Object[]类，
// 若强转其它 类型数组将出现 ClassCastException 错误。

// 这是我平时的写法，初始化一个list.size()大小的数组似乎效率更好一些，如果数组的容量
// 比list小，原来的数组对象不会被使用，浪费系统资源
String[] strs = list.toArray(new String[0]);
```

- 使用工具类 Arrays.asList()把数组转换成集合时，不能使用其修改集合相关的方法，它的 add/remove/clear 方法会抛出 UnsupportedOperationException 异常。  
  说明：asList() 的返回对象是一个 Arrays 的内部类 ArrayList（而不是 java.util.ArrayList），该内部类 并没有实现集合的修改/删除等方法。Arrays.asList 体现的是适配器模式，只是转换接口，后台的数据仍是数组。

```java
String[] str = new String[] { "you", "wu" };
List list = Arrays.asList(str);
第一种情况：list.add("yangguanbao"); 运行时异常。
第二种情况：str[0] = "gujin"; 那么 list.get(0)也会随之修改。
```

- 泛型通配符 `<? extends T>` 来接收返回的数据，此写法的泛型集合不能使用 add() 方 法，而 `<? super T>` 不能使用 get() 方法，作为接口调用赋值时易出错。

```java
// <? extends T>:上界通配符(Upper Bounds Wildcards）
// <? super T>:下界通配符(Lower Bounds Wildcards)
List<? extends C> list1; // list1 的元素的类型只能是 C 和 C 的子类。
List<? super C> list2; // list2 的元素的类型只能是 C 和 C 的父类。
// 简单来说就是 <? extends C> 上界为 C 类型范围粗略理解为 [C,+∞)，
// 不允许添加除 null 的元素，获取的元素类型是 C ；
// <? super C> 下界为 C 类型范围粗略理解为 (-∞,C]，允许添加 C 以及 C 的子类类型元素，
// 获取的元素类型是 Object

// 扩展说一下 PECS(Producer Extends Consumer Super)原则。
// 第一、频繁往外读取内容的，适合用<? extends T>。
// 第二、经常往里插入的，适合用<? super T>。
```

- 不要在 foreach 循环里进行元素的 remove/add 操作。remove 元素请使用 Iterator 方式，如果并发操作，需要对 Iterator 对象加锁。

```java
// 正例：
List<String> list = new ArrayList<>();
list.add("1");
list.add("2");
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    String item = iterator.next();
    if (删除元素的条件) {
        iterator.remove();
    }
}

// 反例：对比ArrayList的remove()和Iterator的remove()方法，可以找到其中的坑。
for (String item : list) {
    if ("1".equals(item)) {
        list.remove(item);
    }
}
```

- 在 JDK7 版本及以上，Comparator 实现类要满足如下三个条件，不然 Arrays.sort()， Collections.sort() 会报 IllegalArgumentException 异常。  
  说明：三个条件如下 1） x，y 的比较结果和 y，x 的比较结果相反。 2） x>y，y>z，则 x>z。 3） x=y，则 x，z 比较结果和 y，z 比较结果相同。

```java
// 反例：下例中没有处理相等的情况，实际使用中可能会出现异常：
new Comparator<Student>() {
    @Override
    public int compare(Student o1, Student o2) {
    	return o1.getId() > o2.getId() ? 1 : -1;
    }
};
```

- 集合泛型定义时，在 JDK7 及以上，使用 diamond 语法或全省略。  
  说明：菱形泛型，即 diamond，直接使用<>来指代前边已经指定的类型。

```java
// 正例：
// <> diamond 方式
HashMap<String, String> userCache = new HashMap<>(16);
// 全省略方式
ArrayList<User> users = new ArrayList(10);
```

- 集合初始化时，指定集合初始值大小。  
  说明：HashMap 使用 HashMap(int initialCapacity) 初始化。
  正例：initialCapacity = (需要存储的元素个数 / 负载因子) + 1。注意负载因子（即 loader factor）默认为 0.75，如果暂时无法确定初始值大小，请设置为 16（即默认值）。  
  反例：HashMap 需要放置 1024 个元素，由于没有设置容量初始大小，随着元素不断增加，容量 7 次被迫扩大，resize() 需要重建 hash 表，严重影响性能。

- 使用 entrySet 遍历 Map 类集合 K-V，而不是 keySet 方式进行遍历。  
  说明：keySet 其实是遍历了 2 次，一次是转为 Iterator 对象，另一次是从 hashMap 中取出 key 所对应的 value。而 entrySet 只是遍历了一次就把 key 和 value 都放到了 entry 中，效率更高。如果是 JDK8，使用 Map.foreach() 方法。  
  正例：values()返回的是 V 值集合，是一个 list 集合对象；keySet()返回的是 K 值集合，是 一个 Set 集合对象；entrySet()返回的是 K-V 值组合集合。

```java
// 创建一个Map
Map<String, Object> infoMap = new HashMap<>();
infoMap.put("name", "Zebe");
infoMap.put("site", "www.zebe.me");
infoMap.put("email", "zebe@vip.qq.com");

// 传统的Map迭代方式
for (Map.Entry<String, Object> entry : infoMap.entrySet()) {
    System.out.println(entry.getKey() + "：" + entry.getValue());
}

// JDK8的迭代方式
infoMap.forEach((key, value) -> {
    System.out.println(key + "：" + value);
});
```

- 高度注意 Map 类集合 K-V 能不能存储 null 值的情况，如下表格：

| 集合类            | Key             | Value           | Super       | 说明                   |
| ----------------- | --------------- | --------------- | ----------- | ---------------------- |
| HashMap           | **允许为 null** | **允许为 null** | AbstractMap | 线程不安全             |
| ConcurrentHashMap | 不允许为 null   | 不允许为 null   | AbstractMap | 锁分段技术（JDK8:CAS） |
| Hashtable         | 不允许为 null   | 不允许为 null   | Dictionary  | 线程安全               |
| TreeMap           | 不允许为 null   | **允许为 null** | AbstractMap | 线程不安全             |

反例： 由于 HashMap 的干扰，很多人认为 ConcurrentHashMap 是可以置入 null 值，而事实上， 存储 null 值时会抛出 NPE 异常。

- 合理利用好集合的有序性(sort)和稳定性(order)，避免集合的无序性(unsort)和不稳定性(unorder)带来的负面影响。  
  说明：有序性是指遍历的结果是按某种比较规则依次排列的。稳定性指集合每次遍历的元素次序是一定的。如：ArrayList 是 order/unsort；HashMap 是 unorder/unsort；TreeSet 是 order/sort。

- 利用 Set 元素唯一的特性，可以快速对一个集合进行去重操作，避免使用 List 的 contains 方法进行遍历、对比、去重操作。

### 1.6 并发处理

- 获取单例对象需要保证线程安全，其中的方法也要保证线程安全。  
  说明：资源驱动类、工具类、单例工厂类都需要注意。

- 创建线程或线程池时请指定有意义的线程名称，方便出错时回溯。

```java
// 正例：
public class TimerTaskThread extends Thread {
	public TimerTaskThread() {
		super.setName("TimerTaskThread");
		...
	}
}
```

- 线程资源必须通过线程池提供，不允许在应用中自行显式创建线程。  
  说明：使用线程池的好处是减少在创建和销毁线程上所消耗的时间以及系统资源的开销，解决资源不足的问题。如果不使用线程池，有可能造成系统创建大量同类线程而导致消耗完内存或者“过度切换”的问题。  
  tips:我们的代码中的很多线程都是自行显式创建的，很少见到通过线程池进行统一管理的。

- 线程池不允许使用 Executors 去创建，而是通过 ThreadPoolExecutor 的方式，这样的处理方式让写的同学更加明确线程池的运行规则，规避资源耗尽的风险。  
  说明：Executors 返回的线程池对象的弊端如下：  
  1）FixedThreadPool 和 SingleThreadPool: 允许的请求队列长度为 Integer.MAX_VALUE，可能会堆积大量的请求，从而导致 OOM。  
  2）CachedThreadPool 和 ScheduledThreadPool: 允许的创建线程数量为 Integer.MAX_VALUE，可能会创建大量的线程，从而导致 OOM。

- SimpleDateFormat 是线程不安全的类，一般不要定义为 static 变量，如果定义为 static，必须加锁，或者使用 DateUtils 工具类。

```java
// 正例：注意线程安全，使用 DateUtils。亦推荐如下处理：
private static final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
	@Override
	protected DateFormat initialValue() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}
};
// 说明：如果是 JDK8 的应用，可以使用 Instant 代替 Date，LocalDateTime 代替 Calendar，
// DateTimeFormatter 代替 SimpleDateFormat，
// 官方给出的解释：simple beautiful strong immutable thread-safe。
```

- 高并发时，同步调用应该去考量锁的性能损耗。能用无锁数据结构，就不要用锁；能锁区块，就不要锁整个方法体；能用对象锁，就不要用类锁。  
  说明：尽可能使加锁的代码块工作量尽可能的小，**避免在锁代码块中调用 RPC 方法**。

- **对多个资源、数据库表、对象同时加锁时，需要保持一致的加锁顺序**，否则可能会造成死锁。  
  说明：线程一需要对表 A、B、C 依次全部加锁后才可以进行更新操作，那么线程二的加锁顺序也必须是 A、B、C，否则可能出现死锁。

- 并发修改同一记录时，避免更新丢失，需要加锁。要么在应用层加锁，要么在缓存加锁，要么在数据库层使用乐观锁，使用 version 作为更新依据。  
  说明：如果每次访问冲突概率小于 20%，推荐使用乐观锁，否则使用悲观锁。乐观锁的重试次数不得小于 3 次。

- 多线程并行处理定时任务时，Timer 运行多个 TimeTask 时，只要其中之一没有捕获抛出的异常，其它任务便会自动终止运行，使用 ScheduledExecutorService 则没有这个问题。

- 使用 CountDownLatch 进行异步转同步操作，每个线程退出前必须调用 countDown 方法，线程执行代码注意 catch 异常，确保 countDown 方法被执行到，避免主线程无法执行至 await 方法，直到超时才返回结果。  
  说明：注意，子线程抛出异常堆栈，不能在主线程 try-catch 到。

- 避免 Random 实例被多线程使用，虽然共享该实例是线程安全的，但会因竞争同一 seed 导致的性能下降。  
  说明：Random 实例包括 java.util.Random 的实例或者 Math.random()的方式。  
  正例：在 JDK7 之后，可以直接使用 API ThreadLocalRandom，而在 JDK7 之前，需要编码保证每个线程持有一个实例。

- 在并发场景下，通过双重检查锁（double-checked locking）实现延迟初始化的优化问题隐患(指令重排会导致 双检锁失效，产生隐患)(可参考 The "Double-Checked Locking is Broken" Declaration)，推荐解决方案中较为简单一种（适用于 JDK5 及以上版本），将目标属性声明为 volatile 型。

```java
// 反例：
class LazyInitDemo {
	private Helper helper = null;
	public Helper getHelper() {
		if (helper == null)
		synchronized(this) {
			if (helper == null)
				helper = new Helper();
			}
		return helper;
	}
	// other methods and fields...
}
```

- volatile 解决多线程内存不可见问题。对于一写多读，是可以解决变量同步问题， 但是如果多写，同样无法解决线程安全问题。

```java
// 如果是 count++操作，使用如下类实现：
AtomicInteger count = new AtomicInteger();
count.addAndGet(1);
// 如果是 JDK8，推 荐使用 LongAdder 对象，比 AtomicLong 性能更好（减少乐观锁的重试次数）。
```

- HashMap 在容量不够进行 resize 时由于高并发可能出现死链，导致 CPU 飙升，在开发过程中可以使用其它数据结构或加锁来规避此风险。

- ThreadLocal 无法解决共享对象的更新问题，ThreadLocal 对象建议使用 static 修饰。这个变量是针对一个线程内所有操作共享的，所以设置为静态变量，所有此类实例共享此静态变量 ，也就是说在类第一次被使用时装载，只分配一块存储空间，所有此类的对象(只要是这个线程内定义的)都可以操控这个变量。

### 1.7 控制语句

- 在一个 switch 块内，每个 case 要么通过 break/return 等来终止，要么注释说明程序将继续执行到哪一个 case 为止；在一个 switch 块内，都必须包含一个 default 语句并且放在最后，即使空代码。

- 在 if/else/for/while/do 语句中必须使用大括号，即使只有一行代码。

- 在高并发场景中，避免使用“等于”判断作为中断或退出的条件。  
  说明：如果并发控制没有处理好，容易产生等值判断被“击穿”的情况，使用大于或小于的区间判断条件来代替。  
  反例：判断剩余奖品数量等于 0 时，终止发放奖品，但因为并发处理错误导致奖品数量瞬间变成了负数，这样的话，活动无法终止。

- 表达异常的分支时，少用 if-else 方式，这种方式可以改写成：

```java
if (condition) {
	...
	return obj;
}
// 接着写 else 的业务逻辑代码;
// 说明：如果非得使用 if()...else if()...else...方式表达逻辑，【强制】避免后续代码维护困难，
// 		请勿超过 3 层。
// 正例：超过 3 层的 if-else 的逻辑判断代码可以使用卫语句、策略模式、状态模式等来实现，其中卫语句示例如下：
public void today() {
	if (isBusy()) {
		System.out.println(“change time.”);
		return;
	}
	if (isFree()) {
		System.out.println(“go to travel.”);
		return;
	}
 	System.out.println(“stay at home to learn Alibaba Java Coding Guidelines.”);
 	return;
}
```

- 除常用方法（如 getXxx/isXxx）等外，不要在条件判断中执行其它复杂的语句，将复杂逻辑判断的结果赋值给一个有意义的布尔变量名，以提高可读性。  
  说明：很多 if 语句内的逻辑相当复杂，阅读者需要分析条件表达式的最终结果，才能明确什么样的条件执行什么样的语句，那么，如果阅读者分析逻辑表达式错误呢？

```java
// 正例：
// 伪代码如下
final boolean existed = (file.open(fileName, "w") != null) && (...) || (...);
if (existed) {
	...
}
// 反例：在我们的代码中可以看到很多这种把复杂冗长的逻辑判断写在if语句中的
if ((file.open(fileName, "w") != null) && (...) || (...)) {
    ...
}
```

- **循环体中的语句要考量性能**，以下操作尽量移至循环体外处理，如：定义对象、变量、 获取数据库连接，进行不必要的 try-catch 操作（这个 try-catch 是否可以移至循环体外）。

- 避免采用取反逻辑运算符。  
  说明：取反逻辑不利于快速理解，并且取反逻辑写法必然存在对应的正向逻辑写法。  
  正例：使用 if (x < 628) 来表达 x 小于 628。  
  反例：使用 if (!(x >= 628)) 来表达 x 小于 628。

- 接口入参保护(即，参数校验)，这种场景常见的是用作批量操作的接口。

- 下列情形，需要进行参数校验：  
  1） 调用频次低的方法。  
  2） 执行时间开销很大的方法。此情形中，参数校验时间几乎可以忽略不计，但如果因为参数错误导致中间执行回退，或者错误，那得不偿失。  
  3） 需要极高稳定性和可用性的方法。  
  4） 对外提供的开放接口，不管是 RPC/API/HTTP 接口。  
  5） 敏感权限入口。

- 下列情形，不需要进行参数校验：  
  1） 极有可能被循环调用的方法。但在方法说明里必须注明外部参数检查要求。  
  2） 底层调用频度比较高的方法。毕竟是像纯净水过滤的最后一道，参数错误不太可能到底层才会暴露问题。一般 DAO 层与 Service 层都在同一个应用中，部署在同一台服务器中，所以 DAO 的参数校验，可以省略。  
  3） 被声明成 private 只会被自己代码所调用的方法，如果能够确定调用方法的代码传入参数已经做过检查或者肯定不会有问题，此时可以不校验参数。

### 1.8 注释规约

注释感觉是我们代码规范的重灾区咯，也是大家最容易忽略的地方。

- 类、类属性、类方法的注释必须使用 Javadoc 规范，使用/\*_ 内容 _/格式，不得使用 // xxx 方式。 说明：在 IDE 编辑窗口中，Javadoc 方式会提示相关注释，生成 Javadoc 可以正确输出相应注 释；在 IDE 中，工程调用方法时，不进入方法即可悬浮提示方法、参数、返回值的意义，提高阅读效率。

- 所有的抽象方法（包括接口中的方法）必须要用 Javadoc 注释、除了返回值、参数、 异常说明外，还必须指出该方法做什么事情，实现什么功能。  
  说明：对子类的实现要求，或者调用注意事项，请一并说明。

- 所有的类都必须添加创建者和创建日期。

- 方法内部单行注释，在被注释语句上方另起一行，使用 // 注释。方法内部多行注释 使用 /\* \*/ 注释，注意与代码对齐。

- 所有的枚举类型字段必须要有注释，说明每个数据项的用途。

- 与其用“半吊子”英文来注释，不如用中文注释把问题说清楚。专有名词与关键字保持英文原文即可。  
  反例：“TCP 连接超时” 解释成 “传输控制协议连接超时”，理解反而费脑筋。

- 代码修改的同时，注释也要进行相应的修改，尤其是参数、返回值、异常、核心逻辑等的修改。  
  说明：代码与注释更新不同步，就像路网与导航软件更新不同步一样，如果导航软件严重滞后， 就失去了导航的意义。

- 谨慎注释掉代码。在上方详细说明，而不是简单地注释掉。如果无用，则删除。  
  说明：代码被注释掉有两种可能性。  
  1）后续会恢复此段代码逻辑。  
  2）永久不用。前者如果没有备注信息，难以知晓注释动机。后者建议直接删掉（代码仓库保存了历史代码）。

- 对于注释的要求：  
  第一、能够准确反应设计思想和代码逻辑；  
  第二、能够描述业务含义，使别的程序员能够迅速了解到代码背后的信息。完全没有注释的大段代码对于阅读者形同天书，注释是给自己看的，即使隔很长时间，也能清晰理解当时的思路；注释也是给继任者看的，使其能够快速接替自己的工作。

- 好的命名、代码结构是自解释的，注释力求精简准确、表达到位。避免出现注释的一个极端：过多过滥的注释，代码的逻辑一旦修改，修改注释是相当大的负担。

```java
// 反例：
// put elephant into fridge
put(elephant, fridge);
// 方法名 put，加上两个有意义的变量名 elephant 和 fridge，已经说明了这是在干什么，
// 语义清晰的代码不需要额外的注释。
```

- 特殊注释标记，请注明标记人与标记时间。注意及时处理这些标记，通过标记扫描， 经常清理此类标记。线上故障有时候就是来源于这些标记处的代码。  
  1） 待办事宜（TODO）:（ 标记人，标记时间，[预计处理时间] ）  
  表示需要实现，但目前还未实现的功能。这实际上是一个 Javadoc 的标签，目前的 Javadoc 还没有实现，但已经被广泛使用。只能应用于类，接口和方法（因为它是一个 Javadoc 标签）。  
  2） 错误，不能工作（FIXME）:（ 标记人，标记时间，[预计处理时间] ）  
  在注释中用 FIXME 标记某代码是错误的，而且不能工作，需要及时纠正的情况。

### 1.9 其它

- 在使用正则表达式时，利用好其预编译功能，可以有效加快正则匹配速度。  
  说明：不要在方法体内定义：Pattern pattern = Pattern.compile(“规则”);

- velocity 调用 POJO 类的属性时，建议直接使用属性名取值即可，模板引擎会自动按规范调用 POJO 的 getXxx()，如果是 boolean 基本数据类型变量（boolean 命名不需要加 is 前缀），会自动调用 isXxx()方法。  
  说明：注意如果是 Boolean 包装类对象，优先调用 getXxx()的方法。

- 注意 Math.random() 这个方法返回是 double 类型，注意取值的范围 0≤x<1（能够 取到零值，注意除零异常），如果想获取整数类型的随机数，不要将 x 放大 10 的若干倍然后取整，直接使用 Random 对象的 nextInt 或者 nextLong 方法。

- 获取当前毫秒数 System.currentTimeMillis(); 而不是 new Date().getTime();  
  说明：如果想获取更加精确的纳秒级时间值，使用 System.nanoTime()的方式。在 JDK8 中， 针对统计时间等场景，推荐使用 Instant 类。

- 不要在视图模板中加入任何复杂的逻辑。  
  说明：根据 MVC 理论，视图的职责是展示，不要抢模型和控制器的活。

- 任何数据结构的构造或初始化，都应指定大小，避免数据结构无限增长吃光内存。

- 及时清理不再使用的代码段或配置信息。  
  说明：对于垃圾代码或过时配置，坚决清理干净，避免程序过度臃肿，代码冗余。  
  正例：对于暂时被注释掉，后续可能恢复使用的代码片断，在注释代码上方，统一规定使用三个斜杠(///)来说明注释掉代码的理由。

## 第二章 异常与日志规范

### 2.1 异常处理

- Java 类库中定义的可以通过预检查方式规避的 RuntimeException 异常不应该通过 catch 的方式来处理，比如：NullPointerException，IndexOutOfBoundsException 等等。  
  说明：无法通过预检查的异常除外，比如，**在解析字符串形式的数字时，不得不通过 catch NumberFormatException 来实现**。  
  正例：if (obj != null) {...}  
  反例：try { obj.method(); } catch (NullPointerException e) {…}

- 异常不要用来做流程控制，条件控制。  
  说明：异常设计的初衷是解决程序运行中的各种意外情况，且异常的处理效率比条件判断方式要低很多。

- catch 时请分清稳定代码和非稳定代码，稳定代码指的是无论如何不会出错的代码。 对于非稳定代码的 catch 尽可能进行区分异常类型，再做对应的异常处理。  
  说明：对大段代码进行 try-catch，使程序无法根据不同的异常做出正确的应激反应，也不利于定位问题，这是一种不负责任的表现。  
  正例：用户注册的场景中，如果用户输入非法字符，或用户名称已存在，或用户输入密码过于简单，在程序上作出分门别类的判断，并提示给用户。

- 捕获异常是为了处理它，不要捕获了却什么都不处理而抛弃之，如果不想处理它，请将该异常抛给它的调用者。最外层的业务使用者，必须处理异常，将其转化为用户可以理解的内容。

- 有 try 块放到了事务代码中，catch 异常后，如果需要回滚事务，一定要注意手动回滚事务。

- finally 块必须对资源对象、流对象进行关闭，有异常也要做 try-catch。  
  说明：如果 JDK7 及以上，可以使用 try-with-resources 方式。

- 不要在 finally 块中使用 return。  
  说明：finally 块中的 return 返回后方法结束执行，不会再执行 try 块中的 return 语句。

- 捕获异常与抛异常，必须是完全匹配，或者捕获异常是抛异常的父类。  
  说明：如果预期对方抛的是绣球，实际接到的是铅球，就会产生意外情况。

- 方法的返回值可以为 null，不强制返回空集合，或者空对象等，必须添加注释充分说明什么情况下会返回 null 值。  
  说明：本手册明确防止 NPE 是调用者的责任。即使被调用方法返回空集合或者空对象，对调用者来说，也并非高枕无忧，必须考虑到远程调用失败、序列化失败、运行时异常等场景返回 null 的情况。

- 防止 NPE，是程序员的基本修养，注意 NPE 产生的场景：  
  1）返回类型为基本数据类型，return 包装数据类型的对象时，自动拆箱有可能产生 NPE。  
  反例：public int f() { return Integer 对象}， 如果为 null，自动解箱抛 NPE。  
  2） 数据库的查询结果可能为 null。  
  3） 集合里的元素即使 isNotEmpty，取出的数据元素也可能为 null。  
  4） 远程调用返回对象时，一律要求进行空指针判断，防止 NPE。  
  5） 对于 Session 中获取的数据，建议 NPE 检查，避免空指针。  
  6） 级联调用 obj.getA().getB().getC()；一连串调用，易产生 NPE。  
  正例：使用 JDK8 的 Optional 类来防止 NPE 问题。

- 定义时区分 unchecked / checked 异常，避免直接抛出 new RuntimeException()， 更不允许抛出 Exception 或者 Throwable，应使用有业务含义的自定义异常。推荐业界已定义过的自定义异常，如：DAOException / ServiceException 等。

- 对于公司外的 http/api 开放接口必须使用“错误码”；而应用内部推荐异常抛出； 跨应用间 RPC 调用优先考虑使用 Result 方式，封装 isSuccess()方法、“错误码”、“错误简短信息”。  
  说明：关于 RPC 方法返回方式使用 Result 方式的理由。  
  1）使用抛异常返回方式，调用方如果没有捕获到就会产生运行时错误。  
  2）如果不加栈信息，只是 new 自定义异常，加入自己的理解的 error message，对于调用端解决问题的帮助不会太多。如果加了栈信息，在频繁调用出错的情况下，数据序列化和传输的性能损耗也是问题。

- 避免出现重复的代码（Don’t Repeat Yourself），即 DRY 原则。  
  说明：随意复制和粘贴代码，必然会导致代码的重复，在以后需要修改时，需要修改所有的副本，容易遗漏。必要时抽取共性方法，或者抽象公共类，甚至是组件化。

```java
// 正例：
// 一个类中有多个 public 方法，都需要进行数行相同的参数校验操作，这个时候请抽取：
private boolean checkParam(DTO dto) {...}
```

### 2.2 日志规约

- 应用中不可直接使用日志系统（Log4j、Logback）中的 API，而应依赖使用日志框架 SLF4J 中的 API，使用门面模式的日志框架，有利于维护和各个类的日志处理方式统一。

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(Abc.class);
```

- 日志文件至少保存 15 天，因为有些异常具备以“周”为频次发生的特点。

- 应用中的扩展日志（如打点、临时监控、访问日志等）命名方式：appName_logType_logName.log。 logType:日志类型，如 stats/monitor/access 等；logName:日志描述。这种命名的好处： 通过文件名就可知道日志文件属于什么应用，什么类型，什么目的，也有利于归类查找。  
  正例：mppserver 应用中单独监控时区转换异常，如：mppserver_monitor_timeZoneConvert.log  
  说明：推荐对日志进行分类，如将错误日志和业务日志分开存放，便于开发人员查看，也便于通过日志对系统进行及时监控。

- 对 trace/debug/info 级别的日志输出，必须使用条件输出形式或者使用占位符的方式。  
  说明：logger.debug("Processing trade with id: " + id + " and symbol: " + symbol); 如果日志级别是 warn，上述日志不会打印，但是会执行字符串拼接操作，如果 symbol 是对象， 会执行 toString()方法，浪费了系统资源，执行了上述操作，最终日志却没有打印。

```java
// 正例：
//（条件）建议采用如下方式
if (logger.isDebugEnabled()) {
    logger.debug("Processing trade with id: " + id + " and symbol: " + symbol);
}
// 正例：（占位符）
logger.debug("Processing trade with id: {} and symbol : {} ", id, symbol);
```

- 避免重复打印日志，浪费磁盘空间，务必在 log4j.xml 中设置 additivity=false。

```xml
<!-- 正例： -->
<logger name="com.taobao.dubbo.config" additivity="false">
```

- 异常信息应该包括两类信息：案发现场信息和异常堆栈信息。如果不处理，那么通过关键字 throws 往上抛出。  
  正例：logger.error(各类参数或者对象 toString() + "\_" + e.getMessage(), e);

- 谨慎地记录日志。生产环境禁止输出 debug 日志；有选择地输出 info 日志；如果使用 warn 来记录刚上线时的业务行为信息，一定要注意日志输出量的问题，避免把服务器磁盘撑爆，并记得及时删除这些观察日志。  
  说明：大量地输出无效日志，不利于系统性能提升，也不利于快速定位错误点。记录日志时请思考：这些日志真的有人看吗？看到这条日志你能做什么？能不能给问题排查带来好处？

- 可以使用 warn 日志级别来记录用户输入参数错误的情况，避免用户投诉时，无所适从。如非必要，请不要在此场景打出 error 级别，避免频繁报警。  
  说明：注意日志输出的级别，error 级别只记录系统逻辑出错、异常或者重要的错误信息。

- 尽量用英文来描述日志错误信息，如果日志中的错误信息用英文描述不清楚的话 使用中文描述即可，否则容易产生歧义。国际化团队或海外部署的服务器由于字符集问题，【强制】 使用全英文来注释和描述日志错误信息。

## 第三章 数据库规范

我们 to B 的业务主要使用的是 Oracle 和 SQL server，去年参与适配了国产的华为 GaussDB 及达梦 DM 数据库。

### 3.1 建表规约

- 临时库、表名必须以 tmp 为前缀，如果是按照日期生成的，以日期为后缀

- 备份库、表必须以 bak 为前缀，如果是按照日期生成的，以日期为后缀

- 表达是与否概念的字段，必须使用 is_xxx 的方式命名，数据类型是 unsigned tinyint （1 表示是，0 表示否）。  
  说明：任何字段如果为非负数，必须是 unsigned。  
  注意：POJO 类中的任何布尔类型的变量，都不要加 is 前缀，所以，需要在&lt;resultMap&gt;设置从 is_xxx 到 Xxx 的映射关系。数据库表示是与否的值，使用 tinyint 类型，坚持 is_xxx 的命名方式是为了明确其取值含义与取值范围。 正例：表达逻辑删除的字段名 is_deleted，1 表示删除，0 表示未删除。  
  tips：我们使用的是 dr 字段代表逻辑删除，且 POJO 和布尔字段也未使用上述规范，这也与我们的 JDBC 框架有关，我们的 JDBC 框架是自己设计的，与 mybatis 等主流框架有很大不同。

- 表名、字段名必须使用小写字母或数字，禁止出现数字开头，禁止两个下划线中间只出现数字。数据库字段名的修改代价很大，因为无法进行预发布，所以字段名称需要慎重考虑。  
  说明：**MySQL 在 Windows 下不区分大小写，但在 Linux 下默认是区分大小写。因此，数据库名、表名、字段名，都不允许出现任何大写字母，避免节外生枝。**  
  正例：aliyun_admin，rdc_config，level3_name  
  反例：AliyunAdmin，rdcConfig，level_3_name

- 表名不使用复数名词。  
  说明：表名应该仅仅表示表里面的实体内容，不应该表示实体数量，对应于 DO 类名也是单数形式，符合表达习惯。

- 禁用保留字，如 desc、range、match、delayed 等，请参考 MySQL 官方保留字。

- 主键索引名为 pk*字段名；唯一索引名为 uk*字段名；普通索引名则为 idx*字段名。  
  说明：pk* 即 primary key；uk* 即 unique key；idx* 即 index 的简称。

- 小数类型为 decimal，禁止使用 float 和 double。  
  说明：float 和 double 在存储的时候，存在精度损失的问题，很可能在值的比较时，得到错误的结果。如果存储的数据范围超过 decimal 的范围，建议将数据拆成整数和小数分开存储。

- 如果存储的字符串长度几乎相等，使用 char 定长字符串类型。  
  tips：公司这一点倒是做的比较规范。

- varchar 是不定长字符串，不预先分配存储空间，长度不要超过 5000，如果存储长度大于此值，定义字段类型为 text，独立出来一张表，用主键来对应，避免影响其它字段索引效率。  
  tips：Oracle 的 varchar 最大长度为 4000，SQL server 8000，这是之前适配数据库时踩过的坑。

- 表必备三字段：id, gmt_create, gmt_modified。  
  说明：其中 id 必为主键，类型为 bigint unsigned、单表时自增、步长为 1。gmt_create、gmt_modified 的类型均为 datetime 类型，前者现在时表示主动创建，后者过去分词表示被动更新。

- **表的命名最好是加上“业务名称\_表的作用”**。 正例：alipay_task / force_project / trade_config

- 库名与应用名称尽量一致。

- 如果修改字段含义或对字段表示的状态追加时，需要及时更新字段注释。

- 字段允许适当冗余，以提高查询性能，但必须考虑数据一致。冗余字段应遵循：  
  1）不是频繁修改的字段。  
  2）不是 varchar 超长字段，更不能是 text 字段。  
  正例：商品类目名称使用频率高，字段长度短，名称基本一成不变，可在相关联的表中冗余存储类目名称，避免关联查询。

- 单表行数超过 500 万行或者单表容量超过 2GB，才推荐进行分库分表。  
  说明：如果预计三年后的数据量根本达不到这个级别，请不要在创建表时就分库分表。

- 合适的字符存储长度，不但节约数据库表空间、节约索引存储，更重要的是提升检索速度。  
  正例：如下表，其中无符号值可以避免误存负数，且扩大了表示范围。

### 3.2 SQL 语句

- 不要使用 count(列名)或 count(常量)来替代 count(_)，count(_)是 SQL92 定义的 标准统计行数的语法，跟数据库无关，跟 NULL 和非 NULL 无关。  
  说明：**count(\*)会统计值为 NULL 的行，而 count(列名)不会统计此列为 NULL 值的行**。

- **count(distinct col) 计算该列除 NULL 之外的不重复行数，注意 count(distinct col1, col2) 如果其中一列全为 NULL，那么即使另一列有不同的值，也返回为 0**。

- 当某一列的值全是 NULL 时，count(col)的返回结果为 0，但 sum(col)的返回结果为 NULL，因此使用 sum()时需注意 NPE 问题。  
  正例：可以使用如下方式来避免 sum 的 NPE 问题：SELECT IF(ISNULL(SUM(g)),0,SUM(g)) FROM table;

- 使用 ISNULL()来判断是否为 NULL 值。  
  说明：NULL 与任何值的直接比较都为 NULL。  
  1） NULL<>NULL 的返回结果是 NULL，而不是 false。  
  2） NULL=NULL 的返回结果是 NULL，而不是 true。  
  3） NULL<>1 的返回结果是 NULL，而不是 true。

- 在代码中写分页查询逻辑时，若 count 为 0 应直接返回，避免执行后面的分页语句。

- 不得使用外键与级联，一切外键概念必须在应用层解决。  
  说明： 以学生和成绩的关系为例，学生表中的 student_id 是主键，那么成绩表中的 student_id 则为外键。如果更新学生表中的 student_id，同时触发成绩表中的 student_id 更新，即为级联更新。外键与级联更新适用于单机低并发，不适合分布式、高并发集群；级联更新是强阻塞，存在数据库更新风暴的风险；外键影响数据库的插入速度。

- 禁止使用存储过程，存储过程难以调试和扩展，更没有移植性。

- 数据订正（特别是删除、修改记录操作）时，要先 select，避免出现误删除，确认无误才能执行更新语句。

- in 操作能避免则避免，若实在避免不了，需要仔细评估 in 后边的集合元素数量，控 制在 1000 个之内。

- 如果有国际化需要，所有的字符存储与表示，均以 utf-8 编码，注意字符统计函数 的区别。  
  说明：  
  SELECT LENGTH("轻松工作")； 返回为 12  
  SELECT CHARACTER_LENGTH("轻松工作")； 返回为 4  
  如果需要存储表情，那么选择 utf8mb4 来进行存储，注意它与 utf-8 编码的区别。

- TRUNCATE TABLE 比 DELETE 速度快，且使用的系统和事务日志资源少，但 TRUNCATE 无事务且不触发 trigger，有可能造成事故，故不建议在开发代码中使用此语句。  
  说明：TRUNCATE TABLE 在功能上与不带 WHERE 子句的 DELETE 语句相同。

### 3.3 ORM 映射

我们的 JDBC 框架是自己研发的，之前也有看过 Mybatis 的源码，两者的设计及使用还是差别挺大的。

- 在表查询中，一律不要使用 \* 作为查询的字段列表，需要哪些字段必须明确写明。  
  说明：  
  1）增加查询分析器解析成本。  
  2）增减字段容易与 resultMap 配置不一致。  
  3）无用字 段增加网络消耗，尤其是 text 类型的字段。

- POJO 类的布尔属性不能加 is，而数据库字段必须加 is\_，要求在 resultMap 中进行字段与属性之间的映射。  
  说明：参见定义 POJO 类以及数据库字段定义规定，在&lt;resultMap&gt;中增加映射，是必须的。 在 MyBatis Generator 生成的代码中，需要进行对应的修改。

- 不要用 resultClass 当返回参数，即使所有类属性名与数据库字段一一对应，也需要定义；反过来，每一个表也必然有一个 POJO 类与之对应。  
  说明：配置映射关系，使字段与 DO 类解耦，方便维护。

- sql.xml 配置参数使用：#{}， #param# 不要使用\${} 此种方式容易出现 SQL 注入。

- iBATIS 自带的 queryForList(String statementName,int start,int size)不推 荐使用。  
  说明：其实现方式是在数据库取到 statementName 对应的 SQL 语句的所有记录，再通过 subList 取 start,size 的子集合。

```java
// 正例：
Map<String, Object> map = new HashMap<>();
map.put("start", start);
map.put("size", size);
```

- 不允许直接拿 HashMap 与 Hashtable 作为查询结果集的输出。  
  说明：resultClass=”Hashtable”，会置入字段名和属性值，但是值的类型不可控。

- 更新数据表记录时，必须同时更新记录对应的 gmt_modified 字段值为当前时间。

- 不要写一个大而全的数据更新接口。传入为 POJO 类，不管是不是自己的目标更新字段，都进行 update table set c1=value1,c2=value2,c3=value3; 这是不对的。执行 SQL 时，不要更新无改动的字段，一是易出错；二是效率低；三是增加 binlog 存储。

- @Transactional 事务不要滥用。事务会影响数据库的 QPS，另外使用事务的地方需要考虑各方面的回滚方案，包括缓存回滚、搜索引擎回滚、消息补偿、统计修正等。

- &lt;isEqual&gt;中的 compareValue 是与属性值对比的常量，一般是数字，表示相等时带上此条件；&lt;isNotEmpty&gt;表示不为空且不为 null 时执行；&lt;isNotNull&gt;表示不为 null 值时 执行。

### 3.4 索引规范

- 业务上具有唯一特性的字段，即使是多个字段的组合，也必须建成唯一索引。  
  说明：不要以为唯一索引影响了 insert 速度，这个速度损耗可以忽略，但提高查找速度是明显的；另外，即使在应用层做了非常完善的校验控制，只要没有唯一索引，根据墨菲定律（只要有这个可能性 就一定会发生），必然有脏数据产生。

- 超过三个表禁止 join。需要 join 的字段，数据类型必须绝对一致；多表关联查询时，保证被关联的字段需要有索引。  
  说明：即使双表 join 也要注意表索引、SQL 性能。

- 在 varchar 字段上建立索引时，必须指定索引长度，没必要对全字段建立索引，根据实际文本区分度决定索引长度即可。  
  说明：索引的长度与区分度是一对矛盾体，一般对字符串类型数据，长度为 20 的索引，区分度会高达 90%以上，可以使用 count(distinct left(列名, 索引长度))/count(\*)的区分度 来确定。

- 页面搜索严禁左模糊或者全模糊，如果需要请走搜索引擎来解决。  
  说明：索引文件具有 B-Tree 的最左前缀匹配特性，如果左边的值未确定，那么无法使用此索引。

- 如果有 order by 的场景，请注意利用索引的有序性。order by 最后的字段是组合索引的一部分，并且放在索引组合顺序的最后，避免出现 file_sort 的情况，影响查询性能。  
  正例：where a=? and b=? order by c; 索引：a_b_c  
  反例：索引中有范围查找，那么索引有序性无法利用，如：WHERE a>10 ORDER BY b; 索引 a_b 无法排序。

- 利用覆盖索引来进行查询操作，避免回表。  
  说明：如果一本书需要知道第 11 章是什么标题，会翻开第 11 章对应的那一页吗？目录浏览一下就好，这个目录就是起到覆盖索引的作用。  
  正例：能够建立索引的种类分为主键索引、唯一索引、普通索引三种，而覆盖索引只是一种查询的一种效果，用 explain 的结果，extra 列会出现：using index。

- 利用延迟关联或者子查询优化超多分页场景。  
  说明：MySQL 并不是跳过 offset 行，而是取 offset+N 行，然后返回放弃前 offset 行，返回 N 行，那当 offset 特别大的时候，效率就非常的低下，要么控制返回的总页数，要么对超过特定阈值的页数进行 SQL 改写。  
  正例：先快速定位需要获取的 id 段，然后再关联：  
  SELECT a.\* FROM 表 1 a, ( select id from 表 1 where 条件 LIMIT 100000,20 ) b where a.id=b.id

- SQL 性能优化的目标：至少要达到 range 级别，要求是 ref 级别，如果可以是 consts 最好。  
  说明：  
  1）consts 单表中最多只有一个匹配行（主键或者唯一索引），在优化阶段即可读取到数据。  
  2）ref 指的是使用普通的索引（normal index）。  
  3）range 对索引进行范围检索。  
  反例：explain 表的结果，type=index，索引物理文件全扫描，速度非常慢，这个 index 级别比较 range 还低，与全表扫描是小巫见大巫。

- 建组合索引的时候，区分度最高的在最左边。  
  正例：如果 where a=? and b=? ，如果 a 列的几乎接近于唯一值，那么只需要单建 idx_a 索引即可。  
  说明：存在非等号和等号混合时，在建索引时，请把等号条件的列前置。如：where c>? and d=? 那么即使 c 的区分度更高，也必须把 d 放在索引的最前列，即索引 idx_d_c。

- 防止因字段类型不同造成的隐式转换，导致索引失效。

- 创建索引时避免有如下极端误解：  
  1）宁滥勿缺。认为一个查询就需要建一个索引。  
  2）宁缺勿滥。认为索引会消耗空间、严重拖慢更新和新增速度。  
  3）抵制惟一索引。认为业务的惟一性一律需要在应用层通过“先查后插”方式解决。

## 安全规约

- 隶属于用户个人的页面或者功能必须进行权限控制校验。  
  说明：防止没有做水平权限校验就可随意访问、修改、删除别人的数据，比如查看他人的私信 内容、修改他人的订单。

- 用户敏感数据禁止直接展示，必须对展示数据进行脱敏。  
  说明：中国大陆个人手机号码显示为:158\*\*\*\*9119，隐藏中间 4 位，防止隐私泄露。

- 用户输入的 SQL 参数严格使用参数绑定或者 METADATA 字段值限定，防止 SQL 注入， 禁止字符串拼接 SQL 访问数据库。

- 用户请求传入的任何参数必须做有效性验证。  
  说明：忽略参数校验可能导致：  
  1）page size 过大导致内存溢出；  
  2）恶意 order by 导致数据库慢查询；  
  3）任意重定向；  
  4）SQL 注入；  
  5）反序列化注入；  
  6）正则输入源串拒绝服务 ReDoS；  
  说明：Java 代码用正则来验证客户端的输入，有些正则写法验证普通用户输入没有问题，但是如果攻击人员使用的是特殊构造的字符串来验证，有可能导致死循环的结果。

- 禁止向 HTML 页面输出未经安全过滤或未正确转义的用户数据。

- 表单、AJAX 提交必须执行 CSRF 安全验证。  
  说明：CSRF(Cross-site request forgery)跨站请求伪造是一类常见编程漏洞。对于存在 CSRF 漏洞的应用/网站，攻击者可以事先构造好 URL，只要受害者用户一访问，后台便在用户不知情的情况下对数据库中用户参数进行相应修改。

- 在使用平台资源，譬如短信、邮件、电话、下单、支付，必须实现正确的防重放的机制，如数量限制、疲劳度控制、验证码校验，避免被滥刷而导致资损。  
  说明：如注册时发送验证码到手机，如果没有限制次数和频率，那么可以利用此功能骚扰到其它用户，并造成短信平台资源浪费。

- 发贴、评论、发送即时消息等用户生成内容的场景必须实现防刷、文本内容违禁词过滤等风控策略。

# 个人经验

作为一名初入职场的开发者，最开始是在逛 B 站刷视频时看到的一个 Spring 源码阅读解析，当时作为一个只知道 SSH 和 CRUD 的 boy，看完后心里就两个词儿“卧槽！牛 B 啊！”而且在去年秋招面试阿里时几乎每次都会被面试官问道“有阅读过什么开源框架吗？”每次我都只能一脸便秘的“嗯…，呃…，啊…，木得…”。这在我心里埋下了一个想法，硬着头皮也要把 Spring 框架源码读一遍，再不济也要看看猪是怎么跑的。

从 7 月份开始到现在，利用业余时间完成了 Spring 核心实现（IoC、DI、AOP）及重要组件实现（MVC、事务、JDBC）的源码阅读，并输出相关博客 7 篇，在 Spring 源码上做的详细注解也维护到了个人 GitHub 上，并且将其整合到了开源学习社区 Doocs 上。

学习方法的话，我个人比较喜欢先在 B 站上看相关视频，知道怎么读，从哪下口。然后自己买了本 计文柯老师的《Spring 技术内幕》，比对着从 Spring 官网下载的源码包潜心研读。第一遍读，什么都不懂，按图索骥，迷迷糊糊的读完了；第二遍读，就轻车熟路一些咯，“卧槽！原来如此！”的感叹声也络绎不绝；第三遍就能够在整体代码设计和细节实现两个不同的层次上去吸收 Spring 框架的优点咯。

这三个月来，阅读 Spring 源码给我带来的提升，主要在专业技能上，但同时也辐射到了我的工作、学习、社交等方面。所以，写这篇文章一方面是应“码农翻身”专栏——刘欣老师的建议，做个经验谈，另一方面也是对自己这三个月学习成果的总结。

下面我将分三个部分，谈一谈自己的经验。

### 一、工作方面（编码规范、编码能力、设计模式、英文阅读）

我所从事的行业做的是 toB 的业务，产品底层平台的框架，代码累累，堆积成山，很多框架都是零几年写的，有的甚至比 Spring 还早。且最近国产化、中台、云服务等概念都在不断落地中，有框架源码的阅读经验，让我能够更从容地面对公司研发的新框架，所维护的产品适配华为高斯数据库时，也更清楚可能是 JDBC 框架中哪里做了特殊处理所导致的问题。当然，最主要的还是对个人编码规范的养成，设计模式的理解应用，英文阅读的能力提升。

作为一个初入职场的开发者，编码规范是一个很重要的点，能够让你写出的代码易于维护、阅读和理解。比如，Spring 框架虽然类图体系复杂丰富，但对于类、方法、参数等的命名非常规范；注释注解也非常严谨，注重格式，不会偷懒；对于异常和日志的处理也具有很好的参考价值。比如，之前产品中有遇到一个“将业务表单中的小数从科学计数法转换成普通计数法”（数值过大的 Double 类型数字默认会以科学记数法显示，这是用户无法接受的），研读了复杂的业务代码之后，发现填充到表单前的数据都是 Object 类型的，且丢失了原本类型，无法通过 instanceof 判断应该转成 String 还是 Double，这让我和我的师傅都有点头疼，但 Spring 源码中有过一段以异常捕获机制处理逻辑代码的片段让我灵光乍现，于是我直接将 Object 强转成 Double 并使其不做科学记数法的处理，并将这段代码 try 住，如果没抛异常，就转换成了 Double，抛了异常，就在 catch 中强转成 String。

另外，部门也经常会做代码评审，规范的编码不但能够获得同事的认可，一点一滴的细节也会使你的 leader 对你刮目相看。

从 IoC 的各顶层接口到中间一层一层的抽象类，再到最后的实现类，这一整套体系的设计和实现，对自己在日常工作中设计某些功能的接口、抽象类和具体实现，都带来了很有价值的参考，设计模式和巧妙的编码技巧也渐渐变得触手可及。比如，设计一个 VO 字段校验功能时，会先定义一个顶层接口，抽象出公共方法，抽象类中有做必输项字段非空校验的，在其中利用模板方法模式对公共功能做具体实现，特性化功能写成抽象方法交由各子类具体实现即可。

Spring 上很多接口和抽象类，其注解甚至比代码还多，我也经常尝试着去阅读理解这些注释，看看自己的理解与书上的差异，用这种方式来提升英文技术文档的阅读能力，往往更实在一些。

### 二、学习方面（学习模式的构建、学以致用）

虽然是做技术的，但我也是一个很爱出去耍的人。构建好自己的学习模式能够让你更从容地面对工作和生活。不加班的情况下（所幸部门加班并不太多），我一般会在晚饭之后以及周日时间充电。不管是学技术还是其它什么东西，我认为 以视频为入口，以业界公认的名书继续深入理解，以社交圈的同行或网上社区为输出交流管道，最后持久化到思维导图及学习文档中。Spring 源码学习是我工作之后对自己学习模式构建的一个尝试，构建起这种学习模式之后，个人的工作和生活也变得更加协调平衡，不至于在繁杂忙碌的工作中渐渐丧失学习能力。另外一个比较重要的就是，看 Spring 源码时经常能看到一些与公司框架有异曲同工之妙的编码技巧及实现，比如异常的批量抛出，ConcurrentHashMap 初始化其容量，ThreadLocal 的使用等等，这些都是在读 Spring 源码之前很少会注意或使用的。

### 三、社交方面（GitHub、事业部内部授课）

对于我来说，既然辛辛苦苦搞懂了一个技术，那就一定得输出自己的理解和经验，装波逼，不然辛辛苦苦几个月，什么产出都没有，过一段时间又把学得给忘了，这和被白嫖有什么区别。而输出知识的话当然要选一些比较优质的平台，比如 GayHub，Doocs 组织和其创建者就是我在 GitHub 上认识的，这些大佬之所以牛逼，能够成事，必然有其原因，加入他们的组织跟着混，准能学到更多我想要的东西（不仅仅是技术方面）。

另外，我所在的事业部也有一个“王者荣耀”的学习进阶活动，将自己的学习成果整理成简单、易于理解的内部授课也更容易获得同事的认可与信赖。

### 个人建议

对于初级开发者学习 Spring 源码来说，我建议配合阿里的《Java 开发手册》一起看，因为编码能力和框架设计能力是需要很长时间的经验积累才能得到大幅提升的，而编码规范则是我们最开始就能做到并做好的事情，也是很多成熟公司越来越重视的东西。另外，阿里的《Java 开发手册》中不少规范都是参考了 Spring 框架的，这也从侧面体现了 Spring 作为业界知名框架，其编码的规范性是深受认可的。

