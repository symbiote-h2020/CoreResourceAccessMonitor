package eu.h2020.symbiote.cram.util;

import eu.h2020.symbiote.cram.messaging.AccessNotificationListener;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SuccessfulAttempts;
import eu.h2020.symbiote.cram.model.SuccessfulAttemptsMessage;
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
        log.debug("Periodic resource popularity update STARTED :" + new Date());
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

        updateResourcesWithQueuedNotifications(accessNotificationListener.getSuccessfulAttemptsMessageList());
    }

    private void updateResourcesWithQueuedNotifications(List<SuccessfulAttemptsMessage> successfulAttemptsMessageList) {
        log.debug("updateResourcesWithQueuedNotifications STARTED" );

        for (SuccessfulAttemptsMessage successfulAttemptsMessage : successfulAttemptsMessageList) {
            updateSuccessfulAttemptsMessage(successfulAttemptsMessage);
            successfulAttemptsMessageList.remove(successfulAttemptsMessage);
        }

        log.debug("updateResourcesWithQueuedNotifications ENDED" );
    }

    public static void updateSuccessfulAttemptsMessage(SuccessfulAttemptsMessage message) {
        for(SuccessfulAttempts successfulAttempts : message.getSuccessfulAttempts()) {
            CramResource cramResource = resourceRepository.findOne(successfulAttempts.getSymbioteId());

            if (cramResource != null){
                log.debug("The views of the resource with id = " + successfulAttempts.getSymbioteId() + " were updated");
                cramResource.addViewsInSubIntervals(successfulAttempts.getTimestamps());
                resourceRepository.save(cramResource);
            }
            else
                log.debug("The resource with id = " + successfulAttempts.getSymbioteId() + " was not found");
        }
    }
}
