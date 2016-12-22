package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.repository.RepositoryManager;

/**
 * Created by Mael on 07/09/2016.
 */
public class PlatformUpdatedConsumer extends SymbioteMessageConsumer<Platform> {

    private static Log log = LogFactory.getLog(PlatformUpdatedConsumer.class);


    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public PlatformUpdatedConsumer(Channel channel) {
        super(channel);
    }

    /**
     * Method implementation used for actions with object passed in delivered message (Platform in JSON in this case)
     *
     * * @param deliveredObject
     */
    @Override
    protected void handleEventObject(Platform deliveredObject) {
        log.info("CRAM received message about updated platform with id: " + deliveredObject.getId());
        
        RepositoryManager.updatePlatform(deliveredObject);   
    }
}