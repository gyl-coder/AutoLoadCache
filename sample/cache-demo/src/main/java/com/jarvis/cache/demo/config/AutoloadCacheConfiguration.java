package com.jarvis.cache.demo.config;

import com.jarvis.cache.manager.api.ICacheManager;
import com.jarvis.cache.manager.impl.map.MapCacheManager;
import com.jarvis.cache.serializer.api.clone.ICloner;
import com.jarvis.cache.serializer.api.serializer.ISerializer;
import com.jarvis.cache.serializer.impl.fastjson.FastjsonSerializer;
import com.jarvis.cache.starter.autoconfigure.AutoloadCacheProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 为了方便测试，使用Map缓存 */
@Configuration
public class AutoloadCacheConfiguration {

    @Bean
    public ICacheManager mapCacheManager(AutoloadCacheProperties config, ICloner cloner) {
        return new MapCacheManager(config.getConfig(), cloner);
    }

    @Bean
    public ISerializer<Object> autoloadCacheSerializer() {
        return new FastjsonSerializer();
    }
}
