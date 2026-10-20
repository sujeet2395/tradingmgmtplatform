package com.learn.tradingmgmt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration 
public class AppConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("My Trading Management API")
                        .version("1.0.0")
                        .description("Detailed documentation for your Spring Boot REST endpoints.")
                        .termsOfService("[http://swagger.io/terms/](http://swagger.io/terms/)")
                        .license(new License().name("Apache 2.0").url("[http://springdoc.org](http://springdoc.org)")));
    }
}
