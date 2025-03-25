package dynamic.thread.pool.sdk.trigger.job;

import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.sdk.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * 上报任务
 */
public class ThreadPoolDataReportJob {

    private Logger logger = LoggerFactory.getLogger(ThreadPoolDataReportJob.class);

    private final DynamicThreadPoolService dynamicThreadPoolService;

    private final RegistryService registryService;

    public ThreadPoolDataReportJob(DynamicThreadPoolService dynamicThreadPoolService, RegistryService registryService) {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registryService = registryService;
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void execReportThreadPoolList() throws Exception {
        logger.info("获取线程池配置列表并上报到注册中心");
        // 获取线程池配置列表
        List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
        // 将列表上报到注册中心
        registryService.reportThreadPool(threadPoolConfigEntities);
        logger.info("上报线程池信息：{}", JSON.toJSONString(threadPoolConfigEntities));
        // 将每个线程池的配置信息上报到注册中心
        for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
            registryService.reportThreadPoolConfigParameter(threadPoolConfigEntity);
            logger.info("上报线程池配置：{}", JSON.toJSONString(threadPoolConfigEntity));
        }
    }

}
