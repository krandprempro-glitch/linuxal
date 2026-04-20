#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Using ld-linux"
echo "========================================="

APP_DIR="/data/data/com.example.mybasic.activity/files"
PROOT_PATH="$APP_DIR/proot"
LD_PATH="$APP_DIR/ld-linux-aarch64.so.1"
ROOTFS_PATH="$APP_DIR/arch-rootfs"
DOWNLOAD_URL="https://github.com/termux/proot-distro/releases/download/v4.34.2/archlinux-aarch64-pd-v4.34.2.tar.xz"
ROOTFS_TAR="$APP_DIR/arch-rootfs.tar.xz"

if [ ! -f "$PROOT_PATH" ]; then
    echo "❌ proot not found!"
    exit 1
fi

if [ ! -f "$LD_PATH" ]; then
    echo "❌ ld-linux not found!"
    exit 1
fi

if [ ! -d "$ROOTFS_PATH" ]; then
    echo "📦 Downloading Arch Linux (~500MB)..."
    curl -L -o "$ROOTFS_TAR" "$DOWNLOAD_URL" || wget -O "$ROOTFS_TAR" "$DOWNLOAD_URL"
    
    echo "📂 Extracting..."
    mkdir -p "$ROOTFS_PATH"
    tar -xJf "$ROOTFS_TAR" -C "$ROOTFS_PATH"
    rm -f "$ROOTFS_TAR"
    
    # نسخ ld-linux إلى rootfs
    cp "$LD_PATH" "$ROOTFS_PATH/lib/ld-linux-aarch64.so.1"
    echo "✅ Installation complete!"
fi

echo "🚀 Starting Arch Linux with ld-linux..."
echo "========================================="

# تشغيل Linux باستخدام proot و ld-linux
$PROOT_PATH \
  -R "$ROOTFS_PATH" \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /sdcard:/mnt/sdcard \
  /lib/ld-linux-aarch64.so.1 /bin/bash
