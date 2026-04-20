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
    
    // تحميل مكتبة proot الأصلية
    static {
        try {
            System.loadLibrary("proot");
            System.out.println("✓ proot library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("✗ Failed to load proot library: " + e.getMessage());
        }
    }
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // طلب الأذونات
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            for (String p : permissions) {
                if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                    break;
                }
            }
        }
        
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
        outputText.setText("LinuxAL Ready.\nLoading...\n");
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
        
        // تشغيل Linux تلقائياً
        startLinux();
    }
    
    private void startLinux() {
        new Thread(() -> {
            try {
                // الحصول على مسار المكتبة
                String prootPath = getApplicationInfo().nativeLibraryDir + "/libproot.so";
                appendToTerminal("✓ proot library: " + prootPath + "\n");
                
                // نسخ سكربت التشغيل
                copyAssetToFile("start-linux.sh", "start-linux.sh");
                
                // تحديث السكربت لاستخدام المكتبة
                String scriptPath = getFilesDir() + "/start-linux.sh";
                updateScriptProotPath(scriptPath, prootPath);
                
                appendToTerminal("🚀 Starting Arch Linux...\n");
                
                // تشغيل السكربت
                linuxProcess = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", scriptPath});
                
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
                
            } catch (Exception e) {
                appendToTerminal("Error: " + e.getMessage() + "\n");
            }
        }).start();
    }
    
    private void updateScriptProotPath(String scriptPath, String prootPath) {
        try {
            File scriptFile = new File(scriptPath);
            StringBuilder content = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(scriptFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("PROOT_PATH=")) {
                    content.append("PROOT_PATH=\"").append(prootPath).append("\"\n");
                } else {
                    content.append(line).append("\n");
                }
            }
            reader.close();
            
            FileWriter writer = new FileWriter(scriptFile);
            writer.write(content.toString());
            writer.close();
            
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
