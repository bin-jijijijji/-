# 智能安防系统（毕业论文原型）

## 1. 系统目标

实现一个 Web 端智能安防原型，包含：

- 权限登录（RBAC：管理员/操作员/查看者）
- 多路视频回放展示（用于论文演示，可替代实时摄像头）
- 基于矩形 ROI 区域的人脸识别
- 人脸名单管理（黑名单/白名单），命中黑名单触发告警并在 Web 端实时提醒

## 2. 总体架构（文字版）

- `backend`（Spring Boot）
  - 登录与权限控制（JWT）
  - 视频/ROI/样本管理接口
  - 接收识别服务回传的事件并落库
  - 通过 WebSocket(STOMP) 向前端推送告警
- `recognition-service`（FastAPI + insightface）
  - 根据 `recognition_jobs` 读取视频路径与 ROI
  - 逐帧抽样检测人脸并提取 embedding
  - 与 `face_embeddings` 做相似度匹配
  - 命中黑/白名单后回传事件到后端
- `MySQL`
  - 存储 RBAC、视频资产、ROI、名单 embedding、识别任务与事件

## 3. 关键数据流

1. 管理员上传样本图片，系统将图片交给识别服务提取 embedding，写入 `face_identities/face_embeddings`
2. 操作员在某一路视频上绘制矩形 ROI，并创建识别任务 `recognition_jobs`
3. 识别服务读取任务参数开始处理视频，命中 `blacklist` 触发 `blacklist_match` 事件
4. 后端保存事件到 `recognition_events`，并通过 WebSocket 广播前端告警列表

## 4. 本地部署（开发用）

### 4.1 准备 MySQL

- 创建数据库：`intelligent_security`
- 修改 `backend/src/main/resources/application.yml` 中的数据库用户名密码（如有需要）
- 运行后让 Spring Boot 根据 JPA 实体自动创建表（`ddl-auto: update`）

### 4.2 启动后端

在 `backend` 目录：

```powershell
mvn -DskipTests package
mvn spring-boot:run
```

后端默认端口：`8080`

### 4.3 启动识别服务

在 `recognition-service` 目录：

```powershell
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 5001
```

识别服务默认端口：`5001`

### 4.4 默认管理员账号

首次启动会自动初始化：

- 用户名：`admin`
- 密码：`admin123`

## 5. 论文可写点建议

- 阈值 `threshold` 对 `TP/FP/FN` 的影响（precision/recall/F1）
- ROI 限制 vs 全画面识别的误报差异
- 处理性能统计（FPS、平均耗时）

