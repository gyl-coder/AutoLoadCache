package com.jarvis.cache.aop.springaop.interceptor;

import com.jarvis.cache.aop.springaop.interceptor.aopproxy.DeleteCacheAopProxy;
import com.jarvis.cache.aop.springaop.util.AopUtil;
import com.jarvis.cache.common.annotation.CacheDelete;
import com.jarvis.cache.core.CacheHandler;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;

/** 对 {@link CacheDelete } 拦截注解 */
@Slf4j
public class CacheDeleteMethodInterceptor implements MethodInterceptor {

    /** 是否开启 对 @CacheDelete 注解的增强逻辑 */
    private final boolean isEnable;

    private final CacheHandler cacheHandler;

    public CacheDeleteMethodInterceptor(CacheHandler cacheHandler, boolean isEnable) {
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
        Object result = invocation.proceed();
        if (method.isAnnotationPresent(CacheDelete.class)) {
            CacheDelete cacheDelete = method.getAnnotation(CacheDelete.class);
            log.debug(
                    invocation.getThis().getClass().getName()
                            + "."
                            + method.getName()
                            + "-->@CacheDelete");
            cacheHandler.deleteCache(new DeleteCacheAopProxy(invocation), cacheDelete, result);
        } else {
            Method specificMethod =
                    AopUtils.getMostSpecificMethod(method, invocation.getThis().getClass());
            if (specificMethod.isAnnotationPresent(CacheDelete.class)) {
                CacheDelete cacheDelete = specificMethod.getAnnotation(CacheDelete.class);
                log.debug(
                        invocation.getThis().getClass().getName()
                                + "."
                                + specificMethod.getName()
                                + "-->@CacheDelete");
                cacheHandler.deleteCache(new DeleteCacheAopProxy(invocation), cacheDelete, result);
            }
        }
        return result;
    }
}
