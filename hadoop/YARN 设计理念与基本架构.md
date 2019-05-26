## YARN 设计理念与基本架构

#### 2.1.1 MRv1局限性
- 拓展性差。JobTrance同时负责资源监控和作业控制，是系统最大的瓶颈，严重制约可拓展性
- 可靠性差。master/slave架构，master存在单点故障
- 资源利用率低。采用基于槽位的资源分配模型。
> 槽位是一种粗粒度的资源
  划分单位，通常一个任务不会用完槽位对应的资源，且其他任务也无法使用这些空
  闲资源。此外，Hadoop 将槽位分为Map Slot 和Reduce Slot 两种，且不允许它们之
  间共享，常常会导致一种槽位资源紧张而另外一种闲置（比如一个作业刚刚提交时，
  只会运行Map Task，此时Reduce Slot 闲置）。
- 无法支持多种计算框架。MR是基于磁盘的离线计算框架，除此之外还有：内存计算框架、流式计算框架、迭代式计算框架

MRv2把资源管理功能抽象成一个**独立的通用的**系统YARN，这意味着hadoop2可以打造一个以YARN为核心的生态系统。

#### 2.1.2　轻量级弹性计算平台
- 发展趋势：出现越来越多的计算框架解决某一特定的问题，比如支持离线处理的MapReduce，支持在线处理的Storm，迭代式计算框架Spark 流式处理框架S4
- 使用一种计算框架一个集群的话运维成本高、资源利用率低，因此会倾向于共享集群，对资源统一管理并对各个任务所需要的资源进行隔离（比如使用cgroup）
- YARN就是这样一个轻量级弹性计算平台，能够对多种框架进行统一管理
    - 资源利用率高、维护成本低、数据共享（不需要跨集群移动数据）

#### hadoop1.0和2.0以及MRv1和MRv2的区别
- hadoop1.0的MR有一个JobTracker和多个TaskTracker组成
- hadoop2.0的优化点：
    - 从单NameNode制约拓展性到提出Federation HDFS（联邦HDFS），让多个NameNode分管不同的目录进而实现访问隔离和横向扩展，解决单点故障
    - 针对MRv1不足，将JobTracker中的资源管理和作业控制功能分别交由组件ResourceManager（负责所有应用的资源分配）和ApplicationMaster（负责管理一个应用）
- MRv1由三部分组成
    - 编程模型：把问题抽象成Map和Reduce，Map把输入解析成k/v处理后写入**本地磁盘**，Reduce则做规约处理，把结果写入HDFS
    - 数据处理引擎：MapTask和ReduceTask组成
    - 运行时环境：一个JobTracker（负责资源管理和所有作业控制）和多个TaskTracker（负责接收JobTracker命令并执行）构成
- MRv2跟MRv1相比，运行时环境不同，体现在YARN负责资源管理和调度，ApplicationMaster负责一个作业的管理

- 已MR为核心和以YARN为核心的软件栈对比

![image](https://raw.githubusercontent.com/fancyChuan/read-the-source/master/hadoop/img/%E4%BB%A5MR%E4%B8%BA%E6%A0%B8%E5%BF%83%E4%B8%8E%E4%BB%A5YARN%E4%B8%BA%E6%A0%B8%E5%BF%83%E7%9A%84%E8%BD%AF%E4%BB%B6%E6%A0%88%E5%AF%B9%E6%AF%94.png)

### 2.3 YARN基本设计思想

#### 2.3.1 基本框架对比
- 第一代MapReduce框架基本架构

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/第一代MR框架基本架构.png?raw=true)

- 第二代MR基本架构

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/第二代MR框架基本架构.png?raw=true)
#### 2.3.2 编程模型对比
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/第二代MR框架基本架构2.png?raw=true)
### 2.4 基本架构
YARN总体上认识Master/Slave结构，RM为master而NM是slave，RM负责对NM上的资源进行统一管理和调度。ApplicationMaster负责想ResourceManager申请资源，并要求NodeManager启动可以占用一定资源的任务。不同的ApplicationMaster在不同的节点上，不会互相影响
#### 2.4.1 YARN基本组成结构
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/YARN的基本架构.png?raw=true)

