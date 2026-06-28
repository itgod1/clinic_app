# 诊所预约挂号小程序

基于uni-app开发的诊所预约挂号小程序，支持多诊所（SaaS多租户）架构。

## 功能特性

### 核心功能
- ✅ 微信一键登录
- ✅ 多诊所选择切换
- ✅ 在线预约挂号
- ✅ 医生排班查询
- ✅ 挂号记录管理
- ✅ 就诊记录查询
- ✅ 就诊人管理

### 技术特点
- Vue3 + Composition API
- 支持微信小程序
- 自动注入诊所ID（clinic_id）
- Token认证机制
- 响应式设计

## 项目结构

```
clinic-miniapp/
├── src/
│   ├── api/              # API接口
│   ├── components/       # 公共组件
│   ├── mixins/          # 混入
│   ├── pages/           # 页面
│   │   ├── clinic/      # 诊所相关
│   │   ├── doctor/      # 医生相关
│   │   ├── index/       # 首页
│   │   ├── login/       # 登录
│   │   ├── medical/     # 就诊记录
│   │   ├── mine/        # 个人中心
│   │   ├── patient/     # 就诊人管理
│   │   └── registration/# 挂号相关
│   ├── static/          # 静态资源
│   ├── store/           # 状态管理
│   ├── utils/           # 工具函数
│   ├── App.vue          # 应用入口
│   ├── main.js          # 主入口
│   ├── manifest.json    # 应用配置
│   ├── pages.json       # 页面配置
│   └── uni.scss         # 全局样式
├── package.json
├── vite.config.js
└── README.md
```

## 安装运行

### 安装依赖
```bash
npm install
```

### 运行到微信小程序
```bash
npm run dev:mp-weixin
```

### 打包发布
```bash
npm run build:mp-weixin
```

## 后端接口对接

### 基础配置
修改 `src/utils/request.js` 中的 `BASE_URL`：
```javascript
const BASE_URL = 'http://localhost:8080/api'
```

### 多诊所支持
系统自动处理诊所ID注入：
- GET请求：clinicId添加到params
- POST/PUT/DELETE请求：clinicId添加到data
- Header中自动携带 `X-Tenant-Id`

### 登录流程
1. 用户微信授权登录
2. 后端返回用户信息和诊所列表
3. 多诊所用户需选择诊所
4. 单诊所用户直接进入首页

## 页面说明

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录页 | `/pages/login/index` | 微信一键登录 |
| 诊所选择 | `/pages/clinic/select` | 多诊所用户选择诊所 |
| 首页 | `/pages/index/index` | 快捷入口、今日挂号 |
| 医生列表 | `/pages/doctor/list` | 按科室筛选医生 |
| 医生详情 | `/pages/doctor/detail` | 医生信息、预约挂号 |
| 挂号确认 | `/pages/registration/confirm` | 选择就诊人、确认预约 |
| 挂号记录 | `/pages/registration/list` | 查看/取消挂号 |
| 就诊记录 | `/pages/medical/list` | 历史就诊记录 |
| 就诊人管理 | `/pages/patient/list` | 管理就诊人信息 |
| 个人中心 | `/pages/mine/index` | 用户信息、设置 |

## 后端API适配

### 需要新增接口

1. **小程序登录**
```
POST /auth/miniapp/login
参数: { code, encryptedData, iv }
返回: { token, userInfo, clinicList, needSelectClinic }
```

2. **获取用户诊所列表**
```
GET /auth/clinics
返回: { list: [{ id, clinicName, ... }] }
```

### 现有接口适配
确保以下接口支持 `clinicId` 参数：
- GET /admin/doctor/list
- GET /admin/registration/list
- GET /admin/patient/list
- GET /admin/medicalRecord/list
- POST /admin/registration/create

## 注意事项

1. **微信小程序配置**
   - 修改 `src/manifest.json` 中的 `appid`
   - 配置服务器域名（request合法域名）

2. **多诊所切换**
   - 切换诊所后会清空当前页面栈并刷新首页
   - 所有请求自动携带新诊所ID

3. **Token过期处理**
   - 401自动跳转登录页
   - 403无权限访问提示

## 开发计划

- [ ] 微信支付集成
- [ ] 消息通知
- [ ] 电子病历查看
- [ ] 处方详情
- [ ] 评价功能
- [ ] 搜索优化

## License

MIT
