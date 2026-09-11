# Demo 4：主动提醒

## 场景
构造一个「即将到期」的任务，验证 ReminderScheduler 每分钟扫描并发送 REMINDER 通知，
且**幂等**（同一任务不重复发送）。

## 构造步骤（管理员/后端操作）
1. 将某个 PENDING 任务的 `due_at` 改为近 N 分钟内到期：
   ```sql
   UPDATE tasks SET due_at = NOW() + INTERVAL 1 MINUTE,
                    status = 'PENDING', reminder_sent = 0
   WHERE id = <taskId>;
   ```
   （实际提醒窗口：`due_at <= now` 且未完成且 `reminder_sent=0` 的任务）
2. 等待 ReminderScheduler（cron `0 * * * * *`，每分钟）下一轮扫描。

## 期望结果
- notifications 表对应用户新增一条 `type=REMINDER`、`related_id=任务ID` 的通知。
- 任务 `reminder_sent` 置 1。
- 继续等待多轮 scheduler 运行，**通知条数不再增加**（幂等）。

## 前端展示
- 通知中心出现：`🔔 你的任务「X」即将到期，请尽快完成`（title/content 为 Rule 风格提示）。
- 展示可点击「标记已读」。

## 接口
- `GET /api/notifications`（列表）
- `GET /api/notifications/unread-count` → `{count}`
- `PUT /api/notifications/{id}/read` / `PUT /api/notifications/read-all`

## 幂等验证脚本（SQL 断言）
```sql
SELECT COUNT(*) FROM notifications WHERE type='REMINDER' AND related_id=<taskId>;
-- t0: 1；再等 2~3 分钟仍为 1
```

## 注意
- Demo 数据因 reminder_sent 幂等只发一次；复现需重新 `UPDATE ... reminder_sent=0`。
- 调度日志见 `campuspilot/startup.log`（ReminderScheduler 行）。