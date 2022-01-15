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

可视化框架
- [davinci](./davinci)