
package com.koshibar.tun;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private String pendingConfig = "{}";
    private boolean disconnectRequested = false;

    private final ActivityResultLauncher<Intent> vpnPermission =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (disconnectRequested) {
                    disconnectRequested = false;
                    return;
                }

                if (result.getResultCode() == Activity.RESULT_OK) {
                    startVpnService();
                } else {
                    sendStatus("disconnected",
                        "Autorisation VPN refusée");
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
        webView.addJavascriptInterface(
            new KoshibarBridge(), "KoshibarNative"
        );

        webView.loadUrl(
            "file:///android_asset/KOSHIBAR_TUN.html"
        );
    }

    private class KoshibarBridge {

        @JavascriptInterface
        public void connect(String config) {
            runOnUiThread(() -> {
                try {
                    pendingConfig = new JSONObject(config).toString();

                    Intent prepare = VpnService.prepare(MainActivity.this);

                    if (prepare != null) {
                        vpnPermission.launch(prepare);
                    } else {
                        startVpnService();
                    }
                } catch (Exception e) {
                    sendStatus("disconnected",
                        "Configuration invalide");
                }
            });
        }

        @JavascriptInterface
        public void disconnect() {
            runOnUiThread(() -> {
                disconnectRequested = true;

                Intent intent = new Intent();
                intent.setClassName(
                    MainActivity.this,
                    "com.koshibar.tun.KoshibarVpnService"
                );
                intent.setAction("com.koshibar.tun.DISCONNECT");

                try {
                    startService(intent);
                } catch (Exception e) {
                    Toast.makeText(
                        MainActivity.this,
                        "Service VPN pas encore installé",
                        Toast.LENGTH_LONG
                    ).show();
                }
            });
        }
    }

    private void startVpnService() {
        Intent intent = new Intent();
        intent.setClassName(
            this,
            "com.koshibar.tun.KoshibarVpnService"
        );
        intent.setAction("com.koshibar.tun.CONNECT");
        intent.putExtra("config", pendingConfig);

        try {
            startService(intent);
            sendStatus("connecting", "Connexion en cours");
        } catch (Exception e) {
            sendStatus("disconnected",
                "Service VPN indisponible");
        }
    }

    private void sendStatus(String status, String message) {
        if (webView == null) return;

        String js =
            "if(window.vpnStatus){" +
            "window.vpnStatus(" +
            JSONObject.quote(status) + ");" +
            "}";

        webView.evaluateJavascript(js, null);

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("KoshibarNative");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
