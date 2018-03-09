package eu.h2020.symbiote.cram.util;

import eu.h2020.symbiote.core.internal.popularity.PopularityUpdate;
import eu.h2020.symbiote.core.internal.popularity.PopularityUpdatesMessage;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.repository.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Created by vasgl on 7/2/2017.
 */
public class ScheduledPopularityUpdate extends TimerTask {

    private static Log log = LogFactory.getLog(ScheduledPopularityUpdate.class);

    private RabbitTemplate rabbitTemplate;
    private static ResourceRepository resourceRepository;
    private String searchExchange;
    private String searchPopularityUpdatesRoutingKey;

    public ScheduledPopularityUpdate(RabbitTemplate rabbitTemplate,  ResourceRepository resourceRepository,
                                     String searchExchange, String searchPopularityUpdatesRoutingKey) {
        Assert.notNull(rabbitTemplate,"RabbitTemplate can not be null!");
        this.rabbitTemplate = rabbitTemplate;

        Assert.notNull(resourceRepository,"resourceRepository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(searchExchange,"searchExchange can not be null!");
        this.searchExchange = searchExchange;

        Assert.notNull(searchPopularityUpdatesRoutingKey,"searchPopularityUpdatesRoutingKey can not be null!");
        this.searchPopularityUpdatesRoutingKey = searchPopularityUpdatesRoutingKey;
    }

    public void run() {
        log.trace("Periodic sending of popularity updates to search engine STARTED at:" + new Date(new Date().getTime()));

        PopularityUpdatesMessage popularityUpdatesMessage = new PopularityUpdatesMessage();

        List<CramResource> listOfCramResources = resourceRepository.findAll();

        log.debug("resourdeRepo size = " + listOfCramResources.size());

        for(CramResource cramResource : listOfCramResources) {
            PopularityUpdate popularityUpdate = new PopularityUpdate();
            popularityUpdate.setId(cramResource.getId());
            popularityUpdate.setViewsInDefinedInterval(cramResource.getViewsInDefinedInterval());
            popularityUpdatesMessage.addToPopularityUpdateList(popularityUpdate);
        }

        // Informing Search Engine
        log.trace("Sending message to exchange = " + searchExchange + " with key = " + searchPopularityUpdatesRoutingKey);
        if (popularityUpdatesMessage.getPopularityUpdateList() != null &&
                popularityUpdatesMessage.getPopularityUpdateList().size() > 0)
        rabbitTemplate.convertAndSend(searchExchange, searchPopularityUpdatesRoutingKey, popularityUpdatesMessage);
        log.trace("Periodic sending of popularity updates to search engine ENDED at:" + new Date(new Date().getTime()));
    }
}
