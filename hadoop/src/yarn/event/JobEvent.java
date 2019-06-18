package yarn.event;

import org.apache.hadoop.yarn.event.AbstractEvent;

public class JobEvent extends AbstractEvent<JobEventType> {
    private String jobID;

    public JobEvent(String jobID, JobEventType type) {
        super(type);
        this.jobID = jobID;
    }

    public String getJobID() {
        return jobID;
    }
}
