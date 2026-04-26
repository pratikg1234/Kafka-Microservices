package com.example.order_service.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(template,
                        (record, ex) -> {
                            String topic = record.topic() + ".dlt";
                            return new TopicPartition(topic, record.partition());
                        });

        FixedBackOff backOff = new FixedBackOff(2000L, 3); // 3 retries, 2 sec gap

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
