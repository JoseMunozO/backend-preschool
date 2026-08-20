package com.preschool.backendpreschool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendPreschoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendPreschoolApplication.class, args);
    }

}
