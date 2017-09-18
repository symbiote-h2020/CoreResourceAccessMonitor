package eu.h2020.symbiote.cram.model;


import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.internal.CoreResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
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
    private String platformId;

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

    public String getPlatformId() { return platformId; }
    public void setPlatformId(String platformId) { this.platformId = platformId; }

    public void addViewsInSubIntervals(List<Date> timestamps, Long noSubIntervals, Long subIntervalDuration) {

        for (Date timestamp : timestamps) {
            if (!addSingleViewInSubIntervals(timestamp)) {
                if (timestamp.before(new Date())) {
                    SubIntervalViews newSubIntervalViews = new SubIntervalViews(new Date(timestamp.getTime()),
                            new Date(timestamp.getTime() + subIntervalDuration), 1);
                    addNextSubIntervalView(newSubIntervalViews, noSubIntervals);
                }
            }
        }
    }

    public boolean addSingleViewInSubIntervals(Date timestamp) {
        boolean foundSubInterval = false;

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

        log.debug("Update STARTED for resource with id = " + getId());
        int currentSizeOfViewList = 0;

        if (viewsInSubIntervals == null) {
            viewsInSubIntervals = new ArrayList<>();
        } else {
            currentSizeOfViewList = viewsInSubIntervals.size();
        }

        SubIntervalViews nextSubIntervalViews = createNextSubIntervalView(currentSizeOfViewList, subIntervalDuration);
        addNextSubIntervalView(nextSubIntervalViews, noSubIntervals);

        log.debug("Update ENDED for resource with id = " + getId());

    }

    public void addNextSubIntervalView(SubIntervalViews nextSubIntervalViews, Long noSubIntervals) {
        viewsInSubIntervals.add(nextSubIntervalViews);
        viewsInDefinedInterval += nextSubIntervalViews.getViews();

        while (viewsInSubIntervals.size() > noSubIntervals) {
            viewsInDefinedInterval -= viewsInSubIntervals.get(0).getViews();
            viewsInSubIntervals.remove(0);
        }
    }

    private SubIntervalViews createNextSubIntervalView(int sizeOfViewList, Long subIntervalDuration) {
        Date newStartSubIntervalTime = new Date();
        Date newEndSubIntervalTime = new Date();
        long newStartSubIntervalTime_ms = viewsInSubIntervals.get(sizeOfViewList - 1).getEndOfInterval().getTime();
        newStartSubIntervalTime.setTime(newStartSubIntervalTime_ms);
        newEndSubIntervalTime.setTime(newStartSubIntervalTime_ms + subIntervalDuration);
        return createNextSubIntervalView(newStartSubIntervalTime, newEndSubIntervalTime);
    }

    private SubIntervalViews createNextSubIntervalView(Date newStartSubIntervalTime, Date newEndSubIntervalTime) {
        return new SubIntervalViews(newStartSubIntervalTime, newEndSubIntervalTime, 0);
    }
}