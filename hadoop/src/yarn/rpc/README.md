
## hadoop RPC的简单应用

hadoop自己设计了一套RPC框架，我们可以基于这个框架很方便的实现分布式RPC应用。主要得益于hadoop暴露的接口以及相关的协议框架


示例文件说明：
- MyProtocol.java
    - 自定义协议接口，所有的协议都需要继承org.apache.hadoop.ipc.VersionedProtocol，它描述了协议的版本信息
    - 在这个自定义协议中，我们定义需要用到的所有方法，比如add()用于相加、echo()用于打印结果
```
// VersionedProtocol的内容，规定了协议要有的两个方法：获取版本号、获取协议签名
package org.apache.hadoop.ipc;

import java.io.IOException;

public interface VersionedProtocol {
    long getProtocolVersion(String var1, long var2) throws IOException;

    ProtocolSignature getProtocolSignature(String var1, long var2, int var4) throws IOException;
}
```    
- MyProtocolImpl.java
    - 自定义协议的实现类，主要实现MyProtocol协议中的自定义的方法以及实现VersionedProtocol所要求的两个方法
    - 这个类主要用于在server端设置服务实例
- MyRPCServer.java
    - 配置并启动自己的RPC server端
    - 主要使用了hadoopRPC对外暴露的RPC.Builder().build()
```
RPC.Server server = new RPC.Builder(new Configuration())
                .setProtocol(MyProtocol.class) // 设置自定义协议
                .setInstance(new MyProtocolImpl()) // 设置协议的实例
                .setBindAddress("localhost")
                .setPort(6666)
                .setNumHandlers(5)
                .build();

server.start();
```
另外，从源码可以看出，hadoopRPC对外暴露的接口并不能设置多个自定义协议，意味着多个协议需要多个server来提供服务
```
// org.apache.hadoop.ipc.RPC的子类Builder的实现源码
public static class Builder {
        private Class<?> protocol = null;
        private Object instance = null;
        private String bindAddress = "0.0.0.0";
        private int port = 0;
        private int numHandlers = 1;
        private int numReaders = -1;
        private int queueSizePerHandler = -1;
        private boolean verbose = false;
        private final Configuration conf;
        private SecretManager<? extends TokenIdentifier> secretManager = null;
        private String portRangeConfig = null;

        public Builder(Configuration conf) {
            this.conf = conf;
        }

        public RPC.Builder setProtocol(Class<?> protocol) {
            this.protocol = protocol;
            return this;
        }

        public RPC.Builder setInstance(Object instance) {
            this.instance = instance;
            return this;
        }

        public RPC.Builder setBindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
            return this;
        }

        public RPC.Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public RPC.Builder setNumHandlers(int numHandlers) {
            this.numHandlers = numHandlers;
            return this;
        }

        public RPC.Builder setnumReaders(int numReaders) {
            this.numReaders = numReaders;
            return this;
        }

        public RPC.Builder setQueueSizePerHandler(int queueSizePerHandler) {
            this.queueSizePerHandler = queueSizePerHandler;
            return this;
        }

        public RPC.Builder setVerbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public RPC.Builder setSecretManager(SecretManager<? extends TokenIdentifier> secretManager) {
            this.secretManager = secretManager;
            return this;
        }

        public RPC.Builder setPortRangeConfig(String portRangeConfig) {
            this.portRangeConfig = portRangeConfig;
            return this;
        }

        public RPC.Server build() throws IOException, HadoopIllegalArgumentException {
            if (this.conf == null) {
                throw new HadoopIllegalArgumentException("conf is not set");
            } else if (this.protocol == null) {
                throw new HadoopIllegalArgumentException("protocol is not set");
            } else if (this.instance == null) {
                throw new HadoopIllegalArgumentException("instance is not set");
            } else {
                return RPC.getProtocolEngine(this.protocol, this.conf).getServer(this.protocol, this.instance, this.bindAddress, this.port, this.numHandlers, this.numReaders, this.queueSizePerHandler, this.verbose, this.conf, this.secretManager, this.portRangeConfig);
            }
        }
    }
```

- MyRPCClient.java
    - 通过hadoopRPC对外暴露的RPC.getProxy()接口获取代理对象，当然，也可以使用waitForProxy()方法，TODO：这个的使用场景是什么？
    - 这个时候需要告知协议以及协议的版本，以和服务端匹配，另外还需要建立和服务端的socket连接


### 总结
总的来说，hadoopRPC框架可以让开发者更快速的实现某个RPC应用