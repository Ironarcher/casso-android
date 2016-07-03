package com.security.casso.casso;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.json.JSONObject;
import android.os.AsyncTask;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class FCMservice extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        //Displaying data in log
        //It is optional
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        //Get data from data object in message
        String datatype = "";
        try{
            if(remoteMessage.getData() != null){
                datatype = remoteMessage.getData().get("reqtype");
                Log.d(TAG, "Datatype: " + datatype);
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        //Calling method to generate notification
        sendNotification("Shake phone to login", 732);

        if(datatype.equals("auth")){
            //Begin phone authentication
            Log.d(TAG, "Beginning authentication");
            userVerify();
            //authenticate();
        }
    }

    //This method is only generating push notification
    //It is same as we did in earlier posts
    private void sendNotification(String messageBody, int notifyID) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Casso")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notifyID, notificationBuilder.build());
    }

    private void updateNotification(String messageBody, int notifyID){
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Casso")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notifyID, notificationBuilder.build());
    }

    private void userVerify(){
        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                //shake_status.setText(String.valueOf(count));
                mSensorManager.unregisterListener(mShakeDetector);
                Log.d(TAG, "Phone shaken");
                authenticate();
            }
        });
    }

    private void authenticate(){
        new confirm_auth_async().execute("https://casso-1339.appspot.com/app/v1.0/authenticate");
    }

    private StringBuffer confirm_auth(String urlString) {

        StringBuffer response = new StringBuffer("");
        try{
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            JSONObject param_temp = new JSONObject();
            try {
                param_temp.put("phonenumber", "5404913036");
                param_temp.put("secretphonekey", "s3BH3C7thmSX9j0K6ag6eqqqJhUTB9gOgu62Qfzf");
                param_temp.put("user_id", "34");
                param_temp.put("phone-id", "Arpad's nonexistant iphone");
                connection.connect();
            } catch(Exception e){
                Log.e(TAG, "EXCEPTION:" + Log.getStackTraceString(e));
            }

            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(param_temp.toString());
            wr.flush();

            InputStream inputStream = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }
            connection.disconnect();
        } catch (IOException e) {
            // Writing exception to log
            e.printStackTrace();
        }
        return response;
    }

    class confirm_auth_async extends AsyncTask<String, Void, StringBuffer>{

        protected StringBuffer doInBackground(String... urls){
            return confirm_auth(urls[0]);
        }

        protected void onPostExecute(StringBuffer strbuffer){
            Log.d(TAG, "result: " + strbuffer.toString());
            if(strbuffer.toString().contains("success")){
                updateNotification("Successfully logged in!", 732);
            }
        }
    }
}