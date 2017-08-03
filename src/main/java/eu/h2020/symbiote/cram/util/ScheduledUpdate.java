package eu.h2020.symbiote.cram.util;

import eu.h2020.symbiote.core.cci.accessNotificationMessages.MessageInfo;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.NotificationMessage;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.core.cci.accessNotificationMessages.SuccessfulPushesMessageInfo;
import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.repository.ResourceRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by vasgl on 7/2/2017.
 */
public class ScheduledUpdate extends TimerTask{

    private static Log log = LogFactory.getLog(ScheduledUpdate.class);

    private static ResourceRepository resourceRepository;
    private Long noSubIntervals;
    private Long subIntervalDuration;
    private AccessNotificationListener accessNotificationListener;

    public ScheduledUpdate(ResourceRepository resourceRepository, Long noSubIntervals,
                           Long subIntervalDuration, AccessNotificationListener accessNotificationListener) {
        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(noSubIntervals,"noSubIntervals can not be null!");
        this.noSubIntervals = noSubIntervals;

        Assert.notNull(subIntervalDuration,"subIntervalDuration can not be null!");
        this.subIntervalDuration = subIntervalDuration;

        Assert.notNull(accessNotificationListener,"accessNotificationListener can not be null!");
        this.accessNotificationListener = accessNotificationListener;
    }

    public void run() {
        log.debug("Periodic resource popularity update STARTED :" + new Date(new Date().getTime()));
        accessNotificationListener.setScheduledUpdateOngoing(true);

        List<CramResource> listOfCramResources = resourceRepository.findAll();

        // Todo: store notifications coming from RAP when this operation happens

        for(Iterator iter = listOfCramResources.iterator(); iter.hasNext();){
            CramResource cramResource = (CramResource) iter.next();
            cramResource.scheduleUpdateInResourceAccessStats(noSubIntervals, subIntervalDuration);
        }

        resourceRepository.save(listOfCramResources);
        accessNotificationListener.setScheduledUpdateOngoing(false);
        log.debug("Periodic resource popularity update ENDED :" + new Date());

        updateResourcesWithQueuedNotifications(accessNotificationListener.getNotificationMessageList());
    }

    private void updateResourcesWithQueuedNotifications(List<NotificationMessage> notificationMessageList) {
        log.debug("updateResourcesWithQueuedNotifications STARTED" );

        for (NotificationMessage notificationMessage : notificationMessageList) {
            updateSuccessfulAttemptsMessage(notificationMessage);
            notificationMessageList.remove(notificationMessage);
        }

        log.debug("updateResourcesWithQueuedNotifications ENDED" );
    }

    public static void updateSuccessfulAttemptsMessage(NotificationMessage message) {
        for(SuccessfulAccessMessageInfo successfulAttempts : message.getSuccessfulAttempts())
            updateResourceViews(successfulAttempts, "SuccessfulAccessMessageInfo");

        for(SuccessfulPushesMessageInfo successfulPushes : message.getSuccessfulPushes())
            updateResourceViews(successfulPushes, "SuccessfulPushesMessageInfo");
    }

    private static void updateResourceViews(MessageInfo messageInfo, String typeOfMessage) {
        log.debug("Updating views due to " + typeOfMessage);

        CramResource cramResource = resourceRepository.findOne(messageInfo.getSymbIoTeId());

        if (cramResource != null){
            log.debug("The views of the resource with id = " + messageInfo.getSymbIoTeId() + " were updated");
            cramResource.addViewsInSubIntervals(messageInfo.getTimestamps());
            resourceRepository.save(cramResource);
        }
        else
            log.debug("The resource with id = " + messageInfo.getSymbIoTeId() + " was not found");
    }
}
