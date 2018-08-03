package eu.h2020.symbiote.cram.managers;

import eu.h2020.symbiote.core.internal.CoreResource;
import eu.h2020.symbiote.core.internal.CoreResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.core.internal.CoreSspResourceRegisteredOrModifiedEventPayload;
import eu.h2020.symbiote.cram.exception.EntityNotFoundException;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SubIntervalViews;
import eu.h2020.symbiote.cram.repository.PlatformRepository;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.repository.SmartSpaceRepository;
import eu.h2020.symbiote.model.mim.Platform;
import eu.h2020.symbiote.model.mim.SmartSpace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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

    private PlatformRepository platformRepository;
    private SmartSpaceRepository smartSpaceRepository;
    private ResourceRepository resourceRepository;
    private Long subIntervalDuration;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository,
                             SmartSpaceRepository smartSpaceRepository,
                             ResourceRepository resourceRepository,
                             @Qualifier("subIntervalDuration") Long subIntervalDuration){

        Assert.notNull(platformRepository,"Platform repository can not be null!");
        this.platformRepository = platformRepository;

        Assert.notNull(smartSpaceRepository,"Smart Space repository can not be null!");
        this.smartSpaceRepository = smartSpaceRepository;

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
    public void savePlatform(Platform platform) {
        platformRepository.save(platform);
        log.info("CRAM saved platform with id: " + platform.getId());
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
    public void updatePlatform(Platform platform) {
        try {
            if (platformRepository.findOne(platform.getId()) == null)
                throw new EntityNotFoundException ("Received an update message for "
                        + "platform with id = " + platform.getId() + " which does not exist.");

            platformRepository.save(platform);
            log.info("CRAM updated platform with id: " + platform.getId());
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
    public void deletePlatform(Platform platform) {
        try {
            if (platformRepository.findOne(platform.getId()) == null)

                throw new EntityNotFoundException ("Received an unregistration message for "
                        + "platform with id = " + platform.getId() + " which does not exist.");

            platformRepository.delete(platform.getId());
            log.info("CRAM deleted platform with id: " + platform.getId());
        } catch (EntityNotFoundException e) {
            log.info(e);
            throw e;
        } catch (Exception e) {
            log.info(e);
        }
    }

    /**
     * Spring AMQP Listener for Smart Space registration requests. This method is invoked when a Smart Space
     * registration is verified and advertised by the Registry. The Smart Space object is then
     * saved to the CoreResourceAccessMonitor local Mongo database.
     *
     * @param smartSpace The SmartS pace object of the newly registered Smart Space
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "smartSpaceRegistration", durable = "${rabbit.exchange.ssp.durable}",
                    autoDelete = "${rabbit.exchange.ssp.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.ssp.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.ssp.durable}", autoDelete  = "${rabbit.exchange.ssp.autodelete}",
                    internal = "${rabbit.exchange.ssp.internal}", type = "${rabbit.exchange.ssp.type}"),
            key = "${rabbit.routingKey.ssp.created}")
    )
    public void saveSmartSpace(SmartSpace smartSpace) {
        smartSpaceRepository.save(smartSpace);
        log.info("CRAM saved Smart Space with id: " + smartSpace.getId());
    }

    /**
     * Spring AMQP Listener for Smart Space update requests. This method is invoked when a Smart Space
     * update request is verified and advertised by the Registry. The Smart Space object is then
     * updated in the CoreResourceAccessMonitor local Mongo database.
     *
     * @param smartSpace The Smart Space object of the updated Smart Space
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "smartSpaceUpdated", durable = "${rabbit.exchange.ssp.durable}",
                    autoDelete = "${rabbit.exchange.ssp.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.ssp.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.ssp.durable}", autoDelete  = "${rabbit.exchange.ssp.autodelete}",
                    internal = "${rabbit.exchange.ssp.internal}", type = "${rabbit.exchange.ssp.type}"),
            key = "${rabbit.routingKey.ssp.modified}")
    )
    public void updateSmartSpace(SmartSpace smartSpace) {
        try {
            if (smartSpaceRepository.findOne(smartSpace.getId()) == null)
                throw new EntityNotFoundException ("Received an update message for "
                        + "Smart Space with id = " + smartSpace.getId() + " which does not exist.");

            smartSpaceRepository.save(smartSpace);
            log.info("CRAM updated Smart Space with id: " + smartSpace.getId());
        } catch (EntityNotFoundException e) {
            log.info(e);
            throw e;
        } catch (Exception e) {
            log.info(e);
        }
    }

    /**
     * Spring AMQP Listener for smartSpace unregistration requests. This method is invoked when a smartSpace
     * unregistration request is verified and advertised by the Registry. The smartSpace object is then
     * deleted from the CoreResourceAccessMonitor local Mongo database.
     *
     * @param smartSpace The smartSpace object of the smartSpace to be deleted
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "smartSpaceUnregistration", durable = "${rabbit.exchange.ssp.durable}",
                    autoDelete = "${rabbit.exchange.ssp.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.ssp.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.ssp.durable}", autoDelete  = "${rabbit.exchange.ssp.autodelete}",
                    internal = "${rabbit.exchange.ssp.internal}", type = "${rabbit.exchange.ssp.type}"),
            key = "${rabbit.routingKey.ssp.removed}")
    )
    public void deleteSmartSpace(SmartSpace smartSpace) {
        try {
            if (smartSpaceRepository.findOne(smartSpace.getId()) == null)

                throw new EntityNotFoundException ("Received an unregistration message for "
                        + "Smart Space with id = " + smartSpace.getId() + " which does not exist.");

            smartSpaceRepository.delete(smartSpace.getId());
            log.info("CRAM deleted Smart Space with id: " + smartSpace.getId());
        } catch (EntityNotFoundException e) {
            log.info(e);
            throw e;
        } catch (Exception e) {
            log.info(e);
        }
    }

    /**
     * Spring AMQP Listener for Platform resource registration requests. This method is invoked when a resource
     * registration is verified and advertised by the Registry. The resource object is then
     * saved to the CoreResourceAccessMonitor local Mongo database.
     *
     * @param message The message of the newly registered resources
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "platformResourceRegistration", durable = "${rabbit.exchange.resource.durable}",
                    autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                    internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
            key = "${rabbit.routingKey.resource.created}")
    )
    public void savePlatformResource(CoreResourceRegisteredOrModifiedEventPayload message)
            throws AmqpRejectAndDontRequeueException {
        saveResource(message);
    }

    /**
     * Spring AMQP Listener for Platform resource update requests. This method is invoked when a resource
     * update request is verified and advertised by the Registry. The resource object is then
     * updated in the CoreResourceAccessMonitor local Mongo database.
     *
     * @param message The message of the newly updated resources
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "platformResourceUpdated", durable = "${rabbit.exchange.resource.durable}",
                    autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                    internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
            key = "${rabbit.routingKey.resource.modified}")
    )
    public void updatedPlatformResource(CoreResourceRegisteredOrModifiedEventPayload message)
            throws AmqpRejectAndDontRequeueException {
        updatedResource(message);
    }

    /**
     * Spring AMQP Listener for Platform resource unregistration requests. This method is invoked when a resource
     * unregistration request is verified and advertised by the Registry. The resource object is then
     * deleted from the CoreResourceAccessMonitor local Mongo database.
     *
     * @param resourcesIds List of resource Ids of the newly deleted resources
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "platformResourceUnregistration", durable = "${rabbit.exchange.resource.durable}",
                    autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                    internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
            key = "${rabbit.routingKey.resource.removed}")
    )
    public void deletePlatformResource(List<String> resourcesIds) {
        deleteResource(resourcesIds);
    }

    /**
     * Spring AMQP Listener for Smart Space resource registration requests. This method is invoked when a resource
     * registration is verified and advertised by the Registry. The resource object is then
     * saved to the CoreResourceAccessMonitor local Mongo database.
     *
     * @param message The message of the newly registered resources
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "sspResourceRegistration", durable = "${rabbit.exchange.resource.durable}",
                    autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                    internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
            key = "${rabbit.routingKey.ssp.sdev.resource.created}")
    )
    public void saveSSPResource(CoreSspResourceRegisteredOrModifiedEventPayload message)
            throws AmqpRejectAndDontRequeueException {
        saveResource(message);
    }

    /**
     * Spring AMQP Listener for Smart Space resource update requests. This method is invoked when a resource
     * update request is verified and advertised by the Registry. The resource object is then
     * updated in the CoreResourceAccessMonitor local Mongo database.
     *
     * @param message The message of the newly updated resources
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "sspResourceUpdated", durable = "${rabbit.exchange.resource.durable}",
                    autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                    internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
            key = "${rabbit.routingKey.ssp.sdev.resource.modified}")
    )
    public void updatedSSPResource(CoreSspResourceRegisteredOrModifiedEventPayload message)
            throws AmqpRejectAndDontRequeueException {
        updatedResource(message);
    }

    /**
     * Spring AMQP Listener for Smart Space resource unregistration requests. This method is invoked when a resource
     * unregistration request is verified and advertised by the Registry. The resource object is then
     * deleted from the CoreResourceAccessMonitor local Mongo database.
     *
     * @param resourcesIds List of resource Ids of the newly deleted resources
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "sspResourceUnregistration", durable = "${rabbit.exchange.resource.durable}",
                    autoDelete = "${rabbit.exchange.resource.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.resource.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.resource.durable}", autoDelete  = "${rabbit.exchange.resource.autodelete}",
                    internal = "${rabbit.exchange.resource.internal}", type = "${rabbit.exchange.resource.type}"),
            key = "${rabbit.routingKey.ssp.sdev.resource.removed}")
    )
    public void deleteSSPResource(List<String> resourcesIds) {
        deleteResource(resourcesIds);
    }

    private void saveResource(CoreResourceRegisteredOrModifiedEventPayload message)
            throws AmqpRejectAndDontRequeueException {

        MongoRepository repository = message instanceof CoreSspResourceRegisteredOrModifiedEventPayload ?
                smartSpaceRepository : platformRepository;

        try {
            if (repository.findOne(message.getPlatformId()) == null)
                throw new EntityNotFoundException ("Received a registration message"
                        + ", but the entity " + "with id = " + message.getPlatformId()
                        + " which owns the resources does not exist.");

            for (CoreResource coreResource : message.getResources()) {
                CramResource cramResource = new CramResource(coreResource);

                cramResource.setResourceUrl(generateResourceURL(cramResource));
                cramResource.setViewsInDefinedInterval(0);
                cramResource.setPlatformId(message.getPlatformId());

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

    private void updatedResource(CoreResourceRegisteredOrModifiedEventPayload message)
            throws AmqpRejectAndDontRequeueException {

        MongoRepository repository = message instanceof CoreSspResourceRegisteredOrModifiedEventPayload ?
                smartSpaceRepository : platformRepository;
        try {
            if (repository.findOne(message.getPlatformId()) == null)
                throw new EntityNotFoundException ("Received an update message"
                        + ", but the entity " + "with id = " + message.getPlatformId()
                        + " which owns the resources does not exist.");
            for (CoreResource coreResource : message.getResources()) {
                CramResource cramResource = resourceRepository.findOne(coreResource.getId());
                if (cramResource == null)
                    throw new EntityNotFoundException ("Received an update message for "
                            + "resource with id = " + coreResource.getId() + ", but the resource does "
                            + "not exist");

                cramResource.setId(coreResource.getId());
                cramResource.setName(coreResource.getName());
                cramResource.setDescription(coreResource.getDescription());
                cramResource.setInterworkingServiceURL(coreResource.getInterworkingServiceURL());
                cramResource.setType(coreResource.getType());
                cramResource.setPlatformId(message.getPlatformId());
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

    private void deleteResource(List<String> resourcesIds) {
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
    private String generateResourceURL (CramResource resource) throws AmqpRejectAndDontRequeueException {
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
            case STATIONARY_SENSOR:
            case MOBILE_SENSOR:
            case DEVICE:
            default:
                return resource.getInterworkingServiceURL().replaceAll("(/rap)?/*$", "")
                        +  "/rap/Sensors('" + resource.getId() + "')";
        }
    }
}