# 实验3　UML 类图

> **课程**：CST9301 软件工程　｜　**版本**：v1.0（2026-11-02 按接口契约 v0.5 建模）
> **文件作用**：实验3 报告（Markdown 版），供 GitHub 在线浏览与团队协作；Word 版为提交用的最终版。两版内容一致。
> **撰写**：易雨杰（成员A，2024611209）　｜　**评审**：叶梅冰（成员B，2024611038）

## 实验基本信息

| 项目 | 内容 |
|---|---|
| 学院 | 数学与计算机学院 |
| 系 | 计算机系 |
| 专业 | 计算机科学与技术 |
| 姓名 | 易雨杰（2024611209）、叶梅冰（2024611038） |
| 实验时间 | 2026 年 11 月 2 日 |
| 实验题目 | 实验3　UML 类图 |
| 成绩 | |

## 一、实验目的

1. 掌握从业务需求与系统功能中识别类与对象的方法，理解名词法提取候选类的完整过程。
2. 掌握类的属性、操作的定义方式，以及职责分配的基本原则。
3. 掌握类间关联、泛化、依赖、组合关系的表达，能在 StarUML 中绘制完整类图并导出。
4. 形成与接口契约一致的系统静态结构，为后续状态图、时序图与编码提供依据。

## 二、实验要求

1. 分析系统业务流程与系统功能，定义系统的初步静态结构，找出重要的类和对象。
2. 使用 StarUML 建模工具绘制类图，并描述和建立类间关系。
3. 为每个类补充分析文档（类字典），说明职责、属性、操作与关联。
4. 提交实验报告。

## 三、主要实验工具

- **StarUML 5.0（with cm）**：安装路径 `D:\StarUML`，用于本项目全部 UML 图的建模与导出，启动前需设置 Windows XP (Service Pack 2) 兼容模式。
- **Git / GitHub**：版本控制与团队协作，主干 `main`，成员A（易雨杰）使用 `dev-A`，成员B（叶梅冰）使用 `dev-B-new`。
- **IntelliJ IDEA + JDK 17 + Maven**：后续编码阶段使用（Maven Wrapper 已随仓库提供）。
- **接口契约 v0.5**（`docs/contract/接口契约.md`）：本实验的类、属性、方法签名、枚举取值全部以契约为准，类图不另造名词。

## 四、实验内容及实验数据记录

### （一）名词法提取候选类

按指导书的建模步骤，先从需求文本中提取名词，再逐层筛选。

**第一步：从需求中找出名词（候选对象）**

自习室、座位、座位号、座位区域、座位类型、学生、学号、手机号、预约、预约记录、预约号、日期、时间段、开始时间、结束时间、签到、签退、签到记录、实际开始时间、实际结束时间、信用分、罚分、违约、值班管理员、系统管理员、用户、账号、用户名、密码、角色、规则参数、信用分阈值、宽限时间。

**第二步：合并含义相同的名词，排除范围以外的名词**

| 处理方式 | 名词 | 说明 |
|---|---|---|
| 合并 | 预约 / 预约记录 / 预约号 | 统一为「预约」这一对象，预约号是它的标识属性 |
| 合并 | 签到记录 / 签退记录 | 统一为「签到签退记录」，同一次使用只产生一条 |
| 合并 | 座位区域 / 座位类型 | 统一为「座位区域」，区域名称即类型名称 |
| 合并 | 违约 / 罚分 / 信用分扣减 | 都是信用分机制的表现，归入「信用分」属性与罚分字段 |
| 排除 | 自习室 | 是整个系统本身，不是系统内的对象 |
| 排除 | 值班管理员 / 系统管理员 | 不作为独立类，由「用户」对象的角色属性区分 |

**第三步：去掉只能作为类属性的名词**

日期、时间段、开始时间、结束时间、座位号、学号、手机号、用户名、密码、信用分、罚分、宽限时间、信用分阈值——这些都依附于某个对象存在，作为属性保留，不单独成类。

