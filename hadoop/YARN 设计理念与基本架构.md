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
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/第二代MR框架基本架构2.png?raw=true)

如上图所示，YARN主要由ResourceManager、NodeManager、ApplicationMaster和Container等几个组件构成
- 1.ResourceManager




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

