package com.subscriptiontracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubscriptionTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionTrackerApplication.class, args);
    }
}
