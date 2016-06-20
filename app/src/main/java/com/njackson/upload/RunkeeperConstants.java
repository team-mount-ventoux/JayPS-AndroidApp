package com.njackson.upload;

/**
 * Constants for Runkeeper's OAuth implementation.
 *
 */
public class RunkeeperConstants {
    public static final String URL_AUTHORIZE = "https://runkeeper.com/apps/authorize";
    public static final String URL_TOKEN = "https://runkeeper.com/apps/token";
    public static final String REDIRECT_URL = "http://localhost/Callback";

    public static final String CREDENTIALS_STORE_PREF_FILE = "oauth";
    public static final String KEY_AUTH_MODE = "auth_mode";

    private RunkeeperConstants() {
    }

}

