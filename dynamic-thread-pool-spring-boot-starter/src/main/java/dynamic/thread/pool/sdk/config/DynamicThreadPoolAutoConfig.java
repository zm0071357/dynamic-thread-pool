package dynamic.thread.pool.sdk.config;

import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import dynamic.thread.pool.sdk.domain.impl.DynamicThreadPoolServiceImpl;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.sdk.registry.RegistryService;
import dynamic.thread.pool.sdk.registry.redis.RedisRegistry;
import dynamic.thread.pool.sdk.registry.zookeeper.ZookeeperRegistry;
import dynamic.thread.pool.sdk.trigger.job.ThreadPoolDataReportJob;
import dynamic.thread.pool.sdk.trigger.listener.ThreadPoolCuratorFrameworkListener;
import dynamic.thread.pool.sdk.trigger.listener.ThreadPoolRedissonListener;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO.*;


/**
 * 动态配置入口
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties({DynamicThreadPoolAutoRedisProperties.class, DynamicThreadPoolAutoZookeeperProperties.class})
public class DynamicThreadPoolAutoConfig {

    private final Logger logger = LoggerFactory.getLogger(DynamicThreadPoolAutoConfig.class);

    private final String BASE_CONFIG_PATH = "/dynamic/thread/pool/config";

    private String applicationName;

    @Value("${dynamic.thread.pool.config.redis.enabled}")
    private boolean redisIsEnabled;

    @Value("${dynamic.thread.pool.config.zookeeper.enabled}")
    private boolean zookeeperIsEnabled;

    /**
     * 创建 Redis客户端
     * @param properties
     * @return
     */
    @Bean("dynamicThreadRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoRedisProperties properties) {
        Config config = new Config();
        // 根据需要可以设定编解码器；https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
        config.setCodec(JsonJacksonCodec.INSTANCE);

        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive());

        RedissonClient redissonClient = Redisson.create(config);
        logger.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", properties.getHost(), properties.getPoolSize(), !redissonClient.isShutdown());

        return redissonClient;
    }

    /**
     * 创建 Redis注册中心，报告线程池状态
     * @param redissonClient
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "dynamic.thread.pool.config.redis.enabled", havingValue = "true", matchIfMissing = false)
    public RegistryService redisRegistry(RedissonClient redissonClient) {
        return new RedisRegistry(redissonClient);
    }

    /**
     * 创建 Redis主题，用于发布和订阅消息
     * @param redissonClient
     * @param threadPoolRedissonListener
     * @return
     */
    @Bean(name = "dynamicThreadPoolRedisTopic")
    @ConditionalOnProperty(name = "dynamic.thread.pool.config.redis.enabled", havingValue = "true", matchIfMissing = false)
    public RTopic threadPoolListener(RedissonClient redissonClient, ThreadPoolRedissonListener threadPoolRedissonListener) {
        // 根据应用名创建消息
        RTopic topic = redissonClient.getTopic(DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey() + "_" + applicationName);
        // 为消息添加监听器
        topic.addListener(ThreadPoolConfigEntity.class, threadPoolRedissonListener);
        return topic;
    }

    /**
     * 创建线程池数据报告任务，用于定期向注册中心报告线程池的状态
     * @param dynamicThreadPoolService
     * @param registryService
     * @return
     */
    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(DynamicThreadPoolService dynamicThreadPoolService, RegistryService registryService) {
        return new ThreadPoolDataReportJob(dynamicThreadPoolService, registryService);
    }

    /**
     * 创建线程池配置调整监听器，用于监听 Redis主题上的消息，当收到消息时，调整线程池的配置
     * @param dynamicThreadPoolService
     * @param registryService
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "dynamic.thread.pool.config.redis.enabled", havingValue = "true", matchIfMissing = false)
    public ThreadPoolRedissonListener threadPoolRedissonListener(DynamicThreadPoolService dynamicThreadPoolService, RegistryService registryService) {
        return new ThreadPoolRedissonListener(dynamicThreadPoolService, registryService);
    }

    /**
     * 创建 Zookeeper客户端
     * @param properties
     * @return
     */
    @Bean(name = "dynamicThreadZookeeperClient")
    public CuratorFramework createWithOptions(DynamicThreadPoolAutoZookeeperProperties properties) {
        ExponentialBackoffRetry backoffRetry = new ExponentialBackoffRetry(properties.getBaseSleepTimeMs(), properties.getMaxRetries());
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(properties.getConnectString())
                .retryPolicy(backoffRetry)
                .sessionTimeoutMs(properties.getSessionTimeoutMs())
                .connectionTimeoutMs(properties.getConnectionTimeoutMs())
                .build();
        client.start();
        return client;
    }

    /**
     * 创建 Zookeeper注册中心，报告线程池状态
     * @param client
     * @return
     */
    @Bean
    @ConditionalOnProperty(name = "dynamic.thread.pool.config.zookeeper.enabled", havingValue = "true", matchIfMissing = false)
    public RegistryService zookeeperRegistry(CuratorFramework client) {
        return new ZookeeperRegistry(client);
    }

    /**
     * 创建线程池配置调整监听器，用于监听Zookeeper节点的消息
     * @param dynamicThreadPoolService
     * @param registryService
     * @param client
     * @return
     * @throws Exception
     */
    @Bean
    @ConditionalOnProperty(name = "dynamic.thread.pool.config.zookeeper.enabled", havingValue = "true", matchIfMissing = false)
    public ThreadPoolCuratorFrameworkListener threadPoolCuratorFrameworkListener(DynamicThreadPoolService dynamicThreadPoolService, RegistryService registryService, CuratorFramework client) throws Exception {
        return new ThreadPoolCuratorFrameworkListener(dynamicThreadPoolService, registryService, client);
    }

    /**
     * 根据配置创建动态线程池服务，用于管理线程池
     * @param applicationContext
     * @param threadPoolExecutorMap
     * @param curatorFramework
     * @param redissonClient
     * @return
     * @throws Exception
     */
    @Bean("dynamicThreadPoolService")
    public DynamicThreadPoolServiceImpl dynamicThreadPoolService(ApplicationContext applicationContext,
                                                                           Map<String, ThreadPoolExecutor> threadPoolExecutorMap,
                                                                           @Autowired(required = false) CuratorFramework curatorFramework,
                                                                           @Autowired(required = false) RedissonClient redissonClient) throws Exception {
        // 通过配置信息获取应用名
        applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");
        if (StringUtils.isBlank(applicationName)) {
            applicationName = "default";
            logger.error("动态线程池启动提示：应用未配置spring.application.name");
        }
        logger.info("应用名：{}", applicationName);
        logger.info("动态线程池信息：{}", JSON.toJSONString(threadPoolExecutorMap.keySet()));
        if (redisIsEnabled) {
            logger.info("使用redis作为配置中心，获取缓存数据");
            // 获取缓存数据，设置本地线程池配置
            // 防止应用重启后使用配置文件的配置
            Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
            for (String threadPoolKey : threadPoolKeys) {
                ThreadPoolConfigEntity threadPoolConfigEntity = redissonClient.<ThreadPoolConfigEntity>getBucket(THREAD_POOL_CONFIG_PARAMETER_LIST_KEY.getKey() + "_" + applicationName + "_" + threadPoolKey).get();
                if (null == threadPoolConfigEntity) {
                    logger.info("未缓存数据:{} {}", applicationName, threadPoolKey);
                    continue;
                }
                ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
                threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
                threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
            }
        } else if (zookeeperIsEnabled) {
            logger.info("使用zookeeper作为配置中心，获取节点数据");
            // 获取Zookeeper的节点数据，设置本地线程池配置
            // 防止应用重启后使用配置文件的配置
            Set<String> threadPoolKeys = threadPoolExecutorMap.keySet();
            for (String threadPoolKey : threadPoolKeys) {
                String path = BASE_CONFIG_PATH.concat("/").concat(applicationName).concat("/").concat(threadPoolKey);
                if (curatorFramework.checkExists().forPath(path) != null) {
                    String jsonStr = new String(curatorFramework.getData().forPath(path), StandardCharsets.UTF_8);
                    ThreadPoolConfigEntity threadPoolConfigEntity = JSON.parseObject(jsonStr, ThreadPoolConfigEntity.class);
                    if (null == threadPoolConfigEntity) {
                        logger.info("无节点数据:{} {}", applicationName, threadPoolKey);
                        continue;
                    }
                    ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolKey);
                    threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
                    threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
                }
            }
        }
        return new DynamicThreadPoolServiceImpl(applicationName, threadPoolExecutorMap);
    }

}
