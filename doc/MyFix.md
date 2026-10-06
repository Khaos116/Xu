# Sure-Xu：MyFix 对照核查与待修改清单

> 本文保留旧项目 2026-09-12 的核查记录，版本、渠道、验证结果和待修项均属于当时基线。2026-10-06 已移植到当前 `Xu/my_dev`；当前采用 Normal 双入口单包，差异及本次验证见 [移植记录](Sure-Xu-Migration.md)。旧项目已移除的果园/水印类不在当前代码中，历史链接仅供追溯。

后续 GR 功能同步、原生库移除和分钟配置控件修复见 [GR 同步记录](GR-Sync.md)。本页其余待修问题仍按各节状态跟踪。

核查日期：2026-09-12。当前代码基线：`e398323`。

参考文档：`E:\Work\Sesame-AG\document\MyFix.md`（含第 12 节门禁与每日状态适配）。初次核查只记录问题；后续按用户要求修改 Gemini 默认选项、模型和请求实现，进展见第 4 项。未修改签名文件、设备配置或 Git 暂存区。

参考文档混合了故障修复、个人偏好、设备现场记录和特定上游架构。以下按当前实际行为判断，不以“没有 MyUtils / 文件名不同”直接判定缺陷。文中的行号是本次定位信息，后续应按符号和流程重新定位。

## 一、7 组问题与处理进展

P1：优先处理，涉及敏感资产、正常功能失败或运行边界；P2：异常恢复与重复请求防护。

### 1. P1：签名文件和口令配置已被 Git 跟踪

对应参考：2.5、4.5、6。

**当前证据**：`git ls-files -- app/xqe.jks app/keystore.properties` 返回两个文件。它们当前已进入版本控制；虽然 `.gitignore` 有 `*.jks` 和 `app/keystore.properties`，忽略规则不会使已跟踪文件退出版本控制。

签名功能本身已经接通，之前的私钥/口令检查及 normal、compatible、legacy 三个 Release 签名校验均通过。`app/build.gradle` 读取本地属性，其中 `storeFile=xqe.jks` 会覆盖默认名 `sesame-release.jks`，无需改默认名。

**待修改**：

- 从 Git 索引移除这两个文件，保留本地文件及现有忽略规则；需要示例时仅提交无密码的属性模板。
- 如果含密钥的提交已推送或共享，另行评估历史清理和签名密钥处置；本次没有检查远端传播情况，不宣称已经泄露，也不擅自更换影响覆盖安装的签名。
- 不把任何密码、密钥内容复制到本文、源码、日志或提交说明。

**验收**：`git ls-files` 不再列出两文件，本地文件仍存在，三个 Release 的 `validateSigning*` 继续通过。

### 2. 已修复：业务日期、午夜任务和时间格式化统一为 GMT+8

对应参考：2.1、12。

**原问题**：系统时区为 GMT+7 时，XU 的默认 Calendar/DateFormat 会让午夜重置、签到窗口、日志日期等晚于北京时间一小时。已参考 AG 的 `util/MyUtils.kt#getInstance()` 和 GR 的 `util/MyUtils.java#getInstance()`，将 GMT+8 日历与格式化入口集中到 XU 现有 `TimeUtil`。

