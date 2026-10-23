# Hotel-Booking-System

> **文件作用**：项目总览与开发入口说明，包含项目目标、目录结构、构建方式、文档索引与 Git 协同规范。
> **课程**：CST9301 软件工程（汕头大学）
> **仓库**：https://github.com/yyj138/Hotel-Booking-System
> **创建时间**：2026-10-23
> **维护者**：易雨杰（成员A，架构与后端） / 成员B（交互与前端）
> **当前版本**：v0.1（A-1 项目初始化）
> **当前开发分支**：`dev-A`

## 一、项目简介

酒店订房管理子系统，严格遵循 **MDA（模型驱动架构）** 规范，从 CIM（业务建模）到 PIM（系统分析）完成全流程 UML 建模（业务用例图、活动图、系统用例图、类图、状态图、时序图），并基于 **Java Swing** 实现桌面端系统。

系统覆盖房态查询、在线预订、入住登记、退房结算、房型房价与房态维护等核心业务，数据采用本地 CSV/TXT 文件持久化，无需部署数据库。

## 二、功能范围

| 使用角色 | 功能 |
|---|---|
| 顾客 | 查询房态、在线预订、取消预订 |
| 前台 | 办理入住、退房结算 |
| 管理员 | 房型房价维护、房态维护、订单管理 |

## 三、技术栈与构建

- 语言/UI：Java 17 + Java Swing
- 构建：Maven（已内置 Maven Wrapper，无需本机安装 Maven）
- 测试：JUnit 5
- 持久化：本地 CSV/TXT 文件（`data/` 目录）

```bash
# Windows
.\mvnw.cmd clean package

# macOS / Linux
./mvnw clean package
```

## 四、目录结构

```text
Hotel-Booking-System/
├── pom.xml                     # Maven 构建配置
├── mvnw / mvnw.cmd             # Maven Wrapper
├── README.md                   # 项目总览（本文件）
├── docs/                       # 课程文档
│   ├── reports/                # 各次实验报告（md + docx）
│   ├── contract/               # 接口契约等团队约定文档
│   └── 工作记录.md              # 开发过程工作记录
├── data/                       # 本地数据文件（房间、订单等 CSV）
└── src/
    ├── main/
    │   ├── java/com/hotel/
    │   │   ├── entity/         # 实体类（Room、BookingOrder 等）
    │   │   ├── dao/            # 数据访问接口
    │   │   │   └── impl/       # 文件持久化实现
    │   │   ├── service/        # 业务逻辑接口
    │   │   │   └── impl/       # 业务逻辑实现
    │   │   ├── ui/             # Swing 界面
    │   │   └── util/           # 通用工具
    │   └── resources/          # 配置与静态资源
    └── test/java/com/hotel/    # 单元测试
```

## 五、项目文档

| 文档 | 说明 |
|---|---|
| [实验1报告（Markdown）](docs/reports/实验1-系统目标及功能简述.md) | 系统目标及功能简述，GitHub 在线可读（含 Mermaid 图） |
| [实验1报告（Word）](docs/reports/实验1-系统目标及功能简述.docx) | 提交用版本 |
| [接口契约](docs/contract/接口契约.md) | 前后端核心类名、方法签名约定 |
| [工作记录](docs/工作记录.md) | 每次任务的过程记录 |

## 六、Git 协同规范

- `main` 为保护分支，只接受经过 Review 的合并。
- 成员A 在 `dev-A` 开发（架构与后端），成员B 在 `dev-B` 开发（交互与前端）。
- 每完成一个任务提交一次，通过 Pull Request 合并到 `main`，合并前需对方 Review。
- 提交信息格式：`任务编号: 简要说明`，例如 `A-1: 初始化项目骨架与实验1报告`。
- 接口契约变更需双方确认，并在 PR 描述中标注 `CONTRACT-CHANGE`。

## 七、当前进度

- [x] A-1 项目初始化：仓库骨架、README、实验1报告（md + docx）
- [x] 接口契约 v0.1（草案）
- [ ] A-2 CIM 业务用例图与在线预订活动图
- [ ] A-3 总类图与类字典
- [ ] A-4 状态图
- [ ] A-5 实体类与 DAO
- [ ] A-6 Service 业务逻辑
