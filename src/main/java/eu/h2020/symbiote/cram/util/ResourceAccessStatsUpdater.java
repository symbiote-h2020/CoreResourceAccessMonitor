package eu.h2020.symbiote.cram.util;

import eu.h2020.symbiote.cram.model.NextPopularityUpdate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;

import java.util.Timer;

import eu.h2020.symbiote.cram.repository.ResourceRepository;

/**
 * Created by vasgl on 7/2/2017.
 */
public class ResourceAccessStatsUpdater {

    private static Log log = LogFactory.getLog(ResourceAccessStatsUpdater.class);

    private static ResourceRepository resourceRepository;
    private NextPopularityUpdate nextPopularityUpdate;
    private Long subIntervalDuration;
    private Long intervalDuration;
    private Long noSubIntervals;

    @Autowired
    public ResourceAccessStatsUpdater(ResourceRepository resourceRepository, NextPopularityUpdate nextPopularityUpdate,
                                      @Qualifier("subIntervalDuration") Long subIntervalDuration,
                                      @Qualifier("intervalDuration") Long intervalDuration,
                                      @Qualifier("noSubIntervals") Long noSubIntervals) {
        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(nextPopularityUpdate,"nextPopularityUpdate can not be null!");
        this.nextPopularityUpdate = nextPopularityUpdate;

        Assert.notNull(subIntervalDuration,"subIntervalDuration can not be null!");
        this.subIntervalDuration = subIntervalDuration;

        Assert.notNull(intervalDuration,"intervalDuration can not be null!");
        this.intervalDuration = intervalDuration;

        Assert.notNull(noSubIntervals,"noSubIntervals can not be null!");
        this.noSubIntervals = noSubIntervals;

        Timer time = new Timer();
        ScheduledUpdate scheduledUpdate = new ScheduledUpdate(this.resourceRepository, this.noSubIntervals, this.subIntervalDuration);
        time.schedule(scheduledUpdate, nextPopularityUpdate.getNextUpdate(), this.subIntervalDuration);
    }
}
