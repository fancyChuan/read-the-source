package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.*;
import org.apache.hadoop.yarn.api.records.ApplicationAccessType;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.impl.pb.TokenPBImpl;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AM2NMDemo {
    ContainerManagementProtocol cm; // ContainerManagement对象

    public AM2NMDemo(Container container) throws IOException {
        /**
         * 1.1 从申请到的Container中获取资源相关信息，包括即将要在NodeManager上启动的地址信息，并联系NM再把本地启动一个代理客户端
         */
        String cmIpPortStr = container.getNodeId().getHost() + ":" + container.getNodeId().getPort();
        InetSocketAddress cmAddress = NetUtils.createSocketAddr(cmIpPortStr);
        System.out.println("Connecting to ContainerManager at " + cmIpPortStr);
        // 启动代理客户端
        this.cm = (ContainerManagementProtocol) RPC.getProxy(ContainerManagementProtocol.class, 1L, cmAddress, new Configuration());
    }

    /**
     * 步骤1：与NM通信启动Container
     */
    public void first() throws IOException {
        /**
         * 1.2 把Container的执行环境等相关信息封装到ContainerLaunchContext中。属性有：
         *      localResources: 所需的本地资源，比如字典文件、jar包等，已k/v格式存储
         *      tokens: Container执行所需的各种token
         *      service_data: 附属服务所需的数据，以k/v存储
         *      environment: 所需的环境变量，以k/v存储
         *      command： Container执行命令，需要是一条shell命令
         *      application_ACLs: 应用程序访问控制表，以k/v格式保存
         */
        ContainerLaunchContext ctx = Records.newRecord(ContainerLaunchContext.class);
        // ctx.setLocalResources();
        // ctx.setTokens();
        // ctx.setServiceData();
        // ctx.setEnvironment();
        // ctx.setCommands();
        Map<ApplicationAccessType, String> acls = new HashMap<>();
        acls.put(ApplicationAccessType.VIEW_APP, "user1 group1"); // 授予用户user1和用户组group1查看权限
        acls.put(ApplicationAccessType.MODIFY_APP, "user1"); // 授予user1修改权限
        ctx.setApplicationACLs(acls);
        /**
         * 1.3 创建一个启动Container的请求实例StartContainerRequest，有两个字段：
         *      container_launch_context: 1.2中封装了信息的ContainerLaunchContext对象
         *      container_token： Container启动时的安全token
         */
        StartContainerRequest request = Records.newRecord(StartContainerRequest.class);
        request.setContainerLaunchContext(ctx);
        // startContainerRequest.setContainer(container); // todo:有setContainer()这个函数吗？
        request.setContainerToken(new TokenPBImpl());
        /**
         * 1.4 通过客户端联系NM启动Container，得到一个StartContainersResponse类型的返回值，包含的字段有：
         *      serivices_meta_data: 附属服务返回的元数据信息
         *      succeeded_requests: 成功运行的Container列表
         *      failed_requests: 运行失败的Container列表
         */
        try {
            // 因为是可以启动多个Container的，那么这里就要求传一个数组
            ArrayList<StartContainerRequest> startContainers = new ArrayList<>();
            startContainers.add(request);
            // 开始通知NM启动容器
            StartContainersResponse response = cm.startContainers(
                    StartContainersRequest.newInstance(startContainers));
            response.getAllServicesMetaData();
            response.getSuccessfullyStartedContainers();
            response.getFailedRequests();
        } catch (YarnException e) {
            e.printStackTrace();
        }
    }

    /**
     * 步骤2：向NM咨询各Container运行状态
     */
    public void second() throws IOException, YarnException {
        GetContainerStatusesRequest getStatusRequest = Records.newRecord(GetContainerStatusesRequest.class);
        GetContainerStatusesResponse statusesResponse = cm.getContainerStatuses(getStatusRequest);
        statusesResponse.getContainerStatuses();
        statusesResponse.getFailedRequests();
    }

    /**
     * 步骤3： Container运行完成后释放资源
     */
    public void thrid() throws IOException, YarnException {
        StopContainersRequest stopRequest = Records.newRecord(StopContainersRequest.class);
        // stopRequest.setContainerIds();
        StopContainersResponse stopResponse = cm.stopContainers(stopRequest);
        stopResponse.getFailedRequests();
        stopResponse.getSuccessfullyStoppedContainers();
    }
}
