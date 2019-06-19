## YARN基础库

YARN的基础库主要有：
- Protocol Buffers
- Apache Avro
- RPC库
- 服务库和事件库
- 状态机库


#### 3.3.1 RPC通信模型
RPC通常采用客户机/服务器模型，两个相互协议的通信模型实现请求-应答协议，有同步方式和异步方式两种

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC同步模式与异步模式对比.png?raw=true)

一个典型的RPC框架主要包含几部分：
- 通信模块
- Stub程序：可以理解为代理程序
- 调度程序：接受来自通信模块的请求信息，根据标识选择一个Stub程序处理
- 客户程序/服务过程

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC通用架构.png?raw=true)

#### 3.3.3 总体架构
- 序列化层
- 函数调用层
- 网络传输层：基于TCP/IP的Socket机制
- 服务器端处理框架：Hadoop RPC采用了基于Reactor设计模型的事件驱动I/O模型

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC整体架构.png?raw=true)

#### 3.3.4 Hadoop RPC使用方法
Hadoop RPC对外提供了两种接口：getProxy/waitForProxy用于构造一个客户端代理对象， RPC.builder().build() 为某个协议实例构造一个服务器对象

步骤大致为：
- 1.定义RPC协议：ClientProtocol
- 2.实现RPC协议：ClientProtocolImpl
- 3.构造并启动RPC Server：MyRPCServer
- 4.构造RPC client并发送RPC请求：MyRPCClient

参见 [自定义RPC实现](https://github.com/fancyChuan/read-the-source/tree/master/hadoop/src/yarn/rpc)

#### 3.3.5 Hadoop RPC类详解
主要由三个大类组成：RPC、Client、Server，分别对应对外编程接口、客户端实现和服务器实现
- ipc.RPC类
    - 构建RPC客户端的方法：getProxy() waitForProxy()
    - 客户端销毁方法：stopProxy()
    - 服务端构建方法： PRC.builder().build()， 之后server.start()启动
    - 与Hadoop1.x中的RPC近支持Writable序列化方式不同，Hadoop2.x允许使用其他框架，通过RPC.setProtocolEngine()设定
- ipc.Client类
    - 主要功能：发送远程过程调用信息并接收执行结果，有两个重要的内部类：Call和Connection
    - Call类：封装一个RPC请求
    - Connection类：Client和Server之间维护一个通信连接
    - 处理流程如下图：
    
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC-Client处理流程.png?raw=true)
- ipc.Server类
    - Master/Slave结构，Master是系统的单点(如NameNode和JobTracker)，是制约系统性能和可拓展性的最关键因素之一
    - ipc.Server将高并发和可拓展性作为设计目标，采用了很多技术：线程池、事件驱动、Reactor设计模式等
    - Reactor
        - 并发编程中一种基于事件驱动的设计模式，有两个特点：
            - 通过派发、分离I/O操作事件提高系统并发性能
            - 提供粗粒度的并发控制，使用单线程实现，避免了复杂的同步处理
        - 几个角色
            - Reactor：I/O事件的派发者
            - Acceptor：接受来自Client的连接，建立与Client对应的Handler，并向Reactor注册此Handler
            - Handler：与Client通信的实体，并按一定的过程实现业务的处理
            - Reader/Sender：为加快处理速度，Reactor模式往往构造一个存放数据处理线程的线程池，一般分离Handler中的读和写两个过程，注册成读事件、写事件，然后交由Reader、Sender处理
    - 处理细节，主要划分为三个阶段：接受请求、处理请求、返回结果
        - 接受请求：Listner及其内部的Reader配合
        - 处理请求：Handler线程完成处理及结果返回
        - 返回结果：Responder线程在结果过大或者网络异常(网络过慢)时获得发送任务

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/Reactor模式工作原理.png?raw=true)                    

    
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC-Server处理流程.png?raw=true)

