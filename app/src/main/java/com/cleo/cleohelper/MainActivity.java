package com.cleo.cleohelper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.provider.Settings.Secure;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity
{
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    static final String TAG = "MainActivity";
    private Login login = null;
    private GoogleCloudMessaging gcm;
    private AtomicInteger msgId = new AtomicInteger();
    private Context context;
    private String regid;

    // Substitute you own sender ID here. This is the project number you got from the API Console.
    String SENDER_ID = "844617561654";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        if(savedInstanceState == null)
        {
            // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
            if (checkPlayServices())
            {
                gcm = GoogleCloudMessaging.getInstance(this);
                regid = getRegistrationId(context);

                if (regid.isEmpty())
                {
                    registerInBackground();
                }

                else
                {
                    login = new Login(context, regid);
                    login.execute();
                }
            }

            else
            {
                Log.i(TAG, "No valid Google Play Services APK found.");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        checkPlayServices();
    }

    // Check device for Play Services APK.
    private boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }

            else
            {
                Log.i(TAG, "This device is not supported.");
                finish();
            }

            return false;
        }

        return true;
    }

    //Stores the registration ID and the app versionCode in the application's
    private void storeRegistrationId(Context context, String regId)
    {
        final SharedPreferences prefs = getGcmPreferences();
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    //Gets the current registration ID for application on GCM service, if there is one.
    private String getRegistrationId(Context context)
    {
        final SharedPreferences prefs = getGcmPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");

        try
        {
            if(registrationId != null)
            {
                if (registrationId.isEmpty())
                {
                    Log.i(TAG, "Registration not found.");
                    return "";
                }
            }
        }

        catch(NullPointerException e)
        {}

        //Get saved registration ID from last login.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);

        if (registeredVersion != currentVersion)
        {
            Log.i(TAG, "App version changed.");
            return "";
        }

        return registrationId;
    }

    //Registers the application with GCM servers asynchronously.
    // Stores the registration ID and the app versionCode in the application's sharedpreferences
    private void registerInBackground()
    {
        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                String msg = "";

                try
                {
                    if (gcm == null)
                    {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }

                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    sendRegistrationIdToBackend();
                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                }

                catch (IOException ex)
                {
                    msg = "Error :" + ex.getMessage();
                }

                return msg;
            }

            @Override
            protected void onPostExecute(String msg)
            {
                login = new Login(context, regid);
                login.execute();
            }
        }.execute(null, null, null);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    //Get application's version code from the {@code PackageManager}.
    private static int getAppVersion(Context context)
    {
        try
        {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        }

        catch (NameNotFoundException e)
        {
            // Should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    // Return sharedpreferences.
    private SharedPreferences getGcmPreferences()
    {
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    // Register ID on the server.
    private void sendRegistrationIdToBackend()
    {
        // Your implementation here.
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Communicates with the RingTo server by inputing the user credentials and
    // obtaining the a token used for every subsequent function.
    class Login extends AsyncTask<Void, Void, Void>
    {
        private String uuid = "";
        private Context context = null;
        private String retSrc = "";
        private String register_id = "";
        final static String URL = "https://ring.to/api/user";
        final static String URL2 = "https://ring.to/api/pusher";
        private JSONObject jObj_returned = null;
        private String android_id = "";

        public Login(Context context, String register_id)
        {
            this.context = context;
            this.register_id = register_id;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            String email = "";
            String mpassword = "";

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL);

            try
            {
                // RingTo API to obtain token
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
                nameValuePairs.add(new BasicNameValuePair("action", "l"));
                nameValuePairs.add(new BasicNameValuePair("password", ""));
                nameValuePairs.add(new BasicNameValuePair("token", "t"));
                nameValuePairs.add(new BasicNameValuePair("email", ""));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();

                if (httpEntity != null)
                {
                    retSrc = EntityUtils.toString(httpEntity);
                    jObj_returned = new JSONObject(retSrc);
                    jObj_returned = (JSONObject) jObj_returned.get("user");
                    retSrc = (String) jObj_returned.get("token");
                    uuid = retSrc;
                    Log.d("MainActivity Token", uuid);
                }
            }

            catch (Exception e)
            {
                e.printStackTrace();
                Log.d("Error", "Cannot Estabilish Connection");
                return null;
            }

            httpClient = new DefaultHttpClient();
            httpPost = new HttpPost(URL2);

            try
            {
                android_id = Secure.getString(context.getContentResolver(),
                        Secure.ANDROID_ID);
                httpPost.setHeader("JB-Token", uuid);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
                nameValuePairs.add(new BasicNameValuePair("action", "r"));
                nameValuePairs.add(new BasicNameValuePair("platform", "a"));
                nameValuePairs.add(new BasicNameValuePair("registration_id", register_id));
                nameValuePairs.add(new BasicNameValuePair("device_id", android_id));
                Log.d("MainActivity RegisterID", register_id);
                Log.d("MainActivity Android ID", android_id);
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();

                if (httpEntity != null)
                {
                    retSrc = EntityUtils.toString(httpEntity);
                    jObj_returned = new JSONObject(retSrc);
                    boolean found_it = (boolean) jObj_returned.get("ok");
                    Log.d("MainActivity Register", Boolean.toString(found_it));
                }
            }

            catch (Exception e)
            {
                e.printStackTrace();
                Log.d("Error", "Cannot Estabilish Connection");
            }

            return null;
        }
    }
}
