package eu.h2020.symbiote.cram.model;

import java.util.Date;
import java.util.Objects;

/**
 * Created by vasgl on 6/29/2017.
 */
public class SubIntervalViews {

    private Date startOfInterval;
    private Date endOfInterval;
    private Integer views;

    public SubIntervalViews() {
        // empty constructor
    }

    public SubIntervalViews(Date startOfInterval, Date endOfInterval, Integer views) {
        this.startOfInterval = startOfInterval;
        this.endOfInterval = endOfInterval;
        this.views = views;
    }

    public Date getStartOfInterval() { return this.startOfInterval; }
    public void setStartOfInterval(Date start) { this.startOfInterval = start; }

    public Date getEndOfInterval() { return this.endOfInterval; }
    public void setEndOfInterval(Date end) { this.endOfInterval = end; }

    public Integer getViews() { return this.views; }
    public void setViews(Integer views) { this.views = views; }

    public void increaseViews(Integer views) {
        this.views += views;
    }

    public boolean belongsToSubInterval (Date timestamp) {
        long time = timestamp.getTime();
        return belongsToSubInterval(time);
    }

    public boolean belongsToSubInterval (long time) {
        return time >= startOfInterval.getTime() && time < endOfInterval.getTime();
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;

        // null check
        if (o == null)
            return false;

        // type check and cast
        if (!(o instanceof SubIntervalViews))
            return false;

        SubIntervalViews subIntervalViews = (SubIntervalViews) o;

        // field comparison
        return Objects.equals(this.getStartOfInterval(), subIntervalViews.getStartOfInterval())
                && Objects.equals(this.getEndOfInterval(), subIntervalViews.getEndOfInterval())
                && Objects.equals(this.getViews(), subIntervalViews.getViews());
    }
}
