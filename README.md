#JsBridge

-----
##Description

Enhanced version of [JsBridge](https://github.com/lzyzsd/JsBridge).

## Support
This project make a bridge between Java and JavaScript.

++Provides safe and convenient way to call Java code from js and call js code from java.
++Provide sychronized and asychronized method to call from js

## Demo
![JsBridge Demo](https://raw.githubusercontent.com/lzyzsd/JsBridge/master/JsBridge.gif)

## Usage

## Use it in Java

add com.github.lzyzsd.BridgeWebView to your layout, it is inherited from WebView.

### Register a Java processor function so that js can call

```java
            ----------register sychronized processor-------------
            webView.registerCallProcessor(0, new BridgeCallProcessor() {
                @Override
                public void process(String data, CallBackFunction callback) {
                    callback.onCallBack(data);
                }
            });
    
            -----------register asychronized processor-------------
            webView.registerFetchProcessor(0, new BridgeFetchProcessor() {
                @Override
                public String process(String data) {
                    return data;
                }
            });

```

js can call this Java through:

```javascript

        -----------synchronized fetch data from native-------------
        var response = window.WebViewJavascriptBridge.fetchNativeData(0, data);
        
        -----------asynchronized call java handler-------------
        window.WebViewJavascriptBridge.callHandler(0
                , {'param': 'processor测试'}
                , function (responseData) {
                    document.getElementById("show").innerHTML = "send get responseData from java, data = " + responseData;
                }

```

### Register a JavaScript handler function so that Java can call

```javascript
            WebViewJavascriptBridge.registerHandler("functionInJs", function (data, responseCallback) {
                document.getElementById("show").innerHTML = ("data from Java: = " + data);
                var responseData = "Javascript Says Right back aka!";
                responseCallback(responseData);
            });

```

Java can call this js handler function "functionInJs" through:

```java

        webView.callJsHandler("functionInJs", new Gson().toJson(user), new CallBackFunction() {
            @Override
            public void onCallBack(String data) {
                Log.d("", "oncallback data = " + data);
            }
        });

```
You can also define a default handler use init method, so that Java can send message to js without assigned handlerName

for example:

```javascript

    bridge.init(function(message, responseCallback) {
        console.log('JS got a message', message);
        var data = {
            'Javascript Responds': 'Wee!'
        };
        console.log('JS responding with', data);
        responseCallback(data);
    });

```

```java
    webView.send("hello");
```

will print 'JS got a message hello' and 'JS responding with' in webview console.

## Notice

This lib will inject a WebViewJavascriptBridge Object to window object.
So in your js, before use WebViewJavascriptBridge, you must detect if WebViewJavascriptBridge exist.
If WebViewJavascriptBridge does not exit, you can listen to WebViewJavascriptBridgeReady event, as the blow code shows:

```javascript

    if (window.WebViewJavascriptBridge) {
        //do your work here
    } else {
        document.addEventListener(
            'WebViewJavascriptBridgeReady'
            , function() {
                //do your work here
            },
            false
        );
    }

```

## License

This project is licensed under the terms of the MIT license.
