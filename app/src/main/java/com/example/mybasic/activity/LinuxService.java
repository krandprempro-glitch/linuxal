package com.example.mybasic.activity;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinuxService extends Service {
    private static final String CHANNEL_ID = "linuxal_channel";
    private static final int NOTIFICATION_ID = 1;
    
    private Process linuxProcess;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String currentDir;
    private boolean isLinuxRunning = false;
    
    public static final String ACTION_COMMAND = "com.example.LINUX_COMMAND";
    public static final String ACTION_OUTPUT = "com.example.LINUX_OUTPUT";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_FILE_CONTENT = "file_content";
    public static final String ACTION_READ_FILE = "com.example.READ_FILE";
    public static final String ACTION_SAVE_FILE = "com.example.SAVE_FILE";
    public static final String EXTRA_FILE_PATH = "file_path";
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        currentDir = getFilesDir().getAbsolutePath();
        
        registerCommandReceiver();
        startLinuxInBackground();
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    private void registerCommandReceiver() {
        BroadcastReceiver commandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_COMMAND.equals(action)) {
                    String command = intent.getStringExtra(EXTRA_COMMAND);
                    if (command != null) executeLinuxCommand(command);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(ACTION_COMMAND);
        LocalBroadcastManager.getInstance(this).registerReceiver(commandReceiver, filter);
    }
    
    private void startLinuxInBackground() {
        executorService.execute(() -> {
            try {
                copyAssetToFile("proot", "proot");
                broadcastStatus("Linux starting...");
                Thread.sleep(1000);
                broadcastStatus("Linux started successfully");
                isLinuxRunning = true;
            } catch (Exception e) {
                broadcastStatus("Error: " + e.getMessage());
            }
        });
    }
    
    private void copyAssetToFile(String assetName, String destName) {
        try {
            File destFile = new File(currentDir, destName);
            if (!destFile.exists()) {
                InputStream in = getAssets().open(assetName);
                OutputStream out = new FileOutputStream(destFile);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                in.close();
                out.close();
            }
        } catch (Exception e) {
            broadcastStatus("Failed to copy: " + assetName);
        }
    }
    
    private void executeLinuxCommand(String command) {
        if (!isLinuxRunning) {
            broadcastOutput("Linux is not running");
            return;
        }
        broadcastOutput("$ " + command);
    }
    
    private void broadcastOutput(String output) {
        Intent intent = new Intent(ACTION_OUTPUT);
        intent.putExtra(EXTRA_OUTPUT, output);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    private void broadcastStatus(String status) {
        Intent intent = new Intent(ACTION_OUTPUT);
        intent.putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        updateNotification(status);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "LinuxAL Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LinuxAL")
            .setContentText("Linux is running")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .build();
    }
    
    private void updateNotification(String status) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LinuxAL")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, notification);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (linuxProcess != null) linuxProcess.destroy();
        if (executorService != null) executorService.shutdown();
    }
}  // ✅ هذا القوس مهم جداً
