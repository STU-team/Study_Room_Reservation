# 演示数据（A-8 联调用）

覆盖 4 种座位状态与 5 种预约状态，用于界面演示与联调，**不参与自动初始化**（`DataInitializer.seedSamples` 仍只写 4 个 AVAILABLE 座位，避免影响单元测试）。

## 内容

| 文件 | 演示点 |
|---|---|
| `seats.csv` | A01 AVAILABLE、A02 RESERVED、B01 OCCUPIED、B02 CLOSED，四种状态齐全 |
| `seat_types.csv` | 靠窗区 2、沙发区 2，与座位数一致 |
| `students.csv` | 张三 99、李四 95（正常）、王五 70（演示 `CREDIT_LOW` 拒绝预约） |
| `users.csv` | `admin`（SYS_ADMIN）、`duty`（DUTY_ADMIN）、`20240001`（STUDENT），密码均为 `123456` |
| `reservations.csv` | 5 条，分别处于 BOOKED / CHECKED_IN / CHECKED_OUT / CANCELLED / NO_SHOW |
| `checkin_records.csv` | 2 条：`RES20261108002` 为在座（`actualEnd` 为空）、`RES20261108003` 已回填并记 `penalty=1` |
| `credit_score_logs.csv` | 2 条：迟到扣 1、爽约扣 5，`reservationId` 为真实散列值，可支持「点预约号跳回预约记录」 |

## 用法

把本目录 7 个 CSV 复制到程序的数据目录（默认仓库根 `data/`），启动前确保目录里只有这 7 个文件：

```powershell
$dst = "E:\Study_Room_Reservation\data"
Remove-Item -LiteralPath $dst -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $dst | Out-Null
Copy-Item -LiteralPath "E:\Study_Room_Reservation\docs\assets\demo-data\*.csv" -Destination $dst
```

或在代码里指定目录：`ServiceFactory.configure(Path.of("docs/assets/demo-data"))`（注意该目录会被写入，演示前先复制一份，不要直接在仓库的只读副本上操作）。

## 注意

- 文件编码必须 **UTF-8 无 BOM**：读取端用 `Files.readString(file, UTF_8)`，带 BOM 会让首列表头变成 `\uFEFFseatNo`，表头校验直接失败。
- 预约日期写的是 `2026-11-08`（相对 2026-11-07 的次日）。若演示日期已过，把 `reservations.csv` 的 `resDate`、`checkin_records.csv` 与 `credit_score_logs.csv` 的时间整体改到演示当天，否则签到会触发 `DEADLINE`（超过签到截止时间）。
- 座位 `status` 为单值字段，一套演示数据只能表现一个时段的占用情况。
