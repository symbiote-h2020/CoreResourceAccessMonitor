package eu.h2020.symbiote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;
import org.junit.Before;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate.RabbitConverterFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.nio.charset.Charset;
import java.net.URL;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.io.IOException;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import eu.h2020.symbiote.repository.RepositoryManager;
import eu.h2020.symbiote.model.*;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest({"webEnvironment=WebEnvironment.RANDOM_PORT", "eureka.client.enabled=false"})
public class CoreResourceAccessMonitorApplicationTests {


	private static final Logger log = LoggerFactory
						.getLogger(CoreResourceAccessMonitorApplicationTests.class);
    private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    @Autowired
    private WebApplicationContext webApplicationContext;

	@Autowired
	private AsyncRabbitTemplate asyncRabbitTemplate;

    private MockMvc mockMvc;

	// Execute the Setup method before the test.
	@Before
	public void setUp() throws Exception {

		mockMvc = webAppContextSetup(webApplicationContext).build();

		GeoJsonPoint point = new GeoJsonPoint(0.1, 0.1);
		Location location = new Location("location_id", "location_name", "location_description", point, 0.1);
		URL plarformUrl = new URL("http://www.symbIoTe.com");
		URL sensor1Url = new URL("http://www.symbIoTe.com/sensor1");
		URL sensor2Url = new URL("http://www.symbIoTe.com/sensor2");
		List<String> observedProperties = Arrays.asList("temp", "air");
		Platform platform = new Platform ("platform_id", "platform_owner", "platform_name", "platform_type", plarformUrl);
		Sensor sensor = new Sensor("sensor_id", "Sensor1", "OpenIoT", "Temperature Sensor", location, observedProperties, platform, sensor1Url);
		Sensor sensor2 = new Sensor("sensor_id2", "Sensor2", "OpenIoT", "Temperature Sensor", location, observedProperties, platform, sensor2Url);

		RepositoryManager.savePlatform(platform);
		RepositoryManager.saveSensor(sensor);
		RepositoryManager.saveSensor(sensor2);

	}

	@Test
	public void testGetResourcesUrl() throws Exception {

		JSONObject query = new JSONObject();
		JSONArray idList = new JSONArray();
		final AtomicReference<JSONObject> resultRef = new AtomicReference<JSONObject>();

		idList.add("sensor_id");
		idList.add("sensor_id2");
		query.put("idList", idList);

        String exchangeName = "symbIoTe.CoreResourceAccessMonitor";
        String routingKey = "symbIoTe.CoreResourceAccessMonitor.coreAPI.get_resource_urls";

        log.info("Before sending the message");

        RabbitConverterFuture<JSONObject> future = asyncRabbitTemplate.convertSendAndReceive(exchangeName, routingKey, query);

        log.info("After sending the message");

	    future.addCallback(new ListenableFutureCallback<JSONObject>() {

	        @Override
	        public void onSuccess(JSONObject result) {

	        	log.info("Successully received resource urls: " + result);
	        	resultRef.set(result);

		    }

	        @Override
	        public void onFailure(Throwable ex) {
	        	fail("Accessed the element which does not exist");
	        }

		});

        TimeUnit.SECONDS.sleep(3);
		assertEquals(resultRef.get().get("sensor_id"), "http://www.symbIoTe.com/sensor1");
		assertEquals(resultRef.get().get("sensor_id2"), "http://www.symbIoTe.com/sensor2");

		// JSONObject result = null;
	 //    try {
	 //        result = future.get();
	 //        log.info("Successully received resource urls: " + result);


		// 	assertEquals(result.get("sensor_id"), "http://www.symbIoTe.com/sensor1");
		// 	assertEquals(result.get("sensor_id2"), "http://www.symbIoTe.com/sensor2");
    
		// }
	 //    catch (ExecutionException e) {
	 //        log.info("The following exception was captured: " + e);
	 //        fail();
	 //    }

	}
}
