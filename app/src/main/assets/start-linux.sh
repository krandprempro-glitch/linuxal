#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Arch Linux"
echo "========================================="

# سيتم تحديث هذا المسار من Java
PROOT_PATH=""
APP_DIR="/data/data/com.example.mybasic.activity/files"
ROOTFS_PATH="$APP_DIR/arch-rootfs"
DOWNLOAD_URL="https://github.com/termux/proot-distro/releases/download/v4.34.2/archlinux-aarch64-pd-v4.34.2.tar.xz"
ROOTFS_TAR="$APP_DIR/arch-rootfs.tar.xz"

if [ -z "$PROOT_PATH" ]; then
    PROOT_PATH="$APP_DIR/proot"
fi

if [ ! -f "$PROOT_PATH" ]; then
    echo "❌ proot not found at: $PROOT_PATH"
    exit 1
fi

if [ ! -d "$ROOTFS_PATH" ]; then
    echo "📦 Downloading Arch Linux (~500MB)..."
    curl -L -o "$ROOTFS_TAR" "$DOWNLOAD_URL" || wget -O "$ROOTFS_TAR" "$DOWNLOAD_URL"
    echo "📂 Extracting..."
    mkdir -p "$ROOTFS_PATH"
    tar -xJf "$ROOTFS_TAR" -C "$ROOTFS_PATH"
    rm -f "$ROOTFS_TAR"
    echo "✅ Installation complete!"
fi

echo "🚀 Starting Arch Linux..."
echo "========================================="

exec $PROOT_PATH \
  -R "$ROOTFS_PATH" \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /sdcard:/mnt/sdcard \
  /usr/bin/bash
