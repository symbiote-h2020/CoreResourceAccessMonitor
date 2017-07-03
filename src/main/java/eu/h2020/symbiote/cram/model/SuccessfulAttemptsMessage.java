package eu.h2020.symbiote.cram.model;

import java.util.List;

/**
 * Created by vasgl on 7/2/2017.
 */
public class SuccessfulAttemptsMessage {
    List<SuccessfulAttempts> successfulAttempts;

    public SuccessfulAttemptsMessage() {
        // empty constructor
    }

    public List<SuccessfulAttempts> getSuccessfulAttempts() {return this.successfulAttempts; }
    public void setSuccessfulAttempts(List<SuccessfulAttempts> successfulAttempts) { this.successfulAttempts = successfulAttempts; }
}
