package com.site.blog.my.core.config;

import com.site.blog.my.core.interceptor.AdminLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.web.util.HttpSessionMutexListener;

import java.nio.file.Path;

@Configuration
public class MyBlogWebMvcConfigurer implements WebMvcConfigurer {
    // 同一会话即使由不同请求包装对象访问，也使用稳定的锁；更换 Session ID 不改变它。
    @Bean
    public ServletListenerRegistrationBean<HttpSessionMutexListener> sessionMutexListener() {
        return new ServletListenerRegistrationBean<>(new HttpSessionMutexListener());
    }

    @Value("${config.FILE_UPLOAD_DIC}")
    private String FILE_UPLOAD_DIC;

    @Autowired
    private AdminLoginInterceptor adminLoginInterceptor;

    public void addInterceptors(InterceptorRegistry registry) {
        // 添加一个拦截器，拦截以/admin为前缀的url路径
        registry.addInterceptor(adminLoginInterceptor).addPathPatterns("/admin/**").excludePathPatterns("/admin/login").excludePathPatterns("/admin/dist/**").excludePathPatterns("/admin/plugins/**");
    }

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 资源目录必须以 / 结尾，否则 Spring 会把最后一级当成文件名
        String location = Path.of(FILE_UPLOAD_DIC).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/upload/**").addResourceLocations(location);
    }
}
