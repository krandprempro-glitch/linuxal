#!/system/bin/sh

echo "========================================="
echo "   LinuxAL - Starting Linux"
echo "========================================="

# إعداد المسارات
PROOT_PATH="/data/data/com.termux/files/usr/assets/proot"
ROOTFS_PATH="/data/data/com.termux/files/usr/assets/rootfs"

# إعطاء صلاحية التنفيذ
chmod +x $PROOT_PATH

# تشغيل Linux باستخدام proot
$PROOT_PATH \
  -R $ROOTFS_PATH \
  -b /dev \
  -b /proc \
  -b /sys \
  -b /data/data/com.termux/files/home:/root \
  /bin/bash
