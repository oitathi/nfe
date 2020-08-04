/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.b2wdigital.fazemu.config;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.google.common.collect.Maps;

import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * @author dailton.almeida
 */
@Configuration
@EnableCaching
public class RedisConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.cluster.nodes}")
    private String[] nodeArray;
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private Integer port;

    @Bean
    public RedisConnectionFactory connectionFactory() {
    	
        List<String> nodes = Arrays.asList(nodeArray);
        if (CollectionUtils.isNotEmpty(nodes) && StringUtils.isNotBlank(nodes.get(0))) {
            LOGGER.info("CONSTRUINDO REDIS CONNECTION FACTORY PARA CLUSTER {}", nodes);
            RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(nodes);
            JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisClusterConfiguration);
            jedisConnectionFactory.setPoolConfig(getPoolConfig());
//            jedisConnectionFactory.afterPropertiesSet();
            return jedisConnectionFactory;
        } else {
            LOGGER.info("CONSTRUINDO REDIS CONNECTION FACTORY STANDALONE {} {}", host, port);
            return new JedisConnectionFactory(new RedisStandaloneConfiguration(host, port));
        }
    }
    
    private JedisPoolConfig getPoolConfig() {
		final JedisPoolConfig poolConfig = new JedisPoolConfig();

		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);

		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestOnCreate(true);
		poolConfig.setTestWhileIdle(true);

		poolConfig.setMinEvictableIdleTimeMillis(60000);
		poolConfig.setTimeBetweenEvictionRunsMillis(30000);
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		
		return poolConfig;
    }

    @Bean
    public RedisOperations<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory());
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setHashValueSerializer(RedisSerializer.json());
        redisTemplate.setValueSerializer(RedisSerializer.json());
        return redisTemplate;
    }
    
    @Bean
    public CacheManager cacheManager() {
        SerializationPair<Object> sp = SerializationPair.fromSerializer(RedisSerializer.json());
        
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(2L))
                .disableKeyPrefix()
                .serializeValuesWith(sp)
                ;

        RedisCacheConfiguration otherConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5L))
                .disableKeyPrefix()
                .serializeValuesWith(sp)
                ;

        Map<String, RedisCacheConfiguration> cacheConfigurations = Maps.newHashMap();
        cacheConfigurations.put("TTL2", cacheConfiguration);
        cacheConfigurations.put("TTL5", otherConfiguration);

        RedisCacheManager result = RedisCacheManager.builder(connectionFactory())
//                .cacheDefaults(cacheConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
        return result;
    }
    
    @Bean
    public KeyGenerator keyGenerator() {
        return (Object target, Method method, Object... params) -> {
            String className = target.getClass().getSimpleName(), methodName = method.getName(), separator = "::";
            if (params.length == 0) {
                return StringUtils.joinWith(separator, className, methodName);
            } else {
                return StringUtils.joinWith(separator, className, methodName, StringUtils.join(params, separator));
            }
        };
    }
    
}
