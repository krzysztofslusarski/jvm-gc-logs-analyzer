package pl.ks.profiling.safepoint.analyzer.commons.shared;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
public class KernelConfiguration {
    @Bean
    StatsService statsService() {
        return new StatsService();
    }
}
