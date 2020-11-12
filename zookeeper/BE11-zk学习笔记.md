
### day01

各个角色在选举的时候、读的时候、写的时候分别承担什么职责


Leader中有3种服务
- 给follower和Observer提供同步服务（BIO）
- 给客户端提供读写请求服务（NIO）
- 选举服务（BIO）
> 这3个服务都是线程，都在QPM这个JVM中

zk核心组件的理解
- leader
- follower
- observer
- 客户端


#### znode数据模型

每个节点就是一条数据

每个server都拥有完整数据

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
    - 