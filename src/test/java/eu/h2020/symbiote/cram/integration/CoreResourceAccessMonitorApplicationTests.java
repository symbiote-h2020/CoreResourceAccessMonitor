package eu.h2020.symbiote.cram.integration;

import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.NextPopularityUpdate;
import eu.h2020.symbiote.cram.repository.CramPersistentVariablesRepository;
import eu.h2020.symbiote.cram.util.ResourceAccessStatsUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Test;
import org.junit.Before;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.HashMap;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.io.FileInputStream;
import java.security.*;

import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.internal.ResourceUrlsRequest;
import eu.h2020.symbiote.security.token.jwt.JWTEngine;
import eu.h2020.symbiote.security.enums.IssuingAuthorityType;

import eu.h2020.symbiote.cram.model.CramResource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT,
                properties = {"eureka.client.enabled=false",
                              "spring.sleuth.enabled=false",
                              "symbiote.testaam" + ".url=http://localhost:8080",
                              "symbiote.coreaam.url=http://localhost:8080",
                              "platform.aam.url=http://localhost:8080",
                              "subIntervalDuration=100000",
                              "intervalDuration=310000",
                              "informSearchInterval=1000000",
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

        List<String> observedProperties = Arrays.asList("temp", "air");
        resourceUrl = platformAAMUrl + "/rap";

        Platform platform = new Platform ();
        platform.setPlatformId("platform_id");
        platform.setName("platform_name");
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");

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
    public void testGetResourcesUrlsWithValidToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
        String platformTokenString;

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        try{
            final String ALIAS = "test aam keystore";
            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(new FileInputStream("./src/test/resources/TestAAM.keystore"), "1234567".toCharArray());
            Key key = ks.getKey(ALIAS, "1234567".toCharArray());
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("status", "test2");
            platformTokenString = JWTEngine.generateJWTToken("test1", attributes, ks.getCertificate(ALIAS).getPublicKey().getEncoded(), IssuingAuthorityType.PLATFORM, DateUtil.addDays(new Date(), 1).getTime(), "securityHandlerTestPlatformAAM", ks.getCertificate(ALIAS).getPublicKey(), (PrivateKey) key);

            query.setToken(platformTokenString);

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

        assertEquals(resourceUrl, resultRef.get().get("sensor_id"));
        assertEquals(resourceUrl, resultRef.get().get("sensor_id2"));

    }

    @Test
    public void testGetResourcesUrlsWithValidOfflineToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
        String platformTokenString;

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        try{
            final String ALIAS = "test aam keystore";
            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(new FileInputStream("./src/test/resources/TestAAM.keystore"), "1234567".toCharArray());
            Key key = ks.getKey(ALIAS, "1234567".toCharArray());
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("status", "VALID_OFFLINE");
            platformTokenString = JWTEngine.generateJWTToken("test1", attributes, ks.getCertificate(ALIAS).getPublicKey().getEncoded(), IssuingAuthorityType.PLATFORM, DateUtil.addDays(new Date(), -7).getTime(), "securityHandlerTestPlatformAAM", ks.getCertificate(ALIAS).getPublicKey(), (PrivateKey) key);

            query.setToken(platformTokenString);

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

        assertEquals(resourceUrl, resultRef.get().get("sensor_id"));
        assertEquals(resourceUrl, resultRef.get().get("sensor_id2"));

    }

    @Test
    public void testGetResourcesUrlsWithExpiredToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
        String platformTokenString;

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        try{
            final String ALIAS = "test aam keystore";
            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(new FileInputStream("./src/test/resources/TestAAM.keystore"), "1234567".toCharArray());
            Key key = ks.getKey(ALIAS, "1234567".toCharArray());
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("status", "EXPIRED");
            platformTokenString = JWTEngine.generateJWTToken("test1", attributes, ks.getCertificate(ALIAS).getPublicKey().getEncoded(), IssuingAuthorityType.PLATFORM, DateUtil.addDays(new Date(), -7).getTime(), "securityHandlerTestPlatformAAM", ks.getCertificate(ALIAS).getPublicKey(), (PrivateKey) key);

            query.setToken(platformTokenString);

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

        assertEquals("Token is EXPIRED", resultRef.get().get("error"));

    }

    @Test
    public void testGetResourcesUrlsWithRevokedToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
        String platformTokenString;

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        try{
            final String ALIAS = "test aam keystore";
            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(new FileInputStream("./src/test/resources/TestAAM.keystore"), "1234567".toCharArray());
            Key key = ks.getKey(ALIAS, "1234567".toCharArray());
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("status", "REVOKED");
            platformTokenString = JWTEngine.generateJWTToken("test1", attributes, ks.getCertificate(ALIAS).getPublicKey().getEncoded(), IssuingAuthorityType.PLATFORM, DateUtil.addDays(new Date(), -7).getTime(), "securityHandlerTestPlatformAAM", ks.getCertificate(ALIAS).getPublicKey(), (PrivateKey) key);

            query.setToken(platformTokenString);

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

        assertEquals("Token is REVOKED", resultRef.get().get("error"));

    }

    @Test
    public void testGetResourcesUrlsWithInvalidToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
        String platformTokenString;

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        try{
            final String ALIAS = "test aam keystore";
            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(new FileInputStream("./src/test/resources/TestAAM.keystore"), "1234567".toCharArray());
            Key key = ks.getKey(ALIAS, "1234567".toCharArray());
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("status", "INVALID");
            platformTokenString = JWTEngine.generateJWTToken("test1", attributes, ks.getCertificate(ALIAS).getPublicKey().getEncoded(), IssuingAuthorityType.PLATFORM, DateUtil.addDays(new Date(), -7).getTime(), "securityHandlerTestPlatformAAM", ks.getCertificate(ALIAS).getPublicKey(), (PrivateKey) key);

            query.setToken(platformTokenString);

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

        assertEquals("Token is INVALID", resultRef.get().get("error"));

    }

    @Test
    public void testGetResourcesUrlsWithNullToken() throws Exception {

        ResourceUrlsRequest query = new ResourceUrlsRequest();
        ArrayList<String> idList = new ArrayList<String>();
        final AtomicReference<Map<String, String>> resultRef = new AtomicReference<Map<String, String>>();
        String platformTokenString;

        idList.add("sensor_id");
        idList.add("sensor_id2");
        query.setIdList(idList);

        try{
            final String ALIAS = "test aam keystore";
            KeyStore ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(new FileInputStream("./src/test/resources/TestAAM.keystore"), "1234567".toCharArray());
            Key key = ks.getKey(ALIAS, "1234567".toCharArray());
            HashMap<String, String> attributes = new HashMap<>();
            attributes.put("status", "NULL");
            platformTokenString = JWTEngine.generateJWTToken("test1", attributes, ks.getCertificate(ALIAS).getPublicKey().getEncoded(), IssuingAuthorityType.PLATFORM, DateUtil.addDays(new Date(), -7).getTime(), "securityHandlerTestPlatformAAM", ks.getCertificate(ALIAS).getPublicKey(), (PrivateKey) key);

            query.setToken(platformTokenString);

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

        assertEquals("Token is NULL", resultRef.get().get("error"));

    }

    @Test
    public void testGetResourcesUrlsThrowingTokenValidationException() throws Exception {

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

        assertEquals("eu.h2020.symbiote.security.exceptions.aam.TokenValidationException: Token could not be validated", resultRef.get().get("error"));

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