#### 3.3.6 Hadoop RPC参数调优
可配置参数：
- Reader线程数目： ipc.server.read.threadpool.size，默认为1，也就是默认Server只包含一个Reader线程
- 每个Handler线程对应的最大Call数目：ipc.server.handler.queue.size，默认是100，每个Handler线程对应的Call队列长度为100。如果有10个Handler，那么整个Call队列最大长度为100*10
- Handler线程数目：yarn.resourcemanager.resource-tracker.client.thread-count和dfs.namenode.service.handler.count指定，默认是50和10
- 客户端最大重试次数：ipc.client.connect.max.retries，默认为10（每两次之间相隔1秒）

#### 3.37 YARN RPC实现
- Hadoop RPC的不足：
    - 仅支持java，如果用户希望直接用C/C++读写HDFS就需要有C/C++的客户端
    - 当前Hadoop版本较多，不同版本之间不能通信。比如0.20.2的JobTracker与2.21.0的TaskTracker不能通信
- 改进：Hadoop YARN将RPC中的序列化部分剥离开，以遍集合现有的开源RPC框架
    - RPC类变成一个工厂，具体的RPC实现授权给RpcEngine实现类（比如WritableRpcEngine、AvroRpcEngine、ProtobufRpcEngine）
    - 用户也可以通过配置参数：rpc.engine.{protocol}以指定协议{protocol}
    - 当前Hadoop RPC只是采用了这些开源框架的序列化机制，底层的函数调用仍采用Hadoop自带的
    - 对外暴露YarnRPC
- YARN采用Protocol Buffers作为默认的 序列化机智，带来的好处有：
    - 基础了Protocol Buffers的优势：
        - 允许在保持向后兼容性的前提下修改协议
        - 支持多语言，方便编写飞java客户端
        - 比Hadoop自带的Writable在性能方面有很大提升
    - 支持升级回滚，比如可以对主备NameNode进行在线升级而不需要考虑版本和协议兼容性

### 3.4 服务库与事件库
#### 3.4.1 服务库
YARN采用基于服务的对象管理模式管理生命周期较长的对象，有几个特点：
- 将被服务化的对象分为4个状态：NOTINITED（被创建）、INITED（已初始化）、STARTED（已启动）、STOPPED（已停止）
- 任何服务状态变化都可以触发另外一些动作
- 可通过组合的方式对任意服务进行组合，以便统一管理

YARN所有关于服务的类图在 org.apache.hadoop.service 中
- 所有的服务对象最终都实现了Service接口，它定义了最基本的服务阶段：初始化、启动、停止等
- AbstractService类：最基本的Service实现，对于非组合服务，直接继承该类即可
- CompositeService类：需要组合服务的对象，继承该类
    - 比如ResourceManager是一个组合服务，组合了：ClientRMService、ApplicationMasterLauncher、ApplicationMasterService
    - NodeManager也是组合服务，和RM一样，内部包含了多个单一服务和组合服务，以实现对内部多种服务的统一管理

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/YARN中服务模型的类图.png?raw=true)

#### 3.4.2 事件库
YARN采用基于事件驱动的并发模型，能够大大增强并发性，提供系统整体性能
- YARN把各种处理逻辑抽象成事件和对应事件调度器，并将事件的处理过程分隔成多个步骤，用有限状态机表示，处理过程大致如下：
    - 处理请求作为事件进入系统，由中央异步调度器（AsyncDispatcher）传递给相应的事件调度器（EventHandler）
    - 事件调度器可能转发给另一个事件调度器，也可能交给带有有限状态机的事件处理器
    - 处理结果以事件的形式输出给中央异步调度器，重复前两个步骤知道处理完成（达到终止条件）

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/YARN的事件处理模型.png?raw=true)

