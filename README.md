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


