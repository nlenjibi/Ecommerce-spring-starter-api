package com.smart_ecomernce_api.smart_ecomernce_api.config;//package com.smart_ecomernce_api.Smart_ecommerce_api.config;
//
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//
//import java.time.Duration;
//import java.util.List;
//
//@Configuration
//@EnableCaching
//public class RedisConfig {
//
//    @Bean
//    public ObjectMapper redisObjectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//
//        // Add mixins for Spring Data classes - THIS IS CRITICAL
//        mapper.addMixIn(PageImpl.class, PageMixin.class);
//
//        // Enable polymorphic type handling with proper validator
//        BasicPolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
//                .allowIfBaseType(Object.class)
//                .build();
//
//        mapper.activateDefaultTyping(
//                validator,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//
//        // Disable features that cause issues
//        mapper.disable(
//                com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS
//        );
//
//        return mapper;
//    }
//
//    /**
//     * Mixin to help Jackson deserialize PageImpl
//     * This provides the constructor that Jackson needs
//     */
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    abstract static class PageMixin<T> {
//
//        @JsonCreator
//        PageMixin(
//                @JsonProperty("content") List<T> content,
//                @JsonProperty("number") int number,
//                @JsonProperty("size") int size,
//                @JsonProperty("totalElements") long totalElements
//        ) {
//            // Jackson will use this constructor
//        }
//    }
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory);
//
//        // Use String serializer for keys
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashKeySerializer(new StringRedisSerializer());
//
//        // Use JSON serializer for values with custom ObjectMapper
//        GenericJackson2JsonRedisSerializer serializer =
//                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
//        template.setValueSerializer(serializer);
//        template.setHashValueSerializer(serializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//        GenericJackson2JsonRedisSerializer serializer =
//                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
//
//        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(10))
//                .serializeKeysWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
//                )
//                .serializeValuesWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
//                )
//                .disableCachingNullValues();
//
//        return RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(config)
//                .withCacheConfiguration("product",
//                        config.entryTtl(Duration.ofMinutes(30)))
//                .withCacheConfiguration("users",
//                        config.entryTtl(Duration.ofMinutes(15)))
//                .withCacheConfiguration("categories",
//                        config.entryTtl(Duration.ofHours(1)))
//                .withCacheConfiguration("orders",
//                        config.entryTtl(Duration.ofMinutes(5)))
//                .transactionAware()
//                .build();
//    }
//}