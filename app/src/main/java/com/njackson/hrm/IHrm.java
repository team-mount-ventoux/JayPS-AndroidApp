package com.njackson.hrm;

import com.njackson.application.IInjectionContainer;
import com.squareup.otto.Bus;

public interface IHrm {
    public void start(String hrm_address, Bus bus, IInjectionContainer containe);
    public void stop();
}
