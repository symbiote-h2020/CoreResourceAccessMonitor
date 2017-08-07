package eu.h2020.symbiote.cram.unit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.h2020.symbiote.cram.model.SubIntervalViews;
import eu.h2020.symbiote.cram.model.CramResource;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * Created by vasgl on 6/29/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class CramResourceTests {


    @Test
    public void addSingleViewInSubIntervalsTest() {
        CramResource cramResource = createCramResourceWithIntervals();
        boolean addedInSubInterval = cramResource.addSingleViewInSubIntervals(new Date(2000));

        assertEquals(true, addedInSubInterval);
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
    }

    @Test
    public void addViewsInSubIntervalsTest() {
        CramResource cramResource = createCramResourceWithIntervals();
        ArrayList<Date> DateList = new ArrayList<>();
        DateList.add(new Date(1000));
        DateList.add(new Date(1500));
        DateList.add(new Date(2000));
        DateList.add(new Date(3000));
        DateList.add(new Date(3500));
        DateList.add(new Date(4000));

        cramResource.addViewsInSubIntervals(DateList);

        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
    }

    @Test
    public void scheduleUpDateInResourceAccessStatsTest() {
        CramResource cramResource = new CramResource();
        cramResource.setViewsInDefinedInterval(0);
        Long subIntervalDuration_ms = new Long(1000);
        Long sizeOfViewList = new Long(3);

        // Create a single subInterval
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(1000 + subIntervalDuration_ms), 0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<>();
        subIntervals.add(subInterval1);
        cramResource.setViewsInSubIntervals(subIntervals);

        // Check that it has only one subInterval
        assertEquals(1, cramResource.getViewsInSubIntervals().size());

        // Check that new subIntervals are added
        cramResource.scheduleUpdateInResourceAccessStats(sizeOfViewList, subIntervalDuration_ms);
        assertEquals(2, cramResource.getViewsInSubIntervals().size());

        cramResource.scheduleUpdateInResourceAccessStats(sizeOfViewList, subIntervalDuration_ms);
        assertEquals(3, cramResource.getViewsInSubIntervals().size());

        ArrayList<Date> DateList = new ArrayList<Date>();
        DateList.add(new Date(1000));
        DateList.add(new Date(1500));
        DateList.add(new Date(2000));
        DateList.add(new Date(3000));
        DateList.add(new Date(3500));
        DateList.add(new Date(4000));

        cramResource.addViewsInSubIntervals(DateList);

        // New subInterval is added
        cramResource.scheduleUpdateInResourceAccessStats(sizeOfViewList, subIntervalDuration_ms);

        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(2).getViews());

        assertEquals(2000, cramResource.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(4000, cramResource.getViewsInSubIntervals().get(2).getStartOfInterval().getTime());
        assertEquals(5000, cramResource.getViewsInSubIntervals().get(2).getEndOfInterval().getTime());

        assertEquals((long) sizeOfViewList, cramResource.getViewsInSubIntervals().size());
        assertEquals(3, (long) cramResource.getViewsInDefinedInterval());

        ArrayList<Date> newDateList = new ArrayList<>();
        newDateList.add(new Date(1000));
        newDateList.add(new Date(4000));
        newDateList.add(new Date(5000));
        cramResource.addViewsInSubIntervals(newDateList);

        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
        assertEquals((long) sizeOfViewList, cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInDefinedInterval());

    }

    @Test
    public void pastNotificationTest() {
        CramResource cramResource = createCramResourceWithIntervals();

        assertEquals(false, cramResource.addSingleViewInSubIntervals(new Date(500)));

        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(2).getViews());

    }

    private CramResource createCramResourceWithIntervals() {
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(2000), 0);
        SubIntervalViews subInterval2 = new SubIntervalViews(new Date(2000), new Date(3000), 0);
        SubIntervalViews subInterval3 = new SubIntervalViews(new Date(3000), new Date(4000), 0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<SubIntervalViews>();
        CramResource cramResource = new CramResource();

        subIntervals.add(subInterval1);
        subIntervals.add(subInterval2);
        subIntervals.add(subInterval3);
        cramResource.setViewsInSubIntervals(subIntervals);
        cramResource.setViewsInDefinedInterval(0);

        return cramResource;
    }
}
