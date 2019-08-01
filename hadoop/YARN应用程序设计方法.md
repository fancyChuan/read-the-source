## YARN应用程序设计方法

### 1. 概述
yarn是一个资源管理系统，负责集群资源的管理和调用，一般需要编写两个组件：
- Client客户端
    - 负责向ResourceManager提交ApplicationMaster，并查询应用程序运行状态 
- ApplicationMaster： 需要考虑RPC调用、任务容错等细节
    - 负责向ResourceManager申请资源（以Container形式表示）
    - 负责与NodeManager通信以启动各个Container
    - 监控任务的运行状态，并在失败时为任务重新申请资源
开发这两个组件十分复杂，需要抽象出一种通用的计算框架，实现一遍Client、ApplicationMaster就可以让所有应用程序重用这两个组件。比如MapReduce就是一种通用的计算框架

编写一个yarn application会涉及的协议：
- ApplicationClientProtocol
    - 用于Client与RM之间
    - 客户端通过该协议实现将application提交到RM，查询程序的状态或者杀死应用程序等功能
- ApplicationMasterProtocol
    - 用于ApplicationMaster与RM之间
    - ApplicationMaster使用该协议向RM注册、申请资源、获取各个任务的情况
- ContainerManagementProtocol:
    - 用于ApplicationMaster和NM之间
    - ApplicationMaster使用该协议要求NodeManager启动/撤销Container或者查询Container的运行状态
### 2. 客户端设计
Application客户端的主要作用：
- 提供一系列访问接口供用户与YARN交互
- 比如提交Application、查询Application运行状态、修改Application属性（如优先级）等

TODO：这里的客户端和概述中说的Client客户端是否是同一个？？

#### 2.1 客户端编写流程
- 步骤1：Client通过RPC函数ApplicationClientProtocol#getNewApplication从RM中获取唯一的applicationID
- 步骤2：Client通过RPC函数ApplicationClientProtocol#submitApplication将ApplicationMaster提交到RM上


### 5. 源码阅读引导
- 通信协议：
- 编程库：
- YARN编程实例：