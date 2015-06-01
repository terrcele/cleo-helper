package com.cleo.cleohelper;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class GcmIntentService extends IntentService
{
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;

    public GcmIntentService()
    {
        super("GcmIntentService");
    }

    public static final String TAG = "GCM Demo";

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty())
        {
            //Filter messages based on message type. Since it is likely that GCM will be
            //extended in the future with new message types, just ignore any message types you're
            //not interested in, or that you don't recognize.
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType))
            {
                sendNotification("Send error: " + extras.toString());
            }

            else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType))
            {
                sendNotification("Deleted messages on server: " + extras.toString());
                // If it's a regular GCM message, do some work.
            }

            else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType))
            {
                sendNotification("Received: " + extras.toString());
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    private void sendNotification(String msg)
    {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_gcm)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        PushyPush trial = new PushyPush(msg);
        trial.execute();
    }

    // Communicates with the RingTo server by inputing the user credentials and
    // obtaining the a token used for every subsequent function.
    class PushyPush extends AsyncTask<Void, Void, Void>
    {
        private String message = "";

        public PushyPush(String message)
        {
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            String pushy_token = "4218d83969b2f60aefadbd";
            String URL = "https://pushy.me/push?api_key=3841958e272f6ebda3474ccfc9df73497ef6c705e6a9cc4e7bb69ab1bdbca986";
            JSONObject jObj_returned = null;
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL);

            try
            {
                String retSrc = "";
                JSONObject jsonObjSend = new JSONObject();
                JSONObject jsonObjSend2 = new JSONObject();
                JSONArray arr = new JSONArray();
                arr.put(pushy_token);
                jsonObjSend.put("registration_ids", arr);
                jsonObjSend.put("data", jsonObjSend2.put("message", message));
                httpPost.setEntity(new StringEntity(jsonObjSend.toString()));
                Log.d("JSONObject", jsonObjSend.toString());
                httpPost.setHeader("Content-type", "application/json");
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();

                if (httpEntity != null)
                {
                    retSrc = EntityUtils.toString(httpEntity);
                    jObj_returned = new JSONObject(retSrc);
                    Log.d("MainActivity Register", jObj_returned.toString());
                }
            }

            catch (Exception e)
            {
                e.printStackTrace();
                Log.d("Error", "Cannot Estabilish Connection");
            }

            return null;
        }

        @Override
        protected void onPreExecute()
        {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
        }
    }
}

