# Transaction

## 特性

Atomic
Consistency
Iso
Duration

## 隔离级别

Read uncommited
Read commited
Repaired Read
Serial

## Propagation

REQUIRED: Support a current transaction, create a new one if none exists.
SUPPORTS: Support a current transaction, execute non-transactionally if none exists.
MANDATORY: Support a current transaction, throw an exception if none exists.
REQUIRES_NEW: Create a new transaction, and suspend the current transaction if one exists.
NOT_SUPPORTED: Execute non-transactionally, suspend the current transaction if one exists.
NEVER: Execute non-transactionally, throw an exception if a transaction exists.
NESTED: Execute within a nested transaction if a current transaction exists, behave like REQUIRED otherwise.

## 分布式事务

### 方式

#### 两阶段提交（2PC）

两阶段提交（Two-phase Commit，2PC），通过引入协调者（Coordinator）来协调参与者的行为，并最终决定这些参与者是否要真正执行事务。

1. 协调者询问参与者事务是否执行成功，参与者发回事务执行结果。
![准备阶段](https://oscimg.oschina.net/oscnet/62cf113cd8c40eaafecb9fc6111beef4480.jpg)
2. 如果事务在每个参与者上都执行成功，事务协调者发送通知让参与者提交事务；否则，协调者发送通知让参与者回滚事务。
![提交阶段](https://oscimg.oschina.net/oscnet/6610fc7d97c9277423a9c620487e09db3f8.jpg)

当准备阶段，因某个节点信息丢失没有响应或节点故障，则发送回滚命令。
当提交阶段执行失败，则重试。

##### 问题

1. 同步阻塞 所有事务参与者在等待其它参与者响应的时候都处于同步阻塞状态，无法进行其它操作。
2. 单点问题 协调者在 2PC 中起到非常大的作用，发生故障将会造成很大影响。特别是在阶段二发生故障，所有参与者会一直等待状态，无法完成其它操作。
3. 数据不一致 在阶段二，如果协调者只发送了部分 Commit 消息，此时网络发生异常，那么只有部分参与者接收到 Commit 消息，也就是说只有部分参与者提交了事务，使得系统数据不一致。
4. 太过保守 任意一个节点失败就会导致整个事务失败，没有完善的容错机制。

#### 三阶段提交（3PC）

引入了超时机制

新增了一个预提交阶段，作为栅栏

#### 补偿事务（TCC）

TCC 其实就是采用的补偿机制，其核心思想是：针对每个操作，都要注册一个与其对应的确认和补偿（撤销）操作。它分为三个阶段：

- Try 阶段主要是对业务系统做检测及资源预留
- Confirm 阶段主要是对业务系统做确认提交，Try阶段执行成功并开始执行 Confirm阶段时，默认 Confirm阶段是不会出错的。即：只要Try成功，Confirm一定成功。
- Cancel 阶段主要是在业务执行错误，需要回滚的状态下执行的业务取消，预留资源释放。

优点： 跟2PC比起来，实现以及流程相对简单了一些，但数据的一致性比2PC也要差一些

缺点： 缺点还是比较明显的，在2,3步中都有可能失败。TCC属于应用层的一种补偿方式，所以需要程序员在实现的时候多写很多补偿的代码，在一些场景中，一些业务流程可能用TCC不太好定义及处理。

#### 本地消息表（异步确保）

本地消息表与业务数据表处于同一个数据库中，这样就能利用本地事务来保证在对这两个表的操作满足事务特性，并且使用了消息队列来保证最终一致性。

1. 在分布式事务操作的一方完成写业务数据的操作之后向本地消息表发送一个消息，本地事务能保证这个消息一定会被写入本地消息表中。
2. 之后将本地消息表中的消息转发到 Kafka 等消息队列中，如果转发成功则将消息从本地消息表中删除，否则继续重新转发。
3. 在分布式事务操作的另一方从消息队列中读取一个消息，并执行消息中的操作。

![本地消息表](https://oscimg.oschina.net/oscnet/915a02484990dda820410c8cad2389c5386.jpg)

优点： 一种非常经典的实现，避免了分布式事务，实现了最终一致性。

缺点： 消息表会耦合到业务系统中，如果没有封装好的解决方案，会有很多杂活需要处理。

#### MQ 事务消息

第一阶段Prepared消息，这个消息对消费者来说不可见，然后发送成功后发送方再执行本地事务。

再根据本地事务的结果向 Broker 发送 Commit 或者 RollBack 命令。

并且 RocketMQ 的发送方会提供一个反查事务状态接口，如果一段时间内半消息没有收到任何操作请求，那么 Broker 会通过反查接口得知发送方事务是否执行成功，然后执行 Commit 或者 RollBack 命令。

如果是 Commit 那么订阅方就能收到这条消息，然后再做对应的操作，做完了之后再消费这条消息即可。

如果是 RollBack 那么订阅方收不到这条消息，等于事务就没执行过。

![RocketMQ](https://pic2.zhimg.com/80/v2-72ba7bed684e855606c44ddda185987d_720w.jpg)

优点： 实现了最终一致性，不需要依赖本地数据库事务。

缺点： 实现难度大，主流MQ不支持，RocketMQ事务消息部分代码也未开源。

#### 最大努力通知

最大努力通知其实只是表明了一种柔性事务的思想：我已经尽力我最大的努力想达成事务的最终一致了。

适用于对时间不敏感的业务，例如短信通知。

#### Saga

Saga是一个长活事务可被分解成可以交错运行的子事务集合。其中每个子事务都是一个保持数据库一致性的真实事务。

- 每个Saga由一系列sub-transaction Ti 组成
- 每个Ti 都有对应的补偿动作Ci，补偿动作用于撤销Ti造成的结果

可以看到，和TCC相比，Saga没有“预留”动作，它的Ti就是直接提交到库。

##### Saga的执行顺序有两种

- T1, T2, T3, ..., Tn
- T1, T2, ..., Tj, Cj,..., C2, C1，其中0 < j < n

##### Saga定义了两种恢复策略

backward recovery，向后恢复，补偿所有已完成的事务，如果任一子事务失败。即上面提到的第二种执行顺序，其中j是发生错误的sub-transaction，这种做法的效果是撤销掉之前所有成功的sub-transation，使得整个Saga的执行结果撤销。
forward recovery，向前恢复，重试失败的事务，假设每个子事务最终都会成功。适用于必须要成功的场景，执行顺序是类似于这样的：T1, T2, ..., Tj(失败), Tj(重试),..., Tn，其中j是发生错误的sub-transaction。该情况下不需要Ci。

显然，向前恢复没有必要提供补偿事务，如果你的业务中，子事务（最终）总会成功，或补偿事务难以定义或不可能，向前恢复更符合你的需求。

理论上补偿事务永不失败，然而，在分布式世界中，服务器可能会宕机，网络可能会失败，甚至数据中心也可能会停电。在这种情况下我们能做些什么？ 最后的手段是提供回退措施，比如人工干预。

##### 和TCC对比

Saga相比TCC的缺点是缺少预留动作，导致补偿动作的实现比较麻烦：Ti就是commit，比如一个业务是发送邮件，在TCC模式下，先保存草稿（Try）再发送（Confirm），撤销的话直接删除草稿（Cancel）就行了。而Saga则就直接发送邮件了（Ti），如果要撤销则得再发送一份邮件说明撤销（Ci），实现起来有一些麻烦。
