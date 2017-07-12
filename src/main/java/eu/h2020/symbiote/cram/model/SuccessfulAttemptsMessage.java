package eu.h2020.symbiote.cram.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vasgl on 7/2/2017.
 */
public class SuccessfulAttemptsMessage {
    List<SuccessfulAttempts> successfulAttempts;

    public SuccessfulAttemptsMessage() {
        this.successfulAttempts = new ArrayList<SuccessfulAttempts>();
    }

    public List<SuccessfulAttempts> getSuccessfulAttempts() {return this.successfulAttempts; }
    public void setSuccessfulAttempts(List<SuccessfulAttempts> successfulAttempts) { this.successfulAttempts = successfulAttempts; }

    public void addSuccessfulAttempts(SuccessfulAttempts successfulAttempt) {
        successfulAttempts.add(successfulAttempt);
    }
}