**第四步：剩下的名词就是分析类**

座位、座位区域、学生、预约、签到签退记录、用户、角色。

### （二）类的属性

属性来源分三类：业务直接给出的字段（如座位号、学号）、状态类属性（如座位状态、预约状态）、关联类属性（如预约中的学生与座位引用）。

| 类 | 属性 | 类型 | 说明 |
|---|---|---|---|
| User | username | String | 登录名 |
| User | password | String | 登录密码 |
| User | role | Role | 角色，取值为枚举 |
| Student | name | String | 姓名 |
| Student | studentId | String | 学号，唯一标识 |
| Student | phone | String | 联系电话 |
| Student | creditScore | int | 信用分，初始 100 |
| SeatType | typeName | String | 区域名称，如「自习区 A」 |
| SeatType | seatCount | int | 该区域座位总数 |
| Seat | seatNo | String | 座位号，唯一标识 |
| Seat | seatType | SeatType | 所属区域 |
| Seat | status | SeatStatus | 座位状态，取值为枚举 |
| Reservation | resNo | String | 预约号，唯一标识 |
| Reservation | student | Student | 预约人 |
| Reservation | seat | Seat | 预约座位 |
| Reservation | resDate | LocalDate | 预约日期 |
| Reservation | startTime | LocalTime | 预约开始时间 |
| Reservation | endTime | LocalTime | 预约结束时间 |
| Reservation | status | ReservationStatus | 预约状态，取值为枚举 |
| CheckinRecord | recordNo | String | 记录号，唯一标识 |
| CheckinRecord | resNo | String | 对应的预约号 |
| CheckinRecord | actualStart | LocalDateTime | 实际开始时间 |
| CheckinRecord | actualEnd | LocalDateTime | 实际结束时间 |
| CheckinRecord | penalty | int | 本次扣减的信用分，无扣分为 0 |

**状态与枚举补充**（对应指导书「补充该类动态属性，如状态」）：

| 枚举 | 取值 | 含义 |
|---|---|---|
| Role | STUDENT / DUTY_ADMIN / SYS_ADMIN | 学生 / 值班管理员 / 系统管理员 |
| SeatStatus | AVAILABLE / RESERVED / OCCUPIED / CLOSED | 可用 / 已预约 / 使用中 / 维修关闭 |
| ReservationStatus | BOOKED / CHECKED_IN / CHECKED_OUT / CANCELLED | 已预约 / 已签到 / 已签退 / 已取消 |

### （三）类的操作与职责分配

按指导书「从动词、功能或系统责任中寻找操作，再合并筛选、合理分配」，把动词归到职责最贴近的类上，避免把业务编排塞进实体类。

**职责分配原则**：

1. 实体类只保留自身状态判定与自身数据维护，不承担跨对象编排；
2. 涉及多个实体的业务流程（预约、签到、签退、爽约扫描）统一由服务类承担；
3. 数据读写由 DAO 类承担，服务类不直接碰文件；
4. 信用分规则参数集中放在配置类，避免散落在各处硬编码。

**实体类的操作**（仅自身状态判定）：

| 类 | 操作 | 说明 |
|---|---|---|
| Seat | isAvailable(): boolean | 判断座位当前是否可用 |
| Student | canReserve(): boolean | 信用分是否达到预约阈值 |
| Student | deductCredit(int points): void | 扣减信用分并做下限保护 |
| Reservation | isCancellable(): boolean | 是否处于可取消状态 |
| Reservation | isOverdueSignIn(now, graceMinutes): boolean | 是否已超过签到截止时间 |
| Reservation | isOvertime(actualEnd): boolean | 实际结束是否晚于预约结束 |

**服务类的操作**（与接口契约 v0.5 完全一致，签名不改）：

