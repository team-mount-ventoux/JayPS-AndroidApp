package com.njackson.live;

import android.location.Location;
import android.util.Log;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LiveTrackingFriend {
    private static final String TAG = "PB-LiveTrackingFriend";

    public int number = 0;
    public String id = "";
    public String nickname = "";
    public Double lat = null, lon = null;
    long ts = 0;
    long dt = -1;
    private long _receivedTimestamp = 0;
    float deltaDistance = 0.0f, bearing = 0.0f;
    private Location _location;

    public LiveTrackingFriend() {
        _location = new Location("JayPS");
    }
    public String toString() {
        return id + " " + _receivedTimestamp + " " + lat + "/" + lon + "--" + ts + "//" + dt;
    }
    public boolean setFromNodeList(NodeList friendChildNodes) {
        id = "";
        for( int j = 0; j < friendChildNodes.getLength(); j++ ) {
            // for each fields

            Node item = friendChildNodes.item( j );
            String nodeName = item.getNodeName();
            if (nodeName.equals("id")) {
                _receivedTimestamp = System.currentTimeMillis() / 1000;
                id = item.getChildNodes().item(0).getNodeValue();
            }
            if (nodeName.equals("nickname")) {
                nickname = item.getChildNodes().item(0).getNodeValue();
            }
            if (nodeName.equals("lat")) {
                lat = Double.valueOf(item.getChildNodes().item(0).getNodeValue());
            }
            if (nodeName.equals("lon")) {
                lon = Double.valueOf(item.getChildNodes().item(0).getNodeValue());
            }
            if (nodeName.equals("ts")) {
                ts = Long.valueOf(item.getChildNodes().item(0).getNodeValue());
            }
        }
        return id != "";
    }
    public boolean updateFromFriend(LiveTrackingFriend friend, Location lastlocation) {
        if ((id == "") || !friend.id.equals(this.id)) {
            Log.e(TAG, "updateFromFriend this " + this.toString());
            Log.e(TAG, "updateFromFriend friend "+friend.toString());
            return false;
        }
        dt = friend.ts-ts;
        //Log.d(TAG, "dt:"+ts+"->"+friend.ts+" "+dt+"s");
        ts = friend.ts;
        lat = friend.lat;
        lon = friend.lon;
        _location.setLatitude(lat);
        _location.setLongitude(lon);

        deltaDistance = lastlocation.distanceTo(_location);
        bearing = lastlocation.bearingTo(_location);
        return true;
    }
    public Location getLocation() {
        return _location;
    }
}
