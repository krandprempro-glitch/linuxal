#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Alpine Linux"
echo "========================================="

APP_DIR="/data/data/com.example.mybasic.activity/files"
PROOT_PATH="$APP_DIR/proot"
ROOTFS_PATH="$APP_DIR/rootfs"

# التحقق من وجود proot
if [ ! -f "$PROOT_PATH" ]; then
    echo "❌ proot not found!"
    exit 1
fi

# التحقق من وجود rootfs
if [ ! -d "$ROOTFS_PATH" ]; then
    echo "📦 Extracting Alpine Linux..."
    mkdir -p "$ROOTFS_PATH"
    cd "$APP_DIR"
    tar -xzf rootfs.tar.gz -C "$ROOTFS_PATH"
    echo "✅ Extraction complete!"
fi

echo "🚀 Starting Alpine Linux..."
echo "========================================="

# تشغيل Alpine Linux مباشرة
$PROOT_PATH \
  -R "$ROOTFS_PATH" \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /sdcard:/mnt/sdcard \
  /bin/sh -c "echo 'Welcome to Alpine Linux!'; exec /bin/sh"
