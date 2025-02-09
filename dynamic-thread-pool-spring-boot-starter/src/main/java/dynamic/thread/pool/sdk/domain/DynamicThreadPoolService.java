package dynamic.thread.pool.sdk.domain;

import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;
import java.util.Set;

public interface DynamicThreadPoolService {

    /**
     * 获取线程池集合
     * @return
     */
    List<ThreadPoolConfigEntity> queryThreadPoolList();

    /**
     * 根据线程池名称获取线程池配置信息
     * @param threadPoolName
     * @return
     */
    ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName);

    /**
     * 更新线程池配置信息
     * @param threadPoolConfigEntity
     */
    void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity);
}
