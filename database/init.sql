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

-- CampusPilot Demo Data
-- This file contains demo data for testing
-- NOTE: All data is marked as DEMO/TEST DATA

USE campuspilot;

-- Insert demo users
-- Password for all users: password
-- BCrypt hash: $2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue
INSERT INTO users (id, username, password, role, status) VALUES
(1, 'admin', '$2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue', 'ADMIN', 'ACTIVE'),
(2, '2021001', '$2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue', 'STUDENT', 'ACTIVE'),
(3, '2021002', '$2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue', 'STUDENT', 'ACTIVE'),
(4, 'counselor1', '$2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue', 'COUNSELOR', 'ACTIVE');

-- Insert demo students (DEMO DATA)
INSERT INTO students (user_id, student_id, name, grade, major, class_name, enrollment_date, status, phone, email) VALUES
(2, '2021001', '张三', '2021', '计算机科学与技术', '计科2101班', '2021-09-01', 'ENROLLED', '13800138001', 'zhangsan@example.com'),
(3, '2021002', '李四', '2021', '软件工程', '软工2101班', '2021-09-01', 'ENROLLED', '13800138002', 'lisi@example.com');

-- Insert demo policies (DEMO DATA)
INSERT INTO policies (title, category, department, description, keywords, status) VALUES
('2025年国家奖学金管理办法', 'SCHOLARSHIP', '学生工作处', '国家奖学金管理办法', '国家奖学金,奖学金,申请,荣耀,优等生', 'ACTIVE'),
('2025年国家励志奖学金管理办法', 'SCHOLARSHIP', '学生工作处', '国家励志奖学金管理办法', '励志奖学金,奖学金,家庭经济困难,申请', 'ACTIVE'),
('2025年国家助学金管理办法', 'AID', '学生工作处', '国家助学金管理办法', '助学金,贫困生,资助,申请', 'ACTIVE'),
('学生请假管理办法', 'LEAVE', '学生工作处', '学生请假管理办法', '请假,请假条,病假,事假,审批', 'ACTIVE'),
('考试管理办法', 'EXAMINATION', '教务处', '考试管理办法', '考试,考场,违纪,考生', 'ACTIVE'),
('宿舍管理规定', 'DORMITORY', '后勤管理处', '宿舍管理规定', '宿舍,门禁,大功率电器,卫生', 'ACTIVE'),
('在读证明办理流程', 'CERTIFICATE', '教务处', '在读证明办理流程', '在读证明,证明,办理,模板', 'ACTIVE');

