package com.example.mybasic.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.*;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.*;

public class MainActivity extends AppCompatActivity {
    private EditText inputCommand;
    private TextView outputText;
    private ScrollView scrollView;
    private Handler mainHandler;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String[] requiredPermissions = {
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        checkAndRequestPermissions();
        
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
    }
    
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE);
            }
        }
    }
    
    private void startLinux() {
        appendToTerminal("\n[Starting Alpine Linux...]\n");
        
        new Thread(() -> {
            try {
                copyAssetToFile("proot", "proot");
                copyAssetToFile("start-linux.sh", "start-linux.sh");
                
                File rootfsDir = new File(getFilesDir(), "rootfs");
                if (!rootfsDir.exists()) {
                    appendToTerminal("[1/2] Extracting rootfs (first time, please wait)...\n");
                    extractRootfs();
                }
                
                appendToTerminal("[2/2] Starting Alpine...\n");
                String scriptPath = getFilesDir() + "/start-linux.sh";
                Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", scriptPath});
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    mainHandler.post(() -> outputText.append(output + "\n"));
                }
                
                process.waitFor();
                
            } catch (Exception e) {
                mainHandler.post(() -> outputText.append("Error: " + e.getMessage() + "\n"));
            }
        }).start();
    }
    
    private void extractRootfs() {
        try {
            File rootfsDir = new File(getFilesDir(), "rootfs");
            rootfsDir.mkdirs();
            
            String tarPath = getFilesDir() + "/rootfs.tar.gz";
            copyAssetToFile("rootfs.tar.gz", "rootfs.tar.gz");
            
            Process process = Runtime.getRuntime().exec(new String[]{
                "/system/bin/sh", "-c", "tar -xzf " + tarPath + " -C " + rootfsDir.getAbsolutePath()
            });
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                final String output = line;
                mainHandler.post(() -> outputText.append(output + "\n"));
            }
            process.waitFor();
            
            new File(tarPath).delete();
            
        } catch (Exception e) {
            mainHandler.post(() -> outputText.append("Extract error: " + e.getMessage() + "\n"));
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
                mainHandler.post(() -> outputText.append("✓ Copied: " + assetName + "\n"));
            }
        } catch (Exception e) {
            mainHandler.post(() -> outputText.append("✗ Failed to copy: " + assetName + "\n"));
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
                mainHandler.post(() -> outputText.append("Error: " + e.getMessage() + "\n"));
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
