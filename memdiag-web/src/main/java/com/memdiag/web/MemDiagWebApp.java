package com.memdiag.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MemDiagWebApp {

    public static void main(String[] args) {
        SpringApplication.run(MemDiagWebApp.class, args);
    }
}
