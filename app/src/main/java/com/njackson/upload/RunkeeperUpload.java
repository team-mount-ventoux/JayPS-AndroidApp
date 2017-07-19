package com.njackson.upload;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.njackson.R;
import com.njackson.application.PebbleBikeApplication;
import com.njackson.pebble.IMessageManager;
import com.njackson.state.IGPSDataStore;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import javax.inject.Inject;

import fr.jayps.android.AdvancedLocation;

public class RunkeeperUpload {

    private static final String TAG = "PB-RunkeeperUpload";

    @Inject IMessageManager _messageManager;
    @Inject SharedPreferences _sharedPreferences;
    @Inject IGPSDataStore _dataStore;
    Activity _activity = null;
    Context _context;

    private class ApiResult {
        String message;
        int serverResponseCode;
        String serverResponseMessage;
    }
    public RunkeeperUpload(Activity activity) {
        ((PebbleBikeApplication) activity.getApplicationContext()).inject(this);
        _activity = activity;
        _context = activity.getApplicationContext();
    }

    public RunkeeperUpload(Context context) {
        ((PebbleBikeApplication) context).inject(this);
        _context = context;
    }

    public void upload(String token) {
        Toast.makeText(_context, "Runkeeper: uploading... Please wait", Toast.LENGTH_LONG).show();
        final String runkeeper_token = token;

        new Thread(new Runnable() {
            public void run() {
               Log.i(TAG, "token: " + runkeeper_token);

                Looper.prepare();
                AdvancedLocation advancedLocation = new AdvancedLocation(_context);
                advancedLocation.debugLevel = _sharedPreferences.getBoolean("PREF_DEBUG", false) ? 1 : 0;
                advancedLocation.debugTagPrefix = "PB-";
                advancedLocation.setElapsedTime(_dataStore.getElapsedTime());
                String json = advancedLocation.getRunkeeperJson(_sharedPreferences.getString("RUNKEEPER_ACTIVITY_TYPE", _context.getString(R.string.RUNKEEPER_ACTIVITY_TYPE_DEFAULT)));

                String message;

                ApiResult res = _upload(runkeeper_token, json);
                message = res.message;
                Log.d(TAG, "_upload: " + res.serverResponseCode + "[" + res.serverResponseMessage + "] - " + res.message);

                if (_activity != null) {
                    final String _message = message;
                    _activity.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(_activity.getApplicationContext(), "Runkeeper: " + _message, Toast.LENGTH_LONG).show();
                            Log.d(TAG, "_message:" + _message);
                        }
                    });
                }
                if (_sharedPreferences.getBoolean("RUNKEEPER_NOTIFICATION", false)) {
                    // use _messageManager and not _bus to be able to send data even if GPS is not started
                    _messageManager.sendMessageToPebble("JayPS - Runkeeper", message);
                }
            }
        }).start();
    }

    private ApiResult _upload(String runkeeper_token, String json) {
        ApiResult result = new ApiResult();

        //String tmp_url = "http://labs.jayps.fr/pebble/strava.php";
        String tmp_url = "https://api.runkeeper.com/fitnessActivities";
        //Log.d(TAG, "UPLOAD url="+tmp_url);

        try {
            URL url = new URL(tmp_url);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestProperty("Content-Type", "application/vnd.com.runkeeper.NewFitnessActivity+json");
            String auth = "Bearer " + runkeeper_token;
            //Log.d(TAG, "auth="+auth);
            urlConnection.setRequestProperty("Authorization", auth);
            urlConnection.setDoOutput(true);

            String data = json;

            //Log.d(TAG, "data="+data);
            //Log.d(TAG, "data[" + data.length() + "]:" + data);

            urlConnection.setFixedLengthStreamingMode(data.length());

            DataOutputStream outputStream = new DataOutputStream( urlConnection.getOutputStream() );
            outputStream.writeBytes(data);

            Log.d(TAG, "outputStream.size():" + outputStream.size());
            // finished with POST request body

            outputStream.flush();
            outputStream.close();

            // checks server's status code first
            // Responses from the server (code and message)
            result.serverResponseCode = urlConnection.getResponseCode();
            result.serverResponseMessage = urlConnection.getResponseMessage();

            //Log.d(TAG, "_upload: " + result.serverResponseCode + "[" + result.serverResponseMessage + "]");

            result.message = result.serverResponseMessage + " (" + result.serverResponseCode + ")";

            //start listening to the stream
            String response = "";
            // Runkeeper doc: Upon a successful submission the request will return 201 Created. If there was an error the request will return 400 Bad Request.
            InputStream is = null;
            if (result.serverResponseCode == 201) {
                is = urlConnection.getInputStream();
                result.message = "Your activity has been created";
            } else if (result.serverResponseCode == 400) {
                is = urlConnection.getErrorStream();
                result.message = "An error has occurred.";
            } else {
                is = urlConnection.getInputStream();
            }
            if (is != null) {
                Scanner inStream = new Scanner(is);
                //process the stream and store it in StringBuilder
                while (inStream.hasNextLine()) {
                    response += (inStream.nextLine()) + "\n";
                }
            }
            Log.d(TAG, "response:" + response);
            if (response != "") {
                /*try {
                    JSONObject jObject = new JSONObject(response);
                    result.status = jObject.getString("status");
                    Log.d(TAG, "result.status:" + result.status);
                } catch (JSONException e) {
                    Log.e(TAG, "Exception:" + e);
                }*/
            }
            urlConnection.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "Exception:" + e);
            //Toast.makeText(_context, "Exception:" + e, Toast.LENGTH_LONG).show();
            //result.message = "" + e;
        }
        return result;
    }
}
