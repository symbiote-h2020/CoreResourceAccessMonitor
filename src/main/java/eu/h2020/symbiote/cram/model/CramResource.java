package eu.h2020.symbiote.cram.model;


import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.internal.CoreResource;
import eu.h2020.symbiote.core.model.internal.CoreResourceType;

import java.util.ArrayList;
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

        for (Iterator subIntervalsIter = viewsInSubIntervals.iterator(); subIntervalsIter.hasNext();) {
            SubIntervalViews subIntervalViews = (SubIntervalViews) subIntervalsIter.next();

            if ( subIntervalViews.belongsToSubInterval(timestamp) ) {
                foundSubInterval = true;
                subIntervalViews.increaseViews(1);
                viewsInDefinedInterval++;
                break;
            }
        }

        return foundSubInterval;
    }

}