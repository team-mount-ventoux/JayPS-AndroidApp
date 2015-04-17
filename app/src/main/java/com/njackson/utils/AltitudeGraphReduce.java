package com.njackson.utils;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 24/08/2013
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */
public class AltitudeGraphReduce {
    
    private static final String TAG = "PB-AltitudeGraphReduce";

    private ArrayList<Integer> _altitudeBins = new ArrayList<Integer>();
    private long _lastAltitudeBinChange = -1;
    private int _altitudeBinSizeMs = 120000;
    private int _numberAltitudesInBin = 0;

    private int _altitudeMax = 0;
    private int _altitudeMin = 99999;

    public void setBinInterval(int value) {
        _altitudeBinSizeMs = value;
    }

    public int getMax() {
        return _altitudeMax;
    }
    public void setMax(int value) {
         _altitudeMax = value;
    }

    public int getMin() {
        return _altitudeMin;
    }
    public void setMin(int value) {
        _altitudeMin = value;
    }

    public ArrayList<Integer> getCache() {
        return _altitudeBins;
    }

    public void setCache(ArrayList<Integer> value) {
         _altitudeBins = value;
    }

    public AltitudeGraphReduce() {

    }

    public void addAltitude(int altitude, long time, float distance) {
        //Log.d(TAG, "addAltitude(" + altitude + "," +  time +  "," + distance + ")");
        // time: elapsed time, in millisecond, and not current time
        if(_lastAltitudeBinChange == -1) {
            _altitudeBins.add(altitude); // initialise the first bin
            _lastAltitudeBinChange = time;
        }

        if (altitude > _altitudeMax) {
            _altitudeMax = altitude;
        }
        if (altitude < _altitudeMin) {
            _altitudeMin = altitude;
        }

        if (_lastAltitudeBinChange + _altitudeBinSizeMs > time) {
            _altitudeBins.set(
                    _altitudeBins.size()-1,
                    (_altitudeBins.get(_altitudeBins.size()-1) * _numberAltitudesInBin + altitude) / (_numberAltitudesInBin + 1)
            ); // set the current altitude into the bin and average
            _numberAltitudesInBin++;
        } else {
            _numberAltitudesInBin = 1;
            _altitudeBins.add(altitude); // create a new bin and add the altitude
            _lastAltitudeBinChange = time;
        }

    }

    public int[] getGraphData() {
        // calculate our graph based upon the stored data
        double binsPerBar = 14.0 / (double)_altitudeBins.size();
        int[] graphData = new int[14];
        double binCount = binsPerBar;

        if(binsPerBar > 1) {
            binsPerBar = 1;
            binCount = 0;
        }

        int currentBinCount =0;
        int currentBinItems =0;
        int lastBin = 0;

        for(int n=0; n < _altitudeBins.size();n++)
        {
            currentBinCount += _altitudeBins.get(n);
            currentBinItems ++;
            binCount += binsPerBar;

            if((int)binCount == lastBin + 1) {
                graphData[lastBin] = (currentBinCount / currentBinItems);
                lastBin ++;
                currentBinCount = 0;
                currentBinItems = 0;
            }
        }

        return graphData;
    }

    public void resetData() {
        _altitudeBins = new ArrayList<Integer>();
        _lastAltitudeBinChange = -1;
        _altitudeMax =0;
        _altitudeMin=99999;
    }

}
