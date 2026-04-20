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
    private Process linuxProcess;
    
    // كود طلب الأذونات
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String[] requiredPermissions = {
        Manifest.permission.INTERNET,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // طلب الأذونات
        checkAndRequestPermissions();
        
        // واجهة المستخدم
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 40, 20, 20);
        layout.setBackgroundColor(Color.BLACK);
        
        TextView title = new TextView(this);
        title.setText("🐧 LinuxAL - Arch Linux");
        title.setTextSize(20);
        title.setTextColor(Color.GREEN);
        title.setPadding(10, 10, 10, 20);
        layout.addView(title);
        
        inputCommand = new EditText(this);
        inputCommand.setHint("$ Enter command...");
        inputCommand.setHintTextColor(Color.GRAY);
        inputCommand.setTextColor(Color.WHITE);
        inputCommand.setBackgroundColor(Color.DKGRAY);
        inputCommand.setPadding(20, 15, 20, 15);
        inputCommand.setSingleLine(true);
        layout.addView(inputCommand);
        
        Button sendButton = new Button(this);
        sendButton.setText("Send");
        sendButton.setBackgroundColor(Color.GREEN);
        sendButton.setTextColor(Color.BLACK);
        layout.addView(sendButton);
        
        scrollView = new ScrollView(this);
        outputText = new TextView(this);
        outputText.setTextSize(11);
        outputText.setTextColor(Color.GREEN);
        outputText.setBackgroundColor(Color.BLACK);
        outputText.setPadding(10, 10, 10, 10);
        outputText.setTypeface(android.graphics.Typeface.MONOSPACE);
        outputText.setText("LinuxAL Ready.\nRequesting permissions...\n");
        scrollView.addView(outputText);
        layout.addView(scrollView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        
        setContentView(layout);
        mainHandler = new Handler(Looper.getMainLooper());
        
        sendButton.setOnClickListener(v -> executeCommand());
        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            executeCommand();
            return true;
        });
        
        // بدء Linux بعد منح الأذونات
        startLinux();
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
            } else {
                appendToTerminal("✓ All permissions granted.\n");
            }
        } else {
            appendToTerminal("✓ Permissions granted (Android < 6.0).\n");
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                appendToTerminal("✓ All permissions granted!\n");
            } else {
                appendToTerminal("⚠️ Some permissions denied. Linux may not work properly.\n");
            }
        }
    }
    
    private void startLinux() {
        // تأخير بسيط لظهور رسالة الأذونات
        mainHandler.postDelayed(() -> {
            appendToTerminal("\n[Starting Arch Linux...]\n");
            new Thread(() -> {
                try {
                    copyAssetToFile("proot", "proot");
                    copyAssetToFile("start-linux.sh", "start-linux.sh");
                    
                    String scriptPath = getFilesDir() + "/start-linux.sh";
                    linuxProcess = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", scriptPath});
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(linuxProcess.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String output = line;
                        mainHandler.post(() -> outputText.append(output + "\n"));
                    }
                    
                    linuxProcess.waitFor();
                } catch (Exception e) {
                    appendToTerminal("Error: " + e.getMessage() + "\n");
                }
            }).start();
        }, 2000);
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
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    mainHandler.post(() -> outputText.append(output + "\n"));
                }
                process.waitFor();
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (linuxProcess != null) {
            linuxProcess.destroy();
        }
    }
}
