package yarn.rpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;

public class MyRPCServer {
    public static void main(String[] args) throws IOException {
        RPC.Server server = new RPC.Builder(new Configuration()).setProtocol(ClientProtocol.class)
                .setInstance(new ClientProtocolImpl())
                .setBindAddress("localhost")
                .setPort(6666)
                .setNumHandlers(5)
                .build();

        server.start();
    }
}
