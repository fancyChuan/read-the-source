package yarn.event;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.event.AsyncDispatcher;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.event.EventHandler;

/**
 * 简化版的MRAppMaster
 */
@SuppressWarnings("unchecked")
public class SimpleMRAppMaster extends CompositeService {
    private Dispatcher dispatcher;      // 中央异步调度器
    private String jobID;
    private int taskNumber;             // 作业包含的任务数
    private String[] taskIDs;           // 作业内部所包含的所有任务

    public SimpleMRAppMaster(String name, String jobID, int taskNumber) {
        super(name);
        this.jobID = jobID;
        this.taskNumber = taskNumber;
        taskIDs = new String[taskNumber];
        for (int i = 0; i < taskNumber; i++) {
            taskIDs[i] = jobID + "_task_" + i;
        }
    }

    public void serviceInit(final Configuration conf) throws Exception {
        dispatcher = new AsyncDispatcher(); // 定义一个中央异步调度器
        // 注册Job和Task事件调度器
        dispatcher.register(JobEventType.class, new JobEventDispatcher());
        dispatcher.register(TaskEventType.class, new TaskEventDispatcher());
        // 添加注册该调度器，后面为其他事件服务
        addService((Service) dispatcher);
        super.serviceInit(conf); // 因为是继承了抽象父类AbstractService，所以这里也需要调用父类的初始服务方法
    }

    @Override
    protected void serviceStart() throws Exception {
        super.serviceStart(); // AbstractService的这个方法是protected的，后面使用的时候无法执行启动服务，所以这个地方需要封装一下
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }


    /**
     * Job事件调度器
     */
    private class JobEventDispatcher implements EventHandler<JobEvent> {
        @Override
        public void handle(JobEvent event) {
            if (event.getType() == JobEventType.JOB_KILL) {
                System.out.println("Receive JOB_KILL enent, killing all the tasks");
                for (int i = 0; i < taskNumber; i++) {
                    // TODO： 这个地方也是，为什么能准确找到对应的EventHandler??
                    dispatcher.getEventHandler().handle(new TaskEvent(taskIDs[i], TaskEventType.T_KILL));
                }
            } else if (event.getType() == JobEventType.JOB_INIT) {
                System.out.println("Receive JOB_INIT event, scheduling tasks");
                for (int i = 0; i < taskNumber; i++) {
                    dispatcher.getEventHandler().handle(new TaskEvent(taskIDs[i], TaskEventType.T_SCHEDULE));
                }
            }
        }
    }

    /**
     * Task事件调度器
     */
    private class TaskEventDispatcher implements EventHandler<TaskEvent> {
        @Override
        public void handle(TaskEvent event) {
            if (event.getType() == TaskEventType.T_KILL) {
                System.out.println("Receive T_KILL event of task " + event.getTaskID());
            } else if (event.getType() == TaskEventType.T_SCHEDULE) {
                System.out.println("Receive T_SCHEDULE event of task " + event.getTaskID());
            }
        }
    }
}
