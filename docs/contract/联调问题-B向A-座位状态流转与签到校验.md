# 联调问题单（B → A）：座位状态可被绕过流程篡改、签到缺少座位校验、座位可用性未按时段判断

> **提出方**：B（界面侧）　｜　**日期**：2026-11-09　｜　**状态**：待 A 确认
> **涉及层**：Service（A-6）为主，界面（B-5）配合　｜　**契约正文**：本次不修改

---

## 一、结论先说

前三个现象都源于 Service 层没有守住状态迁移规则，界面只是把入口摆了出来：

1. `SeatServiceImpl.updateSeatStatus` 只拦了 `OCCUPIED → AVAILABLE`，`OCCUPIED → CLOSED` 和 `RESERVED → 任意状态` 全部放行。
2. `ReservationServiceImpl.signIn` 不校验座位状态，座位被改成 `CLOSED` 或 `OCCUPIED` 后仍然签得进去。
3. 界面侧没有级联处理：系统管理员可以把已预约座位直接改维修，既不撤销预约，也没有任何面向学生的提示。
4. 第四个问题是可用性判断错位：座位状态是单值，`reserve` 和 `listAvailableSeats` 又用“座位必须是 `AVAILABLE`”当唯一门槛，于是一条预约把整条座位锁死，同一座位在其他日期、其他时段也无法再预约（见第四节）。这不是状态图错了，而是把“某个时段被占用”实现成了“整个座位被占用”。
5. 建议 A 的修法：状态图四态和 T-02~T-05 的触发时机都不动，预约成功仍然置 `RESERVED`；只把可用性改成按时段与 `BOOKED` / `CHECKED_IN` 预约判重叠，允许同一座位存在多条不重叠的有效预约（4.3）。

界面隐藏选项只是“不提供入口”，Service 仍然接受非法调用；只要 `signIn` 不校验，值班端到点签到依然会成功。因此真正需要动的是 A-6 的两个实现类。

---

## 二、复现步骤

### 现象 1：使用中的座位可以直接改成“维修”

1. 学生预约 A02 并在值班端签到，座位变为 `OCCUPIED`。
2. 以系统管理员登录 → 座位管理 → 选中 A02 → 修改座位状态 → 选“维修”。
3. 结果：修改成功，座位变为 `CLOSED`。
4. 后果：A02 这条预约仍是 `CHECKED_IN`，没有签退记录，座位却成了维修状态，数据自相矛盾。

### 现象 2：已预约的座位可以被改成“使用中”或“维修”

1. 学生预约 A02（座位变 `RESERVED`，这条预约仍处于 `BOOKED`）。
2. 系统管理员把 A02 改成“使用中”或“维修”。
3. 结果：修改成功。
4. 后果：座位状态与预约状态脱钩；学生端看不到任何变化，值班端到点仍可对该预约执行签到，签到后座位又被改回 `OCCUPIED`。

### 现象 3：座位转维修后，值班端到点仍能签到

1. 承接现象 2，A02 已被改成 `CLOSED`。
2. 值班端在签到窗口内对 A02 的预约执行“签到登记”。
3. 结果：签到成功，预约变 `CHECKED_IN`，座位被改回 `OCCUPIED`，维修状态被覆盖。
4. 期望：拒绝签到并提示座位已转维修。

### 现象 4：学生预约 A02 后，同一座位在其他日期、其他时段也无法预约

1. 学生预约 A02（如明天 10:00–12:00），座位变为 `RESERVED`。
2. 另一名学生（或同一学生）尝试预约 A02 的**后天 14:00–16:00**，时段完全不重叠。
3. 结果：座位列表里查不到 A02；直接调 `reserve` 会抛 `SEAT_UNAVAILABLE`（“座位当前不可预约：RESERVED”）。
4. 影响：一个座位一天只能服务一条预约，时段复用能力丧失；学生端“查空座”看到的是被全局锁死的座位，而不是该时段真正被占用的座位。
5. 期望：预约 10:00–12:00 只让该座位在这两个小时内不可选；其他日期、其他不重叠时段仍能看到、能约。座位状态仍显示为“已预约”，但可用性由所选时段与已有预约是否重叠决定。

---

## 三、现状代码事实（核对日期 2026-11-09）

### 3.1 `SeatServiceImpl.updateSeatStatus`（`src/main/java/com/studyroom/service/impl/SeatServiceImpl.java:33-41`）

