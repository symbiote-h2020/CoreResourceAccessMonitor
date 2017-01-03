package eu.h2020.symbiote.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.Sensor;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.core.ExchangeTypes;

/**
 * Created by tipech on 04/10/2016.
 */
@Component
public class RepositoryManager {

    private static Log log = LogFactory.getLog(RepositoryManager.class);

    private static PlatformRepository platformRepository;

    private static SensorRepository sensorRepository;

    @Autowired
    public RepositoryManager(PlatformRepository platformRepository, SensorRepository sensorRepository){
    	
    	Assert.notNull(platformRepository,"Platform repository can not be null!");
    	this.platformRepository = platformRepository;
    	
    	Assert.notNull(sensorRepository,"Sensor repository can not be null!");
    	this.sensorRepository = sensorRepository;
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-created", durable = "true"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.created")
    )
    public static void savePlatform(Platform deliveredObject) {

        log.info("CRAM received platform: " + deliveredObject);

		platformRepository.save(deliveredObject);
        log.info("Platform saved to database!");
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-updated", durable = "true"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.updated")
    )
    public static void updatePlatform(Platform deliveredObject) {

        log.info("CRAM updated platform: " + deliveredObject);
        platformRepository.save(deliveredObject);
        log.info("Platform updated to database!");
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-platform-deleted", durable = "true"),
        exchange = @Exchange(value = "symbIoTe.platform", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
        key = "symbIoTe.platform.deleted")
    )
    public static void deletePlatform(String platformId) {

        log.info("CRAM deleted platform: " + platformId);

        platformRepository.delete(platformId);
        log.info("Platform deleted from database!");
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-created", durable = "true"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.created")
    )
    public static void saveSensor(Sensor deliveredObject) {

        log.info("CRAM received resource: " + deliveredObject);

		sensorRepository.save(deliveredObject);
        log.info("Sensor saved to database!");
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-updated", durable = "true"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.updated")
    )
    public static void updateSensor(Sensor deliveredObject) {

        log.info("CRAM updated resource: " + deliveredObject);

        sensorRepository.save(deliveredObject);
        log.info("Sensor updated to database!");
    }

    @RabbitListener(bindings = @QueueBinding(
        value = @Queue(value = "symbIoTe-CoreResourceAccessMonitor-resource-deleted", durable = "true"),
        exchange = @Exchange(value = "symbIoTe.resource", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
        key = "symbIoTe.resource.deleted")
    )
    public static void deleteSensor(String sensorId) {

        log.info("CRAM deleted resource: " + sensorId);

        sensorRepository.delete(sensorId);
        log.info("Sensor deleted from database!");
    }
}