package com.github.lzyzsd.jsbridge;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * Created by hlx-wc on 2016/1/5.
 */
public class JsNativeAdapter {
    private IBridgeJsClient mClient = null;

    public JsNativeAdapter(IBridgeJsClient client) {
        mClient = client;
    }

    @JavascriptInterface
    public void jsCall(int protocol, String jsonParam, String jsCallback) {
        Log.d("", "call from js protocol = " + protocol + ", jsonparam = " + jsonParam + ", jsCallback = " + jsCallback);
        if (mClient != null) {
            mClient.onJsCall(protocol, jsonParam, jsCallback);
        }
    }

    @JavascriptInterface
    public String jsFetchNative(int protocol, String jsonParam) {
        Log.d("", "js fetch native protocol = " + protocol + ", jsonParam = " + jsonParam);
        if (mClient != null) {
            return mClient.onJsFetch(protocol, jsonParam);
        }
        return null;
    }

    @JavascriptInterface
    public void jsResponse(String callbackId, String data) {
        Log.d("", "js fetch native callbackId = " + callbackId + ", data = " + data);
        if (mClient != null) {
            mClient.onJsResponse(callbackId, data);
        }
    }
}
