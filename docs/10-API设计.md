# CampusPilot API设计

## 一、API概述

### 1.1 API设计原则
1. **RESTful设计**：遵循REST架构风格
2. **统一响应格式**：所有接口返回统一格式的JSON
3. **版本管理**：API版本通过URL路径管理
4. **认证授权**：所有接口需要JWT认证
5. **参数校验**：所有请求参数需要校验
6. **错误处理**：统一的错误处理机制
7. **日志记录**：所有API调用记录日志

### 1.2 API基础路径
```
/api/v1
```

### 1.3 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1726000000000,
  "requestId": "uuid"
}
```

### 1.4 错误响应格式
```json
{
  "code": 400,
  "message": "参数错误",
  "data": null,
  "timestamp": 1726000000000,
  "requestId": "uuid",
  "errors": [
    {
      "field": "username",
      "message": "用户名不能为空"
    }
  ]
}
```

## 二、认证接口

### 2.1 用户登录
```
POST /api/v1/auth/login
```

**请求参数**：
```json
{
  "username": "string",  // 用户名/学号/工号
  "password": "string"   // 密码
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "string",    // JWT Token
    "tokenType": "Bearer",
    "expiresIn": 86400,   // 过期时间(秒)
    "user": {
      "id": 123456,
      "username": "2021001",
      "role": "STUDENT",
      "name": "张三"
    }
  }
}
```

**错误码**：
- 400：参数错误
- 401：用户名或密码错误
- 403：用户被禁用
- 500：服务器错误

### 2.2 用户登出
```
POST /api/v1/auth/logout
```

**请求头**：
```
Authorization: Bearer {token}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 2.3 获取当前用户
```
GET /api/v1/auth/current
```

**请求头**：
```
Authorization: Bearer {token}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123456,
    "username": "2021001",
    "role": "STUDENT",
    "name": "张三",
    "student": {
      "studentId": "2021001",
      "grade": "2021",
      "major": "计算机科学与技术",
      "className": "计科2101班",
      "status": "ENROLLED"
    }
  }
}
```

### 2.4 修改密码
```
PUT /api/v1/auth/password
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "oldPassword": "string",  // 旧密码
  "newPassword": "string"   // 新密码
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 三、学生信息接口

### 3.1 获取学生信息
```
GET /api/v1/student/profile
```

**请求头**：
```
Authorization: Bearer {token}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "userId": 123456,
    "studentId": "2021001",
    "name": "张三",
    "grade": "2021",
    "major": "计算机科学与技术",
    "className": "计科2101班",
    "enrollmentDate": "2021-09-01",
    "status": "ENROLLED",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "avatar": "http://example.com/avatar.jpg"
  }
}
```

### 3.2 获取学生成绩
```
GET /api/v1/student/scores
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "semester": "string"  // 可选，学期，如"2024-2025-1"
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalCredits": 120,
    "gpa": 3.8,
    "averageScore": 88.5,
    "rank": 15,
    "totalStudents": 150,
    "scores": [
      {
        "id": 1,
        "courseName": "数据结构",
        "courseCode": "CS201",
        "credit": 4,
        "score": 92,
        "grade": "A",
        "semester": "2024-2025-1"
      },
      {
        "id": 2,
        "courseName": "操作系统",
        "courseCode": "CS301",
        "credit": 4,
        "score": 88,
        "grade": "B+",
        "semester": "2024-2025-1"
      }
    ]
  }
}
```

### 3.3 获取学籍状态
```
GET /api/v1/student/status
```

**请求头**：
```
Authorization: Bearer {token}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "ENROLLED",
    "statusName": "在读",
    "enrollmentDate": "2021-09-01",
    "expectedGraduationDate": "2025-06-30",
    "totalCredits": 120,
    "completedCredits": 90,
    "remainingCredits": 30
  }
}
```

## 四、政策接口

### 4.1 获取政策列表
```
GET /api/v1/policies
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "category": "string",    // 可选，政策分类
  "status": "string",      // 可选，政策状态
  "page": 1,               // 页码
  "pageSize": 10           // 每页数量
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 50,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": 1,
        "title": "2025年国家奖学金管理办法",
        "category": "SCHOLARSHIP",
        "categoryName": "奖学金",
        "department": "学生工作处",
        "description": "国家奖学金管理办法",
        "status": "ACTIVE",
        "currentVersion": "v1",
        "effectiveDate": "2025-01-01",
        "expiryDate": "2025-12-31",
        "createdAt": "2025-01-01T00:00:00"
      }
    ]
  }
}
```

### 4.2 获取政策详情
```
GET /api/v1/policies/{id}
```

**请求头**：
```
Authorization: Bearer {token}
```

**路径参数**：
- id：政策ID

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "2025年国家奖学金管理办法",
    "category": "SCHOLARSHIP",
    "categoryName": "奖学金",
    "department": "学生工作处",
    "description": "国家奖学金管理办法",
    "status": "ACTIVE",
    "versions": [
      {
        "id": 1,
        "version": "v1",
        "content": "国家奖学金申请条件：...",
        "effectiveDate": "2025-01-01",
        "expiryDate": "2025-12-31",
        "source": "教育部",
        "status": "ACTIVE"
      }
    ],
    "conditions": [
      {
        "id": 1,
        "conditionName": "年级要求",
        "conditionType": "GRADE",
        "conditionValue": "3,4",
        "operator": "IN",
        "description": "大三或大四学生"
      }
    ],
    "materials": [
      {
        "id": 1,
        "materialName": "国家奖学金申请表",
        "materialType": "FORM",
        "isRequired": true,
        "description": "学校统一格式"
      }
    ]
  }
}
```