| 位置 | 已修改行为 |
|---|---|
| [TimeUtil.java](../app/src/main/java/com/surexu/sesame/util/TimeUtil.java) | `getNow()`、毫秒转日历、今日零点、时间窗口、同日/跨日判断及周数统一 GMT+8；格式化器也显式使用 GMT+8。传入 Calendar 时先按原时区取得绝对时刻，再计算业务日期；时间比较使用传入时间戳的业务日 |
| [ApplicationHook.java](../app/src/main/java/com/surexu/sesame/hook/ApplicationHook.java) | 日期初始化、下一次午夜唤醒、自定义唤醒、跨日更新及闹钟日志使用统一入口 |
| [Statistics.java](../app/src/main/java/com/surexu/sesame/util/Statistics.java)、[Status.java](../app/src/main/java/com/surexu/sesame/util/Status.java)、[MiuixMainActivity.kt](../app/src/main/java/com/surexu/sesame/ui/miuix/MiuixMainActivity.kt) | 保存/刷新统计使用 GMT+8；统计入口对外部 Calendar 也按绝对时刻归一化。保留原时间戳和 TOTAL 累计 |
| [FriendWatch.java](../app/src/main/java/com/surexu/sesame/entity/FriendWatch.java)、[Privilege.java](../app/src/main/java/com/surexu/sesame/model/task/antForest/Privilege.java) | 好友每周刷新、学生签到时段使用 GMT+8；同日判断同时比较年份 |
| [AntFarm.java](../app/src/main/java/com/surexu/sesame/model/task/antFarm/AntFarm.java) | 捐蛋排位、偷榜定时、家庭早安窗口使用 GMT+8；睡觉/起床通过现有 TimeUtil 自动覆盖 |
| [AncientTree.java](../app/src/main/java/com/surexu/sesame/model/task/ancientTree/AncientTree.java)、[KBOrchard.java](../app/src/main/java/com/surexu/sesame/model/task/kbOrchard/KBOrchard.java) | 古树按 GMT+8 的星期枚举判断，不再依赖系统语言的“星期一”字符串；果园签到日期标记使用 GMT+8 |
| [Log.java](../app/src/main/java/com/surexu/sesame/util/Log.java)、[FileUtil.java](../app/src/main/java/com/surexu/sesame/util/FileUtil.java)、[JsonUtil.java](../app/src/main/java/com/surexu/sesame/util/JsonUtil.java) | 日志日期、文件名、今日文件判断/清理、日期解析和 JSON 日期格式化统一 GMT+8；保留线程独立格式化器 |
| [GreenFinance.java](../app/src/main/java/com/surexu/sesame/model/task/greenFinance/GreenFinance.java) | 服务端带 `Z` 或偏移量的 ISO 时间先解析成绝对时刻，再用 GMT+8 判断所属周，避免把 UTC 字面时间误认成本地时间 |

**范围说明**：任务调度、每日状态、签到窗口及日期输出使用北京时间；`System.currentTimeMillis()`、服务端毫秒时间戳、冷却/超时/睡眠时长不加减八小时。不调用 `TimeZone.setDefault()` 修改支付宝宿主进程，不更改设备时区。AG、GR 仅作为参考，未修改它们的文件。

**已验证**：执行 `:app:assembleLegacyRelease` 后运行 [scripts/check_gmt8.py](../scripts/check_gmt8.py)。脚本编译实际 TimeUtil 源码并调用编译后的 Statistics、Status、JsonUtil，在独立 JVM 内依次切换 GMT+7、UTC、GMT+8、洛杉矶时区，验证午夜边界、签到窗口、自定义时刻、午夜闹钟、周一边界、JSON 日期、每日标记、跨年重置及 TOTAL 保留，并检查业务源码没有遗留默认时区日历/格式化入口。无需连接支付宝，不读写用户状态文件；未执行真实 RPC 或挂机任务验证。

### 3. P1：森林收能量忽略请求返回结果，可能解析空响应

对应参考：11.2；同类风险，当前 Java 路径与参考 Kotlin 实现不同。

**当前证据**：

- [AntForestV2.collectEnergy()](../app/src/main/java/com/surexu/sesame/model/task/antForest/AntForestV2.java) 1355 行调用 `ApplicationHook.requestObject(rpcEntity, 0, 0)`，忽略返回值；随后直接对 `rpcEntity.getResponseString()` 构造 `JSONObject`。
- [RpcEntity.java](../app/src/main/java/com/surexu/sesame/entity/RpcEntity.java) 的 `hasResult`/`hasError` 初值为 false，响应初值为 null。
- [NewRpcBridge.requestObject()](../app/src/main/java/com/surexu/sesame/rpc/bridge/NewRpcBridge.java) 在离线、无结果或请求失败时可以返回 null，未必设置 `hasError` 或响应；旧桥接也存在返回 null 的路径。
- 因而“没有 hasError”并不等于“有可解析的响应”。异常被外层 catch 记录后结束本次收取；这不是已证实的整个宿主进程 FATAL 崩溃。