- YARN中，所有核心服务实际上都是一个中央异步调度器，包括RM、NM、MRAppMaster
- 使用YARN事件库时，需要先定义一系列事件Event与事件处理器EventHandler，并注册到中央异步调度器以实现事件统一管理和调度
> 参见 [MRAppMaster.java](https://github.com/fancyChuan/read-the-source/blob/master/hadoop-2.2.0-src/hadoop-mapreduce-project/hadoop-mapreduce-client/hadoop-mapreduce-client-app/src/main/java/org/apache/hadoop/mapreduce/v2/app/MRAppMaster.java)

服务化和事件驱动软件设计思想的引入，是的YARN具有低耦合、高内聚的特点，各个模块只要完成各自功能，模块之间采用事件联系起来，系统设计简单且维护方便
#### 3.3.3 服务库和事件库的使用方法
参见 [服务库和事件库使用方法](https://github.com/fancyChuan/read-the-source/tree/master/hadoop/src/yarn/event)

#### 3.4.4 事件驱动带来的变化
- MRv1对象间的作用关系是基于函数调用的
    - 函数调用的过程是串行的
    - 后来的MRv1通过启动独立线程下载文件解决了阻塞问题，但不是大系统的彻底解决方法
    
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/基于函数调用的工作流程.png?raw=true)

- MRv2引入事件驱动编程模型(更高效、异步、并发)
    - 所有对象被抽象成事件处理器，他们之间通过事件相互关联
    - 每种事件处理器处理一种类型的事件，同时根据需要触发另一个种事件
    - 比如需要下载文件时，只需要向中央异步处理器发送一个事件即可，无需等待，可以先完成其他事情
    
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/基于事件驱动的工作流程.png?raw=true)

### 3.5 状态机
YARN中每种状态由四元组标识：preState(转换前状态)/postState(转换后状态)/event(事件)/hook(回调函数)

YARN中有三种转化方式：
- 一个初始状态、一个最终状态、一种事件
- 一个初始状态、多个最终状态、一种事件
> 接收到事件Event后，执行状态转移函数hook，根据返回值确定返回哪个状态
- 一个初始状态、一个最终状态、多种事件
> 接收到多个事件中的一个，都可以从状态preState到postState

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/YARN状态转化方式.png?raw=true)

#### 3.5.2 状态机类
yarn实现了一个非常简单的状态机库：org.apache.hadoop.yarn.state
- 对外提供一个状态机工厂 StatemachineFactory，提供多种addTransition方法用于添加各种状态转移
- 添加完毕后，调用installTopology完成一个状态机的构建

参见：[状态机类的示例](https://github.com/fancyChuan/read-the-source/tree/master/hadoop/src/yarn/state)

YARN实现了多个状态机对象，包括：
- ResourceManager中的RMAppImpl、RMAppAttempImpl、RMContainerImpl和RMNodeImpl
- NodeManager中的ApplicationImpl、ContainerImpl和LocalizedResource
- MRAppMaster中的JobImpl、TaskImpl和TaskAttemptImpl

#### 3.5.4 状态机可视化
yarn提供了一个状态机可视化工具，步骤为：
- mvn compile -Pvisualize 会生成.gv文件
- 把生成的.gv文件使用可视化包graphviz生成状态机图： dot -Tpng NodeManager.gv > NodeManager.png

### 3.6 源码阅读指导
- Hadoop RPC
    - 源码位置：
        - HadoopRPC内部实现：hadoop-common-project\hadoop-common\src\main\java下的org.apache.hadoop.ipc
        - YARN对RPC的Protocol Buffers封装：hadoop-yarn-project\hadoop-yarn\hadoop-yarn-common\src\main\java下的org.apache.hadoop.yarn.ipc
    - 建议
        - 先尝试使用HadoopRPC实现一个C/S服务器
        - 阅读客户端代码和服务器代码
    - 检验标准：能描述清楚下面两个流程
        - 客户端发送一个请求到接收到请求应答的整个过程是怎么样的，依次经过哪些函数的调用和通信过程
        - 多个客户端并发发送请求到服务器后，服务器是如何处理的
- 服务库、事件库和状态机
    - 源码位置
        - 服务库位置：hadoop-common-project\hadoop-common\src\main\java下的 org.apache.hadoop.service
        - 事件库位置：hadoop-yarn-project\hadoop-yarn\hadoop-yarn-common\src\main\java下的 org.apache.hadoop.yarn.event
        - 状态机库位置：hadoop-yarn-project\hadoop-yarn\hadoop-yarn-common\src\main\java下的 org.apache.hadoop.yarn.state
    - 建议
        - 首先弄清各个包的对外接口
        - 尝试编写几个实例使用这几个库
        - 跟着YARN源码学习这几个库