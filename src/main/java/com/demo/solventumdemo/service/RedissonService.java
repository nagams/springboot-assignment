package com.demo.solventumdemo.service;

import com.demo.solventumdemo.config.DemoConfigProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.redisson.Redisson;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *  Using Redisson java client for the Redis. Redisson provides rich concurrency APIs to work in a distributed
 *  environment. This service component initializes the client to connect to Redis and exposes one Redis semaphore
 *  to be available for use in our controllers.
 */
@Component
public class RedissonService {
    private static final String DISTRIBUTED_SEMAPHORE = "distributedSemaphore";
    private static final int MAX_AVAILABLE = 2;

    private static final Logger LOG = LoggerFactory.getLogger(RedissonService.class);

    RedissonClient redissonClient;
    RSemaphore redissonSemaphore;
    DemoConfigProperties properties;

    public RedissonService(DemoConfigProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(properties.redisHost());

        redissonClient = Redisson.create(config);
        redissonSemaphore = redissonClient.getSemaphore(DISTRIBUTED_SEMAPHORE);
        redissonSemaphore.trySetPermits(MAX_AVAILABLE);
    }

    @PreDestroy
    public void cleanup() {
        LOG.info("Performing cleanup tasks");
        redissonSemaphore.deleteAsync();
    }

    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    public RSemaphore getRedissonSemaphore() {
        return redissonSemaphore;
    }
}
