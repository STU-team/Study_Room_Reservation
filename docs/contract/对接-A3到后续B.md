# 对接说明：实验3 总类图（A-3）-> B 后续活动图/时序图/前端

> **文件作用**：成员A 把实验3 UML 类图交接给成员B，作为后续 B 画实验4 签退活动图、实验6 在线预约时序图、以及 Swing 前端事件绑定时的领域模型依据。B 在评审时对照此文件与类图即可，不需要再问 A 字段含义。
> **课程**：CST9301 软件工程　｜　**编写**：易雨杰（成员A，2024611209）　｜　**接收**：叶梅冰（成员B，2024611038）
> **日期**：2026-11-04　｜　**版本**：v1.1（按 B 评审意见调整：新增 CreditScoreLog 与 CreditChangeReason，ReservationStatus 加 NO_SHOW）　｜　**状态**：A-3 已完成，待 B 二次评审

## 一、A 已经交付什么

| 文件 | 说明 |
|---|---|
| `docs/reports/实验3-UML类图.md` | 实验3 报告 Markdown 版，含名词法四步、属性表、操作表、类间关系、类字典 7 节、12 用例到类的映射、讨论 |
| `docs/reports/实验3-UML类图.docx` | 同上 Word 版，版式复刻实验1 |
| `docs/uml/实验3_总类图.uml` | StarUML 5.0 工程，`PIM业务建模` 模型下挂 `实验3_总类图` 类图 |
| `docs/assets/实验3_总类图.jpg` | 类图导出图（已插入报告图3-1） |
| `docs/assets/实验3_总类图-界面.png` | StarUML 整窗截图（附录用） |

## 二、类图口径（请 B 重点核对）

本类图是**分析类图**，只画业务实体与枚举，不画 Service / DAO / Factory 这些技术分层类。共 7 个业务类 + 4 个枚举：

### 7 个业务类与关键字段

| 类 | 关键字段 | 关键方法 |
|---|---|---|
| User | userId:Long、username:String、password:String、name:String、role:Role | login():boolean、modifyInfo():void |
| Student | studentId:String、name:String、phone:String、creditScore:Integer | （信用分校验在预约流程中读取） |
| SeatType | typeName:String、seatCount:Integer(derived) | 无 |
| Seat | seatId:Long、roomName:String、seatNo:String、status:SeatStatus | updateStatus(newStatus: SeatStatus):void |
| Reservation | reservationId:Long、userId:Long、seatId:Long、createTime:Date、startTime:Date、endTime:Date、signInDeadline:Date、status:ReservationStatus | cancel():boolean、checkIn():boolean、checkOut():boolean、handleNoShow():void |
| CheckinRecord | recordNo:String、actualStart:Date、actualEnd:Date、penalty:Integer | 无 |
| CreditScoreLog | logId:Long、studentId:String、reservationId:String（可空，管理员手工调整时为空）、changeValue:Integer、reason:CreditChangeReason、createTime:Date | 无 |

### 4 个枚举

- `Role`：STUDENT / DUTY_ADMIN / SYS_ADMIN
- `SeatStatus`：AVAILABLE / RESERVED / OCCUPIED / CLOSED
- `ReservationStatus`：BOOKED / CHECKED_IN / CHECKED_OUT / CANCELLED / **NO_SHOW**
- `CreditChangeReason`：**NO_SHOW / LATE_CHECKOUT / OTHER**

### 类间关系（6 条业务关系 + 4 条枚举依赖）

| 关系 | 两端 | 多重性 |
|---|---|---|
| 聚合（空心菱形在 SeatType 端） | SeatType -> Seat | 1 : 0..* |
| 关联 | User -> Reservation | 1 : 0..* |
| 关联 | Seat -> Reservation | 1 : 0..* |
| 关联 | User -> Student | 1 : 0..1 |
| 关联 | Reservation -> CheckinRecord | 1 : 0..1 |
| 关联 | Student -> CreditScoreLog | 1 : 0..* |
| 依赖虚线 | User -> Role、Seat -> SeatStatus、Reservation -> ReservationStatus、CreditScoreLog -> CreditChangeReason | — |

