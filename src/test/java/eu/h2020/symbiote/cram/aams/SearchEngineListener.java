package eu.h2020.symbiote.cram.aams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.core.internal.popularity.PopularityUpdate;
import eu.h2020.symbiote.core.internal.popularity.PopularityUpdatesMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vasgl on 7/17/2017.
 */
@Component
public class SearchEngineListener {
    private static Logger log = LoggerFactory
            .getLogger(SearchEngineListener.class);

    private List<PopularityUpdatesMessage> popularityUpdatesMessages = new ArrayList<>();
    private ObjectMapper mapper = new ObjectMapper();

    public List<PopularityUpdatesMessage> getPopularityUpdatesMessages() { return popularityUpdatesMessages; }

    public int popularityUpdatesMessagesReceived() { return popularityUpdatesMessages.size(); }

    public void clearRequestsReceivedByListener() {
        popularityUpdatesMessages.clear();
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbit.queueName.search.popularityUpdates}", durable = "${rabbit.exchange.search.durable}",
                    autoDelete = "${rabbit.exchange.search.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.search.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.search.durable}", autoDelete  = "${rabbit.exchange.search.autodelete}",
                    internal = "${rabbit.exchange.search.internal}", type = "${rabbit.exchange.search.type}"),
            key = "${rabbit.routingKey.search.popularityUpdates}")
    )
    public void enablerLogicResourcesUpdatedListener(PopularityUpdatesMessage popularityUpdatesMessage) {
        popularityUpdatesMessages.add(popularityUpdatesMessage);

        try {
            String responseInString = mapper.writeValueAsString(popularityUpdatesMessage);
            log.info("SearchListener received popularityUpdate request: " + responseInString);
            log.info("popularityUpdatesMessages.size() = " + popularityUpdatesMessages.size());

            for(PopularityUpdate popularityUpdate : popularityUpdatesMessage.getPopularityUpdateList()) {
                log.info("(resourceId, views) = (" + popularityUpdate.getId() + ", " +
                        popularityUpdate.getViewsInDefinedInterval() + ")");
            }
        } catch (JsonProcessingException e) {
            log.info(e.toString());
        }
    }
}
