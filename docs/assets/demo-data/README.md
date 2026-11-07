# 演示数据（A-8 联调用）

覆盖 5 个座位、5 种预约状态，用于界面演示与联调。**不参与自动初始化**（`DataInitializer.seedSamples` 仍只写 4 个 AVAILABLE 座位，避免影响单元测试）。

## 先用生成器，不要手改日期

预约按业务规则「最多提前 7 天」，静态 CSV 里的日期过几天就会失效（签到会报 `DEADLINE`）。所以演示前请重新生成：

```powershell
cd E:\Study_Room_Reservation
java tools\GenerateDemoData.java data
```

生成器按**运行当天**推算日期，输出 7 个 CSV 到 `data/`（该目录已在 `.gitignore` 里，不入库），并打印本次要用的三个演示单号。目录里的 CSV 只是某一次生成的样例，用于查看结构。

## 内容

| 文件 | 演示点 |
|---|---|
| `seats.csv` | A01 AVAILABLE、A02 RESERVED、B01 OCCUPIED、B02 CLOSED、C01 RESERVED，四种状态齐全 |
| `seat_types.csv` | 靠窗区 2、沙发区 2、研讨区 1，与座位数一致 |
| `students.csv` | 张三 99、李四 95（正常）、王五 70（演示 `CREDIT_LOW` 拒绝预约） |
| `users.csv` | `admin`（SYS_ADMIN）、`duty`（DUTY_ADMIN）、`20240001`（STUDENT），密码均为 `123456` |
| `reservations.csv` | 6 条：BOOKED（今天 +3 天）、CHECKED_IN、CHECKED_OUT、CANCELLED、NO_SHOW，外加 1 条已过期仍未处理的 BOOKED |
| `checkin_records.csv` | 2 条：一条在座（`actualEnd` 为空）、一条已回填并记 `penalty=1` |
| `credit_score_logs.csv` | 2 条：迟到扣 1、爽约扣 5，`reservationId` 为真实散列值，支持「点预约号跳回预约记录」 |

## 三条主线的演示路径

| 演示动作 | 用哪条 | 为什么不会失效 |
|---|---|---|
| 预约 → 签到 | 生成器打印的 `RES…001`（今天 +3 天 09:00） | 在 7 天窗口内，签到截止时间也在未来 |
| 签退 | `CHECKED_IN` 那条（昨天 09:00-11:00） | 签退只校验「签退时刻不早于开始时刻」，日期会被替换成预约日期，传 09:00 之后任意时刻都通过 |
| 爽约扫描 | 昨天 08:00 仍 `BOOKED` 的那条 | 签到截止（08:15）早已过去，`handleNoShow(now)` 必定命中 |

## 注意

- 文件编码 **UTF-8 无 BOM**：读取端用 `Files.readString(file, UTF_8)`，带 BOM 会让首列表头变成 `\uFEFFseatNo`，表头校验直接失败。
- 座位 `status` 为单值字段，一套演示数据只表现一个时段的占用情况。
- 演示爽约会改写 CSV（座位释放、扣分落盘）。想要可重复演示，重新跑一次生成器即可。
- 直接 `ServiceFactory.configure(Path.of("docs/assets/demo-data"))` 会**写坏仓库里的样例**，请改用生成器输出到 `data/`。
