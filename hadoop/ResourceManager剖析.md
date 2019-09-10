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

#### 3.2 三个服务的配合
- [ApplicationMasterLauncher](https://github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-server/hadoop-yarn-server-resourcemanager/src/main/java/org/apache/hadoop/yarn/server/resourcemanager/amlauncher/ApplicationMasterLauncher.java)
    - 既是服务，也是事件处理器
    - 作为事件处理器处理AMLauncherEvent类型的事件，主要有两种：
        - LAUNCH：请求启动一个AM的事件。
        - CLEANUP：请求清理一个AM的事件。
- AMLivelinessMonitor
    - 周期性遍历所有应用的AM，如果在一定时间（yarn.am.liveness-monitor.expiry-interval-ms配置，默认10min）内未汇报心跳，则认为死掉了
    - AM挂掉后，它上面所有正在运行的Container被置为运行失败（RM不会重新执行这些Container，由AM决定是否重新执行）  todo: 谁负责做个事情
    - AM运行失败，RM重新为其申请资源（可以在提交应用是通过函数ApplicationSubmissionContext#setMaxAppAttempts设置重试次数，默认是2）
- ApplicationMasterService（AMS）
    - 负责接收AM的请求：注册、心跳、清理
    - AM启动后要做的第一件事就是向RM注册，通过ApplicationMasterProtocol#registerApplicationMaster实现
    - 心跳通过ApplicationMasterProtocol#allocate实现，有3个作用 
        - 请求支援
        - 获取新分配的资源
        - 形成周期性心跳
    - AM运行结束，通过ApplicationMasterProtocol#finishApplicationMaster实现
    
### 4. NodeManager管理模块
- NMLivelinessMonitor
    - 周期性遍历所有NM，如果在一定时间（yarn.nm.liveness-monitor.expiry-interval-ms配置，默认10min）内未汇报心跳，则认为死掉了，会把其上的所有Container置为失败
- NodesListManager
    - 管理exclude和include节点列表，可通过yarn.resourcemanager.nodes.include-path和yarn.resourcemanager.nodes.exclude-path配置
    - 管理员可以通过 bin/yarn rmadmin -refreshNodes 动态加载上面的两个配置
- ResourceTrackerService
    - 负责处理来自NM的请求：注册、心跳
    - NM启动的第一件事就是向RM注册，通过ResourceTracker#registerNodeManager实现
    - 通过ResourceTracker#nodeHeartbeat汇报心跳
    - 一个节点总的可用资源在NM启动的时候向RM注册，之后不可动态修改，需要重启（YARN-291在尝试引入动态修改的机制）
### 5. Application管理模块
YARN中应用程序是一个比较宽泛的概念，一个应用可能启动多个运行实例，每个实例由一个ApplicationMaster以及一组该AM启动的任务组成
- ApplicationACLsManager
    - 默认情况下，任一个普通用户可以查看所有其他用户的应用程序。也可以在应用代码中指定，如下所示
    - 一些运行在yarn上的计算引擎，比如MR，可以通过参数mapreduce.job.acl-view-job和mapreduce.job.acl-modify-job为应用设置权限
```
// 用户userX编写了代码，并做了授权
ContainerLaunchContext ctx = Records.newRecord(ContainerLaunchContext.class);
Map<ApplicationAccessType, String> acls = new HashMap<>();
acls.put(ApplicationAccessType.VIEW_APP, "user1 group1"); // 授予用户user1和用户组group1查看权限
acls.put(ApplicationAccessType.MODIFY_APP, "user1"); // 授予user1修改权限
ctx.setApplicationACLs(acls);
// 设置之后，应用的所有者userX、集群管理员（通过在yarn-site.xml中设置yarn.admin.acl）和user1具有查看和修改权限，group1具有查询权限
```
- RMAppManager： 管理应用程序的启动和关闭
    - ClientRMService接收到客户端的提交应用请求之后，将调用RMAppManager#submitApplication创建一个RMApp对象，该对象负责维护这个应用的整个生命周期
    - RMApp运行结束，向RMAppManager发送一个RMAppManagerEventType.APP_COMPLETED事件。RMAppManager收到事件后调用finishApplication进行收尾工作，包括：
        - 将应用放到已完成应用列表中，列表大小默认为10000，可通过yarn.resourcemanager.max-completed-applications修改（这个列表是放在内存中的，超过后只能通过磁盘查看日志）
        - 将应用从RMStateStore中移除。RMStateStore记录了应用的运行信息，便于集群故障重启后RM能从这些日志中恢复应用状态，避免全部重新运行
- ContainerAllocationExpirer：
    - AM收到一个Container不能长时间占用（会减低集群的利用率）
    - 默认10min，可以通过yarn.resourcemanager.rm.container-allocation.expiry-interval-ms修改

### 6. 状态机管理
#### 6.1 RMApp状态机
RMApp的实现类RMAppImpl，维护一个Application的完整生命周期，是RM的组成之一。
- 记录了Application可能存在的各个状态（RMAppState）以及导致状态转移的事件（RMAppEvent）。状态转移的同时会触发一个行为，也就是回调函数
- 保存了Application的基本信息和迄今为止所有运行尝试（Application Attempt）的信息

如下图：一共9种状态、12种事件

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RMApp状态机.png?raw=true)

```java
public enum RMAppState {
  NEW,          // 初始状态
  NEW_SAVING,   // 日志记录应用程序基本信息时就处于这个状态。
  SUBMITTED,    // 应用状态已提交。客户端通过ApplicationClientProtocol#submitApplication提交应用，
                // 通过合法性验证和日志记录后，RM创建一个RMAppAttemptImpl对象进行第一次尝试，并把APP状态设置为SUBMITTED
  ACCEPTED,     // 资源调度器同意接受该应用之后所处的状态。应用除了在ClientRMService进行合法性检查，也需要结果资源调度器的合法性检查。比如是否达到应用提交次数的上限
  RUNNING,      
  REMOVING,     // todo：？？
  FINISHING,    // AM通过ApplicationMasterProtocol#finishApplicationMaster通知RM自己运行结束
  FINISHED,     // NM通过心跳汇报AM所在的Container运行结束，这个时候状态才为FINISHED
  FAILED,       // 多种应用可能导致失败：OOM、bug、硬件故障等。
                // 注意：接收到ATTEMPT_FAILED事件后不会立即进入该状态，而是先检查失败次数是否已达到上限（通过yarn.resourcemanager.am.max-attempts配置默认是2）
  KILLED
}
```

- RMAppAttempt： 维护一次运行尝试的生命周期
- RMContainer： 维护了一个Container的运行周期，包括从创建到运行结束整个过程
- RMNode： 为了一个NM的生命周期，包括从启动到运行结束整个过程