package com.lms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Injected wherever "now" matters so that time-dependent logic can be tested deterministically. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