### 4.3 搜索政策
```
POST /api/v1/policies/search
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "query": "string",           // 自然语言查询
  "category": "string",        // 可选，政策分类
  "year": "string",            // 可选，年份
  "topK": 10                   // 可选，返回数量
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "query": "国家奖学金申请条件",
    "total": 3,
    "policies": [
      {
        "id": 1,
        "title": "2025年国家奖学金管理办法",
        "category": "SCHOLARSHIP",
        "relevance": 0.95,
        "content": "国家奖学金申请条件：...",
        "source": "教务处官网",
        "version": "v1",
        "effectiveDate": "2025-01-01"
      }
    ]
  }
}
```

## 五、任务接口

### 5.1 获取任务列表
```
GET /api/v1/tasks
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "status": "string",      // 可选，任务状态
  "priority": "string",    // 可选，优先级
  "page": 1,               // 页码
  "pageSize": 10           // 每页数量
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 20,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": 1,
        "title": "国家奖学金申请",
        "description": "申请2025年国家奖学金",
        "status": "IN_PROGRESS",
        "statusName": "处理中",
        "priority": "HIGH",
        "priorityName": "高",
        "deadline": "2025-09-30T23:59:59",
        "completedAt": null,
        "progress": 60,
        "createdAt": "2025-09-01T10:00:00",
        "policy": {
          "id": 1,
          "title": "2025年国家奖学金管理办法"
        }
      }
    ]
  }
}
```

### 5.2 获取任务详情
```
GET /api/v1/tasks/{id}
```

**请求头**：
```
Authorization: Bearer {token}
```

