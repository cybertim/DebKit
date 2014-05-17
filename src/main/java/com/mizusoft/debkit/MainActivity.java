package com.mizusoft.debkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

    private String text = "";
    private String installPath;
    private boolean ready = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        print("Welcome to DebKit.");
        print("Model: " + android.os.Build.BRAND + " " + android.os.Build.MODEL);
        print("Device: " + android.os.Build.DEVICE + " " + android.os.Build.VERSION.RELEASE);
        print("CPU: " + android.os.Build.CPU_ABI);
        setup();
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
            case R.id.mount:
                mount();
                return true;
            case R.id.unmount:
                umount();
                return true;
            case R.id.install:
                BackgroundTask task = new BackgroundTask();
                task.execute("test");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void print(String s) {
        this.text += "\n" + s;
        final TextView myTextView = (TextView) findViewById(R.id.console);
        myTextView.setText(this.text);
    }

    private void setup() {
        Shell shell = new Shell();
        if (shell.execute("ls /data/local")) {
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
                print("Ready.\nSelect 'settings' to configure your chroot image and select 'install' from the menu to start the debootstrapping process.");
            } else {
                print("No ready!\nOne or more files failed to be initialized. Please send a bugreport by using the 'Send Log' function in the menu.");
            }
        } else {
            ready = false;
            print("You currently DO NOT have root!\nPlease root your device first.\nIf you don't know what this means and would like to know start with googling 'android root'.");
        }
    }

    private void install() {
        if (ready) {
            Shell shell = new Shell();

        } else {
            print("Sorry, unable to install. Are you root?");
        }
    }

    private void umount() {
        if (ready) {
            Shell shell = new Shell();
            shell.execute(installPath + "/debkit umount");
        } else {
            print("Sorry, unable to install. Are you root?");
        }
    }

    private void mount() {
        if (ready) {
            Shell shell = new Shell();
            shell.execute(installPath + "/debkit mount");
        } else {
            print("Sorry, unable to install. Are you root?");
        }
    }

    private class BackgroundTask extends AsyncTask<String, Integer, Integer> {

        Shell shell = new Shell();

        @Override
        protected Integer doInBackground(String... paramss) {
            shell.execute("ls");
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            print(shell.getLog());
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

            if (installPath == null || installPath == "") {
                installPath = file.getPath();
            }
            print(file.getAbsolutePath());

        } catch (Exception e) {
            print(filename + " ... FAILED!");
            return false;
        }
        return true;
    }
}
