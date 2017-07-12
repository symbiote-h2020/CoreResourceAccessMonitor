package eu.h2020.symbiote.cram.messaging;

import eu.h2020.symbiote.cram.model.SuccessfulAttempts;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.util.ScheduledUpdate;
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

import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SuccessfulAttemptsMessage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by vasgl on 7/2/2017.
 */
@Component
public class AccessNotificationListener {

    private static Log log = LogFactory.getLog(AccessNotificationListener.class);

    private static ResourceRepository resourceRepository;

    private Boolean scheduledUpdateOngoing;
    private List<SuccessfulAttemptsMessage> successfulAttemptsMessageList;

    @Autowired
    public AccessNotificationListener(ResourceRepository resourceRepository) {

        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        scheduledUpdateOngoing = new Boolean(false);
        successfulAttemptsMessageList = new ArrayList<SuccessfulAttemptsMessage>();
    }

    public Boolean getScheduledUpdateOngoing() { return this.scheduledUpdateOngoing; }
    public void setScheduledUpdateOngoing(Boolean value) { this.scheduledUpdateOngoing = value; }

    public List<SuccessfulAttemptsMessage> getSuccessfulAttemptsMessageList() { return this.successfulAttemptsMessageList; }
    public void setSuccessfulAttemptsMessageList(List<SuccessfulAttemptsMessage> list) { this.successfulAttemptsMessageList = list; }

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

        if (this.scheduledUpdateOngoing) {
            log.info("Currently, the resource views are under updating, so the SuccessfulAttemptsMessage are queued.");
            successfulAttemptsMessageList.add(message);
        } else if (successfulAttemptsMessageList.size() != 0){
            log.info("Currently, the queued updates are process. The notifications will be queued there.");
            successfulAttemptsMessageList.add(message);
        } else {
            log.info("Successfully updated");
            ScheduledUpdate.updateSuccessfulAttemptsMessage(message);
        }
    }

}