```java
if (seat.getStatus() == SeatStatus.OCCUPIED && status == SeatStatus.AVAILABLE) {
    throw new BusinessException("SEAT_IN_USE", "座位使用中，须先办理签退");
}
seat.updateStatus(status);
seatDao.update(seat);
```

只有一条拦截条件，其余迁移一律放行，包括：

- `OCCUPIED → CLOSED`
- `OCCUPIED → RESERVED`
- `RESERVED → OCCUPIED / CLOSED / AVAILABLE`
- `AVAILABLE → RESERVED / OCCUPIED`

### 3.2 `ReservationServiceImpl.signIn`（`src/main/java/com/studyroom/service/impl/ReservationServiceImpl.java:155-169`）

```java
public Reservation signIn(String resNo) {
    Reservation reservation = require(resNo);
    if (reservation.getStatus() != ReservationStatus.BOOKED) { ... }
    if (new Date().after(reservation.getSignInDeadline())) { ... }
    reservation.checkIn();
    reservationDao.update(reservation);
    setSeatStatus(reservation.getSeatNo(), SeatStatus.OCCUPIED);
    checkinRecordDao.save(newRecord(resNo, new Date(), null, 0));
    return reservation;
}
```

全过程没有读取座位当前状态，`setSeatStatus` 是直接覆盖写入。

### 3.3 测试也按旧规则断言

`src/test/java/com/studyroom/service/SeatAndAuthServiceTest.java:46-59`（`occupiedSeatCannotBeReleasedDirectlyButCanClose`）明确断言 `OCCUPIED → CLOSED` 应当成功。本次按 T-08 口径修正系统管理员的手动改状态规则后，这条测试必须同步改写（改为断言失败且座位状态不变），否则 A 侧 `mvn test` 会失败。

### 3.4 界面侧（B-5，供 A 参考）

| 位置 | 现状 |
|---|---|
| `SysAdminMainFrame.java:247` | 下拉候选是“除当前状态外的全部状态”，不做任何约束 |
| `SysAdminMainFrame.java:288` | 直接调用 `seatService.updateSeatStatus`，不检查关联预约 |
| `DutyAdminMainFrame.java:274-315` | 签到前只校验预约状态与时间，不读座位状态 |
| `StudentMainFrame.java:418` | “我的预约”只有预约状态列，没有座位状态列，也没有维修提示 |

---

## 四、附带问题：座位单值状态导致跨日期/时段无法复用

复现步骤见第二节“现象 4”。这一节记录代码事实与处置建议。

### 4.1 现状代码事实

- `Seat` 只有一个状态字段（`src/main/java/com/studyroom/entity/Seat.java:8`），没有日期/时段维度。
- 持久化同样单值：`data/seats.csv` 表头为 `seatNo,typeName,status`，每个座位一行、只有一个状态，例如 `A02,靠窗区,CLOSED`。
- `ReservationServiceImpl.reserve` 预约成功后立即执行 `seat.updateStatus(SeatStatus.RESERVED)`，把整个座位锁住（`src/main/java/com/studyroom/service/impl/ReservationServiceImpl.java` 的 `reserve` 尾部）。
- `listAvailableSeats` 第一道过滤就是 `if (seat.getStatus() != SeatStatus.AVAILABLE) continue;`，所以只要有人预约过，该座位对所有日期/时段都不可见。
- `reserve` 和 `listAvailableSeats` 里都有一段按时段判断重叠的 `occupied(...)`，但因为座位状态先被拦下，这段时段冲突判断在正常数据下**永远走不到**，实际上不决定可用性。

### 4.2 结论：状态图不变，改的是“可用性怎么算”

**实验5 状态图本身没有错**：T-02 由学生预约触发、T-03 由学生取消或系统判爽约触发、T-04 由值班管理员签到登记触发、T-05 由签退触发，职责清晰，四态（`AVAILABLE` / `RESERVED` / `OCCUPIED` / `CLOSED`）也都保留。B 侧不主张改图，也不改契约。

问题出在**两次语义混用**：

1. 座位状态被当成“某日某时段是否被占用”的判据。实际上它只能表达“整条座位当前处于什么阶段”；
2. `reserve` 用“必须 `AVAILABLE`”做前置门槛，`listAvailableSeats` 用“不是 `AVAILABLE` 就跳过”做过滤，等于让单值状态决定时段可用性。

