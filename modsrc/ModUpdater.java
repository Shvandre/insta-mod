package com.insta.mod;

import android.app.PendingIntent;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ModUpdater {

    public static final String REPO = "Shvandre/insta-mod";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";

    public static class Release {
        public String tag;
        public String apkUrl;
        public String notes;
    }

    public interface CheckCallback {
        void onUpdate(Release rel, String localVersion);
        void onUpToDate(String localVersion);
        void onError(String message);
    }

    public static String getLocalVersion(Context ctx) {
        try {
            InputStream in = ctx.getAssets().open("instamod/version");
            BufferedReader r = new BufferedReader(new InputStreamReader(in));
            String v = r.readLine();
            r.close();
            return v == null ? "unknown" : v.trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public static void check(final Context ctx, final CheckCallback cb) {
        final Handler ui = new Handler(Looper.getMainLooper());
        final Context app = ctx.getApplicationContext();
        final String local = getLocalVersion(app);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(API).openConnection();
                    conn.setRequestProperty("User-Agent", "insta-mod-updater");
                    conn.setRequestProperty("Accept", "application/vnd.github+json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        throw new Exception("GitHub API: HTTP " + code);
                    }
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();

                    JSONObject json = new JSONObject(sb.toString());
                    final Release rel = new Release();
                    rel.tag = json.getString("tag_name");
                    rel.notes = json.optString("body", "");
                    JSONArray assets = json.getJSONArray("assets");
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject a = assets.getJSONObject(i);
                        if (a.getString("name").endsWith(".apk")) {
                            rel.apkUrl = a.getString("browser_download_url");
                            break;
                        }
                    }
                    if (rel.apkUrl == null) {
                        throw new Exception("В релизе " + rel.tag + " нет APK");
                    }
                    if (rel.tag.equals(local)) {
                        ui.post(new Runnable() {
                            @Override
                            public void run() { cb.onUpToDate(local); }
                        });
                    } else {
                        ui.post(new Runnable() {
                            @Override
                            public void run() { cb.onUpdate(rel, local); }
                        });
                    }
                } catch (final Exception e) {
                    ui.post(new Runnable() {
                        @Override
                        public void run() { cb.onError(e.getMessage()); }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    public static void downloadAndInstall(final Context ctx, final Release rel) {
        final Context app = ctx.getApplicationContext();
        Toast.makeText(app, "Скачиваю " + rel.tag + "…", Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection) new URL(rel.apkUrl).openConnection();
                    conn.setRequestProperty("User-Agent", "insta-mod-updater");
                    conn.setInstanceFollowRedirects(true);
                    conn.connect();
                    if (conn.getResponseCode() != 200) {
                        throw new Exception("HTTP " + conn.getResponseCode());
                    }
                    File apk = new File(app.getCacheDir(), "update.apk");
                    InputStream in = conn.getInputStream();
                    FileOutputStream out = new FileOutputStream(apk);
                    byte[] buf = new byte[65536];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    out.close();
                    in.close();
                    install(app, apk);
                } catch (final Exception e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(app, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private static void install(Context ctx, File apk) throws Exception {
        PackageInstaller installer = ctx.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sessionId = installer.createSession(params);
        PackageInstaller.Session session = installer.openSession(sessionId);
        OutputStream out = session.openWrite("base.apk", 0, apk.length());
        InputStream in = new java.io.FileInputStream(apk);
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        session.fsync(out);
        in.close();
        out.close();

        Intent intent = new Intent(ctx, InstallResultReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, sessionId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        IntentSender sender = pi.getIntentSender();
        session.commit(sender);
        session.close();
    }

    public static void showUpdateDialog(final Context ctx, final Release rel) {
        String msg = "Доступна версия " + rel.tag;
        if (rel.notes != null && rel.notes.length() > 0 && rel.notes.length() < 500) {
            msg += "\n\n" + rel.notes;
        }
        new AlertDialog.Builder(ctx)
                .setTitle("Обновление Insta Mod")
                .setMessage(msg)
                .setPositiveButton("Скачать и установить", (d, w) -> downloadAndInstall(ctx, rel))
                .setNegativeButton("Позже", null)
                .show();
    }
}
