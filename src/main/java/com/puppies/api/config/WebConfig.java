package com.puppies.api.config;
import com.puppies.api.security.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/posts/**")
                .addPathPatterns("/api/posts/feed")
                .addPathPatterns("/api/posts/{postId}")
                .excludePathPatterns("/api/auth/login")
                .excludePathPatterns("/api/users");

    }
}