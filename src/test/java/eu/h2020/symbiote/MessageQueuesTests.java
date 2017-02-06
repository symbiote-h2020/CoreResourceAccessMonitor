package eu.h2020.symbiote;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;
import org.junit.Before;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.net.URL;
import com.google.gson.Gson;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

import eu.h2020.symbiote.repository.ResourceRepository;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.model.*;

import static org.junit.Assert.assertEquals;

/** 
 * This file tests the PlatformRepository and ResourceRepository
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={CoreResourceAccessMonitorApplication.class})
@SpringBootTest({"eureka.client.enabled=false"})
public class MessageQueuesTests {


    private static Logger log = LoggerFactory
                          .getLogger(MessageQueuesTests.class);
    
    @Autowired
    private ResourceRepository resourceRepo;
    
    @Autowired    
    private PlatformRepository platformRepo;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private Random rand;

    @Before
    public void setup() throws IOException, TimeoutException {

        rand = new Random();

    }

    @Test
    public void PlatformCreatedTest() throws Exception {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);

        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");


        Gson gson = new Gson();
        String objectInJson = gson.toJson(platform);

        String exchangeName = "symbIoTe.platform";
        String routingKey = exchangeName + ".created";

        MessageProperties props = MessagePropertiesBuilder.newInstance()
            .setContentType("application/json")
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
        Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
            .andProperties(props)
            .build();

        rabbitTemplate.send(exchangeName, routingKey, message);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Platform result = platformRepo.findOne(platformId);
        assertEquals(result.getName(), name);
    }

    @Test
    public void PlatformUpdatedTest() throws Exception {
        
        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);

        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");

        platformRepo.save(platform);

        Gson gson = new Gson();

        String exchangeName = "symbIoTe.platform";
        String routingKey = exchangeName + ".updated";

        String newName = "platform" + rand.nextInt(50000);
        platform.setName(newName);
        String objectInJson = gson.toJson(platform);

        MessageProperties props = MessagePropertiesBuilder.newInstance()
            .setContentType("application/json")
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
        Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
            .andProperties(props)
            .build();

        rabbitTemplate.send(exchangeName, routingKey, message);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Platform result = platformRepo.findOne(platformId);
        assertEquals(result.getName(), newName);
    }

    @Test
    public void PlatformDeletedTest() throws Exception {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);

        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");

        platformRepo.save(platform);

        Gson gson = new Gson();
        String objectInJson = gson.toJson(platform);

        String exchangeName = "symbIoTe.platform";
        String routingKey = exchangeName + ".deleted";


        MessageProperties props = MessagePropertiesBuilder.newInstance()
            .setContentType("application/json")
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
        Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
            .andProperties(props)
            .build();

        rabbitTemplate.send(exchangeName, routingKey, message);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Platform result = platformRepo.findOne(platformId);
        assertEquals(result, null);    
	}

    @Test
    public void SensorCreatedTest() throws Exception {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);
        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");
        platformRepo.save(platform);

        Location location = new Location();
        String locationId = Integer.toString(rand.nextInt(50000));
        location.setId(locationId);
        location.setName("location_name");
        location.setDescription("location_description");
        location.setLatitude(0.1);
        location.setLongitude(0.2);
        location.setAltitude(0.3);

        Resource resource = new Resource();
        String resourceId = Integer.toString(rand.nextInt(50000));
        String resourceName = "sensor" + rand.nextInt(50000);
        List<String> observedProperties = Arrays.asList("air", "temp");
        resource.setId(resourceId);
        resource.setName(resourceName);
        resource.setOwner("OpenIoT");
        resource.setDescription("Temperature Sensor");
        resource.setObservedProperties(observedProperties);
        resource.setResourceURL("http://www.symbIoTe.com/sensor1");
        resource.setLocation(location);
        resource.setFeatureOfInterest("Nothing");
        resource.setPlatformId(platformId);

        Gson gson = new Gson();        
        String objectInJson = gson.toJson(resource);

        String exchangeName = "symbIoTe.resource";
        String routingKey = exchangeName + ".created";

        MessageProperties props = MessagePropertiesBuilder.newInstance()
            .setContentType("application/json")
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
        Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
            .andProperties(props)
            .build();

        rabbitTemplate.send(exchangeName, routingKey, message);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Resource result = resourceRepo.findOne(resourceId);

        assertEquals(result.getResourceURL(), "http://www.symbIoTe.com/rap/Sensor(\"" + resourceId
               + "\")/observations");    
	}

    @Test
    public void SensorUpdatedTest() throws Exception {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);
        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");
        platformRepo.save(platform);

        Location location = new Location();
        String locationId = Integer.toString(rand.nextInt(50000));
        location.setId(locationId);
        location.setName("location_name");
        location.setDescription("location_description");
        location.setLatitude(0.1);
        location.setLongitude(0.2);
        location.setAltitude(0.3);

        Resource resource = new Resource();
        String resourceId = Integer.toString(rand.nextInt(50000));
        String resourceName = "sensor" + rand.nextInt(50000);
        List<String> observedProperties = Arrays.asList("air", "temp");
        resource.setId(resourceId);
        resource.setName(resourceName);
        resource.setOwner("OpenIoT");
        resource.setDescription("Temperature Sensor");
        resource.setObservedProperties(observedProperties);
        resource.setResourceURL("http://www.symbIoTe.com/sensor1");
        resource.setLocation(location);
        resource.setFeatureOfInterest("Nothing");
        resource.setPlatformId(platformId);

        resourceRepo.save(resource);

        Gson gson = new Gson();

        String exchangeName = "symbIoTe.resource";
        String routingKey = exchangeName + ".updated";

        String resourceNewName = "resource" + rand.nextInt(50000);
        resource.setName(resourceNewName);        
        String objectInJson = gson.toJson(resource);
        
        MessageProperties props = MessagePropertiesBuilder.newInstance()
            .setContentType("application/json")
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();

        props.setInferredArgumentType(Resource.class);

        Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
            .andProperties(props)
            .build();

        rabbitTemplate.send(exchangeName, routingKey, message);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Resource result = resourceRepo.findOne(resourceId);
        assertEquals(result.getResourceURL(), "http://www.symbIoTe.com/rap/Sensor(\"" + resourceId
               + "\")/observations");     
	}

    @Test
    public void SensorDeletedTest() throws Exception {

        Platform platform = new Platform ();
        String platformId = Integer.toString(rand.nextInt(50));
        String name = "platform" + rand.nextInt(50000);
        platform.setPlatformId(platformId);
        platform.setName(name);
        platform.setDescription("platform_description");
        platform.setUrl("http://www.symbIoTe.com");
        platform.setInformationModelId("platform_info_model");

        Location location = new Location();
        String locationId = Integer.toString(rand.nextInt(50000));
        location.setId(locationId);
        location.setName("location_name");
        location.setDescription("location_description");
        location.setLatitude(0.1);
        location.setLongitude(0.2);
        location.setAltitude(0.3);

        Resource resource = new Resource();
        String resourceId = Integer.toString(rand.nextInt(50000));
        String resourceName = "sensor" + rand.nextInt(50000);
        List<String> observedProperties = Arrays.asList("air", "temp");
        resource.setId(resourceId);
        resource.setName(resourceName);
        resource.setOwner("OpenIoT");
        resource.setDescription("Temperature Sensor");
        resource.setObservedProperties(observedProperties);
        resource.setResourceURL("http://www.symbIoTe.com/sensor1");
        resource.setLocation(location);
        resource.setFeatureOfInterest("Nothing");
        resource.setPlatformId("platform_id");

        resourceRepo.save(resource);

        Gson gson = new Gson();
        String objectInJson = gson.toJson(resource);

        String exchangeName = "symbIoTe.resource";
        String routingKey = exchangeName + ".deleted";
        
        MessageProperties props = MessagePropertiesBuilder.newInstance()
            .setContentType("application/json")
            .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
            .build();
        Message message = MessageBuilder.withBody(objectInJson.getBytes("UTF-8"))
            .andProperties(props)
            .build();

        rabbitTemplate.send(exchangeName, routingKey, message);

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.SECONDS.sleep(3);

        Resource result = resourceRepo.findOne(resourceId);
        assertEquals(result, null);
    }

}
