# Spring

## Spring Framework

### IOC

org.springframework.beans 和 org.springframework.context 包是 Spring Framework's IoC 容器的基石，BeanFactory 接口提供了能够管理任何类型对象的高级配置机制，ApplicationContext 作为 BeanFactory 的子接口，添加了更多特定于企业级功能。  
在 Spring 中，Bean 是一个由 Spring IoC 容器实例化、组装和管理的对象，Bean 及其之间的依赖关系反映在容器使用的配置元数据中。

#### Bean Scopes

* singleton
* prototype
* request
* session
* application
* websocket

#### Container Extension Points

* BeanFactoryPostProcessor：operates on the bean configuration metadata
* BeanPostProcessor：operates on the bean

#### Standard bean lifecycle

1. BeanNameAware's setBeanName
2. BeanClassLoaderAware's setBeanClassLoader
3. BeanFactoryAware's setBeanFactory
4. EnvironmentAware's setEnvironment
5. EmbeddedValueResolverAware's setEmbeddedValueResolver
6. ResourceLoaderAware's setResourceLoader
7. ApplicationEventPublisherAware's setApplicationEventPublisher
8. MessageSourceAware's setMessageSource
9. ApplicationContextAware's setApplicationContext
10. ServletContextAware's setServletContext
11. postProcessBeforeInitialization methods of BeanPostProcessors
12. InitializingBean's afterPropertiesSet
13. a custom init-method definition
14. postProcessAfterInitialization method of BeanPostProcessors
15. postProcessBeforeDestruction methods of DestructionAwareBeanPostProcessors
16. DisposableBean's destory
17. a custom destory-method definition

### AOP

Aspect-oriened Programming (AOP) 是对 Object-oriented Programming (OOP) 思想的补充和完善。OOP 中模块化的关键单元是类，而 AOP 中的单元是 aspect。Aspect 支持横切多种类型和对象的关注点进行模块化（如事务管理）。

* Aspect: 关注点的模块化，跨越多个类。
* JoinPoint：程序执行的某个特定位置，例如一个方法的执行或一个异常的处理。
* PointCut：匹配连接点的断言。
* Advice：切面在特定连接点执行的行为，通知包含不同的类型：around、before and after。
* Introduction：引介是一种特殊的通知，它为类添加一些属性和方法。这样，即使一个业务类原本没有实现某个接口，通过AOP的引介功能，我们可以动态地为该业务类添加接口的实现逻辑，让业务类成为这个接口的实现类。  
* Weaving：织入是将通知添加对目标类具体连接点的过程。编译时、类加载时、运行时

### Transaction

#### 事务传播行为

1. PROPAGATION_REQUIRED: 如果当前事务存在，方法将会在该事务中运行。否则，会启动一个新的事务。
2. PROPAGATION_SUPPORTS: 表示当前方法不需要事务上下文，但是如果当前事务存在，会在当前事务下运行。
3. PROPAGATION_MANDATORY: 表示该方法必须在事务中运行，如果当前事务不存在，则抛出异常。
4. PROPAGATION_REQUIRED_NEW: 表示当前方法必须运行在它自己的事务中，当前事务被挂起。
5. PROPAGATION_NOT_SUPPORTED： 表示该方法不应该运行在事务中，当前事务被挂起。
6. PROPAGATION_NEVER: 表示当前方法不应该运行在事务中，如果当前有事务在运行，则抛出异常。
7. PROAGATION_NESTED： 嵌套事务。

### MVC

#### 执行流程

1. 用户发送请求至前端控制器 DispatcherServlet
2. DispatcherServlet 收到请求调用 HandlerMapping 处理器映射器。
3. 处理器映射器根据请求url找到具体的处理器，生成处理器对象及处理器拦截器（如果有则生成）一并返回给 DispatcherServlet。
4. DispatcherServlet 通过 HandlerAdapter 处理器适配器调用处理器
5. HandlerAdapter 执行处理器（handler，也叫后端控制器）。
6. Controller 执行完成返回 ModelAndView
7. HandlerAdapter 将 handler 执行结果 ModelAndView 返回给 DispatcherServlet
8. DispatcherServlet 将 ModelAndView 传给 ViewReslover 视图解析器
9. ViewReslover 解析后返回具体 View 对象
10. DispatcherServlet 对 View 进行渲染视图（即将模型数据填充至视图中）
11. DispatcherServlet 响应用户

#### DispatcherServlet

DispatcherServlet 实现 Servlet，是 Spring MVC 中的前端控制器。负责接收 request 请求, 由 HandlerMapping 完成 url 到 Controller 的映射，并交由 HandlerAdapter 调用相应的 Controller 处理。

## Spring Boot

spring boot 包含特性、自动配置

### @SpringBootApplication 注解

等价于 @SpringBootConfiguration, @EnableAutoConfiguration, @ComponentScan

### Externalized Configuration

#### 配置优先级

