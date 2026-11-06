# 对接说明：实体与 CSV 持久层（A-5）-> A-6 Service 与 B 前端

> **文件作用**：成员A 把 A-5 的实体定义与实际落盘格式交接给后续开发。A-6 写 Service 时按本文件知道「实体怎么造、DAO 怎么调、字段从哪来」；B 写前端时按本文件知道「界面拿到的是哪些字段、状态值是什么、编号长什么样」。B 评审时对照本文件与 `docs/development/A5-实现记录.md` 即可，不需要再问 A 某个字段存不存。
> **课程**：CST9301 软件工程　｜　**编写**：易雨杰（成员A，2024611209）　｜　**接收**：叶梅冰（成员B，2024611038）、A-6 编码
> **日期**：2026-11-06　｜　**版本**：v1.2（契约 v0.8 补 `createTime` 列后同步）　｜　**状态**：A-5 已完成，19 个单元测试通过，待 B 评审

## 一、A 已经交付什么

| 文件 | 说明 |
|---|---|
| `docs/development/A5-实现记录.md` | A-5 实现记录：实体状态方法、七张 CSV 的 DAO 覆盖、字段映射约定、遗留项 |
| `src/main/java/com/studyroom/entity/` | 7 个实体 + 4 个枚举（`Role`、`SeatStatus`、`ReservationStatus`、`CreditChangeReason`） |
| `src/main/java/com/studyroom/util/CsvUtil.java` | CSV 读写：UTF-8、引号/逗号/换行转义、整表读写、追加、表头校验、原子替换 |
| `src/main/java/com/studyroom/util/CsvMapper.java` | 日期时间格式 `yyyy-MM-dd HH:mm`、业务编号到 Long 主键的稳定散列 |
| `src/main/java/com/studyroom/util/DataInitializer.java` | 按契约 v0.6 初始化七张 CSV 表头，不覆盖已有数据 |
| `src/main/java/com/studyroom/dao/` | 契约 v0.7 第七节 7 个 DAO 接口 + `DataAccessException` |
| 7 个 CSV 实现类：`CsvSeatDao`、`CsvReservationDao`、`CsvStudentDao`、`CsvUserDao`、`CsvCheckinRecordDao`、`CsvCreditScoreLogDao`、`CsvSeatTypeDao` | 七个实现类分别 implements 第七节对应接口，`CsvStore` 统一负责读写与表头校验 |
| `src/test/java/com/studyroom/` | 4 个测试类共 19 个用例，`mvn test` 全通过 |

## 二、持久化口径（请 B 重点核对）

接口契约 v0.6 未做任何修改，CSV 表头严格按第八节。类图的主键字段与 CSV 业务编号字段之间的落差，全部在 DAO 内映射消化：

| 表 | 表头（契约 v0.6 原文） | 关键映射 |
|---|---|---|
| `seats.csv` | `seatNo,typeName,status` | `seatId` 由 `seatNo` 稳定散列；`roomName` 与 `typeName` 同义复用 |
| `seat_types.csv` | `typeName,seatCount` | 区域清单取自本表；`seatCount` 是派生值，读取时按 `seats.csv` 实时统计，列值仅冗余 |
| `reservations.csv` | `resNo,studentName,studentId,phone,seatNo,resDate,startTime,endTime,status,createTime` | `reservationId` 由 `resNo` 散列；`createTime` 有独立列（契约 v0.8），写入时为空则取 `startTime` 兜底；`signInDeadline` 按 `startTime+15 分钟` 还原；学生姓名与电话为写入时快照 |
| `students.csv` | `name,studentId,phone,creditScore` | 一一对应，无映射 |
| `users.csv` | `username,password,role` | `userId` 由 `username` 散列；`name` 读入时取 `username` |
| `checkin_records.csv` | `recordNo,resNo,actualStart,actualEnd,penalty,createTime` | 一一对应（实体已补 `resNo`、`createTime` 字段） |
| `credit_score_logs.csv` | `logId,studentId,reservationId,changeValue,reason,createTime` | 一一对应，`logId` 由 DAO 自增 |

