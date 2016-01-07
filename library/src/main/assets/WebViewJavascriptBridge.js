//notation: js file can only use this kind of comments
//since comments will cause error when use in webview.loadurl,
//comments will be remove by java use regexp
(function () {
    if (window.WebViewJavascriptBridge) {
        return;
    }

    var receiveMessageQueue = [];
    var messageHandlers = {};

    var responseCallbacks = {};
    var uniqueId = 1;

    function isAndroid() {
        var ua = navigator.userAgent.toLowerCase();
        var isA = ua.indexOf("android") > -1;
        if (isA) {
            return true;
        }
        return false;
    }

    function isIphone() {
        var ua = navigator.userAgent.toLowerCase();
        var isIph = ua.indexOf("iphone") > -1;
        if (isIph) {
            return true;
        }
        return false;
    }

    //set default messageHandler
    function init(messageHandler) {
        if (WebViewJavascriptBridge._messageHandler) {
            throw new Error('WebViewJavascriptBridge.init called twice');
        }
        WebViewJavascriptBridge._messageHandler = messageHandler;
        var receivedMessages = receiveMessageQueue;
        receiveMessageQueue = null;
        for (var i = 0; i < receivedMessages.length; i++) {
            onNativeRequest(receivedMessages[i].handlerName, receivedMessages[i].reqParam, receivedMessages[i].callbackId);
        }
    }

    function registerHandler(handlerName, handler) {
        messageHandlers[handlerName] = handler;
    }

    /////////////////////////////////////////////call native////////////////////////////////

    //asynchronized method used to call native
    function callNativeHandler(protocol, param, responseCallback) {
        var callbackId;
        if (responseCallback) {
            callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
        }
        var paramJson = JSON.stringify(param);
        window.jsBridgeObj.jsCall(protocol, paramJson, callbackId);
    }

    //synchronized method used to fetch data from native`
    function fetchNativeData(protocol, param) {
        if (isAndroid()) {
            var messageQueueString = JSON.stringify(param);
            var response = window.jsBridgeObj.jsFetchNative(protocol, messageQueueString);
            if (typeof console != 'undefined') {
                console.log("sync fetch data from native ret = " + response);
            }
            return response;
        }
    }

    //send response data to native
    function responseToNative(callbackId, responseData) {
        window.jsBridgeObj.jsResponse(callbackId, responseData);
    }

    /////////////////////////////////////////////call native (---END---)////////////////////////////////

    ///////////////////////////////////recv native request/response////////////////////////////
    function onNativeResponse(responseId, responseData) {
        if (typeof console != 'undefined') {
            console.log("onNativeResponse responseId = " + responseId + ", responseData = " + responseData);
        }
        setTimeout(function () {
            var responseCallback = responseCallbacks[responseId];
            if (!responseCallback) {
                return;
            }
            responseCallback(responseData);
            delete responseCallbacks[responseId];
        });
    }

    function onNativeRequest(handlerName, reqParam, callbackId) {
        if (receiveMessageQueue) {
            receiveMessageQueue.push({
                handlerName: handlerName, reqParam: reqParam, callbackId: callbackId
            })
        } else {
            setTimeout(function () {
                //直接发送
                var responseCallback;
                if (callbackId) {
                    var callbackResponseId = callbackId;
                    responseCallback = function (responseData) {
                        responseToNative(callbackResponseId, responseData);
                    };
                }

                var handler = WebViewJavascriptBridge._messageHandler;
                if (handlerName) {
                    handler = messageHandlers[handlerName];
                }
                //查找指定handler
                try {
                    handler(reqParam, responseCallback);
                } catch (exception) {
                    if (typeof console != 'undefined') {
                        console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                    }
                }
            });
        }
    }

    ///////////////////////////////////recv native request/response (---END---)////////////////////////////

    var WebViewJavascriptBridge = window.WebViewJavascriptBridge = {
        init: init,
        registerHandler: registerHandler,
        callHandler: callNativeHandler,
        fetchNativeData: fetchNativeData,
        onNativeResponse: onNativeResponse,
        onNativeRequest: onNativeRequest
    };

    var doc = document;
    var readyEvent = doc.createEvent('Events');
    readyEvent.initEvent('WebViewJavascriptBridgeReady');
    readyEvent.bridge = WebViewJavascriptBridge;
    doc.dispatchEvent(readyEvent);
})();