#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Starting Alpine Linux"
echo "========================================="

# الحصول على مسار التطبيق (يتم تمريره من Java)
APP_DIR="$1"
if [ -z "$APP_DIR" ]; then
    APP_DIR="/data/data/com.example.mybasic.activity/files"
fi

PROOT_PATH="$APP_DIR/proot"
ROOTFS_PATH="$APP_DIR/rootfs"

echo "[1/4] Checking files..."
if [ ! -f "$PROOT_PATH" ]; then
    echo "ERROR: proot not found at $PROOT_PATH"
    exit 1
fi

if [ ! -d "$ROOTFS_PATH" ]; then
    echo "ERROR: rootfs not found at $ROOTFS_PATH"
    exit 1
fi

echo "[2/4] Setting permissions..."
chmod 755 "$PROOT_PATH" 2>/dev/null

echo "[3/4] Testing proot..."
if [ -x "$PROOT_PATH" ]; then
    echo "✓ proot is executable"
else
    echo "✗ proot is NOT executable"
    exit 1
fi

echo "[4/4] Starting Alpine Linux..."
echo "========================================="
echo "   Type 'exit' to close"
echo "========================================="

# تشغيل Alpine Linux
exec "$PROOT_PATH" \
  -R "$ROOTFS_PATH" \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /sdcard:/mnt/sdcard \
  /bin/sh -c "echo 'Welcome to Alpine Linux!'; exec /bin/sh"
