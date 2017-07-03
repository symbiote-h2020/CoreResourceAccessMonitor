package eu.h2020.symbiote.cram.model;

import java.util.Date;
import java.util.List;

/**
 * Created by vasgl on 7/2/2017.
 */
public class SuccessfulAttempts {

    private String accessType;
    private String symbioteId;
    private List<Date> timestamps;

    public SuccessfulAttempts() {
        // empty constructor
    }

    public String getAccessType() { return this.accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public String getSymbioteId() {return this.symbioteId; }
    public void setSymbioteId(String id) { this.symbioteId = id; }

    public List<Date> getTimestamps() {return this.timestamps; }
    public void setTimestamps(List<Date> timestamps) {this.timestamps = timestamps; }
}
