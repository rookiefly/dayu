package com.rookiefly.open.dubbo.dayu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.rookiefly.open.dubbo.dayu.dao.mapper")
public class DubboDayuWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(DubboDayuWebApplication.class, args);
    }

}
