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

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import fr.jayps.android.AdvancedLocation;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class LiveTracking {
	
	private static final String TAG = "PB-LiveTracking";
	
    protected Context _context = null;
    private Location _firstLocation = null;
    private long _prevTime = -1;
    private Location _lastLocation = null;
    private String _activity_id = "";
    private String _bufferPoints = "";
    private String _bufferAccuracies = "";
    private String _login = "";
    private String _password = "";
    private String _url = "";
    private int _versionCode = -1;
    public int numberOfFriends = 0;
    private int _numberOfFriendsSentToPebble = 0;
    
    final int maxNumberOfFriend = 5;
    
    class LiveTrackingFriend {
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
		public boolean updateFromFriend(LiveTrackingFriend friend, Location lastlocation) {
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
    String getLogin() {
        return this._login;
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

    public boolean addPoint(Location firstLocation, Location location) {
        _firstLocation = firstLocation;
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
		new SendLiveTask().execute(new SendLiveTaskParams(_bufferPoints, _bufferAccuracies));
		return true;
    }
    class SendLiveTaskParams {
        String points;
        String accuracies;
        public SendLiveTaskParams(String points, String accuracies) {
            this.points = points;
            this.accuracies = accuracies;
        }
    }
    private class SendLiveTask extends AsyncTask<SendLiveTaskParams, Void, Boolean> {
        protected Boolean doInBackground(SendLiveTaskParams... params) {
            int count = params.length;
            boolean result = false;
            for (int i = 0; i < count; i++) {
                result = result || _send(params[i].points, params[i].accuracies);
            }
            return result;
        }

        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "onPostExecute(" + result + ")");
        }
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
                	nbReceivedFriends++;
                    
                	LiveTrackingFriend f2;
                	if (_friends.containsKey(friend.id)) {
                    	//Log.d(TAG, "update friend "+friend.id);
                    	f2 = _friends.get(friend.id);
                    } else {
                    	//Log.d(TAG, "new friend "+friend.id);
                        friend.number = numberOfFriends;
                        numberOfFriends++;
                        f2 = friend;
                    }
                    f2.updateFromFriend(friend, _lastLocation);
                    _friends.put(friend.id, f2);
                }
            }
            //Iterator<Entry<String, LiveTrackingFriend>> iter = _friends.entrySet().iterator();
			//while (iter.hasNext()) {
				//LiveTrackingFriend f = iter.next().getValue();
				//Log.d(TAG, "+++"+f.toString());
			//}            

            byte[] msgLiveShort = getMsgLiveShort(_firstLocation);
            String[] names = getNames();
            if (msgLiveShort.length > 1) {
                String sending = "";
                
                PebbleDictionary dic = new PebbleDictionary();
                
                if (_numberOfFriendsSentToPebble != msgLiveShort[0] || (5 * Math.random() <= 1)) {
                    _numberOfFriendsSentToPebble = msgLiveShort[0];
                    
                    if (names[0] != null) {
                        dic.addString(Constants.MSG_LIVE_NAME0, names[0]);
                    }
                    if (names[1] != null) {
                        dic.addString(Constants.MSG_LIVE_NAME1, names[1]);
                    }
                    if (names[2] != null) {
                        dic.addString(Constants.MSG_LIVE_NAME2, names[2]);
                    }
                    if (names[3] != null) {
                        dic.addString(Constants.MSG_LIVE_NAME3, names[3]);
                    }
                    if (names[4] != null) {
                        dic.addString(Constants.MSG_LIVE_NAME4, names[4]);
                    }
                    sending += " MSG_LIVE_NAMEx"+msgLiveShort[0];
                }
                dic.addBytes(Constants.MSG_LIVE_SHORT, msgLiveShort);
                for( int i = 0; i < msgLiveShort.length; i++ ) {
                    sending += " msgLiveShort["+i+"]: "   + ((256+msgLiveShort[i])%256);
                }
                Log.d(TAG, sending);

                PebbleKit.sendDataToPebble(_context, Constants.WATCH_UUID, dic);
            }
            
            
            _bufferPoints = _bufferAccuracies = "";
            
            return nbReceivedFriends > 0;
            
        } catch (Exception e) {
        	Log.e(TAG, "Exception:" + e);
        }
        return false;
    }

    public byte[] getMsgLiveShort(Location firstLocation) {
       final int sizeOfAFriend = 9;
       float _distanceConversion = (float) Constants.M_TO_KM; //TODO: miles
       
       byte[] data = new byte[1 + maxNumberOfFriend * sizeOfAFriend];
       
       data[0] = (byte) _friends.size();

       //Log.d(TAG,  "firstLocation: lat="+firstLocation.getLatitude()+"-lon="+firstLocation.getLongitude());
       
       Iterator<Entry<String, LiveTrackingFriend>> iter = _friends.entrySet().iterator();
       while (iter.hasNext()) {
           LiveTrackingFriend f = iter.next().getValue();
           if (f.number >= maxNumberOfFriend) {
               // too many friends, skip this one
               continue;
           }
           
           //Log.d(TAG, firstLocation.toString());
           //Log.d(TAG, f.getLocation().toString());

           Location tmploc = f.getLocation();
           //Log.d(TAG,  f.number + "|lat="+firstLocation.getLatitude()+"-lon="+firstLocation.getLongitude());
           double xpos = firstLocation.distanceTo(tmploc) * Math.sin(firstLocation.bearingTo(tmploc)/180*3.1415);
           double ypos = firstLocation.distanceTo(tmploc) * Math.cos(firstLocation.bearingTo(tmploc)/180*3.1415);
           xpos = Math.floor(xpos/10);
           ypos = Math.floor(ypos/10);
           
           long lastViewed = System.currentTimeMillis() / 1000 - f.ts;
           //Log.d(TAG, "lastViewed="+lastViewed);
           
           data[1 + f.number * sizeOfAFriend + 0] = (byte) (((int) Math.abs(xpos)) % 256);
           data[1 + f.number * sizeOfAFriend + 1] = (byte) ((((int) Math.abs(xpos)) / 256) % 128);
           if (xpos < 0) {
               data[1 + f.number * sizeOfAFriend + 1] += 128;
           }
           data[1 + f.number * sizeOfAFriend + 2] = (byte) (((int) Math.abs(ypos)) % 256);
           data[1 + f.number * sizeOfAFriend + 3] = (byte) ((((int) Math.abs(ypos)) / 256) % 128);
           if (ypos < 0) {
               data[1 + f.number * sizeOfAFriend + 3] += 128;
           }
           data[1 + f.number * sizeOfAFriend + 4] = (byte) (((int) (Math.floor(100 * f.deltaDistance * _distanceConversion) / 1)) % 256);
           data[1 + f.number * sizeOfAFriend + 5] = (byte) (((int) (Math.floor(100 * f.deltaDistance * _distanceConversion) / 1)) / 256);
           data[1 + f.number * sizeOfAFriend + 6] = (byte) (((int) (f.bearing / 360 * 256)) % 256);
           data[1 + f.number * sizeOfAFriend + 7] = (byte) (((int) lastViewed) % 256);
           data[1 + f.number * sizeOfAFriend + 8] = (byte) (((int) lastViewed) / 256);

           
           String strFriend = f.number + "|" + f.nickname + " ";
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
           strFriend += " - xpos="+xpos+"-ypos="+ypos;
           //strFriend += " - lat="+f.lat+"-lon="+f.lon;
           Log.d(TAG, strFriend);
       }
                  
       return data; 
   }
   public String[] getNames() {
     
       String[] names = new String[maxNumberOfFriend];

       Iterator<Entry<String, LiveTrackingFriend>> iter = _friends.entrySet().iterator();
       while (iter.hasNext()) {
           LiveTrackingFriend f = iter.next().getValue();
           if (f.number >= maxNumberOfFriend) {
               // too many friends, skip this one
               continue;
           }
           names[f.number] = f.nickname;
       }

       return names;
   }
}
