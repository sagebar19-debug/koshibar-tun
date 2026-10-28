package com.koshibar.tun;

import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.IOException;

public class KoshibarVpnService extends VpnService {
    private static final String TAG = "KoshibarVpnService";
    
    public static final String ACTION_CONNECT = "com.koshibar.tun.START_VPN";
    public static final String ACTION_DISCONNECT = "com.koshibar.tun.STOP_VPN";

    private ParcelFileDescriptor vpnInterface = null;
    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_CONNECT.equals(action)) {
                startVpnTunnel();
            } else if (ACTION_DISCONNECT.equals(action)) {
                stopVpnTunnel();
            }
        }
        return START_STICKY;
    }

    private void startVpnTunnel() {
        if (isRunning) {
            Log.d(TAG, "Le VPN est déjà en cours d'exécution.");
            return;
        }

        try {
            // Configuration de l'interface TUN d'Android
            VpnService.Builder builder = new VpnService.Builder();
            builder.setSession("Koshibar TUN");
            
            // Adresse IP virtuelle du tunnel
            builder.addAddress("10.0.0.2", 24);
            
            // Routage de tout le trafic Internet vers le tunnel
            builder.addRoute("0.0.0.0", 0);
            
            // Serveurs DNS sécurisés
            builder.addDnsServer("1.1.1.1");
            builder.addDnsServer("8.8.8.8");

            // Création de l'interface réseau virtuelle tun0
            vpnInterface = builder.establish();

            if (vpnInterface != null) {
                isRunning = true;
                Log.d(TAG, "Interface VPN tun0 établie avec succès !");
                
                // Note technique : C'est ici (via vpnInterface.getFd()) que les paquets 
                // réseau interceptés par Android seront lus et redirigés vers ton moteur 
                // de tunneling (Xray, tun2socks ou SSH) dans une future mise à jour.
                
            } else {
                Log.e(TAG, "Échec : La permission VPN a été refusée ou annulée.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Erreur critique lors du démarrage du tunnel : " + e.getMessage(), e);
            stopVpnTunnel();
        }
    }

    private void stopVpnTunnel() {
        try {
            isRunning = false;
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
                Log.d(TAG, "Interface VPN fermée et nettoyée avec succès.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la fermeture de l'interface VPN : " + e.getMessage());
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpnTunnel();
    }
}
