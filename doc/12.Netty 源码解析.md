# Netty 源码解析



# Netty的高级特性

**作为一个高性能的 NIO 通信框架**

作为一个高性能的 NIO 通信框架，Netty 被广泛应用于大数据处理、互联网消息中间件、游戏和金融行业等。大多数应用场景对底层的通信框架都有很高的性能要求，作为综合性能最高的 NIO 框架 之一，Netty 可以完全满足不同领域对高性能通信的需求。本章我们将从架构层对 Netty 的高性能设计和关键代码实现进行剖析，看 Netty 是如何支撑高性能网络通信的。

## RPC 调用性能模型分析

### 传统 RPC 调用性能差的原因

**一、网络传输方式问题。**  
传统的 RPC 框架 或者基于 RMI 等方式的 远程过程调用 采用了同步阻塞 I/O，当客户端的并发压力或者网络时延增大之后，同步阻塞 I/O 会由于频繁的 wait 导致 I/O 线程 经常性的阻塞，由于线程无法高效的工作，I/O 处理能力自然下降。

采用 BIO 通信模型 的服务端，通常由一个独立的 Acceptor 线程 负责监听客户端的连接，接收到客户端连接之后，为其创建一个新的线程处理请求消息，处理完成之后，返回应答消息给客户端，线程销毁，这就是典型的 “ 一请求，一应答 ” 模型。该架构最大的问题就是不具备弹性伸缩能力，当并发访问量增加后，服务端的线程个数和并发访问数成线性正比，由于线程是 Java 虛拟机 非常宝贵的系统资源，当线程数膨胀之后，系统的性能急剧下降，随着并发量的继续增加，可能会发生句柄溢出、线程堆栈溢出等问题，并导致服务器最终宕机。

**二、序列化性能差。**  
Java 序列化 存在如下几个典型问题：

1. Java 序列化机制是 Java 内部的一 种对象编解码技术，无法跨语言使用。例如对于异构系统之间的对接，Java 序列化 后的码流需要能够通过其他语言反序列化成原始对象，这很难支持。
2. 相比于其他开源的序列化框架，Java 序列化 后的码流太大，无论是网络传输还是持久化到磁盘，都会导致额外的资源占用。
3. 序列化性能差，资源占用率高 ( 主要是 CPU 资源占用高 )。

**三、线程模型问题。**  
由于采用 同步阻塞 I/O，这会导致每个 TCP 连接 都占用 1 个线程，由于线程资源是 JVM 虚拟机 非常宝贵的资源，当 I/O 读写阻塞导致线程无法及时释放时，会导致系统性能急剧下降，严重的甚至会导致虚拟机无法创建新的线程。

### IO 通信性能三原则

尽管影响 I/O 通信性能 的因素非常多，但是从架构层面看主要有三个要素。

1. 传输：用什么样的通道将数据发送给对方。可以选择 BIO、NIO 或者 AIO，I/O 模型 在很大程度上决定了通信的性能；
2. 协议：采用什么样的通信协议，HTTP 等公有协议或者内部私有协议。协议的选择不同，性能也不同。相比于公有协议，内部私有协议的性能通常可以被设计得更优；
3. 线程模型：数据报如何读取？读取之后的编解码在哪个线程进行，编解码后的消息如何派发，Reactor 线程模型的不同，对性能的影响也非常大。

## 异步非阻塞通信

在 I/O 编程 过程中，当需要同时处理多个客户端接入请求时，可以利用多线程或者 I/O 多路复用技术 进行处理。I/O 多路复用技术 通过把多个 I/O 的阻塞复用到同一个 select 的阻塞上，从而使得系统在单线程的情况下可以同时处理多个客户端请求。与传统的多线程 / 多进程模型比，I/O 多路复用 的最大优势是系统开销小，系统不需要创建新的额外进程或者线程，也不需要维护这些进程和线程的运行，降低了系统的维护工作量，节省了系统资源。

JDK1.4 提供了对非阻塞 I/O 的支持，JDK1.5 使用 epoll 替代了传统的 select / poll，极大地提升了 NIO 通信 的性能。

与 Socket 和 ServerSocket 类相对应，NIO 也提供了 SocketChannel 和 ServerSocketChannel 两种不同的套接字通道实现。这两种新增的通道都支持阻塞和非阻塞两种模式。阻塞模式使用非常简单，但是性能和可靠性都不好，非阻塞模式则正好相反。开发人员一般可以根据自己的需要来选择合适的模式，一般来说，低负载、低并发的应用程序可以选择 同步阻塞 I/O 以降低编程复杂度。但是对于高负载、高并发的网络应用，需要使用 NIO 的非阻塞模式进行开发。

Netty 的 I/O 线程 NioEventLoop 由于聚合了 多路复用器 Selector，可以同时并发处理成百上千个客户端 SocketChannel。由于读写操作都是非阻塞的，这就可以充分提升 I/O 线程 的运行效率，避免由频繁的 I/O 阻塞 导致的线程挂起。另外，由于 Netty 采用了异步通信模式，一个 I/O 线程 可以并发处理 N 个客户端连接和读写操作，这从根本上解决了传统 同步阻塞 I/O “ 一连接，一线程 ” 模型，架构的性能、弹性伸缩能力和可靠性都得到了极大的提升。

## 高效的 Reactor 线程模型

常用的 Reactor 线程模型 有三种，分别如下。

1. Reactor 单线程模型；
2. Reactor 多线程模型；
3. 主从 Reactor 多线程模型。

Reactor 单线程模型，指的是所有的 I/O 操作 都在同一个 NIO 线程 上面完成，NIO 线程 的职责如下：

1. 作为 NIO 服务端，接收客户端的 TCP 连接；
2. 作为 NIO 客户端，向服务端发起 TCP 连接；
3. 读取通信对端的请求或者应答消息；
4. 向通信对端发送消息请求或者应答消息。

由于 Reactor 模式 使用的是 异步非阻塞 I/O，所有的 I/O 操作 都不会导致阻塞，理论上一个线程可以独立处理所有 I/O 相关的操作。从架构层面看，一个 NIO 线程 确实可以完成其承担的职责。例如，通过 Acceptor 接收客户端的 TCP 连接请求消息，链路建立成功之后，通过 Dispatch 将对应的 ByteBuffer 派发到指定的 Handler 上进行消息解码。用户 Handler 可以通过 NIO 线程 将消息发送给客户端。

对于一些小容量应用场景，可以使用单线程模型，但是对于高负载、大并发的应用却不合适，主要原因如下。

1. 一个 NIO 线程 同时处理成百上千的链路，性能上无法支撑。即便 NIO 线程 的 CPU 负荷 达到 100%，也无法满足海量消息的编码，解码、读取和发送；
2. 当 NIO 线程 负载过重之后，处理速度将变慢，这会导致大量客户端连接超时，超时之后往往会进行重发，这更加重了 NIO 线程 的负载，最终会导致大量消息积压和处理超时，NIO 线程 会成为系统的性能瓶颈；
3. 可靠性问题。一旦 NIO 线程 意外跑飞，或者进入死循环，会导致整个系统通信模块不可用，不能接收和处理外部消息，造成节点故障。

为了解决这些问题，演进出了 Reactor 多线程模型，下面我们看一下 Reactor 多线程模型。

Rector 多线程模型 与单线程模型最大的区别就是有一组 NIO 线程 处理 I/O 操作，它的特点如下。

1. 有一个专门的 NIO 线程 —— Acceptor 线程 用于监听服务端口，接收客户端的 TCP 连接请求；
2. 网络 IO 操作 —— 读、写等由一个 NIO 线程池 负责，线程池可以采用标准的 JDK 线程池 实现，它包含一个任务队列和 N 个可用的线程，由这些 NIO 线程 负责消息的读取、解码、编码和发送；
3. 1 个 NIO 线程 可以同时处理 N 条链路，但是 1 个链路只对应 1 个 NIO 线程，以防止发生并发操作问题。

在绝大多数场景下，Reactor 多线程模型 都可以满足性能需求，但是，在极特殊应用场景中，一个 NIO 线程 负责监听和处理所有的客户端连接可能会存在性能问题。例如百万客户端并发连接，或者服务端需要对客户端的握手消息进行安全认证，认证本身非常损耗性能。在这类场景下，单独一个 Acceptor 线程 可能会存在性能不足问题，为了解决性能问题，产生了第三种 Reactor 线程模型 —— 主从 Reactor 多线程模型。

主从 Reactor 线程模型 的特点是，服务端用于接收客户端连接的不再是个单线程的连接处理 Acceptor，而是一个独立的 Acceptor 线程池。Acceptor 接收到客户端 TCP 连接请求 处理完成后 ( 可能包含接入认证等 )，将新创建的 SocketChannel 注册到 I/O 处理线程池 的某个 I/O 线程 上，由它负责 SocketChannel 的读写和编解码工作。Acceptor 线程池 只用于客户端的登录、握手和安全认证，一旦链路建立成功，就将链路注册到 I/O 处理线程池的 I/O 线程 上，每个 I/O 线程 可以同时监听 N 个链路，对链路产生的 IO 事件 进行相应的 消息读取、解码、编码及消息发送等操作。

利用 主从 Reactor 线程模型，可以解决 1 个 Acceptor 线程 无法有效处理所有客户端连接的性能问题。因此，Netty 官方 也推荐使用该线程模型。

事实上，Netty 的线程模型并非固定不变，通过在启动辅助类中创建不同的 EventLoopGroup 实例 并进行适当的参数配置，就可以支持上述三种 Reactor 线程模型。可以根据业务场景的性能诉求，选择不同的线程模型。

Netty 单线程模型 服务端代码示例如下。

```java
    EventLoopGroup reactor = new NioEventLoopGroup(1);
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(reactor, reactor)
            .channel(NioServerSocketChannel.class)
            ......
```

Netty 多线程模型 代码示例如下。.

```java
    EventLoopGroup acceptor = new NioEventLoopGroup(1);
    EventLoopGroup ioGroup = new NioEventLoopGroup();
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(acceptor, ioGroup)
            .channel(NioServerSocketChannel.class)
            ......
```

Netty 主从多线程模型 代码示例如下。

```java
    EventLoopGroup acceptorGroup = new NioEventLoopGroup();
    EventLoopGroup ioGroup = new NioEventLoopGroup();
    ServerBootstrap bootstrap = new ServerBootstrap();
    bootstrap.group(acceptorGroup, ioGroup)
            .channel(NioServerSocketChannel.class)
            ......
```

## 无锁化的串行设计

在大多数场景下，并行多线程处理可以提升系统的并发性能。但是，如果对于共享资源的并发访问处理不当，会带来严重的锁竞争，这最终会导致性能的下降。为了尽可能地避免锁竞争带来的性能损耗，可以通过串行化设计，即消息的处理尽可能在同一个线程内完成，期间不进行线程切换，这样就避免了多线程竞争和同步锁。

为了尽可能提升性能，Netty 对消息的处理 采用了串行无锁化设计，在 I/O 线程 内部进行串行操作，避免多线程竞争导致的性能下降。Netty 的串行化设计工作原理图如下图所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/Netty%E4%B8%B2%E8%A1%8C%E5%8C%96%E8%AE%BE%E8%AE%A1%E5%B7%A5%E4%BD%9C%E5%8E%9F%E7%90%86-aa7c9d.png)

Netty 的 NioEventLoop 读取到消息之后，直接调用 ChannelPipeline 的 fireChannelRead(Object msg)，只要用户不主动切换线程，一直会由 NioEventLoop 调用到 用户的 Handler，期间不进行线程切换。这种串行化处理方式避免了多线程操作导致的锁的竞争，从性能角度看是最优的。

## 零拷贝

Netty 的 “ 零拷贝 ” 主要体现在如下三个方面。

第一种情况。Netty 的接收和发送 ByteBuffer 采用 堆外直接内存 (DIRECT BUFFERS) 进行 Socket 读写，不需要进行字节缓冲区的二次拷贝。如果使用传统的 堆内存(HEAP BUFFERS) 进行 Socket 读写，JVM 会将 堆内存 Buffer 拷贝一份到 直接内存 中，然后才写入 Socket。相比于堆外直接内存，消息在发送过程中多了一次缓冲区的内存拷贝。

下面我们继续看第二种 “ 零拷贝 ” 的实现 CompositeByteBuf，它对外将多个 ByteBuf 封装成一个 ByteBuf，对外提供统一封装后的 ByteBuf 接口。CompositeByteBuf 实际就是个 ByteBuf 的装饰器，它将多个 ByteBuf 组合成一个集合，然后对外提供统一的 ByteBuf 接口，添加 ByteBuf，不需要做内存拷贝。

第三种 “ 零拷贝 ” 就是文件传输，Netty 文件传输类 DefaultFileRegion 通过 transferTo()方法 将文件发送到目标 Channel 中。很多操作系统直接将文件缓冲区的内容发送到目标 Channel 中，而不需要通过循环拷贝的方式，这是一种更加高效的传输方式，提升了传输性能，降低了 CPU 和内存占用，实现了文件传输的 “ 零拷贝 ” 。

## 内存池

随着 JVM 虚拟机 和 JIT 即时编译技术 的发展，对象的分配和回收是个非常轻量级的工作。但是对于 缓冲区 Buffer，情况却稍有不同，特别是对于堆外直接内存的分配和回收，是一件耗时的操作。为了尽量重用缓冲区，Netty 提供了基于内存池的缓冲区重用机制。ByteBuf 的子类中提供了多种 PooledByteBuf 的实现，基于这些实现 Netty 提供了多种内存管理策略，通过在启动辅助类中配置相关参数，可以实现差异化的定制。

## Socket 与 SocketChannel

网络由下往上分为 物理层、数据链路层、网络层、传输层和应用层。IP 协议 对应于网络层，TCP 协议 对应于传输层，而 HTTP 协议 对应于应用层，三者从本质上来说没有可比性，Socket 则是对 TCP/IP 协议 的封装和应用 (程序员层面上)。也可以说，TPC/IP 协议 是传输层协议，主要解决数据如何在网络中传输，而 HTTP 是应用层协议，主要解决如何包装数据。Socket 是对 TCP/IP 协议 的封装，Socket 本身并不是协议，而是一个 调用接口(API)。 通过 Socket，我们才能使用 TCP/IP 协议。

### 一、利用 Socket 建立网络连接的步骤

建立 Socket 连接 至少需要一对套接字，其中一个运行于客户端，称为 clientSocket ，另一个运行于服务器端，称为 serverSocket 。套接字之间的连接过程分为三个步骤：服务器监听，客户端请求，连接确认。

1. 服务器监听：服务器端套接字并不定位具体的客户端套接字，而是处于等待连接的状态，实时监控网络状态，等待客户端的连接请求。
2. 客户端请求：指客户端的套接字提出连接请求，要连接的目标是服务器端的套接字。为此，客户端的套接字必须首先描述它要连接的服务器的套接字，指出服务器端套接字的地址和端口号，然后就向服务器端套接字提出连接请求。
3. 连接确认：当服务器端套接字监听到或者说接收到客户端套接字的连接请求时，就响应客户端套接字的请求，建立一个新的线程，把服务器端套接字的描述发给 客户端，一旦客户端确认了此描述，双方就正式建立连接。而服务器端套接字继续处于监听状态，继续接收其他客户端套接字的连接请求。

### 二、HTTP 连接 的特点

HTTP 协议 是 Web 联网 的基础，也是手机联网常用的协议之一，HTTP 协议 是建立在 TCP 协议 之上的一种应用。HTTP 连接 最显著的特点是客户端发送的每次请求 都需要服务器回送响应，在请求结束后，会主动释放连接。从建立连接到关闭连接的过程称为 “一次连接”。

### 三、TCP 和 UDP 的区别

1. TCP 是面向连接的，虽然说网络的不安全不稳定特性决定了多少次握手都不能保证连接的可靠性，但 TCP 的三次握手在很大程度上 保证了连接的可靠性。而 UDP 不是面向连接的，UDP 传送数据前并不与对方建立连接，对接收到的数据也不发送确认信号，发送端不知道数据是否会正确接收，当然也不用重发，所以说 UDP 是无连接的、不可靠的一种数据传输协议。
2. 也正由于 1 所说的特点，使得 UDP 的开销更小，数据传输速率更高，因为不必进行收发数据的确认，所以 UDP 的实时性更好。

### 四、Socket 与 SocketChannel 有什么区别

Socket、SocketChannel 二者的实质都是一样的，都是为了实现客户端与服务器端的连接而存在的，但是在使用上却有很大的区别。具体如下：

1. 所属包不同。Socket 在 java.net 包 中，而 SocketChannel 在 java.nio 包 中。
2. 异步方式不同。从包的不同，我们大体可以推断出他们主要的区别：Socket 是阻塞连接，SocketChannel 可以设置为非阻塞连接。使用 ServerSocket 与 Socket 的搭配，服务端 Socket 往往要为每一个 客户端 Socket 分配一个线程，而每一个线程都有可能处于长时间的阻塞状态中。过多的线程也会影响服务器的性能。而使用 SocketChannel 与 ServerSocketChannel 的搭配可以非阻塞通信，这样使得服务器端只需要一个线程就能处理所有 客户端 Socket 的请求。
3. 性能不同。一般来说，高并发场景下，使用 SocketChannel 与 ServerSocketChannel 的搭配会有更好的性能。
4. 使用方式不同。Socket、ServerSocket 类 可以传入不同参数直接实例化对象并绑定 IP 和 端口。而 SocketChannel、ServerSocketChannel 类 需要借助 Selector 类。

下面是 SocketChannel 方式 需要用到的几个核心类：

ServerSocketChannel：ServerSocket 的替代类, 支持阻塞通信与非阻塞通信。

SocketChannel：Socket 的替代类, 支持阻塞通信与非阻塞通信。

Selector：为 ServerSocketChannel 监控接收客户端连接就绪事件, 为 SocketChannel 监控连接服务器读就绪和写就绪事件。

SelectionKey：代表 ServerSocketChannel 及 SocketChannel 向 Selector 注册事件的句柄。当一个 SelectionKey 对象 位于 Selector 对象 的 selected-keys 集合 中时，就表示与这个 SelectionKey 对象 相关的事件发生了。在 SelectionKey 类 中有如下几个静态常量：

- SelectionKey.OP_ACCEPT，客户端连接就绪事件，等于监听 serverSocket.accept()，返回一个 socket。
- SelectionKey.OP_CONNECT，准备连接服务器就绪，跟上面类似，只不过是对于 socket 的，相当于监听了 socket.connect()。
- SelectionKey.OP_READ，读就绪事件, 表示输入流中已经有了可读数据, 可以执行读操作了。
- SelectionKey.OP_WRITE，写就绪事件, 表示可以执行写操作了。

# Netty的架构设计

本博文用于重点分析 Netty 的逻辑架构及关键的架构质量属性，希望有助于大家从 Netty 的架构设计中汲取营养，设计出高性能、高可靠
性和可扩展的程序。

## Netty 的三层架构设计

Netty 采用了典型的三层网络架构进行设计和开发，其逻辑架构图如下所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/25/Netty%E9%80%BB%E8%BE%91%E6%9E%B6%E6%9E%84%E5%9B%BE-bdf2c7.png)

### 通信调度层 Reactor

它由一系列辅助类完成，包括 Reactor 线程 NioEventLoop 及其父类，NioSocketChannel / NioServerSocketChannel 及其父类，Buffer 组件，Unsafe 组件 等。该层的主要职责就是**监听网络的读写和连接操作**，负责**将网络层的数据读取到内存缓冲区**，然后触发各种网络事件，例如连接创建、连接激活、读事件、写事件等，将这些事件触发到 PipeLine 中，由 PipeLine 管理的责任链来进行后续的处理。

### 责任链层 Pipeline

它负责上述的各种网络事件 在责任链中的有序传播，同时负责动态地编排责任链。责任链可以选择监听和处理自己关心的事件，它可以拦截处理事件，以及向前向后传播事件。不同应用的 Handler 节点 的功能也不同，通常情况下，往往会开发 编解码 Hanlder 用于消息的编解码，可以将外部的协议消息转换成 内部的 POJO 对象，这样上层业务则只需要关心处理业务逻辑即可，不需要感知底层的协议差异和线程模型差异，实现了架构层面的分层隔离。

### 业务逻辑编排层 Service ChannelHandler

业务逻辑编排层通常有两类：一类是纯粹的业务逻辑编排，还有一类是其他的应用层协议插件，用于特定协议相关的会话和链路管理。例如，CMPP 协议，用于管理和中国移动短信系统的对接。

架构的不同层面，需要关心和处理的对象都不同，通常情况下，对于业务开发者，只需要关心责任链的拦截和 业务 Handler 的编排。因为应用层协议栈往往是开发一次，到处运行，所以实际上对于业务开发者来说，只需要关心服务层的业务逻辑开发即可。各种应用协议以插件的形式提供，只有协议开发人员需要关注协议插件，对于其他业务开发人员来说，只需关心业务逻辑定制。这种分层的架构设计理念实现了 NIO 框架 各层之间的解耦，便于上层业务协议栈的开发和业务逻辑的定制。

正是由于 Netty 的分层架构设计非常合理，基于 Netty 的各种应用服务器和协议栈开发才能够如雨后春笋般得到快速发展。

## 关键的架构质量属性

### 性能

影响最终产品的性能因素非常多，其中软件因素如下。

- 架构不合理导致的性能问题；
- 编码实现不合理导致的性能问题，例如，锁没用好导致的性能瓶颈。

硬件因素如下。

- 服务器硬件配置太低导致的性能问题；
- 带宽、磁盘的 IOPS 等限制导致的 IO 操作 性能差；
- 测试环境被共用导致被测试的软件产品受到影响。

尽管影响产品性能的因素非常多，但是架构的性能模型合理与否对性能的影响非常大。如果一个产品的架构设计得不好，无论开发如何努力，都很难开发出一个高性能、高可用的软件产品。

“性能是设计出来的，而不是测试出来的”。下面我们看看 Netty 的架构设计是如何实现高性能的。

1. 采用非阻塞的 NIO 类库，基于 Reactor 模式实现，解决了传统 同步阻塞 IO 模式 下一个服务端无法平滑地处理线性增长的客户端的问题。
2. TCP 接收和发送缓冲区**使用直接内存代替堆内存，避免了内存复制**，提升了 IO 读取和写入的性能。
3. 支持通过内存池的方式循环利用 ByteBuffer，避免了频繁创建和销毁 ByteBuffer 带来的性能损耗。
4. 可配置的 IO 线程数、TCP 参数 等，为不同的用户场景提供定制化的调优参数，满足不同的性能场景。
5. 采用环形数组缓冲区实现无锁化并发编程，代替传统的线程安全容器或者锁。
6. 合理地使用线程安全容器、原子类等，提升系统的并发处理能力。
7. 关键资源的处理使用单线程串行化的方式，避免多线程并发访问带来的锁竞争和额外的 CPU 资源消耗问题。
8. 通过引用计数器及时地申请释放不再被引用的对象，细粒度的内存管理降低了 GC 的频率，减少了频繁 GC 带来的延时和 CPU 损耗。

### 可靠性

作为一个高性能的异步通信框架，架构的可靠性是大家选择的另一个重要依据。下面我们看一下 Netty 架构 的可靠性设计。

**1、链路有效性检测**  
由于长连接不需要每次发送消息都创建链路，也不需要在消息交互完成时关闭链路，因此相对于短连接性能更高。对于长连接，一旦链路建立成功便一直维系双方之间的链路，直到系统退出。

为了保证长连接的链路有效性，往往需要通过心跳机制周期性地进行链路检测。使用周期性心跳的原因是：在系统空闲时，例如凌晨，往往没有业务消息。如果此时链路被防火墙 Hang 住，或者遭遇网络闪断、网络单通等，通信双方无法识别出这类链路异常。等到第二天业务高峰期到来时，瞬间的海量业务冲击会导致消息积压无法发送给对方，由于链路的重建需要时间，这期间业务会大量失败 (集群或者分布式组网情况会好一些)。为了解决这个问题，需要周期性的 “心跳检测” 对链路进行有效性检查，一旦发生问题，可以及时关闭链路，重建 TCP 连接。

当有业务消息时，无须心跳检测，可以由业务消息进行链路可用性检测。所以心跳消息往往是在链路空闲时发送的。为了支持心跳机制，Netty 提供了如下两种链路空闲检测机制。

- 读空闲超时机制：当经过 连续的周期 T 没有消息可读时，触发 超时 Handler，用户可以基于 该读空闲超时 Handler 发送心跳消息，进行链路检测，如果连续 N 个周期 仍然没有读取到心跳消息，可以主动关闭这条链路。
- 写空闲超时机制：当经过 连续的周期 T 没有消息要发送时，触发 超时 Handler，用户可以基于 该写空闲超时 Handler 发送心跳消息，进行链路检测，如果连续 N 个周期 仍然没有接收到对方的心跳消息，可以主动关闭这条链路。

为了满足不同用户场景的心跳定制，Netty 提供了空闲状态检测事件通知机制，用户可以订阅：空闲超时事件、读空闲超时机制、写空闲超时事件，在接收到对应的空闲事件之后，灵活地进行定制。

**2、内存保护机制**
Netty 提供多种机制对内存进行保护，包括以下几个方面。

- 通过对象引用计数器对 Netty 的 ByteBuffer 等内置对象进行细粒度的内存申请和释放，对非法的对象引用进行检测和保护。
- 通过内存池来重用 ByteBuffer，节省内存。
- 可设置的内存容量上限，包括 ByteBuffer、线程池线程数等。

### 可定制性

Netty 的可定制性主要体现在以下几点。

- 责任链模式：ChannelPipeline 基于责任链模式开发，便于业务逻辑的拦截、定制和扩展。
- 基于接口的开发：关键的类库都提供了接口或者抽象类，如果 Netty 自身的实现无法满足用户的需求，可以由用户自定义实现相关接口。
- 提供了大量工厂类，通过重载这些工厂类可以按需创建出用户实现的对象。
- 提供了大量的系统参数供用户按需设置，增强系统的场景定制性。

### 可扩展性

基于 Netty 的 基本 NIO 框架，可以方便地进行应用层协议定制，例如，HTTP 协议栈、Thrift 协议栈、FTP 协议栈 等。这些扩展不需要修改 Netty 的源码，直接基于 Netty 的二进制类库即可实现协议的扩展和定制。目前，业界存在大量的基于 Netty 框架 开发的协议，例如基于 Netty 的 HTTP 协议、Dubbo 协议、RocketMQ 内部私有协议 等。

# **Netty 技术细节源码分析**

## ByteBuf的内存泄漏原因与检测原理

该文所涉及的 netty 源码版本为 4.1.6。

