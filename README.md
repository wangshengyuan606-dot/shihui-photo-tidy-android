# 拾回 / 时光整理 Android Demo

拾回是一个本地优先的智能相册整理 Android Demo。项目从个人系统相册分类不合理、整理不方便的真实问题出发，通过 vibe coding 的方式完成需求拆解、Android 原生开发、真机调试和作品集复盘。

当前工程内 App 名称为“时光整理”，包名为 `com.example.phototidy`。

## 当前定位

这是一个用于作品集展示和真机验证的高保真 Demo，不是生产环境可直接上架的软件。

它重点展示：

- Android 真机可运行的相册整理原型。
- Kotlin + Jetpack Compose 的自定义 UI。
- MediaStore 读取真实系统照片和视频。
- 本地智能搜索、相似照片识别和安全删除流程的 Demo 实现。
- 使用 AI 辅助完成需求分析、开发、调试、文档沉淀的 vibe coding 工作流。

## APK

调试包已放在：

```text
release/photo-tidy-demo-debug.apk
```

说明：

- 这是 Debug APK，仅用于本地体验和作品集演示。
- APK 体积较大，主要因为包含 Android 依赖和 ML Kit 相关能力。
- 上传 GitHub 时建议通过 Git LFS 或 GitHub Release 管理 APK。

## 主要功能

### 首页

- 纸纹背景和照片盲盒视觉。
- 搜索框，可按文件名、相册、日期、类型、标签、地点和内容关键词搜索。
- 相册标签快捷入口。
- 照片盲盒支持刷新、保留、删除到回收站和添加标签。
- 照片概览模块展示相似照片、截图、视频等入口。

### 整理页

- 日期、相册、相似三种整理模式。
- 日期模式按年份和月份组织照片。
- 月份卡片展示照片/视频数量、已整理进度和空间大小。
- 月份详情支持日历视图和照片堆叠视图。
- 相册模式按系统相册 bucket 分组。
- 相似模式通过本地算法识别相似照片组，并用星标提示推荐保留项。

### 大图整理

- 大图浏览照片。
- 支持滑动保留或删除。
- 支持缩略图切换。
- 支持标签归档。
- 支持不同手势习惯设置。

### 回收站

- 删除后的媒体先进入 App 内回收站。
- 支持恢复。
- 支持选择态。
- 支持彻底删除前二次确认。

### 压缩页

- 支持从选择态进入压缩页。
- 展示待压缩媒体。
- 提供低 / 中 / 高质量选项。
- 当前完成 UI 和流程演示，未做真实编码压缩。

### 我的页

- 回收站入口。
- 主题卡片。
- 手势习惯设置。
- 每组照片数量设置。
- 权限状态展示和选择更多入口。
- 关于 App。

## 技术栈

```text
Kotlin
Jetpack Compose
Material 3
ViewModel + StateFlow
MediaStore
Coil Compose
Google ML Kit Image Labeling
Google ML Kit Face Detection
ExifInterface + Geocoder
SharedPreferences + JSON Cache
Gradle Wrapper
```

## 本地智能能力

当前所有智能能力都在本机执行：

- ML Kit Image Labeling：生成风景、宠物、猫、狗、食物、建筑等内容标签。
- ML Kit Face Detection：识别人脸数量，用于人物、人像、自拍、合照等搜索词。
- EXIF GPS + Geocoder：提取照片地点信息并加入搜索索引。
- dHash + 汉明距离：识别相似照片组。
- SharedPreferences + JSON：缓存搜索索引，减少重复分析。

## 当前缺陷与边界

### 删除边界

- 当前删除到回收站是 App 内状态变化。
- “彻底删除”当前从 App 可见状态中移除，不等于真实删除系统相册原文件。
- 项目中已预留 `SystemMediaDeleteHelper`，后续可接入 Android `MediaStore.createDeleteRequest`。

### 压缩边界

- 当前压缩页完成选择流程和 UI。
- 暂未实现真实图片重编码。
- 暂未实现视频压缩。

### 状态持久化边界

- 整理状态、保留状态、回收站状态主要保存在 ViewModel 内存中。
- App 重启后的完整整理记录持久化尚未接入 Room。
- 搜索索引、地点索引和已命名人物结构已有本地缓存。

### 人物能力边界

- 当前可检测“有人脸”。
- 尚未实现同一人物的人脸向量聚类。
- 尚未完成完整人物命名和人物相册功能。

### 性能边界

- Demo 阶段默认最多读取最近 500 项媒体。
- ML Kit 内容分析和地点分析都有批量限制。
- 大型相册下仍需进一步做分页、增量扫描和后台任务优化。

### 产品边界

- 不包含账号登录。
- 不包含云同步。
- 不包含会员付费。
- 不包含生产级隐私政策和上架流程。
- 不保证适配所有 Android 厂商系统的媒体删除行为。

## 构建

Windows PowerShell:

```powershell
$env:JAVA_HOME='E:\AndroidStudio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-daemon --console=plain "-Dkotlin.compiler.execution.strategy=in-process"
```

APK 输出：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装到真机

```powershell
$adb='C:\Users\Administrator\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb install -r 'app/build/outputs/apk/debug/app-debug.apk'
& $adb shell monkey -p com.example.phototidy -c android.intent.category.LAUNCHER 1
```

## 截图说明

本地开发过程中使用了真机截图验证页面，但截图包含真实相册内容和个人照片，因此不随公开仓库上传。作品集展示时建议使用经过脱敏处理的截图或重新制作的演示数据截图。

## 文档

- [技术路线](docs/技术路线.md)
- [项目交接说明](docs/项目交接说明.md)
- [当前状态与边界](docs/CURRENT_STATUS.md)
- [第三方引用说明](docs/THIRD_PARTY_NOTICES.md)

## 第三方说明

项目参考和改写了少量 MIT 许可项目的实现思路，具体见 [THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md)。

当前仓库未附带正式开源许可证；如需公开复用，请先补充 LICENSE。
