package com.github.lzyzsd.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge, IBridgeJsClient {

    private final String TAG = "BridgeWebView";

    public static final String toLoadJs = "WebViewJavascriptBridge.js";
    Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();
    Map<Integer, BridgeFetchProcessor> fetchProcessor = new HashMap<Integer, BridgeFetchProcessor>();
    Map<Integer, BridgeCallProcessor> callProcessor = new HashMap<Integer, BridgeCallProcessor>();

    private List<Message> startupMessage = new ArrayList<Message>();

    public List<Message> getStartupMessage() {
        return startupMessage;
    }

    public void setStartupMessage(List<Message> startupMessage) {
        this.startupMessage = startupMessage;
    }

    private long uniqueId = 0;

    /**
     * UI Handler
     */
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BridgeWebView(Context context) {
        super(context);
        init();
    }

    private void init() {
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        this.setWebViewClient(generateBridgeWebViewClient());
    }

    protected BridgeWebViewClient generateBridgeWebViewClient() {
        return new BridgeWebViewClient(this);
    }

    /**
     * register processor for js to fetch native data synchronized
     *
     * @param protocol
     * @param processor
     */
    public void registerFetchProcessor(int protocol, BridgeFetchProcessor processor) {
        fetchProcessor.put(protocol, processor);
    }

    /**
     * register processor for js to call native function asynchronized
     *
     * @param protocol
     * @param processor
     */
    public void registerCallProcessor(int protocol, BridgeCallProcessor processor) {
        callProcessor.put(protocol, processor);
    }

    @Override
    public void send(String data) {
        send(data, null);
    }

    @Override
    public void send(String data, CallBackFunction responseCallback) {
        doSend(null, data, responseCallback);
    }

    /**
     * call javascript registered handler
     *
     * @param handlerName
     * @param data
     * @param callBack
     */
    @Override
    public void callJsHandler(String handlerName, String data, CallBackFunction callBack) {
        doSend(handlerName, data, callBack);
    }

    private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
        JsRequest m = new JsRequest();
        if (!TextUtils.isEmpty(data)) {
            m.data = data;
        }
        if (responseCallback != null) {
            String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            responseCallbacks.put(callbackStr, responseCallback);
            m.callbackId = callbackStr;
        }
        if (!TextUtils.isEmpty(handlerName)) {
            m.handlerName = handlerName;
        }
        queueMessage(m);
    }

    private void queueMessage(Message m) {
        if (startupMessage != null) {
            startupMessage.add(m);
        } else {
            dispatchMessage(m);
        }
    }

    void dispatchMessage(Message m) {
        String javascriptCommand = null;
        String messageJson = null;
        if (m.data != null) {
            messageJson = m.data.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
            messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        }
        if (m instanceof JsResponse) {
            javascriptCommand = String.format(BridgeUtil.JS_RESPONSE, ((JsResponse) m).jsCallbackId, messageJson);
        } else if (m instanceof JsRequest) {
            javascriptCommand = String.format(BridgeUtil.JS_REQUEST, ((JsRequest) m).handlerName, messageJson, ((JsRequest) m).callbackId);

        }

        final String finalJavascriptCommand = javascriptCommand;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (finalJavascriptCommand != null)
                    loadUrl(finalJavascriptCommand);
            }
        });
    }

    public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
        this.loadUrl(jsUrl);
        responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }

    @Override
    public String onJsFetch(int protocol, String jsonParam) {
        BridgeFetchProcessor processor = fetchProcessor.get(protocol);
        if (processor != null) {
            return processor.process(jsonParam);
        }
        return null;
    }

    /**
     * recv protocol
     *
     * @param protocol
     * @param jsonParam
     * @param jsCallbackId
     * @return
     */
    @Override
    public void onJsCall(int protocol, String jsonParam, final String jsCallbackId) {
        CallBackFunction responseFunction = null;
        // if had callbackId
        if (!TextUtils.isEmpty(jsCallbackId)) {
            responseFunction = new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                    JsResponse responseMsg = new JsResponse();
                    responseMsg.jsCallbackId = jsCallbackId;
                    responseMsg.data = data;
                    queueMessage(responseMsg);
                }
            };
        } else {
            responseFunction = new CallBackFunction() {
                @Override
                public void onCallBack(String data) {
                    // do nothing
                }
            };
        }
        BridgeCallProcessor processor;
        processor = callProcessor.get(protocol);
        if (processor != null) {
            processor.process(jsonParam, responseFunction);
        }
    }

    @Override
    public void onJsResponse(String responseId, String responseData) {
        CallBackFunction function = responseCallbacks.get(responseId);
        function.onCallBack(responseData);
        responseCallbacks.remove(responseId);
    }
}