### Netty 中的 ByteBuf 为什么会发生内存泄漏

在 Netty 中，ByetBuf 并不是只采用可达性分析来对 ByteBuf 底层的 `byte[]` 数组来进行垃圾回收，而同时采用引用计数法来进行回收，来保证堆外内存的准确时机的释放。  

在每个 ByteBuf 中都维护着一个 refCnt 用来对 ByteBuf 的被引用数进行记录，当 ByteBuf 的 `retain()` 方法被调用时，将会增加 refCnt 的计数，而其 `release()` 方法被调用时将会减少其被引用数计数。

```java
private boolean release0(int decrement) {
    for (;;) {
        int refCnt = this.refCnt;
        if (refCnt < decrement) {
            throw new IllegalReferenceCountException(refCnt, -decrement);
        }
        if (refCntUpdater.compareAndSet(this, refCnt, refCnt - decrement)) {
            if (refCnt == decrement) {
                deallocate();
                return true;
            }
            return false;
        }
    }
}
```

当调用了 ByteBuf 的 `release()` 方法的时候，最后在上方的 `release0()` 方法中将会为 ByteBuf 的引用计数减一，当引用计数归于 0 的时候，将会调用 `deallocate()` 方法对其对应的底层存储数组进行释放(在池化的 ByteBuf 中，在 `deallocate()` 方法里会把该 ByteBuf 的 `byte[]` 回收到底层内存池中，以确保 `byte[]` 可以重复利用)。 

由于 Netty 中的 ByteBuf 并不是随着申请之后会马上使其引用计数归 0 而进行释放，往往在这两个操作之间还有许多操作，如果在这其中如果发生异常抛出导致引用没有及时释放，在使用池化 ByetBuffer 的情况下内存泄漏的问题就会产生。

当采用了池化的 ByteBuffer 的时候，比如 PooledHeapByteBuf 和 PooledDirectByteBuf，其 `deallocate()` 方法一共主要分为两个步骤。

```java
@Override
protected final void deallocate() {
	if (handle >= 0) {
		final long handle = this.handle;
		this.handle = -1;
		memory = null;
		chunk.arena.free(chunk, handle, maxLength);
		recycle();
	}
}
```

- 将其底层的 `byte[]` 通过 `free()` 方法回收到内存池中等待下一次使用。
- 通过 `recycle()` 方法将其本身回收到对象池中等待下一次使用。  
  关键在第一步的内存回收到池中，如果其引用计数未能在 ByteBuf 对象被回收之前归 0，将会导致其底层占用 `byte[]` 无法回收到内存池 PoolArena 中，导致该部分无法被重复利用，下一次将会申请新的内存进行操作，从而产生内存泄漏。  
  而非池化的 ByteBuffer 即使引用计数没有在对象被回收的时候被归 0，因为其使用的是单独一块 `byte[]` 内存，因此也会随着 java 对象被回收使得底层 `byte[]` 被释放（由 JDK 的 Cleaner 来保证）。

### Netty 进行内存泄漏检测的原理

在 Netty 对于 ByteBuf 的检测中，一共包含 4 个级别。

```java
if (level.ordinal() < Level.PARANOID.ordinal()) {
	if (leakCheckCnt ++ % samplingInterval == 0) {
		reportLeak(level);
		return new DefaultResourceLeak(obj);
	} else {
		return null;
	}
}
```

以默认的 SIMPLE 级别为例，在这个级别下，Netty 将会根据以 ByteBuf 创建的序列号与 113 进行取模来判断是否需要进行内存泄漏的检测追踪。当取模成功的时候，将会为这个 ByteBuf 产生一个对应的 DefaultResourceLeak 对象，DefaultResourceLeak 是一个 PhantomReference 虚引用的子类，并有其对应的 ReferenceQueue。之后通过 SimpleLeakAwareByteBuf 类来将被追踪的 ByteBuf 和 DefaultResourceLeak 包装起来。

```java
@Override
public boolean release(int decrement) {
	boolean deallocated = super.release(decrement);
	if (deallocated) {
		leak.close();
	}
	return deallocated;
}
```

在包装类中，如果该 ByteBuf 成功 deallocated 释放掉了其持有的 byte[]数组将会调用 DefaultResourceLeak 的 `close()` 方法来已通知当前 ByteBuf 已经释放了其持有的内存。  
正是这个虚引用使得该 DefaultResourceLeak 对象被回收的时候将会被放入到与这个虚引用所对应的 ReferenceQueue 中。

```java
DefaultResourceLeak ref = (DefaultResourceLeak) refQueue.poll();
if (ref == null) {
	break;
}

ref.clear();

if (!ref.close()) {
	continue;
}

String records = ref.toString();
if (reportedLeaks.putIfAbsent(records, Boolean.TRUE) == null) {
	if (records.isEmpty()) {
		logger.error("LEAK: {}.release() was not called before it's garbage-collected. " +
				"Enable advanced leak reporting to find out where the leak occurred. " +
				"To enable advanced leak reporting, " +
				"specify the JVM option '-D{}={}' or call {}.setLevel()",
				resourceType, PROP_LEVEL, Level.ADVANCED.name().toLowerCase(), simpleClassName(this));
	} else {
		logger.error(
				"LEAK: {}.release() was not called before it's garbage-collected.{}",
				resourceType, records);
	}
}
```

Netty 会在下一次 ByteBuf 的采样中通过 reportLeak()方法将 ReferenceQueue 中的 DefaultResourceLeak 取出并判断其对应的 ByteBuf 是否已经在其回收前调用过其 `close()` 方法，如果没有，显然在池化 ByteBuf 的场景下内存泄漏已经产生，将会以 ERROR 日志的方式进行日志打印。

以上内容可以结合 JVM 堆外内存的资料进行阅读。

## Netty 的 FastThreadLocal 源码解析

该文中涉及到的 Netty 源码版本为 4.1.6。

### Netty 的 FastThreadLocal 是什么

> A special variant of ThreadLocal that yields higher access performance when accessed from a FastThreadLocalThread.  
> Internally, a FastThreadLocal uses a constant index in an array, instead of using hash code and hash table, to look for a variable. Although seemingly very subtle, it yields slight performance advantage over using a hash table, and it is useful when accessed frequently.  
> To take advantage of this thread-local variable, your thread must be a FastThreadLocalThread or its subtype. By default, all threads created by DefaultThreadFactory are FastThreadLocalThread due to this reason.  
> Note that the fast path is only possible on threads that extend FastThreadLocalThread, because it requires a special field to store the necessary state. An access by any other kind of thread falls back to a regular ThreadLocal.

以上是 Netty 官方文档中关于 FastThreadLocal 的介绍。

简而言之，FastThreadLocal 是在 ThreadLocal 实现上的一种变种，相比 ThreadLocal 内部通过将自身 hash 的方式在 hashTable 上定位需要的变量存储位置，FastThreadLocal 选择在数组上的一个固定的常量位置来存放线程本地变量，这样的操作看起来并没有太大区别，但是相比 ThreadLocal 的确体现了性能上的优势，尤其是在读操作频繁的场景下。

### 如何使用 FastThreadLocal

如果想要得到 FastThreadLocal 的速度优势，必须通过 FastThreadLocalThread 或者其子类的线程，才可以使用，因为这个原因，Netty 的 DefaultThreadFactory，其内部默认线程工厂的 newThread()方法就是直接初始化一个 FastThreadLocalThread ，以便期望在 ThreadLocal 的操作中，得到其性能上带来的优势。

```java
protected Thread newThread(Runnable r, String name) {
    return new FastThreadLocalThread(threadGroup, r, name);
}
```

### FastThreadLocal 的源码实现

#### FastThreadLocal 被访问的入口

当需要用到 FastThreadLocal 的时候，想必和 jdk 原生的 ThreadLocal 的 api 类似，都是通过初始化一个新的 FastThreadLocal 之后，通过其 set()方法初始化并放入一个变量作为线程本地变量存储。

```java
public final void set(V value) {
    if (value != InternalThreadLocalMap.UNSET) {
        set(InternalThreadLocalMap.get(), value);
    } else {
        remove();
    }
}
```

因此，在 FastThreadLocal 的 set()方法中，可以看到，存储本地线程变量的数据结构是一个 InternalThreadLocalMap。

```java
private InternalThreadLocalMap threadLocalMap;
```

在 FastThreadLocalThread 中，因为本身 threadLocalMap 就是其中的一个成员，能够快速得到返回。而其他线程实现，就将面临没有这个成员的尴尬，Netty 也给出了相应的兼容。

```java
public static InternalThreadLocalMap get() {
    Thread thread = Thread.currentThread();
    if (thread instanceof FastThreadLocalThread) {
        return fastGet((FastThreadLocalThread) thread);
    } else {
        return slowGet();
    }
}
```

InternalThreadLocalMap 的 get()方法中，当前线程如果是 FastThreadLocalThread 或是其子类的实现，变直接返回其 InternalThreadLocalMap 进行操作，但对于不属于上述条件的线程，Netty 通过 slowGet()的方式，也将返回一个 InternalThreadLocalMap。

```java
private static InternalThreadLocalMap slowGet() {
    ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = UnpaddedInternalThreadLocalMap.slowThreadLocalMap;
    InternalThreadLocalMap ret = slowThreadLocalMap.get();
    if (ret == null) {
        ret = new InternalThreadLocalMap();
        slowThreadLocalMap.set(ret);
    }
    return ret;
}
```

在 slowGet()方法中，当前线程对应的 InternalThreadLocalMap 会通过原生 jdk 下 ThreadLocal 的方式存储并通过 ThreadLocal 返回，因此，在这个场景下，使用的还是 jdk 原生的 ThreadLocal，但是只占用了原生 ThreadLocal 下的 Entry[]数组的一个位置，具体的变量还是存放在专门为 FastThreadLocal 服务的 InternalThreadLocalMap 中。  
在此，随着 InternalThreadLocalMap 的得到并返回，针对 FastThreadLocal 的 get 和 set 操作，也将变为操作 InternalThreadLocalMap 来达到目的，FastThreadLocal 性能优越的原因，也在 InternalThreadLocalMap 当中。

#### InternalThreadLocalMap 的内部构造

```java
static final AtomicInteger nextIndex = new AtomicInteger();

Object[] indexedVariables;
```

InternalThreadlocalMap 主要由以上两个成员组成，其中 indexedVariables 作为一个 Object[]数组，直接用来存放 FastThreadLocal 对应的 value，每个 FastThreadLocal 对象都会在相应的线程的 ThreadLocalMap 中被分配到对应的 index，而这里的具体下标，则由以上的 nextIndex 成员在每个 FastThreadLocal 初始化的时候分配。

```java
private final int index;

public FastThreadLocal() {
    index = InternalThreadLocalMap.nextVariableIndex();
}
```

每个 FastThreadLocal 在构造方法的过程中，都会通过 InternalThreadlocalMap 的 nextVariableIndex()返回 nextIndex 自加后的结果作为其在 InternalThreadlocalMap 上的下标。后续该 FastThreadLocal 在操作变量的时候可以直接通过该 index 定位到 Object[]数组上的位置。

```java
private static final int variablesToRemoveIndex = InternalThreadLocalMap.nextVariableIndex();
```

而数组上的下标有一个特殊位，一般在其首位也就是 0 的位置，这个位置在 FastThreadLocal 类被加载的时候作为静态变量被设置。在这个位置上，存放的是一个 FastThreadLocal 对象集合，每个存放到 InternalThreadlocalMap 中的 FastThreadLocal 都会被保存在首位的集合中。

```java
public static final Object UNSET = new Object();
```

另外，为了具体区分保存的变量是 null 还是不存在当前变量，InternalThreadLocalMap 中定义了一个为 NULL 的成员变量，以便区分上述情况，在一开始，InternalThreadLocalMap 中的 indexedVariables 数组都是 NULL。

#### FastThreadLocal 的 set()方法的源码分析

相比 FastThreadLocal 的 set 操作，get 方法的过程与逻辑都要简单的多，因此此处主要以其 set 方法为主。

```java
public final void set(V value) {
    if (value != InternalThreadLocalMap.UNSET) {
        set(InternalThreadLocalMap.get(), value);
    } else {
        remove();
    }
}

public final void set(InternalThreadLocalMap threadLocalMap, V value) {
    if (value != InternalThreadLocalMap.UNSET) {
        if (threadLocalMap.setIndexedVariable(index, value)) {
            addToVariablesToRemove(threadLocalMap, this);
        }
    } else {
        remove(threadLocalMap);
    }
}
```

在其 set()方法中，首先会判断 set 的值是否是 InternalThreadLocalMap 中的 NULL 对象来判断是 set 操作还是 remove 操作，如果不是，会通过 InternalThreadLocalMap.get()方法获取当前线程对应的 InternalThreadLocalMap，获取的过程在前文已经描述过。
之后的主要流程主要分为两步：

- 调用 InternalThreadLocalMap 的 setIndexedVariable()方法，将该 FastThreadLocal 成员在构造方法中获得到的 InternalThreadLocalMap 上的下标作为入参传入。

```java
public boolean setIndexedVariable(int index, Object value) {
    Object[] lookup = indexedVariables;
    if (index < lookup.length) {
        Object oldValue = lookup[index];
        lookup[index] = value;
        return oldValue == UNSET;
    } else {
        expandIndexedVariableTableAndSet(index, value);
        return true;
    }
}
```

在 InternalThreadLocalMap 的 setIndexedVariable()方法过程中，set 的过程并不复杂，找到对应的下标，并将对应的值放到 InternalThreadLocalMap 数组下标对应的位置上即宣告结束。但是，因为 FastThreadLocal 在构造过程中虽然预先获得了对应的下标，但是实际上的数组大小可能完全还没有达到相应的大小，就要在此处通过 expandIndexedVariableTableAndSet()方法进行扩容，由于是数组的缘故，只需要扩容后将原来的值复制过去，并将剩余的值用 NULL 对象填满即可。

- 如果上一步 set 成功，通过 addToVariablesToRemove()方法将该 FastThreadLocal 对象放入到 InternalThreadLocalMap 的数组中的首位集合中。在这个集合中，对于 FastThreadLocal 是一个强引用。

这样，对于 FastThreadLocal 的一次 set 操作即宣告结束。

### 相比 ThreadLocal，FastThreadLocal 到底快在哪里

- FastThreadLocal 在具体的定位的过程中，只需要根据在构造方法里获取得到的具体下标就可以定位到具体的数组位置进行变量的存取，而在 jdk 原生的 ThreadLocal 中，具体位置的下标获取不仅需要计算 ThreadLocal 的 hash 值，并需要在 hashTable 上根据 key 定位的结果，一旦定位之后的结果上已经存在其他 ThreadLocal 的变量，那么则是通过线性探测法，在 hashTable 上寻找下一个位置进行，相比 FastThreadLocal 定位的过程要复杂的多。
- FastThreadLocal 由于采取数组的方式，当面对扩容的时候，只需要将原数组中的内容复制过去，并用 NULL 对象填满剩余位置即可，而在 ThreadLocal 中，由于 hashTable 的缘故，在扩容后还需要进行一轮 rehash，在这过程中，仍旧存在 hash 冲突的可能。
- 在 FastThreadLocal 中，遍历当前线程的所有本地变量，只需要将数组首位的集合即可，不需要遍历数组上的每一个位置。
- 在原生的 ThreadLocal 中，由于可能存在 ThreadLocal 被回收，但是当前线程仍旧存活的情况导致 ThreadLocal 对应的本地变量内存泄漏的问题，因此在 ThreadLocal 的每次操作后，都会进行启发式的内存泄漏检测，防止这样的问题产生，但也在每次操作后花费了额外的开销。而在 FastThreadLocal 的场景下，由于数组首位的 FastThreadLocal 集合中保持着所有 FastThreadLocal 对象的引用，因此当外部的 FastThreadLocal 的引用被置为 null，该 FastThreadLocal 对象仍旧保持着这个集合的引用，不会被回收掉，只需要在线程当前业务操作后，手动调用 FastThreadLocal 的 removeAll()方法，将会遍历数组首位集合，回收掉所有 FastThreadLocal 的变量，避免内存泄漏的产生，也减少了原生 ThreadLocal 的启发式检测开销。

```java
private static final class DefaultRunnableDecorator implements Runnable {

    private final Runnable r;

    DefaultRunnableDecorator(Runnable r) {
        this.r = r;
    }

    @Override
    public void run() {
        try {
            r.run();
        } finally {
            FastThreadLocal.removeAll();
        }
    }
}
```

在 Netty 的 DefaultThreadFactory 中，每个线程在执行为任务后都会调用 FastThreadLocal 的 removeAll()方法。

# HashedWheelTimer&schedule

## 前言

前段时间在给自己的玩具项目设计的时候就遇到了一个场景需要定时任务，于是就趁机了解了目前主流的一些定时任务方案，比如下面这些：

- Timer（halo 博客源码中用到了）
- ScheduledExecutorService
- ThreadPoolTaskScheduler（基于 ScheduledExecutorService）
- Netty 的 schedule（用到了 PriorityQueue）
- Netty 的 HashedWheelTimer（时间轮）
- Kafka 的 TimingWheel（层级时间轮）

还有一些分布式的定时任务：

- Quartz
- xxl-job（我实习公司就在用这个）
- ...

因为我玩具项目实现业务 ACK 的方案就打算用 HashedWheelTimer，所以本节核心是分析 HashedWheelTimer，另外会提下它与 schedule 的区别，其它定时任务实现原理就请自动 Google 吧。

> Netty Version：4.1.42

## HashedWheelTimer 实现图示

![HashedWheelTimer实现图示.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595752125587-a7fc6d.png)

大致有个理解就行，关于蓝色格子中的数字，其实就是剩余时钟轮数，这里听不懂也没关系，等后面看到源码解释就懂了~~（大概）~~。

## HashedWheelTimer 简答使用例子

这里顺便列出 schedule 的使用方式，下面是某个 Handler 中的代码：

```java
@Override
public void handlerAdded(final ChannelHandlerContext ctx) {
    // 定时任务
    ScheduledFuture<?> hello_world = ctx.executor().schedule(() -> {
        ctx.channel().write("hello world");
    }, 3, TimeUnit.SECONDS);

    // 构造一个 Timer 实例，同样只执行一次
    Timer timer = new HashedWheelTimer();
    Timeout timeout1 = timer.newTimeout(timeout -> System.out.println("5s 后执行该任务"), 5, TimeUnit.SECONDS);

    // 取消任务
    timeout1.cancel();
}
```

## HashedWheelTimer 源码

### 继承关系、方法

![继承关系&方法.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595751597062-b3c6e9.png)

### 构造函数、属性

请记住这些属性的是干啥用的，后面会频繁遇到：
`io.netty.util.HashedWheelTimer#HashedWheelTimer(java.util.concurrent.ThreadFactory, long, java.util.concurrent.TimeUnit, int, boolean, long)`

```java
public HashedWheelTimer(
        ThreadFactory threadFactory,
        long tickDuration, TimeUnit unit, int ticksPerWheel, boolean leakDetection,
        long maxPendingTimeouts) {

    if (threadFactory == null) {
        throw new NullPointerException("threadFactory");
    }
    if (unit == null) {
        throw new NullPointerException("unit");
    }
    if (tickDuration <= 0) {
        throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
    }
    if (ticksPerWheel <= 0) {
        throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
    }

    // 初始化时间轮，数组长度必须是2的幂次方，便于取模
    // Normalize ticksPerWheel to power of two and initialize the wheel.
    wheel = createWheel(ticksPerWheel);
    // 用于取模运算, tick & mask
    mask = wheel.length - 1;

    // Convert tickDuration to nanos.
    // 毫秒转纳秒
    long duration = unit.toNanos(tickDuration);

    // Prevent overflow.
    // 防止溢出
    if (duration >= Long.MAX_VALUE / wheel.length) {
        throw new IllegalArgumentException(String.format(
                "tickDuration: %d (expected: 0 < tickDuration in nanos < %d",
                tickDuration, Long.MAX_VALUE / wheel.length));
    }

    // 时间刻度设置太小，自动设置为MILLISECOND_NANOS， 并弹出警告日志
    if (duration < MILLISECOND_NANOS) {
        logger.warn("Configured tickDuration {} smaller then {}, using 1ms.",
                    tickDuration, MILLISECOND_NANOS);
        this.tickDuration = MILLISECOND_NANOS;
    } else {
        this.tickDuration = duration;
    }

    // 初始化工作线程，注意这里还没有启动
    // 另外需要注意的是本类中的startTime是在worker第一次启动之后才初始化的，
    // 跟io.netty.util.concurrent.ScheduledFutureTask.START_TIME在类加载的时候初始化是不一样的
    workerThread = threadFactory.newThread(worker);

    // 用来跟踪内存问题的，本节忽略，主讲定时任务的实现
    leak = leakDetection || !workerThread.isDaemon() ? leakDetector.track(this) : null;

    // 最大允许任务等待数
    this.maxPendingTimeouts = maxPendingTimeouts;

    // HashedWheelTimer实例如果超过64个就会弹出警告，告诉你HashedWheelTimer不是这样用的，单个应用只需要一个单例即可
    if (INSTANCE_COUNTER.incrementAndGet() > INSTANCE_COUNT_LIMIT &&
        WARNED_TOO_MANY_INSTANCES.compareAndSet(false, true)) {
        reportTooManyInstances();
    }
}
```

### 添加定时任务

添加定时任务其实就是 Timer 接口的 newTimeOut 方法：
`io.netty.util.HashedWheelTimer#newTimeout`

```java
@Override
public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
    if (task == null) {
        throw new NullPointerException("task");
    }
    if (unit == null) {
        throw new NullPointerException("unit");
    }

    // 获取当前等待任务数
    long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();

    // 如果超出最大等待
    if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
        pendingTimeouts.decrementAndGet();
        throw new RejectedExecutionException("Number of pending timeouts ("
            + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending "
            + "timeouts (" + maxPendingTimeouts + ")");
    }

    // 尝试启动workerThread，startTime=0时会startTimeInitialized.await()，最终就是调用Worker的run方法
    start();

    // Add the timeout to the timeout queue which will be processed on the next tick.
    // During processing all the queued HashedWheelTimeouts will be added to the correct HashedWheelBucket.
    // 这条算式我们可以稍微改下，更容易理解些：
    // long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
    //                                 ↓
    // long deadline = unit.toNanos(delay) - (System.nanoTime() - startTime)
    // 我感觉这样更容易理解些，含义为： 距离任务执行的剩余时间 = 任务截止时间 - (当前时间 - 任务对象初始化时间)
    long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;

    // Guard against overflow.
    if (delay > 0 && deadline < 0) {
        deadline = Long.MAX_VALUE;
    }

    // 构建任务对象
    HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
    // 将任务对象添加到mpsc队列中，mpsc是多生产者单消费者的队列模型，另外mpscQueue是无锁队列，靠的CAS实现的。
    timeouts.add(timeout);
    // 返回任务对象，该对象可以用于取消任务、获取任务信息等
    return timeout;
}
```

这里我们再跟进 start 方法看看：
`io.netty.util.HashedWheelTimer#start`

```java
public void start() {
    switch (WORKER_STATE_UPDATER.get(this)) {
        case WORKER_STATE_INIT:
            if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                // 一半会来到这里，最终就是调用到Worker的run方法
                workerThread.start();
            }
            break;
        case WORKER_STATE_STARTED:
            break;
        case WORKER_STATE_SHUTDOWN:
            throw new IllegalStateException("cannot be started once stopped");
        default:
            throw new Error("Invalid WorkerState");
    }

    // Wait until the startTime is initialized by the worker.
    while (startTime == 0) {
        try {
            // 如果startTime异常，Worker的run方法会处理这种异常，重新唤醒
            startTimeInitialized.await();
        } catch (InterruptedException ignore) {
            // Ignore - it will be ready very soon.
        }
    }
}
```

### 定时任务执行

定时任务的执行逻辑其实就在 Worker 的 run 方法中：
`io.netty.util.HashedWheelTimer.Worker#run`

```java
// 用于处理取消的任务
private final Set<Timeout> unprocessedTimeouts = new HashSet<Timeout>();

// 时钟指针转动的次数
private long tick;

@Override
public void run() {
    // Initialize the startTime.
    startTime = System.nanoTime();
    if (startTime == 0) {
        // We use 0 as an indicator for the uninitialized value here, so make sure it's not 0 when initialized.
        startTime = 1;
    }

    // Notify the other threads waiting for the initialization at start().
    // 之前如果startTime=0，就会进入await状态，这里就要唤醒它
    startTimeInitialized.countDown();

    do {
        /*
            * 等待到下一次 tick 时如果没有时间延迟返回tickDuration * (tick + 1);
            * 如果延迟了则不空转，立马返回“当前时间”
            * 这个“当前时间”是什么呢？比如时钟指针原本第三次 tick 是在300ms，但是由于前面的任务阻塞了50ms，导致进来的时候已经是350ms了
            * 那么这里的返回值就会变成350ms，至于返回值变成350ms会怎么样？貌似也没有怎么样，就是不等待马上执行罢了
            */
        final long deadline = waitForNextTick();
        if (deadline > 0) {
            // 与运算取模，取出数组桶的坐标，相信这个没少见过了
            int idx = (int) (tick & mask);
            // 前面说过HashedWheelTimeout是可以取消任务的，其实就是在这里取消的
            processCancelledTasks();
            // 在时间轮中取出“指针指向的块”
            HashedWheelBucket bucket =
                    wheel[idx];
            // 将任务填充到时间块中
            transferTimeoutsToBuckets();
            // 取出任务并执行
            bucket.expireTimeouts(deadline);
            tick++;
        }
    } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);

    // Fill the unprocessedTimeouts so we can return them from stop() method.
    for (HashedWheelBucket bucket: wheel) {
        bucket.clearTimeouts(unprocessedTimeouts);
    }
    for (;;) {
        HashedWheelTimeout timeout = timeouts.poll();
        if (timeout == null) {
            break;
        }
        if (!timeout.isCancelled()) {
            unprocessedTimeouts.add(timeout);
        }
    }
    // 处理取消的任务
    processCancelledTasks();
}
```

- 取消任务的逻辑这里就不展开看了，也比较简单，有兴趣自行补充即可。

看看上面的 transferTimeoutsToBuckets 方法，如果你看不懂上面图中蓝色格子数字是什么意思，那就认真看看这个方法：
`io.netty.util.HashedWheelTimer.Worker#transferTimeoutsToBuckets`

