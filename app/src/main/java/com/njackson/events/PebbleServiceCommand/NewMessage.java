package com.njackson.events.PebbleServiceCommand;

/**
 * Created by njackson on 14/01/15.
 */
public class NewMessage {

    public String _message;

    public String getMessage() {
        return _message;
    }

    public NewMessage(String message) {
        _message = message;
    }

}