建议把两者拆开：**座位状态回答“这个座位整体处于什么阶段”，可用性回答“我想要的那个时段能不能约”**。

| 座位状态 | 含义（汇总状态） | 何时进入 | 何时离开 |
|---|---|---|---|
| `AVAILABLE` | 没有任何未结束的预约 | 最后一条 `BOOKED` / `CHECKED_IN` 预约结束或取消后 | 新预约成功（T-02）或手动转维修（T-06） |
| `RESERVED` | 至少存在一条 `BOOKED` 预约，但当前没有人在座 | 预约成功（T-02） | 签到（T-04）、取消/爽约（T-03，最后一条预约结束时才回 `AVAILABLE`）、转维修（T-07） |
| `OCCUPIED` | 此刻有已签到的预约在座 | 值班管理员签到登记（T-04） | 签退（T-05）；若该座位还有其他未来预约，签退后回 `RESERVED` 而不是 `AVAILABLE` |
| `CLOSED` | 维修中，不接受任何预约 | 发现损坏（T-06 直接转，T-07 先撤销预约，T-08 先签退） | 维修完成（T-09） |

**可用性判定**（`reserve` / `listAvailableSeats` 共用）：

- 座位为 `CLOSED` → 不可选；
- 其余状态下，把所选日期与起止时间，与该座位所有 `BOOKED`、`CHECKED_IN` 的预约逐一比较；**有重叠则不可选，无重叠则可选**；
- 因此同一座位可以同时存在多条不重叠的有效预约，例如 9 号 10:00–12:00 与 9 号 14:00–16:00、10 号 10:00–12:00 都可以并存；
- 预约成功仍然立即把座位置 `RESERVED`（T-02 时机不变），但 `RESERVED` 不再等于“全时段不可约”，只是“至少有一条未结束预约”的汇总显示。

举例：学生甲预约 A02 在 9 号 10:00–12:00 后，A02 显示 `RESERVED`；学生乙查 9 号 14:00–16:00 仍能看到 A02 并可预约；学生乙查 9 号 11:00–13:00 则看不到或预约被拒（时段重叠）。

**不做的事**：不删除 `RESERVED`，不改 T-02~T-05 的触发者与时机，不改契约第 178-180 行，不改 `seats.csv` 结构（仍是一座位一行一个汇总状态），不引入定时任务。

### 4.3 建议 A 的做法（A-6，待 A 确认）

以下是对 4.2 的落地建议，只动 `ReservationServiceImpl` 的可用性判断与释放逻辑，`SeatServiceImpl` 按 5.2 矩阵收紧手动改状态。**本轮不改代码**，等 A 确认后再排期。

**A 侧改造清单**

| 位置 | 现状 | 建议改法 |
|---|---|---|
| `listAvailableSeats` 第一道过滤 | `if (seat.getStatus() != SeatStatus.AVAILABLE) continue;` | 改为只排除 `CLOSED`：`if (seat.getStatus() == SeatStatus.CLOSED) continue;`，其余交给已有的 `occupied(...)` 按时段判重叠 |
| `reserve` 座位前置校验 | 要求 `seat.getStatus() == SeatStatus.AVAILABLE`，否则 `SEAT_UNAVAILABLE` | 只拒绝 `CLOSED`；`AVAILABLE` / `RESERVED` / `OCCUPIED` 都继续走时段冲突判断（`OCCUPIED` 时预约未来时段是合法的），冲突由 `occupied(...)` 返回 `SEAT_CONFLICT` |
| `reserve` 尾部置位 | 无条件 `seat.updateStatus(SeatStatus.RESERVED)` | 仅当座位当前是 `AVAILABLE` 时置 `RESERVED`；已经是 `RESERVED` 则保持不变（T-02 语义不变，避免覆盖其他状态） |
| `cancelReservation` 释放 | 直接 `release(seatNo, RESERVED)` 回 `AVAILABLE` | 取消成功后先查该座位是否还有其它 `BOOKED` / `CHECKED_IN` 预约：没有才置 `AVAILABLE`（T-03）；还有则保持 `RESERVED` |
| `handleNoShow` 释放 | 逐条爽约后 `release(..., RESERVED)` | 同上，全部爽约处理完再统一重算：无剩余有效预约才回 `AVAILABLE` |
| `signIn` | 不读座位状态，直接写 `OCCUPIED` | 增加座位守卫（5.3）：`CLOSED` 拒绝 `SEAT_UNAVAILABLE`；`OCCUPIED` 拒绝 `SEAT_IN_USE`；`RESERVED` 正常签到并置 `OCCUPIED`；`AVAILABLE` 作为脏数据兜底允许签到并置 `OCCUPIED` |
| `signOut` | 直接置 `AVAILABLE` | 签退后若该座位还有未来 `BOOKED` 预约，置 `RESERVED`；没有才置 `AVAILABLE`（T-05 的文字补注，见下） |
| 新增内部辅助方法 | 现有 `release(String seatNo, SeatStatus expected)` | 抽出 `refreshSeatSummary(String seatNo)`：按该座位全部 `BOOKED` / `CHECKED_IN` 预约重算汇总状态——有 `CHECKED_IN` 则 `OCCUPIED`，否则有 `BOOKED` 则 `RESERVED`，否则 `AVAILABLE`；供取消、爽约、签退三处调用，避免重复逻辑 |

