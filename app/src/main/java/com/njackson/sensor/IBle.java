package com.njackson.sensor;

import com.njackson.application.IInjectionContainer;
import com.squareup.otto.Bus;

public interface IBle {
    public void start(String ble_address1, String ble_address2, String ble_address3, Bus bus, IInjectionContainer containe);
    public void stop();
}
