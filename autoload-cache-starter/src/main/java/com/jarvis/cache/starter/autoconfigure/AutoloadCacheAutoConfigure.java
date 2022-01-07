package com.jarvis.cache.starter.autoconfigure;

import com.jarvis.cache.aop.springaop.MethodAnnotationPointcutAdvisor;
import com.jarvis.cache.aop.springaop.interceptor.CacheDeleteMethodInterceptor;
import com.jarvis.cache.aop.springaop.interceptor.CacheDeleteTransactionalMethodInterceptor;
import com.jarvis.cache.aop.springaop.interceptor.CacheMethodInterceptor;
import com.jarvis.cache.common.annotation.Cache;
import com.jarvis.cache.common.annotation.CacheDelete;
import com.jarvis.cache.common.annotation.CacheDeleteTransactional;
import com.jarvis.cache.core.CacheHandler;
import com.jarvis.cache.lock.api.ILock;
import com.jarvis.cache.manager.api.ICacheManager;
import com.jarvis.cache.script.api.AbstractScriptParser;
import com.jarvis.cache.serializer.api.clone.ICloner;
import com.jarvis.cache.serializer.api.serializer.ISerializer;
import com.jarvis.cache.starter.admin.AutoloadCacheController;
import com.jarvis.cache.starter.admin.HTTPBasicAuthorizeAttribute;
import com.jarvis.cache.starter.properties.AutoloadCacheProperties;

import javax.annotation.PostConstruct;

import org.springframework.aop.framework.autoproxy.AbstractAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 对autoload-cache进行自动配置<br>
 * 需要先完成 {@link AutoloadCacheManageConfiguration AutoloadCacheManageConfiguration}的配置<br>
 * 然后执行此类中的AOP相关的配置<br>
 */
@Configuration
@ConditionalOnClass(name = "com.jarvis.cache.core.CacheHandler")
@AutoConfigureAfter({AutoloadCacheManageConfiguration.class, DistributedLockConfiguration.class})
@ConditionalOnProperty(value = AutoloadCacheProperties.PREFIX + ".enable", matchIfMissing = true)
public class AutoloadCacheAutoConfigure {

    private static final String VALIDATOR_BEAN_NAME = "autoloadCacheAutoConfigurationValidator";

    @Autowired private AutoloadCacheProperties config;

    private final ILock lock;

    /**
     * 配置分布式锁 <br>
     * {@link DistributedLockConfiguration distributedLockConfiguration}
     */
    public AutoloadCacheAutoConfigure(ObjectProvider<ILock> lockProvider) {
        if (null != lockProvider) {
            lock = lockProvider.getIfAvailable();
        } else {
            lock = null;
        }
    }

