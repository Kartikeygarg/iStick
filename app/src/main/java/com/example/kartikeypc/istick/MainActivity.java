package com.example.kartikeypc.istick;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.SystemRequirementsChecker;
import com.estimote.sdk.repackaged.retrofit_v1_9_0.retrofit.RestAdapter;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    public static Map<String, List<String>> PLACES_BY_BEACONS;
    String CurrentPlace = "Unkown";
    boolean connection = false;
    String dstAddress = "172.20.10.3";//"172.20.122.187";
    String dstAddress_DOOR = "172.20.10.4";
    int dstPort = 2390;
    boolean nearDoor = false;
    Button btn_add_number, btn_send_message;
    add_number ad;
    TextView log_textView;
    protected LocationManager mLocationManager;


    static {
        //Contains detail of all the beacons
        Map<String, List<String>> placesByBeacons = new HashMap<>();

        //BlueBerry Beacon (Kitchen)
        placesByBeacons.put("19625:20981", new ArrayList<String>() {{
            add("Kitchen");

        }});

        //Ice Beacon (Bedroom)
        placesByBeacons.put("37550:26353", new ArrayList<String>() {
            {
                add("Bedroom");
            }

        });

        //Mint Beacon (Living Room)
        placesByBeacons.put("24150:27099", new ArrayList<String>() {{
            add("LivingRoom");
        }});

        PLACES_BY_BEACONS = placesByBeacons;
    }

    public List<String> placesNearBeacon(Beacon beacon) {
        String beaconKey = String.format("%d:%d", beacon.getMajor(), beacon.getMinor());

        if (PLACES_BY_BEACONS.containsKey(beaconKey)) {
            return PLACES_BY_BEACONS.get(beaconKey);

        }
        return Collections.emptyList();
    }

    public BeaconManager beaconManager;
    public Region region;
    boolean flag = false;
    MyLocationListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ad = new add_number(MainActivity.this);

        btn_send_message = (Button) findViewById(R.id.btn_send_message);
        btn_add_number = (Button) findViewById(R.id.btn_add_number);
        btn_add_number.getBackground().setAlpha(120);
        log_textView = (TextView) findViewById(R.id.log_textView);

        btn_add_number.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ad.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                ad.show();
            }
        });


        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"Location Permissions required.",Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    12);
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);


        btn_send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<String> num = ad.getNum_list();
                Log.i("Parth Connection", "List size" + num.size());
                for (int i = 0; i < num.size(); i++) {
                    Log.i("Parth Connection", "List " + num.get(i));
                    try {
                        SmsManager smsManager = SmsManager.getDefault();

                        Location currentLocation = getLastBestLocation();
                        StringBuffer smsBody = new StringBuffer();
                        smsBody.append("Hello, I need help. My location is :  ");
                        smsBody.append("http://maps.google.com?q=");
                        smsBody.append(currentLocation.getLatitude());
                        smsBody.append(",");
                        smsBody.append(currentLocation.getLongitude());
                        smsManager.sendTextMessage(num.get(i), null, smsBody.toString(), null, null);
                         //  Toast.makeText(getApplicationContext(), "Message Sent",Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        //  Toast.makeText(getApplicationContext(),ex.getMessage().toString(),Toast.LENGTH_LONG).show();
                        ex.printStackTrace();
                    }
                }
            }
        });

        beaconManager = new BeaconManager(this);
        MyClientTask myClientTask = new MyClientTask();
        myClientTask.execute();

        Button find_stick = (Button) findViewById(R.id.find_stick);
        find_stick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Socket Connection", "FIND STICK BTN PRESSED");
                    /*int resID=getResources().getIdentifier("music", "raw", getPackageName());

                    MediaPlayer mediaPlayer=MediaPlayer.create(getApplicationContext(),resID);
                    mediaPlayer.start();*/
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        byte[] send_array = new byte[4];
                        System.arraycopy(toByteArray("FIND"), 0, send_array, 0, 4);
                        try {
                            DatagramPacket sendMsg = new DatagramPacket(send_array, send_array.length, InetAddress.getByName(dstAddress), dstPort);
                            DatagramSocket sendSocket = new DatagramSocket();
                            sendSocket.setSoTimeout(5000);
                            sendSocket.send(sendMsg);
                            Log.i("Socket Connection", "Find Stick MESSAGE SENT");

                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (SocketException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };

                thread.start();
            }
        });

        beaconManager.setRangingListener(new BeaconManager.RangingListener() {

            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> list) {
                if (!list.isEmpty()) {
                    Beacon nearestBeacon = list.get(0);

                    List<String> places = PLACES_BY_BEACONS.get(String.format("%d:%d", nearestBeacon.getMajor(), nearestBeacon.getMinor()));

                    //Measured Power used to determined if it is near to door
                    int measuredPower = nearestBeacon.getMeasuredPower();
                    Log.i("Socket Connection", "M . P . " + measuredPower);

                    //Opening Door if user is very close to it
                    if (measuredPower <= -85 && nearestBeacon.getMajor() == 19625) {
                        if (!nearDoor) {
                            nearDoor = true;
                            Log.d("MPower Door Function", "Door Opened");
                            nearDoor = true;
                            Thread thread = new Thread() {
                                @Override
                                public void run() {
                                    byte[] send_array = new byte[4];
                                    System.arraycopy(toByteArray("DOOR"), 0, send_array, 0, 4);
                                    try {
                                        DatagramPacket sendMsg = new DatagramPacket(send_array, send_array.length, InetAddress.getByName(dstAddress_DOOR), dstPort);
                                        DatagramSocket sendSocket = new DatagramSocket();
                                        sendSocket.setSoTimeout(5000);
                                        sendSocket.send(sendMsg);
                                        Log.i("Socket Connection", "DOOR OPEN MESSAGE SENT");

                                    } catch (UnknownHostException e) {
                                        e.printStackTrace();
                                    } catch (SocketException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                            };

                            thread.start();
                        }


                    } else {
                        nearDoor = false;
                    }

                    //Rssi is used to
                    int rssi = nearestBeacon.getRssi();
                    Log.d("Rssi", "places" + rssi + " " + places);

                    //Range in which Beacon can be discovered
                    if (rssi <= -40 && rssi >= -95) {
                        CurrentPlace = places.get(0);
                        //On receiving signal turn the flag on
                        if (flag == true) {
                            //Call for the audio File
                            //     Log.d("Beackon", "" + places + "  Rssi:" + rssi);
                        }
                    } else {
                        CurrentPlace = "Unkown";
                        //nearDoor = false;
                    }

                    //Update UI here
                } else {
                    Log.i("Socket Connection", "List empty");
                }
            }

        });

        //Identify the Beacon
        region = new Region("ranged region", UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), null, null);
    }

    public Location getLastBestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long GPSLocationTime = 0;
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if ( 0 < GPSLocationTime - NetLocationTime ) {
            return locationGPS;
        }
        else {
            return locationNet;
        }
    }

        @Override
        protected void onResume() {
            super.onResume();

            SystemRequirementsChecker.checkWithDefaultDialogs(this);

            beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
                @Override
                public void onServiceReady() {
                    beaconManager.startRanging(region);
                }
            });
        }

        @Override
        protected void onPause() {
            beaconManager.stopRanging(region);

            super.onPause();
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            //getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        }

    byte[] toByteArray(String value) {
        byte a[] =  new byte[] {
                (byte)(value.charAt(0) ),
                (byte)(value.charAt(1) ),
                (byte)(value.charAt(2)),
                (byte)value.charAt(3)   };

        return a;
    }

    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location loc)
        {
            //  Log.i("TAG", "Location Listner");
          /*  if( isBetterLocation(loc, previousBestLocation)  ) {


                Log.i("TAG", "Setting new Location Location Diff = " + measure(previousBestLocation, loc));
                previousBestLocation = loc;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txt_latitude.setText("" + loc.getLatitude());
                        txt_longitude.setText("" + loc.getLongitude());
                    }
                });
                /*intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);*/

            //}
        }

        public void onProviderDisabled(String provider)
        {
            Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
        }


        public void onProviderEnabled(String provider)
        {
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }



    public class MyClientTask extends AsyncTask<Void, Void, Void> {


            String message = "Hello, please help";

            public void sendSMS(String msg) {
                List<String> num = ad.getNum_list();
                Log.i("Parth Connection", "List size" + num.size() );
                for(int i=0;i<num.size();i++) {
                    Log.i("SMS Connection", "List " + num.get(i));
                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(num.get(i), null, msg, null, null);
                        Toast.makeText(getApplicationContext(), "Message Sent",Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        //  Toast.makeText(getApplicationContext(),ex.getMessage().toString(),Toast.LENGTH_LONG).show();
                        ex.printStackTrace();
                    }
                }

            }

            @Override
            protected Void doInBackground(Void... params) {


                try {
                    DatagramSocket clientSocket = new DatagramSocket();
                    clientSocket.setSoTimeout(5000);

                    byte[] ping_array = new byte[4];
                    byte[] input_array = new byte[12];
                    System.arraycopy(toByteArray("S@S@"),0,ping_array,0,4);

                    DatagramPacket sendPing = new DatagramPacket(ping_array, ping_array.length, InetAddress.getByName(dstAddress) , dstPort);
                    DatagramPacket receivePing = new DatagramPacket(input_array, input_array.length);

                    while(!connection) {
                        try {
                            clientSocket.send(sendPing);
                            clientSocket.receive(receivePing);
                            //sendSMSMessage();
                            String str = new String(input_array, "UTF-8");
                            Log.d("Socket Connection", " Recevied reply : " + str);
                            if (str.equalsIgnoreCase("acknowledged")) {
                                connection = true;
                                runOnUiThread(new Runnable() //run on ui thread
                                {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Connection made",Toast.LENGTH_LONG).show();
                                        log_textView.setText("Connection has been made...");
                                    }
                                });
                                Log.i("Socket Connection", " Connection set true" );
                            }
                            //Log.i("Socket Connection", " Recevied reply : " + str);
                        }
                        catch (SocketTimeoutException e) {
                            Log.d("Socket Connection"," Time Out Reached");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }
                    while(true)
                    {

                        try {
                            //clientSocket.send(sendPing);
                            clientSocket.receive(receivePing);
                            String str = new String(input_array, "UTF-8");
                            Log.i("Socket Connection"," Recevied reply : "+ str);
                            if(str.equalsIgnoreCase("PANIC BTN..."))
                            {
                               // Toast.makeText(getApplicationContext(), "Connection made",Toast.LENGTH_LONG).show();
                                runOnUiThread(new Runnable() //run on ui thread
                                {
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Panic Btn made",Toast.LENGTH_LONG).show();
                                        log_textView.setText("Panic button pressed...");
                                    }
                                });
                                Log.i("Socket Connection","SMS MESSAGE ");
                                StringBuffer smsBody = new StringBuffer();
                                Location currentLocation = getLastBestLocation();
                                smsBody.append("Hello, I need help. I am in "+CurrentPlace+". My location is :  ");
                                smsBody.append("http://maps.google.com?q=");
                                smsBody.append(currentLocation.getLatitude());
                                smsBody.append(",");
                                smsBody.append(currentLocation.getLongitude());
                               // smsManager.sendTextMessage(num.get(i), null, smsBody.toString(), null, null);


                                /*smsBody.append(currentLocation.getLatitude());
                                smsBody.append(",");
                                smsBody.append(currentLocation.getLongitude());
                                */
                                sendSMS(smsBody.toString());
                            }
                            else if(str.equalsIgnoreCase("LOCATION BTN"))
                            {
                                Log.i("Socket Connection","Location MESSAGE , Current Place"+ CurrentPlace);
                                if(!CurrentPlace.equalsIgnoreCase("Unkown"))
                                {
                                    runOnUiThread(new Runnable() //run on ui thread
                                    {
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Location Btn made",Toast.LENGTH_LONG).show();
                                            log_textView.setText("Location button pressed... Loca : "+ CurrentPlace);

                                            int resID = getResources().getIdentifier(CurrentPlace.toLowerCase(), "raw", getPackageName());
                                            MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), resID);
                                            mediaPlayer.start();
                                        }
                                    });
                                    }
                                else
                                {
                                    runOnUiThread(new Runnable() //run on ui thread
                                    {
                                        public void run() {
                                            Toast.makeText(getApplicationContext(), "Location Btn made",Toast.LENGTH_LONG).show();
                                            log_textView.setText("Location button pressed... LOCATIO UNKWN");
                                        }
                                    });

                                }
                            }
                            else
                            {


                            }


                        } catch (SocketTimeoutException e) {
                            //Log.i("Socket Connection"," Time Out Reached 2");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }  catch (IOException e) {
                    e.printStackTrace();
                }


                return null;



            }





        }



    }
