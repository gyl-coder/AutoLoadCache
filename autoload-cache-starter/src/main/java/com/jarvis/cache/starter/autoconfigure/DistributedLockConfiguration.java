package com.jarvis.cache.starter.autoconfigure;

import com.jarvis.cache.lock.api.ILock;
import com.jarvis.cache.starter.redis.SpringRedisLock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * 对分布式锁进行一些默认配置<br>
 * 如果需要自定义，需要自行覆盖即可
 */
@Slf4j
@Configuration
@AutoConfigureAfter({AutoloadCacheManageConfiguration.class})
public class DistributedLockConfiguration {

    @Bean
    @ConditionalOnMissingBean({ILock.class})
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public ILock autoLoadCacheDistributedLock(RedisConnectionFactory connectionFactory) {
        if (null == connectionFactory) {
            return null;
        }

        SpringRedisLock lock = new SpringRedisLock(connectionFactory);
        if (log.isDebugEnabled()) {
            log.debug("ILock with SpringJedisLock auto-configured");
        }
        return lock;
    }
}
