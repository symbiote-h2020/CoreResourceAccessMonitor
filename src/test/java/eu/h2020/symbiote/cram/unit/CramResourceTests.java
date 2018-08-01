package eu.h2020.symbiote.cram.unit;

import eu.h2020.symbiote.core.internal.CoreResourceType;
import eu.h2020.symbiote.cram.model.CramResource;
import eu.h2020.symbiote.cram.model.SubIntervalViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Vasileios Glykantzis (ICOM)
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
        Long subIntervalDuration_ms = 1000L;
        Long noSubIntervals = 3L;
        CramResource cramResource = createCramResourceWithIntervals();
        ArrayList<Date> DateList = new ArrayList<>();

        DateList.add(new Date(1000));
        DateList.add(new Date(1500));
        DateList.add(new Date(2000));
        DateList.add(new Date(3000));
        DateList.add(new Date(3500));

        cramResource.addViewsInSubIntervals(DateList, noSubIntervals, subIntervalDuration_ms);

        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
    }

    @Test
    public void scheduleUpDateInResourceAccessStatsTest() {
        CramResource cramResource = new CramResource();
        cramResource.setViewsInDefinedInterval(0);
        Long subIntervalDuration_ms = 1000L;
        Long noSubIntervals = 3L;

        // Create a single subInterval
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(1000 + subIntervalDuration_ms), 0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<>();
        subIntervals.add(subInterval1);
        cramResource.setViewsInSubIntervals(subIntervals);

        // Check that it has only one subInterval
        assertEquals(1, cramResource.getViewsInSubIntervals().size());

        // Check that new subIntervals are added
        cramResource.scheduleUpdateInResourceAccessStats(noSubIntervals, subIntervalDuration_ms);
        assertEquals(2, cramResource.getViewsInSubIntervals().size());

        cramResource.scheduleUpdateInResourceAccessStats(noSubIntervals, subIntervalDuration_ms);
        assertEquals(3, cramResource.getViewsInSubIntervals().size());

        ArrayList<Date> DateList = new ArrayList<>();
        DateList.add(new Date(1000));
        DateList.add(new Date(1500));
        DateList.add(new Date(2000));
        DateList.add(new Date(3000));
        DateList.add(new Date(3500));

        cramResource.addViewsInSubIntervals(DateList, noSubIntervals, subIntervalDuration_ms);

        // New subInterval is added
        cramResource.scheduleUpdateInResourceAccessStats(noSubIntervals, subIntervalDuration_ms);

        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(2).getViews());

        assertEquals(2000, cramResource.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(4000, cramResource.getViewsInSubIntervals().get(2).getStartOfInterval().getTime());
        assertEquals(5000, cramResource.getViewsInSubIntervals().get(2).getEndOfInterval().getTime());

        assertEquals((long) noSubIntervals, cramResource.getViewsInSubIntervals().size());
        assertEquals(3, (long) cramResource.getViewsInDefinedInterval());

        ArrayList<Date> newDateList = new ArrayList<>();
        newDateList.add(new Date(4000));
        cramResource.addViewsInSubIntervals(newDateList, noSubIntervals, subIntervalDuration_ms);

        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
        assertEquals((long) noSubIntervals, cramResource.getViewsInSubIntervals().size());
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

    @Test
    public void validNotificationsWithNoSubInterval() {
        CramResource cramResource = createCramResourceWithIntervals();
        cramResource.getViewsInSubIntervals().get(0).setViews(5);
        cramResource.getViewsInSubIntervals().get(1).setViews(5);
        cramResource.setViewsInDefinedInterval(10);

        Long subIntervalDuration_ms = 1000L;
        Long noSubIntervals = 4L;
        ArrayList<Date> dateList = new ArrayList<>();
        dateList.add(new Date(5010));

        cramResource.addViewsInSubIntervals(dateList, noSubIntervals, subIntervalDuration_ms);

        assertEquals(4, cramResource.getViewsInSubIntervals().size());
        assertEquals(1000, cramResource.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(2000, cramResource.getViewsInSubIntervals().get(0).getEndOfInterval().getTime());
        assertEquals(2000, cramResource.getViewsInSubIntervals().get(1).getStartOfInterval().getTime());
        assertEquals(3000, cramResource.getViewsInSubIntervals().get(1).getEndOfInterval().getTime());
        assertEquals(3000, cramResource.getViewsInSubIntervals().get(2).getStartOfInterval().getTime());
        assertEquals(4000, cramResource.getViewsInSubIntervals().get(2).getEndOfInterval().getTime());
        assertEquals(5010, cramResource.getViewsInSubIntervals().get(3).getStartOfInterval().getTime());
        assertEquals(6010, cramResource.getViewsInSubIntervals().get(3).getEndOfInterval().getTime());

        assertEquals(5, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(5, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(3).getViews());

        assertEquals(11, (int) cramResource.getViewsInDefinedInterval());

        // Add new valid notifications with no subInterval
        dateList.clear();
        dateList.add(new Date(6300));
        dateList.add(new Date(6500));

        cramResource.addViewsInSubIntervals(dateList, noSubIntervals, subIntervalDuration_ms);

        assertEquals(4, cramResource.getViewsInSubIntervals().size());
        assertEquals(2000, cramResource.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(3000, cramResource.getViewsInSubIntervals().get(0).getEndOfInterval().getTime());
        assertEquals(3000, cramResource.getViewsInSubIntervals().get(1).getStartOfInterval().getTime());
        assertEquals(4000, cramResource.getViewsInSubIntervals().get(1).getEndOfInterval().getTime());
        assertEquals(5010, cramResource.getViewsInSubIntervals().get(2).getStartOfInterval().getTime());
        assertEquals(6010, cramResource.getViewsInSubIntervals().get(2).getEndOfInterval().getTime());
        assertEquals(6300, cramResource.getViewsInSubIntervals().get(3).getStartOfInterval().getTime());
        assertEquals(7300, cramResource.getViewsInSubIntervals().get(3).getEndOfInterval().getTime());

        assertEquals(5, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(1, (long) cramResource.getViewsInSubIntervals().get(2).getViews());
        assertEquals(2, (long) cramResource.getViewsInSubIntervals().get(3).getViews());

        assertEquals(8, (int) cramResource.getViewsInDefinedInterval());
    }

    @Test
    public void futureNotificationsTest() {
        CramResource cramResource = createCramResourceWithIntervals();
        Long subIntervalDuration_ms = 1000L;
        Long noSubIntervals = 4L;
        ArrayList<Date> dateList = new ArrayList<>();

        Date notification1 = new Date();
        notification1.setTime(notification1.getTime() + 100000);
        Date notification2 = new Date();
        notification2.setTime(notification1.getTime() + 200000);

        dateList.add(notification1);
        dateList.add(notification2);

        cramResource.addViewsInSubIntervals(dateList, noSubIntervals, subIntervalDuration_ms);

        assertEquals(3, cramResource.getViewsInSubIntervals().size());
        assertEquals(1000, cramResource.getViewsInSubIntervals().get(0).getStartOfInterval().getTime());
        assertEquals(2000, cramResource.getViewsInSubIntervals().get(0).getEndOfInterval().getTime());
        assertEquals(2000, cramResource.getViewsInSubIntervals().get(1).getStartOfInterval().getTime());
        assertEquals(3000, cramResource.getViewsInSubIntervals().get(1).getEndOfInterval().getTime());
        assertEquals(3000, cramResource.getViewsInSubIntervals().get(2).getStartOfInterval().getTime());
        assertEquals(4000, cramResource.getViewsInSubIntervals().get(2).getEndOfInterval().getTime());

        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(0).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(1).getViews());
        assertEquals(0, (long) cramResource.getViewsInSubIntervals().get(2).getViews());

        assertEquals(0, (int) cramResource.getViewsInDefinedInterval());

    }

    @Test
    public void equals() {
        CramResource cramResource1 = new CramResource();
        cramResource1.setId("id1");
        cramResource1.setName("Name");
        cramResource1.setDescription(Arrays.asList("comment1", "comment2"));
        cramResource1.setInterworkingServiceURL("resource1.com");
        cramResource1.setPlatformId("p1");
        cramResource1.setResourceUrl("url");
        cramResource1.setType(CoreResourceType.ACTUATOR);
        cramResource1.setViewsInDefinedInterval(5);
        cramResource1.setViewsInSubIntervals(Arrays.asList(new SubIntervalViews(new Date(0), new Date(4), 4),
                new SubIntervalViews(new Date(0), new Date(4), 6)));

        CramResource cramResource2 = new CramResource(cramResource1);

        CramResource cramResource3 = new CramResource(cramResource1);
        cramResource3.setType(CoreResourceType.DEVICE);

        CramResource cramResource4 = new CramResource(cramResource1);
        cramResource4.setName("name4");

        CramResource cramResource5 = new CramResource(cramResource1);
        cramResource5.getViewsInSubIntervals().set(1, new SubIntervalViews(new Date(0), new Date(4), 4));

        assertEquals(true, cramResource1.equals(cramResource2));
        assertEquals(false, cramResource1.equals(cramResource3));
        assertEquals(false, cramResource1.equals(cramResource4));
        assertEquals(false, cramResource1.equals(cramResource5));
    }

    private CramResource createCramResourceWithIntervals() {
        SubIntervalViews subInterval1 = new SubIntervalViews(new Date(1000), new Date(2000), 0);
        SubIntervalViews subInterval2 = new SubIntervalViews(new Date(2000), new Date(3000), 0);
        SubIntervalViews subInterval3 = new SubIntervalViews(new Date(3000), new Date(4000), 0);
        ArrayList<SubIntervalViews> subIntervals = new ArrayList<>();
        CramResource cramResource = new CramResource();

        subIntervals.add(subInterval1);
        subIntervals.add(subInterval2);
        subIntervals.add(subInterval3);
        cramResource.setViewsInSubIntervals(subIntervals);
        cramResource.setViewsInDefinedInterval(0);

        return cramResource;
    }
}
