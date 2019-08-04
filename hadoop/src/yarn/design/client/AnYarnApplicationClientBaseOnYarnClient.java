package yarn.design.client;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.exceptions.YarnException;

import java.io.IOException;

/**
 * 基于YarnClient类实现一个客户端
 */
public class AnYarnApplicationClientBaseOnYarnClient {
    private YarnClient client;

    public void doSomething() throws IOException, YarnException {
        client = YarnClient.createYarnClient();
        Configuration conf = new Configuration();
        client.init(conf);
        // 启动YarnClient
        client.start();
        // 创建一个application
        YarnClientApplication app = client.createApplication();
        // 构造一个ApplicationSubmissionContext用于打包作业信息
        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        ApplicationId appId = appContext.getApplicationId();
        appContext.setApplicationName("app name");
        // 把应用提交到RM上
        client.submitApplication(appContext);
    }
}
