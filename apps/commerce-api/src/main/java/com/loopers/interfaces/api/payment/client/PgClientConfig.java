package com.loopers.interfaces.api.payment.client;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.Retryer;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 * PG Feign Client 설정
 */
@Configuration
public class PgClientConfig {

    @Bean
    public Request.Options options() {
        return new Request.Options(
                1, TimeUnit.SECONDS,
                3, TimeUnit.SECONDS,
                true);
    }

    @Bean
    public Logger.Level logLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 재시도 비활성화 (Resilience4j에서 처리)
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

}
