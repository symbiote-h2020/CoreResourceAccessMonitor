package eu.h2020.symbiote.cram.unit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger log = LoggerFactory
            .getLogger(CramResourceTests.class);

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
        ArrayList<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date(1000));
        dateList.add(new Date(1500));
        dateList.add(new Date(2000));
        dateList.add(new Date(3000));
        dateList.add(new Date(3500));
        dateList.add(new Date(4000));

        cramResource.addViewsInSubIntervals(dateList);

        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
    }

    @Test
    public void scheduleUpdateInResourceAccessStatsTest() {
        CramResource cramResource = new CramResource();
        cramResource.setViewsInDefinedInterval(0);
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(2000), 0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<SubIntervalViews>();
        subIntervals.add(subInterval1);
        cramResource.setViewsInSubIntervals(subIntervals);

        assertEquals(1, cramResource.getViewsInSubIntervals().size());

        cramResource.scheduleUpdateInResourceAccessStats(new Long(3), new Long(1000));
        assertEquals(2, cramResource.getViewsInSubIntervals().size());

        cramResource.scheduleUpdateInResourceAccessStats(new Long(3), new Long(1000));
        assertEquals(3, cramResource.getViewsInSubIntervals().size());

        ArrayList<Date> dateList = new ArrayList<Date>();
        dateList.add(new Date(1000));
        dateList.add(new Date(1500));
        dateList.add(new Date(2000));
        dateList.add(new Date(3000));
        dateList.add(new Date(3500));
        dateList.add(new Date(4000));

        cramResource.addViewsInSubIntervals(dateList);
        cramResource.scheduleUpdateInResourceAccessStats(new Long(3), new Long(1000));

        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(2).getViews());

        assertEquals(2000, cramResource.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(4000, cramResource.getViewsInSubIntervals().get(2).getStartOfInterval().getTime());
        assertEquals(5000, cramResource.getViewsInSubIntervals().get(2).getEndOfInterval().getTime());

        assertEquals(3, cramResource.getViewsInSubIntervals().size());
        assertEquals(3, (long) cramResource.getViewsInDefinedInterval());

        ArrayList<Date> newDateList = new ArrayList<Date>();
        newDateList.add(new Date(4000));
        newDateList.add(new Date(5000));
        cramResource.addViewsInSubIntervals(newDateList);

        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
        assertEquals(3, cramResource.getViewsInSubIntervals().size());
        assertEquals(4, (long) cramResource.getViewsInDefinedInterval());

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
