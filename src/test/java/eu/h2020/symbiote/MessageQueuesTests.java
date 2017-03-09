// package eu.h2020.symbiote;


// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import org.junit.Test;
// import org.junit.Before;

// import org.junit.runner.RunWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

// import java.net.URL;
// import com.google.gson.Gson;

// import com.rabbitmq.client.Channel;
// import com.rabbitmq.client.Connection;
// import com.rabbitmq.client.ConnectionFactory;

// import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.amqp.core.MessageProperties;
// import org.springframework.amqp.core.MessagePropertiesBuilder;
// import org.springframework.amqp.core.Message;
// import org.springframework.amqp.core.MessageBuilder;
// import org.springframework.amqp.core.MessageDeliveryMode;

// import java.io.IOException;
// import java.util.concurrent.TimeoutException;
// import java.util.concurrent.TimeUnit;
// import java.util.Random;
// import java.util.List;
// import java.util.Arrays;

// import eu.h2020.symbiote.repository.ResourceRepository;
// import eu.h2020.symbiote.repository.PlatformRepository;
// import eu.h2020.symbiote.core.model.*;

// import static org.junit.Assert.assertEquals;

// import org.junit.rules.ExpectedException;

// /** 
//  * This file tests the PlatformRepository and ResourceRepository
//  */

// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(classes={CoreResourceAccessMonitorApplication.class})
// @SpringBootTest({"eureka.client.enabled=false"})
// public class MessageQueuesTests {


//     private static Logger log = LoggerFactory
//                           .getLogger(MessageQueuesTests.class);
    
//     @Autowired
//     private ResourceRepository resourceRepo;
    
//     @Autowired    
//     private PlatformRepository platformRepo;
    
//     @Autowired
//     private RabbitTemplate rabbitTemplate;

//     private Random rand;

//     @Value("${rabbit.exchange.platform.name}")
//     private String platformExchangeName;
//     @Value("${rabbit.routingKey.platform.created}")
//     private String platformCreatedRoutingKey;
//     @Value("${rabbit.routingKey.platform.modified}")
//     private String platformUpdatedRoutingKey;
//     @Value("${rabbit.routingKey.platform.removed}")
//     private String platformRemovedRoutingKey;

//     @Value("${rabbit.exchange.resource.name}")
//     private String resourceExchangeName;
//     @Value("${rabbit.routingKey.resource.created}")
//     private String resourceCreatedRoutingKey;
//     @Value("${rabbit.routingKey.resource.modified}")
//     private String resourceUpdatedRoutingKey;
//     @Value("${rabbit.routingKey.resource.removed}")
//     private String resourceRemovedRoutingKey;

//     @Before
//     public void setup() throws IOException, TimeoutException {

//         rand = new Random();

//     }

//     @Test
//     public void PlatformCreatedTest() throws Exception {

//         log.info("PlatformCreatedTest started!!!");
//         Platform platform = createPlatform();

//         sendPlatformMessage(platformExchangeName, platformCreatedRoutingKey, platform);

//         // Sleep to make sure that the platform has been saved to the repo before querying
//         TimeUnit.SECONDS.sleep(3);

//         Platform result = platformRepo.findOne(platform.getPlatformId());
//         assertEquals(platform.getName(), result.getName());

//         platformRepo.delete(platform.getPlatformId());
//     }

//     @Test
//     public void PlatformUpdatedTest() throws Exception {
        
//         Platform platform = createPlatform();

//         platformRepo.save(platform);

//         String newName = "platform" + rand.nextInt(50000);
//         platform.setName(newName);

//         sendPlatformMessage(platformExchangeName, platformUpdatedRoutingKey, platform);

//         // Sleep to make sure that the platform has been saved to the repo before querying
//         TimeUnit.SECONDS.sleep(3);

//         Platform result = platformRepo.findOne(platform.getPlatformId());
//         assertEquals(newName, result.getName());

//         platformRepo.delete(platform.getPlatformId());

//     }

//     @Test
//     public void PlatformDeletedTest() throws Exception {

//         Platform platform = createPlatform();

//         platformRepo.save(platform);

//         sendPlatformMessage(platformExchangeName, platformRemovedRoutingKey, platform);

//         // Sleep to make sure that the platform has been saved to the repo before querying
//         TimeUnit.SECONDS.sleep(3);

//         Platform result = platformRepo.findOne(platform.getPlatformId());
//         assertEquals(null, result);    
// 	}

//     @Test
//     public void SensorCreatedTest() throws Exception {

//         Platform platform = createPlatform();
//         platformRepo.save(platform);

