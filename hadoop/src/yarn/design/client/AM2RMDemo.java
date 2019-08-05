package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.api.ApplicationMasterProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateRequest;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterRequest;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ResourceBlacklistRequest;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.server.utils.BuilderUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class AM2RMDemo {

    public void doSomething() throws IOException, YarnException, InterruptedException {
        long clientVersion = 1L;
        ApplicationMasterProtocol rmClient = RPC.getProxy(ApplicationMasterProtocol.class, clientVersion, new InetSocketAddress("lcoalhost", 666), new Configuration());
        /**
         * 步骤1 AM通过RPC函数向RM注册
         *  （1）创建Protocol buffers消息RegisterApplicationMasterRequest
         *  （2）打包注册信息，需要包括：
         *      - host： AM本次启动所在的节点host
         *      - rpc_port： 本次启动对外的RPC端口号
         *      - tracking_url： 对外提供的追踪web url，客户端可以通过这个url查询应用程序执行状态
         *  （3）成功注册后，会得到一个RegisterApplicationMasterResponse对象，有以下信息：
         *      - maximumcapability： 最大可申请的单个Container占用的资源
         *      - client_to_am_token_master_key： ClientToAMToken master key
         *      - application_ACLs： 应用程序访问控制列表。
         */
        RegisterApplicationMasterRequest registerApplicationMasterRequest = Records.newRecord(RegisterApplicationMasterRequest.class);
        // 在多线程环境下，需要考虑同步
        synchronized (this) {
            // registerApplicationMasterRequest.setApplicationAttemptId(appAttemptId); // TODO: 没有setApplicationAttemptId这个方法，该如何实现？
        }
        registerApplicationMasterRequest.setHost("localhost"); // 设置所在的host
        registerApplicationMasterRequest.setRpcPort(666); // 设置对外的host端口
        registerApplicationMasterRequest.setTrackingUrl(""); // 设置tracking url

        RegisterApplicationMasterResponse response = rmClient.registerApplicationMaster(registerApplicationMasterRequest);
        response.getMaximumResourceCapability();// 最大可申请的单个Container资源
        response.getApplicationACLs(); // 获得访问控制列表
        response.getClientToAMTokenMasterKey(); // ClientToAMToken master key
        /**
         * 步骤2： 申请资源
         *  2.1 AM将需要的资源封装到AllocateRequest对象中，主要包含以下几个字段：
         *      ask： AM请求的资源列表，每个资源请求用ResourceRequest表示
         *          * ResourceRequest包含以下字段：
         *              1> priority： 资源优先级
         *              2> resource_name： 期望资源所在的节点/机架，如果是*，表示任何节点上的资源均可
         *              3> capability： 所需要的资源量，目前支持CPU和内存两种资源
         *              4> num_container: 需要满足以上条件的资源数目
         *              5> relax_locality: 是否松弛本地性，即是否在没有满足本地性资源时自动选择机架本地性资源或者其他资源，默认是true
         *      release： AM释放的container列表，释放的场景有： 任务完成、资源无法使用自动释放资源、主动放弃分配的Container
         *      response_id： 应答id，每次通信，值+1
         *      progress： 应用执行进度
         *      blacklist_request： 请求加入/移除很名单的节点列表，有两个字段： blacklist_additions、blacklist_removals
         */
        AllocateRequest allocateRequest;
        int responseID = 1;
        float appProgress = 0;
        ResourceBlacklistRequest blacklistRequest = ResourceBlacklistRequest.newInstance(new ArrayList<>(), new ArrayList<>());
        while (true) { // 维持与RM之间的周期性心跳，TODO：这里需要考虑如何实现
            synchronized (this) {
                ArrayList<ResourceRequest> askList = new ArrayList<>();
                ArrayList<ContainerId> releaseList = new ArrayList<>();
                allocateRequest = AllocateRequest.newInstance(responseID, appProgress, askList, releaseList, blacklistRequest);
            }
            /**
             * 2.2 向RM申请资源，同时领取新分配的资源
             *  AllocateResponse包含以下字段：
             *      a_m_command: AM需要执行的命令，目前有两种取值：
             *          1> AM_RESYNC（重启）：RM重启或者应用程序信息出现不一致时
             *          2> AM_SHUTDOWN（关闭）：当节点处于黑名单中时
             *      response_id: 应答id，每次通信，值+1
             *      allocated_containers: 分配给该应用的Container列表。通常AM收到一个container之后会在这个容器中运行一个任务
             *      completed_container_statuses: 运行完成的Container状态列表，容器运行的状态有：成功、失败、killed
             *      limit: 集群可用资源总量，getAvailableResources()获取
             *      updated_nodes: 当前集群中所有节点运行状态列表
             *      num_cluster_nodes: 当前集群可用节点总数
             *      preempt: 资源抢占信息。RM要抢占某个应用的资源时，会提前发送一个资源列表让AM主动释放这些资源。如果AM一段时间内未释放，就强制回收。
             *               主要包含的信息：
             *                  strictContract： 必须释放的Container列表
             *                  contract： 包含资源总量和Container列表两类信息。AM可释放这些Container占用的资源，或者释放任意几个占用资源总量达到指定资源量的Container
             *      nm_tokens: NodeManager Token
             *
             *  亮点：周期性的心跳机制，让RM能够动态管理资源（多给资源或者抢占资源）！
             *
             *  另外，YARN采取覆盖式资源申请方式，即AM每次发出的资源请求会覆盖掉之前在同一节点且优先级相同的资源请求。
             *  所以这就要求同一节点上相同优先级的资源请求只能存在一种，否则前面申请的资源就会被覆盖掉
             */
            // 注意：即使AM不需要任何资源，也需要周期性调用这个函数以维持和RM之间的心跳。维持心跳另一个功能是：周期性询问RM是否存在分配给应用程序的资源
            AllocateResponse allocateResponse = rmClient.allocate(allocateRequest);
            // 根据RM的应答信息设计接下来的逻辑，比如将资源分配任务
            allocateResponse.getAMCommand(); // 获得命令
            responseID = allocateRequest.getResponseId();
            allocateResponse.getAllocatedContainers();
            allocateResponse.getCompletedContainersStatuses();
            allocateResponse.getAvailableResources(); // 集群可用资源总量
            allocateResponse.getUpdatedNodes();
            allocateResponse.getNumClusterNodes();
            allocateResponse.getPreemptionMessage(); // 资源抢占信息
            allocateResponse.getNMTokens();
            // ...
            Thread.sleep(1000);
        }
    }
}
