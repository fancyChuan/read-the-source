package yarn.rpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.Server;

import java.io.IOException;

public class MyRPCServer {
    public static void main(String[] args) throws IOException {
        RPC.Server server = new RPC.Builder(new Configuration()).setProtocol(MyProtocol.class) // 两种方式都可以，这个Server extends org.apache.hadoop.ipc.Server
        // Server server = new RPC.Builder(new Configuration()).setProtocol(MyProtocol.class)  // 这个Server直接就是org.apache.hadoop.ipc.Server
                .setInstance(new MyProtocolImpl())
                .setBindAddress("localhost")
                .setPort(6666)
                .setNumHandlers(5)
                .build();

        server.start();
    }
}
