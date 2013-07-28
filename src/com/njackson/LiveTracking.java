package com.njackson;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.location.Location;
import android.util.Base64;
import android.util.Log;

public class LiveTracking {
    protected Context _context = null;
    private long _prevTime = -1;
    private Location _lastLocation = null;
    private String _activity_id = "";
    private String _friends = "";
    private String _bufferPoints = "";
    private String _bufferAccuracies = "";
    public LiveTracking() {
        this._context = null;
    }    
    public LiveTracking(Context context) {
        this._context = context;
        this._lastLocation = new Location("PebbleBike");
    }
    public void addPoint(double lat, double lon, double altitude, long time, float accuracy) {
    	Log.d("JayPS-LiveTracking", "addPoint(" + lat + "," + lon + "," + altitude + "," + time + "," + accuracy+ ")");
    	_bufferPoints += (_bufferPoints != "" ? " " : "") + lat + " " + lon + " " + String.format(Locale.US, "%.1f", altitude) + " " + String.format("%d", (int) (time/1000));
    	_bufferAccuracies += (_bufferAccuracies != "" ? " " : "") + String.format(Locale.US, "%.1f", accuracy);
    	this._friends = "";
    	if (time - _prevTime < 5000) {
    		// too early (dt<5s), do nothing
    		return;
    	} else if (time - _prevTime < 30000) {
    		// too early (5s<dt<30s), save point to send it later
    		Log.d("JayPS-LiveTracking", "too early: skip addPoint(" + lat + "," + lon + "," + altitude + "," + time + ")");
    	} else {
    		// ok
    		_prevTime = time;
    		this._lastLocation.setLatitude(lat);
    		this._lastLocation.setLongitude(lon);
    		this._lastLocation.setAltitude(altitude);
    		this._lastLocation.setTime(time);
    		this._lastLocation.setAccuracy(accuracy);
    		this._send(_bufferPoints, _bufferAccuracies);
    		_bufferPoints = _bufferAccuracies = "";
    	}
    }
    private void _send(String points, String accuracies) {
    	Log.d("JayPS-LiveTracking", "send(" + points + ", " + accuracies + ")");
        try {
        	String request = _activity_id == "" ? "start_activity" : "update_activity";
        	String postParameters = "";
        	String authString = ""; //"login:pass"
        	
        	postParameters = "request=" + request;
        	if (_activity_id == "") {
        		postParameters += "&title=Test&source=PebbleBike&version=1.3";
    		} else {
    			postParameters += "&activity_id="+_activity_id;
    		}
        	if (authString == "") {
        		postParameters += "&autologin=demolive";
        	}        	
        	if (points != "") {
        		postParameters += "&points="+points;
    		}
        	if (accuracies != "") {
        		postParameters += "&jayps_accuracies="+accuracies;
    		}
        	
            URL url = new URL("http://www.jayps.fr/api/mmt.php");
        	HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        	
        	if (authString != "") {
        		String basicAuth = "Basic " + new String(Base64.encode(authString.getBytes(), Base64.NO_WRAP));
        		urlConnection.setRequestProperty ("Authorization", basicAuth);
        	}
        	
    		urlConnection.setDoOutput(true);
    		urlConnection.setRequestMethod("POST");
    		urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");            
			urlConnection.setFixedLengthStreamingMode(postParameters.getBytes().length);
			PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
			out.print(postParameters);
			out.close();
			
			//start listening to the stream
			String response= "";
			Scanner inStream = new Scanner(urlConnection.getInputStream());

			//process the stream and store it in StringBuilder
			while(inStream.hasNextLine()) {
				response+=(inStream.nextLine()) + "\n";
			}
			//Log.d("JayPS-LiveTracking", "response:" + response);
        			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new ByteArrayInputStream(response.getBytes("utf-8"))));
            
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "";
            NodeList nodes;
            Node node;
            
            if (request == "start_activity") {
                expression = "/message/activity_id";
           
                String activity_id = xpath.evaluate(expression, doc);
                Log.d("JayPS-LiveTracking", "activity_id:"+activity_id);
                if (activity_id != "") {
                	_activity_id = activity_id;
                }
            }

            expression = "//friend";
            nodes = (NodeList)xpath.evaluate(expression, doc, XPathConstants.NODESET);
            String friends = "";
            Location location = new Location("PebbleBike");
            float deltaDistance, bearing;

            for( int i = 0; i < nodes.getLength(); i++ ) {
            	// for each friends
                node = nodes.item( i );
                NodeList friendChildNodes = node.getChildNodes();

                String nickname = "";
                Double lat = null, lon = null;
                deltaDistance = 0.0f; 
            	bearing = 0.0f;

                for( int j = 0; j < friendChildNodes.getLength(); j++ ) {
                	// for each fields
                	
                    Node item = friendChildNodes.item( j );
                    String nodeName = item.getNodeName();

                    if (nodeName.equals("nickname")) {
                    	nickname = item.getChildNodes().item(0).getNodeValue();
                    }
                    if (nodeName.equals("lat")) {
                        lat = Double.valueOf(item.getChildNodes().item(0).getNodeValue());
                    }
                    if (nodeName.equals("lon")) {
                        lon = Double.valueOf(item.getChildNodes().item(0).getNodeValue());
                    }
                }
                if (lat != null && lon != null) {
                	Log.d("JayPS-LiveTracking", "lat:"+lat + " - lon:"+lon);
                	location.setLatitude(lat);
                	location.setLongitude(lon);

                    deltaDistance = _lastLocation.distanceTo(location);
                    bearing = _lastLocation.bearingTo(location);
                 }
                
                if (friends != "") {
            		friends += "\n";
                }
                String friend = nickname + " ";
                if (deltaDistance > 1000) {
                	friend += String.format(Locale.US, "%.1f",deltaDistance/1000) + "km";
                } else {
                	friend += String.format(Locale.US, "%.0f",deltaDistance) + "m";
                }
                friend += " " + String.format(Locale.US, "%.0f",bearing) + "°";
                friends += friend;
            }            
            Log.d("JayPS-LiveTracking", "friends:"+friends);
            this._friends = friends;
            
        } catch (Exception e) {
        	Log.d("JayPS-LiveTracking", "Exception:" + e);
        }
    }
    public String getFriends() {
    	return this._friends;
    }
}