**建议的判定顺序**（`reserve`）：

1. 座位不存在 → `SEAT_NOT_FOUND`；
2. 座位为 `CLOSED` → `SEAT_UNAVAILABLE`；
3. 时间参数、7 天窗口、学生信用分等原有校验不变；
4. 与同座位 `BOOKED` / `CHECKED_IN` 预约按时段判重叠 → 重叠则 `SEAT_CONFLICT`；
5. 保存预约；座位为 `AVAILABLE` 时置 `RESERVED`，否则保持。

**系统管理员转维修（T-07）的级联范围**

采纳 4.2 后，同一座位可能有多条 `BOOKED` 预约，因此：

- `RESERVED → CLOSED` 必须先撤销该座位**全部** `BOOKED` 预约（含未来日期/时段），而不是只处理一条；
- 每撤销一条都要按 T-03 语义通知学生（B 侧在“我的预约”里体现，见第七节）；
- 全部撤销后座位汇总状态自然回到 `AVAILABLE`，再置 `CLOSED`；
- 撤销过程中若某条预约已过签到截止时间，`cancelReservation` 会抛 `DEADLINE`（`ReservationServiceImpl.java:146-148`）。此时应先执行爽约判定（`handleNoShow`，按爽约扣 5 分、预约置 `NO_SHOW`），或中止并提示管理员“该预约已过签到时间，请先执行爽约判定”，不能把座位卡在半路；
- 级联由界面编排还是由 Service 提供编排方法，见第八节问题 2。

**T-05 / T-07 的文字补注（图不重画，契约不改）**

- T-05 `OCCUPIED → AVAILABLE`：建议在图下文字里补一句“若该座位仍有其它未开始的预约，签退后汇总状态回 `RESERVED`”，避免实现与图面看起来不一致；
- T-07 `RESERVED → CLOSED`：建议补一句“撤销的是该座位全部未开始的预约”。这两句只加文字注释，箭头与状态节点不动，也不需要走 `CONTRACT-CHANGE`。

**测试改写 / 新增清单（A-6）**

- 改写现有“预约后同座位其他时段不可约”的断言，改为“不重叠时段可约、重叠时段 `SEAT_CONFLICT`”；
- 新增：同一座位两条不重叠预约并存，座位保持 `RESERVED`；
- 新增：取消其中一条后座位仍 `RESERVED`，取消最后一条后回 `AVAILABLE`；
- 新增：签退后若还有未来预约，座位回 `RESERVED`；无未来预约回 `AVAILABLE`；
- 新增：`signIn` 遇 `CLOSED` 抛 `SEAT_UNAVAILABLE`、遇 `OCCUPIED` 抛 `SEAT_IN_USE`，两者均无签到记录、预约仍 `BOOKED`；
- 保留并改写 3.3 的 `occupiedSeatCannotBeReleasedDirectlyButCanClose`，按 5.2 矩阵断言 `OCCUPIED → CLOSED` 失败。

**工作量与验收口径**

