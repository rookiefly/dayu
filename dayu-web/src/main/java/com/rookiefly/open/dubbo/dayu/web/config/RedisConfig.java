package com.rookiefly.open.dubbo.dayu.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@ConfigurationProperties(prefix = "redis.config")
@Data
public class RedisConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String url;

    private Integer maxTotal;

    private Integer maxIdle;

    private Long maxWaitMillis;

    private Boolean testOnBorrow;
}