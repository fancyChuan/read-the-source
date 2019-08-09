package yarn.design.client;

import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.client.api.async.NMClientAsync.CallbackHandler;

import java.nio.ByteBuffer;
import java.util.Map;

public class AM2NMBaseOnModule {
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
