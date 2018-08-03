package eu.h2020.symbiote.cram.integration;


import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.repository.SmartSpaceRepository;
import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


/**
 * This file tests the PlatformRepository and ResourceRepository
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MessageQueuesTests {

    private static Logger log = LoggerFactory.getLogger(MessageQueuesTests.class);

    @Autowired
    private ResourceRepository resourceRepo;

    @Autowired
    private PlatformRepository platformRepo;

    @Autowired
    private SmartSpaceRepository smartSpaceRepo;

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

    @Value("${rabbit.exchange.ssp.name}")
    private String sspExchangeName;
    @Value("${rabbit.routingKey.ssp.created}")
    private String sspCreatedRoutingKey;
    @Value("${rabbit.routingKey.ssp.modified}")
    private String sspUpdatedRoutingKey;
    @Value("${rabbit.routingKey.ssp.removed}")
    private String sspRemovedRoutingKey;

    @Value("${rabbit.exchange.resource.name}")
    private String resourceExchangeName;
    @Value("${rabbit.routingKey.resource.created}")
    private String resourceCreatedRoutingKey;
    @Value("${rabbit.routingKey.resource.modified}")
    private String resourceUpdatedRoutingKey;
    @Value("${rabbit.routingKey.resource.removed}")
    private String resourceRemovedRoutingKey;

    private String platformUrl = "http://www.platform.com/";
    private String sspUrl = "http://www.ssp.com/";

    @Before
    public void setup() {
        clearSetup();
        rand = new Random();
    }

    @After
    public void clearSetup() {
        platformRepo.deleteAll();
        smartSpaceRepo.deleteAll();
        resourceRepo.deleteAll();
        accessNotificationListener.setScheduledUpdateOngoing(false);
        accessNotificationListener.getNotificationMessageList().clear();
        resourceAccessStatsUpdater.cancelTimer();
    }

    @Test
    public void platformCreatedTest() throws Exception {

        log.info("platformCreatedTest started!!!");
        Platform platform = createPlatform();

        sendPlatformMessage(platformExchangeName, platformCreatedRoutingKey, platform);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(1);

        Platform result = platformRepo.findOne(platform.getId());
        log.info("platform.id = " + platform.getId());
        assertEquals(platform.getName(), result.getName());
    }

    @Test
    public void platformUpdatedTest() throws Exception {

        log.info("platformUpdatedTest started!!!");
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
    public void platformDeletedTest() throws Exception {

        log.info("platformDeletedTest started!!!");
        Platform platform = createPlatform();

        platformRepo.save(platform);

        sendPlatformMessage(platformExchangeName, platformRemovedRoutingKey, platform);

        // Sleep to make sure that the platform has been removed from the repo before querying
        TimeUnit.SECONDS.sleep(1);

        Platform result = platformRepo.findOne(platform.getId());
        assertNull(result);

    }

    @Test
    public void smartSpaceCreatedTest() throws Exception {

        log.info("smartSpaceCreatedTest started!!!");
        SmartSpace smartSpace = createSmartSpace();

        sendSSPMessage(sspExchangeName, sspCreatedRoutingKey, smartSpace);

        // Sleep to make sure that the SmartSpace has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(1);

        SmartSpace result = smartSpaceRepo.findOne(smartSpace.getId());
        log.info("smartSpace.id = " + smartSpace.getId());
        assertEquals(smartSpace.getName(), result.getName());
    }

    @Test
    public void smartSpaceUpdatedTest() throws Exception {

        log.info("smartSpaceUpdatedTest started!!!");
        SmartSpace smartSpace = createSmartSpace();

        smartSpaceRepo.save(smartSpace);

        String newName = "smartSpace" + rand.nextInt(50000);
        smartSpace.setName(newName);

        sendSSPMessage(sspExchangeName, sspCreatedRoutingKey, smartSpace);

        // Sleep to make sure that the Smart Space has been updated in the repo before querying
        TimeUnit.SECONDS.sleep(1);

        SmartSpace result = smartSpaceRepo.findOne(smartSpace.getId());
        assertEquals(newName, result.getName());
    }

    @Test
    public void smartSpaceDeletedTest() throws Exception {

        log.info("smartSpaceDeletedTest started!!!");
        SmartSpace smartSpace = createSmartSpace();

        smartSpaceRepo.save(smartSpace);

        sendSSPMessage(sspExchangeName, sspRemovedRoutingKey, smartSpace);

        // Sleep to make sure that the Smart Space has been removed from the repo before querying
        TimeUnit.SECONDS.sleep(1);

        SmartSpace result = smartSpaceRepo.findOne(smartSpace.getId());
        assertNull(result);
    }

    @Test
    public void platformSensorCreatedTest() throws Exception {

        log.info("platformSensorCreatedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        List<CoreResource> resources = createCoreResources(platformUrl);
        CoreResourceRegisteredOrModifiedEventPayload regMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        regMessage.setPlatformId(platform.getId());
        regMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceCreatedRoutingKey, regMessage);

        // Sleep to make sure that the resource has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(resources.get(0).getId());
        assertEquals(platformUrl + "rap/Actuators('" + resources.get(0).getId()
                + "')", result.getResourceUrl());
        assertEquals(0, (long) result.getViewsInDefinedInterval());
        assertEquals((long) subIntervalDuration, result.getViewsInSubIntervals().get(0).getEndOfInterval().getTime() -
                result.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resources.get(1).getId());
        assertEquals(platformUrl + "rap/Services('" + resources.get(1).getId()
                + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resources.get(2).getId());
        assertEquals(platformUrl + "rap/Sensors('" + resources.get(2).getId()
                + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());


        result = resourceRepo.findOne(resources.get(3).getId());
        assertEquals(platformUrl + "rap/Sensors('" + resources.get(3).getId()
                + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resources.get(4).getId());
        assertEquals(platformUrl + "rap/Sensors('" + resources.get(4).getId()
                + "')", result.getResourceUrl());
        assertEquals(platform.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

    }

    @Test
    public void platformSensorUpdatedTest() throws Exception {

        log.info("platformSensorUpdatedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource coreResource1 = createResource(platformUrl);
        CramResource cramResource1 = new CramResource(coreResource1);
        resourceRepo.save(cramResource1);
        CoreResource coreResource2 = createResource(platformUrl);
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
    public void platformSensorDeletedTest() throws Exception {

        log.info("platformSensorDeletedTest started!!!");
        Platform platform = createPlatform();
        platformRepo.save(platform);

        CoreResource coreResource1 = createResource(platformUrl);
        CramResource resource1 = new CramResource(coreResource1);
        resourceRepo.save(resource1);
        CoreResource coreResource2 = createResource(platformUrl);
        CramResource resource2 = new CramResource(coreResource2);
        resourceRepo.save(resource2);
        ArrayList<String> resources = new ArrayList<>();
        resources.add(resource1.getId());
        resources.add(resource2.getId());

        sendResourceDeleteMessage(resourceExchangeName, resourceRemovedRoutingKey, resources);

        // Sleep to make sure that the resource has been deleted from the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(resource1.getId());
        assertNull(result);
        result = resourceRepo.findOne(resource2.getId());
        assertNull(result);

    }

    @Test
    public void sspSensorCreatedTest() throws Exception {

        log.info("sspSensorCreatedTest started!!!");
        SmartSpace smartSpace = createSmartSpace();
        smartSpaceRepo.save(smartSpace);

        List<CoreResource> resources = createCoreResources(sspUrl);
        CoreResourceRegisteredOrModifiedEventPayload regMessage = new CoreResourceRegisteredOrModifiedEventPayload();
        regMessage.setPlatformId(smartSpace.getId());
        regMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceCreatedRoutingKey, regMessage);

        // Sleep to make sure that the resource has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(resources.get(0).getId());
        assertEquals(sspUrl + "rap/Actuators('" + resources.get(0).getId()
                + "')", result.getResourceUrl());
        assertEquals(0, (long) result.getViewsInDefinedInterval());
        assertEquals((long) subIntervalDuration, result.getViewsInSubIntervals().get(0).getEndOfInterval().getTime() -
                result.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(smartSpace.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resources.get(1).getId());
        assertEquals(sspUrl + "rap/Services('" + resources.get(1).getId()
                + "')", result.getResourceUrl());
        assertEquals(smartSpace.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resources.get(2).getId());
        assertEquals(sspUrl + "rap/Sensors('" + resources.get(2).getId()
                + "')", result.getResourceUrl());
        assertEquals(smartSpace.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());


        result = resourceRepo.findOne(resources.get(3).getId());
        assertEquals(sspUrl + "rap/Sensors('" + resources.get(3).getId()
                + "')", result.getResourceUrl());
        assertEquals(smartSpace.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

        result = resourceRepo.findOne(resources.get(4).getId());
        assertEquals(sspUrl + "rap/Sensors('" + resources.get(4).getId()
                + "')", result.getResourceUrl());
        assertEquals(smartSpace.getId(), result.getPlatformId());
        assertNotNull(result.getPolicySpecifier());

    }

    @Test
    public void sspSensorUpdatedTest() throws Exception {

        log.info("sspSensorUpdatedTest started!!!");
        SmartSpace smartSpace = createSmartSpace();
        smartSpaceRepo.save(smartSpace);

        CoreResource coreResource1 = createResource(sspUrl);
        CramResource cramResource1 = new CramResource(coreResource1);
        resourceRepo.save(cramResource1);
        CoreResource coreResource2 = createResource(sspUrl);
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
        updMessage.setPlatformId(smartSpace.getId());
        updMessage.setResources(resources);

        sendResourceMessage(resourceExchangeName, resourceUpdatedRoutingKey, updMessage);

        // Sleep to make sure that the resource has been updated in the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(coreResource1.getId());
        assertEquals(resourceNewName, result.getName());
        assertEquals(smartSpace.getId(), result.getPlatformId());

        result = resourceRepo.findOne(coreResource2.getId());
        assertEquals(resourceNewName, result.getName());
        assertEquals(smartSpace.getId(), result.getPlatformId());

    }

    @Test
    public void sspSensorDeletedTest() throws Exception {

        log.info("sspSensorDeletedTest started!!!");
        SmartSpace smartSpace = createSmartSpace();
        smartSpaceRepo.save(smartSpace);

        CoreResource coreResource1 = createResource(sspUrl);
        CramResource resource1 = new CramResource(coreResource1);
        resourceRepo.save(resource1);
        CoreResource coreResource2 = createResource(sspUrl);
        CramResource resource2 = new CramResource(coreResource2);
        resourceRepo.save(resource2);
        ArrayList<String> resources = new ArrayList<>();
        resources.add(resource1.getId());
        resources.add(resource2.getId());

        sendResourceDeleteMessage(resourceExchangeName, resourceRemovedRoutingKey, resources);

        // Sleep to make sure that the resource has been deleted from the repo before querying
        TimeUnit.SECONDS.sleep(1);

        CramResource result = resourceRepo.findOne(resource1.getId());
        assertNull(result);
        result = resourceRepo.findOne(resource2.getId());
        assertNull(result);

    }

    private Platform createPlatform() {

        Platform platform = new Platform();
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

    private SmartSpace createSmartSpace() {

        SmartSpace smartSpace = new SmartSpace();
        String smartSpaceId = Integer.toString(rand.nextInt(50));
        String name = "smartSpace" + rand.nextInt(50000);
        List<String> descriptions = new ArrayList<>();
        List<InterworkingService> interworkingServices = new ArrayList<>();
        InterworkingService interworkingService = new InterworkingService();

        descriptions.add("ssp_description");
        interworkingService.setUrl(sspUrl);
        interworkingService.setInformationModelId("ssp_description");
        interworkingServices.add(interworkingService);

        smartSpace.setId(smartSpaceId);
        smartSpace.setName(name);
        smartSpace.setDescription(descriptions);
        smartSpace.setInterworkingServices(interworkingServices);

        return smartSpace;
    }

    private CoreResource createResource(String url) {

        CoreResource resource = new CoreResource();
        String resourceId = Integer.toString(rand.nextInt(50000));
        resource.setId(resourceId);
        resource.setName("name1");

        List<String> descriptions = Arrays.asList("comment1", "comment2");
        resource.setDescription(descriptions);
        resource.setInterworkingServiceURL(url);

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

    private List<CoreResource> createCoreResources(String url) {
        CoreResource resource1 = createResource(url);
        CoreResource resource2 = createResource(url);
        CoreResource resource3 = createResource(url);
        CoreResource resource4 = createResource(url);
        CoreResource resource5 = createResource(url);

        resource1.setType(CoreResourceType.ACTUATOR);
        resource2.setType(CoreResourceType.SERVICE);
        resource3.setType(CoreResourceType.DEVICE);
        resource4.setType(CoreResourceType.STATIONARY_SENSOR);
        resource5.setType(CoreResourceType.MOBILE_SENSOR);

        return new ArrayList<>(Arrays.asList(resource1, resource2, resource3, resource4, resource5));
    }
    
    private void sendPlatformMessage (String exchange, String key, Platform platform) {
        rabbitTemplate.convertAndSend(exchange, key, platform,
                m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                });
    }

    private void sendSSPMessage (String exchange, String key, SmartSpace smartSpace) {
        rabbitTemplate.convertAndSend(exchange, key, smartSpace,
                m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                });
    }

    private void sendResourceMessage (String exchange, String key, CoreResourceRegisteredOrModifiedEventPayload resources) {
        rabbitTemplate.convertAndSend(exchange, key, resources,
                m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                });
    }

    private void sendResourceDeleteMessage (String exchange, String key, List<String> resources) {
        rabbitTemplate.convertAndSend(exchange, key, resources,
                m -> {
                    m.getMessageProperties().setContentType("application/json");
                    m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return m;
                });
    }

}
