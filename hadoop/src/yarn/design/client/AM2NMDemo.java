package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.api.records.Container;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AM2NMDemo {
    ContainerManagementProtocol cm; // ContainerManagement对象

    /**
     * 步骤1：与NM通信启动Container
     */
    public AM2NMDemo(Container container) throws IOException {
        /**
         * 1.1 从申请到的Container中获取资源相关信息，包括即将要在NodeManager上启动的地址信息
         */
        String cmIpPortStr = container.getNodeId().getHost() + ":" + container.getNodeId().getPort();
        InetSocketAddress cmAddress = NetUtils.createSocketAddr(cmIpPortStr);
        System.out.println("Connecting to ContainerManager at " + cmIpPortStr);
        this.cm = (ContainerManagementProtocol) RPC.getProxy(ContainerManagementProtocol.class, 1L, cmAddress, new Configuration());
    }
}
