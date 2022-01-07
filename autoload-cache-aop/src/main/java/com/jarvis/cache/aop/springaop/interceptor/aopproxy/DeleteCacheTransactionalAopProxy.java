package com.jarvis.cache.aop.springaop.interceptor.aopproxy;

import com.jarvis.cache.common.aop.DeleteCacheTransactionalAopProxyChain;

import org.aopalliance.intercept.MethodInvocation;

public class DeleteCacheTransactionalAopProxy implements DeleteCacheTransactionalAopProxyChain {

    private final MethodInvocation invocation;

    public DeleteCacheTransactionalAopProxy(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public Object doProxyChain() throws Throwable {
        return invocation.proceed();
    }
}
