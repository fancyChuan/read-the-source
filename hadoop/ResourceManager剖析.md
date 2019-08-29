## ResourceManager剖析

### 1. 概述
#### 1.1 基本职能
整体上，RM总是扮演Server角色，并通过两个RPC协议与NM和AM交互：
- ResourceTracker
    - NM通过该协议向RM注册、汇报节点健康状况和Container运行状态
    - 领取RM下达的命令：包括重新初始化、清理Container等
    - NM周期性主动向RM发起请求，采用的是“pull模型”
- ApplicationMasterProtocol
    - AM向RM注册，申请、释放资源
    - AM和RM之间也采用了pull模型，由AM周期性向RM发起请求
- ApplicationClientProtocol
    - 应用的客户端向RM提交应用、查询应用状态和控制应用等

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/ResourceManager相关的RPC协议.png?raw=true)

RM的主要功能：
- 与客户端交互，处理客户端请求
- 启动和管理AM，并在失败时重新启动
- 管理NM，接受NM的资源汇报信息，向NM下达管理指令
- 资源管理和调度，接受来自AM的资源申请请求，并分配资源

#### 1.2 RM内部架构
- 用户交互模块
    - ClientRMService： 为普通用户提供服务，比如提交应用、获取应用状态
    - AdminService： 为管理员提供的一套独立的服务接口，防止大量普通用户请求使管理员的管理命令饿死。比如动态更新节点列表、更新ACL、更新队列等
    - WebApp： 更友好展示集群资源使用情况核应用运行状态等信息
- NM管理模块
    - NMLiveLinessMonitor: 监控NM是否活着，如果超过时间（默认10分钟）没有汇报心跳信息就认为已死掉，需要从集群中剔除
    - NodesListManager：维护正常节点和异常节点列表，管理exclude、include（黑白名单）节点列表
    - ResourceTrackerService： 处理来自NM的请求，主要包括心跳和注册两种
- AM管理模块
    - AMLivelinessMonitor： 监控AM是否活着。如果默认10分钟没有汇报心跳信息，则认为死掉了，会做以下处理：
        - 它上面正在运行的Container需要置为失败状态
        - AM本身会被重新分配到另一个节点上运行，默认每个AM重试2次，可修改
    - ApplicationMasterLauncher： 与NM通信，要求为某个应用启动AM
    - ApplicationMasterService（AMS）：处理来自AM的请求，主要包括注册和心跳两种信息
- Application管理模块
    - ApplicationACLsManager： 管理app的访问权限，包括两部分：
        - 查看权限： 基本信息
        - 修改权限： 修改应用优先级、kill应用等
    - RMAppManager： 管理应用程序的启动和关闭
    - ContainerAllocationExpirer： AM收到RM新分配的Container后需要在默认10分钟内在对应的NM启动该Container，否则RM将强制回收。已经分配的Container是否被回收由ContainerAllocationExpirer决定和执行
- 状态机管理模块
    - RMApp： 维护了一个应用的整个运行周期，包括启动到运行结束。一个Application会启动多个Application Attempt，因此也可以认为维护了一个Application启动的所有运行实例（Attempt）、
    - RMAppAttempt： 维护一次运行尝试的生命周期
    - RMContainer： 维护了一个Container的运行周期，包括从创建到运行结束整个过程
    - RMNode： 为了一个NM的生命周期，包括从启动到运行结束整个过程
- 安全管理模块
    - 主要由ClientToAMSecretManager、ContainerTokenSecretManager、ApplicationTokenSecretManager等模块组成
- 资源分配模块
    - 主要涉及ResourceScheduler模块，负责按照一定的约束条件分配资源
    - ResourceScheduler是一个可拔插式模块，yarn自带了一个批处理资源调度器FIFO和两个多用户调度器Fair Scheduler、Capacity Scheduler(默认的资源调度器)

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RM内部架构图.png?raw=true)


#### 1.3 ResourceManager事件与事件处理器
RM采用事件驱动机制，内部所有服务和组件通过**中央异步调度器**组织在一起，不同组件之间通过事件进行交互，从而形成一个异步并行的高效系统

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RM内部事件与事件处理器.png?raw=true)

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RM内部事件与事件处理器交互图.png?raw=true)

### 2. 用户交互模块
#### 2.1 ClientRMService
是一个RPC Server，实现了ApplicationClientProtocol协议。 代码位置:[ClientRMService.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-resourcemanager/src/main/java/org/apache/hadoop/yarn/server/resourcemanager/ClientRMService.java)

类中有一个RMContext对象，通过该对象可以获取RM中绝大部分信息，包括节点列表、队列信息、应用列表等。其实现类为：[RMContextImpl.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-resourcemanager/src/main/java/org/apache/hadoop/yarn/server/resourcemanager/RMContextImpl.java)

#### 2.2 AdminService
管理员列表由 yarn.admin.acl 指定，在yarn-site.xml中设置，默认是"*"表示所有人都是管理员

实现代码：[AdminService.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-resourcemanager/src/main/java/org/apache/hadoop/yarn/server/resourcemanager/AdminService.java)

### 3. ApplicationMaster管理
#### 3.1 AM整个生命周期
- 步骤1：ApplicationMasterLauncher与对应的NM通信，启动AM
- 步骤2：AM启动后ApplicationMasterLauncher以事件的形式把AM注册到AMLivelinessMonitor，以启动心跳监控
- 步骤3：AM向ApplicationMasterService注册自己，并将自己的host、port等信息汇报给AMS
- 步骤4：AM周期性向AMS汇报“心跳”信息
- 步骤5：AMS收到心跳信息后，通知AMLivelinessMonitor更新该应用程序最近汇报心跳的时间
- 步骤6：应用运行完成后，AM向AMS请求注销自己
- 步骤7：AMS收到注销请求后，标注应用运行状态为完成，同时通知AMLivelinessMonitor移除对AM的心跳监控

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/ApplicationMaster启动过程.png?raw=true)



- AMLivelinessMonitor
- ApplicationMasterLauncher
- ApplicationMasterService（AMS）
