package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulPushesMessageInfo;
import eu.h2020.symbiote.core.internal.cram.NotificationMessageSecured;
import eu.h2020.symbiote.cram.managers.AuthorizationManager;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.NextPopularityUpdate;
import eu.h2020.symbiote.cram.model.SubIntervalViews;
import eu.h2020.symbiote.cram.model.authorization.AuthorizationResult;
import eu.h2020.symbiote.cram.repository.CramPersistentVariablesRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;

import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Created by vasgl on 7/3/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.sleuth.enabled=false",
        "symbiote.testaam" + ".url=http://localhost:8080",
        "aam.environment.coreInterfaceAddress=http://localhost:8080",
        "platform.aam.url=http://localhost:8080",
        "subIntervalDuration=P0-0-0T0:0:0.2",
        "intervalDuration=P0-0-0T0:0:0.4",
        "informSearchInterval=P0-0-0T1:0:0",
        "symbiote.core.cram.database=symbiote-core-cram-database-rasut",
        "rabbit.queueName.cram.getResourceUrls=cramGetResourceUrls-rasut",
        "rabbit.routingKey.cram.getResourceUrls=symbIoTe.CoreResourceAccessMonitor.coreAPI.get_resource_urls-rasut",
        "rabbit.queueName.cram.accessNotifications=accessNotifications-rasut",
        "rabbit.routingKey.cram.accessNotifications=symbIoTe.CoreResourceAccessMonitor.coreAPI.accessNotifications-rasut",
        "rabbit.queueName.search.popularityUpdates=symbIoTe-search-popularityUpdatesReceived-rasut"})
@ContextConfiguration
@Configuration
@ComponentScan
@EnableAutoConfiguration
@ActiveProfiles("test")
public class ResourceAccessStatsUpdaterTests {


    private static final Logger log = LoggerFactory
            .getLogger(ResourceAccessStatsUpdaterTests.class);

    @Autowired
    private ResourceRepository resourceRepo;

    @Autowired
    private CramPersistentVariablesRepository cramPersistentVariablesRepository;

    @Autowired
    @Qualifier("subIntervalDuration")
    private Long subIntervalDuration;

    @Autowired
    @Qualifier("intervalDuration")
    private Long intervalDuration;

    @Autowired
    @Qualifier("noSubIntervals")
    private Long noSubIntervals;

    @Autowired
    private AccessNotificationListener accessNotificationListener;

    @Autowired
    private AuthorizationManager authorizationManager;

    @Autowired
    private ResourceAccessStatsUpdater resourceAccessStatsUpdater;

    @Value("${rabbit.exchange.cram.name}")
    private String cramExchangeName;

    @Value("${rabbit.routingKey.cram.accessNotifications}")
    private String cramAccessNotificationsRoutingKey;

    @Value("${platform.aam.url}")
    private String platformAAMUrl;

    private String resourceUrl;