- A 侧：`ReservationServiceImpl` 约 30 行改动 + 一个汇总重算辅助方法，测试约 6 个用例；B 侧空座查询本来就按 `(date, start, end, typeName)` 传参，无需改动，只需在“我的预约”里补座位状态展示（第七节）。
- 验收：
  1. 甲预约 A02 在 9 号 10:00–12:00 后，乙仍能预约 A02 在 9 号 14:00–16:00 与 10 号 10:00–12:00；
  2. 乙预约 9 号 11:00–13:00 被拒，错误码 `SEAT_CONFLICT`；
  3. 座位 `CLOSED` 时既不出现在空座列表，也不能直接 `reserve`（`SEAT_UNAVAILABLE`）；
  4. 取消、爽约、签退后座位汇总状态与剩余有效预约一致，不出现“还有 `BOOKED` 却显示 `AVAILABLE`”；
  5. 签到守卫在座位 `CLOSED` / `OCCUPIED` 时拒绝，且不改预约与座位状态。

---

## 五、期望规则

### 5.1 权威依据

- **实验5 座位状态图（`docs/reports/实验5-状态图.md:75-83`）**：

| 迁移 | 事件 | 动作 |
|---|---|---|
| T-02 `AVAILABLE → RESERVED` | 预约成功 | 锁定座位 |
| T-03 `RESERVED → AVAILABLE` | 取消 / 爽约释放 | 释放座位 |
| T-04 `RESERVED → OCCUPIED` | 签到登记 | 座位置 OCCUPIED |
| T-05 `OCCUPIED → AVAILABLE` | 签退释放 | 释放座位 |
| T-06 `AVAILABLE → CLOSED` | 发现损坏，转维修 | 置 CLOSED |
| T-07 `RESERVED → CLOSED` | 发现损坏，转维修 | **先撤销关联预约再置 CLOSED** |
| T-08 `OCCUPIED → CLOSED` | 使用中损坏，转维修 | **先办理签退再置 CLOSED** |
| T-09 `CLOSED → AVAILABLE` | 维修完成 | 置 AVAILABLE |

- **契约 v1.2（`docs/contract/接口契约.md:178-180`）**：只禁止 `OCCUPIED → AVAILABLE`，并明确写“`OCCUPIED -> CLOSED`（发现损坏转维修）允许直接改”。

**在 4.2 多预约模型下的两处补充说明**（图面不动，只补图下文字）：

- **T-05**：`OCCUPIED → AVAILABLE` 指“该座位已无其他未结束预约”。若签退时该座位还有其他未来 `BOOKED` 预约，汇总状态应回 `RESERVED`（仍有人预定了这个座位，只是此刻没人坐）。
- **T-07**：`RESERVED → CLOSED` 的“先撤销关联预约”指撤销该座位**全部** `BOOKED` 预约（含未来日期/时段），不只是当前看到的那一条；全部撤销后座位回 `AVAILABLE`，再按 T-06 置 `CLOSED`。

**关于两处口径**：实验5 的座位状态图本身没有问题，T-02~T-05 分别由学生（预约、取消/爽约）和值班管理员（签到、签退）触发，各自职责清晰，本单不主张修改。本单要约束的是**另一条旁路**——系统管理员在“座位管理”里手动调用 `updateSeatStatus`，它不属于 T-02~T-05 中的任何一个业务动作。

契约 v1.2 第 178-180 行允许 `OCCUPIED → CLOSED` 直接改，与 T-08“先办理签退再置 `CLOSED`”在这条旁路上冲突。B 侧按 T-08 实现（先签退再转维修），理由：契约原文那条要求“发现损坏即可直接转维修”，实现上必须同时处理在座记录、信用分、学生通知等一整套流程，当前范围撑不住。**契约正文不动**，请 A 在联调结论里确认以 T-08 为准的口径；如果 A 坚持契约原文，需要单独说明这套流程怎么落地。

### 5.2 建议的手动改状态矩阵（`SeatService.updateSeatStatus`）

`updateSeatStatus` 只承担“维护动作”，不承担业务流程：

| 当前状态 | 目标状态 | 结果 |
|---|---|---|
| 任意 | 与当前相同 | 允许，直接返回（no-op） |
| `AVAILABLE` | `CLOSED` | 允许（T-06） |
| `CLOSED` | `AVAILABLE` | 允许（T-09） |
| `OCCUPIED` | `AVAILABLE` | 拒绝，`SEAT_IN_USE`，“座位使用中，须先办理签退” |
| `OCCUPIED` | `CLOSED` | 拒绝，`SEAT_IN_USE`，“座位使用中，须先办理签退后才能转维修” |
| `RESERVED` | 任意 | 拒绝，`STATE`，“座位已被预约，不能手动改为「X」；请先取消该座位全部预约或等待签到/爽约处理” |
| 其他组合 | — | 拒绝，`STATE`，“不支持从「A」直接改为「B」” |

