package com.fundmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FundManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundManagerApplication.class, args);
    }
}
