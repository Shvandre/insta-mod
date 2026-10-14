package com.insta.mod;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ModSettingsActivity extends Activity {

    private SharedPreferences prefs;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("instamod", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Insta Mod");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Версия мода: " + ModUpdater.getLocalVersion(this));
        sub.setTextSize(13);
        sub.setTextColor(Color.GRAY);
        sub.setPadding(0, dp(4), 0, dp(16));
        root.addView(sub);

        addSwitch(root,
                "Не отмечать сториз просмотренными",
                "Автор сториз не узнает, что вы её смотрели",
                "no_story_seen", false);
        addSwitch(root,
                "Отключить детект скриншотов",
                "Не отправлять уведомления о скриншотах в Direct и сториз",
                "no_screenshot_detect", false);
        addSwitch(root,
                "Блокировать рекламу",
                "Убирает рекламу из ленты, сториз и reels",
                "no_ads", true);
        addSwitch(root,
                "Блокировать аналитику",
                "Не отправлять события сбора статистики (logging_client_events)",
                "no_analytics", false);

        TextView note = new TextView(this);
        note.setText("Изменения применяются на лету. При проблемах — перезапустите Insta.");
        note.setTextSize(12);
        note.setTextColor(Color.GRAY);
        note.setPadding(0, dp(24), 0, 0);
        root.addView(note);

        Button updateBtn = new Button(this);
        updateBtn.setText("Проверить обновления");
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdates(true);
            }
        });
        root.addView(updateBtn);

        setContentView(scroll);

        checkUpdates(false);
    }

    private void checkUpdates(final boolean manual) {
        ModUpdater.check(this, new ModUpdater.CheckCallback() {
            @Override
            public void onUpdate(ModUpdater.Release rel, String localVersion) {
                ModUpdater.showUpdateDialog(ModSettingsActivity.this, rel);
            }

            @Override
            public void onUpToDate(String localVersion) {
                if (manual) {
                    Toast.makeText(ModSettingsActivity.this,
                            "У вас последняя версия (" + localVersion + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (manual) {
                    Toast.makeText(ModSettingsActivity.this,
                            "Не удалось проверить: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void addSwitch(LinearLayout root, String label, String hint, final String key, boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));

        Switch sw = new Switch(this);
        sw.setText(label);
        sw.setTextSize(16);
        sw.setTextColor(Color.BLACK);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean on) {
                prefs.edit().putBoolean(key, on).apply();
            }
        });
        row.addView(sw);

        TextView h = new TextView(this);
        h.setText(hint);
        h.setTextSize(12);
        h.setTextColor(Color.GRAY);
        h.setPadding(0, dp(2), 0, 0);
        row.addView(h);

        root.addView(row);
    }
}
