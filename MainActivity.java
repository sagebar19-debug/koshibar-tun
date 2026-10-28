package com.koshibar.tun;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private final ActivityResultLauncher<Intent> vpnPermissionLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = new Intent(this, KoshibarVpnService.java);
                intent.setAction(KoshibarVpnService.ACTION_CONNECT);
                startService(intent);
            }
        }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");
        
        // Charger ton fichier HTML depuis les assets
        webView.loadUrl("file:///android_asset/KOSHIBAR_TUN.html");
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void startVpn() {
            Intent intent = VpnService.prepare(MainActivity.this);
            if (intent != null) {
                vpnPermissionLauncher.launch(intent);
            } else {
                Intent serviceIntent = new Intent(MainActivity.this, KoshibarVpnService.java);
                serviceIntent.setAction(KoshibarVpnService.ACTION_CONNECT);
                startService(serviceIntent);
            }
        }

        @JavascriptInterface
        public void stopVpn() {
            Intent intent = new Intent(MainActivity.this, KoshibarVpnService.java);
            intent.setAction(KoshibarVpnService.ACTION_DISCONNECT);
            startService(intent);
        }
    }
}