```java
private void transferTimeoutsToBuckets() {
    // transfer only max. 100000 timeouts per tick to prevent a thread to stale the workerThread when it just
    // adds new timeouts in a loop.
    for (int i = 0; i < 100000; i++) {
        // 取出一个任务对象
        HashedWheelTimeout timeout = timeouts.poll();
        if (timeout == null) {
            // all processed
            break;
        }
        // 如果任务被取消了，则直接过掉
        if (timeout.state() == HashedWheelTimeout.ST_CANCELLED) {
            // Was cancelled in the meantime.
            continue;
        }

        /*
            * remainingRounds的含义就是:时钟还要完整转几回才能执行到任务
            * 比如你的任务是在2500ms之后才执行的（deadline = 2500ms），时钟总共10个刻度，而 tickDuration 为100ms，当前时钟指针已经拨动三次（tick=3）
            * 那 2500 / 100 = 25
            * (25 - 3) / 10 约等于 2
            * 2 就表示 时钟转完当前圈（25-10=15），还要再转一圈（15-10），在第三圈才能执行到该任务
            */
        long calculated = timeout.deadline / tickDuration;
        timeout.remainingRounds = (calculated - tick) / wheel.length;

        final long ticks = Math.max(calculated, tick); // Ensure we don't schedule for past.
        int stopIndex = (int) (ticks & mask);

        // 将任务填充到“时间块”中
        HashedWheelBucket bucket = wheel[stopIndex];
        bucket.addTimeout(timeout);
    }
}
```

继续看看上面 run 方法中的 bucket.expireTimeouts(deadline);，这里面就是拿出任务并执行的逻辑：
`io.netty.util.HashedWheelTimer.HashedWheelBucket#expireTimeouts`

```java
/**
    * Expire all {@link HashedWheelTimeout}s for the given {@code deadline}.
    */
public void expireTimeouts(long deadline) {
    HashedWheelTimeout timeout = head;

    // process all timeouts
    while (timeout != null) {
        HashedWheelTimeout next = timeout.next;
        // 如果剩余轮数 <=0，则表示当前轮就要执行任务了
        if (timeout.remainingRounds <= 0) {
            next = remove(timeout);
            if (timeout.deadline <= deadline) {
                // 执行任务
                timeout.expire();
            } else {
                // The timeout was placed into a wrong slot. This should never happen.
                throw new IllegalStateException(String.format(
                        "timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
            }
        }
        // 如果任务被取消了
        else if (timeout.isCancelled()) {
            next = remove(timeout);
        }
        // 如果任务没被取消，而且剩余轮数>0，则扣除轮数，让任务继续等到至后面轮数
        else {
            timeout.remainingRounds --;
        }
        timeout = next;
    }
}
```

## 和 schedule 对比

