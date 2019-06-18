package yarn.rpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MyRPCClient {
    public static void main(String[] args) throws IOException {
        ClientProtocol proxy = (ClientProtocol) RPC.getProxy(
                ClientProtocol.class,
                ClientProtocol.versionID,
                new InetSocketAddress("localhost",6666),
                new Configuration());

        int result = proxy.add(5, 6);
        String xxx = proxy.echo("xxx");
        System.out.println(result);
        System.out.println(xxx);
    }
}
