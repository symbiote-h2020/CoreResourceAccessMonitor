package eu.h2020.symbiote.cram.messaging;

import eu.h2020.symbiote.core.internal.ResourceUrlsResponse;
import eu.h2020.symbiote.cram.managers.AuthorizationManager;
import eu.h2020.symbiote.cram.model.authorization.AuthorizationResult;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.authorization.ServiceResponseResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;

import java.util.ArrayList;
import java.util.List;
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
    public ResourceUrlsResponse getResourcesUrls(ResourceUrlsRequest resourceUrlsRequest) {

        List<String> resourceList = resourceUrlsRequest.getBody();
        HashMap<String, String> ids = new HashMap<>();
        List<String> notAuthorized = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        log.info("CRAM received a request for the following ids: " + resourceList);

        ServiceResponseResult serviceResponseResult = authorizationManager.generateServiceResponse();

        if (serviceResponseResult.isCreatedSuccessfully()) {
            log.debug("The Service Response was created successfully");

        } else {
            String message = "The Service Response was NOT created successfully";
            log.debug(message);
            return new ResourceUrlsResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, message, ids);
        }

        for (String resourceId : resourceList) {
            CramResource resource = resourceRepository.findOne(resourceId);
            if (resource != null) {

                AuthorizationResult authorizationResult = authorizationManager.checkAccess(resource,
                        resourceUrlsRequest.getSecurityRequest());

                if (authorizationResult.isValidated()) {
                    log.debug("The Security Request is validated!");

                    String url = resource.getResourceUrl();
                    ids.put(resource.getId(), url);
                    log.debug("AccessController found a resource with id " + resourceId +
                            " and url " + url);
                } else {
                    String message = "The Security Request was NOT validated for resource " + resourceId;
                    log.debug(message);
                    notAuthorized.add(resourceId);
                }
            } else {
                log.info("The resource with id " + resourceId + " was not found");
                notFound.add(resourceId);
            }
        }

        String message = "";

        if (notAuthorized.size() == resourceList.size()) {
            message = "The Security Request was NOT validated for any of resource!";
            log.debug(message);
            ResourceUrlsResponse response = new ResourceUrlsResponse(HttpStatus.SC_FORBIDDEN, message, ids);
            response.setServiceResponse(serviceResponseResult.getServiceResponse());
            return response;

        } else if (notAuthorized.size() > 0) {
            message += "Security Request not valid for all the resourceIds [";
            for (String id : notAuthorized) {
                message += id + ", ";
            }
            message = message.substring(0, message.length() - 2);
            message += "]. ";

        }

        if (notFound.size() > 0) {
            message += "Not all the resources were found [";
            for (String id : notFound) {
                message += id + ", ";
            }
            message = message.substring(0, message.length() - 2);
            message += "].";

        }

        if (message.isEmpty()) {
            message = "Success!";
            log.debug(message);
            ResourceUrlsResponse response = new ResourceUrlsResponse(HttpStatus.SC_OK, message, ids);
            response.setServiceResponse(serviceResponseResult.getServiceResponse());
            return response;
        } else {
            log.debug(message);
            ResourceUrlsResponse response = new ResourceUrlsResponse(HttpStatus.SC_PARTIAL_CONTENT, message, ids);
            response.setServiceResponse(serviceResponseResult.getServiceResponse());
            return response;
        }

    }
}