    // Execute the Setup method before the test.
    @Before
    public void setUp() {
        resourceAccessStatsUpdater.cancelTimer();

        List<String> observedProperties = Arrays.asList("temp", "air");
        resourceUrl = platformAAMUrl + "/rap";

        CramResource resource1 = new CramResource();
        resource1.setId("sensor_id_rasut");
        resource1.setInterworkingServiceURL(platformAAMUrl);
        resource1.setResourceUrl(resourceUrl);
        resource1.setViewsInDefinedInterval(0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<>();
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(2000), 0);
        subIntervals.add(subInterval1);
        resource1.setViewsInSubIntervals(subIntervals);

        CramResource resource2 = new CramResource();
        resource2.setId("sensor_id2_rasut");
        resource2.setInterworkingServiceURL(platformAAMUrl);
        resource2.setResourceUrl(resourceUrl);
        resource2.setViewsInDefinedInterval(0);
        resource2.setViewsInSubIntervals(subIntervals);

        resourceRepo.save(resource1);
        resourceRepo.save(resource2);

        doAnswer(new Answer<AuthorizationResult>() {
            @Override
            public AuthorizationResult answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                CramResource cramResource = CramResource.class.cast(args[0]);

                if (!cramResource.getId().equals("sensor_id_invalid_rasut"))
                    return new AuthorizationResult("Validated", true);
                else
                    return new AuthorizationResult("Do not own the resource", false);
            }
        }).when(authorizationManager).checkNotificationSecured(any(), any());

    }

    @After
    public void clearSetup() {
        resourceRepo.deleteAll();
        cramPersistentVariablesRepository.deleteAll();
        accessNotificationListener.setScheduledUpdateOngoing(false);
        accessNotificationListener.getNotificationMessageList().clear();
        resourceAccessStatsUpdater.cancelTimer();
    }

    @Test
    public void testTimerWithEmptySuccessfulAttemptsMessageList() throws Exception {

        resourceAccessStatsUpdater.startTimer();

        TimeUnit.MILLISECONDS.sleep( (long) (1.2 * subIntervalDuration));

        CramResource cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        cramResource = resourceRepo.findOne("sensor_id2_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());

        TimeUnit.MILLISECONDS.sleep( (long) (1.2 * intervalDuration));

        cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals((long) noSubIntervals, (long) cramResource.getViewsInSubIntervals().size());
        cramResource = resourceRepo.findOne("sensor_id2_rasut");
        assertEquals((long) noSubIntervals, (long) cramResource.getViewsInSubIntervals().size());

    }

    @Test
    public void testScheduledUpdateOngoing() throws Exception {

        resourceAccessStatsUpdater.startTimer();

        CramResource cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().size());

        accessNotificationListener.setScheduledUpdateOngoing(false);
        assertEquals(false, accessNotificationListener.getScheduledUpdateOngoing());

        // Sleep for one update
        TimeUnit.MILLISECONDS.sleep( (long) (1.3 * subIntervalDuration));

        cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());

        assertEquals(false, accessNotificationListener.getScheduledUpdateOngoing());
    }

    @Test
    public void testTimerWithNonEmptySuccessfulAttemptsMessageList() throws Exception {

        accessNotificationListener.getNotificationMessageList().add(createSuccessfulAttemptsMessage(false));
        assertEquals(1, accessNotificationListener.getNotificationMessageList().size());

        resourceAccessStatsUpdater.setNextPopularityUpdate(new NextPopularityUpdate(subIntervalDuration));
        resourceAccessStatsUpdater.startTimer();

        TimeUnit.MILLISECONDS.sleep( (long) (1.5 * subIntervalDuration));

        CramResource cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(4, (int) cramResource.getViewsInDefinedInterval()); // The 1st SubInterval is removed

        cramResource = resourceRepo.findOne("sensor_id2_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(4, (int) cramResource.getViewsInDefinedInterval()); // The 1st SubInterval is removed

        assertEquals(0, accessNotificationListener.getNotificationMessageList().size());
    }

    @Test
    public void testTimerWithNonEmptySuccessfulAttemptsMessageListAndInvalidResource() throws Exception {

        CramResource invalidResource = new CramResource();
        invalidResource.setId("sensor_id_invalid_rasut");
        invalidResource.setInterworkingServiceURL(platformAAMUrl);
        invalidResource.setResourceUrl(resourceUrl);
        invalidResource.setViewsInDefinedInterval(0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<>();
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(2000), 0);
        subIntervals.add(subInterval1);
        invalidResource.setViewsInSubIntervals(subIntervals);
        resourceRepo.save(invalidResource);

        accessNotificationListener.getNotificationMessageList().add(createSuccessfulAttemptsMessage(true));
        assertEquals(1, accessNotificationListener.getNotificationMessageList().size());

        resourceAccessStatsUpdater.setNextPopularityUpdate(new NextPopularityUpdate(subIntervalDuration));
        resourceAccessStatsUpdater.startTimer();

        TimeUnit.MILLISECONDS.sleep( (long) (1.5 * subIntervalDuration));

        CramResource cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(4, (int) cramResource.getViewsInDefinedInterval()); // The 1st SubInterval is removed

        cramResource = resourceRepo.findOne("sensor_id2_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(4, (int) cramResource.getViewsInDefinedInterval()); // The 1st SubInterval is removed

        cramResource = resourceRepo.findOne("sensor_id_invalid_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (int) cramResource.getViewsInDefinedInterval()); // The 1st SubInterval is removed

        assertEquals(0, accessNotificationListener.getNotificationMessageList().size());
    }

    private NotificationMessageSecured createSuccessfulAttemptsMessage(boolean invalidResource) {

        ArrayList<Date> dateList = new ArrayList<>();
        Date futureDate = new Date();
        futureDate.setTime(futureDate.getTime() + 1000000);
        dateList.add(new Date(1000));
        dateList.add(new Date(1500));
        dateList.add(futureDate);

        SuccessfulAccessMessageInfo successfulAttempts1 = new SuccessfulAccessMessageInfo();
        successfulAttempts1.setSymbIoTeId("sensor_id_rasut");
        successfulAttempts1.setTimestamps(dateList);

        SuccessfulAccessMessageInfo successfulAttempts2 = new SuccessfulAccessMessageInfo();
        successfulAttempts2.setSymbIoTeId("sensor_id2_rasut");
        successfulAttempts2.setTimestamps(dateList);

        SuccessfulPushesMessageInfo successfulPushes1 = new SuccessfulPushesMessageInfo();
        successfulPushes1.setSymbIoTeId("sensor_id_rasut");
        successfulPushes1.setTimestamps(dateList);

        SuccessfulPushesMessageInfo successfulPushes2 = new SuccessfulPushesMessageInfo();
        successfulPushes2.setSymbIoTeId("sensor_id2_rasut");
        successfulPushes2.setTimestamps(dateList);

        NotificationMessage notificationMessage = new NotificationMessage();
        notificationMessage.addSuccessfulAttempt(successfulAttempts1);
        notificationMessage.addSuccessfulAttempt(successfulAttempts2);
        notificationMessage.addSuccessfulPush(successfulPushes1);
        notificationMessage.addSuccessfulPush(successfulPushes2);

        if (invalidResource) {
            SuccessfulAccessMessageInfo successfulAttempts3 = new SuccessfulAccessMessageInfo();
            successfulAttempts3.setSymbIoTeId("sensor_id_invalid_rasut");
            successfulAttempts3.setTimestamps(dateList);

            SuccessfulPushesMessageInfo successfulPushes3 = new SuccessfulPushesMessageInfo();
            successfulPushes3.setSymbIoTeId("sensor_id_invalid_rasut");
            successfulPushes3.setTimestamps(dateList);

            notificationMessage.addSuccessfulAttempt(successfulAttempts3);
            notificationMessage.addSuccessfulPush(successfulPushes3);
        }

        NotificationMessageSecured notificationMessageSecured = new NotificationMessageSecured();
        notificationMessageSecured.setBody(notificationMessage);

        return notificationMessageSecured;
    }

}

