## zookeeper核心功能和工作机制.md

### 1. zk架构深入理解
ZooKeeper 是一个分布式协调服务，劝架者，仲裁机构。 多个节点如果出现了意见的不一致，需要一个中间机构来调停！
> 对于分布式计算来说，节点挂掉、读写冲突（不一致）这些问题需要有人来协调处理，还有别的场景问题吗？

ZooKeeper 就是一个小型的议会！当分布式系统中的多个节点，如果出现不一致，则把这个不一致的情况往 ZooKeeper 中写。ZooKeeper 会给你返回写成功的响
应。但凡你接收到成功的响应，意味着 ZooKeeper 帮你达成了一致！

ZooKeeper 以一个集群的方式对外提供协调服务，**集群内部的所有节点都保存了一份完整的数据**。其中一个主节点用来做集群管理提供写数据服务，其他的从节点
用来同步数据，提供读数据服务。这些从节点必须保持和主节点的数据状态一致。

![image](images/zk架构理解.png)

1. ZooKeeper 是对等架构，工作的时候，会举行选举，变成 Leader + Follower 架构
2. 在 ZooKeeper 中，没有沿用 Master/Slave(主备)概念，而是引入了 Leader、Follower、Observer 三种角色概念。通过 Leader 选举算法来选定一台服务器充
   当 Leader 节点，Leader 机器为客户端提供读写服务，其他角色，也就是 Follower 提供读服务，在提供读服务的 Follower 和 Observer 中，唯一区别就是
   Observer 不参与 Leader 选举过程，没有选举权和被选举权，因此 **Observer 的作用就是可以在不影响写性能情况下提高集群读性能**。
3. ZooKeeper 系统还有一种角色叫做 Observer，Observer 和 Follower 最大的区别就是 Observer 除了没有选举权 和 被选举权 以外，其他的和 Follower 完全
   一样
4. ZooKeeper 系统的 Leader 就相当于是一个全局唯一的分布式事务发起者，其他所有的 Follower 是事务参与者，拥有投票权
5. ZooKeeper 集群的最佳配置：比如 5,7,9,11,13 个这样的总结点数，Observer 若干，Observer 最好是在需要提升 ZooKeeper 集群服务能力的再进行扩展，而
   不是一开始就设置 Observer 角色！Follower 切记不宜太多！
6. Observer 的作用是 分担整个集群的读数据压力，同时又不增加分布式事务的执行压力，因为分布式事务的执行操作，只会在 Leader 和 Follower 中执行。
   Observer 只是保持跟 Leader 的同步，然后帮忙对外提供读数据服务，从而减轻 ZooKeeper 的数据读取服务压力。
7. ZooKeeper 中的所有数据，都在所有节点保存了一份完整的。所以只要所有节点保持状态一致的话，肯定是所有节点都可以单独对外提供读服务的。
8. ZooKeeper 集群中的所有节点的数据状态通过 ZAB 协议保持一致。ZAB 有两种工作模式：
   1. 崩溃恢复：集群没有 Leader 角色，内部在执行选举
   2. 原子广播：集群有 Leader 角色，Leader 主导分布式事务的执行，向所有的 Follower 节点，按照严格顺序广播事务
   3. 补充一点：实际上，ZAB 有四种工作模式，分别是：ELECTION，DISCOVERY，SYNCHRONIZATION，BROADCAST
9. ZooKeeper 系统中的 Leader 角色可以进行读，写操作，Follower 角色可以进行读操作执行，但是接收到写操作，会转发给 Leader 去执行。ZooKeeper 的所
   有事务操作在 Zookeeper 系统内部都是严格有序串行执行的。
10. ZooKeeper 系统虽然提供了存储系统（类文件系统：树形结构，每个节点不是文件也不是文件夹，是一个 znode ），但是这个存储，只是为自己实现某些功能
    做准备的，而不是提供出来，给用户存储大量数据的，所以，切忌往 ZooKeeper 中存储大量数据，甚至每个 Znode 节点的数据存储大小不能超过 1M
11. ZooKeeper 提供了 znode 节点的常规的增删改查操作，使用这些操作，可以模拟对应的业务操作，使用监听机制，可以让客户端立即感知这种变化。
12. ZooKeeper 集群和其他分布式集群最大的不同，在于 ZooKeeper 是不能进行线性扩展的。因为像 HDFS 的集群服务能力是和集群的节点个数成正比，但是
    ZooKeeper 系统的节点个数到一定程度之后，节点数越多，反而性能越差。
13. ZooKeeper 实现了 A可用性、P分区容错性、C中的写入强一致性，丧失的是C中的读取一致性。ZooKeeper 并不保证读取的是最新数据。如果客户端刚好链接
    到一个刚好没执行事务成功的节点，也就是说没和 Leader 保持一致的 Follower 节点的话，是有可能读取到非最新数据的。如果要保证读取到最新数据，请使
    用 sync 回调处理。这个机制的原理：是先让 Follower 保持和 Leader 一致，然后再返回结果给客户端。
14. Leader 的内部，有三个服务端！
  - 对 Follower 和 Observer 提供服务的
  - 为 Client 服务的
  - 给选举服务

### 2. ZNode数据模型


### 3. watcher监听机制

