-- CampusPilot Database Schema
-- This file contains the database schema for CampusPilot

-- Create database
CREATE DATABASE IF NOT EXISTS campuspilot
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE campuspilot;

-- Drop tables if they exist (for development)
DROP TABLE IF EXISTS tool_execution_logs;
DROP TABLE IF EXISTS agent_execution_logs;
DROP TABLE IF EXISTS agent_messages;
DROP TABLE IF EXISTS agent_sessions;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS task_materials;
DROP TABLE IF EXISTS task_steps;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS policy_materials;
DROP TABLE IF EXISTS policy_conditions;
DROP TABLE IF EXISTS policy_versions;
DROP TABLE IF EXISTS policies;
DROP TABLE IF EXISTS eligibility_rules;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名/学号/工号',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    role VARCHAR(20) NOT NULL COMMENT '角色: STUDENT/COUNSELOR/ADMIN',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    UNIQUE KEY uk_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Create students table
CREATE TABLE students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    student_id VARCHAR(20) NOT NULL COMMENT '学号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    grade VARCHAR(10) NOT NULL COMMENT '年级: 2021/2022/2023/2024',
    major VARCHAR(100) NOT NULL COMMENT '专业',
    class_name VARCHAR(50) NOT NULL COMMENT '班级',
    enrollment_date DATE COMMENT '入学日期',
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' COMMENT '学籍状态: ENROLLED/GRADUATED/SUSPENDED/WITHDRAWN',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    UNIQUE KEY uk_student_id (student_id),
    INDEX idx_user_id (user_id),
    INDEX idx_grade (grade),
    INDEX idx_major (major),
    INDEX idx_class_name (class_name),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

-- Create policies table
CREATE TABLE policies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '政策ID',
    title VARCHAR(200) NOT NULL COMMENT '政策标题',
    category VARCHAR(50) NOT NULL COMMENT '政策分类: SCHOLARSHIP/AID/LEAVE/EXAMINATION/DORMITORY/CERTIFICATE',
    department VARCHAR(100) COMMENT '发布部门',
    description TEXT COMMENT '政策描述',
    keywords VARCHAR(255) COMMENT '关键词(逗号分隔)，用于本地关键词检索',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_category (category),
    INDEX idx_department (department),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='政策表';

-- Create policy_versions table
CREATE TABLE policy_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '政策版本ID',
    policy_id BIGINT NOT NULL COMMENT '政策ID',
    version VARCHAR(20) NOT NULL COMMENT '版本号: v1/v2/v3',
    content TEXT NOT NULL COMMENT '政策内容',
    keywords VARCHAR(255) COMMENT '关键词(逗号分隔)，用于本地关键词检索',
    effective_date DATE NOT NULL COMMENT '生效日期',
    expiry_date DATE COMMENT '失效日期',
    source VARCHAR(200) COMMENT '来源',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE/EXPIRED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_policy_id (policy_id),
    INDEX idx_version (version),
    INDEX idx_effective_date (effective_date),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='政策版本表';

-- Create policy_conditions table
CREATE TABLE policy_conditions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '条件ID',
    policy_version_id BIGINT NOT NULL COMMENT '政策版本ID',
    condition_name VARCHAR(100) NOT NULL COMMENT '条件名称',
    condition_type VARCHAR(50) NOT NULL COMMENT '条件类型: GRADE/SCORE/CREDIT/STATUS',
    condition_value VARCHAR(100) NOT NULL COMMENT '条件值',
    operator VARCHAR(20) NOT NULL COMMENT '运算符: EQ/GT/LT/GTE/LTE/IN/BETWEEN',
    description TEXT COMMENT '条件说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_policy_version_id (policy_version_id),
    INDEX idx_condition_type (condition_type),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (policy_version_id) REFERENCES policy_versions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='政策条件表';

-- Create policy_materials table
CREATE TABLE policy_materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '材料ID',
    policy_version_id BIGINT NOT NULL COMMENT '政策版本ID',
    material_name VARCHAR(100) NOT NULL COMMENT '材料名称',
    material_type VARCHAR(50) NOT NULL COMMENT '材料类型: DOCUMENT/CERTIFICATE/FORM/PHOTO',
    is_required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必填: 0-选填 1-必填',
    description TEXT COMMENT '材料说明',
    template_url VARCHAR(255) COMMENT '模板文件URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_policy_version_id (policy_version_id),
    INDEX idx_material_type (material_type),
    INDEX idx_is_required (is_required),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (policy_version_id) REFERENCES policy_versions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='政策材料表';

