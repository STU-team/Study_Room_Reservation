# Study_Room_Reservation

> **文件作用**：项目总览与开发入口说明，包含项目目标、目录结构、构建方式、文档索引与 Git 协同规范。
> **课程**：CST9301 软件工程（汕头大学）
> **仓库**：https://github.com/STU-team/Study_Room_Reservation（团队仓，本地 `origin`）｜ https://github.com/yyj138/Study_Room_Reservation（成员A 个人仓）
> **创建时间**：2026-10-23
> **维护者**：易雨杰（成员A，架构与后端） / 叶梅冰（成员B，交互与前端）
> **当前版本**：v1.16（交卷前文档收尾：CIM-1 正文统一称「业务用例图」、实验8 报告 Word 提交明细同步到 40 条、实验7 报告按实际更新、仓库地址补个人仓）
> **版本更新说明**：
> - `v1.13`：交付 B-7（实验7 MDA 综合大报告）。
> - `v1.14`：交付 B-6（UML 与运行截图汇总）及 B-8（个人任务与感想、交叉审查）。
> - `v1.15`：A-8 联调收尾——10 项验收全部通过（含「签退后转维修成功」取证），`docs/assets/联调取证/` 定稿 36 张、`联调_运行界面.png` 换 2×3 六格汇总；`对接-A7到A8联调清单` 升 v1.5。
> - *注：v1.15 仅更新文档与截图证据，未改业务代码，测试数仍为 43。*
> **当前开发分支**：`dev-A`/`dev-B-new`

## 一、项目简介

自习室预约管理子系统，严格遵循 **MDA（模型驱动架构）** 规范，从 CIM（业务建模）到 PIM（系统分析）完成全流程 UML 建模（业务用例图、活动图、系统用例图、类图、状态图、时序图），并基于 **Java Swing** 实现桌面端系统。

系统面向高校免费公共自习室场景，覆盖座位查询、在线预约、签到登记、签退释放、座位区域与规则维护等核心业务，以信用分机制（初始100，爽约扣5，超时扣1，低于80禁约）约束预约行为。数据采用本地 CSV/TXT 文件持久化，无需部署数据库。

## 二、功能范围

| 使用角色 | 功能 |
|---|---|
| 学生 | 查询座位、在线预约、取消预约 |
| 值班管理员 | 签到登记、签退释放 |
| 系统管理员 | 座位区域与规则维护、座位状态维护、预约记录与信用分管理 |

## 三、技术栈与构建

- 语言/UI：Java 17 + Java Swing
- 构建：Maven（已内置 Maven Wrapper，无需本机安装 Maven）
- 测试：JUnit 5
- 持久化：本地 CSV/TXT 文件（`data/` 目录）
- 建模：StarUML 5.0（安装于 `D:\StarUML`）

```bash
# Windows
.\mvnw.cmd clean package

# macOS / Linux
./mvnw clean package
```

## 四、团队成员

| 角色 | 姓名 | 学号 | 职责 |
|---|---|---|---|
| 成员A | 易雨杰 | 2024611209 | 架构与后端：CIM-1/2、PIM-2/3、实体与 DAO、Service、实验1/3/5/8 报告 |
| 成员B | 叶梅冰 | 2024611038 | 交互与前端：CIM-3、PIM-4、Swing 界面、实验2/4/6 报告、期末统稿 |

## 五、目录结构

```text
Study_Room_Reservation/
├── pom.xml                     # Maven 构建配置
├── mvnw / mvnw.cmd             # Maven Wrapper
├── README.md                   # 项目总览（本文件）
├── docs/                       # 课程文档
│   ├── reports/                # 各次实验报告（md + docx）
│   ├── contract/               # 接口契约等团队约定文档
│   ├── assets/                 # 报告用图（UML 导出图、界面截图）
│   ├── uml/                    # StarUML 工程文件（.uml，模型源文件）
│   ├── development/            # 代码实现记录（A5-实现记录 等）
│   ├── 工作记录.md              # 开发过程工作记录
│   └── 进度看板.md              # A/B 任务状态与截图清单
├── data/                       # 本地数据文件（座位、预约记录等 CSV）
└── src/
    ├── main/
    │   ├── java/com/studyroom/
    │   │   ├── entity/          # 实体类（Seat、Reservation 等）
    │   │   ├── dao/            # 数据访问接口
    │   │   │   └── impl/       # 文件持久化实现
    │   │   ├── service/        # 业务逻辑接口
    │   │   │   └── impl/       # 业务逻辑实现
    │   │   ├── ui/             # Swing 界面
    │   │   └── util/           # 通用工具
    │   └── resources/          # 配置与静态资源
    └── test/java/com/studyroom/ # 单元测试
```

