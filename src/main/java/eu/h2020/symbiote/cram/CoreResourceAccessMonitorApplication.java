package eu.h2020.symbiote.cram;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.data.mongodb.core.geo.GeoJsonModule;

import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cram.model.NextPopularityUpdate;
import eu.h2020.symbiote.cram.repository.CramPersistentVariablesRepository;
import eu.h2020.symbiote.util.IntervalFormatter;

@EnableDiscoveryClient
@EnableRabbit
@SpringBootApplication
public class CoreResourceAccessMonitorApplication {

	private static Log log = LogFactory.getLog(CoreResourceAccessMonitorApplication.class);

	@Autowired
    private CramPersistentVariablesRepository cramPersistentVariablesRepository;

    @Value("${rabbit.host}") 
    private String rabbitMQHostIP;

    @Value("${rabbit.username}") 
    private String rabbitMQUsername;

    @Value("${rabbit.password}") 
    private String rabbitMQPassword;

    @Value("${aam.environment.coreInterfaceAddress}")
    private String symbioteCoreInterfaceAddress;

    @Value("${subIntervalDuration}")
    private String subIntervalDurationString;

    @Value("${intervalDuration}")
    private String intervalDurationString;

    @Value("${informSearchInterval}")
    private String informSearchIntervalString;

    @Value("${rabbit.exchange.search.name}")
    private String searchExchange;

    @Value("${rabbit.routingKey.search.popularityUpdates}")
    private String searchPopularityUpdatesRoutingKey;

	public static void main(String[] args) {
		SpringApplication.run(CoreResourceAccessMonitorApplication.class, args);
    }

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean(name="searchExchange")
    public String searchExchange() {
        return searchExchange;
    }

    @Bean(name="searchPopularityUpdatesRoutingKey")
    public String searchPopularityUpdatesRoutingKey() {
        return searchPopularityUpdatesRoutingKey;
    }

    @Bean(name="subIntervalDuration")
    public Long subIntervalDuration() {
        IntervalFormatter subInterval = new IntervalFormatter(subIntervalDurationString);
	    return subInterval.getMillis();
    }

    @Bean(name="intervalDuration")
    public Long intervalDuration() {
        IntervalFormatter interval = new IntervalFormatter(intervalDurationString);
        return interval.getMillis();
    }

    @Bean(name="noSubIntervals")
    public Long noSubIntervals(@Qualifier("subIntervalDuration") Long subIntervalDuration,
                               @Qualifier("intervalDuration") Long intervalDuration) {
        log.info("intervalDuration is :" + intervalDuration + " ms");
        log.info("SubIntervalDuration is :" + subIntervalDuration + " ms");
	    return intervalDuration/subIntervalDuration;
    }

    @Bean(name="informSearchInterval")
    public Long informSearchInterval() {
        IntervalFormatter informSearchInterval = new IntervalFormatter(informSearchIntervalString);
        return informSearchInterval.getMillis();
    }

    @Bean
    public NextPopularityUpdate nextPopularityUpdate(@Qualifier("subIntervalDuration") Long subIntervalDuration) {
	    log.info("SubIntervalDuration is :" + subIntervalDuration + " ms");
	    NextPopularityUpdate nextPopularityUpdate = (NextPopularityUpdate) cramPersistentVariablesRepository.findByVariableName("NEXT_POPULARITY_UPDATE");
	    if (nextPopularityUpdate == null) {
            log.info("No NextPopularityUpdate was saved in Database");
            NextPopularityUpdate newPopularityUpdate = new NextPopularityUpdate(subIntervalDuration);
            cramPersistentVariablesRepository.save(newPopularityUpdate);
            return newPopularityUpdate;
        }
        else if (new Date().getTime() > nextPopularityUpdate.getNextUpdate().getTime()){
            log.info("NextPopularityUpdate saved in Database has passed: " + nextPopularityUpdate.getNextUpdate().getTime());
            nextPopularityUpdate.getNextUpdate().setTime(new Date().getTime() + subIntervalDuration);
            log.info("New NextPopularityUpdate is at: " + nextPopularityUpdate.getNextUpdate().getTime());
            cramPersistentVariablesRepository.save(nextPopularityUpdate);
            return nextPopularityUpdate;
        }
	    else {
            log.info("NextPopularityUpdate saved in Database has not passed: " + nextPopularityUpdate.getNextUpdate().getTime());
            return nextPopularityUpdate;
        }

    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }

    @Bean 
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        return converter;
    }


    @Bean
    public ConnectionFactory connectionFactory() throws Exception {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitMQHostIP);
        // connectionFactory.setPublisherConfirms(true);
        // connectionFactory.setPublisherReturns(true);
        connectionFactory.setUsername(rabbitMQUsername);
        connectionFactory.setPassword(rabbitMQPassword);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public AsyncRabbitTemplate asyncRabbitTemplate(RabbitTemplate rabbitTemplate) {

       /**
        * The following AsyncRabbitTemplate constructor uses "Direct replyTo" for replies.
        */
        AsyncRabbitTemplate asyncRabbitTemplate = new AsyncRabbitTemplate(rabbitTemplate);
        asyncRabbitTemplate.setReceiveTimeout(5000);

        return asyncRabbitTemplate;
    }

}
