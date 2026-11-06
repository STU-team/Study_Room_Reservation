# PR #12 描述（最终版，可直接粘贴到 GitHub）

> **用途**：`main ← dev-A` 的 Pull Request 描述。合并方式必须为 **Create a merge commit**（不使用 Squash，保留 27 个提交供实验8 举证）。
> **用法**：打开 PR 页面点标题右侧铅笔编辑，**页面原有的 A-4 描述不要删除**，把下面「正文」整段追加在末尾，用 `---` 分隔。
> **注意**：文档名一律用仓库中的真实文件名（`对接-A5到后续A6与B.md`、`对接-A6到后续B.md`）。

## 建议标题

```
A-4 + A-5 + A-6 + A-7：状态图文档 + CSV持久层DAO + Service业务层 + Git版本控制实验记录
```

## 正文

```markdown
> 本 PR 承接 A-4 的评审线程，A-5 / A-6 / A-7 继续推送至 `dev-A`，不新建 PR。
> 合并方式：Create a merge commit，**不要 Squash**，保留完整提交历史（实验8 需要）。

## 本 PR 前半部分（A-4，原描述已保留）

实验5 UML 状态图：`docs/uml/实验5_状态图.uml`、导出图、`docs/reports/实验5-状态图.md` 与 `.docx`、对接文档 `docs/contract/对接-A4到后续B.md`。

---

## 新增：A-5 实体与 CSV 持久层

- 7 个业务实体、4 枚枚举，7 张 CSV 表 DAO 全覆盖；首次运行自动建表头并写入示例数据（`seedSamples`）。
- 单元测试：19 个用例通过；截图 `docs/assets/A5_测试通过截图.png`。
- 实现记录：`docs/development/A5-实现记录.md`；对接文档：`docs/contract/对接-A5到后续A6与B.md`。

## 新增：A-6 Service 业务层

- 三接口全部实现：`ReservationService`（listAvailableSeats / reserve / cancelReservation / signIn / signOut / handleNoShow）、`SeatService`（listAllSeats / findByNo / updateSeatStatus）、`AuthService`（login / logout）；统一装配入口 `ServiceFactory`。
- **CONTRACT-CHANGE v0.9**：`CheckinRecordDao` 新增 `update(CheckinRecord)`；签到即落盘（`actualEnd`、`penalty` 为空），签退更新同一条记录回填结束时间与扣分。CSV 表头与实体字段不变，查 `actualEnd` 为空的记录即可得到实时在座人员。
- 与前端的分工约定（请 B 确认）：
  1. 爽约扫描：A 只提供纯方法 `handleNoShow(now)`，定时器由前端 `javax.swing.Timer` 实现——程序启动调一次，之后每 60 分钟一次。
  2. `AuthService` 无状态，`login()` 返回 User 对象，登录态由前端自行保存，不提供 `currentUser()`。
- 单元测试：31 个用例通过；截图 `docs/assets/A6_测试通过截图.png`。
- 实现记录：`docs/development/A6-实现记录.md`；对接文档：`docs/contract/对接-A6到后续B.md`。

## 新增：A-7 实验8 版本控制记录

- `docs/reports/实验8-版本控制.md`（v1.2）与 Word 版 `docs/reports/实验8-版本控制.docx`（提交用定稿版式）。
- 分支拓扑截图 `docs/assets/A7_git_graph.png`（本地 `git log --graph --oneline --branches` 输出，可见 `dev-A`、`dev-B-new`、`origin/main` 与合并节点）；PR 页面截图 `docs/assets/A7_pr_screenshot.png`。
- 内容：分支策略、25 条提交明细表、PR 评审闭环、问题与处理、分析与小结。

## 全部 CONTRACT-CHANGE（请 B 重点确认，不认可请在评论区指出，代码随之调整）

1. **v0.7**：契约第七节新增 `CreditScoreLogDao`、`SeatTypeDao` 两个 DAO 接口。
2. **v0.8**：`reservations.csv` 末位补 `createTime` 列（预约提交时间可保真还原）。
3. **v0.9**：`CheckinRecordDao` 增加 `update`；口径改为「签到即落盘、签退回填」，使在座名单可实时查询，CSV 表头不变。

## 验证方式

```powershell
.\mvnw.cmd clean test
```

预期：`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

## 已知边界

1. 座位 `status` 为单值字段（契约第八节），同一座位不支持同一天多时段并行预约。
2. A 侧代码无 GUI 入口，业务跑通证据以单元测试截图呈现；界面交互在 A-8 联调阶段由 B 的前端承担。
3. 后端不启后台线程，爽约轮询由前端 Swing Timer 触发。
4. 预约未限制「过去时段」与「同一学生时段重叠」，契约未规定，已记入 `A6-实现记录.md` 遗留项。

## 文档版本索引（均已在本次推送内）

| 文档 | 版本 |
|---|---|
| `docs/contract/接口契约.md` | v0.9 |
| `docs/development/A5-实现记录.md` | v1.3 |
| `docs/development/A6-实现记录.md` | v1.1 |
| `docs/contract/对接-A5到后续A6与B.md` | v1.3 |
| `docs/contract/对接-A6到后续B.md` | v1.1 |
| `docs/reports/实验8-版本控制.md` | v1.2 |
| `docs/reports/实验3-UML类图.md` / `实验5-状态图.md` | v1.3 / v1.2 |
| `docs/工作记录.md` / `docs/进度看板.md` / `README.md` | v3.0 / 11-07 / v1.2 |

## 评审人

Reviewer：`24mbye`（成员B）
> 请勿直接合并，等 B 评审完成并对三处 CONTRACT-CHANGE 表态后再执行合并。
```

## 网页操作要点

1. 标题按上面替换；正文把页面原有 A-4 段落保留在上方，追加「正文」区块。
2. Reviewers 右侧保持指定 `24mbye`。
3. 底部合并按钮选 **Create a merge commit**，不要 `Squash and merge`、不要 `Rebase and merge`。
4. 保存后无需再推代码，`dev-A` 已全部推送；B 提出修改意见时继续在 `dev-A` 提交，PR 会自动刷新。