## 六、项目文档

| 文档 | 说明 |
|---|---|
| [实验1报告（Markdown）](docs/reports/实验1-系统目标及功能简述.md) | 系统目标及功能简述，GitHub 在线可读 |
| [实验1报告（Word）](docs/reports/实验1-系统目标及功能简述.docx) | 提交用版本（A4，标题1/2，小四/1.5 倍行距） |
| [实验2报告（Markdown）](docs/reports/实验2-系统用例图与用例叙述.md) | B-1 产出：系统用例图与 12 条用例叙述 |
| [实验4报告（Markdown）](docs/reports/实验4-签退活动图.md) | B-2 产出：签退活动图与异常分支说明 |
| [接口契约](docs/contract/接口契约.md) | 前后端核心类名、方法签名约定 |
| [工作记录](docs/工作记录.md) | 每次任务的过程记录（按实际完成人署名） |
| [进度看板](docs/进度看板.md) | A/B 任务状态、产出与截图清单 |
| [CIM 建模说明](docs/reports/CIM建模说明.md) | CIM-1/CIM-2 元素清单、分支条件与实验对应 |
| [A-2 到 B-1 对接说明](docs/contract/对接-A2到B1.md) | 交给成员B 的实验2 输入、命名约定与回传时间 |
| [实验3报告（Markdown）](docs/reports/实验3-UML类图.md) | UML 类图与类字典，7 业务类 + 4 枚举 |
| [A-3 到后续 B 对接说明](docs/contract/对接-A3到后续B.md) | 交给成员B 的类图口径、评审要点与后续任务影响 |
| [实验5报告（Markdown）](docs/reports/实验5-状态图.md) | UML 状态图（自习室预约管理子系统）：座位状态图与预约记录状态图 |
| [A-4 到后续 B 对接说明](docs/contract/对接-A4到后续B.md) | 交给成员B 的状态口径、评审要点与后续任务影响 |
| [A-5 实现记录](docs/development/A5-实现记录.md) | 实体、七张 CSV 持久层、字段映射约定与验证结果 |
| [A-5 到后续 A-6 与 B 对接说明](docs/contract/对接-A5到后续A6与B.md) | 持久化口径、A-6 编码约定、预约号格式与异常约定 |
| [A-6 实现记录](docs/development/A6-实现记录.md) | Service 层实现范围、业务规则落点、实现取舍与测试结果 |
| [A-6 到后续 B 对接说明](docs/contract/对接-A6到后续B.md) | 前端如何获取 Service、方法清单、异常码；B 的开工步骤、三张表列定义与刷新时机、演示注意 |
| [实验8 版本控制实践记录](docs/reports/实验8-版本控制.md) | Git 分支策略、PR 评审闭环、40 条提交明细与问题处理 |
| [实验8 报告（Word）](docs/reports/实验8-版本控制.docx) | 提交用版本，版式与实验5 定稿一致 |
| [A-7 到 A-8 联调清单](docs/contract/对接-A7到A8联调清单.md) | 界面动作对应 Service 方法、12 个异常码提示文案、演示数据用法、验收清单与已知边界 |
| [演示数据](docs/assets/demo-data/README.md) | 7 个 CSV，覆盖 4 种座位状态与 5 种预约状态，供 A-8 联调与答辩演示 |
| [实验6报告（Markdown）](docs/reports/实验6-在线预约时序图.md) | B-3 产出：在线预约时序图与设计讨论 |
| [实验6报告（Word）](docs/reports/实验6-在线预约时序图.docx) | 提交用版本 |
| [实验7报告（Markdown）](docs/reports/实验7-MDA综合大报告.md) | B-7 产出：CIM→PIM→PSM 全链路 MDA 综合报告，图1—图10、表1—表44，契约基线 v1.3 |
| [实验7报告（Word）](docs/reports/实验7-MDA综合大报告.docx) | 提交用版本 |
| [B-4 / B-5 实现记录](docs/development/B4-B5-实现记录.md) | Swing 主窗口骨架与事件绑定：窗口结构、公共样式层、界面动作→Service 调用、异常处理、刷新矩阵与验证结果 |
| [B→A 联调问题单（座位状态与签到）](docs/contract/联调问题-B向A-座位状态流转与签到校验.md) | 座位状态旁路、签到缺座位校验、单值状态导致跨时段不可复用；已确认，A-10 按契约 v1.3 处理 |
| [个人任务及感想（B-8）](docs/reports/05_个人任务及感想-B.md) | B-8 产出：个人任务索引、任务衔接体会、交叉审查记录（A-01～A-04）与证据索引；同名 `.docx` 为提交版 |