关于 schedule 方法加入的定时任务什么时候被执行，你可以参考我之前写的[这篇博客](https://wenjie.store/archives/netty-nioeventloop-boot-2)，在时间操作上和 HashedWheelTimer 大同小异。

schedule 方法也是 Netty 的定时任务实现之一，但是底层的数据结构和 HashedWheelTimer 不一样，schedule 方法用到的数据结构其实和 ScheduledExecutorService 类似，是 PriorityQueue，它是一个优先级的队列。

除此之外，schedule 方法其实也用到 MpscQueue，只是任务执行的时候，会把任务从 PriorityQueue 转移到 MpscQueue 上。

下面来跟踪下 schedule 方法看看，由于主要是看数据结构的区别，所以一些地方在这里我就不深追了

首先来到如下代码：
`io.netty.util.concurrent.AbstractScheduledEventExecutor#schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)`

```java
@Override
public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    ObjectUtil.checkNotNull(command, "command");
    ObjectUtil.checkNotNull(unit, "unit");
    if (delay < 0) {
        delay = 0;
    }
    validateScheduled0(delay, unit);

    return schedule(new ScheduledFutureTask<Void>(
            this, command, null, ScheduledFutureTask.deadlineNanos(unit.toNanos(delay))));
}
```

继续跟进 schedule 方法看看：
`io.netty.util.concurrent.AbstractScheduledEventExecutor#schedule(io.netty.util.concurrent.ScheduledFutureTask<V>)`

```java
private <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
    if (inEventLoop()) {
        scheduledTaskQueue().add(task.setId(nextTaskId++));
    } else {
        executeScheduledRunnable(new Runnable() {
            @Override
            public void run() {
                scheduledTaskQueue().add(task.setId(nextTaskId++));
            }
        }, true, task.deadlineNanos());
    }

    return task;
}
```

继续跟进 scheduledTaskQueue()方法：
`io.netty.util.concurrent.AbstractScheduledEventExecutor#scheduledTaskQueue`

```java
PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue() {
    if (scheduledTaskQueue == null) {
        scheduledTaskQueue = new DefaultPriorityQueue<ScheduledFutureTask<?>>(
                SCHEDULED_FUTURE_TASK_COMPARATOR,
                // Use same initial capacity as java.util.PriorityQueue
                11);
    }
    return scheduledTaskQueue;
}
```

可以看到返回值就是 PriorityQueue，它是一个最小堆实现的优先队列。

## 扩展

### 不同实现的时间复杂度

这里我就直接贴下网上大佬给出的解释：

如果使用最小堆实现的优先级队列：
![最小堆.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595756711656-f9df4f.png)

- 大致意思就是你的任务如果插入到堆顶，时间复杂度为 O(log(n))。

如果使用链表（既然有说道，那就扩展下）：
![链表.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595756928493-37b8b5.png)

- 中间插入后的事件复杂度为 O(n)

单个时间轮：
![单个时间轮.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595757035360-c29046.png)

- 复杂度可以降至 O(1)。

记录轮数的时间轮（其实就是文章开头的那个）：
![记录轮数的时间轮.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595757110003-0c60d7.png)

层级时间轮：
![层级时间轮.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595757328715-285da9.png)

- 时间复杂度是 O(n)，n 是轮子的数量，除此之外还要计算一个轮子上的 bucket。

### 单时间轮缺点

根据上面的图其实不难理解，如果任务是很久之后才执行的、同时要保证任务低延迟，那么单个时间轮所需的 bucket 数就会变得非常多，从而导致内存占用持续升高（CPU 空转时间还是不变的，仅仅是内存需求变高了），如下图：

![image.png](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/image_1595758329809-3f33b0.png)

Netty 对于单个时间轮的优化方式就是记录下 remainingRounds，从而减少 bucket 过多的内存占用。

### 时间轮和 PriorityQueue 对比

看完上面的时间复杂度对比，你可能会觉得：

- Q：时间轮的复杂度只有 O(1)，schedule 和 ScheduledExecutorService 这种都是 O(log(n))，那时间轮不是碾压吗？

- A：你不要忘了，如果任务是在很久之后才执行的，那么时间轮就会产生很多空转，这是非常浪费 CPU 性能的，这种空转消耗可以通过增大 tickDuration 来避免，但这样做又会产生降低定时任务的精度，可能导致一些任务推到很迟才执行。
- A：而 ScheduledExecutorService 不会有这个问题。

另外，Netty 时间轮的实现模型抽象出来是大概这个样子的：

```java
for(Tasks task : tasks) {
    task.doXxx();
}
```

这个抽象是个什么意思呢？你要注意一个点，这里的任务循环执行是同步的，**这意味着你第一个任务执行很慢延迟很高，那么后面的任务全都会被堵住**，所以你加进时间轮的任务不可以是耗时任务，比如一些延迟很高的数据库查询，如果有这种耗时任务，最好再嵌入线程池处理，不要让任务阻塞在这一层。



# HashedWheelTimer时间轮原理分析

该文所涉及的 Netty 源码版本为 4.1.6。

## HashedWheelTimer 是什么

Netty 的时间轮 `HashedWheelTimer` 给出了一个**粗略的定时器实现**，之所以称之为粗略的实现是**因为该时间轮并没有严格的准时执行定时任务**，而是在每隔一个时间间隔之后的时间节点执行，并执行当前时间节点之前到期的定时任务。

当然具体的定时任务的时间执行精度可以通过调节 HashedWheelTimer 构造方法的时间间隔的大小来进行调节，在大多数网络应用的情况下，由于 IO 延迟的存在，并**不会严格要求具体的时间执行精度**，所以默认的 100ms 时间间隔可以满足大多数的情况，不需要再花精力去调节该时间精度。

## HashedWheelTimer 的实现原理

### HashedWheelTimer 内部的数据结构

```java
private final HashedWheelBucket[] wheel;
```

HashedWheelTimer 的主体数据结构 wheel 是一个**由多个链表所组成的数组**，默认情况下该数组的大小为 512。当定时任务准备加入到时间轮中的时候，将会以其等待执行的时间为依据选择该数组上的一个具体槽位上的链表加入。

```java
private HashedWheelTimeout head;
private HashedWheelTimeout tail;
```

在这个 wheel 数组中，每一个槽位都是一条由 HashedWheelTimeout 所组成的**链表**，其中链表中的**每一个节点都是一个等待执行的定时任务**。

### HashedWheelTimer 内部的线程模型

在 HashedWheelTimer 中，其内部是一个单线程的 worker 线程，通过类似 eventloop 的工作模式进行定时任务的调度。

```java
@Override
public void run() {
    // Initialize the startTime.
    startTime = System.nanoTime();
    if (startTime == 0) {
        // We use 0 as an indicator for the uninitialized value here, so make sure it's not 0 when initialized.
        startTime = 1;
    }

    // Notify the other threads waiting for the initialization at start().
    startTimeInitialized.countDown();

    do {
        final long deadline = waitForNextTick();
        if (deadline > 0) {
            transferTimeoutsToBuckets();
            HashedWheelBucket bucket =
                    wheel[(int) (tick & mask)];
            bucket.expireTimeouts(deadline);
            tick++;
        }
    } while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);

    // Fill the unprocessedTimeouts so we can return them from stop() method.
    for (HashedWheelBucket bucket: wheel) {
        bucket.clearTimeouts(unprocessedTimeouts);
    }
    for (;;) {
        HashedWheelTimeout timeout = timeouts.poll();
        if (timeout == null) {
            break;
        }
        unprocessedTimeouts.add(timeout);
    }
}
```

简单看到 HashedWheelTimer 内部的 woker 线程的 `run()`方法，在其首先会记录启动时间作为 startTime 作为接下来调度定时任务的时间依据，而之后会通过 CountDownLatch 来通知所有外部线程当前 worker 工作线程已经初始化完毕。之后的循环体便是当时间轮持续生效的时间里的具体调度逻辑。**时间刻度是时间轮的一个重要属性**，其默认为 100ms，此处的循环间隔便是时间轮的时间刻度，默认情况下就是间隔 100ms 进行一次调度循环。工作线程会维护当前工作线程具体循环了多少轮，用于定位具体执行触发时间轮数组上的哪一个位置上的链表。当时间轮准备 shutdown 的阶段，最后的代码会对未执行的任务整理到未执行的队列中。

由此可见，**worker 线程的 run()方法中基本定义了工作线程的整个生命周期，从初始的初始化到循环体中的具体调度，最后到未执行任务的具体清理**。整体的调度逻辑便主要在这里执行。值得注意的是，在这里的前提下，每个 HashedWheelTimer 时间轮都会有一个工作线程进行调度，所以不需要在 netty 中在每一个连接中单独使用一个 HashedWheelTimer 来进行定时任务的调度，否则可能将对性能产生影响。

### 向 HashedWheelTimer 加入一个定时任务的流程

当调用 HashedWheelTimer 的 newTimeout()方法的时候，即是将定时任务加入时间轮中的 api。

```java
@Override
public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
    if (task == null) {
        throw new NullPointerException("task");
    }
    if (unit == null) {
        throw new NullPointerException("unit");
    }
    start();

    long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;
    HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
    timeouts.add(timeout);
    return timeout;
}
```

当此次是首次向该时间轮加入定时任务的时候，将会通过 start()方法开始执行上文所述的 worker 工作线程的启动与循环调度逻辑，这里暂且不提。之后计算定时任务触发时间相对于时间轮初始化时间的相对时间间隔 deadline，并将其包装为一个链表节点 HashedWheelTimeout ，投入到 timeouts 队列中，等待 worker 工作线程在下一轮调度循环中将其加入到时间轮的具体链表中等待触发执行，timeouts 的实现是一个 mpsc 队列，关于 mpsc 队列可以查看[此文](https://mp.weixin.qq.com/s/VVoDJwrLZrN3mm-jaQJayQ)，这里也符合**多生产者单消费者的队列模型**。

### HashedWheelTimer 中工作线程的具体调度

```java
do {
    final long deadline = waitForNextTick();
    if (deadline > 0) {
        transferTimeoutsToBuckets();
        HashedWheelBucket bucket =
                wheel[(int) (tick & mask)];
        bucket.expireTimeouts(deadline);
        tick++;
    }
} while (WORKER_STATE_UPDATER.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);
```

在 HashedWheelTimer 中的工作线程 run()方法的主要循环中，主要分为三个步骤。

首先 worker 线程会通过 `waitForNextTick()`方法根据时间轮的时间刻度等待一轮循环的开始，在默认情况下时间轮的时间刻度是 100ms，那么此处 worker 线程也将在这个方法中 sleep 相应的时间等待下一轮循环的开始。此处也决定了时间轮的定时任务时间精度。

当 worker 线程经过相应时间间隔的 sleep 之后，也代表新的一轮调度开始。此时，会通过 `transferTimeoutsToBuckets()`方法将之前刚刚加入到 timeouts 队列中的定时任务放入到时间轮具体槽位上的链表中。

```java
for (int i = 0; i < 100000; i++) {
    HashedWheelTimeout timeout = timeouts.poll();
    if (timeout == null) {
        // all processed
        break;
    }
    if (timeout.state() == HashedWheelTimeout.ST_CANCELLED
            || !timeout.compareAndSetState(HashedWheelTimeout.ST_INIT, HashedWheelTimeout.ST_IN_BUCKET)) {
        timeout.remove();
        continue;
    }
    long calculated = timeout.deadline / tickDuration;
    long remainingRounds = (calculated - tick) / wheel.length;
    timeout.remainingRounds = remainingRounds;

    final long ticks = Math.max(calculated, tick); // Ensure we don't schedule for past.
    int stopIndex = (int) (ticks & mask);

    HashedWheelBucket bucket = wheel[stopIndex];
    bucket.addTimeout(timeout);
}
```

首先，在每一轮的调度中，最多只会从 `timeouts` 队列中定位到时间轮 100000 个定时任务，这也是为了防止在这里耗时过久导致后面触发定时任务的延迟。在这里会不断从 timeouts 队列中获取刚加入的定时任务。

**具体的计算流程**便是将定时任务相对于时间轮初始化时间的相对间隔与时间轮的时间刻度相除得到相对于初始化时间的具体轮数，之后便在减去当前轮数得到还需要遍历几遍整个时间轮数组得到 remainingRounds，最后将轮数与时间轮数组长度-1 相与，得到该定时任务到底应该存放到时间轮上哪个位置的链表。

用具体的数组**举个例子**，该时间轮初始化时间为 12 点，时间刻度为 1 小时，时间轮数组长度为 8，当前时间 13 点，当向时间轮加入一个明天 13 点执行的任务的时候，首先得到该任务相对于初始化的时间间隔是 25 小时，也就是需要 25 轮调度，而当前 13 点，当前调度轮数为 1，因此还需要 24 轮调度，就需要再遍历 3 轮时间轮，因此 remainingRounds 为 3，再根据 25 与 8-1 相与的结果为 1，因此将该定时任务放置到时间轮数组下标为 1 的链表上等待被触发。

这便是**一次完整的定时任务加入到时间轮具体位置的计算**。

在 worker 线程的最后，就需要来具体执行定时任务了，首先通过当前循环轮数与时间轮数组长度-1 相与的结果定位具体触发时间轮数组上哪个位置上的链表，再通过 `expireTimeouts()`方法依次对链表上的定时任务进行触发执行。这里的流程就相对很简单，链表上的节点如果 remainingRounds 小于等于 0，那么就可以直接执行这个定时任务，如果 remainingRounds 大于 0，那么显然还没有到达触发的时间点，则将其-1 等待下一轮的调度之后再进行执行。在继续回到上面的例子，当 14 点来临之时，此时工作线程将进行第 2 轮的调度，将会把 2 与 8-1 进行相与得到结果 2，那么当前工作线程就会选择时间轮数组下标为 2 的链表依次判断是否需要触发，如果 remainingRounds 为 0 将会直接触发，否则将会将 remainingRounds-1 等待下一轮的执行。



# MpscLinkedQueue队列原理分析



该文所涉及的 netty 源码版本为 4.1.6。

## MpscLinkedQueue 是什么

在 Netty 的核心中的核心成员 NioEventLoop 中，其中任务队列的实现 taskQueue 便是 MpscLinkedQueue。MpscLinkedQueue 是 Netty 所实现的一个基于多生产者单消费者的无锁队列，针对 NioEventLoop 中任务队列的特点，其单消费者的场景在一开始就避免了从队列中取数据时加锁的必要，而其最精妙的地方便是在多生产者并发从队列中添加数据的时候也没有加锁，达到 Netty 所期望的高性能实现。这是如何实现的？

## MpscLinkedQueue 无锁并发线程安全写入原理

### MpscLinkedQueue 对于尾结点的维护

首先，MpscLinkedQueue 继承自 AtomicReference，也就是说 MpscLinkedQueue 通过继承自 AtomicReference 的方式，显式地维护了一个提供原子读写能力的变量 value。而在 MpscLinkedQueue 中，这个 value 是其内部维护的队列的尾结点。

### MpscLinkedQueue 对于头结点的维护

而后，来看 MpscLinkedQueue 的构造方法。

```java
MpscLinkedQueue() {
    MpscLinkedQueueNode<E> tombstone = new DefaultNode<E>(null);
    headRef = new FullyPaddedReference<MpscLinkedQueueNode<E>>();
    headRef.set(tombstone);
    setTail(tombstone);
}
```

在 MpscLinkedQueue 中，维护着 headRef 头结点字段，其队列内部节点的实现是一个 MpscLinkedQueueNode。MpscLinkedQueueNode 是一个除了存放具体队列元素外只有 next 字段的节点，也就是说，MpscLinkedQueue 的队列是单向的。在构造方法的最后，通过 setTail()方法的，将 MpscLinkedQueue 的尾结点字段 value 也设置为头结点。MpscLinkedQueue 的头结点字段 headRef 的存在可以方便后续直接从头结点开始的队列操作，消费者可以简单判断头尾节点是否相等来确认队列中是否有元素可以消费。

### MpscLinkedQueue 如何做到线程安全的无锁加入

```java
@Override
@SuppressWarnings("unchecked")
public boolean offer(E value) {
    if (value == null) {
        throw new NullPointerException("value");
    }

    final MpscLinkedQueueNode<E> newTail;
    if (value instanceof MpscLinkedQueueNode) {
        newTail = (MpscLinkedQueueNode<E>) value;
        newTail.setNext(null);
    } else {
        newTail = new DefaultNode<E>(value);
    }

    MpscLinkedQueueNode<E> oldTail = replaceTail(newTail);
    oldTail.setNext(newTail);
    return true;
}

private MpscLinkedQueueNode<E> replaceTail(MpscLinkedQueueNode<E> node) {
    return getAndSet(node);
}
```

MpscLinkedQueue 的 offer()方法很简短，但是恰恰就是整个添加队列元素加入的流程，当元素被加入的时候，首先判断加入的元素是否是 MpscLinkedQueueNode，如果不是则进行封装。之后便是整个操作的重点：

- 通过 replaceTail()方法，将当前被加入的节点通过 AtomicReference 所提供的 getAndSet()方法将其设为队列的尾结点，并返回先前的尾结点。这次操作由 UNSAFE 的 CAS 来保证操作的原子性。
- 之后将之前的尾结点的 next 指向新加入的节点，本次加入宣告结束。
  整个操作就到此结束，这里可以看出，MpscLinkedQueue 利用了 AtomicReference 底层 UNSAFE 的能力，通过 CAS 确保新设置进入 value 的节点必定能够和原先的节点达成一个且唯一的联系，那么只需要自顶向下不断通过将这个联系变成引用，那么一条队列便形成了。由于其实现是链表而不是数组，也就没有涉及到资源的竞争，在不加锁的前提下其队列顺序可能不会严格按照加入顺序，但这在当前场景下并不是问题。在这个前提，高并发的插入场景下，每个新进入的新节点都将获取原尾位置 value 上的节点，而自身将会被设置为其后驱节点重新放到尾结点位置上，CAS 在不加锁的前提下保证了前后节点对应关系的唯一性，完成了并发条件下不加锁的线程安全写入。

### MpscLinkedQueue 不支持 remove()

在 MpscLinkedQueue 中，是不支持 remove()的方法去从队列中移除任意一个元素的。原因很简单，消费者和生产者是无锁的，消费者可以通过比较队首和队尾元素是否一致来保证线程安全地从队首取数据，但是 remove()从队列中任意位置修改数据是线程不安全的，主要体现在移除队尾元素可能会导致正在加入的新元素被丢弃。

## MpscLinkedQueue 另外的实现细节

- MpscLinkedQueue 中的头节点被通过 FullyPaddedReference 封装。其内部前后分别填充 56 字节和 64 字节来进行填充以避免伪共享导致的性能损耗，使得其头结点可以高效被访问。关于伪共享的相关知识可以通过搜索引擎进行查询。
- MpscLinkedQueue 在消费者消费数据后，当将下一个节点设置为头结点的时候，并不是直接进行赋值，而是通过 UNSAFE 来根据偏移量赋值，这样做将略微提高性能，主要是内存屏障 storestrore 和 loadstrore 之间的性能差异。

# Recycler对象池原理分析

该文所涉及的 netty 源码版本为 4.1.6。

## Netty 的对象池 Recycler 是什么

Recycler 是 Netty 中基于 ThreadLocal 的轻量化的对象池实现。既然是基于 ThreadLocal，那么就可以将其理解为当前线程在通过对象池 Recycler 得到一个对象之后，在回收对象的时候，不需要将其销毁，而是放回到该线程的对象池中即可，在该线程下一次用到该对象的时候，不需要重新申请空间创建，而是直接重新从对象池中获取。

## Recycler 在 netty 中被如何使用

Recycler 对象池在 netty 中最重要的使用，就在于 netty 的池化 ByteBuf 的场景下。首先，何为池化？以 PooledDirectByteBuf 举例，每一个 PooledDirectByteBuf 在应用线程中使用完毕之后，并不会被释放，而是等待被重新利用，类比线程池每个线程在执行完毕之后不会被立即释放，而是等待下一次执行的时候被重新利用。所谓的对象池也是如此，池化减少了 ByteBuf 创建和销毁的开销，也是 netty 高性能表现的基石之一。

```java
private static final Recycler<PooledDirectByteBuf> RECYCLER = new Recycler<PooledDirectByteBuf>() {
    @Override
    protected PooledDirectByteBuf newObject(Handle<PooledDirectByteBuf> handle) {
        return new PooledDirectByteBuf(handle, 0);
    }
};

static PooledDirectByteBuf newInstance(int maxCapacity) {
    PooledDirectByteBuf buf = RECYCLER.get();
    buf.reuse(maxCapacity);
    return buf;
}
```

PooledDirectByteBuf 在其类加载的过程中，初始化了一个静态的 RECYCLER 成员，通过重写其 newObject()方法达到使 Recycler 可以初始化一个 PooledDirectByteBuf。而在接下来的使用中，只需要通过静态方法 newInstance()就可以从 RECYCLER 对象池的 get()方法获取一个新的 PooledDirectByteBuf 对象返回，而重写的方法 newObject()中的入参 Handler 则提供了 recycle()方法给出了对象重新放入池中回收的能力，这里的具体实现在下文展开。因此，newInstance()方法和 recycle()方法就提供了对象池出池和入池的能力，也通过此，PooledDirectByteBuf 达到了池化的目标。

## Recycler 的实现原理分析

**Recycler 的实现原理很抽象，可以先直接阅读文末的例子再阅读这部分内容。**  
Recycler 中，最核心的是两个通过 ThreadLocal 作为本地线程私有的两个成员，而其实现原理只需要围绕这两个成员分析，就可以对对象池的设计有直接的理解和认识。

- 第一个成员是在 Recycler 被定义的 Stack 成员对象。

```java
private final FastThreadLocal<Stack<T>> threadLocal = new FastThreadLocal<Stack<T>>() {
    @Override
    protected Stack<T> initialValue() {
        return new Stack<T>(Recycler.this, Thread.currentThread(), maxCapacityPerThread, maxSharedCapacityFactor,
                ratioMask, maxDelayedQueuesPerThread);
    }
};
```

顾名思义，这个 Stack 主体是一个堆栈，但是其还维护着一个链表，而链表中的每一个节点都是一个队列。

```java
private DefaultHandle<?>[] elements;
private WeakOrderQueue cursor, prev;
```

上述的 elements 数组便是存放当前线程被回收的对象，当当前线程从该线程的 Recycler 对象池尝试获取新的对象的时候，首先就会从当前 Stack 的这个数组中尝试获取已经在先前被创建并且在当前线程被回收的对象，因为当对象池的对象在当前线程被调用 recycle()的时候，是会直接放到 elements 数组中等待下一次的利用。 那么问题来了，如果从该线程中被申请的这个对象是在另外一个线程中被调用 recycle()方法回收呢？那么该对象就会处于链表中的队列中，当堆栈数组中的对象不存在的时候，将会尝试把链表队列中的对象转移到数组中供当前线程获取。那么其他线程是如何把被回收的对象放到这些链表中的队列的呢？接下来就是另一个成员的使命了。

- 第二个成员是在 Recycler 中也是通过 ThreadLocal 所实现的一个线程本地变量，DELAYED_RECYCLED ，是一个 Stack 和队列的映射 Map。

```java
private static final FastThreadLocal<Map<Stack<?>, WeakOrderQueue>> DELAYED_RECYCLED =
        new FastThreadLocal<Map<Stack<?>, WeakOrderQueue>>() {
    @Override
    protected Map<Stack<?>, WeakOrderQueue> initialValue() {
        return new WeakHashMap<Stack<?>, WeakOrderQueue>();
    }
};
```

第二个成员 DELAYED_RECYCLED 可以通过上文的 Stack 获取一个队列。  
在前一个成员的解释中提到，当别的线程调用另一个线程的对象池的 recycle()方法进行回收的时候，并不会直接落到持有对象池的线程的 Stack 数组当中，当然原因也很简单，在并发情况下这样的操作显然是线程不安全的，而加锁也会带来性能的开销。因此，netty 在 Recycler 对象池中通过更巧妙的方式解决这一问题。  
在前面提到，除了数组，Stack 还持有了一系列队列的组成的链表，这些链表中的每一个节点都是一个队列，这些队列又存放着别的线程所回收到当前线程对象池的对象。那么，这些队列就是各个线程针对持有对象池的专属回收队列，说起来很拗口，看下面的代码。

```java
private void pushLater(DefaultHandle<?> item, Thread thread) {
    // we don't want to have a ref to the queue as the value in our weak map
    // so we null it out; to ensure there are no races with restoring it later
    // we impose a memory ordering here (no-op on x86)
    Map<Stack<?>, WeakOrderQueue> delayedRecycled = DELAYED_RECYCLED.get();
    WeakOrderQueue queue = delayedRecycled.get(this);
    if (queue == null) {
        if (delayedRecycled.size() >= maxDelayedQueues) {
            // Add a dummy queue so we know we should drop the object
            delayedRecycled.put(this, WeakOrderQueue.DUMMY);
            return;
        }
        // Check if we already reached the maximum number of delayed queues and if we can allocate at all.
        if ((queue = WeakOrderQueue.allocate(this, thread)) == null) {
            // drop object
            return;
        }
        delayedRecycled.put(this, queue);
    } else if (queue == WeakOrderQueue.DUMMY) {
        // drop object
        return;
    }

    queue.add(item);
}

private WeakOrderQueue(Stack<?> stack, Thread thread) {
    head = tail = new Link();
    owner = new WeakReference<Thread>(thread);
    synchronized (stack) {
        next = stack.head;
        stack.head = this;
    }

    // Its important that we not store the Stack itself in the WeakOrderQueue as the Stack also is used in
    // the WeakHashMap as key. So just store the enclosed AtomicInteger which should allow to have the
    // Stack itself GCed.
    availableSharedCapacity = stack.availableSharedCapacity;
}
```

pushLater()方法发生在当一个对象被回收的时候，当当前线程不是这个对象所申请的时候的线程时，将会通过该对象的 Stack 直接去通过 DELAYED_RECYCLED 映射到一条队列上，如果没有则创建并建立映射，再把该对象放入到该队列中，以上操作结束后该次回收即宣告结束

```java
private WeakOrderQueue(Stack<?> stack, Thread thread) {
    head = tail = new Link();
    owner = new WeakReference<Thread>(thread);
    synchronized (stack) {
        next = stack.head;
        stack.head = this;
    }

    // Its important that we not store the Stack itself in the WeakOrderQueue as the Stack also is used in
    // the WeakHashMap as key. So just store the enclosed AtomicInteger which should allow to have the
    // Stack itself GCed.
    availableSharedCapacity = stack.availableSharedCapacity;
}
```

如果在操作中，队列是被创建的，会把该队列放置在 Stack 中的链表里的头结点，保证创建该对象的线程在数组空了之后能够通过链表访问到该队列并将该队列中的回收对象重新放到数组中等待被下次重新利用，队列交给 A 线程的链表是唯一的阻塞操作。在这里通过一次阻塞操作，避免后续都不存在资源的竞争问题。

## 举一个例子来解释对象池的原理

_A 线程申请，A 线程回收的场景。_

- 显然，当对象的申请与回收是在一个线程中时，直接把对象放入到 A 线程的对象池中即可，不存在资源的竞争，简单轻松。

_A 线程申请，B 线程回收的场景。_

- 首先，当 A 线程通过其对象池申请了一个对象后，在 B 线程调用 recycle()方法回收该对象。显然，该对象是应该回收到 A 线程私有的对象池当中的，不然，该对象池也失去了其意义。
- 那么 B 线程中，并不会直接将该对象放入到 A 线程的对象池中，如果这样操作在多线程场景下存在资源的竞争，只有增加性能的开销，才能保证并发情况下的线程安全，显然不是 netty 想要看到的。
- 那么 B 线程会专门申请一个针对 A 线程回收的专属队列，在首次创建的时候会将该队列放入到 A 线程对象池的链表首节点（这里是唯一存在的资源竞争场景，需要加锁），并将被回收的对象放入到该专属队列中，宣告回收结束。
- 在 A 线程的对象池数组耗尽之后，将会尝试把各个别的线程针对 A 线程的专属队列里的对象重新放入到对象池数组中，以便下次继续使用。

# 内存池之PoolChunk设计与实现

该文所涉及的 netty 源码版本为 4.1.16。

## 在一开始需要明确的几个概念

在 Netty 的内存池的 PoolChunk 中，先要明确以下几个概念。

- page: page 是 chunk 中所能申请到的最小内存单位。
- chunk: 一个 chunk 是一组 page 的集合
- 在 PoolChunk 中，chunkSize 的大小是 `2^maxOrder * pageSize`，其中 2^maxOrder 是 PoolChunk 中的完全二叉树叶子结点的数量，pageSize 则是单个 page 的大小。

综合如上所述，举一个数字上的例子，默认情况下，单个 Page 的大小为 8192，也就是 8kb，maxOrder 默认情况下是 11，因此在这个情况下 PoolChunk 中的二叉树的叶子节点数量是 2048，chunkSize 的大小则是 2048\*8kb 为 16M。

## PoolChunk 的内部完全二叉树结构

PoolChunk 中的 page 通过一颗完全二叉树来达到快速访达及操作，而不需要通过 O(n)的时间复杂度来进行遍历，并耗费相当大的空间来记录各个 page 的使用情况。一颗完全二叉树的结构如下所示：

- 高度=0 1 个节点 (单个节点表示的大小为 chunkSize)
- 高度=1 2 个节点 (单个节点表示的大小为 chunkSize/2)
- ..
- ..
- 高度=d 2^d 个节点 (单个节点表示的大小为 chunkSize/2^d)
- ..
- 高度=maxOrder 2^maxOrder 个节点 (单个节点的大小为 chunkSize/2^maxOrder，也就是 pageSize)

在这棵树的帮助下，当我们要申请 x 大小的内存的时候 ，得到比 x 最接近的 chunkSize/2^k 的大小，也就是说只要从左开始找到 k 层第一个没有被使用的节点即可开始将其子树的叶子结点的 page 进行分配。

## PoolChunk 的二叉树使用状态

单依靠上述的完全二叉树是无法达到内存池设计的目的的，因为缺少了 page 的使用情况，仍旧需要一个数据结构来辅助记录各个节点的使用情况。  
PoolChunk 中还给出了一个 byte 数组 memoryMap，大小为完全二叉树所有节点的个数，在之前的例子中这个 byte 数组就为 4096。在初始情况下，这个数组每个位置上的初始指为该位置的节点在完全二叉树中的高度。因此，这个数组 memoryMap 就有了以下几种状态。

- 1. memoryMap[i] = i 节点在完全二叉树中的深度，代表当前节点下的子树都还没有被分配。
- 2. memoryMap[i] > i 节点在完全二叉树中的深度, 这个节点下的子树也就有节点被使用，但是仍有节点处于空闲状态。
- 3. memoryMap[i] = maxOrder + 1，这个节点下面的子树已经完全被使用。
     这个 Byte 数组，就相当于为这个完全二叉树准备了状态与索引存储，可以高效的在二叉树中选择定位所需要指定大小的子树进行分配。

## 业务逻辑展开

```java
private int allocateNode(int d) {
    int id = 1;
    int initial = - (1 << d); // has last d bits = 0 and rest all = 1
    byte val = value(id);
    if (val > d) { // unusable
        return -1;
    }
    while (val < d || (id & initial) == 0) { // id & initial == 1 << d for all ids at depth d, for < d it is 0
        id <<= 1;
        val = value(id);
        if (val > d) {
            id ^= 1;
            val = value(id);
        }
    }
    byte value = value(id);
    assert value == d && (id & initial) == 1 << d : String.format("val = %d, id & initial = %d, d = %d",
            value, id & initial, d);
    setValue(id, unusable); // mark as unusable
    updateParentsAlloc(id);
    return id;
}
```

allocateNode(int d)方法用来在完全二叉树中以从左开始的顺序获取一颗高度为 d 的没有被使用过的子树。具体顺序如下：

- 首先从根节点 1 开始，判断 memoryMap[1]的值，如果大于 d，则说明当前的二叉树已经不存在能够分配的节点了。如果小于 d，则可以继续往下分配。
- 如果其左节点在 memoryMap 的值小于 d，则继续从左节点往下寻找。如果大于，则从其右节点开始往下寻找。
- 在下一层的节点中持续进行上述的判断，直到在书中找到符合高度条件的子树。

```java
private long allocateRun(int normCapacity) {
    int d = maxOrder - (log2(normCapacity) - pageShifts);
    int id = allocateNode(d);
    if (id < 0) {
        return id;
    }
    freeBytes -= runLength(id);
    return id;
}
```

allocateRun()方法就是在上文的 allocateNode()的前提下，根据指定的大小的内存在二叉树上分配指定大小的子树。比如说在上述 16M 大小每个 page8kb 的 chunk 中寻求 64k 的内存的时候，需要 8 个 page 叶子结点，那么就是需要一个高度为 4 的完全二叉树，那么也就是只要在 PoolChunk 中通过 allocateNode()方法从完全二叉树的第 7 层开始从左往右找到一颗可以使用的子树即可。

```java
private long allocateSubpage(int normCapacity) {
    // Obtain the head of the PoolSubPage pool that is owned by the PoolArena and synchronize on it.
    // This is need as we may add it back and so alter the linked-list structure.
    PoolSubpage<T> head = arena.findSubpagePoolHead(normCapacity);
    synchronized (head) {
        int d = maxOrder; // subpages are only be allocated from pages i.e., leaves
        int id = allocateNode(d);
        if (id < 0) {
            return id;
        }

        final PoolSubpage<T>[] subpages = this.subpages;
        final int pageSize = this.pageSize;

        freeBytes -= pageSize;

        int subpageIdx = subpageIdx(id);
        PoolSubpage<T> subpage = subpages[subpageIdx];
        if (subpage == null) {
            subpage = new PoolSubpage<T>(head, this, id, runOffset(id), pageSize, normCapacity);
            subpages[subpageIdx] = subpage;
        } else {
            subpage.init(head, normCapacity);
        }
        return subpage.allocate();
    }
}
```

当向 PoolChunk 申请的内存大小小于 pageSize 的时候，将直接通过 allocateSubpage()方法尝试直接在叶子结点，也就是二叉树的最后一层选择一个空的还未使用的叶子结点，在选择的叶子结点中构造一个 PoolSubPage 来返回，而不需要耗费整整一个叶子结点导致内存占用浪费。



# 内存池之从内存池申请内存



该文所涉及的 netty 源码版本为 4.1.16。

## Netty 内存池申请内存流程

在通过 PooledByteBufAllocator 中向内存池中进行内存申请的时候，最先开始的步骤便是从 PooledByteBufAllocator 中一系列 PoolArena 数组中，选择其中一个 PoolArena 进行分配。

这时将会从 PoolArena 数组中选取当前使用量最小的 PoolArena 与当前线程通过 ThreadLocal 进行绑定，之后涉及到内存申请将会直接从这个 PoolArena 进行获取，这个做法在高并发情况下频繁往内存池中进行内存申请的时候可以减少资源竞争，提升效率。

在当前线程获取与其绑定的 PoolArena 之后，接下来就是从 PoolArena 中继续申请内存。  
为了适应各种大小的内存场景，PoolArena 的组成也是为了其设计。

- PoolSubpage 数组 tinySubpagePools：默认情况下，当申请的内存小于 512b 的时候的时候将会从 tinySubpagePools 中直接选择 subPage（内存池中的最小单位）返回
- PoolSubpage 数组 smallSubpagePools：默认情况下，当申请的内存大于 512b 但是小于一个 page 的大小（8kb）的时候，将会从 smallSubpagePools 返回一个 subPage。subPage 是由 poolChunk 中的 page 分配而来。
- PoolChunkList<T> qInit：存储内存利用率 0-25%的 poolChunk
- PoolChunkList<T> q000：存储内存利用率 1-50%的 poolChunk
- PoolChunkList<T> q025：存储内存利用率 25-75%的 poolChunk
- PoolChunkList<T> q050：存储内存利用率 50-100%的 poolChunk
- PoolChunkList<T> q075：存储内存利用率 75-100%的 poolChunk
- PoolChunkList<T> q100：存储内存利用率 100%的 poolChunk、
  当申请的内存大于一个 page（8kb）但又小于一个 poolChunk（2048kb）总大小的时候，将会从各个 PoolChunkList 中尝试获取一个 poolChunk 从中返回。PoolChunkList 是一个由 poolChunk 组成的链表。  
  以上几个 PoolChunkList，由符合各个内存利用率的 poolChunk 组成，这几个 PoolChunkList 之间又互相首尾连接组成队列，方便 PoolChunk 在各个队列中根据自己当前的利用率进行转移到对应的位置上。  
  最后，当申请的内存大于一个 poolChunk 大小的时候将会直接申请一段非池化的内存返回，并不会占用内存池中的内存空间。

最后，到了从 poolChunk 中申请内存的场景，这一部分在[该文](https://github.com/doocs/source-code-hunter/blob/main/docs/Netty/Netty技术细节源码分析/内存池之PoolChunk设计与实现.md)中已经详细说明，这部分也是内存池中获取内存的最后一步。





# **Netty主要组件源码分析**

# ChannelPipeline和ChannelHandler组件

Netty 的 ChannelPipeline 和 ChannelHandler 机制类似于 Servlet 和 Filter 过滤器，这类拦截器实际上是职责链模式的一种变形，主要是为了方便事件的拦截和用户业务逻辑的定制。

Servlet Filter 能够以声明的方式（web.xml 配置文件）插入到 HTTP 请求响应的处理过程中，用于拦截请求和响应，以便能够查看、提取或以某种方式操作正在客户端和服务器之间交换的数据。拦截器封装了业务定制逻辑，能够实现对 Web 应用程序 的预处理和事后处理。

Netty 的 Channel 过滤器 实现原理与 Servlet Filter 机制 一致，它将 Channel 的数据管道 抽象为 ChannelPipeline，消息在 ChannelPipeline 中流动和传递。ChannelPipeline 持有 I/O 事件拦截器 ChannelHandler 链表，由 ChannelHandler 链表 对 IO 事件 进行拦截和处理，可以通过新增和删除 ChannelHandler 来实现不同的业务逻辑定制，不需要对已有的 ChannelHandler 进行修改，能够实现对修改封闭和对扩展的支持。

下面我们对 ChannelPipeline 和 ChannelHandler 的功能进行简单地介绍，然后分析下其源码设计。

## ChannelPipeline 的功能和作用

ChannelPipeline 是 ChannelHandler 的容器，它负责 ChannelHandler 的管理、事件拦截与调度。

#### ChannelPipeline 的事件处理

下图展示了一个消息被 ChannelPipeline 的 ChannelHandler 链拦截和处理的全过程。

```java
 *                                                 I/O Request
 *                                            via {@link Channel} or
 *                                        {@link ChannelHandlerContext}
 *                                                      |
 *  +---------------------------------------------------+---------------+
 *  |                           ChannelPipeline         |               |
 *  |                                                  \|/              |
 *  |    +---------------------+            +-----------+----------+    |
 *  |    | Inbound Handler  N  |            | Outbound Handler  1  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler N-1 |            | Outbound Handler  2  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  .               |
 *  |               .                                   .               |
 *  | ChannelHandlerContext.fireIN_EVT() ChannelHandlerContext.OUT_EVT()|
 *  |        [ method call]                       [method call]         |
 *  |               .                                   .               |
 *  |               .                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  2  |            | Outbound Handler M-1 |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  |               |                                  \|/              |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |    | Inbound Handler  1  |            | Outbound Handler  M  |    |
 *  |    +----------+----------+            +-----------+----------+    |
 *  |              /|\                                  |               |
 *  +---------------+-----------------------------------+---------------+
 *                  |                                  \|/
 *  +---------------+-----------------------------------+---------------+
 *  |               |                                   |               |
 *  |       [ Socket.read() ]                    [ Socket.write() ]     |
 *  |                                                                   |
 *  |  Netty Internal I/O Threads (Transport Implementation)            |
 *  +-------------------------------------------------------------------+
```

从上图可以看出 消息读取和发送处理全流程为：

1. 底层的 SocketChannel.read()方法 读取 ByteBuf，触发 ChannelRead 事件，由 IO 线程 NioEventLoop 调用 ChannelPipeline 的 fireChannelRead(Object msg)方法，将消息传输到 ChannelPipeline 中。
2. 消息依次被 HeadHandler、ChannelHandler1、ChannelHandler2 … TailHandler 拦截和处理，在这个过程中，任何 ChannelHandler 都可以中断当前的流程，结束消息的传递。
3. 调用 ChannelHandlerContext 的 write 方法 发送消息，消息从 TailHandler 开始途经 ChannelHandlerN … ChannelHandler1、HeadHandler，最终被添加到消息发送缓冲区中等待刷新和发送，在此过程中也可以中断消息的传递，例如当编码失败时，就需要中断流程，构造异常的 Future 返回。

Netty 中的事件分为 Inbound 事件 和 Outbound 事件。Inbound 事件 通常由 I/O 线程 触发，例如 TCP 链路建立事件、链路关闭事件、读事件、异常通知事件等，它对应上图的左半部分。触发 Inbound 事件 的方法如下。

1. ChannelHandlerContext.fireChannelRegistered()：Channel 注册事件；
2. ChannelHandlerContext.fireChannelActive()：TCP 链路建立成功，Channel 激活事件；
3. ChannelHandlerContext.fireChannelRead(Object)：读事件；
4. ChannelHandlerContext.fireChannelReadComplete()：读操作完成通知事件；
5. ChannelHandlerContext.fireExceptionCaught(Throwable)：异常通知事件；
6. ChannelHandlerContext.fireUserEventTriggered(Object)：用户自定义事件；
7. ChannelHandlerContext.fireChannelWritabilityChanged()：Channel 的可写状态变化；
8. ChannelHandlerContext.fireChannellnactive()：TCP 连接关闭，链路不可用通知事件。

Outbound 事件 通常是由用户主动发起的 网络 IO 操作，例如用户发起的连接操作、绑定操作、消息发送等操作，它对应上图的右半部分。触发 Outbound 事件 的方法如下：

1. ChannelHandlerContext.bind(SocketAddress, ChannelPromise)：绑定本地地址事件；
2. ChannelHandlerContext.connect(SocketAddress, SocketAddress, ChannelPromise)：连接服务端事件；
3. ChannelHandlerContext.write(Object, ChannelPromise)：发送事件；
4. ChannelHandlerContext.flush()：刷新事件；
5. ChannelHandlerContext.read()：读事件；
6. ChannelHandlerContext.disconnect(ChannelPromise)：断开连接事件；
7. ChannelHandlerContext.close(ChannelPromise)：关闭当前 Channel 事件。

#### ChannelPipeline 自定义拦截器

ChannelPipeline 通过 ChannelHandler 来实现事件的拦截和处理，由于 ChannelHandler 中的事件种类繁多，不同的 ChannelHandler 可能只需要关心其中的个别事件，所以，自定义的 ChannelHandler 只需要继承 ChannelInboundHandlerAdapter / ChannelOutboundHandlerAdapter，覆盖自己关心的方法即可。

下面的两个示例分别展示了：拦截 Channel Active 事件，打印 TCP 链路建立成功日志，和 链路关闭的时候释放资源，代码如下。

```java
public class MyInboundHandler extends ChannelInboundHandlerAdapter {
	@Override
	public void channelActive(ChannelHandlerContext context) {
		System.out.println("欢迎来到，LPL！");
		context.fireChannelActive();
	}
}

public class MyOutboundHandler extends ChannelOutboundHandlerAdapter {
	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		System.out.println("游戏结束...");
		ctx.close();
	}
}
```

#### 构建 pipeline

使用 Netty 时，用户不需要自己创建 pipeline，因为使用 ServerBootstrap 或者 Bootstrap 进行配置后，Netty 会为每个 Channel 连接 创建一个独立的 pipeline。我们只需将自定义的 ChannelHandler 加入到 pipeline 即可。相关代码如下。

```java
ServerBootstrap server = new ServerBootstrap();
server.childHandler(new ChannelInitializer<SocketChannel>() {
    @Override
    public void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
    	/** 解析自定义协议 */
    	pipeline.addLast(new MyDecoder());
    	pipeline.addLast(new MyEncoder());
    	pipeline.addLast(new SocketHandler());
    	/** 解析Http请求 */
		pipeline.addLast(new HttpServerCodec());
		//主要是将同一个http请求或响应的多个消息对象变成一个 fullHttpRequest完整的消息对象
		pipeline.addLast(new HttpObjectAggregator(64 * 1024));
		//主要用于处理大数据流,比如一个1G大小的文件如果你直接传输肯定会撑暴jvm内存的 ,加上这个handler我们就不用考虑这个问题了
		pipeline.addLast(new ChunkedWriteHandler());
    }
});
```

对于类似编解码这样的 ChannelHandler，它存在先后顺序，例如 MessageToMessageDecoder，在它之前往往需要有 ByteToMessageDecoder 将 ByteBuf 解码为对象，然后将对象做二次解码 得到最终的 POJO 对象。pipeline 支持指定位置添加或者删除 ChannelHandler。

#### ChannelPipeline 的主要特性

ChannelPipeline 支持运行时动态的添加或者删除 ChannelHandler，在某些场景下这个特性非常实用。例如当业务高峰期需要对系统做拥塞保护时，就可以根据当前的系统时间进行判断，如果处于业务高峰期，则动态地将 系统拥塞保护 ChannelHandler 添加到当前的 ChannelPipeline 中，当高峰期过去之后，再动态删除 拥塞保护 ChannelHandler。

ChannelPipeline 是线程安全的，这意味着 N 个业务线程可以并发地操作 ChannelPipeline 而不存在多线程并发问题。但 ChannelHandler 不是线程安全的，这意味着 我们需要自己保证 ChannelHandler 的线程安全。

## ChannelPipeline 源码解析

ChannelPipeline 的代码比较简单，它实际上是一个 ChannelHandler 容器，内部维护了一个 ChannelHandler 的链表和迭代器，可以方便地进行 ChannelHandler 的 CRUD。

另外一个比较重要的部分是，当发生某个 I/O 事件 时，如 链路建立、链路关闭、读写操作 等，都会产一个事件，事件在 pipeline 中传播和处理，它是事件处理的总入口。由于 网络 I/O 相关的事件有限，因此 Netty 对这些事件进行了统一抽象，Netty 提供的 和用户自定义的 ChannelHandler 会对感兴趣的事件进行拦截和处理。

pipeline 中以 fireXXX 命名的方法都是从 I/O 线程 流向 用户业务 Handler 的 inbound 事件，它们的实现因功能而异，但是处理步骤类似，都是 调用 HeadHandler 对应的 fireXXX 方法，然后执行事件相关的逻辑操作。

```java
public interface ChannelPipeline
        extends ChannelInboundInvoker, ChannelOutboundInvoker, Iterable<Entry<String, ChannelHandler>> {

    /**
     * 管理 ChannelHandler 的api
     */
    ChannelPipeline addFirst(String name, ChannelHandler handler);

    ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler);

    ChannelPipeline addLast(String name, ChannelHandler handler);

    ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler);

    ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler);

    ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);

    ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler);

    ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler);

    ChannelPipeline addFirst(ChannelHandler... handlers);

    ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers);

    ChannelPipeline addLast(ChannelHandler... handlers);

    ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers);

    ChannelPipeline remove(ChannelHandler handler);

    ChannelHandler remove(String name);

    <T extends ChannelHandler> T remove(Class<T> handlerType);

    ChannelHandler removeFirst();

    ChannelHandler removeLast();

    ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler);

    ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler);

    <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler);

    ChannelHandler first();

    ChannelHandler last();

    ChannelHandler get(String name);

    <T extends ChannelHandler> T get(Class<T> handlerType);

    /**
     * 处理 I/O事件 的api
     */
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

    @Override
    ChannelPipeline flush();
}
```

## ChannelHandler 的功能和作用

ChannelHandler 负责对 I/O 事件 进行拦截处理，它可以选择性地 拦截处理感兴趣的事件，也可以透传和终止事件的传递。基于 ChannelHandler 接口，我们可以方便地进行业务逻辑定制，如 打印日志、统一封装异常信息、性能统计和消息编解码等。

#### ChannelHandlerAdapter

大部分 ChannelHandler 都会选择性 拦截处理感兴趣的 I/O 事件，忽略其他事件，然后交由下一个 ChannelHandler 进行拦截处理。这会导致一个问题：自定义 ChannelHandler 必须要实现 ChannelHandler 的所有接口，包括它不关心的那些事件处理接口，这会导致用户代码的冗余和臃肿，代码的可维护性也会变差。

为了解决这个问题，Netty 提供了 ChannelHandlerAdapter 基类，和 ChannelInboundHandlerAdapter / ChannelOutboundHandlerAdapter 两个实现类，如果 自定义 ChannelHandler 关心某个事件，只需要继承 ChannelInboundHandlerAdapter / ChannelOutboundHandlerAdapter 覆盖对应的方法即可，对于不关心的，可以直接继承使用父类的方法，这样子类的代码就会非常简洁清晰。

## ChannelHandler 组件 的类结构

相对于 ByteBuf 和 Channel，ChannelHandler 的类继承关系稍微简单些，但是它的子类非常多，功能各异，主要可以分为如下四类。

1. ChannelPipeline 的系统 ChannelHandler，用于 I/O 操作 和对事件进行预处理，对用户不可见，这类 ChannelHandler 主要包括 HeadHandler 和 TailHandler；
2. 编解码 ChannelHandler，如 MessageToMessageEncoder、MessageToMessageDecoder、MessageToMessageCodec；
3. 其他系统功能性 ChannelHandler，如 流量整型 Handler、读写超时 Handler、日志 Handler 等；
4. 自定义 ChannelHandler。

ChannelHandler 组件 的核心类及常用类的类图如下。



![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/20200830122632369-8baa92.png)

# Channel和Unsafe组件



```java
public abstract class AbstractNioChannel extends AbstractChannel {

        /**
         * 获取当前的连接状态进行缓存，然后发起连接操作。
         */
        @Override
        public final void connect(
                final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            try {
                if (connectPromise != null) {
                    // Already a connect in process.
                    throw new ConnectionPendingException();
                }

                boolean wasActive = isActive();
                if (doConnect(remoteAddress, localAddress)) {
                    fulfillConnectPromise(promise, wasActive);
                } else {
                    connectPromise = promise;
                    requestedRemoteAddress = remoteAddress;

                    // Schedule connect timeout.
                    int connectTimeoutMillis = config().getConnectTimeoutMillis();
                    if (connectTimeoutMillis > 0) {
                        connectTimeoutFuture = eventLoop().schedule(new Runnable() {
                            @Override
                            public void run() {
                                ChannelPromise connectPromise = AbstractNioChannel.this.connectPromise;
                                ConnectTimeoutException cause =
                                        new ConnectTimeoutException("connection timed out: " + remoteAddress);
                                if (connectPromise != null && connectPromise.tryFailure(cause)) {
                                    close(voidPromise());
                                }
                            }
                        }, connectTimeoutMillis, TimeUnit.MILLISECONDS);
                    }

                    promise.addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isCancelled()) {
                                if (connectTimeoutFuture != null) {
                                    connectTimeoutFuture.cancel(false);
                                }
                                connectPromise = null;
                                close(voidPromise());
                            }
                        }
                    });
                }
            } catch (Throwable t) {
                promise.tryFailure(annotateConnectException(t, remoteAddress));
                closeIfClosed();
            }
        }

        /**
         * 对 TCP三次握手连接结果 进行判断
         */
        @Override
        public final void finishConnect() {
            // Note this method is invoked by the event loop only if the connection attempt was
            // neither cancelled nor timed out.

            assert eventLoop().inEventLoop();

            try {
                boolean wasActive = isActive();
                doFinishConnect();
                fulfillConnectPromise(connectPromise, wasActive);
            } catch (Throwable t) {
                fulfillConnectPromise(connectPromise, annotateConnectException(t, requestedRemoteAddress));
            } finally {
                // Check for null as the connectTimeoutFuture is only created if a connectTimeoutMillis > 0 is used
                // See https://github.com/netty/netty/issues/1770
                if (connectTimeoutFuture != null) {
                    connectTimeoutFuture.cancel(false);
                }
                connectPromise = null;
            }
        }
	}
}
```

# EventLoop组件

## Netty 的线程模型

Netty 线程模型 的设计，也是基于 Reactor 模型，尽管不同的 NIO 框架 对于 Reactor 模式 的实现存在差异，但本质上还是遵循了 Reactor 的基础线程模型。

#### Reactor 单线程模型

Reactor 单线程模型，是指所有的 I/O 操作 都在同一个 NIO 线程 上完成。NIO 线程 的职责如下。

1. 作为 NIO 服务端，接收客户端的 TCP 连接；
2. 作为 NIO 客户端，向服务端发起 TCP 连接；
3. 读取通信对端的请求或者应答消息；
4. 向通信对端发送消息请求或者应答消息。

理论上一个 NIO 线程 可以独立处理所有 I/O 操作。例如，通过 Acceptor 类 接收客户端的 TCP 连接，链路建立成功后，通过 Dispatch 轮询事件就绪的 Channel，将事件分发到指定的 Handler 上进行事件处理。小容量应用场景下，可以使用单线程模型。但对于高负载、大并发的应用场景并不合用。

#### Reactor 多线程模型

Rector 多线程模型 与 单线程模型 最大的区别就是有一组 NIO 线程 来处理 I/O 操作，Reactor 多线程模型 的特点如下。

1. 有专门一个 NIO 线程 (Acceptor 线程) 用于监听服务端，接收客户端的 TCP 连接请求。
2. 网络 IO 操作 由一个 NIO 线程池 负责，由这些 NIO 线程 负责消息的 读取、解码、编码、发送。
3. 一个 NIO 线程 可以同时处理 N 条链路，但是一个链路只对应一个 NIO 线程，防止发生并发操作问题。

Reactor 多线程模型 可以满足大部分场景的需求。但对于 百万级超高并发 或 服务端需要对客户端进行安全认证，但认证非常消耗资源。在这类场景下，单独一个 Acceptor 线程 可能会处理不过来，成为系统的性能瓶颈。

#### Reactor 主从多线程模型

主从 Reactor 多线程模型的特点是，服务端用于接收客户端连接的是一个独立的 NIO 线程池。**Acceptor 线程 与客户端建立 TCP 连接 后，将新的 SocketChannel 注册到 NIO 线程池 的某个 NIO 线程 上，由该 NIO 线程 负责轮询 SocketChannel 上的 IO 事件，并进行事件处理**。

利用 主从多线程模型，可以解决一个服务端监听线程无法有效处理所有客户端连接的性能不足问题。在 Netty 的官方 Demo 中，也是推荐使用该线程模型。

#### Netty 多线程编程最佳实践

1. **如果业务逻辑比较简单，并且没有 数据库操作、线程阻塞的磁盘操作、网路操作等，可以直接在 NIO 线程 上完成业务逻辑编排，不需要切换到用户线程；**
2. **如果业务逻辑比较复杂，不要在 NIO 线程 上完成，建议将解码后的 POJO 消息 封装成 Task，分发到 业务线程池 中由业务线程执行，以保证 NIO 线程 尽快被释放，处理其他的 I/O 操作。**
3. **由于用户场景不同，对于一些复杂系统，很难根据 理论公式 计算出最优线程配置，只能是 结合公式给出一个相对合理的范围，然后对范围内的数据进行性能测试，选择相对最优配置。**

## NioEventLoop 源码解析

```java
public final class NioEventLoop extends SingleThreadEventLoop {

    /**
     * 作为 NIO框架 的 Reactor线程，NioEventLoop 需要处理 网络I/O读写事件，因此它必
     * 须聚合一个多路复用器对象 Selector
     */
    private Selector selector;
    // 通过 provider.open() 从操作系统底层获取 Selector实例
    private final SelectorProvider provider;

	/**
	 * 轮询 事件就绪的channel，进行 IO事件处理
	 */
    private void processSelectedKeys() {
        if (selectedKeys != null) {
            processSelectedKeysOptimized();
        } else {
            processSelectedKeysPlain(selector.selectedKeys());
        }
    }

    private void processSelectedKeysPlain(Set<SelectionKey> selectedKeys) {
        // check if the set is empty and if so just return to not create garbage by
        // creating a new Iterator every time even if there is nothing to process.
        // See https://github.com/netty/netty/issues/597
        if (selectedKeys.isEmpty()) {
            return;
        }

		// 这些代码在 nio编程中应该很熟悉咯
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            final Object a = k.attachment();
            i.remove();

            if (a instanceof AbstractNioChannel) {
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (!i.hasNext()) {
                break;
            }

            if (needsToSelectAgain) {
                selectAgain();
                selectedKeys = selector.selectedKeys();

                // Create the iterator again to avoid ConcurrentModificationException
                if (selectedKeys.isEmpty()) {
                    break;
                } else {
                    i = selectedKeys.iterator();
                }
            }
        }
    }

	/**
	 * 轮询 事件就绪的channel，进行 IO事件处理
	 */
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
    	// 获取 channel 的内部辅助类 Unsafe，通过 Unsafe 进行IO事件处理
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
            	// 获取要处理 channel 所绑定的 eventLoop线程，如果绑定的不是当前的 IO线程的事件，就不处理
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                // If the channel implementation throws an exception because there is no event loop, we ignore this
                // because we are only trying to determine if ch is registered to this event loop and thus has authority
                // to close ch.
                return;
            }
            // Only close ch if ch is still registered to this EventLoop. ch could have deregistered from the event loop
            // and thus the SelectionKey could be cancelled as part of the deregistration process, but the channel is
            // still healthy and should not be closed.
            // See https://github.com/netty/netty/issues/5125
            if (eventLoop != this || eventLoop == null) {
                return;
            }
            // close the channel if the key is not valid anymore
            unsafe.close(unsafe.voidPromise());
            return;
        }

        try {
            int readyOps = k.readyOps();
            // We first need to call finishConnect() before try to trigger a read(...) or write(...) as otherwise
            // the NIO JDK channel implementation may throw a NotYetConnectedException.
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                // remove OP_CONNECT as otherwise Selector.select(..) will always return without blocking
                // See https://github.com/netty/netty/issues/924
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);

                unsafe.finishConnect();
            }

            // 处理写事件
            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                // Call forceFlush which will also take care of clear the OP_WRITE once there is nothing left to write
                ch.unsafe().forceFlush();
            }

            // 处理读事件
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }
}
```

# **基于Netty开发服务端及客户端**

# 基于Netty的服务端开发

## Netty 服务端创建源码分析

当我们直接使用 JDK 的 NIO 类库 开发基于 NIO 的异步服务端时，需要用到 多路复用器 Selector、ServerSocketChannel、SocketChannel、ByteBuffer、SelectionKey 等，相比于传统的 BIO 开发，NIO 的开发要复杂很多，开发出稳定、高性能的异步通信框架，一直是个难题。Netty 为了向使用者屏蔽 NIO 通信 的底层细节，在和用户交互的边界做了封装，目的就是为了减少用户开发工作量，降低开发难度。ServerBootstrap 是 Socket 服务端 的启动辅助类，用户通过 ServerBootstrap 可以方便地创建 Netty 的服务端。

### Netty 服务端创建时序图

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/Netty%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%88%9B%E5%BB%BA%E6%97%B6%E5%BA%8F%E5%9B%BE-0c752c.png)

下面我们对 Netty 服务端创建 的关键步骤和原理进行详细解析。

1、**创建 ServerBootstrap 实例**。ServerBootstrap 是 Netty 服务端 的 启动辅助类，它提供了一系列的方法用于设置服务端启动相关的参数。底层对各种 原生 NIO 的 API 进行了封装，减少了用户与 底层 API 的接触，降低了开发难度。ServerBootstrap 中只有一个 public 的无参的构造函数可以给用户直接使用，ServerBootstrap 只开放一个无参的构造函数 的根本原因是 它的参数太多了，而且未来也可能会发生变化，为了解决这个问题，就需要引入 Builder 建造者模式。

2、**设置并绑定 Reactor 线程池**。Netty 的 Reactor 线程池 是 EventLoopGroup，它实际上是一个 EventLoop 数组。EventLoop 的职责是处理所有注册到本线程多路复用器 Selector 上的 Channel，Selector 的轮询操作由绑定的 EventLoop 线程 的 run()方法 驱动，在一个循环体内循环执行。值得说明的是，EventLoop 的职责不仅仅是处理 网络 IO 事件，用户自定义的 Task 和 定时任务 Task 也统一由 EventLoop 负责处理，这样线程模型就实现了统一。从调度层面看，也不存在从 EventLoop 线程 中再启动其他类型的线程用于异步执行另外的任务，这样就避免了多线程并发操作和锁竞争，提升了 IO 线程 的处理和调度性能。

3、**设置并绑定 服务端 Channel**。作为 NIO 服务端，需要创建 ServerSocketChannel，Netty 对 原生 NIO 类库 进行了封装，对应的实现是 NioServerSocketChannel。对于用户而言，不需要关心 服务端 Channel 的底层实现细节和工作原理，只需要指定具体使用哪种服务端 Channel 即可。因此，Netty 中 ServerBootstrap 的基类 提供了 channel()方法，用于指定 服务端 Channel 的类型。Netty 通过工厂类，利用反射创建 NioServerSocketChannel 对象。由于服务端监听端口往往只需要在系统启动时才会调用，因此反射对性能的影响并不大。相关代
码如下。

```java
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    /**
     * 通过 参数channelClass 创建一个 Channel实例，
     */
    public B channel(Class<? extends C> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        return channelFactory(new ReflectiveChannelFactory<C>(channelClass));
    }
}
```

4、**链路建立的时候创建并初始化 ChannelPipeline**。ChannelPipeline 并不是 NIO 服务端 必需的，它本质就是一个负责处理网络事件的职责链，负责管理和执行 ChannelHandler。网络事件以事件流的形式在 ChannelPipeline 中流转，由 ChannelPipeline 根据 ChannelHandler 的执行策略 调度 ChannelHandler 的执行。典型的网络事件如下。

1. 链路注册；
2. 链路激活；
3. 链路断开；
4. 接收到请求消息；
5. 请求消息接收并处理完毕；
6. 发送应答消息；
7. 链路发生异常；
8. 发生用户自定义事件。

5、**初始化 ChannelPipeline 完成之后，添加并设置 ChannelHandler**。ChannelHandler 是 Netty 提供给用户定制和扩展的关键接口。利用 ChannelHandler 用户可以完成大多数的功能定制，例如消息编解码、心跳、安全认证、TSL/SSL 认证、流量控制和流量整形等。Netty 同时也提供了大量的 系统 ChannelHandler 供用户使用，比较实用的 系统 ChannelHandler 总结如下。

1. 系统编解码框架，ByteToMessageCodec；
2. 基于长度的半包解码器，LengthFieldBasedFrameDecoder；
3. 码流日志打印 Handler，LoggingHandler；
4. SSL 安全认证 Handler，SslHandler；
5. 链路空闲检测 Handler，IdleStateHandler；
6. 流量整形 Handler，ChannelTrafficShapingHandler；
7. Base64 编解码，Base64Decoder 和 Base64Encoder。  
   创建和添加 ChannelHandler 的代码示例如下。

```java
    .childHandler( new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast( new EchoServerHandler() );
            }
    });
```

6、**绑定并启动监听端口**。在绑定监听端口之前系统会做一系列的初始化和检测工作，完成之后，会启动监听端口，并将 ServerSocketChannel 注册到 Selector 上监听客户端连接。

7、**Selector 轮询**。由 Reactor 线程 NioEventLoop 负责调度和执行 Selector 轮询操作，选择准备就绪的 Channel 集合，相关代码如下。

```java
public final class NioEventLoop extends SingleThreadEventLoop {

    private void select(boolean oldWakenUp) throws IOException {
        Selector selector = this.selector;

		......

        int selectedKeys = selector.select(timeoutMillis);
        selectCnt ++;

  		......

    }
}
```

8、**当轮询到 准备就绪的 Channel 之后，就由 Reactor 线程 NioEventLoop 执行 ChannelPipeline 的相应方法，最终调度并执行 ChannelHandler**，接口如下图所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/ChannelPipeline%E7%9A%84%E8%B0%83%E5%BA%A6%E7%9B%B8%E5%85%B3%E6%96%B9%E6%B3%95-a59cb7.png)

9、**执行 Netty 中 系统的 ChannelHandler 和 用户添加定制的 ChannelHandler** 。ChannelPipeline 根据网络事件的类型，调度并执行 ChannelHandler，相关代码如下。

```java
public class DefaultChannelPipeline implements ChannelPipeline {

    @Override
    public final ChannelPipeline fireChannelRead(Object msg) {
        AbstractChannelHandlerContext.invokeChannelRead(head, msg);
        return this;
    }
}
```

### 结合 Netty 源码 对服务端的创建过程进行解析

首先通过构造函数创建 ServerBootstrap 实例，随后，通常会创建两个 EventLoopGroup 实例 (也可以只创建一个并共享)，代码如下。

```java
	EventLoopGroup acceptorGroup = new NioEventLoopGroup();
	EventLoopGroup iOGroup = new NioEventLoopGroup();
```

NioEventLoopGroup 实际就是一个 Reactor 线程池，负责调度和执行客户端的接入、网络读写事件的处理、用户自定义任务和定时任务的执行。通过 ServerBootstrap 的 group()方法 将两个 EventLoopGroup 实例 传入，代码如下。

```java
public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {

    /**
     * Set the {@link EventLoopGroup} for the parent (acceptor) and the child (client). These
     * {@link EventLoopGroup}'s are used to handle all the events and IO for {@link ServerChannel} and
     * {@link Channel}'s.
     */
    public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
        super.group(parentGroup);
        if (childGroup == null) {
            throw new NullPointerException("childGroup");
        }
        if (this.childGroup != null) {
            throw new IllegalStateException("childGroup set already");
        }
        this.childGroup = childGroup;
        return this;
    }
}
```

其中 parentGroup 对象 被设置进了 ServerBootstrap 的父类 AbstractBootstrap 中，代码如下。

```java
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    volatile EventLoopGroup group;

    /**
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-created
     * {@link Channel}
     */
    public B group(EventLoopGroup group) {
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }
        this.group = group;
        return self();
    }
}
```

该方法会被客户端和服务端重用，用于设置 工作 IO 线程，执行和调度网络事件的读写。线程组和线程类型设置完成后，需要设置 服务端 Channel 用于端口监听和客户端链路接入。Netty 通过 Channel 工厂类 来创建不同类型的 Channel，对于服务端，需要创建 NioServerSocketChannel。所以，通过指定 Channel 类型 的方式创建 Channel 工厂。ReflectiveChannelFactory 可以根据 Channel 的类型 通过反射创建 Channel 的实例，服务端需要创建的是 NioServerSocketChannel 实例，代码如下。

```java
public class ReflectiveChannelFactory<T extends Channel> implements ChannelFactory<T> {

    private final Constructor<? extends T> constructor;

    public ReflectiveChannelFactory(Class<? extends T> clazz) {
        ObjectUtil.checkNotNull(clazz, "clazz");
        try {
            this.constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + StringUtil.simpleClassName(clazz) +
                    " does not have a public non-arg constructor", e);
        }
    }

    @Override
    public T newChannel() {
        try {
            return constructor.newInstance();
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class " + constructor.getDeclaringClass(), t);
        }
    }
}
```

指定 NioServerSocketChannel 后，需要设置 TCP 的一些参数，作为服务端，主要是设置 TCP 的 backlog 参数。

backlog 指定了内核为此套接口排队的最大连接个数，对于给定的监听套接口，内核要维护两个队列：未链接队列 和 已连接队列，根据 TCP 三次握手 的 三个子过程来分隔这两个队列。服务器处于 listen 状态 时，收到客户端 syn 过程(connect) 时在未完成队列中创建一个新的条目，然后用三次握手的第二个过程，即服务器的 syn 响应客户端，此条目在第三个过程到达前 (客户端对服务器 syn 的 ack) 一直保留在未完成连接队列中，如果三次握手完成，该条目将从未完成连接队列搬到已完成连接队列尾部。当进程调用 accept 时，从已完成队列中的头部取出一个条目给进程，当已完成队列为空时进程将睡眠，直到有条目在已完成连接队列中才唤醒。backlog 被规定为两个队列总和的最大值，大多数实现默认值为 5，但在高并发 Web 服务器 中此值显然不够。 需要设置此值更大一些的原因是，未完成连接队列的长度可能因为客户端 syn 的到达及等待三次握手的第三个过程延时 而增大。Netty 默认的 backlog 为 100，当然，用户可以修改默认值，这需要根据实际场景和网络状况进行灵活设置。

TCP 参数 设置完成后，用户可以为启动辅助类和其父类分别指定 Handler。两者 Handler 的用途不同：子类中的 Handler 是 NioServerSocketChannel 对应的 ChannelPipeline 的 Handler；父类中的 Handler 是客户端新接入的连接 SocketChannel 对应的 ChannelPipeline 的 Handler。两者的区别可以通过下图来展示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/ServerBootstrap%E7%9A%84Handler%E6%A8%A1%E5%9E%8B-83f81a.png)

