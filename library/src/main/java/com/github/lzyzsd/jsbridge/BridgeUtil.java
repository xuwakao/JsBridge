package com.github.lzyzsd.jsbridge;

import android.content.Context;
import android.util.Base64;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BridgeUtil {
    final static String UNDERLINE_STR = "_";
    final static String CALLBACK_ID_FORMAT = "JAVA_CB_%s";

    final static String JS_RESPONSE = "javascript:WebViewJavascriptBridge.onNativeResponse('%s', '%s');";
    final static String JS_REQUEST = "javascript:WebViewJavascriptBridge.onNativeRequest('%s', '%s', '%s');";

    public static String parseFunctionName(String jsUrl) {
        return jsUrl.replace("javascript:WebViewJavascriptBridge.", "").replaceAll("\\(.*\\);", "");
    }

    public static void webViewLoadLocalJs(WebView view, String path) {
        String jsContent = assetFile2Str(view.getContext(), path);
        view.loadUrl("javascript:" + jsContent);
    }

    public static String assetFile2Str(Context c, String urlStr) {
        InputStream in = null;
        try {
            in = c.getAssets().open(urlStr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line = null;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null && !line.matches("^\\s*\\/\\/.*")) {
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
