package com.banking.engine.config;

import com.banking.engine.messaging.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaConfig {

    @Bean
    KafkaAdmin.NewTopics bankingTopics(BankingProperties properties) {
        return new KafkaAdmin.NewTopics(
                topic(Topics.TRANSFER_COMMANDS, properties),
                topic(Topics.ACCOUNT_COMMANDS, properties),
                topic(Topics.ACCOUNT_RESULTS, properties),
                topic(Topics.TRANSFER_COMMANDS + Topics.DLT_SUFFIX, properties),
                topic(Topics.ACCOUNT_COMMANDS + Topics.DLT_SUFFIX, properties),
                topic(Topics.ACCOUNT_RESULTS + Topics.DLT_SUFFIX, properties));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            BankingProperties properties) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(properties.messaging().partitions());
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + Topics.DLT_SUFFIX, record.partition()));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 8L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class, JacksonException.class);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    private static NewTopic topic(String name, BankingProperties properties) {
        return TopicBuilder.name(name)
                .partitions(properties.messaging().partitions())
                .replicas(properties.messaging().replicas())
                .build();
    }
}
