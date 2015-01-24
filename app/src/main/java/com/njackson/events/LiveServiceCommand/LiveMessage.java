package com.njackson.events.LiveServiceCommand;

import com.njackson.live.LiveTracking;

/**
 * Created by jay on 10/01/15.
 */
public class LiveMessage {

    private String _name0 = null;
    public String getName0() {
        return _name0;
    }
    public void setName0(String name) {
        this._name0 = name;
    }

    private String _name1 = null;
    public String getName1() {
        return _name1;
    }
    public void setName1(String name) {
        this._name1 = name;
    }

    private String _name2 = null;
    public String getName2() {
        return _name2;
    }
    public void setName2(String name) {
        this._name2 = name;
    }

    private String _name3 = null;
    public String getName3() {
        return _name3;
    }
    public void setName3(String name) {
        this._name3 = name;
    }

    private String _name4 = null;
    public String getName4() {
        return _name4;
    }
    public void setName4(String name) {
        this._name4 = name;
    }

    private byte[] _live;
    public byte[] getLive() {
        return _live;
    }
    public void setLive(byte[] live) {
        this._live = live;
    }


    void LiveMessage() {
        _live = new byte[1 + LiveTracking.maxNumberOfFriend * LiveTracking.sizeOfAFriend];
    }


}
