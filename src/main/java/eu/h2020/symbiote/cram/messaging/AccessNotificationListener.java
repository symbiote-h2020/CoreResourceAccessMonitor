package eu.h2020.symbiote.cram.messaging;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.core.internal.cram.NotificationMessageSecured;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.cram.util.ScheduledUpdate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vasgl on 7/2/2017.
 */
@Component
public class AccessNotificationListener {

    private static Log log = LogFactory.getLog(AccessNotificationListener.class);

    private static ResourceRepository resourceRepository;

    private Boolean scheduledUpdateOngoing;
    private List<NotificationMessageSecured> notificationMessageList;

    @Autowired
    public AccessNotificationListener(ResourceRepository resourceRepository) {

        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        scheduledUpdateOngoing = false;
        notificationMessageList = new ArrayList<>();
    }

    public Boolean getScheduledUpdateOngoing() { return this.scheduledUpdateOngoing; }
    public void setScheduledUpdateOngoing(Boolean value) { this.scheduledUpdateOngoing = value; }

    public List<NotificationMessageSecured> getNotificationMessageList() { return this.notificationMessageList; }
    public void setNotificationMessageList(List<NotificationMessageSecured> list) { this.notificationMessageList = list; }

    /**
     * Spring AMQP Listener for Access Notification Requests. This component listens to Access Notification Requests
     * coming from Resource Access Proxy and updates the resource statistics in the local repository.
     *
     * @param messageSecured Contains resource access updates coming from the Resource Access Proxy along with the Security Request
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbit.queueName.cram.accessNotifications}", durable = "${rabbit.exchange.cram.durable}",
                    autoDelete = "${rabbit.exchange.cram.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.cram.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.cram.durable}", autoDelete  = "${rabbit.exchange.cram.autodelete}",
                    internal = "${rabbit.exchange.cram.internal}", type = "${rabbit.exchange.cram.type}"),
            key = "${rabbit.routingKey.cram.accessNotifications}")
    )
    public void listenAndUpdateResourceViewStats(NotificationMessageSecured messageSecured) {

        log.info("NotificationMessage was received.");
        NotificationMessage message = messageSecured.getBody();
        try {
            if (message != null &&
                    ((message.getSuccessfulAttempts() != null && message.getSuccessfulAttempts().size() != 0) ||
                    (message.getSuccessfulPushes() != null && message.getSuccessfulPushes().size() != 0))) {

                if (this.scheduledUpdateOngoing) {
                    log.info("Currently, the resource views are under updating, so the SuccessfulAttemptsMessage are queued.");
                    notificationMessageList.add(messageSecured);
                } else if (notificationMessageList.size() != 0){
                    log.info("Currently, the queued updates are process. The notifications will be queued there.");
                    notificationMessageList.add(messageSecured);
                } else {
                    log.info("Successfully updated");
                    ScheduledUpdate.updateSuccessfulAttemptsMessage(messageSecured);
                }
            }
        } catch (Exception e) {
            log.info(e.toString());
        }
    }
}
