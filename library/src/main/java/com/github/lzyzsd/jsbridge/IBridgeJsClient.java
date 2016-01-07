package com.github.lzyzsd.jsbridge;

/**
 * Created by hlx-wc on 2016/1/5.
 */
public interface IBridgeJsClient {
    void onJsCall(int protocol, String jsonParam, String jsCallback);

    String onJsFetch(int protocol, String jsonParam);

    void onJsResponse(String responseId, String responseData);
}
