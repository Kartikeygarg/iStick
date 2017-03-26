package com.example.kartikeypc.istick;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.List;
import java.util.UUID;

/**
 * Created by KartikeyPC on 10-03-2017.
 */
public class iStick extends Application {

    public BeaconManager beaconManager;

    @Override
    public void onCreate() {
        super.onCreate();

        beaconManager = new BeaconManager(getApplicationContext());
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(com.estimote.sdk.Region region, List<Beacon> list) {
                showNotification("iStick Connected", "Connection Successfull");
            }

            @Override
            public void onExitedRegion(com.estimote.sdk.Region region) {
                showNotification("iStick Not Connected", "Closing Connection");
            }
        });

        //BlueBerry Beacon (Kitchen)
        beaconManager.connect(new BeaconManager.ServiceReadyCallback(){
            @Override
            public void onServiceReady(){
                beaconManager.startMonitoring(new Region("BlueBerry Region",
                        UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), 19625, 20981));
            }
        });

        //Ice Beacon (Bedroom)
        beaconManager.connect(new BeaconManager.ServiceReadyCallback(){
            @Override
            public void onServiceReady(){
                beaconManager.startMonitoring(new Region("Ice Region",
                        UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), 37550, 26353));
            }
        });

        //Mint Beacon (Living Room)
        beaconManager.connect(new BeaconManager.ServiceReadyCallback(){
            @Override
            public void onServiceReady(){
                beaconManager.startMonitoring(new Region("Mint Region",
                        UUID.fromString("B9407F30-F5F8-466E-AFF9-25556B57FE6D"), 24150, 27099));
            }
        });
    }


    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);

        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[]{notifyIntent}, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        notification.defaults |= Notification.DEFAULT_SOUND;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(1, notification);
    }

}
