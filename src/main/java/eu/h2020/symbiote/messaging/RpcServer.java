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

import java.util.ArrayList;
import java.util.Iterator;
import java.net.MalformedURLException;

import org.json.simple.JSONObject;
import io.jsonwebtoken.MalformedJwtException;

import eu.h2020.symbiote.repository.PlatformRepository;
import eu.h2020.symbiote.repository.ResourceRepository;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.commons.security.SecurityHandler;
import eu.h2020.symbiote.commons.security.token.SymbIoTeToken;
import eu.h2020.symbiote.commons.security.token.TokenVerificationException;

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
    public JSONObject getResourcesUrls(JSONObject resourceIdList) throws Exception {

        ArrayList<String> array = (ArrayList<String>)resourceIdList.get("idList");
        Resource firstResource = resourceRepository.findOne(array.get(0));
        if (firstResource != null) {
            log.info("firstResource = " + firstResource);
        }
        else {
            log.info("The resource with specified id was not found");
        }

        String aamUrl = firstResource.getInterworkingServiceURL() + platformAAMUrlPath;
        log.info("CRAM received a request for the following ids: " + resourceIdList);
        try {
            String tokenString = resourceIdList.get("token").toString();
            SymbIoTeToken token = securityHandler.verifyForeignPlatformToken(aamUrl, tokenString);
            log.info("Token " + token + " was verified");
        }
        catch (TokenVerificationException e) { 
            log.error("Token could not be verified");
            JSONObject error = new JSONObject();
            error.put("error", "Token could not be verified");
            return error;
        }
        catch (MalformedJwtException e) { 
            log.error("Token was malformed");
            JSONObject error = new JSONObject();
            error.put("error", "Token was malformed");
            return error;
        }
        catch (Exception e) { 
            log.error("An exception was thrown");
            JSONObject error = new JSONObject();
            error.put("error", e);
            return error;
        }
        

        Iterator<String> iterator = array.iterator();
        JSONObject ids = new JSONObject();

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