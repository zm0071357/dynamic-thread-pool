package dynamic.thread.pool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "zookeeper.sdk.config", ignoreInvalidFields = true)
public class ZookeeperClientConfigProperties {

    /**
     * 连接地址
     */
    private String connectString;

    /**
     * 基本重试时间
     */
    private int baseSleepTimeMs;

    /**
     * 最大重试次数
     */
    private int maxRetries;

    /**
     * 会话超时时间
     */
    private int sessionTimeoutMs;

    /**
     * 连接超时时间
     */
    private int connectionTimeoutMs;

}