## 七、Git 协同规范

- `main` 为保护分支，只接受经过 Review 的合并。
- 成员A 在 `dev-A` 开发（架构与后端），成员B 在 `dev-B-new` 开发（交互与前端）。
- 每完成一个任务提交一次，通过 Pull Request 合并到 `main`，合并前需对方 Review。
- 提交信息前缀：`A-1:`/`B-1:` 完成任务，`docx:` 改 Word，`code:` 改代码，`docs:` 改文档，例如 `A-1: 初始化项目骨架与实验1报告`。
- 接口契约变更需双方确认，并在 PR 描述中标注 `CONTRACT-CHANGE`。
- 课程资料、教材 PDF 与他人作业等不纳入版本库，避免仓库膨胀与版权问题。

## 八、当前进度

- [x] A-1 项目初始化：仓库骨架、README、实验1报告（md + docx）
- [x] 接口契约 v0.4：免费公共自习室模式，信用分机制，去除费率与费用结算（CONTRACT-CHANGE）
- [x] 接口契约 v0.5：新增 `ReservationService.handleNoShow`（爽约自动判定）与 `SeatService.updateSeatStatus` 状态流转约束；信用分规则表补签到截止时间（CONTRACT-CHANGE）
- [x] 接口契约 v0.6：新增 `CreditScoreLog` 信用分流水与 `ReservationStatus.NO_SHOW`；`CreditScoreLog.reservationId` 为 Long、`SeatType.seatCount` 为 `Integer{derived}`（CONTRACT-CHANGE）
- [x] 接口契约 v0.7：第七节补 `CreditScoreLogDao`、`SeatTypeDao`，接口与第八节七张表一一对应（CONTRACT-CHANGE）
- [x] 接口契约 v0.8：`reservations.csv` 补 `createTime` 列，`Reservation.createTime`（预约提交时间）保真可还原（CONTRACT-CHANGE）
- [x] A-2 CIM-1 业务用例图（8 个用例、3 个参与者、系统边界；文件名沿用 `CIM1-系统用例图.uml`/`.jpg`，正文口径为「业务用例图」）
- [x] A-2 CIM-2 "在线预约"活动图（学生/系统两泳道、判断与异常分支）
- [x] A-3 总类图与类字典（7 个业务类 + 4 个枚举、6 条关联含 1 条聚合、4 条枚举依赖；含 CreditScoreLog 信用分流水与 ReservationStatus.NO_SHOW；StarUML 工程 `docs/uml/实验3_总类图.uml`，报告 md + docx，图见 `docs/assets/实验3_总类图.jpg`）
- [x] A-4 状态图：座位状态图（AVAILABLE/RESERVED/OCCUPIED/CLOSED 四状态 + 三条转维修分支，签到登记从 RESERVED 出发）与预约记录状态图（BOOKED/CHECKED_IN/CHECKED_OUT/CANCELLED/NO_SHOW 五状态 + 三条出口）；StarUML 工程 `docs/uml/实验5_状态图.uml`，报告 md + docx，图见 `docs/assets/实验5_座位状态图.jpg`、`docs/assets/实验5_预约记录状态图.jpg`
- [x] A-5 实体类与 CSV 持久层：4 个枚举、7 个实体、CSV 基础设施（`CsvUtil`/`CsvMapper`/`CsvStore`/`DataInitializer`），七张表 DAO 全覆盖（契约第七节 7 个接口全部实现）；七张表字段映射无有损（`createTime` 已按契约 v0.8 补列），类图主键与 CSV 业务编号的落差由 `CsvMapper` 稳定映射消化，预约姓名与手机号为预约时刻快照（见 `docs/development/A5-实现记录.md`）
- [x] A-6 Service 业务逻辑：`ReservationService`（查询可用座位、预约、取消、签到、签退、爽约扫描、预约/在座列表与按号查询）、`SeatService`（座位列表/查询/状态更新）、`AuthService`（登录/登出）、`StudentService`（学生档案与信用分）四个接口与实现，`ServiceFactory` 统一装配；`BusinessException` 带错误码，`DateTimes` 负责 `java.time` 与 `Date` 互转；签到即写 `CheckinRecord`、签退用 `CheckinRecordDao.update` 回填（契约 v0.9），预约窗口限制为 7 天内（v1.0），`listReservations`/`listAllReservations`/`listSeated`/`findReservation` 与 `Reservation.studentName`/`phone`（v1.1、v1.2）；`mvn test` 43 个用例通过（见 `docs/development/A6-实现记录.md`、`docs/contract/对接-A6到后续B.md`）
- [x] A-7 实验8 版本控制记录：分支策略（`dev-A` 单分支长期复用）、提交规范（`A-任务号:` 前缀、禁 emoji）、PR 评审闭环与 40 条提交明细表，附终端 `git log --graph` 分支拓扑截图 `docs/assets/A7_git_graph.png` 与 PR #12 页面截图 `docs/assets/A7_pr_screenshot.png`（见 `docs/reports/实验8-版本控制.md` 与同名 docx）
- [x] A-8 联调与主干合并收尾：`main`（`302bee7`）+ JDK 21 上真机走完《对接-A7到A8联调清单》第五节 10 项验收，2026-11-13 首轮 9 项、2026-11-14 在 C01 补完「签退后转维修成功」，**10 项全部通过**；`.\mvnw.cmd clean test` 43 个用例通过（BUILD SUCCESS）；取证图 36 张存 `docs/assets/联调取证/`，汇总图 `docs/assets/联调_运行界面.png`（2×3 六格）；清单升 v1.5（见 `docs/contract/对接-A7到A8联调清单.md`）
- [x] B-1 实验2 系统用例图与 12 条用例叙述（PR #7、#8 已合并）
- [x] B-2 实验4 "值班管理员签退"泳道活动图与实验4报告（含异常分支与信用分判定，md + docx 已落盘；PR #11 已合并）
- [x] B-3 实验6 "在线预约"时序图与实验6报告（含阶段分离、二次校验防并发设计，导出 UML 图与 StarUML 截图）
- [x] B-4 Swing 主窗口骨架：`AppMain` 入口（`ServiceFactory.configure` + 启动爽约扫描）、`LoginFrame` 登录页、学生端 3 卡、值班端 2 卡 + 侧栏、系统端 4 卡 + 侧栏；公共样式层 `UiTheme`（配色/字体/平铺按钮/底部标签栏）与 `TableFactory`（表头、行高、排序箭头、空状态、状态中文与配色）统一四端视觉（见 `docs/development/B4-B5-实现记录.md` 第二、四节；PR #13 已合并）
- [x] B-5 按钮事件绑定与 Service 调用：登录/退出、座位查询与预约/取消、我的预约、签到/签退、预约号搜索、在座名单、座位状态修改、信用分只读明细、爽约扫描；统一 `BusinessException` 提示与刷新矩阵，在座名单一次建索引、管理端表格双击快捷操作；“我的预约”补座位状态列（见 `docs/development/B4-B5-实现记录.md` 第三、五节；PR #13 已合并）
- [x] B→A 联调问题单：座位状态可被系统管理员绕过、签到缺座位校验、单值座位状态导致跨日期/时段不可复用；A 已确认并由 A-10 在契约 v1.3 落地（详见 `docs/contract/联调问题-B向A-座位状态流转与签到校验.md`）
- [x] B-7 实验7 MDA 综合大报告：按 CIM-1→CIM-2→CIM-3→PIM-1→PIM-2→PIM-3→PIM-4 统稿排版，形成 Markdown 与同名 Word
- [x] B-8 个人任务与感想、交叉审查 A 的图与报告：汇总 B-1～B-8 已完成任务索引；审查 A 侧实验1/3/5/8 报告与对应 UML 图，记录 A-01～A-04 四项差异，已由 A 在 PR #17 修订闭环。个人文档见 docs/reports/05_个人任务及感想-B.md（附录 B 含历史互审截图）。
- [~] B-6 运行截图、UML 图汇总与 3~5 分钟演示视频：前两项已完成（PR #16 合入 `docs/assets/汇总/`），演示视频待录
