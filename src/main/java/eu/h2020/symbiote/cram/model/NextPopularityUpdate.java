package eu.h2020.symbiote.cram.model;

import org.springframework.data.annotation.Id;

import java.util.Date;

/**
 * Created by lebro_000 on 6/30/2017.
 */
public class NextPopularityUpdate extends CramPersistentVariables {

    @Id
    private static final String id = "NEXT_POPULARITY_UPDATE";
    public Date nextUpdate;

    public NextPopularityUpdate() {
        this.nextUpdate = new Date();
    }

    public Date getNextUpdate() { return this.nextUpdate; }
    public void setNextUpdate(Date date) { this.nextUpdate = date; }
}
