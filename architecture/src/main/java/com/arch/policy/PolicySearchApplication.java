package com.arch.policy;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@EnableDubbo
@SpringBootApplication
public class PolicySearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(PolicySearchApplication.class, args);
    }
}
