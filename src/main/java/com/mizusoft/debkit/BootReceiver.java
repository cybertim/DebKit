package com.mizusoft.debkit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 *
 * @author Tim
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context cntxt, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(cntxt);
            boolean prefBoot = sharedPref.getBoolean("prefBoot", false);
            String installPath = sharedPref.getString("installPath", null);
            if (prefBoot && installPath != null) {
                Shell shell = new Shell(null);
                shell.setShellExec(new ShellExec() {

                    @Override
                    public void execute(MainActivity parent, boolean result) {
                        if (result) {
                            Toast.makeText(cntxt, "DebKit is mounted", Toast.LENGTH_SHORT);
                        } else {
                            Toast.makeText(cntxt, "DebKit failed to mount!", Toast.LENGTH_SHORT);
                        }
                    }
                });
                shell.execute(installPath + "/debkit mount");
            }
        }
    }

}
