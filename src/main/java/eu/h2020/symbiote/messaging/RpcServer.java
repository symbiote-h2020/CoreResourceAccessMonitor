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
import eu.h2020.symbiote.security.SecurityHandler;
import eu.h2020.symbiote.security.token.Token;
import eu.h2020.symbiote.security.exceptions.aam.TokenValidationException;
import eu.h2020.symbiote.security.session.AAM;
import eu.h2020.symbiote.core.internal.ResourceUrlsRequest;
import eu.h2020.symbiote.security.exceptions.SecurityHandlerException;
import eu.h2020.symbiote.security.enums.ValidationStatus;
import eu.h2020.symbiote.security.token.jwt.JWTClaims;
import eu.h2020.symbiote.security.token.jwt.JWTEngine;

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

    @Autowired
    private HashMap<String, AAM> aamsMap;

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

        log.info("CRAM received a request for the following ids: " + resourceList);
        try {
            Token token = new Token(resourceUrlsRequest.getToken());

            JWTClaims claims = JWTEngine.getClaimsFromToken(token.getToken());
            String aamInstanceId = claims.getIss();
            AAM aam = aamsMap.get(aamInstanceId);
            
            if (aam == null) {
                rebuildMapUsing();
                aam = aamsMap.get(aamInstanceId);
                if (aam == null)
                  throw new TokenValidationException("The specified platform AAM with aamInstanceId = " + 
                                                     aamInstanceId + " does not exist");
            }

            ValidationStatus status = securityHandler.verifyPlatformToken(aam, token);
            
            switch (status){
                case VALID: {
                    log.info("Token is VALID");  
                    break;
                }
                case VALID_OFFLINE: {
                    log.info("Token is VALID_OFFLINE");  
                    break;
                }
                case EXPIRED: {
                    log.info("Token is EXPIRED");
                    HashMap<String, String> error = new HashMap<String, String>();
                    error.put("error", "Token is EXPIRED");
                    return error;
                }
                case REVOKED: {
                    log.info("Token is REVOKED");  
                    HashMap<String, String> error = new HashMap<String, String>();
                    error.put("error", "Token is REVOKED");
                    return error;                
                }
                case INVALID: {
                    log.info("Token is INVALID");  
                    HashMap<String, String> error = new HashMap<String, String>();
                    error.put("error", "Token is INVALID");
                    return error;                
                }
                case NULL: {
                    log.info("Token is NULL");  
                    HashMap<String, String> error = new HashMap<String, String>();
                    error.put("error", "Token is NULL");
                    return error;
                }
            }             
            log.info("Token " + token + " was verified");
        }
        catch (TokenValidationException e) { 
            log.error("Token could not be verified");
            HashMap<String, String> error = new HashMap<String, String>();
            error.put("error", e.toString());
            return error;
        }
        catch (SecurityHandlerException e) {
            log.info(e); 
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


    private void rebuildMapUsing() throws SecurityHandlerException {
        List<AAM> listOfAAMs = securityHandler.getAvailableAAMs();

        for(Iterator iter = listOfAAMs.iterator(); iter.hasNext();) {
            AAM aam = (AAM) iter.next();
            aamsMap.put(aam.getAamInstanceId(), aam);
        }
    }
}