package com.example.demo.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE_NAME = "campus.telemetry.queue";
    public static final String EXCHANGE_NAME = "campus.telemetry.exchange";
    public static final String ROUTING_KEY = "campus.sensor.telemetry";

    // Declare Queue
    @Bean
    public Queue telemetryQueue() {
        return new Queue(QUEUE_NAME, true); // durable = true
    }

    // Declare Topic Exchange
    @Bean
    public TopicExchange telemetryExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false); // durable = true, autoDelete = false
    }

    // Bind Queue to Exchange with Routing Key
    @Bean
    public Binding telemetryBinding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }
}
