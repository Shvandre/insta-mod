package com.insta.mod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

public class InstallResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(confirm);
            }
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(ctx, "Insta Mod обновлён", Toast.LENGTH_LONG).show();
        } else if (status != PackageInstaller.STATUS_PENDING_USER_ACTION) {
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Toast.makeText(ctx, "Установка не удалась: " + msg, Toast.LENGTH_LONG).show();
        }
    }
}
