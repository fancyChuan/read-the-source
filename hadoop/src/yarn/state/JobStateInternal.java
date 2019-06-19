package yarn.state;

/**
 * 作业内部状态
 *
 * ！这个枚举类不能放到JobStateMachine中作为内部类，
 * 《hadoop技术内幕：深入解析YARN架构设计与实现原理》P74代码案例是把这个类写成内部类的，就会造成
 *  addTransition(JobStateInternal.NEW, JobStateInternal.INITED, JobEventType.JOB_INIT, new InitTransition()) 这个地方报错
 *
 *  TODO:为什么那样就会报错？？
 */
public enum JobStateInternal {
    NEW,
    SETUP,
    INITED,
    RUNNING,
    SUCCEEDED,
    KILLED,
}
