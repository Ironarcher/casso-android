package com.security.casso.casso;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.os.Handler;
import org.json.JSONObject;
import java.util.Timer;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.content.Context;

public class MainActivity extends AppCompatActivity {

    //Declarations
    TextView response;
    TextView isConnected;
    TextView error;
    TextView shake_status;
    volatile boolean authenticating = false;
    String checkAuth_url = "https://casso-1339.appspot.com/app/v1.0/checkAuth/34";
    String auth_url = "https://casso-1339.appspot.com/app/v1.0/authenticate";
    int update_speed = 1000;
    volatile boolean busy = false;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    Timer refresher;
    TimerTask refresher_task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get view reference
        response = (TextView) findViewById(R.id.response);
        isConnected = (TextView) findViewById(R.id.isConnected);
        error = (TextView) findViewById(R.id.error_msg);
        shake_status = (TextView) findViewById(R.id.shake_status);

        refresher = new Timer();
        refresher_task = new AutoRefreshTask();
        refresher.scheduleAtFixedRate(refresher_task, 0, update_speed);
    }

    private class AutoRefreshTask extends TimerTask {
        @Override
        public void run(){
            if(!authenticating && !busy){
                countUp();
                checkForAuth();
            }
        }
    }

    int count = 0;
    private void countUp(){
        count++;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                error.setText(String.valueOf(count));
            }
        });
    }

    private void checkForAuth(){
        new HttpAsyncGet().execute(checkAuth_url);
    }

    private void authenticate(){
        StringEntity parameters = null;
        try {
            JSONObject param_temp = new JSONObject();
            param_temp.put("phonenumber", "5404913036");
            param_temp.put("secretphonekey", "s3BH3C7thmSX9j0K6ag6eqqqJhUTB9gOgu62Qfzf");
            param_temp.put("user_id", "34");
            param_temp.put("phone-id", "Arpad's nonexistant iphone");
            parameters = new StringEntity(param_temp.toString());
        } catch(Exception e){
            error.setText("string entity error");
        }
            HttpAsyncPostParams input_data = new HttpAsyncPostParams(auth_url, parameters);
            new HttpAsyncPost().execute(input_data);
    }

    private void userVerify(){
    // ShakeDetector initialization
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mShakeDetector = new ShakeDetector();
            startupSensor();
            mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

                @Override
                public void onShake(int count) {
                    shake_status.setText(String.valueOf(count));
                    endSensor();
                    authenticate();
                }
            });
    }

    public String GET(String url){
        busy = true;
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";
            busy = false;
        } catch (Exception e) {
            error.setText("get error");
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    //Example params: new StringEntity("{\"qty\":100,\"name\":\"iPad 4\"}");
    public String POST(String url, StringEntity params) {
        busy = true;
        InputStream inputStream = null;
        String result = "";
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(url);
            postRequest.addHeader("Content-Type", "application/json");
            params.setContentType("application/json");
            postRequest.setEntity(params);

            HttpResponse response = httpclient.execute(postRequest);
            /*
            if (response.getStatusLine().getStatusCode() != 200) {
                isConnected.setText(response.getStatusLine().getStatusCode());
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatusLine().getStatusCode());
            }
            */
            inputStream = response.getEntity().getContent();
            if (inputStream != null) {
                result = convertInputStreamToString(inputStream);
            } else {
                result = "error";
            }
            busy = false;
        } catch (Exception e) {
            error.setText("post error");
            Log.d("InputStream", e.getLocalizedMessage());
        }
        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private class HttpAsyncGet extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            //response.setText(result);
            int comm_id = Integer.parseInt(result);
            if(comm_id == 1){
                //Needs user verification
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        response.setText("Shake phone to login");
                    }
                });
                authenticating = true;
                userVerify();
            } else if(comm_id == 0){
                authenticating = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        response.setText("No Authentication required");
                    }
                });
            } else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        response.setText("Error");
                    }
                });
            }
        }
    }

    private static class HttpAsyncPostParams {
        String url;
        StringEntity data;

        HttpAsyncPostParams(String url, StringEntity data){
            this.url = url;
            this.data = data;
        }
    }

    private class HttpAsyncPost extends AsyncTask<HttpAsyncPostParams, Void, String> {

        @Override
        protected String doInBackground(HttpAsyncPostParams... params) {

            return POST(params[0].url, params[0].data);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Toast.makeText(getBaseContext(), "Received!", Toast.LENGTH_LONG).show();
            //response.setText(result);
            isConnected.setText(result);
            authenticating = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void startupSensor() {
        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    public void endSensor() {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
    }

}
