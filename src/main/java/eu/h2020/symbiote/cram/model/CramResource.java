package eu.h2020.symbiote.cram.model;


import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.internal.CoreResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

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

    private static final Logger log = LoggerFactory
            .getLogger(CramResource.class);

    private CoreResourceType type;
    private String resourceUrl;
    private Integer viewsInDefinedInterval;
    private List<SubIntervalViews> viewsInSubIntervals;

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

    public Integer getViewsInDefinedInterval() { return viewsInDefinedInterval; }
    public void setViewsInDefinedInterval(Integer views) { this.viewsInDefinedInterval = views; }

    public List<SubIntervalViews> getViewsInSubIntervals() { return viewsInSubIntervals; }
    public void setViewsInSubIntervals(List<SubIntervalViews> listOfViews) { this.viewsInSubIntervals = listOfViews; }

    public void addViewsInSubIntervals(List<Date> timestamps) {

        for (Iterator timestampsIter = timestamps.iterator(); timestampsIter.hasNext();) {
            Date timestamp = (Date) timestampsIter.next();
            addSingleViewInSubIntervals(timestamp);
        }
    }

    public boolean addSingleViewInSubIntervals(Date timestamp) {
        boolean foundSubInterval = false;

        // Todo: what happens when an old notification comes
        // Todo: what happens when a future notification comes
        // Todo: what happens if a notification comes, which neither old nor future and there is no subinterval

        for (SubIntervalViews subIntervalViews : viewsInSubIntervals) {

            if (subIntervalViews.belongsToSubInterval(timestamp)) {
                foundSubInterval = true;
                subIntervalViews.increaseViews(1);
                viewsInDefinedInterval++;
                break;
            }
        }

        return foundSubInterval;
    }

    public void scheduleUpdateInResourceAccessStats(Long noSubIntervals, Long subIntervalDuration) {

        log.debug("date STARTED for resource with id = " + getId());
        int sizeOfViewList = (viewsInSubIntervals == null) ? 0 : viewsInSubIntervals.size();

        SubIntervalViews nextSubIntervalView = createNextSubIntervalView(sizeOfViewList, subIntervalDuration);
        viewsInSubIntervals.add(nextSubIntervalView);

        if(sizeOfViewList == noSubIntervals) {
            viewsInDefinedInterval -= viewsInSubIntervals.get(0).getViews();
            viewsInSubIntervals.remove(0);
        }

        log.debug("date ENDED for resource with id = " + getId());

    }

    private SubIntervalViews createNextSubIntervalView(int sizeOfViewList, Long subIntervalDuration) {
        Date newStartSubIntervalTime = new Date(new Date().getTime());
        Date newEndSubIntervalTime = new Date(new Date().getTime());
        long newStartSubIntervalTime_ms = viewsInSubIntervals.get(sizeOfViewList - 1).getEndOfInterval().getTime();
        newStartSubIntervalTime.setTime(newStartSubIntervalTime_ms);
        newEndSubIntervalTime.setTime(newStartSubIntervalTime_ms + subIntervalDuration);
        return new SubIntervalViews(newStartSubIntervalTime, newEndSubIntervalTime, 0);
    }
}