package com.smartdocument.ordermanagement.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter

@Configuration
class RabbitMQConfig {
    companion object {
        const val ORDER_EXCHANGE = "order.exchange"
        const val ORDER_PLACED_QUEUE = "order.placed.queue"
        const val ORDER_PLACED_ROUTING_KEY = "order.placed"
        const val ORDER_CANCELLED_QUEUE = "order.cancelled.queue"
        const val ORDER_CANCELLED_ROUTING_KEY = "order.cancelled"
    }

    @Bean
    fun orderExchange(): TopicExchange = TopicExchange(ORDER_EXCHANGE)

    @Bean
    fun orderPlacedQueue(): Queue = Queue(ORDER_PLACED_QUEUE, true)

    @Bean
    fun orderPlacedBinding(orderPlacedQueue: Queue, orderExchange: TopicExchange): Binding =
        BindingBuilder.bind(orderPlacedQueue).to(orderExchange).with(ORDER_PLACED_ROUTING_KEY)

    @Bean
    fun orderCancelledQueue(): Queue = Queue(ORDER_CANCELLED_QUEUE, true)

    @Bean
    fun orderCancelledBinding(orderCancelledQueue: Queue, orderExchange: TopicExchange): Binding =
        BindingBuilder.bind(orderCancelledQueue).to(orderExchange).with(ORDER_CANCELLED_ROUTING_KEY)

    @Bean
    fun jackson2JsonMessageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory, messageConverter: Jackson2JsonMessageConverter): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter
        return template
    }
}
