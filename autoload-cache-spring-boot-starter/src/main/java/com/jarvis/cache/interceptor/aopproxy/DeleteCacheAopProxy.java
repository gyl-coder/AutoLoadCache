package com.jarvis.cache.interceptor.aopproxy;

import com.jarvis.cache.aop.DeleteCacheAopProxyChain;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/** */
public class DeleteCacheAopProxy implements DeleteCacheAopProxyChain {

    private final MethodInvocation invocation;

    private Method method;

    public DeleteCacheAopProxy(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public Object[] getArgs() {
        return invocation.getArguments();
    }

    @Override
    public Object getTarget() {
        return invocation.getThis();
    }

    @Override
    public Method getMethod() {
        if (null == method) {
            this.method = invocation.getMethod();
        }
        return method;
    }
}
