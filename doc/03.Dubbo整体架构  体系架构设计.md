# Dubbo整体架构  体系架构设计

## 项目结构

首先从 GitHub 上 clone 下来 Dubbo 项目，我们根据其中各子项目的项目名，也能大概猜出来各个模块的作用。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/dubbo%E9%A1%B9%E7%9B%AE%E7%BB%93%E6%9E%84-be2632.png)

### dubbo-common

公共逻辑子项目，定义了各子项目中 通用的 组件 和 工具类，如：IO、日志、配置处理等。

### dubbo-rpc

分布式协调服务框架的核心，该模块定义了 RPC 相关的组件，包括 服务发布、服务调用代理、远程调用结果、RPC 调用网络协议，RPC 调用监听器和过滤器等等。该模块提供了默认的 基于 dubbo 协议的实现，还提供了 hessian、http、rmi、及 webservice 等协议的实现，能够满足绝大多数项目的使用需求，另外 还提供了对自定义协议的扩展。

### dubbo-registry

注册中心子项目，它是 RPC 中 consumer 服务消费者 和 provider 服务提供者 两个重要角色的协调者，该子项目定义了核心的 注册中心组件，提供了 mutilcast、redis 和 zookeeper 等多种方式的注册中心实现，用于不同的使用场景。当然，几乎所有的项目都会选择基于 zookeeper 的实现。

### dubbo-remoting

远程通讯子项目，RPC 的实现基础就是远程通讯，consmer 要调用 provider 的远程方法必须通过 远程通讯实现。该模块定义了远程传输器、endpoint 终端、客户端、服务端、编码解码器、数据交换、缓冲区、通讯异常定义 等核心组件。他是对于远程网络通讯的抽象，提供了诸如 netty、mina、http 等 协议和技术框架的实现方式。

### dubbo-monitor

监控子项目，该模块可以监控服务调用的各种信息，例如调用耗时、调用量、调用结果等等，监控中心在调用过程中收集调用的信息，发送到监控服务，在监控服务中可以存储这些信息，对这些数据进行统计分析 和 展示。dubbo 默认提供了一个实现，该实现非常简单，只是作为默认的实现范例，生产环境使用价值不高，往往需要自行实现。

### dubbo-container

容器子项目，是一个独立的容器，以简单的 Main(类) 加载 Spring 启动，因为服务通常不需要 Tomcat/JBoss 等 Web 容器的特性，没必要用 Web 容器去加载服务。

### dubbo-config

配置中心子项目，该模块通过 配置信息，将 dubbo 组件的各个模块整合在一起，给 框架的使用者 提供 可配置的、易用的 分布式服务框架。它定义了面向 dubbo 使用者的各种信息配置，比如服务发布配置、方法发布配置、服务消费配置、应用程序配置、注册中心配置、协议配置、监控配置等等。

### dubbo-cluster

集群子项目，将多个服务提供方伪装为一个提供方，包括：负载均衡、容错、路由等，集群的地址列表可以是静态配置的，也可以是由注册中心下发。

### dubbo-admin

该子项目是一个 web 应用，可以独立部署，用于管理 dubbo 服务，该管理应用可以连接注册中心，读取和更新 注册中心中的内容。

## 实现原理

### 角色类型与运行原理

一个 Dubbo 项目 的角色主要分为如下五种。

- Provider：服务提供方；
- Consumer：服务消费方；
- Registry：服务注册与发现的注册中心；
- Container：服务运行容器；
- Monitor：统计服务的调用次数、调用时间 的监控中心。

其运行原理如下图所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/Dubbo%E5%B7%A5%E4%BD%9C%E5%8E%9F%E7%90%86%E5%9B%BE-5f8e5a.png)

### 工作原理

最后总结下其工作原理。

1. 服务导出：服务提供方 导出服务，监听服务端口；
2. 服务注册：服务提供方 注册服务信息到注册中心；
3. 服务订阅：服务消费方 订阅关注的服务；
4. 服务发现：当服务地址发生变更时，注册中心通知服务消费端；
5. 远程服务调用 ：根据负载均衡策略 选择服务地址，直接调用；
6. 监控：监控器 收集和展示 服务提供方、服务消费方之间 的服务调用统计信息 。

# 集群

## Dubbo集群模块简析

### 集群模块简介

集群，是指同一个服务 被部署在了多个服务器上，每个服务器的任务都相同，能够以较高的性价比，提升系统的 性能、可靠性、灵活性，但同时也要面对 集群中会出现的 负载均衡、容错等问题。dubbo 的集群模块，主要涉及以下几部分内容。

- 负载均衡策略：dubbo 支持的所有负载均衡策略算法；
- 集群容错：Cluster 将 Directory 中的多个 Invoker 伪装成一个 Invoker，对上层透明，伪装过程包含了容错逻辑，调用失败后，重试另一个；
- 路由：dubbo 路由规则，路由规则决定了一次 dubbo 服务调用的目标服务器，路由规则分两种：条件路由规则和脚本路由规则，并且支持可拓展；
- 配置：根据 url 上的配置规则生成配置信息；
- 分组聚合：合并返回结果；
- 本地伪装：mock 通常用于服务降级，mock 只在非业务异常时执行，如 超时、网络异常等。

集群工作过程可分为两个阶段，第一个阶段是在消费者初始化期间，集群 Cluster 为消费者创建 ClusterInvoker 实例。第二个阶段是在消费者进行 RPC 时，以 FailoverClusterInvoker 为例，该实例首先会调用 Directory 的 list()方法 获取 Invoker 列表，然后根据配置的 负载均衡策略，从 Invoker 列表 中选择一个 Inovker，最后将参数传给选择出的 Invoker 实例 进行真正的远程调用。

可将上文中出现的 Invoker 简单理解为服务提供者，Directory 的用途是保存 Invoker 列表，实现类 RegistryDirectory 是一个动态服务目录，可感知注册中心配置的变化，它所持有的 Inovker 列表会随着注册中心内容的变化而变化。每次变化后，RegistryDirectory 会动态增删 Inovker，并调用 Router 的 route 方法进行路由，过滤掉不符合路由规则的 Invoker。

下面我们来看一下 集群模块的项目结构图，结合上文的描述，可以对其有更加深刻的理解。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/dubbo-cluster%E6%A8%A1%E5%9D%97%E5%B7%A5%E7%A8%8B%E7%BB%93%E6%9E%84-ea8f96.png)

### 集群模块核心 API 源码解析

从上图应该也能看出其核心 API 在哪个包里。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/com.alibaba.dubbo.rpc.cluster%E5%8C%85%E7%9B%AE%E5%BD%95-7eff2a.png)

各核心接口的源码如下。

```java
/**
 * 集群接口
 */
@SPI(FailoverCluster.NAME)
public interface Cluster {

    /**
     * 将目录调用程序合并到虚拟调用程序。
     * 基于 Directory ，创建 Invoker 对象，实现统一、透明的 Invoker 调用过程
     *
     * @param directory Directory 对象
     * @param <T>  泛型
     * @return cluster invoker
     * @throws RpcException
     */
    @Adaptive
    <T> Invoker<T> join(Directory<T> directory) throws RpcException;
}

/**
 * Configurator 接口
 */
public interface Configurator extends Comparable<Configurator> {

    /**
     * 配置规则，生成url
     */
    URL getUrl();

    /**
     * 把规则配置到URL中
     */
    URL configure(URL url);

}

@SPI
public interface ConfiguratorFactory {

    /**
     * 获得 Configurator实例
     */
    @Adaptive("protocol")
    Configurator getConfigurator(URL url);

}

public interface Directory<T> extends Node {

    /**
     * 获得服务类型，例如：com.alibaba.dubbo.demo.DemoService
     */
    Class<T> getInterface();

    /**
     * 获得所有服务 Invoker 集合
     */
    List<Invoker<T>> list(Invocation invocation) throws RpcException;
}

@SPI(RandomLoadBalance.NAME)
public interface LoadBalance {

    /**
     * 从 Invoker 集合中，选择一个合适的 Invoker
     */
    @Adaptive("loadbalance")
    <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;
}

@SPI
public interface Merger<T> {

    /**
     * 合并 T 数组，返回合并后的 T 对象
     *
     * @param items T 数组
     * @return T 对象
     */
    T merge(T... items);
}

/**
 * 路由规则接口
 */
public interface Router extends Comparable<Router> {

    /**
     * 获得路由规则的url
     */
    URL getUrl();

    /**
     * 筛选出跟规则匹配的Invoker集合
     *
     * @param invokers   Invoker 集合
     * @param url        refer url
     * @param invocation
     * @return routed invokers 路由后的 Invoker 集合
     * @throws RpcException
     */
    <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;
}

@SPI
public interface RouterFactory {

    /**
     * 创建 Router 对象
     */
    @Adaptive("protocol")
    Router getRouter(URL url);
}
```



## Dubbo 负载均衡简介

负载均衡，无论在常用的中间件 及 框架中，还是现实生活中，都有所体现。比如，一个团队干活，老大肯定要尽可能把任务均匀合理地分下去，让整个团队能高速运转，能力强的多分点，能力弱的少分点，绝对不能去逮着一个人 让他累到死，让其它人闲着。这样的均匀分配任务及压力的思想 放在开发领域 即是“负载均衡”。它就相当于是一个压力均衡机制，通过各种策略，为集群中的每台服务器合理地分配压力，这样 即能提升整个集群的运行效率，又能尽量避免 某个节点因为压力过大而宕机。

在 Dubbo 中也需要负载均衡机制，将消费者的请求 合理分配到服务提供者集群的各个节点上，以提升集群的整体运行效率 和 避免单个节点压力过大而宕机的问题。Dubbo 提供了 4 种负载均衡实现，缺省为 RandomLoadBalance 加权随机调用，如下。

- RandomLoadBalance：加权随机算法，按权重设置随机概率；
- RoundRobinLoadBalance：加权轮询算法，按公约后的权重设置轮询比率；
- LeastActiveLoadBalance：最少响应时间算法，使快速响应的服务提供者 接收更多请求，慢的提供者收到更少请求；
- ConsistentHashLoadBalance：一致性 hash 算法，相同参数的请求总是发到同一提供者。

## 源码赏析

### LoadBalance 接口 和 AbstractLoadBalance

AbstractLoadBalance 实现了 LoadBalance 接口，是负载均衡的抽象类，提供了权重计算等通用功能。

```java
/**
 * LoadBalance. (SPI, Singleton, ThreadSafe)
 *
 * LoadBalance 接口
 */
@SPI(RandomLoadBalance.NAME)
public interface LoadBalance {

    /**
     * 从 Invoker 集合中，选择一个合适的 Invoker
     */
    @Adaptive("loadbalance")
    <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;
}


/**
 * LoadBalance 抽象类，提供了权重计算的功能
 */
public abstract class AbstractLoadBalance implements LoadBalance {

    static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        // 计算权重
        int ww = (int) ((float) uptime / ((float) warmup / (float) weight));
        // 权重范围为 [0, weight] 之间
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }

    @Override
    public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        if (invokers == null || invokers.isEmpty()) {
            return null;
        }
        if (invokers.size() == 1) {
            return invokers.get(0);
        }
        return doSelect(invokers, url, invocation);
    }

    protected abstract <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation);

    protected int getWeight(Invoker<?> invoker, Invocation invocation) {
        // 获得 weight 配置，即服务权重。默认为 100
        int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT);
        if (weight > 0) {
            long timestamp = invoker.getUrl().getParameter(Constants.REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L) {
                // 获得启动总时长
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                // 获得预热需要总时长。默认为 10 * 60 * 1000 = 10 分钟
                int warmup = invoker.getUrl().getParameter(Constants.WARMUP_KEY, Constants.DEFAULT_WARMUP);
                // 处于预热中，计算当前的权重
                if (uptime > 0 && uptime < warmup) {
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight;
    }
}
```

### RandomLoadBalance

该类是基于权重随机算法的负载均衡实现类，我们先来讲讲原理，比如我有有一组服务器 servers = [A, B, C]，他们他们对应的权重为 weights = [6, 3, 1]，权重总和为 10，现在把这些权重值平铺在一维坐标值上，分别出现三个区域，A 区域为[0,6)，B 区域为[6,9)，C 区域为[9,10)，然后产生一个[0, 10)的随机数，看该数字落在哪个区间内，就用哪台服务器，这样权重越大的，被击中的概率就越大。

```java
/**
 * random load balance.
 *
 * 随机，按权重设置随机概率。
 * 在一个截面上碰撞的概率高，但调用量越大分布越均匀，而且按概率使用权重后也比较均匀，有利于动态调整提供者权重。
 */
public class RandomLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "random";

    private final Random random = new Random();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size(); // Number of invokers
        int totalWeight = 0; // The sum of weights
        boolean sameWeight = true; // Every invoker has the same weight?
        // 计算总权限
        for (int i = 0; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation); // 获得权重
            totalWeight += weight; // Sum
            if (sameWeight && i > 0 && weight != getWeight(invokers.get(i - 1), invocation)) {
                sameWeight = false;
            }
        }
        // 权重不相等，随机后，判断在哪个 Invoker 的权重区间中
        if (totalWeight > 0 && !sameWeight) {
            // 随机
            // If (not every invoker has the same weight & at least one invoker's weight>0), select randomly based on totalWeight.
            int offset = random.nextInt(totalWeight);
            // Return a invoker based on the random value.
            // 区间判断
            for (Invoker<T> invoker : invokers) {
                offset -= getWeight(invoker, invocation);
                if (offset < 0) {
                    return invoker;
                }
            }
        }
        // 权重相等，平均随机
        // If all invokers have the same weight value or totalWeight=0, return evenly.
        return invokers.get(random.nextInt(length));
    }
}
```

### RoundRobinLoadBalance

该类是负载均衡基于加权轮询算法的实现，在 nginx 中也有类似的实现。当我们的服务器 性能之间存在明显差异，并希望请求均匀地落到各服务器上，就需要用到加权轮询。

```java
/**
 * Round robin load balance.
 *
 * 轮循，按公约后的权重设置轮循比率。
 * 存在慢的提供者累积请求的问题，比如：第二台机器很慢，但没挂，当请求调到第二台时就卡在那，久而久之，所有请求都卡在调到第二台上。
 */
public class RoundRobinLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "roundrobin";

    /**
     * 服务方法与计数器的映射
     *
     * KEY：serviceKey + "." + methodName
     */
    private final ConcurrentMap<String, AtomicPositiveInteger> sequences = new ConcurrentHashMap<String, AtomicPositiveInteger>();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        int length = invokers.size(); // Number of invokers
        int maxWeight = 0; // The maximum weight
        int minWeight = Integer.MAX_VALUE; // The minimum weight
        final LinkedHashMap<Invoker<T>, IntegerWrapper> invokerToWeightMap = new LinkedHashMap<Invoker<T>, IntegerWrapper>();
        int weightSum = 0;
        // 计算最小、最大权重，总的权重和。
        for (int i = 0; i < length; i++) {
            int weight = getWeight(invokers.get(i), invocation);
            maxWeight = Math.max(maxWeight, weight); // Choose the maximum weight
            minWeight = Math.min(minWeight, weight); // Choose the minimum weight
            if (weight > 0) {
                invokerToWeightMap.put(invokers.get(i), new IntegerWrapper(weight));
                weightSum += weight;
            }
        }
        // 获得 AtomicPositiveInteger 对象
        AtomicPositiveInteger sequence = sequences.get(key);
        if (sequence == null) {
            sequences.putIfAbsent(key, new AtomicPositiveInteger());
            sequence = sequences.get(key);
        }
        // 获得当前顺序号，并递增 + 1
        int currentSequence = sequence.getAndIncrement();
        // 权重不相等，顺序根据权重分配
        if (maxWeight > 0 && minWeight < maxWeight) {
            int mod = currentSequence % weightSum; // 剩余权重
            for (int i = 0; i < maxWeight; i++) { // 循环最大权重
                for (Map.Entry<Invoker<T>, IntegerWrapper> each : invokerToWeightMap.entrySet()) { // 循环 Invoker 集合
                    final Invoker<T> k = each.getKey();
                    final IntegerWrapper v = each.getValue();
                    // 剩余权重归 0 ，当前 Invoker 还有剩余权重，返回该 Invoker 对象
                    if (mod == 0 && v.getValue() > 0) {
                        return k;
                    }
                    // 若 Invoker 还有权重值，扣除它( value )和剩余权重( mod )。
                    if (v.getValue() > 0) {
                        v.decrement();
                        mod--;
                    }
                }
            }
        }
        // 权重相等，平均顺序获得
        // Round robin
        return invokers.get(currentSequence % length);
    }

    private static final class IntegerWrapper {

        /**
         * 权重值
         */
        private int value;

        public IntegerWrapper(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        /**
         * 扣除一
         */
        public void decrement() {
            this.value--;
        }
    }
}
```

### LeastActiveLoadBalance

该负载均衡策略基于最少活跃调用数算法，某个服务活跃调用数越小，表明该服务提供者效率越高，也就表明单位时间内能够处理的请求更多。此时应该选择该类服务器。实现很简单，就是每一个服务都有一个活跃数 active 来记录该服务的活跃值，每收到一个请求，该 active 就会加 1，每完成一个请求，active 就减 1。在服务运行一段时间后，性能好的服务提供者处理请求的速度更快，因此活跃数下降的也越快，此时这样的服务提供者能够优先获取到新的服务请求。除了最小活跃数，还引入了权重值，也就是当活跃数一样的时候，选择利用权重法来进行选择，如果权重也一样，那么随机选择一个。

```java
/**
 * LeastActiveLoadBalance
 *
 * 最少活跃调用数，相同活跃数的随机，活跃数指调用前后计数差。
 * 使慢的提供者收到更少请求，因为越慢的提供者的调用前后计数差会越大。
 */
public class LeastActiveLoadBalance extends AbstractLoadBalance {

    public static final String NAME = "leastactive";

    private final Random random = new Random();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size(); // 总个数
        int leastActive = -1; // 最小的活跃数
        int leastCount = 0; // 相同最小活跃数的个数
        int[] leastIndexes = new int[length]; // 相同最小活跃数的下标
        int totalWeight = 0; // 总权重
        int firstWeight = 0; // 第一个权重，用于于计算是否相同
        boolean sameWeight = true; // 是否所有权重相同
        // 计算获得相同最小活跃数的数组和个数
        for (int i = 0; i < length; i++) {
            Invoker<T> invoker = invokers.get(i);
            int active = RpcStatus.getStatus(invoker.getUrl(), invocation.getMethodName()).getActive(); // 活跃数
            int weight = invoker.getUrl().getMethodParameter(invocation.getMethodName(), Constants.WEIGHT_KEY, Constants.DEFAULT_WEIGHT); // 权重
            if (leastActive == -1 || active < leastActive) { // 发现更小的活跃数，重新开始
                leastActive = active; // 记录最小活跃数
                leastCount = 1; // 重新统计相同最小活跃数的个数
                leastIndexes[0] = i; // 重新记录最小活跃数下标
                totalWeight = weight; // 重新累计总权重
                firstWeight = weight; // 记录第一个权重
                sameWeight = true; // 还原权重相同标识
            } else if (active == leastActive) { // 累计相同最小的活跃数
                leastIndexes[leastCount++] = i; // 累计相同最小活跃数下标
                totalWeight += weight; // 累计总权重
                // 判断所有权重是否一样
                if (sameWeight && weight != firstWeight) {
                    sameWeight = false;
                }
            }
        }
        // assert(leastCount > 0)
        if (leastCount == 1) {
            // 如果只有一个最小则直接返回
            return invokers.get(leastIndexes[0]);
        }
        if (!sameWeight && totalWeight > 0) {
            // 如果权重不相同且权重大于0则按总权重数随机
            int offsetWeight = random.nextInt(totalWeight);
            // 并确定随机值落在哪个片断上
            for (int i = 0; i < leastCount; i++) {
                int leastIndex = leastIndexes[i];
                offsetWeight -= getWeight(invokers.get(leastIndex), invocation);
                if (offsetWeight <= 0) {
                    return invokers.get(leastIndex);
                }
            }
        }
        // 如果权重相同或权重为0则均等随机
        return invokers.get(leastIndexes[random.nextInt(leastCount)]);
    }
}
```

