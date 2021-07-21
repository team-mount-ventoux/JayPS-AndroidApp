package com.njackson.sensor;

import com.njackson.application.IInjectionContainer;
import com.squareup.otto.Bus;
import java.util.Set;

public interface IBle {
    public void start(Set<String> ble_addresses, Bus bus, IInjectionContainer containe);
    public void stop();
}
