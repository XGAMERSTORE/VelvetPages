package com.wildbond.velvetcompanion.v03;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private SharedPreferences prefs;
    private TextToSpeech tts;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(Color.rgb(7, 6, 11));
        getWindow().setNavigationBarColor(Color.rgb(7, 6, 11));

        prefs = getSharedPreferences("velvet_v03", MODE_PRIVATE);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("cs", "CZ"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
                tts.setSpeechRate(0.96f);
                tts.setPitch(1.04f);
            }
        });

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(7, 6, 11));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new NativeBridge(), "Native");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    public class NativeBridge {
        @JavascriptInterface
        public String getConfig() {
            try {
                JSONObject o = new JSONObject();
                o.put("apiKey", prefs.getString("api_key", ""));
                o.put("model", prefs.getString("model", "gpt-6-luna"));
                o.put("voice", prefs.getBoolean("voice", true));
                return o.toString();
            } catch (Exception e) {
                return "{}";
            }
        }

        @JavascriptInterface
        public void saveConfig(String apiKey, String model, boolean voice) {
            prefs.edit()
                    .putString("api_key", apiKey == null ? "" : apiKey.trim())
                    .putString("model", model == null || model.trim().isEmpty() ? "gpt-6-luna" : model.trim())
                    .putBoolean("voice", voice)
                    .apply();
        }

        @JavascriptInterface
        public void speak(String text) {
            if (!prefs.getBoolean("voice", true)) return;
            if (tts != null && text != null && !text.trim().isEmpty()) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "velvet");
            }
        }

        @JavascriptInterface
        public void stopSpeaking() {
            if (tts != null) tts.stop();
        }

        @JavascriptInterface
        public void sendAI(String userMessage, boolean adultMode, String persona, String memoryJson) {
            final String key = prefs.getString("api_key", "").trim();
            final String model = prefs.getString("model", "gpt-6-luna").trim();

            if (key.isEmpty()) {
                postAiResult(errorPayload("need_key", "Není nastavený API klíč. Otevři AI a ulož ho."));
                return;
            }

            new Thread(() -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("model", model.isEmpty() ? "gpt-6-luna" : model);
                    body.put("input", buildPrompt(userMessage, adultMode, persona, memoryJson));
                    body.put("max_output_tokens", 260);

                    URL url = new URL("https://api.openai.com/v1/responses");
                    HttpURLConnection c = (HttpURLConnection) url.openConnection();
                    c.setRequestMethod("POST");
                    c.setConnectTimeout(20000);
                    c.setReadTimeout(50000);
                    c.setDoOutput(true);
                    c.setRequestProperty("Authorization", "Bearer " + key);
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    c.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream os = c.getOutputStream()) {
                        os.write(bytes);
                    }

                    int code = c.getResponseCode();
                    InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
                    String raw = readAll(stream);

                    if (code < 200 || code >= 300) {
                        String msg = "AI chyba " + code;
                        try {
                            JSONObject er = new JSONObject(raw);
                            JSONObject eo = er.optJSONObject("error");
                            if (eo != null && !eo.optString("message").isEmpty()) msg += ": " + eo.optString("message");
                        } catch (Exception ignored) { }
                        postAiResult(errorPayload("api_error", msg));
                        return;
                    }

                    JSONObject response = new JSONObject(raw);
                    String text = extractOutputText(response);
                    if (text == null || text.trim().isEmpty()) {
                        postAiResult(errorPayload("empty", "AI vrátila prázdnou odpověď."));
                        return;
                    }

                    JSONObject result = parseModelJson(text);
                    result.put("ok", true);
                    postAiResult(result);
                } catch (Exception e) {
                    postAiResult(errorPayload("network", "AI spojení selhalo: " + e.getMessage()));
                }
            }).start();
        }
    }

    private String buildPrompt(String userMessage, boolean adultMode, String persona, String memoryJson) {
        String mode = adultMode
                ? "ADULT MODE is enabled. The user has stated they are an adult. You may be romantic, flirtatious and discuss mature relationship or sexuality topics when the user initiates them, while still following the provider's safety rules. Keep every character adult and every intimate interaction consensual."
                : "ADULT MODE is disabled. Keep the tone affectionate, playful and non-explicit.";

        String p = persona == null || persona.trim().isEmpty() ? "gothic playful" : persona.trim();
        String memory = memoryJson == null ? "[]" : memoryJson;
        String user = userMessage == null ? "" : userMessage;

        return "You are Velvet, a fictional adult 3D AI companion in an Android app. " +
                "Speak Czech unless the user uses another language. Personality preset: " + p + ". " + mode + " " +
                "Be natural, warm and concise. You can choose exactly one body action that fits your reply. " +
                "Valid action values: idle, wave, turn, closer, back, sit, stand, smile, dance, jump, pose, look, nod, shake, cross_arms, hands_hips, heart, blow_kiss. " +
                "Valid emotion values: neutral, happy, playful, shy, romantic, sad, annoyed, surprised. " +
                "Return ONLY valid JSON and no markdown, exactly like: {\"reply\":\"text\",\"action\":\"smile\",\"emotion\":\"happy\"}. " +
                "Recent conversation memory: " + memory + "\nUser: " + user;
    }

    private JSONObject parseModelJson(String text) {
        try {
            String cleaned = text.trim();
            if (cleaned.startsWith("```")) {
                int firstNl = cleaned.indexOf('\n');
                int last = cleaned.lastIndexOf("```");
                if (firstNl >= 0 && last > firstNl) cleaned = cleaned.substring(firstNl + 1, last).trim();
            }
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) cleaned = cleaned.substring(start, end + 1);
            JSONObject o = new JSONObject(cleaned);
            if (o.optString("reply").isEmpty()) o.put("reply", text.trim());
            if (o.optString("action").isEmpty()) o.put("action", "idle");
            if (o.optString("emotion").isEmpty()) o.put("emotion", "neutral");
            return o;
        } catch (Exception e) {
            try {
                JSONObject o = new JSONObject();
                o.put("reply", text.trim());
                o.put("action", "idle");
                o.put("emotion", "neutral");
                return o;
            } catch (Exception impossible) {
                return new JSONObject();
            }
        }
    }

    private String extractOutputText(JSONObject response) {
        String helper = response.optString("output_text", "");
        if (!helper.isEmpty()) return helper;
        JSONArray output = response.optJSONArray("output");
        if (output == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            if (item == null) continue;
            JSONArray content = item.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject part = content.optJSONObject(j);
                if (part == null) continue;
                String type = part.optString("type", "");
                if ("output_text".equals(type) || part.has("text")) {
                    String t = part.optString("text", "");
                    if (!t.isEmpty()) {
                        if (sb.length() > 0) sb.append('\n');
                        sb.append(t);
                    }
                }
            }
        }
        return sb.toString();
    }

    private JSONObject errorPayload(String type, String message) {
        try {
            JSONObject o = new JSONObject();
            o.put("ok", false);
            o.put("type", type);
            o.put("message", message);
            return o;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private void postAiResult(JSONObject result) {
        final String payload = result == null ? "{}" : result.toString();
        runOnUiThread(() -> {
            if (webView == null) return;
            String js = "window.__velvetAiResult(" + JSONObject.quote(payload) + ");";
            webView.evaluateJavascript(js, null);
        });
    }
}
