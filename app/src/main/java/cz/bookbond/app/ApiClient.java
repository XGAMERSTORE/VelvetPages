package cz.bookbond.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    public interface Callback {
        void ok(JSONObject data);
        void fail(String message);
    }

    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler main = new Handler(Looper.getMainLooper());
    private String baseUrl;
    private String token;
    private JSONObject currentUser;

    public ApiClient(Context context) {
        prefs = context.getSharedPreferences("bookbond", Context.MODE_PRIVATE);
        baseUrl = prefs.getString("server", "http://10.0.2.2:8090");
        token = prefs.getString("token", "");
        String rawUser = prefs.getString("user", "");
        try { if (!rawUser.isEmpty()) currentUser = new JSONObject(rawUser); } catch (Exception ignored) {}
    }

    public String server() { return baseUrl; }
    public String token() { return token == null ? "" : token; }
    public boolean loggedIn() { return token != null && !token.isEmpty() && currentUser != null; }
    public JSONObject user() { return currentUser; }
    public String userId() { return currentUser == null ? "" : currentUser.optString("id", ""); }

    public void setServer(String value) {
        String v = value == null ? "" : value.trim();
        while (v.endsWith("/")) v = v.substring(0, v.length() - 1);
        baseUrl = v;
        prefs.edit().putString("server", v).apply();
    }

    public void logout() {
        token = "";
        currentUser = null;
        prefs.edit().remove("token").remove("user").apply();
    }

    private void saveAuth(JSONObject data) throws Exception {
        token = data.getString("token");
        currentUser = data.getJSONObject("record");
        prefs.edit().putString("token", token).putString("user", currentUser.toString()).apply();
    }

    private void saveUser(JSONObject record) {
        currentUser = record;
        prefs.edit().putString("user", record.toString()).apply();
    }

    private void async(Task task, Callback cb) {
        executor.execute(() -> {
            try {
                JSONObject out = task.run();
                main.post(() -> cb.ok(out));
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null || msg.trim().isEmpty()) msg = e.getClass().getSimpleName();
                final String f = msg;
                main.post(() -> cb.fail(f));
            }
        });
    }

    private interface Task { JSONObject run() throws Exception; }

    private JSONObject request(String method, String path, JSONObject body) throws Exception {
        if (baseUrl == null || baseUrl.trim().isEmpty()) throw new Exception("Nejdřív nastav adresu serveru.");
        URL url = new URL(baseUrl + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Accept", "application/json");
        if (!token().isEmpty()) c.setRequestProperty("Authorization", token());
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
        }
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (in != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
        }
        c.disconnect();
        if (code < 200 || code >= 300) {
            String detail = sb.toString();
            try {
                JSONObject err = new JSONObject(detail);
                String m = err.optString("message", "HTTP " + code);
                if (err.has("data")) m += " • " + err.optJSONObject("data");
                throw new Exception(m);
            } catch (org.json.JSONException ignored) {
                throw new Exception("Server odpověděl " + code + (detail.isEmpty() ? "" : ": " + detail));
            }
        }
        if (sb.length() == 0) return new JSONObject();
        return new JSONObject(sb.toString());
    }

    private static String q(String value) {
        try { return URLEncoder.encode(value, "UTF-8"); }
        catch (Exception ignored) { return value; }
    }

    public void ping(Callback cb) {
        async(() -> request("GET", "/api/health", null), cb);
    }

    public void login(String email, String password, Callback cb) {
        async(() -> {
            JSONObject b = new JSONObject().put("identity", email.trim()).put("password", password);
            JSONObject out = request("POST", "/api/collections/users/auth-with-password", b);
            saveAuth(out);
            return out;
        }, cb);
    }

    public void register(String email, String password, String name, int age, String city, String genres, String books, Callback cb) {
        async(() -> {
            JSONObject b = new JSONObject()
                    .put("email", email.trim())
                    .put("password", password)
                    .put("passwordConfirm", password)
                    .put("displayName", name.trim())
                    .put("age", age)
                    .put("city", city.trim())
                    .put("genres", genres.trim())
                    .put("books", books.trim())
                    .put("bio", "Hledám někoho, s kým se dá mluvit o knihách.")
                    .put("avatar", "📚");
            request("POST", "/api/collections/users/records", b);
            JSONObject login = request("POST", "/api/collections/users/auth-with-password",
                    new JSONObject().put("identity", email.trim()).put("password", password));
            saveAuth(login);
            return login;
        }, cb);
    }

    public void users(Callback cb) {
        String filter = "id != '" + userId() + "'";
        async(() -> request("GET", "/api/collections/users/records?perPage=100&sort=-updated&filter=" + q(filter), null), cb);
    }

    public void likes(Callback cb) {
        String me = userId();
        String filter = "fromUser = '" + me + "' || toUser = '" + me + "'";
        async(() -> request("GET", "/api/collections/likes/records?perPage=200&sort=-created&filter=" + q(filter), null), cb);
    }

    public void like(String toId, Callback cb) {
        JSONObject b = new JSONObject();
        try { b.put("fromUser", userId()).put("toUser", toId); } catch (Exception ignored) {}
        async(() -> request("POST", "/api/collections/likes/records", b), cb);
    }

    public void messages(String peerId, Callback cb) {
        String me = userId();
        String filter = "(sender='" + me + "' && receiver='" + peerId + "') || (sender='" + peerId + "' && receiver='" + me + "')";
        async(() -> request("GET", "/api/collections/messages/records?perPage=200&sort=created&filter=" + q(filter), null), cb);
    }

    public void sendMessage(String peerId, String body, Callback cb) {
        JSONObject b = new JSONObject();
        try { b.put("sender", userId()).put("receiver", peerId).put("body", body.trim()); } catch (Exception ignored) {}
        async(() -> request("POST", "/api/collections/messages/records", b), cb);
    }

    public void posts(Callback cb) {
        async(() -> request("GET", "/api/collections/posts/records?perPage=100&sort=-created&expand=author", null), cb);
    }

    public void post(String kind, String book, String text, Callback cb) {
        JSONObject b = new JSONObject();
        try { b.put("author", userId()).put("kind", kind).put("book", book.trim()).put("text", text.trim()); } catch (Exception ignored) {}
        async(() -> request("POST", "/api/collections/posts/records", b), cb);
    }

    public void updateProfile(String name, int age, String city, String genres, String books, String bio, String avatar, Callback cb) {
        JSONObject b = new JSONObject();
        try {
            b.put("displayName", name.trim()).put("age", age).put("city", city.trim())
                    .put("genres", genres.trim()).put("books", books.trim()).put("bio", bio.trim()).put("avatar", avatar.trim());
        } catch (Exception ignored) {}
        async(() -> {
            JSONObject rec = request("PATCH", "/api/collections/users/records/" + userId(), b);
            saveUser(rec);
            return rec;
        }, cb);
    }
}
