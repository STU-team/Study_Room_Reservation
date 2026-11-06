# PR 描述：dev-A → main（A-1 ~ A-6）

> **用法**：打开 `https://github.com/STU-team/Study_Room_Reservation/compare/main...dev-A?expand=1`，标题填下面「一、标题」，正文整段复制「二、正文」到描述框，右侧 Reviewer 选 `24mbye`，合并方式选 **Create a merge commit**。
> **状态**：本 PR 已创建，尚未合并；B 未评审。

## 一、标题

```
A-1~A-6：后端模型与实现（分工/用例/CIM/类图/状态图 + 实体 / CSV 持久层 / Service 业务逻辑）
```

## 二、正文

```markdown
## 这个 PR 做了什么

成员 A（易雨杰）从分工文档到后端 Service 层的完整交付，共 22 个提交、64 个文件（+4240 / -396）。

### 建模与文档（A-1 ~ A-4）
- A-1 分工文档与实验1 系统目标功能简述
- A-2 CIM-1/CIM-2/CIM-3（`CIM建模说明.md`、`CIM1-系统用例图.uml`）
- A-3 实验3 UML 类图：7 个业务类 + 4 个枚举 + 5 条关联，报告 md/docx 与 `.uml` 工程
- A-4 实验5 状态图：座位状态图、预约记录状态图，报告 docx 已定稿

### 后端实现（A-5 实体与 CSV 持久层）
- 4 个枚举、7 个实体、`CsvUtil` / `CsvMapper` / `CsvStore` / `DataInitializer`
- 七张 CSV 全覆盖：`seats`、`seat_types`、`reservations`、`students`、`users`、`checkin_records`、`credit_score_logs`
- `CsvStore` 统一表头锁定与行宽校验，DAO 只关心列映射，净减 475 行样板代码
- 首次启动由 `DataInitializer.seedSamples` 灌示例数据（4 座位 / 2 区域 / 2 学生 / admin、duty 两账号）

### 后端实现（A-6 Service 业务逻辑）
- `ReservationService`：listAvailableSeats / reserve / cancelReservation / signIn / signOut / handleNoShow
- `SeatService`：listAllSeats / findByNo / updateSeatStatus
- `AuthService`：login / logout（无状态，登录返回 `User` 由界面保存）
- `ServiceFactory` 静态单例，`configure(Path)` 可切数据目录，首次装配按依赖顺序 new 七个 DAO
- `BusinessException`（code + message）承载 12 个异常码；`util/DateTimes` 解决契约 `java.time` 与实体 `java.util.Date` 的落差
- 业务规则落点：信用分 <80 / 座位非 AVAILABLE / 时段重叠 / 签到截止 = 开始 +15 分钟 / 签退超时扣 1 分写 `LATE_CHECKOUT` / 爽约扣 5 分写 `NO_SHOW`

## CONTRACT-CHANGE（需要 B 评审确认）

本 PR 包含三处对已定稿契约的升级，均为 A 方提出、已在契约第十一节留下变更记录，请 B 在评审时确认：

1. **v0.7** — 第七节补 `CreditScoreLogDao`（save / findByStudentId / findAll）与 `SeatTypeDao`（findAll / findByTypeName），使第七节接口与第八节七张表一一对应。
2. **v0.8** — `reservations.csv` 表头末位补 `createTime`（`yyyy-MM-dd HH:mm`），让"预约提交时间"可保真，供 B 按提交时间排序。
3. **v0.9** — `CheckinRecordDao` 增加 `update(CheckinRecord)`，业务闭环改为**签到即落盘、签退回填**：签到写一条 `actualStart` 有值、`actualEnd` 为空的记录，在座名单可实时查；签退用 `update` 回填同一 `recordNo` 的 `actualEnd` 与 `penalty`。

> 契约原文依据：`docs/contract/接口契约.md`（v0.9）。若 B 不认可上述任一处，请在本 PR 评论指出，按 `CONTRACT-CHANGE` 流程调整后再改代码与文档。

## 怎么验证

```bash
# 需要 JDK 17+
./mvnw clean test          # 或 .\mvnw.cmd test（Windows PowerShell 需加 .\ 前缀）
```

预期输出：`Tests run: 31, Failures: 0, Errors: 0` + `BUILD SUCCESS`。

用例构成：

| 测试类 | 用例 | 覆盖 |
|---|---|---|
| `dao/CsvDaoRoundTripTest` | 6 | 七张表读写往返、`createTime` 保真、签到记录 `save`→`update` 回填 |
| `dao/CsvStudentDaoTest` | 3 | 学生查询与信用分更新 |
| `entity/EntityModelTest` | 6 | 实体状态流转（取消 / 签到 / 签退 / 爽约）与座位状态 |
| `service/ReservationServiceTest` | 8 | 预约、取消、签到、签退、爽约、信用分拦截、时段冲突、座位占用 |
| `service/SeatAndAuthServiceTest` | 4 | 座位列表/查询/状态更新、登录与无状态登出 |
| `util/CsvUtilTest` | 4 | CSV 转义、时间格式、编号散列 |

截图：`docs/assets/A5_测试通过截图.png`（19 用例阶段）、`docs/assets/A6_测试通过截图.png`（31 用例）。

## 给 Reviewer 的检查提示

- 界面层请只通过 `ServiceFactory` 取实例，不要 new 实现类，也不要直接碰 DAO 与 CSV（见 `docs/contract/对接-A6到后续B.md`）。
- 异常统一是 `BusinessException`，用 `getCode()` 分支处理，文案已定：座位使用中必须提示「座位使用中，须先办理签退」。
- 爽约扫描由界面用 `javax.swing.Timer` 触发（启动一次 + 每 60 分钟一次），Service 只提供 `handleNoShow(now)` 纯方法。
- `AuthService` 无状态：`login` 返回 `User` 交给界面保存，`logout` 不清理任何服务端字段。

## 已知边界（未在本 PR 处理）

- **座位复用限制**：`seats.csv` 的 `status` 是单值字段，座位一旦被预约，同日其他时段也查不到。若需按时段复用，要开 `CONTRACT-CHANGE` 重定座位状态口径。
- **未做规则**：不限制"过去时段"预约、不限制同一学生时段重叠（契约未规定），已写入 `docs/development/A6-实现记录.md` 遗留项。
- 后端没有 GUI 入口，业务流程的运行界面截图需等 B 的 Swing 界面接入后由 A-8 联调提供。

## 文档索引

| 文件 | 版本 | 作用 |
|---|---|---|
| `docs/contract/接口契约.md` | v0.9 | A/B 并行开发的唯一依据 |
| `docs/contract/对接-A6到后续B.md` | v1.1 | B 做界面时的 Service 对接约定 |
| `docs/contract/对接-A5到后续A6与B.md` | v1.3 | 持久层字段映射与遗留项 |
| `docs/development/A5-实现记录.md` | v1.3 | 实体与 DAO 实现记录 |
| `docs/development/A6-实现记录.md` | v1.1 | Service 实现记录与取舍 |
| `docs/进度看板.md` | 2026-11-06 | 全员进度、截图清单、阻塞项 |
| `docs/工作记录.md` | v2.7 | 逐条提交记录与待办 |
```

## 三、网页操作要点

1. 打开 compare 链接（`?expand=1` 会直接展开成创建页）。
2. 标题用上面「一、标题」；正文粘贴「二、正文」里的整段（含开头的 ```markdown 与结尾 ``` 不要粘进去）。
3. 右侧 **Reviewers** 勾 `24mbye`。
4. 合并方式保持 **Create a merge commit**（不要 Squash，分支图要留痕迹给实验八用）。
5. **不要自己点合并**，等 B 评审通过后由 B 或你合并；合并后再做 A-8。

## 四、修改旧 PR 描述

如果该 PR 已经创建过，直接进 PR 页面点描述右上角的铅笔图标 → 全选替换 → Save。标题同理点标题旁的 Edit。
