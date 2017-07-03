package eu.h2020.symbiote.cram.integration;


import eu.h2020.symbiote.cram.CoreResourceAccessMonitorApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.internal.CoreResourceType;

import eu.h2020.symbiote.cram.model.CramResource;

import static org.junit.Assert.assertEquals;


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
    @Qualifier("subIntervalDuration")
    private Long subIntervalDuration;

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

    private String platformUrl = "http://www.symbIoTe.com/";

    @Before
    public void setup() throws IOException, TimeoutException {

        rand = new Random();
    }

    @After
    public void clearRepos() {
        platformRepo.deleteAll();
        resourceRepo.deleteAll();
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
    }

    @Test
    public void PlatformUpdatedTest() throws Exception {

        log.info("PlatformUpdatedTest started!!!");
        Platform platform = createPlatform();

        platformRepo.save(platform);

        String newName = "platform" + rand.nextInt(50000);
        platform.setName(newName);

        sendPlatformMessage(platformExchangeName, platformUpdatedRoutingKey, platform);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Platform result = platformRepo.findOne(platform.getPlatformId());
        assertEquals(newName, result.getName());
    }

    @Test
    public void PlatformDeletedTest() throws Exception {

        log.info("PlatformDeletedTest started!!!");
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

        log.info("SensorCreatedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource resource1 = createResource(platform);
        CoreResource resource2 = createResource(platform);
        CoreResource resource3 = createResource(platform);
        CoreResource resource4 = createResource(platform);
        CoreResource resource5 = createResource(platform);

        resource1.setType(CoreResourceType.ACTUATOR);
        resource2.setType(CoreResourceType.SERVICE);
        resource3.setType(CoreResourceType.ACTUATING_SERVICE);
        resource4.setType(CoreResourceType.STATIONARY_SENSOR);
        resource5.setType(CoreResourceType.MOBILE_DEVICE);

        CoreResourceRegisteredOrModifiedEventPayload regMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        ArrayList<CoreResource> resources = new ArrayList<CoreResource>();
        resources.add(resource1);
        resources.add(resource2);
        resources.add(resource3);
        resources.add(resource4);
        resources.add(resource5);
        regMessage.setPlatformId(platform.getPlatformId());
        regMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceCreatedRoutingKey, regMessage);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        CramResource result = resourceRepo.findOne(resource1.getId());
        assertEquals(platformUrl + "rap/Actuators('" + resource1.getId()
               + "')", result.getResourceUrl());
        assertEquals(0, (long) result.getViewsInDefinedInterval());
        assertEquals((long) subIntervalDuration,  result.getViewsInSubIntervals().get(0).getEndOfInterval().getTime() -
                result.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());


        result = resourceRepo.findOne(resource2.getId());
        assertEquals(platformUrl + "rap/Services('" + resource2.getId()
               + "')", result.getResourceUrl());

        result = resourceRepo.findOne(resource3.getId());
        assertEquals(platformUrl + "rap/ActuatingServices('" + resource3.getId()
               + "')", result.getResourceUrl());


        result = resourceRepo.findOne(resource4.getId());
        assertEquals(platformUrl + "rap/Sensors('" + resource4.getId()
               + "')", result.getResourceUrl());

        result = resourceRepo.findOne(resource5.getId());
        assertEquals(platformUrl + "rap/Sensors('" + resource5.getId()
               + "')", result.getResourceUrl());

	}

    @Test
    public void SensorUpdatedTest() throws Exception {

        log.info("SensorUpdatedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource coreResource1 = createResource(platform);
        CramResource cramResource1 = new CramResource(coreResource1);
        resourceRepo.save(cramResource1);
        CoreResource coreResource2 = createResource(platform);
        CramResource cramResource2 = new CramResource(coreResource2);
        resourceRepo.save(cramResource2);

        String resourceNewLabel = "label3";
        List<String> labels = Arrays.asList("label1", "label2", resourceNewLabel);

        CoreResource newCoreResource1 = new CoreResource();
        newCoreResource1.setId(coreResource1.getId());
        newCoreResource1.setLabels(labels);
        newCoreResource1.setInterworkingServiceURL(coreResource1.getInterworkingServiceURL());
        newCoreResource1.setType(CoreResourceType.ACTUATOR);

        CoreResource newCoreResource2 = new CoreResource();
        newCoreResource2.setId(coreResource2.getId());
        newCoreResource2.setLabels(labels);
        newCoreResource2.setInterworkingServiceURL(coreResource2.getInterworkingServiceURL());
        newCoreResource2.setType(CoreResourceType.ACTUATOR);

        CoreResourceRegisteredOrModifiedEventPayload updMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        ArrayList<CoreResource> resources = new ArrayList<CoreResource>();
        resources.add(newCoreResource1);
        resources.add(newCoreResource2);
        updMessage.setPlatformId(platform.getPlatformId());
        updMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceUpdatedRoutingKey, updMessage);


        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        CramResource result = resourceRepo.findOne(coreResource1.getId());
        assertEquals(resourceNewLabel, result.getLabels().get(2));

        result = resourceRepo.findOne(coreResource2.getId());
        assertEquals(resourceNewLabel, result.getLabels().get(2));

	}

    @Test
    public void SensorDeletedTest() throws Exception {

        log.info("SensorDeletedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource coreResource1 = createResource(platform);
        CramResource resource1 = new CramResource(coreResource1);
        resourceRepo.save(resource1);
        CoreResource coreResource2 = createResource(platform);
        CramResource resource2 = new CramResource(coreResource2);
        resourceRepo.save(resource2);
        ArrayList<String> resources = new ArrayList<String>();
        resources.add(resource1.getId());
        resources.add(resource2.getId());

        sendResourceDeleteMessage(resourceExchangeName, resourceRemovedRoutingKey, resources);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        CramResource result = resourceRepo.findOne(resource1.getId());
        assertEquals(null, result);
        result = resourceRepo.findOne(resource2.getId());
        assertEquals(null, result);

    }


    Platform createPlatform() {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);

        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl(platformUrl);
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
        resource.setInterworkingServiceURL(platformUrl);
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
