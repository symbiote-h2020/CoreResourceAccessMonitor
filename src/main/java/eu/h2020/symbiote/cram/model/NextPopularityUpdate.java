package eu.h2020.symbiote.cram.model;

import java.util.Date;

/**
 * Created by lebro_000 on 6/30/2017.
 */
public class NextPopularityUpdate extends CramPersistentVariables {

    private Date nextUpdate;

    public NextPopularityUpdate() {
        // empty constructor
    }

    public NextPopularityUpdate(Long subIntervalDuration) {

        this.nextUpdate = new Date();
        this.variableName = "NEXT_POPULARITY_UPDATE";
        nextUpdate.setTime(nextUpdate.getTime() + subIntervalDuration);
    }

    public Date getNextUpdate() { return this.nextUpdate; }
    public void setNextUpdate(Date date) { this.nextUpdate = date; }
}
