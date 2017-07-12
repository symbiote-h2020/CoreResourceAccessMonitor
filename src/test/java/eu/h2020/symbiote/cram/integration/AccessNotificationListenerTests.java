package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.cram.CoreResourceAccessMonitorApplication;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SubIntervalViews;
import eu.h2020.symbiote.cram.model.SuccessfulAttempts;
import eu.h2020.symbiote.cram.model.SuccessfulAttemptsMessage;
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
        "symbiote.core.cram.database=symbiote-core-cram-database-anlt"})
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
        List<String> observedProperties = Arrays.asList("temp", "air");

        CramResource resource1 = new CramResource();
        resource1.setId("sensor_id");
        resource1.setViewsInDefinedInterval(0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<SubIntervalViews>();
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(20000), 0);
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
        cramPersistentVariablesRepository.deleteAll();
        accessNotificationListener.setScheduledUpdateOngoing(false);
        accessNotificationListener.getSuccessfulAttemptsMessageList().clear();
        resourceAccessStatsUpdater.getTimer().cancel();
        resourceAccessStatsUpdater.getTimer().purge();
    }

    @Test
    public void noUpdateTest() throws Exception{

        SuccessfulAttemptsMessage successfulAttemptsMessage = createSuccessfulAttemptsMessage();
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, successfulAttemptsMessage);

        // Sleep to make sure that message has been received
        TimeUnit.MILLISECONDS.sleep(500);

        CramResource result = resourceRepo.findOne("sensor_id");
        assertEquals(2, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(2, (int) result.getViewsInDefinedInterval());

        result = resourceRepo.findOne("sensor_id2");
        assertEquals(2, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(2, (int) result.getViewsInDefinedInterval());

        // Check that the resources were not queued
        assertEquals(0, accessNotificationListener.getSuccessfulAttemptsMessageList().size());

    }

    @Test
    public void whileUpdatingTest() throws Exception{

        SuccessfulAttemptsMessage successfulAttemptsMessage = createSuccessfulAttemptsMessage();
        accessNotificationListener.setScheduledUpdateOngoing(true);
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, successfulAttemptsMessage);

        // Sleep to make sure that message has been received
        TimeUnit.MILLISECONDS.sleep(500);

        CramResource result = resourceRepo.findOne("sensor_id");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        result = resourceRepo.findOne("sensor_id2");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        // Check that the resources were queued
        assertEquals(1, accessNotificationListener.getSuccessfulAttemptsMessageList().size());
        assertEquals(2, accessNotificationListener.getSuccessfulAttemptsMessageList().get(0)
                .getSuccessfulAttempts().size());
    }

    @Test
    public void noEmptySuccessfulAttemptsMessageList() throws Exception{

        SuccessfulAttemptsMessage successfulAttemptsMessage = createSuccessfulAttemptsMessage();
        accessNotificationListener.getSuccessfulAttemptsMessageList().add(new SuccessfulAttemptsMessage());
        rabbitTemplate.convertAndSend(cramExchangeName, cramAccessNotificationsRoutingKey, successfulAttemptsMessage);

        // Sleep to make sure that message has been received
        TimeUnit.MILLISECONDS.sleep(500);

        CramResource result = resourceRepo.findOne("sensor_id");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        result = resourceRepo.findOne("sensor_id2");
        assertEquals(0, (int) result.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (int) result.getViewsInDefinedInterval());

        // Check that the resources were queued
        assertEquals(2, accessNotificationListener.getSuccessfulAttemptsMessageList().size());
        assertEquals(0, accessNotificationListener.getSuccessfulAttemptsMessageList().get(0)
                .getSuccessfulAttempts().size());
        assertEquals(2, accessNotificationListener.getSuccessfulAttemptsMessageList().get(1)
                .getSuccessfulAttempts().size());
    }

    private SuccessfulAttemptsMessage createSuccessfulAttemptsMessage() {
        ArrayList<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date(1000));
        dateList.add(new Date(1500));
        dateList.add(new Date(20000));

        SuccessfulAttempts successfulAttempts1 = new SuccessfulAttempts();
        successfulAttempts1.setSymbioteId("sensor_id");
        successfulAttempts1.setTimestamps(dateList);

        SuccessfulAttempts successfulAttempts2 = new SuccessfulAttempts();
        successfulAttempts2.setSymbioteId("sensor_id2");
        successfulAttempts2.setTimestamps(dateList);

        SuccessfulAttemptsMessage successfulAttemptsMessage = new SuccessfulAttemptsMessage();
        successfulAttemptsMessage.addSuccessfulAttempts(successfulAttempts1);
        successfulAttemptsMessage.addSuccessfulAttempts(successfulAttempts2);

        return successfulAttemptsMessage;
    }
}
