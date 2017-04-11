package eu.h2020.symbiote;

import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.CustomExchange;
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

import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.messaging.RpcServer;


@Configuration
public class QueueConfig {

    private static Log log = LogFactory.getLog(QueueConfig.class);

    @Autowired
    RepositoryManager repositoryManager;

    @Autowired
    RpcServer rpcServer;

    @Autowired
    ConnectionFactory connectionFactory;

    @Autowired 
    Jackson2JsonMessageConverter jackson2JsonMessageConverter;

    @Value("${rabbit.exchange.platform.name}")
    private String platformExchangeName;
    @Value("${rabbit.exchange.platform.type}")
    private String platformExchangeType;
    @Value("${rabbit.exchange.platform.durable}")
    private boolean platformExchangeDurable;
    @Value("${rabbit.exchange.platform.autodelete}")
    private boolean platformExchangeAutodelete;
    @Value("${rabbit.exchange.platform.internal}")
    private boolean platformExchangeInternal;

    @Value("${rabbit.routingKey.platform.created}")
    private String platformCreatedRoutingKey;
    @Value("${rabbit.routingKey.platform.modified}")
    private String platformUpdatedRoutingKey;
    @Value("${rabbit.routingKey.platform.removed}")
    private String platformRemovedRoutingKey;


    @Value("${rabbit.exchange.resource.name}")
    private String resourceExchangeName;
    @Value("${rabbit.exchange.resource.type}")
    private String resourceExchangeType;
    @Value("${rabbit.exchange.resource.durable}")
    private boolean resourceExchangeDurable;
    @Value("${rabbit.exchange.resource.autodelete}")
    private boolean resourceExchangeAutodelete;
    @Value("${rabbit.exchange.resource.internal}")
    private boolean resourceExchangeInternal;

    @Value("${rabbit.routingKey.resource.created}")
    private String resourceCreatedRoutingKey;
    @Value("${rabbit.routingKey.resource.modified}")
    private String resourceUpdatedRoutingKey;
    @Value("${rabbit.routingKey.resource.removed}")
    private String resourceRemovedRoutingKey;

    @Value("${rabbit.exchange.cram.name}")
    private String cramExchangeName;
    @Value("${rabbit.exchange.cram.type}")
    private String cramExchangeType;
    @Value("${rabbit.exchange.cram.durable}")
    private boolean cramExchangeDurable;
    @Value("${rabbit.exchange.cram.autodelete}")
    private boolean cramExchangeAutodelete;
    @Value("${rabbit.exchange.cram.internal}")
    private boolean cramExchangeInternal;

    @Value("${rabbit.routingKey.cram.getResourceUrls}")
    private String cramGetResourceUrlsRoutingKey;

    @Bean(name="platformExchange")
    Exchange platformExchange() {
        
        switch (platformExchangeType) {
            case "header":  return new HeadersExchange(platformExchangeName, platformExchangeDurable, platformExchangeAutodelete);
            case "topic":   return new TopicExchange(platformExchangeName, platformExchangeDurable, platformExchangeAutodelete);
            case "direct":  return new DirectExchange(platformExchangeName, platformExchangeDurable, platformExchangeAutodelete);
            case "fanout":  return new FanoutExchange(platformExchangeName, platformExchangeDurable, platformExchangeAutodelete);
        }     

        return null;
    }

    @Bean(name="resourceExchange")
    Exchange resourceExchange() {
        
        switch (resourceExchangeType) {
            case "header":  return new HeadersExchange(resourceExchangeName, resourceExchangeDurable, resourceExchangeAutodelete);
            case "topic":   return new TopicExchange(resourceExchangeName, resourceExchangeDurable, resourceExchangeAutodelete);
            case "direct":  return new DirectExchange(resourceExchangeName, resourceExchangeDurable, resourceExchangeAutodelete);
            case "fanout":  return new FanoutExchange(resourceExchangeName, resourceExchangeDurable, resourceExchangeAutodelete);
        }     

        return null;
    }    

    @Bean(name="cramExchange")
    Exchange cramExchange() {
        
        switch (cramExchangeType) {
            case "header":  return new HeadersExchange(cramExchangeName, cramExchangeDurable, cramExchangeAutodelete);
            case "topic":   return new TopicExchange(cramExchangeName, cramExchangeDurable, cramExchangeAutodelete);
            case "direct":  return new DirectExchange(cramExchangeName, cramExchangeDurable, cramExchangeAutodelete);
            case "fanout":  return new FanoutExchange(cramExchangeName, cramExchangeDurable, cramExchangeAutodelete);
        }     

        return null;
    }    

