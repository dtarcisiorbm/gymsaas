package com.gymsaas.shared.infrastructure.time;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * UTC system clock. Business dates are derived from it in the tenant time zone
 * (see TenantCalendar); tests can replace the bean with a fixed clock.
 */
@Configuration
public class TimeConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
