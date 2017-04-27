package eu.h2020.symbiote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;
import org.junit.Before;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate.RabbitConverterFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.io.InputStream;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.io.IOException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.repository.ResourceRepository;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.Location;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.internal.ResourceUrlsRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, 
                properties = {"eureka.client.enabled=false", 
                              "spring.sleuth.enabled=false",
                              "platform.aam.url=http://localhost:8033"})
@ContextConfiguration(locations = {"classpath:test-properties.xml" })
@Configuration
@ComponentScan
@EnableAutoConfiguration
public class CoreResourceAccessMonitorApplicationTests {


    private static final Logger log = LoggerFactory
                        .getLogger(CoreResourceAccessMonitorApplicationTests.class);
    
    @Autowired    
    private AsyncRabbitTemplate asyncRabbitTemplate;

    @Autowired
    private ResourceRepository resourceRepo;
    
    @Autowired    
    private PlatformRepository platformRepo;

    @Value("${rabbit.exchange.cram.name}")
    private String cramExchangeName;

    @Autowired
    private CoreAndPlatformAAMDummyServer coreAndPlatformAAMDummyServer;

    @Value("${rabbit.routingKey.cram.getResourceUrls}")
    private String cramGetResourceUrlsRoutingKey;

    @Value("${platform.aam.url}")
    private String platformAAMUrl;

    // Execute the Setup method before the test.    
    @Before    
    public void setUp() throws Exception {

        if (coreAndPlatformAAMDummyServer == null)
            log.info("coreAndPlatformAAMDummyServer NOT created");
        else
            log.info("coreAndPlatformAAMDummyServer created");

        List<String> observedProperties = Arrays.asList("temp", "air");
        
        Location location = new Location();
        location.setId("location_id");
        location.setName("location_name");
        location.setDescription("location_description");
        location.setLatitude(0.1);
        location.setLongitude(0.2);
        location.setAltitude(0.3);

        Platform platform = new Platform ();
        platform.setPlatformId("platform_id");
        platform.setName("platform_name");
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");

        Resource resource1 = new Resource();
        resource1.setId("sensor_id");
        // resource1.setName("Sensor1");
        // resource1.setOwner("OpenIoT");
        // resource1.setDescription("Temperature Sensor");
        // resource1.setObservedProperties(observedProperties);
        // resource1.setResourceURL("http://www.symbIoTe.com/sensor1");
        // resource1.setLocation(location);
        // resource1.setFeatureOfInterest("Nothing");
        // resource1.setPlatformId("platform_id");
        resource1.setInterworkingServiceURL(platformAAMUrl);


        Resource resource2 = new Resource();
        resource2.setId("sensor_id2");
        // resource2.setName("Sensor2");
        // resource2.setOwner("OpenIoT");
        // resource2.setDescription("Temperature Sensor");
        // resource2.setObservedProperties(observedProperties);
        // resource2.setResourceURL("http://www.symbIoTe.com/sensor2");
        // resource2.setLocation(location);
        // resource2.setFeatureOfInterest("Nothing");
        // resource2.setPlatformId("platform_id");
        resource2.setInterworkingServiceURL(platformAAMUrl);

        platformRepo.save(platform);
        resourceRepo.save(resource1);
        resourceRepo.save(resource2);
    }
    
    @Test    
    public void testGetResourcesUrlsWithValidToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
        final String ALIAS = "mytest";

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        try{
            KeyStore ks = KeyStore.getInstance("JKS");
            InputStream readStream = new FileInputStream("./src/test/resources/certificates/mytest.jks");// Use file stream to load from file system or class.getResourceAsStream to load from classpath
            ks.load(readStream, "password".toCharArray());
            Key key = ks.getKey(ALIAS, "password".toCharArray());
            readStream.close();

            String tokenString=  Jwts.builder()
                .setSubject("test1")
                .setExpiration(DateUtil.addDays(new Date(), 1))
                .claim("name", "test2")
                .signWith(SignatureAlgorithm.RS512, key)
                .compact();
            query.setToken(tokenString);

        } 
        catch(Exception e){
            log.info("Exception thrown");
        }


        log.info("Before sending the message");

        RabbitConverterFuture<Map<String, String>> future = asyncRabbitTemplate.convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        log.info("After sending the message");

        future.addCallback(new ListenableFutureCallback<Map<String, String>>() {

            @Override
            public void onSuccess(Map<String, String> result) {

                log.info("Successully received resource urls: " + result);
                resultRef.set(result);

            }

            @Override
            public void onFailure(Throwable ex) {
                fail("Accessed the element which does not exist");
            }

        });

        while(!future.isDone())
            TimeUnit.SECONDS.sleep(1);

        assertEquals(platformAAMUrl, resultRef.get().get("sensor_id"));
        assertEquals(platformAAMUrl, resultRef.get().get("sensor_id2"));

        platformRepo.delete("platform_id");
        resourceRepo.delete("sensor_id");
        resourceRepo.delete("sensor_id2");
    }

    @Test    
    public void testGetResourcesUrlsWithInvalidToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
            
        query.setToken("invalidToken");
        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        log.info("Before sending the message");

        RabbitConverterFuture<Map<String, String>> future = asyncRabbitTemplate.convertSendAndReceive(cramExchangeName, cramGetResourceUrlsRoutingKey, query);

        log.info("After sending the message");

        future.addCallback(new ListenableFutureCallback<Map<String, String>>() {

            @Override
            public void onSuccess(Map<String, String> result) {

                log.info("Successully received resource urls: " + result);
                resultRef.set(result);

            }

            @Override
            public void onFailure(Throwable ex) {
                fail("Accessed the element which does not exist");
            }

        });

        while(!future.isDone())
            TimeUnit.SECONDS.sleep(1);

        assertEquals("Token could not be verified", resultRef.get().get("error"));

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