本质区别就是：ServerBootstrap 中的 Handler 是 NioServerSocketChannel 使用的，所有连接该监听端口的客户端都会执行它；父类 AbstractBootstrap 中的 Handler 是个工厂类，它为每个新接入的客户端都创建一个新的 Handler。

服务端启动的最后一步，就是绑定本地端口，启动服务，下面我们来分析下这部分代码。

```java
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    private ChannelFuture doBind(final SocketAddress localAddress) {
        final ChannelFuture regFuture = initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        if (regFuture.isDone()) {
            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }
}
```

先看下上述代码调用的 initAndRegister()方法。它首先实例化了一个 NioServerSocketChannel 类型 的 Channel 对象。相关代码如下。

```java
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            channel = channelFactory.newChannel();
            init(channel);
        } catch (Throwable t) {
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();
                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }
        return regFuture;
    }
```

NioServerSocketChannel 创建成功后，对它进行初始化，初始化工作主要有以下三点。

```java
    @Override
    void init(Channel channel) throws Exception {
        final Map<ChannelOption<?>, Object> options = options0();
        synchronized (options) {
            setChannelOptions(channel, options, logger);
        }

        // 1、设置 Socket参数 和 NioServerSocketChannel 的附加属性
        final Map<AttributeKey<?>, Object> attrs = attrs0();
        synchronized (attrs) {
            for (Entry<AttributeKey<?>, Object> e: attrs.entrySet()) {
                @SuppressWarnings("unchecked")
                AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
                channel.attr(key).set(e.getValue());
            }
        }

        // 2、将 AbstractBootstrap 的 Handler 添加到 NioServerSocketChannel
        //    的 ChannelPipeline 中
        ChannelPipeline p = channel.pipeline();

        final EventLoopGroup currentChildGroup = childGroup;
        final ChannelHandler currentChildHandler = childHandler;
        final Entry<ChannelOption<?>, Object>[] currentChildOptions;
        final Entry<AttributeKey<?>, Object>[] currentChildAttrs;
        synchronized (childOptions) {
            currentChildOptions = childOptions.entrySet().toArray(newOptionArray(0));
        }
        synchronized (childAttrs) {
            currentChildAttrs = childAttrs.entrySet().toArray(newAttrArray(0));
        }

        // 3、将用于服务端注册的 Handler ServerBootstrapAcceptor 添加到 ChannelPipeline 中
        p.addLast(new ChannelInitializer<Channel>() {
            @Override
            public void initChannel(final Channel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                ChannelHandler handler = config.handler();
                if (handler != null) {
                    pipeline.addLast(handler);
                }

                ch.eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.addLast(new ServerBootstrapAcceptor(
                                ch, currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
                    }
                });
            }
        });
    }
```

到此，Netty 服务端监听的相关资源已经初始化完毕，就剩下最后一步，注册 NioServerSocketChannel 到 Reactor 线程 的多路复用器上，然后轮询客户端连接事件。在分析注册代码之前，我们先通过下图，看看目前 NioServerSocketChannel 的 ChannelPipeline 的组成。
![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/NioServerSocketChannel%E7%9A%84ChannelPipeline-feaf5e.png)
最后，我们看下 NioServerSocketChannel 的注册。当 NioServerSocketChannel 初始化完成之后，需要将它注册到 Reactor 线程 的多路复用器上监听新客户端的接入，代码如下。

```java
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {

    protected abstract class AbstractUnsafe implements Unsafe {

        /**
         * 将完成初始化的 NioServerSocketChannel 注册到 Reactor线程
         * 的多路复用器上，监听新客户端的接入
         */
        @Override
        public final void register(EventLoop eventLoop, final ChannelPromise promise) {

            ......

            // 首先判断是否是 NioEventLoop 自身发起的操作。如果是，则不存在并发操作，直接
            // 执行 Channel注册；如果由其他线程发起，则封装成一个 Task 放入消息队列中异步执行。
            // 此处，由于是由 ServerBootstrap 所在线程执行的注册操作，所以会将其封装成 Task 投递
            // 到 NioEventLoop 中执行
            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {

                    ......

                }
            }
        }

        private void register0(ChannelPromise promise) {
            try {
                // check if the channel is still open as it could be closed in the mean time when the register
                // call was outside of the eventLoop
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                // 该方法在本类中是一个空实现，下面看一下它在子类 AbstractNioChannel 中的实现
                doRegister();
                neverRegistered = false;
                registered = true;

                pipeline.invokeHandlerAddedIfNeeded();

                safeSetSuccess(promise);
                pipeline.fireChannelRegistered();
                if (isActive()) {
                    if (firstRegistration) {
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        beginRead();
                    }
                }
            } catch (Throwable t) {
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }
	}
}


public abstract class AbstractNioChannel extends AbstractChannel {

    @Override
    protected void doRegister() throws Exception {
        boolean selected = false;
        for (;;) {
            try {
            	// 将 NioServerSocketChannel 注册到 NioEventLoop 的 多路复用器Selector 上
                selectionKey = javaChannel().register(eventLoop().unwrappedSelector(), 0, this);
                return;
            } catch (CancelledKeyException e) {

                ......

            }
        }
    }
}
```

到此，服务端监听启动部分源码已经分析完成。

## 结合 Netty 源码 对客户端接入过程进行解析

负责处理网络读写、连接和客户端请求接入的 Reactor 线程 就是 NioEventLoop，下面我们看下 NioEventLoop 是如何处理新的客户端连接接入的。当 多路复用器 检测到新的准备就绪的 Channel 时，默认执行 processSelectedKeysOptimized()方法，代码如下。

```java
public final class NioEventLoop extends SingleThreadEventLoop {

    private void processSelectedKeys() {
        if (selectedKeys != null) {
            processSelectedKeysOptimized();
        } else {
            processSelectedKeysPlain(selector.selectedKeys());
        }
    }

    private void processSelectedKeysOptimized() {
        for (int i = 0; i < selectedKeys.size; ++i) {
            final SelectionKey k = selectedKeys.keys[i];
            selectedKeys.keys[i] = null;

            final Object a = k.attachment();

            if (a instanceof AbstractNioChannel) {
                // 根据就绪的操作位 SelectionKey，执行不同的操作
                processSelectedKey(k, (AbstractNioChannel) a);
            } else {
                @SuppressWarnings("unchecked")
                NioTask<SelectableChannel> task = (NioTask<SelectableChannel>) a;
                processSelectedKey(k, task);
            }

            if (needsToSelectAgain) {
                selectedKeys.reset(i + 1);
                selectAgain();
                i = -1;
            }
        }
    }

    // 根据就绪的操作位 SelectionKey，执行不同的操作
    private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) {
        // 由于不同的 Channel 执行不同的操作，所以 NioUnsafe 被设计成接口
        // 由不同的 Channel 内部的 NioUnsafe实现类 负责具体实现
        final AbstractNioChannel.NioUnsafe unsafe = ch.unsafe();
        if (!k.isValid()) {
            final EventLoop eventLoop;
            try {
                eventLoop = ch.eventLoop();
            } catch (Throwable ignored) {
                return;
            }
            if (eventLoop != this || eventLoop == null) {
                return;
            }
            unsafe.close(unsafe.voidPromise());
            return;
        }

        try {
            int readyOps = k.readyOps();
            if ((readyOps & SelectionKey.OP_CONNECT) != 0) {
                int ops = k.interestOps();
                ops &= ~SelectionKey.OP_CONNECT;
                k.interestOps(ops);
                unsafe.finishConnect();
            }

            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                ch.unsafe().forceFlush();
            }

            // read()方法 的实现有两个，分别是 NioByteUnsafe 和 NioMessageUnsafe，
            // 对于 NioServerSocketChannel，它使用的是 NioMessageUnsafe
            // 下面看一下 NioMessageUnsafe 对 read() 方法的实现
            if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                unsafe.read();
            }
        } catch (CancelledKeyException ignored) {
            unsafe.close(unsafe.voidPromise());
        }
    }
}


public abstract class AbstractNioMessageChannel extends AbstractNioChannel {

    private final class NioMessageUnsafe extends AbstractNioUnsafe {

        private final List<Object> readBuf = new ArrayList<Object>();

        @Override
        public void read() {
            assert eventLoop().inEventLoop();
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
            allocHandle.reset(config);

            boolean closed = false;
            Throwable exception = null;
            try {
                try {
                    do {
                        // 接收新的客户端连接并创建 NioSocketChannel
                        int localRead = doReadMessages(readBuf);
                        if (localRead == 0) {
                            break;
                        }
                        if (localRead < 0) {
                            closed = true;
                            break;
                        }
                        allocHandle.incMessagesRead(localRead);
                    } while (allocHandle.continueReading());
                } catch (Throwable t) {
                    exception = t;
                }

                int size = readBuf.size();
                for (int i = 0; i < size; i ++) {
                    readPending = false;
                    // 接收到新的客户端连接后，触发 ChannelPipeline 的 channelRead方法。
                    // 事件在 ChannelPipeline 中传递，执行 ServerBootstrapAcceptor 的
                    // channelRead方法
                    pipeline.fireChannelRead(readBuf.get(i));
                }

                ......

            }
        }
    }
}


public class NioServerSocketChannel extends AbstractNioMessageChannel
                             implements io.netty.channel.socket.ServerSocketChannel {

    /**
     * 接收新的客户端连接并创建 NioSocketChannel
     */
    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        SocketChannel ch = SocketUtils.accept(javaChannel());

        try {
            if (ch != null) {
                buf.add(new NioSocketChannel(this, ch));
                return 1;
            }
        } catch (Throwable t) {

            ......

        }
        return 0;
    }
}


public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {

    private static class ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter {

        /**
         * 该方法主要分为如下三个步骤。
         */
        @Override
        @SuppressWarnings("unchecked")
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            final Channel child = (Channel) msg;
			// 第一步:将启动时传入的 childHandler 加入到客户端 SocketChannel 的 ChannelPipeline 中
            child.pipeline().addLast(childHandler);
			// 第二步:设置客户端 SocketChannel 的 TCP参数
            setChannelOptions(child, childOptions, logger);
            for (Entry<AttributeKey<?>, Object> e: childAttrs) {
                child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
            }
			// 第三步:注册 SocketChannel 到多路复用器
            try {
                childGroup.register(child).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            forceClose(child, future.cause());
                        }
                    }
                });
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }
	}
}
```

