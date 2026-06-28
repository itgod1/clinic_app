# 口腔诊所 SaaS 管理系统

一套完整的口腔诊所信息化管理解决方案，包含管理后台、后端 API 和患者端小程序。

## 项目结构

```
clinic_app/
├── clinic-admin/      # 管理后台前端 (Vue 3)
├── clinic-api/        # 后端 API (Spring Boot)
└── clinic-miniapp/    # 患者端小程序 (uni-app)
```

## 功能特性

### 管理后台 (clinic-admin)

- **患者管理** - 患者档案、就诊记录、牙位图
- **医生管理** - 医生信息、排班管理、工作台
- **挂号管理** - 挂号登记、分诊处理
- **就诊管理** - 病历记录、处方开具、影像管理
- **收费管理** - 费用结算、欠费管理
- **库存管理** - 物品入库、出库、查询
- **报表统计** - 日报表、月报表、统计分析
- **系统管理** - 用户、角色、菜单、字典配置
- **多租户** - 支持多诊所数据隔离

### 小程序 (clinic-miniapp)

- **预约挂号** - 在线选择医生、时段预约
- **医生查询** - 医生列表、详情查看
- **就诊记录** - 历史病历查询
- **在线支付** - 费用查询与支付
- **AI 咨询** - 智能问诊
- **AI 回访** - 自动回访提醒

## 技术栈

### 前端 (clinic-admin)

| 技术 | 版本 |
|------|------|
| Vue | 3.4 |
| TypeScript | 5.4 |
| Element Plus | 2.6 |
| Vite | 5.2 |
| Pinia | 2.1 |
| Vue Router | 4.3 |
| ECharts | 5.5 |

### 后端 (clinic-api)

| 技术 | 版本 |
|------|------|
| Spring Boot | 2.7.18 |
| MyBatis Plus | 3.5.5 |
| MySQL | - |
| Redis | - |
| Spring Security | - |
| JWT | 0.11.5 |
| Knife4j (Swagger) | 4.3.0 |

### 小程序 (clinic-miniapp)

| 技术 | 版本 |
|------|------|
| uni-app | 3.0 |
| Vue | 3.3 |
| TypeScript | 5.2 |

## 快速开始

### 环境要求

- Node.js >= 16
- JDK >= 17
- MySQL >= 5.7
- Redis

### 后端启动

```bash
cd clinic-api

# 配置数据库连接 (修改 application.yml)
# 配置 Redis 连接

# 构建并运行
mvn spring-boot:run
```

后端服务启动后访问 API 文档: `http://localhost:8080/doc.html`

### 管理后台启动

```bash
cd clinic-admin

# 安装依赖
npm install

# 开发模式启动
npm run dev

# 生产构建
npm run build
```

### 小程序启动

```bash
cd clinic-miniapp

# 安装依赖
npm install

# 微信小程序开发模式
npm run dev:mp-weixin

# H5 开发模式
npm run dev:h5

# 构建微信小程序
npm run build:mp-weixin

# 构建 H5
npm run build:h5
```

## 项目截图

### 管理后台

- 工作台
- 患者管理
- 就诊记录
- 牙位图
- 影像对比

## 许可证

MIT License