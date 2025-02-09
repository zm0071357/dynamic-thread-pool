package dynamic.thread.pool.sdk.registry;

import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;

import java.util.List;
import java.util.Set;

/**
 * 注册中心接口
 */
public interface RegistryService {

    /**
     * 上报线程池列表
     * @param threadPoolConfigEntities
     */
    void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolConfigEntities);

    /**
     * 上报线程池配置参数
     * @param threadPoolConfigEntity
     */
    void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity);
}
