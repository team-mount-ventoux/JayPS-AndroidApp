package com.njackson.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.njackson.upload.RunkeeperConstants;
import com.njackson.upload.RunkeeperPrivateConstants;
import com.njackson.upload.StravaConstants;
import com.njackson.upload.StravaPrivateConstants;
import com.wuman.android.auth.AuthorizationDialogController;
import com.wuman.android.auth.AuthorizationFlow;
import com.wuman.android.auth.DialogFragmentController;
import com.wuman.android.auth.OAuthManager;
import com.wuman.android.auth.oauth2.store.SharedPreferencesCredentialStore;
import com.njackson.upload.AsyncResourceLoader;
import com.njackson.upload.OAuth;
import com.njackson.R;

import java.io.IOException;
import java.util.Arrays;


public class UploadActivity extends FragmentActivity {

    private static final String TAG = "PB-UploadActivity";
    private enum UploadType {
        STRAVA,
        RUNKEEPER
    }
    private UploadType _type = UploadType.STRAVA;
    private static String _userId;
    private static String _prefTokenKey;
    private static String _redirectUrl;
    private static String _credentialsStorePrefFile;
    private static String _urlToken;
    private static String _clientId;
    private static String _clientSecret;
    private static String _urlAuthorize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (null != intent) { //Null Checking
            if (intent.getStringExtra("type").equals("runkeeper")) {
                _type = UploadType.RUNKEEPER;
            }
        }
        switch (_type) {
            case STRAVA:
                Log.d(TAG, "STRAVA");
                _userId = getApplicationContext().getString(R.string.token_strava);
                _prefTokenKey = "strava_token";
                _credentialsStorePrefFile = StravaConstants.CREDENTIALS_STORE_PREF_FILE;
                _redirectUrl = StravaConstants.REDIRECT_URL;
                _urlToken = StravaConstants.URL_TOKEN;
                _clientId = StravaPrivateConstants.CLIENT_ID;
                _clientSecret = StravaPrivateConstants.CLIENT_SECRET;
                _urlAuthorize = StravaConstants.URL_AUTHORIZE;
                break;
            case RUNKEEPER:
                Log.d(TAG, "RUNKEEPER");
                _userId = getApplicationContext().getString(R.string.token_runkeeper);
                _prefTokenKey = "runkeeper_token";
                _credentialsStorePrefFile = RunkeeperConstants.CREDENTIALS_STORE_PREF_FILE;
                _redirectUrl = RunkeeperConstants.REDIRECT_URL;
                _urlToken = RunkeeperConstants.URL_TOKEN;
                _clientId = RunkeeperPrivateConstants.CLIENT_ID;
                _clientSecret = RunkeeperPrivateConstants.CLIENT_SECRET;
                _urlAuthorize = RunkeeperConstants.URL_AUTHORIZE;
                break;
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentById(android.R.id.content) == null) {
            OAuthFragment list = new OAuthFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }

    @Override
    protected void onDestroy() {
        //Crouton.cancelAllCroutons();
        super.onDestroy();
    }

