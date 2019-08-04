package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.api.ApplicationMasterProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterRequest;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AM2RMDemo {

    public void doSomething() throws IOException, YarnException {
        long clientVersion = 1L;
        ApplicationMasterProtocol rmClient = RPC.getProxy(ApplicationMasterProtocol.class, clientVersion, new InetSocketAddress("lcoalhost", 666), new Configuration());
        /**
         * 步骤1 AM通过RPC函数向RM注册
         *  （1）创建Protocol buffers消息RegisterApplicationMasterRequest
         *  （2）打包注册信息，需要包括：
         *      host
         *      rpc_port： 本次启动对外的RPC端口号
         *      tracking_url： 对外提供的追踪web url，客户端可以通过这个url查询应用程序执行状态
         *  （3）成功注册后，会得到一个RegisterApplicationMasterResponse对象，有以下信息：
         *      maximumcapability： 最大可申请的单个Container占用的资源
         *      client_to_am_token_master_key： ClientToAMToken master key
         *      application_ACLs： 应用程序访问控制列表。
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
    }
}