**路径参数**：
- id：任务ID

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "国家奖学金申请",
    "description": "申请2025年国家奖学金",
    "status": "IN_PROGRESS",
    "statusName": "处理中",
    "priority": "HIGH",
    "priorityName": "高",
    "deadline": "2025-09-30T23:59:59",
    "completedAt": null,
    "createdAt": "2025-09-01T10:00:00",
    "policy": {
      "id": 1,
      "title": "2025年国家奖学金管理办法"
    },
    "steps": [
      {
        "id": 1,
        "stepOrder": 1,
        "title": "查询奖学金政策",
        "description": "查询国家奖学金申请条件",
        "status": "COMPLETED",
        "completedAt": "2025-09-01T10:05:00"
      },
      {
        "id": 2,
        "stepOrder": 2,
        "title": "准备申请材料",
        "description": "准备申请表、成绩单、综合测评证明",
        "status": "IN_PROGRESS",
        "deadline": "2025-09-15T23:59:59"
      },
      {
        "id": 3,
        "stepOrder": 3,
        "title": "提交申请",
        "description": "提交申请材料到学生工作处",
        "status": "PENDING",
        "deadline": "2025-09-30T23:59:59"
      }
    ],
    "materials": [
      {
        "id": 1,
        "materialName": "国家奖学金申请表",
        "materialType": "FORM",
        "status": "SUBMITTED",
        "fileName": "申请表.pdf",
        "filePath": "/uploads/申请表.pdf"
      },
      {
        "id": 2,
        "materialName": "成绩单",
        "materialType": "DOCUMENT",
        "status": "PENDING"
      },
      {
        "id": 3,
        "materialName": "综合测评证明",
        "materialType": "CERTIFICATE",
        "status": "PENDING"
      }
    ]
  }
}
```

### 5.3 创建任务
```
POST /api/v1/tasks
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "title": "string",           // 任务标题
  "description": "string",     // 任务描述
  "policyId": 1,               // 可选，关联政策ID
  "priority": "NORMAL",        // 可选，优先级
  "deadline": "2025-09-30T23:59:59",  // 可选，截止时间
  "steps": [                   // 可选，任务步骤
    {
      "title": "步骤1标题",
      "description": "步骤1描述",
      "deadline": "2025-09-15T23:59:59"
    }
  ],
  "materials": [               // 可选，所需材料
    {
      "materialName": "材料1",
      "materialType": "FORM",
      "isRequired": true
    }
  ]
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1,
    "message": "任务创建成功"
  }
}
```

### 5.4 更新任务状态
```
PUT /api/v1/tasks/{id}/status
```

**请求头**：
```
Authorization: Bearer {token}
```

**路径参数**：
- id：任务ID

**请求参数**：
```json
{
  "status": "COMPLETED",      // 新状态
  "comment": "已完成申请"     // 可选，备注
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 5.5 更新任务步骤状态
```
PUT /api/v1/tasks/{taskId}/steps/{stepId}/status
```

**请求头**：
```
Authorization: Bearer {token}
```

**路径参数**：
- taskId：任务ID
- stepId：步骤ID

**请求参数**：
```json
{
  "status": "COMPLETED",      // 新状态
  "comment": "已完成"         // 可选，备注
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 5.6 上传任务材料
```
POST /api/v1/tasks/{taskId}/materials/{materialId}/upload
```

**请求头**：
```
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**路径参数**：
- taskId：任务ID
- materialId：材料ID

**请求参数**：
- file：文件

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "fileId": 1,
    "fileName": "申请表.pdf",
    "filePath": "/uploads/申请表.pdf",
    "fileSize": 1024000
  }
}
```

### 5.7 删除任务
```
DELETE /api/v1/tasks/{id}
```

**请求头**：
```
Authorization: Bearer {token}
```

**路径参数**：
- id：任务ID

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 六、通知接口

### 6.1 获取通知列表
```
GET /api/v1/notifications
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "type": "string",        // 可选，通知类型
  "isRead": false,         // 可选，是否已读
  "page": 1,               // 页码
  "pageSize": 10           // 每页数量
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 30,
    "unreadCount": 5,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": 1,
        "title": "奖学金申请提醒",
        "content": "你的国家奖学金申请将在3天后截止，请尽快完成材料提交。",
        "type": "REMINDER",
        "typeName": "提醒",
        "isRead": false,
        "createdAt": "2025-09-27T10:00:00",
        "task": {
          "id": 1,
          "title": "国家奖学金申请"
        }
      }
    ]
  }
}
```

### 6.2 标记通知已读
```
PUT /api/v1/notifications/{id}/read
```

**请求头**：
```
Authorization: Bearer {token}
```

**路径参数**：
- id：通知ID

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 6.3 标记所有通知已读
```
PUT /api/v1/notifications/read-all
```

**请求头**：
```
Authorization: Bearer {token}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "count": 5  // 标记已读的数量
  }
}
```

### 6.4 删除通知
```
DELETE /api/v1/notifications/{id}
```

**请求头**：
```
Authorization: Bearer {token}
```

**路径参数**：
- id：通知ID

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 七、Agent接口

### 7.1 Agent聊天
```
POST /api/v1/agent/chat
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "message": "string",         // 用户消息
  "sessionId": "string"        // 可选，会话ID
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "sessionId": "uuid",
    "messageId": "uuid",
    "response": "string",       // AI回复
    "intent": "SCHOLARSHIP_INQUIRY",  // 识别到的意图
    "sources": [                // 引用来源
      {
        "policyId": 1,
        "policyName": "2025年国家奖学金管理办法",
        "relevance": 0.95,
        "excerpt": "国家奖学金申请条件：..."
      }
    ],
    "executionSteps": [        // 执行步骤
      {
        "type": "intent",
        "name": "意图识别",
        "status": "SUCCESS",
        "duration": 500
      },
      {
        "type": "knowledge",
        "name": "知识库检索",
        "status": "SUCCESS",
        "duration": 800
      }
    ],
    "suggestedActions": [      // 建议操作
      {
        "action": "check_eligibility",
        "label": "检查申请资格"
      },
      {
        "action": "create_task",
        "label": "创建申请任务"
      }
    ]
  }
}
```

### 7.2 检查资格
```
POST /api/v1/agent/check-eligibility
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "policyId": 1,                // 政策ID
  "studentId": 1                // 可选，学生ID（默认当前用户）
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "eligible": true,           // 是否符合资格
    "overallScore": 85,         // 综合评分
    "conditions": [             // 条件分析
      {
        "name": "年级要求",
        "required": "大三或大四",
        "actual": "大三",
        "met": true,
        "score": 100
      },
      {
        "name": "绩点要求",
        "required": "≥3.5",
        "actual": "3.8",
        "met": true,
        "score": 100
      },
      {
        "name": "排名要求",
        "required": "前10%",
        "actual": "前10%",
        "met": true,
        "score": 100
      }
    ],
    "missingDocuments": [       // 缺失材料
      "综合测评证明"
    ],
    "suggestions": [            // 建议
      "你的条件满足申请要求",
      "建议尽快准备综合测评证明",
      "申请截止日期：2025年9月30日"
    ]
  }
}
```

### 7.3 Agent工具API

#### 获取学生信息工具
```
POST /api/v1/agent/tools/student-info
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "studentId": 1                // 可选，默认当前用户
}
```

**响应参数**：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "studentId": "2021001",
    "name": "张三",
    "grade": "2021",
    "major": "计算机科学与技术",
    "className": "计科2101班",
    "status": "ENROLLED"
  },
  "message": "success",
  "requestId": "uuid"
}
```

