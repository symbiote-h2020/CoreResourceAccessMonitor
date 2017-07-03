package eu.h2020.symbiote.cram.messaging;

import eu.h2020.symbiote.cram.model.SuccessfulAttempts;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SuccessfulAttemptsMessage;

import java.util.Iterator;

/**
 * Created by vasgl on 7/2/2017.
 */
@Component
public class AccessNotificationListener {

    private static Log log = LogFactory.getLog(AccessNotificationListener.class);

    private static ResourceRepository resourceRepository;

    @Autowired
    public AccessNotificationListener(ResourceRepository resourceRepository) {
        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;
    }

    /**
     * Spring AMQP Listener for Access Notification Requests. This component listens to Access Notification Requests
     * coming from Resource Access Proxy and updates the resource statistics in the local repository.
     *
     * @param message Contains resource access updates coming from the Resource Access Proxy
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "accessNotifications", durable = "${rabbit.exchange.cram.durable}",
                    autoDelete = "${rabbit.exchange.cram.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.cram.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.cram.durable}", autoDelete  = "${rabbit.exchange.cram.autodelete}",
                    internal = "${rabbit.exchange.cram.internal}", type = "${rabbit.exchange.cram.type}"),
            key = "${rabbit.routingKey.cram.accessNotifications}")
    )
    public void listenAndUpdateResourceViewStats(SuccessfulAttemptsMessage message) {

        log.info("SuccessfulAttemptsMessage was received.");

        for(Iterator iter = message.getSuccessfulAttempts().iterator(); iter.hasNext();) {
            SuccessfulAttempts successfulAttempts = (SuccessfulAttempts) iter.next();
            CramResource cramResource = resourceRepository.findOne(successfulAttempts.getSymbioteId());

            if (cramResource != null)
                cramResource.addViewsInSubIntervals(successfulAttempts.getTimestamps());
        }
    }
}
