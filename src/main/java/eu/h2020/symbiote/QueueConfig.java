package eu.h2020.symbiote;

import eu.h2020.symbiote.repository.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;


@Configuration
public class QueueConfig {

    private static Log log = LogFactory.getLog(QueueConfig.class);

    @Autowired
    RepositoryManager repositoryManager;

    @Autowired
    ConnectionFactory connectionFactory;

    @Autowired 
    Jackson2JsonMessageConverter jackson2JsonMessageConverter;


    @Bean(name="platformExchange")
    Exchange platformExchange() {
        return new TopicExchange("symbIoTe.platform", true, false);
    }
    
    @Bean(name="platformRegistration")
    Queue platformRegistrationQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-platform-created", true, false, false);
    }

    @Bean
    Binding resourceRegistrationBinding(@Qualifier("platformRegistration") Queue queue,
                             @Qualifier("platformExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to((TopicExchange) exchange).with("symbIoTe.platform.created");
    }
    
    
    @Bean
    SimpleMessageListenerContainer resourceRegContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("platformRegistrationAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-platform-created");
        container.setMessageListener(listenerAdapter);
        // container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }


    @Bean(name="platformRegistrationAdapter")
    MessageListenerAdapter resourceRegistrationListenerAdapter(RepositoryManager repositoryManager) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(repositoryManager, "savePlatform");
        // adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }
}