-- Insert policy versions (DEMO DATA)
INSERT INTO policy_versions (policy_id, version, content, keywords, effective_date, expiry_date, source, status) VALUES
(1, 'v1', '国家奖学金申请条件：\n1. 具有正式学籍的在校生\n2. 热爱祖国，拥护中国共产党的领导\n3. 遵守宪法和法律，遵守学校规章制度\n4. 诚实守信，道德品质优良\n5. 在校期间学习成绩优异，综合测评成绩排名在本专业前10%\n6. 社会实践、创新能力、综合素质等方面特别突出\n\n申请时间：每年9月1日至9月30日\n\n所需材料：\n1. 国家奖学金申请表\n2. 成绩单（教务处盖章）\n3. 综合测评证明（学院盖章）', '国家奖学金,综合测评,前10%', '2026-01-01', '2026-12-31', '教育部', 'ACTIVE'),
(2, 'v1', '国家励志奖学金申请条件：\n1. 具有正式学籍的在校生\n2. 热爱祖国，拥护中国共产党的领导\n3. 遵守宪法和法律，遵守学校规章制度\n4. 诚实守信，道德品质优良\n5. 在校期间学习成绩优秀\n6. 家庭经济困难，生活俭朴\n\n申请时间：每年9月1日至9月30日\n\n所需材料：\n1. 国家励志奖学金申请表\n2. 成绩单（教务处盖章）\n3. 家庭经济困难证明', '励志奖学金,家庭经济困难,前10%', '2026-01-01', '2026-12-31', '教育部', 'ACTIVE'),
(3, 'v1', '国家助学金申请条件：\n1. 具有正式学籍的在校生\n2. 热爱祖国，拥护中国共产党的领导\n3. 遵守宪法和法律，遵守学校规章制度\n4. 诚实守信，道德品质优良\n5. 家庭经济困难，生活俭朴\n\n申请时间：每年9月1日至9月30日\n\n所需材料：\n1. 国家助学金申请表\n2. 家庭经济困难证明', '助学金,家庭经济困难,贫困', '2026-01-01', '2026-12-31', '教育部', 'ACTIVE'),
(4, 'v1', '学生请假管理办法：\n1. 学生因病或因事不能参加正常教学活动，必须办理请假手续\n2. 请假1天以内，由辅导员批准\n3. 请假1-3天，由学院副书记批准\n4. 请假3天以上，由学院院长批准\n\n所需材料：\n1. 请假条\n2. 相关证明材料（如病假需医院证明）', '请假,审批,病假,事假', '2026-01-01', '2026-12-31', '学生工作处', 'ACTIVE'),
(5, 'v1', '考试管理办法：\n1. 学生必须携带学生证或身份证参加考试\n2. 考试开始30分钟后不得进入考场\n3. 考试期间不得携带手机等电子设备\n4. 违纪行为将按学校相关规定处理', '考试,考场,违纪', '2026-01-01', '2026-12-31', '教务处', 'ACTIVE'),
(6, 'v1', '宿舍管理规定：\n1. 晚上11点门禁，超过时间需登记\n2. 禁止使用大功率电器\n3. 保持宿舍卫生整洁\n4. 禁止留宿外来人员', '宿舍,门禁,大功率电器', '2026-01-01', '2026-12-31', '后勤管理处', 'ACTIVE'),
(7, 'v1', '在读证明办理流程：\n1. 学生本人持学生证到教务处办理\n2. 填写在读证明申请表\n3. 教务处审核后出具证明\n\n所需材料：\n1. 学生证\n2. 在读证明申请表\n3. 一寸照片1张', '在读证明,证明,教务处', '2026-01-01', '2026-12-31', '教务处', 'ACTIVE');

-- Insert policy conditions (DEMO DATA)
INSERT INTO policy_conditions (policy_version_id, condition_name, condition_type, condition_value, operator, description) VALUES
(1, '年级要求', 'GRADE', '3,4', 'IN', '大三或大四学生'),
(1, '绩点要求', 'SCORE', '3.5', 'GTE', '绩点不低于3.5'),
(1, '排名要求', 'RANK', '10', 'LTE', '综合测评排名前10%'),
(2, '年级要求', 'GRADE', '2,3,4', 'IN', '大二、大三或大四学生'),
(2, '家庭经济困难', 'STATUS', 'true', 'EQ', '家庭经济困难学生');

-- Insert policy materials (DEMO DATA)
INSERT INTO policy_materials (policy_version_id, material_name, material_type, is_required, description) VALUES
(1, '国家奖学金申请表', 'FORM', 1, '学校统一格式'),
(1, '成绩单', 'DOCUMENT', 1, '教务处盖章'),
(1, '综合测评证明', 'CERTIFICATE', 1, '学院盖章'),
(2, '国家励志奖学金申请表', 'FORM', 1, '学校统一格式'),
(2, '成绩单', 'DOCUMENT', 1, '教务处盖章'),
(2, '家庭经济困难证明', 'CERTIFICATE', 1, '民政部门或街道办事处盖章');

-- Insert demo tasks (DEMO DATA)
INSERT INTO tasks (user_id, policy_id, title, description, status, priority, deadline) VALUES
(2, 1, '国家奖学金申请', '准备并提交国家奖学金申请材料', 'PENDING', 'HIGH', '2026-09-30 23:59:59'),
(2, 4, '请假申请', '提交请假申请', 'IN_PROGRESS', 'NORMAL', '2026-10-15 23:59:59'),
(3, 1, '国家奖学金申请', '准备并提交国家奖学金申请材料', 'PENDING', 'HIGH', '2026-09-30 23:59:59');

