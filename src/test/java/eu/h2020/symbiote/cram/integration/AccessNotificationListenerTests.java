package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.FailedAccessMessageInfo;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.cram.CoreResourceAccessMonitorApplication;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SubIntervalViews;
import eu.h2020.symbiote.cram.repository.CramPersistentVariablesRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;

import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by vasgl on 7/7/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={CoreResourceAccessMonitorApplication.class})
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.sleuth.enabled=false",
        "subIntervalDuration=100000",
        "intervalDuration=310000",
        "informSearchInterval=1000000",
        "symbiote.core.cram.database=symbiote-core-cram-database-anlt",
        "rabbit.queueName.cram.getResourceUrls=cramGetResourceUrls-anlt",
        "rabbit.routingKey.cram.getResourceUrls=symbIoTe.CoreResourceAccessMonitor.coreAPI.get_resource_urls-anlt",
        "rabbit.queueName.cram.accessNotifications=accessNotifications-anlt",
        "rabbit.routingKey.cram.accessNotifications=symbIoTe.CoreResourceAccessMonitor.coreAPI.accessNotifications-anlt",
        "rabbit.queueName.search.popularityUpdates=symbIoTe-search-popularityUpdatesReceived-anlt"})
public class AccessNotificationListenerTests {

    private static Logger log = LoggerFactory
            .getLogger(AccessNotificationListenerTests.class);

    @Autowired
    private ResourceRepository resourceRepo;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ResourceAccessStatsUpdater resourceAccessStatsUpdater;

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

    @Before
    public void setup() {
        resourceAccessStatsUpdater.cancelTimer();
        List<String> observedProperties = Arrays.asList("temp", "air");

        CramResource resource1 = new CramResource();
        resource1.setId("sensor_id");
        resource1.setViewsInDefinedInterval(0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<>();
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(2000), 0);
        subIntervals.add(subInterval1);
        resource1.setViewsInSubIntervals(subIntervals);

        CramResource resource2 = new CramResource();
        resource2.setId("sensor_id2");
        resource2.setViewsInDefinedInterval(0);
        resource2.setViewsInSubIntervals(subIntervals);

        resourceRepo.save(resource1);
        resourceRepo.save(resource2);
    }

    @After
    public void clearSetup() {
        resourceRepo.deleteAll();
        accessNotificationListener.setScheduledUpdateOngoing(false);
        accessNotificationListener.getNotificationMessageList().clear();
    }

    @Test
    public void noUpdateTest() throws Exception {

        NotificationMessage notificationMessage = createSuccessfulAttemptsMessage();
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        // Sleep to make sure that message has been received
        TimeUnit.MILLISECONDS.sleep(500);

        CramResource result = resourceRepo.findOne("sensor_id");
        assertEquals(2, result.getViewsInSubIntervals().size());
        assertEquals(2, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(3, (int) result.getViewsInDefinedInterval());

        result = resourceRepo.findOne("sensor_id2");
        assertEquals(2, result.getViewsInSubIntervals().size());
        assertEquals(2, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(3, (int) result.getViewsInDefinedInterval());

        // Check that the resources were not queued
        assertEquals(0, accessNotificationListener.getNotificationMessageList().size());

    }

    @Test
    public void whileUpdatingTest() throws Exception {

        NotificationMessage notificationMessage = createSuccessfulAttemptsMessage();
        accessNotificationListener.setScheduledUpdateOngoing(true);
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        // Sleep to make sure that message has been received
        TimeUnit.MILLISECONDS.sleep(500);

        CramResource result = resourceRepo.findOne("sensor_id");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        result = resourceRepo.findOne("sensor_id2");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        // Check that the resources were queued
        assertEquals(1, accessNotificationListener.getNotificationMessageList().size());
        assertEquals(2, accessNotificationListener.getNotificationMessageList().get(0)
                .getSuccessfulAttempts().size());
    }

    @Test
    public void nonEmptyNotificationMessageList() throws Exception {

        NotificationMessage notificationMessage = createSuccessfulAttemptsMessage();
        accessNotificationListener.getNotificationMessageList().add(new NotificationMessage());
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        // Sleep to make sure that message has been received
        TimeUnit.MILLISECONDS.sleep(500);

        CramResource result = resourceRepo.findOne("sensor_id");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        result = resourceRepo.findOne("sensor_id2");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        // Check that the resources were queued
        assertEquals(2, accessNotificationListener.getNotificationMessageList().size());
        assertEquals(0, accessNotificationListener.getNotificationMessageList().get(0)
                .getSuccessfulAttempts().size());
        assertEquals(2, accessNotificationListener.getNotificationMessageList().get(1)
                .getSuccessfulAttempts().size());
    }

    @Test
    public void checkFailedNotificationsAreDiscarded() throws Exception {
        NotificationMessage notificationMessage = new NotificationMessage();
        accessNotificationListener.setScheduledUpdateOngoing(true);

        ArrayList<Date> dateList = new ArrayList<>();
        dateList.add(new Date(1000));
        dateList.add(new Date(1500));
        dateList.add(new Date(20000));

        FailedAccessMessageInfo failedAccessMessageInfo = new FailedAccessMessageInfo("sensor_id", dateList,
                "code", "message", "appId", "issuer", "validationStatus",
                "requestParams");
        notificationMessage.addFailedAttempt(failedAccessMessageInfo);

        // Send Notification Message as created with just a FailedAccessMessageInfo
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        notificationMessage.setSuccessfulAttempts(null);
        notificationMessage.setSuccessfulPushes(null);
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        notificationMessage.setSuccessfulAttempts(new ArrayList<>());
        notificationMessage.setSuccessfulPushes(null);
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        notificationMessage.setSuccessfulAttempts(null);
        notificationMessage.setSuccessfulPushes(new ArrayList<>());
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        notificationMessage.setSuccessfulAttempts(new ArrayList<>());
        notificationMessage.setSuccessfulPushes(new ArrayList<>());
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, notificationMessage);

        // Sleep to make sure that message has been received
        TimeUnit.MILLISECONDS.sleep(500);

        // Check that the messages were not queued
        assertEquals(0, accessNotificationListener.getNotificationMessageList().size());
    }

    private NotificationMessage createSuccessfulAttemptsMessage() {
        ArrayList<Date> dateList = new ArrayList<>();
        Date futureDate = new Date();
        futureDate.setTime(futureDate.getTime() + 1000000);
        dateList.add(new Date(1000));
        dateList.add(new Date(1500));
        dateList.add(new Date(20000));
        dateList.add(futureDate);

        SuccessfulAccessMessageInfo successfulAttempts1 = new SuccessfulAccessMessageInfo();
        successfulAttempts1.setSymbIoTeId("sensor_id");
        successfulAttempts1.setTimestamps(dateList);

        SuccessfulAccessMessageInfo successfulAttempts2 = new SuccessfulAccessMessageInfo();
        successfulAttempts2.setSymbIoTeId("sensor_id2");
        successfulAttempts2.setTimestamps(dateList);

        FailedAccessMessageInfo failedAccessMessageInfo = new FailedAccessMessageInfo("sensor_id", dateList,
                "code", "message", "appId", "issuer", "validationStatus",
                "requestParams");

        NotificationMessage notificationMessage = new NotificationMessage();
        notificationMessage.addSuccessfulAttempt(successfulAttempts1);
        notificationMessage.addSuccessfulAttempt(successfulAttempts2);
        notificationMessage.addFailedAttempt(failedAccessMessageInfo);

        return notificationMessage;
    }
}
