package de.dpc.acceleratorpublic;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by lstrohbuecker on 29.03.2017.
 */

public class PhoneAccelerometer implements SensorEventListener {

        private static final String TAG = PhoneAccelerometer.class.getSimpleName();
        private SensorManager mSensorManager;
        private WindowManager mWindowManager;
        private long[] mAccelGravityTime = new long[20];
        private float[][] mAccelGravityData = new float[20][3];
        //private float[][] mGeomagneticData = new float[100][3];
        private float[] mRotationMatrix = new float[16];
        private float[] bufferedAccelGData = new float[3];
        private float[] bufferedMagnetData = new float[3];
        private int count = 0;
        private int rcount = 0;
        public TextView mTextMsg;

        public PhoneAccelerometer(Context context, TextView mTextMessage) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            mTextMsg = mTextMessage;
        }

        public void start() {
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME );
            //mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME );
        }

        public void stop() {
            mSensorManager.unregisterListener(this);
        }

        private void sendSensorData() {
            try {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < 20; i++) {
                    builder.append(String.valueOf(mAccelGravityTime[i]));
                    builder.append(";");
                    builder.append(String.valueOf(mAccelGravityData[i][0]));
                    builder.append(";");
                    builder.append(String.valueOf(mAccelGravityData[i][1]));
                    builder.append(";");
                    builder.append(String.valueOf(mAccelGravityData[i][2]));
                    builder.append("\n");
                }
                new SendHttpRequestTask().execute("http://acc-lstroh.rhcloud.com/loadData/",builder.toString());
                mTextMsg.setText("waiting for Result\n");
            }catch (Exception se){
                String msg = se.getMessage();
                //mTextMsg.setText("ERROR:\n" + msg );
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        /* Sensor Processing/Rotation Matrix
         * Each time a sensor update happens the onSensorChanged method is called.
         * This is where we receive the raw sensor data.
         * First of all we want to take the sensor data from the accelerometer and magnetometer and smooth it out to reduce jitters.
         * From there we can call the getRotationMatrix function with our smoothed accelerometer and magnetometer data.
         * The rotation matrix that this outputs is mapped to have the y axis pointing out the top of the phone, so when the phone is flat on a table facing north, it will read {0,0,0}.
         * We need it to read {0,0,0} when pointing north, but sitting vertical. To achieve this we simply remap the co-ordinates system so the X axis is negative.
         * The following code example shows how this is acheived.
         */
        @Override
        public void onSensorChanged(SensorEvent event) {

            count = count + 1;
            if (count < 10) return;
            long t = System.currentTimeMillis();
            if (rcount < 20){// 20 Datensätze sammeln
                mAccelGravityTime[rcount] = t;
                mAccelGravityData[rcount][0]=(mAccelGravityData[rcount][0]*2+event.values[0])*0.33334f;
                mAccelGravityData[rcount][1]=(mAccelGravityData[rcount][1]*2+event.values[1])*0.33334f;
                mAccelGravityData[rcount][2]=(mAccelGravityData[rcount][2]*2+event.values[2])*0.33334f;
               /* mTextMsg.append("Daten\n");
                mTextMsg.append("x:" + mAccelGravityData[rcount][0]);
                mTextMsg.append("y:" + mAccelGravityData[rcount][1]);
                mTextMsg.append("x:" + mAccelGravityData[rcount][2]);*/
                rcount = rcount + 1;
            }else {
                rcount = 0;
                sendSensorData();
                //mTextMsg.setText("Daten Sensor\n");
                /*mTextMsg.append("x:" + event.values[0]);
                mTextMsg.append("y:" + event.values[1]);
                mTextMsg.append("x:" + event.values[2]);*/
                mAccelGravityTime[rcount] = t;
                mAccelGravityData[rcount][0]=(mAccelGravityData[rcount][0]*2+event.values[0])*0.33334f;
                mAccelGravityData[rcount][1]=(mAccelGravityData[rcount][1]*2+event.values[1])*0.33334f;
                mAccelGravityData[rcount][2]=(mAccelGravityData[rcount][2]*2+event.values[2])*0.33334f;
            }
            count = 0;
        }

        public String sendHttpRequest(String sturl, String body) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            String msg = "sendHttpRequest Start";
            boolean doInput = true;
            try {
                sturl = sturl + body;
                URL url = new URL(sturl);
                HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
                //urlConn.connect();
                // Timeout for reading InputStream arbitrarily set to 3000ms.
                urlConn.setReadTimeout(3000);
                // Timeout for connection.connect() arbitrarily set to 3000ms.
                urlConn.setConnectTimeout(3000);
                // For this use case, set HTTP method to GET.
                urlConn.setRequestMethod("POST");
                urlConn.setDoOutput(true);
                //urlConn.setDoInput(false);
                urlConn.connect();
                urlConn.getOutputStream().write(body.getBytes());

                if (doInput) {
                    // Get the server response
                    /*InputStream is = urlConn.getInputStream();
                    byte[] b = new byte[1024];
                    while (is.read(b) != -1)
                        baos.write(b);*/
                    msg = msg + "Response Code " + urlConn.getResponseCode();
                    msg = msg + "Message " + urlConn.getResponseMessage();
                }
                urlConn.disconnect();
                //msg = baos.toString();
            } catch (Exception ex) {
                msg = ex.getMessage();
                StackTraceElement[] tracing = ex.getStackTrace();
                for (int i = 0; i < tracing.length; i++) {
                    msg = msg + "\n" + tracing[i].getFileName() + ":" + tracing[i].getLineNumber();
                    msg = msg + " method:" + tracing[i].getMethodName();
                }
            } finally {
                try {
                    baos.close();
                } catch (Exception exe) {
                }
            }
            //edresponse.setText(msg);
            return msg;
        }


        private class SendHttpRequestTask extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... params) {
                String url = params[0];
                String name = params[1];
                String data = sendHttpRequest(url, name);
                return data;
            }

            @Override
            protected void onPostExecute(String result) {
                mTextMsg.append(result);
                //item.setActionView(null);
            }
        }
    }
