# FitnessCheck Bug 修复记录

本记录汇总了针对 `full_test_report.md` 中发现的缺陷所进行的修复及验证过程。

---

### 1. TC-13: 历史数据首组填充失败
*   **问题描述**：新建动作时，第一组数据未自动填充上一次训练的重量，始终为默认值。
*   **原因分析**：原 `WorkoutDao.getLastSetForExercise` 仅按 `id DESC` 排序，无法保证跨训练会话获取到的是“最近日期”的记录。
*   **修复方案**：修改 SQL 查询，关联 `workouts` 表并按 `workouts.date DESC` 排序。
*   **修复效果**：**通过**。验证发现新建动作时成功填充了上一次训练的重量（27.5kg）。

### 2. TC-05: 动作分类筛选响应性问题
*   **问题描述**：分类芯片（FilterChip）偶尔点击无响应，列表未过滤。
*   **修复方案**：
    1. 将 `FilterChip` 替换为 `ElevatedFilterChip` 以增强视觉和点击反馈。
    2. 移除了 `onClick` 中的 `!isSelected` 冗余判断。
    3. 优化了 Chip 的内边距和字体权重，确保更大的点击热区。
*   **当前状态**：**通过**。经过 10 次以上连续快速点击测试，未再出现无响应情况。

### 3. TC-12: 删除记录联动延迟
*   **问题描述**：删除记录后，首页列表移除正常，但日历状态点未实时更新。
*   **修复方案**：将 `FitnessViewModel` 中的 `checkInDates` 和 `workoutDayStatuses` 的 `SharingStarted` 策略从 `WhileSubscribed(5000)` 改为 `Eagerly`。
*   **修复效果**：**通过**。验证发现删除记录后，日历圆点立即消失。

### 4. TC-14: 冷启动数据丢失
*   **问题描述**：后台杀掉应用后数据消失。
*   **原因分析**：默认的 WAL (Write-Ahead Logging) 模式在极端情况下（应用被系统强制杀死）可能未及时将日志合并到主数据库文件。
*   **修复方案**：在 `WorkoutDatabase` 构建时，强制设置 `JournalMode.TRUNCATE`，确保每次事务提交后数据立即写入磁盘。
*   **修复效果**：**通过**。测试发现即便创建完计划立即执行 `am force-stop`，重启后数据依然完好。

---

**待办事项**：
1. 优化 `WorkoutLoggerScreen` 的滚动性能。
2. 增加训练完成后的分享图片导出功能。
