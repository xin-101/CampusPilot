-- ============================================================
-- CampusPilot Migration V002: 比赛演示账号（Demo Accounts）
-- 幂等：可重复执行（INSERT IGNORE）
-- 密码均为 password，BCrypt hash 固定可复用
-- 应用方式:
--   docker exec -i campuspilot-mysql mysql -uroot -proot campuspilot < V002_demo_accounts.sql
-- ============================================================

USE campuspilot;

-- Password for all users: password
-- BCrypt hash: $2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue
INSERT IGNORE INTO users (id, username, password, role, status) VALUES
(5, 'demo_student',   '$2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue', 'STUDENT',   'ACTIVE'),
(6, 'demo_counselor', '$2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue', 'COUNSELOR', 'ACTIVE'),
(7, 'demo_admin',     '$2a$10$lWIPXrEXsEXF71mAklyFnOFYNcSMEkl3H5qdc8jy4HzuAS4CVU2Ue', 'ADMIN',     'ACTIVE');

-- demo_student 关联学生档案（no-eligibility 演示账号，演示学生/王小明）
INSERT IGNORE INTO students (user_id, student_id, name, grade, major, class_name, enrollment_date, status, phone, email) VALUES
(5, 'D2021001', '演示学生', '2021', '计算机科学与技术', '计科2101班', '2021-09-01', 'ENROLLED', '13800000001', 'demo_student@example.com');