**待修改**：

- 保存并检查请求真实返回值和响应是否存在，统一处理离线、无结果、空白结果，接入现有有上限的重试/结束逻辑。
- 若共享入口引入本地合成 JSON，应让返回值与 `RpcEntity` 响应保持一致，调用方不能继续读取旧值。
- 如提供“空值转空对象”助手，只处理 null/空白；非空非法 JSON 仍应暴露解析错误。不能只用 `{}` 掩盖失败，因为后续必填字段读取仍可能失败。

**验收**：离线、桥接无回调、null/空白响应、非空非法 JSON、正常收取分别验证；失败路径不再进入正常 JSON/能量解析，重试有上限。

### 4. Gemini 请求 JSON 已修复，候选项匹配仍待改进

对应参考：2.4。

**原问题**：`getAnswer()` 会把实际换行传给 `getAnswerStr()`，后者直接拼接 JSON，导致选择题及带引号的题目产生非法 JSON，已在初次核查中复现。

**2026-09-12 已修改**：参考 Sesame-AG，使用 `JSONObject`/`JSONArray` 序列化题目，将默认 AI 改为 Gemini，模型改为 `gemini-2.5-flash`。请求改走 Google 官方接口，以 `x-goog-api-key` 请求头传递用户配置的令牌，禁用重定向；响应使用 try-with-resources 关闭。失败日志仅包含 HTTP 状态或异常类型。密钥未写入源码、本文或测试文件。

**已验证**：[scripts/check_gemini.py](../scripts/check_gemini.py) 编译实际 GeminiAI 类，通过 OkHttp 拦截器验证换行、引号、反斜杠、中文序列化、模型地址、鉴权头、答案解析、HTTP/JSON/网络失败及空令牌、日志不泄露令牌。运行方式：`python scripts/check_gemini.py`，需要 JDK 和脚本列出的现有 Gradle 缓存依赖。官方接口实测 HTTP 200；实际 Java 类使用用户提供的密钥回答 `1+1` 返回 `2`。normal、compatible、legacy 的 Debug Java/Kotlin 编译均通过。

**仍待修改**：[GeminiAI.getAnswer()](../app/src/main/java/com/surexu/sesame/model/normal/answerAI/GeminiAI.java) 仍使用 `contains()` 匹配，包含关系的候选项可能误选；后续应规范化输出并匹配候选项，补充标点、解释和重叠选项验证。本次没有启用联网搜索。

**配置说明**：[AnswerAI.java](../app/src/main/java/com/surexu/sesame/model/normal/answerAI/AnswerAI.java) 的默认类型只影响新建或重置配置。已有通义千问配置需手动切换，并在“AI答”中启用功能、填写 Gemini 令牌。

### 5. P2：缺少按账号、按天的异常 RPC 防重与模块冷却

对应参考：2.2、4.1–4.2、11.4、12。

**当前已有能力**：`Status` 的状态文件按 UID 分目录（`FileUtil.getStatusFile()` 481–486 行）；有每日标记、任务计数，以及带 `taskUid` 的 `flagToday()` 重载。不能说本项目完全没有账号隔离或每日状态。

**缺失的具体行为**：

