package eu.h2020.symbiote.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.model.Sensor;

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

    public static void savePlatform(Platform deliveredObject) {

		platformRepository.save(deliveredObject);
        log.info("Platform saved to database!");
    }

    public static void updatePlatform(Platform deliveredObject) {

        platformRepository.save(deliveredObject);
        log.info("Platform updated to database!");
    }

    public static void deletePlatform(String platformId) {

        platformRepository.delete(platformId);
        log.info("Platform deleted from database!");
    }

    public static void saveSensor(Sensor deliveredObject) {

		sensorRepository.save(deliveredObject);
        log.info("Sensor saved to database!");
    }

    public static void updateSensor(Sensor deliveredObject) {

        sensorRepository.save(deliveredObject);
        log.info("Sensor updated to database!");
    }

    public static void deleteSensor(String sensorId) {

        sensorRepository.delete(sensorId);
        log.info("Sensor deleted from database!");
    }
}