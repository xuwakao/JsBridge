package com.github.lzyzsd.jsbridge;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * Created by hlx-wc on 2016/1/5.
 */
public class JsNativeAdapter {
    private IBridgeClient mClient = null;

    public JsNativeAdapter(IBridgeClient client) {
        mClient = client;
    }

    @JavascriptInterface
    public String callFromJs(String msg) {
        Log.d("", "call from js = " + msg);
        if (mClient != null) {
            return mClient.onRecvMsgFromJs(msg);
        }
        return null;
    }
}
