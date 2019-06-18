
基于事件库、服务库搭建一个简化版的MRAppMaster

流程：
1. 定义事件类以及事件类型，如JobEvent和JobEventType
2. 定义一个事件处理器或者说事件调度器(如JobEventDispatcher)用于处理1中定义的事件和事件类型
3. 把2中定义的时间处理器注册到中央异步调度器中(如dispatcher.register(JobEventType.class, new JobEventDispatcher());)