-- Create tasks table
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    policy_id BIGINT COMMENT '关联政策ID',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态: PENDING/IN_PROGRESS/COMPLETED/CANCELLED',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级: LOW/NORMAL/HIGH/URGENT',
    deadline DATETIME COMMENT '截止时间(与due_at一致，作为过期提醒依据)',
    due_at DATETIME COMMENT '截止/到期时间',
    remind_at DATETIME COMMENT '提醒时间(到点发送提醒通知)',
    reminder_sent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '提醒是否已发送: 0-未发送 1-已发送(幂等)',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_user_id (user_id),
    INDEX idx_policy_id (policy_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_deadline (deadline),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (policy_id) REFERENCES policies(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- Create notifications table (Phase 3)
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    student_id VARCHAR(32) COMMENT '学生学号',
    type VARCHAR(32) NOT NULL COMMENT '通知类型: TASK/POLICY/SYSTEM/REMINDER',
    title VARCHAR(255) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    related_id BIGINT COMMENT '关联业务ID(任务/政策)',
    is_read TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    read_at DATETIME COMMENT '已读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_is_read (is_read),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- Create task_steps table
CREATE TABLE task_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '步骤ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    step_order INT NOT NULL COMMENT '步骤顺序',
    title VARCHAR(200) NOT NULL COMMENT '步骤标题',
    description TEXT COMMENT '步骤描述',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '步骤状态: PENDING/IN_PROGRESS/COMPLETED/SKIPPED',
    deadline DATETIME COMMENT '截止时间',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_task_id (task_id),
    INDEX idx_step_order (step_order),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务步骤表';

-- Create task_materials table
CREATE TABLE task_materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '材料ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    material_name VARCHAR(100) NOT NULL COMMENT '材料名称',
    material_type VARCHAR(50) NOT NULL COMMENT '材料类型: DOCUMENT/CERTIFICATE/FORM/PHOTO',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '材料状态: PENDING/SUBMITTED/VERIFIED/REJECTED',
    file_path VARCHAR(255) COMMENT '文件路径',
    file_name VARCHAR(100) COMMENT '文件名',
    remark TEXT COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_task_id (task_id),
    INDEX idx_material_type (material_type),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务材料表';

-- Create agent_sessions table
CREATE TABLE agent_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    session_id VARCHAR(50) NOT NULL COMMENT '会话ID(UUID)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态: ACTIVE/INACTIVE/CLOSED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent会话表';

-- Create agent_messages table
CREATE TABLE agent_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    message_id VARCHAR(50) NOT NULL COMMENT '消息ID(UUID)',
    role VARCHAR(20) NOT NULL COMMENT '角色: USER/ASSISTANT/SYSTEM',
    content TEXT NOT NULL COMMENT '消息内容',
    intent VARCHAR(50) COMMENT '识别到的意图',
    metadata JSON COMMENT '元数据(JSON)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    UNIQUE KEY uk_message_id (message_id),
    INDEX idx_session_id (session_id),
    INDEX idx_role (role),
    INDEX idx_intent (intent),
    INDEX idx_created_at (created_at),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (session_id) REFERENCES agent_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent消息表';

-- Create agent_execution_logs table
CREATE TABLE agent_execution_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '执行日志ID',
    execution_id VARCHAR(50) NOT NULL COMMENT '执行ID(UUID)',
    session_id VARCHAR(50) COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_id VARCHAR(50) COMMENT '消息ID',
    intent VARCHAR(50) COMMENT '意图',
    workflow VARCHAR(50) COMMENT '工作流',
    tools JSON COMMENT '工具列表(JSON)',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration BIGINT COMMENT '耗时(毫秒)',
    status VARCHAR(20) NOT NULL COMMENT '状态: RUNNING/SUCCESS/FAILED/TIMEOUT',
    error TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    UNIQUE KEY uk_execution_id (execution_id),
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_message_id (message_id),
    INDEX idx_intent (intent),
    INDEX idx_workflow (workflow),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent执行日志表';

-- Create tool_execution_logs table
CREATE TABLE tool_execution_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '工具执行日志ID',
    execution_log_id BIGINT NOT NULL COMMENT '执行日志ID',
    tool_name VARCHAR(50) NOT NULL COMMENT '工具名称',
    tool_input JSON COMMENT '工具输入(JSON)',
    tool_output JSON COMMENT '工具输出(JSON)',
    duration BIGINT COMMENT '耗时(毫秒)',
    status VARCHAR(20) NOT NULL COMMENT '状态: SUCCESS/FAILED/TIMEOUT',
    error TEXT COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    INDEX idx_execution_log_id (execution_log_id),
    INDEX idx_tool_name (tool_name),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_is_deleted (is_deleted),
    
    FOREIGN KEY (execution_log_id) REFERENCES agent_execution_logs(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具执行日志表';

-- Create eligibility_rules table (资格规则引擎)
DROP TABLE IF EXISTS eligibility_rules;
CREATE TABLE eligibility_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    rule_id VARCHAR(50) NOT NULL COMMENT '业务规则编码',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    category VARCHAR(50) NOT NULL COMMENT '政策分类: SCHOLARSHIP/AID/LEAVE/EXAMINATION/DORMITORY/CERTIFICATE',
    field_name VARCHAR(50) NOT NULL COMMENT '学生字段: GRADE/GPA/RANK/STATUS/HARDSHIP',
    operator VARCHAR(20) NOT NULL COMMENT '运算符: EQ/GT/GTE/LT/LTE/IN/CONTAINS',
    expected_value VARCHAR(100) NOT NULL COMMENT '期望值',
    weight INT NOT NULL DEFAULT 10 COMMENT '规则权重',
    required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必选: 0-可选 1-必选',
    description TEXT COMMENT '规则说明',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用 1-启用',
    version VARCHAR(20) NOT NULL DEFAULT 'v1' COMMENT '规则版本',
    source_policy_id BIGINT COMMENT '来源政策ID',
    is_demo TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否演示规则: 1-演示 0-正式',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除 1-已删除',
    
    UNIQUE KEY uk_rule_id (rule_id),
    INDEX idx_category (category),
    INDEX idx_field_name (field_name),
    INDEX idx_enabled (enabled),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='资格规则表(演示规则)';