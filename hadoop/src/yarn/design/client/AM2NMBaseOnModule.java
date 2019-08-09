package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync.CallbackHandler;
import org.apache.hadoop.yarn.client.api.async.impl.NMClientAsyncImpl;
import org.apache.hadoop.yarn.util.Records;

import java.nio.ByteBuffer;
import java.util.Map;

public class AM2NMBaseOnModule {
    public static void main(Container container, String[] args) {
        NMClientAsyncImpl nmClientAsync = new NMClientAsyncImpl(new MyNMCallbackHandler());
        nmClientAsync.init(new Configuration());
        nmClientAsync.start(); // 启动与NM通信的客户端

        ContainerLaunchContext ctx = Records.newRecord(ContainerLaunchContext.class);
        nmClientAsync.startContainerAsync(container, ctx);  // 启动Container
        nmClientAsync.getContainerStatusAsync(container.getId(), container.getNodeId());
        nmClientAsync.stopContainerAsync(container.getId(), container.getNodeId());
        nmClientAsync.stop();
    }
}


class MyNMCallbackHandler implements CallbackHandler {

    /**
     * 收到启动Container请求时被调用
     */
    @Override
    public void onContainerStarted(ContainerId containerId, Map<String, ByteBuffer> map) {

    }

    /**
     * 在NM应答（对之前发送的查询状态指令的应答）Container当前状态时被调用
     */
    @Override
    public void onContainerStatusReceived(ContainerId containerId, ContainerStatus containerStatus) {

    }

    /**
     * 在NM应答（对之前发送的停止Container指令的应答）Container已停止时被调用
     */
    @Override
    public void onContainerStopped(ContainerId containerId) {

    }

    /**
     * 当NM启动Container过程中抛出异常时被调用
     */
    @Override
    public void onStartContainerError(ContainerId containerId, Throwable throwable) {

    }

    /**
     * 当NM查询Container运行状态过程中抛出异常时被调用
     */
    @Override
    public void onGetContainerStatusError(ContainerId containerId, Throwable throwable) {

    }

    /**
     * 当NM停止Container过程中抛出异常时被调用
     */
    @Override
    public void onStopContainerError(ContainerId containerId, Throwable throwable) {

    }
}
