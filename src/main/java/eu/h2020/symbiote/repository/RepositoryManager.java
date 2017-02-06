package eu.h2020.symbiote.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.Resource;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.core.ExchangeTypes;

import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;

/**
 * Created by tipech on 04/10/2016.
 */
@Component
public class RepositoryManager {

    private static Log log = LogFactory.getLog(RepositoryManager.class);

    private static PlatformRepository platformRepository;

    private static ResourceRepository resourceRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository, ResourceRepository resourceRepository){
    	
    	Assert.notNull(platformRepository,"Platform repository can not be null!");
    	this.platformRepository = platformRepository;
    	
    	Assert.notNull(resourceRepository,"Sensor repository can not be null!");
    	this.resourceRepository = resourceRepository;
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-created", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.created")
    )
    public static void savePlatform(Platform deliveredObject) {

        platformRepository.save(deliveredObject);
        Gson gson = new Gson();
        String objectInJson = gson.toJson(deliveredObject);
        log.info("CRAM saved platform: " + objectInJson);
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-updated", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.updated")
    )
    public static void updatePlatform(Platform deliveredObject) {

        platformRepository.save(deliveredObject);
        Gson gson = new Gson();
        String objectInJson = gson.toJson(deliveredObject);
        log.info("CRAM saved platform: " + objectInJson);
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-deleted", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.deleted")
    )
    public static void deletePlatform(Platform deliveredObject) {

        platformRepository.delete(deliveredObject.getPlatformId());
        log.info("CRAM deleted platform: " + deliveredObject.getPlatformId());
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-created", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.created")
    )
    public static void saveResource(Resource deliveredObject) {
        Gson gson = new Gson();
        String objectInJson = gson.toJson(deliveredObject);
        log.info("CRAM received resource registration: " + objectInJson);

        deliveredObject.setResourceURL(generateResourceURL(deliveredObject));
        resourceRepository.save(deliveredObject);

        objectInJson = gson.toJson(deliveredObject);
        log.info("CRAM saved resource: " + objectInJson);
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-updated", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.updated")
    )
    public static void updateResource(Resource deliveredObject) {

        deliveredObject.setResourceURL(generateResourceURL(deliveredObject));
        resourceRepository.save(deliveredObject);

        Gson gson = new Gson();
        String objectInJson = gson.toJson(deliveredObject);
        log.info("CRAM updated resource: " + objectInJson);    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-deleted", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.deleted")
    )
    public static void deleteResource(Resource deliveredObject) {

        resourceRepository.delete(deliveredObject.getId());
    
        Gson gson = new Gson();
        String objectInJson = gson.toJson(deliveredObject);
        log.info("CRAM deleted resource: " + deliveredObject.getId());
    }


    private static String generateResourceURL (Resource resource) {


        Platform platform = platformRepository.findOne(resource.getPlatformId());

        return platform.getUrl() + "/rap/Sensor(\"" + resource.getId()
               + "\")/observations";
    }
}