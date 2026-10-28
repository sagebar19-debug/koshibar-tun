public class WebAppInterface {
    Context mContext;

    WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void updateStatus(final String status, final String colorClass) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Exécute du JavaScript dans la WebView pour changer la couleur et le texte
                webView.evaluateJavascript("document.getElementById('state').className = '" + colorClass + "'; document.getElementById('state').innerText = '" + status + "';", null);
            }
        });
    }
}
