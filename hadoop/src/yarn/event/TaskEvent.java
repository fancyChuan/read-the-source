package yarn.event;

import org.apache.hadoop.yarn.event.AbstractEvent;

/**
 * 1. 定义Task事件
 */
public class TaskEvent extends AbstractEvent<TaskEventType> {
    private String taskID;


    public TaskEvent(String taskID, TaskEventType taskEventType) {
        super(taskEventType);
        this.taskID = taskID;
    }

    public String getTaskID() {
        return taskID;
    }
}