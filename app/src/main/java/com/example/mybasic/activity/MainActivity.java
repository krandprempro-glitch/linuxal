package com.example.mybasic.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class MainActivity extends AppCompatActivity {
    private EditText inputCommand;
    private TextView outputText;
    private ScrollView scrollView;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 50, 30, 30);
        layout.setBackgroundColor(Color.BLACK);
        
        TextView title = new TextView(this);
        title.setText("LinuxAL - Linux on Android");
        title.setTextSize(28);
        title.setTextColor(Color.GREEN);
        layout.addView(title);
        
        inputCommand = new EditText(this);
        inputCommand.setHint("Enter command...");
        inputCommand.setHintTextColor(Color.GRAY);
        inputCommand.setTextColor(Color.WHITE);
        inputCommand.setBackgroundColor(Color.DKGRAY);
        inputCommand.setPadding(20, 20, 20, 20);
        layout.addView(inputCommand);
        
        Button runButton = new Button(this);
        runButton.setText("Run");
        runButton.setBackgroundColor(Color.GREEN);
        runButton.setTextColor(Color.BLACK);
        runButton.setPadding(20, 15, 20, 15);
        layout.addView(runButton);
        
        Button clearButton = new Button(this);
        clearButton.setText("Clear");
        clearButton.setBackgroundColor(Color.RED);
        clearButton.setTextColor(Color.WHITE);
        clearButton.setPadding(20, 15, 20, 15);
        layout.addView(clearButton);
        
        Button linuxButton = new Button(this);
        linuxButton.setText("Start Linux");
        linuxButton.setBackgroundColor(Color.BLUE);
        linuxButton.setTextColor(Color.WHITE);
        linuxButton.setPadding(20, 15, 20, 15);
        layout.addView(linuxButton);
        
        scrollView = new ScrollView(this);
        outputText = new TextView(this);
        outputText.setTextSize(12);
        outputText.setTextColor(Color.WHITE);
        outputText.setBackgroundColor(Color.BLACK);
        outputText.setPadding(10, 10, 10, 10);
        outputText.setTypeface(android.graphics.Typeface.MONOSPACE);
        outputText.setText("LinuxAL Ready.\n\n");
        scrollView.addView(outputText);
        layout.addView(scrollView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        
        setContentView(layout);
        mainHandler = new Handler(Looper.getMainLooper());
        
        runButton.setOnClickListener(v -> executeCommand());
        clearButton.setOnClickListener(v -> outputText.setText(""));
        linuxButton.setOnClickListener(v -> startLinux());
    }
    
    private void startLinux() {
        appendToTerminal("\n[Starting Linux with proot...]\n");
        
        new Thread(() -> {
            try {
                // نسخ الملفات
                copyAssetToFile("proot", "proot");
                copyAssetToFile("start-linux.sh", "start-linux.sh");
                
                // فك ضغط rootfs
                File rootfsDir = new File(getFilesDir(), "rootfs");
                if (!rootfsDir.exists()) {
                    appendToTerminal("[1/2] Extracting rootfs (first time, please wait)...\n");
                    extractRootfs();
                }
                
                // تشغيل السكربت
                appendToTerminal("[2/2] Starting Ubuntu...\n");
                String scriptPath = getFilesDir() + "/start-linux.sh";
                Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", scriptPath});
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    runOnUiThread(() -> outputText.append(output + "\n"));
                }
                
                process.waitFor();
                
            } catch (Exception e) {
                appendToTerminal("Error: " + e.getMessage() + "\n");
            }
        }).start();
    }
    
    private void extractRootfs() {
        try {
            File rootfsDir = new File(getFilesDir(), "rootfs");
            rootfsDir.mkdirs();
            
            String tarPath = getFilesDir() + "/rootfs.tar.xz";
            copyAssetToFile("rootfs.tar.xz", "rootfs.tar.xz");
            
            Process process = Runtime.getRuntime().exec(new String[]{
                "/system/bin/sh", "-c", "tar -xf " + tarPath + " -C " + rootfsDir.getAbsolutePath() + " 2>&1"
            });
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                final String output = line;
                runOnUiThread(() -> outputText.append(output + "\n"));
            }
            process.waitFor();
            
            // حذف الملف المضغوط بعد فك الضغط
            new File(tarPath).delete();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void copyAssetToFile(String assetName, String destName) {
        try {
            File destFile = new File(getFilesDir(), destName);
            if (!destFile.exists()) {
                InputStream in = getAssets().open(assetName);
                OutputStream out = new FileOutputStream(destFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
                destFile.setExecutable(true);
                appendToTerminal("✓ Copied: " + assetName + "\n");
            }
        } catch (Exception e) {
            appendToTerminal("✗ Failed to copy: " + assetName + " - " + e.getMessage() + "\n");
        }
    }
    
    private void executeCommand() {
        String command = inputCommand.getText().toString().trim();
        if (command.isEmpty()) return;
        
        appendToTerminal("$ " + command + "\n");
        inputCommand.setText("");
        
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", command});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = errorReader.readLine()) != null) {
                    output.append("[ERR] ").append(line).append("\n");
                }
                
                process.waitFor();
                String finalOutput = output.toString();
                runOnUiThread(() -> {
                    outputText.append(finalOutput);
                    scrollView.fullScroll(View.FOCUS_DOWN);
                });
                
            } catch (Exception e) {
                appendToTerminal("Error: " + e.getMessage() + "\n");
            }
        }).start();
    }
    
    private void appendToTerminal(String text) {
        mainHandler.post(() -> {
            outputText.append(text);
            scrollView.fullScroll(View.FOCUS_DOWN);
        });
    }
    
    private void runOnUiThread(Runnable action) {
        mainHandler.post(action);
    }
}
