package com.insta.mod;

import android.content.Context;
import android.content.SharedPreferences;

// Автопроверка обновлений: не чаще раза в 12 часов.
// Вызывается из WordmarkHook при показе шапки ленты (≈ запуск приложения).
public class AutoUpdate {
    private static final long INTERVAL_MS = 12L * 60 * 60 * 1000;
    private static boolean inFlight = false;

    public static synchronized void maybeCheck(final Context ctx) {
        final SharedPreferences prefs = ctx.getApplicationContext()
                .getSharedPreferences("instamod", Context.MODE_PRIVATE);
        long last = prefs.getLong("last_auto_check", 0);
        if (inFlight || System.currentTimeMillis() - last < INTERVAL_MS) return;
        inFlight = true;
        ModUpdater.check(ctx, new ModUpdater.CheckCallback() {
            @Override
            public void onUpdate(final ModUpdater.Release rel, String localVersion) {
                prefs.edit().putLong("last_auto_check", System.currentTimeMillis()).apply();
                inFlight = false;
                try {
                    ModUpdater.showUpdateDialog(ctx, rel);
                } catch (Exception ignored) {
                    // активность могла умереть, пока шёл запрос
                }
            }

            @Override
            public void onUpToDate(String localVersion) {
                prefs.edit().putLong("last_auto_check", System.currentTimeMillis()).apply();
                inFlight = false;
            }

            @Override
            public void onError(String message) {
                // метку не ставим — повторим при следующем показе шапки
                inFlight = false;
            }
        });
    }
}