说明：这张矩阵只管系统管理员手动改状态这条旁路，**与 T-02~T-05 不冲突**——预约、取消、爽约、签到、签退仍走 `ReservationService`，不经过 `updateSeatStatus`。`RESERVED → CLOSED` 的级联（先取消预约再转维修）当前倾向放在调用方（界面）编排，`SeatService` 保持单一职责，不在 `updateSeatStatus` 内部偷偷改预约记录；最终归属待 A 确认（第八节问题 2）。

**“改成使用中”没有手动路径**：`OCCUPIED` 只能由值班管理员签到登记产生（T-04），系统管理员的下拉里不提供“使用中”这个候选。上表里 `AVAILABLE → OCCUPIED`、`RESERVED → OCCUPIED` 都落在“其他组合”，一律拒绝。

采纳 4.2 后，同一座位可能同时存在多条 `BOOKED` 预约。因此 `RESERVED → CLOSED` 的级联范围是“该座位全部 `BOOKED` 预约（含未来日期/时段）”：逐条取消后座位汇总状态自然回到 `AVAILABLE`，再按 T-06 置 `CLOSED`。不能只取消当前列表里的第一条，也不能在还有 `BOOKED` 时强行置 `CLOSED`，否则剩余预约到点仍会尝试签到。

级联取消有一个代码边界：`cancelReservation` 在超过签到截止时间后会抛 `DEADLINE`（`ReservationServiceImpl.java:146-148`）。若目标预约已过截止，应先执行 `handleNoShow`（按爽约扣 5 分、预约置 `NO_SHOW`、座位释放），再置 `CLOSED`；或者中止并提示管理员“该预约已过签到时间，请先执行爽约判定”。否则转维修会卡在半路。

### 5.3 必需的签到守卫（`ReservationService.signIn`）

这是现象 3 的直接修复点，**不是可选项**。只靠 5.2 矩阵和界面级联不够：`seats.csv` 被手工编辑、或以后新增入口，都可能让 `CLOSED` 座位上留下可签到的预约；服务层必须自己拦住。

位置：在“预约状态为 `BOOKED`”和“未过签到截止时间”两项校验之后、`reservation.checkIn()` 之前，读取座位并校验：

| 座位当前状态 | 结果 |
|---|---|
| 不存在 | `SEAT_NOT_FOUND` |
| `RESERVED` | 正常签到，签到后置 `OCCUPIED`（T-04） |
| `CLOSED` | `SEAT_UNAVAILABLE`，“座位 X 已转维修，无法签到；请联系系统管理员” |
| `OCCUPIED` | `SEAT_IN_USE`，“座位 X 当前为使用中，无法签到；请刷新后确认座位状态” |
| `AVAILABLE` | 允许签到并置 `OCCUPIED`（脏数据兜底：座位状态与预约记录脱钩时不阻断学生） |
| 其他 | `SEAT_UNAVAILABLE`，“座位 X 当前状态为「Y」，无法签到” |

拒绝时预约必须保持 `BOOKED`，不得写 `CheckinRecord`，不得改座位状态。

---

## 六、需要 A 处理的事项

1. **`SeatServiceImpl.updateSeatStatus` 按 5.2 矩阵重写**，补一个状态中文名辅助方法（服务层自用，不依赖 UI 的 `TableFactory`）。要点：`OCCUPIED` 不能改成任何状态；`RESERVED` 不能手动改成任何状态；`AVAILABLE ↔ CLOSED` 允许；不存在“手动改成使用中”的路径。
2. **`ReservationServiceImpl.signIn` 按 5.3 增加座位守卫**（必需），并保证拒绝路径无副作用：预约仍 `BOOKED`、无签到记录、座位状态不变。
3. **`ReservationServiceImpl` 按 4.3 改可用性判断与释放逻辑**（必修，现象 4）：
   - `listAvailableSeats` 只排除 `CLOSED`，其余按时段重叠判断；
   - `reserve` 只拒 `CLOSED`，重叠由 `occupied(...)` 返回 `SEAT_CONFLICT`；
   - 新增汇总重算辅助方法，取消/爽约/签退后按剩余有效预约重算 `AVAILABLE` / `RESERVED` / `OCCUPIED`；
   - `signOut` 后若仍有未来 `BOOKED` 预约，座位回 `RESERVED`。
