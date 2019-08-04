package yarn.design.client;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.api.ApplicationClientProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationRequest;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class AnYarnApplicationClientDemo {
    private ApplicationClientProtocol rmClient; // 用于跟RM通信的客户端，也是一个RPC client

    public AnYarnApplicationClientDemo() throws IOException, YarnException {

        // 这里按道理需要跟ApplicationClientProtocol协议的版本号一致，但却似乎无法从ApplicationClientProtocol获取到版本号
        // todo：该如何从其实现获取？
        long clientVersion = 1L;
        InetSocketAddress rmAddress = new InetSocketAddress("localhost",6666);
        Configuration conf = new Configuration();
        String appName = "";
        HashMap<String, LocalResource> localResource = new HashMap<>();
        HashMap<String, String> env = new HashMap<>();
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
         * 2.1 创建将所有信息打包到一起的对象ApplicationSubmissionContext【数据结构在yarn_protos.proto中定义】
         *  主要包含的字段：
         *      application_id, application_name
         *      priority: 优先级
         *      queue： 所属队列
         *      user： 所属用户
         *      unmanaged： 是否由客户端自己启动ApplicationMaster
         *      cancel_tokens_when_complete： 程序运行完成是否取消Token，通常设置为true，除非需要将该应用程序的token共享给其他应用程序
         *      am_container_spec： 启动ApplicationMaster相关的信息，主要包含：
         *          user：
         *          resource： 启动AM所需的资源，当前支持CPU和内存两种
         *          localResource： AM所需的本地资源，通常是一些外部文件，比如字段等
         *          command： AM的启动命令，一般为shell
         *          environment： AM运行所需要的环境变量
         */
        ApplicationSubmissionContext appContext = Records.newRecord(ApplicationSubmissionContext.class);
        appContext.setApplicationId(applicationId);
        appContext.setApplicationName(appName);
        /**
         * 2.2 创建一个ContainerLaunchContext对象，也就是AM启动上下文并设置相关的属性
         */
        ContainerLaunchContext amContext = Records.newRecord(ContainerLaunchContext.class);  // 构建一个ApplicationMaster启动上下文对象
        amContext.setLocalResources(localResource); // AM启动所需的本地资源
        amContext.setEnvironment(env); // AM启动所需要的环境变量
        /**
         * 2.3 把AM启动上下文打包到ApplicationSubmissionContext中
         */
        appContext.setAMContainerSpec(amContext);
        /**
         * 2.4 创建一个提交应用的请求，并提交
         */
        SubmitApplicationRequest submitRequest = Records.newRecord(SubmitApplicationRequest.class);
        submitRequest.setApplicationSubmissionContext(appContext);
        rmClient.submitApplication(submitRequest); // 把应用程序提交到RM上
    }

    public static void main(String[] args) {

    }
}
