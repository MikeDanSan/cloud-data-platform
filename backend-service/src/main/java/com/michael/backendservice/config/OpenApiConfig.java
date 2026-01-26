package com.michael.backendservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cloud Data Platform API")
                        .version("0.1.0")
                        .description("Backend API for job orchestration and data processing. " +
                                "Supports job creation, status tracking, and S3 upload URL generation.")
                        .contact(new Contact()
                                .name("Michael Sanchez")
                                .email("mikedansanchez@gmail.com")
                                .url("https://https://github.com/MikeDanSan/cloud-data-platform")))
                .servers(List.of(
                        new Server().url("https://api.cloudpipes.net").description("Production"),
                        new Server().url("http://localhost:8080").description("Local Development")
                ))
                .tags(List.of(
                        new Tag().name("Jobs API").description("Public endpoints for job management"),
                        new Tag().name("Internal API").description("Internal endpoints for job workers (not for public use)")
                ));
    }
}