package com.mizusoft.debkit;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 *
 * @author Tim
 */
public class MainActivity extends Activity {

    private final static String BUGREPORT_EMAIL = "debkit@mizusoft.com";
    private final static String BUGREPORT_SUBJECT = "[DekKit Bugreport]";
    private final static String MSG_NO_ROOT = "You currently DO NOT have root!\nPlease root your device first.\nIf you don't know what this means and would like to know start with googling 'android root'.";
    private final static String MSG_BUSY = "There is another process running at the moment. Please let it finish before doing something else.";
    private String text = "";
    private String installPath;
    private boolean ready = true;
    private boolean busy = false;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        this.sharedPref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sp, String string) {
                configure();
            }
        });
        print("Welcome to DebKit.");
        print("Model: " + android.os.Build.BRAND + " " + android.os.Build.MODEL);
        print("Device: " + android.os.Build.DEVICE + " " + android.os.Build.VERSION.RELEASE);
        print("CPU: " + android.os.Build.CPU_ABI);
        print("Checking for root/su...");
        //
        // execute the setup-method if there is root available (by ls'ing a priv directory as su -test)
        Shell shell = new Shell(this);
        shell.setShellExec(new ShellExec() {

            @Override
            public void execute(MainActivity p, boolean r) {
                p.setup(r);
            }
        });
        shell.execute("ls /data/local > /dev/null");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.quit:
                finish();
                return true;
            case R.id.settings:
                startActivity(new Intent(this, PrefActivity.class));
                return true;
            case R.id.help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            case R.id.mount:
                mount();
                return true;
            case R.id.unmount:
                umount();
                return true;
            case R.id.install:
                install();
                return true;
            case R.id.fix:
                fix();
                return true;
            case R.id.bugreport:
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, BUGREPORT_SUBJECT + " " + android.os.Build.BRAND + " " + android.os.Build.MODEL + " " + android.os.Build.VERSION.RELEASE + " " + android.os.Build.CPU_ABI);
                intent.putExtra(Intent.EXTRA_TEXT, text + "\n------8<------\n<my comment here>");
                intent.setData(Uri.parse("mailto:" + BUGREPORT_EMAIL));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void print(String s) {
        if (!this.text.endsWith("\n")) {
            this.text += "\n";
        }
        this.text += s;
        final TextView myTextView = (TextView) findViewById(R.id.console);
        myTextView.setText(this.text);
    }

    public void setup(boolean r) {
        if (r) {
            if (!copyAssets(R.raw.debootstrap, "debootstrap.tar")) {
                ready = false;
            }
            if (!copyAssets(R.raw.busybox, "busybox")) {
                ready = false;
            }
            if (!copyAssets(R.raw.pkgdetails, "pkgdetails")) {
                ready = false;
            }
            if (!copyAssets(R.raw.mke2fs, "mke2fs")) {
                ready = false;
            }
            if (!copyAssets(R.raw.debkit, "debkit")) {
                ready = false;
            }
            if (ready) {
                print("Select 'settings' to configure your chroot image and select 'install' from the menu to start the debootstrapping process.");
                configure();
            } else {
                print("No ready!\nOne or more files failed to be initialized.");
            }
        } else {
            ready = false;
            print(MSG_NO_ROOT);
        }
    }

    private void configure() {
        Shell shell = new Shell(this);
        shell.setShellExec(new ShellExec() {

            @Override
            public void execute(MainActivity parent, boolean result) {
                if (result) {
                    parent.print("Writing new config file ... OK");
                } else {
                    parent.print("Writing new config file ... FAILED!");
                }
            }
        });
        String prefPath = sharedPref.getString("prefPath", "/sdcard/debkit");
        String prefSrc = sharedPref.getString("prefSrc", "ftp.nl.debian.org/debian");
        String prefDebianDistribution = sharedPref.getString("prefDebianDistribution", "wheezy");
        int prefSize = Integer.valueOf(sharedPref.getString("prefSize", "1024"));
        String c = this.installPath + "/config";
        shell.execute(
                "echo '#custom app settings'" + " > " + c,
                "echo 'BIN=" + this.installPath + "' >> " + c,
                "echo 'KIT=" + prefPath + "' >> " + c,
                "echo 'DST=" + prefDebianDistribution + "' >> " + c,
                "echo 'SRC=" + prefSrc + "' >> " + c,
                "echo 'SIZ=" + prefSize + "' >> " + c);
    }

    private void install() {
        if (ready && !busy) {
            // be sure to not let the user start >1 instances of the install process.
            busy = true;
            Shell shell = new Shell(this);
            shell.setShellExec(new ShellExec() {

                @Override
                public void execute(MainActivity parent, boolean result) {
                    busy = false;
                    if (result) {
                        parent.notify("DebKit Image Ready", "your chroot image is ready");
                    }
                }
            });
            shell.execute(installPath + "/debkit install");
        } else {
            if (busy) {
                print(MSG_BUSY);
            } else {
                print(MSG_NO_ROOT);
            }
        }
    }

    private void fix() {
        if (ready) {
            Shell shell = new Shell(this);
            shell.execute(installPath + "/debkit link");
        } else {
            print(MSG_NO_ROOT);
        }
    }

    private void umount() {
        if (ready) {
            Shell shell = new Shell(this);
            shell.execute(installPath + "/debkit umount");
        } else {
            print(MSG_NO_ROOT);
        }
    }

    private void mount() {
        if (ready) {
            Shell shell = new Shell(this);
            shell.execute(installPath + "/debkit mount");
        } else {
            print(MSG_NO_ROOT);
        }
    }

    private boolean copyAssets(int id, String filename) {
        try {
            InputStream ins = getResources().openRawResource(id);
            byte[] buffer = new byte[ins.available()];
            ins.read(buffer);
            ins.close();
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(buffer);
            fos.close();

            File file = getFileStreamPath(filename);
            file.setExecutable(true);

            if (this.installPath == null || "".equals(this.installPath)) {
                this.installPath = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
                this.sharedPref.edit().putString("installPath", installPath).commit();
                print("Install path: " + installPath);
            }
            print(filename + " ... OK.");

        } catch (Exception e) {
            print(filename + " ... FAILED!");
            return false;
        }
        return true;
    }

    public void notify(String title, String text) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, HelpActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification n = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.app_icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();
        notificationManager.notify(0, n);
    }

}