下面我们展开看下 NioSocketChannel 的 register()方法。NioSocketChannel 的注册方法与 ServerSocketChannel 的一致, 也是将 Channel 注册到 Reactor 线程 的多路复用器上。由于注册的操作位是 0，所以，此时 NioSocketChannel 还不能读取客户端发送的消息，下面我们看看 是什么时候修改监听操作位为 OP_READ 的。

执行完注册操作之后，紧接着会触发 ChannelReadComplete 事件。我们继续分析 ChannelReadComplete 在 ChannelPipeline 中的处理流程：Netty 的 Header 和 Tail 本身不关注 ChannelReadComplete 事件 就直接透传，执行完 ChannelReadComplete 后，接着执行 PipeLine 的 read()方法，最终执行 HeadHandler 的 read()方法。

HeadHandler 的 read()方法用来将网络操作位修改为读操作。创建 NioSocketChannel 的时候已经将 AbstractNioChannel 的 readInterestOp 设置为 OP\_ READ，这样，执行 selectionKey. interestOps(interestOps | readInterestOp)操作 时就会把操作位设置为 OP_READ。代码如下。

```java
public abstract class AbstractNioByteChannel extends AbstractNioChannel {

    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);
    }
}
```

到此，新接入的客户端连接处理完成，可以进行网络读写等 IO 操作。

# 基于Netty的客户端开发

相对于服务端，Netty 客户端 的创建更加复杂，除了要考虑线程模型、异步连接、客户端连接超时等因素外，还需要对连接过程中的各种异常进行考虑。本章将对 Netty 客户端 创建的关键流程和源码进行分析，以期读者能够了解客户端创建的细节。

## 基于 Netty 创建客户端的流程分析

Netty 为了向使用者屏蔽 NIO 通信 的底层细节，在和用户交互的边界做了封装，目的就是为了减少用户开发工作量，降低开发难度。Bootstrap 是 Socket 客户端创建工具类，用户通过 Bootstrap 可以方便地创建 Netty 的客户端并发起 异步 TCP 连接操作。

### 基于 Netty 创建客户端 时序图

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E5%9F%BA%E4%BA%8ENetty%E5%88%9B%E5%BB%BA%E5%AE%A2%E6%88%B7%E7%AB%AF%E6%97%B6%E5%BA%8F%E5%9B%BE-2e4c80.png)

### Netty 创建客户端 流程分析

1. 用户线程创建 Bootstrap 实例，通过 API 设置客户端相关的参数，异步发起客户端连接；
2. 创建处理客户端连接、I/O 读写的 Reactor 线程组 NioEventLoopGroup。可以通过构造函数指定 IO 线程 的个数，默认为 CPU 内核数的 2 倍；
3. 通过 Bootstrap 的 ChannelFactory 和用户指定的 Channel 类型 创建用于客户端连接的 NioSocketChannel，它的功能类似于 JDK NIO 类库 提供的 SocketChannel；
4. 创建默认的 Channel、Handler、Pipeline，用于调度和执行网络事件；
5. 异步发起 TCP 连接，判断连接是否成功。如果成功，则直接将 NioSocketChannel 注册到多路复用器上，监听读操作位，用于数据报读取和消息发送；如果没有立即连接成功，则注册连接监听位到多路复用器，等待连接结果；
6. 注册对应的网络监听状态位到多路复用器；
7. 由多路复用器在 IO 现场中轮询各 Channel，处理连接结果；
8. 如果连接成功，设置 Future 结果，发送连接成功事件，触发 ChanneIPipeline 执行；
9. 由 ChannelPipeline 调度执行系统和用户的 ChannelHandler，执行业务逻辑。

## Netty 客户端创建源码分析

Netty 客户端 的创建流程比较繁琐，下面我们针对关键步骤和代码进行分析，通过梳理关键流程来掌握客户端创建的原理。

### 客户端连接辅助类 BootStrap

Bootstrap 是 Netty 提供的客户端连接工具类，主要用于简化客户端的创建，下面我们对它的 主要 API 进行讲解。

设置 lO 线程组：NIO 的特点就是，一个多路复用器可以同时处理上干条链路，这就意味着，NIO 模式中 一个线程可以处理多个 TCP 连接。考虑到 lO 线程 的处理性能，大多数 NIO 框架 都采用线程池的方式处理 IO 读写，Netty 也不例外。客户端相对于服务端，只需要一个处理 IO 读写 的线程组即可，因为 Bootstrap 提供了 设置 IO 线程组 的接口，代码如下。

```java
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    volatile EventLoopGroup group;

    public B group(EventLoopGroup group) {
        if (group == null) {
            throw new NullPointerException("group");
        } else if (this.group != null) {
            throw new IllegalStateException("group set already");
        } else {
            this.group = group;
            return this;
        }
    }
}
```

由于 Netty 的 NIO 线程组 默认采用 EventLoopGroup 接口，因此线程组参数使用 EventLoopGroup。

TCP 参数设置接口：无论是 NIO，还是 BIO，创建客户端套接字的时候通常都会设置连接参数，例如接收和发送缓冲区大小、连接超时时间等。Bootstrap 也提供了客户端 TCP 参数设置接口，代码如下。

```java
public <T> B option(ChannelOption<T> option, T value) {
    if (option == null) {
        throw new NullPointerException("option");
    } else {
        if (value == null) {
            synchronized(this.options) {
                this.options.remove(option);
            }
        } else {
            synchronized(this.options) {
                this.options.put(option, value);
            }
        }

        return this;
    }
}
```

Netty 提供的 主要 TCP 参数 如下。

1、SO_TIMEOUT：控制读取操作将阻塞多少毫秒。如果返回值为 0，计时器就被禁止了，该线程将无限期阻塞；
2、SO_SNDBUF：套接字使用的发送缓冲区大小；
3、SO_RCVBUF：套接字使用的接收缓冲区大小；
4、SO_REUSEADDR：用于决定 如果网络上仍然有数据向旧的 ServerSocket 传输数据，是否允许新的 ServerSocket 绑定到与旧的 ServerSocket 同样的端口上。SO_REUSEADDR 选项 的默认值与操作系统有关，在某些操作系统中，允许重用端口，而在某些操作系统中不允许重用端口；
5、CONNECT_TIMEOUT_MILLIS：客户端连接超时时间，由于 NIO 原生的客户端 并不提供设置连接超时的接口，因此，Netty 采用的是自定义连接超时定时器负责检测和超时控制；

Channel 接口：用于指定客户端使用的 Channel 接口，对于 TCP 客户端连接，默认使用 NioSocketChannel，代码如下。

```java
public B channel(Class<? extends C> channelClass) {
    if (channelClass == null) {
        throw new NullPointerException("channelClass");
    } else {
        return this.channelFactory((io.netty.channel.ChannelFactory)(new ReflectiveChannelFactory(channelClass)));
    }
}
```

BootstrapChannelFactory 利用 参数 channelClass，通过反射机制创建 NioSocketChannel 对象。

设置 Handler 接口：Bootstrap 为了简化 Handler 的编排，提供了 Channellnitializer，它继承了 ChannelHandlerAdapter，当 TCP 链路 注册成功之后，调用 initChannel 接口，用于设置用户 ChanneIHandler。它的代码如下。

```java
public abstract class ChannelInitializer<C extends Channel> extends ChannelInboundHandlerAdapter {

    public final void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (this.initChannel(ctx)) {
            ctx.pipeline().fireChannelRegistered();
        } else {
            ctx.fireChannelRegistered();
        }
    }
}
```

最后一个比较重要的接口就是发起客户端连接，代码如下。

```java
ChannelFuture f = b.connect(host, port).sync();
```

### 客户端连接操作

首先要创建和初始化 NioSocketChannel，代码如下。

```java
public class Bootstrap extends AbstractBootstrap<Bootstrap, Channel> {

    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        if (remoteAddress == null) {
            throw new NullPointerException("remoteAddress");
        } else {
            this.validate();
            return this.doResolveAndConnect(remoteAddress, localAddress);
        }
    }

    private ChannelFuture doResolveAndConnect(final SocketAddress remoteAddress, final SocketAddress localAddress) {
    	// 首先要创建和初始化 NioSocketChannel
        ChannelFuture regFuture = this.initAndRegister();
        final Channel channel = regFuture.channel();
        if (regFuture.isDone()) {
        	// 初始化 Channel 之后，将其注册到 Selector 上
            return !regFuture.isSuccess() ? regFuture : this.doResolveAndConnect0(channel, remoteAddress, localAddress, channel.newPromise());
        } else {
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            regFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        promise.setFailure(cause);
                    } else {
                        promise.registered();
                        Bootstrap.this.doResolveAndConnect0(channel, remoteAddress, localAddress, promise);
                    }

                }
            });
            return promise;
        }
    }

    private ChannelFuture doResolveAndConnect0(final Channel channel, SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise promise) {
        try {
            EventLoop eventLoop = channel.eventLoop();
            AddressResolver<SocketAddress> resolver = this.resolver.getResolver(eventLoop);
            if (!resolver.isSupported(remoteAddress) || resolver.isResolved(remoteAddress)) {
                doConnect(remoteAddress, localAddress, promise);
                return promise;
            }

            Future<SocketAddress> resolveFuture = resolver.resolve(remoteAddress);
            if (resolveFuture.isDone()) {
                Throwable resolveFailureCause = resolveFuture.cause();
                if (resolveFailureCause != null) {
                    channel.close();
                    promise.setFailure(resolveFailureCause);
                } else {
                    doConnect((SocketAddress)resolveFuture.getNow(), localAddress, promise);
                }

                return promise;
            }

            resolveFuture.addListener(new FutureListener<SocketAddress>() {
                public void operationComplete(Future<SocketAddress> future) throws Exception {
                    if (future.cause() != null) {
                        channel.close();
                        promise.setFailure(future.cause());
                    } else {
                        Bootstrap.doConnect((SocketAddress)future.getNow(), localAddress, promise);
                    }

                }
            });
        } catch (Throwable var9) {
            promise.tryFailure(var9);
        }

        return promise;
    }

    private static void doConnect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelPromise connectPromise) {
        final Channel channel = connectPromise.channel();
        channel.eventLoop().execute(new Runnable() {
            public void run() {
                if (localAddress == null) {
                    channel.connect(remoteAddress, connectPromise);
                } else {
                    channel.connect(remoteAddress, localAddress, connectPromise);
                }

                connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        });
    }
}
```

需要注意的是，SocketChannel 执行 connect() 操作后有以下三种结果。

1. 连接成功，返回 True；
2. 暂时没有连接上，服务端没有返回 ACK 应答，连接结果不确定，返回 False；
3. 连接失败，直接抛出 IO 异常。

如果是第二种结果，需要将 NioSocketChannel 中的 selectionKey 设置为 OP_CONNECT，监听连接结果。异步连接返回之后，需要判断连接结果，如果连接成功，则触发 ChannelActive 事件。ChannelActive 事件 最终会将 NioSocketChannel 中的 selectionKey 设置为 SelectionKey.OP_READ，用于监听网络读操作。如果没有立即连接上服务端，则注册 SelectionKey.OP_CONNECT 到多路复用器。如果连接过程发生异常，则关闭链路，进入连接失败处理流程。

### 异步连接结果通知

NioEventLoop 的 Selector 轮询 客户端连接 Channel，当服务端返回握手应答之后，对连接结果进行判断，代码如下。

```java
if ((readyOps & 8) != 0) {
    int ops = k.interestOps();
    ops &= -9;
    k.interestOps(ops);
    unsafe.finishConnect();
}
```

下面对 finishConnect()方法 进行分析，代码如下。

```java
try {
    boolean wasActive = AbstractNioChannel.this.isActive();
    AbstractNioChannel.this.doFinishConnect();
    this.fulfillConnectPromise(AbstractNioChannel.this.connectPromise, wasActive);
} catch (Throwable var5) {
    ......
}
```

doFinishConnect()方法 用于判断 JDK 的 SocketChannel 的连接结果，如果未出错 表示连接成功，其他值或者发生异常表示连接失败。

```java
protected void doFinishConnect() throws Exception {
    if (!this.javaChannel().finishConnect()) {
        throw new Error();
    }
}
```

连接成功之后，调用 fufillConectPromise()方法，触发链路激活事件，该事件由 ChannelPipeline 进行传播。

### 客户端连接超时机制

对于 SocketChannel 接口，JDK 并没有提供连接超时机制，需要 NIO 框架或者用户自己扩展实现。Netty 利用定时器提供了客户端连接超时控制功能，下面我们对该功能进行详细讲解。

首先,用户在创建 Netty 客户端的时候，可以通过 ChannelOption.CONNECT_TIMEOUT_MILLIS 配置项设置连接超时时间，代码如下。

```java
Bootstrap b = new Bootstrap();
b.group(workerGroup);
b.channel(NioSocketChannel.class);
b.option(ChannelOption.SO_KEEPALIVE, true);
b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
```

# **IO技术基础IOTechnologyBase**

# IO模型

## Linux 网络 IO 模型简介

Linux 的内核将所有外部设备都看做一个文件来操作，对一个文件的读写操作会调用内核提供的系统命令，返回一个 fd (file descriptor，文件描述符)。而对一个 socket 的读写也会有相应的描述符，称为 socket fd (socket 描述符)，描述符就是一个数字，它指向内核中的一个结构体(文件路径，数据区等一些属性)。根据 UNIX 网络编程对 I/O 模型 的分类，UNIX 提供了 5 种 I/O 模型，分别如下。

#### 1、阻塞 IO 模型

在内核将数据准备好之前，系统调用会一直等待所有的套接字（Socket）传来数据，默认的是阻塞方式。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E9%98%BB%E5%A1%9EIO%E6%A8%A1%E5%9E%8B-781d28.png)

Java 中的 socket.read()方法 最终会调用底层操作系统的 recvfrom 方法，OS 会判断来自网络的数据报是否准备好，当数据报准备好了之后，OS 就会将数据从内核空间拷贝到用户空间（因为我们的用户程序只能获取用户空间的内存，无法直接获取内核空间的内存）。拷贝完成之后 socket.read() 就会解除阻塞，并得到网络数据的结果。

BIO 中的阻塞，就是阻塞在 2 个地方：

1. OS 等待数据报通过网络发送过来，如果建立连接后数据一直没过来，就会白白浪费线程的资源；
2. 将数据从内核空间拷贝到用户空间。

在这 2 个时候，我们的线程会一直被阻塞，啥事情都不干。

#### 2、非阻塞 IO 模型

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E9%9D%9E%E9%98%BB%E5%A1%9EIO%E6%A8%A1%E5%9E%8B-fdb068.png)

每次应用程序询问内核是否有数据报准备好，当有数据报准备好时，就进行拷贝数据报的操作，从内核拷贝到用户空间，和拷贝完成返回的这段时间，应用进程是阻塞的。但在没有数据报准备好时，并不会阻塞程序，内核直接返回未准备好的信号，等待应用进程的下一次询问。但是，轮寻对于 CPU 来说是较大的浪费，一般只有在特定的场景下才使用。

从图中可以看到，非阻塞 IO 的 recvfrom 调用 会立即得到一个返回结果(数据报是否准备好)，我们可以根据返回结果继续执行不同的逻辑。而阻塞 IO 的 recvfrom 调用，如果无数据报准备好，一定会被阻塞住。虽然 非阻塞 IO 比 阻塞 IO 少了一段阻塞的过程，但事实上 非阻塞 IO 这种方式也是低效的，因为我们不得不使用轮询方法区一直问 OS：“我的数据好了没啊”。

**BIO 不会在 拷贝数据之前 阻塞，但会在将数据从内核空间拷贝到用户空间时阻塞。一定要注意这个地方，Non-Blocking 还是会阻塞的。**

#### 3、IO 复用模型

Linux 提供 select/poll，进程通过将一个或多个 fd 传递给 select 或 poll 系统 调用，阻塞发生在 select/poll 操作上。select/poll 可以帮我们侦测多个 fd 是否处于就绪状态，它们顺序扫描 fd 是否就绪，但支持的 fd 数量有限，因此它的使用也受到了一些制约。Linux 还提供了一个 epoll 系统调用，epoll 使用 基于事件驱动方式 代替 顺序扫描，因此性能更高，当有 fd 就绪时，立即回调函数 rollback。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/IO%E5%A4%8D%E7%94%A8%E6%A8%A1%E5%9E%8B-2c174c.png)

#### 4、信号驱动 IO 模型

首先开启套接口信号驱动 IO 功能，并通过系统调用 sigaction 执行一个信号处理函数（此系统调用立即返回，进程继续工作，它是非阻塞的）。当数据准备就绪时，就为该进程生成一个 SIGIO 信号，通过信号回调通知应用程序调用 recvfrom 来读取数据，并通知主循环函数处理数据。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E4%BF%A1%E5%8F%B7%E9%A9%B1%E5%8A%A8IO%E6%A8%A1%E5%9E%8B-944f01.png)

#### 5、异步 IO 模型

告知内核启动某个操作，并让内核在整个操作完成后(包括将数据从内核复制到用户自己的缓冲区)通知我们。这种模型与信号驱动模型的主要区别是：信号驱动 IO 由内核通知我们何时可以开始一个 IO 操作；异步 IO 模型 由内核通知我们 IO 操作何时已经完成。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E5%BC%82%E6%AD%A5IO%E6%A8%A1%E5%9E%8B-274b85.png)

从这五种 IO 模型的结构 也可以看出，阻塞程度：阻塞 IO>非阻塞 IO>多路转接 IO>信号驱动 IO>异步 IO，效率是由低到高的。

最后，我们看一下数据从客户端到服务器，再由服务器返回结果数据的整体 IO 流程，以便我们更好地理解上述的 IO 模型。
![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E6%95%B0%E6%8D%AE%E5%9C%A8%E5%AE%A2%E6%88%B7%E7%AB%AF%E5%8F%8A%E6%9C%8D%E5%8A%A1%E5%99%A8%E4%B9%8B%E9%97%B4%E7%9A%84%E6%95%B4%E4%BD%93IO%E6%B5%81%E7%A8%8B-f9b664.png)

## IO 多路复用技术

Java NIO 的核心类库中 多路复用器 Selector 就是基于 epoll 的多路复用技术实现。

在 IO 编程 过程中，当需要同时处理多个客户端接入请求时，可以利用多线程或者 IO 多路复用技术 进行处理。IO 多路复用技术 通过把多个 IO 的阻塞复用到同一个 select 的阻塞上，从而使得系统在单线程的情况下可以同时处理多个客户端请求。与传统的多线程/多进程模型比，IO 多路复用 的最大优势是系统开销小，系统不需要创建新的额外进程或线程，也不需要维护这些进程和线程的运行，降低了系统的维护工作量，节省了系统资源，IO 多路复用 的主要应用场景如下。

- 服务器需要同时处理多个处于监听状态或者多个连接状态的套接字;
- 服务器需要同时处理多种网络协议的套接字。

目前支持 IO 多路复用 的系统调用有 select、pselect、poll、epoll，在 Linux 网络编程 过程中，很长一段时间都使用 select 做轮询和网络事件通知，然而 select 的一些固有缺陷导致了它的应用受到了很大的限制，最终 Linux 选择了 epoll。epoll 与 select 的原理比较类似，为了克服 select 的缺点，epoll 作了很多重大改进，现总结如下。

1. 支持一个进程打开的 socket 描述符 (fd) 不受限制(仅受限于操作系统的最大文件句柄数)；
2. IO 效率 不会随着 FD 数目的增加而线性下降；
3. epoll 的 API 更加简单。

值得说明的是，用来克服 select/poll 缺点的方法不只有 epoll, epoll 只是一种 Linux 的实现方案。

## Selector、SelectionKey及Channel组件

Selector、SelectionKey 和 Channel 这三个组件构成了 Java nio 包的核心，也是 Reactor 模型在代码层面的体现。Selector 能让单线程同时处理多个客户端 Channel，非常适用于高并发，传输数据量较小的场景。要使用 Selector，首先要将对应的 Channel 及 IO 事件（读、写、连接）注册到 Selector，注册后会产生一个 SelectionKey 对象，用于关联 Selector 和 Channel，及后续的 IO 事件处理。这三者的关系如下图所示。

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/Selector%E5%92%8CSelectionKey%E5%92%8CChannel%E5%85%B3%E7%B3%BB%E5%9B%BE-665d44.png)

对 nio 编程不熟的同学可以搜索一些简单的 demo 跑一下，下面 我们直接进入源码，窥探一些 nio 的奥秘。

### Selector

其实，不管是 Selector 还是 SelectionKey 的源码，其具体实现类都是依赖于底层操作系统的，这里我们只看一下抽象类 Selector 的源码，日后有事件，再找一些具体的实现类深入分析一下。

```java
public abstract class Selector implements Closeable {

    protected Selector() { }

    /**
     * 获取一个 Selector对象，具体实现依赖于底层操作系统
     */
    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /**
     * 判断该 Selector 是否已开启
     */
    public abstract boolean isOpen();

    /**
     * 当前所有向Selector注册的Channel 所对应的SelectionKey的集合
     */
    public abstract Set<SelectionKey> keys();

    /**
     * 相关事件已经被 Selector 捕获的 SelectionKey的集合
     */
    public abstract Set<SelectionKey> selectedKeys();

    /**
     * 阻塞到至少有一个通道在你注册的事件上就绪了
     */
    public abstract int select() throws IOException;

    /**
     * 和select()一样，除了最长会阻塞timeout毫秒
     */
    public abstract int select(long timeout) throws IOException;

    /**
     * 此方法执行非阻塞的选择操作，如果自从上一次选择操作后，
     * 没有通道变成可选择的，则此方法直接返回 0
     */
    public abstract int selectNow() throws IOException;

    /**
     * 用完Selector后调用其close()方法会关闭该Selector，且使注册到该Selector上的所有SelectionKey实例无效
     * 通道本身并不会关闭
     */
    public abstract void close() throws IOException;
}
```

### SelectionKey

表示 SelectableChannel 在 Selector 中的注册的标记 / 句柄。

```java
public abstract class SelectionKey {

    protected SelectionKey() { }


    // -- Channel and selector operations --

    /**
     * 获取该 SelectionKey 对应的Channel，Channel注册到Selector时会产生该 SelectionKey对象
     */
    public abstract SelectableChannel channel();

    /**
     * 获取该 SelectionKey 对应的 Selector
     */
    public abstract Selector selector();

    /**
     * 该 SelectionKey 是否是有效的
     */
    public abstract boolean isValid();

    // ------ Operation-set accessors ------

    /**
     * 获取该 SelectionKey 的兴趣事件 (既 SelectionKey 的4个 事件静态常量)
     */
    public abstract int interestOps();

    /**
     * 设置该 SelectionKey 的兴趣事件
     */
    public abstract SelectionKey interestOps(int ops);

    /**
     * 获取该 SelectionKey 的已操作集
     */
    public abstract int readyOps();


    // ------ Operation bits and bit-testing convenience methods ------

    /**
     * channel中的数据是否已经可以读取
     */
    public static final int OP_READ = 1 << 0;

    /**
     * channel是否可以开始写入数据
     */
    public static final int OP_WRITE = 1 << 2;

    /**
     * channel是否已经建立连接
     */
    public static final int OP_CONNECT = 1 << 3;

    /**
     * ServerSocketChannel 是否可以与客户端建立连接
     */
    public static final int OP_ACCEPT = 1 << 4;

    /**
     * channel是否可读
     */
    public final boolean isReadable() {
        return (readyOps() & OP_READ) != 0;
    }

    /**
     * channel是否可写
     */
    public final boolean isWritable() {
        return (readyOps() & OP_WRITE) != 0;
    }

    /**
     * channel是否建立连接
     */
    public final boolean isConnectable() {
        return (readyOps() & OP_CONNECT) != 0;
    }

    /**
     * ServerSocketChannel是否可与客户端channel建立连接
     */
    public final boolean isAcceptable() {
        return (readyOps() & OP_ACCEPT) != 0;
    }
}
```

### Channel 组件

平时编码用的比较多的就是 SocketChannel 和 ServerSocketChannel，而将 Channel 与 Selecor 关联到一起的核心 API 则定义在它们的公共父类 SelectableChannel 中，整个 Channel 组件的核心类图如下所示。

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/Channel%E7%BB%84%E4%BB%B6-f34fc4.png)

#### SelectableChannel

