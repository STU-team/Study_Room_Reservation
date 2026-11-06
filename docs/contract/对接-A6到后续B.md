# 对接：A-6 Service 层交给 B

> **文件作用**：成员B 做 Swing 界面（B-4/B-5）时与 A-6 Service 层的对接约定：怎么拿实例、有哪些方法、异常怎么处理。
> **课程**：CST9301 软件工程  
> **作者**：易雨杰（成员A）  
> **创建时间**：2026-11-06  
> **版本**：v1.0  
> **关联**：`docs/contract/接口契约.md` v0.8 第六节、`docs/development/A6-实现记录.md`

## 一、怎么拿到 Service

界面里只通过 `ServiceFactory` 取实例，不要自己 `new` 实现类，也不要直接碰 DAO 或 CSV：

```java
ReservationService reservationService = ServiceFactory.getReservationService();
SeatService seatService = ServiceFactory.getSeatService();
AuthService authService = ServiceFactory.getAuthService();
```

- 三个方法是静态的，内部单例，第一次调用时会按依赖顺序创建七个 DAO、建表并写入示例数据（4 个座位 `A01/A02/B01/B02`、2 个区域、2 名学生、`admin`/`duty` 两个账号），界面首次启动不会是空列表。
- 默认数据目录是相对路径 `data/`；如需指定，启动时先调 `ServiceFactory.configure(Path.of("..."))`，切换后已装配的实例作废，下次获取重建。
- 包结构：接口在 `com.studyroom.service`，实现在 `com.studyroom.service.impl`，异常在 `com.studyroom.service.BusinessException`。接口签名与契约第六节逐字一致。

## 二、方法清单与返回值

| 方法 | 入参 | 返回 | 界面用途 |
|---|---|---|---|
| `listAvailableSeats(date, start, end, typeName)` | `LocalDate`、`LocalTime`、`LocalTime`、区域名或 `null` | `List<Seat>` | 预约页座位列表；`typeName` 传 `null` 表示不限区域 |
| `reserve(seatNo, student, date, start, end)` | 座位号、`Student`（可传当前登录学生）、日期、起止时间 | `Reservation`（含 `resNo`） | 提交预约成功后弹窗展示预约号 |
| `cancelReservation(resNo)` | 预约号 | `Reservation`（状态 `CANCELLED`） | 我的预约列表的取消按钮 |
| `signIn(resNo)` | 预约号 | `Reservation`（状态 `CHECKED_IN`） | 值班管理员签到登记 |
| `signOut(resNo, actualEnd)` | 预约号、`LocalTime` 实际结束 | `CheckinRecord`（含 `penalty`） | 值班管理员签退，`penalty=1` 表示超时扣 1 分 |
| `handleNoShow(now)` | `LocalDateTime` | `int` 处理条数 | 启动时调一次；返回 0 表示无爽约 |
| `listAllSeats()` | — | `List<Seat>` | 座位管理页全量列表 |
| `findByNo(seatNo)` | 座位号 | `Seat` | 座位详情、状态修改前查询 |
| `updateSeatStatus(seatNo, status)` | 座位号、`SeatStatus` | 无 | 管理员改状态；`OCCUPIED → AVAILABLE` 会被拒绝 |
| `login(username, password)` | 用户名、密码 | `User`（含 `role`） | 登录页；按 `role` 决定菜单 |
| `logout()` | — | 无 | 退出登录 |

时间类型：Service 用 `java.time`（`LocalDate`/`LocalTime`/`LocalDateTime`），界面直接用它；存储层精确到分钟，秒会被截断。

## 三、异常处理

所有业务失败抛 `BusinessException`，界面捕获后弹窗显示 `getMessage()`，需要分支处理时用 `getCode()`：

| code | 触发场景 | 建议提示 |
|---|---|---|
| `PARAM` | 参数为空、结束时间早于开始时间、签退时间非法 | 直接显示 message |
| `CREDIT_LOW` | 信用分低于 80 | 提示先去值班台核销扣分记录 |
| `SEAT_UNAVAILABLE` | 座位已被预约/使用/维修 | 刷新座位列表 |
| `SEAT_CONFLICT` | 座位在所选时段已有预约 | 换时段或换座位 |
| `SEAT_NOT_FOUND` | 座位号不存在 | 刷新列表 |
| `SEAT_IN_USE` | OCCUPIED 直接改 AVAILABLE | 提示“须先办理签退” |
| `STATE` | 预约状态不允许该操作（重复取消、未签到就签退等） | 刷新预约列表 |
| `DEADLINE` | 已过签到截止时间 | 提示已判定爽约 |
| `STUDENT_NOT_FOUND` | 学生档案不存在 | 检查学号 |
| `AUTH_FAILED` | 用户名或密码错误 | 停留在登录页 |
| `DATA_INIT` | 数据目录初始化失败 | 检查 `data/` 写权限与表头 |

## 四、演示与截图注意

1. 演示前先启动一次界面，`data/` 下会自动出现七张 CSV 和示例数据；不要手改表头，`DataInitializer` 会拒绝启动。
2. 想演示爽约：用管理员账号对一条未签到的预约调 `handleNoShow(LocalDateTime.now())`，会看到座位回到 AVAILABLE、预约变 `NO_SHOW`、学生信用分减 5，`credit_score_logs.csv` 多一行 `NO_SHOW,-5`。
3. 想演示超时扣分：签到后把签退时间填得晚于预约结束，返回的记录 `penalty=1`，信用分减 1。
4. 界面上的座位状态直接映射中文：`AVAILABLE` 可用、`RESERVED` 已预约、`OCCUPIED` 使用中、`CLOSED` 维修；预约状态：`BOOKED` 待签到、`CHECKED_IN` 使用中、`CHECKED_OUT` 已签退、`CANCELLED` 已取消、`NO_SHOW` 爽约。

## 五、不需要 B 做的事

- 不要在界面里自己 `new` DAO 或读写 CSV，一律走 `ServiceFactory`。
- 不要自己判断信用分门槛、签到截止时间、扣分数值——这些都在 Service 内，界面只展示结果与异常提示。
- 不要改 `Reservation`、`CheckinRecord` 的时间字段类型；Service 已在边界做 `java.time` 与 `Date` 转换。

## 六、遗留与待确认

1. **爽约扫描时机**：契约要求“启动时一次 + 之后定时触发”，A-6 只提供方法，未起定时器。建议 B 在界面启动时调一次，并用 `javax.swing.Timer` 每小时触发（或 `ScheduledExecutorService`）。
2. **签到记录落盘时机**：当前签到到签退之间不写 `checkin_records` 行，签退时一次性写入完整记录。若要做“实时在座名单”，需给 `CheckinRecordDao` 加 `update` 并按 `CONTRACT-CHANGE` 升级契约。
3. `AuthService` 只校验用户名密码，未做会话超时；`AuthServiceImpl.currentUser()` 为契约外的实现类便利方法，界面可用，接口层面不承诺。
