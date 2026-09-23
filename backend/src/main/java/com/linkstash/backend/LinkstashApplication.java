package com.linkstash.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = SqlInitializationAutoConfiguration.class)
@EnableAsync
@MapperScan("com.linkstash.backend.mapper")
public class LinkstashApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkstashApplication.class, args);
    }
}
