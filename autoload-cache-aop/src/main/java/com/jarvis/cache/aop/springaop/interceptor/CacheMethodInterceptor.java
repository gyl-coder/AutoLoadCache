package com.jarvis.cache.aop.springaop.interceptor;

import com.jarvis.cache.aop.springaop.interceptor.aopproxy.CacheAopProxy;
import com.jarvis.cache.aop.springaop.util.AopUtil;
import com.jarvis.cache.common.annotation.Cache;
import com.jarvis.cache.core.CacheHandler;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;

/** 对 {@link Cache } 拦截注解 */
@Slf4j
public class CacheMethodInterceptor implements MethodInterceptor {

    /** 是否开启 对 @Cache 注解的增强逻辑 */
    private final boolean isEnable;

    private final CacheHandler cacheHandler;

    public CacheMethodInterceptor(CacheHandler cacheHandler, boolean isEnable) {
        this.cacheHandler = cacheHandler;
        this.isEnable = isEnable;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (isEnable) {
            return invocation.proceed();
        }
        Method method = invocation.getMethod();
        // if (method.getDeclaringClass().isInterface()) {
        Class<?> cls = AopUtil.getTargetClass(invocation.getThis());
        if (!cls.equals(invocation.getThis().getClass())) {
            log.debug(invocation.getThis().getClass() + "-->" + cls);
            return invocation.proceed();
        }
        // }
        log.debug(invocation.toString());
        if (method.isAnnotationPresent(Cache.class)) {
            Cache cache = method.getAnnotation(Cache.class);
            log.debug(
                    invocation.getThis().getClass().getName()
                            + "."
                            + method.getName()
                            + "-->@Cache");
            return cacheHandler.proceed(new CacheAopProxy(invocation), cache);
        } else {
            Method specificMethod =
                    AopUtils.getMostSpecificMethod(method, invocation.getThis().getClass());
            if (specificMethod.isAnnotationPresent(Cache.class)) {
                Cache cache = specificMethod.getAnnotation(Cache.class);
                log.debug(
                        invocation.getThis().getClass().getName()
                                + "."
                                + specificMethod.getName()
                                + "-->@Cache");
                return cacheHandler.proceed(new CacheAopProxy(invocation), cache);
            }
        }
        return invocation.proceed();
    }
}
