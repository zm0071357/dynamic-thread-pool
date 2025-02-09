package dynamic.thread.pool.sdk.domain.impl;

import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

public class DynamicThreadPoolServiceImpl implements DynamicThreadPoolService {

    private final Logger logger = LoggerFactory.getLogger(DynamicThreadPoolServiceImpl.class);
    private final String applicationName;
    private final Map<String, ThreadPoolExecutor> threadPoolExecutorMap;
    public DynamicThreadPoolServiceImpl(String applicationName, Map<String, ThreadPoolExecutor> threadPoolExecutorMap) {
        this.applicationName = applicationName;
        this.threadPoolExecutorMap = threadPoolExecutorMap;
    }

    @Override
    public List<ThreadPoolConfigEntity> queryThreadPoolList() {
        // 线程池名称集合
        Set<String> threadPoolBeanNames = threadPoolExecutorMap.keySet();
        // 线程池列表
        List<ThreadPoolConfigEntity> threadPoolVOS = new ArrayList<>(threadPoolBeanNames.size());
        // 遍历列表
        for (String threadPoolName : threadPoolBeanNames) {
            // 根据线程名获取线程池信息
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolName);
            // 构建线程池实体对象
            ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(applicationName, threadPoolName);
            // 配置数据
            threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
            threadPoolConfigVO.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
            threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
            threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
            threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue().getClass().getSimpleName());
            threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue().size());
            threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue().remainingCapacity());
            // 加入列表
            threadPoolVOS.add(threadPoolConfigVO);
        }
        return threadPoolVOS;

    }

    @Override
    public ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName) {
        // 根据线程池名称获取线程池
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolName);
        // 线程池为空 没有该线程池则返回一个新线程池实体
        if (null == threadPoolExecutor) {
            return new ThreadPoolConfigEntity(applicationName, threadPoolName);
        }
        // 配置数据
        ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(applicationName, threadPoolName);
        threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        threadPoolConfigVO.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
        threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
        threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
        threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue().getClass().getSimpleName());
        threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue().size());
        threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue().remainingCapacity());

        if (logger.isDebugEnabled()) {
            logger.info("动态线程池，配置查询 应用名:{} 线程池名称:{} 池化配置:{}", applicationName, threadPoolName, JSON.toJSONString(threadPoolConfigVO));
        }

        return threadPoolConfigVO;

    }

    @Override
    public void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity) {
        if (null == threadPoolConfigEntity || !applicationName.equals(threadPoolConfigEntity.getAppName())) {
            return;
        }
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolConfigEntity.getThreadPoolName());
        if (null == threadPoolExecutor) {
            return;
        }
        // 设置参数 调整核心线程数和最大线程数
        threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
        threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
    }
}
