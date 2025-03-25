package dynamic.thread.pool.sdk.registry.zookeeper;

import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.sdk.registry.RegistryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 使用 Zookeeper作为注册中心
 * 将线程池配置存储到 Zookeeper中，实现服务注册
 */
@Slf4j
public class ZookeeperRegistry implements RegistryService {

    private final CuratorFramework curatorFramework;

    private static final String BASE_CONFIG_PATH = "/dynamic/thread/pool/config";

    private static final String BASE_STATUS_PATH = "/dynamic/thread/pool/status";

    public ZookeeperRegistry(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolConfigEntities) throws Exception {
        for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
            String appName = threadPoolConfigEntity.getAppName();
            String threadPoolName = threadPoolConfigEntity.getThreadPoolName();
            if (appName == null || threadPoolName == null) return;
            String configPath = BASE_CONFIG_PATH.concat("/").concat(appName);
            String statusPath = BASE_STATUS_PATH.concat("/").concat(appName);
            if (curatorFramework.checkExists().forPath(configPath) == null) {
                log.info("新增配置节点:{}", configPath);
                log.info("新建状态节点:{}", statusPath);
                curatorFramework.create().creatingParentsIfNeeded().forPath(configPath);
                curatorFramework.create().creatingParentsIfNeeded().forPath(statusPath);
            }
            String configChildren = configPath.concat("/").concat(threadPoolName);
            String statusChildren = statusPath.concat("/").concat(threadPoolName);
            if (curatorFramework.checkExists().forPath(configChildren) == null) {
                log.info("新建配置节点:{}的子节点:{}", configPath, threadPoolName);
                log.info("新建状态节点:{}的子节点:{}", statusPath, threadPoolName);
                curatorFramework.create().creatingParentsIfNeeded().forPath(configChildren);
                curatorFramework.create().creatingParentsIfNeeded().forPath(statusChildren);

                ThreadPoolConfigEntity config = ThreadPoolConfigEntity.builder()
                        .appName(threadPoolConfigEntity.getAppName())
                        .threadPoolName(threadPoolConfigEntity.getThreadPoolName())
                        .corePoolSize(threadPoolConfigEntity.getCorePoolSize())
                        .maximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize())
                        .build();
                String configData = JSON.toJSONString(config);
                log.info("配置节点写入:{} {}", configChildren, configData);
                curatorFramework.setData().forPath(configChildren, configData.getBytes(StandardCharsets.UTF_8));
            }

            ThreadPoolConfigEntity status = ThreadPoolConfigEntity.builder()
                    .poolSize(threadPoolConfigEntity.getPoolSize())
                    .remainingCapacity(threadPoolConfigEntity.getRemainingCapacity())
                    .queueType(threadPoolConfigEntity.getQueueType())
                    .queueSize(threadPoolConfigEntity.getQueueSize())
                    .activeCount(threadPoolConfigEntity.getActiveCount())
                    .build();
            String statusData = JSON.toJSONString(status);
            log.info("状态节点写入:{} {}", statusChildren, status);
            curatorFramework.setData().forPath(statusChildren, statusData.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        log.info("------");
    }
}
