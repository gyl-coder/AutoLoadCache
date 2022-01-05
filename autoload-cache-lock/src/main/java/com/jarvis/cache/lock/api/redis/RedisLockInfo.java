package com.jarvis.cache.lock.api.redis;

import lombok.Data;

/**
 * redis 锁信息
 *
 * @author nobody
 */
@Data
public class RedisLockInfo {

    /** 开始时间 */
    private Long startTime;

    /** 租约时长 */
    private Integer leaseTime;
}