- [ApplicationHook.java](../app/src/main/java/com/surexu/sesame/hook/ApplicationHook.java) 1559–1616 行的请求重载直接转发到 `RpcBridge`；运行时可选择 New/Old 两个桥接。
- [NewRpcBridge.java](../app/src/main/java/com/surexu/sesame/rpc/bridge/NewRpcBridge.java) 120–217 行主要处理离线和 `error=2000`。回调对 `success`/`isSuccess` 判断的是字段是否存在；存在 `success=false` 时也不会因此设置 `hasError`。
- [OldRpcBridge.java](../app/src/main/java/com/surexu/sesame/rpc/bridge/OldRpcBridge.java) 有登录超时、森林 1004 和“系统繁忙”离线保护，后者会设置全局 offline；这不等价于按 UID、RPC、当天隔离的拦截，而且没有庄园方法豁免。
- 未见统一识别 `error/errorTip=1009`（含数字、字符串、空白形式）、稳定请求摘要、当天本地 `9999` 响应及跨日恢复的实现。
- `RpcEntity` 没有请求所属 UID。桥接的限速等待及响应处理没有固定请求账号，未来每日异常记录不能直接取响应时的当前账号。
- [Status.java](../app/src/main/java/com/surexu/sesame/util/Status.java) 69–87 行读取标记不主动跨日，UID 重载也只检查“仍是当前账号”；不能直接当成参考文档要求的迟到响应归属机制。

**绿色经营**：`GreenFinance.check()/run()` 没有每账号冷却检查。`doTick()` 233–239 行失败只跳出当前类型的项目循环，`behaviorTick()` 仍会继续后续类型，下一轮仍可能请求该失败项目。它已有跳过 `status=Y` 成功项目的逻辑，应保留。

**健康岛**：`AntSports.queryAndProcessBubbleTasks()` 2076–2121 行失败仅返回本轮，没有当天停止标记。另外 `needRetry` 为 true 时直接递归调用自身，没有次数上限；若广告接口报告成功但服务端列表一直保留同一 INIT 状态，可持续请求。

**待修改**：

1. 收敛 string/object 请求到共享的最终发送路径，覆盖 New/Old 两种桥接；拦截逻辑不能只补某个任务。`newAsyncRequest()` 在本次图查询及源码搜索中未见调用，不为恢复历史文件而强行启用它。
2. 请求开始捕获 UID，限速后发送前重新校验；迟到响应记录到原账号。每日存储按账号串行化，跨日清理和新日期写入原子完成，日期使用 GMT+8。
3. 使用稳定摘要识别同一 RPC，保留参数区别，避免不同 `behaviorCode` 互相屏蔽；庄园 `.antfarm.` 不启用全局当日封禁。
4. 绿色经营真实封控触发账号冷却（参考值 30 分钟，可配置）并结束当前轮；当天屏蔽单个项目。冷却后跳过成功及被屏蔽项目，本地 `9999` 不重复触发冷却。
5. 健康岛记录当日异常停止状态；将递归重试改为有上限且检查进展的重试。

**验收**：同 UID 同 RPC 二次拦截、不同 UID/项目互不影响、GMT+8 跨日恢复、庄园豁免、`success=false + 1009`、限速期间切号、迟到响应归属、真实封控与本地 `9999` 区分、冷却后处理剩余项目、气泡状态不变时按上限退出。

### 6. P2：庄园遇到未知任务状态会中断整批处理

对应参考：2.2 的庄园终态/每日重试要求、4.3。

**当前证据**：[AntFarm.listFarmTask()](../app/src/main/java/com/surexu/sesame/model/task/antFarm/AntFarm.java) 1671 行直接 `TaskStatus.valueOf()`，枚举只有 TODO、FINISHED、RECEIVED。其他状态会抛异常，catch 在整个列表循环外，后续任务无法继续处理。

当前没有找到参考实现中“最多 5 次”的对应计数器，因此不能报告为现存 `count > 5` 边界错误，也没有证据说明未知状态会被永久保存为停止状态。确认的问题是**单个未知状态导致本轮剩余任务跳过**。

**待修改**：安全识别状态并仅跳过当前未知项目，保留已完成/已领奖状态的处理；对确需重试的未知或异常状态，按 UID、任务和 GMT+8 日期记录有上限的次数。若沿用 5 次目标，应使用 `count >= 5`，不可凭未知状态重复领取奖励。

**验收**：列表首项未知、次项正常时，正常项目仍被处理；重复异常不无限请求，次日恢复，各账号独立。

