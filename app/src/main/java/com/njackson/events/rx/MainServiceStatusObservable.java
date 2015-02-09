package com.njackson.events.rx;

import com.njackson.events.MainService.MainServiceStatus;
import com.njackson.events.base.BaseStatus;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Created by njackson on 09/02/15.
 */
public class MainServiceStatusObservable implements Observable.OnSubscribe<BaseStatus.Status> {

    private final Bus _bus;
    private Subscriber<? super BaseStatus.Status> _observer;
    private final MainServiceStatusObservable thisClass = this;

    @Subscribe
    public void onMainServiceStatus(MainServiceStatus event) {
        _observer.onNext(event.getStatus());
    }

    public MainServiceStatusObservable(Bus bus) {
        _bus = bus;
        _bus.register(this);
    }

    @Override
    public void call(Subscriber<? super BaseStatus.Status> subscriber) {
        _observer = subscriber;
        UnsubscribeAction unsubscribeAction = new UnsubscribeAction();
        _observer.add(Subscriptions.create(unsubscribeAction));
    }

    private class UnsubscribeAction implements Action0 {
        @Override
        public void call() {
            _bus.unregister(thisClass);
        }
    }
}
