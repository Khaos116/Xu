# Sure-Xu/my_main → Xu/my_dev 移植记录

日期：2026-10-06。

- 来源：`E:/Work/Sure-Xu`，`my_main`，提交 `01f2fc6262be44897be4291a9981674a4f41eb9b`。
- 来源上游基线：`37a9f6b`；`my_main` 相对此基线只有一条独立功能提交，工作区没有未提交源码修改。
- 目标：`E:/Work/Xu`，`my_dev`，移植前提交 `5bc5c84`（作者 v3.2.4）。
- 回退参考：`backup/my_dev-before-sure-xu-migration-20261006` 指向移植前提交。
- 旧项目源码、分支和远端配置保持原样；本次只在当前项目提交，不推送远端。

## 同步内容

| 内容 | 当前项目的落点 |
|---|---|
| 福气鱼塘 | `FishTask`、`FishConfig`、鱼塘任务名单、模型注册与独立执行时间；任务与钓鱼默认关闭 |
| 家庭任务 | 可选家庭装饰兑换、`queryRecentFarmFood` 与美食份数检查；兑换默认不勾选 |
| 庄园任务 | 通用任务接入 `FarmTaskPolicy`；保留新版视频/答题分派、游戏任务、轮盘与爱心鸡结号逻辑 |
| Gemini | 恢复 Gemini 2.5 Flash 的官方请求、JSON 参数、超时、响应释放和日志脱敏，增加可选 Google 搜索 |
| AI 接入 | 保留新版自定义 AI、测试按钮与题库纠错；AI 类型默认仍选自定义接口，旧 Gemini 配置键保留 |
| 业务时间 | 统一 GMT+8 日历、日志、统计、状态、任务窗口与日期解析；新版会员秒杀、种花及森林日期入口也复用此工具 |
| 首页 | 显示公共标题、版本与北京时间编译信息，保留新版激活状态、统计刷新和桌宠 |
| 体积优化 | 图片改 WebP，移除自带 sesame SO 与未接入的 C++ 水印文件，迁入精简 R8 反射规则并补充新版 Hook/兼容层保留 |
| 本地构建 | 保留 Normal 双入口单包与资源裁剪；旧 Legacy APK 时间戳归档适配为 Normal 归档 |
| 文档 | [GR-Sync.md](GR-Sync.md)、[MyFix.md](MyFix.md) 完整迁入并加历史基线说明，README 增加入口和检查命令 |
| 检查脚本 | 五份旧脚本均迁入；适配 Normal、Jackson 2.18.2、当前统计结构和双入口，并增加实际混淆 DEX 的 AI 路由检查 |

## 上游重写部分的处理

- 当前配置编辑器已经使用 `configValue` / `setConfigValue`，保留新版控件即可覆盖原分钟倍率修复。
- 旧项目的果园模块、Java 水印类和 JNI 加载/Toast 拦截链已被当前上游移除，不恢复失去依赖和调用入口的旧类。
- 当前统计仅有日/月/年，保留该结构；旧脚本中的 TOTAL 断言相应移除。保留午夜和跨年重置检查。
- 旧文档中三渠道、旧 APK、待修项和验证结果属于 2026-09-12，不能当作当前分支的验证结果。
- 旧 `app/keystore.properties` 与 `app/xqe.jks` 已同步；用户确认它们是公开签名，按要求加入 Git。其他本地签名密钥仍保持忽略。
- 移植前已有的 `.serena/` 和 `gradle/gradle-daemon-jvm.properties` 不纳入提交。

## 验证

| 检查 | 本次结果 |
|---|---|
| `:app:assembleNormalRelease` | 成功；包含 Java/Kotlin 编译、R8、资源裁剪、lintVital 与签名验证 |
| `scripts/check_gemini.py` | 离线通过：请求 JSON、模型/认证、搜索工具、答案解析、失败路径、空令牌和日志脱敏 |
| `scripts/check_gmt8.py` | GMT+7、UTC、GMT+8、洛杉矶四种宿主时区通过：日期、窗口、午夜闹钟、周数、JSON、日状态与跨年重置 |
| `scripts/check_gr_sync.py` | 离线通过：庄园策略、鱼塘配置/时区/响应/令牌/黑名单/切号/中断与家庭兑换 |
| `scripts/check_legacy_apk.py` | Normal 归档与构建产物 SHA-256 一致 |
| `scripts/check_r8_apk.py --serial emulator-5556` | 实际混淆 DEX 通过：模型构造、全模型配置往返、自定义/Gemini 路由、Jackson 泛型、状态与 RPC 数据 |
| `apksigner verify --verbose` | APK 校验通过，v2 签名有效 |
| 完整性 | 原功能源码与 WebP 内容一致；两份原文保留；旧项目工作区无改动，公开签名文件内容相同并加入版本控制；验证 APK 的签名证书与旧 xqe.jks 一致 |

验证产物：`APK/Release/Sure-Xu-Normal-1.3.0_20261006_125804.apk`，2.70 MiB。版本名沿用项目未传 `-Pversion` 时的默认值；这是本地验证产物，并非 v1.3.0 上游源码或新的正式发布。

APK 保留 Normal 的 modern/legacy 双入口，不包含 `libsesame.so`、`libwatermark.so` 或旧 LibraryUtil；第三方依赖的 `libandroidx.graphics.path.so` 仍保留。R8 映射文件位于 `app/build/outputs/mapping/normalRelease/mapping.txt`，发布或留存 APK 时需要一起保存。

本机原有 daemon JVM 配置指定 JDK 25，与现有 Lombok 1.18.32 的注解处理不兼容。验证时临时使用已安装的 JDK 17，并在构建结束后恢复原文件。系统虚拟内存不足时改用单 worker、1 GiB 堆和进程内 Kotlin 编译；没有升级依赖或改动设备时区。

离线检查和混淆 DEX 检查不执行真实支付宝任务、兑换或 AI 联网请求；服务端行为仍需实际使用验证。
