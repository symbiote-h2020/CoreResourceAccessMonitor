package eu.h2020.symbiote.cram.messaging;

import eu.h2020.symbiote.cram.managers.AuthorizationManager;
import eu.h2020.symbiote.cram.model.AuthorizationResult;
import eu.h2020.symbiote.cram.model.CramResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;

import java.util.List;
import java.util.Iterator;
import java.util.HashMap;

import eu.h2020.symbiote.core.internal.ResourceUrlsRequest;
import eu.h2020.symbiote.cram.repository.ResourceRepository;

/**
 * <h1>RPC Server</h1>
 *
 * This listens to RPCs from other symbIoTe Core Components via message queues.
 *
 * @author  Vasileios Glykantzis <vasgl@intracom-telecom.com>
 * @version 0.2.1
 * @since   2017-09-15
 */

@Component
public class RpcServer {

    private static Log log = LogFactory.getLog(RpcServer.class);

    private static ResourceRepository resourceRepository;

    private AuthorizationManager authorizationManager;

    @Autowired
    public RpcServer(ResourceRepository resourceRepository,
                     AuthorizationManager authorizationManager) {

        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(authorizationManager,"AuthorizationManager can not be null!");
        this.authorizationManager = authorizationManager;
    }

    /**
     * Spring AMQP Listener for providing resource urls. This method is invoked when a
     * request for access to resources is made by an aplication/enabler. CoreInterface
     * forwards a list of resource ids to CoreResourceAccessMonitor coming from the
     * application/enabler and CoreResourceAccessMonitor responds with the urls of
     * the specified resources.
     *
     *
     * @param resourceUrlsRequest Contains the list of resource ids along with the token of the user who issued the request
     * @return A map containing urls of the resources specified in the ResourceUrlsRequest
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbit.queueName.cram.getResourceUrls}", durable = "${rabbit.exchange.cram.durable}",
                    autoDelete = "${rabbit.exchange.cram.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.cram.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.cram.durable}", autoDelete  = "${rabbit.exchange.cram.autodelete}",
                    internal = "${rabbit.exchange.cram.internal}", type = "${rabbit.exchange.cram.type}"),
            key = "${rabbit.routingKey.cram.getResourceUrls}")
    )
    public HashMap<String, String> getResourcesUrls(ResourceUrlsRequest resourceUrlsRequest) {

        List<String> resourceList = resourceUrlsRequest.getBody();
        HashMap<String, String> ids = new HashMap<>();

        log.info("CRAM received a request for the following ids: " + resourceList);

        AuthorizationResult authorizationResult = authorizationManager.checkAccess(resourceUrlsRequest.getSecurityRequest());

        if (authorizationResult.isValidated()) {
            log.debug("The Security Request is validated!");

            for (String resourceId : resourceList) {
                CramResource resource = resourceRepository.findOne(resourceId);
                if (resource != null) {

                    String url = resource.getResourceUrl();
                    ids.put(resource.getId(), url);
                    log.info("AccessController found a resource with id " + resource.getId() +
                            " and url " + url);
                } else {
                    log.info("The resource with specified id was not found");
                }
            }
        } else {
            log.debug("The Security Request was NOT validated!");
        }

        return ids;
    }
}