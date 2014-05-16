package com.mizusoft.debkit;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 *
 * @author Tim
 */
public class Shell {

    private String log = "";
    private final static int BUFF_LEN = 1024;

    public boolean execute(String cmnd) {
        boolean result = true;
        try {
            // start 'su' process
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "system/bin/sh"});
            // open all streams
            DataOutputStream stdin = new DataOutputStream(process.getOutputStream());

            stdin.writeBytes(cmnd + "\n");
            stdin.flush();
            stdin.close();

            // grab outputs
            InputStream stdout = process.getInputStream();
            byte[] buffer = new byte[BUFF_LEN];
            int read;
            while (true) {
                read = stdout.read(buffer);
                this.log += new String(buffer, 0, read);
                if (read < BUFF_LEN) {
                    break;
                }
            }

            // wait for the process to finish
            process.waitFor();
            if (process.exitValue() != 0) {
                result = false;
            }

            stdin.close();
        } catch (Exception exception) {
            this.log += exception.getMessage() + "\n";
            result = false;
        }
        return result;
    }

    public String getLog() {
        return this.log;
    }

}
