package dynamic.thread.pool.sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 动态线程池配置
 */
@ConfigurationProperties(prefix = "dynamic.thread.pool.config", ignoreInvalidFields = true)
@Data
public class DynamicThreadPoolAutoProperties {
    /** 状态；open = 开启、close 关闭 */
    private boolean enable;
    /** redis host */
    private String host;
    /** redis port */
    private int port;
    /** 账密 */
    private String password;
    /** 设置连接池的⼤⼩，默认为64 */
    private int poolSize = 64;
    /** 设置连接池的最⼩空闲连接数，默认为10 */
    private int minIdleSize = 10;
    /** 设置连接的最⼤空闲时间（单位：毫秒），超过该时间的空闲连接将被关闭，默认为1000
     0 */
    private int idleTimeout = 10000;
    /** 设置连接超时时间（单位：毫秒），默认为10000 */
    private int connectTimeout = 10000;
    /** 设置连接重试次数，默认为3 */
    private int retryAttempts = 3;
    /** 设置连接重试的间隔时间（单位：毫秒），默认为1000 */
    private int retryInterval = 1000;
    /** 设置定期检查连接是否可⽤的时间间隔（单位：毫秒），默认为0，表示不进⾏定期检查
     */
    private int pingInterval = 0;
    /** 设置是否保持⻓连接，默认为true */
    private boolean keepAlive = true;
}
