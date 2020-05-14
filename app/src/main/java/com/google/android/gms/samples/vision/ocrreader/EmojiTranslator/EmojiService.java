package com.google.android.gms.samples.vision.ocrreader.EmojiTranslator;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;

public class EmojiService {

    private static final String API_ENDPOINT = "http://dyslexiahelper.azurewebsites.net/api/" +
            "MyFunctions?code=crJe1fR592fKXJTCYfM%2FRFG4t%2F3Wl111kJNArZesadnyx3HlMKKIfw%3D%3D";
    private static final String TRANSLATE = "translate";
    private static final String SPACE = " ";
    private static final String URL_SPACE = "%20";
    private static final String URL_QUERY = "&q=%s";

    private EmojiServiceCallback emojiServiceCallback;

    public EmojiService(EmojiServiceCallback emojiServiceCallback) {
        this.emojiServiceCallback = emojiServiceCallback;
    }

    public void translate(Context context, String text) {
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET,
                API_ENDPOINT + String.format(URL_QUERY, text.replaceAll(SPACE, URL_SPACE)),
                null,
                response -> {
                    try {
                        emojiServiceCallback.onSuccess(response.getString(TRANSLATE));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> emojiServiceCallback.onFailure()
        );

        // add it to the RequestQueue
        queue.add(getRequest);
    }
}
