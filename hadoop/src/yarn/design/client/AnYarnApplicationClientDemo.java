package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AnYarnApplicationClientDemo {
    private ApplicationClientProtocol rmClient; // 用于跟RM通信的客户端，也是一个RPC client

    public AnYarnApplicationClientDemo() throws IOException, YarnException {

        // 这里按道理需要跟ApplicationClientProtocol协议的版本号一致，但却似乎无法从ApplicationClientProtocol获取到版本号
        // todo：该如何从其实现获取？
        long clientVersion = 1L;
        InetSocketAddress rmAddress = new InetSocketAddress("localhost",6666);
        Configuration conf = new Configuration();
        /**
         * 步骤一： 获取appid
         * 1.1 通过yarn自带的RPC框架，指定协议后实例化出一个RPC client
         */
        this.rmClient = RPC.getProxy(ApplicationClientProtocol.class, clientVersion, rmAddress, conf);
        /**
         * 1.2 创建一个向RM申请appId的请求，这个请求需要是可序列化的
         *
         *  (1)我们经常用Records.newRecord()方法来构造一个可序列化对象
         *      具体采用的序列化工厂由参数：yarn.ipc.record.factory.class指定
         *      默认值是org.apache.hadoop.yarn.factories.impl.pb.RecordFactoryPBImpl
         *      即构造的是Protocol Buffers序列化对象
         *  (2)返回的是一个GetNewApplicationRequest的请求对象
         */
        GetNewApplicationRequest request = Records.newRecord(GetNewApplicationRequest.class);
        /**
         * 1.3 借助RPC客户端与RM通信，即调用ApplicationClientProtocol#getNewApplication方法
         *
         * 返回对象：GetNewApplicationResponse，主要包含两项信息：
         *      ApplicationId和最大可申请资源
         */
        GetNewApplicationResponse newApp = rmClient.getNewApplication(request);
        ApplicationId applicationId = newApp.getApplicationId(); // appid
        Resource maximumResourceCapability = newApp.getMaximumResourceCapability(); //最大可申请资源
        /**
         * 步骤二：提交app
         * 2.1
         */

        ApplicationSubmissionContext appContext = Records.newRecord(ApplicationSubmissionContext.class);


    }

    public static void main(String[] args) {

    }
}
