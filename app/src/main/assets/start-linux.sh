#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Arch Linux"
echo "========================================="

APP_DIR="/data/data/com.example.mybasic.activity/files"
PROOT_PATH="$APP_DIR/proot"
ROOTFS_PATH="$APP_DIR/arch-rootfs"

# تشغيل Arch Linux
$PROOT_PATH \
  -R $ROOTFS_PATH \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /sdcard:/mnt/sdcard \
  /usr/bin/bash -c "echo 'Welcome to Arch Linux!'; exec /usr/bin/bash"
