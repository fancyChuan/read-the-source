package yarn.state;

import org.apache.hadoop.yarn.event.EventHandler;
import org.apache.hadoop.yarn.state.InvalidStateTransitonException;
import org.apache.hadoop.yarn.state.SingleArcTransition;
import org.apache.hadoop.yarn.state.StateMachine;
import org.apache.hadoop.yarn.state.StateMachineFactory;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class JobStateMachine implements EventHandler<JobEvent> {
    private final String jobID;
    private EventHandler eventHandler;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    private final ReentrantReadWriteLock.ReadLock readLock;

    // 定义状态机工厂  OPERAND=JobStateMachine STATE=JobStateInternal EVENTTYPE=JobEventType EVENT=JobEvent
    protected static final StateMachineFactory<JobStateMachine, JobStateInternal, JobEventType, JobEvent>
            stateMachineFactory = new StateMachineFactory<JobStateMachine, JobStateInternal, JobEventType, JobEvent>(JobStateInternal.NEW)
                .addTransition(JobStateInternal.NEW, JobStateInternal.INITED, JobEventType.JOB_INIT, new InitTransition())
                .addTransition(JobStateInternal.INITED, JobStateInternal.SETUP, JobEventType.JOB_START, new StartTransition())
                .addTransition(JobStateInternal.SETUP, JobStateInternal.RUNNING, JobEventType.JOB_SETUP_COMPLETED, new SetupCompletedTransition())
                .addTransition(JobStateInternal.RUNNING, JobStateInternal.SUCCEEDED, JobEventType.JOB_COMPLETED, new JobTasksCompletedTransition())
                .installTopology();

    // 定义状态机
    private final StateMachine<JobStateInternal, JobEventType, JobEvent> stateMachine;

    // 构造器
    public JobStateMachine(String jobID, EventHandler eventHandler) {
        this.jobID = jobID;
        this.eventHandler = eventHandler;
        // 初始化锁
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
        // 创建状态机
        stateMachine = stateMachineFactory.make(this);
    }

    @Override
    public void handle(JobEvent event) {
        try {
            writeLock.lock();
            JobStateInternal oldState = getInternalState();
            try {
                getStateMachine().doTransition(event.getType(), event);
            } catch (InvalidStateTransitonException e) {
                System.out.println("can't handle this event at current state");
            }
            if (oldState != getInternalState()) {
                System.out.println("Job Transitioned from " + oldState + " to " + getInternalState());
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 下面是跟getter相关的操作
     */
    public String getJobID() {
        return jobID;
    }
    protected StateMachine<JobStateInternal, JobEventType, JobEvent> getStateMachine() {
        return stateMachine;
    }
    public JobStateInternal getInternalState() {
        readLock.lock();
        try {
            return getStateMachine().getCurrentState();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 以下是相关内部类：定义的是状态转移hook
     */
    public static class InitTransition implements SingleArcTransition<JobStateMachine, JobEvent> {
        @Override
        public void transition(JobStateMachine jobStateMachine, JobEvent jobEvent) {
            System.out.println("Receiving event " + jobEvent);
            jobStateMachine.eventHandler.handle(new JobEvent(jobStateMachine.getJobID(), JobEventType.JOB_START));
        }
    }
    public static class StartTransition implements SingleArcTransition<JobStateMachine, JobEvent> {
        @Override
        public void transition(JobStateMachine jobStateMachine, JobEvent jobEvent) {
            System.out.println("Receiving event " + jobEvent);
            jobStateMachine.eventHandler.handle(new JobEvent(jobStateMachine.getJobID(), JobEventType.JOB_SETUP_COMPLETED));
        }
    }
    public static class SetupCompletedTransition implements SingleArcTransition<JobStateMachine, JobEvent> {
        @Override
        public void transition(JobStateMachine jobStateMachine, JobEvent jobEvent) {
            System.out.println("Receiving event " + jobEvent);
            jobStateMachine.eventHandler.handle(new JobEvent(jobStateMachine.getJobID(), JobEventType.JOB_COMPLETED));
        }
    }
    public static class JobTasksCompletedTransition implements SingleArcTransition<JobStateMachine, JobEvent> {
        @Override
        public void transition(JobStateMachine jobStateMachine, JobEvent jobEvent) {
            System.out.println("Receiving event " + jobEvent);
            jobStateMachine.eventHandler.handle(new JobEvent(jobStateMachine.getJobID(), JobEventType.JOB_KILL));
        }
    }
}
