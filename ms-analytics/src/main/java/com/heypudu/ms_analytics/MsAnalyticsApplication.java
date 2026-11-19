package com.heypudu.ms_analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class MsAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAnalyticsApplication.class, args);
    }
}
