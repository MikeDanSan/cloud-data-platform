package com.michael.backendservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.emrserverless.EmrServerlessClient;

@Configuration
public class EmrConfig {

    @Value("${app.aws.region}")
    private String awsRegion;

    @Bean
    public EmrServerlessClient emrServerlessClient() {
        return EmrServerlessClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}