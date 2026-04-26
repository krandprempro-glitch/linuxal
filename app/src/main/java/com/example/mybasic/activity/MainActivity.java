package com.example.mybasic.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.termux.view.TerminalView;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

import java.io.File;

public class MainActivity extends AppCompatActivity implements TerminalSessionClient {
    
    private TerminalView terminalView;
    private TerminalSession terminalSession;
    private EditText inputCommand;
    private Handler mainHandler;
    private String currentDir;
    
    // ربط الخدمة
    private boolean isServiceBound = false;
    
    // مستقبل المخرجات من LinuxService
    private BroadcastReceiver outputReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String output = intent.getStringExtra(LinuxService.EXTRA_OUTPUT);
            if (output != null && terminalSession != null) {
                terminalSession.write(output + "\n");
            }
            
            String status = intent.getStringExtra(LinuxService.EXTRA_STATUS);
            if (status != null && terminalSession != null) {
                terminalSession.write("[INFO] " + status + "\n");
            }
            
            String fileContent = intent.getStringExtra(LinuxService.EXTRA_FILE_CONTENT);
            if (fileContent != null) {
                // عرض محتوى الملف في الطرفية
                terminalSession.write("\n" + fileContent + "\n");
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        terminalView = findViewById(R.id.terminal_view);
        inputCommand = findViewById(R.id.input_command);
        ImageButton sendButton = findViewById(R.id.send_button);
        
        currentDir = getFilesDir().getAbsolutePath();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // إعداد المحطة الطرفية
        String[] env = {"TERM=xterm-256color"};
        terminalSession = new TerminalSession(currentDir, "sh", env, this);
        terminalView.attachSession(terminalSession);
        terminalView.setTextSize(14);
        
        // إعدادات الألوان
        terminalView.setTextColor(Color.WHITE);
        terminalView.setBackgroundColor(Color.parseColor("#1E1E1E"));
        terminalView.setCursorColor(Color.parseColor("#FFFFFF"));
        
        // تسجيل مستقبل المخرجات
        IntentFilter filter = new IntentFilter(LinuxService.ACTION_OUTPUT);
        LocalBroadcastManager.getInstance(this).registerReceiver(outputReceiver, filter);
        
        // بدء الخدمة
        Intent serviceIntent = new Intent(this, LinuxService.class);
        startService(serviceIntent);
        
        sendButton.setOnClickListener(v -> executeCommand());
        
        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                executeCommand();
                return true;
            }
            return false;
        });
        
        // رسالة ترحيب
        terminalSession.write("=== LinuxAL Terminal ===\n");
        terminalSession.write("Type 'help' for commands\n");
        terminalSession.write("Linux service starting...\n");
    }
    
    private void executeCommand() {
        String command = inputCommand.getText().toString().trim();
        if (command.isEmpty()) return;
        
        // معالجة الأوامر الخاصة
        if (command.equals("help")) {
            showHelp();
            inputCommand.setText("");
            return;
        }
        
        if (command.equals("status")) {
            terminalSession.write("Checking Linux status...\n");
            // إرسال أمر للتحقق من الحالة
            Intent intent = new Intent(LinuxService.ACTION_COMMAND);
            intent.putExtra(LinuxService.EXTRA_COMMAND, "echo 'Linux is running'");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            inputCommand.setText("");
            return;
        }
        
        if (command.startsWith("edit ")) {
            String filePath = command.substring(5);
            Intent intent = new Intent(MainActivity.this, EditorActivity.class);
            intent.putExtra("file_path", filePath);
            startActivity(intent);
            inputCommand.setText("");
            return;
        }
        
        if (command.equals("edit")) {
            Intent intent = new Intent(MainActivity.this, EditorActivity.class);
            startActivity(intent);
            inputCommand.setText("");
            return;
        }
        
        // إرسال الأمر إلى LinuxService
        Intent intent = new Intent(LinuxService.ACTION_COMMAND);
        intent.putExtra(LinuxService.EXTRA_COMMAND, command);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        
        // عرض الأمر في الطرفية
        terminalSession.write("$ " + command + "\n");
        inputCommand.setText("");
    }
    
    private void showHelp() {
        String help = "\n=== LinuxAL Commands ===\n" +
                      "help           - Show this help\n" +
                      "status         - Check Linux status\n" +
                      "edit [file]    - Open editor\n" +
                      "apk add <pkg>  - Install packages\n" +
                      "gcc, rustc, python3 - Development tools\n" +
                      "================================\n\n";
        terminalSession.write(help);
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
    public void onLogError(String message, Exception e) {
        terminalSession.write("Error: " + message + "\n");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(outputReceiver);
    }
}
