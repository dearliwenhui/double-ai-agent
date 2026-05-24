package com.dave.ai.transfer.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        config.setCodec(new JsonJacksonCodec());
        SingleServerConfig serverConfig = config.useSingleServer();
        serverConfig.setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase());

        if (redisProperties.getUsername() != null && !redisProperties.getUsername().isBlank()) {
            serverConfig.setUsername(redisProperties.getUsername());
        }
        if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
            serverConfig.setPassword(redisProperties.getPassword());
        }

        Duration timeout = redisProperties.getTimeout();
        if (timeout != null) {
            serverConfig.setTimeout((int) timeout.toMillis());
        }

        Duration connectTimeout = redisProperties.getConnectTimeout();
        if (connectTimeout != null) {
            serverConfig.setConnectTimeout((int) connectTimeout.toMillis());
        }

        return Redisson.create(config);
    }
}