    /**
     * 校验关键对象是否为空
     *
     * @return cacheManagerValidator
     */
    @Bean(name = VALIDATOR_BEAN_NAME)
    public CacheManagerValidator autoloadCacheAutoConfigurationValidator() {
        return new CacheManagerValidator();
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean(CacheHandler.class)
    @ConditionalOnBean({ICacheManager.class, AbstractScriptParser.class, ICloner.class})
    public CacheHandler autoloadCacheHandler(
            ICacheManager cacheManager, AbstractScriptParser scriptParser, ICloner cloner) {
        CacheHandler cacheHandler =
                new CacheHandler(cacheManager, scriptParser, config.getConfig(), cloner);
        cacheHandler.setLock(lock);
        return cacheHandler;
    }

    // -------------------------------- 注册拦截器（通知） --------------------------------
    @Bean
    @ConditionalOnBean(CacheHandler.class)
    @ConditionalOnProperty(
            value = AutoloadCacheProperties.PREFIX + ".enable-read-and-write",
            matchIfMissing = true)
    public CacheMethodInterceptor autoloadCacheMethodInterceptor(CacheHandler cacheHandler) {
        return new CacheMethodInterceptor(cacheHandler, config.isEnable());
    }

    @Bean
    @ConditionalOnBean(CacheHandler.class)
    @ConditionalOnProperty(
            value = AutoloadCacheProperties.PREFIX + ".enable-delete",
            matchIfMissing = true)
    public CacheDeleteMethodInterceptor autoloadCacheDeleteInterceptor(CacheHandler cacheHandler) {
        return new CacheDeleteMethodInterceptor(cacheHandler, config.isEnable());
    }

    @Bean
    @ConditionalOnBean(CacheHandler.class)
    @ConditionalOnProperty(
            value = AutoloadCacheProperties.PREFIX + ".enable-delete",
            matchIfMissing = true)
    public CacheDeleteTransactionalMethodInterceptor autoloadCacheDeleteTransactionalInterceptor(
            CacheHandler cacheHandler) {
        return new CacheDeleteTransactionalMethodInterceptor(cacheHandler, config.isEnable());
    }

    // -------------------------------- 配置Advisor --------------------------------
    @Bean("autoloadCacheAdvisor")
    @ConditionalOnBean(CacheMethodInterceptor.class)
    public AbstractPointcutAdvisor autoloadCacheAdvisor(
            CacheMethodInterceptor cacheMethodInterceptor) {
        AbstractPointcutAdvisor cacheAdvisor =
                new MethodAnnotationPointcutAdvisor(Cache.class, cacheMethodInterceptor);
        cacheAdvisor.setOrder(config.getCacheOrder());
        return cacheAdvisor;
    }

    @Bean("autoloadCacheDeleteAdvisor")
    @ConditionalOnBean(CacheDeleteMethodInterceptor.class)
    public AbstractPointcutAdvisor autoloadCacheDeleteAdvisor(
            CacheDeleteMethodInterceptor cacheDeleteMethodInterceptor) {
        AbstractPointcutAdvisor cacheDeleteAdvisor =
                new MethodAnnotationPointcutAdvisor(
                        CacheDelete.class, cacheDeleteMethodInterceptor);
        cacheDeleteAdvisor.setOrder(config.getDeleteCacheOrder());
        return cacheDeleteAdvisor;
    }

    @Bean("autoloadCacheDeleteTransactionalAdvisor")
    @ConditionalOnBean(CacheDeleteTransactionalMethodInterceptor.class)
    public AbstractPointcutAdvisor autoloadCacheDeleteTransactionalAdvisor(
            CacheDeleteTransactionalMethodInterceptor cacheDeleteTransactionalMethodInterceptor) {
        AbstractPointcutAdvisor cacheDeleteTransactionalAdvisor =
                new MethodAnnotationPointcutAdvisor(
                        CacheDeleteTransactional.class, cacheDeleteTransactionalMethodInterceptor);
        cacheDeleteTransactionalAdvisor.setOrder(config.getDeleteCacheTransactionalOrder());
        return cacheDeleteTransactionalAdvisor;
    }

    // 3.配置ProxyCreator
    @Bean
    @ConditionalOnBean(CacheHandler.class)
    public AbstractAdvisorAutoProxyCreator autoloadCacheAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator proxy = new DefaultAdvisorAutoProxyCreator();
        proxy.setAdvisorBeanNamePrefix("autoloadCache");
        // 代理有两种方式：一种是接口代理（上文提到过的动态代理），一种是CGLIB。默认有接口的类采用接口代理，否则使用CGLIB。如果设置成true,则直接使用CGLIB；
        proxy.setProxyTargetClass(config.isProxyTargetClass());
        // proxy.setInterceptorNames("cacheAdvisor","cacheDeleteAdvisor","cacheDeleteTransactionalAdvisor");//
        // 注意此处不需要设置，否则会执行两次
        return proxy;
    }

    /**
     * 配置请求过滤器，拦截指定url的请求 todo 请求的目的还不确定
     *
     * @return
     */
    @Bean
    @ConditionalOnWebApplication
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        HTTPBasicAuthorizeAttribute httpBasicFilter = new HTTPBasicAuthorizeAttribute(config);
        registrationBean.setFilter(httpBasicFilter);
        List<String> urlPatterns = new ArrayList<String>();
        urlPatterns.add("/autoload-cache-ui.html");
        urlPatterns.add("/autoload-cache/*");
        registrationBean.setUrlPatterns(urlPatterns);
        return registrationBean;
    }

    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnMissingBean(AutoloadCacheController.class)
    public AutoloadCacheController AutoloadCacheController(CacheHandler autoloadCacheHandler) {
        return new AutoloadCacheController(autoloadCacheHandler);
    }

    /** 校验 表达式解析/序列化/内存管理 是否为空 */
    static class CacheManagerValidator {

        @Autowired(required = false)
        private AbstractScriptParser scriptParser;

        @Autowired(required = false)
        private ISerializer<Object> serializer;

        @Autowired(required = false)
        private ICacheManager cacheManager;

        @PostConstruct
        public void checkHasCacheManager() {
            Assert.notNull(this.scriptParser, "No script parser could be auto-configured");
            Assert.notNull(this.serializer, "No serializer could be auto-configured");
            Assert.notNull(this.cacheManager, "No cache manager could be auto-configured");
        }
    }
}
