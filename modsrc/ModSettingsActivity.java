package com.insta.mod;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ModSettingsActivity extends Activity {

    private SharedPreferences prefs;

    // Instagram brand palette
    private static final int COLOR_BG = 0xFF000000;
    private static final int COLOR_CARD = 0xFF121212;
    private static final int COLOR_TITLE = 0xFFF5F5F5;
    private static final int COLOR_HINT = 0xFF9B9B9B;
    private static final int COLOR_SECTION = 0xFF8E8E8E;
    private static final int COLOR_ACCENT = 0xFF0095F6;
    private static final int COLOR_TRACK_OFF = 0xFF333639;
    private static final int[] GRADIENT = {0xFFF58529, 0xFFDD2A7B, 0xFF8134AF, 0xFF515BD4};

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("instamod", MODE_PRIVATE);

        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);
        getWindow().setBackgroundDrawable(new ColorDrawable(COLOR_BG));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(28), pad, dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        // ---- Header ----
        GradientTextView title = new GradientTextView(this);
        title.setText("Insta Mod");
        title.setTextSize(30);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Version " + ModUpdater.getLocalVersion(this));
        sub.setTextSize(13);
        sub.setTextColor(COLOR_HINT);
        sub.setPadding(dp(2), dp(6), 0, 0);
        root.addView(sub);

        // ---- Privacy ----
        addSection(root, "PRIVACY");
        addSwitchCard(root,
                "Hide story views",
                "Story authors won't see that you viewed their story",
                "no_story_seen", false);
        addSwitchCard(root,
                "Disable screenshot detection",
                "Stops screenshot notifications in Direct and stories",
                "no_screenshot_detect", false);

        // ---- Blocking ----
        addSection(root, "BLOCKING");
        addSwitchCard(root,
                "Block ads",
                "Removes ads from feed, stories and reels",
                "no_ads", true);
        addSwitchCard(root,
                "Block analytics",
                "Stops telemetry events (logging_client_events) from being sent",
                "no_analytics", false);

        TextView note = new TextView(this);
        note.setText("Changes apply instantly. Long-press the Insta wordmark in the feed to open this menu.");
        note.setTextSize(12);
        note.setTextColor(COLOR_SECTION);
        note.setLineSpacing(dp(3), 1f);
        note.setPadding(dp(2), dp(20), dp(2), 0);
        root.addView(note);

        // ---- Update button ----
        TextView updateBtn = new TextView(this);
        updateBtn.setText("Check for updates");
        updateBtn.setAllCaps(false);
        updateBtn.setTextSize(15);
        updateBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        updateBtn.setTextColor(Color.WHITE);
        updateBtn.setGravity(Gravity.CENTER);
        updateBtn.setClickable(true);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(COLOR_ACCENT);
        btnBg.setCornerRadius(dp(14));
        RippleDrawable btnRipple = new RippleDrawable(
                ColorStateList.valueOf(0x33FFFFFF), btnBg, btnBg);
        updateBtn.setBackground(btnRipple);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUpdates(true);
            }
        });
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        btnLp.topMargin = dp(20);
        root.addView(updateBtn, btnLp);

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
                            "You're up to date (" + localVersion + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String message) {
                if (manual) {
                    Toast.makeText(ModSettingsActivity.this,
                            "Check failed: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void addSection(LinearLayout root, String name) {
        TextView label = new TextView(this);
        label.setText(name);
        label.setTextSize(11);
        label.setTextColor(COLOR_SECTION);
        label.setLetterSpacing(0.12f);
        label.setPadding(dp(2), dp(22), 0, dp(10));
        root.addView(label);
    }

    private GradientDrawable cardBackground() {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(COLOR_CARD);
        bg.setCornerRadius(dp(16));
        return bg;
    }

    private void addSwitchCard(LinearLayout root, String label, String hint,
                               final String key, boolean def) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(new RippleDrawable(
                ColorStateList.valueOf(0x1FFFFFFF), cardBackground(), cardBackground()));

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(16);
        title.setTextColor(COLOR_TITLE);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textCol.addView(title);

        TextView desc = new TextView(this);
        desc.setText(hint);
        desc.setTextSize(13);
        desc.setTextColor(COLOR_HINT);
        desc.setLineSpacing(dp(2), 1f);
        desc.setPadding(0, dp(3), 0, 0);
        textCol.addView(desc);

        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLp.rightMargin = dp(12);
        card.addView(textCol, textLp);

        final Switch sw = new Switch(this);
        sw.setPadding(0, 0, 0, 0);
        sw.setSwitchPadding(dp(0));
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setTrackDrawable(trackDrawable());
        sw.setThumbDrawable(thumbDrawable());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton btn, boolean on) {
                prefs.edit().putBoolean(key, on).apply();
            }
        });
        card.addView(sw, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw.toggle();
            }
        });

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dp(10);
        card.setPadding(dp(18), dp(16), dp(14), dp(16));
        root.addView(card, cardLp);
    }

    private StateListDrawable trackDrawable() {
        GradientDrawable on = new GradientDrawable();
        on.setColor(COLOR_ACCENT);
        on.setCornerRadius(dp(15));
        on.setSize(dp(46), dp(30));

        GradientDrawable off = new GradientDrawable();
        off.setColor(COLOR_TRACK_OFF);
        off.setCornerRadius(dp(15));
        off.setSize(dp(46), dp(30));

        StateListDrawable track = new StateListDrawable();
        track.addState(new int[]{android.R.attr.state_checked}, on);
        track.addState(new int[0], off);
        return track;
    }

    private android.graphics.drawable.LayerDrawable thumbDrawable() {
        GradientDrawable knob = new GradientDrawable();
        knob.setShape(GradientDrawable.OVAL);
        knob.setColor(Color.WHITE);
        knob.setSize(dp(24), dp(24));

        android.graphics.drawable.LayerDrawable thumb = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{knob});
        thumb.setLayerInset(0, dp(3), dp(3), dp(3), dp(3));
        return thumb;
    }

    // TextView with the Instagram brand gradient painted across its text.
    public static class GradientTextView extends TextView {
        public GradientTextView(android.content.Context ctx) {
            super(ctx);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed, left, top, right, bottom);
            if (changed || getPaint().getShader() == null) {
                getPaint().setShader(new LinearGradient(
                        0, 0, getWidth(), 0,
                        GRADIENT, null, Shader.TileMode.CLAMP));
                invalidate();
            }
        }
    }
}
