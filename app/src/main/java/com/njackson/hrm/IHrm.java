package com.njackson.hrm;

import com.squareup.otto.Bus;

public interface IHrm {
    public void start(String hrm_address, Bus bus);
    public void stop();
}