如上图所示，YARN主要由ResourceManager、NodeManager、ApplicationMaster和Container等几个组件构成
- 1.ResourceManager
    - 全局资源管理器，由两个组件构成：调度器Scheduler、应用管理器Applications Manager, ASM（注意不是ApplicationMaster）
    - (1)调度器：
        - “纯调度器”，仅负责资源分配，不负责任何跟应用相关的工作，比如监控、失败重试等
        - 可以理解为创建资源容器Container（动态分配内存CPU等），从而限定每个任务的资源量
        - 是一个可插拔的组建，可以设计自己的调度器，YARN也提供了FairScheduler和CapacityScheduler
    - (2)应用程序管理器：
        - 负责管理所有应用，包括程序的提交、与调度器协商资源以启动ApplicationMaster、监控ApplicationMaster运行状态并在失败时重启
- 2.ApplicationMaster(AM)
    - 与RMB调度器协商以获得资源（用Container表示）
    - 将得到的资源进一步分配给内部的任务
    - 与NM通信以启动/停止任务
    - 监控任务运行状态，并在任务失败时重新申请资源重启
- 3.NodeManager(NM)
    - 定时向RMB汇报节点资源使用情况
    - 接受并处理来自AM的Container启动、停止等各种请求
- 4.Container
    - YARN中的资源抽象，封装了某个节点上的多维度资源（内存、磁盘、网络、CPU等）
    - AM向RM申请资源时，返回的资源是用Container表示的
    - 不同MRv1中的slot，Container是一个动态资源划分单位，根据application需求动态生成
    - 使用轻量级隔离机制Cgroups进行资源隔离
#### 2.4.2 YARN通信协议
任何组件之间通过RPC协议相互通信，通信双方一个为client，另一个为server端，而且是client主动连接server，也就是说YARN采取的是**拉式(pull-based)通信模型**

YARN主要涉及的协议：
- 作业提交客户端与RM之间的协议-ApplicationClientProtocol，包括提交应用程序、查询应用程序状态等
- Admin管理员与RM之间的协议-ResourceManagerAdministratorProtocol，包括更新系统配置文件如节点黑白名单、用户队列权限等
- AM和RM之间的协议-ApplicationMasterProtocol，AM通过该协议注册和注销自己，并为各任务申请资源
- AN和NM之间的协议-ContainerManagementProtocol，AM通过该协议要求NM启动或停止Container，获取各个Container的使用状态等
- NM和RM之间的协议-ResourceTracker，NM通过该协议想RM注册，并发送心跳信息汇报节点资源使用情况和Container运行情况

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/YARN的RPC协议.png?raw=true)


### 2.5 YARN工作流程
YARN上有短应用程序（运行时间有限）和长应用程序（如HBASE service）两类，也就是一类是直接处理数据，另一个用于部署服务，在服务至上处理数据

运行运行应用有两个阶段：1.申请启动ApplicationMaster 2.由ApplicationMaster创建程序、申请资源并监控运行过程

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/YARN的工作流程.png?raw=true)

- 步骤1　用户向YARN 中提交应用程序， 其中包括ApplicationMaster 程序、启动ApplicationMaster 的命令、用户程序等。
- 步骤2　ResourceManager 为该应用程序分配第一个Container， 并与对应的NodeManager 通信，要求它在这个Container 中启动应用程序的ApplicationMaster。
- 步骤3　ApplicationMaster 首先向ResourceManager 注册， 这样用户可以直接通过ResourceManage 查看应用程序的运行状态，然后它将为各个任务申请资源，并监控它的运行状态，直到运行结束，即重复步骤4~7。
- 步骤4　ApplicationMaster 采用轮询的方式通过RPC 协议向ResourceManager 申请和领取资源。
- 步骤5　一旦ApplicationMaster 申请到资源后，便与对应的NodeManager 通信，要求它启动任务。
- 步骤6　NodeManager 为任务设置好运行环境（包括环境变量、JAR 包、二进制程序等）后，将任务启动命令写到一个脚本中，并通过运行该脚本启动任务。
- 步骤7　各个任务通过某个RPC 协议向ApplicationMaster 汇报自己的状态和进度，以让ApplicationMaster 随时掌握各个任务的运行状态，从而可以在任务失败时重新启动任务。
> 在应用程序运行过程中，用户可随时通过RPC 向ApplicationMaster 查询应用程序的当前运行状态。
- 步骤8　应用程序运行完成后，ApplicationMaster 向ResourceManager 注销并关闭自己。


todo:版本变迁的情况