| 类 | 操作 |
|---|---|
| ReservationService | listAvailableSeats(date, start, end, typeName) |
| ReservationService | reserve(seatNo, student, date, start, end) |
| ReservationService | cancelReservation(resNo) |
| ReservationService | signIn(resNo) |
| ReservationService | signOut(resNo, actualEnd) |
| ReservationService | handleNoShow(now) |
| SeatService | listAllSeats() |
| SeatService | findByNo(seatNo) |
| SeatService | updateSeatStatus(seatNo, status) |
| AuthService | login(username, password) |
| AuthService | logout() |

**DAO 类的操作**（与契约第七节一致，签名不改）：

| 类 | 操作 |
|---|---|
| SeatDao | findAll() / findByNo(seatNo) / update(seat) |
| ReservationDao | save(r) / findByNo(resNo) / findAll() / update(r) / nextResNo() |
| StudentDao | findByStudentId(studentId) / update(student) / findAll() |
| UserDao | findByUsername(username) / save(user) |
| CheckinRecordDao | save(record) / findByResNo(resNo) |

**配置类**（承载 M01 维护的规则参数）：

| 类 | 内容 |
|---|---|
| SystemConfig | creditInitial=100、penaltyNoShow=5、penaltyOvertime=1、forbidCreditThreshold=80、checkinGraceMinutes=15、日期与时间格式 |

> 说明：`SystemConfig` 是配置类，不属于接口契约的实体清单，只是把契约第二节的信用分规则参数集中一处，避免散落成魔法数字。类图中以带构造型的配置类表示。

### （四）类间关系

指导书要求类图体现关联、泛化、依赖等关系，本图共四类：

| 关系 | 两端 | 多重性 | 说明 |
|---|---|---|---|
| 组合 | SeatType → Seat | 1 : 0..* | 座位属于某个区域，区域撤销时其下座位一并失效 |
| 关联 | Student → Reservation | 1 : 0..* | 一个学生可有多条预约记录 |
| 关联 | Seat → Reservation | 1 : 0..* | 一个座位在不同时段可被多次预约 |
| 关联 | Reservation → CheckinRecord | 1 : 0..1 | 只有完成签到的预约才有签到签退记录 |
| 关联 | User → Student | 1 : 0..1 | 学生角色的登录账户对应一份学生档案，管理员账户无学生档案 |
| 依赖 | ReservationService → Reservation / Seat / Student / CheckinRecord | — | 服务类调用实体完成业务 |
| 依赖 | ReservationService / SeatService / AuthService → 各自 DAO | — | 服务类通过 DAO 读写数据 |
| 依赖 | Service → SystemConfig | — | 读取信用分规则参数 |
| 泛化 | 无类间泛化 | — | 三个角色差异用 `Role` 枚举表达，不建 Admin 类层次，避免为三个仅角色不同的对象造三个类 |

> 关于泛化：用例图中「学生／值班管理员／系统管理员」泛化到抽象参与者「用户」，这是参与者层次；在类图中三者合并为 `User` 类，用 `Role` 枚举区分权限，因此类图不出现类之间的泛化箭头。这一点是本次建模相对用例图的一个有意简化，在讨论部分说明。

### （五）类图

本图在 StarUML 5.0 中按上述 19 个类、5 条关联、8 条依赖建模，工程文件为 `docs/uml/实验3_总类图.uml`，导出的图片为 `docs/assets/实验3_总类图.jpg`。

![图3-1 自习室预约管理子系统总类图](../assets/实验3_总类图.jpg)

图3-1 自习室预约管理子系统总类图

**图例说明**：实体类位于 `com.studyroom.entity` 包，服务接口位于 `com.studyroom.service`，DAO 接口位于 `com.studyroom.dao`，配置类位于 `com.studyroom.util`。实心菱形为组合，实线为关联，虚线箭头为依赖。

### （六）类字典

按指导书「补充每个类的分析文档」的要求，逐类说明。

#### 1. User（用户账户）

