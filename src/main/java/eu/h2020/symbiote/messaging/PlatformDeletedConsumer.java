package eu.h2020.symbiote.messaging;

import com.rabbitmq.client.Channel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import eu.h2020.symbiote.model.Platform;
import eu.h2020.symbiote.repository.RepositoryManager;

/**
 * Created by Mael on 07/09/2016.
 */
public class PlatformDeletedConsumer extends SymbioteMessageConsumer<String> {

    private static Log log = LogFactory.getLog(PlatformDeletedConsumer.class);


    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public PlatformDeletedConsumer(Channel channel) {
        super(channel);
    }

    /**
     * Method implementation used for actions with object passed in delivered message (Platform in JSON in this case)
     *
     * * @param platformId
     */
    @Override
    protected void handleEventObject(String platformId) {
        log.info("CRAM received message about deleted platform with id: " + platformId);
        
        RepositoryManager.deletePlatform(platformId);   
    }
}