## 三、B 评审时请重点检查

1. **多重度是否反了**：特别是 SeatType->Seat（1:0..*）、User->Reservation（1:0..*）、Reservation->CheckinRecord（1:0..1）。
2. **状态流转是否闭环**：SeatStatus 的 OCCUPIED 不能直接改 AVAILABLE，必须先签退（契约 v0.6 约束）；ReservationStatus 的 BOOKED -> CHECKED_IN -> CHECKED_OUT 主链路，BOOKED -> CANCELLED（学生主动取消），BOOKED -> NO_SHOW（系统爽约）三条分支。
3. **枚举值是否齐全**：Role 三值、SeatStatus 四值、ReservationStatus 五值（含新增 NO_SHOW）、CreditChangeReason 三值，缺一个都会导致后续代码写死字符串。
4. **信用分落点**：爽约扣 5、超时扣 1、低于 80 禁约、签到宽限 15 分钟——每次扣分都写一条 CreditScoreLog；规则参数在本类图上**没有**单独建 SystemConfig 类，而是放在 M01 维护用例里；B 若觉得有必要，可以在时序图里以参数对象形式出现。
5. **两条扣分链路不要混**：I03（签退释放包含的"判定超时并扣分"）走 `Reservation.checkOut()`，写 `CreditScoreLog(LATE_CHECKOUT, -1)`；爽约扣 5 分不走 I03，由后台定时任务调 `Reservation.handleNoShow()`，把 BOOKED 直接置 NO_SHOW，写 `CreditScoreLog(NO_SHOW, -5)`。
6. **checkIn 的操作者是值班管理员**：Study-F01 参与者是值班管理员在前台录入预约号登记，不是学生自助签到；B 画活动图和时序图时注意泳道是值班管理员。
7. **seatCount 是派生属性**：M01 维护座位数量是通过增删 Seat 记录实现的，不是直接改 SeatType.seatCount；B 画 M01 活动图时注意这个动作落点。

## 四、对 B 后续工作的影响

| B 的任务 | 与本类图的关系 |
|---|---|
| B-2 实验4 签退释放活动图 | 泳道里的对象就是 Reservation、Seat、CheckinRecord、Student；状态变更直接引用 ReservationStatus 与 SeatStatus 枚举 |
| B-3 实验6 在线预约时序图 | 时序图里的实体 lifeline 就是 User/Student/Seat/Reservation；边界类（登录窗、预约窗）与控制类（预约控制器）是新增的设计类，不画进本分析类图 |
| B-4/B-5 Swing 前端 | 字段名与本类图保持一致，事件触发的方法就是 Reservation.cancel / Reservation.checkIn / Reservation.checkOut / Seat.updateStatus / User.login；M03 信用分流水页直接读 CreditScoreLog 表 |
| B-7 大报告统稿 | 直接引用 `docs/assets/实验3_总类图.jpg` 作为 PIM 阶段总览图 |

## 五、不需要 B 做的事

- 不要改本类图的字段名与方法名；如发现与契约 v0.6 不一致，在 PR 评论里指出，由 A 统一修改。
- 不要在类图上新增 Service / DAO 类；这些留到 A-5、A-6 编码阶段再补。
- 不要把 Role 拆成 Admin / Student 三个类；我们用枚举区分，已经在报告讨论节说明理由。

## 六、回传与依赖

| 事项 | 谁 | 时间 |
|---|---|---|
| A-3 类图与报告交付 | 易雨杰 | 已完成（待 PR 合并） |
| B 评审本类图与类字典 | 叶梅冰 | 收到 dev-A PR 后 |
| B 基于本类图开始实验4 活动图 | 叶梅冰 | 评审通过后即可 |