- **职责**：保存系统登录账户，区分三种角色；不承担任何业务判定。
- **属性**：username、password、role。
- **操作**：无业务操作，仅作为 `AuthService` 的数据载体。
- **关联**：与 Student 为 1 : 0..1 关联，`role = STUDENT` 的账户对应一份学生档案。
- **支撑用例**：Study-U01 登录、Study-I01 校验（权限前提）。

#### 2. Student（学生）

- **职责**：保存学生基本信息与信用分，提供信用分是否达标的自身判定。
- **属性**：name、studentId、phone、creditScore。
- **操作**：`canReserve()` 判断信用分是否 ≥ 80；`deductCredit(points)` 扣分并保证不低于 0。
- **关联**：与 Reservation 为 1 : 0..*；与 User 为 0..1 : 1。
- **支撑用例**：Study-C02 在线预约（信用分校验）、Study-M03 预约记录与信用分管理。

#### 3. SeatType（座位区域）

- **职责**：描述一个座位区域及其规模，是 M01 维护规则时的操作对象。
- **属性**：typeName、seatCount。
- **操作**：无业务操作。
- **关联**：与 Seat 为 1 : 0..* 组合。
- **支撑用例**：Study-M01 座位区域与规则维护、Study-C01 查询座位（按区域筛选）。

#### 4. Seat（座位）

- **职责**：保存单个座位的编号、所属区域与状态，提供可用性判定。
- **属性**：seatNo、seatType、status。
- **操作**：`isAvailable()` 判断状态是否为 AVAILABLE。
- **关联**：与 SeatType 为 0..* : 1 组合；与 Reservation 为 1 : 0..* 关联。
- **状态流转**：AVAILABLE → RESERVED（预约成功）→ OCCUPIED（签到）→ AVAILABLE（签退释放）；任意状态 → CLOSED（转维修）；OCCUPIED 不得直接改为 AVAILABLE，必须先签退。
- **支撑用例**：Study-C01、Study-C02、Study-F01、Study-F02、Study-M02 座位状态维护。

#### 5. Reservation（预约记录）

- **职责**：保存一次预约的时段、学生、座位与状态，提供取消与超时的自身判定。
- **属性**：resNo、student、seat、resDate、startTime、endTime、status。
- **操作**：`isCancellable()` 判断是否为 BOOKED；`isOverdueSignIn(now, graceMinutes)` 判断是否已超过签到截止时间；`isOvertime(actualEnd)` 判断是否超时未签退。
- **关联**：与 Student 为 0..* : 1；与 Seat 为 0..* : 1；与 CheckinRecord 为 1 : 0..1。
- **状态流转**：BOOKED → CHECKED_IN（签到）→ CHECKED_OUT（签退）；BOOKED → CANCELLED（取消或爽约）。
- **支撑用例**：Study-C02、Study-C03、Study-F01、Study-F02、Study-I01、Study-I02、Study-I03、Study-M03。

#### 6. CheckinRecord（签到签退记录）

- **职责**：保存一次实际使用的起止时间与本次扣分，作为信用分变动的凭据。
- **属性**：recordNo、resNo、actualStart、actualEnd、penalty。
- **操作**：无业务操作。
- **关联**：与 Reservation 为 0..1 : 1。
- **支撑用例**：Study-F01、Study-F02、Study-I03、Study-M03。

#### 7. Role / SeatStatus / ReservationStatus（枚举）

- **职责**：替代大量布尔属性，把状态取值固定下来，防止出现非法状态字符串。
- **取值**：见（二）状态与枚举补充。
- **说明**：持久化时按英文常量名写入 CSV，与契约第八节一致。

#### 8. AuthService（认证服务）

- **职责**：校验登录名与密码，返回用户对象；负责退出登录清空会话。
- **操作**：`login(username, password)`、`logout()`。
- **依赖**：UserDao。
- **支撑用例**：Study-U01 登录。

#### 9. ReservationService（预约服务）

