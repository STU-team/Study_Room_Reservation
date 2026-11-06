# 演示数据（A-8 联调用）

覆盖 5 种座位状态用法、5 种预约状态，用于界面演示与联调，**不参与自动初始化**（`DataInitializer.seedSamples` 仍只写 4 个 AVAILABLE 座位，避免影响单元测试）。

## 内容

| 文件 | 演示点 |
|---|---|
| `seats.csv` | A01 AVAILABLE、A02 RESERVED、B01 OCCUPIED、B02 CLOSED、C01 RESERVED（订了远期时段的座位），四种状态齐全 |
| `seat_types.csv` | 靠窗区 2、沙发区 2、研讨区 1，与座位数一致 |
| `students.csv` | 张三 99、李四 95（正常）、王五 70（演示 `CREDIT_LOW` 拒绝预约） |
| `users.csv` | `admin`（SYS_ADMIN）、`duty`（DUTY_ADMIN）、`20240001`（STUDENT），密码均为 `123456` |
| `reservations.csv` | 6 条：BOOKED（远期 2027-03-15）、CHECKED_IN、CHECKED_OUT、CANCELLED、NO_SHOW，外加 1 条**已过期但仍 BOOKED** 的记录 |
| `checkin_records.csv` | 2 条：`RES20261106002` 在座（`actualEnd` 为空）、`RES20261106001` 已回填并记 `penalty=1` |
| `credit_score_logs.csv` | 2 条：迟到扣 1、爽约扣 5，`reservationId` 为真实散列值，支持「点预约号跳回预约记录」 |

## 日期设计（为什么这样排）

| 用途 | 记录 | 为什么不会过期 |
|---|---|---|
| 演示「预约→签到」 | `RES20270315001`（2027-03-15 09:00） | 预约在 **4 个多月后**，签到截止时间也在未来，随时点签都能成功 |
| 演示「签退」 | `RES20261106002`（2026-11-06 09:00，`CHECKED_IN`） | 签退只校验「签退时刻不早于开始时刻」，日期会被替换成预约日期，传 `09:00` 之后的任何时刻都能通过 |
| 演示「爽约扫描」 | `RES20261106003`（2026-11-06 08:00，仍 `BOOKED`） | 签到截止 08:15 早已过去，`handleNoShow(now)` 一定命中，转 `NO_SHOW` 并扣 5 分 |
| 历史记录展示 | `CHECKED_OUT`、`CANCELLED`、`NO_SHOW` 各一条 | 只是列表展示，不参与操作 |

## 用法

把本目录 7 个 CSV 复制到程序的数据目录（默认仓库根 `data/`），启动前确保目录里只有这 7 个文件：

```powershell
$dst = "E:\Study_Room_Reservation\data"
Remove-Item -LiteralPath $dst -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $dst | Out-Null
Copy-Item -LiteralPath "E:\Study_Room_Reservation\docs\assets\demo-data\*.csv" -Destination $dst
```

或在代码里指定目录：`ServiceFactory.configure(Path.of("docs/assets/demo-data"))`——注意该目录**会被写入**，演示前先复制一份到临时目录，不要直接在仓库的这份副本上跑。

## 注意

- 文件编码必须 **UTF-8 无 BOM**：读取端用 `Files.readString(file, UTF_8)`，带 BOM 会让首列表头变成 `\uFEFFseatNo`，表头校验直接失败。
- 只有 `RES20270315001` 是为「长期可演示」设计的远期预约；若课程拖到 2027-03 之后，把它改成更远的日期即可，其余记录都是历史数据、不受影响。
- 座位 `status` 为单值字段，一套演示数据只能表现一个时段的占用情况。
- 演示爽约后会改写 CSV（座位释放、扣分落盘），想要可重复演示就重新复制一遍本目录。
