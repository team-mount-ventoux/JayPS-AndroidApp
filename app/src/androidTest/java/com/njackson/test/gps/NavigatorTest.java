package com.njackson.test.gps;

import android.location.Location;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.njackson.gps.Navigator;

public class NavigatorTest extends AndroidTestCase {

    @SmallTest
    public void test1() throws Exception {
        Navigator nav = new Navigator();
        nav.debugLevel = 2;
        Location[] locs = new Location[4];

        int i = 0;

        locs[i] = new Location("Ventoo"); locs[i].setLatitude(43.00); locs[i].setLongitude(0.00); i++;
        locs[i] = new Location("Ventoo"); locs[i].setLatitude(43.01); locs[i].setLongitude(0.07); i++;
        locs[i] = new Location("Ventoo"); locs[i].setLatitude(43.00); locs[i].setLongitude(0.09); i++;
        locs[i] = new Location("Ventoo"); locs[i].setLatitude(43.02); locs[i].setLongitude(0.12); i++;

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
        assertEquals(0.0f, nav.getDistanceToDestination());

        nav.onLocationChanged(addDeltaMin(locs[3]));
        assertEquals(4, nav.getNextIndex());
        assertEquals(0.0f, nav.getDistanceToDestination());

        // restart nav
        nav.onLocationChanged(addDelta(locs[1]));
        assertEquals(1, nav.getNextIndex());

        nav.onLocationChanged(addDeltaMin(locs[1]));
        assertEquals(2, nav.getNextIndex());
    }

    Location addDelta(Location loc) {
        Location result = new Location("Ventoo");
        result.setLatitude(loc.getLatitude()+0.001);
        result.setLongitude(loc.getLongitude()+0.001);
        return result;
    }
    Location addDeltaMin(Location loc) {
        Location result = new Location("Ventoo");
        result.setLatitude(loc.getLatitude()+0.0001);
        result.setLongitude(loc.getLongitude()+0.0001);
        return result;
    }
    Location middle(Location loc1, Location loc2) {
        Location result = new Location("Ventoo");
        result.setLatitude((loc1.getLatitude()+loc2.getLatitude())/2);
        result.setLongitude((loc1.getLongitude()+loc2.getLongitude())/2);
        return result;
    }
}
