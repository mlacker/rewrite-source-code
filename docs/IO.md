# IO

## 概念

阻塞：应用程序调用 read/write 操作时，应用陷入阻塞状态，等操作执行完毕后恢复。

非阻塞：应用调用 read/write 操作时，立刻返回执行状态。

同步：内核通知应用 IO 就绪状态。

异步：内核通知应用 IO 完成状态。

## IO 模型

### BIO

Blocking I/O

同步阻塞

### NIO

同步非阻塞

### I/O 多路复用

### 信号驱动

同步非阻塞

### AIO

异步非阻塞

## IO 线程模型

### Reactor

响应式编程

The reactor design pattern is an event handling pattern for handling service requests delivered concurrently to a service handler by one or more inputs. The service handler then demultiplexes the incoming requests and dispatches them synchronously to the associated request handlers.

Reactor 设计模式是当一或多个输入源并发请求服务的事件处理模式，服务处理器将收到的请求分发到关联的处理器中。

Proactor（命令式编程）
