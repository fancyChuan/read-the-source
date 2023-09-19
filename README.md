# read-the-source
源码研读


大数据源码阅读的思路：
- 掌握其网络通信架构
    - hadoop：hadoopRPC
    - spark：akka -> netty
    - kafka: NIO
- 场景驱动的方式
    - 比如xx组件的启动流程

> 技巧：阅读类的注释


### Hadoop
- NameNode的启动过程
- DataNode的启动过程



程序员能力模型
- 需求分析
- 架构设计
- 架构选型
- 开发代码
- 运维部署
- 责任归属
> chatGPT新范式下能力模型
> - Prompt能力、判断决策能力、商业思维能力、一定的IT能力

大数据框架
- [hadoop](./hadoop)
  - 阅读环境搭建：[hadoop源码阅读环境搭建.md](hadoop/hadoop源码阅读环境搭建.md)
  - [YARN设计理念与基本架构](hadoop/YARN设计理念与基本架构.md)
  - [YARN应用程序设计方法](hadoop/YARN应用程序设计方法.md)
  - [YARN基础库](hadoop/YARN基础库.md)
  - [ResourceManager剖析](hadoop/ResourceManager剖析.md)
- [hive](./hive)


- [zookeeper](./zookeeper)
  - [zookeeper核心功能和工作机制](./zookeeper/zookeeper核心功能和工作机制.md)


- [Spark](./spark)
  - 

可视化框架
- [davinci](./davinci)