package com.xk.xkainocode.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig implements RabbitListenerConfigurer {
    
    // 截图任务队列名称
    public static final String SCREENSHOT_TASK_QUEUE = "screenshot.task.queue";
    
    // 截图任务交换机名称
    public static final String SCREENSHOT_TASK_EXCHANGE = "screenshot.task.exchange";
    
    // 截图任务路由键
    public static final String SCREENSHOT_TASK_ROUTING_KEY = "screenshot.task";
    
    /**
     * 创建截图任务队列
     * 持久化队列，确保消息不会丢失
     */
    @Bean
    public Queue screenshotTaskQueue() {
        return new Queue(SCREENSHOT_TASK_QUEUE, true);
    }
    
    /**
     * 创建截图任务交换机
     * 主题交换机，支持路由键匹配
     */
    @Bean
    public TopicExchange screenshotTaskExchange() {
        return new TopicExchange(SCREENSHOT_TASK_EXCHANGE, true, false);
    }
    
    /**
     * 绑定队列和交换机
     */
    @Bean
    public Binding bindingScreenshotTaskQueue(Queue screenshotTaskQueue, TopicExchange screenshotTaskExchange) {
        return BindingBuilder.bind(screenshotTaskQueue).to(screenshotTaskExchange).with(SCREENSHOT_TASK_ROUTING_KEY);
    }
    
    /**
     * 使用Jackson2JsonMessageConverter替换默认的SimpleMessageConverter
     * 解决反序列化安全限制问题
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * 配置监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
    
    /**
     * 配置消息处理器方法工厂
     */
    @Bean
    public MessageHandlerMethodFactory messageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setMessageConverter(new MappingJackson2MessageConverter());
        return factory;
    }
    
    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
    }
}