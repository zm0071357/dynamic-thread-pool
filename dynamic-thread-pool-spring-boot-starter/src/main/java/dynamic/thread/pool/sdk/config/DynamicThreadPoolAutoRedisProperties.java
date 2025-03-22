package dynamic.thread.pool.sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 动态线程池配置 - Redis
 */
@ConfigurationProperties(prefix = "dynamic.thread.pool.config.redis", ignoreInvalidFields = true)
@Data
public class DynamicThreadPoolAutoRedisProperties {
    /** 状态；open = 开启、close 关闭 */
    private boolean enable;
    /** host */
    private String host;
    /** port */
    private int port;
    /** 账密 */
    private String password;
    /** 设置连接池的大小，默认为64 */
    private int poolSize = 64;
    /** 设置连接池的最小空闲连接数，默认为10 */
    private int minIdleSize = 10;
    /** 设置连接的最大空闲时间（单位：毫秒），超过该时间的空闲连接将被关闭，默认为1000
     0 */
    private int idleTimeout = 10000;
    /** 设置连接超时时间（单位：毫秒），默认为10000 */
    private int connectTimeout = 10000;
    /** 设置连接重试次数，默认为3 */
    private int retryAttempts = 3;
    /** 设置连接重试的间隔时间（单位：毫秒），默认为1000 */
    private int retryInterval = 1000;
    /** 设置定期检查连接是否可用的时间间隔（单位：毫秒），默认为0，表示不进⾏定期检查
     */
    private int pingInterval = 0;
    /** 设置是否保持⻓连接，默认为true */
    private boolean keepAlive = true;
}