- **职责**：承担预约、取消、签到、签退、爽约扫描的全部业务编排与校验，是本系统的核心服务。
- **操作**：
  - `listAvailableSeats(date, start, end, typeName)`：按日期、时段与区域列出可用座位，供 C01 使用；
  - `reserve(seatNo, student, date, start, end)`：校验信用分达标与座位可用（对应 Study-I01）后生成预约；
  - `cancelReservation(resNo)`：仅允许取消 BOOKED 状态的预约并释放座位；
  - `signIn(resNo)`：核验预约记录（对应 Study-I02），预约置 CHECKED_IN、座位置 OCCUPIED；
  - `signOut(resNo, actualEnd)`：判定是否超时（对应 Study-I03），超时扣 1 分，释放座位；
  - `handleNoShow(now)`：扫描所有已过签到截止时间仍未签到的 BOOKED 预约，逐条取消、释放座位、扣 5 分，返回处理条数。
- **依赖**：Reservation / Seat / Student / CheckinRecord 四个实体，ReservationDao / SeatDao / StudentDao / CheckinRecordDao 四个 DAO，以及 SystemConfig。
- **支撑用例**：Study-C01、C02、C03、F01、F02、I01、I02、I03。

#### 10. SeatService（座位服务）

- **职责**：座位查询与状态维护。
- **操作**：`listAllSeats()`、`findByNo(seatNo)`、`updateSeatStatus(seatNo, status)`。
- **约束**：`updateSeatStatus` 将 OCCUPIED 改为 AVAILABLE 时必须拒绝，提示先办理签退；OCCUPIED 改为 CLOSED 允许。
- **依赖**：Seat、SeatType、SeatDao。
- **支撑用例**：Study-M02 座位状态维护、Study-C01 查询座位。

#### 11. 五个 DAO 类

| 类 | 职责 | 数据文件 |
|---|---|---|
| SeatDao | 座位与区域的读写 | `data/seats.csv`、`data/seat_types.csv` |
| ReservationDao | 预约记录读写，生成预约号 | `data/reservations.csv` |
| StudentDao | 学生与信用分读写 | `data/students.csv` |
| UserDao | 用户账户读写 | `data/users.csv` |
| CheckinRecordDao | 签到签退记录读写 | `data/checkin_records.csv` |

所有 DAO 统一约定：UTF-8 编码、首行表头、日期 `yyyy-MM-dd`、时间 `HH:mm`、枚举存英文常量名；文件不存在时初始化示例数据并写回。

#### 12. SystemConfig（配置类）

- **职责**：集中承载 M01 维护的规则参数与全局格式常量。
- **内容**：creditInitial=100、penaltyNoShow=5、penaltyOvertime=1、forbidCreditThreshold=80、checkinGraceMinutes=15、日期时间格式。
- **说明**：参数改动只需改这一处，`reserve` 与 `handleNoShow` 都从这里读取阈值。

#### 13. ServiceFactory（服务工厂）

- **职责**：向界面层提供三个服务实例，隔离界面对实现类的直接依赖。
- **操作**：`getReservationService()`、`getSeatService()`、`getAuthService()`。
- **说明**：具体装配方式在 A-6 实现阶段确定。

### （七）实验2 十二个用例到类的映射

| 用例编号 | 用例名称 | 主要承载类 |
|---|---|---|
| Study-U01 | 登录 | AuthService、UserDao、User |
| Study-C01 | 查询座位 | ReservationService.listAvailableSeats、SeatService、Seat |
| Study-C02 | 在线预约 | ReservationService.reserve、Reservation、SystemConfig |
| Study-C03 | 取消预约 | ReservationService.cancelReservation、Reservation.isCancellable |
| Study-F01 | 签到登记 | ReservationService.signIn、Seat.status、CheckinRecord |
| Study-F02 | 签退释放 | ReservationService.signOut、Reservation.isOvertime、CheckinRecord |
| Study-M01 | 座位区域与规则维护 | SeatType、SystemConfig、SeatService |
| Study-M02 | 座位状态维护 | SeatService.updateSeatStatus、Seat |
| Study-M03 | 预约记录与信用分管理 | ReservationDao、StudentDao、Student.deductCredit |
| Study-I01 | 校验座位与时段可用性 | ReservationService.reserve 内部校验、Seat.isAvailable |
| Study-I02 | 核验预约记录 | ReservationService.signIn 内部校验、Reservation |
| Study-I03 | 判定超时并记录信用分扣减 | ReservationService.signOut、handleNoShow、CheckinRecord.penalty |

