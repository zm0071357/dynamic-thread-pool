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

    public ZookeeperRegistry(CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public void reportThreadPool(List<ThreadPoolConfigEntity> threadPoolConfigEntities) throws Exception {
        for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
            String appName = threadPoolConfigEntity.getAppName();
            String threadPoolName = threadPoolConfigEntity.getThreadPoolName();
            if (appName == null || threadPoolName == null) return;
            String path = BASE_CONFIG_PATH.concat("/").concat(appName);
            if (curatorFramework.checkExists().forPath(path) == null) {
                log.info("新建节点:{}", path);
                curatorFramework.create().creatingParentsIfNeeded().forPath(path);
            }
            String children = path.concat("/").concat(threadPoolName);
            if (curatorFramework.checkExists().forPath(children) == null) {
                log.info("新建节点:{}的子节点:{}", path, threadPoolName);
                curatorFramework.create().creatingParentsIfNeeded().forPath(children);
            }
            String data = JSON.toJSONString(threadPoolConfigEntity);
            curatorFramework.setData().forPath(children, data.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void reportThreadPoolConfigParameter(ThreadPoolConfigEntity threadPoolConfigEntity) {
        log.info("---");
    }
}
