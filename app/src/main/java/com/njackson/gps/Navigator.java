package com.njackson.gps;


import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.njackson.Constants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

@Singleton
public class Navigator {

    private static final String TAG = "PB-Navigator";

    public int debugLevel = 0;

    protected class Poi extends Location {
        public float distance = 0;
        public int index = 0;
        public String name;
        public String desc;

        public Poi(Location l) {
            super(l);
        }
    }
    protected class Climb {
        public Poi start;
        public Poi end;
        int ascent;
        float dist;
    }

    private Poi[] _pointsIni;
    private int _nbPointsIni = 0;
    private Poi[] _pointsSimpl;
    private int _nbPointsSimpl = 0;
    private List<Climb> _climbs;
    private List<Poi> _wpts;
    private float _nextDistance = 0;
    private float _nextBearing = 0;
    private int _nextIndex = -1;
    private float _error = 0;
    private final float MIN_DIST = 1000000;
    private Location _lastSeenLoc = null;
    private float _lastSeenDist = 0;
    private int lastClimbMessage = 0;
    private int lastWptMessage = 0;

    public static final int MIN_ASCENT_VARIATION = 25;
    public static final int MIN_ASCENT_CLIMB = 40;

    public static final int MIN_DIST_ALERT_CLIMB = 50;
    public static final int MIN_DIST_ALERT_WPT = 50;

    public Navigator() {
        _climbs = new ArrayList<>();
        _wpts = new ArrayList<>();
        clearRoute();
    }

