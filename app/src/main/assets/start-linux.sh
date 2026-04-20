#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Arch Linux"
echo "========================================="

APP_DIR="/data/data/com.example.mybasic.activity/files"
PROOT_PATH="$APP_DIR/proot"
ROOTFS_PATH="$APP_DIR/arch-rootfs"
DOWNLOAD_URL="https://github.com/termux/proot-distro/releases/download/v4.34.2/archlinux-aarch64-pd-v4.34.2.tar.xz"
ROOTFS_TAR="$APP_DIR/arch-rootfs.tar.xz"

# التحقق من وجود proot
if [ ! -f "$PROOT_PATH" ]; then
    echo "❌ proot not found!"
    exit 1
fi

# التحقق من وجود rootfs
if [ ! -d "$ROOTFS_PATH" ]; then
    echo "📦 Arch Linux not found. Downloading (~500MB)..."
    
    # تنزيل rootfs
    if command -v curl > /dev/null; then
        curl -L -o "$ROOTFS_TAR" "$DOWNLOAD_URL"
    else
        wget -O "$ROOTFS_TAR" "$DOWNLOAD_URL"
    fi
    
    if [ $? -ne 0 ]; then
        echo "❌ Download failed!"
        exit 1
    fi
    
    echo "📂 Extracting rootfs..."
    mkdir -p "$ROOTFS_PATH"
    tar -xJf "$ROOTFS_TAR" -C "$ROOTFS_PATH"
    
    if [ $? -ne 0 ]; then
        echo "❌ Extraction failed!"
        exit 1
    fi
    
    rm -f "$ROOTFS_TAR"
    echo "✅ Arch Linux installed successfully!"
fi

# إعداد bind mounts
echo "🚀 Starting Arch Linux..."
echo "========================================="

$PROOT_PATH \
  -R "$ROOTFS_PATH" \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /sdcard:/mnt/sdcard \
  -b /storage:/mnt/storage \
  /usr/bin/bash -c "echo 'Welcome to Arch Linux!'; exec /usr/bin/bash"
