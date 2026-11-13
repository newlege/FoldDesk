# FoldDesk Browser

开源 Android 自定义浏览器（WebView），仓库：[https://github.com/newlege/FoldDesk](https://github.com/newlege/FoldDesk)

面向 **Android 平板与折叠屏内屏** 的轻量浏览器 MVP（AndroidX WebView）。核心能力：所有 HTTP(S) 请求默认使用 **桌面版 Chrome User-Agent** 与 Client Hints 元数据，让站点优先返回桌面布局；UI 为全宽 WebView + 触控友好的地址栏，随 `sw600dp` / `sw720dp` 放大间距与控件尺寸。

**非目标（本阶段）**：自编译 Chromium、扩展、账号同步、完整 PC 模式壳。

## 快速开始

### 依赖

- JDK 17+
- Android SDK（API 35；本仓库可在首次构建时通过 `scripts/env.sh` 指向 `android-sdk/` 或本机 `~/Android/Sdk`）

### 构建 Debug APK

```bash
chmod +x scripts/*.sh gradlew
./scripts/build-apk.sh
# 输出: app/build/outputs/apk/debug/app-debug.apk
```

### 单元测试 + UA 校验脚本

```bash
./scripts/run-unit-tests.sh
./scripts/verify-user-agent.sh   # 打印配置的 UA，并用 curl 请求 httpbin.org/headers
```

### 模拟器（多档平板 + 折叠内屏）

详见 [docs/emulator-profiles.md](docs/emulator-profiles.md)。

```bash
./scripts/create-avd.sh --list
./scripts/create-avd.sh tablet_10_1920x1200
./scripts/run-emulator.sh
./scripts/install-and-launch.sh
```

在起始页打开 [httpbin.org/headers](https://httpbin.org/headers)，确认 JSON 里 `User-Agent` 为 Windows 桌面 Chrome（非 Android Mobile）。

### 真机 sideload（平板 / 折叠屏内屏）

1. 开启开发者选项与 USB 调试  
2. `./scripts/build-apk.sh`  
3. `adb install -r app/build/outputs/apk/debug/app-debug.apk`  
4. 启动 **FoldDesk Browser**，用 httpbin 或起始页自检  

Honor Magic V6 等折叠机：建议在内屏展开状态下使用；外屏未专门优化但仍可运行。

### PC 上快速预览（可选）

使用与 APK 相同的 UA 字符串启动 Chrome/Chromium（窗口大小可调，默认 1280×800）：

```bash
./scripts/desktop-preview.sh
FOLDDESK_WIDTH=1920 FOLDDESK_HEIGHT=1200 ./scripts/desktop-preview.sh
```

## 桌面 UA 与 WebView 限制

| 机制 | 作用 |
|------|------|
| `WebSettings.userAgentString` | 替换默认 Android 移动 UA |
| `WebSettingsCompat.setUserAgentMetadata` | Sec-CH-UA* / `navigator.userAgentData`（需系统 WebView 113+） |
| 导航时 `loadUrl(url, headers)` | 附加 `Accept-Language` 等 |

**已知限制**：`navigator.platform` 在 Android WebView 上仍可能为 `Linux armv8l`，无公开 API 改为 `Win32`。依赖 UA / Client Hints 的站点一般可识别为桌面；仅检查 `platform` 的 legacy 脚本可能仍判为 Android。

**与桌面 Chrome 的差距**：Blink 版本随系统 WebView 而变；无扩展；部分 CSS/字体/PDF/插件与 PC Chrome 不同；政务/金融站点可能仍识别 WebView。本应用通过桌面 UA、Client Hints（可用时）、宽 viewport 与 `DesktopViewportHelper` 尽量贴近桌面，**不保证**与 PC Chrome 完全一致。

配置常量见 `app/src/main/java/com/folddesk/browser/DesktopUserAgent.kt`。

## 多标签

- 顶部标签条：点击切换、× 关闭（仅剩一个标签时关闭会回到起始页）
- **+** 新建标签；`target=_blank` / `window.open` 经 `WebChromeClient.onCreateWindow` 打开新标签
- 每个标签独立 WebView，创建时统一应用桌面 UA / WebView 设置

## 布局说明

- **无独立大标题栏**：仅紧凑标签条（约 34dp）+ 单行导航/地址栏（约 40dp 按钮 + 36dp 地址框）+ 系统状态栏 inset
- 平板：`sw600dp` / `sw720dp` 略增大触控尺寸；WebView 占满剩余区域

## 下一步迭代建议

- 更完整的 Client Hints / 请求头对齐桌面 Chrome  
- 平板横屏下的多窗口或侧边栏（非 MVP）  
- 下载、长按菜单、自定义起始页  
- 针对特定站点的 viewport 注入策略  

---

## English (short)

**FoldDesk Browser** is a Kotlin WebView MVP for **Android tablets and foldable inner displays** (7–13" class resolutions plus large square fold screens). It sends a **desktop Chrome User-Agent** by default and scales the chrome UI via `sw600dp` / `sw720dp` buckets.

- Build: `./scripts/build-apk.sh`  
- Tests / UA proof: `./scripts/run-unit-tests.sh` && `./scripts/verify-user-agent.sh`  
- Emulators: `./scripts/create-avd.sh --list` — see [docs/emulator-profiles.md](docs/emulator-profiles.md)  
- Install on device/emulator: `./scripts/install-and-launch.sh`  
- Optional desktop smoke test: `./scripts/desktop-preview.sh`  

License: MIT (see source headers / add `LICENSE` if you fork for release).