#### 获取学生成绩工具
```
POST /api/v1/agent/tools/student-scores
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "studentId": 1,               // 可选，默认当前用户
  "semester": "string"          // 可选，学期
}
```

**响应参数**：
```json
{
  "success": true,
  "data": {
    "totalCredits": 120,
    "gpa": 3.8,
    "averageScore": 88.5,
    "rank": 15,
    "totalStudents": 150
  },
  "message": "success",
  "requestId": "uuid"
}
```

#### 搜索政策工具
```
POST /api/v1/agent/tools/search-policy
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "query": "string",
  "category": "string",         // 可选
  "topK": 5                     // 可选
}
```

**响应参数**：
```json
{
  "success": true,
  "data": {
    "policies": [
      {
        "id": 1,
        "title": "2025年国家奖学金管理办法",
        "relevance": 0.95,
        "content": "国家奖学金申请条件：..."
      }
    ]
  },
  "message": "success",
  "requestId": "uuid"
}
```

#### 检查资格工具
```
POST /api/v1/agent/tools/check-eligibility
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "policyId": 1,
  "studentId": 1
}
```

**响应参数**：
```json
{
  "success": true,
  "data": {
    "eligible": true,
    "conditions": [...],
    "missingDocuments": [...],
    "suggestions": [...]
  },
  "message": "success",
  "requestId": "uuid"
}
```

#### 创建待办工具
```
POST /api/v1/agent/tools/create-todo
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "title": "string",
  "description": "string",
  "deadline": "2025-09-30T23:59:59",
  "relatedPolicyId": 1,
  "steps": [...],
  "materials": [...]
}
```

