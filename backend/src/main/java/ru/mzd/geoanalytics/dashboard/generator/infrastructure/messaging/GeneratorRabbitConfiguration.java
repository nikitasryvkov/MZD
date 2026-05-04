package ru.mzd.geoanalytics.dashboard.generator.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class GeneratorRabbitConfiguration {

    @Bean
    public Jackson2JsonMessageConverter generatorRabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public DirectExchange generatorExchange(GeneratorRabbitProperties properties) {
        return new DirectExchange(properties.getExchange(), true, false);
    }

    @Bean
    public Queue generatorReferenceNetworkQueue(GeneratorRabbitProperties properties) {
        return durableWorkQueue(properties.getQueues().getReferenceNetworkRequest());
    }

    @Bean
    public Queue generatorActiveEventsQueue(GeneratorRabbitProperties properties) {
        return durableWorkQueue(properties.getQueues().getActiveEventsRequest());
    }

    @Bean
    public Queue generatorTrainsSyncQueue(GeneratorRabbitProperties properties) {
        return durableWorkQueue(properties.getQueues().getTrainsSync());
    }

    @Bean
    public Queue generatorEventsSyncQueue(GeneratorRabbitProperties properties) {
        return durableWorkQueue(properties.getQueues().getEventsSync());
    }

    @Bean
    public Queue generatorPersonnelSnapshotQueue(GeneratorRabbitProperties properties) {
        return durableWorkQueue(properties.getQueues().getPersonnelSnapshotSync());
    }

    @Bean
    public Queue generatorReferenceNetworkDeadLetterQueue(GeneratorRabbitProperties properties) {
        return deadLetterQueue(properties.getQueues().getReferenceNetworkRequest());
    }

    @Bean
    public Queue generatorActiveEventsDeadLetterQueue(GeneratorRabbitProperties properties) {
        return deadLetterQueue(properties.getQueues().getActiveEventsRequest());
    }

    @Bean
    public Queue generatorTrainsSyncDeadLetterQueue(GeneratorRabbitProperties properties) {
        return deadLetterQueue(properties.getQueues().getTrainsSync());
    }

    @Bean
    public Queue generatorEventsSyncDeadLetterQueue(GeneratorRabbitProperties properties) {
        return deadLetterQueue(properties.getQueues().getEventsSync());
    }

    @Bean
    public Queue generatorPersonnelSnapshotDeadLetterQueue(GeneratorRabbitProperties properties) {
        return deadLetterQueue(properties.getQueues().getPersonnelSnapshotSync());
    }

    @Bean
    public Binding generatorReferenceNetworkBinding(
        @Qualifier("generatorReferenceNetworkQueue") Queue generatorReferenceNetworkQueue,
        DirectExchange generatorExchange,
        GeneratorRabbitProperties properties
    ) {
        return BindingBuilder.bind(generatorReferenceNetworkQueue)
            .to(generatorExchange)
            .with(properties.getRoutingKeys().getReferenceNetworkRequest());
    }

    @Bean
    public Binding generatorActiveEventsBinding(
        @Qualifier("generatorActiveEventsQueue") Queue generatorActiveEventsQueue,
        DirectExchange generatorExchange,
        GeneratorRabbitProperties properties
    ) {
        return BindingBuilder.bind(generatorActiveEventsQueue)
            .to(generatorExchange)
            .with(properties.getRoutingKeys().getActiveEventsRequest());
    }

    @Bean
    public Binding generatorTrainsSyncBinding(
        @Qualifier("generatorTrainsSyncQueue") Queue generatorTrainsSyncQueue,
        DirectExchange generatorExchange,
        GeneratorRabbitProperties properties
    ) {
        return BindingBuilder.bind(generatorTrainsSyncQueue)
            .to(generatorExchange)
            .with(properties.getRoutingKeys().getTrainsSync());
    }

    @Bean
    public Binding generatorEventsSyncBinding(
        @Qualifier("generatorEventsSyncQueue") Queue generatorEventsSyncQueue,
        DirectExchange generatorExchange,
        GeneratorRabbitProperties properties
    ) {
        return BindingBuilder.bind(generatorEventsSyncQueue)
            .to(generatorExchange)
            .with(properties.getRoutingKeys().getEventsSync());
    }

    @Bean
    public Binding generatorPersonnelSnapshotBinding(
        @Qualifier("generatorPersonnelSnapshotQueue") Queue generatorPersonnelSnapshotQueue,
        DirectExchange generatorExchange,
        GeneratorRabbitProperties properties
    ) {
        return BindingBuilder.bind(generatorPersonnelSnapshotQueue)
            .to(generatorExchange)
            .with(properties.getRoutingKeys().getPersonnelSnapshotSync());
    }

    private Queue durableWorkQueue(String queueName) {
        return QueueBuilder.durable(queueName)
            .deadLetterExchange("")
            .deadLetterRoutingKey(deadLetterQueueName(queueName))
            .build();
    }

    private Queue deadLetterQueue(String queueName) {
        return QueueBuilder.durable(deadLetterQueueName(queueName)).build();
    }

    private String deadLetterQueueName(String queueName) {
        return queueName + ".dlq";
    }
}
