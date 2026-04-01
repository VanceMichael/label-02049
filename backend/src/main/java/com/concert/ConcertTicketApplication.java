package com.concert;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.concert.mapper")
@EnableScheduling
@EnableAsync
public class ConcertTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConcertTicketApplication.class, args);
    }
}
