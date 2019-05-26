## YARN基础库

YARN的基础库主要有：
- Protocol Buffers
- Apache Avro
- RPC库
- 服务库和事件库
- 状态机库


#### 3.3.1 RPC通信模型
RPC通常采用客户机/服务器模型，两个相互协议的通信模型实现请求-应答协议，有同步方式和异步方式两种

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC同步模式与异步模式对比.png?raw=true)

一个典型的RPC框架主要包含几部分：
- 通信模块
- Stub程序：可以理解为代理程序
- 调度程序：接受来自通信模块的请求信息，根据标识选择一个Stub程序处理
- 客户程序/服务过程

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC通用架构.png?raw=true)

#### 3.3.3 总体架构
- 序列化层
- 函数调用层
- 网络传输层：基于TCP/IP的Socket机制
- 服务器端处理框架：Hadoop RPC采用了基于Reactor设计模型的事件驱动I/O模型

![image](https://github.com/fancyChuan/read-the-source/blob/master/hadoop/img/RPC整体架构.png?raw=true)