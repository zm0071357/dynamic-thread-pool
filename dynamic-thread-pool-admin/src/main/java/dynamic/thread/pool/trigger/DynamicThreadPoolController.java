package dynamic.thread.pool.trigger;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.types.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.redisson.api.RList;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/dynamic/thread/pool")
public class DynamicThreadPoolController {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CuratorFramework curatorFramework;

    @Value("${dynamic.thread.pool.config.redis.enabled}")
    private boolean redisIsEnabled;

    @Value("${dynamic.thread.pool.config.zookeeper.enabled}")
    private boolean zookeeperIsEnabled;

    @Value("${feishu.config.web_hook}")
    private String webHook;

    private final String BASE_CONFIG_PATH = "/dynamic/thread/pool/config";

    private final String BASE_STATUS_PATH = "/dynamic/thread/pool/status";

    /**
     * 查询线程池列表
     * curl --request GET \
     * --url 'http://localhost:8089/api/v1/dynamic/thread/pool/query_thread_pool_list'
     */
    @GetMapping("/query_thread_pool_list")
    public Response<List<ThreadPoolConfigEntity>> queryThreadPoolList() {
        List<ThreadPoolConfigEntity> res = new ArrayList<>();
        try {
            if (redisIsEnabled) {
                log.info("redis配置中心，查询线程池数据");
                RList<ThreadPoolConfigEntity> cacheList = redissonClient.getList("THREAD_POOL_CONFIG_LIST_KEY");
                res = cacheList.readAll();
            } else if (zookeeperIsEnabled) {
                log.info("zookeeper配置中心，查询线程池数据");
                // 查询配置节点
                if (curatorFramework.checkExists().forPath(BASE_CONFIG_PATH) != null) {
                    List<String> appNameList = curatorFramework.getChildren().forPath(BASE_CONFIG_PATH);
                    for (String appName : appNameList) {
                        String appConfigPath = BASE_CONFIG_PATH.concat("/").concat(appName);
                        if (curatorFramework.checkExists().forPath(appConfigPath) != null) {
                            List<String> threadPoolNameList = curatorFramework.getChildren().forPath(appConfigPath);
                            for (String threadPoolName : threadPoolNameList) {
                                // 读取配置节点数据
                                String configPath = appConfigPath.concat("/").concat(threadPoolName);
                                String configJson = new String(curatorFramework.getData().forPath(configPath), StandardCharsets.UTF_8);
                                ThreadPoolConfigEntity configEntity = JSON.parseObject(configJson, ThreadPoolConfigEntity.class);

                                // 读取状态节点数据
                                String statusPath = BASE_STATUS_PATH.concat("/").concat(appName).concat("/").concat(threadPoolName);
                                ThreadPoolConfigEntity statusEntity = new ThreadPoolConfigEntity();
                                if (curatorFramework.checkExists().forPath(statusPath) != null) {
                                    String statusJson = new String(curatorFramework.getData().forPath(statusPath), StandardCharsets.UTF_8);
                                    statusEntity = JSON.parseObject(statusJson, ThreadPoolConfigEntity.class);
                                }

                                // 合并数据
                                configEntity.setActiveCount(statusEntity.getActiveCount());
                                configEntity.setQueueSize(statusEntity.getQueueSize());
                                configEntity.setPoolSize(statusEntity.getPoolSize());
                                configEntity.setQueueType(statusEntity.getQueueType());
                                configEntity.setRemainingCapacity(statusEntity.getRemainingCapacity());
                                res.add(configEntity);
                            }
                        }
                    }
                }
            }
            return Response.<List<ThreadPoolConfigEntity>>builder()
                    .code(Response.Code.SUCCESS.getCode())
                    .info(Response.Code.SUCCESS.getInfo())
                    .data(res)
                    .build();
        } catch (Exception e) {
            log.error("查询线程池数据异常", e);
            return Response.<List<ThreadPoolConfigEntity>>builder()
                    .code(Response.Code.UN_ERROR.getCode())
                    .info(Response.Code.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 根据应用名和线程池名称查询对应线程池配置
     * curl --request GET \
     * --url 'http://localhost:8089/dynamic/thread/pool/query_thread_pool_config?appName=dynamic-thread-pool-test-app&threadPoolName=threadPoolExecutor01'
     */
    @GetMapping("/query_thread_pool_config")
    public Response<ThreadPoolConfigEntity> queryThreadPoolConfig(@RequestParam String appName, @RequestParam String threadPoolName) {
        ThreadPoolConfigEntity threadPoolConfigEntity = new ThreadPoolConfigEntity();
        try {
            if (redisIsEnabled) {
                log.info("redis配置中心，查询appName:{}，threadPoolName:{} 配置", appName, threadPoolName);
                String cacheKey = "THREAD_POOL_CONFIG_PARAMETER_LIST_KEY" + "_" + appName + "_" + threadPoolName;
                threadPoolConfigEntity = redissonClient.<ThreadPoolConfigEntity>getBucket(cacheKey).get();
            } if (zookeeperIsEnabled) {
                // 读取配置节点数据
                String configPath = BASE_CONFIG_PATH.concat("/").concat(appName).concat("/").concat(threadPoolName);
                if (curatorFramework.checkExists().forPath(configPath) != null) {
                    String configJson = new String(curatorFramework.getData().forPath(configPath), StandardCharsets.UTF_8);
                    threadPoolConfigEntity = JSON.parseObject(configJson, ThreadPoolConfigEntity.class);
                }

                // 读取状态节点数据
                String statusPath = BASE_STATUS_PATH.concat("/").concat(appName).concat("/").concat(threadPoolName);
                if (curatorFramework.checkExists().forPath(statusPath) != null) {
                    String statusJson = new String(curatorFramework.getData().forPath(statusPath), StandardCharsets.UTF_8);
                    ThreadPoolConfigEntity statusEntity = JSON.parseObject(statusJson, ThreadPoolConfigEntity.class);
                    // 合并状态字段
                    threadPoolConfigEntity.setActiveCount(statusEntity.getActiveCount());
                    threadPoolConfigEntity.setQueueSize(statusEntity.getQueueSize());
                    threadPoolConfigEntity.setPoolSize(statusEntity.getPoolSize());
                    threadPoolConfigEntity.setQueueType(statusEntity.getQueueType());
                    threadPoolConfigEntity.setRemainingCapacity(statusEntity.getRemainingCapacity());
                }
            }
            log.info("appName:{}，threadPoolName:{} 配置:{}", appName, threadPoolName, JSON.toJSONString(threadPoolConfigEntity));
            return Response.<ThreadPoolConfigEntity>builder()
                    .code(Response.Code.SUCCESS.getCode())
                    .info(Response.Code.SUCCESS.getInfo())
                    .data(threadPoolConfigEntity)
                    .build();
        } catch (Exception e) {
            log.error("查询线程池配置异常", e);
            return Response.<ThreadPoolConfigEntity>builder()
                    .code(Response.Code.UN_ERROR.getCode())
                    .info(Response.Code.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 修改线程池配置 - 核心线程数和最大线程数
     * curl --request POST \
     * --url http://localhost:8089/dynamic/thread/pool/update_thread_pool_config \
     * --header 'content-type: application/json' \
     * --data '{
     * "appName":"dynamic-thread-pool-test-app",
     * "threadPoolName": "threadPoolExecutor01",
     * "corePoolSize": 1,
     * "maximumPoolSize": 10
     * }'
     */
    @PostMapping("/update_thread_pool_config")
    public Response<Boolean> updateThreadPoolConfig(@RequestBody ThreadPoolConfigEntity request) {
        try {
            log.info("修改线程池配置开始 {} {} {}", request.getAppName(), request.getThreadPoolName(), JSON.toJSONString(request));
            if (redisIsEnabled) {
                RTopic topic = redissonClient.getTopic("DYNAMIC_THREAD_POOL_REDIS_TOPIC" + "_" + request.getAppName());
                topic.publish(request);
            } else if (zookeeperIsEnabled) {
                String path = BASE_CONFIG_PATH.concat("/").concat(request.getAppName()).concat("/").concat(request.getThreadPoolName());
                String data = JSON.toJSONString(request);
                curatorFramework.setData().forPath(path, data.getBytes(StandardCharsets.UTF_8));
            }
            log.info("修改线程池配置完成 {} {}", request.getAppName(), request.getThreadPoolName());
            // 发送消息到飞书
            String msg = "appName：".concat(request.getAppName()).concat("，")
                    .concat("threadPoolName：").concat(request.getThreadPoolName()).concat("，")
                    .concat("核心参数发生变更：")
                    .concat("corePoolSize：").concat(String.valueOf(request.getCorePoolSize())).concat("，")
                    .concat("maximumPoolSize：").concat(String.valueOf(request.getMaximumPoolSize())).concat("。");
            Map<String,Object> json=new HashMap();
            Map<String,Object> text=new HashMap();
            json.put("msg_type", "text");
            text.put("text", "线程池核心参数变更通知：" + msg);
            json.put("content", text);
            String result = HttpRequest.post(webHook).body(JSON.toJSONString(json), "application/json;charset=UTF-8").execute().body();
            log.info("发送通知:{}，请求结果:{}", msg, result);
            return Response.<Boolean>builder()
                    .code(Response.Code.SUCCESS.getCode())
                    .info(Response.Code.SUCCESS.getInfo())
                    .data(true)
                    .build();
        } catch (Exception e) {
            log.error("修改线程池配置异常 {}", JSON.toJSONString(request), e);
            return Response.<Boolean>builder()
                    .code(Response.Code.UN_ERROR.getCode())
                    .info(Response.Code.UN_ERROR.getInfo())
                    .data(false)
                    .build();
        }
    }
}

