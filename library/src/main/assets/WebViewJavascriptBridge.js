//notation: js file can only use this kind of comments
//since comments will cause error when use in webview.loadurl,
//comments will be remove by java use regexp
(function () {
    if (window.WebViewJavascriptBridge) {
        return;
    }

    var receiveMessageQueue = [];
    var messageHandlers = {};

    var CUSTOM_PROTOCOL_SCHEME = 'yy';
    var QUEUE_HAS_MESSAGE = '__QUEUE_MESSAGE__/';

    var responseCallbacks = {};
    var uniqueId = 1;

    var base64encodechars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    function base64encode(str) {
        if (str === undefined) {
            return str;
        }

        var out, i, len;
        var c1, c2, c3;
        len = str.length;
        i = 0;
        out = "";
        while (i < len) {
            c1 = str.charCodeAt(i++) & 0xff;
            if (i == len) {
                out += base64encodechars.charAt(c1 >> 2);
                out += base64encodechars.charAt((c1 & 0x3) << 4);
                out += "==";
                break;
            }
            c2 = str.charCodeAt(i++);
            if (i == len) {
                out += base64encodechars.charAt(c1 >> 2);
                out += base64encodechars.charAt(((c1 & 0x3) << 4) | ((c2 & 0xf0) >> 4));
                out += base64encodechars.charAt((c2 & 0xf) << 2);
                out += "=";
                break;
            }
            c3 = str.charCodeAt(i++);
            out += base64encodechars.charAt(c1 >> 2);
            out += base64encodechars.charAt(((c1 & 0x3) << 4) | ((c2 & 0xf0) >> 4));
            out += base64encodechars.charAt(((c2 & 0xf) << 2) | ((c3 & 0xc0) >> 6));
            out += base64encodechars.charAt(c3 & 0x3f);
        }
        return out;
    }

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

    //sendMessage add message, 触发native处理 sendMessage
    function _doSend(protocol, param, responseCallback) {
        if (responseCallback) {
            var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
            responseCallbacks[callbackId] = responseCallback;
        }

        if (isIphone()) {
            //TODO
        } else if (isAndroid()) {
            var messageQueueString = JSON.stringify(param);
            var retJson = window.jsBridgeObj.jsCall(messageQueueString);
            if (typeof console != 'undefined') {
                console.log("send sync call ret = " + retJson);
            }
            //if (retJson)
            //    _dispatchMessageFromNative(retJson);
        }
    }

    //提供给native使用,
    function _dispatchMessageFromNative(messageJSON) {
        setTimeout(function () {
            var message = JSON.parse(messageJSON);
            var responseCallback;
            //java call finished, now need to call js callback function
            if (message.responseId) {
                responseCallback = responseCallbacks[message.responseId];
                if (!responseCallback) {
                    return;
                }
                responseCallback(message.responseData);
                delete responseCallbacks[message.responseId];
            } else {
                //直接发送
                if (message.callbackId) {
                    var callbackResponseId = message.callbackId;
                    responseCallback = function (responseData) {
                        _doSend({
                            responseId: callbackResponseId,
                            responseData: responseData
                        });
                    };
                }

                var handler = WebViewJavascriptBridge._messageHandler;
                if (message.handlerName) {
                    handler = messageHandlers[message.handlerName];
                }
                //查找指定handler
                try {
                    handler(message.data, responseCallback);
                } catch (exception) {
                    if (typeof console != 'undefined') {
                        console.log("WebViewJavascriptBridge: WARNING: javascript handler threw.", message, exception);
                    }
                }
            }
        });
    }

    //提供给native调用,receiveMessageQueue 在会在页面加载完后赋值为null,所以
    function _handleMessageFromNative(messageJSON) {
        console.log(messageJSON);
        //check whether initialization has finished
        if (receiveMessageQueue) {
            receiveMessageQueue.push(messageJSON);
        } else {
            _dispatchMessageFromNative(messageJSON);
        }
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
        _handleMessageFromNative: _handleMessageFromNative,
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