package com.jarvis.cache.starter.admin;

import com.jarvis.cache.starter.properties.AutoloadCacheProperties;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Base64;

/**
 * Filter 是 JavaEE 中 Servlet 规范的一个组件，位于包javax.servlet 中，它可以在 HTTP 请求到达 Servlet 之前，被一个或多个Filter处理。
 * <br>
 * 它的工作流程如图：客户端 ---> 过滤器（1，2，3...） ---> controller <br>
 * Filter的这个特性在生产环境中有很广泛的应用，如：修改请求和响应、防止xss攻击、包装二进制流使其可以多次读，等等。<br>
 */
public class HTTPBasicAuthorizeAttribute implements Filter {

    private static final String SESSION_AUTH_ATTRIBUTE = "autoload-cache-auth";

    private final AutoloadCacheProperties properties;

    public HTTPBasicAuthorizeAttribute(AutoloadCacheProperties properties) {
        this.properties = properties;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;
        String sessionAuth = (String) (request).getSession().getAttribute(SESSION_AUTH_ATTRIBUTE);

        if (sessionAuth == null) {
            if (!checkHeaderAuth(request, response)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setHeader("Cache-Control", "no-store");
                response.setDateHeader("Expires", 0);
                response.setHeader(
                        "WWW-Authenticate", "Basic realm=\"input username and password\"");
                return;
            }
        }
        // 如果Filter要使请求继续被处理，就一定要调用filterChain.doFilter()
        chain.doFilter(request, response);
    }

    private boolean checkHeaderAuth(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String userName = properties.getAdminUserName();
        if (null == userName || userName.isEmpty()) {
            return true;
        }
        String password = properties.getAdminPassword();
        String auth = request.getHeader("Authorization");
        if ((auth != null) && (auth.length() > 6)) {
            auth = auth.substring(6, auth.length());
            String decodedAuth = getFromBASE64(auth);
            if (decodedAuth != null) {
                String[] userArray = decodedAuth.split(":");
                if (userArray != null && userArray.length == 2 && userName.equals(userArray[0])) {
                    if ((null == password || password.isEmpty())
                            || (null != password && password.equals(userArray[1]))) {
                        request.getSession().setAttribute(SESSION_AUTH_ATTRIBUTE, decodedAuth);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getFromBASE64(String s) {
        if (s == null) {
            return null;
        }
        try {
            byte[] b = Base64.getDecoder().decode(s);
            return new String(b);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void destroy() {}
}
