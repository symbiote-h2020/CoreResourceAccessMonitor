package eu.h2020.symbiote.messaging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.Message;

import java.util.List;
import java.util.Iterator;
import java.util.HashMap;

import java.net.MalformedURLException;

import io.jsonwebtoken.MalformedJwtException;

import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.ResourceRepository;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.commons.security.SecurityHandler;
import eu.h2020.symbiote.commons.security.token.SymbIoTeToken;
import eu.h2020.symbiote.commons.security.token.TokenVerificationException;
import eu.h2020.symbiote.commons.security.exception.DisabledException;
import eu.h2020.symbiote.core.internal.ResourceUrlsRequest;

import java.io.UnsupportedEncodingException;

/**
* <h1>RPC Server</h1>
* 
* This listens to RPCs from other symbIoTe Core Components via message queues.
*
* @author  Vasileios Glykantzis <vasgl@intracom-telecom.com>
* @version 1.0
* @since   2017-01-26
*/

@Component
public class RpcServer {

    private static Log log = LogFactory.getLog(RpcServer.class);

    private static PlatformRepository platformRepository;

    private static ResourceRepository resourceRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private SecurityHandler securityHandler;

    private String platformAAMUrlPath = "";

    @Autowired
    public RpcServer(PlatformRepository platformRepository, ResourceRepository resourceRepository) {
    	
    	Assert.notNull(platformRepository,"Platform repository can not be null!");
    	this.platformRepository = platformRepository;
    	
    	Assert.notNull(resourceRepository,"Sensor repository can not be null!");
    	this.resourceRepository = resourceRepository;
    }

   /**
   * Spring AMQP Listener for providing resource urls. This method is invoked when a
   * request for access to resources is made by an aplication/enabler. CoreInterface
   * forwards a list of resource ids to CoreResourceAccessMonitor coming from the 
   * application/enabler and CoreResourceAccessMonitor responds with the urls of 
   * the specified resources.
   *
   * 
   * @param resourceIdList The list of resource ids
   * @return The urls of the resources specified in the resourceIdList
   */
    public HashMap<String, String> getResourcesUrls(ResourceUrlsRequest resourceUrlsRequest) throws Exception {

        List<String> resourceList = resourceUrlsRequest.getIdList();
        Resource firstResource = resourceRepository.findOne(resourceList.get(0));
        if (firstResource != null) {
            log.info("firstResource = " + firstResource);
        }
        else {
            log.info("The resource with specified id was not found");
        }

        String aamUrl = firstResource.getInterworkingServiceURL() + platformAAMUrlPath;
        log.info("CRAM received a request for the following ids: " + resourceList);
        try {
            String tokenString = resourceUrlsRequest.getToken();
            SymbIoTeToken token = securityHandler.verifyForeignPlatformToken(aamUrl, tokenString);
            log.info("Token " + token + " was verified");
        }
        catch (DisabledException e) { 
            log.info(e);
        }
        catch (TokenVerificationException e) { 
            log.error("Token could not be verified");
            HashMap<String, String> error = new HashMap<String, String>();
            error.put("error", "Token could not be verified");
            return error;
        }
       

        Iterator<String> iterator = resourceList.iterator();
        HashMap<String, String> ids = new HashMap<String, String>();

        while (iterator.hasNext()) {
            Resource resource = resourceRepository.findOne(iterator.next());
            if (resource != null){

                String url = resource.getInterworkingServiceURL();
                ids.put(resource.getId(), url.toString());
                log.info("AccessController found a resource with id " + resource.getId() +
                     " and url " + url.toString());
            }
            else {
                log.info("The resource with specified id was not found");
            }
        }

        return ids;
    }

}