    public void onLocationChanged(Location location) {
        if (_nbPointsIni == 0) {
            return;
        }
        Log.d(TAG, "onLocationChanged: lat:"+location.getLatitude()+",lon:"+location.getLongitude() + " nbPointsSimpl:" + _nbPointsSimpl);
        int closestPoint = -1;

        if (_nextIndex < 0 ||  _nextIndex >= _nbPointsSimpl) {
            Log.d(TAG, "Next point not yet defined");
            closestPoint = searchClosestPoint(location, null, 0, 0, -1, -1);
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
                    closestPoint = searchClosestPoint(location, null, 0, _nextIndex + 1, -1, -1);
                    if (closestPoint >= 0) {
                        float distanceOnRoute = _pointsSimpl[closestPoint].distance - _pointsSimpl[_nextIndex].distance;
                        float distanceDirect = _pointsSimpl[closestPoint].distanceTo(_pointsSimpl[_nextIndex]);
                        Log.d(TAG, "1. #" + _nextIndex + "=>#" + closestPoint + " distanceOnRoute:" + distanceOnRoute + " distanceDirect:" + distanceDirect);
                        if (distanceOnRoute > 2 * distanceDirect && distanceOnRoute > 300) {
                            Log.d(TAG, "closestPoint too far ahead");
                            int closestPoint2 = searchClosestPoint(location, null, 0, _nextIndex + 1, -1, closestPoint - 1);
                            if (closestPoint2 >= 0) {
                                distanceOnRoute = _pointsSimpl[closestPoint2].distance - _pointsSimpl[_nextIndex].distance;
                                distanceDirect = _pointsSimpl[closestPoint2].distanceTo(_pointsSimpl[_nextIndex]);
                                Log.d(TAG, "2. #" + _nextIndex + "=>#" + closestPoint2 + " distanceOnRoute:" + distanceOnRoute + " distanceDirect:" + distanceDirect);
                                Log.d(TAG,"Replace #" + closestPoint + " by #" + closestPoint2);
                                closestPoint = closestPoint2;
                            }
                        }
                    }
                }
            } else {
                // continue to look for current next point
                closestPoint = searchClosestPoint(location, _lastSeenLoc, _lastSeenDist, 0, _nextIndex, -1);
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
    private int searchClosestPoint(Location location, Location excludeCenter, float exludeRadius, int firstIndex, int expectedIndex, int maxIndex) {
        if (maxIndex < 0 || maxIndex >= _nbPointsSimpl) {
            maxIndex = _nbPointsSimpl-1;
        }
        Log.d(TAG, "searchClosestPoint exludeRadius:" + exludeRadius + " firstIndex:" + firstIndex + " expectedIndex:" + expectedIndex + " maxIndex:" + maxIndex);
        int minIndex = -1;
        float minDist = MIN_DIST;

        for(int i = firstIndex; i <= maxIndex; i++) {
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
    public String[] messageClimb(Location location) {
        String[] result = new String[2];
        String title = "";
        String message = "";
        int newClimbMessage = 0;

        int nb = 0;
        for (Climb climb : _climbs) {
            float distStart = location.distanceTo(climb.start); // in meters
            float distEnd = location.distanceTo(climb.end); // in meters
            if (debugLevel > 1) Log.d(TAG, "climb #" + nb + " start:" + distStart + "m end:" + distEnd);
            if (distStart < MIN_DIST_ALERT_CLIMB) {
                title = "Start climb " + (nb+1) + "/" + _climbs.size();
                message = displayClimb(nb);
                newClimbMessage = 10 * nb + 1;
            }
            if (distEnd < MIN_DIST_ALERT_CLIMB) {
                title = "End climb #" + (nb+1) + "/" + _climbs.size();
                message = displayClimb(nb);
                newClimbMessage = 10 * nb + 2;
            }
            nb++;
        }
        if (newClimbMessage == lastClimbMessage) {
            if (debugLevel > 0) Log.d(TAG, "Skip message: " + message + " same newClimbMessage:" + newClimbMessage);
            title = message = "";
        }
        lastClimbMessage = newClimbMessage;
        if (message != "") {
            Log.d(TAG, "messageClimb(): " + title + " - " + message);
        }
        result[0] = title;
        result[1] = message;
        return result;
    }

    public String[] messageWpt(Location location) {
        String[] result = new String[2];
        String title = "";
        String message = "";
        int newWptMessage = 0;

        float minDist = MIN_DIST;
        int nb = 0;
        for (Poi wpt : _wpts) {
            float dist = location.distanceTo(wpt); // in meters
            if (debugLevel > 0) Log.d(TAG, "wpt #" + nb + " dist:" + dist + "m");
            if (dist < minDist) {
                if (dist < MIN_DIST_ALERT_WPT) {
                    title = "Point " + (nb+1) + "/" + _wpts.size();
                    message = wpt.name + " " + wpt.desc;
                    newWptMessage = 10 * nb + 1;
                }
                minDist = dist;
            }
            nb++;
        }
        if (newWptMessage == lastWptMessage) {
            if (debugLevel > 0) Log.d(TAG, "Skip message: " + message + " same newWptMessage:" + newWptMessage);
            title = message = "";
        }
        lastWptMessage = newWptMessage;
        if (message != "") {
            Log.d(TAG, "messageWpt(): " + title + " - " + message);
        }
        result[0] = title;
        result[1] = message;
        return result;
    }

    public void clearRoute() {
        _nbPointsIni = _nbPointsSimpl = 0;
        _climbs.clear();
        _wpts.clear();
        _nextIndex = -1;
        _nextDistance = 0;
        _lastSeenLoc = null;
        _lastSeenDist = 0;
        lastClimbMessage = 0;
        lastWptMessage = 0;
    }
    public void loadGpx(String gpx) {
        clearRoute();
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
            Location loc = new Location("JayPS");
            for (int i = 0; i < nodes.getLength(); i++) {
                node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    //Log.d(TAG, "lat  " + eElement.getAttribute("lat"));
                    //Log.d(TAG, "ele: " + eElement.getElementsByTagName("ele").item(0).getTextContent());
                    try {
                        loc.setLatitude(Float.parseFloat(eElement.getAttribute("lat")));
                        loc.setLongitude(Float.parseFloat(eElement.getAttribute("lon")));
                        loc.setAltitude(Float.parseFloat(eElement.getElementsByTagName("ele").item(0).getTextContent()));
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


            expression = "//wpt";
            nodes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);
            Log.d(TAG, "length wpt:" + nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                node = nodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    //Log.d(TAG, "lat  " + eElement.getAttribute("lat"));
                    //Log.d(TAG, "ele: " + eElement.getElementsByTagName("ele").item(0).getTextContent());
                    try {
                        loc.setLatitude(Float.parseFloat(eElement.getAttribute("lat")));
                        loc.setLongitude(Float.parseFloat(eElement.getAttribute("lon")));
                        //loc.setAltitude(Float.parseFloat(eElement.getElementsByTagName("ele").item(0).getTextContent()));
                        Poi wpt = new Poi(loc);
                        wpt.name = eElement.getElementsByTagName("name").item(0).getTextContent();
                        wpt.desc = android.text.Html.fromHtml(eElement.getElementsByTagName("desc").item(0).getTextContent()).toString();
                        _wpts.add(wpt);
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
        delectClimbs();

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
            float max_error = 0;
            for(int j = lastIndex; j < i; j++) {
                float error = crossTrackError(_pointsIni[lastIndex], _pointsIni[i], _pointsIni[j]);
                if (error > max_error) {
                    max_error = error;
                }
            }
            //Log.d(TAG, i + ": " + max_error);
            if (Math.abs(max_error) > 50) {
                tmp += "c" + Math.round(Math.abs(max_error));
                if (!keep) {
                    Log.d(TAG, i + ": keep due to error " + tmp);
                    keep = true;
                }
            }
            if (debugLevel > 1) Log.d(TAG, i + ": " + lastIndex2 + "-" + lastIndex + " dist:" + Math.round(_pointsIni[i].distance) + " b1:" + Math.round(bearingLastToMe) + " b2:" + Math.round(bearingLastToNext) + " b3:" + Math.round(bearingMeToNext) + " d1:" + Math.round(diff1) + " d2:" + Math.round(diff2) + " err:" + Math.round(max_error) + " " + (keep ? ("KEEP "  + _pointsIni[i].getLatitude() + "," + _pointsIni[i].getLongitude()) : "REMOVE"));
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
        if (debugLevel >= 1) Log.d(TAG, debug.toString());
    }
    public void delectClimbs() {
        Log.d(TAG, "delectClimbs nbPointsIni:" + _nbPointsIni);
        if (_nbPointsIni == 0) {
            return;
        }
        StringBuilder debug = new StringBuilder();
        _climbs.clear();
        double alt, alt_temp = 0, alt_min_local = 0, alt_max_local = 0, ascent = 0, ascent2 = 0;
        int way = 1;
        int climb_start = 0, climb_start2 = 0, climb_end = 0;
        for (int i = 0; i < _nbPointsIni; i++) {
            StringBuilder debugPoint = new StringBuilder();
            alt = _pointsIni[i].getAltitude();
            //debugPoint.append(i + ": " + Math.floor(alt) + " - ");
            if (i == 0) {
                alt_min_local = alt_max_local = alt;
                climb_start = climb_start2 = i;
            } else {
                double alt_delta =  alt - alt_temp;
                if (alt_delta > 0) {
                    ascent += alt_delta;
                }
                if ((way < 0) && (alt < alt_min_local)) {
                    debugPoint.append(i + ": " + Math.floor(alt) + " " + way + " - case 1: descent, continuing\n");
                    //Log.d(TAG, i + ": " + Math.floor(alt) + " - case 1: descent, continuing");
                    alt_min_local = alt;
                    climb_start2 = i;
                } else if ((way > 0) && (alt > alt_max_local)) {
                    debugPoint.append(i + ": " + Math.floor(alt) + " " + way + " - case 2: climb, continuing (min:" + Math.floor(alt_min_local) + ")\n");
                    alt_max_local = alt;
                    climb_end = i;
                } else if ((way > 0) && (alt <= alt_max_local - MIN_ASCENT_VARIATION)) {
                    double ascent_climb = alt_max_local - alt_min_local;
                    debugPoint.append(i + ": " + Math.floor(alt) + " " + way + " - case 3: climb, and re-descent too much: end climb (" + Math.floor(ascent_climb) + "m)\n");
                    if (ascent_climb > MIN_ASCENT_CLIMB) {
                        ascent2 += ascent_climb;
                        addClimb(climb_start, climb_end);
                    }
                    alt_min_local = alt_max_local = alt;
                    way = -1;
                } else if ((way < 0) && (alt >= alt_min_local + MIN_ASCENT_VARIATION)) {
                    debugPoint.append(i + ": " + Math.floor(alt) + " " + way + " - case 4: descent, and re-climb too much: begin climb (" + Math.floor(alt - alt_min_local) + "m)\n");
                    //Log.d(TAG, i + ": " + Math.floor(alt) + " - case 4: descent, and re-climb too much: begin climb (" + Math.floor(alt - alt_min_local) + "m)");
                    alt_min_local = alt_max_local = alt;
                    way = 1;
                    climb_start = i;
                } else {
                    //debugPoint.append(i + ": " + Math.floor(alt) + " - case 0: flat\n");
                }
            }
            alt_temp = alt;
            //Log.d(TAG, debugPoint.toString());
            debug.append(debugPoint);
        }
        if (way > 0) {
            StringBuilder debugPoint = new StringBuilder();
            double ascent_climb = alt_max_local - alt_min_local;
            if (ascent_climb > MIN_ASCENT_CLIMB) {
                ascent2 += ascent_climb;
                addClimb(climb_start, climb_end);
            }
        }

        //if (debugLevel >= 1) Log.d(TAG, debug.toString());
        Log.d(TAG, "delectClimbs nbPointsIni:" + _nbPointsIni + " ascent:" + Math.floor(ascent) + " ascent2:" + Math.floor(ascent2));
    }
    public void addClimb(int climb_start, int climb_end) {
        int alt_min_local = (int) Math.floor(_pointsIni[climb_start].getAltitude());
        int alt_max_local = (int) Math.floor(_pointsIni[climb_end].getAltitude());
        int ascent_climb = alt_max_local - alt_min_local;
        Log.d(TAG, "New climb (" + climb_start + "-" + climb_end + "): " + alt_min_local + "m -> " + alt_max_local + "m = " + ascent_climb);

        Climb climb = new Climb();
        climb.start = _pointsIni[climb_start];
        climb.end = _pointsIni[climb_end];
        climb.dist = (int) (_pointsIni[climb_end].distance - _pointsIni[climb_start].distance);
        climb.ascent = ascent_climb;
        _climbs.add(climb);
    }
    public String displayClimb(int index) {
        String msg = "";
        if (index < _climbs.size()) {
            Climb climb = _climbs.get(index);
            msg = //"#" + (index+1) + "/" + _climbs.size()
                "Summit " + ((int) climb.end.getAltitude()) + "m"
                + " - climb " + climb.ascent + "m in " + String.format("%.1f", climb.dist/1000) + "km";
            if (climb.dist > 0) {
                msg += " " + String.format("%.1f", 100 * climb.ascent / climb.dist) + "%";
            }
        }
        return msg;
    }

    public void loadRouteToOrux(Activity activity) {

        //Map offline
        Intent i = new Intent("com.oruxmaps.VIEW_MAP_OFFLINE");
        //Map online
        //Intent i = new Intent("com.oruxmaps.VIEW_MAP_ONLINE");
        // Route Waypoints

        List<Double> targetLat = new ArrayList<>();
        List<Double> targetLon = new ArrayList<>();
        List<String> targetNames = new ArrayList<>();
        List<Integer> targetTypes = new ArrayList<>();

        for(int j = 0; j < _nbPointsSimpl; j++) {
            int type = -1; // -1 : do not add
            if (j == _nextIndex) {
                // next point
                type = 1;
            } else if (j == _nbPointsSimpl - 1) {
                // destination
                type = 15;
            } else {
                if (debugLevel > 0) {
                    type = 4;
                }
            }
            if (type >= 0) {
                targetLat.add(_pointsSimpl[j].getLatitude());
                targetLon.add(_pointsSimpl[j].getLongitude());
                targetNames.add("pt" + j);
                targetTypes.add(type);
            }
        }
        int nb = 0;
        for (Climb climb : _climbs) {
            targetLat.add(climb.start.getLatitude());
            targetLon.add(climb.start.getLongitude());
            targetNames.add("Start climb #" + (nb+1) + "/" + _climbs.size() + " " + displayClimb(nb));
            targetTypes.add(29); // 29:Parking Area

            targetLat.add(climb.end.getLatitude());
            targetLon.add(climb.end.getLongitude());
            targetNames.add("End climb #" + (nb+1) + "/" + _climbs.size() + " " + displayClimb(nb));
            targetTypes.add(40); // 40:Summit
            nb++;
        }
        for (Poi wpt : _wpts) {
            targetLat.add(wpt.getLatitude());
            targetLon.add(wpt.getLongitude());
            targetNames.add(wpt.name + " - " + wpt.desc);
            targetTypes.add(11); // 11:crossing
        }
        // doc http://www.oruxmaps.com/foro/viewtopic.php?p=4404#p4404
        double[] targetLat2 = new double[targetLat.size()];
        double[] targetLon2 = new double[targetLon.size()];
        int[] targetTypes2 = new int[targetTypes.size()];

        for (int j = 0; j < targetLat.size(); j++) {
            targetLat2[j] = targetLat.get(j);
            targetLon2[j] = targetLon.get(j);
            targetTypes2[j] = targetTypes.get(j);
        }
        i.putExtra("targetLat", targetLat2);
        i.putExtra("targetLon", targetLon2);
        i.putExtra("targetName", targetNames.toArray(new String[targetNames.size()]));
        i.putExtra("targetType", targetTypes2);
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

    public float getDistanceToDestination(int units) {
        // in m
        float distance = _nbPointsSimpl > 0 && _nextIndex >= 0 && _nextIndex < _nbPointsSimpl ? (_pointsSimpl[_nbPointsSimpl-1].distance - _pointsSimpl[_nextIndex].distance + _nextDistance) : 0;

        if (units == Constants.IMPERIAL || units == Constants.RUNNING_IMPERIAL) {
            distance *= Constants.M_TO_MILES;
        } else if (units == Constants.METRIC || units == Constants.RUNNING_METRIC) {
            distance *= Constants.M_TO_KM;
        } else if (units == Constants.NAUTICAL_IMPERIAL || units == Constants.NAUTICAL_METRIC) {
            distance *= Constants.M_TO_NM;
        }
        return distance; // in km
    }
    public float getNextDistance(int units) {
        // in m
        float distance = _nextDistance;
        if (units == Constants.IMPERIAL || units == Constants.RUNNING_IMPERIAL || units == Constants.NAUTICAL_IMPERIAL) {
            distance *= Constants.M_TO_FEET;
        } else if (units == Constants.METRIC || units == Constants.RUNNING_METRIC || units == Constants.NAUTICAL_METRIC) {
            distance *= Constants.M_TO_M;
        }
        return distance;
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
        if (i >= 0 && i < _nbPointsSimpl) {
            return _pointsSimpl[i];
        }
        return null;
    }
}