### ConsistentHashLoadBalance

该类是负载均衡基于 hash 一致性算法的实现。一致性哈希算法的工作原理如下。

1.  首先根据 ip 或其他的信息为缓存节点生成一个 hash，在 dubbo 中使用参数进行计算 hash。并将这个 hash 投射到 [0, 232 - 1] 的圆环上，当有查询或写入请求时，则生成一个 hash 值。
2.  然后查找第一个大于或等于该 hash 值的缓存节点，并到这个节点中查询或写入缓存项。如果当前节点挂了，则在下一次查询或写入缓存时，为缓存项查找另一个大于其 hash 值的缓存节点即可。

大致效果如下图所示（引用一下官网的图）。每个缓存节点在圆环上占据一个位置，如果缓存项 key 的 hash 值小于缓存节点 hash 值，则到该缓存节点中存储或读取缓存项，这里有两个概念不要弄混，缓存节点就好比 dubbo 中的服务提供者，会有很多的服务提供者，而缓存项就好比是服务引用的消费者。比如下面绿色点对应的缓存项也就是服务消费者将会被存储到 cache-2 节点中。由于 cache-3 挂了，原本应该存到该节点中的缓存项也就是服务消费者最终会存储到 cache-4 节点中，也就是调用 cache-4 这个服务提供者。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E4%B8%80%E8%87%B4%E6%80%A7hash%E7%AE%97%E6%B3%951-1aad9e.png)

但 hash 一致性算法 并不能够保证 负载的平衡性，就拿上面的例子来看，cache-3 挂掉了，那该节点下的所有缓存项都要存储到 cache-4 节点中，这就导致 hash 值低的一直往高的存储，会面临一个不平衡的现象，见下图：

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E4%B8%80%E8%87%B4%E6%80%A7hash%E7%AE%97%E6%B3%952-6ac45f.png)

可以看到最后会变成类似不平衡的现象，那我们应该怎么避免这样的事情，做到平衡性，那就需要引入 “虚拟节点”，“虚拟节点” 是实际节点在 hash 空间的复制品，“虚拟节点” 在 hash 空间 中以 hash 值 排列，如下图。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/%E4%B8%80%E8%87%B4%E6%80%A7hash%E7%AE%97%E6%B3%953-be8349.png)

可以看到各个节点都被均匀分布在圆环上，且一个服务提供者有多个节点存在，分别跟其他节点交错排列，这样做的目的就是避免数据倾斜问题，也就是由于节点不够分散，导致大量请求落到了同一个节点上，而其他节点只会接收到了少量请求的情况。类似第二张图的情况。

看完原理，接下来我们来看看代码。

```java
/**
 * ConsistentHashLoadBalance
 *
 * 一致性 Hash，相同参数的请求总是发到同一提供者。
 * 当某一台提供者挂时，原本发往该提供者的请求，基于虚拟节点，平摊到其它提供者，不会引起剧烈变动。
 */
public class ConsistentHashLoadBalance extends AbstractLoadBalance {

    /**
     * 服务方法与一致性哈希选择器的映射
     *
     * KEY：serviceKey + "." + methodName
     */
    private final ConcurrentMap<String, ConsistentHashSelector<?>> selectors = new ConcurrentHashMap<String, ConsistentHashSelector<?>>();

    @SuppressWarnings("unchecked")
    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        String key = invokers.get(0).getUrl().getServiceKey() + "." + invocation.getMethodName();
        // 基于 invokers 集合，根据对象内存地址来计算定义哈希值
        int identityHashCode = System.identityHashCode(invokers);
        // 获得 ConsistentHashSelector 对象。若为空，或者定义哈希值变更（说明 invokers 集合发生变化），进行创建新的 ConsistentHashSelector 对象
        ConsistentHashSelector<T> selector = (ConsistentHashSelector<T>) selectors.get(key);
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(key, new ConsistentHashSelector<T>(invokers, invocation.getMethodName(), identityHashCode));
            selector = (ConsistentHashSelector<T>) selectors.get(key);
        }
        return selector.select(invocation);
    }

    private static final class ConsistentHashSelector<T> {

        /**
         * 虚拟节点与 Invoker 的映射关系
         */
        private final TreeMap<Long, Invoker<T>> virtualInvokers;
        /**
         * 每个Invoker 对应的虚拟节点数
         */
        private final int replicaNumber;
        /**
         * 定义哈希值
         */
        private final int identityHashCode;
        /**
         * 取值参数位置数组
         */
        private final int[] argumentIndex;

        ConsistentHashSelector(List<Invoker<T>> invokers, String methodName, int identityHashCode) {
            this.virtualInvokers = new TreeMap<Long, Invoker<T>>();
            // 设置 identityHashCode
            this.identityHashCode = identityHashCode;
            URL url = invokers.get(0).getUrl();
            // 初始化 replicaNumber
            this.replicaNumber = url.getMethodParameter(methodName, "hash.nodes", 160);
            // 初始化 argumentIndex
            String[] index = Constants.COMMA_SPLIT_PATTERN.split(url.getMethodParameter(methodName, "hash.arguments", "0"));
            argumentIndex = new int[index.length];
            for (int i = 0; i < index.length; i++) {
                argumentIndex[i] = Integer.parseInt(index[i]);
            }
            // 初始化 virtualInvokers
            for (Invoker<T> invoker : invokers) {
                String address = invoker.getUrl().getAddress();
                // 每四个虚拟结点为一组，为什么这样？下面会说到
                for (int i = 0; i < replicaNumber / 4; i++) {
                    // 这组虚拟结点得到惟一名称
                    byte[] digest = md5(address + i);
                    // Md5是一个16字节长度的数组，将16字节的数组每四个字节一组，分别对应一个虚拟结点，这就是为什么上面把虚拟结点四个划分一组的原因
                    for (int h = 0; h < 4; h++) {
                        // 对于每四个字节，组成一个long值数值，做为这个虚拟节点的在环中的惟一key
                        long m = hash(digest, h);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        public Invoker<T> select(Invocation invocation) {
            // 基于方法参数，获得 KEY
            String key = toKey(invocation.getArguments());
            // 计算 MD5 值
            byte[] digest = md5(key);
            // 计算 KEY 值
            return selectForKey(hash(digest, 0));
        }

        private String toKey(Object[] args) {
            StringBuilder buf = new StringBuilder();
            for (int i : argumentIndex) {
                if (i >= 0 && i < args.length) {
                    buf.append(args[i]);
                }
            }
            return buf.toString();
        }

        private Invoker<T> selectForKey(long hash) {
            // 得到大于当前 key 的那个子 Map ，然后从中取出第一个 key ，就是大于且离它最近的那个 key
            Map.Entry<Long, Invoker<T>> entry = virtualInvokers.tailMap(hash, true).firstEntry();
            // 不存在，则取 virtualInvokers 第一个
        	if (entry == null) {
        		entry = virtualInvokers.firstEntry();
        	}
        	// 存在，则返回
        	return entry.getValue();
        }

        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        // 计算 MD5
        private byte[] md5(String value) {
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.reset();
            byte[] bytes;
            try {
                bytes = value.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.update(bytes);
            return md5.digest();
        }
    }
}
```

# 注册中心

## 注册中心在 Dubbo 中的作用

服务治理框架可以大致分为 服务通信 和 服务管理 两部分，服务管理可以分为服务注册、服务订阅以及服务发现，服务提供者 Provider 会往注册中心注册服务，而消费者 Consumer 会从注册中心中订阅自己关注的服务，并在关注的服务发生变更时 得到注册中心的通知。Provider、Consumer 以及 Registry 之间的依赖关系 如下图所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/Dubbo%E5%B7%A5%E4%BD%9C%E5%8E%9F%E7%90%86%E5%9B%BE-4ebdb8.png)

## dubbo-registry 模块 结构分析

dubbo 的注册中心有多种实现方案，如：zookeeper、redis、multicast 等，本章先看一下 dubbo-registry 模块的核心部分 dubbo-registry-api，具体实现部分放到下章来讲。dubbo-registry 模块 的结构如下图所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/dubbo-registry%E6%A8%A1%E5%9D%97%E7%BB%93%E6%9E%84%E5%9B%BE-3c8c70.png)

### Registry 核心组件类图

典型的 接口 -> 抽象类 -> 实现类 的结构设计，如下图所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/Registry%E7%BB%84%E4%BB%B6%E7%B1%BB%E5%9B%BE-24a773.png)

既然有 Registry 组件，那么按照很多框架的套路，肯定也有一个用于获取 Registry 实例的 RegistryFactory，其中用到了工厂方法模式，不同的工厂类用于获取不同类型的实例。其类图结构如下。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/RegistryFactory%E7%BB%84%E4%BB%B6%E7%B1%BB%E5%9B%BE-f140b5.png)

## 源码详解

根据上面的类图，我们开始从上往下 详解 dubbo 中对于注册中心的设计以及实现。

### RegistryService 接口

RegistryService 是注册中心模块的服务接口，定义了注册、取消注册、订阅、取消订阅以及查询符合条件的已注册数据 等方法。这里统一说明一下 URL，dubbo 是以总线模式来时刻传递和保存配置信息的，配置信息都被放在 URL 上进行传递，随时可以取得相关配置信息，而这里提到了 URL 有别的作用，就是作为类似于节点的作用，首先服务提供者（Provider）启动时需要提供服务，就会向注册中心写下自己的 URL 地址。然后消费者启动时需要去订阅该服务，则会订阅 Provider 注册的地址，并且消费者也会写下自己的 URL。

```java
/**
 * RegistryService. (SPI, Prototype, ThreadSafe)
 *
 * 注册中心服务接口
 */
public interface RegistryService {

    /**
     * 注册数据，比如：提供者地址，消费者地址，路由规则，覆盖规则 等数据。
     * <p>
     * 注册需处理契约：<br>
     * 1. 当URL设置了check=false时，注册失败后不报错，在后台定时重试，否则抛出异常。<br>
     * 2. 当URL设置了dynamic=false参数，则需持久存储，否则，当注册者出现断电等情况异常退出时，需自动删除。<br>
     * 3. 当URL设置了category=routers时，表示分类存储，缺省类别为providers，可按分类部分通知数据。<br>
     * 4. 当注册中心重启，网络抖动，不能丢失数据，包括断线自动删除数据。<br>
     * 5. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
     *
     * @param url 注册信息，不允许为空，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void register(URL url);

    /**
     * 取消注册.
     * <p>
     * 取消注册需处理契约：<br>
     * 1. 如果是dynamic=false的持久存储数据，找不到注册数据，则抛IllegalStateException，否则忽略。<br>
     * 2. 按全URL匹配取消注册。<br>
     *
     * @param url 注册信息，不允许为空，如：dubbo://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     */
    void unregister(URL url);

    /**
     * 订阅符合条件的已注册数据，当有注册数据变更时自动推送.
     * <p>
     * 订阅需处理契约：<br>
     * 1. 当URL设置了check=false时，订阅失败后不报错，在后台定时重试。<br>
     * 2. 当URL设置了category=routers，只通知指定分类的数据，多个分类用逗号分隔，并允许星号通配，表示订阅所有分类数据。<br>
     * 3. 允许以interface,group,version,classifier作为条件查询，如：interface=com.alibaba.foo.BarService&version=1.0.0<br>
     * 4. 并且查询条件允许星号通配，订阅所有接口的所有分组的所有版本，或：interface=*&group=*&version=*&classifier=*<br>
     * 5. 当注册中心重启，网络抖动，需自动恢复订阅请求。<br>
     * 6. 允许URI相同但参数不同的URL并存，不能覆盖。<br>
     * 7. 必须阻塞订阅过程，等第一次通知完后再返回。<br>
     *
     * @param url      订阅条件，不允许为空，如：consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener 变更事件监听器，不允许为空
     */
    void subscribe(URL url, NotifyListener listener);

    /**
     * 取消订阅.
     * <p>
     * 取消订阅需处理契约：<br>
     * 1. 如果没有订阅，直接忽略。<br>
     * 2. 按全URL匹配取消订阅。<br>
     *
     * @param url      订阅条件，不允许为空，如：consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @param listener 变更事件监听器，不允许为空
     */
    void unsubscribe(URL url, NotifyListener listener);

    /**
     * 查询符合条件的已注册数据，与订阅的推模式相对应，这里为拉模式，只返回一次结果。
     *
     * @param url 查询条件，不允许为空，如：consumer://10.20.153.10/com.alibaba.foo.BarService?version=1.0.0&application=kylin
     * @return 已注册信息列表，可能为空，含义同{@link com.alibaba.dubbo.registry.NotifyListener#notify(List<URL>)}的参数。
     * @see com.alibaba.dubbo.registry.NotifyListener#notify(List)
     */
    List<URL> lookup(URL url);
}
```

### Registry 接口

注册中心接口，把节点 Node 以及注册中心服务 RegistryService 的方法整合在了这个接口里面。该接口并没有自己的方法，就是继承了 Node 和 RegistryService 接口。这里的 Node 是节点的接口，里面协定了关于节点的一些操作方法，源码如下。

```java
/**
 * 注册中心接口
 */
public interface Registry extends Node, RegistryService {
}

public interface Node {
	//获得节点地址
    URL getUrl();
    //判断节点是否可用
    boolean isAvailable();
    //销毁节点
    void destroy();
}
```

### AbstractRegistry 抽象类

实现了 Registry 接口的抽象类。为了减轻注册中心的压力，该抽象类把本地 URL 缓存到了 property 文件中，并且实现了注册中心的注册、订阅等方法。

```java
/**
 * 实现了Registry接口的抽象类，实现了如下方法：
 *
 * 1、通用的注册、订阅、查询、通知等方法
 * 2、读取和持久化注册数据到文件，以 properties 格式存储
 */
public abstract class AbstractRegistry implements Registry {

    // URL地址分隔符，用于文件缓存中，服务提供者URL分隔
    private static final char URL_SEPARATOR = ' ';
    // URL地址分隔正则表达式，用于解析文件缓存中服务提供者URL列表
    private static final String URL_SPLIT = "\\s+";

    // Log output
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     *  本地磁盘缓存。
     *  1. 其中特殊的 key 值 .registies 记录注册中心列表 TODO 8019 芋艿，特殊的 key 是
     *  2. 其它均为 {@link #notified} 服务提供者列表
     */
    private final Properties properties = new Properties();
    /**
     * 注册中心缓存写入执行器。
     * 线程数=1
     */
    // File cache timing writing
    private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveRegistryCache", true));
    /**
     * 是否同步保存文件
     */
    private final boolean syncSaveFile;
    /**
     * 数据版本号
     */
    private final AtomicLong lastCacheChanged = new AtomicLong();
    /**
     * 已注册 URL 集合。
     * 注册的 URL 可以是服务提供者的，也可以是服务消费者的
     */
    private final Set<URL> registered = new ConcurrentHashSet<URL>();
    /**
     * 订阅 URL 的监听器集合
     * key：订阅者的 URL ，例如消费者的 URL
     */
    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();
    /**
     * 被通知的 URL 集合
     * key1：消费者的 URL ，例如消费者的 URL ，和 {@link #subscribed} 的键一致
     * key2：分类，例如：providers、consumers、routes、configurators。【实际无 consumers ，因为消费者不会去订阅另外的消费者的列表】
     *            在 {@link Constants} 中，以 "_CATEGORY" 结尾
     */
    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<URL, Map<String, List<URL>>>();
    /**
     * 注册中心 URL
     */
    private URL registryUrl;
    /**
     * 本地磁盘缓存文件，缓存注册中心的数据
     */
    private File file;
    /**
     * 是否销毁
     */
    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public AbstractRegistry(URL url) {
        setUrl(url);
        // Start file save timer
        syncSaveFile = url.getParameter(Constants.REGISTRY_FILESAVE_SYNC_KEY, false);
        // 获得 `file`
        String filename = url.getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-registry-" + url.getParameter(Constants.APPLICATION_KEY) + "-" + url.getAddress() + ".cache");
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid registry store file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
        }
        this.file = file;
        // 加载本地磁盘缓存文件到内存缓存
        loadProperties();
        // 通知监听器，URL 变化结果
        notify(url.getBackupUrls()); // 【TODO 8020】为什么构造方法，要通知，连监听器都没注册
    }

    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            List<URL> result = new ArrayList<URL>(1);
            result.add(url.setProtocol(Constants.EMPTY_PROTOCOL));
            return result;
        }
        return urls;
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public Set<URL> getRegistered() {
        return registered;
    }

    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return subscribed;
    }

    public Map<URL, Map<String, List<URL>>> getNotified() {
        return notified;
    }

    public File getCacheFile() {
        return file;
    }

    public Properties getCacheProperties() {
        return properties;
    }

    public AtomicLong getLastCacheChanged() {
        return lastCacheChanged;
    }

    /**
     * 保存内存缓存到本地磁盘缓存文件，即 {@link #properties} => {@link #file}
     *
     * @param version 数据版本号
     */
    public void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (file == null) {
            return;
        }
        // Save
        try {
            // 创建 .lock 文件
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            // 随机读写文件操作
            RandomAccessFile raf = new RandomAccessFile(lockfile, "rw");
            try {
                FileChannel channel = raf.getChannel();
                try {
                    // 获得文件锁
                    FileLock lock = channel.tryLock();
                    // 获取失败
                    if (lock == null) {
                        throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.registry.file=xxx.properties");
                    }
                    // 获取成功，进行保存
                    // Save
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        FileOutputStream outputFile = new FileOutputStream(file);
                        try {
                            properties.store(outputFile, "Dubbo Registry Cache");
                        } finally {
                            outputFile.close();
                        }
                    // 释放文件锁
                    } finally {
                        lock.release();
                    }
                // 释放文件 Channel
                } finally {
                    channel.close();
                }
            // 释放随机读写文件操作
            } finally {
                raf.close();
            }
        } catch (Throwable e) {
            // 版本号过小，不保存
            if (version < lastCacheChanged.get()) {
                return;
            // 重新异步保存，一般情况下为上面的获取锁失败抛出的异常。通过这样的方式，达到保存成功。
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn("Failed to save registry store file, cause: " + e.getMessage(), e);
        }
    }

    /**
     * 加载本地磁盘缓存文件到内存缓存，即 {@link #file} => {@link #properties}
     */
    private void loadProperties() {
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                // 文件流
                in = new FileInputStream(file);
                // 读取文件流
                properties.load(in);
                if (logger.isInfoEnabled()) {
                    logger.info("Load registry store file " + file + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load registry store file " + file, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    /**
     * 从 `properties` 中获得缓存的 URL 集合
     *
     * @param url URL
     * @return URL 集合
     */
    public List<URL> getCacheUrls(URL url) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key != null && key.length() > 0 // 非空
                    && key.equals(url.getServiceKey()) // 服务键匹配
                    && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_') // TODO 芋艿，_ 是什么
                    && value != null && value.length() > 0) { // 值非空
                String[] arr = value.trim().split(URL_SPLIT);
                List<URL> urls = new ArrayList<URL>();
                for (String u : arr) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
        }
        return null;
    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> result = new ArrayList<URL>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        // 有数据，遍历数据获得
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            // 遍历
            for (List<URL> urls : notifiedUrls.values()) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        // 无数据，通过发起订阅的方式得到数据后，遍历数据获得
        } else {
            // 创建 NotifyListener 对象
            final AtomicReference<List<URL>> reference = new AtomicReference<List<URL>>();
            NotifyListener listener = new NotifyListener() {
                public void notify(List<URL> urls) {
                    reference.set(urls);
                }
            };
            // 订阅获得数据
            subscribe(url, listener); // Subscribe logic guarantees the first notify to return
            List<URL> urls = reference.get();
            // 遍历
            if (urls != null && !urls.isEmpty()) {
                for (URL u : urls) {
                    if (!Constants.EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Register: " + url);
        }
        // 添加到 registered 集合
        registered.add(url);
    }

    @Override
    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unregister: " + url);
        }
        // 移除出 registered 集合
        registered.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Subscribe: " + url);
        }
        // 添加到 subscribed 集合
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners == null) {
            subscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = subscribed.get(url);
        }
        listeners.add(listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            logger.info("Unsubscribe: " + url);
        }
        // 移除出 subscribed 集合
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * 恢复注册和订阅
     *
     * @throws Exception 发生异常
     */
    protected void recover() throws Exception {
        // register 恢复注册
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        // subscribe 恢复订阅
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }

    /**
     * 通知监听器，URL 变化结果。
     *
     * @param urls 通知的 URL 变化结果（全量数据）
     */
    protected void notify(List<URL> urls) {
        if (urls == null || urls.isEmpty()) return;
        // 循环 `subscribed` ，通知监听器们
        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL url = entry.getKey();
            // 匹配
            if (!UrlUtils.isMatch(url, urls.get(0))) {
                continue;
            }
            // 通知监听器
            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        logger.error("Failed to notify registry event, urls: " + urls + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    /**
     * 通知监听器，URL 变化结果。
     *
     * 数据流向 `urls` => {@link #notified} => {@link #properties} => {@link #file}
     *
     * @param url 消费者 URL
     * @param listener 监听器
     * @param urls 通知的 URL 变化结果（全量数据）
     */
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if ((urls == null || urls.isEmpty())
                && !Constants.ANY_VALUE.equals(url.getServiceInterface())) {
            logger.warn("Ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
        }
        // 将 `urls` 按照 `url.parameter.category` 分类，添加到集合
        // 注意，特殊情况，使用 curator 连接 Zookeeper 时，若是服务消费者，连接断开，会出现 category=providers,configurations,routes
        Map<String, List<URL>> result = new HashMap<String, List<URL>>();
        for (URL u : urls) {
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
                List<URL> categoryList = result.get(category);
                if (categoryList == null) {
                    categoryList = new ArrayList<URL>();
                    result.put(category, categoryList);
                }
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }
        // 获得消费者 URL 对应的在 `notified` 中，通知的 URL 变化结果（全量数据）
        Map<String, List<URL>> categoryNotified = notified.get(url);
        if (categoryNotified == null) {
            notified.putIfAbsent(url, new ConcurrentHashMap<String, List<URL>>());
            categoryNotified = notified.get(url);
        }
        // 【按照分类循环】处理通知的 URL 变化结果（全量数据）
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            // 覆盖到 `notified`
            // 当某个分类的数据为空时，会依然有 urls 。其中 `urls[0].protocol = empty` ，通过这样的方式，处理所有服务提供者为空的情况。
            categoryNotified.put(category, categoryList);
            // 保存到文件
            saveProperties(url);
            // 通知监听器
            listener.notify(categoryList);
        }
    }

    /**
     * 保存单个消费者 URL 对应，在 `notified` 的数据，到文件。
     *
     * @param url 消费者 URL
     */
    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }

        try {
            // 拼接 URL
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            if (categoryNotified != null) {
                for (List<URL> us : categoryNotified.values()) {
                    for (URL u : us) {
                        if (buf.length() > 0) {
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    }
                }
            }
            // 设置到 properties 中
            properties.setProperty(url.getServiceKey(), buf.toString());
            // 增加数据版本号
            long version = lastCacheChanged.incrementAndGet();
            // 保存到文件
            if (syncSaveFile) {
                doSaveProperties(version);
            } else {
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    /**
     * 取消注册和订阅
     */
    @Override
    public void destroy() {
        // 已销毁，跳过
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Destroy registry:" + getUrl());
        }
        // 取消注册
        Set<URL> destroyRegistered = new HashSet<URL>(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (URL url : new HashSet<URL>(getRegistered())) {
                if (url.getParameter(Constants.DYNAMIC_KEY, true)) {
                    try {
                        unregister(url); // 取消注册
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unregister url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        // 取消订阅
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener); // 取消订阅
                        if (logger.isInfoEnabled()) {
                            logger.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    public String toString() {
        return getUrl().toString();
    }

    /**
     * 保存配置的 Runnable任务
     */
    private class SaveProperties implements Runnable {

        /**
         * 数据版本号
         */
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        public void run() {
            doSaveProperties(version);
        }
    }
}
```