### 7. P1：现代入口用包名代替进程名，破坏现有子进程分流

对应参考：7、12 对目标包和主进程边界的要求。

**当前证据**：

- [LibXposedEntry.onPackageReady()](../app/src/modern/java/com/surexu/sesame/hook/LibXposedEntry.java) 64–69 行同时把 `lpparam.packageName` 和 `lpparam.processName` 设置成 `param.getPackageName()`。
- [ApplicationHook.handleLoadPackage()](../app/src/main/java/com/surexu/sesame/hook/ApplicationHook.java) 179–190 行以 `processName` 区分主进程与子进程，子进程应只进入抓包 Hook 分支。

当同一目标包在子进程触发这个现代回调时，该分流判断收到的仍是主包名，无法表达真实进程。这是静态可确认的数据传递问题；尚未证明它在真机造成重复任务。

**待修改**：现代入口获取并传递真实进程名，未知时不要伪装成主进程；保留当前“子进程只装抓包、主进程跑业务”的设计。后续若增加 FPA，只在明确验证的运行时路径上适配，不放宽所有环境的身份检查。

**验收**：normal/compatible 的主进程正常初始化；目标包的 `:xxx` 子进程走既有抓包分支，不启动任务；非目标包保持拒绝。legacy 的原始进程信息传递不能回退。

## 二、定制差异及维护建议：不直接认定为同类故障

| 参考项 | Sure-Xu 当前情况 | 处理建议 |
|---|---|---|
| 2.3 自动接受法律声明 | 在本次检查的 ConfigV2、初始化流程及 Java/Kotlin/XML 文本中未见同一套版本接受门禁 | 不新增一套自动接受逻辑，不删除许可证/关于页声明 |
| 2.4 Gemini 模型与联网搜索 | 已改为官方接口、`gemini-2.5-flash`，严格要求直接回答；没有 `google_search` | 用户密钥实测通过；联网搜索未纳入本次修改 |
| 2.4 Token 日志 | 已用鉴权头替代 URL 中的 key，错误响应不再原样输出，异常只记类型 | 最小检查已覆盖错误响应/异常含令牌时不泄露日志 |
| 2.5 APK 自动归档 | 已参考 AG 为 Legacy Release 配置自动归档，执行 `:app:assembleLegacyRelease` 后复制到 `APK/Release/Sure-Xu-Legacy-<版本>_yyyyMMdd_HHmmss.apk`，时间为北京时间 | 仅在 Legacy Release 成功打包后复制；正常/兼容渠道及 Debug 不归档。运行 `python scripts/check_legacy_apk.py` 可核对归档文件与构建产物的 SHA-256 |
| 2.6 庄园开始/结束图标 | 当前是 Java `run()`，不是参考的 `runSuspend()` | 纯显示偏好，按需要加，不作为任务异常修复 |
| 7、12 FPA 3.8 / API 101 | 当前现代 flavor 的依赖和 `module.prop` 均为 API 102；legacy 单独使用传统 API，保留 `assets/xposed_init` | 不能宣称已满足参考的 FPA 3.8/xp101 方案。若需要 FPA 3.8，应作为单独兼容工作核对 API、入口、远程配置、身份及任务执行；不得全项目降级或删除 legacy 的合法入口。真机结论仍待验证 |
| 8 换行约定 | 无 `.gitattributes`；当前工作区 `gradlew` 为 CRLF，HEAD 中为 LF；Git Bash 的 `bash -n ./gradlew` 本次通过 | 建议明确普通文本 LF、bat/cmd CRLF、脚本 LF、二进制不转换。此处是跨平台检出风险，不能说当前 HEAD 的 Linux 脚本已损坏；换行归一化单独提交 |
| 9 五账号槽位 | 未见 `AccountSlotRegistry`、`MAX_EXECUTABLE_ACCOUNT_SLOTS` 或 `account_slot_full`；当前初始化按 UID 加载配置 | 没有“两账号上限需要改成五”的同一问题。不凭空引入五账号上限，也不宣称已完成五账号真机验收；每日状态切号问题见第一节第 5 项 |
| 10 主页编译时间 | 已在 Miuix 主页面共用的固定顶部，以 12sp 小字在 Sure-Xu 标题最右侧显示 `BuildConfig.VERSION_NAME` 和 `BuildConfig.BUILD_TIME`（北京时间），覆盖首页、日志、配置、设置四个 TAB | 复用现有主题颜色与构建字段，随每次打包更新，文字可自动换行；各 TAB 内容独立于顶部滚动 |
| 11.4 步数范围 | `AntSports.tmpStepCount()` 273–285 行使用配置值至配置值+2000，最多 100000；默认配置 22000。未使用 19853–21423 | 如希望保持参考范围，再集中设置上下限。当前 `boot()`/`run()` 已仅在真实步数低于目标时替换/同步，未发现“真实步数再加随机数”或主动降低高步数的同一问题 |

