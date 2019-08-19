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