//         Location location = createLocation();

//         Resource resource = createResource(platform, location);


//         sendResourceMessage(resourceExchangeName, resourceCreatedRoutingKey, resource);

//         // Sleep to make sure that the platform has been saved to the repo before querying
//         TimeUnit.SECONDS.sleep(3);

//         Resource result = resourceRepo.findOne(resource.getId());

//         assertEquals("http://www.symbIoTe.com/rap/Sensor('" + resource.getId()
//                + "')", result.getResourceURL());   

//         platformRepo.delete(platform.getPlatformId());
//         resourceRepo.delete(resource.getId());

// 	}

//     @Test
//     public void SensorUpdatedTest() throws Exception {

//         Platform platform = createPlatform();
//         platformRepo.save(platform);

//         Location location = createLocation();

//         Resource resource = createResource(platform, location);
//         resourceRepo.save(resource);


//         String resourceNewName = "resource" + rand.nextInt(50000);
//         resource.setName(resourceNewName);        
//         sendResourceMessage(resourceExchangeName, resourceUpdatedRoutingKey, resource);


//         // Sleep to make sure that the platform has been saved to the repo before querying
//         TimeUnit.SECONDS.sleep(3);

//         Resource result = resourceRepo.findOne(resource.getId());
//         assertEquals("http://www.symbIoTe.com/rap/Sensor('" + resource.getId()
//                + "')", result.getResourceURL()); 

//         platformRepo.delete(platform.getPlatformId());
//         resourceRepo.delete(resource.getId());
// 	}

//     @Test
//     public void SensorDeletedTest() throws Exception {

//         Platform platform = createPlatform();
//         platformRepo.save(platform);

//         Location location = createLocation();

//         Resource resource = createResource(platform, location);
//         resourceRepo.save(resource);

//         sendResourceMessage(resourceExchangeName, resourceRemovedRoutingKey, resource);

//         // Sleep to make sure that the platform has been saved to the repo before querying
//         TimeUnit.SECONDS.sleep(3);

//         Resource result = resourceRepo.findOne(resource.getId());
//         assertEquals(null, result);

//         platformRepo.delete(platform.getPlatformId());
//     }


//     Platform createPlatform() {

//         Platform platform = new Platform ();
//         String platformId = Integer.toString(rand.nextInt(50));
//         String name = "platform" + rand.nextInt(50000);

//         platform.setPlatformId(platformId);
//         platform.setName(name);
//         platform.setDescription("platform_description");
//         platform.setUrl("http://www.symbIoTe.com/");
//         platform.setInformationModelId("platform_info_model");

//         return platform;
//     }

//     Location createLocation() {

//         Location location = new Location();
//         String locationId = Integer.toString(rand.nextInt(50000));
//         location.setId(locationId);
//         location.setName("location_name");
//         location.setDescription("location_description");
//         location.setLatitude(0.1);
//         location.setLongitude(0.2);
//         location.setAltitude(0.3);

//         return location;
//     }

//     Resource createResource(Platform platform, Location location) {

//         Resource resource = new Resource();
//         String resourceId = Integer.toString(rand.nextInt(50000));
//         String resourceName = "sensor" + rand.nextInt(50000);
//         List<String> observedProperties = Arrays.asList("air", "temp");
//         resource.setId(resourceId);
//         resource.setName(resourceName);
//         resource.setOwner("OpenIoT");
//         resource.setDescription("Temperature Sensor");
//         resource.setObservedProperties(observedProperties);
//         resource.setResourceURL("http://www.symbIoTe.com/sensor1");
//         resource.setLocation(location);
//         resource.setFeatureOfInterest("Nothing");
//         resource.setPlatformId(platform.getPlatformId());

//         return resource;
//     }

//     void sendPlatformMessage (String exchange, String key, Platform object) throws Exception {

//         Gson gson = new Gson();
//         String objectInJson = gson.toJson(object);

//         MessageProperties props = MessagePropertiesBuilder.newInstance()
//             .setContentType("application/json")
//             .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
//             .build();
//         Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
//             .andProperties(props)
//             .build();

//         rabbitTemplate.send(exchange, key, message);
//     }

//     void sendResourceMessage (String exchange, String key, Resource object) throws Exception {

//         Gson gson = new Gson();
//         String objectInJson = gson.toJson(object);

//         MessageProperties props = MessagePropertiesBuilder.newInstance()
//             .setContentType("application/json")
//             .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
//             .build();
//         Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
//             .andProperties(props)
//             .build();

//         rabbitTemplate.send(exchange, key, message);
//     }
// }
