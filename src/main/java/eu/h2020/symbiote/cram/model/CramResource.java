package eu.h2020.symbiote.cram.model;


import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.internal.CoreResourceType;

/**
* <h1>CramResource</h1>
* 
* Extends the Resource class in order to save all the necessary info for CRAM
* 
* @author  Vasileios Glykantzis <vasgl@intracom-telecom.com>
* @version 1.0
* @since   2017-05-24
*/
public class CramResource extends Resource {

    private CoreResourceType type;
    private String resourceUrl;

    public CramResource() {
        // Empty constructor
    }

    public CramResource(CoreResource coreResource) {
        setId(coreResource.getId());
        setLabels(coreResource.getLabels());
        setComments(coreResource.getComments());
        setInterworkingServiceURL(coreResource.getInterworkingServiceURL());
        setType(coreResource.getType());
    }

    public CoreResourceType getType() {
        return type;
    }

    public void setType(CoreResourceType type) {
        this.type = type;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }
}