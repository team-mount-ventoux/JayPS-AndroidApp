package com.njackson.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: njackson
 * Date: 24/08/2013
 * Time: 20:14
 * To change this template use File | Settings | File Templates.
 */
public class AltitudeGraphReduce {

    private static AltitudeGraphReduce _instance;

    private ArrayList<Integer> _altitudeBins = new ArrayList<Integer>();
    private Date _lastAltitudeBinChange = null;
    private int _altitudeBinSizeMs = 120000;

    private int _altitudeMax = 0;
    private int _altitudeMin = 99999;

    public void setBinInterval(int value) {
        _altitudeBinSizeMs = value;
    }

    public int getMax() {
        return _altitudeMax;
    }

    public int getMin() {
        return _altitudeMin;
    }

    public ArrayList<Integer> getCache() {
        return _altitudeBins;
    }

    public AltitudeGraphReduce() {

    }

    public static AltitudeGraphReduce getInstance() {
        if(_instance == null)
            _instance = new AltitudeGraphReduce();
        return _instance;
    }

    public void addAltitude(int altitude) {

        if(_lastAltitudeBinChange == null) {
            _altitudeBins.add(0); // initialise the first bin
            _lastAltitudeBinChange = new Date();
        }

        if(altitude > _altitudeMax)
            _altitudeMax = altitude;
        if(altitude < _altitudeMin)
            _altitudeMin = altitude;

        Calendar cal = Calendar.getInstance();
        cal.setTime(_lastAltitudeBinChange);
        cal.add(Calendar.MILLISECOND, _altitudeBinSizeMs);
        Log.d("PebbleBike", cal.getTime().toString() + " " + new Date().toString());
        if(new Date().before(cal.getTime())) {
            _altitudeBins.set(
                    _altitudeBins.size()-1,
                    (_altitudeBins.get(_altitudeBins.size()-1) + altitude) / 2
            ); // set the current altitude into the bin and average
        } else {
            _altitudeBins.add(altitude); // create a new bin and add the altitude
            _lastAltitudeBinChange = new Date();
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

    public void restData() {
        _altitudeBins = new ArrayList<Integer>();
        _lastAltitudeBinChange = null;
        _altitudeMax =0;
        _altitudeMin=99999;
    }

}
