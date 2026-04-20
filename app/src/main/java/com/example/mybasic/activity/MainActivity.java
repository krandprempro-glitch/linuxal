package com.example.mybasic.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText inputCommand;
    private TextView outputText;
    private ScrollView scrollView;
    private Handler mainHandler;
    private Process linuxProcess;
    
    // قائمة الأذونات المطلوبة
    private static final int PERMISSION_REQUEST_CODE = 100;
    private String[] requiredPermissions;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        
        // إعداد الأذونات حسب إصدار Android
        setupPermissions();
        
        // طلب الأذونات
        checkAndRequestPermissions();
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        inputCommand = findViewById(R.id.input_command);
        outputText = findViewById(R.id.terminal_output);
        scrollView = findViewById(R.id.terminal_scroll);
        ImageButton sendButton = findViewById(R.id.send_button);
        
        outputText.setMovementMethod(new ScrollingMovementMethod());
        outputText.setText("LinuxAL Terminal Ready.\n\n");
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        sendButton.setOnClickListener(v -> executeCommand());
        
        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                executeCommand();
                return true;
            }
            return false;
        });
        
        startLinux();
    }
    
    private void setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requiredPermissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.FOREGROUND_SERVICE
            };
        } else {
            requiredPermissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }
    
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        } else {
            appendToTerminal("✓ All permissions granted.\n");
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
                appendToTerminal("✓ All permissions granted.\n");
            } else {
                appendToTerminal("⚠️ Some permissions denied. Some features may not work.\n");
            }
        }
    }
    
    private void startLinux() {
        appendToTerminal("\n[Starting Linux...]\n");
        
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
