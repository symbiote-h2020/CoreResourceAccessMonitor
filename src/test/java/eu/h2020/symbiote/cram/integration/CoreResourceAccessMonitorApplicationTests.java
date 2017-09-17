package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.core.internal.ResourceUrlsRequest;
import eu.h2020.symbiote.core.internal.ResourceUrlsResponse;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.InterworkingService;
import eu.h2020.symbiote.cram.CoreResourceAccessMonitorApplication;
import eu.h2020.symbiote.cram.managers.AuthorizationManager;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.NextPopularityUpdate;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.authorization.AuthorizationResult;
import eu.h2020.symbiote.cram.model.authorization.ServiceResponseResult;
import eu.h2020.symbiote.cram.repository.CramPersistentVariablesRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;

import org.apache.http.HttpStatus;

import org.mockito.InjectMocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate.RabbitConverterFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        classes = CoreResourceAccessMonitorApplication.class,
        webEnvironment = WebEnvironment.DEFINED_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.sleuth.enabled=false",
                "symbiote.testaam" + ".url=http://localhost:8080",
                "aam.environment.aamAddress=http://localhost:8080",
                "platform.aam.url=http://localhost:8080",
                "subIntervalDuration=P0-0-0T1:0:0",
                "intervalDuration=P0-0-0T3:0:0",
                "informSearchInterval=P0-0-0T1:0:0",
                "symbiote.core.cram.database=symbiote-core-cram-database-cramat",
                "rabbit.queueName.cram.getResourceUrls=cramGetResourceUrls-cramat",
                "rabbit.routingKey.cram.getResourceUrls=symbIoTe.CoreResourceAccessMonitor.coreAPI.get_resource_urls-cramat",
                "rabbit.queueName.cram.accessNotifications=accessNotifications-cramat",
                "rabbit.routingKey.cram.accessNotifications=symbIoTe.CoreResourceAccessMonitor.coreAPI.accessNotifications-cramat",
                "rabbit.queueName.search.popularityUpdates=symbIoTe-search-popularityUpdatesReceived-cramat"})
@ContextConfiguration
@Configuration
@ComponentScan
@EnableAutoConfiguration
@ActiveProfiles("test")
public class CoreResourceAccessMonitorApplicationTests {


    private static final Logger log = LoggerFactory
                        .getLogger(CoreResourceAccessMonitorApplicationTests.class);

    @Autowired
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Autowired
    private ResourceRepository resourceRepo;

    @Autowired
    private PlatformRepository platformRepo;

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
    public void setUp() throws Exception {
        resourceAccessStatsUpdater.cancelTimer();;

        List<String> labels = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        List<InterworkingService> interworkingServiceList = new ArrayList<>();

        resourceUrl = platformAAMUrl + "/rap";
        labels.add("platform_name");
        comments.add("platform_description");
        InterworkingService interworkingService = new InterworkingService();
        interworkingService.setUrl("http://www.symbIoTe.com");
        interworkingService.setInformationModelId("platform_info_model");
        interworkingServiceList.add(interworkingService);

        Platform platform = new Platform();
        platform.setId("platform_id");
        platform.setLabels(labels);
        platform.setComments(comments);
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
        resourceRepo.deleteAll();
        accessNotificationListener.setScheduledUpdateOngoing(false);
        accessNotificationListener.getNotificationMessageList().clear();
    }

    @Test
    public void testGetResourcesUrlsSuccess() throws Exception {

        doReturn(new AuthorizationResult("Validated", true)).when(authorizationManager).checkAccess(any());
        doReturn(new ServiceResponseResult("Service Response", true))
                .when(authorizationManager).generateServiceResponse();

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<>();
        final AtomicReference<ResourceUrlsResponse> resultRef = new AtomicReference<>();

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setBody(idList);

        log.info("Before sending the message");

        RabbitConverterFuture<ResourceUrlsResponse> future = asyncRabbitTemplate.convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        log.info("After sending the message");

        future.addCallback(new ListenableFutureCallback<ResourceUrlsResponse>() {

            @Override
            public void onSuccess(ResourceUrlsResponse result) {

                log.info("Successully received resource urls: " + result);
                resultRef.set(result);

            }

            @Override
            public void onFailure(Throwable ex) {
                fail("Accessed the element which does not exist");
            }

        });

        while(!future.isDone())
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals("Success!", resultRef.get().getMessage());
        assertEquals(HttpStatus.SC_OK, resultRef.get().getStatus());
        assertEquals(resourceUrl, resultRef.get().getBody().get("sensor_id"));
        assertEquals(resourceUrl, resultRef.get().getBody().get("sensor_id2"));
    }


    @Test
    public void testGetResourcesUrlsInvalidSecurityRequest() throws Exception {

        doReturn(new AuthorizationResult("Invalid", false)).when(authorizationManager).checkAccess(any());

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<>();
        final AtomicReference<ResourceUrlsResponse> resultRef = new AtomicReference<>();

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setBody(idList);

        log.info("Before sending the message");

        RabbitConverterFuture<ResourceUrlsResponse> future = asyncRabbitTemplate.convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        log.info("After sending the message");

        future.addCallback(new ListenableFutureCallback<ResourceUrlsResponse>() {

            @Override
            public void onSuccess(ResourceUrlsResponse result) {

                log.info("Successully received resource urls: " + result);
                resultRef.set(result);

            }

            @Override
            public void onFailure(Throwable ex) {
                fail("Accessed the element which does not exist");
            }

        });

        while(!future.isDone())
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals("The Security Request was NOT validated!", resultRef.get().getMessage());
        assertEquals(HttpStatus.SC_FORBIDDEN, resultRef.get().getStatus());
        assertEquals(0, resultRef.get().getBody().size());
    }


    @Test
    public void testGetResourcesUrlsServiceResponseNotCreated() throws Exception {

        doReturn(new AuthorizationResult("Validated", true)).when(authorizationManager).checkAccess(any());
        doReturn(new ServiceResponseResult("Service Response", false))
                .when(authorizationManager).generateServiceResponse();

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<>();
        final AtomicReference<ResourceUrlsResponse> resultRef = new AtomicReference<>();

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setBody(idList);

        log.info("Before sending the message");

        RabbitConverterFuture<ResourceUrlsResponse> future = asyncRabbitTemplate.convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        log.info("After sending the message");

        future.addCallback(new ListenableFutureCallback<ResourceUrlsResponse>() {

            @Override
            public void onSuccess(ResourceUrlsResponse result) {

                log.info("Successully received resource urls: " + result);
                resultRef.set(result);

            }

            @Override
            public void onFailure(Throwable ex) {
                fail("Accessed the element which does not exist");
            }

        });

        while(!future.isDone())
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals("The Service Response was NOT created successfully", resultRef.get().getMessage());
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, resultRef.get().getStatus());
        assertEquals(0, resultRef.get().getBody().size());
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

    static public class DateUtil
    {
        public static Date addDays(Date date, int days)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, days); //minus number would decrement the days
            return cal.getTime();
        }
    }
}
