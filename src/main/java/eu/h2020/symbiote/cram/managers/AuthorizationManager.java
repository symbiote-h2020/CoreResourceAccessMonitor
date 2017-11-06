package eu.h2020.symbiote.cram.managers;

import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.security.accesspolicies.common.SingleTokenAccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.ComponentHomeTokenAccessPolicy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.h2020.symbiote.cram.model.authorization.AuthorizationResult;
import eu.h2020.symbiote.cram.model.authorization.ServiceResponseResult;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 *
 * @author mateuszl
 * @author vasgl
 */
@Component()
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);
    private static final String RAP_IDENTIFIER = "rap";

    private String componentOwnerName;
    private String componentOwnerPassword;
    private String aamAddress;
    private String clientId;
    private String keystoreName;
    private String keystorePass;
    private Boolean securityEnabled;

    private IComponentSecurityHandler componentSecurityHandler;

    @Autowired
    public AuthorizationManager(@Value("${aam.deployment.owner.username}") String componentOwnerName,
                                @Value("${aam.deployment.owner.password}") String componentOwnerPassword,
                                @Value("${aam.environment.coreInterfaceAddress}") String aamAddress,
                                @Value("${aam.environment.clientId}") String clientId,
                                @Value("${aam.environment.keystoreName}") String keystoreName,
                                @Value("${aam.environment.keystorePass}") String keystorePass,
                                @Value("${cram.security.enabled}") Boolean securityEnabled)
            throws SecurityHandlerException, InvalidArgumentsException {

        Assert.notNull(componentOwnerName,"componentOwnerName can not be null!");
        this.componentOwnerName = componentOwnerName;

        Assert.notNull(componentOwnerPassword,"componentOwnerPassword can not be null!");
        this.componentOwnerPassword = componentOwnerPassword;

        Assert.notNull(aamAddress,"aamAddress can not be null!");
        this.aamAddress = aamAddress;

        Assert.notNull(clientId,"clientId can not be null!");
        this.clientId = clientId;

        Assert.notNull(keystoreName,"keystoreName can not be null!");
        this.keystoreName = keystoreName;

        Assert.notNull(keystorePass,"keystorePass can not be null!");
        this.keystorePass = keystorePass;

        Assert.notNull(securityEnabled,"securityEnabled can not be null!");
        this.securityEnabled = securityEnabled;

        if (securityEnabled)
            enableSecurity();
    }

    public AuthorizationResult checkResourceUrlRequest(CramResource resource, SecurityRequest securityRequest) {
        if (securityEnabled) {
            log.debug("Received SecurityRequest to verification: (" + securityRequest + ")");

            if (securityRequest == null) {
                return new AuthorizationResult("SecurityRequest is null", false);
            }


            Set<String> checkedPolicies;
            try {
                checkedPolicies = checkStoredResourcePolicy(resource, securityRequest);
            } catch (InvalidArgumentsException e) {
                e.printStackTrace();
                return new AuthorizationResult(e.getErrorMessage(), false);

            }

            if (checkedPolicies.size() == 1) {
                return new AuthorizationResult("ok", true);
            } else {
                return new AuthorizationResult("The stored resource access policy was not satisfied",
                        false);
            }
        } else {
            log.debug("checkAccess: Security is disabled");

            //if security is disabled in properties
            return new AuthorizationResult("Security disabled", true);
        }
    }

    public AuthorizationResult checkNotificationSecured(CramResource resource, SecurityRequest securityRequest) {
        if (securityEnabled) {
            log.debug("Received SecurityRequest to verification: (" + securityRequest + ")");

            if (securityRequest == null) {
                return new AuthorizationResult("SecurityRequest is null", false);
            }


            Set<String> checkedPolicies;
            try {
                checkedPolicies = checkComponentHomeTokenAccessPolicy(resource, securityRequest);
            } catch (InvalidArgumentsException e) {
                e.printStackTrace();
                return new AuthorizationResult(e.getErrorMessage(), false);

            }

            if (checkedPolicies.size() == 1) {
                return new AuthorizationResult("ok", true);
            } else {
                return new AuthorizationResult("The SingleLocalHomeTokenAccessPolicy was not satisfied",
                        false);
            }
        } else {
            log.debug("checkAccess: Security is disabled");

            //if security is disabled in properties
            return new AuthorizationResult("security disabled", true);
        }
    }

    public ServiceResponseResult generateServiceResponse() {
        if (securityEnabled) {
            try {
                String serviceResponse = componentSecurityHandler.generateServiceResponse();
                return new ServiceResponseResult(serviceResponse, true);
            } catch (SecurityHandlerException e) {
                e.printStackTrace();
                return new ServiceResponseResult("", false);
            }
        } else {
            log.debug("generateServiceResponse: Security is disabled");
            return new ServiceResponseResult("", false);
        }

    }

    private void enableSecurity() throws SecurityHandlerException {
        securityEnabled = true;
        componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                aamAddress,
                keystoreName,
                keystorePass,
                clientId,
                aamAddress,
                false,
                componentOwnerName,
                componentOwnerPassword);

    }

    private Set<String> checkStoredResourcePolicy(CramResource resource, SecurityRequest securityRequest)
            throws InvalidArgumentsException {
        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();

        // Construct policy
        IAccessPolicy policy = SingleTokenAccessPolicyFactory.getSingleTokenAccessPolicy(resource.getPolicySpecifier());
        accessPoliciesMap.put(resource.getId(), policy);

        return componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest);
    }

    private Set<String> checkComponentHomeTokenAccessPolicy(CramResource resource, SecurityRequest securityRequest)
            throws InvalidArgumentsException {
        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();

        accessPoliciesMap.put("ComponentHomeTokenAccessPolicy",
                new ComponentHomeTokenAccessPolicy(resource.getPlatformId(), RAP_IDENTIFIER,  new HashMap<>()));
        return componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest);
    }

    /**
     * Setters and Getters
     */

    public IComponentSecurityHandler getComponentSecurityHandler() {
        return componentSecurityHandler;
    }

    public void setComponentSecurityHandler(IComponentSecurityHandler componentSecurityHandler) {
        this.componentSecurityHandler = componentSecurityHandler;
    }
}