4. **改写 `SeatAndAuthServiceTest.occupiedSeatCannotBeReleasedDirectlyButCanClose`**：`OCCUPIED → CLOSED` 改为断言失败且状态不变。
5. **补测试**：
   - `AVAILABLE ↔ CLOSED` 允许；
   - `RESERVED → OCCUPIED / CLOSED / AVAILABLE` 均抛 `STATE`；
   - `AVAILABLE → OCCUPIED` 抛 `STATE`；
   - `signIn` 遇 `CLOSED` 抛 `SEAT_UNAVAILABLE`，预约仍 `BOOKED`，无签到记录；
   - `signIn` 遇 `OCCUPIED` 抛 `SEAT_IN_USE`，预约仍 `BOOKED`；
   - 同一座位两条不重叠预约并存；重叠预约抛 `SEAT_CONFLICT`；
   - 取消一条后仍 `RESERVED`，取消最后一条回 `AVAILABLE`；
   - 签退后仍有未来预约回 `RESERVED`，否则回 `AVAILABLE`。
6. **确认状态图口径**，在联调结论或 PR 描述里写明“契约 178-180 行与 T-08 冲突，当前按 T-08 实现”，契约正文不改；并按 4.3 在实验5 图下补 T-05 / T-07 两句文字说明（图不重画）。

---

## 七、B 侧配套改动（待 A 确认后执行，本次未动代码）

1. `SysAdminMainFrame.doChangeSeatStatus`
   - `AVAILABLE` 只提供“维修”，`CLOSED` 只提供“可用”；
   - `OCCUPIED` 不提供任何候选，直接提示“须先由值班管理员办理签退”；
   - `RESERVED` 只提供“维修”，且先列出该座位的**全部** `BOOKED` 预约（预约号、学生、时段），用户确认后逐条取消；全部取消后座位汇总状态回 `AVAILABLE`（T-03），再调用 `updateSeatStatus` 置 `CLOSED`（T-06），不会触发 5.2 矩阵里的 `RESERVED` 拒绝。任一条取消失败即中止并提示，不留下“部分取消、座位仍 `RESERVED`”的中间态；
   - 若某条预约已过签到截止时间，`cancelReservation` 会抛 `DEADLINE`，此时先执行爽约判定（`handleNoShow`）再继续，或中止并提示管理员“该预约已过签到时间，请先执行爽约判定”；
   - 取消每一预约时都要在界面提示“已通知预约学生座位转维修”，对应学生端“我的预约”状态变化。
2. `StudentMainFrame`
   - “我的预约”增加“座位状态”列，展示 4.2 的汇总状态；
   - 表格上方增加提示条：座位为 `CLOSED` 的未结束预约，区分“已取消（因转维修）”与“已预约但座位维修中，无法签到”；
   - 查空座列表里，`RESERVED` 但所选时段不冲突的座位仍要显示，备注“该时段可选”，避免学生误以为整座不可约。
3. `DutyAdminMainFrame.doSignIn`
   - 签到前读取座位状态，`CLOSED` / `OCCUPIED` 直接拦截并提示，作为界面侧的提前反馈；服务层按 5.3 做最终校验，两层都保留。

---

## 八、需要 A 回答的问题

1. 座位状态流转以实验5 状态图为准（先签退/先取消再转维修），还是坚持契约 178-180 行“`OCCUPIED → CLOSED` 允许直接改”？
2. 若按状态图，`RESERVED → CLOSED` 的“先撤销该座位全部预约”放在界面编排还是 Service 新增编排方法？当前倾向界面编排，Service 不跨聚合改预约。
3. “通知学生”目前没有契约字段支撑（无消息表、无推送），B 侧只能做到“我的预约”里展示座位状态并在顶部提示；是否认可这一范围？
4. 契约 v1.2 标注已确认，如最终要改 178-180 行措辞，请走 `CONTRACT-CHANGE` 流程，本次不擅自修改。
5. 是否认可 4.2 / 4.3 的做法：状态图四态与 T-02~T-05 时机都不动，预约成功仍置 `RESERVED`；只把可用性改成按时段与 `BOOKED` / `CHECKED_IN` 判重叠，允许同一座位多条不重叠预约；系统管理员手动改状态按 5.2 矩阵收紧，签到按 5.3 加守卫。另请在实验5 图下补 T-05 / T-07 两句文字说明（图不重画、契约不改），并确认排期由谁定。