    @Bean(name="platformRegistration")
    Queue platformRegistrationQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-platform-created", true, false, false);
    }

    @Bean(name="platformUpdated")
    Queue platformUpdateQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-platform-updated", true, false, false);
    }

    @Bean(name="platformUnregistration")
    Queue platformUnregistrationQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-platform-deleted", true, false, false);
    }

    @Bean(name="resourceRegistration")
    Queue resourceRegistrationQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-resource-created", true, false, false);
    }

    @Bean(name="resourceUpdated")
    Queue resourceUpdateQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-resource-updated", true, false, false);
    }

    @Bean(name="resourceUnregistration")
    Queue resourceUnregistrationQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-resource-deleted", true, false, false);
    }

    @Bean(name="cramGetResourceUrls")
    Queue cramGetResourceUrlsQueue() {
        return new Queue("symbIoTe-CoreResourceAccessMonitor-coreAPI-get_resource_urls", true, false, false);
    }

    @Bean
    Binding platformRegistrationBinding(@Qualifier("platformRegistration") Queue queue,
                             @Qualifier("platformExchange") Exchange exchange) {
        switch (platformExchangeType) {
            case "header":  return null;
            case "topic":   return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(platformCreatedRoutingKey);
            case "direct":  return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(platformCreatedRoutingKey);
            case "fanout":  return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }     

        return null;              
    }
    
    @Bean
    Binding platformUpdateBinding(@Qualifier("platformUpdated") Queue queue,
                             @Qualifier("platformExchange") Exchange exchange) {
        switch (platformExchangeType) {
            case "header":  return null;
            case "topic":   return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(platformUpdatedRoutingKey);
            case "direct":  return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(platformUpdatedRoutingKey);
            case "fanout":  return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }     

        return null;  
    }

    @Bean
    Binding platformUnregistrationBinding(@Qualifier("platformUnregistration") Queue queue,
                             @Qualifier("platformExchange") Exchange exchange) {
        switch (platformExchangeType) {
            case "header":  return null;
            case "topic":   return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(platformRemovedRoutingKey);
            case "direct":  return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(platformRemovedRoutingKey);
            case "fanout":  return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }     

        return null;  
    }

    @Bean
    Binding resourceRegistrationBinding(@Qualifier("resourceRegistration") Queue queue,
                             @Qualifier("resourceExchange") Exchange exchange) {
        switch (resourceExchangeType) {
            case "header":  return null;
            case "topic":   return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(resourceCreatedRoutingKey);
            case "direct":  return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(resourceCreatedRoutingKey);
            case "fanout":  return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }     

        return null;              
    }
    
    @Bean
    Binding resourceUpdateBinding(@Qualifier("resourceUpdated") Queue queue,
                             @Qualifier("resourceExchange") Exchange exchange) {
        switch (resourceExchangeType) {
            case "header":  return null;
            case "topic":   return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(resourceUpdatedRoutingKey);
            case "direct":  return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(resourceUpdatedRoutingKey);
            case "fanout":  return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }     

        return null;  
    }

    @Bean
    Binding resourceUnregistrationBinding(@Qualifier("resourceUnregistration") Queue queue,
                             @Qualifier("resourceExchange") Exchange exchange) {
        switch (resourceExchangeType) {
            case "header":  return null;
            case "topic":   return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(resourceRemovedRoutingKey);
            case "direct":  return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(resourceRemovedRoutingKey);
            case "fanout":  return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }     

        return null;  
    }

    @Bean
    Binding cramGetResourceUrlsBinding(@Qualifier("cramGetResourceUrls") Queue queue,
                             @Qualifier("cramExchange") Exchange exchange) {
        switch (cramExchangeType) {
            case "header":  return null;
            case "topic":   return BindingBuilder.bind(queue).to((TopicExchange) exchange).with(cramGetResourceUrlsRoutingKey);
            case "direct":  return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(cramGetResourceUrlsRoutingKey);
            case "fanout":  return BindingBuilder.bind(queue).to((FanoutExchange) exchange);
        }     

        return null;  
    }

    @Bean
    SimpleMessageListenerContainer platformRegContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("platformRegistrationAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-platform-created");
        container.setMessageListener(listenerAdapter);
        container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer platformUpdContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("platformUpdateAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-platform-updated");
        container.setMessageListener(listenerAdapter);
        container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer platformUnregContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("platformUnregistrationAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-platform-deleted");
        container.setMessageListener(listenerAdapter);
        container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer resourceRegContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("resourceRegistrationAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-resource-created");
        container.setMessageListener(listenerAdapter);
        container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer resourceUpdContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("resourceUpdateAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-resource-updated");
        container.setMessageListener(listenerAdapter);
        container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer resourceUnregContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("resourceUnregistrationAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-resource-deleted");
        container.setMessageListener(listenerAdapter);
        container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }

    @Bean
    SimpleMessageListenerContainer cramGetResourceUrlsContainer(ConnectionFactory connectionFactory,
                                             @Qualifier("cramGetResourceUrlsAdapter") MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames("symbIoTe-CoreResourceAccessMonitor-coreAPI-get_resource_urls");
        container.setMessageListener(listenerAdapter);
        container.setMessageConverter(jackson2JsonMessageConverter);
        return container;
    }

    @Bean(name="platformRegistrationAdapter")
    MessageListenerAdapter platformRegistrationListenerAdapter(RepositoryManager repositoryManager) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(repositoryManager, "savePlatform");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }

    @Bean(name="platformUpdateAdapter")
    MessageListenerAdapter platformUpdateListenerAdapter(RepositoryManager repositoryManager) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(repositoryManager, "updatePlatform");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }

    @Bean(name="platformUnregistrationAdapter")
    MessageListenerAdapter platformUnregistrationListenerAdapter(RepositoryManager repositoryManager) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(repositoryManager, "deletePlatform");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }

    @Bean(name="resourceRegistrationAdapter")
    MessageListenerAdapter resourceRegistrationListenerAdapter(RepositoryManager repositoryManager) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(repositoryManager, "saveResource");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }

    @Bean(name="resourceUpdateAdapter")
    MessageListenerAdapter resourceUpdateListenerAdapter(RepositoryManager repositoryManager) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(repositoryManager, "updateResource");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }

    @Bean(name="resourceUnregistrationAdapter")
    MessageListenerAdapter resourceUnregistrationListenerAdapter(RepositoryManager repositoryManager) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(repositoryManager, "deleteResource");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }

    @Bean(name="cramGetResourceUrlsAdapter")
    MessageListenerAdapter cramGetResourceUrlsListenerAdapter(RpcServer rpcServer) {

        MessageListenerAdapter adapter = new MessageListenerAdapter(rpcServer, "getResourcesUrls");
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        return adapter;
    }

}