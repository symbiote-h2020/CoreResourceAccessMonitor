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

import eu.h2020.symbiote.repository.SensorRepository;
import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.model.*;

import static org.junit.Assert.assertEquals;

/** 
 * This file tests the PlatformRepository and SensorRepository
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={CoreResourceAccessMonitorApplication.class})
@SpringBootTest({"eureka.client.enabled=false"})
public class MessageQueuesTests {


	private static Logger log = LoggerFactory
						.getLogger(MessageQueuesTests.class);

	@Autowired
	private SensorRepository sensorRepo;

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
        
        String platformId = Integer.toString(rand.nextInt(50));
		String owner = "mock_owner";
		String name = "platform" + rand.nextInt(50000);
		String type = "mock_type";
		URL plarformUrl = new URL("http://www.symbIoTe.com");
		Platform platform = new Platform(platformId, owner, name, type, plarformUrl);

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
        
        String platformId = Integer.toString(rand.nextInt(50000));
		String owner = "mock_owner";
		String name = "platform" + rand.nextInt(50000);
		String type = "mock_type";
		URL plarformUrl = new URL("http://www.symbIoTe.com");
		Platform platform = new Platform(platformId, owner, name, type, plarformUrl);

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

        String platformId = Integer.toString(rand.nextInt(50000));
		String owner = "mock_owner";
		String name = "platform" + rand.nextInt(50000);
		String type = "mock_type";
		URL plarformUrl = new URL("http://www.symbIoTe.com");
		Platform platform = new Platform(platformId, owner, name, type, plarformUrl);

		platformRepo.save(platform);

        Gson gson = new Gson();
		String objectInJson = gson.toJson(platform.getId());

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

        String platformId = Integer.toString(rand.nextInt(50000));
		String platformOwner = "mock_owner";
		String platformName = "platform" + rand.nextInt(50000);
		String platformType = "mock_type";
		URL plarformUrl = new URL("http://www.symbIoTe.com");
		Platform platform = new Platform(platformId, platformOwner, platformName, platformType, plarformUrl);

        String locationId = Integer.toString(rand.nextInt(50000));
        String locationName = "Paris";
        String locationDescription = "mock_description";
        GeoJsonPoint locationPoint = new GeoJsonPoint(0.1, 0.1);
		Double locationAltitude = 0.1;
		Location location = new Location(locationId, locationName, locationDescription, locationPoint, locationAltitude);

        String sensorId = Integer.toString(rand.nextInt(50000));
		String sensorName = "sensor" + rand.nextInt(50000);
		String sensorOwner = "mock_sensor_owner";
		String sensorDescription = "mock_description";
		List<String> sensorObservedProperties = Arrays.asList("air", "temp");
		URL sensorUrl = new URL("http://www.symbIoTe.com/" + sensorName);
		Sensor sensor = new Sensor(sensorId, sensorName, sensorOwner, sensorDescription, location, sensorObservedProperties, platform, sensorUrl);

        Gson gson = new Gson();
		String objectInJson = gson.toJson(sensor);

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

		Sensor result = sensorRepo.findOne(sensorId);
		assertEquals(result.getName(), sensorName);
	}

	@Test
	public void SensorUpdatedTest() throws Exception {

        String platformId = Integer.toString(rand.nextInt(50000));
		String platformOwner = "mock_owner";
		String platformName = "platform" + rand.nextInt(50000);
		String platformType = "mock_type";
		URL plarformUrl = new URL("http://www.symbIoTe.com");
		Platform platform = new Platform(platformId, platformOwner, platformName, platformType, plarformUrl);

        String locationId = Integer.toString(rand.nextInt(50000));
        String locationName = "Paris";
        String locationDescription = "mock_description";
        GeoJsonPoint locationPoint = new GeoJsonPoint(0.1, 0.1);
		Double locationAltitude = 0.1;
		Location location = new Location(locationId, locationName, locationDescription, locationPoint, locationAltitude);

        String sensorId = Integer.toString(rand.nextInt(50000));
		String sensorName = "sensor" + rand.nextInt(50000);
		String sensorOwner = "mock_sensor_owner";
		String sensorDescription = "mock_description";
		List<String> sensorObservedProperties = Arrays.asList("air", "temp");
		URL sensorUrl = new URL("http://www.symbIoTe.com/" + sensorName);
		Sensor sensor = new Sensor(sensorId, sensorName, sensorOwner, sensorDescription, location, sensorObservedProperties, platform, sensorUrl);

		sensorRepo.save(sensor);

        Gson gson = new Gson();

        String exchangeName = "symbIoTe.resource";
        String routingKey = exchangeName + ".updated";

        String sensorNewName = "sensor" + rand.nextInt(50000);
        sensor.setName(sensorNewName);
		String objectInJson = gson.toJson(sensor);

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

		Sensor result = sensorRepo.findOne(sensorId);
		assertEquals(result.getName(), sensorNewName);
	}

	@Test
	public void SensorDeletedTest() throws Exception {

        String platformId = Integer.toString(rand.nextInt(50000));
		String platformOwner = "mock_owner";
		String platformName = "platform" + rand.nextInt(50000);
		String platformType = "mock_type";
		URL plarformUrl = new URL("http://www.symbIoTe.com");
		Platform platform = new Platform(platformId, platformOwner, platformName, platformType, plarformUrl);

        String locationId = Integer.toString(rand.nextInt(50000));
        String locationName = "Paris";
        String locationDescription = "mock_description";
        GeoJsonPoint locationPoint = new GeoJsonPoint(0.1, 0.1);
		Double locationAltitude = 0.1;
		Location location = new Location(locationId, locationName, locationDescription, locationPoint, locationAltitude);

        String sensorId = Integer.toString(rand.nextInt(50000));
		String sensorName = "sensor" + rand.nextInt(50000);
		String sensorOwner = "mock_sensor_owner";
		String sensorDescription = "mock_description";
		List<String> sensorObservedProperties = Arrays.asList("air", "temp");
		URL sensorUrl = new URL("http://www.symbIoTe.com/" + sensorName);
		Sensor sensor = new Sensor(sensorId, sensorName, sensorOwner, sensorDescription, location, sensorObservedProperties, platform, sensorUrl);

		sensorRepo.save(sensor);

        Gson gson = new Gson();
		String objectInJson = gson.toJson(sensor.getId());

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

		Sensor result = sensorRepo.findOne(sensorId);
		assertEquals(result, null);
	}

}
