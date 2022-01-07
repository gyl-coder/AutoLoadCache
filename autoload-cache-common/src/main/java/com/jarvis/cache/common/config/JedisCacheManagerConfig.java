package com.jarvis.cache.common.config;

import lombok.Data;

/**
 * 对JedisClusterCacheManager 进行配置
 *
 * @author yanliang
 */
@Data
public class JedisCacheManagerConfig {

    /** Hash的缓存时长：等于0时永久缓存；大于0时，主要是为了防止一些已经不用的缓存占用内存;hashExpire小于0时，则使用@Cache中设置的expire值（默认值为-1）。 */
    private int hashExpire = -1;
}
