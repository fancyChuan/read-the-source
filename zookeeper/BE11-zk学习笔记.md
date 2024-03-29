
### day01
zk是一个分布式的协调服务，提供了一个为了解决一些分布式问题而需要进行一些状态存储的数据模型系统
- 从自身特性上看，是可靠的（各节点的数据状态一致）。哪怕是在挂了不超过半数的机器，还能照样提供服务
- 从对外提供的服务上看，外部框架和应用需要利用zk可靠的特性来满足分布式计算场景下的稳定可靠要求

总的来说，zk在运行机制上主要有3种： 选举、读、写
> TODO：怎么理解和记忆选举的过程


那么，zk的各个角色在选举的时候、读的时候、写的时候分别承担什么职责？ zk有3个核心组件：
- leader
- follower
- observer


Leader中有3种服务
- 给follower和Observer提供同步服务（BIO）
- 给客户端提供读写请求服务（NIO）
- 选举服务（BIO）
> 这3个服务都是线程，都在QPM这个JVM中


#### znode数据模型

每个节点就是一条数据

每个server都拥有完整数据（通过ZAB协议保持各个节点的状态一致）

节点的代码抽象叫DataNode，逻辑概念叫ZNode

每个节点都有一个全局唯一的路径

关于znode的理解
- znode的约束（最大出差数据是1M，最好不超过1KB），深度没有约束（一般也不会弄得特别深）
  - 同步的压力
  - 存储的压力（内存有一份，磁盘有一份）
- znode的分类
> TODO：序列编号的作用和含义？
- 小知识
  - 临时节点跟会话绑定
    - 经典用法：hbase master以及hdfs

严格有序，串行执行

#### watcher
监听只会响应一次。

如果要循环监听：那监听之后再重新注册一次

#### 应用

**1.发布/订阅**

**2.命名服务**
唯一ID生成器

**3.集群管理**
- 选举：主节点管理
- 机器的上线和下线：从节点管理

**4.分布式锁**
锁服务：独占锁、共享锁、时序锁

**5.分布式队列管理**
- 同步队列/分布式屏障/分布式栅栏
- 先进先出队列（相当于时序锁）

**6.负载均衡**

**7.配置管理**
TODO：需要具体理解

### day02
zk3.6.3 性能和并发的能力得到极大提升


jute

recipes：样例

#### 网络通信

#### 源码阅读大纲
- zk服务节点启动
```
1、集群启动脚本分析：zkServer.sh start
2、集群启动的启动类的代码执行分析：QuorumPeerMain.main()
3、冷启动数据恢复，从磁盘恢复数据到内存：zkDatabase.loadDatabase()
4、选举：startLeaderElection() + QuorumPeer.lookForLeader()
5、同步：follower.followLeader() + observer.observerLeader()
```
- zk集群正常接收客户端的读写处理
- 额外知识
  - session管理
  - watcher管理和相应

flink的参数配置代码是做的最好的

