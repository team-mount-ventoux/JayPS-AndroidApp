package com.njackson;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
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

import fr.jayps.android.AdvancedLocation;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.util.Base64;
import android.util.Log;

public class LiveTracking {
	
	private static final String TAG = "PB-LiveTracking";
	
    protected Context _context = null;
    private long _prevTime = -1;
    private Location _lastLocation = null;
    private String _activity_id = "";
    private String _bufferPoints = "";
    private String _bufferAccuracies = "";
    private String _login = "";
    private String _password = "";
    private String _url = "";
    private int _versionCode = -1;
    
    class LiveTrackingFriend {
    	public String id = "";
    	public String nickname = "";
    	public Double lat = null, lon = null;
    	long ts = 0;
    	long dt = -1;
    	private long _receivedTimestamp = 0;
    	float deltaDistance = 0.0f, bearing = 0.0f;
    	private Location _location;
    	
    	public LiveTrackingFriend() {
    		_location = new Location("PebbleBike");
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
    	public void compareToLocation(Location location) {
	    	_location.setLatitude(lat);
	    	_location.setLongitude(lon);
	
	    	deltaDistance = location.distanceTo(_location);
	    	bearing = location.bearingTo(_location);
    	}
		public boolean updateFromFriend(LiveTrackingFriend friend) {
			if ((id == "") || !friend.id.equals(this.id)) {
				Log.e(TAG, "updateFromFriend this "+this.toString());
				Log.e(TAG, "updateFromFriend friend "+friend.toString());
				return false;
			}
			dt = friend.ts-ts;
			//Log.d(TAG, "dt:"+ts+"->"+friend.ts+" "+dt+"s");
			ts = friend.ts;
			lat = friend.lat;
			lon = friend.lon;
			return true;
		}
    }
    
    private HashMap<String, LiveTrackingFriend> _friends = new HashMap<String, LiveTrackingFriend>();
    
    public LiveTracking() {
        this._context = null;
    }    
    public LiveTracking(Context context) {
        this._context = context;
        this._lastLocation = new Location("PebbleBike");
        
        // Get current version code
        try {
            PackageInfo packageInfo = this._context.getPackageManager().getPackageInfo(
            		this._context.getPackageName(), 0);

            _versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
        	_versionCode = -1;
        }        
    }
    void setLogin(String login) {
    	this._login = login;
    }
    void setPassword(String password) {
    	this._password = password;
    }
    void setUrl(String url) {
    	this._url = url;
    }

