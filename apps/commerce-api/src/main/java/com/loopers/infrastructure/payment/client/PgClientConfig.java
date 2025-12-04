package com.loopers.infrastructure.payment.client;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Request;
import feign.Retryer;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 * PG Feign Client 설정
 */
@Configuration
public class PgClientConfig {
    /**
     * 재시도 비활성화 (Resilience4j에서 처리)
     */
    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public Request.Options options() {
        return new Request.Options(
                5, TimeUnit.SECONDS,   // connectTimeout: 5초
                10, TimeUnit.SECONDS,  // readTimeout: 10초
                true                   // followRedirects
        );
    }

}
