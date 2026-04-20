package com.example.mybasic.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    private EditText inputCommand;
    private TextView outputText;
    private ScrollView scrollView;

    private File filesDir;
    private File prootFile;
    private File rootfsDir;

    private Process linuxProcess;
    private OutputStream linuxInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filesDir = getFilesDir();
        prootFile = new File(filesDir, "proot");
        rootfsDir = new File(filesDir, "rootfs");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(20,20,20,20);

        inputCommand = new EditText(this);
        inputCommand.setHint("Enter command...");
        inputCommand.setTextColor(Color.WHITE);
        layout.addView(inputCommand);

        Button run = new Button(this);
        run.setText("Run");
        layout.addView(run);

        Button start = new Button(this);
        start.setText("Start Linux");
        layout.addView(start);

        scrollView = new ScrollView(this);
        outputText = new TextView(this);
        outputText.setTextColor(Color.GREEN);
        scrollView.addView(outputText);
        layout.addView(scrollView);

        setContentView(layout);

        run.setOnClickListener(v -> sendCommand());
        start.setOnClickListener(v -> startLinux());
    }

    // ================= START LINUX =================
    private void startLinux() {
        new Thread(() -> {
            try {
                runOnUiThread(() -> log("Preparing Linux...\n"));

                copyAsset("proot", prootFile);
                prootFile.setExecutable(true);

                File tarFile = new File(filesDir, "rootfs.tar.xz");
                copyAsset("rootfs.tar.xz", tarFile);

                if (!rootfsDir.exists()) {
                    runOnUiThread(() -> log("Extracting rootfs...\n"));

                    Process p = Runtime.getRuntime().exec(
                        filesDir + "/proot --link2symlink tar -xJf "
                        + tarFile.getAbsolutePath() + " -C " + filesDir
                    );
                    p.waitFor();
                }

                runOnUiThread(() -> log("Starting Linux...\n"));

                String cmd = prootFile.getAbsolutePath()
                        + " -0 -r " + rootfsDir.getAbsolutePath()
                        + " -b /dev -b /proc -b /sys"
                        + " -w /root /bin/sh";

                linuxProcess = Runtime.getRuntime().exec(cmd);
                linuxInput = linuxProcess.getOutputStream();

                readOutput(linuxProcess);

            } catch (Exception e) {
                runOnUiThread(() -> log("ERROR: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    // ================= SEND COMMAND =================
    private void sendCommand() {
        try {
            String cmd = inputCommand.getText().toString() + "\n";
            inputCommand.setText("");

            if (linuxInput != null) {
                linuxInput.write(cmd.getBytes());
                linuxInput.flush();
            } else {
                log("Linux not started\n");
            }

        } catch (Exception e) {
            log("Error: " + e.getMessage());
        }
    }

    // ================= READ OUTPUT =================
    private void readOutput(Process process) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    runOnUiThread(() -> log(finalLine + "\n"));
                }

            } catch (Exception e) {
                runOnUiThread(() -> log("Read error\n"));
            }
        }).start();
    }

    // ================= COPY ASSET =================
    private void copyAsset(String name, File out) throws IOException {
        if (out.exists()) return;

        InputStream in = getAssets().open(name);
        FileOutputStream outStream = new FileOutputStream(out);

        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            outStream.write(buffer, 0, read);
        }

        in.close();
        outStream.close();
    }

    private void log(String text) {
        outputText.append(text);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}