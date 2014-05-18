package com.mizusoft.debkit;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "/data/data/com.mizusoft.com/files/busybox ash"});
            // open all streams
            DataOutputStream stdin = new DataOutputStream(process.getOutputStream());

            stdin.writeBytes(cmnd + " 2>&1\n");
            stdin.flush();
            stdin.close();

            // grab outputs
            final InputStream stdout = process.getInputStream();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    byte[] buffer = new byte[BUFF_LEN];
                    int read;
                    while (true) {
                        try {
                            read = stdout.read(buffer);
                        } catch (IOException ioe) {
                            break;
                        }
                        log += new String(buffer, 0, read);
                        if (read < BUFF_LEN) {
                            break;
                        }
                    }
                }
            }).start();

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
