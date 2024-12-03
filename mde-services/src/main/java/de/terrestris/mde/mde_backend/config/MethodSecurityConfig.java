package de.terrestris.mde.mde_backend.config;

import de.terrestris.mde.mde_backend.security.MdePermissionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(MdePermissionEvaluator mdePermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(mdePermissionEvaluator);
        return expressionHandler;
    }

    @Bean
    public MdePermissionEvaluator mdePermissionEvaluator() {
        return new MdePermissionEvaluator();
    }
}