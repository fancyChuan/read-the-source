package yarn.event;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

@SuppressWarnings("unchecked")
public class SimpleMRAppMasterTest {

    public static void main(String[] args) throws Exception {
        String jobID = "job_20190616";
        SimpleMRAppMaster appMaster = new SimpleMRAppMaster("simple mrApMaster", jobID, 5);

        YarnConfiguration conf = new YarnConfiguration(new Configuration());
        appMaster.serviceInit(conf);    // 服务初始化
        appMaster.serviceStart();       // 启动服务，注意需要在SimpleMRAppMaster中封装一下

        // 接收事件并调度想要的处理器处理事件
        appMaster.getDispatcher().getEventHandler().handle(new JobEvent(jobID, JobEventType.JOB_KILL)); //TODO：为什么这个地方能够准确的找到对应的事件处理器？？
        appMaster.getDispatcher().getEventHandler().handle(new JobEvent(jobID, JobEventType.JOB_INIT));
    }

}
