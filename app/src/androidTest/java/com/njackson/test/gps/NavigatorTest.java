package com.njackson.test.gps;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.Constants;
import com.njackson.gps.Navigator;

public class NavigatorTest extends AndroidTestCase {

    @SmallTest
    public void test1() throws Exception {
        Navigator nav = new Navigator();
        nav.debugLevel = 2;
        Location[] locs = new Location[4];

        int i = 0;

        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.00); locs[i].setLongitude(0.00); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.01); locs[i].setLongitude(0.07); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.00); locs[i].setLongitude(0.09); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.02); locs[i].setLongitude(0.12); i++;

        nav.addPoints(locs);
        nav.simplifyRoute();
        assertEquals(i, nav.getNbPoints());

        nav.onLocationChanged(locs[0]);
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(addDelta(locs[0]));
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(middle(locs[0], locs[1]));
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(addDelta(locs[1]));
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(addDeltaMin(locs[1]));
        assertEquals(2, nav.getNextIndex());

        nav.onLocationChanged(middle(locs[1], locs[2]));
        assertEquals(2, nav.getNextIndex());

        nav.onLocationChanged(addDeltaMin(locs[0]));
        assertEquals(0, nav.getNextIndex());

        nav.onLocationChanged(middle(locs[0], locs[3]));
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(addDelta(locs[3]));
        assertEquals(3, nav.getNextIndex());

        nav.onLocationChanged(addDeltaMin(locs[3]));
        assertEquals(4, nav.getNextIndex());
        assertEquals(0.0f, nav.getDistanceToDestination(Constants.METRIC));

        nav.onLocationChanged(addDeltaMin(locs[3]));
        assertEquals(4, nav.getNextIndex());
        assertEquals(0.0f, nav.getDistanceToDestination(Constants.METRIC));

        // restart nav
        nav.onLocationChanged(addDelta(locs[1]));
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(addDeltaMin(locs[1]));
        assertEquals(2, nav.getNextIndex());
    }

    @SmallTest
    public void test_return_path() throws Exception {
        Navigator nav = new Navigator();
        nav.debugLevel = 2;
        Location[] locs = new Location[6];

        int i = 0;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.00); locs[i].setLongitude(0.00); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.01); locs[i].setLongitude(0.07); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.00); locs[i].setLongitude(0.09); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.02); locs[i].setLongitude(0.12); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.0001); locs[i].setLongitude(0.0901); i++;
        locs[i] = new Location("JayPS"); locs[i].setLatitude(43.00); locs[i].setLongitude(0.00); i++;

        nav.addPoints(locs);
        nav.simplifyRoute();
        assertEquals(i, nav.getNbPoints());

        nav.onLocationChanged(locs[0]);
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(locs[1]);
        assertEquals(2, nav.getNextIndex());

        nav.onLocationChanged(addDeltaMin(locs[2]));
        assertEquals(3, nav.getNextIndex()); // and not 4, closest

        nav.onLocationChanged(locs[3]);
        assertEquals(4, nav.getNextIndex());

        nav.onLocationChanged(locs[2]); // 2 close to 4
        assertEquals(5, nav.getNextIndex());

        nav.onLocationChanged(addDeltaMin(locs[5]));
        assertEquals(6, nav.getNextIndex());
        assertEquals(0.0f, nav.getDistanceToDestination(Constants.METRIC));

        // restart nav
        nav.onLocationChanged(addDelta(locs[1]));
        assertEquals(1, nav.getNextIndex());

    }

    Location addDelta(Location loc) {
        Location result = new Location("JayPS");
        result.setLatitude(loc.getLatitude()+0.001);
        result.setLongitude(loc.getLongitude()+0.001);
        return result;
    }
    Location addDeltaMin(Location loc) {
        Location result = new Location("JayPS");
        result.setLatitude(loc.getLatitude()+0.0001);
        result.setLongitude(loc.getLongitude()+0.0001);
        return result;
    }
    Location middle(Location loc1, Location loc2) {
        Location result = new Location("JayPS");
        result.setLatitude((loc1.getLatitude()+loc2.getLatitude())/2);
        result.setLongitude((loc1.getLongitude()+loc2.getLongitude())/2);
        return result;
    }
}
