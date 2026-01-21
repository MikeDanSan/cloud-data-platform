package com.michael.backendservice.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CustomInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", new AppInfo("backend-service"))
               .withDetail("runtime", new RuntimeInfo(System.getProperty("java.version")))
               .withDetail("startedAt", Instant.now().toString());
    }

    record AppInfo(String name) {}
    record RuntimeInfo(String java) {}
}
