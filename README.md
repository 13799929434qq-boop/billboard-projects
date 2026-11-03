# 公路广告牌管理系统（Vue 3 + Spring Boot + MySQL）

政企内部业务系统。按角色划分功能：运营/档案员、合同/财务、系统管理员、领导（报表只读）。

## 技术栈

| 端 | 技术 |
|---|---|
| 前端 | Vue 3 + Vite + TypeScript + Element Plus + Pinia + Vue Router + ECharts |
| 后端 | Spring Boot 3（JDK 17）+ MyBatis-Plus + Spring Security（JWT）+ Spring Data Redis + EasyExcel + knife4j |
| 数据库 | MySQL 8 |
| 缓存 | Redis 6+（可选，用于热点数据缓存与登录防爆破锁定；未启动时自动降级直连数据库，不影响使用） |

## 目录结构

```
billboard-vue-spring/
├── backend/          # Spring Boot 后端（单体 jar，端口 8080）
├── frontend/         # Vue 3 前端（Vite，开发端口 5173，代理到 8080）
└── sql/
    ├── schema.sql    # 建库建表
    └── data.sql      # 演示种子数据（含 4 个账号）
```

## 演示账号

| 账号 | 密码 | 角色 | 主要菜单 |
|---|---|---|---|
| dangan | dangan123 | 运营/档案员 | 路段档案、广告牌档案、数据字典 |
| caiwu | caiwu123 | 合同/财务 | 商家档案、合同管理、应收管理、资金流水、经营报表 |
| admin | admin123 | 系统管理员 | 全部菜单 + 账号管理 + 操作日志 |
| lingdao | lingdao123 | 领导 | 经营报表（只读） |

## 启动步骤

### 1. 准备数据库

```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/data.sql
```

如果是在已有数据库上升级本版本（不重新执行 `schema.sql`），需先补全年审字段：

```bash
mysql -u root -p billboard < sql/migration-20261103-compliance.sql
mysql -u root -p billboard < sql/migration-20261103-billboard-change.sql
```

### 2. 启动后端（JDK 17 + Maven）

```bash
cd backend
# 如数据库账号密码非 root/root，修改 src/main/resources/application.yml 或用环境变量：
# export DB_URL=jdbc:mysql://localhost:3306/billboard?...
# export DB_USERNAME=root  DB_PASSWORD=xxx
mvn spring-boot:run
```

- 接口文档（knife4j）：http://localhost:8080/doc.html
- 广告牌照片上传目录：后端工作目录下 `uploads/`（可用 `UPLOAD_DIR` 环境变量改）

### 2.1 Redis（可选）

后端集成了 Redis，用于两个场景：

1. **热点数据缓存**：表单下拉联动数据（路段/广告牌/商家/合同）缓存 5 分钟，业务写操作后自动失效
2. **登录防爆破**：同一账号连续失败 5 次锁定 10 分钟，登录成功自动清零

```bash
# 本地启动 Redis（Docker 最方便）
docker run -d --name redis -p 6379:6379 redis:7

# 非默认地址用环境变量覆盖：
# export REDIS_HOST=127.0.0.1 REDIS_PORT=6379 REDIS_PASSWORD=xxx REDIS_DB=0
```

> 未安装 Redis 也能正常启动和运行：所有缓存/锁定操作带降级容错，连接失败时自动回退为直连数据库、不锁定账号，仅日志输出 warn。

### 3. 启动前端（Node 18+ / pnpm）

```bash
cd frontend
pnpm install
pnpm dev        # http://localhost:5173 ，已配置 /api 代理到 8080
```

### 4. 生产部署

```bash
cd frontend && pnpm build        # 产出 dist/
cd backend && mvn package -DskipTests   # 产出 target/billboard-backend-1.0.0.jar
java -jar target/billboard-backend-1.0.0.jar
```

Nginx 托管 `frontend/dist`，并反向代理 `/api` 与 `/uploads` 到 8080：

```nginx
location /api/     { proxy_pass http://127.0.0.1:8080; }
location /uploads/ { proxy_pass http://127.0.0.1:8080; }
location /         { root /path/to/dist; try_files $uri /index.html; }
```

## 核心业务规则

1. **一份合同绑定多块广告牌**，每块牌每年单独定价；合同生效时按「年 × 牌」自动生成应收账单（`biz_receivable`）。
2. **同一块牌**在生效合同期间不允许时间重叠（签约/生效时校验）。
3. **两本账分离**：应收（`biz_receivable`）与实收流水（`biz_transaction`）分表存储；收款登记在同一事务内「冲减应收 + 追加流水 + 记操作日志」，行锁防并发超收，支持部分缴纳。
4. **三个桩号**：合同桩号 / 实际桩号 / 路政批复桩号独立字段（`biz_billboard.stake_*`）。
5. **合规红线**：路政审批、保险、年审各自记录编号与有效期，90 天内到期自动进入工作台预警。
6. **用地性质联动提示**：公路用地 → 需路政许可；集体土地 → 需流转手续；自有土地 → 直接经营。
7. **黑名单**：黑名单商家不能签订「生效」合同，已签合同自然到期。
8. **操作留痕**：所有写操作记录 `sys_operation_log`（谁、何时、改了什么）。
9. **报表数据全部来自业务表实时聚合**，支持导出 xlsx（含应收催收清单）。

## 说明

- 种子密码使用 Spring Security `{noop}` 明文格式，仅为演示方便；正式使用请将 `SecurityConfig` 中 `PasswordEncoder` 换成 `BCryptPasswordEncoder` 并重置各账号密码。
- JWT 密钥默认 `jwt.secret` 配置项，生产环境请用环境变量 `JWT_SECRET` 覆盖。
