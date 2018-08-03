package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.core.internal.cram.ResourceUrlsRequest;
import eu.h2020.symbiote.core.internal.cram.ResourceUrlsResponse;
import eu.h2020.symbiote.cram.CoreResourceAccessMonitorApplication;
import eu.h2020.symbiote.cram.managers.AuthorizationManager;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.NextPopularityUpdate;
import eu.h2020.symbiote.cram.model.authorization.AuthorizationResult;
import eu.h2020.symbiote.cram.model.authorization.ServiceResponseResult;
import eu.h2020.symbiote.cram.repository.CramPersistentVariablesRepository;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.repository.SmartSpaceRepository;
import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;
import eu.h2020.symbiote.model.mim.InterworkingService;
import eu.h2020.symbiote.model.mim.Platform;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CoreResourceAccessMonitorApplicationTests {


    private static final Logger log = LoggerFactory.getLogger(CoreResourceAccessMonitorApplicationTests.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ResourceRepository resourceRepo;

    @Autowired
    private PlatformRepository platformRepo;

    @Autowired
    private SmartSpaceRepository smartSpaceRepo;

    @Autowired
    private CramPersistentVariablesRepository cramPersistentVariablesRepository;

    @Autowired
    private NextPopularityUpdate nextPopularityUpdate;

    @Autowired
    private ResourceAccessStatsUpdater resourceAccessStatsUpdater;

    @Autowired
    private AccessNotificationListener accessNotificationListener;

    @Autowired
    private AuthorizationManager authorizationManager;

    @InjectMocks
    private CoreResourceAccessMonitorApplication coreResourceAccessMonitorApplication;

    @Autowired
    @Qualifier("noSubIntervals")
    private Long noSubIntervals;

    @Value("${rabbit.exchange.cram.name}")
    private String cramExchangeName;

    @Value("${rabbit.routingKey.cram.getResourceUrls}")
    private String cramGetResourceUrlsRoutingKey;

    @Value("${platform.aam.url}")
    private String platformAAMUrl;

    private String resourceUrl;

    // Execute the Setup method before the test.
    @Before
    public void setUp() {
        clearSetup();

        List<String> descriptions = new ArrayList<>();
        List<InterworkingService> interworkingServiceList = new ArrayList<>();
        String platformName = "platform_name";

        resourceUrl = platformAAMUrl + "/rap";
        descriptions.add("platform_description");
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setUrl("http://www.symbIoTe.com");
        interworkingService.setInformationModelId("platform_info_model");
        interworkingServiceList.add(interworkingService);

        Platform platform = new Platform();
        platform.setId("platform_id");
        platform.setName(platformName);
        platform.setDescription(descriptions);
        platform.setInterworkingServices(interworkingServiceList);

        CramResource resource1 = new CramResource();
        resource1.setId("sensor_id");
        resource1.setInterworkingServiceURL(platformAAMUrl);
        resource1.setResourceUrl(resourceUrl);

        CramResource resource2 = new CramResource();
        resource2.setId("sensor_id2");
        resource2.setInterworkingServiceURL(platformAAMUrl);
        resource2.setResourceUrl(resourceUrl);

        platformRepo.save(platform);
        resourceRepo.save(resource1);
        resourceRepo.save(resource2);
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
    public void testGetResourcesUrlsSuccess() {

        doReturn(new AuthorizationResult("Validated", true)).when(authorizationManager)
                .checkResourceUrlRequest(any(), any());
        doReturn(new ServiceResponseResult("Service Response", true))
                .when(authorizationManager).generateServiceResponse();

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<>();

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setBody(idList);

        log.info("Before sending the message");

        ResourceUrlsResponse result = (ResourceUrlsResponse) rabbitTemplate
                .convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        assertEquals("Success!", result.getMessage());
        assertEquals(HttpStatus.SC_OK, result.getStatus());
        assertEquals(resourceUrl, result.getBody().get("sensor_id"));
        assertEquals(resourceUrl, result.getBody().get("sensor_id2"));
    }


    @Test
    public void testGetResourcesUrlsInvalidSecurityRequest() {

        doReturn(new AuthorizationResult("Invalid", false)).when(authorizationManager)
                .checkResourceUrlRequest(any(), any());
        doReturn(new ServiceResponseResult("Service Response", true))
                .when(authorizationManager).generateServiceResponse();

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<>();

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setBody(idList);

        log.info("Before sending the message");

        ResourceUrlsResponse result = (ResourceUrlsResponse) rabbitTemplate
                .convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        assertEquals("The Security Request was NOT validated for any of resource!", result.getMessage());
        assertEquals(HttpStatus.SC_FORBIDDEN, result.getStatus());
        assertEquals(0, result.getBody().size());
    }


    @Test
    public void testGetResourcesUrlsServiceResponseNotCreated() {

        doReturn(new ServiceResponseResult("Service Response", false))
                .when(authorizationManager).generateServiceResponse();

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<>();

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setBody(idList);

        log.info("Before sending the message");

        ResourceUrlsResponse result = (ResourceUrlsResponse) rabbitTemplate
                .convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        assertEquals("The Service Response was NOT created successfully", result.getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, result.getStatus());
        assertEquals(0, result.getBody().size());
    }

    @Test
    public void testGetResourcesUrlsNotAuthorizedNotFound() {

        CramResource cramResource1 = resourceRepo.findOne("sensor_id");
        CramResource cramResource2 = resourceRepo.findOne("sensor_id2");

        doReturn(new AuthorizationResult("Validated", true)).when(authorizationManager)
                .checkResourceUrlRequest(eq(cramResource1), any());
        doReturn(new AuthorizationResult("Invalid", false)).when(authorizationManager)
                .checkResourceUrlRequest(eq(cramResource2), any());
        doReturn(new ServiceResponseResult("Service Response", true))
                .when(authorizationManager).generateServiceResponse();

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<>();

        idList.add("sensor_id");
        idList.add("sensor_id2");
        idList.add("sensor_id3");
        query.setBody(idList);

        log.info("Before sending the message");

        ResourceUrlsResponse result = (ResourceUrlsResponse) rabbitTemplate
                .convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        assertEquals("Security Request not valid for all the resourceIds [sensor_id2]." +
                " Not all the resources were found [sensor_id3].", result.getMessage());
        assertEquals(HttpStatus.SC_PARTIAL_CONTENT, result.getStatus());
        assertEquals(1, result.getBody().size());
    }

    @Test
    public void NextPopularityUpdateTest() {
        NextPopularityUpdate savedNextPopularityUpdate = (NextPopularityUpdate) cramPersistentVariablesRepository.findByVariableName("NEXT_POPULARITY_UPDATE");
        log.info("savedNextPopularityUpdate = " + savedNextPopularityUpdate.getNextUpdate().getTime());
        log.info("nextPopularityUpdate = " + nextPopularityUpdate.getNextUpdate().getTime());
        assertEquals(savedNextPopularityUpdate.getNextUpdate().getTime(), nextPopularityUpdate.getNextUpdate().getTime());
        cramPersistentVariablesRepository.deleteAll();

    }

    @Test
    public void TestNoIntervals() {
        assertEquals(3, (long) noSubIntervals);
    }
}
