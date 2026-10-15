package com.insta.mod;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

// Долгий тап по вордмарку в шапке ленты открывает настройки мода.
// Вызывается из smali-хуков (MergedFeedsActionBar, ActionBarTitleViewSwitcher).
public class WordmarkHook {
    public static void install(ImageView view) {
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Context ctx = v.getContext();
                Intent i = new Intent(ctx, ModSettingsActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return true;
            }
        });
    }
}