## 三、不应直接迁移的内容

- **参考 3、5 的历史与冲突记录**：不要恢复注释的 GeminiAI2、无业务作用的 CHANGE_KT 占位、未使用白名单。当前 NewRpcBridge/OldRpcBridge 是真实可选运行路径，不能因为它们在 Sesame-AG 中已淘汰就删除。
- **参考 11.1 的真机配置**：网络异常阈值 8/3、森林并发 3、关闭信用模块/普通捐蛋，来自另一个项目及特定账号，不是当前项目已发生同样服务异常的证据。当前普通捐蛋使用 `donationType` 且默认 ZERO，配置键和排位开关也不同，不能直接复制 JSON。
- **参考 11.3 的 Logcat 结论**：本次没有读取当前设备运行日志，不复用“未发现 FATAL/ANR”或“仍有 1009”的真机结论。
- **参考 12 的测试与 APK 记录**：另一个仓库的 14 项测试、API 101 APK 检查和构建结果不代表 Sure-Xu 已验证。不得照搬它的 Gradle 任务名、包名、配置路径或测试框架。
- 引入新每日状态存储时，先评估现有 Status/RuntimeInfo 的职责和账号生命周期；既不能重复造一整套配置系统，也不能把只针对当前 UID 的单例误当成完整隔离实现。

## 四、本次核查与后续验收

已执行：

- codebase-memory 符号定位、请求链查询和覆盖检查；图索引存在时间元数据变化提示，关键分支另以当前源码复核。构建脚本的已知局部解析缺口使用文件内容核对。
- 对源码/资源文本搜索 GMT+8、每日 RPC、五账号、协议、编译时间和 FPA 相关实现；未分析二进制库内部行为，因此“不适用/未见”均限定于本次检查的源码范围。
- 临时编译当前 TimeUtil 源码，复现 GMT+7/GMT+8 跨日与格式化差异。
- 从 Gemini 请求构造源码提取拼接字面量，复现选择题换行、双引号导致非法 JSON。
- 核对签名文件的 Git 跟踪/忽略状态、gradlew 工作区与 HEAD 换行，Git Bash 语法检查。
- 初次核查仅产出文档；后续 Gemini 修改完成三个 flavor 的 Debug 编译、离线检查及官方接口真实答题验证。Legacy 自动归档修改已通过 `:app:assembleLegacyRelease` 完整打包，`python scripts/check_legacy_apk.py` 确认归档 SHA-256 与原产物一致，`apksigner verify` 验证签名通过。未执行支付宝真实 RPC、任务运行或 FPA 真机测试。

后续按第一节各项分别补最小行为验证。共享代码/构建配置改动应覆盖 normal、compatible、legacy；不能只验证一个 modern flavor。可根据改动使用当前项目任务，例如 `:app:compileNormalDebugKotlin`、`:app:compileCompatibleDebugKotlin`、`:app:compileLegacyDebugKotlin` 与 `assembleRelease`，并执行 `git diff --check`。JUnit/Robolectric 等如需新增，应先确定实际测试入口，不为复制参考文档而引入整套测试依赖。
