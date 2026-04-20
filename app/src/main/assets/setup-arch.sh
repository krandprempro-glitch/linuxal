#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Arch Linux Setup"
echo "========================================="

# إعداد المسارات
ARCH_ROOT="/data/data/com.example.mybasic.activity/files/arch-rootfs"
BIN_PATH="/data/data/com.example.mybasic.activity/files"

echo "[1/4] Creating directories..."
mkdir -p $ARCH_ROOT

echo "[2/4] Downloading Arch Linux ARM64..."
if [ ! -f "$BIN_PATH/arch-rootfs.tar.gz" ]; then
    wget -O $BIN_PATH/arch-rootfs.tar.gz https://github.com/termux/proot-distro/releases/download/v4.10.0/archlinux-aarch64-rootfs.tar.xz
fi

echo "[3/4] Extracting rootfs..."
tar -xf $BIN_PATH/arch-rootfs.tar.gz -C $ARCH_ROOT

echo "[4/4] Setting up proot..."
cp $BIN_PATH/proot $ARCH_ROOT/usr/bin/
chmod 755 $ARCH_ROOT/usr/bin/proot

echo "========================================="
echo "Setup completed successfully!"
echo "========================================="
