package com.banking.engine;

import com.banking.engine.config.BankingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(BankingProperties.class)
public class BankingEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingEngineApplication.class, args);
    }
}
