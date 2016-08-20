package com.njackson.gps;


import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

@Singleton
public class Navigator {

    private static final String TAG = "PB-Navigator";

    public int debugLevel = 1;

    protected class Poi extends Location {
        public float distance = 0;
        public int index = 0;

        public Poi(Location l) {
            super(l);
        }
    }

    private Poi[] _pointsIni;
    private int _nbPointsIni = 0;
    private Poi[] _pointsSimpl;
    private int _nbPointsSimpl = 0;
    private float _nextDistance = 0;
    private float _nextBearing = 0;
    private int _nextIndex = -1;
    private float _error = 0;
    private final float MIN_DIST = 1000000;
    private Location _lastSeenLoc = null;
    private float _lastSeenDist = 0;

    public Navigator() {
    }

    public void onLocationChanged(Location location) {
        if (_nbPointsIni == 0) {
            return;
        }
        Log.d(TAG, "onLocationChanged: lat:"+location.getLatitude()+",lon:"+location.getLongitude() + " nbPointsSimpl:" + _nbPointsSimpl);
        int closestPoint = -1;

        if (_nextIndex < 0 ||  _nextIndex >= _nbPointsSimpl) {
            Log.d(TAG, "Next point not yet defined");
            closestPoint = searchClosestPoint(location, null, 0, 0, -1);
            selectNewNextPoint(location, closestPoint);
        }

        if (_nextIndex >= 0) {
            float distToNextIndex = location.distanceTo(_pointsSimpl[_nextIndex]);
            if (distToNextIndex < 50) {
                Log.d(TAG, "Reach next point #" + _nextIndex + " d:" + distToNextIndex);
                if (_nextIndex == _nbPointsSimpl - 1) {
                    Log.d(TAG, "Destination reached!");
                    _nextDistance = 0;
                    _nextIndex = closestPoint = _nbPointsSimpl; // special meaning
                } else {
                    closestPoint = searchClosestPoint(location, null, 0, _nextIndex + 1, -1);
                }
            } else {
                // continue to look for current next point
                closestPoint = searchClosestPoint(location, _lastSeenLoc, _lastSeenDist, 0, _nextIndex);
            }
        }

        if (_nextIndex != closestPoint) {
            selectNewNextPoint(location, closestPoint);
        }
        // compute distance to next point
        if (_nextIndex >= 0 && _nextIndex < _nbPointsSimpl) {
            _nextDistance = location.distanceTo(_pointsSimpl[_nextIndex]);
            _nextBearing = (location.bearingTo(_pointsSimpl[_nextIndex]) + 360) % 360;
            _error = _nextIndex > 0 ? crossTrackError(_pointsSimpl[_nextIndex-1], _pointsSimpl[_nextIndex], location) : 0;
            Log.d(TAG, _nextIndex + "[" + _pointsSimpl[_nextIndex].index + "] dist:" + _nextDistance + " bearing:" + _nextBearing + " error:" + _error);
        } else {
            _nextDistance = 0;
            Log.d(TAG, "No _nextIndex (" + _nextIndex + ")");
        }
    }

