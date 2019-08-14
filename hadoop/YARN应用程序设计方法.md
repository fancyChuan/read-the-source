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

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/应用程序设计相关的通信协议.png?raw=true)

### 2. 客户端设计
Application客户端的主要作用：
- 提供一系列访问接口供用户与YARN交互
- 比如提交Application、查询Application运行状态、修改Application属性（如优先级）等

TODO：这里的客户端和概述中说的Client客户端是否是同一个？？

#### 2.1 客户端编写流程
- 步骤1：Client通过RPC函数ApplicationClientProtocol#getNewApplication从RM中获取唯一的applicationID
- 步骤2：Client通过RPC函数ApplicationClientProtocol#submitApplication将ApplicationMaster提交到RM上

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/客户端提交应用程序.png?raw=true)

除了实现提交Application的功能，客户端还需要提供以下几个接口方法的实现
- getApplicationReport() 获取application运行报告，包括用户、队列、运行状态等信息
- forceKillApplication() 强制杀死application
- getClusterMetrics() 获取集群的metric信息
- getAllApplications() 查看当前系统中所有应用程序
- getClusterNodes() 查询当前系统中所有节点信息
- ...

接口的源码位置： [org.apache.hadoop.yarn.api.ApplicationClientProtocol](https://github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-api/src/main/java/org/apache/hadoop/yarn/api/ApplicationClientProtocol.java) 

注意：为了减轻RM的负载，一般在ApplicationMaster启动之后，客户端直接与AM通信，以查询应用的状态或控制执行流程
> 比如一个MR任务：用户通过RPC协议ApplicationClientProtocol向RM提交应用，一旦MR的AM启动之后，通过了一个RPC协议MRClientProtocol直接与MRAppMaster通信，见下图：

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/客户端获取应用信息及控制应用程序.png?raw=true)

#### 2.2 客户端编程库
YARN还提供了能与RM交互完成各种操作的编程库org.apache.hadoop.yarn.client.YarnClient，这个库对常用函数进行了封装，提供了重试、容错等机制，可以使用该库快速开发一个YARN客户端

直接从RPC协议实现一个YARN客户端：[AnYarnApplicationClientDemo.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop/src/yarn/design/client/AnYarnApplicationClientDemo.java)

利用yarn提供的YarnClient实现一个客户端： 

### 3. ApplicationMaster设计
AM需要与RM和NM两个服务交互，与RM交互，获得任务计算所需的资源；与NM交互，可启动计算任务container并监控知道运行完成
#### 3.1 ApplicationMaster编写流程
##### 3.1.1 AM-RM编写流程
- 步骤1：ApplicationMaster通过RPC函数ApplicationMasterProtocol#registerApplicationMaster向RM注册
- 步骤2：ApplicationMaster通过RPC函数ApplicationMasterProtocol#allocate向RM申请资源
- 步骤3：ApplicationMaster通过RPC函数ApplicationMasterProtocol#finishApplicationMaster通知RM应用程序已运行完毕，并退出

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/ApplicationMaster与ResourceManager通信流程.png?raw=true)

参见： [AM2RMDemo.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop/src/yarn/design/client/AM2RMDemo.java)

> TODO: 思考工厂模式如何优雅的使用？为什么要通过get()来获得工厂？

##### 3.1.2 AM-NM编写流程
- 步骤1： ApplicationMaster将申请到的资源二次分配给内部的任务，并通过RPC函数ContainerManagementProtocol#startContainer与对应的NM通信以启动Container
- 步骤2： 通过RPC函数ContainerManagementProtocol#getContainerStatuses向NM咨询各Container运行状态，必要时AM为任务重新申请资源
- 步骤3： Container运行完成够，通过RPC函数ContainerManagementProtocol#stopContainer释放资源

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/ApplicationMaster与NodeManager通信流程.png?raw=true)

参见： [AM2NMDemo.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop/src/yarn/design/client/AM2NMDemo.java)