## 五、实验数据分析与讨论

### （一）从用例图到类图的推导

用例图回答「谁做什么」，类图回答「用什么做」。本实验的推导路径有三条：

1. **参与者收敛为类**：用例图里的四个参与者（学生、值班管理员、系统管理员、抽象用户）在类图中收敛为 `User` 一个类加 `Role` 枚举。三个角色的差别只是权限，用枚举表达比建三个类更省，也避免出现只有 role 字段不同的重复类。
2. **用例动词分配到类**：把「预约」「签到」「签退」这些动词归到 `ReservationService`，而不是塞进 `Reservation` 类。这样 `Reservation` 只负责自己的状态与判定，业务流程的变化不会污染实体类。
3. **被包含用例落到方法内部**：用例图中的三个被包含用例（校验座位与时段可用性、核验预约记录、判定超时并扣分）在类图中不是独立的类，而是 `reserve`、`signIn`、`signOut` 内部的一段校验逻辑。这与 include 关系的语义一致——被包含用例是被调用方，在代码里就是一个被复用的方法段。

### （二）信用分机制的类图落点

契约 v0.5 引入信用分后，类图上有三处必须同时对齐，否则会出现「扣分找不到责任人」的问题：

1. **分存在哪里**：`Student.creditScore`，扣分方法 `deductCredit` 也放在 Student，保证任何扣分都要经过同一个入口并做下限保护。
2. **扣多少由谁决定**：`SystemConfig` 里的 5 分、1 分、80 分阈值，不在 `reserve` / `signOut` 里写死。
3. **爽约由谁触发**：`ReservationService.handleNoShow(now)`。这是本实验相对 v0.4 的关键补充——如果只靠学生点「取消」或管理员点「签退」，学生既不签到也不取消时座位会被永久占用，所以必须有一个独立扫描入口，它不属于任何一个用例界面，属于系统定时任务。

### （三）一个有意保留的设计缺口

`SystemConfig` 是配置类，不在接口契约的实体清单里。这样处理的好处是规则参数集中、改一处生效；代价是如果老师严格按契约实体清单对照类图，会多出一个类。本次选择保留它，理由是该类只承载常量，不影响对外接口，也不参与持久化业务对象。若评审认为需要收敛，可将五个常量并入 `SeatType` 或改为常量类注释。

### （四）与契约的一致性核对

类图中的 6 个实体、3 个枚举、3 个服务接口、5 个 DAO 接口，其属性名、方法名、方法参数与返回值、枚举取值，逐项对照 `docs/contract/接口契约.md` v0.5，未发现不一致。特别是 `handleNoShow(LocalDateTime now)` 与 `updateSeatStatus` 的 OCCUPIED 约束，均已体现在类图与方法说明中。

## 六、分工与评审说明

| 姓名 | 学号 | 本报告承担工作 |
|---|---|---|
| 易雨杰 | 2024611209 | 名词法分析、类的属性与操作设计、职责分配、类图绘制、类字典撰写、报告排版 |
| 叶梅冰 | 2024611038 | 需求复核、类图与类字典评审、与接口契约的一致性检查 |

本报告由成员A 易雨杰主笔撰写，成员B 叶梅冰参与评审。

仓库：https://github.com/STU-team/Study_Room_Reservation
