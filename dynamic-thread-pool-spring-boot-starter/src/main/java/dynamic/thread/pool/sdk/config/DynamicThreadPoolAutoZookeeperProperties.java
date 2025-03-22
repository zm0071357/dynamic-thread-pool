package dynamic.thread.pool.sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 动态线程池配置 - Zookeeper
 */
@ConfigurationProperties(prefix = "dynamic.thread.pool.config.zookeeper", ignoreInvalidFields = true)
@Data
public class DynamicThreadPoolAutoZookeeperProperties {
    /** 状态；open = 开启、close 关闭 */
    private boolean enable;

    private String connectString;

    private int baseSleepTimeMs = 1000;

    private int maxRetries = 3;

    private int sessionTimeoutMs = 18000;

    private int connectionTimeoutMs = 30000;
}
