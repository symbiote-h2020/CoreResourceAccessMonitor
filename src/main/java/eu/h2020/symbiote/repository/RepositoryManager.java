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
* <h1>Repository Manager for saving platform and resource information</h1>
* 
* It listens to platform and resource events advertised by registry and saves
* them to the local Mongo database.
* 
* @author  Tilemachos Pechlivanoglou
* @author  Vasileios Glykantzis
* @version 1.0
* @since   2017-01-26
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

   /**
   * Spring AMQP Listener for platform registration requests. This method is invoked when a platform
   * registration is verified and advertised by the Registry. The platform object is then
   * saved to the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param platform The platform object of the newly registered platform
   */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-created", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.created")
    )
    public static void savePlatform(Platform platform) {

        platformRepository.save(platform);
        Gson gson = new Gson();
        String objectInJson = gson.toJson(platform);
        log.info("CRAM saved platform: " + objectInJson);
    }

   /**
   * Spring AMQP Listener for platform update requests. This method is invoked when a platform
   * update request is verified and advertised by the Registry. The platform object is then
   * updated in the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param platform The platform object of the updated platform
   */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-updated", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.updated")
    )
    public static void updatePlatform(Platform platform) {

        platformRepository.save(platform);
        Gson gson = new Gson();
        String objectInJson = gson.toJson(platform);
        log.info("CRAM saved platform: " + objectInJson);
    }

   /**
   * Spring AMQP Listener for platform unregistration requests. This method is invoked when a platform
   * unregistration request is verified and advertised by the Registry. The platform object is then
   * deleted from the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param platform The platform object of the platform to be deleted
   */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-deleted", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.deleted")
    )
    public static void deletePlatform(Platform platform) {

        platformRepository.delete(platform.getPlatformId());
        log.info("CRAM deleted platform: " + platform.getPlatformId());
    }

   /**
   * Spring AMQP Listener for resource registration requests. This method is invoked when a resource
   * registration is verified and advertised by the Registry. The resource object is then
   * saved to the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param resource The resource object of the newly registered resource
   */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-created", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.created")
    )
    public static void saveResource(Resource resource) {
        Gson gson = new Gson();
        String objectInJson = gson.toJson(resource);
        log.info("CRAM received resource registration: " + objectInJson);

        resource.setResourceURL(generateResourceURL(resource));
        resourceRepository.save(resource);

        objectInJson = gson.toJson(resource);
        log.info("CRAM saved resource: " + objectInJson);
    }

   /**
   * Spring AMQP Listener for resource update requests. This method is invoked when a resource
   * update request is verified and advertised by the Registry. The resource object is then
   * updated in the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param resource The resource object of the updated resource
   */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-updated", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.updated")
    )
    public static void updateResource(Resource resource) {

        resource.setResourceURL(generateResourceURL(resource));
        resourceRepository.save(resource);

        Gson gson = new Gson();
        String objectInJson = gson.toJson(resource);
        log.info("CRAM updated resource: " + objectInJson);
    }

   /**
   * Spring AMQP Listener for resource unregistration requests. This method is invoked when a resource
   * unregistration request is verified and advertised by the Registry. The resource object is then
   * deleted from the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param resource The resource object of the resource to be deleted
   */
    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-deleted", durable = "true", autoDelete = "false", exclusive = "false"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", 
                             durable = "true", autoDelete  = "false", internal = "false", 
                             type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.deleted")
    )
    public static void deleteResource(Resource resource) {

        resourceRepository.delete(resource.getId());
    
        Gson gson = new Gson();
        String objectInJson = gson.toJson(resource);
        log.info("CRAM deleted resource: " + resource.getId());
    }


    private static String generateResourceURL (Resource resource) {


        Platform platform = platformRepository.findOne(resource.getPlatformId());

        return platform.getUrl() + "/rap/Sensor('" + resource.getId()
               + "')/observation";
    }
}