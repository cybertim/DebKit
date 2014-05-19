package com.mizusoft.debkit;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author Tim
 */
public class Shell extends AsyncTask<String, String, Boolean> {

    private final MainActivity parent;
    private ShellExec shellExec = null;

    public void setShellExec(ShellExec shellExec) {
        this.shellExec = shellExec;
    }

    public Shell(MainActivity parent) {
        this.parent = parent;
    }

    @Override
    protected Boolean doInBackground(String... paramss) {
        int count = paramss.length;
        boolean result = true;
        for (int i = 0; i < count; i++) {
            String param = paramss[i];
            try {
                Runtime terminal = (Runtime) Runtime.getRuntime();
                Process process = terminal.exec("su");
                DataOutputStream stdout = new DataOutputStream(process.getOutputStream());
                stdout.writeBytes(param + "\n");
                stdout.flush();
                stdout.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int n;
                char[] buffer = new char[1024];
                while ((n = reader.read(buffer)) != -1) {
                    //log += String.valueOf(buffer, 0, n);
                    publishProgress(String.valueOf(buffer, 0, n));
                }

                process.waitFor();
                if (process.exitValue() != 0) {
                    result = false;
                }
                stdout.close();
            } catch (IOException ioException) {
                Log.e(Shell.class.getName(), ioException.getMessage());
                result = false;
            } catch (InterruptedException interruptedException) {
                Log.e(Shell.class.getName(), interruptedException.getMessage());
                result = false;
            }
        }
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (shellExec != null) {
            shellExec.execute(parent, result);
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        for (String s : values) {
            parent.print(s);
        }
    }

}