```java
public abstract class SelectableChannel extends AbstractInterruptibleChannel implements Channel {

    protected SelectableChannel() { }

    /**
     * 当前channel是否注册到了某个selector上，新创建的channel都是未注册状态
     */
    public abstract boolean isRegistered();

    /**
     * 根据给定的 Selector，获取本channel注册上去的 SelectionKey
     */
    public abstract SelectionKey keyFor(Selector sel);

    /**
     * 将当前channel及关注的事件，注册到Selector上，返回一个 SelectionKey
     */
    public final SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
        return register(sel, ops, null);
    }

    public abstract SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException;

    /**
     * 设置该channel的阻塞模式，默认为 true阻塞
     */
    public abstract SelectableChannel configureBlocking(boolean block) throws IOException;

    /**
     * 是否为阻塞IO模式
     */
    public abstract boolean isBlocking();
}
```

#### ServerSocketChannel

相当于 BIO 中的 ServerSocket，主要用于服务端与客户端建立连接通信的 channel。

```java
public abstract class ServerSocketChannel extends AbstractSelectableChannel implements NetworkChannel {

    protected ServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * 获取一个 ServerSocketChannel实例，具体实现依赖底层操作系统
     */
    public static ServerSocketChannel open() throws IOException {
        return SelectorProvider.provider().openServerSocketChannel();
    }

    // -- ServerSocket-specific operations --

    /**
     * 绑定ip地址及要监听的端口
     */
    public final ServerSocketChannel bind(SocketAddress local) throws IOException {
        return bind(local, 0);
    }

    public abstract ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException;

    /**
     * 与一个客户端channel建立连接，返回该客户端的存根 SocketChannel
     */
    public abstract SocketChannel accept() throws IOException;
}
```

#### SocketChannel

相当于 BIO 中的 Socket，主要用于通信双方的读写操作。

```java
public abstract class SocketChannel extends AbstractSelectableChannel
    	implements ByteChannel, ScatteringByteChannel, GatheringByteChannel, NetworkChannel {

    protected SocketChannel(SelectorProvider provider) {
        super(provider);
    }

    /**
     * 根据 SocketAddress 获取一个 SocketChannel，具体实现依赖底层操作系统
     */
    public static SocketChannel open(SocketAddress remote) throws IOException {
        SocketChannel sc = open();
        try {
            sc.connect(remote);
        } catch (Throwable x) {
            try {
                sc.close();
            } catch (Throwable suppressed) {
                x.addSuppressed(suppressed);
            }
            throw x;
        }
        assert sc.isConnected();
        return sc;
    }

    public static SocketChannel open() throws IOException {
        return SelectorProvider.provider().openSocketChannel();
    }

    // -- Socket-specific operations --

    /**
     * 绑定要连接的远程服务的ip及端口
     */
    @Override
    public abstract SocketChannel bind(SocketAddress local) throws IOException;

    /**
     * 该channel与服务端是否已连接
     */
    public abstract boolean isConnected();

    // -- ByteChannel operations --

    /**
     * 将 channel 中的数据读到 ByteBuffer
     */
    public abstract int read(ByteBuffer dst) throws IOException;

    public final long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    public abstract long read(ByteBuffer[] dsts, int offset, int length) throws IOException;

    /**
     * 将 ByteBuffer 中的数据写到 channel
     */
    public abstract int write(ByteBuffer src) throws IOException;

    public final long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }

    public abstract long write(ByteBuffer[] srcs, int offset, int length) throws IOException;
}
```

网上关于各种 IO 的博文已经多到飞起，如果你是大神，可以跳过我这个菜鸟的拙文，本博文没有什么特别 NB 的东西，只是集百家之长，并且以自己感到简单舒适的方式输出自己的理解，及学习过程中的经验。

> **把被说烂的BIO、NIO、AIO再从头到尾扯一遍** 

## IO 及基本概念

#### 1、流的概念和作用

**流**：代表任何有能力产出数据的数据源对象或者是有能力接受数据的接收端对象。&lt;Thinking in Java&gt;  
**流的本质**：数据传输，根据数据传输特性将流抽象为各种类，方便更直观的进行数据操作。  
**流的作用**：为数据源和目的地建立一个输送通道。  
Java 中将输入输出抽象称为流，就好像水管，将两个容器连接起来。流是一组有顺序的，有起点和终点的字节集合，是对数据传输的总称或抽象。即数据在两设备间的传输称为流。

每个流只能是输入流或输出流的一种，不能同时具备两个功能，输入流只能进行读操作，对输出流只能进行写操作。在一个数据传输通道中，如果既要写入数据，又要读取数据，则要分别提供两个流。

#### 2、IO 模型

五种 IO 模型包括：阻塞 IO、非阻塞 IO、信号驱动 IO、IO 多路复用、异步 IO。其中，前四个被称为同步 IO。在网络环境下，可以将 IO 分为两步： 1.等待数据到来； 2.数据搬迁。
在互联网应用中，IO 线程大多被阻塞在等待数据的过程中，所以，如果要想提高 IO 效率，需要降低等待的时间。

##### 2.1 阻塞 IO（Blocking I/O）

在内核将数据准备好之前，系统调用会一直等待所有的套接字（Socket），默认的是阻塞方式。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E9%98%BB%E5%A1%9EIO%E6%A8%A1%E5%9E%8B-4893f3.png)

Java 中的 socket.read()会调用 native read()，而 Java 中的 native 方法会调用操作系统底层的 dll，而 dll 是 C/C++编写的，图中的 recvfrom 其实是 C 语言 socket 编程中的一个方法。所以其实我们在 Java 中调用 socket.read()最后也会调用到图中的 recvfrom 方法。

应用程序(也就是我们的代码)想要读取数据就会调用 recvfrom，而 recvfrom 会通知 OS 来执行，OS 就会判断数据报是否准备好(比如判断是否收到了一个完整的 UDP 报文，如果收到 UDP 报文不完整，那么就继续等待)。当数据包准备好了之后，OS 就会将数据从内核空间拷贝到用户空间(因为我们的用户程序只能获取用户空间的内存，无法直接获取内核空间的内存)。拷贝完成之后 socket.read()就会解除阻塞，并得到 read 的结果。

BIO 中的阻塞，就是阻塞在 2 个地方：

1. OS 等待数据报(通过网络发送过来)准备好。
2. 将数据从内核空间拷贝到用户空间。

在这 2 个时候，我们的 BIO 程序就是占着茅坑不拉屎，啥事情都不干。

##### 2.2 非阻塞 IO（Noblocking I/O）

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E9%9D%9E%E9%98%BB%E5%A1%9EIO%E6%A8%A1%E5%9E%8B-e39ebb.png)

每次应用进程询问内核是否有数据报准备好，当有数据报准备好时，就进行拷贝数据报的操作，从内核拷贝到用户空间，和拷贝完成返回的这段时间，应用进程是阻塞的。但在没有数据报准备好时，并不会阻塞程序，内核直接返回未准备就绪的信号，等待应用进程的下一个轮询。但是，轮询对于 CPU 来说是较大的浪费，一般只有在特定的场景下才使用。

Java 的 NIO 就是采用这种方式，当我们 new 了一个 socket 后我们可以设置它是非阻塞的。比如：

```java
// 初始化一个 serverSocketChannel
serverSocketChannel = ServerSocketChannel.open();
serverSocketChannel.bind(new InetSocketAddress(8000));
// 设置serverSocketChannel为非阻塞模式
// 即 accept()会立即得到返回
serverSocketChannel.configureBlocking(false);
```

上面的代码是设置 ServerSocketChannel 为非阻塞，SocketChannel 也可以设置。

从图中可以看到，当设置为非阻塞后，我们的 socket.read()方法就会立即得到一个返回结果(成功 or 失败)，我们可以根据返回结果执行不同的逻辑，比如在失败时，我们可以做一些其他的事情。但事实上这种方式也是低效的，因为我们不得不使用轮询方法去一直问 OS：“我的数据好了没啊”。

**NIO 不会在 recvfrom（询问数据是否准备好）时阻塞，但还是会在将数据从内核空间拷贝到用户空间时阻塞。一定要注意这个地方，Non-Blocking 还是会阻塞的。**

##### 2.3 IO 多路复用（I/O Multiplexing）

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/IO%E5%A4%8D%E7%94%A8%E6%A8%A1%E5%9E%8B-4de7d7.png)

传统情况下 client 与 server 通信需要 3 个 socket(客户端的 socket，服务端的 server socket，服务端中用来和客户端通信的 socket)，而在 IO 多路复用中，客户端与服务端通信需要的不是 socket，而是 3 个 channel，通过 channel 可以完成与 socket 同样的操作，channel 的底层还是使用的 socket 进行通信，但是多个 channel 只对应一个 socket(可能不只是一个，但是 socket 的数量一定少于 channel 数量)，这样仅仅通过少量的 socket 就可以完成更多的连接，提高了 client 容量。

其中，不同的操作系统，对此有不同的实现：

- Windows：selector
- Linux：epoll
- Mac：kqueue

其中 epoll，kqueue 比 selector 更为高效，这是因为他们监听方式的不同。selector 的监听是通过轮询 FD_SETSIZE 来问每一个 socket：“你改变了吗？”，假若监听到事件，那么 selector 就会调用相应的事件处理器进行处理。但是 epoll 与 kqueue 不同，他们把 socket 与事件绑定在一起，当监听到 socket 变化时，立即可以调用相应的处理。
**selector，epoll，kqueue 都属于 Reactor IO 设计。**

##### 2.4 信号驱动（Signal driven IO）

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E4%BF%A1%E5%8F%B7%E9%A9%B1%E5%8A%A8IO%E6%A8%A1%E5%9E%8B-1fc1b3.png)

信号驱动 IO 模型，应用进程告诉内核：当数据报准备好的时候，给我发送一个信号，对 SIGIO 信号进行捕捉，并且调用我的信号处理函数来获取数据报。

##### 2.5 异步 IO（Asynchronous I/O）

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E5%BC%82%E6%AD%A5IO%E6%A8%A1%E5%9E%8B-f91c47.png)

Asynchronous IO 调用中是真正的无阻塞，其他 IO model 中多少会有点阻塞。程序发起 read 操作之后，立刻就可以开始去做其它的事。而在内核角度，当它受到一个 asynchronous read 之后，首先它会立刻返回，所以不会对用户进程产生任何 block。然后，kernel 会等待数据准备完成，然后将数据拷贝到用户内存，当这一切都完成之后，kernel 会给用户进程发送一个 signal，告诉它 read 操作完成了。

可以看出，阻塞程度：阻塞 IO>非阻塞 IO>多路转接 IO>信号驱动 IO>异步 IO，效率是由低到高的。

##### 2.6 Blocking IO 与 Non-Blocking IO 区别？

阻塞或非阻塞只涉及程序和 OS，Blocking IO 会一直 block 程序直到 OS 返回，而 Non-Block IO 在 OS 内核准备数据包的情况下会立即得到返回。

##### 2.7 Asynchronous IO 与 Synchronous IO？

只要有 block 就是同步 IO，完全没有 block 则是异步 IO。所以我们之前所说的 Blocking IO、Non-Blocking IO、IO Multiplex，均为 Synchronous IO，只有 Asynchronous IO 为异步 IO。

##### 2.8 Non-Blocking IO 不是会立即返回没有阻塞吗?

**Non-Blocking IO 在数据包准备时是非阻塞的，但是在将数据从内核空间拷贝到用户空间还是会阻塞**。而 Asynchronous IO 则不一样，当进程发起 IO 操作之后，就直接返回再也不理睬了，由内核完成读写，完成读写操作后 kernel 发送一个信号，告诉进程说 IO 完成。在这整个过程中，进程完全没有被 block。

#### 3、IO 模式（Reactor 与 Proactor）

##### 3.1 Reactor

Reactor(反应器)的设计是一种事件驱动思想，比如 Java NIO 中，socket 过来时有四种事件：
connectable
acceptable
readable
writable
我们为每一种事件都编写一个处理器，然后设置每个 socket 要监听哪种情况，随后就可以调用对应的处理器。

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/20191121200143647-00520b.png)

图中的 input 就可以当作 socket，中间的 Service Hanlder&event dispatch 的作用就是监听每一个 socket(需要实现把 socket 注册进来，并指定要监听哪种情况)，然后给 socket 派发不同的事件。

##### 3.2 Proactor

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/2019112120035031-20ee59.png)

Proactor 与 Reactor 较为类似，以读取数据为例：
**Reactor 模式**

1.  应用程序注册读就绪事件和相关联的事件处理器
2.  事件分离器等待事件的发生
3.  当发生 读就绪事件 的时候，事件分离器调用第一步注册的事件处理器
4.  事件处理器首先执行实际的读取操作，然后根据读取到的内容进行进一步的处理

**Proactor 模式**

1.  应用程序调用一个异步读取操作，然后注册相应的事件处理器，此时事件处理器不关注读取就绪事件，而是关注读取完成事件，这是区别于 Reactor 的关键。
2.  事件分离器等待读取操作完成事件
3.  在事件分离器等待读取操作完成的时候，操作系统调用内核线程完成读取操作（异步 IO 都是操作系统负责将数据读写到应用传递进来的缓冲区供应用程序操作，操作系统扮演了重要角色），并将读取的内容放入用户传递过来的缓存区中。这也是区别于 Reactor 的一点，Proactor 中，应用程序需要传递缓存区。
4.  事件分离器捕获到读取完成事件后，激活应用程序注册的事件处理器，事件处理器直接从缓存区读取数据，而不需要进行实际的读取操作。

**区别**
Reactor 中，监听是否有可读或可写事件，然后读/写操作是由程序进行的。而 Proactor 中，直接监听读/写操作是否完成，也就是说读/写操作是否 OS 来完成，并将读写数据放入一个缓冲区提供给程序。

#### 4、同步与异步，阻塞与非阻塞

同步/异步（描述网络通信模式，适用于请求-响应模型）

同步：发送方发送请求后，需要等待接收响应，否则将一直等待
异步：发送方发送请求后，不需要等待响应，可以继续发送下一个请求，或者主动挂起线程并释放 CPU
阻塞/非阻塞（描述进程的函数调用方式）

阻塞：IO 调用会一直阻塞，直至结果返回才能继续执行
非阻塞：IO 调用会立即返回，不需要等待结果，并可以执行下一个 IO 调用
总结，同步异步和阻塞非阻塞是两个不同的概念，用简单的数据库查询来举一个例子：

如果发送一个请求，需要等待数据库响应，否则将一直等待，这就是同步
如果发送一个请求，不需要数据库响应，就可以继续发送下一个请求(NIO 模式、回调通知模式)，或者主动将任务插入队列中，主动挂起线程并释放 CPU(异步队列模式)，这就是异步

一般来说，同步是最简单的编程方式，而异步编程虽然需要一定技术，但是却能提升系统性能。对于阻塞与非阻塞，阻塞的实时响应性更好，但在高并发情况下阻塞线程数会急剧增加，导致大量的上下文切换会引起挂起/唤醒线程的性能损耗，而非阻塞的性能吞吐量更高，但由于其是顺序执行每一个事件，一旦处理某一个事件过久，会影响后续事件的处理，因此实时响应性较差。

## Java 中的 BIO

#### 传统 Socket 阻塞案例代码

```java
public class TraditionalSocketDemo {

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(7777);
		System.out.println("服务端启动...");
		while (true) {
			// 获取socket套接字
			// accept()阻塞点
			Socket socket = serverSocket.accept();
			System.out.println("有新客户端连接上来了...");
			// 获取客户端输入流
			java.io.InputStream is = socket.getInputStream();
			byte[] b = new byte[1024];
			while (true) {
				// 循环读取数据
				// read() 阻塞点
				int data = is.read(b);
				if (data != -1) {
					String info = new String(b, 0, data, "GBK");
					System.out.println(info);
				} else {
					break;
				}
			}
		}
	}
}
```

在 debugger 代码的过程中会发现，服务端启动，只有当客户端就绪后才进行下一步操作（如果客户端没有就绪，线程阻塞），客户端发送请求，程序才继续往下执行，如果客户端没有发出请求，线程阻塞；**上面的代码有两个阻塞点**：

1.  **等待客户端就绪**；
2.  **等待 OS 将数据从内核拷贝到用户空间（应用程序可以操作的内存空间）**；

#### 传统 bio 多线程版本

```java
public class TraditionalSocketDemo2 {

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(7777);
		System.out.println("服务端启动...");
		while (true) {
			// 获取socket套接字
			// accept()阻塞点
			final Socket socket = serverSocket.accept();
			System.out.println("有新客户端连接上来了...");
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// 获取客户端输入流
						InputStream is = socket.getInputStream();
						byte[] b = new byte[1024];
						while (true) {
							// 循环读取数据
							// read() 阻塞点
							int data = is.read(b);
							if (data != -1) {
								String info = new String(b, 0, data, "GBK");
								System.out.println(info);
							} else {
								break;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
}
```

能够解决传统的 BIO 问题，但是会出现：多少个客户端多少个线程，请求和线程的个数 1:1 关系；操作系统资源耗尽，服务端挂了。使用线程池虽然能控制服务线程的数量，但应对高并发量的访问时，依然会导致大量线程处于阻塞状态，严重影响服务效率。

## Java 中的 NIO

NIO 是一种基于通道和缓冲区的 I/O 方式，它可以使用 Native 函数库直接分配堆外内存（区别于 JVM 的运行时数据区），然后通过一个存储在 java 堆里面的 DirectByteBuffer 对象作为这块内存的直接引用进行操作。这样能在一些场景显著提高性能，因为避免了在 Java 堆和 Native 堆中来回复制数据。

#### 1、Java NIO 组件

NIO 主要有三大核心部分：Channel(通道)，Buffer(缓冲区), Selector（选择器）。传统 IO 是基于字节流和字符流进行操作（基于流），而 NIO 基于 Channel 和 Buffer(缓冲区)进行操作，数据总是从通道读取到缓冲区中，或者从缓冲区写入到通道中。Selector(选择区)用于监听多个通道的事件（比如：连接打开，数据到达）。因此，单个线程可以监听多个数据通道。

##### 1.1 Buffer

Buffer（缓冲区）是一个用于存储特定基本类型数据的容器。除了 boolean 外，其余每种基本类型都有一个对应的 buffer 类。Buffer 类的子类有 ByteBuffer, CharBuffer, DoubleBuffer, FloatBuffer, IntBuffer, LongBuffer, ShortBuffer 。

##### 1.2 Channel

Channel（通道）表示到实体，如硬件设备、文件、**网络套接字**或可以执行一个或多个不同 I/O 操作（如读取或写入）的程序组件的开放的连接。Channel 接口的常用实现类有 FileChannel（对应文件 IO）、DatagramChannel（对应 UDP）、SocketChannel 和 ServerSocketChannel（对应 TCP 的客户端和服务器端）。**Channel 和 IO 中的 Stream(流)是差不多一个等级的。只不过 Stream 是单向的，譬如：InputStream, OutputStream.而 Channel 是双向的，既可以用来进行读操作，又可以用来进行写操作**。

##### 1.3 Selector

Selector（选择器）用于监听多个通道的事件（比如：连接打开，数据到达）。因此，单个的线程可以监听多个数据通道。即用选择器，借助单一线程，就可对数量庞大的活动 I/O 通道实施监控和维护。

写就绪相对有一点特殊，一般来说，你不应该注册写事件。写操作的就绪条件为底层缓冲区有空闲空间，而写缓冲区绝大部分时间都是有空闲空间的，所以当你注册写事件后，写操作一直是就绪的，选择处理线程全占用整个 CPU 资源。所以，只有当你确实有数据要写时再注册写操作，并在写完以后马上取消注册。

基于阻塞式 I/O 的多线程模型中，Server 为每个 Client 连接创建一个处理线程，每个处理线程阻塞式等待可能达到的数据，一旦数据到达，则立即处理请求、返回处理结果并再次进入等待状态。由于每个 Client 连接有一个单独的处理线程为其服务，因此可保证良好的响应时间。但当系统负载增大（并发请求增多）时，Server 端需要的线程数会增加，对于操作系统来说，线程之间上下文切换的开销很大，而且每个线程都要占用系统的一些资源（如内存）。因此，使用的线程越少越好。

但是，现代的操作系统和 CPU 在多任务方面表现的越来越好，所以多线程的开销随着时间的推移，变得越来越小了。实际上，如果一个 CPU 有多个内核，不使用多任务可能是在浪费 CPU 能力。

传统的 IO 处理方式，一个线程处理一个网络连接

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/2019112120352588-b6e365.png)

NIO 处理方式，一个线程可以管理过个网络连接

![在这里插入图片描述](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/20191121203602279-fab503.png)

#### 2、NIO 服务器端如何实现非阻塞？

服务器上所有 Channel 需要向 Selector 注册，而 Selector 则负责监视这些 Socket 的 IO 状态(观察者)，当其中任意一个或者多个 Channel 具有可用的 IO 操作时，该 Selector 的 select()方法将会返回大于 0 的整数，该整数值就表示该 Selector 上有多少个 Channel 具有可用的 IO 操作，并提供了 selectedKeys（）方法来返回这些 Channel 对应的 SelectionKey 集合(一个 SelectionKey 对应一个就绪的通道)。正是通过 Selector，使得服务器端只需要不断地调用 Selector 实例的 select()方法即可知道当前所有 Channel 是否有需要处理的 IO 操作。注：java NIO 就是多路复用 IO，jdk7 之后底层是 epoll 模型。

#### 3、Java NIO 的简单实现

##### 3.1 服务端

```java
public class NioServer {

    private int port;
    private Selector selector;
    private ExecutorService service = Executors.newFixedThreadPool(5);

    public static void main(String[] args){
        new NioServer(8080).start();
    }

    public NioServer(int port) {
        this.port = port;
    }

    public void init() {
        ServerSocketChannel ssc = null;
        try {
            ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.bind(new InetSocketAddress(port));
            selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("NioServer started ......");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
        }
    }

    public void accept(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            sc.register(selector, SelectionKey.OP_READ);
            System.out.println("accept a client : " + sc.socket().getInetAddress().getHostName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        this.init();
        while (true) {
            try {
                int events = selector.select();
                if (events > 0) {
                    Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
                    while (selectionKeys.hasNext()) {
                        SelectionKey key = selectionKeys.next();
                        selectionKeys.remove();
                        if (key.isAcceptable()) {
                            accept(key);
                        } else {
                            service.submit(new NioServerHandler(key));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class NioServerHandler implements Runnable{

        private SelectionKey selectionKey;

        public NioServerHandler(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
        }

        @Override
        public void run() {
            try {
                if (selectionKey.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    socketChannel.read(buffer);
                    buffer.flip();
                    System.out.println("收到客户端"+socketChannel.socket().getInetAddress().getHostName()+"的数据："+new String(buffer.array()));
                    //将数据添加到key中
                    ByteBuffer outBuffer = ByteBuffer.wrap(buffer.array());
                    socketChannel.write(outBuffer);// 将消息回送给客户端
                    selectionKey.cancel();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

##### 3.2 客户端

```java
public class NioClient {
    private static final String host = "127.0.0.1";
    private static final int port = 8080;
    private Selector selector;

