package yarn.rpc;

import java.io.IOException;

/**
 * 自定义的RPC协议
 */
public interface ClientProtocol extends org.apache.hadoop.ipc.VersionedProtocol {

    public static final long versionID = 1L;   // 版本号，不同版本的RPC client 和 server 之间不能通信
    String echo(String value) throws IOException;
    int add(int v1, int v2) throws IOException;

}
