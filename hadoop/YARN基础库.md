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

参见 [自定义RPC实现](https://github.com/fancyChuan/read-the-source/tree/master/hadoop/src/rpc)

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

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/Reactor模式工作原理.png?raw=true)                    

    
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC-Server处理流程.png?raw=true)
  
### 3.5 状态机
YARN中每种状态由四元组标识：preState/postState/event/hook(回调函数)