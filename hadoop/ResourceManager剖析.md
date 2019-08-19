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
    - 
    

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RM内部架构图.png?raw=true)