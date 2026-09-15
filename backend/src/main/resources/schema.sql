-- ============================================
-- 校园课程学业规划智能助手系统 - 数据库表结构
-- 兼容 H2 / MySQL
-- ============================================

-- 课程库表
DROP TABLE IF EXISTS course;
CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(50) NOT NULL,
    class_no VARCHAR(10),
    course_name VARCHAR(150) NOT NULL,
    credit DECIMAL(3,1) NOT NULL DEFAULT 0,
    hours INT,
    course_type VARCHAR(20) NOT NULL DEFAULT '必修',
    course_category VARCHAR(50),
    course_attr VARCHAR(20),
    department VARCHAR(100),
    semester VARCHAR(20),
    prerequisites VARCHAR(500),
    enroll_limit VARCHAR(500),
    capacity INT,
    week_day INT,
    start_section INT,
    duration_sections INT,
    weeks VARCHAR(100),
    classroom VARCHAR(200),
    campus VARCHAR(20),
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 学生信息表
DROP TABLE IF EXISTS student;
CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10),
    grade VARCHAR(20),
    major VARCHAR(100),
    department VARCHAR(100),
    class_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status VARCHAR(20) DEFAULT '在读',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 选课记录表
DROP TABLE IF EXISTS enrollment;
CREATE TABLE enrollment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    enroll_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT '已选',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 成绩表
DROP TABLE IF EXISTS grade;
CREATE TABLE grade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    enrollment_id BIGINT,
    score DECIMAL(5,2),
    grade_level VARCHAR(10),
    credit_earned DECIMAL(3,1) DEFAULT 0,
    semester VARCHAR(20) NOT NULL,
    is_pass TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 毕业要求/培养方案规则表
DROP TABLE IF EXISTS graduation_requirement;
CREATE TABLE graduation_requirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    major VARCHAR(100) NOT NULL,
    grade VARCHAR(20) NOT NULL,
    total_credit DECIMAL(5,1) NOT NULL,
    required_credit DECIMAL(5,1) NOT NULL,
    elective_credit DECIMAL(5,1) NOT NULL,
    public_basic_credit DECIMAL(5,1),
    major_basic_credit DECIMAL(5,1),
    major_core_credit DECIMAL(5,1),
    practice_credit DECIMAL(5,1),
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_enrollment_student ON enrollment(student_id);
CREATE INDEX idx_enrollment_course ON enrollment(course_id);
CREATE INDEX idx_grade_student ON grade(student_id);
CREATE INDEX idx_grade_course ON grade(course_id);
CREATE INDEX idx_course_type ON course(course_type);
