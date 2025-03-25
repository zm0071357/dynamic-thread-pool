package dynamic.thread.pool.sdk.registry.redis;

import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.sdk.registry.RegistryService;
import lombok.extern.flogger.Flogger;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.*;

import static dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO.*;

/**
 * 使用 Redis作为注册中心
 * 将线程池配置存储到 Redis中，实现服务注册
 */
@Slf4j
public class RedisRegistry implements RegistryService {

    // Redis 客户端
    private final RedissonClient redissonClient;

    public RedisRegistry(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolConfigEntities) {
        // 获取线程池列表
        List<ThreadPoolConfigEntity> list = redissonClient.getList(THREAD_POOL_CONFIG_LIST_KEY.getKey());
        // 首次缓存 将全部线程池缓存
        if (list.isEmpty()) {
            log.info("首次缓存数据");
            list.addAll(threadPoolConfigEntities);
            return;
        }
        Map<String, ThreadPoolConfigEntity> map = new HashMap<>();
        for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
            map.put(threadPoolConfigEntity.getThreadPoolName(), threadPoolConfigEntity);
        }
        for (int i = 0; i < list.size(); i++) {
            ThreadPoolConfigEntity cacheThreadPoolConfigEntity = list.get(i);
            ThreadPoolConfigEntity newThreadPoolConfigEntity = map.get(cacheThreadPoolConfigEntity.getThreadPoolName());
            // 缓存存在 - 更新
            // 缓存不存在 - 写入
            if (newThreadPoolConfigEntity != null) {
                list.set(i, newThreadPoolConfigEntity);
            } else {
                list.add(threadPoolConfigEntities.get(i));
            }
        }
    }

    @Override
    public void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        // 拼接缓存key
        String cacheKey = THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + threadPoolConfigEntity.getAppName() + "_" + threadPoolConfigEntity.getThreadPoolName();
        // 根据key获取对应的线程池配置
        RBucket<ThreadPoolConfigEntity> bucket = redissonClient.getBucket(cacheKey);
        // 设置有效期为30天
        bucket.set(threadPoolConfigEntity, Duration.ofDays(30));
    }
}
