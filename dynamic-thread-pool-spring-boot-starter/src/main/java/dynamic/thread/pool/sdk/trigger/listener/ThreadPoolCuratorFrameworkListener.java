package dynamic.thread.pool.sdk.trigger.listener;

import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.sdk.registry.RegistryService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.curator.framework.recipes.cache.CuratorCacheListener.Type.NODE_CHANGED;

/**
 * Zookeeper监听器
 * 只监听配置变化
 */
public class ThreadPoolCuratorFrameworkListener {

    private Logger logger = LoggerFactory.getLogger(ThreadPoolCuratorFrameworkListener.class);

    private final DynamicThreadPoolService dynamicThreadPoolService;

    private final RegistryService registryService;

    private final CuratorFramework client;

    private static final String BASE_CONFIG_PATH = "/dynamic/thread/pool/config";

    public ThreadPoolCuratorFrameworkListener(DynamicThreadPoolService dynamicThreadPoolService, RegistryService registryService, CuratorFramework client) throws Exception {
        this.dynamicThreadPoolService = dynamicThreadPoolService;
        this.registryService = registryService;
        this.client = client;

        if (null == client.checkExists().forPath(BASE_CONFIG_PATH)) {
            client.create().creatingParentsIfNeeded().forPath(BASE_CONFIG_PATH);
            logger.info("DCC 节点监听 base node {} 不存在，新建节点", BASE_CONFIG_PATH);
        }
        CuratorCache curatorCache = CuratorCache.build(client, BASE_CONFIG_PATH);
        curatorCache.start();
        // 添加节点变更监听
        logger.info("添加监听");
        curatorCache.listenable().addListener((type, oldData, data) -> {
            if (type == NODE_CHANGED) {
                handleNodeChanged(data.getPath(), data.getData());
            }
        });
    }

    private void handleNodeChanged(String path, byte[] dataBytes) {
        try {
            logger.info("监听到节点变更，路径：{}，数据：{}", path, new String(dataBytes));
            String relativePath = path.substring(BASE_CONFIG_PATH.length() + 1);
            String[] pathSegments = relativePath.split("/");
            if (pathSegments.length != 2) {
                logger.warn("Invalid zookeeper node path: {}", path);
                return;
            }
            // 解析配置
            ThreadPoolConfigEntity configEntity = JSON.parseObject(
                    new String(dataBytes),
                    ThreadPoolConfigEntity.class);
            // 更新线程池配置
            dynamicThreadPoolService.updateThreadPoolConfig(configEntity);
            logger.info("调整线程池配置，线程池名称：{} 核心线程数：{} 最大线程数：{}",
                    configEntity.getThreadPoolName(),
                    configEntity.getCorePoolSize(),
                    configEntity.getMaximumPoolSize());
            // 上报最新配置到注册中心
            reportUpdatedConfig(configEntity.getThreadPoolName());
        } catch (Exception e) {
            logger.error("处理Zookeeper节点变更失败", e);
        }
    }

    private void reportUpdatedConfig(String threadPoolName) {
        try {
            // 上报完整线程池列表
            registryService.reportThreadPool(dynamicThreadPoolService.queryThreadPoolList());
            // 上报当前线程池详细配置
            ThreadPoolConfigEntity currentConfig = dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolName);
            registryService.reportThreadPoolConfigParameter(currentConfig);
            logger.info("调整线程池配置，线程池名称：{} 更新完成", threadPoolName);
        } catch (Exception e) {
            logger.error("配置上报失败", e);
        }
    }

}