#### 3.2 ApplicationMaster编程库
跟YarnClient一样，AM和RM、NM之间的交互部分也有一个通用的编程库
##### 3.2.1 AM-RM编程库
AM与RM的核心交互逻辑由：AMRMClientImpl和AMRMClientAsync实现
- AMRMClientImpl 阻塞式实现
- AMRMClientAsync 非阻塞式实现
    - AM触发一个操作后，ARRMClientAsync将它封装成事件放入事件队列后返回，而事件的处理由一个专门的线程地负责
    - 如果想实现自己的AM，需要实现AMRMClientAsync.CallbackHandler，提供有5个回调函数
        - public void onContainersCompleted(List<ContainerStatus> statuses);
        - public void onContainersAllocated(List<Container> containers);
        - public void onShutdownRequest();
        - public void onNodesUpdated(List<NodeReport> updatedNodes);
        - public void onError(Throwable e);
> 几个回调函数的调用时机及应用示例参见 [AM2RMBaseOnModuleAsync.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop/src/yarn/design/client/AM2RMBaseOnModuleAsync.java)
    
![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/AM-RM编程库.png?raw=true)

##### 3.2.2 AM-NM编程库
> AM需要与NM通信，而RM也需要跟NM通信以启动AM。因此这个地方编程库命名为 NMClient 而不是 AMNMClient

AM和NM的核心交互逻辑由：NMClientImpl和NMClientAsync实现，跟AM-RM的编程库一样，一个是同步一个是异步

NMClientAsyne.CallbackHandler类似的，也有6个回调函数。

参见: [AM2NMBaseOnModule.java](https://github.com/fancychuan/read-the-source/tree/master/hadoop/src/yarn/design/client/AM2NMBaseOnModule.java)

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/AM-NM编程库.png?raw=true)

### 4. YARN应用实例
YARN自带了两个Application示例程序：DistributedShell和UNManagedAM
#### 4.1 DistributedShell
可以分布式运行shell命令的应用程序，使用语法如下图：

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/DistributedShell参数含义及使用示例.png?raw=true)

使用示例
```
bin/hadoop jar share/hadoop/yarn/hadoop-yarn-applications-distributedshell-*.jar \
org.apache.hadoop.yarn.applications.distributedshell.Client 
  --jar share/hadoop/yarn/hadoop-yarn-applications-distributedshell-*.jar \
  --shell_command ls \
  --container_memory 350 \
  --master_memory 350 \
  --priority 10
```
DistributedShell在源码中由三部分组成，分别为：
- 客户端：[Client.java](https:github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell/src/main/java/org/apache/hadoop/yarn/applications/distributedshell/Client.java)
    - 有三个构造函数：
        - public Client() throws Exception 
        - public Client(Configuration conf) throws Exception 使用自带的ApplicationMaster类
        - Client(String appMasterMainClass, Configuration conf) 可以指定使用的ApplicationMaster实现类
    - 启动客户端，并准备提交应用，比如AM所需的资源、执行的命令等
    ```
    # 组装后在AM中执行的shell命令
    java -Xmx 350m org.apache.hadoop.yarn.applications.distributedshell.ApplicationMaster \
    --container_menory 350 \
    --num_containers 10 \ 
    --priority 10 \
    --shell_command ls \
    1> $LOG_DIR/AppMaster.stdout \
    2> $LOG_DIR/AppMaster.stderr
    ```
    - 提交应用并监控是否运行完成
- AM实现：[ApplicationMaster.java](https:github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell/src/main/java/org/apache/hadoop/yarn/applications/distributedshell/ApplicationMaster.java)
    - 申请资源，并在资源没有达到或者任务失败时重新申请
    - 通过一个新的线程联系NM启动Container并执行命令
- 客户端和AM共用的常量：[DSConstans.java](https:github.com/fancychuan/read-the-source/tree/master/hadoop-2.2.0-src/hadoop-yarn-project/hadoop-yarn/hadoop-yarn-applications/hadoop-yarn-applications-distributedshell/src/main/java/org/apache/hadoop/yarn/applications/distributedshell/DSConstans.java)

#### 4. Unmanaged AM
AM需要占用一个Container，而该Container的位置不确定，给调试带来麻烦。为此引入Unmanaged AM，这种AM不需要RM启动和销毁，而是在客户端启动一个新的进程运行

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/UnmannedAMLauncher使用方法及参数含义.png?raw=true)



### 5. 源码阅读引导
- 通信协议：
- 编程库：
- YARN编程实例：