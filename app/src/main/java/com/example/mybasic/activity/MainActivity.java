package com.example.mybasic.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private EditText inputCommand;
    private TextView outputText;
    private ScrollView scrollView;
    private Handler mainHandler;
    private Process linuxProcess;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        inputCommand = findViewById(R.id.input_command);
        outputText = findViewById(R.id.terminal_output);
        scrollView = findViewById(R.id.terminal_scroll);
        ImageButton sendButton = findViewById(R.id.send_button);
        
        outputText.setTextColor(0xFFFFFFFF);
        outputText.setTextSize(13);
        outputText.setTypeface(android.graphics.Typeface.MONOSPACE);
        outputText.setMovementMethod(new ScrollingMovementMethod());
        outputText.setText("LinuxAL Terminal Ready.\n\n");
        
        inputCommand.setTextColor(0xFFFFFFFF);
        inputCommand.setHintTextColor(0xFF888888);
        
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
    
    private void startLinux() {
        appendToTerminal("\n[Starting Alpine Linux...]\n");
        
        new Thread(() -> {
            try {
                copyAssetToFile("proot", "proot");
                copyAssetToFile("rootfs.tar.gz", "rootfs.tar.gz");
                
                File rootfsDir = new File(getFilesDir(), "rootfs");
                if (!rootfsDir.exists() || rootfsDir.list().length == 0) {
                    appendToTerminal("Extracting rootfs...\n");
                    extractRootfs();
                }
                
                createStartScript();
                
                String scriptPath = getFilesDir() + "/start-linux.sh";
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", scriptPath);
                pb.directory(getFilesDir());
                pb.redirectErrorStream(true);
                linuxProcess = pb.start();
                
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
    
    private void extractRootfs() {
        try {
            String tarPath = getFilesDir() + "/rootfs.tar.gz";
            String destPath = getFilesDir() + "/rootfs";
            
            Process process = Runtime.getRuntime().exec(new String[]{
                "/system/bin/sh", "-c", "mkdir -p " + destPath + " && cd " + destPath + " && tar -xzf " + tarPath
            });
            process.waitFor();
            
            appendToTerminal("✓ Rootfs extracted\n");
            
        } catch (Exception e) {
            appendToTerminal("Extract error: " + e.getMessage() + "\n");
        }
    }
    
    private void createStartScript() {
        try {
            String scriptPath = getFilesDir() + "/start-linux.sh";
            FileWriter fw = new FileWriter(scriptPath);
            fw.write("#!/system/bin/sh\n\n");
            fw.write("echo '========================================='\n");
            fw.write("echo '   LinuxAL - Alpine Linux'\n");
            fw.write("echo '========================================='\n\n");
            fw.write("PROOT_PATH=\"" + getFilesDir() + "/proot\"\n");
            fw.write("ROOTFS_PATH=\"" + getFilesDir() + "/rootfs\"\n\n");
            fw.write("if [ ! -f \"$PROOT_PATH\" ]; then\n");
            fw.write("    echo '❌ proot not found!'\n");
            fw.write("    exit 1\n");
            fw.write("fi\n\n");
            fw.write("if [ ! -d \"$ROOTFS_PATH\" ]; then\n");
            fw.write("    echo '❌ rootfs not found!'\n");
            fw.write("    exit 1\n");
            fw.write("fi\n\n");
            fw.write("chmod 755 $PROOT_PATH\n\n");
            fw.write("echo '🚀 Starting Alpine Linux...'\n");
            fw.write("echo '========================================='\n\n");
            fw.write("exec $PROOT_PATH \\\n");
            fw.write("  -R $ROOTFS_PATH \\\n");
            fw.write("  -b /dev \\\n");
            fw.write("  -b /proc \\\n");
            fw.write("  -b /sys \\\n");
            fw.write("  -b /sdcard:/mnt/sdcard \\\n");
            fw.write("  /bin/sh -c \"echo 'Welcome to Alpine Linux!'; exec /bin/sh\"\n");
            fw.close();
            
            Runtime.getRuntime().exec("chmod +x " + scriptPath);
            
        } catch (Exception e) {
            appendToTerminal("Script error: " + e.getMessage() + "\n");
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
                if (assetName.equals("proot")) {
                    destFile.setExecutable(true);
                }
                appendToTerminal("✓ Copied: " + assetName + "\n");
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
