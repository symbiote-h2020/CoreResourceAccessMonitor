package eu.h2020.symbiote.cram.repository;

import eu.h2020.symbiote.cram.model.SubIntervalViews;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.core.model.internal.CoreResourceType;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.model.internal.CoreResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.h2020.symbiote.cram.exception.EntityNotFoundException;
import eu.h2020.symbiote.cram.model.CramResource;


/**
* <h1>Repository Manager for saving platform and resource information</h1>
* 
* This listens to platform and resource events advertised by registry and saves
* them to the local Mongo database.
* 
* @author  Tilemachos Pechlivanoglou <tipech@intracom-telecom.com>
* @author  Vasileios Glykantzis <vasgl@intracom-telecom.com>
* @version 1.0
* @since   2017-01-26
*/


@Component
public class RepositoryManager {

    private static Log log = LogFactory.getLog(RepositoryManager.class);

    private static PlatformRepository platformRepository;
    private static ResourceRepository resourceRepository;
    private static Long subIntervalDuration;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository, ResourceRepository resourceRepository,
                             @Qualifier("subIntervalDuration") Long subIntervalDuration){
    	
    	Assert.notNull(platformRepository,"Platform repository can not be null!");
    	this.platformRepository = platformRepository;
    	
    	Assert.notNull(resourceRepository,"Resource repository can not be null!");
    	this.resourceRepository = resourceRepository;

        Assert.notNull(subIntervalDuration,"SubIntervalDuration repository can not be null!");
        this.subIntervalDuration = subIntervalDuration;
    }

   /**
   * Spring AMQP Listener for platform registration requests. This method is invoked when a platform
   * registration is verified and advertised by the Registry. The platform object is then
   * saved to the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param platform The platform object of the newly registered platform
   */
   @RabbitListener(bindings = @QueueBinding(
           value = @Queue(value = "platformRegistration", durable = "${rabbit.exchange.platform.durable}",
                   autoDelete = "${rabbit.exchange.platform.autodelete}", exclusive = "false"),
           exchange = @Exchange(value = "${rabbit.exchange.platform.name}", ignoreDeclarationExceptions = "true",
                   durable = "${rabbit.exchange.platform.durable}", autoDelete  = "${rabbit.exchange.platform.autodelete}",
                   internal = "${rabbit.exchange.platform.internal}", type = "${rabbit.exchange.platform.type}"),
           key = "${rabbit.routingKey.platform.created}")
   )
   public static void savePlatform(Platform platform) {
       platformRepository.save(platform);
       log.info("CRAM saved platform with id: " + platform.getPlatformId());
   }

   /**
   * Spring AMQP Listener for platform update requests. This method is invoked when a platform
   * update request is verified and advertised by the Registry. The platform object is then
   * updated in the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param platform The platform object of the updated platform
   */
   @RabbitListener(bindings = @QueueBinding(
           value = @Queue(value = "platformUpdated", durable = "${rabbit.exchange.platform.durable}",
                   autoDelete = "${rabbit.exchange.platform.autodelete}", exclusive = "false"),
           exchange = @Exchange(value = "${rabbit.exchange.platform.name}", ignoreDeclarationExceptions = "true",
                   durable = "${rabbit.exchange.platform.durable}", autoDelete  = "${rabbit.exchange.platform.autodelete}",
                   internal = "${rabbit.exchange.platform.internal}", type = "${rabbit.exchange.platform.type}"),
           key = "${rabbit.routingKey.platform.modified}")
   )
   public static void updatePlatform(Platform platform) {
       try {
            if (platformRepository.findOne(platform.getPlatformId()) == null) 
                throw new EntityNotFoundException ("Received an update message for "
                    + "platform with id = " + platform.getPlatformId() + " which does not exist.");

            platformRepository.save(platform);
            log.info("CRAM updated platform with id: " + platform.getPlatformId());
       } catch (EntityNotFoundException e) {
           log.info(e);
           throw e;
       } catch (Exception e) {
           log.info(e);
       }
   }

   /**
   * Spring AMQP Listener for platform unregistration requests. This method is invoked when a platform
   * unregistration request is verified and advertised by the Registry. The platform object is then
   * deleted from the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param platform The platform object of the platform to be deleted
   */
   @RabbitListener(bindings = @QueueBinding(
           value = @Queue(value = "platformUnregistration", durable = "${rabbit.exchange.platform.durable}",
                   autoDelete = "${rabbit.exchange.platform.autodelete}", exclusive = "false"),
           exchange = @Exchange(value = "${rabbit.exchange.platform.name}", ignoreDeclarationExceptions = "true",
                   durable = "${rabbit.exchange.platform.durable}", autoDelete  = "${rabbit.exchange.platform.autodelete}",
                   internal = "${rabbit.exchange.platform.internal}", type = "${rabbit.exchange.platform.type}"),
           key = "${rabbit.routingKey.platform.removed}")
   )
   public static void deletePlatform(Platform platform) {
       try {
            if (platformRepository.findOne(platform.getPlatformId()) == null) 

                throw new EntityNotFoundException ("Received an unregistration message for "
                    + "platform with id = " + platform.getPlatformId() + " which does not exist.");

            platformRepository.delete(platform.getPlatformId());
            log.info("CRAM deleted platform with id: " + platform.getPlatformId());
       } catch (EntityNotFoundException e) {
           log.info(e);
           throw e;
       } catch (Exception e) {
           log.info(e);
       }
   }

   /**
   * Spring AMQP Listener for resource registration requests. This method is invoked when a resource
   * registration is verified and advertised by the Registry. The resource object is then
   * saved to the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param message The message of the newly registered resources
   */
   @RabbitListener(bindings = @QueueBinding(
           value = @Queue(value = "resourceRegistration", durable = "${rabbit.exchange.resource.durable}",
                   autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
           exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                   durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                   internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
           key = "${rabbit.routingKey.resource.created}")
   )
   public static void saveResource(CoreResourceRegisteredOrModifiedEventPayload message)
           throws AmqpRejectAndDontRequeueException {
       try {
            if (platformRepository.findOne(message.getPlatformId()) == null)
                throw new EntityNotFoundException ("Received a registration message"
                    + ", but the platform " + "with id = " + message.getPlatformId() 
                    + " which owns the resources does not exist.");

            for (CoreResource coreResource : message.getResources()) {
                CramResource cramResource = new CramResource(coreResource);

                cramResource.setResourceUrl(generateResourceURL(cramResource));
                cramResource.setViewsInDefinedInterval(0);

                ArrayList<SubIntervalViews>  subIntervalList = new ArrayList<>();
                Date startDate = new Date(new Date().getTime());
                Date endDate = new Date(startDate.getTime() + subIntervalDuration);
                // Todo: Change endTimestamp
                subIntervalList.add(new SubIntervalViews(startDate, endDate, 0));
                cramResource.setViewsInSubIntervals(subIntervalList);

                resourceRepository.save(cramResource);
                log.info("CRAM saved resource with id: " + cramResource.getId());
            }
       } catch (EntityNotFoundException e) {
           log.info(e);
           throw e;
       } catch (Exception e) {
           log.info(e);
       }
   }

   /**
   * Spring AMQP Listener for resource update requests. This method is invoked when a resource
   * update request is verified and advertised by the Registry. The resource object is then
   * updated in the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param message The message of the newly updated resources
   */
   @RabbitListener(bindings = @QueueBinding(
           value = @Queue(value = "resourceUpdatedd", durable = "${rabbit.exchange.resource.durable}",
                   autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
           exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                   durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                   internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
           key = "${rabbit.routingKey.resource.modified}")
   )
   public static void updatedResource(CoreResourceRegisteredOrModifiedEventPayload message)
           throws AmqpRejectAndDontRequeueException {
       try {
            if (platformRepository.findOne(message.getPlatformId()) == null)
                throw new EntityNotFoundException ("Received an update message"
                    + ", but the platform " + "with id = " + message.getPlatformId() 
                    + " which owns the resources does not exist.");
            for (CoreResource coreResource : message.getResources()) {
                if (resourceRepository.findOne(coreResource.getId()) == null)
                    throw new EntityNotFoundException ("Received an update message for "
                        + "resource with id = " + coreResource.getId() + ", but the resource does "
                        + "not exist");
                CramResource cramResource = new CramResource(coreResource);
                cramResource.setResourceUrl(generateResourceURL(cramResource));
                resourceRepository.save(cramResource);
                log.info("CRAM updated resource with id: " + cramResource.getId());
            }
       } catch (EntityNotFoundException e) {
           log.info(e);
           throw e;
       } catch (Exception e) {
           log.info(e);
       }
   }

   /**
   * Spring AMQP Listener for resource unregistration requests. This method is invoked when a resource
   * unregistration request is verified and advertised by the Registry. The resource object is then
   * deleted from the CoreResourceAccessMonitor local Mongo database.
   * 
   * @param resourcesIds List of resource Ids of the newly deleted resources
   */
   @RabbitListener(bindings = @QueueBinding(
           value = @Queue(value = "resourceUnregistration", durable = "${rabbit.exchange.resource.durable}",
                   autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
           exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                   durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                   internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
           key = "${rabbit.routingKey.resource.removed}")
   )
   public static void deleteResource(List<String> resourcesIds) {
       try {
            for (String id : resourcesIds) {

                if (resourceRepository.findOne(id) == null) 
                    throw new EntityNotFoundException ("Received an unregistration message for " 
                        + "resource with id = " + id + ", but the resource does "
                        + "not exist");

                resourceRepository.delete(id);
                log.info("CRAM deleted resource with id: " + id);
            }
       } catch (EntityNotFoundException e) {
           log.info(e);
           throw e;
       } catch (Exception e) {
           log.info(e);
       }
   }

   private static String generateResourceURL (CramResource resource) throws AmqpRejectAndDontRequeueException {
        CoreResourceType type = resource.getType();
        if (type == null)
          throw new AmqpRejectAndDontRequeueException("The resource type was not set");

        switch (type) {
            case ACTUATOR:
               return resource.getInterworkingServiceURL().replaceAll("(/rap)?/*$", "")
                      +  "/rap/Actuators('" + resource.getId() + "')";
            case SERVICE:
               return resource.getInterworkingServiceURL().replaceAll("(/rap)?/*$", "")
                      +  "/rap/Services('" + resource.getId() + "')";
            case ACTUATING_SERVICE:
               return resource.getInterworkingServiceURL().replaceAll("(/rap)?/*$", "")
                      +  "/rap/ActuatingServices('" + resource.getId() + "')";
            case STATIONARY_SENSOR:
            case MOBILE_SENSOR:
            case MOBILE_DEVICE:
            case STATIONARY_DEVICE:
            default:
               return resource.getInterworkingServiceURL().replaceAll("(/rap)?/*$", "")
                      +  "/rap/Sensors('" + resource.getId() + "')"; 
        }
   }
}