- 时间为 `yyyy-MM-dd HH:mm`；枚举一律写英文常量名（`BOOKED`、`NO_SHOW`、`LATE_CHECKOUT` 等）。
- 空值写空串；`reservationId`（信用分流水里的可空外键）为空即表示管理员手工调整。
- 数据库级唯一性：座位号、学号、预约号、签到记录号在同一文件内不允许重复，重复即抛 `DataAccessException`。

## 三、A-6 编码时请重点注意

1. **`Reservation` 保存前的必填字段**：`resNo`（可先调 `reservationDao.nextResNo()`）、`studentId`、`seatNo`、`startTime`、`endTime`、`status`。缺任一项 DAO 会抛 `IllegalArgumentException` 或 `DataAccessException`，这是刻意设计，避免写出半条记录。
2. **预约号格式**：`RES + yyyyMMdd + 三位序号`（如 `RES20261106001`），由 `CsvReservationDao.nextResNo()` 按当天已有最大序号 +1 生成，与契约示例一致。
3. **同一预约只能有一条签到记录**：`CsvCheckinRecordDao.save` 对重复 `resNo` 直接拒绝，签退再写第二条会失败，A-6 需在签到时就创建记录、签退时只更新内存对象。
4. **座位状态转移由 Service 校验**：`Seat.updateStatus` 只赋值，`OCCUPIED` 不得直接改 `AVAILABLE`（契约 v0.5 约束）要在 `SeatService` 里判断。
5. **信用分流水与扣分是同一步**：`handleNoShow` 扣 5 分写 `NO_SHOW`，`checkOut` 超时扣 1 分写 `LATE_CHECKOUT`，两笔都用 `CsvCreditScoreLogDao.save` 落盘，`reservationId` 填对应预约的 Long 主键。
6. **`CreditScoreLogDao`、`SeatTypeDao` 已进契约 v0.7**：两个接口已在第七节声明，实现类分别 implements，A-6 直接按接口注入即可。
7. **`Reservation.createTime` 已落盘（契约 v0.8）**：`reservations.csv` 末位新增 `createTime` 列，`save` 时若实体未设值则取 `startTime` 写入，读取原样还原；A-6 做“我的预约”列表按提交时间排序时可直接用 `Reservation.getCreateTime()`。

## 四、对 B 后续工作的影响

| B 的任务 | 与本持久层的关系 |
|---|---|
| B-3 实验6 在线预约时序图 | 时序图里 `SeatDao.findByNo` → `ReservationDao.nextResNo` → `save` 这条链路的返回值与异常，按本文件第三节执行 |
| B-4/B-5 Swing 前端 | 列表展示用 `resNo`、`seatNo`、学生快照字段；预约号按 `RES20261106001` 格式展示，不要自己拼日期 |
| B-6 运行截图与演示 | 首次启动会自动建 `data/` 下七张 CSV，演示前不要手动改表头，否则 `DataInitializer` 会拒绝启动 |
| B-7 大报告统稿 | 存储层描述直接引用本文件第二节表格，不必另写一套字段说明 |

## 五、不需要 B 做的事

- 不要改 `data/*.csv` 的表头与列顺序；如需新增列，先在 PR 评论提出，由 A 按 `CONTRACT-CHANGE` 流程改契约后再动代码。
- 不要在前端直接读写 CSV；前端只通过 `service` 层接口拿数据（契约第一节第 2 条）。
- 不要在实体里加界面专用字段；界面需要的展示值由 Service 组装。

## 六、回传与依赖

| 事项 | 谁 | 时间 |
|---|---|---|
| A-5 实体与持久层交付 | 易雨杰 | 已完成（19 个单测通过，待 PR 合并） |
| B 评审字段映射与预约号格式 | 叶梅冰 | 收到 dev-A PR 后 |
| 确认是否把 `CreditScoreLogDao`、`SeatTypeDao` 升级进契约 | 叶梅冰 + 易雨杰 | A-6 开工前 |
| A-6 Service 实现 | 易雨杰 | 契约确认后开始 |
