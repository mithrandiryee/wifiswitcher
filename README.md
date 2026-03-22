# WiFiSwitcher - WiFi IP地址切换器

一款适用于Android手机和电视的应用,用于快速切换预设WiFi网络的IP地址配置。

## 功能特性

- ✅ **多配置管理** - 保存多套IP配置方案
- ✅ **快速切换** - 一键切换不同的IP配置
- ✅ **DHCP/静态IP** - 支持DHCP和静态IP配置
- ✅ **手机&电视** - 适配手机和Android TV界面
- ✅ **配置导入导出** - 备份和恢复配置

## 界面预览

### 主界面
- 当前WiFi状态显示
- 已保存的IP配置列表
- 一键切换IP配置

### 添加配置
- 配置名称
- WiFi SSID选择
- DHCP开关
- 静态IP配置(IP、子网掩码、网关、DNS)

## 使用场景

1. **家庭网络** - 为不同房间的WiFi设置固定IP
2. **办公环境** - 切换不同办公室的网络配置
3. **开发测试** - 快速切换测试环境IP
4. **智能家居** - 为智能设备分配固定IP

## 技术架构

```
com.mithrandiryee.wifiswitcher
├── MainActivity.kt           # 主界面
├── AddProfileActivity.kt     # 添加/编辑配置
├── IPProfile.kt              # 数据模型
├── IPProfileAdapter.kt       # 列表适配器
├── WifiController.kt         # WiFi控制核心
├── IPConfigurator.kt         # IP配置器
├── ProfileStorageManager.kt  # 配置存储管理
├── WiFiProfileManager.kt     # WiFi配置管理
└── NetworkStateReceiver.kt   # 网络状态监听
```

## 权限说明

| 权限 | 用途 |
|------|------|
| ACCESS_WIFI_STATE | 读取WiFi状态 |
| CHANGE_WIFI_STATE | 修改WiFi配置 |
| ACCESS_NETWORK_STATE | 读取网络状态 |
| CHANGE_NETWORK_STATE | 修改网络配置 |
| ACCESS_FINE_LOCATION | Android 10+ WiFi扫描需要 |
| INTERNET | 网络连接测试 |

## 构建要求

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34
- 最低支持: Android 7.0 (API 24)

## 构建步骤

```bash
# 克隆项目
git clone https://github.com/mithrandiryee/wifiswitcher.git

# 用Android Studio打开项目

# 或命令行构建
cd wifiswitcher
./gradlew assembleDebug

# APK位置
app/build/outputs/apk/debug/app-debug.apk
```

## 兼容性

| Android版本 | 状态 | 备注 |
|------------|------|------|
| Android 14 (API 34) | ✅ 完全支持 | 需要附近设备权限 |
| Android 13 (API 33) | ✅ 完全支持 | 需要附近设备权限 |
| Android 12 (API 31) | ✅ 完全支持 | - |
| Android 11 (API 30) | ✅ 支持 | 需要位置权限 |
| Android 10 (API 29) | ✅ 支持 | 需要位置权限 |
| Android 9 (API 28) | ✅ 完全支持 | - |
| Android 8 (API 26) | ✅ 完全支持 | - |
| Android 7 (API 24) | ✅ 完全支持 | - |

## 已知限制

- Android 10+ 需要位置权限才能修改WiFi配置
- 部分厂商定制系统可能需要额外权限
- 无法修改当前已连接网络的IP(需先断开)

## 开发团队

- **架构设计** - 项目结构、数据模型
- **前端UI** - 界面布局、交互设计
- **后端核心** - WiFi/IP切换逻辑
- **测试** - 功能验证

## 许可证

MIT License

## 更新日志

### v1.0.0 (2026-03-21)
- 初始版本发布
- 支持静态IP配置切换
- 支持DHCP切换
- 适配手机和电视界面
