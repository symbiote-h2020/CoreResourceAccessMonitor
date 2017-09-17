package eu.h2020.symbiote.cram.model.authorization;

/**
 * Created by vasgl on 9/16/2017.
 */
public class ServiceResponseResult {
    private String serviceResponse;
    private boolean createdSuccessfully;

    public ServiceResponseResult() {
    }

    public ServiceResponseResult(String serviceResponse, boolean createdSuccessfully) {
        setServiceResponse(serviceResponse);
        setCreatedSuccessfully(createdSuccessfully);
    }

    public String getServiceResponse() { return serviceResponse; }
    public void setServiceResponse(String serviceResponse) { this.serviceResponse = serviceResponse; }

    public boolean isCreatedSuccessfully() { return createdSuccessfully; }
    public void setCreatedSuccessfully(boolean createdSuccessfully) { this.createdSuccessfully = createdSuccessfully; }
}
