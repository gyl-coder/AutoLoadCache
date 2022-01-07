package com.jarvis.cache.starter.properties;

import com.jarvis.cache.common.annotation.Cache;
import com.jarvis.cache.common.config.AutoLoadConfig;
import com.jarvis.cache.common.config.JedisCacheManagerConfig;

import javax.annotation.PostConstruct;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.env.Environment;

/** 自定义配置项 */
@Data
@ConfigurationProperties(prefix = AutoloadCacheProperties.PREFIX)
public class AutoloadCacheProperties {

    public static final String PREFIX = "autoload.cache";

    /** 全局配置项 {@link NestedConfigurationProperty 用于配置类中比较复杂的配置型，例如嵌套类型} */
    @NestedConfigurationProperty private AutoLoadConfig config = new AutoLoadConfig();

    private JedisCacheManagerConfig jedis = new JedisCacheManagerConfig();

    @Autowired private Environment env;

    private boolean namespaceEnable = true;

    private boolean proxyTargetClass = true;

    private boolean enable = true;

    /** {@link Cache 注解是否生效, 默认值为true } */
    private boolean enableReadAndWrite = true;

    /**
     * {@link com.jarvis.cache.common.annotation.CacheDelete CacheDeleteTransactional 注解是否生效,
     * 默认值为true}
     */
    private boolean enableDelete = true;

    /** @Cache 注解AOP执行顺序 */
    private int cacheOrder = Integer.MAX_VALUE;

    /** @DeleteCache 注解AOP执行顺序 */
    private int deleteCacheOrder = Integer.MAX_VALUE;

    /** @DeleteCacheTransactionalAspect 注解AOP执行顺序 */
    private int deleteCacheTransactionalOrder = 0;

    private String adminUserName = "admin";

    private String adminPassword = "admin";

    @PostConstruct
    public void init() {
        if (namespaceEnable && null != env) {
            String namespace = config.getNamespace();

            if (null == namespace || namespace.trim().length() == 0) {
                String applicationName = env.getProperty("spring.application.name");
                if (null != applicationName && applicationName.trim().length() > 0) {
                    config.setNamespace(applicationName);
                }
            }
        }
    }
}
