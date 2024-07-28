package com.github.lovept;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.github.lovept.mapper")
@EnableScheduling
public class RssBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RssBotApplication.class, args);
    }

}
