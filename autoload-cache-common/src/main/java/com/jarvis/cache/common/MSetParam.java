package com.jarvis.cache.common;

import com.jarvis.cache.common.to.CacheKeyTO;
import com.jarvis.cache.common.to.CacheWrapper;

import lombok.Data;

/** */
@Data
public class MSetParam {

    private CacheKeyTO cacheKey;

    private CacheWrapper<Object> result;

    public MSetParam(CacheKeyTO cacheKey, CacheWrapper<Object> result) {
        this.cacheKey = cacheKey;
        this.result = result;
    }
}
