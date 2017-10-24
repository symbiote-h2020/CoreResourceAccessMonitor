package eu.h2020.symbiote.cram.util;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.MessageInfo;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulPushesMessageInfo;
import eu.h2020.symbiote.core.internal.cram.NotificationMessageSecured;
import eu.h2020.symbiote.cram.managers.AuthorizationManager;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.authorization.AuthorizationResult;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

import java.util.*;

/**
 * Created by vasgl on 7/2/2017.
 */
public class ScheduledUpdate extends TimerTask {

    private static Log log = LogFactory.getLog(ScheduledUpdate.class);

    private static ResourceRepository resourceRepository;
    private static Long noSubIntervals;
    private static Long subIntervalDuration;
    private static AccessNotificationListener accessNotificationListener;
    private static PopularityUpdater popularityUpdater;
    private static AuthorizationManager authorizationManager;

    ScheduledUpdate(ResourceRepository resourceRepository, Long noSubIntervals,
                           Long subIntervalDuration, AccessNotificationListener accessNotificationListener,
                           PopularityUpdater popularityUpdater, AuthorizationManager authorizationManager) {
        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(noSubIntervals,"noSubIntervals can not be null!");
        this.noSubIntervals = noSubIntervals;

        Assert.notNull(subIntervalDuration,"subIntervalDuration can not be null!");
        this.subIntervalDuration = subIntervalDuration;

        Assert.notNull(accessNotificationListener,"accessNotificationListener can not be null!");
        this.accessNotificationListener = accessNotificationListener;

        Assert.notNull(popularityUpdater,"popularityUpdater can not be null!");
        this.popularityUpdater = popularityUpdater;

        Assert.notNull(authorizationManager,"authorizationManager can not be null!");
        this.authorizationManager = authorizationManager;
    }

    public void run() {
        log.trace("Periodic resource popularity update STARTED :" + new Date(new Date().getTime()));
        accessNotificationListener.setScheduledUpdateOngoing(true);

        List<CramResource> listOfCramResources = resourceRepository.findAll();

        log.debug("resourdeRepo size = " + resourceRepository.findAll().size());

        for(CramResource cramResource : listOfCramResources) {
            cramResource.scheduleUpdateInResourceAccessStats(noSubIntervals, subIntervalDuration);
        }

        resourceRepository.save(listOfCramResources);
        accessNotificationListener.setScheduledUpdateOngoing(false);
        updateResourcesWithQueuedNotifications(accessNotificationListener.getNotificationMessageList());

        log.trace("Periodic resource popularity update ENDED :" + new Date());
    }

    private void updateResourcesWithQueuedNotifications(List<NotificationMessageSecured> notificationMessageList) {
        log.trace("updateResourcesWithQueuedNotifications STARTED" );

        ArrayList<NotificationMessageSecured> messagesToRemove = new ArrayList<>();
        for (NotificationMessageSecured messageSecured : notificationMessageList) {
            updateSuccessfulAttemptsMessage(messageSecured);
            messagesToRemove.add(messageSecured);
        }
        notificationMessageList.removeAll(messagesToRemove);

        log.trace("updateResourcesWithQueuedNotifications ENDED" );
    }

    public static void updateSuccessfulAttemptsMessage(NotificationMessageSecured message) {
        for(SuccessfulAccessMessageInfo successfulAttempts : message.getBody().getSuccessfulAttempts())
            updateResourceViews(successfulAttempts, message.getSecurityRequest(), "SuccessfulAccessMessageInfo");

        for(SuccessfulPushesMessageInfo successfulPushes : message.getBody().getSuccessfulPushes())
            updateResourceViews(successfulPushes, message.getSecurityRequest(), "SuccessfulPushesMessageInfo");
    }

    private static void updateResourceViews(MessageInfo messageInfo, SecurityRequest securityRequest,
                                            String typeOfMessage) {
        log.trace("Updating views due to " + typeOfMessage);

        CramResource cramResource = resourceRepository.findOne(messageInfo.getSymbIoTeId());
        AuthorizationResult authorizationResult = authorizationManager.checkNotificationSecured(cramResource, securityRequest);

        if (cramResource != null && authorizationResult.isValidated()) {
            cramResource.addViewsInSubIntervals(messageInfo.getTimestamps(), noSubIntervals, subIntervalDuration);
            popularityUpdater.addToPopularityUpdatesMap(cramResource);
            resourceRepository.save(cramResource);

            log.debug("The views of the resource with id = " + messageInfo.getSymbIoTeId() + " were updated");
        }
        else if (cramResource == null)
            log.debug("The resource with id = " + messageInfo.getSymbIoTeId() + " was not found");
        else if (!authorizationResult.isValidated())
            log.debug(authorizationResult.getMessage());
    }
}
