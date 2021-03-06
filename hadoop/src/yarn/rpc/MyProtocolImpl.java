package yarn.rpc;

import org.apache.hadoop.ipc.ProtocolSignature;

import java.io.IOException;

/**
 * 自定义RPC协议的实现
 */
public class MyProtocolImpl implements MyProtocol {
    @Override
    public String echo(String value) throws IOException {
        return value;
    }

    @Override
    public int add(int v1, int v2) throws IOException {
        return v1 + v1;
    }

    // 获取自定义的协议版本号
    @Override
    public long getProtocolVersion(String s, long l) throws IOException {
        return versionID;
    }

    // 获取协议签名
    @Override
    public ProtocolSignature getProtocolSignature(String s, long l, int i) throws IOException {
        return new ProtocolSignature(versionID, null);
    }
}
