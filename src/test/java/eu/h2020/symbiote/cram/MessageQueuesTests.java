package eu.h2020.symbiote.cram;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;
import org.junit.Before;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.URL;

import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.Location;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.model.internal.CoreResource;

import static org.junit.Assert.assertEquals;

import org.junit.rules.ExpectedException;

/** 
 * This file tests the PlatformRepository and ResourceRepository
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={CoreResourceAccessMonitorApplication.class})
@SpringBootTest(properties = {"eureka.client.enabled=false", 
                              "spring.sleuth.enabled=false"})
public class MessageQueuesTests {


    private static Logger log = LoggerFactory
                          .getLogger(MessageQueuesTests.class);
    
    @Autowired
    private ResourceRepository resourceRepo;
    
    @Autowired    
    private PlatformRepository platformRepo;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Random rand;

    @Value("${rabbit.exchange.platform.name}")
    private String platformExchangeName;
    @Value("${rabbit.routingKey.platform.created}")
    private String platformCreatedRoutingKey;
    @Value("${rabbit.routingKey.platform.modified}")
    private String platformUpdatedRoutingKey;
    @Value("${rabbit.routingKey.platform.removed}")
    private String platformRemovedRoutingKey;

    @Value("${rabbit.exchange.resource.name}")
    private String resourceExchangeName;
    @Value("${rabbit.routingKey.resource.created}")
    private String resourceCreatedRoutingKey;
    @Value("${rabbit.routingKey.resource.modified}")
    private String resourceUpdatedRoutingKey;
    @Value("${rabbit.routingKey.resource.removed}")
    private String resourceRemovedRoutingKey;

    @Before
    public void setup() throws IOException, TimeoutException {

        rand = new Random();

    }

    @Test
    public void PlatformCreatedTest() throws Exception {

        log.info("PlatformCreatedTest started!!!");
        Platform platform = createPlatform();

        sendPlatformMessage(platformExchangeName, platformCreatedRoutingKey, platform);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Platform result = platformRepo.findOne(platform.getPlatformId());
        assertEquals(platform.getName(), result.getName());

        platformRepo.delete(platform.getPlatformId());
    }

    @Test
    public void PlatformUpdatedTest() throws Exception {
        
        Platform platform = createPlatform();

        platformRepo.save(platform);

        String newName = "platform" + rand.nextInt(50000);
        platform.setName(newName);

        sendPlatformMessage(platformExchangeName, platformUpdatedRoutingKey, platform);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Platform result = platformRepo.findOne(platform.getPlatformId());
        assertEquals(newName, result.getName());

        platformRepo.delete(platform.getPlatformId());

    }

    @Test
    public void PlatformDeletedTest() throws Exception {

        Platform platform = createPlatform();

        platformRepo.save(platform);

        sendPlatformMessage(platformExchangeName, platformRemovedRoutingKey, platform);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Platform result = platformRepo.findOne(platform.getPlatformId());
        assertEquals(null, result);    
	}

    @Test
    public void SensorCreatedTest() throws Exception {

        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource resource1 = createResource(platform);
        CoreResource resource2 = createResource(platform);

        CoreResourceRegisteredOrModifiedEventPayload regMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        ArrayList<CoreResource> resources = new ArrayList<CoreResource>();
        resources.add(resource1);
        resources.add(resource2);
        regMessage.setPlatformId(platform.getPlatformId());
        regMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceCreatedRoutingKey, regMessage);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Resource result = resourceRepo.findOne(resource1.getId());

        assertEquals("http://www.symbIoTe.com/rap/Sensors('" + resource1.getId()
               + "')", result.getInterworkingServiceURL());   


        result = resourceRepo.findOne(resource2.getId());

        assertEquals("http://www.symbIoTe.com/rap/Sensors('" + resource2.getId()
               + "')", result.getInterworkingServiceURL());   

        platformRepo.delete(platform.getPlatformId());
        resourceRepo.delete(resource1.getId());
        resourceRepo.delete(resource2.getId());
	}

    @Test
    public void SensorUpdatedTest() throws Exception {

        Platform platform = createPlatform();
        platformRepo.save(platform);

        Resource resource1 = createResource(platform);
        resourceRepo.save(resource1);
        Resource resource2 = createResource(platform);
        resourceRepo.save(resource2);

        String resourceNewLabel = "label3";
        List<String> labels = Arrays.asList("label1", "label2", resourceNewLabel);

        CoreResource coreResource1 = new CoreResource();
        coreResource1.setId(resource1.getId());
        coreResource1.setLabels(labels);
        coreResource1.setInterworkingServiceURL(resource1.getInterworkingServiceURL());
        
        CoreResource coreResource2 = new CoreResource();
        coreResource2.setId(resource2.getId());
        coreResource2.setLabels(labels);
        coreResource2.setInterworkingServiceURL(resource2.getInterworkingServiceURL());

        CoreResourceRegisteredOrModifiedEventPayload updMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        ArrayList<CoreResource> resources = new ArrayList<CoreResource>();
        resources.add(coreResource1);
        resources.add(coreResource2);
        updMessage.setPlatformId(platform.getPlatformId());
        updMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceUpdatedRoutingKey, updMessage);


        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Resource result = resourceRepo.findOne(resource1.getId());
        assertEquals(resourceNewLabel, result.getLabels().get(2)); 

        result = resourceRepo.findOne(resource2.getId());
        assertEquals(resourceNewLabel, result.getLabels().get(2)); 

        platformRepo.delete(platform.getPlatformId());
        resourceRepo.delete(resource1.getId());
        resourceRepo.delete(resource1.getId());

	}

    @Test
    public void SensorDeletedTest() throws Exception {

        Platform platform = createPlatform();
        platformRepo.save(platform);

        Resource resource1 = createResource(platform);
        resourceRepo.save(resource1);
        Resource resource2 = createResource(platform);
        resourceRepo.save(resource2);
        ArrayList<String> resources = new ArrayList<String>();
        resources.add(resource1.getId());
        resources.add(resource2.getId());

        sendResourceDeleteMessage(resourceExchangeName, resourceRemovedRoutingKey, resources);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Resource result = resourceRepo.findOne(resource1.getId());
        assertEquals(null, result);
        result = resourceRepo.findOne(resource2.getId());
        assertEquals(null, result);

        platformRepo.delete(platform.getPlatformId());
    }


    Platform createPlatform() {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);

        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com/");
        platform.setInformationModelId("platform_info_model");

        return platform;
    }

    CoreResource createResource(Platform platform) {

        CoreResource resource = new CoreResource();
        String resourceId = Integer.toString(rand.nextInt(50000));
        resource.setId(resourceId);

        List<String> labels = Arrays.asList("label1", "label2");
        List<String> comments = Arrays.asList("comment1", "comment2");
        resource.setLabels(labels);
        resource.setComments(comments);
        resource.setInterworkingServiceURL("http://www.symbIoTe.com/");
        return resource;
    }

    void sendPlatformMessage (String exchange, String key, Platform platform) throws Exception {

        rabbitTemplate.convertAndSend(exchange, key, platform,
            m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                 });
    }

    void sendResourceMessage (String exchange, String key, CoreResourceRegisteredOrModifiedEventPayload resources) throws Exception {

        rabbitTemplate.convertAndSend(exchange, key, resources,
            m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                 });
    }

    void sendResourceDeleteMessage (String exchange, String key, List<String> resources) throws Exception {

        rabbitTemplate.convertAndSend(exchange, key, resources,
            m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                 });
    }

}
