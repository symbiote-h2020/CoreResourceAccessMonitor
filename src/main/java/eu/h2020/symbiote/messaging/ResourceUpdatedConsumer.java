package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.h2020.symbiote.model.Sensor;
import eu.h2020.symbiote.repository.RepositoryManager;

/**
 * Created by Mael on 08/09/2016.
 */
public class ResourceUpdatedConsumer extends SymbioteMessageConsumer<Sensor> {

    private static Log log = LogFactory.getLog(ResourceUpdatedConsumer.class);
    

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public ResourceUpdatedConsumer(Channel channel) {
        super(channel);
    }


    /**
     * Method implementation used for actions with object passed in delivered message (Sensor in JSON in this case)
     *
     * @param deliveredObject
     */
    @Override
    protected void handleEventObject(Sensor deliveredObject) {
        log.info("CRAM received message about updated resource with id: " + deliveredObject.getId());

        RepositoryManager.updateSensor(deliveredObject);   
    }
}