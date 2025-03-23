package dynamic.thread.pool.config;

import com.alibaba.fastjson.JSON;
import dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import dynamic.thread.pool.type.DCCValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Zookeeper配置中心
 */
@Slf4j

public class DCCValueBeanFactory implements BeanPostProcessor {

    private static final String BASE_CONFIG_PATH = "/dynamic/thread/pool";

    private final CuratorFramework client;

    // 存储与 DCC 配置相关联的对象的映射
    private final Map<String, Object> dccObjGroup = new HashMap<>();

    public DCCValueBeanFactory(CuratorFramework client) throws Exception {
        this.client = client;
        // 检查节点是否存在 - 不存在，新建节点
        if (null == client.checkExists().forPath(BASE_CONFIG_PATH)) {
            client.create().creatingParentsIfNeeded().forPath(BASE_CONFIG_PATH);
            log.info("DCC 节点监听 base node {} 不存在，新建节点", BASE_CONFIG_PATH);
        }

        // 创建并启动 CuratorCache，用来监听 Zookeeper 上指定路径的变化
        CuratorCache curatorCache = CuratorCache.build(client, BASE_CONFIG_PATH);
        curatorCache.start();
        // 添加监听器
        // 修改 DCCValueBeanFactory.java 的监听器逻辑
        curatorCache.listenable().addListener((type, oldData, data) -> {
            switch (type) {
                case NODE_CHANGED:
                    String dccValuePath = data.getPath();
                    String matchedPrefix = null;
                    for (String prefix : dccObjGroup.keySet()) {
                        if (dccValuePath.startsWith(prefix + "/") || dccValuePath.equals(prefix)) {
                            if (matchedPrefix == null || prefix.length() > matchedPrefix.length()) {
                                matchedPrefix = prefix;
                            }
                        }
                    }
                    if (matchedPrefix == null) {
                        log.error("未找到与路径关联的Bean: {}", dccValuePath);
                        break;
                    }
                    Object objBean = dccObjGroup.get(matchedPrefix);
                    try {
                        // 获取 Map 字段
                        Field field = objBean.getClass().getDeclaredField("configs");
                        field.setAccessible(true);
                        Map<String, ThreadPoolConfigEntity> configMap = (Map<String, ThreadPoolConfigEntity>) field.get(objBean);
                        if (configMap == null) {
                            configMap = new ConcurrentHashMap<>();
                            field.set(objBean, configMap);
                        }
                        // 提取应用名（路径的最后两部分：appName/threadPoolName）
                        String[] parts = dccValuePath.substring(matchedPrefix.length() + 1).split("/");
                        if (parts.length < 2) {
                            log.error("路径格式错误: {}", dccValuePath);
                            break;
                        }
                        String appName = parts[0];
                        String threadPoolName = parts[1];
                        // 反序列化数据
                        ThreadPoolConfigEntity entity = JSON.parseObject(
                                new String(data.getData(), StandardCharsets.UTF_8),
                                ThreadPoolConfigEntity.class);
                        configMap.put(appName + "_" + threadPoolName, entity);
                        log.info("应用:{}, 线程池:{}, 配置:{}", appName, threadPoolName, entity);
                        field.setAccessible(false);
                    } catch (NoSuchFieldException e) {
                        log.error("字段 configs 不存在", e);
                    } catch (Exception e) {
                        throw new RuntimeException("更新配置失败", e);
                    }
                    break;
            }
        });
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取 bean 的类信息
        Class<?> beanClass = bean.getClass();
        // 获取所有字段
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            // 遍历字段，查找带有 DCCValue 注解的字段
            if (field.isAnnotationPresent(DCCValue.class)) {
                DCCValue dccValue = field.getAnnotation(DCCValue.class);
                try {
                    // 构造 Zookeeper 路径，如果该路径在 Zookeeper 中不存在，则创建该路径
                    String parentPath  = BASE_CONFIG_PATH.concat("/").concat(dccValue.value());
                    if (null == client.checkExists().forPath(parentPath)) {
                        client.create().creatingParentsIfNeeded().forPath(BASE_CONFIG_PATH.concat("/").concat(dccValue.value()));
                        log.info("DCC 节点监听 listener node {} 不存在，新建节点", BASE_CONFIG_PATH.concat("/").concat(dccValue.value()));
                    }
                    // 将 bean 与其对应的 Zookeeper 路径进行关联
                    dccObjGroup.put(parentPath, bean);
                    log.info("注册Bean: 路径={}, Bean类型={}", parentPath, bean.getClass().getName());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bean;
    }

}
