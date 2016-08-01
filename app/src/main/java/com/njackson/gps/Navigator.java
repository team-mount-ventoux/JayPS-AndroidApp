package com.njackson.gps;


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

    protected class Poi extends Location {
        public boolean seen = false;

        public Poi(Location l) {
            super(l);
            this.seen = false;
        }
    }

    Poi[] points;
    private int _nbPoints = 0;
    private float _nextDistance = 0;
    private float _nextBearing = 0;
    private int _nextIndex = 0;
    private int _maxSeenIndex = -1;
    private float _error = 0;

    public Navigator() {
        Log.d(TAG, "Navigator() nbPoints:" + _nbPoints);
    }

    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged: lat:"+location.getLatitude()+",lon:"+location.getLongitude() + " nbPoints:" + _nbPoints);
        float minDist = 1000000;
        int minPoint = -1;
        float minBearing = 0;
        float minError = 0;
        for(int i = 0; i < _nbPoints; i++) {
            float dist = location.distanceTo(points[i]);
            float bearing = (location.bearingTo(points[i]) + 360) % 360;
            float error = i > 0 ? crossTrackError(points[i-1], points[i], location) : 0;
            Log.d(TAG, i + " dist:" + dist + " bearing:" + bearing + " error:" + error + " seen:" + (points[i].seen ? "y" : "n"));
            if (dist < minDist && _maxSeenIndex < i) {
                minDist = dist;
                minPoint = i;
                minBearing = bearing;
                minError = error;
            }
            if (dist < 50) {
                points[i].seen = true;
                _maxSeenIndex = i > _maxSeenIndex ? i : _maxSeenIndex;
            }
        }
        Log.d(TAG, "min:"  + minDist + " bearing: " + minBearing + " error: " + minError + " point #" + minPoint);

        _nextDistance = minDist;
        _nextBearing = minBearing;
        _nextIndex = minPoint;
        _error = minError;
    }
    public void loadGpx(String gpx) {
        _nbPoints = 0;
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
            points = new Poi[nodes.getLength()];
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
                        points[_nbPoints++] = new Poi(loc);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Exception:" + e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
        Log.d(TAG, "nbPoints:" + _nbPoints);
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
        return _nbPoints;
    }
}
