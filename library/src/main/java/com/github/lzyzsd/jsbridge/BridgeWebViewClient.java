package com.github.lzyzsd.jsbridge;

import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by bruce on 10/28/15.
 */
public class BridgeWebViewClient extends WebViewClient implements IBridgeJsClient {
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

        if (webView.getStartupMessage() != null) {
            for (Message m : webView.getStartupMessage()) {
                webView.dispatchMessage(m);
            }
            webView.setStartupMessage(null);
        }
    }

    @Override
    public void onJsCall(int protocol, String jsonParam, String jsCallback) {
        webView.onJsCall(protocol, jsonParam, jsCallback);
    }

    @Override
    public String onJsFetch(int protocol, String jsonParam) {
        return webView.onJsFetch(protocol, jsonParam);
    }

    @Override
    public void onJsResponse(String responseId, String responseData) {
        webView.onJsResponse(responseId, responseData);
    }
}