package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulPushesMessageInfo;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SubIntervalViews;
import eu.h2020.symbiote.cram.repository.CramPersistentVariablesRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by vasgl on 7/3/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.sleuth.enabled=false",
        "symbiote.testaam" + ".url=http://localhost:8080",
        "symbiote.coreaam.url=http://localhost:8080",
        "platform.aam.url=http://localhost:8080",
        "subIntervalDuration=100",
        "intervalDuration=200",
        "informSearchInterval=1000000",
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

    }

    @After
    public void clearSetup() {
        resourceRepo.deleteAll();
        cramPersistentVariablesRepository.deleteAll();
        accessNotificationListener.setScheduledUpdateOngoing(false);
        accessNotificationListener.getNotificationMessageList().clear();
    }

    @Test
    public void testTimerWithEmptySuccessfulAttemptsMessageList() throws Exception {

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

        CramResource cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().size());

        assertEquals(false, accessNotificationListener.getScheduledUpdateOngoing());

        // Sleep for one update
        TimeUnit.MILLISECONDS.sleep( (long) (1.3 * subIntervalDuration));

        cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());

        assertEquals(false, accessNotificationListener.getScheduledUpdateOngoing());
    }

    @Test
    public void testTimerWithNonEmptySuccessfulAttemptsMessageList() throws Exception {

        accessNotificationListener.getNotificationMessageList().add(createSuccessfulAttemptsMessage());
        assertEquals(1, accessNotificationListener.getNotificationMessageList().size());
        TimeUnit.MILLISECONDS.sleep( (long) (1.2 * subIntervalDuration));

        CramResource cramResource = resourceRepo.findOne("sensor_id_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());

        cramResource = resourceRepo.findOne("sensor_id2_rasut");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());

        assertEquals(0, accessNotificationListener.getNotificationMessageList().size());

    }

    private NotificationMessage createSuccessfulAttemptsMessage() {
        ArrayList<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date(1000));
        dateList.add(new Date(1400));
        dateList.add(new Date(20000));

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

        return notificationMessage;
    }
}

