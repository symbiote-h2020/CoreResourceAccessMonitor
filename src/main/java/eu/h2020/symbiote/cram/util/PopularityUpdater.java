package eu.h2020.symbiote.cram.util;

import eu.h2020.symbiote.cram.repository.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.Timer;

/**
 * Created by vasgl on 7/2/2017.
 */
@Component
public class PopularityUpdater {

    private static Log log = LogFactory.getLog(PopularityUpdater.class);

    private Timer timer;
    private ScheduledPopularityUpdate scheduledPopularityUpdate;
    private RabbitTemplate rabbitTemplate;
    private Long informSearchInterval;
    private ResourceRepository resourceRepository;
    private String searchExchange;
    private String searchPopularityUpdatesRoutingKey;

    @Autowired
    public PopularityUpdater(RabbitTemplate rabbitTemplate, @Qualifier("informSearchInterval") Long informSearchInterval,
                             ResourceRepository resourceRepository, @Qualifier("searchExchange") String searchExchange,
                             @Qualifier("searchPopularityUpdatesRoutingKey") String searchPopularityUpdatesRoutingKey) {
        Assert.notNull(rabbitTemplate,"RabbitTemplate can not be null!");
        this.rabbitTemplate = rabbitTemplate;

        Assert.notNull(informSearchInterval,"informSearchInterval can not be null!");
        this.informSearchInterval = informSearchInterval;

        Assert.notNull(resourceRepository,"resourceRepository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(searchExchange,"searchExchange can not be null!");
        this.searchExchange = searchExchange;

        Assert.notNull(searchPopularityUpdatesRoutingKey,"searchPopularityUpdatesRoutingKey can not be null!");
        this.searchPopularityUpdatesRoutingKey = searchPopularityUpdatesRoutingKey;

        this.timer = new Timer();
        startTimer();
    }

    public Timer getTimer() { return this.timer; }
    public ScheduledPopularityUpdate getScheduledUpdate() { return this.scheduledPopularityUpdate; }

    public void restartTimer() {
        cancelTimer();
        startTimer();
    }

    public void startTimer() {
        timer = new Timer();
        scheduledPopularityUpdate = new ScheduledPopularityUpdate(this.rabbitTemplate, resourceRepository,
                searchExchange, searchPopularityUpdatesRoutingKey);
        timer.schedule(scheduledPopularityUpdate, new Date(new Date().getTime() + informSearchInterval),
                this.informSearchInterval);
    }

    public void cancelTimer() {
        timer.cancel();
        timer.purge();
        scheduledPopularityUpdate.cancel();
    }

}
