# TC-17 多训练计划列表遮挡修复与校验记录

日期：2026-06-03

## 问题

当同一天训练计划较多时，Dashboard 底部的悬浮“新增训练计划”按钮会覆盖最后几张训练计划卡片的操作区，导致部分计划的“开始运动”按钮无法触达。

## 修复

- 在 `WorkoutLogList` 的 `LazyColumn` 中增加底部 `contentPadding = 240.dp`，让列表底部内容可以继续滚动到 FAB 上方，并确保最后一个计划的操作按钮完整避开 FAB。
- 为训练计划列表增加 `workout_log_list` 测试标签。
- 为每个计划卡片的“开始运动”按钮增加 `start_workout_button_{workoutId}` 测试标签，便于回归测试定位最后一个计划。

## 回归测试

- 新增 JVM Compose 回归测试：`FitnessDashboardOverlapRegressionTest`
- 新增设备端 Compose instrumentation 回归测试：`FitnessDashboardOverlapInstrumentedTest`
- 测试场景：使用内存数据库创建 8 个同日 `PLANNED` 训练计划，滚动到最后一个计划的 `start_workout_button_{lastWorkoutId}`，断言按钮可见并点击，最后校验导航回调收到最后一个计划 ID。

## 实际校验结果

| 校验项 | 命令 | 结果 |
| :--- | :--- | :--- |
| 主代码与 JVM 测试编译 | `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin` | 通过 |
| 设备端测试代码编译 | `./gradlew :app:compileDebugAndroidTestKotlin` | 通过 |
| 真机 APK 构建 | `./gradlew :app:assembleDebug` | 通过 |
| 真机安装 | `adb -s 23364e7 install -r app/build/outputs/apk/debug/app-debug.apk` | 通过 |
| 真机手动 UI 验证 | 启动 app，使用已有 7 个同日计划，滚动到底部并点击最后一个“核心 训练计划”的“开始运动” | 通过；最后一个“开始运动”按钮 bounds 为 `[1007,2334][1328,2502]`，FAB bounds 为 `[862,2836][1384,3032]`，两者无重叠；点击后进入“训练中”页面 |
| Robolectric 目标测试执行 | `./gradlew :app:testDebugUnitTest --tests com.example.FitnessDashboardOverlapRegressionTest` | 未完成；环境尝试下载 `org.robolectric:android-all-instrumented:16-robolectric-13921718-i7` 时网络连接超时 |
| 设备 connected 测试执行 | `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.FitnessDashboardOverlapInstrumentedTest` | 未完成；设备在线，但 Gradle connected test 长时间无输出且未生成结果目录，已停止 daemon |

截图证据：

- `/tmp/fitnesscheck-fixed-240-bottom.png`：最后一张计划的“开始运动”按钮完整位于 FAB 上方。
- `/tmp/fitnesscheck-after-start-click.png`：点击最后一个计划后进入“训练中”页面。

结论：代码修复和回归测试代码均已通过编译校验。真机手动 UI 验证通过，确认多训练计划列表底部的“开始运动”不再被 FAB 遮挡且可以启动对应计划。自动化执行仍受当前环境的 Robolectric artifact 下载和 connected test 卡住限制影响。
