package eu.h2020.symbiote.messaging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.core.ExchangeTypes;

import java.util.ArrayList;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

import org.json.simple.JSONObject;

import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.SensorRepository;
import eu.h2020.symbiote.model.Sensor;
import eu.h2020.symbiote.model.Platform;

@Component
public class RpcServer {

    private static Log log = LogFactory.getLog(RpcServer.class);

    private static PlatformRepository platformRepository;

    private static SensorRepository sensorRepository;

    @Autowired
    public RpcServer(PlatformRepository platformRepository, SensorRepository sensorRepository) {
    	
    	Assert.notNull(platformRepository,"Platform repository can not be null!");
    	this.platformRepository = platformRepository;
    	
    	Assert.notNull(sensorRepository,"Sensor repository can not be null!");
    	this.sensorRepository = sensorRepository;
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-coreAPI-get_resource_urls", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.CoreResourceAccessMonitor", ignoreDeclarationExceptions = "true", type = ExchangeTypes.DIRECT),
        key = "symbIoTe.CoreResourceAccessMonitor.coreAPI.get_resource_urls")
    )
    public JSONObject getResourceUrls(JSONObject resourceIdList) {

        log.info("CRAM received a request for the following ids: " + resourceIdList);

        ArrayList<String> array = (ArrayList<String>)resourceIdList.get("idList");
        Iterator<String> iterator = array.iterator();
        JSONObject ids = new JSONObject();

        while (iterator.hasNext()) {
            Sensor sensor = sensorRepository.findOne(iterator.next());
            if (sensor != null){

                URL url = sensor.getResourceURL();
                ids.put(sensor.getId(), url.toString());
                log.info("AccessController found a resource with id " + sensor.getId() +
                     " and url " + url.toString());
            }
            else {
                log.info("The resource with id " + sensor.getId() +
                     " was not found");
            }
        }

        return ids;
    }
}