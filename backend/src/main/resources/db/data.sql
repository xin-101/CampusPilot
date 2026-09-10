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
INSERT INTO policies (title, category, department, description, status) VALUES
('2025年国家奖学金管理办法', 'SCHOLARSHIP', '学生工作处', '国家奖学金管理办法', 'ACTIVE'),
('2025年国家励志奖学金管理办法', 'SCHOLARSHIP', '学生工作处', '国家励志奖学金管理办法', 'ACTIVE'),
('2025年国家助学金管理办法', 'AID', '学生工作处', '国家助学金管理办法', 'ACTIVE'),
('学生请假管理办法', 'LEAVE', '学生工作处', '学生请假管理办法', 'ACTIVE'),
('考试管理办法', 'EXAMINATION', '教务处', '考试管理办法', 'ACTIVE'),
('宿舍管理规定', 'DORMITORY', '后勤管理处', '宿舍管理规定', 'ACTIVE'),
('在读证明办理流程', 'CERTIFICATE', '教务处', '在读证明办理流程', 'ACTIVE');

-- Insert policy versions (DEMO DATA)
INSERT INTO policy_versions (policy_id, version, content, effective_date, expiry_date, source, status) VALUES
(1, 'v1', '国家奖学金申请条件：\n1. 具有正式学籍的在校生\n2. 热爱祖国，拥护中国共产党的领导\n3. 遵守宪法和法律，遵守学校规章制度\n4. 诚实守信，道德品质优良\n5. 在校期间学习成绩优异，综合测评成绩排名在本专业前10%\n6. 社会实践、创新能力、综合素质等方面特别突出\n\n申请时间：每年9月1日至9月30日\n\n所需材料：\n1. 国家奖学金申请表\n2. 成绩单（教务处盖章）\n3. 综合测评证明（学院盖章）', '2025-01-01', '2025-12-31', '教育部', 'ACTIVE'),
(2, 'v1', '国家励志奖学金申请条件：\n1. 具有正式学籍的在校生\n2. 热爱祖国，拥护中国共产党的领导\n3. 遵守宪法和法律，遵守学校规章制度\n4. 诚实守信，道德品质优良\n5. 在校期间学习成绩优秀\n6. 家庭经济困难，生活俭朴\n\n申请时间：每年9月1日至9月30日\n\n所需材料：\n1. 国家励志奖学金申请表\n2. 成绩单（教务处盖章）\n3. 家庭经济困难证明', '2025-01-01', '2025-12-31', '教育部', 'ACTIVE'),
(3, 'v1', '国家助学金申请条件：\n1. 具有正式学籍的在校生\n2. 热爱祖国，拥护中国共产党的领导\n3. 遵守宪法和法律，遵守学校规章制度\n4. 诚实守信，道德品质优良\n5. 家庭经济困难，生活俭朴\n\n申请时间：每年9月1日至9月30日\n\n所需材料：\n1. 国家助学金申请表\n2. 家庭经济困难证明', '2025-01-01', '2025-12-31', '教育部', 'ACTIVE'),
(4, 'v1', '学生请假管理办法：\n1. 学生因病或因事不能参加正常教学活动，必须办理请假手续\n2. 请假1天以内，由辅导员批准\n3. 请假1-3天，由学院副书记批准\n4. 请假3天以上，由学院院长批准\n\n所需材料：\n1. 请假条\n2. 相关证明材料（如病假需医院证明）', '2025-01-01', '2025-12-31', '学生工作处', 'ACTIVE'),
(5, 'v1', '考试管理办法：\n1. 学生必须携带学生证或身份证参加考试\n2. 考试开始30分钟后不得进入考场\n3. 考试期间不得携带手机等电子设备\n4. 违纪行为将按学校相关规定处理', '2025-01-01', '2025-12-31', '教务处', 'ACTIVE'),
(6, 'v1', '宿舍管理规定：\n1. 晚上11点门禁，超过时间需登记\n2. 禁止使用大功率电器\n3. 保持宿舍卫生整洁\n4. 禁止留宿外来人员', '2025-01-01', '2025-12-31', '后勤管理处', 'ACTIVE'),
(7, 'v1', '在读证明办理流程：\n1. 学生本人持学生证到教务处办理\n2. 填写在读证明申请表\n3. 教务处审核后出具证明\n\n所需材料：\n1. 学生证\n2. 在读证明申请表\n3. 一寸照片1张', '2025-01-01', '2025-12-31', '教务处', 'ACTIVE');

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
(2, 1, '国家奖学金申请', '准备并提交国家奖学金申请材料', 'PENDING', 'HIGH', '2025-09-30 23:59:59'),
(2, 4, '请假申请', '提交请假申请', 'IN_PROGRESS', 'NORMAL', '2025-10-15 23:59:59'),
(3, 1, '国家奖学金申请', '准备并提交国家奖学金申请材料', 'PENDING', 'HIGH', '2025-09-30 23:59:59');

-- Insert task steps (DEMO DATA)
INSERT INTO task_steps (task_id, step_order, title, description, status) VALUES
(1, 1, '查询奖学金政策', '了解国家奖学金申请条件和要求', 'COMPLETED'),
(1, 2, '准备申请材料', '准备成绩单、综合测评证明等材料', 'PENDING'),
(1, 3, '提交申请', '在规定时间内提交申请', 'PENDING'),
(2, 1, '填写请假条', '填写请假申请表', 'COMPLETED'),
(2, 2, '提交辅导员审批', '将请假条提交给辅导员', 'IN_PROGRESS'),
(2, 3, '等待审批结果', '等待辅导员审批', 'PENDING');

-- Insert demo notifications (DEMO DATA)
INSERT INTO notifications (user_id, task_id, title, content, type, is_read) VALUES
(2, 1, '国家奖学金申请提醒', '国家奖学金申请截止日期为9月30日，请尽快准备材料', 'REMINDER', 0),
(2, 2, '请假申请状态更新', '你的请假申请已提交给辅导员审批', 'INFO', 1),
(3, 3, '国家奖学金申请提醒', '国家奖学金申请截止日期为9月30日，请尽快准备材料', 'REMINDER', 0);