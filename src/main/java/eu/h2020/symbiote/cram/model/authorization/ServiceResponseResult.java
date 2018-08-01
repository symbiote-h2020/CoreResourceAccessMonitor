package eu.h2020.symbiote.cram.model.authorization;

/**
 * Created by vasgl on 9/16/2017.
 */
public final class ServiceResponseResult {
    private final String serviceResponse;
    private final boolean createdSuccessfully;

    public ServiceResponseResult(String serviceResponse, boolean createdSuccessfully) {
        this.serviceResponse = serviceResponse;
        this.createdSuccessfully = createdSuccessfully;
    }

    public String getServiceResponse() { return serviceResponse; }
    public boolean isCreatedSuccessfully() { return createdSuccessfully; }
}
