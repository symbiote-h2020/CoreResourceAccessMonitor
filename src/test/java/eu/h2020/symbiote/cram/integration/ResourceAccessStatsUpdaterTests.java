package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

/**
 * Created by vasgl on 7/3/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {"eureka.client.enabled=false",
                "spring.sleuth.enabled=false",
                "symbiote.testaam" + ".url=http://localhost:8080",
                "symbiote.coreaam.url=http://localhost:8080",
                "platform.aam.url=http://localhost:8080"}
)
@ContextConfiguration
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class ResourceAccessStatsUpdaterTests {


    private static final Logger log = LoggerFactory
            .getLogger(CoreResourceAccessMonitorApplicationTests.class);

    @Autowired
    private ResourceRepository resourceRepo;

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


        CramResource resource2 = new CramResource();
        resource2.setId("sensor_id2");
        resource2.setInterworkingServiceURL(platformAAMUrl);
        resource2.setResourceUrl(resourceUrl);

        resourceRepo.save(resource1);
        resourceRepo.save(resource2);

    }

    @After
    public void clearRepos() {
        resourceRepo.deleteAll();
    }




}

