package com.example.flower.translucentactivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    Thread myThread = null;
    String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.WRITE_SECURE_SETTINGS,
    };

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
                try {
                    myThread = new Thread(new MyServerThread());

                    myThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.MyRadioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // checkedId is the RadioButton selected
                //Get GPS now state (open or closed)

                switch(checkedId){
                    case R.id.HighAccuracy:
                        Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 0);
                        break;
                    case R.id.BatterySaving:
                        Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 1);
                        break;
                    case R.id.GPSOnly:
                        Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 3);
                        break;
                }

//                } else {
//                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 3);
//                }
            }
        });

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if(!checkPermissions()){
                Toast.makeText(getApplicationContext(), "Permissions Not Allowed", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(getApplicationContext(), "Permissions Allowed", Toast.LENGTH_LONG).show();
            }
        }
        try {
            myThread = new Thread(new MyServerThread());
        } catch (IOException e) {
            e.printStackTrace();
        }
        myThread.start();

    }


    class MyServerThread implements Runnable
    {

       private void setMock(double latitude, double longitude, double altitude, float bearing) {
           LocationManager locMgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
           locMgr.addTestProvider (LocationManager.GPS_PROVIDER,
                    "requiresNetwork" == "",
                    "requiresSatellite" == "",
                    "requiresCell" == "",
                    "hasMonetaryCost" == "",
                    true,
                    "supportsSpeed" == "",
                    true,
                Criteria.POWER_LOW,
                Criteria.ACCURACY_HIGH);

           Location newLocation = new Location(LocationManager.GPS_PROVIDER);

           newLocation.setLatitude(latitude);
           newLocation.setLongitude(longitude);
           newLocation.setAltitude(altitude);
           newLocation.setBearing(bearing);
           newLocation.setAccuracy(5);
           newLocation.setTime(System.currentTimeMillis());
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
               newLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
           }
           locMgr.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);

           locMgr.setTestProviderStatus(LocationManager.GPS_PROVIDER,
               LocationProvider.AVAILABLE,
                   null,System.currentTimeMillis());

           locMgr.setTestProviderLocation(LocationManager.GPS_PROVIDER, newLocation);
       }


        MyServerThread() throws IOException {
        }

        @Override
        public void run() {
            try {
                DatagramSocket dsocket = new DatagramSocket(7801);

                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                long startTime = System.currentTimeMillis();

                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                final String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

                while(true){
//                    CONSIDER TESTING WITH THESE ALSO
//                    long currentTime = System.currentTimeMillis();
//                    if( currentTime - startTime > 300){
//                        startTime = currentTime;
                    dsocket.receive(packet);
                    String message = new String(buffer, 0, packet.getLength());
                    Log.e("message", message);
                    if(message.equals("CheckConnection")){
                        Log.e("if", "works");
                        byte[] text = "Online".getBytes();

                        DatagramPacket response = new DatagramPacket(text, text.length, packet.getAddress(), packet.getPort());
                        DatagramSocket rdsocket = new DatagramSocket();
                        rdsocket.send(packet);
                        rdsocket.close();
                    }
                    else{
                        final String[] MockLoc = message.split(",");

                        runOnUiThread(new Runnable() {
                            public void run(){
                                TextView tvIP = (TextView) findViewById(R.id.IPAddressView);
                                tvIP.setText(ip);

                                TextView tvLat = (TextView) findViewById(R.id.LatitudeView);
                                tvLat.setText(MockLoc[0]);

                                TextView tvLon = (TextView) findViewById(R.id.LongitudeView);
                                tvLon.setText(MockLoc[1]);
                            }
                        });

                        setMock(Double.parseDouble(MockLoc[0]), Double.parseDouble(MockLoc[1]), Double.parseDouble(MockLoc[2]), Float.parseFloat(MockLoc[3]));
                    }
                    packet.setLength(buffer.length);
                }

//                }
            }
            catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        }

//        @Override
//            public void run(){
//            Socket s = null;
//            InputStreamReader isr = null;
//            BufferedReader br = null;
//            String message = "empty";
//
//            ServerSocket ss = null;
//
//
//            try {
//                ss = new ServerSocket(7801);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            while(true) {
//                try {
//                    s = ss.accept();
//                    Log.e("thread", "Is Running");
//                    isr = new InputStreamReader(s.getInputStream());
//                    br = new BufferedReader(isr, 1024);
//                    message = br.readLine();
//                    Log.e("message", message);
//                } catch (IOException e) {
//                    e.printStackTrace();
//
//                }
//
//                String[] MockLoc = message.split(",");
//                Log.e("Location", message);
//                setMock(Double.parseDouble(MockLoc[0]), Double.parseDouble(MockLoc[1]), Double.parseDouble(MockLoc[2]));
//
//            }
//        }
    }
}

