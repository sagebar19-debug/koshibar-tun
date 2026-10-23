
package com.koshibar.tun;

import android.content.Intent;
import android.net.VpnService;

import org.json.JSONObject;

public class KoshibarVpnService extends VpnService {

    public static final String ACTION_CONNECT =
            "com.koshibar.tun.CONNECT";

    public static final String ACTION_DISCONNECT =
            "com.koshibar.tun.DISCONNECT";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_DISCONNECT.equals(action)) {
            reportStatus("disconnected", "VPN arrêté");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        if (ACTION_CONNECT.equals(action)) {
            String config = intent.getStringExtra("config");

            try {
                if (config == null) {
                    throw new Exception("Configuration absente");
                }

                new JSONObject(config);

                // Aucun tunnel n'est ouvert sans moteur réseau.
                reportStatus(
                    "disconnected",
                    "Moteur Xray non installé"
                );

            } catch (Exception e) {
                reportStatus(
                    "disconnected",
                    "Configuration VPN invalide"
                );
            }

            stopSelf(startId);
        }

        return START_NOT_STICKY;
    }

    private void reportStatus(String status, String message) {
        Intent result = new Intent(
            "com.koshibar.tun.VPN_STATUS"
        );

        result.setPackage(getPackageName());
        result.putExtra("status", status);
        result.putExtra("message", message);

        sendBroadcast(result);
    }
}
