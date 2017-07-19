package com.njackson.live;

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

import com.njackson.Constants;
import com.njackson.events.LiveServiceCommand.LiveMessage;
import com.squareup.otto.Bus;

import fr.jayps.android.AdvancedLocation;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class LiveTracking implements ILiveTracking {

    private static final String TAG = "PB-LiveTracking";

    private Bus _bus;

    protected Context _context = null;
    private Location _firstLocation = null;
    private long _prevTime = -1;
    private Location _lastLocation = null;
    private String _activity_id = "";
    private String _bufferPoints = "";
    private String _bufferAccuracies = "";
    private String _bufferHeartRates = "";
    private String _bufferCadences = "";
    private String _login = "";
    private String _password = "";
    private String _url = "";
    private int _versionCode = -1;
    public int numberOfFriends = 0;
    private int _numberOfFriendsSentToPebble = 0;

    public final static int maxNumberOfFriend = 5;
    public final static int sizeOfAFriend = 9;

    public static final int TYPE_JAYPS = 1;
    public static final int TYPE_MMT = 2;
    private int _type = TYPE_JAYPS;

    boolean debug = true;


    private HashMap<String, LiveTrackingFriend> _friends = new HashMap<String, LiveTrackingFriend>();

    public LiveTracking(int type, Bus bus) {
        this._context = null;
        this._type = type;
        _bus = bus;
        this._lastLocation = new Location("JayPS");
    }

    public LiveTracking(Context context, int type, Bus bus) {
        this(type, bus);
        this._context = context;
        // Get current version code
        try {
            PackageInfo packageInfo = this._context.getPackageManager().getPackageInfo(
                    this._context.getPackageName(), 0);

            _versionCode = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            _versionCode = -1;
        }
    }

    public String getLogin() {
        return this._login;
    }

    public void setLogin(String login) {
        this._login = login;
    }

    public void setPassword(String password) {
        this._password = password;
    }

    public void setUrl(String url) {
        this._url = url;
    }
    // TODO(nic) : remove me when bus provided by constructor works!
    public void setBus(Bus bus) {
        _bus = bus;
    }
    public boolean addPoint(Location firstLocation, Location location, int heart_rate, int cadence) {
        _firstLocation = firstLocation;
        //Log.d(TAG, "addPoint(" + location.getLatitude() + "," + location.getLongitude() + "," + location.getTime() + "," + location.getAccuracy() + "," + heart_rate + "," + cadence + ")");
        if (location.getTime() - _prevTime < 5000) {
            // too early (dt<5s), do nothing
            return false;
        }
        _bufferPoints += (_bufferPoints != "" ? " " : "") + location.getLatitude() + " " + location.getLongitude() + " " + String.format(Locale.US, "%.1f", location.getAltitude()) + " " + String.format("%d", (int) (location.getTime() / 1000));
        _bufferAccuracies += (_bufferAccuracies != "" ? " " : "") + String.format(Locale.US, "%.1f", location.getAccuracy());
        if (heart_rate > 0 && heart_rate < 255) {
            _bufferHeartRates += (_bufferHeartRates != "" ? " " : "") + heart_rate + " " + String.format("%d", (int) (location.getTime() / 1000));
        }
        if (cadence > 0 && cadence < 255) {
            _bufferCadences += (_bufferCadences != "" ? " " : "") + cadence + " " + String.format("%d", (int) (location.getTime() / 1000));
        }
        if (location.getTime() - _prevTime < 30000) {
            // too early (5s<dt<30s), save point to send it later
            if (debug) {
                Log.d(TAG, "too early: skip addPoint(" + location.getLatitude() + "," + location.getLongitude() + "," + location.getTime() + ")");
			}
            return false;
        }
        // ok
        _prevTime = location.getTime();
        this._lastLocation = location;
        new SendLiveTask().execute(new SendLiveTaskParams(_bufferPoints, _bufferAccuracies, _bufferHeartRates, _bufferCadences));
        return true;
    }

    class SendLiveTaskParams {
        String points;
        String accuracies;
        String heartrates;
        String cadences;

        public SendLiveTaskParams(String points, String accuracies, String heartrates, String cadences) {
            this.points = points;
            this.accuracies = accuracies;
            this.heartrates = heartrates;
            this.cadences = cadences;
        }
    }


    private class SendLiveTask extends AsyncTask<SendLiveTaskParams, Void, Boolean> {
        protected Boolean doInBackground(SendLiveTaskParams... params) {
            int count = params.length;
            boolean result = false;
            for (int i = 0; i < count; i++) {
                result = result || _send(params[i].points, params[i].accuracies, params[i].heartrates, params[i].cadences);
            }
            return result;
        }

        protected void onPostExecute(Boolean result) {
            //if (debug) Log.d(TAG, "onPostExecute(" + result + ")");
        }
    }

    private boolean _send(String points, String accuracies, String heartrates, String cadences) {
        if (debug) Log.d(TAG, "send(" + points + ", " + accuracies + ", " + heartrates + ", " + cadences + ")");
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
                postParameters += "&title=Test&source=JayPS&version=" + _versionCode;
            } else {
                postParameters += "&activity_id=" + _activity_id;
            }

            if (points != "") {
                postParameters += "&points=" + points;
            }
            if (accuracies != "" && _type == TYPE_JAYPS) {
                postParameters += "&jayps_accuracies=" + accuracies;
            }
            if (heartrates != "") {
                postParameters += "&hr=" + heartrates;
            }
            if (cadences != "") {
                postParameters += "&cad=" + cadences;
            }
            String tmp_url = "";
            if (_type == TYPE_JAYPS) {
                tmp_url = _url != "" ? _url : "http://live.jayps.fr/api/mmt.php";
            } else if (_type == TYPE_MMT) {
                tmp_url = _url != "" ? _url : "http://www.mapmytracks.com/api/";
            } else {
                Log.d(TAG, "Missing type");
                return false;
            }
            //if (debug) Log.d(TAG, "url="+tmp_url);
            URL url = new URL(tmp_url);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            if (authString != "") {
                String basicAuth = "Basic " + new String(Base64.encode(authString.getBytes(), Base64.NO_WRAP));
                urlConnection.setRequestProperty("Authorization", basicAuth);
            }

            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setFixedLengthStreamingMode(postParameters.getBytes().length);
            PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
            out.print(postParameters);
            out.close();

            //start listening to the stream
            String response = "";
            Scanner inStream = new Scanner(urlConnection.getInputStream());

            //process the stream and store it in StringBuilder
            while (inStream.hasNextLine()) {
                response += (inStream.nextLine()) + "\n";
            }

            return parseResponse(request, response);

        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
        return false;
    }

    public boolean parseResponse(String request, String response) {
        //Log.d(TAG, "request: "+ request + " response:" + response);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new ByteArrayInputStream(response.getBytes("utf-8"))));

            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "";
            NodeList nodes;
            Node node;

            if(request=="start_activity")

            {
                expression = "/message/activity_id";

                String activity_id = xpath.evaluate(expression, doc);
                if (debug) Log.d(TAG, "activity_id:" + activity_id);
                if (activity_id != "") {
                    _activity_id = activity_id;
                }
            }

            int nbReceivedFriends = 0;

            if(_type==TYPE_JAYPS)

            {
                expression = "//friend";
                nodes = (NodeList) xpath.evaluate(expression, doc, XPathConstants.NODESET);


                for (int i = 0; i < nodes.getLength(); i++) {
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

                    LiveMessage msg = new LiveMessage();
                    if (_numberOfFriendsSentToPebble != msgLiveShort[0] || (5 * Math.random() <= 1)) {
                        _numberOfFriendsSentToPebble = msgLiveShort[0];

                        msg.setName0(names[0]);
                        msg.setName1(names[1]);
                        msg.setName2(names[2]);
                        msg.setName3(names[3]);
                        msg.setName4(names[4]);
                        sending += " MSG_LIVE_NAMEx" + msgLiveShort[0];
                    }
                    msg.setLive(msgLiveShort);
                    for (int i = 0; i < msgLiveShort.length; i++) {
                        sending += " msgLiveShort[" + i + "]: " + ((256 + msgLiveShort[i]) % 256);
                    }
                    if (debug) Log.d(TAG, sending);

                    _bus.post(msg);
                }
            }

            _bufferPoints=_bufferAccuracies=_bufferHeartRates=_bufferCadences="";

            return nbReceivedFriends>0;
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
        }
        return false;
    }

    public byte[] getMsgLiveShort(Location firstLocation) {
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
            if (debug) Log.d(TAG, strFriend);
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
