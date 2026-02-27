package com.jkingai.diagramarchitect.config;

import com.google.api.gax.rpc.ResourceExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // Retry on Spring AI's default transient exceptions + gRPC rate limit errors
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3,
                Map.of(
                        TransientAiException.class, true,
                        ResourceAccessException.class, true,
                        RuntimeException.class, true
                ),
                true  // traverseCauses — walks the cause chain
        );

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(2000);
        backOffPolicy.setMultiplier(3);
        backOffPolicy.setMaxInterval(15000);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                log.warn("Retry attempt {} for LLM call. Error: {}", context.getRetryCount(), throwable.getMessage());
                if (isRateLimitError(throwable)) {
                    log.warn("Rate limit (RESOURCE_EXHAUSTED) detected, backing off before retry");
                }
            }
        });

        return retryTemplate;
    }

    public static boolean isRateLimitError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ResourceExhaustedException) {
                return true;
            }
            if (current.getClass().getSimpleName().equals("ResourceExhaustedException")) {
                return true;
            }
            if (current instanceof io.grpc.StatusRuntimeException sre) {
                if (sre.getStatus().getCode() == io.grpc.Status.Code.RESOURCE_EXHAUSTED
                        || sre.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