1. Command line arguments (java -jar app.jar --name="Spring").
2. Properties from SPRING_APPLICATION_JSON (inline JSON embedded in an environment variable or system property).
3. ServletConfig init parameters.
4. ServletContext init parameters.
5. Java System properties (java -Dname="Spring" -jar app.jar).
6. OS environment variables.
7. Profile-specific application properties outside of your packaged jar (application-{profile}.yaml).
8. Profile-specific application properties packaged inside your jar.
9. Application properties outside of your packaged jar.
10. Application properties packaged inside your jar.
11. @PropertySource annotations on your @Configuration classes.
12. Default properties (specified by setting SpringApplication.setDefaultProperties).

### Application Property Files

1. A /config subdirectory of the current directory
2. The current directory
3. A classpath /config package
4. The classpath root

## Spring Cloud

### Eureka

在具备弹性伸缩能力的分布式集群中，存在大量的服务及其实例。当服务间进行远程调用时，服务寻址则成为首要问题。因此服务注册与发现组件是分布式服务中的核心组件之一。

Eureka 在微服务架构当中作为服务注册与服务发现组件，分为 Eureka Server 和 Eureka Client 两种角色。它的服务集群采用了去中心化的分布式方案，因此当发生网络分区等故障时，服务节点总能返回当前的服务清单，从而保障了服务的可用性，而非集群之间的数据一致性。相比于其它服务注册与发现组件，Eureka 满足 CAP 定理中的 AP。

#### 工作原理

Eureka 工作分为了几个阶段，分别为

**服务注册**：Eureka Client 会通过发送 REST 请求的方式向 Eureka Server 注册自己的服务，提供自身的元数据，例如 IP 地址、端口、运行状况指标 URL、主页地址等信息。Eureka Server 接收到注册请求后，就会把这些元信息存储在一个 ConcurrentHashMap 中。

**服务续约**：在服务注册后，Eureka Client 会维护一个心跳来持续通知 Eureka Server，说明服务一直处于可用状态，防止被剔除。Eureka Client 在默认的情况下会每隔30秒发送一次心跳来进行服务续约。

**服务发现**：服务消费者（Eureka Client）在启动的时候，会发送一个 REST 请求给 Eureka Server，获取上面注册的服务清单，并且缓存在 Eureka Client 本地，默认缓存 30秒。

**服务调用**：服务消费者在获取服务清单后，就可以根据清单中的服务列表信息，查找其它服务的地址，从而进行远程调用。Eureka 有 Region 和 Zone 的概念，一个 Region 可以包含多个 Zone，在进行服务调用时，优先访问处于同一个 Zone 中的服务提供者。

**服务下线**：当 Eureka Client 需要关闭或重启时，就不希望在这个时间段内再有请求进来，所以，就需要提前发送 REST 请求给 Eureka Server，告诉 Eureka Server 自己要下线了，Eureka Server 在收到请求后，就会把该服务状态置为下线（DOWN），并把该下线事件传播出去。

**服务剔除**：有时候，服务实例可能会因为网络故障等原因导致不能提供服务，而此时该实例也没有发送请求给 Eureka Server 来进行服务下线，所以，还需要有服务剔除机制。Eureka Server 在启动的时候会创建一个定时任务，每隔一段时间（默认60秒），从当前服务清单中把超时没有续约（默认90秒）的服务剔除。

**自我保护**：既然 Eureka Server 会定时剔除超时没有续约的服务，那就可能出现一种场景，网络一段时间内发生了异常，所有的服务都没能够进行续约，Eureka Server 就把所有的服务都剔除了，这样显然不太合理。所以，就有了自我保护机制，当短时间内，统计续约失败的比例，如果达到一定阈值，则会触发自我保护机制，在该机制下，Eureka Server 不会剔除任何的微服务，等到正常后，再退出自我保护机制。

**服务同步**：Eureka Server 之间会互相进行注册，构建 Eureka Server 集群，在不同 Eureka Server 之间会进行服务同步，用来保证服务信息的一致性。  

#### 缓存机制

Eureka Server 存在三个变量：（registry、readWriteCacheMap、readOnlyCacheMap）保存服务注册信息，默认情况下定时任务每 30s 将 readWriteCacheMap 同步至 readOnlyCacheMap，每 60s 清理超过 90s 未续约的节点，Eureka Client 每 30s 从 readOnlyCacheMap 更新服务注册信息，而 UI 则从 registry 更新服务注册信息。  

![registry cache](https://img2018.cnblogs.com/blog/1682023/201905/1682023-20190515181628501-1020024778.png)

### Ribbon

#### 负载策略

* RandomRule: 随机策略
* RoundRobinRule: 轮询策略（默认）
* RetryRule: 重试策略
* BestAvailableRule: 最低并发策略
* AvailabilityFilteringRule: 可用过滤策略
* ResponseTimeWeightedRule: 响应时间加权策略
* ZoneAvoidanceRule: 区域权重策略

### Hystrix

#### 隔离策略

* Thread
* Semaphore

#### 实现原理

通过滑动窗口，默认20个请求中错误率超过 50% 打开断路器，经过 5s 休眠窗口后，断路器进入半开状态。
通过 Future 实现超时限制
命令模式

#### 熔断器降级策略

当某个服务熔断之后，服务器将不再被调用，此时客户端可以自己准备一个本地的 fallback 回调，返回一个缺省值。
