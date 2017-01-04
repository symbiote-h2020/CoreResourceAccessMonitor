package eu.h2020.symbiote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import org.springframework.data.mongodb.core.geo.GeoJsonModule;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Created by mateuszl on 22.09.2016.
 */
@EnableDiscoveryClient
@EnableRabbit
@SpringBootApplication
public class CoreResourceAccessMonitorApplication {

	private static Log log = LogFactory.getLog(CoreResourceAccessMonitorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(CoreResourceAccessMonitorApplication.class, args);

    }

    @Bean
    public AlwaysSampler defaultSampler() {
        return new AlwaysSampler();
    }

    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
        // connectionFactory.setUsername("guest");
        // connectionFactory.setPassword("guest");
        return connectionFactory;
}

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }

    @Bean Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        /**
         * It is necessary to register the GeoJsonModule, otherwise the GeoJsonPoint cannot
         * be deserialized by Jackson2JsonMessageConverter.
         */

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GeoJsonModule());
        converter.setJsonObjectMapper(mapper);
        return converter;
    }
    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        return template;
}
}
