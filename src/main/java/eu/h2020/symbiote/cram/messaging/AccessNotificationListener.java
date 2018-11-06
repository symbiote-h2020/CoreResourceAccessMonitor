package eu.h2020.symbiote.cram.messaging;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.core.internal.cram.NotificationMessageResponseSecured;
import eu.h2020.symbiote.core.internal.cram.NotificationMessageSecured;
import eu.h2020.symbiote.cram.managers.AuthorizationManager;
import eu.h2020.symbiote.cram.model.authorization.ServiceResponseResult;
import eu.h2020.symbiote.cram.util.ScheduledUpdate;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vasgl on 7/2/2017.
 */
@Component
public class AccessNotificationListener {

    private static Log log = LogFactory.getLog(AccessNotificationListener.class);

    private AuthorizationManager authorizationManager;

    private Boolean scheduledUpdateOngoing;
    private List<NotificationMessageSecured> notificationMessageList;

    @Autowired
    public AccessNotificationListener(AuthorizationManager authorizationManager) {

        this.authorizationManager = authorizationManager;

        scheduledUpdateOngoing = false;
        notificationMessageList = new ArrayList<>();
    }

    public Boolean getScheduledUpdateOngoing() { return this.scheduledUpdateOngoing; }
    public void setScheduledUpdateOngoing(Boolean value) { this.scheduledUpdateOngoing = value; }

    public List<NotificationMessageSecured> getNotificationMessageList() { return this.notificationMessageList; }

    /**
     * Spring AMQP Listener for Access Notification Requests. This component listens to Access Notification Requests
     * coming from Resource Access Proxy and updates the resource statistics in the local repository.
     *
     * @param messageSecured Contains resource access updates coming from the Resource Access Proxy along with the Security Request
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbit.queueName.cram.accessNotifications}", durable = "${rabbit.exchange.cram.durable}",
                    autoDelete = "${rabbit.exchange.cram.autodelete}", exclusive = "false",
                    arguments= {@Argument(name = "x-message-ttl", value="${spring.rabbitmq.template.reply-timeout}", type="java.lang.Integer")}),
            exchange = @Exchange(value = "${rabbit.exchange.cram.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.cram.durable}", autoDelete  = "${rabbit.exchange.cram.autodelete}",
                    internal = "${rabbit.exchange.cram.internal}", type = "${rabbit.exchange.cram.type}"),
            key = "${rabbit.routingKey.cram.accessNotifications}")
    )
    public NotificationMessageResponseSecured listenAndUpdateResourceViewStats(NotificationMessageSecured messageSecured) {

        log.trace("NotificationMessage was received: " + ReflectionToStringBuilder.toString(messageSecured));
        NotificationMessage message = messageSecured.getBody();
        NotificationMessageResponseSecured responseSecured = new NotificationMessageResponseSecured();

        try {
            if (message != null &&
                    ((message.getSuccessfulAttempts() != null && message.getSuccessfulAttempts().size() != 0) ||
                    (message.getSuccessfulPushes() != null && message.getSuccessfulPushes().size() != 0))) {

                if (this.scheduledUpdateOngoing) {
                    log.debug("Currently, the resource views are under updating, so the SuccessfulAttemptsMessage are queued.");
                    notificationMessageList.add(messageSecured);
                } else if (notificationMessageList.size() != 0){
                    log.debug("Currently, the queued updates are process. The notifications will be queued there.");
                    notificationMessageList.add(messageSecured);
                } else {
                    log.debug("Update will happen immediately");
                    ScheduledUpdate.updateSuccessfulAttemptsMessage(messageSecured);
                }
            }
            // Return the service response
            ServiceResponseResult serviceResponseResult = authorizationManager.generateServiceResponse();
            responseSecured.setServiceResponse(serviceResponseResult.getServiceResponse());

        } catch (Throwable e) {
            log.info(e.toString());
        }

        return responseSecured;
    }
}
