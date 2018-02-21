package eu.h2020.symbiote.cram.integration;


import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.cram.CoreResourceAccessMonitorApplication;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;


/**
 * This file tests the PlatformRepository and ResourceRepository
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={CoreResourceAccessMonitorApplication.class})
@SpringBootTest(properties = {"eureka.client.enabled=false",
                              "spring.sleuth.enabled=false",
                              "subIntervalDuration=P0-0-0T1:0:0",
                              "intervalDuration=P0-0-0T2:0:0",
                              "informSearchInterval=P0-0-0T1:0:0",
                              "symbiote.core.cram.database=symbiote-core-cram-database-mqt",
                              "rabbit.queueName.cram.getResourceUrls=cramGetResourceUrls-mqt",
                              "rabbit.routingKey.cram.getResourceUrls=symbIoTe.CoreResourceAccessMonitor.coreAPI.get_resource_urls-mqt",
                              "rabbit.queueName.cram.accessNotifications=accessNotifications-mqt",
                              "rabbit.routingKey.cram.accessNotifications=symbIoTe.CoreResourceAccessMonitor.coreAPI.accessNotifications-mqt",
                              "rabbit.queueName.search.popularityUpdates=symbIoTe-search-popularityUpdatesReceived-mqt"})
@ActiveProfiles("test")
public class MessageQueuesTests {

    private static Logger log = LoggerFactory
                          .getLogger(MessageQueuesTests.class);

    @Autowired
    private ResourceRepository resourceRepo;

    @Autowired
    private PlatformRepository platformRepo;

    @Autowired
    private ResourceAccessStatsUpdater resourceAccessStatsUpdater;

    @Autowired
    private AccessNotificationListener accessNotificationListener;

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
        resourceAccessStatsUpdater.cancelTimer();
        rand = new Random();
    }

    @After
    public void clearSetup() {
        platformRepo.deleteAll();
        resourceRepo.deleteAll();
        accessNotificationListener.setScheduledUpdateOngoing(false);
        accessNotificationListener.getNotificationMessageList().clear();
        resourceAccessStatsUpdater.cancelTimer();
    }

    @Test
    public void PlatformCreatedTest() throws Exception {

        log.info("PlatformCreatedTest started!!!");
        Platform platform = createPlatform();

        sendPlatformMessage(platformExchangeName, platformCreatedRoutingKey, platform);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(1);

        Platform result = platformRepo.findOne(platform.getId());
        log.info("platform.id = " + platform.getId());
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

        // Sleep to make sure that the platform has been updated in the repo before querying
        TimeUnit.SECONDS.sleep(1);

        Platform result = platformRepo.findOne(platform.getId());
        assertEquals(newName, result.getName());
    }

    @Test
    public void PlatformDeletedTest() throws Exception {

        log.info("PlatformDeletedTest started!!!");
        Platform platform = createPlatform();

        platformRepo.save(platform);

        sendPlatformMessage(platformExchangeName, platformRemovedRoutingKey, platform);

        // Sleep to make sure that the platform has been removed from the repo before querying
        TimeUnit.SECONDS.sleep(1);

        Platform result = platformRepo.findOne(platform.getId());
        assertEquals(null, result);

	}

    @Test
    public void SensorCreatedTest() throws Exception {

        log.info("SensorCreatedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource resource1 = createResource();
        CoreResource resource2 = createResource();
        CoreResource resource3 = createResource();
        CoreResource resource4 = createResource();
        CoreResource resource5 = createResource();

        resource1.setType(CoreResourceType.ACTUATOR);
        resource2.setType(CoreResourceType.SERVICE);
        resource3.setType(CoreResourceType.DEVICE);
        resource4.setType(CoreResourceType.STATIONARY_SENSOR);
        resource5.setType(CoreResourceType.MOBILE_SENSOR);

        CoreResourceRegisteredOrModifiedEventPayload regMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        ArrayList<CoreResource> resources = new ArrayList<>();
        resources.add(resource1);
        resources.add(resource2);
        resources.add(resource3);
        resources.add(resource4);
        resources.add(resource5);
        regMessage.setPlatformId(platform.getId());
        regMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceCreatedRoutingKey, regMessage);

        // Sleep to make sure that the resource has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(resource1.getId());
        assertEquals(platformUrl + "rap/Actuators('" + resource1.getId()
               + "')", result.getResourceUrl());
        assertEquals(0, (long) result.getViewsInDefinedInterval());
        assertEquals((long) subIntervalDuration,  result.getViewsInSubIntervals().get(0).getEndOfInterval().getTime() -
                result.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resource2.getId());
        assertEquals(platformUrl + "rap/Services('" + resource2.getId()
               + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resource3.getId());
        assertEquals(platformUrl + "rap/Sensors('" + resource3.getId()
               + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());


        result = resourceRepo.findOne(resource4.getId());
        assertEquals(platformUrl + "rap/Sensors('" + resource4.getId()
               + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resource5.getId());
        assertEquals(platformUrl + "rap/Sensors('" + resource5.getId()
               + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

	}

    @Test
    public void SensorUpdatedTest() throws Exception {

        log.info("SensorUpdatedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource coreResource1 = createResource();
        CramResource cramResource1 = new CramResource(coreResource1);
        resourceRepo.save(cramResource1);
        CoreResource coreResource2 = createResource();
        CramResource cramResource2 = new CramResource(coreResource2);
        resourceRepo.save(cramResource2);

        String resourceNewName = "name3";

        CoreResource newCoreResource1 = new CoreResource();
        newCoreResource1.setId(coreResource1.getId());
        newCoreResource1.setName(resourceNewName);
        newCoreResource1.setInterworkingServiceURL(coreResource1.getInterworkingServiceURL());
        newCoreResource1.setType(CoreResourceType.ACTUATOR);

        CoreResource newCoreResource2 = new CoreResource();
        newCoreResource2.setId(coreResource2.getId());
        newCoreResource2.setName(resourceNewName);
        newCoreResource2.setInterworkingServiceURL(coreResource2.getInterworkingServiceURL());
        newCoreResource2.setType(CoreResourceType.ACTUATOR);

        CoreResourceRegisteredOrModifiedEventPayload updMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        ArrayList<CoreResource> resources = new ArrayList<>();
        resources.add(newCoreResource1);
        resources.add(newCoreResource2);
        updMessage.setPlatformId(platform.getId());
        updMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceUpdatedRoutingKey, updMessage);


        // Sleep to make sure that the resource has been updated in the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(coreResource1.getId());
        assertEquals(resourceNewName, result.getName());
        assertEquals(platform.getId(), result.getPlatformId());

        result = resourceRepo.findOne(coreResource2.getId());
        assertEquals(resourceNewName, result.getName());
        assertEquals(platform.getId(), result.getPlatformId());

	}

    @Test
    public void SensorDeletedTest() throws Exception {

        log.info("SensorDeletedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource coreResource1 = createResource();
        CramResource resource1 = new CramResource(coreResource1);
        resourceRepo.save(resource1);
        CoreResource coreResource2 = createResource();
        CramResource resource2 = new CramResource(coreResource2);
        resourceRepo.save(resource2);
        ArrayList<String> resources = new ArrayList<>();
        resources.add(resource1.getId());
        resources.add(resource2.getId());

        sendResourceDeleteMessage(resourceExchangeName, resourceRemovedRoutingKey, resources);

        // Sleep to make sure that the resource has been deleted from the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(resource1.getId());
        assertEquals(null, result);
        result = resourceRepo.findOne(resource2.getId());
        assertEquals(null, result);

    }


    private Platform createPlatform() {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);
        List<String> descriptions = new ArrayList<>();
        List<InterworkingService> interworkingServices = new ArrayList<>();
        InterworkingService interworkingService = new InterworkingService();

        descriptions.add("platform_description");
        interworkingService.setUrl(platformUrl);
        interworkingService.setInformationModelId("platform_description");
        interworkingServices.add(interworkingService);

        platform.setId(platformId);
        platform.setName(name);
        platform.setDescription(descriptions);
        platform.setInterworkingServices(interworkingServices);

        return platform;
    }

    private CoreResource createResource() {

        CoreResource resource = new CoreResource();
        String resourceId = Integer.toString(rand.nextInt(50000));
        resource.setId(resourceId);
        resource.setName("name1");

        List<String> descriptions = Arrays.asList("comment1", "comment2");
        resource.setDescription(descriptions);
        resource.setInterworkingServiceURL(platformUrl);

        try {
            resource.setPolicySpecifier(new SingleTokenAccessPolicySpecifier(
                    AccessPolicyType.PUBLIC,
                    new HashMap<>()));
        } catch (InvalidArgumentsException e) {
            e.printStackTrace();
            fail("Could not create IAccessPolicy");
        }

        return resource;
    }

    private void sendPlatformMessage (String exchange, String key, Platform platform) throws Exception {

        rabbitTemplate.convertAndSend(exchange, key, platform,
            m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                 });
    }

    private void sendResourceMessage (String exchange, String key, CoreResourceRegisteredOrModifiedEventPayload resources) throws Exception {

        rabbitTemplate.convertAndSend(exchange, key, resources,
            m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                 });
    }

    private void sendResourceDeleteMessage (String exchange, String key, List<String> resources) throws Exception {

        rabbitTemplate.convertAndSend(exchange, key, resources,
            m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                 });
    }

}
