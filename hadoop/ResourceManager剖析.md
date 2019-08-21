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