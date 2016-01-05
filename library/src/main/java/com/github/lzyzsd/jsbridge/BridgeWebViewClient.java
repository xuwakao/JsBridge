package com.github.lzyzsd.jsbridge;

import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by bruce on 10/28/15.
 */
public class BridgeWebViewClient extends WebViewClient implements IBridgeClient {
    private static final String JS_BRIDGE_OBJ = "jsBridgeObj";

    private BridgeWebView webView;

    public BridgeWebViewClient(BridgeWebView webView) {
        this.webView = webView;
        JsNativeAdapter bridge = new JsNativeAdapter(this);
        webView.addJavascriptInterface(bridge, JS_BRIDGE_OBJ);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (BridgeWebView.toLoadJs != null) {
            BridgeUtil.webViewLoadLocalJs(view, BridgeWebView.toLoadJs);
        }

        //
        if (webView.getStartupMessage() != null) {
            for (Message m : webView.getStartupMessage()) {
                webView.dispatchMessage(m);
            }
            webView.setStartupMessage(null);
        }
    }

    @Override
    public String onRecvMsgFromJs(String message) {
        return webView.recvMsgFromJs(message);
    }
}