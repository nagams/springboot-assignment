package com.demo.solventumdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "demo")
public record DemoConfigProperties(
        String redisHost
) {
}
