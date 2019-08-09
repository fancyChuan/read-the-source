package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.NodeReport;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.util.List;

/**
 * 利用yarn的编程库实现AM与RM的通信（非阻塞式）
 */
public class AM2RMBaseOnModuleAsync {
    public static void main(String[] args) throws IOException, YarnException {
        MyCallbackHandler allocListener = new MyCallbackHandler();
        // 构造一个AM与RM通信的客户端
        AMRMClientAsync<AMRMClient.ContainerRequest> asyncClient = AMRMClientAsync.createAMRMClientAsync(1000, allocListener);
        asyncClient.init(new Configuration());
        asyncClient.start();
        // 向RM注册自己
        String appMasterHostname = "";
        int appMasterRpcPort = 6666;
        String trackingUrl = "";
        RegisterApplicationMasterResponse response = asyncClient.registerApplicationMaster(appMasterHostname, appMasterRpcPort, trackingUrl);
        // 添加Container请求
        AMRMClient.ContainerRequest containerRequest = Records.newRecord(AMRMClient.ContainerRequest.class);
        asyncClient.addContainerRequest(containerRequest);
        // ... 等待程序运行结束
        FinalApplicationStatus status = FinalApplicationStatus.SUCCEEDED;
        String appMsg = "";
        asyncClient.unregisterApplicationMaster(status, appMsg, null); // 通知RM注销AM
        asyncClient.stop();

        // 其他方法
        // asyncClient.removeContainerRequest(); // 请求RM移除资源
        // asyncClient.releaseAssignedContainer(); // 请求RM释放资源
    }
}


class MyCallbackHandler implements AMRMClientAsync.CallbackHandler {

    /**
     * 被调用时机： RM为AM返回的心跳应答中包含完成的Container信息时
     * 注意： 如果心跳应答中同时包含完成的Container和新分配的Container，那么onContainersCompleted也在onContainersAllocated之前调用
     *       也就是说，onContainersCompleted优先级高于onContainersAllocated
     */
    @Override
    public void onContainersCompleted(List<ContainerStatus> list) {

    }

    /**
     * 被调用时机： RM为AM返回的心跳应答中包含新分配的Container信息时
     */
    @Override
    public void onContainersAllocated(List<Container> list) {

    }

    /**
     * 被调用时机： RM通知AM停止工作时 todo： 怎么通知的？也在心跳应答中？
     */
    @Override
    public void onShutdownRequest() {

    }

    /**
     * 被调用时机： RM管理的节点发生变化时（比如节点变得不健康，节点不可用）
     */
    @Override
    public void onNodesUpdated(List<NodeReport> list) {

    }

    /**
     * 被调用时机： 出现任何异常的时候
     */
    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public void onError(Throwable throwable) {

    }
}