    public static class OAuthFragment extends Fragment implements
            LoaderManager.LoaderCallbacks<AsyncResourceLoader.Result<Credential>> {

        private static final int LOADER_GET_TOKEN = 0;
        private static final int LOADER_DELETE_TOKEN = 1;

        private OAuthManager oauth;

        private Button button;
        private TextView message;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.delete_cookies_menu, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.delete_cookies: {
                    CookieSyncManager.createInstance(getActivity());
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeAllCookie();
                    return true;
                }
                default: {
                    return super.onOptionsItemSelected(item);
                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.oauth_login, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            button = (Button) view.findViewById(android.R.id.button1);
            setButtonText(R.string.get_token);
            message = (TextView) view.findViewById(android.R.id.text1);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (v.getTag().equals(R.string.get_token)) {
                        if (getLoaderManager().getLoader(LOADER_GET_TOKEN) == null) {
                            getLoaderManager().initLoader(LOADER_GET_TOKEN, null,
                                    OAuthFragment.this);
                        } else {
                            getLoaderManager().restartLoader(LOADER_GET_TOKEN, null,
                                    OAuthFragment.this);
                        }
                    } else { // R.string.delete_token
                        if (getLoaderManager().getLoader(LOADER_DELETE_TOKEN) == null) {
                            getLoaderManager().initLoader(LOADER_DELETE_TOKEN, null,
                                    OAuthFragment.this);
                        } else {
                            getLoaderManager().restartLoader(LOADER_DELETE_TOKEN, null,
                                    OAuthFragment.this);
                        }
                    }
                }
            });
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // setup credential store
            SharedPreferencesCredentialStore credentialStore =
                    new SharedPreferencesCredentialStore(getActivity(), _credentialsStorePrefFile, OAuth.JSON_FACTORY);

            // setup authorization flow
            AuthorizationFlow flow = new AuthorizationFlow.Builder(
                    BearerToken.queryParameterAccessMethod(),
                    OAuth.HTTP_TRANSPORT,
                    OAuth.JSON_FACTORY,
                    new GenericUrl(_urlToken),
                    new ClientParametersAuthentication(_clientId, _clientSecret),
                    _clientId,
                    _urlAuthorize)
                    .setScopes(Arrays.asList("view_private,write"))
                    .setCredentialStore(credentialStore)
                    .setRequestInitializer(new HttpRequestInitializer() {
                        @Override
                        public void initialize(HttpRequest request) throws IOException {}
                    })
                    .build();
            // setup UI controller
            AuthorizationDialogController controller =
                    new DialogFragmentController(getFragmentManager(), true) {
                        @Override
                        public String getRedirectUri() throws IOException {
                            return _redirectUrl;
                        }

                        @Override
                        public boolean isJavascriptEnabledForWebView() {
                            return true;
                        }

                        @Override
                        public boolean disableWebViewCache() {
                            return false;
                        }

                        @Override
                        public boolean removePreviousCookie() {
                            return false;
                        }

                    };
            // instantiate an OAuthManager instance
            oauth = new OAuthManager(flow, controller);
        }

        @Override
        public Loader<AsyncResourceLoader.Result<Credential>> onCreateLoader(int id, Bundle args) {
            getActivity().setProgressBarIndeterminateVisibility(true);
            button.setEnabled(false);
            message.setText("");
            if (id == LOADER_GET_TOKEN) {
                return new GetTokenLoader(getActivity(), oauth);
            } else {
                return new DeleteTokenLoader(getActivity(), oauth);
            }
        }

        @Override
        public void onLoadFinished(Loader<AsyncResourceLoader.Result<Credential>> loader,
                                   AsyncResourceLoader.Result<Credential> result) {
            Log.d(TAG, "onLoadFinished " + loader.getId());
            if (loader.getId() == LOADER_GET_TOKEN) {
                message.setText(result.success ? "Access OK" : "No access");
                if (result.data != null) {
                    ///@todo _sharedPreferences in a static context...
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("com.njackson_preferences", Context.MODE_PRIVATE).edit();
                    editor.putString(_prefTokenKey, result.data.getAccessToken());
                    editor.commit();
                }
            } else {
                message.setText("");
                SharedPreferences.Editor editor = getActivity().getSharedPreferences("com.njackson_preferences", Context.MODE_PRIVATE).edit();
                editor.putString(_prefTokenKey, "");
                editor.commit();
            }
            if (result.success) {
                if (loader.getId() == LOADER_GET_TOKEN) {
                    setButtonText(R.string.delete_token);
                } else {
                    setButtonText(R.string.get_token);
                }
            } else {
                setButtonText(R.string.get_token);
                Toast.makeText(getActivity(), result.errorMessage, Toast.LENGTH_LONG).show();
            }
            getActivity().setProgressBarIndeterminateVisibility(false);
            button.setEnabled(true);
        }

        @Override
        public void onLoaderReset(Loader<AsyncResourceLoader.Result<Credential>> loader) {
            message.setText("");
            getActivity().setProgressBarIndeterminateVisibility(false);
            button.setEnabled(true);
        }

        @Override
        public void onDestroy() {
            getLoaderManager().destroyLoader(LOADER_GET_TOKEN);
            getLoaderManager().destroyLoader(LOADER_DELETE_TOKEN);
            super.onDestroy();
        }

        private void setButtonText(int action) {
            button.setText(action);
            button.setTag(action);
        }

        private static class GetTokenLoader extends AsyncResourceLoader<Credential> {

            private final OAuthManager oauth;

            public GetTokenLoader(Context context, OAuthManager oauth) {
                super(context);
                this.oauth = oauth;
            }

            @Override
            public Credential loadResourceInBackground() throws Exception {
                Credential credential =
                        oauth.authorizeExplicitly(_userId, null, null).getResult();
                Log.i(TAG, "token: " + credential.getAccessToken());
                return credential;
            }

            @Override
            public void updateErrorStateIfApplicable(AsyncResourceLoader.Result<Credential> result) {
                Credential data = result.data;
                result.success = !TextUtils.isEmpty(data.getAccessToken());
                result.errorMessage = result.success ? null : "error";
            }

        }

        private static class DeleteTokenLoader extends AsyncResourceLoader<Credential> {

            private final OAuthManager oauth;
            private boolean success;

            public DeleteTokenLoader(Context context, OAuthManager oauth) {
                super(context);
                this.oauth = oauth;
            }

            @Override
            public Credential loadResourceInBackground() throws Exception {
                success = oauth.deleteCredential(_userId, null, null).getResult();
                Log.i(TAG, "token deleted: " + success);
                return null;
            }

            @Override
            public void updateErrorStateIfApplicable(Result<Credential> result) {
                result.success = success;
                result.errorMessage = result.success ? null : "error";
            }

        }

    }

}
