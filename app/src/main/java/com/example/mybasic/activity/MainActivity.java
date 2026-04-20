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
    private boolean isLinuxRunning = false;
    private Process linuxProcess;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 50, 30, 30);
        layout.setBackgroundColor(Color.BLACK);
        
        TextView title = new TextView(this);
        title.setText("🐧 LinuxAL - Alpine Linux");
        title.setTextSize(24);
        title.setTextColor(Color.GREEN);
        layout.addView(title);
        
        inputCommand = new EditText(this);
        inputCommand.setHint("$ Enter command...");
        inputCommand.setHintTextColor(Color.GRAY);
        inputCommand.setTextColor(Color.WHITE);
        inputCommand.setBackgroundColor(Color.DKGRAY);
        inputCommand.setPadding(20, 20, 20, 20);
        layout.addView(inputCommand);
        
        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        
        Button runButton = new Button(this);
        runButton.setText("Run");
        runButton.setBackgroundColor(Color.GREEN);
        runButton.setTextColor(Color.BLACK);
        runButton.setPadding(20, 15, 20, 15);
        buttonRow.addView(runButton);
        
        Button clearButton = new Button(this);
        clearButton.setText("Clear");
        clearButton.setBackgroundColor(Color.RED);
        clearButton.setTextColor(Color.WHITE);
        clearButton.setPadding(20, 15, 20, 15);
        buttonRow.addView(clearButton);
        
        Button linuxButton = new Button(this);
        linuxButton.setText("🐧 Start Linux");
        linuxButton.setBackgroundColor(Color.BLUE);
        linuxButton.setTextColor(Color.WHITE);
        linuxButton.setPadding(20, 15, 20, 15);
        buttonRow.addView(linuxButton);
        
        Button stopButton = new Button(this);
        stopButton.setText("⏹️ Stop Linux");
        stopButton.setBackgroundColor(Color.rgb(255, 165, 0));
        stopButton.setTextColor(Color.WHITE);
        stopButton.setPadding(20, 15, 20, 15);
        buttonRow.addView(stopButton);
        
        layout.addView(buttonRow);
        
        scrollView = new ScrollView(this);
        outputText = new TextView(this);
        outputText.setTextSize(11);
        outputText.setTextColor(Color.GREEN);
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
        stopButton.setOnClickListener(v -> stopLinux());
    }
    
    private void startLinux() {
        if (isLinuxRunning) {
            appendToTerminal("Linux is already running!\n");
            return;
        }
        
        appendToTerminal("\n[Starting Alpine Linux...]\n");
        
        new Thread(() -> {
            try {
                // نسخ الملفات
                copyAssetToFile("proot", "proot");
                copyAssetToFile("start-linux.sh", "start-linux.sh");
                
                // فك ضغط rootfs إذا لم يكن موجوداً
                File rootfsDir = new File(getFilesDir(), "rootfs");
                if (!rootfsDir.exists() || rootfsDir.list().length == 0) {
                    appendToTerminal("[1/3] Extracting rootfs (first time, please wait)...\n");
                    extractRootfs();
                }
                
                appendToTerminal("[2/3] Setting up environment...\n");
                
                // تشغيل السكربت
                String scriptPath = getFilesDir() + "/start-linux.sh";
                String appDir = getFilesDir().getAbsolutePath();
                
                appendToTerminal("[3/3] Starting Alpine...\n");
                
                linuxProcess = Runtime.getRuntime().exec(new String[]{
                    "/system/bin/sh", scriptPath, appDir
                });
                
                // قراءة المخرجات
                BufferedReader reader = new BufferedReader(new InputStreamReader(linuxProcess.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(linuxProcess.getErrorStream()));
                
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    mainHandler.post(() -> outputText.append(output + "\n"));
                }
                while ((line = errorReader.readLine()) != null) {
                    final String output = line;
                    mainHandler.post(() -> outputText.append("[ERR] " + output + "\n"));
                }
                
                linuxProcess.waitFor();
                isLinuxRunning = false;
                appendToTerminal("\n[Linux session ended]\n");
                
            } catch (Exception e) {
                appendToTerminal("Error: " + e.getMessage() + "\n");
                isLinuxRunning = false;
            }
        }).start();
        
        isLinuxRunning = true;
    }
    
    private void stopLinux() {
        if (linuxProcess != null) {
            appendToTerminal("\n[Stopping Linux...]\n");
            linuxProcess.destroy();
            isLinuxRunning = false;
        } else {
            appendToTerminal("No Linux process running.\n");
        }
    }
    
    private void extractRootfs() {
        try {
            File rootfsDir = new File(getFilesDir(), "rootfs");
            rootfsDir.mkdirs();
            
            String tarPath = getFilesDir() + "/rootfs.tar.gz";
            copyAssetToFile("rootfs.tar.gz", "rootfs.tar.gz");
            
            Process process = Runtime.getRuntime().exec(new String[]{
                "/system/bin/sh", "-c", "tar -xzf " + tarPath + " -C " + rootfsDir.getAbsolutePath() + " 2>&1"
            });
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                final String output = line;
                mainHandler.post(() -> outputText.append(output + "\n"));
            }
            process.waitFor();
            
            // حذف الملف المضغوط بعد فك الضغط
            new File(tarPath).delete();
            
            appendToTerminal("✓ Rootfs extracted successfully!\n");
            
        } catch (Exception e) {
            appendToTerminal("Extract error: " + e.getMessage() + "\n");
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
            }
        } catch (Exception e) {
            appendToTerminal("Failed to copy: " + assetName + "\n");
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
                mainHandler.post(() -> {
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
}
