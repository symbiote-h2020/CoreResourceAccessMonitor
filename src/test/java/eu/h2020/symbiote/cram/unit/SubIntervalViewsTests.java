package eu.h2020.symbiote.cram.unit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.h2020.symbiote.cram.model.SubIntervalViews;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Vasileios Glykantzis (ICOM)
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class SubIntervalViewsTests {

    @Test
    public void belongsToSubIntervalDateTest(){
        SubIntervalViews subInterval = new SubIntervalViews();

        subInterval.setStartOfInterval(new Date(1000));
        subInterval.setEndOfInterval(new Date(2000));

        assertEquals(true, subInterval.belongsToSubInterval(new Date(1500)));
        assertEquals(false, subInterval.belongsToSubInterval(new Date(2500)));
        assertEquals(true, subInterval.belongsToSubInterval(new Date(1000)));
        assertEquals(false, subInterval.belongsToSubInterval(new Date(2000)));
    }

    @Test
    public void belongsToSubIntervalLongTest(){
        SubIntervalViews subInterval = new SubIntervalViews();

        subInterval.setStartOfInterval(new Date(1000));
        subInterval.setEndOfInterval(new Date(2000));

        assertEquals(true, subInterval.belongsToSubInterval(1500));
        assertEquals(false, subInterval.belongsToSubInterval(2500));
        assertEquals(true, subInterval.belongsToSubInterval(1000));
        assertEquals(false, subInterval.belongsToSubInterval(2000));
    }

    @Test
    public void increaseViewsTest(){
        SubIntervalViews subInterval = new SubIntervalViews();

        subInterval.setViews(1000);
        subInterval.increaseViews(5);

        assertEquals(new Integer(1005), subInterval.getViews());
    }

}
