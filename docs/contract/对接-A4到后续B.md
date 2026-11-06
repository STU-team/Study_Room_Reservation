# 对接说明：实验5 状态图（A-4）-> B 后续活动图/时序图/前端

> **文件作用**：成员A 把实验5 UML 状态图交接给成员B，作为 B 画实验4 签退活动图、实验6 在线预约时序图、以及 Swing 前端界面时「状态怎么切、什么事件触发、什么条件下走哪条分支」的依据。B 评审时对照此文件与两张状态图即可，不需要再问 A 状态含义。
> **课程**：CST9301 软件工程　｜　**编写**：易雨杰（成员A，2024611209）　｜　**接收**：叶梅冰（成员B，2024611038）
> **日期**：2026-11-06　｜　**版本**：v1.0（首版，座位状态图与预约记录状态图）　｜　**状态**：A-4 已完成，待 B 评审

## 一、A 已经交付什么

| 文件 | 说明 |
|---|---|
| `docs/reports/实验5-状态图.md` | 实验5 报告 Markdown 版，含状态图对象筛选依据、两张状态图的状态清单与转移表、一致性核对、讨论 4 小节 |
| `docs/reports/实验5-状态图.docx` | 同上 Word 版，已定稿（标题「实验5 UML状态图（自习室预约管理子系统）」，实验时间 2026 年 10 月 28 日），提交用 |
| `docs/uml/实验5_状态图.uml` | StarUML 5.0 工程，同一个模型文件下含 `座位状态图` 与 `预约记录状态图` 两张图 |
| `docs/assets/实验5_座位状态图.jpg` | 座位状态图导出图（已插入报告图5-1） |
| `docs/assets/实验5_座位状态图-界面.png` | 座位状态图的 StarUML 整窗截图（附录用） |
| `docs/assets/实验5_预约记录状态图.jpg` | 预约记录状态图导出图（已插入报告图5-2） |
| `docs/assets/实验5_预约记录状态图-界面.png` | 预约记录状态图的 StarUML 整窗截图（附录用） |

## 二、状态口径（请 B 重点核对）

### 座位状态（SeatStatus，4 个状态 + 初始/终止伪状态）

状态：`AVAILABLE`（可用）、`RESERVED`（已预约）、`OCCUPIED`（使用中）、`CLOSED`（维修关闭）

| 源状态 | 事件 | 目标状态 |
|---|---|---|
| 初始 | 系统启动加载座位数据 | AVAILABLE |
| AVAILABLE | 预约成功 | RESERVED |
| RESERVED | 取消 / 爽约释放 | AVAILABLE |
| AVAILABLE | 签到 | OCCUPIED |
| OCCUPIED | 签退释放 | AVAILABLE |
| AVAILABLE | 转维修 | CLOSED |
| RESERVED | 转维修（先撤销关联预约） | CLOSED |
| OCCUPIED | 转维修（先办签退） | CLOSED |
| CLOSED | 维修完成 | AVAILABLE |

图上备注：OCCUPIED 不能直接转为 AVAILABLE，必须先签退（契约 v0.6 约束）。

### 预约记录状态（ReservationStatus，5 个状态 + 初始/终止伪状态）

状态：`BOOKED`、`CHECKED_IN`、`CHECKED_OUT`、`CANCELLED`、`NO_SHOW`

| 源状态 | 事件 | 目标状态 |
|---|---|---|
| 初始 | 生成预约 | BOOKED |
| BOOKED | 学生取消 | CANCELLED（终止） |
| BOOKED | 签到登记 | CHECKED_IN |
| CHECKED_IN | 签退释放 | CHECKED_OUT（终止） |
| BOOKED | 超时未签到 | NO_SHOW（终止） |

## 三、B 评审时请重点检查

1. **两个「转维修」的源状态是否缺**：座位从 AVAILABLE、RESERVED、OCCUPIED 三种状态都可能被发现损坏而转 CLOSED，图上画了三条；如果 B 认为 RESERVED 或 OCCUPIED 转维修在实际系统里不该发生，需要在 PR 里说明。
2. **NO_SHOW 与 CANCELLED 是否真的是两条独立出口**：这是 v0.6 契约改动的核心。B 画实验4 签退活动图或统计报表时，这两条必须分开计数，不能合并成「已终止」。
3. **签退释放的参与者是值班管理员**：`OCCUPIED → AVAILABLE` 的事件「签退释放」由值班管理员在前台执行，验收 Study-F02 的参与者；B 画活动图泳道时不要写成学生自助。
4. **超时未签到由后台定时任务触发**：`BOOKED → NO_SHOW` 的监护条件是「当前时间已过签到截止时间（预约开始 + 15 分钟）」，触发者是系统定时扫描，不是任何界面按钮。B 画活动图时这条应放在系统泳道或作为后台事件。
5. **两条扣分规则挂在两条不同转移上**：`CHECKED_IN → CHECKED_OUT` 上挂的是「实际结束晚于预约结束扣 1 分 + 写 `CreditScoreLog(LATE_CHECKOUT, -1)`」；`BOOKED → NO_SHOW` 上挂的是「扣 5 分 + 写 `CreditScoreLog(NO_SHOW, -5)`」。这两条不要画到同一条分支里。
6. **状态名一律用英文常量**：持久化到 CSV 时写的就是 `BOOKED`、`NO_SHOW` 这类英文常量名，B 写前端下拉框或状态色块时直接用这些值，不要另造中文状态字符串。

## 四、对 B 后续工作的影响

| B 的任务 | 与本状态图的关系 |
|---|---|
| B-2 实验4「值班管理员签退」活动图 | 签退流程对应本图 `CHECKED_IN → CHECKED_OUT` 与 `OCCUPIED → AVAILABLE` 两条转移；活动图里的判定节点就是「实际结束时间 > 预约结束时间」这个监护条件；异常分支可以借用「转维修」的处理思路 |
| B-3 实验6「在线预约」时序图 | 预约成功后座位置 RESERVED、预约置 BOOKED，正是本图两条状态图各自的初始转移；时序图末态直接引用 BOOKED / RESERVED |
| B-4/B-5 Swing 前端 | 座位状态色块沿用契约第九节的四色方案（AVAILABLE 绿、RESERVED 橙、OCCUPIED 红、CLOSED 灰）；预约状态列表要能显示 NO_SHOW 并单独标色，不能和 CANCELLED 混为一色 |
| B-6 运行截图与演示 | 演示流程建议按「BOOKED → CHECKED_IN → CHECKED_OUT」主链路走一遍，再补一条 CANCELLED 和一条 NO_SHOW，正好覆盖状态图三条出口 |
| B-7 大报告统稿 | 直接引用 `docs/assets/实验5_座位状态图.jpg` 与 `docs/assets/实验5_预约记录状态图.jpg` 作为 PIM 阶段状态图 |

## 五、不需要 B 做的事

- 不要改本状态图的状态名与事件名；如发现与契约 v0.6 不一致，在 PR 评论里指出，由 A 统一修改。
- 不要在状态图上补 Service / DAO 的调用细节；这些留在 A-6 编码与实验6 时序图里表达。
- 不要把「转维修」画成从 CLOSED 出发的转移；CLOSED 只有一条出边（维修完成 → AVAILABLE）。

## 六、回传与依赖

| 事项 | 谁 | 时间 |
|---|---|---|
| A-4 状态图与报告交付 | 易雨杰 | 已完成（待 PR 合并） |
| B 评审两张状态图与状态转移表 | 叶梅冰 | 收到 dev-A PR 后 |
| B 基于本状态图开始实验4 签退活动图 | 叶梅冰 | 评审通过后即可 |
