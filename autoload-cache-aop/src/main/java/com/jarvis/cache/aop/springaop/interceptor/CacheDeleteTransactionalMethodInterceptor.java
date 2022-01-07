package com.jarvis.cache.aop.springaop.interceptor;

import com.jarvis.cache.aop.springaop.interceptor.aopproxy.DeleteCacheTransactionalAopProxy;
import com.jarvis.cache.aop.springaop.util.AopUtil;
import com.jarvis.cache.common.annotation.CacheDeleteTransactional;
import com.jarvis.cache.core.CacheHandler;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;

/** 对 {@link CacheDeleteTransactional } 拦截注解 */
@Slf4j
public class CacheDeleteTransactionalMethodInterceptor implements MethodInterceptor {

    /** 是否开启 对 @CacheDeleteTransactional 注解的增强逻辑 */
    private final boolean isEnable;

    private final CacheHandler cacheHandler;

    public CacheDeleteTransactionalMethodInterceptor(CacheHandler cacheHandler, boolean isEnable) {
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
        if (method.isAnnotationPresent(CacheDeleteTransactional.class)) {
            CacheDeleteTransactional cacheDeleteTransactional =
                    method.getAnnotation(CacheDeleteTransactional.class);
            log.debug(
                    invocation.getThis().getClass().getName()
                            + "."
                            + method.getName()
                            + "-->@CacheDeleteTransactional");
            return cacheHandler.proceedDeleteCacheTransactional(
                    new DeleteCacheTransactionalAopProxy(invocation), cacheDeleteTransactional);
        } else {
            Method specificMethod =
                    AopUtils.getMostSpecificMethod(method, invocation.getThis().getClass());
            if (specificMethod.isAnnotationPresent(CacheDeleteTransactional.class)) {
                CacheDeleteTransactional cacheDeleteTransactional =
                        specificMethod.getAnnotation(CacheDeleteTransactional.class);
                log.debug(
                        invocation.getThis().getClass().getName()
                                + "."
                                + specificMethod.getName()
                                + "-->@CacheDeleteTransactional");
                return cacheHandler.proceedDeleteCacheTransactional(
                        new DeleteCacheTransactionalAopProxy(invocation), cacheDeleteTransactional);
            }
        }
        return invocation.proceed();
    }
}
