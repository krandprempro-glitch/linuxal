#!/system/bin/sh

# فك الضغط بشكل صحيح
echo "Extracting rootfs..."
rm -rf ./rootfs
mkdir -p ./rootfs
tar -xzf rootfs.tar.gz -C ./rootfs

# إصلاح الأذونات
chmod -R 755 ./rootfs/bin 2>/dev/null
chmod 755 ./rootfs/bin/busybox 2>/dev/null

# تشغيل proot
echo "Starting Alpine Linux..."
./proot -R ./rootfs -b /dev -b /proc -b /sys -b /sdcard:/mnt/sdcard /bin/busybox sh -c "echo 'Welcome to Alpine!'; exec /bin/busybox sh"
