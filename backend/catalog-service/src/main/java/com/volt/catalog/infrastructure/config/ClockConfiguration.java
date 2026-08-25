package com.volt.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Infrastructure configuration that supplies the real system clock.
 *
 * <p>The use case depends on Java's {@link Clock} abstraction instead of
 * calling the current time directly. Tests can therefore inject a fixed clock
 * and remain deterministic.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
