
package com.koshibar.tun;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import org.json.JSONObject;

import java.io.IOException;

public class KoshibarVpnService extends VpnService {

    public static final String ACTION_CONNECT =
            "com.koshibar.tun.CONNECT";

    public static final String ACTION_DISCONNECT =
            "com.koshibar.tun.DISCONNECT";

    private static final String CHANNEL_ID = "koshibar_vpn";
    private static final int NOTIFICATION_ID = 1001;

    private ParcelFileDescriptor vpnInterface;
    private Thread worker;
    private volatile boolean stopping = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_DISCONNECT.equals(action)) {
            stopVpn();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_CONNECT.equals(action)) {
            String config = intent.getStringExtra("config");

            if (config == null) {
                reportStatus("disconnected", "Configuration absente");
                stopSelf();
                return START_NOT_STICKY;
            }

            try {
                // Vérifie seulement que la configuration reçue est du JSON valide.
                new JSONObject(config);
            } catch (Exception e) {
                reportStatus("disconnected", "Configuration invalide");
                stopSelf();
                return START_NOT_STICKY;
            }

            stopVpn();
            stopping = false;
            startForeground(NOTIFICATION_ID, buildNotification(
                    "Préparation du tunnel VPN"));

            worker = new Thread(() -> establishInterface());
            worker.start();
        }

        return START_NOT_STICKY;
    }

    private void establishInterface() {
        ParcelFileDescriptor established = null;

        try {
            Builder builder = new Builder()
                    .setSession("KOSHIBAR TUN")
                    .addAddress("10.10.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .setMtu(1500);

            established = builder.establish();

            if (established == null) {
                reportStatus("disconnected",
                        "Android a refusé de créer le tunnel");
                stopSelf();
                return;
            }

            synchronized (this) {
                if (stopping) {
                    established.close();
                    return;
                }
                vpnInterface = established;
            }

            // Cette interface VPN est créée, mais aucun moteur de transport
            // SSH/Xray/V2Ray n'est encore branché dessus.
            reportStatus("disconnected",
                    "Interface créée, moteur VPN à intégrer");

        } catch (Exception e) {
            if (established != null) {
                try {
                    established.close();
                } catch (IOException ignored) {
                }
            }

            reportStatus("disconnected", "Échec de préparation du tunnel");
            stopSelf();
        }
    }

    private synchronized void stopVpn() {
        stopping = true;

        if (worker != null) {
            worker.interrupt();
            worker = null;
        }

        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException ignored) {
            }
            vpnInterface = null;
        }

        stopForeground(true);
    }

    private Notification buildNotification(String message) {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23
                        ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("KOSHIBAR TUN")
                    .setContentText(message)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        }

        return new Notification.Builder(this)
                .setContentTitle("KOSHIBAR TUN")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "KOSHIBAR VPN",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void reportStatus(String status, String message) {
        Intent intent = new Intent("com.koshibar.tun.VPN_STATUS");
        intent.setPackage(getPackageName());
        intent.putExtra("status", status);
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        stopVpn();
        super.onDestroy();
    }
}
