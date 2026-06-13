package com.awm.app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.awm.dal.mapper")
@ConfigurationPropertiesScan("com.awm")
@EnableScheduling
public class AwmApplication {
    public static void main(String[] args) {
        SpringApplication.run(AwmApplication.class, args);
    }
}
