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
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LinuxService extends Service {
    private static final String CHANNEL_ID = "linuxal_channel";
    private static final int NOTIFICATION_ID = 1;
    
    // عمليات Linux
    private Process linuxProcess;
    private ExecutorService executorService;
    private Handler mainHandler;
    private String currentDir;
    
    // حالة التشغيل
    private boolean isLinuxRunning = false;
    private String lastCommandOutput = "";
    
    // أوامر Broadcast
    public static final String ACTION_COMMAND = "com.example.LINUX_COMMAND";
    public static final String ACTION_OUTPUT = "com.example.LINUX_OUTPUT";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_STATUS = "status";
    
    // أوامر المحرر
    public static final String ACTION_SAVE_FILE = "com.example.SAVE_FILE";
    public static final String ACTION_READ_FILE = "com.example.READ_FILE";
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_FILE_CONTENT = "file_content";
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        currentDir = getFilesDir().getAbsolutePath();
        
        // تسجيل مستقبل الأوامر
        registerCommandReceiver();
        
        // بدء Linux في الخلفية
        startLinuxInBackground();
        
        // بدء الخدمة كـ Foreground
        startForeground(NOTIFICATION_ID, createNotification());
    }
    
    private void registerCommandReceiver() {
        BroadcastReceiver commandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (ACTION_COMMAND.equals(action)) {
                    String command = intent.getStringExtra(EXTRA_COMMAND);
                    if (command != null) {
                        executeLinuxCommand(command);
                    }
                } else if (ACTION_SAVE_FILE.equals(action)) {
                    String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
                    String content = intent.getStringExtra(EXTRA_FILE_CONTENT);
                    saveFile(filePath, content);
                } else if (ACTION_READ_FILE.equals(action)) {
                    String filePath = intent.getStringExtra(EXTRA_FILE_PATH);
                    readFile(filePath);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_COMMAND);
        filter.addAction(ACTION_SAVE_FILE);
        filter.addAction(ACTION_READ_FILE);
        LocalBroadcastManager.getInstance(this).registerReceiver(commandReceiver, filter);
    }
    
    private void startLinuxInBackground() {
        executorService.execute(() -> {
            try {
                // نسخ الملفات المطلوبة
                copyAssetToFile("proot", "proot");
                copyAssetToFile("rootfs.tar.gz", "rootfs.tar.gz");
                
                // استخراج rootfs إذا لم يكن موجوداً
                File rootfsDir = new File(currentDir, "rootfs");
                if (!rootfsDir.exists() || (rootfsDir.list() != null && rootfsDir.list().length == 0)) {
                    extractRootfs();
                }
                
                // إنشاء سكريبت التشغيل المطور
                createAdvancedStartScript();
                
                String scriptPath = currentDir + "/start-linux.sh";
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", scriptPath);
                pb.directory(new File(currentDir));
                pb.redirectErrorStream(true);
                
                linuxProcess = pb.start();
                isLinuxRunning = true;
                
                // إعلام الواجهة بأن Linux بدأ
                broadcastStatus("Linux started successfully");
                
                // قراءة مخرجات Linux وإرسالها للواجهة
                BufferedReader reader = new BufferedReader(new InputStreamReader(linuxProcess.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    mainHandler.post(() -> broadcastOutput(output));
                }
                
                linuxProcess.waitFor();
                isLinuxRunning = false;
                broadcastStatus("Linux stopped");
                
            } catch (Exception e) {
                broadcastStatus("Error: " + e.getMessage());
            }
        });
    }
    
    private void createAdvancedStartScript() {
        try {
            String scriptPath = currentDir + "/start-linux.sh";
            FileWriter fw = new FileWriter(scriptPath);
            fw.write("#!/system/bin/sh\n\n");
            fw.write("# =========================================\n");
            fw.write("#   LinuxAL - Alpine Linux with Dev Tools\n");
            fw.write("# =========================================\n\n");
            
            fw.write("echo '🐧 Starting Alpine Linux...'\n");
            fw.write("echo '📦 Loading development environment...'\n\n");
            
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
            fw.write("echo '✅ Environment ready!'\n");
            fw.write("echo '========================================='\n\n");
            
            fw.write("# إنشاء مجلد للمشاريع\n");
            fw.write("mkdir -p $ROOTFS_PATH/home/developer/projects\n");
            fw.write("mkdir -p $ROOTFS_PATH/home/developer/.local/bin\n\n");
            
            fw.write("# إعداد متغيرات البيئة\n");
            fw.write("export PATH=\"/usr/local/bin:/usr/bin:/bin:$PATH\"\n");
            fw.write("export HOME=/home/developer\n");
            fw.write("export EDITOR=nano\n\n");
            
            fw.write("# إنشاء سكريبت مساعد للمحرر\n");
            fw.write("cat > $ROOTFS_PATH/home/developer/.local/bin/editor << 'EOF'\n");
            fw.write("#!/bin/sh\n");
            fw.write("# فتح المحرر من داخل Linux\n");
            fw.write("echo \"📝 Opening editor for: $1\"\n");
            fw.write("# سيتم إرسال أمر لفتح المحرر في Android\n");
            fw.write("exit 0\n");
            fw.write("EOF\n");
            fw.write("chmod +x $ROOTFS_PATH/home/developer/.local/bin/editor\n\n");
            
            fw.write("echo '========================================='\n");
            fw.write("echo '💻 Welcome to Alpine Linux!'\n");
            fw.write("echo '📁 Projects: /home/developer/projects'\n");
            fw.write("echo '🔧 Commands: apk add, gcc, rustc, python3'\n");
            fw.write("echo '========================================='\n\n");
            
            fw.write("exec $PROOT_PATH \\\n");
            fw.write("  -R $ROOTFS_PATH \\\n");
            fw.write("  -b /dev \\\n");
            fw.write("  -b /proc \\\n");
            fw.write("  -b /sys \\\n");
            fw.write("  -b /sdcard:/mnt/sdcard \\\n");
            fw.write("  -b /storage:/mnt/storage \\\n");
            fw.write("  /bin/sh -c \"cd /home/developer && exec /bin/sh\"\n");
            
            fw.close();
            
            Runtime.getRuntime().exec("chmod +x " + scriptPath);
            
        } catch (Exception e) {
            broadcastStatus("Script error: " + e.getMessage());
        }
    }
    
    private void extractRootfs() {
        try {
            String tarPath = currentDir + "/rootfs.tar.gz";
            String destPath = currentDir + "/rootfs";
            
            broadcastStatus("📦 Extracting rootfs...");
            
            Process process = Runtime.getRuntime().exec(new String[]{
                "/system/bin/sh", "-c", 
                "mkdir -p " + destPath + " && cd " + destPath + " && tar -xzf " + tarPath + " --warning=no-timestamp"
            });
            process.waitFor();
            
            broadcastStatus("✅ Rootfs extracted successfully");
            
        } catch (Exception e) {
            broadcastStatus("Extract error: " + e.getMessage());
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
            }
        } catch (Exception e) {
            broadcastStatus("Failed to copy: " + assetName);
        }
    }
    
    private void executeLinuxCommand(String command) {
        if (!isLinuxRunning || linuxProcess == null) {
            broadcastOutput("❌ Linux is not running");
            return;
        }
        
        executorService.execute(() -> {
            try {
                OutputStream os = linuxProcess.getOutputStream();
                os.write((command + "\n").getBytes());
                os.flush();
                broadcastOutput("$ " + command);
            } catch (Exception e) {
                broadcastOutput("Command error: " + e.getMessage());
            }
        });
    }
    
    private void saveFile(String filePath, String content) {
        executorService.execute(() -> {
            try {
                File file = new File(filePath);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                
                FileWriter fw = new FileWriter(file);
                fw.write(content);
                fw.close();
                
                broadcastStatus("💾 File saved: " + file.getName());
            } catch (Exception e) {
                broadcastStatus("Save error: " + e.getMessage());
            }
        });
    }
    
    private void readFile(String filePath) {
        executorService.execute(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    broadcastStatus("File not found: " + filePath);
                    return;
                }
                
                StringBuilder content = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file)));
                String line;
                while ((line = br.readLine()) != null) {
                    content.append(line).append("\n");
                }
                br.close();
                
                // إرسال المحتوى إلى الواجهة
                Intent intent = new Intent(ACTION_OUTPUT);
                intent.putExtra(EXTRA_FILE_CONTENT, content.toString());
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                
            } catch (Exception e) {
                broadcastStatus("Read error: " + e.getMessage());
            }
        });
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
        
        // تحديث الإشعار
        updateNotification(status);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "LinuxAL Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("LinuxAL is running in background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LinuxAL")
            .setContentText("Linux is running in background")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
    
    private void updateNotification(String status) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LinuxAL")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
        
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
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
        if (linuxProcess != null) {
            linuxProcess.destroy();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
                    
