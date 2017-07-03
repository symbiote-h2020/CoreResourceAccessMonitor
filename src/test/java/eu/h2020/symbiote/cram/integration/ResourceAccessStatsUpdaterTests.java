package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SubIntervalViews;
import eu.h2020.symbiote.cram.repository.ResourceRepository;

import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;
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
        "intervalDuration=200"})
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
    @Qualifier("subIntervalDuration")
    private Long subIntervalDuration;

    @Autowired
    @Qualifier("intervalDuration")
    private Long intervalDuration;

    @Autowired
    @Qualifier("noSubIntervals")
    private Long noSubIntervals;

    @Autowired
    ResourceAccessStatsUpdater resourceAccessStatsUpdater;

    @Value("${rabbit.exchange.cram.name}")
    private String cramExchangeName;

    @Value("${rabbit.routingKey.cram.accessNotifications}")
    private String cramAccessNotificationsRoutingKey;

    @Value("${platform.aam.url}")
    private String platformAAMUrl;

    private String resourceUrl;

    // Execute the Setup method before the test.
    @Before
    public void setUp() throws Exception {

        List<String> observedProperties = Arrays.asList("temp", "air");
        resourceUrl = platformAAMUrl + "/rap";

        CramResource resource1 = new CramResource();
        resource1.setId("sensor_id");
        resource1.setInterworkingServiceURL(platformAAMUrl);
        resource1.setResourceUrl(resourceUrl);
        resource1.setViewsInDefinedInterval(0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<SubIntervalViews>();
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(2000), 0);
        subIntervals.add(subInterval1);
        resource1.setViewsInSubIntervals(subIntervals);

        CramResource resource2 = new CramResource();
        resource2.setId("sensor_id2");
        resource2.setInterworkingServiceURL(platformAAMUrl);
        resource2.setResourceUrl(resourceUrl);
        resource2.setViewsInDefinedInterval(0);
        resource2.setViewsInSubIntervals(subIntervals);

        resourceRepo.save(resource1);
        resourceRepo.save(resource2);

    }

    @After
    public void clearRepos() {
        resourceRepo.deleteAll();
    }

    @Test
    public void testTimer() throws Exception {

        TimeUnit.MILLISECONDS.sleep(subIntervalDuration);
        log.info("testTimer1");

        CramResource cramResource = resourceRepo.findOne("sensor_id");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());
        cramResource = resourceRepo.findOne("sensor_id2");
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().size());

        TimeUnit.MILLISECONDS.sleep(intervalDuration);
        log.info("testTimer2");

        cramResource = resourceRepo.findOne("sensor_id");
        assertEquals((long) noSubIntervals, (long) cramResource.getViewsInSubIntervals().size());
        cramResource = resourceRepo.findOne("sensor_id2");
        assertEquals((long) noSubIntervals, (long) cramResource.getViewsInSubIntervals().size());

        resourceAccessStatsUpdater.getTimer().cancel();
        resourceAccessStatsUpdater.getTimer().purge();
    }




}

