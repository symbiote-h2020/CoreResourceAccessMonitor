package eu.h2020.symbiote.cram.util;

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

    private ResourceRepository resourceRepository;
    private Long noSubIntervals;
    private Long subIntervalDuration;

    public ScheduledUpdate(ResourceRepository resourceRepository, Long noSubIntervals, Long subIntervalDuration) {
        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(noSubIntervals,"noSubIntervals can not be null!");
        this.noSubIntervals = noSubIntervals;

        Assert.notNull(subIntervalDuration,"subIntervalDuration can not be null!");
        this.subIntervalDuration = subIntervalDuration;
    }

    public void run() {
        log.info("Periodic resource popularity update is being done :" + new Date());

        List<CramResource> listOfCramResources = resourceRepository.findAll();

        for(Iterator iter = listOfCramResources.iterator(); iter.hasNext();){
            CramResource cramResource = (CramResource) iter.next();
            cramResource.scheduleUpdateInResourceAccessStats(noSubIntervals, subIntervalDuration);
        }
    }
}
