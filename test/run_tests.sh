#!/bin/bash
# WiFiSwitcher 测试脚本

echo "=== WiFiSwitcher 测试 ==="

# 检查设备
echo "检查连接的设备..."
adb devices

# 构建APK
echo "构建手机版..."
./gradlew assembleMobileDebug

echo "构建电视版..."
./gradlew assembleTvDebug

# 安装测试
echo "安装手机版..."
adb install -r app/build/outputs/apk/mobile/debug/app-mobile-debug.apk

echo "启动应用..."
adb shell am start -n com.mithrandiryee.wifiswitcher.mobile/.MainActivity

echo "=== 测试完成 ==="