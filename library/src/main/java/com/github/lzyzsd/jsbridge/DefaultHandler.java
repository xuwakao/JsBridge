package com.github.lzyzsd.jsbridge;

public class DefaultHandler implements BridgeHandler {

    String TAG = "DefaultHandler";

    @Override
    public String handler(String data, CallBackFunction function) {
        if (function != null) {
            function.onCallBack("DefaultHandler response data");
        }
        return "Default handler return";
    }

}
