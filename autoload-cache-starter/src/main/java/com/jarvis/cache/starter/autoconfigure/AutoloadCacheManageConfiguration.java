package com.jarvis.cache.starter.autoconfigure;

import com.jarvis.cache.manager.api.ICacheManager;
import com.jarvis.cache.manager.impl.jedis.JedisClusterCacheManager;
import com.jarvis.cache.manager.impl.lettuce.LettuceRedisClusterCacheManager;
import com.jarvis.cache.manager.impl.redis.AbstractRedisCacheManager;
import com.jarvis.cache.script.api.AbstractScriptParser;
import com.jarvis.cache.script.ognl.OgnlParser;
import com.jarvis.cache.script.springel.SpringELParser;
import com.jarvis.cache.serializer.api.serializer.ISerializer;
import com.jarvis.cache.serializer.impl.hession.HessianSerializer;
import com.jarvis.cache.serializer.impl.jdk.JdkSerializer;
import com.jarvis.cache.serializer.impl.kryo.KryoSerializer;
import com.jarvis.cache.starter.properties.AutoloadCacheProperties;
import com.jarvis.cache.starter.redis.SpringRedisCacheManager;

import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClusterConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.util.ClassUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.lang.reflect.Field;

/**
 * 对autoload-cache进行一些默认配置<br>
 * 1. 配置表达式解析器<br>
 * 2. 配置序列化工具<br>
 * 如果需要自定义，需要自行覆盖即可
 */
@Slf4j
@Configuration
@ConditionalOnClass(name = "com.jarvis.cache.manager.api.ICacheManager")
@EnableConfigurationProperties(AutoloadCacheProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnProperty(value = "autoload.cache.enable", matchIfMissing = true)
public class AutoloadCacheManageConfiguration {

    // ClassUtils.isPresent 通过反射判断指定类是否存在
    private static final boolean hessianPresent =
            ClassUtils.isPresent(
                    "com.caucho.hessian.io.AbstractSerializerFactory",
                    AutoloadCacheManageConfiguration.class.getClassLoader());

    private static final boolean kryoPresent =
            ClassUtils.isPresent(
                    "com.esotericsoftware.kryo.Kryo",
                    AutoloadCacheManageConfiguration.class.getClassLoader());

    /**
     * 表达式解析器{@link AbstractScriptParser AbstractScriptParser} 注入规则：<br>
     * 如果导入了Ognl的jar包，优先 使用Ognl表达式：{@link OgnlParser OgnlParser}，否则使用{@link SpringELParser
     * SpringELParser}<br>
     *
     * @return todo 这个规则是否生效？？ 直接返回了 SpringELParser ，OgnlParser 从哪里返回？？？？
     */
    @Bean
    @ConditionalOnMissingBean(AbstractScriptParser.class)
    public AbstractScriptParser autoloadCacheScriptParser() {
        return new SpringELParser();
    }

    /**
     * * 序列化工具{@link ISerializer ISerializer} 注入规则：<br>
     * 如果导入了Hessian的jar包，优先使用Hessian：{@link HessianSerializer HessianSerializer},否则使用{@link
     * JdkSerializer JdkSerializer}<br>
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(ISerializer.class)
    public ISerializer<Object> autoloadCacheSerializer() {
        ISerializer<Object> res;
        // 推荐优先使用：Hessian
        if (hessianPresent) {
            res = new HessianSerializer();
            log.debug("HessianSerializer auto-configured");
        } else if (kryoPresent) {
            res = new KryoSerializer();
            log.debug("KryoSerializer auto-configured");
        } else {
            res = new JdkSerializer();
            log.debug("JdkSerializer auto-configured");
        }
        return res;
    }

    @Configuration
    @ConditionalOnClass(Jedis.class)
    static class JedisCacheCacheManagerConfiguration {
        /**
         * 默认只支持{@link JedisClusterCacheManager}<br>
         *
         * @param config
         * @param serializer
         * @param connectionFactory
         * @return
         */
        @Bean
        @ConditionalOnMissingBean(ICacheManager.class)
        @ConditionalOnBean(JedisConnectionFactory.class)
        public ICacheManager autoloadCacheCacheManager(
                AutoloadCacheProperties config,
                ISerializer<Object> serializer,
                JedisConnectionFactory connectionFactory) {
            return createRedisCacheManager(config, serializer, connectionFactory);
        }

        private ICacheManager createRedisCacheManager(
                AutoloadCacheProperties config,
                ISerializer<Object> serializer,
                JedisConnectionFactory connectionFactory) {
            RedisConnection redisConnection = null;
            try {
                redisConnection = connectionFactory.getConnection();
                AbstractRedisCacheManager cacheManager = null;
                if (redisConnection instanceof JedisClusterConnection) {
                    JedisClusterConnection redisClusterConnection =
                            (JedisClusterConnection) redisConnection;
                    // 优先使用JedisCluster; 因为JedisClusterConnection 批量处理，需要使用JedisCluster
                    JedisCluster jedisCluster = redisClusterConnection.getNativeConnection();
                    cacheManager = new JedisClusterCacheManager(jedisCluster, serializer);
                } else {
                    cacheManager = new SpringRedisCacheManager(connectionFactory, serializer);
                }
                // 根据需要自行配置
                cacheManager.setHashExpire(config.getJedis().getHashExpire());
                return cacheManager;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                throw e;
            } finally {
                RedisConnectionUtils.releaseConnection(redisConnection, connectionFactory);
            }
        }
    }

    @Configuration
    @ConditionalOnClass(RedisClient.class)
    static class LettuceCacheCacheManagerConfiguration {
        /**
         * 默认只支持{@link LettuceRedisClusterCacheManager}<br>
         *
         * @param config
         * @param serializer
         * @param connectionFactory
         * @return
         */
        @Bean
        @ConditionalOnMissingBean(ICacheManager.class)
        @ConditionalOnBean(LettuceConnectionFactory.class)
        public ICacheManager autoloadCacheCacheManager(
                AutoloadCacheProperties config,
                ISerializer<Object> serializer,
                LettuceConnectionFactory connectionFactory) {
            return createRedisCacheManager(config, serializer, connectionFactory);
        }

        private ICacheManager createRedisCacheManager(
                AutoloadCacheProperties config,
                ISerializer<Object> serializer,
                LettuceConnectionFactory connectionFactory) {
            RedisConnection redisConnection = null;
            try {
                redisConnection = connectionFactory.getConnection();
                AbstractRedisCacheManager cacheManager = null;
                if (redisConnection instanceof LettuceClusterConnection) {
                    LettuceClusterConnection lettuceClusterConnection =
                            (LettuceClusterConnection) redisConnection;
                    try {
                        Field clusterClientField =
                                LettuceClusterConnection.class.getDeclaredField("clusterClient");
                        clusterClientField.setAccessible(true);
                        RedisClusterClient redisClusterClient =
                                (RedisClusterClient)
                                        clusterClientField.get(lettuceClusterConnection);
                        cacheManager =
                                new LettuceRedisClusterCacheManager(redisClusterClient, serializer);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                } else {
                    cacheManager = new SpringRedisCacheManager(connectionFactory, serializer);
                }
                // 根据需要自行配置
                cacheManager.setHashExpire(config.getJedis().getHashExpire());
                return cacheManager;
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                throw e;
            } finally {
                RedisConnectionUtils.releaseConnection(redisConnection, connectionFactory);
            }
        }
    }
}
