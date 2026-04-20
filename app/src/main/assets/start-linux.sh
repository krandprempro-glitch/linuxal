#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Starting Linux"
echo "========================================="

# إعداد المسارات
PROOT_PATH="/data/data/com.termux/files/usr/assets/proot"
ROOTFS_PATH="/data/data/com.termux/files/usr/assets/rootfs"

# التحقق من وجود الملفات
if [ ! -f "$PROOT_PATH" ]; then
    PROOT_PATH="/data/data/com.example.mybasic.activity/files/proot"
fi

if [ ! -d "$ROOTFS_PATH" ]; then
    ROOTFS_PATH="/data/data/com.example.mybasic.activity/files/rootfs"
fi

echo "[1/3] Checking proot..."
if [ -f "$PROOT_PATH" ]; then
    chmod +x $PROOT_PATH
    echo "✓ proot found at: $PROOT_PATH"
else
    echo "✗ proot not found!"
    exit 1
fi

echo "[2/3] Checking rootfs..."
if [ -d "$ROOTFS_PATH" ]; then
    echo "✓ rootfs found at: $ROOTFS_PATH"
else
    echo "✗ rootfs not found!"
    exit 1
fi

echo "[3/3] Starting Ubuntu..."
echo "========================================="

# تشغيل Linux باستخدام proot
$PROOT_PATH \
  -R $ROOTFS_PATH \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /data/data/com.example.mybasic.activity/files/home:/root \
  /bin/bash -c "echo 'Welcome to Ubuntu on Android!'; exec /bin/bash"