-- Insert task steps (DEMO DATA)
INSERT INTO task_steps (task_id, step_order, title, description, status) VALUES
(1, 1, '查询奖学金政策', '了解国家奖学金申请条件和要求', 'COMPLETED'),
(1, 2, '准备申请材料', '准备成绩单、综合测评证明等材料', 'PENDING'),
(1, 3, '提交申请', '在规定时间内提交申请', 'PENDING'),
(2, 1, '填写请假条', '填写请假申请表', 'COMPLETED'),
(2, 2, '提交辅导员审批', '将请假条提交给辅导员', 'IN_PROGRESS'),
(2, 3, '等待审批结果', '等待辅导员审批', 'PENDING');

-- Insert demo notifications (DEMO DATA)
INSERT INTO notifications (user_id, student_id, type, title, content, related_id, is_read) VALUES
(2, '2021001', 'REMINDER', '国家奖学金申请提醒', '国家奖学金申请截止日期为9月30日，请尽快准备材料', 1, 0),
(2, '2021001', 'TASK', '请假申请状态更新', '你的请假申请已提交给辅导员审批', 2, 1),
(3, '2021002', 'REMINDER', '国家奖学金申请提醒', '国家奖学金申请截止日期为9月30日，请尽快准备材料', 3, 0);

-- Insert demo eligibility rules (资格规则引擎 - DEMO_RULE，仅用于演示，非真实校规)
INSERT INTO eligibility_rules (rule_id, name, category, field_name, operator, expected_value, weight, required, description, enabled, version, source_policy_id, is_demo) VALUES
('DEMO_RULE_SCH_GRADE', '国家奖学金-年级要求', 'SCHOLARSHIP', 'GRADE', 'IN', '3,4', 15, 1, 'DEMO规则：大三或大四学生可申请国家奖学金', 1, 'v1', 1, 1),
('DEMO_RULE_SCH_GPA', '国家奖学金-绩点要求', 'SCHOLARSHIP', 'GPA', 'GTE', '3.5', 20, 1, 'DEMO规则：绩点不低于3.5', 1, 'v1', 1, 1),
('DEMO_RULE_SCH_RANK', '国家奖学金-排名要求', 'SCHOLARSHIP', 'RANK', 'LTE', '10', 25, 1, 'DEMO规则：综合测评排名前10%', 1, 'v1', 1, 1),
('DEMO_RULE_AID_GRADE', '国家励志奖学金-年级要求', 'SCHOLARSHIP', 'GRADE', 'IN', '2,3,4', 15, 1, 'DEMO规则：大二、大三或大四学生', 1, 'v1', 2, 1),
('DEMO_RULE_AID_HARDSHIP', '国家励志奖学金-经济困难', 'SCHOLARSHIP', 'HARDSHIP', 'EQ', 'true', 25, 1, 'DEMO规则：家庭经济困难学生', 1, 'v1', 2, 1),
('DEMO_RULE_AID_STATUS', '国家助学金-学籍状态', 'AID', 'STATUS', 'EQ', 'ENROLLED', 10, 1, 'DEMO规则：具有正式学籍的在读学生', 1, 'v1', 3, 1),
('DEMO_RULE_AID_HARDSHIP2', '国家助学金-经济困难', 'AID', 'HARDSHIP', 'EQ', 'true', 25, 1, 'DEMO规则：家庭经济困难学生', 1, 'v1', 3, 1),
('DEMO_RULE_LEAVE_STATUS', '请假-学籍状态', 'LEAVE', 'STATUS', 'EQ', 'ENROLLED', 10, 1, 'DEMO规则：在读学生可办理请假', 1, 'v1', 4, 1),
('DEMO_RULE_EXAM_STATUS', '考试-学籍状态', 'EXAMINATION', 'STATUS', 'EQ', 'ENROLLED', 10, 1, 'DEMO规则：在读学生可参加考试', 1, 'v1', 5, 1),
('DEMO_RULE_DORM_STATUS', '宿舍-学籍状态', 'DORMITORY', 'STATUS', 'EQ', 'ENROLLED', 10, 1, 'DEMO规则：在读学生适用宿舍规定', 1, 'v1', 6, 1),
('DEMO_RULE_CERT_STATUS', '在读证明-学籍状态', 'CERTIFICATE', 'STATUS', 'EQ', 'ENROLLED', 10, 1, 'DEMO规则：在读学生可办理在读证明', 1, 'v1', 7, 1);