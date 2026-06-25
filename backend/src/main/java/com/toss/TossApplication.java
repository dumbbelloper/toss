package com.toss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TossApplication {

    public static void main(String[] args) {
        SpringApplication.run(TossApplication.class, args);
    }

}
