package com.example.mybasic.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.termux.view.TerminalView;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import java.io.*;

public class MainActivity extends AppCompatActivity implements TerminalSessionClient {
    
    private TerminalView terminalView;
    private TerminalSession terminalSession;
    private EditText inputCommand;
    private Handler mainHandler;
    private Process linuxProcess;
    private String currentDir = "/data/data/com.example.mybasic.activity/files";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        terminalView = findViewById(R.id.terminal_view);
        inputCommand = findViewById(R.id.input_command);
        ImageButton sendButton = findViewById(R.id.send_button);
        
        mainHandler = new Handler(Looper.getMainLooper());
        
        // إعداد المحطة الطرفية
        String[] env = {"TERM=xterm-256color", "PATH=" + System.getenv("PATH")};
        terminalSession = new TerminalSession(currentDir, "sh", env, this);
        terminalView.attachSession(terminalSession);
        terminalView.setTextSize(13);
        
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
                
                File rootfsDir = new File(currentDir, "rootfs");
                if (!rootfsDir.exists() || rootfsDir.list().length == 0) {
                    appendToTerminal("Extracting rootfs...\n");
                    extractRootfs();
                }
                
                createStartScript();
                
                String scriptPath = currentDir + "/start-linux.sh";
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", scriptPath);
                pb.directory(new File(currentDir));
                pb.redirectErrorStream(true);
                linuxProcess = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(linuxProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    mainHandler.post(() -> terminalSession.write(output + "\n"));
                }
                
                linuxProcess.waitFor();
                
            } catch (Exception e) {
                appendToTerminal("Error: " + e.getMessage() + "\n");
            }
        }).start();
    }
    
    private void extractRootfs() {
        try {
            String tarPath = currentDir + "/rootfs.tar.gz";
            String destPath = currentDir + "/rootfs";
            
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
            String scriptPath = currentDir + "/start-linux.sh";
            FileWriter fw = new FileWriter(scriptPath);
            fw.write("#!/system/bin/sh\n\n");
            fw.write("echo '========================================='\n");
            fw.write("echo '   LinuxAL - Alpine Linux'\n");
            fw.write("echo '========================================='\n\n");
            fw.write("PROOT_PATH=\"" + currentDir + "/proot\"\n");
            fw.write("ROOTFS_PATH=\"" + currentDir + "/rootfs\"\n\n");
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
            File destFile = new File(currentDir, destName);
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
        
        terminalSession.write(command + "\n");
        inputCommand.setText("");
    }
    
    private void appendToTerminal(String text) {
        mainHandler.post(() -> terminalSession.write(text));
    }
    
    // TerminalSessionClient methods
    @Override
    public void onTextChanged(TerminalSession session) {
        runOnUiThread(() -> terminalView.onScreenUpdated());
    }
    
    @Override
    public void onTitleChanged(TerminalSession session) {}
    
    @Override
    public void onSessionFinished(TerminalSession session) {}
    
    @Override
    public void onCopyTextToClipboard(TerminalSession session, String text) {}
    
    @Override
    public void onPasteTextFromClipboard(TerminalSession session) {}
    
    @Override
    public void onBell() {}
    
    @Override
    public void onColorsChanged(TerminalSession session) {}
    
    @Override
    public void onTerminalCursorStateChange(boolean state) {}
    
    @Override
    public void setTerminalShellPid(int pid) {}
    
    @Override
    public void onLogError(String message, Exception e) {}
    
    @Override
    public void onKeyDown(int keyCode, int[] metaKeys) {}
    
    @Override
    public boolean onKeyUp(int keyCode, int[] metaKeys) { return false; }
    
    @Override
    public boolean readme() { return false; }
    
    @Override
    public boolean shouldWatchOutput() { return true; }
    
    @Override
    public void onSingleTapUp() {}
    
    @Override
    public void onLongPress() {}
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event, TerminalSession session) { return false; }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event, TerminalSession session) { return false; }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (linuxProcess != null) {
            linuxProcess.destroy();
        }
        if (terminalSession != null) {
            terminalSession.finish();
        }
    }
}