### FailbackRegistry 抽象类

FailbackRegistry 抽象类 继承了上面的 AbstractRegistry，AbstractRegistry 中的注册、订阅等方法，实际上就是一些内存缓存的变化，而真正的注册订阅的实现逻辑在 FailbackRegistry 实现，并且 FailbackRegistry 提供了失败重试的机制。

```java
/**
 * 支持失败重试的 FailbackRegistry抽象类
 */
public abstract class FailbackRegistry extends AbstractRegistry {

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService retryExecutor = Executors.
    		newScheduledThreadPool(1, new NamedThreadFactory("DubboRegistryFailedRetryTimer", true));

    /**
     * 失败重试定时器，定时检查是否有请求失败，如有，无限次重试
     */
    private final ScheduledFuture<?> retryFuture;
    /**
     * 注册失败的 URL 集合
     */
    private final Set<URL> failedRegistered = new ConcurrentHashSet<URL>();
    /**
     * 取消注册失败的 URL 集合
     */
    private final Set<URL> failedUnregistered = new ConcurrentHashSet<URL>();
    /**
     * 订阅失败的监听器集合
     */
    private final ConcurrentMap<URL, Set<NotifyListener>> failedSubscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();
    /**
     * 取消订阅失败的监听器集合
     */
    private final ConcurrentMap<URL, Set<NotifyListener>> failedUnsubscribed = new ConcurrentHashMap<URL, Set<NotifyListener>>();
    /**
     * 通知失败的 URL 集合
     */
    private final ConcurrentMap<URL, Map<NotifyListener, List<URL>>> failedNotified = new ConcurrentHashMap<URL, Map<NotifyListener, List<URL>>>();

    /**
     * 是否销毁
     */
    private AtomicBoolean destroyed = new AtomicBoolean(false);

    public FailbackRegistry(URL url) {
        super(url);
        // 重试频率，单位：毫秒
        int retryPeriod = url.getParameter(Constants.REGISTRY_RETRY_PERIOD_KEY, Constants.DEFAULT_REGISTRY_RETRY_PERIOD);
        // 创建失败重试定时器
        this.retryFuture = retryExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // Check and connect to the registry
                try {
                    retry();
                } catch (Throwable t) { // Defensive fault tolerance
                    logger.error("Unexpected error occur at failed retry, cause: " + t.getMessage(), t);
                }
            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    public Future<?> getRetryFuture() {
        return retryFuture;
    }

    public Set<URL> getFailedRegistered() {
        return failedRegistered;
    }

    public Set<URL> getFailedUnregistered() {
        return failedUnregistered;
    }

    public Map<URL, Set<NotifyListener>> getFailedSubscribed() {
        return failedSubscribed;
    }

    public Map<URL, Set<NotifyListener>> getFailedUnsubscribed() {
        return failedUnsubscribed;
    }

    public Map<URL, Map<NotifyListener, List<URL>>> getFailedNotified() {
        return failedNotified;
    }

    /**
     * 添加到 `failedSubscribed`
     */
    private void addFailedSubscribed(URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners == null) {
            failedSubscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = failedSubscribed.get(url);
        }
        listeners.add(listener);
    }

    /**
     * 移除出 `failedSubscribed` `failedUnsubscribed` `failedNotified`
     */
    private void removeFailedSubscribed(URL url, NotifyListener listener) {
        // 移除出 `failedSubscribed`
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        // 移除出 `failedUnsubscribed`
        listeners = failedUnsubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        // 移除出 `failedNotified`
        Map<NotifyListener, List<URL>> notified = failedNotified.get(url);
        if (notified != null) {
            notified.remove(listener);
        }
    }

    @Override
    public void register(URL url) {
        // 已销毁，跳过
        if (destroyed.get()){
            return;
        }
        // 添加到 `registered` 变量
        super.register(url);
        // 移除出 `failedRegistered` `failedUnregistered` 变量
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        // 向注册中心发送注册请求
        try {
            doRegister(url);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol()); // 非消费者。消费者会在 `ReferenceConfig#createProxy(...)` 方法中，调用 `Invoker#avalible()` 方法，进行检查。
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if (skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to register " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to register " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的注册请求记录到 `failedRegistered`，定时重试
            failedRegistered.add(url);
        }
    }

    @Override
    public void unregister(URL url) {
        // 已销毁，跳过
        if (destroyed.get()){
            return;
        }
        // 移除出 `registered` 变量
        super.unregister(url);
        // 移除出 `failedRegistered` `failedUnregistered` 变量
        failedRegistered.remove(url);
        failedUnregistered.remove(url);
        // 向注册中心发送取消注册请求
        try {
            doUnregister(url);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true)
                    && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if (skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unregister " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to uregister " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的取消注册请求记录到 `failedUnregistered`，定时重试
            failedUnregistered.add(url);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        // 已销毁，跳过
        if (destroyed.get()){
            return;
        }
        // 移除出 `subscribed` 变量
        super.subscribe(url, listener);
        // 移除出 `failedSubscribed` `failedUnsubscribed` `failedNotified`
        removeFailedSubscribed(url, listener);
        // 向注册中心发送订阅请求
        try {
            doSubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            // 如果有缓存的 URL 集合，进行通知。后续订阅成功后，会使用最新的 URL 集合，进行通知。
            List<URL> urls = getCacheUrls(url);
            if (urls != null && !urls.isEmpty()) {
                notify(url, listener, urls);
                logger.error("Failed to subscribe " + url + ", Using cached list: " + urls + " from cache file: " + getUrl().getParameter(Constants.FILE_KEY, System.getProperty("user.home") + "/dubbo-registry-" + url.getHost() + ".cache") + ", cause: " + t.getMessage(), t);
            } else {
                // 如果开启了启动时检测，则直接抛出异常
                // If the startup detection is opened, the Exception is thrown directly.
                boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                        && url.getParameter(Constants.CHECK_KEY, true);
                boolean skipFailback = t instanceof SkipFailbackWrapperException;
                if (check || skipFailback) {
                    if (skipFailback) {
                        t = t.getCause();
                    }
                    throw new IllegalStateException("Failed to subscribe " + url + ", cause: " + t.getMessage(), t);
                } else {
                    logger.error("Failed to subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
                }
            }

            // 将失败的订阅请求记录到 `failedSubscribed`，定时重试
            // Record a failed registration request to a failed list, retry regularly
            addFailedSubscribed(url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        // 已销毁，跳过
        if (destroyed.get()){
            return;
        }
        // 移除出 `unsubscribed` 变量
        super.unsubscribe(url, listener);
        // 移除出 `failedSubscribed` `failedUnsubscribed` `failedNotified`
        removeFailedSubscribed(url, listener);
        // 向注册中心发送取消订阅请求
        try {
            // Sending a canceling subscription request to the server side
            doUnsubscribe(url, listener);
        } catch (Exception e) {
            Throwable t = e;

            // 如果开启了启动时检测，则直接抛出异常
            // If the startup detection is opened, the Exception is thrown directly.
            boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                    && url.getParameter(Constants.CHECK_KEY, true);
            boolean skipFailback = t instanceof SkipFailbackWrapperException;
            if (check || skipFailback) {
                if (skipFailback) {
                    t = t.getCause();
                }
                throw new IllegalStateException("Failed to unsubscribe " + url + " to registry " + getUrl().getAddress() + ", cause: " + t.getMessage(), t);
            } else {
                logger.error("Failed to unsubscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
            }

            // 将失败的订阅请求记录到 `failedUnsubscribed`，定时重试
            // Record a failed registration request to a failed list, retry regularly
            Set<NotifyListener> listeners = failedUnsubscribed.get(url);
            if (listeners == null) {
                failedUnsubscribed.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
                listeners = failedUnsubscribed.get(url);
            }
            listeners.add(listener);
        }
    }

    @Override
    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        // 通知监听器
        try {
            doNotify(url, listener, urls);
        } catch (Exception t) {
            // 将失败的通知记录到 `failedNotified`，定时重试
            Map<NotifyListener, List<URL>> listeners = failedNotified.get(url);
            if (listeners == null) {
                failedNotified.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, List<URL>>());
                listeners = failedNotified.get(url);
            }
            listeners.put(listener, urls);
            logger.error("Failed to notify for subscribe " + url + ", waiting for retry, cause: " + t.getMessage(), t);
        }
    }

    protected void doNotify(URL url, NotifyListener listener, List<URL> urls) {
        super.notify(url, listener, urls);
    }

    @Override
    protected void recover() throws Exception {
        // register 恢复注册，添加到 `failedRegistered` ，定时重试
        Set<URL> recoverRegistered = new HashSet<URL>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                failedRegistered.add(url);
            }
        }
        // subscribe 恢复订阅，添加到 `failedSubscribed` ，定时重试
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<URL, Set<NotifyListener>>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    addFailedSubscribed(url, listener);
                }
            }
        }
    }

    /**
     * 重试
     */
    protected void retry() {
        // 重试执行注册
        if (!failedRegistered.isEmpty()) {
            Set<URL> failed = new HashSet<URL>(failedRegistered); // 避免并发冲突
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry register " + failed);
                }
                try {
                    for (URL url : failed) {
                        try {
                            // 执行注册
                            doRegister(url);
                            // 移除出 `failedRegistered`
                            failedRegistered.remove(url);
                        } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                            logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                    logger.warn("Failed to retry register " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        // 重试执行取消注册
        if (!failedUnregistered.isEmpty()) {
            Set<URL> failed = new HashSet<URL>(failedUnregistered); // 避免并发冲突
            if (!failed.isEmpty()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unregister " + failed);
                }
                try {
                    for (URL url : failed) {
                        try {
                            // 执行取消注册
                            doUnregister(url);
                            // 移除出 `failedUnregistered`
                            failedUnregistered.remove(url);
                        } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                            logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                        }
                    }
                } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                    logger.warn("Failed to retry unregister  " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        // 重试执行注册
        if (!failedSubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = new HashMap<URL, Set<NotifyListener>>(failedSubscribed); // 避免并发冲突
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<URL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry subscribe " + failed);
                }
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                // 执行注册
                                doSubscribe(url, listener);
                                // 移除出监听器
                                listeners.remove(listener);
                            } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                                logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                    logger.warn("Failed to retry subscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        // 重试执行取消注册
        if (!failedUnsubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = new HashMap<URL, Set<NotifyListener>>(failedUnsubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<URL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry unsubscribe " + failed);
                }
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            try {
                                // 执行取消注册
                                doUnsubscribe(url, listener);
                                // 移除出监听器
                                listeners.remove(listener);
                            } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                                logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                    logger.warn("Failed to retry unsubscribe " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
        // 重试执行通知监听器
        if (!failedNotified.isEmpty()) {
            Map<URL, Map<NotifyListener, List<URL>>> failed = new HashMap<URL, Map<NotifyListener, List<URL>>>(failedNotified);
            for (Map.Entry<URL, Map<NotifyListener, List<URL>>> entry : new HashMap<URL, Map<NotifyListener, List<URL>>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                if (logger.isInfoEnabled()) {
                    logger.info("Retry notify " + failed);
                }
                try {
                    for (Map<NotifyListener, List<URL>> values : failed.values()) {
                        for (Map.Entry<NotifyListener, List<URL>> entry : values.entrySet()) {
                            try {
                                NotifyListener listener = entry.getKey();
                                List<URL> urls = entry.getValue();
                                // 通知监听器
                                listener.notify(urls);
                                // 移除出监听器
                                values.remove(listener);
                            } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                                logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                            }
                        }
                    }
                } catch (Throwable t) { // Ignore all the exceptions and wait for the next retry
                    logger.warn("Failed to retry notify " + failed + ", waiting for again, cause: " + t.getMessage(), t);
                }
            }
        }
    }

    @Override
    public void destroy() {
        // 忽略，若已经销毁
        if (!canDestroy()) {
            return;
        }
        // 调用父方法，取消注册和订阅
        super.destroy();
        // 销毁重试任务
        try {
            retryFuture.cancel(true);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    // TODO: 2017/8/30 to abstract this method
    protected boolean canDestroy(){
        return destroyed.compareAndSet(false, true);
    }

    // ==== Template method ====

    protected abstract void doRegister(URL url);

    protected abstract void doUnregister(URL url);

    protected abstract void doSubscribe(URL url, NotifyListener listener);

    protected abstract void doUnsubscribe(URL url, NotifyListener listener);
}
```

### RegistryFactory 和 AbstractRegistryFactory

RegistryFactory 接口 是 Registry 的工厂接口，用来返回 Registry 实例。该接口是一个可扩展接口，可以看到该接口上有个@SPI 注解，并且默认值为 dubbo，也就是默认扩展的是 DubboRegistryFactory。AbstractRegistryFactory 则是实现了 RegistryFactory 接口 的抽象类。其源码如下。

```java
/**
 * 注册中心工厂
 */
@SPI("dubbo")
public interface RegistryFactory {

    /**
     * 根据注册中心连接地址，获取注册中心实例
     * <p>
     * 连接注册中心需处理契约：<br>
     * 1. 当设置check=false时表示不检查连接，否则在连接不上时抛出异常。<br>
     * 2. 支持URL上的username:password权限认证。<br>
     * 3. 支持backup=10.20.153.10备选注册中心集群地址。<br>
     * 4. 支持file=registry.cache本地磁盘文件缓存。<br>
     * 5. 支持timeout=1000请求超时设置。<br>
     * 6. 支持session=60000会话超时或过期设置。<br>
     *
     * @param url 注册中心地址，不允许为空
     * @return 注册中心引用，总不返回空
     */
    @Adaptive({"protocol"})
    Registry getRegistry(URL url);
}

/**
 * 注册中心抽象类
 */
public abstract class AbstractRegistryFactory implements RegistryFactory {

    // Log output
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    // The lock for the acquisition process of the registry
    private static final ReentrantLock LOCK = new ReentrantLock();

    /**
     * Registry 集合
     */
    private static final Map<String, Registry> REGISTRIES = new ConcurrentHashMap<String, Registry>();

    /**
     * Get all registries
     */
    public static Collection<Registry> getRegistries() {
        return Collections.unmodifiableCollection(REGISTRIES.values());
    }

    /**
     * 销毁所有 Registry
     */
    // TODO: 2017/8/30 to move somewhere else better
    public static void destroyAll() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Close all registries " + getRegistries());
        }
        // 获得锁
        LOCK.lock();
        try {
            // 销毁
            for (Registry registry : getRegistries()) {
                try {
                    registry.destroy();
                } catch (Throwable e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            // 清空缓存
            REGISTRIES.clear();
        } finally {
            // 释放锁
            LOCK.unlock();
        }
    }

    /**
     * 获得注册中心 Registry 对象
     *
     * @param url 注册中心地址，不允许为空
     * @return Registry 对象
     */
    @Override
    public Registry getRegistry(URL url) {
        // 修改 URL
        url = url.setPath(RegistryService.class.getName()) // + `path`
                .addParameter(Constants.INTERFACE_KEY, RegistryService.class.getName()) // + `parameters.interface`
                .removeParameters(Constants.EXPORT_KEY, Constants.REFER_KEY); // - `export`
        // 计算 key
        String key = url.toServiceString();
        // 获得锁
        // Lock the registry access process to ensure a single instance of the registry
        LOCK.lock();
        try {
            // 从缓存中获得 Registry 对象
            Registry registry = REGISTRIES.get(key);
            if (registry != null) {
                return registry;
            }
            // 缓存不存在，进行创建 Registry 对象
            registry = createRegistry(url);
            if (registry == null) {
                throw new IllegalStateException("Can not create registry " + url);
            }
            // 添加到缓存
            REGISTRIES.put(key, registry);
            return registry;
        } finally {
            // 释放锁
            // Release the lock
            LOCK.unlock();
        }
    }

    /**
     * 创建 Registry 对象
     *
     * @param url 注册中心地址
     * @return Registry 对象
     */
    protected abstract Registry createRegistry(URL url);
}
```

### NotifyListener 和 RegistryDirectory

最后我们来看一下 dubbo-registry-api 模块下的另一个比较重要的组件，NotifyListener 接口 和 RegistryDirectory 抽象类。NotifyListener 接口 只有一个 notify 方法，通知监听器。当收到服务变更通知时触发。RegistryDirectory 是注册中心服务，维护着所有可用的远程 Invoker 或者本地的 Invoker，它的 Invoker 集合是从注册中心获取的，另外，它实现了 NotifyListener 接口。比如消费方要调用某远程服务，会向注册中心订阅这个服务的所有 服务提供方，在订阅 及 服务提供方数据有变动时，回调消费方的 NotifyListener 服务的 notify 方法，回调接口传入所有服务提供方的 url 地址然后将 urls 转化为 invokers，也就是 refer 应用远程服务。源码如下。

```java
/**
 * 通知监听器
 */
public interface NotifyListener {

    /**
     * 当收到服务变更通知时触发。
     * <p>
     * 通知需处理契约：<br>
     * 1. 总是以服务接口和数据类型为维度全量通知，即不会通知一个服务的同类型的部分数据，用户不需要对比上一次通知结果。<br>
     * 2. 订阅时的第一次通知，必须是一个服务的所有类型数据的全量通知。<br>
     * 3. 中途变更时，允许不同类型的数据分开通知，比如：providers, consumers, routers, overrides，允许只通知其中一种类型，但该类型的数据必须是全量的，不是增量的。<br>
     * 4. 如果一种类型的数据为空，需通知一个empty协议并带category参数的标识性URL数据。<br>
     * 5. 通知者(即注册中心实现)需保证通知的顺序，比如：单线程推送，队列串行化，带版本对比。<br>
     *
     * @param urls 已注册信息列表，总不为空，含义同{@link com.alibaba.dubbo.registry.RegistryService#lookup(URL)}的返回值。
     */
    void notify(List<URL> urls);
}


/**
 * 基于注册中心的 Directory 实现类
 */
public class RegistryDirectory<T> extends AbstractDirectory<T> implements NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryDirectory.class);

    // ========== Dubbo SPI Adaptive 对象 BEGIN ==========

    /**
     * Cluster$Adaptive 对象
     */
    private static final Cluster cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getAdaptiveExtension();
    /**
     * RouterFactory$Adaptive 对象
     */
    private static final RouterFactory routerFactory = ExtensionLoader.getExtensionLoader(RouterFactory.class).getAdaptiveExtension();
    /**
     * ConfiguratorFactory$Adaptive 对象
     */
    private static final ConfiguratorFactory configuratorFactory = ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getAdaptiveExtension();

    // ========== 服务消费者相关 BEGIN ==========

    /**
     * 服务类型，例如：com.alibaba.dubbo.demo.DemoService
     */
    private final Class<T> serviceType; // Initialization at construction time, assertion not null
    /**
     * Consumer URL 的配置项 Map
     */
    private final Map<String, String> queryMap; // Initialization at construction time, assertion not null
    /**
     * 服务方法数组
     */
    private final String[] serviceMethods;
    /**
     * 是否引用多分组
     *
     * 服务分组：https://dubbo.gitbooks.io/dubbo-user-book/demos/service-group.html
     */
    private final boolean multiGroup;

    // ========== 注册中心相关 BEGIN ==========

    /**
     * 注册中心的 Protocol 对象
     */
    private Protocol protocol; // Initialization at the time of injection, the assertion is not null
    /**
     * 注册中心
     */
    private Registry registry; // Initialization at the time of injection, the assertion is not null
    /**
     * 注册中心的服务类，目前是 com.alibaba.dubbo.registry.RegistryService
     *
     * 通过 {@link #url} 的 {@link URL#getServiceKey()} 获得
     */
    private final String serviceKey; // Initialization at construction time, assertion not null
    /**
     * 是否禁止访问。
     *
     * 有两种情况会导致：
     *
     * 1. 没有服务提供者
     * 2. 服务提供者被禁用
     */
    private volatile boolean forbidden = false;

    // ========== 配置规则相关 BEGIN ==========

    /**
     * 原始的目录 URL
     *
     * 例如：zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService?application=demo-consumer&callbacks=1000&check=false&client=netty4&cluster=failback&dubbo=2.0.0&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello,callbackParam,save,update,say03,delete,say04,demo,say01,bye,say02,saves&payload=1000&pid=63400&qos.port=33333&register.ip=192.168.16.23&sayHello.async=true&side=consumer&timeout=10000&timestamp=1527056491064
     */
    private final URL directoryUrl; // Initialization at construction time, assertion not null, and always assign non null value
    /**
     * 覆写的目录 URL ，结合配置规则
     */
    private volatile URL overrideDirectoryUrl; // Initialization at construction time, assertion not null, and always assign non null value
    /**
     * 配置规则数组
     *
     * override rules
     * Priority: override>-D>consumer>provider
     * Rule one: for a certain provider <ip:port,timeout=100>
     * Rule two: for all providers <* ,timeout=5000>
     */
    private volatile List<Configurator> configurators; // The initial value is null and the midway may be assigned to null, please use the local variable reference

    // ========== 服务提供者相关 BEGIN ==========

    /**
     * [url]与[服务提供者 Invoker 集合]的映射缓存
     */
    // Map<url, Invoker> cache service url to invoker mapping.
    private volatile Map<String, Invoker<T>> urlInvokerMap; // The initial value is null and the midway may be assigned to null, please use the local variable reference
    /**
     * [方法名]与[服务提供者 Invoker 集合]的映射缓存
     */
    // Map<methodName, Invoker> cache service method to invokers mapping.
    private volatile Map<String, List<Invoker<T>>> methodInvokerMap; // The initial value is null and the midway may be assigned to null, please use the local variable reference
    /**
     * [服务提供者 Invoker 集合]缓存
     */
    // Set<invokerUrls> cache invokeUrls to invokers mapping.
    private volatile Set<URL> cachedInvokerUrls; // The initial value is null and the midway may be assigned to null, please use the local variable reference

    public RegistryDirectory(Class<T> serviceType, URL url) {
        super(url);
        if (serviceType == null) {
            throw new IllegalArgumentException("service type is null.");
        }
        if (url.getServiceKey() == null || url.getServiceKey().length() == 0) {
            throw new IllegalArgumentException("registry serviceKey is null.");
        }
        this.serviceType = serviceType;
        this.serviceKey = url.getServiceKey();
        // 获得 queryMap
        this.queryMap = StringUtils.parseQueryString(url.getParameterAndDecoded(Constants.REFER_KEY));
        // 获得 overrideDirectoryUrl 和 directoryUrl
        this.overrideDirectoryUrl = this.directoryUrl = url.setPath(url.getServiceInterface()).clearParameters().addParameters(queryMap).removeParameter(Constants.MONITOR_KEY);
        // 初始化 multiGroup
        String group = directoryUrl.getParameter(Constants.GROUP_KEY, "");
        this.multiGroup = group != null && ("*".equals(group) || group.contains(","));
        // 初始化 serviceMethods
        String methods = queryMap.get(Constants.METHODS_KEY);
        this.serviceMethods = methods == null ? null : Constants.COMMA_SPLIT_PATTERN.split(methods);
    }

    /**
     * 将overrideURL 转换为 map，供重新 refer 时使用.
     * 每次下发全部规则，全部重新组装计算
     *
     * @param urls 契约：
     *             </br>1.override://0.0.0.0/...(或override://ip:port...?anyhost=true)&para1=value1...表示全局规则(对所有的提供者全部生效)
     *             </br>2.override://ip:port...?anyhost=false 特例规则（只针对某个提供者生效）
     *             </br>3.不支持override://规则... 需要注册中心自行计算.
     *             </br>4.不带参数的override://0.0.0.0/ 表示清除override
     *
     * @return Configurator 集合
     */
    public static List<Configurator> toConfigurators(List<URL> urls) {
        // 忽略，若配置规则 URL 集合为空
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        // 创建 Configurator 集合
        List<Configurator> configurators = new ArrayList<Configurator>(urls.size());
        for (URL url : urls) {
            // 若协议为 `empty://` ，意味着清空所有配置规则，因此返回空 Configurator 集合
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                configurators.clear();
                break;
            }
            // 对应第 4 条契约，不带参数的 override://0.0.0.0/ 表示清除 override
            Map<String, String> override = new HashMap<String, String>(url.getParameters());
            // The anyhost parameter of override may be added automatically, it can't change the judgement of changing url
            // override 上的 anyhost 可能是自动添加的，不能影响改变url判断
            override.remove(Constants.ANYHOST_KEY);
            if (override.size() == 0) {
                configurators.clear();
                continue;
            }
            // 获得 Configurator 对象，并添加到 `configurators` 中
            configurators.add(configuratorFactory.getConfigurator(url));
        }
        // 排序
        Collections.sort(configurators);
        return configurators;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    /**
     * 发起订阅
     *
     * @param url 消费者 URL
     */
    public void subscribe(URL url) {
        // 设置消费者 URL
        setConsumerUrl(url);
        // 向注册中心，发起订阅
        registry.subscribe(url, this);
    }

    @Override
    public void destroy() {
        if (isDestroyed()) {
            return;
        }
        // 取消订阅
        // unsubscribe.
        try {
            if (getConsumerUrl() != null && registry != null && registry.isAvailable()) {
                registry.unsubscribe(getConsumerUrl(), this);
            }
        } catch (Throwable t) {
            logger.warn("unexpeced error when unsubscribe service " + serviceKey + "from registry" + registry.getUrl(), t);
        }
        // 标记已经销毁
        super.destroy(); // must be executed after unsubscribing
        // 销毁所有 Invoker
        try {
            destroyAllInvokers();
        } catch (Throwable t) {
            logger.warn("Failed to destroy service " + serviceKey, t);
        }
    }

    @Override
    public synchronized void notify(List<URL> urls) {
        // 根据 URL 的分类或协议，分组成三个集合 。
        List<URL> invokerUrls = new ArrayList<URL>(); // 服务提供者 URL 集合
        List<URL> routerUrls = new ArrayList<URL>();
        List<URL> configuratorUrls = new ArrayList<URL>();
        for (URL url : urls) {
            String protocol = url.getProtocol();
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
            if (Constants.ROUTERS_CATEGORY.equals(category) || Constants.ROUTE_PROTOCOL.equals(protocol)) {
                routerUrls.add(url);
            } else if (Constants.CONFIGURATORS_CATEGORY.equals(category) || Constants.OVERRIDE_PROTOCOL.equals(protocol)) {
                configuratorUrls.add(url);
            } else if (Constants.PROVIDERS_CATEGORY.equals(category)) {
                invokerUrls.add(url);
            } else {
                logger.warn("Unsupported category " + category + " in notified url: " + url + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost());
            }
        }
        // 处理配置规则 URL 集合
        // configurators
        if (!configuratorUrls.isEmpty()) {
            this.configurators = toConfigurators(configuratorUrls);
        }
        // 处理路由规则 URL 集合
        // routers
        if (!routerUrls.isEmpty()) {
            List<Router> routers = toRouters(routerUrls);
            if (routers != null) { // null - do nothing
                setRouters(routers);
            }
        }
        // 合并配置规则，到 `directoryUrl` 中，形成 `overrideDirectoryUrl` 变量。
        List<Configurator> localConfigurators = this.configurators; // local reference
        // merge override parameters
        this.overrideDirectoryUrl = directoryUrl;
        if (localConfigurators != null && !localConfigurators.isEmpty()) {
            for (Configurator configurator : localConfigurators) {
                this.overrideDirectoryUrl = configurator.configure(overrideDirectoryUrl);
            }
        }
        // 处理服务提供者 URL 集合
        // providers
        refreshInvoker(invokerUrls);
    }

    /**
     * 根据invokerURL列表转换为invoker列表。转换规则如下：
     *
     * 1.如果url已经被转换为invoker，则不在重新引用，直接从缓存中获取，注意如果url中任何一个参数变更也会重新引用
     * 2.如果传入的invoker列表不为空，则表示最新的invoker列表
     * 3.如果传入的invokerUrl列表是空，则表示只是下发的override规则或route规则，需要重新交叉对比，决定是否需要重新引用。
     *
     * @param invokerUrls 传入的参数不能为null
     */
    // TODO: 2017/8/31 FIXME The thread pool should be used to refresh the address, otherwise the task may be accumulated.
    private void refreshInvoker(List<URL> invokerUrls) {
        if (invokerUrls != null && invokerUrls.size() == 1 && invokerUrls.get(0) != null
                && Constants.EMPTY_PROTOCOL.equals(invokerUrls.get(0).getProtocol())) {
            // 设置禁止访问
            this.forbidden = true; // Forbid to access
            // methodInvokerMap 置空
            this.methodInvokerMap = null; // Set the method invoker map to null
            // 销毁所有 Invoker 集合
            destroyAllInvokers(); // Close all invokers
        } else {
            // 设置允许访问
            this.forbidden = false; // Allow to access
            // 引用老的 urlInvokerMap
            Map<String, Invoker<T>> oldUrlInvokerMap = this.urlInvokerMap; // local reference
            // 传入的 invokerUrls 为空，说明是路由规则或配置规则发生改变，此时 invokerUrls 是空的，直接使用 cachedInvokerUrls 。
            if (invokerUrls.isEmpty() && this.cachedInvokerUrls != null) {
                invokerUrls.addAll(this.cachedInvokerUrls);
            // 传入的 invokerUrls 非空，更新 cachedInvokerUrls 。
            } else {
                this.cachedInvokerUrls = new HashSet<URL>();
                this.cachedInvokerUrls.addAll(invokerUrls); //Cached invoker urls, convenient for comparison //缓存invokerUrls列表，便于交叉对比
            }
            // 忽略，若无 invokerUrls
            if (invokerUrls.isEmpty()) {
                return;
            }
            // 将传入的 invokerUrls ，转成新的 urlInvokerMap
            Map<String, Invoker<T>> newUrlInvokerMap = toInvokers(invokerUrls);// Translate url list to Invoker map
            // 转换出新的 methodInvokerMap
            Map<String, List<Invoker<T>>> newMethodInvokerMap = toMethodInvokers(newUrlInvokerMap); // Change method name to map Invoker Map
            // state change
            // If the calculation is wrong, it is not processed. 如果计算错误，则不进行处理.
            if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
                logger.error(new IllegalStateException("urls to invokers error .invokerUrls.size :" + invokerUrls.size() + ", invoker.size :0. urls :" + invokerUrls.toString()));
                return;
            }
            // 若服务引用多 group ，则按照 method + group 聚合 Invoker 集合
            this.methodInvokerMap = multiGroup ? toMergeMethodInvokerMap(newMethodInvokerMap) : newMethodInvokerMap;
            this.urlInvokerMap = newUrlInvokerMap;
            // 销毁不再使用的 Invoker 集合
            try {
                destroyUnusedInvokers(oldUrlInvokerMap, newUrlInvokerMap); // Close the unused Invoker
            } catch (Exception e) {
                logger.warn("destroyUnusedInvokers error. ", e);
            }
        }
    }

    /**
     * 若服务引用多 group ，则按照 method + group 聚合 Invoker 集合
     */
    private Map<String, List<Invoker<T>>> toMergeMethodInvokerMap(Map<String, List<Invoker<T>>> methodMap) {
        Map<String, List<Invoker<T>>> result = new HashMap<String, List<Invoker<T>>>();
        // 循环方法，按照 method + group 聚合 Invoker 集合
        for (Map.Entry<String, List<Invoker<T>>> entry : methodMap.entrySet()) {
            String method = entry.getKey();
            List<Invoker<T>> invokers = entry.getValue();
            // 按照 Group 聚合 Invoker 集合的结果。其中，KEY：group VALUE：Invoker 集合。
            Map<String, List<Invoker<T>>> groupMap = new HashMap<String, List<Invoker<T>>>();
            // 循环 Invoker 集合，按照 group 聚合 Invoker 集合
            for (Invoker<T> invoker : invokers) {
                String group = invoker.getUrl().getParameter(Constants.GROUP_KEY, "");
                List<Invoker<T>> groupInvokers = groupMap.get(group);
                if (groupInvokers == null) {
                    groupInvokers = new ArrayList<Invoker<T>>();
                    groupMap.put(group, groupInvokers);
                }
                groupInvokers.add(invoker);
            }
            // 大小为 1，使用第一个
            if (groupMap.size() == 1) {
                result.put(method, groupMap.values().iterator().next());
            // 大于 1，将每个 Group 的 Invoker 集合，创建成 Cluster Invoker 对象。
            } else if (groupMap.size() > 1) {
                List<Invoker<T>> groupInvokers = new ArrayList<Invoker<T>>();
                for (List<Invoker<T>> groupList : groupMap.values()) {
                    groupInvokers.add(cluster.join(new StaticDirectory<T>(groupList)));
                }
                result.put(method, groupInvokers);
            // 大小为 0 ，使用原有值
            } else {
                result.put(method, invokers);
            }
        }
        return result;
    }

    /**
     * @param urls
     * @return null : no routers ,do nothing
     * else :routers list
     */
    private List<Router> toRouters(List<URL> urls) {
        List<Router> routers = new ArrayList<Router>();
        if (urls == null || urls.isEmpty()) {
            return routers;
        }
        for (URL url : urls) {
            // 忽略，若是 "empty://" 。一般情况下，所有路由规则被删除时，有且仅有一条协议为 "empty://" 的路由规则 URL
            if (Constants.EMPTY_PROTOCOL.equals(url.getProtocol())) {
                continue;
            }
            // 获得 "router"
            String routerType = url.getParameter(Constants.ROUTER_KEY);
            if (routerType != null && routerType.length() > 0) {
                url = url.setProtocol(routerType);
            }
            try {
                // 创建 Router 对象
                Router router = routerFactory.getRouter(url);
                // 添加到返回结果
                if (!routers.contains(router)) {
                    routers.add(router);
                }
            } catch (Throwable t) {
                logger.error("convert router url to router error, url: " + url, t);
            }
        }
        return routers;
    }

    /**
     * 将服务提供者 URL 集合，转成 Invoker 集合。若该服务提供者 URL 已经转换，则直接复用，不重新引用。
     *
     * @param urls URL 集合
     * @return invokers
     */
    private Map<String, Invoker<T>> toInvokers(List<URL> urls) {
        // 新的 `newUrlInvokerMap`
        Map<String, Invoker<T>> newUrlInvokerMap = new HashMap<String, Invoker<T>>();
        // 若为空，直接返回
        if (urls == null || urls.isEmpty()) {
            return newUrlInvokerMap;
        }
        // 已初始化的服务器提供 URL 集合
        Set<String> keys = new HashSet<String>();
        // 获得引用服务的协议
        String queryProtocols = this.queryMap.get(Constants.PROTOCOL_KEY);
        // 循环服务提供者 URL 集合，转成 Invoker 集合
        for (URL providerUrl : urls) {
            // If protocol is configured at the reference side, only the matching protocol is selected
            // 如果 reference 端配置了 protocol ，则只选择匹配的 protocol
            if (queryProtocols != null && queryProtocols.length() > 0) {
                boolean accept = false;
                String[] acceptProtocols = queryProtocols.split(","); // 可配置多个协议
                for (String acceptProtocol : acceptProtocols) {
                    if (providerUrl.getProtocol().equals(acceptProtocol)) {
                        accept = true;
                        break;
                    }
                }
                if (!accept) {
                    continue;
                }
            }
            // 忽略，若为 `empty://` 协议
            if (Constants.EMPTY_PROTOCOL.equals(providerUrl.getProtocol())) {
                continue;
            }
            // 忽略，若应用程序不支持该协议
            if (!ExtensionLoader.getExtensionLoader(Protocol.class).hasExtension(providerUrl.getProtocol())) {
                logger.error(new IllegalStateException("Unsupported protocol " + providerUrl.getProtocol() + " in notified url: " + providerUrl + " from registry " + getUrl().getAddress() + " to consumer " + NetUtils.getLocalHost()
                        + ", supported protocol: " + ExtensionLoader.getExtensionLoader(Protocol.class).getSupportedExtensions()));
                continue;
            }
            // 合并 URL 参数
            URL url = mergeUrl(providerUrl);
            // 忽略，若已经初始化
            String key = url.toFullString(); // The parameter urls are sorted
            if (keys.contains(key)) { // Repeated url
                continue;
            }
            // 添加到 `keys` 中
            keys.add(key);
            // Cache key is url that does not merge with consumer side parameters, regardless of how the consumer combines parameters, if the server url changes, then refer again
            // 如果服务端 URL 发生变化，则重新 refer 引用
            Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference
            Invoker<T> invoker = localUrlInvokerMap == null ? null : localUrlInvokerMap.get(key);
            if (invoker == null) { // Not in the cache, refer again 未在缓存中，重新引用
                try {
                    // 判断是否开启
                    boolean enabled;
                    if (url.hasParameter(Constants.DISABLED_KEY)) {
                        enabled = !url.getParameter(Constants.DISABLED_KEY, false);
                    } else {
                        enabled = url.getParameter(Constants.ENABLED_KEY, true);
                    }
                    // 若开启，创建 Invoker 对象
                    if (enabled) {
                        // 注意，引用服务
                        invoker = new InvokerDelegate<T>(protocol.refer(serviceType, url), url, providerUrl);
                    }
                } catch (Throwable t) {
                    logger.error("Failed to refer invoker for interface:" + serviceType + ",url:(" + url + ")" + t.getMessage(), t);
                }
                // 添加到 newUrlInvokerMap 中
                if (invoker != null) { // Put new invoker in cache
                    newUrlInvokerMap.put(key, invoker);
                }
            } else { // 在缓存中，直接使用缓存的 Invoker 对象，添加到 newUrlInvokerMap 中
                newUrlInvokerMap.put(key, invoker);
            }
        }
        // 清空 keys
        keys.clear();
        return newUrlInvokerMap;
    }

    /**
     * Merge url parameters. the order is: override > -D >Consumer > Provider
     *
     * 合并 URL 参数，优先级为配置规则 > 服务消费者配置 > 服务提供者配置
     *
     * @param providerUrl 服务提供者 URL
     * @return 合并后的 URL
     */
    private URL mergeUrl(URL providerUrl) {
        // 合并消费端参数
        providerUrl = ClusterUtils.mergeUrl(providerUrl, queryMap); // Merge the consumer side parameters

        // 合并配置规则
        List<Configurator> localConfigurators = this.configurators; // local reference
        if (localConfigurators != null && !localConfigurators.isEmpty()) {
            for (Configurator configurator : localConfigurators) {
                providerUrl = configurator.configure(providerUrl);
            }
        }

        // 不检查连接是否成功，总是创建 Invoker ！因为，启动检查，只有启动阶段需要。此时在检查，已经没必要了。
        providerUrl = providerUrl.addParameter(Constants.CHECK_KEY, String.valueOf(false)); // Do not check whether the connection is successful or not, always create Invoker!

        // The combination of directoryUrl and override is at the end of notify, which can't be handled here
        // 仅合并提供者参数，因为 directoryUrl 与 override 合并是在 notify 的最后，这里不能够处理
        this.overrideDirectoryUrl = this.overrideDirectoryUrl.addParametersIfAbsent(providerUrl.getParameters()); // Merge the provider side parameters

        // 【忽略】因为是对 1.0 版本的兼容
        if ((providerUrl.getPath() == null || providerUrl.getPath().length() == 0)
                && "dubbo".equals(providerUrl.getProtocol())) { // Compatible version 1.0
            //fix by tony.chenl DUBBO-44
            String path = directoryUrl.getParameter(Constants.INTERFACE_KEY);
            if (path != null) {
                int i = path.indexOf('/');
                if (i >= 0) {
                    path = path.substring(i + 1);
                }
                i = path.lastIndexOf(':');
                if (i >= 0) {
                    path = path.substring(0, i);
                }
                providerUrl = providerUrl.setPath(path);
            }
        }

        // 返回服务提供者 URL
        return providerUrl;
    }

    private List<Invoker<T>> route(List<Invoker<T>> invokers, String method) {
        // 创建 Invocation 对象
        Invocation invocation = new RpcInvocation(method, new Class<?>[0], new Object[0]);
        // 获得 Router 数组
        List<Router> routers = getRouters();
        // 根据路由规则，筛选 Invoker 集合
        if (routers != null) {
            for (Router router : routers) {
                if (router.getUrl() != null) {
                    invokers = router.route(invokers, getConsumerUrl(), invocation);
                }
            }
        }
        return invokers;
    }

    /**
     * 将invokers列表转成与方法的映射关系
     *
     * @param invokersMap Invoker列表
     * @return Invoker与方法的映射关系
     */
    private Map<String, List<Invoker<T>>> toMethodInvokers(Map<String, Invoker<T>> invokersMap) {
        // 创建新的 `methodInvokerMap`
        Map<String, List<Invoker<T>>> newMethodInvokerMap = new HashMap<String, List<Invoker<T>>>();
        // 创建 Invoker 集合
        List<Invoker<T>> invokersList = new ArrayList<Invoker<T>>();
        // According to the methods classification declared by the provider URL, the methods is compatible with the registry to execute the filtered methods
        // 按服务提供者 URL 所声明的 methods 分类，兼容注册中心执行路由过滤掉的 methods
        if (invokersMap != null && invokersMap.size() > 0) {
            // 循环每个服务提供者 Invoker
            for (Invoker<T> invoker : invokersMap.values()) {
                String parameter = invoker.getUrl().getParameter(Constants.METHODS_KEY); // methods
                if (parameter != null && parameter.length() > 0) {
                    String[] methods = Constants.COMMA_SPLIT_PATTERN.split(parameter);
                    if (methods != null && methods.length > 0) {
                        // 循环每个方法，按照方法名为维度，聚合到 `methodInvokerMap` 中
                        for (String method : methods) {
                            if (method != null && method.length() > 0 && !Constants.ANY_VALUE.equals(method)) { // 当服务提供者的方法为 "*" ，代表泛化调用
                                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                                if (methodInvokers == null) {
                                    methodInvokers = new ArrayList<Invoker<T>>();
                                    newMethodInvokerMap.put(method, methodInvokers);
                                }
                                methodInvokers.add(invoker);
                            }
                        }
                    }
                }
                // 添加到 `invokersList` 中
                invokersList.add(invoker);
            }
        }
        // 路由全 `invokersList` ，匹配合适的 Invoker 集合
        List<Invoker<T>> newInvokersList = route(invokersList, null);
        // 添加 `newInvokersList` 到 `newMethodInvokerMap` 中，表示该服务提供者的全量 Invoker 集合
        newMethodInvokerMap.put(Constants.ANY_VALUE, newInvokersList);
        // 循环，基于每个方法路由，匹配合适的 Invoker 集合
        if (serviceMethods != null && serviceMethods.length > 0) {
            for (String method : serviceMethods) {
                List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
                if (methodInvokers == null || methodInvokers.isEmpty()) {
                    methodInvokers = newInvokersList;
                }
                newMethodInvokerMap.put(method, route(methodInvokers, method));
            }
        }
        // 循环排序每个方法的 Invoker 集合，并设置为不可变
        // sort and unmodifiable
        for (String method : new HashSet<String>(newMethodInvokerMap.keySet())) {
            List<Invoker<T>> methodInvokers = newMethodInvokerMap.get(method);
            Collections.sort(methodInvokers, InvokerComparator.getComparator());
            newMethodInvokerMap.put(method, Collections.unmodifiableList(methodInvokers));
        }
        return Collections.unmodifiableMap(newMethodInvokerMap);
    }

    /**
     * Close all invokers
     */
    private void destroyAllInvokers() {
        Map<String, Invoker<T>> localUrlInvokerMap = this.urlInvokerMap; // local reference 本地引用，避免并发问题
        if (localUrlInvokerMap != null) {
            // 循环 urlInvokerMap ，销毁所有服务提供者 Invoker
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                try {
                    invoker.destroy();
                } catch (Throwable t) {
                    logger.warn("Failed to destroy service " + serviceKey + " to provider " + invoker.getUrl(), t);
                }
            }
            // urlInvokerMap 清空
            localUrlInvokerMap.clear();
        }
        // methodInvokerMap 置空
        methodInvokerMap = null;
    }

    /**
     * Check whether the invoker in the cache needs to be destroyed
     * If set attribute of url: refer.autodestroy=false, the invokers will only increase without decreasing,there may be a refer leak
     *
     * @param oldUrlInvokerMap
     * @param newUrlInvokerMap
     */
    private void destroyUnusedInvokers(Map<String, Invoker<T>> oldUrlInvokerMap, Map<String, Invoker<T>> newUrlInvokerMap) {
        // 防御性编程，目前不存在这个情况
        if (newUrlInvokerMap == null || newUrlInvokerMap.size() == 0) {
            // 销毁所有服务提供者 Invoker
            destroyAllInvokers();
            return;
        }
        // check deleted invoker
        // 对比新老集合，计算需要销毁的 Invoker 集合
        List<String> deleted = null;
        if (oldUrlInvokerMap != null) {
            Collection<Invoker<T>> newInvokers = newUrlInvokerMap.values();
            for (Map.Entry<String, Invoker<T>> entry : oldUrlInvokerMap.entrySet()) {
                // 若不存在，添加到 `deleted` 中
                if (!newInvokers.contains(entry.getValue())) {
                    if (deleted == null) {
                        deleted = new ArrayList<String>();
                    }
                    deleted.add(entry.getKey());
                }
            }
        }

        // 若有需要销毁的 Invoker ，则进行销毁
        if (deleted != null) {
            for (String url : deleted) {
                if (url != null) {
                    // 移除出 `urlInvokerMap`
                    Invoker<T> invoker = oldUrlInvokerMap.remove(url);
                    if (invoker != null) {
                        try {
                            // 销毁 Invoker
                            invoker.destroy();
                            if (logger.isDebugEnabled()) {
                                logger.debug("destroy invoker[" + invoker.getUrl() + "] success. ");
                            }
                        } catch (Exception e) {
                            logger.warn("destroy invoker[" + invoker.getUrl() + "] failed. " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<Invoker<T>> doList(Invocation invocation) {
        if (forbidden) {
            // 1. No service provider 2. Service providers are disabled
            throw new RpcException(RpcException.FORBIDDEN_EXCEPTION,
                "No provider available from registry " + getUrl().getAddress() + " for service " + getConsumerUrl().getServiceKey() + " on consumer " +  NetUtils.getLocalHost()
                        + " use dubbo version " + Version.getVersion() + ", please check status of providers(disabled, not registered or in blacklist).");
        }
        List<Invoker<T>> invokers = null;
        Map<String, List<Invoker<T>>> localMethodInvokerMap = this.methodInvokerMap; // local reference
        // 获得 Invoker 集合
        if (localMethodInvokerMap != null && localMethodInvokerMap.size() > 0) {
            // 获得方法名、方法参数
            String methodName = RpcUtils.getMethodName(invocation);
            Object[] args = RpcUtils.getArguments(invocation);
            // 【第一】可根据第一个参数枚举路由
            if (args != null && args.length > 0 && args[0] != null
                    && (args[0] instanceof String || args[0].getClass().isEnum())) {
//                invokers = localMethodInvokerMap.get(methodName + "." + args[0]); // The routing can be enumerated according to the first parameter
                invokers = localMethodInvokerMap.get(methodName + args[0]); // The routing can be enumerated according to the first parameter
            }
            // 【第二】根据方法名获得 Invoker 集合
            if (invokers == null) {
                invokers = localMethodInvokerMap.get(methodName);
            }
            // 【第三】使用全量 Invoker 集合。例如，`#$echo(name)` ，回声方法
            if (invokers == null) {
                invokers = localMethodInvokerMap.get(Constants.ANY_VALUE);
            }
            // 【第四】使用 `methodInvokerMap` 第一个 Invoker 集合。防御性编程。
            if (invokers == null) {
                Iterator<List<Invoker<T>>> iterator = localMethodInvokerMap.values().iterator();
                if (iterator.hasNext()) {
                    invokers = iterator.next();
                }
            }
        }
        return invokers == null ? new ArrayList<Invoker<T>>(0) : invokers;
    }

    @Override
    public Class<T> getInterface() {
        return serviceType;
    }

    @Override
    public URL getUrl() {
        return this.overrideDirectoryUrl;
    }

    @Override
    public boolean isAvailable() {
        // 若已销毁，返回不可用
        if (isDestroyed()) {
            return false;
        }
        // 任意一个 Invoker 可用，则返回可用
        Map<String, Invoker<T>> localUrlInvokerMap = urlInvokerMap;
        if (localUrlInvokerMap != null && localUrlInvokerMap.size() > 0) {
            for (Invoker<T> invoker : new ArrayList<Invoker<T>>(localUrlInvokerMap.values())) {
                if (invoker.isAvailable()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Haomin: added for test purpose
     */
    public Map<String, Invoker<T>> getUrlInvokerMap() {
        return urlInvokerMap;
    }

    /**
     * Haomin: added for test purpose
     */
    public Map<String, List<Invoker<T>>> getMethodInvokerMap() {
        return methodInvokerMap;
    }

    /**
     * Invoker 排序器，根据 URL 升序
     */
    private static class InvokerComparator implements Comparator<Invoker<?>> {

        /**
         * 单例
         */
        private static final InvokerComparator comparator = new InvokerComparator();

        private InvokerComparator() {
        }

        public static InvokerComparator getComparator() {
            return comparator;
        }

        @Override
        public int compare(Invoker<?> o1, Invoker<?> o2) {
            return o1.getUrl().toString().compareTo(o2.getUrl().toString());
        }

    }

    /**
     *
     * Invoker 代理类，主要用于存储注册中心下发的 url 地址，用于重新重新 refer 时能够根据 providerURL queryMap overrideMap 重新组装
     *
     * @param <T>
     */
    private static class InvokerDelegate<T> extends InvokerWrapper<T> {

        /**
         * 服务提供者 URL
         *
         * 未经过配置合并
         */
        private URL providerUrl;

        public InvokerDelegate(Invoker<T> invoker, URL url, URL providerUrl) {
            super(invoker, url);
            this.providerUrl = providerUrl;
        }

        public URL getProviderUrl() {
            return providerUrl;
        }
    }
}
```

Dubbo 的注册中心虽然提供了多种实现，但生产上的事实标准基本上都是 基于 Zookeeper 实现的。这种注册中心的实现方法也是 Dubbo 最为推荐的。为了易于理解 Zookeeper 在 Dubbo 中的应用，我们先简单看一下 zookeeper。

由于 Dubbo 是一个分布式 RPC 开源框架，各服务之间单独部署，往往会出现资源之间数据不一致的问题，比如：某一个服务增加或减少了几台机器，某个服务提供者变更了服务地址，那么服务消费者是很难感知到这种变化的。而 Zookeeper 本身就有保证分布式数据一致性的特性。那么 Dubbo 服务是如何被 Zookeeper 的数据结构存储管理的呢，zookeeper 采用的是树形结构来组织数据节点，它类似于一个标准的文件系统，如下图所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/dubbo%E6%B3%A8%E5%86%8C%E4%B8%AD%E5%BF%83%E5%9C%A8zookeeper%E4%B8%AD%E7%9A%84%E7%BB%93%E6%9E%84-2fb576.png)

该图展示了 dubbo 在 zookeeper 中存储的形式以及节点层级。dubbo 的 Root 层是根目录，通过<dubbo:registry group="dubbo" />的“group”来设置 zookeeper 的根节点，缺省值是“dubbo”。Service 层是服务接口的全名。Type 层是分类，一共有四种分类，分别是 providers 服务提供者列表、consumers 服务消费者列表、routes 路由规则列表、configurations 配置规则列表。URL 层 根据不同的 Type 目录：可以有服务提供者 URL 、服务消费者 URL 、路由规则 URL 、配置规则 URL 。不同的 Type 关注的 URL 不同。

zookeeper 以斜杠来分割每一层的 znode 节点，比如第一层根节点 dubbo 就是“/dubbo”，而第二层的 Service 层就是/dubbo/com.foo.Barservice，zookeeper 的每个节点通过路径来表示以及访问，例如服务提供者启动时，向/dubbo/com.foo.Barservice/providers 目录下写入自己的 URL 地址。

dubbo-registry-zookeeper 模块的工程结构如下图所示，里面就俩类，非常简单。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/dubbo-registry-zookeeper%E6%A8%A1%E5%9D%97%E5%B7%A5%E7%A8%8B%E7%BB%93%E6%9E%84%E5%9B%BE-7781d2.png)

### ZookeeperRegistry

该类继承了 FailbackRegistry 抽象类，针对注册中心核心的 服务注册、服务订阅、取消注册、取消订阅，查询注册列表进行展开，这里用到了 模板方法设计模式，FailbackRegistry 中定义了 register()、subscribe()等模板方法和 doRegister()、doSubscribe()抽象方法，ZookeeperRegistry 基于 zookeeper 对这些抽象方法进行了实现。其实你会发现 zookeeper 虽然是最被推荐的，反而它的实现逻辑相对简单，因为调用了 zookeeper 服务组件，很多的逻辑不需要在 dubbo 中自己去实现。

```java
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.registry.zookeeper;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.alibaba.dubbo.common.utils.UrlUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.remoting.zookeeper.ChildListener;
import com.alibaba.dubbo.remoting.zookeeper.StateListener;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ZookeeperRegistry
 *
 * Zookeeper Registry 实现类
 */
public class ZookeeperRegistry extends FailbackRegistry {

    private final static Logger logger = LoggerFactory.getLogger(ZookeeperRegistry.class);

    /**
     * 默认端口
     */
    private final static int DEFAULT_ZOOKEEPER_PORT = 2181;
    /**
     * 默认 Zookeeper 根节点
     */
    private final static String DEFAULT_ROOT = "dubbo";

    /**
     * Zookeeper 根节点
     */
    private final String root;
    /**
     * Service 接口全名集合
     */
    private final Set<String> anyServices = new ConcurrentHashSet<String>();
    /**
     * 监听器集合
     */
    private final ConcurrentMap<URL, ConcurrentMap<NotifyListener, ChildListener>> zkListeners = new ConcurrentHashMap<URL, ConcurrentMap<NotifyListener, ChildListener>>();
    /**
     * Zookeeper 客户端
     */
    private final ZookeeperClient zkClient;

    public ZookeeperRegistry(URL url, ZookeeperTransporter zookeeperTransporter) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        // 获得 Zookeeper 根节点
        String group = url.getParameter(Constants.GROUP_KEY, DEFAULT_ROOT); // `url.parameters.group` 参数值
        if (!group.startsWith(Constants.PATH_SEPARATOR)) {
            group = Constants.PATH_SEPARATOR + group;
        }
        this.root = group;
        // 创建 Zookeeper Client
        zkClient = zookeeperTransporter.connect(url);
        // 添加 StateListener 对象。该监听器，在重连时，调用恢复方法。
        zkClient.addStateListener(new StateListener() {
            public void stateChanged(int state) {
                if (state == RECONNECTED) {
                    try {
                        recover();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
    }

    // 目前只有测试方法使用
    static String appendDefaultPort(String address) {
        if (address != null && address.length() > 0) {
            int i = address.indexOf(':');
            if (i < 0) {
                return address + ":" + DEFAULT_ZOOKEEPER_PORT;
            } else if (Integer.parseInt(address.substring(i + 1)) == 0) {
                return address.substring(0, i + 1) + DEFAULT_ZOOKEEPER_PORT;
            }
        }
        return address;
    }

    @Override
    public boolean isAvailable() {
        return zkClient.isConnected();
    }

    @Override
    public void destroy() {
        // 调用父方法，取消注册和订阅
        super.destroy();
        try {
            // 关闭 Zookeeper 客户端连接
            zkClient.close();
        } catch (Exception e) {
            logger.warn("Failed to close zookeeper client " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doRegister(URL url) {
        try {
            zkClient.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
        } catch (Throwable e) {
            throw new RpcException("Failed to register " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doUnregister(URL url) {
        try {
            zkClient.delete(toUrlPath(url));
        } catch (Throwable e) {
            throw new RpcException("Failed to unregister " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doSubscribe(final URL url, final NotifyListener listener) {
        try {
            // 处理所有 Service 层的发起订阅，例如监控中心的订阅
            if (Constants.ANY_VALUE.equals(url.getServiceInterface())) {
                String root = toRootPath();
                // 获得 url 对应的监听器集合
                ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                if (listeners == null) { // 不存在，进行创建
                    zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                    listeners = zkListeners.get(url);
                }
                // 获得 ChildListener 对象
                ChildListener zkListener = listeners.get(listener);
                if (zkListener == null) { // 不存在 ChildListener 对象，进行创建 ChildListener 对象
                    listeners.putIfAbsent(listener, new ChildListener() {
                        public void childChanged(String parentPath, List<String> currentChilds) {
                            for (String child : currentChilds) {
                                child = URL.decode(child);
                                // 新增 Service 接口全名时（即新增服务），发起该 Service 层的订阅
                                if (!anyServices.contains(child)) {
                                    anyServices.add(child);
                                    subscribe(url.setPath(child).addParameters(Constants.INTERFACE_KEY, child,
                                            Constants.CHECK_KEY, String.valueOf(false)), listener);
                                }
                            }
                        }
                    });
                    zkListener = listeners.get(listener);
                }
                // 创建 Service 节点。该节点为持久节点。
                zkClient.create(root, false);
                // 向 Zookeeper ，Service 节点，发起订阅
                List<String> services = zkClient.addChildListener(root, zkListener);
                // 首次全量数据获取完成时，循环 Service 接口全名数组，发起该 Service 层的订阅
                if (services != null && !services.isEmpty()) {
                    for (String service : services) {
                        service = URL.decode(service);
                        anyServices.add(service);
                        subscribe(url.setPath(service).addParameters(Constants.INTERFACE_KEY, service,
                                Constants.CHECK_KEY, String.valueOf(false)), listener);
                    }
                }
            // 处理指定 Service 层的发起订阅，例如服务消费者的订阅
            } else {
                // 子节点数据数组
                List<URL> urls = new ArrayList<URL>();
                // 循环分类数组
                for (String path : toCategoriesPath(url)) {
                    // 获得 url 对应的监听器集合
                    ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
                    if (listeners == null) { // 不存在，进行创建
                        zkListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, ChildListener>());
                        listeners = zkListeners.get(url);
                    }
                    // 获得 ChildListener 对象
                    ChildListener zkListener = listeners.get(listener);
                    if (zkListener == null) { // 不存在 ChildListener 对象，进行创建 ChildListener 对象
                        listeners.putIfAbsent(listener, new ChildListener() {
                            public void childChanged(String parentPath, List<String> currentChilds) {
                                // 变更时，调用 `#notify(...)` 方法，回调 NotifyListener
                                ZookeeperRegistry.this.notify(url, listener, toUrlsWithEmpty(url, parentPath, currentChilds));
                            }
                        });
                        zkListener = listeners.get(listener);
                    }
                    // 创建 Type 节点。该节点为持久节点。
                    zkClient.create(path, false);
                    // 向 Zookeeper ，PATH 节点，发起订阅
                    List<String> children = zkClient.addChildListener(path, zkListener);
                    // 添加到 `urls` 中
                    if (children != null) {
                        urls.addAll(toUrlsWithEmpty(url, path, children));
                    }
                }
                // 首次全量数据获取完成时，调用 `#notify(...)` 方法，回调 NotifyListener
                notify(url, listener, urls);
            }
        } catch (Throwable e) {
            throw new RpcException("Failed to subscribe " + url + " to zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        ConcurrentMap<NotifyListener, ChildListener> listeners = zkListeners.get(url);
        if (listeners != null) {
            ChildListener zkListener = listeners.get(listener);
            if (zkListener != null) {
                // 向 Zookeeper ，移除订阅
                zkClient.removeChildListener(toUrlPath(url), zkListener);
            }
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            // 循环分类数组，获得所有的 URL 数组
            List<String> providers = new ArrayList<String>();
            for (String path : toCategoriesPath(url)) {
                List<String> children = zkClient.getChildren(path);
                if (children != null) {
                    providers.addAll(children);
                }
            }
            // 匹配
            return toUrlsWithoutEmpty(url, providers);
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + " from zookeeper " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * 获得根目录
     * @return 路径
     */
    private String toRootDir() {
        if (root.equals(Constants.PATH_SEPARATOR)) {
            return root;
        }
        return root + Constants.PATH_SEPARATOR;
    }

    /**
     * @return 根路径
     */
    private String toRootPath() {
        return root;
    }

    /**
     * 获得服务路径
     *
     * Root + Type
     *
     * @param url URL
     * @return 服务路径
     */
    private String toServicePath(URL url) {
        String name = url.getServiceInterface();
        if (Constants.ANY_VALUE.equals(name)) {
            return toRootPath();
        }
        return toRootDir() + URL.encode(name);
    }

    /**
     * 获得分类路径数组
     *
     * Root + Service + Type
     *
     * @param url URL
     * @return 分类路径数组
     */
    private String[] toCategoriesPath(URL url) {
        // 获得分类数组
        String[] categories;
        if (Constants.ANY_VALUE.equals(url.getParameter(Constants.CATEGORY_KEY))) { // * 时，
            categories = new String[]{Constants.PROVIDERS_CATEGORY, Constants.CONSUMERS_CATEGORY,
                    Constants.ROUTERS_CATEGORY, Constants.CONFIGURATORS_CATEGORY};
        } else {
            categories = url.getParameter(Constants.CATEGORY_KEY, new String[]{Constants.DEFAULT_CATEGORY});
        }
        // 获得分类路径数组
        String[] paths = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            paths[i] = toServicePath(url) + Constants.PATH_SEPARATOR + categories[i];
        }
        return paths;
    }

    /**
     * 获得分类路径
     *
     * Root + Service + Type
     *
     * @param url URL
     * @return 分类路径
     */
    private String toCategoryPath(URL url) {
        return toServicePath(url) + Constants.PATH_SEPARATOR + url.getParameter(Constants.CATEGORY_KEY, Constants.DEFAULT_CATEGORY);
    }

    /**
     * 获得 URL 的路径
     *
     * Root + Service + Type + URL
     *
     * 被 {@link #doRegister(URL)} 和 {@link #doUnregister(URL)} 调用
     *
     * @param url URL
     * @return 路径
     */
    private String toUrlPath(URL url) {
        return toCategoryPath(url) + Constants.PATH_SEPARATOR + URL.encode(url.toFullString());
    }

    /**
     * 获得 providers 中，和 consumer 匹配的 URL 数组
     *
     * @param consumer 用于匹配 URL
     * @param providers 被匹配的 URL 的字符串
     * @return 匹配的 URL 数组
     */
    private List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        List<URL> urls = new ArrayList<URL>();
        if (providers != null && !providers.isEmpty()) {
            for (String provider : providers) {
                provider = URL.decode(provider);
                if (provider.contains("://")) { // 是 url
                    URL url = URL.valueOf(provider); // 将字符串转化成 URL
                    if (UrlUtils.isMatch(consumer, url)) { // 匹配
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    /**
     * 获得 providers 中，和 consumer 匹配的 URL 数组
     *
     * 若不存在匹配，则创建 `empty://` 的 URL返回。通过这样的方式，可以处理类似服务提供者为空的情况。
     *
     * @param consumer 用于匹配 URL
     * @param path 被匹配的 URL 的字符串
     * @param providers 匹配的 URL 数组
     * @return 匹配的 URL 数组
     */
    private List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        // 获得 providers 中，和 consumer 匹配的 URL 数组
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        // 若不存在匹配，则创建 `empty://` 的 URL返回
        if (urls == null || urls.isEmpty()) {
            int i = path.lastIndexOf('/');
            String category = i < 0 ? path : path.substring(i + 1);
            URL empty = consumer.setProtocol(Constants.EMPTY_PROTOCOL).addParameter(Constants.CATEGORY_KEY, category);
            urls.add(empty);
        }
        return urls;
    }
}
```

### ZookeeperRegistryFactory

ZookeeperRegistryFactory 继承了 AbstractRegistryFactory 抽象类，实现了其中的抽象方法 如 createRegistry()，源码如下。

```java
/**
 * Zookeeper Registry 工厂
 */
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {

    /**
     * Zookeeper 工厂
     */
    private ZookeeperTransporter zookeeperTransporter;

    /**
     * 设置 Zookeeper 工厂，该方法，通过 Dubbo SPI 注入
     *
     * @param zookeeperTransporter Zookeeper 工厂对象
     */
    public void setZookeeperTransporter(ZookeeperTransporter zookeeperTransporter) {
        this.zookeeperTransporter = zookeeperTransporter;
    }

    @Override
    public Registry createRegistry(URL url) {
        return new ZookeeperRegistry(url, zookeeperTransporter);
    }
}
```

# 远程通信



## dubbo-remoting 模块整体结构设计

服务治理框架 大致可分为 “服务通信” 和 “服务管理” 两部分，前面我们分析了有关注册中心的源码，也就是服务管理，接下来要分析的就是跟服务通信有关的源码，也就是远程通讯模块。该模块中提供了多种客户端和服务端通信的功能，而在对 NIO 框架选型上，dubbo 交由用户选择，它集成了 mina、netty、grizzly 等各类 NIO 框架来搭建 NIO 服务器和客户端，并且利用 dubbo 的 SPI 扩展机制可以让用户自定义选择。dubbo-remoting 的工程结构如下。

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/dubbo-remoting%E7%9A%84%E5%B7%A5%E7%A8%8B%E7%BB%93%E6%9E%84-f44c5a.png)

## dubbo-remoting-api 模块整体结构设计

本篇我们先来看一下 dubbo-remoting 中 dubbo-remoting-api 的项目结构。

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/dubbo-remoting-api%E7%9A%84%E9%A1%B9%E7%9B%AE%E7%BB%93%E6%9E%84-b54eb0.png)

dubbo-remoting-api 定义了远程通信模块最核心的 API，对于 dubbo-remoting-api 的解读会分为如下五个部分，其中第五部分会在本文介绍。

1. buffer 包：缓冲在 NIO 框架 中是很重要的存在，各个 NIO 框架 都实现了自己相应的缓存操作。这个 buffer 包 下包括了缓冲区的接口以及抽象类；
2. exchange 包：信息交换层，其中封装了请求响应模式，在传输层之上重新封装了 Request-Response 语义，为了满足 RPC 的需求。这层可以认为专注在 Request 和 Response 携带的信息上。该层是 RPC 调用 的通讯基础之一；
3. telnet 包：dubbo 支持通过 telnet 命令 来进行服务治理，该包下就封装了这些通用指令的逻辑实现；
4. transport 包：网络传输层，它只负责单向消息传输，是对 Mina、Netty、Grizzly 的抽象，它也可以扩展 UDP 传输，该层也是 RPC 调用 的通讯基础之一；
5. 最外层的源码：该部分也是我们接下来要重点解析的。

结合 dubbo-remoting-api 模块 的外层类和包划分，我们看看下面的官方架构图。

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/Dubbo%E6%95%B4%E4%BD%93%E6%9E%B6%E6%9E%84%E5%9B%BE-82a499.png)

红框标注的部分是 dubbo 整体架构中的 远程通讯架构，其中 Exchange 组件 和 Transport 组件 在框架设计中起到了很重要的作用，也是支撑 Remoting 的核心。

## dubbo-remoting-api 模块最外层源码解析

### Endpoint 接口

dubbo 抽象出了一个端的概念，也就是 Endpoint 接口，这个端就是一个点，而点与点之间可以双向传输。在端的基础上再衍生出通道、客户端以及服务端的概念，也就是下面要介绍的 Channel、Client、Server 三个接口。在传输层，Client 和 Server 的区别只是语义上的区别，并不区分请求和应答职责，而在交换层，Client 和 Server 是有方向的端点，所以区分了明确的请求和应答职责。两者都具备发送的能力，只是客户端和服务端所关注的事情不一样，而 Endpoint 接口抽象的方法就是它们共同拥有的方法。这也就是它们都能被抽象成端的原因。

```java
/**
 * Endpoint. (API/SPI, Prototype, ThreadSafe)
 *
 * Endpoint 接口
 */
public interface Endpoint {

    /**
     * get url.
     *
     * @return url
     */
    URL getUrl();

    /**
     * get channel handler.
     *
     * 获得通道处理器
     *
     * @return channel handler
     */
    ChannelHandler getChannelHandler();

    /**
     * get local address.
     *
     * @return local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * send message.
     *
     * @param message 消息
     * @throws RemotingException
     */
    void send(Object message) throws RemotingException;

    /**
     * send message.
     *
     * @param message 消息
     * @param sent    already sent to socket?
     */
    void send(Object message, boolean sent) throws RemotingException;

    /**
     * close the channel.
     */
    void close();

    /**
     * Graceful close the channel.
     */
    void close(int timeout);

    void startClose();

    /**
     * is closed.
     *
     * @return closed
     */
    boolean isClosed();
}
```

### Channel 接口

该接口是通道接口，通道是信息传输的载体。Channel 可读可写，并且可以异步读写。Channel 是 client 和 server 的数据传输桥梁。Channel 和 client 是一对一的，也就是一个 client 对应一个 Channel，而 Channel 和 server 则是多对一，也就是一个 server 可以对应多个 Channel。

```java
/**
 * Channel. (API/SPI, Prototype, ThreadSafe)
 *
 * 通道接口
 * 可以看到 Channel 继承了 Endpoint，也就是端抽象出来的方法也同样是 channel 所需要的
 */
public interface Channel extends Endpoint {

	/** 获得远程地址 */
    InetSocketAddress getRemoteAddress();

    /** 判断通道是否连接 */
    boolean isConnected();

    /** 判断是否有该key的值 */
    boolean hasAttribute(String key);

    /** 获得该key对应的值 */
    Object getAttribute(String key);

    /** 设置属性 */
    void setAttribute(String key, Object value);

    /** 删除属性 */
    void removeAttribute(String key);
}
```

### ChannelHandler 接口

```java
/**
 * ChannelHandler. (API, Prototype, ThreadSafe)
 *
 * 通道处理器接口
 * 该接口负责Channel中的逻辑处理，可以看到这个接口有@SPI注解，是个可扩展接口
 */
@SPI
public interface ChannelHandler {

    /** 连接该通道 */
    void connected(Channel channel) throws RemotingException;

    /** 断开该通道 */
    void disconnected(Channel channel) throws RemotingException;

    /** 发送给这个通道消息 */
    void sent(Channel channel, Object message) throws RemotingException;

    /** 从这个通道内接收消息 */
    void received(Channel channel, Object message) throws RemotingException;

    /** 从这个通道内捕获异常 */
    void caught(Channel channel, Throwable exception) throws RemotingException;

}
```

### Client 和 Resetable 接口

```java
/**
 * Remoting Client. (API/SPI, Prototype, ThreadSafe)
 *
 * 客户端接口，可以看到它继承了 Endpoint、Channel 和 Resetable接口，继承Endpoint的原因上面已经提到过了，
 * 客户端和服务端其实只是语义上的不同，客户端就是一个点。继承 Channel 是因为客户端跟通道是一一对应的，
 * 所以做了这样的设计，还继承了 Resetable接口 是为了实现 reset方法，该方法已经打上 @Deprecated注解，不推荐使用。
 * 除了这些客户端就只需要关注一个重连的操作。
 */
public interface Client extends Endpoint, Channel, Resetable {

    /** 重连 */
    void reconnect() throws RemotingException;

    /** 重置，不推荐使用 */
    @Deprecated
    void reset(com.alibaba.dubbo.common.Parameters parameters);
}

public interface Resetable {

    // 用于根据新传入的 url 属性，重置自己内部的一些属性
    void reset(URL url);

}
```

### Server 接口

```java
/**
 * Remoting Server. (API/SPI, Prototype, ThreadSafe)
 *
 * 服务端接口，继承了 Endpoint 和 Resetable，继承 Endpoint 是因为服务端也是一个点，
 * 继承 Resetable接口 是为了继承 reset方法。除了这些以外，服务端独有的是检测是否启动成功，
 * 以及获得连接到 该服务端上的所有Channel，这里获得所有Channel其实就是获取所有连接该服务器的客户端
 */
public interface Server extends Endpoint, Resetable {

    /** 是否绑定本地端口，提供服务。即，是否启动成功，可连接，接收消息等 */
    boolean isBound();

    /** 获得连接到 服务端的通道们（客户端） */
    Collection<Channel> getChannels();

    /** 通过远程地址获得该地址对应的通道 */
    Channel getChannel(InetSocketAddress remoteAddress);

    @Deprecated
    void reset(com.alibaba.dubbo.common.Parameters parameters);
}
```

### Codec2 接口

这两个都是编解码器 接口，在网络中进行传输的数据 都是原始的字节序列，这就需要 发送端使用编码器把 要传输的有意义的信息 序列化成字节序列，接收端再使用解码器 把字节序列再反序列化成 有效信息，而同时具备这两种功能的单一组件就叫 编解码器。在 dubbo 中 Codec 是老编解码器接口，而 Codec2 是新编解码器接口，并且 dubbo 已经用 CodecAdapter 把 Codec 适配成 Codec2 了。所以在这里就只介绍下 Codec2 接口。

```java
/**
 * 编解码器接口，需要注意的是：
 * 1、Codec2 有 @SPI注解，是一个可扩展接口；
 * 2、用到了 Adaptive机制，首先去 url 中找 key 为 codec 的 value，来加载 url 携带的配置中指定的 codec的实现；
 * 3、该接口中有个枚举类型 DecodeResult，因为解码过程中，需要解决 TCP 拆包、粘包的场景，所以增加了这两种解码结果，
 *    关于TCP 拆包、粘包的场景 可用看一下Netty源码解析中的内容
 */
@SPI
public interface Codec2 {

    /** 编码 */
    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;

    /** 解码 */
    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;

    /** 解码结果 */
    enum DecodeResult {
        /** 需要更多输入 */
        NEED_MORE_INPUT,
        /** 忽略一些输入 */
        SKIP_SOME_INPUT
    }
}
```

### Decodeable 接口

```java
/**
 * 可解码的接口，该接口有两个作用，第一是在调用真正的 decode方法 实现的时候会有一些校验，
 * 判断是否可以解码，并且对解码失败会有一些消息设置；第二个是被用来 message核对用的。
 * 后面看具体的实现会更了解该接口的作用。
 */
public interface Decodeable {

    /** 解码 */
    void decode() throws Exception;
}
```

### Dispatcher 接口

```java
/**
 * 调度器接口，不同的调度器实现，将操作转发到对应的线程池。
 * 其中 dispatch 是线程池的调度方法，需要注意的是：
 * 1、该接口是一个可扩展接口，并且默认实现AllDispatcher，也就是所有消息都派发到线程池，
 *    包括请求，响应，连接事件，断开事件，心跳等；
 * 2、用了 Adaptive注解，也就是按照 URL中的配置来加载实现类，后面两个参数是为了兼容老版本，
 *    如果这是三个key对应的值都为空，就选择AllDispatcher来实现。
 */
@SPI(AllDispatcher.NAME)
public interface Dispatcher {

    /** dispatch the message to threadpool. */
    @Adaptive({Constants.DISPATCHER_KEY, "dispather", "channel.handler"})
    // The last two parameters are reserved for compatibility with the old configuration
    ChannelHandler dispatch(ChannelHandler handler, URL url);
}
```

### Transporter 接口

```java
/**
 * 网络传输接口，需要注意的是：
 * 1、该接口是一个可扩展接口，并且默认实现 NettyTransporter；
 * 2、用了 dubbo SPI扩展机制中的Adaptive注解，加载对应的bind方法，使用url携带的server或者transporter属性值，
 *    加载对应的connect方法，使用url携带的client或者transporter属性值
 */
@SPI("netty")
public interface Transporter {

    /** 绑定一个服务器 */
    @Adaptive({Constants.SERVER_KEY, Constants.TRANSPORTER_KEY})
    Server bind(URL url, ChannelHandler handler) throws RemotingException;

    /**
     * 连接一个服务器，即创建一个客户端
     *
     * @param url     server url 服务器地址
     * @param handler 通道处理器
     * @return client 客户端
     * @throws RemotingException 当连接发生异常时
     */
    @Adaptive({Constants.CLIENT_KEY, Constants.TRANSPORTER_KEY})
    Client connect(URL url, ChannelHandler handler) throws RemotingException;
}
```

### Transporters 类

```java
/**
 * 1、该类用到了设计模式的外观模式，通过该类的包装，隐藏了内部具体的实现细节，降低了程序的复杂度，
 *    也提高了程序的可维护性。比如，它包装了调用各种实现 Transporter接口 的方法，
 *    通过 getTransporter 来获得 Transporter 的实现对象，具体实现哪个实现类，取决于url中携带的配置信息，
 *    如果url中没有相应的配置，则默认选择 @SPI 中的默认值 netty。
 * 2、bind 和 connect方法分别有两个重载方法，其中的操作只是把把字符串的url转化为URL对象。
 * 3、静态代码块中检测了一下jar包是否有重复。
 */
public class Transporters {

    static {
        // 检查重复的 jar包
        Version.checkDuplicate(Transporters.class);
        Version.checkDuplicate(RemotingException.class);
    }

    private Transporters() {
    }

    public static Server bind(String url, ChannelHandler... handler) throws RemotingException {
        return bind(URL.valueOf(url), handler);
    }

    public static Server bind(URL url, ChannelHandler... handlers) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handlers == null || handlers.length == 0) {
            throw new IllegalArgumentException("handlers == null");
        }
        // 创建 handler
        ChannelHandler handler;
        if (handlers.length == 1) {
            handler = handlers[0];
        } else {
            handler = new ChannelHandlerDispatcher(handlers);
        }
        // 调用Transporter的实现类对象的bind方法。
        // 例如实现NettyTransporter，则调用NettyTransporter的connect，并且返回相应的server
        return getTransporter().bind(url, handler);
    }

    public static Client connect(String url, ChannelHandler... handler) throws RemotingException {
        return connect(URL.valueOf(url), handler);
    }

    public static Client connect(URL url, ChannelHandler... handlers) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        // 创建 handler
        ChannelHandler handler;
        if (handlers == null || handlers.length == 0) {
            handler = new ChannelHandlerAdapter();
        } else if (handlers.length == 1) {
            handler = handlers[0];
        } else {
            handler = new ChannelHandlerDispatcher(handlers);
        }
        // 调用Transporter的实现类对象的connect方法。
        // 例如实现NettyTransporter，则调用NettyTransporter的connect，并且返回相应的client
        return getTransporter().connect(url, handler);
    }

    public static Transporter getTransporter() {
        return ExtensionLoader.getExtensionLoader(Transporter.class).getAdaptiveExtension();
    }
}
```

### 远程通信的异常类

RemotingException、ExecutionException 和 TimeoutException 是远程通信的异常类，内容比较简单，这里就简单介绍下 一笔带过咯。

1. RemotingException 继承了 Exception 类，是远程通信的基础异常；
2. ExecutionException 继承了 RemotingException 类，ExecutionException 是远程通信的执行异常；
3. TimeoutException 继承了 RemotingException 类，TimeoutException 是超时异常。

# SPI（Service Provider Interface接口服务）

## JDK 的 SPI 思想

SPI，即 Service Provider Interface。在面向对象的设计里面，模块之间推荐基于接口编程，而不是对实现类进行硬编码，这样做也是为了模块设计的可插拔原则。

比较典型的应用，如 JDBC，Java 定义了一套 JDBC 的接口，但是 Java 本身并不提供对 JDBC 的实现类，而是开发者根据项目实际使用的数据库来选择驱动程序 jar 包，比如 mysql，你就将 mysql-jdbc-connector.jar 引入进来；oracle，你就将 oracle-jdbc-connector.jar 引入进来。在系统跑的时候，碰到你使用 jdbc 的接口，他会在底层使用你引入的那个 jar 中提供的实现类。

## Dubbo 的 SPI 扩展机制原理

dubbo 自己实现了一套 SPI 机制，并对 JDK 的 SPI 进行了改进。

1. JDK 标准的 SPI 只能通过遍历来查找扩展点和实例化，有可能导致一次性加载所有的扩展点，如果不是所有的扩展点都被用到，就会导致资源的浪费。dubbo 每个扩展点都有多种实现，例如：com.alibaba.dubbo.rpc.Protocol 接口有 InjvmProtocol、DubboProtocol、RmiProtocol、HttpProtocol、HessianProtocol 等实现，如果只是用到其中一个实现，可是加载了全部的实现，会导致资源的浪费。
2. 对配置文件中扩展实现的格式的修改，例如，META-INF/dubbo/com.xxx.Protocol 里的 com.foo.XxxProtocol 格式 改为了 xxx = com.foo.XxxProtocol 这种以键值对的形式，这样做的目的是为了让我们更容易的定位到问题。比如，由于第三方库不存在，无法初始化，导致无法加载扩展点（“A”），当用户配置使用 A 时，dubbo 就会报无法加载扩展点的错误，而不是报哪些扩展点的实现加载失败以及错误原因，**这是因为原来的配置格式没有记录扩展名的 id，导致 dubbo 无法抛出较为精准的异常，这会加大排查问题的难度**。所以改成 key-value 的形式来进行配置。
3. dubbo 的 SPI 机制增加了对 IOC、AOP 的支持，一个扩展点可以直接通过 setter 注入到其他扩展点。

下面我们看一下 Dubbo 的 SPI 扩展机制实现的结构目录。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/SPI%E7%BB%84%E4%BB%B6%E7%9B%AE%E5%BD%95%E7%BB%93%E6%9E%84-e0b2e5.png)

### SPI 注解

首先看一下 SPI 注解。在某个接口上加上 @SPI 注解后，表明该接口为可扩展接口。比如，协议扩展接口 Protocol，如果使用者在 &lt;dubbo:protocol />、&lt;dubbo:service />、&lt;dubbo:reference /> 都没有指定 protocol 属性 的话，那么就默认使用 DubboProtocol 作为接口 Protocol 的实现，因为在 Protocol 上有 @SPI("dubbo")注解。而这个 protocol 属性值 或者默认值会被当作该接口的实现类中的一个 key，dubbo 会去 META-INF.dubbo.internal 下的 com.alibaba.dubbo.rpc.Protocol 文件中找该 key 对应的 value，源码如下。

```java
/**
 * 协议接口
 * Protocol 是服务域，它是 Invoker 暴露和引用的主功能入口，它负责 Invoker 的生命周期管理。
 */
@SPI("dubbo")
public interface Protocol {

    /**
     * Get default port when user doesn't config the port.
     */
    int getDefaultPort();

    /**
     * 暴露远程服务：
     * 1. 协议在接收请求时，应记录请求来源方地址信息：RpcContext.getContext().setRemoteAddress();<br>
     * 2. export() 必须是幂等的，也就是暴露同一个 URL 的 Invoker 两次，和暴露一次没有区别。<br>
     * 3. export() 传入的 Invoker 由框架实现并传入，协议不需要关心。<br>
     *
     * @param <T>     服务的类型
     * @param invoker 服务的执行体
     * @return exporter 暴露服务的引用，用于取消暴露
     * @throws RpcException 当暴露服务出错时抛出，比如端口已占用
     */
    @Adaptive
    <T> Exporter<T> export(Invoker<T> invoker) throws RpcException;

    /**
     * 引用远程服务：<br>
     * 1. 当用户调用 refer() 所返回的 Invoker 对象的 invoke() 方法时，协议需相应执行同 URL 远端 export() 传入的 Invoker 对象的 invoke() 方法。<br>
     * 2. refer() 返回的 Invoker 由协议实现，协议通常需要在此 Invoker 中发送远程请求。<br>
     * 3. 当 url 中有设置 check=false 时，连接失败不能抛出异常，并内部自动恢复。<br>
     *
     * @param <T>  服务的类型
     * @param type 服务的类型
     * @param url  远程服务的URL地址
     * @return invoker 服务的本地代理
     * @throws RpcException 当连接服务提供方失败时抛出
     */
    @Adaptive
    <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException;

    /**
     * 释放协议：<br>
     * 1. 取消该协议所有已经暴露和引用的服务。<br>
     * 2. 释放协议所占用的所有资源，比如连接和端口。<br>
     * 3. 协议在释放后，依然能暴露和引用新的服务。<br>
     */
    void destroy();
}

/**
 * 扩展点接口的标识。
 * 扩展点声明配置文件，格式修改。
 * 以Protocol示例，配置文件META-INF/dubbo/com.xxx.Protocol内容：
 * 由
 *      com.foo.XxxProtocol
 *      com.foo.YyyProtocol
 * 改成使用KV格式
 *      xxx=com.foo.XxxProtocol
 *      yyy=com.foo.YyyProtocol
 *
 * 原因：
 * 当扩展点的static字段或方法签名上引用了三方库，
 * 如果三方库不存在，会导致类初始化失败，
 * Extension标识Dubbo就拿不到了，异常信息就和配置信息对应不起来。
 *
 * 比如:
 * Extension("mina")加载失败，
 * 当用户配置使用mina时，就会报找不到扩展点mina，
 * 而不是报加载扩展点失败，等难以定位具体问题的错误。
 *
 * @author william.liangf
 * @author ding.lid
 * @export
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    /**
     * default extension name
     *
     * 默认拓展名
     */
    String value() default "";
}

// 配置文件 com.alibaba.dubbo.rpc.Protocol 中的内容
dubbo=com.alibaba.dubbo.rpc.protocol.dubbo.DubboProtocol
```

value 就是该 Protocol 接口 的实现类 DubboProtocol，这样就做到了 SPI 扩展。

### ExtensionLoader

ExtensionLoader 扩展加载器，这是 dubbo 实现 SPI 扩展机制 的核心，几乎所有实现的逻辑都被封装在 ExtensionLoader 中，其源码如下。

```java
/**
 * 拓展加载器，Dubbo使用的扩展点获取
 * <ul>
 *      <li>自动注入关联扩展点。</li>
 *      <li>自动Wrap上扩展点的Wrap类。</li>
 *      <li>缺省获得的的扩展点是一个Adaptive Instance。</li>
 * </ul>
 *
 * 另外，该类同时是 ExtensionLoader 的管理容器，例如 {@link #EXTENSION_INSTANCES} 、{@link #EXTENSION_INSTANCES} 属性。
 */
@SuppressWarnings("deprecation")
public class ExtensionLoader<T> {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    private static final String DUBBO_DIRECTORY = "META-INF/dubbo/";

    private static final String DUBBO_INTERNAL_DIRECTORY = DUBBO_DIRECTORY + "internal/";

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    // ============================== 静态属性 ==============================

    /**
     * 拓展加载器集合
     *
     * key：拓展接口
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();
    /**
     * 拓展实现类集合
     *
     * key：拓展实现类
     * value：拓展对象。
     *
     * 例如，key 为 Class<AccessLogFilter>
     *      value 为 AccessLogFilter 对象
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    // ============================== 对象属性 ==============================

    /**
     * 拓展接口。
     * 例如，Protocol
     */
    private final Class<?> type;
    /**
     * 对象工厂
     *
     * 用于调用 {@link #injectExtension(Object)} 方法，向拓展对象注入依赖的属性。
     *
     * 例如，StubProxyFactoryWrapper 中有 `Protocol protocol` 属性。
     */
    private final ExtensionFactory objectFactory;
    /**
     * 缓存的拓展名与拓展类的映射。
     *
     * 和 {@link #cachedClasses} 的 KV 对调。
     *
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final ConcurrentMap<Class<?>, String> cachedNames = new ConcurrentHashMap<Class<?>, String>();
    /**
     * 缓存的拓展实现类集合。
     *
     * 不包含如下两种类型：
     *  1. 自适应拓展实现类。例如 AdaptiveExtensionFactory
     *  2. 带唯一参数为拓展接口的构造方法的实现类，或者说拓展 Wrapper 实现类。例如，ProtocolFilterWrapper 。
     *       拓展 Wrapper 实现类，会添加到 {@link #cachedWrapperClasses} 中
     *
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();

    /**
     * 拓展名与 @Activate 的映射
     *
     * 例如，AccessLogFilter。
     *
     * 用于 {@link #getActivateExtension(URL, String)}
     */
    private final Map<String, Activate> cachedActivates = new ConcurrentHashMap<String, Activate>();
    /**
     * 缓存的拓展对象集合
     *
     * key：拓展名
     * value：拓展对象
     *
     * 例如，Protocol 拓展
     *          key：dubbo value：DubboProtocol
     *          key：injvm value：InjvmProtocol
     *
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();
    /**
     * 缓存的自适应( Adaptive )拓展对象
     */
    private final Holder<Object> cachedAdaptiveInstance = new Holder<Object>();
    /**
     * 缓存的自适应拓展对象的类
     *
     * {@link #getAdaptiveExtensionClass()}
     */
    private volatile Class<?> cachedAdaptiveClass = null;
    /**
     * 缓存的默认拓展名
     *
     * 通过 {@link SPI} 注解获得
     */
    private String cachedDefaultName;
    /**
     * 创建 {@link #cachedAdaptiveInstance} 时发生的异常。
     *
     * 发生异常后，不再创建，参见 {@link #createAdaptiveExtension()}
     */
    private volatile Throwable createAdaptiveInstanceError;

    /**
     * 拓展 Wrapper 实现类集合
     *
     * 带唯一参数为拓展接口的构造方法的实现类
     *
     * 通过 {@link #loadExtensionClasses} 加载
     */
    private Set<Class<?>> cachedWrapperClasses;

    /**
     * 拓展名 与 加载对应拓展类发生的异常 的 映射
     *
     * key：拓展名
     * value：异常
     *
     * 在 {@link #loadFile(Map, String)} 时，记录
     */
    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
        objectFactory = (type == ExtensionFactory.class ? null : ExtensionLoader.getExtensionLoader(ExtensionFactory.class).getAdaptiveExtension());
    }

    /**
     * 是否包含 @SPI 注解
     *
     * @param type 类
     * @param <T> 泛型
     * @return 是否包含
     */
    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    /**
     * 根据拓展点的接口，获得拓展加载器
     *
     * @param type 接口
     * @param <T> 泛型
     * @return 加载器
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        // 必须是接口
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        // 必须包含 @SPI 注解
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }

        // 获得接口对应的拓展点加载器
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<T>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    private static ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    public String getExtensionName(T extensionInstance) {
        return getExtensionName(extensionInstance.getClass());
    }

    public String getExtensionName(Class<?> extensionClass) {
        return cachedNames.get(extensionClass);
    }

    public List<T> getActivateExtension(URL url, String key) {
        return getActivateExtension(url, key, null);
    }

    public List<T> getActivateExtension(URL url, String[] values) {
        return getActivateExtension(url, values, null);
    }

    /**
     * 获得符合自动激活条件的拓展对象数组
     */
    public List<T> getActivateExtension(URL url, String key, String group) {
        // 从 Dubbo URL 获得参数值
        String value = url.getParameter(key);
        // 获得符合自动激活条件的拓展对象数组
        return getActivateExtension(url, value == null || value.length() == 0 ? null : Constants.COMMA_SPLIT_PATTERN.split(value), group);
    }

    /**
     * 获得符合自动激活条件的拓展对象数组
     */
    public List<T> getActivateExtension(URL url, String[] values, String group) {
        List<T> exts = new ArrayList<T>();
        List<String> names = values == null ? new ArrayList<String>(0) : Arrays.asList(values);
        // 处理自动激活的拓展对象们
        // 判断不存在配置 `"-name"` 。例如，<dubbo:service filter="-default" /> ，代表移除所有默认过滤器。
        if (!names.contains(Constants.REMOVE_VALUE_PREFIX + Constants.DEFAULT_KEY)) {
            // 获得拓展实现类数组
            getExtensionClasses();
            // 循环
            for (Map.Entry<String, Activate> entry : cachedActivates.entrySet()) {
                String name = entry.getKey();
                Activate activate = entry.getValue();
                if (isMatchGroup(group, activate.group())) { // 匹配分组
                    // 获得拓展对象
                    T ext = getExtension(name);
                    if (!names.contains(name) // 不包含在自定义配置里。如果包含，会在下面的代码处理。
                            && !names.contains(Constants.REMOVE_VALUE_PREFIX + name) // 判断是否配置移除。例如 <dubbo:service filter="-monitor" />，则 MonitorFilter 会被移除
                            && isActive(activate, url)) { // 判断是否激活
                        exts.add(ext);
                    }
                }
            }
            // 排序
            Collections.sort(exts, ActivateComparator.COMPARATOR);
        }
        // 处理自定义配置的拓展对象们。例如在 <dubbo:service filter="demo" /> ，代表需要加入 DemoFilter （这个是笔者自定义的）。
        List<T> usrs = new ArrayList<T>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (!name.startsWith(Constants.REMOVE_VALUE_PREFIX) && !names.contains(Constants.REMOVE_VALUE_PREFIX + name)) { // 判断非移除的
                // 将配置的自定义在自动激活的拓展对象们前面。例如，<dubbo:service filter="demo,default,demo2" /> ，则 DemoFilter 就会放在默认的过滤器前面。
                if (Constants.DEFAULT_KEY.equals(name)) {
                    if (!usrs.isEmpty()) {
                        exts.addAll(0, usrs);
                        usrs.clear();
                    }
                } else {
                    // 获得拓展对象
                    T ext = getExtension(name);
                    usrs.add(ext);
                }
            }
        }
        // 添加到结果集
        if (!usrs.isEmpty()) {
            exts.addAll(usrs);
        }
        return exts;
    }

    /**
     * 匹配分组
     *
     * @param group 过滤的分组条件。若为空，无需过滤
     * @param groups 配置的分组
     * @return 是否匹配
     */
    private boolean isMatchGroup(String group, String[] groups) {
        // 为空，无需过滤
        if (group == null || group.length() == 0) {
            return true;
        }
        // 匹配
        if (groups != null && groups.length > 0) {
            for (String g : groups) {
                if (group.equals(g)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 是否激活，通过 Dubbo URL 中是否存在参数名为 `@Activate.value` ，并且参数值非空。
     *
     * @param activate 自动激活注解
     * @param url Dubbo URL
     * @return 是否
     */
    private boolean isActive(Activate activate, URL url) {
        String[] keys = activate.value();
        if (keys.length == 0) {
            return true;
        }
        for (String key : keys) {
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                if ((k.equals(key) || k.endsWith("." + key))
                        && ConfigUtils.isNotEmpty(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 返回扩展点实例，如果没有指定的扩展点或是还没加载（即实例化）则返回<code>null</code>。
     * 注意：此方法不会触发扩展点的加载。
     * 一般应该调用{@link #getExtension(String)}方法获得扩展，这个方法会触发扩展点加载。
     */
    @SuppressWarnings("unchecked")
    public T getLoadedExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        return (T) holder.get();
    }

    /**
     * 返回已经加载的扩展点的名字。
     * 一般应该调用 getSupportedExtensions() 方法获得扩展，这个方法会返回所有的扩展点。
     */
    public Set<String> getLoadedExtensions() {
        return Collections.unmodifiableSet(new TreeSet<String>(cachedInstances.keySet()));
    }

    /**
     * 返回指定名字的扩展对象。如果指定名字的扩展不存在，则抛异常 {@link IllegalStateException}.
     *
     * @param name 拓展名
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        // 查找 默认的 拓展对象
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        // 从 缓存中 获得对应的拓展对象
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                // 从 缓存中 未获取到，进行创建缓存对象。
                if (instance == null) {
                    instance = createExtension(name);
                    // 设置创建对象到缓存中
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 返回缺省的扩展，如果没有设置则返回<code>null</code>。
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        // 如果为 true ，不能继续调用 `#getExtension(true)` 方法，会形成死循环。
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
                || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    public boolean hasExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        try {
            return getExtensionClass(name) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public Set<String> getSupportedExtensions() {
        Map<String, Class<?>> clazzes = getExtensionClasses();
        return Collections.unmodifiableSet(new TreeSet<String>(clazzes.keySet()));
    }

    /**
     * 返回缺省的扩展点名，如果没有设置缺省则返回<code>null</code>。
     */
    public String getDefaultExtensionName() {
        getExtensionClasses();
        return cachedDefaultName;
    }

    /**
     * 编程方式添加新扩展点。
     *
     * @param name  扩展点名
     * @param clazz 扩展点类
     * @throws IllegalStateException 要添加扩展点名已经存在。
     */
    public void addExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " already existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
        } else {
            if (cachedAdaptiveClass != null) {
                throw new IllegalStateException("Adaptive Extension already existed(Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
        }
    }

    /**
     * 编程方式添加替换已有扩展点。
     *
     * @param name  扩展点名
     * @param clazz 扩展点类
     * @throws IllegalStateException 要添加扩展点名已经存在。
     * @deprecated 不推荐应用使用，一般只在测试时可以使用
     */
    @Deprecated
    public void replaceExtension(String name, Class<?> clazz) {
        getExtensionClasses(); // load classes

        if (!type.isAssignableFrom(clazz)) {
            throw new IllegalStateException("Input type " +
                    clazz + "not implement Extension " + type);
        }
        if (clazz.isInterface()) {
            throw new IllegalStateException("Input type " +
                    clazz + "can not be interface!");
        }

        if (!clazz.isAnnotationPresent(Adaptive.class)) {
            if (StringUtils.isBlank(name)) {
                throw new IllegalStateException("Extension name is blank (Extension " + type + ")!");
            }
            if (!cachedClasses.get().containsKey(name)) {
                throw new IllegalStateException("Extension name " +
                        name + " not existed(Extension " + type + ")!");
            }

            cachedNames.put(clazz, name);
            cachedClasses.get().put(name, clazz);
            cachedInstances.remove(name);
        } else {
            if (cachedAdaptiveClass == null) {
                throw new IllegalStateException("Adaptive Extension not existed(Extension " + type + ")!");
            }

            cachedAdaptiveClass = clazz;
            cachedAdaptiveInstance.set(null);
        }
    }

    /**
     * 获得自适应拓展对象
     *
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    public T getAdaptiveExtension() {
        // 从缓存中，获得自适应拓展对象
        Object instance = cachedAdaptiveInstance.get();
        if (instance == null) {
            // 若之前未创建报错，
            if (createAdaptiveInstanceError == null) {
                synchronized (cachedAdaptiveInstance) {
                    instance = cachedAdaptiveInstance.get();
                    if (instance == null) {
                        try {
                            // 创建自适应拓展对象
                            instance = createAdaptiveExtension();
                            // 设置到缓存
                            cachedAdaptiveInstance.set(instance);
                        } catch (Throwable t) {
                            // 记录异常
                            createAdaptiveInstanceError = t;
                            throw new IllegalStateException("fail to create adaptive instance: " + t.toString(), t);
                        }
                    }
                }
            // 若之前创建报错，则抛出异常 IllegalStateException
            } else {
                throw new IllegalStateException("fail to create adaptive instance: " + createAdaptiveInstanceError.toString(), createAdaptiveInstanceError);
            }
        }
        return (T) instance;
    }

    /**
     * 获得拓展名不存在时的异常
     *
     * @param name 拓展名
     * @return 异常
     */
    private IllegalStateException findException(String name) {
        // 在 `#loadFile(...)` 方法中，加载时，发生异常
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        // 生成不存在该拓展类实现的异常。
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);
        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }
            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(StringUtils.toString(entry.getValue()));
        }
        return new IllegalStateException(buf.toString());
    }

    /**
     * 创建拓展名的拓展对象，并缓存。
     *
     * @param name 拓展名
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        // 获得拓展名对应的拓展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name); // 抛出异常
        }
        try {
            // 从缓存中，获得拓展对象。
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                // 当缓存不存在时，创建拓展对象，并添加到缓存中。
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            // 注入依赖的属性
            injectExtension(instance);
            // 创建 Wrapper 拓展对象
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && !wrapperClasses.isEmpty()) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    /**
     * 注入依赖的属性
     *
     * @param instance 拓展对象
     * @return 拓展对象
     */
    private T injectExtension(T instance) {
        try {
            if (objectFactory != null) {
                for (Method method : instance.getClass().getMethods()) {
                    if (method.getName().startsWith("set")
                            && method.getParameterTypes().length == 1
                            && Modifier.isPublic(method.getModifiers())) { // setting && public 方法
                        // 获得属性的类型
                        Class<?> pt = method.getParameterTypes()[0];
                        try {
                            // 获得属性
                            String property = method.getName().length() > 3 ? method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4) : "";
                            // 获得属性值
                            Object object = objectFactory.getExtension(pt, property);
                            // 设置属性值
                            if (object != null) {
                                method.invoke(instance, object);
                            }
                        } catch (Exception e) {
                            logger.error("fail to inject via method " + method.getName()
                                    + " of interface " + type.getName() + ": " + e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return instance;
    }

    private Class<?> getExtensionClass(String name) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (name == null)
            throw new IllegalArgumentException("Extension name == null");
        // 获得拓展实现类
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null)
            throw new IllegalStateException("No such extension \"" + name + "\" for " + type.getName() + "!");
        return clazz;
    }

    /**
     * 获得拓展实现类数组
     *
     * @return 拓展实现类数组
     */
    private Map<String, Class<?>> getExtensionClasses() {
        // 从缓存中，获得拓展实现类数组
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    // 从配置文件中，加载拓展实现类数组
                    classes = loadExtensionClasses();
                    // 设置到缓存中
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 加载拓展实现类数组
     *
     * @return 拓展实现类数组
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        // 通过 @SPI 注解，获得默认的拓展实现类名
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if ((value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) cachedDefaultName = names[0];
            }
        }

        // 从配置文件中，加载拓展实现类数组
        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadFile(extensionClasses, DUBBO_INTERNAL_DIRECTORY);
        loadFile(extensionClasses, DUBBO_DIRECTORY);
        loadFile(extensionClasses, SERVICES_DIRECTORY);
        return extensionClasses;
    }

    /**
     * 从一个配置文件中，加载拓展实现类数组。
     *
     * @param extensionClasses 拓展类名数组
     * @param dir 文件名
     */
    private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
        // 完整的文件名
        String fileName = dir + type.getName();
        try {
            Enumeration<java.net.URL> urls;
            // 获得文件名对应的所有文件数组
            ClassLoader classLoader = findClassLoader();
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            // 遍历文件数组
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    java.net.URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                // 跳过当前被注释掉的情况，例如 #spring=xxxxxxxxx
                                final int ci = line.indexOf('#');
                                if (ci >= 0) line = line.substring(0, ci);
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        // 拆分，key=value 的配置格式
                                        String name = null;
                                        int i = line.indexOf('=');
                                        if (i > 0) {
                                            name = line.substring(0, i).trim();
                                            line = line.substring(i + 1).trim();
                                        }
                                        if (line.length() > 0) {
                                            // 判断拓展实现，是否实现拓展接口
                                            Class<?> clazz = Class.forName(line, true, classLoader);
                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }
                                            // 缓存自适应拓展对象的类到 `cachedAdaptiveClass`
                                            if (clazz.isAnnotationPresent(Adaptive.class)) {
                                                if (cachedAdaptiveClass == null) {
                                                    cachedAdaptiveClass = clazz;
                                                } else if (!cachedAdaptiveClass.equals(clazz)) {
                                                    throw new IllegalStateException("More than 1 adaptive class found: "
                                                            + cachedAdaptiveClass.getClass().getName()
                                                            + ", " + clazz.getClass().getName());
                                                }
                                            } else {
                                                // 缓存拓展 Wrapper 实现类到 `cachedWrapperClasses`
                                                try {
                                                    clazz.getConstructor(type);
                                                    Set<Class<?>> wrappers = cachedWrapperClasses;
                                                    if (wrappers == null) {
                                                        cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
                                                        wrappers = cachedWrapperClasses;
                                                    }
                                                    wrappers.add(clazz);
                                                // 缓存拓展实现类到 `extensionClasses`
                                                } catch (NoSuchMethodException e) {
                                                    clazz.getConstructor();
                                                    // 未配置拓展名，自动生成。例如，DemoFilter 为 demo 。主要用于兼容 Java SPI 的配置。
                                                    if (name == null || name.length() == 0) {
                                                        name = findAnnotationName(clazz);
                                                        if (name == null || name.length() == 0) {
                                                            if (clazz.getSimpleName().length() > type.getSimpleName().length()
                                                                    && clazz.getSimpleName().endsWith(type.getSimpleName())) {
                                                                name = clazz.getSimpleName().substring(0, clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
                                                            } else {
                                                                throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + url);
                                                            }
                                                        }
                                                    }
                                                    // 获得拓展名，可以是数组，有多个拓展名。
                                                    String[] names = NAME_SEPARATOR.split(name);
                                                    if (names != null && names.length > 0) {
                                                        // 缓存 @Activate 到 `cachedActivates` 。
                                                        Activate activate = clazz.getAnnotation(Activate.class);
                                                        if (activate != null) {
                                                            cachedActivates.put(names[0], activate);
                                                        }
                                                        for (String n : names) {
                                                            // 缓存到 `cachedNames`
                                                            if (!cachedNames.containsKey(clazz)) {
                                                                cachedNames.put(clazz, n);
                                                            }
                                                            // 缓存拓展实现类到 `extensionClasses`
                                                            Class<?> c = extensionClasses.get(n);
                                                            if (c == null) {
                                                                extensionClasses.put(n, clazz);
                                                            } else if (c != clazz) {
                                                                throw new IllegalStateException("Duplicate extension " + type.getName() + " name " + n + " on " + c.getName() + " and " + clazz.getName());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Throwable t) {
                                        // 发生异常，记录到异常集合
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        } finally {
                            reader.close();
                        }
                    } catch (Throwable t) {
                        logger.error("Exception when load extension class(interface: " +
                                type + ", class file: " + url + ") in " + url, t);
                    }
                } // end of while urls
            }
        } catch (Throwable t) {
            logger.error("Exception when load extension class(interface: " +
                    type + ", description file: " + fileName + ").", t);
        }
    }

    @SuppressWarnings("deprecation")
    private String findAnnotationName(Class<?> clazz) {
        com.alibaba.dubbo.common.Extension extension = clazz.getAnnotation(com.alibaba.dubbo.common.Extension.class);
        if (extension == null) {
            String name = clazz.getSimpleName();
            if (name.endsWith(type.getSimpleName())) {
                name = name.substring(0, name.length() - type.getSimpleName().length());
            }
            return name.toLowerCase();
        }
        return extension.value();
    }

    /**
     * 创建自适应拓展对象
     *
     * @return 拓展对象
     */
    @SuppressWarnings("unchecked")
    private T createAdaptiveExtension() {
        try {
            return injectExtension((T) getAdaptiveExtensionClass().newInstance());
        } catch (Exception e) {
            throw new IllegalStateException("Can not create adaptive extension " + type + ", cause: " + e.getMessage(), e);
        }
    }

    /**
     * @return 自适应拓展类
     */
    private Class<?> getAdaptiveExtensionClass() {
        getExtensionClasses();
        if (cachedAdaptiveClass != null) {
            return cachedAdaptiveClass;
        }
        return cachedAdaptiveClass = createAdaptiveExtensionClass();
    }

    /**
     * 自动生成自适应拓展的代码实现，并编译后返回该类。
     *
     * @return 类
     */
    private Class<?> createAdaptiveExtensionClass() {
        // 自动生成自适应拓展的代码实现的字符串
        String code = createAdaptiveExtensionClassCode();
        // 编译代码，并返回该类
        ClassLoader classLoader = findClassLoader();
        com.alibaba.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
        return compiler.compile(code, classLoader);
    }

    /**
     * 自动生成自适应拓展的代码实现的字符串
     *
     * @return 代码字符串
     */
    private String createAdaptiveExtensionClassCode() {
        StringBuilder codeBuidler = new StringBuilder();
        // 遍历方法数组，判断有 @Adaptive 注解
        Method[] methods = type.getMethods();
        boolean hasAdaptiveAnnotation = false;
        for (Method m : methods) {
            if (m.isAnnotationPresent(Adaptive.class)) {
                hasAdaptiveAnnotation = true;
                break;
            }
        }
        // no need to generate adaptive class since there's no adaptive method found.
        // 完全没有Adaptive方法，则不需要生成Adaptive类
        if (!hasAdaptiveAnnotation)
            throw new IllegalStateException("No adaptive method on extension " + type.getName() + ", refuse to create the adaptive class!");

        // 生成代码：package 和 import
        codeBuidler.append("package " + type.getPackage().getName() + ";");
        codeBuidler.append("\nimport " + ExtensionLoader.class.getName() + ";");
        // 生成代码：类名
        codeBuidler.append("\npublic class " + type.getSimpleName() + "$Adaptive" + " implements " + type.getCanonicalName() + " {");

        // 循环方法
        for (Method method : methods) {
            Class<?> rt = method.getReturnType(); // 返回类型
            Class<?>[] pts = method.getParameterTypes(); // 参数类型数组
            Class<?>[] ets = method.getExceptionTypes(); // 异常类型数组

            Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
            StringBuilder code = new StringBuilder(512); // 方法体的代码
            // 非 @Adaptive 注解，生成代码：生成的方法为直接抛出异常。因为，非自适应的接口不应该被调用。
            if (adaptiveAnnotation == null) {
                code.append("throw new UnsupportedOperationException(\"method ")
                        .append(method.toString()).append(" of interface ")
                        .append(type.getName()).append(" is not adaptive method!\");");
            // @Adaptive 注解，生成方法体的代码
            } else {
                // 寻找 Dubbo URL 参数的位置
                int urlTypeIndex = -1;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].equals(URL.class)) {
                        urlTypeIndex = i;
                        break;
                    }
                }
                // found parameter in URL type
                // 有类型为URL的参数，生成代码：生成校验 URL 非空的代码
                if (urlTypeIndex != -1) {
                    // Null Point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"url == null\");",
                            urlTypeIndex);
                    code.append(s);

                    s = String.format("\n%s url = arg%d;", URL.class.getName(), urlTypeIndex);
                    code.append(s);
                }
                // did not find parameter in URL type
                // 参数没有URL类型
                else {
                    String attribMethod = null;

                    // find URL getter method
                    // 找到参数的URL属性 。例如，Invoker 有 `#getURL()` 方法。
                    LBL_PTS:
                    for (int i = 0; i < pts.length; ++i) {
                        Method[] ms = pts[i].getMethods();
                        for (Method m : ms) {
                            String name = m.getName();
                            if ((name.startsWith("get") || name.length() > 3)
                                    && Modifier.isPublic(m.getModifiers())
                                    && !Modifier.isStatic(m.getModifiers())
                                    && m.getParameterTypes().length == 0
                                    && m.getReturnType() == URL.class) { // pubic && getting 方法
                                urlTypeIndex = i;
                                attribMethod = name;
                                break LBL_PTS;
                            }
                        }
                    }
                    // 未找到，抛出异常。
                    if (attribMethod == null) {
                        throw new IllegalStateException("fail to create adaptive class for interface " + type.getName()
                                + ": not found url parameter or url attribute in parameters of method " + method.getName());
                    }

                    // 生成代码：校验 URL 非空
                    // Null point check
                    String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");",
                            urlTypeIndex, pts[urlTypeIndex].getName());
                    code.append(s);
                    s = String.format("\nif (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");",
                            urlTypeIndex, attribMethod, pts[urlTypeIndex].getName(), attribMethod);
                    code.append(s);

                    // 生成 `URL url = arg%d.%s();` 的代码
                    s = String.format("%s url = arg%d.%s();", URL.class.getName(), urlTypeIndex, attribMethod);
                    code.append(s);
                }

                String[] value = adaptiveAnnotation.value();
                // value is not set, use the value generated from class name as the key
                // 没有设置Key，则使用“扩展点接口名的点分隔 作为Key
                if (value.length == 0) {
                    char[] charArray = type.getSimpleName().toCharArray();
                    StringBuilder sb = new StringBuilder(128);
                    for (int i = 0; i < charArray.length; i++) {
                        if (Character.isUpperCase(charArray[i])) {
                            if (i != 0) {
                                sb.append(".");
                            }
                            sb.append(Character.toLowerCase(charArray[i]));
                        } else {
                            sb.append(charArray[i]);
                        }
                    }
                    value = new String[]{sb.toString()};
                }

                // 判断是否有 Invocation 参数
                boolean hasInvocation = false;
                for (int i = 0; i < pts.length; ++i) {
                    if (pts[i].getName().equals("com.alibaba.dubbo.rpc.Invocation")) {
                        // 生成代码：校验 Invocation 非空
                        // Null Point check
                        String s = String.format("\nif (arg%d == null) throw new IllegalArgumentException(\"invocation == null\");", i);
                        code.append(s);

                        // 生成代码：获得方法名
                        s = String.format("\nString methodName = arg%d.getMethodName();", i);
                        code.append(s);

                        // 标记有 Invocation 参数
                        hasInvocation = true;
                        break;
                    }
                }

                // 默认拓展名
                String defaultExtName = cachedDefaultName;
                // 获得最终拓展名的代码字符串，例如：
                // 【简单】1. url.getParameter("proxy", "javassist")
                // 【复杂】2. url.getParameter(key1, url.getParameter(key2, defaultExtName))
                String getNameCode = null;
                for (int i = value.length - 1; i >= 0; --i) { // 倒序的原因，因为是顺序获取参数，参见【复杂】2. 的例子
                    if (i == value.length - 1) {
                        if (null != defaultExtName) {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation) // 当【有】 Invocation 参数时，使用 `URL#getMethodParameter()` 方法。
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                                else // 当【非】 Invocation 参数时，使用 `URL#getParameter()` 方法。
                                    getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                            else // 当属性名是 "protocol" ，使用 `URL#getProtocl()` 方法获取。
                                getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                        } else {
                            if (!"protocol".equals(value[i]))
                                if (hasInvocation)
                                    getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName); // 此处的 defaultExtName ，可以去掉的。
                                else
                                    getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                            else
                                getNameCode = "url.getProtocol()";
                        }
                    } else {
                        if (!"protocol".equals(value[i]))
                            if (hasInvocation)
                                getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                            else
                                getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                        else
                            getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                    }
                }

                // 生成代码：获取参数的代码。例如：String extName = url.getParameter("proxy", "javassist");
                code.append("\nString extName = ").append(getNameCode).append(";");
                // check extName == null?
                String s = String.format("\nif(extName == null) " +
                                "throw new IllegalStateException(\"Fail to get extension(%s) name from url(\" + url.toString() + \") use keys(%s)\");",
                        type.getName(), Arrays.toString(value));
                code.append(s);

                // 生成代码：拓展对象，调用方法。例如
                // `com.alibaba.dubbo.rpc.ProxyFactory extension = (com.alibaba.dubbo.rpc.ProxyFactory) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.rpc.ProxyFactory.class)
                //                                                                                                           .getExtension(extName);` 。
                s = String.format("\n%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);",
                        type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
                code.append(s);

                // return statement
                if (!rt.equals(void.class)) {
                    code.append("\nreturn ");
                }

                s = String.format("extension.%s(", method.getName());
                code.append(s);
                for (int i = 0; i < pts.length; i++) {
                    if (i != 0)
                        code.append(", ");
                    code.append("arg").append(i);
                }
                code.append(");");
            }

            // 生成方法
            codeBuidler.append("\npublic " + rt.getCanonicalName() + " " + method.getName() + "(");
            for (int i = 0; i < pts.length; i++) {
                if (i > 0) {
                    codeBuidler.append(", ");
                }
                codeBuidler.append(pts[i].getCanonicalName());
                codeBuidler.append(" ");
                codeBuidler.append("arg" + i);
            }
            codeBuidler.append(")");
            if (ets.length > 0) {
                codeBuidler.append(" throws ");  // 异常
                for (int i = 0; i < ets.length; i++) {
                    if (i > 0) {
                        codeBuidler.append(", ");
                    }
                    codeBuidler.append(ets[i].getCanonicalName());
                }
            }
            codeBuidler.append(" {");
            codeBuidler.append(code.toString());
            codeBuidler.append("\n}");
        }

        // 生成类末尾的 `}`
        codeBuidler.append("\n}");

        // 调试，打印生成的代码
        if (logger.isDebugEnabled()) {
            logger.debug(codeBuidler.toString());
        }
        return codeBuidler.toString();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[" + type.getName() + "]";
    }
}
```