    /**
     * Search for closest point, excluding points around excludeCenter (radius exludeRadius)
     * @param location
     * @param excludeCenter
     * @param exludeRadius
     * @return index or -1
     */
    private int searchClosestPoint(Location location, Location excludeCenter, float exludeRadius, int firstIndex, int expectedIndex) {
        Log.d(TAG, "searchClosestPoint exludeRadius:" + exludeRadius + " firstIndex:" + firstIndex + " expectedIndex:" + expectedIndex);
        int minIndex = -1;
        float minDist = MIN_DIST;
        for(int i = firstIndex; i < _nbPointsSimpl; i++) {
            float dist = location.distanceTo(_pointsSimpl[i]);
            if (debugLevel > 1) Log.d(TAG, i + "[" + _pointsSimpl[i].index + "] dist:" + dist + (excludeCenter != null ? " ex:" + excludeCenter.distanceTo(_pointsSimpl[i]) : ""));
            if (dist < minDist) {
                if (excludeCenter != null && excludeCenter.distanceTo(_pointsSimpl[i]) <= exludeRadius && i != expectedIndex) {
                    Log.d(TAG, "exclude #" + i + " d:" + excludeCenter.distanceTo(_pointsSimpl[i]) + "<=" + exludeRadius);
                } else {
                    minDist = dist;
                    minIndex = i;
                }
            }
        }
        Log.d(TAG, "searchClosestPoint(>=#" + firstIndex + "): #:" + minIndex + " d:" + minDist);
        return minIndex;
    }
    private void selectNewNextPoint(Location location, int newNextIndex) {
        if (newNextIndex >= 0 && newNextIndex < _nbPointsSimpl) {
            Log.d(TAG, "New _nextIndex: " + _nextIndex + "=>" + newNextIndex);
            _nextIndex = newNextIndex;
            _lastSeenLoc = location;
            _lastSeenDist = location.distanceTo(_pointsSimpl[newNextIndex]);
        }
    }
    public void loadGpx(String gpx) {
        _nbPointsIni = _nbPointsSimpl = 0;
        _nextIndex = -1;
        _nextDistance = 0;
        _lastSeenLoc = null;
        _lastSeenDist = 0;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new ByteArrayInputStream(gpx.getBytes("utf-8"))));

            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "";
            NodeList nodes;
            Node node;
            expression = "//trkpt";
            nodes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            Log.d(TAG, "length:" + nodes.getLength());
            float distance = 0;
            _pointsIni = new Poi[nodes.getLength()];
            Location loc = new Location("Ventoo");
            for (int i = 0; i < nodes.getLength(); i++) {
                node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    //Log.d(TAG, "lat  " + eElement.getAttribute("lat"));
                    //Log.d(TAG, "ele: " + eElement.getElementsByTagName("ele").item(0).getTextContent());
                    try {
                        loc.setLatitude(Float.parseFloat(eElement.getAttribute("lat")));
                        loc.setLongitude(Float.parseFloat(eElement.getAttribute("lon")));
                        _pointsIni[_nbPointsIni] = new Poi(loc);
                        _pointsIni[_nbPointsIni].index = i;
                        if (_nbPointsIni > 0) {
                            distance += _pointsIni[_nbPointsIni].distanceTo(_pointsIni[_nbPointsIni-1]);
                            _pointsIni[_nbPointsIni].distance = distance;
                        }
                        _nbPointsIni++;
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Exception:" + e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
        Log.d(TAG, "nbPointsIni:" + _nbPointsIni);
        simplifyRoute();

        // debug
        //if (_nbPointsSimpl > 15) {
        //    onLocationChanged(_pointsSimpl[0]);
        //    onLocationChanged(_pointsSimpl[10]);
        //    onLocationChanged(_pointsSimpl[_nbPointsSimpl-1]);
        //}

    }
    public void addPoints(Location[] locs) {
        _pointsIni = new Poi[locs.length];
        _nbPointsIni = 0;
        for(int i = 0; i < locs.length; i++) {
            _pointsIni[_nbPointsIni] = new Poi(locs[i]);
            _pointsIni[_nbPointsIni].index = _nbPointsIni;
            if (_nbPointsIni > 0) {
                _pointsIni[_nbPointsIni].distance = _pointsIni[_nbPointsIni - 1].distance + _pointsIni[_nbPointsIni].distanceTo(_pointsIni[_nbPointsIni - 1]);
            }
            _nbPointsIni++;
        }
    }
    public void simplifyRoute() {
        Log.d(TAG, "simplifyRoute nbPointsIni:" + _nbPointsIni);
        if (_nbPointsIni == 0) {
            return;
        }
        int lastIndex = 0;
        int lastIndex2 = 0;
        StringBuilder debug = new StringBuilder();
        _pointsSimpl = new Poi[_nbPointsIni];

        // force 1st point
        _pointsSimpl[0] = _pointsIni[0];
        _nbPointsSimpl = 1;
        for(int i = 1; i < _nbPointsIni - 1; i++) {
            float bearingLastToMe = (_pointsIni[lastIndex].bearingTo(_pointsIni[i]) + 360) % 360;
            float bearingLastToNext = (_pointsIni[lastIndex].bearingTo(_pointsIni[i + 1]) + 360) % 360;
            float bearingMeToNext = (_pointsIni[i].bearingTo(_pointsIni[i + 1]) + 360) % 360;
            boolean keep = false;
            float diff1 = 0;
            float diff2 = 0;
            String tmp = "";
            diff1 = bearingLastToNext - bearingLastToMe;
            diff2 = bearingMeToNext - bearingLastToMe;
            if (lastIndex != i - 1) {
                if (Math.abs(diff1) > 5) {
                    tmp += "a"+Math.round(Math.abs(diff1));
                    keep = true;
                }
            }
            if (Math.abs(diff2) > 30) {
                tmp += "b"+Math.round(Math.abs(diff2));
                keep = true;
            }
            Log.d(TAG, i + ": " + lastIndex2 + "-" + lastIndex + " dist:" + Math.round(_pointsIni[i].distance) + " b1:" + Math.round(bearingLastToMe) + " b2:" + Math.round(bearingLastToNext) + " b3:" + Math.round(bearingMeToNext) + " d1:" + Math.round(diff1) + " d2:" + Math.round(diff2) + " " + (keep ? ("KEEP "  + _pointsIni[i].getLatitude() + "," + _pointsIni[i].getLongitude()) : "REMOVE"));
            if (keep) {
                debug.append(i + ": " + lastIndex2 + "-" + lastIndex + tmp +", "  + _pointsIni[i].getLatitude() + "," + _pointsIni[i].getLongitude()+"\n");
                lastIndex2 = lastIndex;
                lastIndex = i;
                _pointsSimpl[_nbPointsSimpl] = _pointsIni[i];
                _nbPointsSimpl++;
            }
        }
        // force last point
        _pointsSimpl[_nbPointsSimpl] = _pointsIni[_nbPointsIni - 1];
        _nbPointsSimpl++;
        Log.d(TAG, "simplifyRoute nbPointsIni:" + _nbPointsIni + " nbPointsSimpl:" + _nbPointsSimpl);
        Log.d(TAG, debug.toString());
    }
    public void loadRouteToOrux(Activity activity) {

        //Map offline
        Intent i = new Intent("com.oruxmaps.VIEW_MAP_OFFLINE");
        //Map online
        //Intent i = new Intent("com.oruxmaps.VIEW_MAP_ONLINE");
        // Route Waypoints

        double[] targetLat = new double[_nbPointsSimpl];
        double[] targetLon = new double[_nbPointsSimpl];
        String[] targetNames = new String[_nbPointsSimpl];
        int[] targetTypes = new int[_nbPointsSimpl];

        for(int j = 0; j < _nbPointsSimpl; j++) {
            boolean add = false;
            if (j == _nextIndex) {
                // next point
                targetTypes[j] = 1;
                add = true;
            } else if (j == _nbPointsSimpl - 1) {
                // destination
                targetTypes[j] = 15;
                add = true;
            } else {
                //targetTypes[j] = 4;
            }
            if (add) {
                targetLat[j] = _pointsSimpl[j].getLatitude();
                targetLon[j] = _pointsSimpl[j].getLongitude();
                targetNames[j] = "pt" + j;
            }
        }
        i.putExtra("targetLat", targetLat);
        i.putExtra("targetLon", targetLon);
        i.putExtra("targetName", targetNames);
        i.putExtra("targetType", targetTypes);
        //i.putExtra("navigatetoindex", _nbPointsSimpl-1);
        //index of the wpt. you want to start wpt. navigation.
        //Track points,
        double[] targetLatPoints = new double[_nbPointsIni];
        double[] targetLonPoints = new double[_nbPointsIni];
        for(int j = 0; j < _nbPointsIni; j++) {
            targetLatPoints[j] = _pointsIni[j].getLatitude();
            targetLonPoints[j] = _pointsIni[j].getLongitude();
        }
        i.putExtra("targetLatPoints", targetLatPoints);
        i.putExtra("targetLonPoints", targetLonPoints);
        Log.e(TAG, "Avant startActivity:");
        activity.startActivity(i);
    }


    private static float R = 6371000; // radius of earth (meter)
    private static float DTR = 1.0f / 180 * 3.14159f;    // conversion degrees -> radians

    static float crossTrackError(Location p1, Location p2, Location p3) {
        // distance of a point from a great-circle path (sometimes called cross track error).
        // http://www.movable-type.co.uk/scripts/latlong.html#cross-track
        // var dXt = Math.asin(Math.sin(d13/R)*Math.sin(θ13-θ12)) * R;
        // http://stackoverflow.com/questions/1051723/distance-from-point-to-line-great-circle-function-not-working-right
        // The along-track distance, from the start point to the closest point on the path to the third point, is
        // var dAt = Math.acos(Math.cos(d13/R)/Math.cos(dXt/R)) * R;

        float d13 = p1.distanceTo(p3);
        float t13 = p1.bearingTo(p3);
        float t12 = p1.bearingTo(p2);
        float dXt = (float) (Math.asin(Math.sin(d13/R) * Math.sin((t13-t12) * DTR)) * R);
        float dAt = (float) (Math.acos(Math.cos(d13/R) / Math.cos(dXt / R)) * R);
        return dXt;
    }

    public float getDistanceToDestination() {
        return _nbPointsSimpl > 0 && _nextIndex >= 0 && _nextIndex < _nbPointsSimpl ? (_pointsSimpl[_nbPointsSimpl-1].distance - _pointsSimpl[_nextIndex].distance + _nextDistance) : 0;
    }
    public float getNextDistance() {
        return _nextDistance;
    }
    public float getNextBearing() {
        return _nextBearing;
    }
    public int getNextIndex() {
        return _nextIndex;
    }
    public float getError() {
        return _error;
    }
    public int getNbPoints() {
        return _nbPointsSimpl;
    }
    public Location getPoint(int i) {
        if ((_nextIndex + i >= 0) && (_nextIndex + i < _nbPointsSimpl)) {
            return _pointsSimpl[_nextIndex + i];
        }
        return null;
    }
}