**响应参数**：
```json
{
  "success": true,
  "data": {
    "taskId": 1,
    "message": "待办创建成功"
  },
  "message": "success",
  "requestId": "uuid"
}
```

#### 发送通知工具
```
POST /api/v1/agent/tools/send-notification
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "studentId": 1,
  "title": "string",
  "content": "string",
  "type": "REMINDER",
  "relatedTaskId": 1
}
```

**响应参数**：
```json
{
  "success": true,
  "data": {
    "notificationId": 1,
    "message": "通知发送成功"
  },
  "message": "success",
  "requestId": "uuid"
}
```

## 八、管理员接口

### 8.1 获取系统统计
```
GET /api/v1/admin/statistics
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "date": "string"  // 可选，日期，默认今天
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayRequests": 127,
    "aiHandled": 91,
    "humanIntervention": 36,
    "aiHandlingRate": 71.7,
    "averageResponseTime": 2.8,
    "workflowSuccessRate": 98.1,
    "knowledgeBaseQueries": 89,
    "toolCalls": 156,
    "userSatisfaction": 4.6
  }
}
```

### 8.2 获取用户列表
```
GET /api/v1/admin/users
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "role": "string",        // 可选，角色
  "status": "string",      // 可选，状态
  "keyword": "string",     // 可选，搜索关键词
  "page": 1,
  "pageSize": 10
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1000,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": 123456,
        "username": "2021001",
        "role": "STUDENT",
        "status": "ACTIVE",
        "name": "张三",
        "lastLoginAt": "2025-09-28T10:00:00",
        "createdAt": "2025-01-01T00:00:00"
      }
    ]
  }
}
```

### 8.3 获取Agent执行日志
```
GET /api/v1/admin/agent-logs
```

**请求头**：
```
Authorization: Bearer {token}
```

**请求参数**：
```json
{
  "userId": 123456,          // 可选，用户ID
  "intent": "string",        // 可选，意图
  "status": "string",        // 可选，状态
  "startDate": "2025-09-01", // 可选，开始日期
  "endDate": "2025-09-30",   // 可选，结束日期
  "page": 1,
  "pageSize": 10
}
```

**响应参数**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 500,
    "page": 1,
    "pageSize": 10,
    "list": [
      {
        "id": 1,
        "executionId": "uuid",
        "sessionId": "uuid",
        "userId": 123456,
        "userName": "张三",
        "intent": "SCHOLARSHIP_INQUIRY",
        "workflow": "POLICY_CONSULTATION",
        "tools": ["search_policy", "student_info"],
        "startTime": "2025-09-28T10:00:00",
        "endTime": "2025-09-28T10:00:03",
        "duration": 3000,
        "status": "SUCCESS"
      }
    ]
  }
}
```

## 九、API错误码

### 9.1 通用错误码
| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用 |

### 9.2 业务错误码
| 错误码 | 说明 |
|--------|------|
| 1001 | 用户名或密码错误 |
| 1002 | 用户被禁用 |
| 1003 | Token已过期 |
| 1004 | Token无效 |
| 2001 | 学生信息不存在 |
| 2002 | 学生成绩不存在 |
| 3001 | 政策不存在 |
| 3002 | 政策已失效 |
| 4001 | 任务不存在 |
| 4002 | 任务已完成 |
| 4003 | 任务已取消 |
| 5001 | 通知不存在 |
| 6001 | Agent服务不可用 |
| 6002 | Agent执行超时 |
| 6003 | Agent执行失败 |

## 十、API安全

### 10.1 认证机制
- 所有API需要JWT认证
- Token通过Authorization头传递
- Token格式：Bearer {token}

### 10.2 权限控制
- 学生只能访问自己的数据
- 辅导员只能访问授权学生数据
- 管理员可以访问所有数据

### 10.3 速率限制
- 普通API：100次/分钟
- Agent API：20次/分钟
- 文件上传：10次/分钟

### 10.4 数据校验
- 所有请求参数需要校验
- 敏感数据需要脱敏
- 文件上传需要病毒扫描

---

*本API设计文档为CampusPilot项目的前后端开发、测试、集成提供完整的接口规范。*