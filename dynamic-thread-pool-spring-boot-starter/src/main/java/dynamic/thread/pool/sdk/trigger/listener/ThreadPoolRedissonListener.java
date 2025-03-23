package dynamic.thread.pool.sdk.trigger.listener;

import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.sdk.registry.RegistryService;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 动态线程池变更监听器
 * 实现MessageListener<ThreadPoolConfigEntity> 接口的监听器
 * 负责接收ThreadPoolConfigEntity类型的消息
 */
public class ThreadPoolRedissonListener implements MessageListener<ThreadPoolConfigEntity> {
    private Logger logger = LoggerFactory.getLogger(ThreadPoolRedissonListener.class);

    private final DynamicThreadPoolService dynamicThreadPoolService;

    private final RegistryService registryService;

    public ThreadPoolRedissonListener(DynamicThreadPoolService dynamicThreadPoolService, RegistryService registryService) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registryService = registryService;
    }

    @Override
    public void onMessage(CharSequence charSequence, ThreadPoolConfigEntity threadPoolConfigEntity) {
        logger.info("调整线程池配置，线程池名称：{} 核心线程数：{} 最大线程数：{}", threadPoolConfigEntity.getThreadPoolName(), threadPoolConfigEntity.getCorePoolSize(), threadPoolConfigEntity.getMaximumPoolSize());
        // 更新
        dynamicThreadPoolService.updateThreadPoolConfig(threadPoolConfigEntity);
        logger.info("调整线程池配置，线程池名称：{} 更新完成", threadPoolConfigEntity.getThreadPoolName());
        // 将更新后的线程池列表上报到注册中心
        List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
        try {
            registryService.reportThreadPool(threadPoolConfigEntities);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 将该线程池的配置信息上报到注册中心
        ThreadPoolConfigEntity currentThreadPoolConfigEntity = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolConfigEntity.getThreadPoolName());
        registryService.reportThreadPoolConfigParameter(currentThreadPoolConfigEntity);
        logger.info("上报线程池配置：{}", JSON.toJSONString(currentThreadPoolConfigEntity));
    }
}