    public boolean addPoint(Location location) {
    	//Log.d(TAG, "addPoint(" + location.getLatitude() + "," + location.getLongitude() + "," + location.getAltitude() + "," + location.getTime() + "," + location.getAccuracy()+ ")");
    	if (location.getTime() - _prevTime < 5000) {
    		// too early (dt<5s), do nothing
    		return false;
    	} 
    	_bufferPoints += (_bufferPoints != "" ? " " : "") + location.getLatitude() + " " + location.getLongitude() + " " + String.format(Locale.US, "%.1f", location.getAltitude()) + " " + String.format("%d", (int) (location.getTime()/1000));
    	_bufferAccuracies += (_bufferAccuracies != "" ? " " : "") + String.format(Locale.US, "%.1f", location.getAccuracy());
    	if (location.getTime() - _prevTime < 30000) {
    		// too early (5s<dt<30s), save point to send it later
    		Log.d(TAG, "too early: skip addPoint(" + location.getLatitude() + "," + location.getLongitude() + "," + location.getAltitude() + "," + location.getTime() + ")");
    		return false;
    	}
		// ok
		_prevTime = location.getTime();
		this._lastLocation = location;
		boolean result = this._send(_bufferPoints, _bufferAccuracies);
		_bufferPoints = _bufferAccuracies = "";
		return result;
    }
    private boolean _send(String points, String accuracies) {
    	Log.d(TAG, "send(" + points + ", " + accuracies + ")");
        try {
        	String request = _activity_id == "" ? "start_activity" : "update_activity";
        	String postParameters = "";
        	String authString = ""; //"login:pass"
        	
        	if (_login != "" && _password != "") { 
            	authString = _login + ":" + _password;
            } else {
            	Log.d(TAG, "Missing login or password");
        		return false;
        	}        	

        	
        	postParameters = "request=" + request;
        	if (_activity_id == "") {
        		postParameters += "&title=Test&source=PebbleBike&version="+_versionCode;
    		} else {
    			postParameters += "&activity_id="+_activity_id;
    		}
    	
        	if (points != "") {
        		postParameters += "&points="+points;
    		}
        	if (accuracies != "") {
        		postParameters += "&jayps_accuracies="+accuracies;
    		}
        	
            URL url = new URL(_url != "" ? _url : "http://live.jayps.fr/api/mmt.php");
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
			//Log.d(TAG, "response:" + response);
        			
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
                Log.d(TAG, "activity_id:"+activity_id);
                if (activity_id != "") {
                	_activity_id = activity_id;
                }
            }

            expression = "//friend";
            nodes = (NodeList)xpath.evaluate(expression, doc, XPathConstants.NODESET);
            
            int nbReceivedFriends = 0;
            
            for( int i = 0; i < nodes.getLength(); i++ ) {
            	// for each friends
                node = nodes.item(i);

            	LiveTrackingFriend friend = new LiveTrackingFriend();
            	friend.setFromNodeList(node.getChildNodes());

                if (friend.id != "" && friend.lat != null && friend.lon != null) {
                	friend.compareToLocation(_lastLocation);
                	nbReceivedFriends++;
                    
                    if (_friends.containsKey(friend.id)) {
                    	// update friend
                    	//Log.d(TAG, "update friend "+friend.id);
                    	LiveTrackingFriend f2 = _friends.get(friend.id);
                    	f2.updateFromFriend(friend);
                    	_friends.put(friend.id, f2);
                    } else {
                    	// new friend
                    	//Log.d(TAG, "new friend "+friend.id);
                    	_friends.put(friend.id, friend);
                    }
                 }
                
            }
            //Iterator<Entry<String, LiveTrackingFriend>> iter = _friends.entrySet().iterator();
			//while (iter.hasNext()) {
				//LiveTrackingFriend f = iter.next().getValue();
				//Log.d(TAG, "+++"+f.toString());
			//}            

            return nbReceivedFriends > 0;
            
        } catch (Exception e) {
        	Log.e(TAG, "Exception:" + e);
        }
        return false;
    }
    public String getFriends() {
    	String result = "";
        
    	Iterator<Entry<String, LiveTrackingFriend>> iter = _friends.entrySet().iterator();
		while (iter.hasNext()) {
			LiveTrackingFriend f = iter.next().getValue();
			
			long lastViewed = System.currentTimeMillis() / 1000 - f.ts;
			
			//Log.i(TAG, "--" + f.toString() + "|" + lastViewed);
			
			String strFriend = f.nickname + " ";
            if (f.deltaDistance > 1000) {
            	strFriend += String.format(Locale.US, "%.1f", f.deltaDistance/1000) + "km";
            } else {
            	strFriend += String.format(Locale.US, "%.0f", f.deltaDistance) + "m";
            }
            strFriend += " " + String.format(Locale.US, "%.0f", f.bearing) + "°";
            strFriend += " (" + AdvancedLocation.bearingText(f.bearing) + ")";
            if (lastViewed >= 0) {
            	if (lastViewed < 60) {
	            	strFriend += " (" + lastViewed + "\")";
	            } else if (lastViewed < 60 * 60) {
	            	strFriend += " (" + (lastViewed / 60) + "')";
	            }
            }
            result += (result != "" ? "\n" : "") + strFriend;
		}
		return result;
    }
}
