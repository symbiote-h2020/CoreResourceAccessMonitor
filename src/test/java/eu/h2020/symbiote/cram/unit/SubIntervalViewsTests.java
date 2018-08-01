package eu.h2020.symbiote.cram.unit;

import eu.h2020.symbiote.cram.model.SubIntervalViews;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Vasileios Glykantzis (ICOM)
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SubIntervalViewsTests {

    @Test
    public void belongsToSubIntervalDateTest(){
        SubIntervalViews subInterval = new SubIntervalViews(new Date(1000), new Date(2000), 0);

        assertTrue(subInterval.belongsToSubInterval(new Date(1500)));
        assertFalse(subInterval.belongsToSubInterval(new Date(2500)));
        assertTrue(subInterval.belongsToSubInterval(new Date(1000)));
        assertFalse(subInterval.belongsToSubInterval(new Date(2000)));
    }

    @Test
    public void belongsToSubIntervalLongTest(){
        SubIntervalViews subInterval = new SubIntervalViews(new Date(1000), new Date(2000), 0);

        assertTrue(subInterval.belongsToSubInterval(1500));
        assertFalse(subInterval.belongsToSubInterval(2500));
        assertTrue(subInterval.belongsToSubInterval(1000));
        assertFalse(subInterval.belongsToSubInterval(2000));
    }

    @Test
    public void increaseViewsTest() {
        SubIntervalViews subInterval = new SubIntervalViews(new Date(1000), new Date(2000), 1000);
        subInterval.increaseViews(5);

        assertEquals(new Integer(1005), subInterval.getViews());
    }

    @Test
    public void equals() {
        SubIntervalViews subIntervalViews1 = new SubIntervalViews(new Date(0), new Date(4), 4);
        SubIntervalViews subIntervalViews2 = new SubIntervalViews(new Date(0), new Date(4), 4);
        SubIntervalViews subIntervalViews3 = new SubIntervalViews(new Date(1), new Date(4), 4);

        assertEquals(subIntervalViews2, subIntervalViews1);
        assertNotEquals(subIntervalViews3, subIntervalViews1);
    }

}