    public static void main(String[] args){
        for (int i=0;i<3;i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NioClient client = new NioClient();
                    client.connect(host, port);
                    client.listen();
                }
            }).start();
        }
    }

    public void connect(String host, int port) {
        try {
            SocketChannel sc = SocketChannel.open();
            sc.configureBlocking(false);
            this.selector = Selector.open();
            sc.register(selector, SelectionKey.OP_CONNECT);
            sc.connect(new InetSocketAddress(host, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        while (true) {
            try {
                int events = selector.select();
                if (events > 0) {
                    Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
                    while (selectionKeys.hasNext()) {
                        SelectionKey selectionKey = selectionKeys.next();
                        selectionKeys.remove();
                        //连接事件
                        if (selectionKey.isConnectable()) {
                            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                            if (socketChannel.isConnectionPending()) {
                                socketChannel.finishConnect();
                            }

                            socketChannel.configureBlocking(false);
                            socketChannel.register(selector, SelectionKey.OP_READ);
                            socketChannel.write(ByteBuffer.wrap(("Hello this is " + Thread.currentThread().getName()).getBytes()));
                        } else if (selectionKey.isReadable()) {
                            SocketChannel sc = (SocketChannel) selectionKey.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            sc.read(buffer);
                            buffer.flip();
                            System.out.println("收到服务端的数据："+new String(buffer.array()));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

```

## Java 中的 AIO

JDK1.7 升级了 NIO 类库，升级后的 NIO 类库被称为 NIO 2.0。Java 正式提供了异步文件 I/O 操作，同时提供了与 UNIX 网络编程事件驱动 I/O 对应的 AIO。NIO 2.0 引入了新的异步通道的概念，并提供了异步文件通道和异步套接字通道的实现。

异步通道获取获取操作结果方式：

1. 使用 java.util.concurrent.Future 类表示异步操作的结果；
2. 在执行异步操作的时候传入一个 java.nio.channels，操作完成后会回调 CompletionHandler 接口的实现类。

NIO 2.0 的异步套接字通道是真正的异步非阻塞 I/O，对应于 UNIX 网络编程中的事件驱动 I/O（AIO）。

> **四种IO编程及对比** 

## 传统的 BIO 编程

网络编程的基本模型是 Client/Server 模型，也就是两个进程之间进行相互通信，其中服务端提供位置信息(绑定的 IP 地址和监听端口)，客户端通过连接操作向服务端监听的地址发起连接请求，通过三次握手建立连接，如果连接建立成功，双方就可以通过网络套接字(Socket) 进行通信。

在基于传统同步阻塞模型开发中，ServerSocket 负责绑定 IP 地址，启动监听端口，Socket 负责发起连接操作。连接成功之后，双方通过输入和输出流进行同步阻塞式通信。

### BIO 通信模型

通过下面的通信模型图可以发现，采用 BIO 通信模型的服务端，通常由一个独立的 Acceptor 线程 负责监听客户端的连接，它接收到客户
端连接请求之后为每个客户端创建一个新的线程进行链路处理，处理完成之后，通过输出流返回应答给客户端，线程销毁。这就是典型的 “一请求一应答” 通信模型。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/BIO%E9%80%9A%E4%BF%A1%E6%A8%A1%E5%9E%8B-0ad537.png)

该模型最大的问题就是缺乏弹性伸缩能力，当客户端并发访问量增加后，服务端的线程个数和客户端并发访问数呈 1: 1 的正比关系，由于线程是 Java 虚拟机 非常宝贵的系统资源，当线程数膨胀之后，系统的性能将急剧下降，随着并发访问量的继续增大，系统会发生线程堆栈溢出、创建新线程失败等问题，并最终导致进程宕机或者僵死，不能对外提供服务。

在高性能服务器应用领域，往往需要面向成千上万个客户端的并发连接，这种模型显然无法满足高性能、高并发接入的场景。为了改进 一线程一连接 模型，后来又演进出了一种通过线程池或者消息队列实现 1 个或者多个线程处理 N 个客户端的模型，由于它的底层通信机制依然使用 同步阻塞 IO，所以被称为 “伪异步”。

## 伪异步 IO 编程

为了解决 同步阻塞 IO 面临的一个链路需要一个线程处理的问题，后来有人对它的线程模型进行了优化，后端通过一个线程池来处理多个客户端的请求接入，形成 客户端个数 M：线程池最大线程数 N 的比例关系，其中 M 可以远远大于 N。通过线程池可以灵活地调配线程资源，设置线程的最大值，防止由于海量并发接入导致线程耗尽。

### 伪异步 IO 模型图

采用线程池和任务队列可以实现一种叫做 伪异步的 IO 通信框架，其模型图下。当有新的客户端接入时，将客户端的 Socket 封装成一个 Task 对象 (该类实现了 java.lang.Runnable 接口)，投递到后端的线程池中进行处理，JDK 的线程池维护一个消息队列和 N 个活跃线程，对消息队列中的任务进行处理。由于线程池可以设置消息队列的大小和最大线程数，因此，它的资源占用是可控的，无论多少个客户端并发访问，都不会导致资源的耗尽和宕机。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E4%BC%AA%E5%BC%82%E6%AD%A5IO%E9%80%9A%E4%BF%A1%E6%A8%A1%E5%9E%8B-fd237a.png)

伪异步 IO 通信框架 采用了线程池实现，因此避免了为每个请求都创建一个独立线程造成的线程资源耗尽问题。但是由于它底层的通信依然采用同步阻塞模型，因此无法从根本上解决问题。

### 伪异步 IO 编程弊端分析

要对 伪异步 IO 编程 的弊端进行深入分析，首先我们看两个 Java 同步 IO 的 API 说明，随后结合代码进行详细分析。

```java
public abstract class InputStream implements Closeable {

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream
     * has been reached, the value <code>-1</code> is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * <p> A subclass must provide an implementation of this method.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @exception  IOException  if an I/O error occurs.
     */
    public abstract int read() throws IOException;
}
```

注意其中的一句话 **“This method blocks until input data is available, the end of the stream is detected, or an exception is thrown”**，当对 Socket 的输入流进行读取操作的时候，它会一直阻塞下去，直到发生如下三种事件。

- 有数据可读；
- 可用数据已经读取完毕；
- 发生空指针或者 IO 异常。

这意味着当对方发送请求或者应答消息比较缓慢，或者网络传输较慢时，读取输入流一方的通信线程将被长时间阻塞，如果对方要 60s 才能够将数据发送完成，读取一方的 IO 线程 也将会被同步阻塞 60s，在此期间，其他接入消息只能在消息队列中排队。

下面我们接着对输出流进行分析，还是看 JDK IO 类库 输出流的 API 文档，然后结合文档说明进行故障分析。

```java
public abstract class OutputStream implements Closeable, Flushable {

    /**
     * Writes an array of bytes. This method will block until the bytes are *actually written.
     *
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }
}
```

当调用 OutputStream 的 write()方法 写输出流的时候，它将会被阻塞，直到所有要发送的字节全部写入完毕，或者发生异常。学习过 TCP/IP 相关知识的人都知道，当消息的接收方处理缓慢的时候，将不能及时地从 TCP 缓冲区 读取数据，这将会导致发送方的 TCP window size 不断减小，直到为 0，双方处于 Keep-Alive 状态，消息发送方将不能再向 TCP 缓冲区 写入消息，这时如果采用的是 同步阻塞 IO，write 操作 将会被无限期阻塞，直到 TCP window size 大于 0 或者发生 IO 异常。

通过对输入和输出流的 API 文档 进行分析，我们了解到读和写操作都是同步阻塞的，阻塞的时间取决于对方 IO 线程 的处理速度和 网络 IO 的传输速度。本质上来讲，我们无法保证生产环境的网络状况和对方的应用程序能足够快，如果我们的应用程序依赖对方的处理速度，它的可靠性就非常差。也许在实验室进行的性能测试结果令人满意，但是一旦上线运行，面对恶劣的网络环境和良莠不齐的第三方系统，问题就会如火山一样喷发。

伪异步 IO 实际上仅仅是对之前 IO 线程模型 的一个简单优化，它无法从根本上解决 同步 IO 导致的通信线程阻塞问题。下面我们就简单分析下通信对方返回应答时间过长会引起的级联故障。

1. 服务端处理缓慢，返回应答消息耗费 60s， 平时只需要 10ms。
2. 采用伪异步 I/O 的线程正在读取故障服务节点的响应，由于读取输入流是阻塞的，它将会被同步阻塞 60s。
3. 假如所有的可用线程都被故障服务器阻塞，那后续所有的 1/O 消息都将在队列中排队。
4. 由于线程池采用阻塞队列实现，当队列积满之后，后续入队列的操作将被阻塞。
5. 由于前端只有一个 Accptor 线程接收客户端接入，它被阻塞在线程池的同步阻塞队列之后，新的客户端请求消息将被拒绝，客户端会发生大量的连接超时。
6. 由于几乎所有的连接都超时，调用者会认为系统已经崩溃，无法接收新的请求消息。

## NIO 编程

与 Socket 类 和 ServerSocket 类 相对应，NIO 也提供了 SocketChannel 和 ServerSocketChannel 两种不同的套接字通道实现。这两种新增的通道都支持阻塞和非阻塞两种模式。阻塞模式使用非常简单，但是性能和可靠性都不好，非阻塞模式则正好相反。开发人员可以根据自
己的需要来选择合适的模式。一般来说，低负载、低并发的应用程序可以选择 同步阻塞 IO，以降低编程复杂度；对于高负载、高并发的网络应用，需要使用 NIO 的非阻塞模式进行开发。

### NIO 类库简介

NIO 类库 是在 JDK 1.4 中引入的。NIO 弥补了原来 同步阻塞 IO 的不足，它在 标准 Java 代码 中提供了高速的、面向块的 IO。下面我们简单看一下 NIO 类库 及其 相关概念。

**1、缓冲区 Buffer**  
Buffer 对象 包含了一些要写入或者要读出的数据。在 NIO 类库 中加入 Buffer 对象，是其与 原 IO 类库 的一个重要区别。在面向流的 IO 中，可以将数据直接写入或者将数据直接读到 Stream 对象 中。在 NIO 库中，所有数据都是用缓冲区处理的。在读取数据时，它是直接读到缓冲区中的；在写入数据时，写入到缓冲区中。任何时候访问 NIO 中的数据，都是通过缓冲区进行操作。

缓冲区实质上是一个数组。通常它是一个字节数组（ByteBuffer），也可以使用其他种类的数组。但是一个缓冲区不仅仅是一个数组，缓冲区提供了对数据的结构化访问以及维护读写位置（limit）等信息。最常用的缓冲区是 ByteBuffer，一个 ByteBuffer 提供了一组功能用于操作 byte 数组。除了 ByteBuffer，还有其他的一些缓冲区，事实上，每一种 Java 基本类型（除了 boolean）都对应有一种与之对应的缓冲区，如：CharBuffer、IntBuffer、DoubleBuffer 等等。Buffer 组件中主要类的类图如下所示。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/Buffer%E7%BB%84%E4%BB%B6%E7%B1%BB%E5%9B%BE-817be5.png)

除了 ByteBuffer，每一个 Buffer 类 都有完全一样的操作，只是它们所处理的数据类型不一样。因为大多数 标准 IO 操作 都使用 ByteBuffer，所以它在具有一般缓冲区的操作之外还提供了一些特有的操作，以方便网络读写。

**2、通道 Channel**  
Channel 是一个通道，它就像自来水管一样，网络数据通过 Channel 读取和写入。通道与流的不同之处在于通道是双向的，可以用于读、写，或者二者同时进行；流是单向的，要么是 InputStream，要么是 OutputStream。因为 Channel 是全双工的，所以它可以比流更好地映射底层操作系统的 API。特别是在 UNIX 网络编程模型 中，底层操作系统的通道都是全双工的，同时支持读写操作。Channel 组件中 主要类的类图如下所示，从中我们可以看到最常用的 ServerSocketChannel 和 SocketChannel。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/Channel%E7%BB%84%E4%BB%B6%E7%B1%BB%E5%9B%BE-0781df.png)

**3、多路复用器 Selector**  
多路复用器 Selector 是 Java NIO 编程 的基础，熟练地掌握 Selector 对于 NIO 编程 至关重要。多路复用器提供选择已经就绪的任务的能力。简单来讲，Selector 会不断地轮询 “注册在其上的 Channel”，如果某个 Channel 上面发生读或者写事件，这个 Channel 就处于就绪状态，会被 Selector 轮询出来，然后通过 SelectionKey 可以获取 “就绪 Channel 的集合”，进行后续的 IO 操作。

一个 多路复用器 Selector 可以同时轮询多个 Channel，由于 JDK 使用了 epoll() 代替传统的 select 的实现，所以它并没有最大连接句柄的限制。这也就意味着，只需要一个线程负责 Selector 的轮询，就可以接入成千上万的客户端。下面，我们通过 NIO 编程的序列图 和 源码分析来熟悉相关的概念。

### NIO 服务端序列图

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/NIO%E6%9C%8D%E5%8A%A1%E7%AB%AF%E5%BA%8F%E5%88%97%E5%9B%BE-543262.png)

下面，我们看一下 NIO 服务端 的主要创建过程。

1、打开 ServerSocketChannel，用于监听客户端的连接，它是所有客户端连接的
父管道，示例代码如下。

```java
    ServerSocketChannel acceptorSvr = ServerSocketChannel.open();
```

2、绑定监听端口，设置连接为非阻塞模式，示例代码如下。

```java
    acceptorSvr.socket().bind(new InetSocketAddress(InetAddress.getByName("IP"), port));
    acceptorSvr.configureBlocking(false);
```

3、创建 Reactor 线程，创建多路复用器并启动线程，示例代码如下。

```java
    Selector selector = Selector.open();
    New Thread (new ReactorTask()).start();
```

4、将 ServerSocketChannel 注册到 Reactor 线程 的 多路复用器 Selector 上，监听 ACCEPT 事件，示例代码如下。

```java
    SelectionKey key = acceptorSvr.register(selector, SelectionKey.OP_ ACCEPT, ioHandler);
```

5、多路复用器在线程 run()方法 的无限循环体内轮询 准备就绪的 Key，示例代码如下。

```java
    int num = selector.select();
    Set selectedKeys = selector.selectedKeys();
    Iterator it = selectedKeys.iterator();
    while (it.hasNext()) {
        SelectionKey key = (SelectionKey) it.next();
        // .... deal with IO event ...
```

6、多路复用器 Selector 监听到有新的客户端接入，处理新的接入请求，完成 TCP 三次握手，建立物理链路，示例代码如下。

```java
    SocketChannel channel = svrChannel.accept();
```

7、设置客户端链路为非阻塞模式，示例代码如下。

```java
    channel.configureBlocking(false);
    channel.socket().setReuseAddress(true);
    ......
```

8、将新接入的客户端连接注册到 Reactor 线程 的多路复用器上，监听读操作，读取客户端发送的网络消息，示例代码如下。

```java
    SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ, ioHandler);
```

9、异步读取客户端请求消息到缓冲区，示例代码如下。

```java
    int readNumber = channel.read(receivedBuffer);
```

10、对 ByteBuffer 进行编解码，如果有半包消息指针 reset，继续读取后续的报文，将解码成功的消息封装成 Task，投递到业务线程池中,进行业务逻辑编排，示例代码如下。

```java
    List messageList = null;
    while (byteBuffer.hasRemain()) {
        byteBuffer.mark();
        Object message = decode(byteBuffer) ;
        if (message == null) {
            byteBuffer.reset();
            break;
        }
        messageList.add(message);
    }
    if (!byteBuffer.hasRemain()) {
        byteBuffer.clear();
    } else {
        byteBuffer.compact();
    }
    if (messageList != null && !messageList.isEmpty()) {
        for (Object message : messageList) {
            handlerTask(message);
        }
    }
```

11、将 POJO 对象 encode 成 ByteBuffer，调用 SocketChannel 的 异步 write 接口，将消息异步发送给客户端，示例代码如下。

```java
    socketChannel.write(byteBuffer);
```

注意：如果发送区 TCP 缓冲区满，会导致写半包，此时，需要注册监听写操作位，循环写，直到整包消息写入 TCP 缓冲区。对于 “半包问题” 此处暂不赘述，后续会单独写一篇详细分析 Netty 的处理策略。

### NIO 客户端序列图

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/NIO%E5%AE%A2%E6%88%B7%E7%AB%AF%E5%BA%8F%E5%88%97%E5%9B%BE-1102ca.png)

1、打开 SocketChannel，绑定客户端本地地址 (可选，默认系统会随机分配一个可用的本地地址)，示例代码如下。

```java
    SocketChannel clientChannel = SocketChannel.open();
```

2、设置 SocketChannel 为非阻塞模式，同时设置客户端连接的 TCP 参数，示例代码如下。

```java
    clientChannel.configureBlocking(false);
    socket.setReuseAddress(true);
    socket.setReceiveBufferSize(BUFFER_SIZE);
    socket.setSendBufferSize(BUFFER_SIZE);
```

3、异步连接服务端，示例代码如下。

```java
    boolean connected = clientChannel.connect( new InetSocketAddress("ip", port) );
```

4、判断是否连接成功，如果连接成功，则直接注册读状态位到多路复用器中，如果当前没有连接成功，则重连 (异步连接，返回 false，说明客户端已经发送 syne 包，服务端没有返回 ack 包，物理链路还没有建立)，示例代码如下。

```java
    if (connected) {
        clientChannel.register(selector, SelectionKey.OP_READ, ioHandler);
    } else {
        clientChannel.register(selector, SelectionKey.OP_CONNECT, ioHandler);
    }
```

5、向 Reactor 线程 的多路复用器注册 OP_CONNECT 状态位，监听服务端的 TCP ACK 应答，示例代码如下。

```java
    clientChannel.register(selector, SelectionKey.OP_CONNECT, ioHandler);
```

6、创建 Reactor 线程，创建多路复用器并启动线程，代码如下。

```java
    Selector selector = Selector.open();
    New Thread( new ReactorTask() ).start();
```

7、多路复用器在线程 run()方法 的无限循环体内轮询 准备就绪的 Key，代码如下。

```java
    int num = selector.select();
    Set selectedKeys = selector.selectedKeys();
    Iterator it = selectedKeys.iterator();
    while (it.hasNext()) {
        SelectionKey key = (SelectionKey) it.next();
        // ... deal with IO event ...
    }
```

8、接收 connect 事件，并进行处理，示例代码如下。

```java
    if (key.isConnectable()) {
        // handlerConnect();
    }
```

9、判断连接结果，如果连接成功，注册读事件到多路复用器，示例代码如下。

```java
    if(channel.finishConnect()) {
        registerRead();
    }
```

10、注册读事件到多路复用器，示例代码如下。

```java
    clientChannel.register(selector, SelectionKey.OP_READ, ioHandler);
```

11、异步读客户端请求消息到缓冲区，示例代码如下。

```java
    int readNumber = channel.read(receivedBuffer);
```

12、对 ByteBuffer 进行编解码，如果有半包消息接收缓冲区 Reset，继续读取后续的报文，将解码成功的消息封装成 Task，投递到业务线程池中，进行业务逻辑编排。示例代码如下。

```java
    List messageList = null;
    while (byteBuffer.hasRemain()) {
        byteBuffer.mark();
        object message = decode(byteBuffer);
        if (message == nu11) {
            byteBuffer.reset();
            break;
        }
        messageList.add(message);
    }
    if (!byteBuffer.hasRemain()) {
        byteBuffer.clear();
    } else {
        byteBuffer.compact();
    }
    if ( messageList != null && !messageList.isEmpty() )
        for (Object message : messageList) {
            handlerTask(message);
        }
    }
```

13、将 POJO 对象 encode 成 ByteBuffer，调用 SocketChannel 的 异步 write 接口，将消息异步发送给客户端。示例代码如下。

```java
    socketChannel.write(buffer);
```

## AIO 编程

NIO2.0 引入了新的异步通道的概念，并提供了异步文件通道和异步套接字通道的实现。异步通道提供以下两种方式获取获取操作结果。

- 通过 java.util.concurrent.Future 类 来表示异步操作的结果;
- 在执行异步操作的时候传入一个 java.nio.channels.CompletionHandler 接口 的实现类作为操作完成的回调。

NIO2.0 的异步套接字通道是真正的 异步非阻塞 IO，对应于 UNIX 网络编程 中的 事件驱动 IO (AIO)。它不需要通过多路复用器 (Selector) 对注册的通道进行轮询操作即可实现异步读写，从而简化了 NIO 的编程模型。

由于在实际开发中使用较少，所以这里不对 AIO 进行详细分析。

## 四种 IO 编程模型的对比

对比之前，这里再澄清一下 “伪异步 IO” 的概念。伪异步 IO 的概念完全来源于实践，并没有官方说法。在 JDK NIO 编程 没有流行之前，为了解决 Tomcat 通信线程同步 IO 导致业务线程被挂住的问题，大家想到了一个办法，在通信线程和业务线程之间做个缓冲区，这个缓冲区用于隔离 IO 线程 和业务线程间的直接访问，这样业务线程就不会被 IO 线程 阻塞。而对于后端的业务侧来说，将消息或者 Task 放到线程池后就返回了，它不再直接访问 IO 线程 或者进行 IO 读写，这样也就不会被同步阻塞。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/%E5%9B%9B%E7%A7%8DIO%E6%A8%A1%E5%9E%8B%E7%9A%84%E5%8A%9F%E8%83%BD%E7%89%B9%E6%80%A7%E5%AF%B9%E6%AF%94%E5%9B%BE-0d8668.png)

## 选择 Netty 开发项目的理由

从可维护性角度看，由于 NIO 采用了异步非阻塞编程模型，而且是一个 IO 线程 处理多条链路，它的调试和跟踪非常麻烦，特别是生产环境中的问题，我们无法进行有效的调试和跟踪，往往只能靠一些日志来辅助分析，定位难度很大。

### 为什么不选择 Java 原生 NIO 进行开发

1. NIO 的类库和 API 使用起来非常繁杂，需要熟练掌握 Selector、ServerSocketChannel、SocketChannel、ByteBuffer 等。
2. 需要具备其他的额外技能做铺垫，例如，熟悉 Java 多线程编程。这是因为 NIO 编程 涉及到 Reactor 模式，你必须对 多线程 和 网路编程 非常熟悉，才能编写出高质量的 NIO 程序。
3. 可靠性能力补齐，工作量和难度都非常大。例如客户端面临：断连重连、网络闪断、半包读写、失败缓存、网络拥塞和异常码流的处理，等问题。
4. JDK NIO 的 BUG，例如臭名昭著的 epoll bug，它会导致 Selector 空轮询，最终导致 CPU 100%。虽然官方声称修复了该问题，但是直到 JDK 1.7 版本 该问题仍旧未得到彻底的解决。

### 为什么选择 Netty 进行开发

Netty 是业界最流行的 NIO 框架 之一，它的健壮性、功能、性能、可定制性和可扩展性在同类框架中都是首屈一指的，已经得到成百上千的商用项目验证，例如 Hadoop 的 RPC 框架 Avro ，阿里的 RPC 框架 Dubbo 就使用了 Netty 作为底层通信框架。通过对 Netty 的分析，我们将它的优点总结如下。

- API 使用简单，开发门槛低；
- 功能强大，预置了多种编解码功能，支持多种主流协议；
- 定制能力强，可以通过 ChannelHandler 对通信框架进行灵活地扩展；
- 性能高，通过与其他业界主流的 NIO 框架 对比，Netty 的综合性能最优；
- 成熟、稳定，Netty 修复了已经发现的所有 JDK NIO BUG，业务开发人员不需要再为 NIO 的 BUG 而烦恼；
- 社区活跃，版本迭代周期短，发现的 BUG 可以被及时修复，同时，更多的新功能会加入；
- 经历了大规模的商业应用考验，质量得到验证。Netty 在互联网、大数据、网络游戏、企业应用、电信软件等众多行业已经得到了成功商用，证明它已经完全能够满足不同行业的商业应用了。

正是因为这些优点，Netty 逐渐成为了 Java NIO 编程 的首选框架。









# TCP 粘包/拆包

熟悉 TCP 编程的都知道，无论是服务端还是客户端，当我们读取或者发送消息的时候，都需要考虑 TCP 底层 的 粘包/拆包机制。TCP 粘包/拆包问题，在功能测试时往往不会怎么出现，而一旦并发压力上来，或者发送大报文之后，就很容易出现 粘包 / 拆包问题。如果代码没有考虑，往往就会出现解码错位或者错误，导致程序不能正常工作。本篇博文，我们先简单了解 TCP 粘包/拆包 的基础知识，然后来看看 Netty 是如何解决这个问题的。

### TCP 粘包/拆包问题说明

TCP 是个 “流” 协议，所谓流，就是没有界限的一串数据。TCP 底层 并不了解上层（如 HTTP 协议）业务数据的具体含义，它会根据 TCP 缓冲区 的实际情况进行包的划分，所以在业务上认为，一个完整的包可能会被 TCP 拆分成多个包进行发送，也有可能把多个小的包封装成一个大的数据包发送，这就是所谓的 TCP 粘包和拆包问题。我们可以通过下面的示例图，对 TCP 粘包和拆包问题 进行说明。

![avatar](https://linkeq.oss-cn-chengdu.aliyuncs.com/img/2022/01/26/TCP%E7%B2%98%E5%8C%85%E6%8B%86%E5%8C%85%E9%97%AE%E9%A2%98-d5b11d.png)

假设客户端依次发送了两个数据包 D1 和 D2 给服务端，由于服务端一次读取到的字节数是不确定的，故可能存在以下 4 种情况。

1. 服务端分两次读取到了两个独立的数据包，分别是 D1 和 D2，没有粘包和拆包；
2. 服务端一次接收到了两个数据包，D1 和 D2 粘合在一起，被称为 TCP 粘包；
3. 服务端分两次读取到了两个数据包，第一次读取到了完整的 D1 包 和 D2 包的部分内容，第二次读取到了 D2 包 的剩余内容，这被称为 TCP 拆包；
4. 服务端分两次读取到了两个数据包，第一次读取到了 D1 包的部分内容，第二次读取到了 D1 包的剩余内容 和 D2 包的整包。

如果此时服务端 TCP 接收滑窗非常小，而 数据包 D1 和 D2 比较大，很有可能会发生第 5 种可能，即服务端分多次才能将 D1 和 D2 包 接收完全，期间发生多次拆包。

### TCP 粘包/拆包发生的原因

问题产生的原因有三个，分别如下。

1. **应用程序 write 写入的字节大小 超出了 套接口发送缓冲区大小；**
2. 进行 MSS 大小的 TCP 分段；
3. 以太网帧的 payload 大于 MTU 进行 IP 分片。

### 粘拆包问题的解决策略

由于底层的 TCP 无法理解上层的业务数据，所以在底层是无法保证数据包不被拆分和重组的，这个问题只能通过上层的应用协议栈设计来解决，根据业界的主流协议的解决方案，可以归纳如下。

1. 固定消息长度，例如，每个报文的大小为 固定长度 200 字节，如果不够，空位补空格；
2. 在包尾使用 “回车换行符” 等特殊字符，作为消息结束的标志，例如 FTP 协议，这种方式在文本协议中应用比较广泛；
3. 将消息分为消息头和消息体，在消息头中定义一个 长度字段 Len 来标识消息的总长度;
4. 更复杂的应用层协议。

**注意**：从 TCP 流式设计上来看，TCP 粘包其实是一个伪命题。应用层协议需要自己划分消息的边界。**TCP 粘包问题是因为应用层协议开发者的错误设计导致的，他们忽略了 TCP 协议数据传输的核心机制 — 基于字节流，其本身并不存在数据包的概念。** 所有在 TCP 中传输的数据都是以流的形式进行传输，这就需要应用层协议开发者自行设计消息的边界划分规则。所以粘包总的来说还是以下两点：

- TCP 协议是面向字节流的协议，它可能会重新分割组合应用层协议的消息到多个数据段中；
- 应用层协议没有定义消息的边界，导致数据的接收方无法按边界拆分粘连的消息。

介绍完了 TCP 粘包/拆包 的基础，下面我们来看看 Netty 是如何使用一系列 “半包解码器” 来解决 TCP 粘包/拆包问题的。

## 利用 Netty 的解码器 解决 TCP 粘拆包问题

根据上面的 粘拆包问题解决策略，Netty 提供了相应的解码器实现。有了这些解码器，用户不需要自己对读取的报文进行人工解码，也不需要考虑 TCP 的粘包和拆包。

### LineBasedFrameDecoder 和 StringDecoder 的原理分析

为了解决 TCP 粘包 / 拆包 导致的 半包读写问题，Netty 默认提供了多种编解码器用于处理半包，只要能熟练掌握这些类库的使用，TCP 粘拆包问题 从此会变得非常容易，你甚至不需要关心它们，这也是其他 NIO 框架 和 JDK 原生的 NIO API 所无法匹敌的。对于使用者来说，只要将支持半包解码的 Handler 添加到 ChannelPipeline 对象 中即可，不需要写额外的代码，使用起来非常简单。

```java
    // 示例代码，其中 socketChannel 是一个 SocketChannel对象
    socketChannel.pipeline().addLast( new LineBasedFrameDecoder(1024) );
    socketChannel.pipeline().addLast( new StringDecoder() );
```

LineBasedFrameDecoder 的工作原理是它依次遍历 ByteBuf 中的可读字节，判断看是否有 “\n” 或者 “\r\n”，如果有，就以此位置为结束位置，从可读索引到结束位置区间的字节就组成了一行。它是以换行符为结束标志的解码器，支持携带结束符或者不携带结束符两种解码方式，同时支持配置单行的最大长度。如果连续读取到最大长度后仍然没有发现换行符，就会抛出异常，同时忽略掉之前读到的异常码流。

StringDecoder 的功能非常简单，就是将接收到的对象转换成字符串，然后继续调用后面的 Handler。LineBasedFrameDecoder + StringDecoder 组合 就是按行切换的文本解码器，它被设计用来支持 TCP 的粘包和拆包。

### 其它解码器

除了 LineBasedFrameDecoder 以外，还有两个常用的解码器 DelimiterBasedFrameDecoder 和 FixedLengthFrameDecoder，前者能自动对 “以分隔符做结束标志的消息” 进行解码，后者可以自动完成对定长消息的解码。使用方法也和前面的示例代码相同，结合 字符串解码器 StringDecoder，轻松完成对很多消息的自动解码。
